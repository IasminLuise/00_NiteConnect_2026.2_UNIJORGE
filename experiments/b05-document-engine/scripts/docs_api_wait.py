"""Wait for this isolated host and ONLYOFFICE; never starts other engines."""
import time
import urllib.request


def main():
    for _ in range(120):
        try:
            for url in ('http://localhost:18087/lab/health', 'http://localhost:19982/healthcheck'):
                with urllib.request.urlopen(url, timeout=2) as response:
                    if response.status != 200:
                        raise ValueError('Not ready')
            print('Isolated Docs API host and ONLYOFFICE ready')
            return
        except (OSError, ValueError):
            time.sleep(2)
    raise RuntimeError('Docs API laboratory readiness timeout')


if __name__ == '__main__':
    main()
