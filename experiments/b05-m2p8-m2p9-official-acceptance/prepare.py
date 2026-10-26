"""Create isolated sessions from authentic supplied files; never generates a fake DOCX."""
from __future__ import annotations
import argparse
import json
from pathlib import Path
import shutil
import zipfile
from b05_acceptance.core import sha


def entry(rel, target):
    return {'path': rel.replace('\\', '/'), 'sha256': sha(target)}


def prepare(av3: Path, weekly: Path, out: Path, av3_word_pdf=None, weekly_word_pdf=None):
    out = out.resolve()
    if out.exists():
        raise ValueError('Campanha já existe: proibido sobrescrever evidências')
    docs = [('AV3', av3, av3_word_pdf), ('RELATORIO_SEMANAL', weekly, weekly_word_pdf)]
    for label, source, pdf in docs:
        if not source.is_file() or not zipfile.is_zipfile(source):
            raise ValueError(label + ': arquivo DOCX original real indisponível/inválido')
        with zipfile.ZipFile(source) as z:
            if 'word/document.xml' not in z.namelist() or z.testzip() is not None:
                raise ValueError(label + ': DOCX incorreto')
        if pdf and (not pdf.is_file() or pdf.suffix.lower() != '.pdf' or pdf.read_bytes()[:5] != b'%PDF-'):
            raise ValueError(label + ': PDF de referência não válido')
    out.mkdir(parents=True)
    manifest = {'schema': 1, 'campaign_id': out.name, 'corpus_kind': 'official_nite',
                'engine': 'ONLYOFFICE_DOCS', 'backend_integration_receipt': None, 'documents': []}
    scenarios = ['no_edit', 'edit_text', 'edit_table']
    for label, source, pdf in docs:
        srcpath = out / 'sources' / (label + '.docx')
        srcpath.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(source, srcpath)
        if sha(srcpath) != sha(source):
            raise RuntimeError('Origem copiada com bytes diferentes')
        ref_entry = None
        if pdf:
            refpath = out / 'references' / (label + '-word-reference.pdf')
            refpath.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(pdf, refpath)
            ref_entry = entry(str(refpath.relative_to(out)), refpath)
        record = {'id': label, 'official_source': entry(str(srcpath.relative_to(out)), srcpath),
                  'word_reference_pdf': ref_entry, 'official_source_receipt': None, 'scenarios': []}
        for case in scenarios:
            itempath = out / 'sessions' / label / (case + '-source.docx')
            itempath.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(srcpath, itempath)
            if sha(itempath) != sha(srcpath):
                raise RuntimeError('Sessão não partiu do DOCX original')
            record['scenarios'].append({
                'name': case,
                'session_source_docx': entry(str(itempath.relative_to(out)), itempath),
                'expected_edit': None,
                'engine_docx': None, 'callback_docx': None,
                'reopened_docx': None, 'engine_pdf': None,
                'callback_pdf': None, 'reopened_pdf': None,
                'acquisition_receipt': None, 'editorial_receipt': None, 'technical_receipt': None})
        manifest['documents'].append(record)
    mf = out / 'manifest.json'
    mf.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    return mf


def main():
    ap=argparse.ArgumentParser(description='Criar campanha B05 com DOCX oficiais originais, nunca fixtures sintéticas')
    ap.add_argument('--av3-docx', type=Path, required=True)
    ap.add_argument('--weekly-docx', type=Path, required=True)
    ap.add_argument('--av3-word-pdf', type=Path)
    ap.add_argument('--weekly-word-pdf', type=Path)
    ap.add_argument('--campaign', type=Path, required=True)
    args=ap.parse_args()
    print('Manifesto:', prepare(args.av3_docx, args.weekly_docx, args.campaign, args.av3_word_pdf, args.weekly_word_pdf))
    print('Use apenas saída real do ONLYOFFICE e PDF efetivamente gerado pelo Word. Não use snapshots reconstruídos.')

if __name__ == '__main__':
    main()
