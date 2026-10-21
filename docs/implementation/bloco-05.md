# NITE CONNECT — Plano completo do Bloco 05

**Gestão Documental, Diário de Bordo e Prova de Conceito DOCX / Collabora Online CODE / LibreOffice**
**Estado:** plano revisado em 17/10/2026; execução autorizada somente da POC B05.0; produto e V6 não implementados
**Data de referência:** 16/10/2026
**Base técnica informada e confirmada:** Blocos 01–04 integrados na `main`, commit `e64340835d9a5a8c013cfee8c0ffac49c5054ff0`; 185 testes aprovados na CI após o Bloco 04.
**Instruções do repositório:** `AGENTS.md`, `apps/api/AGENTS.md`; preservar regras e contratos existentes.

> **Fonte conferida:** PDF consolidado local, 16 páginas, extraído com pypdf e páginas relevantes inspecionadas visualmente em 17/10/2026. RF-034–039 e RF-052 (p. 7), permissões (p. 4 e 9), F03/F06 (p. 10), RNF-018/019 (p. 12), POC (p. 13), CA-11/12 (p. 14), Q20/21/22 (p. 15). A revisão foi aceita na conversa; escolhas de produto ainda marcadas como propostas não foram aprovadas por esse aceite.

---

## 1. Decisão executiva e objetivos

### 1.1 Estratégia escolhida para avaliação

1. **Candidato principal:** Collabora Online Development Edition (CODE) para **edição DOCX no navegador e conversão DOCX→PDF no mesmo motor**.
2. **Comparador técnico:** LibreOffice headless para conversão independente, medição de recursos e análise da fidelidade.
3. **Sem dupla infraestrutura por presunção:** somente manter um conversor LibreOffice separado no produto se uma necessidade comprovada justificar manutenção, segurança, desempenho ou melhor saída funcional.
4. **Sem integração prematura:** executar primeiro uma *spike* B05.0 isolada, sem Flyway V6 nem alterações de contratos B01–B04.
5. **Gate formal:** somente iniciar a integração WOPI definitiva e definir infraestrutura persistente depois da avaliação técnica, funcional e operacional.

### 1.2 Resultados esperados do Bloco 05

- Documentos e modelos institucionais organizados e acessíveis segundo o contexto e a autorização.
- Criação de documentos a partir de modelos, upload controlado e metadados consistentes.
- Consulta e download de versões com preservação de autoria e rastreabilidade.
- Visualização e edição de DOCX no navegador, caso a prova de conceito seja aprovada.
- Exportação PDF previsível, associada à versão exata de origem.
- Diário de Bordo vinculado à trajetória de integrantes, projetos e atividades, com histórico de retificações.
- Auditoria, isolamento, controle de concorrência e persistência segura.

### 1.3 Limites explícitos

- Concluir uma atividade B03 **não** cria nem aprova automaticamente uma entrada de Diário.
- Declarar uma atividade no Diário **não** cria horas validadas em B04.
- Documento salvo **não** se transforma automaticamente em entrega, publicação oficial, avaliação acadêmica ou nota.
- PDF será inicialmente formato de visualização/exportação; **edição livre de PDFs não está pressuposta**.
- DOCX é o formato prioritário de edição a testar; outros formatos (ODT, XLSX, PPTX etc.) não entram sem decisão funcional.
- Sem gerenciador genérico de arquivos para toda a universidade, workflows de aprovação acadêmica ou edição anônima irrestrita.
- Sem usar o filesystem privado **local de desenvolvimento** do Bloco 03 como armazenamento institucional definitivo.
- Sem contratação, mudança de domínio, credenciais reais, deploy institucional ou alteração de produção como consequência automática deste planejamento.

---

## 2. Referências de requisitos e verificação obrigatória

| Identificador a conferir na versão consolidada | Intenção recuperada do planejamento | Entrega candidata |
|---|---|---|
| RF-035 | Modelos/documentos padronizados | Biblioteca de modelos versionados; gestão geral/coordenação (RF-035, p. 7) |
| RF-036 | Documentos organizados por contexto e acesso | Catálogo por pessoa/projeto/evento/demanda (p. 7); atividade como referência adicional proposta |
| RF-037 | Editar documentos no navegador | Integração WOPI + Collabora, mediante gate B05.0 |
| RF-038 | Visualização e exportação | DOCX/PDF vinculados a versões |
| RF-039 | Metadados e controle de edição/acesso | Última abertura/edição, autoria e data sob autorização (RF-039, p. 7) |
| RF-034 / Q20 (p. 7 e 15) | Registro narrativo e histórico das contribuições | Entradas estruturadas com referências explícitas e revisões |
| Q22 (p. 15) | Referência técnica à edição integrada | Avaliação Collabora CODE, sem decisão de produto pré-homologada |

**Verificação textual concluída nesta revisão:** RF-034–039, RF-052, F03/F06, CA-11/12, RNF-018/019 e Q20/21/22 conferidos. Na B05.1, fechar somente as decisões complementares ainda abertas e seus contratos. Se o documento exigir um Diário de Bordo editável como documento Word, não converter silenciosamente esse requisito em formulário estruturado; decidir expressamente a representação principal e as exportações.

---

## 3. B05.0 — Prova de conceito obrigatória

### 3.1 Perguntas a responder

- O Collabora CODE abre, edita, salva, reabre e exporta os DOCX representativos do NITE com qualidade aceitável?
- O editor mantém imagens, tabelas, estilos, sumários, cabeçalhos, rodapés, quebras, campos e tipografia sem alterações inaceitáveis?
- O fluxo WOPI funciona com um host próprio; o documento é efetivamente salvo no armazenamento controlado pelo NITE?
- O mesmo servidor Collabora converte o documento editado para PDF de maneira correta e segura?
- Em quais casos o LibreOffice headless apresenta resultados ou operação melhores?
- Qual é o custo em memória/CPU, inicialização, concorrência e latência?
- Quais riscos residuais e restrições impediriam a produção institucional?

### 3.2 Ambiente reprodutível

