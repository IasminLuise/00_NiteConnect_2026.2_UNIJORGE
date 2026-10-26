# ADR B05-005 — Rota ONLYOFFICE para edição documental

**Data de atualização:** 25/10/2026

**Estado da escolha:** **ONLYOFFICE aprovado como motor documental do Bloco 05**, por decisão explícita do responsável nesta conversa em 25/10/2026. A comparação e a seleção de candidatos estão encerradas; G1 de seleção aprovado com ressalvas.

**Estado técnico/editorial:** **NO-GO para edição de documentos oficiais mantido** até superar as pendências de fidelidade. A aprovação de arquitetura não equivale a testes aprovados, aceite institucional ou autorização de produção. Ver a delimitação de G1/G1-F abaixo.

## Contexto

O Bloco 05 precisa avaliar edição DOCX no navegador, salvamento versionado, reabertura no Microsoft Word, exportação PDF, integridade OOXML, segurança e autorização contextual. O checkout atual contém a bancada `experiments/b05-document-engine`, mas não contém referências à integração ONLYOFFICE/WOPI/Docs API em `apps/api` ou `apps/web`. O `wopi-host` é código experimental isolado.

Docs API e WOPI são protocolos de integração com o mesmo motor ONLYOFFICE. A manutenção de ambos na bancada serve à avaliação de fluxo e não representa seleção de dois motores.

## Decisão anterior da bancada — preservada como histórico

- Manter uma bancada local ONLYOFFICE com protocolos Docs API e WOPI, fixtures sintéticas, testes, scripts focados e relatórios do NO-GO mais recente.
- Manter o patch experimental de fonte SDKJS para SAVEDATE/F9 e seu teste de regressão. O patch ainda não está integrado ao runtime oficial do DocumentServer ou ao backend NITE.
- Não relaxar o gate estrutural nem habilitar exceções globais nesta etapa.
- Não integrar ou implantar o editor em `apps/api`, `apps/web` ou produção.
- Não usar documentos oficiais nesta limpeza. Evidências privadas permanecem fora do Git.

## Evidência e estado do NO-GO

O relatório [B05.0.3](../../experiments/b05-document-engine/results/b05-docs-api-final-decision.md) concluiu NO-GO: o fluxo de transporte salvou bytes, mas o gate estrutural recusou a candidata e o controle Word demonstrou diferença de espaçamento (`SpaceAfter` 8 pt contra 0 pt; início do quarto parágrafo 149,25 pt contra 117,75 pt). Manter a saída como equivalente esconderia alteração visual funcional.

O relatório descreve dados brutos em `.runtime/b0503`, mas essa pasta não existe neste checkout. A pasta existente `aud-onlyoffice-spacing-02` foi conferida pelos manifests e corresponde a `B05-AUD-ONLYOFFICE-PAIR-1`; os casos A/B/C não foram executados e seu resultado não substitui B05.0.3. Assim, os relatórios versionados são preservados como registro, mas os artefatos brutos específicos do NO-GO estão indisponíveis para reauditoria independente nesta cópia.

Os controles de SAVEDATE/F9 mais recentes preservados são evidência experimental localizada e não superam o NO-GO de fidelidade nem provam aceitação geral de modelos oficiais. A regra de atualização do corpo e do cabeçalho continua dependente da história selecionada. O build `.8` e o patch SDKJS não são o runtime oficial validado.

## Pendências técnicas para habilitar documentos oficiais

1. Recuperar ou repetir, em ambiente privado autorizado, os dados brutos B05.0.3 e seus hashes/manifests.
2. Integrar a correção SAVEDATE ao runtime ONLYOFFICE que será efetivamente avaliado e testar `F9` na história de cabeçalho, bem como campos do corpo.
3. Demonstrar round-trip DOCX e comparação PDF em condições idênticas, sem descartar diferenças visuais por máscara ou contagem de páginas.
4. Concluir fluxo de aceitação oficial autorizado e revisões técnica, editorial, institucional, operacional e jurídica.
5. Aprovar separadamente a integração de produto, incluindo storage, autorização contextual, callback, versionamento, recuperação e rollback.

Até cumprir esses itens, documentos oficiais permanecem fora da edição pelo motor; a bancada continua sintética e local. Nenhuma decisão de armazenamento institucional, licença, implantação ou operação está inferida.

## Aprovação da escolha — decisão de 25/10/2026

O responsável determinou expressamente aprovar o ONLYOFFICE e resolver a etapa de
escolha do motor. Essa decisão posterior prevalece sobre a condição de candidato
em avaliação: **não é necessário comparar ou aprovar outro motor para continuar
o planejamento do Bloco 05**. Nova avaliação de fornecedor exigirá outra decisão.

O G1 original do planejamento completo reunia seleção, evidências técnicas,
fidelidade editorial e viabilidade operacional. Para registrar precisamente o
alcance desta aprovação, ficam explicitamente separados:

| Controle | Estado após a decisão | Consequência |
|---|---|---|
| G1 — seleção e aprovação arquitetural do motor | **APROVADO COM RESSALVAS**, por decisão do responsável | ONLYOFFICE é a escolha do Bloco 05; encerrar a disputa entre candidatos e prosseguir com o refinamento/contratos |
| G1-F — aceite técnico e editorial | **PENDENTE / NO-GO para oficiais** | Preservar os critérios técnicos do G1 original: runtime real, campos, fidelidade Word/PDF, salvamento, isolamento, recuperação, recursos e aceite editorial |
| G2 — liberação da implementação conforme contratos aprovados | Não é aprovado automaticamente por esta decisão | Conferir decisões funcionais, permissões, estados, Diário e storage, respeitando aprovações posteriores já registradas |
| Homologação institucional e produção | Não autorizadas por esta decisão | Licença, infraestrutura, privacidade, custos, operação e aceite exigem evidência e aprovação próprias |

Essa separação é uma atualização explícita da governança do planejamento, não uma
declaração de que os critérios técnicos originais de G1 passaram. O NO-GO histórico
não é apagado nem convertido em aceitação de diferenças de layout. As pendências
de SAVEDATE/F9, espaçamento e round-trip continuam condições de aceite do motor
escolhido. Não há dispensa de segurança, autorização ou integridade.

Docs API e WOPI continuam protocolos do mesmo motor. A aprovação não altera
contratos de integração ou flags de runtime, não aplica reparos nos DOCX e não
habilita edição de modelos oficiais. O núcleo e o adaptador Docs API existentes
no [PR #17](https://github.com/IasminLuise/00_NiteConnect_2026.2_UNIJORGE/pull/17)
são trabalho separado: este registro não os funde, revalida ou homologa.

Escopo desta entrega: documentação da decisão. Nenhum novo ensaio de motor,
alteração em API, migração, credencial, modelo institucional, merge ou deploy.

## Referências

- [NO-GO B05.0.3](../../experiments/b05-document-engine/results/b05-docs-api-final-decision.md)
- [Baseline](../../experiments/b05-document-engine/results/b05-docs-api-baseline.md)
- [Fidelidade](../../experiments/b05-document-engine/results/b05-docs-api-fidelity.md)
- [Revisão](../../experiments/b05-document-engine/results/b05-docs-api-review.md)
- [Contrato de salvamento](../../experiments/b05-document-engine/results/b05-docs-api-save-contract.md)
- [Diagnóstico SAVEDATE](../../experiments/b05-document-engine/results/b05-savedate-diagnosis.md)
