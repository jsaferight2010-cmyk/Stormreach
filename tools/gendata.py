"""Writes every Stormreach JSON file (assets + data) in Minecraft 26.3 format."""
import os as _os, pathlib as _pl
ROOT = str(_pl.Path(__file__).resolve().parent.parent)  # the Stormreach project folder
VANILLA = _os.environ.get('STORMREACH_VANILLA', str(_pl.Path(ROOT) / '.vanilla'))  # extracted 26.3 jar (assets/ + data/)
MCJAR = _os.environ.get('STORMREACH_MCJAR', str(_pl.Path(ROOT) / '.vanilla' / 'minecraft-26.3.jar'))
import json, os, copy
R = ROOT + '/src/main/resources'
A = f'{R}/assets/stormreach'; D = f'{R}/data/stormreach'; NS = 'stormreach'
VD = VANILLA + ''

def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f: json.dump(obj, f, indent=2); f.write('\n')

def ns(x): return x if ':' in x else f'{NS}:{x}'

# ---------------- block models / blockstates / item defs ----------------
def item_def(name, model): w(f'{A}/items/{name}.json', {'model': {'type': 'minecraft:model', 'model': ns(model)}})

def cube(name, tex=None):
    w(f'{A}/models/block/{name}.json', {'parent': 'minecraft:block/cube_all', 'textures': {'all': ns('block/' + (tex or name))}})
    w(f'{A}/blockstates/{name}.json', {'variants': {'': {'model': ns('block/' + name)}}})
    item_def(name, 'block/' + name)

for b in ('stormstone', 'polished_stormstone', 'stormstone_bricks', 'driftwood_planks',
          'storm_crystal_ore', 'storm_crystal_block', 'storm_lantern'):
    cube(b)

# stormglass: translucent like vanilla glass
w(f'{A}/models/block/stormglass.json', {'parent': 'minecraft:block/cube_all',
  'textures': {'all': {'force_translucent': True, 'sprite': 'stormreach:block/stormglass'}}})
w(f'{A}/blockstates/stormglass.json', {'variants': {'': {'model': 'stormreach:block/stormglass'}}})
item_def('stormglass', 'block/stormglass')

# driftwood log (axis)
w(f'{A}/models/block/driftwood_log.json', {'parent': 'minecraft:block/cube_column',
  'textures': {'end': 'stormreach:block/driftwood_log_top', 'side': 'stormreach:block/driftwood_log'}})
w(f'{A}/models/block/driftwood_log_horizontal.json', {'parent': 'minecraft:block/cube_column_horizontal',
  'textures': {'end': 'stormreach:block/driftwood_log_top', 'side': 'stormreach:block/driftwood_log'}})
w(f'{A}/blockstates/driftwood_log.json', {'variants': {
  'axis=x': {'model': 'stormreach:block/driftwood_log_horizontal', 'x': 90, 'y': 90},
  'axis=y': {'model': 'stormreach:block/driftwood_log'},
  'axis=z': {'model': 'stormreach:block/driftwood_log_horizontal', 'x': 90}}})
item_def('driftwood_log', 'block/driftwood_log')

def stairs_slab(stairs, slab, full, tex):
    t = {'bottom': ns('block/' + tex), 'side': ns('block/' + tex), 'top': ns('block/' + tex)}
    for suffix, parent in (('', 'stairs'), ('_inner', 'inner_stairs'), ('_outer', 'outer_stairs')):
        w(f'{A}/models/block/{stairs}{suffix}.json', {'parent': f'minecraft:block/{parent}', 'textures': t})
    vs = json.load(open(f'{VD}/assets/minecraft/blockstates/oak_stairs.json'))
    s = json.dumps(vs).replace('minecraft:block/oak_stairs', ns('block/' + stairs))
    w(f'{A}/blockstates/{stairs}.json', json.loads(s))
    item_def(stairs, 'block/' + stairs)
    w(f'{A}/models/block/{slab}.json', {'parent': 'minecraft:block/slab', 'textures': t})
    w(f'{A}/models/block/{slab}_top.json', {'parent': 'minecraft:block/slab_top', 'textures': t})
    w(f'{A}/blockstates/{slab}.json', {'variants': {
        'type=bottom': {'model': ns('block/' + slab)}, 'type=double': {'model': ns('block/' + full)},
        'type=top': {'model': ns('block/' + slab + '_top')}}})
    item_def(slab, 'block/' + slab)
stairs_slab('stormstone_brick_stairs', 'stormstone_brick_slab', 'stormstone_bricks', 'stormstone_bricks')
stairs_slab('driftwood_stairs', 'driftwood_slab', 'driftwood_planks', 'driftwood_planks')

# barometer: power 0 = clear, 1-14 = rain, 15 = storm
for k in ('clear', 'rain', 'storm'):
    w(f'{A}/models/block/storm_barometer_{k}.json', {'parent': 'minecraft:block/cube_bottom_top', 'textures': {
        'bottom': 'stormreach:block/polished_stormstone', 'top': 'stormreach:block/polished_stormstone',
        'side': f'stormreach:block/storm_barometer_{k}'}})
w(f'{A}/blockstates/storm_barometer.json', {'variants': {
    f'power={p}': {'model': 'stormreach:block/storm_barometer_' + ('clear' if p == 0 else 'storm' if p == 15 else 'rain')}
    for p in range(16)}})
item_def('storm_barometer', 'block/storm_barometer_storm')

