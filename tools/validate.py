import os as _os, pathlib as _pl
ROOT = str(_pl.Path(__file__).resolve().parent.parent)  # the Stormreach project folder
VANILLA = _os.environ.get('STORMREACH_VANILLA', str(_pl.Path(ROOT) / '.vanilla'))  # extracted 26.3 jar (assets/ + data/)
MCJAR = _os.environ.get('STORMREACH_MCJAR', str(_pl.Path(ROOT) / '.vanilla' / 'minecraft-26.3.jar'))
import json, os, re, glob, zipfile
R=ROOT+'/src/main/resources'; A=R+'/assets/stormreach'; D=R+'/data'
JAVA=ROOT+'/src/main/java'
mc=zipfile.ZipFile(MCJAR); names=set(mc.namelist())
problems=[]
def exists_res(kind, rid, ext='.json'):
    ns_, path = rid.split(':') if ':' in rid else ('minecraft', rid)
    if ns_=='stormreach': return os.path.exists(f'{A}/{kind}/{path}{ext}')
    return f'assets/{ns_}/{kind}/{path}{ext}' in names
# all json parse
for f in glob.glob(R+'/**/*.json', recursive=True):
    try: json.load(open(f))
    except Exception as e: problems.append(f'bad json {f}: {e}')
# registered ids from Java
src=open(JAVA+'/com/stormreach/registry/ModBlocks.java').read()
blocks=re.findall(r'(?:register|stairs)\("([a-z_]+)"', src)
rsrc=open(JAVA+'/com/stormreach/realm/RealmBlocks.java').read()
blocks+=re.findall(r'(?:register|stairs)\("([a-z_]+)"', rsrc)
noitem={'stormberry_shrub','stormgate'}
isrc=open(JAVA+'/com/stormreach/registry/ModItems.java').read()
items=re.findall(r'register\("([a-z_]+)"', isrc) + re.findall(r'(?:register|egg)\("([a-z_]+)"', open(JAVA+'/com/stormreach/realm/RealmItems.java').read()) + [b for b in blocks if b not in noitem]
esrc=open(JAVA+'/com/stormreach/registry/ModEntities.java').read()
ents=[e for e in re.findall(r'register\("([a-z_]+)"', esrc) + re.findall(r'register\("([a-z_]+)"', open(JAVA+'/com/stormreach/realm/RealmEntities.java').read()) if e not in ('squall_bottle','storm_front')]
lang=json.load(open(A+'/lang/en_us.json'))
def model_chain(mid, depth=0):
    if depth>10: return
    if not exists_res('models', mid): problems.append(f'missing model {mid}'); return
    ns_,p=(mid.split(':')+[''])[:2] if ':' in mid else ('minecraft',mid)
    if ns_!='stormreach': return
    m=json.load(open(f'{A}/models/{p}.json'))
    for k,t in m.get('textures',{}).items():
        tid=t['sprite'] if isinstance(t,dict) else t
        if tid.startswith('#'): continue
        if not exists_res('textures', tid, '.png'): problems.append(f'missing texture {tid} in {mid}')
    if 'parent' in m: model_chain(m['parent'], depth+1)
for b in blocks:
    bs=f'{A}/blockstates/{b}.json'
    if not os.path.exists(bs): problems.append(f'no blockstate {b}'); continue
    bsj=json.load(open(bs))
    vals=list(bsj.get('variants',{}).values())+[m['apply'] for m in bsj.get('multipart',[])]
    for v in vals:
        for vv in (v if isinstance(v,list) else [v]): model_chain(vv['model'])
    if f'block.stormreach.{b}' not in lang: problems.append(f'no lang block {b}')
    if b not in ('sovereign_altar','stormgate') and not os.path.exists(f'{D}/stormreach/loot_table/blocks/{b}.json'): problems.append(f'no loot {b}')
for i in items:
    d=f'{A}/items/{i}.json'
    if not os.path.exists(d): problems.append(f'no item def {i}'); continue
    model_chain(json.load(open(d))['model']['model'])
    key=f'item.stormreach.{i}' if i not in blocks else f'block.stormreach.{i}'
    if key not in lang: problems.append(f'no lang {key}')
