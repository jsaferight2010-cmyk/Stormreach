"""Stormreach 4.0 textures: Tempest Spire, Stormgate, Tempest Realm blocks, items, mobs and armor."""
import os as _os, pathlib as _pl
ROOT = str(_pl.Path(__file__).resolve().parent.parent)  # the Stormreach project folder
VANILLA = _os.environ.get('STORMREACH_VANILLA', str(_pl.Path(ROOT) / '.vanilla'))  # extracted 26.3 jar (assets/ + data/)
MCJAR = _os.environ.get('STORMREACH_MCJAR', str(_pl.Path(ROOT) / '.vanilla' / 'minecraft-26.3.jar'))
import random, os, math
from PIL import Image, ImageDraw
OUT = (ROOT if 'ROOT' in globals() else '/home/claude/stormreach') + '/src/main/resources/assets/stormreach/textures'
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
def mix(a, b, t): return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3)) + (255,)

# ---------------- palettes ----------------
TEMPEST = hexc('#3d3358'); TEMPEST_D = hexc('#29223d'); TEMPEST_L = hexc('#5b4d80'); GLOW = hexc('#c79bff'); GLOW_L = hexc('#f1e2ff')
SHALE = hexc('#a9b4c4'); SHALE_D = hexc('#7f8a9b'); SHALE_L = hexc('#cfd7e2')
VOLT = hexc('#f2c94c'); VOLT_D = hexc('#8a6a1e'); VOLT_L = hexc('#fff3b0')
AUR = hexc('#d66be8'); AUR_D = hexc('#7a2a8e'); AUR_L = hexc('#ffd6ff')
ION = hexc('#62f0e0'); ION_L = hexc('#e2fffb'); ION_D = hexc('#1f9c90')
GALE = hexc('#d9dde6'); GALE_D = hexc('#a3aab8'); GALE_L = hexc('#f4f6fa')
PLANK = hexc('#b9c3cf'); PLANK_D = hexc('#8b95a3'); PLANK_L = hexc('#d4dce6')
LEAF = hexc('#7fc8d8'); LEAF_D = hexc('#4d97ab')
GRASS = hexc('#4fb8c4'); GRASS_D = hexc('#2f8794'); SOIL = hexc('#6c6f8f'); SOIL_D = hexc('#4f5270')

def bricks(base, dark, light, seed, mortar=None):
    img, rnd = noise(base, 7, seed)
    mortar = mortar or dark
    for y in range(16):
        for x in range(16):
            row = y // 4
            off = 4 if row % 2 else 0
            if y % 4 == 3 or (x + off) % 8 == 7: img.putpixel((x, y), jitter(mortar, 4, rnd))
            elif y % 4 == 0 and rnd.random() < 0.5: img.putpixel((x, y), jitter(light, 5, rnd))
    return img, rnd

# ---------------- Tempest Spire ----------------
img, rnd = bricks(TEMPEST, TEMPEST_D, TEMPEST_L, 400)
save(img, 'block/tempest_bricks')
for (x, y) in ((2, 1), (3, 2), (4, 2), (5, 3), (10, 8), (11, 9), (11, 10), (12, 11), (6, 13), (7, 14)):
    img.putpixel((x, y), hexc('#17131f'))
save(img, 'block/cracked_tempest_bricks')
img, rnd = noise(TEMPEST, 6, 401); d = ImageDraw.Draw(img)
d.rectangle([0, 0, 15, 15], outline=TEMPEST_D); d.rectangle([2, 2, 13, 13], outline=TEMPEST_L)
for (x, y) in ((8, 3), (7, 4), (7, 5), (6, 6), (7, 7), (8, 7), (9, 7), (8, 8), (8, 9), (7, 10), (7, 11), (6, 12)):
    img.putpixel((x, y), GLOW if (x + y) % 2 else GLOW_L)
save(img, 'block/chiseled_tempest_bricks')
# altar: purple stone with a glowing eye-socket on top
img, rnd = noise(TEMPEST_D, 6, 402); d = ImageDraw.Draw(img)
d.rectangle([0, 0, 15, 15], outline=TEMPEST_L)
for y in range(16):
    for x in range(16):
        r = math.hypot(x - 7.5, y - 7.5)
        if r < 6 and int(r * 2) % 3 == 0: img.putpixel((x, y), GLOW)
        if r < 2.2: img.putpixel((x, y), GLOW_L)
