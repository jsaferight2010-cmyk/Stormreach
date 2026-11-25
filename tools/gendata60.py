"""Stormreach 6.0 (the Overhaul) data: language, models, item definitions, loot, recipes and tags for the overhaul.
Runs after gendata12.py and before descriptions.py. Sections are added stage by stage (see NOTES.md)."""
import json
from gendata9 import (w, rj, ns, tag_add, item_def, gen_item, bmodel, el, shaped, adv, has, lang, L, A, D, MC)

B = 'stormreach:block/'

# ---------------------------------------------------------------- radar scope (stage 2)
L('gui.stormreach.weather.inbound', 'Inbound')
L('gui.stormreach.weather.outbound', 'Outbound')

# ---------------------------------------------------------------- multiblock machines (stage 2)
# the invisible cells of tall machines: one empty model for every state (the controller's renderer draws the machine)
bmodel('machine_part', {'textures': {'particle': 'minecraft:block/iron_block'}, 'elements': []})
w(f'{A}/blockstates/machine_part.json', {'variants': {'': {'model': B + 'machine_part'}}})
L('block.stormreach.machine_part', 'Machine Part')
L('block.stormreach.machine_part.desc', 'Part of a larger machine')

# ---------------------------------------------------------------- HUD panel (goggles / TIV)
L('hud.stormreach.goggles', 'Storm Chaser Goggles')
L('hud.stormreach.tiv', 'TIV Dashboard')
L('hud.stormreach.wind', 'Wind %s from %s %s')
L('hud.stormreach.gust', 'Gusts %s')
L('hud.stormreach.air', '%s  ·  %s')
L('hud.stormreach.no_tornado', 'No tornado in range')
L('hud.stormreach.tornado.at', '%s %s %s')
L('hud.stormreach.tornado.moving', 'Moving %s at %s')
L('hud.stormreach.tornado.peak', 'Peak winds %s')
L('hud.stormreach.tornado.here', 'Wind here %s')
L('hud.stormreach.tiv.speed', 'Speed %s')
L('hud.stormreach.tiv.anchored', 'ANCHORED (spikes down)')
L('key.stormreach.tiv_anchor', 'Drop/Raise TIV Anchors')
L('entity.stormreach.fire_whirl', 'Fire Whirl')

# ---------------------------------------------------------------- salt crystal cluster (stage 2)
# Real halite: a cluster of cubes with stepped hopper faces on a salt crust, not crossed panes. The hitbox follows it.
def salt_el(frm, to, tex='#f', rot=None):
    e = el(frm, to, tex)
    for f in e['faces'].values(): f['uv'] = [0, 0, 16, 16]
    if rot: e['rotation'] = {'origin': [(frm[0] + to[0]) / 2, frm[1], (frm[2] + to[2]) / 2], 'axis': 'y', 'angle': rot}
    return e
salt_cluster = [
    salt_el([1.5, 0, 1.5], [14.5, 1, 14.5], '#c'),
    salt_el([3, 1, 4], [10, 8, 11], rot=22.5),
    salt_el([8.5, 1, 1.5], [13.5, 6, 6.5], rot=-22.5),
    salt_el([5, 1, 8], [9, 12, 12]),
    salt_el([10.5, 1, 9], [14, 4.5, 12.5], rot=22.5),
    salt_el([2, 1, 10.5], [5, 4, 13.5]),
    salt_el([5, 8, 5.5], [8, 11, 8.5], rot=22.5),
]
bmodel('salt_crystal', {'parent': 'minecraft:block/block', 'ambientocclusion': False,
                        'textures': {'particle': B + 'salt_crystal_facet', 'f': B + 'salt_crystal_facet', 'c': B + 'salt_crust'}, 'elements': salt_cluster})
w(f'{A}/models/item/salt_crystal.json', {'parent': 'stormreach:block/salt_crystal'})

# ---------------------------------------------------------------- cistern that shows its water (stage 2)
# An open stone tank: thick walls, an iron grate over the top you can see through, the water surface at its real
# level, and a sight-glass slot in each wall whose water column rises and falls with it.
def uvbox(frm, to, tex, faces='nsewud'):
    e = el(frm, to, tex, faces)
    return e
