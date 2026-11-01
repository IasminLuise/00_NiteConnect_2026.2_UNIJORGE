# Auditoria técnica — Contratos dos Blocos 00–06 para o Bloco 07

> **Registro histórico:** descreve o checkout auditado em 30/10/2026. A decisão de substituição B04 de 01/11/2026 descontinuou os contratos de horas, resumos de participação, justificativas e revisões descritos neste relatório. Para requisitos ativos, consultar [`bloco-04.md`](../implementation/bloco-04.md), [`bloco-04-api.md`](../implementation/bloco-04-api.md) e [`bloco-07-api.md`](../implementation/bloco-07-api.md).

**Repositório auditado:** `D:\nite-connect`, branch `main`, commit `4988778db72543ce6fabfd2391dd2779b0066e13` (30/10/2026).
**Referência funcional adicional:** Adendo à Especificação Funcional — Bloco 07: Regras Institucionais, Participação e Governança, fornecido pelo solicitante; decisões consolidadas e detalhamentos reservados na seção 9 do adendo.
**Escopo:** código Java da API, migrações Flyway, contratos HTTP, registros de execução/testes versionados no repositório e compatibilidade com o adendo.
**Fora da evidência desta auditoria:** estado atual de bancos/produção, execução de testes, carga, interfaces frontend do B07 e homologação institucional.

## Resumo executivo

O backend já oferece bases reutilizáveis para autenticação/autorização, vínculos de projeto e atividade, evidências, presença e horas declaradas, histórico operacional e avaliação acadêmica com proveniência e homologação interna. B04 também expõe resumos individuais de frequência e B03 calcula sinais de prazo para atividades.

Essas capacidades atendem apenas parcialmente aos contratos do Bloco 07. O adendo consolida a carga semanal de 5 horas presenciais e 4 remotas, sem compensação automática entre modalidades, e especifica exceções de calendário, validação remota semanal em conjunto e três tratamentos individuais distintos. O backend atual não calcula obrigação semanal líquida, não oferece revisão em lote nem modela dispensa/reposição. Também não implementa os níveis gerenciais Regular/Atenção/Crítico/Dados insuficientes por projeto, APIs de alertas, painéis, relatórios ou exportação PDF/CSV.

O bloco documental tem migrações históricas para documentos, versões, concessões e Diários de Bordo, mas não foram encontradas APIs nem serviços Java ativos correspondentes. B06 formaliza um `DocumentEvidenceReader`, mas a ausência de implementação de produto impede captura de evidência documental oficial e pode bloquear avaliação/homologação dependente desse pilar.

**Pendências para o planejamento técnico:** especificar fórmulas, unidades, período e proveniência dos indicadores; definir calendário, fuso, fechamento e compatibilidade do fluxo de validação semanal com revisões individuais; operacionalizar as regras já decididas de dispensa/reposição; confirmar disponibilidade documental do B05; e definir valores dos parâmetros de criticidade por projeto. O adendo atribui a gestão desses parâmetros à Gestão Geral e Coordenação e estabelece a carga 5h presencial/4h remota; essas regras são requisitos aprovados, não funcionalidades encontradas no backend.

## Convenção de classificação

- **Implementado:** há código de produto executável e contrato correspondente observáveis.
- **Parcial:** há parte dos dados, operações ou regras, mas falta semântica ou capacidade necessária ao requisito.
- **Apenas especificado:** há documento/interface/schema conceitual, sem implementação de produto completa.
- **Ausente:** não foi encontrado suporte de produto ou especificação suficiente no checkout inspecionado.
- **Não verificável:** dependeria de ambiente ou execução que não foi examinada nesta auditoria.

As conclusões abaixo usam o código do checkout como evidência primária. Migração SQL, contrato documental e testes sintéticos não foram tratados como prova de serviço ativo. O estado de bancos persistentes não foi consultado; existência de arquivo Flyway não comprova aplicação em todos os ambientes.

## Inventário por bloco

### Bloco 00 — Fundação da persistência e da API

**Classificação geral: implementado para a fundação técnica; B00 não aparece como bloco funcional independente no catálogo.** A migração V1 cria somente o schema `nite_connect`, sem tabelas de domínio ou contas fictícias. A configuração atual usa PostgreSQL, Flyway em `classpath:Migrations` e `ddl-auto: validate`; endpoints são protegidos por allowlist e qualquer rota não listada é negada.

**Reuso para B07:** persistência relacional, constraints/índices das migrações, transações e paginação compartilhada. Não existe camada de analytics, exportação ou armazenamento histórico agregado específica do B07.

