# 00_NiteConnect_2026.2_UNIJORGE
Repositório para desenvolvimento do Sistema de Gerenciamento do Núcleo de Inovação e Tecnologia da UNIJORGE.

## Implementação atual — backend

A base atual usa Java 21, Spring Boot 4.1.1, Maven e PostgreSQL 17 em
`apps/api`, como monólito modular. `apps/web` contém uma interface mínima em Next.js para o Diário contínuo, ainda pendente da composição com a autenticação Entra.
As propostas de tecnologias e autenticação nas seções iniciais abaixo são
anteriores à especificação funcional consolidada e não orientam a implementação.

O Bloco 01 implementa identidade Microsoft Entra, integrantes, convites,
papéis administrativos e auditoria. A API permanece deny-all até a configuração
institucional ser habilitada. Configuração, provisionamento inicial, contratos
HTTP e limitações de homologação estão em
[Bloco 01 — contrato HTTP e operação](docs/implementation/bloco-01-api.md).
A fonte funcional é a especificação consolidada em `docs/product` e as decisões
posteriores explicitamente aprovadas.

O staging do Bloco 02 está preparado para Cloudflare Workers/Containers e
PostgreSQL no Neon. O pacote não foi implantado em contas externas; pré-requisitos,
secrets, bootstrap, deploy manual e roteiro de aceite estão no
[runbook de staging do Bloco 02](docs/implementation/staging-bloco-02.md).

O B05 adiciona o core documental e o adaptador ONLYOFFICE Docs API, com bytes
somente locais de desenvolvimento e editor desativado por padrão. Modelos oficiais
continuam bloqueados para edição/conversão. Configuração, endpoints e limites estão
no [contrato B05 local](docs/implementation/bloco-05-core-docs-api.md).

Validação local (Java 21 e Docker disponíveis):

```powershell
Set-Location apps/api
.\mvnw.cmd --batch-mode verify
```

Para subir o PostgreSQL local, configure `DB_PASSWORD` fora do controle de versão
e use `docker compose -f infra/compose.yaml up -d db` na raiz. Para iniciar a API,
execute `.\mvnw.cmd spring-boot:run` em `apps/api`. Não há senha própria nem
credenciais institucionais versionadas; a homologação Microsoft é uma etapa externa.

# NITE CONNECT
### Sistema Integrado de Gestão de Participação, Projetos e Atividades do NITE

## 1. Sobre o projeto

O **NITE CONNECT** é uma proposta de sistema web voltada à centralização e ao gerenciamento das informações, 
atividades e projetos desenvolvidos pelos integrantes do Núcleo de Inovação e Tecnologia (NITE). A iniciativa 
busca proporcionar um ambiente digital integrado para organizar a participação dos membros, acompanhar o 
andamento dos projetos, registrar atividades, gerenciar entregas e facilitar o acompanhamento do desempenho individual e coletivo.

O sistema pretende reduzir a fragmentação das informações, melhorar a comunicação entre os participantes e tornar 
os processos de acompanhamento mais organizados, transparentes e rastreáveis. O desenvolvimento parte de um protótipo 
de interface com identidade visual tecnológica, utilizando tons de preto, azul e branco, com foco em usabilidade, 
responsividade e experiência do usuário.

## 2. Problema e justificativa

A gestão do NITE envolve o acompanhamento simultâneo de integrantes, projetos, atividades, prazos, frequência e entregas.
Quando essas informações estão distribuídas em diferentes ferramentas ou registros, podem surgir dificuldades para:

- Centralizar as informações em um único ambiente.
- Acompanhar o andamento das atividades e dos projetos.
- Monitorar prazos e entregas pendentes.
- Registrar e consultar a participação dos integrantes.
- Manter um histórico confiável das atividades realizadas.
- Produzir informações para acompanhamento e avaliação.
- Garantir que cada usuário visualize e gerencie apenas as informações permitidas ao seu perfil.

Diante desse cenário, o NITE CONNECT propõe uma plataforma centralizada para apoiar a organização 
operacional do núcleo e fornecer informações que auxiliem o acompanhamento de suas atividades.

## 3. Objetivos

### 3.1. Objetivo geral

Desenvolver um sistema web integrado para apoiar a gestão dos integrantes, projetos, atividades, 
registros de participação e entregas do NITE, promovendo organização, transparência, rastreabilidade 
e eficiência nos processos internos.

