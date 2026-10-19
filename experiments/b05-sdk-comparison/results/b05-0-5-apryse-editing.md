# B05.0.5 — edição Apryse

**INCONCLUSIVO — INCONCLUSIVE_AUTOMATION para o ciclo completo.** Inserção e
persistência estão comprovadas; reversão integral e fidelidade não estão.
Não classificar como incapacidade nativa de edição ou bloqueio comprovado de licença.

1. Sessão inspecionada no-op: OfficeEditor presente, readOnly=false e interface
   Editing. Trial embutido informa sete dias e marca d'água; nenhuma chave usada.
   Export contém o texto sintético original. Sem arquivo institucional.
2. Primeiro ciclo novo: `locator.click` encontra o canvas correto, mas seu scroll
   automático posiciona o alvo sob overlays/ribbon. Timeout de interação, sem
   export; excluído de qualquer veredito de perda de conteúdo.
3. Única correção de foco: usar a bounding box atual do canvas observado, clicar
   sem auto-scroll e enviar Ctrl+Home. Sem API que escreva no modelo do SDK.
   Segundo ciclo: inserir XYZ gera prefixo exato apenas no primeiro parágrafo
   do corpo; os demais textos do corpo são iguais. Word confirma o prefixo.
4. Candidata A reaberta em novo browser/instância; o screenshot de abertura e o
   DOCX de origem comprovam persistência. A sequência de três Delete gera B
   com prefixo residual **YZ**, também confirmado em OOXML e Word.

Não se aprovou a reversão por ausência da substring XYZ: isso seria um falso
positivo. A comparação exige restituir todo o texto do corpo. O problema pode
envolver temporização/processamento de teclas, mas não há controle independente
que atribua causa ao SDK. Não foram acrescentados delays/tentativas após atingir
o orçamento de dois ciclos novos. A correção de foco demonstrou progresso real,
não um ciclo XYZ→original completo ou confirmação em dois ciclos positivos.

As três candidatas efetivamente exportadas têm uma página no Word; SAVEDATE
(tipo22) e PAGE (tipo33) continuam campos atualizáveis, com diferenças de idioma/
formato. NUMPAGES não está na fixture básica desta rodada: **NOT_RUN novo**,
sem reutilizar como nova prova o no-op estendido B05.0.4. Espaçamento/tabelas
e alterações de trial continuam sob gate estrito, com **FAIL_STRUCTURAL**.
Partes/mídias de watermark adicionais variam entre exports; o conteúdo de um
header pode ser movido para outro part, portanto um header vazio isolado não
foi chamado de perda de campo. Não houve bypass de marca d'água.

APIs confirmadas nos tipos instalados 12.1.0: initialMode no construtor,
getOfficeEditor, isReadOnlyModeEnabled, getFileData(downloadType:office).
Referências: [configuração](https://sdk.apryse.com/api/web/global.html),
[salvamento](https://docs.apryse.com/web/open-save-document/save.md).
Licença limpa/add-on e direitos de uso institucional não foram obtidos nem
aprovados. Não há plano licenciado verificável que habilite modelos oficiais.

Gate D edição: **INCONCLUSIVE_AUTOMATION**; transporte e inserção: PASS focal;
integridade global: FAIL; direitos para próxima etapa: BLOCKED.

Evidências em `.private/b0505/`: `browser/apryse-inspected-noop`,
`browser/apryse-cycle-1-edit`, `browser/apryse-cycle-2-{edit,remove}`,
`apryse-cycle-proof.json`, manifests, `*-word.json`, PDFs e `render/`.

