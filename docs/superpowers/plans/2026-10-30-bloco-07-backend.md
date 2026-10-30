# Bloco 07 Backend Implementation Plan

> **For agentic workers:** Execute this plan in order with TDD. The requester explicitly authorized implementation. Steps use checkbox tracking.

**Goal:** Entregar as capacidades implementáveis do Bloco 07 integradas aos domínios atuais, com autorização contextual, proveniência, migrações aditivas, APIs documentadas e testes.

**Architecture:** Manter consultas de leitura em módulos de origem explícitos e regras de escrita nos domínios responsáveis. O Bloco 07 compõe projeções e relatórios; B04 mantém estados individuais, B02 mantém ciclo de projeto e nenhuma classificação altera estados existentes.

**Tech Stack:** Java 21, Spring Boot 4.1.1, PostgreSQL, Flyway, Maven, Spring Security, MockMvc e Testcontainers.

**Spec:** `docs/auditorias/bloco-07-contratos.md`, seu adendo fornecido pelo solicitante e `docs/product/NITE_Connect_Especificacao_Funcional_Consolidada_v1.0.pdf`.

## Global Constraints

- Usar carga aprovada de 5h presenciais e 4h remotas, sem compensação entre modalidades.
- Não inferir fuso, fechamento semanal, prazos ou consequências disciplinares não definidos. A baseline de criticidade 7/14 e 14/30 dias foi aprovada explicitamente pelo solicitante e continua configurável por projeto.
- Preservar estados individuais de revisão B04, estados de projeto B02 e APIs existentes.
- Não consumir nem simular validação documental B05 indisponível; não implementar Google Meet.
- Usar migrações Flyway incrementais e manter arquivos históricos imutáveis.
- Aplicar as mesmas autorizações e projeções privadas nas consultas e exportações.

## Review Focus

- Datas limite e semana parcial: provar que filtros inválidos não transbordam e que nenhum calendário/fuso implícito cria obrigação.
- Vínculos de liderança: provar que consulta/exportação não permite acesso cruzado entre projetos.
- Conteúdo administrativo: provar que justificativas e notas acadêmicas não aparecem na projeção do gestor de projeto.
- Mudança de parâmetros: provar que versões históricas continuam reproduzíveis e vigências não se sobrepõem.
- Falta de serviço B05: provar que indicadores independentes funcionam e evidência documental ausente aparece como indisponível.

---

### Task 1: Apuração de participação B04

**Files:** `attendance` domain/application/API/infrastructure; nova migração Flyway; testes `AttendanceBoundaryTest`, `ParticipationReviewsTest` e teste de migração B07.

- [x] Acrescentar testes para agregação separada por modalidade, período sobreposto, período aberto e evento cancelado.
- [x] Acrescentar testes de exceções institucionais, dispensa sem crédito de horas, justificativa simples e reposição reconhecida somente após realização e aprovação.
- [x] Acrescentar revisão remota semanal em conjunto que atualize somente registros selecionados e preserve versões/históricos individuais.
- [x] Implementar persistência, regras transacionais, autorização administrativa e projeção operacional escopada ao líder sem motivo privado.
- [x] Executar testes direcionados e migração PostgreSQL.

### Task 2: Criticidade configurável de projeto

**Files:** novo domínio `projecthealth`; leitura de B02/B03 via contratos; migração Flyway; testes de autorização, versões, vigência e cálculo.

- [x] Testar insuficiência de atividades/eventos operacionais como `DADOS_INSUFICIENTES` e os limites da baseline explicitamente aprovada.
- [x] Testar limiares explícitos por projeto para atrasos, pendências e falta de atualização, incluindo motivos e fontes.
- [x] Testar acesso exclusivo a Gestão Geral/Coordenação e preservação de parâmetros anteriores.
- [x] Implementar parâmetros versionados, vigência, classificação explicável e histórico sem alterar `ProjectStatus` ou `attentionRequired`.
- [x] Executar testes direcionados e migração PostgreSQL.

### Task 3: Projeções de dashboard e análises históricas

**Files:** novos contratos de leitura nos domínios de origem; novo módulo `analytics`; controllers, consultas paginadas e testes por papel.

- [x] Testar painéis próprios, gestor contextual, gestão geral e coordenação; negar acesso a outros projetos.
- [x] Testar filtros relacionais por pessoa/projeto/período/situação e evolução temporal com proveniência/estados.
- [x] Testar que dados B05 indisponíveis e snapshots históricos ausentes não sejam inferidos como fatos.
- [x] Implementar semântica central dos indicadores já definida; publicar como indisponíveis medidas cuja fórmula institucional ainda esteja reservada.
- [x] Executar testes direcionados e regressões dos contratos afetados.

### Task 4: Relatórios CSV/PDF

**Files:** novo módulo de relatórios; contrato HTTP; exportadores CSV/PDF; testes de filtros, escaping, autorização e formato.

- [x] Testar os quatro modelos, filtros, geração rastreável e isolamento de dados por papel.
- [x] Testar CSV seguro para valores com separadores, aspas e prefixos de fórmula.
- [x] Testar PDF legível com período, critérios, geração e proveniência.
- [x] Reutilizar as projeções autorizadas do dashboard; documentar explicitamente métricas e fontes indisponíveis.
- [x] Executar testes e inspeção estrutural dos arquivos exportados.

### Task 5: Contratos, execução e revisão final

**Files:** `docs/implementation/bloco-07-api.md`, `docs/implementation/bloco-07-execution.md`, `Migrations/README.md`, `SecurityConfiguration.java` e teste integrado.

- [x] Documentar endpoints, filtros, estados, privacidade, paginação e formatos.
- [x] Registrar capacidades concluídas, pendências de definição institucional e resultado real das verificações.
- [x] Executar `mvnw.cmd --batch-mode verify` (244 testes; zero falhas/erros) e revisar integralmente o diff.
- [x] Executar `git diff --check` após documentação e revisão final.
