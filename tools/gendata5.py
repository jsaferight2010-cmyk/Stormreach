"""Stormreach 2.1: Stormblight blocks, weather bottles, paintings, Storm Shrine."""
import os as _os, pathlib as _pl
ROOT = str(_pl.Path(__file__).resolve().parent.parent)  # the Stormreach project folder
VANILLA = _os.environ.get('STORMREACH_VANILLA', str(_pl.Path(ROOT) / '.vanilla'))  # extracted 26.3 jar (assets/ + data/)
MCJAR = _os.environ.get('STORMREACH_MCJAR', str(_pl.Path(ROOT) / '.vanilla' / 'minecraft-26.3.jar'))
import json, os
import nbtlib
from nbtlib import tag as T
R = ROOT + '/src/main/resources'
A = f'{R}/assets/stormreach'; D = f'{R}/data/stormreach'; MC = f'{R}/data/minecraft/tags'; C = f'{R}/data/c/tags'
def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f: json.dump(obj, f, indent=2); f.write('\n')
def rj(p): return json.load(open(p))
def tag_add(path, values):
    old = rj(path)['values'] if os.path.exists(path) else []
    w(path, {'values': old + [v for v in values if v not in old]})

# assets
for m in ('stormheart', 'stormheart_active'):
    w(f'{A}/models/block/{m}.json', {'parent': 'minecraft:block/cube_all', 'textures': {'all': f'stormreach:block/{m}'}})
w(f'{A}/blockstates/stormheart.json', {'variants': {f'active={a},stage={s}': {'model': 'stormreach:block/stormheart' + ('_active' if a == 'true' else '')}
    for a in ('true', 'false') for s in range(32)}})
w(f'{A}/items/stormheart.json', {'model': {'type': 'minecraft:model', 'model': 'stormreach:block/stormheart_active'}})
w(f'{A}/models/block/stormscar_turf.json', {'parent': 'minecraft:block/cube_bottom_top', 'textures': {'bottom': 'minecraft:block/dirt',
    'top': 'stormreach:block/stormscar_turf_top', 'side': 'stormreach:block/stormscar_turf_side', 'particle': 'minecraft:block/dirt'}})
w(f'{A}/blockstates/stormscar_turf.json', {'variants': {'': [{'model': 'stormreach:block/stormscar_turf', 'y': y} for y in (0, 90, 180, 270)]}})
w(f'{A}/items/stormscar_turf.json', {'model': {'type': 'minecraft:model', 'model': 'stormreach:block/stormscar_turf'}})
for i in ('bottled_sunshine', 'bottled_rain', 'bottled_thunder'):
    w(f'{A}/models/item/{i}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'stormreach:item/{i}'}})
    w(f'{A}/items/{i}.json', {'model': {'type': 'minecraft:model', 'model': f'stormreach:item/{i}'}})

# loot
w(f'{D}/loot_table/blocks/stormheart.json', {'type': 'minecraft:block', 'pools': [{'condition': {'type': 'minecraft:survives_explosion'},
   'entries': [{'type': 'minecraft:item', 'name': 'stormreach:stormheart'}], 'rolls': 1}], 'random_sequence': 'stormreach:blocks/stormheart'})
w(f'{D}/loot_table/blocks/stormscar_turf.json', {'type': 'minecraft:block', 'pools': [{'entries': [{'type': 'minecraft:alternatives', 'children': [
   {'type': 'minecraft:item', 'condition': 'minecraft:tool/can_silk_touch', 'name': 'stormreach:stormscar_turf'},
   {'type': 'minecraft:item', 'condition': {'type': 'minecraft:survives_explosion'}, 'name': 'minecraft:dirt'}]}], 'rolls': 1}],
   'random_sequence': 'stormreach:blocks/stormscar_turf'})
w(f'{D}/loot_table/entities/storm_front.json', {'type': 'minecraft:entity', 'pools': [], 'random_sequence': 'stormreach:entities/storm_front'})
w(f'{D}/loot_table/chests/storm_shrine.json', {'type': 'minecraft:chest', 'pools': [{'rolls': {'type': 'minecraft:uniform', 'max': 7, 'min': 4}, 'entries': [
    {'type': 'minecraft:item', 'name': f'stormreach:{i}', 'weight': wt, 'modifier': [{'type': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'max': hi, 'min': lo}}]}
    for i, wt, lo, hi in (('storm_shard', 20, 3, 8), ('storm_cell', 10, 1, 2), ('bottled_rain', 8, 1, 2), ('bottled_sunshine', 8, 1, 2),
                          ('bottled_thunder', 4, 1, 1), ('fulgurite_shard', 12, 2, 5), ('tempest_tonic', 6, 1, 2), ('thunderhead_horn', 2, 1, 1))]}],
    'random_sequence': 'stormreach:chests/storm_shrine'})

