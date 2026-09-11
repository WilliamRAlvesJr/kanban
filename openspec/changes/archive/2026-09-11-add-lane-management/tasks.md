## 1. POST /boards/{boardId}/lanes

- [x] 1.1 Escrever em `LaneApiTest` os três cenários de **Criação de lane**, com a lane arquivada de **Lane criada no fim** montada por `JdbcTemplate` (`archived_at` e `position` no mesmo `UPDATE`), e a linha de `POST` de **Endpoint de lane em quadro alheio**, e confirmar que falham com o `404` de rota inexistente
- [x] 1.2 Criar `V4__create_lanes.sql` e confirmar que `./mvnw test -Dtest=KanbanApplicationTests` sobe o contexto com `ddl-auto=validate`

```diff
+++ b/src/main/resources/db/migration/V4__create_lanes.sql
+CREATE TABLE lanes (
+    id          uuid        PRIMARY KEY,
+    board_id    uuid        NOT NULL REFERENCES boards (id) ON DELETE CASCADE,
+    name        text        NOT NULL,
+    position    integer,
+    created_at  timestamptz NOT NULL,
+    updated_at  timestamptz NOT NULL,
+    archived_at timestamptz,
+    CONSTRAINT ux_lanes_board_id_position UNIQUE (board_id, position) DEFERRABLE INITIALLY IMMEDIATE,
+    CONSTRAINT ck_lanes_position_archived CHECK ((archived_at IS NULL) = (position IS NOT NULL))
+);
```

- [x] 1.3 Criar `Lane` e confirmar que o contexto sobe com a validação do schema

```diff
+++ b/src/main/java/com/william/kanban/lane/Lane.java
+@Entity
+@Table(name = "lanes")
+class Lane {
+
+	@Id
+	private UUID id;
+
+	private UUID boardId;
+
+	private String name;
+
+	private Integer position;
+
+	@CreationTimestamp
+	@Column(updatable = false)
+	private OffsetDateTime createdAt;
+
+	@UpdateTimestamp
+	private OffsetDateTime updatedAt;
+
+	private OffsetDateTime archivedAt;
+
+	protected Lane() {
+	}
+
+	Lane(UUID boardId, String name, int position) {
...
```

- [x] 1.4 Tornar `BoardService` pública com `requireOwned` e confirmar que `BoardApiTest` continua passando

```diff
+++ b/src/main/java/com/william/kanban/board/BoardService.java
 @Service
-class BoardService {
+public class BoardService {
...
+	public void requireOwned(UUID id, UUID ownerId) {
...
```

- [x] 1.5 Criar `LaneRepository` com `countByBoardIdAndArchivedAtIsNull`, os records `CreateLaneRequest` e `LaneResponse`, `LaneService.create` chamando `requireOwned` e o `POST /boards/{boardId}/lanes` em `LaneController`, até os testes de 1.1 passarem

```diff
+++ b/src/main/java/com/william/kanban/lane/LaneController.java
+@RestController
+@RequestMapping("/boards/{boardId}/lanes")
+class LaneController {
...
+	@PostMapping
+	@ResponseStatus(HttpStatus.CREATED)
+	LaneResponse create(
+
+			@AuthenticationPrincipal
+			UUID accountId,
+
+			@PathVariable
+			UUID boardId,
+
+			@RequestBody
+			CreateLaneRequest request
+
+	) {
...
```

- [x] 1.6 Escrever o cenário **Nome inválido na criação** como `@ParameterizedTest` com as três entradas, confirmar que falha com `201`, e adicionar `@Valid` e as anotações de Bean Validation com o limite em `LaneLimits` até passar

```diff
+++ b/src/main/java/com/william/kanban/lane/CreateLaneRequest.java
+record CreateLaneRequest(
+
+		@NotBlank
+		@Size(max = NAME_MAX_LENGTH)
+		String name
+
+) {
+}
```

- [x] 1.7 Escrever o cenário **Criações simultâneas**, com dez `POST` disparados de threads diferentes depois de um `CountDownLatch`, e confirmar que falha com `LaneService.create` chamando `requireOwned`
- [x] 1.8 Criar `BoardRepository.findWithLockByIdAndOwnerId` e `BoardService.lockOwned`, e trocar `requireOwned` por `lockOwned` em `LaneService.create`, até o teste de 1.7 passar

```diff
+++ b/src/main/java/com/william/kanban/board/BoardRepository.java
+	@Lock(LockModeType.PESSIMISTIC_WRITE)
+	Optional<Board> findWithLockByIdAndOwnerId(UUID id, UUID ownerId);
...
```

```diff
+++ b/src/main/java/com/william/kanban/board/BoardService.java
+	@Transactional(propagation = Propagation.MANDATORY)
+	public void lockOwned(UUID id, UUID ownerId) {
...
```

