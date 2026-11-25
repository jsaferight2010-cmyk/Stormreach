"""Computes the rarity / drop-rate tables for GUIDE.md straight from the mod's data files and source, so the guide
never drifts from what is actually in the jar. Writes tools/guide_tables.md (pasted into GUIDE.md by make_guide)."""
import glob, json, os, pathlib, re
ROOT = pathlib.Path(__file__).resolve().parent.parent
R = ROOT / 'src/main/resources'; D = R / 'data/stormreach'; J = ROOT / 'src/main/java/com/stormreach'
lang = json.load(open(R / 'assets/stormreach/lang/en_us.json'))
vlang = {}
for vl in ('/home/claude/mc/vd/assets/minecraft/lang/en_us.json', str(ROOT / '.vanilla/assets/minecraft/lang/en_us.json')):
    if os.path.exists(vl): vlang = json.load(open(vl)); break


def name(i):
    ns, p = i.split(':') if ':' in i else ('minecraft', i)
    if ns == 'stormreach': return lang.get(f'item.stormreach.{p}') or lang.get(f'block.stormreach.{p}') or p
    return vlang.get(f'item.minecraft.{p}') or vlang.get(f'block.minecraft.{p}') or p.replace('_', ' ').title()


def band(p):
    if p >= 0.999: return 'Guaranteed'
    if p >= 0.5: return 'Common'
    if p >= 0.15: return 'Uncommon'
    if p >= 0.05: return 'Rare'
    if p >= 0.01: return 'Very rare'
    return 'Legendary'


def num_range(n):
    if isinstance(n, (int, float)): return (n, n)
    if n.get('type', 'minecraft:uniform') == 'minecraft:uniform': return (n['min'], n['max'])
    if n.get('type') == 'minecraft:constant': return (n['value'], n['value'])
    return (1, 1)


def entry_info(e):
    """(chance multiplier from conditions, avg count, count text)."""
    mult = 1.0
    conds = [e['condition']] if 'condition' in e else []
    while conds:
        c = conds.pop()
        if c['type'] == 'minecraft:all_of': conds += c['terms']
        elif c['type'] == 'minecraft:random_chance': mult *= c['chance']
        elif c['type'] == 'minecraft:random_chance_with_enchanted_bonus': mult *= c['unenchanted_chance']
    lo, hi = 1, 1
    for m in e.get('modifier', []):
        if m['type'] == 'minecraft:set_count' and 'condition' not in m: lo, hi = num_range(m['count'])
    lo_c = max(lo, 0); avg = (lo_c + hi) / 2 if lo >= 0 else sum(max(0, v) for v in range(int(lo), int(hi) + 1)) / (hi - lo + 1)
    p_nonzero = 1.0 if lo >= 1 else (sum(1 for v in range(int(lo), int(hi) + 1) if v >= 1) / (hi - lo + 1))
    txt = f'{max(lo, 0):g}' if lo == hi else f'{max(lo, 0):g}–{hi:g}'
    return mult, avg, p_nonzero, txt


def table_stats(path):
    t = json.load(open(path)); out = {}
    for pool in t['pools']:
        r_lo, r_hi = num_range(pool.get('rolls', 1)); rolls = list(range(int(r_lo), int(r_hi) + 1))
        pool_mult = entry_info(pool)[0] if 'condition' in pool else 1.0
        flat = []
        for e in pool['entries']:
            if e['type'] == 'minecraft:alternatives': flat.append(e['children'][-1])  # the non-silk-touch branch
            else: flat.append(e)
        total = sum(e.get('weight', 1) for e in flat)
        for e in flat:
            if e['type'] != 'minecraft:item': continue
            mult, avg, p_nonzero, txt = entry_info(e)
            p_pick = e.get('weight', 1) / total * mult * p_nonzero * pool_mult
            p_any = sum(1 - (1 - p_pick) ** r for r in rolls) / len(rolls)
            exp = sum(rolls) / len(rolls) * e.get('weight', 1) / total * mult * pool_mult * avg
            o = out.setdefault(e['name'], [0.0, 0.0, txt])
            o[0] = 1 - (1 - o[0]) * (1 - p_any); o[1] += exp
    return out


def md_table(head, rows):
    s = '| ' + ' | '.join(head) + ' |\n|' + '|'.join('---' for _ in head) + '|\n'
    return s + ''.join('| ' + ' | '.join(str(c) for c in r) + ' |\n' for r in rows)


