"""Stormreach 3.0: generated storm counterparts for every vanilla block and mob, stormwater and stormlava,
Nether and End storm biomes."""
import os as _os, pathlib as _pl
ROOT = str(_pl.Path(__file__).resolve().parent.parent)  # the Stormreach project folder
VANILLA = _os.environ.get('STORMREACH_VANILLA', str(_pl.Path(ROOT) / '.vanilla'))  # extracted 26.3 jar (assets/ + data/)
MCJAR = _os.environ.get('STORMREACH_MCJAR', str(_pl.Path(ROOT) / '.vanilla' / 'minecraft-26.3.jar'))
import json, os, re, glob, copy
from PIL import Image
R = ROOT + '/src/main/resources' if 'ROOT' in globals() else '/home/claude/stormreach/src/main/resources'
VA = (VANILLA if 'VANILLA' in globals() else '/home/claude/mc/vd') + '/assets/minecraft'
VD = (VANILLA if 'VANILLA' in globals() else '/home/claude/mc/vd') + '/data/minecraft'
A = f'{R}/assets/stormreach'; D = f'{R}/data/stormreach'; MCT = f'{R}/data/minecraft/tags'; CT = f'{R}/data/c/tags'
TINT = -6310696  # 0xFF9FB4D8 as signed ARGB
def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f: json.dump(obj, f, indent=1); f.write('\n')
def rj(p): return json.load(open(p))
vlang = rj(f'{VA}/lang/en_us.json')

# ---------------- which blocks get a storm counterpart ----------------
SKIP_EXACT = {'air', 'cave_air', 'void_air', 'water', 'lava', 'bubble_column', 'fire', 'soul_fire', 'nether_portal', 'end_portal',
    'end_gateway', 'structure_void', 'light', 'barrier', 'moving_piston', 'piston_head', 'frosted_ice', 'tripwire', 'chest',
    'trapped_chest', 'ender_chest', 'furnace', 'blast_furnace', 'smoker', 'beacon', 'hopper', 'dispenser', 'dropper', 'barrel',
    'brewing_stand', 'lectern', 'jukebox', 'spawner', 'trial_spawner', 'vault', 'conduit', 'beehive', 'bee_nest', 'sculk_sensor',
    'calibrated_sculk_sensor', 'sculk_shrieker', 'sculk_catalyst', 'comparator', 'daylight_detector', 'command_block',
    'chain_command_block', 'repeating_command_block', 'structure_block', 'jigsaw', 'crafter', 'decorated_pot', 'chiseled_bookshelf',
    'enchanting_table', 'bell', 'creaking_heart', 'test_block', 'test_instance_block', 'suspicious_sand', 'suspicious_gravel',
    'flower_pot', 'cauldron', 'water_cauldron', 'lava_cauldron', 'powder_snow_cauldron', 'cake', 'campfire', 'soul_campfire',
    'end_portal_frame', 'dragon_egg', 'reinforced_deepslate', 'bedrock', 'redstone_wire', 'kelp', 'kelp_plant'}
SKIP_PATTERNS = [r'potted_', r'infested_', r'candle_cake$', r'_sign$', r'_banner$', r'_bed$', r'_head$', r'_skull$', r'shulker_box$',
    r'copper_golem_statue$', r'_shelf$', r'copper_chest$', r'_stem$', r'^attached_', r'command_block$']
def eligible(b):
    if b in SKIP_EXACT: return False
    return not any(re.search(p, b) for p in SKIP_PATTERNS)
all_blocks = sorted(f[:-5] for f in os.listdir(f'{VA}/blockstates'))
blocks = [b for b in all_blocks if eligible(b) and f'block.minecraft.{b}' in vlang]  # item frames have blockstates but are entities
with open(f'{R}/stormreach_storm_blocks.txt', 'w') as f:
    f.write('# Vanilla blocks that get a generated storm counterpart (stormreach:storm_<name>). Written by tools/gendata6.py\n')
    f.write('\n'.join(blocks) + '\n')
