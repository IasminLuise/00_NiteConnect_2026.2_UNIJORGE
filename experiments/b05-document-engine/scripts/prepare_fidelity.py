"""Prepare fresh private scenario copies. Sources are supplied through a private manifest."""
import argparse
import hashlib
import json
import uuid
from pathlib import Path

from fidelity import ROOT, inventory, private_path


def prepare(manifest, out):
    out = private_path(ROOT / '.runtime', out)
    out.mkdir(parents=True, exist_ok=False)
    for folder in ('fixtures', 'data', 'outputs', 'word'):
        (out / folder).mkdir()
    sources = json.loads(Path(manifest).read_text(encoding='utf-8-sig'))
    if len(sources) != 2:
        raise ValueError('Exactly two authorized DOCX sources required')
    scenarios = []
    for number in range(1, 7):
        source = Path(sources[(number - 1) % 2]['source'])
        data = source.read_bytes()
        name = f'F{number:02}.docx'
        details = inventory(data)
        (out / 'fixtures' / name).write_bytes(data)
        # Java UUID.nameUUIDFromBytes uses MD5 with RFC 4122 version/variant bits.
        identifier = str(uuid.UUID(bytes=hashlib.md5(name.encode()).digest(), version=3))
        scenarios.append({'alias': name, 'document': identifier, 'source_path': str(source.resolve()),
                          'source_sha256': details['sha256'], 'scenario': ('original', 'noop', 'fill')[(number - 1) // 2]})
    (out / 'scenarios.json').write_text(json.dumps(scenarios, indent=2), encoding='utf-8')
    overlay = {'services': {'wopi-host': {'volumes': [str(out / 'data') + ':/data', str(out / 'fixtures') + ':/fixtures:ro']}}}
    (out / 'compose.override.json').write_text(json.dumps(overlay, indent=2), encoding='utf-8')
    print('Prepared six fresh private scenario copies; no source modified')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--manifest', type=Path, required=True)
    parser.add_argument('--out', type=Path, required=True)
    args = parser.parse_args()
    prepare(args.manifest, args.out)
