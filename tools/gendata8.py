"""Stormreach 5.0 data: tornadoes and meteorology, backpacks, the Hollow Labyrinth, the Stranger and Cyrrus, the Hollow
Shrine, Elderstorm Grove, cinematics, sounds, enchantments and the achievement system."""
import os as _os, pathlib as _pl
ROOT = str(_pl.Path(__file__).resolve().parent.parent)  # the Stormreach project folder
VANILLA = _os.environ.get('STORMREACH_VANILLA', str(_pl.Path(ROOT) / '.vanilla'))  # extracted 26.3 jar (assets/ + data/)
if not _os.path.isdir(VANILLA) and _os.path.isdir('/home/claude/mc/vd'): VANILLA = '/home/claude/mc/vd'
import json, os, re, math, random
import nbtlib
from nbtlib import tag as T
R = ROOT + '/src/main/resources'
A = f'{R}/assets/stormreach'; D = f'{R}/data/stormreach'; NS = 'stormreach'
VA = f'{VANILLA}/assets/minecraft'; VD = f'{VANILLA}/data/minecraft'
MC = f'{R}/data/minecraft/tags'; C = f'{R}/data/c/tags'; WG = f'{D}/worldgen'
def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f: json.dump(obj, f, indent=2); f.write('\n')
def ns(x): return x if ':' in x or x.startswith('#') else f'{NS}:{x}'
def rj(p): return json.load(open(p))
def tag_add(path, values):
    old = rj(path)['values'] if os.path.exists(path) else []
    ids = {v if isinstance(v, str) else v['id'] for v in old}
    w(path, {'values': old + [v for v in values if (v if isinstance(v, str) else v['id']) not in ids]})
lang = rj(f'{A}/lang/en_us.json')
def L(k, v): lang[k] = v

# =====================================================================================
# ASSETS
# =====================================================================================
def item_def(name, model): w(f'{A}/items/{name}.json', {'model': {'type': 'minecraft:model', 'model': ns(model)}})
def gen_item(name, tex=None):
    w(f'{A}/models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'stormreach:item/{tex or name}'}})
    item_def(name, 'item/' + name)
def handheld(name):
    w(f'{A}/models/item/{name}.json', {'parent': 'minecraft:item/handheld', 'textures': {'layer0': f'stormreach:item/{name}'}})
    item_def(name, 'item/' + name)
def model(name, obj): w(f'{A}/models/block/{name}.json', obj)
def simple_state(name, m=None): w(f'{A}/blockstates/{name}.json', {'variants': {'': {'model': ns('block/' + (m or name))}}})
def cube(name, tex=None):
    model(name, {'parent': 'minecraft:block/cube_all', 'textures': {'all': ns('block/' + (tex or name))}}); simple_state(name); item_def(name, 'block/' + name)
def column(name):
    model(name, {'parent': 'minecraft:block/cube_column', 'textures': {'end': f'stormreach:block/{name}_top', 'side': f'stormreach:block/{name}'}})
    model(f'{name}_horizontal', {'parent': 'minecraft:block/cube_column_horizontal', 'textures': {'end': f'stormreach:block/{name}_top', 'side': f'stormreach:block/{name}'}})
    w(f'{A}/blockstates/{name}.json', {'variants': {'axis=x': {'model': f'stormreach:block/{name}_horizontal', 'x': 90, 'y': 90},
        'axis=y': {'model': f'stormreach:block/{name}'}, 'axis=z': {'model': f'stormreach:block/{name}_horizontal', 'x': 90}}})
    item_def(name, 'block/' + name)
def cross(name, tex=None):
    model(name, {'parent': 'minecraft:block/cross', 'textures': {'cross': f'stormreach:block/{tex or name}'}})
    simple_state(name)
    w(f'{A}/models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'stormreach:block/{tex or name}'}})
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
def box(frm, to, tex, faces='nsewud', uv=None):
    names = {'n': 'north', 's': 'south', 'e': 'east', 'w': 'west', 'u': 'up', 'd': 'down'}
    f = {}
    for c in faces:
        t = tex[c] if isinstance(tex, dict) else tex
        f[names[c]] = {'texture': t}
    return {'from': frm, 'to': to, 'faces': f}
FACING_Y = {'north': 0, 'east': 90, 'south': 180, 'west': 270}

# ---- meteorology
for name in ('weather_station', 'tornado_radar', 'weather_monitor', 'storm_detector', 'tornado_siren'):
    side = f'stormreach:block/{name}_side'
    top = f'stormreach:block/{name}_top' if name != 'tornado_siren' else side
    model(name, {'parent': 'minecraft:block/cube', 'textures': {'particle': side, 'north': f'stormreach:block/{name}_front', 'south': side,
           'east': side, 'west': side, 'up': top, 'down': side}})
    simple_state(name); item_def(name, 'block/' + name)
# 5.1: the Doppler radar is a multi-block structure; its controller is the pedestal on top of the mast
model('tornado_radar_item', {'parent': 'minecraft:block/cube', 'textures': {'particle': 'stormreach:block/tornado_radar_side',
      'north': 'stormreach:block/tornado_radar_front', 'south': 'stormreach:block/tornado_radar_side', 'east': 'stormreach:block/tornado_radar_side',
      'west': 'stormreach:block/tornado_radar_side', 'up': 'stormreach:block/tornado_radar_top', 'down': 'stormreach:block/tornado_radar_side'}})
item_def('tornado_radar', 'block/tornado_radar_item')
model('tornado_radar', {'parent': 'minecraft:block/block', 'textures': {'particle': 'stormreach:block/radar_pedestal', 'p': 'stormreach:block/radar_pedestal'},
      'elements': [box([3, 0, 3], [13, 9, 13], '#p'), box([5, 9, 5], [11, 11, 11], '#p')]})
model('radar_part_base', {'parent': 'minecraft:block/block', 'textures': {'particle': 'stormreach:block/radar_base', 'b': 'stormreach:block/radar_base'},
      'elements': [box([0, 0, 0], [16, 8, 16], '#b')]})
model('radar_part_mast', {'parent': 'minecraft:block/block', 'textures': {'particle': 'stormreach:block/radar_mast', 'm': 'stormreach:block/radar_mast'},
      'elements': [box([5, 0, 5], [11, 16, 11], '#m')]})
model('radar_part_dish', {'textures': {'particle': 'stormreach:block/radar_dish'}})
w(f'{D}/loot_table/blocks/radar_part.json', {'type': 'minecraft:block', 'pools': []})  # the pedestal drops the radar
w(f'{A}/blockstates/radar_part.json', {'variants': {f'part={p}': {'model': ns(f'block/radar_part_{p}')} for p in ('base', 'mast', 'dish')}})
model('anemometer', {'parent': 'minecraft:block/block', 'textures': {'particle': 'stormreach:block/anemometer', 'a': 'stormreach:block/anemometer'}, 'elements': [
    box([7, 0, 7], [9, 12, 9], '#a'), box([1, 12, 7.5], [15, 13, 8.5], '#a'), box([7.5, 12, 1], [8.5, 13, 15], '#a'),
    box([0, 11, 6], [2, 14, 9], '#a'), box([14, 11, 7], [16, 14, 10], '#a'), box([7, 11, 0], [10, 14, 2], '#a'), box([6, 11, 14], [9, 14, 16], '#a')]})
simple_state('anemometer'); item_def('anemometer', 'block/anemometer')
model('thermometer', {'parent': 'minecraft:block/block', 'textures': {'particle': 'stormreach:block/thermometer', 't': 'stormreach:block/thermometer',
       'm': 'stormreach:block/anemometer'}, 'elements': [box([5, 0, 5], [11, 1, 11], '#m'), box([7, 1, 7], [9, 15, 9], {'n': '#t', 's': '#t', 'e': '#t', 'w': '#t', 'u': '#m', 'd': '#m'})]})
simple_state('thermometer'); item_def('thermometer', 'block/thermometer')
for n in ('storm_radar', 'weather_radio'): gen_item(n)
SPAWNER_KINDS = ['natural', 'rope', 'needle', 'drill_bit', 'cone', 'stovepipe', 'wedge', 'large_wedge', 'multi_vortex', 'satellite', 'companion', 'landspout', 'waterspout']
EVENT_KINDS = ['supercell', 'downburst', 'hailstorm', 'ball_lightning', 'dispel']
for k in SPAWNER_KINDS: gen_item(f'tornado_spawner_{k}')
for k in EVENT_KINDS: gen_item(f'weather_event_{k}')
gen_item('tornado_director')
gen_item('stormreach_field_guide'); gen_item('storm_crafters_handbook')
# GameTest environment for tests that change world-wide state (weather, every tornado): its own batch, run apart
w(f'{D}/test_environment/isolated.json', {'type': 'minecraft:all_of', 'definitions': []})
# ---- 5.1 stage 3: hail on the ground, puddles
for big in (False, True):
    tex = 'stormreach:block/hail_layer' + ('_big' if big else '')
    for layers in range(1, 9):
        h = layers * 2
        model(f'hail_layer{"_big" if big else ""}_{layers}', {'parent': 'minecraft:block/thin_block' if h < 16 else 'minecraft:block/cube_all',
              'textures': {'particle': tex, 'texture': tex, 'all': tex},
              'elements': [box([0, 0, 0], [16, h, 16], {c: '#texture' for c in 'nsewud'})]})
w(f'{A}/blockstates/hail_layer.json', {'variants': {f'big={str(b).lower()},layers={l}': {'model': ns(f'block/hail_layer{"_big" if b else ""}_{l}')}
                                                    for b in (False, True) for l in range(1, 9)}})
item_def('hail_layer', 'block/hail_layer_2')
for size, inset in ((1, 4), (2, 2), (3, 0)):
    model(f'puddle_{size}', {'parent': 'minecraft:block/block', 'textures': {'particle': 'stormreach:block/puddle', 'p': 'stormreach:block/puddle'},
          'elements': [{'from': [inset, 0, inset], 'to': [16 - inset, 0.5, 16 - inset],
                        'faces': {'up': {'texture': '#p', 'uv': [inset, inset, 16 - inset, 16 - inset]}}}]})
w(f'{A}/blockstates/puddle.json', {'variants': {f'size={n}': {'model': ns(f'block/puddle_{n}')} for n in (1, 2, 3)}})

# ---- backpacks
TIERS = ['diamond', 'obsidian', 'netherite', 'storm_crystal', 'voltite', 'aurorite', 'maelstrom']
for t in TIERS:
    gen_item(f'{t}_backpack')
    if t != 'diamond': gen_item(f'{t}_backpack_upgrade')
gen_item('storm_chasers_pack')

# ---- labyrinth
cube('labyrinth_seal')
# 5.1: the three locks built into the Vault Door, each for its own key, lit once set
for kind in ('gale', 'thunder', 'hollow'):
    for filled in (False, True):
        model(f'labyrinth_keyhole_{kind}{"_set" if filled else ""}', {'parent': 'minecraft:block/orientable', 'textures': {
            'front': f'stormreach:block/labyrinth_keyhole_{kind}{"_set" if filled else ""}', 'side': 'stormreach:block/vault_door',
            'top': 'stormreach:block/vault_door'}})
w(f'{A}/blockstates/labyrinth_keyhole.json', {'variants': {f'facing={f},filled={str(fl).lower()},kind={i}':
    {'model': f'stormreach:block/labyrinth_keyhole_{k}{"_set" if fl else ""}', **({'y': y} if y else {})}
    for f, y in FACING_Y.items() for i, k in enumerate(('gale', 'thunder', 'hollow')) for fl in (False, True)}})
cube('vault_door'); cube('vault_wall'); cube('hollow_lamp')
w(f'{D}/loot_table/blocks/hollow_lamp.json', {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'condition': {'type': 'minecraft:survives_explosion'}, 'entries': [{'type': 'minecraft:item', 'name': 'stormreach:hollow_lamp'}]}]})
for k in ('gale', 'thunder', 'hollow'): gen_item(f'labyrinth_key_{k}')
item_def('vault_door', 'block/vault_door'); item_def('vault_wall', 'block/vault_wall'); item_def('hollow_lamp', 'block/hollow_lamp')
item_def('labyrinth_keyhole', 'block/labyrinth_keyhole_gale')
for half in ('lower', 'upper'):
    # open box: back, sides and a floor or roof, plus a two-sided grate across the front (so it reads from inside too)
    side = 'stormreach:block/rusted_locker_bottom'; grate = f'stormreach:block/rusted_locker_grate_{half}'
    cap = box([1, 0, 1], [15, 1, 15], '#s') if half == 'lower' else box([1, 15, 1], [15, 16, 15], '#s')
    model(f'rusted_locker_{half}', {'parent': 'minecraft:block/block', 'textures': {'particle': side, 's': side, 'g': grate}, 'elements': [
        box([1, 0, 14], [15, 16, 15], '#s'), box([1, 0, 1], [2, 16, 14], '#s'), box([14, 0, 1], [15, 16, 14], '#s'), cap,
        box([2, 0, 1], [14, 16, 2], {'n': '#g', 's': '#g'}, faces='ns')]})
