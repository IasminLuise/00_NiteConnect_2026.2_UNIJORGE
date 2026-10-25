# B05.0.3 — revisão independente e correções

Uma revisão readonly independente avaliou `da96df4..7616904`, com contexto novo,
contra plano, instruções, diff completo, código e testes. Não é approval formal
no GitHub. Não executou motor, Word ou documentos privados. Nenhum achado Critical.

| Achado | Classificação final / efeito | Correção e validação |
|---|---|---|
| Pasta `browser/` ausente no runtime novo | Important; CI encerra antes do primeiro smoke | Preparador cria o pai; caso Python reproduziu ENOENT, depois criou checkpoint exclusivo e recusou reutilização |
| Callback 6 sem operação pendente | Important; checkpoint assinado antigo poderia sobrescrever mais novo | Exigir `userdata` não vazio e igualdade com comando pendente; teste Java RED→GREEN cobre ausente, divergente, correlacionado, obsoleto e replay sem rollback |
| Reabrir novamente sessão antiga | Important; invalida sucessora legítima | Reabrir exige chave ainda ativa; teste Java RED→GREEN mantém sucessora válida após segunda chamada recusada |
| Nova sessão herda recuperação da anterior | Reclassificado de Minor para Important: link de recuperação enganoso em fluxo de integridade | Limpar `candidateHash/reason/lastStatus/forceEpoch` transitórios; RED→GREEN confirma ausência de link falso e preserva bytes da tentativa original |
| T01–T04 com descrições divergentes | Reclassificado de Minor para Important: PASS associado a requisito diferente compromete rastreabilidade da decisão | Matriz inteira usa descrições T01–T32 literais do plano; comparação estática falhou antes e passou depois, sem novo ensaio |

Foi realizada uma única passagem de correção, sem segunda revisão. Após os
ajustes: Maven `verify`, **53 testes, zero falhas/erros**; Python, **41 testes OK**;
adapter smoke, exit 0; matriz, diff-check e 177 hashes históricos íntegros.
CI remota anterior falhou com o mesmo ENOENT identificado pelo reviewer; não foi
apresentada como sucesso. O fechamento registra a execução remota do código
corrigido em [decisão final](b05-docs-api-final-decision.md).

O contrato de status 6 agora aceita somente o comando correlacionado do host
experimental. Não suporta checkpoints espontâneos sem ordenação. O ensaio real
de force save continua NOT-RUN pela parada B2; teste de host não o homologa.
As correções de estado/protocolo não alteraram o gate, motor ou os documentos.

## Julgamentos preservados e custo se incorretos

O reviewer listou explicitamente assuntos que não julgou. O executor decidiu:

1. Preservar checkout, branch derivada e ledger A–F privado em vez dos helpers
   genéricos: evita alterar mounts históricos. Custo: organização de execução
   diferente da skill, sem isolamento físico extra; diff/branch revisados.
2. Renderizar pelo Word/PDF/PNG: cumpre o requisito de comparação visual
   do plano e usa a referência exigida. Custo: sem segundo renderer nesta rodada.
3. Oficiais e SAVEDATE na Docs API continuam bloqueados por B, sem julgamento
   positivo presumido. Custo: compatibilidade desses arquivos segue desconhecida.
4. UX ampliada/plugins/mobile/coedição não serão implementados após a parada.
   Custo: capacidade de produto não comprovada; não selecionar pela demo mínima.
5. Licença/capacidade/TLS/G1/G2 não recebem aprovação. Custo: viabilidade jurídica
   e operacional não decidida, mantendo integração bloqueada.
6. Force save/retries/falhas prolongadas reais ficam sem ensaio; corrigir os
   defeitos de host identificados não reabre B3–B5. Custo: comportamento do motor
   nesses cenários não certificado.
7. Causa interna de layout e superioridade causal sobre WOPI não são afirmadas.
   Custo: diagnóstico limitado aos resultados observáveis, sem ranking geral.
8. Word/PDF/PNGs e 177 hashes foram conferidos pelo executor, sem novo acesso do
   reviewer a privados. Custo: evidência material local sem dupla verificação;
   autoridade institucional continua necessária.

As escolhas de helpers/renderer que o reviewer também deixou de julgar estão
cobertas por 1–2, sem nova delegação. **Minor adiado: nenhum.** Nenhum desses
julgamentos converte o NO-GO ou os NOT-RUN em aprovação.
