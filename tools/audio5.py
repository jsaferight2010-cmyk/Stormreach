"""Stormreach 5.0 audio: Cyrrus's voice-over, the adaptive Labyrinth music, the boss theme and the sound effects.

Voice: Piper TTS (voice "Ryan high", dataset licence CC BY-NC-SA 4.0) + an ffmpeg "corruption" chain whose strength
rises line by line, so Cyrrus sounds more like the storm the more of himself he gives up. Everything else is
synthesised here with numpy, so there are no samples from anywhere else.

Needs: python3 -m piper (pip install piper-tts), ffmpeg with libvorbis, numpy, scipy.
Writes src/main/resources/assets/stormreach/sounds/**.ogg and tools/voice_lines.json (measured line lengths, which
gendata8.py turns into the cinematic timing so subtitles, actions and audio stay in sync).
Run it on its own: python tools/audio5.py [--voice path/to/model.onnx]. run_all.py keeps the sounds folder.
"""
import json, math, os, pathlib, subprocess, sys, tempfile
import numpy as np
from scipy.io import wavfile
from scipy.signal import butter, sosfilt

ROOT = pathlib.Path(__file__).resolve().parent.parent
SND = ROOT / 'src/main/resources/assets/stormreach/sounds'
VOICE = os.environ.get('STORMREACH_VOICE', '/home/claude/voices/voice-en-us-ryan-high/en-us-ryan-high.onnx')
if '--voice' in sys.argv: VOICE = sys.argv[sys.argv.index('--voice') + 1]
SR = 44100
rng = np.random.default_rng(5005)

# id -> (text, corruption 0..1). Ids are the sound-event paths under stormreach:voice.cyrrus.*
LINES = {
    'l01': ('So. You followed the signs.', 0.10),
    'l02': ('The footsteps behind you. The shape between the trees, gone the moment you looked.', 0.15),
    'l03': ('That was me. It has always been me.', 0.25),
    'l04': ('My name was Cyrrus. I was a traveller once, like you. I climbed to the Sovereign\'s crown, and asked the storm to answer me.', 0.35),
    'l05': ('It answered. It filled me, until there was nothing left but wind.', 0.5),
    'l06': ('You carried my heart out of the dark. You set my eyes in stone. You read my journal.', 0.6),
    'l07': ('Thank you. You have woken me completely.', 0.8),
    'l08': ('Now. Let the sky decide which of us walks home.', 1.0),
    'phase2': ('The rain will take your eyes. You won\'t need them.', 1.0),
    'phase3': ('You hold your ground. The wind will fix that.', 1.0),
    'phase4': ('Come, children of the squall. Feed on them.', 1.0),
    'phase5': ('No more voices. Only the storm.', 1.0),
    'd01': ('The rain is stopping.', 0.5),
    'd02': ('Strange. I can hear my own footsteps again.', 0.25),
    'd03': ('Keep the crown, traveller. Don\'t ask it for anything.', 0.05),
}


def ogg(path, data, sr=SR, stereo=False):
    path = pathlib.Path(path); path.parent.mkdir(parents=True, exist_ok=True)
    data = np.asarray(data, dtype=np.float64)
    peak = np.max(np.abs(data)) or 1.0
    data = data / peak
    # 6.0: a faint high noise floor (about -70 dB) so the Vorbis encoder doesn't add whistles to rumbling sounds
    from scipy.signal import butter, sosfilt
    floor = sosfilt(butter(4, 6000, 'high', fs=sr, output='sos'), np.random.default_rng(7).standard_normal(data.shape[0])) * 3e-4
    data = data + (floor[:, None] if data.ndim == 2 else floor)
    data = (data / (np.max(np.abs(data)) or 1.0) * 0.89 * 32767).astype(np.int16)
    with tempfile.NamedTemporaryFile(suffix='.wav', delete=False) as t:
        wavfile.write(t.name, sr, data)
    subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', t.name, '-ac', '2' if stereo else '1', '-c:a', 'libvorbis', '-q:a', '6', str(path)], check=True)
    os.unlink(t.name)


def lp(x, hz, order=4): return sosfilt(butter(order, hz, 'low', fs=SR, output='sos'), x)
def hp(x, hz, order=4): return sosfilt(butter(order, hz, 'high', fs=SR, output='sos'), x)
def bp(x, lo, hi, order=3): return sosfilt(butter(order, [lo, hi], 'band', fs=SR, output='sos'), x)
def t_(sec): return np.arange(int(sec * SR)) / SR
def env(n, a=0.01, r=0.2):
    e = np.ones(n); ai, ri = max(1, int(a * SR)), max(1, int(r * SR))
    e[:ai] = np.linspace(0, 1, ai); e[-ri:] *= np.linspace(1, 0, ri); return e
