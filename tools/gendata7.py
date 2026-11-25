"""Stormreach 4.0 data: Tempest Spire, Tempest Sovereign, Stormgate and the Tempest Realm dimension."""
import os as _os, pathlib as _pl
ROOT = str(_pl.Path(__file__).resolve().parent.parent)  # the Stormreach project folder
VANILLA = _os.environ.get('STORMREACH_VANILLA', str(_pl.Path(ROOT) / '.vanilla'))  # extracted 26.3 jar (assets/ + data/)
MCJAR = _os.environ.get('STORMREACH_MCJAR', str(_pl.Path(ROOT) / '.vanilla' / 'minecraft-26.3.jar'))
import json, os, re, copy
import nbtlib
from nbtlib import tag as T
_ROOT = ROOT if 'ROOT' in globals() else '/home/claude/stormreach'
_VAN = VANILLA if 'VANILLA' in globals() else '/home/claude/mc/vd'
R = _ROOT + '/src/main/resources'
A = f'{R}/assets/stormreach'; D = f'{R}/data/stormreach'; NS = 'stormreach'
VA = f'{_VAN}/assets/minecraft'; VD = f'{_VAN}/data/minecraft'
MC = f'{R}/data/minecraft/tags'; C = f'{R}/data/c/tags'; WG = f'{D}/worldgen'
def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f: json.dump(obj, f, indent=2); f.write('\n')
def ns(x): return x if ':' in x else f'{NS}:{x}'
def rj(p): return json.load(open(p))
def tag_add(path, values):
    old = rj(path)['values'] if os.path.exists(path) else []
    ids = {v if isinstance(v, str) else v['id'] for v in old}
    w(path, {'values': old + [v for v in values if (v if isinstance(v, str) else v['id']) not in ids]})
lang = rj(f'{A}/lang/en_us.json')
def L(k, v): lang[k] = v

# =====================================================================================
# ASSETS: block models, blockstates, item definitions
# =====================================================================================
def item_def(name, model): w(f'{A}/items/{name}.json', {'model': {'type': 'minecraft:model', 'model': ns(model)}})
def simple_state(name, model=None): w(f'{A}/blockstates/{name}.json', {'variants': {'': {'model': ns('block/' + (model or name))}}})
def cube(name, tex=None):
    w(f'{A}/models/block/{name}.json', {'parent': 'minecraft:block/cube_all', 'textures': {'all': ns('block/' + (tex or name))}})
    simple_state(name); item_def(name, 'block/' + name)
def bottom_top(name, top, side, bottom):
    w(f'{A}/models/block/{name}.json', {'parent': 'minecraft:block/cube_bottom_top', 'textures': {'top': ns(top), 'side': ns(side), 'bottom': ns(bottom)}})
def column(name):
    w(f'{A}/models/block/{name}.json', {'parent': 'minecraft:block/cube_column', 'textures': {'end': f'stormreach:block/{name}_top', 'side': f'stormreach:block/{name}'}})
    w(f'{A}/models/block/{name}_horizontal.json', {'parent': 'minecraft:block/cube_column_horizontal', 'textures': {'end': f'stormreach:block/{name}_top', 'side': f'stormreach:block/{name}'}})
    w(f'{A}/blockstates/{name}.json', {'variants': {'axis=x': {'model': f'stormreach:block/{name}_horizontal', 'x': 90, 'y': 90},
        'axis=y': {'model': f'stormreach:block/{name}'}, 'axis=z': {'model': f'stormreach:block/{name}_horizontal', 'x': 90}}})
    item_def(name, 'block/' + name)
