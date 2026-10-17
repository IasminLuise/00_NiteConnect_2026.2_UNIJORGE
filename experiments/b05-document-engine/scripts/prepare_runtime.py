"""Derive a restrictive local configuration from the exact pinned CODE image."""
from pathlib import Path
import subprocess
import uuid
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
CODE = 'collabora/code:26.04.5.1.1@sha256:56541aad09403975646dfb87a3716e38afecce09b5f853391eb6d937a0ccd402'

def prepare():
    runtime = ROOT / '.runtime'
    runtime.mkdir(exist_ok=True)
    (runtime / 'data').mkdir(exist_ok=True)
    (ROOT / 'results' / 'generated').mkdir(parents=True, exist_ok=True)
    name = 'b05-config-' + uuid.uuid4().hex[:12]
    subprocess.run(['docker', 'create', '--name', name, CODE], check=True, stdout=subprocess.DEVNULL)
    try:
        subprocess.run(['docker', 'cp', name + ':/etc/coolwsd/coolwsd.xml', str(runtime / 'coolwsd.xml')], check=True, stdout=subprocess.DEVNULL)
    finally:
        subprocess.run(['docker', 'rm', name], check=True, stdout=subprocess.DEVNULL)
    tree = ET.parse(runtime / 'coolwsd.xml')
    root = tree.getroot()
    def value(path, text):
        node = root.find(path)
        if node is None:
            raise RuntimeError('Pinned image configuration changed: ' + path)
        node.text = text
    value('ssl/enable', 'false')
    value('ssl/termination', 'false')
    value('server_name', 'localhost:19980')
    value('admin_console/enable', 'false')
    value('logging/level', 'error')
    value('security/capabilities', 'true')
    value('net/content_security_policy', "frame-ancestors http://localhost:18085;")
    allowed = root.find('net/post_allow')
    allowed.clear()
    allowed.set('allow', 'true')
    ET.SubElement(allowed, 'host').text = '172.30.50.2/32'
    root.find('net/lok_allow').clear()
    groups = root.find('storage/wopi/alias_groups')
    groups.clear()
    groups.set('mode', 'groups')
    group = ET.SubElement(groups, 'group')
    ET.SubElement(group, 'host', {'allow': 'true'}).text = 'http://wopi-host:8080'
    tree.write(runtime / 'coolwsd.xml', encoding='utf-8', xml_declaration=True)
    print('Configuração local derivada do digest fixado; conversão permitida somente ao host WOPI.')

if __name__ == '__main__':
    prepare()