def note(f, sec, kind='sine', a=0.01, r=0.3):
    t = t_(sec)
    if kind == 'saw': w = 2 * (t * f % 1) - 1
    elif kind == 'square': w = np.sign(np.sin(2 * np.pi * f * t))
    elif kind == 'bell': w = sum(np.sin(2 * np.pi * f * m * t) * g * np.exp(-t * d) for m, g, d in ((1, 1, 3), (2.76, 0.5, 5), (5.4, 0.25, 8), (8.9, 0.12, 12)))
    else: w = np.sin(2 * np.pi * f * t)
    return w * env(len(t), a, r)
def place(buf, x, at):
    i = int(at * SR); j = min(len(buf), i + len(x))
    if i < len(buf): buf[i:j] += x[:j - i]
def kick(sec=0.4, f0=110, f1=40):
    t = t_(sec); f = f1 + (f0 - f1) * np.exp(-t * 25); ph = 2 * np.pi * np.cumsum(f) / SR
    return np.sin(ph) * np.exp(-t * 9)
def snare(sec=0.25):
    t = t_(sec); return (hp(rng.standard_normal(len(t)), 1500) * 0.6 + np.sin(2 * np.pi * 190 * t) * 0.5) * np.exp(-t * 18)
def hat(sec=0.06):
    t = t_(sec); return hp(rng.standard_normal(len(t)), 7000) * np.exp(-t * 60)
def midi(n): return 440 * 2 ** ((n - 69) / 12)


# -------------------------------------------------------------------------------- voice
def voice_line(key, text, c):
    out = SND / 'voice' / 'cyrrus' / f'{key}.ogg'
    with tempfile.TemporaryDirectory() as d:
        raw = f'{d}/raw.wav'
        subprocess.run([sys.executable, '-m', 'piper', '-m', VOICE, '--length-scale', '1.18', '--sentence-silence', '0.35', '-f', raw],
                       input=text.encode(), check=True, capture_output=True)
        if os.environ.get('STORMREACH_KEEP_RAW'):  # for the intelligibility check
            os.makedirs(os.environ['STORMREACH_KEEP_RAW'], exist_ok=True); subprocess.run(['cp', raw, f"{os.environ['STORMREACH_KEEP_RAW']}/{key}.wav"])
        f1 = 2 ** (-(2 + 3 * c) / 12)          # 2 to 5 semitones down
        f2 = f1 / 2                             # sub-octave "storm" layer
        g2 = 0.12 + 0.43 * c
        echo = f'aecho=0.8:{0.5 + 0.2 * c:.2f}:{int(60 + 50 * c)}|{int(140 + 120 * c)}:{0.2 + 0.12 * c:.2f}|{0.1 + 0.1 * c:.2f}'
        graph = (f'[0]aresample={SR},asplit=3[a][b][w];'
                 f'[a]asetrate={SR}*{f1:.4f},aresample={SR},atempo={1 / f1:.4f}[m];'
                 f'[b]asetrate={SR}*{f2:.4f},aresample={SR},atempo=2.0,atempo={1 / f2 / 2:.4f},volume={g2:.2f}[s];'
                 f'[w]highpass=f=3000,volume={0.35 * c:.2f}[h];'
                 f'[m][s][h]amix=inputs=3:normalize=0,asoftclip=type=tanh:param={1 + 2 * c:.2f},'
                 f'lowpass=f={10000 - 3000 * c:.0f},{echo},apad=pad_dur=0.6,alimiter=limit=0.9[o]')
        proc = f'{d}/proc.wav'
        subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', raw, '-filter_complex', graph, '-map', '[o]', '-ac', '1', proc], check=True)
        sr, data = wavfile.read(proc)
        data = data.astype(np.float64)
        if data.ndim > 1: data = data.mean(axis=1)
        # corruption: a crackle of static whose density follows the voice's own loudness
        envl = lp(np.abs(data) / (np.max(np.abs(data)) or 1), 30, 2)
        crackle = (rng.random(len(data)) < 0.001 + 0.004 * c) * rng.standard_normal(len(data)) * envl * 0.4 * c * np.max(np.abs(data))
        data = data + crackle
        ogg(out, data, sr)
        return len(data) / sr


