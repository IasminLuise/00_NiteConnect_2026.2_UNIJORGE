#!/usr/bin/env python3
"""Compare independent Word/PDF metrics to OOXML diagnostics; emit sanitized decision.

Never publishes a document, promotes quarantine, or labels editorial acceptance.
"""
from __future__ import annotations
import argparse
import json
from pathlib import Path
from typing import Any
import fitz
from ooxml_fidelity import private_path

# Changing text naturally changes pixels. This is a metric, NOT a pixel pass criterion.
def pdf_differences(a: Path,b: Path):
    with fitz.open(a) as left,fitz.open(b) as right:
        pages_a=len(left);pages_b=len(right)
        diffs=[]; sizes=[]
        for j in range(min(pages_a,pages_b)):
            ap=left[j];bp=right[j]
            sizes.append({'page':j+1,'referencePt':[round(ap.rect.width,3),round(ap.rect.height,3)],
                         'candidatePt':[round(bp.rect.width,3),round(bp.rect.height,3)]})
            if abs(ap.rect.width-bp.rect.width)>0.01 or abs(ap.rect.height-bp.rect.height)>0.01:
                diffs.append({'page':j+1,'changedPixels':None,'reason':'PAGE_SIZE_CHANGED'})
                continue
            # Same PDF renderer, 144 dpi. No mask: highlights unintended changes for manual review.
            pix_a=ap.get_pixmap(matrix=fitz.Matrix(2,2),alpha=False)
            pix_b=bp.get_pixmap(matrix=fitz.Matrix(2,2),alpha=False)
            if (pix_a.width,pix_a.height)!=(pix_b.width,pix_b.height):
                diffs.append({'page':j+1,'changedPixels':None,'reason':'RASTER_SIZE_MISMATCH'});continue
            data_a=pix_a.samples;data_b=pix_b.samples
            changed=sum(data_a[k:k+3]!=data_b[k:k+3] for k in range(0,len(data_a),3))
            diffs.append({'page':j+1,'changedPixels':changed,'totalPixels':pix_a.width*pix_a.height})
        return {'sourcePages':pages_a,'candidatePages':pages_b,'paper':sizes,'rawPixelDifferences':diffs}

def _expected_word_edit(a,b,index):
    if len(a)!=len(b):return False
    changed=[j for j,(x,y) in enumerate(zip(a,b)) if x.get('text')!=y.get('text')]
    # Word's paragraph enumeration can include headers differently. An edit not located
    # at exact body index must be checked manually, never guessed.
    return changed==[index]

