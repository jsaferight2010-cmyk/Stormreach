"""Stormreach 2.0 data: loot, recipes, tags, biomes, trees, structures, enchantments, brewing, lang, advancements."""
import os as _os, pathlib as _pl
ROOT = str(_pl.Path(__file__).resolve().parent.parent)  # the Stormreach project folder
VANILLA = _os.environ.get('STORMREACH_VANILLA', str(_pl.Path(ROOT) / '.vanilla'))  # extracted 26.3 jar (assets/ + data/)
MCJAR = _os.environ.get('STORMREACH_MCJAR', str(_pl.Path(ROOT) / '.vanilla' / 'minecraft-26.3.jar'))
import json, os, copy, glob, gzip, io
import nbtlib
from nbtlib import tag as T
R = ROOT + '/src/main/resources'
A = f'{R}/assets/stormreach'; D = f'{R}/data/stormreach'; NS = 'stormreach'
VD = VANILLA + ''; WGV = f'{VD}/data/minecraft/worldgen'
def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f: json.dump(obj, f, indent=2); f.write('\n')
def ns(x): return x if ':' in x else f'{NS}:{x}'
def rj(p): return json.load(open(p))
def tag_add(path, values):
    old = rj(path)['values'] if os.path.exists(path) else []
    w(path, {'values': old + [v for v in values if v not in old]})
MC = f'{R}/data/minecraft/tags'; C = f'{R}/data/c/tags'

# ---------------- loot ----------------
def self_drop(name):
    w(f'{D}/loot_table/blocks/{name}.json', {'type': 'minecraft:block', 'pools': [{'condition': {'type': 'minecraft:survives_explosion'},
        'entries': [{'type': 'minecraft:item', 'name': ns(name)}], 'rolls': 1}], 'random_sequence': f'stormreach:blocks/{name}'})
SELF = ['stormstone_stairs', 'stormstone_wall', 'charged_stormstone', 'polished_stormstone_stairs', 'polished_stormstone_wall',
        'cracked_stormstone_bricks', 'chiseled_stormstone', 'stormstone_brick_wall', 'driftwood_fence', 'driftwood_fence_gate',
        'driftwood_trapdoor', 'driftwood_button', 'driftwood_pressure_plate', 'stormpine_log', 'stripped_stormpine_log', 'stormpine_planks',
        'stormpine_sapling', 'stormpine_stairs', 'stormpine_fence', 'stormpine_fence_gate', 'stormpine_trapdoor', 'stormpine_button',
        'stormpine_pressure_plate', 'fulgurite_lantern', 'wind_vane', 'storm_collector', 'sea_rocket', 'salt_block']
for b in SELF: self_drop(b)
for slab in ('stormstone_slab', 'polished_stormstone_slab', 'stormpine_slab'):
    w(f'{D}/loot_table/blocks/{slab}.json', {'type': 'minecraft:block', 'pools': [{'entries': [{'type': 'minecraft:item',
      'modifier': [{'type': 'minecraft:set_count', 'condition': {'type': 'minecraft:match_block', 'blocks': ns(slab), 'state': {'type': 'double'}}, 'count': 2},
                   {'type': 'minecraft:explosion_decay'}], 'name': ns(slab)}], 'rolls': 1}], 'random_sequence': f'stormreach:blocks/{slab}'})
for door in ('driftwood_door', 'stormpine_door'):
    v = rj(f'{VD}/data/minecraft/loot_table/blocks/oak_door.json')
    w(f'{D}/loot_table/blocks/{door}.json', json.loads(json.dumps(v).replace('minecraft:oak_door', ns(door)).replace('minecraft:blocks/oak_door', f'stormreach:blocks/{door}')))
w(f'{D}/loot_table/blocks/stormglass_pane.json', {'type': 'minecraft:block', 'pools': [{'condition': 'minecraft:tool/can_silk_touch',
  'entries': [{'type': 'minecraft:item', 'name': 'stormreach:stormglass_pane'}], 'rolls': 1}], 'random_sequence': 'stormreach:blocks/stormglass_pane'})
leaves = json.dumps(rj(f'{VD}/data/minecraft/loot_table/blocks/spruce_leaves.json'))
w(f'{D}/loot_table/blocks/stormpine_leaves.json', json.loads(leaves.replace('minecraft:spruce_leaves', 'stormreach:stormpine_leaves')
   .replace('minecraft:spruce_sapling', 'stormreach:stormpine_sapling').replace('minecraft:blocks/spruce_leaves', 'stormreach:blocks/stormpine_leaves')))
def alt_drop(name, item, lo, hi):
    w(f'{D}/loot_table/blocks/{name}.json', {'type': 'minecraft:block', 'pools': [{'entries': [{'type': 'minecraft:alternatives', 'children': [
      {'type': 'minecraft:item', 'condition': 'minecraft:tool/can_silk_touch', 'name': ns(name)},
      {'type': 'minecraft:item', 'modifier': [{'type': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'max': hi, 'min': lo}},
         {'type': 'minecraft:apply_bonus', 'enchantment': 'minecraft:fortune', 'formula': 'minecraft:uniform_bonus_count', 'parameters': {'bonusMultiplier': 1}},
         {'type': 'minecraft:explosion_decay'}], 'name': ns(item)}]}], 'rolls': 1}], 'random_sequence': f'stormreach:blocks/{name}'})
alt_drop('fulgurite', 'fulgurite_shard', 2, 4)
alt_drop('salt_crystal', 'salt', 1, 3)
w(f'{D}/loot_table/blocks/saltgrass.json', {'type': 'minecraft:block', 'pools': [{'condition': 'minecraft:tool/can_shear_blocks' if False else {'type': 'minecraft:survives_explosion'},
  'entries': [{'type': 'minecraft:item', 'name': 'stormreach:saltgrass'}], 'rolls': 1}], 'random_sequence': 'stormreach:blocks/saltgrass'})

def mob_loot(name, pools):
    w(f'{D}/loot_table/entities/{name}.json', {'type': 'minecraft:entity', 'pools': pools, 'random_sequence': f'stormreach:entities/{name}'})
def pool(item, lo, hi, looting=True, chance=None):
    mods = [{'type': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'max': hi, 'min': lo}}]
    if looting: mods.append({'type': 'minecraft:enchanted_count_increase', 'count': {'type': 'minecraft:uniform', 'max': 1.0, 'min': 0.0}, 'enchantment': 'minecraft:looting'})
    p = {'entries': [{'type': 'minecraft:item', 'modifier': mods, 'name': ns(item)}], 'rolls': 1}
    if chance: p['condition'] = {'type': 'minecraft:killed_by_player'} if chance == 'player' else {'type': 'minecraft:random_chance', 'chance': chance}
    return p
