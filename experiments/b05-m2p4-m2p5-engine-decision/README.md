# B05-M2P4 + B05-M2P5 — diagnóstico de round-trip e decisão de motor

**Duas perguntas, um patch, uma rodada coordenada.** Instrumentação **offline / somente leitura**, anexável ao NITE CONNECT, para trabalhar sobre as evidências reais produzidas pelo executor já existente. Não altera `apps/api`, editor, autorização, documento oficial, JAR, Flyway, gate, banco ou credenciais. Não cria sessão ONLYOFFICE nem executa Word automaticamente.

## Perguntas que o kit responde

| Fase | Pergunta | Saída |
|---|---|---|
| B05-M2P4 | O que efetivamente muda em cada etapa `source → SDK → callback → Word/PDF`, inclusive `no_edit`? | Comparação por pares de conteúdo, OPC, estilos, notas, campos pre-Word, raster de PDFs, repetição do PDF de origem, probes de glifos e inventário Word pré/pós-exportação |
| B05-M2P5 | O conjunto de evidências já sustenta adoção definitiva do ONLYOFFICE no backend? | Matriz `PASS_SCOPE_ONLY` / `UNPROVEN` / `FAIL`, bloqueios e recomendação consultiva `HOLD_DECISION_NOT_ENOUGH_EVIDENCE` ou `NO_GO_FOR_DEFINITIVE_ADOPTION` |

**Invariantes de segurança:** todo resultado contém `HOLD_QUARANTINE`, `canPromote=false`, `officialApproved=false`, `blocked_gates=[G1,G2,M3]`, `backend_final_integration_authorized=false`. Nenhuma função desse kit concede `GO`, cria allowlist/tolerância, corrige o OOXML ou promove documentos. Uma indicação de `NO_GO` é *recomendação técnica para adoção definitiva*, não rollback/deploy. Uma indicação de `HOLD` significa lacuna de evidência, **não** aprovação condicionada automática.

## Por que esta rodada existe

Relatório da execução real M2P3 de 24/10/2026:

- `RICH-01` reproduziu Arial Bold/FCFCFC a partir de HTML formatado; `TYPE-03` e `PLAIN-01` preservaram Cambria preta. Não há instrumentação histórica suficiente para atribuir o caso M2P2 ao motor isoladamente.
- `no_edit` também registrou **1.452 pixels** fora da região de edição: **109** na célula 2.3 e **1.343** no parágrafo 5 da página 2. Não descartar ou normalizar esses pixels.
- O DOCX SDK e o DOCX callback **não são idênticos**: foi observado `PAGE=2` contra `PAGE=1` e alteração de data em `docProps/core.xml`, antes do Word. O procedimento Word `SaveAs2` pode atualizar `SAVEDATE` e não demonstra igualdade histórica.
- Permanecem transformações de partes OPC, notas, estilos e instruções de campos não qualificadas. Nada disso foi automaticamente considerado equivalente.

O kit faz uma **triagem reprodutível** desses canais. Ele não identifica por si só o componente interno responsável pelo deslocamento de um glifo, não executa o processo de captura real e não substitui revisão da estrutura OOXML em profundidade nem homologação editorial.

## Instalação no repositório existente

O patch é **aditivo**. Branch esperada: `feat/b05-core-docs-api`. O relatório M2P3 indicava HEAD histórico `017281a1830724cd66eee6ee505a704e9fa37b4d` com alterações locais. Confirme o estado real antes da aplicação; não resete nem limpe `.git`, arquivos `untracked` ou evidências `.private`.

```powershell
# Execute a partir da raiz do repositório
 git branch --show-current
 git status --short
 git apply --check .\NITE_CONNECT_B05-M2P4_M2P5.patch
 git apply .\NITE_CONNECT_B05-M2P4_M2P5.patch

 cd experiments/b05-m2p4-m2p5-engine-decision
 py -3 -m pip install pymupdf numpy  # somente na venv privada da instrumentação
 py -3 -m unittest discover -s tests -v
 py -3 -m compileall -q b05_decision evaluate.py
```

O teste usa apenas dados inteiramente fictícios gerados em temporários, sem serviços externos.

### Estrutura de evidência real

Monte uma campanha nova, com arquivo `manifest.json` no diretório privado e pelo menos **quatro controles** (`no_edit`, `typed`, `plain_paste`, `rich_paste`). Use `examples/manifest.example.json`, substituindo todos os placeholders, e `examples/operator-log.example.json`. Os arquivos descritos no manifesto são relativos ao próprio manifesto, com SHA-256 reais. **Não reutilize o mesmo caminho físico** entre modalidades. Os hashes podem coincidir quando as origens independentes possuem os mesmos bytes.

Para cada controle preserve:

