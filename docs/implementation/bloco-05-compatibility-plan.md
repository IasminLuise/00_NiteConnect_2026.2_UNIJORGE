# NITE CONNECT — Plano de execução B05.0.1

**Resolução focalizada de compatibilidade DOCX, validação de editores web e decisão arquitetural**
**Situação:** planejamento para execução; nenhum teste adicional executado por este documento
**Autoridade:** especificação funcional consolidada `docs/product/NITE_Connect_Especificacao_Funcional_Consolidada_v1.0.pdf`, instruções `AGENTS.md` e `apps/api/AGENTS.md`, decisões posteriores expressamente aprovadas; resultados originais do laboratório B05.0.
**Escopo aprovado para esta proposta:** investigar e decidir o motor documental; **não** implementar V6, `documents`, `journal`, novo frontend de produto, storage institucional, deploy ou homologação por inferência.

> **Princípio de trabalho:** o laboratório B05.0 está concluído como investigação inicial com decisão **NO-GO** para os dois modelos oficiais nas versões testadas. B05.0.1 é uma investigação nova e focalizada para resolver ou delimitar definitivamente as incompatibilidades. Teste verde de integração WOPI não significa fidelidade DOCX aprovada. Nenhuma hipótese é apresentada como correção demonstrada.

---

## 1. Objetivo, pergunta decisória e limites

### 1.1 Pergunta central

**Qual arquitetura permite ao NITE CONNECT oferecer uma experiência de biblioteca documental com edição DOCX integrada e versões controladas, preservando os dois modelos oficiais ao abrir, editar, salvar, reabrir no Microsoft Word e exportar PDF, sob requisitos operacionais e de segurança viáveis?**

A resposta pode ser: (a) Collabora Online aprovado mediante evidência; (b) ONLYOFFICE aprovado mediante evidência; (c) integração Microsoft 365/SharePoint por caminho contratual/institucional viável e confirmado; ou (d) **nenhum editor web aprovado**, com decisão explícita de não liberar edição web dos modelos até reformulação do requisito/fluxo por autoridade competente. A alternativa (d) é uma decisão definitiva de NO-GO para edição web neste estágio, **não** autorização silenciosa para prosseguir com o Bloco 05 completo.

### 1.2 Objetivos mensuráveis

- Isolar a transformação do campo `SAVEDATE` no ONLYOFFICE e Collabora, identificando o estágio de importação, representação, atualização ou serialização que altera a semântica.
- Identificar quais propriedades causam a diferença de paginação do relatório no Collabora, distinguindo **renderização do original** de **alteração persistida após salvar**.
- Avaliar correções reais: versões, patches documentados, configuração estritamente justificada ou solução tecnicamente segura **sem alterar silenciosamente modelos oficiais**.
- Reexecutar somente os cenários afetados e regressões de segurança/persistência diretamente impactadas pelas mudanças.
- Selecionar arquitetura documental com evidência verificável e registrar alternativa de contingência se os editores avaliados não forem aprovados.
- Manter o laboratório original reproduzível e preservar íntegros os dados e documentos oficiais fornecidos para ensaio.

### 1.3 Fora do escopo

- CRUD documental de produto, Diário de Bordo, Flyway V6, integração Entra do produto, armazenamento institucional permanente, regras de aprovação acadêmica, API `apps/api`, frontend `apps/web`, produção, contratação, transferência para SharePoint, consentimento institucional e alterações no conteúdo dos modelos oficiais.
- Refazer integralmente P01–P20 ou o corpus F01–F09 **sem alteração que justifique a regressão**.
- Inventar percentual de fidelidade, aceitar igualdade de páginas como prova de equivalência, atribuir o problema histórico do logotipo definitivamente a um motor sem reprodução estável, ou declarar que o Microsoft Word garante igualdade universal entre ambientes.
- Copiar fontes proprietárias para imagens versionadas ou redistribuir documentos reais por Git/CI/chamados públicos.

---

## 2. Fonte de verdade e fatos imutáveis da primeira rodada

### 2.1 Documentos prioritários

| Documento | Controle Word | Collabora CODE 26.04.5.1.1 | ONLYOFFICE Community 10.0.0.1 | Consequência |
|---|---|---|---|---|
| Relatório oficial, DOCX original | 7 páginas | 6 páginas | 7 páginas | Collabora diverge na paginação |
| Relatório após salvar/round-trip controlado | 7 páginas | 6 páginas inclusive ao reabrir no Word | 7 páginas no motor e no Word | No Collabora a divergência **não** é apenas visualização interna |
| AV3 original | 5 páginas | 5, com data renderizada de forma diferente | 5, campo com resultado em cache | Página igual não prova integridade do campo |
| AV3 após inserir/remover e três preenchimentos controlados | 5 páginas nos controles equivalentes | `SAVEDATE` com máscara incompatível no Word | `SAVEDATE` convertido para `REF` | Ambos reprovados nos campos |

**Observações complementares já verificadas:**

