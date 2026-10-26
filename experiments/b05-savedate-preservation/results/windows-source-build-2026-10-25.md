# B05 — build de fontes e controles integrados SAVEDATE/F9

## Resultado

O suporte nativo experimental foi portado para fontes fixadas do SDK e incorporado
a uma imagem NITE reproduzível. F9 calculou SAVEDATE simples e complexo na UI real.
Depois da correção do snapshot do renderizador, callback e reabertura preservaram
os 11 SAVEDATE da fixture, sem conversão para REF. A qualificação de equivalência
completa com o Word permanece **HOLD** pelas diferenças descritas abaixo.

Execução Windows em 24–25/10/2026, com retomada em 25/10 após encerramento dos
processos locais. Word 16.0.17932; Brave; somente documentos sintéticos. Trabalho
em `feat/b05-core-docs-api`, HEAD `017281a1830724cd66eee6ee505a704e9fa37b4d`.
O diff rastreado anterior foi comparado com o checkpoint privado e permaneceu
idêntico. O checkout de auditoria em `D:\nite-connect` foi preservado.

## Implementação e build

- `native/savedate.js`: tipo próprio SAVEDATE, parser, cálculo pelo último
  salvamento, resolução de idioma e tratamento delimitado de switches.
- `prepare_source.py`: alterações nos pontos originais de parsing, atualização e
  serialização de campos simples/complexos; valida revisão, arquivos e anchors.
- `source.lock.json`: SDK `v10.0.0.169`, revisão
  `3679e0375a758d7dc59d31337a45461be3525ed8`, imagem base e hashes de código público.
- `Dockerfile.native` e `build_source_image.py`: archive original verificado,
  builder Python oficial dessa revisão, ambos os bundles Word, gzip sem timestamp
  e metadados de arquivos/diretório fixados. Não usam o patch de símbolos minificados.
- `prepare_runtime_campaign.py`: matriz nativa adicional, com campos simples
  entrando no motor sem o adaptador prévio.

Imagem final local: `nite/onlyoffice-savedate:10.0.0-169-nite.3-final`.
Duas reconstruções independentes com `--no-cache` produziram o mesmo image ID:
`sha256:ee5e230f0c9d69463f047b1d87206fb6231f97c9c5b430321286dd394881ede1`.
Isso comprova reprodução da imagem nesta bancada. É build próprio NITE sobre
base oficial fixada; não é lançamento oficial nem aceite pelo fornecedor.

### Defeito adicional localizado no fluxo real

A primeira imagem reconstruía os JavaScripts, mas herdava `word/sdk-all.bin`.
F9 funcionava no browser; o callback ainda convertia os dois SAVEDATE simples
em `REF \* MERGEFORMAT`. O defeito foi constatado nos bytes adquiridos antes
de abrir o arquivo no Word, com caches aparentemente corretos.

