# Decisão do ensaio controlado de fidelidade

17/10/2026. **NO-GO para aprovar integralmente os dois modelos nos motores testados.**
Isso não declara impossibilidade de correção em versões futuras. A integração de
produto continua bloqueada; nenhum modelo oficial foi adaptado para contornar bugs.

## Método e controles

Executor em `scripts/fidelity.py`, preparação em `prepare_fidelity.py`, controle
Microsoft Word em `word_control.ps1` e ensaios de navegador em `editor_cycles.cjs`
e `onlyoffice_cycles.cjs`. Pacotes/fontes são os do runtime do Codex e das imagens
fixadas. Nenhuma dependência adicionada à API do produto.

Cada modelo recebeu três cópias independentes: original, inserção/remoção e três
ciclos de preenchimento curto (` B1 B2 B3` na primeira linha do corpo). No Word,
os controles receberam o mesmo conteúdo, conferido por parágrafo e célula. As
versões são baixadas do host WOPI, identificadas e convertidas pelos mesmos bytes.
O executor não apaga espaços nem usa igualdade do número de páginas como nota.

No CODE, o navegador usa o transporte de composição e comandos UNO do editor;
o save solicita serialização mesmo sem modificação pendente. Não equivale a um
teste exclusivo de teclado humano. No ONLYOFFICE, usa teclado no editor e aguarda
nova versão WOPI após encerrar a sessão; indicador de Save não é prova suficiente.
O caso inserir/remover no ONLYOFFICE inclui salvamento intermediário e reabertura.

Evidências privadas: `.runtime/fidelity-20261017-run2`, `.runtime/onlyoffice`,
`.runtime/reproductions-20261017` e `.runtime/onlyoffice-reproductions-20261017`.
O primeiro ensaio de automação, em run1, não integra os resultados conclusivos.

## Matriz observada

| Caso | Word controle | Collabora CODE 26.04.5.1.1 | ONLYOFFICE Community 10.0.0.1 |
|---|---|---|---|
| Relatório original | 7 páginas | 6 páginas | 7 páginas |
| Relatório inserir/remover | 7 páginas | 6 no motor e no Word | 7 no motor e no Word |
| Relatório três preenchimentos | 7 páginas | 6 no motor e no Word | 7 no motor e no Word |
| AV3 original | 5 páginas | 5 páginas; data japonesa | 5 páginas; valor de data em cache |
| AV3 inserir/remover | 5 páginas | 5; erro de formato ao reabrir no Word | 5; erro de referência ao reabrir no Word |
| AV3 três preenchimentos | 5 páginas | 5; erro de formato no Word | 5; erro de referência no Word |

**Conteúdo:** ONLYOFFICE preservou exatamente os parágrafos e células dos controles
nos quatro casos finais. CODE preservou o corpo/células do AV3, mas removeu três
espaços finais em dois parágrafos do relatório, inclusive no cenário sem alteração
final. Isso não representa perda de palavras; permanece uma diferença explícita.

**Campos:** no AV3, CODE regrava SAVEDATE com máscara contendo localidade japonesa;
o Word apresenta erro de caractere de formato. ONLYOFFICE regrava o campo como
`REF \\* MERGEFORMAT`, perdendo SAVEDATE; o Word apresenta erro de indicador sem
nome. O PDF do próprio ONLYOFFICE mantém o resultado em cache e oculta esse defeito.
Logo, comparar apenas o PDF exportado pelo motor daria um falso positivo.

**Imagem:** nesta rodada controlada, os PDFs Word de ambos os motores mostraram
o logotipo nas cinco páginas. Reexportar o mesmo DOCX histórico que havia produzido
um PDF com logotipo somente na última página mostrou o logotipo nas seis páginas;
o controle Word com os mesmos marcadores longos também teve seis páginas e seis
logotipos. A ocorrência anterior é de uma renderização, não prova de falha
persistente da ancoragem no DOCX. Não há correção do motor demonstrada para ela.
O controle Word agora chama Repaginate antes de exportar. Integridade de bytes,
isoladamente, continua insuficiente para certificar posição/visibilidade.

## Intervenções e ponto de parada

- `LANG/LC_ALL=pt_BR.UTF-8` no processo CODE: relatório permaneceu com seis páginas
  e AV3 continuou com representação japonesa. Não adotado como correção.
- Montagem individual somente leitura de 20 arquivos das famílias necessárias
  (Calibri, Arial, Times New Roman, Consolas e Cambria): relatório permaneceu com
  seis páginas; AV3 ficou inválido visualmente, com duas páginas e 90 caracteres
  extraídos. Configuração rejeitada, sem copiar/distribuir fontes no repositório.
