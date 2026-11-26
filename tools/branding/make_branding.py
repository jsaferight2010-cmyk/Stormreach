"""6.0 branding: the mod icon (Mod Menu, the jar), the GitHub banner and social preview. Original art, drawn here.
Run: python3 tools/branding/make_branding.py  (writes the icon into assets and the banners into tools/branding/out)."""
import math, pathlib, random
from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = pathlib.Path(__file__).resolve().parents[2]
OUT = pathlib.Path(__file__).resolve().parent / 'out'
OUT.mkdir(exist_ok=True)
FONT_DIR = pathlib.Path('/usr/share/fonts/truetype')


def font(size, bold=True):
    for name in (('dejavu/DejaVuSans-Bold.ttf' if bold else 'dejavu/DejaVuSans.ttf'), 'liberation/LiberationSans-Bold.ttf'):
        p = FONT_DIR / name
        if p.exists(): return ImageFont.truetype(str(p), size)
    return ImageFont.load_default()


def lerp(a, b, t): return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(len(a)))


def scene(W, H, seed=6, cx=0.5):
    """The storm coast: a dark sky, a supercell with an anvil, a funnel touching down on a cliff, lightning, and the sea."""
    r = random.Random(seed)
    img = Image.new('RGBA', (W, H))
    d = ImageDraw.Draw(img)
    horizon = int(H * 0.70)
    for y in range(H):                                                     # sky, then sea
        if y < horizon:
            t = y / horizon
            d.line([(0, y), (W, y)], fill=lerp((14, 20, 38, 255), (66, 88, 112, 255), t ** 1.4))
        else:
            t = (y - horizon) / (H - horizon)
            d.line([(0, y), (W, y)], fill=lerp((40, 72, 96, 255), (10, 28, 44, 255), t))
    # glow behind the storm (lightning lighting the cloud from inside)
    glow = Image.new('RGBA', (W, H)); gd = ImageDraw.Draw(glow)
    gx, gy = int(W * cx), int(H * 0.30)
    gd.ellipse([gx - W * 0.35, gy - H * 0.25, gx + W * 0.35, gy + H * 0.25], fill=(120, 190, 255, 90))
    img.alpha_composite(glow.filter(ImageFilter.GaussianBlur(W * 0.08)))
    # the cloud: anvil on top, a tower of billows, flat dark base
    cl = Image.new('RGBA', (W, H)); c = ImageDraw.Draw(cl)
    base = int(H * 0.46)
    c.ellipse([W * (cx - 0.55), H * 0.04, W * (cx + 0.55), H * 0.24], fill=(92, 106, 128, 255))          # anvil
    for _ in range(26):                                                                                  # billows
        bx = W * (cx + r.uniform(-0.30, 0.30)); by = H * r.uniform(0.14, 0.42); br = W * r.uniform(0.07, 0.14)
        shade = r.randint(70, 118)
        c.ellipse([bx - br, by - br * 0.8, bx + br, by + br * 0.8], fill=(shade, shade + 12, shade + 30, 255))
    c.rectangle([0, base, W, H], fill=(0, 0, 0, 0))                                                      # flat base
    c.ellipse([W * (cx - 0.40), base - H * 0.07, W * (cx + 0.40), base + H * 0.012], fill=(48, 56, 74, 255))   # dark rain-free base
    img.alpha_composite(cl.filter(ImageFilter.GaussianBlur(W * 0.004)))
    # rain shaft to the left
    rain = Image.new('RGBA', (W, H)); rd = ImageDraw.Draw(rain)
    for i in range(int(W * 0.25)):
        x = W * (cx - 0.40) + r.uniform(0, W * 0.22); y0 = base + r.uniform(0, H * 0.05)
        rd.line([(x, y0), (x - W * 0.02, horizon)], fill=(150, 180, 210, 40), width=max(1, W // 400))
    img.alpha_composite(rain)
    # cliff on the right
    cliff = [(W * (cx + 0.10), horizon), (W * (cx + 0.16), H * 0.60), (W * (cx + 0.30), H * 0.57), (W * (cx + 0.45), H * 0.58),
             (W * 1.0, H * 0.56), (W, horizon)]
    d = ImageDraw.Draw(img)
    d.polygon(cliff, fill=(34, 40, 48, 255))
    d.polygon([(p[0], p[1] + H * 0.012) for p in cliff[1:5]] + [(cliff[4][0], cliff[4][1] + H * 0.03), (cliff[1][0], cliff[1][1] + H * 0.03)], fill=(52, 76, 54, 255))
    # the funnel: a cone from the cloud base to the cliff top, with a debris cloud
    fx_top, fx_bot = W * (cx + 0.22), W * (cx + 0.27)
    fun = Image.new('RGBA', (W, H)); fd = ImageDraw.Draw(fun)
    steps = 60
    for i in range(steps):
        t = i / steps
        y = base + (H * 0.575 - base) * t
        x = fx_top + (fx_bot - fx_top) * t + math.sin(t * 5) * W * 0.008
        hw = W * (0.085 * (1 - t) ** 1.5 + 0.012)
        fd.ellipse([x - hw, y - H * 0.012, x + hw, y + H * 0.012], fill=(118, 126, 138, 235))
    fd.ellipse([fx_bot - W * 0.06, H * 0.55, fx_bot + W * 0.06, H * 0.60], fill=(96, 92, 86, 170))
    img.alpha_composite(fun.filter(ImageFilter.GaussianBlur(W * 0.003)))
    # lightning: a jagged bolt with a glow
    bolt = Image.new('RGBA', (W, H)); bd = ImageDraw.Draw(bolt)
    x, y = W * (cx - 0.10), base
    pts = [(x, y)]
    while y < horizon:
        y += H * r.uniform(0.03, 0.06); x += W * r.uniform(-0.035, 0.03); pts.append((x, min(y, horizon)))
    bd.line(pts, fill=(170, 220, 255, 255), width=max(3, W // 40), joint='curve')
    glow2 = bolt.filter(ImageFilter.GaussianBlur(W * 0.02))
    img.alpha_composite(glow2); img.alpha_composite(glow2)
    bd2 = ImageDraw.Draw(bolt); bd2.line(pts, fill=(245, 252, 255, 255), width=max(2, W // 90), joint='curve')
    img.alpha_composite(bolt)
    # whitecaps
    d = ImageDraw.Draw(img)
    for _ in range(int(W * 0.12)):
        y = r.uniform(horizon + H * 0.02, H); x = r.uniform(0, W); ln = W * r.uniform(0.01, 0.035) * (0.5 + (y - horizon) / (H - horizon))
        d.line([(x, y), (x + ln, y - H * 0.004)], fill=(200, 225, 240, 160), width=max(1, W // 300))
    return img


def icon():
    S = 1024
    img = scene(S, S, seed=6, cx=0.42)
    mask = Image.new('L', (S, S), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, S - 1, S - 1], radius=S // 7, fill=255)
    out = Image.new('RGBA', (S, S)); out.paste(img, (0, 0), mask)
    ImageDraw.Draw(out).rounded_rectangle([4, 4, S - 5, S - 5], radius=S // 7, outline=(120, 190, 240, 255), width=S // 64)
    out = out.resize((512, 512), Image.LANCZOS)
    out.save(ROOT / 'src/main/resources/assets/stormreach/icon.png')
    out.save(OUT / 'icon_512.png')
    out.resize((128, 128), Image.LANCZOS).save(OUT / 'icon_128.png')
    return out


def banner(W, H, name):
    img = scene(W * 2, H * 2, seed=7, cx=0.66).resize((W, H), Image.LANCZOS)
    shade = Image.new('RGBA', (W, H)); sd = ImageDraw.Draw(shade)
    for x in range(int(W * 0.62)):
        sd.line([(x, 0), (x, H)], fill=(6, 10, 20, int(200 * (1 - x / (W * 0.62)) ** 0.8)))
    img.alpha_composite(shade)
    d = ImageDraw.Draw(img)
    tsize = int(H * 0.19)
    d.text((int(W * 0.06), int(H * 0.26)), 'STORMREACH', font=font(tsize), fill=(235, 245, 255, 255), stroke_width=max(2, tsize // 30), stroke_fill=(20, 60, 100, 255))
    d.text((int(W * 0.065), int(H * 0.26) + int(tsize * 1.18)), 'Storms, tornadoes and a sky realm on a storm-swept coast', font=font(int(H * 0.045), False), fill=(190, 215, 235, 255))
    d.text((int(W * 0.065), int(H * 0.26) + int(tsize * 1.18) + int(H * 0.075)), 'Fabric  ·  Minecraft Java 26.3  ·  v6.0', font=font(int(H * 0.038)), fill=(130, 200, 250, 255))
    img.convert('RGB').save(OUT / name)


if __name__ == '__main__':
    icon()
    banner(1280, 640, 'social_preview_1280x640.png')
    banner(1600, 400, 'readme_banner_1600x400.png')
    print('branding: icon and banners written')
