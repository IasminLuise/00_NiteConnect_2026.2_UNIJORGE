# B05.0.1 B — diagnóstico de SAVEDATE

17/10/2026. Mesmas imagens do baseline; nenhuma correção aplicada ao motor ou
ao caminho de salvamento. Entradas/versões/PDFs/observações Word em `.runtime/b0501`.

## CX-001 — ONLYOFFICE: representação simples versus complexa

Hipótese: o defeito depende da representação OOXML do campo. Variável: somente
`fldSimple` versus a sequência `fldChar(begin)/instrText/separate/result/end`.
Controle: DOCX mínimo sintético com um parágrafo, um cabeçalho, mesma instrução
`SAVEDATE \* MERGEFORMAT`, mesmo cache, settings/styles e nenhum dado institucional.
Gerador: `scripts/compatibility_cases.py`, `field_package(False/True)`.

Protocolo: `onlyoffice_cycles.cjs` fez inserir XYZ → salvar/fechar → capturar a nova
versão → reabrir/remover XYZ → salvar/fechar → capturar outra versão. O corpo final
é exatamente o original nos dois casos. Os arquivos capturados foram abertos no
Word, com atualização local explícita de SAVEDATE/REF e exportação PDF.

| Forma de entrada | Primeiro save | Save após remover | Word das duas versões |
|---|---|---|---|
| Simples | `REF \* MERGEFORMAT` | REF | tipo 3; erro de referência |
| Complexa | SAVEDATE | SAVEDATE | tipo 22; atualização válida, sem erro |

Na variante complexa o cache persistido permaneceu igual ao original. A abertura
e atualização no Word produzem data válida. Isso prova preservação nessa fixture,
não cálculo automático de SAVEDATE no navegador nem aprovação do AV3 oficial.

Uma segunda rodada independente, com diretório de versões novo e os mesmos bytes
da fixture simples, voltou a salvar REF. O Word também confirmou o erro.

### Estágio localizado

`probe_onlyoffice_import.cjs` leu o modelo carregado **antes de inserir texto ou
solicitar save**: a instrução reconstruída já era REF (enum interno 8), enquanto
a versão imutável no host permanecia com o SAVEDATE original e sem nova versão.
O probe depende de nomes internos observados no build fixado; não é contrato
de integração nem deve ser portado por suposição a outra versão.

Portanto a perda já ocorre na fronteira de importação/representação do editor;
PutFile apenas persiste a alteração. O subestágio exato entre conversor nativo,
leitura binária e construção do objeto JavaScript **não foi isolado**.

O [parser público de instruções](https://github.com/ONLYOFFICE/sdkjs/blob/9d2138866d929be32f1d107e8b5bfae36156e097/word/Editor/Paragraph/ComplexFieldInstruction.js)
tem ramos para DATE/TIME e encaminha instruções não reconhecidas a REF, sem ramo
SAVEDATE. [ParaField.GetInstructionLine](https://github.com/ONLYOFFICE/sdkjs/blob/9d2138866d929be32f1d107e8b5bfae36156e097/word/Editor/Field.js)
reconstrói o nome por tipo. Esses pontos sustentam a hipótese de suporte incompleto;
o commit público consultado não foi identificado como o commit de build do binário
instalado. Não se apresenta um patch causal comprovado a partir dessa leitura.

Resultado: hipótese confirmada para as fixtures; modelo oficial com campo simples
continua FAIL. Ação: relato ao fornecedor com os dois controles e estado de importação.
Nenhum envio feito. Não converter automaticamente o modelo oficial para campo complexo.

## CX-002/CX-003 — CODE: redução do caso de máscara japonesa

CX-002 perguntou se styles/settings originais eram indispensáveis. Copiou-se
somente o campo para um documento mínimo, primeiro com o contexto original e
depois com contexto sintético. Ambas as cópias produziram a máscara inválida.
A hipótese de necessidade desse contexto foi refutada.

CX-003 retirou também o XML do campo original: `field_package(False)` cria do zero
um SAVEDATE simples com cache e idioma pt-BR. O CODE reproduziu a máscara
`[$-1B000411]dd\/MM'/A'd\ HH:mm:ss\ AM/PM` no primeiro save e após remover XYZ.
Os corpos finais voltaram exatamente aos originais. No Word, os campos continuam
tipo 22, mas a atualização/exportação apresenta erro de formato.

O novo caso fonte-livre reproduz o defeito observado no AV3 e pode ser gerado sem
fornecer o AV3, imagens, hyperlinks ou fontes proprietárias. É diferente da antiga
F07 baseada nos defaults do python-docx, que não reproduzia a localidade japonesa.
Nas reduções, o PDF CODE original não mostra o resultado do campo; após o primeiro
save, o PDF passa a mostrar a representação japonesa e o DOCX recebe a máscara
inválida. Portanto o caso fonte-livre reproduz a falha de serialização/Word, mas
não reproduz integralmente a visualização inicial do AV3. Essas reduções não têm
o pacote de propriedades de documento do AV3; não se atribui causalidade a essa
diferença sem outro experimento. A função nativa que seleciona a máscara não foi localizada.

Protocolo: `editor_cycles.cjs --persistent-noop true` adiciona XYZ e persiste uma
versão; fecha/reabre, remove três caracteres por UNO e persiste a segunda versão.
Difere do Undo sem save intermediário do ensaio antigo, que continua documentado
como tal. Não se alega equivalência de interação humana com o runner ONLYOFFICE.

Resultado: reprodução sintética integral da instrução inválida e do erro Word;
causa interna exata não isolada. Ação: relato fonte-livre ao fornecedor. Nenhuma
configuração ou versão corretiva foi demonstrada.

## Limite de interpretação

O inventário inclui todos os headers do pacote. CODE criou partes adicionais;
contagem de campos/partes isoladamente não prova uma nova falha de exibição.
A reprovação aqui se apoia na instrução e no erro Word, não na contagem.
As ocorrências do logotipo e as decisões do B05.0 não foram reclassificadas.
