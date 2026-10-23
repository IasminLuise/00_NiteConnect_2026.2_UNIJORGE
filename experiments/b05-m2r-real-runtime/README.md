# B05-M2R — Campanha única de runtime real ONLYOFFICE / PR #17

**Status do pacote original:** executor preparado, ainda sem execução real na entrega do ZIP. **Execução posterior no Windows:** concluída com integração observada e candidata em quarentena; consulte [o relatório e seus limites](results/windows-2026-10-23.md). Word/fidelidade editorial não foram homologados. Sem alterações no backend B05, no gate B05 experimental, em V1–V8 ou nos modelos institucionais.

Referência de código: `feat/b05-core-docs-api`, PR #17, `d9353a48a5f41da0c655fc85e7af680478533a16` (base empilhada PR #16). A campanha usa os endpoints reais de `DocumentController` e o objeto `Session.editor` já existente; **não utiliza servidor HTTP sintético como motor**.

## Escopo — uma rodada, três frentes

1. **Runtime/navegador:** API Java com autenticação Microsoft Entra real, ONLYOFFICE `documentserver:10.0.0.1`, editor `DocsAPI.DocEditor`, sinal `onDocumentReady`, edição humana exclusivamente sintética e callback assinado ao produto.
2. **Versão/quarentena/PDF:** diferencia `AVAILABLE`, `QUARANTINED` e ausência de salvamento; após encerramento observa o histórico e a revisão atual. Confere cabeçalho `X-Document-Version`, assinatura `%PDF-` e igualdade dos bytes obtidos em duas conversões da *mesma versão*. **Não confere equivalência visual Word.**
3. **Segurança/revogação:** leitura anônima proibida, capability adulterada proibida, callback sem assinatura proibido, fonte de uma nova sessão de leitura válida antes de revogar e inválida após revogar. A suíte do PR17 permanece a evidência de concorrência/idempotência sintética; replays exatos de callbacks reais e coedição entre dois atores são **NOT_RUN** aqui até haver observabilidade/identidades adequadas. O teste de symlink ignorado na suíte Windows **não é considerado aprovado**.

## Pré-condições sem atalhos

- **Apenas desenvolvimento descartável, sem documentos ou dados reais.** Não usar a base persistente existente, Neon, staging institucional ou modelos oficiais. O compose desta pasta tem projeto e DB separados, ports apenas em loopback, e **não remove volumes**.
- Java 21, Docker Desktop, Node.js >=20, Python 3 com `python-docx` disponível, identidade Entra legítima com autorização de administrador NITE. Sem identidade válida, **BLOCKED_AUTH**: não criar JWT falso nem desabilitar a segurança.
- Segredos de JWT do editor, HMAC das capabilities e DB com pelo menos 32 bytes quando aplicável, externos ao repositório. Segredo do ONLYOFFICE e `DOCUMENTS_EDITOR_JWT_SECRET` devem coincidir; segredos HMAC e JWT distintos. Não colar bearer/JWT/URL assinada em chat, logs ou relatório.
- O servidor Docs API pode ser HTTP **somente no loopback do host**, com a exceção de desenvolvimento `DOCUMENTS_EDITOR_ALLOW_INSECURE_LOOPBACK=true`. A **API pública usada em fontes e callbacks precisa ser HTTPS confiável e alcançável de dentro do contêiner**; `localhost` do contêiner não é o Windows. Não desabilitar validação de certificado, proteção SSRF, JWT, TLS nem allowlists de origem para fazer o ensaio passar.
- O editor deve permanecer desabilitado por padrão fora da sessão local (`DOCUMENTS_EDITOR_ENABLED=false`), os modelos oficiais continuam bloqueados em código e o diretório `DOCUMENTS_STORAGE_ROOT` precisa de ACLs privadas do Windows.

## 1. Topologia descartável (PowerShell na raiz do repositório PR #17)

O Docker só publica a base e o serviço ONLYOFFICE na interface loopback. Exemplo de comandos (variáveis devem ser providas por gerenciador de segredos do operador; **não** salve um `.env` com tokens ou credenciais dentro do repositório):