- O ONLYOFFICE preservou parágrafos e células nos quatro casos finais verificados, mas alterou o tipo do campo `SAVEDATE`.
- O Collabora preservou o corpo/células do AV3, mas removeu três espaços finais em dois parágrafos do relatório; registrar a diferença, sem classificá-la como perda de palavras.
- O PDF exportado pelo ONLYOFFICE pode manter o resultado de data em cache e **mascarar** o defeito OOXML; é obrigatória a reabertura no Word.
- A antiga ocorrência de logotipo só na última página **não** se reproduziu de modo estável. Não constitui bug confirmado para corrigir nesta rodada.
- `LANG/LC_ALL=pt_BR.UTF-8`, disponibilização de Calibri/famílias de fontes e restauração isolada de `tblPr`/`tblGrid` **não** resolveram a paginação do Collabora; montagem ampla de fontes chegou a invalidar visualmente o AV3. Não repetir sem hipótese nova e diferente.
- No comparador ONLYOFFICE, `SAVEDATE` → `REF` foi reproduzido também com fixture sintética. No CODE, uma fixture simples modificou a máscara, mas **não** reproduziu integralmente o comportamento de localidade japonesa do AV3.
- Os testes incluíram Word repaginado, originais preservados por SHA-256, versões WOPI imutáveis, ciclos reais no navegador e inspeção de OOXML. A inspeção visual/aceite editorial ainda é pendente.

### 2.2 Documentação a consultar **antes de alterar código**

1. `experiments/b05-document-engine/results/2026-10-17-fidelity-decision.md` — decisão controlada e critérios reprovados.
2. `experiments/b05-document-engine/ADR-B05-001.md` — base B05.0, matriz P01–P20 e pendências de G1.
3. `experiments/b05-document-engine/results/2026-10-17-official-models.md` — relatório original e Word.
4. `experiments/b05-document-engine/results/2026-10-17-av3-and-fidelity.md` — AV3 e diagnósticos de fontes/grade.
5. `experiments/b05-document-engine/results/vendor-cases.md` — reproduções parciais e relatos preparados, **não enviados**.
6. `experiments/b05-document-engine/scripts/FIDELITY.md` — protocolo de execução privada.
7. `docs/implementation/bloco-05.md` e `docs/implementation/bloco-05-fidelity-execution.md` — gates e contratos previstos.
8. `experiments/b05-document-engine/README.md`, Docker Compose, executores e host WOPI.

Esses materiais fornecem as premissas. Verificar os hashes e o estado da branch no início da execução, sem substituir os resultados anteriores por estimativas.

---

## 3. Política de branch, proteção do repositório e ambiente

### 3.1 Branch de trabalho

O PR #9 estava aberto quando este plano foi escrito. Resolver na abertura da execução:

- **Se o PR #9 já estiver integrado:** atualizar `main` e criar `spike/b05-compatibilidade-docx` a partir do merge, confirmando que o laboratório original está na base.
- **Se o PR #9 continuar aberto:** preferir integrar a pesquisa revisada ao repositório antes de criar a nova branch. Se a implementação não puder esperar esse merge, criar branch derivada de `spike/b05-collabora-wopi-docx` e PR **empilhado**, apontando para a branch da spike; somente rebasear para `main` depois que PR #9 for integrado. Não duplicar milhares de linhas de laboratório nem apresentar os dois PRs como independentes quando não são.
- Jamais commitar diretamente em `main` ou alterar o PR #9 retroativamente de forma a reescrever suas conclusões.

Alterações admitidas: `experiments/b05-document-engine/**`, documentação `docs/implementation/bloco-05-*`, ADR, testes e workflow da spike **se necessários**. Qualquer alteração fora desse perímetro exige justificativa explícita e aprovação prévia.

### 3.2 Dados e versões

- Manter originais, controles Word, DOCX intermediários, PDFs, PNGs, logotipos e relatórios brutos em `.runtime/` **privado e ignorado**. Usar cópias de trabalho com UUID e checksums; jamais sobrescrever originais.
- Preservar a montagem de fontes e configuração de Docker anteriores como histórico reconstituível. Não importar imagens nem fontes proprietárias no Git.
- Fixar **tag e digest de arquitetura** dos motores em cada ensaio; não confundir digest multiarch com nova versão.
- Registrar Word versão/build, sistema operacional, bibliotecas, Docker, fontes efetivamente carregadas, digest de imagens, scripts, commits e hashes dos arquivos. Não tratar o ambiente Windows/Word de controle como padrão universal.
- Separar Collabora e ONLYOFFICE por redes, volumes, portas e perfis; nunca compartilhar diretórios de versões entre rodadas ou reutilizar saída anterior como PDF novo.
- Nenhuma mudança em credenciais, `.env`, serviços contratados, banco Neon, Cloudflare ou deploy.

### 3.3 Validação inicial mínima

- Conferir estado da branch e `git status`; executar somente testes rápidos que validem integridade dos scripts afetados.
- Verificar com `git diff --check` e conferir que `.runtime`, `fixtures/generated` e `results/generated` seguem ignorados.
- Os testes da API principal **não** precisam ser repetidos enquanto `apps/api` e contratos compartilhados não forem alterados; seu baseline é de 185 testes no Bloco 04.

---

## 4. Estratégia de investigação e regras antirrepetição

### 4.1 Ordem de prioridade

**Trilha 1 — ONLYOFFICE / `SAVEDATE`: prioridade de resolução.** O relatório já mantém sete páginas no Word e no motor, e o defeito do campo foi reduzido a fixture sintética. Buscar primeiro solução que preserve o tipo e o resultado atualizável do campo.

**Trilha 2 — Collabora / `SAVEDATE` e paginação:** manter candidato, mas não repetir hipóteses descartadas. Separar campo (erro semântico) de paginação (renderização e round-trip). Avançar se existir hipótese técnica ou correção candidata verificável.