# recipes
def shaped(name, pattern, key, result, count=1, cat='misc'):
    w(f'{D}/recipe/{name}.json', {'type': 'minecraft:crafting_shaped', 'category': cat, 'key': key, 'pattern': pattern,
       'result': {'count': count, 'id': result if ':' in result else f'stormreach:{result}'}})
shaped('stormheart', ['SCS', 'CBC', 'SCS'], {'S': 'stormreach:storm_shard', 'C': 'stormreach:storm_cell', 'B': 'stormreach:storm_crystal_block'}, 'stormheart')
w(f'{D}/recipe/bottled_sunshine.json', {'type': 'minecraft:crafting_shapeless', 'category': 'misc',
   'ingredients': ['minecraft:glass_bottle', 'minecraft:sunflower', 'stormreach:salt'], 'result': {'count': 1, 'id': 'stormreach:bottled_sunshine'}})
w(f'{D}/recipe/bottled_rain.json', {'type': 'minecraft:crafting_shapeless', 'category': 'misc',
   'ingredients': ['minecraft:glass_bottle', 'minecraft:kelp', 'stormreach:storm_shard'], 'result': {'count': 1, 'id': 'stormreach:bottled_rain'}})
w(f'{D}/recipe/bottled_thunder.json', {'type': 'minecraft:crafting_shapeless', 'category': 'misc',
   'ingredients': ['stormreach:bottled_rain', 'stormreach:storm_cell'], 'result': {'count': 1, 'id': 'stormreach:bottled_thunder'}})

# tags
tag_add(f'{MC}/block/mineable/pickaxe.json', ['stormreach:stormheart'])
tag_add(f'{MC}/block/needs_iron_tool.json', ['stormreach:stormheart'])
tag_add(f'{MC}/block/mineable/shovel.json', ['stormreach:stormscar_turf'])
for t in ('grass_blocks', 'animals_spawnable_on', 'rabbits_spawnable_on', 'wolves_spawnable_on', 'goats_spawnable_on', 'turns_into_dirt_path', 'bats_spawnable_on'):
    tag_add(f'{MC}/block/{t}.json', ['stormreach:stormscar_turf'])
tag_add(f'{C}/block/grass_blocks.json', ['stormreach:stormscar_turf'])

# paintings
for name, wdt, hgt in (('storm_lighthouse', 2, 3), ('squall_at_sea', 4, 2), ('the_stormwarden', 2, 2)):
    w(f'{D}/painting_variant/{name}.json', {'asset_id': f'stormreach:{name}', 'author': {'color': 'gray', 'translate': f'painting.stormreach.{name}.author'},
       'height': hgt, 'title': {'color': 'yellow', 'translate': f'painting.stormreach.{name}.title'}, 'width': wdt})
tag_add(f'{MC}/painting_variant/placeable.json', ['stormreach:storm_lighthouse', 'stormreach:squall_at_sea', 'stormreach:the_stormwarden'])

# Storm Shrine: a ring of charged stormstone pillars around an altar holding a dormant Stormheart
palette = []; pindex = {}
def st(bid, **props):
    key = (bid, tuple(sorted(props.items())))
    if key not in pindex:
        e = {'id': T.String(bid)}
        if props: e['properties'] = T.Compound({k: T.String(v) for k, v in props.items()})
        pindex[key] = len(palette); palette.append(T.Compound(e))
    return pindex[key]
b = {}; chests = {}
S = 11
for x in range(S):
    for z in range(S):
        for y in range(7): b[(x, y, z)] = st('minecraft:air')
        d = ((x - 5) ** 2 + (z - 5) ** 2) ** 0.5
        if d <= 5.3: b[(x, 0, z)] = st('stormreach:polished_stormstone' if d < 3 else 'stormreach:stormstone_bricks')
for (x, z) in ((1, 5), (9, 5), (5, 1), (5, 9), (2, 2), (8, 8), (2, 8), (8, 2)):
    h = 4 if (x == 5 or z == 5) else 3
    for y in range(1, h): b[(x, y, z)] = st('stormreach:stormstone_brick_wall', up='true', north='none', south='none', east='none', west='none', waterlogged='false')
    b[(x, h, z)] = st('stormreach:charged_stormstone')
