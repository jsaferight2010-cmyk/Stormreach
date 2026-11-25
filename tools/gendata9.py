"""Stormreach 5.2 (Stormwild) data: the new wildlife and storm-chasing content: models, blockstates, item definitions,
language, loot, recipes, tags, biome spawns and advancements. Runs after gendata8.py (it reads and extends its lang)."""
import os as _os, pathlib as _pl, json, os, zipfile
ROOT = str(_pl.Path(__file__).resolve().parent.parent)
VANILLA = _os.environ.get('STORMREACH_VANILLA', str(_pl.Path(ROOT) / '.vanilla'))
if not _os.path.isdir(VANILLA) and _os.path.isdir('/home/claude/mc/vd'): VANILLA = '/home/claude/mc/vd'
R = ROOT + '/src/main/resources'
A = f'{R}/assets/stormreach'; D = f'{R}/data/stormreach'; NS = 'stormreach'
VA = f'{VANILLA}/assets/minecraft'; MC = f'{R}/data/minecraft/tags'; WG = f'{D}/worldgen'


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f: json.dump(obj, f, indent=2); f.write('\n')
def rj(p): return json.load(open(p))
def ns(x): return x if ':' in x or x.startswith('#') else f'{NS}:{x}'
def tag_add(path, values):
    old = rj(path)['values'] if os.path.exists(path) else []
    ids = {v if isinstance(v, str) else v['id'] for v in old}
    w(path, {'values': old + [v for v in values if (v if isinstance(v, str) else v['id']) not in ids]})
lang = rj(f'{A}/lang/en_us.json')
def L(k, v): lang[k] = v


