# Bloco 03 — plano e registro de execução

Especificação: `bloco-03.md`; contratos: `bloco-03-api.md`.
Execução na branch única `feat/bloco-03-atividades-evidencias`, base `93aca0ff`.
O responsável aprovou D01–D12 na conversa e pediu seu registro pós-implementação.
Execução contínua nesta sessão, com testes direcionados e regressão final.

## Restrições comuns

Java 21/Spring Boot 4.1.1/JDBC/PostgreSQL 17; não adicionar dependências.
V1–V3 imutáveis; Entra e autorização contextual existentes; negar por padrão.
Não alterar `.env`, produção, contratos anteriores ou publicar credenciais.
Anexos locais privados opt-in, TXT/PNG/JPEG, 10 MiB, 20 milhões de pixels.

## Etapas e interfaces

- [x] B03.1: decisões D01–D12 aprovadas; registrar no fechamento dos documentos.
- [x] B03.2: `V4__activities_assignments_evidence.sql`; verificar banco limpo,
  upgrade de V3 e constraints. `ActivityRepository` mantém dados e períodos.
- [x] B03.3: `ActivityStatus`, `Activity`, `ActivityPolicy`, `ActivityService`,
  contratos/controller; testes HTTP e domínio para cadastro/transições/edição.
- [x] B03.4: atribuições/candidatos/histórico; contratos de MemberDirectory e
  ProjectDirectory; testes de isolamento, saída, versões e revogação concorrente.
- [x] B03.5: `ActivityClock` e filtros calculados com data única; testes de
  hoje/ontem, fuso, estados terminais e projeto pausado.
- [x] B03.6: `EvidenceService`, `EvidenceRepository`, `EvidenceObjectStore`,
  `LocalEvidenceObjectStore` e validação; preparação/finalização em transações
  distintas, testes de bytes, retirada, correção e falhas/reconciliação.
- [x] B03.7 (validação local): regressão, revisão independente e documentação
  concluídas. A publicação/CI são acompanhadas no PR de entrega; sem deploy/merge.

Cada incremento começa por teste que expõe ausência do comportamento; executar
antes da implementação, implementar e verificar. Testes de domínio sem Spring;
HTTP/persistência com MockMvc/Testcontainers e identidades sintéticas.

## Foco da revisão

Revogação enquanto upload/escrita aguarda; filtros/contagem sem vazamento;
histórico pessoal após saída sem motivos de terceiros; órfãos privados após
falha de commit; exatamente um executor no PostgreSQL mesmo em SQL direto.

## Registro

- B03.1: aprovada na conversa; nenhuma alteração institucional implícita.
- Pré-verificação: Docker daemon inicialmente indisponível; solicitado início
  do Docker Desktop instalado, sem alteração de configuração.

- B03.2–B03.6: implementadas e verificadas no PostgreSQL 17 via Testcontainers.
- Revisão: três P2 corrigidos, testes RED→GREEN observados; reavaliação sem achados
  concretos remanescentes nos pontos corrigidos.
- B03.7: `mvnw.cmd --batch-mode verify` → 157 aprovados; após refinamento de presença
  de projectId, `mvnw.cmd --batch-mode -Dtest=ActivitiesBoundaryTest,ActivityDeadlinesTest,ActivityAuthorizationTest verify`
  → 22 aprovados e JAR atualizado. CI integral será executada no commit publicado.
- Decisões D01–D12 registradas pós-implementação na seção 4 de `bloco-03.md` e nos
  contratos HTTP, conforme pedido explícito do responsável.
- Limites: armazenamento institucional e PDF/DOCX não fazem parte do escopo inicial;
  nenhum dado externo ou ambiente institucional foi modificado.
