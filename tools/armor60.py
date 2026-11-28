"""Stormreach 6.0 armour: equipped textures painted piece by piece on the humanoid armour UV layout.

The 5.x armour layers flooded every UV box (head, body, arms and the whole leg) with one noisy colour, so a full set
rendered as a speckled blob: boots covered the entire leg, the chestplate hid the belt, the helmet had no face.
Here every piece is designed on its own faces (64x32 layout, painted at 4 px per texel = 256x128):

  helmet     head box   (0,0)   brow band, eye slit with nose guard, cheek guards, crest, neck guard; open face
  chestplate body box   (16,16) cuirass with collar, centre ridge, emblem, segmented abdomen; stops above the belt
             arm boxes  (40,16) pauldron over the shoulder, open upper arm, bracer, open hand
  boots      leg boxes  (0,16)  lower five rows only: cuff, greave, toe cap, sole
  leggings   (humanoid_leggings) belt + buckle on the waist rows, tassets, thigh plates, knee guards; open shins

Transparent texels let the player (and the other pieces) show through, which is what separates the pieces visually.
Runs after remaster64 so it has the last word on these files.
"""
import pathlib, random
from PIL import Image, ImageDraw

ROOT = pathlib.Path(__file__).resolve().parent.parent
EQ = ROOT / 'src/main/resources/assets/stormreach/textures/entity/equipment'
S = 4  # pixels per texel


def hexc(h, a=255):
    h = h.lstrip('#'); return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3)) + (a[3] if len(a) > 3 else 255,)


def faces(u, v, w, h, d):
    """Face rectangles (in texels) of a cube UV box: name -> (x, y, width, height)."""
    return {'top': (u + d, v, w, d), 'bottom': (u + d + w, v, w, d), 'right': (u, v + d, d, h),
            'front': (u + d, v + d, w, h), 'left': (u + d + w, v + d, d, h), 'back': (u + d + w + d, v + d, w, h)}


