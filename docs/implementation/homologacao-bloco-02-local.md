# Evidências de homologação técnica local — Bloco 02

**Data:** 2026-10-16
**Ambiente:** computador local, Docker Desktop, API disponível somente em `127.0.0.1:8081` e PostgreSQL 17 do Compose.
**Escopo:** validação técnica local; não representa aceite institucional nem implantação externa.

## Versão

- O Bloco 02 está integrado à `main` pelo merge commit `8458ec39a0051a1a8800c25aa9be8378c44753ee` ([PR #4](https://github.com/IasminLuise/00_NiteConnect_2026.2_UNIJORGE/pull/4)).
- A API foi verificada a partir da branch local `infra/staging-bloco-02`, commit `fca12a540dd44c1fa9b0c521a5140d861b92e916`. As diferenças desta branch em `apps/api` em relação à `main` são o Dockerfile e a configuração de ambiente externo; o código funcional da API contém o Bloco 02 integrado.
- Imagem local: `nite-connect-api:integrated`, ID `sha256:8d19701fb7b4838b0a4e19a403d77b4bcf111f7d463e1060c1c31581fbb3a242`.

## Execução e banco

- Container `nite-connect-api-local` iniciado e ativo, publicado apenas em `127.0.0.1:8081`; o serviço Java anteriormente ativo em `8080` foi preservado.
- O container conecta ao PostgreSQL 17 local pela rede interna do Compose. Nenhum acesso público foi configurado e nenhum banco Neon ou produção foi alterado.
- No banco local, `public.flyway_schema_history` registra as versões `1`, `2` e `3` com `success=true`.
- O banco tem um integrante ativo, uma identidade institucional vinculada e uma atribuição `GESTOR_GERAL`. Este registro é local e não inclui PII neste relatório.
- No PostgreSQL efêmero do Testcontainers, o teste de migração validou e aplicou as três migrações em schema vazio.

## Verificações executadas

| Verificação | Evidência |
| --- | --- |
| Em `apps/api`: `.\mvnw.cmd --batch-mode verify` com Java 21 | BUILD SUCCESS; 111 testes, 0 falhas, 0 erros, 0 ignorados |
| Inicialização da imagem Docker | Container ativo; Flyway validou/aplicou migração até `v3`; Spring Boot iniciou em `8080` dentro do container |
| `GET http://127.0.0.1:8081/actuator/health` | HTTP 200, `status=UP` |
| `GET http://127.0.0.1:8081/api/v1/projects` sem token | HTTP 401, conforme esperado para API protegida |
| CI do PR #4 | Checks `verify-api` concluídos com sucesso antes do merge |

Os testes automatizados usam identidades sintéticas em bancos de teste isolados; portanto, os cenários de autorização com vários atores não exigem várias contas Entra reais.

## Pendências institucionais

- O fluxo autenticado com a identidade Entra representativa foi executado no Postman Web pelo Desktop Agent local: `GET /api/v1/me` confirmou uma sessão `ATIVO` com os papéis `INTEGRANTE` e `GESTOR_GERAL`; `GET /api/v1/projects` respondeu com lista vazia e paginação válida. O relatório não armazena tokens nem dados pessoais da resposta. Uma única conta institucional permite essa validação representativa; os demais perfis e a separação entre usuários permanecem cobertos pelos testes automatizados, sem equivaler a uma homologação real multi-identidade no Entra.
- O responsável institucional pelo aceite não foi identificado formalmente nesta sessão e nenhum aceite foi presumido ou registrado.
- A URL local `127.0.0.1` só é acessível neste computador. Este passo não publica um staging institucional na internet.

**Resultado:** implantação local e validação técnica concluídas; homologação institucional e aceite formal permanecem pendentes.

## Verificações adicionais via Postman CLI

**Data:** 2026-10-16

| Requisição | Resultado HTTP | Asserção Postman | Interpretação |
| --- | --- | --- | --- |
| `GET /actuator/health` | 200 | Passou | API local saudável (`UP`). |
| `GET /api/v1/me` sem credencial | 401 | Passou | Identidade não é exposta sem autenticação. |
| `GET /api/v1/projects` sem credencial | 401 | Passou | Consulta protegida por autenticação. |
| `POST /api/v1/projects` sem credencial | 401 | Passou | Não foi criado projeto sem autenticação. |
| `POST /api/v1/proposals` sem credencial | 401 | Passou | Não foi criada proposta sem autenticação. |

As quatro respostas 401 são o resultado funcional esperado e suas asserções passaram; o Postman CLI ainda encerra esses comandos com código 1 porque a resposta HTTP não é 2xx. Nesta etapa da CLI, não havia sessão autenticada. A validação posterior com OAuth PKCE pelo Postman Web/Desktop Agent está registrada abaixo; os testes anônimos, isoladamente, não a substituem.

## Reexecução completa e Postman Web

**Data:** 2026-10-16

| Verificação | Resultado |
| --- | --- |
| `apps/api/.\mvnw.cmd --batch-mode verify` com Temurin Java 21.0.12.1 | BUILD SUCCESS; 111 testes, 0 falhas, 0 erros, 0 ignorados. Testcontainers iniciou PostgreSQL 17; as migrações V1–V3 foram validadas e a migração de banco limpo e de V2 para V3 passou. |
| Requisição da coleção `GET postman-echo.com/get` pelo Postman Web | HTTP 200; a asserção existente exige 200. |
| Requisição da coleção `POST postman-echo.com/post` pelo Postman Web | HTTP 200; a asserção existente aceita 200 ou 201. |
| `GET http://127.0.0.1:8081/api/v1/me` autenticado com OAuth 2.0 Authorization Code + PKCE no Postman Web/Desktop Agent | HTTP 200; sessão ativa e papéis institucionais retornados. Resposta com dados pessoais não foi copiada para este relatório. |
| `GET http://127.0.0.1:8081/api/v1/projects` autenticado com o mesmo perfil | HTTP 200; `items=[]`, `page=0`, `size=20`, `total=0`. |

A primeira tentativa do Maven usou Java 17 e não iniciou testes, pois as classes existentes exigem Java 21. A repetição selecionou Temurin Java 21 apenas no processo do comando, sem alterar configuração permanente, e concluiu com sucesso.

A requisição da coleção foi ajustada apenas na sessão do Postman para usar `127.0.0.1:8081` (sem salvar a URL alterada na coleção compartilhada). O Desktop Agent local completou OAuth PKCE sem interação manual e executou as duas consultas autenticadas acima. A suíte automatizada cobre os outros perfis e cenários com identidades sintéticas; não houve mutações de dados institucionais nesta sessão. A homologação institucional formal, o responsável designado pelo aceite e a implantação em ambiente institucional continuam pendentes.

## Credencial do ensaio

Durante a inspeção do gerenciador de tokens do Postman, o bearer token apareceu
na saída de acessibilidade da automação. Seu valor não foi copiado para este
relatório ou para o repositório. A credencial deve ser tratada como comprometida.
O responsável deve encerrar a sessão e verificar as opções de revogação no
Microsoft Entra antes de novo ensaio. Não há evidência de revogação nesta sessão:
um access token já emitido pode continuar válido até expirar, mesmo após encerrar
a sessão ou remover o item do Postman.
