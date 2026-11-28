"""Stormreach 5.2 (Stormwild) textures: item icons (32x32 pixel art, shaded on a colour ramp with a dark outline,
then upscaled to 64x by remaster64), spawn eggs, and the new blocks (drawn directly at 64x, which remaster64 leaves
alone). Mob textures come from mobmodels.py."""
import pathlib, math, zipfile, io, os
import numpy as np
from PIL import Image
from scipy.ndimage import distance_transform_edt, binary_dilation

ROOT = pathlib.Path(__file__).resolve().parent.parent
TEX = ROOT / 'src' / 'main' / 'resources' / 'assets' / 'stormreach' / 'textures'
MCJAR = os.environ.get('STORMREACH_MCJAR', str(ROOT / '.vanilla' / 'minecraft-26.3.jar'))
rng = np.random.default_rng(52)
S = 32


def C(h, a=255): return np.array([int(h[i:i + 2], 16) for i in (1, 3, 5)] + [a], dtype=float)


def ramp(base, n=6):
    base = np.asarray(base[:3], float)
    shadow = base * 0.38 + np.array([24, 26, 60]) * 0.3
    hi = np.minimum(255, base * 1.18 + np.array([255, 246, 222]) * 0.22)
    return np.array([shadow + (base - shadow) * (i / (n - 1) * 2) if i < (n - 1) / 2 else base + (hi - base) * ((i / (n - 1) - 0.5) * 2) for i in range(n)])


yy, xx = np.mgrid[0:S, 0:S] + 0.5


class Icon:
    def __init__(self, size=S):
        self.n = size
        self.rgb = np.zeros((size, size, 3)); self.a = np.zeros((size, size)); self.mask = np.zeros((size, size), bool)

    def shape(self, m, base, light=(-0.55, -0.7), bump=1.0, alpha=255, levels=6, outline=True, pattern=None):
        """Fills a mask with volume shading: a dome from the distance to the edge, lit from the upper left."""
        if not m.any(): return
        d = distance_transform_edt(m)
        hgt = np.sqrt(np.clip(d / max(1.5, d.max()), 0, 1)) * bump
        gy, gx = np.gradient(hgt)
        lv = 0.55 + (gx * light[0] + gy * light[1]) * 2.4 + 0.12 * hgt
        if pattern is not None: lv = lv + pattern
        R = ramp(base, levels)
        idx = np.clip(np.round(lv * (levels - 1)), 0, levels - 1).astype(int)
        self.rgb[m] = R[idx][m]; self.a[m] = alpha; self.mask |= m
        if outline:
            ring = binary_dilation(m) & ~self.mask
            self.rgb[ring] = R[0] * 0.55; self.a[ring] = 255
            self.mask |= ring

    def paint(self, m, col, alpha=255):
        self.rgb[m] = np.asarray(col[:3]); self.a[m] = alpha

    def save(self, rel):
        p = TEX / f'{rel}.png'
        p.parent.mkdir(parents=True, exist_ok=True)
        Image.fromarray(np.dstack([np.clip(self.rgb, 0, 255), self.a]).astype(np.uint8), 'RGBA').save(p)


def circle(cx, cy, r): return (xx - cx) ** 2 + (yy - cy) ** 2 <= r * r
def ellipse(cx, cy, rx, ry, ang=0.0):
    c, s = math.cos(ang), math.sin(ang)
    u, v = (xx - cx) * c + (yy - cy) * s, -(xx - cx) * s + (yy - cy) * c
    return (u / rx) ** 2 + (v / ry) ** 2 <= 1
def rect(x0, y0, x1, y1): return (xx >= x0) & (xx < x1) & (yy >= y0) & (yy < y1)
def thick_path(pts, r):
    m = np.zeros((S, S), bool)
    for (x0, y0), (x1, y1) in zip(pts, pts[1:]):
        for t in np.linspace(0, 1, 24):
            m |= circle(x0 + (x1 - x0) * t, y0 + (y1 - y0) * t, r)
    return m
def line(pts, w=0.6):
    return thick_path(pts, w)


# ---------------------------------------------------------------------------------------------------- items
def cloud_ambergris():
    i = Icon()
    m = circle(14, 17, 8) | circle(20, 14, 6) | circle(19, 21, 6) | circle(10, 12, 4.5)
    swirl = np.sin(np.hypot(xx - 15, yy - 16) * 0.9 + np.arctan2(yy - 16, xx - 15) * 2) * 0.08
    i.shape(m, C('#d6c896'), pattern=swirl + 0.05 * (rng.random((S, S)) - 0.5))
    i.paint(m & (rng.random((S, S)) < 0.05), C('#f2e2a8'))
    i.paint(line([(12, 15), (15, 13), (18, 15)], 0.5) & m, C('#a8986a'))
    i.save('item/cloud_ambergris')