Criar a branch de investigação `spike/b05-collabora-wopi-docx` a partir da `main` validada, sem misturar código de produção. Estrutura sugerida:

```text
experiments/b05-document-engine/
  README.md
  compose.yaml
  .env.example                 # sem credenciais reais
  wopi-host/                  # serviço de teste isolado, mínimo
  fixtures/README.md          # manifestos e hashes, nunca dados sensíveis
  scripts/                    # execução e medições
  results/                    # template dos resultados, sem documentos privados
  ADR-B05-001.md              # decisão de arquitetura
```

Serviços locais: (a) Collabora CODE com imagem **fixada por versão/digest**, (b) host WOPI mínimo, preferencialmente Spring Boot/Java 21 para testar compatibilidade com a stack; (c) diretório ou armazenamento de objetos **de teste** com versões; (d) conversor LibreOffice headless comparativo **isolado**. Opcionalmente reverse proxy local para HTTPS/WSS e origem consistente.

**Topologia de rede obrigatória:** o navegador deve alcançar a URL do Collabora; o Collabora deve alcançar a URL WOPI; o host WOPI deve alcançar o repositório de arquivos de teste. Não assumir que `localhost` significa o mesmo endereço dentro e fora de contêineres. Usar DNS de serviço e origens explicitamente configuradas, sem `--network=host` como requisito de produção.

Na prova inicial, autenticação pode usar contas sintéticas e capacidades temporárias; não usar dados pessoais de integrantes. Na integração definitiva, tokens WOPI serão emitidos somente após autorização Entra/contextual no backend.

### 3.3 Corpus obrigatório de documentos DOCX

| Código | Documento de teste | Risco principal |
|---|---|---|
| F01 | Texto simples, estilos, listas e títulos | Formatação básica |
| F02 | Documento acadêmico com regras tipográficas fornecidas, margens e recuos | Fidelidade acadêmica |
| F03 | Tabelas com mesclagem, repetição de cabeçalho e quebra de página | Paginação |
| F04 | Imagens ancoradas, legendas e referências | Posicionamento |
| F05 | Cabeçalho, rodapé, numeração e páginas especiais | Campos e seções |
| F06 | Sumário automático, referências cruzadas e notas | Atualização de campos |
| F07 | Documento com comentários e alterações controladas | Semântica de edição |
| F08 | Documento com fontes especiais, símbolos e equações | Fontes/substituição |
| F09 | Documento longo (dezenas de páginas) e repetidos ciclos de edição | Desempenho/estabilidade |
| F10 | Documento propositalmente corrompido, malformado ou com extensão falsa | Segurança e tratamento de erros |

Quando houver baseline produzido pelo Microsoft Word ou documento original aprovado, mantê-lo imutável e registrar sua origem. Não afirmar equivalência de layout ao Word sem uma referência visual. **Fixar as fontes instaladas** em cada motor e documentar versões/ambiente. Evitar documentos pessoais/reais antes de autorização.

### 3.4 Execução comparativa

Para cada fixture válida, executar:

1. Preservar DOCX original com SHA-256 e metadados da fixture.
2. Converter original para PDF no Collabora (`POST /cool/convert-to/pdf`, conforme recursos habilitados e allowlist de origem).
3. Converter o mesmo original para PDF pelo LibreOffice headless, com versão e perfil fixados.
4. Abrir DOCX via WOPI no editor, editar um conjunto de elementos definidos, salvar e fechar.
5. Reabrir o arquivo **recuperado pelo WOPI host**, não uma cópia local ocultamente mantida no navegador.
6. Baixar o novo DOCX, validar conteúdo e estrutura e converter novamente para PDF.
7. Comparar renderizações página a página, contagem de páginas, tabelas, estilos, imagens, campos e conteúdo textual. Registrar diferenças aceitas e rejeitadas com capturas sintéticas.
8. Repetir abertura/edição/salvamento por pelo menos três ciclos em fixtures críticas.
9. Medir uso de memória/CPU, tempos de inicialização, abertura, salvamento e conversão, isoladamente e com usuários simultâneos.
10. Registrar falhas de parser, fontes ausentes, mudanças indevidas, timeout, locks e documentos não salvos.

Os testes de conversão não substituem os testes de salvamento WOPI. Os PDFs comparados deverão ser renderizados para imagens; hash de PDF não é critério suficiente, pois metadados podem variar sem alterar o layout.

### 3.5 Host WOPI de teste — operações e segurança

**Visualização:** `CheckFileInfo` e `GetFile` sobre identificadores opacos. **Edição:** `PutFile` e as operações de lock pertinentes (`Lock`, `RefreshLock`, `Unlock`, `UnlockAndRelock`), mais tratamento de conflitos. Avaliar necessidade de `PutRelativeFile`/Save As e compatibilidade com a versão específica testada; não anunciar operações não implementadas em `CheckFileInfo`.

Implementar: descoberta de endpoint (`/hosting/discovery`), URL de iframe gerada pelo fluxo documentado, token de acesso específico do documento (expiração, permissão leitura/escrita, revogação), validação da origem/caller apropriada, WOPI host allowlist, TLS na topologia aplicável, proibição de caminhos do cliente, verificação de tamanho/versão/lock e salvamento atômico com versão imutável. Manter tokens fora de logs e URLs públicas não intencionais. Testar ciclo de login/cookie/CSP/iframe/WebSocket.

**Nota:** o host WOPI **não é fornecido automaticamente pelo Collabora**. Ele precisa expor os arquivos e autorizar edições; o Collabora não é banco de dados nem repositório documental do NITE CONNECT.

### 3.6 Matriz de teste B05.0

