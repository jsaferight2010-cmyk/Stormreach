"""Stormreach 5.1 texture remaster: every block, item, mob and armour texture redrawn at high resolution.

Blocks are repainted at 64x by material: bricks get real mortar, bevelled bricks and cracks; stone gets strata and
pits; ores get faceted gems; crystal blocks are cut into lit facets; logs get bark and growth rings; planks get
grain and nail holes; leaves are drawn leaf by leaf; turf gets blades and an overhanging fringe; clouds billow; lamps
glow; metal is brushed and riveted. The colours of each block come from its original 16x design (darkest, middle,
lightest and accent tones), so everything keeps its identity and the storm palette.

Everything that is a picture rather than a material (items, plants, machines, altars, trophies, mobs, armour) is
remastered with an edge-aware 4x upscale (EPX twice: diagonals come out smooth instead of stair-stepped), then lit:
a soft volume shade from the top left, rim highlights, a darker outline, and fine surface grain.

Textures already drawn at 64x (the 5.1 radar, vault, locks, lamps, hail, puddles) and the smooth procedural ones
(tornado, clouds, hailstones) are left alone. Runs last in run_all.py, on the freshly generated 16x textures.
"""
import glob, os, pathlib, random
import numpy as np
from PIL import Image

ROOT = pathlib.Path(__file__).resolve().parent.parent
TEX = ROOT / 'src' / 'main' / 'resources' / 'assets' / 'stormreach' / 'textures'
N = 64
SKIP_ENTITY = {'tornado_funnel', 'tornado_debris', 'tornado_wallcloud', 'storm_cloud', 'hailstone', 'tornado'}
try:  # the rebuilt mob models paint their own textures at 4x (tools/mobmodels.py)
    import sys as _sys, pathlib as _pl
    _sys.path.insert(0, str(_pl.Path(__file__).resolve().parent))
    from mobmodels import MOB_TEXTURES as _MOBS
    SKIP_ENTITY |= set(_MOBS)
except ImportError:
    pass


# ------------------------------------------------------------------------------------------------ helpers
def rng_for(name):
    return np.random.default_rng(sum((i + 1) * ord(c) for i, c in enumerate(name)))


def lum(c):
    return 0.299 * c[..., 0] + 0.587 * c[..., 1] + 0.114 * c[..., 2]


def palette(src):
    """dark, mid, light and accent colours (float RGB 0..255) from a source texture's opaque pixels."""
    a = np.asarray(src.convert('RGBA'), dtype=np.float64).reshape(-1, 4)
    a = a[a[:, 3] > 0][:, :3]
    if len(a) == 0: a = np.array([[128.0, 128, 128]])
    L = lum(a)
    order = np.argsort(L)
    dark = a[order[int(len(a) * 0.12)]]
    mid = np.median(a, axis=0)
    light = a[order[min(len(a) - 1, int(len(a) * 0.9))]]
    sat = a.max(1) - a.min(1)
    accent = a[np.argmax(sat + L * 0.15)]
    if sat.max() < 40: accent = light
    return dark, mid, light, accent


def value_noise(rng, cells, size=N):
    """Tileable smooth noise in 0..1."""
    g = rng.random((cells, cells))
    x = np.arange(size) / size * cells
    i0 = np.floor(x).astype(int) % cells
    i1 = (i0 + 1) % cells
    f = x - np.floor(x)
    f = f * f * (3 - 2 * f)
    rows = g[i0][:, i0] * (1 - f)[None, :] + g[i0][:, i1] * f[None, :]
    rows2 = g[i1][:, i0] * (1 - f)[None, :] + g[i1][:, i1] * f[None, :]
    return rows * (1 - f)[:, None] + rows2 * f[:, None]


def fbm(rng, octaves=((4, 1.0), (8, 0.5), (16, 0.25), (32, 0.12)), size=N):
    out = sum(value_noise(rng, c, size) * w for c, w in octaves)
    return out / sum(w for _, w in octaves)


def mixc(a, b, t):
    t = np.asarray(t)[..., None] if np.ndim(t) else t
    return a * (1 - t) + b * t


