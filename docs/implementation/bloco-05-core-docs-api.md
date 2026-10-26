# B05 — núcleo documental, ONLYOFFICE e Diário contínuo

Implementação em branch derivada da `main`, com V6 documental e V7 para o Diário contínuo. O armazenamento foi escolhido exclusivamente para desenvolvimento local. Não há adaptador R2/S3 nem autorização de produção. A implementação não registra um `DocumentEvidenceReader` positivo no B06: ausência de evidência homologada continua indisponibilidade, nunca nota zero.

## Configuração

Por padrão o storage e o editor ficam desativados. O catálogo depende de PostgreSQL; operações com bytes exigem storage habilitado. Propriedades Spring e variáveis correspondentes em `apps/api/src/main/resources/application.yml`:

| Variável | Configuração local |
| --- | --- |
| `DOCUMENTS_LOCAL_DEVELOPMENT` | `true` para habilitar o adaptador local; perfis `prod`/`production` o desabilitam |
| `DOCUMENTS_STORAGE_ROOT` | Diretório absoluto privado fora de static/public, Downloads compartilhados e Git |
| `DOCUMENTS_EDITOR_ENABLED` | `true` somente para ensaio sintético com motor configurado |
| `DOCUMENTS_EDITOR_SERVER_URL` | Origem HTTPS do ONLYOFFICE, sem path/query/fragment; downloads/conversor presos a essa origem |
| `DOCUMENTS_EDITOR_JWT_SECRET` | Segredo próprio de pelo menos 32 bytes compartilhado com ONLYOFFICE; nunca retornar ao cliente |
| `DOCUMENTS_PUBLIC_BASE_URL` | Origem HTTPS desta API, alcançável pelo motor, sem path/query/fragment |
| `DOCUMENTS_CAPABILITY_SECRET` | Outro segredo de pelo menos 32 bytes, usado no HMAC das URLs efêmeras |
| `DOCUMENTS_EDITOR_ALLOW_INSECURE_LOOPBACK` | `false`; exceção explícita permite HTTP somente em localhost/127.0.0.1/::1 |

Nenhum `.env` ou credencial foi alterado. O operador deve fornecer os segredos por mecanismo privado, sem colocar valores em comandos compartilhados ou no repositório. Não reutilizar tokens Entra como segredo do motor. Uma origem HTTP de contêiner/rede privada não é loopback: use TLS/proxy local adequado. JWT do ONLYOFFICE deve sair no header `Authorization: Bearer ...`, com algoritmo HS256 e segredo correspondente. Não desabilitar JWT para contornar erros. O G1-F permanece NO-GO até a execução normal dos modelos oficiais e aceite editorial descritos no relatório de resultados.

Arquivos recebem chave UUID gerada pelo servidor; nome/caminho do cliente não é usado. Escrita `CREATE_NEW`, leitura limitada e verificação de tamanho/SHA-256 preservam versões. Links simbólicos/junctions são recusados. O diretório e ancestrais precisam de ACL do SO privada; este adaptador não configura permissões do Windows. Backups devem preservar banco e objetos juntos. Bytes gravados antes de uma transação falhar podem ficar órfãos recuperáveis; não há coletor nem exclusão automática.

## Contrato HTTP

Todas as APIs abaixo exigem JWT institucional Entra e membro ativo. Administração significa o contrato `AccessService.requireAdministrator`, sem autorização automática por login Microsoft. Coleções usam `page` (base zero) e `size` (1–100), resposta `PageResult`. Nenhum DTO público retorna hash privado ou caminho de storage.

| Método e recurso `/api/v1` | Comportamento |
| --- | --- |
| `GET/POST /document-templates` | Catálogo administrativo; criar `{title,official,reason}` |
| `GET /document-templates/{id}/versions` | Versões administrativas paginadas, com ID/revisão/autor/data |
| `POST /document-templates/{id}/versions` | Multipart `file`, `expectedVersion`, `reason`; cria versão de modelo imutável |
| `POST /document-templates/{id}/retire` | Administrativo `{expectedVersion,reason,confirmed:true}`; impede novas atribuições |
| `GET/POST /documents` | Lista filtrada por acesso; criação administrativa com `{title,context,templateVersionId,reason,...}` |
| `GET /documents/{id}` | Leitura contextual autorizada |
| `GET /documents/{id}/versions` | Histórico de metadados das versões disponíveis/quarentena |
| `GET /documents/{id}/history` | Trilha administrativa paginada |
| `POST /documents/{id}/versions` | Multipart sintético `file`, `expectedVersion`, `reason`; candidata disponível ou quarentena |
| `GET /documents/{id}/versions/{version}/download` | Bytes exatos autorizados; quarentena somente administrador |
| `POST /documents/{id}/grants` | Administrativo `{memberId,canEdit,expectedVersion,reason}`; troca concessão ativa |
| `POST /documents/{id}/grants/{member}/revoke` | Administrativo `{expectedVersion,reason}`; revoga concessão/ciclos |
| `POST /documents/{id}/archive` | Administrativo `{expectedVersion,reason,confirmed:true}`; preserva histórico e bloqueia edição |
| `POST /documents/{id}/restore` | Administrativo `{versionId,expectedVersion,reason,confirmed:true}`; aponta para versão disponível anterior, sem reescrever bytes |
| `POST /documents/{id}/editor-sessions` | `{expectedVersion,readOnly}`; retorna ID/prazo/configuração assinada para `DocsAPI.DocEditor` |
| `POST /documents/{id}/editor-sessions/{cycle}/close` | Editor autorizado revoga ciclo compartilhado; resposta 204 |
| `POST /documents/{id}/versions/{version}/pdf` | PDF ligado à versão solicitada, nunca à versão atual por presunção |

