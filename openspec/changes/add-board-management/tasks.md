## 1. POST /boards

- [ ] 1.1 Escrever em `BoardApiTest` os três cenários de **Criação de quadro** mais o cenário **Criação preenche updated_at**, e confirmar que falham com `404` na rota
- [ ] 1.2 Criar `V3__create_boards.sql` e confirmar que `./mvnw test -Dtest=KanbanApplicationTests` sobe o contexto com `ddl-auto=validate`

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

- [ ] 1.3 Criar `Board`, com o carimbo de tempo pela aplicação, e confirmar que o contexto sobe com a validação do schema

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

- [ ] 1.4 Criar `BoardRepository`, os records `CreateBoardRequest` e `BoardResponse`, `BoardService.create` e o `POST /boards` em `BoardController`, até os testes de 1.1 passarem
- [ ] 1.5 Escrever os quatro cenários de **Validação da entrada de criação**, confirmar que falham com `201`, e adicionar as anotações de Bean Validation com os limites em constantes de `BoardService` até passarem

## 2. GET /boards/{id}

- [ ] 2.1 Escrever os dois cenários de **Consulta de um quadro** e os três de **Isolamento entre donos**, e confirmar que falham
- [ ] 2.2 Criar `BoardNotFoundException` e ligá-la ao `404` no `GlobalExceptionHandler`

```diff
+++ b/src/main/java/com/william/kanban/shared/GlobalExceptionHandler.java
-	@ExceptionHandler(AccountNotFoundException.class)
-	ProblemDetail handleNotFound(AccountNotFoundException e) {
+	@ExceptionHandler({AccountNotFoundException.class, BoardNotFoundException.class})
+	ProblemDetail handleNotFound(RuntimeException e) {
...
```

- [ ] 2.3 Criar `BoardService.findById`, que resolve o quadro por `findByIdAndOwnerId`, e o `GET /boards/{id}`, até os testes de 2.1 passarem

```diff
+++ b/src/main/java/com/william/kanban/board/BoardRepository.java
+	Optional<Board> findByIdAndOwnerId(UUID id, UUID ownerId);
...
```

## 3. GET /boards

- [ ] 3.1 Escrever os seis cenários de **Listagem dos quadros da conta**, incluindo a ordem por `created_at` e o `400` do valor inválido, e confirmar que falham
- [ ] 3.2 Criar os três finders da listagem, `BoardService.list` e o endpoint, até os testes de 3.1 passarem

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

## 4. PATCH /boards/{id}

- [ ] 4.1 Escrever o teste dos três estados do corpo (`{}`, `{"description": null}`, `{"description": "x"}`) e confirmar que o Jackson 3 entrega `null` para campo ausente e `Optional.empty()` para `null` explícito. Se não entregar, trocar o `Optional` por um wrapper próprio de três estados antes de seguir
- [ ] 4.2 Escrever os quatro cenários de **Edição parcial do quadro** e confirmar que falham
- [ ] 4.3 Criar `UpdateBoardRequest`, `BoardService.update` e o `PATCH /boards/{id}`, até os testes de 4.2 passarem

```diff
+++ b/src/main/java/com/william/kanban/board/UpdateBoardRequest.java
+record UpdateBoardRequest(
+		Optional<String> name,
+		Optional<String> description,
+		Optional<Boolean> archived) {
+}
```

- [ ] 4.4 Escrever os quatro cenários de **Validação da entrada de edição**, confirmar que falham com `200`, e verificar os limites dentro de `BoardService` até passarem
- [ ] 4.5 Escrever os quatro cenários de **Arquivamento reversível**, confirmar que falham, e converter `archived` em `archived_at` no service até passarem
- [ ] 4.6 Escrever os dois cenários de alteração de **Carimbo de updated_at**, lendo a coluna por `JdbcTemplate` antes e depois, e confirmar que passam pelo dirty checking do Hibernate sem código novo

## 5. Integridade e documentação

- [ ] 5.1 Escrever os dois cenários de **Quadros apagados com a conta**, apagando a linha de `accounts` por `JdbcTemplate`, e confirmar que passam pela chave estrangeira criada em 1.2
- [ ] 5.2 Escrever o cenário **Endpoints documentados no OpenAPI** lendo `/v3/api-docs`, confirmar que falha nos códigos de resposta, e anotar os quatro endpoints com `@ApiResponses` até passar
- [ ] 5.3 Rodar `./mvnw -q verify` e confirmar que o `jacoco` passa dos 80% de instrução e de branch
- [ ] 5.4 Rodar `./mvnw test-compile org.pitest:pitest-maven:mutationCoverage` e confirmar que o pacote `board` fica acima de 80% de mutantes mortos
- [ ] 5.5 Atualizar a seção **Estado do projeto** do `CLAUDE.md` com a capability `board-management`