def synthesize(ooxml:dict, ref:dict, candidate:dict, pixels:dict,
               *, changed_word_index:int|None=None, tolerance_pt:float=0.75):
    if ref.get('id')!='source' or candidate.get('id')!='candidate':raise ValueError('Wrong Word jobs')
    ra=ref.get('paragraphs',[]);ca=candidate.get('paragraphs',[])
    if not isinstance(ra,list) or not isinstance(ca,list):raise ValueError('Missing Word paragraphs')
    metrics={}
    metrics['wordBuildsMatch']=(ref.get('wordBuild')==candidate.get('wordBuild') and ref.get('wordVersion')==candidate.get('wordVersion'))
    # Legacy reports lack the observation; new probes must not hide a known
    # change between pre-PDF metrics and the state after PDF generation.
    if 'layoutChangedDuringPdf' in ref or 'layoutChangedDuringPdf' in candidate:
        metrics['wordPdfLayoutStateStable']=(ref.get('layoutChangedDuringPdf') is False and candidate.get('layoutChangedDuringPdf') is False)
    metrics['wordPageCountsEqual']=(isinstance(ref.get('pages'),int) and ref.get('pages')==candidate.get('pages'))
    metrics['wordPdfPageCountsEqual']=(pixels['sourcePages']==pixels['candidatePages']==ref.get('pages'))
    metrics['paperSizesEqual']=all(q['referencePt']==q['candidatePt'] for q in pixels['paper'])
    metrics['wordFieldCodesEqual']=(ref.get('fields')==candidate.get('fields'))
    metrics['wordParagraphCountsEqual']=(len(ra)==len(ca) and len(ra)>0)
    if changed_word_index is None and len(ra)==len(ca):
        changes=[i for i,(x,y) in enumerate(zip(ra,ca)) if x.get('text')!=y.get('text')]
        changed_word_index=changes[0] if len(changes)==1 else None
    metrics['wordEditPositionVerified']=_expected_word_edit(ra,ca,changed_word_index) if changed_word_index is not None else False
    different=[]; coordinates=[]
    for j,(a,b) in enumerate(zip(ra,ca)):
        if j==changed_word_index:continue
        # No tolerance on formatting properties. Coordinate tolerance covers Word rounding.
        keys=('font','fontSize','after','line','lineRule','page')
        changed=[k for k in keys if a.get(k)!=b.get(k)]
        if changed:different.append({'index':j,'attributes':changed})
        for axis in ('x','y'):
            aa=a.get(axis);bb=b.get(axis)
            if not isinstance(aa,(float,int)) or not isinstance(bb,(float,int)) or abs(aa-bb)>tolerance_pt:
                coordinates.append({'index':j,'axis':axis,'deltaPt':round(bb-aa,3) if isinstance(aa,(float,int)) and isinstance(bb,(float,int)) else None})
    metrics['unchangedParagraphFormatsEqual']=(not different and metrics['wordParagraphCountsEqual'])
    metrics['unchangedAnchorsWithinTolerance']=(not coordinates and metrics['wordParagraphCountsEqual'])
    metrics['ooxmlTextOnlyPrequalified']=ooxml.get('outcome')=='STRUCTURAL_TEXT_ONLY_PREQUALIFIED_NOT_FIDELITY_APPROVAL'
    blocked=[k for k,v in metrics.items() if not v]
    status=('SYNTHETIC_WORD_LAYOUT_PREQUALIFIED_MANUAL_VISUAL_REVIEW_REQUIRED' if not blocked else 'HOLD_QUARANTINE')
    return {'schema':'b05-m2p-summary-v1','decision':status,'canPromote':False,'officialApproved':False,
            'metrics':metrics,'blockers':blocked,'pdf':pixels,'firstFormatDiffs':different[:12],
            'formatDiffCount':len(different),'firstAnchorDiffs':coordinates[:12],
            'anchorDiffCount':len(coordinates),'ooxmlBlockers':ooxml.get('blockers',[]),
            'interpretation':'PDF pixel differences include the authorized text edit; visual sign-off is separate.'}

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--ooxml',type=Path,required=True);p.add_argument('--source-word',type=Path,required=True)
    p.add_argument('--candidate-word',type=Path,required=True);p.add_argument('--source-pdf',type=Path,required=True)
    p.add_argument('--candidate-pdf',type=Path,required=True)
    p.add_argument('--changed-word-index',type=int,default=None)
    p.add_argument('--out',type=Path,required=True)
    a=p.parse_args()
    for x in (a.ooxml,a.source_word,a.candidate_word,a.source_pdf,a.candidate_pdf,a.out):private_path(x)
    if a.out.exists():raise SystemExit('Refusing to overwrite report')
    oo=json.loads(a.ooxml.read_text(encoding='utf-8-sig'))
    ref=json.loads(a.source_word.read_text(encoding='utf-8-sig'))
    cand=json.loads(a.candidate_word.read_text(encoding='utf-8-sig'))
    pixels=pdf_differences(a.source_pdf,a.candidate_pdf)
    result=synthesize(oo,ref,cand,pixels,changed_word_index=a.changed_word_index)
    a.out.parent.mkdir(parents=True,exist_ok=True)
    with a.out.open('x',encoding='utf-8') as f:json.dump(result,f,indent=2,ensure_ascii=False)
    print(json.dumps({'decision':result['decision'],'blockers':result['blockers'],'pageCounts':[ref.get('pages'),cand.get('pages')]}))
if __name__=='__main__':main()