out = []
# ---------------------------------------------------------------- item rarity tiers
src = ''.join(open(f).read() for f in glob.glob(str(J / '**/*.java'), recursive=True))
tiers = {'UNCOMMON': [], 'RARE': [], 'EPIC': []}
for m in re.finditer(r'(?:item|block)\("([a-z_0-9]+)"[^;]*?Rarity\.([A-Z]+)', src):
    if m.group(2) in tiers: tiers[m.group(2)].append(m.group(1))
out.append('### Item rarity (name colour)\n\nMinecraft colours item names by rarity: white = Common, yellow = Uncommon, aqua = Rare, magenta = Epic. '
           'Everything not listed is Common. (Tornado spawners and weather events are Epic and creative-only.)\n\n')
rows = []
for t, label in (('EPIC', 'Epic (magenta)'), ('RARE', 'Rare (aqua)'), ('UNCOMMON', 'Uncommon (yellow)')):
    items = sorted({name('stormreach:' + i) for i in tiers[t] if not i.startswith('tornado_spawner_') and not i.startswith('weather_event_') and not i.endswith('_')})
    rows.append((label, ', '.join(items)))
out.append(md_table(['Rarity', 'Items'], rows))

# ---------------------------------------------------------------- mob drops
MOBS = ['cyrrus', 'tempest_sovereign', 'stormwarden', 'hollow_keeper', 'glimmerstag', 'thistlehop', 'skyray', 'puffling', 'static_wisp', 'cyclone',
        'stormhawk', 'thunder_crab', 'cliff_crab', 'harbor_seal', 'brine_wraith', 'squall', 'ball_lightning', 'storm_gull', 'waterspout',
        'thunderbird', 'cloud_whale', 'shock_eel', 'sparkskink', 'storm_jelly', 'gale_sprite', 'tidewrack_hermit', 'squall_serpent', 'hailstone_golem', 'aurora_moth', 'storm_petrel']
out.append('\n### Mob drops\n\nChance is per kill without Looting (Looting adds up to +1 per level to most counts). '
           '"Drop-rate rarity" uses the same bands everywhere in this guide: Guaranteed 100%, Common ≥50%, Uncommon 15–50%, Rare 5–15%, '
           'Very rare 1–5%, Legendary <1%.\n\n')
rows = []
for m in MOBS:
    p = D / f'loot_table/entities/{m}.json'
    if not p.exists(): continue
    st = table_stats(p)
    mob = lang.get(f'entity.stormreach.{m}', m)
    for item, (pa, exp, txt) in sorted(st.items(), key=lambda kv: -kv[1][0]):
        rows.append((mob, name(item), txt, f'{pa * 100:.1f}%', band(pa)))
out.append(md_table(['Mob', 'Drop', 'Count', 'Chance per kill', 'Drop-rate rarity'], rows))
out.append('\nThe Stranger and tornadoes drop nothing. Stranger: after you have seen it 3+ times, each time it vanishes there is a 25% chance it '
           'leaves a Hollow Remnant where it stood (Uncommon, but only after the third sighting).\nEvery Storm-variant vanilla mob drops what its vanilla counterpart drops.\n')

# ---------------------------------------------------------------- chests
CHESTS = [('labyrinth_treasure', 'Hollow Labyrinth: treasure chamber'), ('labyrinth_vault', 'Hollow Labyrinth: corner key vault (×3)'),
          ('labyrinth_room', 'Hollow Labyrinth: side rooms'), ('hollow_shrine', 'Hollow Shrine'), ('sky_vault', 'Sky Vault'),
          ('tempest_spire', 'Tempest Spire'), ('storm_shrine', 'Storm Shrine'), ('lighthouse', 'Lighthouse'), ('wrecked_skiff', 'Wrecked Skiff')]
out.append('\n### Chest loot (chance per chest)\n\n')
for key, title in CHESTS:
    p = D / f'loot_table/chests/{key}.json'
    if not p.exists(): continue
    st = table_stats(p); rows = []
    for item, (pa, exp, txt) in sorted(st.items(), key=lambda kv: (-kv[1][0], name(kv[0]))):
        nm = 'Music Disc (Otherside)' if item.endswith('music_disc_otherside') else name(item)
        rows.append((nm, txt, f'{pa * 100:.1f}%', f'{exp:.2f}', band(pa)))
    out.append(f'**{title}**\n\n' + md_table(['Item', 'Stack', 'Chance per chest', 'Avg. per chest', 'Drop-rate rarity'], rows) + '\n')

# ---------------------------------------------------------------- structures
out.append('### Structure rarity\n\nStructures are placed on a grid: the world is cut into regions of *spacing × spacing* chunks and each region gets '
           'at most one attempt, at least *separation* chunks from the region edge. "One per" is the region size in blocks; the structure appears '
           'there only if the biome fits. Every structure here works with `/locate structure`.\n\n')
