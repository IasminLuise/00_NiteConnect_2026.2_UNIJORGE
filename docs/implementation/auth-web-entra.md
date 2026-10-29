# Login Web — Microsoft Entra ID

## Fluxo

1. A tela envia o usuário ao provedor OIDC `microsoft-entra-id` do Auth.js.
2. O Next.js executa Authorization Code com PKCE, `state` e `nonce`; o navegador
   não recebe um access token legível pelo JavaScript.
3. No callback, o servidor envia o access token como `Authorization: Bearer` a
   `GET /api/v1/me`. A API valida issuer, audience, tenant e escopo e só responde
   com a identidade quando o integrante está ativo.
4. Apenas essa identidade aprovada é incluída na sessão do NITE. O cookie de
   sessão Auth.js é JWE criptografado, `HttpOnly`, `SameSite=Lax`, `Secure` em
   produção e limitado a oito horas. O cookie de produção usa o prefixo `__Host-`.
5. Access/refresh tokens permanecem no payload criptografado que só o servidor
   lê. Access tokens são renovados antes do vencimento; o refresh token rotacionado
   substitui o anterior quando a Microsoft envia um novo. Nenhum token é exposto
   pelo objeto de sessão ao frontend.
6. Sair encerra a sessão do NITE e remove o cookie da aplicação. A sessão SSO da
   Microsoft no navegador não é encerrada.

Os papéis mostrados na tela são uma projeção de `GET /api/v1/me`. A API reavalia
estado e autorização no banco a cada chamada; o cookie não concede acesso a rotas.
O login não cria cadastro nem ativa convite.

## Registros no Microsoft Entra

São necessários um registro de API (o `ENTRA_AUDIENCE` já esperado pelo backend)
e um registro de cliente Web.

### API

1. Configure **Expose an API** com Application ID URI `api://<API_CLIENT_ID>`.
2. Exponha o escopo delegado `access_as_user` para usuários autenticados.
3. Configure a API com `ENTRA_ENABLED=true`, `ENTRA_TENANT_ID=<TENANT_ID>`,
   `ENTRA_AUDIENCE=<API_CLIENT_ID>` e `ENTRA_REQUIRED_SCOPE=access_as_user`.

### Cliente Web

1. Crie um App Registration **single tenant** e adicione a plataforma **Web**.
2. Cadastre estes Redirect URIs, substituindo o host de produção pelo domínio
   HTTPS real do frontend:
   - Local: `http://localhost:3000/api/auth/callback/microsoft-entra-id`
   - Produção: `https://<DOMINIO_WEB>/api/auth/callback/microsoft-entra-id`
3. Crie um client secret para o servidor. Guarde o valor somente no secret
   manager do ambiente; não o versione nem o envie ao navegador.
4. Em **API permissions**, adicione a permissão **delegated**
   `api://<API_CLIENT_ID>/access_as_user` e obtenha consentimento conforme a
   política institucional do tenant.
5. Não adicione `User.Read` nem permissões de aplicação do Microsoft Graph.

## Variáveis do frontend

Configure no servidor do frontend (exemplo local em `.env.local`, nunca commitado):

| Variável | Valor |
|---|---|
| `AUTH_SECRET` | Segredo aleatório de pelo menos 32 bytes, exclusivo deste ambiente |
| `AUTH_URL` | Origin canônica do frontend; local `http://localhost:3000`, produção HTTPS |
| `AUTH_TRUST_HOST` | `true` apenas quando o host é fornecido por proxy confiável |
| `AUTH_MICROSOFT_ENTRA_ID_ID` | Application (client) ID do **cliente Web** |
| `AUTH_MICROSOFT_ENTRA_ID_SECRET` | Valor do client secret do cliente Web |
| `ENTRA_TENANT_ID` | Directory (tenant) ID institucional, UUID |
| `ENTRA_API_SCOPE` | `api://<API_CLIENT_ID>/access_as_user` |
| `API_BASE_URL` | Origin HTTPS da API; em desenvolvimento, `http://localhost:8080` |

Gere `AUTH_SECRET` localmente com Node.js:

```powershell
node -e "console.log(require('node:crypto').randomBytes(32).toString('base64'))"
```

As variáveis Microsoft e o domínio definitivo dependem do tenant e do registro
institucional. A configuração de código não cria nem altera recursos no Entra.
