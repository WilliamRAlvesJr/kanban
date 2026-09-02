## Why

O kanban ainda não tem nenhuma noção de quem usa o sistema: não existe usuário, nem persistência de qualquer tipo. Antes de modelar board, coluna ou card, é preciso ter uma identidade para associar a esses dados. Esta change cria essa base: a conta de usuário, com senha guardada em hash, e o primeiro acesso a banco do projeto.

## What Changes

- Nova tabela `accounts` com `id` (uuid), `email`, `display_name` e `password_hash`.
- `POST /accounts` cadastra uma conta. Recebe email, nome de exibição e senha; devolve 201 com os dados públicos da conta (sem senha) e 409 quando o email já existe.
- `GET /accounts/{id}` devolve os dados públicos de uma conta, ou 404.
- A senha nunca é guardada em texto puro: a service aplica `BCryptPasswordEncoder` antes de persistir, e o hash nunca sai em resposta de API.
- O email é normalizado para minúsculo na service, e o banco recusa qualquer valor não normalizado por constraint. A unicidade é garantida pelo banco, não por consulta prévia.
- Primeira infraestrutura de persistência do projeto: Postgres em desenvolvimento, JPA para acesso, Flyway para o schema e Testcontainers nos testes, rodando o mesmo Postgres.

Fora de escopo nesta change: login, sessão, emissão de token, proteção de rotas e desativação de conta (`disabled_at`). Cada um vira spec própria.

## Capabilities

### New Capabilities
- `account-management`: cadastro e consulta de contas de usuário, incluindo normalização de email, unicidade e armazenamento seguro de senha.

### Modified Capabilities

Nenhuma. O projeto ainda não tem specs.

## Impact

- **Código novo**: pacote `com.william.kanban.account`, organizado por feature, com controller, service, repository e entidade.
- **API**: dois endpoints novos sob `/accounts`. Ambos entram automaticamente no OpenAPI já exposto pelo springdoc.
- **Dependências**: `spring-boot-starter-data-jpa`, driver do Postgres, `flyway-core` (mais `flyway-database-postgresql`) e `spring-security-crypto`. Em escopo de teste, `spring-boot-testcontainers`, `org.testcontainers:postgresql` e `org.testcontainers:junit-jupiter`, com versões vindas do BOM do parent. O starter completo de segurança fica de fora de propósito: ele ligaria a filter chain padrão, que passaria a exigir login em `/v3/api-docs` e `/swagger-ui`, hoje abertos, e bloquearia o `POST /accounts` por CSRF.
- **Configuração**: `application.properties` passa a exigir dados de conexão do Postgres. Os testes recebem a conexão de um container declarado com `@ServiceConnection`, sem URL fixa.
- **Testes existentes**: só resta `KanbanApplicationTests`, que é `@SpringBootTest` e passa a subir o container assim que houver JPA no classpath, o que torna o Docker requisito de `./mvnw test`.
- **Migrations**: primeira migration do projeto, `V1__create_accounts.sql`, escrita para Postgres e exercitada pelos testes no mesmo banco usado em desenvolvimento.
