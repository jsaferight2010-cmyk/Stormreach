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
    data = data / peak
    # 6.0: a faint high noise floor (about -70 dB) stops the Vorbis encoder inventing "birdies" (thin whistles near
    # 8 kHz) on low, rumbling sounds whose highs are almost silent; it is far below anything you can hear
    floor = hp(rng.standard_normal(data.shape[0]), 6000) * 3e-4
    data = data + (floor[:, None] if data.ndim == 2 else floor)
    data = (data / (np.max(np.abs(data)) or 1.0) * 0.89 * 32767).astype(np.int16)
    with tempfile.NamedTemporaryFile(suffix='.wav', delete=False) as t:
        wavfile.write(t.name, SR, data)
    subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', t.name, '-ac', '2' if stereo else '1', '-c:a', 'libvorbis', '-q:a', '6', str(path)], check=True)
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

# ---------------------------------------------------------------------------------------------- 5.2 Stormwild
def moan(f_path, sec, vib=3.0, harm=(1.0, 0.45, 0.2)):
    """A whale-like moan following a pitch path [(t, Hz), ...] with vibrato and a few harmonics."""
    t = t_(sec)
    ts, fs = zip(*f_path)
    f = np.interp(t, np.array(ts) * sec, fs) * (1 + 0.012 * np.sin(2 * np.pi * vib * t))
    ph = 2 * np.pi * np.cumsum(f) / SR
    out = sum(a * np.sin(ph * (k + 1)) for k, a in enumerate(harm))
    return out * adsr(len(t), 0.25, 0.3, 0.8, 0.6)
def whale_song():
    out = moan([(0, 150), (0.35, 230), (0.6, 205), (1.0, 140)], 3.2)
    place(out, moan([(0, 320), (0.5, 410), (1.0, 300)], 1.4, 5, (0.4, 0.15)) * 0.5, 1.4)
    return reverb(lp(out, 2200), 2.6, 0.45, 1800)
def whale_hurt(): return reverb(moan([(0, 210), (1.0, 120)], 0.9, 6), 1.4, 0.3, 1800)
def whale_death(): return reverb(moan([(0, 200), (0.4, 150), (1.0, 55)], 3.5, 2), 2.6, 0.45, 1500)
def crackle(sec, rate):
    out = np.zeros(int(sec * SR))
    for _ in range(int(sec * rate)):
        place(out, hp(noise(0.012), 2500) * expdecay(0.012, 300) * (0.3 + rng.random() * 0.7), rng.random() * sec * 0.95)
    return out
def eel_crackle(): return crackle(0.6, 40)
def eel_zap():
    t = t_(0.35)
    buzz = np.sign(np.sin(2 * np.pi * 120 * t)) * np.sin(2 * np.pi * 2400 * t + 3 * np.sin(2 * np.pi * 60 * t))
    return mix(bp(buzz, 300, 6000) * expdecay(0.35, 8) * 0.6, hp(noise(0.35), 1500) * expdecay(0.35, 18), crackle(0.35, 60))
def eel_hurt(): return mix(np.sin(glide(900, 560, 0.3)) * adsr(int(0.3 * SR), 0.005, 0.05, 0.7, 0.15) * 0.5, crackle(0.3, 50))
def eel_death(): t = t_(1.0); return mix(np.sin(glide(700, 180, 1.0)) * np.exp(-t * 2.5) * 0.5, crackle(1.0, 30) * np.linspace(1, 0, len(t)))
def skink_chirp(): return chirp(3200, 3700, 0.035, 4, 0.03, 3.0)
def jelly_chime(): return reverb(mix(bell(1568, 1.8, 0.5) * 0.5, np.concatenate([np.zeros(int(0.12 * SR)), bell(2349, 1.6, 0.4)]) * 0.3), 2.0, 0.5)
def jelly_hurt(): return reverb(mix(bell(1480, 0.6, 1.2) * 0.5, bell(1568, 0.6, 1.2) * 0.5), 1.0, 0.3)
def jelly_death():
    out = np.zeros(int(2.2 * SR))
    for k, f in enumerate((2093, 1568, 1319, 1047, 784)): place(out, bell(f, 1.2, 0.5) * (0.6 - k * 0.08), k * 0.18)
    return reverb(out, 2.0, 0.45)
def sprite_trill(): return reverb(chirp(2600, 3400, 0.03, 9, 0.02, 6.0), 0.8, 0.25)
def hermit_click():
    out = np.zeros(int(0.3 * SR))
    for k in range(3): place(out, knock(2000, 7000, 0.035, 90) * (1 - k * 0.2), k * 0.07)
    return out
def hermit_hide(): return mix(bp(noise(0.25), 400, 2000) * np.linspace(1, 0, int(0.25 * SR)) * 0.6, knock(300, 1500, 0.15, 30))
def screech(sec, f0, f1):
    t = t_(sec)
    ph = glide(f0, f1, sec, 0.8)
    tone = np.sin(ph) + 0.5 * np.sin(2 * ph + 0.3) + 0.35 * np.sin(3 * ph) + 0.25 * np.sign(np.sin(ph))
    rasp = bp(noise(sec), 1500, 6000) * (0.6 + 0.4 * np.sin(2 * np.pi * 45 * t))
    return (tone * 0.6 + rasp * 0.5) * adsr(len(t), 0.02, 0.15, 0.75, sec * 0.4)
