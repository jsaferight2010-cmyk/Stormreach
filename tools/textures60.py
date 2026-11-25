"""Stormreach 6.0 textures: the plain white texture the procedural machine models are coloured on, and the 6.0 item
and block art. Runs after textures10.py (before remaster64, which leaves anything already larger than 16x alone)."""
import pathlib
from PIL import Image

ROOT = pathlib.Path(__file__).resolve().parent.parent
TEX = ROOT / 'src/main/resources/assets/stormreach/textures'


def save(img, rel):
    p = TEX / rel; p.parent.mkdir(parents=True, exist_ok=True); img.save(p)


save(Image.new('RGBA', (16, 16), (255, 255, 255, 255)), 'misc/white.png')


def salt_facet():
    """Halite grows as cubes with stepped 'hopper' faces: concentric squares, milky white with a faint rose tint."""
    import random
    r = random.Random(60)
    img = Image.new('RGBA', (64, 64))
    px = img.load()
    for y in range(64):
        for x in range(64):
            d = max(abs(x - 31.5), abs(y - 31.5))          # square distance from the centre -> hopper steps
            step = int(d) // 6
            base = 232 - step * 5 + (6 if int(d) % 6 == 0 else 0)
            n = r.randint(-4, 4)
            c = (min(255, base + n + 6), min(255, base + n - 2), min(255, base + n), 255)
            if int(d) % 6 == 5: c = (c[0] - 22, c[1] - 24, c[2] - 20, 255)
            if d > 30.5: c = (255, 250, 250, 255) if (x < 2 or y < 2) else (176, 168, 172, 255)
            px[x, y] = c
    return img


def salt_crust():
    import random
    r = random.Random(61)
    img = Image.new('RGBA', (64, 64))
    for y in range(64):
        for x in range(64):
            v = 196 + r.randint(-14, 10)
            img.putpixel((x, y), (v + 4, v, v - 2, 255))
    return img


save(salt_facet(), 'block/salt_crystal_facet.png')
save(salt_crust(), 'block/salt_crust.png')


def palette(name, colours):
    """A 16x16 swatch sheet (4x4 cells of 4 px) that 3D item models colour their faces from."""
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for i, c in enumerate(colours):
        x0, y0 = (i % 4) * 4, (i // 4) * 4
        for y in range(4):
            for x in range(4):
                img.putpixel((x0 + x, y0 + y), c)
    save(img.resize((64, 64), Image.NEAREST), f'item/{name}.png')   # 64 px so remaster64 leaves the swatches alone


def hexa(h):
    h = h.lstrip('#'); return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


# umbrella: storm-blue and white canopy panels, steel shaft, wooden J handle, black tips
palette('umbrella_parts', [hexa(c) for c in ('#2f5f9e', '#eef2f6', '#5b6269', '#7a5232', '#1c1e22', '#f2c94c', '#3f78c0', '#c9d3dd')])
# stormkite: navy and sky sail, lightning-yellow stripe, spruce spars, red/white tail bows, cord
palette('stormkite_parts', [hexa(c) for c in ('#1f3e74', '#7fc4ec', '#f2c94c', '#6b4a2c', '#d33a2c', '#f4f4f4', '#d8d0bf', '#2a5aa0')])


def umbrella_canopy():
    """Seen from above: eight alternating gores (wedges) of storm blue and white."""
    import math
    img = Image.new('RGBA', (32, 32))
    for y in range(32):
        for x in range(32):
            a = math.atan2(y - 15.5, x - 15.5)
            k = int((a + math.pi) / (math.pi / 4)) % 2
            r = math.hypot(x - 15.5, y - 15.5)
            c = hexa('#2f5f9e') if k else hexa('#eef2f6')
            if int(r) % 5 == 4: c = tuple(int(v * 0.9) for v in c[:3]) + (255,)
            img.putpixel((x, y), c)
    save(img, 'item/umbrella_canopy.png')


umbrella_canopy()


def sight_glass():
    """A brass-framed gauge glass: clear, faintly blue, with a highlight streak."""
    img = Image.new('RGBA', (64, 64), (190, 225, 245, 70))
    for y in range(64):
        for x in range(64):
            if x < 6 or x > 57 or y < 4 or y > 59: img.putpixel((x, y), (176, 138, 64, 255) if (x + y) % 7 else (210, 172, 92, 255))
            elif 14 <= x <= 18: img.putpixel((x, y), (235, 248, 255, 120))
    save(img, 'block/sight_glass.png')


def grate():
    img = Image.new('RGBA', (64, 64))
    for y in range(64):
        for x in range(64):
            v = 58 + ((x * 7 + y * 3) % 11)
            img.putpixel((x, y), (v, v + 2, v + 6, 255))
    save(img, 'block/iron_grate.png')


sight_glass(); grate()


# machine icons rendered from the very meshes the game draws (tools/art/icon_*.png, made with the offline mesh renderer)
import shutil
ART = pathlib.Path(__file__).resolve().parent / 'art'
for art, item in (('radar', 'tornado_radar'), ('turbine', 'wind_turbine'), ('siren', 'tornado_siren'), ('station', 'weather_station')):
    shutil.copy(ART / f'icon_{art}.png', TEX / 'item' / f'{item}.png')

print('textures60: done')