Evidências: [V1__initialize_schema.sql, L1-L3](../../apps/api/src/main/resources/Migrations/V1__initialize_schema.sql#L1-L3); [Migrations/README.md, L1-L18](../../apps/api/src/main/resources/Migrations/README.md#L1-L18); [application.yml, L6-L27](../../apps/api/src/main/resources/application.yml#L6-L27); [SecurityConfiguration.java, L34-L94](../../apps/api/src/main/java/br/edu/unijorge/niteconnect/shared/security/SecurityConfiguration.java#L34-L94); [PageResult.java, L5-L10](../../apps/api/src/main/java/br/edu/unijorge/niteconnect/shared/api/PageResult.java#L5-L10).

### Bloco 01 — Identidade, papéis globais e auditoria administrativa

**Classificação geral: implementado.** A API valida identidade institucional Entra e consulta vínculo e papéis persistidos no banco; autenticação não concede acesso operacional automaticamente. Estados de integrante: `PENDENTE_INGRESSO`, `ATIVO`, `AFASTADO`, `DESLIGADO`. Os dois papéis globais codificados são `COORDENADOR` e `GESTOR_GERAL`; `INTEGRANTE` é papel efetivo derivado do vínculo ativo. Não há papel global `GESTOR_PROJETO`: liderança é contextual ao projeto no B02.

Cadastro, convites, alterações de papel e estado têm histórico/auditoria e controle otimista de versão. Essa base permite montar projeções de dashboard respeitando autorização no backend, mas o contrato de B01 não oferece um endpoint analítico consolidado por perfil. A auditoria administrativa existente é distinta dos históricos específicos dos módulos.

Evidências: [GlobalRole.java, L3-L4](../../apps/api/src/main/java/br/edu/unijorge/niteconnect/access/domain/GlobalRole.java#L3); [MemberStatus.java, L3-L4](../../apps/api/src/main/java/br/edu/unijorge/niteconnect/members/domain/MemberStatus.java#L3-L4); [AccessService.java, L28-L49](../../apps/api/src/main/java/br/edu/unijorge/niteconnect/access/application/AccessService.java#L28-L49); [bloco-01-api.md, L1-L24 e L45-L67](../implementation/bloco-01-api.md#L1-L24); [AuditTrail.java, L17-L41](../../apps/api/src/main/java/br/edu/unijorge/niteconnect/audit/application/AuditTrail.java#L17-L41).

### Bloco 02 — Propostas, projetos, equipe e liderança

**Classificação geral: implementado.** Projetos têm UUID, título/descrição, estado, datas inicial/final opcionais, referência opcional à proposta, criador e versão. Estados: `PLANEJAMENTO`, `ATIVO`, `PAUSADO`, `CONCLUIDO`. Participações registram responsabilidade, início/fim, autores e motivos; transferências e históricos de liderança têm período de validade. A consulta de projeto limita integrantes a participação vigente ou administração; gestão por liderança exige vínculo contextual ativo.

**Limites para B07:** início/fim não geram transições automáticas; projeto não possui parâmetro de criticidade, limiar configurável, agregação de desempenho ou estado de risco. O histórico preserva status e períodos de vínculo, mas as projeções correntes de participação não constituem uma consulta histórica “como era em uma data” para todos os atributos do projeto.

Evidências: [ProjectStatus.java, L3-L13](../../apps/api/src/main/java/br/edu/unijorge/niteconnect/projects/domain/ProjectStatus.java#L3-L13); [ProjectDirectory.java, L16-L64 e L99-L120](../../apps/api/src/main/java/br/edu/unijorge/niteconnect/projects/application/ProjectDirectory.java#L16-L64); [bloco-02-api.md, L13-L35 e L45-L65](../implementation/bloco-02-api.md#L13-L35); [ProjectAccess.java, L14-L22](../../apps/api/src/main/java/br/edu/unijorge/niteconnect/access/application/ProjectAccess.java#L14-L22).

### Bloco 03 — Atividades, atribuições e evidências operacionais

**Classificação geral: implementado para atividades e evidências; parcial para entregas/ciclos e validação de entrega.** Atividade pode se vincular a projeto (opcional na criação), possui executor principal obrigatório, colaboradores por período, prazo, estado, conclusão e versão. Estados: `PLANEJADA`, `EM_EXECUCAO`, `CONCLUIDA`, `CANCELADA`, com transições explícitas e reabertura condicionada. Evidências incluem texto, link e objetos carregados, autoria, atribuição relacionada, correção de evidência e retirada; uploads locais têm validação/estado de disponibilidade.

A API deriva `deadlinePassed`, `overdue` e `attentionRequired` da data limite, estado terminal e estado `ATIVO` do projeto. Isso é um sinal básico reutilizável, não uma criticidade configurável ou alerta persistente. A auditoria do domínio preserva eventos e versões; não foi encontrada entidade operacional de ciclo de entrega submetida/validada, solicitação de correção de entrega ou decisão de entrega distinta de concluir/reabrir atividade.

Evidências: [ActivityStatus.java, L3-L14](../../apps/api/src/main/java/br/edu/unijorge/niteconnect/activities/domain/ActivityStatus.java#L3-L14); [ActivityService.java, L34-L57 e L82-L90](../../apps/api/src/main/java/br/edu/unijorge/niteconnect/activities/application/ActivityService.java#L34-L57); [ActivityService.java, L127-L138](../../apps/api/src/main/java/br/edu/unijorge/niteconnect/activities/application/ActivityService.java#L127-L138); [ActivityEvidenceReader.java, L15-L31](../../apps/api/src/main/java/br/edu/unijorge/niteconnect/activities/application/ActivityEvidenceReader.java#L15-L31); [bloco-03-api.md, L7-L43](../implementation/bloco-03-api.md#L7-L43).

### Bloco 04 — Agenda, presença, horas remotas e justificativas

**Classificação geral: implementado para registros e revisões operacionais; parcial para apuração e regras B07.** Agenda suporta modalidade `PRESENCIAL`/`REMOTA`, estados de evento, participantes esperados e sessão QR temporária. Registros de presença/trabalho são períodos com início/fim, modalidade, origem, atividade/evidência opcionais e revisão `PENDENTE`/`APROVADA`/`RECUSADA`. Há validação de sobreposição por integrante e proteção contra presença duplicada no mesmo encontro. Declarações remotas requerem intervalo fechado passado e começam pendentes; justificar falta não cria horas. Correções propostas e correções administrativas têm decisões e histórico.

Há `GET /api/v1/me/participation-summary` e resumo administrativo por integrante/período, com segundos registrados/validados/pendentes/recusados, períodos abertos, encontros esperados/atendidos/ausências. A consulta recorta datas por fuso configurado, mas soma horas de todas as modalidades em conjunto e não agrupa por semana. A revisão é individual por período; não há operação de validação semanal por conjunto. Eventos armazenam título, descrição, tipo, modalidade, início/fim e participantes esperados, mas não referência de projeto ou resumo estruturado separado. O calendário não calcula obrigação semanal, dispensa por exceção ou horas exigíveis líquidas. Os totais de reuniões são contagens, não integração com Meet.

O objeto `Justification` representa ocorrência/ausência, com motivo e decisões `ACEITA`, `RECUSADA`, `COMPLEMENTACAO_SOLICITADA`. `Correction` trata ajuste de presença/período. Não há tipo operacional próprio para dispensa autorizada versus reposição, nem quantidade, modalidade, prazo e realização de reposição. A justificativa aceita tampouco gera horas; essa regra existente coincide com a justificativa simples do adendo, mas não implementa os dois tratamentos adicionais.

Evidências: [AttendanceContracts.java, L19-L36](../../apps/api/src/main/java/br/edu/unijorge/niteconnect/attendance/api/AttendanceContracts.java#L19-L36); [bloco-04-api.md, L15-L43](../implementation/bloco-04-api.md#L15-L43); [bloco-04-api.md, L45-L73 e L75-L95](../implementation/bloco-04-api.md#L45-L73); [ParticipationQueries.java, L19-L24 e L57-L80](../../apps/api/src/main/java/br/edu/unijorge/niteconnect/attendance/application/ParticipationQueries.java#L19-L24); [Participation.java, L9-L32](../../apps/api/src/main/java/br/edu/unijorge/niteconnect/attendance/domain/Participation.java#L9-L32).

### Bloco 05 — Documentos e Diários de Bordo

**Classificação geral: parcial no esquema; ausente como módulo de produto ativo no código Java encontrado.** V6 cria tabelas para modelos, documentos, versões, concessões, ciclos de editor, recibos de callback, PDFs e eventos; V7 vincula no máximo um `document_id` por integrante e registra proveniência de importação do diário; V10 registra sessões de inspeção local. Esses arquivos são estruturas de banco, não provam APIs, serviço de armazenamento, estados de validação funcional, editor em execução ou leitura documental acadêmica. No código Java do produto inspecionado não há pacote/controller/repository de documentos ou diários; a única referência principal é o contrato `DocumentEvidenceReader` no domínio acadêmico.

**Impacto direto:** evidência de B03 anexada a atividade não substitui documento oficial ou Diário de Bordo validado. Os metadados da V7 provam somente uma estrutura de diário/importação, sem operação de diário, ciclo de submissão ou aprovação disponível para o B07.

Evidências: [Migrations/README.md, L11-L18](../../apps/api/src/main/resources/Migrations/README.md#L11-L18); [V6__document_management.sql, L1-L73](../../apps/api/src/main/resources/Migrations/V6__document_management.sql#L1-L73); [V7__continuous_member_diaries.sql, L1-L24](../../apps/api/src/main/resources/Migrations/V7__continuous_member_diaries.sql#L1-L24); [V10__local_document_inspection.sql, L1-L14](../../apps/api/src/main/resources/Migrations/V10__local_document_inspection.sql#L1-L14); [AcademicEvidenceService.java, L41-L47](../../apps/api/src/main/java/br/edu/unijorge/niteconnect/academic/application/AcademicEvidenceService.java#L41-L47).

### Bloco 06 — Avaliação acadêmica

**Classificação geral: implementado para o fluxo de avaliação interna e parcial por dependências documentais/institucionais.** Há períodos, políticas versionadas, adesão por integrante, disciplinas, plano individual versionado, evidências, avaliações manuais por pilar, cálculo decimal, revisões, homologações, reabertura, encaminhamento interno e eventos/histórico. Estados incluem `SOLICITADA`, `EM_ANALISE`, `CORRECAO_SOLICITADA`, `DEFERIDA`, `INDEFERIDA`, `EM_ACOMPANHAMENTO`, `CALCULADA`, `EM_REVISAO`, `HOMOLOGADA`; os pilares são documentação, apresentação e entregas. Capturas preservam identidade/revisão/autor/validador, intervalo, escopo de leitura e instante da captura.

A política acadêmica diferencia `GESTOR_GERAL` e `COORDENADOR`: gestor geral prepara e calcula sob restrições, coordenação pode pontuar/decidir/homologar. Homologação exige cálculo atual, evidências atuais, política confirmada e ausência de revisão pendente. O resultado acadêmico homologado não equivale à publicação de nota universitária.

A captura de evidência de atividade e presença reutiliza leitores dos módulos de origem com autorização contextual. A fonte `DOCUMENT` depende de um provedor; sem implementação, a captura retorna indisponibilidade. Política, ritos e parâmetros sem referência confirmada bloqueiam os fluxos que deles dependem. Os documentos de execução registram testes anteriores, inclusive 218 testes no estado documentado, mas nenhum foi executado nesta auditoria e a execução histórica não foi repetida como validação do checkout atual.

Evidências: [bloco-06-api.md, L1-L27](../implementation/bloco-06-api.md#L1-L27); [bloco-06-api.md, L29-L55](../implementation/bloco-06-api.md#L29-L55); [AcademicStates.java, L3-L11](../../apps/api/src/main/java/br/edu/unijorge/niteconnect/academic/domain/AcademicStates.java#L3-L11); [AcademicRecords.java, L13-L40](../../apps/api/src/main/java/br/edu/unijorge/niteconnect/academic/domain/AcademicRecords.java#L13-L40); [AcademicEvidenceService.java, L32-L55](../../apps/api/src/main/java/br/edu/unijorge/niteconnect/academic/application/AcademicEvidenceService.java#L32-L55); [AcademicAuthorizationPolicy.java, L13-L23](../../apps/api/src/main/java/br/edu/unijorge/niteconnect/academic/application/AcademicAuthorizationPolicy.java#L13-L23); [AcademicHomologationService.java, L19-L34](../../apps/api/src/main/java/br/edu/unijorge/niteconnect/academic/application/AcademicHomologationService.java#L19-L34); [bloco-06-execution.md, L92-L100](../implementation/bloco-06-execution.md#L92-L100).

## Matriz de compatibilidade do Bloco 07

| Requisito aprovado | Fontes/operações existentes | Nível | Compatibilidade e limite observado |
|---|---|---|---|
| Dashboards por perfil/contexto | Identidade/papéis B01; projetos e vínculos B02; escopo de atividade B03; autorização acadêmica B06 | **Parcial** | Há autorização contextual reutilizável e consultas operacionais por domínio. Não existem endpoints de dashboard nem projeções agregadas por integrante, liderança de projeto, gestão geral e coordenação. Toda leitura B07 deve preservar as regras de cada domínio e a dupla autorização acadêmica/origem quando aplicável. |
| Carga semanal 5h presencial/4h remota, sem compensação entre modalidades | `Period.modality`; `participation-summary` | **Parcial** | A regra está aprovada no adendo, mas a modalidade não é separada no resumo. Não há horas exigíveis por modalidade, nem cálculo de saldo semanal ou impedimento de compensação no agregado; datas são recortadas por fuso e intervalo arbitrários. |
| Trabalho remoto, associação contextual e sem duplicidade | B04 work logs, QR, atividade/evidência opcional, sobreposição | **Parcial** | Períodos e bloqueio de sobreposição são reutilizáveis. Um work log pode referenciar atividade/evidência, mas não projeto/reunião/documento diretamente; a agenda não relaciona evento a projeto. Não há validação semanal por conjunto. Não há crédito automático por tarefa concluída, reunião agendada ou documento preenchido. Google Meet não é dependência inicial. |
| Validação semanal remota por conjunto com decisões por lançamento | B04 revisão individual de `Period` | **Ausente** para lote; **parcial** para decisão individual | Aprovado no adendo. B04 permite aprovar/recusar períodos fechados individualmente e preservar histórico; não há submissão/revisão de conjunto semanal nem pedido de correção em lote. A extensão deve conservar os estados individuais `PENDENTE/APROVADA/RECUSADA`. |
| Reunião remota com data, duração, participantes, projeto e resumo | B04 evento, períodos de presença e participantes esperados | **Parcial** | Evento traz data/hora, duração prevista, participantes, modalidade e descrição. Falta relação explícita evento–projeto, resumo pós-reunião estruturado e consolidação de participação remota por reunião. Meet é integração futura opcional. |
| Exceções institucionais de calendário: cancelamento, recesso, suspensão e remarcação | B04 eventos `FERIADO`/`CANCELADO` e agenda | **Parcial** | Há tipo feriado e cancelamento de evento, mas não calendário de obrigações, abrangência de exceção, remarcação vinculada nem cálculo líquido das horas exigíveis. As regras aprovadas distinguem dispensa presencial, remota e integral e proíbem horas dispensadas de aparecerem como trabalhadas; nada disso é calculado hoje. |
| Três tratamentos: justificativa simples, dispensa autorizada e reposição autorizada | `absence-justifications`, `correction_requests`, decisões/revisões | **Parcial** | O adendo define a semântica: justificativa simples não altera obrigação; dispensa reduz horas exigíveis sem criar horas trabalhadas; reposição é autorizada individualmente e só reconhecida após realização e validação. O backend possui apenas justificativa e correção, sem entidade/ciclo de vida/quantidade/prazo para dispensa e reposição. |
| Reuniões presenciais com QR temporário, sem presumir carga integral por check-in | Agenda B04, sessão/QR de validade curta, check-in e períodos | **Parcial** | QR e registro vinculado ao evento estão implementados; períodos e revisões são a fonte da duração reconhecida. Não há cálculo de horas exigíveis por reunião/semana, portanto o backend atual não transforma o check-in em carga integral — compatível com a regra aprovada, mas incompleto para apuração. |
| Classificação por projeto: Regular, Atenção, Crítico ou Dados insuficientes | Projeto B02; `attentionRequired` B03; papéis globais B01 | **Parcial** para sinal de prazo; **ausente** para governança/classificação configurável | O adendo aprova níveis, motivos e indicação dos parâmetros/registros de origem; limiares concretos ficam para planejamento. Há apenas `attentionRequired` derivado de atraso e estado de projeto ativo. Não há níveis, dados insuficientes, parâmetros versionados por projeto ou histórico de vigência. |
| Indicadores rastreáveis e estados declarado/validado/preliminar/homologado | B04 estado de revisão e histórico; B03 autoria/versão; B06 snapshots e estados acadêmicos | **Parcial** | Proveniência e estados estão disponíveis em domínios específicos. O B04 já distingue declarado/revisado e B06 preliminar/homologado; não há modelo transversal de indicador que retenha fórmula, instante de corte, filtros, fonte, versão e estado do valor exibido/exportado. |
| Alertas configuráveis por projeto | prazo B03 e histórico de projeto/atividade | **Ausente** | Sinal de atraso não é alerta configurável, não tem limiar por projeto, destinatário, estado de processamento, silenciamento ou trilha de disparo. |
| Análises históricas e relacionais | Históricos B01–B04; capturas imutáveis B06; filtros/paginação por entidade | **Parcial** | Há eventos e períodos históricos, inclusive períodos de equipe/liderança e histórico de participação. Faltam contratos analíticos que combinem fatos sob tempo/evento, definições de métricas históricas e algumas projeções “como era na data”. Não reconstruir snapshots B06 com dados correntes. |
| PDF/CSV por filtros de pessoa, projeto, período e situação | Listagens paginadas de módulos; contrato funcional consolidado RF-050 | **Apenas especificado** no requisito funcional; **ausente** no backend atual | Não foram encontrados endpoints, formato de relatório, gerador, limites, esquema CSV/PDF nem política de filtragem de exportação no backend. O RF-050 aparece no PDF consolidado, p. 8, mas não como implementação. |
| Acesso contextual e proteção de dados por perfil | `ProjectAccess`, `AccessService`, `AcademicAuthorizationPolicy`, APIs B04/B06 | **Parcial** | B02 permite liderança ler/gerir o próprio projeto; porém resumos, justificativas e revisões B04 são administrativos globais, sem consulta de frequência escopada ao líder do projeto. O adendo exige acompanhamento operacional dentro do projeto e veda acesso automático a notas acadêmicas/justificativas pessoais. B07 precisa de projeções/autorizações específicas; exportação deve aplicar o mesmo escopo. |

Referência funcional complementar: [Especificação Funcional Consolidada v1.0, pp. 4, 8, 10, 14–15](../product/NITE_Connect_Especificacao_Funcional_Consolidada_v1.0.pdf), incluindo RF-050/051, Q15/Q30–32 e papéis. O PDF registra os requisitos gerais; o adendo fornecido complementa as decisões de frequência, exceções de calendário, justificativas, criticidade e privacidade. Ambos são requisitos funcionais; não comprovam operações implementadas.

## Dependências, lacunas e divergências

### Fontes e operações reutilizáveis

1. **Identidade/contexto:** B01 oferece identidade ativa, papéis persistidos e ciclo de situação; B02 fornece consultas de projeto, participação e liderança atuais e históricos; B03 fornece atividade, atribuições, prazos, evidências e histórico.
2. **Frequência:** B04 fornece intervalos com modalidade, `registeredSeconds`/`validatedSeconds`, QR, correção, revisão e histórico. Os dados base são utilizáveis, mas o endpoint de resumo atual não satisfaz a forma semanal e separada aprovada.
3. **Acadêmico:** B06 oferece período, políticas, referência temporal, recortes por semestre, status de avaliação, snapshots das fontes e controles de homologação/revisão. É a referência de proveniência e de dados preliminares/homologados, não uma fonte genérica de atividade gerencial.
4. **Persistência:** Flyway, PostgreSQL, índices por integrante/data/entidade, paginação estável e transações já fazem parte da stack. O lock administrativo compartilhado dá consistência a gravações/capturas críticas no desenho atual; não equivale a uma plataforma pronta de consultas analíticas.

### Lacunas prioritárias para esclarecer antes do desenho

- **Apuração temporal:** detalhar calendário semanal, fuso, fechamento, semanas parciais e exceções não cobertas. As exceções previstas no adendo já estão decididas e precisam ser codificadas sem alterar as nove horas de forma indiscriminada: cancelamento presencial sem remarcação dispensa cinco horas presenciais; recesso integral dispensa ambas; suspensão exclusivamente presencial dispensa só presencial; remarcação mantém a obrigação na nova data; recesso parcial afeta somente o que suspender. Impedir que as mesmas horas fiquem simultaneamente exigíveis e dispensadas.
- **Separação e reconciliação:** expor previsto, exigível, registrado, pendente e validado separadamente por modalidade. Períodos não sobrepostos já são protegidos em B04; consultas devem evitar dupla contagem quando um trabalho tiver referências a projeto, atividade, reunião ou evidência.
- **Validação semanal:** definir o contrato da submissão/revisão por conjunto e sua relação com revisão individual B04; permitir decisão/correção/rejeição de registros específicos preservando histórico e estados individuais.
- **Dispensa/reposição:** operacionalizar as regras aprovadas. Registrar horas/modalidade dispensadas sem convertê-las em trabalhadas; para reposição, reter autorização individual, prazo e responsável, reconhecer somente após realização e validação e informar atraso sem penalidade automática.
- **Reuniões remotas:** fornecer relação evento–projeto e resumo estruturado quando necessários aos indicadores. Google Meet é opcional; monitoramento invasivo, gravação e transcrição não são pré-requisitos gerais.
- **Documentos/Diário de Bordo:** confirmar a fonte de verdade e disponibilidade de B05, seus estados, autoria, versões, associação temporal/contextual e autorização. V6/V7/V10 são migrações históricas append-only; qualquer mudança deve respeitar o histórico Flyway dos ambientes. O `DocumentEvidenceReader` B06 não tem provedor de produto.
- **Indicadores:** especificar fórmula, unidade, janela temporal, granularidade, inclusões/exclusões, versão e instante de corte. Diferenciar dados declarados, validados, preliminares e homologados; valores agregados não devem apresentar pendências como horas reconhecidas.
- **Criticidade/alertas:** os quatro níveis e a governança por Gestão Geral/Coordenação estão definidos; ainda faltam os valores concretos por projeto, fórmulas, vigência, origem/motivos e semântica de alerta. Distinguir classificação calculada de disparo/destinatário e não converter toda ocorrência administrativa em alerta.
- **Relatórios:** definir catálogo, campos por perfil, filtros, ordenação, fuso, corte, limites, CSV/PDF e proteção dos campos pessoais. Aplicar as mesmas projeções/autorização usadas nas consultas.
- **Acesso:** criar consulta operacional de frequência no escopo de liderança de projeto se necessária ao acompanhamento aprovado; não usar essa ampliação para revelar justificativas pessoais ou notas acadêmicas. Validar acesso com os módulos de origem.

### Risco de fronteira arquitetural

A arquitetura registrada é monólito modular por domínio. Os contratos B06 determinam leitores fornecidos pelos módulos de origem, em vez de consultas arbitrárias a repositórios/tabelas internos. O B07 deve compor serviços de consulta/projeções explícitos, sem transferir para um “motor de dashboard” a autoridade de autorização ou a semântica de presença, atividade, projeto e avaliação. O relatório gerencial não deve tratar evento/histórico genérico como registro substituto dos estados de negócio.

## Análise de impacto

### Atendível diretamente com leitura dos contratos atuais

- Listar projetos e vínculos atualmente autorizados, estado/prazo de atividades, responsáveis e eventos de agenda.
- Mostrar volumes existentes de participação e presença e o número de encontros esperados/atendidos/ausências no recorte disponível.
- Exibir status operacionais de revisão de períodos, pedidos de correção/justificativa e estados de avaliação acadêmica permitidos ao perfil.
- Reutilizar histórico e proveniência de B01–B04/B06, mantendo motivo e conteúdo sensível sob projeções autorizadas.

Esses usos continuam sujeitos a compor filtros e permissões corretamente; “diretamente” significa que as fontes existem, não que haja endpoint ou dashboard pronto.

### Exige adaptação de contrato ou projeção

- Totais semanais por modalidade e situação, comparação com referências 5h/4h e tendência histórica reproduzível.
- Consolidação remota por origem/reunião/atividade/documento com reconciliação e ausência de duplicidade.
- Indicadores relacionais projeto–equipe–atividade–frequência com snapshots de corte, filtros e estado declarado/validado.
- Sinais de acompanhamento com criticidade por projeto, regras configuráveis, trilha de alteração e ocorrência de alerta.
- Painéis agregados por papel e relatórios paginados/filtrados, incluindo autorização por contexto e exportações CSV/PDF.
- Integração a B05 e ao pilar documental acadêmico, se houver implementação documental ativa aprovada.

### Definições reservadas ao planejamento e à validação complementar

O adendo consolida as regras de carga semanal, ausência, dispensa, reposição, exceções de calendário, níveis de classificação por projeto, responsabilidades administrativas e proteção por perfil. Permanecem reservados para detalhamento (adendo, §9):

- Calendário semanal de referência, fuso e instante de fechamento.
- Tratamento de semanas incompletas e situações individuais excepcionais não contempladas explicitamente.
- Procedimento operacional para reposições vencidas, sem inferir penalidade automática.
- Disponibilidade efetiva dos contratos documentais do Bloco 05.
- Compatibilidade entre validação semanal em conjunto e revisão individual existente.
- Fórmulas, unidades, períodos e proveniência dos indicadores.
- Valores concretos dos parâmetros de criticidade por projeto.
- Estruturas de consulta e exportação dos relatórios.

Esses detalhes não podem contradizer as decisões consolidadas nem introduzir regras punitivas/acadêmicas não homologadas.

## Recomendações técnicas priorizadas

### P0 — Contratos bloqueadores do planejamento técnico

1. **Fechar o catálogo de indicadores B07** com identificador, fórmula, unidade, fonte, granularidade, fuso/período, corte, estados do dado e perfis autorizados. Diferenciar declarado, pendente, validado, preliminar e homologado sem colapsar estados de origem.
2. **Projetar a apuração semanal** por obrigação presencial/remota: horas previstas, exigíveis, registradas, pendentes e validadas; regras de exceção de calendário; sem compensação automática; sem contar dispensa como trabalho. Definir semana/fuso/fechamento que o adendo reserva.
3. **Planejar a validação semanal remota em conjunto** com aprovações, correções ou rejeições por registro e trilha histórica, compatível com os estados individuais B04.
4. **Detalhar os três tratamentos administrativos já aprovados**: justificativa simples, dispensa e reposição com horas/modalidade/prazo/responsável/motivo; reconhecimento somente após realização e validação; sem penalidade automática por vencimento.
5. **Resolver a dependência documental**: verificar o escopo ativo pretendido para B05 contra V6/V7/V10 e confirmar onde Diário de Bordo/documentos são mantidos. Antes de consumir documentos em B07, definir APIs/leitores de origem e alinhar o `DocumentEvidenceReader` ausente no produto; preservar migrações aplicadas.

### P1 — Governança, projeções e autorização

6. Definir parâmetros por projeto para Regular/Atenção/Crítico/Dados insuficientes, responsáveis Gestão Geral/Coordenação, motivo/fontes, vigência e histórico imutável de mudanças; não alterar ciclos de estado do B02.
7. Criar interfaces de leitura/projeções por domínio com filtro/paginação estável, instante de corte, proveniência e classificação de estado. Evitar acesso B07 direto às tabelas internas de outros módulos.
8. Especificar a matriz de autorização: integrantes consultam próprios dados autorizados; líder vê somente dados operacionais da equipe/projeto; B07 não libera notas acadêmicas nem justificativas pessoais pela liderança; gestão geral/coordenação mantêm escopo conforme módulo de origem. Relatórios e exportações usam as mesmas projeções e controles.
9. Definir como líderes de projeto recebem acompanhamento de frequência sem abrir rotas B04 administrativas globais indiscriminadamente.
10. Definir como analisar histórico “como era na data”; quando a origem não preserva a revisão necessária, declarar o limite ou registrar snapshot B07 em vez de recalcular o passado com o estado corrente.
11. Para reuniões remotas, especificar o vínculo evento–projeto e o resumo estruturado, reutilizando data/duração/participantes já disponíveis; integração Google Meet é opcional.

### P2 — Relatórios e operação

12. Definir contratos versionados para CSV/PDF e limites de consulta/exportação, incluindo valores vazios, estados mistos, fuso, corte, fontes e proteção contra exportação de detalhes privados.
13. Priorizar informações relevantes ao perfil; não transformar toda ocorrência administrativa em alerta/notificação. Distinguir classificação de projeto da ocorrência/destinatário/estado de alerta.
14. Somente após P0/P1, planejar índices/consultas agregadas conforme volume e planos de execução medidos. Não introduzir cache, jobs, filas ou tabelas de agregação como premissa antes de evidenciar necessidade operacional.
15. Manter relatórios de execução de teste separados desta auditoria: consultar os testes existentes relacionados à modalidade, filtros, escopos e migrações ao finalizar implementação; os testes desta auditoria não foram executados.

## Referências adicionais examinadas

- Adendo à Especificação Funcional — Bloco 07: Regras Institucionais, Participação e Governança, fornecido pelo solicitante, especialmente §§2–7 e 9. Trata-se da referência funcional adicional desta auditoria; os requisitos nela contidos não equivalem a código implementado.

- [Especificação funcional consolidada v1.0](../product/NITE_Connect_Especificacao_Funcional_Consolidada_v1.0.pdf), especialmente pp. 4, 8, 10, 14–15. O RF-050/051 e os Q15/Q30–32 são requisitos especificados; não comprovam endpoints ativos.
- [Catálogo Flyway](../../apps/api/src/main/resources/Migrations/README.md), que registra V1–V10 e identifica V6/V7/V10 como estruturas históricas no catálogo atual.
- [Contrato HTTP B04](../implementation/bloco-04-api.md) e [contrato/API B06](../implementation/bloco-06-api.md), confrontados com controllers, serviços e domínio Java.
- Testes versionados inspecionados como evidência de intenção/cobertura: [ParticipationReviewsTest.java](../../apps/api/src/test/java/br/edu/unijorge/niteconnect/ParticipationReviewsTest.java), [AcademicHomologationTest.java](../../apps/api/src/test/java/br/edu/unijorge/niteconnect/AcademicHomologationTest.java) e testes de migração B04/B06. **Não foram executados nesta auditoria.**
