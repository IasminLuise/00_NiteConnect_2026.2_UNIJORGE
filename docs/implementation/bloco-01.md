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
3. Primeiro Coordenador provisionado por comando local explícito, sem HTTP,
   somente em banco ainda sem identidades; processo encerra após a operação.

As três propostas foram explicitamente aprovadas nesta execução em 15/10/2026;
não são normas institucionais.

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
- [ ] B01.7 — Suíte integrada, revisão de segurança, documentação de operação
  e evidências. Homologação Microsoft real depende do ambiente institucional.

## Evidências

- Base: 803fdeb, branch de origem chore/fundacao-tecnica; árvore limpa.
- Fundação: `mvnw.cmd --batch-mode verify`, Java 21 explicitamente selecionado:
  3 testes, 0 falhas/erros, PostgreSQL 17 Testcontainers; BUILD SUCCESS.
- Ambiente: Maven herdava JAVA_HOME incompatível com release 21. Seleção
  corrigida somente no processo do comando; nenhuma configuração global alterada.

## Revisão final

Verificar associação cruzada de identidades, identidade sem ingresso, alteração
de permissão durante token válido, concorrência entre administradores, rollback
de auditoria e ausência de dados pessoais/segredos nos erros e respostas públicas.

## Registro de execução

- B01.2/B01.3: testes iniciais falharam por 403 em vez de 401 e zero das seis
  tabelas. Após implementação: 2/2 verdes contra PostgreSQL real.
- B01.4/B01.5/B01.6: cadastro primeiro retornou 404; após implementação,
  11/11 testes verdes (isolamento, escalada, duplicidade, versão e estados).
- Convites: criação primeiro retornou 404; após implementação, 16/16 testes
  integrados verdes, incluindo aceite idempotente, revogação e rollback.
- Validação criptográfica: 20/20 testes verdes, com RSA efêmero e validadores
  usados pelo decoder de produção; nenhum token institucional foi utilizado.
- Bootstrap: teste primeiro encontrou zero coordenadores. A implementação
  criou o primeiro coordenador; ajustes subsequentes corrigiram o teste para
  invocar a opção explícita de modo sem servidor e a exceção propagada no Boot 4.
- Decisão técnica: JdbcTemplate (já disponível) mantém SQL e contratos
  explícitos por domínio; evita entidades JPA artificiais e dependências novas.
- Decisão técnica: preservar um administrador ativo e serializar alterações
  administrativas com advisory lock transacional. Evita perda concorrente de
  autoridade; custo: essas escritas de baixo volume não executam em paralelo.
- Ajuste de contrato: justificativas de DELETE ficam em JSON, sem dados
  administrativos reservados em URLs e logs de acesso de proxies.
- Homologação Entra institucional real e CI remoto permanecem evidências separadas.
- Validação integrada após o código final: `mvnw.cmd --batch-mode verify`:
  BUILD SUCCESS, 56 testes, zero falhas/erros/ignorados; JAR empacotado.
  Classes: ApiFoundationTest (4), BootstrapProcessTest (1),
  EntraConfigurationTest (3), EntraTokenValidationTest (20),
  IdentityBoundaryTest (28). Docker/PostgreSQL 17 reais nos testes de persistência.
- `git diff --check` sem erros; V1 preservada. Alterações posteriores somente
  documentais não invalidam a execução Java e não exigem repeti-la.
- Ao incluir arquivos novos no índice, `git diff --cached --check` encontrou
  um espaço ao final do delimitador de text block em teste. Removido sem mudar
  o texto JSON nem o comportamento; suíte anterior permanece válida.
- A main remota contém a fundação via squash no PR #1 (7c00cad), com árvore
  idêntica à base 803fdeb. A entrega deve partir dessa main para evitar duplicar
  a fundação no diff do PR; reposicionar somente o commit novo preserva conteúdo.

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

A marca de B01.3 acima registra implementação e validação automatizada. A
homologação com token emitido no tenant institucional continua pendente em
B01.7, sem credenciais ou conta autorizada disponibilizadas nesta execução.

## Revisão independente e limites de conclusão

Revisão somente leitura por agente independente: código, testes, migração,
contratos e trechos pertinentes do PDF. Sem defeitos bloqueantes ou achados
P0/P1/P2. Relatórios Surefire conferidos, sem repetir a suíte.

Melhorias P3 registradas para revisão posterior (sem correção obrigatória):

1. Adicionar teste de bootstrap em modo web: guard existente recusa a operação,
   mas falta testar esse caminho e a ausência de gravações no banco.
2. Adicionar principal JWT simulado com Entra desabilitado ao teste da fundação:
   reforça a cobertura da condição `decoder != null`; o guard está implementado.
3. Ajustar nome ou completar `expiredInvitationRequiresExplicitRevocationBeforeReplacement`:
   o teste atual comprova rejeição de aceite expirado, sem exercitar substituição.

Limites mantidos após a revisão:

- E-mail externo, Graph, frontend e liderança de projetos permanecem fora do
  bloco autorizado. Reintroduzi-los ampliaria integrações e escopo sem aprovação.
- Entra institucional real requer tenant, aplicação e conta autorizada. Não
  declarar homologação com base em chaves efêmeras ou principal simulado.
- CI remoto e implantação não são demonstrados pelo build local; serão
  registrados separadamente. Nenhuma operação de implantação foi autorizada.
- Regras institucionais de desligamento automático, retenção e avaliação não
  foram presumidas; automatizá-las antecipadamente poderia alterar direitos.

O código e a entrega para revisão estão prontos; o aceite institucional final
do bloco continua condicionado à homologação Microsoft real.