```powershell
$M2R = 'experiments/b05-m2r-real-runtime'
docker compose -p nite-m2r -f "$M2R/compose.onlyoffice-dev.yaml" up -d
# Inspecionar readiness e logs localmente, mas NÃO publicar logs do motor com URLs de callback.
docker compose -p nite-m2r -f "$M2R/compose.onlyoffice-dev.yaml" ps
```

`NITE_M2R_DB_PASSWORD` e `DOCUMENTS_EDITOR_JWT_SECRET` são exigidos pelo compose; sem eles o Docker não inicia. Não reutilize senhas/volumes da base existente. Executar o backend na branch PR17 sobre banco dedicado `jdbc:postgresql://127.0.0.1:15433/niteconnect_m2r`, usuário `niteconnect`, senha local exclusiva. Flyway executará V1–V8 **somente nessa base**. Não executar `flyway repair`, `outOfOrder` ou renumeração de migrações.

### Topologia das três conexões

| Cliente | Destino | Restrição |
|---|---|---|
| Navegador | `http://127.0.0.1:18080/web-apps/apps/api/documents/api.js` | Loopback de desenvolvimento, Docs API assinado |
| Java backend | `http://127.0.0.1:18080/converter` e download de callback | `DOCUMENTS_EDITOR_ALLOW_INSECURE_LOOPBACK=true` apenas durante o ensaio |
| ONLYOFFICE contêiner | `https://<host HTTPS alcançável do contêiner>/api/v1/document-provider/...` | URL de `DOCUMENTS_PUBLIC_BASE_URL`, certificado e cadeia confiáveis dentro do contêiner |

A API Java pode escutar HTTP `127.0.0.1:8080` **atrás de um proxy HTTPS de desenvolvimento**. O proxy/TLS não é provisionado por este patch: use a topologia confiável que já existe no ambiente ou prepare-a antes de avançar. Se o DNS/certificado não passar a partir do contêiner, marque `BLOCKED_NETWORK_TLS`. Não defina `DOCUMENTS_PUBLIC_BASE_URL=http://host.docker.internal:8080`: o adaptador atual o rejeita corretamente. Não configure uma URL de callback inacessível apenas para obter `onDocumentReady`.

Configuração de API (valores exemplificativos, sem segredos):

```powershell
$env:DB_URL='jdbc:postgresql://127.0.0.1:15433/niteconnect_m2r'
$env:DB_USERNAME='niteconnect'
$env:DB_PASSWORD=$env:NITE_M2R_DB_PASSWORD
$env:DOCUMENTS_LOCAL_DEVELOPMENT='true'
$env:DOCUMENTS_STORAGE_ROOT='C:\CAMINHO_PRIVADO\nite-m2r-documents'
$env:DOCUMENTS_EDITOR_ENABLED='true'
$env:DOCUMENTS_EDITOR_SERVER_URL='http://127.0.0.1:18080'
$env:DOCUMENTS_EDITOR_ALLOW_INSECURE_LOOPBACK='true'
$env:DOCUMENTS_PUBLIC_BASE_URL='https://HOST-CONFIAVEL-PARA-O-CONTÊINER'
# DOCUMENTS_EDITOR_JWT_SECRET e DOCUMENTS_CAPABILITY_SECRET: previamente injetados de origem segura.
# ENTRA_ENABLED e parâmetros Entra: configurar com credenciais institucionais válidas e autorizadas.
Set-Location apps/api
.\mvnw.cmd spring-boot:run
```

Validações de pré-conexão na própria máquina, com confiança TLS sem `-k` / `--insecure`:

```powershell
# A partir do Windows:
Invoke-WebRequest -Uri 'http://127.0.0.1:18080/healthcheck' -UseBasicParsing
# A partir do contêiner, testar a origem HTTPS configurada SEM imprimir tokens:
# docker compose -p nite-m2r -f "$M2R/compose.onlyoffice-dev.yaml" exec onlyoffice-m2r curl --fail --silent --show-error 'https://HOST-CONFIAVEL-PARA-O-CONTÊINER/actuator/health'
```