def thunderbird_cry():
    out = mix(screech(1.3, 2300, 1500), lp(noise(1.3), 120) * expdecay(1.3, 1.2) * 0.6)
    return reverb(out, 1.8, 0.35, 4000)
def thunderbird_hurt(): return reverb(screech(0.45, 2600, 1900), 0.9, 0.25)
def thunderbird_death():
    out = mix(screech(2.2, 2100, 600), np.zeros(int(2.2 * SR)))
    place(out, thunder()[:int(2.0 * SR)] * 0.5, 0.4)
    return reverb(out, 2.4, 0.4, 3000)
def kite_flap():
    t = t_(0.3)
    return lp(noise(0.3), 1800) * np.abs(np.sin(2 * np.pi * 14 * t)) ** 3 * np.linspace(1, 0.3, len(t))
def conch():
    t = t_(2.4)
    f = 196 * (1 + 0.004 * np.sin(2 * np.pi * 4.5 * t))
    ph = 2 * np.pi * np.cumsum(f) / SR
    tone = sum(a * np.sin(ph * k) for k, a in ((1, 1.0), (2, 0.55), (3, 0.3), (4, 0.18), (5, 0.1)))
    breath = bp(noise(2.4), 300, 1800) * 0.12
    return reverb((tone * 0.5 + breath) * adsr(len(t), 0.35, 0.2, 0.85, 0.7), 2.2, 0.4, 2500)
NEW52 = {'creature.whale_song': whale_song, 'creature.whale_hurt': whale_hurt, 'creature.whale_death': whale_death,
         'creature.eel_crackle': eel_crackle, 'creature.eel_zap': eel_zap, 'creature.eel_hurt': eel_hurt, 'creature.eel_death': eel_death,
         'creature.skink_chirp': skink_chirp, 'creature.jelly_chime': jelly_chime, 'creature.jelly_hurt': jelly_hurt, 'creature.jelly_death': jelly_death,
         'creature.sprite_trill': sprite_trill, 'creature.hermit_click': hermit_click, 'creature.hermit_hide': hermit_hide,
         'creature.thunderbird_cry': thunderbird_cry, 'creature.thunderbird_hurt': thunderbird_hurt, 'creature.thunderbird_death': thunderbird_death,
         'item.kite_flap': kite_flap, 'item.conch': conch}

# ---------------------------------------------------------------------------------------------- 5.3 Skyward
def serpent_roar():
    # a long, throaty sky-dragon roar: two detuned growling voices sweeping down, a hiss on top, and thunder under it
    sec = 2.6
    out = titan(150, 70, sec, 0.8) * 0.6 + titan(152, 68, sec, 0.8) * 0.4
    out = out + hp(noise(sec), 2500) * np.sin(np.pi * t_(sec) / sec) ** 2 * 0.25
    place(out, lp(thunder()[:int(2.0 * SR)], 300) * 0.4, 0.3)
    return reverb(out, 2.2, 0.35, 2200)
def serpent_hurt(): t = t_(0.5); return mix(hp(noise(0.5), 1800) * np.exp(-t * 6) * 0.7, titan(220, 160, 0.5, 0.6) * 0.5)
def serpent_death():
    out = mix(titan(140, 40, 3.2, 0.7), np.zeros(int(3.2 * SR)))
    place(out, crackle(2.0, 25) * 0.5, 0.6)
    return reverb(out, 2.6, 0.4, 2000)
def golem_crackle():
    # ice settling: a few sharp tinkles and creaks
    out = np.zeros(int(0.9 * SR))
    for i in range(6):
        at = rng.random() * 0.75
        place(out, tone_hit(2400 + rng.random() * 2600, 0.12, 40) * 0.3, at); place(out, knock(2500, 8000, 0.05, 80) * 0.4, at)
    place(out, bp(noise(0.5), 200, 900) * np.sin(np.pi * t_(0.5) / 0.5) * 0.3, 0.1)
    return out
def golem_hurt(): return mix(shatter()[:int(0.5 * SR)] * 0.6, knock(200, 1200, 0.3, 14) * 0.7)
def golem_death():
    out = shatter() * 0.8
    out = np.concatenate([out, np.zeros(max(0, int(1.5 * SR) - len(out)))])
    place(out, debris(14, 1500, 7000, 0.9) * 0.6, 0.05)
    place(out, lp(noise(0.6), 300) * expdecay(0.6, 6) * 0.8, 0)
    return reverb(out, 1.4, 0.3)
def golem_throw(): return mix(throw() * 0.8, knock(300, 2000, 0.2, 18) * 0.6)
def moth_flutter(): t = t_(0.5); return lp(noise(0.5), 1200) * np.abs(np.sin(2 * np.pi * 22 * t)) ** 4 * np.sin(np.pi * t / 0.5) * 0.6
def stormfly_buzz():
    t = t_(1.2)
    f = 240 + 30 * np.sin(2 * np.pi * 3 * t)
    ph = 2 * np.pi * np.cumsum(f) / SR
    tone = np.sign(np.sin(ph)) * 0.25 + np.sin(ph * 2) * 0.2
    return lp(tone, 2500) * adsr(len(t), 0.1, 0.1, 0.8, 0.4) * 0.5 + crackle(1.2, 6) * 0.15
