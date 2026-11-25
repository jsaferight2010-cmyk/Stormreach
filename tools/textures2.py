"""Stormreach 2.0 textures."""
import os as _os, pathlib as _pl
ROOT = str(_pl.Path(__file__).resolve().parent.parent)  # the Stormreach project folder
VANILLA = _os.environ.get('STORMREACH_VANILLA', str(_pl.Path(ROOT) / '.vanilla'))  # extracted 26.3 jar (assets/ + data/)
MCJAR = _os.environ.get('STORMREACH_MCJAR', str(_pl.Path(ROOT) / '.vanilla' / 'minecraft-26.3.jar'))
import random, os, math
from PIL import Image, ImageDraw
OUT = ROOT + '/src/main/resources/assets/stormreach/textures'
for d in ('block', 'item', 'entity', 'entity/equipment/humanoid', 'entity/equipment/humanoid_leggings', 'entity/equipment/wings', 'mob_effect'):
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
STONE = hexc('#4f5a69'); STONE_L = hexc('#6d7a8c'); STONE_D = hexc('#353d49')
CRYS = hexc('#6fdcff'); CRYS_L = hexc('#d2f7ff'); CRYS_D = hexc('#2f9fd0')
WOOD = hexc('#a49e90'); WOOD_D = hexc('#7d776a'); WOOD_L = hexc('#c2bcad')
PINE = hexc('#4a3a2e'); PINE_D = hexc('#33271f'); PINE_L = hexc('#6b5443')
PLANK = hexc('#7a6a5a'); PLANK_D = hexc('#5c4e41'); PLANK_L = hexc('#93816e')

# ---- stone variants ----
img = load('block/stormstone'); rnd = random.Random(101)
for (x0, y0) in ((3, 2), (10, 6), (5, 11), (12, 13)):
    for k in range(4):
        x, y = x0 + k, y0 + (k % 2)
        if 0 <= x < 16 and 0 <= y < 16: img.putpixel((x, y), CRYS if k % 2 else CRYS_L)
save(img, 'block/charged_stormstone')
img = load('block/stormstone_bricks'); rnd = random.Random(102)
for (x, y) in ((2, 1), (3, 2), (3, 3), (4, 4), (11, 8), (12, 9), (12, 10), (7, 13), (8, 14)):
    img.putpixel((x, y), hexc('#22272f'))
save(img, 'block/cracked_stormstone_bricks')
img = load('block/polished_stormstone')
d = ImageDraw.Draw(img)
d.rectangle([4, 4, 11, 11], outline=STONE_D)
for (x, y) in ((8, 5), (7, 6), (7, 7), (8, 7), (9, 7), (8, 8), (7, 9), (6, 10)): img.putpixel((x, y), CRYS)
save(img, 'block/chiseled_stormstone')

# ---- wood families ----
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
save(log_side(PINE, PINE_D, 103), 'block/stormpine_log')
save(log_top(PINE, hexc('#a89074'), hexc('#8a7358'), 104), 'block/stormpine_log_top')
save(log_side(hexc('#a89074'), hexc('#8a7358'), 105), 'block/stripped_stormpine_log')
save(log_top(hexc('#a89074'), hexc('#a89074'), hexc('#8a7358'), 106), 'block/stripped_stormpine_log_top')
save(planks(PLANK, PLANK_D, PLANK_L, 107), 'block/stormpine_planks')
# leaves: blue-green needles with gaps (cutout)
img = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); rnd = random.Random(108)
for y in range(16):
    for x in range(16):
        if rnd.random() < 0.78:
            img.putpixel((x, y), jitter(hexc('#3f6b5e') if rnd.random() < 0.6 else hexc('#2c5046'), 10, rnd))
        if rnd.random() < 0.03: img.putpixel((x, y), hexc('#a8d8e8'))
save(img, 'block/stormpine_leaves')
img = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); rnd = random.Random(109)
for y in range(10, 16): img.putpixel((8, y), PINE)
for lvl, w in ((2, 1), (4, 2), (6, 3), (8, 4), (10, 3)):
    for x in range(8 - w, 9 + w):
        img.putpixel((x, lvl), jitter(hexc('#3f6b5e'), 10, rnd)); img.putpixel((x, lvl + 1), jitter(hexc('#2c5046'), 10, rnd))
