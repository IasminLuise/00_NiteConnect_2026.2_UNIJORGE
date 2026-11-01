# Bloco 07 — contratos HTTP

Todos os endpoints exigem token Entra Bearer validado pela API. Datas usam `YYYY-MM-DD`; intervalos de relatório são inclusivos e interpretados no fuso configurado da aplicação. Intervalos aceitos: até 366 dias. Nenhum endpoint infere quinta-feira, fuso institucional ou fechamento oficial.

## Dashboards

| Endpoint | Escopo |
|---|---|
| `GET /api/v1/me/dashboard?from=&to=` | Próprios registros, atividades e situação atual das atividades atribuídas ao integrante. |
| `GET /api/v1/projects/{projectId}/dashboard?from=&to=` | Liderança contextual vigente do projeto ou Gestão Geral/Coordenação. |
| `GET /api/v1/analytics/dashboard?from=&to=` | Gestão Geral/Coordenação, para o conjunto de projetos visíveis administrativamente. |

As respostas incluem período, instante de geração, indicadores, registros de apoio e `unavailableMetrics`. Participação é separada por modalidade e situação de revisão. `participation.open` conta períodos sem saída em métrica própria; períodos abertos não entram em contagem pendente nem em segundos registrados/aprovados. `validatedSeconds` contabiliza somente períodos fechados aprovados. Períodos de presença ligados a evento cancelado não entram nos agregados. Dispensações reduzem a obrigação em `/me/participation-weekly-summary` somente se houver política de carga homologada aplicável e nunca são horas trabalhadas. Atrasos são calculados com base no estado atual na geração. O painel de projeto não atribui presenças sem vínculo de projeto.

Os indicadores agregados são calculados integralmente na origem e não a partir das linhas de apoio limitadas. O limite de linhas pode truncar a tabela de apoio; nesse caso, `unavailableMetrics` informa a limitação sem alterar os totais agregados. `recordsInPeriod` significa períodos sobrepostos ao intervalo nos painéis de participação e projetos no escopo nos painéis gerenciais. No painel do integrante, `activities` e seus estados contam atividades atribuídas criadas no intervalo; atrasos refletem a situação atual dessas atividades. No painel gerencial, projetos, atividades, membros ativos e classificação de saúde são fotografias do momento da consulta.

## Relatórios

Modelos (`{type}`):

- `FREQUENCIA_PARTICIPACAO`: lançamentos com pessoa, modalidade, tipo, estado de revisão, estado do período e duração recortada; não inclui justificativa ou nota privada. Colunas: `periodId`, `memberName`, `modality`, `kind`, `reviewStatus`, `periodStatus`, `startsAt`, `endsAt`, `secondsInRange`. `reviewStatus` é `PENDENTE/APROVADA/RECUSADA`; `periodStatus` é `ABERTO/DECLARADO/PENDENTE/VALIDADO/RECUSADO`. Períodos abertos aparecem com `secondsInRange=0`; registros de presença vinculados a evento cancelado são excluídos. O filtro `situation` continua aplicando-se a `reviewStatus` corrente.
- `ATIVIDADES_ENTREGAS`: atividade, projeto, executor atual, situação atual, criação, prazo, conclusão e atraso atual.
- `ACOMPANHAMENTO_PROJETOS`: estado oficial atual, indicadores de atividade, tamanho atual da equipe e classificação explicável de criticidade.
- `GERENCIAL_CONSOLIDADO`: visão por projeto com os indicadores operacionais agregados. Não incorpora notas/resultados acadêmicos do Bloco 06.

Consultas JSON:

- `GET /api/v1/me/reports/{type}?from=&to=&projectId=&situation=&page=0&size=50`
- `GET /api/v1/projects/{projectId}/reports/{type}?from=&to=&situation=&page=0&size=50`
- `GET /api/v1/analytics/reports/{type}?from=&to=&projectId=&memberId=&situation=&page=0&size=50`

Os dois primeiros são limitados ao próprio integrante ou à liderança do projeto. Filtros `projectId`/`memberId` são administrativos no endpoint gerencial. `situation` aceita os estados individuais do modelo solicitado (`PENDENTE/APROVADA/RECUSADA`, estados B03 de atividade ou estados oficiais B02 de projeto). `page` começa em zero; `size` aceita 1–200. A resposta informa `total`, critérios, colunas, linhas e limitações da fonte. As projeções de projeto/atividade exibem estado atual, não reconstroem um estado histórico sem snapshot.