for lv in range(5):
    for storm in (False, True):
        els = [el([0, 0, 0], [16, 2, 16], '#wall')]                                            # floor
        for (a, b, horiz) in ((0, 2, True), (14, 16, True)):                                     # north/south walls with a slot
            z0, z1 = a, b
            els += [el([0, 2, z0], [7, 16, z1], '#wall'), el([9, 2, z0], [16, 16, z1], '#wall'),
                    el([7, 2, z0], [9, 3, z1], '#wall'), el([7, 15, z0], [9, 16, z1], '#wall')]
            gz = z0 + 0.5 if a == 0 else z1 - 0.5
            els.append(el([7, 3, min(gz, gz) - 0.01], [9, 15, max(gz, gz) + 0.01], '#glass', faces='ns'))
            if lv: els.append(el([7.2, 3, z0 + 0.6], [8.8, 3 + lv * 3, z1 - 0.6], '#water', faces='nsu'))
        for (a, b) in ((0, 2), (14, 16)):                                                        # east/west walls with a slot
            els += [el([a, 2, 2], [b, 16, 7], '#wall'), el([a, 2, 9], [b, 16, 14], '#wall'),
                    el([a, 2, 7], [b, 3, 9], '#wall'), el([a, 15, 7], [b, 16, 9], '#wall')]
            gx = a + 0.5 if a == 0 else b - 0.5
            els.append(el([gx - 0.01, 3, 7], [gx + 0.01, 15, 9], '#glass', faces='ew'))
            if lv: els.append(el([a + 0.6, 3, 7.2], [b - 0.6, 3 + lv * 3, 8.8], '#water', faces='ewu'))
        if lv: els.append(el([2, 2, 2], [14, 2 + lv * 3, 14], '#water', faces='u'))           # the water surface
        for x in (4, 7.5, 11):                                                                   # iron grate bars
            els.append(el([x, 15.3, 2], [x + 0.8, 16, 14], '#iron'))
        els.append(el([2, 15.2, 7.5], [14, 15.8, 8.5], '#iron'))
        bmodel(f"cistern_{lv}{'_storm' if storm else ''}", {'parent': 'minecraft:block/block',
               'textures': {'particle': B + 'cistern', 'wall': B + 'cistern', 'glass': {'force_translucent': True, 'sprite': B + 'sight_glass'}, 'iron': B + 'iron_grate',
                            'water': B + ('tank_stormwater' if storm else 'tank_water')}, 'elements': els})
w(f'{A}/blockstates/cistern.json', {'variants': {f'level={lv},storm={s}': {'model': B + f"cistern_{lv}{'_storm' if s == 'true' else ''}"}
                                                 for lv in range(5) for s in ('false', 'true')}})
w(f'{A}/models/item/cistern.json', {'parent': 'stormreach:block/cistern_3'})

