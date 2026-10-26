# B05-M2P2 — ciclo real e fidelidade da fixture rica

**Campanha completa executada em 23/10/2026, America/Bahia. Resultado editorial:
`HOLD_QUARANTINE`.** O runtime real integrou, persistiu a candidata e preservou o
veto do produto. Word e PDFs confirmaram duas páginas, mas a candidata contém uma
alteração de formatação demonstrada e diferenças que não autorizam publicação.

Este relatório complementa a [primeira execução offline](windows-2026-10-23.md),
preservada com seus resultados e pendências daquele momento. Nenhum relatório antigo,
DOCX ou PDF foi sobrescrito.

## Execução e preservação

- Branch `feat/b05-core-docs-api`, base `017281a1830724cd66eee6ee505a704e9fa37b4d`, com incrementos M2P/M2P2 locais.
- Uma fixture inteiramente fictícia, um ciclo de edição humana e um novo projeto/banco descartável, sem reutilizar IDs ou volumes da rodada M2R anterior.
- Microsoft Entra real: assinatura, issuer, audience, validade e scope verificados; autorização local `/me` respondeu 200. Token somente em memória, sem `.env` alterado.
- Cloudflare Quick Tunnel autorizado explicitamente para esta rodada. Proxy expôs apenas healthcheck, fontes com capability e callbacks com JWT. TLS validado de dentro do contêiner sem ignorar certificados.
- ONLYOFFICE `documentserver:10.0.0.1`; Word Desktop `16.0`, build `16.0.17932`.
- Python `3.13.6`, `lxml==6.1.1`, `PyMuPDF==1.26.7`, `python-docx==1.2.0`, `numpy==2.5.4`; dependências apenas da instrumentação privada.
- Objetos reais de origem/candidata copiados do armazenamento imutável após conferir tamanhos e hashes contra os metadados do banco descartável. A origem coincide byte a byte com a fixture provisionada.
- Word abriu os dois DOCX em leitura; hashes antes/depois iguais. Não houve reconstrução de candidata, normalização, salvamento de DOCX ou promoção forçada.

## Integração real

O editor indicou `onDocumentReady`, exibiu duas páginas e recebeu a edição humana.
O operador confirmou o botão de encerramento. Callback autenticado persistiu exatamente
uma nova versão **`QUARANTINED`**, com `OOXML_PROTECTED_CHANGE`; a origem permaneceu
`AVAILABLE`, sem mudança de head.

Passaram os controles observados: acesso anônimo negado, capability adulterada negada,
callback sem assinatura negado, sessão assinada, ausência de erro do editor, fonte
válida antes de revogar e negada depois. O PDF de produto tinha magic `%PDF-`, cabeçalho
da versão e bytes repetidos iguais.

O resultado do executor foi `M2R_INTEGRATION_DEMONSTRATED_NOT_EDITORIAL_APPROVAL`,
com `QUARANTINED_GUARD_ENFORCED`. O PDF de produto refere-se à versão **disponível de
origem**, não à candidata em quarentena; a repetição pode usar cache e não equivale
a duas conversões independentes. A comparação editorial abaixo usa PDFs Word do par real.

## Paginação, conteúdo e campos

| Verificação | Origem / candidata | Resultado |
|---|---|---|
| Páginas Word e PDF | 2 / 2 | Paginação preservada nesta fixture |
| Papel | Letter, 612 × 792 pt nas duas páginas | Igual |
| Parágrafos OOXML / Word | 25 / 25 e 28 / 28 | Enumerações distintas, contagens preservadas |
| Texto visível do corpo | Somente a troca esperada no parágrafo 2 | Edição conferida integralmente |
| Tabela 3×3 | Nove textos de célula iguais | Conteúdo preservado |
| Nota de usuário | Uma referência em ambos, texto igual | Sem perda textual de nota demonstrada |
| Imagem sintética | Mídia sem alteração de bytes | Preservada |
| Campos Word | `SAVEDATE` tipo 22, `PAGE` tipo 33, `NUMPAGES` tipo 26 em ambos | Não houve conversão para `REF` |
| Métricas de corpo/páginas antes/depois de gerar PDF | Sem alteração medida em ambos | Estado posterior ao PDF também registrado |

