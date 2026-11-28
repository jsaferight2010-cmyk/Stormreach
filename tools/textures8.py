"""Stormreach 5.3 (Skyward) textures: item icons (32x32 pixel art, shaded on a colour ramp with a dark outline, then
upscaled to 64x by remaster64), spawn eggs, the Stormchaser suit's armour layers, and the new blocks (drawn directly at
64x, which remaster64 leaves alone). Mob textures come from mobmodels.py. Reuses the drawing kit in textures7.py."""
import math
import numpy as np
from PIL import Image
from scipy.ndimage import binary_dilation
from textures7 import (TEX, S, C, ramp, xx, yy, Icon, circle, ellipse, rect, line, thick_path, bottle, egg,
                       N, gx64, gy64, save64, noise64)

rng = np.random.default_rng(53)


# ---------------------------------------------------------------------------------------------------- materials
def serpent_scale():
    i = Icon()
    m = (((xx - 16) / 8.5) ** 2 + ((yy - 13) / np.where(yy < 13, 9.5, 14)) ** 2 <= 1)
    ridges = 0.08 * np.cos((yy - 4) * 0.9) * (np.abs(xx - 16) < 6)
    i.shape(m, C('#3c4c80'), pattern=ridges, bump=1.1)
    i.paint(m & binary_dilation(~m, iterations=2) & (yy > 14), C('#9fe8ff'))
    i.paint(line([(16, 6), (16, 22)], 0.45) & m, ramp(C('#55689e'))[4])
    i.paint(ellipse(12.5, 10, 1.3, 2.6, 0.3) & m, C('#e8f4ff'))
    i.save('item/serpent_scale')


def squall_heart():
    i = Icon()
    heart = (circle(11.5, 12, 6) | circle(20.5, 12, 6) | ((yy >= 12) & (np.abs(xx - 16) < (27 - yy) * 0.86)))
    r = np.hypot(xx - 16, yy - 15); th = np.arctan2(yy - 15, xx - 16)
    swirl = 0.12 * np.sin(th * 3 + r * 0.8)
    i.shape(heart, C('#7a56d8'), pattern=swirl, bump=1.2)
    spiral = (np.abs(((th + r * 0.5) % math.pi) - 1.3) < 0.28) & heart & (r > 1.5) & (r < 9)
    i.paint(spiral, C('#b8f0ff'), 255)
    i.paint(circle(16, 15, 2.2), C('#ffffff'))
    i.paint(ellipse(10.5, 9.5, 1.3, 2.2, 0.6), C('#e8dcff'))
    i.save('item/squall_heart')


def glacial_core():
    i = Icon()
    gem = (np.abs(xx - 16) / 10 + np.abs(yy - 16) / 12) <= 1
    facets = 0.1 * np.sign(np.sin((xx - 16) * 0.45)) * np.sign(np.sin((yy - 16) * 0.4))
    i.shape(gem, C('#a8daf4'), pattern=facets, bump=0.9, alpha=235)
    i.paint(line([(16, 4), (16, 28)], 0.35) | line([(6, 16), (26, 16)], 0.35), C('#e8f8ff'), 240)
    glow = circle(16, 16, 3.5)
    i.paint(glow, C('#effcff'))
    i.paint(circle(16, 16, 1.6), C('#ffffff'))
    i.paint(ellipse(12, 11, 1.2, 2.4, 0.6) & gem, C('#ffffff'))
    i.save('item/glacial_core')


def aurora_dust():
    i = Icon()
    pile = ellipse(16, 22, 11, 6) | ellipse(16, 18, 7, 5)
    g = np.clip((xx - 5) / 22, 0, 1)
    i.shape(pile, C('#6ad8b8'), bump=0.8)
    cols = [ramp(C('#7ff0c8'))[4], ramp(C('#b07aff'))[4], ramp(C('#ff92dc'))[4]]
    band = (np.floor(g * 3 + 0.2 * np.sin(yy * 0.9))).clip(0, 2).astype(int)
    for k in range(3):
        i.paint(pile & (band == k) & ~binary_dilation(~pile, iterations=1), cols[k] * 0.9)
    sparks = pile & (rng.random((S, S)) < 0.12)
    i.paint(sparks, C('#ffffff'))
    for (sx, sy) in ((9, 9), (23, 8), (17, 5)):
        i.paint(line([(sx - 1.5, sy), (sx + 1.5, sy)], 0.3) | line([(sx, sy - 1.5), (sx, sy + 1.5)], 0.3), C('#e8fff4'))
    i.save('item/aurora_dust')


