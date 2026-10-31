# NITE Connect Web

Frontend Next.js App Router, React e TypeScript estrito em `apps/web`. O login
institucional usa Microsoft Entra ID com Authorization Code + OIDC/PKCE pelo
servidor Next.js (BFF). O servidor valida o token delegado em
`GET /api/v1/me` antes de criar uma sessão para integrante ativo.

## Execução local

Use Node.js 24 LTS e npm:

```powershell
npm ci
npm run dev
```

Defina as variáveis de `apps/web/.env.example` em um arquivo `.env.local` local.
Não versione credenciais. O fluxo e as instruções de registro estão em
[`docs/implementation/auth-web-entra.md`](../../docs/implementation/auth-web-entra.md).

Validação local: `npm run lint`, `npm run typecheck`, `npm run test` e `npm run build`.

## Sessão e chamadas protegidas

- O navegador recebe apenas um cookie `HttpOnly`, `SameSite=Lax`; em produção
  ele usa `Secure` e o prefixo `__Host-`.
- A sessão Auth.js é um JWE criptografado com `AUTH_SECRET`, dura até oito horas
  e mantém access/refresh tokens inacessíveis ao JavaScript do navegador.
- O servidor pede access tokens para o escopo delegado da API, confere a
  identidade e os papéis atuais com `GET /api/v1/me` e renova tokens expirados.
- A API continua responsável por autorização em cada endpoint; o estado e os
  papéis do cookie servem à interface e não substituem as verificações do backend.
- `API_BASE_URL` é usado exclusivamente pelo servidor Next.js. A aplicação não
  grava tokens em `localStorage` ou `sessionStorage` e não solicita permissões do
  Microsoft Graph.

## Rotas autenticadas e shell

- As páginas operacionais ficam em `src/app/(authenticated)/`. O route group não
  altera os caminhos públicos e aplica a validação de sessão no layout compartilhado.
- O shell de `src/components/shell/` recebe apenas dados de apresentação, papéis e
  contextos de liderança autorizados; ele não depende dos componentes do dashboard.
- O seletor troca o contexto na rota atual. A navegação lateral só liga para páginas
  existentes; módulos ainda sem tela permanecem desabilitados e identificados como
  “Em desenvolvimento”.
- Login, `/`, `/foundation` e ferramentas de prévia em desenvolvimento ficam fora do
  route group autenticado.
