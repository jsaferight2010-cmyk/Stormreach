"""Stormreach 5.1 audio: every sound effect in tools/sfx_list.py, synthesised from scratch (numpy + scipy; no samples,
no recordings, nothing taken from Minecraft), and the new Labyrinth score: three synced layers (calm, tense, chase)
built on an original, slow, piano-and-pad piece in the spirit of quiet ambient game music.
Needs numpy, scipy and ffmpeg with libvorbis. Writes src/main/resources/assets/stormreach/sounds/sfx/*.ogg and
replaces sounds/music/labyrinth_{calm,tense,chase}.ogg. run_all.py keeps the sounds folder, so run this by hand."""
import os, pathlib, subprocess, sys, tempfile
import numpy as np
from scipy.io import wavfile
from scipy.signal import butter, sosfilt, fftconvolve
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from sfx_list import SFX

SR = 44100
ROOT = pathlib.Path(__file__).resolve().parent.parent
SND = ROOT / 'src' / 'main' / 'resources' / 'assets' / 'stormreach' / 'sounds'
rng = np.random.default_rng(5150)

def ogg(path, data, stereo=False):
    path = pathlib.Path(path); path.parent.mkdir(parents=True, exist_ok=True)
    data = np.asarray(data, dtype=np.float64)
    peak = np.max(np.abs(data)) or 1.0
    data = (data / peak * 0.89 * 32767).astype(np.int16)
    with tempfile.NamedTemporaryFile(suffix='.wav', delete=False) as t:
        wavfile.write(t.name, SR, data)
    subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', t.name, '-ac', '2' if stereo else '1', '-c:a', 'libvorbis', '-q:a', '5', str(path)], check=True)
    os.unlink(t.name)

def lp(x, hz, o=4): return sosfilt(butter(o, min(hz, SR / 2 - 100), 'low', fs=SR, output='sos'), x)
def hp(x, hz, o=4): return sosfilt(butter(o, hz, 'high', fs=SR, output='sos'), x)
def bp(x, lo, hi, o=3): return sosfilt(butter(o, [lo, min(hi, SR / 2 - 100)], 'band', fs=SR, output='sos'), x)
def t_(sec): return np.arange(int(sec * SR)) / SR
def noise(sec): return rng.standard_normal(int(sec * SR))
def adsr(n, a=0.01, d=0.1, s=0.7, r=0.2):
    e = np.full(n, float(s)); ai, di, ri = max(1, int(a * SR)), max(1, int(d * SR)), max(1, int(r * SR))
    ai, ri = min(ai, n), min(ri, n)
    e[:ai] = np.linspace(0, 1, ai)
    k = max(0, min(di, n - ai)); e[ai:ai + k] = np.linspace(1, s, di)[:k]
    e[n - ri:] *= np.linspace(1, 0, ri); return e
def expdecay(sec, k): return np.exp(-t_(sec) * k)
def glide(f0, f1, sec, curve=1.0):
    t = t_(sec); f = f0 + (f1 - f0) * (t / sec) ** curve
    return 2 * np.pi * np.cumsum(f) / SR
def reverb(x, sec=1.6, mix=0.35, damp=3000):
    ir = noise(sec) * np.exp(-t_(sec) * (6.9 / sec)); ir = lp(ir, damp); ir /= np.max(np.abs(ir))
    wet = fftconvolve(x, ir)[:len(x) + int(sec * SR)]
    out = np.zeros(len(wet)); out[:len(x)] += x * (1 - mix); return out + wet / (np.max(np.abs(wet)) or 1) * mix * np.max(np.abs(x))
def mix(*xs):
    n = max(len(x) for x in xs); out = np.zeros(n)
    for x in xs: out[:len(x)] += x
    return out
def place(buf, x, at):
    i = int(at * SR); j = min(len(buf), i + len(x))
    if i < len(buf): buf[i:j] += x[:j - i]

