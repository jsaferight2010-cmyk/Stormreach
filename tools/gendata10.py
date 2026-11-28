"""Stormreach 5.3 (Skyward) data: models, blockstates, item definitions, the Stormchaser equipment asset, language, loot,
recipes, tags, spawns and advancements for the Skyward content. Runs after gendata9.py (it reads and extends the lang)."""
import json, os
from gendata9 import (w, rj, ns, tag_add, item_def, gen_item, bmodel, cube, el, self_drop, mob_loot, pool, shaped, shapeless,
                      add_spawns, cnt, adv, has, killed, CODE, lang, L, A, D, MC, VA)

# ------------------------------------------------------------------------------------------- items
for n in ('serpent_scale', 'squall_heart', 'glacial_core', 'aurora_dust', 'bottled_stormfly', 'bottled_lightning', 'cloud_seeder',
          'supercell_seeder', 'chasers_coffee', 'snow_cone', 'stormchaser_goggles', 'stormchaser_jacket', 'stormchaser_trousers', 'stormchaser_boots'):
    gen_item(n)
w(f'{A}/models/item/gale_blade.json', {'parent': 'minecraft:item/handheld', 'textures': {'layer0': 'stormreach:item/gale_blade'}})
item_def('gale_blade', 'item/gale_blade')
for m in ('squall_serpent', 'hailstone_golem', 'aurora_moth', 'stormfly'):
    gen_item(f'{m}_spawn_egg')
w(f'{A}/equipment/stormchaser.json', {'layers': {'humanoid': [{'texture': 'stormreach:stormchaser'}], 'humanoid_baby': [{'texture': 'stormreach:stormchaser'}],
                                                 'humanoid_leggings': [{'texture': 'stormreach:stormchaser'}]}})

# ------------------------------------------------------------------------------------------- blocks
cube('serpentscale_block')
cube('glacial_lamp')
bmodel('aurora_glass', {'parent': 'minecraft:block/cube_all', 'textures': {'all': {'force_translucent': True, 'sprite': 'stormreach:block/aurora_glass'}}})
w(f'{A}/blockstates/aurora_glass.json', {'variants': {'': {'model': 'stormreach:block/aurora_glass'}}})
item_def('aurora_glass', 'block/aurora_glass')
# Storm Shelter Hatch: vanilla iron trapdoor shapes and state layout, our texture
for part in ('bottom', 'top', 'open'):
    bmodel(f'storm_shelter_hatch_{part}', {'parent': f'minecraft:block/template_trapdoor_{part}', 'textures': {'texture': 'stormreach:block/storm_shelter_hatch'}})
td = rj(f'{VA}/blockstates/iron_trapdoor.json')
w(f'{A}/blockstates/storm_shelter_hatch.json', json.loads(json.dumps(td).replace('minecraft:block/iron_trapdoor', 'stormreach:block/storm_shelter_hatch')))
item_def('storm_shelter_hatch', 'block/storm_shelter_hatch_bottom')
# Wind Turbine: a foot, a white mast and the nacelle; the rotor is drawn by its block-entity renderer
TT = {'particle': 'stormreach:block/wind_turbine', 'mast': 'stormreach:block/wind_turbine', 'base': 'stormreach:block/wind_turbine_base',
      'nacelle': 'stormreach:block/wind_turbine_nacelle'}
bmodel('wind_turbine', {'parent': 'minecraft:block/block', 'textures': TT, 'elements': [
    el([4, 0, 4], [12, 2, 12], '#base'), el([5.5, 2, 5.5], [10.5, 3, 10.5], '#base'),
    el([7, 3, 7], [9, 15, 9], '#mast', uv=[7, 0, 9, 12]),
    el([6.5, 15, 2.5], [9.5, 18.5, 11.5], '#nacelle'), el([7, 15.5, 11.5], [9, 18, 13.5], '#nacelle'),
    el([7.5, 18.5, 9], [8.5, 20, 10], '#base')]})
w(f'{A}/blockstates/wind_turbine.json', {'variants': {f'facing={f}': {'model': 'stormreach:block/wind_turbine', **({'y': r} if r else {})}
                                                      for f, r in (('north', 0), ('east', 90), ('south', 180), ('west', 270))}})
