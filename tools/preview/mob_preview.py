"""Offline preview of the generated mob models (a source check only, not a game render).

  python3 tools/preview/mob_preview.py sheet out.png [Class ...]        3/4 view of each model at rest
  python3 tools/preview/mob_preview.py anim out.png Class [clips]       animation strip: rows of frames per clip
  python3 tools/preview/mob_preview.py gif out.gif Class clip           an animated GIF of one clip

Poses are evaluated exactly like the generated Java: the same clips, Catmull-Rom sampling, weights and head look
(the hand-written procedural lines in each model's `extra`, like ring spins, are not run here).
"""
import sys, math, pathlib
import numpy as np
from PIL import Image, ImageDraw
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[1]))
import mobmodels as mm

S = 300


def rot(rx, ry, rz):
    return mm.rot(rx, ry, rz)


# ---- keyframe sampling (mirrors client/anim/Keys.java) --------------------------------------------------------
def cr(p0, p1, p2, p3, u):
    u2, u3 = u * u, u * u * u
    return 0.5 * (2 * p1 + (-p0 + p2) * u + (2 * p0 - 5 * p1 + 4 * p2 - p3) * u2 + (-p0 + 3 * p1 - 3 * p2 + p3) * u3)


def loop(k, t):
    k = sorted(k); n = len(k)
    if n == 1: return k[0][1]
    t -= math.floor(t)
    i0 = n - 1
    for j in range(n):
        if k[j][0] > t: i0 = j - 1; break
    if i0 < 0: i0 = n - 1
    i1, im, i2 = (i0 + 1) % n, (i0 - 1) % n, (i0 + 2) % n
    span = k[i1][0] - k[i0][0]
    if span <= 0: span += 1
    u = t - k[i0][0]
    if u < 0: u += 1
    return cr(k[im][1], k[i0][1], k[i1][1], k[i2][1], min(1, u / span))


def once(k, t):
    k = sorted(k); n = len(k)
    if t <= k[0][0] or n == 1: return k[0][1]
    if t >= k[-1][0]: return k[-1][1]
    i0 = 0
    while i0 < n - 2 and k[i0 + 1][0] <= t: i0 += 1
    i1, im, i2 = i0 + 1, max(0, i0 - 1), min(n - 1, i0 + 2)
    u = (t - k[i0][0]) / max(1e-6, k[i1][0] - k[i0][0])
    return cr(k[im][1], k[i0][1], k[i1][1], k[i2][1], u)


def ease(x):
    x = min(1, max(0, x)); return x * x * (3 - 2 * x)


def fidget(t, seed, period, dur):
    local = (t + seed * period * 7.31) % period
    start = period - dur
    return (local - start) / dur if local >= start else -1


def evaluate(model, st):
    """st: dict with t, walk_pos, walk_speed, air, swim, attack_age, attack_kind, hurt_age, seed, yaw, pitch."""
    pose = {}

    def bump(part, ch, val):
        d = pose.setdefault(part, {'r': np.zeros(3), 'p': np.zeros(3), 's': np.zeros(3)})
        if ch[0] == 'r': d['r']['xyz'.index(ch[1])] += math.radians(val)
        elif ch[0] == 'p': d['p']['xyz'.index(ch[1])] += val
        elif ch == 's': d['s'] += val
        else: d['s']['xyz'.index(ch[1])] += val

    moveW = ease(min(1, st['walk_speed'] * 2.2)); idleW = 1 - moveW
    air = st['air']; ground = 1 - air; swim = st['swim']; dry = 1 - swim
    env = dict(moveW=moveW, idleW=idleW, air=air, ground=ground, swim=swim, dry=dry)
    W = lambda e: eval(e.replace('f', ''), {}, env)
    t = st['t']
    for c in model.clips:
        if c.kind in ('time', 'dist'):
            w = W(c.weight)
            ph = t / c.length + st['seed'] if c.kind == 'time' else st['walk_pos'] * c.length
            if w > 0.002:
                for (part, ch), k in c.tracks.items(): bump(part, ch, loop(k, ph) * w)
        elif c.kind == 'fidget':
            w = W(c.weight); ph = fidget(t, st['seed'], c.length, c.dur)
            if ph >= 0 and w > 0.002:
                for (part, ch), k in c.tracks.items(): bump(part, ch, once(k, ph) * w)
        elif c.kind in ('attack', 'cast'):
            ph = st['attack_age'] / c.length
            if 0 <= ph < 1 and st['attack_kind'] == c.kind:
                for (part, ch), k in c.tracks.items(): bump(part, ch, once(k, ph))
        elif c.kind == 'hurt':
            ph = st['hurt_age'] / c.length
            if 0 <= ph < 1:
                for (part, ch), k in c.tracks.items(): bump(part, ch, once(k, ph))
    for part, wy, wx in model.look:
        bump(part, 'ry', st['yaw'] * wy); bump(part, 'rx', st['pitch'] * wx)
    return pose


