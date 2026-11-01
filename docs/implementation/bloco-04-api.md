# Bloco 04 — contrato ativo de Agenda e presença

**Escopo substituído em 2026-11-01:** a decisão institucional descontinuou Minha participação, apuração de horas e solicitações/revisões ligadas a horas. Este documento descreve apenas o contrato B04 ativo de Agenda, sessões e presença por encontro. A futura Frequência ainda não tem página, indicadores de assiduidade nem contrato coletivo.

Todas as rotas exigem token Entra Bearer. Datas de consulta usam `YYYY-MM-DD`; instantes de gravação usam ISO-8601 com fuso. A autorização de leitura exige integrante ativo; mutações de agenda e sessão exigem papel institucional autorizado no `AccessService`. Presença é sempre gravada pelo próprio integrante ativo.

## Agenda e encontros

| Método e rota | Acesso | Contrato |
|---|---|---|
| `GET /api/v1/agenda/events?from=&to=&page=0&size=20` | Integrante ativo | Eventos paginados; `from`/`to` são opcionais e limitam eventos que cruzam o intervalo. |
| `GET /api/v1/agenda/events/{id}` | Integrante ativo | Evento individual sem lista de integrantes esperados. |
| `POST /api/v1/agenda/events` | Coordenação/Gestão Geral | `{title,description,kind,modality,startsAt,endsAt,expectedMemberIds,reason}`. |
| `PATCH /api/v1/agenda/events/{id}` | Coordenação/Gestão Geral | Campos do evento, `expectedVersion` e `reason`; agenda com sessão/presença existente não pode ser reescrita. |
| `GET /api/v1/agenda/events/{id}/participants?page=0&size=20` | Coordenação/Gestão Geral | Lista expectativa e presença para conferência do encontro. |
| `POST /api/v1/agenda/events/{id}/session` | Coordenação/Gestão Geral | `{expectedVersion,reason}`; abre sessão e devolve QR temporário sem cache. |
| `POST /api/v1/agenda/events/{id}/close` | Coordenação/Gestão Geral | `{expectedVersion,reason}`; encerra a sessão/evento conforme estado atual. |
| `POST /api/v1/agenda/events/{id}/cancel` | Coordenação/Gestão Geral | `{expectedVersion,reason,confirmed:true}`; cancelamento explícito e auditado. |

Tipos atuais: `EventKind` (`ENCONTRO`, `EXTRAORDINARIO`, `FERIADO`, `EVENTO`), `Modality` (`PRESENCIAL`, `REMOTA`) e `EventStatus` (`AGENDADO`, `ABERTO`, `ENCERRADO`, `CANCELADO`). Feriado não admite participantes esperados nem sessão de presença.

## QR e presença por encontro

| Método e rota | Acesso | Contrato |
|---|---|---|
| `GET /api/v1/me/agenda/expected-events?from=&to=&page=0&size=20` | Integrante ativo | Próprios eventos esperados, status do evento e se já existe presença. `from` e `to` devem ser enviados juntos ou omitidos juntos. |
| `GET /api/v1/me/attendance?from=&to=&page=0&size=20` | Integrante ativo | Próprios registros de presença por encontro, paginados; filtros de data opcionais em par. |
| `POST /api/v1/me/attendance/check-in` | Integrante ativo | `{eventId,payload}`; grava entrada usando o QR temporário fornecido no encontro e o relógio do servidor. |
| `POST /api/v1/me/attendance/check-out` | Integrante ativo | `{expectedVersion}` da presença própria; fecha a sessão aberta. |
| `POST /api/v1/me/attendance/{id}/return` | Integrante ativo, titular do registro | `{expectedVersion,payload}`; registra retorno à mesma presença usando QR válido do encontro. |

`SessionPayload` contém `eventId`, `payload`, `expiresAt` e versão. O token é aleatório, armazenado como hash e expira em até 120 segundos ou no fim do evento, o que ocorrer primeiro. Renovar o QR invalida o anterior. A API aceita presença somente com evento em sessão aberta, dentro da janela temporal, sem cancelamento/encerramento e com QR válido. O encontro não precisa ter o integrante na lista de expectativa para registrar a presença; a conferência administrativa distingue presença esperada e não esperada.

`AttendanceView` contém `id`, `eventId`, `memberId`, `version` e segmentos `periods` com `id`, `startsAt`, `endsAt` e `version`. Um encontro gera uma presença lógica por integrante; saída e retorno mantêm essa presença e criam segmentos associados. A resposta não inclui modalidade do período, duração, horas, revisão ou saldo. A modalidade e os metadados do evento continuam no recurso de Agenda.

O serviço exige integrante ativo, confirma titularidade na rota de retorno, aplica versão esperada, serializa gravações concorrentes com `AdministrativeTransactions` e mantém auditoria. Registros de presença e trilhas existentes são preservados para a futura Frequência e para leitores intermodulares autorizados.

## Escopo descontinuado

Não há rotas ativas para lançamentos remotos, resumos ou históricos de horas, exigências semanais, política 5h/4h, justificativas, comprovantes, correções, dispensas, reposições, filas/revisões ou expurgos de anexos. As antigas rotas sob `/api/v1/me/work-logs`, `/api/v1/me/participation-*`, `/api/v1/attendance/participation-*`, `/api/v1/attendance/work-log-*`, `/api/v1/me/absence-justifications` e `/api/v1/attendance/justification-*` foram removidas e não são atendidas pela API.

O Bloco 07 não agrega presença em horas. A captura acadêmica B06 usa um leitor interno de presença por encontro, com autorização própria; não é endpoint pessoal nem contrato de jornada.

## Persistência histórica e migrações

V1–V20 permanecem como histórico append-only necessário à reconstrução. V21 separa os intervalos de presença em `attendance_segments`, a trilha de Agenda/presença em `attendance_events` e remove as tabelas e linhas do escopo antigo de horas, políticas, solicitações, justificativas, comprovantes e expurgo. A trilha global preserva eventos de Agenda/presença e B01–B03/B06/B07; eventos de período ligados à presença passam a `ATTENDANCE_SEGMENT`.

V21 não altera `calendar_events`, `event_participants`, `attendance_records` ou evidências B03. Ela não usa `CASCADE` e interrompe a migração quando um evento `PERIOD` não tem registro de origem para classificação. O SQL remove metadados e históricos armazenados no PostgreSQL; não apaga arquivos externos nem cópias de segurança, que dependem de inventário e procedimento operacional próprios.
