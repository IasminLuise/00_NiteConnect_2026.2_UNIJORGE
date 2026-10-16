# Bloco 02 — projetos, propostas, equipes e liderança

## Contrato aprovado (B02.1)

Implementação única das sete etapas do planejamento fornecido pelo responsável.
Fonte funcional: especificação consolidada v1.0, páginas 4, 6, 9–11; RF-008,
RF-009, RF-014 (participações), RF-029, RF-030, CA-08, CA-15 e CA-16.

As seis decisões abaixo foram explicitamente aprovadas na conversa de implementação:

- Integrantes ATIVOS cadastram propostas e consultam as próprias; somente propostas
  PENDENTES podem ser editadas pelo autor ou administração. Administração consulta todas.
- Proposta: PENDENTE → APROVADA ou REJEITADA; APROVADA → CONVERTIDA.
  Decisões finais, justificadas; apenas administração decide e converte.
- Administração global altera estados; liderança contextual edita dados e equipe.
- Projeto: PLANEJAMENTO → ATIVO; ATIVO → PAUSADO ou CONCLUIDO;
  PAUSADO → ATIVO ou CONCLUIDO; CONCLUIDO → ATIVO. Sem transição automática.
- Ex-participante ATIVO consulta seus períodos e identificação mínima dos projetos;
  histórico gerencial somente para administração e liderança vigente.
- Liderança exige participação vigente e integrante ATIVO. Saída de líder exige
  transferência ou vacância administrativa explícita anterior.

Não há papel global GESTOR_PROJETO. Estado administrativo atual é verificado em
cada requisição; afastamento/desligamento torna liderança inefetiva sem apagar
períodos nem transferir automaticamente a responsabilidade. Reativação pode tornar
efetiva uma liderança ainda vigente; transferência/vacância encerra o período e
impede sua restauração por reativação.

## Arquitetura e invariantes

Java 21, Spring Boot, JdbcTemplate e PostgreSQL/Flyway existentes, sem dependências
novas. `projects` e `proposals` possuem contratos explícitos. `ProjectDirectory`
expõe consultas de vínculo a `access`; acesso aos integrantes usa `MemberDirectory`
e `AccessService`. Auditoria compartilhada ampliada por migração aditiva, preservando
registros antigos e o contrato `record` do Bloco 01.

Alterações usam o mesmo advisory lock transacional do Bloco 01, adquirido antes da
revalidação da autorização. Versão do projeto protege também equipe e liderança;
versão da proposta protege edição, decisão e conversão. Serialização global é
intencional para o baixo volume administrativo atual; não introduzir cache/fila.

Participações e lideranças têm períodos separados: transferência não encerra a
participação do gestor anterior. Índices únicos parciais impedem dois vínculos
abertos por pessoa/projeto e duas lideranças abertas por projeto. Históricos não
são excluídos; períodos fechados não são reescritos. Conversão cria projeto e
atualiza proposta com auditoria na mesma transação; origem é única.

## Execução integrada

- [x] B02.1: inspecionar código/PDF e formalizar as seis decisões, DTOs e matriz de testes.
- [x] B02.2: V3 aditiva; testar banco limpo e evolução V2 com históricos existentes.
- [x] B02.3: API de projetos, dados, filtros, paginação e trajetória de estados.
- [x] B02.4: participação/saída/retorno, transferência/vacância e históricos.
- [x] B02.5: autorização contextual, isolamento e revogação imediata.
- [x] B02.6: propostas/decisões/conversão única e auditoria transacional.
- [x] B02.7: testes negativos/concorrência/regressão, revisão e documentação; validação local concluída, PR #4 revisado, checks aprovados e merge realizado.

Cada frente começa com testes que demonstram o comportamento ausente, segue com
implementação e validação direcionada. Encerramento: `mvnw.cmd --batch-mode verify`
com PostgreSQL real por Testcontainers e revisão do diff. Nenhuma regra de tarefas,
progresso, indicadores, calendário, documentos, AV3 ou frontend é antecipada.

## Registro de execução

