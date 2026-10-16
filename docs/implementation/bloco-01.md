# Bloco 01 — identidade, integrantes e permissões

Fonte: conversa “Planejamento do Bloco 1”, seções 1–15, e especificação
funcional consolidada v1.0, especialmente RF-013, RF-017, RF-019–021,
RF-052, CA-01–03 e CA-16. Implementação única, backend primeiro.

## Contratos e limites

- API stateless com OAuth2 Resource Server; tokens de acesso Microsoft Entra
  v2, assinatura RS256, emissor específico do tenant, audience da API,
  tenant/object ID válidos e escopo delegado configurado. Sem senhas próprias.
- Papéis globais: COORDENADOR e GESTOR_GERAL. Todo integrante ativo possui
  acesso aos próprios dados. GESTOR_PROJETO não é papel global; vínculos de
  projeto serão contratos do Bloco 02. Não conceder acesso pessoal por liderança.
- Cadastro mínimo: UUID, nome, e-mail institucional @unijorge.com.br,
  estado, versão e datas. Sem CPF, notas ou dados acadêmicos neste bloco.
- Estados: PENDENTE_INGRESSO, ATIVO, AFASTADO e DESLIGADO. Sem exclusão de
  integrante/identidade/histórico. Ingresso depende de convite pré-autorizado.
- Autorização considera banco atual em cada operação, ignorando roles do token.
  Motivos administrativos e histórico ficam restritos à administração.
- Paginação limitada, erros sem dados internos, DTOs explícitos e validação.
- Novas migrações após V1; restrições relacionais e operações transacionais.
- Configuração Microsoft ausente mantém deny-all; configuração habilitada
  incompleta impede inicialização. Sem mudanças em .env ou credenciais.
- Concorrência: versão esperada nas alterações; transações, bloqueio de
  operações administrativas e nova checagem de autoridade dentro da transação.
  Impedir remoção do último administrador ativo.

## Decisões abertas na fonte e aprovadas pelo responsável

1. AFASTADO sem acesso operacional.
2. Convite com e-mail institucional e identidade Microsoft previamente conferida
   (tenant ID + object ID), aceito somente pela identidade indicada; sem envio
   externo neste bloco. Expiração informada explicitamente pela administração.
3. Primeiro administrador global (COORDENADOR ou GESTOR_GERAL) provisionado
   com papel explícito por comando local, sem HTTP, somente em banco ainda sem
   identidades; processo encerra após a operação.

As três propostas foram explicitamente aprovadas nesta execução em 15/10/2026.
A escolha do papel no bootstrap foi ampliada por decisão posterior do responsável,
que selecionou GESTOR_GERAL para a primeira conta. Não são normas institucionais.

## Sete etapas e verificação

- [x] B01.1 — Formalizar DTOs, acesso, ingresso e bootstrap; registrar decisões.
- [x] B01.2 — Migração com members, identity_accounts, invitations,
  role_assignments, member_status_history e audit_events. Verificar chaves,
  unicidade, Flyway e rollback contra PostgreSQL real.
- [x] B01.3 — Validação Entra e GET /api/v1/me. Testar assinatura, issuer,
  audience, tenant, expiração, escopo, claims ausentes e app-only/ID token.
- [x] B01.4 — Cadastro, consulta própria/administrativa, PATCH com versão,
  listagem paginada, convites e aceite. Testar duplicidade e isolamento.
- [x] B01.5 — Concessão/revogação global e reavaliação imediata do acesso.
  Testar escalada, liderança sem poder global e último administrador.
- [x] B01.6 — Transições, reativação e auditoria atômica. Testar preservação,
  justificativas, versões conflitantes e falha composta sem persistência parcial.
- [x] B01.7 — Suíte integrada, revisão de segurança, documentação de operação
  e teste autenticado com conta institucional real no ambiente local. O alcance
  da homologação técnica está registrado abaixo.

## Evidências

- Base: 803fdeb, branch de origem chore/fundacao-tecnica; árvore limpa.
- Fundação: `mvnw.cmd --batch-mode verify`, Java 21 explicitamente selecionado:
  3 testes, 0 falhas/erros, PostgreSQL 17 Testcontainers; BUILD SUCCESS.
