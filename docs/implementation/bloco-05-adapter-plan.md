# NITE CONNECT — Plano de execução B05.0.2

**Mitigação controlada de `SAVEDATE`, validação de salvamento e decisão de integração do editor web**
**Estado:** planejamento para implementação experimental; nenhum motor aprovado por este documento
**Data de referência:** 17/10/2026

> **Decisão de produto a respeitar:** a B05.0.2 é a última tentativa **delimitada** de uma mitigação própria, testável e não silenciosa para os defeitos comprovados. O primeiro candidato é ONLYOFFICE Community 10.0.0.1 com um adaptador *pré-edição*, limitado aos documentos explicitamente homologados. Não significa que a mitigação já funcionou nem que a arquitetura esteja escolhida. Se falhar, acionar uma decisão expressa entre revisão formal dos modelos e arquitetura Microsoft 365/SharePoint, sem ficar aguardando indefinidamente fornecedores.

---

## 1. Resultado esperado e pergunta decisória

**Pergunta:** é possível manter o modelo oficial original imutável, preparar uma cópia DOCX de trabalho semanticamente equivalente, abrir/editar/salvar via ONLYOFFICE/WOPI e aceitar a nova versão **somente** quando o modelo AV3, o relatório e seus elementos relevantes continuarem corretos no Microsoft Word e na exportação PDF?

O experimento precisa entregar:

1. Um adaptador OOXML determinístico, **limitado à transformação de um `SAVEDATE` simples conhecido para sua representação complexa equivalente**, sem reparos gerais de documentos.
2. Um validador independente dos bytes devolvidos pelo editor, com política *fail closed* e indicação clara do motivo de rejeição.
3. Um fluxo de persistência experimental que não substitua a versão atual antes de validar a candidata e que preserve o original e as versões já confirmadas.
4. Ensaios completos, repetíveis e comparáveis para os **dois modelos oficiais**, com referência no Word, além de regressões sintéticas.
5. Uma decisão arquitetural conclusiva: **mitigação aprovada para prosseguir aos gates seguintes**, **mitigação rejeitada**, ou **resultado inconclusivo com bloqueio mantido**. Nenhum desses estados equivale automaticamente a homologação institucional.

**Fora do escopo:** implementar `apps/api/documents`, `journal`, Flyway V6, `apps/web`, storage institucional, autenticação Entra de produção, licenças/contratações, deploy, envio de chamados ou alteração permanente de um modelo oficial. Não resolver a falha de paginação do Collabora nesta rodada: esse fornecedor permanece como comparativo/alternativa futura, com seus próprios bloqueios documentados. Não criar um editor próprio nem um fork permanente do ONLYOFFICE.

---

## 2. Fonte de verdade e evidência de partida

### 2.1 Documentos obrigatórios de leitura

Consultar na branch que contém os PRs empilhados:

- `AGENTS.md` e `apps/api/AGENTS.md` — limites de produto, segurança e método.
- `docs/product/NITE_Connect_Especificacao_Funcional_Consolidada_v1.0.pdf` — RF-034–039, RF-052, critérios de documento/Diário, sem inventar regras institucionais.
- `docs/implementation/bloco-05.md` — G1, G2, decisões D01–D26, escopo funcional ainda pendente.
- `docs/implementation/bloco-05-compatibility-plan.md` — protocolo da rodada B05.0.1.
- `experiments/b05-document-engine/ADR-B05-001.md` — evidências B05.0 e adendo B05.0.1.
- `experiments/b05-document-engine/results/2026-10-17-fidelity-decision.md` — baseline oficial.
- `experiments/b05-document-engine/results/b05-compatibility-final-decision.md` — resultado final da B05.0.1.
- `experiments/b05-document-engine/results/b05-savedate-diagnosis.md` — `SAVEDATE` simples/complexo.
- `experiments/b05-document-engine/results/b05-collabora-pagination.md` — por que o Collabora não é o candidato primário.
- `experiments/b05-document-engine/results/b05-microsoft-feasibility.md` — condições e limites da rota Microsoft.
- `experiments/b05-document-engine/scripts/FIDELITY-B0501.md`, `compatibility.py`, `compatibility_cases.py`, `fidelity.py` e os runners WOPI.

### 2.2 Baseline imutável