O renderizador nativo pode carregar esse snapshot antes de executar as fontes.
O caminho está documentado no código primário
[CreateEditorContext/RunEditorWithSnapshot](https://github.com/ONLYOFFICE/core/blob/master/DesktopEditor/doctrenderer/editors.cpp).
O Dockerfile final remove exclusivamente o snapshot Word herdado, seu gzip e o
cache derivado dentro da nova imagem. A base e os artefatos anteriores permanecem.
Sem o snapshot, o renderizador carrega os bundles novos. O controle real posterior
passou de 9 SAVEDATE + 2 REF para 11 SAVEDATE + 0 REF, inclusive após reabrir.

A inicialização gera caches e reinicia serviços depois do primeiro healthcheck.
Uma tentativa foi interrompida por esse reinício e ficou inconclusiva. Os dois
controles editados finais foram refeitos depois da estabilização do serviço.
Não se atribuiu essa perda de conexão ao campo e não se contou a tentativa como
aceitação. Logs do Supervisor registraram o SIGTERM/restart de inicialização.

## Cobertura efetivamente exercitada

A fixture contém três páginas, três tabelas, duas quebras explícitas, logotipo,
cabeçalhos primeira/par/principal e rodapés. São 17 campos: 11 SAVEDATE, três PAGE
e três NUMPAGES. Os três SAVEDATE dos cabeçalhos continuam complexos pelo adaptador
local anterior; o corpo acrescenta dois SAVEDATE simples e seis complexos nativos.

| Controle do corpo | Resultado observado na UI e no OOXML anterior ao Word |
| --- | --- |
| Simples pt-BR, sem picture | `02/01/2020 00:04:05` |
| Simples en-US, sem picture | `1/2/2020 12:04:05 AM` |
| Complexo de-DE, sem picture | `02.01.2020 00:04:05` |
| Complexo fr-FR, sem picture | `02/01/2020 00:04:05` |
| `\@ "MMMM" \* Upper` pt-BR | `JANEIRO` |
| Picture ISO + MERGEFORMAT | Data, azul/itálico preservados em todo o resultado |
| Picture ISO + CHARFORMAT | Data, vermelho/negrito do primeiro caractere da instrução |
| Switch não implementado `\h` | Instrução e `CACHE_NATIVE_UNSUPPORTED` preservados |

F9 também foi executado explicitamente no cabeçalho da primeira página do
controle de tabela: `02/01/2020 03:04:05` passou a `02/01/2020 00:04:05`, conforme
o instante sintético e fuso da bancada. As outras histórias foram inventariadas;
não se afirma F9 manual em cada um dos seis cabeçalhos/rodapés nesta rodada.

O parser contempla `\@`, MERGEFORMAT, CHARFORMAT, Upper, Lower, Caps e FirstCap.
O cálculo delega pictures ao formatador existente. Testes de despacho usam stubs
de documento/formatador; não substituem a matriz real acima. Não há prova universal
de todas as pictures, combinações, idiomas, calendários ou formatações mistas.
LCID desconhecido, metadado ausente e switches não suportados recusam a atualização
sem apagar o cache. A data atual não é usada como fallback.

### Diferenças semânticas em relação ao Word

1. **Idiomas em documento misto:** a implementação seleciona idioma efetivo do run,
   depois idioma padrão do documento. O Word usado aqui escolheu formato padrão
   pt-BR para os quatro campos sem picture do documento misto, mesmo quando código
   e resultado complexos tinham de-DE/fr-FR. Em quatro controles adicionais com
   todos os `w:lang` definidos uniformemente, Word produziu os padrões pt-BR,
   en-US, de-DE e fr-FR correspondentes. Não há equivalência demonstrada para a
   política de seleção do idioma no documento misto.
2. **Precisão temporal:** o core contém segundos `05`; o cálculo NITE os conserva.
   Word apresentou `00` para os segundos da mesma data, antes da exportação PDF.
   Essa diferença não é perda de instrução. Não foi criada normalização que a oculte.
3. **MERGEFORMAT ao aumentar o resultado:** o cache inicial sintético é menor que
   a data calculada. Na referência Word, os três caracteres finais `:00` recebem
   Arial-BoldMT/vermelho, herdados do código; na candidata NITE, toda a data fica
   Arial-ItalicMT/azul. A implementação preserva propriedades uniformes do cache,
   mas não reproduz a distribuição por caractere do Word nessa extensão de texto.
   É diferença funcional de formatação observada, sem exceção ao gate.

## Integração real e procedência

Login Entra legítimo, assinatura/issuer/audience/scope validados, API local com
autorização operacional retornando 200. Token mantido apenas no processo; não
salvo em disco ou incorporado à imagem. Nenhum túnel externo nesta execução.

Cada documento deriva da segunda versão imutável do modelo, a cópia compatível.
Bytes de original, cópia, vínculo e head foram conferidos também no PostgreSQL.
Permissão normal `download:false`, gate e quarentena permaneceram ativos.

| Caso final | Operação | Callback normal | Resultado |
| --- | --- | --- | --- |
| Sem edição | Abrir e fechar, sem F9 | 1 e 4 | Sem candidata de edição; snapshot administrativo separado |
| Texto | `MARCADOR_EDITAVEL_LOCAL_1` → `MARCADOR_EDITADO_LOCAL_1`, F9 do corpo | 1 e 2 | Candidata QUARANTINED; head original mantido |
| Tabela | Uma célula editada; F9 do corpo e do primeiro cabeçalho | 1 e 2 | Candidata QUARANTINED; head original mantido |

Para os casos editados, `engine.docx` foi obtido da localização de callback após
o ACK do backend; `callback.docx` veio da versão persistida. Não são exportações
independentes anteriores ao callback. A reabertura/exportação usa a inspeção
administrativa somente leitura anteriormente autorizada, registrada separadamente.
No caso sem edição, a amostra administrativa importada também permanece em
quarentena. Nada foi promovido ou restaurado como versão corrente.

Os 11 SAVEDATE mantiveram instruções exatas e representação simples/complexa
na exportação e reabertura. Corpo inteiro, inclusive células, corresponde às
edições declaradas e às sete substituições de cache esperadas pelo F9; a oitava
permanece recusada. Três tabelas, duas quebras e bytes de imagem preservados.

Outras alterações permanecem explícitas: PAGE e NUMPAGES simples ganharam
`\* MERGEFORMAT`; caches PAGE do callback não atualizado no rodapé permanecem
`1`, enquanto a exportação após reabrir recalcula primeira/par/principal para
`1/2/3`. Isso não autoriza afirmar identidade dos caches do pacote. Word reconheceu
SAVEDATE como tipo 22 nos arquivos finais. A relação órfã
`word/_rels/comments.xml.rels` continua aparecendo nas exportações; não foi corrigida
ou dispensada por este trabalho.

Controles negativos reais, nos três documentos finais: fonte autorizada 200,
capability adulterada 403, acesso anônimo à inspeção 401, fonte revogada 403,
restauração de quarentena 409, head mantido e leitura de inspeção auditada.
Status 2 foi confirmado em `document_callback_receipts`; a ação do histórico
nesses casos é `EDITOR_VERSION_QUARANTINED`, não `EDITOR_STATUS_2`.

## PDFs e diferenças localizadas

Doze PDFs finais em seis pares: Word read-only, macros desabilitadas, mesmas
condições de exportação `SaveAs2`, atualização explícita de campos e conferência
dos hashes de origem antes/depois. Raster RGB integral a 144 dpi, sem máscaras.
Todas as páginas mantêm 1224 × 1584 pixels; todos os PDFs têm três páginas.

| Caso/par | Pixels diferentes páginas 1 / 2 / 3 | Explicação |
| --- | --- | --- |
| Sem edição: origem × motor | 0 / 0 / 0 | Mesma renderização pareada pelo Word |
| Sem edição: origem × reabertura | 0 / 0 / 0 | Mesma renderização pareada pelo Word |
| Texto: origem × motor | 6344 / 0 / 0 | 5942 da edição prevista, 347 de MERGEFORMAT e 55 no campo alemão |
| Texto: callback × reabertura | 1022 / 128 / 128 | Minuto da exportação Word `12:31` → `12:32` nos campos de data |
| Tabela: origem × motor | 2362 / 0 / 0 | 1910 da célula prevista, 347 de MERGEFORMAT e 105 no campo alemão |
| Tabela: callback × reabertura | 1197 / 149 / 149 | Minuto da exportação Word `12:34` → `12:35` nos campos de data |

MERGEFORMAT foi localizado na página 1, x=541–570/y=511–527; a diferença de fonte
e cor está descrita acima. No campo alemão, as faixas ficam em y=364–379, com
mesmos texto, ArialMT e tamanho. A extração por glifo mostrou deslocamento de
aproximadamente 0,0062 pt em parte da sequência; a rasterização amplifica esse
deslocamento para 55/105 pixels, conforme os algarismos do minuto. A origem exata
dessa diferença de posicionamento não foi demonstrada; permanece inconclusiva.
As imagens completas e as faixas de diferença ficaram preservadas para revisão.

SaveAs2 atualiza SAVEDATE novamente, inclusive na referência. Logo, PDFs corretos
não provam correção do F9 NITE nem equivalência do cache anterior. A prova de F9
vem da UI e do OOXML adquirido antes do Word. Nenhum resultado não idêntico foi
substituído por PASS e nenhuma região de data foi excluída.

## Validação e limites da entrega

Com as fontes finais, foram executados os comandos abaixo; a sequência e a
validade após o último ajuste de imagem estão discriminadas em seguida:

```powershell
python -m unittest discover -s experiments/b05-savedate-preservation/tests -v
$env:B05_SDK_SOURCE='.private/savedate-source-build-20261024-01/sdkjs'
node --test experiments/b05-savedate-preservation/tests/source_native.test.mjs experiments/b05-savedate-preservation/tests/local_provision.test.mjs
$env:B05_SDK_BUNDLE='.private/savedate-fix-20261024-01/sdk-all-installed.js'
node --test experiments/b05-savedate-preservation/tests/native_sdk.test.mjs
node --check experiments/b05-savedate-preservation/native/savedate.js
python -m py_compile experiments/b05-savedate-preservation/prepare_source.py experiments/b05-savedate-preservation/build_source_image.py experiments/b05-savedate-preservation/prepare_runtime_campaign.py
git diff --check
```

19 testes Python, seis Node das fontes, cinco Node de provisionamento e oito Node
do protótipo anterior aprovados. Os 19 Python e 11 Node de fontes/provisionamento
foram executados antes da remoção final do snapshot herdado; não foram repetidos,
pois esse ajuste somente mudou o empacotamento Docker, sem alterar suas fontes,
contratos ou dependências. Os oito testes do protótipo e a verificação de sintaxe
Python/Node foram executados depois. Casos negativos
incluem adulteração da origem/archive, saída pública, sobrescrita, outros tipos de
campo, metadado ausente, LCID desconhecido e switch recusado antes de remover cache.
Os ciclos integrados finais foram executados após a correção do Dockerfile.
As mudanças posteriores são documentação e análise das evidências.

A suíte histórica `experiments/b05-sdk-comparison` não existe neste worktree de
implementação; a tentativa nesse caminho falhou por ausência do diretório/módulo,
sem executar testes. Os mesmos comandos foram então executados em
`D:\nite-connect`: **92 testes Python e cinco controles focais aprovados**.
Incluem negativos de fonte/espaçamento, perda de campos, partes e relações.
Esse resultado do checkout de auditoria não substitui os testes do SDK modificado.

Java/Flyway/API não foram recompilados nesta rodada: nenhuma alteração nessas
camadas. A validação anterior de 277 testes aprovados e um ignorado permanece
como evidência histórica do JAR, não como nova execução. Houve controles HTTP,
autorização e persistência reais com esse JAR nesta rodada.

Nenhum commit, push, publicação de imagem, PR, alteração de produção, integração
de outros motores ou exceção global ao gate. A cobertura delimitada passou;
equivalência universal de idiomas/switches, formatação por caractere MERGEFORMAT,
posicionamento residual e integridade OPC completa permanecem sem aprovação.

## Evidências privadas

Em `.private/savedate-source-build-20261024-01`: código upstream e diff privado,
sete contextos/resultados de build, comparação de reprodução, fixtures e hashes,
inventários dos campos e formatação, capturas UI, ciclos/callbacks/reaberturas,
logs, provas de autorização/quarentena, dump da bancada e controles de idioma Word.

Resultados finais: `campaign-resume-03/cases/no_edit` e
`campaign-resume-04/cases/{edit_text,edit_table}`; comparação integral em
`analysis-final-02`. As campanhas anteriores, inclusive as tentativas inválidas,
permanecem separadas. `final-inventory-private.json`,
`final-functional-checks-private.json` e `database-controls-final-private.json`
registram as verificações sem publicar bytes, identificadores ou hashes privados.

Encerramento confirmado: contêineres próprios parados, volumes preservados,
portas 5179/5181/15433/18080/18082/18083 fechadas, processo que detinha o access
token encerrado e bootstrap temporário de autenticação descartado. Nenhum túnel
externo foi aberto. Manifesto final privado registra 128 arquivos de evidência.


## Reteste oficial de idioma no F9 — build `.7` (25/10/2026)

A tentativa intermediária na build `.6` reproduziu um defeito: o documento guardava
`w:lang=de-DE`, mas o cálculo recebia o run separador do campo e escrevia
`outubro/OUTUBRO`. A correção seleciona o run da instrução (`SAVEDATE`) antes do
run separador para resolver o idioma e para obter a formatação de `CHARFORMAT`.
O gate e a produção não foram alterados.

O reteste foi executado no Brave com a build privada
`nite/onlyoffice-savedate:10.0.0-169-nite.7`, sobre cópias Word-normalizadas dos
dois modelos oficiais. Os probes SAVEDATE foram inseridos apenas nessas cópias.
Em cada documento, a operação no editor foi selecionar o corpo, pressionar F9,
salvar e coletar o DOCX pelo callback local.

| Documento | Saídas F9 conferidas no DOCX | Estrutura conferida |
| --- | --- | --- |
| Plano AV3 | `25.10.2026`; `25 Oktober 2026`; `2026-10-25`; `OKTOBER` | 4 campos SAVEDATE no corpo; 1 no cabeçalho manteve o cache anterior, pois o F9 foi aplicado ao corpo |
| Relatório semanal | `25.10.2026`; `25 Oktober 2026`; `2026-10-25`; `OKTOBER` | 1 tabela, 1 imagem embutida e 4 elementos hyperlink mantidos; 4 campos SAVEDATE no corpo |

Todos os valores de campo foram exportados com `w:lang=de-DE`; instruções de campo
permaneceram `SAVEDATE`, incluindo `\@`, `MERGEFORMAT`, `CHARFORMAT` e `Upper`.
O AX do Brave também exibiu `25 Oktober 2026` e `OKTOBER` após F9. A imagem `.7`
foi construída de fontes fixadas, usando o archive upstream verificado.

**Resultado delimitado:** o cálculo F9 no corpo, com esses quatro casos de formatação
e o idioma explícito alemão, foi confirmado nos dois documentos oficiais privados.
Isso corrige a divergência de idioma reproduzida em `.6`; não prova suporte universal.
F9 direto no cabeçalho ainda não foi repetido na `.7`. Esta rodada não comparou PDFs,
geometria/paginação, destinos/ativação de hyperlinks, nem toda a matriz de idiomas,
calendários, switches e formatações mistas. A diferença de paginação anteriormente
observada no Plano (Word 5 páginas contra cache ONLYOFFICE 4 páginas) permanece
aberta.

Testes após a alteração: 19 testes Python da pasta `experiments/b05-savedate-preservation/tests`,
10 testes `source_native.test.mjs`, `node --check` do módulo nativo, `py_compile` dos
scripts de preparação/build e `git diff --check`: aprovados. As evidências de DOCX e
hashes ficam em `.private/savedate-official-20261025-01/reports/` e não são publicadas.

## Retomada assistida no Brave — corpo e cabeçalho (25/10/2026)

Na retomada acompanhada, a fixture `Plano AV3` carregou no Brave a partir do controle local; a captura mostrou o cabeçalho sintético com SAVEDATE `25.10.2026 13:21:00`. A seleção do corpo, F9 e Ctrl+S resultaram em callback local ONLYOFFICE status 2 com DOCX exportado. A análise OOXML privada confirma que os quatro campos do corpo mantiveram suas instruções e `w:lang=de-DE`, com valores `25.10.2026 13:53:00`, `25 Oktober 2026 13:53:00`, `2026-10-25 13:53:00` e `OKTOBER`. O campo PAGE do cabeçalho e os elementos do cabeçalho permaneceram presentes; o SAVEDATE do cabeçalho continuou em `13:21:00`, pois F9 foi aplicado ao corpo. Isso distingue a atualização explícita do corpo do cache independente do cabeçalho.

A execução anterior isolada de F9 no cabeçalho da mesma build `.7` já havia atualizado o SAVEDATE do cabeçalho de `13:11:00` para `13:21:00`, preservando a instrução. Portanto, há duas verificações de escopo separado: F9 no corpo nesta retomada e F9 direto no cabeçalho em execução anterior. Não se deve interpretar a última candidata como uma atualização simultânea dos dois contextos.

A callback local recebeu primeiro status 1 e depois status 2, com URL de download e arquivo DOCX; nenhum serviço externo foi usado. O gate e produção permaneceram inalterados. O PDF comparativo anterior continua mostrando diferenças raster substanciais sem máscaras (média aproximada de 15,4% dos pixels no Plano e 10,4% no Relatório); essa comparação usou a candidata anterior, portanto não certifica a aparência da nova candidata combinada. Fidelidade visual e cobertura universal continuam pendentes.

Evidência DOCX, eventos do callback e hash da nova candidata permanecem privados em `.private/savedate-official-20261025-01/reports/`. Nenhum hash privado foi incluído aqui.

## Correção do layout da bancada local (25/10/2026)

A captura do Brave mostrou a superfície do editor limitada a uma faixa horizontal. A página servida pelo host local calculava a altura do DocsAPI como `calc(100% - 40px)` enquanto o elemento pai já tinha `height:100%`; não havia um contêiner flex reservando os 40 px do cabeçalho e alocando o espaço restante ao iframe. O host agora usa coluna flex, `min-height:0` no contêiner do editor e altura `100%` para o DocsAPI/iframe. Alteração restrita à página de controle privada; não altera motor, gate ou produção.

Verificações: novo teste de regressão `official_local_host.test.mjs` passou (1/1), `node --check` do host passou, `git diff --check` dos arquivos tocados passou, e a página corrigida em `127.0.0.1:5197` retornou HTTP 200 com a estrutura flex/altura esperada; `/health` respondeu `ok=true`. A reconexão de automação ao Brave foi interrompida por não conseguir confirmar a URL ativa, então **a correção visual ainda não foi reobservada no navegador**. Use a URL local acima para abrir a mesma fixture; só após conferir uma captura em tela cheia a bancada poderá ser considerada visualmente pronta.

## Estado atualizado — frente SAVEDATE/REF/F9 no escopo NITE (25/10/2026)

A cobertura exigida para esta frente foi delimitada aos documentos do NITE em PT-BR; suporte a todos os idiomas, calendários e switches não é pré-requisito para essa aceitação. A exigência de matriz universal permanece somente como limite de qualquer alegação de suporte universal.

A inspeção privada dos 16 DOCX presentes na campanha oficial M2P8 encontrou `pt-BR` como idioma padrão (`w:docDefaults/w:rPrDefault/w:rPr/w:lang`) em todos. O campo SAVEDATE original observado no cabeçalho usa `SAVEDATE \* MERGEFORMAT` e não tem `w:lang` direto no run; portanto, o padrão do documento é o idioma relevante neste caso. Não se encontrou, nessa instrução original, picture `\@`, switch de calendário ou transformação `Upper/Lower/Caps`. A cobertura para tais recursos não será tratada como bloqueio do caso NITE se não forem encontrados em outros modelos incluídos no escopo aprovado.

### Já demonstrado

- O build local experimental com correção nativa mantém a instrução como `SAVEDATE` em ciclos sintéticos e executa atualização F9; a matriz integrada anterior cobre casos PT-BR e alguns casos de controle em outros idiomas/formatações.
- A build `.7` foi exercitada no Brave com probes inseridos em cópias privadas dos dois documentos oficiais; corpo e cabeçalho foram verificados em execuções separadas. Esses probes usaram `de-DE`, não a instrução original PT-BR.
- O motor/runtime da campanha oficial M2P8, `onlyoffice/documentserver:10.0.0.1`, converteu o SAVEDATE original do cabeçalho para `REF`; o Word exibiu erro de indicador. A falha é real para aquele runtime, e a execução usou o caminho de inspeção com permissões especiais.

### O que falta para fechar SAVEDATE/REF/F9 no NITE

1. Executar a bancada de avaliação com a imagem NITE construída com a correção, em um ambiente isolado. Não se trata de publicar em produção.
2. Usar uma cópia imutável do modelo NITE com o campo original `SAVEDATE \* MERGEFORMAT`, idioma padrão PT-BR e sem probes de outros idiomas. Executar no Brave F9 no contexto em que o campo está (cabeçalho), salvar, obter a candidata pelo callback, reabrir a versão persistida e atualizar/verificar o campo no Word.
3. Confirmar em cada etapa que a instrução continua `SAVEDATE` (tipo Word 22), que não foi convertida em `REF` (tipo 3), que o valor apresentado corresponde à data/hora de salvamento no formato PT-BR e que o campo não mostra erro. O restante do texto e os campos PAGE/NUMPAGES não devem ser alterados pelo tratamento do SAVEDATE.
4. Repetir a prova no fluxo de persistência normal que será avaliado. A execução M2P8 de inspeção read-only, embora útil para diagnóstico, registrou `normal_permissions=false` e não fecha essa validação de integração.
5. Preservar controles negativos já cobertos pelos testes do código: `REF`, `DATE`, `PAGE`, `NUMPAGES`, switch não suportado e idioma/metadado desconhecido não podem ser convertidos silenciosamente nem perder o cache. Reexecutar os testes direcionados após qualquer alteração na implementação/runtime.

Não é necessário testar idiomas mundiais, calendários não utilizados, nem todos os switches do Word para concluir este escopo PT-BR. Se outro modelo oficial trouxer idioma ou instrução SAVEDATE diferente, ele será avaliado como uma extensão explícita do corpus, sem ampliar silenciosamente o suporte declarado. Fidelidade visual integral, relações OPC e hyperlinks continuam nas frentes próprias e não são consideradas resolvidas por esta prova de campo.