**Trilha 3 — Alternativa Microsoft 365:** análise de viabilidade documental iniciada paralelamente, sem implantação, permissão institucional presumida ou promessa de Word web embutido no host próprio.

### 4.2 Cada experimento deve responder a uma pergunta

Antes de executá-lo, registrar em uma ficha curta:

| Campo | Conteúdo exigido |
|---|---|
| ID | `CX-001`, `CX-002`... |
| Hipótese | Uma causa ou correção específica e falseável |
| Caso que já a suporta | Evidência anterior e arquivo/versão correspondente |
| Variável alterada | Exatamente uma categoria: versão, importação, exportação, estilo, campo ou propriedade |
| Controle | Mesmo DOCX de origem, Word de referência, fonte e script comparáveis |
| Comando/ambiente | Versões, digests e parâmetros não secretos |
| Observações | DOCX/OOXML, PDF, Word, hashes e versão WOPI |
| Resultado | Confirmada, refutada, inconclusiva — com evidência |
| Próxima ação | Prosseguir, alterar hipótese, escalar ao fornecedor ou parar |

**Limite inicial:** até duas hipóteses técnicas novas por motor para cada defeito e, se necessário, até duas versões/patches candidatas justificadas por evidência de alteração pertinente. Qualquer ampliação exige decisão escrita antes de consumir novas rodadas. Não é limite de tempo nem garantia de que um defeito será corrigido.

### 4.3 Matriz pequena, de alto poder diagnóstico

Rodar apenas estes três cenários por modelo e candidato:

- **C0 — Original sem salvar:** comparar Word e motor para detectar problemas de importação/renderização.
- **C1 — Inserir/remover com persistência:** efetuar alteração real, salvar versão intermediária, desfazer a alteração, salvar e reabrir no Word; detectar degradação mesmo sem mudança líquida no texto.
- **C2 — Três preenchimentos curtos:** mesmo texto do controle Word (` B1 B2 B3`), três checkpoints imutáveis, fechamento/reabertura e inspeção do DOCX final.

Arquivos de entrada, texto inserido, locais de edição, opções do Word e método de coleta devem ser idênticos aos controles anteriores. O transporte de edição do Collabora usa comandos UNO/composição e o ONLYOFFICE usa teclado: registrar a diferença e não alegar equivalência de interação humana; comparar a **mudança semântica final** e os bytes persistidos.

---

## 5. Etapas de execução e gates

| Etapa | Nome | Produto verificável | Gate |
|---|---|---|---|
| B05.0.1-A | Congelamento de baseline e triagem | Manifesto de evidência, versão e questões delimitadas | A |
| B05.0.1-B | Diagnóstico `SAVEDATE` | Reproduções minimizadas e causa/estágio classificado | B |
| B05.0.1-C | Diagnóstico de paginação Collabora | Inventário de diferenças e hipóteses testadas | C |
| B05.0.1-D | Correções/versões candidatas | Resultados focais CODE e ONLYOFFICE, sem regressões silenciosas | D |
| B05.0.1-E | Viabilidade Microsoft 365 | Relatório factual de elegibilidade e integração, sem pressupor direitos | E |
| B05.0.1-F | Revalidação de modelos e decisão | Matriz, ADR, escolha/rejeição e plano operacional residual | F |

A etapa E pode ocorrer paralelamente a B–D. A etapa F não começa com hipóteses vagas: necessita de pelo menos um candidato funcional corrigido ou de evidência suficiente para encerrar os candidatos e selecionar caminho alternativo.

### B05.0.1-A — Congelar baseline e preparar instrumentos

**Implementar:**

- Confirmar branch, PR #9, árvore limpa, versões e digests. Criar `results/b05-compatibility-baseline.md` e manifesto JSON **somente com dados não pessoais**.
- Recuperar snapshots exatos dos ensaios anteriores; auditar hashes de originais, controles e checkpoints privados sem publicar valores que associem pessoas ou documentos.
- Estabelecer funções pequenas e testadas para inventário semântico OOXML e comparação antes/depois, incluindo corpos, cabeçalhos, rodapés, `word/settings.xml`, `styles.xml`, seções e relações relevantes.
- Diferenciar campos `w:fldSimple` e campos complexos (`w:fldChar`, `w:instrText`), além de resultado armazenado, formato, idioma, tipo, instrução e estrutura de relações. Inventário não deve executar links externos, macros nem atualizar campos.
- Padronizar classificação: `unchanged`, `expected-edit`, `semantic-regression`, `layout-regression`, `unverified`.

**Não fazer:** baixar/sobrescrever documentos históricos por padrão, reexecutar F01–F09 sem necessidade, copiar XML bruto de documentos pessoais ao Git.

**Aceite A:** ferramenta de inventário tem testes unitários com fixtures sintéticas e compara ao menos o `SAVEDATE` conhecido em dois formatos OOXML sem gerar falso positivo por simples mudança de representação equivalente. O manifesto identifica precisamente o que já foi observado.

### B05.0.1-B — Forense de `SAVEDATE` e campos DOCX

**B.1 ONLYOFFICE:**