mob_loot('thunder_crab', [pool('crab_claw', 1, 3), pool('storm_shard', 0, 2)])
mob_loot('harbor_seal', [])
mob_loot('brine_wraith', [pool('salt', 0, 2), pool('minecraft:rotten_flesh', 0, 2), pool('storm_shard', 1, 1, False, 0.08)])
mob_loot('ball_lightning', [pool('storm_shard', 0, 1)])
mob_loot('waterspout', [])
mob_loot('stormwarden', [pool('tempest_core', 1, 1, False), pool('storm_crystal_block', 2, 4), pool('storm_cell', 1, 3)])
# lighthouse / skiff chest loot
def chest(name, entries, rolls):
    w(f'{D}/loot_table/chests/{name}.json', {'type': 'minecraft:chest', 'pools': [{'rolls': rolls, 'entries': [
        {'type': 'minecraft:item', 'name': ns(i), 'weight': wt, 'modifier': [{'type': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'max': hi, 'min': lo}}]}
        for i, wt, lo, hi in entries]}], 'random_sequence': f'stormreach:chests/{name}'})
chest('lighthouse', [('storm_shard', 20, 2, 6), ('storm_cell', 6, 1, 2), ('gull_feather', 15, 2, 5), ('salted_cod', 15, 2, 5),
                     ('minecraft:compass', 5, 1, 1), ('minecraft:map', 8, 1, 1), ('tempest_staff', 3, 1, 1), ('squall_in_a_bottle', 10, 1, 3),
                     ('minecraft:candle', 10, 2, 6), ('thunderhead_horn', 1, 1, 1), ('minecraft:emerald', 10, 1, 4)], {'type': 'minecraft:uniform', 'max': 8, 'min': 4})
chest('wrecked_skiff', [('driftwood_planks', 20, 3, 8), ('salt', 15, 2, 6), ('salted_cod', 15, 1, 4), ('minecraft:string', 10, 1, 5),
                        ('minecraft:iron_nugget', 10, 2, 8), ('storm_shard', 8, 1, 3), ('minecraft:gold_ingot', 4, 1, 2), ('rock_candy', 8, 1, 3)],
      {'type': 'minecraft:uniform', 'max': 6, 'min': 3})

# ---------------- recipes ----------------
def shaped(name, pattern, key, result, count=1, cat='misc', group=None):
    r = {'type': 'minecraft:crafting_shaped', 'category': cat, 'key': {k: v if v.startswith('#') else ns(v) for k, v in key.items()},
         'pattern': pattern, 'result': {'count': count, 'id': ns(result)}}
    if group: r['group'] = group
    w(f'{D}/recipe/{name}.json', r)
def shapeless(name, ings, result, count=1, cat='misc'):
    w(f'{D}/recipe/{name}.json', {'type': 'minecraft:crafting_shapeless', 'category': cat,
       'ingredients': [i if i.startswith('#') else ns(i) for i in ings], 'result': {'count': count, 'id': ns(result)}})
def cook(name, ing, result, xp=0.35, food=True):
    kinds = (('smelting', 200, ''), ('smoking', 100, '_from_smoking'), ('campfire_cooking', 600, '_from_campfire_cooking')) if food else (('smelting', 200, ''),)
    for t, time, suffix in kinds:
        w(f'{D}/recipe/{name}{suffix}.json', {'type': f'minecraft:{t}', 'category': 'food' if food else 'blocks', 'cookingtime': time,
           'experience': xp, 'ingredient': ns(ing), 'result': {'id': ns(result)}})
def cut(name, ing, result, count=1):
    w(f'{D}/recipe/{name}.json', {'type': 'minecraft:stonecutting', 'ingredient': ns(ing), 'result': {'count': count, 'id': ns(result)}})
def stairs_slab_wall(base, prefix):
    shaped(f'{prefix}_stairs', ['#  ', '## ', '###'], {'#': base}, f'{prefix}_stairs', 4, 'building')
    shaped(f'{prefix}_slab', ['###'], {'#': base}, f'{prefix}_slab', 6, 'building')
    shaped(f'{prefix}_wall', ['###', '###'], {'#': base}, f'{prefix}_wall', 6, 'misc')
    cut(f'{prefix}_stairs_from_{base}_stonecutting', base, f'{prefix}_stairs'); cut(f'{prefix}_slab_from_{base}_stonecutting', base, f'{prefix}_slab', 2)
    cut(f'{prefix}_wall_from_{base}_stonecutting', base, f'{prefix}_wall')
stairs_slab_wall('stormstone', 'stormstone'); stairs_slab_wall('polished_stormstone', 'polished_stormstone')
shaped('stormstone_brick_wall', ['###', '###'], {'#': 'stormstone_bricks'}, 'stormstone_brick_wall', 6)
cut('stormstone_brick_wall_from_stormstone_bricks_stonecutting', 'stormstone_bricks', 'stormstone_brick_wall')
cook('cracked_stormstone_bricks', 'stormstone_bricks', 'cracked_stormstone_bricks', 0.1, food=False)
shaped('chiseled_stormstone', ['#', '#'], {'#': 'stormstone_brick_slab'}, 'chiseled_stormstone', 1, 'building')
cut('chiseled_stormstone_from_stormstone_stonecutting', 'stormstone', 'chiseled_stormstone')
def wood_recipes(o, planks):
    shaped(f'{o}_fence', ['W#W', 'W#W'], {'W': planks, '#': 'minecraft:stick'}, f'{o}_fence', 3, 'misc', 'wooden_fence')
    shaped(f'{o}_fence_gate', ['#W#', '#W#'], {'W': planks, '#': 'minecraft:stick'}, f'{o}_fence_gate', 1, 'redstone', 'wooden_fence_gate')
    shaped(f'{o}_door', ['##', '##', '##'], {'#': planks}, f'{o}_door', 3, 'redstone', 'wooden_door')
    shaped(f'{o}_trapdoor', ['###', '###'], {'#': planks}, f'{o}_trapdoor', 2, 'redstone', 'wooden_trapdoor')
    shapeless(f'{o}_button', [planks], f'{o}_button', 1, 'redstone')
    shaped(f'{o}_pressure_plate', ['##'], {'#': planks}, f'{o}_pressure_plate', 1, 'redstone')