def radio_alert():
    # the weather-radio attention tone: a steady 1050 Hz beep, three times, with a little speaker grit
    out = np.zeros(int(2.4 * SR))
    for i in range(3):
        t = t_(0.55)
        beep = (np.sin(2 * np.pi * 1050 * t) + 0.12 * np.sin(2 * np.pi * 3150 * t)) * adsr(len(t), 0.005, 0.02, 0.9, 0.02)
        place(out, beep, i * 0.75)
    return bp(out, 300, 3400) + bp(noise(2.4), 300, 3400) * 0.015
NEW53 = {'creature.serpent_roar': serpent_roar, 'creature.serpent_hurt': serpent_hurt, 'creature.serpent_death': serpent_death,
         'creature.golem_crackle': golem_crackle, 'creature.golem_hurt': golem_hurt, 'creature.golem_death': golem_death,
         'creature.golem_throw': golem_throw, 'creature.moth_flutter': moth_flutter, 'creature.stormfly_buzz': stormfly_buzz,
         'item.radio_alert': radio_alert}

# ---------------------------------------------------------------------------------------------- 5.4 Thunderfront
def frog_croak():
    # a two-pulse "rrr-ribbit": a low buzzy voice gated into fast pulses, with a throaty resonance
    out = np.zeros(int(0.75 * SR))
    for at, sec, f0 in ((0.0, 0.28, 330), (0.36, 0.22, 360)):
        t = t_(sec)
        ph = 2 * np.pi * np.cumsum(f0 * (1 - 0.15 * t / sec)) / SR
        voice = np.sign(np.sin(ph)) * 0.4 + np.sin(ph * 2) * 0.3
        gate = (np.sin(2 * np.pi * 38 * t) > -0.2).astype(float)
        place(out, bp(voice * gate, 250, 1800) * adsr(len(t), 0.01, 0.05, 0.8, 0.06), at)
    return out
def petrel_call():
    # a dry, purring chitter: a burst of fast clicks over a buzzy whistle
    out = np.zeros(int(0.7 * SR))
    for k in range(9):
        place(out, tone_hit(2600 + 400 * np.sin(k), 0.03, 120) * 0.5, k * 0.045); place(out, knock(2000, 6000, 0.03, 160) * 0.4, k * 0.045)
    place(out, chirp(1900, 2300, 0.18, 1) * 0.5, 0.42)
    return out
def dust_devil_whirl():
    # a rushing hiss that sweeps up and down, with grit pattering inside it
    sec = 2.4; t = t_(sec)
    sweep = 900 + 600 * np.sin(2 * np.pi * 0.8 * t)
    hiss = np.zeros(len(t)); x = noise(sec)
    for k in range(0, len(t), 2205):
        seg = x[k:k + 2205]; f = float(sweep[min(k, len(t) - 1)])
        hiss[k:k + len(seg)] = bp(seg, max(200, f * 0.6), f * 1.6)
    grit = (rng.random(len(t)) < 0.002) * rng.standard_normal(len(t)) * 3
    return (hiss * np.sin(np.pi * t / sec) ** 0.6 + hp(grit, 3000) * 0.3) * 0.8
def wind_chime():
    # one struck brass tube: a bright inharmonic bell with a long shimmering tail
    t = t_(3.0); f = 1046.5
    tone = sum(a * np.sin(2 * np.pi * f * m * t) * np.exp(-t * d) for m, a, d in ((1, 1.0, 1.2), (2.76, 0.45, 2.2), (5.40, 0.22, 3.5), (8.93, 0.1, 5.0)))
    tone[:200] += noise(200 / SR)[:200] * 0.2
    return reverb(tone * 0.6, 1.8, 0.35)
NEW54 = {'creature.frog_croak': frog_croak, 'creature.petrel_call': petrel_call, 'creature.dust_devil_whirl': dust_devil_whirl, 'block.wind_chime': wind_chime}

# ---------------------------------------------------------------------------------------------- 5.5 Live Wire
def capacitor_charge():
    # a rising electric whine with crackle, ending in a soft clunk as the jars fill
    sec = 1.6; t = t_(sec)
    whine = np.sin(2 * np.pi * np.cumsum(220 + 900 * (t / sec) ** 1.6) / SR) * 0.4 * np.sin(np.pi * t / sec) ** 0.5
    out = whine + crackle(sec, 50) * 0.5
    place(out, knock(80, 600, 0.3, 14) * 0.8, sec - 0.3)
    return reverb(out, 0.9, 0.25)
def machine_hum():
    # mains hum: 60 Hz and its harmonics, with a little motor whirr
    t = t_(1.5)
    hum = sum(a * np.sin(2 * np.pi * f * t) for f, a in ((60, 0.6), (120, 0.35), (180, 0.15), (240, 0.08)))
    whirr = bp(noise(1.5), 600, 1400) * 0.15 * (1 + 0.5 * np.sin(2 * np.pi * 9 * t))
    return (hum + whirr) * adsr(len(t), 0.1, 0.1, 0.9, 0.3)
NEW55 = {'block.capacitor_charge': capacitor_charge, 'block.machine_hum': machine_hum}

def fire_whirl_roar():
    # a low turbulent roar (filtered noise with a slow swirl) under a bed of crackling embers
    sec = 3.5; n = int(sec * SR); t = t_(sec)
    roar = lp(noise(sec), 260) * (0.7 + 0.3 * np.sin(2 * np.pi * 0.9 * t)) * 1.4
    body = bp(noise(sec), 300, 1400) * 0.35 * (0.6 + 0.4 * np.sin(2 * np.pi * 1.7 * t + 1))
    crack = np.zeros(n)
    for _ in range(90):
        at = rng.uniform(0, sec - 0.05); c = hp(noise(0.03), 2500) * expdecay(0.03, 140) * rng.uniform(0.2, 0.7)
        place(crack, c, at)
    x = (roar + body + crack) * adsr(n, 0.3, 0.2, 0.9, 0.6)
    return reverb(x, 1.4, 0.25, 2500)