```diff
+++ b/src/main/java/com/william/kanban/lane/LaneService.java
 	@Transactional
 	Lane create(UUID boardId, UUID ownerId, String name) {
-		boardService.requireOwned(boardId, ownerId);
+		boardService.lockOwned(boardId, ownerId);
...
```

## 2. GET /boards/{boardId}/lanes

- [x] 2.1 Escrever os cinco cenários de **Listagem das lanes do quadro**, montando posições e arquivamento por `JdbcTemplate`, a linha de `GET` de **Endpoint de lane em quadro alheio** e o cenário **Quadro inexistente**, e confirmar que falham
- [x] 2.2 Criar os três finders da listagem, `LaneService.list` chamando `requireOwned` e o `GET /boards/{boardId}/lanes`, até os testes de 2.1 passarem

```diff
+++ b/src/main/java/com/william/kanban/lane/LaneRepository.java
+	List<Lane> findByBoardIdOrderByPositionAscArchivedAtDesc(UUID boardId);
+
+	List<Lane> findByBoardIdAndArchivedAtIsNullOrderByPositionAsc(UUID boardId);
+
+	List<Lane> findByBoardIdAndArchivedAtIsNotNullOrderByArchivedAtDesc(UUID boardId);
...
```

## 3. PUT /boards/{boardId}/lanes/{laneId}

- [x] 3.1 Escrever os dois cenários de **Substituição do nome**, a linha de `PUT` de **Endpoint de lane em quadro alheio**, a linha de `PUT` de **Lane de outro quadro** e o cenário **Lane inexistente**, e confirmar que falham
- [x] 3.2 Criar `LaneNotFoundException`, pública com construtor package-private, e ligá-la ao `404` no `GlobalExceptionHandler`

```diff
+++ b/src/main/java/com/william/kanban/shared/GlobalExceptionHandler.java
-	@ExceptionHandler({AccountNotFoundException.class, BoardNotFoundException.class})
+	@ExceptionHandler({AccountNotFoundException.class, BoardNotFoundException.class, LaneNotFoundException.class})
 	ProblemDetail handleNotFound(RuntimeException e) {
...
```

- [x] 3.3 Criar `LaneRepository.findByIdAndBoardId`, `UpdateLaneRequest`, `LaneService.rename`, que chama `lockOwned` antes de buscar a lane, e o `PUT /boards/{boardId}/lanes/{laneId}`, até os testes de 3.1 passarem
- [x] 3.4 Escrever o cenário **Nome inválido na substituição** como `@ParameterizedTest`, confirmar que falha, e adicionar `@Valid` e as anotações de Bean Validation com a constante de `LaneLimits` até passar
- [x] 3.5 Escrever os cenários **Renomeação carimba updated_at** e **Renomeação sem mudança de valor**, lendo a coluna por `JdbcTemplate` antes e depois, e confirmar que passam sem código novo, pelo dirty checking do Hibernate

## 4. POST archive e POST restore

- [x] 4.1 Escrever os quatro cenários de **Arquivamento reversível** e as linhas de `archive` e `restore` de **Endpoint de lane em quadro alheio** e de **Lane de outro quadro**, e confirmar que falham
- [x] 4.2 Criar `LaneRepository.shiftDownAbove`, `LaneService.archive`, `LaneService.restore` e os dois endpoints, até os testes de 4.1 passarem

```diff
+++ b/src/main/java/com/william/kanban/lane/LaneRepository.java
+	@Modifying(flushAutomatically = true, clearAutomatically = true)
+	@Query(value = "UPDATE lanes SET position = position - 1 WHERE board_id = :boardId AND position > :position",
+			nativeQuery = true)
+	void shiftDownAbove(UUID boardId, int position);
...
```

```diff
+++ b/src/main/java/com/william/kanban/lane/LaneService.java
+	@Transactional
+	Lane archive(UUID boardId, UUID laneId, UUID ownerId) {
+		boardService.lockOwned(boardId, ownerId);
+		Lane lane = findInBoard(boardId, laneId);
+		if (lane.getArchivedAt() != null) {
+			return lane;
+		}
+		int oldPosition = lane.getPosition();
+		lane.setArchivedAt(OffsetDateTime.now());
+		lane.setPosition(null);
+		repository.shiftDownAbove(boardId, oldPosition);
+		return lane;
+	}
+
+	@Transactional
+	Lane restore(UUID boardId, UUID laneId, UUID ownerId) {
+		boardService.lockOwned(boardId, ownerId);
+		Lane lane = findInBoard(boardId, laneId);
+		if (lane.getArchivedAt() == null) {
+			return lane;
+		}
+		lane.setPosition((int) repository.countByBoardIdAndArchivedAtIsNull(boardId));
+		lane.setArchivedAt(null);
+		return lane;
+	}
...
```