# plants
w(f'{A}/models/block/sea_thrift.json', {'parent': 'minecraft:block/cross', 'textures': {'cross': 'stormreach:block/sea_thrift'}})
w(f'{A}/blockstates/sea_thrift.json', {'variants': {'': {'model': 'stormreach:block/sea_thrift'}}})
w(f'{A}/models/item/sea_thrift.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'stormreach:block/sea_thrift'}})
item_def('sea_thrift', 'item/sea_thrift')
for a in range(4):
    w(f'{A}/models/block/stormberry_shrub_stage{a}.json', {'parent': 'minecraft:block/cross',
      'textures': {'cross': f'stormreach:block/stormberry_shrub_stage{a}'}})
w(f'{A}/blockstates/stormberry_shrub.json', {'variants': {
    f'age={a}': {'model': f'stormreach:block/stormberry_shrub_stage{a}'} for a in range(4)}})

# plain items
GEN = ['storm_shard', 'gull_feather', 'crab_claw', 'cooked_crab_claw', 'stormberries', 'stormberry_pie', 'storm_chowder',
       'thrift_tea', 'thunderhead_horn', 'cliff_crab_spawn_egg', 'storm_gull_spawn_egg', 'squall_spawn_egg']
HAND = ['tempest_staff', 'stormshard_sword', 'stormshard_pickaxe', 'stormshard_axe', 'stormshard_shovel']
for i in GEN:
    w(f'{A}/models/item/{i}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'stormreach:item/{i}'}})
    item_def(i, 'item/' + i)
for i in HAND:
    w(f'{A}/models/item/{i}.json', {'parent': 'minecraft:item/handheld', 'textures': {'layer0': f'stormreach:item/{i}'}})
    item_def(i, 'item/' + i)

# ---------------- language ----------------
lang = {
 'itemGroup.stormreach': 'Stormreach',
 'biome.stormreach.stormreach_coast': 'Stormreach Coast',
 'block.stormreach.stormstone': 'Stormstone', 'block.stormreach.polished_stormstone': 'Polished Stormstone',
 'block.stormreach.stormstone_bricks': 'Stormstone Bricks', 'block.stormreach.stormstone_brick_stairs': 'Stormstone Brick Stairs',
 'block.stormreach.stormstone_brick_slab': 'Stormstone Brick Slab', 'block.stormreach.driftwood_log': 'Driftwood Log',
 'block.stormreach.driftwood_planks': 'Driftwood Planks', 'block.stormreach.driftwood_stairs': 'Driftwood Stairs',
 'block.stormreach.driftwood_slab': 'Driftwood Slab', 'block.stormreach.storm_crystal_ore': 'Storm Crystal Ore',
 'block.stormreach.storm_crystal_block': 'Block of Storm Crystal', 'block.stormreach.stormglass': 'Stormglass',
 'block.stormreach.storm_lantern': 'Storm Lantern', 'block.stormreach.storm_barometer': 'Storm Barometer',
 'block.stormreach.sea_thrift': 'Sea Thrift', 'block.stormreach.stormberry_shrub': 'Stormberry Shrub',
 'item.stormreach.storm_shard': 'Storm Shard', 'item.stormreach.gull_feather': 'Gull Feather',
 'item.stormreach.crab_claw': 'Crab Claw', 'item.stormreach.cooked_crab_claw': 'Cooked Crab Claw',
 'item.stormreach.stormberries': 'Stormberries', 'item.stormreach.stormberry_pie': 'Stormberry Pie',
 'item.stormreach.storm_chowder': 'Storm Chowder', 'item.stormreach.thrift_tea': 'Thrift Tea',
 'item.stormreach.tempest_staff': 'Tempest Staff', 'item.stormreach.thunderhead_horn': 'Thunderhead Horn',
 'item.stormreach.stormshard_sword': 'Stormshard Sword', 'item.stormreach.stormshard_pickaxe': 'Stormshard Pickaxe',
 'item.stormreach.stormshard_axe': 'Stormshard Axe', 'item.stormreach.stormshard_shovel': 'Stormshard Shovel',
 'item.stormreach.cliff_crab_spawn_egg': 'Cliff Crab Spawn Egg', 'item.stormreach.storm_gull_spawn_egg': 'Storm Gull Spawn Egg',
 'item.stormreach.squall_spawn_egg': 'Squall Spawn Egg',
 'entity.stormreach.cliff_crab': 'Cliff Crab', 'entity.stormreach.storm_gull': 'Storm Gull', 'entity.stormreach.squall': 'Squall',
 'item.stormreach.tempest_staff.tooltip': 'Calls lightning where you aim. Free to use during a thunderstorm.',
 'item.stormreach.tempest_staff.no_target': 'Nothing in range to strike',
 'item.stormreach.thunderhead_horn.tooltip': 'Summons a Thunderhead over the world. Single use.',
 'item.stormreach.thunderhead_horn.used': 'A Thunderhead gathers on the horizon...',
 'item.stormreach.thunderhead_horn.no_sky': 'There is no sky here to call a storm from',
 'item.stormreach.stormshard_sword.tooltip': 'In a thunderstorm, hits may call lightning on the target.',
 'message.stormreach.thunderhead': 'A Thunderhead rolls in off the sea!',
 'command.stormreach.thunderhead': 'Summoned a Thunderhead for %s minute(s)',
 'command.stormreach.calm': 'The storm passes',
 'command.stormreach.status': 'Weather: %s. Standing in a storm zone: %s',
 'command.stormreach.weather.clear': 'clear', 'command.stormreach.weather.gale': 'Gale (rain and wind)',
 'command.stormreach.weather.thunderhead': 'Thunderhead',
 'command.stormreach.reloaded': 'Reloaded config/stormreach.properties',
 'config.stormreach.title': 'Stormreach Settings',
 'config.stormreach.page.world': 'World & Weather', 'config.stormreach.page.mobs': 'Mobs', 'config.stormreach.page.items': 'Items',
 'config.stormreach.reset': 'Reset to Defaults', 'config.stormreach.reset.tooltip': 'Puts every Stormreach setting back to its default.',
 'config.stormreach.biomeEnabled': 'Stormreach Coast',
 'config.stormreach.biomeEnabled.tooltip': 'Generate the Stormreach Coast biome in new chunks. Restart the game after changing.',
 'config.stormreach.biomeFrequency': 'Coast Frequency',
 'config.stormreach.biomeFrequency.tooltip': 'How much of the cool, wet beaches become Stormreach Coast. Restart after changing; only affects new chunks.',
 'config.stormreach.biomeFrequency.rare': 'Rare', 'config.stormreach.biomeFrequency.normal': 'Normal', 'config.stormreach.biomeFrequency.common': 'Common',
 'config.stormreach.oreGeneration': 'Lightning Grows Crystals',
 'config.stormreach.oreGeneration.tooltip': 'Lightning striking Stormstone can turn it into Storm Crystal Ore.',
 'config.stormreach.stormWeather': 'Storm Weather',
 'config.stormreach.stormWeather.tooltip': 'Rain brings Gales and thunder brings Thunderheads to the Stormreach Coast.',
 'config.stormreach.stormsOnAllCoasts': 'Storms on All Coasts',
 'config.stormreach.stormsOnAllCoasts.tooltip': 'Also bring Gales and Thunderheads to every beach and ocean, not just the Stormreach Coast.',
 'config.stormreach.lightningFrequency': 'Lightning',
 'config.stormreach.lightningFrequency.tooltip': 'How often Thunderhead lightning strikes near you. 0 turns it off.',
 'config.stormreach.lightningHarmful': 'Dangerous Lightning',
 'config.stormreach.lightningHarmful.tooltip': 'When off, Thunderhead lightning is only for show: no fire, no damage.',
 'config.stormreach.wind': 'Wind Gusts', 'config.stormreach.wind.tooltip': 'Storm gusts push you around when you are outside. Sneak to brace yourself.',
 'config.stormreach.windStrength': 'Wind Strength', 'config.stormreach.windStrength.tooltip': 'How hard the gusts push.',
 'config.stormreach.stormParticles': 'Sea Spray', 'config.stormreach.stormParticles.tooltip': 'Show wind-blown spray and sparks during storms.',
 'config.stormreach.stormMessages': 'Storm Warnings', 'config.stormreach.stormMessages.tooltip': 'Show a message when a Thunderhead reaches you.',
 'config.stormreach.crabSpawning': 'Cliff Crabs', 'config.stormreach.crabSpawning.tooltip': 'Cliff Crabs spawn on the Stormreach Coast.',
 'config.stormreach.gullSpawning': 'Storm Gulls', 'config.stormreach.gullSpawning.tooltip': 'Storm Gulls spawn on the Stormreach Coast.',
 'config.stormreach.squallSpawning': 'Squalls', 'config.stormreach.squallSpawning.tooltip': 'Hostile Squalls form inside Thunderheads.',
 'config.stormreach.maxSquallsPerPlayer': 'Max Squalls', 'config.stormreach.maxSquallsPerPlayer.tooltip': 'Most Squalls allowed near each player at once.',
 'config.stormreach.tempestStaffCooldown': 'Staff Cooldown', 'config.stormreach.tempestStaffCooldown.tooltip': 'Seconds between Tempest Staff strikes.',
 'config.stormreach.tempestStaffRange': 'Staff Range', 'config.stormreach.tempestStaffRange.tooltip': 'How far away (in blocks) the Tempest Staff can strike.',
 'config.stormreach.stormbladeLightningChance': 'Sword Lightning', 'config.stormreach.stormbladeLightningChance.tooltip': 'Chance per hit that the Stormshard Sword calls lightning during a thunderstorm.',
 'config.stormreach.hornStormMinutes': 'Horn Storm Length', 'config.stormreach.hornStormMinutes.tooltip': 'How long a Thunderhead Horn storm lasts, in minutes.',
}
w(f'{A}/lang/en_us.json', lang)

# ---------------- loot tables ----------------
def self_drop(name):
    return {'type': 'minecraft:block', 'pools': [{'condition': {'type': 'minecraft:survives_explosion'},
            'entries': [{'type': 'minecraft:item', 'name': ns(name)}], 'rolls': 1}], 'random_sequence': f'stormreach:blocks/{name}'}
for b in ('stormstone', 'polished_stormstone', 'stormstone_bricks', 'stormstone_brick_stairs', 'driftwood_log', 'driftwood_planks',
          'driftwood_stairs', 'storm_crystal_block', 'storm_lantern', 'storm_barometer', 'sea_thrift'):
    w(f'{D}/loot_table/blocks/{b}.json', self_drop(b))
for slab in ('stormstone_brick_slab', 'driftwood_slab'):
    w(f'{D}/loot_table/blocks/{slab}.json', {'type': 'minecraft:block', 'pools': [{'entries': [{'type': 'minecraft:item',
      'modifier': [{'type': 'minecraft:set_count', 'condition': {'type': 'minecraft:match_block', 'blocks': ns(slab), 'state': {'type': 'double'}}, 'count': 2},
                   {'type': 'minecraft:explosion_decay'}], 'name': ns(slab)}], 'rolls': 1}], 'random_sequence': f'stormreach:blocks/{slab}'})
w(f'{D}/loot_table/blocks/stormglass.json', {'type': 'minecraft:block', 'pools': [{'condition': 'minecraft:tool/can_silk_touch',
  'entries': [{'type': 'minecraft:item', 'name': 'stormreach:stormglass'}], 'rolls': 1}], 'random_sequence': 'stormreach:blocks/stormglass'})
w(f'{D}/loot_table/blocks/storm_crystal_ore.json', {'type': 'minecraft:block', 'pools': [{'entries': [{'type': 'minecraft:alternatives', 'children': [
  {'type': 'minecraft:item', 'condition': 'minecraft:tool/can_silk_touch', 'name': 'stormreach:storm_crystal_ore'},
  {'type': 'minecraft:item', 'modifier': [{'type': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'max': 2, 'min': 1}},
     {'type': 'minecraft:apply_bonus', 'enchantment': 'minecraft:fortune', 'formula': 'minecraft:ore_drops'}, {'type': 'minecraft:explosion_decay'}],
   'name': 'stormreach:storm_shard'}]}], 'rolls': 1}], 'random_sequence': 'stormreach:blocks/storm_crystal_ore'})
def shrub_pool(age, lo, hi):
    return {'condition': {'type': 'minecraft:match_block', 'blocks': 'stormreach:stormberry_shrub', 'state': {'age': str(age)}},
            'entries': [{'type': 'minecraft:item', 'name': 'stormreach:stormberries'}],
            'modifier': [{'type': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'max': hi, 'min': lo}},
                         {'type': 'minecraft:apply_bonus', 'enchantment': 'minecraft:fortune', 'formula': 'minecraft:uniform_bonus_count', 'parameters': {'bonusMultiplier': 1}}],
            'rolls': 1}
