"""Stormreach 5.0 textures: tornadoes, meteorology, backpacks, the Hollow Labyrinth, the story, Elderstorm Grove."""
import os as _os, pathlib as _pl
ROOT = str(_pl.Path(__file__).resolve().parent.parent)  # the Stormreach project folder
import random, os, math
from PIL import Image, ImageDraw, ImageFilter
OUT = ROOT + '/src/main/resources/assets/stormreach/textures'
for d in ('block', 'item', 'entity', 'entity/equipment/humanoid', 'entity/equipment/humanoid_leggings'):
    os.makedirs(f'{OUT}/{d}', exist_ok=True)


def hexc(h, a=255):
    h = h.lstrip('#'); return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)
def jitter(c, amt, rnd):
    d = rnd.randint(-amt, amt); return tuple(max(0, min(255, v + d)) for v in c[:3]) + (c[3],)
def save(img, path): img.save(f'{OUT}/{path}.png')
def load(path): return Image.open(f'{OUT}/{path}.png').convert('RGBA')
def noise(base, amt, seed, w=16, h=16):
    rnd = random.Random(seed); img = Image.new('RGBA', (w, h))
    for y in range(h):
        for x in range(w): img.putpixel((x, y), jitter(base, amt, rnd))
    return img, rnd
def blank(w=16, h=16): return Image.new('RGBA', (w, h), (0, 0, 0, 0))
def shade(c, f): return tuple(max(0, min(255, int(v * f))) for v in c[:3]) + (c[3],)
def art(rows, palette, name):
    img = blank()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != '.': img.putpixel((x, y), hexc(palette[ch]))
    save(img, f'item/{name}')
def outline(img, col):
    """Adds a 1px dark outline around opaque pixels (item-icon style)."""
    out = img.copy(); px = img.load()
    for y in range(img.height):
        for x in range(img.width):
            if px[x, y][3]: continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                xx, yy = x + dx, y + dy
                if 0 <= xx < img.width and 0 <= yy < img.height and px[xx, yy][3] > 0:
                    out.putpixel((x, y), col); break
    return out

METAL = hexc('#9aa4ad'); METAL_D = hexc('#5d666e'); METAL_L = hexc('#d4dbe0')
RUST = hexc('#8a4b2a'); RUST_D = hexc('#5a2e1a'); RUST_L = hexc('#b8703f')
VOID = hexc('#1c1726'); VOID_L = hexc('#3a2f52'); HOLLOW = hexc('#7f6cff'); HOLLOW_L = hexc('#c9c0ff')
STORM = hexc('#6fdcff'); STORM_D = hexc('#2b7fa3'); GOLD = hexc('#f2c94c'); GOLD_D = hexc('#a07a1e'); RED = hexc('#e04a4a')
ELDER = hexc('#4a3b5c'); ELDER_D = hexc('#2f2540'); ELDER_L = hexc('#6d5a85')
EPLANK = hexc('#7a6696'); EPLANK_D = hexc('#574872'); EPLANK_L = hexc('#9b88b6')
ELEAF = hexc('#3fa0a8'); ELEAF_D = hexc('#2b6f86'); FRUIT = hexc('#7ee8ff'); FRUIT_L = hexc('#e6fcff'); FRUIT_D = hexc('#2f9cc8')

# =====================================================================================
# Tornado funnel (64x64): translucent streaky condensation funnel with dust near the base
# =====================================================================================
rnd = random.Random(5001); img = blank(64, 64)
for y in range(64):
    for x in range(64):
        streak = 0.5 + 0.5 * math.sin((x * 0.7 + y * 0.25) + math.sin(y * 0.4) * 2)
        g = int(120 + 60 * streak + rnd.randint(-14, 14))
        a = int(120 + 90 * streak + rnd.randint(-20, 20))
        tint = (g, g + 4, g + 10)
        if y % 32 > 24: tint = (int(g * 0.85) + 18, int(g * 0.78) + 10, int(g * 0.7))  # dust skirt band
        img.putpixel((x, y), tint + (max(40, min(235, a)),))
save(img, 'entity/tornado')

# =====================================================================================
# Meteorology blocks
# =====================================================================================
def casing(seed, base=METAL, dark=METAL_D, light=METAL_L):
    img, rnd = noise(base, 6, seed)
    for i in range(16):
        img.putpixel((i, 0), light); img.putpixel((0, i), light); img.putpixel((i, 15), dark); img.putpixel((15, i), dark)
    for (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13)): img.putpixel((x, y), dark)  # rivets
    return img, rnd
def screen(img, x0, y0, x1, y1, bg, fg, rnd, kind):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1): img.putpixel((x, y), bg)
    if kind == 'radar':
        cx, cy = (x0 + x1) / 2, (y0 + y1) / 2; r = (x1 - x0) / 2
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                d = math.hypot(x - cx, y - cy)
                if abs(d - r * 0.5) < 0.5 or abs(d - r) < 0.6: img.putpixel((x, y), shade(fg, 0.6))
        for k in range(int(r)): img.putpixel((int(cx + k * 0.7), int(cy - k * 0.7)), fg)
        img.putpixel((int(cx) - 2, int(cy) + 1), RED); img.putpixel((int(cx) - 1, int(cy) + 1), hexc('#4ad04a'))
    elif kind == 'graph':
        for x in range(x0 + 1, x1):
            y = int((y0 + y1) / 2 + math.sin(x * 0.9) * (y1 - y0) / 3)
            img.putpixel((x, y), fg)
    elif kind == 'text':
        for y in range(y0 + 1, y1, 2):
            for x in range(x0 + 1, x1 - rnd.randint(0, 3)): img.putpixel((x, y), fg)
