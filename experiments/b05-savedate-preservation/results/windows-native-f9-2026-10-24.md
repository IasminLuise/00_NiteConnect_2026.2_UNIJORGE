# B05 — suporte nativo experimental a SAVEDATE/F9

F9 corrigido e validado no SDK modificado da bancada: campos simples e complexos
passaram a calcular SAVEDATE a partir da propriedade de último salvamento, sem
usar REF, data fixa ou substituição posterior na exportação. A saída simples foi
reaberta e exportada novamente, mantendo instrução e cache. Resultado restrito à
versão exata e aos controles descritos; não constitui release oficial do fornecedor.

## Implementação e limites de aplicação

`native_sdk_patch.mjs` produz uma cópia privada nova do bundle do ONLYOFFICE
10.0.0.1, build 169. Exige SHA-256 exato do artefato público e ocorrência única de
cada ponto de alteração; recusa outra versão, segunda aplicação e sobrescrita.
O digest fixa o código do fornecedor, não identifica documentos privados.

O patch altera código interno do SDK, não um handler externo que substitui F9:

1. Instrução e tipo próprios de SAVEDATE, separados de DATE, TIME e REF.
2. Serialização de campo simples conserva SAVEDATE e seus argumentos/switches.
3. Atualização de campo complexo e consulta de resultado usam o cálculo próprio.
4. Campo simples usa o caminho existente de substituição de runs mapeados.
5. Data vem exclusivamente do objeto Core carregado pelo SDK. Metadado ausente
   ou inválido não é substituído pelo relógio atual; o cache existente é conservado
   no caminho de atualização.
6. O formatador do SDK recebe o valor de data calculado e a picture `\@` do campo.
   A conversão considera o horário civil do navegador e o calendário Excel usado
   pelo formatador, sem aplicar novamente a conversão de meia-noite local do SDK.

Pictures explícitas usam o formatador existente. Defaults sem picture estão
limitados a LCIDs 1033 e 1046; os demais conservam o cache, sem alegação de cálculo
implementado. No caminho simples, o default atual é 1033; a resolução do idioma
do run simples ainda precisa ser ampliada para cobrir defaults de outros idiomas.
Não se certificam todos os switches/calendários Word, flags de bloqueio ou todos
os modelos institucionais a partir desta fixture.

Esta é uma alteração experimental do bundle compilado e fixado. Não houve build
completo de um fork do fornecedor nem publicação upstream. Um build mantido do
SDK deve portar esses pontos para os fontes e repetir os controles; não aplicar
substituições textuais em versões diferentes ou em serviço de produção.

## Execução Windows

Worktree `b05-core-docs-api`, branch `feat/b05-core-docs-api`, base
`017281a1830724cd66eee6ee505a704e9fa37b4d`; trabalho local anterior preservado.
Contêiner próprio do ONLYOFFICE com portas em loopback, JWT técnico efêmero e
fixture inteiramente sintética. Sem túnel, modelos oficiais ou token Entra.
Harness direto do Docs API, com exportação técnica autorizada da bancada: não
representa nova validação do fluxo NITE autenticado com `download:false`.
Backend, adaptador anterior, gate, quarentena, permissões e produção inalterados.

A fixture contém quatro SAVEDATE: um no corpo com picture explícita
`yyyy-MM-dd HH:mm:ss` e três nos cabeçalhos primeira/par/principal. Inclui também
três tabelas, logotipo, PAGE/NUMPAGES, duas quebras de página e um REF negativo
com bookmark inexistente. As cópias simples e complexa derivam da mesma origem.

| Controle real | Operação | Resultado |
| --- | --- | --- |
| Motor original, campo complexo | Selecionar corpo e pressionar F9 | Cache do SAVEDATE vira `Error! Reference source not found.`; DOCX preservado como controle negativo |
| SDK corrigido, campo complexo | F9 no corpo e no cabeçalho da primeira página; exportar | Corpo `2020-01-02 00:04:05`; cabeçalho `1/2/2020 12:04:05 AM`; sem erro SAVEDATE |
| SDK corrigido, campo simples sem adaptador | Mesmas operações e exportação | Mesmo resultado correto; campo continua simples e instrução continua SAVEDATE |
| Reabertura da saída simples | Abrir no motor corrigido, sem novo F9, e exportar | Quatro instruções SAVEDATE preservadas; cache do corpo continua `2020-01-02 00:04:05` |

A origem tem `dcterms:modified=2020-01-02T03:04:05Z`. O navegador está em
America/Bahia, UTC−03:00: o resultado esperado é 02/01/2020 00:04:05. Não se
inseriu a data de execução de 2026. No teste inicial da conversão, o helper do SDK
deslocou o resultado para o dia anterior; esse defeito foi isolado, corrigido e
retestado. Os testes de fuso incluem UTC, Bahia e Kathmandu, com mudança de dia
e offset fracionário. O REF existente não foi convertido em SAVEDATE.

