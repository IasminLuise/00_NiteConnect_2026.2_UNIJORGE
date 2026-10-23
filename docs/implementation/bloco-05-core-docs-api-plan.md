# B05 Core e Docs API — execução em desenvolvimento

Autorização: pedido do usuário em 23/10/2026 para implementar o planejamento ADR B05-005, seguido de escolha explícita de armazenamento **somente local de desenvolvimento**. A autorização não é aceite editorial/legal ou habilitação de modelos oficiais. Base isolada: head do B06 `01b3fd9`, sem merge dos PRs existentes; branch `feat/b05-core-docs-api`.

## Contrato da implementação

- M1: catálogo de modelos versionados, documentos por contexto, ACL explícita, versões imutáveis, histórico, download autorizado, arquivamento e recuperação sem exclusão física.
- M2: provider ONLYOFFICE Docs API desativado por padrão; sessões/ciclos com chave colaborativa compartilhada, URLs efêmeras, JWT assinado, callbacks idempotentes, downloads limitados e sem redirects, candidatas persistidas disponíveis ou em quarentena, conversão PDF ligada à versão exata.
- Storage exige configuração local de desenvolvimento explícita; nenhum fornecedor remoto, credencial, `.env`, recurso de produção ou endpoint frontend existente será criado/alterado por presunção.
- Modelos oficiais podem ser catalogados/atribuídos sob administração, mas não editados ou convertidos pelo motor. Edição/conversão disponíveis apenas para sintéticos em desenvolvimento.
- O guard Java adicional é conservador; só aceita texto w:t mantendo estrutura/partes protegidas. Ele não substitui nem flexibiliza o gate experimental e não certifica fidelidade global.
- B06 mantém integração documental indisponível: não existe versão homologada e nenhum provider positivo acadêmico será registrado para documentos sintéticos.
- Concorrência segue transações/locks já adotados pelo monólito, CAS do documento, invalidação dos ciclos na mudança de ACL e revisão das permissões no callback. I/O remoto ocorre fora do lock transacional.

## Etapas verificáveis

1. Testar bytes privados e guard OOXML (ZIP/XML inseguro, campo, partes, fonte e espaçamento).
2. Adicionar V8 após V6/V7, domínio/serviços/APIs; testar ACL pessoal/projeto, inatividade, revisões obsoletas, histórico e rollback com PostgreSQL real.
3. Implementar protocolo Docs API e testar servidor HTTP sintético: JWT, SSRF/redirect, falhas e conversão.
4. Integrar ciclos/callbacks/representações; testar replay, revogação, alteração concorrente, quarentena e preservação da versão anterior.
5. Executar suíte Java/PostgreSQL aplicável, conferir contratos HTTP e diff, revisão independente e relatório de execução.

M3: permanece bloqueado pelos vetos documentais já demonstrados e pelas aprovações editorial/licenciamento/operação pendentes; não repetir as campanhas exploratórias ou habilitar edição oficial.
