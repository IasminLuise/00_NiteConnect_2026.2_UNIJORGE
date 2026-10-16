# Plano — staging do Bloco 02

Especificação: `docs/superpowers/specs/2026-10-16-bloco-02-staging-design.md`.
Execução direta autorizada na conversa, na branch `infra/staging-bloco-02`.

1. Criar `infra/tests/staging-smoke.ps1`: instância descartável com identidades
   fictícias, validação de rede, processo sem root, healthcheck, HTTP 401,
   Flyway V1–V3 e persistência após reinício. Confirmar falha sem Compose.
2. Adicionar `apps/api/Dockerfile`, `apps/api/.dockerignore` e
   `infra/compose.staging.yaml`. A imagem copia o JAR validado, executa sem root;
   Compose exige configuração Entra e segredo, publica somente API em loopback.
3. Executar `apps/api/mvnw.cmd --batch-mode verify`, construir imagem e executar
   o smoke test. Corrigir problemas antes de empacotar.
4. Atualizar CI para testar a imagem também em PRs e exportar bundle somente
   em push na main: imagem Docker, Compose, metadados SHA e SHA256SUMS.
5. Escrever runbook operacional e roteiro de aceite, atualizar entrada ativa do
   README e revisar diff. Não provisionar infraestrutura externa.
6. Revisão independente da branch, commit, push e PR com evidências de execução.

Conclusão local requer testes e empacotamento aprovados. Homologação institucional
continua pendente até execução na VM com contas reais de teste e aceite registrado.
