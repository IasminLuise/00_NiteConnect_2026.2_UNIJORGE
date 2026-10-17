"""Convert the exact persisted versions in both engines; never print session tokens."""
from pathlib import Path
from urllib.request import Request, urlopen
from urllib.error import HTTPError
import argparse
import hashlib
import json
import subprocess
import time
import uuid
from lab import compose_env

ROOT = Path(__file__).resolve().parents[1]
COMPOSE = ['docker', 'compose', '-f', str(ROOT / 'compose.yaml')]
HOST = 'http://localhost:18085'

def request(path, post=False):
    return urlopen(Request(HOST + path, data=b'' if post else None, headers={'Origin': HOST}), timeout=55)

def convert_libreoffice(source, name, version, generated, inputs, runner=subprocess.run):
    # Every invocation gets a new output path, even for retries of the same version.
    comparator = inputs / ('compare-' + version + '-' + uuid.uuid4().hex + '-' + name)
    comparator.write_bytes(source)
    start = time.monotonic()
    try:
        result = runner(COMPOSE + ['run', '--rm', '--no-deps', 'libreoffice', '/input/' + comparator.name], capture_output=True, text=True, timeout=60, env=compose_env())
        output = generated / 'libreoffice' / comparator.name.replace('.docx', '.pdf')
        if result.returncode != 0 or not output.exists() or not output.read_bytes().startswith(b'%PDF-'):
            return {'status': 'failed', 'exit_code': result.returncode}
        return {'status': 'converted', 'seconds': round(time.monotonic() - start, 3), 'bytes': output.stat().st_size, 'sha256': hashlib.sha256(output.read_bytes()).hexdigest(), 'file': 'libreoffice/' + output.name}
    except subprocess.TimeoutExpired:
        return {'status': 'timeout'}

def measure(selected):
    run_id = uuid.uuid4().hex
    generated = ROOT / 'results' / 'generated'
    (generated / 'collabora').mkdir(parents=True, exist_ok=True)
    (generated / 'libreoffice').mkdir(exist_ok=True)
    with request('/lab/documents') as response:
        documents = json.load(response)
    results = []
    def checkpoint():
        report = json.dumps(results, indent=2) + '\n'
        (generated / ('conversions-' + run_id + '.json')).write_text(report, encoding='utf-8')
        (generated / 'conversions.json').write_text(report, encoding='utf-8')
    for document in documents:
        name = document['name']
        if selected and name.removesuffix('.docx') not in selected:
            continue
        # Fresh lab starts with one version; choose the pointer, not a lexical UUID ordering.
        with request('/lab/documents/' + document['id'] + '/current') as response:
            current = json.load(response)
        version = current['version']
        with request(f"/lab/documents/{document['id']}/versions/{version}") as response:
            source = response.read()
        row = {'fixture': name, 'source_version': version, 'source_sha256': hashlib.sha256(source).hexdigest()}
        start = time.monotonic()
        try:
            with request(f"/lab/documents/{document['id']}/versions/{version}/pdf", post=True) as response:
                pdf = response.read()
                assert response.headers['X-Source-Version'] == version
            assert pdf.startswith(b'%PDF-')
            output = generated / 'collabora' / (version + '-' + run_id + '-' + name.replace('.docx', '.pdf'))
            output.write_bytes(pdf)
            row['collabora'] = {'status': 'converted', 'seconds': round(time.monotonic() - start, 3), 'bytes': len(pdf), 'sha256': hashlib.sha256(pdf).hexdigest(), 'file': 'collabora/' + output.name}
        except HTTPError as error:
            row['collabora'] = {'status': 'failed', 'http_status': error.code, 'seconds': round(time.monotonic() - start, 3)}
        row['libreoffice'] = convert_libreoffice(source, name, version, generated, ROOT / 'fixtures' / 'generated')
        results.append(row)
        checkpoint()
        print(name, 'Collabora:', row['collabora']['status'], 'LibreOffice:', row['libreoffice']['status'])
    stats = subprocess.run(COMPOSE + ['stats', '--no-stream', '--format', 'json'], capture_output=True, text=True, check=True, env=compose_env()).stdout
    (generated / 'docker-stats.jsonl').write_text(stats, encoding='utf-8')
    if any(r[engine]['status'] != 'converted' for r in results for engine in ('collabora', 'libreoffice')):
        raise SystemExit('Há conversões com falha; consultar results/generated/conversions.json')

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--fixtures', nargs='*')
    measure(parser.parse_args().fixtures)