### 3.2. Objetivos específicos

- Centralizar informações relacionadas aos integrantes e aos projetos.
- Permitir o gerenciamento de usuários e seus respectivos perfis de acesso.
- Organizar atividades, responsáveis, prazos e situações de execução.
- Possibilitar registros no Diário de Bordo.
- Apoiar o controle de frequência e das modalidades de participação presencial e remota.
- Acompanhar entregas, pendências e resultados.
- Disponibilizar indicadores e relatórios para acompanhamento gerencial.
- Implementar notificações e alertas sobre prazos e pendências.
- Manter histórico de registros e alterações relevantes.
- Facilitar o acesso às informações por meio de uma interface responsiva.
- Criar uma base tecnológica que permita futuras melhorias e expansões.

## 4. Público-alvo

O sistema será destinado aos participantes e responsáveis pela organização do NITE, considerando diferentes níveis de acesso.

Os perfis inicialmente previstos são:

| Perfil | Responsabilidades previstas |
|---|---|
| Administrador | Gerenciar configurações gerais, usuários e permissões. |
| Coordenador | Acompanhar projetos, integrantes, atividades e indicadores. |
| Orientador | Acompanhar atividades, entregas e participação dos integrantes sob sua orientação. |
| Integrante | Consultar projetos, acompanhar tarefas, registrar atividades e visualizar seu histórico. |

As permissões definitivas de cada perfil deverão ser especificadas e validadas durante o levantamento dos requisitos.

## 5. Funcionalidades previstas

### 5.1. Autenticação e controle de acesso

- Tela de login.
- Autenticação de usuários.
- Controle de acesso baseado em perfis e permissões.
- Fluxo de recuperação de senha.
- Gerenciamento seguro de sessões.
- Proteção de páginas e funcionalidades restritas.

### 5.2. Dashboard e visão geral

- Indicadores resumidos de projetos e atividades.
- Visualização de pendências e prazos próximos.
- Resumo de frequência e participação.
- Acompanhamento de entregas.
- Central de notificações e alertas.
- Acesso rápido às funcionalidades principais.

### 5.3. Gestão de integrantes

- Cadastro e consulta de integrantes.
- Atualização dos dados cadastrais.
- Associação de usuários a projetos.
- Definição de perfis e permissões.
- Consulta ao histórico individual de participação.

### 5.4. Gestão de projetos

- Cadastro e edição de projetos.
- Definição de responsáveis e participantes.
- Acompanhamento de etapas e progresso.
- Organização das atividades relacionadas.
- Consulta à situação e ao histórico dos projetos.

### 5.5. Gestão de atividades e tarefas

- Cadastro e atribuição de atividades.
- Definição de responsáveis e prazos.
- Atualização de status.
- Acompanhamento de pendências.
- Identificação de tarefas atrasadas ou próximas do vencimento.
- Consulta ao histórico de execução.

### 5.6. Diário de Bordo

- Registro de atividades desenvolvidas.
- Documentação do progresso dos projetos.
- Organização cronológica dos registros.
- Associação dos registros a integrantes, atividades ou projetos.
- Consulta ao histórico de contribuições.

### 5.7. Controle de frequência e participação

- Registro de presença.
- Identificação da modalidade presencial ou remota.
- Consulta ao histórico de frequência.
- Consolidação dos registros por período.
- Disponibilização de informações para acompanhamento da participação.

As regras de registro, validação e cálculo da frequência deverão ser definidas durante a especificação funcional.

### 5.8. Entregas e avaliações

- Registro e acompanhamento de entregas.
- Controle de prazos.
- Consulta à situação das entregas.
- Organização de evidências e documentos.
- Apoio ao acompanhamento do desempenho e da participação acadêmica.
- Possibilidade de integração com critérios de avaliação definidos pelo NITE.

Os critérios avaliativos e a eventual utilização dos registros para composição de avaliações acadêmicas deverão ser formalmente definidos e validados pelos responsáveis.

### 5.9. Notificações e alertas

- Alertas de prazos próximos.
- Avisos sobre atividades pendentes.
- Notificações relacionadas a entregas.
- Sinalização de informações que exigem atenção.
- Possibilidade de ampliação para notificações por outros canais.

### 5.10. Indicadores e relatórios

