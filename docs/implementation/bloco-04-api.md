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

## Consultas e ações pessoais consolidadas

Todas as rotas `/me` exigem integrante ATIVO e limitam a consulta ao membro do
token. Listas usam `{items,page,size,total}`, ordenação determinística e page
iniciado em zero; filtros `from` e `to`, quando aceitos como par, são datas
inclusivas no fuso configurado. O limite `to=LocalDate.MAX` é inválido.

| Método e caminho | Permissão e uso | Payload / resposta relevantes |
|---|---|---|
| GET `/me/participation/expected-events?from=&to=&page=&size=` | Próprios encontros para os quais há expectativa individual; filtra a data inicial do evento. | `ExpectedEvent`: dados do evento, modalidade, status, `expected`, `checkedIn`, `attendanceId`. Não inclui evento inesperado. |
| GET `/me/participation/timeline?from=&to=&page=&size=` | Próprios períodos de presença e lançamentos remotos, inclusive ligados a evento cancelado. | `TimelineItem`: evento quando houver, período, modalidade, intervalo, `durationSeconds`, atividade/evidência autorizadas, origem, `reviewStatus`, `periodStatus`, `replacementAdjustmentId`, revisão e versão. |
| GET `/me/participation/calendar-exceptions?from=&to=&page=&size=` | Histórico global de exceções oficiais que afetam a semana do integrante, em projeção sanitizada. | Traz horas dispensadas, tipo, período e títulos públicos dos eventos relacionados; não expõe motivo administrativo, criador nem outros campos internos. |
| GET `/me/participation-replacements?page=&size=` | Solicitações de reposição do integrante. | `ReplacementView`: decisão e prazo, períodos associados, situação atual de cada período, segundos reconhecidos e saldo restante. |
| GET `/me/participation-adjustments?page=&size=` | Solicitações e dispensas próprias. | `AdjustmentView`; não inclui alocações de períodos, para isso usar a rota de reposições. |
| POST `/me/work-logs/review-requests/{id}/responses` | Integrante dono do lançamento remoto, enquanto aguarda resposta. | `{expectedPeriodVersion,startsAt,endsAt,description,activityId?,evidenceId?,reason}`. Registra proposta versionada; não altera imediatamente o lançamento. |

`periodStatus` é a situação funcional exposta à pessoa: `ABERTO` quando sem saída;
`CANCELADO` para registro ligado a evento cancelado; `VALIDADO`/`RECUSADO` após
decisão; `DECLARADO` para lançamento remoto ainda pendente; e `PENDENTE` para
presença fechada sem aprovação. O `reviewStatus` original continua disponível
separadamente. Duração é nula para período aberto ou ligado a evento cancelado;
eventos cancelados continuam no histórico para explicar o registro, mas não entram
nos totais de horas dos resumos ou relatórios.

`expected-events` e `timeline` não são equivalentes: a primeira consulta mostra
expectativas de agenda; a segunda mostra somente períodos registrados. A pessoa
pode iniciar check-in sem constar como esperada se houver evento `ABERTO` e QR
válido. O payload do QR é transitório, tem janela técnica máxima de 120 segundos e
é transmitido ao backend junto com `eventId`; o frontend não deve gerar nem confiar
em um QR local. Check-in, checkout e retorno permanecem sujeitos a versão,
intervalo, modalidade do evento, sobreposição e sessão válida conforme as regras
acima.

## Correção de lançamento remoto e reposição

