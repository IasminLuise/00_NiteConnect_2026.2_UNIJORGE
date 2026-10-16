# NITE Connect — ambiente de staging do Bloco 02

## Objetivo

Preparar uma implantação repetível do backend integrado do Bloco 02 em uma VM
administrada pela instituição, para homologação com Microsoft Entra e dados de
teste. A implantação deve ser separada de produção e manualmente promovida por
um operador, sem executar qualquer operação de produção.

## Decisões de escopo

- O alvo aprovado para este desenho é uma VM/host sob administração institucional.
- A implantação será conteinerizada com Docker Compose, sem dependência de
  provedor cloud específico.
- O backend será o artefato da `main` integrado pela PR #4, identificado por SHA
  imutável. Não haverá deploy automático nem promoção para produção.
- A aplicação usará um PostgreSQL exclusivo para staging, sem cópia de dados de
  produção. Dados funcionais de teste serão sintéticos; as identidades usadas
  para autenticação serão contas de teste aprovadas pela instituição.
- A VM fornecerá o proxy reverso institucional e o certificado HTTPS. O
  contêiner da API ficará publicado somente em loopback; PostgreSQL não terá
  porta publicada no host.
- A configuração de Entra, o segredo do banco e dados usados no bootstrap inicial
  ficarão em arquivo operacional protegido na VM, fora do repositório e dos
  artefatos de build. Nenhum `.env` local será lido, alterado ou copiado.
- A promoção fica manual: CI valida e empacota a imagem; operador transfere o
  artefato para a VM e inicia a implantação após conferir o SHA e os checks.

## Componentes e responsabilidades

1. `apps/api/Dockerfile`: imagem de runtime Java 21 sem toolchain Maven, copiando
   apenas o JAR produzido e validado pelo CI; processo sem root.
2. `apps/api/.dockerignore`: restringe o contexto da imagem e exclui credenciais,
   histórico Git, fontes de teste e arquivos temporários.
3. `.github/workflows/backend-ci.yml`: depois de `mvn verify`, constrói e testa a
   imagem a partir do JAR validado nos PRs e pushes cobertos. Exporta o bundle
   para validar o empacotamento, mas só publica o arquivo Docker identificável
   pelo SHA em push na `main`. Não usa registry nem credenciais de deploy.
4. `infra/compose.staging.yaml`: define API e PostgreSQL, rede privada, volume
   persistente de staging, verificação de saúde do PostgreSQL, reinício após
   falha e bind da API em `127.0.0.1:8080`. PostgreSQL não expõe porta no host.
5. `docs/implementation/staging-bloco-02.md`: registra pré-requisitos da VM,
   obtenção e conferência do artefato, configuração externa, bootstrap único,
   inicialização, verificação, execução da homologação e recuperação do staging.

O banco será dedicado à homologação. Flyway executará V1–V3 na inicialização da
primeira instância da API. A implantação inicia uma única instância durante a
migração; JPA permanece em `ddl-auto: validate`. A API usa `/actuator/health`,
cuja resposta não revela detalhes internos.

## Segredos e autenticação

O operador cria na VM um arquivo de ambiente fora do checkout, acessível apenas
aos administradores operacionais. Ele contém somente os valores necessários à
conexão PostgreSQL e à validação Entra (`DB_PASSWORD`, `ENTRA_ENABLED`,
`ENTRA_TENANT_ID`, `ENTRA_AUDIENCE` e `ENTRA_REQUIRED_SCOPE`). A configuração
efêmera do bootstrap administrativo é fornecida apenas ao processo one-shot de
bootstrap e removida antes da execução normal. Tokens, senhas e identificadores
pessoais não entram no repositório, nos logs de deploy, no artefato ou neste
documento.

O estágio exige uma identidade administrativa de teste provisionada como
`GESTOR_GERAL` e identidades institucionais de teste para os demais perfis. O
login é feito pelo fluxo institucional aprovado, por exemplo via cliente OAuth
de teste/Postman; a API valida access tokens e não fornece tela de login neste
bloco. Conta ou token não será criado por código de infraestrutura.

## Implantação e verificação

1. Confirmar que a execução de CI pertence ao SHA integrado na `main` e que todos
   os checks requeridos terminaram com sucesso.
2. Baixar o arquivo da imagem Docker daquela execução, verificar o SHA documentado e
   transferi-lo à VM por canal institucional autenticado.
3. Confirmar Docker Engine/Compose e arquitetura compatível com a imagem. Criar
   o arquivo operacional de ambiente com permissões restritas; conferir que o
   proxy HTTPS encaminhará somente para o bind local da API.
4. Criar o banco vazio do estágio e iniciar uma instância. Confirmar no log de
   inicialização que Flyway aplicou V1, V2 e V3 e que a aplicação iniciou sem
   erro. Nenhum dado real de produção será importado.
5. Executar o bootstrap inicial uma única vez com a identidade de teste
   previamente autorizada pela instituição; remover imediatamente as opções de
   bootstrap e iniciar a API no modo normal.
6. Verificar healthcheck pela rota interna e pelo domínio HTTPS, autenticação
   Entra, mapeamento de identidade e papel. Rodar a matriz T01–T24 do Bloco 02
   com perfis representativos e registrar resultado, SHA, versão Flyway,
   executor e aceite do responsável institucional.

O aceite requer evidências de execução; ter CI verde ou testes automatizados
aprovados não conclui a homologação institucional. Os testes manuais devem usar
valores sintéticos, salvo as identidades de teste explicitamente autorizadas.

## Recuperação e limites

Esta especificação não autoriza criar VM, DNS, certificados, contas Entra, custos
de provedor, banco externo ou recursos de produção. A instituição precisa
informar host/arquitetura, procedimento de acesso, DNS/proxy TLS, janela, operador
e responsável pelo aceite antes da implantação real.

Nenhuma migração Flyway aplicada será removida ou reescrita. Se a implantação de
staging falhar, parar a API e diagnosticar mantendo o volume intacto. A volta à
versão anterior do contêiner é permitida apenas se compatível com o schema
existente; caso contrário, restaurar o snapshot de staging ou reconstruir o banco
de teste conforme o operador institucional. Não executar downgrade ou reset
automático.

O host e a imagem inicial são escolhas de staging de baixa disponibilidade; não
definem arquitetura de produção nem uma política de backup institucional.

## Evidências de contexto

- Backend Java 21 / Spring Boot 4.1.1 e PostgreSQL 17; o CI executa `mvn verify`.
- No início deste trabalho, o Compose continha PostgreSQL local e serviço de
  Testcontainers, sem API de runtime; não havia Dockerfile, registry ou deploy.
- A API mantém deny-all enquanto Entra estiver desabilitado, expõe apenas o
  healthcheck e aplica Flyway automaticamente na inicialização.
- A homologação local do Bloco 01 não comprova homologação do Bloco 02 em VM.
- O aceite depende de host, proxy, parâmetros institucionais e testadores, ainda
  não fornecidos; esta especificação não presume seus valores.
