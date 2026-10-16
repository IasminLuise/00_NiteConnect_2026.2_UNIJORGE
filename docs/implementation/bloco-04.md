# Bloco 04 — Agenda, presença e períodos de atuação

## Base e precedência (B04.1)

Base: main 1c0252a9, Blocos 01–03. Planejamento fornecido na conversa e
PDF consolidado v1.0, conferido diretamente com pypdf. Páginas 4–5, 7, 9–11,
14–15: RF-001/002/003/014/018/022–027/052, F02/F03 e CA-04–07/16.
O PDF exige QR temporário por encontro, ocorrências de saída/retorno, pedidos
de correção e complementação de justificativa, ausentes da proposta inicial.
Não implementar presença genérica de laboratório. Backend somente.

## Decisões funcionais

As propostas do planejamento são a base autorizada pela solicitação de execução.
Saída esquecida, autoridade de revisão e anexos foram confirmados expressamente
na conversa durante B04.1. Nenhuma decisão abaixo homologa norma acadêmica.

| ID | Regra adotada |
|---|---|
| D01 | Integrante ATIVO registra apenas sua presença por QR de encontro oficial; administração administra agenda e sessões. |
| D02 | Entrada/saída usam Clock do servidor. Saída esquecida mantém período aberto, sem duração consolidada até correção administrativa; não fechar automaticamente. Retorno é ocorrência da mesma presença. |
| D03 | Trabalho remoto declarado em intervalo fechado passado, descrição e atividade/evidências opcionais. Vínculos precisam ser acessíveis e próprios; declaração não é comprovação. |
| D04 | Somente GESTOR_GERAL/COORDENADOR ATIVO revisa registros; revisão explícita não equivale a abono ou cumprimento institucional. Não revisar o próprio registro. |
| D05 | Registros iniciam PENDENTE; APROVADA/RECUSADA por decisão administrativa com motivo. Somente intervalos fechados aprovados entram na duração validada. Correção invalida aprovação vigente. |
| D06 | Rejeitar intervalos sobrepostos de presença e trabalho do mesmo integrante, inclusive em concorrência. Intervalos semiabertos [início,fim); adjacência permitida. |
| D07 | Participação esperada explícita; ausência objetiva somente após encontro encerrado, não cancelado, com expectativa e sem presença. Sem punição automática. |
| D08 | Justificativa PENDENTE, COMPLEMENTACAO_SOLICITADA, ACEITA ou RECUSADA; complementação pelo autor reabre análise. Não gera presença/horas. |
| D09 | Gestor de projeto não ganha acesso a frequência de terceiros. Agenda comum de ativos; lista de participantes/check-ins restrita à administração. |
| D10 | Sem mínimo de horas, tolerância, prazo para justificar, compensação, desligamento, sanção ou nota. Feriados e encontros extraordinários são categorias de calendário, sem abono. |
| D11 | Pedido pessoal e correção administrativa confirmada, versionada e motivada; manter snapshots originais no histórico imutável. Correção pode criar presença ausente após pedido autorizado. |
| D12 | Comprovantes de justificativa desabilitados até decisão de armazenamento e privacidade. Não reutilizar arquivos locais do Bloco 03. Evidências de atividade continuam sob suas autorizações originais. |

## Arquitetura e integridade

Módulo attendance com agenda, períodos, análise e consultas. JDBC/Flyway,
Clock já configurado (America/Bahia), IdentityDirectory, AccessService,
MemberDirectory, contrato explícito ActivityParticipation e AuditTrail.
V5 aditiva; V1–V4 imutáveis. Uma presença lógica por encontro/integrante;
intervalos separados permitem saída/retorno sem duplicar check-in.
Históricos append-only com snapshots estruturados e auditoria na mesma transação.
Não copiar motivos pessoais em auditoria global: registrar metadados mínimos;
detalhes reservados ficam no histórico pessoal/administrativo do módulo.

Escritas usam AdministrativeTransactions.lock para revalidar acesso após adquirir
o lock e serializar revogações com escritas dos blocos anteriores. Inicialmente
serializa operações; contenção é limitação conhecida. Banco também serializa
intervalos por integrante e impede sobreposição inclusive em SQL concorrente.
Versões otimistas em agenda, presença, períodos, justificativas e pedidos.
Consultas por período recortam intervalos nos limites [from,to) do fuso do Clock;
resumo separa frequência e duração, pendentes e validadas. Intervalos abertos
não contribuem duração. Filtros e propriedade aplicam-se antes da paginação.

QR: capacidade opaca aleatória de 256 bits, apenas hash SHA-256 persistido,
validade técnica curta de 120 segundos, limitada pelo término do encontro.
Renovação invalida token anterior; encerramento/cancelamento invalida a janela.
Somente resposta de abertura/renovação entrega payload para renderização pelo
futuro frontend; não há frontend nem imagem QR neste bloco. Payload não aparece
em histórico, auditoria ou listagem. QR não comprova localização física.

## Etapas e testes