# ---------------------------------------------------------------- 3D held items: umbrella and Stormkite (stage 2)
def cell(i):
    """uv of the centre of swatch i on a 16x16 parts palette (4x4 cells)."""
    x0, y0 = (i % 4) * 4, (i // 4) * 4
    return [x0 + 1, y0 + 1, x0 + 3, y0 + 3]
def pel(frm, to, i, faces='nsewud', tex='#p'):
    e = el(frm, to, tex, faces)
    for f in e['faces'].values(): f['uv'] = cell(i)
    return e

# Umbrella, open: steel shaft, wooden J handle, an eight-gore canopy (stepped dome), black rib tips, a ferrule on top.
um = [pel([7.5, -5, 7.5], [8.5, 27, 8.5], 2),                                   # shaft
      pel([7.25, -9, 7.25], [8.75, -4.5, 8.75], 3), pel([7.25, -10.5, 7.25], [8.75, -9, 11.5], 3),   # J handle
      pel([7.25, -9, 10], [8.75, -7.5, 11.5], 3)]
for (lo, hi, inset) in ((21, 22.2, -5), (22.2, 23.6, -3), (23.6, 25, 0), (25, 26.2, 3), (26.2, 27, 5.5)):
    e = el([inset, lo, inset], [16 - inset, hi, 16 - inset], '#c')
    for k, f in e['faces'].items():
        f['uv'] = [0, 0, 16, 16] if k in ('up', 'down') else [0, 6, 16, 10]
    um.append(e)
for (x, z) in ((-5, -5), (20.5, -5), (-5, 20.5), (20.5, 20.5), (7.5, -6), (7.5, 21), (-6, 7.5), (21, 7.5)):
    um.append(pel([x, 20.4, z], [x + 0.6, 21, z + 0.6], 4))                        # rib tips
um.append(pel([7.6, 27, 7.6], [8.4, 29, 8.4], 4))                                    # ferrule
w(f'{A}/models/item/umbrella_open.json', {'textures': {'particle': 'stormreach:item/umbrella', 'p': 'stormreach:item/umbrella_parts', 'c': 'stormreach:item/umbrella_canopy'},
    'elements': um, 'display': {
        'thirdperson_righthand': {'rotation': [0, 0, 0], 'translation': [0, 12, 1], 'scale': [0.8, 0.8, 0.8]},
        'thirdperson_lefthand': {'rotation': [0, 0, 0], 'translation': [0, 12, 1], 'scale': [0.8, 0.8, 0.8]},
        'firstperson_righthand': {'rotation': [0, -90, 8], 'translation': [3, 4, 0], 'scale': [0.55, 0.55, 0.55]},
        'firstperson_lefthand': {'rotation': [0, 90, -8], 'translation': [3, 4, 0], 'scale': [0.55, 0.55, 0.55]},
        'head': {'rotation': [0, 0, 0], 'translation': [0, 12, 0], 'scale': [0.7, 0.7, 0.7]}}})

# Stormkite: a diamond kite held upright by its spine, sail facing forward, tail of bows hanging below.
kite = []
y = 4.0
while y < 28:
    w_ = 18 * (28 - y) / 8 if y >= 20 else 18 * (y - 4) / 16
    w_ = max(1.0, w_)
    band = 1 if 16 <= y < 18 else (2 if 10 <= y < 12 else (0 if (int(y) // 2) % 2 == 0 else 7))
    kite.append(pel([8 - w_ / 2, y, 7.75], [8 + w_ / 2, y + 2, 8.25], band))
    y += 2
kite += [pel([7.5, 3, 8.25], [8.5, 29, 8.75], 3), pel([-1.5, 19.5, 8.25], [17.5, 20.5, 8.75], 3)]     # spars
for k in range(5):                                                                                     # tail bows
    yy = 1.5 - k * 3; xx = 8 + (0.8 if k % 2 else -0.8)
    kite += [pel([7.9, yy, 7.9], [8.1, yy + 3, 8.1], 6), pel([xx - 1.5, yy - 0.5, 7.7], [xx + 1.5, yy + 0.5, 8.3], 4 if k % 2 == 0 else 5)]
w(f'{A}/models/item/stormkite_held.json', {'textures': {'particle': 'stormreach:item/stormkite', 'p': 'stormreach:item/stormkite_parts'},
    'elements': kite, 'display': {
        'thirdperson_righthand': {'rotation': [0, 90, 0], 'translation': [0, 9, 1], 'scale': [0.75, 0.75, 0.75]},
        'thirdperson_lefthand': {'rotation': [0, -90, 0], 'translation': [0, 9, 1], 'scale': [0.75, 0.75, 0.75]},
        'firstperson_righthand': {'rotation': [0, -75, 5], 'translation': [2, 5, 0], 'scale': [0.5, 0.5, 0.5]},
        'firstperson_lefthand': {'rotation': [0, 75, -5], 'translation': [2, 5, 0], 'scale': [0.5, 0.5, 0.5]}}})

def held_or_icon(name, held_model):
    """Flat painted icon in the inventory, on the ground and in frames; the 3D model in hand."""
    w(f'{A}/items/{name}.json', {'model': {'type': 'minecraft:select', 'property': 'minecraft:display_context',
        'cases': [{'when': ['gui', 'ground', 'fixed', 'on_shelf'], 'model': {'type': 'minecraft:model', 'model': f'stormreach:item/{name}'}}],
        'fallback': {'type': 'minecraft:model', 'model': f'stormreach:item/{held_model}'}}})
held_or_icon('umbrella', 'umbrella_open')
held_or_icon('stormkite', 'stormkite_held')

# machine items show an icon of the finished machine (rendered from its real model)
for n in ('tornado_radar', 'wind_turbine', 'tornado_siren', 'weather_station'):
    gen_item(n)

# Portable Storm Radar in hand: a rugged field unit (rubber bumpers, live PPI screen that glows, keypad, antenna)
pr = [pel([3, 0, 7], [13, 14, 9.5], 0), pel([2.5, -0.5, 6.6], [13.5, 1.5, 9.9], 1), pel([2.5, 12.5, 6.6], [13.5, 14.5, 9.9], 1),
      pel([11, 14.5, 7.8], [12, 22, 8.8], 5), pel([10.8, 22, 7.6], [12.2, 23, 9.0], 6),
      pel([4, 1.5, 6.9], [6, 3, 7.0], 4), pel([7, 1.5, 6.9], [9, 3, 7.0], 5), pel([10, 1.5, 6.9], [12, 3, 7.0], 5)]
screen = el([4, 4, 6.8], [12, 12.5, 7.0], '#s', faces='n'); screen['faces']['north']['uv'] = [0, 0, 16, 16]; screen['light_emission'] = 12
pr.append(screen)
w(f'{A}/models/item/storm_radar_held.json', {'textures': {'particle': 'stormreach:item/storm_radar', 'p': 'stormreach:item/radar_parts', 's': 'stormreach:item/storm_radar_screen'},
    'elements': pr, 'display': {
        'thirdperson_righthand': {'rotation': [-30, 180, 0], 'translation': [0, 3, 2], 'scale': [0.55, 0.55, 0.55]},
        'thirdperson_lefthand': {'rotation': [-30, 180, 0], 'translation': [0, 3, 2], 'scale': [0.55, 0.55, 0.55]},
        'firstperson_righthand': {'rotation': [-25, 160, 0], 'translation': [-2, 3, -2], 'scale': [0.6, 0.6, 0.6]},
        'firstperson_lefthand': {'rotation': [-25, 200, 0], 'translation': [-2, 3, -2], 'scale': [0.6, 0.6, 0.6]}}})
held_or_icon('storm_radar', 'storm_radar_held')

# ---------------------------------------------------------------- puddles and hail (stage 3)
gen_item('puddle')
for n in ('hail_small', 'hail_medium', 'hail_large'):
    w(f'{A}/particles/{n}.json', {'textures': ['stormreach:hail_stone_0', 'stormreach:hail_stone_1', 'stormreach:hail_stone_2']})

# Hail on the ground: a granular crust that thickens with each layer and a scatter of loose stones on top (bigger
# lumps when golf-ball hail or larger fell), three random layouts per state so a field of hail never tiles.
import random as _r
def hail_model(layers, big, variant):
    rnd = _r.Random(layers * 31 + big * 7 + variant * 101)
    h = 2 * layers
    els = [el([0, 0, 0], [16, max(0.6, h - 1.2), 16], '#c')]
    for e in els:
        for f in e['faces'].values(): f['uv'] = [0, 0, 16, 16]
    n = 10 + layers * 4 if not big else 7 + layers * 3
    for _ in range(n):
        size = rnd.choice((1, 1, 1.5, 2)) if not big else rnd.choice((2, 2.5, 3, 3.5))
        x, z = rnd.uniform(0.2, 15.8 - size), rnd.uniform(0.2, 15.8 - size)
        y0 = max(0, h - 1.2 - size * 0.35)
        e = el([round(x, 2), round(y0, 2), round(z, 2)], [round(x + size, 2), round(y0 + size * 0.85, 2), round(z + size, 2)], '#p')
        for f in e['faces'].values(): f['uv'] = [0, 0, 16, 16]
        if rnd.random() < 0.5: e['rotation'] = {'origin': [x + size / 2, y0, z + size / 2], 'axis': 'y', 'angle': rnd.choice((-22.5, 22.5))}
        els.append(e)
    name = f'hail_layer{"_big" if big else ""}_{layers}_v{variant}'
    bmodel(name, {'parent': 'minecraft:block/block', 'ambientocclusion': False,
                  'textures': {'particle': B + 'hail_crust', 'c': B + 'hail_crust', 'p': B + 'hail_pellet'}, 'elements': els})
    return B + name
variants = {}
for b in (False, True):
    for l in range(1, 9):
        lv = min(l, 4)
        variants[f'big={str(b).lower()},layers={l}'] = [{'model': hail_model(lv, b, v)} for v in range(3)]
w(f'{A}/blockstates/hail_layer.json', {'variants': variants})

with open(f'{A}/lang/en_us.json', 'w') as f: json.dump(lang, f, indent=2, ensure_ascii=False); f.write('\n')
print('gendata60: overhaul data written')