def faces_of(model, tex, pose=None):
    D = mm.DENSITY
    out = []

    def walk(p, M, T0):
        d = (pose or {}).get(p.name)
        r = np.array(p.rot) + (d['r'] if d else 0)
        pv = np.array(p.pivot) + (d['p'] if d else 0)
        sc = np.ones(3) + (d['s'] if d else 0)
        P0 = T0 + M @ pv
        R = M @ rot(*r) @ np.diag(sc)
        for b in p.boxes:
            x, y, z = b.o; w, h, dd = b.s; u, v = b.uv
            c = lambda a, bb, cc: P0 + R @ np.array([x + a * w, y + bb * h, z + cc * dd], float)
            fs = [(c(0, 0, 0), c(1, 0, 0), c(0, 1, 0), (u + dd, v + dd, w, h)),
                  (c(1, 0, 1), c(0, 0, 1), c(1, 1, 1), (u + 2 * dd + w, v + dd, w, h)),
                  (c(0, 0, 1), c(0, 0, 0), c(0, 1, 1), (u, v + dd, dd, h)),
                  (c(1, 0, 0), c(1, 0, 1), c(1, 1, 0), (u + dd + w, v + dd, dd, h)),
                  (c(0, 0, 1), c(1, 0, 1), c(0, 0, 0), (u + dd, v, w, dd)),
                  (c(0, 1, 0), c(1, 1, 0), c(0, 1, 1), (u + dd + w, v, w, dd))]
            for o, a, bq, (tu, tv, tw, th) in fs:
                if tw and th: out.append((o, a - o, bq - o, tex[tv * D:(tv + th) * D, tu * D:(tu + tw) * D]))
        for ch in p.children: walk(ch, R, P0)

    for p in model.parts: walk(p, np.eye(3), np.zeros(3))
    return out


def render(model, pose=None, yaw=-35, pitch=18, frame=None, size=S):
    tex = np.asarray(Image.open(mm.TEX / f'{model.texture}.png').convert('RGBA')).astype(float)
    fl = faces_of(model, tex, pose)
    V = rot(math.radians(pitch), 0, 0) @ rot(0, math.radians(yaw), 0) @ np.diag([-1, 1, 1])
    if frame is None:
        pts = np.array([V @ q for f in fl for q in (f[0], f[0] + f[1], f[0] + f[2], f[0] + f[1] + f[2])])
        lo, hi = pts[:, :2].min(0), pts[:, :2].max(0)
        frame = ((size - 30) / max(hi - lo), (lo + hi) / 2)
    k, mid = frame
    img = np.zeros((size, size, 3)); img[:] = (34, 38, 46); zb = np.full((size, size), -1e9)
    L = np.array([0.4, -0.8, -0.45]); L /= np.linalg.norm(L)
    for o, a, b, t in fl:
        o2, a2, b2 = V @ o, V @ a, V @ b
        n = np.cross(a2, b2); nn = np.linalg.norm(n)
        if nn < 1e-9 or n[2] > 0: continue
        shade = 0.6 + 0.4 * max(0.0, float(np.dot(-n / nn, L)))
        corners = [o2 + s_ * a2 + r_ * b2 for s_, r_ in ((0, 0), (1, 0), (1, 1), (0, 1))]
        sx = [(q[0] - mid[0]) * k + size / 2 for q in corners]; sy = [(q[1] - mid[1]) * k + size / 2 for q in corners]
        x0, x1 = max(0, int(min(sx))), min(size - 1, int(max(sx)) + 1); y0, y1 = max(0, int(min(sy))), min(size - 1, int(max(sy)) + 1)
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
        col = t[np.clip((r_ * th).astype(int), 0, th - 1), np.clip((s_ * tw).astype(int), 0, tw - 1)]
        m = inside & (col[..., 3] > 20) & (-z > zb[y0:y1, x0:x1])
        zb[y0:y1, x0:x1][m] = -z[m]
        img[y0:y1, x0:x1][m] = col[..., :3][m] * shade
    return Image.fromarray(np.clip(img, 0, 255).astype(np.uint8)), frame