w(f'{A}/blockstates/rusted_locker.json', {'variants': {f'facing={f},half={h}': {'model': f'stormreach:block/rusted_locker_{h}', **({'y': y} if y else {})}
    for f, y in FACING_Y.items() for h in ('lower', 'upper')}})
gen_item('rusted_locker'); gen_item('labyrinth_key'); gen_item('heart_of_the_labyrinth'); gen_item('hollow_keeper_spawn_egg')

# ---- story
for lit in ('', '_lit'):
    model(f'relic_pedestal{lit}', {'parent': 'minecraft:block/block', 'textures': {'particle': 'stormreach:block/relic_pedestal_side',
           's': 'stormreach:block/relic_pedestal_side', 't': f'stormreach:block/relic_pedestal_top{lit}'}, 'elements': [
        box([1, 0, 1], [15, 3, 15], '#s'), box([4, 3, 4], [12, 12, 12], '#s'), box([2, 12, 2], [14, 15, 14], {c: '#s' for c in 'nsewd'} | {'u': '#t'})]})
w(f'{A}/blockstates/relic_pedestal.json', {'variants': {f'lit={l},relic={r}': {'model': 'stormreach:block/relic_pedestal' + ('_lit' if l == 'true' else '')}
    for l in ('false', 'true') for r in range(1, 5)}})
item_def('relic_pedestal', 'block/relic_pedestal')
for o in range(1, 5):
    for st in ('', '_struck'):
        model(f'storm_conduit_{o}{st}', {'parent': 'minecraft:block/block', 'textures': {'particle': f'stormreach:block/storm_conduit_{o}{st}',
               's': f'stormreach:block/storm_conduit_{o}{st}', 't': 'stormreach:block/storm_conduit_top'}, 'elements': [
            box([3, 0, 3], [13, 2, 13], '#t'), box([5, 2, 5], [11, 16, 11], {'n': '#s', 's': '#s', 'e': '#s', 'w': '#s', 'u': '#t', 'd': '#t'})]})
w(f'{A}/blockstates/storm_conduit.json', {'variants': {f'order={o},struck={s}': {'model': f'stormreach:block/storm_conduit_{o}' + ('_struck' if s == 'true' else '')}
    for o in range(1, 5) for s in ('false', 'true')}})
item_def('storm_conduit', 'block/storm_conduit_1')
model('hollow_altar', {'parent': 'minecraft:block/cube_bottom_top', 'textures': {'top': 'stormreach:block/hollow_altar_top', 'side': 'stormreach:block/hollow_altar_side',
       'bottom': 'stormreach:block/hollow_altar_bottom'}})
simple_state('hollow_altar'); item_def('hollow_altar', 'block/hollow_altar')
TROPHIES = ['sovereign_trophy', 'keeper_trophy', 'cyrrus_trophy', 'tornado_trophy']
for t in TROPHIES:
    fig = f'stormreach:block/{t}_figure'
    shape = {'tornado_trophy': [box([5, 4, 5], [11, 7, 11], '#f'), box([4, 7, 4], [12, 11, 12], '#f'), box([3, 11, 3], [13, 14, 13], '#f')],
             'sovereign_trophy': [box([5, 4, 5], [11, 12, 11], '#f'), box([4, 12, 4], [12, 14, 12], '#g'), box([6, 14, 6], [10, 15, 10], '#g')],
             'keeper_trophy': [box([4, 4, 5], [12, 13, 11], '#f'), box([5, 13, 5], [11, 15, 11], '#f')],
             'cyrrus_trophy': [box([6, 4, 6], [10, 13, 10], '#f'), box([3, 9, 7], [13, 10, 9], '#f'), box([5, 13, 5], [11, 15, 11], '#g')]}[t]
    model(t, {'parent': 'minecraft:block/block', 'textures': {'particle': 'stormreach:block/trophy_base', 'b': 'stormreach:block/trophy_base', 'f': fig,
           'g': 'stormreach:block/trophy_base'}, 'elements': [box([2, 0, 2], [14, 2, 14], '#b'), box([3, 2, 3], [13, 4, 13], '#b')] + shape})
    simple_state(t); item_def(t, 'block/' + t)
for n in ['journal_page_1', 'journal_page_2', 'journal_page_3', 'journal_page_4', 'journal_page_5', 'weathered_journal', 'hollow_remnant', 'maelstrom_core',
          'stormcrown', 'lightning_charm', 'storm_chaser_cap', 'chasers_badge', 'stranger_spawn_egg', 'cyrrus_spawn_egg']:
    gen_item(n)
handheld('weathervane_scepter'); handheld('weatherbreaker')
w(f'{A}/equipment/stormcrown.json', {'layers': {'humanoid': [{'texture': 'stormreach:stormcrown'}], 'humanoid_baby': [{'texture': 'stormreach:stormcrown'}]}})
w(f'{A}/equipment/storm_chaser_cap.json', {'layers': {'humanoid': [{'texture': 'stormreach:storm_chaser_cap'}], 'humanoid_baby': [{'texture': 'stormreach:storm_chaser_cap'}]}})

# ---- forest
column('elderstorm_log'); column('stripped_elderstorm_log'); cube('elderstorm_planks'); cube('elderstorm_leaves')
wm = {'dark_oak_planks': 'elderstorm_planks', 'dark_oak_door_top': 'elderstorm_door_top', 'dark_oak_door_bottom': 'elderstorm_door_bottom', 'dark_oak_trapdoor': 'elderstorm_trapdoor'}
for kind in ('fence', 'fence_gate', 'door', 'trapdoor', 'button', 'pressure_plate', 'stairs', 'slab'):
    clone_family(f'dark_oak_{kind}', f'elderstorm_{kind}', wm)
