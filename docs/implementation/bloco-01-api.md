# Bloco 01 — contrato HTTP e operação

Todas as rotas `/api/v1` exigem `Authorization: Bearer <access_token>`
Microsoft Entra v2 para a API. O endpoint `/actuator/health` continua público.
Não há login local, senha própria, sessão HTTP, registro público ou frontend
neste bloco. HTTPS deve ser aplicado no ambiente de implantação.

## Identidade e autorização

Configure no processo, sem commitar credenciais:

| Variável | Valor |
|---|---|
| `ENTRA_ENABLED` | `true` para habilitar; padrão `false` mantém deny-all |
| `ENTRA_TENANT_ID` | UUID do tenant institucional autorizado |
| `ENTRA_AUDIENCE` | Application (client) ID da **API** no token de acesso v2 |
| `ENTRA_REQUIRED_SCOPE` | Nome do escopo delegado exposto pela API e consentido no tenant |

Não é necessário client secret para validar assinaturas no Resource Server.
O registro do cliente que solicitará tokens é uma configuração institucional
externa; ele deve solicitar o escopo desta API. Tokens de Microsoft Graph,
ID tokens e tokens app-only não autorizam acesso. Não usar `common` como issuer.

O decoder RS256 consulta chaves Microsoft pelo endereço HTTPS fixo derivado
do tenant; valida issuer, audience, timestamps, `tid`, `oid`, `ver=2.0` e
`scp`. Há a tolerância técnica padrão de relógio do Spring Security (60s).
Nenhuma permissão global é extraída de `roles`, e-mail ou grupos do token.
Autorização usa `(tid, oid)`, o estado e os papéis atuais no PostgreSQL em
cada requisição. E-mail institucional é dado cadastral, não chave de identidade.

As decisões abaixo foram aprovadas pelo responsável nesta implementação:

- PENDENTE_INGRESSO, AFASTADO e DESLIGADO não possuem acesso operacional.
- Convite possui e-mail do cadastro e object ID previamente conferido pela
  administração no tenant autorizado. Aceite exige exatamente essa identidade.
- Primeiro administrador global (COORDENADOR ou GESTOR_GERAL) é provisionado
  por comando administrativo local explícito, com o papel escolhido informado.

COORDENADOR e GESTOR_GERAL têm administração global neste bloco. INTEGRANTE é
o acesso básico de qualquer integrante ATIVO. Liderança de projeto fica no
Bloco 02, sem papel global GESTOR_PROJETO e sem acesso a dados de colegas.

## Representações

`Member`: `id` (UUID), `name`, `institutionalEmail`, `status`, `version` (inteiro),
`createdAt` e `updatedAt` (ISO-8601 UTC). Sem CPF, tokens ou motivos administrativos.
Nomes têm até 160 caracteres; e-mails até 254 e domínio exato `@unijorge.com.br`,
normalizados em minúsculas. Nome é aparado. Motivos são obrigatórios, até 1000
caracteres, e ficam em corpo JSON (inclusive DELETE), fora da URL.

Listas retornam `{ "items": [...], "page": 0, "size": 20, "total": 0 }`.
`page` inicia em zero; `size` varia de 1 a 100, padrão 20. Ordenação estável
por nome/id para integrantes; data/id para eventos e convites.

## Endpoints