for (x, z) in ((4, 4), (6, 4), (4, 6), (6, 6)):
    b[(x, 1, z)] = st('stormreach:chiseled_stormstone')
b[(5, 1, 5)] = st('stormreach:polished_stormstone')
b[(5, 2, 5)] = st('stormreach:stormheart', active='false', stage='0')
for (x, z) in ((5, 4), (4, 5), (6, 5), (5, 6)):
    b[(x, 1, z)] = st('stormreach:stormstone_brick_stairs', facing={(5, 4): 'south', (4, 5): 'east', (6, 5): 'west', (5, 6): 'north'}[(x, z)],
                      half='bottom', shape='straight', waterlogged='false')
b[(5, 1, 8)] = st('minecraft:chest', facing='north', type='single', waterlogged='false'); chests[(5, 1, 8)] = 'stormreach:chests/storm_shrine'
b[(8, 1, 5)] = st('stormreach:storm_collector', charge='0')
bl = []
for pos, s in b.items():
    e = {'pos': T.List[T.Int]([T.Int(v) for v in pos]), 'state': T.Int(s)}
    if pos in chests: e['nbt'] = T.Compound({'LootTable': T.String(chests[pos]), 'components': T.Compound({}), 'id': T.String('minecraft:chest')})
    bl.append(T.Compound(e))
root = T.Compound({'size': T.List[T.Int]([T.Int(S), T.Int(7), T.Int(S)]), 'entities': T.List[T.Compound]([]), 'blocks': T.List[T.Compound](bl),
                   'palette': T.List[T.Compound](palette), 'DataVersion': T.Int(5023)})
os.makedirs(f'{D}/structure', exist_ok=True)
nbtlib.File(root, root_name='').save(f'{D}/structure/storm_shrine.nbt', gzipped=True)
WG = f'{D}/worldgen'
w(f'{WG}/template_pool/storm_shrine.json', {'elements': [{'element': {'element_type': 'minecraft:single_pool_element', 'location': 'stormreach:storm_shrine',
    'processors': {'processors': []}, 'projection': 'rigid'}, 'weight': 1}], 'fallback': 'minecraft:empty'})
w(f'{WG}/structure/storm_shrine.json', {'type': 'minecraft:jigsaw', 'biomes': '#stormreach:has_storm_shrine', 'max_distance_from_center': 80,
    'project_start_to_heightmap': 'WORLD_SURFACE_WG', 'size': 1, 'spawn_overrides': {}, 'start_height': {'absolute': 0},
    'start_pool': 'stormreach:storm_shrine', 'step': 'surface_structures', 'terrain_adaptation': 'beard_thin', 'use_expansion_hack': False})
w(f'{WG}/structure_set/storm_shrine.json', {'placement': {'type': 'minecraft:random_spread', 'salt': 161803398, 'separation': 10, 'spacing': 32},
    'structures': [{'structure': 'stormreach:storm_shrine', 'weight': 1}]})
tag_add(f'{D}/tags/worldgen/biome/has_storm_shrine.json', ['stormreach:thunderspire_highlands', 'stormreach:squallwind_bluffs'])