save(img, 'block/stormpine_sapling')

def door(base, dark, frame, seed, name, window):
    for half in ('top', 'bottom'):
        img, rnd = noise(base, 6, seed + (half == 'top'))
        for y in range(16):
            for x in range(16):
                if x in (0, 15) or (half == 'top' and y == 0) or (half == 'bottom' and y == 15) or y == (15 if half == 'top' else 0):
                    img.putpixel((x, y), jitter(frame, 4, rnd))
                elif x in (5, 10): img.putpixel((x, y), jitter(dark, 4, rnd))
        if half == 'top' and window:
            for y in range(3, 9):
                for x in range(3, 13):
                    img.putpixel((x, y), (0, 0, 0, 0) if (x not in (7, 8) and y != 6) else jitter(frame, 3, rnd))
        if half == 'bottom':
            img.putpixel((12, 1), hexc('#c9b37a')); img.putpixel((12, 2), hexc('#c9b37a'))
        save(img, f'block/{name}_door_{half}')
    # item icon
    icon = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    top = load(f'block/{name}_door_top').resize((8, 8), Image.NEAREST); bot = load(f'block/{name}_door_bottom').resize((8, 8), Image.NEAREST)
    icon.paste(top, (4, 0)); icon.paste(bot, (4, 8))
    save(icon, f'item/{name}_door')
    img, rnd = noise(base, 6, seed + 7)
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15): img.putpixel((x, y), jitter(frame, 4, rnd))
            elif window and 4 <= x <= 11 and 4 <= y <= 11 and not (x in (7, 8) or y in (7, 8)): img.putpixel((x, y), (0, 0, 0, 0))
            elif y % 5 == 0: img.putpixel((x, y), jitter(dark, 4, rnd))
    save(img, f'block/{name}_trapdoor')
door(WOOD_L, WOOD_D, WOOD, 110, 'driftwood', True)
door(PLANK, PLANK_D, PINE, 120, 'stormpine', False)

# ---- glass pane edge, fulgurite, lantern ----
img = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); rnd = random.Random(130)
for y in range(16):
    for x in range(7, 9): img.putpixel((x, y), jitter(hexc('#9fd8ee', 230), 6, rnd))
save(img, 'block/stormglass_pane_top')
img, rnd = noise(hexc('#c9b98f'), 10, 131)
for i in range(7):
    x, y = rnd.randrange(16), rnd.randrange(16)
    for k in range(rnd.randint(3, 6)):
        xx, yy = (x + k) % 16, (y + rnd.randint(-1, 1)) % 16
        img.putpixel((xx, yy), hexc('#f3ecd8') if k % 2 else hexc('#7a6a48'))
for i in range(5): img.putpixel((rnd.randrange(16), rnd.randrange(16)), CRYS_L)
save(img, 'block/fulgurite')
lan = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); rnd = random.Random(132)
for y in range(2, 9):
    for x in range(0, 6):
        edge = x in (0, 5) or y in (2, 8)
        lan.putpixel((x, y), jitter(hexc('#8a7a58'), 6, rnd) if edge else jitter(hexc('#9ff3ff'), 10, rnd))
for y in range(9, 15):
    for x in range(0, 6): lan.putpixel((x, y), jitter(hexc('#8a7a58'), 6, rnd))
for y in range(0, 2):
    for x in range(1, 5): lan.putpixel((x, y), jitter(hexc('#6e6046'), 6, rnd))
for y in range(10, 14):
    for x in range(1, 5): lan.putpixel((x, y), jitter(hexc('#6e6046'), 6, rnd))
for (x0, y0) in ((11, 1), (11, 10)):
    for y in range(y0, y0 + 2):
        for x in range(x0, x0 + 3): lan.putpixel((x, y), hexc('#5a503c'))
save(lan, 'block/fulgurite_lantern')
item = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
big = lan.crop((0, 2, 6, 9)).resize((8, 10), Image.NEAREST); item.paste(big, (4, 4))
item.paste(lan.crop((1, 0, 5, 2)).resize((6, 2), Image.NEAREST), (5, 2))
for x in (7, 8): item.putpixel((x, 0), hexc('#5a503c')); item.putpixel((x, 1), hexc('#5a503c'))
save(item, 'item/fulgurite_lantern')