| Método e caminho | Acesso | Entrada / saída |
|---|---|---|
| GET `/api/v1/me` | Integrante ativo | `{member: Member, roles: string[]}` |
| GET `/api/v1/members` | Administração | Query `page`, `size`, `status` opcional; lista de Member |
| GET `/api/v1/members/{id}` | Próprio ativo ou administração | Member; colega sem permissão retorna 404 |
| POST `/api/v1/members` | Administração | `{name, institutionalEmail}`; 201 Member PENDENTE_INGRESSO |
| PATCH `/api/v1/members/{id}` | Administração | `{name, institutionalEmail, expectedVersion, reason}`; 200 Member |
| PATCH `/api/v1/members/{id}/status` | Administração | `{status, expectedVersion, reason}`; 200 Member |
| GET `/api/v1/members/{id}/history` | Administração | Query `page`, `size`; lista de transições |
| POST `/api/v1/members/{id}/roles` | Administração | `{role, expectedVersion, reason}`; 200 lista de papéis efetivos |
| DELETE `/api/v1/members/{id}/roles/{role}` | Administração | Corpo `{expectedVersion, reason}`; 204 |
| POST `/api/v1/invitations` | Administração | `{memberId, objectId, expiresAt, reason}`; 201 Invitation |
| GET `/api/v1/invitations` | Administração | Query `page`, `size`; lista de Invitation |
| POST `/api/v1/invitations/{id}/accept` | Identidade destinatária do convite | Sem corpo; 200 Member ativado |
| DELETE `/api/v1/invitations/{id}` | Administração | Corpo `{reason}`; 204, revogação lógica |
| GET `/api/v1/audit-events` | Administração | Query `page`, `size`; lista de eventos administrativos |

`Invitation`: `id`, `memberId`, `tenantId`, `objectId`, `createdBy`, `createdAt`,
`expiresAt`, `acceptedAt`, `revokedAt`. Apenas administração recebe a lista;
aceite não divulga dados de um convite para outra identidade (404).

Transição: `id`, `actorId`, `previousStatus`, `newStatus`, `reason`, `occurredAt`.
Auditoria: `id`, `actorId`, `memberId`, `action`, `beforeValue`, `afterValue`,
`reason`, `occurredAt`. A consulta de auditoria inclui dados reservados e é
exclusiva da administração. Banco rejeita UPDATE/DELETE desses históricos.

Cada alteração de cadastro, estado ou papel incrementa a versão do integrante.
Consulte o integrante antes da próxima alteração; uma versão desatualizada gera
409. Conceder papel exige integrante ativo; revogação também funciona para
afastados/desligados. Reativação preserva os papéis ainda existentes e não
restaura papéis revogados. Não se pode retirar o último administrador ativo.

O ingresso PENDENTE_INGRESSO → ATIVO ocorre exclusivamente pelo aceite. Não há
transição administrativa para PENDENTE_INGRESSO. AFASTADO e DESLIGADO podem
voltar a ATIVO por decisão administrativa com justificativa; outras mudanças
entre esses estados preservam o histórico. Nenhum integrante é excluído.

Convites só são criados para pendentes sem identidade associada. `expiresAt`
é escolhido pela administração e deve ser futuro; não há prazo institucional
presumido. Um convite pendente por integrante e por identidade é permitido.
Para substituir convite expirado, revogue-o explicitamente antes de criar outro.
Aceite repetido é idempotente, mas não reativa vínculo suspenso. Não há serviço
de envio de e-mail ou integração com Graph neste bloco; entrega do identificador
do convite fica a cargo da administração por canal autorizado.

## Erros e concorrência

- 400: DTO, UUID, enum ou paginação inválidos.
- 401: token ausente/inválido/expirado quando Entra está habilitado.
- 403: identidade sem ingresso, vínculo inativo ou autoridade insuficiente.
- 404: recurso inexistente ou pessoal sem autorização; convite de outra identidade.
- 409: duplicidade, versão desatualizada, transição incompatível ou último administrador.

Erros de aplicação usam ProblemDetail sem SQL, tokens ou dados de entrada.
Rotas não implementadas permanecem negadas. Com Entra desabilitado, rotas
internas continuam 403, inclusive se houver um principal simulado por teste.

Alterações administrativas e aceite são transacionais. Um advisory lock do
PostgreSQL serializa essas operações de baixo volume; após adquirir o lock,
a autoridade é consultada novamente. Versões evitam sobrescrita silenciosa.
Falha de vínculo, integridade ou auditoria desfaz toda a operação. Leituras
não usam esse lock; não há cache de autorização ou infraestrutura adicional.

## Primeiro administrador global

