# Bloco 03 final UI refinement Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refinar lista, detalhe e padrões visuais de atividades sem alterar os contratos institucionais B03.

**Architecture:** Manter páginas autenticadas como Server Components e limitar interatividade a folhas client com estado local. Reutilizar tokens, Dialog existente, Server Actions, capacidades B03 e consultas paginadas reais.

**Tech Stack:** Next.js 16 App Router, React 19, TypeScript estrito, Tailwind CSS 4, Vitest.

**Spec:** `C:/Users/breno/.codex/attachments/fe93441f-680d-4a85-a6e2-679c6a3bea55/Pasted text.txt`

## Global Constraints

- Preservar rotas, shell, Microsoft Entra, contratos, permissões e dados B03 existentes.
- Estados oficiais: `PLANEJADA`, `EM_EXECUCAO`, `CONCLUIDA`, `CANCELADA`; atraso permanece condição derivada.
- Mutação exige motivo e `expectedVersion`; conflito `409` não repete a escrita.
- Upload aparece apenas quando a capacidade contextual `upload` autoriza.
- Reutilizar o Design System e não adicionar dependências.
- Não fazer commit/push e não iniciar o Bloco 04.

## Review Focus

- Pesquisa local deve ser somente sobre a página carregada — a busca ampla permanece no servidor. Teste: `activityQuery` preserva `q` e `page` ao navegar/retornar.
- Confirmação é necessária somente para cancelamento ou reabertura. Teste: destino comum não mostra confirmação e destino crítico a exige.
- Candidatos vazios, sem permissão e com falha devem divergir. Teste: cada `ApiIssue` tem mensagem contextual e nenhum caso vazio é erro.
- Paginação inexistente não deve ocupar a interface. Teste: lista e histórico de uma página omitem navegação.
- Mídia privada não deve receber URLs externas ou caminhos internos. Teste: download usa apenas a rota privada autenticada já existente.

---

### Task 1: Lista e semântica de situação

**Files:** `activity-list.tsx`, `activity-list.test.tsx`, página `atividades/page.tsx`, tokens em `globals.css`.

- [ ] Testar visual semântico de quatro estados, atraso distinto, busca visível, filtros avançados recolhidos e paginação apenas com múltiplas páginas.
- [ ] Implementar badge reutilizável com tokens Light/Dark e metadados legíveis.
- [ ] Compactar navegação de situação em faixa horizontal; recolher filtros avançados e mostrar filtros ativos.
- [ ] Remover texto de ordenação fixa e ocultar paginação em página única.
- [ ] Rodar os testes focados de lista.

### Task 2: Ações contextuais e evidências

**Files:** `activity-detail.tsx`, novos componentes de diálogo/forms em `components/activities/`, respectivos testes.

- [ ] Testar ações autorizadas, título “Ações”, formulário de evidência por tipo e confirmações apenas críticas.
- [ ] Implementar diálogos de edição e situação com motivo, versão, confirmação condicional, teclado e responsividade.
- [ ] Implementar uma entrada “Adicionar evidência” com um tipo selecionado; preservar upload e retirada condicionados a capacidade.
- [ ] Rodar testes focados de detalhe e diálogos.

### Task 3: Participantes e ordem móvel

**Files:** detalhe `page.tsx`, `data.ts`, `activity-detail.tsx`, testes relacionados.

- [ ] Testar pesquisa/paginação de candidatos e estados vazio, acesso negado e erro.
- [ ] Adicionar filtros `candidateQ`/`candidatePage` às consultas reais existentes.
- [ ] Mostrar propriedades essenciais antes do histórico em mobile e preservar paginação real do histórico.
- [ ] Rodar testes focados e typecheck.

### Task 4: Revisão visual e validação

**Files:** componentes B03 afetados e capturas em `.playwright-cli/bloco-03-visual/`.

- [ ] Validar Light/Dark, larguras 320, 360, 768, 1024 e 1440 px; teclado, foco, Escape e overflow.
- [ ] Capturar comparativos antes/depois de lista, detalhe, situação, formulários, evidências e mobile.
- [ ] Executar lint, typecheck, testes, build e `git diff --check`; registrar limites de integração.

