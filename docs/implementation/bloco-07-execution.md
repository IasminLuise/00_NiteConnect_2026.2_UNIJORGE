# Bloco 07 — registro de implementação e execução

**Estado:** capacidades implementáveis concluídas no backend local. A aceitação institucional e a ativação em ambiente compartilhado/produção não foram realizadas.

## Implementado

- Participação semanal B04 com semana explícita de sete dias, separação presencial/remota, exceções de calendário versionadas, lotes de revisão que preservam decisões individuais, pedidos de correção, dispensa sem crédito de horas e reposição reconhecida somente após trabalho compatível aprovado.
- Saúde do projeto com baseline aprovada versionada por projeto: atraso 7/14 dias e inatividade 14/30 dias; Gestão Geral/Coordenação pode ajustar cada projeto com histórico explicável. A inatividade deriva de eventos B03 auditáveis. A avaliação não altera o estado oficial do projeto nem `attentionRequired`.
- Dashboards para integrante, liderança contextual e gestão; quatro modelos de relatório em JSON, CSV e PDF; filtros e paginação limitados; comparação de períodos e histórico relacional conforme eventos efetivamente preservados.
- Projeções/exportações escopadas por papel e projeto. Justificativas e notas acadêmicas não são expostas no relatório de projeto; CSV protege contra fórmulas de planilha.
- Contratos de API e limites de dados registrados em [bloco-07-api.md](bloco-07-api.md). Migrações aditivas V11 e V12 preservam o histórico anterior.

## Verificações executadas após a alteração final

No worktree isolado `D:\nite-connect-worktrees\bloco-07-backend`, com Java 21 e Docker/Testcontainers disponíveis:

- `\.\mvnw.cmd --batch-mode verify` (executado a partir de `apps/api`): **BUILD SUCCESS**, 244 testes, 0 falhas, 0 erros e 0 ignorados. Incluiu testes de integração com PostgreSQL 17 e aplicou/validou todas as 12 migrações, incluindo V11 e V12.
- `git diff --check`: sem apontamentos, executado após documentação final.

Verificações direcionadas de saúde de projeto, participação, governança e relatórios já haviam passado durante o desenvolvimento. A suíte completa acima as substitui como validação final de código.

## Definições e integrações pendentes

- A baseline foi aprovada explicitamente pelo solicitante para esta implementação e é um ponto de partida operacional configurável por projeto; a instituição pode substituí-la por política própria. Valores: atenção/crítico em 7/14 dias de atraso e 14/30 dias sem eventos B03 relevantes.
- Datas de relatório usam o fuso configurado pela aplicação; a semana B04 recebe seus sete dias explicitamente. A aplicação não presume calendário institucional nem fechamento semanal.
- B05 (validação documental) e Google Meet continuam indisponíveis/não integrados. Indicadores dependentes disso são declarados indisponíveis; não são inferidos de anexos ou agenda.
- Comparações são contagens técnicas de eventos preservados, não desempenho homologado. Estados atuais não são reconstruídos como snapshots históricos.
- A suíte não valida Entra em tenant real, homologação institucional, deploy, migração de base compartilhada nem comportamento em produção.

## Avaliação

O backend está implementado e validado para o escopo tecnicamente determinável, em worktree e branch isolados, sem commit ou push. Permanecem as limitações de integração e validação de ambiente listadas acima.