- Resumo dos projetos em andamento.
- Indicadores de atividades concluídas e pendentes.
- Relatórios de frequência e participação.
- Acompanhamento de entregas por período.
- Informações gerenciais para apoiar a tomada de decisões.

### 5.11. Auditoria e rastreabilidade

- Histórico de alterações relevantes.
- Identificação do usuário responsável por cada operação registrada.
- Registro de datas e horários das alterações.
- Rastreabilidade de informações relacionadas a projetos, atividades e entregas.
- Restrições de acesso aos registros conforme as permissões estabelecidas.

## 6. Tecnologias e ferramentas

A implementação será organizada em etapas. As tecnologias abaixo representam uma proposta inicial de arquitetura e poderão ser ajustadas conforme os requisitos, a infraestrutura disponível e as decisões da equipe.

| Tecnologia ou ferramenta | Finalidade |
|---|---|
| HTML5 | Estruturação das páginas web. |
| CSS3 | Estilização, identidade visual, responsividade e animações. |
| JavaScript | Interações e comportamentos da interface. |
| React (planejado) | Organização de interfaces em componentes, caso adotado no frontend definitivo. |
| Node.js e Express (planejados) | Implementação do backend e das APIs. |
| PostgreSQL ou MySQL (a definir) | Persistência e organização dos dados. |
| Git | Controle de versões do código. |
| GitHub | Hospedagem do repositório, documentação e colaboração. |
| Visual Studio Code | Ambiente de desenvolvimento. |

**Observação:** o protótipo inicial utiliza HTML, CSS e JavaScript em um único arquivo. A adoção das tecnologias planejadas deverá ser confirmada antes da implementação definitiva.

## 7. Arquitetura proposta

A arquitetura prevista segue uma separação entre interface, lógica de negócio e armazenamento de dados.

```text
NITE CONNECT
     |
     v
Frontend Web
HTML + CSS + JavaScript
ou React
     |
     v
API / Backend
Node.js + Express
     |
     v
Banco de Dados
PostgreSQL ou MySQL
```

### Responsabilidades das camadas

- **Frontend:** apresenta as telas, recebe as interações dos usuários e exibe os dados.
- **Backend:** processa solicitações, valida informações, aplica regras de negócio e verifica permissões.
- **Banco de dados:** armazena os dados persistentes do sistema.
- **Controle de versões:** registra a evolução do código e facilita o trabalho colaborativo.

A comunicação entre as camadas deverá ser realizada por interfaces definidas, com validação das solicitações e proteção dos dados.

## 8. Etapas de desenvolvimento

O desenvolvimento será conduzido de forma incremental, permitindo validar os resultados antes de avançar para etapas mais complexas.

1. **Levantamento de requisitos:** identificação das necessidades, funcionalidades, regras e perfis de usuários.
2. **Planejamento técnico:** definição da arquitetura, tecnologias e organização do repositório.
3. **Prototipagem:** criação e validação das telas e dos fluxos de navegação.
4. **Modelagem de dados:** definição das entidades, atributos, relacionamentos e restrições do banco de dados.
5. **Desenvolvimento do frontend:** implementação das interfaces e dos componentes de interação.
6. **Desenvolvimento do backend:** implementação das APIs, autenticação, permissões e regras de negócio.
7. **Integração:** conexão entre interface, backend e banco de dados.
8. **Implementação dos módulos:** desenvolvimento progressivo das funcionalidades priorizadas.
9. **Testes e validação:** verificação dos requisitos, correção de falhas e validação com os usuários.
10. **Implantação:** configuração do ambiente de produção e disponibilização do sistema.
11. **Manutenção e evolução:** correções, melhorias e implementação de novas funcionalidades.

O cronograma detalhado do projeto será utilizado para distribuir as atividades, definir responsáveis, acompanhar dependências e monitorar os prazos.

## 9. Estratégia de implementação incremental

Para reduzir riscos e facilitar a validação, recomenda-se organizar o desenvolvimento em versões.

### Versão inicial — MVP

- Autenticação e controle de acesso.
- Cadastro e gerenciamento de integrantes.
- Cadastro e acompanhamento de projetos.
- Cadastro e acompanhamento de atividades.
- Dashboard com informações essenciais.

### Evolução funcional

- Diário de Bordo.
- Controle de frequência presencial e remota.
- Entregas e acompanhamento de prazos.
- Histórico de participação.
- Notificações e alertas.