wood_recipes('driftwood', 'driftwood_planks'); wood_recipes('stormpine', 'stormpine_planks')
shapeless('stormpine_planks', ['#stormreach:stormpine_logs'], 'stormpine_planks', 4, 'building')
shaped('stormpine_stairs', ['#  ', '## ', '###'], {'#': 'stormpine_planks'}, 'stormpine_stairs', 4, 'building', 'wooden_stairs')
shaped('stormpine_slab', ['###'], {'#': 'stormpine_planks'}, 'stormpine_slab', 6, 'building', 'wooden_slab')
shaped('stormglass_pane', ['###', '###'], {'#': 'stormglass'}, 'stormglass_pane', 16, 'misc')
shaped('fulgurite_lantern', ['FFF', 'FSF', 'FFF'], {'F': 'fulgurite_shard', 'S': 'storm_shard'}, 'fulgurite_lantern', 1, 'misc')
shaped('fulgurite', ['##', '##'], {'#': 'fulgurite_shard'}, 'fulgurite', 1, 'building')
shaped('salt_block', ['###', '###', '###'], {'#': 'salt'}, 'salt_block', 1, 'building')
shapeless('salt_from_salt_block', ['salt_block'], 'salt', 9)
shaped('wind_vane', [' C ', ' C ', 'SIS'], {'C': 'minecraft:copper_ingot', 'S': 'storm_shard', 'I': 'minecraft:iron_ingot'}, 'wind_vane', 1, 'redstone')
shaped('storm_collector', ['GSG', 'CRC', 'PPP'], {'G': 'stormglass', 'S': 'storm_crystal_block', 'C': 'minecraft:copper_ingot',
       'R': 'minecraft:lightning_rod', 'P': 'polished_stormstone'}, 'storm_collector', 1, 'redstone')
for piece, pat in (('helmet', ['SSS', 'S S']), ('chestplate', ['S S', 'SSS', 'SSS']), ('leggings', ['SSS', 'S S', 'S S']), ('boots', ['S S', 'S S'])):
    shaped(f'stormshard_{piece}', pat, {'S': 'storm_shard'}, f'stormshard_{piece}', 1, 'equipment')
shaped('gale_wings', ['FSF', 'F F', 'F F'], {'F': 'gull_feather', 'S': 'storm_cell'}, 'gale_wings', 1, 'equipment')
shaped('squall_in_a_bottle', [' F ', 'SBS'], {'F': 'gull_feather', 'S': 'storm_shard', 'B': 'minecraft:glass_bottle'}, 'squall_in_a_bottle', 3, 'misc')
shaped('stormbreaker', ['CTC', ' L ', ' L '], {'C': 'storm_crystal_block', 'T': 'tempest_core', 'L': 'stormpine_log'}, 'stormbreaker', 1, 'equipment')
cook('fried_gull_egg', 'gull_egg', 'fried_gull_egg')
cook('cooked_stormfin', 'stormfin', 'cooked_stormfin')
shapeless('crab_cakes', ['cooked_crab_claw', 'minecraft:bread', 'salt', 'gull_egg'], 'crab_cakes', 2)
shapeless('stormberry_jam', ['stormberries', 'stormberries', 'stormberries', 'minecraft:sugar', 'minecraft:glass_bottle'], 'stormberry_jam')
shapeless('stormberry_juice', ['stormberries', 'stormberries', 'minecraft:glass_bottle'], 'stormberry_juice')
shapeless('seaweed_salad', ['minecraft:kelp', 'minecraft:kelp', 'sea_thrift', 'salt', 'minecraft:bowl'], 'seaweed_salad')
shapeless('salted_cod', ['minecraft:cod', 'salt'], 'salted_cod')
shapeless('sailors_stew', ['salted_cod', 'minecraft:potato', 'minecraft:carrot', 'stormberries', 'minecraft:bowl'], 'sailors_stew')
shapeless('tempest_tonic', ['storm_cell', 'minecraft:honey_bottle', 'stormberries'], 'tempest_tonic')
shapeless('rock_candy', ['salt', 'minecraft:sugar', 'minecraft:stick'], 'rock_candy', 2)
shapeless('purple_dye_from_sea_rocket', ['sea_rocket'], 'minecraft:purple_dye')

# brewing (data-driven in 26.3)
def brew(name, base, reagent, out, bottles=('potion', 'splash_potion', 'lingering_potion')):
    for b in bottles:
        w(f'{D}/recipe/brewing/{b}_{name}.json', {'type': 'minecraft:brewing',
           'input': {'item': f'minecraft:{b}', 'potion_contents': {'potions': base}},
           'output': {'components': {'minecraft:potion_contents': {'potion': out}}, 'id': f'minecraft:{b}'},
           'reagent': {'item': reagent}})
brew('static_charge', 'minecraft:awkward', 'stormreach:storm_shard', 'stormreach:static_charge')
brew('long_static_charge', 'stormreach:static_charge', 'minecraft:redstone', 'stormreach:long_static_charge')
brew('grounded', 'minecraft:awkward', 'stormreach:fulgurite_shard', 'stormreach:grounded')
brew('long_grounded', 'stormreach:grounded', 'minecraft:redstone', 'stormreach:long_grounded')

# ---------------- tags ----------------
tag_add(f'{MC}/block/mineable/pickaxe.json', [ns(b) for b in ('stormstone_stairs', 'stormstone_slab', 'stormstone_wall', 'charged_stormstone',
    'polished_stormstone_stairs', 'polished_stormstone_slab', 'polished_stormstone_wall', 'cracked_stormstone_bricks', 'chiseled_stormstone',
    'stormstone_brick_wall', 'fulgurite_lantern', 'wind_vane', 'storm_collector', 'salt_block', 'salt_crystal')])
tag_add(f'{MC}/block/mineable/shovel.json', ['stormreach:fulgurite'])
tag_add(f'{MC}/block/mineable/axe.json', [ns(b) for b in ('driftwood_fence', 'driftwood_fence_gate', 'driftwood_door', 'driftwood_trapdoor',
    'driftwood_button', 'driftwood_pressure_plate', 'stormpine_log', 'stripped_stormpine_log', 'stormpine_planks', 'stormpine_stairs',
    'stormpine_slab', 'stormpine_fence', 'stormpine_fence_gate', 'stormpine_door', 'stormpine_trapdoor', 'stormpine_button', 'stormpine_pressure_plate')])
