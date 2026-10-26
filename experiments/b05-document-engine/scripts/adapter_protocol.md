# Protocolo B05.0.2

Usar ambiente local autorizado e runtimes do workspace. Todos os modelos,
manifestos, variantes, scripts de edição privados, PDFs e capturas ficam em
`.runtime/b0502/`, ignorado. Não usar documentos oficiais na CI.

## Preparação e transformação

`docx_adapter.normalize_copy(bytes, policy)` retorna `(working_bytes, manifest)`.
`policy` exige `original_sha256`, `template_version` e lista ordenada `fields`
com `part` e `instruction` **exatos**. Hashes privados nunca entram no Git.
`eligible` faz as mesmas verificações sem escrever. `verify_idempotence` verifica
o hash de trabalho e uma segunda normalização sem campos simples restantes.
O módulo não grava arquivos nem sobrescreve a origem; o chamador usa destinos novos.

O escopo aceito é campo SAVEDATE diretamente em parágrafo, cache de runs de texto
com rPr inicial opcional, flags conhecidas e não bloqueadas. Campos aninhados,
MCE/proteção e estruturas desconhecidas são recusados. REF/outros campos e
SAVEDATE já complexo não são convertidos. Pacotes sem SAVEDATE saem byte-idênticos.
Preserva-se a instrução integral; não há substituição de data ou switches.
As partes não afetadas preservam seus bytes; a parte tocada é reserializada com
DOM que conserva declarações de namespaces. O manifesto inclui diff canônico.

Fixture fonte-livre: `adapter_cases.savedate_source()` inclui core properties
com data de salvamento conhecida; o adaptador não escreve essa data. A fixture
antiga sem core é um controle separado, sem semântica last-save completa.

Executar testes Python antes de Word. Criar jobs privados para
`word_layout_probe.ps1`: comparar entrada/variante, atualizar SAVEDATE local,
repaginar, exportar e inspecionar páginas. Não salvar os originais no Word.

## Política no host experimental

Somente no overlay privado definir `LAB_SAVE_SCRIPT=/policy/save-script.properties`
e montar esse arquivo somente leitura. Exemplo sintético:

```properties
F01.docx=XYZ|
```

Cada item separado por `|` é o prefixo **completo esperado** para o primeiro
parágrafo do corpo original naquela versão. Acima: inserir XYZ, depois removê-lo.
Outros parágrafos devem permanecer exatos. Esta versão não autoriza preenchimento
arbitrário nem implementa o roteiro C2 de append. Documento/etapa sem script é negado.
O baseline vem das fixtures de entrada, inclusive após restart, nunca da saída.
O prefixo é aplicado à primeira ocorrência de texto de um run diretamente no
primeiro parágrafo, preservando a associação entre texto, run, propriedades e
cache. Alvo dentro de campo/contêiner não suportado é recusado. Não redistribuir
texto entre runs para satisfazer uma comparação somente por parágrafo.

`ScriptedDocxGate` verifica corpo e snapshot conservador de partes XML/mídia.
Ignora prefixos de namespace e revisão rsid. Compara todo texto associado à
estrutura, incluindo caches/whitespace e segmentos TEXT/CDATA coalescidos, além
da comparação por parágrafo; somente o prefixo esperado no alvo conhecido é
autorizado. Metadados core/app são dinâmicos e não certificam layout.
Nenhum outro delta do motor foi liberado. Não resolve cascata de estilos, equivalência
de âncoras ou cálculo de campos; alterações desconhecidas são recusadas como não
verificadas. Não chamar essa política de validador institucional completo.

Autorização, locks e CAS são revalidados antes da política. Recusa semântica retorna
HTTP 500, sem `X-WOPI-ItemVersion`, e não altera `current`/versões confirmadas.
A candidata fica em `<data>/<document>/rejected/`, inacessível pelos downloads de
versões: até 64 entradas (32 pares DOCX+metadata) por documento, sem exclusão automática.
Ao atingir o limite/falhar disco, responder 503 e manter a versão anterior.
Pacotes inválidos continuam recusados pelo validador de entrada; não são publicados.

HTTP 500 usa o código de erro genérico do
[PutFile Microsoft](https://learn.microsoft.com/en-us/microsoft-365/cloud-storage-partner-program/rest/files/putfile).
Conflito usa 409 e o lock header; os headers específicos do cliente estão na
[documentação ONLYOFFICE](https://api.onlyoffice.com/docs/docs-api/using-wopi/wopi-rest-api/putfile/).
O JSON com motivo é do laboratório; não existe contrato WOPI universal de UX para ele.

## Navegador e ponto de parada

Com ONLYOFFICE fixado e host saudável no dataset novo:

```powershell
node scripts/adapter_save_probe.cjs --run <diretorio-privado-novo> --playwright <pacote-bundled> --chrome <executavel-bundled>
```

Executar a partir da raiz do laboratório, ajustando o caminho do script se necessário.
O diretório precisa de `scenarios.json` no formato dos runners existentes. O probe
insere XYZ, solicita save, observa versão/indicador, fecha e reabre; guarda JSON e
screenshots privados. Não interpreta o indicador do editor como prova de persistência.
Uma repetição exige identidade WOPI isolada: novo volume com o mesmo ID pode restaurar
backup pendente do motor. O probe recusa esse estado inicial; tentativa contaminada
não integra o resultado independente. Nunca limpar caches/volumes históricos para
fabricar uma rodada limpa.
`probe_onlyoffice_import.cjs` anterior reconhece campo simples, mas não demonstrou
suporte ao complexo: resultado sem match é INCONCLUSIVO, não campo perdido.

Se C falhar, não executar D/E ou afrouxar o validador para produzir GO. Conservar
artefatos, restaurar datasets sintéticos padrão, parar serviços usados sem remover
volumes e reconferir hashes históricos. Não repetir matriz oficial sem novo plano.
