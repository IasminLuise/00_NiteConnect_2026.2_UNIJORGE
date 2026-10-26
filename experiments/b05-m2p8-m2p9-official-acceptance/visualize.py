"""Review all page pixels without masks; produces private side-by-side diff evidence."""
from __future__ import annotations
import argparse
import json
from pathlib import Path
from b05_acceptance.core import safe_file, checked_file


def compare(original: Path, candidate: Path, dest: Path, dpi: int = 144):
    import fitz
    from PIL import Image, ImageChops
    pairs = []
    with fitz.open(original) as left, fitz.open(candidate) as right:
        for n in range(max(len(left), len(right))):
            if n >= len(left) or n >= len(right):
                pairs.append({'page': n + 1, 'comparison': 'PAGE_MISSING'})
                continue
            pix1=left[n].get_pixmap(matrix=fitz.Matrix(dpi/72,dpi/72),colorspace=fitz.csRGB,alpha=False)
            pix2=right[n].get_pixmap(matrix=fitz.Matrix(dpi/72,dpi/72),colorspace=fitz.csRGB,alpha=False)
            l=Image.frombytes('RGB', (pix1.width,pix1.height), pix1.samples)
            r=Image.frombytes('RGB', (pix2.width,pix2.height), pix2.samples)
            if l.size != r.size:
                pairs.append({'page':n+1,'comparison':'PAGE_GEOMETRY_CHANGED'})
                continue
            # Difference not normalized; retain RGB magnitude, with visual alpha increase only in review image.
            raw=ImageChops.difference(l,r)
            raw.save(dest / f'page-{n+1:03d}-raw-diff.png')
            layout=Image.new('RGB',(l.width*3,l.height),'white')
            layout.paste(l,(0,0));layout.paste(r,(l.width,0));layout.paste(raw,(2*l.width,0))
            layout.save(dest / f'page-{n+1:03d}-reference-candidate-diff.png')
            pairs.append({'page': n+1, 'comparison':'ALL_PIXELS_RETAINED', 'size_px': list(l.size)})
    return pairs


def main():
    ap=argparse.ArgumentParser()
    ap.add_argument('--manifest',required=True,type=Path)
    ap.add_argument('--out-dir',required=True,type=Path)
    args=ap.parse_args(); root=args.manifest.resolve().parent
    out=args.out_dir.resolve()
    if out.exists() or not out.is_relative_to(root):
        ap.error('Diretório de saída deve ser novo e contido na campanha privada')
    raw=json.loads(args.manifest.read_text(encoding='utf-8'))
    tasks=[]
    for doc in raw['documents']:
        ref=doc.get('word_reference_pdf')
        if not ref: continue
        refpath=checked_file(root,ref,'.pdf',{})
        for case in doc['scenarios']:
            for key in ('engine_pdf','callback_pdf','reopened_pdf'):
                if case.get(key):
                    other=checked_file(root,case[key],'.pdf',{})
                    tasks.append((doc['id'],case['name'],key,refpath,other))
    if not tasks: ap.error('Nenhum par PDF real disponível; não gerar provas inventadas')
    out.mkdir(parents=True)
    log=[]
    for doc,case,key,a,b in tasks:
        here=out/doc/case/key;here.mkdir(parents=True)
        pages=compare(a,b,here)
        log.append({'document':doc,'scenario':case,'stage':key,'pages':pages})
    (out/'index.json').write_text(json.dumps(log,indent=2,ensure_ascii=False),encoding='utf-8')
    print(len(tasks),'pares renderizados, todas páginas; evidências privadas em',out)

if __name__=='__main__': main()
