"""6.0: one-off clean-up of the sounds audio5.py made (blocks, entities, ambience, voices, the Cyrrus score) without
re-running the voice synthesis: decode, add the same faint -70 dB high noise floor audio5/sfx51 now add, re-encode
at Vorbis quality 6. Removes the encoder whistles ("birdies") near 8 kHz. Safe to run again (it only re-encodes)."""
import pathlib, subprocess, tempfile, os
import numpy as np, soundfile as sf
from scipy.io import wavfile
from scipy.signal import butter, sosfilt
SND = pathlib.Path(__file__).resolve().parent.parent / 'src/main/resources/assets/stormreach/sounds'
rng = np.random.default_rng(60)
n = 0
for sub in ('block', 'entity', 'ambient', 'voice', 'music/cyrrus.ogg'):
    files = [SND / sub] if sub.endswith('.ogg') else sorted((SND / sub).rglob('*.ogg'))
    for f in files:
        x, sr = sf.read(f, always_2d=True)
        floor = sosfilt(butter(4, 6000, 'high', fs=sr, output='sos'), rng.standard_normal(x.shape[0])) * 3e-4 * (np.max(np.abs(x)) or 1)
        y = x + floor[:, None]
        y = (y / (np.max(np.abs(y)) or 1) * 0.89 * 32767).astype(np.int16)
        with tempfile.NamedTemporaryFile(suffix='.wav', delete=False) as t:
            wavfile.write(t.name, sr, y if y.shape[1] > 1 else y[:, 0])
        subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', t.name, '-ac', str(x.shape[1]), '-c:a', 'libvorbis', '-q:a', '6', str(f)], check=True)
        os.unlink(t.name); n += 1
print(f'reencode_audio: {n} files')