for e in ents:
    if f'entity.stormreach.{e}' not in lang: problems.append(f'no lang entity {e}')
    if not os.path.exists(f'{A}/textures/entity/{e}.png'): problems.append(f'no entity texture {e}')
    if not os.path.exists(f'{D}/stormreach/loot_table/entities/{e}.json'): problems.append(f'no entity loot {e}')
# id existence in recipes/loot/tags
ours={f'stormreach:{x}' for x in items+blocks+ents}
vanilla_items={k.split('.',2)[2] for k in json.loads(mc.read('assets/minecraft/lang/en_us.json')) if re.match(r'^(item|block|entity)\.minecraft\.[a-z_0-9]+$',k)}
# generated 3.0 content: storm counterparts, fluids
gen_blocks=[l.strip() for l in open(R+'/stormreach_storm_blocks.txt') if l.strip() and not l.startswith('#')]
gen_items=[b for b in gen_blocks if f'assets/minecraft/items/{b}.json' in names and b!='wheat']
vanilla_ents=[k.split('.')[2] for k in json.loads(mc.read('assets/minecraft/lang/en_us.json')) if re.match(r'^entity\.minecraft\.[a-z_0-9]+$',k)]
eggs=[n.split('/')[-1][:-len('_spawn_egg.json')] for n in names if re.match(r'assets/minecraft/items/[a-z_0-9]+_spawn_egg\.json$',n)]
fluids=['stormwater','flowing_stormwater','stormlava','flowing_stormlava','stormwater_bucket','stormlava_bucket']
ours|={f'stormreach:storm_{x}' for x in gen_blocks+vanilla_ents}|{f'stormreach:storm_{x}_spawn_egg' for x in eggs}|{f'stormreach:{x}' for x in fluids}
ours|={'stormreach:storm_wastes','stormreach:storm_void'}
def models_in(o):
    if isinstance(o,dict):
        for k,v in o.items():
            if k=='model' and isinstance(v,str): yield v
            else: yield from models_in(v)
    elif isinstance(o,list):
        for v in o: yield from models_in(v)
checked=set()
def chain_once(mid):
    if mid not in checked: checked.add(mid); model_chain(mid)
for b in gen_blocks:
    bs=f'{A}/blockstates/storm_{b}.json'
    if not os.path.exists(bs): problems.append(f'no generated blockstate {b}'); continue
    for m in models_in(json.load(open(bs))): chain_once(m)
    if f'block.stormreach.storm_{b}' not in lang: problems.append(f'no lang storm_{b}')
for b in gen_items:
    d=f'{A}/items/storm_{b}.json'
    if not os.path.exists(d): problems.append(f'no generated item def {b}'); continue
    for m in models_in(json.load(open(d))): chain_once(m)
for e in eggs:
    if f'item.stormreach.storm_{e}_spawn_egg' not in lang: problems.append(f'no lang egg {e}')
for fl in ('stormwater','stormlava'):
    for m in models_in(json.load(open(f'{A}/blockstates/{fl}.json'))): chain_once(m)
    for m in models_in(json.load(open(f'{A}/items/{fl}_bucket.json'))): chain_once(m)
    if f'block.stormreach.{fl}' not in lang: problems.append(f'no lang {fl}')
for t in ('stormlava_still','stormlava_flow'):
    if not os.path.exists(f'{A}/textures/block/{t}.png'): problems.append(f'no texture {t}')
def chk(i, where):
    if isinstance(i, dict): i=i['id']
    if i.startswith('#'): return
    if i.startswith('stormreach:'):
        if i not in ours and not i.startswith('stormreach:stormreach_coast'): problems.append(f'unknown id {i} in {where}')
    elif i.startswith('minecraft:') and i.split(':')[1] not in vanilla_items:
        problems.append(f'unknown vanilla id {i} in {where}')

# ---------------- 5.0 registries (tornado, meteorology, backpacks, labyrinth, story, forest) ----------------
def jsrc(rel): return open(JAVA + '/com/stormreach/' + rel).read()
nb, ni, ne = [], [], []
for rel in ('tornado/WeatherItems.java', 'labyrinth/LabyrinthRegistry.java', 'story/StoryRegistry.java', 'forest/ForestRegistry.java', 'backpack/Backpacks.java'):
    src5 = jsrc(rel)
    nb += re.findall(r'(?:block|trophy)\("([a-z_0-9]+)"', src5); ni += re.findall(r'\bitem\("([a-z_0-9]+)"', src5); ne += re.findall(r'\bentity\("([a-z_0-9]+)"', src5)
