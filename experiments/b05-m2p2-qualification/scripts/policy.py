#!/usr/bin/env python3
"""Versioned SHADOW classification only. It cannot publish or authorize real documents."""
from __future__ import annotations

POLICY='B05_M2P2_SHADOW_V1'


def decide(semantics:dict,visual:dict,word_summary:dict|None=None):
    failures=[]
    if not semantics.get('expectedEditExact'):failures.append('EXPECTED_EDIT_NOT_PROVEN')
    if semantics.get('risks'):failures.append('UNSAFE_OOXML_REFERENCES_OR_CONTENT_TYPES')
    if semantics.get('classification')!='EXACT_TEXT_ONLY':failures.append('OOXML_TRANSFORMS_NOT_AUTHORIZED')
    if semantics.get('styles',{}).get('state')!='EQUIVALENT_BY_EXACT_STYLE_DEFINITION':failures.append('STYLE_SEMANTICS_NOT_PROVEN')
    if semantics.get('fields',{}).get('state')=='DIFFERENT':failures.append('FIELD_SEMANTICS_DIFFER')
    if semantics.get('notes',{}).get('status')!='UNCHANGED':failures.append('NOTE_PARTS_NOT_QUALIFIED')
    if semantics.get('protectedParts',{}).get('mediaChanged'):failures.append('MEDIA_CHANGED')
    if visual.get('classification')!='VISUAL_MEASUREMENT_PREQUALIFIED_NOT_WORD_FIDELITY':failures.append('WORD_PDF_VISUAL_NOT_PREQUALIFIED')
    if word_summary is None:failures.append('WORD_METRICS_MISSING')
    else:
        if word_summary.get('decision') not in ('SYNTHETIC_WORD_LAYOUT_PREQUALIFIED_MANUAL_VISUAL_REVIEW_REQUIRED',):
            failures.append('WORD_METRICS_NOT_PREQUALIFIED')
    # Even no failures is only a request for explicit human policy review. This
    # code has no DOCX write or DB path and cannot change production status.
    return {'schema':'b05-m2p2-policy-v1','policy':POLICY,'scope':'SYNTHETIC_ONLY_SHADOW_NO_PRODUCT_WRITE',
            'decision':'SYNTHETIC_POLICY_REVIEW_ELIGIBLE' if not failures else 'HOLD_QUARANTINE',
            'blockers':sorted(set(failures)),'canPromote':False,'officialApproved':False,
            'releaseGate':'G1_G2_M3_STILL_BLOCKED',
            'notice':'A successful shadow outcome is not authorization for promotion or official editing.'}