def cross(name, item_tex=True):
    w(f'{A}/models/block/{name}.json', {'parent': 'minecraft:block/cross', 'textures': {'cross': f'stormreach:block/{name}'}})
    simple_state(name)
    w(f'{A}/models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'stormreach:block/{name}'}})
    item_def(name, 'item/' + name)
def clone_family(vname, oname, texmap):
    bs = json.dumps(rj(f'{VA}/blockstates/{vname}.json'))
    itm = json.dumps(rj(f'{VA}/items/{vname}.json'))
    for m in set(re.findall(r'"minecraft:block/([a-z0-9_]+)"', bs + itm)):
        if not m.startswith(vname): continue
        s = json.dumps(rj(f'{VA}/models/block/{m}.json'))
        for vt, ot in texmap.items(): s = s.replace(f'"minecraft:block/{vt}"', f'"stormreach:block/{ot}"')
        w(f'{A}/models/block/{m.replace(vname, oname, 1)}.json', json.loads(s))
    w(f'{A}/blockstates/{oname}.json', json.loads(bs.replace(f'"minecraft:block/{vname}', f'"stormreach:block/{oname}')))
    it = itm.replace(f'minecraft:block/{vname}', f'stormreach:block/{oname}').replace(f'minecraft:item/{vname}', f'stormreach:item/{oname}')
    w(f'{A}/items/{oname}.json', json.loads(it))
    if 'stormreach:item/' in it and os.path.exists(f'{VA}/models/item/{vname}.json'):
        s = json.dumps(rj(f'{VA}/models/item/{vname}.json')).replace(f'minecraft:item/{vname}', f'stormreach:item/{oname}')
        for vt, ot in texmap.items(): s = s.replace(f'"minecraft:block/{vt}"', f'"stormreach:block/{ot}"')
        w(f'{A}/models/item/{oname}.json', json.loads(s))
def stone_set(prefix, tex, wall=True):
    clone_family('stone_stairs', f'{prefix}_stairs', {'stone': tex})
    clone_family('stone_slab', f'{prefix}_slab', {'stone': tex})
    bs = rj(f'{A}/blockstates/{prefix}_slab.json'); bs['variants']['type=double'] = {'model': f'stormreach:block/{tex}'}
    w(f'{A}/blockstates/{prefix}_slab.json', bs)
    if wall: clone_family('cobblestone_wall', f'{prefix}_wall', {'cobblestone': tex})

for b in ('tempest_bricks', 'cracked_tempest_bricks', 'chiseled_tempest_bricks', 'skyshale', 'cobbled_skyshale', 'polished_skyshale',
          'skyshale_bricks', 'chiseled_skyshale', 'skysoil', 'voltite_ore', 'aurorite_ore', 'ion_ore', 'voltite_block', 'aurorite_block',
          'ion_lamp', 'nimbus_cloud', 'thundercloud', 'gale_cloud', 'galewood_planks', 'galewood_leaves'):
    cube(b)
stone_set('tempest_brick', 'tempest_bricks'); stone_set('cobbled_skyshale', 'cobbled_skyshale'); stone_set('skyshale_brick', 'skyshale_bricks')
bottom_top('static_grass', 'block/static_grass_top', 'block/static_grass_side', 'block/skysoil'); simple_state('static_grass'); item_def('static_grass', 'block/static_grass')
bottom_top('sovereign_altar', 'block/sovereign_altar_top', 'block/sovereign_altar_side', 'block/sovereign_altar_bottom')
w(f'{A}/blockstates/sovereign_altar.json', {'variants': {f'ritual={i}': {'model': 'stormreach:block/sovereign_altar'} for i in range(16)}})
item_def('sovereign_altar', 'block/sovereign_altar')
bottom_top('stormgate_frame', 'block/stormgate_frame_top', 'block/stormgate_frame_side', 'block/stormgate_frame_bottom')
bottom_top('stormgate_frame_eye', 'block/stormgate_frame_top_eye', 'block/stormgate_frame_side', 'block/stormgate_frame_bottom')
w(f'{A}/blockstates/stormgate_frame.json', {'variants': {'eye=false': {'model': 'stormreach:block/stormgate_frame'}, 'eye=true': {'model': 'stormreach:block/stormgate_frame_eye'}}})
item_def('stormgate_frame', 'block/stormgate_frame')
face = {'texture': '#portal', 'uv': [0, 0, 16, 16]}
w(f'{A}/models/block/stormgate.json', {'textures': {'portal': 'stormreach:block/stormgate', 'particle': 'stormreach:block/stormgate'},
    'elements': [{'from': [0, 0, 0], 'to': [16, 12, 16], 'faces': {'up': dict(face), 'down': dict(face),
        'north': {**face, 'uv': [0, 4, 16, 16], 'cullface': 'north'}, 'south': {**face, 'uv': [0, 4, 16, 16], 'cullface': 'south'},
        'east': {**face, 'uv': [0, 4, 16, 16], 'cullface': 'east'}, 'west': {**face, 'uv': [0, 4, 16, 16], 'cullface': 'west'}}}]})
simple_state('stormgate')
column('galewood_log'); column('stripped_galewood_log')
wm = {'oak_planks': 'galewood_planks', 'oak_door_top': 'galewood_door_top', 'oak_door_bottom': 'galewood_door_bottom', 'oak_trapdoor': 'galewood_trapdoor'}
for kind in ('fence', 'fence_gate', 'door', 'trapdoor', 'button', 'pressure_plate'):
    clone_family(f'oak_{kind}', f'galewood_{kind}', wm)
w(f'{A}/models/item/galewood_door.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'stormreach:item/galewood_door'}})
clone_family('oak_stairs', 'galewood_stairs', {'oak_planks': 'galewood_planks'})
clone_family('oak_slab', 'galewood_slab', {'oak_planks': 'galewood_planks'})
bs = rj(f'{A}/blockstates/galewood_slab.json'); bs['variants']['type=double'] = {'model': 'stormreach:block/galewood_planks'}; w(f'{A}/blockstates/galewood_slab.json', bs)
for p in ('galewood_sapling', 'skybloom', 'stormcap', 'gale_grass'): cross(p)

FLAT = ['tempest_sigil', 'tempest_eye', 'raw_voltite', 'voltite_ingot', 'aurorite', 'stormhawk_feather', 'ion_shard', 'cloudberries',
        'cloudberry_tart', 'raw_skyray', 'cooked_skyray', 'storm_nectar', 'gale_bread', 'cloudstep_boots']
TOOLS = []
for m in ('voltite', 'aurorite'):
    TOOLS += [f'{m}_{t}' for t in ('sword', 'pickaxe', 'axe', 'shovel', 'hoe')]
    FLAT += [f'{m}_{a}' for a in ('helmet', 'chestplate', 'leggings', 'boots')]
for i in FLAT + TOOLS:
    w(f'{A}/models/item/{i}.json', {'parent': 'minecraft:item/handheld' if i in TOOLS else 'minecraft:item/generated', 'textures': {'layer0': f'stormreach:item/{i}'}})
    item_def(i, 'item/' + i)
MOBS = ['tempest_sovereign', 'skyray', 'puffling', 'static_wisp', 'cyclone', 'stormhawk']
for mob in MOBS:
    e = f'{mob}_spawn_egg'
    w(f'{A}/models/item/{e}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'stormreach:item/{e}'}})
    item_def(e, 'item/' + e)
for name, layers in (('voltite', ('humanoid', 'humanoid_baby', 'humanoid_leggings')), ('aurorite', ('humanoid', 'humanoid_baby', 'humanoid_leggings')),
                     ('cloudstep', ('humanoid', 'humanoid_baby'))):
    w(f'{A}/equipment/{name}.json', {'layers': {l: [{'texture': f'stormreach:{name}'}] for l in layers}})

# =====================================================================================
# DATA: loot
# =====================================================================================
def self_drop(name):
    w(f'{D}/loot_table/blocks/{name}.json', {'type': 'minecraft:block', 'pools': [{'condition': {'type': 'minecraft:survives_explosion'},
        'entries': [{'type': 'minecraft:item', 'name': ns(name)}], 'rolls': 1}], 'random_sequence': f'stormreach:blocks/{name}'})
for b in ('tempest_bricks', 'cracked_tempest_bricks', 'chiseled_tempest_bricks', 'tempest_brick_stairs', 'tempest_brick_wall', 'stormgate_frame',
          'cobbled_skyshale', 'cobbled_skyshale_stairs', 'cobbled_skyshale_wall', 'polished_skyshale', 'skyshale_bricks', 'skyshale_brick_stairs',
          'skyshale_brick_wall', 'chiseled_skyshale', 'skysoil', 'nimbus_cloud', 'thundercloud', 'gale_cloud', 'galewood_log', 'stripped_galewood_log',
          'galewood_planks', 'galewood_sapling', 'galewood_stairs', 'galewood_fence', 'galewood_fence_gate', 'galewood_trapdoor', 'galewood_button',
          'galewood_pressure_plate', 'voltite_block', 'aurorite_block', 'ion_lamp', 'skybloom', 'stormcap'):
    self_drop(b)
for slab in ('tempest_brick_slab', 'cobbled_skyshale_slab', 'skyshale_brick_slab', 'galewood_slab'):
    w(f'{D}/loot_table/blocks/{slab}.json', {'type': 'minecraft:block', 'pools': [{'entries': [{'type': 'minecraft:item',
      'modifier': [{'type': 'minecraft:set_count', 'condition': {'type': 'minecraft:match_block', 'blocks': ns(slab), 'state': {'type': 'double'}}, 'count': 2},
                   {'type': 'minecraft:explosion_decay'}], 'name': ns(slab)}], 'rolls': 1}], 'random_sequence': f'stormreach:blocks/{slab}'})
v = rj(f'{VD}/loot_table/blocks/oak_door.json')
w(f'{D}/loot_table/blocks/galewood_door.json', json.loads(json.dumps(v).replace('minecraft:oak_door', 'stormreach:galewood_door').replace('minecraft:blocks/oak_door', 'stormreach:blocks/galewood_door')))
def silk_or(name, item, lo, hi, fortune=True):
    mods = [{'type': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'max': hi, 'min': lo}}]
    if fortune: mods.append({'type': 'minecraft:apply_bonus', 'enchantment': 'minecraft:fortune', 'formula': 'minecraft:ore_drops'})
    mods.append({'type': 'minecraft:explosion_decay'})
    w(f'{D}/loot_table/blocks/{name}.json', {'type': 'minecraft:block', 'pools': [{'entries': [{'type': 'minecraft:alternatives', 'children': [
      {'type': 'minecraft:item', 'condition': 'minecraft:tool/can_silk_touch', 'name': ns(name)},
      {'type': 'minecraft:item', 'modifier': mods, 'name': ns(item)}]}], 'rolls': 1}], 'random_sequence': f'stormreach:blocks/{name}'})
silk_or('voltite_ore', 'raw_voltite', 1, 1); silk_or('aurorite_ore', 'aurorite', 1, 1); silk_or('ion_ore', 'ion_shard', 2, 4)
silk_or('skyshale', 'cobbled_skyshale', 1, 1, fortune=False); silk_or('static_grass', 'skysoil', 1, 1, fortune=False)
w(f'{D}/loot_table/blocks/gale_grass.json', {'type': 'minecraft:block', 'pools': [{'entries': [{'type': 'minecraft:item',
   'condition': 'minecraft:tool/can_shear_blocks', 'name': 'stormreach:gale_grass'}], 'rolls': 1}], 'random_sequence': 'stormreach:blocks/gale_grass'})
leaves = json.dumps(rj(f'{VD}/loot_table/blocks/oak_leaves.json'))
leaves = leaves.replace('minecraft:oak_leaves', 'stormreach:galewood_leaves').replace('minecraft:oak_sapling', 'stormreach:galewood_sapling') \
               .replace('minecraft:apple', 'stormreach:cloudberries').replace('minecraft:blocks/oak_leaves', 'stormreach:blocks/galewood_leaves')
w(f'{D}/loot_table/blocks/galewood_leaves.json', json.loads(leaves))
def mob_loot(name, pools):
    w(f'{D}/loot_table/entities/{name}.json', {'type': 'minecraft:entity', 'pools': pools, 'random_sequence': f'stormreach:entities/{name}'})
def pool(item, lo, hi, looting=True, smelt=False):
    mods = [{'type': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'max': hi, 'min': lo}}]
    if smelt: mods.append({'type': 'minecraft:furnace_smelt', 'condition': {'type': 'minecraft:entity_properties', 'entity': 'this', 'predicate': {'flags': {'is_on_fire': True}}}})
    if looting: mods.append({'type': 'minecraft:enchanted_count_increase', 'count': {'type': 'minecraft:uniform', 'max': 1.0, 'min': 0.0}, 'enchantment': 'minecraft:looting'})
    return {'entries': [{'type': 'minecraft:item', 'modifier': mods, 'name': ns(item)}], 'rolls': 1}
# The Sovereign drops exactly one Tempest Eye, never more (looting doesn't apply).
mob_loot('tempest_sovereign', [{'entries': [{'type': 'minecraft:item', 'name': 'stormreach:tempest_eye'}], 'rolls': 1}])
mob_loot('skyray', [pool('raw_skyray', 1, 3, smelt=True)])
mob_loot('puffling', [pool('nimbus_cloud', 0, 1)])
mob_loot('static_wisp', [pool('ion_shard', 0, 2), pool('storm_shard', 0, 1)])
mob_loot('cyclone', [pool('gale_cloud', 0, 2)])
mob_loot('stormhawk', [pool('stormhawk_feather', 0, 2)])
def chest(name, entries, rolls):
    w(f'{D}/loot_table/chests/{name}.json', {'type': 'minecraft:chest', 'pools': [{'rolls': rolls, 'entries': [
        {'type': 'minecraft:item', 'name': ns(i), 'weight': wt, 'modifier': [{'type': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'max': hi, 'min': lo}}]}
        for i, wt, lo, hi in entries]}], 'random_sequence': f'stormreach:chests/{name}'})
chest('tempest_spire', [('storm_shard', 20, 3, 8), ('storm_cell', 10, 1, 3), ('storm_crystal_block', 6, 1, 2), ('tempest_tonic', 8, 1, 2),
                        ('thunderhead_horn', 4, 1, 1), ('minecraft:diamond', 6, 1, 3), ('minecraft:emerald', 10, 2, 5), ('charged_stormstone', 10, 2, 6),
                        ('minecraft:golden_apple', 4, 1, 1), ('bottled_thunder', 6, 1, 2), ('tempest_bricks', 12, 4, 12), ('fulgurite', 8, 2, 6)],
      {'type': 'minecraft:uniform', 'max': 7, 'min': 4})
