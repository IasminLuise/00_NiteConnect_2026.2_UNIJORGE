# Staging do Bloco 02 — Cloudflare + Neon

## Estado

O repositório está preparado para executar a API Java em Cloudflare Containers e
usar PostgreSQL gerenciado no Neon. O projeto Neon `nite-connect-staging` foi
provisionado em `aws-sa-east-1`, com branch `main` e banco
`niteconnect_staging`; o banco foi verificado vazio. Nenhum Worker foi
implantado e nenhum secret foi configurado no Cloudflare. A conta Cloudflare
está no plano Gratuito; Containers exige o plano Workers Pago, cuja contratação
e cobrança por uso não foram autorizadas. O aceite institucional e a migração
de produção continuam pendentes.

Este repositório contém o backend; `apps/web` ainda não é um frontend integrado.
A homologação funcional usa cliente HTTP/Postman com access token institucional
e contas de teste autorizadas. Login Microsoft não concede autorização por si só.

## Topologia

- Cloudflare Worker recebe HTTPS e encaminha as requisições para um Durable
  Object/Container que executa a imagem Java 21.
- Cloudflare Container é efêmero. Dados e schema ficam no PostgreSQL do Neon.
- API usa `DB_URL` com endpoint pooled Neon (`-pooler`) e TLS.
- Flyway usa `FLYWAY_URL` com endpoint direto, sem `-pooler`, e TLS. Separar a
  conexão de migração evita depender de estado de sessão por meio do PgBouncer.
- A instância é `basic` (1 GiB) e `max_instances=1` como ponto inicial de
  staging. Medir memória, tempo de inicialização, cold starts e custo antes de
  homologar e aumentar capacidade.
- O acesso à internet do Container permite a conexão PostgreSQL TCP/TLS ao Neon
  e consulta às chaves públicas do Microsoft Entra. Os endpoints continuam
  autenticados pela API.
- Cloudflare Containers está disponível no Workers Paid. Esta implementação
  usa a política padrão; a política alternativa `durable_object` permanece em
  beta e não é usada aqui. Validar sizing, cold starts, custo e requisitos de
  disponibilidade no plano contratado.

## Pré-requisitos institucionais

1. Contratação autorizada do plano Cloudflare Workers Pago, necessário para
   Containers, e definição do limite de gasto mensal para o uso excedente.
2. Acesso à conta Cloudflare autorizada com Workers/Containers habilitados.
3. Projeto/branch e banco de staging vazios no Neon. O ambiente atual é
   `nite-connect-staging` (`aws-sa-east-1`), branch `main`, banco
   `niteconnect_staging`; revalidar antes do bootstrap.
4. Tenant Microsoft Entra, audience/client ID e scope delegado aprovados para a
   API; contas de teste, incluindo um `GESTOR_GERAL`, autorizadas.
5. URL `workers.dev` temporária ou domínio HTTPS controlado pela instituição,
   operador de implantação, testadores e pessoa responsável pelo aceite.

Nenhuma conta, banco, DNS, domínio, segredo ou identidade é provisionada por CI.
Não usar dados reais de produção no staging.

## Configuração segura

Na pasta `infra/cloudflare`, instale dependências com `npm ci`. Configure cada
valor com o prompt interativo do Wrangler; não coloque segredos em argumentos,
arquivos versionados ou logs:

~~~powershell
npx wrangler secret put DB_URL
npx wrangler secret put DB_USERNAME
npx wrangler secret put DB_PASSWORD
npx wrangler secret put FLYWAY_URL
npx wrangler secret put ENTRA_TENANT_ID
npx wrangler secret put ENTRA_AUDIENCE
npx wrangler secret put ENTRA_REQUIRED_SCOPE
~~~

Os dois URLs são JDBC. Converter localmente os dados apresentados pelo Neon,
sem copiar a senha ao chat:

~~~text
Aplicação: jdbc:postgresql://<host-com--pooler>/<database>?sslmode=require
Flyway:    jdbc:postgresql://<host-direto>/<database>?sslmode=require
~~~

Use o mesmo usuário/senha exclusivos de staging, salvo se a política do projeto
definir um papel de migração separado. O valor `DB_URL` usa o host `-pooler`; o
`FLYWAY_URL` usa o host direto. O Wrangler guarda os valores como secrets do
Worker e o código os transmite somente ao processo Java do Container, que
precisa deles para abrir as conexões JDBC.

