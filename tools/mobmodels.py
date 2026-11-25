"""Stormreach 5.1 mob models: every Stormreach creature rebuilt as a detailed jointed model, with a texture painted
to match it at 4x density (64x-equivalent detail).

Each model is described below as parts and boxes (Minecraft model units, y down, 16 units = 1 block). This script
1. packs every box's UV island into the texture,
2. paints each island by material (shell, fur, feather, scale, metal, crystal, glow, eye, cloud, water, cloth...)
   with per-face lighting, edge definition and fine detail, and
3. writes the Java model class (src/client/java/com/stormreach/client/model/gen), with its animation.
Don't edit the generated Java or PNGs; change the description here and rerun (run_all.py does).
"""
import pathlib, math
import numpy as np
from PIL import Image

ROOT = pathlib.Path(__file__).resolve().parent.parent
JAVA = ROOT / 'src' / 'client' / 'java' / 'com' / 'stormreach' / 'client' / 'model' / 'gen'
TEX = ROOT / 'src' / 'main' / 'resources' / 'assets' / 'stormreach' / 'textures' / 'entity'
DENSITY = 4


# ================================================================================================ description DSL
class Box:
    def __init__(self, x, y, z, w, h, d, mat, inflate=0.0, mirror=False):
        self.o = (x, y, z); self.s = (int(w), int(h), int(d)); self.mat = mat; self.inflate = inflate; self.mirror = mirror
        self.uv = None


class Part:
    def __init__(self, name, pivot=(0, 0, 0), rot=(0, 0, 0), boxes=(), field=None):
        self.name = name; self.pivot = pivot; self.rot = rot; self.boxes = list(boxes); self.children = []; self.field = field

    def add(self, *parts):
        self.children.extend(parts); return self


class Model:
    def __init__(self, cls, texture, mats, parts, anim, state='LivingEntityRenderState', doc='', extra='', flags=()):
        self.cls = cls; self.texture = texture; self.mats = mats; self.parts = parts; self.anim = anim; self.flags = flags
        self.state = state; self.doc = doc; self.extra = extra


def mirror_part(p, suffix_from='left', suffix_to='right'):
    """A mirrored copy (x flipped) of a part tree, renaming left->right."""
    q = Part(p.name.replace(suffix_from, suffix_to), (-p.pivot[0], p.pivot[1], p.pivot[2]), (p.rot[0], -p.rot[1], -p.rot[2]),
             [Box(-(b.o[0] + b.s[0]), b.o[1], b.o[2], *b.s, b.mat, b.inflate, not b.mirror) for b in p.boxes],
             p.field.replace(suffix_from.capitalize(), suffix_to.capitalize()).replace(suffix_from, suffix_to) if p.field else None)
    q.children = [mirror_part(c, suffix_from, suffix_to) for c in p.children]
    return q


# ================================================================================================ materials / painting
def C(h): return np.array([int(h[i:i + 2], 16) for i in (1, 3, 5)], dtype=np.float64)


def value_noise(rng, cells, w, h):
    g = rng.random((cells + 1, cells + 1))
    ys = np.linspace(0, cells, h, endpoint=False); xs = np.linspace(0, cells, w, endpoint=False)
    y0 = ys.astype(int); x0 = xs.astype(int); fy = ys - y0; fx = xs - x0
    fy = fy * fy * (3 - 2 * fy); fx = fx * fx * (3 - 2 * fx)
    a = g[y0][:, x0]; b = g[y0][:, x0 + 1]; c = g[y0 + 1][:, x0]; d = g[y0 + 1][:, x0 + 1]
    top = a * (1 - fx) + b * fx; bot = c * (1 - fx) + d * fx
    return top * (1 - fy)[:, None] + bot * fy[:, None]


def fbm(rng, w, h, base=3):
    return (value_noise(rng, base, w, h) + 0.5 * value_noise(rng, base * 2, w, h) + 0.25 * value_noise(rng, base * 4, w, h)) / 1.75