def glowgel():
    i = Icon()
    m = circle(16, 19, 8.5) | (((yy > 6) & (yy < 19)) & (np.abs(xx - 16) < (yy - 6) * 0.62))
    i.shape(m, C('#5fd8f8'), alpha=225, bump=1.3)
    i.paint(circle(16, 20, 4) & m, C('#c8f6ff'), 235)
    i.paint(ellipse(12.5, 15, 1.5, 2.5, -0.4), C('#ffffff'), 255)
    i.save('item/glowgel')


def eel(name, body, belly, spots, grill=False):
    i = Icon()
    pts = [(5, 22 + 4 * math.sin(k * 0.55)) for k in range(0)]
    path = [(4 + k * 1.2, 16 + 7 * math.sin(k * 0.32)) for k in range(22)]
    m = np.zeros((S, S), bool)
    for k, (x, y) in enumerate(path):
        r = 3.2 if k < 18 else 3.2 - (k - 17) * 0.6
        m |= circle(x, y, max(1.0, r))
    i.shape(m, body)
    belly_m = np.zeros((S, S), bool)
    for x, y in path[1:18]: belly_m |= circle(x + 0.4, y + 1.8, 1.0)
    i.paint(belly_m & m, belly[:3])
    if spots is not None:
        sp = m & (rng.random((S, S)) < 0.09) & ~belly_m
        i.paint(sp, spots)
    if grill:
        for gx in (9, 14, 19, 24): i.paint(m & (np.abs(xx + yy * 0.3 - gx - 5) < 0.6), C('#4a2a14'))
    i.paint(circle(path[1][0] + 0.5, path[1][1] - 1.2, 0.8), C('#fff6b0') if spots is not None else C('#2a1a0a'))
    i.save(f'item/{name}')


def charged_scale():
    i = Icon()
    m = ellipse(16, 16, 7, 11, 0.6)
    ridges = 0.07 * np.sin((xx * 0.8 + yy * 0.5) * 1.4)
    i.shape(m, C('#3f9a88'), pattern=ridges)
    bolt = line([(19, 8), (14, 15), (18, 16), (13, 24)], 0.7) & m
    i.paint(bolt, C('#f8f6c0'))
    i.paint(binary_dilation(bolt) & ~bolt & m, C('#9ff0ff'))
    i.save('item/charged_scale')


def gale_essence():
    i = Icon()
    m = circle(16, 16, 10)
    i.shape(m, C('#cfeaff'), alpha=170, outline=False)
    r = np.hypot(xx - 16, yy - 16); th = np.arctan2(yy - 16, xx - 16)
    spiral = (np.abs(((th + r * 0.45) % (math.pi)) - 1.2) < 0.32) & m & (r > 2)
    i.paint(spiral, C('#ffffff'), 240)
    i.paint(circle(16, 16, 2.2), C('#9fe0ff'), 255)
    ring = binary_dilation(m) & ~m
    i.paint(ring, C('#6a9ac0'), 200)
    i.save('item/gale_essence')


def bottle(i, liquid_col, alpha=210):
    body = circle(16, 21, 8)
    neck = rect(13, 7, 19, 14)
    lip = rect(12, 6, 20, 8)
    glass = body | neck | lip
    i.shape(glass, C('#cfe2ee'), alpha=150, outline=True)
    liquid = circle(16, 21, 6.5) & (yy > 15)
    i.shape(liquid, liquid_col, alpha=alpha, outline=False)
    i.paint(rect(14, 3, 18, 7), C('#8a5a34'))
    i.paint(ellipse(12, 19, 1.0, 2.6, 0.3), C('#ffffff'), 230)
    return liquid


def bottled_gale():
    i = Icon()
    liq = bottle(i, C('#bfe8ff'))
    r = np.hypot(xx - 16, yy - 21); th = np.arctan2(yy - 21, xx - 16)
    i.paint(liq & (np.abs(((th + r * 0.6) % math.pi) - 1.3) < 0.3), C('#ffffff'), 240)
    i.save('item/bottled_gale')


