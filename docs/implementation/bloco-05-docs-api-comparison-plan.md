# NITE CONNECT — Plano de execução B05.0.3

**Avaliação final do ONLYOFFICE: Docs API × integração WOPI — fidelidade DOCX, salvamento verificável e experiência própria do NITE CONNECT**
**Estado:** planejamento para implementação experimental, **não executado**.
**Fonte de verdade:** `AGENTS.md`, `apps/api/AGENTS.md`, `docs/product/NITE_Connect_Especificacao_Funcional_Consolidada_v1.0.pdf`, documentos B05.0–B05.0.2 e decisões expressamente aprovadas.

> **Decisão de escopo:** o Collabora Online está **descartado como candidato**. Não executar, instalar, atualizar, comparar desempenho, portar adaptação ou gastar desenvolvimento novo com Collabora/LibreOffice. Evidências históricas existentes não serão excluídas. O único motor sob análise nesta rodada é **ONLYOFFICE Docs**, nas modalidades **Docs API e WOPI**. Comparar interfaces de integração do *mesmo motor* não constitui avaliação de dois mecanismos DOCX independentes.

---

## 1. Objetivo, pergunta de decisão e resultado esperado

### 1.1 Pergunta única

**Na versão fixada do ONLYOFFICE Docs, a integração via Docs API oferece uma experiência suficientemente personalizável e um contrato de salvamento/recuperação confiável para que o NITE CONNECT preserve os modelos DOCX oficiais — inclusive `SAVEDATE` — e confirme versões validadas no próprio armazenamento, superando os bloqueios práticos da integração WOPI?**

O resultado final será um destes:

- **ONLYOFFICE selecionado condicionalmente:** Docs API aprovada em salvamento real, integridade e experiência **com os dois modelos oficiais**, sem regressões críticas; continuam necessários G1 operacional, licenciamento e G2 antes do produto.
- **ONLYOFFICE descartado da arquitetura do NITE CONNECT:** os testes específicos falham nos critérios obrigatórios, ou sua edição/licença/UX não é viável. Encerrar novas pesquisas com WOPI/Docs API e iniciar **decisão de nova solução**, sem migrar silenciosamente para outro fornecedor.
- **Inconclusivo por dependência externa identificada:** infraestrutura, direitos/licenciamento ou autorização editorial indispensável ausente. Documentar bloqueador, responsável e decisão concreta; **não declarar GO** nem prolongar indefinidamente a pesquisa.

### 1.2 Princípios não negociáveis

1. **Mesma versão do motor:** preferencialmente ONLYOFFICE Community `10.0.0.1`, exatamente os digests fixados nos relatórios; não atribuir uma diferença de versão ao protocolo. Se Docs API não estiver funcional nessa instalação, documentar por quê antes de escolher outra versão.
2. **Testes novos e mínimos:** reutilizar baseline, scripts, modelo de documentos, Word de controle e relatórios B05.0.1/B05.0.2. Não repetir o corpus completo ou comparações com motores descartados.
3. **Separar três estados:** (a) alterações no navegador; (b) alterações recebidas pelo servidor ONLYOFFICE; (c) **arquivo validado, versionado e confirmado no host NITE experimental**. Somente (c) autoriza sinalização própria de “salvo no NITE CONNECT”.
4. **Não confundir sucesso do callback com aprovação:** só responder sucesso do *callback* depois da verificação e persistência durável no host, de acordo com o contrato da versão ensaiada; registrar falhas e seu comportamento de retry/recuperação.
5. **Fidelidade não é aparência:** `SAVEDATE`, estrutura OOXML, Word reaberto, versão persistida, PDF do exato DOCX e inspeção de elementos precisam concordar.
6. **Não afrouxar a validação para gerar GO:** qualquer equivalência admitida de reserialização precisa de prova independente, critérios escritos e regressão negativa. A candidata do motor não pode servir de gabarito para ela própria.
7. **Sem integração de produto:** não alterar `apps/api`, `apps/web`, Flyway V6, credenciais Entra institucionais, bancos externos, Data Center de produção ou modelos oficiais originais.
8. **Sem promessa de editor React completo:** Docs API fornece um editor pronto, configurável e incorporado; não oferece automaticamente uma engine headless ou substituição ilimitada do canvas/ribbon por componentes próprios.

### 1.3 Fora do escopo

Clone pixel-perfect do Microsoft Word/SharePoint; produção, licenças comerciais, publicação externa, migração de documentos, coedição massiva, nova API principal, Diário de Bordo, novo motor, fork do ONLYOFFICE, reparador de DOCX no pós-save, upload institucional para serviço terceiro, aceitação de modelo alterado sem autorização e regressão integral de Blocos 01–04.

---

## 2. Situação anterior: o que não precisa ser redescoberto

