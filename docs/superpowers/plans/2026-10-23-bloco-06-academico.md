# Bloco 06 — Plano de implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans para execução nesta sessão, ou superpowers:subagent-driven-development se o usuário escolher delegação. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Implementar o núcleo acadêmico definido no contrato B06.0, sem ativar regras institucionais não confirmadas nem substituir o B05.

**Architecture:** Domínio `academic` no monólito JDBC. Leituras autorizadas fornecidas pelos módulos de origem, registros acadêmicos versionados, cálculo decimal e homologação restrita ao coordenador. Evidência documental oficial permanece indisponível até um provedor B05 real; o sistema deve representar esse impedimento sem nota artificial.

**Tech Stack:** Java 21, Spring Boot 4.1.1, Spring Security, JDBC, PostgreSQL 17, Flyway, JUnit, MockMvc e Testcontainers; sem novas dependências de produto.

**Spec:** [Contrato B06.0](../../implementation/bloco-06-contrato.md), RF-042–047, Q24–29 e CA-13/14 da especificação consolidada.

**Estado:** plano aprovado explicitamente pelo usuário, com execução nesta sessão e revisão independente ao final. Tarefas do núcleo implementadas; regressão com 209 testes aprovada. Revisão final e PR em preparação. Valores institucionais continuam sujeitos a confirmação.

## Global Constraints

- Homologação, registro/alteração de pontuação e reabertura exclusivos de `COORDENADOR` ativo; `GESTOR_GERAL` isolado recebe 403.
- Pesos **30/20/50 são exclusivamente exemplo**, sem seed, fallback ou padrão de produção.
- Política e insumos efetivamente utilizados permanecem associados à revisão; não recalcular histórico automaticamente.
- B01–B04 preservados; `academic` não lê suas tabelas internas diretamente.
- Nenhuma alteração em `.env`, credenciais, README, frontend, produção ou experimentos B05.
- Preservar V1–V5; a numeração acadêmica depende do inventário registrado em B06.0.
- Não usar `outOfOrder`, `repair`, limpeza de banco ou integração documental paralela.
- Trabalho em worktree isolado; branch de implementação `feat/bloco-06-avaliacao-academica`, derivada do contrato sobre a `main` atualizada; sem merge automático.

## Review Focus

1. Papel revogado entre autorização e escrita: transação deve serializar com alterações administrativas existentes e revalidar vínculo/papel antes de gravar.
2. Retirada/correção da evidência durante captura: avaliação deve guardar um retrato coerente no corte transacional, sem leituras independentes que misturem versões.
3. Períodos com fuso, datas extremas e contribuição parcial: intervalo explícito e validado, sem overflow ou atribuição integral de horas fora do semestre.
4. Origem ausente, upload pendente ou B05 indisponível: distinguir insuficiência de evidência de nota zero e impedir homologação incompleta.
5. Revisão concorrente com homologação/encaminhamento: versão esperada, trilha imutável e sucessão inequívoca, sem alterar a decisão já enviada.

## Convenções e interfaces compartilhadas

Diretório Java base: `apps/api/src/main/java/br/edu/unijorge/niteconnect/`. Diretório de testes: `apps/api/src/test/java/br/edu/unijorge/niteconnect/`. Os caminhos abaixo são relativos a essas bases quando iniciados por `academic/`, `members/`, `projects/`, `activities/` ou `attendance/`.

- DTOs de comandos em `academic/api/AcademicContracts.java`; records de leitura em `academic/domain/AcademicRecords.java`; estados/enums em `academic/domain/AcademicStates.java`.
- Mutação de agregado existente recebe `long expectedVersion` e `String reason`; operação crítica também recebe `boolean confirmed`.
- `AcademicAuthorizationPolicy.requireCoordinator(UUID actor)` valida papel atual e vínculo ativo. `requireRead(UUID actor, UUID memberId)` aceita titular ativo ou administração autorizada, retornando projeção adequada; gestor de projeto não ganha acesso adicional.
- `AcademicTransactions.write(Supplier<T> operation)` usa transação e o lock de `AdministrativeTransactions`, revalidando as permissões dentro da operação. Captura final usa corte consistente e protocolo de versões/locks definido e testado na tarefa 4, sem promover isolamento dentro de transação já iniciada.
- `AcademicRepository` acessa somente tabelas `academic_*`; leituras estrangeiras pertencem aos provedores. DTOs públicos não expõem entidades internas, hashes de credencial ou binários.
- Coleções HTTP usam `PageResult<T>`, filtros explícitos e ordenação estável. Rotas sob `/api/v1/academic/...` para operações administrativas e `/api/v1/me/academic/...` para titular; liberar somente métodos/caminhos implementados na allowlist HTTP.
- Campos institucionais não confirmados ficam ausentes/desabilitados, com erro de domínio claro ao executar operação dependente. Não preencher com valores presumidos.

