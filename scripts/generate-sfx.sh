#!/usr/bin/env bash
set -euo pipefail
DIR="$(dirname "$0")/../app/src/main/res/raw"
mkdir -p "$DIR"
python - "$DIR" <<'PY'
import math, wave, struct, sys, os
outdir = sys.argv[1]
fr = 44100
amp = 16000

def tone(name, freq, duration=0.15):
    n = int(fr*duration)
    with wave.open(os.path.join(outdir, name), 'w') as f:
        f.setnchannels(1)
        f.setsampwidth(2)
        f.setframerate(fr)
        for i in range(n):
            val = int(amp*math.sin(2*math.pi*freq*i/fr))
            f.writeframes(struct.pack('<h', val))

tone('correct.wav', 880)
tone('skip.wav', 440)
PY

echo "Generated correct.wav and skip.wav in $DIR"
