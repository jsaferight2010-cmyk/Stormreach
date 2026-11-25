"""Stormreach 2.0 JSON: block families cloned from vanilla templates, items, lang, data."""
import os as _os, pathlib as _pl
ROOT = str(_pl.Path(__file__).resolve().parent.parent)  # the Stormreach project folder
VANILLA = _os.environ.get('STORMREACH_VANILLA', str(_pl.Path(ROOT) / '.vanilla'))  # extracted 26.3 jar (assets/ + data/)
MCJAR = _os.environ.get('STORMREACH_MCJAR', str(_pl.Path(ROOT) / '.vanilla' / 'minecraft-26.3.jar'))
import json, os, re, copy, glob, zipfile
R = ROOT + '/src/main/resources'
A = f'{R}/assets/stormreach'; D = f'{R}/data/stormreach'; NS = 'stormreach'
VD = VANILLA + ''; VA = f'{VD}/assets/minecraft'
def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f: json.dump(obj, f, indent=2); f.write('\n')
def ns(x): return x if ':' in x else f'{NS}:{x}'
def rj(p): return json.load(open(p))

def clone_family(vname, oname, texmap, extra_models=()):
    """Copy vanilla blockstate + its models, renaming vname->oname and remapping textures."""
    bs = json.dumps(rj(f'{VA}/blockstates/{vname}.json'))
    itm = json.dumps(rj(f'{VA}/items/{vname}.json'))
    models = set(re.findall(r'"minecraft:block/([a-z0-9_]+)"', bs + itm)) | set(extra_models)
    for m in models:
        if not m.startswith(vname) and m not in extra_models: continue
        md = rj(f'{VA}/models/block/{m}.json')
        s = json.dumps(md)
        for vt, ot in texmap.items(): s = s.replace(f'"minecraft:block/{vt}"', f'"stormreach:block/{ot}"')
        w(f'{A}/models/block/{m.replace(vname, oname, 1)}.json', json.loads(s))
    bs = bs.replace(f'"minecraft:block/{vname}', f'"stormreach:block/{oname}')
    w(f'{A}/blockstates/{oname}.json', json.loads(bs))
    it = json.dumps(rj(f'{VA}/items/{vname}.json')).replace(f'minecraft:block/{vname}', f'stormreach:block/{oname}').replace(f'minecraft:item/{vname}', f'stormreach:item/{oname}')
    w(f'{A}/items/{oname}.json', json.loads(it))
    if 'stormreach:item/' in it:
        im = rj(f'{VA}/models/item/{vname}.json'); s = json.dumps(im)
        s = s.replace(f'minecraft:item/{vname}', f'stormreach:item/{oname}')
        for vt, ot in texmap.items(): s = s.replace(f'"minecraft:block/{vt}"', f'"stormreach:block/{ot}"')
        w(f'{A}/models/item/{oname}.json', json.loads(s))

def cube(name, tex=None):
    w(f'{A}/models/block/{name}.json', {'parent': 'minecraft:block/cube_all', 'textures': {'all': ns('block/' + (tex or name))}})
    w(f'{A}/blockstates/{name}.json', {'variants': {'': {'model': ns('block/' + name)}}})
    w(f'{A}/items/{name}.json', {'model': {'type': 'minecraft:model', 'model': ns('block/' + name)}})

# stone family
for b in ('charged_stormstone', 'cracked_stormstone_bricks', 'chiseled_stormstone', 'fulgurite', 'salt_block'):
    cube(b)
def stone_set(prefix, tex):
    t = {'stone': tex, 'stone_bricks': tex, 'cobblestone': tex}
    clone_family('stone_stairs', f'{prefix}_stairs', {'stone': tex})
    clone_family('stone_slab', f'{prefix}_slab', {'stone': tex})
    # slab double variant should point at full block
    bs = rj(f'{A}/blockstates/{prefix}_slab.json'); bs['variants']['type=double'] = {'model': f'stormreach:block/{tex}'}
    w(f'{A}/blockstates/{prefix}_slab.json', bs)
    clone_family('cobblestone_wall', f'{prefix}_wall', {'cobblestone': tex})