w(f'{A}/models/item/wind_turbine.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'stormreach:item/wind_turbine'}})
item_def('wind_turbine', 'item/wind_turbine')
# Stormfly Jar: a glass jar with a copper lid and glowing flies inside, standing or hanging from a chain loop
JT = {'particle': 'stormreach:block/stormfly_jar_lid', 'glass': {'force_translucent': True, 'sprite': 'stormreach:block/stormfly_jar'},
      'inside': 'stormreach:block/stormfly_jar_inside', 'lid': 'stormreach:block/stormfly_jar_lid', 'chain': 'minecraft:block/iron_chain'}
def jar(y0, hanging):
    els = [el([6, y0 + 0.5, 6], [10, y0 + 6.5, 10], '#inside', shade=False),
           el([5, y0, 5], [11, y0 + 8, 11], '#glass'),
           el([5.5, y0 + 8, 5.5], [10.5, y0 + 9, 10.5], '#lid'), el([6.5, y0 + 9, 6.5], [9.5, y0 + 9.5, 9.5], '#lid')]
    if hanging:
        els += [{'from': [8, y0 + 9.5, 6.5], 'to': [8, y0 + 15.5, 9.5], 'faces': {'east': {'texture': '#chain', 'uv': [0, 0, 3, 6]}, 'west': {'texture': '#chain', 'uv': [0, 0, 3, 6]}},
                 'rotation': {'origin': [8, 8, 8], 'axis': 'y', 'angle': 45}},
                {'from': [6.5, y0 + 9.5, 8], 'to': [9.5, y0 + 15.5, 8], 'faces': {'north': {'texture': '#chain', 'uv': [3, 0, 6, 6]}, 'south': {'texture': '#chain', 'uv': [3, 0, 6, 6]}},
                 'rotation': {'origin': [8, 8, 8], 'axis': 'y', 'angle': 45}}]
    return els
bmodel('stormfly_jar', {'parent': 'minecraft:block/block', 'textures': JT, 'elements': jar(0, False)})
bmodel('stormfly_jar_hanging', {'parent': 'minecraft:block/block', 'textures': JT, 'elements': jar(1, True)})
w(f'{A}/blockstates/stormfly_jar.json', {'variants': {'hanging=false': {'model': 'stormreach:block/stormfly_jar'},
                                                      'hanging=true': {'model': 'stormreach:block/stormfly_jar_hanging'}}})
w(f'{A}/models/item/stormfly_jar.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'stormreach:item/stormfly_jar'}})
item_def('stormfly_jar', 'item/stormfly_jar')

# ------------------------------------------------------------------------------------------- language
names = {
    'item.stormreach.serpent_scale': 'Serpent Scale', 'item.stormreach.squall_heart': 'Squall Heart', 'item.stormreach.glacial_core': 'Glacial Core',
    'item.stormreach.aurora_dust': 'Aurora Dust', 'item.stormreach.bottled_stormfly': 'Bottled Stormfly', 'item.stormreach.bottled_lightning': 'Bottled Lightning',
    'item.stormreach.cloud_seeder': 'Cloud Seeder', 'item.stormreach.supercell_seeder': 'Supercell Seeder',
    'item.stormreach.gale_blade': 'Gale Blade', 'item.stormreach.chasers_coffee': "Chaser's Coffee", 'item.stormreach.snow_cone': 'Snow Cone',
    'item.stormreach.stormchaser_goggles': 'Stormchaser Goggles', 'item.stormreach.stormchaser_jacket': 'Stormchaser Jacket',
    'item.stormreach.stormchaser_trousers': 'Stormchaser Trousers', 'item.stormreach.stormchaser_boots': 'Stormchaser Boots',
    'block.stormreach.wind_turbine': 'Wind Turbine', 'block.stormreach.storm_shelter_hatch': 'Storm Shelter Hatch', 'block.stormreach.stormfly_jar': 'Stormfly Jar',
    'block.stormreach.aurora_glass': 'Aurora Glass', 'block.stormreach.serpentscale_block': 'Serpentscale Block', 'block.stormreach.glacial_lamp': 'Glacial Lamp',
    'entity.stormreach.squall_serpent': 'Squall Serpent', 'entity.stormreach.hailstone_golem': 'Hailstone Golem', 'entity.stormreach.aurora_moth': 'Aurora Moth',
    'entity.stormreach.stormfly': 'Stormfly', 'entity.stormreach.bottled_lightning': 'Bottled Lightning',
    # tooltips
    'item.stormreach.squall_heart.tooltip': 'The still eye of a Squall Serpent. A Gale Blade is forged around one.',
    'item.stormreach.glacial_core.tooltip': 'The cold heart of a Hailstone Golem. It never melts.',
    'item.stormreach.aurora_dust.tooltip': 'Shed by Aurora Moths at night. Makes Aurora Glass.',
    'item.stormreach.bottled_stormfly.tooltip': 'A glass bottle used on a Stormfly. Put it in a jar for a lamp.',
    'item.stormreach.bottled_lightning.tooltip': 'Caught by holding an empty bottle near a strike. Throw it to call the bolt back down.',
    'item.stormreach.cloud_seeder.tooltip': 'Fire outdoors to bring rain.',
    'item.stormreach.supercell_seeder.tooltip': 'Fire outdoors to bring a thunderstorm, and everything that comes with one.',
    'item.stormreach.gale_blade.tooltip': 'Hits throw the target skyward. Use: ride an updraft (5 s cooldown).',
    'item.stormreach.stormchaser_goggles.tooltip': 'Heads-up readout: the nearest tornado, or the wind and pressure. Full suit: immune to lightning.',
    'item.stormreach.stormchaser_jacket.tooltip': 'Braced against the wind: tornadoes push you a third as hard. Full suit: immune to lightning.',
    'item.stormreach.stormchaser_trousers.tooltip': 'Speed I in the rain. Full suit: immune to lightning.',
    'item.stormreach.stormchaser_boots.tooltip': 'Speed I in the rain. Full suit: immune to lightning.',
    # messages
    'radio.stormreach.emergency': '⚠ TORNADO EMERGENCY — TAKE SHELTER NOW',
    'radio.stormreach.tornado.detail': 'A %s tornado is on the ground %s blocks to the %s, moving %s at %s mph.',
    'radio.stormreach.severe': 'SEVERE THUNDERSTORM WARNING', 'radio.stormreach.severe.detail': 'Lightning, damaging winds and large hail possible. Get indoors.',
    'radio.stormreach.statement': 'SPECIAL WEATHER STATEMENT', 'radio.stormreach.rain.detail': 'Rain moving in.',
    'radio.stormreach.clear': 'ALL CLEAR', 'radio.stormreach.clear.detail': 'The storm has passed.',
    'radio.stormreach.thunderbird': 'WILDLIFE ADVISORY', 'radio.stormreach.thunderbird.detail': 'A Thunderbird is reported in your area. Avoid open high ground.',
    'radio.stormreach.forecast': 'Forecast: %s', 'radio.stormreach.conditions': 'Wind %s mph gusting %s from the %s · %s hPa · storm risk: %s',
    'item.stormreach.cloud_seeder.no_sky': 'Needs open sky.', 'item.stormreach.cloud_seeder.already': 'The sky is already doing that.',
    'item.stormreach.cloud_seeder.done': 'The clouds thicken. Rain is on its way.',
    'item.stormreach.supercell_seeder.done': 'The sky boils over. A supercell is building.',
    'item.stormreach.stormchaser_goggles.clear': 'Wind %s mph from the %s · %s hPa',
    'item.stormreach.stormchaser_goggles.tornado': '◉ %s tornado · %s m %s · peak %s mph',
    'block.stormreach.wind_turbine.reading': 'Wind %s mph, gusting %s · output %s/15',
    'item.stormreach.bottled_lightning.caught': 'You caught the lightning!',
}
for k, v in names.items(): L(k, v)
for m, t in (('squall_serpent', 'Squall Serpent'), ('hailstone_golem', 'Hailstone Golem'), ('aurora_moth', 'Aurora Moth'), ('stormfly', 'Stormfly')):
    L(f'item.stormreach.{m}_spawn_egg', f'{t} Spawn Egg')

# ------------------------------------------------------------------------------------------- loot
sample = f'{D}/loot_table/blocks/nacre_block.json'
for b in ('wind_turbine', 'storm_shelter_hatch', 'stormfly_jar', 'serpentscale_block', 'glacial_lamp'):
    w(f'{D}/loot_table/blocks/{b}.json', json.loads(json.dumps(rj(sample)).replace('nacre_block', b)))
# Aurora Glass drops nothing without Silk Touch, like glass
glass = rj(f'{D}/loot_table/blocks/nacre_block.json')
gp = glass['pools'][0]
gp.pop('conditions', None); gp.pop('condition', None)
gp['condition'] = {'type': 'minecraft:match_tool', 'predicate': {'predicates': {'minecraft:enchantments': [{'enchantments': 'minecraft:silk_touch', 'levels': {'min': 1}}]}}}
w(f'{D}/loot_table/blocks/aurora_glass.json', json.loads(json.dumps(glass).replace('nacre_block', 'aurora_glass')))
mob_loot('squall_serpent', [pool('serpent_scale', 3, 6), pool('squall_heart', 1, 1, looting=False)])
mob_loot('hailstone_golem', [pool('glacial_core', 1, 1, looting=False, chance=0.5), pool('minecraft:packed_ice', 1, 3), pool('minecraft:snowball', 2, 5)])
mob_loot('aurora_moth', [pool('aurora_dust', 1, 2)])
mob_loot('stormfly', [pool('minecraft:glowstone_dust', 0, 1)])
mob_loot('bottled_lightning', [])

# ------------------------------------------------------------------------------------------- recipes
shaped('serpentscale_block', ['SS', 'SS'], {'S': 'serpent_scale'}, 'serpentscale_block', 1, 'building')
shaped('glacial_lamp', [' I ', 'ICI', ' I '], {'I': 'minecraft:packed_ice', 'C': 'glacial_core'}, 'glacial_lamp', 4, 'building')
shaped('aurora_glass', ['GGG', 'GDG', 'GGG'], {'G': 'minecraft:glass', 'D': 'aurora_dust'}, 'aurora_glass', 8, 'building')
shaped('stormfly_jar', [' L ', 'GBG', 'GGG'], {'L': 'minecraft:copper_ingot', 'G': 'minecraft:glass_pane', 'B': 'bottled_stormfly'}, 'stormfly_jar', 1, 'building')
shaped('storm_shelter_hatch', ['IOI', 'III'], {'I': 'minecraft:iron_ingot', 'O': 'minecraft:orange_dye'}, 'storm_shelter_hatch', 2, 'redstone')
shaped('wind_turbine', ['WAW', ' I ', ' R '], {'W': 'minecraft:white_wool', 'A': 'anemometer', 'I': 'minecraft:iron_ingot', 'R': 'minecraft:redstone'},
       'wind_turbine', 1, 'redstone')
shapeless('cloud_seeder', ['minecraft:firework_rocket', 'bottled_rain', 'minecraft:snowball'], 'cloud_seeder', 2)
shapeless('supercell_seeder', ['cloud_seeder', 'bottled_lightning', 'minecraft:gunpowder'], 'supercell_seeder', 1)
shaped('gale_blade', ['S', 'H', 'B'], {'S': 'serpent_scale', 'H': 'squall_heart', 'B': 'minecraft:breeze_rod'}, 'gale_blade', 1, 'equipment')
shapeless('chasers_coffee', ['minecraft:cocoa_beans', 'minecraft:cocoa_beans', 'minecraft:sugar', 'minecraft:glass_bottle'], 'chasers_coffee', 1, 'misc')
shapeless('snow_cone', ['minecraft:snowball', 'minecraft:snowball', 'minecraft:sugar', 'minecraft:sweet_berries', 'minecraft:paper'], 'snow_cone', 2, 'misc')
shaped('stormchaser_goggles', ['LLL', 'GCG'], {'L': 'minecraft:leather', 'G': 'minecraft:light_blue_stained_glass_pane', 'C': 'minecraft:copper_ingot'},
       'stormchaser_goggles', 1, 'equipment')
shaped('stormchaser_jacket', ['L L', 'OGO', 'LLL'], {'L': 'minecraft:leather', 'O': 'minecraft:orange_dye', 'G': 'minecraft:glow_ink_sac'},
       'stormchaser_jacket', 1, 'equipment')
shaped('stormchaser_trousers', ['LLL', 'O O', 'L L'], {'L': 'minecraft:leather', 'O': 'minecraft:orange_dye'}, 'stormchaser_trousers', 1, 'equipment')
shaped('stormchaser_boots', ['L L', 'S S'], {'L': 'minecraft:leather', 'S': 'minecraft:slime_ball'}, 'stormchaser_boots', 1, 'equipment')

# ------------------------------------------------------------------------------------------- tags
tag_add(f'{D}/tags/item/repairs_stormchaser.json', ['minecraft:leather'])
tag_add(f'{D}/tags/item/repairs_gale_blade.json', ['stormreach:serpent_scale'])
tag_add(f'{MC}/block/mineable/pickaxe.json', ['stormreach:wind_turbine', 'stormreach:storm_shelter_hatch', 'stormreach:stormfly_jar',
                                              'stormreach:serpentscale_block', 'stormreach:glacial_lamp'])
tag_add(f'{MC}/block/trapdoors.json', ['stormreach:storm_shelter_hatch'])
tag_add(f'{MC}/item/trapdoors.json', ['stormreach:storm_shelter_hatch'])
tag_add(f'{MC}/item/swords.json', ['stormreach:gale_blade'])
tag_add(f'{MC}/item/head_armor.json', ['stormreach:stormchaser_goggles'])
tag_add(f'{MC}/item/chest_armor.json', ['stormreach:stormchaser_jacket'])
tag_add(f'{MC}/item/leg_armor.json', ['stormreach:stormchaser_trousers'])
tag_add(f'{MC}/item/foot_armor.json', ['stormreach:stormchaser_boots'])
tag_add(f'{MC}/item/dyeable.json', [])

# ------------------------------------------------------------------------------------------- spawns
for b in ('cloudsea_expanse', 'aurora_spires', 'galewood_highlands'):
    add_spawns(b, 'monster', [cnt('stormreach:squall_serpent', 1, 1, 2)])
for b in ('aurora_spires', 'galewood_highlands', 'elderstorm_grove', 'stormreach_coast', 'squallwind_bluffs'):
    add_spawns(b, 'ambient', [cnt('stormreach:aurora_moth', 1, 3, 10)])

# ------------------------------------------------------------------------------------------- advancements
adv('radio_warning', 'meteorology', 'weather_radio', CODE, 'This Is Not a Test', 'Carry a Weather Radio while a tornado is on the ground within 400 blocks')
adv('cloud_seeder', 'meteorology', 'cloud_seeder', CODE, 'Rainmaker', 'Fire a Cloud Seeder and make it rain')
adv('supercell_seeder', 'cloud_seeder', 'supercell_seeder', CODE, 'Playing God', 'Fire a Supercell Seeder and start a thunderstorm', frame='goal')
adv('stormchaser_suit', 'meteorology', 'stormchaser_jacket', CODE, 'Suited Up', 'Wear the full Stormchaser suit')
adv('bottled_lightning', 'root', 'bottled_lightning', {'b': has('bottled_lightning')}, 'Lightning in a Bottle', 'Catch a lightning strike in an empty bottle',
    frame='goal')
adv('stormfly_jar', 'root', 'bottled_stormfly', {'b': has('bottled_stormfly')}, 'Jar of Sparks', 'Catch a Stormfly in a bottle')
adv('hailstone_golem', 'root', 'glacial_core', {'k': killed('hailstone_golem')}, 'Thaw', 'Defeat a Hailstone Golem')
adv('squall_serpent', 'root', 'squall_heart', {'k': killed('squall_serpent')}, 'Serpent of the Squall', 'Slay a Squall Serpent in the Tempest Realm',
    frame='challenge', xp=150)
adv('gale_blade', 'squall_serpent', 'gale_blade', {'g': has('gale_blade')}, 'Riding the Gale', 'Forge a Gale Blade around a Squall Heart')

with open(f'{A}/lang/en_us.json', 'w') as f: json.dump(lang, f, indent=2, ensure_ascii=False); f.write('\n')
print('gendata10: Skyward data written')
