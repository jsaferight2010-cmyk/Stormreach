"""Builds the two in-game guidebooks' content from the project's own files:
- assets/stormreach/guide/guide.json: the Stormreach Field Guide (the beginner's book), from tools/field_guide.md
  (6.0: its own text for new players, with no rarity classes or drop chances; GUIDE.md keeps the full tables)
- assets/stormreach/guide/crafting.json: the Storm Crafter's Handbook, every Stormreach recipe, read from data/stormreach/recipe
The client draws the crafting pages with real item icons. Re-run after changing GUIDE.md or recipes (run_all.py does)."""
import glob, json, os, pathlib, re
ROOT = pathlib.Path(__file__).resolve().parent.parent
RES = ROOT / 'src' / 'main' / 'resources'
OUT = RES / 'assets' / 'stormreach' / 'guide'
OUT.mkdir(parents=True, exist_ok=True)

def clean(t):
    t = re.sub(r'\*\*([^*]+)\*\*', r'\1', t)
    t = re.sub(r'\*([^*]+)\*', r'\1', t)
    t = re.sub(r'`([^`]+)`', r'\1', t)
    t = re.sub(r'\[([^\]]+)\]\([^)]*\)', r'\1', t)
    return t.strip()

chapters, cur = [], None
guide = (ROOT / 'tools' / 'field_guide.md').read_text(encoding='utf-8')
guide = re.sub(r'<!--.*?-->', '', guide, flags=re.S)
RARITY = re.compile(r'\b(common|uncommon|rare|very rare|epic|legendary)\b|\d+(\.\d+)?\s*%', re.I)
for raw in guide.splitlines():
    if RARITY.search(raw) and not raw.lstrip().startswith('#'):
        raise SystemExit(f'field_guide.md: the beginner guide must not classify rarity or give chances: {raw.strip()}')
    line = raw.rstrip()
    if line.startswith('## '):
        cur = {'title': clean(line[3:]), 'lines': []}
        chapters.append(cur)
        continue
    if cur is None or not line.strip() or line.strip() == '---': continue
    s = line.strip()
    if s.startswith('### '): cur['lines'].append({'s': 'h', 't': clean(s[4:])})
    elif s.startswith('|'):
        cells = [clean(c) for c in s.strip('|').split('|')]
        if all(set(c) <= set('-: ') for c in cells): continue
        cur['lines'].append({'s': 'table', 't': '  |  '.join(cells)})
    elif re.match(r'^\d+\. ', s):
        cur['lines'].append({'s': 'bullet', 'd': 0, 't': clean(s)})
    elif re.match(r'^\s*[-*] ', line):
        depth = (len(line) - len(line.lstrip())) // 2
        cur['lines'].append({'s': 'bullet', 'd': depth, 't': clean(re.sub(r'^[-*] ', '', s))})
    elif s.startswith('>'): cur['lines'].append({'s': 'note', 't': clean(s.lstrip('> '))})
    else: cur['lines'].append({'s': 'p', 't': clean(s)})
json.dump({'chapters': chapters}, open(OUT / 'guide.json', 'w'), indent=1)

def ing(x):
    """An ingredient as a single id string ('#tag' for tags); lists take their first option."""
    if isinstance(x, list): x = x[0] if x else ''
    if isinstance(x, dict): x = x.get('item') or ('#' + x['tag'] if 'tag' in x else '')
    return x or ''

recipes = []
for f in sorted(glob.glob(str(RES / 'data' / 'stormreach' / 'recipe' / '**' / '*.json'), recursive=True)):
    d = json.load(open(f))
    t = d['type'].split(':')[1]
    res = d.get('result') or d.get('output') or {}
    rid, count = (res.get('id'), res.get('count', 1)) if isinstance(res, dict) else (res, 1)
    if t == 'brewing':   # 6.0: brewing recipes were missing from the handbook
        out = d['output']; pc = out.get('components', {}).get('minecraft:potion_contents', {})
        recipes.append({'result': out['id'], 'count': 1, 'category': 'brewing', 'kind': 'brewing', 'potion': pc.get('potion', ''),
                        'input_potion': d['input'].get('potion_contents', {}).get('potions', ''),
                        'grid': [ing(d['reagent']), d['input']['item']] + [''] * 7})
        continue
    if not rid: continue
    r = {'result': rid, 'count': count, 'category': d.get('category', 'misc')}
    if t == 'crafting_shaped':
        pat = d['pattern']; key = d['key']
        grid = [''] * 9
        for y, row in enumerate(pat):
            for x, ch in enumerate(row):
                if ch != ' ': grid[y * 3 + x] = ing(key[ch])
        r.update(kind='shaped', grid=grid)
    elif t == 'crafting_shapeless':
        grid = [ing(i) for i in d['ingredients']][:9]
        r.update(kind='shapeless', grid=grid + [''] * (9 - len(grid)))
    elif t == 'crafting_transmute':
        r.update(kind='shapeless', grid=[ing(d['input']), ing(d['material'])] + [''] * 7)
    elif t in ('smelting', 'blasting', 'smoking', 'campfire_cooking', 'stonecutting'):
        r.update(kind=t, grid=[ing(d['ingredient'])] + [''] * 8)
    else: continue
    recipes.append(r)
order = {'building': 0, 'equipment': 1, 'redstone': 2, 'food': 3, 'blocks': 4, 'misc': 5, 'brewing': 6}
recipes.sort(key=lambda r: (order.get(r['category'], 9), r['result'], r.get('potion', '')))
json.dump({'recipes': recipes}, open(OUT / 'crafting.json', 'w'), indent=1)
print(f'guidebooks: {len(chapters)} chapters, {len(recipes)} recipes')
