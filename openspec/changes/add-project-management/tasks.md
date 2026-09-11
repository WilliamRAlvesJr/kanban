## 1. POST /projects

- [x] 1.1 Escrever em `ProjectApiTest` os três cenários de **Criação de projeto** e o cenário **Criação preenche updated_at** de projeto, e confirmar que falham com o `404` de rota inexistente
- [x] 1.2 Criar `V5__create_projects.sql` e confirmar que `./mvnw test -Dtest=KanbanApplicationTests` sobe o contexto com `ddl-auto=validate`

```diff
+++ b/src/main/resources/db/migration/V5__create_projects.sql
+CREATE TABLE projects (
+    id          uuid        PRIMARY KEY,
+    owner_id    uuid        NOT NULL REFERENCES accounts (id) ON DELETE CASCADE,
+    name        text        NOT NULL,
+    description text,
+    created_at  timestamptz NOT NULL,
+    updated_at  timestamptz NOT NULL,
+    archived_at timestamptz
+);
+
+CREATE INDEX ix_projects_owner_id ON projects (owner_id);
```

- [x] 1.3 Criar `Project`, com o mesmo mapeamento de tempo de `Board`, e confirmar que o contexto sobe com a validação do schema

```diff
+++ b/src/main/java/com/william/kanban/project/Project.java
+@Entity
+@Table(name = "projects")
+class Project {
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
+
+	protected Project() {
+	}
+
+	Project(UUID ownerId, String name, String description) {
...
```

- [x] 1.4 Criar `ProjectRepository`, os records `CreateProjectRequest`, sem anotações de validação, e `ProjectResponse`, `ProjectService.create` e o `POST /projects` em `ProjectController`, até os testes de 1.1 passarem
- [x] 1.5 Escrever o cenário **Entrada inválida na criação** de projeto como `@ParameterizedTest` com as quatro entradas, confirmar que falha com `201`, e adicionar `@Valid` e as anotações de Bean Validation com os limites em `ProjectLimits` até passar

## 2. GET /projects

- [x] 2.1 Escrever os seis cenários de **Listagem dos projetos da conta**, arquivando por `JdbcTemplate`, e confirmar que falham
- [x] 2.2 Criar os três finders da listagem, `ProjectService.list` e o `GET /projects`, até os testes de 2.1 passarem

```diff
+++ b/src/main/java/com/william/kanban/project/ProjectRepository.java
+	List<Project> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);
+
+	List<Project> findByOwnerIdAndArchivedAtIsNullOrderByCreatedAtDesc(UUID ownerId);
+
+	List<Project> findByOwnerIdAndArchivedAtIsNotNullOrderByCreatedAtDesc(UUID ownerId);
...
```

## 3. GET /projects/{projectId}

- [x] 3.1 Escrever os dois cenários de **Consulta de um projeto**, a linha de `GET` de **Endpoint de projeto ativo alheio** e o cenário **Projeto inexistente**, e confirmar que falham
- [x] 3.2 Criar `ProjectNotFoundException`, pública com construtor package-private, e ligá-la ao `404` no `GlobalExceptionHandler`

```diff
+++ b/src/main/java/com/william/kanban/shared/GlobalExceptionHandler.java
 	@ExceptionHandler({AccountNotFoundException.class, BoardNotFoundException.class,
-			LaneNotFoundException.class})
+			LaneNotFoundException.class, ProjectNotFoundException.class})
 	ProblemDetail handleNotFound(RuntimeException e) {
...
```

- [x] 3.3 Criar `ProjectRepository.findByIdAndOwnerId`, `ProjectService.findById` e o `GET /projects/{projectId}`, até os testes de 3.1 passarem

## 4. PUT /projects/{projectId}

- [x] 4.1 Escrever os três cenários de **Substituição do projeto** e a linha de `PUT` de **Endpoint de projeto ativo alheio**, e confirmar que falham
- [x] 4.2 Criar `UpdateProjectRequest`, sem anotações de validação, `ProjectService.update` e o `PUT /projects/{projectId}`, até os testes de 4.1 passarem
- [x] 4.3 Escrever o cenário **Entrada inválida na substituição** de projeto como `@ParameterizedTest`, confirmar que falha, e adicionar `@Valid` e as anotações com as constantes de `ProjectLimits` até passar
- [x] 4.4 Escrever os cenários **Alteração carimba updated_at** e **Edição sem mudança de valor** de projeto, lendo a coluna por `JdbcTemplate`, e confirmar que passam sem código novo, pelo dirty checking do Hibernate

## 5. POST archive e POST restore de projeto

- [x] 5.1 Escrever os cenários **Projeto arquivado**, **Projeto restaurado**, **Arquivamento repetido** e **Restauração de projeto ativo**, a linha de `archive` de **Endpoint de projeto ativo alheio** e o cenário **Restauração de projeto alheio**, e confirmar que falham
- [x] 5.2 Criar `ProjectService.archive`, `ProjectService.restore` e os dois endpoints, até os testes de 5.1 passarem

