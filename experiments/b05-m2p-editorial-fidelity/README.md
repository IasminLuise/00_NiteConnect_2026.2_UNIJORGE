# B05-M2P — Fidedignidade editorial e admissibilidade de publicação

**Estado: instrumento de pré-qualificação implementado, não homologação editorial.**

Este incremento parte do PR #17 (`feat/b05-core-docs-api`) **sem modificar o backend, a V8, o `DocxIntegrity`, a integração ONLYOFFICE, o gate Python histórico ou os modelos oficiais**. Ele utiliza os bytes reais `source.docx` e `candidate-quarantined.docx` já preservados privadamente no M2R, para determinar por que a candidata não pode ser promovida e medir as diferenças no Word Desktop. Não é necessário fazer outra sessão de edição para repetir esse diagnóstico.

## Decisão de engenharia

- Fonte de verdade: DOCX de origem e candidato salvos pelo produto; Word Desktop, executado read-only, para paginação; arquivos PDF exportados pelo Word para comparação visual.
- Um OOXML com texto certo, mas com estilos, campos, partes, relações ou configurações alteradas, **não é equivalente por presunção**. Mudanças estruturais podem ser legítimas, mas precisam de autorização e prova por categoria.
- Esta ferramenta **não** promove versões e não abre exceções no guard. Relatórios positivos são pré-qualificações, não provas suficientes para aceitar todo DOCX.
- A candidata do M2R já mostrou alterações em 5 partes adicionadas, 2 removidas, 15 comuns modificadas. Isso implica `QUARANTINE_REQUIRED` na comparação de conjuntos de partes. Não registrar como bug de rede/ONLYOFFICE nem como perda comprovada de notas/campos sem examinar os dados.
- A decisão de criação de uma política de **publicação restrita a fixtures sintéticas** será tomada a partir de diferenças tipificadas e evidências Word/PDF. Nenhuma configuração possibilita publicação dos modelos oficiais.

## Requisitos do operador

Windows com Word Desktop e PowerShell; checkout da branch `feat/b05-core-docs-api` contendo o PR #17; Python 3.13 com `lxml==6.1.1` e `PyMuPDF==1.26.7` (apenas instrumentação, não dependências do produto). Testes locais exigem também `python-docx==1.2.0`. Registrar versões efetivas usadas.

Use **somente** as cópias em `experiments/b05-m2r-real-runtime/.private/run-20261023-03/` registradas no relatório M2R. Se os bytes já não estiverem disponíveis, **pare e registre ausência de fonte**; não reconstrua a candidata e não faça alterações silenciosas nos originais.

**Não** use o AV3 oficial como objeto de alteração; não publique, normalize, corrija, reconstrua ou abra write-mode os arquivos analisados. PDF exportado pelo Word é apenas evidência read-only. O relatório final pode ser compartilhado; não compartilhe `word/*.json`, DOCX, PDFs, relatório com caminhos/tokens ou `expected.json`.

## Execução única no Windows

A partir da raiz do repositório (PowerShell). Substitua a raiz apenas se a pasta privada tiver outro nome. Use paths absolutos para não depender do diretório corrente.

```powershell
$repo = (Get-Location).Path
$dir = Join-Path $repo 'experiments/b05-m2r-real-runtime/.private/run-20261023-03'
$work = Join-Path $repo 'experiments/b05-m2p-editorial-fidelity/.private/run-01'
$src = Join-Path $dir 'ooxml/source.docx'
$edit = Join-Path $dir 'ooxml/candidate-quarantined.docx'
$script = Join-Path $repo 'experiments/b05-m2p-editorial-fidelity/scripts'
if (!(Test-Path -LiteralPath $src -PathType Leaf) -or !(Test-Path -LiteralPath $edit -PathType Leaf)) { throw 'Cópias M2R não disponíveis; PARAR' }
if (Test-Path -LiteralPath $work) { throw 'Rodada existente: não sobrescrever' }
New-Item -Path $work -ItemType Directory | Out-Null

# A mudança textual da fixture M2R é conhecida. Descubra no DOCX privado a
# posição zero-based do parágrafo contendo EDITE AQUI. Não cole texto privado em relatórios.
# Exemplo ordinal 0: troque-o pelo índice observado; erro de ordinal -> HOLD seguro.
@{ paragraphIndex = 0; oldFragment = 'EDITE AQUI'; newFragment = 'EDIÇÃO M2R REAL' } |
  ConvertTo-Json -Compress | Set-Content -LiteralPath (Join-Path $work 'expected.json') -Encoding UTF8

python "$script/ooxml_fidelity.py" --source "$src" --candidate "$edit" `
  --expected "$work/expected.json" --out "$work/ooxml.json"
```

O índice é zero-based **incluindo os parágrafos das células da tabela**, coletados em ordem do `w:body` pelo instrumento. Uma expectativa incorreta gera `UNEXPECTED_TEXT_CHANGE`, não um PASS. É permitido usar uma expectativa com textos completos (`oldText/newText`) se for mais preciso; nunca exiba o texto nos resultados públicos.

Em seguida preparar duas tarefas **read-only** no Word, com caminhos privados:

```powershell
@(
  @{id='source';docx="$src";pdf="$work/source-word.pdf";json="$work/source-word.json"},
  @{id='candidate';docx="$edit";pdf="$work/candidate-word.pdf";json="$work/candidate-word.json"}
) | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath "$work/word-jobs.json" -Encoding UTF8

powershell -NoProfile -ExecutionPolicy Bypass -File "$script/word_readonly.ps1" -JobsFile "$work/word-jobs.json"