1. Partir da fixture sintética que já transforma `SAVEDATE` em `REF`; registrar OOXML antes/depois, versões WOPI e os passos exatos de edição.
2. Determinar se a alteração surge no carregamento, ao primeiro save, após uma atualização de campos, no reabrir ou na conversão DOCX; a prova pode usar checkpoints intermediários, nunca somente PDFs.
3. Fazer redução adicional da fixture, mantendo no mínimo campo original, resultado em cache e mecanismo que causa falha. A redução só é válida se continuar reproduzindo `SAVEDATE` → `REF` no DOCX salvo e erro no Word.
4. Procurar relatórios/patches/releases oficiais pertinentes e verificar versões afetadas, sem inferir correção por uma versão ser mais recente.

**B.2 Collabora:**

1. Usar o caso AV3 privado e a fixture sintética F07 existentes. Inventariar campo antes/depois nos XML de cabeçalho/corpo; documentar instrução, máscara de data, token de localidade e erro após reabertura no Word.
2. Reduzir progressivamente uma **cópia** do AV3 com preservação das partes indispensáveis (campo, cabeçalho, estilos, idioma, configurações e relações), sempre reexecutando o mesmo caso após cada redução.
3. Se a fixture reduzida reproduzir o erro, classificá-la e torná-la publicável **somente após revisão de privacidade/licenças**; uma redução parcial não deve ser declarada reprodução integral.
4. Verificar se o defeito ocorre sem edição líquida; diferenciar máscara visual legítima de campo semanticamente inválido no Word.

**Não implementar correção silenciosa de XML no caminho de salvamento.** Uma camada de preservação de campo só pode ser considerada como **experimento alternativo explicitamente autorizado** se documentar escopo, limites, integridade semântica, round-trip, segurança e regressões em vários documentos. Ela não pode esconder defeitos na interface, modificar o modelo oficial nem fabricar uma aprovação.

**Aceite B:** para cada motor, (i) reprodução privada comprovada; (ii) classificação do estágio causador ou registro `não isolado`; (iii) caso mínimo sintético onde viável, com status honesto; (iv) pelo menos uma ação técnica justificável — patch/versão, relato ao fornecedor, ou encerramento do candidato.

### B05.0.1-C — Diagnóstico da paginação do relatório no Collabora

**Método de comparação em duas fronteiras:**

- **Fronteira 1, leitura:** mesmo DOCX original → Word 7 páginas e CODE 6. Localizar a primeira quebra divergente, comparando posição de parágrafos/tabela, cabeçalhos, rodapés, seção A4 paisagem, métricas e layout.
- **Fronteira 2, serialização:** DOCX original 7 no Word → salvar pelo CODE → DOCX salvo 6 no Word. Fazer diff OOXML orientado por estrutura para identificar propriedades persistidas que mudaram e potencialmente alteram o layout.

**Investigar somente hipóteses ainda não eliminadas**, uma de cada vez: propriedades de parágrafo/estilo, espaçamentos, regras de quebra, `keepNext`, `keepLines`, `widowControl`, propriedades de linhas/células de tabelas, `sectPr`, configurações `compat`, cabeçalhos/rodapés e métricas de fonte/substituições efetivamente utilizadas. A lista é de **hipóteses**, não de causas comprovadas.

**Instrumentos:** inventário OOXML de propriedades relevantes, Word com `Repaginate`, PDFs de origem exata, rasterização todas as páginas, inspeção visual com localização da primeira divergência, comparação de áreas e objetos sem afirmar equivalência a partir de contagem de páginas. Preservar registro de mudanças com impacto semântico.

**Aceite C:** um dos resultados abaixo, fundamentado:

- **C1:** propriedade/etapa causadora isolada, correção candidata reproduzível; ou
- **C2:** mapa de diferenças e evidências negativas suficientes, sem falsa causa única, para encaminhar ao fornecedor ou encerrar a hipótese Collabora no escopo definido.

O método não exige prometer correção. Não repetir locale, Calibri ou somente grade de tabela sem hipótese inédita explicitamente documentada.

### B05.0.1-D — Aplicar/avaliar correções com escopo controlado

**Ordem preferencial:** ONLYOFFICE (um defeito reproduzível, paginação mais próxima) e depois Collabora (dois defeitos independentes). Essa é priorização de diagnóstico, **não** seleção definitiva de fornecedor.

Para cada candidato de correção:

1. Registrar versão/digest ou commit do patch, licença, fonte, changelog/issue e por que ele poderia resolver a causa observada.
2. Construir ambiente **novo** e isolado, mantendo imagem antiga para A/B; não trocar `latest` nem reutilizar volumes/documentos já editados.
3. Rodar **primeiro** fixture sintética mínima do defeito; falhou → registrar e parar candidato (não executar ensaio oficial completo sem potencial de sucesso).
4. Passou fixture → rodar C0/C1/C2 dos dois modelos oficiais com controles Word equivalentes, versões imutáveis e conferência de campos/estilos/imagens/paginação. Priorizar reabertura em Word, não só PDF do motor.
5. Corrigiu o defeito-alvo → reexecutar testes de contrato WOPI, persistência, CAS, tokens e downloads somente se a mudança afetar host/editor/protocolo; manter matriz de regressão pequena e justificada.
6. Testar repetibilidade em pelo menos duas execuções independentes do caso que havia falhado, com ambientes e hashes controlados, para descartar falso positivo de cache/tempo.
7. Registrar qualquer novo defeito, inclusive alterações de conteúdo, espaços, campos, cabeçalhos, imagens, relação, fórmulas e perda de links. **Uma correção que introduz outra falha crítica é reprovação.**

