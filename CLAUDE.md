# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Estado do projeto

A capability `account-management` está implementada. O pacote `com.william.kanban.account` tem `Account`, `AccountRepository`, `AccountService`, `AccountController`, `AccountNotFoundException` e os records `CreateAccountRequest` e `AccountResponse`. `POST /accounts` responde 201 sem header `Location`, com o email em minúsculas e a senha em hash BCrypt, 400 na entrada inválida e 409 no email repetido; `GET /accounts/me` responde 200 com a conta do token. `AccountService` é a única classe pública do pacote, e só `authenticate` sai dele: o `password_hash` não atravessa a fronteira.

A capability `authentication` está implementada. O pacote `com.william.kanban.auth` tem `AuthToken`, `AuthTokenRepository`, `AuthService`, `AuthController`, `InvalidCredentialsException`, `BearerAuthenticationFilter`, `ProblemDetailAuthenticationEntryPoint`, `SecurityConfig`, `OpenApiSecurityConfig` e os records `LoginRequest`, `LoginResponse` e `IssuedToken`. `POST /auth/login` responde 201 com um token opaco de 256 bits, gravado em `auth_tokens` só como hash SHA-256, 400 na entrada inválida e 401 na credencial errada, com o mesmo corpo para senha errada e email inexistente; `POST /auth/logout` apaga a linha do token da própria chamada e responde 204. As rotas abertas são `POST /accounts`, `POST /auth/login` e a documentação OpenAPI; toda outra exige `Authorization: Bearer` e responde 401 em `ProblemDetail`.

A capability `project-management` está implementada. O pacote `com.william.kanban.project` tem `Project`, `ProjectRepository`, `ProjectService`, `ProjectController`, `ProjectNotFoundException`, `ProjectLimits` e os records `CreateProjectRequest`, `UpdateProjectRequest` e `ProjectResponse`. `POST /projects` responde 201 com o dono tirado do token; `GET /projects` lista os projetos da conta do mais recente para o mais antigo, com o filtro opcional `archived`; `GET /projects/{projectId}` responde 200; `PUT /projects/{projectId}` substitui `name` e `description`; `POST /projects/{projectId}/archive` e `POST /projects/{projectId}/restore` carimbam e limpam `archived_at` sem tocar nos quadros, e a operação repetida não altera o projeto. Entrada inválida responde 400, e projeto de outra conta responde 404 com o mesmo corpo de projeto inexistente. `ProjectService` é pública, e só `isOwned` e `requireOwned` saem dela.

A capability `board-management` está implementada. O pacote `com.william.kanban.board` tem `Board`, `BoardRepository`, `BoardService`, `BoardController`, `BoardNotFoundException`, `BoardLimits` e os records `CreateBoardRequest`, `UpdateBoardRequest`, `MoveBoardRequest` e `BoardResponse`. O quadro pertence a um projeto pela coluna `project_id`, e à conta dona desse projeto. `POST /projects/{projectId}/boards` responde 201; `GET /projects/{projectId}/boards` lista os quadros do projeto do mais recente para o mais antigo, com o filtro opcional `archived`; `GET /boards/{id}` responde 200; `PUT /boards/{id}` substitui `name` e `description`; `POST /boards/{id}/archive` e `POST /boards/{id}/restore` carimbam e limpam `archived_at`, e a operação repetida não altera o quadro; `POST /boards/{id}/move` grava o `project_id` do corpo e responde 404 de projeto quando o destino não é da conta. Entrada inválida responde 400. Toda leitura por id busca o quadro por `findById` ou pela variante com trava pessimista `findWithLockById` e confere o dono com `ProjectService.isOwned`, então quadro de outra conta responde 404 com o mesmo corpo de quadro inexistente. `created_at`, `updated_at` e `archived_at` vêm do relógio da JVM, e as colunas de tempo de `projects` e `boards` não têm `DEFAULT`. `BoardService` é pública, e só `requireOwned` e `lockOwned` saem dela.

A capability `lane-management` está implementada. O pacote `com.william.kanban.lane` tem `Lane`, `LaneRepository`, `LaneService`, `LaneController`, `LaneNotFoundException`, `LaneOrderMismatchException`, `LaneLimits` e os records `CreateLaneRequest`, `UpdateLaneRequest`, `ReorderLanesRequest` e `LaneResponse`. `POST /boards/{boardId}/lanes` cria a lane na última posição; `GET /boards/{boardId}/lanes` lista as ativas por `position` e depois as arquivadas da mais recente para a mais antiga, com o filtro opcional `archived`; `PUT /boards/{boardId}/lanes/{laneId}` substitui `name`; `POST .../archive` grava `null` em `position` e desce as ativas acima dela, e `POST .../restore` põe a lane no fim; `PUT /boards/{boardId}/lanes/order` recebe em `lane_ids` todas as ativas na nova ordem e responde 409 quando o conjunto difere. As ativas de um quadro têm `position` de 0 a n - 1: a restrição única `(board_id, position)` é `DEFERRABLE`, e um `CHECK` exige `position` nula exatamente na lane arquivada, então `archived_at` e `position` mudam no mesmo `UPDATE`. Toda escrita de `LaneService` começa por `BoardService.lockOwned`, que trava a linha do quadro até o fim da transação; a listagem usa `requireOwned`. `updated_at` muda com `name` e `archived_at`, nunca com `position`: a posição de lane que não é o alvo da requisição só muda por query nativa. Não há exclusão de lane por endpoint.

