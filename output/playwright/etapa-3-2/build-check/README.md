# NITE Connect Web

Frontend Next.js App Router, React e TypeScript estrito em `apps/web`. Inclui uma página neutra em `/foundation` e a interface inicial do Diário contínuo em `/`.

## Execução local

Use Node.js 24 LTS e npm:

```powershell
npm install
npm run dev
```

Validação local: `npm run lint`, `npm run typecheck`, `npm run test` e `npm run build`.

## Diário contínuo

A interface consulta `GET /api/v1/diaries/me` e `GET /api/v1/diaries/me/activities`, abre sessões ONLYOFFICE pelo fluxo de documentos e baixa DOCX/PDF da versão exata.

- `NITE_CONNECT_API_URL`: origem HTTP(S) do backend. O proxy Next.js mantém a chamada same-origin e encaminha apenas as rotas permitidas do Diário/editor/download.
- `NEXT_PUBLIC_ONLYOFFICE_ORIGIN`: origem usada para validar o script `api.js` devolvido pela sessão do editor.
- `API_BASE_URL`: configuração reservada para chamadas servidor-servidor futuras; não é consumida atualmente.
- `DiaryWorkspace` recebe um access token do provedor institucional. A integração Microsoft Entra ainda não está conectada, por isso a página inicial fica sem sessão.

Não há login demonstrativo nem armazenamento local de tokens. O backend é a autoridade de autorização. O provider documental permanece opt-in para desenvolvimento e a edição de modelos oficiais continua bloqueada pelo gate G1-F.