chest('sky_vault', [('voltite_ingot', 20, 2, 6), ('aurorite', 8, 1, 3), ('ion_shard', 18, 2, 6), ('cloudberry_tart', 12, 1, 3), ('storm_nectar', 10, 1, 2),
                    ('stormhawk_feather', 12, 2, 5), ('cloudstep_boots', 3, 1, 1), ('voltite_sword', 4, 1, 1), ('aurorite_pickaxe', 2, 1, 1),
                    ('minecraft:saddle', 8, 1, 1), ('galewood_sapling', 10, 1, 3), ('tempest_tonic', 6, 1, 2)],
      {'type': 'minecraft:uniform', 'max': 8, 'min': 4})

# =====================================================================================
# DATA: recipes
# =====================================================================================
def shaped(name, pattern, key, result, count=1, cat='misc', group=None):
    r = {'type': 'minecraft:crafting_shaped', 'category': cat, 'key': {k: v if v.startswith('#') else ns(v) for k, v in key.items()},
         'pattern': pattern, 'result': {'count': count, 'id': ns(result)}}
    if group: r['group'] = group
    w(f'{D}/recipe/{name}.json', r)
def shapeless(name, ings, result, count=1, cat='misc'):
    w(f'{D}/recipe/{name}.json', {'type': 'minecraft:crafting_shapeless', 'category': cat,
       'ingredients': [i if i.startswith('#') else ns(i) for i in ings], 'result': {'count': count, 'id': ns(result)}})
def cook(name, ing, result, xp=0.35, kinds=('smelting',)):
    times = {'smelting': 200, 'blasting': 100, 'smoking': 100, 'campfire_cooking': 600}
    for t in kinds:
        suffix = '' if t == kinds[0] else f'_from_{t}'
        w(f'{D}/recipe/{name}{suffix}.json', {'type': f'minecraft:{t}', 'category': 'food' if t in ('smoking', 'campfire_cooking') or name.startswith('cooked') else 'misc',
           'cookingtime': times[t], 'experience': xp, 'ingredient': ns(ing), 'result': {'id': ns(result)}})
def cut(name, ing, result, count=1):
    w(f'{D}/recipe/{name}.json', {'type': 'minecraft:stonecutting', 'ingredient': ns(ing), 'result': {'count': count, 'id': ns(result)}})
def ssw(base, prefix, wall=True):
    shaped(f'{prefix}_stairs', ['#  ', '## ', '###'], {'#': base}, f'{prefix}_stairs', 4, 'building')
    shaped(f'{prefix}_slab', ['###'], {'#': base}, f'{prefix}_slab', 6, 'building')
    cut(f'{prefix}_stairs_from_{base}_stonecutting', base, f'{prefix}_stairs'); cut(f'{prefix}_slab_from_{base}_stonecutting', base, f'{prefix}_slab', 2)
    if wall:
        shaped(f'{prefix}_wall', ['###', '###'], {'#': base}, f'{prefix}_wall', 6)
        cut(f'{prefix}_wall_from_{base}_stonecutting', base, f'{prefix}_wall')
# the road to the realm
shaped('tempest_sigil', ['CSC', 'HTB', 'CSC'], {'C': 'storm_cell', 'S': 'storm_crystal_block', 'T': 'tempest_core', 'H': 'thunderhead_horn', 'B': 'stormheart'},
       'tempest_sigil', 1, 'misc')
shaped('stormgate_frame', ['FCF', 'CSC', 'FCF'], {'F': 'fulgurite', 'C': 'charged_stormstone', 'S': 'storm_crystal_block'}, 'stormgate_frame', 2, 'building')
shaped('tempest_bricks', ['SS', 'SS'], {'S': 'stormstone_bricks'}, 'tempest_bricks', 1, 'building')  # placeholder overwritten below
os.remove(f'{D}/recipe/tempest_bricks.json')
shapeless('tempest_bricks', ['stormstone_bricks', 'stormstone_bricks', 'stormstone_bricks', 'stormstone_bricks', 'minecraft:amethyst_shard'], 'tempest_bricks', 4, 'building')
ssw('tempest_bricks', 'tempest_brick')
cook('cracked_tempest_bricks', 'tempest_bricks', 'cracked_tempest_bricks', 0.1)
shaped('chiseled_tempest_bricks', ['#', '#'], {'#': 'tempest_brick_slab'}, 'chiseled_tempest_bricks', 1, 'building')
cut('chiseled_tempest_bricks_from_tempest_bricks_stonecutting', 'tempest_bricks', 'chiseled_tempest_bricks')
# skyshale
cook('skyshale', 'cobbled_skyshale', 'skyshale', 0.1)
shaped('polished_skyshale', ['##', '##'], {'#': 'skyshale'}, 'polished_skyshale', 4, 'building')
shaped('skyshale_bricks', ['##', '##'], {'#': 'polished_skyshale'}, 'skyshale_bricks', 4, 'building')
shaped('chiseled_skyshale', ['#', '#'], {'#': 'skyshale_brick_slab'}, 'chiseled_skyshale', 1, 'building')
for res in ('polished_skyshale', 'skyshale_bricks', 'chiseled_skyshale'): cut(f'{res}_from_skyshale_stonecutting', 'skyshale', res)
ssw('cobbled_skyshale', 'cobbled_skyshale'); ssw('skyshale_bricks', 'skyshale_brick')
# galewood
shapeless('galewood_planks', ['#stormreach:galewood_logs'], 'galewood_planks', 4, 'building')
shaped('galewood_stairs', ['#  ', '## ', '###'], {'#': 'galewood_planks'}, 'galewood_stairs', 4, 'building', 'wooden_stairs')
shaped('galewood_slab', ['###'], {'#': 'galewood_planks'}, 'galewood_slab', 6, 'building', 'wooden_slab')
shaped('galewood_fence', ['W#W', 'W#W'], {'W': 'galewood_planks', '#': 'minecraft:stick'}, 'galewood_fence', 3, 'misc', 'wooden_fence')
shaped('galewood_fence_gate', ['#W#', '#W#'], {'W': 'galewood_planks', '#': 'minecraft:stick'}, 'galewood_fence_gate', 1, 'redstone', 'wooden_fence_gate')
shaped('galewood_door', ['##', '##', '##'], {'#': 'galewood_planks'}, 'galewood_door', 3, 'redstone', 'wooden_door')
shaped('galewood_trapdoor', ['###', '###'], {'#': 'galewood_planks'}, 'galewood_trapdoor', 2, 'redstone', 'wooden_trapdoor')
shapeless('galewood_button', ['galewood_planks'], 'galewood_button', 1, 'redstone')
shaped('galewood_pressure_plate', ['##'], {'#': 'galewood_planks'}, 'galewood_pressure_plate', 1, 'redstone')
# metals and gems
cook('voltite_ingot', 'raw_voltite', 'voltite_ingot', 0.7, ('smelting', 'blasting'))
cook('voltite_ingot_from_ore', 'voltite_ore', 'voltite_ingot', 0.7, ('smelting', 'blasting'))
cook('aurorite_from_ore', 'aurorite_ore', 'aurorite', 1.0, ('smelting', 'blasting'))
shaped('voltite_block', ['###', '###', '###'], {'#': 'voltite_ingot'}, 'voltite_block', 1, 'building')
shapeless('voltite_ingot_from_block', ['voltite_block'], 'voltite_ingot', 9)
shaped('aurorite_block', ['###', '###', '###'], {'#': 'aurorite'}, 'aurorite_block', 1, 'building')
shapeless('aurorite_from_block', ['aurorite_block'], 'aurorite', 9)
shaped('ion_lamp', [' I ', 'IGI', ' I '], {'I': 'ion_shard', 'G': 'minecraft:glass'}, 'ion_lamp', 1, 'building')
TOOLPAT = {'sword': ['M', 'M', 'S'], 'pickaxe': ['MMM', ' S ', ' S '], 'axe': ['MM', 'MS', ' S'], 'shovel': ['M', 'S', 'S'], 'hoe': ['MM', ' S', ' S']}
ARMPAT = {'helmet': ['MMM', 'M M'], 'chestplate': ['M M', 'MMM', 'MMM'], 'leggings': ['MMM', 'M M', 'M M'], 'boots': ['M M', 'M M']}
for metal, item in (('voltite', 'voltite_ingot'), ('aurorite', 'aurorite')):
    for t, pat in TOOLPAT.items(): shaped(f'{metal}_{t}', pat, {'M': item, 'S': 'minecraft:stick'}, f'{metal}_{t}', 1, 'equipment')
    for a, pat in ARMPAT.items(): shaped(f'{metal}_{a}', pat, {'M': item}, f'{metal}_{a}', 1, 'equipment')