w(f'{A}/models/item/elderstorm_door.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'stormreach:item/elderstorm_door'}})
bs = rj(f'{A}/blockstates/elderstorm_slab.json'); bs['variants']['type=double'] = {'model': 'stormreach:block/elderstorm_planks'}; w(f'{A}/blockstates/elderstorm_slab.json', bs)
for p in ('elderstorm_sapling', 'thunderbloom', 'sparkfern'): cross(p)
for a in range(3): model(f'stormfruit_pod_stage{a}', {'parent': 'minecraft:block/cross', 'textures': {'cross': f'stormreach:block/stormfruit_pod_stage{a}'}})
w(f'{A}/blockstates/stormfruit_pod.json', {'variants': {f'age={a}': {'model': f'stormreach:block/stormfruit_pod_stage{a}'} for a in range(3)}})
for n in ('stormfruit', 'gilded_stormfruit', 'stormfruit_preserve', 'thistle_down', 'glimmer_antler', 'glimmerstag_spawn_egg', 'thistlehop_spawn_egg'):
    gen_item(n)

# =====================================================================================
# SOUNDS
# =====================================================================================
VOICE = rj(f'{ROOT}/tools/voice_lines.json')['lines']
sounds = {
    'block.tornado_siren': {'sounds': [{'name': 'stormreach:block/tornado_siren', 'attenuation_distance': 96}], 'subtitle': 'subtitles.stormreach.block.tornado_siren'},
    'entity.tornado.roar': {'sounds': [{'name': 'stormreach:entity/tornado_roar', 'attenuation_distance': 64}], 'subtitle': 'subtitles.stormreach.entity.tornado.roar'},
    'music.labyrinth.calm': {'sounds': [{'name': 'stormreach:music/labyrinth_calm', 'stream': True}]},
    'music.labyrinth.tense': {'sounds': [{'name': 'stormreach:music/labyrinth_tense', 'stream': True}]},
    'music.labyrinth.chase': {'sounds': [{'name': 'stormreach:music/labyrinth_chase', 'stream': True}]},
    'music.cyrrus': {'sounds': [{'name': 'stormreach:music/cyrrus', 'stream': True}]},
    'ambient.heartbeat': {'sounds': [{'name': 'stormreach:ambient/heartbeat'}], 'subtitle': 'subtitles.stormreach.ambient.heartbeat'},
    'entity.stranger.whisper': {'sounds': [{'name': 'stormreach:entity/stranger_whisper', 'attenuation_distance': 12}], 'subtitle': 'subtitles.stormreach.entity.stranger.whisper'},
    'entity.hollow_keeper.screech': {'sounds': [{'name': 'stormreach:entity/hollow_keeper_screech'}], 'subtitle': 'subtitles.stormreach.entity.hollow_keeper.screech'},
}
import sys as _sys; _sys.path.insert(0, f'{ROOT}/tools')
from sfx_list import SFX
for _sid, (_sub, _old, _att) in SFX.items():   # 5.1: every effect is the mod's own (tools/sfx51.py)
    sounds[_sid] = {'sounds': [{'name': f'stormreach:sfx/{_sid.replace(".", "_")}', 'attenuation_distance': _att}], 'subtitle': f'subtitles.stormreach.{_sid}'}
for k in VOICE: sounds[f'voice.cyrrus.{k}'] = {'sounds': [{'name': f'stormreach:voice/cyrrus/{k}', 'stream': True, 'attenuation_distance': 64}]}
w(f'{A}/sounds.json', sounds)
for k, v in {'block.tornado_siren': 'Tornado siren wails', 'entity.tornado.roar': 'Tornado roars', 'ambient.heartbeat': 'Heart pounds',
             'entity.stranger.whisper': 'Something whispers', 'entity.hollow_keeper.screech': 'Hollow Keeper screeches'}.items():
    L(f'subtitles.stormreach.{k}', v)

# =====================================================================================
# CINEMATICS (timing derived from the measured voice lines, so nothing overlaps)
# =====================================================================================
def ticks(sec): return int(math.ceil(sec * 20))
def script(name, header, rows):
    out = [f'# {header}', '# tick | cue id | subtitle key | sound | actions   (generated by tools/gendata8.py from tools/voice_lines.json)']
    for r in rows: out.append(' | '.join(str(x) for x in r[:4]) + ' | ' + ('' if r[4] == '-' else r[4]))
    os.makedirs(f'{D}/cinematic', exist_ok=True)
    open(f'{D}/cinematic/{name}.txt', 'w').write('\n'.join(out) + '\n')
INTRO_ACTIONS = {'l01': '-', 'l02': '-', 'l03': 'grow:1.5', 'l04': 'rain', 'l05': 'grow:2.5,strike_ring', 'l06': 'thunder', 'l07': 'grow:4.0,corrupt,strike_ring', 'l08': 'music'}
rows = [(0, 'rumble', '-', '-', 'darken,thunder'), (40, 'emerge', '-', '-', 'rise:6')]
t = 80
for k in ('l01', 'l02', 'l03', 'l04', 'l05', 'l06', 'l07', 'l08'):
    rows.append((t, k, f'dialogue.stormreach.cyrrus.{k}', f'stormreach:voice.cyrrus.{k}', INTRO_ACTIONS[k]))
    t += ticks(VOICE[k]['seconds']) + 12
rows.append((t, 'begin', '-', '-', 'fight'))
script('cyrrus_intro', 'Cyrrus emerges at the Hollow Altar (played once; the boss is invulnerable until "fight").', rows)
INTRO_LENGTH = t
script('cyrrus_phases', 'Lines spoken when Cyrrus enters each phase (cue ids phase2..phase5; ticks unused).',
       [(0, k, f'dialogue.stormreach.cyrrus.{k}', f'stormreach:voice.cyrrus.{k}', '-') for k in ('phase2', 'phase3', 'phase4', 'phase5')])
rows = []; t = 10
for k in ('d01', 'd02', 'd03'):
    rows.append((t, k, f'dialogue.stormreach.cyrrus.{k}', f'stormreach:voice.cyrrus.{k}', '-')); t += ticks(VOICE[k]['seconds']) + 15
script('cyrrus_defeat', 'Cyrrus\'s last words, played where he fell.', rows)
for k, v in VOICE.items(): L(f'dialogue.stormreach.cyrrus.{k}', v['text'].replace('Now. Let', 'Now — let'))
L('dialogue.stormreach.speaker.cyrrus', '<Cyrrus>')

# =====================================================================================
# LANGUAGE
# =====================================================================================
BLOCK_NAMES = {
 'anemometer': 'Anemometer', 'thermometer': 'Thermometer', 'weather_station': 'Weather Station', 'storm_detector': 'Storm Detector',
 'tornado_radar': 'Doppler Radar', 'tornado_siren': 'Tornado Siren', 'weather_monitor': 'Weather Monitor',
 'rusted_locker': 'Rusted Locker', 'labyrinth_seal': 'Labyrinth Seal', 'labyrinth_keyhole': 'Labyrinth Keyhole',
 'relic_pedestal': 'Relic Pedestal', 'storm_conduit': 'Storm Conduit', 'hollow_altar': 'Hollow Altar',
 'sovereign_trophy': 'Sovereign Trophy', 'keeper_trophy': 'Keeper Trophy', 'cyrrus_trophy': 'Cyrrus Trophy', 'tornado_trophy': 'Tornado Trophy',
 'elderstorm_log': 'Elderstorm Log', 'stripped_elderstorm_log': 'Stripped Elderstorm Log', 'elderstorm_planks': 'Elderstorm Planks',
 'elderstorm_leaves': 'Elderstorm Leaves', 'elderstorm_sapling': 'Elderstorm Sapling', 'elderstorm_stairs': 'Elderstorm Stairs',
 'elderstorm_slab': 'Elderstorm Slab', 'elderstorm_fence': 'Elderstorm Fence', 'elderstorm_fence_gate': 'Elderstorm Fence Gate',
 'elderstorm_door': 'Elderstorm Door', 'elderstorm_trapdoor': 'Elderstorm Trapdoor', 'elderstorm_button': 'Elderstorm Button',
 'elderstorm_pressure_plate': 'Elderstorm Pressure Plate', 'stormfruit_pod': 'Stormfruit', 'thunderbloom': 'Thunderbloom', 'sparkfern': 'Sparkfern'}
for k, v in BLOCK_NAMES.items(): L(f'block.stormreach.{k}', v)
MORPH = {'rope': 'Rope', 'needle': 'Needle', 'drill_bit': 'Drill-bit', 'cone': 'Cone', 'stovepipe': 'Stovepipe', 'wedge': 'Wedge', 'large_wedge': 'Large Wedge'}
SPAWNER_NAMES = {'natural': 'Tornado (Natural Climatology)', 'rope': 'Rope Tornado', 'needle': 'Needle Tornado', 'drill_bit': 'Drill-bit Tornado',
    'cone': 'Cone Tornado', 'stovepipe': 'Stovepipe Tornado', 'wedge': 'Wedge Tornado', 'large_wedge': 'Large Wedge Tornado',
    'multi_vortex': 'Multiple-Vortex Tornado', 'satellite': 'Tornado with Satellite', 'companion': 'Tornado with Companion', 'landspout': 'Landspout',
    'waterspout': 'Waterspout'}
ITEM_NAMES = {
 'storm_radar': 'Handheld Storm Radar', 'weather_radio': 'Weather Radio',
 'weather_event_supercell': 'Supercell', 'weather_event_downburst': 'Downburst', 'weather_event_hailstorm': 'Hailstorm',
 'weather_event_ball_lightning': 'Ball Lightning Swarm', 'weather_event_dispel': 'Dispel Weather Events',
 'diamond_backpack': 'Diamond Backpack', 'obsidian_backpack': 'Obsidian Backpack', 'netherite_backpack': 'Netherite Backpack',
 'storm_crystal_backpack': 'Storm Crystal Backpack', 'voltite_backpack': 'Voltite Backpack', 'aurorite_backpack': 'Aurorite Backpack',
 'maelstrom_backpack': 'Maelstrom Backpack', 'storm_chasers_pack': "Storm Chaser's Pack",
 'labyrinth_key': 'Labyrinth Key', 'heart_of_the_labyrinth': 'Heart of the Labyrinth', 'hollow_keeper_spawn_egg': 'Hollow Keeper Spawn Egg',
 'journal_page_1': 'Torn Journal Page (I)', 'journal_page_2': 'Torn Journal Page (II)', 'journal_page_3': 'Torn Journal Page (III)',
 'journal_page_4': 'Torn Journal Page (IV)', 'journal_page_5': 'Torn Journal Page (V)', 'weathered_journal': 'Weathered Journal',
 'hollow_remnant': 'Hollow Remnant', 'maelstrom_core': 'Maelstrom Core', 'stormcrown': 'Stormcrown', 'weathervane_scepter': 'Weathervane Scepter',
 'weatherbreaker': 'Weatherbreaker', 'lightning_charm': 'Lightning Charm', 'storm_chaser_cap': "Storm Chaser's Cap", 'chasers_badge': "Chaser's Badge",
 'stranger_spawn_egg': 'Stranger Spawn Egg', 'cyrrus_spawn_egg': 'Cyrrus Spawn Egg',
 'stormfruit': 'Stormfruit', 'gilded_stormfruit': 'Gilded Stormfruit', 'stormfruit_preserve': 'Stormfruit Preserve', 'thistle_down': 'Thistle Down',
 'glimmer_antler': 'Glimmer Antler', 'glimmerstag_spawn_egg': 'Glimmerstag Spawn Egg', 'thistlehop_spawn_egg': 'Thistlehop Spawn Egg'}
for k, v in SPAWNER_NAMES.items(): ITEM_NAMES[f'tornado_spawner_{k}'] = v
for t in TIERS[1:]: ITEM_NAMES[f'{t}_backpack_upgrade'] = ITEM_NAMES[f'{t}_backpack'].replace('Backpack', 'Upgrade Kit')
for k, v in ITEM_NAMES.items(): L(f'item.stormreach.{k}', v)
for k, v in {'tornado': 'Tornado', 'hollow_keeper': 'Hollow Keeper', 'stranger': '???', 'cyrrus': 'Cyrrus, the Hollow Storm',
             'glimmerstag': 'Glimmerstag', 'thistlehop': 'Thistlehop'}.items(): L(f'entity.stormreach.{k}', v)
TIPS = {
 'labyrinth_key': ['An old Labyrinth Key. Since the Vault Door was rebuilt, each lock needs its own key: Gale, Thunder or Hollow.'],
 'heart_of_the_labyrinth': ['It beats, slowly, out of time with yours.', 'Someone left it here on purpose.'],
 'gilded_stormfruit': ['A Stormfruit dipped in gold. Something old would like it.'],
 'journal_page_1': ['"Day 3. The Stormreach coast never sleeps. At night I hear the wind say a name.', 'Not mine. Not yet."'],
 'journal_page_2': ['"I found the crowned thing\'s tower. It is not a king. It is a door.', 'I mean to knock."'],
 'journal_page_3': ['"Below the ground there is a place built to hold something.', 'The walls lean inward, listening."'],
 'journal_page_4': ['"I left my heart in the deepest room so the storm could not take all of me.', 'Whoever finds it — do not bring it back."'],
 'journal_page_5': ['"If you are reading this, you saw me among the trees. I am sorry.', 'I could not stop following you."'],
 'weathered_journal': ['Five pages, bound together again. It is heavier than it should be.', 'Lay it on the Hollow Altar.'],
 'hollow_remnant': ['Left behind where the Stranger stood.', 'Cold, and faintly humming.'],
 'maelstrom_core': ['What was left of Cyrrus when the storm let go.'],
 'stormcrown': ['Wearer is immune to lightning. Don\'t ask it for anything.'],
 'storm_chaser_cap': ['Warns you of tornadoes nearby, like a weather radio.'],
 'chasers_badge': ['Carried: tornado debris and winds do half damage, and you get tornado warnings.'],
 'lightning_charm': ['In your offhand: you are Grounded, immune to lightning and storm winds.'],
 'weathervane_scepter': ['Use: cycle the weather (clear → rain → thunder → clear).'],
 'weatherbreaker': ['Hits hurl the target with wind; one in four calls lightning.'],
}
for k, lines in TIPS.items():
    for i, line in enumerate(lines): L(f'item.stormreach.{k}.tooltip' + ('' if i == 0 else str(i + 1)), line)
L('item.stormreach.weathervane_scepter.tooltip', 'Use: cycle the weather. Sneak + use: call a downburst ahead of you.')
L('item.stormreach.weatherbreaker.tooltip', 'Hits hurl the target with wind; one in four calls lightning.')
MSG = {
 # instruments
 'instrument.stormreach.anemometer': 'Wind: %s mph sustained, gusting %s mph (%s km/h) — %s',
 'instrument.stormreach.vane': 'Wind from the %s',
 'instrument.stormreach.barometer': 'Pressure: %s hPa (%s inHg)',
 'instrument.stormreach.thermometer': 'Temperature: %s °C (%s °F) — dew point %s °C, humidity %s%%',
 'instrument.stormreach.detector.clear': 'No precipitation detected.', 'instrument.stormreach.detector.rain': 'Rain detected overhead.',
 'instrument.stormreach.detector.storm': 'Thunderstorm overhead: lightning in the area.',
 'instrument.stormreach.detector.tornado': 'TORNADO DETECTED to the %s!',
 'instrument.stormreach.siren': 'The siren wails while it gets a redstone signal. Wire it to a Doppler Radar or Storm Detector for automatic warnings.',
 'instrument.stormreach.station.header': '— Weather Station —', 'instrument.stormreach.station.now': 'Now: %s',
 'instrument.stormreach.station.wind': 'Wind: %s mph gusting %s, from the %s',
 'instrument.stormreach.station.air': 'Air: %s °C, %s%% humidity, dew point %s °C, %s hPa',
 'instrument.stormreach.station.forecast': 'Forecast: %s', 'instrument.stormreach.station.risk': 'Severe weather outlook: %s risk',
 'instrument.stormreach.monitor.unlinked': 'Place a Weather Station and a Doppler Radar within 8 blocks to link this monitor.',
 'instrument.stormreach.radar.contact': 'Rotation %s, %s blocks away: gate-to-gate shear %s mph, moving %s mph toward the %s',
 'instrument.stormreach.radar.detail': ' — estimated %s, about %s blocks wide, %s %s',
 'instrument.stormreach.radar.none': 'Radar: no rotation detected.',
 'instrument.stormreach.radio': 'NOAA-style weather radio: %s. Severe outlook: %s.',
 'weather.stormreach.clear': 'Clear', 'weather.stormreach.rain': 'Rain', 'weather.stormreach.thunderstorm': 'Thunderstorm',
 'tornado.stormreach.multivortex': '(multiple-vortex)',
 'event.stormreach.tornado.warning': 'TORNADO WARNING: a tornado is on the ground %s of you, %s blocks away. Take shelter!',
 'event.stormreach.tornado.survey': 'Damage survey: rated %s. Highest wind supported by damage %s mph (peak gust %s mph). %s blocks damaged along a %s km path, up to %s blocks wide. Appearance: %s.',
 'item.stormreach.tornado_spawner.needs_water': 'A waterspout needs open water: aim at a lake or the sea.',
 'block.stormreach.hail_layer': 'Hail',
 'block.stormreach.puddle': 'Puddle',
 'entity.stormreach.hailstone': 'Hailstone',
 'entity.stormreach.storm_cell': 'Storm Cloud',
 'item.stormreach.backpack.worn_hint': 'Sneak + use to wear it (it has its own slot, above your offhand).',
 'gui.stormreach.weather.device.anemometer': 'Anemometer',
 'gui.stormreach.weather.device.thermometer': 'Thermometer',
 'gui.stormreach.weather.device.weather_station': 'Weather Station',
 'gui.stormreach.weather.device.storm_detector': 'Storm Detector',
 'gui.stormreach.weather.device.tornado_radar': 'Doppler Radar',
 'gui.stormreach.weather.device.tornado_siren': 'Tornado Siren',
 'gui.stormreach.weather.device.weather_monitor': 'Weather Monitor: Central Weather Interface',
 'gui.stormreach.weather.device.barometer': 'Barometer',
 'gui.stormreach.weather.device.wind_vane': 'Wind Vane',
 'gui.stormreach.weather.device.storm_radar': 'Portable Storm Radar',
 'gui.stormreach.weather.device.weather_radio': 'Weather Radio',
 'gui.stormreach.weather.tab.overview': 'Overview',
 'gui.stormreach.weather.tab.radar': 'Radar',
 'gui.stormreach.weather.tab.wind': 'Wind',
 'gui.stormreach.weather.tab.air': 'Air',
 'gui.stormreach.weather.tab.forecast': 'Forecast',
 'gui.stormreach.weather.reflectivity': 'Reflectivity',
 'gui.stormreach.weather.velocity': 'Velocity',
 'gui.stormreach.weather.sky': 'Sky: %s',
 'gui.stormreach.weather.sky.clear': 'clear',
 'gui.stormreach.weather.sky.rain': 'rain',
 'gui.stormreach.weather.sky.thunderstorm': 'thunderstorm',
 'gui.stormreach.weather.wind_line': 'Wind: %s mph, gusting %s, from the %s',
 'gui.stormreach.weather.air_line': 'Air: %s °C, humidity %s%%',
 'gui.stormreach.weather.risk': 'Severe weather risk: %s',
 'gui.stormreach.weather.no_tornadoes': 'No tornadoes in range.',
 'gui.stormreach.weather.tornado_warning': 'TORNADO WARNING',
 'gui.stormreach.weather.contact_short': '%s, %s blocks, %s (estimated)',
 'gui.stormreach.weather.wind_avg': 'Average wind: %s mph (%s km/h)',
 'gui.stormreach.weather.wind_gust': 'Gusts: %s mph (%s km/h)',
 'gui.stormreach.weather.wind_from': 'From the %s',
 'gui.stormreach.weather.temperature': 'Temperature: %s °C (%s °F)',
 'gui.stormreach.weather.dew_point': 'Dew point: %s °C',
 'gui.stormreach.weather.humidity': 'Humidity: %s%%',
 'gui.stormreach.weather.pressure': 'Pressure: %s hPa (%s inHg)',
 'gui.stormreach.weather.ground': 'Ground wetness: %s%%',
 'gui.stormreach.weather.forecast': 'Forecast: %s',
 'gui.stormreach.weather.range': 'Range %s blocks',
 'block.stormreach.tornado_radar.blocked': 'Not enough room: the Doppler radar needs a clear 3x3 space, 7 blocks tall.',
 'block.stormreach.radar_part': 'Doppler Radar',
 'item.stormreach.tornado_radar.tooltip': 'Places a 3x3, 7-block-tall radar with a rotating dish. Use any part to open the radar scope.',
 'item.stormreach.labyrinth_key_gale': 'Gale Key',
 'item.stormreach.labyrinth_key_thunder': 'Thunder Key',
 'item.stormreach.labyrinth_key_hollow': 'Hollow Key',
 'block.stormreach.vault_door': 'Vault Door',
 'block.stormreach.vault_wall': 'Vault Plating',
 'block.stormreach.hollow_lamp': 'Hollow Lamp',
 'block.stormreach.labyrinth_keyhole.wrong': 'This lock only takes the %s.',
 'block.stormreach.labyrinth_keyhole.set': 'The %s is set in this lock.',
 'item.stormreach.labyrinth_key_gale.tooltip': 'One of three keys to the Vault Door at the heart of the maze. It fits only the Gale lock.',
 'item.stormreach.labyrinth_key_thunder.tooltip': 'One of three keys to the Vault Door at the heart of the maze. It fits only the Thunder lock.',
 'item.stormreach.labyrinth_key_hollow.tooltip': 'One of three keys to the Vault Door at the heart of the maze. It fits only the Hollow lock.',
 'item.stormreach.stormreach_field_guide': 'Stormreach Field Guide',
 'item.stormreach.storm_crafters_handbook': "Storm Crafter's Handbook",
 'item.stormreach.stormreach_field_guide.tooltip': 'Everything in Stormreach: weather, tornadoes, the Labyrinth, the realm, rarity tables.',
 'item.stormreach.storm_crafters_handbook.tooltip': 'Every Stormreach recipe, with the real items.',
 'item.stormreach.guidebooks.given': "You received the Stormreach Field Guide and the Storm Crafter's Handbook. Use them to read.",
 'gui.stormreach.guide.kind.shaped': 'Crafting table',
 'gui.stormreach.guide.kind.shapeless': 'Crafting (any arrangement)',
 'gui.stormreach.guide.kind.smelting': 'Furnace',
 'gui.stormreach.guide.kind.blasting': 'Blast furnace',
 'gui.stormreach.guide.kind.smoking': 'Smoker',
 'gui.stormreach.guide.kind.campfire_cooking': 'Campfire',
 'gui.stormreach.guide.kind.stonecutting': 'Stonecutter',
 **{f'subtitles.stormreach.{_k}': _v[0] for _k, _v in __import__('sfx_list').SFX.items()},
 'block.stormreach.sovereign_altar.arena': 'The storm lifts you to the Sovereign\'s arena!',
 'block.stormreach.sovereign_altar.arena_return': 'The storm sets you back down where you stood.',
 'item.stormreach.tornado_director': 'Tornado Director',
 'item.stormreach.tornado_director.tooltip': 'Creative only. Point at the ground and use it to send the nearest tornado there.',
 'item.stormreach.tornado_director.tooltip2': 'Sneak and use to let it wander on its own again.',
 'item.stormreach.tornado_director.tooltip3': 'Waterspouts still won\'t leave the water.',
 'item.stormreach.tornado_director.creative_only': 'The Tornado Director only works in Creative mode.',
 'item.stormreach.tornado_director.none': 'There is no tornado within 512 blocks.',
 'item.stormreach.tornado_director.released': 'The tornado is following its own path again.',
 'item.stormreach.tornado_director.sent': 'Tornado heading for %s, %s (%s blocks away).',
 'key.stormreach.see_inside_tornadoes': 'See inside tornadoes (Creative)',
 'key.stormreach.see_inside_tornadoes.on': 'See-inside view on: tornadoes are see-through and their cores are marked.',
 'key.stormreach.see_inside_tornadoes.off': 'See-inside view off.',
 'key.stormreach.see_inside_tornadoes.creative_only': 'The see-inside view only works in Creative or Spectator mode.',
 'event.stormreach.dispel': 'The storm breaks up. The sky is clearing.',
 'event.stormreach.dispel.clear': 'The sky is already clear.',
 'event.stormreach.supercell': 'A supercell is organising: watch the wall cloud for rotation.',
 'item.stormreach.tornado_spawner.rating': 'Intensity: %s', 'item.stormreach.tornado_spawner.current': 'Intensity: %s',
 'item.stormreach.tornado_spawner.help': 'Use on the ground to spawn. Sneak + use to change the intensity.',
 'item.stormreach.tornado_spawner.cap': 'Too many tornadoes already (see Mod Menu → Stormreach → Tornadoes).',
 # backpacks
 'item.stormreach.backpack.slots': '%s / %s slots used', 'item.stormreach.backpack.magnet': 'Built-in Magnetism',
 'item.stormreach.backpack.open': 'Use, or press B, to open', 'item.stormreach.backpack.none': 'You aren\'t carrying a backpack.',
 'gui.stormreach.backpack.sort': 'Sort', 'gui.stormreach.backpack.page': 'Page %s / %s',
 'key.stormreach.open_backpack': 'Open Backpack', 'key.category.stormreach.stormreach': 'Stormreach',
 # labyrinth
 'block.stormreach.rusted_locker.hidden': 'You are hiding. Press Use to step out.',
 'block.stormreach.rusted_locker.occupied': 'Someone is already hiding in there.',
 'block.stormreach.rusted_locker.found': 'The locker door is torn open.',
 'block.stormreach.labyrinth_keyhole.keys': '%s / 3 keys set',
 'block.stormreach.labyrinth_keyhole.hint': 'A lock in the Vault Door. It takes the %s.',
 # shrine
 'block.stormreach.relic_pedestal.accepted': 'The pedestal takes the relic and begins to glow.',
 'block.stormreach.relic_pedestal.lit': 'The relic rests here, glowing.',
 'block.stormreach.relic_pedestal.riddle1': '"What I left in the deepest room, bring it back." (Heart of the Labyrinth)',
 'block.stormreach.relic_pedestal.riddle2': '"The crowned thing\'s eye, which never closes." (Tempest Eye)',
 'block.stormreach.relic_pedestal.riddle3': '"Fruit of the old grove, dressed in gold." (Gilded Stormfruit)',
 'block.stormreach.relic_pedestal.riddle4': '"What I leave behind when you look at me." (Hollow Remnant)',
 'block.stormreach.storm_conduit.notches': 'This conduit has %s notch(es). Call lightning onto the conduits in order.',
 'block.stormreach.storm_conduit.unready': 'The conduit stays dark. Light every pedestal first.',
 'block.stormreach.storm_conduit.right': 'The conduit drinks the lightning.',
 'block.stormreach.storm_conduit.wrong': 'Wrong order! Every conduit goes dark.',
 'block.stormreach.storm_conduit.complete': 'All four conduits blaze. The altar is waiting.',
 'block.stormreach.hollow_altar.pedestals': 'Four empty pedestals surround the altar.',
 'block.stormreach.hollow_altar.conduits': 'The conduits are dark. They want lightning, one notch at a time.',
 'block.stormreach.hollow_altar.hint': 'A book-shaped hollow is worn into the stone.',
 'entity.stormreach.cyrrus.phase2': 'Phase 2: the rain thickens and the light fails.', 'entity.stormreach.cyrrus.phase3': 'Phase 3: the wind turns against you.',
 'entity.stormreach.cyrrus.phase4': 'Phase 4: Cyrrus calls the squalls.', 'entity.stormreach.cyrrus.phase5': 'Phase 5: Cyrrus becomes the storm.',
 'entity.stormreach.cyrrus.crosswind': 'A crosswind howls across the arena: arrows will be blown off course.',
 'entity.stormreach.cyrrus.maelstrom': 'A maelstrom forms. Get out of the centre!',
 # effects & tabs
 'effect.stormreach.hidden': 'Hidden', 'effect.stormreach.labyrinth': 'Lost in the Labyrinth',
 'enchantment.stormreach.capacity': 'Capacity', 'enchantment.stormreach.magnetism': 'Magnetism', 'enchantment.stormreach.organization': 'Organization',
 'enchantment.stormreach.soulbound': 'Soulbound', 'enchantment.stormreach.resilience': 'Resilience',
 'stat.stormreach.stranger_sightings': 'Strange Sightings', 'biome.stormreach.elderstorm_grove': 'Elderstorm Grove',
}
for k, v in MSG.items(): L(k, v)
for k, v in MORPH.items(): L(f'tornado.stormreach.morph.{k}', v)
for k, v in {'building': 'Stormreach: Building', 'nature': 'Stormreach: Nature', 'equipment': 'Stormreach: Tools & Combat', 'food': 'Stormreach: Food',
             'materials': 'Stormreach: Materials', 'spawn_eggs': 'Stormreach: Spawn Eggs', 'weather_events': 'Stormreach: Weather Events',
             'meteorology': 'Stormreach: Meteorology'}.items(): L(f'itemGroup.stormreach.{k}', v)
CFG = {'tornadoSimulationDistance': ('Tornado Simulation Distance', 'Tornadoes farther than this from every player stop damaging blocks and moving entities.'),
       'maxActiveTornadoes': ('Max Tornadoes at Once', 'Upper limit on live tornadoes per world, natural and spawned.'),
       'tornadoBlockChecksPerTick': ('Block Checks per Tick', 'How many blocks each tornado tests for wind damage per tick. Lower is faster.'),
       'tornadoDebrisLimit': ('Airborne Debris Limit', 'Most falling-block debris a tornado keeps in the air at once.'),
       'tornadoDestruction': ('Tornado Block Damage', 'Off: tornadoes still push and hurt entities but break no blocks.'),
       'tornadoSizePercent': ('Tornado Size', 'Scales tornado width relative to real-world size (100% = real scale).'),
       'particleDensity': ('Particle Density', 'Scales the mod\'s weather and tornado particles.'),
       'backpackMagnetInterval': ('Backpack Magnet Interval', 'Ticks between Magnetism sweeps. Higher is lighter on the server.')}
for k, (n, t) in CFG.items(): L(f'config.stormreach.{k}', n); L(f'config.stormreach.{k}.tooltip', t)
L('config.stormreach.page.tornadoes', 'Tornadoes'); L('config.stormreach.page.effects', 'Effects & Load')

# =====================================================================================
# LOOT
# =====================================================================================
def self_drop(name):
    w(f'{D}/loot_table/blocks/{name}.json', {'type': 'minecraft:block', 'pools': [{'condition': {'type': 'minecraft:survives_explosion'},
        'entries': [{'type': 'minecraft:item', 'name': ns(name)}], 'rolls': 1}], 'random_sequence': f'stormreach:blocks/{name}'})
for b in ['anemometer', 'thermometer', 'weather_station', 'storm_detector', 'tornado_radar', 'tornado_siren', 'weather_monitor'] + TROPHIES + \
         ['elderstorm_log', 'stripped_elderstorm_log', 'elderstorm_planks', 'elderstorm_sapling', 'elderstorm_stairs', 'elderstorm_fence', 'elderstorm_fence_gate',
          'elderstorm_trapdoor', 'elderstorm_button', 'elderstorm_pressure_plate', 'thunderbloom']:
    self_drop(b)
w(f'{D}/loot_table/blocks/elderstorm_slab.json', {'type': 'minecraft:block', 'pools': [{'entries': [{'type': 'minecraft:item',
  'modifier': [{'type': 'minecraft:set_count', 'condition': {'type': 'minecraft:match_block', 'blocks': 'stormreach:elderstorm_slab', 'state': {'type': 'double'}}, 'count': 2},
               {'type': 'minecraft:explosion_decay'}], 'name': 'stormreach:elderstorm_slab'}], 'rolls': 1}], 'random_sequence': 'stormreach:blocks/elderstorm_slab'})
for name, vanilla in (('elderstorm_door', 'oak_door'), ('rusted_locker', 'oak_door')):
    v = rj(f'{VD}/loot_table/blocks/{vanilla}.json')
    w(f'{D}/loot_table/blocks/{name}.json', json.loads(json.dumps(v).replace(f'minecraft:{vanilla}', f'stormreach:{name}').replace(f'minecraft:blocks/{vanilla}', f'stormreach:blocks/{name}')))
leaves = json.dumps(rj(f'{VD}/loot_table/blocks/dark_oak_leaves.json'))
leaves = leaves.replace('minecraft:dark_oak_leaves', 'stormreach:elderstorm_leaves').replace('minecraft:dark_oak_sapling', 'stormreach:elderstorm_sapling') \
               .replace('minecraft:apple', 'stormreach:stormfruit').replace('minecraft:blocks/dark_oak_leaves', 'stormreach:blocks/elderstorm_leaves')
w(f'{D}/loot_table/blocks/elderstorm_leaves.json', json.loads(leaves))
w(f'{D}/loot_table/blocks/sparkfern.json', {'type': 'minecraft:block', 'pools': [{'entries': [{'type': 'minecraft:alternatives', 'children': [
    {'type': 'minecraft:item', 'condition': 'minecraft:tool/can_shear', 'name': 'stormreach:sparkfern'},
    {'type': 'minecraft:item', 'condition': {'type': 'minecraft:random_chance', 'chance': 0.125}, 'name': 'stormreach:thistle_down'}]}], 'rolls': 1}],
    'random_sequence': 'stormreach:blocks/sparkfern'})
w(f'{D}/loot_table/blocks/stormfruit_pod.json', {'type': 'minecraft:block', 'pools': [{'condition': {'type': 'minecraft:match_block',
    'blocks': 'stormreach:stormfruit_pod', 'state': {'age': '2'}}, 'entries': [{'type': 'minecraft:item', 'name': 'stormreach:stormfruit',
    'modifier': [{'type': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 1, 'max': 2}}]}], 'rolls': 1}],
    'random_sequence': 'stormreach:blocks/stormfruit_pod'})
