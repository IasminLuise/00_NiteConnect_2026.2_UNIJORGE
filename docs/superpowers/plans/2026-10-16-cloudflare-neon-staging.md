# Staging Cloudflare + Neon — plano de implementação

> Execução direta autorizada nesta conversa; atualização da branch `infra/staging-bloco-02` e do PR5 existente.

**Objetivo:** trocar o staging não implantado em Docker Compose/VM por Worker + Cloudflare Container para API Java e Neon PostgreSQL.

**Arquitetura:** Cloudflare Worker encaminha requisições para um Durable Object/Container único de staging. O Container executa o JAR verificado. Neon guarda os dados; a API conecta pelo endpoint pooled e Flyway pelo endpoint direto, ambos com TLS.

**Tecnologias:** Java 21, Spring Boot, PostgreSQL/JDBC, Docker image, Cloudflare Workers/Containers, Durable Objects, Wrangler e Neon.

**Especificação:** `docs/superpowers/specs/2026-10-16-bloco-02-staging-design.md`.

## Escopo

- Remover Compose de staging, smoke test exclusivo do Compose, empacotamento offline e instruções da VM.
- Manter o Dockerfile necessário à imagem de Container Cloudflare.
- Adicionar Worker/Container, configuração Wrangler, typecheck e guia Neon/Cloudflare.
- Manter Maven/Testcontainers e os testes existentes do backend.
- Não provisionar recursos cloud, registrar secrets ou implantar sem conta e valores autorizados.

## Etapas

1. Criar app Wrangler em `infra/cloudflare/` com rota HTTP e binding Durable Object para o Container; passar somente bindings necessários ao Java em runtime.
2. Separar endpoint da aplicação e endpoint Flyway em `application.yml`, mantendo fallback retrocompatível para ambientes existentes.
3. Remover Compose/smoke de staging e substituir o workflow de bundle offline por validação de imagem e Worker sem deploy.
4. Reescrever runbook, spec e README para Neon/Cloudflare, com secrets interativos, bootstrap único, deploy manual, limitations beta e aceite institucional.
5. Executar validações do backend, do Dockerfile e da configuração Wrangler; revisar diff e atualizar PR5.

## Foco de revisão

- Secrets não podem aparecer em `vars`, logs, artefatos ou linha de comando persistida.
- Migração Flyway deve usar endpoint direto; chamadas da aplicação devem usar endpoint pooled.
- Inicialização/cold start precisa aguardar API pronta antes do proxy encaminhar requisições.
- O Worker não deve contornar autenticação e autorização da API.
- README, CI, spec e runbook não podem continuar indicando VM/Compose como caminho ativo.
