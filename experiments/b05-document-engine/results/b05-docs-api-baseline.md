# B05.0.3 — baseline isolada

Execução local iniciada em 17/10/2026 (America/Bahia; logs do motor em UTC). Base
`da96df43618114fe71e494cc9ccc8183d9d6797c`, PR #11 aberto, branch derivada
`spike/b05-docs-api-vs-wopi`. PRs #9/#10/#11 preservados; nenhum merge.

ONLYOFFICE Community `10.0.0.1`, índice de imagem
`sha256:ca17889f10b11141cc6441836aebdd2835cf3e1b49681b2fd6e155c94b7850b3`;
runtime identifica versão 10.0.0, build 169. Não houve troca de versão. O projeto
Compose `nite-b05-docs-api` usa cache, dados e credenciais novos. Gateway em
loopback 19982, host em loopback 18087, motor sem porta publicada. Limites do
motor: 4 GiB/2 CPUs. Disponibilidade do Docker local foi restabelecida iniciando
o Docker Desktop instalado. Nenhum Collabora/LibreOffice foi iniciado.

F01 é a cópia exata da fixture sintética histórica de texto/estilos/lista; F02 é
a cópia complexa já adaptada de B05.0.2. **F02 não foi editada no motor nesta
rodada**, porque B parou em F01. Em checkout limpo, o preparador gera F02 a partir
do caso source-free existente e registra essa proveniência distinta; não afirma
que essa geração substitui os bytes do controle local histórico.

`historical-freeze.json`, privado, congela 177 arquivos históricos, incluindo os
originais/checkpoints/controles e B05.0.2. Hashes, capabilities, keys e versões
ficam em `.runtime/b0503/`; não são anexados ao PR/CI. Os modelos oficiais não
foram transformados, abertos novamente ou enviados ao motor nesta rodada.

Configuração assinada, edição e readonly abriram por `DocEditor`. O host usa JWT
HS256, capacidade por sessão, validade de uma hora, usuário sintético e origem
fixa. Não representa autenticação institucional. Runtime confirmou JWT browser,
inbox e outbox habilitados; callbacks observados tinham token no corpo e claims
planas. A flag `outbox.inBody` lida era false: registrar o comportamento efetivo
deste build, sem generalizar a documentação. Nenhum segredo é versionado.

Referências oficiais consultadas: [API/WOPI](https://api.onlyoffice.com/docs/docs-api/using-wopi/api-vs-wopi),
[callback](https://api.onlyoffice.com/docs/docs-api/usage-api/callback-handler),
[salvamento](https://api.onlyoffice.com/docs/docs-api/get-started/how-it-works/saving-file)
e [JWT no corpo](https://api.onlyoffice.com/docs/docs-api/additional-api/signature/request/token-in-body).
São contratos publicados; abertura/callback locais são a evidência da edição
instalada. Não inferir direitos comerciais da disponibilidade técnica.

Execução e paths de evidência: [protocolo](../scripts/DOCS-API-B0503.md).