**Permissões/limites de correção:** não editar modelos originais, não substituir `SAVEDATE` por data estática, não forçar PDF como fonte da verdade, não restaurar automaticamente XML do original sobre conteúdo salvo sem provar validade, não atualizar indiscriminadamente pacotes/fontes, não criar fork permanente de motor sem análise de manutenção e licenças.

**Aceite D:** para cada motor, relatório `PASS/FAIL/INCONCLUSIVO` por defeito e versão, com evidências antes/depois, regressões seletivas e justificativa de continuar/parar.

### B05.0.1-E — Análise de viabilidade Microsoft 365, **sem presumir integração**

Esta etapa é **análise arquitetural/contratual**, não ativação de serviço nem fallback automaticamente aprovado.

**E.1 Diferenciar três possibilidades:**

- **Edição no Microsoft Word para a Web com documentos guardados pelo NITE CONNECT:** requer elegibilidade e participação no **Cloud Storage Partner Program (CSPP)**, no caso de integração direta do serviço Microsoft 365 via WOPI. A documentação atual indica o programa para fornecedores independentes de armazenamento SaaS e **não** para clientes Microsoft 365 comuns; não presumir que o núcleo universitário se qualifique.
- **Documentos guardados no SharePoint/OneDrive institucional, com catálogo/metadados/vínculos no NITE CONNECT:** avaliar uso autorizado de Microsoft Graph, links/abertura no Word web, controle de versões e notificações, mas reconhecer que a **fonte de verdade dos arquivos** passaria ao Microsoft 365 e que vínculo não equivale a edição embutida no próprio site.
- **Edição externa no Word desktop, com reenvio versionado ao NITE CONNECT:** contingência que preserva autoria e versões sem editor web integrado, porém muda a experiência desejada; exigiria aprovação de escopo e não equivale ao Gate G1 aprovado para editor web.

**E.2 Verificar sem fazer alterações institucionais:** existência e tipo de licenças/tenant, direitos do administrador, política de SharePoint, aprovações de consentimento Graph, capacidade de integração autorizada, modelo de permissão, privacidade e localização de dados, custos, dependência de conectividade, rastreabilidade de edições, suporte, retenção e estratégia de saída. Não usar Entra login como prova de direito ao Word Online.

**E.3 Consultar documentação oficial:**

- Microsoft CSPP/eligibilidade: https://learn.microsoft.com/en-us/microsoft-365/cloud-storage-partner-program/online/overview
- Aplicação ao CSPP: https://learn.microsoft.com/en-us/microsoft-365/cloud-storage-partner-program/online/apply-for-cspp-program
- WOPI Microsoft: https://learn.microsoft.com/en-us/microsoft-365/cloud-storage-partner-program/online/
- Microsoft Graph e SharePoint: verificar documentação atual específica do caminho proposto e permissões mínimas antes de concluir viabilidade.

**Aceite E:** matriz `elegível / depende de autorização / inviável ou não confirmado` para cada rota, indicando onde ficam os bytes, quem controla versões, permissões e auditoria, como o usuário abriria/editaria/salvaria, limites de licença/contrato e custos não estimados. Não escrever “SharePoint integrado” sem prova institucional.

### B05.0.1-F — Revalidação final, decisão e fechamento

**Entradas:** relatórios A–E, ADR original e critérios funcionais dos dois modelos. Revalidar no candidato **que efetivamente passou nos testes focais**, não repetir exaustivamente todas as imagens/fixtures antigas sem relação com a mudança.

**F.1 Critérios necessários de fidelidade documental (Gate G1-F):**

- **Relatório:** original e versões após C1/C2 devem manter semântica e paginação de referência no Word para o conteúdo equivalente; nenhuma alteração não aprovada em tabelas, imagens, margens, cabeçalhos/rodapés, links, divisões e posição de elementos. A contagem de 7 páginas é referência dos casos controlados, **não** regra para qualquer conteúdo futuro.
- **AV3:** `SAVEDATE` preserva o **tipo e funcionalidade**, sem transformação para `REF`, máscara inválida ou erro ao reabrir/atualizar no Word; elementos, tabelas, logo, cabeçalhos e demais campos permanecem coerentes. A referência de 5 páginas refere-se aos controles equivalentes, não a acréscimos arbitrários de texto.
- **Ciclos:** C0, C1 e C2 em ambos os documentos, com os mesmos conteúdos de controle e no mínimo duas execuções independentes do defeito corrigido; nenhuma perda de dados, nova regressão semântica ou degradação acumulada.
- **Persistência:** cada save comprovado por `PutFile`/versão imutável capturada; hashes, IDs e `X-Source-Version` coerentes; download e reabertura do **arquivo que o host salvou**.
- **Inspeção:** OOXML + PDF rasterizado + reabertura no Word + comparação editorial. Fontes/ambiente identificados; o responsável pelos modelos aprova desvios estritamente não críticos se houver, **antes** de qualificar como GO.

**F.2 Regra fundamental:** **G1-F aprovado não equivale automaticamente a G1 completo.** A ADR B05.0 registra pendências P01–P20 (revogação durante coedição/WSS, TLS, segurança de produção, carga-alvo, backups, aceite editorial, licenças etc.). Ao encontrar motor com fidelidade aprovada, criar plano de fechamento **focalizado das pendências restantes de G1** antes de liberar B05.2.