ni = [i for i in ni if not i.endswith('_')]
ni += ['tornado_spawner_' + k.strip().lower() for k in re.findall(r'enum SpawnerKind \{([^}]*)\}', jsrc('tornado/WeatherItems.java'))[0].replace(';', '').split(',') if k.strip()]
ni = [i.strip() for i in ni]
ni += ['weather_event_' + k.strip().lower() for k in re.findall(r'enum EventKind \{([^}]*)\}', jsrc('tornado/WeatherItems.java'))[0].replace(';', '').split(',') if k.strip()]
tiers = re.findall(r'[A-Z_]+\("([a-z_]+)", \d+\)', jsrc('sim/BackpackTier.java'))
ni += [f'{t}_backpack' for t in tiers] + [f'{t}_backpack_upgrade' for t in tiers[1:]]
ne += ['tornado']
noitem5 = {'stormfruit_pod'}
for b in nb:
    bs = f'{A}/blockstates/{b}.json'
    if not os.path.exists(bs): problems.append(f'no blockstate {b}'); continue
    for m in models_in(json.load(open(bs))): chain_once(m)
    if f'block.stormreach.{b}' not in lang: problems.append(f'no lang block {b}')
    blk_src = ''.join(jsrc(r) for r in ('labyrinth/LabyrinthRegistry.java', 'story/StoryRegistry.java'))
    noloot = re.search(r'"' + b + r'"[^;]*noLootTable', blk_src)
    if not noloot and not os.path.exists(f'{D}/stormreach/loot_table/blocks/{b}.json'): problems.append(f'no loot {b}')
for i in ni + [b for b in nb if b not in noitem5]:
    d = f'{A}/items/{i}.json'
    if not os.path.exists(d): problems.append(f'no item def {i}'); continue
    for m in models_in(json.load(open(d))): chain_once(m)
    key = f'block.stormreach.{i}' if i in nb else f'item.stormreach.{i}'
    if key not in lang: problems.append(f'no lang {key}')
for e in ne:
    if f'entity.stormreach.{e}' not in lang: problems.append(f'no lang entity {e}')
    if e != 'tornado' and not os.path.exists(f'{D}/stormreach/loot_table/entities/{e}.json'): problems.append(f'no entity loot {e}')
for tex in ('tornado', 'stranger', 'cyrrus', 'cyrrus_final', 'hollow_keeper', 'glimmerstag', 'thistlehop'):
    if not os.path.exists(f'{A}/textures/entity/{tex}.png'): problems.append(f'no entity texture {tex}')
for eq in ('stormcrown', 'storm_chaser_cap'):
    if not os.path.exists(f'{A}/equipment/{eq}.json') or not os.path.exists(f'{A}/textures/entity/equipment/humanoid/{eq}.png'): problems.append(f'equipment {eq} incomplete')
ours |= {f'stormreach:{x}' for x in nb + ni + ne}
# sounds: every registered sound event has a sounds.json entry, every entry's file exists, subtitles have lang
sj = json.load(open(f'{A}/sounds.json'))
for path in re.findall(r'register\("([a-z_.0-9]+)"\)', open(JAVA + '/com/stormreach/registry/ModSounds.java').read()):
    if path not in sj: problems.append(f'sound {path} missing from sounds.json')
for k, v in sj.items():
    for snd in v['sounds']:
        nm = snd['name'] if isinstance(snd, dict) else snd
        if nm.startswith('stormreach:') and not os.path.exists(f"{A}/sounds/{nm.split(':')[1]}.ogg"): problems.append(f'missing ogg for {k}: {nm}')
    if 'subtitle' in v and v['subtitle'] not in lang: problems.append(f'missing subtitle lang {v["subtitle"]}')
# cinematic scripts: 5 fields, ascending ticks, sounds registered, subtitles translated
for sc in re.findall(r'"([a-z_]+)"', re.search(r'SCRIPTS = \{([^}]*)\}', open(JAVA + '/com/stormreach/registry/ModSounds.java').read()).group(1)):
    fp = f'{D}/stormreach/cinematic/{sc}.txt'
    if not os.path.exists(fp): problems.append(f'missing cinematic {sc}'); continue
    last = -1
    for ln in open(fp):
        ln = ln.strip()
        if not ln or ln.startswith('#'): continue
        parts = [x.strip() for x in ln.split('|')]
        if len(parts) != 5: problems.append(f'{sc}: bad line {ln}'); continue
        if int(parts[0]) < last: problems.append(f'{sc}: ticks go backwards at {parts[1]}')
        last = int(parts[0])
        if parts[3] not in ('', '-') and parts[3].split(':')[1] not in sj: problems.append(f'{sc}: sound {parts[3]} not in sounds.json')
        if parts[2] not in ('', '-') and parts[2] not in lang: problems.append(f'{sc}: subtitle {parts[2]} untranslated')