### Evolução gerencial

- Indicadores e relatórios.
- Funcionalidades de avaliação.
- Auditoria e rastreabilidade ampliadas.
- Melhorias de acessibilidade, segurança e desempenho.
- Possíveis integrações com ferramentas externas.

A ordem definitiva de implementação deverá respeitar as prioridades e dependências identificadas no cronograma oficial.

## 10. Requisitos não funcionais

Além das funcionalidades, o sistema deverá considerar requisitos de qualidade.

- **Segurança:** proteger informações, credenciais e sessões de acesso.
- **Privacidade:** limitar o tratamento e a exposição de dados pessoais ao necessário.
- **Usabilidade:** oferecer navegação intuitiva e consistente.
- **Responsividade:** adaptar a interface a diferentes tamanhos de tela.
- **Manutenibilidade:** manter o código organizado, documentado e fácil de evoluir.
- **Confiabilidade:** validar operações e preservar a integridade dos dados.
- **Desempenho:** responder às solicitações dentro de tempos adequados ao uso previsto.
- **Rastreabilidade:** manter registros relevantes para acompanhamento e auditoria.
- **Acessibilidade:** considerar boas práticas de navegação por teclado, contraste e identificação de elementos.

## 11. Segurança e proteção de dados

A implementação deverá considerar medidas de segurança desde as primeiras etapas.

- Armazenar senhas utilizando algoritmos apropriados de hash, nunca em texto puro.
- Validar os dados recebidos pelo frontend e pelo backend.
- Aplicar autorização no servidor para operações restritas.
- Proteger sessões e credenciais.
- Utilizar HTTPS no ambiente publicado.
- Configurar variáveis de ambiente para segredos e credenciais.
- Implementar backups e procedimentos de recuperação.
- Limitar o acesso a dados pessoais conforme a necessidade de cada perfil.
- Avaliar os requisitos aplicáveis da Lei Geral de Proteção de Dados Pessoais (LGPD).
- Registrar alterações importantes sem expor informações sensíveis desnecessárias.

## 12. Testes e critérios de qualidade

Os testes serão realizados ao longo do desenvolvimento e antes da implantação.

### Testes previstos

- Testes unitários das funções e regras de negócio.
- Testes de integração entre frontend, API e banco de dados.
- Testes funcionais dos módulos.
- Testes de autenticação e autorização.
- Testes de validação dos dados.
- Testes de responsividade e usabilidade.
- Testes de recuperação de dados e backups, conforme a infraestrutura adotada.

### Critérios de aceitação

Uma funcionalidade será considerada concluída quando:

- Atender aos requisitos definidos.
- Executar corretamente os fluxos previstos.
- Validar os dados de entrada.
- Respeitar os perfis e as permissões.
- Apresentar mensagens adequadas para erros e operações realizadas.
- Passar pelos testes estabelecidos para a funcionalidade.
- Possuir documentação compatível com sua implementação.

## 13. Gerenciamento do desenvolvimento

Para manter a organização e a previsibilidade do projeto, serão adotadas as seguintes práticas:

- Manter o cronograma atualizado.
- Definir responsáveis e prazos para cada atividade.
- Registrar dependências entre tarefas.
- Priorizar funcionalidades de acordo com sua importância.
- Documentar decisões técnicas e mudanças de escopo.
- Acompanhar riscos e reservar tempo para correções.
- Utilizar controle de versões e revisão de código.
- Validar entregas parciais antes de iniciar etapas dependentes.
- Atualizar a documentação conforme o sistema evoluir.

Mudanças que afetem requisitos, arquitetura ou prazos deverão ser avaliadas antes de serem incorporadas ao planejamento.

## 14. Estado atual do projeto

O projeto encontra-se na fase inicial de desenvolvimento, com um protótipo visual navegável destinado a demonstrar a identidade visual, a tela de login e a estrutura conceitual do dashboard.

### O que o protótipo já apresenta

- Interface com identidade visual tecnológica.
- Tela de login demonstrativa.
- Exibição simulada do dashboard.
- Indicadores ilustrativos de projetos, atividades e frequência.
- Representação visual de tarefas, alertas e acessos rápidos.
- Navegação demonstrativa entre seções.
- Layout adaptável a diferentes tamanhos de tela.

### Limitações atuais