shaped('cloudstep_boots', ['F F', 'N N'], {'F': 'stormhawk_feather', 'N': 'nimbus_cloud'}, 'cloudstep_boots', 1, 'equipment')
# food
cook('cooked_skyray', 'raw_skyray', 'cooked_skyray', 0.35, ('smelting', 'smoking', 'campfire_cooking'))
shapeless('cloudberry_tart', ['cloudberries', 'cloudberries', 'cloudberries', 'minecraft:sugar', 'minecraft:egg', 'minecraft:wheat'], 'cloudberry_tart')
shapeless('storm_nectar', ['skybloom', 'skybloom', 'minecraft:honey_bottle'], 'storm_nectar')
shaped('gale_bread', ['GGG'], {'G': 'gale_grass'}, 'gale_bread', 1, 'food')
shapeless('light_blue_dye_from_skybloom', ['skybloom'], 'minecraft:light_blue_dye')
shapeless('purple_dye_from_stormcap', ['stormcap'], 'minecraft:purple_dye')

# =====================================================================================
# DATA: tags
# =====================================================================================
def bt(t, vals): tag_add(f'{MC}/block/{t}.json', [ns(v) for v in vals])
def it(t, vals): tag_add(f'{MC}/item/{t}.json', [ns(v) for v in vals])
STONES = ['tempest_bricks', 'cracked_tempest_bricks', 'chiseled_tempest_bricks', 'tempest_brick_stairs', 'tempest_brick_slab', 'tempest_brick_wall',
          'stormgate_frame', 'skyshale', 'cobbled_skyshale', 'cobbled_skyshale_stairs', 'cobbled_skyshale_slab', 'cobbled_skyshale_wall', 'polished_skyshale',
          'skyshale_bricks', 'skyshale_brick_stairs', 'skyshale_brick_slab', 'skyshale_brick_wall', 'chiseled_skyshale', 'voltite_ore', 'aurorite_ore',
          'ion_ore', 'voltite_block', 'aurorite_block']
bt('mineable/pickaxe', STONES)
GALE_WOOD = ['galewood_log', 'stripped_galewood_log', 'galewood_planks', 'galewood_stairs', 'galewood_slab', 'galewood_fence', 'galewood_fence_gate',
             'galewood_door', 'galewood_trapdoor', 'galewood_button', 'galewood_pressure_plate']
bt('mineable/axe', GALE_WOOD); bt('mineable/shovel', ['skysoil', 'static_grass']); bt('mineable/hoe', ['galewood_leaves'])
bt('needs_stone_tool', ['voltite_ore', 'voltite_block', 'ion_ore']); bt('needs_iron_tool', ['aurorite_ore', 'aurorite_block', 'stormgate_frame'])
for t, vals in (('stairs', ['tempest_brick_stairs', 'cobbled_skyshale_stairs', 'skyshale_brick_stairs']), ('slabs', ['tempest_brick_slab', 'cobbled_skyshale_slab', 'skyshale_brick_slab']),
                ('walls', ['tempest_brick_wall', 'cobbled_skyshale_wall', 'skyshale_brick_wall'])):
    bt(t, vals); it(t, vals)
for t, vals in (('logs', ['#stormreach:galewood_logs']), ('logs_that_burn', ['#stormreach:galewood_logs']), ('planks', ['galewood_planks']),
                ('wooden_stairs', ['galewood_stairs']), ('wooden_slabs', ['galewood_slab']), ('wooden_fences', ['galewood_fence']), ('fence_gates', ['galewood_fence_gate']),
                ('wooden_doors', ['galewood_door']), ('wooden_trapdoors', ['galewood_trapdoor']), ('wooden_buttons', ['galewood_button']),
                ('wooden_pressure_plates', ['galewood_pressure_plate']), ('leaves', ['galewood_leaves']), ('saplings', ['galewood_sapling']),
                ('small_flowers', ['skybloom', 'stormcap'])):
    bt(t, vals); it(t, vals)
for k in ('block', 'item'):
    w(f'{D}/tags/{k}/galewood_logs.json', {'values': ['stormreach:galewood_log', 'stormreach:stripped_galewood_log']})
bt('flower_pots' if False else 'dirt', ['skysoil']); bt('grass_blocks', ['static_grass']); bt('animals_spawnable_on', ['static_grass'])
bt('cannot_replace_below_tree_trunk', ['skysoil', 'static_grass'])
bt('portals', ['stormgate']); bt('dragon_immune', ['stormgate', 'sovereign_altar', 'stormgate_frame']); bt('wither_immune', ['stormgate', 'sovereign_altar'])
bt('replaceable_by_trees', ['gale_grass']); bt('replaceable', ['gale_grass']); bt('sword_efficient', ['gale_grass'])
bt('beacon_base_blocks', ['voltite_block', 'aurorite_block'])
tag_add(f'{C}/block/ores.json', ['stormreach:voltite_ore', 'stormreach:aurorite_ore', 'stormreach:ion_ore'])
tag_add(f'{C}/item/ores.json', ['stormreach:voltite_ore', 'stormreach:aurorite_ore', 'stormreach:ion_ore'])
tag_add(f'{C}/item/ingots.json', ['stormreach:voltite_ingot']); tag_add(f'{C}/item/gems.json', ['stormreach:aurorite'])
tag_add(f'{C}/item/raw_materials.json', ['stormreach:raw_voltite'])
tag_add(f'{C}/block/storage_blocks.json', ['stormreach:voltite_block', 'stormreach:aurorite_block'])
tag_add(f'{C}/item/storage_blocks.json', ['stormreach:voltite_block', 'stormreach:aurorite_block'])
it('stone_tool_materials', ['cobbled_skyshale']); it('stone_crafting_materials', ['cobbled_skyshale'])
it('beacon_payment_items', ['voltite_ingot', 'aurorite'])
for t in ('sword', 'pickaxe', 'axe', 'shovel', 'hoe'):
    it(f'{t}s', [f'voltite_{t}', f'aurorite_{t}'])
for a, tagname in (('helmet', 'head_armor'), ('chestplate', 'chest_armor'), ('leggings', 'leg_armor'), ('boots', 'foot_armor')):
    it(tagname, [f'voltite_{a}', f'aurorite_{a}'] + (['cloudstep_boots'] if a == 'boots' else []))
it('fox_food' if False else 'meat', ['raw_skyray', 'cooked_skyray'])
w(f'{D}/tags/item/voltite_tool_materials.json', {'values': ['stormreach:voltite_ingot']})
w(f'{D}/tags/item/aurorite_tool_materials.json', {'values': ['stormreach:aurorite']})
w(f'{D}/tags/item/cloudstep_repair_materials.json', {'values': ['stormreach:nimbus_cloud']})
tag_add(f'{MC}/entity_type/can_equip_saddle.json', ['stormreach:skyray'])
tag_add(f'{MC}/entity_type/fall_damage_immune.json', ['stormreach:tempest_sovereign', 'stormreach:skyray', 'stormreach:static_wisp', 'stormreach:cyclone', 'stormreach:stormhawk'])
tag_add(f'{MC}/entity_type/wither_friends' if False else f'{MC}/entity_type/freeze_immune_entity_types.json', ['stormreach:tempest_sovereign'])

# =====================================================================================
# WORLDGEN: the Tempest Realm dimension
# =====================================================================================
w(f'{D}/dimension_type/tempest_realm.json', {
    'ambient_light': 0.1,
    'attributes': {
        'minecraft:audio/ambient_sounds': {'mood': {'block_search_extent': 8, 'offset': 2.0, 'sound': 'minecraft:ambient.cave', 'tick_delay': 6000}},
        'minecraft:audio/background_music': {'default': {'max_delay': 24000, 'min_delay': 6000, 'sound': 'minecraft:music.overworld.jagged_peaks'}},
        'minecraft:gameplay/bed_rule': {'can_set_spawn': 'always', 'can_sleep': 'when_dark', 'error_message': {'translate': 'block.minecraft.bed.no_sleep'}},
        'minecraft:gameplay/respawn_anchor_works': False,
        'minecraft:gameplay/straw_bed_rule': {'can_set_spawn': 'never', 'can_sleep': 'when_dark', 'destroy_on_leave': True, 'error_message': {'translate': 'block.minecraft.bed.no_sleep'}},
        'minecraft:visual/ambient_light_color': '#1a1f33',
        'minecraft:visual/cloud_color': '#cc3a4a6e',
        'minecraft:visual/cloud_height': 60.0,
        'minecraft:visual/fog_color': '#4a5a8c',
        'minecraft:visual/sky_color': '#3b4f7c'},
    'coordinate_scale': 1.0, 'default_clock': 'minecraft:overworld', 'has_ceiling': False, 'has_ender_dragon_fight': False,
    'has_skylight': True, 'height': 256, 'infiniburn': '#minecraft:infiniburn_overworld', 'logical_height': 256, 'min_y': 0,
    'monster_spawn_block_light_limit': 0, 'monster_spawn_light_level': {'type': 'minecraft:uniform', 'max_inclusive': 7, 'min_inclusive': 0},
    'timelines': '#minecraft:in_overworld'})
REALM_BIOMES = {
    # name: (temperature, humidity) centre, sky, fog
    'galewood_highlands': ((0.0, 0.3), '#4d6a9c', '#5b77a8'),
    'static_fields': ((0.4, -0.3), '#5a7ab8', '#6d8cc4'),
    'cloudsea_expanse': ((-0.5, 0.0), '#6a84b0', '#8aa2c8'),
    'aurora_spires': ((0.6, 0.5), '#3a2f6b', '#4b3f80'),
}
def realm_params(t, h):
    full = [-1.0, 1.0]
    return {'continentalness': full, 'depth': 0.0, 'erosion': full, 'humidity': [h - 0.35, h + 0.35], 'offset': 0.0,
            'temperature': [t - 0.35, t + 0.35], 'weirdness': full}