- [x] 4.3 Escrever os cenários **Arquivamento carimba updated_at**, **Restauração carimba updated_at**, **Arquivamento sem mudança de valor** e **Lane deslocada pelo arquivamento**, e confirmar que passam sem código novo, pelo dirty checking na lane alvo e pela query nativa de 4.2 nas deslocadas
- [x] 4.4 Escrever o cenário **Arquivamento e criação simultâneos** como `@RepeatedTest(20)`, e confirmar que passa com o código de 4.2 e falha com `LaneService.archive` chamando `requireOwned` no lugar de `lockOwned`

## 5. PUT /boards/{boardId}/lanes/order

- [x] 5.1 Escrever os cenários **Ordem regravada**, **Lane arquivada fora da ordem** e **Quadro sem lanes ativas**, e a linha de `order` de **Endpoint de lane em quadro alheio**, e confirmar que falham
- [x] 5.2 Criar `ReorderLanesRequest` sem anotações de validação, `LaneRepository.parkActivePositions`, `LaneRepository.assignPosition`, `LaneService.reorder` e o `PUT /boards/{boardId}/lanes/order`, até os testes de 5.1 passarem

```diff
+++ b/src/main/java/com/william/kanban/lane/LaneRepository.java
+	@Modifying(flushAutomatically = true, clearAutomatically = true)
+	@Query(value = "UPDATE lanes SET position = -position - 1 WHERE board_id = :boardId AND archived_at IS NULL",
+			nativeQuery = true)
+	void parkActivePositions(UUID boardId);
+
+	@Modifying(flushAutomatically = true, clearAutomatically = true)
+	@Query(value = "UPDATE lanes SET position = :position WHERE id = :id", nativeQuery = true)
+	void assignPosition(UUID id, int position);
...
```

- [x] 5.3 Escrever o cenário **Lista malformada** como `@ParameterizedTest`, confirmar que falha, e adicionar `@Valid` e a validação de `ReorderLanesRequest` até passar

```diff
+++ b/src/main/java/com/william/kanban/lane/ReorderLanesRequest.java
+record ReorderLanesRequest(
+
+		@NotNull
+		@JsonProperty("lane_ids")
+		List<@NotNull UUID> laneIds
+
+) {
+
+	@JsonIgnore
+	@AssertTrue
+	boolean isLaneIdsDistinct() {
+		return laneIds == null || new HashSet<>(laneIds).size() == laneIds.size();
+	}
+
+}
```

- [x] 5.4 Escrever o cenário **Conjunto diferente das lanes ativas** como `@ParameterizedTest`, confirmar que falha, e criar `LaneOrderMismatchException`, o `409` no `GlobalExceptionHandler` e a comparação de conjuntos em `LaneService.reorder`, antes de `parkActivePositions`, até passar

```diff
+++ b/src/main/java/com/william/kanban/shared/GlobalExceptionHandler.java
+	@ExceptionHandler(LaneOrderMismatchException.class)
+	ProblemDetail handleLaneOrderMismatch(LaneOrderMismatchException e) {
+		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
+	}
...
```

- [x] 5.5 Escrever o cenário **Reordenação não carimba updated_at** e confirmar que passa sem código novo, pelas queries nativas de 5.2

## 6. Integridade e documentação

- [x] 6.1 Escrever os cenários **Quadro apagado** e **Conta apagada**, apagando as linhas por `JdbcTemplate`, e confirmar que passam pela chave estrangeira criada em 1.2
- [x] 6.2 Escrever o cenário **Endpoints documentados no OpenAPI** lendo `/v3/api-docs`, confirmar que falha no `409` de `PUT /boards/{boardId}/lanes/order`, e adicionar essa resposta ao `OpenApiCustomizer` até passar

```diff
+++ b/src/main/java/com/william/kanban/shared/OpenApiResponsesConfig.java
 			addProblem(openApi.getPaths().get("/auth/login").getPost(), "401", "Email ou senha inválidos");
+			addProblem(openApi.getPaths().get("/boards/{boardId}/lanes/order").getPut(), "409",
+					"Lista diferente das lanes ativas do quadro");
...
```

- [x] 6.3 Rodar `./mvnw -q verify` e confirmar que o `jacoco` passa dos 80% de instrução e de branch
- [x] 6.4 Rodar `./mvnw test-compile org.pitest:pitest-maven:mutationCoverage` e confirmar que o pacote `lane` fica acima de 80% de mutantes mortos
- [x] 6.5 Atualizar a seção **Estado do projeto** do `CLAUDE.md` com a capability `lane-management`