for name, kind, scr in (('weather_station', 'text', hexc('#163a2a')), ('tornado_radar', 'radar', hexc('#0e2a1a')), ('weather_monitor', 'graph', hexc('#101c33')),
                        ('storm_detector', 'graph', hexc('#2a1030'))):
    img, rnd = casing(5010 + len(name))
    fg = {'radar': hexc('#4ad04a'), 'graph': STORM, 'text': hexc('#7dff9a')}[kind]
    if name == 'storm_detector': fg = hexc('#ff8a5a')
    screen(img, 3, 3, 12, 10, scr, fg, rnd, kind)
    for x in (4, 7, 10): img.putpixel((x, 12), RED if x == 4 else GOLD)
    save(img, f'block/{name}_front')
    img, rnd = casing(5020 + len(name)); save(img, f'block/{name}_side')
    img, rnd = casing(5030 + len(name), METAL_D, shade(METAL_D, 0.7), METAL)
    if name == 'tornado_radar':
        for y in range(16):
            for x in range(16):
                if math.hypot(x - 7.5, y - 7.5) < 5.5: img.putpixel((x, y), METAL_L if (x + y) % 3 else METAL)
    save(img, f'block/{name}_top')
# siren: horn grille faces
img, rnd = casing(5040, hexc('#c8a23a'), hexc('#7d6420'), hexc('#ead27a'))
for y in range(16):
    for x in range(16):
        d = math.hypot(x - 7.5, y - 7.5)
        if 1.5 < d < 6.5 and int(d * 1.6) % 2 == 0: img.putpixel((x, y), hexc('#3b2f12'))
save(img, 'block/tornado_siren_front')
img, rnd = casing(5041, hexc('#c8a23a'), hexc('#7d6420'), hexc('#ead27a')); save(img, 'block/tornado_siren_side')
# anemometer / thermometer textures (used by custom element models)
img, rnd = noise(METAL, 6, 5050); save(img, 'block/anemometer')
img, rnd = noise(hexc('#e8ecef'), 4, 5051)
for y in range(2, 14): img.putpixel((7, y), RED); img.putpixel((8, y), RED)
for y in range(3, 14, 2): img.putpixel((10, y), METAL_D)
for (x, y) in ((6, 13), (7, 14), (8, 14), (9, 13)): img.putpixel((x, y), RED)
save(img, 'block/thermometer')

# handheld items
art(['................', '..KKKKKKKKKKK...', '..KSSSSSSSSSK...', '..KSgSSSSSSSK...', '..KSSgSSRSSSK...', '..KSSSgSSSSSK...', '..KSSSSgSSSSK...',
     '..KSSSSSSSSSK...', '..KKKKKKKKKKK...', '..KMMMMMMMMMK...', '..KMRMMGMMYMK...', '..KMMMMMMMMMK...', '..KKKKKKKKKKK...', '................',
     '................', '................'], {'K': '#2b3036', 'S': '#0e2a1a', 'g': '#4ad04a', 'R': '#e04a4a', 'M': '#7d868f', 'G': '#4ad04a', 'Y': '#f2c94c'}, 'storm_radar')
art(['..........A.....', '..........A.....', '..........A.....', '...KKKKKKKKKK...', '...KMMMMMMMMK...', '...KMggggggMK...', '...KMggggggMK...',
     '...KMMMMMMMMK...', '...KMoMoMoMoK...', '...KMMMMMMMMK...', '...KMoMoMoMoK...', '...KMMMMMMMMK...', '...KMMRMMYMMK...', '...KKKKKKKKKK...',
     '................', '................'], {'K': '#2b3036', 'M': '#c9452e', 'g': '#7dff9a', 'o': '#5a1f14', 'R': '#ffffff', 'Y': '#f2c94c', 'A': '#9aa4ad'}, 'weather_radio')

# tornado spawners: a swirl bottle, one tint per morphology
SPAWN_TINT = {'natural': '#9aa4ad', 'rope': '#d8dde2', 'needle': '#c0c8d0', 'drill_bit': '#8f8a84', 'cone': '#7f8a94', 'stovepipe': '#5f6a74',
              'wedge': '#4a525a', 'large_wedge': '#33383d', 'multi_vortex': '#6a6f96', 'satellite': '#7f9fb0', 'companion': '#9f8fb0',
              'landspout': '#b39a74', 'waterspout': '#5f9fd0'}
def funnel_icon(name, tint, shape, seed):
    img = blank(); rnd = random.Random(seed); c = hexc(tint)
    for y in range(1, 15):
        t = y / 14
        if shape == 'rope': half = 0.8 + math.sin(y * 0.9) * 0.3
        elif shape == 'needle': half = 3.5 * (1 - t) + 0.4
        elif shape == 'drill_bit': half = 5.5 if y < 6 else 1.0 + (14 - y) * 0.15
        elif shape == 'stovepipe': half = 3.0
        elif shape in ('wedge', 'large_wedge'): half = (6.8 if shape == 'large_wedge' else 5.5) - t * 1.0
        elif shape == 'landspout': half = 1.0 + (0.6 if y > 11 else 0)
        else: half = 6 * (1 - t) + 1
        cx = 7.5 + (math.sin(y * 0.6) * 1.5 if shape in ('rope', 'landspout') else 0)
        for x in range(16):
            if abs(x - cx) <= half: img.putpixel((x, y), jitter(c, 14, rnd) if (x + y) % 4 else shade(c, 1.25))
    if shape == 'multi_vortex':
        for y in range(4, 15):
            for cx in (4, 11):
                img.putpixel((int(cx + math.sin(y * 0.8)), y), hexc('#e6edf3'))
    if shape in ('satellite', 'companion'):
        for y in range(6, 15): img.putpixel((13 + (y % 2), y), hexc('#e6edf3'))
    if shape == 'waterspout':
        for x in range(2, 14): img.putpixel((x, 15), hexc('#2b7fd8'))
    if shape == 'landspout':
        for x in range(4, 12): img.putpixel((x, 15), hexc('#b39a74'))
    for x in range(1, 15): img.putpixel((x, 0), hexc('#3a4048'))  # the parent cloud base
    save(outline(img, hexc('#1a1d21')), f'item/{name}')
