"""Private, explicit checkpoints for Word/WOPI fidelity experiments (not a product API)."""
import argparse
import hashlib
import io
import json
import subprocess
import time
import urllib.request
import zipfile
import xml.etree.ElementTree as ET
import uuid
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
W = '{http://schemas.openxmlformats.org/wordprocessingml/2006/main}'
NS = {'w': W[1:-1]}


def private_path(runtime, target):
    runtime, target = Path(runtime).resolve(), Path(target).resolve()
    if target == runtime or not target.is_relative_to(runtime):
        raise ValueError('Output must be a child of this lab .runtime directory')
    return target


def inventory(data):
    """Preserve whitespace, paragraph/cell boundaries; collect all fields and image anchors."""
    result = {'sha256': hashlib.sha256(data).hexdigest(), 'body': [], 'tables': [],
              'fields': [], 'headers': [], 'images': [], 'anchors': [], 'links': []}
    with zipfile.ZipFile(io.BytesIO(data)) as archive:
        if len(archive.infolist()) > 512 or sum(i.file_size for i in archive.infolist()) > 64 * 1024 * 1024:
            raise ValueError('Package too large for the experiment')
        for item in archive.infolist():
            name = item.filename
            if item.file_size > 16 * 1024 * 1024:
                raise ValueError('Part too large')
            blob = archive.read(name)
            if name.startswith('word/media/'):
                result['images'].append({'part': name, 'sha256': hashlib.sha256(blob).hexdigest()})
            if not name.endswith(('.xml', '.rels')):
                continue
            if b'<!DOCTYPE' in blob.upper() or b'<!ENTITY' in blob.upper():
                raise ValueError('DTD and entities prohibited')
            root = ET.fromstring(blob)
            paragraphs = [''.join((n.text or '') if n.tag == W + 't' else '\t' if n.tag == W + 'tab' else '\n'
                                  for n in p.iter() if n.tag in (W + 't', W + 'tab', W + 'br'))
                          for p in root.iter(W + 'p')]
            if name == 'word/document.xml':
                result['body'] = paragraphs
                result['tables'] = [[['\n'.join(''.join(n.text or '' for n in p.iter(W + 't'))
                                                        for p in cell.findall('w:p', NS))
                                      for cell in row.findall('w:tc', NS)]
                                     for row in table.findall('w:tr', NS)] for table in root.iter(W + 'tbl')]
            if name.startswith(('word/header', 'word/footer')):
                result['headers'].append({'part': name, 'paragraphs': paragraphs})
            fields = []
            for element in root.iter():
                if element.tag == W + 'fldChar':
                    kind = element.get(W + 'fldCharType')
                    if kind == 'begin':
                        fields.append([])
                    elif kind == 'end' and fields:
                        result['fields'].append({'part': name, 'instruction': ''.join(fields.pop())})
                elif element.tag == W + 'instrText' and fields:
                    fields[-1].append(element.text or '')
                elif element.get(W + 'instr'):
                    result['fields'].append({'part': name, 'instruction': element.get(W + 'instr')})
                if element.tag.endswith(('}anchor', '}inline', '}shape', '}pict')):
                    result['anchors'].append({'part': name, 'xml': ET.tostring(element, encoding='unicode')})
                if element.get('TargetMode', '').lower() == 'external':
                    result['links'].append({'part': name, 'type': element.get('Type'), 'target': element.get('Target')})
    return result


def validate_capture(previous, current, data):
    if previous == current:
        raise ValueError('No new persisted version; an editor save indicator is insufficient')
    return inventory(data)


def public_summary(result):
    allowed = ('engine', 'cycles', 'status', 'pages', 'body_preserved', 'fields_valid', 'logo_visible')
    return {key: result[key] for key in allowed if key in result}


def request(base, path, post=False):
    return urllib.request.urlopen(urllib.request.Request(base + path, data=b'' if post else None,
                                   headers={'Origin': base}), timeout=60)


def capture(base, document, out, previous=None, pdf=True, version=None):
    out = private_path(ROOT / '.runtime', out)
    # A failed attempt may leave an empty/incomplete directory; never reuse it.
    out.mkdir(parents=True, exist_ok=False)
    if version is None:
        with request(base, '/lab/documents/' + document + '/current') as response:
            version = json.load(response)['version']
    with request(base, f'/lab/documents/{document}/versions/{version}') as response:
        data = response.read()
    result = validate_capture(previous, version, data) if previous else inventory(data)
    result['version'] = version
    converted = None
    if pdf:
        with request(base, f'/lab/documents/{document}/versions/{version}/pdf', True) as response:
            if response.headers.get('X-Source-Version') != version:
                raise ValueError('PDF does not identify the exact source version')
            converted = response.read()
        if not converted.startswith(b'%PDF-'):
            raise ValueError('Invalid PDF')
    (out / 'source.docx').write_bytes(data)
    if converted is not None:
        (out / 'engine.pdf').write_bytes(converted)
    # Completion marker is written last, after all required artifacts exist.
    (out / 'inventory.json').write_text(json.dumps(result, indent=2, ensure_ascii=False), encoding='utf-8')
    return {'version': version, 'sha256': result['sha256']}


def pdf_inventory(path):
    from pypdf import PdfReader
    reader = PdfReader(path)
    fonts = set()
    texts = []
    for page in reader.pages:
        texts.append(page.extract_text() or '')
        for value in page.get('/Resources', {}).get('/Font', {}).values():
            fonts.add(str(value.get_object().get('/BaseFont', '')))
    return {'pages': len(reader.pages), 'fonts': sorted(fonts), 'page_texts': texts}