| ID | Caso obrigatório | Condição de aceite |
|---|---|---|
| P01 | Inicialização Docker limpa | Discovery e health verificáveis |
| P02 | DOCX→PDF no Collabora | PDF íntegro e legível |
| P03 | DOCX→PDF no LibreOffice | Resultado comparativo registrado |
| P04 | Editar/salvar/reabrir DOCX | Alteração persiste no WOPI host |
| P05 | Três ciclos de edição | Sem corrupção nem perda silenciosa |
| P06 | Fontes e formato acadêmico | Layout inspecionado e diferenças catalogadas |
| P07 | Tabelas/imagens/sumário | Elementos preservados ou limitações explicitadas |
| P08 | Abrir com token expirado/revogado | Negado |
| P09 | Usuário com acesso somente leitura | Nenhum salvamento autorizado |
| P10 | Tentar WOPI ID de outro documento | Não há IDOR/vazamento |
| P11 | Edição simultânea autorizada | Locks/conflictos tratados sem sobrescrita silenciosa |
| P12 | Recurso alterado antes do PutFile | Conflito detectado/recuperável |
| P13 | Falha de armazenamento durante salvamento | Versão anterior recuperável, erro explícito |
| P14 | Conversão indisponível/timeout | Falha controlada; DOCX fonte intocado |
| P15 | Arquivo inválido, extensão falsa, ZIP bomb | Recusa/isolamento seguro, sem exaustão descontrolada |
| P16 | Conversão com origem não autorizada | Endpoint não exposto publicamente |
| P17 | Download após revogação | Negado |
| P18 | Logs, temporários e tokens | Sem conteúdo privado/segredos vazados |
| P19 | Sessões simultâneas e documento longo | Memória/CPU/latência medidas, sem OOM não tratado |
| P20 | Reproduzir experimento em máquina limpa | Ambiente e resultados rastreáveis |

### 3.7 Critérios de GO / GO com ressalvas / NO-GO

**Não negociáveis (GO):** sem perda silenciosa; salvamento WOPI efetivo; isolamento por documento/usuário; revogação funcionando; versões anteriores recuperáveis; conversão não expondo dados; nenhuma falha crítica não resolvida; documentação de implantação local reproduzível. **Aceite editorial:** responsáveis conferem os DOCX prioritários (incluindo os modelos acadêmicos) e aprovam explicitamente desvios de layout. Não usar um limiar arbitrário de pixels como único critério.

**GO com ressalvas:** diferenças visuais limitadas e aprovadas, gargalos mensurados que não inviabilizam a carga-alvo, funcionalidades avançadas adiadas. Registrar mitigação, responsável e escopo.

**NO-GO:** perdas de conteúdo, salvamento inconsistente, bypass de autorização, conflitos que sobrescrevam versões, falhas críticas de segurança, indisponibilidade operacional incompatível com recursos aprovados ou fidelidade inadequada nos documentos obrigatórios.

O resultado é um `ADR-B05-001.md` com evidências, versões, matrizes, gráficos de consumo e decisão. **Se NO-GO**, não adicionar LibreOffice como remendo automático: revisar a integração, limites dos formatos ou comparar outra solução de edição antes de aprovar novo caminho.

---

## 4. B05.1 — Refinamento funcional antes da V6

### 4.1 Decisões documentais propostas (pendentes de aprovação)

| ID | Questão | Proposta de partida |
|---|---|---|
| D01 | Unidade de documento | UUID e versão imutável de conteúdo; metadados versionados |
| D02 | Contextos permitidos | Pessoa, projeto, evento ou demanda administrativa (RF-036); atividade como referência adicional proposta, não substitui evento |
| D03 | Permissão de leitura | Derivada de contexto + ACL explícita apenas onde necessário; sem exposição pública por padrão |
| D04 | Permissão de criação/edição | Documentos oficiais: somente GESTOR_GERAL/COORDENADOR criam/retiram e definem destinatários; edição conforme atribuição; criação não oficial por integrantes depende de decisão expressa |
| D05 | Documento pessoal | Privado ao autor e a funções explicitamente aprovadas; definir alcance da administração |
| D06 | Modelos | Versionamento e catálogo autorizados; modelo não é documento oficial sem aprovação |
| D07 | Fonte editável | DOCX inicialmente; upload PDF em somente leitura sujeito à política de tipos |
| D08 | Formatos secundários | ODT, XLSX, PPTX, imagens e ZIP fora do escopo inicial salvo confirmação |
| D09 | Documento histórico | Versões imutáveis, download de versão anterior autorizado, sem sobrescrita |
| D10 | Colaboração | Definir se edição simultânea é obrigatória no primeiro release ou apenas edição única bloqueada |
| D11 | Publicação/arquivamento | Estados não são homologação acadêmica nem validação de entregas |
| D12 | Exclusão/retirada | Retirada lógica com motivo; política de retenção e expurgo sujeita a governança |
| D13 | Conversão | PDF derivado da versão exata DOCX; resultado não substitui fonte editável |
| D14 | Metadados/documentos sensíveis | Visibilidade mínima e prevenção de vazamento em pesquisas/históricos |
| D15 | Persistência | Object storage privado persistente; provedor/custo/backup precisam ser aprovados |
| D16 | Editor institucional | Collabora aprovado apenas após gate B05.0; CODE não presumido como edição de produção |

### 4.2 Decisões do Diário de Bordo propostas (pendentes de aprovação)

| ID | Questão | Proposta de partida |
|---|---|---|
| D17 | Forma principal do Diário | Entrada estruturada no sistema com exportação DOCX/PDF; confirmar se também se exige edição DOCX direta |
| D18 | Periodicidade | Registro periódico individual, preferencialmente semanal (RF-034); sem prazo obrigatório ou penalidade; periodicidade formal depende de inventário |
| D19 | Associação de contexto | Integrante obrigatório; projeto e atividade opcionais conforme elegibilidade |
| D20 | Conteúdo | Data/período da contribuição, título, texto, referências e autoria; limites a aprovar |
| D21 | Retificações | Criar revisão auditada; preservar texto anterior, autor, motivo e instante |
| D22 | Leitura por liderança | Definir se líder lê entradas do projeto, resumos ou somente entradas compartilhadas |
| D23 | Homologação | Registro de Diário não equivale a validação de horas B04, entrega formal ou nota |
| D24 | Documentos anexos | Associar documentos autorizados ao Diário; não duplicar blobs ou permissões por cópia |
| D25 | Exportação | Definir template, capa, período, ordenação, campos e responsabilidades editoriais |
| D26 | Regras de revisão | Definir se existem `RASCUNHO`, `REGISTRADO`, `REVISADO`/`VALIDADO`; não criar aprovação institucional sem autorização |