def mob_loot(name, pools): w(f'{D}/loot_table/entities/{name}.json', {'type': 'minecraft:entity', 'pools': pools, 'random_sequence': f'stormreach:entities/{name}'})
def pool(item, lo, hi, looting=True, chance=None, smelt=False):
    mods = [{'type': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'max': hi, 'min': lo}}]
    if smelt: mods.append({'type': 'minecraft:furnace_smelt', 'condition': {'type': 'minecraft:entity_properties', 'entity': 'this', 'predicate': {'flags': {'is_on_fire': True}}}})
    if looting: mods.append({'type': 'minecraft:enchanted_count_increase', 'count': {'type': 'minecraft:uniform', 'max': 1.0, 'min': 0.0}, 'enchantment': 'minecraft:looting'})
    p = {'entries': [{'type': 'minecraft:item', 'modifier': mods, 'name': ns(item)}], 'rolls': 1}
    if chance: p['condition'] = {'type': 'minecraft:all_of', 'terms': [{'type': 'minecraft:killed_by_player'}, {'type': 'minecraft:random_chance_with_enchanted_bonus',
        'enchantment': 'minecraft:looting', 'enchanted_chance': {'type': 'minecraft:linear', 'base': chance + 0.02, 'per_level_above_first': 0.02}, 'unenchanted_chance': chance}]}
    return p
