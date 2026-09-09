## 1. Dependências e configuração

- [x] 1.1 Adicionar ao `pom.xml` as dependências de runtime, sem versão explícita; verificar com `./mvnw -q dependency:resolve`

```diff
+++ b/pom.xml
   <dependencies>
+    <dependency>
+      <groupId>org.springframework.boot</groupId>
+      <artifactId>spring-boot-starter-data-jpa</artifactId>
+    </dependency>
+    <dependency>
+      <groupId>org.springframework.boot</groupId>
+      <artifactId>spring-boot-starter-validation</artifactId>
+    </dependency>
+    <dependency>
+      <groupId>org.springframework.boot</groupId>
+      <artifactId>spring-boot-flyway</artifactId>
+    </dependency>
+    <dependency>
+      <groupId>org.flywaydb</groupId>
+      <artifactId>flyway-core</artifactId>
+    </dependency>
+    <dependency>
+      <groupId>org.flywaydb</groupId>
+      <artifactId>flyway-database-postgresql</artifactId>
+    </dependency>
+    <dependency>
+      <groupId>org.springframework.security</groupId>
+      <artifactId>spring-security-crypto</artifactId>
+    </dependency>
+    <dependency>
+      <groupId>org.postgresql</groupId>
+      <artifactId>postgresql</artifactId>
+      <scope>runtime</scope>
+    </dependency>
     ...
   </dependencies>
```

- [x] 1.2 Adicionar as dependências de teste; verificar que `./mvnw -q dependency:resolve` continua passando

```diff
+++ b/pom.xml
+    <dependency>
+      <groupId>org.springframework.boot</groupId>
+      <artifactId>spring-boot-testcontainers</artifactId>
+      <scope>test</scope>
+    </dependency>
+    <dependency>
+      <groupId>org.testcontainers</groupId>
+      <artifactId>testcontainers-postgresql</artifactId>
+      <scope>test</scope>
+    </dependency>
+    <dependency>
+      <groupId>org.testcontainers</groupId>
+      <artifactId>testcontainers-junit-jupiter</artifactId>
+      <scope>test</scope>
+    </dependency>
```

- [x] 1.3 Preencher `application.properties` com conexão por variável de ambiente, sem default; verificar que `./mvnw spring-boot:run` sem as variáveis falha na subida ao criar o `dataSource`

```diff
+++ b/src/main/resources/application.properties
+spring.datasource.url=${KANBAN_DB_URL}
+spring.datasource.username=${KANBAN_DB_USER}
+spring.datasource.password=${KANBAN_DB_PASSWORD}
+spring.jpa.hibernate.ddl-auto=validate
```

- [x] 1.4 Criar `TestcontainersConfiguration` em `src/test/java` e importá-la em `KanbanApplicationTests`; verificar que `./mvnw test -Dtest=KanbanApplicationTests` sobe o container e o contexto

```diff
+++ b/src/test/java/com/william/kanban/TestcontainersConfiguration.java
+@TestConfiguration(proxyBeanMethods = false)
+class TestcontainersConfiguration {
+    @Bean
+    @ServiceConnection
+    PostgreSQLContainer postgres() { ... }
+}
+++ b/src/test/java/com/william/kanban/KanbanApplicationTests.java
+@Import(TestcontainersConfiguration.class)
 @SpringBootTest
 class KanbanApplicationTests { ... }
```

## 2. Ciclo: cadastro cria a conta

- [x] 2.1 Criar `AccountApiTest` (`@SpringBootTest` com `MockMvc`, importando `TestcontainersConfiguration`, limpando `accounts` antes de cada teste) com o teste de `POST /accounts` que espera 201, corpo com `id`, `email` e `displayName`, e header `Location`; verificar que falha, porque a tabela `accounts` ainda não existe