# ---------------------------------------------------------------------------------------------- sound effects
def thunder():
    crack = bp(noise(0.25), 1500, 9000) * expdecay(0.25, 18)
    rumble = lp(noise(4.0), 180) * (np.exp(-t_(4.0) * 0.9)) * (0.6 + 0.4 * np.abs(np.sin(2 * np.pi * 1.3 * t_(4.0))))
    out = rumble * 3.0; place(out, crack * 0.8, 0.0); place(out, bp(noise(0.6), 300, 2500) * expdecay(0.6, 6) * 0.6, 0.05)
    return reverb(out, 2.0, 0.3, 1200)
def gust():
    t = t_(1.6); f = 250 + 700 * np.sin(np.pi * t / 1.6) ** 2
    x = np.zeros(len(t))
    for lo in (0.8, 1.0, 1.25):  # a few band layers swept together read as moving air
        x += bp(noise(1.6), 150 * lo, 1400 * lo)
    return x * np.sin(np.pi * t / 1.6) ** 1.5 * (0.7 + 0.3 * np.sin(2 * np.pi * f / 400 * t))
def wind():
    t = t_(5.0); x = bp(noise(5.0), 120, 900) * (0.6 + 0.4 * np.sin(2 * np.pi * 0.37 * t) * np.sin(2 * np.pi * 0.11 * t + 1))
    whistle = np.sin(glide(620, 540, 5.0)) * 0.06 * (0.5 + 0.5 * np.sin(2 * np.pi * 0.23 * t))
    return (x + whistle) * adsr(len(t), 0.6, 0.1, 1.0, 0.8)
def bell(f, sec=2.2, bright=1.0):
    t = t_(sec)
    return sum(np.sin(2 * np.pi * f * m * t) * g * np.exp(-t * d) for m, g, d in ((1, 1, 2.2), (2.01, 0.55 * bright, 3.3), (3.03, 0.3 * bright, 5), (4.2, 0.18 * bright, 8), (6.8, 0.1 * bright, 12)))
def chime(): out = mix(bell(1318.5), 0.6 * np.concatenate([np.zeros(int(0.07 * SR)), bell(1975.5)])); return reverb(out, 1.2, 0.3)
def shatter():
    out = np.zeros(int(0.9 * SR))
    for k in range(14):
        at = rng.random() * 0.15; f = 2000 + rng.random() * 5000
        place(out, np.sin(2 * np.pi * f * t_(0.25)) * np.exp(-t_(0.25) * (20 + rng.random() * 30)) * (0.3 + rng.random()), at)
    place(out, hp(noise(0.3), 3000) * expdecay(0.3, 15), 0)
    return out
def hum():
    t = t_(3.0); x = sum(np.sin(2 * np.pi * f * t) * g for f, g in ((82.4, 1), (164.8, 0.4), (247.2, 0.2), (329.6, 0.12)))
    return x * (0.75 + 0.25 * np.sin(2 * np.pi * 4.2 * t)) * adsr(len(t), 0.3, 0.1, 1, 0.5)
def activate():
    t = t_(2.0); sweep = np.sin(glide(110, 880, 2.0, 2.2)) * adsr(len(t), 0.05, 0.1, 1, 0.4)
    shimmer = sum(bell(f, 2.0, 0.6) for f in (880, 1108.7, 1318.5)) * 0.25
    zap = bp(noise(2.0), 2000, 8000) * np.exp(-((t - 1.2) ** 2) * 30) * 0.3
    return reverb(sweep * 0.6 + shimmer + zap, 1.4, 0.35)
def dispel():
    out = np.zeros(int(2.8 * SR))
    for k, f in enumerate((523.3, 659.3, 784.0, 1046.5, 1318.5)):
        place(out, bell(f, 1.8, 0.7) * 0.5, k * 0.12)
    air = bp(noise(2.8), 400, 3000) * np.sin(np.pi * t_(2.8) / 2.8) ** 2 * 0.35
    return reverb(out + air, 1.8, 0.4)
