# Bloco 06 — Registro de preparação e execução

## Estado em 23/10/2026

Pedido recebido: implementar o Bloco 06 conforme o contrato B06.0. O [plano detalhado](../superpowers/plans/2026-10-23-bloco-06-academico.md) foi aprovado explicitamente pelo usuário, com execução nesta sessão e revisão independente ao final. Núcleo acadêmico implementado, revisão independente tratada e validação local final aprovada; isso não representa ativação institucional.

O checkout `D:\nite-connect` permanece na investigação B05. A implementação usa o worktree `C:\Users\breno\.codex\worktrees\b06-contrato\nite-connect`, branch `feat/bloco-06-avaliacao-academica`, derivada de `22415af` (contrato/plano), sobre a base `d787916bbd581ee2ec4c9d2ee85fe5af02f97239` da `main`.

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

**Estratégia adotada após aprovação do plano:** `V6__academic_assessment.sql`; V1–V5 imutáveis. O B05 usará a próxima versão disponível quando sua implementação for autorizada, sem migração retroativa, sem `outOfOrder` ou `repair`. Os documentos B05 não existem na base `main` deste worktree: a referência futura das branches B05 deve ser reconciliada quando esse trabalho avançar, sem transportar implementação/experimentos nesta branch.

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
- Aprovação recebida antes de implementar: executar nesta sessão com revisão independente ao final. G0-A aprovado por esse aceite do contrato/plano; G0-B auditado e estratégia V6 adotada para os ambientes conhecidos. Não é homologação institucional ou auditoria de bancos não identificados.

## Execução, desvios técnicos e evidências

- B06.1–B06.3: períodos, políticas confirmáveis/imutáveis, autorização específica, adesão opcional e Plano Individual versionado. Testes HTTP observados RED→GREEN; núcleo inicial com 5 testes aprovados e plano com 1 teste aprovado.
- B06.4: leitores autorizados nos módulos de origem, captura imutável, comparação de revisão e apresentação atestada pela coordenação; 2 testes de evidência observados RED→GREEN.
- B06.5–B06.7: notas humanas, cálculo decimal com insumos preservados, revisão, homologação e reabertura; testes direcionados de cálculo/avaliação/homologação aprovados após corrigir o cast de JSONB no vínculo inicial de referências.
- B06.8: contrato documental futuro sem implementação fictícia, contracts de notificação sem envio e encaminhamento condicionado a procedimento institucional; teste positivo de encaminhamento exclusivo de fixture sintética.
- Concorrência/revisão/encaminhamento/migração: grupo de 6 testes aprovado; homologações concorrentes produzem uma decisão e revogação enquanto a operação aguarda o lock impede gravação.
- Integridade: vínculo entre avaliação e evidência é relacional, com chaves compostas da adesão e proteção de histórico. V6 nunca foi aplicada a banco persistente pela tarefa; somente a bancos efêmeros de teste.
- Ruling: reutilizar diretamente `AdministrativeTransactions` em vez de criar `AcademicTransactions` genérico — o protocolo já serializa as mutações operacionais relevantes e evita capturar snapshot SQL antes da aquisição do lock; custo: contenção global já existente em operações de baixo volume.
- Ruling: permitir descoberta de políticas confirmadas a integrantes ativos — a adesão precisa conhecer critérios e identificadores; rascunhos ficam restritos ao coordenador; custo: exposição interna dos critérios oficiais, necessária ao fluxo.
- Ruling: publicar OpenAPI em JSON, não YAML — formato nativo sem adicionar parser/dependência; custo: arquivo mais extenso, mantendo compatibilidade OpenAPI.

## Validação final

Revisão independente realizada sobre a branch completa até `d4bc737`, em contexto novo. Nenhum achado crítico; quatro achados importantes tratados na rodada de correção:

1. Projeções do gestor geral sem notas individuais, motivos privados ou insumos completos de cálculo; auditoria limitada a metadados. A mesma proteção foi estendida à listagem de atributos das evidências por não coordenadores.
2. Motivos administrativos de revisões iniciadas pela coordenação ocultos na consulta própria.
3. Divulgação própria restrita aos cálculos realmente homologados e às revisões de notas efetivamente utilizadas, preservando o histórico já publicado.
4. Orientação pública ao integrante em `feedback`, separada de `reason` privado, nas decisões de adesão/plano. V7 aditiva preserva V6 já publicada.

Regressões: cinco testes de privacidade observados com cinco falhas esperadas antes das correções; grupo de 11 testes de privacidade/concorrência/B04/migração aprovado depois. O sexto teste reproduziu separadamente a exposição de atributos na listagem (uma falha, zero erros) antes da proteção adicional. Duas tentativas sem Docker disponível falharam na inicialização e não contam como evidência RED de comportamento. Docker Desktop foi reiniciado; nenhuma configuração de ambiente ou credencial foi alterada.

Cobertura adicional de concorrência usa mutação real B03 enquanto a homologação aguarda o lock; releitura recusa a origem alterada. Leitor B04 cobre recorte do intervalo, período aberto sem duração consolidada e autorização contextual. Não houve segunda rodada de revisão independente: os achados foram tratados com regressão e nova suíte integral conforme o método aprovado.

Após a última alteração de código: `mvnw.cmd --batch-mode verify` com Java 21 — **218 testes, 0 falhas, 0 erros, 0 ignorados; BUILD SUCCESS**, duração 02:26 min, em 23/10/2026. PostgreSQL 17/Testcontainers, migrações V1→V7 e V3→V5→V7 e empacotamento executados. Log local: `apps/api/target-b06-final-verify.log` (ignorado).

Validação documental: OpenAPI JSON corresponde às 40 operações dos controllers, sem operationId duplicado e com referências resolvidas. JAR contém o contrato documental e não contém a fixture/provedor sintético de teste. `git diff --check` sem erros. V1–V5 não alteradas desde a base main; V6 não alterada desde a publicação anterior. Correção de feedback aplicada por V7 aditiva.

As execuções anteriores de 185 testes (baseline), 209 testes (núcleo antes da revisão) e 11 testes direcionados são registros válidos dos estados anteriores; o resultado de 218 testes é a evidência final e as substitui para a conclusão atual. Documentação e atualização do PR após essa execução não alteram o código validado; não exigem outra suíte integral. Frontend, serviços externos, homologação institucional e carga não foram testados, pois não integram este escopo. Banco persistente local e Neon não receberam migrações.

Entrega: [PR #16](https://github.com/IasminLuise/00_NiteConnect_2026.2_UNIJORGE/pull/16), base `docs/bloco-06-contrato` ([PR #15](https://github.com/IasminLuise/00_NiteConnect_2026.2_UNIJORGE/pull/15)). Branch/worktree mantidos para revisão; sem merge/deploy. CI do commit final deve ser consultado no PR, sem usar os checks da versão anterior como prova das correções.

Pendências reais: provedor documental B05 real e parâmetros/rito institucional confirmados; G5 e encerramento funcional integral não declarados. Encaminhamento é registro interno condicionado, sem envio ou confirmação de lançamento universitário.