B05.1 somente termina após decisões aprovadas, rastreabilidade conferida no PDF e publicação de contratos HTTP preliminares.

---

## 5. Arquitetura candidata pós-gate

```text
Navegador do integrante (sessão Entra/NITE CONNECT)
         |
         v
NITE CONNECT Web (Next.js, quando houver frontend integrado)
         |                  \
         v                   \ iframe/editor em origem Collabora
NITE CONNECT API Java        v
  - autenticação Entra       Collabora Online (serviço isolado)
  - política por contexto       | WOPI: CheckFileInfo/GetFile/PutFile/locks
  - gestão documental    <------+
  - diário de bordo
  - metadados/versões ------> PostgreSQL (Neon ou aprovado)
  - objetos privados --------> Object storage persistente (provedor aprovado)
  - conversão via API -------> Collabora /cool/convert-to/pdf (servidor-servidor)
```

**Separação de responsabilidade:** Collabora é motor de edição/renderização/conversão, nunca fonte de verdade documental. PostgreSQL guarda metadados, versões, vínculos, ACL, locks e trilhas; objetos privados guardam bytes. O backend aplica permissão de leitura/escrita em cada operação WOPI e documento. O frontend apenas hospeda a experiência e não decide autorização.

O staging atual preparado no repositório usa Cloudflare Container `basic` (1 GiB) para a API Java, com `max_instances=1` e `sleepAfter=10m`; **não instalar Collabora no mesmo contêiner** por padrão. Avaliar serviço isolado e custos antes de qualquer deploy. O provedor de objetos não está aprovado; Cloudflare R2, S3 compatível ou infraestrutura própria são **alternativas a avaliar**, não escolhas já contratadas.

### Fronteiras entre módulos

- `documents`: agregados de documento, modelo, versão, download, conversão, WOPI.
- `journal`: entrada e revisão de Diário de Bordo; contratos explícitos para documento/atividade/projeto.
- `activities` B03: fornece consulta mínima/eligibilidade por contrato, sem acesso direto a suas tabelas de outro módulo.
- `attendance` B04: histórico/horas consultável conforme permissão, **sem mutação implícita por Diário**.
- `members/projects/access`: autorização contextual, nomes minimizados, status ATIVO.
- `audit`: manter trilha global sem conteúdos privados ou tokens.

---

## 6. Persistência proposta — Flyway V6 (somente depois de B05.1)

Nome inicial: `V6__documents_templates_journal.sql` (sujeito a refinamento antes de migração). V1–V5 são imutáveis; migrações posteriores sempre aditivas.

| Tabela candidata | Campos/invariantes importantes |
|---|---|
| `document_templates` | id, nome, finalidade, tipo, origem, status, versão, responsável |
| `document_template_versions` | template_id, version, object_id, checksum, data, imutabilidade |
| `documents` | id, título, contexto/tipo, autor, status, current_version_id, versão otimista, datas |
| `document_versions` | id, document_id, número, object_id, hash, MIME validado, size, created_by/at, motivo; append-only |
| `document_objects` | chave opaca, checksum, tamanho, localização lógica, estado de persistência, sem URL pública |
| `document_derivatives` | versão de origem, formato derivado, hash/objeto, motor+versão e data |
| `document_edit_sessions` | token hash/referência, documento, usuário, nível de acesso, expiração/revogação; evitar guardar token em claro |
| `document_locks` | lock de edição por documento, token/id WOPI, expiração, versão esperada |
| `document_assignments` | destinatários e acesso explícito, concessão/retirada por autoridade; contrato ainda proposto |
| `document_events` | trilha append-only de criação, atualização, conversão, permissões, versões, retirada |
| `journal_entries` | autor, data/intervalo descritivo, contexto opcional, status, revisão vigente, datas |
| `journal_revisions` | entrada, número, corpo, autor, motivo, instante; imutável |
| `journal_links` | referências documentais ou de atividade autorizadas, sem cópia de arquivos |
| `journal_events` | eventos de revisão/estado/permissão, append-only se não cobertos por trilha equivalente |

Restrições essenciais: FK sem cascade delete de históricos; unicidade de `(document_id, version_number)`; hashes e tamanho válidos; exatamente uma versão corrente coerente; não aceitar edição de versão já confirmada; `document_versions` não pode ser reescrita; `journal_revisions` não pode ser reescrita; lock único vigente segundo a semântica WOPI; referência a documento/atividade deve existir e respeitar contexto também na camada de serviço. Tipos e contextos devem ser finitos e explicitamente modelados — evitar texto arbitrário que contorne autorização.

**Escrita documento/objeto:** nunca pressupor transação atômica entre banco e storage. Preferir staging + hash + promoção + commit de metadados + reconciliação/garbage collection seguros; não publicar objeto antes do commit; recuperar versão anterior após falha. Preservar rastreabilidade de uploads interrompidos e cópias derivadas órfãs. Testar perda de conexão e crash entre as etapas.

---

## 7. Contratos HTTP candidatos (não congelados)

Todos sob `/api/v1`; Entra JWT, integrante ATIVO, autorização contextual, 401/403/404/409/413/415/503, `ProblemDetail`, paginação `PageResult`, `expectedVersion` e `reason` onde houver mutação. UUID de outro contexto deve responder 404 quando apropriado; operações rejeitadas nunca vazam nomes, metadados ou existência dos documentos.

### Documentos e modelos

