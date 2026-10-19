# B05.0.5 — paginação SuperDoc

**NO_GO_NA_CONFIGURACAO_ENSAIADA.** Duas sessões independentes, mesma origem
imutável: seis páginas no editor; original e export no-op com cinco no Word.
Nenhuma alteração candidata de configuração foi aplicada.

| Medida | Sessão 1 | Sessão 2 | Word readonly |
|---|---:|---:|---:|
| Páginas | 6 | 6 | 5 |
| Contagens dos eventos de paginação | 6, 6 | 6, 6 | não aplicável |
| Janela estável exigida | ≥3 s | ≥3 s | Repaginate |
| Página do primeiro título após a quebra manual | 3 | 3 | 2 |
| Página 2 com corpo sem texto editorial visível | sim | sim | não |

A primeira divergência de **distribuição entre páginas** está no parágrafo OOXML
ordinal 15 (zero-based, incluindo células), Word parágrafo 22. O mesmo source
anchor aparece como fragmento vazio na página 2 e texto na página 3. No Word,
a posição do início do parágrafo fica antes da quebra, na página 1; por isso foi
adicionada extração opcional da primeira letra após controles: página 2,
Y=210,75 pt. Confundir essas duas posições ocultaria a divergência.

Na segunda sessão: fragmento vazio a Y=285,39 CSS px dentro da página 2; texto
do título a Y=293,39 px dentro da página 3. As medidas são de caixas DOM; não
são métricas tipográficas idênticas às coordenadas Word. Viewport 1440×1100,
escala reportada pelo navegador, zoom CSS 1 e transform none na coleta inicial.
Papel OOXML 603×792 pt, correspondente ao shell 804×1056 CSS px. Margens
laterais 56,7 pt; superior/inferior 28,35 pt; header/footer 35,45 pt.

O parágrafo tem `w:br w:type="page"` seguido de `w:lastRenderedPageBreak`.
O segundo registra a paginação calculada anteriormente, conforme
[ISO/IEC 29500 reproduzida pela Microsoft](https://learn.microsoft.com/en-us/dotnet/api/documentformat.openxml.wordprocessing.lastrenderedpagebreak?view=openxml-3.0.1).
**Hipótese, não causa comprovada:** tratamento duplicado da fronteira, ou sua
interação com o fluxo anterior, poderia explicar a página vazia. Não foi isolado
o algoritmo interno nem encontrado ajuste suportado que resolvesse este caso.
Não se removeu nenhum marcador para obter cinco páginas.

Fontes computadas observadas: Times New Roman, Arial e Consolas, coerentes com
as famílias declaradas/Word. `document.fonts.check` retornou disponível para as
famílias consultadas, mas não prova qual arquivo de fonte desenhou cada glifo;
substituição e causa métrica fina continuam **INCONCLUSIVE**. Não houve montagem
de fontes, mudança de margens, CSS corretivo, versão ou XML.

A primeira coleta de detalhes ocorreu antes de materializar páginas virtualizadas
3–6: ausência de DOM não foi tratada como páginas vazias. A segunda captura
materializou cada página por scroll e guardou tiles superior/inferior sem alterar
o layout do documento. O erro material da página 2 foi observado em ambas.
Foram inspecionadas visualmente a fronteira inicial, páginas 1–3 e a página 6,
além da página 2 Word. Não se declara homologação visual de todas as páginas.

Gate B: **FAIL**. F07/A-B **NOT_RUN**, por ausência de hipótese única de
configuração suportada. Orçamento de duas sessões consumido; parar sem correção.

Evidências privadas: `browser/av3-diagnostic-{1,2}/pagination.json`, tiles PNG,
exports, `av3-source.json/pdf`, `av3-text-position.json`, `av3-diagnostic-1-word.json`,
`av3-ooxml-layout.json`, `render/`, sob `.private/b0505/`.
