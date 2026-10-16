# Bloco 02 — contratos HTTP

Todas as rotas exigem Bearer Microsoft Entra e integrante ATIVO, além das
permissões abaixo. Entra desabilitado continua deny-all. Erros, UUIDs, paginação
e ProblemDetail seguem o Bloco 01: 400 inválido, 401 token ausente/inválido,
403 autoridade insuficiente, 404 inexistente ou recurso de outro contexto,
409 versão/estado/vínculo conflitante. Sem dados pessoais de colegas.

Listas: `{items, page, size, total}`, `page=0`, `size=20`, máximo 100.
Projetos ordenados por título/id; períodos e eventos por data/id.

## Dados e concorrência

Projeto: `id, title, description, status, startsOn, endsOn, proposalId, createdBy,
version, createdAt, updatedAt`. Datas opcionais ISO LocalDate; término não pode
anteceder início. Não geram transições automáticas. Título obrigatório até 200,
descrição obrigatória até 10000. Motivos obrigatórios até 1000, em corpo JSON.

Todas as mutações de projeto existente recebem `expectedVersion` do **projeto**;
inclusive equipe e liderança incrementam essa versão. Retorne a consultar o
projeto após cada operação. Propostas têm versão própria, também obrigatória em
edição, decisão e conversão. Versões inteiras >=0. Inclusões novas começam em 0.

## Projetos

| Método / rota | Entrada | Autorização / saída |
|---|---|---|
| GET `/api/v1/projects` | page, size, status opcional | Administração: todos; demais: participações vigentes; Page de Projeto |
| GET `/api/v1/me/projects` | page, size, status opcional | Somente participações vigentes do próprio ator, inclusive para administradores |
| GET `/api/v1/projects/{id}` | — | Participante vigente ou administração; Projeto |
| POST `/api/v1/projects` | title, description, startsOn?, endsOn?, reason | Administração; 201 Projeto PLANEJAMENTO |
| PATCH `/api/v1/projects/{id}` | title, description, startsOn?, endsOn?, expectedVersion, reason | Gestor contextual/administração; 200 Projeto; campos de dados substituídos integralmente |
| PATCH `/api/v1/projects/{id}/status` | status, expectedVersion, reason | Administração; 200 Projeto |
| GET `/api/v1/projects/{id}/history` | page, size | Gestor contextual/administração; Page de eventos de estado, incluindo criação |
| GET `/api/v1/projects/{id}/members` | page, size | Participante/administração; resumo sem e-mail, estado administrativo ou motivos |
| POST `/api/v1/projects/{id}/members` | memberId, responsibility, expectedVersion, reason | Gestor contextual/administração; 201 período; exige alvo ATIVO |
| DELETE `/api/v1/projects/{id}/members/{memberId}` | expectedVersion, reason | Gestor contextual/administração; 204, encerra período vigente; liderança precisa ser encerrada antes |
| GET `/api/v1/projects/{id}/members/history` | page, size | Gestor contextual/administração; Page de períodos completos |
| GET `/api/v1/projects/{id}/leadership` | — | Participante/administração; memberId (nullable), effective |
| POST `/api/v1/projects/{id}/leadership-transfers` | memberId (nullable), expectedVersion, reason | Administração; 200 liderança; null estabelece vacância explícita |
| GET `/api/v1/projects/{id}/leadership/history` | page, size | Gestor contextual/administração; Page de períodos de liderança |
| GET `/api/v1/me/projects/history` | page, size | Histórico pessoal: período, responsabilidade, projectId/title; sem dados gerenciais |
| GET `/api/v1/me/projects/leadership/history` | page, size | Períodos de liderança do próprio ator, com projectId/title; sem motivos administrativos |

Liderança não cria automaticamente participação. Designe primeiro a participação,
depois transfira a liderança. Transferência preserva ambas as participações e fecha
apenas a liderança anterior. `effective=false` indica vacância ou líder indisponível;
o estado atual do integrante é verificado sem apagar o vínculo histórico.

