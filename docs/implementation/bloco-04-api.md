# Contratos HTTP — Bloco 04

Base `/api/v1`, JWT Microsoft Entra validado e vínculo ATIVO obrigatório.
Administração = GESTOR_GERAL ou COORDENADOR; liderança de projeto não concede
permissões administrativas de frequência. Nenhum endpoint é público.

UUIDs, JSON, instantes ISO-8601 com offset (ex.: `2026-10-10T12:00:00Z`).
Versões `expectedVersion` inteiras >= 0 obrigatórias nas mutações existentes.
`reason`: 1–1000 caracteres, não vazio; operações críticas exigem `confirmed:true`.
Falhas: 400 entrada inválida, 403 acesso negado, 404 recurso pessoal de terceiro
ou inexistente, 409 duplicidade/estado/versão/sobreposição. Erros `ProblemDetail`
sem dados internos. `GET` paginados retornam `{items,page,size,total}`; page>=0,
size=1–100 (padrão 20). Ordem fixa. Sem exclusão física por API.

## Agenda e QR

| Método e caminho | Permissão / entrada / saída |
|---|---|
| GET `/agenda/events` | Ativo; filtros opcionais from/to (dias inclusivos); lista Event. |
| GET `/agenda/events/{id}` | Ativo; Event sem lista de integrantes ou capacidade QR. |
| POST `/agenda/events` | Administração; EventCreate; 201 Event. |
| PATCH `/agenda/events/{id}` | Administração; EventCreate + expectedVersion; somente AGENDADO sem registros dependentes. |
| POST `/agenda/events/{id}/session` | Administração; Command; abre/renova QR, devolve SessionPayload com Cache-Control:no-store. |
| POST `/agenda/events/{id}/close` | Administração; Command; encerra sessão ou encontro AGENDADO já terminado, registra conferência. |
| POST `/agenda/events/{id}/cancel` | Administração; Command + confirmed:true; cancela AGENDADO/ABERTO, invalida QR. |
| GET `/agenda/events/{id}/participants` | Administração; conferência paginada da união de esperados e presentes, com memberId/memberName/expected/checkedIn/attendanceId. |

EventCreate: title(1–200), description(0–10000), kind
`ENCONTRO|EXTRAORDINARIO|FERIADO|EVENTO`, modality `PRESENCIAL|REMOTA`,
startsAt < endsAt, expectedMemberIds (lista explícita, até 1000 ATIVOS distintos),
reason. FERIADO não admite participantes esperados nem sessão de presença.
Event: id, mesmos metadados públicos, status
`AGENDADO|ABERTO|ENCERRADO|CANCELADO`, version. Criação sempre AGENDADO.
Na consulta de agenda, `to=LocalDate.MAX` retorna 400, pois o limite inclusivo
não pode ser convertido para o início do dia seguinte. Filtros isolados continuam opcionais.

Command: expectedVersion, reason, confirmed opcional (obrigatório no cancelamento).
Sessão somente dentro do intervalo do encontro; janela técnica de até 120 segundos.
SessionPayload: eventId, payload opaco, expiresAt, version. O frontend futuro
renderizará esse payload como QR e transmitirá eventId no check-in; não há imagem
QR nem UI neste bloco. Renovação invalida o código anterior. Encerramento não
fecha silenciosamente períodos ainda abertos nem os valida como horas.
Conferência combina a lista com `/attendance/records?memberId=...` e revisões.

## Presença e declaração remota

| Método e caminho | Contrato |
|---|---|
| POST `/me/attendance/check-in` | `{eventId,payload}`; 201 AttendanceView. Horário do servidor; uma presença por encontro/pessoa. |
| POST `/me/attendance/check-out` | `{expectedVersion}` da AttendanceView; fecha a única presença aberta, retorna AttendanceView. |
| POST `/me/attendance/{id}/return` | `{expectedVersion,payload}`; novo intervalo na mesma presença, sessão válida obrigatória. |
| GET `/me/attendance` | Lista AttendanceView; from/to opcionais em par, filtra sobreposição dos intervalos. |
| POST `/me/work-logs` | `{startsAt,endsAt,description,activityId?,evidenceId?}`; 201 Period. |
| GET `/me/work-logs` | Lista Period WORK_LOG; from/to opcionais em par. |
| GET `/attendance/records` | Administração; lista Period; memberId/kind/from/to opcionais. |

AttendanceView: id (presença lógica), eventId, memberId, version e periods.
Period: id (identificador usado nas revisões/correções), memberId, attendanceId
(nulo para trabalho), kind `ATTENDANCE|WORK_LOG`, modality, startsAt, endsAt,
description, activityId, evidenceId, origin `QR|DECLARATION|CORRECTION`,
reviewStatus `PENDENTE|APROVADA|RECUSADA`, reviewedBy, reviewedAt, reviewReason,
version. `endsAt:null` indica intervalo aberto sem duração consolidada.

Check-in não exige expectativa individual: um ATIVO com QR válido pode comparecer
como não esperado; aparece na conferência. Duas presenças abertas e sobreposições
com qualquer período da mesma pessoa são recusadas. Retorno não duplica presença.
Intervalos adjacentes são permitidos. Duplicidade de check-in retorna 409, não cria
registros extras. Checkout com versão antiga ou sem entrada retorna 409.

