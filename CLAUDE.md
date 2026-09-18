# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Estado do projeto

A capability `account-management` está implementada nos pacotes por camada: `AccountController` em `controller`, `AccountService` em `service`, `AccountMapper` em `mapper`, `AccountRepository` em `repository`, `Account` em `entity`, `AccountNotFoundException` em `exception` e os records `CreateAccountRequest`, `AccountResponse` e `AccountSummary` em `dto.account`. `POST /accounts` responde 201 com `Location: /accounts/me` e os links `self` e `login`, grava o email em minúsculas e a senha em hash BCrypt, e responde 400 na entrada inválida e 409 no email repetido; `GET /accounts/me` responde 200 com a conta do token e os links `self` e `projects`. `AccountService` devolve só records de `dto.account`; `auth` e `project` chamam dele `authenticate`, `findIdByEmail` e `summariesOf`, e só ele lê o `password_hash`.

A capability `authentication` está implementada. O pacote `com.william.kanban.auth` tem `AuthToken`, `AuthTokenRepository`, `AuthService`, `AuthController`, `InvalidCredentialsException`, `BearerAuthenticationFilter`, `ProblemDetailAuthenticationEntryPoint`, `SecurityConfig`, `OpenApiSecurityConfig` e os records `LoginRequest`, `LoginResponse` e `IssuedToken`. `POST /auth/login` responde 201 com um token opaco de 256 bits e os links `me` e `logout`, sem `Location`; o token fica gravado em `auth_tokens` só como hash SHA-256, 400 na entrada inválida e 401 na credencial errada, com o mesmo corpo para senha errada e email inexistente; `POST /auth/logout` apaga a linha do token da própria chamada e responde 204. As rotas abertas são `GET /`, `POST /accounts`, `POST /auth/login` e a documentação OpenAPI; toda outra exige `Authorization: Bearer` e responde 401 em `ProblemDetail`.

A capability `project-management` está implementada. O pacote `com.william.kanban.project` tem `Project`, `ProjectRepository`, `ProjectService`, `ProjectController`, `ProjectModelAssembler`, `ProjectNotFoundException`, `ProjectLimits` e os records `CreateProjectRequest`, `UpdateProjectRequest` e `ProjectResponse`. `POST /projects` responde 201 com o dono tirado do token; `GET /projects` lista, do mais recente para o mais antigo, os projetos de que a conta é dona e aqueles em que ela é membro com `view_project`, com o filtro opcional `archived`; `GET /projects/{projectId}` responde 200; `PUT /projects/{projectId}` substitui `name` e `description`; `POST /projects/{projectId}/archive` e `POST /projects/{projectId}/restore` carimbam e limpam `archived_at` sem tocar nos quadros, e a operação repetida não altera o projeto. Entrada inválida responde 400, projeto de que a conta não é dona nem membro responde 404 com o mesmo corpo de projeto inexistente, e membro sem a permissão do endpoint responde 403. `ProjectService` é pública, e só `isOwned`, `requireOwned` e `requireAccess` saem dela; `requireAccess` devolve o `ProjectAccess` da conta, e os assemblers montam os links a partir dele.

A capability `project-membership` está implementada, no pacote `com.william.kanban.project`: `ProjectMember`, `ProjectMemberRepository`, `ProjectMemberService`, `ProjectMemberController`, `ProjectMemberModelAssembler`, `ProjectPermission`, `ProjectMemberNotFoundException`, `ProjectAccessDeniedException` e os records `ProjectAccess`, `ProjectView`, `ProjectMemberView`, `ProjectMembers`, `AddProjectMemberRequest`, `UpdateProjectMemberPermissionsRequest` e `ProjectMemberResponse`. Membro é a conta vinculada ao projeto em `project_members`, e cada permissão dele é uma linha de `project_member_permissions` com o nome da constante de `ProjectPermission`; no JSON a permissão sai em minúsculas, e as constantes seguem a ordem alfabética desse valor, que é a ordem de `permissions` na resposta. A dona não é membro e atende todo endpoint sem permissão. `POST /projects/{projectId}/members` recebe `email` e responde 204 em toda entrada válida, gravando por `INSERT ... ON CONFLICT DO NOTHING` só a conta existente que não é a dona; `GET /projects/{projectId}/members` e `GET /projects/{projectId}/members/{memberId}` trazem `email` e `display_name` por `AccountService.summariesOf`; `PUT .../{memberId}/permissions` substitui o conjunto e `DELETE .../{memberId}` responde 204. A permissão é conferida antes da busca do `memberId`, e membro de outro projeto responde 404 de membro. A coleção `permissions` é lazy, e todo finder que a lê leva `@EntityGraph`.

