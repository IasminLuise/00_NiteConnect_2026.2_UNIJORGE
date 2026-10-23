# B05 Core / Docs API — execução local de 23/10/2026

## Escopo e estado

Implementação autorizada pelo pedido de execução do planejamento ADR B05-005, com decisão explícita de bytes **apenas locais de desenvolvimento**. Branch `feat/b05-core-docs-api`, base B06 `01b3fd91b7d046ae937c6afcec6cecc680a7d14d`. A base contém V6/V7 acadêmicas; B05 acrescenta V8. O checkout de auditoria `D:\nite-connect` foi preservado, assim como suas alterações e evidências. Não foram incorporados patches experimentais ao produto nem alterado o gate Python original.

M1: catálogo e versões recuperáveis de modelos, documentos por contexto, ACL explícita, versões imutáveis, trilha/download, arquivamento e restauração administrativa.

M2 local: contrato `DocumentEditorProvider`, adaptador ONLYOFFICE Docs API desativado por padrão, configuração JWT, fontes efêmeras com HMAC, callbacks autenticados/autorizados e idempotentes, publicação transacional ou quarentena e PDF fixado à versão. Não houve frontend novo ou uso de documentos oficiais. Configuração operacional e contratos: [B05 local](bloco-05-core-docs-api.md).

A implementação do protocolo foi exercitada com servidor HTTP sintético e adaptador efetivo. **Não é uma execução do runtime ONLYOFFICE nem prova de edição no navegador.** O ensaio real da implantação isolada, desempenho/concorrência do motor, editorial/Word, licença, infraestrutura persistente e M3 continuam pendentes. O veto de edição/conversão oficial é fixo; não há flag para liberá-lo nesta implementação. B06 continua sem provedor documental homologado.

## Verificações finais

Executado após a última alteração de código em Windows, Java 21.0.12.1, Docker 29.9.1 e PostgreSQL 17.11:

```powershell
Set-Location apps/api
.\mvnw.cmd --batch-mode verify
```

**BUILD SUCCESS em 03:14 min**, concluído às 15:46 de 23/10/2026 (America/Bahia). Surefire: **261 testes, 0 falhas, 0 erros, 1 ignorado** — 260 aprovados. Desses, 43 são testes novos do B05: 42 aprovados e 1 ignorado. O teste condicional de link simbólico em `DocumentObjectStoreTest` foi ignorado porque o Windows não concedeu a permissão necessária; não há aprovação presumida dessa verificação. O pacote JAR foi construído e reempacotado com sucesso.

`git diff --check`: sucesso. `git status --short` conferido no worktree isolado; apenas os arquivos desta implementação. Migrações V1–V7 e arquivos `.env` permanecem sem alteração. `.private/`, logs e build são ignorados pelo Git.

As verificações direcionadas anteriores (35 testes, zero falhas/erros, um skip) foram substituídas pela suíte final completa. A primeira suíte ampla revelou a regressão de bootstrap corrigida abaixo. A segunda foi interrompida e não conta como evidência de aprovação; a execução final começou com containers novos e concluiu integralmente. Log local ignorado: `apps/api/target-b05-resumed-verify.log`; relatórios XML em `apps/api/target/surefire-reports`.

Não executados por não pertencerem a este escopo: suítes Python/Node dos experimentos preservados no checkout de auditoria, novas campanhas Word/PDF/SDK e camadas frontend. Não houve mudança nesses experimentos. Pendentes: runtime real do motor, navegador, aceite de fidelidade e produção. A CI remota é uma verificação separada, ainda não registrada como aprovada neste relatório.

## Evidências cobertas pelos testes novos