tag_add(f'{MC}/block/mineable/hoe.json', ['stormreach:stormpine_leaves'])
tag_add(f'{MC}/block/needs_stone_tool.json', ['stormreach:storm_collector', 'stormreach:wind_vane'])
tag_add(f'{D}/tags/block/stormpine_logs.json', ['stormreach:stormpine_log', 'stormreach:stripped_stormpine_log'])
tag_add(f'{D}/tags/item/stormpine_logs.json', ['stormreach:stormpine_log', 'stormreach:stripped_stormpine_log'])
for kind in ('block', 'item'):
    tag_add(f'{MC}/{kind}/logs.json', ['#stormreach:stormpine_logs']); tag_add(f'{MC}/{kind}/logs_that_burn.json', ['#stormreach:stormpine_logs'])
    tag_add(f'{MC}/{kind}/planks.json', ['stormreach:stormpine_planks'])
    tag_add(f'{MC}/{kind}/leaves.json', ['stormreach:stormpine_leaves'])
    tag_add(f'{MC}/{kind}/saplings.json', ['stormreach:stormpine_sapling'])
    tag_add(f'{MC}/{kind}/wooden_stairs.json', ['stormreach:stormpine_stairs']); tag_add(f'{MC}/{kind}/wooden_slabs.json', ['stormreach:stormpine_slab'])
    tag_add(f'{MC}/{kind}/wooden_fences.json', ['stormreach:driftwood_fence', 'stormreach:stormpine_fence'])
    tag_add(f'{MC}/{kind}/fence_gates.json', ['stormreach:driftwood_fence_gate', 'stormreach:stormpine_fence_gate'])
    tag_add(f'{MC}/{kind}/wooden_doors.json', ['stormreach:driftwood_door', 'stormreach:stormpine_door'])
    tag_add(f'{MC}/{kind}/wooden_trapdoors.json', ['stormreach:driftwood_trapdoor', 'stormreach:stormpine_trapdoor'])
    tag_add(f'{MC}/{kind}/wooden_buttons.json', ['stormreach:driftwood_button', 'stormreach:stormpine_button'])
    tag_add(f'{MC}/{kind}/wooden_pressure_plates.json', ['stormreach:driftwood_pressure_plate', 'stormreach:stormpine_pressure_plate'])
    tag_add(f'{MC}/{kind}/walls.json', ['stormreach:stormstone_wall', 'stormreach:polished_stormstone_wall', 'stormreach:stormstone_brick_wall'])
    tag_add(f'{MC}/{kind}/stairs.json', ['stormreach:stormstone_stairs', 'stormreach:polished_stormstone_stairs', 'stormreach:stormpine_stairs'])
    tag_add(f'{MC}/{kind}/slabs.json', ['stormreach:stormstone_slab', 'stormreach:polished_stormstone_slab', 'stormreach:stormpine_slab'])
    tag_add(f'{MC}/{kind}/small_flowers.json', ['stormreach:sea_rocket', 'stormreach:saltgrass'])
tag_add(f'{MC}/block/fences.json', ['stormreach:driftwood_fence', 'stormreach:stormpine_fence'])
tag_add(f'{MC}/block/overworld_natural_logs.json', ['stormreach:stormpine_log'])
tag_add(f'{MC}/item/head_armor.json', ['stormreach:stormshard_helmet']); tag_add(f'{MC}/item/chest_armor.json', ['stormreach:stormshard_chestplate'])
tag_add(f'{MC}/item/leg_armor.json', ['stormreach:stormshard_leggings']); tag_add(f'{MC}/item/foot_armor.json', ['stormreach:stormshard_boots'])
tag_add(f'{MC}/item/swords.json', ['stormreach:stormbreaker'])
tag_add(f'{MC}/item/fishes.json', ['stormreach:stormfin', 'stormreach:cooked_stormfin'])
tag_add(f'{MC}/item/egg.json', [])  # placeholder kept empty
os.remove(f'{MC}/item/egg.json')
tag_add(f'{C}/item/foods.json', [ns(i) for i in ('fried_gull_egg', 'crab_cakes', 'stormberry_jam', 'stormberry_juice', 'seaweed_salad', 'salted_cod',
    'sailors_stew', 'stormfin', 'cooked_stormfin', 'tempest_tonic', 'rock_candy')])
tag_add(f'{C}/item/foods/raw_fish.json', ['stormreach:stormfin']); tag_add(f'{C}/item/foods/cooked_fish.json', ['stormreach:cooked_stormfin', 'stormreach:salted_cod'])
tag_add(f'{C}/item/foods/soup.json', ['stormreach:sailors_stew', 'stormreach:seaweed_salad'])
tag_add(f'{C}/item/foods/candy.json', ['stormreach:rock_candy']); tag_add(f'{C}/item/eggs.json', ['stormreach:gull_egg'])
tag_add(f'{C}/item/dusts.json', ['stormreach:salt'])
tag_add(f'{C}/block/glass_panes.json', ['stormreach:stormglass_pane']); tag_add(f'{C}/item/glass_panes.json', ['stormreach:stormglass_pane'])
tag_add(f'{C}/item/armors.json', [ns(f'stormshard_{p}') for p in ('helmet', 'chestplate', 'leggings', 'boots')])
tag_add(f'{C}/item/melee_weapon_tools.json', ['stormreach:stormbreaker', 'stormreach:stormshard_sword'])
tag_add(f'{C}/block/storage_blocks.json', ['stormreach:salt_block']); tag_add(f'{C}/item/storage_blocks.json', ['stormreach:salt_block'])
# armor enchantability
for t, items in (('armor', ['helmet', 'chestplate', 'leggings', 'boots']), ('head_armor', ['helmet']), ('chest_armor', ['chestplate']),
                 ('leg_armor', ['leggings']), ('foot_armor', ['boots'])):
    tag_add(f'{MC}/item/enchantable/{t}.json', [ns(f'stormshard_{i}') for i in items])
