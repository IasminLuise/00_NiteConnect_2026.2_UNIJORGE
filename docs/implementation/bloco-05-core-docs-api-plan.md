# B05 — núcleo documental e Diário contínuo

## Escopo desta branch

Branch isolada `feat/b05-documents-diary`, criada sobre `main` (`39ae59cc0ad03f126fbc111d7ff00275f9a25444`). O código B05 foi portado seletivamente, sem domínio acadêmico/B06. Armazenamento e execução são locais/de desenvolvimento. Não houve deploy, provisionamento, alteração de credenciais, migração em banco persistente nem edição dos modelos oficiais.

O ONLYOFFICE foi mantido como única rota em avaliação, conforme decisão vigente. A seleção G1 não equivale ao aceite técnico/editorial G1-F. Edição e conversão de documentos oficiais continuam bloqueadas no backend até a qualificação real descrita em `bloco-05-core-docs-api-results.md`.

## Núcleo documental

O backend disponibiliza catálogo versionado de modelos, documentos por contexto e ACL, versões imutáveis, histórico, arquivamento/restauração, sessões ONLYOFFICE Docs API, callbacks, downloads DOCX e PDF associado à versão solicitada. O armazenamento de bytes local exige habilitação explícita em configuração de desenvolvimento. O editor também fica desativado por padrão. O guard OOXML conserva estrutura protegida e coloca candidatos incompatíveis em quarentena; isso não prova fidelidade visual.

Versões Flyway desta branch: V6 (`document_management`) e V7 (`continuous_member_diaries`). A escolha foi feita depois de auditorias somente leitura: Neon staging não possui `flyway_schema_history`; o relatório local persistente conhecido estava em V1–V3. Existe outro serviço PostgreSQL Windows em 5432 cuja identidade/acesso não foi estabelecido. Nenhuma migração foi aplicada nele. Por isso V6/V7 são adequadas à base limpa e aos ambientes auditados, mas sua aplicação em qualquer banco persistente segue bloqueada até auditoria desse serviço e dos alvos de implantação.

Como B06 permaneceu fora desta branch, as migrações futuras do B06 que disputem V6/V7 devem ser rebased/renumeradas sobre esta sequência antes de integração; não mesclar duas histórias com a mesma versão Flyway.

## Diário contínuo

Cada integrante tem um documento pessoal contínuo. A importação inicial manual do DOCX exportado do SharePoint registra integrante, ator, instante e motivo; datas de origem são opcionais e identificadas como `SHAREPOINT` somente quando recebidas. O fluxo não integra Microsoft Graph. O autor consulta/edita; coordenação e gestor geral consultam; gestores de projeto e usuários sem vínculo não recebem acesso administrativo por inferência. Cada salvamento normal cria uma versão do documento. O painel semanal lê atividades com prazo e evidências pessoais, sem inserir conteúdo no DOCX e sem validar horas ou presença.

Rotas adicionadas:

- `POST /api/v1/diaries/{memberId}/import` — importação inicial administrativa multipart.
- `GET /api/v1/diaries/me` e `GET /api/v1/diaries/{memberId}` — autor e leitura administrativa autorizada.
- `GET /api/v1/diaries/me/activities?from&to&page&size` — atividades e evidências pessoais em intervalo de até sete dias.

Sessões do editor, versões e downloads reutilizam as rotas de documentos existentes.

## Interface

`apps/web` contém uma aplicação mínima Next.js/TypeScript com navegação semanal, painel lateral, ações de abrir editor e baixar DOCX/PDF da versão exata. A página não simula login: o provedor Entra ainda não está conectado nesta branch; sem token, as ações permanecem desabilitadas. Quando há token injetado, um proxy same-origin encaminha somente as rotas explicitamente permitidas. A integração da sessão institucional é pendência de composição com B01, não uma credencial local inventada.

## Qualificação G1-F e critério de decisão

A evidência histórica continua insuficiente: o runtime `10.0.0.1` alterou `SAVEDATE` do AV3; o relatório semanal foi anteriormente barrado antes da abertura por hyperlinks externos HTTPS; resultados de paginação divergiram entre campanhas e hashes/fluxos precisam ser reconciliados. O build experimental `.7` não foi comprovado no ciclo normal completo. Não há execução real atual dos dois modelos nesta branch.

A aprovação exige os três fluxos nos dois modelos oficiais, controles Word equivalentes, motor fixado por digest, callbacks normais, reabertura no Word e inspeção visual/editorial completa sem perdas, incluindo `SAVEDATE`, logotipo, hyperlinks, tabelas, paginação, DOCX e PDFs. Se qualquer caso falhar, manter `NO-GO` para uso desses modelos. Sem edição automática de OOXML para mascarar defeitos. Não usar percentuais de páginas como medida de fidelidade.

## Validação automatizada

Os testes de backend usam PostgreSQL efêmero Testcontainers. Cobrem migração vazia e V5→V7, importação e imutabilidade/versionamento do Diário, autorização, atividades/evidências, integridade OOXML, vínculos HTTPS externos sem dereferenciamento e fluxos do provider simulado. Isso não constitui execução do runtime ONLYOFFICE nem aceite institucional.

A matriz atual e os resultados efetivamente executados estão em `bloco-05-core-docs-api-results.md`. Não liberar produção, migrar banco persistente, integrar Graph ou habilitar edição de modelos oficiais neste estágio.
