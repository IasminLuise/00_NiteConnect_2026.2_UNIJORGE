# Staging institucional — Bloco 02

## Estado e pré-requisitos

Este pacote prepara a implantação; não comprova execução na VM ou aceite
institucional. O backend ainda não possui frontend. A homologação usa a API com
Postman/cliente OAuth institucional, contas de teste aprovadas e dados sintéticos.

O operador precisa confirmar:

- VM Linux x86_64 com Docker Engine e Docker Compose, disco para imagem/volume e
  acesso administrativo institucional. O artefato inicial é linux/amd64.
- URL HTTPS, DNS e proxy reverso no mesmo host apontando para
  http://127.0.0.1:8080. A API é publicada só em loopback; o banco não tem porta
  publicada. Encaminhar Authorization sem registrar tokens no proxy.
- Saída de rede para Microsoft Entra/JWKS.
- Tenant, client ID da API e escopo delegado aprovados; identidade administrativa
  de teste e pelo menos duas outras identidades para testar isolamento contextual.
- Operador, testadores, janela e responsável pelo aceite definidos pela instituição.

Administradores Docker podem inspecionar variáveis dos contêineres. O arquivo de
configuração deve ficar fora do Git, protegido e acessível somente aos operadores.
Não usar dados acadêmicos reais ou copiar o banco de produção.

## Artefato validado

Backend CI executa Maven verify, constrói a imagem do JAR testado e exercita o
Compose descartável. Somente push na main aprovado nessas etapas publica o
artefato nite-connect-staging-<SHA> no GitHub Actions, disponível por 14 dias:

- api-image.tar: imagem nite-connect-api:<SHA>, sem credenciais.
- compose.staging.yaml: contrato de execução daquele release.
- RELEASE.txt: commit completo, plataforma e ID da imagem.
- RUNBOOK.md e SHA256SUMS: procedimento e integridade dos arquivos.

Baixar da execução bem-sucedida correspondente ao commit da main, confirmar o SHA
no GitHub e transferir por canal institucional autenticado. Checksums detectam
corrupção; a procedência é a execução CI autenticada do repositório.
Na VM, dentro do diretório do bundle:

~~~bash
sha256sum -c SHA256SUMS
cat RELEASE.txt
docker load --input api-image.tar
~~~

Conferir o ID carregado com IMAGE_ID do RELEASE e o SHA da tag. Não substituir
por latest, reconstruir na VM ou usar bundle de PR. Não há registry,
deploy automático ou credenciais SSH no workflow.

## Configuração externa

O operador cria /etc/nite-connect/staging.env fora do checkout, com permissão
0600 e diretório protegido. Preencher no gerenciador/editor seguro, sem histórico
de shell/transcrição; não enviar valores ao chat:

| Variável | Valor |
|---|---|
| STAGING_IMAGE_TAG | SHA completo de RELEASE, conferido no GitHub |
| DB_PASSWORD | Senha exclusiva do banco de staging |
| ENTRA_TENANT_ID | UUID do tenant autorizado |
| ENTRA_AUDIENCE | Client ID da API, audience do access token v2 |
| ENTRA_REQUIRED_SCOPE | Nome do escopo delegado, sem prefixo de URI |
| STAGING_API_PORT | Opcional; padrão 8080 em loopback |

Usar a sintaxe de env-file do Compose; valores com caracteres especiais devem
ser representados corretamente, por exemplo com aspas simples para preservar $.
ENTRA_ENABLED=true é fixado pelo Compose. Sem parâmetros obrigatórios o Compose
recusa iniciar. Não colocar opções de bootstrap nesse arquivo.

Não imprimir docker compose config ou docker inspect em logs: podem conter
segredos. Para validar sem exibir valores:

~~~bash
docker compose --env-file /etc/nite-connect/staging.env -f compose.staging.yaml config --quiet
~~~

O banco é niteconnect_staging, usuário niteconnect, no volume exclusivo do
projeto nite-connect-staging. DB_URL aponta para o serviço db; não recebe URL
externa. Trocar DB_PASSWORD no arquivo não altera senha de banco já criado:
uma rotação exige procedimento coordenado no PostgreSQL e na aplicação.

## Primeira inicialização e administrador

Iniciar somente o banco:

~~~bash
docker compose --env-file /etc/nite-connect/staging.env -f compose.staging.yaml up -d --wait db
~~~

O operador disponibiliza no próprio processo, por mecanismo seguro, as variáveis
NITE_BOOTSTRAP_OBJECT_ID, NITE_BOOTSTRAP_NAME, NITE_BOOTSTRAP_INSTITUTIONAL_EMAIL
e NITE_BOOTSTRAP_REASON, com a identidade institucional previamente conferida.
O comando transmite os nomes, sem valores escritos na linha de comando:

~~~bash
docker compose --env-file /etc/nite-connect/staging.env -f compose.staging.yaml run --rm --no-deps \
  -e NITE_BOOTSTRAP_OBJECT_ID -e NITE_BOOTSTRAP_NAME \
  -e NITE_BOOTSTRAP_INSTITUTIONAL_EMAIL -e NITE_BOOTSTRAP_REASON \
  -e NITE_BOOTSTRAP_ROLE=GESTOR_GERAL \
  api --spring.main.web-application-type=none --nite.bootstrap.enabled=true
