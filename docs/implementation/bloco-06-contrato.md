# Bloco 06 — Contrato de Implementação B06.0

**Atualização da sequência de migrações (27/10/2026):** o domínio acadêmico usa V8/V9. O catálogo completo está em `apps/api/src/main/resources/Migrations/README.md`; as versões já aplicadas são append-only e permanecem no histórico do Flyway.

**Data:** 23/10/2026. **Estado atual:** contrato/plano aprovados pelo usuário para implementação nesta sessão; núcleo implementado, revisão independente tratada e 218 testes aprovados; PR #16 aberto para revisão. Ativação integral continua condicionada às decisões institucionais e à disponibilidade das evidências exigidas.
**G0-A:** aprovado com o plano de execução. **G0-B:** ambientes conhecidos auditados e V6 acadêmica adotada. **G0:** encerrado nesse escopo; ver [evidências de execução](bloco-06-execution.md). O texto abaixo preserva o contrato inicialmente submetido; estados históricos de preparação não substituem o registro atual.
**Branch:** `docs/bloco-06-contrato`. **Base:** `origin/main`, commit `d787916bbd581ee2ec4c9d2ee85fe5af02f97239`.

## 1. Objetivo, autoridade e limites

Transformar registros operacionais em uma avaliação acadêmica individual por semestre, com critérios versionados, evidências rastreáveis, revisão e homologação interna humana. O sistema prepara informações para encaminhamento; não decide nem lança a nota oficial da universidade.

Fontes, em ordem de aplicação:

1. Decisões posteriores explicitamente aprovadas pelo responsável pelo produto: quatro correções da auditoria B06.0 e três refinamentos aprovados em 23/10/2026, reproduzidos neste contrato.
2. [Especificação funcional consolidada v1.0](../product/NITE_Connect_Especificacao_Funcional_Consolidada_v1.0.pdf): RF-042–047 e RF-048/049 (p. 8), papéis e permissões (p. 4), F07/F08 (p. 10), CA-13/14/16 (p. 14) e Q24–29 (p. 15).
3. Código e contratos efetivamente existentes nos Blocos 01–04; PDFs históricos são fontes de origem, sem precedência sobre as decisões acima.

Classificação utilizada:

| Código | Significado | Efeito |
|---|---|---|
| D | Decisão funcional aprovada na conversa ou no PDF consolidado | Deve ser preservada; não equivale a homologação institucional |
| T | Escolha técnica proposta neste contrato | Sujeita à revisão de G0-A; nomes e assinaturas não representam código existente |
| I | Decisão institucional ainda pendente | Não atribuir valor ou procedimento por suposição; bloquear a operação dependente |
| E | Evidência observada no repositório ou ambiente | Limitada à referência e ao ambiente identificados |

**Fora desta entrega:** código de produto, migrações, endpoints, OpenAPI executável, frontend, implantação, alteração de credenciais, editor ou armazenamento documental. Não antecipar painéis do B07. Não considerar este documento aprovação da implementação.

### Abordagem selecionada e alternativas

A direção aprovada é manter `academic` como domínio do monólito, lendo contratos explícitos dos módulos de origem e preservando referências e retratos mínimos dos dados utilizados. Isso mantém o domínio independente de um editor documental e exige definir os contratos e a reconciliação de versões.

Consultas diretas do módulo acadêmico às tabelas internas de outros módulos reduziriam o trabalho inicial, mas contrariariam a fronteira arquitetural aprovada e duplicariam autorização. Um fluxo documental paralelo também duplicaria armazenamento e autorização sem aprovação. Ambas são descartadas; não são alternativas em aberto.

## 2. Base técnica auditada