## Tarefa 0 — Base, estratégia Flyway e baseline

**Arquivos:** criar `docs/implementation/bloco-06-execution.md`; atualizar somente o estado factual do contrato e o planejamento futuro B05 se a versão documental precisar ser realocada. Não editar migração B05 inexistente.

- [x] Registrar consulta somente leitura de `public.flyway_schema_history` do banco local `niteconnect`: V1–V3 aplicadas, checksums correspondentes; registrar instante e identidade do ambiente sem dados pessoais.
- [x] Registrar consulta somente leitura do Neon `nite-connect-staging`, branch `main`, banco `niteconnect_staging`: ausência de tabelas de aplicação e de histórico.
- [x] Registrar branches remotas inspecionadas e versões encontradas; V6 acadêmica somente se não houver conflito em nenhuma dessas evidências. Limitar a conclusão aos ambientes conhecidos, sem declarar auditoria de produção inexistente.
- [x] Executar baseline `apps/api/mvnw.cmd --batch-mode verify` com Java 21 e Docker; registrar contagem real. Não reutilizar os 185 históricos como resultado atual.
- [x] Abrir branch de implementação isolada, preservando o PR documental e o checkout B05. Commit do registro de base e plano após revisão do plano.

## Tarefa 1 — Períodos, políticas e autorização específica (B06.1)

**Arquivos:** criar `academic/domain/AcademicRecords.java`, `AcademicStates.java`, `AcademicRules.java`; `academic/application/AcademicAuthorizationPolicy.java`, `AcademicTransactions.java`, `AcademicTermService.java`; `academic/infrastructure/AcademicRepository.java`; `academic/api/AcademicContracts.java`, `AcademicTermController.java`; criar `apps/api/src/main/resources/db/migration/V6__academic_assessment.sql` somente após tarefa 0. Modificar `shared/security/SecurityConfiguration.java` somente para rotas implementadas. Testes: `AcademicPolicyTest`, `AcademicAuthorizationTest`, `Block06MigrationTest`.

**Interfaces:** `AcademicTermService.create(UUID actor, CreateTerm command): TermView`; `createPolicy(UUID actor, UUID termId, CreatePolicy command): PolicyView`; `confirmPolicy(UUID actor, UUID policyId, ConfirmPolicy command): PolicyView`. Comandos contêm datas/zone do período, identificadores de versão, pesos decimais por pilar, mínimo/máximo de nota, escala, `RoundingMode`, limites/janelas e referência institucional; política em preparação pode estar incompleta. Confirmação exige configuração completa do escopo e justificativa institucional.

- [x] Escrever e executar testes RED: gestor geral não confirma política nem registra nota; coordenador inativo/revogado é negado; pesos inválidos, datas invertidas e extremos são recusados; não há default 30/20/50; política utilizada não é editável.
- [x] Implementar migração aditiva das tabelas necessárias a todo o domínio, constraints de versão/unicidade/escala, índices de consultas e proteção de históricos. Incluir `ACADEMIC_*` em auditoria sem remover valores anteriores. Testar integridade referencial sem exclusão histórica por desativação de integrante.
- [x] Expor criação/consulta paginada de períodos e versões de política, confirmação exclusiva do coordenador; acrescentar `/academic/terms`, `/academic/terms/{id}/policies`, `/academic/policies/{id}/confirmation` à allowlist por método exato.
- [x] Rodar `mvnw.cmd --batch-mode -Dtest=AcademicPolicyTest,AcademicAuthorizationTest,Block06MigrationTest test`; exigir GREEN. Migração testa banco novo e upgrade V3/V5 com dados sintéticos preservados. Commit focado.