O alerta bruto `UNEXPECTED_TEXT_CHANGE` inclui dois parágrafos: a edição esperada e
a quebra explícita no índice 13. No segundo caso, `w:br/@w:type="page"` foi mantido e
foi acrescentado **`w:clear="all"`**. O instrumento inclui atributos de controles na
assinatura textual. Não foi encontrado outro texto visível inesperado; esse alerta
não deve ser descrito como erro de digitação do operador. O atributo continua uma
transformação protegida, sem regra de equivalência habilitada.

O probe percorreu `StoryRanges`/`NextStoryRange`, inventariou instrução, tipo e resultado
antes da atualização explícita, depois dela e após gerar PDF. A atualização explícita
não mudou os resultados nesta execução. O `SaveAs2` mudou o resultado apresentado de
`SAVEDATE` em ambos; `PAGE` e `NUMPAGES` não mudaram nessas etapas. Os valores exatos
permanecem nos inventários privados.

Diferenças de instrução observadas:

- `SAVEDATE` e `NUMPAGES`: espaços externos acrescentados ao redor da mesma instrução com `\* MERGEFORMAT`;
- `PAGE`: passou de `PAGE` para **`PAGE \* MERGEFORMAT`**.

O diagnóstico `FIELD_SEMANTICS_DIFFER` não prova perda de campos: os três tipos e nomes
permaneceram. A instrução de formatação adicional continua diferente e sem exceção
autorizada. Igualdade dos resultados **após** salvar PDF também não prova igualdade
das datas de gravação originais: o próprio procedimento Word atualizou `SAVEDATE`.

## Bloqueio da exportação Word e correção do probe

`ExportAsFixedFormat` ficou sem retornar depois da repaginação e do inventário;
não se observou diálogo de erro. Desabilitar atualização de campos na impressão não
resolveu o controle. A causa interna da chamada continua não isolada.

O caminho alternativo **`SaveAs2` em formato PDF**, com argumentos COM `[object]`/`[ref]`,
concluiu. A primeira tentativa desse caminho expôs erro de marshaling de `psobject`;
a tipagem explícita resolveu esse erro. O método gerou os dois PDFs de duas páginas,
com os DOCX originais inalterados. A mudança é opt-in (`-PdfMethod SaveAs2`), mantendo
o método original como padrão. Não é uma correção ou regravação do pacote DOCX.

Uma revisão independente apontou que as métricas anteriores poderiam pertencer ao
estado anterior ao PDF. O probe passou a registrar corpo/páginas antes e depois,
repaginar após o PDF e sinalizar `layoutChangedDuringPdf`. A síntese bloqueia uma
divergência conhecida. O teste de regressão falhou antes da correção e passou depois.
Essas métricas continuam parciais: não certificam cada run ou todos os recursos Word.

## Fidelidade visual a 144 dpi

As quatro páginas renderizadas foram inspecionadas. Nenhuma região foi excluída da
contagem. Os PNGs e uma nova comparação direta dos PDFs produziram as mesmas contagens.
A repetição de renderização do mesmo PDF foi estável; não é uma nova exportação Word.

| Local | Pixels diferentes | Evidência / classificação |
|---|---:|---|
| Página 1, linha do marcador | 4.727 | Mistura da troca textual esperada com alteração real de fonte, peso, tamanho e cor |
| Página 1, célula 2.3 | 109 | Texto e fonte iguais; deslocamento de origem de glifos até 0,016327 pt |
| Página 2, quinto parágrafo de validação | 1.343 | Texto e fonte iguais; deslocamento de origem de glifos até 0,075867 pt |
| Total | **6.179** | 4.836 na página 1 e 1.343 na página 2 |

Os 1.452 pixels fora da região automática da edição são integralmente mantidos.
O deslocamento interno de glifos foi medido, mas a propriedade causal não foi isolada;
não se presumiu ruído de antialiasing nem equivalência funcional por uma tolerância.
A métrica de âncoras de parágrafo, com tolerância de 0,75 pt, não substitui essa análise.

