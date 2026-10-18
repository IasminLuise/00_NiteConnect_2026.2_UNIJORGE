# Equivalências limitadas de comparação B05.0.4

O gate recebido confundia algumas mudanças de serialização com perda de conteúdo.
A saída dos SDKs não é gabarito. A investigação utilizou reprodução sintética
independente, controles Word, fonte normativa e testes negativos. Nenhuma regra
altera os bytes dos DOCX; hashes das candidatas continuam correspondendo ao export.

## Default e XML

O default exige textos/propriedades, campos, mídias, partes, relações, tabelas e
estrutura. Prefixos de namespace e ordem de atributos não são propriedades do
documento: o inventário já usava nomes XML expandidos. O fingerprint das partes
protegidas agora usa a mesma árvore, evitando rejeitar `numbering.xml` cuja árvore
é idêntica apenas porque seus bytes foram reserializados. Nenhum elemento ou
atributo de layout é removido nessa comparação.

## Opt-in de identificadores

`--allow-paragraph-id-additions` permite somente adição de `w14:paraId` em
parágrafos que não tinham ID. Valores devem ter oito dígitos hexadecimais, ser
maiores que zero, menores que `0x80000000` e únicos por parte. IDs existentes não
são remapeados, excluídos ou ignorados. Havendo atributos de referência a IDs
fora do próprio `w:p/@w14:paraId`, a adição não é ignorada. IDs em outras
estruturas também permanecem estritos. Duplicatas, intervalos inválidos, IDs
existentes alterados e referências têm regressões negativas.

Fonte primária: [MS-DOCX 2.6.2.3](https://learn.microsoft.com/en-us/openspecs/office_standards/ms-docx/a0e7d2e2-2246-44c6-96e8-1cf009823615).
Esta norma define identidade, não uma garantia universal de equivalência.
Os controles Word desta fixture também preservaram campos e métricas.

## Opt-in de runs simples

`--allow-plain-text-run-segmentation` trata, somente no corpo, runs adjacentes de
texto simples sem atributos nem formatação direta. Não cruza campos, bookmarks,
hyperlinks, revisões ou outros elementos. Não ignora `rPr`, `xml:space`, texto
com bordas de whitespace, quebras ou tabs. Um run inteiramente vazio e sem
atributos pode desaparecer na árvore comparada. Adicionar XYZ em um novo run
sem formatação e removê-lo deixando um run vazio foi reproduzido no editor e no
Word; fonte, tamanho, métricas e campos permaneceram iguais. Formatação nova
continua causando FAIL.

Fonte primária: [Runs em WordprocessingML](https://learn.microsoft.com/en-us/office/open-xml/word/working-with-runs).
O teste positivo é fonte-livre, produzido independentemente dos SDKs; não armazena
um XML exportado como whitelist.

## Limites que não foram dispensados

Não há equivalência para remover `footnotes.xml`, `endnotes.xml`, configurações
de notas ou suas relações. Isso ocorreu em C0 oficial SuperDoc e continua FAIL,
mesmo com texto/mídia preservados. Não se conclui que notas reais foram perdidas.
Não há exceção para água de trial, novas mídias, reescrita de estilos/campos ou
outras alterações Apryse. Word e inspeção visual são obrigatórios além do gate.