def to_img(rgb, alpha=None):
    rgb = np.clip(rgb, 0, 255).astype(np.uint8)
    if alpha is None: alpha = np.full(rgb.shape[:2], 255, np.uint8)
    return Image.fromarray(np.dstack([rgb, np.clip(alpha, 0, 255).astype(np.uint8)]), 'RGBA')


def shade(rgb, k):
    return rgb * np.asarray(k)[..., None]


# ------------------------------------------------------------------------------------------------ EPX remaster
def epx2(a):
    """Scale2x/EPX on an RGBA array: smooth diagonals, crisp edges."""
    h, w = a.shape[:2]
    P = a
    A = np.roll(P, 1, 0); A[0] = P[0]
    B = np.roll(P, -1, 1); B[:, -1] = P[:, -1]
    C = np.roll(P, 1, 1); C[:, 0] = P[:, 0]
    D = np.roll(P, -1, 0); D[-1] = P[-1]
    eq = lambda x, y: np.all(x == y, axis=-1)
    o1 = np.where((eq(C, A) & ~eq(C, D) & ~eq(A, B))[..., None], A, P)
    o2 = np.where((eq(A, B) & ~eq(A, C) & ~eq(B, D))[..., None], B, P)
    o3 = np.where((eq(D, C) & ~eq(D, B) & ~eq(C, A))[..., None], C, P)
    o4 = np.where((eq(B, D) & ~eq(B, A) & ~eq(D, C))[..., None], D, P)
    out = np.zeros((h * 2, w * 2, 4), a.dtype)
    out[0::2, 0::2] = o1; out[0::2, 1::2] = o2; out[1::2, 0::2] = o3; out[1::2, 1::2] = o4
    return out