**F.3 Possíveis decisões finais:**

| Resultado | Decisão permitida | Consequência |
|---|---|---|
| Motor passa G1-F; requisitos G1 restantes concluídos e aprovados | **GO técnico G1** | Pode seguir à B05.1 de decisões D01–D26 e, depois de G2, B05.2; não equivale a deploy/homologação |
| Motor passa G1-F; faltam requisitos operacionais G1 | **FIDELIDADE APROVADA, G1 PENDENTE** | Fechar só os riscos G1 pendentes; não implementar V6 |
| Um motor corrige parte, mas falha campo/layout obrigatório | **NO-GO desse motor/versão** | Encerrar candidato no escopo testado; avaliar outra versão ou alternativa autorizada |
| Microsoft documentalmente viável, mas integração não testada | **Alternativa candidata, não GO** | Elaborar POC e aprovações próprias; não aprovar por analogia ao Word desktop |
| Nenhuma alternativa viável dentro do escopo | **Decisão de não habilitar editor integrado para modelos oficiais** | Responsável deve decidir alteração formal de escopo (ex.: edição externa) ou suspensão do Bloco 05; nunca interpretar como G1 verde |

**Aceite F:** `ADR-B05-001` atualizada por adendo sem reescrever a decisão de 17/10, matriz final assinável, resultado explícito, limitações residuais, responsável pela aprovação, plano de continuidade e PR revisado. Não declarar “Bloco 05 concluído” somente por decisão do motor.

---

## 6. Matriz focal de verificações

| ID | Prioridade | Cenário | Passagem / falha objetiva |
|---|---|---|---|
| R01 | P0 | Recuperar origem e checkpoints imutáveis | Hash, versão e Word baseline coerentes |
| R02 | P0 | Inventário `SAVEDATE` sintético original | Campo classificado por tipo, instrução, resultado e parte OOXML |
| R03 | P0 | ONLYOFFICE no-op controlado | Detectar alteração para `REF` / campo preservado |
| R04 | P0 | ONLYOFFICE três ciclos | `SAVEDATE` preservado em todas as versões, Word sem erro |
| R05 | P0 | CODE AV3 original sem editar | Identificar representação inicial, inclusive máscara/localidade |
| R06 | P0 | CODE AV3 no-op | Não introduzir máscara inválida nem erro Word |
| R07 | P0 | CODE AV3 três ciclos | Campo, corpo, tabelas, cabeçalhos e imagens íntegros |
| R08 | P0 | Reabrir Word, atualizar campo e exportar | Campo semanticamente válido, sem erro oculto por cache PDF |
| R09 | P0 | Relatório original Word vs CODE | Primeira quebra/layout divergente identificada |
| R10 | P0 | Relatório salvo CODE reaberto no Word | Não reduzir indevidamente 7→6 nos casos equivalentes |
| R11 | P0 | Relatório no-op e três ciclos | Sem regressão persistida, espaços e hyperlinks catalogados |
| R12 | P0 | A/B de versão/patch candidato | Mudança única, digest, controle, resultado replicável |
| R13 | P0 | Salvar DOCX real no host | Nova versão confirmada, sem sobrescrever anterior |
| R14 | P0 | Converter versão exata final | Identificador/sha conferido, PDF do mesmo DOCX |
| R15 | P0 | Inspeção editorial modelos oficiais | Aprovação expressa ou reprovação com localização da diferença |
| R16 | P1 | Casos mínimos sintéticos de ambos os motores | Reproduções corretas sem dados pessoais |
| R17 | P1 | Regressões de estrutura ZIP/OOXML alterada | Sem conversões malformadas, conteúdo ativo indevido ou falhas abertas |
| R18 | P1 | Autorizações WOPI após mudança no host | Somente leitura 403, outro documento 404, revogação 401 |
| R19 | P1 | Locks e CAS após mudança no host | Escrita antiga não substitui versão atual |
| R20 | P1 | Versionamento ao reiniciar serviços | Arquivo recuperado da fonte de verdade do host |
| R21 | P1 | Referência ao logotipo | Não atribuir bug sem reprodução em condições controladas |
| R22 | P1 | Regressão seletiva F01–F09 | Só fixtures afetadas pela mudança; justificar expansão |
| R23 | P1 | Caminhos Microsoft possíveis | Elegibilidade, identidade, armazenamento, licenças sem presunção |
| R24 | P0 | ADR e gates | G1-F/G1/G2 distintos, com evidência e autoridade |

**P0** = indispensável para decidir o defeito/fidelidade; **P1** = necessário quando a alteração/caminho impactar o respectivo controle. Um caso não executado fica `PENDENTE` ou `NÃO APLICÁVEL JUSTIFICADO`, nunca `PASS`.

### Verificações automatizadas existentes a preservar

```powershell
# Na raiz do repositório; host de laboratório isolado.
mvn --batch-mode -f experiments/b05-document-engine/wopi-host/pom.xml verify
python -m unittest discover -s experiments/b05-document-engine/scripts -p 'test_*.py'

# Apenas se scripts forem alterados; sintaxe dos executores relevantes.
node --check experiments/b05-document-engine/scripts/editor_cycles.cjs
node --check experiments/b05-document-engine/scripts/onlyoffice_cycles.cjs

# Conferência de alterações e arquivos privados ignorados.
git diff --check
```