NOT_BLOCK_ITEM = {'wheat'}  # the wheat crop's item is wheat_seeds; 'wheat' is the harvest
# a block has a same-named block item when its item def exists and the name is a block name, not an item name (wheat is an item)
with_items = {b for b in blocks if os.path.exists(f'{VA}/items/{b}.json') and b not in NOT_BLOCK_ITEM}
storm_item_ids = {f'minecraft:{b}' for b in with_items}

# ---------------- tinted, flattened copies of vanilla block models ----------------
made = {}
def vmodel(mid):
    p = mid.split(':')[-1]
    path = f'{VA}/models/{p}.json'
    return rj(path) if os.path.exists(path) else None
def flatten(mid):
    """Returns our model id for a tinted copy of vanilla model mid (minecraft:block/x)."""
    if mid in made: return made[mid]
    chain = []; cur = mid
    while cur:
        m = vmodel(cur)
        if m is None: break
        chain.append(m); cur = m.get('parent')
        if cur and cur.startswith('builtin/'): break
    textures = {}; elements = None; display = None; ao = None; root_parent = None
    for m in reversed(chain):
        textures.update(m.get('textures', {}))
    for m in chain:
        if elements is None and 'elements' in m: elements = m['elements']
        if display is None and 'display' in m: display = m['display']
        if ao is None and 'ambientocclusion' in m: ao = m['ambientocclusion']
    if elements is None:
        made[mid] = mid  # nothing to tint (e.g. builtin); use vanilla as is
        return mid
    els = copy.deepcopy(elements)
    for e in els:
        for face in e.get('faces', {}).values():
            face.setdefault('tintindex', 0)
    out = {'parent': 'minecraft:block/block', 'textures': textures, 'elements': els}
    if display: out['display'] = display
    if ao is not None: out['ambientocclusion'] = ao
    name = 'storm_' + mid.split('/')[-1]
    w(f'{A}/models/block/{name}.json', out)
    made[mid] = f'stormreach:block/{name}'
    return made[mid]

def remap_models(obj):
    s = json.dumps(obj)
    for m in set(re.findall(r'"(minecraft:block/[a-z0-9_/]+)"', s)):
        s = s.replace(f'"{m}"', f'"{flatten(m)}"')
    return json.loads(s)

def tint_item_def(node):
    if isinstance(node, dict):
        if node.get('type') == 'minecraft:model':
            mid = node['model']
            if mid.startswith('minecraft:block/'): node['model'] = flatten(mid)
            node['tints'] = [{'type': 'minecraft:constant', 'value': TINT}, {'type': 'minecraft:constant', 'value': TINT}]
            return node
        for k, v in node.items(): tint_item_def(v)
    elif isinstance(node, list):
        for v in node: tint_item_def(v)
    return node

for b in blocks:
    bs = rj(f'{VA}/blockstates/{b}.json')
    w(f'{A}/blockstates/storm_{b}.json', remap_models(bs))
    if b in with_items:
        w(f'{A}/items/storm_{b}.json', tint_item_def(rj(f'{VA}/items/{b}.json')))

# ---------------- loot tables ----------------
def storm_ids(s):
    for vid in sorted(storm_item_ids | {f'minecraft:{b}' for b in blocks}, key=len, reverse=True):
        s = s.replace(f'"{vid}"', '"stormreach:storm_' + vid.split(':')[1] + '"')
    return s
nloot = 0
for b in blocks:
    p = f'{VD}/loot_table/blocks/{b}.json'
    if not os.path.exists(p): continue
    s = open(p).read()
    # Drops that are items we made storm versions of become storm items; other drops (diamonds, sticks...) stay vanilla.
    s = re.sub(r'"(minecraft:[a-z0-9_]+)"', lambda m: ('"stormreach:storm_' + m.group(1).split(':')[1] + '"') if m.group(1) in storm_item_ids else m.group(0), s)
    s = s.replace(f'"minecraft:blocks/{b}"', f'"stormreach:blocks/storm_{b}"')
    # match_block conditions name the block itself
    s = s.replace(f'"blocks": "minecraft:{b}"', f'"blocks": "stormreach:storm_{b}"')
    s = s.replace(f'"block": "minecraft:{b}"', f'"block": "stormreach:storm_{b}"')
    os.makedirs(f'{D}/loot_table/blocks', exist_ok=True)
    open(f'{D}/loot_table/blocks/storm_{b}.json', 'w').write(s); nloot += 1