class Painter:
    def __init__(self, pal, seed):
        self.img = Image.new('RGBA', (64 * S, 32 * S), (0, 0, 0, 0))
        self.px = self.img.load(); self.pal = pal; self.rnd = random.Random(seed)

    # -- primitives in pixel space --
    def put(self, x, y, c):
        if 0 <= x < 64 * S and 0 <= y < 32 * S: self.px[x, y] = c

    def clear(self, x0, y0, x1, y1):
        for y in range(int(y0), int(y1)):
            for x in range(int(x0), int(x1)): self.put(x, y, (0, 0, 0, 0))

    def plate(self, x0, y0, x1, y1, base=None, light=None, dark=None, brushed=True, bevel=1):
        """A metal plate with a top-left highlight, bottom-right shadow, soft vertical light falloff and faint brushing."""
        base = base or self.pal['base']; light = light or self.pal['light']; dark = dark or self.pal['dark']
        x0, y0, x1, y1 = int(x0), int(y0), int(x1), int(y1)
        hgt = max(1, y1 - y0)
        for y in range(y0, y1):
            t = (y - y0) / hgt
            row = mix(mix(base, light, 0.18), mix(base, dark, 0.22), t)
            streak = self.rnd.uniform(-0.04, 0.04) if brushed else 0
            for x in range(x0, x1):
                c = mix(row, light if streak > 0 else dark, abs(streak) + self.rnd.uniform(0, 0.03))
                self.put(x, y, c)
        for i in range(bevel):
            for x in range(x0, x1): self.put(x, y0 + i, mix(light, base, 0.25)); self.put(x, y1 - 1 - i, mix(dark, base, 0.2))
            for y in range(y0, y1): self.put(x0 + i, y, mix(light, base, 0.35)); self.put(x1 - 1 - i, y, mix(dark, base, 0.3))
        self.put(x0, y0, light); self.put(x1 - 1, y1 - 1, dark)

    def line(self, x0, y0, x1, y1, c, w=1):
        d = ImageDraw.Draw(self.img); d.line([(x0, y0), (x1, y1)], fill=c, width=w)

    def rivet(self, x, y):
        self.put(x, y, self.pal['light']); self.put(x + 1, y, self.pal['base']); self.put(x, y + 1, self.pal['base']); self.put(x + 1, y + 1, self.pal['dark'])

    def gem(self, cx, cy, r=3):
        g, gl = self.pal['glow'], self.pal['glow_l']
        for y in range(-r, r + 1):
            for x in range(-r, r + 1):
                if abs(x) + abs(y) <= r: self.put(cx + x, cy + y, mix(g, gl, max(0, (-x - y) / (2 * r) + 0.3)))
        for i in range(-r - 1, r + 2):
            for (x, y) in ((cx + i, cy - (r + 1 - abs(i))), (cx + i, cy + (r + 1 - abs(i)))): self.put(x, y, self.pal['dark'])

    def trim(self, x0, x1, y, c=None):
        c = c or self.pal['trim']
        for x in range(int(x0), int(x1)): self.put(x, y, c); self.put(x, y + 1, mix(c, self.pal['dark'], 0.4))

    def motif(self, x0, y0, x1, y1):
        """Set-specific decoration inside a rectangle (pixel coords)."""
        kind = self.pal['motif']; r = self.rnd
        if kind == 'shard':   # faceted crystal shards growing from the lower edge
            for i in range(3):
                cx = x0 + (x1 - x0) * (i + 1) / 4 + r.randint(-2, 2); hgt = (y1 - y0) * r.uniform(0.45, 0.8)
                for y in range(int(y1 - hgt), int(y1)):
                    half = (y - (y1 - hgt)) / hgt * 3.5
                    for x in range(int(cx - half), int(cx + half) + 1):
                        self.put(x, y, mix(self.pal['glow'], self.pal['glow_l'], 0.7 if x < cx else 0.15))
        elif kind == 'volt':  # a forked lightning bolt
            c, cl = self.pal['glow'], self.pal['glow_l']
            pts = [(x0 + (x1 - x0) * 0.6, y0), (x0 + (x1 - x0) * 0.35, y0 + (y1 - y0) * 0.5),
                   (x0 + (x1 - x0) * 0.62, y0 + (y1 - y0) * 0.48), (x0 + (x1 - x0) * 0.4, y1)]
            for a, b in zip(pts, pts[1:]): self.line(a[0], a[1], b[0], b[1], c, 3)
            for a, b in zip(pts, pts[1:]): self.line(a[0], a[1], b[0], b[1], cl, 1)
        elif kind == 'aurora':  # ribbons of aurora light rippling across the plate
            cols = [hexc('#ff6fd8'), hexc('#7fe8ff'), hexc('#7dffb0')]
            for k, c in enumerate(cols):
                for x in range(int(x0), int(x1)):
                    import math
                    y = y0 + (y1 - y0) * (0.25 + 0.22 * k) + 2.2 * math.sin((x - x0) / 5.0 + k * 1.7)
                    for dy in range(2): self.put(x, int(y) + dy, mix(c, self.pal['base'], 0.15 + 0.3 * dy))
        elif kind == 'cloud':  # soft puffs
            for i in range(5):
                cx = r.uniform(x0, x1); cy = r.uniform(y0, y1); rad = r.uniform(3, 6)
                for y in range(int(cy - rad), int(cy + rad) + 1):
                    for x in range(int(cx - rad), int(cx + rad) + 1):
                        if (x - cx) ** 2 + (y - cy) ** 2 <= rad * rad and x0 <= x < x1 and y0 <= y < y1:
                            self.put(x, y, mix(self.pal['light'], self.pal['base'], ((y - cy) / rad + 1) * 0.35))

    def save(self, path):
        path.parent.mkdir(parents=True, exist_ok=True); self.img.save(path)


def T(v):  # texel -> pixel
    return int(round(v * S))


