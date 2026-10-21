"""Optional visual evidence: pypdfium2 + Pillow (available in Codex runtime)."""
from pathlib import Path
import json
import pypdfium2 as pdfium
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1] / 'results' / 'generated'

def render():
    folder = ROOT / 'rendered'
    folder.mkdir(exist_ok=True)
    report = []
    conversions = json.loads((ROOT / 'conversions.json').read_text(encoding='utf-8'))
    for conversion in conversions:
        if any(conversion[engine]['status'] != 'converted' for engine in ('collabora','libreoffice')):
            continue
        code = conversion['fixture'].removesuffix('.docx')
        first = ROOT / conversion['collabora'].get('file', 'collabora/' + code + '.pdf')
        second = ROOT / conversion['libreoffice'].get('file', 'libreoffice/compare-' + first.name)
        if not second.exists():
            continue
        docs = [pdfium.PdfDocument(first), pdfium.PdfDocument(second)]
        texts = []
        for doc in docs:
            texts.append('\n'.join(doc[i].get_textpage().get_text_range() for i in range(len(doc))))
        row = {'fixture': code, 'source_version': conversion['source_version'], 'pages_collabora': len(docs[0]), 'pages_libreoffice': len(docs[1]), 'same_extracted_text': ''.join(texts[0].split()) == ''.join(texts[1].split()), 'editorial_acceptance': 'pending'}
        report.append(row)
        for n in range(max(map(len, docs))):
            canvas = Image.new('RGB', (1240, 930), '#dddddd')
            draw = ImageDraw.Draw(canvas)
            for column, doc in enumerate(docs):
                draw.text((column * 620 + 10, 8), f'{code} | {"Collabora" if column == 0 else "LibreOffice"} | pagina {n+1}', fill='black')
                if n < len(doc):
                    image = doc[n].render(scale=1).to_pil().convert('RGB')
                    image.thumbnail((610, 890))
                    canvas.paste(image, (column * 620, 30))
            canvas.save(folder / f'{code}-{n+1:03}.png')
        for doc in docs:
            doc.close()
        print(row)
    (ROOT / 'visual-comparison.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')

if __name__ == '__main__':
    render()