# ---------------- tags: storm blocks join their vanilla block's tags ----------------
def tag_merge(path, values):
    old = rj(path)['values'] if os.path.exists(path) else []
    ids = {v if isinstance(v, str) else v['id'] for v in old}
    w(path, {'values': old + [v for v in values if (v if isinstance(v, str) else v['id']) not in ids]})
eligible_set = set(blocks)
for kind, pool in (('block', eligible_set), ('item', with_items)):
    for f in glob.glob(f'{VD}/tags/{kind}/**/*.json', recursive=True):
        rel = os.path.relpath(f, f'{VD}/tags/{kind}')
        vals = rj(f)['values']
        add = [{'id': f'stormreach:storm_{v.split(":")[1]}', 'required': False}
               for v in vals if isinstance(v, str) and v.startswith('minecraft:') and v.split(':')[1] in pool]
        if add: tag_merge(f'{MCT}/{kind}/{rel}', add)

# ---------------- mobs ----------------
eggs = sorted(f[:-len('_spawn_egg.json')] for f in os.listdir(f'{VA}/items') if f.endswith('_spawn_egg.json'))
for mob in eggs:
    w(f'{A}/items/storm_{mob}_spawn_egg.json', {'model': {'type': 'minecraft:model', 'model': f'minecraft:item/{mob}_spawn_egg',
        'tints': [{'type': 'minecraft:constant', 'value': TINT}, {'type': 'minecraft:constant', 'value': TINT}]}})
storm_shard_pool = {'rolls': 1, 'condition': {'type': 'minecraft:killed_by_player'},
    'entries': [{'type': 'minecraft:item', 'name': 'stormreach:storm_shard', 'modifier': [
        {'type': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'max': 2, 'min': 0}},
        {'type': 'minecraft:enchanted_count_increase', 'count': {'type': 'minecraft:uniform', 'max': 1.0, 'min': 0.0}, 'enchantment': 'minecraft:looting'}]}]}
for f in glob.glob(f'{VD}/loot_table/entities/*.json'):
    mob = os.path.basename(f)[:-5]
    t = rj(f)
    t['pools'] = t.get('pools', []) + [storm_shard_pool]
    t['random_sequence'] = f'stormreach:entities/storm_{mob}'
    w(f'{D}/loot_table/entities/storm_{mob}.json', t)

