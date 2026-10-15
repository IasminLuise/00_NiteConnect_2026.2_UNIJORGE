# Regras específicas da API Spring Boot

**Aplicação:** `br.edu.unijorge.niteconnect.NiteConnectApplication`

- Java 21, Spring Boot 4.1.1, Maven, PostgreSQL, Flyway, Spring Security.
- Priorize código Java legível, nomes com significado e componentes pequenos.
- Módulos novos pertencem a pacotes por domínio (`identity`, `members`, `projects` etc.). Crie-os só quando houver código real.
- Dentro do módulo, use `api`, `application`, `domain` e `infrastructure` apenas quando forem necessários; evite camadas artificiais.
- Exponha DTOs nas APIs; entidades JPA não são contratos HTTP.
- Não use `@Data` indiscriminadamente em entidades JPA; não exponha segredos ou dados sensíveis em logs, erros ou `toString()`.
- Transações no serviço de aplicação; consultas, paginação e índices devem ser intencionais.
- O servidor é a autoridade de autorização, inclusive para documentos, projetos, membros e dados acadêmicos.
- A API começa em modo **deny-all**: só o endpoint de saúde é público. Não abra endpoints sem implementar e testar o mecanismo de proteção.
- OAuth2/OIDC com Microsoft Entra será implementado no Bloco 01; não criar senhas próprias nem autenticação fictícia.
- Migrações em `src/main/resources/db/migration`: append-only, versionadas e testadas com PostgreSQL.
- Testes unitários cobrem regras puras; testes de integração cobrem segurança, Flyway e persistência real.
- Nunca afirmar que um teste passou sem saída real do comando/CI correspondente.

## Comandos esperados

Na pasta `apps/api`:
- Maven disponível: `./mvnw verify` (Linux/macOS) ou `.\mvnw.cmd verify` (Windows).
- Alternativa sem Maven local: `docker compose -f ../../infra/compose.yaml --profile test run --rm api-tests`.
- Para iniciar a API: `./mvnw spring-boot:run` (ou `.\mvnw.cmd` no Windows) após configurar `DB_PASSWORD` e subir o PostgreSQL.
