# Bloco 06 — API acadêmica

Contrato HTTP: [OpenAPI 3.1 em JSON](bloco-06-api.json). O formato JSON evita adicionar dependência de parsing YAML e mantém o contrato consumível por ferramentas OpenAPI. São 40 operações implementadas; as rotas não listadas permanecem negadas.

## Acesso e limites

JWT institucional autenticado não substitui vínculo ativo e autorização por operação. O backend consulta os papéis persistidos. Coordenador registra políticas, notas, decisões e homologações; gestor geral prepara referências e calcula resultados com notas já atribuídas, sem poder de pontuação ou homologação. Liderança de projeto não amplia acesso acadêmico.

Todas as coleções são paginadas: `page=0`, `size=20`, tamanho máximo 100 e ordenação estável. A resposta contém `items`, `page`, `size`, `total`. Identificadores são UUIDs, instantes usam ISO-8601, e valores de nota/peso são números decimais. Mutação de registro existente exige `expectedVersion`; operação crítica exige também `confirmed=true` e motivo.

| Grupo | Rotas principais | Autorização |
|---|---|---|
| Períodos e políticas | `/api/v1/academic/terms`, `/terms/{id}/policies`, `/api/v1/academic/policies/{id}/confirmation` | Coordenador escreve; integrantes ativos consultam períodos e políticas confirmadas; rascunhos só para coordenador |
| Adesões | `/api/v1/me/academic/enrollments`, `/api/v1/academic/enrollments`, `/{id}/decisions` | Titular solicita/revisa; coordenador decide; administração acompanha |
| Planos | `/api/v1/me/academic/enrollments/{id}/plans`, `/api/v1/me/academic/plans/{id}/submission`, `/api/v1/academic/plans/{id}/decisions` | Titular elabora/submete; coordenador aprova/corrige |
| Evidências | `/api/v1/me/academic/enrollments/{id}/evidence`, `/api/v1/academic/enrollments/{id}/evidence`, `/presentations`, `/api/v1/academic/evidence/{id}/comparison` | Dupla autorização acadêmica e na origem; atestação de apresentação exclusiva da coordenação |
| Notas e cálculos | `/api/v1/academic/enrollments/{id}/assessments`, `/calculations` | Coordenador pontua; administração pode calcular sem mudar as notas |
| Revisão | `/api/v1/me/academic/enrollments/{id}/reviews`, `/api/v1/academic/enrollments/{id}/reviews`, `/api/v1/academic/reviews/{id}/decisions` | Titular solicita no rito/prazo configurado; coordenador inicia correção e decide |
| Homologação | `/api/v1/academic/calculations/{id}/homologations`, `/api/v1/academic/homologations/{id}/reopen` | Somente coordenador, com confirmação, motivo e versão |
| Encaminhamento | `/api/v1/academic/homologations/{id}/forwardings` | Coordenador registra comprovante conforme procedimento confirmado; administração consulta |
| Auditoria | `/api/v1/academic/enrollments/{id}/history` | Coordenador consulta valores anteriores/posteriores; gestor geral recebe metadados sem motivos ou conteúdo; trilha acadêmica não é copiada à auditoria administrativa genérica |

As rotas abreviadas na tabela são apenas agrupamento de leitura; os caminhos completos e métodos estão no OpenAPI. Consultas próprias de planos, evidências, avaliações, cálculos e revisões usam `/api/v1/me/academic/enrollments/{id}/...` conforme o contrato.

Consultas e cálculo pelo gestor geral retornam projeções de preparação: avaliações sem nota, motivo ou referências privadas; cálculos com resultado agregado e metadados, sem política/insumos completos. Listagem de evidências por não coordenadores omite `snapshot.attributes`; o conteúdo atual deve ser consultado no módulo de origem com sua autorização contextual. A captura continua exigindo autorização na origem. Motivos de revisão iniciada pela coordenação ficam ocultos nas consultas do integrante.

`DecideEnrollment.feedback` é obrigatório para `REQUEST_CORRECTION` e `REJECT`; `DecidePlan.feedback` é obrigatório para `REQUEST_CORRECTION`. Esse texto (até 1000 caracteres) é destinado ao integrante e aparece em `Enrollment.feedback`/`Plan.feedback`. `reason` continua sendo a justificativa administrativa privada; não deve ser usado como substituto de `feedback`.