def bottled_stormfly():
    i = Icon()
    liq = bottle(i, C('#1c2a3c'), alpha=200)
    for (fx, fy) in ((13, 19), (18, 23), (16, 17), (19, 19), (14, 24)):
        i.paint(circle(fx, fy, 1.4) & liq, C('#fbffb0'))
        i.paint(binary_dilation(circle(fx, fy, 1.4), iterations=1) & ~circle(fx, fy, 1.4) & liq, C('#8ad0e0'), 230)
    i.save('item/bottled_stormfly')


def bottled_lightning():
    i = Icon()
    liq = bottle(i, C('#3a2a6a'), alpha=220)
    bolt = line([(18, 14), (14, 20), (18, 21), (14, 28)], 0.65) & liq
    i.paint(binary_dilation(bolt) & liq, C('#9fe8ff'))
    i.paint(bolt, C('#ffffff'))
    i.save('item/bottled_lightning')


# ---------------------------------------------------------------------------------------------------- tools
def weather_radio():
    i = Icon()
    body = rect(5, 11, 27, 27)
    i.shape(body, C('#e8e4da'), bump=0.4)
    i.paint(rect(5, 23, 27, 27), ramp(C('#e07a2a'))[3])
    grille = rect(7, 13, 16, 22) & ((xx.astype(int) + yy.astype(int)) % 2 == 0)
    i.paint(rect(7, 13, 16, 22), C('#3a4048')); i.paint(grille, C('#6a7078'))
    i.paint(rect(18, 13, 25, 17), C('#1c2a24'))
    i.paint(rect(19, 14, 24, 16), C('#7ff0a0'))
    i.paint(circle(20, 20, 1.5), C('#e04a3a')); i.paint(circle(23.5, 20, 1.5), C('#8a929a'))
    i.paint(line([(24, 11), (28, 2)], 0.5), C('#8a929a'))
    i.paint(circle(28, 2, 1.1), C('#e04a3a'))
    i.save('item/weather_radio')


def seeder(name, body, cone, accent, bolt=False):
    i = Icon()
    ang = math.pi / 4                                  # long axis pointing up and to the right
    ax, ay = math.cos(ang), -math.sin(ang)
    cx, cy = 15, 17
    rocket = ellipse(cx, cy, 3.6, 9, ang)
    nose = rocket & ((xx - cx) * ax + (yy - cy) * ay > 4.5)
    tail = (xx - cx) * ax + (yy - cy) * ay < -6.5
    puff = (circle(6, 27, 3.2) | circle(3.5, 24.5, 2.3) | circle(9.5, 29.5, 2.1)) & ~rocket
    i.shape(puff, C('#e8eef4'), bump=0.6, alpha=235)
    tx, ty = cx - ax * 7, cy - ay * 7
    px, py = -ay, ax
    fins = line([(tx, ty), (tx + px * 4.5 - ax * 2.5, ty + py * 4.5 - ay * 2.5)], 1.0) | line([(tx, ty), (tx - px * 4.5 - ax * 2.5, ty - py * 4.5 - ay * 2.5)], 1.0)
    i.shape(fins & ~rocket, C(accent), bump=0.3)
    i.shape(rocket, C(body), bump=0.7)
    i.paint(nose, ramp(C(cone))[3]); i.paint(nose & ((xx - cx) * ax + (yy - cy) * ay > 7), ramp(C(cone))[4])
    i.paint(rocket & tail, ramp(C(accent))[2])
    i.paint(rocket & (np.abs((xx - cx) * ax + (yy - cy) * ay - 2.5) < 0.6), ramp(C(accent))[3])
    i.paint(circle(cx + ax * 0.5 - px * 1.2, cy + ay * 0.5 - py * 1.2, 1.1) & rocket, C('#bfe8ff'))
    if bolt:
        b = line([(cx - 1.0, cy + 2.5), (cx + 0.6, cy + 0.2), (cx - 0.6, cy), (cx + 1.2, cy - 2.4)], 0.4) & rocket
        i.paint(b, C('#ffe25a'))
    i.save(f'item/{name}')


