"""Generates every Stormreach texture as 16x16 (blocks/items) or 64x32 (mobs) pixel art."""
import os as _os, pathlib as _pl
ROOT = str(_pl.Path(__file__).resolve().parent.parent)  # the Stormreach project folder
VANILLA = _os.environ.get('STORMREACH_VANILLA', str(_pl.Path(ROOT) / '.vanilla'))  # extracted 26.3 jar (assets/ + data/)
MCJAR = _os.environ.get('STORMREACH_MCJAR', str(_pl.Path(ROOT) / '.vanilla' / 'minecraft-26.3.jar'))
import random, os, math
from PIL import Image

OUT = ROOT + '/src/main/resources/assets/stormreach/textures'
for d in ('block', 'item', 'entity'):
    os.makedirs(f'{OUT}/{d}', exist_ok=True)

def hexc(h, a=255):
    h = h.lstrip('#'); return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)

def jitter(c, amt, rnd):
    d = rnd.randint(-amt, amt)
    return tuple(max(0, min(255, v + d)) for v in c[:3]) + (c[3],)

def save(img, path):
    img.save(f'{OUT}/{path}.png')

def noise_tile(base, amt, seed, size=16):
    rnd = random.Random(seed); img = Image.new('RGBA', (size, size))
    for y in range(size):
        for x in range(size):
            img.putpixel((x, y), jitter(base, amt, rnd))
    return img, rnd

# ---------- palettes ----------
STONE = hexc('#4f5a69'); STONE_L = hexc('#6d7a8c'); STONE_D = hexc('#353d49')
WOOD = hexc('#a49e90'); WOOD_D = hexc('#7d776a'); WOOD_L = hexc('#c2bcad')
CRYS = hexc('#6fdcff'); CRYS_L = hexc('#d2f7ff'); CRYS_D = hexc('#2f9fd0')