A CI da spike confirmou **28 testes Java e 12 Python** no commit base e conversão sintética F01, mas **não** os ensaios com modelos oficiais. Novos testes devem reproduzir os defeitos antes de corrigir (`RED→GREEN`). Não presumir que contagem de testes continuará fixa.

---

## 7. Regra de diagnóstico, propostas de correção e envolvimento de fornecedores

### 7.1 Hierarquia preferencial de correção

1. **Correção nativa comprovada** no motor/versão, verificável com caso sintético e modelo oficial.
2. Patch específico, com escopo de manutenção, licença e segurança demonstrados; não incorporar fork permanente automaticamente.
3. Configuração documentada e reproduzível que **de fato** corrija os modelos sem prejudicar outro modelo, com fonte licenciada/legalmente disponível.
4. Como hipótese excepcional, mecanismo de compatibilidade pós-save **apenas para investigação autorizada**, com invariantes OOXML, integridade, testes negativos e total transparência. Sem aprovação explícita, **não implementar**.
5. Alternativa de fornecedor/arquitetura ou rejeição formal do requisito de edição integrada nesse estágio.

Não aplicar normalização de estilos, achatamento de campos, substituição por texto estático, edição da fonte oficial ou “reparo” de XML que esconda erro e não preserve semântica.

### 7.2 Relatos aos fornecedores

Usar `results/vendor-cases.md` como rascunho, mas antes de publicar:

- Preferir fixture sintética minimizada sem informações pessoais, propriedade intelectual institucional, links internos ou fontes licenciadas.
- Distinguir **reprodução integral** de **reprodução parcial** (especialmente o caso de máscara japonesa do CODE).
- Especificar versão e digest, resultado esperado/observado, comparação OOXML, passos, Word build, ambiente e logs sanitizados.
- Não anexar originais oficiais, PDFs com dados pessoais nem fonte proprietária. Nenhum chamado deve ser enviado automaticamente; envio depende de autorização e revisão de privacidade.
- Registrar URL do ticket, resposta, versão prometida e mudança de status somente após ocorrerem efetivamente.

---

## 8. Análise de risco e segurança

| Risco | Controle obrigatório na investigação |
|---|---|
| Vazamento de dados dos modelos oficiais | `.runtime` privado, manifestos sem texto/nomes, ausência de arquivos no Git/CI |
| Corrupção por salvar/sobrescrever fonte | Cópias de trabalho, hashes, versões WOPI imutáveis e Word controle independente |
| Confundir PDF correto com DOCX válido | Inspecionar campo OOXML e reabrir DOCX no Word |
| Fazer conclusão a partir de cache/execução interrompida | Checkpoints por versão exata, diretório novo e execução completa |
| Falso GO por igualdade de páginas | Inspeção de conteúdo, campos, layouts e aceite editorial |
| Expor conversor/host da spike | Portas loopback, gateway, dados sintéticos na interface pública; manter JWT desabilitado ONLYOFFICE somente no laboratório |
| Falhas em links externos do DOCX | Validador não buscar conteúdo externo, macros desabilitadas no Word, hyperlinks conservados sem fetch |
| Atribuir erro a fonte sem prova | Fontes e substituições inventariadas; hipótese isolada; direitos de uso respeitados |
| Efeito adverso de versão candidata | Ambientes imutáveis lado a lado, tags+digest, teste negativo e reversão fácil |
| Assumir acesso Microsoft por ter Entra | Checagem CSPP/licença/tenant/Graph/consentimento e autoridade institucional |
| Uso indevido de laboratório como produto | Documentação expressa, workflow separado e nenhuma dependência em `apps/api` |

Dados oficiais não devem ir a serviços de terceiros, sandboxes externas, CI pública ou plataformas de diagnóstico sem autorização institucional específica. A existência de documentos de teste no computador do operador não implica direito de redistribuição.

---

## 9. Entregáveis versionáveis e estrutura recomendada

**Plano (este arquivo):** `docs/implementation/bloco-05-compatibility-plan.md`, após revisão/aprovação e inclusão por branch. **Relatórios previstos para a execução:**

```text
experiments/b05-document-engine/
  ADR-B05-001.md                         # adendo de decisão, sem apagar NO-GO original
  results/
    b05-compatibility-baseline.md         # evidências e versões; sem dados privados
    b05-savedate-diagnosis.md             # ambos os motores, reproduções e patches
    b05-collabora-pagination.md           # fronteiras render/save e hipóteses
    b05-engine-candidate-results.md       # tentativas A/B, controles e regressões
    b05-microsoft-feasibility.md          # elegibilidade/arquitetura, sem credenciais
    b05-compatibility-final-decision.md   # GO/NO-GO e gates separados
    vendor-cases.md                      # atualizado, ainda não enviado automaticamente
  scripts/
    ...                                   # pequenos comparadores/testes incrementais
  .runtime/                               # SOMENTE PRIVADO / IGNORADO NO GIT
```

Arquivos auxiliares (`JSON`, gráficos e inventários) podem ter novos nomes com o identificador da execução. Não gravar hashes pessoais ou texto sensível em relatórios versionáveis quando permitirem identificação. Logs técnicos devem evitar tokens WOPI e URLs contendo capacidades.

### Contrato do relatório final