# advancements: parents exist, reward loot tables exist, icons are real items, code-awarded ones are well-formed
code_awarded = set(re.findall(r'award\([a-z]+, "([a-z_0-9]+)"\)', ''.join(open(f).read() for f in glob.glob(JAVA + '/**/*.java', recursive=True)))) - {'trigger'}
for f in glob.glob(D + '/stormreach/advancement/*.json'):
    a = json.load(open(f)); n = os.path.basename(f)[:-5]
    if 'parent' in a and not os.path.exists(f"{D}/stormreach/advancement/{a['parent'].split(':')[1]}.json"): problems.append(f'advancement {n}: missing parent {a["parent"]}')
    for lt in a.get('rewards', {}).get('loot', []):
        if not os.path.exists(f"{D}/stormreach/loot_table/{lt.split(':')[1]}.json"): problems.append(f'advancement {n}: missing reward table {lt}')
    if 'display' in a: chk(a['display']['icon']['id'], f'advancement {n}')
    for c in a['criteria'].values():
        for it_ in (c.get('conditions', {}).get('items') or []):
            ids = it_.get('items'); ids = ids if isinstance(ids, list) else [ids]
            for i in ids: chk(i, f'advancement {n}')
for n in code_awarded:
    fp = f'{D}/stormreach/advancement/{n}.json'
    if not os.path.exists(fp): problems.append(f'code awards {n} but no advancement exists'); continue
    a = json.load(open(fp))
    if list(a['criteria']) != ['trigger'] or a['criteria']['trigger']['trigger'] != 'minecraft:impossible': problems.append(f'code-awarded {n} must have one impossible "trigger" criterion')
rewards = [lt for f in glob.glob(D + '/stormreach/advancement/*.json') for lt in json.load(open(f)).get('rewards', {}).get('loot', [])]
if len(rewards) != len(set(rewards)): problems.append('an achievement reward table is used twice')
reward_items = [re.findall(r'"name": "([a-z_:]+)"', open(f).read()) for f in glob.glob(D + '/stormreach/loot_table/ach/*.json')]
flat = [i for l in reward_items for i in l]
if len(flat) != len(set(flat)): problems.append('the same reward item is granted by two achievements')
# structures referenced by template pools exist
for f in glob.glob(D + '/stormreach/worldgen/template_pool/*.json'):
    for loc_ in re.findall(r'"location": "stormreach:([a-z_]+)"', open(f).read()):
        if not os.path.exists(f'{D}/stormreach/structure/{loc_}.nbt'): problems.append(f'missing structure nbt {loc_}')
# placed features reference features; biomes reference placed features
for f in glob.glob(D + '/stormreach/worldgen/placed_feature/*.json'):
    ft = json.load(open(f))['feature']
    if isinstance(ft, str) and ft.startswith('stormreach:') and not os.path.exists(f"{D}/stormreach/worldgen/feature/{ft.split(':')[1]}.json"): problems.append(f'placed {f} -> missing feature {ft}')
for f in glob.glob(D + '/stormreach/worldgen/biome/*.json'):
    for step in json.load(open(f))['features']:
        for pf in step:
            if pf.startswith('stormreach:') and not os.path.exists(f"{D}/stormreach/worldgen/placed_feature/{pf.split(':')[1]}.json"): problems.append(f'biome {f} -> missing placed {pf}')
for f in glob.glob(D + '/stormreach/worldgen/feature/*.json'):
    for i in re.findall(r'"(?:id|block_provider)": "(stormreach:[a-z_]+)"', open(f).read()):
        if i not in ours and not os.path.exists(f"{D}/stormreach/worldgen/block_state_provider/{i.split(':')[1]}.json"): problems.append(f'feature {os.path.basename(f)}: unknown {i}')

