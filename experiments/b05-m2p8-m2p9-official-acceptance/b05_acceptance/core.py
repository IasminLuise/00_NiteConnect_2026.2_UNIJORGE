"""B05-M2P8/M2P9: independent evidence on authentic NITE DOCX.

Never creates a 'pass' from fixture-type tests or missing observations. Read-only.
Requires earlier B05 M2P4/M2P6 analyzers in the repo; no OOXML normalization.
"""
from __future__ import annotations

import hashlib
import json
import os
from pathlib import Path
import posixpath
import sys
from zipfile import ZipFile
from xml.etree import ElementTree as ET

ROOT = Path(__file__).resolve().parents[3]
PREV_SEMANTIC = ROOT / 'experiments' / 'b05-m2p6-m2p7-qualification'
PREV_PDF = ROOT / 'experiments' / 'b05-m2p4-m2p5-engine-decision'


def analyzers():
    if not (PREV_SEMANTIC / 'b05_qualification' / 'semantic.py').is_file() or not (PREV_PDF / 'b05_decision' / 'pdf.py').is_file():
        raise RuntimeError('Dependências locais M2P6/M2P7 e M2P4/M2P5 não encontradas no repositório; aplique patches anteriores.')
    for folder in (PREV_SEMANTIC, PREV_PDF):
        if str(folder) not in sys.path:
            sys.path.insert(0, str(folder))
    from b05_qualification.semantic import inspect_docx, compare_docs
    from b05_decision.pdf import raster_compare
    return inspect_docx, compare_docs, raster_compare


def sha(path):
    h = hashlib.sha256()
    with open(path, 'rb') as f:
        for buf in iter(lambda: f.read(1024 * 1024), b''):
            h.update(buf)
    return h.hexdigest()


def safe_file(root: Path, relative: str, *, suffix: str | None = None) -> Path:
    if not isinstance(relative, str) or not relative or '\\' in relative or ':' in relative or relative.startswith('/'):
        raise ValueError('Caminho deve ser relativo, local e sem links remotos')
    root = root.resolve()
    try:
        target = (root / relative).resolve(strict=True)
    except (OSError, RuntimeError) as exc:
        raise ValueError('Evidência ausente ou caminho inválido') from exc
    if not target.is_relative_to(root) or not target.is_file() or target.is_symlink():
        raise ValueError('Arquivo fora da campanha / ausente / link não permitido')
    if suffix is not None and target.suffix.lower() != suffix:
        raise ValueError('Extensão diferente da esperada')
    if target.stat().st_size > 200 * 1024 * 1024:
        raise ValueError('Arquivo excede 200 MB')
    return target


def checked_file(root, entry, suffix, ledger):
    if not isinstance(entry, dict) or not isinstance(entry.get('path'), str) or not isinstance(entry.get('sha256'), str):
        raise ValueError('Evidência requer caminho e hash original')
    target = safe_file(root, entry['path'], suffix=suffix)
    observed = sha(target)
    if observed != entry['sha256'] or len(observed) != 64:
        raise ValueError('SHA-256 divergente: evidência alterada')
    # A distinct physical copy is required for each acquired stage.
    if target in ledger and ledger[target] != observed:
        raise ValueError('Evidência reaproveitada com hash divergente')
    ledger[target] = observed
    return target


def table_cell_texts(path: Path) -> list[str]:
    with ZipFile(path) as archive:
        xml = archive.read('word/document.xml')
    if b'<!DOCTYPE' in xml.upper() or b'<!ENTITY' in xml.upper():
        raise ValueError('XML não confiável')
    root = ET.fromstring(xml)
    w = '{http://schemas.openxmlformats.org/wordprocessingml/2006/main}'
    return [''.join((node.text or '') for node in cell.iter(w+'t'))
            for table in root.iter(w+'tbl') for cell in table.iter(w+'tc')]