def conch(name, base, band=None):
    i = Icon()
    m = np.zeros((S, S), bool)
    for k in range(9):
        t = k / 8
        m |= circle(8 + t * 15, 22 - t * 12, 7.5 - t * 5.5)
    m |= ellipse(10, 20, 6, 4, -0.6)
    hue = np.stack([np.sin(xx * 0.3) * 18, np.sin(xx * 0.3 + 2) * 18, np.sin(xx * 0.3 + 4) * 18], -1)
    bands = 0.1 * np.sin((xx * 0.7 - yy * 0.9))
    i.shape(m, base, pattern=bands)
    i.rgb[m] = np.clip(i.rgb[m] + hue[m], 0, 255)
    mouth = ellipse(9, 21, 3.5, 2.2, -0.6)
    i.paint(mouth, C('#f6c8d8'))
    i.paint(ellipse(9.5, 21.5, 2, 1, -0.6), C('#c88098'))
    if band is not None:
        i.paint(m & (np.abs((xx - 16) * 0.6 + (yy - 16) * 0.8) < 1.1), band)
        i.paint(circle(23.5, 9.5, 1.6), band)
    i.save(f'item/{name}')


def plume():
    i = Icon()
    vane = ellipse(17, 14, 5.5, 12, 0.75)
    i.shape(vane, C('#3a4258'), pattern=0.08 * np.sin((xx - yy) * 1.6))
    tip = vane & (xx - yy > 6)
    i.paint(vane & (np.abs((xx - 17) * 0.75 + (yy - 14) * -0.66) > 3.2) & (rng.random((S, S)) < 0.5), C('#9fe8ff'))
    i.paint(tip & (rng.random((S, S)) < 0.6), C('#e8fbff'))
    shaft = line([(6, 27), (25, 5)], 0.55)
    i.paint(shaft, C('#e8e2d0'))
    i.paint(line([(4.5, 28.5), (8, 25)], 0.7), C('#b8a888'))
    i.save('item/thunderbird_plume')


def stormkite():
    i = Icon()
    d = np.abs(xx - 17) / 9 + np.abs(yy - 13) / 11
    kite = d <= 1
    i.shape(kite, C('#e07a2a'), bump=0.6)
    i.paint(kite & (xx > 17) & (yy < 13), ramp(C('#3a6ac0'))[3])
    i.paint(kite & (xx < 17) & (yy > 13), ramp(C('#3a6ac0'))[2])
    i.paint(line([(17, 2.5), (17, 23.5)], 0.45) | line([(8.5, 13), (25.5, 13)], 0.45), C('#5a3a1a'))
    tail = [(17, 24), (15, 26), (17, 28), (15, 30)]
    i.paint(line(tail, 0.45), C('#c8c0a8'))
    for (bx, by), col in zip(((15.5, 26), (16.5, 28.2)), ('#9fe8ff', '#f2c94c')):
        i.paint(ellipse(bx, by, 1.6, 0.9), C(col))
    i.paint(line([(17, 13), (6, 30)], 0.35), C('#e8e8e8'))
    i.save('item/stormkite')


def tiv_icon():
    i = Icon()
    hull = rect(3, 15, 29, 23)
    cab = rect(9, 8, 22, 15)
    i.shape(hull | cab, C('#4a5058'), bump=0.5)
    i.paint(rect(22, 13, 29, 16), C('#e07a2a'))
    i.paint(rect(3, 18, 29, 19), C('#e07a2a'))
    i.paint(rect(11, 10, 15, 14) | rect(16, 10, 20, 14), C('#9fc8e0'))
    i.paint(rect(14, 6, 19, 8), C('#ffb84a'))
    i.paint(rect(16, 3, 17, 6), C('#8a929a'))
    i.paint(rect(27, 16, 29, 18), C('#fffbe0'))
    for cx in (9, 23):
        i.shape(circle(cx, 23.5, 4), C('#2a2a2c'), bump=0.8)
        i.paint(circle(cx, 23.5, 1.5), C('#b0b8c0'))
    i.save('item/intercept_vehicle')


def balloon_icon():
    i = Icon()
    i.shape(ellipse(16, 11, 8, 9), C('#f4f2ee'), bump=1.2)
    i.paint(ellipse(12.5, 7.5, 1.6, 2.4, 0.4), C('#ffffff'))
    i.paint(line([(16, 20), (15, 24), (16, 27)], 0.4), C('#c8c0a8'))
    i.shape(rect(14, 26, 19, 30), C('#f2f2f2'), bump=0.4)
    i.paint(rect(14.5, 27, 18.5, 28), C('#e04a3a'))
    i.save('item/weather_balloon')