w(f'{D}/dimension/tempest_realm.json', {'type': 'stormreach:tempest_realm', 'generator': {'type': 'minecraft:noise',
    'biome_source': {'type': 'minecraft:multi_noise', 'biomes': [{'biome': f'stormreach:{n}', 'parameters': realm_params(*v[0])} for n, v in REALM_BIOMES.items()]},
    'settings': 'stormreach:tempest_realm'}})
ns_settings = rj(f'{VD}/worldgen/noise_settings/floating_islands.json')
ow = rj(f'{VD}/worldgen/noise_settings/overworld.json')['noise_router']
ns_settings['noise_router']['temperature'] = ow['temperature']; ns_settings['noise_router']['vegetation'] = ow['vegetation']
ns_settings['default_block'] = 'stormreach:skyshale'; ns_settings['default_fluid'] = 'minecraft:air'
ns_settings['material_rule'] = 'stormreach:tempest_realm'; ns_settings['disable_mob_generation'] = False
w(f'{WG}/noise_settings/tempest_realm.json', ns_settings)
def blk(b): return {'type': 'minecraft:block', 'result_state': b}
w(f'{WG}/material_rule/tempest_realm.json', {'type': 'minecraft:sequence', 'sequence': [
    {'type': 'minecraft:condition', 'if_true': {'type': 'minecraft:biome', 'biome_is': ['stormreach:aurora_spires']},
     'then_run': {'type': 'minecraft:condition', 'if_true': 'minecraft:on_floor', 'then_run': blk('stormreach:polished_skyshale')}},
    {'type': 'minecraft:condition', 'if_true': 'minecraft:on_floor', 'then_run': blk('stormreach:static_grass')},
    {'type': 'minecraft:condition', 'if_true': 'minecraft:under_floor', 'then_run': blk('stormreach:skysoil')}]})
# features
def wf(name, obj): w(f'{WG}/feature/{name}.json', obj)
def wp(name, feature, placement): w(f'{WG}/placed_feature/{name}.json', {'feature': ns(feature), 'placement': placement})
def tree(name, base, height, foliage_extra=None):
    t = rj(f'{VD}/worldgen/feature/{base}.json')
    t['trunk_provider'] = {'id': 'stormreach:galewood_log', 'properties': {'axis': 'y'}}
    t['foliage_provider'] = {'id': 'stormreach:galewood_leaves', 'properties': {'distance': '7', 'persistent': 'false', 'waterlogged': 'false'}}
    t['decorators'] = []
    if height: t['trunk_placer']['base_height'] = height
    wf(name, t)
tree('galewood', 'birch', 6); tree('tall_galewood', 'fancy_oak', None)
wf('trees_galewood', {'type': 'minecraft:random_selector', 'default': 'stormreach:galewood_checked',
    'features': [{'chance': 0.3, 'feature': 'stormreach:tall_galewood_checked'}]})
SURVIVE = {'type': 'minecraft:block_predicate_filter', 'predicate': {'type': 'minecraft:would_survive', 'state': {'id': 'stormreach:galewood_sapling', 'properties': {'stage': '0'}}}}
wp('galewood_checked', 'galewood', [SURVIVE]); wp('tall_galewood_checked', 'tall_galewood', [SURVIVE])
def tree_place(name, counts):
    wp(name, 'trees_galewood', [{'type': 'minecraft:count', 'count': {'type': 'minecraft:weighted_list', 'distribution': [{'data': c, 'weight': wt} for c, wt in counts]}},
        {'type': 'minecraft:in_square'}, {'type': 'minecraft:heightmap', 'heightmap': 'OCEAN_FLOOR'}, {'type': 'minecraft:biome'}])
tree_place('trees_galewood_dense', ((3, 5), (5, 3), (1, 2))); tree_place('trees_galewood_sparse', ((0, 6), (1, 3)))
def ore(name, state, size, count, lo, hi, target='stormreach:skyshale', air=0.0, rarity=None):
    wf(name, {'type': 'minecraft:ore', 'discard_chance_on_air_exposure': air, 'size': size,
              'targets': [{'state': state, 'target': {'predicate_type': 'minecraft:block_match', 'block': target}}]})
    pl = ([{'type': 'minecraft:rarity_filter', 'chance': rarity}] if rarity else [{'type': 'minecraft:count', 'count': count}]) + [{'type': 'minecraft:in_square'},
        {'type': 'minecraft:height_range', 'height': {'type': 'minecraft:uniform', 'max_inclusive': {'absolute': hi}, 'min_inclusive': {'absolute': lo}}},
        {'type': 'minecraft:biome'}]
    wp(name, name, pl)
ore('ore_voltite', 'stormreach:voltite_ore', 9, 14, 0, 200)
ore('ore_aurorite', 'stormreach:aurorite_ore', 4, 3, 0, 120, air=0.5)
ore('ore_aurorite_rich', 'stormreach:aurorite_ore', 5, 6, 0, 200, air=0.3)
ore('ore_ion', 'stormreach:ion_ore', 7, 8, 0, 220)
# clouds: "ores" that replace air, floating between and below the islands
ore('cloud_nimbus', 'stormreach:nimbus_cloud', 48, 0, 20, 200, target='minecraft:air', rarity=2)
ore('cloud_thunder', 'stormreach:thundercloud', 40, 0, 10, 160, target='minecraft:air', rarity=4)
ore('cloud_gale', 'stormreach:gale_cloud', 44, 0, 40, 220, target='minecraft:air', rarity=5)
def patch(name, block, chance, count, spread):
    wf(name, {'type': 'minecraft:simple_block', 'to_place': {'id': ns(block)}})
    tri = lambda m: {'type': 'minecraft:trapezoid', 'max': m, 'min': -m, 'plateau': 0}
    wp(name, name, [{'type': 'minecraft:rarity_filter', 'chance': chance}, {'type': 'minecraft:in_square'},
        {'type': 'minecraft:heightmap', 'heightmap': 'MOTION_BLOCKING'}, {'type': 'minecraft:biome'}, {'type': 'minecraft:count', 'count': count},
        {'type': 'minecraft:offset', 'x': tri(spread), 'y': tri(3), 'z': tri(spread)},
        {'type': 'minecraft:block_predicate_filter', 'predicate': {'type': 'minecraft:all_of', 'predicates': [
            {'type': 'minecraft:matching_block_tag', 'tag': 'minecraft:air'}, {'type': 'minecraft:would_survive', 'state': {'id': ns(block)}}]}}])
patch('patch_skybloom', 'skybloom', 2, 24, 7); patch('patch_stormcap', 'stormcap', 4, 12, 5); patch('patch_gale_grass', 'gale_grass', 1, 48, 7)
# ion crystal spires on the aurora islands: a column of ion lamps and polished skyshale
wf('ion_spire', {'type': 'minecraft:block_column', 'allowed_placement': {'type': 'minecraft:matching_block_tag', 'tag': 'minecraft:air'}, 'direction': 'up',
    'layers': [{'height': {'type': 'minecraft:uniform', 'max_inclusive': 5, 'min_inclusive': 2}, 'provider': {'id': 'stormreach:polished_skyshale'}},
               {'height': 1, 'provider': {'id': 'stormreach:ion_lamp'}}], 'prioritize_tip': False})
wp('ion_spire', 'ion_spire', [{'type': 'minecraft:rarity_filter', 'chance': 2}, {'type': 'minecraft:in_square'},
    {'type': 'minecraft:heightmap', 'heightmap': 'MOTION_BLOCKING'}, {'type': 'minecraft:biome'}])
