# B05-M2P3 — Isolamento causal e qualificação de fidelidade

**Escopo:** instrumentação independente, somente leitura, para a etapa posterior ao
[B05-M2P2]. **Não** substitui o executor M2R/M2P2, não edita DOCX e **nunca** autoriza
promoção editorial. Toda execução registra `HOLD_QUARANTINE`, `canPromote=false`,
`officialApproved=false` e mantém G1/G2/M3 bloqueados.

## Origem do escopo

Relatório `resultados(1).md`, 23/10/2026: callback autenticado persistiu versão
`QUARANTINED`, mas observou-se alteração do marcador de Cambria preta para Arial
Bold quase branca, além de mudanças em campos, `w:br/@w:clear`, partes OPC,
notas e 1.452 pixels fora da região automática da edição. O modo de inserção do
operador não foi registrado. A causa interna de `ExportAsFixedFormat` não foi
isolada e `SaveAs2` foi alternativa opt-in. **Não** atribuir automaticamente a
regressão ao mecanismo de edição ou à operação humana.

## Aplicação ao repositório

O kit adiciona somente a pasta `experiments/b05-m2p3-causal-fidelity/`.

1. Confira `git status --short`, branch e hash antes de aplicar. Branch esperada:
   `feat/b05-core-docs-api`. A base histórica no relatório era
   `017281a1830724cd66eee6ee505a704e9fa37b4d`; **não** presuma que ainda é HEAD.
2. Aplique o `.patch` na raiz do repositório com `git apply --check ...` e,
   após revisão, `git apply ...`, ou copie somente esta pasta.
3. Execute em Python >= 3.10 (testado na versão do ambiente anexado).
4. Para comparações de PDFs, instale `pymupdf` em ambiente isolado.
5. Execute `python -m unittest discover -s experiments/b05-m2p3-causal-fidelity/tests -v`
   a partir da pasta do kit; para execução da raiz use o comando abaixo.

```sh
cd experiments/b05-m2p3-causal-fidelity
python -m unittest discover -s tests -v
python -m py_compile qualify.py b05_m2p3/*.py
```

> `unittest` cria apenas material inteiramente sintético e descartável em diretórios
> temporários. Não há necessidade de credenciais, Entra, Cloudflare, API, túnel,
> ONLYOFFICE ou Word para rodar a suíte local. Esses componentes são necessários
> somente na **aquisição real** das quatro candidatas, fora do kit.

## Rodada real: isolamento por operação

Crie **quatro projetos/sessões realmente independentes** e com a mesma fixture
sintética rica em condições equivalentes, com uma origem imutável por sessão.
Cada controle recebe IDs e caminhos próprios; hashes iguais entre cópias da
origem são aceitáveis, mas **não** reutilize o mesmo arquivo de origem em duas
linhas do manifesto. Capture versão do ONLYOFFICE, configurações de editor,
mecanismo de persistência, sistema operacional, conversor Word e configurações
de PDF. Não insira material acadêmico real, tokens ou identificadores privados
no relatório compartilhável.

| Modo | Intervenção | Registro exigido |
|---|---|---|
| `no_edit` | Abrir e encerrar sem modificar | eventos `opened` e `closed` |
| `typed` | Substituição por digitação direta | evento `typed_text` |
| `plain_paste` | Colar como texto simples | evento `paste_plain` |
| `rich_paste` | Colar com formatação | evento `paste_rich` |

**Importante:** o rótulo declarado no JSON **não prova** que essa foi a ação.
Registre evidência verificável da operação observada (captura de tela/vídeo,
log de evento do editor quando disponível e diário do operador). Classifique
honestamente `evidence_strength`: `attested` (relato humano), `observed`
(observação registrada) ou `instrumented` (evento instrumentado).
O campo não gera equivalência editorial nem causalidade automática.

Para cada sessão: exporte um par DOCX origem/candidata do **armazenamento real**,
registre os SHA-256 **antes** de iniciar a qualificação, gere PDFs comparáveis
pelo mesmo procedimento Word controlado e documente o método utilizado. Quando
usar Word, registre corpo e páginas antes **e depois** da exportação;
`SaveAs2` pode atualizar `SAVEDATE` e a igualdade posterior não reconstitui o
estado anterior. Trate um `ExportAsFixedFormat` travado como ocorrência distinta.
Não modifique/corrija a candidata nem elimine regiões com diffs.

Faça um `manifest.json` com a estrutura de `examples/manifest.example.json`,
preenchendo caminhos relativos ao manifesto e hashes SHA-256 **reais** dos
quatro arquivos de cada sessão (DOCX origem/candidata, PDF origem/candidata).
Crie um `operator.json` **por sessão** seguindo `examples/operator-log.example.json`.
Declare `expected_edit` apenas para as três sessões editadas. Para `no_edit`,
use `null`. O retângulo `edit_region` opcional usa coordenadas PDF em pontos e
**não** exclui nenhum pixel: separa contagens dentro/fora da edição.

```sh
cd experiments/b05-m2p3-causal-fidelity
python qualify.py \
  --manifest .private/campaign-01/manifest.json \
  --out-dir .private/campaign-01/qualification-01 \
  --dpi 144
```

A execução aborta em caso de arquivo ausente, SHA-256 divergente, log incoerente,
reuso de arquivos entre sessões ou erro de leitura. Destino preexistente é
recusado para preservar a evidência. Os arquivos gerados são `qualification.json`
e `qualification.md`. A análise não altera os documentos de entrada.

## O que o analisador mede

- Compara conteúdo visível do corpo com **uma** substituição textual esperada,
  sinalizando edições invisíveis/inesperadas sem publicar parágrafos;
- Compara formatação OOXML **explícita** por posição de run, quebra de página,
  propriedades de parágrafo, instruções de campos (incluindo espaços e switches),
  mídias, notas e referências;
- Inventaria partes OOXML e relações; diferenças são **alertas**, não
  regressões independentes comprovadas nem equivalências implícitas;
- Rasteriza todas as páginas dos PDFs a 144 dpi, conta **todos** os pixels
  diferentes, páginas e tamanhos, com separação opcional dentro/fora da região;
- Consolida alertas por modo. A saída é `OBSERVATIONAL_ASSOCIATION_ONLY` e
  não transforma correlação em uma atribuição causal ao editor.

## Limitações importantes

O inventário é deliberadamente conservador e **não** substitui inspeção visual,
Word COM `StoryRanges/NextStoryRange`, resolução de estilos herdados, comparação
OPC exaustiva, verificação de equivalência semântica das instruções dos campos,
nem captura real de cliques, teclado ou operações de colagem. Com runs divididos
ou reordenados, reporta `RUN_STRUCTURE_CHANGED` e não inventa pareamentos.
Comparação PDF depende de PyMuPDF; a tipografia efetiva requer verificação
externa. O relatório público omite texto de parágrafos, caminhos locais,
credenciais e IDs de documentos; ainda assim, os artefatos de qualificação
**devem** permanecer em `.private/` até uma revisão humana de divulgação.

**Critério de saída do marco:** produzir evidência suficiente para discriminar
cenários e propor o próximo experimento/correção; não reclassificar a versão
anterior como aprovada. Não alterar gates, allowlists, regras de publicação,
normalização corretiva, backend Java ou worktree de auditoria. Sem merge/deploy.