w(f'{D}/loot_table/blocks/stormberry_shrub.json', {'type': 'minecraft:block', 'modifier': {'type': 'minecraft:explosion_decay'},
  'pools': [shrub_pool(0, 1, 1), shrub_pool(1, 1, 1), shrub_pool(2, 1, 2), shrub_pool(3, 2, 4)], 'random_sequence': 'stormreach:blocks/stormberry_shrub'})

def mob_loot(name, item, lo, hi, smelt=False):
    mods = [{'type': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'max': hi, 'min': lo}}]
    if smelt:
        mods.append({'type': 'minecraft:furnace_smelt', 'condition': {'type': 'minecraft:any_of', 'terms': [
            {'type': 'minecraft:entity_properties', 'entity': 'this', 'predicate': {'minecraft:flags': {'is_on_fire': True}}},
            {'type': 'minecraft:entity_properties', 'entity': 'direct_attacker', 'predicate': {'minecraft:equipment': {'mainhand': {'predicates': {'minecraft:enchantments': [{'enchantments': '#minecraft:smelts_loot'}]}}}}}]}})
    mods.append({'type': 'minecraft:enchanted_count_increase', 'count': {'type': 'minecraft:uniform', 'max': 1.0, 'min': 0.0}, 'enchantment': 'minecraft:looting'})
    w(f'{D}/loot_table/entities/{name}.json', {'type': 'minecraft:entity', 'pools': [{'entries': [{'type': 'minecraft:item', 'modifier': mods, 'name': ns(item)}], 'rolls': 1}],
       'random_sequence': f'stormreach:entities/{name}'})