def remaster(src, scale, name, outline=True, lighting=True):
    """Edge-aware upscale plus lighting: for pictures (items, plants, machines, mobs)."""
    a = np.asarray(src.convert('RGBA'))
    s = 1
    while s < scale:
        a = epx2(a); s *= 2
    a = a.astype(np.float64)
    rgb, al = a[..., :3], a[..., 3]
    rng = rng_for(name)
    h, w = al.shape
    solid = al > 0
    if lighting:
        # volume: distance-to-edge shading lit from the top left, so flat sprites read as rounded objects
        from scipy.ndimage import distance_transform_edt, shift
        d = distance_transform_edt(solid)
        depth = np.clip(d / max(2.0, scale * 1.5), 0, 1)
        up = shift(solid.astype(float), (scale * 0.75, scale * 0.75), order=0, cval=0)
        down = shift(solid.astype(float), (-scale * 0.75, -scale * 0.75), order=0, cval=0)
        lit = (solid & (up < 0.5))          # edges facing the light
        shaded = (solid & (down < 0.5))     # edges facing away
        k = 0.92 + 0.1 * depth
        k = np.where(lit, k * 1.14, k)
        k = np.where(shaded, k * 0.8, k)
        # colour regions: soften inner seams a little so 4x4 cells don't show
        grain = 1 + (fbm(rng, ((max(2, w // 8), 1.0), (max(4, w // 4), 0.5)), size=max(w, h))[:h, :w] - 0.5) * 0.08
        rgb = shade(rgb, k * grain)
        if outline:
            from scipy.ndimage import binary_erosion
            edge = solid & ~binary_erosion(solid, iterations=max(1, scale // 2))
            rgb = np.where(edge[..., None], rgb * 0.72, rgb)
    return to_img(rgb, al)


# ------------------------------------------------------------------------------------------------ block materials
def bricks(src, name, rows=4, cols=2, cracked=False, chiseled=False):
    dark, mid, light, accent = palette(src)
    rng = rng_for(name)
    tex = fbm(rng)
    y, x = np.mgrid[0:N, 0:N]
    bh, bw = N // rows, N // cols
    row = y // bh
    xo = (x + (row % 2) * (bw // 2)) % N
    col = xo // bw
    ly, lx = y % bh, xo % bw
    mortar = (ly < 2) | (lx < 2)
    brick_id = row * 10 + col
    tint = np.vectorize(lambda i: 0.88 + 0.24 * ((i * 2654435761) % 1000) / 1000)(brick_id)
    base = mixc(mid, light, tex * 0.6) * tint[..., None]
    # bevel: lit top-left rim, shaded bottom-right rim of each brick
    k = np.ones((N, N))
    k = np.where(((ly >= 2) & (ly < 4)) | ((lx >= 2) & (lx < 4)), 1.13, k)
    k = np.where((ly >= bh - 2) | (lx >= bw - 2), 0.8, k)
    pits = rng.random((N, N)) < 0.025
    k = np.where(pits, 0.75, k)
    rgb = shade(base, k)
    rgb = np.where(mortar[..., None], mixc(dark * 0.75, dark, tex), rgb)
    if cracked:
        for _ in range(5):
            cx, cy = rng.integers(0, N, 2)
            for _ in range(rng.integers(10, 24)):
                rgb[cy % N, cx % N] = dark * 0.55
                cx += rng.integers(-1, 2); cy += rng.integers(0, 2)
    if chiseled:
        rgb = chisel(rgb, dark, light, accent, rng)
    return to_img(rgb)


def chisel(rgb, dark, light, accent, rng):
    """A carved frame with a recessed panel and a storm glyph in the middle."""
    y, x = np.mgrid[0:N, 0:N]
    m = np.minimum(np.minimum(x, y), np.minimum(N - 1 - x, N - 1 - y))
    rgb = rgb.copy()
    rgb[m < 3] = shade(rgb[m < 3], 1.15)
    rgb[(m >= 3) & (m < 5)] = dark
    inner = m >= 10
    rgb[inner] = shade(rgb[inner], 0.82)
    rgb[(m >= 10) & (m < 11)] = shade(light[None], 1.0)
    # lightning-bolt glyph
    pts = [(36, 14), (28, 30), (35, 30), (27, 50), (40, 27), (33, 27), (39, 14)]
    from PIL import ImageDraw
    im = Image.new('L', (N, N), 0)
    ImageDraw.Draw(im).polygon(pts, fill=255)
    g = np.asarray(im) > 0
    rgb[g] = mixc(accent, np.array([255.0, 255, 255]), 0.25)
    return rgb


def stone(src, name, polished=False, cobbled=False, veins=False):
    dark, mid, light, accent = palette(src)
    rng = rng_for(name)
    n = fbm(rng)
    strata = 0.5 + 0.5 * np.sin(np.mgrid[0:N, 0:N][0] / N * np.pi * 6 + fbm(rng, ((4, 1),)) * 6)
    t = n * 0.75 + strata * 0.25
    t = np.clip((t - t.mean()) * 2.4 + 0.5, 0, 1)        # stretch: real rock has visible light and dark patches
    rgb = mixc(mixc(dark, mid, np.clip(t * 1.6, 0, 1)), light, np.clip((t - 0.62) * 2.6, 0, 1))
    pits = rng.random((N, N)) < 0.03
    rgb[pits] = shade(rgb[pits], 0.78)
    if cobbled:
        # rounded stones from a cellular pattern
        pts = rng.random((22, 2)) * N
        y, x = np.mgrid[0:N, 0:N]
        dists = []
        for px, py in pts:
            for ox in (-N, 0, N):
                for oy in (-N, 0, N):
                    dists.append(np.hypot(x - px - ox, y - py - oy))
        D = np.sort(np.stack(dists), axis=0)
        edge = D[1] - D[0]
        rgb = shade(rgb, np.clip(0.7 + edge / 10, 0.55, 1.15))
        rgb[edge < 1.6] = dark * 0.7
    if polished:
        y, x = np.mgrid[0:N, 0:N]
        m = np.minimum(np.minimum(x, y), np.minimum(N - 1 - x, N - 1 - y))
        rgb = mixc(rgb, mid, 0.55)
        rgb[m < 2] = shade(light[None], 1.05)
        rgb[(m >= 2) & (m < 4)] = shade(mid[None], 0.75)
        rgb[(m >= N - 1)] = dark
    if veins:
        rgb = add_veins(rgb, accent, rng)
    return to_img(rgb)


def add_veins(rgb, accent, rng, count=6):
    glow = np.zeros((N, N))
    for _ in range(count):
        x, y = rng.integers(0, N, 2).astype(float)
        ang = rng.random() * np.pi * 2
        for _ in range(rng.integers(18, 40)):
            ang += (rng.random() - 0.5) * 1.2
            x = (x + np.cos(ang)) % N; y = (y + np.sin(ang)) % N
            glow[int(y), int(x)] = 1
    from scipy.ndimage import gaussian_filter
    halo = gaussian_filter(glow, 1.4, mode='wrap')
    halo /= halo.max() or 1
    rgb = mixc(rgb, accent, np.clip(halo * 0.9, 0, 0.9))
    rgb[glow > 0] = mixc(accent, np.array([255.0, 255, 255]), 0.55)
    return rgb


def ore(src, name, host=None):
    dark, mid, light, accent = palette(src)
    # host rock: the darker, less saturated colours of the ore texture
    rng = rng_for(name)
    base = np.asarray(stone(src, name + '_host'), dtype=np.float64)[..., :3]
    base = mixc(base, np.array([lum(mid)] * 3), 0.35)
    from PIL import ImageDraw
    im = Image.fromarray(base.astype(np.uint8)).convert('RGBA')
    d = ImageDraw.Draw(im)
    for _ in range(9):
        cx, cy = rng.integers(8, N - 8, 2)
        r = rng.integers(6, 11)
        k = rng.integers(5, 7)
        ang0 = rng.random() * 6.28
        pts = [(cx + np.cos(ang0 + i * 6.28 / k) * r, cy + np.sin(ang0 + i * 6.28 / k) * r * 0.85) for i in range(k)]
        d.polygon(pts, fill=tuple(int(v) for v in shade(accent, 0.6)) + (255,))
        # facets: a lit upper-left half and a bright glint
        d.polygon([pts[0], pts[1], pts[2], (cx, cy)], fill=tuple(int(min(255, v * 1.15)) for v in accent) + (255,))
        d.polygon([pts[-1], pts[0], (cx, cy)], fill=tuple(int(v) for v in accent) + (255,))
        d.ellipse([cx - 2, cy - r * 0.6 - 1, cx, cy - r * 0.6 + 1], fill=(255, 255, 255, 230))
    return im


def crystal_block(src, name):
    dark, mid, light, accent = palette(src)
    rng = rng_for(name)
    pts = rng.random((14, 2)) * N
    y, x = np.mgrid[0:N, 0:N]
    best = np.full((N, N), 1e9); second = np.full((N, N), 1e9); idx = np.zeros((N, N), int)
    for i, (px, py) in enumerate(pts):
        for ox in (-N, 0, N):
            for oy in (-N, 0, N):
                dd = np.hypot(x - px - ox, y - py - oy)
                closer = dd < best
                second = np.where(closer, best, np.minimum(second, dd))
                idx = np.where(closer, i, idx); best = np.where(closer, dd, best)
    facet_light = np.array([0.75 + 0.5 * rng.random() for _ in pts])[idx]
    grad = 1 - (x + y) / (2 * N) * 0.3
    rgb = shade(mixc(mid, light, 0.35), facet_light * grad)
    rgb = mixc(rgb, accent, 0.2)
    edge = (second - best) < 1.3
    rgb[edge] = mixc(light, np.array([255.0, 255, 255]), 0.5)
    sparkle = rng.random((N, N)) < 0.006
    rgb[sparkle] = 255
    return to_img(rgb)


def metal_block(src, name):
    dark, mid, light, accent = palette(src)
    rng = rng_for(name)
    y, x = np.mgrid[0:N, 0:N]
    brushed = value_noise(rng, 64)[0][None, :] * 0.5 + fbm(rng) * 0.5   # fine horizontal brushing
    rgb = mixc(mid, light, brushed)
    panel = (x % 32 < 1) | (y % 32 < 1)
    rgb[panel] = dark
    rgb[(x % 32 == 1) | (y % 32 == 1)] = shade(light[None], 1.1)
    for cx in (5, 26, 37, 58):
        for cy in (5, 26, 37, 58):
            rgb[cy - 1:cy + 2, cx - 1:cx + 2] = shade(dark[None], 1.2)
            rgb[cy - 1, cx - 1] = light
    return to_img(mixc(rgb, accent, 0.15))


def log_side(src, name, stripped=False):
    dark, mid, light, accent = palette(src)
    rng = rng_for(name)
    y, x = np.mgrid[0:N, 0:N]
    streak = value_noise(rng, 32)[0][None, :].repeat(N, 0)
    wav = np.sin(x / N * np.pi * (10 if not stripped else 6) + fbm(rng, ((4, 1), (8, 0.5))) * 4)
    t = np.clip(0.5 + 0.35 * wav + (streak - 0.5) * 0.6, 0, 1)
    rgb = mixc(dark, mid if stripped else mixc(mid, light, 0.2), t)
    if not stripped:
        cracks = (np.abs(wav) < 0.12) & (fbm(rng) > 0.45)
        rgb[cracks] = dark * 0.6
        for _ in range(2):   # knots
            cx, cy = rng.integers(10, N - 10, 2)
            r = np.hypot((x - cx) * 1.3, y - cy)
            rgb[r < 4] = mixc(dark, rgb[r < 4], np.clip(r[r < 4] / 4, 0, 1))
    else:
        rgb = mixc(rgb, light, 0.25)
    return to_img(rgb)


def log_top(src, name, stripped=False):
    dark, mid, light, accent = palette(src)
    rng = rng_for(name)
    y, x = np.mgrid[0:N, 0:N]
    r = np.hypot(x - N / 2 + 0.5, y - N / 2 + 0.5) + fbm(rng, ((4, 1), (8, 0.5))) * 4
    rings = 0.5 + 0.5 * np.sin(r * 0.9)
    rgb = mixc(mid, light, rings * 0.6)
    bark = r > N / 2 - 5
    if not stripped:
        rgb[bark] = mixc(dark, mid, fbm(rng)[bark] * 0.5)
    rgb[np.abs(r - (N / 2 - 5)) < 0.8] = dark
    return to_img(rgb)


def planks(src, name):
    dark, mid, light, accent = palette(src)
    rng = rng_for(name)
    y, x = np.mgrid[0:N, 0:N]
    board = y // 16
    offs = (board * 23) % N
    grain = np.sin((y + fbm(rng, ((4, 1), (16, 0.4))) * 6) * 1.3) * 0.5 + 0.5
    t = np.clip(0.25 + 0.5 * grain, 0, 1)
    tint = np.array([0.9, 1.05, 0.95, 1.0])[board % 4]
    rgb = shade(mixc(mid, light, t * 0.55), tint)
    seam = (y % 16 == 0) | (((x + offs) % N) == 0)
    rgb[seam] = dark
    rgb[(y % 16 == 1)] = shade(rgb[(y % 16 == 1)], 1.12)
    rgb[(y % 16 == 15)] = shade(rgb[(y % 16 == 15)], 0.82)
    for b in range(4):   # nail holes
        for nx in ((b * 23 + 4) % N, (b * 23 + 36) % N):
            rgb[b * 16 + 7:b * 16 + 9, nx:nx + 2] = dark * 0.6
    return to_img(rgb)


def leaves(src, name):
    dark, mid, light, accent = palette(src)
    rng = rng_for(name)
    from PIL import ImageDraw
    srca = np.asarray(src.convert('RGBA').resize((N, N), Image.BILINEAR))[..., 3]
    under = to_img(np.broadcast_to(shade(dark, 0.8), (N, N, 3)).copy(), np.where(srca > 100, 255, 0))
    im = under.copy()
    d = ImageDraw.Draw(im)
    for i in range(260):
        cx, cy = rng.random(2) * N
        ang = rng.random() * 6.28
        L, W = 5 + rng.random() * 3, 2.2 + rng.random()
        pts = [(cx + np.cos(ang) * L, cy + np.sin(ang) * L), (cx + np.cos(ang + 1.57) * W, cy + np.sin(ang + 1.57) * W),
               (cx - np.cos(ang) * L * 0.6, cy - np.sin(ang) * L * 0.6), (cx - np.cos(ang + 1.57) * W, cy - np.sin(ang + 1.57) * W)]
        t = rng.random()
        c = mixc(dark, light, t) if rng.random() > 0.03 else mixc(light, accent, 0.5)
        for ox in (-N, 0, N):
            for oy in (-N, 0, N):
                d.polygon([(px + ox, py + oy) for px, py in pts], fill=tuple(int(v) for v in c) + (255,))
    a = np.asarray(im).astype(np.float64)
    # gaps: keep the original's see-through pattern, softened so the holes are leaf-shaped rather than square
    al = np.where(srca < 40, 0, a[..., 3])
    return to_img(a[..., :3], al)


def turf_top(src, name):
    dark, mid, light, accent = palette(src)
    rng = rng_for(name)
    n = fbm(rng)
    rgb = mixc(dark, light, np.clip(n * 1.2 - 0.1, 0, 1))
    blades = rng.random((N, N)) < 0.18
    rgb[blades] = shade(rgb[blades], 1.18)
    tips = rng.random((N, N)) < 0.05
    rgb[tips] = mixc(light, accent, 0.4)[None]
    return to_img(rgb)


def turf_side(src, name):
    a = np.asarray(src.convert('RGBA')).astype(np.float64)
    top = a[:3].reshape(-1, 4)[:, :3]; bottom = a[8:].reshape(-1, 4)[:, :3]
    g_dark, g_light = top[np.argmin(lum(top))], top[np.argmax(lum(top))]
    s_dark, s_light = bottom[np.argmin(lum(bottom))], bottom[np.argmax(lum(bottom))]
    rng = rng_for(name)
    soil = mixc(s_dark, s_light, fbm(rng))
    pebbles = rng.random((N, N)) < 0.02
    soil[pebbles] = shade(soil[pebbles], 1.25)
    x = np.arange(N)
    fringe = (12 + 6 * np.sin(x * 0.7) * np.sin(x * 0.23 + 1) + rng.integers(0, 5, N)).astype(int)
    y = np.mgrid[0:N, 0:N][0]
    grass = y < fringe[None, :]
    g = mixc(g_dark, g_light, np.clip(fbm(rng, ((8, 1), (32, 0.5))) * 1.3, 0, 1))
    rgb = np.where(grass[..., None], g, soil)
    rgb[(y == fringe[None, :])] = g_dark * 0.8
    return to_img(rgb)


def soil(src, name):
    dark, mid, light, accent = palette(src)
    rng = rng_for(name)
    rgb = mixc(dark, light, fbm(rng))
    p = rng.random((N, N)) < 0.04
    rgb[p] = shade(rgb[p], 1.3)
    return to_img(rgb)


def cloud(src, name):
    dark, mid, light, accent = palette(src)
    rng = rng_for(name)
    n = fbm(rng, ((3, 1.0), (6, 0.6), (12, 0.35), (24, 0.15)))
    rgb = mixc(mid, light, np.clip(n * 1.5 - 0.2, 0, 1))
    y = np.mgrid[0:N, 0:N][0]
    rgb = shade(rgb, 1.05 - 0.15 * y / N)
    if 'thunder' in name or 'charged' in name:
        rgb = add_veins(rgb, accent, rng, 3)
    srca = np.asarray(src.convert('RGBA').resize((N, N), Image.BILINEAR))[..., 3]
    return to_img(rgb, srca)


def lamp(src, name):
    dark, mid, light, accent = palette(src)
    y, x = np.mgrid[0:N, 0:N]
    d = np.hypot(x - 31.5, y - 31.5) / 32
    core = np.clip(1 - d, 0, 1) ** 1.5
    rgb = mixc(shade(accent, 0.65), mixc(light, np.array([255.0, 255, 255]), 0.5), core)
    m = np.minimum(np.minimum(x, y), np.minimum(N - 1 - x, N - 1 - y))
    rgb[m < 5] = shade(dark[None], 0.9)
    rgb[(m >= 5) & (m < 6)] = light
    grid = ((x - 5) % 18 < 2) | ((y - 5) % 18 < 2)
    rgb[grid & (m >= 5)] = shade(dark[None], 1.1)
    return to_img(rgb)


def glass(src, name):
    a = np.asarray(src.convert('RGBA').resize((N, N), Image.NEAREST)).astype(np.float64)
    dark, mid, light, accent = palette(src)
    y, x = np.mgrid[0:N, 0:N]
    m = np.minimum(np.minimum(x, y), np.minimum(N - 1 - x, N - 1 - y))
    rgb = np.full((N, N, 3), mid)
    al = np.full((N, N), 90.0)
    rgb[m < 3] = dark; al[m < 3] = 230
    streak = (np.abs((x - y) - 12) < 2) | (np.abs((x - y) + 20) < 1)
    rgb[streak & (m >= 3)] = 255; al[streak & (m >= 3)] = 170
    return to_img(rgb, al)


# which painter each block gets
def block_painter(name):
    n = name
    if n.endswith(('_door_top', '_door_bottom', '_trapdoor')): return 'picture'
    if n.startswith('chiseled_'):
        def paint(s, nm):
            dark, mid, light, accent = palette(s)
            base = np.asarray(stone(s, nm, polished=True), dtype=np.float64)[..., :3]
            return to_img(chisel(base, dark, light, accent, rng_for(nm)))
        return paint
    if 'brick' in n: return lambda s, nm: bricks(s, nm, cracked=n.startswith('cracked'))
    if n.startswith('polished_'): return lambda s, nm: stone(s, nm, polished=True)
    if n.startswith('cobbled_'): return lambda s, nm: stone(s, nm, cobbled=True)
    if n in ('charged_stormstone',): return lambda s, nm: stone(s, nm, veins=True)
    if n in ('stormstone', 'skyshale', 'fulgurite', 'salt_block'): return lambda s, nm: stone(s, nm)
    if n.endswith('_ore'): return ore
    if n in ('storm_crystal_block', 'aurorite_block', 'salt_crystal'): return crystal_block
    if n in ('voltite_block',): return metal_block
    if n == 'stormglass': return glass
    if n.endswith('_log_top'): return lambda s, nm: log_top(s, nm, stripped=n.startswith('stripped'))
    if n.endswith('_log'): return lambda s, nm: log_side(s, nm, stripped=n.startswith('stripped'))
    if n.endswith('_planks'): return planks
    if n.endswith('_leaves'): return leaves
    if n.endswith('_turf_top') or n in ('static_grass_top',): return turf_top
    if n.endswith('_turf_side') or n in ('static_grass_side',): return turf_side
    if n in ('skysoil',): return soil
    if n in ('gale_cloud', 'nimbus_cloud', 'thundercloud'): return cloud
    if n in ('ion_lamp',): return lamp
    return 'picture'


# ------------------------------------------------------------------------------------------------ run
def main():
    done = {'block': 0, 'item': 0, 'entity': 0}
    for f in sorted(glob.glob(str(TEX / 'block' / '*.png'))):
        name = pathlib.Path(f).stem
        src = Image.open(f).convert('RGBA')
        w, h = src.size
        if w >= N: continue
        painter = block_painter(name)
        if h != w or painter == 'picture':
            out = remaster(src, N // w, name, outline=False, lighting=h == w)
        else:
            out = painter(src, name)
        out.save(f); done['block'] += 1
    for f in sorted(glob.glob(str(TEX / 'item' / '*.png'))):
        src = Image.open(f).convert('RGBA')
        if src.size[0] >= N: continue
        remaster(src, N // src.size[0], pathlib.Path(f).stem).save(f); done['item'] += 1
    for f in sorted(glob.glob(str(TEX / 'entity' / '**' / '*.png'), recursive=True)):
        name = pathlib.Path(f).stem
        if name in SKIP_ENTITY: continue
        src = Image.open(f).convert('RGBA')
        w = src.size[0]
        s = 4 if w <= 64 else 2 if w <= 128 else 1
        if s == 1: continue
        remaster(src, s, name, outline=False).save(f); done['entity'] += 1
    print(f"remaster64: {done['block']} blocks, {done['item']} items, {done['entity']} mob/armour textures redrawn")


if __name__ == '__main__':
    main()