Saída 200 de `/actuator/health` não comprova o sucesso do fluxo de callback; a execução real abaixo é obrigatória. Em ambientes com CA privada, instalar a cadeia confiável de modo apropriado nos sistemas que consomem TLS. Não ignorar erros certificados.

## 2. Fixture e provisionamento de produto REAL (apenas DB descartável)

Com os diretórios de destino dentro de `.private` e novos:

```powershell
Set-Location RAIZ_DO_REPOSITORIO
python experiments/b05-m2r-real-runtime/scripts/make_fixture.py --out experiments/b05-m2r-real-runtime/.private/run-01/fixture.docx
node --test experiments/b05-m2r-real-runtime/tests/core.test.mjs
```

O gerador produz um documento de uma página com texto e tabela fictícios. Não utiliza AV3, RA, PII ou templates institucionais. É permitido observar a formatação no Word Desktop, mas **não comparar como referência de homologação**.

O operador deve obter um **access token Entra legítimo** por fluxo autorizado, injetado em `NITE_M2R_ACCESS_TOKEN` apenas no processo/sessão local. O token precisa representar um membro ativo com autorização administrativa NITE. Não há suporte neste pacote para criar contas falsas, contornar o provedor nem armazenar o token. **Não publicar o comando usado para injetá-lo.**

```powershell
node experiments/b05-m2r-real-runtime/scripts/provision.mjs `
  --confirm-disposable-dev YES `
  --api-base 'https://ORIGEM_DA_API_USADA_PELO_OPERADOR' `
  --fixture experiments/b05-m2r-real-runtime/.private/run-01/fixture.docx `
  --output experiments/b05-m2r-real-runtime/.private/run-01/provision-private.json
```

O CLI cria um **modelo não oficial**, uma **versão de modelo** e um **documento administrativo não oficial** através da API de produto. Exige banco descartável; não há exclusão automática ou rollback de dados confirmados. A saída privada contém os identificadores necessários para a rodada.

## 3. Uma sessão real de navegador + persistência + negativos

```powershell
$private = 'experiments/b05-m2r-real-runtime/.private/run-01'
$docId = (Get-Content -Raw "$private/provision-private.json" | ConvertFrom-Json).documentId
node experiments/b05-m2r-real-runtime/scripts/run.mjs `
  --confirm-local-dev YES `
  --api-base 'https://ORIGEM_DA_API_USADA_PELO_OPERADOR' `
  --document-id $docId `
  --output "$private/browser-01" `
  --wait-seconds 90
```

Se a API do operador só estiver em HTTP local, é permitida **somente** `--api-base http://127.0.0.1:<porta> --allow-http-loopback YES`. Isso não substitui a obrigatoriedade de `DOCUMENTS_PUBLIC_BASE_URL` HTTPS para o tráfego do motor. O executor é uma página local efêmera, **não é o futuro frontend B07** e não encaminha a identidade Entra para o navegador.

1. Abrir no navegador local a URL `http://127.0.0.1:5179/` indicada pelo CLI. Não colar URL de fontes assinadas no navegador/log.
2. Aguardar a indicação **Documento aberto**. Editar a frase fictícia `EDITE AQUI` uma única vez e concluir em até seis minutos, sem modificar estrutura ou tabela deliberadamente. O próprio motor ainda pode reserializar OOXML: não forçar aprovação caso caia em quarentena.
3. Clicar **Terminei a edição — fechar editor**. O SDK é destruído e o programa observa a nova versão por até 90 s; o ONLYOFFICE costuma enviar status 2 após o encerramento, não imediatamente ao digitar.
4. Inspecionar `browser-01/m2r-shareable.json` e o histórico privado do documento no ambiente descartável. **Não interpretar `{error:0}` como prova de fidelidade**. Se houver quarentena, preservar bytes/versões e explicar o motivo sem promover a candidata.
5. O executor verifica ainda fonte válida e posterior revogação numa **sessão de leitura independente**, callback sem token, capability adulterada e conversão PDF de uma versão disponível fixada pelo ID.