mob_loot('cliff_crab', 'crab_claw', 1, 2, smelt=True)
mob_loot('storm_gull', 'gull_feather', 0, 2)
mob_loot('squall', 'storm_shard', 0, 1)

# ---------------- recipes ----------------
def shaped(name, pattern, key, result, count=1, cat='misc', group=None):
    r = {'type': 'minecraft:crafting_shaped', 'category': cat, 'key': {k: ns(v) if not v.startswith('#') else v for k, v in key.items()},
         'pattern': pattern, 'result': {'count': count, 'id': ns(result)}}
    if group: r['group'] = group
    w(f'{D}/recipe/{name}.json', r)
def shapeless(name, ings, result, count=1, cat='misc'):
    w(f'{D}/recipe/{name}.json', {'type': 'minecraft:crafting_shapeless', 'category': cat,
       'ingredients': [ns(i) if not i.startswith('#') else i for i in ings], 'result': {'count': count, 'id': ns(result)}})
def cook(name, ing, result, xp=0.35):
    for t, time, suffix in (('smelting', 200, ''), ('smoking', 100, '_from_smoking'), ('campfire_cooking', 600, '_from_campfire_cooking')):
        w(f'{D}/recipe/{name}{suffix}.json', {'type': f'minecraft:{t}', 'category': 'food', 'cookingtime': time, 'experience': xp,
           'ingredient': ns(ing), 'result': {'id': ns(result)}})
def cut(name, ing, result, count=1):
    w(f'{D}/recipe/{name}.json', {'type': 'minecraft:stonecutting', 'ingredient': ns(ing), 'result': {'count': count, 'id': ns(result)}})