for k, tint in SPAWN_TINT.items(): funnel_icon(f'tornado_spawner_{k}', tint, k, 5100 + len(k))
# weather event items: rune stones
def rune(name, base, glyph, seed):
    img = blank(); rnd = random.Random(seed)
    for y in range(2, 14):
        for x in range(3, 13):
            if (x, y) in ((3, 2), (12, 2), (3, 13), (12, 13)): continue
            img.putpixel((x, y), jitter(hexc('#4b4f63'), 8, rnd))
    for (x, y) in glyph: img.putpixel((x, y), hexc(base))
    save(outline(img, hexc('#15161c')), f'item/{name}')
rune('weather_event_supercell', '#9fe3ff', [(x, 5) for x in range(5, 11)] + [(8, y) for y in range(6, 11)] + [(7, 11), (6, 12)], 5150)
rune('weather_event_downburst', '#ffffff', [(8, y) for y in range(4, 10)] + [(5, 10), (6, 10), (7, 11), (8, 11), (9, 11), (10, 10), (11, 10), (4, 11), (12, 11)], 5151)
rune('weather_event_hailstorm', '#e6f6ff', [(5, 5), (8, 4), (11, 6), (6, 8), (9, 8), (5, 11), (8, 11), (11, 10), (7, 6), (10, 9)], 5152)
rune('weather_event_ball_lightning', '#f2c94c', [(x, y) for x in range(6, 11) for y in range(6, 11) if math.hypot(x - 8, y - 8) < 2.4], 5153)
rune('weather_event_dispel', '#7dff9a', [(x, 8) for x in range(5, 12)] + [(8, y) for y in range(5, 12)] + [(6, 6), (10, 10), (10, 6), (6, 10)], 5154)

# =====================================================================================
# Backpacks
# =====================================================================================
BP = {'diamond': ('#4ad6c8', '#1f8a80', '#b8fff6'), 'obsidian': ('#3b2b55', '#1a1226', '#7d5fb0'), 'netherite': ('#4a4144', '#2a2426', '#857a7e'),
      'storm_crystal': ('#6fdcff', '#2b7fa3', '#e6fbff'), 'voltite': ('#f2c94c', '#8a6a1e', '#fff3b0'), 'aurorite': ('#d66be8', '#7a2a8e', '#ffd6ff'),
      'maelstrom': ('#7f6cff', '#2b1f66', '#f1e2ff'), 'storm_chasers_pack': ('#c9452e', '#6e2016', '#f2c94c')}
def backpack(name, col, seed):
    base, dark, light = (hexc(c) for c in col); rnd = random.Random(seed)
    leather = hexc('#7a5230'); img = blank()
    for y in range(3, 15):
        for x in range(3, 13):
            if (x, y) in ((3, 3), (12, 3), (3, 14), (12, 14)): continue
            img.putpixel((x, y), jitter(base, 8, rnd))
    for x in range(4, 12): img.putpixel((x, 7), dark); img.putpixel((x, 6), light if x % 3 else dark)  # flap edge
    for y in range(3, 7):
        for x in range(4, 12): img.putpixel((x, y), jitter(shade(base, 0.85), 6, rnd))
    for y in range(9, 13):
        for x in range(5, 11): img.putpixel((x, y), jitter(shade(base, 0.9), 5, rnd))
    for x in range(5, 11): img.putpixel((x, 9), dark)
    img.putpixel((7, 8), hexc('#e0e0e0')); img.putpixel((8, 8), hexc('#e0e0e0'))  # clasp
    for (x, y) in ((6, 1), (7, 1), (8, 1), (9, 1), (5, 2), (10, 2)): img.putpixel((x, y), leather)  # handle
    for y in range(4, 14): img.putpixel((2, y), leather); img.putpixel((13, y), leather)  # straps
    save(outline(img, shade(dark, 0.6)), f'item/{name}')
for k, col in BP.items(): backpack(k if k == 'storm_chasers_pack' else f'{k}_backpack', col, 5200 + len(k))
def kit(name, col, seed):
    base, dark, light = (hexc(c) for c in col); rnd = random.Random(seed); img = blank()
    for y in range(5, 13):
        for x in range(2, 14): img.putpixel((x, y), jitter(hexc('#8a6a48'), 6, rnd))
    for x in range(2, 14): img.putpixel((x, 5), hexc('#5a4430'))
    for y in range(3, 11):
        for x in range(5, 11): img.putpixel((x, y), jitter(base, 8, rnd))
    for (x, y) in ((7, 6), (8, 6), (7, 7), (8, 7)): img.putpixel((x, y), light)
    for x in range(1, 4): img.putpixel((x + 10, 12), dark); img.putpixel((x + 11, 13), dark)
    save(outline(img, hexc('#2a1d12')), f'item/{name}')
for k, col in BP.items():
    if k not in ('diamond', 'storm_chasers_pack'): kit(f'{k}_backpack_upgrade', col, 5250 + len(k))

# =====================================================================================
# Hollow Labyrinth
# =====================================================================================
img, rnd = noise(hexc('#2d2a33'), 6, 5300)
for y in range(16):
    for x in range(16):
        if (x + y * 3) % 7 == 0: img.putpixel((x, y), jitter(VOID_L, 8, rnd))
        if abs(x - 7.5) < 1 or abs(y - 7.5) < 1: img.putpixel((x, y), jitter(HOLLOW, 20, rnd))
save(img, 'block/labyrinth_seal')
for k in range(4):
    img, rnd = noise(hexc('#3b3428'), 6, 5310 + k)
    for i in range(16): img.putpixel((i, 0), GOLD_D); img.putpixel((i, 15), GOLD_D); img.putpixel((0, i), GOLD_D); img.putpixel((15, i), GOLD_D)
    for (x, y) in ((7, 4), (8, 4), (6, 5), (9, 5), (6, 6), (9, 6), (7, 7), (8, 7), (7, 8), (8, 8), (7, 9), (8, 9), (7, 10), (8, 10)):
        img.putpixel((x, y), hexc('#0b0a08'))
    for j in range(k):  # one lit notch per inserted key
        for x in (3 + j * 4, 4 + j * 4): img.putpixel((x, 13), GOLD); img.putpixel((x, 12), hexc('#fff3b0'))
    save(img, f'block/labyrinth_keyhole_{k}')