def orphan_rels(path: Path) -> list[str]:
    """Compare anomaly identities, not only cardinalities (M2P7 limitation)."""
    with ZipFile(path) as archive:
        names = set(archive.namelist())
        bad = []
        for rel in sorted(p for p in names if p.endswith('.rels') and p != '_rels/.rels'):
            folder, name = posixpath.split(rel)
            if not folder.endswith('/_rels'):
                continue
            owner = posixpath.join(folder[:-6], name[:-5])
            if owner not in names:
                bad.append(rel)
        return bad


def _receipt(root, entry, ledger, kind):
    if entry is None:
        return None
    path = checked_file(root, entry, '.json', ledger)
    data = json.loads(path.read_text(encoding='utf-8'))
    if not isinstance(data, dict) or data.get('kind') != kind:
        raise ValueError('Recibo não corresponde à categoria esperada')
    return data


def _case_receipt(root, entry, ledger, doc_id, scenario, source_sha, acquired):
    data = _receipt(root, entry, ledger, 'acquisition')
    if data is None:
        return None
    if data.get('document') != doc_id or data.get('scenario') != scenario:
        raise ValueError('Proveniência de outra operação/documento')
    integrity = (data.get('source_sha256') == source_sha and
                 isinstance(data.get('acquired_sha256'), dict) and
                 all(data['acquired_sha256'].get(stage) == sha(path) for stage, path in acquired.items()))
    normal = (data.get('normal_permissions') is True and
              data.get('permission_override') is False and
              data.get('test_authorized') is True and
              bool(data.get('editor_version')) and bool(data.get('capture_method')) and
              bool(data.get('operator_recorded_at')))
    return {'valid_normal_permissions': bool(normal and integrity), 'callback_observed': data.get('callback_status'),
            'captured_hashes_verified': bool(integrity)}


def _review_receipt(root, entry, ledger, doc_id, scenario, observed_hash, role='editorial'):
    data = _receipt(root, entry, ledger, role + '_review')
    if not data:
        return {'status': 'MISSING'}
    if data.get('document') != doc_id or data.get('scenario') != scenario or data.get('candidate_sha256') != observed_hash:
        return {'status': 'MISMATCH'}
    if data.get('reviewer_role') != role or not data.get('full_pages_inspected') or not data.get('reason'):
        return {'status': 'INCOMPLETE'}
    decision = data.get('decision')
    return {'status': decision if decision in ('ACCEPT', 'REJECT') else 'INCOMPLETE'}


def _compare_docx(a, b, expected_edit, inspect_docx, compare_docs):
    ia, ib = inspect_docx(a), inspect_docx(b)
    comparison = compare_docs(ia, ib, expected_edit)
    # Existing analyzer's owner counter missed this in the original distributed kit.
    owners_a, owners_b = set(orphan_rels(a)), set(orphan_rels(b))
    new_orphans = sorted(owners_b - owners_a)
    fixed_orphans = sorted(owners_a - owners_b)
    return {'text_as_expected': comparison['expected_visible_text_only'],
            'paragraph_counts': comparison['paragraph_counts'],
            'field_instructions_equal': comparison['field_instructions_equal_in_order'],
            'notes_referenced_text_signatures_equal': comparison['notes_referenced_text_signatures_equal'],
            'explicit_format_mismatch_chars': comparison['explicit_format_mismatch_chars_on_matched_text'],
            'parts_added': comparison['parts_added'], 'parts_removed': comparison['parts_removed'],
            'parts_modified_count': len(comparison['parts_modified']),
            'broken_relationships_delta': comparison['candidate_new_broken_relationships'],
            'missing_content_types_delta': comparison['candidate_new_missing_content_types'],
            'new_orphan_owner_rels': new_orphans, 'removed_orphan_owner_rels': fixed_orphans,
            'features_source': ia['features'], 'features_candidate': ib['features'],
            'structural_equivalence_not_proven': bool(
                comparison['parts_added'] or comparison['parts_removed'] or comparison['parts_modified'] or
                not comparison['field_instructions_equal_in_order'] or not comparison['notes_referenced_text_signatures_equal'] or
                comparison['explicit_format_mismatch_chars_on_matched_text'])}