def logbook():
    i = Icon()
    cover = rect(6, 4, 26, 28)
    i.shape(cover, C('#2f5a66'), bump=0.4)
    i.paint(rect(24, 5, 26, 27), C('#e8e2d0'))
    i.paint(rect(6, 4, 9, 28), ramp(C('#2f5a66'))[1])
    # a tornado embossed on the cover
    for k, (w_, y) in enumerate(((6, 9), (5, 11), (4, 13), (3, 15), (2.2, 17), (1.6, 19), (1, 21))):
        i.paint(rect(17 - w_ + k * 0.3, y, 17 + w_ + k * 0.3, y + 1.2), C('#c8e8f0'))
    i.paint(rect(20, 14, 27, 16), C('#8a5a34'))
    i.paint(rect(25, 14, 27, 16), C('#d8b070'))
    i.save('item/chasers_logbook')


def lantern_icon():
    i = Icon()
    i.shape(rect(11, 9, 21, 25), C('#3a4048'), bump=0.4)
    i.paint(rect(12.5, 11, 19.5, 23), C('#bff4ff'))
    i.paint(rect(14, 13, 18, 21), C('#ffffff'))
    i.shape(rect(10, 7, 22, 10) | rect(10, 24, 22, 27), C('#4a5058'), bump=0.3)
    i.paint(rect(15, 3, 17, 7), C('#6a7078'))
    i.save('item/skylight_lantern')


def gauge_icon():
    i = Icon()
    i.shape(rect(12, 4, 20, 26), C('#cfe2ee'), alpha=170)
    i.paint(rect(13, 15, 19, 25), C('#4a8ad8'), 230)
    for y in range(6, 26, 3): i.paint(rect(12, y, 15, y + 0.6), C('#2a3a4a'))
    i.shape(rect(10, 26, 22, 29), C('#8a929a'), bump=0.3)
    i.shape(rect(10, 3, 22, 5), C('#8a929a'), bump=0.3)
    i.save('item/rain_gauge')


def egg(name, base, spot, seed):
    r2 = np.random.default_rng(seed)
    i = Icon()
    m = ((xx - 16) / 9.5) ** 2 + ((yy - 17) / (12 if yy.mean() else 12)) ** 2 <= 1
    m = ((xx - 16) / 9.5) ** 2 + ((yy - 17.5) / np.where(yy < 17.5, 12.5, 11.0)) ** 2 <= 1
    spots = np.zeros((S, S), bool)
    for _ in range(9):
        spots |= circle(r2.uniform(9, 23), r2.uniform(8, 27), r2.uniform(1.2, 2.6))
    i.shape(m, C(base), pattern=-0.05 * spots)
    sp = spots & m & ~binary_dilation(~m, iterations=2)
    R = ramp(C(spot))
    i.rgb[sp] = R[3]
    i.paint(ellipse(12, 11, 1.4, 2.6, 0.3) & m, C('#ffffff'))
    i.save(f'item/{name}_spawn_egg')


# ---------------------------------------------------------------------------------------------------- blocks, 64x
N = 64
gy64, gx64 = np.mgrid[0:N, 0:N] + 0.5


def save64(rgb, a, rel):
    p = TEX / f'{rel}.png'; p.parent.mkdir(parents=True, exist_ok=True)
    Image.fromarray(np.dstack([np.clip(rgb, 0, 255), a]).astype(np.uint8), 'RGBA').save(p)


def noise64(cells, seed):
    r = np.random.default_rng(seed)
    g = r.random((cells + 1, cells + 1))
    t = np.linspace(0, cells, N, endpoint=False); i0 = t.astype(int); f = t - i0; f = f * f * (3 - 2 * f)
    a = g[i0][:, i0]; b = g[i0][:, i0 + 1]; c = g[i0 + 1][:, i0]; d = g[i0 + 1][:, i0 + 1]
    return (a * (1 - f) + b * f) * (1 - f)[:, None] + (c * (1 - f) + d * f) * f[:, None]


