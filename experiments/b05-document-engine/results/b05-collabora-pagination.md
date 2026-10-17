# B05.0.1 C — paginação e tamanho efetivo de fonte

17/10/2026. CODE 26.04.5.1.1 fixado. Nenhuma fonte foi montada novamente e não
se repetiu a restauração de tblPr/tblGrid. Apenas cópias diagnósticas foram alteradas.

## Fronteira de leitura

O PDF Word atual do mesmo DOCX original mantém sete páginas; o PDF CODE do
checkpoint original mantém seis. Uma âncora textual única do parágrafo OOXML 37
está na página 2 no Word e na página 1 no CODE. A inspeção da parte inferior das
duas primeiras páginas confirma que o CODE acomoda mais uma linha da tabela na
primeira página. O método de âncoras remove whitespace apenas para localização;
não é um comparador de integridade nem prova de igualdade nas áreas anteriores.
Os textos e as imagens comparativas ficam privados.

## CX-004 — fonte regravada explica a paginação no Word?

Hipótese: tamanhos materializados como 10 pt pelo CODE reduzem a altura do relatório.
Variável: somente os valores 20→24 (meios pontos) em `w:sz`/`w:szCs` do
`word/document.xml` **salvo pelo CODE**. Controle: mesmo arquivo, corpo, tabelas,
imagens, styles/settings, mesmo Word e opções de exportação.

O XML original contém tamanhos explícitos 22/28 em alguns trechos e Normal de 24.
O salvo introduz tamanho 20 em muitos trechos. O Word confirmou a diferença efetiva:

| Fonte de dados Word | 12 pt | 10 pt | 11 pt | 14 pt | Mistos | Páginas |
|---|---:|---:|---:|---:|---:|---:|
| Original | 168 | 0 | 40 | 4 | 4 | 7 |
| Salvo CODE, sem edição líquida | 28 | 140 | 40 | 4 | 4 | 6 |
| Cópia diagnóstica: 304 propriedades 20→24 | 168 | 0 | 40 | 4 | 4 | 7 |

As contagens são de parágrafos retornados pelo COM Word, incluindo estruturas de
células; não devem ser confundidas com os 190 parágrafos XML do corpo. A primeira
diferença de fronteira observada nessa comparação Word é no parágrafo COM 78:
original começa na página 2 e termina na 3; salvo começa/termina na 2, com 10 pt.

Na cópia diagnóstica, as páginas de início/fim de **todos os parágrafos COM** voltaram
a coincidir com o original. Isso demonstra impacto causal dessas propriedades
na paginação persistida desta amostra. Não comprova igualdade de medidas, bordas,
âncoras, pixels ou correção do motor. Os espaços finais perdidos continuam perdidos.
Não usar a cópia diagnóstica como modelo nem inseri-la no caminho de salvamento.

## CX-005 — reprodução sintética e controle de docDefaults

Uma tabela sintética sem estilos institucionais, com Normal 12 pt e sem tamanhos
diretos nos runs, produziu: Word original 2 páginas; CODE original 1; DOCX após
inserir/salvar/remover/salvar 1 página também no Word, com tamanhos 20 materializados.
O corpo voltou exatamente ao original. Há reprodução independente de conteúdo privado.
Na tabela mínima, espaçamentos e demais propriedades também são regravados; seu
impacto isolado não foi medido. A atribuição causal somente ao tamanho foi
demonstrada na cópia diagnóstica do relatório, não generalizada à tabela sintética.

A hipótese adicional foi que a falta de `docDefaults/rPrDefault/rPr/sz` causaria
a escolha de 10 pt. Inseriu-se **apenas** esse default, valor 24, numa cópia da mesma
tabela. Word continuou com 2 páginas; CODE original/salvo continuou com 1.
Hipótese refutada. A ausência desse default não explica sozinha o defeito.

O problema ficou delimitado à resolução/materialização de tamanho herdado no caso
de tabela, com impacto na leitura e no round-trip. O ponto exato no importador ou
na cascata de estilos de tabela permanece não isolado. Nenhuma correção suportada
foi encontrada; não se atribui a causa exclusivamente a fontes ausentes.

## Ponto de parada

Duas hipóteses de paginação avaliadas; não ampliar tentativas sem nova evidência
ou decisão escrita. Relato preparado com a tabela sintética e o controle de
docDefaults. O modelo oficial permanece intacto, CODE permanece NO-GO, aceite
editorial pendente. Evidências em `.runtime/b0501/word`, `code-run1`,
`code-font-control` e `reading-boundaries.json`.