def warp():
    t = t_(0.9); return (np.sin(glide(900, 120, 0.9, 0.6)) * 0.6 + bp(noise(0.9), 300, 4000) * 0.3) * adsr(len(t), 0.01, 0.1, 0.8, 0.4)
def metal_hit(f=420, sec=0.9, k=6.0):
    t = t_(sec)
    ring = sum(np.sin(2 * np.pi * f * m * t) * g * np.exp(-t * k * m ** 0.5) for m, g in ((1, 1), (2.32, 0.6), (3.87, 0.4), (5.41, 0.25)))
    return ring + bp(noise(sec), 800, 6000) * expdecay(sec, 40) * 0.8
def metal_place(): out = metal_hit(310, 1.0, 5); place(out, metal_hit(470, 0.6, 8) * 0.6, 0.12); return out
def equip_pack():
    out = np.zeros(int(0.7 * SR))
    for at in (0.0, 0.12, 0.2, 0.31):
        place(out, bp(noise(0.12), 600, 4000) * adsr(int(0.12 * SR), 0.01, 0.02, 0.6, 0.08), at)
    place(out, metal_hit(1800, 0.25, 25) * 0.4, 0.42)  # the buckle
    return out
def equip_metal(): out = metal_hit(980, 0.5, 14) * 0.7; place(out, metal_hit(1320, 0.4, 16) * 0.5, 0.08); return out
def bottle():
    out = np.zeros(int(0.8 * SR))
    for k in range(9):
        f = 300 + k * 60 + rng.random() * 40
        place(out, np.sin(glide(f, f * 1.6, 0.08)) * np.exp(-t_(0.08) * 30) * 0.5, 0.05 + k * 0.07)
    return out
def pick(): out = bp(noise(0.18), 1500, 7000) * expdecay(0.18, 30); place(out, np.sin(glide(900, 500, 0.12)) * expdecay(0.12, 25) * 0.4, 0.03); return out
def pop(): t = t_(0.25); return np.sin(glide(180, 520, 0.25, 0.5)) * np.exp(-t * 14) + bp(noise(0.25), 200, 2000) * expdecay(0.25, 30) * 0.3
def throw(): t = t_(0.45); return bp(noise(0.45), 300, 2500) * np.sin(np.pi * t / 0.45) ** 2
def spear_hit(): return metal_hit(700, 0.4, 18) * 0.6 + lp(noise(0.4), 400) * expdecay(0.4, 20)
def chain():
    out = np.zeros(int(0.5 * SR))
    for k in range(6): place(out, metal_hit(1200 + rng.random() * 900, 0.15, 35) * (0.4 + rng.random() * 0.5), k * 0.045 + rng.random() * 0.02)
    return out
def stone_tap(): t = t_(0.12); return bp(noise(0.12), 1800, 7000) * np.exp(-t * 60) + np.sin(2 * np.pi * 2900 * t) * np.exp(-t * 80) * 0.4
def grind():
    t = t_(1.8); x = lp(noise(1.8), 700) * (0.5 + 0.5 * np.abs(np.sin(2 * np.pi * 9 * t + np.sin(2 * np.pi * 1.7 * t) * 3)))
    return x * adsr(len(t), 0.1, 0.1, 1, 0.3) + bp(noise(1.8), 2000, 5000) * 0.1
def boom():
    t = t_(3.0); body = np.sin(glide(70, 28, 3.0, 0.4)) * np.exp(-t * 1.6)
    return reverb(body * 1.4 + lp(noise(3.0), 220) * np.exp(-t * 2.2), 2.2, 0.35, 900)
def door_open():
    t = t_(1.2); creak = bp(noise(1.2), 300, 1600) * (0.4 + 0.6 * np.abs(np.sin(glide(14, 6, 1.2))))
    out = creak * adsr(len(t), 0.05, 0.1, 0.8, 0.3) * 0.6; place(out, metal_hit(260, 0.9, 5) * 0.7, 0.85); return out