shapeless('driftwood_planks', ['driftwood_log'], 'driftwood_planks', 4, 'building')
shaped('driftwood_stairs', ['#  ', '## ', '###'], {'#': 'driftwood_planks'}, 'driftwood_stairs', 4, 'building', 'wooden_stairs')
shaped('driftwood_slab', ['###'], {'#': 'driftwood_planks'}, 'driftwood_slab', 6, 'building', 'wooden_slab')
shaped('polished_stormstone', ['##', '##'], {'#': 'stormstone'}, 'polished_stormstone', 4, 'building')
shaped('stormstone_bricks', ['##', '##'], {'#': 'polished_stormstone'}, 'stormstone_bricks', 4, 'building')
shaped('stormstone_brick_stairs', ['#  ', '## ', '###'], {'#': 'stormstone_bricks'}, 'stormstone_brick_stairs', 4, 'building')
shaped('stormstone_brick_slab', ['###'], {'#': 'stormstone_bricks'}, 'stormstone_brick_slab', 6, 'building')
for res, n in (('polished_stormstone', 1), ('stormstone_bricks', 1), ('stormstone_brick_stairs', 1), ('stormstone_brick_slab', 2)):
    cut(f'{res}_from_stormstone_stonecutting', 'stormstone', res, n)
cut('stormstone_brick_stairs_from_stormstone_bricks_stonecutting', 'stormstone_bricks', 'stormstone_brick_stairs')
cut('stormstone_brick_slab_from_stormstone_bricks_stonecutting', 'stormstone_bricks', 'stormstone_brick_slab', 2)
shaped('stormglass', ['GGG', 'GSG', 'GGG'], {'G': 'minecraft:glass', 'S': 'storm_shard'}, 'stormglass', 8, 'building')
shaped('storm_crystal_block', ['SSS', 'SSS', 'SSS'], {'S': 'storm_shard'}, 'storm_crystal_block', 1, 'building')
shapeless('storm_shard_from_storm_crystal_block', ['storm_crystal_block'], 'storm_shard', 9)
shaped('storm_lantern', [' G ', 'GSG', ' G '], {'G': 'stormglass', 'S': 'storm_shard'}, 'storm_lantern', 2, 'building')
shaped('storm_barometer', ['PPP', 'PSP', 'PRP'], {'P': 'polished_stormstone', 'S': 'storm_shard', 'R': 'minecraft:redstone'}, 'storm_barometer', 1, 'redstone')
shaped('tempest_staff', ['  S', ' DF', 'D  '], {'S': 'storm_shard', 'D': 'driftwood_planks', 'F': 'gull_feather'}, 'tempest_staff', 1, 'equipment')
shaped('thunderhead_horn', [' F ', 'SHS', ' C '], {'F': 'gull_feather', 'S': 'storm_shard', 'H': 'minecraft:goat_horn', 'C': 'storm_crystal_block'}, 'thunderhead_horn', 1, 'equipment')
shaped('stormshard_sword', ['S', 'S', 'T'], {'S': 'storm_shard', 'T': 'minecraft:stick'}, 'stormshard_sword', 1, 'equipment')
shaped('stormshard_pickaxe', ['SSS', ' T ', ' T '], {'S': 'storm_shard', 'T': 'minecraft:stick'}, 'stormshard_pickaxe', 1, 'equipment')
shaped('stormshard_axe', ['SS', 'ST', ' T'], {'S': 'storm_shard', 'T': 'minecraft:stick'}, 'stormshard_axe', 1, 'equipment')
shaped('stormshard_shovel', ['S', 'T', 'T'], {'S': 'storm_shard', 'T': 'minecraft:stick'}, 'stormshard_shovel', 1, 'equipment')
cook('cooked_crab_claw', 'crab_claw', 'cooked_crab_claw')
shapeless('stormberry_pie', ['stormberries', 'stormberries', 'minecraft:sugar', 'minecraft:egg'], 'stormberry_pie')
shapeless('storm_chowder', ['cooked_crab_claw', 'minecraft:baked_potato', 'minecraft:kelp', 'minecraft:bowl'], 'storm_chowder')
shapeless('thrift_tea', ['sea_thrift', 'sea_thrift', 'minecraft:sugar', 'minecraft:glass_bottle'], 'thrift_tea')
shapeless('pink_dye_from_sea_thrift', ['sea_thrift'], 'minecraft:pink_dye')

# ---------------- tags ----------------
def tag(path, values):
    w(path, {'values': values})
MC = f'{R}/data/minecraft/tags'; C = f'{R}/data/c/tags'
tag(f'{MC}/block/mineable/pickaxe.json', ['stormreach:stormstone', 'stormreach:polished_stormstone', 'stormreach:stormstone_bricks',
    'stormreach:stormstone_brick_stairs', 'stormreach:stormstone_brick_slab', 'stormreach:storm_crystal_ore',
    'stormreach:storm_crystal_block', 'stormreach:storm_barometer'])
tag(f'{MC}/block/mineable/axe.json', ['stormreach:driftwood_log', 'stormreach:driftwood_planks', 'stormreach:driftwood_stairs', 'stormreach:driftwood_slab'])
tag(f'{MC}/block/needs_stone_tool.json', ['stormreach:storm_crystal_ore'])
tag(f'{MC}/block/needs_iron_tool.json', ['stormreach:storm_crystal_block'])
for t in ('logs', 'logs_that_burn', 'overworld_natural_logs'):
    tag(f'{MC}/block/{t}.json', ['stormreach:driftwood_log'])
for t in ('logs', 'logs_that_burn'):
    tag(f'{MC}/item/{t}.json', ['stormreach:driftwood_log'])
