# NITE Connect Web

Frontend Next.js App Router, React e TypeScript estrito em `apps/web`. As rotas `/` e `/foundation` apresentam a página neutra de fundação.

## Execução local

Use Node.js 24 LTS e npm:

```powershell
npm install
npm run dev
```

Validação local: `npm run lint`, `npm run typecheck`, `npm run test` e `npm run build`.

## Configuração

`API_BASE_URL` está reservada para futuras chamadas servidor-servidor e não é consumida atualmente. Não há login demonstrativo nem armazenamento local de tokens; a integração Microsoft Entra ainda não está conectada.