| Método e caminho | Permissão e efeito |
|---|---|
| GET `/me/work-logs/review-requests?page=&size=` | Próprios pedidos pendentes e encerrados, com lançamento atual e resposta, se enviada. |
| POST `/me/work-logs/review-requests/{id}/responses` | Envia uma única proposta enquanto o pedido está em `AGUARDANDO_RESPOSTA`; exige período próprio, remoto, fechado, pendente e `expectedPeriodVersion` atual. |
| GET `/attendance/work-log-review-requests?memberId=&page=&size=` | Somente Gestão Geral/Coordenação; fila administrativa com membro, solicitação, lançamento atual e resposta. |
| POST `/attendance/work-log-review-requests/{id}/decisions` | Somente Gestão Geral/Coordenação e outro ator que o integrante; `{expectedResponseVersion,decision:ACEITA|RECUSADA,reason,confirmed:true}`. |
| GET `/me/participation-replacements?page=&size=` | Próprias reposições, alocações e saldo. |
| POST `/me/participation-replacements` | Solicita `{weekStart,weekEnd,modality,hours,reason}`; a semana deve ter sete dias consecutivos. |
| POST `/attendance/participation-replacements/{id}/decisions` | Administração decide; aceitar exige modalidade igual à solicitada, horas não superiores às solicitadas e prazo posterior à semana de origem. |
| POST `/attendance/participation-replacements/{id}/periods/{periodId}` | Administração associa um período do próprio integrante, fechado, aprovado, na modalidade autorizada, após a semana de origem e até o prazo. |

Uma correção aceita substitui intervalo, descrição e referências de atividade/evidência
do lançamento, mantendo-o `PENDENTE` para revisão normal. As referências são
revalidadas no envio e na decisão por `ActivityParticipation`; indisponibilidade ou
falta de autorização não revela o recurso e não cria upload novo. Recusa preserva
o lançamento. Proposta e decisão ficam em tabelas imutáveis, com versões e eventos
de histórico. A pessoa acompanha o resultado em `review-requests`; a revisão de
horas continua no fluxo administrativo de revisão individual ou semanal.

Horas de reposição são associação de um período já aprovado e elegível, não horas
adicionais. `recognizedSeconds` só conta o período enquanto seu estado atual for
`APROVADA`; `remainingSeconds` é nulo enquanto a solicitação não estiver autorizada
e passa a ser autorizado menos reconhecido depois da decisão. Reposição não altera
o resumo da semana de origem e não duplica a duração do período nos totais de tempo.
Dispensa é apresentada em ajuste semanal e reduz a obrigação somente quando houver
política homologada aplicável; nunca cria período nem hora realizada.

## Política de carga horária e histórico de exceções pessoais

`GET /me/participation-weekly-summary?from=&to=` exige exatamente sete dias e
retorna dados por modalidade, exceção de calendário segura, ajustes pessoais e
`loadPolicy`. A confirmação institucional fixa 5 horas presenciais e 4 remotas,
mas não ativa uma vigência por si só. A resposta usa `institutionalConfirmation`
com estado `CONFIRMADA`; não usa mais `provisionalReference` nem classifica esses
parâmetros como pendentes de homologação. `requiredSeconds` e `remainingSeconds`
continuam nulos até existir uma versão ativada que cubra integralmente a semana.
Sem cobertura, `loadPolicy.status` é `SEM_POLITICA_APLICAVEL`; configurações
parciais, sucessivas ou sobrepostas mantêm estados distintos. Nenhuma data é
inferida da data atual, da semana consultada ou da confirmação institucional.

A administração global (`GESTOR_GERAL` ou `COORDENADOR`) gerencia versões por:

| Método e caminho | Entrada / efeito |
|---|---|
| GET `/attendance/participation-load-policies?page=&size=` | Histórico administrativo de versões e eventos; inclui autores, horários, motivos, vigência e referência de aprovação. |
| POST `/attendance/participation-load-policies` | Cria rascunho com `{presentialHours,remoteHours,effectiveFrom?,intendedSemester?,effectiveWindow?,reason}`; a primeira configuração aceita somente 5h/4h. Para planejamento sem data, informe semestre e janela oficial. |
| POST `/attendance/participation-load-policies/{id}/revisions` | Cria uma nova versão imutável com `{expectedVersion,presentialHours,remoteHours,effectiveFrom?,intendedSemester?,effectiveWindow?,reason}` e marca a anterior como substituída. |
| POST `/attendance/participation-load-policies/{id}/approvals` | Aprova o rascunho com `{expectedVersion,sourceReference,reason}`; registra ator e motivo. |
| POST `/attendance/participation-load-policies/{id}/activations` | Ativa somente versão aprovada com `{expectedVersion,reason}`. A vigência usada é a data explícita registrada no rascunho. |

