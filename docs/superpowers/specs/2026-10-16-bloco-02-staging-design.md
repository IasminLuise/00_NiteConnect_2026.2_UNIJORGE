# NITE Connect — staging Cloudflare + Neon

## Objetivo

Preparar uma implantação repetível do backend Java do Bloco 02 em Cloudflare Containers, usando um PostgreSQL exclusivo de staging no Neon. O ambiente serve à homologação institucional com Microsoft Entra e dados sintéticos. Não executa promoção automática para produção.

## Decisões aprovadas

- Cloudflare Workers + Containers executam a API Spring Boot existente; o Worker encaminha as requisições HTTP para um Durable Object que gerencia o Container.
- Neon fornece PostgreSQL gerenciado. A API usa endpoint pooled com TLS; Flyway usa endpoint direto com TLS, pois migrações exigem conexão de sessão.
- O Dockerfile é mantido porque Cloudflare Containers recebe uma imagem Linux. Docker Compose, banco local de runtime, VM, proxy institucional e bundle manual são removidos deste caminho.
- A implantação permanece manual. CI testa e empacota a imagem e valida o Worker, sem credenciais Cloudflare/Neon nem deploy automático.
- O schema é criado/aplicado pelas migrações Flyway existentes. Nenhum dado de produção será copiado.
- Segredos de Neon e parâmetros autorizados do Entra serão armazenados como secrets do Worker e passados ao processo Java apenas em runtime. Não entram em arquivos versionados, artefatos ou logs.
- Nenhum recurso será criado em Cloudflare ou Neon pelo código da CI. Provisionamento real precisa de conta autorizada, branch/banco de staging e parâmetros institucionais.

## Componentes

1. `apps/api/Dockerfile` e `.dockerignore`: imagem mínima da aplicação Java 21, construída a partir do JAR verificado, sem Maven e sem root.
2. `infra/cloudflare/`: Worker TypeScript, binding Durable Object/Container, configuração Wrangler e dependências fixadas por lockfile.
3. `.github/workflows/backend-ci.yml`: preserva `mvn verify` e PostgreSQL Testcontainers, constrói a imagem e executa typecheck/dry-run do Worker. Não publica nem implanta.
4. `docs/implementation/staging-bloco-02.md`: configuração segura Cloudflare/Neon, bootstrap, deploy manual, verificação e roteiro T01–T24.

## Conexões e inicialização

- `DB_URL`: JDBC para host pooled Neon (`-pooler`) com `sslmode=require`.
- `FLYWAY_URL`: JDBC para host direto Neon, sem `-pooler`, também com TLS.
- `DB_USERNAME` e `DB_PASSWORD`: papel exclusivo de staging.
- `ENTRA_ENABLED=true`, tenant, audience e scope são obrigatórios no Worker.
- O Container usa `basic` (1 GiB) como ponto inicial e uma instância para staging; limites reais de memória, cold start e custo precisam ser avaliados no ambiente antes de ampliar o uso.
- Cada inicialização aplica/verifica Flyway e valida o schema com JPA. Dados persistentes ficam no Neon; disco local do Container é descartável.
- Bootstrap de `GESTOR_GERAL` é uma operação única executada por operador autorizado contra o banco vazio, usando o JAR aprovado e conexão direta. O comando normal mantém `nite.bootstrap.enabled=false`.

## Limites e aceite

Containers da Cloudflare estão em beta e não oferecem SLA de produção conforme documentação atual. O ambiente pode atender à homologação, mas essa limitação precisa ser considerada antes de produção.

O aceite institucional exige implantação real, URL HTTPS, identidades de teste aprovadas, execução da matriz funcional e responsável designado. CI verde, dry-run e configuração no Git não equivalem a homologação. Região Neon, domínio/rota Cloudflare e configurações Entra dependem de escolha e autorização institucional.

## Substituição da decisão anterior

Esta especificação substitui a proposta anterior de VM/Docker Compose. Nenhuma VM foi provisionada. A implementação anterior era apenas configuração versionada e testes descartáveis locais; Compose e smoke test correspondentes são removidos nesta revisão. O Dockerfile permanece somente como formato de imagem requerido por Cloudflare Containers.