def tiv_spikes():
    # hydraulic hiss, then four heavy steel spikes biting into the ground almost together
    sec = 1.2; n = int(sec * SR); out = np.zeros(n)
    place(out, bp(noise(0.35), 1800, 6000) * adsr(int(0.35 * SR), 0.02, 0.1, 0.6, 0.15) * 0.4, 0)
    for k, at in enumerate((0.28, 0.31, 0.335, 0.36)):
        th = lp(noise(0.25), 180) * expdecay(0.25, 18) * 1.2
        ring = np.sin(2 * np.pi * (140 + 23 * k) * t_(0.25)) * expdecay(0.25, 25) * 0.5
        place(out, th + ring, at)
    return reverb(out, 0.8, 0.18, 3500)
def eel_discharge():
    # a fat electric buzz (60 Hz mains-like hum with harmonics) that swells and snaps off, over sizzling sparks and a watery fizz
    sec = 0.9; n = int(sec * SR); t = t_(sec)
    buzz = sum(np.sign(np.sin(2 * np.pi * f * t)) * a for f, a in ((60, 0.5), (120, 0.3), (180, 0.15)))
    buzz = bp(buzz * (0.6 + 0.4 * np.sin(2 * np.pi * 13 * t)), 80, 2400) * adsr(n, 0.05, 0.1, 0.9, 0.08)
    snap = np.zeros(n); place(snap, hp(noise(0.06), 1800) * expdecay(0.06, 60) * 1.2, 0.0)
    fizz = bp(noise(sec), 2500, 9000) * 0.25 * np.linspace(1, 0.2, n)
    return reverb(mix(buzz, crackle(sec, 70), snap, fizz), 0.6, 0.15, 4000)
def _moan(sec, f0, f1, vowel=(300, 870), rough=0.35):
    # a drowned voice: a breathy sawtooth glottal source, slow vibrato, pushed through two vowel formants
    t = t_(sec); f = np.linspace(f0, f1, len(t)) * (1 + 0.02 * np.sin(2 * np.pi * 5.5 * t))
    ph = np.cumsum(f) / SR; src = 2 * (ph % 1) - 1 + rough * noise(sec)
    return bp(src, vowel[0] * 0.7, vowel[0] * 1.3) * 1.2 + bp(src, vowel[1] * 0.8, vowel[1] * 1.2) * 0.6
def _bubbles(sec, count, lo=300, hi=900):
    out = np.zeros(int(sec * SR))
    for _ in range(count):
        d = rng.uniform(0.02, 0.06); f = rng.uniform(lo, hi)
        b = np.sin(2 * np.pi * np.cumsum(np.linspace(f, f * 1.8, int(d * SR))) / SR) * expdecay(d, 50)   # a rising plip
        place(out, b * rng.uniform(0.2, 0.6), rng.uniform(0, sec - d))
    return out
def brine_ambient():
    sec = 2.2; n = int(sec * SR)
    v = _moan(sec, 92, 74, (320, 800)) * adsr(n, 0.25, 0.3, 0.7, 0.6)
    gurgle = lp(noise(sec), 500) * (0.5 + 0.5 * np.sign(np.sin(2 * np.pi * 7 * t_(sec)))) * 0.4
    return reverb(mix(v, gurgle, _bubbles(sec, 26)), 1.1, 0.25, 2200)
def brine_hurt():
    sec = 0.55; n = int(sec * SR)
    v = _moan(sec, 160, 110, (380, 950), 0.6) * adsr(n, 0.005, 0.08, 0.6, 0.2)
    splash = hp(noise(0.25), 1200) * expdecay(0.25, 18) * 0.6
    out = mix(v, _bubbles(sec, 12, 500, 1400)); place(out, splash, 0)
    return reverb(out, 0.6, 0.2, 3000)
def brine_death():
    sec = 2.6; n = int(sec * SR); t = t_(sec)
    v = _moan(1.4, 120, 55, (300, 700), 0.5) * adsr(int(1.4 * SR), 0.02, 0.2, 0.7, 0.8)
    drain = lp(noise(sec), 900) * np.clip((t - 0.6) / 0.6, 0, 1) * np.exp(-np.clip(t - 1.2, 0, None) * 1.6) * 0.5   # water running off
    hiss = bp(noise(sec), 3000, 9000) * np.clip((t - 1.0), 0, 1) * np.exp(-np.clip(t - 1.6, 0, None) * 2) * 0.2      # salt fizzing
    out = mix(drain, hiss, _bubbles(sec, 40)); place(out, v, 0)
    return reverb(out, 1.4, 0.3, 2000)
def brine_step():
    sec = 0.25
    squelch = lp(noise(sec), 700) * expdecay(sec, 22) * 0.9 + bp(noise(sec), 1500, 5000) * expdecay(sec, 40) * 0.3
    return mix(squelch, _bubbles(sec, 3, 400, 900) * 0.5)
