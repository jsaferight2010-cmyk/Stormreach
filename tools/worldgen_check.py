"""Static checks for Stormreach world generation data (runs before the game ever loads it).

Catches the class of bug behind the 5.5.0 crash "Tried to biome check an unregistered feature, or a
feature that should not restrict the biome": a placed feature that carries a `minecraft:biome` placement
filter is only legal as a *top-level* biome decoration step. Nested inside a selector (random_selector,
simple_random_selector, random_boolean_selector, random_patch, vegetation_patch, root_system, ...) the
biome check has no top-level feature to look up and Minecraft throws during chunk generation.

Checks:
  1. every placed feature listed in a biome resolves (ours or vanilla)
  2. every placed feature points at a configured feature that exists
  3. every placed-feature reference nested in a configured feature resolves, recursively
  4. no nested placed feature (by id or inline) has a minecraft:biome filter
  5. every placed feature with a minecraft:biome filter is used as a top-level step of at least one biome
     (otherwise it can only ever be placed in a context where the biome check fails)
  6. the same rules hold for vanilla's own data (self-test of the checker)

Run directly (python3 tools/worldgen_check.py) or import problems() from validate.py.
"""
import glob, json, os, pathlib, sys

ROOT = str(pathlib.Path(__file__).resolve().parent.parent)
VANILLA = os.environ.get('STORMREACH_VANILLA', str(pathlib.Path(ROOT) / '.vanilla'))
OURS = ROOT + '/src/main/resources/data'

# config keys whose values are placed features (id string, inline object, list, or weighted entries)
PF_KEYS = {'default', 'feature', 'features', 'feature_true', 'feature_false', 'vegetation_feature', 'placed_feature'}


def _load(root, kind):
    out = {}
    for f in glob.glob(f'{root}/*/worldgen/{kind}/**/*.json', recursive=True):
        ns = f[len(root) + 1:].split('/')[0]
        rel = f.split(f'/worldgen/{kind}/')[1][:-5]
        out[f'{ns}:{rel}'] = json.load(open(f))
    return out


def _norm(i):
    return i if ':' in i else 'minecraft:' + i


def _has_biome_filter(placed):
    return any(isinstance(m, dict) and m.get('type') == 'minecraft:biome' for m in placed.get('placement', []))


def check(roots):
    configured, placed, biomes = {}, {}, {}
    for r in roots:
        configured.update(_load(r, 'feature'))
        placed.update(_load(r, 'placed_feature'))
        biomes.update(_load(r, 'biome'))
    problems = []
    top_level = set()
    for b, data in biomes.items():
        for step in data.get('features', []):
            for pf in (step if isinstance(step, list) else [step]):
                pf = _norm(pf)
                top_level.add(pf)
                if pf not in placed:
                    problems.append(f'biome {b}: placed feature {pf} does not exist')

    def visit_configured(cf, where, seen):
        if isinstance(cf, str):
            cf = _norm(cf)
            if cf not in configured:
                problems.append(f'{where}: configured feature {cf} does not exist')
                return
            if cf in seen:
                return
            seen = seen | {cf}
            body, where = configured[cf], f'{where} -> {cf}'
        else:
            body = cf
        walk(body, where, seen)

    def visit_placed(pf, where, seen, nested):
        if isinstance(pf, dict) and 'chance' in pf and 'feature' in pf:  # weighted entry
            pf = pf['feature']
        if isinstance(pf, str):
            pf = _norm(pf)
            if pf not in placed:
                problems.append(f'{where}: placed feature {pf} does not exist')
                return
            body, label = placed[pf], pf
        elif isinstance(pf, dict) and 'feature' in pf:
            body, label = pf, '(inline placed feature)'
        elif isinstance(pf, dict) and 'type' in pf:  # inline configured feature used directly
            walk(pf, where, seen)
            return
        else:
            return
        if nested and _has_biome_filter(body):
            problems.append(f'{where}: nested placed feature {label} has a minecraft:biome filter '
                            '(only legal on top-level biome steps; crashes chunk generation)')
        visit_configured(body['feature'], f'{where} -> {label}', seen)

    def walk(node, where, seen):
        if isinstance(node, dict):
            for k, v in node.items():
                if k in PF_KEYS and k != 'feature' or (k == 'feature' and isinstance(node, dict) and 'type' in node):
                    for x in (v if isinstance(v, list) else [v]):
                        visit_placed(x, where, seen, nested=True)
                elif isinstance(v, (dict, list)):
                    walk(v, where, seen)
        elif isinstance(node, list):
            for x in node:
                walk(x, where, seen)

    for pf, body in placed.items():
        visit_placed(pf, 'top-level', frozenset(), nested=False)
        if _has_biome_filter(body) and pf not in top_level:
            problems.append(f'placed {pf} has a minecraft:biome filter but no biome lists it as a top-level step')
    return sorted(set(problems))


def problems():
    vanilla = check([VANILLA + '/data'])
    if vanilla:
        return ['worldgen_check self-test failed on vanilla data: ' + p for p in vanilla[:5]]
    ours = [p for p in check([VANILLA + '/data', OURS]) if 'stormreach:' in p]
    return ours


if __name__ == '__main__':
    ps = problems()
    print('\n'.join(ps) if ps else 'WORLDGEN CHECKS PASSED')
    sys.exit(1 if ps else 0)
