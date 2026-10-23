# Bloco 06 — Registro de preparação e execução

## Estado em 23/10/2026

Pedido recebido: implementar o Bloco 06 conforme o contrato B06.0. Preparação da execução concluída; [plano detalhado](../superpowers/plans/2026-10-23-bloco-06-academico.md) aguardando revisão e escolha da forma de execução. Nenhum código acadêmico ou migração criado nesta preparação.

O checkout `D:\nite-connect` permanece na investigação B05. A preparação usa o worktree `C:\Users\breno\.codex\worktrees\b06-contrato\nite-connect`, branch `docs/bloco-06-contrato`, baseado em `d787916bbd581ee2ec4c9d2ee85fe5af02f97239`, acrescido do contrato `48786c4`.

## G0-B — Evidências dos ambientes conhecidos

Consultas feitas em 23/10/2026, somente leitura do banco. O contêiner local já existente `nite-connect-db-1` foi iniciado para consulta. Nenhuma migração foi aplicada ao banco persistente local nem ao Neon; a execução dos testes utiliza bancos efêmeros próprios.

### PostgreSQL local persistente

- Ambiente: contêiner `nite-connect-db-1`, imagem `postgres:17-alpine`, banco `niteconnect`.
- Tabela de controle localizada em `public.flyway_schema_history`.
- Consulta: `SELECT installed_rank,version,description,type,script,checksum,installed_on,success FROM public.flyway_schema_history ORDER BY installed_rank`.
- Histórico encontrado:

| Versão | Script | Checksum | Sucesso |
|---|---|---|---|
| 1 | `V1__initialize_schema.sql` | -894135534 | Sim |
| 2 | `V2__identity_members_access_audit.sql` | -1741155028 | Sim |
| 3 | `V3__projects_proposals_memberships.sql` | 21855801 | Sim |

Os três checksums coincidem com CRC32 dos arquivos correspondentes da base, com leitura por linhas UTF-8 sem terminadores, no formato utilizado pelo Flyway. Essa comparação não constitui execução de `flyway validate` no banco persistente. Não há V4, V5 ou V6 no histórico consultado; o fato de V4/V5 existirem no repositório não implica que tenham sido aplicadas nesse ambiente.

### Neon staging

- Projeto `nite-connect-staging` (`empty-tooth-04776998`), PostgreSQL 17.
- Única branch retornada pelo inventário: `main` (`br-sweet-wave-b6621efl`).
- Banco listado: `niteconnect_staging`.
- Consulta: `SELECT schemaname, tablename FROM pg_tables WHERE schemaname NOT IN ('pg_catalog','information_schema') ORDER BY schemaname,tablename`.
- Resultado: nenhuma linha. Banco sem tabelas de aplicação ou `flyway_schema_history` na consulta atual; nenhuma migração aplicada por esta tarefa.

### Repositório e migrações futuras

- `origin/main` atualizado para `d787916bbd581ee2ec4c9d2ee85fe5af02f97239` na preparação do contrato.
- Inventário remoto atualizado via GitHub API: 15 branches, incluindo as branches B05 e a documental B06; árvore de cada commit consultada recursivamente.
- Nenhuma dessas árvores contém arquivo de migração V6 ou superior em `apps/api/src/main/resources/db/migration`.
- V6 documental é referência de planejamento do B05, não migração encontrada/aplicada nessas evidências.

**Estratégia técnica proposta para a implementação:** `V6__academic_assessment.sql`; V1–V5 imutáveis. O B05 usará a próxima versão disponível quando sua implementação for autorizada, sem migração retroativa, sem `outOfOrder` ou `repair`. A referência futura nos documentos B05 deve ser reconciliada em alteração própria, sem transportar implementação/experimentos B05.

Esse inventário cobre os ambientes conhecidos e identificados no projeto; não afirma ter auditado um ambiente de produção ou outros bancos não identificados. Se aparecer migração conflitante em outro ambiente/branch antes da implementação, interromper a escolha do número e reconciliar sem editar histórico aplicado.

## Baseline executado antes de modificar produto

Diretório: `apps/api` no worktree isolado.

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot'
.\mvnw.cmd --batch-mode verify
```

Resultado observado: **185 testes, 0 falhas, 0 erros, 0 ignorados; BUILD SUCCESS**, duração Maven 02:20 min. PostgreSQL 17/Testcontainers, cadeia Flyway existente e empacotamento JAR executados. Log local: `apps/api/target-b06-baseline.log` (não versionado).

Este resultado é baseline atual de B01–B04; não valida código B06 ainda inexistente. Alterações exclusivamente documentais posteriores não invalidam a execução. A implementação exigirá testes direcionados e regressão final conforme o plano.

## Decisões registradas

- Regra de produção ainda não homologada não será inventada: parâmetros ausentes bloqueiam as operações dependentes.
- A ausência de evidência documental oficial do B05 não será mascarada por provedor de produto fictício, nota zero ou redistribuição de pesos.
- Fluxo positivo de homologação pode ser testado com provedor sintético exclusivo do classpath de teste, sem sugerir prontidão institucional ou liberar a integração no runtime.
- Aprovação do plano/forma de execução permanece pendente nesta entrega de preparação; nenhuma implementação de produto foi iniciada antecipadamente.