WorkLog requer intervalo fechado já ocorrido, description(1–10000). Atividade é
opcional; se fornecida, exigir acesso e atribuição vigente do autor. Evidência é
opcional, da mesma atividade, disponível e de autoria do integrante. Não transfere
evidência nem concede acesso novo. Declaração inicia PENDENTE e não comprova horas.

## Justificativas e correções

| Método e caminho | Entrada / resultado |
|---|---|
| POST `/me/absence-justifications` | `{eventId,reason}`; 201 Justification, encontro ocorrido/encerrado e expectativa explícita. |
| GET `/me/absence-justifications` | Lista própria. |
| POST `/me/absence-justifications/{id}/complements` | `{expectedVersion,reason}`; apenas COMPLEMENTACAO_SOLICITADA, retorna a PENDENTE. |
| GET `/attendance/justifications` | Administração; memberId opcional. |
| POST `/attendance/justifications/{id}/decisions` | Administração; `{expectedVersion,decision,reason}`; somente PENDENTE. |
| POST `/attendance/records/{id}/reviews` | Administração; `{expectedVersion,status,reason}`; período fechado, status APROVADA ou RECUSADA diferente do vigente. |
| POST `/me/attendance/corrections` | `{periodId? ,eventId?,startsAt,endsAt,reason}`; exatamente um alvo; 201 Correction. |
| GET `/me/attendance/corrections` | Lista própria. |
| GET `/attendance/corrections` | Administração; memberId opcional. |
| POST `/attendance/corrections/{id}/decisions` | Administração; `{expectedVersion,expectedPeriodVersion?,decision,reason,confirmed:true}`. |
| POST `/attendance/records/{id}/corrections` | Administração; `{expectedVersion,startsAt,endsAt,reason,confirmed:true}`; correção direta auditada. |

Justification: id, memberId, eventId, reason, status, decisionReason, decidedBy,
decidedAt, version. Uma por integrante/encontro. Decision:
`ACEITA|RECUSADA|COMPLEMENTACAO_SOLICITADA`; pedido de complementação preserva
motivo anterior no histórico. Justificativa pode contextualizar ausência ou saída
antecipada; não altera comparecimento nem duração. Sem anexos/comprovantes.

Correction: id, memberId, periodId/eventId, proposedStart, proposedEnd, reason,
status `PENDENTE|ACEITA|RECUSADA`, decisionReason, decidedBy, decidedAt, version.
Correção por eventId serve para check-in ausente em encontro esperado já aberto ou
encerrado, sem presença existente. Por periodId só pode apontar período próprio.
Decisão aceita de período exige expectedPeriodVersion, para não aplicar pedido
antigo sobre alterações posteriores. Nova presença corrigida fica PENDENTE.
Administração não pode revisar, decidir justificativa ou corrigir seu próprio
registro; outro administrador deve decidir. Correções invalidam a aprovação vigente.
Em presença, o intervalo corrigido deve estar dentro do encontro; cancelamento
permite sanear período existente, mas impede nova presença ou aprovação de horas.
Sobreposição reverte a correção e a decisão por inteiro.

## Apuração e histórico

| Método e caminho | Consulta |
|---|---|
| GET `/me/participation-summary` | from/to obrigatórios; Summary próprio. |
| GET `/attendance/summary` | Administração; memberId e from/to obrigatórios. |
| GET `/me/participation-history` | Histórico próprio; from/to opcionais em par. |
| GET `/attendance/history` | Administração; memberId e from/to opcionais. Sem memberId inclui agenda e registros administrativos. |

Dias são interpretados no fuso configurado pelo Clock (inicial America/Bahia).
Consultas inclusivas por dia correspondem ao intervalo semiaberto
`[início de from, início de to+1)`. Duração é recortada nesse intervalo,
inclusive em registros atravessando meia-noite. Datas invertidas retornam 400.

Summary: zone, from, to (instantes normalizados), registeredSeconds,
validatedSeconds, pendingSeconds, refusedSeconds, openPeriods, pendingRecords,
expectedMeetings, attendedMeetings e absentMeetings. Durações em segundos inteiros;
registeredSeconds soma fechados (incluindo pendentes/recusados); validatedSeconds
soma somente APROVADA; abertos não somam duração. Encontros cancelados são excluídos
da apuração, mantendo registros consultáveis. Justificativas não criam períodos.

Frequência objetiva conta encontros ENCERRADO iniciados no período em que a pessoa
era esperada. attendedMeetings é presença registrada, não homologação acadêmica;
absentMeetings = expectedMeetings - attendedMeetings. Não há percentual oficial,
punição, mínimo institucional ou abono implícito. Não esperados não geram ausência.

History: id, entityType, entityId, memberId, actorId, action, entityVersion,
beforeValue/afterValue (JSON estruturado), reason, occurredAt. Somente autor e
administração acessam histórico pessoal; agenda com expectativas somente no
histórico administrativo. Conteúdo reservado não é replicado na auditoria global,
que contém apenas ação, entidade e versão. Histórico é imutável no banco.
