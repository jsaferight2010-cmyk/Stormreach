"""5.1 texture pass: every Stormreach block and item texture becomes 64x, and mob and armour textures 4x their old size
(animated strips keep their frame layout).
Each original pixel becomes a 4x4 cell with a soft bevel (lit top-left, shaded bottom-right) and fine grain, so the
designs stay recognisable but surfaces read as carved, stacked or riveted at 64x. Textures already drawn at 64x
(the 5.1 additions) are left alone. Runs last in run_all.py. This is a detail pass on the existing designs, not a
hand redraw: individual textures can still be replaced by real 64x art later (draw it at 64x and this skips it)."""
import glob, os, pathlib, random
from PIL import Image
ROOT = pathlib.Path(__file__).resolve().parent.parent
BLOCK = ROOT / 'src' / 'main' / 'resources' / 'assets' / 'stormreach' / 'textures' / 'block'
S = 4
def clamp(v): return max(0, min(255, int(v)))
TEX = BLOCK.parent
# smooth, procedurally drawn textures that are already high resolution and would only lose by a pixel-art bevel
SKIP = {'tornado_funnel', 'tornado_debris', 'tornado_wallcloud', 'storm_cloud', 'hailstone', 'tornado'}
def targets():
    for f in sorted(glob.glob(str(BLOCK / '*.png'))): yield f, 64
    for f in sorted(glob.glob(str(TEX / 'item' / '*.png'))): yield f, 64
    for f in sorted(glob.glob(str(TEX / 'entity' / '**' / '*.png'), recursive=True)):
        if pathlib.Path(f).stem not in SKIP: yield f, None
def detail(img, S, seed):
    w, h = img.size
    rnd = random.Random(seed)
    out = Image.new('RGBA', (w * S, h * S))
    src = img.load(); dst = out.load()
    for y in range(h):
        for x in range(w):
            r, g, b, a = src[x, y]
            up = src[x, y - 1] if y > 0 else None
            left = src[x - 1, y] if x > 0 else None
            down = src[x, y + 1] if y < h - 1 else None
            right = src[x + 1, y] if x < w - 1 else None
            def differs(o): return o is not None and (abs(o[0] - r) + abs(o[1] - g) + abs(o[2] - b) > 24 or o[3] != a)
            for dy in range(S):
                for dx in range(S):
                    k = 1.0 + (rnd.random() - 0.5) * 0.07
                    if dy == 0 and differs(up): k *= 1.10
                    if dx == 0 and differs(left): k *= 1.07
                    if dy == S - 1 and differs(down): k *= 0.86
                    if dx == S - 1 and differs(right): k *= 0.90
                    dst[x * S + dx, y * S + dy] = (clamp(r * k), clamp(g * k), clamp(b * k), a) if a else (0, 0, 0, 0)
    return out
n = 0
for f, target in targets():
    img = Image.open(f).convert('RGBA')
    w, h = img.size
    if target:                                  # blocks and items: up to 64 wide
        if w >= target or target % w: continue
        S = target // w
    else:                                       # mobs and armour: 4x small sheets, 2x medium, leave large ones
        S = 4 if w <= 64 else 2 if w <= 128 else 1
        if S == 1: continue
    detail(img, S, sum(map(ord, os.path.basename(f))) * 131).save(f)
    n += 1
print(f'hd64: {n} textures given the 64x detail pass')
