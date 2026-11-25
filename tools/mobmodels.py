"""Stormreach 5.1 mob models: every Stormreach creature as a detailed jointed model, a texture painted to fit it
(4x density, shaded colour ramps, bevelled edges, crisp material detail), and keyframed animation.

Each model is described below as parts and boxes (Minecraft model units, y down, 16 units = 1 block, feet at y 24),
plus animation clips. A clip is a set of keyframe tracks (part, channel) -> [(t, value)] with t in [0,1]:
  idle / walk / fly / swim loops, fidgets (idle flourishes now and then), attack / cast (played when the server sends
  an AnimPulse) and hurt. Channels: rx ry rz (degrees), px py pz (units), s / sx sy sz (scale delta).
At runtime (client/anim) tracks are sampled with Catmull-Rom splines and blended by how much the mob is moving,
flying or swimming, then head-look and turn-lean are layered on top.

This script writes client/model/gen/*Model.java and textures/entity/*.png. Don't edit those; change this file and
rerun it (run_all.py does). tools/preview/mob_preview.py renders contact sheets and animation strips offline.
"""
import pathlib, math
import numpy as np
from PIL import Image

ROOT = pathlib.Path(__file__).resolve().parent.parent
JAVA = ROOT / 'src' / 'client' / 'java' / 'com' / 'stormreach' / 'client' / 'model' / 'gen'
TEX = ROOT / 'src' / 'main' / 'resources' / 'assets' / 'stormreach' / 'textures' / 'entity'
DENSITY = 4


# ================================================================================================ description DSL
class Box:
    def __init__(self, x, y, z, w, h, d, mat, inflate=0.0, mirror=False):
        self.o = (float(x), float(y), float(z)); self.s = (max(1, int(round(w))), max(1, int(round(h))), max(1, int(round(d))))
        self.mat = mat; self.inflate = inflate; self.mirror = mirror; self.uv = None


class Part:
    def __init__(self, name, pivot=(0, 0, 0), rot=(0, 0, 0), boxes=()):
        self.name = name; self.pivot = tuple(float(v) for v in pivot); self.rot = tuple(float(v) for v in rot)
        self.boxes = list(boxes); self.children = []

    def add(self, *parts):
        self.children.extend(parts); return self

    @property
    def field(self):
        a = self.name.split('_')
        return a[0] + ''.join(w.capitalize() for w in a[1:])


P = Part


def mirror_part(p):
    """A mirrored copy (x flipped) of a part tree, renaming left -> right."""
    q = Part(p.name.replace('left', 'right'), (-p.pivot[0], p.pivot[1], p.pivot[2]), (p.rot[0], -p.rot[1], -p.rot[2]),
             [Box(-(b.o[0] + b.s[0]), b.o[1], b.o[2], *b.s, b.mat, b.inflate, not b.mirror) for b in p.boxes])
    q.children = [mirror_part(c) for c in p.children]
    return q


def pair(p):
    return [p, mirror_part(p)]


class Clip:
    """kind: time (length = ticks per loop), dist (length = cycles per walk-position unit), fidget (length = period,
    dur), attack / cast (dur ticks), hurt."""
    def __init__(self, kind, length, tracks, weight='1f', dur=None):
        self.kind = kind; self.length = length; self.tracks = tracks; self.weight = weight; self.dur = dur


class Model:
    def __init__(self, cls, texture, mats, parts, clips, doc='', look=(), extra='', grounded=True, flags=(), translucent=False):
        self.cls = cls; self.texture = texture; self.mats = mats; self.parts = parts; self.clips = clips; self.translucent = translucent
        self.doc = doc; self.look = look; self.extra = extra; self.grounded = grounded; self.flags = flags


# ---- keyframe helpers ------------------------------------------------------------------------------------------
def even(*vals):
    n = len(vals)
    return [(i / n, v) for i, v in enumerate(vals)]


def shift(keys, d):
    return sorted((((t + d) % 1.0), v) for t, v in keys)


def scale(keys, k):
    return [(t, v * k) for t, v in keys]


def add(keys, c):
    return [(t, v + c) for t, v in keys]


def hold(v):
    return [(0.0, v)]


MIRROR_NEG = {'ry', 'rz', 'px'}


def sym(tracks):
    """Copies every left_* track to the matching right_* part (mirrored), unless that track is given."""
    out = dict(tracks)
    for (part, ch), keys in tracks.items():
        if 'left' in part:
            rk = (part.replace('left', 'right'), ch)
            if rk not in out:
                out[rk] = scale(keys, -1) if ch in MIRROR_NEG else list(keys)
    return out


def T(**kw):
    """T(body_rx=[...], left_leg_ry=[...]) -> {(part, ch): keys}; the channel is the last _xx."""
    out = {}
    for k, v in kw.items():
        part, ch = k.rsplit('_', 1)
        out[(part, ch)] = v
    return out


def hurt_clip(body, head=None, k=1.0):
    tr = {(body, 'rx'): [(0, 0), (0.15, -12 * k), (0.4, 4 * k), (0.7, -1), (1, 0)],
          (body, 'pz'): [(0, 0), (0.12, 0.8 * k), (0.5, -0.1), (1, 0)]}
    if head:
        tr[(head, 'rx')] = [(0, 0), (0.18, -16 * k), (0.5, 5), (1, 0)]
        tr[(head, 'rz')] = [(0, 0), (0.2, 7 * k), (0.55, -3), (1, 0)]
    return Clip('hurt', 10, tr)


def gait_leg(upper, lower, phase, swing=25, knee=40, lift_side=None):
    """A leg cycle: the upper leg sweeps from forward to back through stance, then swings forward with the knee bent.
    Stance takes 60% of the cycle, so the swing forward is quicker, like a real step."""
    tr = {(upper, 'rx'): shift([(0, -swing), (0.3, -swing * 0.05), (0.6, swing), (0.8, swing * 0.15)], phase)}
    if lower:
        tr[(lower, 'rx')] = shift([(0, -knee * 0.05), (0.55, knee * 0.05), (0.72, knee), (0.92, knee * 0.2)], phase)
    return tr


def merge(*ds):
    out = {}
    for d in ds: out.update(d)
    return out


# ================================================================================================ painting
def C(h): return np.array([int(h[i:i + 2], 16) for i in (1, 3, 5)], dtype=np.float64)


def ramp(base, n=7):
    """A shading ramp around a base colour: cool, saturated shadows and warm, lighter highlights."""
    base = np.asarray(base, float)
    shadow = base * 0.36 + np.array([22, 26, 58]) * 0.3
    hi = np.minimum(255, base * 1.16 + np.array([255, 246, 220]) * 0.2)
    out = []
    for i in range(n):
        x = i / (n - 1)
        out.append(shadow + (base - shadow) * (x / 0.5) if x < 0.5 else base + (hi - base) * ((x - 0.5) / 0.5))
    return np.array(out)


BAYER = (np.array([[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]]) / 16.0 - 0.47)


def value_noise(rng, cells, w, h):
    g = rng.random((cells + 1, cells + 1))
    ys = np.linspace(0, cells, h, endpoint=False); xs = np.linspace(0, cells, w, endpoint=False)
    y0 = ys.astype(int); x0 = xs.astype(int); fy = ys - y0; fx = xs - x0
    fy = fy * fy * (3 - 2 * fy); fx = fx * fx * (3 - 2 * fx)
    a = g[y0][:, x0]; b = g[y0][:, x0 + 1]; c = g[y0 + 1][:, x0]; d = g[y0 + 1][:, x0 + 1]
    top = a * (1 - fx) + b * fx; bot = c * (1 - fx) + d * fx
    return top * (1 - fy)[:, None] + bot * fy[:, None]


def fbm(rng, w, h, base=3):
    return (value_noise(rng, base, w, h) + 0.5 * value_noise(rng, base * 2, w, h) + 0.25 * value_noise(rng, base * 4, w, h)) / 1.75


FACE_LIGHT = {'top': 0.16, 'front': 0.02, 'left': -0.04, 'right': -0.04, 'back': -0.08, 'bottom': -0.24}


