"""Stormreach 2.1 textures: Stormblight blocks, weather bottles, paintings."""
import os as _os, pathlib as _pl
ROOT = str(_pl.Path(__file__).resolve().parent.parent)  # the Stormreach project folder
VANILLA = _os.environ.get('STORMREACH_VANILLA', str(_pl.Path(ROOT) / '.vanilla'))  # extracted 26.3 jar (assets/ + data/)
MCJAR = _os.environ.get('STORMREACH_MCJAR', str(_pl.Path(ROOT) / '.vanilla' / 'minecraft-26.3.jar'))
import random, math, os
from PIL import Image, ImageDraw
OUT = ROOT + '/src/main/resources/assets/stormreach/textures'
os.makedirs(f'{OUT}/painting', exist_ok=True)
def hexc(h, a=255):
    h = h.lstrip('#'); return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)
def jitter(c, amt, rnd):
    d = rnd.randint(-amt, amt); return tuple(max(0, min(255, v + d)) for v in c[:3]) + (c[3],)
def save(img, p): img.save(f'{OUT}/{p}.png')
CRYS = hexc('#6fdcff'); CRYS_L = hexc('#d2f7ff'); CRYS_D = hexc('#2f9fd0')
# Stormheart: dark crystal core in a stormstone frame
for active in (False, True):
    rnd = random.Random(300); img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - 7.5, y - 7.5)
            if x in (0, 15) or y in (0, 15): c = jitter(hexc('#353d49'), 4, rnd)
            elif x in (1, 14) or y in (1, 14): c = jitter(hexc('#4f5a69'), 5, rnd)
            elif r < 2.2: c = hexc('#ffffff') if active else hexc('#3a6f88')
            elif r < 4.5: c = jitter(CRYS_L if active else hexc('#2f5a70'), 8, rnd)
            else: c = jitter(CRYS if active else hexc('#22394a'), 10, rnd)
            img.putpixel((x, y), c)
    for (x0, y0, dx, dy) in ((2, 2, 1, 1), (13, 2, -1, 1), (2, 13, 1, -1), (13, 13, -1, -1)):
        for k in range(4): img.putpixel((x0 + dx * k, y0 + dy * k), CRYS_L if active else hexc('#3a6f88'))
    save(img, 'block/stormheart_active' if active else 'block/stormheart')
# Stormscar turf: grey-blue scorched grass
rnd = random.Random(301); top = Image.new('RGBA', (16, 16))
for y in range(16):
    for x in range(16):
        c = hexc('#5f7a86') if rnd.random() < 0.6 else hexc('#4a6270')
        if rnd.random() < 0.05: c = CRYS
        top.putpixel((x, y), jitter(c, 8, rnd))
save(top, 'block/stormscar_turf_top')
rnd = random.Random(302); side = Image.new('RGBA', (16, 16))
for y in range(16):
    for x in range(16):
        side.putpixel((x, y), jitter(hexc('#79553a') if rnd.random() < 0.7 else hexc('#5e412c'), 6, rnd))
for x in range(16):
    h = rnd.randint(2, 5)
    for y in range(h): side.putpixel((x, y), jitter(hexc('#5f7a86'), 8, rnd))
    if rnd.random() < 0.2: side.putpixel((x, h), CRYS)
save(side, 'block/stormscar_turf_side')
# weather bottles
def bottle(fill, hi, name, sym):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); d = img.load()
    rows = ['................', '......CC........', '......KK........', '......GG........', '.....G..G.......', '.....G..G.......',
            '....G....G......', '...GFFFFFFG.....', '...GFFFFFFG.....', '...GFFFFFFG.....', '...GFFFFFFG.....', '...GFFFFFFG.....',
            '....GGGGGG......', '................', '................', '................']
    pal = {'C': '#8b5a2b', 'K': '#6b4220', 'G': '#cfe8f2', 'F': fill}
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != '.': d[x, y] = hexc(pal[ch])
    for (x, y) in sym: d[x, y] = hexc(hi)
    save(img, f'item/{name}')
bottle('#9fd8ff', '#ffe066', 'bottled_sunshine', [(6, 9), (5, 9), (7, 9), (6, 8), (6, 10), (4, 8), (8, 10), (4, 10), (8, 8)])
bottle('#3c5a8a', '#9fe8ff', 'bottled_rain', [(5, 8), (5, 9), (8, 9), (8, 10), (6, 10), (6, 11)])
bottle('#2a3644', '#ffe066', 'bottled_thunder', [(7, 7), (6, 8), (6, 9), (7, 9), (6, 10), (5, 11)])
# paintings (our own pixel art)
def painting(name, w, h, draw):
    img = Image.new('RGBA', (w, h), (0, 0, 0, 255)); draw(img, ImageDraw.Draw(img), random.Random(hash(name) & 0xffff)); save(img, f'painting/{name}')
def sea_sky(img, rnd, horizon):
    w, h = img.size
    for y in range(h):
        for x in range(w):
            if y < horizon:
                t = y / horizon; c = (int(45 + 40 * t), int(52 + 45 * t), int(70 + 50 * t), 255)
            else:
                t = (y - horizon) / max(1, h - horizon); c = (int(28 + 12 * t), int(60 + 25 * t), int(98 + 20 * t), 255)
            img.putpixel((x, y), jitter(c, 6, rnd))
def p_light(img, d, rnd):
    sea_sky(img, rnd, 34)
    d.rectangle([12, 14, 19, 38], fill=(220, 222, 228)); 
    for y in range(16, 38, 6): d.rectangle([12, y, 19, y + 2], fill=(190, 60, 50))
    d.rectangle([11, 9, 20, 13], fill=(70, 80, 95)); d.rectangle([13, 10, 18, 12], fill=(255, 240, 150))
    d.polygon([(18, 11), (31, 6), (31, 16)], fill=(255, 245, 190, 255))
    d.rectangle([6, 38, 26, 47], fill=(80, 88, 100))
def p_sea(img, d, rnd):
    sea_sky(img, rnd, 14)
    for i in range(0, 64, 5): d.line([(i, 18 + (i % 10) // 3), (i + 4, 16 + (i % 10) // 3)], fill=(225, 235, 245))
    d.polygon([(22, 20), (40, 20), (37, 25), (25, 25)], fill=(110, 80, 55)); d.line([(31, 8), (31, 20)], fill=(90, 70, 50))
    d.polygon([(32, 9), (40, 18), (32, 18)], fill=(230, 225, 210))
    d.line([(50, 0), (46, 6), (49, 6), (44, 14)], fill=(210, 247, 255)); d.line([(10, 0), (13, 5), (11, 5), (14, 11)], fill=(210, 247, 255))
def p_warden(img, d, rnd):
    for y in range(32):
        for x in range(32): img.putpixel((x, y), jitter((30, 36, 50, 255), 6, rnd))
    for (cx, cy, r) in ((16, 10, 8), (16, 17, 6), (16, 23, 4), (16, 28, 2)):
        d.ellipse([cx - r, cy - r // 2 - 1, cx + r, cy + r // 2 + 1], fill=(70, 80, 100))
    d.rectangle([12, 8, 13, 9], fill=(255, 240, 120)); d.rectangle([19, 8, 20, 9], fill=(255, 240, 120))
    d.line([(4, 2), (7, 8), (5, 8), (8, 15)], fill=(111, 220, 255)); d.line([(28, 3), (25, 9), (27, 9), (24, 16)], fill=(111, 220, 255))
painting('storm_lighthouse', 32, 48, p_light)
painting('squall_at_sea', 64, 32, p_sea)
painting('the_stormwarden', 32, 32, p_warden)
print('textures3 done')