Semântica temporal: `FREQUENCIA_PARTICIPACAO` inclui períodos que se sobrepõem ao intervalo e recorta a duração dentro dele; `ATIVIDADES_ENTREGAS` inclui atividades criadas no intervalo, embora prazo, conclusão e atraso sejam valores atuais; `ACOMPANHAMENTO_PROJETOS` e `GERENCIAL_CONSOLIDADO` incluem projetos do escopo autorizado, sem restringi-los pela data de criação. Estado do projeto, equipe, atividades e criticidade desses dois relatórios são valores atuais na geração. Se houver filtro de integrante, o vínculo ao projeto deve se sobrepor ao intervalo. A situação opcional filtra o estado corrente da entidade correspondente.

Exportações usam os mesmos escopos e filtros:

- `GET /api/v1/me/reports/{type}/export?from=&to=&format=CSV|PDF&projectId=&situation=`
- `GET /api/v1/projects/{projectId}/reports/{type}/export?from=&to=&format=CSV|PDF&situation=`
- `GET /api/v1/analytics/reports/{type}/export?from=&to=&format=CSV|PDF&projectId=&memberId=&situation=`

CSV usa UTF-8, quoting de campos, escaping de aspas e proteção contra fórmulas de planilha. PDF registra modelo, período, critério, instante de geração, registros e indisponibilidades. Fontes com grande volume têm limite operacional indicado em `unavailableMetrics`; reduza o período ou aplique escopo antes de exportar.

## Histórico relacional

- `GET /api/v1/projects/{projectId}/analytics/history?from=&to=&page=0&size=50`: liderança contextual ou administração.
- `GET /api/v1/analytics/history?from=&to=&projectId=&page=0&size=50`: administração.
- `GET /api/v1/me/analytics/comparison?baselineFrom=&baselineTo=&comparisonFrom=&comparisonTo=&projectId=`: próprio integrante, opcionalmente dentro de projeto acessível.
- `GET /api/v1/projects/{projectId}/analytics/comparison?baselineFrom=&baselineTo=&comparisonFrom=&comparisonTo=`: liderança contextual ou administração.
- `GET /api/v1/analytics/comparison?baselineFrom=&baselineTo=&comparisonFrom=&comparisonTo=&projectId=&memberId=`: administração.

O histórico pagina eventos de mudança de estado de projeto e eventos de atividade efetivamente preservados, com IDs de origem, ação, instante e estados conhecidos. Não inventa snapshots anteriores nem converte o estado atual em fato passado. A comparação exige janelas inclusivas não sobrepostas com a mesma duração e apresenta contagens de criações de atividade, eventos B03 preservados, períodos iniciados e, quando não filtrada por integrante, mudanças de estado de projeto. Essas contagens de eventos não constituem indicador de desempenho homologado nem reconstroem situação histórica completa.

## Participação semanal B04

- `GET /api/v1/me/participation-weekly-summary?from=&to=` recebe exatamente sete dias consecutivos explícitos. Apresenta registro, pendente, aprovado, recusado e períodos abertos por modalidade, além de `loadPolicy`. A confirmação institucional dos parâmetros é `institutionalConfirmation` (`CONFIRMADA`, 5h presencial/4h remota), separada da vigência semanal. Sem uma versão ativada que cubra a semana inteira, `requiredSeconds` e `remainingSeconds` são nulos e não são exibidos como zero no dashboard; a obrigação líquida e o saldo só aparecem com política aplicável. `plannedConfiguration` representa plano futuro e seu estado: `SEM_VIGENCIA_DEFINIDA`, `AGUARDANDO_ATIVACAO` ou `ATIVADA_PARA_FUTURO`. O dashboard informa plano e data pendente/disponível sem antecipar exigência nem saldo. A política administrativa tem versões e eventos append-only; a aprovação admite plano sem data, mas a ativação exige data explícita e referência institucional.