Os mesmos papéis globais (`GESTOR_GERAL` ou `COORDENADOR`) podem criar, revisar,
aprovar e ativar; não foi criada separação de funções entre esses papéis. Cada
ação é gravada com ator, motivo, referência institucional e evento append-only.
Um plano 5h/4h para `2027.1` pode ser criado e aprovado com
`effectiveFrom=null`, `intendedSemester="2027.1"` e
`effectiveWindow="PRIMEIRA_SEMANA_OFICIAL_DE_ATIVIDADES"`. A resposta pessoal
inclui `plannedConfiguration` com estado `SEM_VIGENCIA_DEFINIDA`; exigência e
saldo continuam nulos. Ativação sem data é recusada. Quando o calendário oficial
definir a data, uma nova revisão datada deve ser aprovada e ativada; a data nunca
é inferida e a política não alcança semanas anteriores. Ativações em data anterior
à última vigência são recusadas. Uma versão já ativada não pode ser editada.
Sem versão aplicável, os registros, pendências, validações, recusas e períodos
abertos permanecem consultáveis, mas não há obrigação ou saldo semanal calculado.

## Comprovantes privados de justificativas

Formatos aceitos são PDF, JPG/JPEG e PNG, até 5.000.000 bytes por arquivo. O limite
é de 5 anexos ativos por justificativa e 10 por integrante. Isso corresponde a
25.000.000 bytes por justificativa e 50.000.000 bytes por integrante no máximo,
calculados como quantidade × 5.000.000 bytes. Uploads pendentes reservam vaga e
tamanho atomicamente; uma falha com limpeza do objeto concluída libera ambos. Reservas
`PENDENTE` deixadas por interrupção são reconciliadas na inicialização e antes do próximo
upload do integrante; se o armazenamento não permitir limpar o objeto, a reserva permanece
ativa até uma nova tentativa de recuperação. Itens `FALHA` e `REMOVIDO` não ocupam
quota nem aparecem na lista operacional, enquanto seus eventos permanecem no histórico.
O envio é opcional. O backend valida extensão, MIME, assinatura/conteúdo e tamanho. Não há
URLs públicas; download autenticado responde `private, no-store` e `nosniff`.
O titular consulta e envia apenas em justificativa própria. Download administrativo
e gestão de retenção exigem Gestão Geral/Coordenação. O contexto `COMPLEMENTACAO`
só é aceito quando a justificativa está em `COMPLEMENTACAO_SOLICITADA`; o contexto
`JUSTIFICATIVA` só no estado `PENDENTE`.

| Método e caminho | Permissão e efeito |
|---|---|
| GET `/me/absence-justifications/{id}/attachments` | Titular; lista metadados e estado da capacidade de upload. |
| POST `/me/absence-justifications/{id}/attachments` | Titular; multipart `file`, `context` opcional (`JUSTIFICATIVA` por padrão). |
| GET `/me/absence-justifications/{id}/attachments/{attachmentId}/download` | Titular e somente comprovante da justificativa própria. |
| GET `/attendance/justifications/{id}/attachments[/{attachmentId}/download]` | Gestão Geral/Coordenação; consulta e download autenticado. |
| GET/POST `/attendance/justification-attachment-retention-policies` | Administração; histórico paginado e proposta versionada. A primeira proposta exige 24 meses; todas as propostas e revisões registram carência de exatamente 30 dias. |
| POST `/attendance/justification-attachment-retention-policies/{id}/revisions` | Administração; cria versão imutável da proposta ainda não validada. |
| POST `/attendance/justification-attachment-retention-policies/{id}/validations` | Só UUIDs explicitamente configurados em `JUSTIFICATION_RETENTION_AUTHORIZED_VALIDATOR_IDS`; valida também referências de tratamento de backup e dados sensíveis. |
| POST `/attendance/justifications/{id}/retention-classifications` | Administração; registra semestre e fonte oficial após análise definitiva. |
| POST `/attendance/justification-attachments/{id}/retention-holds[/{holdId}/releases]` | Administração; registra conservação excepcional e liberação com motivo. |
| GET `/attendance/justification-attachments/{id}/retention-holds` | Administração; consulta eventos de aplicação/liberação com fundamento, autoria e motivo. |
| POST `/attendance/justification-attachments/{id}/deletions` | Administração; descarte manual, confirmado, após expiração e sem hold ativo. |