A capability `board-management` está implementada. O pacote `com.william.kanban.board` tem `Board`, `BoardRepository`, `BoardService`, `BoardController`, `BoardModelAssembler`, `BoardNotFoundException`, `BoardLimits` e os records `CreateBoardRequest`, `UpdateBoardRequest`, `MoveBoardRequest` e `BoardResponse`. O quadro pertence a um projeto pela coluna `project_id`, e à conta dona desse projeto. `POST /projects/{projectId}/boards` responde 201 à dona e ao membro com `add_boards`; `GET /projects/{projectId}/boards` lista os quadros do projeto do mais recente para o mais antigo, com o filtro opcional `archived`, à dona e ao membro com `view_boards`; `GET /boards/{id}` responde 200; `PUT /boards/{id}` substitui `name` e `description`; `POST /boards/{id}/archive` e `POST /boards/{id}/restore` carimbam e limpam `archived_at`, e a operação repetida não altera o quadro; `POST /boards/{id}/move` grava o `project_id` do corpo e responde 404 de projeto quando o destino não é da conta. Entrada inválida responde 400. Toda leitura por id busca o quadro por `findById` ou pela variante com trava pessimista `findWithLockById` e confere o dono com `ProjectService.isOwned`, então quadro de outra conta responde 404 com o mesmo corpo de quadro inexistente, inclusive para membro do projeto; o destino do `move` também só aceita projeto da conta. `created_at`, `updated_at` e `archived_at` vêm do relógio da JVM, e as colunas de tempo de `projects` e `boards` não têm `DEFAULT`. `BoardService` é pública, e só `requireOwned` e `lockOwned` saem dela.

A capability `lane-management` está implementada. O pacote `com.william.kanban.lane` tem `Lane`, `LaneRepository`, `LaneService`, `LaneController`, `LaneModelAssembler`, `LaneNotFoundException`, `LaneOrderMismatchException`, `LaneLimits` e os records `CreateLaneRequest`, `UpdateLaneRequest`, `ReorderLanesRequest` e `LaneResponse`. `POST /boards/{boardId}/lanes` cria a lane na última posição; `GET /boards/{boardId}/lanes` lista as ativas por `position` e depois as arquivadas da mais recente para a mais antiga, com o filtro opcional `archived`; `GET /boards/{boardId}/lanes/{laneId}` responde 200 com a lane, ativa ou arquivada, e 404 de lane quando ela não é do quadro da URL; `PUT /boards/{boardId}/lanes/{laneId}` substitui `name`; `POST .../archive` grava `null` em `position` e desce as ativas acima dela, e `POST .../restore` põe a lane no fim; `PUT /boards/{boardId}/lanes/order` recebe em `lane_ids` todas as ativas na nova ordem e responde 409 quando o conjunto difere. As ativas de um quadro têm `position` de 0 a n - 1: a restrição única `(board_id, position)` é `DEFERRABLE`, e um `CHECK` exige `position` nula exatamente na lane arquivada, então `archived_at` e `position` mudam no mesmo `UPDATE`. Toda escrita de `LaneService` começa por `BoardService.lockOwned`, que trava a linha do quadro até o fim da transação; a listagem e a consulta usam `requireOwned`. `updated_at` muda com `name` e `archived_at`, nunca com `position`: a posição de lane que não é o alvo da requisição só muda por query nativa. Não há exclusão de lane por endpoint.

A capability `api-hypermedia` está implementada. Toda resposta de sucesso com corpo sai em HAL, `application/hal+json`, com `href` relativo à raiz. As listagens trazem os itens em `_embedded.projects`, `_embedded.boards`, `_embedded.lanes` ou `_embedded.members`, com array vazio quando não há item, e o `self` da listagem não leva query string. Toda escrita de projeto, quadro e lane e o `PUT .../permissions` de membro respondem só com `_links.self`, e a criação leva `Location` igual ao `self`. Cada projeto, quadro, lane e membro traz `self` e um link por endpoint que opera sobre ele e que a conta pode chamar, com `archive` só no recurso ativo e `restore` só no arquivado. O `ModelAssembler` de cada pacote monta esses links, e `RootController`, em `com.william.kanban.shared`, responde `GET /` com links que dependem de a requisição trazer token válido.

O `GlobalExceptionHandler`, em `com.william.kanban.shared`, converte essas exceções em `ProblemDetail`, e o `OpenApiResponsesConfig`, no mesmo pacote, documenta essas respostas no OpenAPI. Card está por fazer.

## Stack