def door_close(): out = metal_hit(220, 1.0, 6); place(out, lp(noise(0.3), 500) * expdecay(0.3, 15), 0); return out
def horn():
    t = t_(3.2); f = 92.5 * (1 + 0.004 * np.sin(2 * np.pi * 5 * t))
    saw = 2 * ((np.cumsum(f) / SR) % 1) - 1
    x = bp(saw, 150, 1400) + 0.5 * bp(2 * ((np.cumsum(f * 1.5) / SR) % 1) - 1, 200, 1200)
    return reverb(x * adsr(len(t), 0.35, 0.2, 0.85, 0.9), 2.4, 0.4, 2000)
def chirp(f0, f1, sec, n=2, gap=0.09, vib=0.0):
    out = np.zeros(int((sec + gap) * n * SR) + 1)
    for k in range(n):
        t = t_(sec); ph = glide(f0, f1, sec, 0.7) + vib * np.sin(2 * np.pi * 30 * t)
        place(out, (np.sin(ph) + 0.3 * np.sin(2 * ph)) * adsr(len(t), 0.005, 0.02, 0.8, sec * 0.4), k * (sec + gap))
    return out
def small_ambient(): return chirp(1800, 2600, 0.08, 3, 0.06, 2.0)
def small_hurt(): return mix(chirp(2400, 1300, 0.16, 1), bp(noise(0.2), 2000, 6000) * expdecay(0.2, 25) * 0.3)
def small_death(): out = chirp(2000, 600, 0.45, 1); place(out, bp(noise(0.4), 500, 3000) * expdecay(0.4, 10) * 0.4, 0.3); return out
def glide_call():
    t = t_(1.6); ph = glide(420, 760, 1.6, 1.5) + 3 * np.sin(2 * np.pi * 5.5 * t)
    return reverb((np.sin(ph) + 0.25 * np.sin(2.01 * ph)) * adsr(len(t), 0.15, 0.1, 0.8, 0.5), 1.2, 0.3)
def glide_hurt(): t = t_(0.4); return (np.sin(glide(700, 380, 0.4)) + 0.3 * bp(noise(0.4), 400, 2500)) * adsr(len(t), 0.005, 0.05, 0.7, 0.2)
def glide_death(): t = t_(1.4); return reverb(np.sin(glide(600, 160, 1.4, 0.7)) * adsr(len(t), 0.01, 0.1, 0.8, 0.8), 1.2, 0.35)
def wind_hurt(): return mix(gust()[:int(0.5 * SR)] * np.linspace(1, 0, int(0.5 * SR)), chirp(900, 500, 0.2, 1) * 0.3)
def wind_death():
    t = t_(1.8); x = bp(noise(1.8), 200, 3000) * np.exp(-t * 1.2)
    return x + np.sin(glide(800, 90, 1.8, 0.5)) * np.exp(-t * 1.5) * 0.4
def titan(f0, f1, sec, grit=0.6):
    t = t_(sec); ph = glide(f0, f1, sec, 0.8)
    saw = 2 * ((ph / (2 * np.pi)) % 1) - 1
    voice = sum(bp(saw, lo, hi) * g for lo, hi, g in ((90, 300, 1.0), (500, 900, 0.6), (1400, 2600, 0.25)))  # formants
    return voice * (1 - grit + grit * np.abs(lp(noise(sec), 60)) * 3) * adsr(len(t), sec * 0.12, 0.1, 0.9, sec * 0.35)
def titan_ambient(): return reverb(titan(55, 48, 2.6, 0.5), 1.6, 0.3, 1500)
def titan_roar(): return reverb(titan(70, 42, 3.0, 0.7) + lp(noise(3.0), 150) * np.sin(np.pi * t_(3.0) / 3.0) * 0.6, 2.0, 0.3, 1500)
def titan_hurt(): return titan(110, 70, 0.7, 0.6)
def titan_death():
    out = reverb(titan(80, 22, 4.5, 0.8), 2.8, 0.35, 1200); place(out, boom()[:int(3.0 * SR)] * 0.7, 3.0); return out