# ---- salt ----
img, rnd = noise(hexc('#eceff2'), 7, 140)
for i in range(10): img.putpixel((rnd.randrange(16), rnd.randrange(16)), hexc('#ffffff'))
for i in range(6): img.putpixel((rnd.randrange(16), rnd.randrange(16)), hexc('#c8cfd6'))
save(img, 'block/salt_block')
img = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); rnd = random.Random(141)
for (cx, h, w) in ((5, 9, 2), (9, 12, 2), (12, 6, 1), (7, 6, 1)):
    for y in range(16 - h, 16):
        for x in range(cx - w + 1, cx + w):
            img.putpixel((x, y), jitter(hexc('#f4f6f8') if x == cx else hexc('#d3dae1'), 5, rnd))
    img.putpixel((cx, 16 - h), hexc('#ffffff'))
save(img, 'block/salt_crystal')

# ---- plants ----
img = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); rnd = random.Random(150)
for (sx, top) in ((4, 6), (8, 3), (12, 7)):
    for y in range(top + 1, 16): img.putpixel((sx, y), jitter(hexc('#5f8a6a'), 6, rnd))
    for (dx, dy) in ((-1, 0), (1, 0), (0, -1), (0, 0), (-1, 1), (1, 1)):
        img.putpixel((sx + dx, top + dy), jitter(hexc('#b28ce0'), 12, rnd))
    img.putpixel((sx, top), hexc('#f0e6ff'))