# -------------------------------------------------------------------------------- music: the Labyrinth (three synced layers)
BPM = 90; BEAT = 60 / BPM; BARS = 12; LOOP = BARS * 4 * BEAT   # 32 s, identical for all three layers
KEY = [midi(n) for n in (45, 48, 50, 51, 52, 55, 57)]           # A minor with a flat-5 for unease
def drone():
    t = t_(LOOP)
    d = sum(np.sin(2 * np.pi * f * t + np.sin(2 * np.pi * 0.07 * t) * 2) * g for f, g in ((55, 0.5), (55 * 1.5, 0.18), (110 * 1.007, 0.15), (77.78, 0.08)))
    return lp(d, 600) * (0.8 + 0.2 * np.sin(2 * np.pi * t / LOOP))
def layer_calm():
    buf = drone() * 0.7
    r = np.random.default_rng(1)
    for bar in range(BARS):
        for k in range(2):
            if r.random() < 0.75:
                f = KEY[r.integers(len(KEY))] * 4
                place(buf, note(f, 2.6, 'bell', 0.002, 1.2) * 0.22, bar * 4 * BEAT + (k * 2 + r.random() * 0.5) * BEAT)
    for bar in range(0, BARS, 3):  # far-off metallic groans
        g = note(70 + r.random() * 20, 4.0, 'saw', 1.0, 2.0)
        place(buf, bp(g, 200, 900) * 0.12, bar * 4 * BEAT + 1.5)
    return buf
def layer_tense():
    buf = drone() * 0.5
    for i in range(BARS * 8):  # eighth-note ostinato, filtered saw
        f = KEY[(0, 0, 3, 0, 4, 0, 3, 1)[i % 8]] * 2
        place(buf, lp(note(f, BEAT * 0.45, 'saw', 0.005, 0.08), 1400) * 0.22, i * BEAT / 2)
    for i in range(BARS * 4):  # heartbeat kick on every beat
        place(buf, kick(0.3, 90, 45) * 0.55, i * BEAT)
        place(buf, kick(0.25, 80, 45) * 0.3, i * BEAT + 0.22)
    for bar in range(0, BARS, 2):  # string swell
        sw = sum(note(f * 2, 4 * BEAT * 2, 'saw', 2.5, 2.0) for f in (KEY[0], KEY[3], KEY[4]))
        place(buf, lp(sw, 1800) * 0.08, bar * 4 * BEAT)
    return buf