- Java 21, Spring Boot 4.1.1 (parent POM), Maven via wrapper
- `spring-boot-starter-webmvc` para REST, `spring-boot-starter-hateoas` para as respostas em HAL e `springdoc-openapi-starter-webmvc-ui` 3.1.0 para OpenAPI e Swagger UI
- Persistência em Postgres com `spring-boot-starter-data-jpa`; schema versionado por Flyway em `src/main/resources/db/migration`, com `spring.jpa.hibernate.ddl-auto=validate`
- Em Spring Boot 4 a autoconfiguração de cada integração vem em módulo próprio: `flyway-core` sozinho não migra nada sem `org.springframework.boot:spring-boot-flyway`
- `spring-boot-starter-validation` para as anotações de Bean Validation e `spring-boot-starter-security` pelo `BCryptPasswordEncoder` e pela cadeia de filtros
- `kanban.auth.token-ttl` fica no `application.properties`, com default `24h` lido como `Duration`; prazo de sessão não entra nas variáveis de ambiente, que derrubam a subida quando faltam
- `KanbanApplication` exclui `UserDetailsServiceAutoConfiguration`: desligar `httpBasic` e `formLogin` não impede a subida de gerar o usuário `user` com senha aleatória
- Testes: `spring-boot-starter-test`, `spring-boot-starter-webmvc-test` (JUnit 5) e Testcontainers, que sobem um `postgres:17-alpine` por `TestcontainersConfiguration`
- Lombok opcional, pelo `@With` dos records de requisição que os testes reaproveitam: o processador fica em `annotationProcessorPaths` do `maven-compiler-plugin`, sem o qual o javac do JDK 26 ignora a anotação, e o `spring-boot-maven-plugin` o exclui do jar. O `lombok.config` marca o código gerado com `@lombok.Generated`, que JaCoCo e PIT ignoram
- Teste de mutação pelo `pitest-maven` 1.30.0 com `pitest-junit5-plugin` 1.2.3, fora do ciclo padrão: `./mvnw clean test-compile org.pitest:pitest-maven:mutationCoverage` gera `target/pit-reports/` e reprova abaixo de 80% de mutantes mortos. O `clean` descarta as classes que o compilador do Eclipse, usado pelo VS Code, grava em `target/classes`: elas geram outro conjunto de mutantes, e o número de mortos deixa de ser comparável
- Cobertura pelo `jacoco-maven-plugin`: a fase `test` gera o relatório em `target/site/jacoco/` e roda o `check`, que reprova o build abaixo de 80% de instrução ou de branch; `KanbanApplication` fica fora da medição
- Regras de dependência entre camadas pelo `archunit-junit5` 1.5.0, no `ArchitectureTest`, que roda no `./mvnw test`
- Análise estática pelo SonarQube Cloud, organização `williamralvesjr`, projeto `WilliamRAlvesJr_kanban`, na análise automática que roda a cada push no GitHub. O build não tem scanner: com a análise automática ligada, o SonarQube Cloud recusa análise vinda de CI. A análise automática não importa cobertura, que fica só no JaCoCo
- Análise de vulnerabilidade pelo Snyk CLI, instalado fora do repositório em `C:\Desenv\snyk`. O `.snyk` exclui `src/test/**` do Snyk Code, e o `pom.xml` fixa `jackson-bom.version` e `jackson-2-bom.version` num patch acima do que o parent traz

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

O `snyk test` chama `mvnw.cmd` sem caminho, então o diretório do projeto entra no `PATH`:

```bash
export SNYK_TOKEN=<token>
snyk code test                  # análise estática
PATH="$PWD:$PATH" snyk test     # dependências
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

## Convenções

- Pacote raiz `com.william.kanban`
- Código de conta organizado por responsabilidade, em pacotes sob a raiz: `controller`, `service`, `mapper`, `repository`, `entity`, `dto` com um subpacote por feature, como `dto.account`, e `exception`. `auth`, `project`, `board`, `lane` e `shared` são pacotes por feature
- O service devolve record de `dto` e nunca entidade. Toda conversão entre entidade e record passa pelo mapper, chamado só pelo service; normalização e hash ficam no service, e o mapper só copia valores
- `ArchitectureTest`, em `com.william.kanban` de `src/test`, analisa só as classes de `src/main` e falha quando `controller` acessa camada fora de `service` e `dto`; `service`, camada fora de `repository`, `mapper`, `entity`, `dto` e `exception`; `mapper`, camada fora de `entity` e `dto`; classe fora de `service`, `repository` e `mapper` depende de `entity` ou `repository`; ou classe que não é `AccountService` chama `Account.getPasswordHash`
- Os pacotes de `src/test` espelham os de `src/main`, exceto `schema`: teste de restrição do banco fica em `schema`, uma classe por tabela, como `AccountsTableTest`, e grava a linha por `JdbcTemplate`
- `pom.xml` mantém `<license>`, `<developers>` e `<scm>` vazios de propósito, para anular a herança do parent POM
- `.openspec-ui/`, `.claude/tmp/` e `roteiros/` estão no `.gitignore` (estado local da UI do OpenSpec, temporários do Claude Code e material das vídeo-aulas)
- Código em inglês: pacote, classe, método, variável, coluna, tabela, endpoint e campo de JSON
- Campo de JSON em snake_case e campo Java em camelCase: o record leva `@JsonProperty("display_name")` onde o nome tem mais de uma palavra, e a anotação vale tanto para o Jackson 3 da aplicação quanto para o schema que o springdoc gera com Jackson 2
- Record com anotação em algum componente, e método com anotação em algum parâmetro, abrem a lista numa linha própria, separam os itens por linha em branco, com cada anotação numa linha acima do tipo, e fecham com uma linha em branco e o `) {` sozinho na indentação da declaração; constante usada em anotação entra por import estático
- Resposta só com links é `LinksModel`, de `com.william.kanban.shared`, e listagem é `CollectionModel<Object>`: método não devolve tipo com curinga, que o Sonar aponta pela `java:S1452`
- Link é `Link.of` com caminho literal relativo à raiz: `WebMvcLinkBuilder` grava esquema e host no `href`. Listagem vazia passa por `EmbeddedWrappers.emptyCollectionOf`, porque `CollectionModel` sem item omite `_embedded`
- Criação devolve `ResponseEntity.created` e mantém `@ResponseStatus(HttpStatus.CREATED)`: sem a anotação o springdoc documenta `200`. O schema do `EntityModel<XResponse>` aparece no OpenAPI como `EntityModelXResponse`
- Controller não leva anotação de OpenAPI: o `OpenApiResponsesConfig` documenta `400` para parâmetro com `@Valid` ou `@RequestParam`, `401` para parâmetro com `@AuthenticationPrincipal` ou do tipo `Authentication`, `403` para `@PathVariable` de nome `projectId` e `404` para `@PathVariable`. Resposta que não sai da assinatura, como o `409` de `POST /accounts`, entra no `OpenApiCustomizer` da mesma classe
- `ApiClient`, de `com.william.kanban.support` em `src/test`, envia a requisição do teste de API e confere a resposta sem exceção checada: `api.post(path).withToken(token).withBody(body).perform()` devolve a resposta, e os métodos `expect...` conferem status, `Location`, `_links` e campos. Cadeia sem `perform()` não envia nada. Cada classe de teste cria o seu no `@BeforeEach` a partir do `MockMvc`: registrado como bean por `@Import`, ele muda a chave do cache de contexto e sobe outro container
- `AccountFixture`, de `com.william.kanban.support`, cria conta por `createAccount` e `createAna` e emite token por `tokenOf`, pela API, e traz as constantes `ANA` e `ANA_LOGIN`. `ApiPaths`, no mesmo pacote, traz os caminhos que mais de um teste usa, e `TableRows` conta as linhas de uma tabela por `count`. `AccountFixture` e `TableRows` não são beans: o `@BeforeEach` cria as duas depois do `ApiClient`, nos campos `accounts` e `rows`
- Em todo o código, `src/main` e `src/test`, nenhuma linha passa de 90 caracteres, com tab contando 4, e variável local inicializada na declaração usa `var`. Cadeia de chamadas quebrada em linhas põe uma chamada por linha, um tab adiante do início da cadeia. Argumento, elemento de array e bloco de texto que ocupam várias linhas vão um tab adiante da linha que os abre: a lista fecha com `)` ou `}` sozinho na indentação dessa linha, e o bloco de texto fecha com `"""` na indentação do `"""` que o abre
- Corpo de requisição em teste é o record de requisição de `src/main`, como `CreateAccountRequest`, com o valor padrão numa constante do teste, como `ANA`. O record leva `@With`, que gera `withEmail` e os demais, e `@JsonInclude(NON_NULL)`, com o qual `with...(null)` tira o campo do JSON
- Prosa em português do Brasil, com acentuação correta: artefatos do OpenSpec, documentos do projeto, comentário e Javadoc, mensagem de commit
- O `CLAUDE.md` descreve o estado atual: stack, comandos, convenções, estrutura e fluxo. Não recebe motivo de mudança, comparação com o que era antes nem histórico
