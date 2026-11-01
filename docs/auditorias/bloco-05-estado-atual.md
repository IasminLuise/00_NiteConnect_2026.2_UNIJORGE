# Auditoria técnica — Bloco 05: Gestão Documental e Diário de Bordo

> **Registro histórico:** descreve o checkout auditado em 30/10/2026. As referências a horas, justificativas ou frequência do B04 refletem o contrato anterior e não são requisitos ativos após a decisão de substituição de 01/11/2026. Consultar os documentos B04/B07 ativos em [`docs/implementation`](../implementation/bloco-04.md).

**Data da auditoria:** 2026-10-30
**Checkout:** `main`, commit `4988778db72543ce6fabfd2391dd2779b0066e13` (`2026-10-29`, merge de `main`).
**Estado observado antes da auditoria:** `main...origin/main`; a única alteração não rastreada era a pasta `docs/auditorias/`, que já continha `bloco-07-contratos.md`. O relatório do B07 não foi editado; seu SHA-256 antes e depois foi conferido como `E254972755D3968AE10DF031E14ACAB3D986A4466DDBD06852E81E31BB1FCB00`. Este arquivo é a única adição desta tarefa.

## 1. Escopo, método e critério de evidência

A auditoria é de leitura e descreve o checkout indicado, não um ambiente de produção nem o estado atual de qualquer banco remoto. Foram inspecionados código Java, migrações Flyway, documentação técnica/funcional versionada e histórico Git dos commits que adicionaram e removeram o B05. **Código atual é evidência de operação atual; migrações sem repositórios/serviços são apenas estruturas persistentes; commits anteriores provam existência histórica, não disponibilidade no `main`.** Documentos funcionais definem intenção, não implementação.

Os requisitos consolidados são a referência funcional: a especificação consolidada determina que os PDFs anteriores não prevalecem quando divergirem (PDF consolidado v1.0, p. 2). Ela descreve Diário periódico individual, documentos associados a contexto e edição/exportação com editor a validar (p. 7, 10–13 e 15). Requisitos de privacidade, segurança de arquivos e fidelidade de importação/edição/exportação constam na p. 12. Nomenclatura e periodicidade do diário, inventário de modelos e infraestrutura institucional permanecem pontos a definir na especificação (p. 13 e Q20–Q23, p. 15).

**Classificações:**

- **Implementado:** há código operacional no checkout atual que executa o contrato.
- **Parcial:** existe parte operacional, mas faltam estados, fluxo, cobertura ou integração necessários.
- **Apenas especificado:** consta em documento, migração ou contrato sem implementação operacional atual.
- **Ausente:** não foi encontrada implementação nem contrato operacional atual no escopo examinado.
- **Não verificável:** depende de ambiente, decisão ou artefato fora do checkout auditado.

## 2. Síntese do estado atual

**O Bloco 05 não está operacional no `main`.** O catálogo Flyway conserva as migrações documentais V6, V7 e V10, mas as identifica expressamente como estruturas históricas retidas devido ao histórico de bancos já migrados (`apps/api/src/main/resources/Migrations/README.md:L12-L16`). Não há pacotes B05 atuais de documentos/diários, controladores, serviços ou repositórios. A configuração de segurança termina em negação por padrão (`apps/api/src/main/java/br/edu/unijorge/niteconnect/shared/security/SecurityConfiguration.java:L84-L94`), e as rotas de documentos/diários vistas em um commit anterior não pertencem ao checkout.