def brine_spit():
    sec = 0.9; n = int(sec * SR); t = t_(sec)
    retch = _moan(0.35, 140, 200, (420, 1100), 0.8) * adsr(int(0.35 * SR), 0.01, 0.05, 0.8, 0.1)
    spray = bp(noise(sec), 900, 7000) * np.clip(t / 0.25 - 0.9, 0, 1) * np.exp(-np.clip(t - 0.4, 0, None) * 5) * 0.9
    out = mix(spray, _bubbles(sec, 20, 600, 1600)); place(out, retch, 0)
    return reverb(out, 0.7, 0.2, 3500)
NEW60 = {'entity.fire_whirl.roar': fire_whirl_roar, 'vehicle.tiv.spikes': tiv_spikes, 'creature.eel_discharge': eel_discharge,
         'creature.brine_ambient': brine_ambient, 'creature.brine_hurt': brine_hurt, 'creature.brine_death': brine_death,
         'creature.brine_step': brine_step, 'creature.brine_spit': brine_spit}

# ================================================================================================ 6.0 sound redesign
# The 5.x recipes leaned on pure sine tones and chirps, which read as squeaky and synthetic. These replace the worst of
# them with physically-minded designs:
#   * impacts are a short noise strike exciting the material's own resonant modes (a bank of decaying resonators),
#     with a low "body" thump, so stone thuds, metal rings inharmonically, glass ticks, crystal chimes softly;
#   * plants and loose ground are clouds of tiny noise grains (rustle, crunch), never tones;
#   * small creature voices are a breathy, jittery glottal source through vocal-tract formants, pitched low enough to
#     sound like an animal rather than a whistle.
# Every block-material sound gets three variants (see VARIANTS below) so footsteps and breaking never loop audibly.
from scipy.signal import lfilter
def resonator(x, f, decay):
    """A two-pole resonance at f Hz that rings for about 1/decay seconds."""
    r = np.exp(-decay / SR); w = 2 * np.pi * f / SR
    return lfilter([1 - r], [1, -2 * r * np.cos(w), r * r], x)
def strike(sec=0.01, bright=4000):
    """The excitation of a hit: a few milliseconds of shaped noise (brighter = harder)."""
    n = int(sec * SR); return lp(rng.standard_normal(n) * np.linspace(1, 0, n) ** 2, bright)
def modal(modes, sec, bright=4000, jitter=0.03):
    """A struck object: modes are (Hz, gain, decay per second). Each hit detunes its modes slightly."""
    x = np.zeros(int(sec * SR)); e = strike(0.008, bright); x[:len(e)] = e
    out = np.zeros_like(x)
    for f, g, d in modes: out += resonator(x, f * (1 + rng.uniform(-jitter, jitter)), d) * g
    return out / (np.max(np.abs(out)) or 1)
def grains(n, sec, lo, hi, grain=0.012, decay=None, env=None):
    """A cloud of n tiny noise grains between lo and hi Hz spread over sec seconds (rustle, crunch, patter)."""
    out = np.zeros(int((sec + 0.05) * SR))
    for _ in range(n):
        g = int(grain * rng.uniform(0.5, 1.6) * SR)
        bit = bp(rng.standard_normal(g), lo * rng.uniform(0.8, 1.2), hi * rng.uniform(0.85, 1.1), 2) * np.hanning(g)
        at = rng.random() ** (decay or 1.0) * sec
        place(out, bit * rng.uniform(0.2, 1.0), at)
    if env is not None: out[:len(env)] *= env[:len(out)]
    return out
def thump(f=90, sec=0.12, k=30): t = t_(sec); return np.sin(2 * np.pi * f * t * (1 - 0.3 * t / sec)) * np.exp(-t * k)
def voice(sec, f0, f1, formants, breath=0.25, jitter=0.02, buzz=0.7):
    """A creature voice: a glottal pulse train (with pitch jitter) plus breath, shaped by vocal-tract formants."""
    t = t_(sec); f = np.linspace(f0, f1, len(t)) * (1 + jitter * lp(rng.standard_normal(len(t)), 30) * 8)
    ph = np.cumsum(f) / SR
    pulse = np.maximum(0, np.sin(2 * np.pi * ph)) ** 6 * buzz          # narrow glottal pulses, rich in harmonics
    src = pulse + breath * rng.standard_normal(len(t)) * 0.3
    out = sum(resonator(src, F, 80 + F * 0.06) * g for F, g in formants)
    return hp(out, 80) / (np.max(np.abs(out)) or 1)

# ---- block materials (break, step, place)
STONE = ((180, 1.0, 60), (430, 0.7, 70), (790, 0.4, 90), (1350, 0.25, 120))
METAL = ((320, 1.0, 5), (745, 0.7, 7), (1290, 0.5, 9), (1980, 0.3, 12), (2710, 0.2, 16))
GLASS = ((1150, 0.8, 28), (2480, 0.5, 35), (3900, 0.3, 45))
CRYSTAL = ((880, 0.8, 9), (1320, 0.55, 11), (2110, 0.35, 14))
def m_stone_break(): return mix(modal(STONE, 0.4, 3000) * 0.8, thump(85, 0.2, 18) * 0.9, grains(28, 0.22, 400, 3500, decay=1.6) * 0.6)
def m_stone_step(): return mix(modal(STONE, 0.12, 2200) * 0.5, thump(110, 0.08, 45) * 0.6, grains(4, 0.04, 600, 3000) * 0.3)
def m_stone_place(): return mix(modal(STONE, 0.3, 2500) * 0.7, thump(75, 0.18, 22))
def m_metal_break(): return mix(modal(METAL, 0.9, 5000) * 0.8, grains(10, 0.2, 1500, 6000, decay=1.5) * 0.25, thump(120, 0.1, 30) * 0.4)
def m_metal_step(): return mix(modal(tuple((f, g, d * 5) for f, g, d in METAL[:3]), 0.12, 2200) * 0.35, thump(140, 0.06, 50) * 0.6)   # a footfall on plate: a dull clank
def m_metal_place(): return mix(modal(METAL, 0.8, 3500) * 0.8, thump(90, 0.15, 25) * 0.5)
def m_glass_break():
    out = mix(modal(GLASS, 0.3, 7000) * 0.5, grains(46, 0.35, 2000, 7500, grain=0.006, decay=2.0) * 0.8)
    for _ in range(6): place(out, modal(((rng.uniform(2200, 4200), 1, 60),), 0.12, 8000) * 0.25, rng.uniform(0.02, 0.3))
    return out