| Questão | Evidência já disponível | Consequência para B05.0.3 |
|---|---|---|
| WOPI funciona sinteticamente | PR #9: host de laboratório, versões, WOPI, conversão, segurança básica | Reutilizar como controle; não refazer a spike |
| ONLYOFFICE + `SAVEDATE` simples | PR #10: no modelo carregado vira `REF` antes de editar | Não esperar que mudar WOPI para Docs API corrija o parser nativo |
| Variante complexa | PR #10: preservada em caso sintético, atualizada corretamente no Word | Reutilizar fixture mínima e adaptador como **hipótese**, não solução homologada |
| Adaptador controlado | PR #11: `b0502-savedate-1` aprovado sinteticamente no Word | Reutilizar sem modificar originais; comparar mesma cópia normalizada nos dois caminhos |
| WOPI com gate conservador | PR #11: candidato chegou, foi recusado por mudanças não certificadas; UI antecipou “All changes saved”; backup reapareceu | Reproduzir **um único controle focal WOPI** e concentrar novos esforços em Docs API |
| Corrupção definitiva/perda | **Não comprovada** | Não acusar o fornecedor ou gate sem nova prova |
| Modelos oficiais + adaptador | **Nunca ensaiados** (parada no Gate C) | Abrir essa etapa **apenas após passar persistência e validação sintéticas** |
| G1-F/G1/G2 | G1-F não aprovado; G1 não aprovado; G2 fechado | Nenhum resultado sintético abre gates de produto |

**Ler antes de programar:** `experiments/b05-document-engine/results/b05-adapter-final-decision.md`, `b05-adapter-save-validation.md`, `b05-adapter-synthetic-tests.md`, `b05-compatibility-final-decision.md`, `ADR-B05-001.md`, `docs/implementation/bloco-05-adapter-plan.md`, `scripts/docx_adapter.py`, `scripts/adapter_protocol.md`, `scripts/adapter_save_probe.cjs`, `ScriptedDocxGate.java`, `DocumentStore.java`; `AGENTS.md` e especificação consolidada. Preservar integralmente os relatórios negativos anteriores.