`ENTRA_ENABLED=true` é fixado na configuração. Não adicione dados de bootstrap
como secrets permanentes do serviço.

## Bootstrap inicial

O banco precisa estar vazio e pertencer exclusivamente ao staging. O CI da
`main` disponibiliza por 14 dias o JAR verificado como artefato
`nite-connect-staging-bootstrap-<SHA>`. Para criar o primeiro `GESTOR_GERAL`,
o operador autorizado baixa o JAR da execução correspondente ao commit e o
executa localmente uma única vez com as variáveis temporárias
`NITE_BOOTSTRAP_OBJECT_ID`, `NITE_BOOTSTRAP_NAME`,
`NITE_BOOTSTRAP_INSTITUTIONAL_EMAIL` e `NITE_BOOTSTRAP_REASON`, além de
`DB_URL`, `DB_USERNAME` e `DB_PASSWORD` apontando para o endpoint direto do
Neon. Execute `java -jar <JAR> --spring.main.web-application-type=none
--nite.bootstrap.enabled=true`; o runtime normal mantém o bootstrap desativado.
Forneça valores por mecanismo seguro do executor, nunca na linha de comando ou
em log persistente, e remova-os logo após a execução. Não repita o bootstrap em
atualizações.

A execução deve terminar com código zero e registrar Flyway V1–V3 aplicadas.
Depois, configure os secrets do Worker com `DB_URL` pooled e `FLYWAY_URL`
direto, publique o Container e valide `/actuator/health` e
`/api/v1/projects` sem token (esperado: HTTP 401). Se o bootstrap falhar,
preserve o banco e investigue; não limpe nem recrie o branch sem autorização.

## Deploy manual

O CI valida a API, constrói a imagem no PR e valida tipos/bundle Wrangler. Não
executa `wrangler deploy`, não usa credenciais cloud, não cria banco nem publica
artefato de implantação. Após merge e configuração de conta/secrets pelo
operador autorizado:

~~~powershell
Set-Location infra/cloudflare
npm ci
npx wrangler deploy
~~~

O primeiro deploy pode levar alguns minutos para distribuir a imagem. Acompanhe
o estado do Worker e Container no painel Cloudflare e confira uma requisição que
chegue à API. Configure domínio próprio apenas após validar a URL temporária e
aprovar a rota institucional. A URL, SHA, release e quem executou devem constar
no registro de homologação.

## Atualizações, migrações e recuperação

Cada release inicia com secrets existentes. Flyway valida/aplica migrações na
inicialização do Container usando conexão Neon direta; mantenha apenas uma
instância durante atualização de schema. Nunca edite migrações já aplicadas.
Crie e valide uma branch/snapshot Neon recuperável antes de mudança de schema.
Retorne à imagem anterior somente quando compatível com o schema atual; não faça
downgrade automático. O disco do Container é efêmero e não é cópia de segurança.

## Roteiro de aceite institucional

Registrar data, URL, SHA completo, execução CI, versão Flyway, executor,
testadores e responsável institucional. Usar identidades autorizadas e dados
sintéticos.

| Grupo | Validação no staging | Evidência |
|---|---|---|
| Autenticação | `GET /api/v1/me`, ausência de token e integrante sem vínculo | Respostas e negações esperadas |
| T01–T05, T13 | Criar projetos, ingressar integrantes, gestor próprio/alheio | Persistência, vínculos e isolamento |
| T06–T10, T21 | Transferência/vacância de liderança, saída/retorno, desligamento e revogação | Uma liderança vigente e períodos preservados |
| T11–T12 | Pausar, retomar, concluir e reabrir com justificativas | Histórico completo e estado final |
| T14–T16 | Proposta, aprovação, conversão repetida e concorrente | Origem preservada e projeto único |
| T17, T22–T23 | Integrante inexistente/inativo, versão antiga e dados inválidos | Rejeição sem alteração parcial |
| T18–T20, T24 | Falha de auditoria, migrações e suíte anterior | CI com rollback, V1–V3 e 111 testes preservados |

Falhas de integração, concorrência e auditoria são verificadas pela suíte local
com Testcontainers. Não injetar falhas artificiais no banco de homologação.
Marcar cada caso aprovado/reprovado/não executado e registrar evidência sem
segredos. Somente a pessoa responsável designada pela instituição concede o
aceite formal. CI verde e deploy técnico não concluem a homologação.