```diff
+++ b/src/test/java/com/william/kanban/account/AccountApiTest.java
+@SpringBootTest
+@AutoConfigureMockMvc
+@Import(TestcontainersConfiguration.class)
+class AccountApiTest {
+    @BeforeEach
+    void cleanUp() { ... delete from accounts ... }
+
+    @Test
+    void createsAccount() { ... expect(status().isCreated()) ... }
+}
```

- [x] 2.2 Criar a migration e a entidade; verificar que `contextLoads` passa com `ddl-auto=validate`, o que prova o mapeamento coerente com a tabela

```diff
+++ b/src/main/resources/db/migration/V1__create_accounts.sql
+CREATE TABLE accounts (
+    id            uuid        PRIMARY KEY,
+    email         text        NOT NULL CHECK (email = lower(email)),
+    display_name  text        NOT NULL,
+    password_hash text        NOT NULL,
+    created_at    timestamptz NOT NULL DEFAULT now()
+);
+CREATE UNIQUE INDEX ux_accounts_email ON accounts (email);
+++ b/src/main/java/com/william/kanban/account/Account.java
+@Entity
+@Table(name = "accounts")
+class Account {
+    @Id
+    private UUID id;
+    private String email;
+    private String displayName;
+    private String passwordHash;
+    private OffsetDateTime createdAt;
+
+    Account(String email, String displayName, String passwordHash) {
+        this.id = UUID.randomUUID();
+        ...
+    }
+}
```

- [x] 2.3 Criar `AccountRepository`, os records, `AccountService` e `AccountController` com o caminho mínimo de criação, gravando o valor recebido direto em `password_hash`; verificar que o teste 2.1 fica verde

```diff
+++ b/src/main/java/com/william/kanban/account/AccountRepository.java
+interface AccountRepository extends JpaRepository<Account, UUID> {}
+++ b/src/main/java/com/william/kanban/account/CreateAccountRequest.java
+record CreateAccountRequest(String email, String displayName, String password) {}
+++ b/src/main/java/com/william/kanban/account/AccountResponse.java
+record AccountResponse(UUID id, String email, String displayName) {}
+++ b/src/main/java/com/william/kanban/account/AccountService.java
+@Service
+class AccountService {
+    Account create(String email, String displayName, String password) {
+        return repository.save(new Account(email, displayName, password));
+    }
+}
+++ b/src/main/java/com/william/kanban/account/AccountController.java
+@RestController
+@RequestMapping("/accounts")
+class AccountController {
+    @PostMapping
+    ResponseEntity<AccountResponse> create(@RequestBody CreateAccountRequest request) { ... 201 + Location ... }
+}
```

## 3. Ciclo: normalização do email e hash da senha

- [x] 3.1 Acrescentar o teste que envia o email `"Ana@Exemplo.com"` e espera `"ana@exemplo.com"` na coluna `email` e na resposta; verificar que falha porque o email é gravado como veio
- [x] 3.2 Normalizar o email no `AccountService`; verificar que o teste 3.1 fica verde

```diff
+++ b/src/main/java/com/william/kanban/account/AccountService.java
     Account create(String email, String displayName, String password) {
-        return repository.save(new Account(email, displayName, password));
+        return repository.save(new Account(email.toLowerCase(Locale.ROOT), displayName, password));
     }
```

- [x] 3.3 Acrescentar os testes que exigem `password_hash` validando contra a senha original e diferente dela, e que a resposta não traz campo de senha; verificar que o primeiro falha com a senha gravada em texto puro
- [x] 3.4 Aplicar `BCryptPasswordEncoder` no `AccountService`; verificar que os testes 3.3 ficam verdes

```diff
+++ b/src/main/java/com/william/kanban/account/AccountService.java
+    private final PasswordEncoder encoder = new BCryptPasswordEncoder();
+
     Account create(String email, String displayName, String password) {
-        return repository.save(new Account(email.toLowerCase(Locale.ROOT), displayName, password));
+        return repository.save(new Account(
+            email.toLowerCase(Locale.ROOT), displayName, encoder.encode(password)));
     }
```

