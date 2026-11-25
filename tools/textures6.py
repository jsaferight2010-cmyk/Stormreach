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
# Tornado Director (item): a brass-capped rod with a little spinning funnel at its tip
os.makedirs(f'{OUT}/item', exist_ok=True)
rows = ['................',
        '..........GGGG..',
        '.........GWWWWG.',
        '..........GWWG..',
        '...........GWG..',
        '..........GWG...',
        '...........G....',
        '..........KK....',
        '.........KYK....',
        '........KYK.....',
        '.......KYK......',
        '......KYK.......',
        '.....KBK........',
        '....KBK.........',
        '...KBK..........',
        '...KK...........']
pal = {'G': (110, 118, 128, 255), 'W': (205, 214, 222, 255), 'K': (40, 34, 52, 255), 'Y': (242, 201, 76, 255), 'B': (123, 79, 201, 255)}
img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
for y, row in enumerate(rows):
    for x, ch in enumerate(row):
        if ch in pal: img.putpixel((x, y), pal[ch])
img.save(f'{OUT}/item/tornado_director.png')
# hailstone: clear ice with milky growth rings (hail grows in layers as it cycles through the updraft)
W, H = 64, 32
ring = fbm(W, H, [(4, 2, 1.0), (8, 4, 0.5)], 1501)
img = Image.new('RGBA', (W, H))
for y in range(H):
    for x in range(W):
        band = 0.5 + 0.5 * math.sin(y / H * math.pi * 7 + ring(x, y) * 3)
        v = 0.78 + 0.22 * band
        img.putpixel((x, y), (int(220 * v), int(236 * v), 255, int(255 * (0.55 + 0.4 * band))))
img.save(f'{OUT}/entity/hailstone.png')

# storm cloud: soft billows, bright where the cloud is thick
W, H = 128, 64
puff = fbm(W, H, [(4, 2, 1.0), (8, 4, 0.6), (16, 8, 0.35), (32, 16, 0.15)], 1601)
img = Image.new('RGBA', (W, H))
for y in range(H):
    for x in range(W):
        c = puff(x, y)
        v = 0.72 + 0.4 * c
        img.putpixel((x, y), (int(255 * min(1, v)),) * 3 + (int(255 * min(1, 0.7 + 0.5 * c)),))
img.save(f'{OUT}/entity/storm_cloud.png')

# hail on the ground (block textures, 64x): small pellets, and a pile with big stones in it
os.makedirs(f'{OUT}/block', exist_ok=True)
for name, big in (('hail_layer', False), ('hail_layer_big', True)):
    rnd = random.Random(1701 + big)
    img = Image.new('RGBA', (64, 64), (196, 214, 226, 255))
    for _ in range(26 if big else 140):
        r = rnd.randint(5, 10) if big else rnd.randint(1, 3)
        x, y = rnd.randint(0, 63), rnd.randint(0, 63)
        for dy in range(-r, r + 1):
            for dx in range(-r, r + 1):
                d = (dx * dx + dy * dy) ** 0.5
                if d <= r:
                    lit = 1.0 - 0.35 * (dx + dy) / (2 * r) - 0.15 * d / r   # light from the upper left
                    c = (int(min(255, 205 * lit + 30)), int(min(255, 222 * lit + 28)), int(min(255, 240 * lit + 20)), 255)
                    img.putpixel(((x + dx) % 64, (y + dy) % 64), c)
                    if d < r * 0.35 and dx < 0 and dy < 0: img.putpixel(((x + dx) % 64, (y + dy) % 64), (250, 253, 255, 255))
    img.save(f'{OUT}/block/{name}.png')

# puddle: dark, glassy water with soft ripple rings
rnd = random.Random(1801)
img = Image.new('RGBA', (64, 64))
rip = [(rnd.randint(0, 63), rnd.randint(0, 63), rnd.randint(6, 18)) for _ in range(5)]
for y in range(64):
    for x in range(64):
        v = 0
        for cx, cy, rr in rip:
            for ox in (-64, 0, 64):
                for oy in (-64, 0, 64):
                    d = ((x - cx - ox) ** 2 + (y - cy - oy) ** 2) ** 0.5
                    if abs(d - rr) < 1.2: v += 0.35 * (1 - abs(d - rr) / 1.2)
        v = min(1, v)
        img.putpixel((x, y), (int(58 + 90 * v), int(84 + 90 * v), int(118 + 80 * v), int(150 + 60 * v)))
img.save(f'{OUT}/block/puddle.png')
print('textures6 done')