F9 pela UI foi observado no corpo e no cabeçalho da primeira página. Os outros
dois cabeçalhos foram inventariados/exportados e avaliados no Word; não se afirma
que F9 foi acionado individualmente neles nesta rodada. Reabertura foi controle
de preservação, não teste de F9 depois de um novo salvamento.

A primeira sessão foi interrompida antes da exportação positiva final e o Docker
ficou indisponível. Evidência negativa e tentativas foram preservadas; a conclusão
positiva usa pasta nova `resume-01`. O serviço reiniciou durante o controle simples;
a sessão foi reconectada e F9 foi executado novamente antes da captura. Não se
usou a sessão desconectada como prova de sucesso.

## Word, formatação e PDFs

Word 16.0, build 17932: três pares de origem/candidata, seis PDFs. Read-only,
macros desabilitadas, todas as histórias atualizadas explicitamente, fonte DOCX
conferida por hash antes/depois e mantida intacta. Word reconheceu os quatro
SAVEDATE como tipo 22, sem erros SAVEDATE, em cada candidata. PAGE e NUMPAGES
mantidos; REF negativo continua tipo REF e produz o erro esperado ao atualizar
no Word. Esse erro intencional não foi atribuído a SAVEDATE.

O probe precisou importar explicitamente Microsoft.PowerShell.Utility no processo
Windows PowerShell: a primeira execução não encontrou Get-FileHash e não gerou
evidência PDF válida. A nova execução concluiu os três pares; falha inicial ficou
registrada separadamente. Nenhuma configuração global de PowerShell foi alterada.

PDFs via `SaveAs2`, raster RGB a 144 dpi, comparação integral sem máscaras.
Todas as páginas têm 1224 × 1584 pixels; todos os documentos mantêm três páginas,
três tabelas, duas quebras explícitas e os mesmos bytes de imagem.

| Par Word | Pixels diferentes por página | Causa verificada |
| --- | --- | --- |
| Origem complexa × export após F9 nativo | 251 / 127 / 127, total 505 | Minuto da exportação Word: `23:22` → `23:23`; no cabeçalho e no campo do corpo |
| Origem simples × export após F9 nativo | 0 / 0 / 0 | Mesmos resultados renderizados nas condições pareadas |
| Fonte da reabertura × nova exportação | 0 / 0 / 0 | Sem nova divergência visual nas condições pareadas |

No primeiro par, 127 pixels por cabeçalho estão nas linhas y=129–145. Na página 1,
outros 124 pixels do campo do corpo estão em y=364–380. O texto extraído dos PDFs
confirma a mudança do minuto; nenhuma região foi excluída. As datas do corpo e
cabeçalhos mantiveram ArialMT, tamanho aproximado 11,04 pt, na origem e candidata.
Não reapareceu a alteração indevida para negrito observada no erro REF anterior.
Páginas completas e diferenças foram revisadas visualmente.

SaveAs2 atualiza SAVEDATE de novo durante a geração PDF, inclusive na referência.
Logo, os PDFs demonstram geometria/formatação após atualização no Word; a prova
do valor produzido pelo F9 no ONLYOFFICE vem da UI e do cache OOXML adquirido
antes do Word. Não se usa a atualização corretiva do Word para certificar F9.

Corpo visível só alterou o cache sintético esperado para a data calculada, nas
duas representações. Reabertura manteve o corpo visível integralmente. Nenhum
resultado estrutural foi convertido em PASS; anomalias OPC anteriores não foram
tratadas por este patch.

## Validação

Após a implementação final:

```powershell
$env:B05_SDK_BUNDLE='.private/savedate-fix-20261024-01/sdk-all-installed.js'
node --test experiments/b05-savedate-preservation/tests/native_sdk.test.mjs
node --check experiments/b05-savedate-preservation/native_sdk_patch.mjs
git diff --check
```

Oito testes nativos aprovados. Exercitam trechos reais do parser/serializador do
bundle fixado, isolamento de DATE/TIME/REF, tipo SAVEDATE, metadado inválido,
formatação, fusos, despacho de atualização simples, sintaxe do bundle resultante
e recusa de artefato divergente ou já modificado. Casos de defeito foram observados
falhando antes das respectivas correções. Stubs de documento/formatador nos testes
de despacho não substituem as capturas do motor real acima.

Os 15 testes Python e cinco Node da preparação/provisionamento anteriores não
foram repetidos: seus arquivos e dependências não mudaram. Java/Flyway/API também
não repetidos; nenhuma alteração nessas camadas. Não se declara nova aceitação
institucional nem integração de SDKs adicionais.

Evidências, fontes do fornecedor, bundles, hashes, DOCX, inventários, logs, seis
PDFs e imagens ficam em `.private/savedate-native-f9-20261024-01`, com resultados
positivos finais em `resume-01`. Nada disso precisa ser publicado no Git.
