# Protocolo focal B05.0.1

Executar na branch derivada do PR #9. Evidência original permanece intacta.
Resultados A–F em `../results/b05-*.md`; manifesto público não contém hashes privados.
Usar runtimes Python/Node do workspace; o host de laboratório deve estar saudável
antes de capturar. Não iniciar a API do produto.

## Instrumentos e comandos

```powershell
python -m unittest discover -s experiments/b05-document-engine/scripts -p 'test_*.py'
python experiments/b05-document-engine/scripts/compatibility.py <copia.docx> --after <salvo.docx> --out <novo-json-em-.runtime>
python experiments/b05-document-engine/scripts/compatibility_cases.py --av3 <copia-privada> --saved-report <salvo-code-privado> --out <novo-diretorio-em-.runtime>
```

O último comando cria seis arquivos privados. F01: campo simples fonte-livre;
F02: complexo equivalente; F03: somente campo e contexto styles/settings do AV3,
sem referências às notas removidas; F04: campo original com contexto sintético;
F05: tabela Normal 12 pt; F06: cópia diagnóstica do relatório salvo com tamanhos
20→24. F03/F04/F06 dependem de fonte privada; nunca publicá-los automaticamente.
Nenhum deles é transformador de produção.

Para controle de docDefaults de F05, numa cópia nova inserir somente
`<w:sz w:val="24"/>` em `word/styles.xml`, dentro de `docDefaults/rPrDefault/rPr`.
Preservar original, recalcular o SHA descritivo da nova fixture e registrar a hipótese
antes do ensaio. O controle foi refutado como correção de paginação.

Manter um diretório de dados por rodada/motor. Não montar o mesmo `/data` em
CODE e ONLYOFFICE. Copiar somente fixtures necessárias e seus aliases/UUIDs para
um manifesto privado novo; assegurar que o hash corresponde à cópia atual.
Subir overlays privados usando os Compose existentes e registrar seus digests.

```powershell
node experiments/b05-document-engine/scripts/onlyoffice_cycles.cjs --run <privado> --playwright <pacote> --chrome <executavel> --python <runtime>
node experiments/b05-document-engine/scripts/editor_cycles.cjs --run <privado> --playwright <pacote> --chrome <executavel> --python <runtime> --persistent-noop true
```

O C1 novo salva XYZ, fecha/reabre, remove XYZ e salva outra versão. CODE remove
por UNO; ONLYOFFICE por teclado. Conferir corpo exato no checkpoint final e
registrar a diferença de transporte. Sem `--persistent-noop true`, o comportamento
histórico do runner CODE é preservado (Undo e um save), não o novo C1.

`fidelity.py capture --version <observada>` e `convert_onlyoffice` trabalham com
objetos imutáveis. O host precisa ter montado **o dataset dessa versão**; 404 de
outro dataset não é falha de conversão do editor. Diretórios incompletos de captura
não podem ser reutilizados. Converter duas vezes no mesmo destino é recusado.

## Probe de importação ONLYOFFICE

```powershell
node experiments/b05-document-engine/scripts/probe_onlyoffice_import.cjs --run <privado-com-fixture-simples> --playwright <pacote> --chrome <executavel>
```

Somente para o build fixado: nomes internos `ye`, `xZb`, `kH`, lidos no bundle
instalado identificado no manifesto. Aguarda anexação do cabeçalho, percorre um
grafo limitado sem executar getters e registra instruções privadamente. Não altera
o documento nem salva. Source version/hash são registrados e a versão após fechar
deve continuar igual. Falha de instrumentação/ausência de match é inconclusiva.
Não considerar esse probe API pública nem reutilizá-lo em outra versão por analogia.

## Word e inspeção

`word_layout_probe.ps1 -JobsFile <privado.json>` recebe lista com `id`, `source`,
`pdf`, `json` e `update_fields`. Todos os caminhos de documento/saída ficam em
`.runtime`, destinos novos. Abre em leitura; atualiza somente SAVEDATE/REF locais
quando solicitado; repagina/exporta e registra tipo/código/cache, páginas e fonte
efetiva por parágrafo. Não salva o DOCX. Restaurar opções/fechar só documentos do ensaio.

Rasterizar os PDFs exatos com `fidelity.py render`. Conferir as mensagens no Word,
não apenas o cache no PDF do editor. O pacote mínimo sem propriedades de modificação
não reproduz a aparência C0 do AV3: CODE mostra campo vazio antes do save e máscara
japonesa depois. Registrar a distinção, sem declarar reprodução visual integral.

## Encerramento

Comparar novamente hashes dos originais, controles e checkpoints históricos.
Auditar source/inventory/objeto persistido e metadados PDF das novas capturas.
Manter `.runtime`, `fixtures/generated`, `results/generated` ignorados.
Fechar navegadores; restaurar host CODE com corpus sintético padrão; restaurar
o dataset sintético ONLYOFFICE e parar seu Compose sem apagar volumes. Não enviar
chamados nem anexar documentos privados. Nenhum GO deriva só da execução destes comandos.
