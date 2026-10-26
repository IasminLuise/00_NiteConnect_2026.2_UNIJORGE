# Corpus sintético B05

Gerar com `python scripts/generate_fixtures.py` a partir do diretório do experimento.
Somente biblioteca padrão Python 3.10+. Saída ignorada em `generated/`.
ZIPs têm timestamp fixo e ordem estável; manifest.json registra SHA-256 e origem.

F01: texto/estilos/lista numerada OOXML; F02: composição acadêmica sintética, sem norma homologada;
F03: tabela longa/mesclada/cabeçalho repetido; F04: imagem inline/ancorada/legenda;
F05: cabeçalho/rodapé/campo de página; F06: sumário/referência/nota de rodapé;
F07: alterações controladas/comentário; F08: fontes livres/símbolos/OMML;
F09: quarenta páginas planejadas; F10: extensão falsa.

Limite conhecido: F05 ainda não cobre múltiplas seções/páginas especiais.
Completar esse cenário antes de marcar P06/P07 aprovados.
O teste Java gera separadamente pacote com expansão excessiva, sem gravá-lo no Git.

Não são modelos institucionais, baseline Microsoft Word, nem aceite editorial.
Modelos reais anonimizados/autorizados e critérios editoriais continuam pendentes.
Não adicionar arquivos privados. A política do host recusa macros, objetos binários
embutidos e relações externas; é uma restrição técnica da spike, não política do produto.