for x in (2, 6, 10, 14): img.putpixel((x, 15), hexc('#6f9a76')); img.putpixel((x, 14), hexc('#6f9a76'))
save(img, 'block/sea_rocket')
img = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); rnd = random.Random(151)
for i in range(9):
    x = rnd.randint(1, 14); h = rnd.randint(6, 13); lean = rnd.choice((-1, 0, 1))
    for k in range(h):
        xx = x + (lean if k > h // 2 else 0)
        img.putpixel((max(0, min(15, xx)), 15 - k), jitter(hexc('#8fa86a') if k < h - 2 else hexc('#c8d49a'), 8, rnd))
save(img, 'block/saltgrass')

# ---- machines ----
img = load('block/polished_stormstone'); rnd = random.Random(160)
d = ImageDraw.Draw(img); d.rectangle([2, 2, 13, 13], outline=hexc('#b87333'))
save(img, 'block/storm_collector_side')
save(load('block/polished_stormstone'), 'block/storm_collector_bottom')
for c in range(5):
    img = load('block/polished_stormstone')
    for y in range(3, 13):
        for x in range(3, 13):
            r = math.hypot(x - 7.5, y - 7.5)
            if r < 5:
                lit = r < 1.2 + c * 0.9
                img.putpixel((x, y), CRYS_L if lit and r < 1.5 else CRYS if lit else hexc('#2a3644'))
    save(img, f'block/storm_collector_top_{c}')
img, rnd = noise(hexc('#b87333'), 10, 161)
for y in range(16):
    for x in range(16):
        if rnd.random() < 0.15: img.putpixel((x, y), jitter(hexc('#5fa38a'), 8, rnd))  # verdigris
save(img, 'block/wind_vane')

# ---- items ----
def art(rows, palette, name):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != '.': img.putpixel((x, y), hexc(palette[ch]))
    save(img, f'item/{name}')
art(['................','................','................','.......WW.......','......WSWW......','.....WWWSWW.....','....WWSWWWSW....','...WWWWWWWWWW...','...SWWWSWWWWS...','....SSSSSSSS....','................','................','................','................','................','................'],
    {'W': '#f4f6f8', 'S': '#c8cfd6'}, 'salt')
art(['................','..........C.....','.........CL.....','........CLY.....','.......CLYY.....','.......LYYD.....','......CYYD......','......LYD.......','.....CYYD.......','.....LYD........','....CYD.........','....YD..........','...YD...........','...D............','................','................'],
    {'C': '#f3ecd8', 'L': '#d2f7ff', 'Y': '#c9b98f', 'D': '#7a6a48'}, 'fulgurite_shard')
art(['................','......MMMM......','.....MKKKKM.....','.....MCCLCM.....','.....MCLCCM.....','.....MCCCLM.....','.....MLCCCM.....','.....MCCLCM.....','.....MCLCCM.....','.....MCCCLM.....','.....MLCCCM.....','.....MCCLCM.....','.....MKKKKM.....','......MMMM......','................','................'],
    {'M': '#b87333', 'K': '#7a4a22', 'C': '#6fdcff', 'L': '#d2f7ff'}, 'storm_cell')
art(['................','.......LL.......','......LCCL......','.....LCWWCL.....','....LCWWWWCL....','...LCWWYYWWCL...','..LCWWYWWYWWCL..','..CWWYWWWWYWWC..','..CWWYWWWWYWWC..','..LCWWYWWYWWCL..','...LCWWYYWWCL...','....LCWWWWCL....','.....LCWWCL.....','......LCCL......','.......LL.......','................'],
    {'L': '#2f9fd0', 'C': '#6fdcff', 'W': '#e9fbff', 'Y': '#ffe066'}, 'tempest_core')
art(['................','................','................','.......WW.......','......WWWW......','.....WWSWWW.....','.....WSWWWW.....','....WWWWWSWW....','....WWWWWWWW....','....WSWWWWWW....','.....WWWWSW.....','.....WWWWWW.....','......WWWW......','................','................','................'],
    {'W': '#e8e2d0', 'S': '#8d7f63'}, 'gull_egg')
art(['................','................','.....WWWWWW.....','...WWWWWWWWWW...','..WWWWWYYWWWWW..','..WWWWYYYYWWWW..','..WWWWYYYYWWWW..','..WWWWWYYWWWWW..','...WWWWWWWWWW...','....WWWWWWWW....','................','................','................','................','................','................'],
    {'W': '#f7f4ec', 'Y': '#f2b23a'}, 'fried_gull_egg')
art(['................','................','................','....BBBBBBBB....','...BOOOOOOOOB...','..BOORRORROOOB..','..BOOOOOOOOOOB..','..BBBBBBBBBBBB..','...BOOROOROOB...','..BOOOOOOOOOOB..','..BBBBBBBBBBBB..','................','................','................','................','................'],
    {'B': '#a8692e', 'O': '#e0a95a', 'R': '#d4553b'}, 'crab_cakes')
def bottle(fill, name, hi):
    art(['................','......CC........','......KK........','......GG........','.....G..G.......','.....G..G.......','....G....G......','...GFFFFFFG.....','...GFHFFFFG.....','...GFFFFHFG.....','...GFFHFFFG.....','...GFFFFFFG.....','....GGGGGG......','................','................','................'],
        {'C': '#8b5a2b', 'K': '#6b4220', 'G': '#cfe8f2', 'F': fill, 'H': hi}, name)
bottle('#5b2f99', 'stormberry_jam', '#8b5cf6')
bottle('#a06ae8', 'stormberry_juice', '#d8c4ff')
bottle('#3aa0d0', 'tempest_tonic', '#e9fbff')
art(['................','................','................','................','................','...WWWWWWWWWW...','..WGGRGGKGGRGW..','..BGKGGGGRGKGB..','..BBGGKGRGGGBB..','...BBBBBBBBBB...','...BDBBBBBBDB...','....BBBBBBBB....','.....BBBBBB.....','................','................','................'],
    {'W': '#3f6b45', 'G': '#4f8a52', 'K': '#2c5a33', 'R': '#e88bb8', 'B': '#8b5a2b', 'D': '#6b4220'}, 'seaweed_salad')
art(['................','................','................','................','.........SSS....','..WWWWWWWWWWS...','.WWSWWWSWWWWWS..','WWWWWWWWWWSWWWS.','.WWWWSWWWWWWWS..','..WWWWWWWWWWS...','.........SSS....','................','................','................','................','................'],
    {'W': '#e2d6bf', 'S': '#b5a68a'}, 'salted_cod')
art(['................','................','................','................','................','...OOOOOOOOOO...','..OCSPCCOSCPCO..','..BCCCSCCCCCCB..','..BBCPCCSCCCBB..','...BBBBBBBBBB...','...BDBBBBBBDB...','....BBBBBBBB....','.....BBBBBB.....','................','................','................'],
    {'O': '#c9a06a', 'C': '#d9b07a', 'S': '#e2d6bf', 'P': '#e07a4f', 'B': '#8b5a2b', 'D': '#6b4220'}, 'sailors_stew')
fish = ['................','................','................','................','..........TT....','..BBBBBBB.TT....','.BBLBBBBBBTT....','BEBBBBLBBBBTT...','BBBBBBBBBBBTT...','.BBBBLBBBBTT....','..BBBBBBB.TT....','..........TT....','................','................','................','................']
art(fish, {'B': '#4a6a8a', 'L': '#9fe8ff', 'E': '#111111', 'T': '#6fdcff'}, 'stormfin')
art(fish, {'B': '#a8743e', 'L': '#e8c27a', 'E': '#3a2a1a', 'T': '#c98f45'}, 'cooked_stormfin')
art(['................','................','.......S........','......SCS.......','.....SCLCS......','.....SCCCS......','......SCS.......','.......K........','.......K........','.......K........','.......K........','.......K........','................','................','................','................'],
    {'S': '#d2f7ff', 'C': '#f3ecd8', 'L': '#ffffff', 'K': '#c2bcad'}, 'rock_candy')
art(['................','......CC........','......KK........','.....GGGG.......','....G....G......','...G.W..W.G.....','...G..WW..G.....','...GW.WW.WG.....','...G.W..W.G.....','...G..WW..G.....','...GW....WG.....','....GGGGGG......','................','................','................','................'],
    {'C': '#8b5a2b', 'K': '#6b4220', 'G': '#cfe8f2', 'W': '#9aa8b8'}, 'squall_in_a_bottle')
art(['................','..DDDDDDDD......','..DCCCCCCD......','..DCLCLCCDD.....','..DCCCCCCDKD....','..DDDDDDDDKKD...','.........DKKD...','..........DKKD..','...........DKKD.','............DKD.','.............DD.','................','................','................','................','................'],
    {'D': '#2a3644', 'C': '#6fdcff', 'L': '#e9fbff', 'K': '#a8743e'}, 'stormbreaker')
AR = {'C': '#6fdcff', 'L': '#d2f7ff', 'D': '#2f9fd0', 'S': '#4f5a69', 'K': '#353d49'}
art(['................','................','....SSSSSSSS....','...SCCCLCCCCS...','...SCKKKKKKCS...','...SCK....KCS...','...SS......SS...','................','................','................','................','................','................','................','................','................'], AR, 'stormshard_helmet')
art(['................','...SSS....SSS...','..SCCSSSSSSCCS..','..SCCCCLCCCCCS..','..SSCCCCCCCCSS..','...SCCLCCCCCS...','...SCCCCCLCCS...','...SCCCCCCCCS...','...SCLCCCCCCS...','...SCCCCCCLCS...','...SSSSSSSSSS...','................','................','................','................','................'], AR, 'stormshard_chestplate')
art(['................','....SSSSSSSS....','....SCCCCCCS....','....SCCLCCCS....','....SCCSSCCS....','....SCS..SCS....','....SCS..SCS....','....SLS..SCS....','....SCS..SLS....','....SCS..SCS....','....SSS..SSS....','................','................','................','................','................'], AR, 'stormshard_leggings')
art(['................','................','................','................','................','................','...SS......SS...','...SCS....SCS...','...SCS....SCS...','..SCCS....SCCS..','..SLCS....SLCS..','..SSSS....SSSS..','................','................','................','................'], AR, 'stormshard_boots')
art(['................','................','..WW........WW..','.WGWW......WWGW.','.WGGWW....WWGGW.','.WGWGWW..WWGWGW.','.WWGWGW..WGWGWW.','..WGGWG..GWGGW..','..WWGWG..GWGWW..','...WGWG..GWGW...','...WWGW..WGWW...','....WGW..WGW....','....WWS..SWW....','.....SS..SS.....','................','................'],
    {'W': '#f2f4f7', 'G': '#9aa3ad', 'S': '#6fdcff'}, 'gale_wings')
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
egg('#3a7fd4', '#9fe8ff', 'thunder_crab_spawn_egg', 170)
egg('#7d8792', '#3a3f47', 'harbor_seal_spawn_egg', 171)
egg('#3f6b5e', '#e2d6bf', 'brine_wraith_spawn_egg', 172)
egg('#e9fbff', '#6fdcff', 'ball_lightning_spawn_egg', 173)
egg('#1e2a38', '#6fdcff', 'stormwarden_spawn_egg', 174)

# ---- entities ----
def uv_fill(img, u, v, w, h, d, base, rnd, top=None, amt=10):
    for yy in range(v, v + d + h):
        for xx in range(u, u + 2 * (d + w)):
            if (yy < v + d) and not (u + d <= xx < u + d + 2 * w): continue
            c = top if (top is not None and yy < v + d) else base
            img.putpixel((xx, yy), jitter(c, amt, rnd))
crab = load('entity/cliff_crab'); px = crab.load()
for y in range(crab.height):
    for x in range(crab.width):
        r, g, b, a = px[x, y]
        if a and r > g + 30: px[x, y] = (int(b * 0.6), int(g * 0.9 + 40), min(255, r + 30), a)
for (x, y) in ((8, 1), (10, 3), (12, 2), (9, 4)): crab.putpixel((x, y), CRYS_L)
crab.save(f'{OUT}/entity/thunder_crab.png')

rnd = random.Random(180); seal = Image.new('RGBA', (64, 32), (0, 0, 0, 0))
GREY = hexc('#7d8792'); BELLY = hexc('#b9c0c8')
uv_fill(seal, 0, 0, 8, 7, 12, GREY, rnd, top=hexc('#6a737d'), amt=8)
for i in range(14): seal.putpixel((rnd.randrange(0, 40), rnd.randrange(12, 19)), hexc('#4e555d'))
uv_fill(seal, 0, 19, 6, 5, 5, GREY, rnd, amt=6)
for (x, y) in ((6, 25), (9, 25)): seal.putpixel((x, y), (15, 15, 15, 255))
uv_fill(seal, 22, 19, 3, 2, 2, BELLY, rnd, amt=4)
seal.putpixel((24, 21), (30, 30, 30, 255))
uv_fill(seal, 40, 0, 6, 2, 5, hexc('#5f6770'), rnd, amt=5)
uv_fill(seal, 32, 19, 5, 1, 3, hexc('#5f6770'), rnd, amt=5)
uv_fill(seal, 32, 23, 5, 1, 3, hexc('#5f6770'), rnd, amt=5)
seal.save(f'{OUT}/entity/harbor_seal.png')

# Brine Wraith: our own 64x64 humanoid skin (zombie UV layout)
rnd = random.Random(181); w = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
SKIN = hexc('#5f8f86'); CLOTH = hexc('#3a4f66'); ROPE = hexc('#c2b08a')
uv_fill(w, 0, 0, 8, 8, 8, SKIN, rnd, amt=10)            # head
for (x, y) in ((9, 11), (10, 11), (13, 11), (14, 11)): w.putpixel((x, y), hexc('#d8fff6'))  # glowing eyes
for x in range(9, 15): w.putpixel((x, 14), hexc('#2a3a36'))
uv_fill(w, 16, 16, 8, 12, 4, CLOTH, rnd, amt=8)         # body (torn coat)
for i in range(30): w.putpixel((rnd.randrange(16, 40), rnd.randrange(20, 32)), jitter(SKIN, 8, rnd))
for x in range(20, 28): w.putpixel((x, 26), ROPE)
uv_fill(w, 40, 16, 4, 12, 4, SKIN, rnd, amt=10)         # right arm
uv_fill(w, 32, 48, 4, 12, 4, SKIN, rnd, amt=10)         # left arm
uv_fill(w, 0, 16, 4, 12, 4, CLOTH, rnd, amt=8)          # right leg
uv_fill(w, 16, 48, 4, 12, 4, CLOTH, rnd, amt=8)         # left leg
for i in range(40):  # seaweed strands
    x, y = rnd.randrange(0, 56), rnd.randrange(16, 64)
    if w.getpixel((x, y))[3]: w.putpixel((x, y), jitter(hexc('#3f6b45'), 10, rnd))
w.save(f'{OUT}/entity/brine_wraith.png')

rnd = random.Random(182); orb = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
for yy in range(0, 8):
    for xx in range(0, 16): orb.putpixel((xx, yy), (255, 255, 255, 255) if rnd.random() < 0.5 else (220, 250, 255, 255))
for yy in range(8, 22):
    for xx in range(0, 28):
        orb.putpixel((xx, yy), (111 + rnd.randint(0, 60), 220, 255, 110 + rnd.randint(0, 60)))
orb.save(f'{OUT}/entity/ball_lightning.png')

rnd = random.Random(183); sp = Image.new('RGBA', (128, 64), (0, 0, 0, 0))
for yy in range(64):
    for xx in range(128):
        stripe = ((xx + yy * 2) // 5) % 2
        b = 170 + rnd.randint(-20, 30)
        sp.putpixel((xx, yy), (90 if stripe else 140, b, 230, 120 if stripe else 170))
sp.save(f'{OUT}/entity/waterspout.png')

sq = load('entity/squall'); px = sq.load()
for y in range(sq.height):
    for x in range(sq.width):
        r, g, b, a = px[x, y]
        if a: px[x, y] = (int(r * 0.45), int(g * 0.5), min(255, int(b * 0.8) + 20), min(255, a + 40))
for (x, y) in ((7, 8), (10, 8), (8, 9), (9, 9)): sq.putpixel((x, y), (255, 240, 120, 255))
sq.save(f'{OUT}/entity/stormwarden.png')

# ---- equipment textures ----
def armor_layer(path, parts):
    img = Image.new('RGBA', (64, 32), (0, 0, 0, 0)); rnd = random.Random(hash(path) & 0xffff)
    for (u, v, w_, h, d) in parts:
        uv_fill(img, u, v, w_, h, d, hexc('#5aa8c8'), rnd, top=hexc('#7fd0ec'), amt=10)
        for i in range(w_ * h // 3):
            x = rnd.randrange(u, u + 2 * (d + w_)); y = rnd.randrange(v + d, v + d + h)
            img.putpixel((x, y), CRYS_L if rnd.random() < 0.4 else hexc('#2c5a7a'))
    img.save(f'{OUT}/entity/equipment/{path}/stormshard.png')
armor_layer('humanoid', [(0, 0, 8, 8, 8), (16, 16, 8, 12, 4), (40, 16, 4, 12, 4), (0, 16, 4, 12, 4)])
armor_layer('humanoid_leggings', [(16, 16, 8, 12, 4), (0, 16, 4, 12, 4)])
wings = Image.new('RGBA', (64, 32), (0, 0, 0, 0)); rnd = random.Random(190)
for yy in range(0, 22):
    for xx in range(22, 46):
        if yy < 2 and not (24 <= xx < 44): continue
        c = hexc('#f2f4f7') if (xx - yy) % 6 else hexc('#9aa3ad')
        if yy > 16: c = hexc('#6fdcff')
        wings.putpixel((xx, yy), jitter(c, 6, rnd))
wings.save(f'{OUT}/entity/equipment/wings/gale_wings.png')

# ---- mob effect icons (18x18) ----
ic = Image.new('RGBA', (18, 18), (0, 0, 0, 0)); d = ImageDraw.Draw(ic)
d.polygon([(10, 1), (4, 10), (9, 10), (7, 17), (14, 7), (9, 7), (11, 1)], fill=hexc('#6fdcff'), outline=hexc('#2f9fd0'))
ic.save(f'{OUT}/mob_effect/static_charge.png')
ic = Image.new('RGBA', (18, 18), (0, 0, 0, 0)); d = ImageDraw.Draw(ic)
d.rectangle([8, 1, 9, 11], fill=hexc('#b87333'))
for i, wdt in enumerate((8, 5, 2)): d.rectangle([9 - wdt, 12 + i * 2, 8 + wdt, 12 + i * 2], fill=hexc('#8c6a3e'))
ic.save(f'{OUT}/mob_effect/grounded.png')
print('textures2 done')
