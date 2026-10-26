"""Private diagnostic copies, never part of the WOPI save path."""
import argparse
import io
import json
import re
import uuid
import hashlib
import zipfile
import xml.etree.ElementTree as ET
from pathlib import Path

from fidelity import ROOT, W, private_path


def reduced_settings(data):
    # The minimal package omits note parts. Retaining their separator references
    # makes Word reject an otherwise valid reduction; preserve all other settings.
    return re.sub(rb'<w:(footnotePr|endnotePr)\b[^>]*>.*?</w:\1>', b'', data, flags=re.S)


def field_package(complex_field=False, table=False, field_xml=None, context=None):
    header = field_xml or ('<w:r><w:fldChar w:fldCharType="begin"/></w:r><w:r><w:instrText>SAVEDATE \\* MERGEFORMAT</w:instrText></w:r><w:r><w:fldChar w:fldCharType="separate"/></w:r><w:r><w:t>23/09/2026 23:56:00</w:t></w:r><w:r><w:fldChar w:fldCharType="end"/></w:r>' if complex_field else '<w:fldSimple w:instr="SAVEDATE \\* MERGEFORMAT"><w:r><w:t>23/09/2026 23:56:00</w:t></w:r></w:fldSimple>')
    wrap = lambda name, body: f'<w:{name} xmlns:w="{W[1:-1]}" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">{body}</w:{name}>'.encode()
    paragraph = '<w:p><w:r><w:t>Synthetic diagnostic paragraph with inherited font size.</w:t></w:r></w:p>'
    body = '<w:tbl><w:tblPr/><w:tblGrid><w:gridCol w:w="9000"/></w:tblGrid><w:tr><w:tc>' + paragraph * 30 + '</w:tc></w:tr></w:tbl><w:p/>' if table else paragraph
    sections = '<w:sectPr><w:headerReference w:type="default" r:id="rId2"/><w:pgSz w:w="11906" w:h="16838"/><w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440" w:header="720" w:footer="720"/></w:sectPr>'
    parts = {
        '[Content_Types].xml': b'<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/><Override PartName="/word/header1.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.header+xml"/><Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/><Override PartName="/word/settings.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.settings+xml"/></Types>',
        '_rels/.rels': b'<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/></Relationships>',
        'word/_rels/document.xml.rels': b'<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/><Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/header" Target="header1.xml"/><Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/settings" Target="settings.xml"/></Relationships>',
        'word/document.xml': wrap('document', '<w:body>' + body + sections + '</w:body>'),
        'word/header1.xml': wrap('hdr', '<w:p>' + ('' if table else header) + '</w:p>'),
        'word/styles.xml': wrap('styles', '<w:docDefaults><w:rPrDefault><w:rPr><w:rFonts w:ascii="Calibri" w:hAnsi="Calibri"/><w:lang w:val="pt-BR"/></w:rPr></w:rPrDefault></w:docDefaults><w:style w:type="paragraph" w:default="1" w:styleId="Normal"><w:name w:val="Normal"/><w:rPr><w:sz w:val="24"/></w:rPr></w:style>'),
        'word/settings.xml': wrap('settings', '<w:compat><w:compatSetting w:name="compatibilityMode" w:uri="http://schemas.microsoft.com/office/word" w:val="14"/></w:compat>')}
    parts.update(context or {})
    out = io.BytesIO()
    with zipfile.ZipFile(out, 'w', zipfile.ZIP_DEFLATED) as z:
        for name, data in parts.items():
            z.writestr(name, data)
    return out.getvalue()


def generate(av3, saved_report, out):
    out = private_path(ROOT / '.runtime', out)
    out.mkdir(parents=True, exist_ok=False)
    fixtures = out / 'fixtures'
    fixtures.mkdir()
    (out / 'data').mkdir()
    with zipfile.ZipFile(av3) as z:
        header = ET.fromstring(z.read('word/header1.xml'))
        original_field = next(header.iter(W + 'fldSimple'))
        field_xml = ET.tostring(original_field, encoding='unicode')
        context = {p: z.read(p) for p in ('word/styles.xml', 'word/settings.xml')}
        context['word/settings.xml'] = reduced_settings(context['word/settings.xml'])
    outputs = {'F01': field_package(), 'F02': field_package(True),
               'F03': field_package(field_xml=field_xml, context=context),
               'F04': field_package(field_xml=field_xml), 'F05': field_package(table=True)}
    # Diagnostic copy only: change newly materialized 10pt to Normal's 12pt.
    # Never called from the host, converters or save path.
    buf = io.BytesIO()
    with zipfile.ZipFile(saved_report) as z, zipfile.ZipFile(buf, 'w', zipfile.ZIP_DEFLATED) as target:
        replacements = 0
        for item in z.infolist():
            data = z.read(item)
            if item.filename == 'word/document.xml':
                data, replacements = re.subn(rb'(<w:sz(?:Cs)?\b[^>]*\bw:val=")20(")', rb'\g<1>24\2', data)
            target.writestr(item, data)
    if replacements == 0:
        raise ValueError('Expected size property absent; diagnostic hypothesis does not apply')
    outputs['F06'] = buf.getvalue()
    scenarios = []
    for alias, data in outputs.items():
        name = alias + '.docx'
        (fixtures / name).write_bytes(data)
        scenarios.append({'alias': name, 'document': str(uuid.UUID(bytes=hashlib.md5(name.encode()).digest(), version=3)),
                          'scenario': 'noop' if alias != 'F06' else 'original', 'sha256': hashlib.sha256(data).hexdigest()})
    (out / 'scenarios.json').write_text(json.dumps(scenarios, indent=2), encoding='utf-8')
    (out / 'diagnostic.json').write_text(json.dumps({'font_size_replacements': replacements}), encoding='utf-8')
    overlay = {'services': {'wopi-host': {'volumes': [str((out/'data').resolve())+':/data', str(fixtures.resolve())+':/fixtures:ro']}}}
    (out / 'compose.override.json').write_text(json.dumps(overlay), encoding='utf-8')
    print('Six private diagnostic cases created; F06 contains official content and must never be published')


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--av3', type=Path, required=True)
    p.add_argument('--saved-report', type=Path, required=True)
    p.add_argument('--out', type=Path, required=True)
    args = p.parse_args()
    generate(args.av3, args.saved_report, args.out)
