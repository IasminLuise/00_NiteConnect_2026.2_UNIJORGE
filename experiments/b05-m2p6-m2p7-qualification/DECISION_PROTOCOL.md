# Protocolo de encerramento B05 — ratificação externa obrigatória

## Decisões possíveis e limites

**M2P6 (técnica):** inventariar as diferenças de partes e relacionamentos, notas referenciadas, instruções de campos, estilos e trechos explícitos de formatação. Erros OPC novos nas candidatas são impedimentos técnicos a revisar. Qualquer equivalência além dos atributos literalmente comparados fica **não demonstrada** até evidência independente.

**M2P7 (institucional):** verificar existência/consistência documental das entradas (hashes dos arquivos imutáveis), cobertura de recursos declarados, recibos de autorização, 8 verificações reais de integração normal, resoluções dos `UNPROVEN` anteriores e os 3 pareceres. Presença de recibo **não garante validade científica**: verificar independentemente cada arquivo, signatário, corpus e procedimento.

A saída `READY_FOR_FORMAL_GATE_REVIEW_NOT_APPROVAL` representa que há material suficiente **para apreciação humana**, não que o motor esteja certificado. O sistema não concede `GO`, mesmo se todas as entradas tiverem recibos. Uma decisão institucional definitiva necessita deliberação formal separada, aceite editorial com escopo e gates reais reexecutados sobre o corpus autorizado.

## Matriz decisória humana posterior (fora do patch)

| Estado final deliberável pela equipe | Pré-condições mínimas |
|---|---|
| GO no escopo aprovado | Sem falha impeditiva; equivalência comprovada nas estruturas e recursos exigidos; corpus representativo formalmente aceito; funcionamento com permissões **normais** da API; autenticação, callbacks, quarentena e exportação demonstrados; revisão técnica/editorial/institucional independentes concluídas; gates reais aprovados |
| GO condicionado por escopo | Somente funções e tipos documentais avaliados estão contemplados, limitações são explicitamente aceitas em ato institucional, sem flexibilizar proteções nem declarar fidelidade fora do escopo |
| NO-GO | Falha impeditiva reproduzida ou risco inaceitável aprovado pela instância responsável; registrar alternativa e plano de transição, sem afirmar que outro motor é mais fiel sem teste |
| HOLD | Qualquer prova essencial ausente ou inconclusiva; preservar `HOLD_QUARANTINE`, `officialApproved=false`, G1/G2/M3 bloqueados |

## Critérios em aberto na campanha precedente

O relatório real M2P4 + M2P5 manteve os seguintes **9 `UNPROVEN`** (usar IDs exatos do `decision.json` em produção, não copiar de uma memória):

- `NOTES_SEMANTICS`
- `OOXML_STRUCTURAL_EQUIVALENCE`
- `FIELD_INSTRUCTIONS`
- `VISUAL_FIDELITY`
- `GLYPH_LOCALIZATION`
- `WORD_PRE_POST_EXPORT`
- `OFFICIAL_REPRESENTATIVE_CORPUS`
- `BACKEND_GATE_INTEGRATION`
- `INDEPENDENT_EDITORIAL_SIGNOFF`

Fonte: relatório `windows-2026-10-24(1).md`. Quatro controles reais M2P3: `CTRL-01`, `TYPE-03`, `PLAIN-01`, `RICH-01`.

**Sem exceções para 1.452 pixels.** O resultado de raster no controle sem edição permanece pendente. Alterações de `SAVEDATE` não são excluídas dos totais; registrar variações e preservar campos em cache anteriores ao Word. Não criar equivalências de estilos/relações por ignorar IDs sem provar resolução e funcionalidade.