| Constatação E | Consequência |
|---|---|
| Java 21, Spring Boot 4.1.1, Spring Security, JDBC, PostgreSQL e Flyway no backend | Preservar dependências e organização por domínio; não introduzir serviços externos |
| V1–V20 no checkout atual; V1–V19 são histórico append-only e V20 apenas documenta tabelas B04 aposentadas | Não remover migrações históricas nem inferir dados de produção a partir do checkout |
| `main` remota sete commits à frente da antiga referência local `e643408`; comparação contendo apenas documentos | Worktree documental parte da referência remota atualizada, sem transportar código experimental |
| `AccessService.requireAdministrator()` aceita `COORDENADOR` e `GESTOR_GERAL` | Não usar esse método isoladamente para registrar notas, homologar ou reabrir homologação |
| `MemberDirectory`, `ProjectDirectory` e `ActivityParticipation` são contratos já existentes | Reutilizar conceitos e verificações; eles não constituem leitores acadêmicos completos |
| `AttendanceQueries` expõe apenas agenda esperada e presenças próprias; horas/revisões não são contratos ativos | Capturar presença por evento sem derivar horas; histórico permanece sujeito à autorização acadêmica própria |
| `SecurityConfiguration` usa allowlist de operações e `anyRequest().denyAll()` | Toda futura operação acadêmica exige proteção HTTP e autorização contextual no serviço |
| Docker Engine indisponível na tentativa de consulta inicial | Histórico Flyway e execução PostgreSQL não foram verificados nesta auditoria |

Contagens históricas de testes do B04 descrevem escopos anteriores e não são referência para a validação atual. O histórico dos demais blocos não substitui a validação do código acadêmico.

## 3. Rastreabilidade funcional

Os identificadores abaixo pertencem à versão consolidada v1.0. Não os combinar com números iguais de PDFs antigos. CA-13 é um cenário transversal de adesão, evidências e ausência de lançamento oficial; CA-14 trata da preservação entre semestres. Os demais cenários são critérios técnicos derivados, não novos CA oficiais.

| Requisito | Decisão Q | Critério oficial relacionado | Responsabilidade B06 | Verificação futura |
|---|---|---|---|---|
| RF-042 — adesão semestral opcional | Q24 | CA-13 | Solicitação por integrante ativo, período e disciplinas; sem confundir vínculo NITE com AV3 | Sem adesão implícita; unicidade integrante/período; recusa de inativo, duplicidade e janela/limite inválidos |
| RF-043 — disciplinas e evidências | Q26 | CA-13 | Referências individuais e conferência de pertinência acadêmica | Negar evidência de outro contexto; identificar origem, contribuição, versão e decisão de pertinência |
| RF-044 — critérios configuráveis | Q25 | CA-14 | Política por período, pesos, escala, precisão e arredondamento versionados | Não ativar parâmetros ausentes; não alterar a política já utilizada; preservar resultados antigos |
| RF-045 — resultado preliminar | Q29 | CA-13 e CA-14 | Cálculo reproduzível com política válida, insumos e revisões identificados | `BigDecimal`; zero distinto de ausência; pilar ausente impede resultado completo; reprodução histórica |
| RF-046 — revisão | Q28 | Sem CA específico; CA-16 para alteração crítica | Pedido, análise, correção e decisão auditada, conforme rito institucional | Isolamento entre integrantes; janela/rito válidos; sem sobrescrita de homologação |
| RF-047 — consolidação e encaminhamento | Q27 | CA-13 | Homologação interna pela coordenação e encaminhamento separado | `GESTOR_GERAL` recebe 403 ao homologar; nenhuma escrita no portal universitário; confirmação externa depende de procedimento |

RF-048/049 e CA-16 são transversais: autoria, motivo, instante, estado anterior/posterior, confirmação de operações críticas e acesso restrito ao histórico.

O Plano Individual está no planejamento aprovado como capacidade do B06. O ciclo de versões será definido tecnicamente, mas sua obrigatoriedade como condição para pontuar ou submeter Diário de Bordo permanece **I**: não reintroduzir como norma a interpretação de PDF histórico. Evidências documentais sem provedor validado continuam indisponíveis para homologação.

## 4. Matriz de autorização acadêmica

**D:** autorização pedagógica distinta de administração. Verificar vínculo ativo e papéis atuais persistidos no backend em cada operação; não confiar apenas em claims de token. Liderança de projeto não concede acesso acadêmico a colegas. Uma pessoa com ambos os papéis pode agir como coordenador; os testes negativos usam gestor geral sem esse papel.

**T:** política específica no domínio acadêmico, nome candidato `AcademicAuthorizationPolicy.requireCoordinator()`. Não alterar indiscriminadamente `AccessService.requireAdministrator()` nem os contratos dos B01–B04.

