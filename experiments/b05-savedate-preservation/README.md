# B05 — preservação experimental de SAVEDATE

**Compatibilidade local e correção nativa experimental, validada em versão fixada.**
O adaptador evita a perda da instrução durante a serialização DOCX.
Separadamente, `native_sdk_patch.mjs` corrige parser, serialização e cálculo
SAVEDATE/F9 no bundle exato usado pela bancada. Ver limites e evidências abaixo.
Não usar este resultado como autorização para produção ou exceção ao gate.

O problema foi localizado no parser do SDK: SAVEDATE não é reconhecido e cai
no caminho REF. Na leitura de `fldSimple`, esse tipo é usado para reconstruir
a instrução, perdendo o identificador original. O caminho de campo complexo
mantém o texto original da instrução. Fontes primárias consultadas:

- [Parser de campos](https://github.com/ONLYOFFICE/sdkjs/blob/master/word/Editor/Paragraph/ComplexFieldInstruction.js).
- [Leitura e escrita de campos simples](https://github.com/ONLYOFFICE/sdkjs/blob/master/word/Editor/Serialize2.js).
- [Reconstrução da instrução em ParaField](https://github.com/ONLYOFFICE/sdkjs/blob/master/word/Editor/Field.js).

## Uso explícito em cópias privadas

```powershell
python experiments/b05-savedate-preservation/make_fixture.py --out .private/savedate/source.docx
python experiments/b05-savedate-preservation/savedate.py --source .private/savedate/source.docx --out .private/savedate/compat.docx
python -m unittest discover -s experiments/b05-savedate-preservation/tests -v
```

Reutiliza `lxml`, já utilizado pela dependência `python-docx` da bancada.
O gerador também usa Pillow, já disponível. Nenhuma instalação ou alteração de
dependências do produto é realizada pelo comando. Não altera `.env`, credenciais,
fontes instaladas ou arquivos originais.

Somente `w:fldSimple` de SAVEDATE é transformado em sequência
`begin/instrText/separate/resultado/end`, preservando exatamente instrução,
runs do resultado, formatação, `fldLock` e `dirty`. Não força atualização de
campo, não insere data atual e não tenta adivinhar se um REF era SAVEDATE.
Campos complexos existentes e todas as outras partes ZIP ficam intactos.
Uma segunda aplicação retorna os mesmos bytes.

O comando exige origem e saída distintas sob `.private`, não sobrescreve arquivos
e recusa estruturas aninhadas, atributos desconhecidos, DTD, assinaturas,
macros, arquivos inseguros, relações externas e pacotes acima dos limites.
Não substitui o validador do backend nem aceita uma rejeição de integridade.

## Evidência e limite

### Integração local pelo provisionamento normal

```powershell
python experiments/b05-savedate-preservation/prepare_local.py --campaign .private/savedate-local/campaign-01
node experiments/b05-savedate-preservation/local_provision.mjs --api-base http://127.0.0.1:18082 --campaign .private/savedate-local/campaign-01 --case no_edit --confirm-local-compatibility YES --token-env NITE_M2R_ACCESS_TOKEN
```

Requer API da bancada isolada, armazenamento local, administrador autorizado por
Entra e bearer obtido pelo fluxo institucional e mantido em memória/ambiente.
Nunca passar token na linha de comando ou no chat. As outras operações são
`edit_text` e `edit_table`. Para os testes de transporte:

```powershell
node --test experiments/b05-savedate-preservation/tests/local_provision.test.mjs
```

O preparador gera somente fixture sintética nova, registra hashes privados,
origem/cópia compatível e três casos planejados. Não fabrica saídas do motor.
O provisionador aceita somente API em loopback, verifica integridade antes dos
requests e cria duas versões imutáveis do modelo: original e compatível.
O documento deriva explicitamente da segunda versão. O download do head inicial
é conferido contra os bytes compatíveis antes de registrar o recibo privado.

Nenhuma resposta de fonte é transformada silenciosamente e nenhum objeto já
armazenado é substituído. As permissões do editor continuam sendo emitidas pela
API; o provisionador não pede download adicional nem altera JWT. Há auditoria
normal de criação de versões. Essa rotina é opt-in e exclusiva da bancada;
nenhum caminho automático de produção chama o adaptador.

Para captura e reabertura de candidatas em quarentena, a rodada usa a inspeção
administrativa já autorizada e registra essa procedência separadamente. Isso
não comprova aceitação integral sob permissões normais de produção.

### Preservação isolada e limitação nativa

Fixture inteiramente sintética, três páginas, cabeçalhos principal/primeira/par,
rodapés, PAGE/NUMPAGES, logo e tabelas. Testes reais no ONLYOFFICE 10.0.0.1:
simples, complexo nativo, adaptado, reabertura e F9 explícito.

O campo simples virou REF. O complexo nativo e o adaptado preservaram SAVEDATE;
Word confirmou tipo 22 sem erro. A transformação isolada e o primeiro ciclo
adaptado produziram zero pixels diferentes nas três páginas a 144 dpi nas
comparações Word registradas. Isso não prova suporte universal a documentos.

O controle negativo F9 anterior apresentou “Error! Reference source not found.” no
editor, mesmo com a instrução preservada. Essa falha motivou a correção nativa
experimental descrita na seção seguinte. O adaptador permanece experimental;
nenhum caminho de produção chama este módulo. Nessa campanha isolada anterior,
não houve modelos oficiais, Entra, permissões de produto ou liberação de quarentena.
A integração local posterior, descrita acima, validou Entra e API reais; manteve
quarentena e permissões normais e usou somente fixtures sintéticas.

Preservação isolada: `results/windows-2026-10-24.md`.
Integração local: `results/windows-local-integration-2026-10-24.md`.

### Suporte nativo experimental a SAVEDATE/F9

```powershell
# Origem: cópia privada do sdkjs/word/sdk-all.js da imagem exata da bancada.
node experiments/b05-savedate-preservation/native_sdk_patch.mjs .private/native/sdk-all-original.js .private/native/sdk-all-patched.js
$env:B05_SDK_BUNDLE='.private/native/sdk-all-original.js'
node --test experiments/b05-savedate-preservation/tests/native_sdk.test.mjs
```

Não altera serviço automaticamente. Aplicação somente em contêiner novo e isolado
da bancada, preservando o bundle original e o aviso de licença do fornecedor.
A cópia modificada deve ser instalada antes de abrir a sessão do editor; nenhum
script de produção chama este módulo. Recusa versões divergentes e sobrescrita.

Oito testes direcionados e controles reais de F9 no corpo/cabeçalho, campo simples
sem adaptador, campo complexo e reabertura. Usa metadado de último salvamento,
mantém instruções e delega a picture ao formatador do SDK. Defaults sem picture
são limitados; não representa suporte universal a locales/switches Word nem build
oficial de um fork. Não substitui gate ou quarentena.

Resultados e limitações: `results/windows-native-f9-2026-10-24.md`.

### Build NITE a partir das fontes fixadas

`native/savedate.js` e `prepare_source.py` implementam parser, cálculo e
serialização nos símbolos originais do SDK. `source.lock.json` fixa a revisão
upstream, os arquivos de entrada e a imagem base. O builder oficial dessa
revisão concatena as fontes; os dois bundles Word são reconstruídos juntos.
Trata-se de imagem própria de qualificação local, sem publicação pelo fornecedor.

```powershell
# Checkout privado da revisão registrada em source.lock.json.
python experiments/b05-savedate-preservation/build_source_image.py --sdk-source .private/sdkjs --out .private/build-01 --image nite/onlyoffice-savedate:local-01 --no-cache
python experiments/b05-savedate-preservation/prepare_runtime_campaign.py --campaign .private/native-campaign-01
$env:B05_SDK_SOURCE='.private/sdkjs'
node --test experiments/b05-savedate-preservation/tests/source_native.test.mjs
```

O preparador exige checkout limpo e revisão exata. O build arquiva a revisão
original e aplica a alteração no contexto privado, verificando seu hash antes
da extração. Cada saída deve ser nova; nenhum comando inicia o serviço ou
provisiona documentos automaticamente. A execução integrada exige bancada local
autorizada e token Entra legítimo mantido em memória.

O Dockerfile remove o snapshot Word herdado `sdk-all.bin` e seu cache: o
renderizador nativo pode preferir esse snapshot aos bundles novos. Sem essa
remoção, F9 pode funcionar na tela e o callback ainda perder SAVEDATE simples.
A inicialização gera caches e reinicia serviços depois do primeiro healthcheck;
aguarde o término dessa etapa antes de abrir sessões de controle.

Cobertura delimitada: `\@`, `MERGEFORMAT`, `CHARFORMAT` e conversões de caixa.
Idioma efetivo do run, com fallback ao documento; locale desconhecido,
metadado ausente e switches não suportados recusam a atualização e mantêm o
cache. Isso não certifica todas as pictures, idiomas ou calendários do Word.
O cálculo utiliza o metadado de último salvamento, sem substituir pela data atual.

Controles integrados e divergências: `results/windows-source-build-2026-10-25.md`.