def cnt(t, lo, hi, wt): return {'type': t, 'count': {'type': 'minecraft:uniform', 'max_inclusive': hi, 'min_inclusive': lo}, 'weight': wt}
BIO_FEATURES = {
    'galewood_highlands': (['stormreach:cloud_nimbus', 'stormreach:cloud_gale'], ['stormreach:ore_voltite', 'stormreach:ore_aurorite', 'stormreach:ore_ion'],
                           ['stormreach:trees_galewood_dense', 'stormreach:patch_gale_grass', 'stormreach:patch_skybloom']),
    'static_fields': (['stormreach:cloud_nimbus', 'stormreach:cloud_thunder'], ['stormreach:ore_voltite', 'stormreach:ore_aurorite', 'stormreach:ore_ion'],
                      ['stormreach:trees_galewood_sparse', 'stormreach:patch_gale_grass', 'stormreach:patch_skybloom', 'stormreach:patch_stormcap']),
    'cloudsea_expanse': (['stormreach:cloud_nimbus', 'stormreach:cloud_thunder', 'stormreach:cloud_gale'], ['stormreach:ore_voltite', 'stormreach:ore_ion'],
                         ['stormreach:trees_galewood_sparse', 'stormreach:patch_gale_grass']),
    'aurora_spires': (['stormreach:cloud_thunder'], ['stormreach:ore_voltite', 'stormreach:ore_aurorite_rich', 'stormreach:ore_ion'],
                      ['stormreach:ion_spire', 'stormreach:patch_stormcap']),
}
SPAWNS = {
    'galewood_highlands': ([cnt('stormreach:puffling', 2, 4, 10), cnt('stormreach:skyray', 1, 2, 6)], [cnt('stormreach:static_wisp', 1, 2, 60), cnt('stormreach:stormhawk', 1, 1, 20), cnt('stormreach:cyclone', 1, 1, 15)]),
    'static_fields': ([cnt('stormreach:puffling', 3, 5, 14), cnt('stormreach:skyray', 1, 2, 4)], [cnt('stormreach:static_wisp', 1, 3, 70), cnt('stormreach:cyclone', 1, 2, 25)]),
    'cloudsea_expanse': ([cnt('stormreach:skyray', 2, 3, 12), cnt('stormreach:puffling', 1, 2, 4)], [cnt('stormreach:stormhawk', 1, 2, 40), cnt('stormreach:static_wisp', 1, 1, 30)]),
    'aurora_spires': ([cnt('stormreach:skyray', 1, 1, 4)], [cnt('stormreach:static_wisp', 2, 4, 80), cnt('stormreach:stormhawk', 1, 1, 20), cnt('stormreach:cyclone', 1, 1, 10)]),
}
for name, ((t, h), sky, fog) in REALM_BIOMES.items():
    air, ores, veg = BIO_FEATURES[name]
    creatures, monsters = SPAWNS[name]
    feats = [[] for _ in range(11)]
    feats[2] = air; feats[6] = ores; feats[9] = veg
    w(f'{WG}/biome/{name}.json', {
        'attributes': {
            'minecraft:gameplay/natural_mob_spawns': {'argument': {'spawn_costs': {}, 'spawns_by_category': {'creature': creatures, 'monster': monsters}}},
            'minecraft:visual/sky_color': sky, 'minecraft:visual/fog_color': fog, 'minecraft:visual/water_fog_color': '#2a3f66',
            'minecraft:visual/ambient_particles': {'argument': [{'particle': {'type': 'minecraft:electric_spark'}, 'probability': 0.004 if name != 'aurora_spires' else 0.012}], 'modifier': 'append'}},
        'carvers': [], 'downfall': 0.6, 'effects': {'water_color': '#5b8fd8', 'grass_color': '#4fb8c4', 'foliage_color': '#7fc8d8'},
        'features': feats, 'has_precipitation': False, 'temperature': 0.5})
w(f'{D}/tags/worldgen/biome/is_tempest_realm.json', {'values': [f'stormreach:{n}' for n in REALM_BIOMES]})

# =====================================================================================
# STRUCTURES: Tempest Spire (Overworld) and Sky Vault (realm)
# =====================================================================================
def build_nbt(name, size, b, chests, entities=()):
    palette = []; pindex = {}
    def st(key):
        bid, props = key
        k = (bid, tuple(sorted(props.items())))
        if k not in pindex:
            e = {'id': T.String(bid)}
            if props: e['properties'] = T.Compound({kk: T.String(vv) for kk, vv in props.items()})
            pindex[k] = len(palette); palette.append(T.Compound(e))
        return pindex[k]
    bl = []
    for pos, key in sorted(b.items()):
        e = {'pos': T.List[T.Int]([T.Int(v) for v in pos]), 'state': T.Int(st(key))}
        if pos in chests: e['nbt'] = T.Compound({'LootTable': T.String(chests[pos]), 'components': T.Compound({}), 'id': T.String('minecraft:chest')})
        bl.append(T.Compound(e))
    root = T.Compound({'size': T.List[T.Int]([T.Int(v) for v in size]), 'entities': T.List[T.Compound]([]), 'blocks': T.List[T.Compound](bl),
                       'palette': T.List[T.Compound](palette), 'DataVersion': T.Int(5023)})
    os.makedirs(f'{D}/structure', exist_ok=True)
    nbtlib.File(root, root_name='').save(f'{D}/structure/{name}.nbt', gzipped=True)
def S(bid, **p): return (bid if ':' in bid else 'stormreach:' + bid, p)
import random
AIR = S('minecraft:air')
WALLP = dict(up='true', north='none', south='none', east='none', west='none', waterlogged='false')
# ---- Tempest Spire: 11x11 footprint, 34 tall ----
rnd = random.Random(7001); b = {}; chests = {}
W, H = 11, 34
for x in range(W):
    for z in range(W):
        for y in range(H): b[(x, y, z)] = AIR
def brick(): return S('cracked_tempest_bricks') if rnd.random() < 0.2 else S('tempest_bricks')
for x in range(W):
    for z in range(W):
        b[(x, 0, z)] = S('tempest_bricks')  # foundation
# tower body: a 7x7 hollow shaft (x,z 2..8), with floors every 8 blocks
for y in range(1, 26):
    for x in range(2, 9):
        for z in range(2, 9):
            edge = x in (2, 8) or z in (2, 8)
            corner = x in (2, 8) and z in (2, 8)
            if edge:
                b[(x, y, z)] = S('chiseled_tempest_bricks') if corner and y % 8 == 0 else brick()
            elif y % 8 == 0:
                b[(x, y, z)] = S('tempest_bricks')
    # windows
    if y % 8 in (3, 4):
        for (x, z) in ((5, 2), (5, 8), (2, 5), (8, 5)): b[(x, y, z)] = S('stormglass_pane' if False else 'minecraft:air')
# entrance (south side z=8)
for y in (1, 2): b[(5, y, 8)] = AIR
b[(5, 3, 8)] = S('tempest_brick_stairs', facing='north', half='top', shape='straight', waterlogged='false')
# ladder column against the north wall, with holes through the floors
for y in range(1, 26):
    b[(5, y, 3)] = S('minecraft:ladder', facing='south', waterlogged='false')
# buttresses at the four outer corners
for (x, z) in ((1, 1), (1, 9), (9, 1), (9, 9)):
    for y in range(1, 14 - (rnd.random() < 0.5) * 3): b[(x, y, z)] = S('tempest_brick_wall', **WALLP)
for (x, z) in ((1, 5), (9, 5), (5, 1)):
    for y in range(1, 6): b[(x, y, z)] = brick()
# chests on the first two floors
b[(7, 9, 7)] = S('minecraft:chest', facing='west', type='single', waterlogged='false'); chests[(7, 9, 7)] = 'stormreach:chests/tempest_spire'
b[(3, 17, 7)] = S('minecraft:chest', facing='east', type='single', waterlogged='false'); chests[(3, 17, 7)] = 'stormreach:chests/tempest_spire'
b[(7, 1, 7)] = S('minecraft:chest', facing='west', type='single', waterlogged='false'); chests[(7, 1, 7)] = 'stormreach:chests/tempest_spire'
for (x, y, z) in ((3, 9, 3), (7, 17, 3), (3, 1, 5)): b[(x, y, z)] = S('storm_lantern')
# top: a wide 11x11 platform with the altar, ringed by pillars that end in charged stone
for x in range(W):
    for z in range(W):
        d = max(abs(x - 5), abs(z - 5))
        if d <= 5 and not (abs(x - 5) == 5 and abs(z - 5) == 5): b[(x, 26, z)] = S('tempest_bricks') if d < 5 else S('tempest_brick_slab', type='bottom', waterlogged='false')
b[(5, 26, 3)] = AIR  # ladder exit
b[(5, 25, 3)] = S('minecraft:ladder', facing='south', waterlogged='false')
for (x, z) in ((1, 1), (1, 9), (9, 1), (9, 9), (5, 0), (0, 5), (10, 5), (5, 10)):
    if (x, z) in ((5, 0), (0, 5), (10, 5), (5, 10)):
        continue
    for y in range(27, 31): b[(x, y, z)] = S('tempest_brick_wall', **WALLP)
    b[(x, 31, z)] = S('charged_stormstone')
for (x, z) in ((4, 4), (6, 4), (4, 6), (6, 6)): b[(x, 27, z)] = S('chiseled_tempest_bricks')
b[(5, 27, 5)] = S('sovereign_altar', ritual='0')
for (x, z, f) in ((5, 4, 'south'), (4, 5, 'east'), (6, 5, 'west'), (5, 6, 'north')):
    b[(x, 27, z)] = S('tempest_brick_stairs', facing=f, half='bottom', shape='straight', waterlogged='false')
build_nbt('tempest_spire', (W, H, W), b, chests)
w(f'{WG}/template_pool/tempest_spire.json', {'elements': [{'element': {'element_type': 'minecraft:single_pool_element', 'location': 'stormreach:tempest_spire',
    'processors': {'processors': []}, 'projection': 'rigid'}, 'weight': 1}], 'fallback': 'minecraft:empty'})
w(f'{WG}/structure/tempest_spire.json', {'type': 'minecraft:jigsaw', 'biomes': '#stormreach:has_tempest_spire', 'max_distance_from_center': 80,
    'project_start_to_heightmap': 'WORLD_SURFACE_WG', 'size': 1, 'spawn_overrides': {}, 'start_height': {'absolute': 0},
    'start_pool': 'stormreach:tempest_spire', 'step': 'surface_structures', 'terrain_adaptation': 'beard_thin', 'use_expansion_hack': False})