| Operação | Integrante ativo | Gestor de projeto | Gestor geral | Coordenador ativo |
|---|---|---|---|---|
| Solicitar adesão e indicar disciplinas | Próprias, conforme política | Próprias, como integrante | Sem substituir solicitação do aluno | Sem substituir solicitação do aluno |
| Consultar acompanhamento acadêmico | Próprio, com divulgação conforme rito | Próprio; liderança não amplia acesso | Dados mínimos necessários à preparação | Conforme competência e contexto |
| Preparar referências e conferir integridade operacional | Próprias, sem validar a si mesmo academicamente | Próprias; opera B03 apenas no escopo já autorizado | Permitido no escopo administrativo descrito abaixo | Permitido |
| Aprovar elegibilidade e pertinência disciplina–trabalho | Não | Não | Não | Somente com critérios institucionais válidos |
| Registrar ou alterar pontuação dos pilares | Não | Não | Não | Permitido, com justificativa e evidências |
| Calcular resultado preliminar | Não dispara operação administrativa | Não | Permitido somente sobre insumos já avaliados, sem alterá-los | Permitido |
| Solicitar revisão | Própria, conforme rito | Própria | Não em nome do aluno; pode sinalizar inconsistência | Pode iniciar correção administrativa justificada |
| Decidir revisão de pontuação | Não | Não | Não | Permitido conforme rito |
| Homologar ou reabrir homologação interna | Não | Não | Não | Permitido com confirmação, motivo e auditoria |
| Encaminhar resultado interno | Não | Não | Preparar dados, sem declarar envio oficial | Executar conforme procedimento aprovado |
| Confirmar lançamento/convalidação externos | Não | Não | Não | Desabilitado até procedimento formal; depois registrar comprovação |
| Consultar auditoria acadêmica administrativa | Não; apenas andamento próprio autorizado | Sem privilégio adicional | Escopo mínimo de apoio, sem documentos sensíveis indiscriminados | Conforme competência |

O escopo de preparação do gestor geral é uma **T proposta**: organizar referências, consultar integridade e pendências operacionais, preparar dados para conferência e solicitar cálculo sem modificar notas. Não aprova política, elegibilidade, pertinência pedagógica, pontuação, revisão de nota ou homologação. Publicação/divulgação do resultado ao integrante depende do rito institucional; enquanto indefinido, acompanhamento não deve expor rascunhos de notas ou justificativas internas.

Para leitura de evidências operacionais, a autorização acadêmica não amplia automaticamente a permissão no módulo de origem. Verificar ambas. Caso uma permissão específica adicional seja necessária, aprová-la e testá-la antes de expor o conteúdo.

Verificações mínimas futuras: chamada HTTP direta de homologação por gestor geral → **403**; sem autenticação → bloqueio pela segurança HTTP; integrante inativo ou coordenador revogado → recusa; acesso cruzado entre integrantes/projetos → recusa sem exposição de conteúdo; chamada direta ao serviço também protegida. Negar operação antes de produzir efeitos ou revelar dados do alvo; o teste 403 usa um alvo válido conhecido.

## 5. Contratos de leitura intermodulares

Todos os leitores abaixo são **T propostas**, não classes existentes. Fornecidos pelo módulo dono do dado, via contratos internos explícitos; `academic` não importa seus repositórios nem consulta suas tabelas. Reutilizar os contratos atuais quando suficientes; acrescentar somente a projeção faltante.

### 5.1 Envelope mínimo de proveniência v1

| Grupo | Dados e semântica |
|---|---|
| Requisição | Ator autenticado, integrante alvo, referência de origem e intervalo do período acadêmico; finalidade de leitura para avaliação |
| Identidade | Tipo de origem, identificador persistente, integrante relacionado e vínculo que sustenta a contribuição individual |
| Contexto temporal | Período acadêmico solicitado e datas do fato; a associação ao semestre é do B06, não um campo presumido em B03/B04 |
| Revisão | Versão nativa ou identidade estável da revisão; versão do contrato separada da versão do dado |
| Responsáveis | Autor do registro e, se existir decisão de validação, responsável, instante e revisão dessa decisão |
| Validação | Estado nativo e significado; ausência de validação explicitamente informada, sem inventar aprovador |
| Coleta | Instante de coleta, projeção mínima dos dados usados e cobertura temporal da consulta |
| Autorização | Contexto efetivamente autorizado: ator, alvo, escopo pessoal/projeto/administrativo e decisão do provedor; não retornar bearer token ou permissão reutilizável |