def assess(manifest_path: Path) -> dict:
    manifest_path = manifest_path.resolve(strict=True)
    root = manifest_path.parent
    raw = json.loads(manifest_path.read_text(encoding='utf-8'))
    if raw.get('schema') != 1 or raw.get('corpus_kind') != 'official_nite' or raw.get('engine') != 'ONLYOFFICE_DOCS':
        raise ValueError('Este teste exige corpus oficial NITE e motor explícito ONLYOFFICE_DOCS')
    documents = raw.get('documents')
    if not isinstance(documents, list) or {d.get('id') for d in documents if isinstance(d, dict)} != {'AV3', 'RELATORIO_SEMANAL'} or len(documents) != 2:
        raise ValueError('Exigidos exatamente AV3 e RELATORIO_SEMANAL, sem fixtures substitutas')
    inspect_docx, compare_docs, raster_compare = analyzers()
    ledger, cases, problems, pending = {}, [], [], []
    for doc in documents:
        doc_id = doc['id']
        source = checked_file(root, doc.get('official_source'), '.docx', ledger)
        src_initial_hash = sha(source)
        official_receipt = _receipt(root, doc.get('official_source_receipt'), ledger, 'official_source')
        if official_receipt is None or official_receipt.get('document') != doc_id or official_receipt.get('sha256') != src_initial_hash or official_receipt.get('authorized_corpus') is not True or not official_receipt.get('owner_approval'):
            pending.append(doc_id + ':OFFICIAL_SOURCE_AUTHORIZATION_NOT_ATTESTED')
        baseline_pdf = checked_file(root, doc.get('word_reference_pdf'), '.pdf', ledger) if doc.get('word_reference_pdf') else None
        if baseline_pdf is None: pending.append(doc_id + ':WORD_REFERENCE_PDF_MISSING')
        scenarios = doc.get('scenarios', [])
        if {c.get('name') for c in scenarios if isinstance(c, dict)} != {'no_edit', 'edit_text', 'edit_table'} or len(scenarios) != 3:
            raise ValueError('Três operações reais obrigatórias por modelo')
        for case in scenarios:
            name = case['name']; label = doc_id + '/' + name
            expected = case.get('expected_edit') if name != 'no_edit' else None
            if name != 'no_edit' and (not isinstance(expected, dict) or not all(isinstance(expected.get(v), str) and expected[v] for v in ('before', 'after')) or expected['before'] == expected['after']):
                pending.append(label + ':EDIT_SPEC_MISSING')
                expected = None
            file_map = {}; missing = []
            stages = ['engine_docx', 'reopened_docx'] + (['callback_docx'] if name != 'no_edit' else [])
            for stage in stages:
                if case.get(stage) is None:
                    missing.append(stage)
                else:
                    file_map[stage] = checked_file(root, case[stage], '.docx', ledger)
            pdf_map = {}
            for stage in ('engine_pdf', 'reopened_pdf') + (('callback_pdf',) if name != 'no_edit' else ()):
                if case.get(stage) is None:
                    missing.append(stage)
                else:
                    pdf_map[stage] = checked_file(root, case[stage], '.pdf', ledger)
            origin_session_hash = checked_file(root, case.get('session_source_docx'), '.docx', ledger) if case.get('session_source_docx') else None
            if origin_session_hash is None or sha(origin_session_hash) != src_initial_hash:
                missing.append('isolated_session_source_matches_official')
            receipt = _case_receipt(root, case.get('acquisition_receipt'), ledger, doc_id, name, src_initial_hash, file_map)
            if receipt is None or not receipt['valid_normal_permissions']:
                missing.append('verified_normal_permissions_receipt')
            if receipt and name == 'no_edit' and receipt.get('callback_observed') not in (4, '4'):
                missing.append('observed_terminal_status_4_no_edit')
            if receipt and name != 'no_edit' and receipt.get('callback_observed') not in (2, '2'):
                missing.append('observed_terminal_status_2_edit')
            if len(set(file_map.values())) < len(file_map):
                missing.append('distinct_stage_files_required')
            if missing:
                pending.append(label + ':MISSING_' + ','.join(sorted(set(missing))))
            pairs, rasters = {}, {}
            if name == 'edit_table' and expected and sum(cell.count(expected['before']) for cell in table_cell_texts(source)) != 1:
                problems.append(label + ':EXPECTED_TABLE_CELL_TARGET_NOT_UNIQUE')
            for stage, file in file_map.items():
                if name == 'edit_table' and expected and sum(cell.count(expected['after']) for cell in table_cell_texts(file)) != 1:
                    problems.append(label + ':TABLE_CELL_EDIT_NOT_FOUND_' + stage)
                pairs['SOURCE_TO_' + stage.upper()] = _compare_docx(source, file, expected, inspect_docx, compare_docs)
                if not pairs['SOURCE_TO_' + stage.upper()]['text_as_expected']:
                    problems.append(label + ':VISIBLE_TEXT_CHANGED_' + stage)
                if pairs['SOURCE_TO_' + stage.upper()]['new_orphan_owner_rels'] or pairs['SOURCE_TO_' + stage.upper()]['broken_relationships_delta'] or pairs['SOURCE_TO_' + stage.upper()]['missing_content_types_delta']:
                    problems.append(label + ':NEW_OPC_ANOMALY_' + stage)
            if 'engine_docx' in file_map and 'callback_docx' in file_map:
                pairs['ENGINE_TO_CALLBACK'] = _compare_docx(file_map['engine_docx'], file_map['callback_docx'], None, inspect_docx, compare_docs)
                if not pairs['ENGINE_TO_CALLBACK']['text_as_expected']:
                    problems.append(label + ':SDK_CALLBACK_TEXT_MISMATCH')
            if 'engine_docx' in file_map and 'reopened_docx' in file_map:
                pairs['ENGINE_TO_REOPENED'] = _compare_docx(file_map['engine_docx'], file_map['reopened_docx'], None, inspect_docx, compare_docs)
                if not pairs['ENGINE_TO_REOPENED']['text_as_expected']:
                    problems.append(label + ':ENGINE_TO_REOPENED_TEXT_CHANGED')
            if 'callback_docx' in file_map and 'reopened_docx' in file_map:
                pairs['CALLBACK_TO_REOPENED'] = _compare_docx(file_map['callback_docx'], file_map['reopened_docx'], None, inspect_docx, compare_docs)
                if not pairs['CALLBACK_TO_REOPENED']['text_as_expected']:
                    problems.append(label + ':REOPENED_TEXT_CHANGED')
            if baseline_pdf:
                for stage, file in pdf_map.items():
                    result_pdf = raster_compare(baseline_pdf, file, dpi=144)
                    rasters['WORD_REFERENCE_TO_' + stage.upper()] = result_pdf
                    if not result_pdf['comparable']:
                        problems.append(label + ':PDF_PAGE_OR_GEOMETRY_MISMATCH_' + stage)
                    if name == 'no_edit' and result_pdf['total_different_pixels'] > 0:
                        pending.append(label + ':NO_EDIT_RASTER_RESIDUAL_NEEDS_FORMAL_ADJUDICATION')
            if 'engine_pdf' in pdf_map and 'callback_pdf' in pdf_map:
                rasters['ENGINE_TO_CALLBACK'] = raster_compare(pdf_map['engine_pdf'], pdf_map['callback_pdf'], dpi=144)
            receipt_e = _review_receipt(root, case.get('editorial_receipt'), ledger, doc_id, name,
                                        sha(file_map['callback_docx'] if 'callback_docx' in file_map else file_map['engine_docx']) if file_map else '')
            receipt_t = _review_receipt(root, case.get('technical_receipt'), ledger, doc_id, name,
                                        sha(file_map['callback_docx'] if 'callback_docx' in file_map else file_map['engine_docx']) if file_map else '', 'technical')
            if receipt_e['status'] != 'ACCEPT':
                pending.append(label + ':EDITORIAL_' + receipt_e['status'])
            if receipt_t['status'] != 'ACCEPT':
                pending.append(label + ':TECHNICAL_' + receipt_t['status'])
            if receipt_e['status'] == 'REJECT': problems.append(label + ':EDITORIAL_REJECTED')
            if receipt_t['status'] == 'REJECT': problems.append(label + ':TECHNICAL_REJECTED')
            if any(p['structural_equivalence_not_proven'] for p in pairs.values()):
                if receipt_t['status'] != 'ACCEPT': pending.append(label + ':STRUCTURAL_TRANSFORMATIONS_NOT_ADJUDICATED')
            cases.append({'document': doc_id, 'scenario': name, 'provenance': receipt or {'valid_normal_permissions': False},
                          'missing_artifacts': missing, 'comparisons': pairs, 'raster': rasters,
                          'editorial_review': receipt_e, 'technical_review': receipt_t, 'original_hash_preserved': sha(source) == src_initial_hash})
    control = _receipt(root, raw.get('backend_integration_receipt'), ledger, 'backend_integration')
    if control is None or not all(control.get(k) is True for k in ('entra_authorized', 'rbac_enforced', 'signed_callback', 'quarantine', 'source_head_preserved', 'audit_recovery', 'normal_permissions')):
        pending.append('NORMAL_BACKEND_INTEGRATION_NOT_ATTESTED')
    if control and control.get('permission_override') is not False:
        problems.append('BACKEND_USED_PERMISSION_OVERRIDE')
    if control and control.get('test_authorized') is not True:
        pending.append('BACKEND_TEST_AUTHORIZATION_NOT_ATTESTED')
    if any(sha(p) != h for p, h in ledger.items()):
        raise RuntimeError('Alguma evidência foi modificada durante a avaliação')
    dual_rejection = any(c['editorial_review']['status'] == 'REJECT' and c['technical_review']['status'] == 'REJECT' for c in cases)
    status = ('NO_GO_RECOMMENDED_TWO_INDEPENDENT_REJECTIONS' if dual_rejection else
              'STOP_AND_REVIEW_DEMONSTRATED_RISKS' if problems else
              'HOLD_MISSING_OFFICIAL_EVIDENCE' if pending else
              'TECHNICALLY_ELIGIBLE_FOR_FORMAL_DECISION_NOT_APPROVED')
    return {'campaign_id': raw.get('campaign_id'), 'schema': 1, 'report_kind': 'OFFICIAL_DOCUMENT_ACCEPTANCE',
            'engine': 'ONLYOFFICE_DOCS', 'corpus': ['AV3', 'RELATORIO_SEMANAL'],
            'real_round_trip_completed': not any(c['missing_artifacts'] for c in cases),
            'cases': cases, 'risk_flags': sorted(set(problems)), 'pending': sorted(set(pending)),
            'recommendation': status, 'technical_editorial_rejection_both_present': dual_rejection, 'canPromote': False, 'officialApproved': False,
            'backend_final_integration_authorized': False, 'formal_approval_granted': False,
            'gate_controls_unchanged': True, 'all_input_hashes_verified_before_and_after': True,
            'limits': ['A integridade dos arquivos e o raster são verificados; comportamento interativo é evidência atestada externa.',
                       'Comparações de formatação OOXML explícita não certificam estilo efetivo ou equivalência estrutural.',
                       'Diferenças de raster são registradas integralmente, sem mascaramento ou aprovação automática.',
                       'Revisão editorial e integração normal dependem de recibos independentes auditáveis.']}