stone_set('stormstone', 'stormstone')
stone_set('polished_stormstone', 'polished_stormstone')
clone_family('cobblestone_wall', 'stormstone_brick_wall', {'cobblestone': 'stormstone_bricks'})

# wood families
def wood_set(o, planks, door_tex):
    m = {'oak_planks': planks, 'oak_door_top': f'{o}_door_top', 'oak_door_bottom': f'{o}_door_bottom', 'oak_trapdoor': f'{o}_trapdoor'}
    for kind in ('fence', 'fence_gate', 'door', 'trapdoor', 'button', 'pressure_plate'):
        clone_family(f'oak_{kind}', f'{o}_{kind}', m)
    # door item icon
    w(f'{A}/models/item/{o}_door.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'stormreach:item/{o}_door'}})
wood_set('driftwood', 'driftwood_planks', 'driftwood')
wood_set('stormpine', 'stormpine_planks', 'stormpine')
cube('stormpine_planks')
for log in ('stormpine_log', 'stripped_stormpine_log'):
    w(f'{A}/models/block/{log}.json', {'parent': 'minecraft:block/cube_column', 'textures': {'end': f'stormreach:block/{log}_top', 'side': f'stormreach:block/{log}'}})
    w(f'{A}/models/block/{log}_horizontal.json', {'parent': 'minecraft:block/cube_column_horizontal', 'textures': {'end': f'stormreach:block/{log}_top', 'side': f'stormreach:block/{log}'}})
    w(f'{A}/blockstates/{log}.json', {'variants': {'axis=x': {'model': f'stormreach:block/{log}_horizontal', 'x': 90, 'y': 90},
        'axis=y': {'model': f'stormreach:block/{log}'}, 'axis=z': {'model': f'stormreach:block/{log}_horizontal', 'x': 90}}})
    w(f'{A}/items/{log}.json', {'model': {'type': 'minecraft:model', 'model': f'stormreach:block/{log}'}})
clone_family('spruce_stairs', 'stormpine_stairs', {'spruce_planks': 'stormpine_planks'})
clone_family('spruce_slab', 'stormpine_slab', {'spruce_planks': 'stormpine_planks'})
bs = rj(f'{A}/blockstates/stormpine_slab.json'); bs['variants']['type=double'] = {'model': 'stormreach:block/stormpine_planks'}; w(f'{A}/blockstates/stormpine_slab.json', bs)
# leaves: no biome tint, cutout from texture
w(f'{A}/models/block/stormpine_leaves.json', {'parent': 'minecraft:block/cube_all', 'textures': {'all': 'stormreach:block/stormpine_leaves'}})
w(f'{A}/blockstates/stormpine_leaves.json', {'variants': {'': {'model': 'stormreach:block/stormpine_leaves'}}})
w(f'{A}/items/stormpine_leaves.json', {'model': {'type': 'minecraft:model', 'model': 'stormreach:block/stormpine_leaves'}})
for cross, itemtex in (('stormpine_sapling', 'block'), ('sea_rocket', 'block'), ('saltgrass', 'block')):
    w(f'{A}/models/block/{cross}.json', {'parent': 'minecraft:block/cross', 'textures': {'cross': f'stormreach:block/{cross}'}})
    w(f'{A}/blockstates/{cross}.json', {'variants': {'': {'model': f'stormreach:block/{cross}'}}})
    w(f'{A}/models/item/{cross}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'stormreach:block/{cross}'}})
    w(f'{A}/items/{cross}.json', {'model': {'type': 'minecraft:model', 'model': f'stormreach:item/{cross}'}})
# glass pane
clone_family('glass_pane', 'stormglass_pane', {'glass': 'stormglass', 'glass_pane_top': 'stormglass_pane_top'})
w(f'{A}/models/item/stormglass_pane.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'stormreach:block/stormglass'}})
# lantern
clone_family('lantern', 'fulgurite_lantern', {'lantern': 'fulgurite_lantern'})
w(f'{A}/models/item/fulgurite_lantern.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'stormreach:item/fulgurite_lantern'}})
# salt crystal (like amethyst cluster, faces all directions)
clone_family('amethyst_cluster', 'salt_crystal', {'amethyst_cluster': 'salt_crystal'})
w(f'{A}/models/item/salt_crystal.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'stormreach:block/salt_crystal'}})
# storm collector (charge 0-4 changes the top)
for c in range(5):
    w(f'{A}/models/block/storm_collector_{c}.json', {'parent': 'minecraft:block/cube_bottom_top', 'textures': {
        'bottom': 'stormreach:block/storm_collector_bottom', 'top': f'stormreach:block/storm_collector_top_{c}', 'side': 'stormreach:block/storm_collector_side'}})
w(f'{A}/blockstates/storm_collector.json', {'variants': {f'charge={c}': {'model': f'stormreach:block/storm_collector_{c}'} for c in range(5)}})
w(f'{A}/items/storm_collector.json', {'model': {'type': 'minecraft:model', 'model': 'stormreach:block/storm_collector_4'}})
# wind vane: post + arrow pointing north, rotated by facing
def el(fr, to, tex='#vane'):
    return {'from': fr, 'to': to, 'faces': {f: {'texture': tex} for f in ('north', 'south', 'east', 'west', 'up', 'down')}}
w(f'{A}/models/block/wind_vane.json', {'parent': 'minecraft:block/block', 'textures': {'particle': 'stormreach:block/wind_vane', 'vane': 'stormreach:block/wind_vane'},
   'elements': [el([7, 0, 7], [9, 12, 9]), el([7.5, 12, 2], [8.5, 13, 14]), el([7.75, 11, 1], [8.25, 14, 4]),
                el([7.75, 10, 11], [8.25, 15, 15]), el([6, 14, 6], [10, 15, 10])]})
w(f'{A}/blockstates/wind_vane.json', {'variants': {f'facing={d}': {'model': 'stormreach:block/wind_vane', 'y': r}
    for d, r in (('north', 0), ('east', 90), ('south', 180), ('west', 270))}})
w(f'{A}/items/wind_vane.json', {'model': {'type': 'minecraft:model', 'model': 'stormreach:block/wind_vane'}})

# plain items
GEN = ['salt', 'fulgurite_shard', 'storm_cell', 'tempest_core', 'gull_egg', 'fried_gull_egg', 'crab_cakes', 'stormberry_jam',
       'stormberry_juice', 'seaweed_salad', 'salted_cod', 'sailors_stew', 'stormfin', 'cooked_stormfin', 'tempest_tonic', 'rock_candy',
       'stormshard_helmet', 'stormshard_chestplate', 'stormshard_leggings', 'stormshard_boots', 'gale_wings', 'squall_in_a_bottle',
       'thunder_crab_spawn_egg', 'harbor_seal_spawn_egg', 'brine_wraith_spawn_egg', 'ball_lightning_spawn_egg', 'stormwarden_spawn_egg']
for i in GEN:
    w(f'{A}/models/item/{i}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'stormreach:item/{i}'}})
    w(f'{A}/items/{i}.json', {'model': {'type': 'minecraft:model', 'model': f'stormreach:item/{i}'}})
w(f'{A}/models/item/stormbreaker.json', {'parent': 'minecraft:item/handheld', 'textures': {'layer0': 'stormreach:item/stormbreaker'}})
w(f'{A}/items/stormbreaker.json', {'model': {'type': 'minecraft:model', 'model': 'stormreach:item/stormbreaker'}})

# equipment assets
w(f'{A}/equipment/stormshard.json', {'layers': {'humanoid': [{'texture': 'stormreach:stormshard'}],
   'humanoid_baby': [{'texture': 'stormreach:stormshard'}], 'humanoid_leggings': [{'texture': 'stormreach:stormshard'}]}})
w(f'{A}/equipment/gale_wings.json', {'layers': {'wings': [{'texture': 'stormreach:gale_wings', 'use_player_texture': True}]}})
print('assets2 done')