Não presumir contador de versão em toda origem. Para dados imutáveis, usar identidade de revisão; para conteúdo sem versão nativa, definir token determinístico da projeção relevante e seu esquema de serialização. Esse token detecta mudança, mas não prova autoria nem substitui autorização. Paginação com ordem estável, limites e filtros de período explícitos é obrigatória para coleções; resultados parciais não podem ser apresentados como captura completa.

### 5.2 Provedores e lacunas identificadas

| Origem / leitor candidato | Reutilização verificada | Projeção acadêmica e limites |
|---|---|---|
| B01 `MemberAcademicReader` | `MemberDirectory`, `AccessService` | ID, situação, versão e identidade mínima; não copiar contato/claims desnecessários. Situação atual restringe acesso, sem apagar histórico |
| B02 `ProjectAcademicReader` | `ProjectDirectory`, `ProjectAccess` | Projeto e contribuição individual com vínculo/intervalo histórico e versão pertinente. Vínculo atual isolado não comprova participação no semestre passado |
| B03 `ActivityEvidenceReader` | `ActivityPolicy`, `ActivityService`, `EvidenceService`, `ActivityParticipation` | Atividade, projeto opcional, atribuição, autor, evidência e correções/retirada, versões e disponibilidade. `ActivityParticipation` valida contexto próprio de declaração; não exporta captura acadêmica |
| B04 `AttendanceAcademicReader` | `AttendanceRepository`, segmentos `ATTENDANCE` ligados a `attendance_records` e evento de Agenda | Captura apenas presença individual por encontro: ID/título/tipo do evento, modalidade, início/fim do segmento, indicador aberto e revisões de presença/evento. Exige autorização acadêmica própria e vínculo do período ao integrante; rejeita lançamentos remotos históricos e encontros cancelados. Não expõe duração em segundos, aprovação de horas, revisor nem justificativa |
| `DocumentEvidenceReader` | Não há provedor documental de produto nessa base | Documento, versão, titular/contexto e validação; integração indisponível. Não simular documento válido nem criar armazenamento paralelo |

Atividade concluída não é entrega academicamente validada. Não presumir um módulo de ciclos/entregas existente a partir do fluxo conceitual do PDF: a integração usa apenas capacidades comprovadas e registra a lacuna onde não houver entidade ou decisão operacional correspondente. Arquivo anexado a uma atividade não equivale a documento acadêmico oficial.

### 5.3 Alterações posteriores e consistência

1. Capturar referências e valores mínimos usados em cada revisão de avaliação; separar captura operacional da conferência pedagógica.
2. Antes do cálculo e da homologação, revalidar a autorização e as revisões relevantes. Origem inacessível, retirada, ausente ou divergente impede a nova consolidação até tratamento explícito; não converter indisponibilidade em nota zero.
3. **T:** captura final coerente em transação PostgreSQL com snapshot consistente e coordenação de concorrência das revisões acadêmicas. Registrar o instante de corte; uma alteração operacional confirmada depois desse corte é posterior, não parte da captura. Detalhar isolamento/locks ao implementar, sem depender de duas leituras não atômicas.
4. Após homologação, mudanças na origem não alteram resultado, captura, política ou decisões já registradas. Ao consultar/reutilizar a origem, comparar revisão/token e sinalizar divergência; não prometer monitoramento automático nem notificações de alteração nesta etapa.
5. A correção acadêmica gera nova revisão ligada à anterior, com justificativa e nova decisão do coordenador. Não buscar a origem atual para reconstruir silenciosamente um cálculo histórico.
6. Releitura atual pode ser negada mesmo existindo captura histórica. A captura segue autorização acadêmica própria e retenção institucional; não serve para contornar bloqueio de acesso a arquivos. Não duplicar binários, links assinados, comprovantes sensíveis ou dados pessoais sem necessidade aprovada.