tag(f'{MC}/block/planks.json', ['stormreach:driftwood_planks']); tag(f'{MC}/item/planks.json', ['stormreach:driftwood_planks'])
tag(f'{MC}/block/wooden_stairs.json', ['stormreach:driftwood_stairs']); tag(f'{MC}/item/wooden_stairs.json', ['stormreach:driftwood_stairs'])
tag(f'{MC}/block/wooden_slabs.json', ['stormreach:driftwood_slab']); tag(f'{MC}/item/wooden_slabs.json', ['stormreach:driftwood_slab'])
tag(f'{MC}/block/stairs.json', ['stormreach:stormstone_brick_stairs', 'stormreach:driftwood_stairs'])
tag(f'{MC}/item/stairs.json', ['stormreach:stormstone_brick_stairs', 'stormreach:driftwood_stairs'])
tag(f'{MC}/block/slabs.json', ['stormreach:stormstone_brick_slab', 'stormreach:driftwood_slab'])
tag(f'{MC}/item/slabs.json', ['stormreach:stormstone_brick_slab', 'stormreach:driftwood_slab'])
tag(f'{MC}/block/small_flowers.json', ['stormreach:sea_thrift']); tag(f'{MC}/item/small_flowers.json', ['stormreach:sea_thrift'])
tag(f'{MC}/block/base_stone_overworld.json', ['stormreach:stormstone'])
tag(f'{MC}/block/impermeable.json', ['stormreach:stormglass'])
tag(f'{MC}/item/swords.json', ['stormreach:stormshard_sword']); tag(f'{MC}/item/pickaxes.json', ['stormreach:stormshard_pickaxe'])
tag(f'{MC}/item/axes.json', ['stormreach:stormshard_axe']); tag(f'{MC}/item/shovels.json', ['stormreach:stormshard_shovel'])
tag(f'{D}/tags/item/storm_shard_tool_materials.json', ['stormreach:storm_shard'])
tag(f'{MC}/worldgen/biome/is_beach.json', ['stormreach:stormreach_coast'])
tag(f'{MC}/worldgen/biome/is_overworld.json', ['stormreach:stormreach_coast'])
tag(f'{MC}/worldgen/biome/has_structure/mineshaft.json', ['stormreach:stormreach_coast'])
tag(f'{MC}/worldgen/biome/has_structure/ruined_portal_standard.json', ['stormreach:stormreach_coast'])
# Convention (c:) tags so other mods recognise our things
tag(f'{C}/block/ores.json', ['stormreach:storm_crystal_ore']); tag(f'{C}/item/ores.json', ['stormreach:storm_crystal_ore'])
tag(f'{C}/block/storage_blocks.json', ['stormreach:storm_crystal_block']); tag(f'{C}/item/storage_blocks.json', ['stormreach:storm_crystal_block'])
tag(f'{C}/item/gems.json', ['stormreach:storm_shard'])
tag(f'{C}/item/feathers.json', ['stormreach:gull_feather'])
tag(f'{C}/item/foods.json', ['stormreach:crab_claw', 'stormreach:cooked_crab_claw', 'stormreach:stormberries', 'stormreach:stormberry_pie',
                            'stormreach:storm_chowder', 'stormreach:thrift_tea'])
tag(f'{C}/item/foods/berry.json', ['stormreach:stormberries'])
tag(f'{C}/item/foods/raw_meat.json', ['stormreach:crab_claw']); tag(f'{C}/item/foods/cooked_meat.json', ['stormreach:cooked_crab_claw'])
tag(f'{C}/item/foods/soup.json', ['stormreach:storm_chowder'])
tag(f'{C}/block/glass_blocks.json', ['stormreach:stormglass']); tag(f'{C}/item/glass_blocks.json', ['stormreach:stormglass'])
tag(f'{C}/worldgen/biome/is_beach.json', ['stormreach:stormreach_coast'])
tag(f'{C}/worldgen/biome/is_windswept.json', ['stormreach:stormreach_coast'])
tag(f'{C}/worldgen/biome/is_overworld.json', ['stormreach:stormreach_coast'])

# ---------------- worldgen ----------------
WG = f'{D}/worldgen'
w(f'{WG}/feature/ore_stormstone.json', {'type': 'minecraft:ore', 'discard_chance_on_air_exposure': 0.0, 'size': 48, 'targets': [
  {'state': 'stormreach:stormstone', 'target': {'predicate_type': 'minecraft:tag_match', 'tag': 'minecraft:base_stone_overworld'}}]})
w(f'{WG}/placed_feature/ore_stormstone.json', {'feature': 'stormreach:ore_stormstone', 'placement': [
  {'type': 'minecraft:count', 'count': 10}, {'type': 'minecraft:in_square'},
  {'type': 'minecraft:height_range', 'height': {'type': 'minecraft:uniform', 'max_inclusive': {'absolute': 128}, 'min_inclusive': {'absolute': 30}}},
  {'type': 'minecraft:biome'}]})
w(f'{WG}/feature/ore_storm_crystal.json', {'type': 'minecraft:ore', 'discard_chance_on_air_exposure': 0.0, 'size': 6, 'targets': [
  {'state': 'stormreach:storm_crystal_ore', 'target': {'predicate_type': 'minecraft:tag_match', 'tag': 'minecraft:base_stone_overworld'}}]})
w(f'{WG}/placed_feature/ore_storm_crystal.json', {'feature': 'stormreach:ore_storm_crystal', 'placement': [
  {'type': 'minecraft:count', 'count': 8}, {'type': 'minecraft:in_square'},
  {'type': 'minecraft:height_range', 'height': {'type': 'minecraft:trapezoid', 'max_inclusive': {'absolute': 112}, 'min_inclusive': {'absolute': 0}}},
  {'type': 'minecraft:biome'}]})