# ------------------------------------------------------------------------------------------- models and item defs
def item_def(name, model): w(f'{A}/items/{name}.json', {'model': {'type': 'minecraft:model', 'model': ns(model)}})
def gen_item(name):
    w(f'{A}/models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'stormreach:item/{name}'}})
    item_def(name, 'item/' + name)
def bmodel(name, obj): w(f'{A}/models/block/{name}.json', obj)
def cube(name):
    bmodel(name, {'parent': 'minecraft:block/cube_all', 'textures': {'all': f'stormreach:block/{name}'}})
    w(f'{A}/blockstates/{name}.json', {'variants': {'': {'model': f'stormreach:block/{name}'}}})
    item_def(name, 'block/' + name)
def el(frm, to, tex, faces='nsewud', uv=None, shade=True):
    f = {}
    for k, d in zip('nsewud', ('north', 'south', 'east', 'west', 'up', 'down')):
        if k in faces: f[d] = {'texture': tex, **({'uv': uv} if uv else {})}
    e = {'from': frm, 'to': to, 'faces': f}
    if not shade: e['shade'] = False
    return e


for n in ('cloud_ambergris', 'glowgel', 'voltaic_eel', 'cooked_voltaic_eel', 'charged_scale', 'gale_essence', 'bottled_gale', 'pearlescent_shell',
          'tide_conch', 'thunderbird_plume', 'stormkite', 'intercept_vehicle', 'weather_balloon', 'chasers_logbook'):
    gen_item(n)
for m in ('cloud_whale', 'shock_eel', 'sparkskink', 'storm_jelly', 'gale_sprite', 'tidewrack_hermit', 'thunderbird'):
    gen_item(f'{m}_spawn_egg')
for b in ('nacre_block', 'nacre_bricks'):
    cube(b)
# Glowgel block: a translucent jelly shell around a smaller glowing core, like slime
bmodel('glowgel_block', {'parent': 'minecraft:block/block', 'textures': {'particle': 'stormreach:block/glowgel_block', 'all': 'stormreach:block/glowgel_block'},
                         'elements': [el([3, 3, 3], [13, 13, 13], '#all', uv=[3, 3, 13, 13], shade=False), el([0, 0, 0], [16, 16, 16], '#all')]})
w(f'{A}/blockstates/glowgel_block.json', {'variants': {'': {'model': 'stormreach:block/glowgel_block'}}})
item_def('glowgel_block', 'block/glowgel_block')
# Skylight Lantern: vanilla's lantern shapes, our texture
for kind in ('lantern', 'hanging_lantern'):
    bmodel(f'skylight_{kind}', {'parent': f'minecraft:block/template_{kind}', 'textures': {'lantern': 'stormreach:block/skylight_lantern'}})
w(f'{A}/blockstates/skylight_lantern.json', {'variants': {
    'hanging=false': {'model': 'stormreach:block/skylight_lantern'}, 'hanging=true': {'model': 'stormreach:block/skylight_hanging_lantern'}}})
w(f'{A}/models/item/skylight_lantern.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'stormreach:item/skylight_lantern'}})
item_def('skylight_lantern', 'item/skylight_lantern')
# Rain gauge: a glass tube on a metal foot, with water rising inside in 8 steps
GT = {'particle': 'stormreach:block/rain_gauge_base', 'glass': 'stormreach:block/rain_gauge', 'water': 'stormreach:block/rain_gauge_water',
      'base': 'stormreach:block/rain_gauge_base'}
variants = {}
for lv in range(9):
    els = [el([5, 0, 5], [11, 1, 11], '#base'), el([5.5, 13, 5.5], [10.5, 14, 10.5], '#base'),
           el([6, 1, 6], [10, 13, 10], '#glass', faces='nsew', uv=[0, 0, 16, 16])]
    if lv: els.append(el([6.3, 1, 6.3], [9.7, 1 + lv * 1.45, 9.7], '#water', faces='nsewu'))
    bmodel(f'rain_gauge_{lv}', {'parent': 'minecraft:block/block', 'textures': GT, 'elements': els})
    variants[f'level={lv}'] = {'model': f'stormreach:block/rain_gauge_{lv}'}
w(f'{A}/blockstates/rain_gauge.json', {'variants': variants})
w(f'{A}/models/item/rain_gauge.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'stormreach:item/rain_gauge'}})
item_def('rain_gauge', 'item/rain_gauge')
# Tornado probe: a squat orange instrument barrel with a lid, a light and a little anemometer
PT = {'particle': 'stormreach:block/tornado_probe', 'side': 'stormreach:block/tornado_probe', 'top': 'stormreach:block/tornado_probe_top',
      'metal': 'stormreach:block/rain_gauge_base'}
for rec in (False, True):
    t = dict(PT); t['light'] = f"stormreach:block/tornado_probe_light_{'on' if rec else 'off'}"
    els = [el([3, 0, 3], [13, 9, 13], '#side', faces='nsewd'), {'from': [3, 0, 3], 'to': [13, 9, 13], 'faces': {'up': {'texture': '#top'}}},
           el([2.5, 9, 2.5], [13.5, 10, 13.5], '#metal'), el([2, 1, 2], [3, 2, 14], '#metal'), el([13, 1, 2], [14, 2, 14], '#metal'),
           el([10, 10, 4], [12, 11.5, 6], '#light'), el([7.5, 10, 7.5], [8.5, 14, 8.5], '#metal'),
           el([5.5, 14, 7.5], [10.5, 14.6, 8.5], '#metal'), el([7.5, 14, 5.5], [8.5, 14.6, 10.5], '#metal'),
           el([5, 13.6, 7], [6, 14.8, 9], '#side'), el([10, 13.6, 7], [11, 14.8, 9], '#side'), el([7, 13.6, 5], [9, 14.8, 6], '#side'), el([7, 13.6, 10], [9, 14.8, 11], '#side')]
    bmodel(f"tornado_probe{'_recorded' if rec else ''}", {'parent': 'minecraft:block/block', 'textures': t, 'elements': els})
var = {}
for facing, rot in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
    for rec in ('false', 'true'):
        var[f'facing={facing},recorded={rec}'] = {'model': f"stormreach:block/tornado_probe{'_recorded' if rec == 'true' else ''}", **({'y': rot} if rot else {})}
w(f'{A}/blockstates/tornado_probe.json', {'variants': var})
item_def('tornado_probe', 'block/tornado_probe')

# ------------------------------------------------------------------------------------------- language
names = {
    'item.stormreach.cloud_ambergris': 'Cloud Ambergris', 'item.stormreach.glowgel': 'Glowgel', 'item.stormreach.voltaic_eel': 'Voltaic Eel',
    'item.stormreach.cooked_voltaic_eel': 'Cooked Voltaic Eel', 'item.stormreach.charged_scale': 'Charged Scale', 'item.stormreach.gale_essence': 'Gale Essence',
    'item.stormreach.bottled_gale': 'Bottled Gale', 'item.stormreach.pearlescent_shell': 'Pearlescent Shell', 'item.stormreach.tide_conch': 'Tide Conch',
    'item.stormreach.thunderbird_plume': 'Thunderbird Plume', 'item.stormreach.stormkite': 'Stormkite',
    'item.stormreach.intercept_vehicle': 'Tornado Intercept Vehicle', 'item.stormreach.weather_balloon': 'Weather Balloon',
    'item.stormreach.chasers_logbook': "Storm Chaser's Logbook",
    'block.stormreach.glowgel_block': 'Glowgel Block', 'block.stormreach.skylight_lantern': 'Skylight Lantern', 'block.stormreach.nacre_block': 'Nacre Block',
    'block.stormreach.nacre_bricks': 'Nacre Bricks', 'block.stormreach.rain_gauge': 'Rain Gauge', 'block.stormreach.tornado_probe': 'Tornado Probe',
    'entity.stormreach.cloud_whale': 'Cloud Whale', 'entity.stormreach.shock_eel': 'Shock Eel', 'entity.stormreach.sparkskink': 'Sparkskink',
    'entity.stormreach.storm_jelly': 'Storm Jelly', 'entity.stormreach.gale_sprite': 'Gale Sprite', 'entity.stormreach.tidewrack_hermit': 'Tidewrack Hermit',
    'entity.stormreach.thunderbird': 'Thunderbird', 'entity.stormreach.intercept_vehicle': 'Tornado Intercept Vehicle',
    'entity.stormreach.weather_balloon': 'Weather Balloon',
    'block.stormreach.rain_gauge.reading': 'Rain gauge: %s mm', 'block.stormreach.rain_gauge.emptied': 'Rain gauge emptied',
    'block.stormreach.tornado_probe.armed': 'Tornado probe armed: waiting for a tornado to pass over',
    'block.stormreach.tornado_probe.reading': 'Probe reading: peak wind %s mph (%s), core passed within %s blocks, on day %s',
    'item.stormreach.weather_balloon.launched': 'Balloon away! It will radio its sounding back from the storm tops.',
    'item.stormreach.chasers_logbook.title': "Storm Chaser's Log", 'item.stormreach.chasers_logbook.tooltip': '%s tornadoes logged',
    'item.stormreach.chasers_logbook.seen': 'Tornadoes witnessed: %s · strongest: %s · closest: %s',
    'item.stormreach.chasers_logbook.field': 'Vehicle intercepts: %s · probe readings: %s (peak %s mph) · balloons launched: %s',
    'chase.stormreach.dash.clear': 'TIV %s mph · no tornado on radar · wind %s mph from %s · %s hPa',
    'chase.stormreach.dash.tornado': '◉ %s tornado · %s m %s · moving %s at %s mph · peak %s mph · wind here %s mph %s',
    'chase.stormreach.dash.anchored': '· ANCHORED',
    'chase.stormreach.sounding.title': 'Radiosonde sounding from %s, %s', 'chase.stormreach.sounding.surface': 'Surface: %s °C, dew point %s °C, humidity %s%%, %s hPa',
    'chase.stormreach.sounding.aloft': 'Burst at y=%s: %s °C · cloud base ~%s m · freezing level ~%s m',
    'chase.stormreach.sounding.cape': 'Instability (CAPE) ~%s J/kg · deep-layer shear %s kt', 'chase.stormreach.sounding.risk': 'Severe weather risk: %s',
}
for k, v in names.items(): L(k, v)
for m, t in (('cloud_whale', 'Cloud Whale'), ('shock_eel', 'Shock Eel'), ('sparkskink', 'Sparkskink'), ('storm_jelly', 'Storm Jelly'),
             ('gale_sprite', 'Gale Sprite'), ('tidewrack_hermit', 'Tidewrack Hermit'), ('thunderbird', 'Thunderbird')):
    L(f'item.stormreach.{m}_spawn_egg', f'{t} Spawn Egg')

# ------------------------------------------------------------------------------------------- loot
def self_drop(name):
    w(f'{D}/loot_table/blocks/{name}.json', {'type': 'minecraft:block', 'pools': [{'bonus_rolls': 0.0, 'conditions': [{'condition': 'minecraft:survives_explosion'}],
       'entries': [{'type': 'minecraft:item', 'name': ns(name)}], 'rolls': 1.0}], 'random_sequence': f'stormreach:blocks/{name}'})
for b in ('glowgel_block', 'skylight_lantern', 'nacre_block', 'nacre_bricks', 'rain_gauge', 'tornado_probe'): self_drop(b)
# match the loot format already used by the mod's other block tables
sample = rj(f'{D}/loot_table/blocks/elderstorm_planks.json') if os.path.exists(f'{D}/loot_table/blocks/elderstorm_planks.json') else None
if sample:
    for b in ('glowgel_block', 'skylight_lantern', 'nacre_block', 'nacre_bricks', 'rain_gauge', 'tornado_probe'):
        t = json.loads(json.dumps(sample).replace('elderstorm_planks', b)); w(f'{D}/loot_table/blocks/{b}.json', t)
def mob_loot(name, pools): w(f'{D}/loot_table/entities/{name}.json', {'type': 'minecraft:entity', 'pools': pools, 'random_sequence': f'stormreach:entities/{name}'})
def pool(item, lo, hi, looting=True, chance=None, smelt=False):
    mods = [{'type': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}}]
    if looting: mods.append({'type': 'minecraft:enchanted_count_increase', 'count': {'type': 'minecraft:uniform', 'min': 0, 'max': 1}, 'enchantment': 'minecraft:looting'})
    if smelt: mods.append({'type': 'minecraft:furnace_smelt', 'condition': {'type': 'minecraft:entity_properties', 'entity': 'this', 'predicate': {'flags': {'is_on_fire': True}}}})
    p = {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': ns(item), 'modifier': mods}]}
    if chance: p['condition'] = {'type': 'minecraft:random_chance', 'chance': chance}
    return p
# reuse the mod's own pool format if gendata8 left an example
ex = f'{D}/loot_table/entities/glimmerstag.json'
if os.path.exists(ex):
    gpool = rj(ex)['pools'][0]
    def pool(item, lo, hi, looting=True, chance=None, smelt=False, _t=gpool):
        p = json.loads(json.dumps(_t))
        e = p['entries'][0]; e['name'] = ns(item)
        for m in e.get('modifier', e.get('functions', [])):
            if 'count' in m and m.get('type', m.get('function')) == 'minecraft:set_count': m['count'] = {'type': 'minecraft:uniform', 'min': lo, 'max': hi}
        if not looting:
            key = 'modifier' if 'modifier' in e else 'functions'
            e[key] = [m for m in e.get(key, []) if 'looting' not in json.dumps(m)]
        if smelt:
            key = 'modifier' if 'modifier' in e else 'functions'
            e.setdefault(key, []).append({'type': 'minecraft:furnace_smelt', 'condition': {'type': 'minecraft:entity_properties', 'entity': 'this',
                                          'predicate': {'flags': {'is_on_fire': True}}}})
        p.pop('condition', None); p.pop('conditions', None)
        if chance: p['condition'] = {'type': 'minecraft:random_chance', 'chance': chance}
        return p
mob_loot('cloud_whale', [pool('cloud_ambergris', 1, 2), pool('glowgel', 0, 2)])
mob_loot('shock_eel', [pool('voltaic_eel', 1, 1, smelt=True), pool('charged_scale', 1, 1, looting=False, chance=0.2)])
mob_loot('sparkskink', [pool('charged_scale', 1, 1, looting=False, chance=0.15)])
mob_loot('storm_jelly', [pool('glowgel', 1, 3)])
mob_loot('gale_sprite', [pool('gale_essence', 0, 1)])
mob_loot('tidewrack_hermit', [pool('pearlescent_shell', 0, 1), pool('minecraft:kelp', 0, 1)])
mob_loot('thunderbird', [pool('thunderbird_plume', 2, 4), pool('charged_scale', 1, 3)])
mob_loot('intercept_vehicle', [pool('intercept_vehicle', 1, 1, looting=False)])
mob_loot('weather_balloon', [])

# ------------------------------------------------------------------------------------------- recipes
def shaped(name, pattern, key, result, count=1, cat='misc'):
    w(f'{D}/recipe/{name}.json', {'type': 'minecraft:crafting_shaped', 'category': cat, 'key': {k: ns(v) for k, v in key.items()}, 'pattern': pattern,
                                  'result': {'count': count, 'id': ns(result)}})
def shapeless(name, ings, result, count=1, cat='misc'):
    w(f'{D}/recipe/{name}.json', {'type': 'minecraft:crafting_shapeless', 'category': cat, 'ingredients': [ns(i) for i in ings], 'result': {'count': count, 'id': ns(result)}})
def cook(name, ing, result, xp=0.35):
    for t, time, suffix in (('smelting', 200, ''), ('smoking', 100, '_from_smoking'), ('campfire_cooking', 600, '_from_campfire_cooking')):
        w(f'{D}/recipe/{name}{suffix}.json', {'type': f'minecraft:{t}', 'category': 'food', 'cookingtime': time, 'experience': xp, 'ingredient': ns(ing),
                                              'result': {'id': ns(result)}})
shaped('glowgel_block', ['GGG', 'GGG', 'GGG'], {'G': 'glowgel'}, 'glowgel_block', 1, 'building')
shapeless('glowgel_from_block', ['glowgel_block'], 'glowgel', 9)
shaped('skylight_lantern', [' N ', 'GAG', ' N '], {'N': 'minecraft:iron_nugget', 'G': 'glowgel', 'A': 'cloud_ambergris'}, 'skylight_lantern', 2, 'building')
shaped('nacre_block', ['SS', 'SS'], {'S': 'pearlescent_shell'}, 'nacre_block', 1, 'building')
shaped('nacre_bricks', ['NN', 'NN'], {'N': 'nacre_block'}, 'nacre_bricks', 4, 'building')
shaped('tide_conch', [' G ', 'SPS', ' S '], {'G': 'minecraft:gold_ingot', 'S': 'pearlescent_shell', 'P': 'minecraft:prismarine_shard'}, 'tide_conch', 1, 'equipment')
shaped('stormkite', ['PPP', 'SCS', ' K '], {'P': 'thunderbird_plume', 'S': 'minecraft:string', 'C': 'charged_scale', 'K': 'minecraft:stick'}, 'stormkite', 1, 'equipment')
shapeless('bottled_gale', ['gale_essence', 'minecraft:glass_bottle'], 'bottled_gale', 1)
cook('cooked_voltaic_eel', 'voltaic_eel', 'cooked_voltaic_eel')
shaped('intercept_vehicle', ['IGI', 'BAB', 'C C'], {'I': 'minecraft:iron_ingot', 'G': 'minecraft:glass_pane', 'B': 'minecraft:iron_block',
       'A': 'anemometer', 'C': 'minecraft:coal_block'}, 'intercept_vehicle', 1, 'misc')
shapeless('weather_balloon', ['minecraft:slime_ball', 'minecraft:slime_ball', 'minecraft:string', 'thermometer'], 'weather_balloon', 2)
shaped('rain_gauge', [' G ', ' G ', ' I '], {'G': 'minecraft:glass_pane', 'I': 'minecraft:iron_ingot'}, 'rain_gauge', 1, 'redstone')
shaped('tornado_probe', [' A ', 'ORO', 'BBB'], {'A': 'anemometer', 'O': 'minecraft:orange_dye', 'R': 'minecraft:redstone', 'B': 'minecraft:iron_block'},
       'tornado_probe', 1, 'redstone')
shapeless('chasers_logbook', ['minecraft:book', 'minecraft:feather', 'minecraft:ink_sac', 'minecraft:compass'], 'chasers_logbook', 1)

# ------------------------------------------------------------------------------------------- tags
tag_add(f'{MC}/block/mineable/pickaxe.json', ['stormreach:nacre_block', 'stormreach:nacre_bricks', 'stormreach:skylight_lantern', 'stormreach:tornado_probe', 'stormreach:rain_gauge'])
tag_add(f'{MC}/item/fishes.json', ['stormreach:voltaic_eel', 'stormreach:cooked_voltaic_eel'])

# ------------------------------------------------------------------------------------------- biome spawns
def cnt(t_, lo, hi, wt): return {'type': t_, 'count': {'type': 'minecraft:uniform', 'max_inclusive': hi, 'min_inclusive': lo}, 'weight': wt}
def add_spawns(biome, cat, entries):
    p = f'{WG}/biome/{biome}.json'
    if not os.path.exists(p): return
    b = rj(p)
    sp = b['attributes']['minecraft:gameplay/natural_mob_spawns']['argument']['spawns_by_category']
    lst = sp.setdefault(cat, [])
    for e in entries:
        if not any(x['type'] == e['type'] for x in lst): lst.append(e)
    w(p, b)
for b in ('stormreach_coast', 'squallwind_bluffs'):
    add_spawns(b, 'creature', [cnt('stormreach:sparkskink', 2, 3, 8), cnt('stormreach:tidewrack_hermit', 1, 3, 8)])
    add_spawns(b, 'water_creature', [cnt('stormreach:shock_eel', 1, 2, 6)])
add_spawns('brinewater_marsh', 'water_creature', [cnt('stormreach:shock_eel', 1, 3, 10)])
add_spawns('brinewater_marsh', 'creature', [cnt('stormreach:tidewrack_hermit', 1, 2, 5)])
add_spawns('static_fields', 'creature', [cnt('stormreach:sparkskink', 2, 4, 10)])
add_spawns('cloudsea_expanse', 'creature', [cnt('stormreach:cloud_whale', 1, 1, 3), cnt('stormreach:storm_jelly', 2, 4, 8)])
add_spawns('aurora_spires', 'creature', [cnt('stormreach:storm_jelly', 2, 3, 6), cnt('stormreach:gale_sprite', 1, 3, 6)])
add_spawns('galewood_highlands', 'creature', [cnt('stormreach:gale_sprite', 1, 3, 8)])
add_spawns('elderstorm_grove', 'creature', [cnt('stormreach:gale_sprite', 1, 2, 5)])

# ------------------------------------------------------------------------------------------- advancements
CODE = {'trigger': {'trigger': 'minecraft:impossible'}}
def has(i): return {'conditions': {'items': [{'items': ns(i)}]}, 'trigger': 'minecraft:inventory_changed'}
def killed(e): return {'conditions': {'entity': {'type': 'minecraft:entity_properties', 'entity': 'this', 'predicate': {'minecraft:entity_type': ns(e)}}},
                       'trigger': 'minecraft:player_killed_entity'}
def adv(name, parent, icon, crit, title, desc, frame='task', hidden=False, xp=None):
    d = {'parent': ns(parent), 'criteria': crit, 'requirements': [list(crit)],
         'display': {'title': {'translate': f'advancements.stormreach.{name}.title'}, 'description': {'translate': f'advancements.stormreach.{name}.description'},
                     'icon': {'id': ns(icon)}, 'frame': frame, 'hidden': hidden}}
    if xp: d['rewards'] = {'experience': xp}
    w(f'{D}/advancement/{name}.json', d)
    L(f'advancements.stormreach.{name}.title', title); L(f'advancements.stormreach.{name}.description', desc)
adv('cloud_ambergris', 'root', 'cloud_ambergris', {'a': has('cloud_ambergris')}, 'Song of the Cloudsea', 'Find Cloud Ambergris left behind by a Cloud Whale')
adv('shock_eel', 'root', 'voltaic_eel', {'k': killed('shock_eel')}, 'Shocking Discovery', 'Defeat a Shock Eel')
adv('charged_scale', 'root', 'charged_scale', {'c': has('charged_scale')}, 'Shed Some Light', 'Collect a Charged Scale from a storm-charged Sparkskink')
adv('gale_friend', 'root', 'gale_essence', CODE, 'Friend of the Gale', 'Give a Gale Sprite Thistle Down')
adv('stormkite_glide', 'charged_scale', 'stormkite', CODE, 'Kite Rider', 'Glide for ten seconds on a Stormkite')
adv('thunderbird', 'root', 'thunderbird_plume', {'k': killed('thunderbird')}, 'Thunderstruck', 'Bring down a Thunderbird', frame='challenge', xp=100)
adv('weather_balloon', 'meteorology', 'weather_balloon', CODE, 'Upper Air', 'Launch a weather balloon and get its sounding back')
adv('chaser_first', 'meteorology', 'chasers_logbook', CODE, 'Chaser', "Log your first tornado in a Storm Chaser's Logbook")
adv('chaser_close', 'chaser_first', 'chasers_logbook', CODE, 'Too Close for Comfort', 'Log a tornado from within 50 blocks')
adv('chaser_violent', 'chaser_first', 'chasers_logbook', CODE, 'Violent', 'Log an EF4 or stronger tornado', frame='goal')
adv('chaser_veteran', 'chaser_first', 'chasers_logbook', CODE, 'Veteran Chaser', 'Log ten different tornadoes', frame='challenge', xp=150)
adv('tiv_intercept', 'meteorology', 'intercept_vehicle', CODE, 'Intercept', 'Anchor a Tornado Intercept Vehicle within 40 blocks of a tornado', frame='goal')
adv('probe_hit', 'meteorology', 'tornado_probe', CODE, 'Data in the Core', 'Read a Tornado Probe that a tornado passed over')
adv('probe_violent', 'probe_hit', 'tornado_probe', CODE, 'Turtle in the Vortex', 'Record winds of 166 mph or more with a Tornado Probe', frame='challenge', xp=100)

with open(f'{A}/lang/en_us.json', 'w') as f: json.dump(lang, f, indent=2, ensure_ascii=False); f.write('\n')
print('gendata9: Stormwild data written')
