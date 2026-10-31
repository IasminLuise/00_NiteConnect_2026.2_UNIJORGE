# NITE Connect — Instruções para agentes de desenvolvimento

## Fonte de verdade
- Consulte `docs/product/NITE_Connect_Especificacao_Funcional_Consolidada_v1.0.pdf` antes de implementar regras de negócio.
- Decisões funcionais posteriores e explicitamente aprovadas têm precedência sobre PDFs históricos e sobre o antigo README.
- Identifique regras institucionais não homologadas; não invente pesos, prazos, tolerâncias, critérios de desligamento ou normas universitárias.
- Não trate o login Microsoft como autorização automática ao NITE Connect.

## Arquitetura
- Monorepo leve: `apps/api` (Java + Spring Boot) e `apps/web` (frontend Next.js ativo).
- Backend: monólito modular, organizado por domínio e não por diretórios globais `controllers/services/repositories`.
- PostgreSQL é o banco principal. Dados e alterações de esquema devem passar por Flyway; não usar `ddl-auto=update`.
- Não adicione microsserviços, filas, caching distribuído, plataformas de orquestração ou abstrações genéricas sem necessidade aprovada.
- Interfaces entre módulos devem depender de contratos explícitos; não acessar tabelas ou classes internas de outro domínio indiscriminadamente.

## Método de trabalho
- Implementar somente o bloco combinado e seus pré-requisitos reais.
- Antes de uma alteração: identificar requisito(s), impacto, teste e segurança.
- Adicionar testes para novos comportamentos e correções; executar a suíte aplicável antes de declarar conclusão.
- Atualizar o README apenas quando comandos, dependências ou arquitetura realmente mudarem.
- Não implementar lógica nova a partir do documento inicial que contradiga a especificação funcional consolidada.
- Não fazer commits diretamente na `main`; trabalhar em branch específica e abrir PR para revisão.
- Não apagar trabalho de outros colaboradores sem discussão e confirmação.

## Segurança e integridade
- Nunca commitar credenciais, tokens, `.env`, documentos privados ou dados reais de estudantes.
- Autenticação institucional: Microsoft Entra; autorizações operacionais ficam no backend.
- Verificar acesso por perfil, vínculo e contexto; negar por padrão.
- Não alterar registros históricos ou migracões Flyway já aplicadas; criar novas migrações.
- Preservar trilha de auditoria em alterações administrativas relevantes quando a funcionalidade for implementada.

## Critérios gerais para concluir um bloco
1. Código compila e verificações automatizadas estão verdes.
2. Testes de regras, falhas e acesso indevido cobrem o bloco.
3. Migrações foram verificadas contra PostgreSQL real quando houver persistência.
4. APIs respeitam validação, autenticação/autorização e contratos documentados.
5. Funcionalidade está revisada e nenhuma dependência de regra institucional foi presumida.

Veja `apps/api/AGENTS.md` antes de alterar o backend.