tag_add(f'{MC}/item/enchantable/durability.json', [ns(f'stormshard_{i}') for i in ('helmet', 'chestplate', 'leggings', 'boots')] + ['stormreach:gale_wings', 'stormreach:stormbreaker'])
tag_add(f'{MC}/item/enchantable/equippable.json', ['stormreach:gale_wings'])
tag_add(f'{MC}/item/trimmable_armor.json', [ns(f'stormshard_{i}') for i in ('helmet', 'chestplate', 'leggings', 'boots')])
# biomes
NEW_BIOMES = ['squallwind_bluffs', 'thunderspire_highlands', 'brinewater_marsh']
tag_add(f'{MC}/worldgen/biome/is_overworld.json', [ns(b) for b in NEW_BIOMES])
tag_add(f'{MC}/worldgen/biome/is_beach.json', ['stormreach:squallwind_bluffs'])
tag_add(f'{MC}/worldgen/biome/is_hill.json', ['stormreach:thunderspire_highlands'])
tag_add(f'{MC}/worldgen/biome/is_mountain.json', ['stormreach:thunderspire_highlands'])
tag_add(f'{MC}/worldgen/biome/has_structure/mineshaft.json', [ns(b) for b in NEW_BIOMES])
tag_add(f'{MC}/worldgen/biome/has_structure/swamp_hut.json', ['stormreach:brinewater_marsh'])
tag_add(f'{MC}/worldgen/biome/has_structure/ruined_portal_mountain.json', ['stormreach:thunderspire_highlands'])
tag_add(f'{MC}/worldgen/biome/has_structure/ruined_portal_swamp.json', ['stormreach:brinewater_marsh'])
tag_add(f'{C}/worldgen/biome/is_overworld.json', [ns(b) for b in NEW_BIOMES])
tag_add(f'{C}/worldgen/biome/is_swamp.json', ['stormreach:brinewater_marsh']); tag_add(f'{C}/worldgen/biome/is_wet.json', ['stormreach:brinewater_marsh', 'stormreach:stormreach_coast'])
tag_add(f'{C}/worldgen/biome/is_mountain.json', ['stormreach:thunderspire_highlands']); tag_add(f'{C}/worldgen/biome/is_windswept.json', ['stormreach:thunderspire_highlands', 'stormreach:squallwind_bluffs'])
tag_add(f'{C}/worldgen/biome/is_stony_shores.json', ['stormreach:squallwind_bluffs']); tag_add(f'{C}/worldgen/biome/is_beach.json', ['stormreach:squallwind_bluffs'])
tag_add(f'{D}/tags/worldgen/biome/has_lighthouse.json', ['stormreach:stormreach_coast', 'stormreach:squallwind_bluffs'])
tag_add(f'{D}/tags/worldgen/biome/has_wrecked_skiff.json', ['stormreach:stormreach_coast', 'stormreach:brinewater_marsh', 'minecraft:beach'])
# damage type
w(f'{D}/damage_type/hail.json', {'effects': 'freezing', 'exhaustion': 0.1, 'message_id': 'stormreach.hail', 'scaling': 'never'})
tag_add(f'{MC}/damage_type/damages_helmet.json', ['stormreach:hail']); tag_add(f'{MC}/damage_type/no_knockback.json', ['stormreach:hail'])
tag_add(f'{MC}/damage_type/is_freezing.json', [])
os.remove(f'{MC}/damage_type/is_freezing.json')

# ---------------- enchantments ----------------
E = f'{D}/enchantment'
w(f'{E}/conductive.json', {'anvil_cost': 4, 'description': {'translate': 'enchantment.stormreach.conductive'},
  'effects': {'minecraft:post_attack': [{'affected': 'victim', 'enchanted': 'attacker',
     'effect': {'type': 'minecraft:all_of', 'effects': [
        {'type': 'minecraft:damage_entity', 'damage_type': 'minecraft:lightning_bolt', 'min_damage': {'type': 'minecraft:linear', 'base': 2.0, 'per_level_above_first': 1.5},
         'max_damage': {'type': 'minecraft:linear', 'base': 4.0, 'per_level_above_first': 2.0}},
        {'type': 'minecraft:spawn_particles', 'horizontal_position': {'type': 'in_bounding_box'}, 'horizontal_velocity': {'base': 0.0},
         'particle': {'type': 'minecraft:electric_spark'}, 'speed': 1.0, 'vertical_position': {'type': 'in_bounding_box'}, 'vertical_velocity': {'base': 0.2}},
        {'type': 'minecraft:play_sound', 'pitch': 1.6, 'sound': 'minecraft:entity.lightning_bolt.impact', 'volume': 0.6}]},
     'requirements': {'type': 'minecraft:any_of', 'terms': [{'type': 'minecraft:weather_check', 'raining': True},
        {'type': 'minecraft:random_chance', 'chance': {'type': 'minecraft:enchantment_level', 'amount': {'type': 'minecraft:linear', 'base': 0.2, 'per_level_above_first': 0.1}}}]}}]},
  'max_cost': {'base': 50, 'per_level_above_first': 10}, 'max_level': 3, 'min_cost': {'base': 15, 'per_level_above_first': 10},
  'slots': ['mainhand'], 'supported_items': '#minecraft:enchantable/sharp_weapon', 'weight': 2})
w(f'{E}/galeforce.json', {'anvil_cost': 4, 'description': {'translate': 'enchantment.stormreach.galeforce'},
  'effects': {'minecraft:location_changed': [{'effect': {'type': 'minecraft:attribute', 'amount': {'type': 'minecraft:linear', 'base': 0.02, 'per_level_above_first': 0.015},
     'attribute': 'minecraft:movement_speed', 'id': 'stormreach:enchantment.galeforce', 'operation': 'add_value'},
     'requirements': {'type': 'minecraft:all_of', 'terms': [{'type': 'minecraft:weather_check', 'raining': True},
        {'type': 'minecraft:entity_properties', 'entity': 'this', 'predicate': {'minecraft:location': {'can_see_sky': True}}}]}}]},
  'max_cost': {'base': 40, 'per_level_above_first': 10}, 'max_level': 3, 'min_cost': {'base': 10, 'per_level_above_first': 10},
  'slots': ['feet'], 'supported_items': '#minecraft:enchantable/foot_armor', 'weight': 2})
w(f'{E}/grounded.json', {'anvil_cost': 2, 'description': {'translate': 'enchantment.stormreach.grounded'},
  'effects': {'minecraft:damage_protection': [{'effect': {'type': 'minecraft:add', 'value': {'type': 'minecraft:linear', 'base': 4.0, 'per_level_above_first': 4.0}},
     'requirements': {'type': 'minecraft:damage_source_properties', 'predicate': {'tags': [{'expected': True, 'id': '#minecraft:is_lightning'},
        {'expected': False, 'id': '#minecraft:bypasses_invulnerability'}]}}}]},
  'max_cost': {'base': 30, 'per_level_above_first': 10}, 'max_level': 3, 'min_cost': {'base': 8, 'per_level_above_first': 8},
  'slots': ['armor'], 'supported_items': '#minecraft:enchantable/armor', 'weight': 4})
for t in ('non_treasure', 'tradeable', 'on_random_loot', 'on_traded_equipment'):
    tag_add(f'{MC}/enchantment/{t}.json', ['stormreach:conductive', 'stormreach:galeforce', 'stormreach:grounded'])