| Item | Observação confirmada em B05.0/B05.0.1 | Interpretação obrigatória |
|---|---|---|
| ONLYOFFICE 10.0.0.1 | `SAVEDATE` simples passa a `REF` já na representação importada | Alterar somente o DOCX **depois** do PutFile é insuficiente como estratégia principal |
| ONLYOFFICE sintético | `SAVEDATE` complexo preservou instrução e atualização válida no Word | É uma hipótese de mitigação **apenas** para a fixture; não está aprovada no AV3 |
| Relatório original | Controle Word: 7 páginas; ONLYOFFICE: 7 nas condições ensaiadas | Igualdade de páginas não prova fidelidade visual completa |
| AV3 original | Controle Word: 5 páginas; erro de campo após salvar no ONLYOFFICE | A integridade OOXML e a reabertura no Word são mandatórias |
| Collabora | Máscara `SAVEDATE` inválida e redução 12→10 pt associada à paginação | Manter NO-GO; não reaproveitar “correções” diagnósticas |
| WOPI host da spike | PutFile, versões imutáveis, capacidades, locks e CAS demonstrados | A infraestrutura experimental **não** é o host institucional definitivo |

Referências técnicas congeladas para reprodução: PR [#9](https://github.com/IasminLuise/00_NiteConnect_2026.2_UNIJORGE/pull/9) sobre `main`, PR [#10](https://github.com/IasminLuise/00_NiteConnect_2026.2_UNIJORGE/pull/10) sobre a branch de #9; commit final de #10 `ac60d148ace51784aecfc7d648d2e95ffc524f8b`. Conferir o estado novamente **no início** da execução; não presumir que continuam abertos. A CI anterior teve 28 testes Java de laboratório e 25 Python, mas não aprovou a fidelidade dos modelos.

---

## 3. Política de implementação e branches

1. **Se os PRs #9 e #10 ainda estiverem abertos:** criar `spike/b05-adaptador-docx` a partir de `spike/b05-compatibilidade-docx`, com PR empilhado apontando para essa base. Não abrir PR diretamente contra `main` incluindo de novo milhares de linhas de #9/#10.
2. **Se #9 e #10 já estiverem integrados:** atualizar `main`, conferir o merge e criar a nova branch a partir da base integrada.
3. **Se apenas #9 estiver integrado:** alinhar/revisar primeiro a base de #10, preservando seus resultados, antes da nova branch.
4. Somente alterar `experiments/b05-document-engine/**`, documentação pertinente a B05.0.2 e eventual CI específica da spike. Qualquer alteração em `apps/api/**`, Flyway, Cloudflare, Neon, `apps/web/**`, contratos de B01–B04 ou infraestrutura institucional exige autorização nova, fora desta execução.
5. Não modificar retroativamente resultados dos PRs anteriores: registrar **adendos** e novas evidências com referência às versões de origem.
6. Nenhum merge, deploy, compra, alteração de `.env`, credenciais ou envio a terceiros está autorizado por este planejamento.

**Privacidade:** modelos preenchidos, documentos oficiais, XML extraído, hashes identificáveis, DOCX intermediários, capturas de tela, PDFs, traces, logs contendo tokens e resultados brutos permanecem em `.runtime/` ignorado pelo Git. Publicar somente fixtures sintéticas e relatórios agregados revisados. Não incorporar nem distribuir fontes proprietárias.

---

## 4. Arquitetura do experimento

```text
Modelo oficial original (imutável, com hash e versão)
     |
     | autorização de uso de cópia no ensaio
     v
Preparador DOCX — somente cópia de trabalho
  - reconhece modelo/campo elegível
  - valida pacote e preserva partes não afetadas
  - transforma fldSimple SAVEDATE → campo complexo equivalente
  - gera manifesto de proveniência e alterações permitidas
     |
     v
Controle pré-editor (OOXML + Word C0 + PDF de referência)
     | falha => descartar a variante, manter original
     v
Dataset de trabalho WOPI, separado do original
     |
     v
ONLYOFFICE fixado por digest (edição no navegador)
     |
     v
PutFile -> candidata em estágio temporário
  - validar pacote, versão base, locks e capacidades
  - analisar campos, conteúdo, relações, mídia e propriedades
  - rejeitar regressões conhecidas ou modificações inesperadas
     |
     +---- FALHA -> versão corrente inalterada; erro explícito; evidência privada
     |
     v
Commit atômico da versão experimental validada
     |
     v
Reabertura Word + exportação PDF + inspeção do fluxo C0/C1/C2
     |
     v
Relatório final + decisão G1-F (sem promoção automática a G1/G2)
```

### 4.1 Fronteiras de responsabilidade

- **Original:** permanece imutável, com identificação e hash; nunca é substituído pela variante normalizada.
- **Preparador:** transforma apenas uma cópia elegível; deve ser uma transformação OOXML documentada, idempotente e explícita, não um filtro arbitrário aplicado em todo arquivo recebido.
- **WOPI host experimental:** entrega os bytes de trabalho, revalida autorização/conflito e só confirma uma versão quando o validador aprova. Não é o backend NITE real.
- **Validador:** determina integridade com base em invariantes e no protocolo do ensaio; não infere intenção do usuário comparando ingenuamente o DOCX salvo com o original.
- **Word de controle:** verifica que o campo permanece válido após atualização e que o layout/semântica dos modelos são aceitáveis. A renderização de Word depende da versão e do ambiente congelados.
- **Resultado:** a aprovação é restrita ao corpus, versões e operações demonstradas; não significa compatibilidade universal de DOCX.

### 4.2 Versionamento/proveniência que o ensaio deve provar

Registrar em manifesto **privado**: `original_id/hash`, `template_version`, `working_id/hash`, `adapter_version`, `transform_rules`, `source_version`, `editor_image_digest`, `save_candidate_hash`, `validated_version/hash`, motivo e instante. O original e a cópia de trabalho **não devem compartilhar ambiguamente o mesmo version_id**. O PDF de saída sempre se relaciona a uma versão imutável exata. A distinção entre *documento original*, *variante para edição* e *versão retornada pelo motor* deve ser visível no relatório de auditoria.

---

## 5. Contrato exato do adaptador pré-edição

### 5.1 Elegibilidade (deny by default)

Processar **somente** cópias de modelos previamente identificados por versão/hash em manifesto privado, após autorização para testar a transformação. Distinguir ao menos:

- `SAVEDATE` em `w:fldSimple` com a instrução esperada e a estrutura filha explicitamente suportada;
- `SAVEDATE` já complexo, que **não deve ser alterado**;
- documentos sem `SAVEDATE`, que devem sair idênticos ou ser ignorados;
- campos `REF`, `DATE`, `TIME` e outros tipos, que **nunca** devem ser convertidos por heurística;
- campos aninhados, protegidos, conteúdo misto, MCE/AlternateContent, partições inesperadas ou relações não suportadas: **abortar** e registrar motivo, sem aproximar ou descartar informação.

### 5.2 Transformação OOXML controlada

Para um `w:fldSimple` elegível, criar sequência OOXML complexa válida preservando a instrução `SAVEDATE` e seus switches, o resultado/cache, os atributos de bloqueio aplicáveis, as propriedades de runs, espaçamento e o posicionamento no story (inclusive cabeçalhos). Tratar `xml:space="preserve"` quando necessário e manter ordenação/contêiner compatíveis. Não hardcodar data, remover switches, achatar campo em texto ou inventar outro valor.

O implementador deverá usar parser XML com namespaces e política de pacote OPC explícita; evitar `regex` para reescrever indiscriminadamente XML de produção. Não sobrescrever elementos desconhecidos: falhar quando a estrutura não puder ser reproduzida fielmente. Uma implementação inicial pode aceitar **uma única estrutura conhecida**, desde que o escopo seja declarado.

**Preservação de pacote:** manter inalterados todos os bytes de partes que não precisem ser reserializadas, em especial `word/media/*`, relações, headers/footers não afetados e configurações. Reempacotamento ZIP altera hash global e pode alterar metadados do contêiner; a igualdade exigida é de conteúdo relevante de partes e semântica, não de hash de ZIP idêntico. O inventário/manifesto deve discriminar exatamente as partes tocadas, com diff canônico e hashes. Verificar referências, `[Content_Types].xml` e relationships; não reordenar/remover seções ou âncoras.

### 5.3 Propriedades indispensáveis

- **Idempotência:** normalizar duas vezes a mesma cópia não cria campos duplicados nem nova alteração.
- **Determinismo:** os bytes transformados das partes afetadas são reproduzíveis no mesmo ambiente e versão do adaptador, salvo metadados explicitamente conhecidos.
- **Não destrutividade:** o original não é modificado e pode ser recuperado a qualquer momento.
- **Equivalência:** Word reconhece `SAVEDATE` (tipo correto), a atualização não gera erro, e cache/data e formatação comportam-se conforme os controles; avaliar a semântica de data de salvamento, que é dinâmica.
- **Interoperabilidade:** ONLYOFFICE importa a variante como `SAVEDATE`, não a transforma em `REF` no estado carregado, e a mantém após editar/salvar/reabrir.
- **Falha segura:** se a variante for incompatível ou inconclusiva, nenhum documento é promovido silenciosamente.

**Importante:** o sucesso da fixture complexa em B05.0.1 **não** prova equivalência da variante complexa produzida para o AV3 real; isso será testado, não presumido.

---

## 6. Contrato do validador pós-edição e salvamento

### 6.1 Ordem da avaliação

1. **Entrada:** validar extensão/tipo, ZIP/central directory, relações OPC, tamanho expandido, macros/conteúdo ativo segundo a política do laboratório, XML seguro sem entidades externas; preservar limites do host existente.
2. **Capacidade/conflito:** autorização WOPI, documento, sessão, versão base, timestamp/CAS e locks; não permitir *force overwrite* por ausência de versão.
3. **Campo crítico:** `SAVEDATE` continua reconhecível como tal, sem regressão para `REF`, sem máscara inválida, com instrução e regras de cache coerentes com a operação esperada.
4. **Estrutura:** tabelas, parágrafos, seções, cabeçalhos, rodapés, hyperlinks/relações, imagens/âncoras e propriedades não relacionadas à edição permanecem íntegros dentro dos critérios definidos.
5. **Mudança autorizada:** comparar somente com um *roteiro de edição independente* (por exemplo, inserir/remover `XYZ`, ou ` B1 B2 B3`) e controlar diferenças permitidas do próprio motor; nunca tratar a saída como seu próprio gabarito.
6. **Persistência:** gravar a candidata de maneira temporária/atômica e só atualizar o ponteiro da versão corrente em caso de aprovação; rejeições não podem destruir versões anteriores.
7. **Evidência Word:** para aceitação final do ensaio, reabrir a **versão exata salva** no Word, atualizar apenas campos locais permitidos, repaginar, exportar PDF e inspecionar; a análise OOXML automática não substitui esse controle.

### 6.2 Respostas e UX do editor

A recusa de um PutFile precisa ser **observável e compreensível**: não devolver `200` caso a gravação não tenha sido aceita. Definir e testar resposta WOPI compatível com o motor para erro de integridade e conflito; o formato exato do retorno será validado contra o protocolo/cliente utilizado, não inventado. Verificar como o ONLYOFFICE apresenta falha de salvamento e se conserva os dados não persistidos durante a recuperação.

Ao falhar, preservar a última versão aprovada e registrar o hash/razão da candidata rejeitada em armazenamento **privado de laboratório** com retenção controlada, nunca em download público automático. Testar fluxo de reabertura, aviso ao usuário, recuperação segura da tentativa e ausência de perda silenciosa. Se o editor ignorar o erro e transmitir “salvo”, o fluxo **não está aprovado** mesmo que o backend preserve o histórico.

### 6.3 Atenção a falsas garantias

- `SAVEDATE` preservado **não** garante que todos os outros campos continuam válidos.
- Texto do corpo preservado **não** garante layout/cabeçalho/rodapé.
- Página 7/7 e 5/5 **não** comprova identidade visual.
- PDF renderizado pelo próprio ONLYOFFICE pode exibir cache que esconde campo corrompido.
- A correção do AV3 **não** comprova compatibilidade universal com DOCX.
- Validar um PutFile não prova que o motor não tenha corrompido o conteúdo antes da primeira gravação; por isso o C0 após normalização é obrigatório.

---

## 7. Etapas de execução (com pontos de parada)

| Etapa | Nome | Saída verificável | Avança quando |
|---|---|---|---|
| A | Preparação/segurança e baseline | Branch, manifestos e corpus congelados | Evidências e permissões conferidas |
| B | Adaptador sintético | Transformação `fldSimple`→complexo + testes | Word e inventário passam sem perda |
| C | Integração de laboratório | Pré-edição WOPI + validador + estados de falha | Não há confirmação falsa nem sobrescrita |
| D | Ensaios oficiais controlados | C0/C1/C2 dos dois modelos | Ambos passam campo, corpo, layout e Word |
| E | Validação operacional focal | Token, conflito, concorrência, falhas e recuperação impactados | Contrato de save confiável |
| F | Decisão final e transição | ADR, matriz, alternativa, proposta de integração | Gates documentados e decisão expressa |

### B05.0.2-A — Congelar o ambiente e autorizar o uso de cópias

**Implementar:** verificar PRs #9/#10, `git status`, hashes dos originais/capturas, documentos autorizados, digests CODE/ONLYOFFICE e versão Word; registrar base nova e versões de scripts. Reaproveitar fixtures existentes sem apagá-las. Confirmar autorização para tratar cópias dos **modelos oficiais** na nova rodada; autorização histórica de leitura não implica publicação. Criar dataset novo sob `.runtime/b0502/<rodada>`.

**Aceite A:** nenhum arquivo histórico ou original alterado; todos os dados privados excluídos de `git status`/diff; documentação de origem e controles Word identificados. Não iniciar normalização oficial sem autorização de teste correspondente.

### B05.0.2-B — Implementar e provar a transformação em casos sintéticos

**Implementar:** módulo isolado (sugestão `scripts/docx_adapter.py`) com API explícita de `eligible`, `normalize_copy`, `manifest`, `verify_idempotence`; testes (`scripts/test_docx_adapter.py`). Reutilizar o inventário `compatibility.py` e fixtures de `compatibility_cases.py` onde possível; não duplicar um segundo parser amplo sem necessidade.

**Testar antes de integração WOPI:** casos de campo simples/complexo, cache, idioma, switches, cabeçalho, múltiplos campos, ausência de campo, atributos/estrutura desconhecida, XML inválido, relações externas, ZIP anômalo e pacotes grandes. Registrar RED→GREEN dos testes que demonstram a falha e sua mitigação. Abrir as variantes válidas no Word, atualizar campos locais e conferir resultado; comparar pacote original e variante.

**Gate B — parar** se perder qualquer propriedade crítica, alterar um campo não elegível, gerar erro Word ou se a transformação não for idempotente.

### B05.0.2-C — Integrar no **laboratório**, com validação pré-commit

**Implementar:** preparar variante fora do host real; registrar origem e variant_id; entregar a cópia pela rota WOPI experimental. Acrescentar validação por uma **porta/estratégia do laboratório** antes de confirmar `PutFile` (não modificar API principal). Não criar mecanismo de “reparação retroativa” que reconstrua `SAVEDATE` a partir de `REF` já salvo. Não regravar o original.

**Cobrir:** upload da variante, C0 aberto sem salvar, salvar e reabrir, semântica de conflito WOPI, modo somente leitura, revogação, falha de disco, confirmação de salvamento, candidata rejeitada, recuperação/aviso na interface. Caso a alteração no host Java experimental seja necessária, executar novamente Maven e testes Java; se não necessária, justificar a instrumentação externa escolhida.

**Gate C — parar** se um erro de integridade for apresentado como salvamento concluído, se a versão corrente avançar após rejeição, se não houver recuperação segura ou se o ONLYOFFICE já converter a variante normalizada em `REF` ao importá-la.

### B05.0.2-D — Provar compatibilidade nos dois documentos oficiais

Aplicar o mesmo protocolo de B05.0.1 a **cópias independentes** de ambos os modelos, comparadas com controles Word equivalentes. Usar somente versões imutáveis capturadas do host; fechar e reabrir entre ciclos. Não reutilizar PDF de outro checkpoint ou dataset.

**Sequência obrigatória, em duas rodadas independentes se os ensaios focais passarem:**

- **C0-original:** exportar o modelo original no Word, registrar campos, paginação e layout.
- **C0-normalizado:** abrir a variante no Word **antes** do ONLYOFFICE, atualizar campos localmente e confirmar compatibilidade e layout. Abrir no ONLYOFFICE sem salvar e conferir visualização/importação e integridade do arquivo no host.
- **C1:** inserir `XYZ`, salvar/confirmar versão 1, fechar/reabrir, remover `XYZ`, salvar/confirmar versão 2, reabrir os bytes da versão 2 no Word. O corpo esperado ao fim é o original; diferenças de campo dinâmica devem ser justificadas.
- **C2:** realizar três preenchimentos curtos equivalentes (` B1 B2 B3`) no controle Word e no editor, com três saves/versões e reaberturas. Conferir todos os checkpoints e saída final.

**Relatório semanal:** controle Word original 7 páginas; verificar paginação final equivalente e inspeção página a página de tabelas, fontes, bordas, quebras, hyperlinks e imagem. A contagem de páginas não basta.

**AV3:** controle Word original 5 páginas; campo `SAVEDATE` deve continuar funcional e sem erro no Word; conferir cabeçalho, logotipo, tabelas, campos, cache, impressão e exportação. O preenchimento pode mudar paginação legitimamente; comparar **ao controle com o mesmo preenchimento**, não forçar cinco páginas em todos os cenários.

Registrar diferenças em: conteúdo (inclusive whitespace conhecido), campos, seções, estilos, fontes, imagens/âncoras, links, layout e comportamento de salvamento. Fazer inspeção editorial explícita, sem inventar aceite por ausência de falha automatizada.

**Gate D — parar** ao encontrar perda de semântica, transformação do `SAVEDATE` em `REF`, erro ao atualizar no Word, regressão visual relevante não autorizada, perda de conteúdo, divergência entre versões ou falha não recuperável. Sem solução demonstrada, não ficar repetindo configurações antigas.

### B05.0.2-E — Confiabilidade operacional focal

Somente após passar a etapa D, verificar os controles que a nova arquitetura **afeta**: token/documento incorreto, expiração e revogação durante sessão, leitura sem escrita, edição concorrente/conflito de versão, falha de `PutFile`, salvamento rejeitado, recuperação após restart e preservação dos artefatos originais. Avaliar o comportamento de erro e recuperação no navegador, não apenas HTTP. Não afirmar aprovação completa de P01–P20 com base neste ensaio: TLS/WSS, escala, egress, proof keys, backup/storage e licenciamento ainda exigem plano e aceite de G1.

**Gate E — parar** se a proteção de versões exigir aceitar corrupções ou se a UX esconder uma falha que arrisque perder dados.

### B05.0.2-F — Decisão, documentação e arquitetura de destino

Produzir `results/b05-adapter-final-decision.md` e **novo adendo**, não reescrita, em `ADR-B05-001.md`. Classificar:

- **GO-F condicional:** ambos os modelos e cenários passam com a variante autorizada, sem falhas críticas conhecidas, e o responsável aprova expressamente a representação de edição. O motor passa a ser **candidato validado para esse corpus**, não editor universal; G1 ainda precisa encerrar segurança, operação, suporte e licença, e G2 continua exigindo D01–D26/contratos/storage.
- **NO-GO-F:** regressão de semântica, campo ou layout não resolvida, falha de salvamento ou recuperação, ou variante não autorizada. Não adotar adaptador nem editor nos modelos oficiais.
- **INCONCLUSIVO:** não houve arquivo/autorização/aceite ou evidência suficiente. Manter bloqueio, explicitar exatamente o impedimento, sem falsos PASS.

A etapa termina quando a decisão e sua consequência estão registradas; **não exige continuar experimentando até produzir um GO artificial**.

---

## 8. Matriz focal de testes e critérios

| ID | Verificação | Resultado obrigatório |
|---|---|---|
| A01 | Modelo original e controles congelados por hash | Sem modificação e sem publicação |
| A02 | Instrução `SAVEDATE` simples reconhecida corretamente | Elegível só quando corresponde ao contrato |
| A03 | `SAVEDATE` complexo existente | Mantido, sem duplicar begin/end |
| A04 | Campo `REF` ou outro | Nunca reescrito por aproximação |
| A05 | Campo não suportado/aninhado/protegido | Falha controlada; original preservado |
| A06 | Múltiplas instâncias em header/footer | Apenas partes elegíveis transformadas, sem perdas |
| A07 | Cache, switches, atributos, idioma e formatação | Preservados/conferidos no Word |
| A08 | Partes não afetadas, mídia e relações | Sem alteração indevida; hashes por parte |
| A09 | Repetição do normalizador | Idempotente e determinística |
| A10 | ZIP/OOXML malformado, macro, expansão, XML DTD | Recusado sem exaustão nem escrita parcial |
| A11 | C0-normalizado antes do editor | Word reconhece campo, sem erro e sem regressão visual |
| A12 | Importação ONLYOFFICE sem save | Campo permanece SAVEDATE; origem WOPI intacta |
| A13 | C1 sintético inserir/salvar/remover/salvar | Duas versões, corpo final original, campo válido |
| A14 | C2 sintético três preenchimentos | Três versões, conteúdo e campo válidos |
| A15 | PutFile com `SAVEDATE→REF` proposital | Rejeitado; versão atual não avança |
| A16 | PutFile com perda de parte ou imagem | Rejeitado; versão antiga recuperável |
| A17 | Save válido com edição esperada | Confirmado uma vez; versão exata e hash rastreáveis |
| A18 | Save concorrente ou versão-base antiga | Conflito; nenhum overwrite silencioso |
| A19 | Token revogado/escopo diferente/somente leitura | Escrita negada conforme contrato |
| A20 | Falha de storage ou timeout durante save | Documento íntegro; UX não afirma sucesso |
| A21 | Erro de validação visto no editor | Mensagem/falha observável e recuperação definida |
| A22 | AV3 C0 original × normalizado | 5 páginas de referência original; campo funcional e layout conferido |
| A23 | AV3 C1/C2 | Campo válido no Word, corpo/estrutura e PDF coerentes |
| A24 | Relatório C0 original × normalizado | 7 páginas de controle, inspeção tipográfica e visual |
| A25 | Relatório C1/C2 | Mesma intervenção Word/editor, sem regressão não autorizada |
| A26 | Duas rodadas independentes finais | Mesmo veredito, sem reutilização de checkpoints |
| A27 | Integridade de bytes e proveniência da versão | Manifesto completo, PDF associado à versão exata |
| A28 | Regressão dos scripts anteriores | Testes Python/Java aplicáveis verdes na CI |
| A29 | Revisão de segurança e dados privados | Nenhuma evidência pessoal, token ou fonte no Git |
| A30 | Aceite editorial dos responsáveis | Registrado explicitamente, ou status PENDENTE |
| A31 | G1-F / G1 / G2 separados | Sem promoção automática a produto |
| A32 | Caminho alternativo se NO-GO | Decisão registrada, não protelada |

**Interpretação:** `PASS` exige evidência reproduzível da execução; `FAIL` descreve falha observada; `PENDENTE` depende de autorização/aceite/outro gate; `NÃO EXECUTADO` não se converte em aprovação. Não declarar porcentagem de fidelidade sem métrica autorizada. Registros de screenshots/Word oficiais permanecem privados.

---

## 9. Gatilhos decisórios, rollback e alternativas

### 9.1 Se a mitigação funcionar

1. Registrar **GO-F condicional** para ONLYOFFICE + variante normalizada apenas nos modelos/hashes aprovados, com limites conhecidos.
2. Documentar claramente que a estratégia tem dependência de formatos específicos de `SAVEDATE` e da versão fixada do motor, com regressão mandatória em upgrades e em cada novo modelo.
3. Encerrar pendências **G1 operacional**: segurança WOPI real, proof keys conforme fornecedor, isolamento/TLS/WSS, revogação durante coedição, armazenamento durável, backup, carga e licenciamento/suporte de produção.
4. Submeter decisões **G2** (D01–D26, matriz de acesso, modelos, contexto, Diário, storage, contratos) aos responsáveis.
5. **Somente após aprovações formais**, abrir um plano/PR distinto de integração do módulo `documents` no backend Java 21/Spring Boot/PostgreSQL, com versão/histórico e adapter versionados, seguido das etapas do Diário; não copiar endpoints `/lab` sem autorização real.
6. Na API definitiva, usar uma interface limitada `DocumentCompatibilityAdapter` (nome ilustrativo), específica para modelos aprovados, e persistir a proveniência, a estratégia usada, as revisões autorizadas e as falhas auditáveis. Se o editor for atualizado ou o modelo mudar, revogar a aprovação anterior até nova regressão.

### 9.2 Se não funcionar: alternativa 1 — revisão **formal** dos modelos

Propor à autoridade responsável versões **novas** dos modelos oficiais, preservando os originais e justificando substituições de campos/documentação. Exigir análise de regras acadêmicas, aceite editorial, Word e ONLYOFFICE, e rastreabilidade da versão publicada. Não reformatar ou substituir o AV3 unilateralmente. A adaptação de modelo é **mudança institucional de artefato**, não solução técnica silenciosa.

### 9.3 Se não funcionar: alternativa 2 — arquitetura Microsoft 365/SharePoint

Solicitar confirmação institucional de tenant, licenças, administrador, permissões de aplicativo, política de dados/armazenamento/retenção e autorização de PoC sintética. Testar catálogo NITE + arquivos efetivamente armazenados no SharePoint/OneDrive, edição pelo Word web e leitura/versões/Graph, sem prometer iframe ou WOPI Microsoft sobre storage privado. O CSPP não decorre de possuir login Entra ou licença Microsoft 365. Não migrar arquivos reais nem solicitar consentimento amplo sem aprovação. Se o armazenamento tiver de permanecer próprio do NITE, a viabilidade dessa alternativa deve ser reavaliada formalmente.

### 9.4 Ponto de parada e rollback

- Limitar a implementação à regra `SAVEDATE` simples→complexo e às verificações de integridade necessárias; não construir reescritor OOXML geral.
- **Uma tentativa implementada + correções motivadas por testes**; alterações adicionais ao modelo, motor, arquitetura ou fork exigem um novo experimento aprovado, com hipótese e limite explícitos.
- Se um gate falhar, preservar os artefatos, restaurar o dataset padrão da spike, desligar os serviços usados e **não apagar** `.runtime` ou versões históricas.
- Não adotar “reparo após save”, substituição textual de `REF` por `SAVEDATE`, achatamento de campos nem tratamento de erro que declare `200` após rejeição.
- Independente do resultado, preparar relatos fonte-livres para fornecedores se forem autorizados, sem condicionar a entrega à resposta deles.

---

## 10. Entregáveis e arquivos propostos

```text
docs/implementation/
  bloco-05-adapter-plan.md                  # cópia versionada deste plano, após aprovação
  bloco-05-adapter-execution.md             # execução, decisões e gates reais
experiments/b05-document-engine/
  ADR-B05-001.md                            # adendo B05.0.2, sem apagar B05.0/0.1
  README.md                                 # comandos novos somente se implementados
  scripts/
    docx_adapter.py                         # exemplo de módulo isolado do experimento
    test_docx_adapter.py
    adapter_protocol.md                     # execução/reprodução e limites
    ...                                     # reaproveitar compatibility*, fidelity* e runners
  results/
    b05-adapter-baseline.md
    b05-adapter-synthetic-tests.md
    b05-adapter-official-models.md
    b05-adapter-save-validation.md
    b05-adapter-final-decision.md
  .runtime/b0502/                            # dados privados e binários NÃO versionados
```

A implementação pode ajustar nomes após inspeção, desde que mantenha separação clara entre testes, resultados, validação, decisões e dados privados. Relatórios versionáveis não devem conter conteúdo identificável de documentos oficiais.

### Contrato do relatório final

Registrar commit/branch/digest; modelo e hashes privados; versão Word; versão do adaptador; transforms aceitos/rejeitados; C0/C1/C2 original/normalizado/editor/Word/PDF; matriz A01–A32; falhas, severidade, hipótese e evidência; auditoria de versões; parecer da revisão independente; CI executada; status de G1-F/G1/G2; indicação de aprovação editorial **real** ou pendente; recomendação e próximo PR. Não publicar PDFs/prints pessoais.

---

## 11. Verificações técnicas e CI

**Baseline a manter:** os 25 testes Python e 28 testes Java da CI específica de #10; 185 testes da API principal na `main` são baseline histórico, **não resultado desta rodada**. Reexecutar o conjunto afetado sempre que código correspondente mudar. Testes de documentos oficiais rodam somente no ambiente privado autorizado, nunca na CI pública.

Comandos de referência a partir da raiz (ajustar caso mudem caminhos):

```powershell
python -m unittest discover -s experiments/b05-document-engine/scripts -p 'test_*.py'
mvn --batch-mode -f experiments/b05-document-engine/wopi-host/pom.xml verify
node --check experiments/b05-document-engine/scripts/editor_cycles.cjs
node --check experiments/b05-document-engine/scripts/onlyoffice_cycles.cjs
node --check experiments/b05-document-engine/scripts/probe_onlyoffice_import.cjs
python experiments/b05-document-engine/scripts/generate_fixtures.py
git diff --check
git status --short
```

Ao adicionar ou alterar Java experimental, criar testes HTTP do erro PutFile, CAS e pointer imutável e repetir Maven. Ao modificar o runner, executar testes sintéticos em navegador real; `node --check` verifica somente sintaxe, não comportamento. A CI deve executar uma fixture sintética adaptada com expectativa positiva e outra com rejeição de regressão; esse teste não substitui Word/visual/aceite dos modelos oficiais.

**Critérios mínimos de integração da pesquisa:** testes aplicáveis aprovados, ausência de vazamento em diff/logs, revisão independente do escopo modificado, matriz preenchida sem falsos PASS, ADR atualizada e nenhum arquivo do produto alterado. O resultado pode ser NO-GO sem invalidar a qualidade do trabalho experimental.

---

## 12. Instrução executável para a equipe/agente

> Implementar **somente** B05.0.2 no laboratório documental do NITE CONNECT. Antes de alterar qualquer arquivo, ler `AGENTS.md`, `apps/api/AGENTS.md`, a especificação consolidada, os planos B05, relatórios finais B05.0/B05.0.1, ADR e protocolos; conferir estado dos PRs #9/#10 e criar branch/PR empilhado quando necessário. Preservar originais e `.runtime`. Construir um normalizador OOXML **pré-edição** limitado a `SAVEDATE` simples elegível numa cópia de trabalho, com manifesto e testes de idempotência/preservação. Validar a variante no Word antes de abrir no ONLYOFFICE. Implementar/ligar ao host experimental uma verificação pós-PutFile que rejeite alterações indevidas sem avançar a versão atual e sem comunicar sucesso falso. Executar primeiro fixtures sintéticas e somente depois, com autorização, C0/C1/C2 completos dos dois modelos oficiais e duas rodadas independentes. Rejeitar soluções baseadas apenas em PDFs com contagem igual de páginas. Registrar todos os resultados, limitações e status de G1-F/G1/G2; não alterar a API principal, Flyway, frontend, storage institucional nem modelos oficiais. Se a mitigação falhar, encerrar com NO-GO justificado e uma recomendação expressa de revisão formal dos modelos ou PoC Microsoft autorizada. Nunca criar GO sem evidências nem realizar merge/deploy/compra/envio a terceiros sem aprovação.

---

## 13. Critério final de encerramento da B05.0.2

A B05.0.2 estará **concluída como trabalho experimental** quando o código, testes, auditoria e documentação permitirem afirmar **uma destas duas decisões**:

- **Viabilidade comprovada no escopo:** variante autorizada preservou campo, corpo, layout e comportamento nos modelos oficiais e nas operações testadas, em execuções independentes; nenhuma falha crítica conhecida; seguir para aprovação operacional G1, decisões G2 e planejamento de backend.
- **Viabilidade não comprovada:** ao menos uma falha ou ausência de evidência impede liberar o editor; publicar NO-GO/INCONCLUSIVO e encaminhar formalmente uma das duas alternativas. **Não** avançar por inércia com V6 ou editor quebrado.

A conclusão de pesquisa é independente da aprovação de produção. **Nenhuma integração real pode ser declarada concluída a partir deste plano ou de CI sintética verde.**

### Referências internas

- [PR #9 — B05.0 laboratório](https://github.com/IasminLuise/00_NiteConnect_2026.2_UNIJORGE/pull/9)
- [PR #10 — B05.0.1 diagnóstico focal](https://github.com/IasminLuise/00_NiteConnect_2026.2_UNIJORGE/pull/10)
- `experiments/b05-document-engine/results/b05-compatibility-final-decision.md`
- `experiments/b05-document-engine/results/b05-savedate-diagnosis.md`
- `experiments/b05-document-engine/results/b05-collabora-pagination.md`
- `experiments/b05-document-engine/results/b05-microsoft-feasibility.md`
- `experiments/b05-document-engine/ADR-B05-001.md`

**Status final desta entrega:** apenas planejamento; B05.0.2 ainda não executada, G1-F reprovado nas versões atuais, G1 não aprovado e G2 fechado.