def one(item, n=1): return {'entries': [{'type': 'minecraft:item', 'name': ns(item), **({'modifier': [{'type': 'minecraft:set_count', 'count': n}]} if n > 1 else {})}], 'rolls': 1}
mob_loot('glimmerstag', [pool('minecraft:leather', 0, 2), pool('glimmer_antler', 1, 1, looting=False, chance=0.25)])
mob_loot('thistlehop', [pool('thistle_down', 0, 2)])
mob_loot('hollow_keeper', [one('minecraft:echo_shard', 2), pool('minecraft:iron_ingot', 2, 6), pool('minecraft:copper_ingot', 4, 10)])
mob_loot('cyrrus', [one('maelstrom_core', 2), one('stormcrown'), one('weathervane_scepter'), pool('minecraft:nether_star', 1, 1, looting=False),
                    pool('tempest_core', 2, 4)])
mob_loot('stranger', []); mob_loot('tornado', [])
def entry(i, wt, lo, hi, extra=None):
    e = {'type': 'minecraft:item', 'name': ns(i), 'weight': wt}
    mods = [{'type': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'max': hi, 'min': lo}}] if (lo, hi) != (1, 1) else []
    if extra: mods += extra
    if mods: e['modifier'] = mods
    return e
def chest(name, pools):
    w(f'{D}/loot_table/chests/{name}.json', {'type': 'minecraft:chest', 'pools': pools, 'random_sequence': f'stormreach:chests/{name}'})
def rolls(lo, hi): return {'type': 'minecraft:uniform', 'max': hi, 'min': lo}
ENCH = [{'type': 'minecraft:enchant_randomly', 'options': '#minecraft:on_random_loot'}]
LAB_ROOM = [('minecraft:bone', 20, 1, 4), ('minecraft:string', 15, 1, 3), ('minecraft:rotten_flesh', 15, 1, 3), ('minecraft:iron_nugget', 14, 2, 8),
            ('minecraft:copper_ingot', 10, 1, 4), ('minecraft:torch', 10, 2, 6), ('minecraft:bread', 8, 1, 2), ('minecraft:candle', 6, 1, 2),
            ('storm_shard', 6, 1, 3), ('minecraft:iron_ingot', 4, 1, 2), ('minecraft:echo_shard', 1, 1, 1)]
chest('labyrinth_room', [{'rolls': rolls(2, 4), 'entries': [entry(*e) for e in LAB_ROOM]},
                         {'rolls': 1, 'entries': [{'type': 'minecraft:empty', 'weight': 92}, entry('journal_page_3', 8, 1, 1)]}])
chest('labyrinth_vault', [{'rolls': 1, 'entries': [entry('labyrinth_key', 1, 1, 1)]},
                          {'rolls': rolls(2, 4), 'entries': [entry('minecraft:gold_ingot', 14, 2, 6), entry('minecraft:diamond', 6, 1, 3),
                              entry('minecraft:emerald', 10, 2, 5), entry('storm_cell', 8, 1, 2), entry('minecraft:golden_apple', 5, 1, 1),
                              entry('minecraft:book', 8, 1, 1, ENCH), entry('minecraft:iron_sword', 5, 1, 1, ENCH), entry('minecraft:name_tag', 4, 1, 1),
                              entry('charged_stormstone', 10, 2, 6)]}])
for _k in ('gale', 'thunder', 'hollow'):
    chest(f'labyrinth_vault_{_k}', [{'rolls': 1, 'entries': [entry(f'labyrinth_key_{_k}', 1, 1, 1)]},
                                    {'rolls': rolls(2, 4), 'entries': [entry('minecraft:gold_ingot', 14, 2, 6), entry('minecraft:diamond', 6, 1, 3),
                                        entry('minecraft:emerald', 10, 2, 5), entry('storm_cell', 8, 1, 2), entry('minecraft:golden_apple', 5, 1, 1),
                                        entry('minecraft:book', 8, 1, 1, ENCH), entry('minecraft:name_tag', 4, 1, 1), entry('charged_stormstone', 10, 2, 6)]}])
chest('labyrinth_treasure', [{'rolls': 1, 'entries': [entry('heart_of_the_labyrinth', 1, 1, 1)]},
                             {'rolls': 1, 'entries': [entry('journal_page_4', 1, 1, 1)]},
                             {'rolls': rolls(3, 5), 'entries': [entry('minecraft:diamond', 12, 2, 5), entry('minecraft:netherite_scrap', 4, 1, 2),
                                 entry('minecraft:enchanted_golden_apple', 2, 1, 1), entry('minecraft:totem_of_undying', 3, 1, 1),
                                 entry('minecraft:diamond_chestplate', 4, 1, 1, ENCH), entry('storm_crystal_block', 8, 1, 2), entry('tempest_core', 6, 1, 1),
                                 entry('minecraft:music_disc_otherside', 2, 1, 1), entry('minecraft:experience_bottle', 10, 3, 8)]}])
chest('hollow_shrine', [{'rolls': 1, 'entries': [entry('journal_page_5', 1, 1, 1)]},
                        {'rolls': rolls(2, 4), 'entries': [entry('ion_shard', 14, 2, 6), entry('voltite_ingot', 10, 1, 4), entry('aurorite', 5, 1, 2),
                            entry('gilded_stormfruit', 3, 1, 1), entry('stormfruit', 14, 2, 4), entry('minecraft:experience_bottle', 8, 2, 5)]}])
# journal pages 1 and 2 join existing chests (Sky Vault and Tempest Spire)
for name, pg, chance in (('sky_vault', 'journal_page_1', 12), ('tempest_spire', 'journal_page_2', 15)):
    p = f'{D}/loot_table/chests/{name}.json'; lt = rj(p)
    lt['pools'] = [x for x in lt['pools'] if 'journal' not in json.dumps(x)] + [{'rolls': 1, 'entries': [{'type': 'minecraft:empty', 'weight': 100 - chance}, entry(pg, chance, 1, 1)]}]
    w(p, lt)
# achievement rewards (one item each, never repeated)
for adv_name, item in (('ach/lightning_rod', 'lightning_charm'), ('ach/into_the_cage', 'storm_chaser_cap'), ('ach/ef5', 'chasers_badge'),
                       ('ach/sovereign', 'sovereign_trophy'), ('ach/keeper', 'keeper_trophy'), ('ach/cyrrus', 'cyrrus_trophy'),
                       ('ach/violent', 'tornado_trophy'), ('ach/packrat', 'storm_chasers_pack')):
    w(f'{D}/loot_table/{adv_name}.json', {'type': 'minecraft:advancement_reward', 'pools': [one(item)]})

# =====================================================================================
# RECIPES
# =====================================================================================
def shaped(name, pattern, key, result, count=1, cat='misc', group=None):
    r = {'type': 'minecraft:crafting_shaped', 'category': cat, 'key': {k: ns(v) for k, v in key.items()}, 'pattern': pattern, 'result': {'count': count, 'id': ns(result)}}
    if group: r['group'] = group
    w(f'{D}/recipe/{name}.json', r)
def shapeless(name, ings, result, count=1, cat='misc'):
    w(f'{D}/recipe/{name}.json', {'type': 'minecraft:crafting_shapeless', 'category': cat, 'ingredients': [ns(i) for i in ings], 'result': {'count': count, 'id': ns(result)}})
# meteorology
shaped('anemometer', [' C ', 'CIC', ' I '], {'C': 'minecraft:copper_ingot', 'I': 'minecraft:iron_ingot'}, 'anemometer', 1, 'redstone')
shaped('thermometer', [' G ', ' R ', ' I '], {'G': 'minecraft:glass_pane', 'R': 'minecraft:redstone', 'I': 'minecraft:iron_nugget'}, 'thermometer', 1, 'redstone')
shaped('weather_station', ['ABT', 'ICI', 'IRI'], {'A': 'anemometer', 'B': 'storm_barometer', 'T': 'thermometer', 'I': 'minecraft:iron_ingot',
       'C': 'minecraft:comparator', 'R': 'minecraft:redstone_block'}, 'weather_station', 1, 'redstone')
shaped('storm_detector', ['GSG', 'IRI', 'III'], {'G': 'minecraft:glass', 'S': 'storm_shard', 'I': 'minecraft:iron_ingot', 'R': 'minecraft:redstone'}, 'storm_detector', 1, 'redstone')
shaped('tornado_radar', ['ADA', 'CSC', 'ITI'], {'A': 'glimmer_antler', 'D': 'minecraft:daylight_detector', 'C': 'storm_cell', 'S': 'storm_detector',
       'I': 'minecraft:iron_block', 'T': 'minecraft:observer'}, 'tornado_radar', 1, 'redstone')
shaped('tornado_siren', ['GNG', 'GRG', 'III'], {'G': 'minecraft:gold_ingot', 'N': 'minecraft:note_block', 'R': 'minecraft:redstone_block', 'I': 'minecraft:iron_ingot'},
       'tornado_siren', 1, 'redstone')
shaped('weather_monitor', ['PPP', 'PEP', 'IRI'], {'P': 'minecraft:glass_pane', 'E': 'minecraft:ender_eye', 'I': 'minecraft:iron_ingot', 'R': 'minecraft:redstone'},
       'weather_monitor', 1, 'redstone')
