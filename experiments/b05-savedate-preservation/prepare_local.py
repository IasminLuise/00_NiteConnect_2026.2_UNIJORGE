"""Prepare fresh synthetic original/compatible inputs for normal local API provisioning."""
from __future__ import annotations
import argparse
import hashlib
import json
from pathlib import Path
from docx import Document
from make_fixture import make
from savedate import convert_savedate


def _entry(folder: Path, file: Path):
    return {'path': file.relative_to(folder).as_posix(),
            'sha256': hashlib.sha256(file.read_bytes()).hexdigest()}


def prepare(folder: Path):
    folder = folder.resolve()
    if '.private' not in folder.parts: raise ValueError('Campaign must be private')
    folder.mkdir(parents=True, exist_ok=False)
    seed = folder/'synthetic-seed.docx'; make(seed)
    document = Document(seed)
    index = 0
    for paragraph in document.paragraphs:
        if paragraph.text.startswith('MARCADOR_EDITAVEL:'):
            index += 1
            paragraph.runs[0].text = paragraph.runs[0].text.replace(
                'MARCADOR_EDITAVEL:', f'MARCADOR_EDITAVEL_LOCAL_{index}:', 1)
    if index != 3: raise ValueError('Unexpected synthetic fixture markers')
    original = folder/'original.docx'; document.save(original)
    raw = original.read_bytes(); compatible_bytes, changes = convert_savedate(raw)
    if len(changes) != 3 or original.read_bytes() != raw:
        raise ValueError('Compatibility conversion or original preservation failed')
    compatible = folder/'compatible.docx'
    with compatible.open('xb') as stream: stream.write(compatible_bytes)
    cases = []
    for name, edit in [('no_edit', None),
                       ('edit_text', {'before': 'MARCADOR_EDITAVEL_LOCAL_1', 'after': 'MARCADOR_EDITADO_LOCAL_1'}),
                       ('edit_table', {'before': 'Célula sintética 0-1-0', 'after': 'Célula editada LOCAL 0-1-0'})]:
        sub = folder/'cases'/name; sub.mkdir(parents=True)
        source = sub/'compatible-source.docx'
        with source.open('xb') as stream: stream.write(compatible_bytes)
        cases.append({'name': name, 'status': 'PLANNED', 'source': _entry(folder, source),
                      'expected_edit': edit, 'engine_docx': None, 'callback_docx': None,
                      'reopened_docx': None})
    plan = {'schema': 'b05-savedate-local-compat-v1', 'corpus': 'synthetic_only', 'local_only': True,
            'original': _entry(folder, original), 'compatible': _entry(folder, compatible),
            'compatibility': {'kind': 'simple_savedate_to_complex', 'converted_fields': len(changes),
                              'changed_parts': sorted({c['part'] for c in changes}),
                              'separate_template_versions_required': True},
            'normal_editor_download': False, 'gate_modified': False, 'native_f9_fixed': False,
            'cases': cases}
    with (folder/'plan-private.json').open('x', encoding='utf-8') as stream:
        json.dump(plan, stream, ensure_ascii=False, indent=2)
    return plan


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--campaign', type=Path, required=True); args = parser.parse_args()
    result = prepare(args.campaign)
    print(json.dumps({'planned_cases': len(result['cases']), 'converted_fields': 3,
                      'engine_executed': False, 'native_f9_fixed': False}))
