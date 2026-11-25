"""5.1 block texture pass: every 16x Stormreach block texture becomes 64x (animated strips keep their frame layout).
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
n = 0
for f in sorted(glob.glob(str(BLOCK / '*.png'))):
    img = Image.open(f).convert('RGBA')
    w, h = img.size
    if w != 16: continue                      # already high resolution
    rnd = random.Random(sum(map(ord, os.path.basename(f))) * 131)
    out = Image.new('RGBA', (w * S, h * S))
    src = img.load(); dst = out.load()
    for y in range(h):
        for x in range(w):
            r, g, b, a = src[x, y]
            # neighbours decide whether this cell has a lit or shaded edge (no bevel inside flat runs of one colour)
            up = src[x, (y - 1) % h] if y % 16 else None
            left = src[(x - 1) % w, y]
            down = src[x, (y + 1) % h] if (y + 1) % 16 else None
            right = src[(x + 1) % w, y]
            def differs(o): return o is not None and (abs(o[0] - r) + abs(o[1] - g) + abs(o[2] - b) > 24 or o[3] != a)
            for dy in range(S):
                for dx in range(S):
                    k = 1.0 + (rnd.random() - 0.5) * 0.07          # grain
                    if dy == 0 and differs(up): k *= 1.10
                    if dx == 0 and differs(left): k *= 1.07
                    if dy == S - 1 and differs(down): k *= 0.86
                    if dx == S - 1 and differs(right): k *= 0.90
                    dst[x * S + dx, y * S + dy] = (clamp(r * k), clamp(g * k), clamp(b * k), a) if a else (0, 0, 0, 0)
    out.save(f)
    n += 1
print(f'hd64: {n} block textures upscaled to 64x')