def glowgel_block():
    n = noise64(4, 1) * 0.6 + noise64(8, 2) * 0.4
    base = np.array([95, 216, 248.0]); deep = np.array([40, 120, 190.0]); core = np.array([210, 248, 255.0])
    rgb = deep + (base - deep) * n[..., None]
    d = np.minimum(np.minimum(gx64, N - gx64), np.minimum(gy64, N - gy64))
    rgb = np.where((d < 3)[..., None], base * 1.15, rgb)
    rgb = np.where((d < 1.5)[..., None], deep * 0.8, rgb)
    inner = (np.abs(gx64 - 32) < 14) & (np.abs(gy64 - 32) < 14)
    rgb = np.where(inner[..., None], rgb * 0.6 + core * 0.4, rgb)
    a = np.full((N, N), 170.0); a[d < 3] = 220; a[inner] = 200
    r = np.random.default_rng(3)
    for _ in range(14):
        cx, cy, rr = r.uniform(6, 58), r.uniform(6, 58), r.uniform(1.5, 3.5)
        b = (gx64 - cx) ** 2 + (gy64 - cy) ** 2 <= rr * rr
        rgb[b] = rgb[b] * 0.7 + 255 * 0.3; a[b] = 235
        hl = (gx64 - cx + rr * 0.4) ** 2 + (gy64 - cy + rr * 0.4) ** 2 <= (rr * 0.35) ** 2
        rgb[hl] = 255
    save64(rgb, a, 'block/glowgel_block')