## Fluxo

1. Coordenador cria período com intervalo explícito e fuso válido.
2. Cria uma versão de política em preparação. Confirmação exige pesos somando exatamente 100%, escala, arredondamento, disciplinas/cursos elegíveis, limite, janela e referência institucional. Não há valores padrão de produção.
3. Integrante solicita adesão opcional, declarando curso/códigos de disciplina conforme a política. O curso não é inferido da identidade Microsoft nem acrescentado silenciosamente ao cadastro B01.
4. Coordenador coloca a adesão em análise e decide a pertinência/elegibilidade; pode solicitar correção.
5. Plano Individual tem revisões e aprovação próprias. Sua obrigatoriedade depende da configuração institucional; não cria bloqueio no Diário de Bordo ainda ausente.
6. Evidências preservam ID, autoria, contexto temporal, versão da origem, validação quando existente, instante de captura e escopo autorizado. Fonte `ACTIVITY` identifica a evidência B03, não apenas a atividade; `ATTENDANCE` identifica um segmento de presença associado a um evento B04 e não inclui horas ou estado de revisão de horas. `PRESENTATION` exige atestação humana da coordenação.
7. Coordenador atribui nota justificada para cada pilar, com evidências pertinentes. O cálculo preserva a política, as avaliações, os retratos das evidências, a versão do algoritmo e o instante de corte.
8. Homologação exige cálculo atual, evidências atuais, todos os pilares e ausência de revisão pendente. A captura usa o lock transacional administrativo compartilhado com os módulos operacionais; não mistura versões de gravações normais concorrentes.
9. Pedido de revisão não apaga a homologação anterior. Aceite/reabertura leva a nova revisão; nova homologação aponta a antecessora. Registro de encaminhamento referencia a homologação exata, sem ser reapontado após correção.

## Condições de ativação

- Pesos 30/20/50 aparecem apenas em testes sintéticos, nunca em seed, fallback ou configuração de produto.
- Ausência de avaliação não significa zero. Pilar ausente ou fonte indisponível impede resultado completo/homologável.
- Não existe implementação de `DocumentEvidenceReader` no produto. Assim, não há evidência documental oficial disponível para completar o pilar documental; não há redução/redistribuição de peso como atalho.
- O teste positivo de homologação usa provedor documental exclusivamente no classpath de teste. Não comprova integração documental real, aceite institucional ou prontidão de produção.
- Notas/cálculos próprios só são divulgados com homologação e `discloseResults=true` na política confirmada. A consulta retorna exclusivamente cálculos homologados e as revisões das avaliações efetivamente utilizadas; homologar uma versão não publica rascunhos descartados. O resultado agregado não inclui os insumos privados. A homologação anterior continua consultável enquanto outra revisão está em andamento.
- `reviewWindowSeconds` ausente impede a solicitação de revisão que depende do prazo institucional. `forwardingProcedureReference` ausente impede marcar encaminhamento.
- Não há endpoint de confirmação de lançamento universitário, emissão de certificado, assinatura, publicação no Portal do Aluno ou envio real de notificações. Os records de integração futura contêm referências e não ativam esses serviços.

## Erros e histórico

Erros usam `application/problem+json`: 400 para dados inválidos, 401 para autenticação ausente/inválida, 403 para autorização negada, 404 para origem/recurso não encontrado e 409 para versão obsoleta, transição inválida, parâmetro institucional ausente ou integração indisponível. Com Entra desabilitado, mesmo JWT principal simulado é negado pela segurança HTTP.

Uma origem retirada/inacessível provoca recusa explícita na releitura; não altera a captura nem produz zero. Comparação de revisão diferente retorna `changed=true`. Nenhuma origem atual é usada para reconstruir silenciosamente cálculo histórico.

V8 cria tabelas acadêmicas, vínculos relacionais entre avaliação e evidências da mesma adesão, proteção de histórico e políticas confirmadas imutáveis. V9 acrescenta o retorno público de correção/indeferimento e índices de consulta do histórico publicado. V1–V8 permanecem intactas nesta correção posterior à revisão. Desativação cadastral restringe acesso, sem apagar as informações acadêmicas.