def stormstone(seed=1):
    img, rnd = noise_tile(STONE, 8, seed)
    # pale wind-carved streaks running diagonally
    for i in range(5):
        x, y = rnd.randrange(16), rnd.randrange(16)
        for k in range(rnd.randint(3, 6)):
            img.putpixel(((x + k) % 16, (y + k // 2) % 16), jitter(STONE_L, 6, rnd))
    for i in range(4):
        x, y = rnd.randrange(16), rnd.randrange(16)
        for k in range(rnd.randint(2, 4)):
            img.putpixel(((x + k) % 16, (y - k) % 16), jitter(STONE_D, 4, rnd))
    return img

save(stormstone(), 'block/stormstone')

img, rnd = noise_tile(STONE, 4, 2)
for i in range(16):
    for (x, y) in ((i, 0), (0, i)): img.putpixel((x, y), jitter(STONE_L, 4, rnd))
    for (x, y) in ((i, 15), (15, i)): img.putpixel((x, y), jitter(STONE_D, 4, rnd))
for y in range(3, 13):
    for x in range(3, 13):
        if x in (3, 12) or y in (3, 12): img.putpixel((x, y), jitter(STONE_D, 3, rnd))
save(img, 'block/polished_stormstone')

img, rnd = noise_tile(STONE, 6, 3)
for y in range(16):
    for x in range(16):
        row = y // 4; off = 4 if row % 2 else 0
        if y % 4 == 3 or (x + off) % 8 == 7:
            img.putpixel((x, y), jitter(STONE_D, 4, rnd))
        elif y % 4 == 0:
            img.putpixel((x, y), jitter(STONE_L, 5, rnd))
save(img, 'block/stormstone_bricks')

# Driftwood: bleached, weathered grey wood
img, rnd = noise_tile(WOOD, 6, 4)
for x in range(16):
    if x % 4 in (1,) or rnd.random() < 0.15:
        for y in range(16):
            if rnd.random() < 0.8: img.putpixel((x, y), jitter(WOOD_D, 5, rnd))
for i in range(3):
    x, y = rnd.randrange(16), rnd.randrange(14)
    img.putpixel((x, y), hexc('#5e594f')); img.putpixel((x, y + 1), hexc('#6b665b'))
save(img, 'block/driftwood_log')

img, rnd = noise_tile(WOOD_L, 5, 5)
cx = cy = 7.5
for y in range(16):
    for x in range(16):
        r = math.hypot(x - cx, y - cy)
        if x in (0, 15) or y in (0, 15): img.putpixel((x, y), jitter(WOOD_D, 5, rnd))
        elif int(r) % 3 == 0: img.putpixel((x, y), jitter(WOOD, 5, rnd))
save(img, 'block/driftwood_log_top')

img, rnd = noise_tile(WOOD_L, 6, 6)
for y in range(16):
    for x in range(16):
        if y % 4 == 3: img.putpixel((x, y), jitter(WOOD_D, 4, rnd))
        elif rnd.random() < 0.12: img.putpixel((x, y), jitter(WOOD, 4, rnd))
    if y % 4 == 1:
        for x in ((y * 5) % 16,): img.putpixel((x, y), hexc('#6b665b'))
save(img, 'block/driftwood_planks')

# Stormglass: pale blue frame, mostly clear centre with wind streaks
img = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); rnd = random.Random(7)
for i in range(16):
    for (x, y) in ((i, 0), (i, 15), (0, i), (15, i)):
        img.putpixel((x, y), jitter(hexc('#9fd8ee', 230), 6, rnd))
for (x0, y0, n) in ((3, 4, 4), (6, 9, 5), (9, 3, 3), (10, 11, 3)):
    for k in range(n):
        img.putpixel((x0 + k, y0 - k // 2), hexc('#e4f8ff', 150))
for y in range(1, 15):
    for x in range(1, 15):
        if img.getpixel((x, y))[3] == 0: img.putpixel((x, y), hexc('#bfe9fa', 40))
save(img, 'block/stormglass')

# Storm crystal ore
img = stormstone(8); rnd = random.Random(8)
for (cx, cy) in ((4, 4), (11, 6), (6, 11), (12, 12)):
    for (dx, dy) in ((0, 0), (1, 0), (0, 1), (-1, 0), (0, -1), (1, -1)):
        x, y = cx + dx, cy + dy
        if 0 <= x < 16 and 0 <= y < 16:
            img.putpixel((x, y), CRYS_L if (dx, dy) == (0, -1) or (dx, dy) == (1, -1) else jitter(CRYS, 10, rnd))
    img.putpixel((cx + 1, cy + 1), CRYS_D)
save(img, 'block/storm_crystal_ore')

img, rnd = noise_tile(CRYS, 10, 9)
for y in range(16):
    for x in range(16):
        if (x + y) % 6 == 0: img.putpixel((x, y), jitter(CRYS_L, 6, rnd))
        elif (x - y) % 7 == 0: img.putpixel((x, y), jitter(CRYS_D, 6, rnd))
        if x in (0, 15) or y in (0, 15): img.putpixel((x, y), jitter(CRYS_D, 5, rnd))
save(img, 'block/storm_crystal_block')

# Storm lantern: glass frame around a bright crystal core
img = Image.new('RGBA', (16, 16)); rnd = random.Random(10)
for y in range(16):
    for x in range(16):
        edge = x in (0, 15) or y in (0, 15)
        r = math.hypot(x - 7.5, y - 7.5)
        if edge: c = jitter(STONE_D, 4, rnd)
        elif x in (1, 14) or y in (1, 14): c = jitter(hexc('#9fd8ee'), 5, rnd)
        elif r < 2.5: c = hexc('#ffffff')
        elif r < 4.5: c = jitter(CRYS_L, 6, rnd)
        else: c = jitter(CRYS, 12, rnd)
        img.putpixel((x, y), c)
save(img, 'block/storm_lantern')

# Storm barometer: polished stone with a dial face (clear / rain / storm)
def baro(kind):
    img = Image.open(f'{OUT}/block/polished_stormstone.png').copy(); rnd = random.Random(11)
    for y in range(3, 13):
        for x in range(3, 13):
            if math.hypot(x - 7.5, y - 7.5) <= 4.6: img.putpixel((x, y), hexc('#dfe6ee'))
    if kind == 'clear':
        for y in range(5, 11):
            for x in range(5, 11):
                if math.hypot(x - 7.5, y - 7.5) <= 2.3: img.putpixel((x, y), hexc('#ffd54a'))
    elif kind == 'rain':
        for (x, y) in ((5, 5), (5, 6), (8, 7), (8, 8), (10, 5), (10, 6), (6, 9), (6, 10)):
            img.putpixel((x, y), hexc('#3c78d8'))
    else:
        for (x, y) in ((9, 4), (8, 5), (8, 6), (7, 7), (8, 7), (9, 7), (8, 8), (7, 9), (6, 10)):
            img.putpixel((x, y), hexc('#7fe3ff'))
        for (x, y) in ((5, 5), (10, 10)): img.putpixel((x, y), hexc('#2b3340'))
    return img
for k in ('clear', 'rain', 'storm'):
    save(baro(k), f'block/storm_barometer_{k}')

# Sea thrift: pink pom-pom flowers on thin stems
img = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); rnd = random.Random(12)
STEM = hexc('#5f7d4a')
for (sx, top) in ((5, 5), (10, 3), (8, 7)):
    for y in range(top + 2, 16): img.putpixel((sx, y), jitter(STEM, 6, rnd))
    for dy in range(-1, 2):
        for dx in range(-1, 2):
            if abs(dx) + abs(dy) < 2 or rnd.random() < 0.5:
                img.putpixel((sx + dx, top + 1 + dy), jitter(hexc('#e88bb8'), 14, rnd))
    img.putpixel((sx, top), hexc('#f7c6de'))
for x in (3, 4, 11, 12, 7):
    img.putpixel((x, 15), jitter(STEM, 6, rnd)); img.putpixel((x, 14), jitter(hexc('#6f8f57'), 6, rnd))
save(img, 'block/sea_thrift')

# Stormberry shrub, four ripeness stages
def shrub(stage):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); rnd = random.Random(13)
    LEAF = hexc('#55705d'); LEAF_D = hexc('#3e5546')
    h = [6, 8, 9, 10][stage]
    for y in range(16 - h, 16):
        w = int((y - (16 - h)) / h * 6) + 2
        for x in range(8 - w, 8 + w):
            if rnd.random() < 0.7: img.putpixel((x, y), jitter(LEAF if rnd.random() < 0.6 else LEAF_D, 8, rnd))
    berries = [0, 3, 6, 10][stage]
    for i in range(berries):
        x = rnd.randint(3, 12); y = rnd.randint(16 - h + 1, 14)
        col = hexc('#6a3fa0') if stage < 3 else hexc('#8b5cf6')
        img.putpixel((x, y), col); img.putpixel((x, y - 1) if y > 0 else (x, y), hexc('#c4b5fd'))
    return img
for s in range(4):
    save(shrub(s), f'block/stormberry_shrub_stage{s}')

# ---------- items (ASCII art) ----------
def art(rows, palette, name):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != '.': img.putpixel((x, y), hexc(palette[ch]))
    save(img, f'item/{name}')

CP = {'L': '#d2f7ff', 'C': '#6fdcff', 'D': '#2f9fd0', 'K': '#1d5f86'}
art(['................',
     '..........L.....',
     '.........LC.....',
     '........LCCD....',
     '.......LCCD.....',
     '......LCCCD.....',
     '.....LCCCD......',
     '....LCCCD.......',
     '....CCCDD.......',
     '...LCCDD........',
     '...CCDD.........',
     '...CDD..........',
     '..CDK...........',
     '..DK............',
     '................',
     '................'], CP, 'storm_shard')

art(['................',
     '...........WW...',
     '..........WWG...',
     '.........WWGW...',
     '........WWGW....',
     '.......WWGWW....',
     '......WWGWW.....',
     '.....WWGWWS.....',
     '....WWGWWS......',
     '....WGWWS.......',
     '...WGWSS........',
     '...GWSS.........',
     '..GSS...........',
     '..Q.............',
     '.Q..............',
     '................'], {'W': '#f2f4f7', 'G': '#9aa3ad', 'S': '#6c7682', 'Q': '#c9b38a'}, 'gull_feather')

claw = ['................',
        '................',
        '...RR...........',
        '..RHRR..........',
        '..RHHRR.........',
        '...RRHR.........',
        '.....RR..RR.....',
        '.....RRRRHRR....',
        '.....RHHHHHRR...',
        '......RRHHHRRR..',
        '.......RRRRHRR..',
        '.........RRRRD..',
        '..........RRDD..',
        '...........DD...',
        '................',
        '................']
art(claw, {'R': '#d4553b', 'H': '#f28b6b', 'D': '#8f2f1f'}, 'crab_claw')
art(claw, {'R': '#b8452a', 'H': '#f0a060', 'D': '#6e2414'}, 'cooked_crab_claw')

art(['................',
     '.......G........',
     '......GG........',
     '.....G.G........',
     '....PPLPP.......',
     '...PLPPPLP......',
     '...PPPDPPP......',
     '..PLPPPLPPP.....',
     '..PPPDPPPDP.....',
     '...PPPLPPP......',
     '....PDPPP.......',
     '.....PPP........',
     '................',
     '................',
     '................',
     '................'], {'G': '#5f7d4a', 'P': '#7c4dcc', 'L': '#c4b5fd', 'D': '#4c2a85'}, 'stormberries')

art(['................',
     '................',
     '................',
     '................',
     '.....CCCCCC.....',
     '...CCPCCPCCCC...',
     '..CPPPPPPPPPPC..',
     '.CCPLPPLPPLPPCC.',
     '.CPPPPPPPPPPPPC.',
     '.BCCCCCCCCCCCCB.',
     '.BBBBBBBBBBBBBB.',
     '..BDDDDDDDDDDB..',
     '...BBBBBBBBBB...',
     '................',
     '................',
     '................'], {'C': '#e8c27a', 'P': '#7c4dcc', 'L': '#c4b5fd', 'B': '#c98f45', 'D': '#a06a2c'}, 'stormberry_pie')

art(['................',
     '................',
     '................',
     '................',
     '................',
     '...WWWWWWWWWW...',
     '..WSSOSSSSOSSW..',
     '..BSSSSGSSSSSB..',
     '..BBSOSSSSSSBB..',
     '...BBBBBBBBBB...',
     '...BDBBBBBBDB...',
     '....BBBBBBBB....',
     '.....BBBBBB.....',
     '................',
     '................',
     '................'], {'W': '#d8cbb0', 'S': '#efe2c4', 'O': '#e07a4f', 'G': '#6f8f57', 'B': '#8b5a2b', 'D': '#6b4220'}, 'storm_chowder')

art(['................',
     '......CC........',
     '......KK........',
     '......GG........',
     '.....G..G.......',
     '.....G..G.......',
     '....G....G......',
     '...GTTTTTTG.....',
     '...GTPTTTTG.....',
     '...GTTTTPTG.....',
     '...GTTPTTTG.....',
     '...GTTTTTTG.....',
     '....GGGGGG......',
     '................',
     '................',
     '................'], {'C': '#8b5a2b', 'K': '#6b4220', 'G': '#cfe8f2', 'T': '#e8a0c4', 'P': '#f7c6de'}, 'thrift_tea')

art(['................',
     '...........LC...',
     '..........LCCD..',
     '..........CCD...',
     '.........WDD....',
     '........WW......',
     '.......WD.......',
     '......WD........',
     '.....WD.........',
     '....WD..........',
     '...WD...........',
     '..WD............',
     '.WD.............',
     '.D..............',
     '................',
     '................'], {'L': '#d2f7ff', 'C': '#6fdcff', 'D': '#7d776a', 'W': '#c2bcad'}, 'tempest_staff')

art(['................',
     '................',
     '.............GG.',
     '............GGD.',
     '...........GBD..',
     '..........GGD...',
     '.........GBGD...',
     '.......GGGGD....',
     '.....GGBGGD.....',
     '...GGGGGGD......',
     '..GLLGGGD.......',
     '..LLLGGD........',
     '..LLLGD.........',
     '...LDD..........',
     '................',
     '................'], {'G': '#8d96a3', 'D': '#58606c', 'B': '#6fdcff', 'L': '#d8dde4'}, 'thunderhead_horn')

TOOL = {'C': '#6fdcff', 'L': '#d2f7ff', 'D': '#2f9fd0', 'W': '#c2bcad', 'S': '#7d776a'}
art(['................',
     '.............LL.',
     '............LCL.',
     '...........LCD..',
     '..........LCD...',
     '.........LCD....',
     '........LCD.....',
     '.......LCD......',
     '..D...LCD.......',
     '..DD.LCD........',
     '...DDCD.........',
     '....SD..........',
     '...WSDD.........',
     '..WS..D.........',
     '.WS.............',
     '................'], TOOL, 'stormshard_sword')
art(['................',
     '....LCCCCCD.....',
     '...LCDDDDDCD....',
     '..LCD.....DCD...',
     '..CD.....WS.CD..',
     '..D.....WS...D..',
     '.......WS.......',
     '......WS........',
     '.....WS.........',
     '....WS..........',
     '...WS...........',
     '..WS............',
     '.WS.............',
     '................',
     '................',
     '................'], TOOL, 'stormshard_pickaxe')
art(['................',
     '.......LCC......',
     '......LCCCD.....',
     '.....LCCCD......',
     '.....CCCWS......',
     '......DWSD......',
     '......WS........',
     '.....WS.........',
     '....WS..........',
     '...WS...........',
     '..WS............',
     '.WS.............',
     '................',
     '................',
     '................',
     '................'], TOOL, 'stormshard_axe')
art(['................',
     '...........LC...',
     '..........LCCD..',
     '.........LCCD...',
     '.........CCD....',
     '........WSD.....',
     '.......WS.......',
     '......WS........',
     '.....WS.........',
     '....WS..........',
     '...WS...........',
     '..WS............',
     '.WS.............',
     '................',
     '................',
     '................'], TOOL, 'stormshard_shovel')

def egg(base, spot, name, seed):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); rnd = random.Random(seed)
    for y in range(16):
        for x in range(16):
            dx = (x - 7.5) / 5.2; dy = (y - 8.5) / (6.8 if y > 8 else 7.4)
            if dx * dx + dy * dy <= 1.0:
                c = hexc(base)
                if rnd.random() < 0.18: c = hexc(spot)
                if dx * dx + dy * dy > 0.75: c = tuple(int(v * 0.75) for v in c[:3]) + (255,)
                img.putpixel((x, y), c)
    img.putpixel((5, 4), (255, 255, 255, 255)); img.putpixel((5, 5), (240, 240, 240, 255))
    save(img, f'item/{name}')
egg('#d4553b', '#f2c14e', 'cliff_crab_spawn_egg', 20)
egg('#eef1f4', '#8d96a3', 'storm_gull_spawn_egg', 21)
egg('#4a5563', '#6fdcff', 'squall_spawn_egg', 22)

# ---------- mobs: fill each cube's UV area ----------
def uv_fill(img, u, v, w, h, d, base, rnd, front=None, top=None, amt=10):
    for yy in range(v, v + d + h):
        for xx in range(u, u + 2 * (d + w)):
            if (yy < v + d) and not (u + d <= xx < u + d + 2 * w): continue
            c = base
            if top is not None and yy < v + d: c = top
            img.putpixel((xx, yy), jitter(c, amt, rnd))
    if front:
        front(img, u + d, v + d, w, h)

rnd = random.Random(30); crab = Image.new('RGBA', (64, 32), (0, 0, 0, 0))
SHELL = hexc('#c8482e'); SHELL_T = hexc('#de6a3f'); LEG = hexc('#a83a24')
uv_fill(crab, 0, 0, 8, 3, 6, SHELL, rnd, top=SHELL_T)
for (x, y) in ((8, 2), (9, 3), (11, 2), (12, 4)): crab.putpixel((x, y), hexc('#f2a07a'))  # shell spots
uv_fill(crab, 0, 10, 2, 2, 4, hexc('#e0603f'), rnd)
uv_fill(crab, 12, 10, 2, 2, 4, hexc('#e0603f'), rnd)
for i in range(3):
    uv_fill(crab, 30, i * 2, 4, 1, 1, LEG, rnd, amt=6)
    uv_fill(crab, 40, i * 2, 4, 1, 1, LEG, rnd, amt=6)
for u in (30, 34):
    uv_fill(crab, u, 8, 1, 2, 1, hexc('#d8c0a0'), rnd, amt=4)
    crab.putpixel((u + 1, 8), hexc('#111111'))
crab.save(f'{OUT}/entity/cliff_crab.png')

rnd = random.Random(31); gull = Image.new('RGBA', (64, 32), (0, 0, 0, 0))
WHITE = hexc('#eef1f4'); GREY = hexc('#8d96a3'); YEL = hexc('#f2c14e')
uv_fill(gull, 0, 0, 4, 4, 7, WHITE, rnd, top=GREY, amt=6)
def gull_face(img, x, y, w, h):
    img.putpixel((x, y + 1), (20, 20, 20, 255)); img.putpixel((x + w - 1, y + 1), (20, 20, 20, 255))
uv_fill(gull, 24, 0, 3, 3, 3, WHITE, rnd, amt=5)
# eyes on the head's sides
gull.putpixel((25, 4), (20, 20, 20, 255)); gull.putpixel((31, 4), (20, 20, 20, 255))
uv_fill(gull, 36, 0, 1, 1, 2, YEL, rnd, amt=4)
gull.putpixel((38, 2), hexc('#d9452b'))
uv_fill(gull, 24, 8, 3, 1, 3, GREY, rnd, amt=6)
for v in (12, 18):
    uv_fill(gull, 0, v, 6, 1, 5, GREY, rnd, top=hexc('#a7b0ba'), amt=6)
    for xx in range(0, 22):
        if gull.getpixel((xx, v + 5))[3] or True:
            pass
    for xx in (5, 10, 16, 21):
        for yy in range(v, v + 6):
            if gull.getpixel((xx, yy))[3]: gull.putpixel((xx, yy), hexc('#3a3f47'))  # black wing tips
uv_fill(gull, 40, 8, 1, 4, 1, YEL, rnd, amt=4)
uv_fill(gull, 44, 8, 1, 4, 1, YEL, rnd, amt=4)
gull.save(f'{OUT}/entity/storm_gull.png')

rnd = random.Random(32); sq = Image.new('RGBA', (64, 32), (0, 0, 0, 0))
def cloud(img, u, v, w, h, d, alpha):
    for yy in range(v, v + d + h):
        for xx in range(u, u + 2 * (d + w)):
            if (yy < v + d) and not (u + d <= xx < u + d + 2 * w): continue
            g = rnd.randint(70, 130)
            c = (g, g + 10, g + 25, alpha + rnd.randint(-30, 20))
            if rnd.random() < 0.06: c = (130, 225, 255, 230)
            img.putpixel((xx, yy), c)
cloud(sq, 0, 0, 6, 6, 6, 200)
def eyes(img, x, y, w, h):
    for (ex, ey) in ((x + 1, y + 2), (x + w - 2, y + 2)):
        img.putpixel((ex, ey), (200, 245, 255, 255))
eyes(sq, 6, 6, 6, 6)
cloud(sq, 0, 12, 10, 2, 10, 150)
cloud(sq, 24, 0, 4, 5, 4, 170)
cloud(sq, 40, 0, 2, 4, 2, 130)
sq.save(f'{OUT}/entity/squall.png')

# Mod icon (128x128): stormy sky, lightning bolt, sea
icon = Image.new('RGBA', (128, 128)); rnd = random.Random(40)
for y in range(128):
    for x in range(128):
        if y < 80:
            t = y / 80; c = (int(40 + 30 * t), int(48 + 40 * t), int(64 + 50 * t), 255)
        else:
            t = (y - 80) / 48; c = (int(30 + 10 * t), int(70 + 20 * t), int(110 + 20 * t), 255)
        icon.putpixel((x, y), jitter(c, 6, rnd))
for (cx, cy, r) in ((30, 28, 22), (60, 20, 26), (95, 30, 22), (75, 38, 18)):
    for y in range(max(0, cy - r), cy + r):
        for x in range(max(0, cx - r), min(128, cx + r)):
            if (x - cx) ** 2 + ((y - cy) * 1.6) ** 2 < r * r:
                icon.putpixel((x, y), jitter((88, 96, 112, 255), 8, rnd))
bolt = [(64, 40), (56, 62), (66, 62), (54, 92), (76, 58), (66, 58), (74, 40)]
from PIL import ImageDraw
dr = ImageDraw.Draw(icon)
dr.polygon(bolt, fill=(210, 247, 255, 255), outline=(111, 220, 255, 255))
for i in range(0, 128, 9):
    dr.line([(i, 96 + (i % 18) // 3), (i + 6, 94 + (i % 18) // 3)], fill=(220, 235, 245, 255))
icon = icon.resize((128, 128))
icon.save(ROOT + '/src/main/resources/assets/stormreach/icon.png')
print('textures done')