def paint_face(img, al, x0, y0, w, h, mat, face, rng):
    if w <= 0 or h <= 0: return
    kind, cols = mat[0], mat[1:]
    base = cols[0]; second = cols[1] if len(cols) > 1 else cols[0] * 0.6
    yy, xx = np.mgrid[0:h, 0:w]
    fy, fx = (yy + 0.5) / h, (xx + 0.5) / w
    n = fbm(rng, w, h, 2 + (w + h) // 28)
    side = face not in ('top', 'bottom')
    alpha = np.full((h, w), 255.0)
    over = None  # (mask, rgb) colour overrides painted after shading

    # ---- special, unramped materials --------------------------------------------------------------------------
    if kind == 'glow':
        d = np.hypot(fx - 0.5, fy - 0.5) * 2
        core = np.clip(1 - d, 0, 1) ** 0.8
        rgb = second[None, None] * (1 - core[..., None]) + np.minimum(255, base * 1.2)[None, None] * core[..., None]
        sparkle = (rng.random((h, w)) < 0.03)
        rgb = np.where(sparkle[..., None], np.array([255.0, 255, 255]), rgb)
        img[y0:y0 + h, x0:x0 + w] = np.clip(rgb, 0, 255); al[y0:y0 + h, x0:x0 + w] = 255
        return
    if kind == 'eye':
        rgb = np.broadcast_to(second * 0.8, (h, w, 3)).copy()
        if side:
            d = np.hypot(fx - 0.5, fy - 0.5)
            rgb = np.where((d < 0.46)[..., None], base[None, None], rgb)
            rgb = np.where((d < 0.46)[..., None] & (fy > 0.55)[..., None], base * 0.7, rgb)
            rgb = np.where((d < 0.22)[..., None], np.array([12.0, 12, 18]), rgb)
            rgb = np.where((np.hypot(fx - 0.33, fy - 0.3) < 0.12)[..., None], np.array([255.0, 255, 255]), rgb)
        img[y0:y0 + h, x0:x0 + w] = rgb; al[y0:y0 + h, x0:x0 + w] = 255
        return

    if kind == 'mothwing':
        # sheer aurora wing: colour flows from base to second along the span, dark veins fan out from the root,
        # a wavy band and an eyespot, a pale fringe at the edge, and some see-through
        g = np.clip(fx * 0.8 + fy * 0.35 + (n - 0.5) * 0.3, 0, 1)
        rgb = base[None, None] * (1 - g[..., None]) + second[None, None] * g[..., None]
        lit = 0.85 + FACE_LIGHT[face] * 0.5 + 0.18 * np.sin(fx * 9 + fy * 4 + n * 3)
        rgb = rgb * lit[..., None]
        ang = np.arctan2(fy + 0.15, fx + 0.02)
        vein = np.abs(np.sin(ang * 9)) < 0.08
        band = np.abs(fx - 0.62 - 0.06 * np.sin(fy * 14)) < 0.035
        d = np.hypot((fx - 0.55) * 1.4, fy - 0.5)
        ring = (d > 0.12) & (d < 0.2)
        rgb = np.where((vein | band)[..., None], rgb * 0.55, rgb)
        rgb = np.where(ring[..., None], np.array([30.0, 20, 50]), rgb)
        rgb = np.where((d < 0.09)[..., None], np.array([250.0, 255, 240]), rgb)
        fringe = (fx > 0.92) | (fy < 0.06) | (fy > 0.94)
        rgb = np.where(fringe[..., None], np.minimum(255, rgb * 0.4 + 160), rgb)
        img[y0:y0 + h, x0:x0 + w] = np.clip(rgb, 0, 255)
        al[y0:y0 + h, x0:x0 + w] = np.where(fringe, 235, np.clip(170 + 50 * g, 0, 255))
        return

    # ---- value field: lighting + material pattern, quantized onto the ramp --------------------------------------
    v = 0.5 + FACE_LIGHT[face] + (n - 0.5) * 0.16
    if side: v -= 0.12 * fy
    R = ramp(base)
    if kind in ('shell', 'claw'):
        # broad lacquered plates: a few gently curved seams, each plate lit toward its upper edge, with pitting
        wave = np.sin(xx * 0.18 + (fbm(rng, w, h, 2) - 0.5) * 3) * 2
        band = (yy + wave) / 18.0
        seam = (np.abs(band - np.round(band)) < 0.035) & (yy > 2) & (yy < h - 2)
        v += 0.12 * (1 - (band % 1)) - 0.06
        v -= 0.26 * seam
        v -= 0.1 * (rng.random((h, w)) < 0.025)
        if face == 'top': v += 0.2 * np.clip(1 - np.hypot(fx - 0.35, fy - 0.3) * 2.2, 0, 1)
    elif kind == 'fur':
        # tufts: strands in clumps three pixels wide, each lit at the root and darker at the tip
        clump = (xx // 3)
        off = (np.sin(clump * 2.3) * 3 + np.sin(clump * 0.7) * 2).astype(int)
        phase = (yy + off) % 8
        v += 0.14 * (1 - phase / 7.0) - 0.06 + 0.05 * np.sin(clump * 1.7)
        v -= 0.12 * ((phase == 7) | (xx % 3 == 0) * (phase > 4))
    elif kind == 'feather':
        # overlapping rounded feathers in staggered rows: each lit along its shaft, with a dark curved trailing edge
        FW, FH = 8, 7
        row = yy // FH
        lx = (xx + (row % 2) * (FW // 2)) % FW - (FW - 1) / 2
        ly = yy % FH
        arc = ly - (FH - 1.5) + (lx * lx) / (FW * 0.7)
        v += 0.14 * (1 - np.abs(lx) / (FW / 2)) + 0.05 * (ly / FH) - 0.06
        v -= 0.08 * (np.abs(lx) < 0.6) * (ly < FH - 2)
        v -= 0.26 * (np.abs(arc) < 0.75)
        v += 0.08 * ((arc < -0.75) & (arc > -1.9))
    elif kind == 'scale':
        row = yy // 5
        cx = (xx + (row % 2) * 3) % 6
        v += 0.12 * (1 - (yy % 5) / 5) - 0.04
        v -= 0.24 * ((cx == 0) | (yy % 5 == 4))
    elif kind == 'dscale':
        # overlapping rounded dragon scales, each with a lit crown and a dark rim, with a sheen across the plate
        SW, SH = 5, 4
        row = yy // SH
        lx = (xx + (row % 2) * (SW // 2 + 0.5)) % SW - (SW - 1) / 2
        ly = yy % SH
        rim = ly - (SH - 1) + (lx * lx) / (SW * 0.55)
        v += 0.16 * (1 - ly / SH) * (1 - np.abs(lx) / SW) - 0.05
        v -= 0.28 * (np.abs(rim) < 0.6)
        v += 0.12 * (np.abs((xx * 0.7 - yy) % 30 - 8) < 3)
    elif kind == 'hail':
        # packed hailstones: round lumps with a bright upper-left highlight, shaded undersides and dark gaps
        bl = np.zeros((h, w)); hl = np.zeros((h, w)); taken = np.full((h, w), -1.0)
        for _ in range(max(3, w * h // 26)):
            cx, cy, r = rng.random() * w, rng.random() * h, 2.2 + rng.random() * 2.6
            dd = np.hypot(xx - cx, yy - cy) / r
            inside = (dd < 1) & (1 - dd > taken)
            taken = np.where(inside, 1 - dd, taken)
            bl = np.where(inside, 1 - dd, bl)
            hl = np.where(inside, np.clip(1 - np.hypot(xx - cx + r * 0.35, yy - cy + r * 0.35) / (r * 0.45), 0, 1), hl)
        v = 0.38 + 0.38 * np.sqrt(np.clip(bl, 0, 1)) + 0.25 * hl + FACE_LIGHT[face] * 0.6 + (n - 0.5) * 0.06
        v = np.where(taken < 0, 0.18, v)
    elif kind == 'metal':
        brush = value_noise(rng, max(2, w // 2), w, h)[:1].repeat(h, 0)
        v += 0.1 * (brush - 0.5)
        v += 0.2 * (np.abs((xx - yy * 0.6) % 26 - 6) < 2.5)  # specular streak
        if w > 10 and h > 10:
            for ry, rx in ((2, 2), (2, w - 3), (h - 3, 2), (h - 3, w - 3)):
                over_mask = np.hypot(yy - ry, xx - rx) < 1.6
                v = np.where(over_mask, 0.85, v)
                v = np.where((np.hypot(yy - ry, xx - rx) < 2.4) & ~over_mask, 0.2, v)
    elif kind == 'stone':
        bw, bh = 14, 9
        row = yy // bh
        mortar = (yy % bh == 0) | ((xx + (row % 2) * (bw // 2)) % bw == 0)
        v -= 0.26 * mortar
        v += 0.1 * (fbm(rng, w, h, 5) - 0.5)
        crack = np.abs(np.sin(xx * 0.31 + yy * 0.17 + n * 8)) < 0.035
        v -= 0.2 * crack
        rune = (np.abs(np.sin(xx * 0.2 + n * 5) * 6 + h * 0.5 - yy) < 0.7) & (n > 0.52)
        over = (rune & side, second)
    elif kind == 'cloth':
        fold = np.sin(xx * 0.45 + n * 4)
        v += 0.12 * fold + 0.03 * ((xx + yy) % 2)
        if side:
            hem = fy > 0.88
            over = (hem & (yy % 3 != 0), second)
    elif kind == 'tatter':
        fold = np.sin(xx * 0.45 + n * 4)
        v += 0.12 * fold
        if side:
            jag = fy > 0.72 + 0.22 * value_noise(rng, max(2, w // 3), w, 1)[0][None, :] + 0.05 * np.sin(xx * 1.9)
            alpha = np.where(jag, 0, 255.0)
    elif kind == 'bone':
        v += 0.1 * (fbm(rng, w, h, 4) - 0.5)
        v -= 0.22 * (np.abs(np.sin(xx * 0.7 + yy * 1.1 + n * 7)) < 0.04)
        v -= 0.14 * (rng.random((h, w)) < 0.05)
    elif kind == 'leather':
        v += 0.06 * (n - 0.5)
        over = (((yy == 2) | (yy == h - 3)) & (xx % 4 < 2), second)
    elif kind == 'crystal':
        k = ((xx * 3 + yy * 5) // 9 % 4) / 3.0
        v = 0.42 + 0.34 * k + FACE_LIGHT[face]
        edge = (np.abs(fx - fy) < 0.05) | (np.abs(fx + fy - 1) < 0.05)
        v = np.where(edge, 1.0, v)
        alpha[:] = 232
    elif kind == 'cloud':
        bl = np.zeros((h, w))
        for _ in range(max(2, w * h // 90)):
            cx, cy, r = rng.random() * w, rng.random() * h, 3 + rng.random() * 6
            bl = np.maximum(bl, np.clip(1 - np.hypot(xx - cx, yy - cy) / r, 0, 1))
        v = 0.46 + 0.36 * bl + FACE_LIGHT[face] * 0.6 + (n - 0.5) * 0.06
        alpha = 215 + 40 * np.clip(bl + n * 0.5, 0, 1)
    elif kind == 'water':
        wave = np.sin(yy * 0.7 + np.sin(xx * 0.35) * 2.5 + n * 6) * 0.5 + 0.5
        v = 0.35 + 0.35 * wave + FACE_LIGHT[face] * 0.6
        foam = wave > 0.93
        over = (foam, np.array([236.0, 248, 255]))
        alpha = 165 + 75 * wave
    elif kind == 'spots':
        # dark hide with bright, glowing spots
        v += 0.08 * (n - 0.5)
        sp = fbm(rng, w, h, 3) > 0.7
        over = (sp, np.minimum(255, second * 1.1))
    elif kind == 'tread':
        v -= 0.22 * ((yy // 3 + xx // 6) % 2 == 0) + 0.05
    elif kind == 'grille':
        v -= 0.3 * (xx % 4 < 2)
    elif kind == 'decal':
        # painted panels: a bold stripe band and panel seams with bolts
        v += 0.06 * (n - 0.5)
        stripe = (fy > 0.55) & (fy < 0.78)
        v -= 0.22 * (((xx % 24) == 0) | ((yy % 20) == 0))
        bolt = ((xx % 24) == 3) & ((yy % 20) == 3)
        v = np.where(bolt, 0.85, v)
        if side: over = (stripe, second)
    elif kind == 'jelly':
        ring = np.sin(np.hypot(fx - 0.5, fy - 0.3) * 20 + n * 4) * 0.5 + 0.5
        v = 0.45 + 0.3 * ring + FACE_LIGHT[face] * 0.5
        alpha = 120 + 80 * ring
    elif kind == 'skin':
        spots = fbm(rng, w, h, 5) > 0.63
        v += 0.08 * (n - 0.5)
        over = (spots, R[3] * 0.7 + second * 0.3)
    # 'dark' and anything else: just shading and noise

    # bevel: lit top edges, shadowed bottoms and sides, so every box reads crisply
    if side and h > 3 and w > 3:
        v = v + 0.13 * (yy == 0) - 0.2 * (yy == h - 1) - 0.09 * ((xx == 0) | (xx == w - 1))
    elif face == 'top' and h > 3 and w > 3:
        v = v + 0.1 * ((yy == 0) | (xx == 0)) - 0.08 * ((yy == h - 1) | (xx == w - 1))
    d = BAYER[yy % 4, xx % 4] * 0.9
    idx = np.clip(np.floor(v * (len(R) - 1) + 0.5 + d), 0, len(R) - 1).astype(int)
    rgb = R[idx]
    if over is not None:
        m, col = over
        rgb = np.where(m[..., None], np.asarray(col, float)[None, None] if np.ndim(col) == 1 else col, rgb)
    img[y0:y0 + h, x0:x0 + w] = np.clip(rgb, 0, 255)
    al[y0:y0 + h, x0:x0 + w] = np.clip(alpha, 0, 255)


def all_parts(parts):
    for p in parts:
        yield p
        yield from all_parts(p.children)


def all_boxes(parts):
    for p in all_parts(parts):
        yield from p.boxes


def pack(model):
    boxes = sorted(all_boxes(model.parts), key=lambda b: (-(b.s[2] + b.s[1]), -(b.s[0] + b.s[2])))
    for W in (64, 128, 256):
        x = y = shelf = 0
        ok = True
        for b in boxes:
            w, h, d = b.s
            iw, ih = 2 * (w + d), d + h
            if iw > W: ok = False; break
            if x + iw > W: x = 0; y += shelf; shelf = 0
            b.uv = (x, y); x += iw; shelf = max(shelf, ih)
        H = y + shelf
        if ok:
            for Hp in (32, 64, 128, 256):
                if H <= Hp <= W: return W, Hp
    raise ValueError(f'{model.cls}: does not fit a 256x256 texture')


def paint(model):
    W, H = pack(model)
    rng = np.random.default_rng(sum(map(ord, model.cls)))
    D = DENSITY
    img = np.zeros((H * D, W * D, 3)); al = np.zeros((H * D, W * D))
    for b in all_boxes(model.parts):
        u, v = b.uv; w, h, d = b.s
        mat = model.mats[b.mat]
        faces = {'top': (u + d, v, w, d), 'bottom': (u + d + w, v, w, d), 'right': (u, v + d, d, h),
                 'front': (u + d, v + d, w, h), 'left': (u + d + w, v + d, d, h), 'back': (u + 2 * d + w, v + d, w, h)}
        for face, (fx, fy, fw, fh) in faces.items():
            paint_face(img, al, fx * D, fy * D, fw * D, fh * D, mat, face, rng)
    TEX.mkdir(parents=True, exist_ok=True)
    Image.fromarray(np.dstack([img, al]).astype(np.uint8), 'RGBA').save(TEX / f'{model.texture}.png')
    return W, H


# ================================================================================================ rest-pose geometry
def rot(rx, ry, rz):
    cx, sx, cy, sy, cz, sz = math.cos(rx), math.sin(rx), math.cos(ry), math.sin(ry), math.cos(rz), math.sin(rz)
    X = np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]]); Y = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    Z = np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]])
    return Z @ Y @ X


def lowest_point(parts, pose=None):
    low = -1e9

    def walk(p, M, T0):
        nonlocal low
        pv = np.array(p.pivot) + (pose.get(p.name, {}).get('p', 0) if pose else 0)
        R = M @ rot(*p.rot); Pp = T0 + M @ pv
        for b in p.boxes:
            for cx in (0, 1):
                for cy in (0, 1):
                    for cz in (0, 1):
                        c = Pp + R @ (np.array(b.o) + np.array(b.s) * (cx, cy, cz))
                        low = max(low, c[1])
        for ch in p.children: walk(ch, R, Pp)

    for p in parts: walk(p, np.eye(3), np.zeros(3))
    return low


def ground(model):
    """Moves the model so its lowest point rests on y = 24."""
    if not model.grounded: return
    dy = 24 - lowest_point(model.parts)
    for p in model.parts: p.pivot = (p.pivot[0], p.pivot[1] + dy, p.pivot[2])


# ================================================================================================ Java output
def jf(v):
    v = float(v)
    s = f'{v:.4f}'.rstrip('0').rstrip('.')
    return (s if s not in ('', '-0') else '0') + 'f'


CH = {'rx': ('xRot', True), 'ry': ('yRot', True), 'rz': ('zRot', True), 'px': ('x', False), 'py': ('y', False), 'pz': ('z', False)}


def java(model, W, H):
    fields, lookups, defs = [], [], []
    names = {p.name for p in all_parts(model.parts)}
    counter = [0]

    def walk(p, parent_var, path):
        counter[0] += 1
        var = f'p{counter[0]}'
        cub = 'CubeListBuilder.create()'
        for b in p.boxes:
            cub += f'.texOffs({b.uv[0]}, {b.uv[1]})' + ('.mirror()' if b.mirror else '')
            dims = f'{jf(b.o[0])}, {jf(b.o[1])}, {jf(b.o[2])}, {b.s[0]}, {b.s[1]}, {b.s[2]}'
            cub += f'.addBox({dims}, new CubeDeformation({jf(b.inflate)}))' if b.inflate else f'.addBox({dims})'
            if b.mirror: cub += '.mirror(false)'
        pose = (f'PartPose.offsetAndRotation({jf(p.pivot[0])}, {jf(p.pivot[1])}, {jf(p.pivot[2])}, {jf(p.rot[0])}, {jf(p.rot[1])}, {jf(p.rot[2])})'
                if any(p.rot) else f'PartPose.offset({jf(p.pivot[0])}, {jf(p.pivot[1])}, {jf(p.pivot[2])})')
        defs.append(f'        PartDefinition {var} = {parent_var}.addOrReplaceChild("{p.name}", {cub}, {pose});')
        fields.append(f'    final ModelPart {p.field};')
        lookups.append(f'        {p.field} = root' + ''.join(f'.getChild("{n}")' for n in path + [p.name]) + ';')
        for c in p.children: walk(c, var, path + [p.name])

    for p in model.parts: walk(p, 'root', [])

    statics, body = [], []
    kid = [0]

    def keys_const(keys):
        kid[0] += 1
        name = f'K{kid[0]}'
        flat = ', '.join(f'{jf(t)}, {jf(v)}' for t, v in sorted(keys))
        statics.append(f'    private static final float[] {name} = {{{flat}}};')
        return name

    def apply_tracks(tracks, sample, wexpr):
        out = []
        for (part, ch), keys in sorted(tracks.items()):
            if part not in names:
                if part.startswith('right_'): continue   # a mirrored copy of a one-sided part (sym)
                raise ValueError(f'{model.cls}: no part {part}')
            f = next(p.field for p in all_parts(model.parts) if p.name == part)
            k = keys_const(keys)
            val = f'Keys.{sample}({k}, ph)'
            if ch in CH:
                fld, is_rot = CH[ch]
                out.append(f'            {f}.{fld} += {"Rig.DEG * " if is_rot else ""}{val} * {wexpr};')
            elif ch == 's':
                out.append(f'            {{ float d = {val} * {wexpr}; {f}.xScale += d; {f}.yScale += d; {f}.zScale += d; }}')
            elif ch in ('sx', 'sy', 'sz'):
                out.append(f'            {f}.{ch[1]}Scale += {val} * {wexpr};')
            else:
                raise ValueError(ch)
        return out

    for c in model.clips:
        if c.kind == 'time':
            body.append(f'        {{ float w = {c.weight}; float ph = t / {jf(c.length)} + s.seed;')
            body.append('          if (w > 0.002f) {'); body += apply_tracks(c.tracks, 'loop', 'w'); body.append('        } }')
        elif c.kind == 'dist':
            body.append(f'        {{ float w = {c.weight}; float ph = s.walkAnimationPos * {jf(c.length)};')
            body.append('          if (w > 0.002f) {'); body += apply_tracks(c.tracks, 'loop', 'w'); body.append('        } }')
        elif c.kind == 'fidget':
            body.append(f'        {{ float w = {c.weight}; float ph = Rig.fidget(t, s.seed, {jf(c.length)}, {jf(c.dur)});')
            body.append('          if (ph >= 0 && w > 0.002f) {'); body += apply_tracks(c.tracks, 'once', 'w'); body.append('        } }')
        elif c.kind in ('attack', 'cast'):
            kind = 'AnimPulse.ATTACK' if c.kind == 'attack' else 'AnimPulse.CAST'
            body.append(f'        {{ float ph = Rig.pulse(s.attackAge, {jf(c.length)});')
            body.append(f'          if (ph >= 0 && s.attackKind == {kind}) {{'); body += apply_tracks(c.tracks, 'once', '1f'); body.append('        } }')
        elif c.kind == 'hurt':
            body.append(f'        {{ float ph = Rig.pulse(s.hurtAge, {jf(c.length)});')
            body.append('          if (ph >= 0) {'); body += apply_tracks(c.tracks, 'once', '1f'); body.append('        } }')
    for part, wy, wx in model.look:
        f = next(p.field for p in all_parts(model.parts) if p.name == part)
        body.append(f'        {f}.yRot += s.yRot * Rig.DEG * {jf(wy)}; {f}.xRot += s.xRot * Rig.DEG * {jf(wx)};')
    return f'''package com.stormreach.client.model.gen;

import com.stormreach.anim.AnimPulse;
import com.stormreach.client.anim.Keys;
import com.stormreach.client.anim.Rig;
import com.stormreach.client.anim.RigState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * {model.doc}
 * Generated by tools/mobmodels.py (texture textures/entity/{model.texture}.png, {W}x{H} units at {DENSITY}x). Don't edit.
 */
@SuppressWarnings("unused")
public class {model.cls} extends EntityModel<RigState> {{
{chr(10).join(statics)}

{chr(10).join(fields)}

    public {model.cls}(ModelPart root) {{
        super(root{', net.minecraft.client.renderer.rendertype.RenderTypes::entityTranslucent' if model.translucent else ''});
{chr(10).join(lookups)}
    }}

    public static LayerDefinition createBodyLayer() {{
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
{chr(10).join(defs)}
        return LayerDefinition.create(mesh, {W}, {H});
    }}

    @Override
    public void setupAnim(RigState s) {{
        super.setupAnim(s);
        float t = s.ageInTicks;
        float moveW = Rig.ease(Math.min(1f, s.walkAnimationSpeed * 2.2f)), idleW = 1f - moveW;
        float air = s.air, ground = 1f - air, swim = s.isInWater ? 1f : 0f, dry = 1f - swim;
        float turn = Mth.clamp(s.turn, -12f, 12f);
{chr(10).join(body)}
{model.extra}
    }}
}}
'''


# ================================================================================================ the creatures
M = []


# ---- Cliff Crab and Thunder Crab ------------------------------------------------------------------------------
def crab(cls, tex, k, mats, thunder, doc):
    S = lambda v: v * k
    Bx = lambda x, y, z, w, h, d, m: Box(S(x), S(y), S(z), S(w), S(h), S(d), m)
    body = P('body', (0, 18, 0), boxes=[Bx(-5, -2, -4, 10, 4, 8, 'shell'), Bx(-4, -3, -3, 8, 1, 6, 'shell2'),
                                       Bx(-4.5, 2, -3.5, 9, 1, 7, 'belly'), Bx(-2, -1, -4.5, 4, 2, 1, 'mouth'),
                                       Bx(4.5, -2.5, -2, 1, 1, 1, 'shell2'), Bx(-5.5, -2.5, -2, 1, 1, 1, 'shell2'),
                                       Bx(4.5, -2.5, 1, 1, 1, 1, 'shell2'), Bx(-5.5, -2.5, 1, 1, 1, 1, 'shell2')])
    body.add(*pair(P('left_mandible', (S(1), S(1), -S(4.5)), boxes=[Bx(-0.5, -0.5, -1, 1, 1, 1, 'claw2')])))
    body.add(*pair(P('left_eye', (S(2), -S(3), -S(3)), boxes=[Bx(-0.5, -3, -0.5, 1, 3, 1, 'shell2'), Bx(-1, -5, -1, 2, 2, 2, 'eye')])))
    claw = P('left_arm', (S(5), S(0.5), -S(2.5)), (0, -0.5, 0.15), [Bx(0, -1, -1, 3, 2, 2, 'shell')]).add(
        P('left_forearm', (S(3), 0, 0), (0, -0.7, 0), [Bx(0, -1, -1, 3, 2, 2, 'shell')]).add(
            P('left_hand', (S(3), 0, 0), (0, 1.1, 0), [Bx(-1.5, -1.5, -4, 3, 3, 4, 'claw'), Bx(-1, 0.5, -7, 2, 1, 3, 'claw2')]).add(
                P('left_pincer', (0, -S(1.5), -S(4)), boxes=[Bx(-1, 0, -3, 2, 1, 3, 'claw2')]))))
    body.add(*pair(claw))
    for i in range(3):
        leg = P(f'left_leg{i}', (S(4.5), S(1), S(-2 + i * 2.2)), (0, (i - 1) * -0.35, 0.45), [Bx(0, -0.5, -0.5, 3, 1, 1, 'shell2')]).add(
            P(f'left_shin{i}', (S(3), 0, 0), (0, 0, 0.7), [Bx(0, -0.5, -0.5, 3, 1, 1, 'shell2')]).add(
                P(f'left_foot{i}', (S(3), 0, 0), (0, 0, 0.35), [Bx(0, -0.5, -0.5, 2, 1, 1, 'dark')])))
        body.add(*pair(leg))
    if thunder:
        for i, (x, z) in enumerate(((-3, -2), (0, -1), (3, -2), (-2, 2), (2, 2))):
            body.add(P(f'spine{i}', (S(x), -S(3), S(z)), (0.15 * (z < 0) - 0.15 * (z > 0), 0, 0.2 * (x / 3)),
                       [Box(-1, -S(3), -1, 2, S(3), 2, 'crystal')]))
        body.add(P('antenna', (0, -S(3), S(1)), boxes=[Box(-0.5, -S(5), -0.5, 1, S(5), 1, 'metal'), Box(-1, -S(5) - 2, -1, 2, 2, 2, 'glow')]))
    legs = {}
    for i in range(3):
        for side, ph in (('left', 0.0 if i != 1 else 0.5), ('right', 0.5 if i != 1 else 0.0)):
            sg = 1 if side == 'left' else -1
            legs[(f'{side}_leg{i}', 'ry')] = shift([(0, 18 * sg), (0.5, -18 * sg)], ph)
            legs[(f'{side}_leg{i}', 'rz')] = shift([(0, 0), (0.45, 0), (0.75, -26 * sg)], ph)
            legs[(f'{side}_shin{i}', 'rz')] = shift([(0, 0), (0.45, 0), (0.75, 14 * sg)], ph)
    clips = [
        Clip('time', 50, sym(T(body_py=[(0, 0), (0.5, 0.25)], left_eye_rz=even(0, 7, 0, -5), left_arm_rx=even(0, -4, 0, 3),
                                left_mandible_rz=even(0, 18, 0, -6, 12, 0))), 'idleW'),
        Clip('dist', 0.21 / max(k, 1) ** 0.5, merge(legs, T(body_rz=even(0, 2.5, 0, -2.5), body_py=even(0, -0.3, 0, -0.3),
                                                       left_arm_rz=hold(-12), right_arm_rz=hold(12))), 'moveW'),
        Clip('fidget', 150, sym(T(left_pincer_rx=[(0, 0), (0.12, -38), (0.22, 12), (0.32, -32), (0.42, 12), (0.55, 0), (1, 0)],
                                  left_arm_rz=[(0, 0), (0.12, -28), (0.6, -26), (0.85, 0), (1, 0)],
                                  left_eye_py=[(0, 0), (0.1, -0.5), (0.7, -0.5), (1, 0)])), 'idleW', dur=36),
        Clip('attack', 14, merge(sym(T(left_arm_ry=[(0, 0), (0.18, 28), (0.32, -12), (1, 0)], left_arm_rz=[(0, 0), (0.15, -30), (0.5, -10), (1, 0)],
                                       left_pincer_rx=[(0, 0), (0.12, -42), (0.28, 14), (0.6, 0), (1, 0)])),
                                 T(body_pz=[(0, 0), (0.22, -S(1.3)), (1, 0)], body_rx=[(0, 0), (0.2, 6), (1, 0)]))),
        hurt_clip('body', None),
        Clip('hurt', 10, sym(T(left_eye_py=[(0, 0), (0.15, 2), (0.7, 1), (1, 0)]))),
    ]
    extra = '        body.yRot -= turn * 0.02f;\n'
    if thunder:
        clips.append(Clip('time', 18, T(antenna_rz=even(0, 6, -3, 4, -6, 2), antenna_rx=even(0, -3, 4, 0, -2, 3)), '1f'))
    M.append(Model(cls, tex, mats, [body], clips, doc=doc, extra=extra))


crab('CliffCrabModel', 'cliff_crab', 1, {'shell': ('shell', C('#b5522f'), C('#6e2a16')), 'shell2': ('shell', C('#cf6a38'), C('#7a3418')),
     'belly': ('skin', C('#e8c79e'), C('#c49a6c')), 'mouth': ('dark', C('#4a1e12')), 'claw': ('claw', C('#c95f32'), C('#5c2410')),
     'claw2': ('claw', C('#e6d2b8'), C('#a08060')), 'eye': ('eye', C('#e8c040'), C('#2a1208')), 'dark': ('dark', C('#4a2014'))}, False,
     'A shore crab: ridged shell, three-jointed legs that scuttle in a tripod gait, swaying eye stalks, chewing mandibles, and claws that snap.')
crab('ThunderCrabModel', 'thunder_crab', 1.5, {'shell': ('shell', C('#2f4a66'), C('#16243a')), 'shell2': ('shell', C('#3f6488'), C('#1c3048')),
     'belly': ('skin', C('#9fb6c8'), C('#6a8296')), 'mouth': ('dark', C('#0c1420')), 'claw': ('claw', C('#355878'), C('#121e30')),
     'claw2': ('metal', C('#b8c4cc'), C('#606a72')), 'eye': ('eye', C('#7fd4ff'), C('#10202c')), 'crystal': ('crystal', C('#7fd4ff'), C('#2a6a96')),
     'metal': ('metal', C('#9aa4ae'), C('#4a525a')), 'glow': ('glow', C('#e0f8ff'), C('#3a8ac0')), 'dark': ('dark', C('#14202e'))}, True,
     'The Thunder Crab: a storm-charged giant crab with crystal spines and a twitching lightning antenna.')


# ---- Storm Gull and Stormhawk --------------------------------------------------------------------------------
def bird(cls, tex, mats, k, raptor, doc):
    S = lambda v: v * k
    Bx = lambda x, y, z, w, h, d, m: Box(S(x), S(y), S(z), S(w), S(h), S(d), m)
    body = P('body', (0, 17, 0), boxes=[Bx(-2, -2, -4, 4, 4, 7, 'feather'), Bx(-1.5, -1, -5, 3, 3, 2, 'belly'),
                                       Bx(-1.5, -2.5, -3, 3, 1, 5, 'feather2'), Bx(-1.5, 1.5, -3, 3, 1, 5, 'belly')])
    head = P('head', (0, -S(2.5), 0), boxes=[Bx(-1.5, -3, -2.5, 3, 3, 4, 'feather2')]).add(
        *pair(P('left_eye', (S(1.5), -S(2), -S(1)), boxes=[Box(-0.3, -0.5, -0.5, 1, 1, 1, 'eye')])),
        P('beak', (0, -S(1.5), -S(2.5)), boxes=[Bx(-0.5, -0.5, -3, 1, 1, 3, 'beak')] ).add(
            *([P('beak_hook', (0, 0, -S(3)), (0.7, 0, 0), [Bx(-0.5, -0.5, -1, 1, 1, 1, 'beak')])] if raptor else [])),
        P('jaw', (0, -S(1.0) + 0.02, -S(2.5)), boxes=[Bx(-0.5, 0, -2.5, 1, 1, 2.5, 'beak2')]))
    if raptor:
        head.add(P('crest', (0, -S(3), S(0.5)), (-0.7, 0, 0), [Bx(-0.5, -3, 0, 1, 3, 2, 'accent')]),
                 P('crest2', (0, -S(3), S(1.2)), (-0.35, 0, 0), [Bx(-0.5, -2, 0, 1, 2, 1, 'accent')]))
    neck = P('neck', (0, -S(1.5), -S(4)), (0.2, 0, 0), [Bx(-1, -2.5, -1.5, 2, 3, 2, 'feather2')]).add(head)
    body.add(neck)
    tip = P('left_wing_tip', (S(4), 0, 0), boxes=[Bx(0, -0.5, 0, 4, 1, 3, 'tip')])
    for i, (z, ry) in enumerate(((0.2, 0.15), (1.2, -0.1), (2.2, -0.35))):
        tip.add(P(f'left_primary{i}', (S(3.6), 0, S(z)), (0, ry, 0), [Box(0, -0.45, 0, S(3), 1, 1, 'tip')]))
    wing = P('left_wing', (S(2), -S(1.5), -S(2.5)), boxes=[Bx(0, -0.5, 0, 4, 1, 5, 'feather')]).add(
        P('left_wing_mid', (S(4), 0, S(0.5)), boxes=[Bx(0, -0.5, 0, 4, 1, 4, 'feather')]).add(tip))
    body.add(*pair(wing))
    tail = P('tail', (0, -S(1), S(3)), (-0.15, 0, 0), [Bx(-1, -0.5, 0, 2, 1, 4, 'tail')]).add(
        *pair(P('left_tail', (S(0.5), 0, 0), (0, 0.3, 0), [Box(0, -0.4, 0, S(1), 1, S(4), 'tail')])))
    body.add(tail)
    leg = P('left_leg', (S(1), S(2), S(0.5)), boxes=[Bx(-0.5, -0.5, -0.5, 1, 2, 1, 'feather')]).add(
        P('left_shank', (0, S(1.5), 0), boxes=[Bx(-0.5, 0, -0.5, 1, 3, 1, 'leg')]).add(
            P('left_foot', (0, S(3), 0), boxes=[Bx(-1, 0, -2, 2, 1, 3, 'leg')])))
    body.add(*pair(leg))
    beat = 18 if raptor else 12
    fly = sym(T(left_wing_rz=[(0, -36), (0.4, 26), (0.55, 20), (0.8, -24)], left_wing_mid_rz=[(0.08, -12), (0.48, 10), (0.85, -8)],
                left_wing_tip_rz=[(0.15, -14), (0.55, 13), (0.92, -10)], left_wing_ry=[(0.1, 4), (0.6, -6)],
                body_py=[(0.05, 0.6), (0.45, -0.7)], body_rx=hold(-6), left_leg_rx=hold(70), left_shank_rx=hold(30),
                left_tail_ry=hold(12), tail_rx=[(0.1, 4), (0.6, -4)], neck_rx=hold(-12)))
    fold = sym(T(left_wing_ry=hold(-82), left_wing_rz=hold(12), left_wing_mid_ry=hold(-12), left_wing_tip_ry=hold(-8),
                 left_wing_mid_rz=hold(-6), left_tail_ry=hold(-10)))
    clips = [
        Clip('time', 70, fold, 'ground'),
        Clip('time', 70, T(body_py=[(0, 0), (0.5, 0.25)], neck_rx=even(0, 3, 0, -2), tail_rx=even(0, 3, -2, 0)), 'idleW * ground'),
        Clip('dist', 0.2, merge(sym(gait_leg('left_leg', 'left_shank', 0, 30, 35)), gait_leg('right_leg', 'right_shank', 0.5, 30, 35),
                                T(body_rz=even(5, -5), body_py=even(0, -0.35, 0, -0.35), neck_pz=even(0, -S(0.9), 0, -S(0.9)),
                                  tail_ry=even(-6, 6))), 'moveW * ground'),
        Clip('time', beat, fly, 'air'),
        Clip('fidget', 190, sym(T(neck_rx=[(0, 0), (0.2, -38), (0.7, -36), (1, 0)], jaw_rx=[(0, 0), (0.25, 32), (0.35, 8), (0.45, 32), (0.55, 8), (0.7, 0)],
                                  left_wing_rz=[(0, 0), (0.25, -22), (0.6, -22), (1, 0)], left_wing_ry=[(0, 0), (0.25, 20), (0.6, 20), (1, 0)])),
             'idleW * ground', dur=34),
        Clip('fidget', 120, T(head_rz=[(0, 0), (0.2, 25), (0.45, 25), (0.65, -10), (1, 0)], head_ry=[(0, 0), (0.3, -15), (0.7, 10), (1, 0)]),
             'idleW * ground', dur=24),
        hurt_clip('body', 'head', 1.2),
    ]
    if raptor:
        clips.append(Clip('attack', 16, sym(T(left_leg_rx=[(0, 0), (0.15, -75), (0.45, -70), (1, 0)], left_foot_rx=[(0, 0), (0.15, -30), (0.3, 20), (1, 0)],
                                              left_wing_rz=[(0, 0), (0.15, -50), (0.45, 20), (1, 0)], neck_rx=[(0, 0), (0.2, 28), (0.5, 0)],
                                              jaw_rx=[(0, 0), (0.12, 35), (0.4, 30), (0.7, 0)], body_rx=[(0, 0), (0.15, -20), (0.5, 10), (1, 0)]))))
    look = [('neck', 0.35, 0.3), ('head', 0.65, 0.7)]
    extra = ('        body.zRot -= turn * 0.035f * air;\n        tail.yRot += turn * 0.03f;\n'
             '        if (s.vy < -0.05f && air > 0.5f) { float g = Math.min(1f, -s.vy * 4f) * air; leftWing.zRot += 0.35f * g; rightWing.zRot -= 0.35f * g; }\n')
    M.append(Model(cls, tex, mats, [body], clips, doc=doc, look=look, extra=extra))


bird('StormGullModel', 'storm_gull', {'feather': ('feather', C('#9aa6b2'), C('#5a6670')), 'feather2': ('feather', C('#eef2f6'), C('#b0bcc6')),
     'belly': ('feather', C('#f8fafc'), C('#c8d0d8')), 'beak': ('shell', C('#f2b13c'), C('#b0701c')), 'beak2': ('shell', C('#e8742c'), C('#a04010')),
     'eye': ('eye', C('#f0e6b0'), C('#202020')), 'tail': ('feather', C('#f2f4f6'), C('#9aa6b0')), 'tip': ('feather', C('#2e343c'), C('#f0f2f4')),
     'leg': ('skin', C('#f0a848'), C('#b0701c'))}, 1, False,
     'The Storm Gull: folds its wings on the ground and flaps with real down- and up-strokes in the air; it waddles, squawks and glances about.')
bird('StormhawkModel', 'stormhawk', {'feather': ('feather', C('#4e3f80'), C('#241c44')), 'feather2': ('feather', C('#6e5ca8'), C('#2c2448')),
     'belly': ('feather', C('#c8c0e0'), C('#7a70a0')), 'beak': ('shell', C('#f2c94c'), C('#8a6a1e')), 'beak2': ('shell', C('#d8a83a'), C('#7a5a14')),
     'eye': ('eye', C('#ffd24a'), C('#1a1420')), 'tail': ('feather', C('#3a3060'), C('#f2c94c')), 'accent': ('feather', C('#f2c94c'), C('#a07a20')),
     'tip': ('feather', C('#7a68c0'), C('#f2c94c')), 'leg': ('scale', C('#e8b840'), C('#8a6a1e'))}, 1.4, True,
     'The Stormhawk: a violet raptor with a hooked beak and golden crest; it strikes with its talons swung forward and wings flared.')


# ---- Harbor Seal ---------------------------------------------------------------------------------------------
seal = P('body', (0, 19, 0), boxes=[Box(-4, -3, -5, 8, 6, 7, 'skin'), Box(-3.5, 2.5, -4.5, 7, 1, 6, 'belly')]).add(
    P('rear', (0, 0, 2), boxes=[Box(-3.5, -2.5, 0, 7, 5, 5, 'skin'), Box(-3, 2, 0.5, 6, 1, 4, 'belly')]).add(
        P('rear2', (0, 0.5, 5), boxes=[Box(-2.5, -2, 0, 5, 4, 4, 'skin')]).add(
            P('tail', (0, 0.5, 4), boxes=[Box(-1, -0.5, 0, 2, 1, 1, 'dark')]).add(
                *pair(P('left_fluke', (0.5, 0, 0.5), (0, 0.45, 0), [Box(0, -0.5, 0, 2, 1, 3, 'dark')]))))),
    P('head', (0, -2, -5), boxes=[Box(-3, -3, -4, 6, 5, 5, 'skin'), Box(-2, -1, -6, 4, 3, 2, 'snout'), Box(-1, -1.3, -6.4, 2, 1, 1, 'nose'),
                                  Box(1, -2, -4.3, 1, 1, 1, 'eye'), Box(-2, -2, -4.3, 1, 1, 1, 'eye'),
                                  Box(2, -0.5, -5.5, 1, 1, 1, 'whisker'), Box(-3, -0.5, -5.5, 1, 1, 1, 'whisker')]).add(
        P('jaw', (0, 1.5, -4), boxes=[Box(-1.5, 0, -2, 3, 1, 2, 'snout')])))
seal.add(*pair(P('left_flipper', (4, 2, -3), (0, 0.3, 0.6), [Box(0, -0.5, -1.5, 3, 1, 3, 'dark')]).add(
    P('left_flipper_tip', (3, 0, 0), (0, 0, 0.2), [Box(0, -0.5, -1.5, 2, 1, 3, 'dark')]))))
M.append(Model('HarborSealModel', 'harbor_seal', {'skin': ('skin', C('#8a8e94'), C('#4a4e56')), 'belly': ('skin', C('#c4c6c8'), C('#9a9ca0')),
    'dark': ('skin', C('#5a5e66'), C('#3a3e44')), 'snout': ('skin', C('#a6a8ac'), C('#6a6c70')), 'whisker': ('bone', C('#e8e8e0')),
    'nose': ('dark', C('#1a1a1e')), 'eye': ('eye', C('#1a1a1a'), C('#101010'))}, [seal], [
    Clip('time', 90, sym(T(body_py=[(0, 0), (0.5, 0.2)], rear_ry=even(0, 4, 0, -4), left_fluke_ry=even(0, 12, 0, -6),
                           left_flipper_rz=even(0, 6, 0, -3), rear2_ry=shift(even(0, 5, 0, -5), 0.1))), 'idleW * dry'),
    Clip('dist', 0.17, merge(T(body_rx=[(0, -7), (0.5, 7)], body_py=[(0, -0.7), (0.3, 0.1), (0.5, 0.5), (0.8, 0)],
                               rear_rx=shift([(0, 9), (0.5, -9)], 0.2), rear2_rx=shift([(0, 8), (0.5, -8)], 0.35),
                               tail_rx=shift([(0, 15), (0.5, -12)], 0.5), head_rx=shift([(0, 5), (0.5, -5)], 0.1)),
                             sym(T(left_flipper_ry=[(0, 30), (0.45, -35), (0.7, -20)], left_flipper_rz=[(0, 0.0), (0.45, -15), (0.75, 10)]))),
         'moveW * dry'),
    Clip('time', 28, merge(T(rear_ry=[(0, -16), (0.5, 16)], rear2_ry=shift([(0, -18), (0.5, 18)], 0.12), tail_ry=shift([(0, -22), (0.5, 22)], 0.24),
                             body_ry=shift([(0, 5), (0.5, -5)], 0.4)), sym(T(left_flipper_ry=hold(-55), left_flipper_rz=hold(-10)))), 'swim'),
    Clip('fidget', 220, sym(T(left_flipper_ry=[(0, 0), (0.15, -55), (0.25, -30), (0.35, -55), (0.45, -30), (0.55, -55), (0.8, 0)],
                              head_rx=[(0, 0), (0.15, -28), (0.7, -26), (1, 0)], jaw_rx=[(0, 0), (0.6, 0), (0.66, 30), (0.74, 4), (0.8, 30), (0.9, 0)])),
         'idleW * dry', dur=44),
    hurt_clip('body', 'head')],
    doc='The Harbor Seal: a three-part body that humps along on land and sways side to side in water, flipper claps and a bark.',
    look=[('head', 1.0, 1.0)], extra='        rear.yRot -= turn * 0.03f;\n        rear2.yRot -= turn * 0.04f;\n'))


# ---- Squall --------------------------------------------------------------------------------------------------
squall = P('core', (0, 12, 0), boxes=[Box(-3, -3, -3, 6, 6, 6, 'glow'), Box(-2, -1.5, -3.3, 1, 1, 1, 'face'), Box(1, -1.5, -3.3, 1, 1, 1, 'face'),
                                      Box(-1, 0.5, -3.3, 2, 1, 1, 'face')])
for r, (y, rad, n) in enumerate(((-4, 6, 5), (1, 8, 7), (6, 5, 4))):
    ring = P(f'ring{r}', (0, y, 0))
    for i in range(n):
        a = i * 2 * math.pi / n
        ring.add(P(f'puff{r}_{i}', (math.cos(a) * rad, 0, math.sin(a) * rad), (0, -a, 0),
                   [Box(-2, -1.5, -1.5, 4, 3, 3, 'cloud'), Box(-1, -2.5, -1, 2, 1, 2, 'cloud2')]))
    squall.add(ring)
squall.add(P('wisp0', (0, 3, 0), boxes=[Box(-2, 0, -2, 4, 3, 4, 'cloud')]).add(
    P('wisp1', (0, 3, 0), boxes=[Box(-1.5, 0, -1.5, 3, 3, 3, 'cloud')]).add(
        P('wisp2', (0, 3, 0), boxes=[Box(-1, 0, -1, 2, 3, 2, 'cloud2')]).add(P('wisp3', (0, 3, 0), boxes=[Box(-0.5, 0, -0.5, 1, 2, 1, 'cloud2')])))))
M.append(Model('SquallModel', 'squall', {'glow': ('glow', C('#f0faff'), C('#7fb0d0')), 'face': ('dark', C('#1a3448')),
    'cloud': ('cloud', C('#c0ccd6'), C('#8a98a4')), 'cloud2': ('cloud', C('#e2e8ee'), C('#9aa6b0'))}, [squall], [
    Clip('time', 40, T(core_py=[(0, 0), (0.5, -1.2)], core_s=[(0, 0), (0.5, 0.06)], wisp0_rz=even(0, 10, 0, -10), wisp1_rz=shift(even(0, 14, 0, -14), 0.12),
                       wisp2_rz=shift(even(0, 18, 0, -18), 0.24), wisp3_rz=shift(even(0, 24, 0, -24), 0.36), wisp1_rx=shift(even(0, 10, 0, -10), 0.3)), '1f'),
    Clip('attack', 15, T(core_pz=[(0, 0), (0.08, 1), (0.22, -3), (1, 0)], core_sz=[(0, 0), (0.08, -0.15), (0.22, 0.3), (0.45, -0.1), (0.7, 0)],
                         core_sx=[(0, 0), (0.08, 0.12), (0.22, -0.15), (0.45, 0.06), (0.7, 0)], ring1_ry=[(0, 0), (1, 220)], ring0_ry=[(0, 0), (1, -260)],
                         wisp0_rx=[(0, 0), (0.25, 40), (0.6, -10), (1, 0)], wisp1_rx=[(0, 0), (0.3, 35), (0.7, -10), (1, 0)])),
    Clip('hurt', 10, T(core_s=[(0, 0), (0.15, -0.2), (0.4, 0.08), (1, 0)], core_rz=[(0, 0), (0.15, 15), (0.5, -5), (1, 0)]))],
    doc='The Squall: a glowing storm-core with a face, in three counter-spinning cloud rings, trailing a four-jointed wispy tail; it squashes and lunges when it rams.',
    grounded=False, extra='        ring0.yRot += t * 0.3f;\n        ring1.yRot -= t * 0.22f;\n        ring2.yRot += t * 0.4f;\n        wisp0.zRot -= turn * 0.04f;\n'))


# ---- Stormwarden ---------------------------------------------------------------------------------------------
hips = P('hips', (0, 4, 0), boxes=[Box(-6, -2, -3.5, 12, 4, 7, 'stone')])
torso = P('torso', (0, -2, 0), boxes=[Box(-7, -11, -4, 14, 11, 8, 'stone'), Box(-6, -10, -4.6, 12, 6, 1, 'metal'), Box(-2, -8, -5.2, 4, 4, 1, 'glow')])
for i, (x, z, rx, rz) in enumerate(((-4, 2, 0.5, -0.3), (0, 3, 0.7, 0), (4, 2, 0.5, 0.3))):
    torso.add(P(f'back_crystal{i}', (x, -9, z), (rx, 0, rz), [Box(-1, -5, -1, 2, 5, 2, 'crystal')]))
head = P('head', (0, -11, -1), boxes=[Box(-4, -7, -4, 8, 7, 8, 'stone'), Box(-4.5, -5.5, -4.6, 9, 1, 2, 'metal'), Box(-3, -4, -4.4, 6, 2, 1, 'glow'),
                                      Box(-3, -1.5, -4.6, 6, 2, 2, 'stone2')])
for i, (x, z, rx, rz, hgt) in enumerate(((-3, 0, 0, -0.35, 4), (3, 0, 0, 0.35, 4), (0, 1.5, 0.35, 0, 5), (-1.5, -2, -0.2, -0.15, 3), (1.5, -2, -0.2, 0.15, 3))):
    head.add(P(f'crown{i}', (x, -7, z), (rx, 0, rz), [Box(-1, -hgt, -1, 2, hgt, 2, 'crystal')]))
torso.add(head)
arm = P('left_arm', (8, -9, 0), boxes=[Box(-2, -3, -3.5, 6, 4, 7, 'metal')]).add(
    P('left_spike', (2, -3, 0), (0, 0, 0.4), [Box(-1, -4, -1, 2, 4, 2, 'crystal')]),
    P('left_upper', (1, 1, 0), boxes=[Box(-2.5, 0, -2.5, 5, 8, 5, 'stone')]).add(
        P('left_fore', (0, 8, 0), boxes=[Box(-3, 0, -3, 6, 8, 6, 'stone'), Box(-3.5, 4, -3.5, 7, 3, 7, 'metal')]).add(
            P('left_fist', (0, 8, 0), boxes=[Box(-3, 0, -3, 6, 5, 6, 'stone2')]))))
torso.add(*pair(arm))
hips.add(torso)
leg = P('left_leg', (3.5, 6, 0), boxes=[Box(-3, 0, -3, 6, 8, 6, 'stone')]).add(
    P('left_shin', (0, 8, 0), boxes=[Box(-3, 0, -3, 6, 8, 6, 'stone'), Box(-3.5, 0, -3.6, 7, 3, 1, 'metal')]).add(
        P('left_foot', (0, 8, -0.5), boxes=[Box(-3.5, 0, -4.5, 7, 3, 8, 'metal')])))
wslam = [(0, 0), (0.28, -150), (0.4, -165), (0.5, -15), (0.62, -8), (1, 0)]
M.append(Model('StormwardenModel', 'stormwarden', {'stone': ('stone', C('#5a6270'), C('#8fe0ff')), 'stone2': ('stone', C('#4a505c'), C('#8fe0ff')),
    'glow': ('glow', C('#d8f6ff'), C('#3a8ac0')), 'crystal': ('crystal', C('#7fd4ff'), C('#2a6a96')), 'metal': ('metal', C('#8a9298'), C('#40464c'))},
    [hips, *pair(leg)], [
    Clip('time', 80, sym(T(torso_rx=[(0, 0), (0.5, -2.5)], torso_py=[(0, 0), (0.5, -0.3)], left_arm_rx=even(0, 3, 0, -2), left_arm_rz=even(-4, -6),
                           left_fore_rx=even(-8, -12), head_rz=even(0, 2, 0, -2))), 'idleW'),
    Clip('dist', 0.12, merge(sym(gait_leg('left_leg', 'left_shin', 0, 24, 40)), gait_leg('right_leg', 'right_shin', 0.5, 24, 40),
                             T(left_foot_rx=shift([(0, 12), (0.3, 0), (0.6, -14), (0.8, 10)], 0), right_foot_rx=shift([(0, 12), (0.3, 0), (0.6, -14), (0.8, 10)], 0.5),
                               hips_py=even(0, 1.0, 0, 1.0), hips_rz=even(3, -3), torso_ry=even(-7, 7), head_ry=even(5, -5),
                               left_arm_rx=[(0, 22), (0.5, -22)], right_arm_rx=[(0, -22), (0.5, 22)],
                               left_fore_rx=shift([(0, -10), (0.5, -28)], 0.1), right_fore_rx=shift([(0, -28), (0.5, -10)], 0.1))), 'moveW'),
    Clip('attack', 32, merge(sym(T(left_arm_rx=wslam, left_arm_rz=[(0, 0), (0.28, -12), (0.5, 8), (1, 0)], left_fore_rx=[(0, 0), (0.28, -35), (0.5, 0), (1, 0)])),
                             T(torso_rx=[(0, 0), (0.28, -16), (0.5, 26), (0.72, 24), (1, 0)], hips_py=[(0, 0), (0.28, -0.5), (0.5, 2.8), (0.72, 2.6), (1, 0)],
                               left_leg_rx=[(0, 0), (0.5, -18), (0.72, -18), (1, 0)], right_leg_rx=[(0, 0), (0.5, -18), (0.72, -18), (1, 0)],
                               left_shin_rx=[(0, 0), (0.5, 36), (0.72, 36), (1, 0)], right_shin_rx=[(0, 0), (0.5, 36), (0.72, 36), (1, 0)],
                               head_rx=[(0, 0), (0.28, -20), (0.5, 15), (1, 0)]))),
    Clip('cast', 40, sym(T(left_arm_rz=[(0, 0), (0.3, -72), (0.7, -76), (1, 0)], left_arm_rx=[(0, 0), (0.3, -40), (0.7, -45), (1, 0)],
                           head_rx=[(0, 0), (0.25, -28), (0.75, -28), (1, 0)], torso_rx=[(0, 0), (0.3, -10), (0.75, -10), (1, 0)]))),
    hurt_clip('torso', 'head', 0.7)],
    doc='The Stormwarden: a towering storm-stone golem with a crystal crown and back, a glowing visor and core; heavy heel-to-toe steps, an overhead slam and a summoning roar.',
    look=[('head', 0.8, 0.8), ('torso', 0.2, 0.0)], extra='        hips.yRot -= turn * 0.015f;\n'))


# ---- Ball Lightning and Static Wisp --------------------------------------------------------------------------
orb = P('core', (0, 16, 0), boxes=[Box(-3, -3, -3, 6, 6, 6, 'glow'), Box(-4, -2, -2, 8, 4, 4, 'glow2'), Box(-2, -4, -2, 4, 8, 4, 'glow2'),
                                   Box(-2, -2, -4, 4, 4, 8, 'glow2')])
for i in range(3):
    arc = P(f'arc{i}', (0, 0, 0), (i * 1.0, i * 0.7, 0))
    for j in range(6):
        a = j * math.pi / 3
        arc.add(P(f'bolt{i}_{j}', (math.cos(a) * 6, 0, math.sin(a) * 6), (0, -a, 0.35 * (1 if j % 2 else -1)),
                  [Box(-1.5, -0.5, -0.5, 2, 1, 1, 'spark'), Box(0.3, -1.3, -0.5, 1, 1, 1, 'spark')]))
    orb.add(arc)
M.append(Model('BallLightningModel', 'ball_lightning', {'glow': ('glow', C('#ffffff'), C('#9fe0ff')), 'glow2': ('glow', C('#c8eeff'), C('#4a9ad0')),
    'spark': ('glow', C('#fff6c0'), C('#c0e8ff'))}, [orb], [
    Clip('time', 7, T(core_px=[(0, 0), (0.2, 0.4), (0.45, -0.3), (0.7, 0.2)], core_pz=[(0, 0), (0.3, -0.3), (0.55, 0.4), (0.8, -0.1)],
                      core_s=[(0, 0), (0.2, 0.12), (0.35, -0.05), (0.6, 0.15), (0.8, 0)]), '1f'),
    Clip('time', 40, T(core_py=[(0, 0), (0.5, -0.8)]), '1f'),
    Clip('attack', 10, T(core_s=[(0, 0), (0.25, 0.6), (1, 0)])), Clip('cast', 10, T(core_s=[(0, 0), (0.25, 0.6), (1, 0)])),
    Clip('hurt', 10, T(core_s=[(0, 0), (0.2, -0.3), (1, 0)]))],
    doc='Ball Lightning: a white-hot, jittering core in three spinning arcs of zig-zag sparks.', grounded=False,
    extra='        core.yRot += t * 0.2f;\n        arc0.yRot += t * 0.6f; arc1.xRot += t * 0.5f; arc2.zRot += t * 0.7f;\n'))

wisp = P('core', (0, 14, 0), boxes=[Box(-2.5, -2.5, -2.5, 5, 5, 5, 'glow'), Box(-1.5, -1, -2.8, 1, 1, 1, 'face'), Box(0.5, -1, -2.8, 1, 1, 1, 'face')])
wtr = {}
for i in range(7):
    a = i * 2 * math.pi / 7
    tier = i % 2
    wisp.add(P(f'spike{i}', (math.cos(a) * 2.3, -2.3 + tier * 1.5, math.sin(a) * 2.3), (math.sin(a) * (0.55 + 0.4 * tier), 0, -math.cos(a) * (0.55 + 0.4 * tier)),
               [Box(-0.5, -4 + tier, -0.5, 1, 4 - tier, 1, 'spark')]))
    wtr[(f'spike{i}', 'sy')] = shift([(0, 0), (0.15, 0.35), (0.3, -0.1), (0.5, 0)], i / 7)
wisp.add(P('trail0', (0, 2.5, 0), boxes=[Box(-1.5, 0, -1.5, 3, 3, 3, 'glow2')]).add(
    P('trail1', (0, 3, 0), boxes=[Box(-1, 0, -1, 2, 3, 2, 'glow2')]).add(P('trail2', (0, 3, 0), boxes=[Box(-0.5, 0, -0.5, 1, 3, 1, 'spark')]))))
flare = {(f'spike{i}', 'sy'): [(0, 0), (0.2, 0.8), (0.5, 0.3), (1, 0)] for i in range(7)}
M.append(Model('StaticWispModel', 'static_wisp', {'glow': ('glow', C('#fff2a8'), C('#d0a020')), 'glow2': ('glow', C('#ffe070'), C('#a07010')),
    'spark': ('glow', C('#ffffff'), C('#ffe070')), 'face': ('dark', C('#3a2a00'))}, [wisp], [
    Clip('time', 30, merge(T(core_py=[(0, 0), (0.5, -1.4)], trail0_rx=even(0, 18, 0, -18), trail1_rx=shift(even(0, 24, 0, -24), 0.15),
                             trail2_rx=shift(even(0, 30, 0, -30), 0.3), trail0_rz=shift(even(0, 12, 0, -12), 0.25)), wtr), '1f'),
    Clip('cast', 14, merge(flare, T(core_s=[(0, 0), (0.2, 0.35), (1, 0)]))), Clip('attack', 14, merge(flare, T(core_s=[(0, 0), (0.2, 0.35), (1, 0)]))),
    Clip('hurt', 10, T(core_s=[(0, 0), (0.2, -0.3), (1, 0)]))],
    doc='The Static Wisp: a golden spark with a flickering two-tier crown of spikes and a whipping tail; the spikes flare when it zaps.',
    grounded=False, extra='        core.yRot += t * 0.15f;\n'))


# ---- Waterspout (mob) and Cyclone ----------------------------------------------------------------------------
def spout(cls, tex, mats, attack, doc):
    parts, tr = [], {}
    for i in range(6):
        rad = 2 + i * 1.3
        ring = P(f'ring{i}', (0, 24 - i * 4, 0))
        n = 6 + i
        for j in range(n):
            a = j * 2 * math.pi / n
            ring.add(P(f'seg{i}_{j}', (math.cos(a) * rad, 0, math.sin(a) * rad), (0, -a, 0.12), [Box(-1.5, -2, -0.5, 3, 4, 1, 'a' if (i + j) % 2 else 'b')]))
        if i == 3:
            for j in range(3):
                a = j * 2.1
                ring.add(P(f'debris{j}', (math.cos(a) * (rad + 1.5), -1, math.sin(a) * (rad + 1.5)), (0.4 * j, a, 0.3), [Box(-0.5, -0.5, -0.5, 1, 1, 1, 'debris')]))
        parts.append(ring)
        tr[(f'ring{i}', 'px')] = shift([(0, -0.35 * i), (0.5, 0.35 * i)], i * 0.1)
        tr[(f'ring{i}', 'pz')] = shift([(0, -0.35 * i), (0.5, 0.35 * i)], i * 0.1 + 0.25)
        tr[(f'ring{i}', 'sy')] = shift([(0, 0), (0.5, 0.1)], i * 0.15)
    clips = [Clip('time', 50, tr, '1f')]
    if attack:
        clips.append(Clip('attack', 12, {(f'ring{i}', 's'): shift([(0, 0), (0.3, 0.25), (1, 0)], 0) for i in range(6)}))
    extra = ''.join(f'        ring{i}.yRot += t * {0.35 - i * 0.03:.2f}f;\n' for i in range(6))
    M.append(Model(cls, tex, mats, parts, clips, doc=doc, grounded=False, extra=extra))


spout('WaterspoutModel', 'waterspout', {'a': ('water', C('#7fbfe0'), C('#3a7aa0')), 'b': ('water', C('#bfe6f6'), C('#5a9ac0')),
      'debris': ('dark', C('#4a5a50'))}, False, 'The Waterspout: six rings of spinning water, wider toward the top, that corkscrew as they turn.')
spout('CycloneModel', 'cyclone', {'a': ('cloud', C('#b8c4cc'), C('#7a8892')), 'b': ('cloud', C('#dde4ea'), C('#9aa6b0')),
      'debris': ('dark', C('#5a5048'))}, True, 'The Cyclone: six rings of spinning wind with debris caught in them; it swells when it flings you.')


# ---- Tempest Sovereign ---------------------------------------------------------------------------------------
sov = P('body', (0, -3, 0), boxes=[Box(-5, -2, -3, 10, 12, 6, 'armor'), Box(-5.5, -2.5, -3.5, 11, 5, 7, 'metal'), Box(-2, 0, -4, 4, 4, 1, 'glow'),
                                   Box(-5.5, 8, -3.5, 11, 2, 7, 'metal')])
sov.add(*pair(P('left_pauldron', (5.5, -2, 0), (0, 0, -0.15), [Box(0, -2, -3, 4, 3, 6, 'metal')]).add(
    P('left_pspike0', (2, -2, -1.5), (-0.3, 0, 0.45), [Box(-0.5, -4, -0.5, 1, 4, 1, 'crystal')]),
    P('left_pspike1', (2.5, -2, 1.5), (0.3, 0, 0.6), [Box(-0.5, -3, -0.5, 1, 3, 1, 'crystal')]))))
shead = P('head', (0, -2.5, 0), boxes=[Box(-4, -8, -4, 8, 8, 8, 'skin'), Box(-3.5, -6, -4.6, 7, 5, 1, 'mask'), Box(-3, -5, -4.9, 2, 1, 1, 'eye'),
                                      Box(1, -5, -4.9, 2, 1, 1, 'eye'), Box(-4.5, -9, -4.5, 9, 2, 9, 'metal'), Box(-1, -10, -5, 2, 2, 1, 'glow')])
for i, (x, z, rx, rz, h) in enumerate(((-3.5, -3.5, -0.3, -0.3, 4), (3.5, -3.5, -0.3, 0.3, 4), (-3.5, 3.5, 0.3, -0.3, 3), (3.5, 3.5, 0.3, 0.3, 3))):
    shead.add(P(f'crown{i}', (x, -9, z), (rx, 0, rz), [Box(-0.5, -h, -0.5, 1, h, 1, 'metal')]))
shead.add(*pair(P('left_horn', (3, -8, 0), (0, 0, 0.6), [Box(-1, -5, -1, 2, 5, 2, 'crystal')]).add(
    P('left_horn_tip', (0, -5, 0), (0, 0, -0.5), [Box(-0.5, -4, -0.5, 1, 4, 1, 'crystal')]))))
sov.add(shead)
sov.add(*pair(P('left_arm', (6.5, -1, 0), boxes=[Box(-1.5, 0, -1.5, 3, 6, 3, 'armor')]).add(
    P('left_fore', (0, 6, 0), boxes=[Box(-1.5, 0, -1.5, 3, 5, 3, 'armor'), Box(-2, 1, -2, 4, 3, 4, 'metal')]).add(
        P('left_hand', (0, 5, 0), boxes=[Box(-1.5, 0, -1.5, 3, 3, 3, 'skin'), Box(-1, 3, -1, 2, 2, 2, 'glow')])))))
sov.add(P('cape', (0, -1.5, 3.2), boxes=[Box(-5, 0, 0, 10, 10, 1, 'cloth')]).add(
    P('cape_mid', (0, 10, 0), boxes=[Box(-5, 0, 0, 10, 8, 1, 'cloth')]).add(P('cape_low', (0, 8, 0), boxes=[Box(-5, 0, 0, 10, 6, 1, 'cloth_hem')]))))
sov.add(P('skirt', (0, 10, 0), boxes=[Box(-6, 0, -4, 12, 5, 8, 'cloth')]).add(
    P('vortex0', (0, 5, 0), boxes=[Box(-5, 0, -3.5, 10, 4, 7, 'cloud')]).add(
        P('vortex1', (0, 4, 0), boxes=[Box(-3.5, 0, -2.5, 7, 4, 5, 'cloud')]).add(
            P('vortex2', (0, 4, 0), boxes=[Box(-2, 0, -1.5, 4, 4, 3, 'cloud')]).add(P('vortex3', (0, 4, 0), boxes=[Box(-1, 0, -1, 2, 3, 2, 'cloud')]))))))
halo = P('ring', (0, -17, 0))
for i in range(12):
    a = i * math.pi / 6
    halo.add(P(f'halo{i}', (math.cos(a) * 7, 0, math.sin(a) * 7), (0, -a, 0), [Box(-1, -1, -0.5, 2, 2, 1, 'glow' if i % 3 == 0 else 'crystal')]))
orbs = [P('orb_a', (12, 2, 0), boxes=[Box(-1.5, -1.5, -1.5, 3, 3, 3, 'glow')]), P('orb_b', (-12, 2, 0), boxes=[Box(-1.5, -1.5, -1.5, 3, 3, 3, 'glow')])]
M.append(Model('SovereignModel', 'tempest_sovereign', {'armor': ('metal', C('#3a3460'), C('#f2c94c')), 'metal': ('metal', C('#c8a440'), C('#6a5418')),
    'skin': ('skin', C('#9aa6c8'), C('#5a6688')), 'mask': ('metal', C('#dfe6f0'), C('#7a8496')), 'eye': ('glow', C('#ffffff'), C('#7fd4ff')),
    'glow': ('glow', C('#e8f8ff'), C('#5ab0e8')), 'crystal': ('crystal', C('#7fd4ff'), C('#2a6a96')), 'cloth': ('cloth', C('#2c2450'), C('#f2c94c')),
    'cloth_hem': ('tatter', C('#2c2450'), C('#f2c94c')), 'cloud': ('cloud', C('#8a96b0'), C('#4a5670'))}, [sov, halo] + orbs, [
    Clip('time', 64, merge(sym(T(left_arm_rz=even(-22, -30), left_arm_rx=even(-16, -24), left_fore_rx=shift(even(-18, -30), 0.1),
                                 left_hand_rx=shift(even(-6, -16), 0.2))),
                           T(body_py=[(0, 0), (0.5, -1.4)], cape_rx=[(0, 8), (0.5, 15)], cape_mid_rx=shift([(0, 5), (0.5, 13)], 0.15),
                             cape_low_rx=shift([(0, 4), (0.5, 16)], 0.3), skirt_rz=even(-3, 3), vortex0_rx=shift(even(-4, 4), 0.1),
                             vortex1_rx=shift(even(-6, 6), 0.2), vortex2_rx=shift(even(-9, 9), 0.3), vortex3_rx=shift(even(-12, 12), 0.4),
                             head_rz=even(0, 2, 0, -2))), '1f'),
    Clip('dist', 0.08, T(body_rx=hold(12), cape_rx=hold(25), cape_mid_rx=hold(12), skirt_rx=hold(-10), vortex0_rx=hold(-8)), 'moveW'),
    Clip('attack', 26, merge(sym(T(left_arm_rx=[(0, 0), (0.25, 35), (0.45, -88), (0.7, -80), (1, 0)], left_arm_rz=[(0, 0), (0.25, -40), (0.45, -8), (1, 0)],
                                   left_fore_rx=[(0, 0), (0.25, -40), (0.45, 0), (1, 0)])),
                             T(body_rx=[(0, 0), (0.25, -12), (0.45, 15), (1, 0)], cape_rx=[(0, 0), (0.3, -10), (0.5, 45), (1, 0)],
                               cape_mid_rx=[(0, 0), (0.35, -6), (0.55, 30), (1, 0)]))),
    Clip('cast', 40, merge(sym(T(left_arm_rx=[(0, 0), (0.3, -160), (0.75, -165), (1, 0)], left_arm_rz=[(0, 0), (0.3, -24), (0.75, -30), (1, 0)])),
                           T(head_rx=[(0, 0), (0.3, -22), (0.75, -22), (1, 0)], ring_s=[(0, 0), (0.35, 0.35), (0.75, 0.35), (1, 0)],
                             body_py=[(0, 0), (0.3, -2), (0.75, -2.5), (1, 0)]))),
    hurt_clip('body', 'head', 0.6)],
    doc='The Tempest Sovereign: a crowned, armoured storm lord with crystal horns, a halo of lightning, a three-part cape and a body that trails into a spinning vortex; it pushes gusts out with both arms and raises them to cast.',
    grounded=False, look=[('head', 1.0, 1.0)],
    extra='''        vortex0.yRot += t * 0.2f; vortex1.yRot -= t * 0.3f; vortex2.yRot += t * 0.45f; vortex3.yRot -= t * 0.6f;
        ring.yRot += t * (s.attackAge < 40 && s.attackKind == AnimPulse.CAST ? 0.45f : 0.12f);
        orbA.x = Mth.cos(t * 0.1f) * 12; orbA.z = Mth.sin(t * 0.1f) * 12; orbA.y = 2 + Mth.sin(t * 0.2f) * 2;
        orbB.x = -orbA.x; orbB.z = -orbA.z; orbB.y = 2 - Mth.sin(t * 0.2f) * 2;
        cape.xRot += Math.min(0.6f, s.speed * 3f);
'''))


# ---- Skyray ------------------------------------------------------------------------------------------------
ray = P('body', (0, 20, 0), boxes=[Box(-6, -1.5, -6, 12, 3, 12, 'skin'), Box(-4, -2.5, -4, 8, 1, 8, 'skin2'), Box(-5, 1.5, -5, 10, 1, 10, 'belly')])
ray.add(P('head', (0, 0, -6), boxes=[Box(-3, -1, -3, 6, 2, 3, 'skin'), Box(-2, 0.5, -3.3, 4, 1, 1, 'mouth')]).add(
    *pair(P('left_horn', (2.5, 0, -3), (0, -0.3, 0), [Box(-0.5, -0.5, -3, 1, 1, 3, 'skin2')]).add(
        P('left_horn_tip', (0, 0, -3), (0.5, 0, 0), [Box(-0.5, -0.5, -2, 1, 1, 2, 'glow')]))),
    *pair(P('left_eye', (3, -0.5, -1.5), boxes=[Box(-0.3, -0.5, -0.5, 1, 1, 1, 'eye')]))))
ray.add(*pair(P('left_wing', (6, 0, 0), boxes=[Box(0, -1, -5, 5, 2, 10, 'skin')]).add(
    P('left_wing_mid', (5, 0, 0.5), boxes=[Box(0, -0.5, -4, 5, 1, 8, 'skin2')]).add(
        P('left_wing_tip', (5, 0, 1), boxes=[Box(0, -0.5, -2.5, 4, 1, 5, 'skin2'), Box(3, -0.45, -2.5, 1, 1, 5, 'glow')])))))
ray.add(P('tail', (0, 0, 6), boxes=[Box(-1, -1, 0, 2, 2, 5, 'skin2')]).add(
    P('tail_mid', (0, 0, 5), boxes=[Box(-0.5, -0.5, 0, 1, 1, 5, 'skin2')]).add(
        P('tail_end', (0, 0, 5), boxes=[Box(-0.5, -0.5, 0, 1, 1, 4, 'skin2')]).add(P('stinger', (0, 0, 4), boxes=[Box(-1, -1, 0, 2, 2, 2, 'glow')])))))
ray.add(P('saddle', (0, -2.5, 0), boxes=[Box(-3.5, -1, -3, 7, 1, 6, 'leather'), Box(-0.5, -2, -3, 1, 1, 1, 'leather'), Box(3.5, -1, -1, 1, 4, 2, 'leather'),
                                         Box(-4.5, -1, -1, 1, 4, 2, 'leather')]))
M.append(Model('SkyrayModel', 'skyray', {'skin': ('skin', C('#4a6aa0'), C('#d8e8ff')), 'skin2': ('skin', C('#5a7ec0'), C('#2a3c66')),
    'belly': ('skin', C('#d8e4f2'), C('#a0b4cc')), 'eye': ('eye', C('#f2e070'), C('#101828')), 'glow': ('glow', C('#c8f0ff'), C('#4a8ac0')),
    'leather': ('leather', C('#7a4a2a'), C('#d8b070')), 'mouth': ('dark', C('#1a2438'))}, [ray], [
    Clip('time', 38, sym(T(left_wing_rz=[(0, -24), (0.5, 24)], left_wing_mid_rz=shift([(0, -28), (0.5, 28)], 0.12),
                           left_wing_tip_rz=shift([(0, -34), (0.5, 34)], 0.24), body_py=shift([(0, 0.7), (0.5, -0.7)], 0.3),
                           body_rx=shift(even(2, -2), 0.3), tail_ry=even(-10, 10), tail_mid_ry=shift(even(-14, 14), 0.12),
                           tail_end_ry=shift(even(-18, 18), 0.24), left_horn_tip_rx=even(10, 30))), '0.55f + 0.45f * air'),
    Clip('fidget', 160, sym(T(left_wing_rz=[(0, 0), (0.25, -40), (0.5, 40), (0.75, -20), (1, 0)], body_rz=[(0, 0), (0.25, 10), (0.6, -6), (1, 0)])),
         'idleW', dur=30),
    hurt_clip('body', None, 0.8)],
    doc='The Skyray: a broad manta of the storm sky whose wings ripple in a travelling wave, with curling head fins and a four-jointed glowing tail; it banks into turns. Its saddle shows when saddled.',
    grounded=False,
    extra='        body.zRot -= turn * 0.04f;\n        tail.yRot += turn * 0.05f; tailMid.yRot += turn * 0.04f;\n        saddle.visible = s.saddled;\n'))


# ---- Puffling ------------------------------------------------------------------------------------------------
puff = [P('body', (0, 16, 0), boxes=[Box(-4, -3, -5, 8, 6, 10, 'skin')]),
        P('fluff', (0, 16, 0), boxes=[Box(-6, -5, -7, 12, 9, 14, 'cloud')]).add(
            P('fluff_top', (0, -5, 0), boxes=[Box(-4, -3, -5, 8, 3, 10, 'cloud2'), Box(-2, -4, -2, 4, 1, 4, 'cloud2')]),
            *pair(P('left_fluff', (6, -1, 0), boxes=[Box(-0.5, -3, -4, 2, 5, 8, 'cloud2')])),
            P('lump0', (-4, -5.5, -5), boxes=[Box(-1.5, -1.5, -1.5, 4, 3, 3, 'cloud2')]),
            P('lump1', (3, -5.5, -4), boxes=[Box(-1.5, -1.5, -1.5, 3, 3, 4, 'cloud2')]),
            P('lump2', (-1, -6, 1), boxes=[Box(-1.5, -1.5, -1.5, 4, 3, 4, 'cloud2')]),
            P('lump3', (4, -5, 3), boxes=[Box(-1.5, -1.5, -1.5, 3, 3, 3, 'cloud2')]),
            P('lump4', (-4.5, -5, 4), boxes=[Box(-1.5, -1.5, -1.5, 4, 3, 4, 'cloud2')]),
            P('lump5', (6, -3, -5), boxes=[Box(-1.5, -1.5, -1.5, 3, 3, 4, 'cloud2')]),
            P('lump6', (-7, -2, -4), boxes=[Box(-1.5, -1.5, -1.5, 4, 3, 3, 'cloud2')]),
            P('lump7', (6, 1, 4), boxes=[Box(-1.5, -1.5, -1.5, 3, 3, 4, 'cloud2')]),
            P('lump8', (-7, 0, 3), boxes=[Box(-1.5, -1.5, -1.5, 4, 3, 4, 'cloud2')]),
            P('lump9', (0, -3, 7), boxes=[Box(-1.5, -1.5, -1.5, 3, 3, 3, 'cloud2')]),
            P('lump10', (-3, 0, 7), boxes=[Box(-1.5, -1.5, -1.5, 4, 3, 4, 'cloud2')]),
            P('lump11', (3, -4, -7.5), boxes=[Box(-1.5, -1.5, -1.5, 3, 3, 4, 'cloud2')]),
            P('lump12', (-2, -1, -7.5), boxes=[Box(-1.5, -1.5, -1.5, 4, 3, 3, 'cloud2')]),
            P('spark', (0, -9, 0), boxes=[Box(-1, -2, -1, 2, 2, 2, 'glow')])),
        P('head', (0, 12, -6), boxes=[Box(-3, -4, -5, 6, 6, 6, 'skin'), Box(-3.5, -5, -4, 7, 2, 4, 'cloud2'), Box(-2, -1, -6, 4, 3, 1, 'snout'),
                                      Box(1, -2.5, -5.3, 1, 1, 1, 'eye'), Box(-2, -2.5, -5.3, 1, 1, 1, 'eye')]).add(
            *pair(P('left_ear', (3, -3, -2), (0, 0, 0.5), [Box(0, -0.5, -1, 3, 1, 2, 'skin')]).add(P('left_ear_tip', (3, 0, 0), boxes=[Box(0, -0.5, -1, 1, 1, 2, 'pink')]))))]
for i in range(4):
    x, z = (2.5 if i % 2 == 0 else -2.5), (-3.5 if i < 2 else 3.5)
    puff.append(P(f'leg{i}', (x, 19, z), boxes=[Box(-1, 0, -1, 2, 3, 2, 'skin')]).add(
        P(f'shin{i}', (0, 3, 0), boxes=[Box(-1, 0, -1, 2, 1, 2, 'skin')]).add(P(f'hoof{i}', (0, 1, 0), boxes=[Box(-1, 0, -1, 2, 1, 2, 'dark')]))))
ptrot = merge(*[gait_leg(f'leg{i}', f'shin{i}', ph, 32, 40) for i, ph in ((0, 0), (3, 0), (1, 0.5), (2, 0.5))])
M.append(Model('PufflingModel', 'puffling', {'skin': ('skin', C('#e6d6ee'), C('#a890b8')), 'cloud': ('cloud', C('#e8ecf4'), C('#a8b0c0')),
    'cloud2': ('cloud', C('#f8fafe'), C('#c0c8d4')), 'glow': ('glow', C('#fff2a8'), C('#c0a040')), 'snout': ('skin', C('#f2b8c8'), C('#c08898')),
    'pink': ('skin', C('#f2a8c0'), C('#b07088')), 'eye': ('eye', C('#1a1028'), C('#2a1a38')), 'dark': ('dark', C('#5a4a66'))}, puff, [
    Clip('time', 80, sym(T(fluff_s=[(0, 0), (0.5, 0.035)], left_ear_rz=even(0, 6, 0, -4), spark_py=[(0, 0), (0.5, -0.8)], spark_s=[(0, 0), (0.5, 0.25)])), '1f'),
    Clip('dist', 0.2, merge(ptrot, sym(T(fluff_py=even(0, -0.6, 0, -0.6), body_py=even(0, -0.5, 0, -0.5), head_py=shift(even(0, -0.5, 0, -0.5), 0.06),
                                         head_rx=shift(even(0, 4, 0, 4), 0.06), left_ear_rz=shift(even(0, 18, 0, 18), 0.15),
                                         fluff_rz=even(2, -2)))), 'moveW'),
    Clip('fidget', 240, T(head_rx=[(0, 0), (0.15, 58), (0.3, 54), (0.45, 60), (0.6, 54), (0.85, 58), (1, 0)],
                          head_py=[(0, 0), (0.15, 2), (0.85, 2), (1, 0)], head_ry=[(0, 0), (0.3, 8), (0.55, -8), (0.85, 0), (1, 0)]), 'idleW', dur=70),
    Clip('fidget', 170, sym(T(fluff_rz=[(0, 0), (0.1, 12), (0.2, -12), (0.3, 10), (0.4, -9), (0.5, 6), (0.6, -3), (0.75, 0), (1, 0)],
                              head_rz=[(0, 0), (0.12, -14), (0.22, 14), (0.32, -12), (0.42, 10), (0.55, -5), (0.75, 0), (1, 0)],
                              left_ear_rz=[(0, 0), (0.12, 30), (0.22, -20), (0.32, 25), (0.42, -15), (0.6, 0)])), 'idleW', dur=24),
    hurt_clip('body', 'head')],
    doc='The Puffling: a little sky lamb in a thundercloud fleece with a spark on top; it trots, grazes and gives itself a shake. Shearing removes the fleece.',
    look=[('head', 1.0, 1.0)], flags=('sheared',),
    extra='        fluff.visible = !s.sheared;\n'))


# ---- Glimmerstag and Thistlehop ------------------------------------------------------------------------------
stag = [P('body', (0, 10, 0), boxes=[Box(-4, -4, -6, 8, 8, 12, 'fur'), Box(-3.5, -4.5, -8.5, 7, 8, 3, 'fur'), Box(-4, -4.5, 5, 8, 8, 3, 'fur'),
                                     Box(-3.5, 3.5, -6, 7, 1, 11, 'belly'), Box(-1, -5, -5, 2, 1, 10, 'fur2')]).add(
            P('tail', (0, -3.5, 8), (0.4, 0, 0), [Box(-1, 0, 0, 2, 3, 1, 'belly')]))]
shead = P('head', (0, -9, 0), boxes=[Box(-2.5, -3, -5, 5, 4, 6, 'fur'), Box(-1.5, -2, -8, 3, 3, 3, 'fur2'), Box(-1, -2.2, -8.5, 2, 1, 1, 'nose'),
                                     Box(2.2, -2.5, -3.5, 1, 1, 1, 'eye'), Box(-3.2, -2.5, -3.5, 1, 1, 1, 'eye')]).add(
    P('jaw', (0, 1, -5), boxes=[Box(-1.5, 0, -3, 3, 1, 3, 'fur2')]),
    *pair(P('left_ear', (2.3, -2.5, -0.5), (0, 0, 0.6), [Box(0, -0.5, -0.5, 3, 1, 2, 'fur')])))
for side in (1, -1):
    nm = 'left' if side > 0 else 'right'
    shead.add(P(f'{nm}_antler', (side * 1.5, -3, -1), (0.1, 0, side * 0.3), [Box(-0.5, -6, -0.5, 1, 6, 1, 'crystal')]).add(
        P(f'{nm}_tine0', (0, -2.5, 0), (-0.8, 0, side * 0.4), [Box(-0.5, -3, -0.5, 1, 3, 1, 'crystal')]),
        P(f'{nm}_beam2', (0, -6, 0), (0.3, 0, -side * 0.25), [Box(-0.5, -4, -0.5, 1, 4, 1, 'crystal')]).add(
            P(f'{nm}_tine1', (0, -2, 0), (0, 0, side * 0.9), [Box(-0.5, -3, -0.5, 1, 3, 1, 'crystal')]),
            P(f'{nm}_tip', (0, -4, 0), boxes=[Box(-1, -2, -1, 2, 2, 2, 'glow')]))))
stag.append(P('neck', (0, 7, -7), (0.35, 0, 0), [Box(-2, -9, -2, 4, 10, 4, 'fur'), Box(-1, -9, 1.6, 2, 9, 1, 'fur2')]).add(shead))
for i in range(4):
    front = i < 2
    x = 2.5 if i % 2 == 0 else -2.5
    if front:
        stag.append(P(f'leg{i}', (x, 13, -6), boxes=[Box(-1, -1, -1.5, 2, 6, 3, 'fur')]).add(
            P(f'shin{i}', (0, 5, 0), boxes=[Box(-1, 0, -1, 2, 5, 2, 'fur')]).add(P(f'hoof{i}', (0, 5, 0), boxes=[Box(-1, 0, -1, 2, 1, 2, 'hoof')]))))
    else:
        stag.append(P(f'leg{i}', (x, 12, 6), boxes=[Box(-1.5, -1, -2, 3, 6, 4, 'fur')]).add(
            P(f'shin{i}', (0, 5, 1), boxes=[Box(-1, 0, -1, 2, 6, 2, 'fur')]).add(P(f'hoof{i}', (0, 6, 0), boxes=[Box(-1, 0, -1, 2, 1, 2, 'hoof')]))))
swalk = merge(*[gait_leg(f'leg{i}', f'shin{i}', ph, 24, 45) for i, ph in ((2, 0), (0, 0.25), (3, 0.5), (1, 0.75))])
M.append(Model('GlimmerstagModel', 'glimmerstag', {'fur': ('fur', C('#6a8a7a'), C('#3a5048')), 'fur2': ('fur', C('#587466'), C('#2e423a')),
    'belly': ('fur', C('#d8e6dc'), C('#a0b4a8')), 'nose': ('dark', C('#2a2a2a')), 'eye': ('eye', C('#2a1a10'), C('#101010')),
    'crystal': ('crystal', C('#9ff0e0'), C('#3a9a88')), 'glow': ('glow', C('#e8fff8'), C('#6ad8c0')), 'hoof': ('dark', C('#3a3430'))}, stag, [
    Clip('time', 100, sym(T(body_py=[(0, 0), (0.5, 0.2)], neck_rx=even(0, 2, 0, -1), tail_rx=even(0, 6, 0, -3),
                            left_tip_s=even(0, 0.2), right_tip_s=shift(even(0, 0.2), 0.5))), '1f'),
    Clip('dist', 0.13, merge(swalk, T(body_py=even(0, -0.4, 0, -0.4), neck_rx=even(0, 4, 0, 4), tail_rx=even(10, 0))), 'moveW'),
    Clip('fidget', 260, T(neck_rx=[(0, 0), (0.18, 60), (0.82, 60), (1, 0)], head_rx=[(0, 0), (0.2, 18), (0.8, 18), (1, 0)],
                          jaw_rx=[(0, 0), (0.3, 0), (0.36, 14), (0.42, 0), (0.48, 14), (0.54, 0), (0.6, 14), (0.66, 0), (1, 0)]), 'idleW', dur=80),
    Clip('fidget', 70, T(left_ear_rz=[(0, 0), (0.25, 35), (0.5, -10), (0.75, 20), (1, 0)]), 'idleW', dur=12),
    Clip('fidget', 90, T(right_ear_rz=[(0, 0), (0.25, -35), (0.5, 10), (1, 0)], tail_rx=[(0, 0), (0.3, 35), (0.6, -5), (1, 0)]), 'idleW', dur=14),
    hurt_clip('body', 'head')],
    doc='The Glimmerstag: an elderstorm deer with jointed legs in a four-beat walk, flicking ears and tail, grazing, and branching crystal antlers whose tips pulse.',
    look=[('neck', 0.4, 0.3), ('head', 0.6, 0.7)], extra='        neck.yRot -= turn * 0.02f;\n'))

hop = [P('body', (0, 19, 0), boxes=[Box(-3, -3, -4, 6, 5, 8, 'fur'), Box(-2.5, 1.5, -3.5, 5, 1, 7, 'belly'), Box(-3.5, -2, 1, 7, 4, 3, 'fur')]).add(
           *[P(f'thistle{i}', (x, -3, z), (rx, 0, rz), [Box(-0.5, -3, -0.5, 1, 3, 1, 'thorn'), Box(-1, -4, -1, 2, 1, 2, 'bloom'),
                                                         Box(-0.5, -5, -0.5, 1, 1, 1, 'bloom2')]) for i, (x, z, rx, rz) in
             enumerate(((-1.5, -2, -0.3, -0.2), (1.5, -1, -0.2, 0.2), (0, 1, 0.1, 0), (-1.5, 2.5, 0.3, -0.2), (1.5, 3, 0.3, 0.2)))],
           P('tail', (0, -1, 4), boxes=[Box(-1.5, -1.5, 0, 3, 3, 2, 'belly')]))]
hhead = P('head', (0, 17, -4), boxes=[Box(-2.5, -3, -4, 5, 4, 4, 'fur'), Box(-3, -1.5, -3.5, 6, 2, 2, 'belly'),
                                     Box(2.1, -2, -3, 1, 1, 1, 'eye'), Box(-3.1, -2, -3, 1, 1, 1, 'eye')]).add(
    P('nose', (0, -1.2, -4), boxes=[Box(-1, -0.5, -0.6, 2, 1, 1, 'nose')]),
    *pair(P('left_ear', (1.2, -3, -1.5), (-0.2, 0, 0.15), [Box(-0.5, -4, -0.5, 2, 4, 1, 'fur')]).add(
        P('left_ear_tip', (0, -4, 0), boxes=[Box(-0.5, -3, -0.5, 2, 3, 1, 'fur2')]))))
hop.append(hhead)
hop += pair(P('left_hind', (2.5, 19, 2), boxes=[Box(-1, -1, -2, 2, 4, 4, 'fur')]).add(P('left_hind_foot', (0, 3, -1), boxes=[Box(-1, 0, -3, 2, 1, 5, 'belly')])))
hop += pair(P('left_front', (1.8, 20, -3), boxes=[Box(-0.5, 0, -0.5, 1, 4, 1, 'fur')]))
hopk = [(0, 0), (0.12, 0.4), (0.42, -3.6), (0.7, -1.2), (0.86, 0)]
M.append(Model('ThistlehopModel', 'thistlehop', {'fur': ('fur', C('#8a7a5a'), C('#5a4a34')), 'fur2': ('fur', C('#5a4a34'), C('#3a2e20')),
    'belly': ('fur', C('#e8dcc4'), C('#b8a888')), 'nose': ('skin', C('#d08898'), C('#905060')), 'eye': ('eye', C('#1a1408'), C('#0a0804')),
    'thorn': ('shell', C('#5a7a3a'), C('#2a3a1a')), 'bloom': ('fur', C('#b06ad6'), C('#6a3a8a')), 'bloom2': ('fur', C('#d898f0'), C('#8a4ab0'))}, hop, [
    Clip('time', 40, sym(T(body_py=[(0, 0), (0.5, 0.15)], left_ear_rx=even(0, 4, 0, -3), left_ear_tip_rx=shift(even(0, 6, 0, -4), 0.1))), 'idleW'),
    Clip('fidget', 22, T(nose_py=[(0, 0), (0.25, -0.3), (0.5, 0), (0.75, -0.3), (1, 0)], nose_s=[(0, 0), (0.25, 0.15), (0.5, 0), (0.75, 0.15), (1, 0)]),
         'idleW', dur=8),
    Clip('dist', 0.16, sym(T(body_py=hopk, head_py=shift(hopk, 0.03), left_hind_py=shift(hopk, -0.04), left_front_py=shift(hopk, 0.02), body_rx=[(0, 5), (0.3, -20), (0.55, 12), (0.8, 4)],
                             head_rx=[(0, 0), (0.35, 12), (0.6, -6)], left_hind_rx=[(0, 0), (0.3, 62), (0.6, -22), (0.9, -5)],
                             left_hind_foot_rx=[(0, 0), (0.3, 30), (0.6, -10)], left_front_rx=[(0, 10), (0.3, -30), (0.55, 42), (0.8, 5)],
                             left_ear_rx=[(0.1, 0), (0.42, 28), (0.75, -12)], left_ear_tip_rx=[(0.15, 0), (0.48, 22), (0.8, -10)])), 'moveW'),
    Clip('fidget', 210, sym(T(body_rx=[(0, 0), (0.2, -45), (0.8, -45), (1, 0)], body_py=[(0, 0), (0.2, -2.6), (0.8, -2.6), (1, 0)], head_py=[(0, 0), (0.2, -6.5), (0.8, -6.5), (1, 0)],
                              head_pz=[(0, 0), (0.2, 2.2), (0.8, 2.2), (1, 0)], head_rx=[(0, 0), (0.2, -10), (0.5, -18), (0.8, -10), (1, 0)],
                              head_ry=[(0, 0), (0.35, 30), (0.6, -30), (0.8, 0)], left_front_rx=[(0, 0), (0.2, -50), (0.8, -50), (1, 0)],
                              left_front_py=[(0, 0), (0.2, -5.5), (0.8, -5.5), (1, 0)], left_front_pz=[(0, 0), (0.2, 1.5), (0.8, 1.5), (1, 0)],
                              left_ear_rx=[(0, 0), (0.25, -12), (0.8, -12), (1, 0)])), 'idleW', dur=56),
    hurt_clip('body', 'head')],
    doc='The Thistlehop: a hare-like forest hopper with long jointed ears and blooming thistles on its back; it really hops, twitches its nose and stands up to look around.',
    look=[('head', 1.0, 1.0)]))


# ---- Hollow Keeper (the Labyrinth's guardian) ----------------------------------------------------------------
kleg = P('left_leg', (2, 8, 0), boxes=[Box(-1.5, 0, -1.5, 3, 8, 3, 'cloth')]).add(
    P('left_shin', (0, 8, 0), boxes=[Box(-1, 0, -1, 2, 7, 2, 'bone')]).add(P('left_foot', (0, 7, 0), boxes=[Box(-1.5, 0, -3, 3, 1, 4, 'bone')])))
khips = P('hips', (0, 7, 0), boxes=[Box(-4, -1, -2.5, 8, 3, 5, 'cloth')]).add(
    P('robe_front', (0, 2, -2.7), boxes=[Box(-4, 0, -0.5, 8, 7, 1, 'cloth')]).add(P('robe_front_low', (0, 7, 0), boxes=[Box(-3.5, 0, -0.5, 7, 7, 1, 'rags')])),
    P('robe_back', (0, 2, 2.7), boxes=[Box(-4, 0, -0.5, 8, 7, 1, 'cloth')]).add(P('robe_back_low', (0, 7, 0), boxes=[Box(-4, 0, -0.5, 8, 8, 1, 'rags')])),
    P('lantern', (4.5, 1, -1), boxes=[Box(-0.5, 0, -0.5, 1, 3, 1, 'metal')]).add(
        P('lantern_cage', (0, 3, 0), boxes=[Box(-1.5, 0, -1.5, 3, 1, 3, 'metal'), Box(-1, 1, -1, 2, 3, 2, 'glow'), Box(-1.5, 4, -1.5, 3, 1, 3, 'metal')])))
ktorso = P('torso', (0, -1, 0), (0.25, 0, 0), [Box(-4, -12, -2.5, 8, 12, 5, 'cloth'), Box(-3, -11, -3.1, 6, 6, 1, 'bone'), Box(-1, -9, -3.5, 2, 2, 1, 'glow')])
khead = P('head', (0, -3, 0), boxes=[Box(-3.5, -8, -3.5, 7, 8, 7, 'bone'), Box(-4, -9, -2.5, 8, 9, 7, 'hood'), Box(-3, -6.5, -4.1, 6, 5, 1, 'mask'),
                                    Box(-2.5, -5.5, -4.4, 2, 2, 1, 'eye'), Box(0.5, -5.5, -4.4, 2, 2, 1, 'eye')]).add(
    P('jaw', (0, -1.5, -1), boxes=[Box(-2.5, 0, -3, 5, 2, 4, 'bone')]))
ktorso.add(P('neck', (0, -12, -1), (0.2, 0, 0), [Box(-1, -3, -1, 2, 3, 2, 'bone')]).add(khead))
karm = P('left_arm', (5, -11, 0), (0, 0, -0.06), [Box(-1, -1, -1.5, 3, 10, 3, 'cloth'), Box(-1.5, 6, -2, 4, 4, 4, 'rags')]).add(
    P('left_fore', (0.5, 9, 0), boxes=[Box(-1, 0, -1, 2, 9, 2, 'bone')]).add(
        P('left_hand', (0, 9, 0), boxes=[Box(-1.5, 0, -1.5, 3, 2, 3, 'bone')]).add(
            *[P(f'left_finger{i}', (x, 2, z), (0.25, 0, 0), [Box(-0.5, 0, -0.5, 1, 4, 1, 'claw')]) for i, (x, z) in enumerate(((-1, -1), (0, -1), (1, -1), (0.5, 1)))])))
ktorso.add(*pair(karm))
khips.add(ktorso)
fingers = {(f'left_finger{i}', 'rx'): [(0, 0), (0.2, -25), (0.4, 40), (1, 0)] for i in range(4)}
M.append(Model('HollowKeeperModel', 'hollow_keeper', {'cloth': ('cloth', C('#3a3448'), C('#1a1622')), 'rags': ('tatter', C('#34303e'), C('#1a1622')),
    'hood': ('cloth', C('#2a2634'), C('#141018')), 'metal': ('metal', C('#6a6458'), C('#2e2a24')), 'glow': ('glow', C('#bff0ff'), C('#3a8ab0')),
    'bone': ('bone', C('#d8d2c0'), C('#8a8270')), 'mask': ('bone', C('#e8e2d0'), C('#8a8270')), 'claw': ('bone', C('#9a9280'), C('#4a4438')),
    'eye': ('glow', C('#9fe8ff'), C('#0a1a24'))}, [khips, *pair(kleg)], [
    Clip('time', 90, sym(T(torso_rx=[(0, 0), (0.5, 3)], hips_py=[(0, 0), (0.5, 0.2)], head_rz=even(-4, 4), left_arm_rx=even(-3, 3),
                           left_fore_rx=shift(even(-6, -2), 0.1), lantern_rz=even(-6, 6), lantern_cage_rz=shift(even(-5, 5), 0.15),
                           robe_front_low_rx=even(-2, 2), left_finger0_rx=even(0, 8, 0, -4))), 'idleW'),
    Clip('fidget', 140, T(head_rz=[(0, 0), (0.08, 30), (0.6, 27), (0.75, 0), (1, 0)], head_rx=[(0, 0), (0.08, 8), (0.6, 8), (1, 0)],
                          jaw_rx=[(0, 0), (0.1, 20), (0.6, 18), (0.8, 0)]), 'idleW', dur=24),
    Clip('dist', 0.15, merge(sym(gait_leg('left_leg', 'left_shin', 0, 28, 50)), gait_leg('right_leg', 'right_shin', 0.5, 28, 50),
                             T(left_foot_rx=[(0, 14), (0.3, 0), (0.6, -16), (0.8, 8)], right_foot_rx=shift([(0, 14), (0.3, 0), (0.6, -16), (0.8, 8)], 0.5),
                               hips_py=even(0, 0.7, 0, 0.7), hips_rz=even(3, -3), torso_ry=even(-8, 8), head_ry=even(6, -6),
                               left_arm_rx=[(0, 26), (0.5, -26)], right_arm_rx=[(0, -26), (0.5, 26)],
                               left_fore_rx=shift([(0, -6), (0.5, -26)], 0.12), right_fore_rx=shift([(0, -26), (0.5, -6)], 0.12),
                               robe_front_rx=even(-10, 6), robe_front_low_rx=shift(even(-14, 8), 0.15), robe_back_rx=even(8, -6),
                               robe_back_low_rx=shift(even(16, -6), 0.15), lantern_rx=shift(even(-18, 18), 0.2), lantern_cage_rx=shift(even(-12, 12), 0.35))),
         'moveW'),
    Clip('attack', 18, merge(fingers, T(left_arm_rx=[(0, 0), (0.25, -150), (0.45, -15), (0.6, 12), (1, 0)], left_arm_rz=[(0, 0), (0.25, -32), (0.45, 28), (1, 0)],
                                        left_fore_rx=[(0, 0), (0.25, -30), (0.45, 0), (1, 0)], torso_ry=[(0, 0), (0.25, -28), (0.45, 32), (1, 0)],
                                        torso_rx=[(0, 0), (0.25, -8), (0.45, 14), (1, 0)], jaw_rx=[(0, 0), (0.18, 38), (0.6, 36), (1, 0)]))),
    hurt_clip('torso', 'head', 0.8)],
    doc='The Hollow Keeper: a gaunt hooded figure three blocks tall with a bone mask, glowing eyes, long clawed arms, tattered robes and a swinging lantern; it stalks with long strides, twitches its head, and swipes.',
    look=[('neck', 0.3, 0.3), ('head', 0.7, 0.7)], extra='        torso.yRot -= turn * 0.02f;\n'))

# =============================================================================================== 5.2 Stormwild
# ---- Cloud Whale ----------------------------------------------------------------------------------------------
wbody = P('body', (0, 6, 0), boxes=[Box(-14, -12, -16, 28, 24, 32, 'skin'), Box(-12, 10, -15, 24, 3, 28, 'belly')])
whead = P('head', (0, 0, -16), boxes=[Box(-12, -10, -18, 24, 19, 18, 'skin'), Box(-9, -12, -16, 18, 2, 12, 'skin2'),
                                      Box(11.6, -2, -12, 1, 2, 2, 'eye'), Box(-12.6, -2, -12, 1, 2, 2, 'eye'),
                                      Box(-6, -11, -17, 3, 1, 3, 'bone'), Box(4, -11, -14, 2, 1, 2, 'bone'), Box(8, -6, -18.5, 2, 2, 1, 'bone')]).add(
    P('jaw', (0, 6, -1), boxes=[Box(-11, 0, -16.5, 22, 5, 17, 'belly')]))
wbody.add(whead)
for i, (x, z, w_, d_) in enumerate(((-6, -10, 9, 9), (5, -4, 8, 10), (-3, 3, 10, 9), (6, 9, 7, 7), (-7, 11, 6, 6), (0, -13, 7, 5))):
    wbody.add(P(f'puff{i}', (x, -12, z), boxes=[Box(-w_ / 2, -4, -d_ / 2, w_, 4, d_, 'cloud'), Box(-w_ / 4, -6, -d_ / 4, w_ / 2, 2, d_ / 2, 'cloud2')]))
wbody.add(*pair(P('left_fin', (13, 7, -8), (0, 0.15, 0.45), [Box(0, -1, -4, 14, 2, 9, 'skin')]).add(
    P('left_fin_tip', (14, 0, 0.5), (0, 0.2, 0.1), [Box(0, -1, -3, 10, 2, 6, 'skin2')]))))
wtail = P('tail', (0, -1, 16), boxes=[Box(-10, -9, 0, 20, 18, 16, 'skin'), Box(-8, 7.5, 1, 16, 2, 14, 'belly')]).add(
    P('tail_mid', (0, 1, 16), boxes=[Box(-6, -6, 0, 12, 12, 14, 'skin')]).add(
        P('tail_end', (0, 0, 14), boxes=[Box(-3, -4, 0, 6, 8, 10, 'skin2')]).add(
            *pair(P('left_fluke', (2, 0, 7), (0, 0.45, 0), [Box(0, -1, -3, 14, 2, 9, 'skin2'), Box(10, -1.1, 3, 5, 2, 4, 'cloud2')])))))
wbody.add(wtail)
M.append(Model('CloudWhaleModel', 'cloud_whale', {'skin': ('skin', C('#7f97b8'), C('#c8d8ee')), 'skin2': ('skin', C('#6b82a6'), C('#b8c8e0')),
    'belly': ('shell', C('#dfe8f2'), C('#a8b8cc')), 'eye': ('eye', C('#1c2840'), C('#0c1420')), 'bone': ('bone', C('#e8eef2'), C('#9aa6b0')),
    'cloud': ('cloud', C('#eef3f8'), C('#b8c4d0')), 'cloud2': ('cloud', C('#ffffff'), C('#d0d8e0'))}, [wbody], [
    Clip('time', 90, sym(T(body_rx=[(0, -2.5), (0.5, 2.5)], tail_rx=shift([(0, 6), (0.5, -6)], 0.12), tail_mid_rx=shift([(0, 10), (0.5, -10)], 0.24),
                           tail_end_rx=shift([(0, 14), (0.5, -14)], 0.36), head_rx=shift([(0, 2), (0.5, -2)], 0.9),
                           left_fin_rz=shift([(0, -10), (0.5, 12)], 0.1), left_fin_tip_rz=shift([(0, -8), (0.5, 10)], 0.25),
                           body_py=shift([(0, 0.6), (0.5, -0.6)], 0.2), jaw_rx=even(0, 2))), '1f'),
    Clip('fidget', 420, T(jaw_rx=[(0, 0), (0.2, 20), (0.45, 14), (0.7, 20), (1, 0)], head_rx=[(0, 0), (0.25, -6), (0.75, -6), (1, 0)]), '1f', dur=90),
    hurt_clip('body', 'head', 0.5)],
    doc='The Cloud Whale: a sky giant with a cloud-tufted back, barnacled head, two-jointed fins and a three-jointed tail; it undulates slowly as it swims through the air and opens its jaw to sing.',
    grounded=False, extra='        tail.yRot += turn * 0.05f; tailMid.yRot += turn * 0.04f;\n'))

# ---- Shock Eel ------------------------------------------------------------------------------------------------
eel = P('head', (0, 20, -10), boxes=[Box(-1.5, -1.5, -4, 3, 3, 4, 'skin'), Box(1.3, -1, -3, 1, 1, 1, 'eye'), Box(-2.3, -1, -3, 1, 1, 1, 'eye')]).add(
    P('jaw', (0, 0.5, 0), boxes=[Box(-1.5, 0, -4, 3, 1, 4, 'belly')]))
prev = eel
for i, (w_, ln) in enumerate(((3, 4), (3, 4), (3, 4), (2, 4), (2, 4), (2, 4), (1, 4))):
    seg = P(f'seg{i}', (0, 0, 4 if i else 0), boxes=[Box(-w_ / 2, -w_ / 2, 0, w_, w_, ln, 'skin'), Box(-0.5, -w_ / 2 - 1, 1, 1, 1, ln - 2, 'fin')])
    prev.add(seg); prev = seg
eswim = {}
for i in range(7):
    eswim[(f'seg{i}', 'ry')] = shift([(0, -(7 + i * 2.6)), (0.5, 7 + i * 2.6)], i * 0.1)
eswim[('head', 'ry')] = [(0, 5), (0.5, -5)]
M.append(Model('ShockEelModel', 'shock_eel', {'skin': ('spots', C('#2c3446'), C('#ffe25a')), 'belly': ('skin', C('#6a7488'), C('#3a4050')),
    'fin': ('glow', C('#ffd84a'), C('#a07a20')), 'eye': ('glow', C('#fff6b0'), C('#ffd84a'))}, [eel], [
    Clip('time', 16, eswim, '1f'),
    Clip('fidget', 80, T(jaw_rx=[(0, 0), (0.3, 18), (0.6, 0)]), '1f', dur=10),
    Clip('attack', 10, T(jaw_rx=[(0, 0), (0.2, 40), (0.6, 0)], head_pz=[(0, 0), (0.2, -2.5), (1, 0)], seg0_ry=[(0, 0), (0.3, 25), (1, 0)])),
    hurt_clip('head', None, 0.6)],
    doc='The Shock Eel: an eight-part body spotted with glowing yellow and a glowing fin ridge, swimming in a travelling wave; it lunges and snaps.',
    grounded=False))

# ---- Sparkskink -----------------------------------------------------------------------------------------------
sk = P('body', (0, 21, 0), boxes=[Box(-2, -1.5, -4, 4, 3, 8, 'scale'), Box(-1.5, 1.2, -3.5, 3, 1, 7, 'belly')])
skhead = P('head', (0, -0.5, -4), boxes=[Box(-1.5, -1.5, -3, 3, 3, 3, 'scale'), Box(-1, -0.8, -5, 2, 2, 2, 'scale'),
                                         Box(1.3, -1.2, -2.5, 1, 1, 1, 'eye'), Box(-2.3, -1.2, -2.5, 1, 1, 1, 'eye')]).add(
    P('jaw', (0, 1, -3), boxes=[Box(-1, 0, -2, 2, 1, 2, 'belly')]))
sk.add(skhead)
for i in range(5):
    z = -3 + i * 1.6
    sk.add(P(f'crest{i}', (0, -1.5, z), (-0.3, 0, 0), [Box(-0.5, -1.5 + (i in (1, 2)) * -0.5, -0.5, 1, 2, 1, 'crest')]))
    sk.add(P(f'spark{i}', (0, -1.5, z), (-0.3, 0, 0), [Box(-0.5, -2.2, -0.6, 1, 2, 1, 'glow')]))
tl = P('tail0', (0, -0.5, 4), boxes=[Box(-1.5, -1, 0, 3, 2, 4, 'scale')])
t1 = P('tail1', (0, 0, 4), boxes=[Box(-1, -0.75, 0, 2, 1, 4, 'scale')])
t2 = P('tail2', (0, 0, 4), boxes=[Box(-0.5, -0.5, 0, 1, 1, 4, 'scale')]).add(P('tail3', (0, 0, 4), boxes=[Box(-0.5, -0.5, 0, 1, 1, 3, 'crest')]))
tl.add(t1.add(t2)); sk.add(tl)
for side, z in (('front', -2.5), ('back', 2.5)):
    sk.add(*pair(P(f'left_{side}', (2, 0.5, z), (0, 0.25 if side == 'front' else -0.25, 0.45), [Box(0, -0.5, -0.5, 3, 1, 1, 'scale')]).add(
        P(f'left_{side}_foot', (3, 0, 0), (0, 0, 0.95), [Box(-0.5, 0, -0.5, 1, 2, 1, 'scale'), Box(-1, 1.5, -1.5, 2, 1, 2, 'belly')]))))
skwalk = {}
for leg, ph in (('left_front', 0), ('right_back', 0), ('right_front', 0.5), ('left_back', 0.5)):
    sg = 1 if leg.startswith('left') else -1
    skwalk[(leg, 'ry')] = shift([(0, 32 * sg), (0.5, -32 * sg)], ph)
    skwalk[(leg, 'rz')] = shift([(0, 0), (0.5, 0), (0.75, -28 * sg)], ph)
skwalk.update(T(body_ry=[(0, -9), (0.5, 9)], tail0_ry=shift([(0, 12), (0.5, -12)], 0.1), tail1_ry=shift([(0, 16), (0.5, -16)], 0.2),
                tail2_ry=shift([(0, 20), (0.5, -20)], 0.3), head_ry=shift([(0, 7), (0.5, -7)], 0.05)))
M.append(Model('SparkskinkModel', 'sparkskink', {'scale': ('scale', C('#3f7a6a'), C('#1e3c34')), 'belly': ('scale', C('#c8d8a0'), C('#8a9a60')),
    'crest': ('shell', C('#2f5a50'), C('#163028')), 'glow': ('glow', C('#c8f8ff'), C('#4ad0e8')), 'eye': ('eye', C('#f2d040'), C('#0c1410'))}, [sk], [
    Clip('time', 60, T(body_py=[(0, 0), (0.5, 0.15)], tail1_ry=even(0, 8, 0, -8), tail2_ry=shift(even(0, 12, 0, -12), 0.1), tail3_ry=shift(even(0, 16, 0, -16), 0.2)), 'idleW'),
    Clip('dist', 0.32, skwalk, 'moveW'),
    Clip('fidget', 130, T(body_py=[(0, 0), (0.18, -1.2), (0.35, 0), (0.55, -1.2), (0.72, 0), (1, 0)],
                          head_rx=[(0, 0), (0.18, -15), (0.35, 0), (0.55, -15), (0.72, 0)]), 'idleW', dur=22),
    Clip('fidget', 45, T(head_ry=[(0, 0), (0.15, 28), (0.75, 28), (1, 0)]), 'idleW', dur=14),
    Clip('fidget', 70, T(jaw_rx=[(0, 0), (0.2, 25), (0.4, 0)]), 'idleW', dur=8),
    hurt_clip('body', 'head')],
    doc='The Sparkskink: a sprawling coastal lizard with a five-spine crest that glows when it is storm-charged, an S-curving walk, a four-jointed tail, and the push-up display lizards do.',
    look=[('head', 1.0, 0.8)],
    extra='        for (ModelPart p : new ModelPart[] {spark0, spark1, spark2, spark3, spark4}) p.visible = s.charged;\n        tail0.yRot += turn * 0.05f;\n'))

# ---- Storm Jelly ----------------------------------------------------------------------------------------------
jbell = P('bell', (0, 9, 0), boxes=[Box(-5, -4, -5, 10, 5, 10, 'jelly'), Box(-4, -6, -4, 8, 2, 8, 'jelly'), Box(-2.5, -7, -2.5, 5, 1, 5, 'jelly2'),
                                    Box(-2.5, -3, -2.5, 5, 4, 5, 'glow')])
jtr = {}
for i in range(8):
    a = i * math.pi / 4
    jbell.add(P(f'rim{i}', (math.cos(a) * 4.6, 1, math.sin(a) * 4.6), (0, -a + math.pi / 2, 0), [Box(-1.5, 0, -0.5, 3, 2, 1, 'jelly2')]))
    jtr[(f'rim{i}', 'rx')] = [(0, 0), (0.25, 28), (0.6, -6), (1, 0)]
for i in range(6):
    a = i * math.pi / 3 + 0.3
    t0 = P(f'tent{i}', (math.cos(a) * 3.2, 1, math.sin(a) * 3.2), boxes=[Box(-0.5, 0, -0.5, 1, 4, 1, 'tent')])
    t0.add(P(f'tent{i}b', (0, 4, 0), boxes=[Box(-0.5, 0, -0.5, 1, 4, 1, 'tent')]).add(P(f'tent{i}c', (0, 4, 0), boxes=[Box(-0.5, 0, -0.5, 1, 3, 1, 'glow2')])))
    jbell.add(t0)
    jtr[(f'tent{i}', 'rx')] = shift(even(8, -8), i * 0.11); jtr[(f'tent{i}b', 'rx')] = shift(even(12, -12), i * 0.11 + 0.15)
    jtr[(f'tent{i}c', 'rx')] = shift(even(16, -16), i * 0.11 + 0.3); jtr[(f'tent{i}', 'rz')] = shift(even(6, -6), i * 0.13 + 0.2)
jbell.add(P('arm', (0, 1, 0), boxes=[Box(-1, 0, -1, 2, 6, 2, 'jelly2')]).add(P('arm2', (0, 6, 0), boxes=[Box(-1, 0, -1, 2, 5, 2, 'jelly2')])))
jtr.update(T(bell_sx=[(0, 0), (0.25, -0.12), (0.6, 0.04), (1, 0)], bell_sz=[(0, 0), (0.25, -0.12), (0.6, 0.04), (1, 0)],
             bell_sy=[(0, 0), (0.25, 0.08), (0.6, -0.03), (1, 0)], bell_py=[(0, 0), (0.3, -1.2), (1, 0)],
             arm_rx=shift(even(6, -6), 0.2), arm2_rx=shift(even(10, -10), 0.35)))
M.append(Model('StormJellyModel', 'storm_jelly', {'jelly': ('jelly', C('#8fd8ff'), C('#3a8ac0')), 'jelly2': ('jelly', C('#c8a8ff'), C('#6a4ab0')),
    'glow': ('glow', C('#f0fcff'), C('#7fd4ff')), 'glow2': ('glow', C('#fff6c0'), C('#c0a0ff')), 'tent': ('jelly', C('#b8e8ff'), C('#5a9ad0'))}, [jbell], [
    Clip('time', 34, jtr, '1f'),
    Clip('attack', 12, {(f'tent{i}', 'rx'): [(0, 0), (0.25, -35), (1, 0)] for i in range(6)}),
    Clip('hurt', 10, T(bell_s=[(0, 0), (0.2, -0.2), (1, 0)]))],
    doc='The Storm Jelly: a translucent glowing bell with a frilled rim, six three-jointed tentacles and oral arms; it pulses (contracting, rising, flaring its rim) and its tentacles trail in waves.',
    grounded=False, translucent=True))

# ---- Gale Sprite ----------------------------------------------------------------------------------------------
gs = P('body', (0, 17, 0), boxes=[Box(-1.5, -2, -1, 3, 4, 2, 'wisp')])
gs.add(P('head', (0, -2, 0), boxes=[Box(-2, -4, -2, 4, 4, 4, 'wisp2'), Box(-1.6, -2.8, -2.3, 1, 1, 1, 'eye'), Box(0.6, -2.8, -2.3, 1, 1, 1, 'eye')]).add(
    *pair(P('left_antenna', (1, -4, -1), (-0.4, 0, 0.35), [Box(-0.5, -3, -0.5, 1, 3, 1, 'wisp'), Box(-0.5, -4, -0.5, 1, 1, 1, 'glow')]))))
gs.add(*pair(P('left_wing_front', (1.2, -1.5, -0.2), (0, 0.2, 0), [Box(0, -0.5, -1, 6, 1, 2, 'wing')])))
gs.add(*pair(P('left_wing_back', (1.2, -0.5, 0.6), (0, -0.25, 0), [Box(0, -0.5, 0, 5, 1, 2, 'wing')])))
gs.add(*pair(P('left_arm', (1.5, -1.5, 0), (0, 0, -0.3), [Box(0, 0, -0.5, 1, 3, 1, 'wisp2')])))
gs.add(P('tail0', (0, 2, 0.5), boxes=[Box(-1, 0, -1, 2, 3, 2, 'wisp')]).add(P('tail1', (0, 3, 0), boxes=[Box(-0.5, 0, -0.5, 1, 3, 1, 'wisp')]).add(
    P('tail2', (0, 3, 0), boxes=[Box(-0.5, 0, -0.5, 1, 2, 1, 'glow')]))))
M.append(Model('GaleSpriteModel', 'gale_sprite', {'wisp': ('cloud', C('#e0f0fa'), C('#9ac0d8')), 'wisp2': ('cloud', C('#f6fbff'), C('#b8d4e8')),
    'wing': ('jelly', C('#d8f4ff'), C('#7ac8e8')), 'glow': ('glow', C('#ffffff'), C('#9fe0ff')), 'eye': ('eye', C('#4ac0e8'), C('#0c2030'))}, [gs], [
    Clip('time', 3, sym(T(left_wing_front_rz=[(0, -38), (0.5, 34)], left_wing_back_rz=shift([(0, -34), (0.5, 30)], 0.25))), '1f'),
    Clip('time', 26, sym(T(body_py=[(0, 0), (0.5, -1)], tail0_rx=even(12, -6), tail1_rx=shift(even(16, -8), 0.15), tail2_rx=shift(even(22, -10), 0.3),
                           tail0_rz=shift(even(10, -10), 0.25), left_arm_rz=even(-8, 4), left_antenna_rz=shift(even(10, -4), 0.2))), '1f'),
    Clip('fidget', 160, T(body_rx=[(0, 0), (1, -360)], body_py=[(0, 0), (0.5, -3), (1, 0)]), '1f', dur=20),
    hurt_clip('body', 'head')],
    doc='The Gale Sprite: a tiny cloud-bodied wind spirit with big eyes, glowing antennae, four buzzing dragonfly wings and a curling tail; now and then it loops the loop.',
    grounded=False, translucent=True, look=[('head', 1.0, 1.0)]))

# ---- Tidewrack Hermit -----------------------------------------------------------------------------------------
hshell = P('shell', (0, 18.5, 1.5), (-0.2, 0, 0), [Box(-4, -4, -3, 8, 7, 8, 'shell'), Box(-3, -7, -1, 6, 3, 6, 'shell2'), Box(-2, -9, 0, 4, 2, 4, 'shell'),
                                                  Box(-1, -10.5, 1, 2, 2, 2, 'shell2'), Box(-4.5, -3.5, -3.5, 9, 6, 1, 'nacre'), Box(3.5, -3, 0, 1, 4, 3, 'barnacle')])
hbody = P('body', (0, 20.5, -1.5), boxes=[Box(-2.5, -2, -3, 5, 3, 3, 'skin')]).add(
    *pair(P('left_eye', (1, -2, -2.5), (0, 0, 0.15), [Box(-0.5, -3, -0.5, 1, 3, 1, 'skin'), Box(-1, -4, -1, 2, 1, 2, 'eye')])),
    P('left_claw', (2.2, 0, -3), (0, -0.3, 0), [Box(-1, -1.5, -3.5, 3, 3, 3.5, 'claw')]).add(P('left_pincer', (0.5, -1.5, -3.5), boxes=[Box(-1, 0, -2, 2, 1, 2, 'claw2')])),
    P('right_claw', (-2.2, 0.5, -3), (0, 0.3, 0), [Box(-1, -1, -2.5, 2, 2, 2.5, 'claw')]),
    *pair(P('left_leg0', (2.5, 0.5, -2), (0, 0.3, 0.6), [Box(0, -0.5, -0.5, 3, 1, 1, 'claw')]).add(P('left_shin0', (3, 0, 0), (0, 0, 0.9), [Box(0, -0.5, -0.5, 3, 1, 1, 'claw2')]))),
    *pair(P('left_leg1', (2.5, 0.5, -0.5), (0, -0.2, 0.6), [Box(0, -0.5, -0.5, 3, 1, 1, 'claw')]).add(P('left_shin1', (3, 0, 0), (0, 0, 0.9), [Box(0, -0.5, -0.5, 3, 1, 1, 'claw2')]))))
hlegs = {}
for i, (sd, ph) in enumerate((('left', 0), ('right', 0.5))):
    for j in range(2):
        sg = 1 if sd == 'left' else -1
        hlegs[(f'{sd}_leg{j}', 'ry')] = shift([(0, 20 * sg), (0.5, -20 * sg)], ph + j * 0.5)
        hlegs[(f'{sd}_leg{j}', 'rz')] = shift([(0, 0), (0.5, 0), (0.75, -22 * sg)], ph + j * 0.5)
hlegs.update(T(shell_rz=even(4, -4), shell_rx=even(0, 3, 0, 3), body_ry=even(5, -5)))
M.append(Model('TidewrackHermitModel', 'tidewrack_hermit', {'shell': ('shell', C('#d8b8a8'), C('#8a6a5a')), 'shell2': ('shell', C('#c89c88'), C('#7a5a4a')),
    'nacre': ('shell', C('#f2e6f8'), C('#b8a8d0')), 'barnacle': ('bone', C('#cfd4d0'), C('#7a8480')), 'skin': ('skin', C('#d8784a'), C('#8a3a1a')),
    'claw': ('claw', C('#e0683a'), C('#7a2a10')), 'claw2': ('claw', C('#f0c8a0'), C('#a07850')), 'eye': ('eye', C('#e8d040'), C('#1a1008'))}, [hshell, hbody], [
    Clip('time', 50, sym(T(left_eye_rz=even(0, 8, 0, -5), left_claw_rx=even(0, -4, 0, 3), left_pincer_rx=even(0, -10, 0, -2))), 'idleW'),
    Clip('dist', 0.25, hlegs, 'moveW'),
    Clip('fidget', 110, T(left_claw_rx=[(0, 0), (0.2, -30), (0.35, 5), (0.5, -30), (0.65, 5), (1, 0)], left_pincer_rx=[(0, 0), (0.2, -30), (0.3, 0), (0.5, -30), (0.6, 0)]),
         'idleW', dur=24),
    hurt_clip('shell', None, 0.6)],
    doc='The Tidewrack Hermit: a hermit crab in a pearly spiral shell with a nacre lip and a barnacle, one big claw and one small, and eye stalks; it pulls right back into its shell to hide.',
    extra='''        float h = s.hide;
        body.z += h * 3.2f; body.y -= h * 0.6f;
        body.xScale -= h * 0.45f; body.yScale -= h * 0.45f; body.zScale -= h * 0.45f;
        shell.y += h * 1.2f; shell.xRot += h * 0.2f;
'''))

# ---- Thunderbird ----------------------------------------------------------------------------------------------
bird('ThunderbirdModel', 'thunderbird', {'feather': ('feather', C('#3a4258'), C('#1a1e2c')), 'feather2': ('feather', C('#4e5872'), C('#20263a')),
     'belly': ('feather', C('#b8c4d8'), C('#68748c')), 'beak': ('shell', C('#f2c94c'), C('#8a6a1e')), 'beak2': ('shell', C('#d8a83a'), C('#7a5a14')),
     'eye': ('eye', C('#9fe8ff'), C('#0a1420')), 'tail': ('feather', C('#2c3246'), C('#9fe8ff')), 'accent': ('crystal', C('#9fe8ff'), C('#3a7ab0')),
     'tip': ('feather', C('#c8f0ff'), C('#3a6a9a')), 'leg': ('scale', C('#e8b840'), C('#8a6a1e'))}, 2.4, True,
     'The Thunderbird: a storm-grey raptor nearly six blocks across, with lightning-blue flight feathers and a crystal crest; it screams and throws its wings wide to call lightning, and dives talons-first.')
M[-1].clips.append(Clip('cast', 24, sym(T(left_wing_rz=[(0, 0), (0.3, -70), (0.75, -70), (1, 0)], left_wing_tip_rz=[(0, 0), (0.3, -20), (0.75, -20), (1, 0)],
                                         neck_rx=[(0, 0), (0.3, -40), (0.75, -40), (1, 0)], jaw_rx=[(0, 0), (0.25, 40), (0.75, 40), (1, 0)],
                                         left_tail_ry=[(0, 0), (0.3, 25), (0.75, 25), (1, 0)]))))

# ---- Tornado Intercept Vehicle --------------------------------------------------------------------------------
tiv = P('body', (0, 17, 0), boxes=[Box(-13, -9, -20, 26, 9, 40, 'armor'), Box(-12, -12, -20, 24, 3, 12, 'hood'), Box(-11, -8, -21, 22, 6, 1, 'grille'),
                                  Box(-12.5, -9.5, 10, 25, 1, 10, 'armor2'), Box(-13.5, -4, -19, 1, 4, 38, 'metal'), Box(12.5, -4, -19, 1, 4, 38, 'metal'),
                                  Box(-10, -7, -21.4, 3, 2, 1, 'light'), Box(7, -7, -21.4, 3, 2, 1, 'light'),
                                  Box(-11, -7, 20.2, 3, 2, 1, 'tail_light'), Box(8, -7, 20.2, 3, 2, 1, 'tail_light')])
tiv.add(P('plow', (0, -2, -21), (0.5, 0, 0), [Box(-14, -4, -3, 28, 5, 3, 'metal')]))
for i, (x, z) in enumerate(((-12, -8), (11, -8), (-12, 9), (11, 9), (-12, 0.5), (11, 0.5))):
    tiv.add(P(f'pillar{i}', (x + 0.5, -9, z), boxes=[Box(-1, -14, -1, 2, 14, 2, 'armor2')]))
tiv.add(P('roof', (0, -23, 0.5), boxes=[Box(-13, -2, -10, 26, 2, 21, 'armor'), Box(-11, -3, -6, 22, 1, 14, 'armor2'),
                                         Box(-9, -3.6, -10.4, 18, 1, 2, 'beacon')]).add(
    P('mast', (0, -3, 7), boxes=[Box(-0.5, -9, -0.5, 1, 9, 1, 'metal')]).add(
        P('anemometer', (0, -9, 0), boxes=[Box(-0.5, -1, -0.5, 1, 1, 1, 'metal')] + [Box(-0.5 + dx * 3, -1.5, -0.5 + dz * 3, 1, 2, 1, 'cup')
                                                                                       for dx, dz in ((1, 0), (-0.5, 0.86), (-0.5, -0.86))]),
        P('vane', (0, -6, 0), boxes=[Box(-0.5, -0.5, -3, 1, 1, 6, 'metal'), Box(-0.5, -1.5, 2, 1, 3, 2, 'hood')])),
    P('dome', (-7, -3, 3), boxes=[Box(-2.5, -3, -2.5, 5, 3, 5, 'dome'), Box(-1.5, -4, -1.5, 3, 1, 3, 'dome')])))
for i, (x, z) in enumerate(((-6.5, -11), (6.5, -11))):
    tiv.add(P(f'slat{i}', (x, -15, -8.9), boxes=[Box(-5.5, 0, -0.5, 11, 1, 1, 'armor2')]))
for side, sx in (('left', 13), ('right', -13)):
    tiv.add(P(f'{side}_door_bar', (sx * 0.985, -15, 1), boxes=[Box(-0.5, 0, -8, 1, 1, 16, 'armor2')]))
wheels = []
for nm, x, z in (('wheel_fl', 12.5, -12), ('wheel_fr', -12.5, -12), ('wheel_bl', 12.5, 12), ('wheel_br', -12.5, 12)):
    wheels.append(P(nm, (x, 19.5, z), boxes=[Box(-2, -4.5, -4.5, 4, 9, 9, 'tire'), Box(-2.5 if x > 0 else -2.5, -2, -2, 5, 4, 4, 'hub')]))
anchors = [P(f'anchor{i}', (x, 15, z), boxes=[Box(-1, 0, -1, 2, 6, 2, 'metal'), Box(-1.5, 5, -1.5, 3, 1, 3, 'metal')]) for i, (x, z) in
           enumerate(((10, -17), (-10, -17), (10, 17), (-10, 17)))]
M.append(Model('InterceptVehicleModel', 'intercept_vehicle', {'armor': ('decal', C('#4a5058'), C('#e07a2a')), 'armor2': ('metal', C('#3a3f46'), C('#20242a')),
    'hood': ('decal', C('#e07a2a'), C('#f2f2f2')), 'grille': ('grille', C('#2a2e34'), C('#101214')), 'metal': ('metal', C('#8a929a'), C('#3a4048')),
    'light': ('glow', C('#fffbe0'), C('#e0c060')), 'tail_light': ('glow', C('#ff6a5a'), C('#a01a10')), 'beacon': ('glow', C('#ffb84a'), C('#c06010')),
    'cup': ('shell', C('#d8dde2'), C('#6a7078')), 'dome': ('shell', C('#e8ecf0'), C('#9aa4ae')), 'tire': ('tread', C('#2a2a2c'), C('#101010')),
    'hub': ('metal', C('#b0b8c0'), C('#505860'))}, [tiv, *wheels, *anchors], [
    Clip('time', 30, T(body_py=[(0, 0), (0.5, 0.15)]), 'moveW'),
    hurt_clip('body', None, 0.3)],
    doc='The Tornado Intercept Vehicle: an armoured chase truck with a storm-orange hood, a plow, slit-armoured windows, a roof beacon bar, a radar dome and an instrument mast whose anemometer spins in the wind; four hydraulic anchors drop when it is parked.',
    extra='''        float roll = s.walkAnimationPos * 1.1f;
        wheelFl.xRot = roll; wheelFr.xRot = roll; wheelBl.xRot = roll; wheelBr.xRot = roll;
        wheelFl.yRot = Mth.clamp(-turn * 0.08f, -0.5f, 0.5f); wheelFr.yRot = wheelFl.yRot;
        anemometer.yRot = t * (0.35f + s.speed * 3f);
        vane.yRot = Mth.sin(t * 0.02f) * 0.3f;
        body.zRot += Mth.clamp(turn * 0.01f, -0.06f, 0.06f);
        for (ModelPart a : new ModelPart[] {anchor0, anchor1, anchor2, anchor3}) { a.visible = s.hide > 0.02f; a.y += s.hide * 4f; }
'''))

# ---- Weather Balloon ------------------------------------------------------------------------------------------
bal = P('balloon', (0, -6, 0), boxes=[Box(-6, -6, -6, 12, 12, 12, 'latex'), Box(-7, -4, -4, 14, 8, 8, 'latex'), Box(-4, -4, -7, 8, 8, 14, 'latex'),
                                       Box(-4, -7, -4, 8, 14, 8, 'latex'), Box(-2, 6.5, -2, 4, 3, 4, 'latex2'), Box(-1, 9, -1, 2, 2, 2, 'latex2')])
string = P('string', (0, 11, 0), boxes=[Box(-0.5, 0, -0.5, 1, 8, 1, 'cord')]).add(
    P('string2', (0, 8, 0), boxes=[Box(-0.5, 0, -0.5, 1, 7, 1, 'cord')]).add(
        P('sonde', (0, 7, 0), boxes=[Box(-1.5, 0, -1, 3, 4, 2, 'sonde'), Box(-0.5, 4, -0.5, 1, 3, 1, 'cord'), Box(-1.2, 1, -1.3, 2, 1, 1, 'light')])))
bal.add(string)
M.append(Model('WeatherBalloonModel', 'weather_balloon', {'latex': ('skin', C('#f4f2ee'), C('#d8d4cc')), 'latex2': ('skin', C('#e0dcd4'), C('#a8a49c')),
    'cord': ('dark', C('#c8c0a8')), 'sonde': ('decal', C('#f2f2f2'), C('#e04a3a')), 'light': ('glow', C('#ff5a4a'), C('#a01a10'))}, [bal], [
    Clip('time', 50, T(balloon_rz=even(4, -4), balloon_rx=shift(even(3, -3), 0.25), string_rz=shift(even(-6, 6), 0.1), string2_rz=shift(even(-8, 8), 0.2),
                       sonde_rz=shift(even(-10, 10), 0.3), sonde_ry=even(0, 20, 0, -20)), '1f')],
    doc='The Weather Balloon: a latex balloon that swells as it climbs, trailing a radiosonde with a blinking light on a swaying cord.',
    grounded=False, extra='        float g = Math.min(1f, s.ageInTicks / 600f) * 0.7f;\n        balloon.xScale += g; balloon.yScale += g; balloon.zScale += g;\n'))

# ================================================================================================ 5.3 Skyward
# ---- Squall Serpent -------------------------------------------------------------------------------------------
sq = P('body', (0, 12, -2), boxes=[Box(-4, -3.5, -1, 8, 7, 8, 'scale'), Box(-3.5, 2.5, -0.5, 7, 1.5, 7, 'belly'), Box(-0.5, -6, 0, 1, 3, 6, 'fin')])
shead = P('head', (0, -0.5, -1), boxes=[Box(-4, -3.5, -8, 8, 6, 8, 'scale'), Box(-3, -2, -12.5, 6, 4, 5, 'scale'), Box(-2.5, -2.6, -12.8, 5, 1, 3, 'scale2'),
                                        Box(-4.3, -2.5, -6.5, 1, 1, 2, 'eye'), Box(3.3, -2.5, -6.5, 1, 1, 2, 'eye'),
                                        Box(-2.5, 1.6, -12, 5, 1, 1, 'tooth'), Box(-4.5, -4.5, -1.5, 9, 8, 1, 'fin'),
                                        Box(-1, -4.2, -11, 2, 1, 7, 'scale2')])
shead.add(P('jaw', (0, 2, -4), boxes=[Box(-3, 0, -8, 6, 2, 8, 'belly'), Box(-2.5, -1, -7.5, 5, 1, 1, 'tooth')]))
shead.add(*pair(P('left_horn', (2.5, -3.5, -2), (-0.65, 0, 0.3), [Box(-0.5, -6, -0.5, 1, 6, 1, 'horn')]).add(
    P('left_horn_tip', (0, -6, 0), (-0.5, 0, 0), [Box(-0.5, -4, -0.5, 1, 4, 1, 'horn'), Box(-0.5, -2, 0.5, 1, 1, 2, 'horn')]))))
shead.add(*pair(P('left_whisker', (3, 0.5, -11.5), (0, 0.6, 0.3), [Box(0, -0.5, -0.5, 4, 1, 1, 'fin')]).add(
    P('left_whisker2', (4, 0, 0), (0, 0.4, 0.2), [Box(0, -0.5, -0.5, 4, 1, 1, 'glow')]))))
sq.add(shead)
sq.add(*pair(P('left_claw', (3.8, 2.5, 3), (0, -0.4, 0.5), [Box(0, -1, -1, 3, 2, 2, 'scale')]).add(
    P('left_talon', (3, 0, 0), (0, 0, 0.8), [Box(0, -0.5, -1.5, 2, 1, 3, 'horn')]))))
prev, seg_tr, segs = sq, {}, []
for i in range(6):
    k = 1 - i * 0.11
    w, h = 8 * k, 7 * k
    seg = P(f'seg{i}', (0, 0, 7 if i == 0 else 8 * (1 - (i - 1) * 0.11)), boxes=[
        Box(-w / 2, -h / 2, 0, w, h, 8 * k + 0.4, 'scale'), Box(-w / 2 + 0.5, h / 2 - 1, 0.5, w - 1, 1.5, 8 * k - 0.6, 'belly'),
        Box(-0.5, -h / 2 - 2.5 * k, 1, 1, 2.5 * k, 6 * k, 'fin')])
    prev.add(seg); prev = seg; segs.append(seg.field)
    seg_tr[(f'seg{i}', 'ry')] = shift(even(9 + i * 1.5, -(9 + i * 1.5)), 0.12 * (i + 1))
    seg_tr[(f'seg{i}', 'rx')] = shift(even(4, -4), 0.12 * (i + 1) + 0.25)
prev.add(P('tail_fin', (0, 0, 8 * (1 - 5 * 0.11)), boxes=[Box(-4, -0.5, 0, 8, 1, 6, 'fin'), Box(-0.5, -3, 0, 1, 6, 5, 'fin'), Box(-0.5, -1, 4, 1, 2, 3, 'glow')]))
seg_tr.update(sym(T(body_py=even(0, -1.2, 0, 1.2), body_ry=even(-5, 5), head_ry=shift(even(4, -4), 0.05), jaw_rx=even(2, 6),
                    left_whisker_rz=shift(even(-10, 10), 0.1), left_whisker2_rz=shift(even(-14, 14), 0.25), left_whisker_ry=shift(even(6, -6), 0.2),
                    left_claw_rz=even(10, -6), left_talon_rz=shift(even(10, -10), 0.2), tail_fin_ry=shift(even(18, -18), 0.85))))
M.append(Model('SquallSerpentModel', 'squall_serpent', {'scale': ('dscale', C('#3c4c80'), C('#141a3a')), 'scale2': ('dscale', C('#55689e'), C('#20284e')),
    'belly': ('scale', C('#a8b4d4'), C('#525e80')), 'fin': ('crystal', C('#9fe8ff'), C('#3a6ab0')), 'horn': ('bone', C('#dfe3ee'), C('#6a7090')),
    'tooth': ('bone', C('#f4f2ea'), C('#a09e90')), 'eye': ('eye', C('#ffe070'), C('#120c04')), 'glow': ('glow', C('#eafaff'), C('#6ad0ff'))}, [sq], [
    Clip('time', 44, seg_tr, '1f'),
    Clip('fidget', 220, T(head_rx=[(0, 0), (0.2, -35), (0.75, -35), (1, 0)], jaw_rx=[(0, 0), (0.2, 50), (0.75, 50), (1, 0)],
                          body_rx=[(0, 0), (0.2, -20), (0.75, -20), (1, 0)], seg0_rx=[(0, 0), (0.2, 15), (0.75, 15), (1, 0)]), '1f', dur=36),
    Clip('attack', 12, T(head_rx=[(0, 0), (0.3, -25), (0.55, 20), (1, 0)], jaw_rx=[(0, 0), (0.3, 55), (0.55, 0), (1, 0)],
                         body_pz=[(0, 0), (0.3, 3), (0.55, -6), (1, 0)], seg0_rx=[(0, 0), (0.3, -12), (0.55, 10), (1, 0)])),
    Clip('cast', 30, T(body_rx=[(0, 0), (0.3, -45), (0.8, -45), (1, 0)], seg0_rx=[(0, 0), (0.3, 30), (0.8, 30), (1, 0)], seg1_rx=[(0, 0), (0.3, 18), (0.8, 18), (1, 0)],
                       jaw_rx=[(0, 0), (0.3, 60), (0.8, 60), (1, 0)], left_horn_rx=[(0, 0), (0.3, -10), (1, 0)], right_horn_rx=[(0, 0), (0.3, -10), (1, 0)])),
    hurt_clip('body', 'head', 1.0)],
    doc='The Squall Serpent: a horned sky-dragon three blocks long that swims through the air on a travelling wave down six tapering segments, with glowing crystal fins, whiskers that trail and a forked tail fin; it rears and gapes to spit ball lightning and lunges to bite.',
    grounded=False, look=[('head', 0.7, 0.6)],
    extra='        ' + ' '.join(f'{f}.yRot += turn * {0.012 * (j + 1):.3f}f;' for j, f in enumerate(segs)) + '\n'))

# ---- Hailstone Golem ------------------------------------------------------------------------------------------
gb = P('body', (0, 10, 0), boxes=[Box(-6, -12, -4, 12, 12, 8, 'ice'), Box(-7, -12.5, -4.5, 14, 5, 9, 'hail'), Box(-2.5, -9.5, -4.4, 5, 5, 1, 'core'),
                                  Box(-5, -3, -3.5, 10, 4, 7, 'ice2'), Box(4, -6, -4.6, 2, 2, 1, 'hail'), Box(-6.4, -5, 1, 2, 3, 2, 'hail'),
                                  Box(-3, -15, -2, 6, 3, 4, 'hail')])
gb.add(P('head', (0, -13, -1.5), boxes=[Box(-3.5, -6, -3.5, 7, 6, 7, 'ice2'), Box(-2.6, -3.6, -3.7, 2, 1, 1, 'eye'), Box(0.6, -3.6, -3.7, 2, 1, 1, 'eye'),
                                         Box(-2, -2, -3.8, 4, 1, 1, 'dark'), Box(-4, -7, -4, 8, 2, 8, 'hail')]).add(
    *[P(f'icicle{i}', (x, -7, z), boxes=[Box(-0.5, -h, -0.5, 1, h, 1, 'icicle')]) for i, (x, z, h) in enumerate(((-3, -2, 3), (0, -3, 4), (3, -1, 3), (-1, 2, 2), (2, 2, 3)))]))
for sd in ('left',):
    gb.add(*pair(P('left_arm', (7, -10, 0), (0, 0, -0.12), [Box(0, -2.5, -3, 5, 10, 6, 'ice'), Box(-0.5, -3, -3.5, 6, 4, 7, 'hail')]).add(
        P('left_forearm', (2.5, 7.5, 0), boxes=[Box(-2.5, 0, -2.5, 5, 9, 5, 'ice2'), Box(-3.5, 7, -3.5, 7, 6, 7, 'hail'), Box(-1, 3, -2.8, 2, 2, 1, 'icicle')]))))
legs = [P(f'{sd}_leg', (3.4 * sg, 10, 0), boxes=[Box(-3, 0, -3, 6, 9, 6, 'ice'), Box(-3.5, 9, -3.5, 7, 5, 7, 'hail')]) for sd, sg in (('left', 1), ('right', -1))]
gwalk = merge(gait_leg('left_leg', None, 0, 22), gait_leg('right_leg', None, 0.5, 22),
              T(left_arm_rx=shift([(0, 18), (0.5, -18)], 0.5), right_arm_rx=[(0, 18), (0.5, -18)], body_rz=even(4, -4), body_ry=even(-6, 6),
                body_py=[(0, 0), (0.25, 0.6), (0.5, 0), (0.75, 0.6)], left_forearm_rx=shift([(0, -10), (0.5, -26)], 0.5), right_forearm_rx=[(0, -10), (0.5, -26)]))
M.append(Model('HailstoneGolemModel', 'hailstone_golem', {'ice': ('crystal', C('#cde6f4'), C('#5f8eb0')), 'ice2': ('crystal', C('#e6f3fb'), C('#86afc8')),
    'hail': ('hail', C('#f0f5f8'), C('#9fb4c4')), 'icicle': ('crystal', C('#f4fbff'), C('#a8d4ee')), 'core': ('glow', C('#effcff'), C('#6ec8ff')),
    'eye': ('glow', C('#c8f6ff'), C('#3a9ad8')), 'dark': ('dark', C('#2c4252'))}, [gb, *legs], [
    Clip('time', 70, sym(T(body_sy=[(0, 0), (0.5, 0.025)], left_arm_rz=even(-3, 2), left_forearm_rx=hold(-12), head_ry=even(0, 6, 0, -6))), 'idleW'),
    Clip('dist', 0.2, gwalk, 'moveW'),
    Clip('fidget', 180, T(head_rz=[(0, 0), (0.15, 14), (0.3, -14), (0.45, 12), (0.6, -10), (0.75, 0)], body_rz=[(0, 0), (0.2, 3), (0.4, -3), (0.6, 0)]), 'idleW', dur=26),
    Clip('cast', 20, sym(T(left_arm_rx=[(0, 0), (0.35, -165), (0.55, -175), (0.75, -40), (1, 0)], left_forearm_rx=[(0, 0), (0.35, -30), (0.6, -40), (0.75, 0)],
                           body_rx=[(0, 0), (0.35, -12), (0.7, 14), (1, 0)]))),
    Clip('attack', 14, sym(T(left_arm_rx=[(0, 0), (0.35, -150), (0.6, -20), (1, 0)], left_forearm_rx=[(0, 0), (0.35, -40), (0.6, 0)],
                             body_rx=[(0, 0), (0.35, -8), (0.6, 18), (1, 0)], head_rx=[(0, 0), (0.6, 10), (1, 0)]))),
    hurt_clip('body', 'head', 0.7)],
    doc='The Hailstone Golem: a lumbering two-and-a-half-block giant of packed ice crusted with hailstones, with a glowing storm core in its chest, an icicle crown and fists like boulders; it heaves both arms overhead to hurl hail and brings them down to smash.',
    look=[('head', 0.8, 0.5)]))

# ---- Aurora Moth ----------------------------------------------------------------------------------------------
am = P('body', (0, 18, 0), boxes=[Box(-2, -2, -3, 4, 4, 5, 'fur'), Box(-2.5, -2.5, -2.5, 5, 2, 3, 'fur2'), Box(-1.5, -1.5, 2, 3, 3, 5, 'fur3'), Box(-1, -1, 6.6, 2, 2, 1, 'glow')])
am.add(P('head', (0, -0.3, -3), boxes=[Box(-2, -2, -2.5, 4, 4, 2.5, 'fur2'), Box(-2.6, -1.6, -2.2, 1.2, 2, 2, 'eye'), Box(1.4, -1.6, -2.2, 1.2, 2, 2, 'eye')]).add(
    *pair(P('left_antenna', (0.8, -2, -2), (-0.6, 0, 0.5), [Box(-0.5, -4, -0.5, 1, 4, 1, 'fur2'), Box(0, -4, -0.5, 2, 3, 1, 'feather')]))))
am.add(*pair(P('left_wing', (1.8, -1.8, -1.5), (0, 0, 0.05), [Box(0, -0.5, -4, 10, 1, 7, 'wing')])))
am.add(*pair(P('left_hindwing', (1.6, -1.4, 1.5), (0, -0.15, 0.1), [Box(0, -0.5, 0, 7, 1, 5, 'wing2')])))
am.add(*pair(P('left_legs', (1.5, 1.8, -1), (0, 0, 0.7), [Box(0, -0.5, -1.5, 2, 1, 3, 'fur3')])))
M.append(Model('AuroraMothModel', 'aurora_moth', {'fur': ('fur', C('#3e3468'), C('#1a1432')), 'fur2': ('fur', C('#ddd6f2'), C('#7c6ca4')),
    'fur3': ('fur', C('#5a4a8a'), C('#2a2048')), 'feather': ('feather', C('#e8e0ff'), C('#8a7ac0')),
    'wing': ('mothwing', C('#86f4cc'), C('#a05ae8')), 'wing2': ('mothwing', C('#ff92dc'), C('#48c4ea')), 'eye': ('glow', C('#c0fff0'), C('#38c0a0')),
    'glow': ('glow', C('#e8fff4'), C('#7af0c0'))}, [am], [
    Clip('time', 7, sym(T(left_wing_rz=[(0, -46), (0.5, 38)], left_hindwing_rz=shift([(0, -40), (0.5, 32)], 0.12), body_py=[(0, 0.5), (0.5, -0.5)])), '1f'),
    Clip('time', 40, sym(T(body_rx=even(-6, 4), left_antenna_rx=shift(even(-8, 6), 0.2), left_antenna_rz=even(4, -4))), '1f'),
    Clip('fidget', 120, sym(T(left_wing_rz=[(0, 0), (0.2, 30), (0.8, 30), (1, 0)], left_hindwing_rz=[(0, 0), (0.2, 26), (0.8, 26), (1, 0)])), '1f', dur=28),
    hurt_clip('body', 'head', 0.8)],
    doc='The Aurora Moth: a plush violet moth with feathered antennae and four sheer wings that shimmer from aurora green to rose; it flutters round lanterns at night and now and then glides with its wings held high.',
    grounded=False, translucent=True, look=[('head', 0.8, 0.8)]))

# ---- Stormfly -------------------------------------------------------------------------------------------------
sf = P('body', (0, 20, 0), boxes=[Box(-1, -1, -1.5, 2, 2, 3, 'shell'), Box(-0.8, -0.8, 1.2, 1.6, 1.6, 2.5, 'glow')])
sf.add(P('head', (0, 0, -1.5), boxes=[Box(-1, -1, -1.5, 2, 2, 1.5, 'shell2'), Box(-1.2, -0.8, -1.4, 0.6, 1, 1, 'eye'), Box(0.6, -0.8, -1.4, 0.6, 1, 1, 'eye')]).add(
    *pair(P('left_antenna', (0.5, -1, -1.2), (-0.7, 0, 0.4), [Box(-0.5, -2, -0.5, 1, 2, 1, 'shell2')]))))
sf.add(*pair(P('left_wing', (0.6, -1, -0.5), (0, 0.25, 0), [Box(0, -0.5, -1, 3.5, 1, 2.5, 'wing')])))
sf.add(*pair(P('left_elytron', (0.5, -1, -1), (0, -0.2, -0.35), [Box(0, -0.5, 0, 1.5, 1, 3, 'shell')])))
M.append(Model('StormflyModel', 'stormfly', {'shell': ('shell', C('#2e3a52'), C('#121826')), 'shell2': ('shell', C('#465670'), C('#1c2434')),
    'wing': ('jelly', C('#e0f4ff'), C('#80b8e0')), 'eye': ('eye', C('#cfe8ff'), C('#0a1018')), 'glow': ('glow', C('#fbffd8'), C('#a8f0ff'))}, [sf], [
    Clip('time', 2, sym(T(left_wing_rz=[(0, -50), (0.5, 40)])), '1f'),
    Clip('time', 30, sym(T(body_py=even(0, -0.6, 0, 0.6), body_rx=even(-10, -4), left_antenna_rz=even(6, -6))), '1f'),
    hurt_clip('body', None, 0.5)],
    doc='The Stormfly: a firefly whose tail glows with stored lightning, on two buzzing wings under raised wing cases.',
    grounded=False, translucent=True))

# ================================================================================================ 5.4 Thunderfront
# ---- Rainfrog -------------------------------------------------------------------------------------------------
rf = P('body', (0, 21, 0.5), (-0.12, 0, 0), [Box(-3, -2.5, -3.5, 6, 4, 7, 'skin'), Box(-2.5, 1.2, -3, 5, 1, 6, 'belly'), Box(-2, -3, -1, 4, 1, 4, 'skin2')])
rhead = P('head', (0, -0.8, -3.5), (0.12, 0, 0), [Box(-3.5, -2.5, -4, 7, 3, 4, 'skin'), Box(-3.2, -4, -3, 2.2, 1.6, 2.2, 'skin2'), Box(1, -4, -3, 2.2, 1.6, 2.2, 'skin2'),
                                                  Box(-3, -4.4, -2.8, 1.8, 1.4, 1.8, 'eye'), Box(1.2, -4.4, -2.8, 1.8, 1.4, 1.8, 'eye')])
rhead.add(P('jaw', (0, 0.4, -0.5), boxes=[Box(-3.5, 0, -3.6, 7, 1, 3.8, 'belly')]),
          P('sac', (0, 1.2, -2.2), boxes=[Box(-2, -0.4, -1.5, 4, 1.6, 3, 'sac')]))
rf.add(rhead)
rf.add(*pair(P('left_arm', (2.4, 0.8, -2.6), (0.2, 0, -0.25), [Box(-0.5, 0, -0.5, 1, 3, 1, 'skin')]).add(
    P('left_hand', (0, 3, 0), boxes=[Box(-1, -0.3, -1.6, 2.2, 0.6, 2, 'toe')]))))
rf.add(*pair(P('left_thigh', (2.6, 0.2, 2.2), (0, -0.25, 0), [Box(-0.5, -1.2, -1.5, 2.2, 2.4, 4, 'skin')]).add(
    P('left_shin', (0.6, 0.6, 2.3), (0.5, 0, 0), [Box(-0.6, -0.6, -4, 1.4, 1.2, 4, 'skin2')]).add(
        P('left_foot', (0, 0.4, -4), (-0.5, 0, 0), [Box(-1, 0, -0.5, 2.6, 0.6, 3.2, 'toe')])))))
hop = sym(T(body_py=[(0, 0), (0.12, 0.4), (0.4, -3.2), (0.7, -0.4), (0.82, 0.3), (1, 0)], body_rx=[(0, 0), (0.12, 6), (0.4, -12), (0.75, 6), (1, 0)],
            left_thigh_rx=[(0, 0), (0.12, -12), (0.3, 55), (0.62, 20), (0.85, -6), (1, 0)], left_shin_rx=[(0, 0), (0.12, 10), (0.3, -60), (0.62, -15), (1, 0)],
            left_foot_rx=[(0, 0), (0.3, 50), (0.62, 10), (1, 0)], left_arm_rx=[(0, 0), (0.25, -40), (0.55, -60), (0.78, 20), (1, 0)],
            head_rx=[(0, 0), (0.4, -6), (0.8, 4), (1, 0)]))
croak = T(sac_s=[(0, 0), (0.25, 0.95), (0.45, 0.3), (0.65, 1.05), (0.88, 0.2), (1, 0)], jaw_rx=[(0, 0), (0.25, 4), (0.45, 0), (0.65, 4), (1, 0)],
          head_rx=[(0, 0), (0.2, -10), (0.85, -10), (1, 0)], body_sy=[(0, 0), (0.25, 0.06), (0.45, 0), (0.65, 0.06), (1, 0)])
M.append(Model('RainfrogModel', 'rainfrog', {'skin': ('skin', C('#3fae8a'), C('#1d5c47')), 'skin2': ('skin', C('#55c49e'), C('#24705a')),
    'belly': ('scale', C('#eae2b0'), C('#a89a66')), 'sac': ('skin', C('#c8eef6'), C('#7ab8d0')), 'toe': ('skin', C('#2f8a6c'), C('#f2c94c')),
    'eye': ('eye', C('#f2c94c'), C('#c8962c'))}, [rf], [
    Clip('time', 50, T(body_sy=[(0, 0), (0.5, 0.03)], sac_s=[(0, 0), (0.5, 0.25)]), 'idleW'),
    Clip('dist', 0.45, hop, 'moveW'),
    Clip('cast', 26, croak),
    Clip('fidget', 140, T(head_ry=[(0, 0), (0.2, 25), (0.7, 25), (1, 0)], left_arm_rz=[(0, 0), (0.3, -20), (0.6, 0)]), 'idleW', dur=24),
    hurt_clip('body', 'head', 0.8)],
    doc='The Rainfrog: a blue-green tree frog with bulging golden eyes, spotted skin and a pale throat sac; it hops with its long hind legs kicking out behind, and croaks with its throat puffing up twice.',
    look=[('head', 0.6, 0.3)]))

# ---- Storm Petrel ---------------------------------------------------------------------------------------------
bird('StormPetrelModel', 'storm_petrel', {'feather': ('feather', C('#2c2a30'), C('#141218')), 'feather2': ('feather', C('#3a3740'), C('#18161c')),
     'belly': ('feather', C('#46424c'), C('#221f26')), 'beak': ('shell', C('#1c1a1e'), C('#0a0a0c')), 'beak2': ('shell', C('#26242a'), C('#0e0e10')),
     'eye': ('eye', C('#3a3030'), C('#060606')), 'tail': ('feather', C('#f2f2f2'), C('#2c2a30')), 'tip': ('feather', C('#24222a'), C('#0e0c12')),
     'leg': ('scale', C('#1c1a1e'), C('#3a6ab0'))}, 0.75, False,
     'The Storm Petrel: a small sooty seabird with a bright white rump band and long wings; it flutters low over the waves, feet dangling to patter on the water.')

# ---- Dust Devil -----------------------------------------------------------------------------------------------
dd_parts, dd_spin = [], []
for i in range(7):
    w = 2.2 + i * 1.55; h = 7.6; y = 24 - i * 7.2
    ring = P(f'ring{i}', (0, y - h / 2, 0), boxes=[Box(-w, -h / 2, -w, 2 * w, h, 1.6, 'sand' if i % 2 else 'sand2'),
                                                   Box(-w, -h / 2, w - 1.6, 2 * w, h, 1.6, 'sand2' if i % 2 else 'sand'),
                                                   Box(-w, -h / 2, -w + 1.6, 1.6, h, 2 * w - 3.2, 'sand'), Box(w - 1.6, -h / 2, -w + 1.6, 1.6, h, 2 * w - 3.2, 'sand2')])
    for k in range(2 + i // 2):
        a = k * 2.4 + i
        ring.add(P(f'grit{i}_{k}', (math.cos(a) * (w + 1.2), (k % 3 - 1) * 2.2, math.sin(a) * (w + 1.2)), boxes=[Box(-0.6, -0.6, -0.6, 1.2, 1.2, 1.2, 'grit')]))
    dd_parts.append(ring); dd_spin.append(ring.field)
M.append(Model('DustDevilModel', 'dust_devil', {'sand': ('cloud', C('#e6d2a0'), C('#b0905a')), 'sand2': ('cloud', C('#d8bc84'), C('#9a7a48')),
    'grit': ('stone', C('#a88a5a'), C('#5a4a30'))}, dd_parts, [
    Clip('time', 30, {**{(f'ring{i}', 'sx'): shift(even(0.06, -0.06), i * 0.11) for i in range(7)},
                      **{(f'ring{i}', 'sz'): shift(even(-0.06, 0.06), i * 0.11) for i in range(7)}}, '1f')],
    doc='The Dust Devil: a whirling column of sand that widens as it climbs, seven bands turning at different speeds and swaying like a rope, with grit flung round its edge.',
    grounded=False, translucent=True,
    extra=''.join(f'        {f}.yRot += t * {0.62 - j * 0.06:.2f}f; {f}.x += Mth.sin(t * 0.09f + {j * 0.7:.1f}f) * {j * 0.55:.2f}f; {f}.z += Mth.cos(t * 0.07f + {j * 0.5:.1f}f) * {j * 0.4:.2f}f;\n'
                  for j, f in enumerate(dd_spin))))

MOB_TEXTURES = {m.texture for m in M}


def main():
    JAVA.mkdir(parents=True, exist_ok=True)
    for old in JAVA.glob('*.java'): old.unlink()
    for m in M:
        ground(m)
        W, H = paint(m)
        (JAVA / f'{m.cls}.java').write_text(java(m, W, H))
    print(f'mobmodels: {len(M)} models, {sum(len(list(all_parts(m.parts))) for m in M)} parts, '
          f'{sum(len(c.tracks) for m in M for c in m.clips)} animation tracks')


if __name__ == '__main__':
    main()