**Regressão de formatação demonstrada no documento:** o run do marcador passou da
fonte herdada Cambria preta (PDF aproximadamente 11,04 pt) para `w:rFonts=Arial`,
`w:b`, `w:color/@w:val="fcfcfc"` e `w:sz/@w:val="20"` (10 pt). O PDF confirma Arial
Bold aproximadamente 9,96 pt, quase branco, reduzindo fortemente a legibilidade.
Os 4.727 pixels dessa linha não podem ser classificados como somente a edição esperada.

A sessão não registrou o modo de inserção/colagem do operador. Portanto, o resultado
demonstra a alteração da candidata, **não** que o motor sozinho necessariamente a
produziu. A regressão é real independentemente de sua origem; essa rodada não é uma
prova isolada da fidelidade do motor sob entrada exclusivamente textual.

## Classificação estrutural e decisão

21 partes na origem, 25 na candidata: seis adicionadas, duas removidas e 18 comuns
modificadas. Entre as adicionadas há relações de cabeçalho/rodapé/notas e endnotes;
foram removidas thumbnail e `stylesWithEffects.xml`. As 152 renomeações de IDs de
estilos não bastaram para demonstrar equivalência das definições; o instrumento
reportou 320 assinaturas diferentes ou ausentes, não 320 regressões comprovadas.

Há nota de usuário preservada **e** nova infraestrutura de endnotes com separadores.
`NEW_EMPTY_NOTES_NOT_APPROVED` descreve a infraestrutura nova, não ausência de toda nota
no documento. A comparação de referências resolvidas foi diferente; não foram
enumerados riscos de tipos OPC/relações pelo instrumento, mas isso não autoriza as
transformações nem constitui validação OPC exaustiva.

Decisão shadow **`HOLD_QUARANTINE`**: formatação funcionalmente alterada, transformações
OOXML sem equivalência demonstrada, diferenças de instrução e resíduos visuais mantidos.
`canPromote=false`, `officialApproved=false`; G1/G2/M3 bloqueados. Não foi criada regra
de publicação, allowlist, exceção de estilo ou normalização corretiva. O bloqueio
editorial desta rodada não é falha de acesso, TLS, callback ou automação.

## Validação, evidências e encerramento

- M2P: `python -m unittest discover -s experiments/b05-m2p-editorial-fidelity/tests -v` — **26 aprovados** após a correção final.
- M2P2: `python -m unittest discover -s experiments/b05-m2p2-qualification/tests -v` — **27 aprovados** nesta continuação; resultados permanecem válidos, pois seus scripts não mudaram depois dessa execução.
- `python -m py_compile` nos scripts M2P/M2P2 e parser PowerShell do probe — aprovados.
- Execução real do probe sobre origem/candidata com `-PdfMethod SaveAs2 -UpdateFields` — concluída, dois hashes inalterados, duas páginas em cada PDF.
- OOXML, `fidelity_report.py` e `qualify.py` executados sobre o par real; resultados brutos preservados, sem trocar a expectativa para forçar PASS.
- `git status --short` e `git diff --check` — inspecionados após as alterações finais.
- Java/API/gate histórico não alterados; suas suítes não foram repetidas. A API real e V1–V8 foram utilizadas somente na nova base descartável. Isso não é uma nova execução das suítes Java anteriores.

Evidências privadas em `experiments/b05-m2p2-qualification/.private/runtime-01/`
(callback, armazenamento, manifesto), `rich-evaluation-01/` (par, Word, campos, PDFs,
rasters, diferenças e localizações) e controles Word anteriores preservados. Os
resultados compartilháveis anexos não contêm tokens, IDs de documento, hashes privados,
caminhos absolutos ou conteúdo de parágrafos.

Túnel, API, servidor de login e serviços deste projeto descartável encerrados; portas
locais de campanha verificadas fechadas. Volumes e evidências foram mantidos. Nenhum
serviço de produção, modelo oficial ou worktree de auditoria foi alterado. Alterações
continuam locais, sem commit, merge ou deploy.
