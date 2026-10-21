"""Deterministic synthetic OOXML corpus. Standard library only; no official template."""
from pathlib import Path
import base64
import hashlib
import json
import struct
import zlib
from zipfile import ZipFile, ZipInfo, ZIP_DEFLATED

ROOT = Path(__file__).resolve().parents[1]
W = 'http://schemas.openxmlformats.org/wordprocessingml/2006/main'
R = 'http://schemas.openxmlformats.org/officeDocument/2006/relationships'

def paragraph(text, style=None):
    from xml.sax.saxutils import escape
    props = f'<w:pPr><w:pStyle w:val="{style}"/></w:pPr>' if style else ''
    return f'<w:p>{props}<w:r><w:t xml:space="preserve">{escape(text)}</w:t></w:r></w:p>'

def field(instruction):
    return f'<w:p><w:fldSimple w:instr="{instruction}"><w:r><w:t>1</w:t></w:r></w:fldSimple></w:p>'

def image_png():
    def chunk(kind, data):
        return struct.pack('!I', len(data)) + kind + data + struct.pack('!I', zlib.crc32(kind + data))
    scan = b''.join(b'\0' + bytes((30, 80, 130)) * 160 for _ in range(80))
    return b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('!2I5B', 160, 80, 8, 2, 0, 0, 0)) + chunk(b'IDAT', zlib.compress(scan)) + chunk(b'IEND', b'')

def package(body, extra=None, rels='', extra_types='', section=''):
    parts = {
        '[Content_Types].xml': f'<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Default Extension="png" ContentType="image/png"/><Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/><Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>{extra_types}</Types>',
        '_rels/.rels': f'<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="{R}/officeDocument" Target="word/document.xml"/></Relationships>',
        'word/document.xml': f'<w:document xmlns:w="{W}" xmlns:r="{R}" xmlns:m="http://schemas.openxmlformats.org/officeDocument/2006/math"><w:body>{body}<w:sectPr>{section}<w:pgSz w:w="11906" w:h="16838"/><w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440"/></w:sectPr></w:body></w:document>',
        'word/styles.xml': f'<w:styles xmlns:w="{W}"><w:docDefaults><w:rPrDefault><w:rPr><w:rFonts w:ascii="Liberation Serif" w:hAnsi="Liberation Serif"/><w:sz w:val="24"/></w:rPr></w:rPrDefault></w:docDefaults><w:style w:type="paragraph" w:default="1" w:styleId="Normal"><w:name w:val="Normal"/></w:style><w:style w:type="paragraph" w:styleId="Heading1"><w:name w:val="heading 1"/><w:basedOn w:val="Normal"/><w:pPr><w:outlineLvl w:val="0"/></w:pPr><w:rPr><w:b/><w:sz w:val="32"/></w:rPr></w:style></w:styles>',
        'word/_rels/document.xml.rels': f'<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="styles" Type="{R}/styles" Target="styles.xml"/>{rels}</Relationships>',
    }
    parts.update(extra or {})
    return parts

def write(path, parts):
    with ZipFile(path, 'w', compression=ZIP_DEFLATED) as archive:
        for name, value in sorted(parts.items()):
            entry = ZipInfo(name, (2026, 10, 17, 0, 0, 0))
            entry.compress_type = ZIP_DEFLATED
            archive.writestr(entry, value.encode('utf-8') if isinstance(value, str) else value)