## Tarefa 2 — Adesão opcional e disciplinas (B06.2)

**Arquivos:** criar `academic/application/AcademicEnrollmentService.java`, `academic/api/AcademicEnrollmentController.java`; ampliar records, DTOs e repository próprios. Testes: `AcademicEnrollmentTest`.

**Interfaces:** `request(UUID actor, RequestEnrollment command): EnrollmentView`; `revise(UUID actor, UUID id, ReviseEnrollment command): EnrollmentView`; `decide(UUID actor, UUID id, DecideEnrollment command): EnrollmentView`. Comandos identificam período, disciplinas por código/curso declarado e justificativa; decisão de pertinência/elegibilidade exclusiva do coordenador, apoiada em critérios configurados. Curso não existe no cadastro atual: tratá-lo como declaração acadêmica conferida, sem inferir pelo e-mail ou ampliar `Member` silenciosamente.

- [x] RED: participação NITE não cria adesão; apenas titular ativo solicita; unicidade integrante/período e disciplinas normalizadas sem duplicidade; política não confirmada, janela ausente/fechada e limite excedido bloqueiam a solicitação dependente.
- [x] Implementar rascunho/solicitação/análise/correção/deferimento/indeferimento com versões e histórico; não oferecer desistência irrevogável ou solicitação administrativa em nome do aluno sem regra aprovada.
- [x] Rotas: `/me/academic/enrollments`, `/me/academic/enrollments/{id}`, `/academic/enrollments` e `/academic/enrollments/{id}/decisions`; leitura própria isolada e preparação administrativa sem poder pedagógico.
- [x] GREEN: `mvnw.cmd --batch-mode -Dtest=AcademicEnrollmentTest,AcademicAuthorizationTest test`; commit.

## Tarefa 3 — Plano Individual versionado (B06.3)

**Arquivos:** criar `academic/application/IndividualPlanService.java`, `academic/api/IndividualPlanController.java`; ampliar modelos e repository. Testes: `IndividualPlanTest`.

**Interfaces:** `save(UUID actor, UUID enrollmentId, SavePlan command): PlanView`; `submit(UUID actor, UUID planId, SubmitPlan command): PlanView`; `decide(UUID actor, UUID planId, DecidePlan command): PlanView`.

- [x] RED: somente titular edita/submete seu plano; coordenador analisa; gestor geral não aprova academicamente; nova revisão não sobrescreve texto/decisão anteriores.
- [x] Implementar objetivos e planejamento como conteúdo estruturado limitado, histórico e transições do contrato; não criar editor DOCX nem bloquear Diário de Bordo inexistente. Obrigatoriedade institucional permanece desabilitada enquanto não confirmada.
- [x] Rotas próprias `/me/academic/enrollments/{id}/plans`, `/me/academic/plans/{id}/submission`; decisão em `/academic/plans/{id}/decisions`; consultas de versões autorizadas.
- [x] GREEN: `mvnw.cmd --batch-mode -Dtest=IndividualPlanTest test`; commit.

## Tarefa 4 — Provedores de evidência, captura e divergência (B06.4)

**Arquivos:** criar `members/application/MemberAcademicReader.java`, `projects/application/ProjectAcademicReader.java`, `activities/application/ActivityEvidenceReader.java`, `attendance/application/AttendanceAcademicReader.java`, `academic/application/AcademicEvidenceService.java`, `academic/integration/DocumentEvidenceReader.java`, `academic/api/AcademicEvidenceController.java`; ampliar contratos próprios. Testes: `AcademicEvidenceTest`, `AcademicSnapshotConsistencyTest`.

**Interfaces:** leitores operacionais expõem `read(UUID actor, UUID memberId, UUID sourceId, Instant from, Instant to): Snapshot` em records próprios, sem retorno de repositório estrangeiro. `AcademicEvidenceService.capture(UUID actor, UUID enrollmentId, CaptureEvidence command): EvidenceView`; `compare(UUID actor, UUID evidenceId): EvidenceComparison`; contrato documental retorna explicitamente indisponibilidade em ausência de bean B05 real, nunca snapshot fictício.

