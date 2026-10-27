# Catálogo de migrações PostgreSQL

Este diretório é a fonte de execução do Flyway, configurada em `application.yml` como `classpath:Migrations`. Cada arquivo `Vn__*.sql` é versionado e append-only: depois de aplicado, seu conteúdo não deve ser alterado. Mudanças posteriores devem entrar em uma nova versão.

| Versão | Arquivo SQL | Bloco/escopo relacionado |
|---|---|---|
| V1 | [`V1__initialize_schema.sql`](V1__initialize_schema.sql) | Fundação do schema `nite_connect`; reserva o início do histórico sem criar dados institucionais ou contas fictícias. |
| V2 | [`V2__identity_members_access_audit.sql`](V2__identity_members_access_audit.sql) | B01: membros, contas institucionais, convites, atribuições de papel, histórico de status e auditoria imutável. |
| V3 | [`V3__projects_proposals_memberships.sql`](V3__projects_proposals_memberships.sql) | B02: propostas, projetos, vínculos e históricos de liderança/status; amplia o contrato de auditoria. |
| V4 | [`V4__activities_assignments_evidence.sql`](V4__activities_assignments_evidence.sql) | B03: atividades, atribuições, eventos e evidências, com regras de preservação de histórico. |
| V5 | [`V5__agenda_attendance_work_logs.sql`](V5__agenda_attendance_work_logs.sql) | B04: agenda, presença e registros de trabalho associados às atividades. |
| V6 | [`V6__document_management.sql`](V6__document_management.sql) | B05: catálogo de modelos, documentos, versões, PDFs, relações de origem, callbacks e eventos documentais. |
| V7 | [`V7__continuous_member_diaries.sql`](V7__continuous_member_diaries.sql) | B05: diários contínuos por integrante e proveniência da importação inicial. |
| V8 | [`V8__academic_assessment.sql`](V8__academic_assessment.sql) | B06: períodos, políticas, matrículas, planos, evidências, avaliações, cálculos, homologações, revisões e eventos acadêmicos. |
| V9 | [`V9__academic_public_feedback.sql`](V9__academic_public_feedback.sql) | B06: campos de feedback público e índices de consulta para homologações e revisões. |
| V10 | [`V10__local_document_inspection.sql`](V10__local_document_inspection.sql) | B05, ferramenta local opcional: sessões isoladas de inspeção documental; não promove versões nem altera o ciclo normal do editor. |

## Aplicação

O Spring Boot executa essas versões pelo Flyway. Os testes de upgrade configuram a mesma localização explicitamente. Não execute os arquivos manualmente em bancos já gerenciados pelo Flyway nem renumere versões aplicadas.