- [x] 3.5 Acrescentar o teste que insere email em maiúsculas direto pelo `JdbcTemplate` e espera erro do banco; verificar que passa pelo `CHECK (email = lower(email))` da migration

## 4. Ciclo: validação de entrada

- [x] 4.1 Acrescentar os testes de campo ausente e de email em formato inválido, ambos esperando 400 e nenhuma conta criada; verificar que falham com 500
- [x] 4.2 Anotar `CreateAccountRequest` e o parâmetro do controller, e criar o `GlobalExceptionHandler`; verificar que os testes 4.1 ficam verdes

```diff
+++ b/src/main/java/com/william/kanban/account/CreateAccountRequest.java
-record CreateAccountRequest(String email, String displayName, String password) {}
+record CreateAccountRequest(
+    @NotBlank @Email String email,
+    @NotBlank String displayName,
+    @NotBlank String password) {}
+++ b/src/main/java/com/william/kanban/account/AccountController.java
-    ResponseEntity<AccountResponse> create(@RequestBody CreateAccountRequest request)
+    ResponseEntity<AccountResponse> create(@Valid @RequestBody CreateAccountRequest request)
+++ b/src/main/java/com/william/kanban/shared/GlobalExceptionHandler.java
+@RestControllerAdvice
+class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
+}
```

## 5. Ciclo: unicidade do email

- [x] 5.1 Acrescentar os testes de email repetido e de email repetido com outra caixa, ambos esperando 409; verificar que falham com 500 vindo da violação do índice `ux_accounts_email`
- [x] 5.2 Traduzir a violação em conflito no `GlobalExceptionHandler`, restrita ao índice único de email; verificar que os testes 5.1 ficam verdes

```diff
+++ b/src/main/java/com/william/kanban/shared/GlobalExceptionHandler.java
+    @ExceptionHandler(DataIntegrityViolationException.class)
+    ProblemDetail handleConflict(DataIntegrityViolationException e) { ... ux_accounts_email -> 409 ... }
```

## 6. Ciclo: consulta por id

- [x] 6.1 Acrescentar o teste de `GET /accounts/{id}` que espera 200 com `id`, `email` e `displayName`; verificar que falha por não existir o endpoint
- [x] 6.2 Criar o `findById` no service e no controller; verificar que o teste 6.1 fica verde

```diff
+++ b/src/main/java/com/william/kanban/account/AccountService.java
+    Account findById(UUID id) {
+        return repository.findById(id).orElseThrow(() -> new AccountNotFoundException(id));
+    }
+++ b/src/main/java/com/william/kanban/account/AccountController.java
+    @GetMapping("/{id}")
+    AccountResponse findById(@PathVariable UUID id) { ... }
```

- [x] 6.3 Acrescentar os testes de id inexistente esperando 404 e de id fora do formato uuid esperando 400; verificar que o primeiro falha com 500 e o segundo já passa pelo `ResponseEntityExceptionHandler`
- [x] 6.4 Tratar `AccountNotFoundException` no `GlobalExceptionHandler`; verificar que o teste 6.3 fica verde

```diff
+++ b/src/main/java/com/william/kanban/shared/GlobalExceptionHandler.java
+    @ExceptionHandler(AccountNotFoundException.class)
+    ProblemDetail handleNotFound(AccountNotFoundException e) { ... 404 ... }
```

## 7. Fechamento

- [x] 7.1 Acrescentar o teste que busca o documento OpenAPI e confirma `POST /accounts` e `GET /accounts/{id}` com seus códigos de resposta; verificar que passa com o que o springdoc já publica
- [x] 7.2 Rodar `./mvnw -q verify` com Docker ativo e verificar que a suíte inteira passa
- [x] 7.3 Registrar em `CLAUDE.md` a stack de persistência, o pré-requisito de Docker para `./mvnw test` e as variáveis de conexão; verificar que o arquivo descreve o estado atual do projeto após esta change
