"""Entirely synthetic and anonymous test data. Never the real M2P2 fixture."""
import json
import zipfile
from pathlib import Path
from xml.sax.saxutils import escape

from b05_m2p3.docx_inspector import sha256_file

PRE = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body>'''
POST = '</w:body></w:document>'


def make_document(path: Path, *, text="Antigo", bold=False, font=None,
                  br_clear=None, field="PAGE", extra_parts=None, media=b"img",
                  color=None, size=None):
    properties = ""
    if font or bold or color or size:
        properties = "<w:rPr>"
        if font:
            properties += f'<w:rFonts w:ascii="{escape(font)}"/>'
        if bold:
            properties += '<w:b/>'
        if color:
            properties += f'<w:color w:val="{escape(color)}"/>'
        if size:
            properties += f'<w:sz w:val="{escape(str(size))}"/>'
        properties += "</w:rPr>"
    break_xml = '<w:br w:type="page"' + (f' w:clear="{br_clear}"' if br_clear else '') + '/>'
    doc = (PRE + '<w:p><w:r>' + properties + '<w:t>' + escape(text)
           + '</w:t></w:r></w:p>'
           + '<w:p><w:r>' + break_xml + '</w:r></w:p>'
           + '<w:p><w:r><w:fldChar w:fldCharType="begin"/>'
           + '<w:instrText xml:space="preserve">' + escape(field) + '</w:instrText>'
           + '<w:fldChar w:fldCharType="end"/></w:r></w:p>' + POST)
    payloads = {
        '[Content_Types].xml': b'<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"/>',
        'word/document.xml': doc.encode('utf-8'),
        'word/media/img.png': media,
    }
    payloads.update(extra_parts or {})
    with zipfile.ZipFile(path, 'w') as output:
        for name, data in payloads.items():
            output.writestr(name, data)
    return path


def make_pdf(path: Path, text="Antigo", *, size=(200, 200)):
    import fitz
    with fitz.open() as pdf:
        page = pdf.new_page(width=size[0], height=size[1])
        page.insert_text((15, 35), text, fontsize=11)
        pdf.save(str(path))
    return path


def make_campaign(base: Path, *, wrong_mode=None):
    trials = []
    modes = ['no_edit', 'typed', 'plain_paste', 'rich_paste']
    actions = {'no_edit': ['opened', 'closed'], 'typed': ['typed_text'],
               'plain_paste': ['paste_plain'], 'rich_paste': ['paste_rich']}
    for index, mode in enumerate(modes):
        subdir = base / f'session{index}'
        subdir.mkdir()
        names = ['source_docx', 'candidate_docx', 'source_pdf', 'candidate_pdf']
        paths = {name: subdir / (name + ('.pdf' if name.endswith('pdf') else '.docx')) for name in names}
        text = 'Antigo' if mode == 'no_edit' else 'Novo'
        make_document(paths['source_docx'])
        make_document(paths['candidate_docx'], text=text, bold=mode == 'rich_paste')
        make_pdf(paths['source_pdf'])
        make_pdf(paths['candidate_pdf'], text=text)
        events = [{'at': f'2026-10-23T10:00:{k:02}+00:00', 'event': event}
                  for k, event in enumerate(actions[mode])]
        log = subdir / 'operator.json'
        log.write_text(json.dumps({'session_label': f'S{index}', 'confirmed_mode': wrong_mode or mode,
                                   'evidence_strength': 'attested', 'editor_version': 'synthetic',
                                   'events': events}))
        trials.append({'id': f'S{index}', 'mode': mode,
                       **{name: str(path.relative_to(base)) for name, path in paths.items()},
                       'expected_edit': None if mode == 'no_edit' else {'before': 'Antigo', 'after': 'Novo'},
                       'evidence': {**{name + '_sha256': sha256_file(path) for name, path in paths.items()},
                                    'operator_log': str(log.relative_to(base))}})
    manifest = base / 'manifest.json'
    manifest.write_text(json.dumps({'schema_version': 1, 'campaign_id': 'synthetic-test',
                                    'trials': trials}))
    return manifest
