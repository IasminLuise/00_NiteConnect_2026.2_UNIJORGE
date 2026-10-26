#!/usr/bin/env python3
"""One-shot sanitized M2P2 decision: private input only, fresh private output only."""
from __future__ import annotations
import argparse,json
from pathlib import Path
from semantic import analyze_semantics
from visual import compare_pdfs
from policy import decide


def _private(path):
    if '.private' not in path.resolve().parts:raise ValueError('Expected private path')
    return path


def main():
    ap=argparse.ArgumentParser()
    for option in ('source','candidate','expected','source-pdf','candidate-pdf','word-summary','out'):
        ap.add_argument('--'+option,required=True,type=Path)
    a=ap.parse_args()
    for value in vars(a).values():_private(value)
    if a.out.exists():raise SystemExit('Do not overwrite results')
    expected=json.loads(a.expected.read_text(encoding='utf-8-sig'))
    word=json.loads(a.word_summary.read_text(encoding='utf-8-sig'))
    sem=analyze_semantics(a.source,a.candidate,expected)
    vis=compare_pdfs(a.source_pdf,a.candidate_pdf)
    result={'schema':'b05-m2p2-shareable-v1','semantics':sem,'visual':vis,'shadowPolicy':decide(sem,vis,word),
            'dataClassification':'NO_TEXT_BYTES_TOKENS_HASHES_OR_FILEPATHS',
            'conditions':'Requires private manual review of DOCX, Word and PDF before any policy implementation.'}
    a.out.parent.mkdir(parents=True,exist_ok=True)
    with a.out.open('x',encoding='utf-8') as f:json.dump(result,f,ensure_ascii=False,indent=2)
    print(json.dumps({'decision':result['shadowPolicy']['decision'],'visual':vis['classification'],'semantic':sem['classification']}))

if __name__=='__main__':main()