shaped('storm_radar', ['ICI', 'IDI', 'IRI'], {'I': 'minecraft:iron_ingot', 'C': 'minecraft:compass', 'D': 'storm_detector', 'R': 'minecraft:redstone'}, 'storm_radar', 1, 'equipment')
shaped('weather_radio', [' I ', 'NSN', 'IRI'], {'I': 'minecraft:iron_ingot', 'N': 'minecraft:note_block', 'S': 'storm_shard', 'R': 'minecraft:redstone'}, 'weather_radio', 1, 'equipment')
# backpacks: Diamond is crafted, then each kit + the previous backpack makes the next one (contents and enchantments carry over)
shaped('diamond_backpack', ['DLD', 'SCS', 'DLD'], {'D': 'minecraft:diamond', 'L': 'minecraft:leather', 'S': 'storm_shard', 'C': 'minecraft:chest'}, 'diamond_backpack', 1, 'equipment')
KITS = {'obsidian': (['OFO', 'CSC', 'OFO'], {'O': 'minecraft:obsidian', 'F': 'fulgurite', 'C': 'charged_stormstone', 'S': 'storm_cell'}),
        'netherite': (['CBC', 'SNS', 'CBC'], {'C': 'storm_crystal_block', 'B': 'minecraft:blaze_rod', 'S': 'storm_cell', 'N': 'minecraft:netherite_ingot'}),
        'storm_crystal': (['CSC', 'HTH', 'CSC'], {'C': 'storm_crystal_block', 'S': 'storm_cell', 'H': 'charged_stormstone', 'T': 'tempest_core'}),
        'voltite': (['VIV', 'FSF', 'VIV'], {'V': 'voltite_block', 'I': 'ion_shard', 'F': 'stormhawk_feather', 'S': 'storm_cell'}),
        'aurorite': (['AGA', 'IEI', 'AGA'], {'A': 'aurorite_block', 'G': 'gilded_stormfruit', 'I': 'ion_lamp', 'E': 'tempest_eye'}),
        'maelstrom': (['XAX', 'EME', 'XAX'], {'X': 'minecraft:netherite_ingot', 'A': 'aurorite_block', 'E': 'tempest_eye', 'M': 'maelstrom_core'})}
for i, t in enumerate(TIERS[1:]):
    pat, key = KITS[t]
    shaped(f'{t}_backpack_upgrade', pat, key, f'{t}_backpack_upgrade', 1, 'equipment')
    w(f'{D}/recipe/{t}_backpack.json', {'type': 'minecraft:crafting_transmute', 'category': 'equipment', 'group': 'stormreach_backpack',
       'input': f'stormreach:{TIERS[i]}_backpack', 'material': f'stormreach:{t}_backpack_upgrade', 'result': {'id': f'stormreach:{t}_backpack'}})
# story
shapeless('weathered_journal', ['journal_page_1', 'journal_page_2', 'journal_page_3', 'journal_page_4', 'journal_page_5', 'minecraft:book', 'hollow_remnant'],
          'weathered_journal')
shaped('weatherbreaker', [' M ', ' E ', ' S '], {'M': 'maelstrom_core', 'E': 'tempest_eye', 'S': 'minecraft:netherite_sword'}, 'weatherbreaker', 1, 'equipment')
# forest
shaped('gilded_stormfruit', ['GGG', 'GFG', 'GGG'], {'G': 'minecraft:gold_ingot', 'F': 'stormfruit'}, 'gilded_stormfruit', 1, 'misc')
shapeless('stormfruit_preserve', ['stormfruit', 'stormfruit', 'minecraft:sugar', 'minecraft:glass_bottle'], 'stormfruit_preserve', 1, 'misc')
shapeless('string_from_thistle_down', ['thistle_down', 'thistle_down', 'thistle_down'], 'minecraft:string', 1, 'misc')
shapeless('elderstorm_planks', ['#stormreach:elderstorm_logs'], 'elderstorm_planks', 4, 'building')
shaped('elderstorm_stairs', ['#  ', '## ', '###'], {'#': 'elderstorm_planks'}, 'elderstorm_stairs', 4, 'building', 'wooden_stairs')
shaped('elderstorm_slab', ['###'], {'#': 'elderstorm_planks'}, 'elderstorm_slab', 6, 'building', 'wooden_slab')
shaped('elderstorm_fence', ['W#W', 'W#W'], {'W': 'elderstorm_planks', '#': 'minecraft:stick'}, 'elderstorm_fence', 3, 'misc', 'wooden_fence')
shaped('elderstorm_fence_gate', ['#W#', '#W#'], {'W': 'elderstorm_planks', '#': 'minecraft:stick'}, 'elderstorm_fence_gate', 1, 'redstone', 'wooden_fence_gate')
shaped('elderstorm_door', ['##', '##', '##'], {'#': 'elderstorm_planks'}, 'elderstorm_door', 3, 'redstone', 'wooden_door')
shaped('elderstorm_trapdoor', ['###', '###'], {'#': 'elderstorm_planks'}, 'elderstorm_trapdoor', 2, 'redstone', 'wooden_trapdoor')
shapeless('elderstorm_button', ['elderstorm_planks'], 'elderstorm_button', 1, 'redstone')
shaped('elderstorm_pressure_plate', ['##'], {'#': 'elderstorm_planks'}, 'elderstorm_pressure_plate', 1, 'redstone')
shapeless('yellow_dye_from_thunderbloom', ['thunderbloom'], 'minecraft:yellow_dye', 1, 'misc')
# 5.1 guidebooks can always be re-crafted
shapeless('stormreach_field_guide', ['minecraft:book', 'storm_shard'], 'stormreach_field_guide')
shapeless('storm_crafters_handbook', ['minecraft:book', 'minecraft:crafting_table'], 'storm_crafters_handbook')

# =====================================================================================
# TAGS
# =====================================================================================
def bt(t, vals): tag_add(f'{MC}/block/{t}.json', [ns(v) for v in vals])
def it(t, vals): tag_add(f'{MC}/item/{t}.json', [ns(v) for v in vals])
BACKPACKS = [f'{t}_backpack' for t in TIERS] + ['storm_chasers_pack']
w(f'{D}/tags/item/backpacks.json', {'values': [ns(b) for b in BACKPACKS]})
w(f'{D}/tags/item/maelstrom_repair_materials.json', {'values': ['stormreach:maelstrom_core']})
for k in ('block', 'item'):
    w(f'{D}/tags/{k}/elderstorm_logs.json', {'values': ['stormreach:elderstorm_log', 'stormreach:stripped_elderstorm_log']})
INSTR = ['anemometer', 'thermometer', 'weather_station', 'storm_detector', 'tornado_radar', 'tornado_siren', 'weather_monitor']
bt('mineable/pickaxe', INSTR + ['rusted_locker'] + TROPHIES); bt('needs_iron_tool', ['rusted_locker'])
ELDER = ['elderstorm_log', 'stripped_elderstorm_log', 'elderstorm_planks', 'elderstorm_stairs', 'elderstorm_slab', 'elderstorm_fence', 'elderstorm_fence_gate',
         'elderstorm_door', 'elderstorm_trapdoor', 'elderstorm_button', 'elderstorm_pressure_plate']
bt('mineable/axe', ELDER + ['stormfruit_pod']); bt('mineable/hoe', ['elderstorm_leaves'])
for t, vals in (('logs', ['#stormreach:elderstorm_logs']), ('logs_that_burn', ['#stormreach:elderstorm_logs']), ('planks', ['elderstorm_planks']),
                ('wooden_stairs', ['elderstorm_stairs']), ('wooden_slabs', ['elderstorm_slab']), ('wooden_fences', ['elderstorm_fence']),
                ('fence_gates', ['elderstorm_fence_gate']), ('wooden_doors', ['elderstorm_door']), ('wooden_trapdoors', ['elderstorm_trapdoor']),
                ('wooden_buttons', ['elderstorm_button']), ('wooden_pressure_plates', ['elderstorm_pressure_plate']), ('leaves', ['elderstorm_leaves']),
                ('saplings', ['elderstorm_sapling']), ('small_flowers', ['thunderbloom'])):
    bt(t, vals); it(t, vals)
bt('replaceable_by_trees', ['sparkfern']); bt('replaceable', ['sparkfern']); bt('sword_efficient', ['sparkfern', 'stormfruit_pod'])
bt('dragon_immune', ['relic_pedestal', 'storm_conduit', 'hollow_altar', 'labyrinth_seal', 'labyrinth_keyhole'])
bt('wither_immune', ['relic_pedestal', 'storm_conduit', 'hollow_altar', 'labyrinth_seal', 'labyrinth_keyhole'])
it('head_armor', ['stormcrown', 'storm_chaser_cap']); it('swords', ['weatherbreaker'])
it('enchantable/head_armor', ['stormcrown', 'storm_chaser_cap']); it('enchantable/armor', ['stormcrown', 'storm_chaser_cap'])
it('enchantable/sword', ['weatherbreaker']); it('enchantable/weapon', ['weatherbreaker']); it('enchantable/durability', ['weatherbreaker', 'stormcrown', 'storm_chaser_cap'])
it('enchantable/sharp_weapon', ['weatherbreaker']); it('enchantable/fire_aspect', ['weatherbreaker'])
it('enchantable/vanishing', BACKPACKS)
it('fox_food', ['stormfruit'])
tag_add(f'{C}/item/foods/fruit.json', ['stormreach:stormfruit', 'stormreach:gilded_stormfruit'])
tag_add(f'{C}/item/foods.json', ['stormreach:stormfruit', 'stormreach:gilded_stormfruit', 'stormreach:stormfruit_preserve'])

# =====================================================================================
# ENCHANTMENTS (effects are implemented in code; see backpack/BackpackEnchants)
# =====================================================================================
def enchant(name, max_level, weight, anvil, min_c, max_c):
    w(f'{D}/enchantment/{name}.json', {'anvil_cost': anvil, 'description': {'translate': f'enchantment.stormreach.{name}'}, 'effects': {},
       'max_cost': {'base': max_c, 'per_level_above_first': 10}, 'max_level': max_level, 'min_cost': {'base': min_c, 'per_level_above_first': 10},
       'slots': ['any'], 'supported_items': '#stormreach:backpacks', 'weight': weight})
enchant('capacity', 3, 5, 4, 10, 60); enchant('magnetism', 1, 3, 4, 15, 65); enchant('organization', 1, 4, 2, 5, 55)
enchant('soulbound', 1, 1, 8, 25, 75); enchant('resilience', 1, 3, 4, 15, 65)
tag_add(f'{MC}/enchantment/in_enchanting_table.json', ['stormreach:capacity', 'stormreach:magnetism', 'stormreach:organization', 'stormreach:resilience'])
tag_add(f'{MC}/enchantment/non_treasure.json', ['stormreach:capacity', 'stormreach:magnetism', 'stormreach:organization', 'stormreach:resilience'])
tag_add(f'{MC}/enchantment/treasure.json', ['stormreach:soulbound'])
tag_add(f'{MC}/enchantment/tradeable.json', ['stormreach:capacity', 'stormreach:magnetism', 'stormreach:organization', 'stormreach:soulbound', 'stormreach:resilience'])
tag_add(f'{MC}/enchantment/on_random_loot.json', ['stormreach:capacity', 'stormreach:magnetism', 'stormreach:organization', 'stormreach:soulbound', 'stormreach:resilience'])

# =====================================================================================
# WORLDGEN: Elderstorm Grove (Tempest Realm), the Hollow Labyrinth (Overworld), the Hollow Shrine (Tempest Realm)
# =====================================================================================
def wf(name, obj): w(f'{WG}/feature/{name}.json', obj)
def wp(name, feature, placement): w(f'{WG}/placed_feature/{name}.json', {'feature': ns(feature), 'placement': placement})
w(f'{WG}/block_state_provider/stormfruit_pod.json', {'type': 'minecraft:randomized_int', 'property': 'age',
   'source': {'id': 'stormreach:stormfruit_pod', 'properties': {'age': '0'}}, 'values': {'type': 'minecraft:uniform', 'max_inclusive': 2, 'min_inclusive': 0}})
SKYSOIL_BELOW = {'type': 'minecraft:rule_based', 'rules': [{'if_true': {'type': 'minecraft:not', 'predicate': {'type': 'minecraft:matching_block_tag',
                 'tag': 'minecraft:cannot_replace_below_tree_trunk'}}, 'then': {'id': 'stormreach:skysoil'}}]}
POD = {'type': 'minecraft:attached_to_leaves', 'block_provider': 'stormreach:stormfruit_pod', 'directions': ['down'], 'exclusion_radius_xz': 1,
       'exclusion_radius_y': 0, 'probability': 0.18, 'required_empty_blocks': 1}