## 6. Estados, transições e responsabilidades

Os nomes abaixo são **T propostas de modelagem** para revisão em G0-A; não novos estados normativos da universidade. Estados de adesão, plano, avaliação, revisão e encaminhamento são separados. Toda mutação exige versão esperada e transação atômica com histórico; requisições concorrentes ou repetidas não podem produzir duas homologações ou dois resultados oficiais internos para a mesma revisão.

### 6.1 Adesão e Plano Individual

| Objeto | Transições propostas | Responsabilidade e condição |
|---|---|---|
| Adesão | `RASCUNHO → SOLICITADA → EM_ANALISE → DEFERIDA` ou `INDEFERIDA` | Integrante solicita; coordenador decide com critérios oficiais configurados |
| Adesão corrigida | `EM_ANALISE → CORRECAO_SOLICITADA → SOLICITADA` | Coordenador registra motivo; integrante envia nova revisão |
| Plano Individual | `RASCUNHO → SUBMETIDO → EM_ANALISE → APROVADO` | Integrante elabora/submete; coordenador analisa a dimensão acadêmica |
| Plano corrigido | `EM_ANALISE → CORRECAO_SOLICITADA → SUBMETIDO` | Preservar versão submetida e decisão anterior |

Unicidade de adesão por integrante/período não impede histórico de revisões. Disciplinas não podem se repetir na mesma revisão. Cancelamento/desistência, efeitos após deferimento e eventual irrevogabilidade dependem do rito **I**; não oferecer transição definitiva por inferência. Alterar plano aprovado exige nova versão e nova aprovação, sem invalidar avaliações antigas automaticamente.

### 6.2 Avaliação e revisão

| Transição / operação | Quem | Precondição e efeito |
|---|---|---|
| Iniciar `EM_ACOMPANHAMENTO` | Fluxo autorizado após adesão deferida | Identificar integrante/período e política; não criar nota pela participação no NITE |
| Registrar/revisar pilar | Coordenador | Valor na escala, motivo, referências e versão; ausência permanece distinta de zero |
| `EM_ACOMPANHAMENTO → CALCULADA` | Coordenador ou preparação administrativa permitida | Política válida, todos os pilares avaliados e insumos coerentes; resultado sempre preliminar |
| Corrigir antes de homologar | Coordenador | Nova revisão de pilar torna o cálculo anterior não atual; recalcular antes de homologar |
| Abrir revisão | Integrante sobre si, ou coordenador em correção justificada | Registrar pedido e base exata; rito/prazos válidos. Guardar estado de origem, sem apagar cálculo |
| Analisar `EM_REVISAO` | Coordenador | Rejeição fundamentada preserva resultado; procedência gera novas avaliações/cálculo. Decisão auditada restaura estado compatível com o resultado |
| `CALCULADA → HOMOLOGADA` | Somente coordenador | Adesão deferida, política válida, cálculo atual e completo, sem revisão pendente, captura íntegra, confirmação e auditoria |
| Reabrir homologação | Somente coordenador | Motivo, confirmação e nova revisão; homologação anterior continua imutável, identificada como objeto da revisão |

Pedido sobre homologação não a reescreve nem a anula automaticamente; decisão de reabertura é explícita. Rejeição mantém a decisão anterior. Substituição exige novo cálculo e homologação, com vínculo de sucessão e identificação inequívoca da revisão vigente. Nenhuma edição ordinária em registro homologado é permitida.

### 6.3 Encaminhamento e confirmação externa

Dimensão independente da nota: `NAO_ENCAMINHADA → ENCAMINHADA`; eventual `CONFIRMADA_EXTERNAMENTE` só será habilitada após definição do procedimento **I**. Cada encaminhamento referencia a homologação exata e registra responsável e instante. Nova revisão não retifica automaticamente um encaminhamento anterior: registrar pendência de novo encaminhamento conforme procedimento.

Homologação interna não representa lançamento ou convalidação universitária. Não há professor cadastrado como novo perfil, escrita automática em plataforma externa ou confirmação por simples clique sem comprovação. Integração documental definitiva, assinatura e certificados dependem de decisões técnicas e das autoridades competentes.

## 7. Política acadêmica e preservação histórica

