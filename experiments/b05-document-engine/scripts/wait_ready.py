"""Bounded readiness check used locally and in CI."""
import time
from urllib.request import urlopen

def wait_ready():
    deadline = time.monotonic() + 50
    pending = {'http://localhost:18086/lab/health', 'http://localhost:19981/hosting/discovery'}
    while pending and time.monotonic() < deadline:
        for url in list(pending):
            try:
                with urlopen(url, timeout=2) as response:
                    if response.status == 200:
                        pending.remove(url)
            except OSError:
                pass
        if pending:
            time.sleep(1)
    if pending:
        raise SystemExit('Serviços não ficaram prontos em 50s: ' + ', '.join(sorted(pending)))
    print('Health e discovery disponíveis.')

if __name__ == '__main__':
    wait_ready()