## 6. POST /projects/{projectId}/boards

- [x] 6.1 Em `BoardApiTest` e `LaneApiTest`, fazer `createBoard` criar o projeto por `POST /projects` e o quadro por `POST /projects/{projectId}/boards`. Em `BoardApiTest`, escrever os quatro cenários de **Criação de quadro no projeto**, reescrever os de **Validação da entrada de criação**, **Criação preenche updated_at** e **Quadro ativo** com a rota e o `project_id`, escrever **Criação em projeto alheio** e **Projeto inexistente**, apagar `takesOwnerFromToken`, os testes da listagem por conta, a consulta a `GET /boards` em `archivesBoard` e `restoresBoard` e as linhas de `/boards` em `documentsBoardEndpoints`, e confirmar que falham
- [x] 6.2 Criar `V6__add_project_to_boards.sql`, trocar `ownerId` por `projectId` em `Board`, trocar os finders por `owner_id` de `BoardRepository` por `findWithLockById`, tornar `ProjectService` pública com `isOwned` e `requireOwned`, conferir a posse em `BoardService.findById` e `BoardService.lockOwned` pelos dois passos do design, apagar `BoardService.list` e o `GET /boards`, e passar `projectId` por `BoardController.create` e `BoardService.create`, até os testes de 6.1, os de isolamento de quadro e os de `LaneApiTest` passarem

```diff
+++ b/src/main/resources/db/migration/V6__add_project_to_boards.sql
+ALTER TABLE boards DROP COLUMN owner_id;
+
+ALTER TABLE boards ADD COLUMN project_id uuid NOT NULL REFERENCES projects (id) ON DELETE CASCADE;
+
+CREATE INDEX ix_boards_project_id ON boards (project_id);
```

```diff
+++ b/src/main/java/com/william/kanban/board/Board.java
 	@Id
 	private UUID id;
 
-	private UUID ownerId;
+	private UUID projectId;
...
-	Board(UUID ownerId, String name, String description) {
+	Board(UUID projectId, String name, String description) {
...
-	UUID getOwnerId() {
-		return ownerId;
+	UUID getProjectId() {
+		return projectId;
 	}
...
```

```diff
+++ b/src/main/java/com/william/kanban/board/BoardRepository.java
-	Optional<Board> findByIdAndOwnerId(UUID id, UUID ownerId);
-
 	@Lock(LockModeType.PESSIMISTIC_WRITE)
-	Optional<Board> findWithLockByIdAndOwnerId(UUID id, UUID ownerId);
+	Optional<Board> findWithLockById(UUID id);
-
-	List<Board> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);
-
-	List<Board> findByOwnerIdAndArchivedAtIsNullOrderByCreatedAtDesc(UUID ownerId);
-
-	List<Board> findByOwnerIdAndArchivedAtIsNotNullOrderByCreatedAtDesc(UUID ownerId);
...
```

```diff
+++ b/src/main/java/com/william/kanban/project/ProjectService.java
 @Service
-class ProjectService {
+public class ProjectService {
...
+	public boolean isOwned(UUID id, UUID ownerId) {
+		return repository.existsByIdAndOwnerId(id, ownerId);
+	}
+
+	public void requireOwned(UUID id, UUID ownerId) {
+		if (!isOwned(id, ownerId)) {
+			throw new ProjectNotFoundException(id);
+		}
+	}
...
```

```diff
+++ b/src/main/java/com/william/kanban/project/ProjectRepository.java
+	boolean existsByIdAndOwnerId(UUID id, UUID ownerId);
...
```

```diff
+++ b/src/main/java/com/william/kanban/board/BoardService.java
-	BoardService(BoardRepository repository) {
+	BoardService(BoardRepository repository, ProjectService projectService) {
...
-	Board create(UUID ownerId, String name, String description) {
-		return repository.save(new Board(ownerId, name, description));
+	Board create(UUID projectId, UUID ownerId, String name, String description) {
+		projectService.requireOwned(projectId, ownerId);
+		return repository.save(new Board(projectId, name, description));
 	}
...
 	@Transactional(propagation = Propagation.MANDATORY)
 	public void lockOwned(UUID id, UUID ownerId) {
-		repository.findWithLockByIdAndOwnerId(id, ownerId)
+		repository.findWithLockById(id)
+				.filter(board -> projectService.isOwned(board.getProjectId(), ownerId))
 				.orElseThrow(() -> new BoardNotFoundException(id));
 	}
 
 	Board findById(UUID id, UUID ownerId) {
-		return repository.findByIdAndOwnerId(id, ownerId)
+		return repository.findById(id)
+				.filter(board -> projectService.isOwned(board.getProjectId(), ownerId))
 				.orElseThrow(() -> new BoardNotFoundException(id));
 	}
...
```

