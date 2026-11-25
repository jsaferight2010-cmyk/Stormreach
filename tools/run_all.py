"""Regenerates every texture and JSON file under src/main/resources from the scripts in this folder.
Needs Python 3 with Pillow and nbtlib (pip install pillow nbtlib) and the vanilla 26.3 jar extracted into .vanilla/ (see CLAUDE.md)."""
import os, pathlib, runpy, shutil, sys
here = pathlib.Path(__file__).resolve().parent
res = here.parent / 'src' / 'main' / 'resources'
vanilla = pathlib.Path(os.environ.get('STORMREACH_VANILLA', here.parent / '.vanilla'))
if not (vanilla / 'assets' / 'minecraft' / 'blockstates').is_dir():
    sys.exit(f'vanilla assets not found at {vanilla}: extract the 26.3 jar there or set STORMREACH_VANILLA (nothing was changed)')
# the .ogg files come from audio5.py (needs Piper TTS), so they are kept rather than regenerated here
snd = res / 'assets' / 'stormreach' / 'sounds'; keep = here / '.sounds_keep'
shutil.rmtree(keep, ignore_errors=True)
if snd.exists(): shutil.copytree(snd, keep)
for d in ('assets', 'data'):
    shutil.rmtree(res / d, ignore_errors=True)
try:
  for s in ('textures.py', 'textures2.py', 'textures3.py', 'gendata.py', 'gendata2.py', 'gendata3.py', 'gendata5.py', 'gendata4.py', 'textures4.py', 'gendata6.py', 'gendata7.py', 'textures5.py', 'gendata8.py', 'fixorder.py'):
    print('==', s); runpy.run_path(str(here / s), run_name='__main__')
finally:
  # put the sounds back even if a script failed
  if keep.exists(): shutil.copytree(keep, snd, dirs_exist_ok=True); shutil.rmtree(keep)
print('done. now run: python tools/validate.py and python tools/featorder.py')
