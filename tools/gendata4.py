"""Stormreach 2.0 language, advancements and recipe-book unlocks."""
import os as _os, pathlib as _pl
ROOT = str(_pl.Path(__file__).resolve().parent.parent)  # the Stormreach project folder
VANILLA = _os.environ.get('STORMREACH_VANILLA', str(_pl.Path(ROOT) / '.vanilla'))  # extracted 26.3 jar (assets/ + data/)
MCJAR = _os.environ.get('STORMREACH_MCJAR', str(_pl.Path(ROOT) / '.vanilla' / 'minecraft-26.3.jar'))
import json, os, glob
R = ROOT + '/src/main/resources'
A = f'{R}/assets/stormreach'; D = f'{R}/data/stormreach'
def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f: json.dump(obj, f, indent=2); f.write('\n')
lang = json.load(open(f'{A}/lang/en_us.json'))
def title(s): return ' '.join(p.capitalize() if p not in ('of', 'a', 'in') else p for p in s.split('_'))
blocks = ['stormstone_stairs', 'stormstone_slab', 'stormstone_wall', 'charged_stormstone', 'polished_stormstone_stairs', 'polished_stormstone_slab',
          'polished_stormstone_wall', 'cracked_stormstone_bricks', 'chiseled_stormstone', 'stormstone_brick_wall', 'driftwood_fence',
          'driftwood_fence_gate', 'driftwood_door', 'driftwood_trapdoor', 'driftwood_button', 'driftwood_pressure_plate', 'stormpine_log',
          'stripped_stormpine_log', 'stormpine_planks', 'stormpine_leaves', 'stormpine_sapling', 'stormpine_stairs', 'stormpine_slab', 'stormpine_fence',
          'stormpine_fence_gate', 'stormpine_door', 'stormpine_trapdoor', 'stormpine_button', 'stormpine_pressure_plate', 'stormglass_pane', 'fulgurite',
          'fulgurite_lantern', 'salt_block', 'salt_crystal', 'wind_vane', 'storm_collector', 'sea_rocket', 'saltgrass']
for b in blocks: lang[f'block.stormreach.{b}'] = title(b)
lang['block.stormreach.salt_block'] = 'Block of Salt'
items = {'salt': 'Salt', 'fulgurite_shard': 'Fulgurite Shard', 'storm_cell': 'Storm Cell', 'tempest_core': 'Tempest Core', 'gull_egg': 'Gull Egg',
         'fried_gull_egg': 'Fried Gull Egg', 'crab_cakes': 'Crab Cakes', 'stormberry_jam': 'Stormberry Jam', 'stormberry_juice': 'Stormberry Juice',
         'seaweed_salad': 'Seaweed Salad', 'salted_cod': 'Salted Cod', 'sailors_stew': "Sailor's Stew", 'stormfin': 'Stormfin',
         'cooked_stormfin': 'Cooked Stormfin', 'tempest_tonic': 'Tempest Tonic', 'rock_candy': 'Rock Candy', 'stormshard_helmet': 'Stormshard Helmet',
         'stormshard_chestplate': 'Stormshard Chestplate', 'stormshard_leggings': 'Stormshard Leggings', 'stormshard_boots': 'Stormshard Boots',
         'gale_wings': 'Gale Wings', 'squall_in_a_bottle': 'Squall in a Bottle', 'stormbreaker': 'Stormbreaker',
         'thunder_crab_spawn_egg': 'Thunder Crab Spawn Egg', 'harbor_seal_spawn_egg': 'Harbor Seal Spawn Egg',
         'brine_wraith_spawn_egg': 'Brine Wraith Spawn Egg', 'ball_lightning_spawn_egg': 'Ball Lightning Spawn Egg',
         'stormwarden_spawn_egg': 'Stormwarden Spawn Egg'}
for k, v in items.items(): lang[f'item.stormreach.{k}'] = v
for e, n in (('thunder_crab', 'Thunder Crab'), ('harbor_seal', 'Harbor Seal'), ('brine_wraith', 'Brine Wraith'), ('ball_lightning', 'Ball Lightning'),
             ('waterspout', 'Waterspout'), ('stormwarden', 'The Stormwarden'), ('squall_bottle', 'Squall in a Bottle')):
    lang[f'entity.stormreach.{e}'] = n