O relatório não inclui IDs, SHA privados, conteúdo DOCX, PDF bruto, URLs assinadas, bearer/JWT, headers secretos ou capturas. Se houver falha, compartilhe apenas `m2r-shareable.json` e uma descrição humana sanitizada; mantenha todos os arquivos e logs brutos no Windows.

## 4. Classificação e critérios de conclusão

| Resultado | Decisão de engenharia |
|---|---|
| `M2R_INTEGRATION_DEMONSTRATED_NOT_EDITORIAL_APPROVAL` + `VERSION_AVAILABLE_NOT_EDITORIAL_APPROVAL` | Runtime, armazenamento e callback passaram nos controles observados; ainda exige revisão OOXML/Word separada |
| `M2R_INTEGRATION_DEMONSTRATED_NOT_EDITORIAL_APPROVAL` + `QUARANTINED_GUARD_ENFORCED` | Motor integrado; proteção detectou mudança estrutural. **M2R operacional, edição publicável NÃO demonstrada**. Examinar delta privado antes de propor ajustes |
| `NO_PERSISTED_CALLBACK` ou `M2R_NOT_CONCLUDED` | Não há prova de persistência real; investigar rede, callback, TTL, save status e logs privados |
| `EDITOR_NOT_READY` ou `M2R_BLOCKED_OR_FAILED` | Bloqueado por ambiente/autorização/SDK. Não liberar M2 |
| PDF funciona mas cabeçalho diverge/bytes diferem na mesma versão | Reprovar vínculo ou imutabilidade da conversão; não declarar M2R concluído |

A conclusão `M2R_INTEGRATION_DEMONSTRATED_NOT_EDITORIAL_APPROVAL` cobre **somente os controles realizados neste ensaio**. Não prova coedição de dois usuários, exact-replay de callback vendor, comportamento de concorrência em produção, segurança física contra symlinks no Windows, desempenho, licença, export visual Word nem `SAVEDATE`/`PAGE`. A lista `pending` no JSON registra essas lacunas. Repetir a campanha com identidades distintas e observabilidade privada controlada apenas se autorizado.

**Se/quando M2R passar**, o próximo gate é M3 editorial com arquivos oficiais privados, Word Desktop e parâmetros institucionais confirmados. O veto oficial e G1/G2 permanecem ativos. Não fazer merge/deploy a partir do status M2R.

## 5. Regra de parada e limpeza

- Sem identidade real, sem TLS válido, sem callbacks autenticados, sem DB descartável ou sem editor real: **parar e registrar o bloqueio**; não trocar por HTTP mock.
- Encerrar apenas a API iniciada nesta rodada e os serviços do projeto Docker `nite-m2r`. **Não** executar `down -v` automaticamente; o volume isolado pode conter evidências que precisam de revisão. Não tocar no banco persistente B01–B06, no checkout `D:\nite-connect` nem nas branches de experimentos B05.
- `Remove-Item Env:NITE_M2R_ACCESS_TOKEN` depois da sessão; encerrar também outras variáveis sensíveis e processos do operador. Segredos nunca entram no ZIP compartilhável.

Referências oficiais: [Docs API callbacks](https://api.onlyoffice.com/docs/docs-api/usage-api/callback-handler), [DocsAPI DocEditor/events](https://api.onlyoffice.com/docs/docs-api/usage-api/doceditor), [Conversão /converter](https://api.onlyoffice.com/docs/docs-api/additional-api/conversion-api/request) e [JWT Docker](https://helpcenter.onlyoffice.com/docs/installation/docs-configure-jwt.aspx).

## Correções locais do executor antes da campanha Windows

A revisão independente identificou dois pontos corrigidos sem alterar o produto:
- resultado final exige `editorRuntimeReal=true` e ausência de erro, com atualização após as verificações assíncronas;
- servidor de página local valida `Host` exato e `Origin` dos sinais para limitar DNS rebinding.

Dois testes de regressão foram observados falhando antes das correções; a suíte passou com nove testes após os ajustes. O histórico de execução Windows e suas limitações ficam em `results/windows-2026-10-23.md`. Nenhuma dessas correções permite promover candidata em quarentena ou editar modelos oficiais.