| Método | Endpoint proposto | Função |
|---|---|---|
| GET | `/documents` | Catálogo autorizado com filtros de projeto/tipo/autor/status |
| POST | `/documents` | Criar metadados e origem de documento a partir de modelo ou upload |
| GET | `/documents/{id}` | Detalhe autorizado e versão corrente |
| PATCH | `/documents/{id}` | Alterar metadados com versão/motivo |
| GET | `/documents/{id}/versions` | Histórico paginado de versões |
| GET | `/documents/{id}/versions/{versionId}` | Metadados de versão autorizada |
| GET | `/documents/{id}/versions/{versionId}/download` | Download autenticado |
| POST | `/documents/{id}/uploads` | Upload com validação e registro imutável de versão |
| POST | `/documents/{id}/editor-sessions` | Emitir autorização curta e URL de abertura de editor após gate |
| POST | `/documents/{id}/exports` | Converter versão selecionada para PDF; devolver status/arquivo |
| GET | `/documents/{id}/exports/{exportId}` | Consultar derivado autorizado |
| GET | `/documents/{id}/history` | Trilha documental contextual |
| GET | `/document-templates` | Listar modelos disponíveis |
| POST | `/document-templates` | Criar modelo com gestão autorizada |
| POST | `/document-templates/{id}/versions` | Nova versão imutável do modelo |

**WOPI:** endpoint separado com semântica própria (por exemplo, `/wopi/files/{id}` e `/wopi/files/{id}/contents`), não exposto como rota genérica autenticada por JWT de usuário. Ele recebe *capabilities* de curta duração emitidas pelo backend e valida assinatura/identidade/escopo/expiração/lock/versão em cada chamada. Rotas explicitamente allowlisted e auditadas, sem confundir token WOPI com sessão web ou ID de documento. Implementar `CheckFileInfo`, `GetFile`, `PutFile`, operações de lock necessárias e alternativas de Save As apenas se anunciadas no protocolo. Definir o suporte a colaboradores simultâneos no contrato.

### Diário de Bordo

| Método | Endpoint proposto | Função |
|---|---|---|
| GET | `/me/journal-entries` | Listagem pessoal paginada |
| POST | `/me/journal-entries` | Criar entrada pessoal |
| GET | `/journal-entries/{id}` | Ler entrada conforme contexto/autoridade |
| PATCH | `/journal-entries/{id}` | Alterar conforme estado/versão ou criar revisão |
| POST | `/journal-entries/{id}/revisions` | Retificação explícita com motivo |
| GET | `/journal-entries/{id}/history` | Histórico autorizado de revisões |
| POST | `/journal-entries/{id}/links` | Associar projeto/atividade/documento permitido |
| POST | `/journal-exports` | Exportar conjunto autorizado em DOCX/PDF |
| GET | `/projects/{id}/journal-entries` | Consulta do projeto somente se D22 aprovada |

Preferir contratos de diário independentes do formato de arquivo. Caso se aprove Diário DOCX como documento principal, revisar essa API antes da implementação para não manter duas fontes de verdade da mesma narrativa.

---

## 8. Gestão documental — comportamentos obrigatórios

### 8.1 Criação, upload e organização

- Criar documento em contexto elegível, com nome validado, finalidade, autor, tipo e status apropriado.
- Criar documento a partir de **cópia de uma versão imutável** de modelo, mantendo origem e número do modelo usado.
- Upload com política de formatos, assinatura/estrutura, tamanho, macros/conteúdo ativo, extensões e MIME coerentes; DOCX é pacote ZIP e exige validação contra descompressão excessiva.
- Não disponibilizar caminho físico, objeto privado, token nem URL de download estático.
- Listar e filtrar **após** aplicar visibilidade, inclusive no `count(*)` da paginação.

### 8.2 Edição e versões

- Iniciar sessão de leitura ou edição conforme autoridade, com expiração/revogação.
- Verificar lock e a versão base ao salvar; salvar sempre **nova versão imutável**.
- Salvar é confirmação de persistência, não simples feedback visual do editor; emitir versão/hash recuperáveis.
- Em disputa de escrita ou expiração, retornar conflito controlado e fornecer recuperação sem sobrescrita silenciosa.
- Retirada/arquivamento e fim de participação revogam escrita/visibilidade conforme decisões D02–D05.
- Limitar conteúdo de auditoria a metadados; texto do documento só é acessível pela autorização documental.

### 8.3 Exportação e visualização

- Exportação PDF vinculada a `document_version_id`, versão do motor, parâmetros e checksum.
- PDF derivado deve ser reproduzível em sentido funcional; não prometer bytes idênticos quando o motor inclui metadados diferentes.
- Recusar conteúdo danificado e conversão parcial; tratar indisponibilidade temporária sem afetar o DOCX fonte.
- Operações pesadas não devem manter o advisory lock administrativo durante conversão/renderização.
- Converter versões específicas evita que o usuário solicite PDF de uma versão e receba outra editada segundos depois.

---

## 9. Diário de Bordo — modelo e semântica

**Proposta principal sujeita à B05.1:** Diário como entradas estruturadas, com histórico auditável e exportação documental. Esse modelo dá boas consultas e evita indexar texto DOCX para cada registro, porém **não substitui** um Diário DOCX editável se a especificação consolidada o exigir.

Campos iniciais: autor, data/período de referência, título, descrição do trabalho/progresso, dificuldades, próximos passos opcionais, projeto e atividade associados quando autorizados, documentos/evidências vinculados, criado em/alterado em, versão e estado se aprovado. Nenhum desses campos determina nota, produtividade ou carga horária por si só.

- Entradas pessoais sem projeto devem ser permitidas se coerentes com o requisito de Diário individual; criação para terceiro não será presumida.
- Ao citar atividade B03, validar contexto e participação vigente na escrita; manter histórico da referência sem conceder leitura posterior indevida.
- Ao citar período B04, não permitir que texto do Diário altere a duração ou o status de revisão de horas.
- Revisões guardam conteúdo anterior e motivo, sem `DELETE` físico na API comum.
- Acesso após desligamento/saída e visibilidade por líderes/administradores devem ter regras explícitas, distintas para contexto individual e de projeto.
- Exportação pode compor período + entradas ordenadas cronologicamente, mantendo referência às revisões utilizadas e ao modelo de exportação.
- Não compartilhar automaticamente todo o Diário pessoal com qualquer participante de um projeto.

---

## 10. Plano de implementação (somente após gate B05.0 e decisões B05.1)

