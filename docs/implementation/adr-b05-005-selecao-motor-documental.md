# ADR B05-005 — Rota ONLYOFFICE para edição documental

**Data de atualização:** 25/10/2026

**Estado:** ONLYOFFICE é a única rota de motor mantida nesta bancada; **NO-GO para edição de documentos oficiais**. Esta decisão restringe a pesquisa local e não constitui aprovação institucional nem integração de produto.

## Contexto

O Bloco 05 precisa avaliar edição DOCX no navegador, salvamento versionado, reabertura no Microsoft Word, exportação PDF, integridade OOXML, segurança e autorização contextual. O checkout atual contém a bancada `experiments/b05-document-engine`, mas não contém referências à integração ONLYOFFICE/WOPI/Docs API em `apps/api` ou `apps/web`. O `wopi-host` é código experimental isolado.

Docs API e WOPI são protocolos de integração com o mesmo motor ONLYOFFICE. A manutenção de ambos na bancada serve à avaliação de fluxo e não representa seleção de dois motores.

## Decisão

- Manter uma bancada local ONLYOFFICE com protocolos Docs API e WOPI, fixtures sintéticas, testes, scripts focados e relatórios do NO-GO mais recente.
- Manter o patch experimental de fonte SDKJS para SAVEDATE/F9 e seu teste de regressão. O patch ainda não está integrado ao runtime oficial do DocumentServer ou ao backend NITE.
- Não relaxar o gate estrutural nem habilitar exceções globais nesta etapa.
- Não integrar ou implantar o editor em `apps/api`, `apps/web` ou produção.
- Não usar documentos oficiais nesta limpeza. Evidências privadas permanecem fora do Git.

## Evidência e estado do NO-GO

O relatório [B05.0.3](../../experiments/b05-document-engine/results/b05-docs-api-final-decision.md) concluiu NO-GO: o fluxo de transporte salvou bytes, mas o gate estrutural recusou a candidata e o controle Word demonstrou diferença de espaçamento (`SpaceAfter` 8 pt contra 0 pt; início do quarto parágrafo 149,25 pt contra 117,75 pt). Manter a saída como equivalente esconderia alteração visual funcional.

O relatório descreve dados brutos em `.runtime/b0503`, mas essa pasta não existe neste checkout. A pasta existente `aud-onlyoffice-spacing-02` foi conferida pelos manifests e corresponde a `B05-AUD-ONLYOFFICE-PAIR-1`; os casos A/B/C não foram executados e seu resultado não substitui B05.0.3. Assim, os relatórios versionados são preservados como registro, mas os artefatos brutos específicos do NO-GO estão indisponíveis para reauditoria independente nesta cópia.

Os controles de SAVEDATE/F9 mais recentes preservados são evidência experimental localizada e não superam o NO-GO de fidelidade nem provam aceitação geral de modelos oficiais. A regra de atualização do corpo e do cabeçalho continua dependente da história selecionada. O build `.8` e o patch SDKJS não são o runtime oficial validado.

## Condições para reabrir a decisão

1. Recuperar ou repetir, em ambiente privado autorizado, os dados brutos B05.0.3 e seus hashes/manifests.
2. Integrar a correção SAVEDATE ao runtime ONLYOFFICE que será efetivamente avaliado e testar `F9` na história de cabeçalho, bem como campos do corpo.
3. Demonstrar round-trip DOCX e comparação PDF em condições idênticas, sem descartar diferenças visuais por máscara ou contagem de páginas.
4. Concluir fluxo de aceitação oficial autorizado e revisões técnica, editorial, institucional, operacional e jurídica.
5. Aprovar separadamente a integração de produto, incluindo storage, autorização contextual, callback, versionamento, recuperação e rollback.

Até cumprir esses itens, documentos oficiais permanecem fora da edição pelo motor; a bancada continua sintética e local. Nenhuma decisão de armazenamento institucional, licença, implantação ou operação está inferida.

## Referências

- [NO-GO B05.0.3](../../experiments/b05-document-engine/results/b05-docs-api-final-decision.md)
- [Baseline](../../experiments/b05-document-engine/results/b05-docs-api-baseline.md)
- [Fidelidade](../../experiments/b05-document-engine/results/b05-docs-api-fidelity.md)
- [Revisão](../../experiments/b05-document-engine/results/b05-docs-api-review.md)
- [Contrato de salvamento](../../experiments/b05-document-engine/results/b05-docs-api-save-contract.md)
- [Diagnóstico SAVEDATE](../../experiments/b05-document-engine/results/b05-savedate-diagnosis.md)
