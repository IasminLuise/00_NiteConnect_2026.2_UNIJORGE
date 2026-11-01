# Bloco 07 — contratos HTTP ativos

Todos os endpoints exigem token Entra Bearer validado pela API. Datas de relatório usam `YYYY-MM-DD`, são inclusivas e interpretadas no fuso configurado. Os relatórios aceitam janelas de até 366 dias. O dashboard e as análises abaixo preservam indicadores B03/B02 independentes de B04.

**Substituição B04/B07 em 2026-11-01:** foram removidos os resumos, saldos, revisões, horas e relatórios de participação da antiga página Minha participação. Nenhum dashboard ou relatório B07 ativo consulta esses contratos. Agenda/QR continuam no B04 e não são agregados como horas.

## Dashboards

| Endpoint | Escopo |
|---|---|
| `GET /api/v1/me/dashboard?from=&to=` | Próprias atividades atribuídas e indicadores independentes do integrante. |
| `GET /api/v1/projects/{projectId}/dashboard?from=&to=` | Liderança contextual vigente do projeto ou Gestão Geral/Coordenação. |
| `GET /api/v1/analytics/dashboard?from=&to=` | Gestão Geral/Coordenação para projetos visíveis administrativamente. |

As respostas contêm período, instante de geração, indicadores, registros de apoio e `unavailableMetrics`. Contagens de atividades são calculadas sobre os dados-fonte, independentemente do limite de linhas de apoio. No painel pessoal, atividades e estados contam atividades atribuídas criadas no intervalo; atraso usa a situação atual. Painéis de projeto/gestão mostram valores operacionais atuais. Dados indisponíveis não são interpretados como zero. B04 não fornece horas ou saldo a estes painéis.

## Relatórios

Modelos ativos (`{type}`):

- `ATIVIDADES_ENTREGAS`: atividade, projeto opcional, executor atual, situação, criação, prazo, conclusão e atraso atual.
- `ACOMPANHAMENTO_PROJETOS`: estado oficial atual, indicadores de atividade, tamanho atual da equipe e criticidade.
- `GERENCIAL_CONSOLIDADO`: visão por projeto com indicadores operacionais agregados; não inclui notas/resultados acadêmicos B06.

Modelos e rotas de frequência/hora, incluindo `FREQUENCIA_PARTICIPACAO`, foram removidos do enum `ReportType` e não são aceitos.

Consultas:

- `GET /api/v1/me/reports/{type}?from=&to=&projectId=&situation=&page=0&size=50`
- `GET /api/v1/projects/{projectId}/reports/{type}?from=&to=&situation=&page=0&size=50`
- `GET /api/v1/analytics/reports/{type}?from=&to=&projectId=&memberId=&situation=&page=0&size=50`

Exportações CSV/PDF usam os mesmos escopos e filtros:

- `GET /api/v1/me/reports/{type}/export?from=&to=&format=CSV|PDF&projectId=&situation=`
- `GET /api/v1/projects/{projectId}/reports/{type}/export?from=&to=&format=CSV|PDF&situation=`
- `GET /api/v1/analytics/reports/{type}/export?from=&to=&format=CSV|PDF&projectId=&memberId=&situation=`

Rotas pessoais limitam-se ao integrante autenticado; rotas de projeto exigem liderança contextual ou administração; filtros por `memberId` são administrativos. `page` começa em zero e `size` aceita 1–200. CSV é UTF-8 e protege contra fórmulas de planilha. PDF identifica modelo, período, critério, geração e limitações.

## Histórico relacional e comparações

- `GET /api/v1/projects/{projectId}/analytics/history?from=&to=&page=0&size=50`: liderança contextual ou administração.
- `GET /api/v1/analytics/history?from=&to=&projectId=&page=0&size=50`: administração.
- `GET /api/v1/me/analytics/comparison?baselineFrom=&baselineTo=&comparisonFrom=&comparisonTo=&projectId=`: próprio integrante, opcionalmente em projeto acessível.
- `GET /api/v1/projects/{projectId}/analytics/comparison?baselineFrom=&baselineTo=&comparisonFrom=&comparisonTo=`: liderança contextual ou administração.
- `GET /api/v1/analytics/comparison?baselineFrom=&baselineTo=&comparisonFrom=&comparisonTo=&projectId=&memberId=`: administração.

O histórico retorna eventos preservados de projeto e atividade com ID, ação, instante e estados conhecidos; não inventa snapshots. A comparação exige janelas inclusivas de mesma duração, sem sobreposição, e retorna contagens de criação de atividades e eventos B03 preservados; consultas sem integrante também contam mudanças de situação do projeto. Essas contagens não são indicadores homologados de desempenho. A comparação não lê presença nem calcula horas.

## Criticidade de projeto

- `GET /api/v1/projects/{projectId}/health`: integrante do projeto ou administração; expõe situação, motivos, versão de política, parâmetros e fontes atuais.
- `POST /api/v1/projects/{projectId}/health-policy`: Gestão Geral/Coordenação; exige `expectedVersion`, limiares positivos e motivo. Cada versão é preservada.
- `GET /api/v1/projects/{projectId}/health-policy/history?page=0&size=50`: administração.
- `POST /api/v1/projects/{projectId}/health-evaluations`: liderança contextual ou administração; persiste avaliação e fontes imutáveis.
- `GET /api/v1/projects/{projectId}/health-history?page=0&size=50`: integrante autorizado ou administração.

Projetos recebem política baseline versionada: atraso de atividade aberta marca Atenção a partir de 7 dias e Crítico a partir de 14; ausência de atualização relevante marca Atenção a partir de 14 dias e Crítico a partir de 30. Atividades canceladas não compõem a origem da criticidade. Sem evento B03 suficiente, o estado pode ser `DADOS_INSUFICIENTES`. A política não altera o estado oficial do projeto.

## Limites das integrações

B05 documental permanece indisponível; evidências/Diários de Bordo não são simulados. Reuniões usam Agenda e presença por QR B04, sem integração Google Meet. Leitores acadêmicos seguem suas próprias regras e não ampliam automaticamente o acesso às fontes. O dashboard B07 não consulta nenhum contrato retirado de horas, justificativas ou revisão de presença.
