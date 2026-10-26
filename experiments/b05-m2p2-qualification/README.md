# NITE CONNECT — B05-M2P.2: qualificação semântica e visual antes de publicação

**Estado:** patch incremental de instrumentação e política `B05_M2P2_SHADOW_V1`; **não** implementa promoção, nova allowlist Java, relaxamento do `DocxIntegrity`, homologação Word ou liberação de modelos oficiais. O objetivo é responder se uma candidata merece uma revisão técnica de publicação, com evidência e sem mascarar incerteza.

## Origem e limites

- Aplicar à branch `feat/b05-core-docs-api` (PR #17) **depois** do incremento local B05-M2P (`experiments/b05-m2p-editorial-fidelity`). Este patch é apenas aditivo; não depende de merge, publicação ou alterações da migração V8.
- Não reimportar nem editar arquivos oficiais, nem reconstruir/normalizar a candidata real. As duas cópias da rodada M2R continuam dentro de `.private`; Word trabalha **read-only**.
- Nenhum DOCX, PDF, token, hash privado, nome pessoal ou conteúdo do documento entra no relatório compartilhável. Nomes internos de parts OOXML são contados/classificados, mas não se copiam textos ou caminhos privados.
- A decisão de homologação G1/G2/M3 permanece bloqueada. O status `SYNTHETIC_POLICY_REVIEW_ELIGIBLE` **não** promove uma versão e **não** aprova um modelo oficial.

## Três frentes em uma campanha

**A — Equivalência semântica OOXML.** Verifica a substituição de texto esperada; distingue IDs de estilos de suas definições, inspeciona campos de todos os stories Word relevantes, inventaria notas e referências, resolve destinos de relacionamentos, verifica tipos OPC e identifica remoção de mídia/thumbnail/stylesWithEffects. A renomeação `Heading1`→`693` poderá ser classificada como *definição correspondente* caso essa identidade realmente se confirme, mas alterações adicionais de árvore, estilo, campos e partes continuam bloqueadas. Uma parte de notas nova sem conteúdo de usuário não se torna automaticamente permitida. Todas as classes de mudança são observações; nenhuma delas aprova equivalência global por presunção.

**B — Comparação visual independente.** Com os **PDFs exportados pelo Word**, confirma papel e contagem de páginas, conta diferenças sem descartar pixels, localiza automaticamente a única linha que mudou no texto extraído e calcula diferenças dentro/fora da união de suas caixas. Não aceita máscaras editadas pelo operador. Todas as regiões residuais (incluindo os **35 pixels na tabela** do M2P) são mantidas como inconclusivas; pequenas diferenças nunca viram antialiasing presumido. Confirma determinismo de nova renderização do PDF de origem. A comparação de caixas/fonte das demais linhas usa tolerância de 0,75 pt; o layout completo não é certificado só por essa métrica.

**C — Política versionada de qualificação.** `policy/B05_M2P2_SHADOW_V1.json` especifica escopo sintético e veto a conteúdo não demonstrado. O executor só produz `HOLD_QUARANTINE` ou `SYNTHETIC_POLICY_REVIEW_ELIGIBLE`; `canPromote=false` e `officialApproved=false` são invariantes. A política exige também o resumo Word do B05-M2P: o relatório anterior `HOLD_QUARANTINE` não é reescrito como PASS apenas com este incremento. A única rota para futuro backend será nova revisão explícita, precedida de evidência positiva, testes negativos, controles de CAS, auditoria e revogação. Não existe atalho de flag para liberar edições oficiais.

## Pré-condições no Windows

1. Verificar a branch/worktree e não aplicar patch com conflitos. Não fazer `git reset --hard`, apagar `.private` ou sobrescrever resultados.
2. Python com `lxml==6.1.1`, `PyMuPDF==1.26.7`, `numpy` e `python-docx==1.2.0`; Word Desktop 16.0, conforme ambiente registrado no B05-M2P. O código de produto Java não usa nenhuma dessas bibliotecas.
3. Manter os pares privados `source.docx` e `candidate-quarantined.docx`, o arquivo privado `expected.json`, dois PDFs `source-word.pdf`/`candidate-word.pdf` e `m2p-shareable.json` **da mesma rodada**, todos já coletados. Não associar dados de rodadas diferentes.
4. Conferir localmente os hashes dos dois DOCX antes e depois conforme o manifesto privado anterior. O novo relatório não os divulga.

## Etapa 1 — Avaliar a candidata M2R já preservada

PowerShell na **raiz do repositório**. Os caminhos do exemplo são relativos à execução registrada; ajuste-os para os **paths reais**, verifique cada arquivo e não utilize a pasta do modelo institucional:

```powershell
$root = (Get-Location).Path
$scripts = Join-Path $root 'experiments/b05-m2p2-qualification/scripts'
$old = Join-Path $root 'experiments/b05-m2p-editorial-fidelity/.private/run-01'
$m2r = Join-Path $root 'experiments/b05-m2r-real-runtime/.private/run-20261023-03'
$src = Join-Path $m2r 'ooxml/source.docx'
$candidate = Join-Path $m2r 'ooxml/candidate-quarantined.docx'
$expected = Join-Path $old 'expected.json'
$word = Join-Path $old 'm2p-shareable.json'
$srcPdf = Join-Path $old 'source-word.pdf'
$editPdf = Join-Path $old 'candidate-word.pdf'
$out = Join-Path $root 'experiments/b05-m2p2-qualification/.private/run-01/m2p2-shareable.json'
$inputs = @($src,$candidate,$expected,$word,$srcPdf,$editPdf)
foreach ($f in $inputs) { if (-not (Test-Path -LiteralPath $f -PathType Leaf)) { throw "Arquivo privado ausente: $f — PARAR" } }
if (Test-Path -LiteralPath $out) { throw 'Saída já existente: não sobrescrever' }

python "$scripts/qualify.py" --source "$src" --candidate "$candidate" `
  --expected "$expected" --source-pdf "$srcPdf" --candidate-pdf "$editPdf" `
  --word-summary "$word" --out "$out"
```

**Saída compartilhável:** somente `experiments/b05-m2p2-qualification/.private/run-01/m2p2-shareable.json`. Os documentos, PDFs, textos de expectativas e hashes permanecem privados no worktree. Não encaminhar paths locais em capturas de tela.

**Hipótese prévia, não resultado novo:** como o relatório anterior contém mudanças de estilos e partes protegidas e 35 pixels inexplicados na tabela, esperamos `HOLD_QUARANTINE`. Se o código informar algo diferente, conferir primeiro integridade dos inputs e a implementação; não declarar aprovação.

## Etapa 2 — Ampliar a cobertura com uma fixture fictícia rica

A fixture nova é **100% fictícia** e inclui: duas páginas por quebra explícita, título, estilos, tabela 3×3, imagem PNG gerada localmente, cabeçalho com `SAVEDATE`, rodapé com `PAGE` e `NUMPAGES`, e nota de rodapé verdadeira. O documento foi renderizado em duas páginas durante os testes locais do patch, mas isso não substitui Word Desktop.

```powershell
$rich = Join-Path $root 'experiments/b05-m2p2-qualification/.private/rich-run-01/fixture-rich.docx'
if (Test-Path -LiteralPath $rich) { throw 'Fixture existente; não sobrescrever' }
python "$scripts/make_rich_fixture.py" --out "$rich"
```

Para obter a **candidata editada pelo motor real**, reutilizar o provisionamento e executor do M2R em um **novo projeto/banco descartável** conforme `experiments/b05-m2r-real-runtime/README.md`, trocando **somente** `--fixture` pelo caminho de `$rich`. O usuário realiza **uma** edição explícita no navegador: trocar `MARCADOR_EDITAVEL_M2P2` por `MARCADOR_EDITADO_M2P2` no texto do corpo, sem alterar tabela, nota ou cabeçalho de propósito. Não simular callback nem produzir arquivo “equivalente” no Python para declarar sucesso do motor.

Se Entra, HTTPS confiável, runtime REAL do ONLYOFFICE e autorização de edição não estiverem disponíveis, marcar a etapa `NOT_RUN_ENVIRONMENT_BLOCKED`. **Não** desabilitar segurança. A nova execução deverá preservar `source-rich.docx`/`candidate-rich.docx` privadamente e não reutilizar IDs/volumes do M2R anterior. Uma candidata em quarentena pode ser analisada offline, mas nunca promovida à força.

Após salvar, preparar novo `expected-rich.json` privado com `paragraphIndex=2`, `oldFragment=MARCADOR_EDITAVEL_M2P2` e `newFragment=MARCADOR_EDITADO_M2P2` (conferir índice pelo XML se o motor modificar a estrutura). Rodar o Word em leitura com `experiments/b05-m2p-editorial-fidelity/scripts/word_readonly.ps1` em duas tarefas `source` e `candidate`, gerar os dois PDFs e o resumo com `fidelity_report.py`, **sempre dentro da nova `.private`** e sem substituir PDFs/JSON anteriores. Em seguida executar `qualify.py` com os novos caminhos. Registrar Word versão/build, ONLYOFFICE versão, documentos Word PDF, contagens de páginas e critérios de parada.

**Regra dos campos dinâmicos:** `SAVEDATE` pode variar como resultado apresentado pelo Word; a instrução OOXML deve ser preservada. Não esconder diferença temporal sob uma máscara visual para forçar PASS. Da mesma forma, alteração de `PAGE`/`NUMPAGES` para `REF` é reprovação semântica mesmo que o PDF pareça igual.

## Testes e verificação do patch

```powershell
python -m unittest discover -s experiments/b05-m2p-editorial-fidelity/tests -v
python -m unittest discover -s experiments/b05-m2p2-qualification/tests -v
Get-ChildItem experiments/b05-m2p2-qualification/scripts -Filter *.py | ForEach-Object { python -m py_compile $_.FullName }
git diff --check
```

Só a segunda linha acrescenta novos testes; a primeira prova que os testes históricos do M2P continuam funcionando. `py_compile` e os testes não substituem uma execução Word/ONLYOFFICE. Não afirme suíte Java/API reexecutada porque o backend, V8 e guard permanecem inalterados.

## Critérios de decisão e ponto de parada

| Resultado consolidado | Classificação | Próxima ação |
| --- | --- | --- |
| Diferença estrutural não justificada, nota/campo mudado, relação insegura ou pixel fora da edição | `HOLD_QUARANTINE` | Preservar origem e candidata; **não** relaxar `DocxIntegrity` |
| Texto esperado + OOXML estritamente protegido + métrica PDF sem residual + resumo Word positivo na fixture sintética | `SYNTHETIC_POLICY_REVIEW_ELIGIBLE` | Revisão humana **antes** de qualquer mudança no backend; sem promoção |
| Runtime/Word/candidata ausente | `NOT_RUN_ENVIRONMENT_BLOCKED` (registro de execução, não emitido automaticamente pelo analisador) | Documentar ausência; não gerar candidato artificial |
| Candidato oficial, AV3, relatório institucional | **Proibido nesta rodada** | Manter G1/G2/M3 bloqueados |

Um único ciclo com candidata já conhecida, mais **uma** fixture ampliada em runtime real se o ambiente estiver preparado. Não iniciar uma explosão de variantes, não criar allowlists por nome de parte e não fazer “normalização corretiva” silenciosa. Nenhuma decisão técnica positiva aqui pode liberar o editor oficial sem a homologação documental independente.

## Execução Windows registrada

Ver [results/windows-2026-10-23.md](results/windows-2026-10-23.md) e o JSON
compartilhável ao lado. A candidata preservada permaneceu `HOLD_QUARANTINE`.
A fixture rica estava pendente nessa primeira entrega offline. A continuação com
login, túnel autorizado, edição humana, callback, Word e comparação de duas páginas
foi executada; ver [results/windows-2026-10-23-rich-runtime.md](results/windows-2026-10-23-rich-runtime.md).
O resultado também foi `HOLD_QUARANTINE`, com alteração de formatação demonstrada.

Para reproduzir o procedimento Word que concluiu, prepare jobs novos para o par real e
execute `word_readonly.ps1 -JobsFile CAMINHO_PRIVADO -PdfMethod SaveAs2 -UpdateFields`.
O formato de saída é PDF; o DOCX de entrada permanece em leitura, com hash verificado.
O método original `ExportAsFixedFormat` ficou sem retornar nesta fixture; a causa interna
permanece não isolada. `SaveAs2` atualizou o resultado de `SAVEDATE` em memória; seus
inventários antes/depois e essa limitação precisam acompanhar a comparação visual.

Uma revisão independente identificou que o diagnóstico de notas do pacote original
não reconhecia conteúdo visual em `w:sym`/VML. A classificação de separador vazio
agora aceita somente os elementos conhecidos de separação e sua formatação limitada;
nós desconhecidos ficam em revisão. Essa classificação não autoriza adicionar notas:
a política continua bloqueando `NEW_EMPTY_NOTES_NOT_APPROVED`.

A correspondência entre DOCX, PDFs e resumo Word depende do manifesto privado da
campanha conferido pelo operador. O CLI não demonstra sozinho essa proveniência.
Os testes smoke com PDFs e métricas simulados provam o instrumento, não o motor.