save(img, 'block/sovereign_altar_top')
img, rnd = bricks(TEMPEST_D, hexc('#1b1629'), TEMPEST, 403)
for x in range(2, 14): img.putpixel((x, 1), GLOW); img.putpixel((x, 2), TEMPEST_L)
for (x, y) in ((7, 6), (8, 6), (6, 7), (9, 7), (7, 8), (8, 8)): img.putpixel((x, y), GLOW_L)
save(img, 'block/sovereign_altar_side')
img, rnd = noise(TEMPEST_D, 5, 404); save(img, 'block/sovereign_altar_bottom')

# ---------------- Stormgate ----------------
img, rnd = noise(hexc('#24425a'), 6, 410); d = ImageDraw.Draw(img)
d.rectangle([0, 0, 15, 15], outline=hexc('#6fdcff')); d.rectangle([1, 1, 14, 14], outline=hexc('#183044'))
for y in range(4, 12):
    for x in range(4, 12): img.putpixel((x, y), jitter(hexc('#0e1a26'), 3, rnd))
save(img, 'block/stormgate_frame_top')
for y in range(4, 12):
    for x in range(4, 12):
        r = math.hypot(x - 7.5, y - 7.5)
        img.putpixel((x, y), GLOW_L if r < 1.3 else GLOW if r < 2.6 else hexc('#7b4fc9') if r < 3.8 else img.getpixel((x, y)))
save(img, 'block/stormgate_frame_top_eye')
img, rnd = bricks(hexc('#24425a'), hexc('#142838'), hexc('#3b6582'), 411)
for x in range(16): img.putpixel((x, 0), hexc('#6fdcff'))
for (x, y) in ((4, 7), (5, 6), (6, 7), (7, 6), (8, 7), (9, 6), (10, 7), (11, 6)): img.putpixel((x, y), hexc('#6fdcff'))
save(img, 'block/stormgate_frame_side')
img, rnd = noise(hexc('#183044'), 5, 412); save(img, 'block/stormgate_frame_bottom')
# animated, translucent portal surface
frames = 16; port = Image.new('RGBA', (16, 16 * frames)); rnd = random.Random(413)
for f in range(frames):
    t = f / frames * 2 * math.pi
    for y in range(16):
        for x in range(16):
            v = (math.sin(x * 0.7 + t) + math.sin(y * 0.6 - t * 1.3) + math.sin((x + y) * 0.4 + t * 0.7)) / 3
            v = (v + 1) / 2
            c = mix(hexc('#2a1f5c'), hexc('#7fe6ff'), v ** 1.6)
            if rnd.random() < 0.02: c = GLOW_L
            port.putpixel((x, y + f * 16), c[:3] + (200,))
port.save(f'{OUT}/block/stormgate.png')
open(f'{OUT}/block/stormgate.png.mcmeta', 'w').write('{"animation": {"frametime": 2}}\n')

# ---------------- Skyshale ----------------
img, rnd = noise(SHALE, 8, 420)
for i in range(14):
    x, y = rnd.randrange(16), rnd.randrange(16)
    for k in range(rnd.randint(2, 5)): img.putpixel(((x + k) % 16, y), jitter(SHALE_D, 4, rnd))
for i in range(5): img.putpixel((rnd.randrange(16), rnd.randrange(16)), SHALE_L)
save(img, 'block/skyshale'); shale = img
img, rnd = noise(SHALE, 10, 421)
for cy in range(0, 16, 5):
    for cx in range((cy * 3) % 6, 16, 6):
        for y in range(cy, min(16, cy + 4)):
            for x in range(cx, min(16, cx + 5)):
                if x in (cx, cx + 4) or y in (cy, cy + 3): img.putpixel((x, y), jitter(SHALE_D, 5, rnd))
save(img, 'block/cobbled_skyshale')
img, rnd = noise(SHALE_L, 5, 422); d = ImageDraw.Draw(img); d.rectangle([0, 0, 15, 15], outline=SHALE_D)
save(img, 'block/polished_skyshale')
img, rnd = bricks(SHALE, SHALE_D, SHALE_L, 423); save(img, 'block/skyshale_bricks')
img = load('block/polished_skyshale'); d = ImageDraw.Draw(img); d.rectangle([3, 3, 12, 12], outline=SHALE_D)
for (x, y) in ((7, 5), (8, 6), (7, 7), (8, 8), (7, 9), (8, 10)): img.putpixel((x, y), ION)
save(img, 'block/chiseled_skyshale')
def ore(base_img, color, light, dark, seed, name, n=6):
    img = base_img.copy(); rnd = random.Random(seed)
    for i in range(n):
        cx, cy = rnd.randint(2, 13), rnd.randint(2, 13)
        for (dx, dy) in ((0, 0), (1, 0), (0, 1), (-1, 0), (1, 1)):
            if rnd.random() < 0.85: img.putpixel((cx + dx, cy + dy), dark if (dx + dy) % 2 else color)
        img.putpixel((cx, cy), light)
    save(img, f'block/{name}')