Contextos: `PERSONAL` exige `ownerId`; `PROJECT` exige `projectId`; `EVENT` exige `eventId`; `ADMINISTRATIVE` não aceita esses vínculos. `demandReference` opcional é referência textual, não FK a um módulo inexistente. Proprietário pessoal acessa seu documento; liderança acessa documentos do próprio projeto enquanto participa; concessão explícita habilita outros usuários. Documento de projeto exige também vínculo atual ao projeto, mesmo com concessão. Liderança não abre documento pessoal de membro por pertencer à equipe. Administração tem acesso contextual global. Criação/atribuição e concessões permanecem administrativas.

`expectedVersion` é a revisão otimista do registro do documento/modelo, diferente da `revision` sequencial dos bytes; quarentena e mudanças de ACL também incrementam a primeira. Revisão obsoleta retorna 409. Restauração recupera a seleção da versão, sem desarquivar o documento. Exclusão física não existe.

Downloads e PDF usam `Cache-Control: no-store`, `nosniff`, attachment e `X-Document-Version`. PDFs são cacheados por versão imutável; conversão falha sem criar PDF ou mudar a versão atual. Limite de objetos 8 MiB; ZIP/XML possuem limites adicionais contra expansão, partes perigosas e DTD/entidades. Resultado PDF exige assinatura `%PDF-`; não é certificação visual do conteúdo.

## Protocolo do motor

O canal `/api/v1/document-provider/**` tem cadeia de segurança própria: não aceita token do motor como identidade Entra. Apenas GET `/sources/{id}?token=...` e POST `/cycles/{id}/callback` são admitidos; cada um valida seu mecanismo específico.

- Origem: HMAC, TTL e autorização atual; grant por ator, documento e versão exata. Sessão: até 15 minutos, chave colaborativa compartilhada por documento/modo; fonte inicial fica fixada no ciclo. Conversão: grant de 5 minutos fixado na versão solicitada.
- Callback: JWT assinado, body vinculado aos claims relevantes, chave/ciclo ativos e autorização atual do iniciador e dos autores assinados. `users` representa os atores declarados pelo motor; a aplicação não infere autoria por nome no documento.
- Status 1 registra presença; 2 salva e fecha; 6 force-save sem fechar; 4 fecha sem alterações; 3/7 registram falha e encerram o ciclo. Após 4/3/7 é necessário abrir outro ciclo; reconexão tardia do motor ao mesmo ciclo não é retomada automaticamente.
- Receipt identifica payload efetivo (key/status/url/users); replay autenticado já recebido é reconhecido sem outra versão, mesmo após fechamento. URL diferente constitui outra candidata, ainda sujeita ao ciclo e ao guard. Downloads só na origem configurada, sem redirects, com limite e timeout.
- I/O remoto ocorre fora do lock do banco; publicação revalida permissões/estado/revisão. Save concorrente atrasado pode ir para quarentena `STALE_HEAD`; ciclo revogado/expirado é recusado. Erro de download não altera a versão vigente; o motor pode repetir enquanto o ciclo permanecer válido.
- Uma candidata bem formada pode ser persistida em `QUARANTINED`. `{"error":0}` confirma recebimento durável, não aprovação de fidelidade. Histórico expõe rejeição e versão de origem. Recuperação administrativa não promove quarentena.

Revogação/novo upload/restore invalidam ciclos abertos. Um editor pode encerrar o ciclo compartilhado e afetar colaboradores: é encerramento explícito, não descarte de bytes já recebidos. Novas sessões exigem a revisão vigente.

O guard Java é adicional e conservador: apenas conteúdo em nós `w:t` já existentes pode mudar; estrutura XML, atributos, campos e partes protegidas devem permanecer. Metadados existentes `docProps/core.xml` são a exceção delimitada. Não aceita inserção de runs/parágrafos/partes; rejeições do Word/ONLYOFFICE podem ser falsos positivos e ficam em quarentena. Não é o gate Python experimental, não habilita uma equivalência global e não homologa semântica/layout. Documentos oficiais recebem veto fixo a edição e conversão, inclusive se o editor estiver habilitado. Cadastro, atribuição e download não representam autorização de uso institucional.

## Validação e limites

Testes automatizados usam documentos sintéticos e PostgreSQL efêmero via Testcontainers. A interface mínima em `apps/web` mostra o Diário e o painel semanal, mas depende do provedor Entra para receber token e não simula login. Estes testes não substituem teste normal do runtime ONLYOFFICE no navegador, round-trip Word ou homologação editorial. Não há implantação, provisionamento de storage remoto, integração com SharePoint Graph ou liberação de edição oficial.

O esquema usado por esta branch termina em V7. A suíte testa inicialização vazia até V7 e upgrade V5→V7, incluindo preservação da auditoria existente. A numeração foi escolhida após auditoria somente leitura dos bancos identificados; confira as ressalvas sobre o serviço PostgreSQL local não identificado no relatório de resultados antes de migrar qualquer banco persistente.

## Referências do protocolo

Conferidas em 23/10/2026: [callback/status/users](https://api.onlyoffice.com/docs/docs-api/usage-api/callback-handler), [JWT de saída no header](https://api.onlyoffice.com/docs/docs-api/additional-api/signature/request/token-in-header), [assinatura no body para conversão](https://api.onlyoffice.com/docs/docs-api/additional-api/signature/request/token-in-body) e [Conversion API](https://api.onlyoffice.com/docs/docs-api/additional-api/conversion-api/request). São contratos do fornecedor, não prova da execução do runtime neste checkout.