def gale_blade():
    i = Icon()
    blade = thick_path([(9, 23), (26, 6)], 1.9) | thick_path([(24, 8), (28, 4)], 1.0)
    i.shape(blade, C('#bfe8f8'), bump=0.6, alpha=245)
    edge = line([(10, 21), (27, 4.5)], 0.35) & blade
    i.paint(edge, C('#ffffff'))
    i.paint(line([(11, 22.5), (24, 9.5)], 0.35) & blade, C('#6ab8e0'))
    guard = line([(6, 19), (13, 26)], 1.3)
    i.shape(guard, C('#7a56d8'), bump=0.5)
    i.paint(circle(9.5, 22.5, 1.7), C('#b8f0ff')); i.paint(circle(9.5, 22.5, 0.8), C('#ffffff'))
    grip = thick_path([(8, 24), (4, 28)], 1.1)
    i.shape(grip & ~guard, C('#4a3a2a'), bump=0.4)
    i.paint(grip & ~guard & ((xx.astype(int) + yy.astype(int)) % 3 == 0), C('#8a6a4a'))
    i.shape(circle(3.2, 28.8, 1.6), C('#7a56d8'), bump=0.5)
    # wind ribbons curling round the blade
    for k, (a, b) in enumerate(((14, 13), (19, 9))):
        arc = (np.abs(np.hypot(xx - a, yy - b) - 4) < 0.45) & (yy < b + 1) & ~blade
        i.paint(arc, C('#e8f8ff'), 210)
    i.save('item/gale_blade')


# ---------------------------------------------------------------------------------------------------- food
def chasers_coffee():
    i = Icon()
    liq = bottle(i, C('#5a3418'), alpha=245)
    i.paint(liq & (yy < 17), C('#c89a68'))
    for k, x0 in enumerate((13, 17)):
        steam = np.zeros((S, S), bool)
        for t in np.linspace(0, 1, 16):
            steam |= circle(x0 + 1.4 * math.sin(t * 6 + k), 5 - t * 4.5, 0.55)
        i.paint(steam & ~rect(12, 3, 20, 8), C('#f2f2f2'), 200)
    i.paint(rect(13, 22, 19, 25), C('#e07a2a'))
    i.save('item/chasers_coffee')


def snow_cone():
    i = Icon()
    cone = (yy > 14) & (np.abs(xx - 16) < (30 - yy) * 0.45)
    i.shape(cone, C('#e8d8b0'), pattern=0.06 * ((xx.astype(int) + yy.astype(int)) % 4 < 2), bump=0.4)
    ice = circle(16, 12, 7.5) & (yy < 16.5)
    i.shape(ice, C('#dff2ff'), bump=1.0)
    syrup = ice & (np.hypot(xx - 13, yy - 9) < 4.5)
    i.paint(syrup & ~binary_dilation(~ice), ramp(C('#5aa8f0'))[3])
    i.paint(ice & (rng.random((S, S)) < 0.18), C('#ffffff'))
    i.save('item/snow_cone')


# ---------------------------------------------------------------------------------------------------- the Stormchaser suit
ORANGE, CHAR, REFL, RUBBER = C('#e07a2a'), C('#3a3f46'), C('#dfe6ec'), C('#2a2c30')


def goggles_icon():
    i = Icon()
    strap = rect(3, 14, 29, 18)
    i.shape(strap, CHAR, bump=0.3)
    for cx in (11, 21):
        i.shape(circle(cx, 16, 5.5), C('#8a929a'), bump=0.8)
        i.shape(circle(cx, 16, 4), C('#4ac0e8'), bump=1.0, outline=False)
        i.paint(ellipse(cx - 1.5, 14.2, 1.1, 1.8, 0.5), C('#e8fbff'))
    i.paint(rect(15.5, 15, 16.5, 17), C('#8a929a'))
    i.save('item/stormchaser_goggles')