ore(shale, VOLT, VOLT_L, VOLT_D, 424, 'voltite_ore')
ore(shale, AUR, AUR_L, AUR_D, 425, 'aurorite_ore', 4)
ore(shale, ION, ION_L, ION_D, 426, 'ion_ore', 7)
img, rnd = noise(VOLT, 10, 427); d = ImageDraw.Draw(img); d.rectangle([0, 0, 15, 15], outline=VOLT_D); d.rectangle([1, 1, 14, 14], outline=VOLT_L)
for (x, y) in ((9, 3), (8, 4), (7, 5), (6, 6), (7, 7), (8, 8), (9, 8), (8, 9), (7, 10), (6, 11)): img.putpixel((x, y), VOLT_D)
save(img, 'block/voltite_block')
img, rnd = noise(AUR, 12, 428)
for y in range(16):
    for x in range(16):
        if (x + y) % 5 == 0: img.putpixel((x, y), jitter(AUR_L, 8, rnd))
        elif (x - y) % 7 == 0: img.putpixel((x, y), jitter(AUR_D, 8, rnd))
save(img, 'block/aurorite_block')
img, rnd = noise(ION_D, 6, 429)
for y in range(2, 14):
    for x in range(2, 14):
        r = math.hypot(x - 7.5, y - 7.5)
        img.putpixel((x, y), ION_L if r < 2 else ION if r < 4.5 else jitter(ION_D, 6, rnd))
d = ImageDraw.Draw(img); d.rectangle([0, 0, 15, 15], outline=hexc('#355a66'))
save(img, 'block/ion_lamp')

# ---------------- soil, grass, clouds ----------------
img, rnd = noise(SOIL, 9, 430)
for i in range(10): img.putpixel((rnd.randrange(16), rnd.randrange(16)), SOIL_D)
for i in range(4): img.putpixel((rnd.randrange(16), rnd.randrange(16)), ION)
save(img, 'block/skysoil'); soil = img
img, rnd = noise(GRASS, 9, 431)
for i in range(12): img.putpixel((rnd.randrange(16), rnd.randrange(16)), GRASS_D)
for i in range(4): img.putpixel((rnd.randrange(16), rnd.randrange(16)), hexc('#bff7ff'))
save(img, 'block/static_grass_top')
img = soil.copy(); rnd = random.Random(432)
for x in range(16):
    depth = 3 + (1 if rnd.random() < 0.5 else 0) + (1 if rnd.random() < 0.2 else 0)
    for y in range(depth): img.putpixel((x, y), jitter(GRASS if y < depth - 1 else GRASS_D, 8, rnd))
save(img, 'block/static_grass_side')
def cloudtex(base, edge, seed, name, alpha, spark=None):
    img = Image.new('RGBA', (16, 16)); rnd = random.Random(seed)
    for y in range(16):
        for x in range(16):
            v = (math.sin(x * 0.9 + seed) + math.cos(y * 0.8 + seed * 0.3) + rnd.random()) / 3
            c = mix(edge, base, (v + 1) / 2)
            img.putpixel((x, y), c[:3] + (alpha,))
    if spark:
        for i in range(5): img.putpixel((rnd.randrange(16), rnd.randrange(16)), spark[:3] + (255,))
    save(img, f'block/{name}')
cloudtex(hexc('#ffffff'), hexc('#d6dde8'), 440, 'nimbus_cloud', 210)
cloudtex(hexc('#4a5a8c'), hexc('#26304f'), 441, 'thundercloud', 225, ION_L)
cloudtex(hexc('#dfe8ee'), hexc('#9fb3c2'), 442, 'gale_cloud', 170)

# ---------------- Galewood ----------------
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
bark = log_side(GALE, GALE_D, 450)
rnd = random.Random(451)
for i in range(6):  # wind-streaks of storm-blue in the bark
    x, y = rnd.randrange(16), rnd.randrange(16)
    for k in range(3): bark.putpixel((x, (y + k) % 16), hexc('#8fc4d8'))