- Ambiente: Maven herdava JAVA_HOME incompatível com release 21. Seleção
  corrigida somente no processo do comando; nenhuma configuração global alterada.
- Entrega: PR #2 integrado à `main` no commit `e13ecb5`; as duas execuções
  `verify-api` do commit final passaram no GitHub Actions.

## Critérios da revisão final

A revisão contemplou associação cruzada de identidades, identidade sem ingresso,
alteração de permissão durante token válido, concorrência entre administradores,
rollback de auditoria e ausência de dados pessoais/segredos nos erros e respostas
públicas.

## Registro de execução

- B01.2/B01.3: testes iniciais falharam por 403 em vez de 401 e zero das seis
  tabelas. Após implementação: 2/2 verdes contra PostgreSQL real.
- B01.4/B01.5/B01.6: cadastro primeiro retornou 404; após implementação,
  11/11 testes verdes (isolamento, escalada, duplicidade, versão e estados).
- Convites: criação primeiro retornou 404; após implementação, 16/16 testes
  integrados verdes, incluindo aceite idempotente, revogação e rollback.
- Validação criptográfica: 20/20 testes verdes, com RSA efêmero e validadores
  usados pelo decoder de produção; nessa suíte, nenhum token institucional foi
  utilizado.
- Bootstrap: teste inicial encontrou zero administradores. A implementação
  passou a criar o primeiro administrador global com papel explícito; o teste
  final cobre GESTOR_GERAL, modo sem servidor, rejeição do modo web e recusa
  de novo provisionamento.
- Decisão técnica: JdbcTemplate (já disponível) mantém SQL e contratos
  explícitos por domínio; evita entidades JPA artificiais e dependências novas.
- Decisão técnica: preservar um administrador ativo e serializar alterações
  administrativas com advisory lock transacional. Evita perda concorrente de
  autoridade; custo: essas escritas de baixo volume não executam em paralelo.
- Ajuste de contrato: justificativas de DELETE ficam em JSON, sem dados
  administrativos reservados em URLs e logs de acesso de proxies.
- Suíte automatizada, CI remoto e teste institucional manual são evidências
  distintas; seus resultados e limites constam abaixo.
- Validação integrada após o código final: `mvnw.cmd --batch-mode verify`:
  BUILD SUCCESS, 58 testes, zero falhas/erros/ignorados; JAR empacotado.
  Classes: ApiFoundationTest (5), BootstrapProcessTest (2),
  EntraConfigurationTest (3), EntraTokenValidationTest (20),
  IdentityBoundaryTest (28). Docker/PostgreSQL 17 reais nos testes de persistência.
- `git diff --check` sem erros; V1 preservada. Alterações posteriores somente
  documentais não invalidam a execução Java e não exigem repeti-la.
- Ao incluir arquivos novos no índice, `git diff --cached --check` encontrou
  um espaço ao final do delimitador de text block em teste. Removido sem mudar
  o texto JSON nem o comportamento; suíte anterior permanece válida.
- A `main` remota contém a fundação via PR #1 e o Bloco 01 via PR #2. A árvore
  do merge `e13ecb5` coincide com a branch validada no PR #2.

## Homologação técnica Microsoft Entra

Na sessão de validação de 15/10/2026, uma conta institucional real obteve um
access token delegado pelo cliente OAuth de teste configurado no tenant. A API
local recebeu esse token pelo Postman. Após o provisionamento administrativo
explícito da identidade no PostgreSQL local, com estado ATIVO e papel
GESTOR_GERAL, `GET /api/v1/me` retornou HTTP 200. O vínculo da identidade, o
estado, o papel e os eventos de auditoria foram conferidos na base local.
Nenhum token, identificador pessoal ou credencial é reproduzido neste registro.

O teste comprova, nesse ambiente e para essa conta, a emissão do token
institucional, sua aceitação pela API e a autorização baseada no vínculo local.
Os testes automatizados cobrem as falhas e regras de acesso descritas abaixo.
Essa execução manual não abrange login pelo futuro frontend, convite de uma
segunda conta real, todas as rotas administrativas, implantação em produção ou
aceite institucional formal desses ambientes.