**D:** política versionada por período, vinculada de forma permanente a cada avaliação/cálculo. Guardar versão exata dos pilares, valores de entrada, revisões, pesos, escala, regra de arredondamento, versão do algoritmo, resultado e instante. Mudança de semestre não dispara recálculo histórico.

**T:** separar política em preparação de política confirmada para uso. A confirmação exige coordenador autorizado, referência à decisão institucional e configuração completa. Uma versão utilizada não é editável; correção gera nova versão. Não migrar avaliações existentes automaticamente para outra política. Uma revisão deve explicitar se preserva a política original ou usa nova versão autorizada; a troca depende de justificativa e validade institucional.

Parâmetros **I** obrigatórios conforme operação: cursos/disciplinas elegíveis, limite de disciplinas, janela de adesão, rito de conferência de pertinência, pesos, escala, precisão/arredondamento, rito/prazos de revisão, divulgação, procedimento de encaminhamento e confirmação externa. Um parâmetro faltante bloqueia somente o fluxo que depende dele; não usar valor implícito.

Pilares previstos: documentação individual, apresentações e entregas reais. Os pesos **30/20/50 são exclusivamente exemplo**, sem seed, fallback ou padrão de produção. O motor genérico deve usar `BigDecimal`, validar pesos e escala conforme a convenção explícita da política e não produzir resultado homologável sem política válida. Simulações, se aprovadas futuramente, ficam separadas e inelegíveis para homologação.

**T:** validar pesos não negativos e soma exata de 100% na representação decimal adotada; intervalos de notas e arredondamento vêm da política. Não renormalizar automaticamente pesos nem converter pilar não avaliado em zero. Pilar documental sem provedor ou evidência aceita não está avaliado por presunção; nesse caso, a homologação permanece bloqueada. Não redistribuir seu peso para os demais.

Penalidades, tolerâncias, compensações, frequência mínima, certificados e desligamento não são deduzidos de dados operacionais. Permanecem inativos até definição institucional e aprovação de escopo. Desativar integrante restringe acesso sem excluir avaliações, autoria ou vínculos históricos.

## 8. Gates, persistência e decisões pendentes

### 8.1 G0-A — Contrato funcional e arquitetural

Evidências para aprovação: revisão explícita deste arquivo, matriz RF/Q/CA, permissões por operação, contratos de leitura e suas lacunas, transições, preservação histórica e limites de integração/B07. Escolhas **T** tornam-se o contrato técnico aprovado somente após esse aceite. Parâmetros **I** podem continuar pendentes se o bloqueio da funcionalidade dependente estiver explicitamente mantido.

**Estado:** artefato formalizado para revisão. A aprovação da auditoria e dos refinamentos antecede este arquivo e não é registrada como aprovação automática do contrato.

### 8.2 G0-B — Base real de persistência

Antes de nomear a primeira migração acadêmica:

1. Inventariar ambientes locais persistentes, staging e quaisquer outros que tenham recebido versões do projeto, identificando responsável e base consultada, sem publicar credenciais ou dados de alunos.
2. Com acesso autorizado e somente leitura, localizar o schema da tabela `flyway_schema_history`; consultar `installed_rank`, `version`, `description`, `type`, `script`, `checksum`, `installed_on` e `success`. Não presumir o schema da tabela de controle pela localização das tabelas de negócio.
3. Confrontar histórico, arquivos e checksums do commit de cada ambiente. Consultar branches/PRs relevantes atualizados antes da decisão; referências locais isoladas podem estar desatualizadas.
4. Registrar ambiente, momento, commit, versão aplicada e eventuais divergências. Um banco novo/Testcontainers comprova aplicação da cadeia escolhida, mas não substitui inventário dos bancos existentes.
5. Não renumerar nem editar migrações já aplicadas. Alterações futuras no esquema devem receber uma versão aditiva e revisão própria.
6. Se V6 existir ou tiver sido aplicada em qualquer ambiente, não reutilizar ou renumerar sua identidade: preparar plano específico de reconciliação antes de avançar. Não habilitar `outOfOrder`, executar `repair`, limpar histórico ou apagar banco como atalho.