- Checkout inicialmente limpo, `main` local em `40eb098`.
- Branch: `feat/bloco-02-projetos-equipes`.
- Fetch SSH inicial indisponível (`Permission denied (publickey)`); referência
  `origin/main` local coincidia com `main`. Consulta posterior autenticada via
  `gh api repos/IasminLuise/00_NiteConnect_2026.2_UNIJORGE/commits/main --jq .sha`
  confirmou o mesmo `40eb098fe0449b80f7917085e811632647ed85c6` no servidor.
- Primeiro teste de criação executado antes do código: 201 esperado, 403 recebido.
- Maven inicial usava JDK incompatível; validações seguintes usam JAVA_HOME de Java
  21 somente no processo. Não houve alteração de `.env` ou credenciais.

## Matriz de rastreabilidade

| Regras / testes planejados | Evidência automatizada |
|---|---|
| RN01, RN02, RN07, RN16; T01, T03, T09, T10 | ProjectsBoundaryTest: criação sem equipe, participações múltiplas, retorno com novo período e vacância; Block02IntegrityTest: períodos imutáveis |
| RN03, RN13; T02, T04, T05, T13, T21 | ProjectsBoundaryTest: gestor do próprio projeto, negativa no alheio, transferência revoga autoridade, sem dados pessoais; Block02RevocationTest: históricos alheios recusados |
| RN04, RN05, RN15; T06, T07 | Transferências simultâneas com vencedor único; índices únicos testados diretamente; liderança e participação mantêm períodos independentes |
| RN06, RN08; T08, T17, T22 | Afastamento/desligamento revogam autoridade; reativação seletiva; write aguardando desligamento; ingresso inativo recusado; integrante inexistente recusado por API e FK |
| RN09, RN10; T14, T15, T16 | ProposalsBoundaryTest: aprovação, rejeição final, conversão única, duas conversões concorrentes e origem preservada |
| RN11, RN12; T11, T12 | Histórico de pausa, retomada, conclusão e reabertura; Block02RulesTest: matriz de transições e decisões |
| RN14; T18 | Falha real de trigger de auditoria: rollback de equipe, estado, transferência e último evento de conversão (incluindo projeto/proposta/históricos/eventos intermediários) |
| T19, T20 | Instalação V1–V3 em PostgreSQL limpo; Block02MigrationTest evolui V2 com dados e históricos imutáveis para V3, valida checksums e preservação |
| T23 | Versões obsoletas, período inválido, paginação, estado/decisão inválidos e justificativas obrigatórias; regras unitárias e limites compartilhados do Bloco 01 |
| T24 | Suíte integral inclui os 58 testes do Bloco 01; única adequação troca `/projects` por `/activities` no teste de rotas ainda não implementadas |

### Rastreabilidade da especificação consolidada

| Requisito | Entrega no Bloco 02 | Evidência principal |
|---|---|---|
| RF-008 | Projeto formal independente da equipe, dados e situação | `ProjectService`, `ProjectsBoundaryTest` |
| RF-009 | Participações com entrada, saída, responsabilidade e períodos preservados | `ProjectDirectory`, `ProjectsBoundaryTest`, `Block02IntegrityTest` |
| RF-014 (projetos) | Consulta do histórico pessoal sem conteúdo gerencial | `GET /api/v1/me/projects/history`, `GET /api/v1/me/projects/leadership/history`, `ProjectsBoundaryTest` |
| RF-029 | Proposta separada, decisão e conversão com origem única | `ProposalService`, `ProposalsBoundaryTest` |
| RF-030 | Estados e liderança contextual com transferência e histórico | `ProjectService`, `Block02RulesTest`, `ProjectsBoundaryTest` |
| CA-08 | Vacância ou gestor indisponível sem excluir o projeto | `ProjectsBoundaryTest`, `Block02RevocationTest` |
| CA-15 | Pausa e retomada explícitas e históricas | `ProjectsBoundaryTest` |
| CA-16 | Autorizações no backend e auditoria transacional | `ProjectAccess`, `Block02IntegrityTest`, `Block02RevocationTest` |

Para RF-010, RF-015, RF-016 e RF-031, o contrato de integração disponível é a
identidade estável do projeto, seu estado e suas consultas autorizadas de
participação/liderança em `projects.application` e `access.application`.
Atividades poderão referenciar o identificador do projeto; andamento e alertas
deverão considerar o estado (especialmente `PAUSADO`) e a liderança efetiva.
Os DTOs, regras de cálculo, critérios de alerta e endpoints desses quatro RFs
serão definidos nos blocos correspondentes; nenhum percentual ou sinalização
automática foi implementado no Bloco 02.