LEAVES = {'id': 'stormreach:elderstorm_leaves', 'properties': {'distance': '7', 'persistent': 'false', 'waterlogged': 'false'}}
LOG = {'id': 'stormreach:elderstorm_log', 'properties': {'axis': 'y'}}
t = rj(f'{VD}/worldgen/feature/fancy_oak.json')
t.update({'trunk_provider': LOG, 'foliage_provider': LEAVES, 'below_trunk_provider': SKYSOIL_BELOW, 'decorators': [POD]})
t['trunk_placer']['base_height'] = 6; t['foliage_placer']['radius'] = 3
wf('elderstorm', t)
g = rj(f'{VD}/worldgen/feature/mega_jungle_tree.json')
g.update({'trunk_provider': LOG, 'foliage_provider': LEAVES, 'below_trunk_provider': SKYSOIL_BELOW, 'decorators': [POD], 'ignore_vines': True})
g['trunk_placer'] = {'type': 'minecraft:mega_jungle_trunk_placer', 'base_height': 16, 'height_rand_a': 4, 'height_rand_b': 10}
g['foliage_placer'] = {'type': 'minecraft:jungle_foliage_placer', 'height': 3, 'offset': 0, 'radius': 3}
wf('giant_elderstorm', g)
SURVIVE = {'type': 'minecraft:block_predicate_filter', 'predicate': {'type': 'minecraft:would_survive', 'state': {'id': 'stormreach:elderstorm_sapling', 'properties': {'stage': '0'}}}}
wp('elderstorm_checked', 'elderstorm', [SURVIVE]); wp('giant_elderstorm_checked', 'giant_elderstorm', [SURVIVE])
wf('trees_elderstorm', {'type': 'minecraft:random_selector', 'default': 'stormreach:elderstorm_checked', 'features': [{'chance': 0.4, 'feature': 'stormreach:giant_elderstorm_checked'}]})
wp('trees_elderstorm', 'trees_elderstorm', [{'type': 'minecraft:count', 'count': {'type': 'minecraft:weighted_list', 'distribution': [{'data': 4, 'weight': 6}, {'data': 6, 'weight': 3}, {'data': 2, 'weight': 1}]}},
    {'type': 'minecraft:in_square'}, {'type': 'minecraft:heightmap', 'heightmap': 'OCEAN_FLOOR'}, {'type': 'minecraft:biome'}])
def patch(name, block, chance, count, spread):
    wf(name, {'type': 'minecraft:simple_block', 'to_place': {'id': ns(block)}})
    tri = lambda m: {'type': 'minecraft:trapezoid', 'max': m, 'min': -m, 'plateau': 0}
    wp(name, name, [{'type': 'minecraft:rarity_filter', 'chance': chance}, {'type': 'minecraft:in_square'},
        {'type': 'minecraft:heightmap', 'heightmap': 'MOTION_BLOCKING'}, {'type': 'minecraft:biome'}, {'type': 'minecraft:count', 'count': count},
        {'type': 'minecraft:offset', 'x': tri(spread), 'y': tri(3), 'z': tri(spread)},
        {'type': 'minecraft:block_predicate_filter', 'predicate': {'type': 'minecraft:all_of', 'predicates': [
            {'type': 'minecraft:matching_block_tag', 'tag': 'minecraft:air'}, {'type': 'minecraft:would_survive', 'state': {'id': ns(block)}}]}}])
patch('patch_thunderbloom', 'thunderbloom', 3, 16, 6); patch('patch_sparkfern', 'sparkfern', 1, 40, 7)
def cnt(t_, lo, hi, wt): return {'type': t_, 'count': {'type': 'minecraft:uniform', 'max_inclusive': hi, 'min_inclusive': lo}, 'weight': wt}
grove = rj(f'{WG}/biome/galewood_highlands.json')
grove['attributes']['minecraft:gameplay/natural_mob_spawns']['argument']['spawns_by_category'] = {
    'creature': [cnt('stormreach:glimmerstag', 2, 4, 12), cnt('stormreach:thistlehop', 2, 5, 14), cnt('stormreach:puffling', 1, 2, 3)],
    'monster': [cnt('stormreach:static_wisp', 1, 2, 40), cnt('stormreach:stormhawk', 1, 1, 10)]}
grove['attributes']['minecraft:visual/sky_color'] = '#3f5f8c'; grove['attributes']['minecraft:visual/fog_color'] = '#4a6f8f'
grove['attributes']['minecraft:visual/ambient_particles'] = {'argument': [{'particle': {'type': 'minecraft:glow'}, 'probability': 0.003},
    {'particle': {'type': 'minecraft:electric_spark'}, 'probability': 0.002}], 'modifier': 'append'}
grove['effects'] = {'water_color': '#5b8fd8', 'grass_color': '#3fa0a8', 'foliage_color': '#3fa0a8'}
feats = [[] for _ in range(11)]
feats[2] = ['stormreach:cloud_nimbus']; feats[6] = ['stormreach:ore_voltite', 'stormreach:ore_aurorite', 'stormreach:ore_ion']
feats[9] = ['stormreach:trees_elderstorm', 'stormreach:patch_sparkfern', 'stormreach:patch_thunderbloom', 'stormreach:patch_skybloom']
grove['features'] = feats
w(f'{WG}/biome/elderstorm_grove.json', grove)
tag_add(f'{D}/tags/worldgen/biome/is_tempest_realm.json', ['stormreach:elderstorm_grove'])
dim = rj(f'{D}/dimension/tempest_realm.json'); bl = dim['generator']['biome_source']['biomes']
bl[:] = [b for b in bl if b['biome'] != 'stormreach:elderstorm_grove']
full = [-1.0, 1.0]
bl.append({'biome': 'stormreach:elderstorm_grove', 'parameters': {'continentalness': full, 'depth': 0.0, 'erosion': full, 'humidity': [0.45, 1.0],
           'offset': 0.0, 'temperature': [-0.3, 0.35], 'weirdness': full}})
w(f'{D}/dimension/tempest_realm.json', dim)

NO_SPAWNS = {c: {'bounding_box': 'full', 'spawns': []} for c in ('monster', 'creature', 'ambient', 'axolotls', 'underground_water_creature', 'water_creature', 'water_ambient', 'misc')}
w(f'{WG}/structure/hollow_labyrinth.json', {'type': 'stormreach:labyrinth', 'biomes': '#stormreach:has_structure/hollow_labyrinth', 'spawn_overrides': NO_SPAWNS,
   'step': 'underground_structures', 'terrain_adaptation': 'none'})
w(f'{D}/tags/worldgen/biome/has_structure/hollow_labyrinth.json', {'values': ['#minecraft:is_overworld']})
w(f'{WG}/structure_set/hollow_labyrinth.json', {'placement': {'type': 'minecraft:random_spread', 'salt': 1777215, 'separation': 24, 'spacing': 64},
   'structures': [{'structure': 'stormreach:hollow_labyrinth', 'weight': 1}]})

def build_nbt(name, size, b, chests):
    palette = []; pindex = {}
    def st(key):
        bid, props = key; k = (bid, tuple(sorted(props.items())))
        if k not in pindex:
            e = {'id': T.String(bid)}
            if props: e['properties'] = T.Compound({kk: T.String(vv) for kk, vv in props.items()})
            pindex[k] = len(palette); palette.append(T.Compound(e))
        return pindex[k]
    blocks = []
    for pos, key in sorted(b.items()):
        e = {'pos': T.List[T.Int]([T.Int(v) for v in pos]), 'state': T.Int(st(key))}
        if pos in chests: e['nbt'] = T.Compound({'LootTable': T.String(chests[pos]), 'components': T.Compound({}), 'id': T.String('minecraft:chest')})
        blocks.append(T.Compound(e))
    root = T.Compound({'size': T.List[T.Int]([T.Int(v) for v in size]), 'entities': T.List[T.Compound]([]), 'blocks': T.List[T.Compound](blocks),
                       'palette': T.List[T.Compound](palette), 'DataVersion': T.Int(5023)})
    os.makedirs(f'{D}/structure', exist_ok=True)
    nbtlib.File(root, root_name='').save(f'{D}/structure/{name}.nbt', gzipped=True)
def S(bid, **p): return (bid if ':' in bid else 'stormreach:' + bid, p)
rnd = random.Random(8001); b = {}; chests = {}; W = 17; H = 9; c = 8
for x in range(W):
    for z in range(W):
        for y in range(H): b[(x, y, z)] = S('minecraft:air')
for x in range(W):
    for z in range(W):
        r = math.hypot(x - c, z - c)
        if r <= 8.4: b[(x, 2, z)] = S('skyshale_bricks') if rnd.random() > 0.12 else S('cobbled_skyshale')
        if r <= 7.0: b[(x, 1, z)] = S('skyshale')
        if r <= 4.5: b[(x, 0, z)] = S('skyshale') if rnd.random() > 0.15 else S('ion_ore')
        if r <= 2.2: b[(x, 2, z)] = S('chiseled_tempest_bricks')
for k in range(-6, 7):  # a cross of tempest bricks
    b[(c + k, 2, c)] = S('tempest_bricks'); b[(c, 2, c + k)] = S('tempest_bricks')
b[(c, 3, c)] = S('hollow_altar')
for i, (x, z) in enumerate(((c - 4, c - 4), (c + 4, c - 4), (c + 4, c + 4), (c - 4, c + 4))):
    b[(x, 3, z)] = S('relic_pedestal', relic=str(i + 1), lit='false')
for (x, z), order in (((c, c - 7), 1), ((c + 7, c), 3), ((c, c + 7), 2), ((c - 7, c), 4)):  # strike order is not clockwise: read the notches
    b[(x, 3, z)] = S('storm_conduit', order=str(order), struck='false')
for a in range(8):  # ring of broken pillars
    ang = a * math.pi / 4 + math.pi / 8; x = int(round(c + math.cos(ang) * 6.5)); z = int(round(c + math.sin(ang) * 6.5))
    for y in range(3, 3 + rnd.randint(1, 4)): b[(x, y, z)] = S('chiseled_skyshale') if y == 3 else S('skyshale_bricks')
b[(c + 1, 3, c + 2)] = S('minecraft:chest', facing='south', type='single', waterlogged='false'); chests[(c + 1, 3, c + 2)] = 'stormreach:chests/hollow_shrine'
for (x, z) in ((c - 2, c - 2), (c + 2, c + 2), (c - 2, c + 2), (c + 2, c - 2)): b[(x, 3, z)] = S('minecraft:soul_lantern', hanging='false', waterlogged='false')
build_nbt('hollow_shrine', (W, H, W), b, chests)
w(f'{WG}/template_pool/hollow_shrine.json', {'elements': [{'element': {'element_type': 'minecraft:single_pool_element', 'location': 'stormreach:hollow_shrine',
    'processors': {'processors': []}, 'projection': 'rigid'}, 'weight': 1}], 'fallback': 'minecraft:empty'})
w(f'{WG}/structure/hollow_shrine.json', {'type': 'minecraft:jigsaw', 'biomes': '#stormreach:is_tempest_realm', 'max_distance_from_center': 80,
    'size': 1, 'spawn_overrides': {}, 'start_height': {'type': 'minecraft:uniform', 'max_inclusive': {'absolute': 175}, 'min_inclusive': {'absolute': 130}},
    'start_pool': 'stormreach:hollow_shrine', 'step': 'surface_structures', 'terrain_adaptation': 'none', 'use_expansion_hack': False})
w(f'{WG}/structure_set/hollow_shrine.json', {'placement': {'type': 'minecraft:random_spread', 'salt': 666013, 'separation': 40, 'spacing': 120},
    'structures': [{'structure': 'stormreach:hollow_shrine', 'weight': 1}]})

# =====================================================================================
# ACHIEVEMENTS: four tabs (main Stormreach tab additions, Meteorology, the Hollow Labyrinth, the Chronicle)
# Code-awarded ones (StormAdvancements.award) use a single "trigger" criterion of type minecraft:impossible.
# Each reward item is granted by exactly one advancement.
# =====================================================================================
ADV_LIST = []
def adv(name, parent, icon, crit, title, desc, frame='task', hidden=False, reqs=None, reward=None, xp=None, root_bg=None):
    d = {'criteria': crit, 'display': {'title': {'translate': f'advancements.stormreach.{name}.title'},
         'description': {'translate': f'advancements.stormreach.{name}.description'}, 'icon': {'id': ns(icon)}, 'frame': frame, 'hidden': hidden},
         'requirements': reqs or [list(crit)]}
    if parent: d['parent'] = ns(parent)
    if root_bg: d['display'].update({'background': f'stormreach:gui/advancements/backgrounds/{root_bg}', 'show_toast': True, 'announce_to_chat': True})
    rw = {}
    if reward: rw['loot'] = [f'stormreach:ach/{reward}']
    if xp: rw['experience'] = xp
    if rw: d['rewards'] = rw
    w(f'{D}/advancement/{name}.json', d)
    L(f'advancements.stormreach.{name}.title', title); L(f'advancements.stormreach.{name}.description', desc)
    ADV_LIST.append((name, parent, title, desc, frame, hidden, reward, xp))