# ---------------- 26.3 data-format checks against the vanilla data (these only fail at world load) ----------------
def _types(o, acc):
    if isinstance(o, dict):
        for k, v in o.items():
            if k == 'type' and isinstance(v, str): acc.add(v)
            _types(v, acc)
    elif isinstance(o, list):
        for x in o: _types(x, acc)
VDM = VANILLA + '/data/minecraft'
vtypes = {'minecraft:advancement_reward'}
for f in glob.glob(VDM + '/loot_table/**/*.json', recursive=True) + glob.glob(VDM + '/predicate/**/*.json', recursive=True) + glob.glob(VDM + '/item_modifier/**/*.json', recursive=True):
    _types(json.load(open(f)), vtypes)
for f in glob.glob(D + '/*/loot_table/**/*.json', recursive=True):
    t_ = json.load(open(f)); found = set(); _types(t_, found)
    for t in sorted(found - vtypes): problems.append(f'loot {os.path.relpath(f, D)}: type {t} is not used anywhere in 26.3 vanilla loot')
    for pr in re.findall(r'"condition": "([^"]+)"', open(f).read()):
        pns, ppath = pr.split(':')
        if pns == 'minecraft' and not os.path.exists(f'{VDM}/predicate/{ppath}.json'): problems.append(f'loot {os.path.relpath(f, D)}: unknown predicate {pr}')
CRAFT_CATS = {'building', 'redstone', 'equipment', 'misc'}; COOK_CATS = {'food', 'blocks', 'misc'}
for f in glob.glob(D + '/*/recipe/*.json'):
    r_ = json.load(open(f)); cat = r_.get('category')
    if cat is None: continue
    ok = cat in (CRAFT_CATS if r_['type'].startswith('minecraft:crafting') else COOK_CATS if r_['type'] in ('minecraft:smelting', 'minecraft:blasting', 'minecraft:smoking', 'minecraft:campfire_cooking') else CRAFT_CATS | COOK_CATS)
    if not ok: problems.append(f'recipe {os.path.basename(f)}: category {cat} is not valid for {r_["type"]}')
for f in glob.glob(D + '/*/worldgen/biome/*.json'):
    for k, v in json.load(open(f)).get('attributes', {}).items():
        if isinstance(v, dict) and 'argument' in v and 'modifier' not in v: problems.append(f'biome {os.path.basename(f)}: attribute {k} has "argument" but no "modifier"')
for f in glob.glob(D+'/stormreach/recipe/*.json'):
    r=json.load(open(f)); ids=[r['result']['id']]
    ids+= list(r.get('key',{}).values()) + r.get('ingredients',[]) + ([r['ingredient']] if 'ingredient' in r else [])
    for i in ids: chk(i, os.path.basename(f))
for f in glob.glob(D+'/stormreach/loot_table/**/*.json', recursive=True):
    for i in re.findall(r'"name": "([a-z_:]+)"', open(f).read()): chk(i, os.path.basename(f))
for f in glob.glob(D+'/*/tags/**/*.json', recursive=True):
    if any(k in f for k in ('/worldgen/', '/damage_type/', '/enchantment/', '/painting_variant/')): continue
    for i in json.load(open(f))['values']: chk(i, f.split('data/')[1])
# lang keys used in Java exist
for jf in glob.glob(ROOT+'/src/**/*.java', recursive=True):
    for k in re.findall(r'translatable\("([a-z_.]+)"', open(jf).read()):
        if k.endswith('.') or k.endswith('.phase'): continue
        if k not in lang and not k.startswith('gui.'): problems.append(f'lang key {k} used in {os.path.basename(jf)} missing')
# config keys used with prefix
for jf in glob.glob(ROOT+'/src/client/**/*.java', recursive=True):
    s=open(jf).read()
    for k in re.findall(r'(?:toggle|slider)\("([a-zA-Z]+)"', s):
        for suf in ('', '.tooltip'):
            if f'config.stormreach.{k}{suf}' not in lang: problems.append(f'missing config lang {k}{suf}')
print('\n'.join(problems[:60]) if problems else 'ALL CHECKS PASSED', f'[{len(problems)} problems] ({len(blocks)}+{len(nb)} blocks, {len(items)}+{len(ni)} items, {len(ents)}+{len(ne)} mobs; generated {len(gen_blocks)} blocks, {len(gen_items)} items, {len(eggs)} eggs, {len(checked)} models)')