def titan_charge():
    t = t_(1.6); return np.sin(glide(60, 400, 1.6, 2.5)) * (t / 1.6) ** 2 + bp(noise(1.6), 1000, 6000) * (t / 1.6) ** 3 * 0.4
def titan_blast(): out = boom()[:int(1.8 * SR)]; place(out, hp(noise(0.5), 600) * expdecay(0.5, 9) * 1.2, 0); return out
def hail():
    out = np.zeros(int(1.5 * SR))
    for k in range(40): place(out, stone_tap() * (0.2 + rng.random() * 0.6), rng.random() * 1.4)
    return out
def splash():
    t = t_(0.5); x = bp(noise(0.5), 600, 6000) * np.exp(-t * 9)
    for k in range(6): place(x, np.sin(glide(800 + rng.random() * 800, 1600 + rng.random() * 900, 0.05)) * expdecay(0.05, 40) * 0.3, 0.05 + rng.random() * 0.3)
    return x
def radar_ping(): t = t_(1.2); return reverb(np.sin(2 * np.pi * 1760 * t) * np.exp(-t * 6) + np.sin(2 * np.pi * 3520 * t) * np.exp(-t * 12) * 0.3, 1.0, 0.4)

# ---- block material sounds: break (a crunch with debris), step (a short footfall), place (a solid set-down)
def knock(lo, hi, sec, k): return bp(noise(sec), lo, hi) * expdecay(sec, k)
def tone_hit(f, sec, k): return np.sin(2 * np.pi * f * t_(sec)) * expdecay(sec, k)
def debris(n, lo, hi, spread=0.25):
    out = np.zeros(int((spread + 0.2) * SR))
    for i in range(n): place(out, knock(lo, hi, 0.08, 55) * (0.3 + rng.random() * 0.7), rng.random() * spread)
    return out
MAT = {
    'stone': (lambda: mix(knock(150, 2500, 0.3, 14), debris(9, 800, 5000)), lambda: mix(knock(200, 1800, 0.09, 45), tone_hit(140, 0.06, 60) * 0.3),
              lambda: mix(knock(120, 1500, 0.25, 18), tone_hit(95, 0.2, 20) * 0.5)),
    'crystal': (lambda: mix(shatter()[:int(0.6 * SR)], bell(1760, 0.6, 0.8) * 0.3), lambda: mix(tone_hit(2637, 0.12, 30) * 0.4, knock(2000, 8000, 0.05, 70) * 0.4),
                lambda: mix(bell(1318.5, 0.5, 0.6) * 0.5, knock(1000, 6000, 0.08, 50) * 0.4)),
    'metal': (lambda: mix(metal_hit(520, 0.6, 9), debris(5, 1500, 7000)), lambda: metal_hit(880, 0.12, 40) * 0.5, lambda: metal_hit(340, 0.45, 10)),
    'soft': (lambda: knock(150, 1800, 0.3, 12), lambda: knock(250, 2500, 0.1, 40) * 0.6, lambda: knock(100, 1200, 0.2, 18)),
    'plant': (lambda: mix(debris(14, 1500, 7000, 0.3), knock(400, 3000, 0.2, 14) * 0.5), lambda: debris(4, 2000, 7000, 0.08),
              lambda: debris(8, 1200, 6000, 0.15)),
    'loose': (lambda: mix(debris(18, 300, 4000, 0.3), knock(80, 900, 0.3, 12)), lambda: debris(5, 400, 4000, 0.1), lambda: debris(10, 200, 3000, 0.2)),
    'glass': (lambda: shatter(), lambda: mix(tone_hit(3100, 0.08, 45) * 0.4, knock(3000, 9000, 0.04, 80) * 0.5), lambda: mix(tone_hit(2200, 0.25, 18) * 0.5, knock(2000, 8000, 0.06, 60) * 0.4)),
}
MAT_SYNTH = {f'block.mat.{m}.{ev}': fns[i] for m, fns in MAT.items() for i, ev in enumerate(('break', 'step', 'place'))}