def generate():
    folder = ROOT / 'fixtures' / 'generated'
    folder.mkdir(parents=True, exist_ok=True)
    fixtures = {}
    numbered = ''.join('<w:p><w:pPr><w:numPr><w:ilvl w:val="0"/><w:numId w:val="1"/></w:numPr></w:pPr><w:r><w:t>' + text + '</w:t></w:r></w:p>' for text in ('Primeiro item', 'Segundo item'))
    numbering = f'<w:numbering xmlns:w="{W}"><w:abstractNum w:abstractNumId="0"><w:lvl w:ilvl="0"><w:start w:val="1"/><w:numFmt w:val="decimal"/><w:lvlText w:val="%1."/></w:lvl></w:abstractNum><w:num w:numId="1"><w:abstractNumId w:val="0"/></w:num></w:numbering>'
    fixtures['F01'] = package(paragraph('F01 — Texto e estilos', 'Heading1') + paragraph('NITE sintético. Acentos: ação, João, contribuição.') + numbered, {'word/numbering.xml': numbering}, f'<Relationship Id="numbering" Type="{R}/numbering" Target="numbering.xml"/>', '<Override PartName="/word/numbering.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.numbering+xml"/>')
    fixtures['F02'] = package(paragraph('F02 — Modelo acadêmico SINTÉTICO', 'Heading1') + paragraph('Sem norma institucional homologada. Margens e fonte são parâmetros técnicos de teste.') + ''.join(paragraph('Parágrafo de ensaio para paginação, recuos e legibilidade. ' * 5) for _ in range(12)))
    cells = '<w:tr><w:trPr><w:tblHeader/></w:trPr><w:tc><w:tcPr><w:gridSpan w:val="2"/></w:tcPr>' + paragraph('Cabeçalho mesclado e repetido') + '</w:tc></w:tr>'
    cells += ''.join('<w:tr><w:tc>' + paragraph(f'Linha {i}') + '</w:tc><w:tc>' + paragraph('Conteúdo sintético de tabela ' * 4) + '</w:tc></w:tr>' for i in range(70))
    fixtures['F03'] = package(paragraph('F03 — Tabela longa', 'Heading1') + '<w:tbl><w:tblPr><w:tblW w:w="9000" w:type="dxa"/></w:tblPr><w:tblGrid><w:gridCol w:w="3000"/><w:gridCol w:w="6000"/></w:tblGrid>' + cells + '</w:tbl>')
    drawing = '''<w:p><w:r><w:drawing><wp:inline xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing"><wp:extent cx="1828800" cy="914400"/><wp:docPr id="1" name="Imagem sintética"/><a:graphic xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"><a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/picture"><pic:pic xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture"><pic:nvPicPr><pic:cNvPr id="0" name="fixture.png"/><pic:cNvPicPr/></pic:nvPicPr><pic:blipFill><a:blip r:embed="image"/><a:stretch><a:fillRect/></a:stretch></pic:blipFill><pic:spPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="1828800" cy="914400"/></a:xfrm><a:prstGeom prst="rect"><a:avLst/></a:prstGeom></pic:spPr></pic:pic></a:graphicData></a:graphic></wp:inline></w:drawing></w:r></w:p>'''
    anchored = drawing.replace('<wp:inline ', '<wp:anchor distT="0" distB="0" distL="0" distR="0" simplePos="0" relativeHeight="1" behindDoc="0" locked="0" layoutInCell="1" allowOverlap="1" ').replace('<wp:extent', '<wp:simplePos x="0" y="0"/><wp:positionH relativeFrom="column"><wp:posOffset>0</wp:posOffset></wp:positionH><wp:positionV relativeFrom="paragraph"><wp:posOffset>0</wp:posOffset></wp:positionV><wp:extent').replace('<wp:docPr id="1"', '<wp:wrapSquare wrapText="bothSides"/><wp:docPr id="2"').replace('</wp:inline>', '</wp:anchor>')
    fixtures['F04'] = package(paragraph('F04 — Imagens inline e ancorada', 'Heading1') + drawing + paragraph('Figura 1 — retângulo inline sintético.') + anchored + paragraph('Figura 2 — retângulo ancorado sintético.') + paragraph('Texto ao redor da figura ancorada. ' * 15), {'word/media/fixture.png': image_png()}, f'<Relationship Id="image" Type="{R}/image" Target="media/fixture.png"/>')
    fixtures['F05'] = package(paragraph('F05 — Cabeçalho, rodapé e campos', 'Heading1') + paragraph('Conteúdo principal.'), {'word/header1.xml': f'<w:hdr xmlns:w="{W}">{paragraph("Cabeçalho sintético")}</w:hdr>', 'word/footer1.xml': f'<w:ftr xmlns:w="{W}">{field("PAGE")}</w:ftr>'}, f'<Relationship Id="header" Type="{R}/header" Target="header1.xml"/><Relationship Id="footer" Type="{R}/footer" Target="footer1.xml"/>', '<Override PartName="/word/header1.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.header+xml"/><Override PartName="/word/footer1.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.footer+xml"/>', '<w:headerReference w:type="default" r:id="header"/><w:footerReference w:type="default" r:id="footer"/>')
    fixtures['F06'] = package(paragraph('F06 — Sumário e referências', 'Heading1') + field('TOC \\o &quot;1-3&quot;') + paragraph('Seção de referência', 'Heading1') + '<w:p><w:bookmarkStart w:id="1" w:name="destino"/><w:r><w:t>Alvo sintético</w:t></w:r><w:bookmarkEnd w:id="1"/></w:p>' + field('REF destino'))
    fixtures['F06']['word/document.xml'] = fixtures['F06']['word/document.xml'].replace('<w:sectPr>', '<w:p><w:r><w:t>Texto com nota</w:t></w:r><w:r><w:footnoteReference w:id="1"/></w:r></w:p><w:sectPr>')
    fixtures['F06']['word/footnotes.xml'] = f'<w:footnotes xmlns:w="{W}"><w:footnote w:type="separator" w:id="-1"><w:p><w:r><w:separator/></w:r></w:p></w:footnote><w:footnote w:type="continuationSeparator" w:id="0"><w:p><w:r><w:continuationSeparator/></w:r></w:p></w:footnote><w:footnote w:id="1">{paragraph("Nota de rodapé sintética.")}</w:footnote></w:footnotes>'
    fixtures['F06']['word/_rels/document.xml.rels'] = fixtures['F06']['word/_rels/document.xml.rels'].replace('</Relationships>', f'<Relationship Id="footnotes" Type="{R}/footnotes" Target="footnotes.xml"/></Relationships>')
    fixtures['F06']['[Content_Types].xml'] = fixtures['F06']['[Content_Types].xml'].replace('</Types>', '<Override PartName="/word/footnotes.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.footnotes+xml"/></Types>')
    tracked = '<w:p><w:ins w:id="1" w:author="Pessoa Sintética A" w:date="2026-10-17T00:00:00Z"><w:r><w:t>Inserção controlada</w:t></w:r></w:ins><w:del w:id="2" w:author="Pessoa Sintética B" w:date="2026-10-17T00:00:00Z"><w:r><w:delText>Exclusão controlada</w:delText></w:r></w:del></w:p>'
    comment = '<w:p><w:commentRangeStart w:id="0"/><w:r><w:t>Texto comentado</w:t></w:r><w:commentRangeEnd w:id="0"/><w:r><w:commentReference w:id="0"/></w:r></w:p>'
    fixtures['F07'] = package(paragraph('F07 — Revisões e comentário', 'Heading1') + tracked + comment, {'word/comments.xml': f'<w:comments xmlns:w="{W}"><w:comment w:id="0" w:author="Pessoa Sintética A" w:date="2026-10-17T00:00:00Z">{paragraph("Comentário sintético preservável.")}</w:comment></w:comments>'}, f'<Relationship Id="comments" Type="{R}/comments" Target="comments.xml"/>', '<Override PartName="/word/comments.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.comments+xml"/>')
    fixtures['F08'] = package(paragraph('F08 — Símbolos e equação', 'Heading1') + '<w:p><w:r><w:rPr><w:rFonts w:ascii="DejaVu Sans" w:hAnsi="DejaVu Sans"/></w:rPr><w:t>α β γ ∑ √ ∞ → ação</w:t></w:r></w:p><w:p><m:oMath><m:f><m:num><m:r><m:t>1</m:t></m:r></m:num><m:den><m:r><m:t>2</m:t></m:r></m:den></m:f></m:oMath></w:p>')
    fixtures['F09'] = package(paragraph('F09 — Documento longo', 'Heading1') + ''.join(paragraph(f'Página sintética {i}') + paragraph('Teste de estabilidade de conteúdo. ' * 25) + ('<w:p><w:r><w:br w:type="page"/></w:r></w:p>' if i < 40 else '') for i in range(1, 41)))
    manifest = []
    for code, parts in fixtures.items():
        path = folder / (code + '.docx')
        write(path, parts)
        manifest.append({'id': code, 'file': path.name, 'sha256': hashlib.sha256(path.read_bytes()).hexdigest(), 'bytes': path.stat().st_size, 'origin': 'synthetic-generator-v1', 'editorial_approved': False})
    (folder / 'F10.docx').write_bytes(b'This is not a DOCX file. Synthetic invalid input.')
    (folder / 'manifest.json').write_text(json.dumps(manifest, indent=2) + '\n', encoding='utf-8')
    print(f'{len(manifest)} fixtures válidas e F10 inválida geradas. Nenhum modelo oficial ou baseline Word.')

if __name__ == '__main__':
    generate()