**Fonte pública do próprio fornecedor:** [comparativo API vs WOPI](https://api.onlyoffice.com/docs/docs-api/using-wopi/api-vs-wopi), [fluxo de salvamento](https://api.onlyoffice.com/docs/docs-api/get-started/how-it-works/saving-file), [callback handler](https://api.onlyoffice.com/docs/docs-api/usage-api/callback-handler), [configuração e eventos](https://api.onlyoffice.com/docs/docs-api/usage-api/config/events), [Docs API](https://api.onlyoffice.com/docs/docs-api/get-started/how-it-works), [forçar salvamento](https://api.onlyoffice.com/docs/docs-api/additional-api/command-service/forcesave), [personalização](https://api.onlyoffice.com/docs/docs-api/usage-api/config/editor/customization/customization-standard-branding), [plugins](https://api.onlyoffice.com/docs/docs-api/usage-api/config/editor/plugins), [restrições Community](https://helpcenter.onlyoffice.com/pt-BR/docs/faq/docs-community.aspx). Confirmar parâmetros e disponibilidade na **versão fixada**, não em exemplos de outra release/edição.

---

## 3. Comparação justa: o que se mantém e o que varia

### 3.1 Controles iguais

- Mesmo ONLYOFFICE Docs, tag + digest por plataforma; mesmo Word de referência, fonte e sistema documentados.
- Mesmos bytes iniciais das fixtures sintéticas e do DOCX adaptado; hash e identificador de conteúdo no manifesto privado.
- Mesmos roteiros de texto: documento original sem editar; `XYZ` inserido e salvo; remoção de `XYZ`; três pequenos preenchimentos se o gate for alcançado.
- Mesmos testes de integridade do backend experimental, limites DOCX e verificador independente de conteúdo.
- Mesmas regras de versão anterior imutável, rejeição pré-commit, CAS e registro de tentativa.
- Isolamento entre datasets, usuários sintéticos, keys, caches e sessões. Nunca reutilizar `document.key` ou ID WOPI contaminado por backups de rodadas anteriores.

### 3.2 Variável principal

| Dimensão | WOPI — controle histórico | Docs API — novidade a comprovar |
|---|---|---|
| Abertura | Discovery + sessão/capability WOPI | `DocEditor` + `document.url`, `document.key`, `editorConfig.callbackUrl`, configuração assinada |
| Persistência | `PutFile` + resposta HTTP/CAS | `callbackUrl` com `status` 2/6, download controlado da candidata, validação, commit e resposta do callback |
| Confirmação visual | Indicador do motor não equivale a versão do host | Eventos do editor também **não** equivalem; NITE mostra estado do host por consulta/evento próprio |
| Save sem fechar | Fluxo observado e edição WOPI | **Force save** via botão/comando, com confirmação posterior do commit e tratamento de `status` 6/7 |
| Identificação de versão | Item version/CAS do host WOPI | `document.key` por ciclo, ID de versão do host e `callback` correlacionados; chave nova após finalização |
| Interface | Personalização WOPI restrita | Tema, configuração do editor, toolbar, recursos adicionais e plugins conforme edição/licença |
| Coedição | Locks/fluxo WOPI | `document.key` compartilhada na sessão, callbacks e permissões; sem assumir transações no banco do produto |
| Segurança | Capacidades WOPI, proof keys e controles do host | JWT de configuração/comandos/callbacks conforme versão, URLs controladas, autenticação/autorização no host |

**Cuidado:** `onDocumentStateChange(false)` indica que alterações foram enviadas ao serviço de edição — **não** que o NITE persistiu o DOCX. `status:6` é um *force save* de estado intermediário, não necessariamente versão final. A resposta `{"error":0}` só pode ser dada depois da conclusão válida da operação exigida.

---

## 4. Política de branch, privacidade e ambiente

1. Confirmar #9/#10/#11 e `git status`. Caso #11 siga aberto, criar `spike/b05-docs-api-vs-wopi` **a partir de** `spike/b05-adaptador-docx`, com PR **empilhado sobre #11**. Se os pais já estiverem integrados, usar `main` atualizado e PR normal. Não rebasear ou mergear sem revisão.
2. Mudanças permitidas: `experiments/b05-document-engine/**`, `docs/implementation/bloco-05-*`, workflow próprio da spike e adendo na ADR. **Nada em** `apps/api`, `apps/web`, Flyway, infraestrutura institucional e modelos reais.
3. Manter `.runtime/` ignorado e o corpus histórico imutável. Nunca versionar DOCX/PDF pessoais, capturas autenticadas, fontes proprietárias, tokens JWT, callback URLs assinadas ou logs com segredos.
4. Criar runtime sintético próprio da Docs API; escolher portas loopback e redes isoladas. O laboratório ONLYOFFICE deve usar JWT ativo na nova integração — não transportar o `JWT_ENABLED=false` da comparação WOPI como baseline de segurança.
5. Documentar caminho entre navegador ↔ Docs Server ↔ host (URLs acessíveis *do contêiner*, não apenas do browser), comunicação TLS/WSS quando chegar ao gate de operação; não atribuir isolamento de egress à rede Docker sem prova.
6. Documentar se a instalação Community efetivamente expõe Docs API, plugins e recursos de aparência selecionados. Não habilitar licença/edição paga sem aprovação.
7. Não apagar volumes históricos para limpar backups. Usar identidade nova de documento/chave e ambiente isolado.

---

## 5. Arquitetura do ensaio (somente laboratório)

```text
                     Navegador / wrapper NITE experimental
                        |                       |
                   DocEditor (iframe)       estado confirmado pelo host
                        |                       ^
                  ONLYOFFICE Docs            API de consulta de versões
                        |                       |
                   callback(status) --> endpoint experimental /docs-api/callback
                                                |
                                 verificar JWT/chave/versão/origem do callback
                                                |
                                 baixar candidato do servidor permitido
                                                |
                           validar ZIP/OOXML + regras independentes de edição
                                  /                         \
                          APROVADO                         REPROVADO
                            |                                |
                   persistir versão imutável         quarentena privada limitada
                   atualizar current (CAS)          manter current e versões
                   responder sucesso               responder falha documentada
                            |                                |
                     estado confirmado             estado erro/pendente no NITE
```

**Separação obrigatória:** o callback não deve fazer proxy ao código WOPI artificialmente. Criar pequeno **adaptador de entrada do protocolo**, reutilizando as operações internas de versionamento e validação do host quando semanticamente apropriado. Não converter callback em PutFile fictício nem usar o banco de produção.

### 5.1 Modelo de estados próprio do NITE (wrapper experimental)

`editing-local` → `in-editor-service` → `save-requested` → `candidate-received` → `validation-pending` → `committed(version, checksum)` **OU** `rejected(reason)` / `timeout` / `recovery-required`.

Requisitos:

- Nunca renderizar “Salvo no NITE CONNECT” antes do estado `committed` lido do host.
- Recarregar a página ou fechar/reabrir não pode criar uma falsa versão confirmada.
- Eventos com status 1/4, mudanças locais ou callback 6 não são confirmação automática.
- Distinguir estado da edição em memória do motor do armazenamento NITE; mensagens internas do ONLYOFFICE não podem ser apagadas ou reivindicadas como próprias; exibir indicação externa clara quando divergem.
- Mostrar erro e recuperação possíveis sem divulgar detalhes internos, token, documento pessoal ou URL assinada.

### 5.2 Protocolo do callback

- Status Docs API a registrar: **1** (edição), **2** (arquivo pronto para salvamento após sessão), **3** (erro), **4** (fechado sem mudanças), **6** (*force save* com estado exportado), **7** (erro de *force save*).
- `document.key` identifica a sessão/versão da edição; deve ser renovada após salvar versão final conforme contrato do fornecedor, **não** em cada digitação. Não reutilizar chave entre trabalhos independentes; coeditores do mesmo trabalho compartilham chave autorizada.
- O servidor do NITE (host experimental) deve correlacionar chave, documento, revisão de entrada, usuário/escopo e callback específico **sem confiar cegamente nos campos declarados pelo cliente**.
- Para 2 ou 6: baixar o DOCX de URL autenticada/autorizada, verificar origem e tamanho, validar antes do commit, preservar original, criar versão somente por CAS correto e então responder `{"error":0}`. Não confirmar versão só porque chegou status 2/6.
- Para 3/7: registrar erro, não promover, expor falha ao wrapper, testar recuperação. Para 1/4: somente estado, sem inventar nova versão.
- Confirmar empiricamente **o que acontece com resposta de erro** (por exemplo HTTP 500 + `{"error":1}`), retry, timeout e mensagens do editor **no build fixado**; não assumir que Docs API garante recuperação por especificação.
- Proteção contra duplicatas/callback fora de ordem: idempotência pelo conjunto key + checkpoint + hash/identificador do callback e CAS; nunca criar duas versões iguais por replay nem sobrescrever uma versão mais recente.
- Em *force save*, verificar que os snapshots intermediários são distinguíveis do arquivo final. Configurar voluntariamente `forcesave` e comparar com o comportamento padrão.

### 5.3 Segurança focal do callback

Autenticar mensagens conforme JWT/assinatura suportados; validar claims, validade e chave; limitar *download URLs* ao serviço de edição controlado, bloqueando redirecionamentos externos/SSRF, IPs privados inesperados e DNS rebinding com política de rede; impor timeouts, streaming/limites de bytes, tipos MIME/ZIP e concorrência. Tokens e URLs devem ser efêmeros e não aparecer em logs. URLs `document.url` devem ser de leitura autorizada e limitadas ao documento/versão. Escopos reais de acesso continuam no backend do produto futuro, **não** delegados às permissões visuais da Docs API. Confirmar TLS/WSS e limites de callbacks antes de alegar G1 operacional.

---

## 6. Etapas com gates e regras de parada

### B05.0.3-A — Baseline factual, montagem e confronto de contratos

**Executar:** congelar SHA, versões, flags, XML de entrada, artefatos dos testes anteriores. Inventariar diferenças oficiais da API e WOPI **somente para funções que o NITE necessita**, incluindo disponibilidade de Docs API no build Community. Implementar pequena página de wrapper local (não `apps/web`) que abra F01 sintética por `DocEditor` com configuração e chave assinadas. Isolar credenciais localmente.

**Aceite A:** docs e scripts mostram abertura, edição autorizada e readonly; `document.key`, callback, identidade do usuário sintético e URLs são verificáveis. O corpus anterior não mudou. Se edição Docs API não está disponível na versão/edição sob teste, classificar bloqueio de licença/configuração e parar sem inventar teste.

### B05.0.3-B — *Save pipeline* real pela Docs API (**gate prioritário**)

Ordem fixa dos ensaios, sem duplicar toda a matriz anterior:

1. **Controle sem validação restritiva**, mas com DOCX estrutural válido: inserir `XYZ`, pedir salvamento, fechar, receber callback, persistir via host e recuperar bytes/hash/versão. Isso isola transporte Docs API versus recusa do gate B05.0.2.
2. **Repetir com validação ativa:** aplicar o contrato independente de conteúdo e campos; investigar cada delta reserializado em `styles/settings/document/header` com caso mínimo + Word. **Não autorizar todos os deltas em bloco**; se não conseguir atestar equivalência segura, parar.
3. **Force save enquanto edição permanece aberta:** solicitar via comando ou botão, observar callback 6, hash e versão. Testar sem forçar; distinguir 2,6,7 e atraso normal do callback.
4. **Recusa induzida:** `REF` sintético ou mídia alterada. Verificar erro no callback, ponteiro não avançado, aviso/estado externo correto e recuperação possível sem perda silenciosa. Salvar posteriormente candidato legítimo em sessão isolada.
5. **Falha operacional induzida:** callback indisponível, demora controlada, resposta não 0, arquivo rejeitado, callback duplicado, ordem invertida e fechamento precoce. Observar se a falha é recuperável de forma durável, inclusive depois de reinício do serviço/host.

**Gate B (binário):** comprovar ao menos um ciclo legítimo browser → candidato → callback → validação → versão imutável **aceita** → Word/bytes → reabertura; comprovar recusa sem sucesso e recuperação. Se o editor indicar salvamento mas o host não confirmou, a **interface NITE** não pode indicar sucesso e deve sinalizar pendência. Não há GO só porque “All changes saved” apareceu.

**Parada:** se a Docs API não permite diferenciar confirmação real, guardar/restituir candidato ou corrigir o contrato de rejeição, **não realizar ensaios oficiais**. Encerrar a alternativa sem repetir WOPI indefinidamente.

### B05.0.3-C — Fidelidade de SAVEDATE e documentos (somente se B passou)

**Primeira fase — sintética:** executar a mesma fixture de `SAVEDATE` simples (controle de falha) e o mesmo DOCX complexo normalizado `b0502-savedate-1` por Docs API; comparar com versão WOPI histórica, sem reexecutar todo o laboratório. `SAVEDATE` simples falhar não reprova automaticamente o caminho **se o produto decidir explicitamente usar cópia adaptada**; não esconder essa dependência. Verificar entrada, candidata, novo DOCX persistido, Word tipo 22, instrução/cache/atualização, PDF e paginação. Verificar se editor calcula o campo ao vivo ou apenas preserva semântica para Word; registrar necessidade funcional.

**Segunda fase — oficial autorizada:** se as fixtures e persistência passaram, criar apenas **cópias autorizadas** dos dois modelos oficiais em `.runtime`, conforme políticas de hashes/versionamento; nunca sobrescrever o modelo. Rodar C0 e C1 focais no AV3 + relatório, depois C2 se ambos passaram, com controle Word repaginado, campos/cabeçalhos/tabelas/mídias e PDFs derivados dos bytes efetivamente confirmados. Repetir o defeito corrigido duas vezes com IDs/cache isolados. Documentar que usuário editor e rota de edição foram comparáveis; diferenças relevantes ficam explícitas.

**Gate C:** sem `REF` inesperado ou `SAVEDATE` inválido, sem perda semântica e com datas válidas onde exigidas; relatório mantendo suas sete páginas de referência e AV3 suas cinco **mais** inspeção de layout/editorial, não apenas contagem; versões e SHA de cada checkpoint verificáveis; origem intacta. Aceite por responsável institucional **não é presumido**.

**Parada:** falha crítica conhecida em qualquer modelo ⇒ ONLYOFFICE continua NO-GO, não continuar tentando outras famílias de campos ou reformatar automaticamente o modelo para “passar”.

### B05.0.3-D — Experiência de produto pela Docs API (apenas com B/C tecnicamente viáveis)

Implementar **wrapper protótipo**, coerente com identidade do NITE CONNECT, e exercitar:

- Embed em layout de biblioteca, título, breadcrumbs, botão de voltar e barra externa de estado persistido; modo leitura/edição; desktop e viewport móvel **sem prometer edição móvel Community se a licença não suportar**.
- Tema escuro/claro (quando suportado), idioma pt-BR, toolbar compacta, esconder elementos opcionais, informações de usuário sintético, controle de comentários/revisão e downloads.
- Plugin inofensivo de demonstração **somente se suportado nessa edição**: carregar, ativar/desativar, receber evento sem acesso a arquivo privado/segredo. Não confundir disponibilidade de plugin com permissão de substituir integralmente UI/canvas.
- Histórico de versões **do NITE CONNECT** mostrado externamente e recuperado de versões imutáveis; controlar salvamento e downloads do original exato, não links não autorizados de motor.
- Dois usuários sintéticos: coedição simples, identificação consistente, comentários/alterações e descarte/revogação; sem alegar aprovação G1 de coedição concorrente em escala.
- Mapa de requisitos: o que Docs API fornece, o que o NITE implementa, o que requer licença específica e o que **não atende** (não inventar “tudo que SharePoint oferece”).

**Gate D:** protótipo de experiência convincente e factível **sem fork** e com controle do backend sobre versões/permissões. A interface oficial ONLYOFFICE pode coexistir com layout NITE; **não exigir remoção da marca na Community**, pois a licença restringe isso.

### B05.0.3-E — Licença, segurança, operação e custo

Checar edição Community de forma factual: limite técnico de **20 conexões simultâneas de edição**, exigência de marca e avisos, obrigações AGPLv3, inexistência de suporte/SLA comercial, limites de mobile/white-label/clustering. Os dados publicados pelo fornecedor não substituem consulta jurídica para o uso do NITE e eventual necessidade de Enterprise/Developer. Listar recursos de personalização permitidos efetivamente na edição instalada, não confundir parâmetros marcados “Developer only” com Community.

Produzir tabela de requisitos de capacidade (concorrência estimada, disponibilidade, hosting, backup, custos de implantação, atualizações e licenças). Sem contratar, aceitar licenças em nome da instituição ou ligar serviço externo. Atualizar P01–P20 da ADR **somente onde a Docs API altera riscos**: não copiar aprovação WOPI automaticamente. Segurança G1 completa não fica aprovada apenas pelo Gate B/C/D.

**Gate E:** matriz de licença/direitos e riscos conhecida; sem requisito essencial dependente de licença/condição indisponível ou não aprovada. Se dependência institucional inviabiliza a arquitetura, encerrar como NO-GO de produto, mesmo com DOCX íntegro.

### B05.0.3-F — Decisão final / encerramento do motor

Emitir relatório comparativo vinculando cada conclusão aos hashes, callback IDs, chaves, controles, Word e versões **sem revelar segredos/dados privados**. Atualizar ADR em adendo e registrar:

- **GO técnico condicional (Docs API):** Gate B + C + D + E atendidos, com aceite editorial e lacunas operacionais explícitas. OnlyOffice vira motor selecionado **sem liberar produção**. Abrir plano de fechamento G1 total, então G2 e integração backend Java/Flyway somente com autorização.
- **NO-GO definitivo ONLYOFFICE:** falha crítica persistente em B/C, recursos D impossíveis ou licença E inviável; congelar pesquisas ONLYOFFICE/WOPI e avançar para **seleção de outra arquitetura**. Próxima deliberação: avaliação institucional da rota Microsoft 365/SharePoint ou revisão formal dos modelos/escopo; **não adotar nenhuma automaticamente**.
- **Inconclusivo condicionado:** se depender de aceite editorial ou licença/admin externo ainda não acessível, registrar a pendência e interromper atividades técnicas, sem declarar sucesso.

Não abrir V6/documentos/Diário de Bordo ou alterar API até G1 e G2. Não reclassificar NO-GO histórico como sucesso.

---

## 7. Matriz focal T01–T32

**Regra:** os itens T01–T16 discriminam fluxo/protocolo; T17–T23 fidelidade; T24–T28 UX/capacidade; T29–T32 encerramento. Não rodar fases C/D/E se Gate B falhar.

| ID | Verificação | Evidência obrigatória |
|---|---|---|
| T01 | Freeze versão/digest, branch e histórico | Manifesto e status Git |
| T02 | Mesmo motor para WOPI/Docs API, dados isolados | Digests, datasets e keys |
| T03 | Abertura Docs API editor/readonly | Browser + config sanitizada |
| T04 | JWT config/documento, negativa de token inválido | Teste positivo/negativo |
| T05 | `document.key` nova versus cache de versão anterior | Fonte/hash correto |
| T06 | Insert + callback `status 2` + aceitação | Versão nova e hash host |
| T07 | `status 6` force save com editor aberto | Snapshot intermediário correto |
| T08 | Sem força: callback/tempo/fechamento normais | Timeline sem inferência |
| T09 | `onDocumentStateChange` não confirma NITE | UI mantém estado pendente |
| T10 | Recusa de REF conhecido | Sem commit + erro real |
| T11 | Recusa de mídia/estrutura crítica | Sem commit + quarentena |
| T12 | Replay/duplicata do mesmo callback | Idempotência sem nova versão |
| T13 | Callback fora de ordem, key obsoleta | CAS impede sobrescrita |
| T14 | Falha/timeout no download da candidata | Retenção, erro e recuperação |
| T15 | Restart e recuperação de não salvos | Prova durável, não apenas cache |
| T16 | Comparação objetiva com uma rodada WOPI focada | Decisão pelo protocolo, sem motor novo |
| T17 | `SAVEDATE` simples sem adaptação | Falha conhecida confirmada ou contraste demonstrado |
| T18 | Cópia com `SAVEDATE` complexo | Campo íntegro no arquivo salvo |
| T19 | Word controla tipo 22, cache e atualização | Probe Word do arquivo exato |
| T20 | Corpo e campos, estilos, relações, mídia | Diff semântico independente |
| T21 | AV3 C0/C1 normalizado | Word/PDF/inspeção, se B aprovado |
| T22 | Relatório C0/C1 | 7 páginas + fidelidade visual |
| T23 | C2 e duas rodadas independentes | Checkpoints/version/hash exatos |
| T24 | NITE wrapper/tema/toolbar | Screens e parâmetros suportados |
| T25 | Plugin sintético + recursos por edição | Execução real ou indisponibilidade |
| T26 | Histórico/download/versão autenticados | Consulta a objetos imutáveis |
| T27 | Permissões/coedição básica | Negativas e sessão real |
| T28 | Estado de erro/rejeição na UX | Mensagem clara e não enganosa |
| T29 | Direitos Community/AGPL/branding/conexões | Matriz licença e risco |
| T30 | Riscos e lacunas G1 operacionais | ADR sem aprovação presumida |
| T31 | Revisão independente e CI sintética | Logs, SHA, diff |
| T32 | Parecer GO/NO-GO e contingência decidida | Relatório final + ADR |

Para cada linha, usar estados `PASS`, `FAIL`, `BLOCKED`, `NOT-RUN`, `INCONCLUSIVE` e uma URL/path de evidência. **Nunca** contabilizar `NOT-RUN` como `PASS`. Separar resultado sintético e oficial.

---

## 8. Critérios de comparação decisória (não confundir com pontuação estética)

| Eixo | Requisito mínimo para selecionar ONLYOFFICE | Elimina candidato? |
|---|---|---|
| **Salvamento** | Callback real aceito com versão do host; rejeição comunicada e recuperável sem falsa confirmação NITE | **Sim** |
| **Integridade DOCX** | `SAVEDATE` e corpo/campos preservados nos modelos oficiais; validação não exige relaxamento cego | **Sim** |
| **Layout** | Paginação de referência e inspeção visual sem defeitos críticos | **Sim** |
| **Segurança** | Chaves/JWT, download restrito, versão/CAS, callback, permissões e privacidade verificáveis | **Sim** |
| **Experiência** | Embed e UX documental adequada; wrapper NITE pode refletir estado real | **Sim, se requisito essencial não atendido** |
| **Licença/operação** | Condições aprováveis para número de usuários, branding, suporte e infraestrutura | **Sim** |
| **Recursos adicionais** | Plugins, tema, revisão, histórico, mobile e customização documentados por edição | Avaliar por necessidade; não compensam falha crítica |

Não atribuir pontuação geral que possa fazer “mais plugins” superar corrupção documental. Se os critérios eliminatórios passarem, comparar diferenciais da Docs API versus WOPI em ficha simples e escolher a superfície que efetivamente agrega benefícios.

**Ponto de parada operacional:** uma única montagem Docs API, um fluxo completo de save positivo e um de recusa, depois **uma rodada focal de correção** somente se existir causa e teste RED→GREEN. Se continuar sem persistência aceita/recuperável, parar e emitir NO-GO. Outras tentativas precisam de decisão escrita prévia; não abrir nova sequência de versões do motor ou ajustes aleatórios do gate.

---

## 9. Contratos de aceite para o backend futuro — desenho, não implementação

Se e somente se a Docs API for selecionada condicionalmente, produzir artefato de arquitetura para futura integração ao Spring Boot:

- Domínio documental independente com identidade, autorização por usuário/papel/contexto, referência imutável de modelo original, working copy e versões validadas.
- Adaptador ONLYOFFICE Docs API isolado em módulo de infraestrutura; contrato de configuração/URL/callback/comando, sem expor detalhes do servidor de edição a outros domínios.
- Versionamento com transação, CAS/ETag, persistência de objetos e metadados, auditoria e recuperação; desenho de reconciliação idempotente e restauração antes de escolha concreta do storage.
- Indicador de persistência do NITE derivado exclusivamente do backend, independentemente da interface do editor.
- Riscos residuais P01–P20 e as decisões D01–D26 para G2 formalmente revisadas. Não fingir equivalência entre versões nativas do ONLYOFFICE e versões do NITE.
- Sem API endpoints de produto, schema, Flyway, migração, frontend persistente ou deploy nesta etapa.

---

## 10. Segurança, privacidade e qualidade

- Todas as fixtures oficiais sob `.runtime/` autorizado; não incluir em CI, issue, vendor ticket, artifact público, trace HAR ou PR. Cópias normalizadas **nunca** substituem originais.
- APIs de laboratório por loopback/rede isolada; evitar endpoints sem autenticação em ambiente acessível externamente. JWT de laboratório com segredo efêmero fora do Git.
- Resolver SSRF em download de `callback.url` e URLs de documentos; não seguir redirects arbitrários, limitar DNS/rede/bytes/timeouts. Validar DOCX antes de analisar e antes de versionar.
- Falha segura e observável: rejeição não cria versão, não publica PDF de candidata nem acusa corrupção sem evidência. Quarentena limitada e não acessível pelos endpoints de download, com manifesto privado por tentativa.
- Testar callbacks concorrentes/duplicados e a situação quando duas sessões legítimas compartilham a mesma `key` de coedição; negar sessões indevidas.
- Fonte e biblioteca sem atualização de links externos, macros e material embutido executável; limites e validador anteriores permanecem ativos.
- Documentar condições de licença/branding com fonte do fornecedor e aprovação institucional/jurídica futura, sem afirmar isenção pelo caráter acadêmico.

---

## 11. Testes, CI e validação executável

**Reutilizar:** suíte Python de adaptador/inventário; suíte Java do host e testes WOPI do PR #11 como regressão, mantendo nomes e números reais emitidos pelo comando. Não fixar contagem esperada (40/37) depois de adicionar novos testes.

**Novos testes mínimos necessários:**

- Testes Java do novo contrato Docs API: parsing/assinatura/callback por status, erro, replay, idempotência, proteção contra key obsoleta, fetch permitido, download falho, versão/CAS e quarentena.
- Testes de integração host × adaptador: arquivo sintético com campo complexo, aceite, recusa e integridade de bytes persistidos; não usar HTTP 200 no callback como substituto de hash.
- Browser automatizado: Docs API C1 positivo, recusa e recuperação, screenshots de estado, callbacks/versões correlacionados; não depende de Word na CI pública.
- CI GitHub: Python/Java + smoke sintético isolado na Docs API **quando executável com segurança no runner**; marcar `NOT-RUN` se não viável, não declarar verde falsamente. Testes Word COM e modelos oficiais **somente locais e privados**.
- Comandos preliminares conforme `AGENTS.md`, workflow e paths do laboratório. Comparar `git diff --check`, status de `.runtime` e verificar inexistência de arquivos privados versionados.
- Revisão independente com relatório de achados e correção de bloqueadores; nunca relatar revisão interna como approval formal do GitHub se ela não estiver registrada.

**Ordem de gate na CI:** A (configuração/serviço) → B (callback e persistência) → C (fidelidade, local) → D (interface) → E (licença) → F (decisão). CI verde é requisito necessário, **não** homologação.

---

## 12. Entregáveis versionáveis recomendados

Todos novos sob o mesmo diretório experimental, sem substituir os arquivos históricos:

```text
docs/implementation/bloco-05-docs-api-comparison-plan.md
experiments/b05-document-engine/
  results/b05-docs-api-baseline.md
  results/b05-docs-api-save-contract.md
  results/b05-docs-api-fidelity.md
  results/b05-docs-api-ux-features.md
  results/b05-docs-api-licensing.md
  results/b05-docs-api-final-decision.md
  scripts/DOCS-API-B0503.md
  scripts/docs_api_save_probe.cjs
  scripts/docs_api_cases.py          # somente se necessário
  wopi-host/                          # adaptação experimental isolada
  .runtime/b0503/                     # ignorado no Git
```

Possíveis fontes adicionais (nome exato a escolher na execução) para configuração `DocEditor`, endpoint do callback, teste de JWT/callback e testes Java. Não introduzir estrutura grandiosa de produção para uma comparação curta.

O **relatório final** deverá conter: SHA/PR base; digest, licença e parâmetros; matriz T01–T32; diagrama/timeline de callback versus host; exemplo anonimizado das mensagens; IDs da versão e hashes privados; comparativo WOPI × Docs API (diferenças causais, não suposições); leitura Word e screenshots relevantes; limitações de hardware/licença; decisão assinável GO/NO-GO/inconclusiva; atualização aditiva de `ADR-B05-001.md`; trilha para backend ou descarte do ONLYOFFICE.

---

## 13. Sequência de execução para agente/equipe

1. Ler especificação consolidada, `AGENTS.md`, histórico B05.0–B05.0.2 e este plano; confirmar existência dos PRs, SHA e estado de branch.
2. Criar branch empilhada adequadamente; congelar baseline e isolar datasets, credenciais e cache; não editar produto.
3. Implementar wrapper Docs API sintético e endpoint de callback no host experimental com JWT; obter **primeiro save positivo** com versão no host.
4. Executar recusa e recuperação reais; comparar uma única vez com o comportamento WOPI conhecido. Não habilitar modelos oficiais enquanto persistência e estado estiverem incertos.
5. Se Gate B passar, usar adaptador anterior sem alterar os originais; comprovar `SAVEDATE` com a fixture de core properties e testar cópias oficiais autorizadas.
6. Se Gate C passar, testar recursos de editor, permissões e UX do wrapper NITE. Verificar edição Community e dependências comerciais antes de concluir adequação.
7. Executar revisões/CI aplicáveis, restaurar ambiente e conferir históricos. Publicar somente código, documentação, fixtures sintéticas autorizadas e metadados não sensíveis.
8. Atualizar ADR e registrar decisão sem forçar GO; abrir PR, **não mergear**. Não iniciar backend real, V6 ou frontend documental.

### Instrução operacional pronta para repasse

> **Executar B05.0.3 conforme este plano.** Foco exclusivo no ONLYOFFICE Docs API versus o baseline WOPI do mesmo ONLYOFFICE; Collabora e LibreOffice fora de todo experimento novo. Não refazer corpus, repetir hipóteses refutadas ou alterar `apps/api`/`apps/web`/Flyway/modelos oficiais originais. Priorizar um ciclo real Docs API com callback seguro, candidato validado, versão imutável e confirmação externa do NITE; em seguida recusa/recuperação comprovadas. Parar no Gate B se o contrato de salvamento não for confiável. Apenas depois testar o adaptador `SAVEDATE` e os dois modelos oficiais em cópias privadas, seguido da experiência de interface/plugins e análise de licença. Manter PR empilhado e evidências privadas; publicar relatório T01–T32, CI e adendo ADR; concluir com GO técnico condicional ou NO-GO/inconclusivo fundamentado, sem avançar G1/G2 ou produto por inferência.

---

## 14. Critério de conclusão formal

**B05.0.3 está concluída como investigação** quando o relatório e a ADR apresentam evidência reproduzível e um veredito por requisito, inclusive os não executados. **ONLYOFFICE está aprovado como arquitetura candidata** somente com os Gates B/C/D/E atendidos e aceite editorial/autoridade documentados; G1 total e G2 ainda exigem aprovações próprias. **ONLYOFFICE deve ser descartado** se falhar um critério eliminatório após a rodada focal autorizada. Não escolher Microsoft 365, outro editor ou revisão de modelos sem nova decisão expressa.

*Este plano prepara a execução; nenhum código, branch, PR ou modelo oficial foi modificado por sua elaboração.*
