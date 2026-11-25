"""5.1 overhaul textures: tornado and waterspout funnels, debris and spray clouds.
Greyscale with alpha; the renderer tints them per tornado type, so one texture serves every kind."""
import os as _os, pathlib as _pl
ROOT = _os.environ.get('STORMREACH_ROOT', str(_pl.Path(__file__).resolve().parent.parent))
import math, random
from PIL import Image, ImageFilter
OUT = ROOT + '/src/main/resources/assets/stormreach/textures'
os = _os
os.makedirs(f'{OUT}/entity', exist_ok=True)

def tile_noise(w, h, cells_x, cells_y, seed):
    """Smooth value noise that wraps in x (so it tiles round the funnel) and in y."""
    rnd = random.Random(seed)
    g = [[rnd.random() for _ in range(cells_x)] for _ in range(cells_y)]
    def at(x, y):
        fx, fy = x / w * cells_x, y / h * cells_y
        x0, y0 = int(fx) % cells_x, int(fy) % cells_y
        x1, y1 = (x0 + 1) % cells_x, (y0 + 1) % cells_y
        tx, ty = fx - int(fx), fy - int(fy)
        tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
        a = g[y0][x0] + (g[y0][x1] - g[y0][x0]) * tx
        b = g[y1][x0] + (g[y1][x1] - g[y1][x0]) * tx
        return a + (b - a) * ty
    return at

def fbm(w, h, layers, seed):
    fs = [(tile_noise(w, h, cx, cy, seed + i), amp) for i, (cx, cy, amp) in enumerate(layers)]
    tot = sum(a for _, a in fs)
    return lambda x, y: sum(f(x, y) * a for f, a in fs) / tot

# funnel: long vertical striations (the spiralling condensation bands) over soft cloudy lumps
W, H = 128, 256
streak = fbm(W, H, [(16, 2, 1.0), (32, 4, 0.5), (64, 8, 0.25)], 1101)
lumps = fbm(W, H, [(4, 8, 1.0), (8, 16, 0.6), (16, 32, 0.3)], 1201)
img = Image.new('RGBA', (W, H))
for y in range(H):
    for x in range(W):
        s, l = streak(x, y), lumps(x, y)
        v = 0.55 + 0.5 * (s - 0.5) * 2 + 0.35 * (l - 0.5)
        v = max(0.25, min(1.0, v))
        a = 0.62 + 0.38 * l
        img.putpixel((x, y), (int(255 * v), int(255 * v), int(255 * v), int(255 * min(1, a))))
img.filter(ImageFilter.GaussianBlur(0.6)).save(f'{OUT}/entity/tornado_funnel.png')

# debris / spray cloud: billowing clumps with ragged, see-through gaps
W, H = 128, 64
clump = fbm(W, H, [(6, 3, 1.0), (12, 6, 0.7), (24, 12, 0.4), (48, 24, 0.2)], 1301)
img = Image.new('RGBA', (W, H))
for y in range(H):
    for x in range(W):
        c = clump(x, y)
        v = 0.45 + 0.6 * c
        a = max(0.0, min(1.0, (c - 0.18) * 2.2))
        img.putpixel((x, y), (int(255 * min(1, v)),) * 3 + (int(255 * a),))
img.filter(ImageFilter.GaussianBlur(0.8)).save(f'{OUT}/entity/tornado_debris.png')

# lowered wall cloud underside: broad dark rolls
W, H = 128, 64
roll = fbm(W, H, [(8, 2, 1.0), (16, 4, 0.6), (32, 8, 0.3)], 1401)
img = Image.new('RGBA', (W, H))
for y in range(H):
    for x in range(W):
        c = roll(x, y)
        v = 0.5 + 0.5 * c
        img.putpixel((x, y), (int(255 * v),) * 3 + (int(255 * (0.75 + 0.25 * c)),))
img.save(f'{OUT}/entity/tornado_wallcloud.png')
print('textures6 done')