| Etapa | Implementação | Saída/verificação |
|---|---|---|
| **B05.0** | Spike Collabora/LibreOffice/WOPI/DOCX | ADR e relatório de comparação; gate GO/NO-GO |
| **B05.1** | Requisitos oficiais, D01–D26, matriz de permissões, estado, contratos | Plano funcional aprovado e rastreabilidade conferida |
| **B05.2** | Migração V6, invariantes, históricos e repositórios | Flyway banco novo e V5→V6, PostgreSQL 17 real |
| **B05.3** | Porta de storage persistente, upload seguro, catálogo e modelos | Testes de objetos, versões, isolamento, falhas e recuperação |
| **B05.4** | APIs documentais, metadados, versão, download, contexto e auditoria | MockMvc/integração, acesso positivo/negativo e concorrência |
| **B05.5** | Host WOPI, sessões, Collabora, locks, salvamento | E2E editar/salvar/reabrir; token expirado, revogado e conflito |
| **B05.6** | Conversão, visualização/exportação PDF e rastreamento de derivados | PDF da versão correta, testes de falha/timeout e fidelidade |
| **B05.7** | Diário de Bordo, revisões, contexto, pesquisa e exportação | Registros e retificações sem geração automática de horas |
| **B05.8** | Segurança, observabilidade, recuperação, documentação, revisão independente, CI e PR | Nova suíte + 185 regressões antigas; não executar deploy/merge automático |

**Ordem de implementação:** branch nova a partir da `main` integrada, contrato aprovado antes da V6, testes RED→GREEN por incremento, regressão completa ao final e sempre após mudanças compartilhadas. Na ausência de aprovação para storage institucional, continuar somente no modo desenvolvimento/testes com adaptador isolado, e não declarar implantação documental institucional concluída.

Branches recomendadas: `spike/b05-collabora-wopi-docx` para investigação e `feat/bloco-05-gestao-documental-diario` para produto. Fazer PR separado da POC ou publicar ADR para revisão antes de qualquer código de produção.

---

## 11. Permissões iniciais — matriz a homologar

| Ação | Autor da versão pessoal | Participante vigente do projeto | Líder vigente | Administração global |
|---|---|---|---|---|
| Criar documento pessoal | Sim | Não por participação | Não por liderança | Política D05 |
| Ler documento pessoal | Sim | Não | Não | Política D05 |
| Criar documento do projeto | Conforme função permitida | A decidir D04 | Sim, se autorizado | Sim |
| Ler documento do projeto | Conforme vínculo e classificação | Conforme classificação | Sim | Sim |
| Editar documento do projeto | Com permissão expressa | Não automaticamente | Conforme classificação | Sim |
| Gerir modelos institucionais | Não | Não | Não por padrão | Sim |
| Consultar Diário próprio | Sim | Não | Não por padrão | Conforme D22 |
| Consultar Diário de integrante da equipe | Não por padrão | Não | Conforme D22 | Conforme D22 |
| Exportar/documentar versões | Mesma permissão de leitura | Mesma permissão de leitura | Mesma permissão de leitura | Conforme política |

A `main` dos blocos anteriores mantém `AccessService.requireActive`, `ProjectAccess`, identidades Entra e escopo contextual. Reutilizar os contratos existentes em vez de criar permissões paralelas. Histórico pessoal **não** concede automaticamente acesso eterno ao conteúdo compartilhado de colegas após saída do projeto.

---

## 12. Testes de integração e segurança B05.2–B05.8

| ID | Teste | Aceite |
|---|---|---|
| T01 | Documento pessoal sem projeto | Criado sem projeto fictício |
| T02 | Documento de projeto por autorizado | Contexto gravado |
| T03 | Projeto alheio/UUID forjado | 404/403 sem metadados indevidos |
| T04 | Integrante AFASTADO/DESLIGADO | Operações negadas conforme política |
| T05 | Criar a partir de modelo versão N | Origem e versão gravadas |
| T06 | Criar modelo sem permissão | Negado |
| T07 | Alterar metadados com versão antiga | 409 e sem gravação parcial |
| T08 | Upload com extensão/MIME falso | Recusado |
| T09 | DOCX corrompido/zip bomb | Recusado, consumo limitado |
| T10 | Upload grande/sem espaço | 413/503 controlados; sem documento disponível |
| T11 | Download por documento e versão | Bytes/hash íntegros |
| T12 | Download após revogação | Negado |
| T13 | Alterar versão antiga | Bloqueado; append-only |
| T14 | Nova versão válida | Histórico preservado e versão corrente correta |
| T15 | Dois salvamentos com mesma base | Sem perda silenciosa |
| T16 | Falha de banco após escrita de objeto | Reconciliação segura, versão anterior intacta |
| T17 | Falha de storage após preparo | Sem versão disponível inválida |
| T18 | Recuperação de órfão/staging | Não excluir AVAILABLE; dry-run efetivo |
| T19 | Sessão editor somente leitura | Não consegue PutFile |
| T20 | Token WOPI expirado | Rejeição |
| T21 | Token WOPI de documento A usado em B | Rejeição |
| T22 | Reativação de token revogado | Rejeição |
| T23 | Lock/refresh/unlock e expirado | Protocolo obedecido |
| T24 | Edição por dois usuários autorizados | Conflitos tratados sem corrupção |
| T25 | Edição por autor que perdeu participação | Salvamento rejeitado após revogação |
| T26 | Conversão DOCX→PDF normal | Derivado válido e atribuído à versão |
| T27 | Converter versão N enquanto N+1 é editada | PDF corresponde a N |
| T28 | Timeout/erro do Collabora | DOCX intacto e erro recuperável |
| T29 | URL pública de conversão fora da allowlist | Negado |
| T30 | PDF ou anexo com scripts/macros suspeitos | Política de tipos aplicada |
| T31 | Pesquisa e paginação de documentos | `count` e itens restritos antes dos filtros |
| T32 | Auditoria de criação/revisão/download crítico | Metadados sem conteúdo secreto |
| T33 | Falha injetada na auditoria | Mutação DB revertida |
| T34 | Diário independente | Registro pessoal válido |
| T35 | Diário associado a projeto vigente | Associação autorizada |
| T36 | Diário associado a projeto/atividade alheia | Negado |
| T37 | Diário referenciando B03 | Não altera estado da atividade |
| T38 | Diário referenciando B04 | Não altera horas validadas |
| T39 | Retificação do Diário | Original e motivo preservados |
| T40 | Revisão concorrente de Diário | Uma vitória ou conflito controlado |
| T41 | Diário de colega | Isolamento conforme D22 |
| T42 | Exportação de Diário do período | Entradas e versões corretas |
| T43 | Retirada lógica de documento | Histórico permanece; leitura conforme política |
| T44 | Logs, URLs, headers e erros | Sem conteúdo/tokens/chaves internas |
| T45 | WOPI/callback inesperado com rota não permitida | Deny-all |
| T46 | Migração V5→V6 com dados de B01–B04 | Integridade anterior preservada |
| T47 | Banco novo V1→V6 | Schema coerente e restrições presentes |
| T48 | Concorrência storage+DB+revogação | Resultado íntegro e auditável |
| T49 | Frontend DOCX em browser (quando disponível) | Abre, salva, reabre e exporta E2E |
| T50 | `mvnw.cmd --batch-mode verify` | 185 testes prévios preservados + novos, zero skips involuntários |