w(f'{WG}/structure_set/tempest_spire.json', {'placement': {'type': 'minecraft:random_spread', 'salt': 271828182, 'separation': 16, 'spacing': 48},
    'structures': [{'structure': 'stormreach:tempest_spire', 'weight': 1}]})
w(f'{D}/tags/worldgen/biome/has_tempest_spire.json', {'values': ['stormreach:stormreach_coast', 'stormreach:thunderspire_highlands', 'stormreach:squallwind_bluffs']})
# ---- Sky Vault: a floating ruin high in the realm sky ----
rnd = random.Random(7002); b = {}; chests = {}
W, H = 13, 9
for x in range(W):
    for z in range(W):
        for y in range(H): b[(x, y, z)] = AIR
for x in range(W):
    for z in range(W):
        r = ((x - 6) ** 2 + (z - 6) ** 2) ** 0.5
        if r <= 6.3: b[(x, 1, z)] = S('skyshale_bricks') if rnd.random() > 0.1 else S('cobbled_skyshale')
        if r <= 4.5: b[(x, 0, z)] = S('skyshale')
        if r <= 2.5 and rnd.random() < 0.8: b[(x, -0 if False else 0, z)] = S('ion_ore') if rnd.random() < 0.2 else S('skyshale')
for x in range(3, 10):
    for z in range(3, 10):
        edge = x in (3, 9) or z in (3, 9)
        for y in range(2, 7):
            if edge and not (z == 9 and x == 6 and y < 4) and rnd.random() > (0.05 if y < 5 else 0.45):
                b[(x, y, z)] = S('skyshale_bricks') if (x + y) % 5 else S('chiseled_skyshale')
        if y == 6 and not edge: pass
for x in range(4, 9):
    for z in range(4, 9):
        if rnd.random() > 0.35: b[(x, 6, z)] = S('skyshale_brick_slab', type='bottom', waterlogged='false')
b[(6, 2, 7)] = S('minecraft:chest', facing='north', type='single', waterlogged='false'); chests[(6, 2, 7)] = 'stormreach:chests/sky_vault'
b[(4, 2, 4)] = S('minecraft:chest', facing='east', type='single', waterlogged='false'); chests[(4, 2, 4)] = 'stormreach:chests/sky_vault'
b[(8, 2, 4)] = S('ion_lamp'); b[(4, 2, 8)] = S('ion_lamp'); b[(6, 2, 5)] = S('aurorite_block') if rnd.random() < 0.3 else S('voltite_block')
for (x, z) in ((0, 6), (12, 6), (6, 0), (2, 2), (10, 10), (11, 3)):
    b[(x, 2, z)] = S('nimbus_cloud'); b[(x, 1, z)] = S('nimbus_cloud')
b = {k: v for k, v in b.items() if k[1] >= 0}
build_nbt('sky_vault', (W, H, W), b, chests)
w(f'{WG}/template_pool/sky_vault.json', {'elements': [{'element': {'element_type': 'minecraft:single_pool_element', 'location': 'stormreach:sky_vault',
    'processors': {'processors': []}, 'projection': 'rigid'}, 'weight': 1}], 'fallback': 'minecraft:empty'})
w(f'{WG}/structure/sky_vault.json', {'type': 'minecraft:jigsaw', 'biomes': '#stormreach:is_tempest_realm', 'max_distance_from_center': 80,
    'size': 1, 'spawn_overrides': {}, 'start_height': {'type': 'minecraft:uniform', 'max_inclusive': {'absolute': 200}, 'min_inclusive': {'absolute': 150}},
    'start_pool': 'stormreach:sky_vault', 'step': 'surface_structures', 'terrain_adaptation': 'none', 'use_expansion_hack': False})
w(f'{WG}/structure_set/sky_vault.json', {'placement': {'type': 'minecraft:random_spread', 'salt': 314159265, 'separation': 8, 'spacing': 24},
    'structures': [{'structure': 'stormreach:sky_vault', 'weight': 1}]})

# =====================================================================================
# ADVANCEMENTS
# =====================================================================================
def has(i, count=None):
    c = {'items': ns(i)}
    if count: c['count'] = {'min': count}
    return {'conditions': {'items': [c]}, 'trigger': 'minecraft:inventory_changed'}
def adv(name, parent, icon, crit, title, desc, frame='task', hidden=False, reqs=None):
    w(f'{D}/advancement/{name}.json', {'parent': f'stormreach:{parent}', 'criteria': crit,
       'display': {'title': {'translate': f'advancements.stormreach.{name}.title'}, 'description': {'translate': f'advancements.stormreach.{name}.description'},
                   'icon': {'id': ns(icon)}, 'frame': frame, 'hidden': hidden}, 'requirements': reqs or [list(crit)]})
    L(f'advancements.stormreach.{name}.title', title); L(f'advancements.stormreach.{name}.description', desc)
def loc(struct): return {'conditions': {'player': {'type': 'minecraft:entity_properties', 'entity': 'this', 'predicate': {'minecraft:location': {'structures': struct}}}}, 'trigger': 'minecraft:location'}
adv('tempest_spire', 'stormwarden', 'chiseled_tempest_bricks', {'l': loc('stormreach:tempest_spire')}, 'Where the Storm Is Crowned', 'Find a Tempest Spire')
adv('tempest_sigil', 'tempest_spire', 'tempest_sigil', {'s': has('tempest_sigil')}, 'A Royal Summons', 'Craft a Tempest Sigil')
adv('tempest_sovereign', 'tempest_sigil', 'tempest_eye', {'k': {'conditions': {'entity': {'type': 'minecraft:entity_properties', 'entity': 'this',
    'predicate': {'minecraft:entity_type': 'stormreach:tempest_sovereign'}}}, 'trigger': 'minecraft:player_killed_entity'}},
    'Regicide', 'Defeat the Tempest Sovereign', frame='challenge')
adv('four_eyes', 'tempest_sovereign', 'tempest_eye', {'e': has('tempest_eye', 4)}, 'Eyes of the Storm', 'Hold four Tempest Eyes at once', frame='goal')
adv('tempest_realm', 'four_eyes', 'stormgate_frame', {'d': {'conditions': {'to': 'stormreach:tempest_realm'}, 'trigger': 'minecraft:changed_dimension'}},
    'Above the Storm', 'Open a Stormgate and enter the Tempest Realm', frame='goal')
adv('ride_skyray', 'tempest_realm', 'minecraft:saddle', {'r': {'conditions': {'player': {'type': 'minecraft:entity_properties', 'entity': 'this',
    'predicate': {'minecraft:vehicle': {'minecraft:entity_type': 'stormreach:skyray'}}}}, 'trigger': 'minecraft:tick'}}, 'Ray Rider', 'Saddle and ride a Skyray')
adv('voltite', 'tempest_realm', 'voltite_ingot', {'v': has('voltite_ingot')}, 'Bottled Current', 'Smelt a Voltite Ingot')
adv('aurorite', 'voltite', 'aurorite', {'a': has('aurorite')}, 'Northern Lights', 'Mine Aurorite')
adv('aurorite_armor', 'aurorite', 'aurorite_chestplate', {p: has(f'aurorite_{p}') for p in ('helmet', 'chestplate', 'leggings', 'boots')},
    'Dressed for the Storm', 'Wear a full set of Aurorite armor', frame='challenge', reqs=[['helmet'], ['chestplate'], ['leggings'], ['boots']])
adv('sky_vault', 'tempest_realm', 'ion_lamp', {'l': loc('stormreach:sky_vault')}, 'Castles in the Air', 'Find a Sky Vault')
adv('cloudstep', 'tempest_realm', 'cloudstep_boots', {'c': has('cloudstep_boots')}, 'Head in the Clouds', 'Make Cloudstep Boots')
adv('fell_from_sky', 'tempest_realm', 'nimbus_cloud', {'d': {'conditions': {'from': 'stormreach:tempest_realm', 'to': 'minecraft:overworld'}, 'trigger': 'minecraft:changed_dimension'}},
    'Long Way Down', 'Leave the Tempest Realm', hidden=True)

