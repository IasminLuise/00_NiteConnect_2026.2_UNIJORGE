# B05.0.5 — protocolo focal

O orçamento da rodada registrada foi consumido. Os comandos são reprodução
documentada, não autorização para rodadas adicionais nem para usar oficiais no
Apryse. Não sobrescrever tags, evidências ou arquivos existentes.

1. Conferir status/diff e head do PR13; criar branch derivada. Congelar originais
   e resultados antigos sob nova raiz privada, nunca publicar hashes de documentos.
2. Na raiz: `python -m unittest discover -s experiments/b05-sdk-comparison/tests -v`.
   No web: `npm ci --ignore-scripts --no-audit --no-fund`, `npm run vendor:apryse`,
   `npm run build`, `npm run dev`. Runtimes exatos no manifesto privado.
3. `node experiments/b05-sdk-comparison/tests/focal_diagnostics.cjs` e
   `node --check experiments/b05-sdk-comparison/scripts/browser_probe.cjs`.
4. Runner existente com parâmetros obrigatórios `--run <nova-raiz-privada>`,
   `--engine superdoc|apryse`, `--tag <unico>`, `--playwright <pacote-absoluto>`,
   `--chrome <executavel-absoluto>`, `--source <copia-dentro-do-run>`.
   Novo `--diagnostic-pagination true` captura contagem/eventos/DOM/tiles; novo
   `--diagnostic-focus true` captura modo/superfícies e habilita foco observado
   para as fases edit/remove. Sem flags, conserva o protocolo anterior.
5. Diagnósticos usam `/?b0505=1`, sem expor SDK na janela nem API de mutação.
   Paginação espera estabilidade de contagem/geometria/texto DOM ≥3s, limite30s;
   detalhes de cada página são coletados após materialização por scroll.
   Capturas top/bottom evitam tomar clipping do painel por conteúdo inexistente.
   Conteúdo virtualizado exige distinguir medição estável de certificação visual.
6. Bloqueio HTTP/WS e CSP loopback. Flags focais interrompem antes do export se
   observado request não autorizado; dados privados continuam ignorados. Não
   inserir chave em CLI, URL, logs ou repositório. Seis sessões novas nesta rodada.
7. Apryse: `--phase edit`, depois `--phase remove` em nova instância, source=A.
   O clique é relativo à bounding box do canvas observado em12.1.0, seguido de
   Ctrl+Home. Continua limitação geométrica da automação, não seletor semântico.
   Não repetir silenciosamente teclas ou usar API para forçar texto.
8. `python .../scripts/cycle_probe.py --source <original> --edited <A>
   --reverted <B> --out <privado-novo.json>` compara **todo** corpo por parágrafo.
   PrefixoXYZ ausente não basta; residualYZ falha. Saída zero do CLI significa
   análise executada, não PASS do ciclo ou de fidelidade.
9. `python .../scripts/notes_probe.py --before <original> --after <candidata>
   --out <privado-novo.json>` inventaria sem aprovar equivalência. Gate existente
   `ooxml_gate.py` permanece estrito, com apenas opt-ins B05.0.4 documentados.
10. `word_probe.ps1 -JobsFile <jobs-privado.json>` abre readonly, macros/links
    desabilitados, restaura opções. Jobs podem incluir `diagnostic_layout:true`
    para a primeira letra após controles; sem isso, saída anterior preservada.
    Campo SAVEDATE pode atualizar no PDF; não confundir relógio com quebra real.
11. Guardar logs, userAgent, Word build, locale, viewport/DPR, hashes, sessões e
    status em manifesto privado. Revalidar originais/histórico, parar somente
    o Vite iniciado pela rodada. Não apagar caches, exports ou capturas.

Nesta rodada: inspeção no-op Apryse → tentativa1 interceptada → único ajuste
de foco → tentativa2 inserção comprovada → reabertura e remoção parcial.
Encerramento obrigatório, sem terceiro ciclo. Ver matriz e limites nos relatórios.

