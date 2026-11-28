import os as _os, pathlib as _pl
ROOT = str(_pl.Path(__file__).resolve().parent.parent)  # the Stormreach project folder
VANILLA = _os.environ.get('STORMREACH_VANILLA', str(_pl.Path(ROOT) / '.vanilla'))  # extracted 26.3 jar (assets/ + data/)
MCJAR = _os.environ.get('STORMREACH_MCJAR', str(_pl.Path(ROOT) / '.vanilla' / 'minecraft-26.3.jar'))
import json,glob,sys
from collections import defaultdict
V=VANILLA + '/data/minecraft/worldgen/biome/*.json'
O=ROOT + '/src/main/resources/data/stormreach/worldgen/biome/*.json'
def check(files):
    biomes=[json.load(open(f)) for f in files]
    # vanilla sorts by (step, feature) jointly: nodes are (step, feature)
    edges=defaultdict(set); indeg=defaultdict(int); nodes=set()
    for b in biomes:
        flat=[(s,x) for s,fs in enumerate(b['features']) for x in fs]
        for i in range(len(flat)-1):
            a,c=flat[i],flat[i+1]
            nodes.update([a,c])
            if c not in edges[a]: edges[a].add(c); indeg[c]+=1
    q=[n for n in nodes if indeg[n]==0]; seen=0
    while q:
        n=q.pop(); seen+=1
        for m in edges[n]:
            indeg[m]-=1
            if indeg[m]==0: q.append(m)
    return len(nodes)-seen, [n for n in nodes if indeg[n]>0]
print('vanilla only:', check(glob.glob(V))[0])
bad, rest = check(glob.glob(V)+glob.glob(O))
print('with ours:', bad); print(sorted(rest)[:30])