def jacket_icon():
    i = Icon()
    torso = rect(9, 6, 23, 27)
    arms = rect(3, 7, 9, 23) | rect(23, 7, 29, 23)
    i.shape(torso | arms, ORANGE, bump=0.4)
    i.paint(rect(9, 6, 23, 10), ramp(CHAR)[3]); i.paint(rect(3, 7, 9, 10) | rect(23, 7, 29, 10), ramp(CHAR)[3])
    i.paint(rect(9, 18, 23, 20) | rect(3, 18, 9, 20) | rect(23, 18, 29, 20), REFL)
    i.paint(rect(15.5, 6, 16.5, 27), ramp(CHAR)[1])
    i.paint(rect(11, 21, 14, 24) | rect(18, 21, 21, 24), ramp(ORANGE)[2])
    i.paint(rect(13, 4, 19, 7), ramp(CHAR)[2])
    i.save('item/stormchaser_jacket')


def trousers_icon():
    i = Icon()
    legs = rect(9, 5, 23, 10) | rect(9, 10, 15.5, 29) | rect(16.5, 10, 23, 29)
    i.shape(legs, CHAR, bump=0.4)
    i.paint(rect(9, 5, 23, 7), ramp(CHAR)[1])
    i.paint(rect(10, 17, 15, 20) | rect(17, 17, 22, 20), ramp(ORANGE)[3])
    i.paint(rect(9, 24, 15.5, 25) | rect(16.5, 24, 23, 25), REFL)
    i.save('item/stormchaser_trousers')


def boots_icon():
    i = Icon()
    for x0 in (5, 18):
        b = rect(x0, 10, x0 + 7, 24) | rect(x0, 20, x0 + 10, 27)
        i.shape(b, RUBBER, bump=0.4)
        i.paint(rect(x0, 25, x0 + 10, 27), ORANGE)
        i.paint(rect(x0, 13, x0 + 7, 14.5), REFL)
        i.paint(rect(x0, 10, x0 + 7, 11.5), ramp(ORANGE)[3])
    i.save('item/stormchaser_boots')


def cube_uv(img, a, u, v, w, h, d, faces):
    """Paints a Minecraft cube UV unwrap: faces maps 'top','bottom','right','front','left','back' to a painter f(w,h)->(rgb,alpha)."""
    rects = {'top': (u + d, v, w, d), 'bottom': (u + d + w, v, w, d), 'right': (u, v + d, d, h), 'front': (u + d, v + d, w, h),
             'left': (u + d + w, v + d, d, h), 'back': (u + 2 * d + w, v + d, w, h)}
    for k, (x0, y0, fw, fh) in rects.items():
        if k not in faces or fw <= 0 or fh <= 0: continue
        rgb, al = faces[k](fw, fh)
        img[y0:y0 + fh, x0:x0 + fw] = rgb; a[y0:y0 + fh, x0:x0 + fw] = al


def fabric(base, fw, fh, seed, ripstop=True):
    r = np.random.default_rng(seed)
    yy_, xx_ = np.mgrid[0:fh, 0:fw]
    R = ramp(base, 7)
    v = 3.6 + r.normal(0, 0.45, (fh, fw)) + (0.6 if ripstop else 0) * (((xx_ % 4) == 0) | ((yy_ % 4) == 0)) * -1
    v[0, :] += 0.8; v[-1, :] -= 0.9
    return R[np.clip(np.round(v), 0, 6).astype(int)]