CODE = {'trigger': {'trigger': 'minecraft:impossible'}}
def has(i, count=None):
    c = {'items': ns(i) if isinstance(i, str) else [ns(x) for x in i]}
    if count: c['count'] = {'min': count}
    return {'conditions': {'items': [c]}, 'trigger': 'minecraft:inventory_changed'}
def killed(e): return {'conditions': {'entity': {'type': 'minecraft:entity_properties', 'entity': 'this', 'predicate': {'minecraft:entity_type': ns(e)}}},
                       'trigger': 'minecraft:player_killed_entity'}
def loc(struct): return {'conditions': {'player': {'type': 'minecraft:entity_properties', 'entity': 'this', 'predicate': {'minecraft:location': {'structures': struct}}}}, 'trigger': 'minecraft:location'}
def in_biome(bio): return {'conditions': {'player': {'type': 'minecraft:entity_properties', 'entity': 'this', 'predicate': {'minecraft:location': {'biomes': bio}}}}, 'trigger': 'minecraft:location'}

# ---- main tab: backpacks
adv('open_backpack', 'root', 'diamond_backpack', CODE, 'Pack Mule', 'Open a backpack')
prev = 'open_backpack'
BP_TITLES = {'obsidian': ('Volcanic Luggage', 'Upgrade to an Obsidian Backpack'), 'netherite': ('Fireproof Freight', 'Upgrade to a Netherite Backpack'),
             'storm_crystal': ('Bottled Lightning, Packed Neatly', 'Upgrade to a Storm Crystal Backpack'), 'voltite': ('Current Carrier', 'Upgrade to a Voltite Backpack'),
             'aurorite': ('Northern Satchel', 'Upgrade to an Aurorite Backpack'), 'maelstrom': ('The Eye of the Bag', 'Upgrade to a Maelstrom Backpack')}
for t in TIERS[1:]:
    ttl, dsc = BP_TITLES[t]
    adv(f'backpack_{t}', prev, f'{t}_backpack', {'b': has(f'{t}_backpack')}, ttl, dsc,
        frame='challenge' if t == 'maelstrom' else 'goal' if t in ('netherite', 'aurorite') else 'task',
        reward='packrat' if t == 'storm_crystal' else None, xp={'netherite': 100, 'storm_crystal': 150, 'aurorite': 250, 'maelstrom': 750}.get(t))
    prev = f'backpack_{t}'
adv('backpack_soulbound', 'open_backpack', 'diamond_backpack', {'s': {'conditions': {'items': [{'items': '#stormreach:backpacks',
    'predicates': {'minecraft:enchantments': [{'enchantments': 'stormreach:soulbound'}]}}]}, 'trigger': 'minecraft:inventory_changed'}},
    'Till Death Do Us Pack', 'Hold a backpack enchanted with Soulbound', hidden=True)
# ---- main tab: vanilla weather
adv('struck_by_lightning', 'root', 'lightning_charm', {'h': {'conditions': {'damage': {'type': {'tags': [{'id': '#minecraft:is_lightning', 'expected': True}]}}},
    'trigger': 'minecraft:entity_hurt_player'}}, 'Lightning Rod', 'Be struck by lightning and live', reward='lightning_rod')
adv('channeler', 'struck_by_lightning', 'minecraft:trident', {'c': {'trigger': 'minecraft:channeled_lightning'}}, 'Conductor', 'Call lightning down with a Channeling trident')
adv('rod_placed', 'struck_by_lightning', 'minecraft:lightning_rod', {'p': {'conditions': {'location': {'type': 'minecraft:match_block', 'blocks': 'minecraft:lightning_rod'}}, 'trigger': 'minecraft:placed_block'}}, 'Better Safe', 'Place a Lightning Rod')
# ---- main tab: Elderstorm Grove
adv('elderstorm_grove', 'tempest_realm', 'elderstorm_sapling', {'b': in_biome('stormreach:elderstorm_grove')}, 'Old Growth', 'Find an Elderstorm Grove in the Tempest Realm')
adv('pick_stormfruit', 'elderstorm_grove', 'stormfruit', CODE, 'Shocking Snack', 'Pick a ripe Stormfruit')
adv('gilded_stormfruit', 'pick_stormfruit', 'gilded_stormfruit', {'g': has('gilded_stormfruit')}, 'Gold Leaf', 'Make a Gilded Stormfruit')
adv('breed_glimmerstag', 'elderstorm_grove', 'glimmer_antler', {'b': {'conditions': {'child': {'type': 'minecraft:entity_properties', 'entity': 'this',
    'predicate': {'minecraft:entity_type': 'stormreach:glimmerstag'}}}, 'trigger': 'minecraft:bred_animals'}}, 'Fawn Over It', 'Breed two Glimmerstags with Stormfruit')
adv('thistle_down', 'elderstorm_grove', 'thistle_down', {'t': has('thistle_down')}, 'Gone With the Wind', 'Collect Thistle Down from a Thistlehop')
# the Sovereign now leaves a trophy behind
sov = rj(f'{D}/advancement/tempest_sovereign.json'); sov['rewards'] = {'loot': ['stormreach:ach/sovereign'], 'experience': 200}; w(f'{D}/advancement/tempest_sovereign.json', sov)

# ---- Meteorology tab
METEO = ['anemometer', 'thermometer', 'storm_barometer', 'wind_vane', 'weather_station', 'storm_detector', 'tornado_radar', 'tornado_siren', 'weather_monitor', 'storm_radar', 'weather_radio']
adv('meteorology', None, 'weather_station', {'i': has(METEO)}, 'Stormreach: Meteorology', 'Build or find any weather instrument', root_bg='meteorology')
adv('forecaster_1', 'meteorology', 'anemometer', {'a': has('anemometer')}, 'Forecaster I: Which Way the Wind Blows', 'Make an Anemometer')
adv('forecaster_2', 'forecaster_1', 'weather_station', {'s': has('weather_station')}, 'Forecaster II: Station Chief', 'Build a Weather Station')
adv('forecaster_3', 'forecaster_2', 'storm_detector', {'d': has('storm_detector')}, 'Forecaster III: Something on the Scope', 'Build a Storm Detector')
adv('forecaster_4', 'forecaster_3', 'tornado_radar', {'r': has('tornado_radar')}, 'Forecaster IV: Hook Echo', 'Build a Doppler Radar', frame='goal', xp=100)
adv('forecaster_5', 'forecaster_4', 'weather_monitor', {'m': has('weather_monitor')}, 'Forecaster V: The Whole Picture', 'Build a Weather Monitor', xp=150)
adv('weather_radio', 'meteorology', 'weather_radio', {'r': has(['weather_radio', 'storm_radar'])}, 'This Is a Test', 'Carry a Weather Radio or a Handheld Storm Radar')
adv('tornado_siren', 'weather_radio', 'tornado_siren', {'s': has('tornado_siren')}, 'Civil Defence', 'Build a Tornado Siren')
adv('witness_tornado', 'meteorology', 'tornado_spawner_cone', CODE, 'Funnel Cloud', 'Be within 400 blocks of a tornado as it touches down')
adv('tornado_survey', 'witness_tornado', 'tornado_spawner_rope', CODE, 'Damage Survey', 'Be near a tornado when it lifts and read its EF rating')
adv('violent_tornado', 'tornado_survey', 'tornado_spawner_wedge', CODE, 'Violent', 'Witness a tornado rated EF4 or stronger', frame='goal', reward='violent', xp=250)
adv('ef5_tornado', 'violent_tornado', 'tornado_spawner_large_wedge', CODE, 'Incredible', 'Witness a tornado rated EF5', frame='challenge', reward='ef5', xp=500)
adv('inside_tornado', 'witness_tornado', 'storm_chaser_cap', CODE, 'Into the Bear\'s Cage', 'Get caught inside a tornado\'s wind field and survive', frame='goal', hidden=True, reward='into_the_cage')

# ---- Hollow Labyrinth tab
adv('enter_labyrinth', None, 'rusted_locker', CODE, 'The Hollow Labyrinth', 'Find your way into a Hollow Labyrinth', root_bg='labyrinth')
adv('hide_locker', 'enter_labyrinth', 'rusted_locker', CODE, 'Hold Still', 'Hide inside a Rusted Locker')
adv('hide_survived', 'hide_locker', 'rusted_locker', CODE, 'Holding My Breath', 'Stay hidden while the Hollow Keeper searches right outside your locker', frame='goal', hidden=True, xp=100)
adv('labyrinth_key', 'enter_labyrinth', 'labyrinth_key', {'k': has('labyrinth_key')}, 'Skeleton Key', 'Find a Labyrinth Key in a corner vault')
adv('three_keys', 'labyrinth_key', 'labyrinth_key', {'k': has('labyrinth_key', 3)}, 'Three Corners', 'Hold all three Labyrinth Keys')
adv('labyrinth_unsealed', 'three_keys', 'labyrinth_keyhole', CODE, 'Unsealed', 'Set three keys in the Labyrinth Keyhole', frame='goal')
adv('heart_of_the_labyrinth', 'labyrinth_unsealed', 'heart_of_the_labyrinth', {'h': has('heart_of_the_labyrinth')}, 'A Heart Left Behind',
    'Take the Heart of the Labyrinth from the treasure chamber', frame='goal', xp=200)
adv('kill_keeper', 'enter_labyrinth', 'keeper_trophy', {'k': killed('hollow_keeper')}, 'Keeper of Nothing', 'Defeat the Hollow Keeper', frame='challenge', reward='keeper', xp=300)

# ---- Chronicle tab (the story)
adv('stranger_seen', None, 'stranger_spawn_egg', CODE, 'Someone in the Trees', 'See something that shouldn\'t be there', root_bg='chronicle')
adv('stranger_five', 'stranger_seen', 'hollow_remnant', CODE, 'Followed', 'Catch sight of the Stranger five times', hidden=True)
adv('hollow_remnant', 'stranger_five', 'hollow_remnant', {'r': has('hollow_remnant')}, 'What It Leaves Behind', 'Pick up a Hollow Remnant')
adv('journal_page', 'stranger_seen', 'journal_page_1', {'p': has([f'journal_page_{i}' for i in range(1, 6)])}, 'Dear Diary', 'Find a torn journal page')
adv('all_pages', 'journal_page', 'journal_page_5', {f'p{i}': has(f'journal_page_{i}') for i in range(1, 6)}, 'Every Last Page',
    'Collect all five journal pages', frame='goal', reqs=[[f'p{i}'] for i in range(1, 6)])
adv('weathered_journal', 'all_pages', 'weathered_journal', {'j': has('weathered_journal')}, 'Bound Again', 'Rebind the Weathered Journal')
adv('hollow_shrine', 'stranger_seen', 'hollow_altar', {'l': loc('stormreach:hollow_shrine')}, 'Where It Waits', 'Find the Hollow Shrine in the Tempest Realm', frame='goal')
adv('shrine_ritual', 'hollow_shrine', 'storm_conduit', CODE, 'The Ritual', 'Light every pedestal, strike every conduit, and lay the journal on the altar', frame='goal')
adv('defeat_cyrrus', 'shrine_ritual', 'cyrrus_trophy', CODE, 'Let the Sky Decide', 'Defeat Cyrrus, the Hollow Storm', frame='challenge', reward='cyrrus', xp=1000)
adv('stormcrown', 'defeat_cyrrus', 'stormcrown', {'c': has('stormcrown')}, 'Don\'t Ask It for Anything', 'Claim the Stormcrown')
adv('weatherbreaker', 'defeat_cyrrus', 'weatherbreaker', {'w': has('weatherbreaker')}, 'Weatherbreaker', 'Forge the Weatherbreaker from a Maelstrom Core')
adv('stormreach_complete', 'defeat_cyrrus', 'maelstrom_core', {'sov': killed('tempest_sovereign'), 'keep': killed('hollow_keeper'), 'cyr': killed('cyrrus'),
    'pack': has('maelstrom_backpack')}, 'Eye of the Hollow Storm', 'Defeat the Sovereign, the Keeper and Cyrrus, and carry a Maelstrom Backpack',
    frame='challenge', hidden=True, reqs=[['sov'], ['keep'], ['cyr'], ['pack']], xp=2000)
json.dump([{'id': a[0], 'parent': a[1], 'title': a[2], 'description': a[3], 'frame': a[4], 'hidden': a[5], 'reward': a[6], 'xp': a[7]} for a in ADV_LIST],
          open(f'{ROOT}/tools/advancements5.json', 'w'), indent=1)

w(f'{A}/lang/en_us.json', lang)
print('data8 done')