| Etapa | Entrega | Verificação |
|---|---|---|
| B04.1 | PDF, D01–D12 e contratos | Revisão documental e rastreabilidade |
| B04.2 | V5, índices, versões, histórico | PostgreSQL 17 novo e V4→V5; preservar dados |
| B04.3 | Agenda, expectativas, QR e conferência | Permissões, versões, renovação, expiração, cancelamento |
| B04.4 | Presença, ocorrências e trabalho remoto | Duplicidade, sobreposição, retornos, revogação e concorrência |
| B04.5 | Justificativas, revisões e correções | Complementação, histórico, rollback da auditoria |
| B04.6 | Resumos/históricos e consultas | Fuso, meia-noite, recorte, zero, isolamento e paginação |
| B04.7 | Integração, revisão e entrega | Maven verify, revisão independente, PR e CI |

## Validação e entrega

Executado até a integração: 16 testes novos sem falhas, em PostgreSQL 17 via
Testcontainers. Quatro regressões reproduziram os achados antes das correções;
depois dos ajustes, 9 testes direcionados passaram (4 regressões e 5 de transação).
Falha na auditoria reverteu presença/período/histórico; teste de revogação confirmou
a espera em `pg_locks` antes do afastamento.

Revisão independente em duas leituras: quatro achados corrigidos, segunda leitura
sem novos defeitos concretos. Revisão estática não foi apresentada como teste.
Contratos em `bloco-04-api.md`.

### Verificações após o código final

Na pasta `apps/api`:

| Comando | Resultado |
|---|---|
| `mvnw.cmd --batch-mode verify` | BUILD SUCCESS; 184 testes, 0 falhas/erros/ignorados; JAR gerado. 157 regressões anteriores e 27 testes novos. |
| `mvnw.cmd --batch-mode -Dtest=AttendanceReviewRegressionTest,ParticipationTransactionsTest test` | 9 testes aprovados após os quatro ajustes da revisão; a regressão integral posterior também os executou. |
| `git diff --check` e validação UTF-8/whitespace dos novos arquivos | Sem problemas. |

Log integral local ignorado: `apps/api/target/bloco04-verify-final.log`.
O código não foi alterado após essa regressão; edições posteriores registram apenas
documentação de entrega. Não repetir bateria local por alteração documental.
CI executa novamente a suíte e valida imagem/Worker; consultar resultado do PR.
Não houve teste frontend, deploy, credenciais reais, homologação de normas ou
armazenamento institucional: não se aplicam ao backend local deste escopo.

### Rastreabilidade de testes

| Cenários da proposta | Cobertura |
|---|---|
| T01–T06, T20 | AttendanceBoundaryTest; ParticipationIntegrityTest (JWT, próprio integrante, situação, saída/retorno, isolamento). |
| T07–T09 | ParticipationTransactionsTest; AttendanceReviewRegressionTest (agenda, versão, esperado/não esperado, cancelamento e encerramento). |
| T10–T12 | ParticipationReviewsTest (pedido, complementação, decisão e ausência de horas artificiais). |
| T13–T16 | ParticipationIntegrityTest e ParticipationReviewsTest (remoto, atividade própria/inacessível e sobreposição). |
| T17, T23–T24 | ParticipationTransactionsTest (SQL concorrente, rollback de auditoria e revogação com espera real no lock). |
| T18–T19, T25–T26 | ParticipationReviewsTest e ParticipationIntegrityTest (meia-noite Bahia, recorte, vazio, validação e isolamento). |
| T21–T22 | ParticipationReviewsTest e ParticipationIntegrityTest (snapshots originais, reset da aprovação e bloqueio SQL de reescrita sem histórico/exclusão). |
| T27 | Block04MigrationTest (V4→V5 preserva integrante, projeto, atividade, atribuição, evidência e auditoria); testes Spring validam banco novo. |
| T28 | Regressão integral: 184 testes aprovados. |

Também testados: QR expirado/renovado/encerrado, retorno em outra identidade,
presença simultânea em dois encontros, autoaprovação administrativa, confirmação
de correção, presença ausente por decisão e as quatro regressões da revisão.

## Refinamentos da revisão

- Presença ausente só pode ser criada por correção em encontro ABERTO/ENCERRADO.
  Agenda AGENDADO com dependentes também não pode ser reescrita.
- Conferência administrativa inclui a união de esperados e presentes; diferencia
  comparecimento sem expectativa, sem expor a lista aos integrantes.
- Administração pode encerrar encontro AGENDADO já terminado, mesmo quando perdeu
  a janela de QR. Preservar ausência objetiva sem inventar presença.
- Cancelamento permite saneamento auditado de períodos existentes, mas impede
  criação de presença e aprovação. Cancelados não entram na apuração.
- `attendedMeetings` indica presença registrada entre os esperados, não validação
  acadêmica. Duração validada é apurada separadamente em `validatedSeconds`.

## Limitações de implantação

Sem alteração de produção, .env, credenciais, dependências ou V1–V4. Backend fornece
payload do QR; renderização depende do futuro frontend. QR não comprova localização.
Lock administrativo comum serializa escritas; medir contenção antes de escalar.
Comprovantes, critérios de abono/carga horária e demais normas institucionais ficam
fora desta entrega. `nite.activities.zone` continua configurando o Clock
compartilhado; não foi renomeada, preservando compatibilidade com o Bloco 03.
