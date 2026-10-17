# B05.0.2 — execução delimitada

17/10/2026. Base `ac60d148ace51784aecfc7d648d2e95ffc524f8b`, branch
`spike/b05-adaptador-docx`, derivada de #10. PRs #9/#10 conferidos abertos.
Somente laboratório/documentação; nenhuma alteração do produto ou modelos oficiais.

## Resultado

**Parada no Gate C. NO-GO-F para liberar o fluxo experimental.** A transformação
fonte-livre preserva o campo no Word; a candidata devolvida pelo ONLYOFFICE também
contém SAVEDATE complexo. Isso não aprova os modelos oficiais. O editor mostrou
“All changes saved” antes de haver uma versão confirmada pelo host. A gravação foi
recusada; na reabertura apareceu aviso de recuperação de backup, ainda não salvo
no storage. Não se abriu o Gate D nem se executou E.

## Etapas e decisões

- A: autorização do plano cobre cópias privadas para ensaio; não publicação ou
  aceite institucional. Congelados 42 arquivos históricos/originais/controles.
- B: adaptador por hash/version/instruções exatas, preservação de partes não
  tocadas e testes RED→GREEN. Word reconhece tipo 22. A primeira fixture não tinha
  propriedade last-save e retornava zeros antes da impressão; uma fixture nova
  com core properties confirmou a mesma data válida antes/depois da transformação.
- C: porta `CandidateGate` antes do commit, script independente, quarentena
  privada limitada e HTTP 500 sem versão de sucesso. CAS/locks permanecem antes
  da política. A política não aprendeu exceções a partir da saída do editor.
- D/E: NÃO EXECUTADOS por parada C; nenhum DOCX oficial normalizado nesta rodada.
- F: decisão e proposta de transição publicadas; sem merge/deploy/envio externo.

Escolhas operacionais: continuar no checkout do laboratório com branch derivada
para preservar caminhos/mounts; usar ledger A–F privado e conservar `.runtime`.
O custo é isolamento por branch e bookkeeping distinto do helper de skills;
os gates e a retenção científica do plano permanecem obrigatórios.

## Interfaces e limites

B produz bytes de trabalho e manifesto privado; C entrega os bytes por WOPI e
avalia uma candidata independente antes de alterar o ponteiro. Uma transformação
permitida na entrada não autoriza reparação depois do save. O modelo original,
o working hash, a versão WOPI e a candidata rejeitada são objetos distintos.

O validador é conservador: mudanças de layout/partes que não consegue certificar
recebem `UNVERIFIED_FIELDS_OR_LAYOUT`. **Recusa não demonstra corrupção** dessas
mudanças. Não foi obtido um save real do motor aprovado por essa política.
Uma gravação positiva fabricada por roteiro independente passa nos testes HTTP;
isso não substitui aprovação do round-trip do editor.

Execução/reprodução: [protocolo](../../experiments/b05-document-engine/scripts/adapter_protocol.md).
Resultados e matriz: [decisão](../../experiments/b05-document-engine/results/b05-adapter-final-decision.md).