# ---------------- trees and new worldgen ----------------
WG = f'{D}/worldgen'
pine = rj(f'{WGV}/feature/pine.json')
pine['trunk_provider'] = {'id': 'stormreach:stormpine_log', 'properties': {'axis': 'y'}}
pine['foliage_provider'] = {'id': 'stormreach:stormpine_leaves', 'properties': {'distance': '7', 'persistent': 'false', 'waterlogged': 'false'}}
pine['trunk_placer']['base_height'] = 7
w(f'{WG}/feature/stormpine.json', pine)
w(f'{WG}/placed_feature/stormpine_checked.json', {'feature': 'stormreach:stormpine', 'placement': [
   {'type': 'minecraft:block_predicate_filter', 'predicate': {'type': 'minecraft:would_survive', 'state': 'stormreach:stormpine_sapling'}}]})
# Placed features nested inside a selector must never carry a minecraft:biome filter: the biome check needs the
# top-level placed feature, which a nested one is not, and Minecraft throws "Tried to biome check an unregistered
# feature". The coastal fallen_driftwood keeps its biome filter; the tree selector gets its own filter-free variant.
w(f'{WG}/placed_feature/fallen_driftwood_checked.json', {'feature': 'stormreach:fallen_driftwood', 'placement': [
   {'type': 'minecraft:block_predicate_filter', 'predicate': {'type': 'minecraft:would_survive', 'state': 'minecraft:oak_sapling'}}]})
w(f'{WG}/feature/trees_thunderspire.json', {'type': 'minecraft:random_selector', 'default': 'stormreach:stormpine_checked',
   'features': [{'chance': 0.05, 'feature': 'stormreach:fallen_driftwood_checked'}]})
w(f'{WG}/placed_feature/trees_thunderspire.json', {'feature': 'stormreach:trees_thunderspire', 'placement': [
   {'type': 'minecraft:count', 'count': {'type': 'minecraft:weighted_list', 'distribution': [{'data': 1, 'weight': 6}, {'data': 3, 'weight': 3}, {'data': 0, 'weight': 2}]}},
   {'type': 'minecraft:in_square'}, {'type': 'minecraft:surface_water_depth_filter', 'max_water_depth': 0},
   {'type': 'minecraft:heightmap', 'heightmap': 'OCEAN_FLOOR'}, {'type': 'minecraft:biome'}]})
w(f'{WG}/placed_feature/trees_marsh.json', {'feature': 'stormreach:trees_thunderspire', 'placement': [
   {'type': 'minecraft:rarity_filter', 'chance': 4}, {'type': 'minecraft:in_square'}, {'type': 'minecraft:surface_water_depth_filter', 'max_water_depth': 0},
   {'type': 'minecraft:heightmap', 'heightmap': 'OCEAN_FLOOR'}, {'type': 'minecraft:biome'}]})
w(f'{WG}/feature/disk_salt.json', {'type': 'minecraft:disk', 'half_height': 1, 'radius': {'type': 'minecraft:uniform', 'max_inclusive': 5, 'min_inclusive': 2},
   'state_provider': {'id': 'stormreach:salt_block'}, 'target': {'type': 'minecraft:matching_blocks', 'blocks': ['minecraft:dirt', 'minecraft:grass_block', 'minecraft:sand', 'minecraft:clay', 'minecraft:mud']}})
w(f'{WG}/placed_feature/disk_salt.json', {'feature': 'stormreach:disk_salt', 'placement': [
   {'type': 'minecraft:count', 'count': 3}, {'type': 'minecraft:in_square'}, {'type': 'minecraft:heightmap', 'heightmap': 'WORLD_SURFACE_WG'},
   {'type': 'minecraft:biome'}]})
def patch(name, state, chance, count, spread):
    w(f'{WG}/feature/{name}.json', {'type': 'minecraft:simple_block', 'to_place': state})
    w(f'{WG}/placed_feature/{name}.json', {'feature': f'stormreach:{name}', 'placement': [
      {'type': 'minecraft:rarity_filter', 'chance': chance}, {'type': 'minecraft:in_square'}, {'type': 'minecraft:heightmap', 'heightmap': 'MOTION_BLOCKING'},
      {'type': 'minecraft:biome'}, {'type': 'minecraft:count', 'count': count},
      {'type': 'minecraft:offset', 'x': {'type': 'minecraft:trapezoid', 'max': spread, 'min': -spread, 'plateau': 0},
       'y': {'type': 'minecraft:trapezoid', 'max': 3, 'min': -3, 'plateau': 0}, 'z': {'type': 'minecraft:trapezoid', 'max': spread, 'min': -spread, 'plateau': 0}},
      {'type': 'minecraft:block_predicate_filter', 'predicate': {'type': 'minecraft:all_of', 'predicates': [
         {'type': 'minecraft:matching_block_tag', 'tag': 'minecraft:air'}, {'type': 'minecraft:would_survive', 'state': state}]}}]})
patch('patch_sea_rocket', {'id': 'stormreach:sea_rocket'}, 2, 32, 7)
patch('patch_saltgrass', {'id': 'stormreach:saltgrass'}, 1, 48, 7)
w(f'{WG}/feature/salt_crystals.json', {'type': 'minecraft:simple_block', 'to_place': {'id': 'stormreach:salt_crystal', 'properties': {'facing': 'up', 'waterlogged': 'false'}}})
w(f'{WG}/placed_feature/salt_crystals.json', {'feature': 'stormreach:salt_crystals', 'placement': [
   {'type': 'minecraft:count', 'count': 12}, {'type': 'minecraft:in_square'}, {'type': 'minecraft:heightmap', 'heightmap': 'MOTION_BLOCKING'}, {'type': 'minecraft:biome'},
   {'type': 'minecraft:block_predicate_filter', 'predicate': {'type': 'minecraft:all_of', 'predicates': [
      {'type': 'minecraft:matching_block_tag', 'tag': 'minecraft:air'},
      {'type': 'minecraft:matching_blocks', 'blocks': 'stormreach:salt_block', 'offset': [0, -1, 0]}]}}]})
w(f'{WG}/feature/charged_spire.json', {'type': 'minecraft:block_column', 'allowed_placement': {'type': 'minecraft:matching_block_tag', 'tag': 'minecraft:air'},
   'direction': 'up', 'layers': [{'height': {'type': 'minecraft:uniform', 'max_inclusive': 5, 'min_inclusive': 2}, 'provider': {'id': 'stormreach:stormstone'}},
                                 {'height': 1, 'provider': {'id': 'stormreach:charged_stormstone'}}], 'prioritize_tip': True})
