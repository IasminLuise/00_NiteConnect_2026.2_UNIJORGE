# Bloco 04 — Agenda e presença por encontro

## Decisão de substituição de escopo — 2026-11-01

A funcionalidade antiga **Minha participação** foi descontinuada por decisão de produto. O trabalho atual removeu seus consumidores, serviços e contratos de horas, carga semanal, justificativas/comprovantes, correções, dispensas, reposições e revisões. A política anterior de 5h presenciais + 4h remotas deixa de ser requisito ativo e não deve ser apresentada como obrigação.

O escopo B04 ativo fica limitado à Agenda, sessões autorizadas e registro autenticado de presença por QR temporário. V21 preserva os dados de Agenda, presença e sua auditoria em tabelas próprias e remove do PostgreSQL o escopo B04 descontinuado de horas, políticas, solicitações, justificativas, comprovantes e respectivas trilhas.

## Nova definição preliminar — Frequência

**Frequência** será definida em etapa própria. A direção preliminar é confirmar presença por QR temporário em encontros autorizados do NITE, permitir consulta pessoal de assiduidade e restringir acompanhamento coletivo à Coordenação e Gestão Geral. Nenhum cálculo, percentual, classificação de falta, obrigação de horas ou nova política foi aprovado por este registro. Não há página Frequência nem endpoints coletivos ativos.

No shell, Meu espaço está preparado nesta ordem: Minhas atividades, Meus projetos, Agenda e Frequência. Os três módulos sem rota aparecem desabilitados, sem navegação para URL inexistente.

## Regras de preservação vigentes

- QR é disponibilizado por sessão autorizada e lido pelo integrante no encontro; validade é temporária e validada no servidor.
- A presença é do integrante autenticado; o retorno exige titularidade, sessão QR válida e versão atual.
- Uma presença lógica por encontro/integrante; saída/retorno permanecem como segmentos do mesmo registro.
- Agenda e sessões são administradas por papéis institucionais autorizados; consulta pessoal não revela a lista de outras pessoas.
- O leitor interno B06 pode capturar presença individual como evidência acadêmica segundo autorização própria. Ele não contabiliza horas.
- Presenças e seus eventos de auditoria são preservados. Metadados de comprovantes do escopo descontinuado são removidos do banco; arquivos externos e backups não são apagados por V21.

## Estado do banco e próximos gates

V1–V20 permanecem imutáveis no histórico Flyway. V5 originalmente criou a tabela mista `participation_periods`; V21 migra seus segmentos `ATTENDANCE` para `attendance_segments`, move a trilha B04 ativa para `attendance_events` e remove as tabelas/linhas do escopo aposentado. As evidências B03 e auditoria de outros blocos são preservadas.

V21 remove metadados de anexos no PostgreSQL, mas não localiza nem apaga objetos fora do banco ou cópias de segurança. Como o inventário local não apontou metadados de anexos, nenhum arquivo pode ser vinculado com segurança a comprovantes B04 por esta migração. Um descarte externo exige inventário e procedimento próprios.

## Verificação desta descontinuação

A regressão corrente deve cobrir V20→V21 com dados sintéticos, preservação de Agenda/QR/presença e auditoria, eliminação do escopo aposentado, checks de integridade e os fluxos atuais de check-in/saída/retorno. B06 continua lendo presença por encontro; B07 não consulta horas ou solicitações descontinuadas.
