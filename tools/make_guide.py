"""Builds GUIDE.md from guide_template.md and the computed tables (run guide_tables.py first)."""
import pathlib, runpy
here = pathlib.Path(__file__).resolve().parent
runpy.run_path(str(here / 'guide_tables.py'))
t = (here / 'guide_template.md').read_text().replace('{{TABLES}}', (here / 'guide_tables.md').read_text())
(here.parent / 'GUIDE.md').write_text(t)
print('GUIDE.md written')