w(f'{WG}/placed_feature/charged_spire.json', {'feature': 'stormreach:charged_spire', 'placement': [
   {'type': 'minecraft:rarity_filter', 'chance': 3}, {'type': 'minecraft:in_square'}, {'type': 'minecraft:heightmap', 'heightmap': 'MOTION_BLOCKING'},
   {'type': 'minecraft:biome'}, {'type': 'minecraft:block_predicate_filter', 'predicate': {'type': 'minecraft:matching_block_tag', 'tag': 'minecraft:base_stone_overworld', 'offset': [0, -1, 0]}}]})
w(f'{WG}/placed_feature/ore_storm_crystal_rich.json', {'feature': 'stormreach:ore_storm_crystal', 'placement': [
  {'type': 'minecraft:count', 'count': 16}, {'type': 'minecraft:in_square'},
  {'type': 'minecraft:height_range', 'height': {'type': 'minecraft:trapezoid', 'max_inclusive': {'absolute': 200}, 'min_inclusive': {'absolute': 40}}},
  {'type': 'minecraft:biome'}]})

def make_biome(name, base, sky, fog, water, water_fog, grass, foliage, temp, downfall, add_feats, creatures, monsters_extra, extra_attrs=None, replace_veg=None):
    b = rj(f'{WGV}/biome/{base}.json')
    at = b['attributes']; sp = at['minecraft:gameplay/natural_mob_spawns']['argument']['spawns_by_category']
    at['minecraft:visual/sky_color'] = sky; at['minecraft:visual/fog_color'] = fog; at['minecraft:visual/water_fog_color'] = water_fog
    at.pop('minecraft:audio/background_music', None)
    if extra_attrs: at.update(extra_attrs)
    sp['creature'] = creatures
    sp['monster'] = sp.get('monster', []) + monsters_extra
    eff = {'water_color': water, 'grass_color': grass, 'foliage_color': foliage}
    b['effects'] = eff
    b['temperature'] = temp; b['downfall'] = downfall; b['has_precipitation'] = True
    f = b['features']
    if replace_veg is not None: f[9] = replace_veg
    for step, feats in add_feats.items(): f[step] = f[step] + [x for x in feats if x not in f[step]]
    w(f'{WG}/biome/{name}.json', b)
def cnt(t, lo, hi, wt): return {'type': t, 'count': {'type': 'minecraft:uniform', 'max_inclusive': hi, 'min_inclusive': lo}, 'weight': wt}
make_biome('squallwind_bluffs', 'stony_shore', '#5f6f88', '#8d9aab', '#33557d', '#1b2f47', '#6a8a63', '#587a52', 0.3, 0.9,
    {2: ['stormreach:stormstone_boulder'], 6: ['stormreach:ore_stormstone', 'stormreach:ore_storm_crystal'], 9: ['stormreach:patch_sea_rocket', 'stormreach:patch_sea_thrift']},
    [cnt('stormreach:storm_gull', 2, 4, 12), cnt('stormreach:harbor_seal', 1, 3, 8), cnt('stormreach:cliff_crab', 2, 3, 6)],
    [cnt('stormreach:brine_wraith', 1, 2, 25)])
make_biome('thunderspire_highlands', 'windswept_gravelly_hills', '#4f5d75', '#7d889a', '#2f4d73', '#17283d', '#5f7d6a', '#4a6b5a', 0.2, 0.8,
    {2: ['stormreach:stormstone_boulder'], 6: ['stormreach:ore_stormstone', 'stormreach:ore_storm_crystal_rich'], 9: ['stormreach:charged_spire']},
    [cnt('stormreach:storm_gull', 2, 3, 10), cnt('minecraft:goat', 1, 3, 5), cnt('minecraft:sheep', 2, 3, 6)], [],
    extra_attrs={'minecraft:visual/ambient_particles': {'argument': [{'particle': {'type': 'minecraft:white_ash'}, 'probability': 0.004}], 'modifier': 'append'}},
    replace_veg=['minecraft:glow_lichen', 'stormreach:trees_thunderspire', 'minecraft:patch_grass_badlands', 'stormreach:charged_spire',
                 'minecraft:brown_mushroom_normal', 'minecraft:red_mushroom_normal'])
make_biome('brinewater_marsh', 'swamp', '#6b7a7e', '#a4aeae', '#4d6b66', '#22332f', '#7f9363', '#6d7f52', 0.6, 0.95,
    {6: ['stormreach:disk_salt']},
    [cnt('stormreach:harbor_seal', 1, 3, 8), cnt('stormreach:cliff_crab', 2, 4, 10), cnt('minecraft:frog', 2, 5, 8)],
    [cnt('stormreach:brine_wraith', 1, 3, 60)],
    replace_veg=['minecraft:glow_lichen', 'stormreach:trees_marsh', 'stormreach:fallen_driftwood', 'minecraft:patch_grass_normal',
                 'stormreach:patch_saltgrass', 'stormreach:salt_crystals', 'minecraft:patch_waterlily', 'minecraft:brown_mushroom_swamp',
                 'minecraft:red_mushroom_swamp', 'minecraft:brown_mushroom_normal', 'minecraft:red_mushroom_normal',
                 'minecraft:patch_sugar_cane_swamp', 'minecraft:seagrass_swamp'])
# Brine wraiths also haunt the Stormreach Coast
coast = rj(f'{WG}/biome/stormreach_coast.json')
mons = coast['attributes']['minecraft:gameplay/natural_mob_spawns']['argument']['spawns_by_category']['monster']
if not any(m['type'] == 'stormreach:brine_wraith' for m in mons): mons.append(cnt('stormreach:brine_wraith', 1, 2, 20))
coast['attributes']['minecraft:gameplay/natural_mob_spawns']['argument']['spawns_by_category']['creature'].append(cnt('stormreach:harbor_seal', 1, 2, 4)) \
    if not any(m['type'] == 'stormreach:harbor_seal' for m in coast['attributes']['minecraft:gameplay/natural_mob_spawns']['argument']['spawns_by_category']['creature']) else None
w(f'{WG}/biome/stormreach_coast.json', coast)

# ---------------- structures (NBT templates built block by block) ----------------
palette = []; pindex = {}
def st(bid, **props):
    key = (bid, tuple(sorted(props.items())))
    if key not in pindex:
        e = {'id': T.String(bid)}
        if props: e['properties'] = T.Compound({k: T.String(v) for k, v in props.items()})
        pindex[key] = len(palette); palette.append(T.Compound(e))
    return pindex[key]