w(f'{WG}/feature/stormstone_boulder.json', {'type': 'minecraft:block_blob', 'can_place_on': {'type': 'minecraft:matching_block_tag',
  'tag': 'minecraft:forest_rock_can_place_on'}, 'state': 'stormreach:stormstone'})
w(f'{WG}/placed_feature/stormstone_boulder.json', {'feature': 'stormreach:stormstone_boulder', 'placement': [
  {'type': 'minecraft:count', 'count': 2}, {'type': 'minecraft:in_square'}, {'type': 'minecraft:heightmap', 'heightmap': 'MOTION_BLOCKING'}, {'type': 'minecraft:biome'}]})
fallen = json.load(open(f'{VD}/data/minecraft/worldgen/feature/fallen_oak_tree.json'))
fallen['trunk_provider'] = {'id': 'stormreach:driftwood_log', 'properties': {'axis': 'y'}}
fallen['log_decorators'] = []
w(f'{WG}/feature/fallen_driftwood.json', fallen)
w(f'{WG}/placed_feature/fallen_driftwood.json', {'feature': 'stormreach:fallen_driftwood', 'placement': [
  {'type': 'minecraft:rarity_filter', 'chance': 2}, {'type': 'minecraft:in_square'}, {'type': 'minecraft:heightmap', 'heightmap': 'MOTION_BLOCKING'},
  {'type': 'minecraft:biome'}, {'type': 'minecraft:block_predicate_filter', 'predicate': {'type': 'minecraft:would_survive', 'state': 'minecraft:oak_sapling'}}]})
def patch(name, state, chance, count, spread):
    w(f'{WG}/feature/{name}.json', {'type': 'minecraft:simple_block', 'to_place': state})
    w(f'{WG}/placed_feature/{name}.json', {'feature': f'stormreach:{name}', 'placement': [
      {'type': 'minecraft:rarity_filter', 'chance': chance}, {'type': 'minecraft:in_square'}, {'type': 'minecraft:heightmap', 'heightmap': 'MOTION_BLOCKING'},
      {'type': 'minecraft:biome'}, {'type': 'minecraft:count', 'count': count},
      {'type': 'minecraft:offset', 'x': {'type': 'minecraft:trapezoid', 'max': spread, 'min': -spread, 'plateau': 0},
       'y': {'type': 'minecraft:trapezoid', 'max': 3, 'min': -3, 'plateau': 0}, 'z': {'type': 'minecraft:trapezoid', 'max': spread, 'min': -spread, 'plateau': 0}},
      {'type': 'minecraft:block_predicate_filter', 'predicate': {'type': 'minecraft:all_of', 'predicates': [
         {'type': 'minecraft:matching_block_tag', 'tag': 'minecraft:air'}, {'type': 'minecraft:would_survive', 'state': state}]}}]})
patch('patch_sea_thrift', {'id': 'stormreach:sea_thrift'}, 2, 40, 7)
patch('patch_stormberry_shrub', {'id': 'stormreach:stormberry_shrub', 'properties': {'age': '3'}}, 4, 24, 5)

beach = json.load(open(f'{VD}/data/minecraft/worldgen/biome/beach.json'))
biome = copy.deepcopy(beach)
spawns = biome['attributes']['minecraft:gameplay/natural_mob_spawns']['argument']['spawns_by_category']
spawns['creature'] = [
    {'type': 'stormreach:cliff_crab', 'count': {'type': 'minecraft:uniform', 'max_inclusive': 4, 'min_inclusive': 2}, 'weight': 12},
    {'type': 'stormreach:storm_gull', 'count': {'type': 'minecraft:uniform', 'max_inclusive': 3, 'min_inclusive': 2}, 'weight': 10},
    {'type': 'minecraft:turtle', 'count': {'type': 'minecraft:uniform', 'max_inclusive': 3, 'min_inclusive': 2}, 'weight': 2}]
biome['attributes']['minecraft:visual/sky_color'] = '#6c7d96'
biome['attributes']['minecraft:visual/fog_color'] = '#9aa8b8'
biome['attributes']['minecraft:visual/water_fog_color'] = '#1e3550'
biome['effects'] = {'water_color': '#3a5f8a', 'grass_color': '#6f9468', 'foliage_color': '#5d7f58'}
biome['temperature'] = 0.4
biome['downfall'] = 0.9
biome['has_precipitation'] = True
f = biome['features']
f[6] = f[6] + ['stormreach:ore_stormstone', 'stormreach:ore_storm_crystal']
f[2] = f[2] + ['stormreach:stormstone_boulder']
f[9] = ['minecraft:glow_lichen', 'stormreach:fallen_driftwood', 'minecraft:patch_grass_plain', 'minecraft:patch_tall_grass',
        'stormreach:patch_sea_thrift', 'stormreach:patch_stormberry_shrub', 'minecraft:brown_mushroom_normal',
        'minecraft:red_mushroom_normal', 'minecraft:patch_sugar_cane']
w(f'{WG}/biome/stormreach_coast.json', biome)

# pack.mcmeta for the mod's resources (26.3: resource 97.1, data 121.0)

print('data done')

