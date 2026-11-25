"""Offline preview of the generated mob models: rasterizes every textured box (with the part pivots and rotations
composed the way ModelPart does, R = Rz*Ry*Rx) from a 3/4 view. Source-asset inspection only, not a game render.
usage: python3 tools/preview/mob_preview.py out.png [ClassName ...]"""
import sys, math, pathlib
import numpy as np
from PIL import Image, ImageDraw
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[1]))
import mobmodels as mm

S = 300


def rot(rx, ry, rz):
    cx, sx, cy, sy, cz, sz = math.cos(rx), math.sin(rx), math.cos(ry), math.sin(ry), math.cos(rz), math.sin(rz)
    X = np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]]); Y = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]]); Z = np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]])
    return Z @ Y @ X


def faces_of(model, tex):
    D = mm.DENSITY
    out = []

    def walk(p, M, T):
        R = M @ rot(*p.rot); P0 = T + M @ np.array(p.pivot, float)
        for b in p.boxes:
            x, y, z = b.o; w, h, d = b.s; u, v = b.uv
            c = lambda a, bb, cc: P0 + R @ np.array([x + a * w, y + bb * h, z + cc * d], float)
            # (corner origin, edge s, edge t, texture rect) per face; s/t go along the texture's +u/+v
            fs = {'north': (c(0, 0, 0), c(1, 0, 0), c(0, 1, 0), (u + d, v + d, w, h)),
                  'south': (c(1, 0, 1), c(0, 0, 1), c(1, 1, 1), (u + 2 * d + w, v + d, w, h)),
                  'west': (c(0, 0, 1), c(0, 0, 0), c(0, 1, 1), (u, v + d, d, h)),
                  'east': (c(1, 0, 0), c(1, 0, 1), c(1, 1, 0), (u + d + w, v + d, d, h)),
                  'up': (c(0, 0, 1), c(1, 0, 1), c(0, 0, 0), (u + d, v, w, d)),
                  'down': (c(0, 1, 0), c(1, 1, 0), c(0, 1, 1), (u + d + w, v, w, d))}
            for o, a, bq, (tu, tv, tw, th) in fs.values():
                if tw and th: out.append((o, a - o, bq - o, tex[tv * D:(tv + th) * D, tu * D:(tu + tw) * D]))
        for ch in p.children: walk(ch, R, P0)

    for p in model.parts: walk(p, np.eye(3), np.zeros(3))
    return out


def render(model, yaw=-35, pitch=20):
    tex = np.asarray(Image.open(mm.TEX / f'{model.texture}.png').convert('RGBA')).astype(float)
    fl = faces_of(model, tex)
    V = rot(math.radians(pitch), 0, 0) @ rot(0, math.radians(yaw), 0) @ np.diag([-1, 1, 1])  # model y is down, like image y; x mirrored on render
    pts = np.array([V @ q for f in fl for q in (f[0], f[0] + f[1], f[0] + f[2], f[0] + f[1] + f[2])])
    lo, hi = pts[:, :2].min(0), pts[:, :2].max(0)
    k = (S - 30) / max(hi - lo); mid = (lo + hi) / 2
    img = np.zeros((S, S, 3)); img[:] = (34, 38, 46); zb = np.full((S, S), -1e9)
    L = np.array([0.4, 0.8, -0.45]); L /= np.linalg.norm(L)
    for o, a, b, t in fl:
        o2, a2, b2 = V @ o, V @ a, V @ b
        n = np.cross(a2, b2); nn = np.linalg.norm(n)
        if nn < 1e-9 or n[2] > 0: continue  # back-facing (viewer looks along +z; front faces have n.z < 0)
        shade = 0.55 + 0.45 * max(0.0, -float(np.dot(n / nn, -L)))
        q = [(o2 + s * a2 + r * b2) for s, r in ((0, 0), (1, 0), (1, 1), (0, 1))]
        sx = [(p[0] - mid[0]) * k + S / 2 for p in q]; sy = [(p[1] - mid[1]) * k + S / 2 for p in q]
        x0, x1 = max(0, int(min(sx))), min(S - 1, int(max(sx)) + 1); y0, y1 = max(0, int(min(sy))), min(S - 1, int(max(sy)) + 1)
        if x1 <= x0 or y1 <= y0: continue
        A = np.array([[a2[0] * k, b2[0] * k], [a2[1] * k, b2[1] * k]])
        if abs(np.linalg.det(A)) < 1e-9: continue
        Ai = np.linalg.inv(A)
        yy, xx = np.mgrid[y0:y1, x0:x1]
        dx = xx + 0.5 - sx[0]; dy = yy + 0.5 - sy[0]
        s_ = Ai[0, 0] * dx + Ai[0, 1] * dy; r_ = Ai[1, 0] * dx + Ai[1, 1] * dy
        inside = (s_ >= 0) & (s_ < 1) & (r_ >= 0) & (r_ < 1)
        z = o2[2] + s_ * a2[2] + r_ * b2[2]
        th, tw = t.shape[:2]
        ti = np.clip((r_ * th).astype(int), 0, th - 1); tj = np.clip((s_ * tw).astype(int), 0, tw - 1)
        col = t[ti, tj]
        m = inside & (col[..., 3] > 20) & (-z > zb[y0:y1, x0:x1])
        zb[y0:y1, x0:x1][m] = -z[m]
        img[y0:y1, x0:x1][m] = col[..., :3][m] * shade
    return Image.fromarray(np.clip(img, 0, 255).astype(np.uint8))


def main():
    out = sys.argv[1]
    names = sys.argv[2:] or [m.cls for m in mm.M]
    ms = [m for m in mm.M if m.cls in names]
    for m in ms: mm.pack(m)
    cols = 4; rows = math.ceil(len(ms) / cols)
    sheet = Image.new('RGB', (cols * S, rows * (S + 16)), (20, 22, 28))
    d = ImageDraw.Draw(sheet)
    for i, m in enumerate(ms):
        sheet.paste(render(m), ((i % cols) * S, (i // cols) * (S + 16) + 16))
        d.text(((i % cols) * S + 6, (i // cols) * (S + 16) + 2), m.cls.replace('Model', ''), fill=(220, 220, 230))
    sheet.save(out)


if __name__ == '__main__':
    main()
