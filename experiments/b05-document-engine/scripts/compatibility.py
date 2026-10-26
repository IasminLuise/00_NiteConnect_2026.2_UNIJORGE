"""Read-only private OOXML diagnostics for B05.0.1; never repairs saved documents."""
import argparse
import hashlib
import io
import json
import re
import zipfile
import xml.etree.ElementTree as ET
from pathlib import Path

from fidelity import ROOT, W, private_path


def normalize_instruction(value):
    # Preserve whitespace inside pictures and quoted bookmark names.
    tokens = re.findall(r'"[^"]*"|[^\s"]+', value)
    if tokens:
        tokens[0] = tokens[0].upper()
    return ' '.join(tokens)


def canonical(element):
    return [element.tag, sorted(element.attrib.items()), element.text or '',
            [canonical(child) for child in element]]


def text(element):
    return ''.join(n.text or '' if n.tag == W + 't' else '\t' if n.tag == W + 'tab' else '\n'
                   for n in element.iter() if n.tag in (W + 't', W + 'tab', W + 'br'))


def fields(root, part):
    result, stack = [], []

    def start(representation, instruction='', locked=None):
        locked = False if locked in (None, '0', 'false', 'off') else True if locked in ('1', 'true', 'on') else locked
        field = {'part': part, 'representation': representation, 'instruction': instruction,
                 'cached': '', 'locked': locked, 'complete': representation == 'simple'}
        result.append(field)
        stack.append({'field': field, 'phase': 'result' if representation == 'simple' else 'instruction'})

    def visit(node):
        if node.tag == W + 'fldSimple':
            start('simple', node.get(W + 'instr', ''), node.get(W + 'fldLock'))
            for child in node:
                visit(child)
            stack.pop()
            return
        if node.tag == W + 'fldChar':
            kind = node.get(W + 'fldCharType')
            if kind == 'begin':
                start('complex', locked=node.get(W + 'fldLock'))
            elif kind == 'separate' and stack:
                stack[-1]['phase'] = 'result'
            elif kind == 'end' and stack:
                stack.pop()['field']['complete'] = True
        elif node.tag == W + 'instrText' and stack and stack[-1]['phase'] == 'instruction':
            stack[-1]['field']['instruction'] += node.text or ''
        elif node.tag in (W + 't', W + 'tab', W + 'br'):
            for entry in stack:
                if entry['phase'] == 'result':
                    entry['field']['cached'] += text(node)
        for child in node:
            visit(child)

    visit(root)
    for field in result:
        field['normalized'] = normalize_instruction(field['instruction'])
        field['type'] = field['normalized'].split(' ', 1)[0]
    return result


def prose(root):
    """Paragraph text excluding all field instructions/results, with state across paragraphs."""
    result, depth = [], 0
    for paragraph in root.iter(W + 'p'):
        fragments = []

        def visit(node):
            nonlocal depth
            if node.tag == W + 'fldSimple':
                return
            if node.tag == W + 'fldChar':
                kind = node.get(W + 'fldCharType')
                if kind == 'begin':
                    depth += 1
                elif kind == 'end':
                    depth = max(0, depth - 1)
            elif depth == 0 and node.tag in (W + 't', W + 'tab', W + 'br'):
                fragments.append(text(node))
            for child in node:
                visit(child)

        visit(paragraph)
        result.append(''.join(fragments))
    return result


