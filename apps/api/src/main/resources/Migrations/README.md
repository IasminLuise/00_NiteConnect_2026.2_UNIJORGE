# Catálogo de migrações PostgreSQL

Este diretório é a fonte de execução do Flyway, configurada em `application.yml` como `classpath:Migrations`. Cada arquivo `Vn__*.sql` é versionado e append-only: depois de aplicado, seu conteúdo não deve ser alterado. Mudanças posteriores devem entrar em uma nova versão.

| Versão | Arquivo SQL | Bloco/escopo relacionado |
|---|---|---|
| V1 | [`V1__initialize_schema.sql`](V1__initialize_schema.sql) | Fundação do schema `nite_connect`; reserva o início do histórico sem criar dados institucionais ou contas fictícias. |
| V2 | [`V2__identity_members_access_audit.sql`](V2__identity_members_access_audit.sql) | B01: membros, contas institucionais, convites, atribuições de papel, histórico de status e auditoria imutável. |
| V3 | [`V3__projects_proposals_memberships.sql`](V3__projects_proposals_memberships.sql) | B02: propostas, projetos, vínculos e históricos de liderança/status; amplia o contrato de auditoria. |
| V4 | [`V4__activities_assignments_evidence.sql`](V4__activities_assignments_evidence.sql) | B03: atividades, atribuições, eventos e evidências, com regras de preservação de histórico. |
| V5 | [`V5__agenda_attendance_work_logs.sql`](V5__agenda_attendance_work_logs.sql) | B04: agenda, sessões e presença por encontro; a mesma migração também criou a tabela histórica mista `participation_periods`. |
| V6 | [`V6__document_management.sql`](V6__document_management.sql) | Estruturas históricas de registros, versões e eventos; mantida porque consta no histórico de bancos já migrados. |
| V7 | [`V7__continuous_member_diaries.sql`](V7__continuous_member_diaries.sql) | Estrutura histórica complementar; mantida porque consta no histórico de bancos já migrados. |
| V8 | [`V8__academic_assessment.sql`](V8__academic_assessment.sql) | B06: períodos, políticas, matrículas, planos, evidências, avaliações, cálculos, homologações, revisões e eventos acadêmicos. |
| V9 | [`V9__academic_public_feedback.sql`](V9__academic_public_feedback.sql) | B06: campos de feedback público e índices de consulta para homologações e revisões. |
| V10 | [`V10__local_document_inspection.sql`](V10__local_document_inspection.sql) | Estrutura histórica de inspeção local; mantida porque consta no histórico de bancos já migrados. |
| V11 | [`V11__project_health_governance.sql`](V11__project_health_governance.sql) | B07: baseline inicial aprovada de criticidade por projeto, ajustes versionados e avaliações baseadas em atividade B03 imutáveis. |
| V12 | [`V12__attendance_governance_and_weekly_reviews.sql`](V12__attendance_governance_and_weekly_reviews.sql) | Contratos históricos de exceções semanais, dispensas/reposições e revisão de registros; sem APIs ativas após a substituição de escopo B04. |
| V13 | [`V13__personal_participation_contracts.sql`](V13__personal_participation_contracts.sql) | Contratos históricos de carga semanal e revisão remota; preservados sem consumidores ativos. |
| V14 | [`V14__participation_load_policy_administration.sql`](V14__participation_load_policy_administration.sql) | Histórico da administração versionada de carga semanal; preservado sem APIs ativas. |
| V15 | [`V15__planned_participation_load_policy.sql`](V15__planned_participation_load_policy.sql) | Histórico de vigência planejada da carga semanal; preservado sem APIs ativas. |
| V16 | [`V16__private_justification_attachments.sql`](V16__private_justification_attachments.sql) | Histórico de metadados/auditoria de comprovantes e retenção; armazenamento e registros não foram removidos. |
| V17 | [`V17__cap_justification_deletion_grace.sql`](V17__cap_justification_deletion_grace.sql) | Histórico de regra de descarte de comprovantes; não constitui política ativa de retenção. |
| V18 | [`V18__audited_justification_retention_purge.sql`](V18__audited_justification_retention_purge.sql) | Histórico de tabelas de expurgo de comprovantes; rotina e endpoints ativos foram retirados. |
| V19 | [`V19__justification_attachment_limits_and_minimum_grace.sql`](V19__justification_attachment_limits_and_minimum_grace.sql) | Histórico de limites/metadados de anexos; preservado sem fluxo ativo. |
| V20 | [`V20__retire_work_hour_contracts_preserve_history.sql`](V20__retire_work_hour_contracts_preserve_history.sql) | Marca tabelas históricas substituídas como inativas, preservando esquema, dados e trilhas; não executa exclusões nem altera registros. |

## Aplicação

O Spring Boot executa essas versões pelo Flyway. Os testes de upgrade configuram a mesma localização explicitamente. Não execute os arquivos manualmente em bancos já gerenciados pelo Flyway nem renumere versões aplicadas.