# ---------------- advancements ----------------
import shutil, glob as _g
os.makedirs(f'{A}/textures/gui/advancements/backgrounds', exist_ok=True)
shutil.copy(f'{A}/textures/block/stormstone.png', f'{A}/textures/gui/advancements/backgrounds/stormreach.png')
ADV = f'{D}/advancement'
def has_item(item): return {'conditions': {'items': [{'items': ns(item)}]}, 'trigger': 'minecraft:inventory_changed'}
def adv(name, parent, icon, criteria, frame='task', title=None, desc=None, root=False):
    disp = {'title': {'translate': f'advancements.stormreach.{name}.title'},
            'description': {'translate': f'advancements.stormreach.{name}.description'},
            'icon': {'id': ns(icon)}, 'frame': frame}
    if root:
        disp.update({'background': 'stormreach:gui/advancements/backgrounds/stormreach', 'show_toast': False, 'announce_to_chat': False})
    a = {'criteria': criteria, 'display': disp, 'requirements': [list(criteria)]}
    if parent: a['parent'] = f'stormreach:{parent}'
    w(f'{ADV}/{name}.json', a)
    lang[f'advancements.stormreach.{name}.title'] = title
    lang[f'advancements.stormreach.{name}.description'] = desc
adv('root', None, 'storm_crystal_block', {'in_biome': {'conditions': {'player': {'type': 'minecraft:entity_properties', 'entity': 'this',
    'predicate': {'minecraft:location': {'biomes': 'stormreach:stormreach_coast'}}}}, 'trigger': 'minecraft:location'}},
    title='Stormreach', desc='Set foot on the storm-swept Stormreach Coast', root=True)
adv('storm_shard', 'root', 'storm_shard', {'shard': has_item('storm_shard')},
    title='Bottled Lightning', desc='Collect a Storm Shard from storm crystal ore or a Squall')
adv('crab_breeder', 'root', 'stormberries', {'bred': {'conditions': {'child': {'type': 'minecraft:entity_properties', 'entity': 'this',
    'predicate': {'minecraft:entity_type': 'stormreach:cliff_crab'}}}, 'trigger': 'minecraft:bred_animals'}},
    title='Crustacean Courtship', desc='Breed two Cliff Crabs with Stormberries')
adv('storm_chowder', 'crab_breeder', 'storm_chowder', {'ate': {'conditions': {'item': {'items': 'stormreach:storm_chowder'}}, 'trigger': 'minecraft:consume_item'}},
    title='Riding Out the Storm', desc='Eat a bowl of Storm Chowder')
adv('kill_squall', 'storm_shard', 'squall_spawn_egg', {'killed': {'conditions': {'entity': {'type': 'minecraft:entity_properties', 'entity': 'this',
    'predicate': {'minecraft:entity_type': 'stormreach:squall'}}}, 'trigger': 'minecraft:player_killed_entity'}},
    title='Calm Before the Storm', desc='Break up a Squall')
adv('tempest_staff', 'storm_shard', 'tempest_staff', {'staff': has_item('tempest_staff')},
    title='Ride the Lightning', desc='Craft a Tempest Staff')
adv('thunderhead_horn', 'tempest_staff', 'thunderhead_horn', {'horn': has_item('thunderhead_horn')}, frame='goal',
    title='Stormcaller', desc='Craft a Thunderhead Horn and bend the sky to your will')
adv('barometer', 'storm_shard', 'storm_barometer', {'baro': has_item('storm_barometer')},
    title='Under Pressure', desc='Build a Storm Barometer to read the weather')
w(f'{A}/lang/en_us.json', lang)

# recipe unlocks (so recipes show up in the recipe book)
UNLOCK = {'driftwood_planks': 'driftwood_log', 'driftwood_stairs': 'driftwood_planks', 'driftwood_slab': 'driftwood_planks',
          'polished_stormstone': 'stormstone', 'stormstone_bricks': 'polished_stormstone', 'stormstone_brick_stairs': 'stormstone_bricks',
          'stormstone_brick_slab': 'stormstone_bricks', 'stormglass': 'storm_shard', 'storm_crystal_block': 'storm_shard',
          'storm_shard_from_storm_crystal_block': 'storm_crystal_block', 'storm_lantern': 'stormglass', 'storm_barometer': 'storm_shard',
          'tempest_staff': 'storm_shard', 'thunderhead_horn': 'gull_feather', 'stormshard_sword': 'storm_shard',
          'stormshard_pickaxe': 'storm_shard', 'stormshard_axe': 'storm_shard', 'stormshard_shovel': 'storm_shard',
          'stormberry_pie': 'stormberries', 'storm_chowder': 'cooked_crab_claw', 'thrift_tea': 'sea_thrift',
          'pink_dye_from_sea_thrift': 'sea_thrift', 'cooked_crab_claw': 'crab_claw', 'cooked_crab_claw_from_smoking': 'crab_claw',
          'cooked_crab_claw_from_campfire_cooking': 'crab_claw'}
for f in _g.glob(f'{D}/recipe/*.json'):
    r = os.path.basename(f)[:-5]
    trig = UNLOCK.get(r, 'stormstone' if 'stonecutting' in r else None)
    if not trig: print('no unlock for', r); continue
    w(f'{ADV}/recipes/{r}.json', {'parent': 'minecraft:recipes/root', 'criteria': {
        'has_item': has_item(trig), 'has_the_recipe': {'conditions': {'recipes': f'stormreach:{r}'}, 'trigger': 'minecraft:recipe_unlocked'}},
        'requirements': [['has_the_recipe', 'has_item']], 'rewards': {'recipes': [f'stormreach:{r}']}})
print('advancements done')