# =====================================================================================
# LANGUAGE
# =====================================================================================
NAMES = {
 'tempest_bricks': 'Tempest Bricks', 'cracked_tempest_bricks': 'Cracked Tempest Bricks', 'chiseled_tempest_bricks': 'Chiseled Tempest Bricks',
 'tempest_brick_stairs': 'Tempest Brick Stairs', 'tempest_brick_slab': 'Tempest Brick Slab', 'tempest_brick_wall': 'Tempest Brick Wall',
 'sovereign_altar': 'Sovereign Altar', 'stormgate_frame': 'Stormgate Frame', 'stormgate': 'Stormgate',
 'skyshale': 'Skyshale', 'cobbled_skyshale': 'Cobbled Skyshale', 'cobbled_skyshale_stairs': 'Cobbled Skyshale Stairs', 'cobbled_skyshale_slab': 'Cobbled Skyshale Slab',
 'cobbled_skyshale_wall': 'Cobbled Skyshale Wall', 'polished_skyshale': 'Polished Skyshale', 'skyshale_bricks': 'Skyshale Bricks',
 'skyshale_brick_stairs': 'Skyshale Brick Stairs', 'skyshale_brick_slab': 'Skyshale Brick Slab', 'skyshale_brick_wall': 'Skyshale Brick Wall',
 'chiseled_skyshale': 'Chiseled Skyshale', 'skysoil': 'Skysoil', 'static_grass': 'Static Grass', 'nimbus_cloud': 'Nimbus Cloud',
 'thundercloud': 'Thundercloud', 'gale_cloud': 'Gale Cloud', 'galewood_log': 'Galewood Log', 'stripped_galewood_log': 'Stripped Galewood Log',
 'galewood_planks': 'Galewood Planks', 'galewood_leaves': 'Galewood Leaves', 'galewood_sapling': 'Galewood Sapling', 'galewood_stairs': 'Galewood Stairs',
 'galewood_slab': 'Galewood Slab', 'galewood_fence': 'Galewood Fence', 'galewood_fence_gate': 'Galewood Fence Gate', 'galewood_door': 'Galewood Door',
 'galewood_trapdoor': 'Galewood Trapdoor', 'galewood_button': 'Galewood Button', 'galewood_pressure_plate': 'Galewood Pressure Plate',
 'voltite_ore': 'Voltite Ore', 'aurorite_ore': 'Aurorite Ore', 'ion_ore': 'Ion Ore', 'voltite_block': 'Block of Voltite', 'aurorite_block': 'Block of Aurorite',
 'ion_lamp': 'Ion Lamp', 'skybloom': 'Skybloom', 'stormcap': 'Stormcap', 'gale_grass': 'Gale Grass'}
for k, v in NAMES.items(): L(f'block.stormreach.{k}', v)
ITEMS = {'tempest_sigil': 'Tempest Sigil', 'tempest_eye': 'Tempest Eye', 'raw_voltite': 'Raw Voltite', 'voltite_ingot': 'Voltite Ingot', 'aurorite': 'Aurorite',
 'stormhawk_feather': 'Stormhawk Feather', 'ion_shard': 'Ion Shard', 'cloudberries': 'Cloudberries', 'cloudberry_tart': 'Cloudberry Tart',
 'raw_skyray': 'Raw Skyray', 'cooked_skyray': 'Cooked Skyray', 'storm_nectar': 'Storm Nectar', 'gale_bread': 'Gale Bread', 'cloudstep_boots': 'Cloudstep Boots'}
for m in ('voltite', 'aurorite'):
    for p in ('sword', 'pickaxe', 'axe', 'shovel', 'hoe', 'helmet', 'chestplate', 'leggings', 'boots'):
        ITEMS[f'{m}_{p}'] = f'{m.title()} {p.title()}'
ENT = {'tempest_sovereign': 'Tempest Sovereign', 'skyray': 'Skyray', 'puffling': 'Puffling', 'static_wisp': 'Static Wisp', 'cyclone': 'Cyclone', 'stormhawk': 'Stormhawk'}
for k, v in ENT.items():
    L(f'entity.stormreach.{k}', v); ITEMS[f'{k}_spawn_egg'] = f'{v} Spawn Egg'
for k, v in ITEMS.items(): L(f'item.stormreach.{k}', v)
for k, v in {'galewood_highlands': 'Galewood Highlands', 'static_fields': 'Static Fields', 'cloudsea_expanse': 'Cloudsea Expanse', 'aurora_spires': 'Aurora Spires'}.items():
    L(f'biome.stormreach.{k}', v)
L('itemGroup.stormreach.tempest_realm', 'Stormreach: Tempest Realm')
L('dimension.stormreach.tempest_realm', 'Tempest Realm')
L('dimension.stormreach.tempest_realm.enter', 'You step through the storm and into the Tempest Realm.')
L('dimension.stormreach.tempest_realm.fell', 'You tumble out of the Tempest Realm and through the clouds below...')
TT = {
 'item.stormreach.tempest_sigil.tooltip': 'Use it on the Sovereign Altar atop a Tempest Spire.',
 'item.stormreach.tempest_sigil.tooltip2': 'It calls a thunderstorm and begins a long ritual;',
 'item.stormreach.tempest_sigil.tooltip3': 'when the ritual ends, the Tempest Sovereign descends.',
 'item.stormreach.tempest_eye.tooltip': "Torn from the Tempest Sovereign's crown.",
 'item.stormreach.tempest_eye.tooltip2': 'Set Eyes into a ring of 16 Stormgate Frames around 3x3 stormwater,',
 'item.stormreach.tempest_eye.tooltip3': 'then call lightning onto the pool to open the way to the Tempest Realm.',
 'block.stormreach.stormgate_frame.tooltip': 'Build a flat 5x5 ring of 16 frames around a 3x3 pool of stormwater sources.',
 'block.stormreach.stormgate_frame.tooltip2': 'Set Tempest Eyes into the frames, then strike the pool with lightning.',
 'block.stormreach.stormgate_frame.tooltip3': 'Breaking a frame shatters its Eye.',
 'block.stormreach.sovereign_altar.tooltip': 'The Tempest Sovereign answers this altar during a thunderstorm,',
 'block.stormreach.sovereign_altar.tooltip2': 'or when a Tempest Sigil is offered.',
 'block.stormreach.nimbus_cloud.tooltip': 'Soft: you sink slowly through it and take no fall damage.',
 'block.stormreach.thundercloud.tooltip': 'Charged: it flings you high into the air.',
 'block.stormreach.gale_cloud.tooltip': 'Windy: it carries you along with the wind.',
 'item.stormreach.ion_shard.tooltip': 'Crunch one to heal instantly.',
 'item.stormreach.cloudstep_boots.tooltip': 'No fall damage. Sneak in mid-air to glide.',
 'item.stormreach.voltite_sword.tooltip': 'Every hit arcs to one more nearby enemy.',
 'item.stormreach.aurorite_sword.tooltip': 'Every hit arcs to one more nearby enemy.',
}
lang.update(TT)
ALT = {'busy': 'The altar is already crackling with a ritual.', 'already': 'The Tempest Sovereign is already abroad.',
       'dormant': 'The altar is cold. It answers only a thunderstorm, or a Tempest Sigil.', 'stirring': 'The altar hums with the storm overhead...',
       'ritual_begins': 'The sky splits. A ritual of the storm has begun.', 'awakens': 'The Sovereign Altar stirs in the storm...',
       'descends': 'The Tempest Sovereign descends!'}
for k, v in ALT.items(): L(f'block.stormreach.sovereign_altar.{k}', v)
GATE = {'ring': 'Stormgate ring: %1$s/16 frames.', 'eyes': 'Tempest Eyes: %2$s/%3$s set.', 'pool': 'The ring is ready. Fill the 3x3 centre with stormwater sources (%4$s/9).',
        'ready': 'The Stormgate is complete. Call lightning onto its stormwater!', 'open': 'The Stormgate is open.', 'opened': 'Lightning tears the sky open: the Stormgate is open!'}
for k, v in GATE.items(): L(f'block.stormreach.stormgate.{k}', v)
L('entity.stormreach.tempest_sovereign.phase2', 'The Tempest Sovereign calls the Squalls to its side!')
L('entity.stormreach.tempest_sovereign.phase3', 'The Tempest Sovereign rages! The storm begins to pull...')
L('entity.stormreach.tempest_sovereign.defeated', 'The Tempest Sovereign falls. One of its Eyes remains.')
CFG = {'sovereignNaturalSpawns': ('Sovereign Answers Storms', 'During a thunderstorm the Sovereign Altar may summon the Tempest Sovereign by itself when a player is near.'),
       'sovereignSpawnChance': ('Natural Summon Chance', 'Chance per altar tick during a thunderstorm. Lower makes the Sovereign rarer.'),
       'sigilRitualSeconds': ('Sigil Ritual Length', 'How long the Tempest Sigil ritual lasts before the Sovereign descends.'),
       'sovereignHealth': ('Sovereign Health', 'Maximum health of the Tempest Sovereign.'),
       'eyesRequired': ('Eyes to Open a Stormgate', 'How many Tempest Eyes a Stormgate ring needs (4 to 16).'),
       'returnGateBuilt': ('Build Return Gates', 'Arriving in the Tempest Realm builds a return Stormgate if there is none nearby.'),
       'realmFallToOverworld': ('Fall Back to the Overworld', 'Falling off the bottom of the Tempest Realm drops you into the Overworld sky with Slow Falling.'),
       'realmLightning': ('Realm Lightning', 'Lightning strikes the islands of the Tempest Realm.'),
       'realmMobSpawning': ('Realm Mob Spawning', 'Skyrays, Pufflings, Static Wisps, Cyclones and Stormhawks spawn in the Tempest Realm.'),
       'cloudBlocks': ('Cloud Effects', 'Nimbus, Thunder and Gale Clouds slow, launch and push you.'),
       'skyraySpeed': ('Skyray Flight Speed', 'How fast a ridden Skyray flies.')}
for k, (n, t) in CFG.items(): L(f'config.stormreach.{k}', n); L(f'config.stormreach.{k}.tooltip', t)
L('config.stormreach.page.realm', 'Realm')
w(f'{A}/lang/en_us.json', lang)
print('data7 done')