def layer_chase():
    buf = np.zeros(int(LOOP * SR))
    for i in range(BARS * 16):  # sixteenths
        at = i * BEAT / 4
        if i % 4 == 0: place(buf, kick() * 0.9, at)
        if i % 8 == 4: place(buf, snare() * 0.6, at)
        if i % 2: place(buf, hat() * 0.25, at)
    riff = (0, 0, 3, 0, 6, 5, 3, 1)
    for i in range(BARS * 8):
        f = KEY[riff[i % 8]] * (2 if (i // 16) % 2 == 0 else 2.0 * 1.0595 ** 1)
        place(buf, np.tanh(lp(note(f, BEAT * 0.4, 'square', 0.002, 0.05), 2200) * 2) * 0.22, i * BEAT / 2)
    for bar in range(BARS):  # brass-like stab on the downbeat
        st = sum(note(f, BEAT * 0.8, 'saw', 0.01, 0.3) for f in (KEY[0] * 2, KEY[3] * 2, KEY[5] * 2))
        place(buf, lp(st, 2500) * 0.12, bar * 4 * BEAT)
    return buf


# -------------------------------------------------------------------------------- music: Cyrrus's theme
def cyrrus_theme():
    bpm = 120; beat = 60 / bpm; bars = 32; length = bars * 4 * beat  # 64 s
    buf = np.zeros(int(length * SR))
    prog = [(45, 48, 52), (41, 45, 48), (43, 47, 50), (44, 47, 52)]   # Am F G E(ish)
    for bar in range(bars):
        chord = prog[(bar // 2) % 4]
        pad = sum(note(midi(n + 12), 4 * beat, 'saw', 0.3, 0.6) * (1 + 0.003 * math.sin(n)) for n in chord)
        t = t_(4 * beat); pad *= 1 + 0.15 * np.sin(2 * np.pi * 5 * t)  # choir-ish vibrato
        place(buf, lp(pad, 1500) * 0.10, bar * 4 * beat)
        place(buf, lp(note(midi(chord[0] - 12), 4 * beat, 'saw', 0.01, 0.2), 300) * 0.35, bar * 4 * beat)
        for k in range(8):
            at = bar * 4 * beat + k * beat / 2
            if k in (0, 3, 4, 6) or (bar % 4 == 3 and k == 7): place(buf, kick(0.45, 120, 38), at)
            if k in (2, 6): place(buf, snare() * 0.7, at)
            place(buf, hat() * 0.2, at)
        if bar >= 8:  # melody enters
            mel = (69, 72, 76, 74, 72, 71, 69, 64)
            for k, n in enumerate(mel[:4] if bar % 2 == 0 else mel[4:]):
                place(buf, np.tanh(note(midi(n + 12), beat * 0.9, 'saw', 0.01, 0.2) * 1.5) * 0.08, bar * 4 * beat + k * beat)
        if bar % 8 == 7:  # thunder roll
            th = lp(np.cumsum(rng.standard_normal(int(3 * SR))) * 0.002, 200) * env(int(3 * SR), 0.05, 2.5)
            place(buf, th * 3, bar * 4 * beat + 1)
    return buf


# -------------------------------------------------------------------------------- effects
def siren():
    t = t_(8.0); f = 450 + 350 * (0.5 - 0.5 * np.cos(2 * np.pi * t / 8))
    ph = 2 * np.pi * np.cumsum(f) / SR
    s = np.sin(ph) + 0.4 * np.sin(2 * ph) + 0.15 * np.sin(3 * ph)
    return lp(s, 3000) * env(len(t), 0.6, 1.2)
def roar():
    n = int(6 * SR); b = np.cumsum(rng.standard_normal(n)); b -= np.convolve(b, np.ones(4000) / 4000, 'same')
    t = t_(6); mod = 0.7 + 0.3 * np.sin(2 * np.pi * 0.4 * t) * np.sin(2 * np.pi * 0.13 * t)
    r = lp(b, 350) * mod + hp(lp(rng.standard_normal(n), 2500), 600) * 0.15 * mod
    m = int(0.5 * SR); r[:m] = r[:m] * np.linspace(0, 1, m) + r[-m:] * np.linspace(1, 0, m); return r[:-m]
def heartbeat():
    buf = np.zeros(int(1.0 * SR)); place(buf, lp(kick(0.25, 70, 35), 200), 0.0); place(buf, lp(kick(0.25, 60, 32), 200) * 0.7, 0.28); return buf
def whisper():
    """Noise-vocoded speech: Piper speaks, then the voice is replaced by breath shaped in 16 bands."""
    with tempfile.TemporaryDirectory() as d:
        subprocess.run([sys.executable, '-m', 'piper', '-m', VOICE, '--length-scale', '1.3', '-f', f'{d}/w.wav'],
                       input=b'Behind you.', check=True, capture_output=True)
        subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', f'{d}/w.wav', '-ar', str(SR), '-ac', '1', f'{d}/w2.wav'], check=True)
        _, v = wavfile.read(f'{d}/w2.wav')
    v = v.astype(np.float64) / 32768; out = np.zeros(len(v)); noise_ = rng.standard_normal(len(v))
    edges = np.geomspace(200, 9000, 17)
    for lo, hi in zip(edges[:-1], edges[1:]):
        e = lp(np.abs(bp(v, lo, hi)), 40, 2); out += bp(noise_, lo, hi) * e
    return hp(out, 400)
def screech():
    t = t_(1.6); f = 600 + 900 * np.exp(-t * 2) + 40 * np.sin(2 * np.pi * 9 * t)
    ph = 2 * np.pi * np.cumsum(f) / SR
    s = np.sin(ph + 3 * np.sin(ph * 1.41)) + 0.5 * np.sin(ph * 2.73) + hp(rng.standard_normal(len(t)), 3000) * 0.2
    return np.tanh(s * 2) * env(len(t), 0.02, 0.8)


if __name__ == '__main__':
    lengths = {}
    for k, (text, c) in LINES.items():
        lengths[k] = round(voice_line(k, text, c), 3); print('voice', k, lengths[k], 's')
    json.dump({'voice': 'Piper en-us ryan-high (CC BY-NC-SA 4.0)', 'lines': {k: {'text': v[0], 'seconds': lengths[k]} for k, v in LINES.items()}},
              open(ROOT / 'tools/voice_lines.json', 'w'), indent=2)
    ogg(SND / 'music/labyrinth_calm.ogg', layer_calm(), stereo=True)
    ogg(SND / 'music/labyrinth_tense.ogg', layer_tense(), stereo=True)
    ogg(SND / 'music/labyrinth_chase.ogg', layer_chase(), stereo=True)
    ogg(SND / 'music/cyrrus.ogg', cyrrus_theme(), stereo=True)
    ogg(SND / 'block/tornado_siren.ogg', siren())
    ogg(SND / 'entity/tornado_roar.ogg', roar())
    ogg(SND / 'ambient/heartbeat.ogg', heartbeat())
    ogg(SND / 'entity/stranger_whisper.ogg', whisper())
    ogg(SND / 'entity/hollow_keeper_screech.ogg', screech())
    print('audio5 done')