def m_glass_step(): return mix(modal(GLASS, 0.08, 5000) * 0.35, thump(160, 0.05, 60) * 0.4)
def m_glass_place(): return mix(modal(tuple((f, g, d * 2) for f, g, d in GLASS), 0.15, 4000) * 0.45, thump(120, 0.08, 40) * 0.5)
def m_crystal_break(): return mix(modal(CRYSTAL, 1.2, 6000) * 0.6, grains(30, 0.3, 1800, 6500, grain=0.008, decay=1.8) * 0.6, thump(100, 0.12, 30) * 0.3)
def m_crystal_step(): return mix(modal(tuple((f, g, d * 4) for f, g, d in CRYSTAL), 0.15, 2000) * 0.25, thump(130, 0.06, 50) * 0.5)
def m_crystal_place(): return mix(modal(CRYSTAL, 1.0, 4000) * 0.55, thump(90, 0.12, 28) * 0.5)
def m_soft_break(): return mix(thump(70, 0.25, 15), grains(22, 0.2, 150, 1800, grain=0.02, decay=1.4) * 0.7)
def m_soft_step(): return mix(thump(90, 0.1, 35) * 0.7, grains(6, 0.06, 200, 1500, grain=0.02) * 0.5)
def m_soft_place(): return mix(thump(75, 0.18, 22), grains(8, 0.1, 150, 1200, grain=0.02) * 0.4)
def m_plant_break(): return mix(grains(70, 0.4, 900, 6000, grain=0.01, decay=1.3) * 0.8, grains(8, 0.3, 300, 1500, grain=0.02) * 0.4)   # leaves and twigs
def m_plant_step(): return grains(16, 0.12, 900, 5000, grain=0.01) * 0.6
def m_plant_place(): return mix(grains(22, 0.18, 800, 4500, grain=0.012) * 0.6, thump(110, 0.08, 40) * 0.3)
def m_loose_break(): return mix(grains(90, 0.35, 250, 3200, grain=0.008, decay=1.5) * 0.9, thump(70, 0.2, 18) * 0.6)    # gravel and sand
def m_loose_step(): return mix(grains(30, 0.12, 300, 3000, grain=0.008) * 0.8, thump(90, 0.06, 45) * 0.4)
def m_loose_place(): return mix(grains(45, 0.2, 200, 2500, grain=0.01, decay=1.4) * 0.7, thump(70, 0.15, 25) * 0.5)
MAT = {m: (globals()[f'm_{m}_break'], globals()[f'm_{m}_step'], globals()[f'm_{m}_place']) for m in ('stone', 'crystal', 'metal', 'soft', 'plant', 'loose', 'glass')}
MAT_SYNTH = {f'block.mat.{m}.{ev}': fns[i] for m, fns in MAT.items() for i, ev in enumerate(('break', 'step', 'place'))}
def stone_tap(): return mix(modal(((900, 1, 120), (2100, 0.5, 160)), 0.1, 6000) * 0.6, grains(3, 0.03, 1500, 5000) * 0.4)   # hail on stone
def hail():
    out = np.zeros(int(1.5 * SR))
    for k in range(55): place(out, stone_tap() * (0.15 + rng.random() * 0.6), rng.random() * 1.4)
    return mix(out, grains(120, 1.4, 600, 4000, grain=0.004) * 0.25)
def equip_metal(): out = modal(METAL, 0.5, 5000) * 0.5; place(out, grains(12, 0.15, 800, 4000, grain=0.01) * 0.6, 0.0); return out   # mail settling

# ---- small creatures: animal voices instead of whistles
def small_ambient():   # a soft chirruping trill, low and breathy
    out = np.zeros(int(0.6 * SR))
    for k in range(3): place(out, voice(0.08, 900, 1150, ((1200, 1), (2600, 0.4)), 0.35) * 0.7, k * 0.13)
    return out
def small_hurt(): return voice(0.22, 1100, 700, ((1100, 1), (2400, 0.5)), 0.5, 0.05)
def small_death(): return mix(voice(0.55, 950, 380, ((900, 1), (2100, 0.45)), 0.45, 0.04), grains(10, 0.3, 300, 2000) * 0.2)
def skink_chirp(): return mix(voice(0.05, 1500, 1700, ((1600, 1), (3000, 0.3)), 0.3) * 0.6, grains(6, 0.05, 2000, 5000, grain=0.004) * 0.4)
def sprite_trill():   # a fluttering, airy trill (it is made of wind and light, not a bird whistle)
    out = np.zeros(int(1.2 * SR))
    for k in range(8): place(out, voice(0.05, 1300 + 60 * k, 1500 + 60 * k, ((1500, 1), (3200, 0.3)), 0.6) * 0.5, k * 0.07)
    return reverb(mix(out, grains(40, 0.9, 1500, 6000, grain=0.01) * 0.15), 0.9, 0.3, 4000)