def render(path, out):
    import pypdfium2 as pdfium
    from PIL import Image, ImageDraw
    out = private_path(ROOT / '.runtime', out)
    out.mkdir(parents=True, exist_ok=True)
    document = pdfium.PdfDocument(str(path))
    sheet = Image.new('RGB', (1000, 700 * ((len(document) + 1) // 2)), '#cccccc')
    draw = ImageDraw.Draw(sheet)
    for number in range(len(document)):
        page = document[number]
        bitmap = page.render(scale=1.6)
        picture = bitmap.to_pil().convert('RGB')
        picture.save(out / f'page-{number + 1:03}.png')
        picture.thumbnail((490, 660))
        x, y = (number % 2) * 500, (number // 2) * 700
        sheet.paste(picture, (x, y + 25))
        draw.text((x + 10, y + 5), str(number + 1), fill='black')
        bitmap.close()
        page.close()
    sheet.save(out / 'contact.png')
    document.close()


def environment(containers):
    results = []
    for name in containers:
        data = json.loads(subprocess.check_output(['docker', 'inspect', name], text=True))[0]
        # Do not record environment variables, commands or logs: may contain credentials.
        allowed_env = ('LANG=', 'LC_ALL=', 'LAB_ENGINE=')
        results.append({'container': name, 'image_id': data['Image'], 'image': data['Config']['Image'],
                        'locale': [e for e in data['Config']['Env'] if e.startswith(allowed_env)],
                        'mounts': [{'source': m['Source'], 'target': m['Destination'], 'read_only': not m['RW']}
                                   for m in data['Mounts']]})
    return results


def convert_onlyoffice(host, document, version, out):
    """Use internal conversion API; source is the immutable version, never current."""
    out = private_path(ROOT / '.runtime', out)
    out.mkdir(parents=True, exist_ok=True)
    if (out / 'engine.pdf').exists() or (out / 'conversion.json').exists():
        raise FileExistsError('Conversion checkpoint already exists')
    path = f'/lab/documents/{document}/versions/{version}'
    with request(host, path) as response:
        source_hash = hashlib.sha256(response.read()).hexdigest()
    if (out / 'source.docx').exists() and hashlib.sha256((out / 'source.docx').read_bytes()).hexdigest() != source_hash:
        raise ValueError('Captured source differs from the requested immutable version')
    payload = json.dumps({'async': False, 'filetype': 'docx', 'outputtype': 'pdf',
                          'key': str(uuid.uuid4()), 'title': 'source.docx',
                          'url': 'http://wopi-host:8080' + path}).encode()
    result = subprocess.run(['docker', 'exec', '-i', 'nite-b05-onlyoffice-onlyoffice-1', 'curl', '-fsS',
                             'http://localhost/converter', '-H', 'Content-Type: application/json',
                             '-H', 'Accept: application/json', '--data-binary', '@-'], input=payload, capture_output=True, timeout=90)
    if result.returncode:
        raise ValueError('ONLYOFFICE conversion request failed')
    converted = json.loads(result.stdout)
    if converted.get('error') or not converted.get('endConvert'):
        raise ValueError('ONLYOFFICE conversion error: ' + str(converted.get('error')))
    from urllib.parse import urlsplit
    uri = urlsplit(converted['fileUrl'])
    if uri.scheme != 'http' or uri.hostname not in ('localhost', '127.0.0.1'):
        raise ValueError('Unexpected internal conversion download destination')
    downloaded = subprocess.run(['docker', 'exec', 'nite-b05-onlyoffice-onlyoffice-1', 'curl', '-fsS', converted['fileUrl']],
                                capture_output=True, timeout=60)
    if downloaded.returncode or not downloaded.stdout.startswith(b'%PDF-'):
        raise ValueError('ONLYOFFICE PDF download failed')
    (out / 'engine.pdf').write_bytes(downloaded.stdout)
    (out / 'conversion.json').write_text(json.dumps({'source_version': version, 'source_sha256': source_hash}), encoding='utf-8')
    return {'version': version, 'source_sha256': source_hash}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    subs = parser.add_subparsers(dest='command', required=True)
    p = subs.add_parser('capture')
    p.add_argument('--host', default='http://localhost:18085')
    p.add_argument('--document', required=True)
    p.add_argument('--out', type=Path, required=True)
    p.add_argument('--previous')
    p.add_argument('--version', help='Exact observed immutable version; never follow a later current pointer')
    p.add_argument('--no-pdf', action='store_true')
    p = subs.add_parser('render')
    p.add_argument('--pdf', type=Path, required=True)
    p.add_argument('--out', type=Path, required=True)
    p = subs.add_parser('environment')
    p.add_argument('--containers', nargs='+', required=True)
    p.add_argument('--out', type=Path, required=True)
    args = parser.parse_args()
    if args.command == 'capture':
        print(json.dumps(capture(args.host, args.document, args.out, args.previous, not args.no_pdf, args.version)))
    elif args.command == 'render':
        render(args.pdf, args.out)
    else:
        out = private_path(ROOT / '.runtime', args.out)
        out.write_text(json.dumps(environment(args.containers), indent=2), encoding='utf-8')
        print('Private environment inventory recorded')


if __name__ == '__main__':
    main()
