# Bloco 07 — contratos HTTP

Todos os endpoints exigem token Entra Bearer validado pela API. Datas usam `YYYY-MM-DD`; intervalos de relatório são inclusivos e interpretados no fuso configurado da aplicação. Intervalos aceitos: até 366 dias. Nenhum endpoint infere quinta-feira, fuso institucional ou fechamento oficial.

## Dashboards

| Endpoint | Escopo |
|---|---|
| `GET /api/v1/me/dashboard?from=&to=` | Próprios registros, atividades e situação atual das atividades atribuídas ao integrante. |
| `GET /api/v1/projects/{projectId}/dashboard?from=&to=` | Liderança contextual vigente do projeto ou Gestão Geral/Coordenação. |
| `GET /api/v1/analytics/dashboard?from=&to=` | Gestão Geral/Coordenação, para o conjunto de projetos visíveis administrativamente. |

As respostas incluem período, instante de geração, indicadores, registros de apoio e `unavailableMetrics`. Participação é separada por modalidade e situação de revisão. `validatedSeconds` contabiliza somente registros aprovados; dispensas reduzem a obrigação em `/me/participation-weekly-summary` e nunca são horas trabalhadas. Atrasos são calculados com base no estado atual na geração. O painel de projeto não atribui presenças sem vínculo de projeto.

## Relatórios

Modelos (`{type}`):

- `FREQUENCIA_PARTICIPACAO`: lançamentos com pessoa, modalidade, tipo, estado e duração recortada; não inclui justificativa ou nota privada.
- `ATIVIDADES_ENTREGAS`: atividade, projeto, executor atual, situação atual, criação, prazo, conclusão e atraso atual.
- `ACOMPANHAMENTO_PROJETOS`: estado oficial atual, indicadores de atividade, tamanho atual da equipe e classificação explicável de criticidade.
- `GERENCIAL_CONSOLIDADO`: visão por projeto com os indicadores operacionais agregados. Não incorpora notas/resultados acadêmicos do Bloco 06.

Consultas JSON:

- `GET /api/v1/me/reports/{type}?from=&to=&projectId=&situation=&page=0&size=50`
- `GET /api/v1/projects/{projectId}/reports/{type}?from=&to=&situation=&page=0&size=50`
- `GET /api/v1/analytics/reports/{type}?from=&to=&projectId=&memberId=&situation=&page=0&size=50`

Os dois primeiros são limitados ao próprio integrante ou à liderança do projeto. Filtros `projectId`/`memberId` são administrativos no endpoint gerencial. `situation` aceita os estados individuais do modelo solicitado (`PENDENTE/APROVADA/RECUSADA`, estados B03 de atividade ou estados oficiais B02 de projeto). `page` começa em zero; `size` aceita 1–200. A resposta informa `total`, critérios, colunas, linhas e limitações da fonte. As projeções de projeto/atividade exibem estado atual, não reconstroem um estado histórico sem snapshot.

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

- `GET /api/v1/me/participation-weekly-summary?from=&to=` recebe exatamente sete dias consecutivos explícitos. Apresenta carga-base 5h presencial/4h remota, obrigação líquida por modalidade, registrado, pendente, aprovado, recusado, períodos abertos e saldo após validação.
- `POST /api/v1/attendance/calendar-exceptions` e `GET /api/v1/attendance/calendar-exceptions?from=&to=&page=0&size=50` administram exceções versionadas e auditadas. O intervalo deve conter sete dias; valores de dispensa não viram horas realizadas.
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

Os limites de atraso contam dias de calendário desde o dia seguinte ao prazo; os de inatividade contam períodos completos de 24 horas. “Atualização relevante” é o evento auditável mais recente de criação/alteração da atividade, mudança de status ou executor/colaborador, ou inclusão/retirada de evidência. A consulta usa `activity_events`, nunca apenas `activities.updated_at`. Se faltar evento B03, o tempo é `null` e, sem outro sinal de atraso suficiente, a classificação é `DADOS_INSUFICIENTES`. A política é configurável individualmente pela Gestão Geral/Coordenação, sem alterar estados oficiais do projeto.

## Limites das integrações

O serviço de documentos B05 não está implementado no backend. Evidências/Diários de Bordo são informados como indisponíveis; anexos B03 não os substituem. Reuniões usam os dados próprios de agenda e períodos B04; Google Meet não é integrado. Alertas com destinatário, silenciamento ou disparo automático e métricas acadêmicas não são criados por esta camada.