# language
lang = rj(f'{A}/lang/en_us.json')
lang.update({
 'block.stormreach.stormheart': 'Stormheart', 'block.stormreach.stormscar_turf': 'Stormscar Turf',
 'item.stormreach.bottled_sunshine': 'Bottled Sunshine', 'item.stormreach.bottled_rain': 'Bottled Rain', 'item.stormreach.bottled_thunder': 'Bottled Thunder',
 'item.stormreach.weather_bottle.clear': 'The clouds break apart. Clear skies!', 'item.stormreach.weather_bottle.rain': 'Rain sweeps in off the sea...',
 'item.stormreach.weather_bottle.thunder': 'Thunder rolls overhead!',
 'entity.stormreach.storm_front': 'Storm Front',
 'message.stormreach.heart_awake': 'The Stormheart wakes. The Stormblight begins to spread!',
 'message.stormreach.heart_dormant': 'A dormant Stormheart. Wake it with a Storm Cell, lightning or redstone. Calm it with Salt.',
 'message.stormreach.heart_status': 'Stormblight radius: %s / %s blocks',
 'message.stormreach.heart_calmed': 'The Stormheart falls quiet.',
 'message.stormreach.front_released': 'A Storm Front tears loose and rolls across the land!',
 'message.stormreach.blight_off': 'The Stormblight is turned off in the Stormreach settings.',
 'message.stormreach.front_off': 'Storm Fronts are turned off in the Stormreach settings.',
 'painting.stormreach.storm_lighthouse.title': 'Storm Lighthouse', 'painting.stormreach.storm_lighthouse.author': 'Stormreach',
 'painting.stormreach.squall_at_sea.title': 'Squall at Sea', 'painting.stormreach.squall_at_sea.author': 'Stormreach',
 'painting.stormreach.the_stormwarden.title': 'The Stormwarden', 'painting.stormreach.the_stormwarden.author': 'Stormreach',
 'config.stormreach.page.blight': 'Stormblight',
 'config.stormreach.spreadMode': 'Spread Mode',
 'config.stormreach.spreadMode.tooltip': 'Off: nothing spreads. Contained: only an awake Stormheart converts, up to its radius. Unlimited: Stormscar Turf keeps spreading by itself, forever. Careful!',
 'config.stormreach.spreadMode.off': 'Off', 'config.stormreach.spreadMode.contained': 'Contained', 'config.stormreach.spreadMode.unlimited': 'Unlimited',
 'config.stormreach.heartMaxRadius': 'Heart Radius', 'config.stormreach.heartMaxRadius.tooltip': 'How far (in blocks) a Stormheart spreads the Stormblight.',
 'config.stormreach.spreadSpeed': 'Spread Speed', 'config.stormreach.spreadSpeed.tooltip': 'How fast Stormhearts and Storm Fronts convert blocks. Higher costs more performance.',
 'config.stormreach.convertMobs': 'Convert Mobs', 'config.stormreach.convertMobs.tooltip': 'The Stormblight turns mobs into Stormreach mobs (chickens into gulls, zombies into brine wraiths, creepers into ball lightning...). Named mobs and pets are safe.',
 'config.stormreach.blightBiome': 'Change Biome', 'config.stormreach.blightBiome.tooltip': 'A fully grown Stormheart turns its area into Stormreach Coast, so storms come there.',
 'config.stormreach.heartActivatesOnPlace': 'Wake on Place', 'config.stormreach.heartActivatesOnPlace.tooltip': 'Stormhearts start spreading as soon as they are placed.',
 'config.stormreach.stormFront': 'Storm Fronts', 'config.stormreach.stormFront.tooltip': 'Allow releasing a Storm Front by using a Thunderhead Horn on a Stormheart.',
 'config.stormreach.stormFrontMinutes': 'Front Lifetime', 'config.stormreach.stormFrontMinutes.tooltip': 'How long a Storm Front travels before it breaks up.',
})
w(f'{A}/lang/en_us.json', lang)

# advancements
def has_item(i): return {'conditions': {'items': [{'items': f'stormreach:{i}'}]}, 'trigger': 'minecraft:inventory_changed'}
def adv(name, parent, icon, criteria, ttl, desc, frame='task'):
    w(f'{D}/advancement/{name}.json', {'parent': f'stormreach:{parent}', 'criteria': criteria,
       'display': {'title': {'translate': f'advancements.stormreach.{name}.title'}, 'description': {'translate': f'advancements.stormreach.{name}.description'},
                   'icon': {'id': f'stormreach:{icon}'}, 'frame': frame}, 'requirements': [list(criteria)]})
    lang[f'advancements.stormreach.{name}.title'] = ttl; lang[f'advancements.stormreach.{name}.description'] = desc
adv('stormheart', 'storm_cell', 'stormheart', {'h': has_item('stormheart')}, 'Heart of the Storm', 'Obtain a Stormheart')
adv('stormscar', 'stormheart', 'stormscar_turf', {'s': {'conditions': {'player': {'type': 'minecraft:entity_properties', 'entity': 'this',
    'predicate': {'minecraft:stepping_on': {'block': {'blocks': 'stormreach:stormscar_turf'}}}}}, 'trigger': 'minecraft:location'}},
    'Scarred Earth', 'Walk on land the Stormblight has claimed')
adv('weather_maker', 'storm_cell', 'bottled_sunshine', {'s': has_item('bottled_sunshine'), 'r': has_item('bottled_rain')},
    'Weather Maker', 'Bottle both sunshine and rain')
w(f'{A}/lang/en_us.json', lang)
print('data5 done')