python "$script/fidelity_report.py" `
  --ooxml "$work/ooxml.json" `
  --source-word "$work/source-word.json" --candidate-word "$work/candidate-word.json" `
  --source-pdf "$work/source-word.pdf" --candidate-pdf "$work/candidate-word.pdf" `
  --out "$work/m2p-shareable.json"
```

**Importante:** o Word pode enumerar parágrafos de modo distinto do XML. O sintetizador identifica automaticamente **um único** parágrafo alterado pelo texto no Word; com zero ou vários, ele bloqueia. `--changed-word-index N` somente para conferência manual da posição nos JSONs privados; essa flag não corrige diferenças. As páginas PDF com texto intencionalmente alterado apresentarão pixels diferentes: essa contagem não deve ser usada isoladamente como reprovação ou aprovação. **Inspecione visualmente ambos PDFs, páginas, tabelas, cabeçalhos, campos, margens, imagens e quebras.**

## Validação local do incremento

```powershell
python -m unittest discover -s experiments/b05-m2p-editorial-fidelity/tests -v
python -m py_compile experiments/b05-m2p-editorial-fidelity/scripts/ooxml_fidelity.py
python -m py_compile experiments/b05-m2p-editorial-fidelity/scripts/fidelity_report.py

git diff --check
```

O relatório compartilhável `m2p-shareable.json` exibe apenas contagens, part names, deltas de coordenadas e classificações; não contém conteúdo textual, hashes, IDs pessoais, tokens ou paths. Guardar o restante sob `.private` (ignorável pelo Git).

### Limitações observadas na execução Windows

Resultado registrado em [results/windows-2026-10-23.md](results/windows-2026-10-23.md).
O diagnóstico `FIELD_INSTRUCTIONS_CHANGED` compara também a presença das partes:
adicionar notas contendo apenas separadores altera a assinatura mesmo sem campos.
Esse rótulo isolado não prova perda ou reescrita de instruções. Conferir o inventário
Word e os elementos `fldSimple`, `instrText` e `fldChar` antes de interpretar a causa.
O algoritmo fornecido foi preservado para manter o resultado bruto reproduzível.

As métricas Word de fonte/espaçamento/âncora excluem o parágrafo editado; por isso
não comprovam preservação da formatação desse parágrafo nem da formatação de cada
run. A ausência de campos, imagens e cabeçalhos na fixture não valida esses recursos.
`sourceUnmodified` no probe original era uma declaração do procedimento; na primeira
execução foi verificada separadamente por hashes privados. O probe atual mede os hashes
antes/depois e exige `Word.ReadOnly`, mantendo esses valores somente no JSON privado.

Na fixture rica, `ExportAsFixedFormat` ficou sem retornar. A alternativa explícita
`-PdfMethod SaveAs2` usa argumentos COM tipados e grava somente PDF, fechando sem salvar
o DOCX; ela foi executada sobre o par real com hashes inalterados. `SaveAs2` pode atualizar
o resultado de `SAVEDATE` em memória. Use o mesmo método nos dois documentos e examine
`fieldsBeforeUpdate`, `fieldsAfterUpdate` e `fields` após o PDF. `-UpdateFields` faz a
atualização explícita de todas as histórias percorridas. Métricas de corpo/páginas são
guardadas antes e depois do PDF; uma divergência conhecida bloqueia a síntese. Relatórios
antigos sem essa observação não adquirem retrospectivamente essa garantia.

## Matriz de decisão de publicação futura

| Evidência | Ação |
|---|---|
| `PART_SET_CHANGED`, alteração de `styles.xml`/`stylesWithEffects.xml`, campos, notas ou mídias | `QUARANTINE_REQUIRED`, estudar comportamento e risco por categoria; **nunca** allowlist genérica |
| Apenas texto esperado no XML, sem mudança de partes protegidas | Pré-qualificação técnica; exigir layout Word e revisão visual |
| Word muda páginas, parágrafos, coordenadas ou estilos não editados | `HOLD_QUARANTINE` |
| Word mantém geometria e estrutura, diferenças visuais limitadas à edição intencional | Elegível a revisão de política para documento **sintético**; ainda sem publicação automática |
| Qualquer modelo oficial, SAVEDATE→REF, nota/relacionamento removido, divergência não explicada | G1/G2 e M3 permanecem bloqueados |

### Critério de parada e próximo commit de produto

Rodar **uma vez** a análise para a candidata existente; não gerar dezenas de variantes. O pacote fecha a medição de fidelidade estrutural/Word de um salvamento real. A próxima alteração **no backend**, caso a evidência permita, deverá introduzir política versionada de aceitação **restrita a documentos sintéticos**, com testes de regressão que recusem cada classe de alteração proibida, preservação de `basedOn`/CAS, auditoria e passagem de `QUARANTINED` para `AVAILABLE` somente mediante fluxo explícito autorizado e justificado. Esse código **não** está presente neste incremento porque o pacote candidato está privado e sua análise ainda não foi executada no Word. Não alterar a política por suposição.

**Estado alvo B05-M2P:** edição real → candidata → avaliação OOXML → provas Word/PDF → decisão explícita → versão publicada e reaberta. **Estado deste incremento:** até provas Word/PDF e classificação, sem mudança da condição de publicação. Nada aqui certifica fidelidade completa de todos os recursos possíveis do Word; o aceite institucional requer matriz documental dos modelos oficiais e testes separados de SAVEDATE/PAGE/imagens/notas e formatação.