def petrel_call():   # a dry, purring chatter: pulses of a throaty voice
    out = np.zeros(int(0.75 * SR))
    for k in range(9): place(out, voice(0.04, 780, 820, ((850, 1), (1900, 0.5)), 0.4) * (0.5 + 0.3 * np.sin(k)), k * 0.05)
    place(out, voice(0.2, 700, 950, ((900, 1), (2100, 0.45)), 0.35) * 0.7, 0.48)
    return out
def thunderbird_hurt(): return reverb(mix(voice(0.5, 420, 300, ((700, 1), (1500, 0.6), (2600, 0.3)), 0.6, 0.06), grains(30, 0.3, 1500, 6000) * 0.25), 0.9, 0.25, 3000)
def hermit_click():
    out = np.zeros(int(0.35 * SR))
    for k in range(4): place(out, modal(((1400, 1, 220), (2600, 0.4, 300)), 0.04, 5000) * 0.6, k * 0.06 + rng.uniform(0, 0.015))
    return out

# ---- 6.0 second pass: ice, eels, machines
ICE = ((640, 1.0, 45), (1490, 0.6, 60), (2380, 0.35, 80), (3310, 0.2, 110))
def golem_crackle():   # ice settling: creaks (slow grain clouds) and the odd sharp tick
    out = grains(18, 0.8, 300, 1800, grain=0.03) * 0.5
    for _ in range(3): place(out, modal(ICE, 0.25, 5000) * rng.uniform(0.2, 0.45), rng.uniform(0.05, 0.6))
    return out
def golem_hurt(): return mix(modal(ICE, 0.5, 6000) * 0.6, grains(40, 0.3, 1200, 6000, grain=0.006, decay=1.8) * 0.6, thump(80, 0.25, 15) * 0.8)
def golem_death():
    out = mix(grains(120, 1.0, 900, 7000, grain=0.007, decay=1.6) * 0.8, thump(60, 0.6, 6) * 1.0, np.zeros(int(1.6 * SR)))
    for _ in range(10): place(out, modal(ICE, 0.3, 7000) * rng.uniform(0.2, 0.5), rng.uniform(0, 0.9))
    for _ in range(6): place(out, mix(thump(rng.uniform(70, 120), 0.2, 20), grains(10, 0.1, 400, 3000)) * 0.4, rng.uniform(0.3, 1.2))   # chunks landing
    return reverb(out, 1.4, 0.3, 3000)
def golem_throw(): return mix(bp(noise(0.5), 200, 1500) * np.sin(np.pi * t_(0.5) / 0.5) ** 2 * 0.8, grains(14, 0.2, 400, 2500, grain=0.02) * 0.5, thump(90, 0.15, 25) * 0.6)
def eel_hurt(): return mix(voice(0.3, 380, 260, ((500, 1), (1300, 0.5)), 0.5, 0.06) * 0.6, crackle(0.3, 50) * 0.6, grains(10, 0.2, 400, 2500, grain=0.01) * 0.3)
def eel_death():
    t = t_(1.2)
    return mix(voice(1.0, 320, 120, ((450, 1), (1100, 0.4)), 0.6, 0.05) * 0.6, crackle(1.2, 30) * np.linspace(1, 0, len(t)) * 0.7,
               grains(30, 1.0, 300, 2000, grain=0.02) * 0.3)
def capacitor_charge():
    # a rising electric buzz (mains-like harmonics climbing in pitch), crackle, then the jars clunk full
    sec = 1.6; t = t_(sec); f = 90 + 260 * (t / sec) ** 1.5
    ph = np.cumsum(f) / SR; saw = 2 * (ph % 1) - 1
    buzz = bp(saw, 150, 3200) * 0.5 * np.sin(np.pi * t / sec) ** 0.5
    out = mix(buzz, crackle(sec, 50) * 0.5)
    place(out, mix(modal(((180, 1, 30), (410, 0.5, 40)), 0.3, 2000), thump(70, 0.2, 18)) * 0.8, sec - 0.3)
    return reverb(out, 0.9, 0.25, 3000)
def machine_hum():
    # a working machine: mains hum through a transformer resonance, a cooling fan, and the tick of a bearing
    t = t_(1.5)
    hum = sum(a * np.sin(2 * np.pi * f * t) for f, a in ((60, 0.5), (120, 0.4), (180, 0.2), (240, 0.12), (300, 0.06)))
    hum = hum * (1 + 0.04 * np.sin(2 * np.pi * 0.7 * t))
    fan = bp(noise(1.5), 250, 1800) * 0.18 * (1 + 0.25 * np.sin(2 * np.pi * 23 * t))
    ticks = np.zeros(len(t))
    for k in range(9): place(ticks, modal(((2100, 1, 250),), 0.02, 6000) * 0.05, k / 6 + rng.uniform(0, 0.01))
    return (hum + fan + ticks) * adsr(len(t), 0.1, 0.1, 0.9, 0.3)

# the per-release tables captured the old functions when they were built: point them at the redesigned ones
_REDONE = {'small_ambient', 'small_hurt', 'small_death', 'skink_chirp', 'sprite_trill', 'petrel_call', 'thunderbird_hurt',
           'hermit_click', 'stone_tap', 'hail', 'equip_metal', 'golem_crackle', 'golem_hurt', 'golem_death', 'golem_throw',
           'eel_hurt', 'eel_death', 'capacitor_charge', 'machine_hum'}