Upload permanece indisponível por padrão: exige diretório privado configurado em
`JUSTIFICATION_ATTACHMENT_LOCAL_ROOT`, aprovação institucional explícita via
`JUSTIFICATION_ATTACHMENT_STORAGE_APPROVED=true` e política de retenção validada por
identidade autorizada. O sinal de aprovação é independente da existência do diretório:
configurar o adaptador local não significa que ele foi aprovado para uso institucional.
O único provider existente é o adaptador local opt-in; não foi encontrada infraestrutura
privada institucional provisionada neste repositório. Ele é separado das evidências B03.
Sem qualquer condição, upload e download permanecem indisponíveis; justificativas sem
arquivos continuam válidas.

A proposta calcula vencimento após o período de retenção da data mais recente entre
encerramento definitivo da análise e fim do semestre classificado; `deletionDeadlineOn`
é o vencimento da retenção mais uma carência mínima de 30 dias. A exclusão manual e o
expurgo só podem apagar o objeto na data-limite ou depois dela. Exclusões manuais e agendadas
compartilham um lock de sessão, e uma classificação de retenção não pode mudar enquanto há
uma exclusão pendente. Registros de políticas
anteriores com valor menor também recebem o piso efetivo de 30 dias. A rotina identifica somente comprovantes de
justificativas definitivamente aceitas/recusadas, com classificação oficial de fim de
semestre, política de origem validada e sem retenção excepcional ativa. Estados de
upload pendente, falho e pedidos de justificativa ainda pendentes ficam de fora.

| Método e caminho | Permissão e efeito |
|---|---|
| POST `/attendance/justification-attachment-retention-purges` | Gestão Geral/Coordenação; recebe `{mode:"DRY_RUN"|"EXECUTE",confirmed,reason}`. Execução exige confirmação explícita, política validada, armazenamento aprovado e `JUSTIFICATION_ATTACHMENT_RETENTION_PURGE_EXECUTION_ENABLED=true`. |
| GET `/attendance/justification-attachment-retention-purges?page=&size=` | Gestão Geral/Coordenação; histórico paginado de execuções, contagens e resultado. |
| GET `/attendance/justification-attachment-retention-purges/{runId}` | Gestão Geral/Coordenação; resultado por comprovante, sem caminho de arquivo ou conteúdo. |

Execuções registram cabeçalho, eventos e resultado por anexo em histórico append-only.
Uma falha de armazenamento restaura o estado disponível e pode ser repetida; se a
exclusão do objeto concluir mas a confirmação no banco falhar, o estado pendente permite
repetir a exclusão idempotente e concluir a remoção do metadado. A rotina não altera
registros de participação ou justificativas. Uma execução concorrente é serializada por
lock de sessão PostgreSQL. A agenda usa UTC, fica desligada por padrão e, mesmo quando
habilitada, roda em simulação por padrão. Execução real exige as configurações explícitas
`JUSTIFICATION_ATTACHMENT_RETENTION_PURGE_ENABLED=true`,
`JUSTIFICATION_ATTACHMENT_RETENTION_PURGE_EXECUTION_ENABLED=true` e
`JUSTIFICATION_ATTACHMENT_RETENTION_PURGE_DRY_RUN=false`.

O backend possui agora a rotina e a trilha técnica, mas não há garantia operacional de
descarte logo após o fim dos 30 dias mínimos enquanto agenda, provider institucional e
monitoramento não forem configurados e operados; 30 dias é o prazo mínimo de espera,
não um prazo máximo para executar o descarte. A aprovação de privacidade ainda deve validar retenção,
tratamento de dados sensíveis e referências de backup; a instituição também deve definir
como as cópias de segurança expiram e são removidas. O local opt-in não representa essa
aprovação e nenhum expurgo irreversível automático fica ativo por padrão.