**Estado:** pendente. A indisponibilidade observada do Docker é uma limitação local; não demonstra ausência de bancos externos. Ambientes inacessíveis permanecem pendentes, nunca classificados como vazios. G0 só encerra com G0-A aprovado e G0-B evidenciado e decidido.

### 8.3 Decisões e dependências

| Tema | Classificação | Tratamento para avançar |
|---|---|---|
| Adesão opcional; homologação interna exclusiva de coordenador | D | Preservar e testar sem ampliar poderes de gestor geral |
| Política congelada e insumos preservados | D | Versionamento imutável e revisão rastreável |
| Contratos v1, estados e escopo administrativo deste arquivo | T | Aprovação de G0-A; detalhes menores de nomes/DTOs ficam ao implementador |
| Critérios acadêmicos, janelas, limites e elegibilidade | I | Confirmação institucional antes da ativação do fluxo dependente |
| Obrigatoriedade do Plano Individual; rito de cancelamento, revisão e divulgação | I | Manter capacidade planejada sem criar condicionantes normativos implícitos |
| Encaminhamento e comprovação de lançamento externo | I | Operação externa desabilitada até procedimento aprovado |
| Retenção, conteúdo sensível e comprovantes | I | Captura mínima; não copiar arquivos nem definir prazo de retenção arbitrário |
| Evidência documental oficial e certificados | I | Contrato futuro, sem editor/armazenamento paralelo ou provedor fictício |
| Identidade da migração acadêmica | G0-B | Decidir somente após histórico real; V6 é candidata |

## 9. Critérios de validação da futura implementação

Não representam testes executados nesta entrega documental.

| Categoria | Evidência exigida no incremento correspondente |
|---|---|
| Autorização | HTTP direto e serviço: gestor geral sem papel coordenador negado ao pontuar/homologar/reabrir; coordenador ativo autorizado no contexto; revogação/inatividade efetivas |
| Isolamento | Próprios dados versus colega; liderança contextual sem acesso acadêmico adicional; origem e captura autorizadas separadamente |
| Domínio | Adesão opcional e única; disciplinas sem duplicidade; transições válidas; motivos, confirmações e versões obrigatórios |
| Cálculo | Precisão decimal; limites/rounding da política; zero versus ausente; política incompleta/inválida; captura e reprodução exatas |
| Histórico | Nova política não reescreve semestre anterior; correção/retirada na origem não altera homologação; revisão gera sucessora auditável |
| Concorrência | Atualizações simultâneas, captura coerente no instante de corte, homologação repetida, revogação de papel e reabertura sem perda de histórico |
| Persistência | PostgreSQL real: cadeia nova e upgrade a partir da base confirmada; constraints, índices, rollback transacional e histórico preservado |
| Integração | Regressão dos contratos afetados B01–B04; ausência de provedor documental não produz evidência nem nota artificial |

Comandos serão selecionados por teste/módulo na implementação, seguindo `apps/api/AGENTS.md`. Verificação completa aplicável antes do encerramento técnico e após mudanças em contratos compartilhados ou persistência. G0 aprovado não substitui os gates posteriores G1–G5 nem autoriza operação institucional.

## 10. Registro da entrega documental anterior à implementação

- Único artefato novo: este contrato; nenhum código de produto, dependência, `.env`, migração, README ou experimento alterado.
- Worktree isolado criado a partir da `main` remota atualizada; checkout `D:\nite-connect` mantido em sua branch de investigação com alterações locais preservadas.
- Verificação documental executada: script Python confirmou presença de RF-042–047, Q24–29, CA-13/14, gates e perfis; referência local ao PDF resolvida. Leitura crítica conferiu permissões, transições e marcadores D/T/I/E; `git diff --no-index --check` não apontou whitespace inválido.
- Revisão independente de planejamento: nenhuma questão importante remanescente identificada; pendências institucionais e G0-B continuam explícitos, sem aprovação por inferência.
- Testes Java, build, Flyway e integração não executados: não se aplicam à alteração exclusivamente Markdown; nenhuma alegação de funcionamento do B06 decorre desta entrega.
- Próximo passo: revisão e aprovação do contrato G0-A; obter evidências de G0-B. B06.1 somente após estratégia de migração validada e autorização da etapa de implementação.
