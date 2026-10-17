"""Allow ONLY the comparison WOPI host; no credentials/config contents are printed."""
import json
import subprocess


def initialize():
    host = json.loads(subprocess.check_output(['docker', 'inspect', 'nite-b05-onlyoffice-wopi-host-1'], text=True))[0]
    networks = host['NetworkSettings']['Networks']
    if len(networks) != 1:
        raise RuntimeError('Expected one dedicated comparison network')
    address = next(iter(networks.values()))['IPAddress']
    import ipaddress
    if not ipaddress.ip_address(address).is_private:
        raise RuntimeError('Expected private comparison host')
    settings = {'rules': [{'address': address, 'allowed': True}, {'address': '*', 'allowed': False}],
                'useforrequest': False, 'errorcode': 403}
    script = "import json;from pathlib import Path;p=Path('/etc/onlyoffice/documentserver/local.json');c=json.loads(p.read_text());c['services']['CoAuthoring']['ipfilter']=json.loads(" + repr(json.dumps(settings)) + ");p.write_text(json.dumps(c))"
    subprocess.run(['docker', 'exec', 'nite-b05-onlyoffice-onlyoffice-1', 'python3', '-c', script], check=True, capture_output=True)
    subprocess.run(['docker', 'exec', 'nite-b05-onlyoffice-onlyoffice-1', 'supervisorctl', 'restart', 'ds:docservice'], check=True, capture_output=True)
    print('ONLYOFFICE WOPI destination restricted to the comparison host')


if __name__ == '__main__':
    initialize()
