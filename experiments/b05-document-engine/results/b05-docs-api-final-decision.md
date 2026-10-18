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

| ID | Requisito do plano | Estado | Evidência / limite |
|---|---|---|---|
| T01 | Freeze versão/digest, branch e histórico | PASS | [baseline](b05-docs-api-baseline.md), base `da96df4`, `historical-freeze.json`: 177 hashes conferidos; PRs preservados |
| T02 | Mesmo motor para WOPI/Docs API, dados isolados | PASS | `compose.docs-api.yaml` fixa o digest histórico; `active/private-manifest.json`, raízes distintas e keys novas; não significa WOPI novo concluído |
| T03 | Abertura Docs API editor/readonly | PASS | `active/browser/{control-contract-fixed,readonly}/`, abertura efetiva dos dois modos e config assinada |
| T04 | JWT config/documento, negativa de token inválido | PASS | `DocsSecurityTest`/`DocsApiHttpTest`: assinatura, escopo e token inválido recusados pelo host; config válida aberta no browser; não houve ataque real ao SDK |
| T05 | `document.key` nova versus cache de versão anterior | PASS | `active/browser/recovery-after-restart/result.json`, fonte exata e key nova |
| T06 | Insert + callback `status 2` + aceitação | PASS | `active/browser/control-contract-fixed/`, somente controle estrutural |
| T07 | `status 6` force save com editor aberto | NOT-RUN | B2 parou antes de B3; suporte implementado não é prova operacional |
| T08 | Sem força: callback/tempo/fechamento normais | PASS | Timelines controle/estrito, status 2; sem promessa geral de latência |
| T09 | `onDocumentStateChange` não confirma NITE | PASS | Screens `pending.png`, commits zero e barra externa pendente |
| T10 | Recusa de REF conhecido | PASS | `DocsApiServiceTest.strictRefRejectionKeepsOldVersionAndDurableRecovery`; sintético Java, não indução real no editor |
| T11 | Recusa de mídia/estrutura crítica | PASS | `active/browser/strict-contract-fixed/`; gate mantém ponteiro e candidata |
| T12 | Replay/duplicata do mesmo callback | PASS | `DocsApiServiceTest.signedCallbackPersistsExactBytesAndReplayIsIdempotentAcrossRestart`; sem replay real do fornecedor |
| T13 | Callback fora de ordem, key obsoleta | PASS | Testes Java de chave obsoleta e callback 6 sem operação/correlação recusados; replay sem rollback; concorrência real não homologada |
| T14 | Falha/timeout no download da candidata | NOT-RUN | Redirect recusado em Java; falhas reais de URL de setup retidas, sem ensaio completo de timeout/recuperação |
| T15 | Restart e recuperação de não salvos | PASS | Reinício real do host + runner de recuperação; bytes aceitos/rejeitados iguais, nova key; não cobre restart de todo o motor |
| T16 | Comparação objetiva com uma rodada WOPI focada | INCONCLUSIVE | `wopi-control/`: timeout de seletor e restart durante espera; sem conclusão causal nova |
| T17 | `SAVEDATE` simples sem adaptação | BLOCKED | Gate C não aberto; evidência B05.0.1 preservada |
| T18 | Cópia com `SAVEDATE` complexo | BLOCKED | F02 preparada, não editada pelo motor nesta rodada |
| T19 | Word controla tipo 22, cache e atualização | BLOCKED | Word aberto somente para F01, sem campos SAVEDATE |
| T20 | Corpo e campos, estilos, relações, mídia | FAIL | `analysis/`: corpo correto, métricas/layout diferentes; equivalência das demais partes não certificada |
| T21 | AV3 C0/C1 normalizado | BLOCKED | Nenhum teste oficial novo |
| T22 | Relatório C0/C1 | BLOCKED | Nenhum teste oficial novo |
| T23 | C2 e duas rodadas independentes | BLOCKED | Gates B/C não aprovados |
| T24 | NITE wrapper/tema/toolbar | NOT-RUN | Embed/barra/toolbar mínimos A/B observados; Gate D completo não executado |
| T25 | Plugin sintético + recursos por edição | NOT-RUN | Gate D não aberto |
| T26 | Histórico/download/versão autenticados | NOT-RUN | Download exato autenticado e imutável ensaiado; histórico visual completo não executado |
| T27 | Permissões/coedição básica | NOT-RUN | Readonly/negativas locais; dois coeditores e revogação não exercitados |
| T28 | Estado de erro/rejeição na UX | PASS | Screen estrita + recuperação pós-restart, sem falsa confirmação NITE |
| T29 | Direitos Community/AGPL/branding/conexões | NOT-RUN | [licença](b05-docs-api-licensing.md), nenhuma aprovação presumida |
| T30 | Riscos e lacunas G1 operacionais | PASS | [ADR](../ADR-B05-001.md), lacunas registradas; **G1 não aprovado** |
| T31 | Revisão independente e CI sintética | PASS | [Revisão e correções](b05-docs-api-review.md); [CI 39089268410](https://github.com/IasminLuise/00_NiteConnect_2026.2_UNIJORGE/actions/runs/39089268410), código `ffe2b0d`; sem approval formal/GO |
| T32 | Parecer GO/NO-GO e contingência decidida | PASS | Este relatório + adendo ADR; nova solução depende de decisão institucional |

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
Após a passagem de correção da [revisão independente](b05-docs-api-review.md):
Maven **53 testes sem falhas/erros**, Python **41 OK**, adapter smoke exit 0,
checagem literal T01–T32 e diff-check passaram. Não houve segunda revisão.
Browser local: controle estrutural, recusa estrita, readonly, restart/recuperação e
reabertura passaram nos respectivos objetivos limitados. Tentativas de setup e
WOPI incompletas não foram contadas como PASS. Word COM/PDF/PNGs: mesma edição,
mesmo texto e diferença de layout comprovada. Testes antigos do host foram
reexecutados porque `DocumentStore` mudou. API principal, Flyway/PostgreSQL e
suíte de Blocos 01–04 não se aplicam: esses componentes não foram alterados.

Os 177 hashes do congelamento histórico foram recalculados: todos intactos.
O projeto Compose novo foi parado e o Docker Desktop devolvido ao estado parado
inicial, sem apagar volumes/caches nem iniciar os projetos históricos. Revisão
independente concluída e corrigida; [CI remota do código `ffe2b0d`](https://github.com/IasminLuise/00_NiteConnect_2026.2_UNIJORGE/actions/runs/39089268410)
**success**, incluindo transporte aceito, recusa estrita, readonly e
restart/recuperação/reabertura. A primeira execução remota falhou antes do smoke
por diretório ausente, corrigido com RED→GREEN, sem atribuir essa falha ao motor.
Os testes locais de Word/layout anteriores continuam válidos: as correções
posteriores são de controle de sessão/checkpoint/preparação e não alteram o motor,
a fixture ou o gate. Não foram repetidos ensaios oficiais, force save real ou
Gates C/D/E. Nenhum componente do produto mudou.

[PR #12](https://github.com/IasminLuise/00_NiteConnect_2026.2_UNIJORGE/pull/12)
aberto sobre #11, sem merge. Este fechamento documental não modifica o código
ensaiado em `ffe2b0d`. Evidências privadas e comandos de reprodução:
[protocolo](../scripts/DOCS-API-B0503.md).
