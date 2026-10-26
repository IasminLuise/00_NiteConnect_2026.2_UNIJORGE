<!-- BEGIN:nextjs-agent-rules -->

## This is NOT the Next.js you know

This version has breaking changes — APIs, conventions, and file structure may all differ from your training data. Read the relevant guide in `node_modules/next/dist/docs/` (resolved from this file's directory; in monorepos the `next` package may not be visible from the repo root) before writing any code. Heed deprecation notices.

This block is written and re-added by `next dev` — verify at `node_modules/next/dist/server/lib/generate-agent-files.js`. Removing it from a diff only re-creates the uncommitted change; committing it with your work keeps the tree clean.

<!-- END:nextjs-agent-rules -->

## Regras do projeto

- Consulte a especificação funcional consolidada e contratos em `docs/implementation` antes de implementar fluxos reais. Decisões explícitas mais recentes prevalecem sobre hipóteses antigas.
- A rota `/foundation` é neutra e não representa autenticação ou dashboard operacional. A rota `/` contém o Diário contínuo em integração; não invente dados, papéis, notas ou reuniões.
- Use Next.js App Router, React e TypeScript estrito. Prefira Server Components; use componentes de cliente apenas quando houver interatividade.
- O backend é autoridade de identidade, permissões e regras. Não implemente login demonstrativo, armazenamento local de access tokens ou segredos no cliente.
- A integração Entra e a estratégia de sessão segura precisam estar definidas antes de conectar autenticação ao frontend.
- Execute `npm run lint`, `npm run typecheck`, `npm run test` e `npm run build` antes de afirmar conclusão.
- Versione `package-lock.json` e use `npm ci` na CI. Não altere contratos, migrações, infraestrutura ou documentação histórica sem necessidade explícita.