## Rastreabilidade dos testes previstos

| Casos do planejamento | Evidência automatizada |
|---|---|
| T01–T03 | IdentityBoundaryTest + EntraTokenValidationTest (ausência, assinatura, expiração, issuer/audience/tenant e claims) |
| T04–T07 | unknownInstitutionalIdentityCannotReadInternalData, activeMemberCanOnlyReadOwnPersonalData, tokenRolesCannotElevatePrivileges |
| T08–T09 | administrativeCreationIsPersistentAndAudited, duplicateInstitutionalEmailIsAConflict, identityAlreadyLinkedCannotBeInvitedForAnotherMember |
| T10 | inactiveMembershipCannotAccessOperationalEndpoints |
| T11–T13 | removalImmediatelyDeniesExistingIdentityAndPreservesHistory, missingJustificationAndInvalidRoleAreBadRequests |
| T14 | permissionRevocationTakesEffectOnNextRequest |
| T15 | simultaneousUpdatesHaveOneWinnerAndOneConflict, concurrentAdministrativeDeactivationPreservesOneAuthorizedAdministrator |
| T16 | identityPersistenceIsInstalledByFlyway + inicialização de todos os containers limpos |
| T17 | invalidOrNonInstitutionalInputIsRejected, missingJustificationAndInvalidRoleAreBadRequests |
| T18 | auditFailureRollsBackTheEntireAdministrativeOperation |

A marca de B01.3 registra implementação e validação automatizada. O teste
posterior com token institucional real integra a evidência de B01.7 descrita
acima; não substitui a cobertura negativa e de concorrência da suíte.

## Revisão independente e limites de conclusão

Revisão somente leitura por agente independente do código inicial do PR:
testes, migração, contratos e trechos pertinentes do PDF. Sem defeitos
bloqueantes ou achados P0/P1/P2. Os ajustes finais de testes e bootstrap foram
conferidos no diff e validados pela suíte completa e pelo CI do PR #2.

Melhorias P3 atendidas após a revisão:

1. `webBootstrapIsRejectedWithoutProvisioningAnyRecords`: inicialização servlet
   rejeitada pelo guard, sem registros nas seis tabelas, em PostgreSQL isolado.
2. `disabledEntraDeniesEvenAnAuthenticatedJwtPrincipal`: Entra explicitamente
   desabilitado; principal JWT simulado recebe 403 antes de alcançar um handler.
3. `expiredInvitationRequiresExplicitRevocationBeforeReplacement`: cobre aceite
   expirado rejeitado, substituição bloqueada antes da revogação, revogação
   explícita, novo convite aceito, identidade única e auditoria preservada.

Validação direcionada após esses três ajustes: Java 21, comando
`mvnw.cmd --batch-mode -Dtest=BootstrapProcessTest,ApiFoundationTest,IdentityBoundaryTest test`:
BUILD SUCCESS, 35 testes, zero falhas/erros/ignorados, PostgreSQL 17 via
Testcontainers. A mudança posterior para escolher o papel global no bootstrap
alterou código de produção; por isso, o estado final foi validado novamente com
`mvnw.cmd --batch-mode verify` (58 testes e empacotamento) e no CI remoto.

Limites mantidos após a revisão:

- E-mail externo, Graph, frontend e liderança de projetos permanecem fora do
  bloco autorizado. Reintroduzi-los ampliaria integrações e escopo sem aprovação.
- O teste real do Entra comprova o fluxo descrito para uma conta no ambiente
  local; chaves efêmeras e principal simulado sustentam somente a suíte.
- O CI remoto passou no PR #2. Não há evidência de implantação em produção ou
  de aceite institucional formal para esse ambiente.
- Regras institucionais de desligamento automático, retenção e avaliação não
  foram presumidas; automatizá-las antecipadamente poderia alterar direitos.

O Bloco 01 está tecnicamente concluído no backend e integrado à `main`. O teste
Microsoft real foi concluído no ambiente local com o alcance descrito acima;
aceite institucional formal e operação em produção exigem evidências próprias.