lang.update({
 'biome.stormreach.squallwind_bluffs': 'Squallwind Bluffs', 'biome.stormreach.thunderspire_highlands': 'Thunderspire Highlands',
 'biome.stormreach.brinewater_marsh': 'Brinewater Marsh',
 'effect.stormreach.static_charge': 'Static Charge', 'effect.stormreach.grounded': 'Grounded',
 'item.minecraft.potion.effect.static_charge': 'Potion of Static Charge', 'item.minecraft.splash_potion.effect.static_charge': 'Splash Potion of Static Charge',
 'item.minecraft.lingering_potion.effect.static_charge': 'Lingering Potion of Static Charge', 'item.minecraft.tipped_arrow.effect.static_charge': 'Arrow of Static Charge',
 'item.minecraft.potion.effect.grounded': 'Potion of Grounding', 'item.minecraft.splash_potion.effect.grounded': 'Splash Potion of Grounding',
 'item.minecraft.lingering_potion.effect.grounded': 'Lingering Potion of Grounding', 'item.minecraft.tipped_arrow.effect.grounded': 'Arrow of Grounding',
 'enchantment.stormreach.conductive': 'Conductive', 'enchantment.stormreach.galeforce': 'Galeforce', 'enchantment.stormreach.grounded': 'Grounded',
 'death.attack.stormreach.hail': '%1$s was pelted to death by hail', 'death.attack.stormreach.hail.player': '%1$s was pelted by hail while fighting %2$s',
 'item.stormreach.stormbreaker.tooltip': 'Use to unleash a thunderclap shockwave.',
 'message.stormreach.boss_summoned': 'The sky splits open. The Stormwarden descends!',
 'message.stormreach.boss_needs_storm': 'The crystal hums... it needs a thunderstorm and open sky.',
 'message.stormreach.boss_disabled': 'Summoning the Stormwarden is turned off in the Stormreach settings.',
 'command.stormreach.forecast': 'Weather: %s. Wind blowing toward the %s.',
 'command.stormreach.wind.north': 'north', 'command.stormreach.wind.south': 'south', 'command.stormreach.wind.east': 'east', 'command.stormreach.wind.west': 'west',
 'config.stormreach.page.weather': 'Weather',
 'config.stormreach.page.world': 'World',
 'config.stormreach.bluffsEnabled': 'Squallwind Bluffs', 'config.stormreach.bluffsEnabled.tooltip': 'Generate Squallwind Bluffs (takes half of the Stony Shores). Restart after changing; new chunks only.',
 'config.stormreach.highlandsEnabled': 'Thunderspire Highlands', 'config.stormreach.highlandsEnabled.tooltip': 'Generate Thunderspire Highlands (takes half of the Windswept Gravelly Hills). Restart after changing; new chunks only.',
 'config.stormreach.marshEnabled': 'Brinewater Marsh', 'config.stormreach.marshEnabled.tooltip': 'Generate Brinewater Marsh (takes half of the Swamps). Restart after changing; new chunks only.',
 'config.stormreach.fulguriteFromLightning': 'Lightning Makes Fulgurite', 'config.stormreach.fulguriteFromLightning.tooltip': 'Lightning that hits sand fuses it into Fulgurite.',
 'config.stormreach.stormfinFishing': 'Stormfin Fishing', 'config.stormreach.stormfinFishing.tooltip': 'You can catch Stormfin while it rains. Needs a world reload to change.',
 'config.stormreach.hail': 'Hail', 'config.stormreach.hail.tooltip': 'Thunderheads over the Thunderspire Highlands bring hail that hurts if you have no helmet.',
 'config.stormreach.ballLightning': 'Ball Lightning', 'config.stormreach.ballLightning.tooltip': 'Ball Lightning drifts out of Thunderheads toward players.',
 'config.stormreach.waterspouts': 'Waterspouts', 'config.stormreach.waterspouts.tooltip': 'Waterspouts form over the sea during Thunderheads.',
 'config.stormreach.sealSpawning': 'Harbor Seals', 'config.stormreach.sealSpawning.tooltip': 'Harbor Seals spawn on Stormreach shores.',
 'config.stormreach.wraithSpawning': 'Brine Wraiths', 'config.stormreach.wraithSpawning.tooltip': 'Brine Wraiths haunt the coasts and marshes at night.',
 'config.stormreach.crabConversion': 'Thunder Crabs', 'config.stormreach.crabConversion.tooltip': 'Lightning turns Cliff Crabs into hostile Thunder Crabs.',
 'config.stormreach.bossSummoning': 'Stormwarden Boss', 'config.stormreach.bossSummoning.tooltip': 'Allow summoning the Stormwarden with a Storm Cell on a Block of Storm Crystal.',
 'config.stormreach.armorSetBonus': 'Stormshard Set Bonus', 'config.stormreach.armorSetBonus.tooltip': 'A full Stormshard set makes you immune to lightning and faster in thunderstorms.',
 'config.stormreach.galeWingsBoost': 'Gale Wings Boost', 'config.stormreach.galeWingsBoost.tooltip': 'Storm gusts boost you while gliding with Gale Wings.',
})
w(f'{A}/lang/en_us.json', lang)