```diff
+++ b/src/main/java/com/william/kanban/board/BoardController.java
 @RestController
-@RequestMapping("/boards")
 class BoardController {
...
-	@PostMapping
+	@PostMapping("/projects/{projectId}/boards")
 	@ResponseStatus(HttpStatus.CREATED)
 	BoardResponse create(
 
 			@AuthenticationPrincipal
 			UUID accountId,
 
+			@PathVariable
+			UUID projectId,
+
 			@Valid
 			@RequestBody
 			CreateBoardRequest request
 
 	) {
...
-	@GetMapping("/{id}")
+	@GetMapping("/boards/{id}")
...
```

```diff
+++ b/src/main/java/com/william/kanban/board/BoardResponse.java
 		UUID id,
 
+		@JsonProperty("project_id")
+		UUID projectId,
+
 		String name,
...
```

- [x] 6.3 Trocar `/boards` por `/projects` na requisição sem token de `SecurityConfigTest` e confirmar que ela continua respondendo `401`

## 7. GET /projects/{projectId}/boards

- [x] 7.1 Escrever os seis cenários de **Listagem dos quadros do projeto** e o cenário **Listagem de projeto alheio**, devolver a `archivesBoard` e `restoresBoard` a consulta à listagem com `archived=false`, agora em `GET /projects/{projectId}/boards`, e confirmar que falham
- [x] 7.2 Criar os três finders por `project_id`, `BoardService.list` chamando `ProjectService.requireOwned` e o `GET /projects/{projectId}/boards`, até os testes de 7.1 passarem

```diff
+++ b/src/main/java/com/william/kanban/board/BoardRepository.java
+	List<Board> findByProjectIdOrderByCreatedAtDesc(UUID projectId);
+
+	List<Board> findByProjectIdAndArchivedAtIsNullOrderByCreatedAtDesc(UUID projectId);
+
+	List<Board> findByProjectIdAndArchivedAtIsNotNullOrderByCreatedAtDesc(UUID projectId);
...
```

## 8. POST /boards/{id}/move

- [x] 8.1 Escrever os cenários **Quadro movido**, **Destino arquivado**, **Destino igual ao projeto atual**, **Destino de outra conta**, **Destino inexistente**, **Movimentação de quadro alheio** e **Movimentação carimba updated_at**, e confirmar que falham
- [x] 8.2 Criar `MoveBoardRequest` sem anotações de validação, `Board.setProjectId`, `BoardService.move` e o `POST /boards/{id}/move`, até os testes de 8.1 passarem

```diff
+++ b/src/main/java/com/william/kanban/board/BoardService.java
+	@Transactional
+	Board move(UUID id, UUID ownerId, UUID projectId) {
+		Board board = findById(id, ownerId);
+		projectService.requireOwned(projectId, ownerId);
+		board.setProjectId(projectId);
+		return board;
+	}
...
```

- [x] 8.3 Escrever o cenário **Entrada inválida na movimentação** como `@ParameterizedTest`, confirmar que falha nas linhas sem `project_id` e com `project_id` `null`, e adicionar `@Valid` e `@NotNull` até passar

```diff
+++ b/src/main/java/com/william/kanban/board/MoveBoardRequest.java
+record MoveBoardRequest(
+
+		@NotNull
+		@JsonProperty("project_id")
+		UUID projectId
+
+) {
+}
```

## 9. Integridade e documentação

- [x] 9.1 Escrever em `ProjectApiTest` os cenários **Quadros intactos no arquivamento** e **Quadros intactos na restauração**, e confirmar que passam sem código novo, porque `ProjectService.archive` e `ProjectService.restore` só tocam na linha de `projects`
- [x] 9.2 Escrever os cenários **Conta apagada** e **Projeto de outra conta preservado** de projeto e **Projeto apagado** e **Quadro de outro projeto preservado** de quadro, apagando as linhas por `JdbcTemplate`, e confirmar que passam pelas chaves estrangeiras de 1.2 e 6.2
- [x] 9.3 Escrever o cenário **OpenAPI lista os endpoints** de projeto lendo `/v3/api-docs`, reescrever `documentsBoardEndpoints` com as rotas da spec de quadro, e confirmar que passam sem mudança no `OpenApiResponsesConfig`
- [x] 9.4 Rodar `./mvnw -q verify` e confirmar que o `jacoco` passa dos 80% de instrução e de branch
- [x] 9.5 Rodar `./mvnw test-compile org.pitest:pitest-maven:mutationCoverage` e confirmar que os pacotes `project` e `board` ficam acima de 80% de mutantes mortos
- [x] 9.6 Zerar o Postgres de desenvolvimento com os comandos do Migration Plan do design e confirmar que `./mvnw spring-boot:run` aplica `V5` e `V6`. Task sem código
- [x] 9.7 Atualizar a seção **Estado do projeto** do `CLAUDE.md` com a capability `project-management` e com as rotas, a posse pelo projeto e a movimentação em `board-management`