BASE = dict(t=0, walk_pos=0, walk_speed=0, air=0, swim=0, attack_age=1e6, attack_kind='attack', hurt_age=1e6, seed=0, yaw=0, pitch=0)


def clip_states(model, clip, n):
    out = []
    flyer = any(c.weight == 'air' for c in model.clips)
    for i in range(n):
        st = dict(BASE); f = i / n
        if clip == 'idle':
            st['t'] = f * 80
        elif clip == 'walk':
            st['walk_speed'] = 1; dist = next(c.length for c in model.clips if c.kind == 'dist'); st['walk_pos'] = f / dist
        elif clip == 'fly':
            st['air'] = 1; ln = next(c.length for c in model.clips if c.kind == 'time' and c.weight == 'air'); st['t'] = f * ln
        elif clip == 'swim':
            st['swim'] = 1; ln = next(c.length for c in model.clips if c.kind == 'time' and c.weight == 'swim'); st['t'] = f * ln
        elif clip in ('attack', 'cast'):
            ln = next(c.length for c in model.clips if c.kind == clip); st['attack_age'] = f * ln; st['attack_kind'] = clip
            st['air'] = 1 if flyer else 0
        elif clip == 'hurt':
            st['hurt_age'] = f * 10
        elif clip.startswith('fidget'):
            c = [c for c in model.clips if c.kind == 'fidget'][int(clip[6:] or 0)]
            st['t'] = c.length - c.dur + f * c.dur
        out.append(st)
    return out


def available_clips(model):
    kinds = [c.kind for c in model.clips]
    names = ['idle']
    if 'dist' in kinds: names.append('walk')
    if any(c.weight == 'air' for c in model.clips): names.append('fly')
    if any(c.weight == 'swim' for c in model.clips): names.append('swim')
    names += [f'fidget{i}' for i in range(kinds.count('fidget'))]
    names += [k for k in ('attack', 'cast') if k in kinds]
    return names + ['hurt']


def model_named(name):
    m = next(m for m in mm.M if m.cls in (name, name + 'Model'))
    mm.ground(m); mm.pack(m)
    return m


def main():
    mode, out = sys.argv[1], sys.argv[2]
    if mode == 'sheet':
        ms = [model_named(n) for n in (sys.argv[3:] or [m.cls for m in mm.M])]
        cols = 4; rows = math.ceil(len(ms) / cols)
        sheet = Image.new('RGB', (cols * S, rows * (S + 16)), (20, 22, 28)); d = ImageDraw.Draw(sheet)
        for i, m in enumerate(ms):
            sheet.paste(render(m)[0], ((i % cols) * S, (i // cols) * (S + 16) + 16))
            d.text(((i % cols) * S + 6, (i // cols) * (S + 16) + 2), m.cls.replace('Model', ''), fill=(220, 220, 230))
        sheet.save(out)
    elif mode == 'anim':
        m = model_named(sys.argv[3])
        clips = sys.argv[4:] or available_clips(m)
        n, sz = 8, 170
        _, frame = render(m, size=sz, yaw=-60)
        sheet = Image.new('RGB', (n * sz + 60, len(clips) * sz), (20, 22, 28)); d = ImageDraw.Draw(sheet)
        for r, c in enumerate(clips):
            d.text((4, r * sz + 4), c, fill=(220, 220, 230))
            for i, st in enumerate(clip_states(m, c, n)):
                sheet.paste(render(m, evaluate(m, st), size=sz, yaw=-60, frame=frame)[0], (60 + i * sz, r * sz))
        sheet.save(out)
    elif mode == 'gif':
        m = model_named(sys.argv[3]); c = sys.argv[4]
        _, frame = render(m, size=240, yaw=-60)
        frames = [render(m, evaluate(m, st), size=240, yaw=-60, frame=frame)[0] for st in clip_states(m, c, 24)]
        frames[0].save(out, save_all=True, append_images=frames[1:], duration=60, loop=0)


if __name__ == '__main__':
    main()