save(bark, 'block/galewood_log')
save(log_top(GALE, PLANK_L, PLANK_D, 452), 'block/galewood_log_top')
save(log_side(PLANK_L, PLANK, 453), 'block/stripped_galewood_log')
save(log_top(PLANK_L, PLANK_L, PLANK, 454), 'block/stripped_galewood_log_top')
save(planks(PLANK, PLANK_D, PLANK_L, 455), 'block/galewood_planks')
img = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); rnd = random.Random(456)
for y in range(16):
    for x in range(16):
        if rnd.random() < 0.75: img.putpixel((x, y), jitter(LEAF if rnd.random() < 0.6 else LEAF_D, 10, rnd))
        if rnd.random() < 0.03: img.putpixel((x, y), hexc('#fff0a8'))
save(img, 'block/galewood_leaves')
img = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); rnd = random.Random(457)
for y in range(9, 16): img.putpixel((8, y), GALE_D)
for (cx, cy, r) in ((8, 6, 3.5), (5, 9, 2), (11, 9, 2)):
    for y in range(16):
        for x in range(16):
            if math.hypot(x - cx, y - cy) < r: img.putpixel((x, y), jitter(LEAF, 12, rnd))
save(img, 'block/galewood_sapling')
def door(base, dark, frame, seed, name):
    for half in ('top', 'bottom'):
        img, rnd = noise(base, 6, seed + (half == 'top'))
        for y in range(16):
            for x in range(16):
                if x in (0, 15) or y == (0 if half == 'top' else 15) or y == (15 if half == 'top' else 0):
                    img.putpixel((x, y), jitter(frame, 4, rnd))
                elif x in (5, 10): img.putpixel((x, y), jitter(dark, 4, rnd))
        if half == 'top':
            for y in range(3, 9):
                for x in range(3, 13):
                    img.putpixel((x, y), (0, 0, 0, 0) if (x not in (7, 8) and y != 6) else jitter(frame, 3, rnd))
        else:
            img.putpixel((12, 1), VOLT); img.putpixel((12, 2), VOLT)
        save(img, f'block/{name}_door_{half}')
    icon = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    icon.paste(load(f'block/{name}_door_top').resize((8, 8), Image.NEAREST), (4, 0))
    icon.paste(load(f'block/{name}_door_bottom').resize((8, 8), Image.NEAREST), (4, 8))
    save(icon, f'item/{name}_door')
    img, rnd = noise(base, 6, seed + 7)
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15): img.putpixel((x, y), jitter(frame, 4, rnd))
            elif 4 <= x <= 11 and 4 <= y <= 11 and not (x in (7, 8) or y in (7, 8)): img.putpixel((x, y), (0, 0, 0, 0))
            elif y % 5 == 0: img.putpixel((x, y), jitter(dark, 4, rnd))
    save(img, f'block/{name}_trapdoor')
door(PLANK, PLANK_D, GALE_D, 460, 'galewood')

# ---------------- plants ----------------
def flower(name, stem, petal, centre, seed):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); rnd = random.Random(seed)
    for y in range(7, 16): img.putpixel((8, y), jitter(stem, 6, rnd))
    img.putpixel((7, 12), stem); img.putpixel((6, 11), stem); img.putpixel((9, 13), stem); img.putpixel((10, 12), stem)
    for (dx, dy) in ((0, -2), (-2, 0), (2, 0), (0, 2), (-1, -1), (1, -1), (-1, 1), (1, 1)):
        img.putpixel((8 + dx, 5 + dy), jitter(petal, 10, rnd))
    img.putpixel((8, 5), centre)
    save(img, f'block/{name}')
flower('skybloom', hexc('#3f8f9a'), hexc('#9fd8ff'), hexc('#ffffff'), 470)
img = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); rnd = random.Random(471)
for (cx, top, w) in ((5, 7, 3), (11, 9, 2)):
    for y in range(top + 2, 16): img.putpixel((cx, y), hexc('#d8d0e8'))
    for x in range(cx - w, cx + w + 1):
        img.putpixel((x, top), jitter(hexc('#8a4fd8'), 10, rnd)); img.putpixel((x, top + 1), jitter(hexc('#5b2f99'), 10, rnd))
    img.putpixel((cx - 1, top), GLOW_L); img.putpixel((cx + 1, top - 0), ION_L)