O `GlobalExceptionHandler`, em `com.william.kanban.shared`, converte essas exceções em `ProblemDetail`, e o `OpenApiResponsesConfig`, no mesmo pacote, documenta essas respostas no OpenAPI. Card está por fazer.

## Stack

- Java 21, Spring Boot 4.1.1 (parent POM), Maven via wrapper
- `spring-boot-starter-webmvc` para REST e `springdoc-openapi-starter-webmvc-ui` 3.1.0 para OpenAPI e Swagger UI
- Persistência em Postgres com `spring-boot-starter-data-jpa`; schema versionado por Flyway em `src/main/resources/db/migration`, com `spring.jpa.hibernate.ddl-auto=validate`
- Em Spring Boot 4 a autoconfiguração de cada integração vem em módulo próprio: `flyway-core` sozinho não migra nada sem `org.springframework.boot:spring-boot-flyway`
- `spring-boot-starter-validation` para as anotações de Bean Validation e `spring-boot-starter-security` pelo `BCryptPasswordEncoder` e pela cadeia de filtros
- `kanban.auth.token-ttl` fica no `application.properties`, com default `24h` lido como `Duration`; prazo de sessão não entra nas variáveis de ambiente, que derrubam a subida quando faltam
- `KanbanApplication` exclui `UserDetailsServiceAutoConfiguration`: desligar `httpBasic` e `formLogin` não impede a subida de gerar o usuário `user` com senha aleatória
- Testes: `spring-boot-starter-test`, `spring-boot-starter-webmvc-test` (JUnit 5) e Testcontainers, que sobem um `postgres:17-alpine` por `TestcontainersConfiguration`
- Teste de mutação pelo `pitest-maven` 1.30.0 com `pitest-junit5-plugin` 1.2.3, fora do ciclo padrão: `./mvnw test-compile org.pitest:pitest-maven:mutationCoverage` gera `target/pit-reports/` e reprova abaixo de 80% de mutantes mortos
- Cobertura pelo `jacoco-maven-plugin`: a fase `test` gera o relatório em `target/site/jacoco/` e roda o `check`, que reprova o build abaixo de 80% de instrução ou de branch; `KanbanApplication` fica fora da medição
- Análise estática pelo SonarQube Community Build em container, no profile `sonar` do `compose.yaml`, com Postgres próprio, e pelo `sonar-maven-plugin`, só em `pluginManagement`. `sonar.host.url` fica no `pom.xml`: sem ele o scanner envia a análise para o SonarQube Cloud e falha com 403. A cobertura vem do `jacoco.xml` do `verify`, com `KanbanApplication` em `sonar.coverage.exclusions`

## Comandos

Use o wrapper `./mvnw`, no Git Bash: é o shell do projeto, e não há Maven global garantido.
No Git Bash a forma `.\mvnw.cmd` falha com `.mvnw.cmd: command not found`, porque a barra
invertida escapa o `m`.

`./mvnw test` exige Docker: os testes de contexto sobem um Postgres em container.

Antes de `spring-boot:run`, suba o banco de desenvolvimento e exporte a conexão. Sem as três variáveis a aplicação falha ao criar o `dataSource`, sem dizer qual falta.

```bash
docker compose up -d            # kanban-postgres na porta 5432
export KANBAN_DB_URL=jdbc:postgresql://localhost:5432/kanban
export KANBAN_DB_USER=kanban
export KANBAN_DB_PASSWORD=kanban
```

```bash
./mvnw spring-boot:run          # sobe a aplicação
./mvnw test                     # todos os testes
./mvnw test -Dtest=KanbanApplicationTests            # uma classe
./mvnw test -Dtest=KanbanApplicationTests#contextLoads   # um teste
./mvnw test -Djacoco.skip=true  # testes sem medir cobertura
./mvnw clean package            # jar em target/
./mvnw -q verify                # build completo silencioso
```

```bash
docker compose --profile sonar up -d            # SonarQube em http://localhost:9000
export SONAR_TOKEN=<token>                      # token gerado em My Account > Security
./mvnw clean verify sonar:sonar                 # testes, cobertura e análise
docker compose stop sonarqube sonarqube-db      # para o SonarQube
```

