#!/usr/bin/env python3
"""Two-PDF comparison: authorize no pixel differences outside an inferred edit line.

Changed-line ROI is derived from independently extracted PDF text, not a user mask.
A pass is a limited visual measurement, never a full DOCX equivalence proof.
"""
from __future__ import annotations
from pathlib import Path
import fitz
import numpy as np

DPI=144
EPS_PT=0.75


def lines_of(page):
    lines=[]
    for block in page.get_text('dict',sort=False)['blocks']:
        if block.get('type')!=0:continue
        for line in block.get('lines',[]):
            spans=line.get('spans',[])
            if not spans:continue
            lines.append({'text':''.join(s.get('text','') for s in spans),
                          'rect':fitz.Rect(line['bbox']),
                          'spans':[(s.get('font',''),round(float(s.get('size',0)),3),fitz.Rect(s['bbox'])) for s in spans]})
    return lines


def close_boxes(a,b,tol):
    return all(abs(x-y)<=tol for x,y in zip(a,b))


def _pixel_box(r, width,height):
    scale=DPI/72
    return max(0,int((r.x0-2)*scale)),max(0,int((r.y0-2)*scale)),min(width,int((r.x1+2)*scale)+1),min(height,int((r.y1+2)*scale)+1)


def _components(mask,max_count=30000):
    """Bounding boxes of residual differences; never implies antialiasing or safety."""
    ys,xs=np.nonzero(mask)
    if not len(xs):return []
    if len(xs)>max_count:
        return [{'pixels':int(len(xs)),'bboxPx':[int(xs.min()),int(ys.min()),int(xs.max()+1),int(ys.max()+1)],'truncated':True}]
    pixels=set(zip(map(int,xs),map(int,ys)))
    components=[]
    while pixels:
        start=pixels.pop();todo=[start];n=0;x0=x1=start[0];y0=y1=start[1]
        while todo:
            x,y=todo.pop();n+=1;x0=min(x0,x);x1=max(x1,x);y0=min(y0,y);y1=max(y1,y)
            for neighbor in ((x+1,y),(x-1,y),(x,y+1),(x,y-1)):
                if neighbor in pixels:pixels.remove(neighbor);todo.append(neighbor)
        components.append({'pixels':n,'bboxPx':[x0,y0,x1+1,y1+1]})
    return sorted(components,key=lambda c:-c['pixels'])[:12]


def compare_pdfs(source:Path,candidate:Path):
    if '.private' not in source.resolve().parts or '.private' not in candidate.resolve().parts:
        raise ValueError('PDFs must remain inside .private')
    with fitz.open(source) as a,fitz.open(candidate) as b:
        out={'schema':'b05-m2p2-visual-v1','sourcePages':len(a),'candidatePages':len(b),
             'pages':[],'unexplainedPixels':0,'editedPixels':0,'changedTextLines':0,
             'layoutPreserved':False,'classification':'HOLD_QUARANTINE'}
        if len(a)!=len(b) or not len(a):return out
        geometry_ok=True;all_lines_ok=True;edit_lines=[]
        for page_idx in range(len(a)):
            left=a[page_idx];right=b[page_idx]
            if not close_boxes(left.rect,right.rect,0.01):geometry_ok=False;break
            src_lines=lines_of(left);cand_lines=lines_of(right)
            if len(src_lines)!=len(cand_lines):all_lines_ok=False
            modified=[];unmodified_ok=len(src_lines)==len(cand_lines)
            for i,(l,r) in enumerate(zip(src_lines,cand_lines)):
                if l['text']!=r['text']:
                    modified.append((l,r));edit_lines.append((page_idx,i))
                else:
                    if not close_boxes(l['rect'],r['rect'],EPS_PT) or [s[:2] for s in l['spans']] != [s[:2] for s in r['spans']]:
                        unmodified_ok=False
            pix_a=left.get_pixmap(matrix=fitz.Matrix(2,2),colorspace=fitz.csRGB,alpha=False)
            pix_b=right.get_pixmap(matrix=fitz.Matrix(2,2),colorspace=fitz.csRGB,alpha=False)
            if (pix_a.width,pix_a.height,pix_a.n)!=(pix_b.width,pix_b.height,pix_b.n):geometry_ok=False;break
            width,height=pix_a.width,pix_a.height
            pixels_a=np.frombuffer(pix_a.samples,dtype=np.uint8).reshape(height,width,3)
            pixels_b=np.frombuffer(pix_b.samples,dtype=np.uint8).reshape(height,width,3)
            # Same rasterization repeated on the same PDF should be deterministic.
            selfcontrol=left.get_pixmap(matrix=fitz.Matrix(2,2),colorspace=fitz.csRGB,alpha=False)
            repeat_ok=selfcontrol.samples==pix_a.samples
            changed=np.any(pixels_a!=pixels_b,axis=2)
            authorized=np.zeros((height,width),dtype=bool)
            for l,r in modified:
                # Changes are limited to the union of the changed LINE bboxes. A
                # second changed line means the overall policy will HOLD.
                rect=l['rect']|r['rect']
                x0,y0,x1,y1=_pixel_box(rect,width,height)
                authorized[y0:y1,x0:x1]=True
            unexpected=changed & ~authorized
            area=int(authorized.sum());region_fraction=round(area/(width*height),6)
            counts={'page':page_idx+1,'pagePixels':width*height,
                    'authorizedROIPixels':area,'authorizedROIPageFraction':region_fraction,
                    'rawChangedPixels':int(changed.sum()),
                    'pixelsInChangedLineROI':int((changed&authorized).sum()),
                    'unexpectedPixels':int(unexpected.sum()),
                    'residualComponents':_components(unexpected),
                    'sameDocumentRenderRepeatStable':repeat_ok,
                    'nonEditedLinesGeometryAndFontsEqual':unmodified_ok,
                    'changedTextLineCount':len(modified)}
            out['pages'].append(counts)
            out['unexplainedPixels']+=counts['unexpectedPixels']
            out['editedPixels']+=counts['pixelsInChangedLineROI']
            out['changedTextLines']+=len(modified)
            # Fail closed if layout boxes span implausibly broad portions of a page.
            all_lines_ok=all_lines_ok and unmodified_ok and repeat_ok and region_fraction<=0.05
        out['layoutPreserved']=geometry_ok and all_lines_ok
        if geometry_ok and all_lines_ok and len(edit_lines)==1 and out['unexplainedPixels']==0:
            out['classification']='VISUAL_MEASUREMENT_PREQUALIFIED_NOT_WORD_FIDELITY'
        elif not geometry_ok:out['classification']='PAGE_GEOMETRY_MISMATCH_HOLD'
        elif len(edit_lines)!=1:out['classification']='EDITED_LINE_SCOPE_INCONCLUSIVE_HOLD'
        elif out['unexplainedPixels']>0:out['classification']='UNEXPLAINED_RESIDUAL_HOLD'
        else:out['classification']='UNCHANGED_LINE_LAYOUT_INCONCLUSIVE_HOLD'
        return out
