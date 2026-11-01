# Bloco 04 — Agenda e presença por encontro

## Decisão de substituição de escopo — 2026-11-01

A funcionalidade antiga **Minha participação** foi descontinuada por decisão de produto. O trabalho atual removeu seus consumidores, serviços e contratos de horas, carga semanal, justificativas/comprovantes, correções, dispensas, reposições e revisões. A política anterior de 5h presenciais + 4h remotas deixa de ser requisito ativo e não deve ser apresentada como obrigação.

O escopo B04 ativo fica limitado à Agenda, sessões autorizadas e registro autenticado de presença por QR temporário. Os dados históricos permanecem preservados; a migração V20 documenta o estado sem apagar esquema, linhas ou arquivos.

## Nova definição preliminar — Frequência

**Frequência** será definida em etapa própria. A direção preliminar é confirmar presença por QR temporário em encontros autorizados do NITE, permitir consulta pessoal de assiduidade e restringir acompanhamento coletivo à Coordenação e Gestão Geral. Nenhum cálculo, percentual, classificação de falta, obrigação de horas ou nova política foi aprovado por este registro. Não há página Frequência nem endpoints coletivos ativos.

No shell, Meu espaço está preparado nesta ordem: Minhas atividades, Meus projetos, Agenda e Frequência. Os três módulos sem rota aparecem desabilitados, sem navegação para URL inexistente.

## Regras de preservação vigentes

- QR é disponibilizado por sessão autorizada e lido pelo integrante no encontro; validade é temporária e validada no servidor.
- A presença é do integrante autenticado; o retorno exige titularidade, sessão QR válida e versão atual.
- Uma presença lógica por encontro/integrante; saída/retorno permanecem como segmentos do mesmo registro.
- Agenda e sessões são administradas por papéis institucionais autorizados; consulta pessoal não revela a lista de outras pessoas.
- O leitor interno B06 pode capturar presença individual como evidência acadêmica segundo autorização própria. Ele não contabiliza horas.
- Presenças, eventos de auditoria, migrações e anexos históricos não foram apagados.

## Estado do banco e próximos gates

V1–V19 permanecem imutáveis. V5 criou `calendar_events`, `event_participants`, `attendance_records`, `participation_periods` (mista), justificativas/correções e trilhas. V12–V19 acrescentaram tabelas exclusivas ao domínio de horas, revisão e comprovantes. V20 apenas classifica as tabelas por comentário; as APIs/serviços foram retirados e os dados foram preservados.

Antes de qualquer migração destrutiva, inventariar dados por tabela, distinguir produção de dados sintéticos, verificar referências B06 e avaliar binários/cópias de segurança de comprovantes. A decisão de retenção e descarte precisa de autorização institucional específica. Uma migração futura deve preservar `attendance_records`, segmentos vinculados a `attendance_id` e trilhas de auditoria utilizadas pela Frequência.

## Verificação desta descontinuação

A regressão corrente cobre Agenda, sessão/QR, check-in, saída, retorno, histórico pessoal e isolamento de integrante; B06 lê presença sem métricas de horas; B07 dashboards/relatórios deixam de consultar indicadores de participação. Resultados atuais estão registrados no relatório de entrega da tarefa, e não devem ser inferidos de logs históricos de execução anteriores.