save(img, 'block/stormcap')
img = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); rnd = random.Random(472)
for i in range(10):
    x = rnd.randint(1, 14); h = rnd.randint(6, 14); lean = rnd.choice((-1, 1))
    for k in range(h):
        xx = x + (lean * (k // 5))
        img.putpixel((max(0, min(15, xx)), 15 - k), jitter(GRASS if k < h - 3 else hexc('#a8f0ff'), 8, rnd))
save(img, 'block/gale_grass')

# ---------------- items ----------------
def art(rows, palette, name):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != '.': img.putpixel((x, y), hexc(palette[ch]))
    save(img, f'item/{name}')
def recolor(src, name, dark, mid, light):
    img = load(f'item/{src}'); px = img.load()
    for y in range(16):
        for x in range(16):
            r, g, b, a = px[x, y]
            if a and b > r + 25:
                lum = (r + g + b) / 3
                c = light if lum > 200 else mid if lum > 110 else dark
                px[x, y] = c
    save(img, name)
for piece in ('sword', 'pickaxe', 'axe', 'shovel', 'helmet', 'chestplate', 'leggings', 'boots'):
    recolor(f'stormshard_{piece}', f'item/voltite_{piece}', VOLT_D, VOLT, VOLT_L)
    recolor(f'stormshard_{piece}', f'item/aurorite_{piece}', AUR_D, AUR, AUR_L)
hoe = ['................','................','...MMMMMM.......','..MLLLLLLM......','...MMMMHHM......','.......HH.......','......HH........','.....HH.........','....HH..........','...HH...........','..HH............','.HH.............','................','................','................','................']
art(hoe, {'M': '#8a6a1e', 'L': '#f2c94c', 'H': '#c2bcad'}, 'voltite_hoe')
art(hoe, {'M': '#7a2a8e', 'L': '#d66be8', 'H': '#c2bcad'}, 'aurorite_hoe')
recolor('stormshard_boots', 'item/cloudstep_boots', hexc('#9fb3c2'), hexc('#e6edf3'), hexc('#ffffff'))
art(['................','.......GG.......','......GWWG......','.....GWPPWG.....','....GWPLLPWG....','...GWPLEELPWG...','...GWPLEELPWG...','....GWPLLPWG....','.....GWPPWG.....','......GWWG......','.......GG.......','.......KK.......','......KKKK......','.....KK..KK.....','................','................'],
    {'G': '#3d3358', 'W': '#c79bff', 'P': '#7b4fc9', 'L': '#f1e2ff', 'E': '#6fdcff', 'K': '#8a6a1e'}, 'tempest_sigil')
art(['................','..OOOOOOOOOOOO..','..OPPPPPPPPPPO..','..OPWWWWWWWWPO..','..OPWCCCCCCWPO..','..OPWCKKKKCWPO..','..OPWCKLLKCWPO..','..OPWCKLLKCWPO..','..OPWCKKKKCWPO..','..OPWCCCCCCWPO..','..OPWWWWWWWWPO..','..OPPPPPPPPPPO..','..OOOOGGGGOOOO..','.....GYYYYG.....','......GGGG......','................'],
    {'O': '#29223d', 'P': '#5a3d94', 'W': '#c79bff', 'C': '#6fdcff', 'K': '#1b2a3c', 'L': '#e9fbff', 'G': '#8a6a1e', 'Y': '#f2c94c'}, 'realm_pass')
art(['................','................','.....OOOOOO.....','....OPPPPPPO....','...OPPWWWWPPO...','..OPPWCCCCWPPO..','..OPWCCKKCCWPO..','..OPWCKKKKCWPO..','..OPWCKKKKCWPO..','..OPWCCKKCCWPO..','..OPPWCCCCWPPO..','...OPPWWWWPPO...','....OPPPPPPO....','.....OOOOOO.....','................','................'],
    {'O': '#29223d', 'P': '#7b4fc9', 'W': '#f1e2ff', 'C': '#6fdcff', 'K': '#0e1a26'}, 'tempest_eye')
art(['................','................','.....DDD........','....DVVVD.......','...DVLVVVD......','...DVVVVVVD.....','..DVVVLVVVD.....','..DVVVVVVD......','...DVVVVD.......','....DDDD........','................','................','................','................','................','................'],
    {'D': '#8a6a1e', 'V': '#c9a33a', 'L': '#fff3b0'}, 'raw_voltite')
art(['................','................','................','................','.....DDDDDDD....','....DVVVVVVLD...','...DVVVVVVLLD...','..DVVVVVVVVD....','..DDDDDDDDD.....','................','................','................','................','................','................','................'],
    {'D': '#8a6a1e', 'V': '#f2c94c', 'L': '#fff3b0'}, 'voltite_ingot')
art(['................','.......D........','......DAD.......','.....DALAD......','....DAALAAD.....','...DAAALAAAD....','..DAAAALAAAAD...','...DAAAAAAAD....','....DAAAAAD.....','.....DAAAD......','......DAD.......','.......D........','................','................','................','................'],
    {'D': '#7a2a8e', 'A': '#d66be8', 'L': '#ffd6ff'}, 'aurorite')
art(['................','..........FF....','.........FWF....','........FWWF....','.......FWWF.....','......FWSF......','.....FWSF.......','....FWSF........','...FWSF.........','...FSF..........','..FF............','.Q..............','Q...............','................','................','................'],
    {'F': '#3a4f66', 'W': '#9fb3c2', 'S': '#f2c94c', 'Q': '#6b4220'}, 'stormhawk_feather')
art(['................','................','.......D........','......DCD.......','.....DCLCD......','.....DCLCD......','....DCCLCCD.....','....DCLCCCD.....','.....DCLCD......','.....DCCCD......','......DCD.......','.......D........','................','................','................','................'],
    {'D': '#1f9c90', 'C': '#62f0e0', 'L': '#e2fffb'}, 'ion_shard')
art(['................','................','.......G........','......G.........','....BBGBB.......','...BWBBBWB......','...BBBBBBB......','..BBWBBBBBB.....','..BBBBBWBBB.....','...BBBBBBB......','....BBBBB.......','................','................','................','................','................'],
    {'G': '#3f8f9a', 'B': '#f2a65a', 'W': '#ffe3b8'}, 'cloudberries')
art(['................','................','................','................','.....CBCBCB.....','....CBBBBBBC....','...CWWWWWWWWC...','...CKKKKKKKKC...','...CCCCCCCCCC...','................','................','................','................','................','................','................'],
    {'C': '#d9b07a', 'B': '#f2a65a', 'W': '#fff1d6', 'K': '#a8743e'}, 'cloudberry_tart')
ray = ['................','................','................','.......BB.......','.....BBBBB......','...BBLBBBBBB....','..BBBBBBLBBBBT..','...BBBBBBBB..TT.','.....BBBBB.....T','.......BB.......','................','................','................','................','................','................']
art(ray, {'B': '#e0a3a8', 'L': '#f7d4d6', 'T': '#c98088'}, 'raw_skyray')
art(ray, {'B': '#b9733e', 'L': '#e8b47a', 'T': '#8f5228'}, 'cooked_skyray')
art(['................','......CC........','......KK........','......GG........','.....G..G.......','.....G..G.......','....G....G......','...GFFFFFFG.....','...GFHFFFFG.....','...GFFFFHFG.....','...GFFHFFFG.....','...GFFFFFFG.....','....GGGGGG......','................','................','................'],
    {'C': '#8b5a2b', 'K': '#6b4220', 'G': '#cfe8f2', 'F': '#9fd8ff', 'H': '#ffffff'}, 'storm_nectar')
art(['................','................','................','................','................','.....CCCCCC.....','...CCBBBBBBCC...','..CBBWBBBWBBBC..','..CBBBBBBBBBBC..','..CCCCCCCCCCCC..','................','................','................','................','................','................'],
    {'C': '#a8743e', 'B': '#e0c08a', 'W': '#9fd8ff'}, 'gale_bread')
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
egg('#3d3358', '#c79bff', 'tempest_sovereign_spawn_egg', 480)
egg('#e0a3a8', '#6fdcff', 'skyray_spawn_egg', 481)
egg('#f4f6fa', '#9fb3c2', 'puffling_spawn_egg', 482)
egg('#62f0e0', '#ffffff', 'static_wisp_spawn_egg', 483)
egg('#9fb3c2', '#e6edf3', 'cyclone_spawn_egg', 484)
egg('#3a4f66', '#f2c94c', 'stormhawk_spawn_egg', 485)

# ---------------- entities ----------------
def uv_fill(img, u, v, w, h, d, base, rnd, top=None, amt=10):
    for yy in range(v, v + d + h):
        for xx in range(u, u + 2 * (d + w)):
            if (yy < v + d) and not (u + d <= xx < u + d + 2 * w): continue
            c = top if (top is not None and yy < v + d) else base
            img.putpixel((xx, yy), jitter(c, amt, rnd))
def front(u, v, w, h, d):
    """Pixel origin of a box's front (north) face."""
    return u + d, v + d
# Tempest Sovereign 128x64
rnd = random.Random(490); s = Image.new('RGBA', (128, 64), (0, 0, 0, 0))
ROBE = hexc('#3d3358'); ROBE_D = hexc('#29223d'); SKIN = hexc('#b8c8e8')
uv_fill(s, 0, 0, 8, 8, 8, SKIN, rnd, top=hexc('#9fb0d4'), amt=8)
fx, fy = front(0, 0, 8, 8, 8)
for (x, y) in ((1, 3), (2, 3), (5, 3), (6, 3)): s.putpixel((fx + x, fy + y), hexc('#6fdcff'))
for (x, y) in ((1, 4), (6, 4)): s.putpixel((fx + x, fy + y), hexc('#e9fbff'))
for x in range(2, 6): s.putpixel((fx + x, fy + 6), hexc('#5a6a8c'))
uv_fill(s, 32, 0, 9, 2, 9, hexc('#f2c94c'), rnd, top=hexc('#fff3b0'), amt=8)
for x in range(41, 59, 3): s.putpixel((x, 10), hexc('#c79bff'))
uv_fill(s, 70, 0, 1, 3, 1, hexc('#fff3b0'), rnd, amt=4)
uv_fill(s, 0, 16, 10, 12, 6, ROBE, rnd, top=ROBE_D, amt=8)
for y in range(22, 34):
    s.putpixel((6 + 5, y), hexc('#f2c94c'))
for x in range(6, 16): s.putpixel((x, 30), hexc('#c79bff'))
uv_fill(s, 0, 34, 12, 10, 8, ROBE_D, rnd, amt=10)
for i in range(60):
    x, y = rnd.randrange(0, 40), rnd.randrange(42, 52)
    s.putpixel((x, y), jitter(hexc('#7b4fc9') if rnd.random() < 0.5 else hexc('#6fdcff'), 10, rnd))
uv_fill(s, 32, 16, 3, 12, 3, ROBE, rnd, amt=8); uv_fill(s, 44, 16, 3, 12, 3, ROBE, rnd, amt=8)
for (u0) in (32, 44):
    for x in range(u0, u0 + 12): s.putpixel((x, 29), SKIN); s.putpixel((x, 30), SKIN)
for yy in range(34, 55):
    for xx in range(40, 120):
        s.putpixel((xx, yy), jitter(hexc('#9fe8ff', 200) if (xx + yy) % 3 else hexc('#ffffff', 230), 10, rnd))
uv_fill(s, 88, 0, 3, 3, 3, hexc('#e9fbff'), rnd, amt=10)
s.save(f'{OUT}/entity/tempest_sovereign.png')
# Skyray 64x64
rnd = random.Random(491); r = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
RAY = hexc('#7f6aa8'); RAY_B = hexc('#e8e0f4')
uv_fill(r, 0, 0, 14, 2, 10, RAY, rnd, top=hexc('#6a5893'), amt=8)
for i in range(18): r.putpixel((rnd.randrange(10, 38), rnd.randrange(0, 10)), hexc('#6fdcff'))
uv_fill(r, 0, 12, 8, 1, 8, RAY, rnd, top=hexc('#8f7ab8'), amt=8)
uv_fill(r, 0, 21, 8, 1, 8, RAY, rnd, top=hexc('#8f7ab8'), amt=8)
for (v0) in (12, 21):
    for x in range(8, 16): r.putpixel((x, v0 + 7), hexc('#6fdcff'))
uv_fill(r, 0, 30, 1, 1, 12, hexc('#5a4a80'), rnd, amt=6)
uv_fill(r, 28, 30, 6, 2, 3, RAY, rnd, amt=8)
r.putpixel((29 + 3, 33), (20, 20, 30, 255)); r.putpixel((29 + 6, 33), (20, 20, 30, 255))
r.save(f'{OUT}/entity/skyray.png')
# Puffling 64x64
rnd = random.Random(492); p = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
uv_fill(p, 0, 0, 8, 6, 10, hexc('#8a93a8'), rnd, amt=6)
uv_fill(p, 0, 16, 12, 9, 14, hexc('#f4f6fa'), rnd, top=hexc('#ffffff'), amt=10)
for i in range(40): p.putpixel((rnd.randrange(0, 52), rnd.randrange(16, 39)), hexc('#d6dde8'))
uv_fill(p, 36, 0, 6, 6, 6, hexc('#9aa3b8'), rnd, amt=6)
fx, fy = front(36, 0, 6, 6, 6)
p.putpixel((fx + 1, fy + 2), (30, 30, 40, 255)); p.putpixel((fx + 4, fy + 2), (30, 30, 40, 255))
p.putpixel((fx + 2, fy + 4), hexc('#e8a0b8')); p.putpixel((fx + 3, fy + 4), hexc('#e8a0b8'))
uv_fill(p, 0, 40, 2, 5, 2, hexc('#6c7488'), rnd, amt=5)
p.save(f'{OUT}/entity/puffling.png')
# Static Wisp 32x32 (orb layout: core 4^3 at 0,0; shell 7^3 at 0,8)
rnd = random.Random(493); w = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
uv_fill(w, 0, 0, 4, 4, 4, hexc('#ffffff'), rnd, amt=4)
for yy in range(8, 22):
    for xx in range(0, 28):
        if (yy < 15) and not (7 <= xx < 21): continue
        w.putpixel((xx, yy), jitter(hexc('#62f0e0', 150) if rnd.random() < 0.7 else hexc('#e2fffb', 200), 12, rnd))
w.save(f'{OUT}/entity/static_wisp.png')
# Cyclone 128x64 (waterspout layout uses the whole sheet)
rnd = random.Random(494); c = Image.new('RGBA', (128, 64), (0, 0, 0, 0))
for yy in range(64):
    for xx in range(128):
        v = math.sin(xx * 0.25 + yy * 0.5)
        col = hexc('#e6edf3') if v > 0.3 else hexc('#9fb3c2') if v > -0.4 else hexc('#6c7a8c')
        c.putpixel((xx, yy), jitter(col[:3] + (150,), 8, rnd))
c.save(f'{OUT}/entity/cyclone.png')
# Stormhawk 64x32 (gull layout)
rnd = random.Random(495); h = Image.new('RGBA', (64, 32), (0, 0, 0, 0))
HAWK = hexc('#3a4f66'); HAWK_L = hexc('#9fb3c2')
uv_fill(h, 0, 0, 4, 4, 7, HAWK, rnd, top=hexc('#2c3e52'), amt=8)
uv_fill(h, 24, 0, 3, 3, 3, HAWK_L, rnd, amt=6)
fx, fy = front(24, 0, 3, 3, 3); h.putpixel((fx, fy + 1), hexc('#f2c94c')); h.putpixel((fx + 2, fy + 1), hexc('#f2c94c'))
uv_fill(h, 36, 0, 1, 1, 2, hexc('#f2c94c'), rnd, amt=4)
uv_fill(h, 24, 8, 3, 1, 3, HAWK, rnd, amt=6)
for v0 in (12, 18):
    uv_fill(h, 0, v0, 6, 1, 5, HAWK, rnd, top=hexc('#4a6380'), amt=8)
    for x in range(5, 17, 2): h.putpixel((x, v0 + 4), hexc('#f2c94c'))
uv_fill(h, 40, 8, 1, 4, 1, hexc('#f2c94c'), rnd, amt=4); uv_fill(h, 44, 8, 1, 4, 1, hexc('#f2c94c'), rnd, amt=4)
h.save(f'{OUT}/entity/stormhawk.png')

# ---------------- armor layers ----------------
def armor_layer(path, name, parts, base, top, spark_a, spark_b):
    img = Image.new('RGBA', (64, 32), (0, 0, 0, 0)); rnd = random.Random(hash(path + name) & 0xffff)
    for (u, v, w_, h_, d_) in parts:
        uv_fill(img, u, v, w_, h_, d_, base, rnd, top=top, amt=10)
        for i in range(w_ * h_ // 3):
            x = rnd.randrange(u, u + 2 * (d_ + w_)); y = rnd.randrange(v + d_, v + d_ + h_)
            img.putpixel((x, y), spark_a if rnd.random() < 0.4 else spark_b)
    img.save(f'{OUT}/entity/equipment/{path}/{name}.png')
FULL = [(0, 0, 8, 8, 8), (16, 16, 8, 12, 4), (40, 16, 4, 12, 4), (0, 16, 4, 12, 4)]
LEGS = [(16, 16, 8, 12, 4), (0, 16, 4, 12, 4)]
armor_layer('humanoid', 'voltite', FULL, VOLT, VOLT_L, VOLT_D, hexc('#ffffff'))
armor_layer('humanoid_leggings', 'voltite', LEGS, VOLT, VOLT_L, VOLT_D, hexc('#ffffff'))
armor_layer('humanoid', 'aurorite', FULL, AUR, AUR_L, AUR_D, hexc('#6fdcff'))
armor_layer('humanoid_leggings', 'aurorite', LEGS, AUR, AUR_L, AUR_D, hexc('#6fdcff'))
armor_layer('humanoid', 'cloudstep', [(0, 16, 4, 12, 4)], hexc('#e6edf3'), hexc('#ffffff'), hexc('#9fb3c2'), hexc('#ffffff'))
print('textures4 done')