def nacre(rel, bricks=False):
    n = noise64(3, 7)
    bands = np.sin(gy64 * 0.35 + np.sin(gx64 * 0.12) * 3 + n * 6)
    hue = np.stack([228 + 20 * np.sin(bands * 2 + 0), 222 + 20 * np.sin(bands * 2 + 2.1), 238 + 16 * np.sin(bands * 2 + 4.2)], -1)
    rgb = hue * (0.9 + 0.1 * bands[..., None])
    sheen = np.clip(1 - np.abs(gx64 - gy64 - 10) / 18, 0, 1)
    rgb = rgb + sheen[..., None] * 18
    if bricks:
        row = (gy64 // 16).astype(int)
        mortar = (gy64 % 16 < 1.5) | (((gx64 + (row % 2) * 16) % 32) < 1.5)
        edge_hi = (gy64 % 16 >= 1.5) & (gy64 % 16 < 3)
        rgb = np.where(mortar[..., None], np.array([150, 138, 160.0]), rgb)
        rgb = np.where(edge_hi[..., None] & ~mortar[..., None], rgb * 1.06, rgb)
    else:
        d = np.minimum(np.minimum(gx64, N - gx64), np.minimum(gy64, N - gy64))
        rgb = np.where((d < 2)[..., None], rgb * 0.86, rgb)
    save64(rgb, np.full((N, N), 255.0), rel)


def lantern_block():
    """The Skylight Lantern texture: vanilla lantern's layout (so the vanilla lantern models fit it), recoloured to
    storm steel with a pale-cyan glow, and redrawn at 64x."""
    try:
        with zipfile.ZipFile(MCJAR) as z:
            src = Image.open(io.BytesIO(z.read('assets/minecraft/textures/block/lantern.png'))).convert('RGBA')
    except (FileNotFoundError, KeyError):
        return
    a = np.asarray(src).astype(float)
    lum = a[..., :3].mean(-1)
    sat = a[..., :3].max(-1) - a[..., :3].min(-1)
    glow = (sat > 60) & (lum > 110)
    out = np.zeros_like(a)
    steel = np.array([58, 64, 74.0]) + (lum[..., None] / 255) * np.array([90, 96, 108.0])
    out[..., :3] = np.where(glow[..., None], np.array([150, 232, 255.0]) * (0.6 + 0.4 * lum[..., None] / 255) + 40, steel)
    out[..., 3] = a[..., 3]
    big = np.kron(out, np.ones((4, 4, 1)))
    H, W = big.shape[:2]
    yy4, xx4 = np.mgrid[0:H, 0:W]
    bev = ((yy4 % 4 == 0) * 10 - (yy4 % 4 == 3) * 12 + (xx4 % 4 == 0) * 6 - (xx4 % 4 == 3) * 6)
    big[..., :3] = np.clip(big[..., :3] + bev[..., None] * (big[..., 3:4] > 0), 0, 255)
    Image.fromarray(big.astype(np.uint8), 'RGBA').save(TEX / 'block' / 'skylight_lantern.png')


def gauge_blocks():
    # glass tube with graduation marks (mostly transparent), the water, and the metal base
    rgb = np.zeros((N, N, 3)); rgb[:] = (210, 230, 240); a = np.full((N, N), 40.0)
    edge = (gx64 < 4) | (gx64 > N - 4)
    a[edge] = 140; rgb[edge] = (230, 244, 250)
    marks = ((gy64 % 8) < 1.2) & (gx64 < 20)
    rgb[marks] = (40, 56, 72); a[marks] = 230
    big = ((gy64 % 32) < 1.6) & (gx64 < 30)
    rgb[big] = (20, 30, 44); a[big] = 255
    hl = (gx64 > 40) & (gx64 < 46); rgb[hl] = 255; a[hl] = 110
    save64(rgb, a, 'block/rain_gauge')
    n = noise64(6, 11)
    water = np.array([60, 130, 216.0]) * (0.85 + 0.25 * n[..., None])
    save64(water, np.full((N, N), 200.0), 'block/rain_gauge_water')
    n = noise64(10, 12)
    metal = np.array([138, 146, 154.0]) * (0.85 + 0.2 * n[..., None])
    d = np.minimum(np.minimum(gx64, N - gx64), np.minimum(gy64, N - gy64))
    metal = np.where((d < 3)[..., None], metal * 0.7, metal)
    save64(metal, np.full((N, N), 255.0), 'block/rain_gauge_base')


def probe_blocks():
    n = noise64(8, 21)
    paint = np.array([224, 122, 42.0]) * (0.88 + 0.18 * n[..., None])
    rib = (gy64 % 21) < 2.5
    paint = np.where(rib[..., None], paint * 0.72, paint)
    stripe = (gy64 > 40) & (gy64 < 52)
    hazard = ((gx64 + gy64) // 6 % 2 == 0)
    paint = np.where((stripe & hazard)[..., None], np.array([32, 32, 36.0]), np.where(stripe[..., None], np.array([240, 200, 40.0]), paint))
    bolt = ((gx64 % 16) > 6) & ((gx64 % 16) < 9) & ((gy64 % 21) > 5) & ((gy64 % 21) < 8)
    paint = np.where(bolt[..., None], np.array([200, 200, 205.0]), paint)
    label = (gx64 > 20) & (gx64 < 44) & (gy64 > 14) & (gy64 < 30)
    paint = np.where(label[..., None], np.array([236, 236, 230.0]), paint)
    txt = label & (((gy64 - 18) % 5) < 1.4) & (gx64 > 23) & (gx64 < 41)
    paint = np.where(txt[..., None], np.array([40, 40, 44.0]), paint)
    save64(paint, np.full((N, N), 255.0), 'block/tornado_probe')
    top = np.array([120, 126, 134.0]) * (0.85 + 0.2 * noise64(9, 22)[..., None])
    r = np.hypot(gx64 - 32, gy64 - 32)
    top = np.where((np.abs(r - 26) < 2)[..., None], top * 0.6, top)
    top = np.where(((r < 22) & (((np.arctan2(gy64 - 32, gx64 - 32) * 8 / math.pi) % 2) < 0.15))[..., None], top * 0.75, top)
    save64(top, np.full((N, N), 255.0), 'block/tornado_probe_top')
    for nm, col in (('tornado_probe_light_on', (255, 70, 60)), ('tornado_probe_light_off', (90, 40, 40))):
        c = np.array(col, float) * (0.8 + 0.3 * np.clip(1 - np.hypot(gx64 - 24, gy64 - 24) / 40, 0, 1))[..., None]
        save64(c, np.full((N, N), 255.0), f'block/{nm}')


def main():
    cloud_ambergris(); glowgel(); charged_scale(); gale_essence(); bottled_gale(); plume(); stormkite()
    eel('voltaic_eel', C('#2c3446'), C('#6a7488'), C('#ffe25a'))
    eel('cooked_voltaic_eel', C('#9a6236'), C('#c89060'), None, grill=True)
    conch('pearlescent_shell', C('#ecd8e0'))
    conch('tide_conch', C('#e8a890'), band=C('#f2c94c'))
    tiv_icon(); balloon_icon(); logbook(); lantern_icon(); gauge_icon()
    for k, (name, b, s) in enumerate((('cloud_whale', '#7f97b8', '#eef3f8'), ('shock_eel', '#2c3446', '#ffe25a'), ('sparkskink', '#3f7a6a', '#9ff0ff'),
                                      ('storm_jelly', '#8fd8ff', '#c8a8ff'), ('gale_sprite', '#e0f0fa', '#4ac0e8'), ('tidewrack_hermit', '#d8b8a8', '#e0683a'),
                                      ('thunderbird', '#3a4258', '#9fe8ff'))):
        egg(name, b, s, 500 + k)
    glowgel_block(); nacre('block/nacre_block'); nacre('block/nacre_bricks', bricks=True); lantern_block(); gauge_blocks(); probe_blocks()
    print('textures7: 5.2 items, eggs and blocks painted')


if __name__ == '__main__':
    main()
