## 1. POST /boards

- [x] 1.1 Escrever em `BoardApiTest` os três cenários de **Criação de quadro** mais o cenário **Criação preenche updated_at**, e confirmar que falham com `404` na rota
- [x] 1.2 Criar `V3__create_boards.sql` e confirmar que `./mvnw test -Dtest=KanbanApplicationTests` sobe o contexto com `ddl-auto=validate`

```diff
+++ b/src/main/resources/db/migration/V3__create_boards.sql
+CREATE TABLE boards (
+    id          uuid        PRIMARY KEY,
+    owner_id    uuid        NOT NULL REFERENCES accounts (id) ON DELETE CASCADE,
+    name        text        NOT NULL,
+    description text,
+    created_at  timestamptz NOT NULL,
+    updated_at  timestamptz NOT NULL,
+    archived_at timestamptz
+);
+
+CREATE INDEX ix_boards_owner_id ON boards (owner_id);
```

- [x] 1.3 Criar `Board`, com o carimbo de tempo pela aplicação, e confirmar que o contexto sobe com a validação do schema

```diff
+++ b/src/main/java/com/william/kanban/board/Board.java
+@Entity
+@Table(name = "boards")
+class Board {
+
+	@Id
+	private UUID id;
+
+	private UUID ownerId;
+
+	private String name;
+
+	private String description;
+
+	@CreationTimestamp
+	@Column(updatable = false)
+	private OffsetDateTime createdAt;
+
+	@UpdateTimestamp
+	private OffsetDateTime updatedAt;
+
+	private OffsetDateTime archivedAt;
...
```

- [x] 1.4 Criar `BoardRepository`, os records `CreateBoardRequest` e `BoardResponse`, `BoardService.create` e o `POST /boards` em `BoardController`, até os testes de 1.1 passarem
- [x] 1.5 Escrever os quatro cenários de **Validação da entrada de criação**, confirmar que falham com `201`, e adicionar as anotações de Bean Validation com os limites em constantes de `BoardLimits` até passarem

## 2. GET /boards/{id}

- [x] 2.1 Escrever os dois cenários de **Consulta de um quadro** e, de **Isolamento entre donos**, os cenários **Consulta de quadro alheio** e **Quadro inexistente**, e confirmar que falham
- [x] 2.2 Criar `BoardNotFoundException` e ligá-la ao `404` no `GlobalExceptionHandler`

```diff
+++ b/src/main/java/com/william/kanban/shared/GlobalExceptionHandler.java
-	@ExceptionHandler(AccountNotFoundException.class)
-	ProblemDetail handleNotFound(AccountNotFoundException e) {
+	@ExceptionHandler({AccountNotFoundException.class, BoardNotFoundException.class})
+	ProblemDetail handleNotFound(RuntimeException e) {
...
```

- [x] 2.3 Criar `BoardService.findById`, que resolve o quadro por `findByIdAndOwnerId`, e o `GET /boards/{id}`, até os testes de 2.1 passarem

```diff
+++ b/src/main/java/com/william/kanban/board/BoardRepository.java
+	Optional<Board> findByIdAndOwnerId(UUID id, UUID ownerId);
...
```

## 3. GET /boards

- [x] 3.1 Escrever os seis cenários de **Listagem dos quadros da conta**, incluindo a ordem por `created_at` e o `400` do valor inválido, e confirmar que falham
- [x] 3.2 Criar os três finders da listagem, `BoardService.list` e o endpoint, até os testes de 3.1 passarem

```diff
+++ b/src/main/java/com/william/kanban/board/BoardRepository.java
+	List<Board> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);
+
+	List<Board> findByOwnerIdAndArchivedAtIsNullOrderByCreatedAtDesc(UUID ownerId);
+
+	List<Board> findByOwnerIdAndArchivedAtIsNotNullOrderByCreatedAtDesc(UUID ownerId);
...
```

```diff
+++ b/src/main/java/com/william/kanban/board/BoardController.java
+	@GetMapping
+	List<BoardResponse> list(@AuthenticationPrincipal UUID accountId,
+			@RequestParam(required = false) Boolean archived) {
...
```

## 4. PUT /boards/{id}

- [x] 4.1 Escrever os três cenários de **Substituição do quadro** e o cenário **Edição de quadro alheio**, e confirmar que falham
- [x] 4.2 Criar `UpdateBoardRequest`, `BoardService.update` e o `PUT /boards/{id}`, até os testes de 4.1 passarem
- [x] 4.3 Escrever os quatro cenários de **Validação da entrada de substituição**, confirmar que falham, e adicionar `@Valid` e as anotações de Bean Validation com as constantes de `BoardLimits` até passarem
- [x] 4.4 Escrever os dois cenários de alteração de **Carimbo de updated_at**, lendo a coluna por `JdbcTemplate` antes e depois, e confirmar que passam pelo dirty checking do Hibernate sem código novo

## 5. POST /boards/{id}/archive e POST /boards/{id}/restore

- [x] 5.1 Escrever os quatro cenários de **Arquivamento reversível** e os cenários **Arquivamento de quadro alheio** e **Restauração de quadro alheio**, e confirmar que falham
- [x] 5.2 Criar `BoardService.archive`, `BoardService.restore` e os dois endpoints, até os testes de 5.1 passarem

```diff
+++ b/src/main/java/com/william/kanban/board/BoardController.java
+	@PostMapping("/{id}/archive")
+	BoardResponse archive(@AuthenticationPrincipal UUID accountId, @PathVariable UUID id) {
...
+	@PostMapping("/{id}/restore")
+	BoardResponse restore(@AuthenticationPrincipal UUID accountId, @PathVariable UUID id) {
...
```

## 6. Integridade e documentação

- [x] 6.1 Escrever os dois cenários de **Quadros apagados com a conta**, apagando a linha de `accounts` por `JdbcTemplate`, e confirmar que passam pela chave estrangeira criada em 1.2
- [x] 6.2 Escrever o cenário **Endpoints documentados no OpenAPI** lendo `/v3/api-docs`, confirmar que falha nos códigos de resposta, e criar `OpenApiResponsesConfig` em `com.william.kanban.shared`, que deriva as respostas de erro da assinatura de cada endpoint, até passar
- [x] 6.3 Rodar `./mvnw -q verify` e confirmar que o `jacoco` passa dos 80% de instrução e de branch
- [x] 6.4 Rodar `./mvnw test-compile org.pitest:pitest-maven:mutationCoverage` e confirmar que o pacote `board` fica acima de 80% de mutantes mortos
- [x] 6.5 Atualizar a seção **Estado do projeto** do `CLAUDE.md` com a capability `board-management`