img, rnd = noise(hexc('#3b3428'), 6, 5320); save(img, 'block/labyrinth_keyhole_side')
for half in ('top', 'bottom'):
    img, rnd = noise(RUST, 10, 5330 + (half == 'top'))
    for y in range(16):
        for x in range(16):
            if rnd.random() < 0.18: img.putpixel((x, y), jitter(RUST_L, 10, rnd))
            if x in (0, 15): img.putpixel((x, y), RUST_D)
    edge = 0 if half == 'top' else 15
    for x in range(16): img.putpixel((x, edge), RUST_D)
    if half == 'top':
        for y in range(3, 9, 2):
            for x in range(4, 12): img.putpixel((x, y), hexc('#120c08'))  # vents
    else:
        img.putpixel((12, 2), METAL_L); img.putpixel((12, 3), METAL)
    save(img, f'block/rusted_locker_{half}')
# the locker's front grate, seen from outside and (through the slats) from inside while hiding
for half in ('upper', 'lower'):
    img, rnd = noise(RUST, 10, 5335 + (half == 'upper'))
    for y in range(16):
        for x in range(16):
            if rnd.random() < 0.15: img.putpixel((x, y), jitter(RUST_L, 10, rnd))
            if x in (0, 15): img.putpixel((x, y), RUST_D)
    rows = range(2, 13, 2) if half == 'upper' else range(9, 14, 2)
    for y in rows:
        for x in range(2, 14): img.putpixel((x, y), (0, 0, 0, 0))
        for x in range(2, 14): img.putpixel((x, y + 1), jitter(RUST_D, 6, rnd))  # slat lip shadow
    if half == 'lower':
        img.putpixel((12, 3), METAL_L); img.putpixel((12, 4), METAL)  # handle
    save(img, f'block/rusted_locker_grate_{half}')
img = blank(); icon_t = load('block/rusted_locker_top').resize((8, 8), Image.NEAREST); icon_b = load('block/rusted_locker_bottom').resize((8, 8), Image.NEAREST)
img.paste(icon_t, (4, 0)); img.paste(icon_b, (4, 8)); save(img, 'item/rusted_locker')
art(['................', '................', '.....GGG........', '....GYYYG.......', '....GY.YG.......', '....GYYYG.......', '.....GGG........',
     '......G.........', '......G.........', '......GG........', '......G.........', '......GGG.......', '......G.........', '................',
     '................', '................'], {'G': '#a07a1e', 'Y': '#f2c94c'}, 'labyrinth_key')
img = blank(); rnd = random.Random(5340)
for y in range(16):
    for x in range(16):
        d = math.hypot(x - 7.5, (y - 8) * 1.1)
        heart = (abs(x - 7.5) + max(0, 6 - y)) < 6.5 and y > 2 and y < 14 and (abs(x - 7.5) < 7.5 - (y - 6) * 0.9 if y > 6 else True)
        if heart: img.putpixel((x, y), jitter(HOLLOW if d > 3 else HOLLOW_L, 14, rnd))
save(outline(img, VOID), 'item/heart_of_the_labyrinth')

# =====================================================================================
# Story: journal, remnant, core, crown, scepter, weatherbreaker, rewards, shrine blocks, trophies
# =====================================================================================
def page(name, seed, torn=False):
    img = blank(); rnd = random.Random(seed)
    for y in range(2, 15):
        for x in range(3, 13):
            if torn and x > 10 and (y * 3 + x) % 5 == 0: continue
            img.putpixel((x, y), jitter(hexc('#d8c8a0'), 8, rnd))
    for y in range(4, 13, 2):
        for x in range(4, 11 - rnd.randint(0, 3)): img.putpixel((x, y), hexc('#6b5a48'))
    save(outline(img, hexc('#5a4a30')), f'item/{name}')
for i in range(1, 6): page(f'journal_page_{i}', 5400 + i, torn=True)
art(['................', '..BBBBBBBBBBB...', '..BLLLLLLLLLBb..', '..BLPPPPPPPLBb..', '..BLPkPPPPPLBb..', '..BLPPkPPPPLBb..', '..BLPPPkPPPLBb..',
     '..BLPPPPPPPLBb..', '..BLLLLLLLLLBb..', '..BLLLGGLLLLBb..', '..BLLLGGLLLLBb..', '..BLLLLLLLLLBb..', '..BBBBBBBBBBBb..', '...bbbbbbbbbbb..',
     '................', '................'], {'B': '#3a2a22', 'L': '#5a3f30', 'P': '#c8b890', 'k': '#7f6cff', 'G': '#c9c0ff', 'b': '#1e1612'}, 'weathered_journal')
img = blank(); rnd = random.Random(5410)
for y in range(3, 14):
    for x in range(4, 12):
        if math.hypot(x - 7.5, y - 8) < 4.6 + rnd.random(): img.putpixel((x, y), jitter(VOID_L, 12, rnd))
for (x, y) in ((6, 7), (9, 7)): img.putpixel((x, y), HOLLOW_L)
save(outline(img, VOID), 'item/hollow_remnant')
img = blank(); rnd = random.Random(5411)
for y in range(16):
    for x in range(16):
        d = math.hypot(x - 7.5, y - 7.5); ang = math.atan2(y - 7.5, x - 7.5)
        if d < 6.2:
            sw = 0.5 + 0.5 * math.sin(ang * 3 + d * 1.3)
            img.putpixel((x, y), jitter(HOLLOW_L if d < 1.8 else (HOLLOW if sw > 0.5 else hexc('#2b1f66')), 10, rnd))
save(outline(img, hexc('#120c2a')), 'item/maelstrom_core')
art(['................', '................', '..G..G..G..G....', '..GG.GG.GG.GG...', '..GGGGGGGGGGG...', '..GSSGSSGSSGG...', '..GGGGGGGGGGG...',
     '..gggggggggggg..', '................', '................', '................', '................', '................', '................',
     '................', '................'], {'G': '#9fb3c2', 'S': '#7f6cff', 'g': '#5d666e'}, 'stormcrown')