def write_structure(name, blocks, size, chests):
    global palette, pindex
    bl = []
    for (x, y, z), s in blocks.items():
        entry = {'pos': T.List[T.Int]([T.Int(x), T.Int(y), T.Int(z)]), 'state': T.Int(s)}
        if (x, y, z) in chests:
            entry['nbt'] = T.Compound({'LootTable': T.String(chests[(x, y, z)]), 'components': T.Compound({}), 'id': T.String('minecraft:chest')})
        bl.append(T.Compound(entry))
    root = T.Compound({'size': T.List[T.Int]([T.Int(v) for v in size]), 'entities': T.List[T.Compound]([]),
                       'blocks': T.List[T.Compound](bl), 'palette': T.List[T.Compound](palette), 'DataVersion': T.Int(5023)})
    path = f'{D}/structure/{name}.nbt'; os.makedirs(os.path.dirname(path), exist_ok=True)
    nbtlib.File(root, root_name='').save(path, gzipped=True)
    palette = []; pindex = {}

# Storm Lighthouse: 7x7 stormstone-brick tower, 18 tall, lantern room on top
b = {}; chests = {}
AIR = None
for y in range(0, 18):
    for x in range(7):
        for z in range(7):
            edge = x in (0, 6) or z in (0, 6)
            corner = x in (0, 6) and z in (0, 6)
            if y == 0:
                b[(x, y, z)] = st('stormreach:polished_stormstone')
            elif y < 13:
                if edge and not corner:
                    if (y % 4 == 2) and (x == 3 or z == 3): b[(x, y, z)] = st('stormreach:stormglass_pane')
                    elif y in (1, 2) and x == 3 and z == 0: b[(x, y, z)] = st('minecraft:air')
                    else: b[(x, y, z)] = st('stormreach:stormstone_bricks' if (y + x + z) % 5 else 'stormreach:cracked_stormstone_bricks')
                elif corner: b[(x, y, z)] = st('stormreach:chiseled_stormstone')
                else: b[(x, y, z)] = st('minecraft:air')
            elif y == 13:
                b[(x, y, z)] = st('stormreach:polished_stormstone')
            elif y in (14, 15, 16):
                if corner: b[(x, y, z)] = st('stormreach:stormstone_brick_wall', up='true', north='none', south='none', east='none', west='none', waterlogged='false')
                elif x in (1, 5) and z in (1, 5) or (edge): b[(x, y, z)] = st('stormreach:stormglass') if y == 15 and not corner else st('minecraft:air')
                else: b[(x, y, z)] = st('minecraft:air')
            elif y == 17:
                b[(x, y, z)] = st('stormreach:stormstone_brick_slab', type='bottom', waterlogged='false')
# spiral ladder-ish stairs: simple ladder on inside wall
for y in range(1, 13):
    b[(3, y, 5)] = st('minecraft:ladder', facing='north', waterlogged='false')
b[(3, 13, 5)] = st('minecraft:air')
b[(3, 14, 3)] = st('stormreach:storm_lantern'); b[(3, 15, 3)] = st('stormreach:storm_crystal_block'); b[(3, 16, 3)] = st('stormreach:storm_lantern')
b[(1, 1, 1)] = st('minecraft:chest', facing='south', type='single', waterlogged='false'); chests[(1, 1, 1)] = 'stormreach:chests/lighthouse'
b[(5, 14, 5)] = st('minecraft:chest', facing='west', type='single', waterlogged='false'); chests[(5, 14, 5)] = 'stormreach:chests/lighthouse'
b[(5, 1, 1)] = st('stormreach:storm_barometer', power='0')
b[(3, 18 - 1, 3)] = st('stormreach:wind_vane', facing='north') if False else b[(3, 17, 3)]
write_structure('lighthouse', b, (7, 18, 7), chests)

# Wrecked Skiff: a broken driftwood rowboat half-buried in sand, with a chest
b = {}; chests = {}
for x in range(9):
    for z in range(5):
        for y in range(3):
            b[(x, y, z)] = st('minecraft:air')
hull = [(x, 0, z) for x in range(1, 8) for z in range(1, 4)] + [(0, 1, 2), (8, 1, 2)] + [(x, 1, z) for x in range(1, 8) for z in (0, 4)]
for p in hull: b[p] = st('stormreach:driftwood_planks')
for x in (2, 5): b[(x, 1, 2)] = st('stormreach:driftwood_slab', type='bottom', waterlogged='false')
b[(6, 1, 1)] = st('minecraft:air'); b[(6, 1, 0)] = st('minecraft:air')  # broken side
b[(4, 1, 2)] = st('minecraft:chest', facing='east', type='single', waterlogged='false'); chests[(4, 1, 2)] = 'stormreach:chests/wrecked_skiff'
b[(3, 1, 3)] = st('stormreach:driftwood_fence', north='false', south='false', east='false', west='false', waterlogged='false')
b[(3, 2, 3)] = st('stormreach:driftwood_fence', north='false', south='false', east='false', west='false', waterlogged='false')
b[(1, 1, 1)] = st('stormreach:salt_crystal', facing='up', waterlogged='false')
write_structure('wrecked_skiff', b, (9, 3, 5), chests)

for name, size, biomes, adapt, sep, spacing, salt in (('lighthouse', 1, '#stormreach:has_lighthouse', 'beard_thin', 12, 40, 271828183),
                                                       ('wrecked_skiff', 1, '#stormreach:has_wrecked_skiff', 'beard_thin', 6, 20, 314159265)):
    w(f'{WG}/template_pool/{name}.json', {'elements': [{'element': {'element_type': 'minecraft:single_pool_element', 'location': f'stormreach:{name}',
        'processors': {'processors': []}, 'projection': 'rigid'}, 'weight': 1}], 'fallback': 'minecraft:empty'})
    w(f'{WG}/structure/{name}.json', {'type': 'minecraft:jigsaw', 'biomes': biomes, 'max_distance_from_center': 80,
        'project_start_to_heightmap': 'WORLD_SURFACE_WG', 'size': size, 'spawn_overrides': {}, 'start_height': {'absolute': 0},
        'start_pool': f'stormreach:{name}', 'step': 'surface_structures', 'terrain_adaptation': adapt, 'use_expansion_hack': False})
    w(f'{WG}/structure_set/{name}.json', {'placement': {'type': 'minecraft:random_spread', 'salt': salt, 'separation': sep, 'spacing': spacing},
        'structures': [{'structure': f'stormreach:{name}', 'weight': 1}]})
print('data3 done')
