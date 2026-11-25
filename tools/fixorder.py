"""Re-sorts every Stormreach biome's feature lists into one global order that agrees with vanilla,
so Minecraft's feature-order check can never find a cycle."""
import os as _os, pathlib as _pl
ROOT = str(_pl.Path(__file__).resolve().parent.parent)  # the Stormreach project folder
VANILLA = _os.environ.get('STORMREACH_VANILLA', str(_pl.Path(ROOT) / '.vanilla'))  # extracted 26.3 jar (assets/ + data/)
MCJAR = _os.environ.get('STORMREACH_MCJAR', str(_pl.Path(ROOT) / '.vanilla' / 'minecraft-26.3.jar'))
import json, glob, heapq
from collections import defaultdict
V = VANILLA + '/data/minecraft/worldgen/biome/*.json'
O = ROOT + '/src/main/resources/data/stormreach/worldgen/biome/*.json'
OUR_ORDER = ['stormreach:stormstone_boulder', 'stormreach:ore_stormstone', 'stormreach:ore_storm_crystal', 'stormreach:ore_storm_crystal_rich',
             'stormreach:disk_salt', 'stormreach:trees_thunderspire', 'stormreach:trees_marsh', 'stormreach:fallen_driftwood',
             'stormreach:charged_spire', 'stormreach:patch_saltgrass', 'stormreach:salt_crystals', 'stormreach:patch_sea_thrift',
             'stormreach:patch_sea_rocket', 'stormreach:patch_stormberry_shrub']
edges = defaultdict(set); indeg = defaultdict(int); nodes = set()
for f in glob.glob(V):
    b = json.load(open(f))
    flat = [(s, x) for s, fs in enumerate(b['features']) for x in fs]
    for n in flat: nodes.add(n)
    for a, c in zip(flat, flat[1:]):
        if c not in edges[a]: edges[a].add(c); indeg[c] += 1
heap = [n for n in nodes if indeg[n] == 0]; heapq.heapify(heap); rank = {}
while heap:
    n = heapq.heappop(heap); rank[n] = len(rank)
    for m in sorted(edges[n]):
        indeg[m] -= 1
        if indeg[m] == 0: heapq.heappush(heap, m)
assert len(rank) == len(nodes), 'vanilla has a cycle?'
BIG = 10 ** 6
for f in glob.glob(O):
    b = json.load(open(f))
    for s, fs in enumerate(b['features']):
        def key(x):
            if (s, x) in rank: return rank[(s, x)]
            return BIG + (OUR_ORDER.index(x) if x in OUR_ORDER else 999)
        b['features'][s] = sorted(dict.fromkeys(fs), key=key)
    json.dump(b, open(f, 'w'), indent=2); open(f, 'a').write('\n')
print('feature order fixed')