- [x] RED: ator sem leitura de origem é recusado; atribuição/autoria deve corresponder ao aluno; atividade sem projeto é válida; vínculo histórico não é confundido com vínculo atual; upload PENDING/FAILED/retirado não sustenta captura válida; falta de B05 é indisponibilidade explícita.
- [x] Implementar envelope v1 do contrato: IDs, versões nativas ou token estável da projeção, autor, validação/revisor quando existente, intervalo, coleta e escopo autorizado; diferenciar versão de contrato e versão do dado. Origem sem decisão de validação retorna ausência, sem aprovador inventado.
- [x] Implementar captura transacional coerente com corte explícito; preservar snapshot mínimo, referências e divergências detectadas em releitura. Não copiar binários, segredos ou links temporários, nem fazer polling externo. Testar corrida entre correção de origem e captura, e mudanças após homologação.
- [x] Rotas `/academic/enrollments/{id}/evidence`, `/me/academic/enrollments/{id}/evidence` e comparação autorizada; consultas paginadas e filtros limitados.
- [x] GREEN: `mvnw.cmd --batch-mode -Dtest=AcademicEvidenceTest,AcademicSnapshotConsistencyTest test`; commit.

## Tarefa 5 — Pilares e cálculo preliminar (B06.5–B06.6)

**Arquivos:** criar `academic/application/PillarAssessmentService.java`, `AcademicCalculationService.java`, `academic/domain/AcademicCalculator.java`, `academic/api/AcademicAssessmentController.java`; ampliar records e repository. Testes: `AcademicCalculatorTest`, `AcademicAssessmentTest`.

**Interfaces:** `AcademicCalculator.calculate(PolicySnapshot policy, List<PillarInput> inputs): CalculationResult`; `PillarAssessmentService.assess(UUID actor, UUID enrollmentId, AssessPillar command): AssessmentView`; `AcademicCalculationService.calculate(UUID actor, UUID enrollmentId, Calculate command): CalculationView`.

- [x] RED: zero é nota atribuída; ausência/incompletude não viram zero; `BigDecimal`, soma exata de pesos, extremos, arredondamento da política, reprodução após mudança de semestre; gestor geral não pontua, mas pode calcular com notas existentes sem modificá-las.
- [x] Implementar notas humanas justificadas com referências por pilar e revisão imutável; captura de política/insumos/versões/algoritmo em cada cálculo. Referências operacionais não viram nota automaticamente. Apresentações exigem registro humano verificável no domínio, não inferência por presença.
- [x] Recusar cálculo completo/homologável com pilar sem evidência aceita, política não confirmada ou insumos divergentes; com B05 indisponível, não fabricar documentação nem redistribuir pesos.
- [x] Rotas `/academic/enrollments/{id}/assessments`, `/academic/enrollments/{id}/calculations`; consultas próprias não divulgam rascunhos/notas até rito de divulgação confirmado.
- [x] GREEN: `mvnw.cmd --batch-mode -Dtest=AcademicCalculatorTest,AcademicAssessmentTest,AcademicAuthorizationTest test`; commit.

## Tarefa 6 — Revisão, homologação e sucessão (B06.7)

**Arquivos:** criar `academic/application/AcademicReviewService.java`, `AcademicHomologationService.java`, `academic/api/AcademicReviewController.java`; ampliar repository/DTOs. Testes: `AcademicReviewTest`, `AcademicHomologationTest`, `AcademicConcurrencyTest`.

**Interfaces:** `request(UUID actor, UUID enrollmentId, RequestReview command): ReviewView`; `decide(UUID actor, UUID reviewId, DecideReview command): ReviewView`; `homologate(UUID actor, UUID calculationId, Homologate command): HomologationView`; `reopen(UUID actor, UUID homologationId, Reopen command): ReviewView`.