# ---------------- pieces ----------------
def helmet(p, style='helm'):
    f = faces(0, 0, 8, 8, 8)
    fx, fy, _, _ = f['front']
    if style == 'crown':
        # a circlet: band on rows 1..3 of every side face, points above, gem at the front
        for name in ('front', 'left', 'right', 'back'):
            x, y, w, h = f[name]
            p.plate(T(x), T(y + 1.5), T(x + w), T(y + 3.5))
            for k in range(w // 2):
                px0 = T(x + k * 2 + 0.5); p.plate(px0, T(y + 0.5), px0 + S, T(y + 1.6))
            p.trim(T(x), T(x + w), T(y + 3.5) - 1)
        p.gem(T(fx + 4), T(fy + 2.5), 3)
        return
    if style == 'cap':
        # a weather-service cap: crown on the upper half, logo, brim shadow on the front
        for name in ('top', 'front', 'left', 'right', 'back'):
            x, y, w, h = f[name]
            if name == 'top': p.plate(T(x), T(y), T(x + w), T(y + h), brushed=False, bevel=0); continue
            p.plate(T(x), T(y), T(x + w), T(y + 3.2), brushed=False, bevel=0)
        p.plate(T(fx), T(fy + 3.2), T(fx + 8), T(fy + 3.9), base=p.pal['dark'], light=p.pal['base'], dark=p.pal['dark'], brushed=False, bevel=0)
        p.gem(T(fx + 4), T(fy + 1.6), 3)
        bx, by, _, _ = f['back']; p.clear(T(bx + 3), T(by + 2.2), T(bx + 5), T(by + 3.2))  # strap gap
        return
    if style == 'goggles':
        return
    for name, (x, y, w, h) in f.items():
        if name == 'bottom': continue
        p.plate(T(x), T(y), T(x + w), T(y + h))
    # crest ridge over the top and down the back
    tx, ty, tw, th = f['top']
    p.plate(T(tx + 3.3), T(ty), T(tx + 4.7), T(ty + th), base=p.pal['trim'], light=p.pal['light'], dark=p.pal['dark'])
    bx, by, bw, bh = f['back']
    p.plate(T(bx + 3.3), T(by), T(bx + 4.7), T(by + 5), base=p.pal['trim'], light=p.pal['light'], dark=p.pal['dark'])
    # front: brow band, eye slit, nose guard, cheek guards, open mouth
    p.trim(T(fx), T(fx + 8), T(fy + 2.6))
    p.clear(T(fx + 1), T(fy + 3.2), T(fx + 7), T(fy + 4.6))          # eye slit
    p.plate(T(fx + 3.4), T(fy + 2.8), T(fx + 4.6), T(fy + 6.0), base=p.pal['trim'], light=p.pal['light'], dark=p.pal['dark'])  # nose guard
    p.clear(T(fx + 2.0), T(fy + 6.0), T(fx + 6.0), T(fy + 8))        # open mouth
    for cx in (fx + 0.6, fx + 7.0): p.rivet(T(cx), T(fy + 6.6))
    p.motif(T(fx + 1), T(fy + 0.3), T(fx + 7), T(fy + 2.5))
    # sides: ear guard ring with a rivet, back: neck guard flare
    for name in ('left', 'right'):
        x, y, w, h = f[name]
        p.trim(T(x), T(x + w), T(y + 2.6))
        p.rivet(T(x + 3.6), T(y + 5)); p.line(T(x + 1), T(y + 7.6), T(x + 7), T(y + 7.6), p.pal['dark'])
    p.trim(T(bx), T(bx + 8), T(by + 6.2))


def chest(p, chaser=False):
    f = faces(16, 16, 8, 12, 4)
    for name, (x, y, w, h) in f.items():
        if name == 'bottom': continue
        hh = 10 if name != 'top' else h
        p.plate(T(x), T(y), T(x + w), T(y + hh))
    fx, fy, _, _ = f['front']
    p.clear(T(fx + 2.5), T(fy), T(fx + 5.5), T(fy + 0.8))              # collar notch
    p.line(T(fx + 2.5), T(fy + 0.8), T(fx + 4), T(fy + 2.2), p.pal['dark']); p.line(T(fx + 5.5), T(fy + 0.8), T(fx + 4), T(fy + 2.2), p.pal['dark'])
    p.trim(T(fx), T(fx + 8), T(fy + 0.9))
    p.line(T(fx + 4), T(fy + 2.4), T(fx + 4), T(fy + 6.4), p.pal['light'])  # centre ridge
    p.motif(T(fx + 1.2), T(fy + 2.2), T(fx + 6.8), T(fy + 6.6))
    p.gem(T(fx + 4), T(fy + 4.2), 3)
    for k, yy in enumerate((7.0, 8.0, 9.0)):                            # segmented abdomen bands
        p.plate(T(fx + 0.4 + k * 0.2), T(fy + yy), T(fx + 7.6 - k * 0.2), T(fy + yy + 0.9), bevel=1)
    bx, by, _, _ = f['back']
    p.line(T(bx + 4), T(by + 0.5), T(bx + 4), T(by + 9.5), p.pal['dark'], 2)  # spine
    for yy in (3, 6): p.rivet(T(bx + 1), T(by + yy)); p.rivet(T(bx + 6.6), T(by + yy))
    for name in ('left', 'right'):
        x, y, w, h = f[name]; p.trim(T(x), T(x + w), T(y + 0.9))
    # arms: pauldron (rows 0..4 + top), open upper arm, bracer rows 7..11, open hand
    a = faces(40, 16, 4, 12, 4)
    for name, (x, y, w, h) in a.items():
        if name == 'bottom': continue
        if name == 'top': p.plate(T(x), T(y), T(x + w), T(y + h)); continue
        p.plate(T(x), T(y), T(x + w), T(y + 4.2))
        p.trim(T(x), T(x + w), T(y + 4.2) - 2)
        p.plate(T(x), T(y + 7), T(x + w), T(y + 11), bevel=1)
        p.trim(T(x), T(x + w), T(y + 7))
        p.rivet(T(x + 1.6), T(y + 1.8))
    if chaser: pass


def boots(p):
    f = faces(0, 16, 4, 12, 4)
    for name, (x, y, w, h) in f.items():
        if name == 'top': continue
        if name == 'bottom':
            p.plate(T(x), T(y), T(x + w), T(y + h), base=p.pal['sole'], light=mix(p.pal['sole'], (255, 255, 255, 255), 0.2), dark=p.pal['sole'], brushed=False); continue
        p.plate(T(x), T(y + 7.2), T(x + w), T(y + 12))
        p.trim(T(x), T(x + w), T(y + 7.2))                               # cuff
        for xx in range(T(x), T(x + w)):                                 # sole
            for yy in range(T(y + 11.3), T(y + 12)): p.put(xx, yy, p.pal['sole'])
    fx, fy, _, _ = f['front']
    p.plate(T(fx + 0.3), T(fy + 9.4), T(fx + 3.7), T(fy + 11.3), base=p.pal['trim'], light=p.pal['light'], dark=p.pal['dark'])  # toe cap
    p.motif(T(fx + 0.5), T(fy + 7.8), T(fx + 3.5), T(fy + 9.4))


def leggings(p):
    f = faces(16, 16, 8, 12, 4)
    for name, (x, y, w, h) in f.items():
        if name == 'top': continue
        if name == 'bottom': p.plate(T(x), T(y), T(x + w), T(y + h)); continue
        p.plate(T(x), T(y + 9.2), T(x + w), T(y + 12), base=p.pal['belt'], light=mix(p.pal['belt'], (255, 255, 255, 255), 0.25), dark=mix(p.pal['belt'], (0, 0, 0, 255), 0.4))
        p.trim(T(x), T(x + w), T(y + 9.2))
    fx, fy, _, _ = f['front']
    p.plate(T(fx + 3), T(fy + 9.6), T(fx + 5), T(fy + 11.6), base=p.pal['trim'], light=p.pal['light'], dark=p.pal['dark'])  # buckle
    p.clear(T(fx + 3.5), T(fy + 10.1), T(fx + 4.5), T(fy + 11.1))
    l = faces(0, 16, 4, 12, 4)
    for name, (x, y, w, h) in l.items():
        if name in ('top', 'bottom'): continue
        p.plate(T(x), T(y), T(x + w), T(y + 3.2))                        # tasset
        p.plate(T(x), T(y + 3.2), T(x + w), T(y + 5.8), bevel=1)         # thigh
        p.trim(T(x), T(x + w), T(y + 3.2))
        p.plate(T(x), T(y + 5.8), T(x + w), T(y + 7.4), base=p.pal['trim'], light=p.pal['light'], dark=p.pal['dark'])  # knee
    lx, ly, _, _ = l['front']
    p.rivet(T(lx + 1.6), T(ly + 6.3)); p.motif(T(lx + 0.4), T(ly + 0.4), T(lx + 3.6), T(ly + 3.0))


SETS = {
    'stormshard': dict(base=hexc('#4f9fc6'), light=hexc('#c6f1ff'), dark=hexc('#1f4f6e'), trim=hexc('#8fd6f2'), glow=hexc('#5fe0ff'), glow_l=hexc('#e8fdff'), belt=hexc('#2b3a48'), sole=hexc('#1c2630'), motif='shard'),
    'voltite': dict(base=hexc('#d9a62a'), light=hexc('#fff2a8'), dark=hexc('#7a5212'), trim=hexc('#f6d36a'), glow=hexc('#6fd2ff'), glow_l=hexc('#ffffff'), belt=hexc('#3b2e1c'), sole=hexc('#2a2014'), motif='volt'),
    'aurorite': dict(base=hexc('#5a3c9a'), light=hexc('#d9c8ff'), dark=hexc('#26164a'), trim=hexc('#c0c8d8'), glow=hexc('#ff7ad8'), glow_l=hexc('#ffe6fb'), belt=hexc('#2a2040'), sole=hexc('#1a1428'), motif='aurora'),
    'cloudstep': dict(base=hexc('#dfe8f0'), light=hexc('#ffffff'), dark=hexc('#8ea3b6'), trim=hexc('#b9cde0'), glow=hexc('#9fe2ff'), glow_l=hexc('#ffffff'), belt=hexc('#8ea3b6'), sole=hexc('#6f8396'), motif='cloud'),
    'stormcrown': dict(base=hexc('#9fb3c2'), light=hexc('#e9f2ff'), dark=hexc('#4b5a6a'), trim=hexc('#c9c0ff'), glow=hexc('#7f6cff'), glow_l=hexc('#ffffff'), belt=hexc('#333333'), sole=hexc('#222222'), motif='none'),
    'storm_chaser_cap': dict(base=hexc('#c9452e'), light=hexc('#f07a5e'), dark=hexc('#6e2014'), trim=hexc('#f2c94c'), glow=hexc('#f2c94c'), glow_l=hexc('#fff5c8'), belt=hexc('#333333'), sole=hexc('#222222'), motif='none'),
}

if __name__ == '__main__':
    for name in ('stormshard', 'voltite', 'aurorite'):
        p = Painter(SETS[name], hash(name) & 0xffff)
        helmet(p); chest(p); boots(p); p.save(EQ / 'humanoid' / f'{name}.png')
        q = Painter(SETS[name], (hash(name) + 7) & 0xffff)
        leggings(q); q.save(EQ / 'humanoid_leggings' / f'{name}.png')
    p = Painter(SETS['cloudstep'], 11); boots(p); p.save(EQ / 'humanoid' / 'cloudstep.png')
    p = Painter(SETS['stormcrown'], 12); helmet(p, 'crown'); p.save(EQ / 'humanoid' / 'stormcrown.png')
    p = Painter(SETS['storm_chaser_cap'], 13); helmet(p, 'cap'); p.save(EQ / 'humanoid' / 'storm_chaser_cap.png')
    print('armor60: 6 armour sets repainted piece by piece')
