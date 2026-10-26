# B05 — resultados da implementação e decisão do gate

Data da execução: 26/10/2026. Branch isolada `feat/b05-documents-diary`, derivada da `main` `39ae59cc0ad03f126fbc111d7ff00275f9a25444`. O diff não contém alterações acadêmicas/B06, documentos oficiais, hashes dos documentos ou evidências privadas. Não houve deploy, alteração de credenciais ou migração em banco persistente.

## Entrega técnica desta branch

- Núcleo documental portado seletivamente do trabalho B05 anterior: catálogo versionado, documentos por contexto/ACL, versões imutáveis, trilha, arquivamento/restauração, sessões ONLYOFFICE Docs API, callbacks, download DOCX e PDF associado à versão exata. Storage local e editor permanecem desativados por padrão; documentos oficiais mantêm veto de edição/conversão.
- Migrações V6 (`document_management`) e V7 (`continuous_member_diaries`). O Diário inicial é importado manualmente como versão imutável; registra integrante, autor, instante, motivo e datas de origem somente quando fornecidas. As datas externas ficam marcadas `SHAREPOINT`. Não há integração Graph.
- APIs `POST /api/v1/diaries/{memberId}/import`, `GET /api/v1/diaries/me`, `GET /api/v1/diaries/{memberId}` e `GET /api/v1/diaries/me/activities?from&to&page&size`. Autor lê/edita; coordenação e gestor geral leem/auditam; importação inicial é administrativa. O painel combina atividades com prazo e evidências pessoais, sem gravar no DOCX nem validar presença/horas.
- Interface mínima Next.js/TypeScript para Diário, painel semanal, editor e downloads por versão. Um proxy same-origin permite somente as rotas documentais necessárias, exige Bearer token, recusa rotas não listadas e exige HTTPS para API fora do loopback. Não cria login fictício: o provedor Entra não existe nesta base e a página desabilita as ações até receber a sessão.
- Hyperlinks OOXML externos HTTPS são preservados como relações sem dereferenciamento. HTTP, esquemas inseguros e outros relacionamentos externos são recusados pelo guard.

## Evidências de motor e documentos oficiais

**G1-F permanece NO-GO.** A suíte real do runtime ONLYOFFICE com os dois modelos oficiais, controles Word correspondentes e reabertura no Word não foi executada nesta branch. Não há imagem DocumentServer no Docker local. Não foram copiados, abertos ou editados os originais nesta execução.

A evidência versionada anterior continua limitada e aponta riscos concretos:

- O relatório B05.0.1 reproduz, em fixture sintética, a conversão do `SAVEDATE` simples para `REF` no ONLYOFFICE `10.0.0.1`; a variante complexa preservou a instrução em ensaio limitado, mas não homologou o AV3 oficial.
- O relatório B05.0.3 registrou as falhas de fidelidade do motor e marcou modelos oficiais como bloqueados/not-run. Em campanha anterior, o relatório semanal foi parado antes do editor pelos hyperlinks HTTPS; esse resultado não comprova perda pelo motor.
- A evidência bruta `.runtime/b0503` referenciada pelo relatório histórico não está disponível nesta cópia. Os resultados de quatro/cinco páginas não foram reconciliados por bytes/configuração/fluxo nesta execução; portanto não são tratados como se viessem do mesmo documento.
- A fonte SDKJS no commit `3679e0375a758d7dc59d31337a45461be3525ed8` foi obtida em snapshot privado reduzido. O patch SAVEDATE passou no teste de parser/localidade/switch/F9/NUMPAGES. **Isso é verificação de fonte, não build de DocumentServer, imagem com digest, ciclo normal, nem aprovação de modelo oficial.** Manifesto/hashes da fonte e patch permanecem em `.private/`.

Sem os três cenários normais de edição/callback/reabertura para cada modelo, inspeção de todas as páginas e controles Word equivalentes, não há base para aceitar fidelidade ou habilitar edição oficial. A aprovação G1 de seleção do ONLYOFFICE permanece distinta do G1-F técnico/editorial.

## Validações executadas