Os comprovantes opcionais de justificativas têm fluxo B04 próprio, separado de evidências B03. A lista pessoal retorna estado de disponibilidade e metadados; upload/download são limitados ao titular e downloads são privados. Formatos aceitos: PDF/JPG/JPEG/PNG, até 5.000.000 bytes. Upload depende de armazenamento privado configurado, aprovação institucional explícita e política de retenção validada por responsável autorizado. O dashboard B07 não recebe nem expõe documentos ou seus conteúdos.
- `POST /api/v1/attendance/calendar-exceptions` e `GET /api/v1/attendance/calendar-exceptions?from=&to=&page=0&size=50` administram exceções versionadas e auditadas. O intervalo deve conter sete dias; valores de dispensa não viram horas realizadas.

Para cancelamento e remarcação, o encontro presencial cancelado que justifica a exceção deve ocorrer dentro da semana declarada, interpretado no fuso configurado da aplicação. O novo encontro de uma remarcação deve continuar presencial, não cancelado e posterior ao original.
- `POST /api/v1/attendance/work-log-review-batches` recebe de 1 a 100 decisões por período pendente fechado na semana explícita: `APROVAR`, `REJEITAR` ou `CORRECAO_SOLICITADA`. Aprovação/recusa atualiza cada registro pelo fluxo individual existente; pedido de correção preserva o lançamento pendente e registra o item do lote.
- `GET /api/v1/me/work-logs/review-requests` lista pedidos de correção dirigidos ao próprio integrante.
- `POST /api/v1/me/participation-replacements` abre solicitação de reposição por modalidade e horas. `POST /api/v1/attendance/participation-replacements/{id}/decisions` registra decisão administrativa com modalidade, quantidade e prazo somente quando autorizada.
- `POST /api/v1/attendance/participation-dispensations` registra dispensa individual aprovada pela administração, sem gerar período de trabalho.
- `POST /api/v1/attendance/participation-replacements/{id}/periods/{periodId}` associa reposição realizada a período aprovado compatível, dentro do prazo e sem ultrapassar a carga autorizada.
- `GET /api/v1/me/participation-adjustments` lista decisões próprias; `GET /api/v1/attendance/participation-adjustments?memberId=&page=0&size=50` é administrativo.

Justificativas simples continuam no fluxo B04 existente e não reduzem carga nem criam horas. Nenhum prazo vencido produz consequência automática.

## Criticidade de projeto

- `GET /api/v1/projects/{projectId}/health`: participante do projeto ou administração; apresenta situação, motivos, versão de política, parâmetros e fontes atuais.
- Cada projeto recebe automaticamente uma política baseline versionada: atraso em atividade aberta = Atenção a partir de 7 dias e Crítico a partir de 14; sem atualização relevante = Atenção a partir de 14 dias e Crítico a partir de 30. Projetos criados futuramente recebem a mesma versão inicial.
- `POST /api/v1/projects/{projectId}/health-policy`: somente Gestão Geral/Coordenação; exige `expectedVersion`, limiares positivos e motivo. Atenção deve ser inferior a Crítico. Cada versão é preservada; o histórico distingue baseline aprovada de configuração pela Gestão.
- `GET /api/v1/projects/{projectId}/health-policy/history?page=0&size=50`: administração.
- `POST /api/v1/projects/{projectId}/health-evaluations`: liderança contextual ou administração; persiste avaliação e fontes imutáveis.
- `GET /api/v1/projects/{projectId}/health-history?page=0&size=50`: participante autorizado ou administração.

Os limites de atraso contam dias de calendário desde o dia seguinte ao prazo apenas para atividades abertas; os de inatividade contam períodos completos de 24 horas. “Atualização relevante” é o evento auditável mais recente de criação/alteração da atividade, mudança de status ou executor/colaborador, ou inclusão/retirada de evidência, em atividades do projeto não canceladas, incluindo entregas concluídas. A consulta usa `activity_events`, nunca apenas `activities.updated_at`. Atividades concluídas contribuem para a atualização recente, mas não para o cálculo de atraso. Se faltar evento B03, o tempo é `null` e, sem outro sinal de atraso suficiente, a classificação é `DADOS_INSUFICIENTES`. A política é configurável individualmente pela Gestão Geral/Coordenação, sem alterar estados oficiais do projeto.

## Limites das integrações

O serviço de documentos B05 não está implementado no backend. Evidências/Diários de Bordo são informados como indisponíveis; anexos B03 não os substituem. Reuniões usam os dados próprios de agenda e períodos B04; Google Meet não é integrado. Alertas com destinatário, silenciamento ou disparo automático e métricas acadêmicas não são criados por esta camada.
