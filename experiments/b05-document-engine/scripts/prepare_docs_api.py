"""Prepare fresh isolated synthetic runtime; secrets remain private. Never clears caches."""
import argparse
import hashlib
import json
import os
import secrets
import shutil
from pathlib import Path
from fidelity import ROOT, private_path


def prepare(target):
    target = private_path(ROOT / '.runtime', target)
    target.mkdir(parents=True, exist_ok=False)
    for name in ('fixtures', 'wopi-data', 'docs-data', 'engine-cache', 'engine-logs', 'engine-keys', 'browser'):
        (target / name).mkdir()
    shutil.copyfile(ROOT / 'fixtures/generated/F01.docx', target / 'fixtures/F01.docx')
    # Local comparison preserves the prior exact bytes; clean CI uses the existing source-free case.
    previous = ROOT / '.runtime/b0502/fresh-identity/fixtures/F02.docx'
    provenance = 'b0502-exact-copy'
    if previous.is_file():
        shutil.copyfile(previous, target / 'fixtures/F02.docx')
    else:
        from adapter_cases import savedate_source
        from docx_adapter import normalize_copy, digest
        source = savedate_source()
        adapted, _ = normalize_copy(source, {'original_sha256': digest(source),
            'template_version': 'source-free-ci-v1', 'fields': [
                {'part': 'word/header1.xml', 'instruction': 'SAVEDATE \\* MERGEFORMAT'}]})
        (target / 'fixtures/F02.docx').write_bytes(adapted)
        provenance = 'source-free-ci-v1'
    secret = secrets.token_urlsafe(48)
    operator = secrets.token_urlsafe(32)
    (target / 'jwt-secret').write_text(secret, encoding='ascii')
    (target / 'operator').write_text(operator, encoding='ascii')
    (target / 'save-script.properties').write_text('F01.docx=XYZ|\nF02.docx=XYZ|\n', encoding='ascii')
    uid, gid = (os.getuid(), os.getgid()) if hasattr(os, 'getuid') else (10001, 10001)
    overlay = {'services': {
        'wopi-host': {'user': f'{uid}:{gid}', 'environment': {
            'LAB_DATA': '/wopi-data', 'LAB_FIXTURES': '/fixtures', 'LAB_SAVE_SCRIPT': '/private/save-script.properties',
            'B05_DOCS_DATA': '/docs-data', 'B05_JWT_SECRET_FILE': '/private/jwt-secret', 'B05_OPERATOR_FILE': '/private/operator',
            'B05_ENGINE_PIN': 'http://172.29.53.10', 'B05_EDITOR_ORIGIN': 'http://localhost:19982'},
            'volumes': [str(target / 'wopi-data') + ':/wopi-data', str(target / 'docs-data') + ':/docs-data',
                        str(target / 'fixtures') + ':/fixtures:ro', str(target) + ':/private:ro']},
        'onlyoffice': {'environment': {'JWT_SECRET': secret}, 'volumes': [str(target / 'engine-cache') + ':/var/lib/onlyoffice',
            str(target / 'engine-logs') + ':/var/log/onlyoffice', str(target / 'engine-keys') + ':/var/www/onlyoffice/Data']}}}
    path = target / 'compose.private.json'
    path.write_text(json.dumps(overlay, indent=2), encoding='utf-8')
    for name in ('jwt-secret', 'operator', 'compose.private.json'):
        (target / name).chmod(0o600)
    manifest = {'fixtures': {p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in (target / 'fixtures').glob('*.docx')},
                'entry_url': 'http://localhost:18087/docs-api/lab?access=' + operator, 'scope': 'synthetic-only',
                'complex_fixture_provenance': provenance}
    (target / 'private-manifest.json').write_text(json.dumps(manifest, indent=2), encoding='utf-8')
    (target / 'private-manifest.json').chmod(0o600)
    print('Fresh Docs API runtime prepared; credentials and entry URL retained privately')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--out', type=Path, required=True)
    prepare(parser.parse_args().out)