- Não possui backend integrado.
- Não possui banco de dados persistente.
- Não realiza autenticação real.
- Não aplica controle de acesso efetivo por perfil.
- Não persiste alterações dos usuários.
- Não envia notificações reais.
- Não implementa integralmente as regras de negócio.
- Utiliza informações fictícias para fins demonstrativos.

Os recursos apresentados na interface não devem ser considerados funcionalidades concluídas até que sejam implementados e validados.

## 15. Como executar localmente

A API requer Java 21 e PostgreSQL. No Windows, o Docker Desktop pode fornecer o PostgreSQL local:

1. Preencha `DB_PASSWORD` no `.env` da raiz do projeto. O arquivo é ignorado pelo Git; não o envie ao GitHub.
2. Inicie o Docker Desktop.
3. Na raiz do projeto, suba o banco:

   ```powershell
   docker compose --env-file .env -f infra/compose.yaml up -d db
   ```

4. Inicie a API:

   ```powershell
   Set-Location apps/api
   .\mvnw.cmd spring-boot:run
   ```

O Spring Boot importa o `.env` da raiz durante a execução a partir de `apps/api`. Para validar a compilação e os testes, execute `.\mvnw.cmd verify` nesse diretório; os testes de integração usam Testcontainers e precisam do Docker Desktop ativo. A API expõe a verificação de saúde em `http://localhost:8080/actuator/health`.

Para parar o banco sem apagar os dados locais, execute na raiz `docker compose --env-file .env -f infra/compose.yaml stop db`.

## 16. Organização sugerida do repositório

A estrutura poderá evoluir conforme as tecnologias definitivas forem escolhidas.

```text
nite-connect/
├── README.md
├── docs/
│   ├── requisitos.md
│   ├── arquitetura.md
│   ├── modelagem-banco.md
│   └── cronograma.md
├── prototype/
│   └── index.html
├── frontend/
│   └── ...
├── backend/
│   └── ...
├── database/
│   └── ...
└── tests/
    └── ...
```

Os diretórios representam uma organização proposta. Eles deverão ser criados conforme a evolução real do projeto, evitando manter pastas vazias ou arquivos que ainda não existem.

## 17. Contribuição e controle de versões

O Git e o GitHub serão utilizados para organizar o histórico de desenvolvimento e facilitar a colaboração.

Fluxo sugerido:

1. Criar uma branch para a funcionalidade ou correção.
2. Implementar e testar as alterações.
3. Registrar mudanças com mensagens de commit claras.
4. Revisar o código e a documentação.
5. Abrir um Pull Request, quando aplicável.
6. Integrar as alterações após a validação.

Exemplos de mensagens de commit:

```text
feat: adicionar cadastro de projetos
feat: implementar controle de frequência
fix: corrigir validação do formulário
docs: atualizar documentação do projeto
test: adicionar testes de autenticação
```

## 18. Resultados esperados

Ao final do desenvolvimento, espera-se disponibilizar uma plataforma que:

- Centralize as informações relevantes do NITE.
- Facilite a organização e o acompanhamento dos projetos.
- Permita monitorar atividades, prazos e entregas.
- Ofereça registros estruturados de participação.
- Melhore a consulta ao histórico dos integrantes.
- Apoie a produção de indicadores e relatórios.
- Reforce a segurança e o controle de acesso às informações.
- Forneça uma base sustentável para manutenção e expansão.

Os resultados serão avaliados considerando os requisitos estabelecidos, o funcionamento das funcionalidades implementadas e a validação com os usuários.

## 19. Considerações finais

O NITE CONNECT propõe uma solução digital para apoiar a gestão e o acompanhamento das atividades desenvolvidas pelo Núcleo de Inovação e Tecnologia.

Seu desenvolvimento será conduzido de forma planejada e incremental, partindo da validação dos requisitos e do protótipo visual até a implementação das funcionalidades, integração dos componentes, testes e implantação.

Mais do que reunir informações em um portal, o objetivo é construir uma ferramenta que favoreça a organização, a transparência, a rastreabilidade e o acompanhamento contínuo dos projetos e da participação dos integrantes.

## 20. Licença

A licença de distribuição e utilização do projeto deverá ser definida pelos responsáveis antes da publicação de uma versão oficial.

---

**NITE CONNECT — Conectando ideias. Impulsionando projetos.**