art(['..............C.', '.............CSC', '..............C.', '.............M..', '............M...', '...........M....', '..........M.....',
     '.........M......', '........M.......', '.......M........', '......M.........', '.....M..........', '....M...........', '...G............',
     '..G.............', '................'], {'C': '#f1e2ff', 'S': '#7f6cff', 'M': '#6d5a85', 'G': '#f2c94c'}, 'weathervane_scepter')
art(['.............SS.', '............SLS.', '...........SLS..', '..........SLS...', '.........SLS....', '........SLS.....', '...H...SLS......',
     '...HH.SLS.......', '....HHLS........', '.....HH.........', '....HHHH........', '...HH..HH.......', '..GG....H.......', '.GG.............',
     '................', '................'], {'S': '#3a2f6b', 'L': '#c9c0ff', 'H': '#7f6cff', 'G': '#f2c94c'}, 'weatherbreaker')
art(['................', '.......GG.......', '......G..G......', '.......GG.......', '......YYYY......', '.....YY..YY.....', '....YY.YY.YY....',
     '....Y..YYY.Y....', '....Y.YYY..Y....', '....YY.YY.YY....', '.....YY..YY.....', '......YYYY......', '................', '................',
     '................', '................'], {'G': '#a07a1e', 'Y': '#f2c94c'}, 'lightning_charm')
art(['................', '................', '................', '.....RRRRRR.....', '....RRRRRRRR....', '...RRWWRRRRRR...', '...RRRRRRRRRR...',
     '...KKKKKKKKKK...', '.........KKKKK..', '................', '................', '................', '................', '................',
     '................', '................'], {'R': '#c9452e', 'W': '#f2c94c', 'K': '#5a1f14'}, 'storm_chaser_cap')
art(['................', '.....RRRRRR.....', '......RRRR......', '.......RR.......', '......GGGG......', '.....GYYYYG.....', '....GYYKKYYG....',
     '....GYKYYKYG....', '....GYYKKYYG....', '....GYKYYKYG....', '.....GYYYYG.....', '......GGGG......', '................', '................',
     '................', '................'], {'R': '#c9452e', 'G': '#a07a1e', 'Y': '#f2c94c', 'K': '#5a5f66'}, 'chasers_badge')

# shrine blocks
img, rnd = noise(VOID, 6, 5450)
for i in range(16): img.putpixel((i, 0), VOID_L); img.putpixel((i, 15), VOID_L)
save(img, 'block/relic_pedestal_side')
for lit in (False, True):
    img, rnd = noise(VOID_L, 6, 5451)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if abs(d - 5) < 0.7: img.putpixel((x, y), HOLLOW_L if lit else hexc('#4b4268'))
            if d < 2.2 and lit: img.putpixel((x, y), hexc('#ffffff'))
    save(img, 'block/relic_pedestal_top' + ('_lit' if lit else ''))
for struck in (False, True):
    for order in range(1, 5):
        img, rnd = noise(hexc('#b86f4a') if not struck else hexc('#5fd8c0'), 8, 5460 + order)
        for y in range(16):
            for x in (0, 15): img.putpixel((x, y), shade(img.getpixel((x, y)), 0.7))
        for j in range(order):  # notches tell the strike order
            for x in range(5, 11): img.putpixel((x, 3 + j * 3), hexc('#2a1810') if not struck else hexc('#e6fffb'))
        save(img, f'block/storm_conduit_{order}' + ('_struck' if struck else ''))
img, rnd = noise(hexc('#b86f4a'), 8, 5470); save(img, 'block/storm_conduit_top')
img, rnd = noise(VOID, 5, 5480)
for y in range(16):
    for x in range(16):
        if (x in (2, 13) or y in (2, 13)) and 2 <= x <= 13 and 2 <= y <= 13: img.putpixel((x, y), HOLLOW)
        if abs(x - 7.5) + abs(y - 7.5) < 3: img.putpixel((x, y), HOLLOW_L)
save(img, 'block/hollow_altar_top')
img, rnd = noise(VOID, 5, 5481)
for x in range(16): img.putpixel((x, 3), HOLLOW); img.putpixel((x, 12), HOLLOW)
for x in range(2, 14, 3): img.putpixel((x, 7), HOLLOW_L); img.putpixel((x + 1, 8), HOLLOW_L)
save(img, 'block/hollow_altar_side')
img, rnd = noise(shade(VOID, 0.8), 4, 5482); save(img, 'block/hollow_altar_bottom')
# trophies: gold base + coloured figure
img, rnd = noise(GOLD, 10, 5490)
for i in range(16): img.putpixel((i, 0), hexc('#fff3b0')); img.putpixel((i, 15), GOLD_D)
save(img, 'block/trophy_base')
for name, col in (('sovereign_trophy', '#3d3358'), ('keeper_trophy', '#4b4f63'), ('cyrrus_trophy', '#7f6cff'), ('tornado_trophy', '#9aa4ad')):
    img, rnd = noise(hexc(col), 12, 5491 + len(name))
    for y in range(0, 16, 4):
        for x in range(16): img.putpixel((x, y), shade(hexc(col), 1.3))
    save(img, f'block/{name}_figure')

# =====================================================================================
# Elderstorm Grove
# =====================================================================================
def log_side(base, dark, seed):
    img, rnd = noise(base, 6, seed)
    for x in range(16):
        if x % 4 == 1 or rnd.random() < 0.15:
            for y in range(16):
                if rnd.random() < 0.8: img.putpixel((x, y), jitter(dark, 5, rnd))
    return img
def log_top(bark, ring_a, ring_b, seed):
    img, rnd = noise(ring_a, 5, seed)
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - 7.5, y - 7.5)
            if x in (0, 15) or y in (0, 15): img.putpixel((x, y), jitter(bark, 5, rnd))
            elif int(r) % 3 == 0: img.putpixel((x, y), jitter(ring_b, 5, rnd))
    return img