def armour_layers():
    # humanoid: goggles (a strap round the head with two lenses), jacket (body + arms) and boots (lower legs)
    img = np.zeros((32, 64, 3)); a = np.zeros((32, 64))
    def strap(fw, fh, lenses=False):
        rgb = np.zeros((fh, fw, 3)); al = np.zeros((fh, fw))
        band = slice(2, 5)
        rgb[band] = ramp(CHAR)[2]; al[band] = 255
        rgb[2] = ramp(CHAR)[4]
        if lenses:
            for cx in (1, fw - 3):
                rgb[1:6, cx:cx + 3] = ramp(C('#8a929a'))[3]; al[1:6, cx:cx + 3] = 255
                rgb[2:5, cx + 0:cx + 2] = C('#4ac0e8')[:3]; rgb[2, cx] = (232, 251, 255)
        return rgb, al
    cube_uv(img, a, 0, 0, 8, 8, 8, {'front': lambda w_, h_: strap(w_, h_, True), 'right': strap, 'left': strap, 'back': strap})
    def jacket(fw, fh, face, arm=False, seed=0):
        rgb = fabric(ORANGE, fw, fh, seed)
        if face in ('top', 'bottom') or fh < 10: return rgb, np.full((fh, fw), 255.0)
        rgb[:3] = fabric(CHAR, fw, 3, seed + 1)                        # charcoal yoke over the shoulders
        rgb[7] = REFL[:3]; rgb[8] = REFL[:3] * 0.82                   # reflective tape
        if arm: rgb[-2:] = fabric(CHAR, fw, 2, seed + 2)              # cuffs
        if face == 'front' and not arm:
            rgb[:, fw // 2] = ramp(CHAR)[1]                           # zip
            rgb[9:11, 1:3] = ramp(ORANGE)[2]; rgb[9:11, fw - 3:fw - 1] = ramp(ORANGE)[2]   # pocket flaps
            rgb[3:5, 1:3] = C('#f2c94c')[:3]                          # chaser's patch
        return rgb, np.full((fh, fw), 255.0)
    cube_uv(img, a, 16, 16, 8, 12, 4, {k: (lambda k: lambda w_, h_: jacket(w_, h_, k, seed=11))(k) for k in ('top', 'bottom', 'right', 'front', 'left', 'back')})
    cube_uv(img, a, 40, 16, 4, 12, 4, {k: (lambda k: lambda w_, h_: jacket(w_, h_, k, arm=True, seed=12))(k) for k in ('top', 'bottom', 'right', 'front', 'left', 'back')})
    def boot(fw, fh, face):
        rgb = np.zeros((fh, fw, 3)); al = np.zeros((fh, fw))
        if face in ('top',): return rgb, al
        rgb[:] = fabric(RUBBER, fw, fh, 21, ripstop=False); al[:] = 0
        if face == 'bottom': al[:] = 255; rgb[:] = ramp(ORANGE)[2]; return rgb, al
        top = fh - 6                                                   # boots cover the lowest 6 pixels of the leg
        al[top:] = 255
        rgb[top] = ramp(ORANGE)[4]; rgb[top + 2] = REFL[:3]
        rgb[-1] = ramp(ORANGE)[2]
        return rgb, al
    cube_uv(img, a, 0, 16, 4, 12, 4, {k: (lambda k: lambda w_, h_: boot(w_, h_, k))(k) for k in ('bottom', 'right', 'front', 'left', 'back')})
    p = TEX / 'entity/equipment/humanoid/stormchaser.png'; p.parent.mkdir(parents=True, exist_ok=True)
    Image.fromarray(np.dstack([np.clip(img, 0, 255), a]).astype(np.uint8), 'RGBA').save(p)

    # humanoid_leggings: storm trousers, charcoal with orange knee pads and a reflective band
    img = np.zeros((32, 64, 3)); a = np.zeros((32, 64))
    def trouser(fw, fh, face, leg=True):
        rgb = fabric(CHAR, fw, fh, 31); al = np.full((fh, fw), 255.0)
        if face in ('top', 'bottom'): return rgb, al
        if leg:
            rgb[4:7] = fabric(ORANGE, fw, 3, 32)
            rgb[9] = REFL[:3]
        else:
            rgb[fh - 3:fh - 2] = ramp(CHAR)[1]                        # belt
            rgb[fh - 3, fw // 2 - 1:fw // 2 + 1] = C('#c8c0a8')[:3]
            al[:fh - 4] = 0
        return rgb, al
    cube_uv(img, a, 0, 16, 4, 12, 4, {k: (lambda k: lambda w_, h_: trouser(w_, h_, k))(k) for k in ('top', 'bottom', 'right', 'front', 'left', 'back')})
    cube_uv(img, a, 16, 16, 8, 12, 4, {k: (lambda k: lambda w_, h_: trouser(w_, h_, k, leg=False))(k) for k in ('right', 'front', 'left', 'back')})
    p = TEX / 'entity/equipment/humanoid_leggings/stormchaser.png'; p.parent.mkdir(parents=True, exist_ok=True)
    Image.fromarray(np.dstack([np.clip(img, 0, 255), a]).astype(np.uint8), 'RGBA').save(p)


# ---------------------------------------------------------------------------------------------------- blocks, 64x
def edge(d_px=2):
    return np.minimum(np.minimum(gx64, N - gx64), np.minimum(gy64, N - gy64)) < d_px


def serpentscale_block():
    SW, SH = 12, 9
    row = (gy64 // SH).astype(int)
    lx = (gx64 + (row % 2) * SW / 2) % SW - SW / 2
    ly = gy64 % SH
    rim = ly - (SH - 1.5) + lx * lx / (SW * 0.5)
    base, deep, hi, glow = np.array([60, 76, 128.0]), np.array([22, 28, 58.0]), np.array([110, 132, 190.0]), np.array([159, 232, 255.0])
    v = np.clip(1 - ly / SH, 0, 1) * (1 - np.abs(lx) / SW) + (noise64(6, 4) - 0.5) * 0.2
    rgb = deep + (hi - deep) * v[..., None]
    rgb = np.where((np.abs(rim) < 0.9)[..., None], glow * 0.75, rgb)
    rgb = np.where((np.abs(rim) < 0.45)[..., None], glow, rgb)
    sheen = np.clip(1 - np.abs(gx64 * 0.8 - gy64 - 4) / 10, 0, 1)
    rgb = rgb + sheen[..., None] * 22
    rgb = np.where(edge(1.2)[..., None], deep, rgb)
    save64(rgb, np.full((N, N), 255.0), 'block/serpentscale_block')


def glacial_lamp():
    n = noise64(4, 9) * 0.6 + noise64(10, 10) * 0.4
    ice, deep, core = np.array([196, 230, 248.0]), np.array([110, 168, 206.0]), np.array([240, 252, 255.0])
    rgb = deep + (ice - deep) * n[..., None]
    r = np.hypot(gx64 - 32, gy64 - 32)
    rgb = rgb + np.clip(1 - r / 26, 0, 1)[..., None] ** 1.5 * (core - rgb) * 0.9
    th = np.arctan2(gy64 - 32, gx64 - 32)
    cracks = (np.abs(np.sin(th * 5 + r * 0.08)) < 0.06) & (r > 8)
    rgb = np.where(cracks[..., None], np.array([255, 255, 255.0]), rgb)
    rgb = np.where(edge(4)[..., None], np.array([150, 196, 224.0]) * (0.85 + 0.15 * n[..., None]), rgb)
    rgb = np.where(edge(1.5)[..., None], np.array([92, 140, 176.0]), rgb)
    save64(rgb, np.full((N, N), 255.0), 'block/glacial_lamp')


def aurora_glass():
    """Animated: 16 frames of curtains of aurora light drifting across a pale glass pane with a frame."""
    frames = []
    for f in range(16):
        ph = f / 16 * 2 * math.pi
        curtain = np.sin(gx64 * 0.12 + ph + np.sin(gy64 * 0.05 + ph) * 1.6) * 0.5 + 0.5
        fall = np.clip(1 - gy64 / 64 + 0.2 * np.sin(gx64 * 0.2 + ph * 2), 0, 1)
        g = np.clip(gx64 / 64 + 0.25 * np.sin(ph + gy64 * 0.04), 0, 1)
        col = np.array([126, 240, 200.0]) * (1 - g[..., None]) + np.array([176, 110, 255.0]) * g[..., None]
        k = (curtain * fall)[..., None]
        rgb = np.array([220, 236, 246.0]) * (1 - k) + col * k
        a = 70 + 120 * (curtain * fall)
        streak = (np.abs((gx64 - gy64 * 0.5 + f * 2) % 40 - 6) < 1.2)
        rgb = np.where(streak[..., None], np.minimum(255, rgb + 40), rgb)
        fr = edge(3)
        rgb = np.where(fr[..., None], np.array([196, 212, 230.0]), rgb); a = np.where(fr, 235, a)
        rgb = np.where(edge(1)[..., None], np.array([120, 136, 160.0]), rgb)
        frames.append((rgb, a))
    rgb = np.concatenate([f[0] for f in frames], 0); a = np.concatenate([f[1] for f in frames], 0)
    p = TEX / 'block/aurora_glass.png'
    Image.fromarray(np.dstack([np.clip(rgb, 0, 255), np.clip(a, 0, 255)]).astype(np.uint8), 'RGBA').save(p)
    p.with_suffix('.png.mcmeta').write_text('{\n  "animation": {\n    "frametime": 4,\n    "interpolate": true\n  }\n}\n')


def shelter_hatch():
    n = noise64(5, 12)
    steel = np.array([92, 100, 110.0]) * (0.88 + 0.16 * n[..., None])
    rgb = steel.copy()
    plate = (np.abs(gx64 - 32) < 26) & (np.abs(gy64 - 32) < 26)
    rgb = np.where(plate[..., None], rgb * 1.08, rgb)
    hazard = ((gy64 < 8) | (gy64 > 56)) & ~edge(2)
    stripe = ((gx64 + gy64) // 6 % 2 == 0)
    rgb = np.where((hazard & stripe)[..., None], np.array([240, 176, 40.0]), rgb)
    rgb = np.where((hazard & ~stripe)[..., None], np.array([34, 36, 40.0]), rgb)
    for (cx, cy) in ((10, 12), (54, 12), (10, 52), (54, 52), (32, 12), (32, 52)):
        b = np.hypot(gx64 - cx, gy64 - cy)
        rgb = np.where((b < 2.2)[..., None], np.array([180, 188, 196.0]), rgb)
        rgb = np.where(((b >= 2.2) & (b < 3))[..., None], np.array([50, 54, 60.0]), rgb)
    wheel = np.abs(np.hypot(gx64 - 32, gy64 - 32) - 11) < 2
    spokes = ((np.abs(gx64 - 32) < 1.3) | (np.abs(gy64 - 32) < 1.3)) & (np.hypot(gx64 - 32, gy64 - 32) < 11)
    rgb = np.where((wheel | spokes)[..., None], np.array([200, 60, 40.0]), rgb)
    rgb = np.where(edge(1.5)[..., None], np.array([46, 50, 56.0]), rgb)
    save64(rgb, np.full((N, N), 255.0), 'block/storm_shelter_hatch')


def turbine_textures():
    n = noise64(6, 14)
    white = np.array([236, 240, 244.0]) * (0.92 + 0.08 * n[..., None])
    seams = (gy64 % 16 < 1.2)
    rgb = np.where(seams[..., None], white * 0.82, white)
    rivets = (gy64 % 16 > 2.5) & (gy64 % 16 < 4.5) & (gx64 % 8 > 2.5) & (gx64 % 8 < 4.5)
    rgb = np.where(rivets[..., None], white * 0.7, rgb)
    shade = np.clip(np.abs(gx64 - 32) / 32, 0, 1) ** 2
    rgb = rgb * (1 - 0.22 * shade[..., None])
    save64(rgb, np.full((N, N), 255.0), 'block/wind_turbine')
    # the nacelle: white with an orange band and a vent grille
    rgb2 = white.copy()
    rgb2 = np.where(((gy64 > 22) & (gy64 < 30))[..., None], np.array([224, 122, 42.0]), rgb2)
    vent = (gy64 > 36) & (gy64 < 52) & (gx64 > 10) & (gx64 < 54) & (gy64 % 4 < 2)
    rgb2 = np.where(vent[..., None], np.array([70, 76, 84.0]), rgb2)
    rgb2 = np.where(edge(2)[..., None], white * 0.78, rgb2)
    save64(rgb2, np.full((N, N), 255.0), 'block/wind_turbine_nacelle')
    # the foot: dark steel with a hazard band
    base = np.array([70, 76, 84.0]) * (0.9 + 0.12 * n[..., None])
    stripe = ((gx64 + gy64) // 8 % 2 == 0) & (gy64 > 44)
    base = np.where(stripe[..., None], np.array([240, 176, 40.0]), base)
    base = np.where(edge(2)[..., None], base * 0.7, base)
    save64(base, np.full((N, N), 255.0), 'block/wind_turbine_base')


def stormfly_jar():
    # glass: a pale pane with a frame and highlight, mostly see-through
    n = noise64(4, 16)
    rgb = np.broadcast_to(np.array([214, 232, 242.0]), (N, N, 3)).copy()
    a = 60 + 30 * n
    hl = (np.abs(gx64 - 14) < 3) & (gy64 > 8) & (gy64 < 56)
    rgb[hl] = 255; a[hl] = 170
    fr = edge(3); rgb[fr] = (170, 196, 214); a[fr] = 220
    save64(rgb, a, 'block/stormfly_jar')
    # inside: a dark storm haze with glowing flies
    rgb2 = np.array([30, 44, 64.0]) + noise64(5, 17)[..., None] * 30
    r = np.random.default_rng(18)
    for _ in range(16):
        cx, cy = r.uniform(6, 58), r.uniform(6, 58)
        d = np.hypot(gx64 - cx, gy64 - cy)
        rgb2 = np.where((d < 5)[..., None], rgb2 + (np.array([200, 250, 160.0]) - rgb2) * np.clip(1 - d / 5, 0, 1)[..., None] ** 2, rgb2)
        rgb2 = np.where((d < 1.8)[..., None], np.array([255, 255, 220.0]), rgb2)
    save64(rgb2, np.full((N, N), 255.0), 'block/stormfly_jar_inside')
    # lid: copper with a ring
    lid = np.array([176, 104, 60.0]) * (0.85 + 0.2 * noise64(4, 19)[..., None])
    ring = np.abs(np.hypot(gx64 - 32, gy64 - 32) - 18) < 3
    lid = np.where(ring[..., None], lid * 1.25, lid)
    lid = np.where(edge(2)[..., None], lid * 0.7, lid)
    save64(lid, np.full((N, N), 255.0), 'block/stormfly_jar_lid')


def block_item_icons():
    i = Icon()
    i.shape(rect(15, 10, 17.5, 30), C('#eceff3'), bump=0.4)
    i.shape(rect(12, 7, 21, 11), C('#eceff3'), bump=0.6)
    i.paint(rect(12, 8.5, 21, 9.5), C('#e07a2a'))
    for ang in (math.pi / 2, math.pi / 2 + 2.09, math.pi / 2 + 4.19):
        tipx, tipy = 12 + math.cos(ang) * 10, 9 - math.sin(ang) * 10
        i.shape(thick_path([(12, 9), (tipx, tipy)], 0.9), C('#f4f6f8'), bump=0.3)
        i.paint(circle(tipx, tipy, 1.0), C('#e07a2a'))
    i.shape(circle(11.5, 9, 1.6), C('#8a929a'), bump=0.6)
    i.save('item/wind_turbine')
    i = Icon()
    i.shape(rect(10, 9, 22, 27), C('#d6e8f2'), alpha=170, bump=0.6)
    i.shape(rect(11, 6, 21, 9), C('#b06838'), bump=0.5)
    for (fx, fy) in ((14, 14), (18, 17), (15, 21), (19, 23), (13, 24)):
        i.paint(circle(fx, fy, 1.3), C('#fbffb0'))
    i.paint(rect(11, 11, 12.5, 25), C('#ffffff'), 200)
    i.save('item/stormfly_jar')


def main():
    serpent_scale(); squall_heart(); glacial_core(); aurora_dust(); bottled_stormfly(); bottled_lightning()
    seeder('cloud_seeder', '#eef2f6', '#4a8ad8', '#8a929a')
    seeder('supercell_seeder', '#3a3460', '#f2c94c', '#7a56d8', bolt=True)
    gale_blade(); chasers_coffee(); snow_cone()
    goggles_icon(); jacket_icon(); trousers_icon(); boots_icon(); armour_layers()
    for k, (name, b, s) in enumerate((('squall_serpent', '#3c4c80', '#9fe8ff'), ('hailstone_golem', '#cde6f4', '#6ec8ff'),
                                      ('aurora_moth', '#3e3468', '#86f4cc'), ('stormfly', '#2e3a52', '#fbffb0'))):
        egg(name, b, s, 530 + k)
    serpentscale_block(); glacial_lamp(); aurora_glass(); shelter_hatch(); turbine_textures(); stormfly_jar(); block_item_icons()
    print('textures8: 5.3 items, eggs, armour and blocks painted')


if __name__ == '__main__':
    main()