# advancements
ADV = f'{D}/advancement'
def has_item(item): return {'conditions': {'items': [{'items': item if ':' in item else f'stormreach:{item}'}]}, 'trigger': 'minecraft:inventory_changed'}
def adv(name, parent, icon, criteria, ttl, desc, frame='task', any_of=True, hidden=False):
    a = {'parent': f'stormreach:{parent}', 'criteria': criteria,
         'display': {'title': {'translate': f'advancements.stormreach.{name}.title'}, 'description': {'translate': f'advancements.stormreach.{name}.description'},
                     'icon': {'id': icon if ':' in icon else f'stormreach:{icon}'}, 'frame': frame, 'hidden': hidden},
         'requirements': [list(criteria)] if any_of else [[c] for c in criteria]}
    w(f'{ADV}/{name}.json', a); lang[f'advancements.stormreach.{name}.title'] = ttl; lang[f'advancements.stormreach.{name}.description'] = desc
def in_biome(b): return {'conditions': {'player': {'type': 'minecraft:entity_properties', 'entity': 'this', 'predicate': {'minecraft:location': {'biomes': f'stormreach:{b}'}}}}, 'trigger': 'minecraft:location'}
def killed(e): return {'conditions': {'entity': {'type': 'minecraft:entity_properties', 'entity': 'this', 'predicate': {'minecraft:entity_type': f'stormreach:{e}'}}}, 'trigger': 'minecraft:player_killed_entity'}
adv('all_biomes', 'root', 'stormpine_sapling', {b: in_biome(b) for b in ('stormreach_coast', 'squallwind_bluffs', 'thunderspire_highlands', 'brinewater_marsh')},
    'Storm Chaser', 'Visit every Stormreach biome', frame='challenge', any_of=False)
adv('fulgurite', 'storm_shard', 'fulgurite_shard', {'f': has_item('fulgurite_shard')}, 'Glass from the Sky', 'Find fulgurite where lightning struck sand')
adv('storm_cell', 'barometer', 'storm_cell', {'c': has_item('storm_cell')}, 'Captured Lightning', 'Collect a Storm Cell from a Storm Collector')
adv('stormwarden', 'storm_cell', 'tempest_core', {'k': killed('stormwarden')}, 'Eye of the Storm', 'Defeat the Stormwarden', frame='challenge')
adv('stormbreaker', 'stormwarden', 'stormbreaker', {'s': has_item('stormbreaker')}, 'Thunderclap', 'Forge the Stormbreaker', frame='goal')
adv('gale_wings', 'storm_cell', 'gale_wings', {'g': has_item('gale_wings')}, 'Riding the Gale', 'Craft a pair of Gale Wings')
adv('stormshard_armor', 'storm_shard', 'stormshard_chestplate', {p: has_item(f'stormshard_{p}') for p in ('helmet', 'chestplate', 'leggings', 'boots')},
    'Lightning Rod', 'Wear a full set of Stormshard armor', any_of=False)
adv('thunder_crab', 'crab_breeder', 'thunder_crab_spawn_egg', {'k': killed('thunder_crab')}, 'Shell Shock', 'Defeat a Thunder Crab')
adv('stormfin', 'root', 'stormfin', {'f': has_item('stormfin')}, 'Fishing in the Rain', 'Catch a Stormfin while it rains')
adv('lighthouse', 'root', 'storm_lantern', {'l': {'conditions': {'player': {'type': 'minecraft:entity_properties', 'entity': 'this',
    'predicate': {'minecraft:location': {'structures': 'stormreach:lighthouse'}}}}, 'trigger': 'minecraft:location'}},
    'Keeper of the Light', 'Find a Storm Lighthouse')
w(f'{A}/lang/en_us.json', lang)

# recipe-book unlocks for every recipe that doesn't have one yet
first_ing = {}
for f in glob.glob(f'{D}/recipe/*.json'):
    r = os.path.basename(f)[:-5]
    if os.path.exists(f'{ADV}/recipes/{r}.json'): continue
    d = json.load(open(f))
    ings = list(d.get('key', {}).values()) + d.get('ingredients', []) + ([d['ingredient']] if 'ingredient' in d else [])
    ours = [i for i in ings if i.startswith('stormreach:')] or [i for i in ings if not i.startswith('#')]
    trig = ours[0] if ours else ('stormreach:stormpine_log' if r == 'stormpine_planks' else None)
    if trig is None: print('no trigger', r); continue
    w(f'{ADV}/recipes/{r}.json', {'parent': 'minecraft:recipes/root', 'criteria': {
        'has_item': has_item(trig), 'has_the_recipe': {'conditions': {'recipes': f'stormreach:{r}'}, 'trigger': 'minecraft:recipe_unlocked'}},
        'requirements': [['has_the_recipe', 'has_item']], 'rewards': {'recipes': [f'stormreach:{r}']}})
print('lang/adv done', len(lang))
