# B05.0.1 F — decisão técnica e gates

17/10/2026. **NO-GO para habilitar edição web dos dois modelos oficiais nas
versões avaliadas.** Não houve correção nativa validada nem novo motor aprovado.
A investigação chegou ao ponto de parada com reproduções melhores e propriedades
causais delimitadas. Não é conclusão do Bloco 05 nem autorização para produto.

## 1. Baseline

Base `360fc24`; PR #9 aberto. CODE 26.04.5.1.1 e ONLYOFFICE 10.0.0.1 em redes,
dados e portas separados; digests de índice/plataforma em
[manifesto](b05-compatibility-baseline.json). Não foi executada imagem candidata.
Word 16.0/build 16.0.17932 é a referência local, sem garantia universal de layout.
[Relatório A](b05-compatibility-baseline.md) registra o congelamento.

## 2. Defeitos e reproduções

- ONLYOFFICE: perda de SAVEDATE **simples** já observada no modelo do editor antes
  de edição/save solicitado; dois ensaios sintéticos independentes persistem REF
  e o Word dá erro. A variante complexa sintética preserva instrução e atualiza no
  Word; não foi aplicada ao AV3 nem constitui solução aprovada.
- CODE: novo pacote sintético reproduz a máscara inválida e o erro Word após save.
  Seu C0 mostra campo vazio; não reproduz integralmente a visualização japonesa
  inicial do AV3. A distinção está no [relatório B](b05-savedate-diagnosis.md).
- CODE/paginação: fonte efetiva de muitos parágrafos do relatório cai de 12 para
  10 pt. Uma cópia diagnóstica restaurou tamanhos/fronteiras de página no Word.
  Uma tabela sintética reproduz 2→1 página e fonte 10 pt. Não há correção aplicada
  ao motor ou ao modelo. Ver [relatório C](b05-collabora-pagination.md).

## 3. Hipóteses, tentativas e limites

[Relatório D](b05-engine-candidate-results.md) contém CX-001–005, controles,
variáveis e comandos. A necessidade de styles/settings originais para a máscara
foi refutada; adicionar tamanho em docDefaults também não resolveu a tabela.
Calibri, localidade e grade já refutados não foram repetidos. Não foi localizado
patch/release pertinente que justificasse instalar outra versão. Isso não prova
que uma versão futura ou intervenção do fornecedor seja inviável.

Os subestágios nativos exatos continuam não isolados. As ações justificadas são
os relatos com controles mínimos; nenhum chamado foi enviado. Não foi implementado
preservador/reparador de XML, achatamento de campo ou adaptação automática de modelo.

## 4. Matriz C0/C1/C2 dos modelos oficiais

Dados do ensaio controlado B05.0, reusados após auditoria dos bytes. Não se repetiu
a matriz oficial nesta rodada: nenhum candidato passou pelo gate focal D.
O no-op CODE anterior usa Undo sem save intermediário; o C1 novo, somente nas
fixtures focais, usa duas versões persistidas. Não se confunde os dois protocolos.

| Modelo/caso | Word equivalente | CODE | ONLYOFFICE | Estado desta rodada |
|---|---|---|---|---|
| Relatório C0 | 7 páginas | 6 | 7 | Baseline válido; leitura Word adicional confirmou 7 |
| Relatório no-op B05.0 | 7 | 6 no motor/Word | 7 no motor/Word | Reusado; sem candidato corrigido |
| Relatório C2 | 7 | 6 no motor/Word | 7 no motor/Word | Reusado; sem candidato corrigido |
| AV3 C0 | 5 | 5; data japonesa | 5; cache | Reusado |
| AV3 no-op B05.0 | 5 | máscara inválida no Word | REF/erro no Word | Reusado; novas fixtures confirmam falhas |
| AV3 C2 | 5 | máscara inválida no Word | REF/erro no Word | Reusado; sem candidato corrigido |

Nenhum candidato satisfaz zero falhas conhecidas. A paginação melhor no ONLYOFFICE
não compensa a perda de campo, nem igualdade de páginas prova equivalência visual.

## 5. Campos e layout

O relatório B documenta campos simples/complexos, cache, instrução e tipos Word.
No relatório C, a primeira âncora textual única divergente localizada em C0 é o
parágrafo XML 37 (Word página 2/CODE página 1); a inspeção do final da primeira
página confirma a linha extra acomodada pelo CODE. Na fronteira salvo→Word, a
primeira divergência encontrada é o parágrafo COM 78, com mudança efetiva 12→10 pt.

Trocar somente 304 propriedades de tamanho numa cópia do salvo restaurou as sete
páginas e as fronteiras de todos os parágrafos COM. Isso é evidência diagnóstica;
não recupera os espaços perdidos nem certifica bordas, âncoras ou todo o layout.
O logotipo não recebeu nova atribuição de defeito.

## 6. Persistência e inspeção

Foram capturadas 16 novas versões de save por navegador em oito cenários C1
sintéticos e sete checkpoints de originais, totalizando 23 capturas concluídas.
Cada hash confere com inventory e o objeto imutável do dataset correspondente.
Os oito corpos finais voltaram exatamente às entradas. As quatro conversões
ONLYOFFICE registram a mesma versão/hash capturada; CODE confere X-Source-Version.
O probe de importação ONLYOFFICE manteve a versão original inalterada.

