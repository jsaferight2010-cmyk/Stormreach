"""Stormreach 5.4 (Thunderfront) textures: item icons (32x32, upscaled to 64x by remaster64), spawn eggs, and the Tesla
Coil and Wind Chimes block textures (drawn at 64x). Mob textures come from mobmodels.py. Reuses textures7's kit."""
import math
import numpy as np
from textures7 import (S, C, ramp, xx, yy, Icon, circle, ellipse, rect, line, thick_path, bottle, egg, N, gx64, gy64, save64, noise64)

rng = np.random.default_rng(54)


def umbrella():
    i = Icon()
    # a storm-blue canopy, scalloped along the rim, on a curved-handle shaft
    canopy = (((xx - 16) / 13) ** 2 + ((yy - 14) / 10) ** 2 <= 1) & (yy <= 14)
    for k in range(4):
        canopy &= ~circle(6.5 + k * 6.3, 15.6, 2.6)
    ribs = (np.abs(np.arctan2(yy - 14, xx - 16) + math.pi / 2) % 0.62 < 0.08)
    i.shape(canopy, C('#3a6ac0'), pattern=-0.12 * ribs, bump=0.8)
    i.paint(canopy & (np.arange(S)[None, :] // 7 % 2 == 0) & (yy < 9), ramp(C('#3a6ac0'))[4])
    i.paint(circle(16, 3.6, 1.1), C('#c8d0d8'))
    shaft = line([(16, 12), (16, 26)], 0.55)
    i.paint(shaft & ~canopy, C('#8a929a'))
    hook = (np.abs(np.hypot(xx - 13.5, yy - 26) - 2.5) < 0.7) & (yy >= 26)
    i.shape(hook | (line([(16, 22), (16, 26)], 0.7)), C('#6a4a2a'), bump=0.4)
    i.save('item/umbrella')


def omen_feather():
    i = Icon()
    t = np.linspace(0, 1, 48)
    spine = [(6 + 19 * s, 28 - 23 * s + 2.5 * math.sin(s * 3)) for s in t]
    vane = np.zeros((S, S), bool)
    for s_, (x, y) in zip(t, spine):
        if s_ < 0.12: continue
        vane |= circle(x, y, 0.8 + 4.2 * math.sin(math.pi * min(1.0, (s_ - 0.12) / 0.88)) ** 0.6)
    barbs = np.sin((xx + yy) * 1.3) * 0.08
    i.shape(vane, C('#34313a'), pattern=barbs, bump=0.7)
    i.paint(vane & ((xx + yy).astype(int) % 4 == 0), ramp(C('#34313a'))[1])
    i.paint(vane & (yy < 13) & (xx > 17), C('#eeeeee'))
    i.paint(vane & (yy < 13) & (xx > 17) & ((xx + yy).astype(int) % 4 == 0), C('#c8c8c8'))
    i.paint(thick_path(spine, 0.45), C('#d8d0c0'))
    for (sx, sy) in ((26, 15), (21, 6), (11, 15)):
        i.paint(circle(sx, sy, 0.9), C('#9fe8ff'))
    i.save('item/omen_feather')


def bottled_dust_devil():
    i = Icon()
    liq = bottle(i, C('#d8bc84'), alpha=170)
    r = np.hypot(xx - 16, yy - 21)
    funnel = liq & (np.abs(xx - 16 - np.sin(yy * 0.9) * 0.8) < (yy - 13) * 0.42) & (yy > 14)
    i.paint(funnel, C('#b0905a'), 230)
    i.paint(funnel & ((yy.astype(int) + xx.astype(int)) % 3 == 0), C('#e6d2a0'), 240)
    i.save('item/bottled_dust_devil')


def tesla_icon():
    i = Icon()
    i.shape(rect(9, 25, 23, 29), C('#4a5058'), bump=0.4)
    i.shape(rect(13, 12, 19, 25), C('#c87a3a'), bump=0.5)
    i.paint(rect(13, 12, 19, 25) & (yy.astype(int) % 2 == 0), ramp(C('#c87a3a'))[1])
    i.shape(ellipse(16, 10, 10, 4), C('#c8d0d8'), bump=1.0)
    i.paint(ellipse(16, 10, 5, 1.6), ramp(C('#c8d0d8'))[1])
    for pts in (((6, 8), (3, 5), (5, 3), (2, 1)), ((26, 8), (29, 5), (27, 3), (30, 1))):
        i.paint(line(list(pts), 0.4), C('#bff4ff'))
    i.save('item/tesla_coil')


def chimes_icon():
    i = Icon()
    i.shape(ellipse(16, 6, 8, 2.2), C('#8a5a34'), bump=0.6)
    i.paint(line([(16, 1), (16, 4)], 0.4), C('#8a929a'))
    for k, (x, ln) in enumerate(((9, 15), (12.5, 19), (16, 22), (19.5, 18), (23, 14))):
        i.shape(rect(x - 1, 9, x + 1.2, 9 + ln), C('#c8a050'), bump=0.6)
        i.paint(line([(x, 7), (x, 9)], 0.25), C('#6a7078'))
    i.shape(circle(16, 18, 1.8), C('#8a5a34'), bump=0.5)
    i.save('item/wind_chimes')


# ---------------------------------------------------------------------------------------------------- blocks, 64x
def copper(seed, oxid=0.0):
    n = noise64(6, seed)
    base = np.array([200, 120, 70.0]) * (0.85 + 0.25 * n[..., None])
    green = np.array([90, 170, 140.0])
    patina = np.clip((noise64(4, seed + 1) - (1 - oxid)) * 4, 0, 1)
    return base * (1 - patina[..., None]) + green * patina[..., None]


def tesla_blocks():
    # the coil: copper wire wound in tight turns, with a highlight running down it
    rgb = copper(30, 0.15)
    turns = (gy64 % 4) < 1.2
    rgb = np.where(turns[..., None], rgb * 0.62, rgb)
    rgb = rgb + np.clip(1 - np.abs(gx64 - 22) / 6, 0, 1)[..., None] * 40
    save64(rgb, np.full((N, N), 255.0), 'block/tesla_coil')
    # the toroid: polished steel with a soft ring highlight
    n = noise64(5, 31)
    steel = np.array([190, 198, 206.0]) * (0.9 + 0.12 * n[..., None])
    ring = np.abs(np.hypot(gx64 - 32, gy64 - 32) - 20) < 5
    steel = np.where(ring[..., None], np.minimum(255, steel * 1.15), steel)
    steel = np.where((np.hypot(gx64 - 32, gy64 - 32) < 9)[..., None], steel * 0.7, steel)
    save64(steel, np.full((N, N), 255.0), 'block/tesla_coil_top')
    # the base: dark insulator with a copper band and ceramic rings
    base = np.array([58, 62, 70.0]) * (0.9 + 0.15 * noise64(5, 32)[..., None])
    base = np.where(((gy64 > 24) & (gy64 < 32))[..., None], copper(33)[..., :], base)
    base = np.where(((gy64 > 40) & (gy64 % 8 < 3))[..., None], np.array([226, 222, 210.0]), base)
    base = np.where((np.minimum(np.minimum(gx64, N - gx64), np.minimum(gy64, N - gy64)) < 2)[..., None], base * 0.7, base)
    save64(base, np.full((N, N), 255.0), 'block/tesla_coil_base')
    # charged: the coil glowing blue-white between the turns
    glow = copper(30, 0.15)
    glow = np.where(turns[..., None], np.array([190, 244, 255.0]), glow * 0.9)
    save64(glow, np.full((N, N), 255.0), 'block/tesla_coil_charged')


def chimes_blocks():
    brass = np.array([214, 172, 90.0]) * (0.88 + 0.2 * noise64(6, 40)[..., None])
    brass = brass + np.clip(1 - np.abs(gx64 % 16 - 5) / 3, 0, 1)[..., None] * 45
    save64(brass, np.full((N, N), 255.0), 'block/wind_chimes')
    wood = np.array([138, 90, 52.0]) * (0.85 + 0.25 * noise64(3, 41)[..., None])
    rings = np.sin(np.hypot(gx64 - 32, gy64 - 32) * 0.7) * 0.5 + 0.5
    wood = wood * (0.9 + 0.12 * rings[..., None])
    save64(wood, np.full((N, N), 255.0), 'block/wind_chimes_top')


def main():
    umbrella(); omen_feather(); bottled_dust_devil(); tesla_icon(); chimes_icon()
    for k, (name, b, s) in enumerate((('rainfrog', '#3fae8a', '#f2c94c'), ('storm_petrel', '#2c2a30', '#f2f2f2'), ('dust_devil', '#d8bc84', '#9a7a48'))):
        egg(name, b, s, 540 + k)
    tesla_blocks(); chimes_blocks()
    print('textures9: 5.4 items, eggs and blocks painted')


if __name__ == '__main__':
    main()
