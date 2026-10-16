# Plano de execução integrado — Bloco 04

Objetivo: implementar o planejamento aprovado como uma entrega, em sete etapas.
Spec: bloco-04.md e PDF consolidado v1.0. Execução nesta sessão.
Stack: Java 21/Spring Boot/JDBC/PostgreSQL 17/Flyway/Testcontainers.

## Restrições

Sem novas dependências, .env, migrações anteriores, frontend ou produção.
Preservar módulos e contratos anteriores, negar acesso por padrão.
Branch feat/bloco-04-agenda-frequencia; sem merge/deploy automático.

## Foco de revisão

- QR expirado, renovado ou encontro encerrado não pode registrar presença.
- Saída/retorno não pode duplicar duração nem presença por encontro.
- Correção/revisão deve competir com revogação usando a mesma transação.
- Histórico pessoal não pode expor dados de colegas; contagens já filtradas.
- Fuso/recorte/meia-noite e intervalos abertos devem produzir duração consistente.

## Sequência

- [x] B04.1 Ler instruções/PDF/base, fechar decisões na conversa e registrar desenho.
- [x] B04.2 Escrever Block04MigrationTest, executar RED; criar V5 e executar GREEN.
  Tabelas calendar_events/event_participants/attendance_records/participation_periods/
  absence_justifications/correction_requests/participation_events. Testar PostgreSQL real.
- [x] B04.3 Escrever AttendanceBoundaryTest de agenda/QR, executar RED; implementar
  AgendaController/AgendaService/AttendanceRepository e repetir teste específico.
- [x] B04.4 Escrever testes de entrada/saída/retorno/work logs/overlap, executar RED;
  implementar ParticipationService, contratos e ActivityParticipation; executar GREEN.
- [x] B04.5 Escrever testes de decisão/correção/imutabilidade/rollback, executar RED;
  implementar ParticipationReviewService; executar GREEN.
- [x] B04.6 Escrever testes de recorte temporal, resumos/histórico/isolamento; executar
  RED, implementar ParticipationQueries; executar GREEN.
- [x] B04.7 Implementação, regressão integral mvnw.cmd --batch-mode verify e revisão
  independente concluídas; correções com regressão e contratos/resultados atualizados.
  Commit/PR/CI são acompanhados na entrega desta conversa e no provedor.

## Registro

2026-10-16: árvore limpa em main 1c0252a9; fetch SSH falhou (publickey), fetch HTTPS
confirmou mesma main. Branch específica criada. Docker 29.7.2 disponível.