A auditoria final reconfirmou 42 arquivos históricos: três originais, 29 capturas
e dez controles Word. Nenhum foi alterado. Houve 20 exportações/probes Word novos,
incluindo 11 de versões salvas. Foram rasterizados 39 PDFs/58 páginas. A inspeção
visual focal confirmou erros de campo e a quebra da primeira página; rasterizar
todas as páginas não equivale a inspecionar editorialmente todas elas.

## 7. Aceite editorial

Não concedido. Não há assinatura/data de um responsável institucional pelos modelos.
A decisão técnica segue os critérios de falha já aprovados; não presume autoridade
para alterar modelo, liberar edição externa, contratar serviço ou aceitar desvios.

## 8. Alternativa Microsoft

[Relatório E](b05-microsoft-feasibility.md): CSPP não está disponível por mero login
ou licença Microsoft 365; elegibilidade do projeto não comprovada. Catálogo NITE
com arquivos no SharePoint é uma candidata condicionada a direitos institucionais
e PoC própria, com Microsoft como fonte dos arquivos. Edição desktop/reenvio muda
a experiência requerida. Nenhum caminho foi implantado nem aprovado por analogia
com o Word desktop.

## 9. Gates separados

| Gate | Estado | Motivo |
|---|---|---|
| G1-F | REPROVADO nas versões testadas | SAVEDATE nos dois motores; paginação/conteúdo no CODE |
| G1 completo | NÃO APROVADO | Fidelidade reprovada e pendências operacionais P01–P20 do ADR |
| G2 | NÃO ABERTO/APROVADO | Decisões funcionais/contratos D01–D26 e autorização de produto pendentes |

## 10. Decisão e continuidade

Manter edição integrada desses modelos desabilitada para os candidatos ensaiados.
Priorizar correção nativa do SAVEDATE simples no ONLYOFFICE, por ser o candidato
mais próximo no relatório. CODE tem dois bloqueios reproduzíveis para fornecedor.
Retomar ensaios quando houver patch/versão com alteração pertinente: primeiro o
caso sintético, depois C0/C1/C2 dos dois modelos e duas execuções independentes do
defeito corrigido. Fechar as demais pendências G1 antes de qualquer avanço de produto.

Se esse caminho não for viável, submeter ao responsável uma PoC SharePoint autorizada
ou mudança formal para edição externa. Não avançar com V6/edição web reprovada.
Responsável por decisão institucional/aceite: a designar; aprovação não presumida.

## 11. Verificações

- Após alterações de instrumentação: 25 testes Python aprovados (13 novos), com
  RED→GREEN observado; sintaxe dos três runners/probes Node e parse PowerShell aprovados.
- Host Java não mudou: 28 testes do baseline/CI #9 permanecem válidos; não foram
  repetidos localmente. CI da branch nova valida código sintético, não modelos oficiais.
- API principal e seus contratos não mudaram; seus 185 testes históricos não foram
  repetidos. Não se atribui esse baseline antigo a uma nova execução.
- C0/C1/C2 oficiais e corpus completo não repetidos: gate focal falhou, sem candidato
  corretivo. TLS/coedição/revogação ativa/carga/aceite editorial permanecem pendentes.

## 12. Rastreabilidade

[PR baseline #9](https://github.com/IasminLuise/00_NiteConnect_2026.2_UNIJORGE/pull/9),
[plano B05.0.1](../../../docs/implementation/bloco-05-compatibility-plan.md),
[protocolo](../scripts/FIDELITY-B0501.md) e adendo no ADR-B05-001.
O PR empilhado desta execução será registrado após sua criação, sem merge.
Modelos oficiais e produção preservados; nenhum conteúdo pessoal ou fonte binária
foi versionado. Serviços de laboratório restaurados conforme conferência final da entrega.

## Matriz R01–R24

| IDs | Estado e limite |
|---|---|
| R01 | PASS: baseline/controles e versões auditados |
| R02 | PASS: campos simples/complexos, tipo/instrução/cache/parte inventariados |
| R03 | FAIL do motor: simples vira REF; complexo é controle distinto |
| R04 | Baseline C2 FAIL; não repetido sem correção focal |
| R05 | Baseline AV3 C0 válido; reduzido C0 vazio, sem alegar reprodução visual integral |
| R06–R08 | FAIL de fidelidade; novas fixtures C1 e atualização Word confirmam erros |
| R09 | PASS diagnóstico: diferença inicial localizada/inspecionada; layout continua FAIL |
| R10 | FAIL do motor; propriedade de tamanho com impacto demonstrado em cópia |
| R11 | Baseline válido; novos C1 sintéticos sem perda de corpo; sem aprovação oficial |
| R12 | Não executado: nenhuma versão/patch pertinente identificado; não chamar de PASS |
| R13–R14 | PASS nos saves/capturas/conversões efetivamente executados |
| R15 | PENDENTE: aceite editorial institucional |
| R16 | Parcial: reproduções fonte-livres do export/Word e tabela; C0 CODE reduzido difere |
| R17 | PASS instrumentação: entradas duplicadas e entidades UTF-16/limites; não é corpus hostil |
| R18–R19 | Baseline do host válido; sem mudança WOPI/Java, não repetido localmente |
| R20 | PASS limitado: versões recuperadas após recriar host/remontar dataset; não é teste de crash |
| R21 | Sem atribuição nova ao logotipo; baseline preservado |
| R22 | Não repetido completo; seletividade justificada pelos gates |
| R23 | Análise documental concluída; direitos/PoC Microsoft não confirmados |
| R24 | Gates e autoridade separados; aceite institucional não presumido |