Cobrir testes de domínio, MockMvc, Testcontainers PostgreSQL 17 e integração local WOPI/Collabora com documentos sintéticos. Não confundir teste de contrato HTTP com validação visual de fidelidade. Não fixar artificialmente um número de novos testes como critério de qualidade.

---

## 13. Ameaças, privacidade, resiliência e operação

**Modelo de ameaça mínimo:** IDOR entre documentos; token WOPI vazado/reutilizado; SSRF ou callback não autorizado; conversão pública explorável; macros e conteúdo ativo; payload DOCX comprimido malicioso; path traversal/symlink; overwrite concorrente; exposição de arquivo temporário ou cache; logs com conteúdo ou URL portadora de credenciais; iframe/origem insegura; websocket não autenticado; usuário removido durante edição; objeto órfão após crash; saída PDF de versão errada.

**Controles:** allowlist explícita de origens/hosts e rotas, autenticação e autorização por ação, tokens curtos com privilégio mínimo e revogação, TLS no ambiente compartilhado, CSP/frame-ancestors dimensionados para o editor, validação de formato e limites, isolação de processos de conversão, mínimo acesso a storage, checksums, backup/restauração testados, expurgo seguro de temporários, timeouts, limites de upload, rate limiting conforme medição, métricas sem dados sensíveis e CI para regressão.

**Operação/custos:** medir memória ociosa e por sessão, CPU na conversão, tempos de abertura/primeira edição/salvamento, pico de documentos simultâneos, disponibilidade, cold start, tamanho médio, crescimento de versões, custos de armazenamento/egress e política de retenção. O contêiner Cloudflare atualmente preparado para a API **não é** um destino automático para Collabora.

**Licença/suporte:** CODE é edição de desenvolvimento sem compromisso de suporte ou estabilidade para produção. Formalizar decisão de edição de produção e revisar licenciamento, atualizações e necessidade de SLA com o responsável antes de implantação institucional.

---

## 14. Critérios de conclusão e gates

### Gate G0 — iniciar POC

- Docker Desktop funcional; ambientes de teste e fixtures sintéticas disponíveis.
- Origem/domínios de teste, versões de imagem e fontes padronizadas.
- Não há necessidade de credenciais de produção, DNS institucional ou compra.

### Gate G1 — aprovar motor documental

- P01–P20 executados com evidências e falhas classificadas, incluindo desktop/mobile. Fixtures sintéticas habilitam triagem técnica, mas não substituem modelos representativos autorizados do NITE para aceite editorial.
- WOPI E2E e DOCX salvo/reaberto comprovados.
- Fidelidade de arquivos obrigatórios aprovada editorialmente.
- Política de suporte, isolamento e recursos definida como viável para próximo estágio.
- ADR-B05-001 assinado/aprovado pelo responsável; GO/GO com ressalvas/NO-GO explícito.

### Gate G2 — liberar implementação B05.2

- PDF consolidado conferido na B05.1 com identificação dos requisitos de Diário e documentos.
- D01–D26 decididas (nem todas precisam de uma preferência idêntica à proposta).
- Matriz de acesso, estados e payloads assinados; formato principal do Diário definido.
- Decisão de armazenamento persistente ou restrição formal a desenvolvimento/testes.

### Gate G3 — concluir implementação técnica

- V6 aprovada, nova/upgrade DB PostgreSQL 17 testados.
- Versionamento e permissões sem vazamentos, integração WOPI e conversão com critérios aprovados, Diário implementado.
- Testes anteriores (185 baseline) e novos passam na CI; falhas/revogação/concorrência testadas.
- PR revisado independentemente, sem defeitos bloqueantes; documentos operacionais atualizados.
- Sem merge/deploy por mera conclusão da suíte.

### Gate G4 — homologação institucional separada

- Storage persistente, backups, privacidade, licenças, infraestrutura, domínio/HTTPS, carga e custos aprovados.
- Ambiente de homologação com usuários sintéticos/autorizados; política de retenção e revisão de conteúdos definida.
- Pessoa institucional autorizada fornece aceite; CI verde não substitui essa homologação.

---

## 15. Entregáveis e arquivos recomendados

```text
docs/implementation/
  bloco-05.md                   # objetivo, decisões, escopo, rastreabilidade
  bloco-05-api.md               # contratos HTTP e WOPI implementados
  bloco-05-execution.md         # execução, matriz de testes, resultados
  bloco-05-storage-runbook.md   # recuperação, backup, formato, deploy após aprovação
  adr-b05-001-document-engine.md
experiments/b05-document-engine/
  README.md
  compose.yaml
  wopi-host/
  fixtures/README.md
  results/README.md
apps/api/src/main/java/br/edu/unijorge/niteconnect/documents/
apps/api/src/main/java/br/edu/unijorge/niteconnect/journal/
apps/api/src/main/resources/db/migration/
  V6__documents_templates_journal.sql
```