for _d in (NEW52, NEW53, NEW54, NEW55):
    for _k, _f in list(_d.items()):
        if getattr(_f, '__name__', '') in _REDONE: _d[_k] = globals()[_f.__name__]

# ---- 6.0: the two biggest sounds, rebuilt (they were made by audio5.py; now they come from here)
def _seamless(x, fade):
    """Folds the end of x onto its start over `fade` seconds so it loops without a seam."""
    n = int(fade * SR); body = x[:-n].copy(); w = np.linspace(0, 1, n)
    body[:n] = body[:n] * w + x[-n:] * (1 - w); return body
def tornado_roar():
    sec = 6.5; t = t_(sec)
    rumble = lp(noise(sec), 110) * (0.75 + 0.25 * np.sin(2 * np.pi * 0.31 * t)) * 2.2                         # the ground-shaking low end
    centre = 260 + 120 * np.sin(2 * np.pi * 0.17 * t) + 60 * np.sin(2 * np.pi * 0.43 * t + 1)
    train = np.zeros(len(t))
    for lo in (0.7, 1.0, 1.4):                                                                                 # the "freight train": a churning mid roar
        train += bp(noise(sec), 90 * lo, 1100 * lo)
    train *= (0.65 + 0.35 * np.abs(np.sin(2 * np.pi * 6.5 * t + 2 * np.sin(2 * np.pi * 0.6 * t))))
    train = lp(train, 900) * (0.6 + 0.4 * (centre / 380))
    hiss = bp(noise(sec), 1200, 4200) * 0.18 * (0.6 + 0.4 * np.sin(2 * np.pi * 0.23 * t))                   # air tearing through debris
    debris = np.zeros(len(t))
    for _ in range(26): place(debris, modal(STONE if rng.random() < 0.5 else ((150, 1, 40), (330, 0.6, 50)), 0.18, 1800) * rng.uniform(0.1, 0.35), rng.uniform(0, sec - 0.2))
    out = mix(rumble, train, hiss, debris)
    return _seamless(reverb(out, 1.6, 0.25, 900)[:len(t)], 1.0)
def tornado_siren():
    """A mechanical rotor siren: winds up, holds with a wobble, winds down. One 6 s cycle."""
    sec = 6.0; t = t_(sec)
    up, hold = 2.2, 1.3
    f = np.where(t < up, 160 + 580 * (1 - (1 - t / up) ** 2.2),
         np.where(t < up + hold, 740 + 10 * np.sin(2 * np.pi * 3.1 * t), 150 + 590 * (1 - np.clip((t - up - hold) / (sec - up - hold), 0, 1)) ** 1.8))
    ph = 2 * np.pi * np.cumsum(f) / SR
    rotor = np.tanh(3 * np.sin(ph)) + 0.35 * np.tanh(3 * np.sin(2 * ph + 0.4)) + 0.2 * np.sin(3 * ph)          # chopped air: square-ish, odd-rich
    horn = bp(rotor, 300, 2600) + 0.4 * bp(rotor, 2600, 5200)                                                  # the horn's throat
    motor = lp(noise(sec), 70) * 0.5 + np.sin(2 * np.pi * 48 * t) * 0.15                                       # the motor under it
    level = np.clip(t / 0.3, 0, 1) * np.clip((sec - t) / 0.6, 0, 1)
    dry = (horn + motor * 0.6) * level
    echo = np.zeros(len(dry)); place(echo, dry * 0.28, 0.42)                                                   # slapback off distant buildings
    return reverb(mix(dry, echo)[:len(t)], 2.2, 0.3, 3500)

# sounds that get three variants (files _2 and _3 next to the first)
VARIANTS = set(MAT_SYNTH) | {'block.stone_tap', 'creature.small_ambient', 'creature.skink_chirp', 'creature.petrel_call'}

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
    **MAT_SYNTH, **NEW52, **NEW53, **NEW54, **NEW55, **NEW60,
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
        for v in range(3 if sid in VARIANTS else 1):   # 6.0: variants, so repeated sounds don't sound identical
            x = fn()
            x = x * 0.9 / (np.max(np.abs(x)) or 1)
            ogg(SND / 'sfx' / (sid.replace('.', '_') + ('' if v == 0 else f'_{v + 1}') + '.ogg'), x)
    ogg(SND / 'entity' / 'tornado_roar.ogg', tornado_roar() * 0.9)
    ogg(SND / 'block' / 'tornado_siren.ogg', tornado_siren() * 0.9)
    missing = [s for s in SFX if s not in SYNTH]
    if missing: sys.exit(f'no synth for {missing}')
    calm, tense, chase = score_calm(), score_tense(), score_chase()
    ogg(SND / 'music' / 'labyrinth_calm.ogg', np.stack([calm, np.roll(calm, 220)], 1), stereo=True)
    ogg(SND / 'music' / 'labyrinth_tense.ogg', np.stack([tense, np.roll(tense, 180)], 1), stereo=True)
    ogg(SND / 'music' / 'labyrinth_chase.ogg', np.stack([chase, np.roll(chase, 120)], 1), stereo=True)
    print(f'sfx51: {len(SYNTH)} effects, Labyrinth score {LOOP:.0f} s per layer')