SYNTH = {
    'weather.thunder': thunder, 'weather.gust': gust, 'weather.wind': wind, 'magic.chime': chime, 'magic.shatter': shatter, 'magic.hum': hum,
    'magic.activate': activate, 'magic.dispel': dispel, 'magic.warp': warp, 'block.metal_place': metal_place, 'item.equip_pack': equip_pack,
    'item.equip_metal': equip_metal, 'item.bottle': bottle, 'item.pick': pick, 'item.pop': pop, 'item.throw': throw, 'item.spear_hit': spear_hit,
    'block.chain': chain, 'block.stone_tap': stone_tap, 'block.grind': grind, 'block.boom': boom, 'block.metal_door_open': door_open,
    'block.metal_door_close': door_close, 'event.horn': horn, 'creature.small_ambient': small_ambient, 'creature.small_hurt': small_hurt,
    'creature.small_death': small_death, 'creature.glide_ambient': glide_call, 'creature.glide_hurt': glide_hurt, 'creature.glide_death': glide_death,
    'creature.wind_hurt': wind_hurt, 'creature.wind_death': wind_death, 'creature.titan_ambient': titan_ambient, 'creature.titan_roar': titan_roar,
    'creature.titan_hurt': titan_hurt, 'creature.titan_death': titan_death, 'creature.titan_charge': titan_charge, 'creature.titan_blast': titan_blast,
    'weather.hail': hail, 'weather.puddle_splash': splash, 'block.radar_ping': radar_ping,
    **MAT_SYNTH,
}

# ---------------------------------------------------------------------------------------------- the Labyrinth score
# "Hollow Halls": an original piece in D Dorian, 60 bpm, 24 bars. A soft felt-piano motif that keeps almost resolving,
# under a slow pad; the tense layer adds a low pulse and a held dissonance, the chase layer drums and a driving bass.
BPM = 60; BEAT = 60 / BPM; BARS = 24; LOOP = BARS * 4 * BEAT
def midi(n): return 440 * 2 ** ((n - 69) / 12)
def piano(f, sec=3.5, vel=1.0):
    t = t_(sec)
    tone = sum(np.sin(2 * np.pi * f * m * t * (1 + 0.0004 * m * m)) * g * np.exp(-t * d) for m, g, d in ((1, 1, 0.9), (2, 0.42, 1.5), (3, 0.2, 2.4), (4, 0.09, 3.4), (5, 0.05, 4.5)))
    hammer = lp(noise(0.03), 2500) * expdecay(0.03, 120) * 0.08
    out = tone * adsr(len(t), 0.004, 0.2, 0.85, 0.6) * vel; out[:len(hammer)] += hammer; return lp(out, 3200)
CHORDS = [(50, 57, 62, 65), (48, 55, 60, 64), (46, 53, 58, 62), (45, 52, 57, 60)]   # Dm9-ish, C, Bb, Am: Dorian wandering
MOTIF = [(0, 74, 1.0), (1, 72, 0.7), (1.5, 69, 0.6), (2.5, 67, 0.8), (3.5, 69, 0.5)]  # (beat, note, velocity)
def pad(notes, sec):
    t = t_(sec); x = sum(np.sin(2 * np.pi * midi(n) * t + 0.3 * np.sin(2 * np.pi * 0.2 * t + n)) for n in notes) / len(notes)
    return lp(x, 900) * adsr(len(t), 1.5, 0.5, 0.8, 1.5)