INFO = {'hollow_labyrinth': ('Overworld, underground (any biome)', 'Very rare'), 'hollow_shrine': ('Tempest Realm, floating (any realm biome)', 'Legendary'),
        'tempest_spire': ('Overworld, Stormreach biomes', 'Rare'), 'sky_vault': ('Tempest Realm sky', 'Uncommon'), 'lighthouse': ('Stormreach Coast', 'Uncommon'),
        'storm_shrine': ('Stormreach biomes', 'Uncommon'), 'wrecked_skiff': ('Stormreach beaches and shores', 'Common')}
rows = []
for f in sorted(glob.glob(str(D / 'worldgen/structure_set/*.json'))):
    n = os.path.basename(f)[:-5]; pl = json.load(open(f))['placement']
    where, rar = INFO.get(n, ('', ''))
    rows.append((f'`stormreach:{n}`', where, pl['spacing'], pl['separation'], f"{pl['spacing'] * 16}×{pl['spacing'] * 16} blocks", rar))
rows.sort(key=lambda r: -r[2])
out.append(md_table(['Structure', 'Where', 'Spacing (chunks)', 'Separation', 'At most one per', 'Rarity'], rows))

# ---------------------------------------------------------------- ores
out.append('\n### Ores and clouds\n\n')
rows = []
realm_bios = {b.split(':')[1] for b in json.load(open(D / 'tags/worldgen/biome/is_tempest_realm.json'))['values']}
used_in = {}
for bf in glob.glob(str(D / 'worldgen/biome/*.json')):
    for step in json.load(open(bf))['features']:
        for pf_ in step: used_in.setdefault(pf_.split(':')[1], set()).add(os.path.basename(bf)[:-5])
for f in sorted(glob.glob(str(D / 'worldgen/placed_feature/*.json'))):
    pf = json.load(open(f)); n = os.path.basename(f)[:-5]
    if not (n.startswith('ore_') or n.startswith('cloud_')): continue
    feat = json.load(open(D / 'worldgen/feature' / (pf['feature'].split(':')[1] + '.json')))
    if feat.get('type') != 'minecraft:ore': continue
    block = feat['targets'][0]['state'] if isinstance(feat['targets'][0]['state'], str) else feat['targets'][0]['state'].get('id', feat['targets'][0]['state'].get('Name'))
    freq = ''
    hr = ''
    for p in pf['placement']:
        if p['type'] == 'minecraft:count': freq = f"{p['count']} veins per chunk" if isinstance(p['count'], int) else 'varies'
        if p['type'] == 'minecraft:rarity_filter': freq = f"1 vein per {p['chance']} chunks"
        if p['type'] == 'minecraft:height_range':
            h = p['height']; hr = f"y {h['min_inclusive'].get('absolute', '?')} to {h['max_inclusive'].get('absolute', '?')}"
    air = feat.get('discard_chance_on_air_exposure', 0)
    bios = used_in.get(n, set()); dim = 'Tempest Realm' if bios and bios <= realm_bios else 'Overworld (Stormreach biomes)'
    rows.append((name(block), dim, freq, f"up to {feat['size']} blocks", hr, f'{air * 100:.0f}%' if air else '—'))
out.append(md_table(['Block', 'Dimension', 'Frequency', 'Vein size', 'Height', 'Skipped when touching air'], rows))
out.append('\nAurorite is the rarest realm ore (3 small veins per chunk below y 120, half of them discarded if exposed to air; richer in Aurora Spires).\n')

# ---------------------------------------------------------------- tornadoes
prof = open(J / 'sim/TornadoProfile.java').read()
def arr(nm): return [float(x) for x in re.search(nm + r'\s*=\s*\{([^}]*)\}', prof).group(1).split(',')]
sc, ls, ws = arr('SUPERCELL_CLIMATOLOGY'), arr('LANDSPOUT_CLIMATOLOGY'), arr('WATERSPOUT_CLIMATOLOGY')
mv = arr('MULTIVORTEX_CHANCE')
mw = [[int(x) for x in row.split(',')] for row in re.findall(r'\{([0-9, ]+)\}', re.search(r'MORPH_WEIGHTS = \{(.*?)\};', prof, re.S).group(1))]
EF = [('EF0', '65–85'), ('EF1', '86–110'), ('EF2', '111–135'), ('EF3', '136–165'), ('EF4', '166–200'), ('EF5', 'over 200')]
out.append('\n### Tornado ratings: how often each EF rating happens\n\nRatings follow the U.S. climatology (most tornadoes are weak; violent ones are rare). '
           'The rating is given *after* the tornado lifts, from the strongest damage it actually caused, just like a real damage survey: a violent '
           'tornado that only crosses open fields can be rated low. The Intensity Bias setting in the config file shifts these odds.\n\n')