- Não repetir Calibri/grade da tabela como soluções já descartadas. Diferenças de
  estilos e serialização restantes não receberam uma causa única comprovada.
- Consulta ao registro oficial: 26.04.5.1.1 segue a tag CODE mais recente da série.
  Nenhuma candidata com correção pertinente identificada. O digest do índice
  multiarch é diferente do digest amd64; não é uma nova versão do motor.
- ONLYOFFICE foi comparado em Compose, rede, dados e portas separados. Imagem
  fixada por digest, marca preservada, sem compra, publicação ou homologação legal
  de produção. Sua falha de SAVEDATE impede recomendação definitiva.

Não há transformação corretiva de XML no caminho de salvamento. A troca de motor
não foi aplicada ao produto. A fronteira observada é importação/exportação DOCX;
o host comprova a versão recebida e não corrige semanticamente os documentos.

## Reproduções e limites

`make_reproductions.py` gera um SAVEDATE sintético e uma imagem sintética com a
geometria da ancoragem. Os arquivos ficam privados para inspeção antes de qualquer
divulgação. Nenhum chamado ou mensagem foi enviado aos fornecedores.

No CODE, o SAVEDATE simples sintético regrava uma máscara diferente, mas não
reproduziu a localidade japonesa do AV3; a imagem isolada permaneceu nas três
páginas no Word. Portanto esses arquivos são controles/reproduções parciais,
não provas de que o caso completo foi minimizado. A combinação exata de cabeçalho,
estilos e propriedades do AV3 ainda precisa ser reduzida para um relato público.
No ONLYOFFICE, o SAVEDATE sintético também se tornou REF, confirmando a perda do
tipo de campo independentemente do modelo oficial.

A inspeção visual usa PNGs de todas as páginas e folhas de contato. Não há aceite
editorial nem comparação pixel a pixel. Igualdade de paginação no ONLYOFFICE não
certifica equivalência de quebras, medidas ou tipografia; os campos já bastam para
reprovar o requisito de zero falhas conhecidas.

## Recomendação

Manter o bloqueio e preparar relatos técnicos para correção dos fornecedores:
primeiro SAVEDATE, depois a paginação do relatório. A ocorrência histórica do
logotipo exige reprodução estável antes de atribuição ao motor. ONLYOFFICE
é candidato comparativo mais próximo na paginação e preservação do corpo, mas
não atende ao requisito de preservar integralmente o modelo AV3. Não flexibilizar
silenciosamente o requisito nem aprovar G1 com base apenas no relatório semanal.

Fontes primárias consultadas: [WOPI ONLYOFFICE](https://api.onlyoffice.com/docs/docs-api/using-wopi/overview),
[instalação Community](https://helpcenter.onlyoffice.com/docs/installation/docs-community-install-docker.aspx),
[código de egress WOPI](https://github.com/ONLYOFFICE/server/blob/master/DocService/sources/wopiEgress.js)
e [licença do DocumentServer](https://github.com/ONLYOFFICE/DocumentServer/blob/master/LICENSE).

## Validação final e estado entregue

- Host: 28 testes Java aprovados após o suporte de discovery/timestamp do comparador.
- Executor: 12 testes Python aprovados após correções da revisão independente;
  sintaxe dos dois executores Node validada. A revisão encontrou mistura de saídas
  e corrida de versões; ambos corrigidos com testes que falharam antes do ajuste.
- Auditoria retroativa: 18 checkpoints oficiais correspondem aos identificadores e
  hashes das versões persistidas; capturas anteriores continuam válidas.
- F01–F09 sintéticos: 9/9 convertidos em CODE, LibreOffice e ONLYOFFICE (27 PDFs).
  Conversão bem-sucedida não equivale a validação visual/funcional de todos os casos.
- Probes ONLYOFFICE: RO PutFile 403, documento alheio 404, revogação 200 seguida de
  GET 401, conversão pública 403. Teste de payload binário usa octet-stream.
- Código/API principal e migrações intactos; verificações da API não repetidas.
- Relatos preparados em `vendor-cases.md`, não enviados. Fontes/modelos intactos;
  nenhum commit, push, deploy ou alteração de `.env`.

O plano atingiu seu ponto de parada. A compatibilidade dos modelos não foi
corrigida integralmente; não há solução aprovada escondida atrás dos testes verdes.
