# Bloco 03 — atividades, responsabilidades, prazos e evidências

## 1. Situação e fontes

**B03.1 aprovada pelo responsável; implementação integrada e validada localmente.**
Data: 16/10/2026. Branch: `feat/bloco-03-atividades-evidencias`.
Base verificada: `93aca0ff` (merge do PR #6). O conteúdo da árvore dessa base
coincide com o checkout anterior, `8f8aa609`. A atualização foi obtida por HTTPS
após falha de autenticação SSH, sem alterar configuração de credenciais.

Fontes: planejamento fornecido na conversa e
`docs/product/NITE_Connect_Especificacao_Funcional_Consolidada_v1.0.pdf`,
páginas 4, 6–9, 11–12 e 14. O planejamento fornecido tem 13 seções numeradas,
parecer final e sete etapas. A numeração deste documento organiza o refinamento,
sem atribuir ao documento original seções que ele não contém.

Os 115 testes anteriores são uma referência informada pelo planejamento e pelos
registros do Bloco 02, não um resultado de execução desta tarefa.

## 2. Objetivo e limites

Entregar uma implementação integrada que permita planejar atividades, atribuir
um executor e colaboradores, acompanhar execução e prazo, registrar evidências
e preservar contribuições. Projeto é opcional. A identidade vem do Entra;
autorização depende do integrante ATIVO e do contexto no backend.

Não incluir horas, presença, Diário de Bordo, ciclos, validação formal de entregas,
progresso percentual, AV3, alertas parametrizados ou documentos oficiais.
Conclusão não valida horas, entregas ou notas. Não presumir normas institucionais.

## 3. Base inspecionada e impactos necessários

- Java 21, Spring Boot 4.1.1, JDBC e PostgreSQL; Flyway V1–V3 existentes.
- `AccessService.requireActive/isAdministrator`, `ProjectAccess.requireRead/requireManage`
  e os contratos `MemberDirectory`/`ProjectDirectory` são as fronteiras existentes.
- `AdministrativeTransactions.lock()` adquire um advisory lock transacional comum
  às alterações de acesso, integrantes e projetos. Autorização de escrita é
  reavaliada depois da aquisição do lock.
- `MemberDirectory.candidates` busca ATIVOS **sem** participação vigente no projeto.
  Não serve para escolher executores de atividades vinculadas. Criar consulta
  explícita para participantes elegíveis, preservando a rota do Bloco 02.
- `AuditTrail.recordEntity` participa obrigatoriamente da transação chamadora.
  A constraint `audit_entity_check` da V3 aceita apenas MEMBER/PROJECT/PROPOSAL;
  ampliar pela V4 para ACTIVITY/EVIDENCE, sem editar V3 ou reescrever eventos.
- `SecurityConfiguration` usa allowlist de método/rota e deny-all no restante.
  Acrescentar somente rotas implementadas e testadas; não liberar um prefixo inteiro.
- `PageResult` limita páginas a 100 itens. Reutilizar formato e validação.
- O staging tem preparação para Cloudflare Containers. Disco local de contêiner
  não deve ser considerado armazenamento persistente de anexos institucionais.

## 4. Decisões aprovadas pelo responsável

O responsável aprovou explicitamente D01–D12 na conversa e solicitou seu registro
nos documentos pós-implementação. Esta seção é o registro das decisões funcionais
e técnicas aprovadas para o Bloco 03; elas não constituem novas normas universitárias.
Os limites de anexos e implantação são parte expressa desse aceite.

| ID | Decisão aprovada | Consequência / limite |
|---|---|---|
| D01 | Integrante ATIVO pode criar atividade independente para si, sem colaboradores. | Distribuir trabalho independente para terceiros ou adicionar colaboradores exige administração global. |
| D02 | O criador pode editar título, descrição e prazo, iniciar, concluir, cancelar e reabrir sua atividade independente enquanto continuar sendo seu executor e ela não tiver terceiros no histórico de atribuições. | Quando houver delegação, o criador conserva leitura, mas gestão fica com administração; executor delegado só inicia/conclui e registra evidências. Evita autoridade permanente do autor sobre atividade transferida. |
| D03 | Estados PLANEJADA, EM_EXECUCAO, CONCLUIDA e CANCELADA. Sem exclusão física. | CANCELADA resolve cadastro por engano sem confundir com conclusão; é acréscimo à proposta inicial de três estados. |
| D04 | Executor principal inicia e conclui. Colaboradores registram evidências, sem concluir a tarefa de outra pessoa. Gestor contextual/administração também pode iniciar/concluir; só gestão ou dono pessoal D02 cancela/reabre. | Permissão gerencial explícita, mesmo sem atribuição de execução. Reabertura sempre justificada. |
| D05 | Em projeto PLANEJAMENTO: planejar/editar/atribuir/cancelar, sem iniciar execução. ATIVO: operação normal. PAUSADO: leitura e manutenção gerencial, sem nova execução, conclusão ou evidência. CONCLUIDO: somente leitura de atividades; retomar o projeto antes de novas mutações. | Não mudar regras das rotas de projetos do Bloco 02. Esta restrição incide somente sobre atividades. Administração não contorna silenciosamente o estado do projeto. |
| D06 | Fixar `projectId` na criação neste bloco. | Transferência entre contextos altera quem pode ler todo o histórico; adiar rota de transferência até haver política de divulgação das evidências. A seção 10 original sugeriu operação administrativa, sem definir essa política. |
| D07 | Prazo como LocalDate, fuso configurável com valor inicial America/Bahia. Sem tolerância ou dias úteis presumidos. | Data de hoje ainda não está vencida. Mudança de prazo exige motivo e evento anterior/posterior. |
| D08 | Evidência é imutável; correção cria nova evidência com `correctsEvidenceId` da mesma atividade. Autor vigente pode retirar a própria evidência; gestão pode retirar qualquer evidência do contexto. | Retirada é lógica e justificada, preserva autoria e metadados. Conteúdo retirado fica restrito à gestão; não há edição ou restauração silenciosa. |
| D09 | Anexos privados com adaptador de filesystem somente para desenvolvimento/testes; nenhuma contratação ou configuração de serviço externo nesta entrega. | Integração institucional permanece pendente até aprovação de armazenamento persistente. Recomendação futura: objeto privado; não expor disco efêmero do staging como solução institucional. |
| D10 | Primeira allowlist de anexos: TXT UTF-8, PNG e JPEG, até 10 MiB e 20 milhões de pixels por imagem. | Limites técnicos aprovados para este bloco. PDF/DOCX, SVG, HTML, ZIP e executáveis ficam fora desta primeira allowlist; suportar PDF/DOCX exige validação estrutural e avaliação de processamento seguro. Limitação explicitamente aceita no conjunto D01–D12. |
| D11 | Ex-participante ATIVO consulta somente suas atribuições e contribuições históricas por rota pessoal, com projeção mínima. | Não recupera acesso ao detalhe completo, aos anexos de terceiros ou ao histórico gerencial após perder o vínculo. AFASTADO/DESLIGADO não tem acesso operacional. |
| D12 | Mutação exige `expectedVersion` da atividade, motivo e auditoria; operações críticas também exigem `confirmed=true`. | Confirmar cancelamento, reabertura, substituição de executor e retirada de evidência. Reusar o lock administrativo para escritas inicialmente, com operações de arquivo fora do lock. |

Recomendação de execução: manter um módulo `activities` com evidências coesas e
um contrato interno pequeno de armazenamento. Separar módulos completos de
atividades e evidências agora acrescentaria coordenação transacional sem consumidor
independente demonstrado. Provedor remoto agora exigiria credenciais e uma decisão
de infraestrutura que ainda não foi fornecida. Nenhuma dependência foi adicionada; os formatos de D10 usam capacidades existentes do Java.

## 5. Matriz de acesso aprovada

Sempre exigir integrante ATIVO. Administração = GESTOR_GERAL ou COORDENADOR.
Liderança é contextual e vigente. Atribuição histórica não substitui participação
vigente de projeto. Criador de atividade de projeto não ganha leitura permanente.

| Operação | Atividade de projeto | Atividade independente |
|---|---|---|
| Ler detalhe/evidências disponíveis | Participante vigente ou administração | Criador, atribuído vigente ou administração |
| Criar | Gestor contextual ou administração | D01; administração pode designar outro integrante ATIVO |
| Editar/atribuir | Gestor contextual ou administração | Administração; dono pessoal D02 edita sem delegar |
| Iniciar/concluir | Executor vigente e elegível; gestor contextual/administração | Executor vigente; administração; conforme D04 |
| Cancelar/reabrir | Gestor contextual ou administração | Administração ou dono pessoal D02 |
| Registrar evidência | Executor/colaborador vigente e elegível; gestor contextual/administração | Executor/colaborador vigente ou administração |
| Ler histórico de gestão/atribuições | Gestor contextual ou administração | Administração ou dono pessoal D02 |
| Retirar evidência | Autor se ainda elegível, gestor contextual ou administração | Autor se ainda atribuído, dono pessoal D02 ou administração |
| Consultar histórico pessoal limitado | Próprios períodos/contribuições, inclusive após saída | Próprios períodos/contribuições |

Gestão respeita D05. Read não significa write. UUID de outro contexto retorna 404;
quem pode ler mas não operar recebe 403. Conflitos de versão/estado retornam 409.
Nomes são consultados em lote por `MemberDirectory.names`; não expor e-mail,
situação administrativa ou motivos reservados de colegas.

## 6. Ciclo de vida e atribuições

Criação sempre PLANEJADA, com um executor vigente e sem `completedAt`.

| Origem | Destino | Regra adicional |
|---|---|---|
| PLANEJADA | EM_EXECUCAO | D04 e contexto liberado para execução |
| EM_EXECUCAO | CONCLUIDA | D04; gravar completedAt pelo servidor |
| PLANEJADA / EM_EXECUCAO | CANCELADA | Gestão/dono pessoal; confirmação e motivo |
| CONCLUIDA / CANCELADA | EM_EXECUCAO | Gestão/dono pessoal; confirmação e motivo; limpar conclusão vigente |

Repetição de estado e demais transições: 409. Eventos guardam conclusões anteriores.
CONCLUIDA/CANCELADA permitem somente leitura, retirada gerencial justificada de
evidência ou reabertura; não editar escopo, prazo, atribuições ou anexar contribuição
antes de reabrir. Em CONCLUIDO, D05 prevalece também para retirada/reabertura.

O executor é obrigatório, distinto dos colaboradores e ATIVO na atribuição.
Em projeto, exigir participação vigente na atribuição **e na escrita posterior**.
Desligamento/saída preserva períodos; apresentar disponibilidade operacional
separada da existência da atribuição. Não escolher substituto automaticamente.
Substituição encerra período e abre outro na mesma transação; se o alvo era
colaborador, encerrar esse período antes de iniciar o de executor, com evento.
Trocar pelo executor atual ou repetir colaborador vigente retorna 409.

## 7. Persistência e invariantes

Nova migração `V4__activities_assignments_evidence.sql`, aditiva:

- `activities`: UUID, title(200), description(10000), project_id nullable,
  status, due_on date nullable, created_by, version >= 0 e instantes do servidor.
  FK sem cascade delete; CHECK de completed_at coerente com CONCLUIDA.
- `activity_assignments`: atividade, integrante, papel EXECUTOR/COLLABORATOR,
  início/autor/motivo e encerramento/autor/motivo. Índice único parcial por
  atividade/integrante vigente e por atividade para executor vigente.
  Trigger permite apenas encerrar um período uma vez. Constraint trigger
  diferida verifica exatamente um executor no commit, inclusive em SQL direto.
- `activity_events`: atividade, ação, ator, motivo, instante, versão resultante,
  valores anteriores/posteriores estruturados. Append-only; índice por atividade/data/id.
- `activity_evidence`: atividade, autor, atribuição nullable para evidência
  gerencial, tipo TEXT/LINK/TECHNICAL/FILE/IMAGE, conteúdo/referência,
  correção opcional da mesma atividade, criação e retirada lógica. Preservar
  campos originais e permitir somente a retirada única. FK composta garante
  que atribuição/autoria/atividade e evidência corrigida pertencem ao contexto.
- `evidence_objects`: chave opaca gerada no servidor, upload vinculado a
  atividade/ator, nome de exibição, tipo validado, tamanho, SHA-256 e estado.
  Não guardar caminhos fornecidos pelo cliente nem URLs públicas.
- Ampliar `audit_entity_check` para ACTIVITY/EVIDENCE sem UPDATE nos registros
  antigos. Falha na auditoria reverte a mutação inteira.

Índices para projeto/estado/prazo, atribuições vigentes por integrante e históricos
por atividade/data/id. Planejar filtros com SQL parametrizado; não permitir
ordenação ou identificadores SQL arbitrários. Nenhuma tabela de outro módulo
será modificada por SQL do módulo `activities`.

## 8. Concorrência e fronteiras de módulos

Cada escrita: iniciar transação; adquirir `AdministrativeTransactions.lock()`;
resolver integrante e contexto; revalidar autoridade e elegibilidade; conferir
`expectedVersion`; persistir alteração/eventos/auditoria; commit. Incrementar
somente a versão da atividade, uma vez por comando concluído, inclusive evidência.
Não incrementar versão de projeto por operação de atividade.

O lock comum tem custo de serialização; é uma escolha inicial para preservar a
garantia de revogação já testada. Não realizar upload/download ou decodificação
de imagem enquanto o lock estiver adquirido. Não ampliar o lock às consultas.
Contenção é limitação a medir, não justificativa para inventar infraestrutura.

`ProjectDirectory` fornece contexto/participações; `MemberDirectory` fornece
nomes/elegibilidade/pesquisa restrita. A nova pesquisa deve representar candidatos
ATIVOS da equipe para atribuição; independentes somente para administração.
Integrante criando para si usa o próprio identificador, sem busca global.

## 9. Prazos e consultas

Usar `Clock` injetável e ZoneId configurável. `deadlinePassed` = dueOn anterior
ao dia atual; `overdue` = deadlinePassed e status PLANEJADA/EM_EXECUCAO.
CONCLUIDA/CANCELADA não geram pendência, mesmo com data ultrapassada.
`attentionRequired` = overdue e (sem projeto ou projeto ATIVO).
Campo operacional, sem notificação ou julgamento de produtividade.

Em PAUSADO/PLANEJAMENTO/CONCLUIDO, mostrar atraso objetivo sem sinalização
de apoio equivalente à de ATIVO. Não mover prazo ou status automaticamente.
Uma resposta usa a mesma data de referência para todos os itens e para o filtro
overdue, sem diferença entre SQL de contagem e itens.

Listas paginadas, filtros por status, projeto, executor, dueFrom/dueTo e overdue;
validar intervalo invertido. A visibilidade aplica-se antes de contar/paginar.
Filtros nunca ampliam acesso. Ordenação fixa por criação desc/id.

## 10. Evidências e arquivos privados

Texto e registro técnico: conteúdo obrigatório até 10000 caracteres. Link: HTTP(S)
absoluto até 2048 caracteres, sem credenciais ou controles. O servidor não busca,
pré-visualiza ou segue a URL. Correção referencia somente evidência existente da
mesma atividade; preservar original e autoria. Não copiar conteúdo em logs/auditoria.

Para D09/D10, implementar uma porta `EvidenceObjectStore` com staging, promoção,
leitura e descarte idempotente por chave gerada. Raiz privada fora de diretórios
públicos, configurada sem modificar `.env`. Adaptador local opt-in; sem configuração,
rotas de arquivos autenticadas retornam 503 após verificar o contexto.

Upload em duas fases: preparar autorização e recibo PENDING com versão/motivo;
validar/gravar bytes fora do lock; finalizar em nova transação com revalidação
completa e expectedVersion original. Somente após objeto privado íntegro e commit
dos metadados/evento/auditoria a evidência fica AVAILABLE. Conflito/revogação/falha
não gera evidência disponível; limpar staging ou manter recibo FAILED para recuperação.

Estados PENDING/AVAILABLE/FAILED; recibo FAILED não é evidência válida. Queda após
gravar objeto e antes do commit deixa objeto órfão privado, nunca disponível por
API. Reconciliação explícita identifica recibos incompletos e remove objetos sem
evidência confirmada, com período de segurança configurável e modo dry-run.
Não criar job distribuído; validar o fluxo de recuperação no adaptador de testes.

TXT: UTF-8 estrito, sem NUL, sem aceitar HTML como formato de evidência executável.
PNG/JPEG: verificar assinatura, dimensões antes da decodificação e leitura completa;
recusar arquivo truncado, extensão/MIME incompatíveis e limites excedidos.
Não alegar proteção antivírus só por assinatura/decodificação. Revisão de malware
e política institucional de formatos permanecem requisitos de implantação.

Download passa pelo backend e verifica atividade + evidenceId, estado e retirada;
retornar attachment, nome sanitizado, `nosniff` e `Cache-Control: no-store`.
Nunca retornar chave/caminho interno ou URL pública. Falta de arquivo/integridade
gera erro controlado, sem fornecer bytes parciais como evidência íntegra.

## 11. Histórias e aceitação

- Como integrante, crio minha atividade independente e acompanho execução sem
  projeto fictício (RF-004/010, CA-09; D01/D02).
- Como gestor contextual, atribuo trabalho somente a integrantes elegíveis da
  minha equipe e preservo atribuições substituídas (RF-032, CA-16).
- Como executor, inicio/concluo explicitamente minha atividade; como colaborador,
  acrescento evidências sem concluir o trabalho de outro executor (RF-005/033).
- Como gestor, altero prazo ou reabro com motivo, versão e registro anterior
  preservado (RF-006/007, CA-16).
- Como ex-participante ATIVO, consulto minhas contribuições sem obter registros
  reservados de colegas (RF-014, RF-021/052).
- Como administração, acompanho tarefas/prazos sem inferir horas ou nota e sem
  tratar projeto pausado como irregular pelos gatilhos de ativo (RF-015, CA-15).

## 12. Sete etapas da implementação integrada

| Etapa | Entrega e validação prevista |
|---|---|
| B03.1 | D01–D12 aprovadas na conversa; decisões registradas nesta seção 4 e no contrato HTTP. |
| B03.2 | V4, JDBC, constraints/índices/históricos; Testcontainers PostgreSQL 17, banco novo e evolução de V3 com dados. |
| B03.3 | Domínio de estados, cadastro/edição/leitura/cancelamento/reabertura; testes unitários e MockMvc. |
| B03.4 | Atribuições, candidatos, isolamento e histórico pessoal; testar substituição concorrente e revogação. |
| B03.5 | Clock, prazos/filtros/atrasos; testar virada de data e estados de projeto. |
| B03.6 | Evidência imutável, correção/retirada e adaptador privado aprovado; testar falhas entre objeto/banco e recuperação. |
| B03.7 | Regressão integral, revisão, documentação de resultados, PR e CI. Sem deploy/merge automático. |

Uma branch, incrementos integrados. Plano de execução e registro em
`bloco-03-execution.md`. Não dividir o bloco em
produtos independentes ou declarar conclusão parcial como conclusão do bloco.

## 13. Plano de validação rastreável

| Cenários do planejamento | Cobertura planejada |
|---|---|
| T01–T07 | Criação com/sem projeto, liderança própria/alheia, executor inexistente/inativo/fora da equipe. |
| T08–T12 | Duplicidade, papel exclusivo, transferência atômica, concorrência e saída preservando evidências. |
| T13–T16 | Transições, confirmação crítica, reabertura, prazo anterior/posterior e motivo. |
| T17–T19 | Sem prazo, hoje/ontem, fuso configurado, terminal e projetos pausados/concluídos. |
| T20–T25 | Texto/link/técnico, arquivo sem permissão, troca de evidenceId/contexto, falha/recuperação e correção imutável. |
| T26–T28 | Revogação/saída concorrente, expectedVersion antigo e rollback em falha de auditoria. |
| T29 | Banco limpo e V3→V4 preservando dados, constraints e imutabilidade anteriores. |
| T30 | `mvnw.cmd --batch-mode verify`, todos os testes antigos e novos, PostgreSQL 17 real via Testcontainers. |

Complementar com seleção de candidatos sem vazamento, visibilidade antes da
paginação/contagem, histórico mínimo de ex-participante, raiz privada/path traversal,
limites/tipos/conteúdo, chamada de download após retirada e finalização de upload
após revogação. Cada comportamento novo exige teste de sucesso e de falha relevante.

Começar por testes específicos. Executar regressão ampla após V4, configuração
compartilhada e integração final; não repetir toda a suíte a cada ajuste local.
Se V4 elevar o total de migrações, atualizar expectativas fixas em testes existentes
sem remover suas asserções de preservação. Não confundir essa adequação com remover
regressões do Bloco 02.

## 14. Evidências e validação

Executado: leitura do planejamento, extração textual do PDF via `pypdf`, inspeção
de instruções, tipos, permissões, serviços, auditoria, migrações e testes relacionados;
verificação de Git limpo; fetch HTTPS da main e criação da branch a partir de 93aca0f.

Validação documental executada com Python: UTF-8, newline final, ausência de
espaços finais e marcadores de conflito, 15 seções deste refinamento, 12 decisões
e sete etapas presentes; passou nos dois arquivos. `git diff --check` não reportou
problemas nos arquivos rastreados; os dois documentos novos ainda não rastreados
foram verificados pelo script. `git status --short` confirmou somente esses dois
arquivos novos, sem alteração de código ou migrações.

A etapa inicial alterou somente documentos. A implementação subsequente acrescentou
15 classes Java no módulo `activities`, V4, contratos e testes. Nenhuma dependência,
credencial, `.env`, V1/V2/V3 ou configuração de produção foi alterada.

### Validações executadas

Comandos na pasta `apps/api` (PowerShell):

| Comando / rodada | Resultado efetivamente observado |
|---|---|
| `mvnw.cmd --batch-mode -Dtest=ActivitiesBoundaryTest,ActivityUploadsTest,Block03*,ActivityDeadlinesTest test` | 23 testes aprovados na primeira integração, incluindo PostgreSQL 17 e upgrade V3→V4. |
| `mvnw.cmd --batch-mode -Dtest=ActivityAuthorizationTest,ActivityUploadsTest test` | 16 testes aprovados na rodada de arquivos/permissões, antes dos últimos refinamentos. |
| Testes direcionados dos achados da revisão e bloqueio de inativos | Quatro falhas reproduzidas antes das correções; quatro testes aprovados depois. Logs locais `target/bloco03-review-red.log` e `target/bloco03-review-green.log`. |
| `mvnw.cmd --batch-mode verify` | 157 testes aprovados (115 anteriores e 42 novos), sem falhas/erros/ignorados; JAR gerado. Log `target/bloco03-verify-final.log`. |
| `mvnw.cmd --batch-mode -Dtest=ActivitiesBoundaryTest,ActivityDeadlinesTest,ActivityAuthorizationTest verify` | 22 testes aprovados e JAR gerado **após o último ajuste** de rejeição de `projectId: null`. Log `target/bloco03-immutable-null-green.log`. |
| `git diff --check` e validação documental UTF-8/whitespace | Sem problemas encontrados. |

A regressão integral local precedeu somente o refinamento do campo proibido
`projectId` na edição. Depois dele foram repetidas as três classes afetadas (22
testes), preservando os resultados dos demais módulos. A CI executa novamente a
suíte integral no commit do PR; o resultado remoto deve ser consultado no próprio PR.
Não executar bateria frontend ou integração institucional: não houve frontend,
implantação ou armazenamento institucional neste escopo.

A primeira regressão ampla expôs uma expectativa obsoleta no teste do Bloco 01,
que ainda tratava `/activities` como não implementada. Foi atualizada para aceitar
a rota autorizada e manter o bloqueio de uma operação desconhecida. O teste de
migração do Bloco 02 foi fixado em V2→V3; a nova classe cobre explicitamente V3→V4,
sem remover as asserções de preservação anteriores. Não há falha preexistente
omitida no resultado final local.

### Revisão independente

Revisão estática integrada encontrou três pontos corrigidos com regressões:
recibos PENDING sem arquivo fora da recuperação, snapshots textuais ambíguos e
histórico sem nomes legíveis de ex-atribuídos. A segunda leitura independente não
encontrou defeitos concretos remanescentes nesses pontos. Também foi corrigida a
ordem da autorização, negando inativos antes de revelar a existência do recurso.
A revisão não substitui os testes ou o aceite do PR.

## 15. Pendências reais de entrega e implantação

D01–D12 estão aprovadas, inclusive cancelamento, permissões pessoais, projeto fixo
na criação, formatos iniciais e armazenamento somente de desenvolvimento/testes.
Código, testes e revisão local estão concluídos; publicação do PR e acompanhamento
da CI são a etapa de entrega. Nenhum merge ou deploy faz parte desta execução.

Para anexos institucionais: autorizar armazenamento persistente, configurar
credenciais fora do Git e validar disponibilidade, recuperação, política de formatos
e tratamento de arquivos maliciosos. PDF/DOCX e provedor institucional estão fora
do escopo inicial aprovado. Homologação institucional permanece distinta da técnica.