def inventory(data):
    result = {'sha256': hashlib.sha256(data).hexdigest(), 'body': [], 'stories': {}, 'prose': {},
              'fields': [], 'properties': {}, 'relationships': [], 'images': []}
    with zipfile.ZipFile(io.BytesIO(data)) as z:
        entries = z.infolist()
        names = [e.filename for e in entries]
        if len(entries) > 512 or len(names) != len(set(names)) or sum(e.file_size for e in entries) > 64 * 1024 * 1024:
            raise ValueError('Ambiguous or oversized OOXML package')
        for entry in entries:
            if entry.file_size > 16 * 1024 * 1024:
                raise ValueError('Oversized OOXML part')
            blob = z.read(entry)
            name = entry.filename
            if name.startswith('word/media/'):
                result['images'].append({'part': name, 'sha256': hashlib.sha256(blob).hexdigest()})
            if not name.endswith(('.xml', '.rels')):
                continue
            checked = blob.replace(b'\0', b'').upper()
            if b'<!DOCTYPE' in checked or b'<!ENTITY' in checked:
                raise ValueError('DTD/entities prohibited, including UTF-16/32')
            root = ET.fromstring(blob)
            if name.endswith('.rels'):
                for rel in root:
                    result['relationships'].append({'part': name, 'id': rel.get('Id'),
                        'type': rel.get('Type'), 'target': rel.get('Target'), 'mode': rel.get('TargetMode', 'Internal')})
            if name.startswith('word/') and not name.endswith('.rels'):
                result['fields'].extend(fields(root, name))
                paragraphs = [text(p) for p in root.iter(W + 'p')]
                if paragraphs:
                    result['stories'][name] = paragraphs
                    result['prose'][name] = prose(root)
                if name == 'word/document.xml':
                    result['body'] = paragraphs
                if name in ('word/settings.xml', 'word/styles.xml', 'word/fontTable.xml'):
                    result['properties'][name] = canonical(root)
                else:
                    tags = {W + tag for tag in ('pPr', 'rPr', 'sectPr', 'tblPr', 'tblGrid', 'trPr', 'tcPr')}
                    result['properties'][name] = [canonical(n) for n in root.iter() if n.tag in tags]
    return result


def compare(before, after, expected_body=None, layout_regression=False):
    def change(a, b, changed='unverified'):
        return {'classification': 'unchanged' if a == b else changed}

    a, b = before['fields'], after['fields']
    identity = lambda fs: [(f['part'], f['type'], f['locked']) for f in fs]
    semantics = lambda fs: [(f['part'], f['normalized'], f['cached'], f['locked']) for f in fs]
    field_status = change(semantics(a), semantics(b))
    if identity(a) != identity(b):
        # Renumbered/extra story parts require section/relation inspection; do
        # not call an unused duplicated header a proven functional regression.
        matched_parts = [f['part'] for f in a] == [f['part'] for f in b]
        field_status['classification'] = 'semantic-regression' if matched_parts else 'unverified'
    elif any(not f['complete'] for f in a + b):
        field_status['classification'] = 'unverified'
    body_status = change(before['body'], after['body'],
                         'unverified' if before['prose'].get('word/document.xml') == after['prose'].get('word/document.xml') else 'semantic-regression')
    if before['body'] != after['body'] and expected_body is not None and after['body'] == expected_body:
        body_status['classification'] = 'expected-edit'
    # IDs may be renumbered; count/targets/types remain observable. Never fetch targets.
    relations = lambda inv: sorted((r['part'], r['type'] or '', r['target'] or '', r['mode']) for r in inv['relationships'])
    other_stories = lambda inv: {p: v for p, v in inv['prose'].items() if p != 'word/document.xml'}
    return {'fields': field_status, 'body': body_status,
            'stories': change(other_stories(before), other_stories(after), 'semantic-regression'),
            'properties': change(before['properties'], after['properties'], 'layout-regression' if layout_regression else 'unverified'),
            'relationships': change(relations(before), relations(after)),
            'images': change(before['images'], after['images'])}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('source', type=Path)
    parser.add_argument('--after', type=Path)
    parser.add_argument('--out', type=Path, required=True)
    args = parser.parse_args()
    out = private_path(ROOT / '.runtime', args.out)
    before = inventory(args.source.read_bytes())
    result = {'before': before}
    if args.after:
        after = inventory(args.after.read_bytes())
        result.update(after=after, comparison=compare(before, after))
    with out.open('x', encoding='utf-8') as stream:
        json.dump(result, stream, indent=2, ensure_ascii=False)
    print('Private read-only OOXML inventory written')


if __name__ == '__main__':
    main()
