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


def wind_generator():
    """6.0 Creative Wind Generator: a caged fan on the front, riveted steel sides with a violet creative stripe, a vented back."""
    import math
    N = 64
    def steel(x, y):
        v = 92 + ((x * 13 + y * 7) % 9) - (6 if (x // 16 + y // 16) % 2 else 0)
        return (v, v + 4, v + 12, 255)
    side = Image.new('RGBA', (N, N))
    for y in range(N):
        for x in range(N):
            c = steel(x, y)
            if x < 3 or x > 60 or y < 3 or y > 60: c = (52, 58, 70, 255)
            if 26 <= y <= 37: c = (122, 72, 196, 255) if (x + y) % 9 else (150, 98, 222, 255)   # creative stripe
            if (x in (7, 56)) and (y in (7, 56)): c = (190, 196, 206, 255)                      # rivets
            side.putpixel((x, y), c)
    save(side, 'block/wind_generator_side.png')
    top = Image.new('RGBA', (N, N))
    for y in range(N):
        for x in range(N):
            c = steel(x, y)
            if x < 3 or x > 60 or y < 3 or y > 60: c = (52, 58, 70, 255)
            top.putpixel((x, y), c)
    save(top, 'block/wind_generator_top.png')
    back = Image.new('RGBA', (N, N))
    for y in range(N):
        for x in range(N):
            c = steel(x, y)
            if x < 3 or x > 60 or y < 3 or y > 60: c = (52, 58, 70, 255)
            elif 10 <= x <= 53 and 10 <= y <= 53 and (y - 10) % 6 < 3: c = (26, 30, 38, 255)        # vent slots
            back.putpixel((x, y), c)
    save(back, 'block/wind_generator_back.png')
    front = Image.new('RGBA', (N, N))
    cx = cy = 31.5
    for y in range(N):
        for x in range(N):
            dx, dy = x - cx, y - cy
            r = math.hypot(dx, dy); a = math.atan2(dy, dx)
            c = steel(x, y)
            if x < 3 or x > 60 or y < 3 or y > 60: c = (52, 58, 70, 255)
            elif r < 28:
                c = (18, 22, 30, 255)                                                           # dark housing
                blade = (math.degrees(a) % 72) < 30 + 10 * (1 - r / 28)                        # five swept blades
                if 5 < r < 26 and blade: c = (178, 196, 214, 255) if r < 22 else (150, 168, 190, 255)
                if r <= 5: c = (122, 72, 196, 255)                                              # violet hub
                if abs(r - 27) < 1.2 or (r < 27 and (abs(dx) < 0.7 or abs(dy) < 0.7) and r > 5): c = (210, 214, 222, 255)  # cage
                if abs(r - 16) < 0.6: c = (200, 204, 212, 255)
            front.putpixel((x, y), c)
    save(front, 'block/wind_generator_front.png')


wind_generator()


# ---------------------------------------------------------------- stage 10: signs of the storm
def weatherglass():
    """A storm glass: a clear tube on a brass foot with a brass cap, and the liquid inside in four states, from clear to
    full of feathery crystals (the 'storm' look of a real Fitzroy glass)."""
    import math, random
    r = random.Random(1010)
    S = 64
    tube = Image.new('RGBA', (S, S))
    px = tube.load()
    for y in range(S):
        for x in range(S):
            edge = min(x, S - 1 - x)
            a = 70 if edge > 6 else 120 + (6 - edge) * 12
            hl = 1 if 14 <= x <= 18 else 0                                  # a bright vertical highlight
            c = (200 + hl * 50, 235 + hl * 20, 245, min(255, a + hl * 90))
            px[x, y] = c
    save(tube, 'block/weatherglass_tube.png')

    cap = Image.new('RGBA', (S, S))
    px = cap.load()
    for y in range(S):
        for x in range(S):
            band = (y // 8) % 2
            n = r.randint(-8, 8)
            base = (182 + band * 18 + n, 140 + band * 14 + n, 62 + n, 255)       # brass, banded like turned metal
            if y in (0, S - 1) or x in (0, S - 1): base = (110, 82, 36, 255)
            px[x, y] = base
    save(cap, 'block/weatherglass_cap.png')

    def fern(px, cx, cy, length, angle, depth, col):
        """A feathery crystal: a spine with smaller spines off it."""
        for i in range(int(length)):
            x = int(cx + math.cos(angle) * i); y = int(cy - math.sin(angle) * i)
            if 0 <= x < S and 0 <= y < S: px[x, y] = col
            if depth > 0 and i > 2 and i % 4 == 0:
                for side in (-1, 1): fern(px, x, y, length * 0.35, angle + side * 0.9, depth - 1, col)

    for state in range(4):
        img = Image.new('RGBA', (S, S))
        px = img.load()
        cloud = (0, 0, 10, 26)[state]
        for y in range(S):
            for x in range(S):
                k = r.randint(0, 6)
                px[x, y] = (205 - cloud, 228 - cloud, 238, 46 + cloud * 2 + k)   # the liquid, hazier as the storm nears
        col = (246, 250, 255, 235)
        if state >= 1:                                                       # a few crystals settle at the bottom
            for _ in range(3 + state * 2): fern(px, r.randint(8, 56), S - 2, r.randint(6, 10) + state * 3, math.pi / 2 + r.uniform(-0.6, 0.6), 1, col)
        if state >= 2:                                                       # feathers grow up the glass
            for _ in range(4 + state * 3): fern(px, r.randint(6, 58), r.randint(S // 2, S - 4), r.randint(8, 14) + state * 2, math.pi / 2 + r.uniform(-0.8, 0.8), 2, col)
        if state == 3:                                                       # storm: crystals all through, and a few hanging from the top
            for _ in range(10): fern(px, r.randint(6, 58), r.randint(2, 6), r.randint(10, 18), -math.pi / 2 + r.uniform(-0.6, 0.6), 2, col)
            for _ in range(30): px[r.randint(2, 61), r.randint(2, 61)] = (255, 255, 255, 255)
        save(img, f'block/weatherglass_fluid_{state}.png')

    # the item icon: the glass upright, cap and foot, liquid with a few crystals
    icon = Image.new('RGBA', (16, 16))
    p = icon.load()
    for y in range(16):
        for x in range(5, 11):
            if y >= 14 or y <= 1: p[x, y] = (196, 150, 66, 255) if y not in (0, 15) else (120, 88, 38, 255)
            elif x in (5, 10): p[x, y] = (190, 230, 245, 200)
            else: p[x, y] = (205, 228, 238, 120)
    for x, y in ((7, 12), (8, 11), (7, 10), (8, 9), (6, 12), (9, 12), (8, 7)): p[x, y] = (250, 252, 255, 255)
    for y in range(14, 16):
        for x in (4, 11): p[x, y] = (120, 88, 38, 255)
    save(icon, 'item/weatherglass.png')


weatherglass()


def glaze():
    """Clear ice skin: mostly see-through, with frozen drip streaks and a few trapped bubbles."""
    import random
    r = random.Random(1011)
    S = 64
    img = Image.new('RGBA', (S, S))
    px = img.load()
    for y in range(S):
        for x in range(S):
            n = r.randint(-6, 6)
            px[x, y] = (196 + n, 226 + n, 250, 92 + r.randint(0, 10))
    for _ in range(26):                                                      # frozen streaks
        x0, y0, ln = r.randint(0, S - 1), r.randint(0, S - 1), r.randint(6, 18)
        for i in range(ln):
            x, y = (x0 + i // 3) % S, (y0 + i) % S
            px[x, y] = (236, 248, 255, 150)
    for _ in range(14):
        x, y = r.randint(1, S - 2), r.randint(1, S - 2)
        px[x, y] = (255, 255, 255, 190)
    save(img, 'block/glaze.png')


glaze()