O histórico demonstra que um núcleo de documentos e Diário contínuo, com API, serviços e testes, foi implementado no commit `49f5100` (mensagem “feat(documents): add document core and continuous diary (#20)”) e teve integração de revisão local no `dabdf9c` (#22). O commit `0938f94` removeu o código de produto B05 da linha principal. Portanto, “já foi implementado em branch/commit histórico” não equivale a “está disponível, integrado ou pronto para reativação”.

| Capacidade | Estado no checkout atual | Evidência / limite |
|---|---|---|
| Persistência documental e de versões | **Apenas especificado** | Esquema remanescente em V6, sem repositórios/serviços B05 no `main` (`Migrations/README.md:L12-L16`; V6:L1-L73). |
| Diário individual contínuo | **Apenas especificado** | V7 guarda vínculo e proveniência de importação; não há serviço/API atual (`V7:L1-L24`). |
| APIs de documento, editor e diário | **Ausente** | Existiram no commit histórico `49f5100`; não estão no código atual nem nas rotas permitidas da configuração de segurança. |
| Editor ONLYOFFICE | **Não disponível no produto atual; decisão histórica** | ADR histórico selecionou ONLYOFFICE com ressalvas, manteve NO-GO editorial para documento oficial e não autorizou integração/produção (`dabdf9c:docs/implementation/adr-b05-005-selecao-motor-documental.md:L5-L7,L20-L39,L54-L57`). |
| Documento oficial/PDF institucional | **Ausente / não homologado** | O ADR registra que o fluxo salvou bytes, mas falhou no gate de fidelidade, e que licenciamento, armazenamento e operação não foram aprovados (`dabdf9c:docs/implementation/adr-b05-005-selecao-motor-documental.md:L23-L39`). |
| Evidência documental para B06 | **Parcial** | Interface `DocumentEvidenceReader` e validação de snapshot existem; nenhum provider de produto. Sem provider, captura retorna conflito (`AcademicEvidenceService.java:L41-L48`; `DocumentEvidenceReader.java:L7-L10`; `bloco-06-api.md:L45-L46`). |
| Armazenamento de arquivos atual reutilizável | **Parcial, escopo diferente** | `LocalEvidenceObjectStore` atende evidências/attachments B03, opt-in local, não é armazenamento B05 (`LocalEvidenceObjectStore.java:L17-L36`). |
| Estado do banco remoto | **Não verificável** | Nenhuma conexão/consulta a banco foi feita; histórico de migrações não prova o estado atual de ambiente algum. |

## 3. Inventário por camada e contratos

### 3.1 Migrações V6, V7 e V10

A V6 cria catálogo e versões imutáveis de modelos, documentos com contexto (`PERSONAL`, `PROJECT`, `EVENT`, `ADMINISTRATIVE`), versões binárias com hash/tamanho/estado, concessões de acesso, ciclos de editor, grants temporários de origem, recibos de callback, PDFs versionados e eventos documentais. Há índices para versões, proprietário/projeto e histórico, além de triggers de imutabilidade para versões de modelo, versões documentais, PDFs e eventos (`V6__document_management.sql:L1-L73`). A migração amplia os tipos aceitos pela auditoria genérica para `DOCUMENT` e `DOCUMENT_TEMPLATE` (V6:L75-L80).

A V7 limita a um Diário por integrante (`member_id` como PK) e impede duplicar `document_id`; registra importador, instante, motivo e metadados de origem SharePoint. Um trigger torna a proveniência imutável (`V7__continuous_member_diaries.sql:L1-L24`). O conteúdo fica apontado para `documents`; a tabela `diaries` não define entradas, períodos, submissões, estados de revisão ou associação explícita às atividades/projetos. Essa estrutura, isoladamente, não implementa edição contínua nem ciclo do Diário.

A V10 cria `document_inspection_sessions`, com documento, versão, ator, chave do editor, status, validade e último status do provider; está descrita como inspeção local opt-in que não promove versão nem altera o ciclo normal (`V10__local_document_inspection.sql:L1-L14`; README:L16). Não foi encontrada API/serviço de inspeção operacional no `main`.

**Implicação de compatibilidade:** as tabelas impõem restrições úteis a uma futura implementação, mas não são interfaces de aplicação prontas. V6 não contém `activity_id` em `documents`; V7 não modela itens diários nem períodos/validação. Alterações de esquema precisam ser aditivas, em nova migração, respeitando o catálogo append-only (`Migrations/README.md:L3,L18-L20`). A forma como V6/V7/V10 foram aplicadas em cada banco não é verificável por esta auditoria.

### 3.2 APIs e fluxo histórico (não operacionais no `main`)

No commit `49f5100`, o `DocumentController` expunha catálogo e versões de modelos, criação/listagem/leitura de documentos, histórico, upload/download de versão, grants/revogação, arquivamento/restauração, sessões de editor e PDF (`49f5100:apps/api/src/main/java/br/edu/unijorge/niteconnect/documents/api/DocumentController.java:L15-L42`). O upload histórico limitava DOCX a 8 MiB e os downloads usavam `no-store`, `nosniff` e versão identificada no header (L32-L49). Havia controlador separado para leitura da origem e callback do provider (código no mesmo commit, pacote `documents/api`).

O `DocumentService` histórico aplicava versão esperada e lock transacional, preservava versão baseada na anterior e atualizava a cabeça somente para conteúdo aceito; candidata recusada era retida em quarentena (commit `49f5100`, `documents/application/DocumentService.java:L119-L129`). Grants exigiam administrador, vínculo ativo ao projeto para destinatário e revogavam sessões; Diário não aceitava grant genérico nem arquivamento/restauração genérica (L131-L150). Eventos do documento também eram enviados ao `AuditTrail` (L152). São decisões verificáveis do código daquele commit, mas não contratos ativos do checkout.

O editor histórico tinha abstração `DocumentEditorProvider` e serviço de sessão. A sessão era curta (15 min), com capability assinada HMAC, verificação de callback, detecção de replay, checagem de versão corrente e quarentena de salvamento atrasado/desatualizado; o PDF era associado a uma versão imutável (`49f5100:apps/api/src/main/java/br/edu/unijorge/niteconnect/documents/application/DocumentEditorService.java:L20-L42,L43-L74,L83-L123,L126-L138`). O comentário no fluxo distingue recibo durável de aceite editorial (L120-L123). Isso não remove o impedimento de fidelidade editorial do ADR.

A API histórica de Diário tinha importação inicial de DOCX por administrador, leitura própria ou por auditor autorizado e painel de atividades/evidências por intervalo de até sete dias (`49f5100:apps/api/src/main/java/br/edu/unijorge/niteconnect/diaries/api/DiaryController.java:L23-L67`). A política permitia ler próprio Diário; coordenador/gestor geral podiam auditar; edição era apenas pelo titular; importação exigia esses papéis administrativos (`49f5100:apps/api/src/main/java/br/edu/unijorge/niteconnect/diaries/application/DiaryAccessPolicy.java:L10-L27`). O serviço importava um único documento pessoal, proibia segundo Diário, registrava proveniência e recuperava atividades e evidências B03 para um painel (`49f5100:apps/api/src/main/java/br/edu/unijorge/niteconnect/diaries/application/DiaryService.java:L45-L76`). Não havia no fluxo examinado um modelo de registros datados do Diário, validação semanal ou horas derivadas; o painel não equivale a um diário estruturado.

### 3.3 Segurança, armazenamento e integridade

**Atual:** a V6 mantém constraints de contexto, FKs, tamanhos, hashes e triggers imutáveis; a V7 protege proveniência. Isso é integridade do esquema, não prova de autorização em endpoint. A implementação de B03 `LocalEvidenceObjectStore` declara ser somente adapter local de desenvolvimento/teste e não deve ser exposta como conteúdo estático; quando não configurada fica indisponível (`apps/api/src/main/java/br/edu/unijorge/niteconnect/activities/infrastructure/LocalEvidenceObjectStore.java:L17-L36`). Não se deve conectar esse adapter diretamente a documentos oficiais sem contrato e revisão de isolamento/retensão.

**Histórico:** testes existentes no commit `49f5100` cobriam políticas de acesso de Diário, isolamento entre documentos, concorrência de versão, quarentena, grants, sessões/callback, armazenamento privado, limites/hash e migração. Arquivos de teste: `DiaryAccessPolicyTest`, `DiaryWorkflowTest`, `DocumentAccessConcurrencyTest`, `DocumentCoreTest`, `DocumentEditorWorkflowTest`, `DocumentMigrationTest`, `DocumentObjectStoreTest` e `DocumentTemplateCatalogTest` (árvore `apps/api/src/test` do commit). Eles indicam escopo de proteção então implementado, mas foram removidos com o código e não validam o `main` atual. Não foi feita execução de testes nesta auditoria.

### 3.4 Diário e dependências B03, B04, B06 e B07

- **B03 — atividades/evidências:** o painel de Diário histórico agregava consultas autorizadas de atividades e anexos por intervalo; não criava um vínculo durável atividade–entrada do Diário nem reconciliação de horas. Hoje a infraestrutura de evidência B03 continua separada; sua interface de armazenamento não deve ser tratada como repositório documental.
- **B04 — agenda/frequência/trabalho:** V6 referencia `calendar_events` para documentos no contexto `EVENT`, mas isso não constitui API B05 atual. Não há contrato B05 atual para ler presença, duração ou justificativas. Regras de apuração de frequência e work logs não se convertem automaticamente em registro de Diário ou carga horária documental.
- **B06 — avaliação:** `DocumentEvidenceReader.read(actor, member, source, from, to)` exige que o provider autorize a leitura e coordene consistência de versão com a captura acadêmica (`DocumentEvidenceReader.java:L7-L10`). `AcademicEvidenceService.current` valida versão do contrato, origem/ID, integrante, intervalo, autor da leitura, revisão e atributos; se não há provider, responde conflito “Integração de evidências documentais indisponível” (`AcademicEvidenceService.java:L32-L48`). Testes acadêmicos usam provider sintético somente no classpath de teste (`AcademicSyntheticSupport.java:L13-L21`); teste também verifica o conflito sem provider (`AcademicEvidenceTest.java:L24-L30`). A evidência documental aprovada não pode ser considerada disponível até integração real, autorização contextual e snapshot coerente. O próprio contrato B06 explicita a pendência (`docs/implementation/bloco-06-api.md:L45-L46`).
- **B07 — indicadores e relatórios:** a auditoria atual não encontrou API B05 ativa para fornecer documentos, validações ou eventos. Indicadores por estado de evidência, trilha temporal ou exportação PDF/CSV dependem de fonte operacional e semântica de estados estáveis; não se devem agregar diretamente linhas de tabelas históricas como se fossem eventos de negócio válidos.

## 4. Arquitetura e decisões anteriores

O desenho funcional consolidado prevê monólito modular Spring/PostgreSQL com serviço de arquivos e editor integrado, mantendo responsabilidades de documentos e integração no backend; é orientação de arquitetura, não implantação (PDF consolidado, p. 13). O código histórico refletiu separação de API, aplicação, domínio, repositório, armazenamento e provider de editor. O checkout atual reteve parte do esquema e perdeu as camadas operacionais.

O ADR B05-005, disponível no histórico `dabdf9c`, registra ONLYOFFICE como escolha arquitetural aprovada com ressalvas, mas mantém **NO-GO para edição de documentos oficiais** até superar a fidelidade Word/PDF. O teste B05.0.3 citado salvou bytes, mas gate estrutural recusou a candidata e comparação Word mostrou `SpaceAfter` de 8 pt versus 0 pt e deslocamento do quarto parágrafo de 149,25 pt versus 117,75 pt (ADR histórico, L5-L7,L23-L29). O ADR condiciona a continuidade a recuperar/repetir os artefatos, validar SAVEDATE/F9 no runtime efetivo, comprovar round-trip DOCX/PDF e obter aceite; integração de produto, storage, autorização, callback, versionamento, recuperação e rollback são decisão separada (L33-L39,L54-L57). O texto também registra que não havia aprovação institucional/produção (L56-L57).

Assim, seleção do motor não é autorização para ligar um serviço, usar documentos institucionais ou declarar fidelidade. Os PDFs de requisitos anteriores contêm regras antigas (por exemplo, obrigatoriedade de evidência/plano e pesos fixos); como a especificação consolidada declara precedência, essas regras não devem ser importadas como contrato atual sem confirmação institucional (PDF consolidado p. 2; PDFs anteriores, p. 12–20).

## 5. Lacunas e impedimentos

1. **Ausência de produto B05 no `main` — impeditivo técnico imediato.** Rotas, serviços, políticas e testes de produto foram retirados. V6/V7/V10 não são API e qualquer reativação exige revisão, portabilidade e integração ao estado atual de B01–B06.
2. **Sem modelo operacional de Diário — impeditivo funcional.** A tabela V7 só relaciona integrante a documento importado. Falta decidir se registros são texto estruturado, documentos editáveis em períodos, ou coexistência com referências; falta ciclo de declaração, submissão, validação, correção, fechamento e histórico.
3. **Sem armazenamento B05 configurado/contratado — impeditivo de operação.** O adapter local B03 não é solução institucional. Permanecem por decidir provedor, backup, retenção, recuperação, disponibilidade, criptografia, limites, varredura e ciclo de vida de objetos.
4. **Fidelidade/aceite de editor oficial bloqueados.** A decisão histórica de motor tem gate editorial NO-GO. Documentos oficiais não podem ser editados/exportados pelo motor com base apenas no protótipo histórico.
5. **Integração B06 incompleta.** Sem provider de `DocumentEvidenceReader`, capturas documentais não funcionam no produto; snapshots precisam definir que versão/estado conta como evidência no período e garantir autorização e consistência transacional.
6. **Sem contrato de estados canônicos.** No histórico havia `AVAILABLE`/`QUARANTINED` para versão, estados de ciclo de editor e eventos; isso não define estado de validação de conteúdo/Diário (declarado, submetido, validado, homologado), quem decide nem efeitos de correção. B07 requer essas distinções sem inferir que estado técnico do arquivo equivale a homologação acadêmica.
7. **Sem semântica institucional para período e horas.** Especificação funcional deixou periodicidade/nome para validação. O adendo do B07 fixa 5h presenciais e 4h remotas, mas isso exige contrato entre B04 e B05 e política de contabilização (período, fuso, arredondamento, sobreposição, correção e fechamento); não é executável por esquema atual.
8. **Sem modelos documentais institucionais definidos.** Inventário, donos, versões, campos, assinatura, evidência oficial e manutenção de modelos precisam de aprovação. A tabela de templates não garante que o catálogo real esteja aprovado.
9. **Segurança contextual não está revalidada no código atual.** As políticas históricas eram específicas (titular, auditores globais, administrador de grants) e não mapeiam automaticamente aos perfis/contextos agora requeridos. Necessário revisar menor privilégio, isolamento por projeto, gestores, coordenação, auditoria de leitura/download/exportação e proteção de atributos privados.
10. **Histórico de aplicação/ambientes não verificável.** V6/V7/V10 são append-only e descritas como já existentes em históricos de bancos; sem acesso ao histórico Flyway de cada ambiente, não há base para alterar ou repetir migrações.
11. **Relatórios e consulta analítica sem fonte estável.** Não há endpoints B05, leitura histórica ativa, filtros de autorização ou política de retenção. Semântica, fuso e corte temporal de relatórios e exportações ainda precisam de contrato; agregações não devem expor conteúdo/documentos privados.

## 6. Matriz de compatibilidade com necessidades do B07

Legenda de atendimento: **Direto** = existe contrato atual utilizável; **Parcial** = fonte adjacente ou estrutura histórica reaproveitável, com lacunas; **Não atendido** = inexistente no produto; **Decisão** = requer política/aceite institucional. O “nível” é sobre o checkout auditado, não sobre commits anteriores.

| Necessidade do B07 | Fonte/contrato existente | Atendimento | Impacto / condicionante |
|---|---|---|---|
| Visões por integrante, gestor, gestão geral e coordenação | Papéis e autorização dos módulos-base; histórico B05 continha política própria | **Não atendido / decisão** | Definir matriz de ações por contexto, objeto e dado derivado; não ampliar escopo por papel global sem regra explícita. |
| Evidências documentais identificáveis e consultáveis | `DocumentEvidenceReader` B06 sem provider operacional; V6 histórica | **Não atendido** | Implementar integração B05↔B06 com autorização de origem e snapshot versionado. |
| Dados declarados, validados, preliminares e homologados | B06 tem status/captura/homologação; estados B05 inexistentes atualmente | **Parcial** | Mapear estados de negócio separados de disponibilidade/quarentena do arquivo; definir transições e autoridade. |
| Carga semanal: 5h presencial e 4h remota, apuradas separadamente | B04 tem registros de presença/trabalho, sem contrato B05 de reconciliação | **Parcial / decisão** | Definir período, fuso, sobreposição e fonte canônica de cada hora. Evitar soma duplicada entre reunião, atividade e documento. |
| Trabalho remoto por períodos vinculado a reunião/atividade/documentação | V5 é fonte adjacente; Diário histórico só mostrava atividades/evidências | **Não atendido** | Definir entidade de período remoto, vínculos múltiplos sem dupla contagem, prova e edição/correção. |
| Validação semanal do trabalho remoto | Nenhum fluxo operacional B05 encontrado | **Não atendido / decisão** | Definir validador, prazo, devolução, bloqueio/correção e efeitos da validação. |
| Justificativas: ausência, dispensa e reposição | B04 possui domínio de justificativas, mas não foi encontrado contrato B05 de efeito nos indicadores | **Parcial / decisão** | Preservar tipos e tratamentos distintos; definir se dispensa altera meta, reposição credita horas e ausência apenas explica. |
| Reunião presencial com QR temporário; futura integração Meet | V4/V5 podem relacionar evento/frequência; nenhuma API documental ou contrato QR B05 atual | **Parcial** | QR e Meet são integração/fluxo separado; Meet deve permanecer opcional, com fonte de presença e política antifraude decididas. |
| Criticidade parametrizada por projeto | V6 permite documento associado a projeto; nenhum parâmetro de criticidade B05 encontrado | **Não atendido / decisão** | Definir schema, limites, versionamento e administradores autorizados (gestão geral/coordenação). |
| Alertas configuráveis por projeto | Sem serviço B05 de alertas documentais ou configuração de criticidade | **Não atendido** | Requer evento/fonte confiável, limiares, destinatários, frequência, silenciamento e trilha de alteração. |
| Histórico relacional projeto–integrante–atividade–documento | V6 conhece project; V7 member; painel histórico consulta atividade/evidência, sem associação documental persistente com atividade | **Parcial** | Precisará de modelo de relações e snapshots temporais. Não inferir relação apenas por proximidade de datas/títulos. |
| Indicadores históricos/rastreáveis | V6 preservava versões/eventos; não há serviço ativo e V7 guarda proveniência de importação | **Parcial** | Reaproveitar esquema após validação; definir log de transições, ator, instante, origem, revisão e corte de agregação. |
| PDFs/CSVs por perfil | PDF de documento era conversão histórica via editor; não existe exportação B05 analítica atual | **Não atendido** | Contrato de filtros e redaction por autorização; PDF documental não equivale a relatório PDF. CSV exige estabilidade de colunas e regras de exposição. |
| Armazenamento, integridade e recuperação | Hash/versão/constraints históricos; storage B03 local só desenvolvimento | **Parcial** | Selecionar storage institucional e testar backup/restore, objetos órfãos, retenção, integridade e indisponibilidade. |
| Integração futura com B06 | Interface de leitura e validações já definidas no B06 | **Parcial** | É o ponto reutilizável mais concreto, mas dependente do provider B05 e do modelo de evidência aprovado. |

## 7. Alternativas e recomendações técnicas

As alternativas abaixo são encaminhamentos para decisão; nenhuma é implementação nem escolha institucional já aprovada.

### A. Reativar o núcleo histórico, com revalidação

**Vantagens:** recupera APIs, services e testes existentes, inclusive versionamento, quarentena, autorização e callback; reduz tempo de redescoberta. **Limitações:** código foi removido e não é compatível presumidamente com estado atual; exige cherry-pick/portabilidade seletiva (não aplicar commit completo), auditoria de segurança e contratos, reexecução de testes e atualização do `DocumentEvidenceReader`. O risco principal é tratar testes antigos como prova de compatibilidade atual.

### B. Reimplementar incrementalmente sobre as migrações mantidas

**Vantagens:** integra explicitamente apenas contratos necessários, respeitando módulos e estado corrente; permite separar armazenamento, Diário, editor e integração acadêmica. **Limitações:** exige restabelecer APIs, autorização e testes desde o início; risco de divergência das tabelas antigas, que foram preservadas como histórico e não necessariamente são o modelo institucional final.

### C. Separar Diário estruturado do documento editável

**Vantagens:** permite registros periódicos, horas, referências a atividades/reuniões, validação e métricas tipadas sem extrair semântica de texto DOCX; editor vira anexo/representação, sem fonte de cálculo. **Limitações:** exige desenho institucional do ciclo, retenção, autoria, correções e coesão entre registro e artefato. É recomendável se indicadores dependem de dados estruturados; validação da responsável de produto é necessária.

### D. Diário como documento contínuo com dados estruturados auxiliares

**Vantagens:** mais próximo da V7 e do modelo de edição contínua; conserva narrativa livre e modelos documentais. **Limitações:** manter agregados confiáveis demanda metadados estruturados paralelos e regra inequívoca para sincronização do conteúdo editável; risco de divergência entre texto e horas/estados. Exige decidir se texto ou eventos estruturados são fonte oficial.

### Encaminhamento recomendado

Recomenda-se planejar em etapas: (1) fechar decisões institucionais e estados/semântica do Diário; (2) decidir fonte canônica para horas e vínculos com B04/B03; (3) contratar armazenamento e resolver o gate do editor; (4) aprovar matriz de autorização e auditoria; (5) implementar e testar o modelo/API B05; (6) integrar o `DocumentEvidenceReader` ao B06 com snapshot/revisão consistente; (7) só então definir consultas/indicadores/exports do B07. A separação entre registros estruturados e DOCX/PDF deve ser decidida antes do esquema: para métricas e alertas, o dado estruturado evita interpretação frágil de documentos livres.

### Decisões que requerem validação do responsável/instituição

- O Diário será estruturado, documental ou híbrido? Qual periodicidade, datas de corte, edição e estados/transições?
- Quais papéis podem criar, editar, validar, homologar, visualizar, baixar e exportar por contexto (pessoal/projeto/evento/administrativo)?
- Quem administra templates e parâmetros de criticidade? Qual fronteira entre gestão geral e coordenação?
- Qual é a fonte canônica e regra de deduplicação de horas presenciais/remotas, inclusive períodos sobrepostos e correções?
- Qual tratamento formal distingue explicação de ausência, dispensa e reposição, e como cada caso afeta a meta?
- Que evidência documental pode compor o pilar B06 e em que estado? Qual versão e janela temporal entram no snapshot acadêmico?
- ONLYOFFICE permanece o motor de produto após superar NO-GO? Quais modelos oficiais são aceitos e qual autoridade dá o aceite editorial/institucional?
- Qual storage, região, retenção, backup, descarte, limites, criptografia, recuperação e licenças são autorizados?
- Quais dados podem aparecer em alertas/relatórios e quais são restritos, inclusive conteúdo de documentos e justificativas?

## 8. Recomendações priorizadas

| Prioridade | Encaminhamento | Motivo / critério de saída |
|---|---|---|
| **P0 — antes do desenho detalhado** | Formalizar ciclo do Diário, semântica de estados e entidade/fonte canônica para períodos/horas. | Evita que métricas derivem de texto livre ou que B04/B05 contem o mesmo período mais de uma vez. |
| **P0** | Resolver armazenamento institucional e gate editorial do editor; reavaliar ONLYOFFICE e condições de operação. | ADR mantém NO-GO oficial e a V6 não configura storage operacional. |
| **P0** | Aprovar matriz de acesso contextual e dados visíveis por perfil/ação. | API atual B05 não existe; autorização histórica não é suficiente para B07. |
| **P1 — antes da implementação B05** | Fazer inventário de migrações Flyway por ambiente, sem repetir/editar versões aplicadas; elaborar migrações novas e aditivas. | V6/V7/V10 são append-only e já constam em históricos de bancos, segundo README. |
| **P1** | Definir interfaces entre B05–B03/B04/B06: referências, validação, snapshot, versões, fuso, falhas e idempotência. | B06 exige reader e consistência; fontes de atividade/frequência têm domínios próprios. |
| **P1** | Decidir reutilização seletiva ou reimplementação a partir do histórico `49f5100`; realizar revisão de compatibilidade e segurança antes de portar código. | O commit anterior demonstra capacidade técnica, mas não está integrado no `main`. |
| **P2 — antes do B07** | Instrumentar eventos/consulta histórica e definir semântica/denominadores dos indicadores, limiares de alerta e política de exportação. | Não há fonte operacional B05 ou estado canônico para relatórios atuais. |
| **P2** | Restaurar testes de contrato e regressão para autorização, concorrência, imutabilidade, isolamento, restore, callbacks, storage e snapshots B06. | Cobertura B05 identificada está apenas em commit removido. |

## 9. Conclusão

**Prontidão do Bloco 05: baixa no checkout atual; não está pronto para iniciar implementação de dashboards dependentes de seus dados nem para ser considerado concluído.** Há ativos úteis: migrações históricas com restrições de integridade, um núcleo de produto anteriormente implementado e testado em commits anteriores, uma seleção arquitetural histórica de ONLYOFFICE e uma interface B06 preparada para provider documental. Esses ativos reduzem incerteza, mas não substituem código operacional, integração atual, armazenamento institucional ou aceite.

O impedimento mais imediato é a ausência completa de API/serviços B05 ativos no `main`; o impedimento de produto mais relevante é não haver contrato aprovado para Diário estruturado, horas e estados rastreáveis. A edição de documentos oficiais permanece bloqueada pela evidência de NO-GO editorial registrada no ADR. B06 não pode capturar evidência documental real sem `DocumentEvidenceReader` de produto. Antes de planejar B07 em cima de documentos, horas, validações ou alertas, devem ser decididos fonte canônica, autorização, estados e política institucional. A evolução deve manter autoria e histórico nos módulos de origem e oferecer ao B07 projeções agregáveis com versão, período, origem e nível de validação explícitos.

**Validação desta entrega:** auditoria somente de leitura; nenhum teste, build, migração, serviço ou banco foi executado. Não houve alteração no código, nas migrações, nos contratos, nas configurações nem no relatório anterior do B07. O status final deve conter somente este arquivo novo além do estado pré-existente `?? docs/auditorias/`.
