# B05.0.5 — notas SuperDoc

**Inventário esclarecido; equivalência não certificada. Gate inalterado.**

| Corpus | Original | Export | Referências de uso no original |
|---|---|---|---:|
| AV3 | footnotes e endnotes, cada uma com IDs −1/0 | partes removidas | 0 |
| Relatório semanal | mesma estrutura, com spacing nos separadores | partes removidas | 0 |

IDs −1/0 têm tipos separator/continuationSeparator, elementos separadores e
nenhum texto de nota regular. As propriedades globais e relações de notas
também são removidas. A busca cobre todos os XML, corpo, seções e estilos;
o inventário registra propriedades, content types, relações e referências.
Não foi identificada referência de nota de uso apontando para parte removida.
AV3 usa export novo B05.0.5; relatório usa export **histórico B05.0.4 congelado**,
sem nova sessão oficial. Não se relata perda comprovada de notas reais.

Isso não autoriza uma equivalência geral: os separadores e configurações podem
ser relevantes quando uma nota for inserida posteriormente. A norma descreve
essas estruturas, não garante equivalência editorial após removê-las:
[Footnotes](https://learn.microsoft.com/en-us/dotnet/api/documentformat.openxml.wordprocessing.footnotes?view=openxml-3.0.1),
[FootnoteProperties](https://learn.microsoft.com/en-us/dotnet/api/documentformat.openxml.wordprocessing.footnoteproperties?view=openxml-3.0.1).

O novo `notes_probe.py` é somente inventário. Um controle sintético independente
do fornecedor diferencia separador de nota com texto; identidade passa e perda
de conteúdo falha no gate. É teste do scanner/gate, **não** certificação Word de
equivalência nem teste de preservação de notas reais pelo SuperDoc. Essa prova
mais ampla não foi produzida; por isso não foi criada flag nova de tolerância.

Gate C: **INCONCLUSIVE** para equivalência, **FAIL_STRUCTURAL mantido** nas
candidatas oficiais. Nenhum XML reparado após export. A falha de prévia já é
eliminatória; não expandir pesquisa de notas para tentar aprovar o candidato.

Evidências privadas: `.private/b0505/notes-inventory.json`, `notes-av3-v2.json`
e arquivos de origem/export vinculados pelos manifestos privados.

