# NITE Connect Web — Diário contínuo

Interface inicial em Next.js/TypeScript para o Diário semanal. A página consulta `GET /api/v1/diaries/me` e `GET /api/v1/diaries/me/activities`, abre sessões ONLYOFFICE pelo fluxo de documentos e baixa DOCX/PDF da versão exata.

## Configuração local

- `NITE_CONNECT_API_URL`: origem HTTP(S) do backend. O proxy Next.js mantém a chamada same-origin e só encaminha as rotas do Diário/editor/download explicitamente permitidas.
- `NEXT_PUBLIC_ONLYOFFICE_ORIGIN`: origem confiável usada para validar o script `api.js` devolvido pela sessão do editor.
- O componente `DiaryWorkspace` recebe `accessToken` do provedor de autenticação institucional. Esta bancada não cria login ou armazenamento local de tokens; a página inicial fica em estado sem sessão até a integração Entra ser conectada.

O serviço backend local deve ter o provider documental ativado apenas em ambiente de desenvolvimento. A edição de modelos oficiais permanece bloqueada pelo gate G1-F.

```powershell
npm install
npm run dev
npm run typecheck
npm run build
```