rows = []
for i, (ef, mph) in enumerate(EF):
    rows.append((ef, mph + ' mph', f'{sc[i] * 100:.1f}%', band(sc[i]), f'{ls[i] * 100:.0f}%' if ls[i] else '—', f'{ws[i] * 100:.0f}%' if ws[i] else '—',
                 f'{mv[i] * 100:.0f}%'))
out.append(md_table(['Rating', '3-second gust', 'Supercell tornadoes', 'Rarity', 'Landspouts', 'Waterspouts', 'Multiple-vortex chance'], rows))
out.append('\n**What they look like, by rating (supercell tornadoes)**\n\n')
MN = ['Rope', 'Needle', 'Drill-bit', 'Cone', 'Stovepipe', 'Wedge', 'Large wedge']
rows = []
for i, (ef, _) in enumerate(EF):
    tot = sum(mw[i]); rows.append([ef] + [f'{w / tot * 100:.0f}%' if w else '—' for w in mw[i]])
out.append(md_table(['Rating'] + MN, rows))
out.append('\nAppearance is not intensity: a rope can still be strong, and wedges can be weak. Satellite tornadoes (8%) only form beside EF3+ '
           'tornadoes at least 200 m wide; a separate companion tornado forms 5% of the time.\n')
out.append('\n### Natural events\n\n')
out.append(md_table(['Event', 'Chance', 'When and where', 'Rarity'], [
    ('Supercell tornado', '0.6% per player every 10 s', 'Overworld thunderstorm, any non-frozen biome (about one per 2–4 thunderstorms near you)', 'Rare'),
    ('Landspout', '0.15% per player every 10 s', 'Rain without thunder, warm biomes (temperature > 0.6), not over water', 'Very rare'),
    ('Waterspout', '0.4% every 10 s (0.1% in clear weather)', 'Over oceans and rivers', 'Very rare'),
    ('The Stranger appears', '2% every 30 s, ×1.8 in forests, ×1.5 in rain, ×1.4 at night, ×1.3 in darkness (max 25%)',
     'Overworld, anywhere; grows a little bolder with every sighting', 'Rare'),
    ('A sign of the Stranger', '3% every 30 s', 'Footsteps, whispers, smoke, a snuffed campfire, a cairn with a soul torch', 'Rare'),
]))
# ---------------------------------------------------------------- backpacks
bt = re.findall(r'([A-Z_]+)\("([a-z_]+)", (\d+)\)', open(J / 'sim/BackpackTier.java').read())
out.append('\n### Backpack tiers\n\n')
rows = []
for i, (_, tid, slots) in enumerate(bt):
    s = int(slots)
    rows.append((name('stormreach:' + tid + '_backpack'), s, s + 27, -(-(s + 27) // 54), 'crafted' if i == 0 else f'{name("stormreach:" + bt[i - 1][1] + "_backpack")} + {name("stormreach:" + tid + "_backpack_upgrade")}'))
rows.append(("Storm Chaser's Pack", 45, 72, 2, 'achievement reward only; built-in Magnetism'))
out.append(md_table(['Backpack', 'Slots', 'Slots with Capacity III', 'Pages (max)', 'How to get'], rows))
# ---------------------------------------------------------------- achievement rewards
adv = json.load(open(ROOT / 'tools/advancements5.json'))
REW = {'lightning_rod': 'Lightning Charm', 'into_the_cage': "Storm Chaser's Cap", 'ef5': "Chaser's Badge", 'violent': 'Tornado Trophy',
       'keeper': 'Keeper Trophy', 'cyrrus': 'Cyrrus Trophy', 'packrat': "Storm Chaser's Pack"}
out.append('\n### Achievements with rewards\n\nEach reward item comes from exactly one achievement. The Sovereign Trophy is the reward for the existing '
           '*Regicide* achievement (defeat the Tempest Sovereign, +200 XP).\n\n')
rows = [(a['title'], a['description'], REW.get(a['reward'], '—'), a['xp'] or '—', 'Yes' if a['hidden'] else 'No') for a in adv if a['reward'] or a['xp']]
out.append(md_table(['Achievement', 'How', 'Item reward', 'XP', 'Hidden'], rows))
open(ROOT / 'tools/guide_tables.md', 'w').write('\n'.join(out))
print('guide tables written:', sum(s.count('\n|') for s in out), 'table rows')