Transições aprovadas: PLANEJAMENTO→ATIVO; ATIVO→PAUSADO/CONCLUIDO;
PAUSADO→ATIVO/CONCLUIDO; CONCLUIDO→ATIVO. Repetição de estado é 409.
Nenhum estado impede por si só manutenção autorizada de dados/equipe.

## Propostas

Proposta: `id, title, description, authorId, status, decidedBy, decidedAt,
decisionReason, version, createdAt, updatedAt`. Limites de título/descrição iguais
aos projetos. `decisionReason` acessível apenas ao autor e administração.

| Método / rota | Entrada | Autorização / saída |
|---|---|---|
| GET `/api/v1/proposals` | page, size, status opcional | Autor vê as próprias; administração todas |
| POST `/api/v1/proposals` | title, description, reason | ATIVO; autor derivado da identidade; 201 PENDENTE |
| GET `/api/v1/proposals/{id}` | — | Autor/administração; 200 Proposta |
| PATCH `/api/v1/proposals/{id}` | title, description, expectedVersion, reason | Autor/administração, somente PENDENTE; 200 Proposta |
| POST `/api/v1/proposals/{id}/decisions` | status (APROVADA ou REJEITADA), expectedVersion, reason | Administração, somente PENDENTE; 200 Proposta |
| POST `/api/v1/proposals/{id}/convert` | expectedVersion, reason | Administração, somente APROVADA; 201 Projeto |

Conversão usa título/descrição aprovados, datas de planejamento inicialmente nulas,
cria PLANEJAMENTO e preserva `proposalId`; proposta se torna CONVERTIDA na mesma
transação. Repetição ou concorrência com versão anterior: 409, sem projeto adicional.

## Auditoria e históricos

GET `/api/v1/audit-events` continua administrativo e preserva campos antigos.
Acrescenta `entityType` (MEMBER/PROJECT/PROPOSAL) e `entityId`. Para eventos novos
de projeto/proposta, `memberId` é null; eventos de integrantes mantêm esse campo.
Nos registros anteriores à V3, `entityId` é lido a partir de `memberId`, sem
reescrita. Consumidores devem usar entityType/entityId para identificar o alvo.

Criação/edição, mudança de estado, entrada/saída, liderança, decisão e conversão
geram auditoria transacional. Falha de auditoria desfaz toda a operação. Históricos
de estado e auditoria são imutáveis; períodos só podem ser encerrados uma vez.
Nenhuma rota de exclusão física de projetos, propostas ou históricos é exposta.

## Validação

Na pasta `apps/api`, Java 21 e Docker disponíveis:

```powershell
.\mvnw.cmd --batch-mode '-Dtest=ProjectsBoundaryTest' test
.\mvnw.cmd --batch-mode verify
```

Os testes automatizados usam identidades fictícias e PostgreSQL 17 isolado por
Testcontainers. Nenhum deles migra o banco local de desenvolvimento ou produção.
Para executar essa suíte, não é necessário novo serviço externo, credencial,
configuração Entra ou alteração de `.env`.

Em 16/10/2026, a suíte integral passou com 111 testes e PostgreSQL 17 via
Testcontainers. No ensaio local da API integrada em `127.0.0.1:8081`, o Postman
Web com Desktop Agent e OAuth PKCE recebeu HTTP 200 em `GET /api/v1/me` e
`GET /api/v1/projects`; a listagem de projetos estava vazia. As chamadas sem
token a essas rotas receberam HTTP 401. A URL `8081` identifica somente o
ensaio local; o contrato HTTP é independente da porta de implantação. Consulte
as [evidências de execução](homologacao-bloco-02-local.md) e o
[roteiro de staging](staging-bloco-02.md). Os demais fluxos do Bloco 02 foram
verificados automaticamente com identidades sintéticas; o aceite institucional
formal continua pendente.