def score_calm():
    buf = np.zeros(int(LOOP * SR) + SR * 4)
    for bar in range(BARS):
        ch = CHORDS[(bar // 2) % 4]
        if bar % 2 == 0: place(buf, pad(ch, 8 * BEAT + 1.5) * 0.18, bar * 4 * BEAT)
        place(buf, piano(midi(ch[0] - 12), 5.0, 0.55), bar * 4 * BEAT)
        if bar % 4 in (0, 2):   # the motif, transposed down a step every other phrase, with a missing note now and then
            shift = 0 if (bar // 4) % 2 == 0 else -2
            for b, n, v in MOTIF:
                if (bar * 7 + b * 3) % 11 == 0: continue
                place(buf, piano(midi(n + shift), 3.0, v * 0.5), bar * 4 * BEAT + b * BEAT)
        else:                   # an answer: two high notes, far apart
            place(buf, piano(midi(ch[3] + 12), 3.0, 0.3), bar * 4 * BEAT + 1.5 * BEAT)
            place(buf, piano(midi(ch[2] + 12), 3.0, 0.25), bar * 4 * BEAT + 3 * BEAT)
    buf = reverb(buf, 3.0, 0.45, 2500)
    return loopify(buf)
def score_tense():
    buf = np.zeros(int(LOOP * SR) + SR * 4)
    for i in range(BARS * 4):   # a low heartbeat pulse on every beat
        t = t_(0.5); place(buf, np.sin(glide(70, 42, 0.5)) * np.exp(-t * 8) * 0.6, i * BEAT)
    for bar in range(0, BARS, 4):   # a held minor-second cluster that swells and fades
        place(buf, pad((62, 63, 69), 16 * BEAT) * 0.25, bar * 4 * BEAT)
    for bar in range(BARS):     # a ticking high piano note, off the beat
        place(buf, piano(midi(86), 1.5, 0.12), bar * 4 * BEAT + 2.5 * BEAT)
    return loopify(reverb(buf, 2.2, 0.35, 2000))
def score_chase():
    buf = np.zeros(int(LOOP * SR) + SR * 4)
    for i in range(BARS * 8):
        at = i * BEAT / 2
        t = t_(0.35)
        if i % 2 == 0: place(buf, np.sin(glide(120, 45, 0.35)) * np.exp(-t * 10) * 0.9, at)          # kick
        if i % 4 == 2: place(buf, (hp(noise(0.2), 1500) * 0.6) * expdecay(0.2, 20), at)               # snare
        place(buf, hp(noise(0.05), 7000) * expdecay(0.05, 70) * 0.2, at + BEAT / 4)                   # hat
        n = CHORDS[(i // 16) % 4][0] - 12 + (0, 0, 7, 0, 5, 0, 3, 2)[i % 8]
        place(buf, lp(2 * ((t_(0.25) * midi(n)) % 1) - 1, 900) * adsr(int(0.25 * SR), 0.005, 0.05, 0.7, 0.1) * 0.35, at)  # driving bass
    return loopify(reverb(buf, 1.2, 0.2, 3000))
def loopify(buf):
    """Folds the reverb tail back onto the start so the loop has no seam."""
    n = int(LOOP * SR); out = buf[:n].copy(); tail = buf[n:]; out[:len(tail)] += tail; return out

if __name__ == '__main__':
    for sid, fn in SYNTH.items():
        x = fn()
        x = x * 0.9 / (np.max(np.abs(x)) or 1)
        ogg(SND / 'sfx' / (sid.replace('.', '_') + '.ogg'), x)
    missing = [s for s in SFX if s not in SYNTH]
    if missing: sys.exit(f'no synth for {missing}')
    calm, tense, chase = score_calm(), score_tense(), score_chase()
    ogg(SND / 'music' / 'labyrinth_calm.ogg', np.stack([calm, np.roll(calm, 220)], 1), stereo=True)
    ogg(SND / 'music' / 'labyrinth_tense.ogg', np.stack([tense, np.roll(tense, 180)], 1), stereo=True)
    ogg(SND / 'music' / 'labyrinth_chase.ogg', np.stack([chase, np.roll(chase, 120)], 1), stereo=True)
    print(f'sfx51: {len(SYNTH)} effects, Labyrinth score {LOOP:.0f} s per layer')