- [x] RED: chamada HTTP direta de gestor geral a homologação/reabertura/nota → 403; revogação/inativação concorrente impede escrita; revisão pendente/cálculo desatualizado/pilar faltante impedem homologação; motivo, confirmação e expectedVersion obrigatórios.
- [x] Implementar rito configurado de revisão, rejeição preservando resultado e procedência com nova revisão; não editar cálculo nem homologação anterior. Um pedido não invalida automaticamente decisão homologada.
- [x] Homologação serializada e auditada com nova verificação da política, insumos e permissões; reabertura e sucessora identificadas sem duas revisões vigentes. Homologação repetida/requisição obsoleta não duplica efeitos.
- [x] Rotas `/me/academic/enrollments/{id}/reviews`, `/academic/reviews/{id}/decisions`, `/academic/calculations/{id}/homologations`, `/academic/homologations/{id}/reopen`; testes positivos completos podem usar provedor documental sintético restrito ao classpath de teste, explicitamente sem homologação institucional ou runtime fictício.
- [x] GREEN: `mvnw.cmd --batch-mode -Dtest=AcademicReviewTest,AcademicHomologationTest,AcademicConcurrencyTest,AcademicAuthorizationTest test`; commit.

## Tarefa 7 — Encaminhamento e contratos documentais (B06.8)

**Arquivos:** criar `academic/application/AcademicForwardingService.java`, `academic/integration/AcademicDocumentRequest.java`, `AcademicNotificationEvent.java`, `academic/api/AcademicForwardingController.java`. Testes: `AcademicForwardingTest`.

**Interfaces:** `forward(UUID actor, UUID homologationId, ForwardAssessment command): ForwardingView`; contratos de solicitação documental e evento como records versionados, sem envio externo ou provedor de produto nesta etapa.

- [x] RED: sem procedimento institucional confirmado, não marcar encaminhamento oficial nem confirmação externa; gestor geral só prepara dados; referenciar homologação exata, sem reapontar envio antigo após revisão.
- [x] Implementar registro interno autorizado quando procedimento estiver configurado; confirmação externa permanece desabilitada sem procedimento/comprovação; não criar integração com portal, notificações reais, assinatura ou certificados.
- [x] Rotas somente das operações implementadas e efetivamente condicionadas; contrato documental sem editor/armazenamento paralelo. Snapshot exportável técnico não é documento oficial emitido.
- [x] GREEN: `mvnw.cmd --batch-mode -Dtest=AcademicForwardingTest test`; commit.

## Tarefa 8 — Contrato HTTP, regressão e encerramento técnico (B06.9)

**Arquivos:** criar `docs/implementation/bloco-06-api.yaml` (OpenAPI 3.1), `bloco-06-api.md`; atualizar `bloco-06-execution.md` e estado do contrato conforme evidências reais. Testes: `AcademicBoundaryTest`, complementar testes das tarefas anteriores sem duplicar seus cenários.

- [ ] Escrever e executar testes RED para endpoints finais, campos obrigatórios/limites, paginação, versão obsoleta, método não permitido, rota desconhecida, acesso sem JWT/Entra desabilitado e dados próprios versus alheios. Preservar todos os campos dos contratos anteriores.
- [ ] Documentar todas as rotas/DTOs implementados, erros 400/401/403/404/409, autenticação bearer e autorização contextual, estados e indisponibilidade B05. Validar correspondência entre OpenAPI, controllers e testes; não prometer endpoints ausentes.
- [ ] Executar testes específicos finais; depois `mvnw.cmd --batch-mode verify` uma vez para regressão integral, PostgreSQL real e Flyway. Reexecutar amplos somente se correção posterior invalidar os resultados. `git diff --check` e confirmar V1–V5 inalteradas.
- [ ] Revisão independente da branch inteira conforme `superpowers:requesting-code-review`, corrigindo achados relevantes com regressão antes de publicar a entrega. Não considerar resultado institucional homologado a partir de testes sintéticos.
- [ ] Abrir PR próprio de implementação, com base explicitada se o contrato ainda não estiver integrado; acompanhar CI aplicável. Sem merge/deploy automático. Registrar gates técnicos atingidos e pendências B05/institucionais mantidas fora de ativação.

## Limite de conclusão

O núcleo pode ser implementado/testado com integrações indisponíveis corretamente representadas. Sem política institucional confirmada e evidência documental real quando exigida, o ambiente de produto não terá resultado homologável; não declarar encerramento funcional integral de B06 ou G5. O relatório final distingue capacidade de domínio comprovada em testes, prontidão de integração e autorização institucional.
