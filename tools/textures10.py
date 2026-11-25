"""Stormreach 5.5 (Live Wire) textures: item icons for the power and water blocks and the Multimeter (32x32, upscaled
by remaster64), and their block textures (64x). Reuses textures7's kit."""
import math
import numpy as np
from textures7 import S, C, ramp, xx, yy, Icon, circle, ellipse, rect, line, thick_path, N, gx64, gy64, save64, noise64

FULL = np.full((N, N), 255.0)


def edge(d):
    return np.minimum(np.minimum(gx64, N - gx64), np.minimum(gy64, N - gy64)) < d


def tone(base, seed, amt=0.18, cells=6):
    return np.array(base, float) * (1 - amt / 2 + amt * noise64(cells, seed)[..., None])


def copper(seed):
    return tone([206, 124, 72], seed, 0.25) + np.clip(1 - np.abs(gx64 - 20) / 8, 0, 1)[..., None] * 30


# ---------------------------------------------------------------------------------------------------- blocks
def blocks():
    # Copper Cable: copper core in a dark rubber sleeve with a yellow tracer stripe
    rgb = tone([46, 48, 54], 60, 0.2)
    core = np.abs(gy64 - 32) < 10
    rgb = np.where(core[..., None], copper(61), rgb)
    rgb = np.where((np.abs(gy64 - 32) < 2)[..., None], np.array([240, 200, 70.0]), rgb)
    rgb = np.where(((gx64 % 16) < 2)[..., None] & ~core[..., None], rgb * 0.75, rgb)
    save64(rgb, FULL, 'block/copper_cable')
    # Storm Capacitor: steel base, glass jar (dark and lit)
    base = tone([86, 92, 102], 62)
    base = np.where(((gy64 % 21) < 3)[..., None], np.array([60, 64, 72.0]), base)
    base = np.where(edge(2)[..., None], base * 0.7, base)
    save64(base, FULL, 'block/storm_capacitor')
    for lit in (False, True):
        g = tone([150, 176, 196] if not lit else [190, 236, 255], 63 + lit, 0.15)
        band = (gy64 > 40) & (gy64 < 56)
        g = np.where(band[..., None], copper(64), g)
        hl = (np.abs(gx64 - 16) < 3)
        g = np.where(hl[..., None] & ~band[..., None], np.minimum(255, g + 50), g)
        if lit:
            arc = np.abs(gy64 - (20 + 6 * np.sin(gx64 * 0.4))) < 1.5
            g = np.where(arc[..., None], np.array([255, 255, 255.0]), g)
        save64(g, FULL, f"block/storm_capacitor_jar{'_lit' if lit else ''}")
    # Water Wheel hub: oak with iron bands
    hub = tone([150, 104, 62], 65, 0.25)
    hub = np.where(((gx64 % 32) < 4)[..., None], np.array([120, 126, 134.0]), hub)
    save64(hub, FULL, 'block/water_wheel')
    # Arc Lamp: cage bars and a bulb, off and on
    cage = tone([70, 74, 82], 66)
    bars = ((gx64 % 16) < 3) | ((gy64 % 16) < 3)
    for on in (False, True):
        bulb = np.array([255, 250, 220.0]) if on else np.array([120, 128, 140.0])
        img = np.where(bars[..., None], cage, bulb * (0.9 + 0.1 * noise64(4, 67)[..., None]))
        if on: img = np.where((np.hypot(gx64 - 32, gy64 - 32) < 12)[..., None], np.array([255, 255, 255.0]), img)
        save64(img, FULL, f"block/arc_lamp_{'on' if on else 'off'}")
    # Shock Plate: a copper grid on dark steel with hazard corners
    plate = tone([54, 58, 64], 68)
    grid = ((gx64 % 8) < 2) | ((gy64 % 8) < 2)
    plate = np.where(grid[..., None], copper(69), plate)
    corner = ((gx64 < 10) & (gy64 < 10)) | ((gx64 > 54) & (gy64 > 54))
    plate = np.where((corner & ((gx64 + gy64) // 4 % 2 == 0))[..., None], np.array([240, 176, 40.0]), plate)
    save64(plate, FULL, 'block/shock_plate')
    # Electric Pump: steel housing with a copper band and a pressure gauge; top with an intake grate
    side = tone([96, 104, 114], 70)
    side = np.where(((gy64 > 26) & (gy64 < 34))[..., None], copper(71), side)
    gauge = np.hypot(gx64 - 46, gy64 - 14) < 7
    side = np.where(gauge[..., None], np.array([236, 232, 220.0]), side)
    side = np.where(((np.abs(np.arctan2(gy64 - 14, gx64 - 46) + 2.3) < 0.15) & gauge)[..., None], np.array([200, 40, 30.0]), side)
    side = np.where(edge(2)[..., None], side * 0.7, side)
    save64(side, FULL, 'block/electric_pump')
    top = tone([96, 104, 114], 72)
    top = np.where(((np.hypot(gx64 - 32, gy64 - 32) < 20) & ((gx64 % 6) < 2))[..., None], np.array([40, 44, 50.0]), top)
    top = np.where(edge(2)[..., None], top * 0.7, top)
    save64(top, FULL, 'block/electric_pump_top')
    # Sprinkler: brass head
    head = tone([212, 170, 86], 73, 0.25)
    head = np.where(((gy64 % 12) < 2)[..., None], head * 0.7, head)
    save64(head, FULL, 'block/sprinkler')
    # Rain Barrel: oak staves with iron hoops; the top rim
    staves = tone([142, 98, 56], 74, 0.2) * (0.92 + 0.08 * ((gx64 // 8) % 2))[..., None]
    staves = np.where(((gx64 % 8) < 1)[..., None], staves * 0.7, staves)
    hoops = (np.abs(gy64 - 10) < 3) | (np.abs(gy64 - 54) < 3)
    staves = np.where(hoops[..., None], np.array([92, 98, 106.0]), staves)
    save64(staves, FULL, 'block/rain_barrel')
    inner = tone([110, 76, 44], 75, 0.2)
    save64(inner, FULL, 'block/rain_barrel_inside')
    # captured water and Stormwater surfaces
    w = noise64(5, 76)
    water = np.array([52, 98, 196.0]) + (w[..., None] - 0.5) * 50
    water = np.where((np.sin(gx64 * 0.3 + gy64 * 0.2) > 0.92)[..., None], np.array([190, 220, 255.0]), water)
    save64(water, FULL, 'block/tank_water')
    storm = np.array([70, 76, 180.0]) + (w[..., None] - 0.5) * 50
    sparks = np.random.default_rng(77).random((N, N)) < 0.012
    storm = np.where(sparks[..., None], np.array([220, 250, 255.0]), storm)
    save64(storm, FULL, 'block/tank_stormwater')
    # Cistern: dressed stone with a copper spout band
    stone = tone([128, 130, 136], 78)
    mortar = (gy64 % 16 < 1.5) | (((gx64 + (gy64 // 16) % 2 * 16) % 32) < 1.5)
    stone = np.where(mortar[..., None], stone * 0.7, stone)
    save64(stone, FULL, 'block/cistern')
    topc = tone([128, 130, 136], 79)
    topc = np.where((~edge(8))[..., None], np.array([60, 62, 68.0]), topc)
    topc = np.where((~edge(8) & ((gx64 % 8) < 2))[..., None], np.array([92, 98, 106.0]), topc)
    save64(topc, FULL, 'block/cistern_top')


# ---------------------------------------------------------------------------------------------------- icons
def icons():
    i = Icon(); i.shape(thick_path([(3, 22), (12, 18), (20, 18), (29, 10)], 2.4), C('#2e3036'), bump=0.6)
    i.paint(thick_path([(3, 22), (12, 18), (20, 18), (29, 10)], 0.6), C('#f0c846'))
    for x, y in ((3, 22), (29, 10)): i.shape(circle(x, y, 1.6), C('#c87a3a'), bump=0.5)
    i.save('item/copper_cable')
    i = Icon(); i.shape(rect(4, 24, 28, 29), C('#5a606a'), bump=0.4)
    for x in (6, 13, 20):
        i.shape(rect(x, 9, x + 6, 24), C('#a8dcf4'), alpha=235, bump=0.8)
        i.paint(rect(x, 18, x + 6, 21), C('#c87a3a')); i.paint(rect(x + 2.5, 5, x + 3.5, 9), C('#8a929a'))
    i.paint(line([(9, 12), (11, 14), (9, 16)], 0.35), C('#ffffff'))
    i.save('item/storm_capacitor')
    i = Icon()
    rim = (np.abs(np.hypot(xx - 16, yy - 16) - 12) < 1.6)
    i.shape(rim, C('#6a4628'), bump=0.4)
    for k in range(8):
        a = k * math.pi / 4
        i.shape(thick_path([(16 + math.cos(a) * 3, 16 + math.sin(a) * 3), (16 + math.cos(a) * 14, 16 + math.sin(a) * 14)], 1.0), C('#9a6a3e'), bump=0.4)
    i.shape(circle(16, 16, 3), C('#8a929a'), bump=0.8)
    i.save('item/water_wheel')
    i = Icon(); i.shape(rect(11, 6, 21, 26), C('#4a5058'), bump=0.4)
    i.paint(rect(12.5, 8, 19.5, 24), C('#fffbe0')); i.paint(rect(14, 11, 18, 21), C('#ffffff'))
    for y in (11, 16, 21): i.paint(rect(11, y, 21, y + 1), C('#4a5058'))
    i.save('item/arc_lamp')
    i = Icon(); i.shape(rect(3, 19, 29, 25), C('#3a3e44'), bump=0.3)
    i.paint(rect(4, 20, 28, 24) & ((xx.astype(int) % 4 < 1) | (yy.astype(int) % 2 < 1)), C('#c87a3a'))
    i.paint(line([(14, 4), (11, 11), (16, 11), (13, 18)], 0.6), C('#bff4ff'))
    i.save('item/shock_plate')
    i = Icon(); i.shape(rect(7, 6, 25, 26), C('#606872'), bump=0.5)
    i.paint(rect(7, 14, 25, 17), C('#c87a3a')); i.paint(circle(20, 10, 2.6), C('#ece8dc')); i.paint(line([(20, 10), (18.5, 8.5)], 0.3), C('#c02820'))
    i.shape(rect(14, 26, 18, 30), C('#8a929a'), bump=0.4); i.paint(rect(10, 28, 22, 30), C('#4a8ad8'))
    i.save('item/electric_pump')
    i = Icon(); i.shape(rect(14, 14, 18, 28), C('#8a929a'), bump=0.4); i.shape(ellipse(16, 12, 6, 3), C('#d4aa56'), bump=0.8)
    for k in range(7):
        a = math.pi + k * math.pi / 6
        i.paint(circle(16 + math.cos(a) * 11, 10 + math.sin(a) * 6, 0.9), C('#6ab8f0'))
    i.save('item/sprinkler')
    i = Icon(); body = rect(8, 6, 24, 28) | ellipse(16, 17, 9.5, 11) & rect(6, 6, 26, 28)
    i.shape(body, C('#8e623a'), pattern=0.06 * ((xx.astype(int) // 3) % 2), bump=0.6)
    for y in (9, 24): i.paint(rect(6, y, 26, y + 2) & body, C('#5c626a'))
    i.paint(ellipse(16, 6.5, 7, 1.6), C('#4a8ad8'))
    i.save('item/rain_barrel')
    i = Icon(); i.shape(rect(5, 7, 27, 28), C('#808288'), bump=0.4)
    i.paint(rect(5, 7, 27, 28) & ((yy.astype(int) % 6 == 0) | ((xx.astype(int) + (yy.astype(int) // 6) % 2 * 5) % 10 == 0)), C('#5a5c62'))
    i.paint(rect(8, 4, 24, 8), C('#4a8ad8')); i.shape(rect(26, 20, 30, 23), C('#c87a3a'), bump=0.3)
    i.save('item/cistern')
    i = Icon(); i.shape(rect(9, 4, 23, 27), C('#e0a020'), bump=0.5)
    i.paint(rect(11, 6, 21, 13), C('#1c2a24')); i.paint(rect(12, 8, 20, 11), C('#7ff0a0'))
    i.paint(circle(16, 19, 3.2), C('#3a3e44')); i.paint(line([(16, 19), (17.5, 17)], 0.35), C('#ffffff'))
    for x, col in ((6, '#c02820'), (26, '#2a2a2c')):
        i.paint(line([(x, 28), (x, 22), (12 if x < 16 else 20, 26)], 0.5), C(col))
    i.save('item/multimeter')


def main():
    blocks(); icons()
    print('textures10: 5.5 power and water textures painted')


if __name__ == '__main__':
    main()