def planks(base, dark, light, seed):
    img, rnd = noise(base, 6, seed)
    for y in range(16):
        for x in range(16):
            if y % 4 == 3: img.putpixel((x, y), jitter(dark, 4, rnd))
            elif rnd.random() < 0.12: img.putpixel((x, y), jitter(light, 4, rnd))
    for y in range(0, 16, 4):
        x = (y * 7 + 3) % 16
        for yy in range(y, y + 3): img.putpixel((x, yy), jitter(dark, 4, rnd))
    return img
bark = log_side(ELDER, ELDER_D, 5500); rnd = random.Random(5501)
for i in range(5):  # glowing veins of stored charge
    x, y = rnd.randrange(16), rnd.randrange(16)
    for k in range(4): bark.putpixel((x, (y + k) % 16), FRUIT if k else FRUIT_L)
save(bark, 'block/elderstorm_log')
save(log_top(ELDER, EPLANK_L, EPLANK_D, 5502), 'block/elderstorm_log_top')
save(log_side(EPLANK_L, EPLANK, 5503), 'block/stripped_elderstorm_log')
save(log_top(EPLANK_L, EPLANK_L, EPLANK, 5504), 'block/stripped_elderstorm_log_top')
save(planks(EPLANK, EPLANK_D, EPLANK_L, 5505), 'block/elderstorm_planks')
img = blank(); rnd = random.Random(5506)
for y in range(16):
    for x in range(16):
        if rnd.random() < 0.78: img.putpixel((x, y), jitter(ELEAF if rnd.random() < 0.6 else ELEAF_D, 10, rnd))
        if rnd.random() < 0.025: img.putpixel((x, y), FRUIT_L)
save(img, 'block/elderstorm_leaves')
img = blank(); rnd = random.Random(5507)
for y in range(8, 16): img.putpixel((8, y), ELDER)
for (cx, cy, r) in ((8, 5, 3.2), (5, 8, 2.2), (11, 8, 2.2)):
    for y in range(16):
        for x in range(16):
            if math.hypot(x - cx, y - cy) < r: img.putpixel((x, y), jitter(ELEAF, 12, rnd))
img.putpixel((8, 5), FRUIT_L)
save(img, 'block/elderstorm_sapling')
for half in ('top', 'bottom'):
    img, rnd = noise(EPLANK, 6, 5510 + (half == 'top'))
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y == (0 if half == 'top' else 15): img.putpixel((x, y), jitter(ELDER, 4, rnd))
            elif x in (5, 10): img.putpixel((x, y), jitter(EPLANK_D, 4, rnd))
    if half == 'top':
        for y in range(3, 9):
            for x in range(3, 13):
                if (x - 7.5) ** 2 / 20 + (y - 6) ** 2 / 8 < 1: img.putpixel((x, y), (0, 0, 0, 0))
    else:
        img.putpixel((12, 1), FRUIT); img.putpixel((12, 2), FRUIT)
    save(img, f'block/elderstorm_door_{half}')
icon = blank(); icon.paste(load('block/elderstorm_door_top').resize((8, 8), Image.NEAREST), (4, 0))
icon.paste(load('block/elderstorm_door_bottom').resize((8, 8), Image.NEAREST), (4, 8)); save(icon, 'item/elderstorm_door')
img, rnd = noise(EPLANK, 6, 5512)
for y in range(16):
    for x in range(16):
        if x in (0, 15) or y in (0, 15): img.putpixel((x, y), jitter(ELDER, 4, rnd))
        elif 4 <= x <= 11 and 4 <= y <= 11 and (x - 7.5) ** 2 + (y - 7.5) ** 2 < 10: img.putpixel((x, y), (0, 0, 0, 0))
save(img, 'block/elderstorm_trapdoor')
for age in range(3):  # stormfruit pod, hanging from leaves
    img = blank(); rnd = random.Random(5520 + age); r = (1.8, 2.8, 3.8)[age]; cy = 16 - (5, 7, 9)[age]
    for y in range(0, int(cy - r) + 1): img.putpixel((8, y), ELDER)
    for y in range(16):
        for x in range(16):
            if math.hypot(x - 7.5, (y - cy) * 0.9) < r:
                c = (hexc('#6aa080'), FRUIT_D, FRUIT)[age]
                img.putpixel((x, y), jitter(c, 12, rnd) if (x + y) % 3 else (FRUIT_L if age == 2 else shade(c, 1.2)))
    save(img, f'block/stormfruit_pod_stage{age}')
img = blank(); rnd = random.Random(5530)  # thunderbloom: yellow zig-zag petals
for y in range(8, 16): img.putpixel((8, y), hexc('#3f7f5a'))
img.putpixel((7, 12), hexc('#3f7f5a')); img.putpixel((9, 13), hexc('#3f7f5a'))
for (x, y) in ((8, 2), (7, 3), (8, 3), (9, 3), (6, 4), (8, 4), (10, 4), (5, 5), (7, 5), (9, 5), (11, 5), (6, 6), (8, 6), (10, 6), (7, 7), (9, 7)):
    img.putpixel((x, y), jitter(GOLD, 14, rnd))