## Revisão e limitações

Revisão independente de código e revisão incremental do histórico pessoal de
liderança não encontraram finding acionável. Lacunas indicadas foram cobertas por
testes de revogação durante espera, reativação seletiva, proposta de outro autor,
históricos de outro projeto e rollback da auditoria final de conversão.

O histórico pessoal de liderança possui rota própria com apenas projectId, título
e datas. Histórico gerencial não é liberado a antigos gestores por terem liderado
no passado. Não foi alterado o login institucional nem o contrato de papéis globais.

Na primeira suíte integral, 108/110 casos passaram. Um teste legado esperava a
negação de `/projects`, agora implementado; passou a usar `/activities`. O outro
teste consultava pg_stat_activity dentro de uma transação longa e não observava
a espera; diagnóstico com pg_locks comprovou lock concedido e outro pendente
para a mesma chave. O teste passou a sincronizar pela espera real em pg_locks.
Esses ajustes são de teste e não alteram o mecanismo de autorização.

Validação automatizada usa PostgreSQL real isolado, principais JWT fictícios para
testes de autorização e testes criptográficos preservados do Bloco 01. O ensaio
local posterior com Microsoft Entra está registrado nas
[evidências de homologação técnica](homologacao-bloco-02-local.md). Nenhuma dessas
verificações equivale ao aceite institucional ou à migração de produção.
O advisory lock global continua sendo um compromisso explícito de baixo volume.

## Resultado da validação local

Em 15/10/2026, após a última alteração de código e testes:

- `mvnw.cmd --batch-mode verify`: BUILD SUCCESS; **111 testes, 0 falhas, 0 erros,
  0 ignorados** (58 anteriores + 53 novos), compilação Java 21 e JAR executável.
- `mvnw.cmd --batch-mode -Dtest=Block02RevocationTest#queuedWriteRechecksAuthorityAfterConcurrentDeactivationCommits,IdentityBoundaryTest#basicAuthenticationAndUnimplementedRoutesStayDenied,Block02IntegrityTest#leadershipAuditFailureRestoresPreviousOpenPeriodAndProjectVersion test`:
  três verificações direcionadas aprovadas antes do verify final.
- `git diff --check`: sem problemas de whitespace.

A primeira execução integrada de 45 cenários já havia passado; o verify final
inclui esses comportamentos e todas as correções posteriores. Na branch de
funcionalidade, apenas documentação e metadados Git foram atualizados após
essa execução. Não foram executados frontend/build Next.js ou homologação
institucional, pois não pertencem ao escopo. Não foi alterado `.env`, credencial
ou banco externo durante a implementação funcional.

## Estado posterior da entrega (16/10/2026)

- O [PR #4](https://github.com/IasminLuise/00_NiteConnect_2026.2_UNIJORGE/pull/4)
  foi integrado à `main` no commit `8458ec39a0051a1a8800c25aa9be8378c44753ee`;
  os checks `verify-api` constam como aprovados.
- A suíte foi reexecutada na branch de infraestrutura após a integração: 111
  testes, sem falhas, erros ou testes ignorados; Flyway V1–V3 foi verificado em
  PostgreSQL 17 pelo Testcontainers. A branch acrescentou suporte de implantação,
  sem alterar os módulos funcionais de projetos e propostas.
- A API integrada foi iniciada em Docker no computador local. O Postman Web com
  Desktop Agent e OAuth PKCE confirmou `GET /api/v1/me` e
  `GET /api/v1/projects` autenticados com HTTP 200. As negativas sem token
  retornaram HTTP 401. As evidências detalhadas ficam no
  [registro local](homologacao-bloco-02-local.md).
- O [staging Cloudflare + Neon](staging-bloco-02.md) está preparado, mas o
  Container não foi implantado porque a contratação do Workers Paid não foi
  autorizada. Aceite institucional formal e migração de produção seguem pendentes.

A matriz T01–T24 acima representa cobertura automatizada com identidades
sintéticas; as duas consultas autenticadas manuais não exercitam todos esses
cenários com contas institucionais distintas.