# ---------------- stormwater / stormlava ----------------
w(f'{A}/models/block/stormwater.json', {'textures': {'particle': 'minecraft:block/water_still'}})
w(f'{A}/models/block/stormlava.json', {'textures': {'particle': 'stormreach:block/stormlava_still'}})
for fl in ('stormwater', 'stormlava'):
    w(f'{A}/blockstates/{fl}.json', {'variants': {'': {'model': f'stormreach:block/{fl}'}}})
    w(f'{A}/models/item/{fl}_bucket.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'stormreach:item/{fl}_bucket'}})
    w(f'{A}/items/{fl}_bucket.json', {'model': {'type': 'minecraft:model', 'model': f'stormreach:item/{fl}_bucket'}})
tag_merge(f'{R}/data/minecraft/tags/fluid/water.json', ['stormreach:stormwater', 'stormreach:flowing_stormwater'])
tag_merge(f'{R}/data/minecraft/tags/fluid/lava.json', ['stormreach:stormlava', 'stormreach:flowing_stormlava'])
# animated plasma texture for stormlava (our own art), plus bucket icons
import random, math
def plasma(size, frames, seed, name):
    rnd = random.Random(seed); img = Image.new('RGBA', (size, size * frames))
    pts = [(rnd.random() * size, rnd.random() * size, rnd.random() * 6.28) for _ in range(6)]
    for fr in range(frames):
        t = fr / frames * 6.283
        for y in range(size):
            for x in range(size):
                v = sum(math.sin(math.hypot(x - px, y - py) * 0.55 + t + ph) for px, py, ph in pts) / len(pts)
                v = (v + 1) / 2
                r = int(40 + 120 * v ** 3); g = int(110 + 120 * v ** 1.5); b = int(200 + 55 * v)
                img.putpixel((x, y + fr * size), (min(r, 255), min(g, 255), min(b, 255), 255))
    img.save(f'{A}/textures/block/{name}.png')
    w(f'{A}/textures/block/{name}.png.mcmeta', {'animation': {'frametime': 3}})
os.makedirs(f'{A}/textures/block', exist_ok=True)
plasma(16, 20, 400, 'stormlava_still'); plasma(32, 16, 401, 'stormlava_flow')
def bucket(fill, hi, name):
    rows = ['................', '................', '....MMMMMMMM....', '...M........M...', '..MFFFFFFFFFFM..', '..MFHFFFFFHFFM..',
            '..MFFFFFFFFFFM..', '...MKKKKKKKKM...', '...MKKKKKKKKM...', '....MKKKKKKM....', '....MKKKKKKM....', '.....MKKKKM.....',
            '.....MMMMMM.....', '................', '................', '................']
    pal = {'M': (112, 116, 120, 255), 'K': (156, 160, 165, 255), 'F': fill, 'H': hi}
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != '.': img.putpixel((x, y), pal[ch])
    img.save(f'{A}/textures/item/{name}.png')
bucket((58, 111, 184, 255), (159, 232, 255, 255), 'stormwater_bucket')
bucket((120, 200, 255, 255), (255, 255, 255, 255), 'stormlava_bucket')

# ---------------- Nether / End storm biomes (painted by the Stormblight, never generated naturally) ----------------
def storm_biome(name, base, sky, fog, extra_monsters):
    b = rj(f'{VD}/worldgen/biome/{base}.json')
    at = b.setdefault('attributes', {})
    at['minecraft:visual/sky_color'] = sky; at['minecraft:visual/fog_color'] = fog
    at['minecraft:visual/ambient_particles'] = {'argument': [{'particle': {'type': 'minecraft:electric_spark'}, 'probability': 0.006}], 'modifier': 'append'}
    sp = at.get('minecraft:gameplay/natural_mob_spawns', {}).get('argument', {}).get('spawns_by_category')
    if sp is not None: sp['monster'] = sp.get('monster', []) + extra_monsters
    w(f'{D}/worldgen/biome/{name}.json', b)
def cnt(t, lo, hi, wt): return {'type': t, 'count': {'type': 'minecraft:uniform', 'max_inclusive': hi, 'min_inclusive': lo}, 'weight': wt}
storm_biome('storm_wastes', 'nether_wastes', '#3b4a6b', '#2a3550', [cnt('stormreach:thunder_crab', 1, 2, 20), cnt('stormreach:ball_lightning', 1, 1, 10)])
storm_biome('storm_void', 'end_highlands', '#1c2340', '#141a33', [cnt('stormreach:squall', 1, 2, 10)])
tag_merge(f'{R}/data/minecraft/tags/worldgen/biome/is_nether.json', ['stormreach:storm_wastes'])
tag_merge(f'{R}/data/minecraft/tags/worldgen/biome/is_end.json', ['stormreach:storm_void'])

# ---------------- language ----------------
lang = rj(f'{A}/lang/en_us.json')
for b in blocks:
    name = vlang.get(f'block.minecraft.{b}')
    if name: lang[f'block.stormreach.storm_{b}'] = f'Storm {name}'
for k, v in vlang.items():
    m = re.match(r'^entity\.minecraft\.([a-z0-9_]+)$', k)
    if m: lang[f'entity.stormreach.storm_{m.group(1)}'] = f'Storm {v}'
for mob in eggs:
    name = vlang.get(f'item.minecraft.{mob}_spawn_egg')
    if name: lang[f'item.stormreach.storm_{mob}_spawn_egg'] = f'Storm {name}'
lang.update({
 'itemGroup.stormreach.storm_blocks': 'Stormreach: Storm Blocks', 'itemGroup.stormreach.storm_mobs': 'Stormreach: Storm Mobs',
 'block.stormreach.stormwater': 'Stormwater', 'block.stormreach.stormlava': 'Stormlava',
 'item.stormreach.stormwater_bucket': 'Stormwater Bucket', 'item.stormreach.stormlava_bucket': 'Stormlava Bucket',
 'biome.stormreach.storm_wastes': 'Storm Wastes', 'biome.stormreach.storm_void': 'Storm Void',
 'config.stormreach.useGeneratedBlocks': 'Storm Every Block', 'config.stormreach.useGeneratedBlocks.tooltip': 'The Stormblight turns any block without a themed counterpart into its Storm version.',
 'config.stormreach.useGeneratedMobs': 'Storm Every Mob', 'config.stormreach.useGeneratedMobs.tooltip': 'The Stormblight turns any mob without a themed counterpart into its Storm version (bosses included, except the End dragon).',
 'config.stormreach.stormMobPerks': 'Storm Mob Powers', 'config.stormreach.stormMobPerks.tooltip': 'Storm mobs have 25% more health, are immune to lightning, shock what they hit, and storm bosses call lightning.',
 'config.stormreach.stormwaterShocks': 'Stormwater Shocks', 'config.stormreach.stormwaterShocks.tooltip': 'Swimming in stormwater shocks you unless you are Grounded or in full Stormshard armor.',
 'config.stormreach.stormInOtherDimensions': 'Nether & End Storms', 'config.stormreach.stormInOtherDimensions.tooltip': 'Storm Wastes and Storm Void get permanent dry storms: sparks, lightning, Ball Lightning and Squalls.',
})
w(f'{A}/lang/en_us.json', lang)

# advancements
def adv(name, parent, icon, crit, ttl, desc, frame='task'):
    w(f'{D}/advancement/{name}.json', {'parent': f'stormreach:{parent}', 'criteria': crit,
       'display': {'title': {'translate': f'advancements.stormreach.{name}.title'}, 'description': {'translate': f'advancements.stormreach.{name}.description'},
                   'icon': {'id': icon}, 'frame': frame}, 'requirements': [list(crit)]})
    lang[f'advancements.stormreach.{name}.title'] = ttl; lang[f'advancements.stormreach.{name}.description'] = desc
adv('stormwater', 'stormheart', 'stormreach:stormwater_bucket', {'b': {'conditions': {'items': [{'items': 'stormreach:stormwater_bucket'}]}, 'trigger': 'minecraft:inventory_changed'}},
    'Charged Waters', 'Fill a bucket with stormwater')
adv('stormlava', 'stormwater', 'stormreach:stormlava_bucket', {'b': {'conditions': {'items': [{'items': 'stormreach:stormlava_bucket'}]}, 'trigger': 'minecraft:inventory_changed'}},
    'Liquid Lightning', 'Fill a bucket with stormlava')
adv('storm_wither', 'stormwarden', 'minecraft:wither_skeleton_skull', {'k': {'conditions': {'entity': {'type': 'minecraft:entity_properties', 'entity': 'this',
    'predicate': {'minecraft:entity_type': 'stormreach:storm_wither'}}}, 'trigger': 'minecraft:player_killed_entity'}},
    'The Withering Storm', 'Defeat a Storm Wither', frame='challenge')
w(f'{A}/lang/en_us.json', lang)
print(f'data6 done: {len(blocks)} storm blocks ({len(with_items)} with items), {len(made)} models, {nloot} loot tables, {len(eggs)} storm spawn eggs')
