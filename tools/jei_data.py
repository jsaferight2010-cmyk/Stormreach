"""Writes assets/stormreach/jei/drops.tsv for the JEI plugin (client/compat/jei): every Stormreach mob's drops, with
count and chance, computed from the mod's own loot tables (reusing guide_tables' parser) so JEI never drifts from
the jar. Runs after the gendata scripts."""
import json, pathlib, sys
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from guide_tables import table_stats, D, R

out = {}
for p in sorted((D / 'loot_table/entities').glob('*.json')):
    egg = R / 'assets/stormreach/items' / f'{p.stem}_spawn_egg.json'
    if not egg.exists(): continue
    try:
        stats = table_stats(p)
    except (KeyError, TypeError, ValueError):
        continue   # a table in a shape the parser doesn't follow (some generated Storm mobs reference vanilla tables)
    rows = [{'item': i if ':' in i else f'minecraft:{i}', 'count': txt, 'chance': round(chance, 4)}
            for i, (chance, _exp, txt) in sorted(stats.items(), key=lambda kv: -kv[1][0])]
    if rows: out[f'stormreach:{p.stem}'] = rows
# 6.0: the Sovereign's Tempest Eye appears over the altar (not as loot), but it's still what beating it gives
out.setdefault('stormreach:tempest_sovereign', []).insert(0, {'item': 'stormreach:tempest_eye', 'count': '1', 'chance': 1.0})
dst = R / 'assets/stormreach/jei/drops.tsv'
dst.parent.mkdir(parents=True, exist_ok=True)
old = R / 'assets/stormreach/jei/drops.json'
if old.exists(): old.unlink()
dst.write_text(''.join(f"{mob}\t{r['item']}\t{r['count']}\t{r['chance']}\n" for mob, rows in out.items() for r in rows), encoding='utf-8')
print(f'jei_data: drops for {len(out)} mobs')
