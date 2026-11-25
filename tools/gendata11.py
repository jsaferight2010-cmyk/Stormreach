"""Stormreach 5.4 (Thunderfront) data: models, blockstates, item definitions, language, loot, recipes, tags, spawns and
advancements for the Thunderfront content. Runs after gendata10.py (it reads and extends the lang)."""
import json
from gendata9 import (w, rj, ns, tag_add, item_def, gen_item, bmodel, el, mob_loot, pool, shaped, add_spawns, cnt, adv, has, CODE, lang, L, A, D, MC)

# ------------------------------------------------------------------------------------------- items
for n in ('omen_feather', 'bottled_dust_devil'):
    gen_item(n)
w(f'{A}/models/item/umbrella.json', {'parent': 'minecraft:item/handheld', 'textures': {'layer0': 'stormreach:item/umbrella'}})
item_def('umbrella', 'item/umbrella')
for m in ('rainfrog', 'storm_petrel', 'dust_devil'):
    gen_item(f'{m}_spawn_egg')

# ------------------------------------------------------------------------------------------- blocks
for charged in (False, True):
    t = {'particle': 'stormreach:block/tesla_coil_base', 'base': 'stormreach:block/tesla_coil_base', 'top': 'stormreach:block/tesla_coil_top',
         'coil': f"stormreach:block/tesla_coil{'_charged' if charged else ''}"}
    bmodel(f"tesla_coil{'_charged' if charged else ''}", {'parent': 'minecraft:block/block', 'textures': t, 'elements': [
        el([2, 0, 2], [14, 3, 14], '#base'), el([4, 3, 4], [12, 4, 12], '#base'),
        el([5, 4, 5], [11, 13, 11], '#coil', shade=not charged),
        el([1, 13, 1], [15, 15.5, 15], '#top'), el([3, 15.5, 3], [13, 16, 13], '#top'), el([6.5, 16, 6.5], [9.5, 17.5, 9.5], '#top')]})