Não há linter configurado.

## Fluxo OpenSpec

O repositório é spec-driven via OpenSpec (`@fission-ai/openspec`, schema `spec-driven`). As specs vivem em `openspec/specs/`, as mudanças em andamento em `openspec/changes/`, os templates de change em `openspec/templates/`. O ciclo é: `/opsx:propose` (gera proposal, spec delta, design, tasks) → `/opsx:apply` (implementa as tasks) → `/opsx:verify` → `/opsx:archive`.

O profile instalado é `custom`, com 11 workflows: além do ciclo acima, `explore`, `new`, `continue`, `ff`, `update`, `sync` e `bulk-archive`. Os slash commands ficam em `.claude/commands/opsx/` e as skills em `.claude/skills/openspec-*`, gerados por `openspec update` a partir da lista de workflows na config global (`%APPDATA%/openspec/config.json`) e versionados no repositório.

Regra do fluxo: a fase de proposta **não edita código de projeto**. Ao propor, pare depois dos artefatos de planejamento e espere um novo pedido do usuário para implementar, mesmo que o pedido original diga "construa" ou "corrija".

`openspec/config.yaml` alimenta a geração dos artefatos e é o lugar de mexer no formato deles. Ele não repete stack, comandos nem convenções: aponta para este arquivo.

- `context:` traz as regras de escrita dos artefatos e as de diagrama. Vale para todo artefato gerado.
- `rules.proposal`, `rules.specs`, `rules.tasks` e `rules.design` trazem o que é específico de cada um.
- `rules.tasks` exige TDD: cada comportamento vira dois checkboxes, o do teste que falha e o do código que o faz passar, agrupados por comportamento observável em vez de por camada.
- As regras de escrita cortam racional de decisão, trabalho futuro, estado anterior e hedge. As de diagrama pedem Mermaid no lugar da prosa sempre que o conteúdo for fluxo, sequência, estado ou modelo de dados.

`openspec/agent-harness.json` fixa `autonomyLevel: assisted` e `reviewGate: human-required`.

## Hooks

`.claude/settings.json` liga um hook `PreToolUse` em `Bash`, filtrado por `if: "Bash(git commit *)"`, que roda `.claude/hooks/verificar-claude-md.py`.

O hook roda antes do comando, então nega sempre o `git commit` precedido de `git add` na mesma chamada ou com `-a`/`--all`: o índice ainda não tem o que vai para o commit, e o `git add` precisa vir numa chamada separada.

Fora desse caso, o script lê `git diff --cached`. Libera o commit quando o índice está vazio ou já contém o `CLAUDE.md`; senão nega uma vez, listando os arquivos que vão para o commit, e grava a marca do índice em `.claude/tmp/claude-md-conferido-<sessão>.txt`. A segunda tentativa com o mesmo índice passa, então o gate não entra em laço.

O `CLAUDE.md` descreve o estado atual: stack, comandos, convenções, estrutura e fluxo. Não recebe motivo de mudança, comparação com o que era antes nem histórico.

## Convenções

- Pacote raiz `com.william.kanban`
- `pom.xml` mantém `<license>`, `<developers>` e `<scm>` vazios de propósito, para anular a herança do parent POM
- `.openspec-ui/`, `.claude/tmp/` e `roteiros/` estão no `.gitignore` (estado local da UI do OpenSpec, temporários do Claude Code e material das vídeo-aulas)
- Código em inglês: pacote, classe, método, variável, coluna, tabela, endpoint e campo de JSON
- Campo de JSON em snake_case e campo Java em camelCase: o record leva `@JsonProperty("display_name")` onde o nome tem mais de uma palavra, e a anotação vale tanto para o Jackson 3 da aplicação quanto para o schema que o springdoc gera com Jackson 2
- Record com anotação em algum componente, e método com anotação em algum parâmetro, abrem a lista numa linha própria, separam os itens por linha em branco, com cada anotação numa linha acima do tipo, e fecham com uma linha em branco e o `) {` sozinho na indentação da declaração; constante usada em anotação entra por import estático
- Controller não leva anotação de OpenAPI: o `OpenApiResponsesConfig` documenta `400` para parâmetro com `@Valid` ou `@RequestParam`, `401` para parâmetro com `@AuthenticationPrincipal` ou do tipo `Authentication`, e `404` para `@PathVariable`. Resposta que não sai da assinatura, como o `409` de `POST /accounts`, entra no `OpenApiCustomizer` da mesma classe
- Prosa em português do Brasil, com acentuação correta: artefatos do OpenSpec, documentos do projeto, comentário e Javadoc, mensagem de commit
