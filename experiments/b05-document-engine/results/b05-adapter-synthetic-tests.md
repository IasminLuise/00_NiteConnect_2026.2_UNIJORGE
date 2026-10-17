# B05.0.2 B — transformação sintética

Adaptador `b0502-savedate-1`: hash/version e declaração exata de cada campo simples;
conversão pré-editor para begin/instrText/separate/cache/end. Cache/run properties,
switches e flags suportadas preservados. Sem reparo após save.

38 testes Python aprovados, sendo 13 novos testes do contrato. Os testes observaram
RED antes da implementação e antes das correções de campo aninhado, TargetMode
desconhecido e referências OPC pendentes. Cobrem múltiplas instâncias/header/footer, no-op complexo/outros tipos,
preservação de partes, idempotência/determinismo, XML/ZIP, entidades, macro e limites.
Os 25 testes anteriores permanecem incluídos, não somente citados como históricos.

Word reconheceu tipo 22 nas variantes. A primeira fixture, herdada de B05.0.1,
não tinha core properties: a atualização antes da impressão devolvia data zero,
enquanto a impressão atualizava a apresentação. Isso **não** comprovava data de
último salvamento válida. Foi criado um novo controle fonte-livre com propriedade
de salvamento conhecida. Original e normalizado retornaram a mesma data válida,
uma página, sem erro de campo. Não se alterou o adaptador para hardcodar datas.

A candidata rejeitada pelo host da primeira fixture também continha SAVEDATE
complexo e era reconhecida no Word, mas não corrige a limitação de origem sem core.
Nenhum desses controles constitui aprovação do AV3 real ou do relatório.

A candidata com core foi reaberta no Word: SAVEDATE tipo 22, data válida atualizada,
corpo XYZ e uma página. Foram seis probes/exportações Word nesta rodada, todos
sintéticos. No controle com core, a atualização explícita antes da exportação
mostra a data conhecida; na exportação PDF o Word reapresenta data atual nos dois
arquivos, com PNGs idênticos entre original/normalizado. Não se certifica cálculo
de data no browser a partir disso, nem se compara timestamp dinâmico como texto fixo.