| Grupo | Proteções verificadas |
| --- | --- |
| `DocumentObjectStoreTest` | Opt-in local e bloqueio em produção, chaves privadas, imutabilidade, corrupção/tamanho e symlink quando permitido pelo SO |
| `DocxIntegrityTest` | Texto permitido; regressões de fonte/espaçamento/campo/partes, ZIP/XML perigoso, conteúdo protegido fora da raiz e divergência entre diretório central/local |
| `OnlyOfficeDocsApiAdapterTest` | JWT root/payload e assinatura/expiração/algoritmo, users assinados, configuração read-only, origens/credenciais/redirects, limites/conversão e PDF excessivo 502 |
| `DocumentCoreTest` | Acesso pessoal, isolamento entre colegas, versões/restauração, quarentena, inatividade/CAS e veto oficial |
| `DocumentTemplateCatalogTest` | Fluxo catálogo→ID de versão→atribuição, paginação, privacidade do storage e administração |
| `DocumentAccessConcurrencyTest` | Vínculo de projeto além da ACL, grant leitura/edição, duas gravações simultâneas, restauração cruzada/quarentena e arquivamento confirmado |
| `DocumentEditorWorkflowTest` | Ciclo compartilhado, adulteração/expiração/revogação de fonte, assinatura e ACL do callback, JSON inválido, replay, saves, falha/retry e PDF ligado à origem exata |
| `DocumentMigrationTest` | Atualização V7→V8 preservando auditoria, ampliação limitada dos tipos documentais e rejeição de tipo desconhecido |

As classes integradas usam PostgreSQL 17 real via Testcontainers e MockMvc. Apenas o motor remoto é simulado por servidor HTTP, sem mock do adaptador. O teste de concorrência da API exige exatamente uma publicação para duas requisições com a mesma revisão. Isso não mede a capacidade de coedição real do fornecedor nem prova que toda corrida de callback foi exaurida.

## Correções identificadas durante a execução

- A primeira integração expôs restrição preexistente de tipos em `audit_events`: o domínio novo gerava conflito 409. V8 estende explicitamente a constraint com DOCUMENT/DOCUMENT_TEMPLATE, preservando os tipos anteriores; migrações já existentes não foram alteradas.
- Revisão independente identificou lacuna de consulta de versões do catálogo: criada rota administrativa paginada, testando reconstrução do fluxo por GET sem depender da resposta anterior de upload.
- A atualização CAS de publicação agora exige exatamente uma linha alterada; JSON inválido de callback retorna 400; limite do conversor foi alinhado a 8 MiB para falha upstream 502 antes do armazenamento.
- A suíte completa identificou regressão no bootstrap sem web: a nova cadeia de segurança do motor dependia de HttpSecurity fora de aplicação Servlet. Ela agora segue a mesma condição Servlet da segurança principal; o teste existente de bootstrap preserva a regressão.
- O teste histórico B06 de migração foi fixado em target V7, conservando seu contrato. Não é ocultação de V8: o teste documental cobre V7→V8 e as inicializações da suíte completa aplicam V1→V8.

Nenhum problema foi atribuído a credenciais institucionais ou ao runtime ONLYOFFICE nesta rodada, pois não foram usados. Não há inferência de fidelidade a partir dos testes positivos sintéticos.

## Limitações operacionais

- Storage exige ACL privada do SO nos ancestrais; não é adaptador de produção nem armazém institucional. Banco e objetos precisam de backup conjunto; órfãos de transação falha ficam recuperáveis, sem coleta automática.
- Guard conservador aceita apenas alterações textuais em nós existentes do corpo e exceção delimitada de metadados; mudanças legítimas de estrutura pelo Word/motor podem ir para quarentena. Não existe normalizador/reparador silencioso.
- Ciclos têm TTL de 15 minutos; fontes de conversão, 5 minutos. Expiração/revogação exige reabrir sessão. Estados 4/3/7 são terminais nesta implementação; reconexão tardia ao mesmo ciclo é recusada.
- Um editor pode revogar um ciclo compartilhado; bytes já persistidos permanecem. `error:0` significa receipt durável, não aprovação editorial.
- O runtime real e sua topologia TLS/proxy devem ser validados antes de declarar M2 operacional. Nenhum novo serviço foi provisionado nem produção alterada; Entra e segredos permanecem sob configuração do operador.