1. `source_docx`: origem byte a byte verificada no armazenamento real da API;
2. `sdk_docx`: candidato de `downloadAs('docx')` capturado **antes** do Word, inclusive no controle sem edição;
3. `callback_docx`: candidato persistido `QUARANTINED` no armazenamento da API. Para `no_edit`, declare `callback_status=not_created` se não houver nova versão; não invente candidato;
4. `source_pdf`, `sdk_pdf` e, quando houver callback, `callback_pdf`: exportações Word com mesmo build/procedimento, sem salvar DOCX. Repita o PDF da origem no controle `no_edit` como `source_pdf_repeat` para medir variabilidade do **próprio caminho de conversão**. A repetição precisa ser nova exportação, não uma segunda rasterização do mesmo PDF;
5. `word_inventory`: inventário de corpo/páginas/campos **antes e depois** de `SaveAs2`, por origem, SDK e callback quando presente, capturado com probe Word já existente, seguindo `examples/word-inventory.example.json`;
6. `operator_log`: modo declarado, eventos e tipo honesto de evidência (`attested`, `observed`, `instrumented`). Logs atestados não são promovidos a instrumentados por serem rotulados como colagem;
7. `probes`: regiões de comparação de glifos, em pontos PDF, escolhidas após inspeção manual, por exemplo a célula 2.3 e o parágrafo 5. A região **não mascara pixels**; apenas mede offsets e estilos de spans selecionados.

**Adquira os documentos no fluxo privado de M2P3**, sem alterar a configuração normal da API. Se for absolutamente necessário repetir o override temporário de `download`, mantenha exclusivamente o procedimento já autorizado para fixture não oficial, com validação de JWT, versão original, expiração e demais permissões. Este kit **não** concede autorização para essa alteração por si só e não contém segredo.

A instrumentação opcional em `tools/paste-metadata-listener.js` observa **somente metadados** de eventos `paste` na página em que é instalada com permissão; não coleta conteúdo do clipboard, não grava arquivos nem faz requisições. Eventos dentro de iframe *cross-origin* não são visíveis ao script externo. `clipboard_formats_offered` não comprova qual formato foi de fato inserido. Preserve provas de tela/edição externa quando possível e nunca classifique `attested` como `instrumented` sem evidência real.

### Execução única com duas respostas

```powershell
cd experiments/b05-m2p4-m2p5-engine-decision
py -3 evaluate.py --manifest .private/campaign-03/manifest.json --out-dir .private/campaign-03/qualification-01
```

Saídas **criadas somente se todas as entradas forem verificadas antes e depois**:

- `decision.json`: todas as comparações por par e critérios M2P4/M2P5, sem texto integral de documentos, hashes de conteúdo ou caminhos locais;
- `decision.md`: resumo da decisão e critérios, com DPI **dinâmico** do JSON (`environment.dpi`), não fixo no título.

O diretório de saída **não pode preexistir**. O programa rejeita entrada ausente, hash diferente, reuso de arquivo, manifesto incompleto, probe fora de página e alterações ocorridas durante a análise. Arquivos de entrada nunca são regravados. Se a execução falhar, corrija a **aquisição ou o manifesto**, nunca altere um documento para obter PASS.

## Limites da inferência e regra de encerramento

- `PASS_SCOPE_ONLY` vale somente para os pares e propriedades realmente inspecionados. Nunca representa homologação do motor, equivalência semântica de todos os recursos ou aprovação oficial.
- `UNPROVEN` cobre estilos normalizados, alterações de campos/nota, raster não idêntico, proveniência insuficiente e corpus não representativo. Nenhuma equivalência automática, tolerância de pixel, máscara ou allowlist é adicionada.
- `FAIL` cobre perda textual/mídia, divergência de páginas/tamanho, ou declaração de alteração de fonte/layout durante exportação Word; `NO_GO_FOR_DEFINITIVE_ADOPTION` exige análise humana do risco e não muda o produto.
- Para decisão final do backend será necessário ainda um **corpus oficial representativo aprovado**, delimitação dos tipos de documento e requisitos de fidelidade, verificação independente de campos/estilos/nota/OPC, testes reais de API/permissão/exportação e parecer institucional de aceite. O kit não tem como certificar estas evidências externas por uma chave JSON.
- O modo HTML conhecido é **suficiente para reproduzir** a alteração visual, mas não prova a origem histórica M2P2. A diferença no `no_edit` é **anterior à edição**, mas não revela isoladamente qual estágio interno a gerou.
- Compare SDK/callback **antes do Word** e mantenha seus respectivos artefatos. Um PDF igual pós `UpdateFields` não reverte valores históricos nos DOCX.

## Privacidade e checklist de encerramento

Use somente fixtures sintéticas para compartilhamento. Relatórios gerados omitem texto visível integral, conteúdo de campos, hash de entrada e caminhos absolutos, mas **IDs de sessão e versões são fornecidos pelo manifesto**: evite dados pessoais/segredos também nesses valores. Logs e DOCX/PDF ficam em `.private/`, ignorado pelo Git. Confirme que o manifesto completo e outputs com material sensível não serão anexados a commits. Verifique fechamento dos serviços de bancada no executor que realizou a aquisição; o kit offline não pode garanti-lo. Não fazer commit, merge, deploy ou promoção sem autorização explícita.

**Pequenas correções documentais da rodada M2P3:** execute os testes dentro da pasta (`cd ...; python -m unittest discover -s tests -v`). O DPI aparece no JSON e não deve ser fixado em textos para rodadas com valores distintos.