**Artefatos da prova:** manifestos de versões, fixtures, relatórios por DOCX, PDFs sintéticos comparativos, métricas CPU/RAM/latência, incidentes, decisões e *runbook* reproduzível. Não versionar arquivos privados de integrantes ou chaves/tokens.

---

## 16. Perguntas que precisam de resposta do responsável

1. O Diário de Bordo deverá ser primariamente **entradas estruturadas exportáveis** ou **um documento DOCX editado no navegador**? Há exigência de modelo oficial do núcleo?
2. Quem pode **ler, editar, revisar e exportar** Diário pessoal, Diário associado a projeto e documentos com acesso reservado?
3. A primeira entrega precisa de **edição simultânea** ou pode iniciar com edição controlada por lock e um editor ativo por documento?
4. Qual serviço persistente de arquivos poderá ser usado depois da POC? Existe aprovação para um provedor de objetos (inclusive infraestrutura própria do NITE)?
5. Quais modelos DOCX oficiais e documentos acadêmicos representativos podemos usar **sem dados pessoais** como corpus de aceitação?
6. Quais formatos são realmente obrigatórios no primeiro release (DOCX, PDF, outros)?
7. Quais prazos, estados ou fluxos de aprovação documental/Diário têm homologação institucional, se existirem?

A POC B05.0 pode começar antes de todas essas respostas, desde que use dados sintéticos e recursos locais. **A V6 e a integração de produto aguardam as decisões que alteram o contrato.**

---

## 17. Referências verificáveis de tecnologia

- Collabora Online CODE (desenvolvimento e limites de uso): https://www.collaboraonline.com/code/ e https://www.collaboraonline.com/faqs/
- Collabora Online, SDK Manual, WOPI e API de conversão: https://sdk.collaboraonline.com/CO-SDK-manual.pdf
- Collabora, conversão via `/cool/convert-to/pdf`: https://www.collaboraonline.com/document-conversion/
- Microsoft Learn, WOPI host e operações de edição: https://learn.microsoft.com/en-us/microsoft-365/cloud-storage-partner-program/online/wopi-requirements
- Microsoft Learn, semântica de locks e tokens: https://learn.microsoft.com/en-us/microsoft-365/cloud-storage-partner-program/rest/concepts
- LibreOffice Help, exportação PDF por linha de comando: https://help.libreoffice.org/latest/en-US/text/shared/guide/pdf_params.html
- Repositório NITE CONNECT: https://github.com/IasminLuise/00_NiteConnect_2026.2_UNIJORGE

**Situação deste documento:** planejamento técnico proposto. Não representa execução da POC, aprovação de D01–D26, implementação da V6, contratação de infraestrutura ou aceite institucional.


## 18. Ajustes vinculantes da revisão aceita em 17/10/2026

- RF-034 é o Diário; Q20/21/22 confirmadas. O PDF não exige Diário principalmente DOCX. Entradas estruturadas com exportação seguem recomendação técnica, sem aprovação implícita de D17.
- RF-035: criação, disponibilização, atualização administrativa e retirada de modelos/documentos oficiais ficam na gestão geral/coordenação. Líder não ganha criação oficial por liderança. Distinguir edição de conteúdo atribuído de administração oficial.
- F06: modelar destinatários/atribuições e retirada de acesso explicitamente, com operações e testes positivos/negativos. Ainda não congelar payloads ou escolher ACL genérica.
- RF-036: evento é contexto obrigatório do produto; atividade é referência adicional proposta. Usar contrato explícito do módulo attendance, sem consultar tabelas internas diretamente.
- RF-039: incluir última abertura, última edição, autoria e instante; definir abertura efetiva e visibilidade. Não prometer que simples eventos de auditoria implementam a consulta.
- RF-034/F03: recuperar atividades/evidências autorizadas para seleção e observações sem redigitar, por contratos explícitos; sem validar horas ou concluir atividades automaticamente.
- POC: página mínima de integração dentro do experimento, desktop/mobile, sessões simultâneas, autosave, fechamento, reabertura efetiva pelo host e perda/revogação de sessão durante edição. Não depende de iniciar o frontend de produto.
- Fixar imagem e conferir comportamento da versão: o SDK 24.04 usa LastModifiedTime/X-COOL-WOPI-Timestamp para alterações externas. Locks Microsoft não provam compatibilidade com Collabora. Não impor expectedVersion imutável desde a abertura a todos os autosaves.
- Revalidar permissão/revogação e versão na confirmação de salvamento. Processamento e transferência externa fora do lock administrativo global; integrar ao protocolo de revogação existente somente na fase de produto.
- CI separada para experiments/** e branch spike/**: o workflow da API não cobre o experimento. Verificações locais/CI da POC não substituem E2E visual e aceite editorial.
- Base verificada: main e64340835d9a5a8c013cfee8c0ffac49c5054ff0; GitHub Actions run 38970129697, 185 testes, zero falhas/erros/skips, BUILD SUCCESS. Não repetir Maven da API por mudanças exclusivas no experimento.
- Sem V6, alteração de apps/api, .env, autenticação Entra, produção, compra ou deploy nesta execução. Branch spike/b05-collabora-wopi-docx.

### Rastreabilidade adicional e decisões já definidas

| Fonte | Aceite a acrescentar |
|---|---|
| RF-035 / CA-11 | Integrante/líder não cria nem retira oficial; consegue editar atribuído quando autorizado. |
| RF-036 | Documento de evento autorizado; vínculo a evento inexistente/alheio recusado. |
| RF-039 / F06 | Abertura e edição atualizam metadados corretos; consulta não vaza informações de terceiros. |
| RF-034 / F03 | Recuperação autorizada de contribuições evita redigitação e não muda B03/B04. |
| RF-052 / CA-12 | Liderança não abre documento pessoal de colega por simples vínculo à equipe. |
| RNF-018/019 / seção 9.2 | Arquivos inválidos limitados; modelos representativos, desktop/mobile e concorrência básica testados. |

O gate de produto permanece pendente. O aceite desta revisão não homologa provedor, representação final do Diário, visibilidade administrativa de dados pessoais, nem regras institucionais.