`b05-compatibility-final-decision.md` deverá conter, nesta ordem:

1. Baseline e motores/digests testados.
2. Problemas originais e reproduções mínimas (integral/parcial).
3. Hipóteses, tentativas e falsificações, inclusive tentativas descartadas.
4. Matriz C0/C1/C2 para os dois modelos, Word e motores.
5. Diferenças OOXML de campos e diferenças visuais de paginação.
6. Evidência de persistência WOPI das versões finais.
7. Aceite/reprovação editorial por responsável e datas (se existente).
8. Matriz de viabilidade Microsoft com dependências institucionais.
9. Estado de G1-F, G1 e G2 **separadamente**.
10. Seleção/rejeição por motor e motivo; riscos residuais; próximo passo autorizado.
11. Testes Java/Python/CI executados e quais testes oficiais não fazem parte da CI.
12. Links do PR e das documentações; confirmações de que modelos e produção permaneceram intactos.

---

## 10. Critérios de concluir a B05.0.1

A etapa **de investigação** fica tecnicamente encerrada se, e somente se:

- B05.0.1-A–F tiverem relatório explícito, inclusive negativas e limites, sem esconder falhas;
- os defeitos `SAVEDATE` e paginação tiverem causas/candidatos avaliados ou decisão justificada de parar, com reproduções e hipóteses verificáveis;
- cada motor tiver status formal nos **dois** modelos oficiais, inclusive `NO-GO` quando aplicável;
- houver decisão arquitetural/estratégia escolhida, ou declaração formal de impossibilidade de aprovar editor sob as condições atuais, com autoridade responsável e consequência clara;
- G1-F, G1 total e G2 não forem confundidos;
- testes afetados, verificações de segurança e CI da spike estiverem verdes para o código efetivamente modificado, com revisão independente;
- os documentos oficiais permanecerem intactos; nenhum dado pessoal tiver sido versionado;
- nenhuma migração V6, mudança em `apps/api`, contratação, produção ou integração de motor no produto tiver ocorrido sem autorização adicional.

**Não é condição de êxito da pesquisa encontrar necessariamente uma correção.** É condição de êxito não deixar indefinida a decisão: ou existe uma solução aprovada com critérios verificáveis, ou existe uma rejeição fundamentada e um caminho posterior explicitamente submetido à aprovação. **Não avançar automaticamente o produto com um editor reprovado.**

---

## 11. Instrução de execução para a equipe/agente

> Trabalhe exclusivamente na resolução focalizada B05.0.1, usando como baseline a spike `spike/b05-collabora-wopi-docx` e os relatórios de 17/10/2026. Verifique primeiro o PR #9 e escolha branch conforme a seção 3. Preserve os originais dos dois modelos e toda a documentação B05.0. Não altere `main`, `apps/api`, `apps/web` nem migrações V1–V5; não crie V6. Inicie por inventário de `SAVEDATE` e experimento mínimo reproduzível no ONLYOFFICE, depois CODE, e separe o diagnóstico de paginação. Para cada hipótese exija RED→GREEN ou evidência equivalente controlada; descarte tentativas sem poder diagnóstico. Compare sempre DOCX salvo e reaberto no Word e não apenas PDFs. Considere a alternativa Microsoft por análise oficial de elegibilidade, sem presumir CSPP ou licenças. Entregue relatórios A–F, ADR revisada, matriz de testes, decisão G1-F/G1/G2 clara e PR independente ou empilhado, sem merge/deploy automático. Não declare Bloco 05 completo até que os gates funcionais e técnicos posteriores tenham sido aprovados.

---

## 12. Referências

### Evidência primária do projeto

- [PR #9 — laboratório documental](https://github.com/IasminLuise/00_NiteConnect_2026.2_UNIJORGE/pull/9)
- [Decisão controlada de fidelidade, 17/10/2026](https://github.com/IasminLuise/00_NiteConnect_2026.2_UNIJORGE/blob/spike/b05-collabora-wopi-docx/experiments/b05-document-engine/results/2026-10-17-fidelity-decision.md)
- [ADR-B05-001, evidências, limites e gates](https://github.com/IasminLuise/00_NiteConnect_2026.2_UNIJORGE/blob/spike/b05-collabora-wopi-docx/experiments/b05-document-engine/ADR-B05-001.md)
- `results/2026-10-17-av3-and-fidelity.md`, `results/2026-10-17-official-models.md`, `results/vendor-cases.md` na mesma branch.
- `docs/implementation/bloco-05.md` e `docs/implementation/bloco-05-fidelity-execution.md` na mesma branch.

### Documentação externa de viabilidade (não substitui ensaios locais)

- [Microsoft — WOPI/CSPP, visão geral e elegibilidade](https://learn.microsoft.com/en-us/microsoft-365/cloud-storage-partner-program/online/overview)
- [Microsoft — processo de aplicação ao CSPP](https://learn.microsoft.com/en-us/microsoft-365/cloud-storage-partner-program/online/apply-for-cspp-program)
- [ONLYOFFICE — WOPI e parâmetros](https://api.onlyoffice.com/docs/docs-api/using-wopi/overview)
- [Collabora Online — repositório/issues](https://github.com/CollaboraOnline/online/issues)

**Resultado deste documento:** plano executável, **não** implementação. A decisão tecnológica final permanece `NO-GO` para os motores/versões já testados até surgir evidência contrária validada.
