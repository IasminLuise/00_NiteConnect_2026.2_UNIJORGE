"""Compose wrapper: run unprivileged containers as the Linux host UID for bind mounts."""
from pathlib import Path
import os
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
COMPOSE = ['docker', 'compose', '-f', str(ROOT / 'compose.yaml')]

def compose_env():
    env = os.environ.copy()
    uid, gid = (os.getuid(), os.getgid()) if hasattr(os, 'getuid') else (10001, 10001)
    if uid == 0:
        raise RuntimeError('Execute o laboratório com usuário comum, não root.')
    env['B05_UID'], env['B05_GID'] = str(uid), str(gid)
    return env

if __name__ == '__main__':
    raise SystemExit(subprocess.run(COMPOSE + sys.argv[1:], env=compose_env()).returncode)