img.putpixel((8, 5), hexc('#ffffff'))
save(img, 'block/thunderbloom')
img = blank(); rnd = random.Random(5531)  # sparkfern: cyan fronds with sparks
for i in range(7):
    x = rnd.randint(2, 13); h = rnd.randint(7, 14); lean = rnd.choice((-1, 1))
    for k in range(h):
        xx = max(0, min(15, x + lean * (k // 4)))
        img.putpixel((xx, 15 - k), jitter(hexc('#3fb8b0'), 10, rnd))
        if k % 3 == 2 and 0 <= xx + lean < 16: img.putpixel((xx + lean, 15 - k), jitter(hexc('#2b8f8a'), 8, rnd))
    img.putpixel((max(0, min(15, x + lean * (h // 4))), 15 - h), FRUIT_L)
save(img, 'block/sparkfern')
art(['................', '.......B........', '......BL........', '.....SSSSS......', '....SFFFFFS.....', '...SFFWFFFFS....', '...SFWFFFFFS....',
     '...SFFFFFFFS....', '...SFFFFFFFS....', '...SFFFFFFFS....', '....SFFFFFS.....', '.....SSSSS......', '................', '................',
     '................', '................'], {'B': '#4a3b5c', 'L': '#3fa0a8', 'S': '#2f9cc8', 'F': '#7ee8ff', 'W': '#e6fcff'}, 'stormfruit')
art(['................', '.......B........', '......BL........', '.....GGGGG......', '....GYYYYYG.....', '...GYYWYYYYG....', '...GYWYYYYYG....',
     '...GYYYYYYYG....', '...GYYYYYYYG....', '...GYYYYYYYG....', '....GYYYYYG.....', '.....GGGGG......', '................', '................',
     '................', '................'], {'B': '#4a3b5c', 'L': '#3fa0a8', 'G': '#a07a1e', 'Y': '#f2c94c', 'W': '#fffbe0'}, 'gilded_stormfruit')
art(['................', '.....KKKKK......', '......CCC.......', '.....G...G......', '....G.....G.....', '....GFFFFFG.....', '....GFWFFFG.....',
     '....GFFFFFG.....', '....GFFFFFG.....', '....GFFFFFG.....', '.....GGGGG......', '................', '................', '................',
     '................', '................'], {'K': '#7a5230', 'C': '#d8c8a0', 'G': '#c8e8f0', 'F': '#2f9cc8', 'W': '#7ee8ff'}, 'stormfruit_preserve')
art(['................', '.......W........', '.....W.W.W......', '......WWW.......', '....WWWWWWW.....', '......WWW.......', '.....W.W.W......',
     '.......W........', '.......w........', '.......w........', '.......w........', '................', '................', '................',
     '................', '................'], {'W': '#f4f6fa', 'w': '#b8a890'}, 'thistle_down')
art(['................', '..A.......A.....', '..A..A.A..A.....', '...AA...AA......', '....A...A.......', '....A...A.......', '....AAAAA.......',
     '......A.........', '......A.........', '......A.........', '......A.........', '................', '................', '................',
     '................', '................'], {'A': '#c9f4ff'}, 'glimmer_antler')

# =====================================================================================
# Spawn eggs
# =====================================================================================
def egg(base, spot, name, seed):
    img = blank(); rnd = random.Random(seed)
    for y in range(16):
        for x in range(16):
            dx = (x - 7.5) / 5.2; dy = (y - 8.5) / (6.8 if y > 8 else 7.4)
            if dx * dx + dy * dy <= 1.0:
                c = hexc(base)
                if rnd.random() < 0.18: c = hexc(spot)
                if dx * dx + dy * dy > 0.75: c = shade(c, 0.75)
                img.putpixel((x, y), c)
    img.putpixel((5, 4), (255, 255, 255, 255)); img.putpixel((5, 5), (240, 240, 240, 255))
    save(img, f'item/{name}')
egg('#3b3428', '#7f6cff', 'hollow_keeper_spawn_egg', 5600)
egg('#1c1726', '#e6e6e6', 'stranger_spawn_egg', 5601)
egg('#2b1f66', '#c9c0ff', 'cyrrus_spawn_egg', 5602)
egg('#7a6696', '#7ee8ff', 'glimmerstag_spawn_egg', 5603)
egg('#f4f6fa', '#3fb8b0', 'thistlehop_spawn_egg', 5604)

# =====================================================================================
# Entities (humanoid 64x64 layout: head 0,0  body 16,16  right arm 40,16  right leg 0,16, left arm 32,48, left leg 16,48)
# =====================================================================================
def uv_fill(img, u, v, w, h, d, base, rnd, top=None, amt=10):
    for yy in range(v, v + d + h):
        for xx in range(u, u + 2 * (d + w)):
            if (yy < v + d) and not (u + d <= xx < u + d + 2 * w): continue
            c = top if (top is not None and yy < v + d) else base
            img.putpixel((xx, yy), jitter(c, amt, rnd))
HUMAN = [((0, 0, 8, 8, 8), 'head'), ((16, 16, 8, 12, 4), 'body'), ((40, 16, 4, 12, 4), 'arm'), ((32, 48, 4, 12, 4), 'arm'),
         ((0, 16, 4, 12, 4), 'leg'), ((16, 48, 4, 12, 4), 'leg')]
def humanoid(name, colors, seed, eyes, face=None, glow=None):
    img = blank(64, 64); rnd = random.Random(seed)
    for (u, v, w_, h_, d_), part in HUMAN:
        base = colors[part]; uv_fill(img, u, v, w_, h_, d_, base, rnd, top=shade(base, 1.1), amt=9)
        if glow:
            for i in range(w_ * h_ // 5):
                x = rnd.randrange(u, u + 2 * (d_ + w_)); y = rnd.randrange(v + d_, v + d_ + h_)
                img.putpixel((x, y), glow)
    if face:
        for y in range(8, 16):
            for x in range(8, 16): img.putpixel((x, y), jitter(face, 6, rnd))
    for (x, y) in ((9, 12), (10, 12), (13, 12), (14, 12)): img.putpixel((x, y), eyes)
    return img
# the Stranger: a hooded traveller's silhouette; no face, just pale eyes
s = humanoid('stranger', {'head': hexc('#22202a'), 'body': hexc('#2b2733'), 'arm': hexc('#24212c'), 'leg': hexc('#1c1a22')}, 5700, hexc('#e8f0ff'), face=hexc('#0d0c10'))
s.save(f'{OUT}/entity/stranger.png')
c = humanoid('cyrrus', {'head': hexc('#3a2f6b'), 'body': hexc('#2b1f66'), 'arm': hexc('#3a2f6b'), 'leg': hexc('#241a52')}, 5701, hexc('#c9c0ff'),
             face=hexc('#1a1240'), glow=hexc('#9fe3ff'))
c.save(f'{OUT}/entity/cyrrus.png')
c = humanoid('cyrrus_final', {'head': hexc('#120c2a'), 'body': hexc('#1a1240'), 'arm': hexc('#120c2a'), 'leg': hexc('#0d0920')}, 5702, hexc('#ff6a6a'),
             face=hexc('#05030c'), glow=hexc('#7f6cff'))
c.save(f'{OUT}/entity/cyrrus_final.png')
k = humanoid('hollow_keeper', {'head': hexc('#4b4f63'), 'body': hexc('#3b3428'), 'arm': hexc('#4b4f63'), 'leg': hexc('#3b3428')}, 5703, HOLLOW_L,
             face=hexc('#1c1a22'), glow=RUST)
for y in range(12, 16):  # a jaw of rusted locker-slats
    for x in range(9, 15, 2): k.putpixel((x, y), RUST_L)
k.save(f'{OUT}/entity/hollow_keeper.png')
# Glimmerstag 64x64 (matches ForestModels.StagModel UVs)
img = blank(64, 64); rnd = random.Random(5710)
uv_fill(img, 0, 0, 8, 8, 16, hexc('#7a6696'), rnd, top=hexc('#8b78a8'))           # body
uv_fill(img, 0, 24, 4, 10, 4, hexc('#6d5a85'), rnd)                                # neck
uv_fill(img, 16, 24, 5, 4, 7, hexc('#7a6696'), rnd, top=hexc('#8b78a8'))          # head
uv_fill(img, 40, 0, 1, 7, 1, hexc('#c9f4ff'), rnd, amt=4)                          # antlers (glowing)
for x in range(40, 64):
    for y in range(0, 9): img.putpixel((x, y), jitter(hexc('#c9f4ff'), 6, rnd))
uv_fill(img, 48, 16, 2, 10, 2, hexc('#4a3b5c'), rnd)                               # legs
for i in range(30):  # pale spots on the flank
    img.putpixel((rnd.randrange(16, 40), rnd.randrange(16, 24)), hexc('#e6e0f0'))
img.putpixel((21, 33), hexc('#0b0a10')); img.putpixel((24, 33), hexc('#0b0a10'))
img.save(f'{OUT}/entity/glimmerstag.png')
img = blank(32, 32); rnd = random.Random(5711)
uv_fill(img, 0, 0, 7, 7, 7, hexc('#f4f6fa'), rnd, top=hexc('#ffffff'), amt=12)
for i in range(25): img.putpixel((rnd.randrange(0, 28), rnd.randrange(0, 14)), hexc('#3fb8b0'))
uv_fill(img, 0, 14, 2, 4, 1, hexc('#e6edf3'), rnd); uv_fill(img, 14, 14, 2, 2, 3, hexc('#9fb3c2'), rnd)
img.putpixel((9, 10), hexc('#1c1726')); img.putpixel((12, 10), hexc('#1c1726'))
img.save(f'{OUT}/entity/thistlehop.png')

# equipment layers (64x32)
def armor_layer(path, name, parts, base, top, spark_a, spark_b):
    img = blank(64, 32); rnd = random.Random(hash(path + name) & 0xffff)
    for (u, v, w_, h_, d_) in parts:
        uv_fill(img, u, v, w_, h_, d_, base, rnd, top=top, amt=10)
        for i in range(w_ * h_ // 3):
            x = rnd.randrange(u, u + 2 * (d_ + w_)); y = rnd.randrange(v + d_, v + d_ + h_)
            img.putpixel((x, y), spark_a if rnd.random() < 0.4 else spark_b)
    img.save(f'{OUT}/entity/equipment/{path}/{name}.png')
armor_layer('humanoid', 'stormcrown', [(0, 0, 8, 8, 8), (32, 0, 8, 8, 8)], hexc('#9fb3c2'), hexc('#c9c0ff'), hexc('#7f6cff'), hexc('#ffffff'))
armor_layer('humanoid', 'storm_chaser_cap', [(0, 0, 8, 8, 8)], hexc('#c9452e'), hexc('#d8573e'), hexc('#f2c94c'), hexc('#8a2a1a'))
print('textures5 done')

# advancement tab backgrounds (16x16 tiles)
os.makedirs(f'{OUT}/gui/advancements/backgrounds', exist_ok=True)
for name, base, accent, seed in (('meteorology', hexc('#3b4a5c'), STORM, 5800), ('labyrinth', hexc('#2d2a33'), HOLLOW, 5801), ('chronicle', hexc('#1c1726'), hexc('#c9c0ff'), 5802)):
    img, rnd = noise(base, 7, seed)
    for i in range(16): img.putpixel((i, 15), shade(base, 0.7)); img.putpixel((15, i), shade(base, 0.7))
    for i in range(3): img.putpixel((rnd.randrange(16), rnd.randrange(16)), shade(accent, 0.6))
    img.save(f'{OUT}/gui/advancements/backgrounds/{name}.png')
print('backgrounds done')

# mob effect icons (18x18). A missing one shows as the purple-black "missing texture" in the HUD and inventory.
os.makedirs(f'{OUT}/mob_effect', exist_ok=True)
def effect_icon(name, draw):
    img = Image.new('RGBA', (18, 18), (0, 0, 0, 0)); d = ImageDraw.Draw(img); draw(d)
    save(outline(img, (20, 18, 28, 255)), f'mob_effect/{name}')
effect_icon('hidden', lambda d: (d.rectangle([4, 2, 13, 15], fill=RUST), [d.line([6, y, 11, y], fill=(14, 10, 8, 255)) for y in (5, 7, 9, 11)],
                                 d.point([(8, 8), (9, 8)], fill=HOLLOW_L)))
effect_icon('labyrinth', lambda d: (d.rectangle([2, 2, 15, 15], outline=HOLLOW, width=2), d.line([6, 2, 6, 11], fill=HOLLOW, width=2),
                                    d.line([10, 6, 10, 15], fill=HOLLOW, width=2), d.line([6, 11, 10, 11], fill=HOLLOW_L)))