| Verificação | Resultado |
|---|---|
| `apps/api`: `.\mvnw.cmd --batch-mode verify` | **BUILD SUCCESS**; 236 testes, 0 falhas, 0 erros, 1 ignorado. O skip foi `rejectsSymbolicLinkRootAncestorAndObjectWhenSupported`, pois o Windows não habilitou o caso de symlink. PostgreSQL 17 efêmero via Testcontainers validou banco vazio e upgrade V5→V7. |
| Backend focado antes da suíte final: Diary, ACL, OOXML e migrations | 18 testes, 0 falhas/erros/skips. |
| `apps/web`: `npm run typecheck` e `npm run build` | ambos aprovados; rotas `/`, `/api/v1/[...path]` e ícone construídas. |
| Proxy web local | `/` respondeu 200; Diário sem token 401; rota fora da lista 404. Servidor de desenvolvimento encerrado após o smoke. |
| `python -m unittest discover -s experiments/b05-document-engine/scripts -p "test_*.py"` | **29 testes OK**. As advertências de nomes ZIP duplicados são fixtures negativas existentes. |
| `node experiments/b05-document-engine/scripts/test_savedate_sdkjs_source.cjs` | **PASS** após carregar a fonte imutável fixada e o patch somente em `.private/`; sem runtime DocumentServer. |
| `git diff --check` | aprovado. |

A primeira suíte completa reportou uma expectativa de migração antiga que aplicava V6/V7 ao testar especificamente V4→V5. O teste B04 agora fixa `target("5")`; o teste documental separado cobre V5→V7. Também removi a fragilidade de um teste preexistente de presença que fixava a data antes de uma virada de dia, vinculando a consulta ao dia local do evento criado. Ambos passaram na suíte final.

## Auditoria Flyway e decisões de execução

Leituras anteriores nesta tarefa encontraram Neon staging sem `flyway_schema_history` e um relatório local persistente conhecido em V1–V3. Outro serviço PostgreSQL Windows na porta 5432 não pôde ser identificado/autenticado; nenhuma migração foi aplicada nele. V6/V7 ficam nesta branch para base `main` limpa/testes e os ambientes auditados. Antes de migrar qualquer banco persistente, auditar o serviço desconhecido e o alvo específico. As migrações B06 futuras devem ser renumeradas/rebased se disputarem V6/V7.

**Ruling — numeração V6/V7:** adotada para esta branch após as leituras somente leitura acima. Custo se o serviço não auditado já tiver versão posterior: conflito de histórico. Mitigação: nenhuma aplicação persistente até auditoria.

**Ruling — autenticação web:** a página fica inoperante até B01 injetar a sessão Entra; não foi criado armazenamento de token/login alternativo. Custo: sem fluxo ponta a ponta autônomo nesta branch. Mitigação: conectar o provedor institucional antes de habilitar a tela.

## Pendências e próximos gates

1. Restaurar evidências privadas anteriores disponíveis no worktree de auditoria e reconciliar cada DOCX por hash, runtime/digest, configuração, exportação e permissão. Esta cópia não tem o bruto `.runtime/b0503`.
2. Executar current runtime `10.0.0.1` e no máximo um candidato de DocumentServer construído de fonte fixada, cada qual com digest/configuração registrada. O teste de fonte feito não substitui o build candidato.
3. Executar abertura sem edição, alteração+remoção e três ciclos de preenchimento nos dois modelos, no fluxo normal, produzir controles Word com bytes/conteúdo equivalentes, callback e reabertura Word, validar campos, hyperlinks, logotipo, tabelas, quebras e PDFs em todas as páginas.
4. Fazer revisão institucional/editorial dos modelos e revisão de licença/manutenção do build candidato.
5. Auditar o serviço PostgreSQL local em 5432 antes de qualquer migração persistente; integrar o token provider Entra; rebasear B06 depois, sem tocar seus PRs nesta branch.
6. Definir armazenamento persistente e aceites de operação antes de produção. Nenhum deployment ou storage remoto foi feito.

A decisão desta branch é **GO para revisão de código/testes em desenvolvimento; NO-GO para habilitar edição dos modelos oficiais, aceitar G1-F ou ir a produção**. A quantidade de páginas não foi convertida em porcentagem de fidelidade.
