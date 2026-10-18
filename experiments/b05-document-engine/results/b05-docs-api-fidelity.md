# B05.0.3 — fidelidade e motivo da parada

**FAIL sintético em B2; modelos oficiais NOT-RUN.** F01 conserva o texto
esperado, mas seu layout no Word muda. Não é adequado aprovar todos os deltas
como reserialização equivalente.

Foram comparados a origem, o DOCX persistido pelo controle, a candidata estrita
retida e uma cópia **independente no Word com o mesmo `XYZ` no início**. Inventário
confirmou igualdade de texto por parágrafo entre controle Word e candidata.
Word 16.0, build 16.0.17932 abriu e repaginou todos em uma página. PDFs foram
exportados pelo Word a partir desses arquivos, renderizados e inspecionados.
Uma página em ambos não significa layout igual.

| Medida efetiva no Word | Controle Word XYZ | Candidata do motor XYZ |
|---|---:|---:|
| SpaceAfter dos quatro parágrafos | 8 pt | 0 pt |
| LineSpacingRule | 5 | 0 |
| LineSpacing | aproximadamente 13,9 pt | 12 pt |
| Posição vertical do parágrafo 2 | 101,25 pt | 90,75 pt |
| Posição vertical do parágrafo 4 | 149,25 pt | 117,75 pt |
| Páginas | 1 | 1 |

A redução dos espaços é visível nos PNGs; não é diferença de rasterização.
A entrada contém dois estilos e a saída 190. Há mudanças/adicionamentos em
`document`, `styles`, `settings`, `numbering`, relações, tema, fontes, notas e
metadados. Contagem de estilos **não** é critério de corrupção; a mudança nas
métricas efetivas impede afirmar equivalência global. Não foi isolada a causa
interna exata do parser/exportador do fornecedor. Não foi criado reparador nem
whitelist de saída. A própria F01 pequena, source-free, serve de reprodução
focal de quatro parágrafos; não se iniciou nova série de variantes.

`docs_api_word_control.ps1` produz o controle em cópia; `word_layout_probe.ps1`
agora registra espaçamento e posição além de páginas/fontes. Evidência privada:
`analysis/word-XYZ-control.docx`, `word-XYZ-metrics.json`, `candidate-metrics.json`,
PDFs/PNGs, inventários e XMLs antes/depois. Não extrapolar esse resultado para
alegar corrupção dos documentos oficiais que não foram ensaiados.

SAVEDATE simples/complexo, atualização tipo 22, AV3, sete páginas do relatório,
C0/C1/C2 e duas rodadas oficiais: **NOT-RUN/BLOCKED pelo Gate B**. A descoberta
histórica do campo complexo continua histórica; não foi homologada pela Docs
API. Originais e evidências anteriores preservados. Não há porcentagem de
fidelidade, aceite institucional ou aprovação G1-F.
