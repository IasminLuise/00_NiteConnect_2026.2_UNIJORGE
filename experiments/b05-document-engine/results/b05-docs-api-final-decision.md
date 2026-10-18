# B05.0.3 — decisão final e matriz T01–T32

**NO-GO para selecionar ONLYOFFICE na arquitetura exigida pelo NITE CONNECT.
Encerrar esta pesquisa Docs API/WOPI, conforme o ponto de parada autorizado.**
O controle de transporte salvou de fato; o ensaio com validação ativa foi
recusado e o Word mostrou alteração real de espaçamento com o mesmo conteúdo.
Não há aprovação dos modelos oficiais, nem evidência de que trocar o protocolo
resolva a fidelidade. Não é diagnóstico de perda definitiva/corrupção universal
do ONLYOFFICE.

Branch `spike/b05-docs-api-vs-wopi`, baseada em
`da96df43618114fe71e494cc9ccc8183d9d6797c` (PR #11). Imagem e ambiente em
[baseline](b05-docs-api-baseline.md). O ensaio usa o mesmo motor 10.0.0.1 fixado
por digest, sem Collabora/LibreOffice novos. Não comparar dois motores distintos
quando a variável é Docs API/WOPI.

## Resultado dos gates

| Gate | Resultado | Consequência |
|---|---|---|
| A | PASS técnico local | Configuração assinada, edição/readonly e fontes isoladas |
| B1 | PASS controle estrutural | Callback 2, commit, bytes/hash e reabertura; não aprova fidelidade |
| B2 | FAIL | Gate ativo rejeita deltas; Word XYZ confirma mudança de layout |
| B3–B5 | NOT-RUN nos fluxos reais restantes | Ordem fixa interrompida; testes Java parciais não equivalem aos ensaios |
| C | BLOCKED | SAVEDATE e modelos oficiais não ensaiados |
| D | BLOCKED | Somente wrapper mínimo A/B, sem homologação de UX |
| E | BLOCKED | Licença/operação não revalidadas |
| F | Decisão NO-GO documentada | Revisão/CI e entrega do PR registradas no fechamento |

O Word de controle com `XYZ` e a candidata têm o mesmo texto por parágrafo. No
controle, `SpaceAfter=8 pt`; na candidata, `0 pt`. O quarto parágrafo inicia em
149,25 pt contra 117,75 pt. Ambos têm uma página. Logo, aceitar toda a saída como
equivalente esconderia uma diferença real de layout. Detalhes, métricas e limites
da causalidade em [fidelidade](b05-docs-api-fidelity.md).

## Matriz de evidência

`PASS` é sempre limitado à evidência descrita. `BLOCKED` significa dependência de
gate; `NOT-RUN`, ausência de execução; `INCONCLUSIVE`, ensaio sem conclusão válida.
Paths privados são relativos a `experiments/b05-document-engine/.runtime/b0503/`.
Nenhum dado privado acompanha a matriz pública.

| ID | Estado | Evidência / limite |
|---|---|---|
| T01 Docs API/WOPI documentados | PASS | [baseline](b05-docs-api-baseline.md), URLs oficiais consultadas |
| T02 Community/build disponível | PASS | `active/browser/readonly/`, abertura editável do controle; direitos comerciais não inferidos |
| T03 Configuração/JWT/contexto | PASS | `DocsSecurityTest`, `DocsApiHttpTest`; config assinada realmente aberta |
| T04 Leitura/edição | PASS | `active/browser/readonly/`, controle; negativa de escrita do host em Java |
| T05 Key/fonte/cache | PASS | `active/browser/recovery-after-restart/result.json`, fonte exata e key nova |
| T06 Callback 2 + aceite | PASS | `active/browser/control-contract-fixed/`, somente controle estrutural |
| T07 Force save real | NOT-RUN | B2 parou antes de B3; suporte implementado não é prova operacional |
| T08 Fechamento normal | PASS | Timelines controle/estrito, status 2; sem promessa geral de latência |
| T09 Evento não confirma host | PASS | Screens `pending.png`, commits zero e barra externa pendente |
| T10 REF recusado | PASS | `DocsApiServiceTest.strictRefRejectionKeepsOldVersionAndDurableRecovery`; sintético Java, não indução real no editor |
| T11 Estrutura recusada | PASS | `active/browser/strict-contract-fixed/`; gate mantém ponteiro e candidata |
| T12 Replay | PASS | `DocsApiServiceTest.signedCallbackPersistsExactBytesAndReplayIsIdempotentAcrossRestart`; sem replay real do fornecedor |
| T13 Key obsoleta | PASS | `DocsApiServiceTest.statusOneFourAndErrorsNeverCreateVersionsAndStaleKeyCannotOverwrite`; concorrência real não homologada |
| T14 Download falho/timeout | NOT-RUN | Redirect recusado em Java; falhas reais de URL de setup retidas, sem ensaio completo de timeout/recuperação |
| T15 Restart/recuperação | PASS | Reinício real do host + runner de recuperação; bytes aceitos/rejeitados iguais, nova key; não cobre restart de todo o motor |
| T16 Rodada WOPI focal | INCONCLUSIVE | `wopi-control/`: timeout de seletor e restart durante espera; sem conclusão causal nova |
| T17 SAVEDATE simples | BLOCKED | Gate C não aberto; evidência B05.0.1 preservada |
| T18 SAVEDATE complexo | BLOCKED | F02 preparada, não editada pelo motor nesta rodada |
| T19 Word tipo 22/cache | BLOCKED | Word aberto somente para F01, sem campos SAVEDATE |
| T20 Integridade/layout | FAIL | `analysis/`: corpo correto, métricas/layout diferentes; equivalência das demais partes não certificada |
| T21 AV3 C0/C1 | BLOCKED | Nenhum teste oficial novo |
| T22 Relatório/7 páginas | BLOCKED | Nenhum teste oficial novo |
| T23 C2/duas rodadas | BLOCKED | Gates B/C não aprovados |
| T24 Wrapper/tema/toolbar | NOT-RUN | Embed/barra/toolbar mínimos A/B observados; Gate D completo não executado |
| T25 Plugin | NOT-RUN | Gate D não aberto |
| T26 Histórico/download | NOT-RUN | Download exato autenticado e imutável ensaiado; histórico visual completo não executado |
| T27 Permissões/coedição | NOT-RUN | Readonly/negativas locais; dois coeditores e revogação não exercitados |
| T28 UX de rejeição | PASS | Screen estrita + recuperação pós-restart, sem falsa confirmação NITE |
| T29 Direitos/licença | NOT-RUN | [licença](b05-docs-api-licensing.md), nenhuma aprovação presumida |
| T30 G1 operacional | PASS | [ADR](../ADR-B05-001.md), lacunas registradas; **G1 não aprovado** |
| T31 Revisão e CI | BLOCKED | Fechamento pendente de revisão independente e execução remota |
| T32 Decisão/contingência | PASS | Este relatório + adendo ADR; nova solução depende de decisão institucional |

## Comparação e decisão

Docs API permitiu uma barra externa baseada em versão efetiva, recuperação
autenticada e uma configuração mais direta de embed. Isso é evidência técnica
local, **não superioridade geral nem homologação de UX/licença**. A rodada WOPI
nova não completou; os resultados históricos continuam negativos no fluxo
conservador. Não há medição nova válida de desempenho ou recuperação comparativa.

A limitação observada agora vai além de o gate não reconhecer XML: no Word houve
diferença de layout no caso sintético. Não foi localizada a causa interna do
fornecedor nem homologada equivalência para todas as partes. A rodada focal não
relaxou o gate nem modificou os modelos para forçar GO. Por isso a investigação
termina aqui, e não em novas versões/variantes automaticamente.

**Recomendação:** submeter uma decisão institucional sobre outra arquitetura,
incluindo viabilidade Microsoft 365/SharePoint, ou revisão formal dos modelos e
do requisito de fidelidade. Nenhuma alternativa foi escolhida, integrada ou
contratada. Não iniciar backend documental, V6, frontend de produto ou produção.
G1-F/G1 permanecem sem aprovação e G2 fechado; aceite editorial não é presumido.

## Validação e fechamento

Antes da revisão: Maven `verify` do host, **50 testes, zero falhas/erros**;
Python `unittest discover`, **41 testes OK**; `adapter_ci.py`, exit 0.
Browser local: controle estrutural, recusa estrita, readonly, restart/recuperação e
reabertura passaram nos respectivos objetivos limitados. Tentativas de setup e
WOPI incompletas não foram contadas como PASS. Word COM/PDF/PNGs: mesma edição,
mesmo texto e diferença de layout comprovada. Testes antigos do host foram
reexecutados porque `DocumentStore` mudou. API principal, Flyway/PostgreSQL e
suíte de Blocos 01–04 não se aplicam: esses componentes não foram alterados.

Os 177 hashes do congelamento histórico foram recalculados: todos intactos.
O projeto Compose novo foi parado, sem apagar volumes/caches nem iniciar os
projetos históricos. Revisão independente e CI remota ainda pendentes neste
checkpoint. Evidências privadas e comandos de reprodução:
[protocolo](../scripts/DOCS-API-B0503.md).