unset NITE_BOOTSTRAP_OBJECT_ID NITE_BOOTSTRAP_NAME NITE_BOOTSTRAP_INSTITUTIONAL_EMAIL NITE_BOOTSTRAP_REASON
~~~

Esse processo aplica Flyway V1–V3, provisiona o administrador transacionalmente e
encerra sem servidor HTTP. Conferir saída zero antes de continuar. O bootstrap
recusa banco já provisionado; não repetir em atualizações. A execução normal da
imagem força bootstrap desabilitado. Os demais integrantes ingressam por convite
autenticado, com papéis atribuídos pela API.

~~~bash
docker compose --env-file /etc/nite-connect/staging.env -f compose.staging.yaml up -d api
curl --fail http://127.0.0.1:8080/actuator/health
docker compose --env-file /etc/nite-connect/staging.env -f compose.staging.yaml exec -T db \
  psql -U niteconnect -d niteconnect_staging -c \
  'select version, success from public.flyway_schema_history order by installed_rank;'
~~~

Se STAGING_API_PORT foi alterado, ajustar curl/proxy. Esperado: health UP,
versões 1, 2 e 3 bem-sucedidas. Conferir também pelo domínio HTTPS aprovado.
Sem token, /api/v1/projects deve retornar 401. A API deve continuar saudável
após reinício; o volume não deve ser apagado.

## Atualização e recuperação

Conferir novo bundle, preservar o release anterior e providenciar snapshot/backup
restaurável do banco de staging antes de alteração de schema. Parar a API antiga,
atualizar a tag no arquivo externo e iniciar uma instância. O startup
valida/aplica Flyway antes de atender requisições; falha impede o serviço.

Falha: parar a API, preservar logs/volume e diagnosticar. Reverter apenas a imagem
quando compatível com o schema; caso contrário, usar recuperação de staging
aprovada pelo operador. Não reescrever migrações, fazer downgrade SQL ou executar
down --volumes no staging institucional. Reconstrução com perda dos dados de
teste exige autorização do responsável.

Logs locais têm rotação (10 MiB, três arquivos por serviço); ajustar ao padrão
operacional institucional. Não gravar tokens ou dados pessoais nas evidências.
Retenção e política de backup serão institucionais.

## Roteiro de aceite

Registrar data, URL, SHA completo, execução CI, versões Flyway, testadores e
responsável pelo aceite. Usar contratos dos Blocos 01 e 02; o token deve ser
access token v2 para a API. Login Microsoft sozinho não concede autorização.

| Grupo | Execução no staging | Evidência esperada |
|---|---|---|
| Autenticação | Administrador real de teste faz GET /api/v1/me; testar sem token e integrante não vinculado | 200 para identidade provisionada; negações corretas |
| T01–T05, T13 | Administração cria dois projetos; integrantes ingressam; gestor altera o próprio e tenta o alheio | Persistência e vínculos independentes; acesso alheio recusado |
| T06–T10, T21 | Transferir liderança, explicitar vacância, saída/retorno, desligamento e retirada de autoridade | Uma liderança vigente; históricos preservados; nova requisição perde acesso |
| T11–T12 | Ativar, pausar, retomar, concluir e reabrir com motivos e versões atuais | Histórico completo e estado final correto |
| T14–T16 | Propor, aprovar e converter; repetir e enviar duas conversões concorrentes | Origem preservada, um projeto e conflito na tentativa duplicada |
| T17, T22–T23 | Integrante inexistente/inativo, versões antigas, motivo ausente e datas inválidas | Erro correspondente sem alteração parcial |
| T18–T20, T24 | Conferir CI do release e versões na VM | Rollback de auditoria, V2→V3, instalação limpa e 58 testes anteriores comprovados pela suíte isolada |

Não injetar triggers de falha no banco institucional: T18 é coberto pelo teste
isolado automatizado. Corridas de liderança/conversão também têm testes
automatizados; verificações no staging avaliam o release implantado.

Registrar cada caso como aprovado/reprovado/não executado, resultado esperado,
observado e evidência sem segredos. Falha relevante impede aceite.
Somente o responsável institucional designado registra a homologação formal.
Pacote pronto, CI verde e smoke local não substituem esse registro.

## Validação local do pacote

Após executar mvnw.cmd --batch-mode verify em apps/api, na raiz:

~~~powershell
docker build --platform linux/amd64 -t nite-connect-api:smoke-local apps/api
pwsh -NoProfile -File infra/tests/staging-smoke.ps1 -ImageTag smoke-local
~~~

O teste cria projeto Compose com nome aleatório, porta efêmera, banco/segredo
descartáveis; verifica rede, usuário sem root, health, HTTP 401, migrações e
bootstrap e reinício. Ao terminar remove somente recursos desse projeto descartável e
restaura variáveis de processo anteriores.
