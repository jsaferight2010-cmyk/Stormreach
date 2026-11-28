"""Stormreach 5.5 (Live Wire) data: the power and water blocks' models and blockstates, item definitions, language
(including the JEI pages), loot, recipes, tags and advancements. Runs after gendata11.py."""
import json
from gendata9 import (w, rj, ns, tag_add, item_def, gen_item, bmodel, el, shaped, adv, has, lang, L, A, D, MC)

B = 'stormreach:block/'
def model_item(name): w(f'{A}/models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'stormreach:item/{name}'}}); item_def(name, 'item/' + name)
def single(name, model=None): w(f'{A}/blockstates/{name}.json', {'variants': {'': {'model': B + (model or name)}}})

# Copper Cable: a core and an arm per connected side (multipart, like the chorus plant)
bmodel('copper_cable_core', {'parent': 'minecraft:block/block', 'textures': {'particle': B + 'copper_cable', 'c': B + 'copper_cable'},
                             'elements': [el([6, 6, 6], [10, 10, 10], '#c')]})
bmodel('copper_cable_side', {'parent': 'minecraft:block/block', 'textures': {'particle': B + 'copper_cable', 'c': B + 'copper_cable'},
                             'elements': [el([6.5, 6.5, 0], [9.5, 9.5, 6], '#c', faces='ewud')]})
parts = [{'apply': {'model': B + 'copper_cable_core'}}]
for d, rot in (('north', {}), ('east', {'y': 90}), ('south', {'y': 180}), ('west', {'y': 270}), ('up', {'x': 270}), ('down', {'x': 90})):
    parts.append({'when': {d: 'true'}, 'apply': {'model': B + 'copper_cable_side', 'uvlock': True, **rot}})
w(f'{A}/blockstates/copper_cable.json', {'multipart': parts})
model_item('copper_cable')

# Storm Capacitor: a steel base with three glass jars that light up as it charges
for lit in (False, True):
    jar = B + 'storm_capacitor_jar' + ('_lit' if lit else '')
    els = [el([0, 0, 0], [16, 3, 16], '#base')]
    for x, z in ((2, 2), (9, 2), (2, 9), (9, 9)):
        els += [el([x, 3, z], [x + 5, 13, z + 5], '#jar', shade=not lit), el([x + 2, 13, z + 2], [x + 3, 15, z + 3], '#base')]
    bmodel('storm_capacitor' + ('_lit' if lit else ''), {'parent': 'minecraft:block/block', 'textures': {'particle': B + 'storm_capacitor', 'base': B + 'storm_capacitor', 'jar': jar}, 'elements': els})
w(f'{A}/blockstates/storm_capacitor.json', {'variants': {f'level={i}': {'model': B + 'storm_capacitor' + ('_lit' if i else '')} for i in range(5)}})
model_item('storm_capacitor')

# Water Wheel: the axle housing; the wheel itself is drawn by its renderer
bmodel('water_wheel', {'parent': 'minecraft:block/block', 'textures': {'particle': B + 'water_wheel', 'h': B + 'water_wheel'},
                       'elements': [el([5, 5, 5], [11, 11, 11], '#h'), el([0, 7, 7], [16, 9, 9], '#h')]})
w(f'{A}/blockstates/water_wheel.json', {'variants': {f'axis={a}': {'model': B + 'water_wheel', **({'y': 90} if a == 'z' else {})} for a in ('x', 'z')}})
model_item('water_wheel')

# Arc Lamp: a caged bulb
for on in (False, True):
    bmodel(f"arc_lamp_{'on' if on else 'off'}", {'parent': 'minecraft:block/block', 'textures': {'particle': B + 'arc_lamp_off', 'l': B + f"arc_lamp_{'on' if on else 'off'}"},
           'elements': [el([3, 0, 3], [13, 14, 13], '#l', shade=not on), el([5, 14, 5], [11, 16, 11], '#l')]})
w(f'{A}/blockstates/arc_lamp.json', {'variants': {f'lit={v}': {'model': B + f"arc_lamp_{'on' if v == 'true' else 'off'}"} for v in ('false', 'true')}})
model_item('arc_lamp')

# Shock Plate: a 1-pixel grid on the floor
bmodel('shock_plate', {'parent': 'minecraft:block/block', 'textures': {'particle': B + 'shock_plate', 'p': B + 'shock_plate'}, 'elements': [el([0, 0, 0], [16, 1, 16], '#p')]})
single('shock_plate'); model_item('shock_plate')

# Electric Pump: a full block
bmodel('electric_pump', {'parent': 'minecraft:block/cube_bottom_top', 'textures': {'side': B + 'electric_pump', 'top': B + 'electric_pump_top', 'bottom': B + 'electric_pump_top'}})
single('electric_pump'); model_item('electric_pump')

# Sprinkler: a pipe with a brass head
bmodel('sprinkler', {'parent': 'minecraft:block/block', 'textures': {'particle': B + 'sprinkler', 's': B + 'sprinkler', 'p': B + 'copper_cable'},
                     'elements': [el([6.5, 0, 6.5], [9.5, 5, 9.5], '#p'), el([5, 5, 5], [11, 7, 11], '#s'), el([7, 7, 7], [9, 8, 9], '#s')]})
single('sprinkler'); model_item('sprinkler')

# Rain Barrel: an open barrel whose water level shows (water or Stormwater)
for lv in range(5):
    for storm in (False, True):
        els = [el([1, 0, 1], [15, 1, 15], '#in'), el([1, 1, 1], [15, 16, 2], '#side'), el([1, 1, 14], [15, 16, 15], '#side'),
               el([1, 1, 2], [2, 16, 14], '#side'), el([14, 1, 2], [15, 16, 14], '#side')]
        if lv: els.append(el([2, 1, 2], [14, 1 + lv * 3.5, 14], '#water', faces='u'))
        bmodel(f"rain_barrel_{lv}{'_storm' if storm else ''}", {'parent': 'minecraft:block/block',
               'textures': {'particle': B + 'rain_barrel', 'side': B + 'rain_barrel', 'in': B + 'rain_barrel_inside', 'water': B + ('tank_stormwater' if storm else 'tank_water')},
               'elements': els})
w(f'{A}/blockstates/rain_barrel.json', {'variants': {f'level={lv},storm={s}': {'model': B + f"rain_barrel_{lv}{'_storm' if s == 'true' else ''}"}
                                                     for lv in range(5) for s in ('false', 'true')}})
model_item('rain_barrel')

# Cistern: a stone tank with a grated top
bmodel('cistern', {'parent': 'minecraft:block/cube_bottom_top', 'textures': {'side': B + 'cistern', 'top': B + 'cistern_top', 'bottom': B + 'cistern'}})
single('cistern'); model_item('cistern')

gen_item('multimeter')

# ------------------------------------------------------------------------------------------- language
names = {
    'block.stormreach.copper_cable': 'Copper Cable', 'block.stormreach.storm_capacitor': 'Storm Capacitor', 'block.stormreach.water_wheel': 'Water Wheel',
    'block.stormreach.arc_lamp': 'Arc Lamp', 'block.stormreach.shock_plate': 'Shock Plate', 'block.stormreach.electric_pump': 'Electric Pump',
    'block.stormreach.sprinkler': 'Sprinkler', 'block.stormreach.rain_barrel': 'Rain Barrel', 'block.stormreach.cistern': 'Cistern',
    'item.stormreach.multimeter': 'Multimeter',
    'block.stormreach.tank.reading': '%s of %s buckets of %s', 'block.stormreach.tank.water': 'water', 'block.stormreach.tank.stormwater': 'Stormwater',
    'item.stormreach.multimeter.node': '%s · %s / %s SU · %s SU/t · %s other machines on the network',
    'item.stormreach.multimeter.role.generator': 'Generator', 'item.stormreach.multimeter.role.storage': 'Storage', 'item.stormreach.multimeter.role.consumer': 'Machine',
    'item.stormreach.multimeter.cable': 'Cable run: %s blocks · %s machines connected', 'item.stormreach.multimeter.nothing': 'No power or water here.',
    # JEI
    'jei.stormreach.power': 'Storm Power', 'jei.stormreach.water': 'Water Capture', 'jei.stormreach.catching': 'Catching and Gathering',
    'jei.stormreach.drops': 'Creature Drops', 'jei.stormreach.drops.row': '%s: %s (%s%%)',
    'jei.stormreach.power.turbine': 'Makes 2 SU a tick per level of wind, up to 30 in a gale. Needs open sky.',
    'jei.stormreach.power.wheel': 'Makes 4 SU a tick for each side or the bottom with moving water against it, up to 16.',
    'jei.stormreach.power.wheel_storm': 'Moving Stormwater doubles a Water Wheel\'s output, up to 32 SU a tick.',
    'jei.stormreach.power.capacitor': 'Stores 100,000 SU. Lightning striking within 24 blocks fills half of it at once. Feeds machines on its network.',
    'jei.stormreach.power.lamp': 'Uses 1 SU a tick. Light level 15 while powered.',
    'jei.stormreach.power.plate': 'Uses 100 SU per shock: 4 damage to a hostile mob standing on it, up to twice a second.',
    'jei.stormreach.power.pump': 'Uses 80 SU to lift 250 mB from the water source beneath it into a tank beside or above it.',
    'jei.stormreach.power.sprinkler': 'Uses 80 SU and 50 mB from a tank it sits on or beside every 2 seconds to water a 9x9 area.',
    'jei.stormreach.power.tesla': 'Draws 20 SU per point of charge from the network.',
    'jei.stormreach.power.tesla_cell': 'A Storm Cell charges a Tesla Coil halfway. Lightning within 24 blocks does too.',
    'jei.stormreach.water.rain': 'A Rain Barrel under open sky catches 25 mB of rain a second.',
    'jei.stormreach.water.thunder': 'In a thunderstorm it catches 60 mB a second, as Stormwater.',
    'jei.stormreach.water.drain': 'A Rain Barrel set on a Cistern drains into it, 250 mB a second.',
    'jei.stormreach.water.pump': 'An Electric Pump lifts water from below. Ponds, rivers and the sea never run dry; a lone source does.',
    'jei.stormreach.water.bucket': 'Use a bucket on a Rain Barrel or Cistern holding a bucket or more.',
    'jei.stormreach.water.storm_bucket': 'A tank holding Stormwater fills buckets with Stormwater.',
    'jei.stormreach.water.bottle': 'Use a glass bottle on a tank for a water bottle (250 mB).',
    'jei.stormreach.water.pour': 'Pour water or Stormwater buckets into a tank.',
    'jei.stormreach.water.sprinkler': 'Keeps farmland wet, gives crops and saplings an extra growth tick (two with Stormwater), and puts out fires.',
    'jei.stormreach.water.wheel': 'Moving water against a Water Wheel makes power.',
    'jei.stormreach.catch.stormfly': 'Use a glass bottle on a Stormfly. They swarm before and during storms.',
    'jei.stormreach.catch.lightning': 'Hold an empty glass bottle within 8 blocks of a lightning strike.',
    'jei.stormreach.catch.dust_devil': 'Use a glass bottle on a Dust Devil (deserts, badlands and savannas on clear days).',
    'jei.stormreach.catch.collector': 'A Storm Collector under open sky charges in rain and storms. When full, take a Storm Cell with an empty hand.',
    'jei.stormreach.catch.gale_sprite': 'Give a Gale Sprite Thistle Down and it gives you a Gale Essence.',
    'jei.stormreach.catch.sparkskink': 'A storm-charged Sparkskink sheds a Charged Scale every few minutes.',
    'jei.stormreach.catch.whale': 'A Cloud Whale leaves a lump of Cloud Ambergris every five to ten minutes.',
    'jei.stormreach.catch.moth': 'Aurora Moths shed Aurora Dust at night.',
    'jei.stormreach.catch.gull': 'Storm Gulls lay eggs every five to ten minutes.',
    'jei.stormreach.catch.barrel_bottle': 'Any Rain Barrel or Cistern with water fills glass bottles.',
}
for k, v in names.items(): L(k, v)

# ------------------------------------------------------------------------------------------- loot, recipes, tags
sample = rj(f'{D}/loot_table/blocks/nacre_block.json')
for b in ('copper_cable', 'storm_capacitor', 'water_wheel', 'arc_lamp', 'shock_plate', 'electric_pump', 'sprinkler', 'rain_barrel', 'cistern'):
    w(f'{D}/loot_table/blocks/{b}.json', json.loads(json.dumps(sample).replace('nacre_block', b)))
shaped('copper_cable', ['KKK', 'CCC', 'KKK'], {'K': 'minecraft:dried_kelp', 'C': 'minecraft:copper_ingot'}, 'copper_cable', 8, 'redstone')
shaped('storm_capacitor', ['ICI', 'GSG', 'ICI'], {'I': 'minecraft:iron_ingot', 'C': 'minecraft:copper_ingot', 'G': 'minecraft:glass', 'S': 'storm_cell'}, 'storm_capacitor', 1, 'redstone')
shaped('water_wheel', ['PSP', 'SIS', 'PSP'], {'P': '#minecraft:planks', 'S': 'minecraft:stick', 'I': 'minecraft:iron_ingot'}, 'water_wheel', 1, 'redstone')
shaped('arc_lamp', [' N ', 'NGN', ' C '], {'N': 'minecraft:iron_nugget', 'G': 'minecraft:glowstone', 'C': 'copper_cable'}, 'arc_lamp', 2, 'redstone')
shaped('shock_plate', ['CWC'], {'C': 'minecraft:copper_ingot', 'W': 'copper_cable'}, 'shock_plate', 1, 'redstone')
shaped('electric_pump', ['ICI', 'PBP', 'ICI'], {'I': 'minecraft:iron_ingot', 'C': 'copper_cable', 'P': 'minecraft:piston', 'B': 'minecraft:bucket'}, 'electric_pump', 1, 'redstone')
shaped('sprinkler', ['NIN', ' C '], {'N': 'minecraft:iron_nugget', 'I': 'minecraft:iron_ingot', 'C': 'copper_cable'}, 'sprinkler', 1, 'redstone')
shaped('rain_barrel', ['P P', 'P P', 'PSP'], {'P': '#minecraft:planks', 'S': '#minecraft:wooden_slabs'}, 'rain_barrel', 1, 'misc')
shaped('cistern', ['S S', 'SBS', 'SSS'], {'S': 'minecraft:stone_bricks', 'B': 'minecraft:bucket'}, 'cistern', 1, 'misc')
shaped('multimeter', [' G ', 'RCR', ' I '], {'G': 'minecraft:glass_pane', 'R': 'minecraft:redstone', 'C': 'minecraft:copper_ingot', 'I': 'minecraft:iron_ingot'}, 'multimeter', 1, 'equipment')
tag_add(f'{MC}/block/mineable/pickaxe.json', ['stormreach:copper_cable', 'stormreach:storm_capacitor', 'stormreach:arc_lamp', 'stormreach:shock_plate',
                                              'stormreach:electric_pump', 'stormreach:sprinkler', 'stormreach:cistern'])
tag_add(f'{MC}/block/mineable/axe.json', ['stormreach:water_wheel', 'stormreach:rain_barrel'])

adv('storm_capacitor', 'meteorology', 'storm_capacitor', {'c': has('storm_capacitor')}, 'Live Wire', 'Build a Storm Capacitor to store the storm\'s power')
adv('cistern', 'meteorology', 'cistern', {'c': has('cistern')}, 'Every Drop Counts', 'Build a Cistern to store captured rain')
adv('sprinkler', 'cistern', 'sprinkler', {'s': has('sprinkler')}, 'Green Thumb', 'Build a Sprinkler to water your crops')

with open(f'{A}/lang/en_us.json', 'w') as f: json.dump(lang, f, indent=2, ensure_ascii=False); f.write('\n')
print('gendata12: Live Wire data written')
