# NITE CONNECT — Bloco 05: Gestão Documental e Diário de Bordo

**Estado da rota de edição:** ONLYOFFICE Docs API/WOPI permanece em avaliação local. O relatório mais recente conclui **NO-GO para documentos oficiais**. Nenhuma integração com `apps/api` ou `apps/web` foi encontrada neste checkout; a bancada experimental não é o backend do produto.

**Decisão técnica vigente:** [ADR B05-005](adr-b05-005-selecao-motor-documental.md). Os protocolos Docs API e WOPI são caminhos de integração do mesmo motor ONLYOFFICE, não motores distintos. Esta decisão documental não habilita o editor no produto, não aprova uso institucional e não autoriza deploy.

## Objetivos funcionais do bloco

- Organizar documentos e modelos institucionais segundo contexto e autorização.
- Criar documentos a partir de modelos, controlar upload e manter metadados consistentes.
- Consultar e baixar versões com autoria e rastreabilidade.
- Avaliar edição de DOCX no navegador, preservando conteúdo, semântica OOXML, campos e aparência.
- Exportar PDF associado à versão DOCX exata.
- Vincular Diário de Bordo à trajetória de integrantes, projetos e atividades, com histórico de retificações.
- Garantir autorização contextual, isolamento, controle de concorrência e persistência segura.

## Limites funcionais

- Concluir atividade B03 não cria nem aprova entrada de Diário.
- Declarar atividade no Diário não cria horas validadas em B04.
- Documento salvo não se transforma automaticamente em entrega, publicação oficial, avaliação ou nota.
- PDF é inicialmente formato de visualização/exportação; edição livre de PDF não está pressuposta.
- DOCX é o formato prioritário. O suporte a outros formatos requer decisão explícita.
- Não criar gerenciador genérico de arquivos, workflow acadêmico ou edição anônima irrestrita.
- Não usar filesystem privado local de desenvolvimento como armazenamento institucional definitivo.

## Requisitos rastreados

| Requisito | Intenção | Escopo B05 |
|---|---|---|
| RF-035 | Modelos e documentos padronizados | Biblioteca de modelos versionados e gestão autorizada |
| RF-036 | Organização por contexto e acesso | Catálogo por pessoa, projeto, evento ou demanda |
| RF-037 | Edição no navegador | Somente após superar o gate de fidelidade e aprovações |
| RF-038 | Visualização e exportação | DOCX/PDF vinculados à versão exata |
| RF-039 | Metadados e controle de acesso/edição | Autoria, datas e acesso conforme autorização |
| RF-034 / Q20 | Registro narrativo e histórico de contribuições | Entradas estruturadas com referências e revisões |
| Q22 | Edição documental integrada | Avaliação ONLYOFFICE sob os gates definidos no ADR |

A especificação consolidada permanece fonte de verdade para regras de negócio; este documento não cria pesos, prazos, tolerâncias ou normas institucionais.

## Estado de engenharia e gates

- A implementação do host em `experiments/b05-document-engine/wopi-host` é isolada e experimental. Não é integração real ao backend NITE.
- O ONLYOFFICE Docs API/WOPI passou por controles técnicos locais, mas o ensaio Docs API encontrou alteração real de espaçamento confirmada no Word. O gate de fidelidade rejeitou a candidata.
- O relatório B05.0.3 registra NO-GO, com os gates C/D/E bloqueados ou não executados; não há aceite para documentos oficiais.
- A prova SAVEDATE/F9 e o patch SDKJS permanecem experimentais e não foram incorporados ao runtime oficial do DocumentServer nem ao produto.
- Os documentos oficiais não devem ser editados por esta bancada enquanto não houver autorização de campanha específica e condições normais de fluxo.
- A integração produtiva exigirá contratos de armazenamento, autorização contextual, callback, versionamento, recuperação, revisão técnica/editorial/institucional e decisão de operação.

## Referências atuais

- [ADR B05-005 — rota ONLYOFFICE e estado NO-GO](adr-b05-005-selecao-motor-documental.md)
- [Relatório final Docs API B05.0.3](../../experiments/b05-document-engine/results/b05-docs-api-final-decision.md)
- [Baseline do ensaio](../../experiments/b05-document-engine/results/b05-docs-api-baseline.md)
- [Fidelidade DOCX/PDF](../../experiments/b05-document-engine/results/b05-docs-api-fidelity.md)
- [Revisão e validação](../../experiments/b05-document-engine/results/b05-docs-api-review.md)
- [Diagnóstico SAVEDATE](../../experiments/b05-document-engine/results/b05-savedate-diagnosis.md)
- [Laboratório ONLYOFFICE](../../experiments/b05-document-engine/README.md)