def paint_face(img, al, x0, y0, w, h, mat, face, rng):
    """Paints one face rectangle (pixels) of a box in the given material."""
    if w <= 0 or h <= 0: return
    kind, cols = mat[0], mat[1:]
    base, second = cols[0], cols[1] if len(cols) > 1 else cols[0] * 0.75
    yy, xx = np.mgrid[0:h, 0:w]
    fy, fx = (yy + 0.5) / h, (xx + 0.5) / w
    n = fbm(rng, w, h, 2 + (w + h) // 24)
    alpha = np.full((h, w), 255.0)
    if kind == 'fur':
        # tufts: short strands that lean down, varied per column, with darker roots between them
        col = rng.random(w)[None, :]
        phase = (yy + (col * 7).astype(int)) % 5
        strand = 1.0 - phase / 6.0
        rgb = base[None, None] * (0.72 + 0.22 * n[..., None] + 0.16 * strand[..., None] + 0.06 * col[..., None])
        rgb = np.where((phase == 4)[..., None], rgb * 0.8 + second * 0.2, rgb)
    elif kind == 'shell':
        plates = ((xx // max(3, w // 3)) + (yy // max(3, h // 2))) % 2
        rgb = base[None, None] * (0.85 + 0.2 * n[..., None])
        rgb = np.where(plates[..., None] == 1, rgb * 0.93, rgb)
        gloss = np.clip(1 - np.hypot(fx - 0.3, fy - 0.25) * 3, 0, 1)
        rgb = rgb + gloss[..., None] * 60
    elif kind == 'feather':
        # overlapping feathers in staggered rows: a light vane that darkens toward its edge, a shaft down the middle
        FW, FH = 6, 5
        row = yy // FH
        lx = (xx + (row % 2) * (FW // 2)) % FW
        ly = yy % FH
        vane = 1.0 - np.abs(lx - FW / 2 + 0.5) / (FW / 2)
        rgb = base[None, None] * (0.7 + 0.25 * vane[..., None] + 0.15 * n[..., None] + 0.08 * (ly / FH)[..., None])
        rgb = np.where((lx == FW // 2)[..., None], rgb * 0.88, rgb)
        rgb = np.where((ly == FH - 1)[..., None] | (lx == 0)[..., None], second[None, None] * 0.85 + rgb * 0.15, rgb)
        rgb = np.where((fy > 0.8)[..., None], rgb * 0.85 + second * 0.15, rgb)
    elif kind == 'scale':
        row = yy // 3
        cx = (xx + (row % 2) * 2) % 4
        rim = (cx == 0) | (yy % 3 == 0)
        rgb = base[None, None] * (0.88 + 0.2 * n[..., None])
        rgb = np.where(rim[..., None], second[None, None], rgb)
    elif kind == 'metal':
        brush = value_noise(rng, max(2, w), w, h)[:1].repeat(h, 0)
        rgb = base[None, None] * (0.8 + 0.25 * brush[..., None] + 0.1 * n[..., None])
        for (ry, rx) in ((1, 1), (1, w - 2), (h - 2, 1), (h - 2, w - 2)):
            if 0 <= ry < h and 0 <= rx < w and w > 6 and h > 6: rgb[ry, rx] = second
    elif kind == 'crystal':
        k = 0.75 + 0.5 * ((xx * 3 + yy * 5) // 7 % 3) / 3
        rgb = (base[None, None] * k[..., None]) * 0.8 + second * 0.2
        edge = (np.abs(fx - fy) < 0.06) | (np.abs(fx + fy - 1) < 0.06)
        rgb = np.where(edge[..., None], np.minimum(255, base * 1.4), rgb)
        alpha[:] = 235
    elif kind == 'glow':
        d = np.hypot(fx - 0.5, fy - 0.5) * 2
        rgb = second[None, None] * (1 - np.clip(1 - d, 0, 1)[..., None]) + np.minimum(255, base * 1.25)[None, None] * np.clip(1 - d, 0, 1)[..., None]
        rgb = np.maximum(rgb, base * 0.8)
    elif kind == 'eye':
        rgb = np.broadcast_to(second, (h, w, 3)).copy()
        if face == 'front':
            d = np.hypot(fx - 0.5, fy - 0.5)
            rgb = np.where((d < 0.42)[..., None], base[None, None], rgb)
            rgb = np.where((d < 0.18)[..., None], np.array([15.0, 15, 20])[None, None], rgb)
            rgb = np.where((np.hypot(fx - 0.35, fy - 0.32) < 0.1)[..., None], np.array([255.0, 255, 255])[None, None], rgb)
    elif kind == 'cloud':
        rgb = base[None, None] * (0.85 + 0.3 * n[..., None])
        alpha = 200 + 55 * n
    elif kind == 'water':
        waves = np.sin(yy * 0.9 + np.sin(xx * 0.5) * 2 + n * 6) * 0.5 + 0.5
        rgb = base[None, None] * (0.8 + 0.3 * waves[..., None])
        alpha = 170 + 70 * waves
    elif kind == 'cloth':
        weave = ((xx + yy) % 2) * 0.05
        rgb = base[None, None] * (0.85 + 0.2 * n[..., None] + weave[..., None])
        hem = fy > 0.9
        rgb = np.where(hem[..., None], second[None, None], rgb)
    elif kind == 'bone':
        rgb = base[None, None] * (0.9 + 0.15 * n[..., None])
        crack = (np.abs(np.sin(xx * 0.8 + yy * 1.3 + n * 5)) < 0.05)
        rgb = np.where(crack[..., None], rgb * 0.7, rgb)
    elif kind == 'leather':
        rgb = base[None, None] * (0.85 + 0.2 * n[..., None])
        stitch = ((yy == 1) | (yy == h - 2)) & (xx % 3 == 0)
        rgb = np.where(stitch[..., None], second[None, None], rgb)
    elif kind == 'dark':
        rgb = base[None, None] * (0.8 + 0.3 * n[..., None])
    else:  # 'skin' and anything else: soft mottled
        spots = (fbm(rng, w, h, 4) > 0.62)
        rgb = base[None, None] * (0.86 + 0.22 * n[..., None])
        rgb = np.where(spots[..., None], rgb * 0.85 + second * 0.15, rgb)
    # lighting per face, soft top-to-bottom occlusion, crisp edges
    light = {'top': 1.12, 'bottom': 0.72, 'front': 1.0, 'back': 0.88, 'left': 0.93, 'right': 0.93}[face]
    k = light * (1.04 - 0.1 * fy) if face not in ('top', 'bottom') else light
    rgb = rgb * k[..., None] if np.ndim(k) else rgb * k
    if kind not in ('glow', 'eye') and w > 2 and h > 2:
        rgb[0, :] *= 1.08; rgb[:, 0] *= 1.05; rgb[-1, :] *= 0.82; rgb[:, -1] *= 0.86
    img[y0:y0 + h, x0:x0 + w] = np.clip(rgb, 0, 255)
    al[y0:y0 + h, x0:x0 + w] = np.clip(alpha, 0, 255)


def all_boxes(parts):
    for p in parts:
        for b in p.boxes: yield b
        yield from all_boxes(p.children)


def pack(model):
    """Shelf-packs every box's UV island; returns (texture width, height) in units."""
    boxes = sorted(all_boxes(model.parts), key=lambda b: -(b.s[2] + b.s[1]))
    for W in (64, 128, 256):
        x = y = shelf = 0
        ok = True
        for b in boxes:
            w, h, d = b.s
            iw, ih = 2 * (w + d), d + h
            if iw > W: ok = False; break
            if x + iw > W: x = 0; y += shelf; shelf = 0
            b.uv = (x, y); x += iw; shelf = max(shelf, ih)
        H = y + shelf
        if ok:
            for Hp in (32, 64, 128, 256):
                if H <= Hp and Hp <= W: return W, Hp
    raise ValueError(f'{model.cls}: does not fit a 256x256 texture')


def paint(model):
    W, H = pack(model)
    rng = np.random.default_rng(sum(map(ord, model.cls)))
    img = np.zeros((H * DENSITY, W * DENSITY, 3)); al = np.zeros((H * DENSITY, W * DENSITY))
    D = DENSITY
    for b in all_boxes(model.parts):
        u, v = b.uv; w, h, d = b.s
        mat = model.mats[b.mat]
        faces = {'top': (u + d, v, w, d), 'bottom': (u + d + w, v, w, d), 'right': (u, v + d, d, h),
                 'front': (u + d, v + d, w, h), 'left': (u + d + w, v + d, d, h), 'back': (u + 2 * d + w, v + d, w, h)}
        for face, (fx, fy, fw, fh) in faces.items():
            paint_face(img, al, fx * D, fy * D, fw * D, fh * D, mat, face, rng)
    TEX.mkdir(parents=True, exist_ok=True)
    Image.fromarray(np.dstack([img, al]).astype(np.uint8), 'RGBA').save(TEX / f'{model.texture}.png')
    return W, H


# ================================================================================================ Java output
def jf(v):
    v = float(v)
    return (f'{v:.4f}'.rstrip('0').rstrip('.') or '0') + 'f'


def java(model, W, H):
    fields, lookups, defs = [], [], []

    counter = [0]

    def walk(p, parent_var, path):
        counter[0] += 1
        var = f'p{counter[0]}'
        cub = 'CubeListBuilder.create()'
        for b in p.boxes:
            cub += f'.texOffs({b.uv[0]}, {b.uv[1]})' + ('.mirror()' if b.mirror else '')
            dims = f'{jf(b.o[0])}, {jf(b.o[1])}, {jf(b.o[2])}, {b.s[0]}, {b.s[1]}, {b.s[2]}'
            cub += f'.addBox({dims}, new CubeDeformation({jf(b.inflate)}))' if b.inflate else f'.addBox({dims})'
            if b.mirror: cub += '.mirror(false)'
        pose = (f'PartPose.offsetAndRotation({jf(p.pivot[0])}, {jf(p.pivot[1])}, {jf(p.pivot[2])}, {jf(p.rot[0])}, {jf(p.rot[1])}, {jf(p.rot[2])})'
                if any(p.rot) else f'PartPose.offset({jf(p.pivot[0])}, {jf(p.pivot[1])}, {jf(p.pivot[2])})')
        defs.append(f'        PartDefinition {var} = {parent_var}.addOrReplaceChild("{p.name}", {cub}, {pose});')
        if p.field:
            fields.append(f'    final ModelPart {p.field};')
            lookups.append(f'        {p.field} = root' + ''.join(f'.getChild("{n}")' for n in path + [p.name]) + ';')
        for c in p.children: walk(c, var, path + [p.name])

    for p in model.parts: walk(p, 'root', [])
    state = model.state
    state_decl = ''
    if state == 'State':
        state_decl = ('    /** Extra render state, filled in by the renderer. */\n    public static class State extends LivingEntityRenderState {\n'
                      + ''.join(f'        public boolean {f};\n' for f in model.flags) + '    }\n\n')
    return f'''package com.stormreach.client.model.gen;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

/**
 * {model.doc}
 * Generated by tools/mobmodels.py (texture textures/entity/{model.texture}.png, {W}x{H} units at {DENSITY}x). Don't edit.
 */
@SuppressWarnings("unused")
public class {model.cls} extends EntityModel<{model.cls + '.State' if state == 'State' else state}> {{
{state_decl}{chr(10).join(fields)}

    public {model.cls}(ModelPart root) {{
        super(root);
{chr(10).join(lookups)}
    }}

    public static LayerDefinition createBodyLayer() {{
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
{chr(10).join(defs)}
        return LayerDefinition.create(mesh, {W}, {H});
    }}

    @Override
    public void setupAnim({model.cls + '.State' if state == 'State' else state} s) {{
        super.setupAnim(s);
        float t = s.ageInTicks, walk = s.walkAnimationPos * 0.6662f, amt = Math.min(1f, s.walkAnimationSpeed);
{model.anim}
    }}
{model.extra}}}
'''


# ================================================================================================ the creatures
M = []
P = Part

# ---- Cliff Crab and Thunder Crab ------------------------------------------------------------------------------
def crab(cls, tex, k, mats, spines, doc):
    s = lambda v: v * k
    body = P('body', (0, 24 - s(5), 0), boxes=[Box(-s(5), -s(2), -s(4), s(10), s(4), s(8), 'shell'),
                                               Box(-s(4), -s(3), -s(3), s(8), s(1), s(6), 'shell2'),
                                               Box(-s(4.5), s(2), -s(3.5), s(9), s(1), s(7), 'belly'),
                                               Box(-s(1.5), 0, -s(4.6), s(3), s(1), 1, 'dark')], field='body')
    if spines:
        for i, (x, z) in enumerate(((-3, -2), (0, -1), (3, -2), (-2, 2), (2, 2))):
            body.add(P(f'spine_{i}', (s(x), -s(3), s(z)), (0.15 * (z < 0) - 0.15 * (z > 0), 0, 0.2 * (x / 3)),
                       [Box(-1, -s(3), -1, 2, s(3), 2, 'crystal')]))
        body.add(P('antenna', (0, -s(3), s(1)), boxes=[Box(-0.5, -s(5), -0.5, 1, s(5), 1, 'metal'), Box(-1, -s(6), -1, 2, 2, 2, 'glow')], field='antenna'))
    eye = P('left_eye', (s(2), -s(2), -s(3.5)), boxes=[Box(-0.5, -s(3), -0.5, 1, s(3), 1, 'shell2'), Box(-1, -s(3) - 2, -1, 2, 2, 2, 'eye')], field='leftEye')
    body.add(eye, mirror_part(eye))
    claw = P('left_claw', (s(5), s(0.5), -s(3)), (0, -0.35, 0), [Box(0, -1, -1, s(3), 2, 2, 'shell')], field='leftClaw').add(
        P('left_hand', (s(3), 0, 0), boxes=[Box(-1, -s(1.5), -s(4), s(3), s(3), s(4), 'claw')]).add(
            P('left_pincer', (0, s(1.5), -s(4)), boxes=[Box(-0.5, 0, -s(3), s(2), 1, s(3), 'claw2')], field='leftPincer')))
    body.add(claw, mirror_part(claw))
    for i in range(3):
        leg = P(f'left_leg_{i}', (s(5), s(1), -s(2) + i * s(2.2)), (0, 0, 0.5), [Box(0, -0.5, -0.5, s(4), 1, 1, 'shell2')], field=f'leftLeg{i}').add(
            P(f'left_foot_{i}', (s(4), 0, 0), (0, 0, 0.9), [Box(-0.5, 0, -0.5, 1, s(4), 1, 'shell2')]))
        body.add(leg, mirror_part(leg))
    anim = '''        body.y = 24f - {h}f + Mth.sin(t * 0.1f) * 0.15f;
''' .replace('{h}', jf(s(5))[:-1]) + ''.join(f'''        leftLeg{i}.yRot = Mth.cos(walk * 2.4f + {i * 2.1:.1f}f) * 0.5f * amt;
        rightLeg{i}.yRot = -Mth.cos(walk * 2.4f + {i * 2.1:.1f}f) * 0.5f * amt;
        leftLeg{i}.zRot = 0.5f + Mth.sin(walk * 2.4f + {i * 2.1:.1f}f) * 0.3f * amt;
        rightLeg{i}.zRot = -0.5f - Mth.sin(walk * 2.4f + {i * 2.1:.1f}f) * 0.3f * amt;
''' for i in range(3)) + '''        leftPincer.xRot = 0.15f + Mth.sin(t * 0.25f) * 0.25f;
        rightPincer.xRot = 0.15f - Mth.sin(t * 0.25f + 1f) * 0.25f;
        leftClaw.xRot = Mth.sin(t * 0.08f) * 0.1f;
        rightClaw.xRot = -Mth.sin(t * 0.08f) * 0.1f;
        leftEye.zRot = Mth.sin(t * 0.07f) * 0.15f;
        rightEye.zRot = -Mth.cos(t * 0.06f) * 0.15f;
''' + ('        antenna.zRot = Mth.sin(t * 0.3f) * 0.08f;\n' if spines else '')
    M.append(Model(cls, tex, mats, [body], anim, doc=doc))

crab('CliffCrabModel', 'cliff_crab', 1, {'shell': ('shell', C('#b5522f'), C('#6e2a16')), 'shell2': ('shell', C('#d0703c'), C('#7a3418')),
     'belly': ('skin', C('#e9c9a0'), C('#c49a6c')), 'dark': ('dark', C('#3a1a10')), 'claw': ('shell', C('#c95f32'), C('#5c2410')),
     'claw2': ('shell', C('#e6d2b8'), C('#a08060')), 'eye': ('eye', C('#1c1c1c'), C('#2a2a2a'))}, False,
     'A shore crab: ridged shell, jointed legs, eye stalks that sway, and claws whose pincers snap.')
crab('ThunderCrabModel', 'thunder_crab', 1.5, {'shell': ('shell', C('#2f4a66'), C('#16243a')), 'shell2': ('shell', C('#3f6488'), C('#1c3048')),
     'belly': ('skin', C('#9fb6c8'), C('#6a8296')), 'dark': ('dark', C('#0c1420')), 'claw': ('shell', C('#355878'), C('#121e30')),
     'claw2': ('metal', C('#b8c4cc'), C('#606a72')), 'eye': ('eye', C('#7fd4ff'), C('#10202c')), 'crystal': ('crystal', C('#7fd4ff'), C('#2a6a96')),
     'metal': ('metal', C('#9aa4ae'), C('#4a525a')), 'glow': ('glow', C('#c8f0ff'), C('#3a8ac0'))}, True,
     'The Thunder Crab: a storm-charged giant crab with crystal spines and a lightning antenna.')

# ---- Storm Gull and Stormhawk --------------------------------------------------------------------------------
def bird(cls, tex, mats, k, raptor, doc):
    s = lambda v: v * k
    body = P('body', (0, 24 - s(6), 0), boxes=[Box(-s(2), -s(2), -s(4), s(4), s(4), s(8), 'feather'), Box(-s(1.5), s(1), -s(3), s(3), 1, s(5), 'belly')], field='body')
    head = P('head', (0, -s(2), -s(4)), boxes=[Box(-s(1.5), -s(3), -s(3), s(3), s(3), s(3), 'feather2')], field='head').add(
        P('beak', (0, -s(1.5), -s(3)), (0.2 if raptor else 0, 0, 0), [Box(-0.5, -0.5, -s(2.5), 1, 1 + raptor, s(2.5), 'beak')]),
        P('left_eye', (s(1.5), -s(2), -s(2)), boxes=[Box(0, -0.5, -0.5, 1, 1, 1, 'eye')]),
        P('right_eye', (-s(1.5), -s(2), -s(2)), boxes=[Box(-1, -0.5, -0.5, 1, 1, 1, 'eye')]))
    if raptor:
        head.add(P('crest', (0, -s(3), -s(1)), (-0.5, 0, 0), [Box(-0.5, -s(3), 0, 1, s(3), s(2), 'accent')]))
    body.add(head)
    wing = P('left_wing', (s(2), -s(1.5), -s(2)), boxes=[Box(0, 0, 0, s(5), 1, s(6), 'feather')], field='leftWing').add(
        P('left_wingtip', (s(5), 0, 0), boxes=[Box(0, 0, 0, s(5 if raptor else 6), 1, s(5), 'tip')], field='leftWingtip'))
    body.add(wing, mirror_part(wing))
    body.add(P('tail', (0, -s(1), s(4)), (-0.2, 0, 0), [Box(-s(2), 0, 0, s(4), 1, s(4), 'tail')], field='tail'))
    leg = P('left_leg', (s(1), s(2), s(1)), boxes=[Box(-0.5, 0, -0.5, 1, s(3), 1, 'beak'), Box(-1, s(3), -1.5, 2, 1, 2, 'beak')], field='leftLeg')
    body.add(leg, mirror_part(leg))
    anim = f'''        float flap = Mth.sin(t * {0.45 if not raptor else 0.3}f);
        leftWing.zRot = -0.15f + flap * 0.7f;
        rightWing.zRot = 0.15f - flap * 0.7f;
        leftWingtip.zRot = flap * 0.4f;
        rightWingtip.zRot = -flap * 0.4f;
        head.yRot = s.yRot * Mth.DEG_TO_RAD;
        head.xRot = s.xRot * Mth.DEG_TO_RAD;
        tail.xRot = -0.2f + Mth.sin(t * 0.2f) * 0.1f;
        leftLeg.xRot = Mth.cos(walk) * 0.8f * amt;
        rightLeg.xRot = -Mth.cos(walk) * 0.8f * amt;
        body.y = 24f - {s(6)}f + Mth.sin(t * 0.45f) * 0.4f;
'''
    M.append(Model(cls, tex, mats, [body], anim, doc=doc))

bird('StormGullModel', 'storm_gull', {'feather': ('feather', C('#e8ecf0'), C('#9aa6b0')), 'feather2': ('feather', C('#f6f8fa'), C('#b0bcc6')),
     'belly': ('feather', C('#ffffff'), C('#d8dee4')), 'beak': ('shell', C('#f2b13c'), C('#b0701c')), 'eye': ('eye', C('#121212'), C('#202020')),
     'tail': ('feather', C('#4a5560'), C('#2a3038')), 'tip': ('feather', C('#3a4048'), C('#f0f2f4'))}, 1, False, 'The Storm Gull: a grey-backed gull with two-jointed wings, a fanned tail and webbed feet.')
bird('StormhawkModel', 'stormhawk', {'feather': ('feather', C('#5a4a8a'), C('#2c2448')), 'feather2': ('feather', C('#6e5ca8'), C('#2c2448')),
     'belly': ('feather', C('#c8c0e0'), C('#8a80b0')), 'beak': ('shell', C('#f2c94c'), C('#8a6a1e')), 'eye': ('eye', C('#ffd24a'), C('#1a1420')),
     'tail': ('feather', C('#3a3060'), C('#f2c94c')), 'accent': ('feather', C('#f2c94c'), C('#a07a20')), 'tip': ('feather', C('#7a68c0'), C('#f2c94c'))}, 1.5, True,
     'The Stormhawk: a violet raptor with a hooked beak, a golden crest, broad jointed wings and a banded tail.')

# ---- Harbor Seal ---------------------------------------------------------------------------------------------
seal = P('body', (0, 20, 0), boxes=[Box(-4, -3, -6, 8, 6, 8, 'skin'), Box(-3.5, 2.5, -5.5, 7, 1, 7, 'belly')], field='body').add(
    P('rear', (0, 0, 2), boxes=[Box(-3, -2.5, 0, 6, 5, 6, 'skin')], field='rear').add(
        P('tail', (0, 0.5, 6), boxes=[Box(-3, -0.5, 0, 6, 1, 3, 'dark')], field='tail')),
    P('head', (0, -2, -6), boxes=[Box(-3, -3, -4, 6, 5, 5, 'skin'), Box(-1.5, -1, -6, 3, 2, 2, 'snout'),
                                  Box(-3.5, -1, -5.5, 1, 1, 1, 'whisker'), Box(2.5, -1, -5.5, 1, 1, 1, 'whisker')], field='head').add(
        P('left_eye', (2, -2, -4), boxes=[Box(-0.5, -0.5, -0.5, 1, 1, 1, 'eye')]), P('right_eye', (-2, -2, -4), boxes=[Box(-0.5, -0.5, -0.5, 1, 1, 1, 'eye')])))
flip = P('left_flipper', (4, 2, -3), (0, 0, 0.6), [Box(0, -0.5, -1.5, 4, 1, 3, 'dark')], field='leftFlipper')
seal.add(flip, mirror_part(flip))
M.append(Model('HarborSealModel', 'harbor_seal', {'skin': ('skin', C('#8a8e94'), C('#5a5e66')), 'belly': ('skin', C('#c4c6c8'), C('#9a9ca0')),
    'dark': ('skin', C('#5a5e66'), C('#3a3e44')), 'snout': ('skin', C('#a6a8ac'), C('#6a6c70')), 'whisker': ('bone', C('#e8e8e0')),
    'eye': ('eye', C('#101010'), C('#202020'))}, [seal], '''        rear.yRot = Mth.sin(walk * 1.5f) * 0.25f * amt + Mth.sin(t * 0.05f) * 0.05f;
        tail.xRot = Mth.sin(t * 0.15f) * 0.2f;
        leftFlipper.zRot = 0.6f + Mth.sin(walk * 2f) * 0.5f * amt;
        rightFlipper.zRot = -0.6f - Mth.sin(walk * 2f) * 0.5f * amt;
        head.yRot = s.yRot * Mth.DEG_TO_RAD;
        head.xRot = s.xRot * Mth.DEG_TO_RAD;''', doc='The Harbor Seal: a round two-part body that wiggles, whiskered snout, front flippers and a tail fluke.'))

# ---- Squall and Stormwarden ----------------------------------------------------------------------------------
squall = P('core', (0, 14, 0), boxes=[Box(-3, -3, -3, 6, 6, 6, 'glow')], field='core').add(
    P('face', (0, 0, -3), boxes=[Box(-2, -1, -0.5, 1, 1, 1, 'eye'), Box(1, -1, -0.5, 1, 1, 1, 'eye')]))
for r, (y, rad, n) in enumerate(((-4, 6, 6), (1, 8, 8), (6, 5, 5))):
    ring = P(f'ring_{r}', (0, y, 0), field=f'ring{r}')
    for i in range(n):
        a = i * 2 * math.pi / n
        ring.add(P(f'puff_{r}_{i}', (math.cos(a) * rad, 0, math.sin(a) * rad), (0, -a, 0), [Box(-1.5, -1, -1, 3, 2, 2, 'cloud')]))
    squall.add(ring)
tail = P('wisp_0', (0, 3, 0), boxes=[Box(-2, 0, -2, 4, 3, 4, 'cloud')], field='wisp0').add(
    P('wisp_1', (0, 3, 0), boxes=[Box(-1.5, 0, -1.5, 3, 3, 3, 'cloud')], field='wisp1').add(
        P('wisp_2', (0, 3, 0), boxes=[Box(-1, 0, -1, 2, 3, 2, 'cloud')])))
squall.add(tail)
M.append(Model('SquallModel', 'squall', {'glow': ('glow', C('#e8f6ff'), C('#7fb0d0')), 'eye': ('eye', C('#1a3448'), C('#1a3448')),
    'cloud': ('cloud', C('#c8d4dc'), C('#8a98a4'))}, [squall], '''        ring0.yRot = t * 0.3f;
        ring1.yRot = -t * 0.22f;
        ring2.yRot = t * 0.4f;
        core.y = 14f + Mth.sin(t * 0.15f) * 1.2f;
        wisp0.zRot = Mth.sin(t * 0.2f) * 0.2f;
        wisp1.xRot = Mth.cos(t * 0.25f) * 0.25f;''', doc='The Squall: a glowing storm-core in three counter-spinning rings of cloud, trailing a wispy tail.'))

warden = P('body', (0, 2, 0), boxes=[Box(-7, -12, -4, 14, 12, 8, 'stone'), Box(-3, -9, -4.5, 6, 6, 1, 'glow')], field='body')
warden.add(P('head', (0, -12, 0), boxes=[Box(-4, -7, -4, 8, 7, 8, 'stone'), Box(-3, -5, -4.5, 6, 2, 1, 'glow')], field='head').add(
    P('crown_0', (-3, -7, 0), (0, 0, -0.3), [Box(-1, -4, -1, 2, 4, 2, 'crystal')]), P('crown_1', (3, -7, 0), (0, 0, 0.3), [Box(-1, -4, -1, 2, 4, 2, 'crystal')]),
    P('crown_2', (0, -7, 2), (0.3, 0, 0), [Box(-1, -5, -1, 2, 5, 2, 'crystal')])))
arm = P('left_arm', (7, -10, 0), boxes=[Box(0, -2, -3, 5, 5, 6, 'metal'), Box(0.5, 3, -2.5, 4, 8, 5, 'stone')], field='leftArm').add(
    P('left_forearm', (2.5, 11, 0), boxes=[Box(-2.5, 0, -3, 5, 9, 6, 'stone'), Box(-3, 9, -3.5, 6, 3, 7, 'metal')], field='leftForearm'),
    P('left_spike', (2.5, -2, 0), (0, 0, 0.4), [Box(-1, -4, -1, 2, 4, 2, 'crystal')]))
warden.add(arm, mirror_part(arm))
leg = P('left_leg', (3.5, 2, 0), boxes=[Box(-3, 0, -3, 6, 10, 6, 'stone')], field='leftLeg').add(
    P('left_foot', (0, 10, 0), boxes=[Box(-3.5, 0, -4, 7, 12, 7, 'stone'), Box(-3.5, 10, -5, 7, 2, 1, 'metal')]))
root_w = [warden, leg, mirror_part(leg)]
M.append(Model('StormwardenModel', 'stormwarden', {'stone': ('shell', C('#5a6270'), C('#2c3038')), 'glow': ('glow', C('#bdf0ff'), C('#3a8ac0')),
    'crystal': ('crystal', C('#7fd4ff'), C('#2a6a96')), 'metal': ('metal', C('#8a9298'), C('#40464c'))}, root_w, '''        head.yRot = s.yRot * Mth.DEG_TO_RAD;
        head.xRot = s.xRot * Mth.DEG_TO_RAD;
        leftLeg.xRot = Mth.cos(walk * 0.6f) * 0.9f * amt;
        rightLeg.xRot = -Mth.cos(walk * 0.6f) * 0.9f * amt;
        leftArm.xRot = -Mth.cos(walk * 0.6f) * 0.7f * amt + Mth.sin(t * 0.06f) * 0.05f;
        rightArm.xRot = Mth.cos(walk * 0.6f) * 0.7f * amt - Mth.sin(t * 0.06f) * 0.05f;
        leftForearm.xRot = -0.25f - Math.abs(Mth.cos(walk * 0.6f)) * 0.3f * amt;
        rightForearm.xRot = -0.25f - Math.abs(Mth.sin(walk * 0.6f)) * 0.3f * amt;''',
    doc='The Stormwarden: a towering storm-stone golem with a crystal crown, plated shoulders, glowing visor and core.'))

# ---- Ball Lightning and Static Wisp --------------------------------------------------------------------------
orb = P('core', (0, 16, 0), boxes=[Box(-3, -3, -3, 6, 6, 6, 'glow'), Box(-4, -2, -2, 8, 4, 4, 'glow2'), Box(-2, -4, -2, 4, 8, 4, 'glow2'),
                                   Box(-2, -2, -4, 4, 4, 8, 'glow2')], field='core')
for i in range(3):
    arc = P(f'arc_{i}', (0, 0, 0), (i * 1.0, i * 0.7, 0), field=f'arc{i}')
    for j in range(6):
        a = j * math.pi / 3
        arc.add(P(f'bolt_{i}_{j}', (math.cos(a) * 6, 0, math.sin(a) * 6), (0, -a, 0.3 * (j % 2)), [Box(-1, -0.5, -0.5, 2, 1, 1, 'spark')]))
    orb.add(arc)
M.append(Model('BallLightningModel', 'ball_lightning', {'glow': ('glow', C('#ffffff'), C('#9fe0ff')), 'glow2': ('glow', C('#bfeaff'), C('#4a9ad0')),
    'spark': ('glow', C('#fff6c0'), C('#c0e8ff'))}, [orb], '''        core.y = 16f + Mth.sin(t * 0.3f) * 0.8f;
        core.yRot = t * 0.2f;
        arc0.yRot = t * 0.6f; arc1.xRot = t * 0.5f; arc2.zRot = t * 0.7f;''', doc='Ball Lightning: a white-hot core in three spinning arcs of sparks.'))

wisp = P('core', (0, 14, 0), boxes=[Box(-2.5, -2.5, -2.5, 5, 5, 5, 'glow'), Box(-1.5, -1, -2.8, 1, 1, 1, 'eye'), Box(0.5, -1, -2.8, 1, 1, 1, 'eye')], field='core')
for i in range(5):
    a = i * 2 * math.pi / 5
    wisp.add(P(f'spike_{i}', (math.cos(a) * 2.5, -2.5, math.sin(a) * 2.5), (math.sin(a) * 0.5, 0, -math.cos(a) * 0.5), [Box(-0.5, -4, -0.5, 1, 4, 1, 'spark')]))
trail = P('trail_0', (0, 2.5, 0), boxes=[Box(-1.5, 0, -1.5, 3, 3, 3, 'glow2')], field='trail0').add(
    P('trail_1', (0, 3, 0), boxes=[Box(-1, 0, -1, 2, 3, 2, 'glow2')], field='trail1').add(P('trail_2', (0, 3, 0), boxes=[Box(-0.5, 0, -0.5, 1, 3, 1, 'spark')])))
wisp.add(trail)
M.append(Model('StaticWispModel', 'static_wisp', {'glow': ('glow', C('#fff2a8'), C('#d0a020')), 'glow2': ('glow', C('#ffe070'), C('#a07010')),
    'spark': ('glow', C('#ffffff'), C('#ffe070')), 'eye': ('eye', C('#3a2a00'), C('#3a2a00'))}, [wisp], '''        core.y = 14f + Mth.sin(t * 0.2f) * 1.5f;
        core.yRot = t * 0.15f;
        trail0.xRot = Mth.sin(t * 0.3f) * 0.3f;
        trail1.zRot = Mth.cos(t * 0.35f) * 0.35f;''', doc='The Static Wisp: a crackling golden spark with a crown of spikes and a trailing tail.'))

# ---- Waterspout (mob) and Cyclone: stacked rings of spinning water or wind --------------------------------------
def spout(cls, tex, mats, doc):
    parts = []
    for i in range(6):
        rad = 2 + i * 1.3
        ring = P(f'ring_{i}', (0, 24 - i * 4, 0), field=f'ring{i}')
        n = 6 + i
        for j in range(n):
            a = j * 2 * math.pi / n
            ring.add(P(f'seg_{i}_{j}', (math.cos(a) * rad, 0, math.sin(a) * rad), (0, -a, 0), [Box(-1.5, -2, -0.5, 3, 4, 1, 'a' if (i + j) % 2 else 'b')]))
        parts.append(ring)
    anim = ''.join(f'        ring{i}.yRot = t * {0.35 - i * 0.03:.2f}f + {i * 0.4:.1f}f;\n        ring{i}.x = Mth.sin(t * 0.1f + {i * 0.5:.1f}f) * {i * 0.3:.1f}f;\n' for i in range(6))
    M.append(Model(cls, tex, mats, parts, anim, doc=doc))

spout('WaterspoutModel', 'waterspout', {'a': ('water', C('#7fbfe0'), C('#3a7aa0')), 'b': ('water', C('#bfe6f6'), C('#5a9ac0'))},
      'The Waterspout: six rings of spinning water, wider toward the top, that sway as they turn.')
spout('CycloneModel', 'cyclone', {'a': ('cloud', C('#b8c4cc'), C('#7a8892')), 'b': ('cloud', C('#dde4ea'), C('#9aa6b0'))},
      'The Cyclone: six rings of spinning wind and cloud, wider toward the top, that sway as they turn.')

# ---- Tempest Sovereign ---------------------------------------------------------------------------------------
sov = P('body', (0, -3, 0), boxes=[Box(-5, -2, -3, 10, 12, 6, 'armor'), Box(-5.5, -2.5, -3.5, 11, 3, 7, 'metal'), Box(-2, 1, -3.6, 4, 4, 1, 'glow')], field='body')
sov.add(P('head', (0, -2, 0), boxes=[Box(-4, -8, -4, 8, 8, 8, 'skin'), Box(-4.5, -9, -4.5, 9, 2, 9, 'metal'), Box(-3, -5, -4.2, 2, 1, 1, 'eye'),
                                     Box(1, -5, -4.2, 2, 1, 1, 'eye')], field='head').add(
    *[P(f'horn_{i}', (x, -9, z), (rx, 0, rz), [Box(-0.5, -5, -0.5, 1, 5, 1, 'crystal')]) for i, (x, z, rx, rz) in
      enumerate(((-3.5, -3.5, -0.3, -0.3), (3.5, -3.5, -0.3, 0.3), (-3.5, 3.5, 0.3, -0.3), (3.5, 3.5, 0.3, 0.3), (0, -4, -0.4, 0)))]))
arm = P('left_arm', (6.5, -1, 0), boxes=[Box(-1.5, 0, -1.5, 3, 7, 3, 'armor'), Box(-2, -1.5, -2, 4, 3, 4, 'metal')], field='leftArm').add(
    P('left_hand', (0, 7, 0), boxes=[Box(-1.5, 0, -1.5, 3, 5, 3, 'skin'), Box(-2, 3, -2, 4, 2, 4, 'glow')], field='leftHand'))
sov.add(arm, mirror_part(arm))
cape = P('cape', (0, -1.5, 3), boxes=[Box(-5, 0, 0, 10, 14, 1, 'cloth')], field='cape').add(P('cape_low', (0, 14, 0), boxes=[Box(-5, 0, 0, 10, 8, 1, 'cloth')], field='capeLow'))
sov.add(cape)
vortex = P('skirt', (0, 10, 0), boxes=[Box(-6, 0, -4, 12, 5, 8, 'cloth')], field='skirt').add(
    P('vortex_0', (0, 5, 0), boxes=[Box(-5, 0, -3.5, 10, 4, 7, 'cloud')], field='vortex0').add(
        P('vortex_1', (0, 4, 0), boxes=[Box(-3.5, 0, -2.5, 7, 4, 5, 'cloud')], field='vortex1').add(
            P('vortex_2', (0, 4, 0), boxes=[Box(-2, 0, -1.5, 4, 4, 3, 'cloud')], field='vortex2'))))
sov.add(vortex)
halo = P('ring', (0, -17, 0), field='ring')
for i in range(12):
    a = i * math.pi / 6
    halo.add(P(f'halo_{i}', (math.cos(a) * 7, 0, math.sin(a) * 7), (0, -a, 0), [Box(-1, -1, -0.5, 2, 2, 1, 'glow' if i % 3 == 0 else 'crystal')]))
orbs = [P('orb_a', (12, 2, 0), boxes=[Box(-1.5, -1.5, -1.5, 3, 3, 3, 'glow')], field='orbA'), P('orb_b', (-12, 2, 0), boxes=[Box(-1.5, -1.5, -1.5, 3, 3, 3, 'glow')], field='orbB')]
M.append(Model('SovereignModel', 'tempest_sovereign', {'armor': ('metal', C('#3a3460'), C('#f2c94c')), 'metal': ('metal', C('#c8a440'), C('#6a5418')),
    'skin': ('skin', C('#9aa6c8'), C('#5a6688')), 'eye': ('glow', C('#ffffff'), C('#7fd4ff')), 'glow': ('glow', C('#e8f8ff'), C('#5ab0e8')),
    'crystal': ('crystal', C('#7fd4ff'), C('#2a6a96')), 'cloth': ('cloth', C('#2c2450'), C('#f2c94c')), 'cloud': ('cloud', C('#8a96b0'), C('#4a5670'))},
    [sov, halo] + orbs, '''        head.yRot = s.yRot * Mth.DEG_TO_RAD;
        head.xRot = s.xRot * Mth.DEG_TO_RAD;
        leftArm.zRot = -0.35f - Mth.sin(t * 0.08f) * 0.25f;
        rightArm.zRot = 0.35f + Mth.sin(t * 0.08f) * 0.25f;
        leftArm.xRot = Mth.cos(t * 0.06f) * 0.3f - 0.4f;
        rightArm.xRot = -Mth.cos(t * 0.06f) * 0.3f - 0.4f;
        leftHand.xRot = -0.3f + Mth.sin(t * 0.1f) * 0.2f;
        rightHand.xRot = -0.3f - Mth.sin(t * 0.1f) * 0.2f;
        cape.xRot = 0.12f + Mth.sin(t * 0.09f) * 0.08f;
        capeLow.xRot = 0.1f + Mth.sin(t * 0.09f + 0.8f) * 0.12f;
        skirt.zRot = Mth.sin(t * 0.1f) * 0.06f;
        vortex0.yRot = t * 0.2f; vortex1.yRot = -t * 0.3f; vortex2.yRot = t * 0.45f;
        ring.yRot = t * 0.12f;
        orbA.x = Mth.cos(t * 0.1f) * 12; orbA.z = Mth.sin(t * 0.1f) * 12; orbA.y = 2 + Mth.sin(t * 0.2f) * 2;
        orbB.x = -orbA.x; orbB.z = -orbA.z; orbB.y = 2 - Mth.sin(t * 0.2f) * 2;''',
    doc='The Tempest Sovereign: a crowned, armoured storm lord with crystal horns, a halo of lightning, a flowing cape, and a body that trails away into a spinning vortex.'))

# ---- Skyray ------------------------------------------------------------------------------------------------
ray = P('body', (0, 20, 0), boxes=[Box(-6, -1.5, -6, 12, 3, 12, 'skin'), Box(-5, 1.5, -5, 10, 1, 10, 'belly'), Box(-4, -2.5, -4, 8, 1, 8, 'skin2')], field='body')
ray.add(P('head', (0, 0, -6), boxes=[Box(-3, -1, -3, 6, 2, 3, 'skin')]).add(
    P('left_fin', (2.5, 0, -3), (0, -0.3, 0), [Box(-0.5, -0.5, -3, 1, 1, 3, 'skin2')]), P('right_fin', (-2.5, 0, -3), (0, 0.3, 0), [Box(-0.5, -0.5, -3, 1, 1, 3, 'skin2')]),
    P('left_eye', (3, -0.5, -1.5), boxes=[Box(0, -0.5, -0.5, 1, 1, 1, 'eye')]), P('right_eye', (-3, -0.5, -1.5), boxes=[Box(-1, -0.5, -0.5, 1, 1, 1, 'eye')])))
wing = P('left_wing', (6, 0, 0), boxes=[Box(0, -1, -5, 6, 2, 10, 'skin')], field='leftWing').add(
    P('left_wingtip', (6, 0, 0), boxes=[Box(0, -0.5, -3.5, 6, 1, 7, 'skin2')], field='leftWingtip'))
ray.add(wing, mirror_part(wing))
ray.add(P('tail', (0, 0, 6), boxes=[Box(-1, -1, 0, 2, 2, 6, 'skin2')], field='tail').add(
    P('tail_mid', (0, 0, 6), boxes=[Box(-0.5, -0.5, 0, 1, 1, 6, 'skin2')], field='tailMid').add(
        P('tail_tip', (0, 0, 6), boxes=[Box(-0.5, -0.5, 0, 1, 1, 5, 'glow')], field='tailTip'))))
ray.add(P('saddle', (0, -2.5, 0), boxes=[Box(-3.5, -1, -3, 7, 1, 6, 'leather')], field='saddle'))
M.append(Model('SkyrayModel', 'skyray', {'skin': ('skin', C('#4a6aa0'), C('#2a3c66')), 'skin2': ('skin', C('#5a7ec0'), C('#2a3c66')),
    'belly': ('skin', C('#d8e4f2'), C('#a0b4cc')), 'eye': ('eye', C('#f2e070'), C('#101828')), 'glow': ('glow', C('#c8f0ff'), C('#4a8ac0')),
    'leather': ('leather', C('#7a4a2a'), C('#d8b070'))}, [ray], '''        float flap = Mth.sin(t * 0.15f);
        leftWing.zRot = flap * 0.35f;
        rightWing.zRot = -flap * 0.35f;
        leftWingtip.zRot = Mth.sin(t * 0.15f - 0.6f) * 0.45f;
        rightWingtip.zRot = -Mth.sin(t * 0.15f - 0.6f) * 0.45f;
        tail.yRot = Mth.sin(t * 0.1f) * 0.25f;
        tailMid.yRot = Mth.sin(t * 0.1f - 0.7f) * 0.3f;
        tailTip.yRot = Mth.sin(t * 0.1f - 1.4f) * 0.35f;
        tail.xRot = Mth.cos(t * 0.12f) * 0.08f;
        saddle.visible = s.saddled;''', state='State', flags=('saddled',), doc='The Skyray: a broad manta of the storm sky, with two-jointed wing fins, head fins, and a three-jointed glowing whip tail.'))

# ---- Puffling (keeps its sheared state) ----------------------------------------------------------------------
puff = [P('body', (0, 16, 0), boxes=[Box(-4, -3, -5, 8, 6, 10, 'skin')], field='body'),
        P('fluff', (0, 16, 0), boxes=[Box(-6, -5, -7, 12, 9, 14, 'cloud')], field='fluff').add(
            P('fluff_top', (0, -5, 0), boxes=[Box(-4, -3, -5, 8, 3, 10, 'cloud')]),
            P('fluff_spark', (0, -8, 0), boxes=[Box(-1, -2, -1, 2, 2, 2, 'glow')], field='spark')),
        P('head', (0, 13, -5), boxes=[Box(-3, -4, -6, 6, 6, 6, 'skin'), Box(-2, -2, -7, 4, 2, 1, 'snout')], field='head').add(
            P('left_ear', (3, -3, -3), (0, 0, 0.5), [Box(0, -0.5, -1, 3, 1, 2, 'skin')], field='leftEar'),
            P('right_ear', (-3, -3, -3), (0, 0, -0.5), [Box(-3, -0.5, -1, 3, 1, 2, 'skin')], field='rightEar'),
            P('left_eye', (2, -2, -6), boxes=[Box(-0.5, -0.5, -0.5, 1, 1, 1, 'eye')]), P('right_eye', (-2, -2, -6), boxes=[Box(-0.5, -0.5, -0.5, 1, 1, 1, 'eye')]))]
for i in range(4):
    x, z = (-2.5 if i % 2 == 0 else 2.5), (-3.5 if i < 2 else 3.5)
    puff.append(P(f'leg_{i}', (x, 19, z), boxes=[Box(-1, 0, -1, 2, 5, 2, 'skin'), Box(-1.2, 4, -1.2, 2, 1, 2, 'dark')], field=f'leg{i}'))
M.append(Model('PufflingModel', 'puffling', {'skin': ('skin', C('#e6d6ee'), C('#a890b8')), 'cloud': ('cloud', C('#f4f6fa'), C('#c0c8d4')),
    'glow': ('glow', C('#fff2a8'), C('#c0a040')), 'snout': ('skin', C('#f2b8c8'), C('#c08898')), 'eye': ('eye', C('#1a1028'), C('#1a1028')),
    'dark': ('dark', C('#5a4a66'))}, puff, '''        fluff.visible = !s.sheared;
        spark.y = -8f + Mth.sin(t * 0.2f) * 0.5f;
        head.yRot = s.yRot * Mth.DEG_TO_RAD;
        head.xRot = s.xRot * Mth.DEG_TO_RAD;
        leftEar.zRot = 0.5f + Mth.sin(t * 0.1f) * 0.15f;
        rightEar.zRot = -0.5f - Mth.sin(t * 0.1f) * 0.15f;
        leg0.xRot = Mth.cos(walk) * 1.4f * amt;
        leg3.xRot = Mth.cos(walk) * 1.4f * amt;
        leg1.xRot = Mth.cos(walk + Mth.PI) * 1.4f * amt;
        leg2.xRot = Mth.cos(walk + Mth.PI) * 1.4f * amt;''', state='State', flags=('sheared',),
    doc='The Puffling: a little sky lamb in a thundercloud fleece with a spark on top, floppy ears and a pink snout.'))

# ---- Glimmerstag and Thistlehop ------------------------------------------------------------------------------
stag = [P('body', (0, 11, 0), boxes=[Box(-4, -4, -8, 8, 8, 16, 'fur'), Box(-3.5, 3, -7, 7, 1, 13, 'belly')], field='body').add(
            P('tail', (0, -3, 8), (0.5, 0, 0), [Box(-1, 0, 0, 2, 3, 1, 'belly')], field='tail')),
        P('neck', (0, 8, -7), boxes=[Box(-2, -9, -2, 4, 10, 4, 'fur')], field='neck').add(
            P('head', (0, -9, 0), boxes=[Box(-2.5, -3, -6, 5, 4, 7, 'fur'), Box(-1.5, -1, -8, 3, 2, 2, 'nose'),
                                         Box(-2.8, -2.5, -3.5, 1, 1, 1, 'eye'), Box(1.8, -2.5, -3.5, 1, 1, 1, 'eye')], field='head').add(
                P('left_ear', (2.5, -2.5, -1), (0, 0, 0.6), [Box(0, -0.5, -0.5, 3, 1, 2, 'fur')], field='leftEar'),
                P('right_ear', (-2.5, -2.5, -1), (0, 0, -0.6), [Box(-3, -0.5, -0.5, 3, 1, 2, 'fur')], field='rightEar'),
                *[P(f'antler_{"left" if side > 0 else "right"}', (side * 1.5, -3, -1), (0, 0, side * 0.25), [Box(-0.5, -7, -0.5, 1, 7, 1, 'crystal')]).add(
                    P(f'tine_{"left" if side > 0 else "right"}_0', (0, -3, 0), (0, 0, side * 0.9), [Box(-0.5, -3, -0.5, 1, 3, 1, 'crystal')]),
                    P(f'tine_{"left" if side > 0 else "right"}_1', (0, -5.5, 0), (-0.6, 0, side * 0.5), [Box(-0.5, -3, -0.5, 1, 3, 1, 'crystal')]),
                    P(f'tip_{"left" if side > 0 else "right"}', (0, -7, 0), boxes=[Box(-1, -2, -1, 2, 2, 2, 'glow')])) for side in (-1, 1)]))]
for i in range(4):
    x, z = (-2.5 if i % 2 == 0 else 2.5), (-6 if i < 2 else 6)
    stag.append(P(f'leg{i}', (x, 14, z), boxes=[Box(-1, 0, -1, 2, 5, 2, 'fur')], field=f'leg{i}').add(
        P(f'shin{i}', (0, 5, 0), boxes=[Box(-1, 0, -1, 2, 5, 2, 'fur'), Box(-1.2, 4, -1.2, 2, 1, 2, 'hoof')])))
M.append(Model('GlimmerstagModel', 'glimmerstag', {'fur': ('fur', C('#6a8a7a'), C('#3a5048')), 'belly': ('fur', C('#d8e6dc'), C('#a0b4a8')),
    'nose': ('dark', C('#2a2a2a')), 'eye': ('eye', C('#1a1a1a'), C('#1a1a1a')), 'crystal': ('crystal', C('#9ff0e0'), C('#3a9a88')),
    'glow': ('glow', C('#e8fff8'), C('#6ad8c0')), 'hoof': ('dark', C('#3a3430'))}, stag, '''        neck.yRot = s.yRot * Mth.DEG_TO_RAD * 0.5f;
        neck.xRot = s.xRot * Mth.DEG_TO_RAD * 0.5f;
        leftEar.zRot = 0.6f + Mth.sin(t * 0.13f) * 0.12f;
        rightEar.zRot = -0.6f - Mth.sin(t * 0.13f) * 0.12f;
        tail.xRot = 0.5f + Mth.sin(t * 0.3f) * 0.2f;
        for (int i = 0; i < 4; i++) {
            ModelPart l = i == 0 ? leg0 : i == 1 ? leg1 : i == 2 ? leg2 : leg3;
            l.xRot = Mth.cos(walk + (i == 0 || i == 3 ? 0 : Mth.PI)) * 1.1f * amt;
        }''', doc='The Glimmerstag: an elderstorm deer with jointed legs and branching crystal antlers that glow at the tips.'))

hop = [P('body', (0, 20, 0), boxes=[Box(-3, -3, -4, 6, 5, 8, 'fur'), Box(-2.5, 1.5, -3.5, 5, 1, 7, 'belly')], field='body').add(
           *[P(f'thistle_{i}', (x, -3, z), (rx, 0, rz), [Box(-0.5, -3, -0.5, 1, 3, 1, 'thorn'), Box(-1, -4, -1, 2, 1, 2, 'bloom')]) for i, (x, z, rx, rz) in
             enumerate(((-1.5, -2, -0.3, -0.2), (1.5, -1, -0.2, 0.2), (0, 1, 0.1, 0), (-1.5, 2.5, 0.3, -0.2), (1.5, 3, 0.3, 0.2)))],
           P('tail', (0, -1, 4), boxes=[Box(-1.5, -1.5, 0, 3, 3, 2, 'belly')])),
       P('head', (0, 17, -4), boxes=[Box(-2.5, -3, -4, 5, 4, 4, 'fur'), Box(-1, -1, -4.5, 2, 1, 1, 'nose'),
                                     Box(-2.7, -2, -3, 1, 1, 1, 'eye'), Box(1.7, -2, -3, 1, 1, 1, 'eye')], field='head').add(
           P('left_ear', (1.2, -3, -1.5), (-0.2, 0, 0.15), [Box(-0.5, -5, -0.5, 2, 5, 1, 'fur')], field='leftEar'),
           P('right_ear', (-1.2, -3, -1.5), (-0.2, 0, -0.15), [Box(-1.5, -5, -0.5, 2, 5, 1, 'fur')], field='rightEar'))]
for side in (1, -1):
    nm = 'left' if side > 0 else 'right'
    hop.append(P(f'{nm}_hind', (side * 2.5, 20, 2), boxes=[Box(-1, 0, -2, 2, 4, 4, 'fur'), Box(-1, 3, -3, 2, 1, 4, 'belly')], field=f'{nm}Hind'))
    hop.append(P(f'{nm}_front', (side * 1.8, 20, -3), boxes=[Box(-0.5, 0, -0.5, 1, 4, 1, 'fur')], field=f'{nm}Front'))
M.append(Model('ThistlehopModel', 'thistlehop', {'fur': ('fur', C('#8a7a5a'), C('#5a4a34')), 'belly': ('fur', C('#e8dcc4'), C('#b8a888')),
    'nose': ('skin', C('#d08898'), C('#905060')), 'eye': ('eye', C('#1a1408'), C('#1a1408')), 'thorn': ('shell', C('#5a7a3a'), C('#2a3a1a')),
    'bloom': ('fur', C('#b06ad6'), C('#6a3a8a'))}, hop, '''        head.yRot = s.yRot * Mth.DEG_TO_RAD;
        head.xRot = s.xRot * Mth.DEG_TO_RAD;
        float hopk = Math.abs(Mth.sin(walk)) * amt;
        body.y = 20f - hopk * 2f;
        head.y = 17f - hopk * 2f;
        leftHind.xRot = -hopk * 1.2f; rightHind.xRot = -hopk * 1.2f;
        leftFront.xRot = hopk * 0.9f; rightFront.xRot = hopk * 0.9f;
        leftEar.xRot = -0.2f + Mth.sin(t * 0.2f) * 0.1f - hopk * 0.4f;
        rightEar.xRot = -0.2f + Mth.cos(t * 0.2f) * 0.1f - hopk * 0.4f;''', doc='The Thistlehop: a hare-like forest hopper with long ears and blooming thistles on its back.'))

# ---- Hollow Keeper (the Labyrinth's guardian) ----------------------------------------------------------------
keeper = [P('body', (0, -6, 0), boxes=[Box(-4, 0, -2, 8, 14, 4, 'cloth'), Box(-4.5, 0, -2.5, 9, 4, 5, 'metal'), Box(-1.5, 5, -2.6, 3, 3, 1, 'glow')], field='body').add(
              P('tatters', (0, 14, 0), boxes=[Box(-4, 0, -2, 8, 6, 4, 'cloth')], field='tatters'),
              P('lantern', (3, 11, -3), boxes=[Box(-1.5, 0, -1.5, 3, 4, 3, 'glow'), Box(-0.5, -2, -0.5, 1, 2, 1, 'metal')], field='lantern')),
          P('head', (0, -6, 0), boxes=[Box(-3.5, -9, -3.5, 7, 9, 7, 'bone'), Box(-4, -9.5, -4, 8, 3, 8, 'cloth'),
                                        Box(-2.5, -6, -3.8, 2, 2, 1, 'eye'), Box(0.5, -6, -3.8, 2, 2, 1, 'eye')], field='head')]
arm = P('left_arm', (5, -5, 0), boxes=[Box(-1, -1, -1.5, 3, 14, 3, 'cloth')], field='leftArm').add(
    P('left_claw', (0.5, 13, 0), boxes=[Box(-1.5, 0, -1.5, 3, 8, 3, 'bone')], field='leftClaw'))
keeper += [arm, mirror_part(arm)]
legk = P('left_leg', (2, 8, 0), boxes=[Box(-1.5, 0, -1.5, 3, 16, 3, 'cloth')], field='leftLeg')
keeper += [legk, mirror_part(legk)]
M.append(Model('HollowKeeperModel', 'hollow_keeper', {'cloth': ('cloth', C('#3a3448'), C('#1a1622')), 'metal': ('metal', C('#6a6458'), C('#2e2a24')),
    'glow': ('glow', C('#bff0ff'), C('#3a8ab0')), 'bone': ('bone', C('#d8d2c0'), C('#8a8270')), 'eye': ('glow', C('#7fe0ff'), C('#0a1a24'))}, keeper,
    '''        head.yRot = s.yRot * Mth.DEG_TO_RAD;
        head.xRot = s.xRot * Mth.DEG_TO_RAD - 0.15f;
        leftLeg.xRot = Mth.cos(walk) * 0.9f * amt;
        rightLeg.xRot = -Mth.cos(walk) * 0.9f * amt;
        leftArm.xRot = -Mth.cos(walk) * 0.6f * amt - 0.1f + Mth.sin(t * 0.05f) * 0.05f;
        rightArm.xRot = Mth.cos(walk) * 0.6f * amt - 0.1f - Mth.sin(t * 0.05f) * 0.05f;
        leftArm.zRot = -0.08f; rightArm.zRot = 0.08f;
        leftClaw.xRot = -0.3f - Mth.sin(t * 0.07f) * 0.1f;
        rightClaw.xRot = -0.3f - Mth.cos(t * 0.07f) * 0.1f;
        tatters.xRot = Mth.sin(t * 0.1f) * 0.08f + amt * 0.25f;
        lantern.zRot = Mth.sin(t * 0.12f + walk) * 0.25f;''',
    doc='The Hollow Keeper: a gaunt robed figure, three blocks tall, with a bone mask, glowing eyes, long clawed arms and a swinging lantern.'))

MOB_TEXTURES = {m.texture for m in M}


def main():
    JAVA.mkdir(parents=True, exist_ok=True)
    for old in JAVA.glob('*.java'): old.unlink()
    for m in M:
        W, H = paint(m)
        (JAVA / f'{m.cls}.java').write_text(java(m, W, H))
    print(f'mobmodels: {len(M)} models and textures')


if __name__ == '__main__':
    main()
