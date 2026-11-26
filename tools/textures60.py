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
palette('radar_parts', [hexa(c) for c in ('#2b3036', '#3d444c', '#0c2a14', '#30e070', '#e0b030', '#9aa2aa', '#c03028', '#e8eef2')])
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


def radar_screen():
    """The handheld radar's screen: a green PPI with range rings, a sweep and a red hook echo."""
    import math
    img = Image.new('RGBA', (64, 64), (8, 22, 12, 255))
    for y in range(64):
        for x in range(64):
            r = math.hypot(x - 31.5, y - 33.5)
            if abs(r - 9) < 0.6 or abs(r - 18) < 0.6 or abs(r - 27) < 0.6: img.putpixel((x, y), (30, 96, 46, 255))
            a = math.atan2(y - 33.5, x - 31.5)
            if r < 28 and abs(a + 0.9) < 0.04: img.putpixel((x, y), (120, 255, 150, 255))
            hx, hy = x - 42, y - 22
            if hx * hx * 0.5 + hy * hy < 30: img.putpixel((x, y), (40, 200, 60, 255))
            if hx * hx * 0.5 + hy * hy < 12: img.putpixel((x, y), (240, 220, 40, 255))
            if hx * hx * 0.6 + (hy - 1) ** 2 < 4: img.putpixel((x, y), (230, 40, 30, 255))
    save(img, 'item/storm_radar_screen.png')


radar_screen()


def rain_shaft():
    """Vertical rain streaks for the precipitation curtain under storm cells (tiles vertically, scrolls in game)."""
    import random
    r = random.Random(62)
    img = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
    for k in range(70):
        x = r.randrange(64); y0 = r.randrange(64); ln = r.randint(10, 26); a = r.randint(90, 200)
        for i in range(ln):
            y = (y0 + i) % 64
            img.putpixel((x, y), (205, 214, 226, int(a * (0.4 + 0.6 * i / ln))))
    for y in range(64):
        for x in range(64):
            p = img.getpixel((x, y))
            if p[3] == 0: img.putpixel((x, y), (150, 160, 175, 70))
    save(img, 'entity/rain_shaft.png')


rain_shaft()


def hail_sprites():
    """Falling hailstones: milky ice balls with a bright top-left highlight and a translucent rim (32 px)."""
    import math
    for k in range(3):
        img = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
        rr = 13.5 - k * 1.0
        for y in range(32):
            for x in range(32):
                dx, dy = x - 15.5, y - 15.5
                lump = 1 + 0.06 * math.sin(math.atan2(dy, dx) * (3 + k) + k)
                d = math.hypot(dx, dy) / (rr * lump)
                if d > 1: continue
                shade = 1 - 0.35 * max(0, (dx + dy) / (2 * rr))
                hl = max(0, 1 - math.hypot(dx + 5, dy + 5) / 5)
                v = int(min(255, (205 + 40 * hl) * shade))
                a = 255 if d < 0.82 else int(255 * (1 - (d - 0.82) / 0.18) * 0.9)
                img.putpixel((x, y), (v - 8, v, min(255, v + 12), a))
        save(img, f'particle/hail_stone_{k}.png')


def hail_pellets():
    """Packed hail on the ground: a granular crust of white and blue-grey pellets, and a pellet texture for lumps."""
    import random
    r = random.Random(63)
    crust = Image.new('RGBA', (64, 64))
    for y in range(64):
        for x in range(64):
            v = 206 + r.randint(-18, 12)
            crust.putpixel((x, y), (v - 6, v, min(255, v + 10), 255))
    for _ in range(140):
        cx, cy, rad = r.randrange(64), r.randrange(64), r.choice((1, 2, 2, 3))
        for y in range(-rad, rad + 1):
            for x in range(-rad, rad + 1):
                if x * x + y * y <= rad * rad:
                    t = (x + y) / (2 * rad + 0.01)
                    v = int(236 - 40 * max(0, t))
                    crust.putpixel(((cx + x) % 64, (cy + y) % 64), (v - 4, v + 2, min(255, v + 14), 255))
    save(crust, 'block/hail_crust.png')
    pel = Image.new('RGBA', (64, 64))
    for y in range(64):
        for x in range(64):
            t = (x + y) / 126
            v = int(250 - 60 * t)
            pel.putpixel((x, y), (v - 6, v, min(255, v + 12), 255))
    save(pel, 'block/hail_pellet.png')


hail_sprites(); hail_pellets()


def puddle_icon():
    import math
    img = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
    for y in range(64):
        for x in range(64):
            dx, dy = (x - 31.5) / 26, (y - 34) / 16
            r = math.hypot(dx, dy) * (1 + 0.08 * math.sin(math.atan2(dy, dx) * 3))
            if r < 1:
                v = 1 - r
                img.putpixel((x, y), (int(60 + 80 * v), int(110 + 80 * v), int(190 + 50 * v), 220))
                if 0.55 < r < 0.62 and y < 34: img.putpixel((x, y), (220, 236, 255, 230))
    save(img, 'item/puddle.png')


puddle_icon()


def locker_doors():
    """Rusted locker front: the lower half is a solid door (riveted, dented, with the latch); only the upper half has
    louvred slats, at the hiding player's eye level, so you see out without being seen from below."""
    import random
    r = random.Random(64)
    rust, rust_d, rust_l, metal = (150, 82, 44), (92, 48, 26), (196, 120, 70), (176, 182, 188)
    def base():
        img = Image.new('RGBA', (64, 64))
        for y in range(64):
            for x in range(64):
                c = tuple(max(0, min(255, v + r.randint(-14, 14))) for v in rust)
                if r.random() < 0.08: c = tuple(max(0, min(255, v + r.randint(-10, 10))) for v in rust_l)
                if x < 4 or x > 59: c = rust_d
                img.putpixel((x, y), c + (255,))
        return img
    low = base()
    for y in (6, 57):
        for x in range(4, 60): low.putpixel((x, y), rust_d + (255,))
    for (x, y) in ((8, 10), (55, 10), (8, 52), (55, 52), (31, 10), (31, 52)):
        for dx in range(2):
            for dy in range(2): low.putpixel((x + dx, y + dy), (rust_l if dx + dy == 0 else rust_d) + (255,))
    for y in range(24, 34):                                   # latch
        for x in range(46, 50): low.putpixel((x, y), metal + (255,) if x < 48 else (120, 126, 132, 255))
    for y in range(14, 46):                                   # a long dent catching the light
        low.putpixel((20 + (y % 3 == 0), y), tuple(min(255, v + 22) for v in rust) + (255,))
    save(low, 'block/rusted_locker_grate_lower.png')
    up = base()
    for y in range(12, 48, 6):                                # louvres across the middle of the upper door
        for x in range(8, 56):
            for k in range(3): up.putpixel((x, y + k), (0, 0, 0, 0))
            up.putpixel((x, y + 3), rust_d + (255,))
            up.putpixel((x, y - 1), tuple(min(255, v + 25) for v in rust) + (255,))
    for x in range(4, 60): up.putpixel((x, 4), rust_d + (255,)); up.putpixel((x, 58), rust_d + (255,))
    save(up, 'block/rusted_locker_grate_upper.png')


locker_doors()

print('textures60: done')