w(f'{A}/blockstates/tesla_coil.json', {'variants': {f'charge={c}': {'model': 'stormreach:block/tesla_coil' + ('_charged' if c else '')} for c in range(6)}})
w(f'{A}/models/item/tesla_coil.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'stormreach:item/tesla_coil'}})
item_def('tesla_coil', 'item/tesla_coil')
CT = {'particle': 'stormreach:block/wind_chimes', 'tube': 'stormreach:block/wind_chimes', 'top': 'stormreach:block/wind_chimes_top', 'string': 'minecraft:block/iron_chain'}
tubes = [el([x, 14 - ln, z], [x + 1, 14, z + 1], '#tube') for x, z, ln in ((4, 7.5, 7), (6.2, 4.6, 10), (9.8, 4.6, 12), (11, 7.5, 9), (7.5, 11, 11))]
bmodel('wind_chimes', {'parent': 'minecraft:block/block', 'textures': CT, 'elements': [
    el([3, 14, 3], [13, 15, 13], '#top'), el([7.5, 15, 7.5], [8.5, 16, 8.5], '#tube'),
    el([7, 7, 7], [9, 9, 9], '#top'), el([7.75, 9, 7.75], [8.25, 14, 8.25], '#tube'), el([6.5, 3, 7.75], [9.5, 6, 8.25], '#top')] + tubes})
w(f'{A}/blockstates/wind_chimes.json', {'variants': {'': {'model': 'stormreach:block/wind_chimes'}}})
w(f'{A}/models/item/wind_chimes.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'stormreach:item/wind_chimes'}})
item_def('wind_chimes', 'item/wind_chimes')

# ------------------------------------------------------------------------------------------- language
names = {
    'item.stormreach.umbrella': 'Umbrella', 'item.stormreach.omen_feather': 'Omen Feather', 'item.stormreach.bottled_dust_devil': 'Bottled Dust Devil',
    'block.stormreach.tesla_coil': 'Tesla Coil', 'block.stormreach.wind_chimes': 'Wind Chimes',
    'entity.stormreach.rainfrog': 'Rainfrog', 'entity.stormreach.storm_petrel': 'Storm Petrel', 'entity.stormreach.dust_devil': 'Dust Devil',
    'entity.stormreach.bottled_dust_devil': 'Bottled Dust Devil',
    'block.stormreach.tesla_coil.reading': 'Tesla Coil: %s%% charged %s', 'block.stormreach.tesla_coil.held': '(held by redstone)',
    'item.stormreach.omen_feather.still': 'The feather lies still. It only knows the Overworld\'s sky.',
    'item.stormreach.omen_feather.rain_ends': 'The rain should ease in about %s min.',
    'item.stormreach.omen_feather.thunder_now': 'The feather bristles: the thunder holds for about %s min more.',
    'item.stormreach.omen_feather.thunder_soon': 'The feather bristles: thunder within about %s min.',
    'item.stormreach.omen_feather.settled': 'The feather lies flat: settled weather for at least %s min.',
    'item.stormreach.omen_feather.rain_coming': 'The feather stirs: rain in about %s min.',
}
for k, v in names.items(): L(k, v)
for m, t in (('rainfrog', 'Rainfrog'), ('storm_petrel', 'Storm Petrel'), ('dust_devil', 'Dust Devil')):
    L(f'item.stormreach.{m}_spawn_egg', f'{t} Spawn Egg')

# ------------------------------------------------------------------------------------------- loot
sample = rj(f'{D}/loot_table/blocks/nacre_block.json')
for b in ('tesla_coil', 'wind_chimes'):
    w(f'{D}/loot_table/blocks/{b}.json', json.loads(json.dumps(sample).replace('nacre_block', b)))
mob_loot('rainfrog', [])
mob_loot('storm_petrel', [pool('minecraft:feather', 0, 2), pool('omen_feather', 1, 1, looting=False, chance=0.25)])
mob_loot('dust_devil', [])
mob_loot('bottled_dust_devil', [])

# ------------------------------------------------------------------------------------------- recipes
shaped('tesla_coil', [' L ', 'CSC', 'III'], {'L': 'minecraft:lightning_rod', 'C': 'minecraft:copper_ingot', 'S': 'storm_shard', 'I': 'minecraft:iron_ingot'},
       'tesla_coil', 1, 'redstone')
shaped('wind_chimes', [' S ', 'WWW', 'C C'], {'S': 'minecraft:string', 'W': '#minecraft:wooden_slabs', 'C': 'minecraft:copper_ingot'}, 'wind_chimes', 1, 'misc')
shaped('umbrella', ['WWW', ' S ', ' S '], {'W': '#minecraft:wool', 'S': 'minecraft:stick'}, 'umbrella', 1, 'equipment')

# ------------------------------------------------------------------------------------------- tags
tag_add(f'{MC}/block/mineable/pickaxe.json', ['stormreach:tesla_coil', 'stormreach:wind_chimes'])

# ------------------------------------------------------------------------------------------- spawns
add_spawns('brinewater_marsh', 'creature', [cnt('stormreach:rainfrog', 2, 4, 10)])
add_spawns('stormreach_coast', 'creature', [cnt('stormreach:rainfrog', 1, 3, 5)])
add_spawns('elderstorm_grove', 'creature', [cnt('stormreach:rainfrog', 1, 3, 4)])

# ------------------------------------------------------------------------------------------- advancements
adv('omen_feather', 'meteorology', 'omen_feather', {'f': has('omen_feather')}, 'Red Sky at Night', 'Get an Omen Feather from a Storm Petrel')
adv('bottled_dust_devil', 'root', 'bottled_dust_devil', CODE, 'Whirlwind in a Bottle', 'Catch a Dust Devil in a glass bottle')
adv('tesla_coil', 'meteorology', 'tesla_coil', {'t': has('tesla_coil')}, 'It\'s Alive', 'Build a Tesla Coil')
adv('umbrella_hail', 'root', 'umbrella', CODE, 'Under the Weather', 'Let an Umbrella take a hailstone for you')

with open(f'{A}/lang/en_us.json', 'w') as f: json.dump(lang, f, indent=2, ensure_ascii=False); f.write('\n')
print('gendata11: Thunderfront data written')