Operação local de implantação, somente com identificadores reais previamente
conferidos pela instituição. Nunca colocar dados reais em arquivos versionados.
Não executar contra produção sem autorização específica.

Após configurar banco e Entra no processo e gerar o JAR (`mvnw.cmd verify`),
informe ao processo local as propriedades abaixo (valores demonstrativos devem
ser substituídos pelo operador; não são contas pré-criadas):

```powershell
# Na pasta apps/api; dados fornecidos pelo operador, sem escrevê-los no repositório.
java -jar target/nite-connect-api-0.0.1-SNAPSHOT.jar `
  --spring.main.web-application-type=none `
  --nite.bootstrap.enabled=true `
  --nite.bootstrap.role=GESTOR_GERAL `
  "--nite.bootstrap.object-id=$bootstrapObjectId" `
  "--nite.bootstrap.name=$bootstrapName" `
  "--nite.bootstrap.institutional-email=$bootstrapEmail" `
  "--nite.bootstrap.reason=$bootstrapReason"
```

As variáveis acima precisam ser fornecidas pelo operador; não executar com valores
vazios. Não registrar histórico de shell/transcrições com dados pessoais. Como
alternativa, use variáveis de ambiente Spring `NITE_BOOTSTRAP_OBJECT_ID`,
`NITE_BOOTSTRAP_ROLE`, `NITE_BOOTSTRAP_NAME`,
`NITE_BOOTSTRAP_INSTITUTIONAL_EMAIL` e `NITE_BOOTSTRAP_REASON`, mantendo os flags
não sensíveis no comando.

O processo exige modo `none`, valida propriedades, adquire o lock, recusa banco
que já contenha identidade ou papel, valida o papel global escolhido, cria
integrante ativo, identidade, o papel informado, histórico e auditoria na mesma
transação, e encerra. Não inicia
servidor nem endpoint de bootstrap. Remova as opções/variáveis de bootstrap
antes da execução normal. Outros administradores são convidados e recebem
papel pela API após o ingresso. Não há mecanismo público de recuperação
administrativa; recuperar um ambiente corrompido exige procedimento operacional.

## Persistência e validação

V1 permanece intacta. V2 instala seis tabelas, FKs sem exclusão em cascata,
unicidade de e-mail e identidade, índices, estados/papéis limitados e históricos
imutáveis. O acesso SQL usa JdbcTemplate já fornecido pela stack existente;
as transações são demarcadas nos serviços. Não há dependência nova de ORM,
mensageria ou infraestrutura. O único starter novo é OAuth2 Resource Server.

```powershell
Set-Location apps/api
.\mvnw.cmd --batch-mode verify
```

Docker deve estar disponível para Testcontainers/PostgreSQL 17. Java 21 é
obrigatório; confira o Java efetivamente usado com `.\mvnw.cmd -version`.
Testes criptográficos usam chaves efêmeras de teste; testes de autorização
simulam somente o principal e usam aplicação, migrações e PostgreSQL reais.
Bootstrap é testado inicializando a aplicação em modo sem servidor.

Homologação técnica no ambiente local realizada em 15/10/2026: uma conta
institucional real obteve access token delegado com o cliente OAuth de teste;
o Postman enviou esse token à API. Após provisionamento administrativo explícito
da identidade como integrante ATIVO e GESTOR_GERAL no PostgreSQL local,
`GET /api/v1/me` retornou HTTP 200. O vínculo, o papel e a auditoria foram
conferidos sem registrar token, credenciais ou identificadores pessoais neste
documento. Isso comprova a emissão e aceitação do token institucional e a
autorização desse integrante no ambiente testado.

A execução não cobre login pelo futuro frontend, convite autenticado de outra
conta real, todas as rotas administrativas, implantação em produção ou aceite
institucional formal para esse ambiente. Os testes automatizados cobrem regras
e falhas separadamente; o PR #2 passou no GitHub Actions e foi integrado à
`main`. Consulte o resultado e os limites em `docs/implementation/bloco-01.md`.
