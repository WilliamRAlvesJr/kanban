## 1. POST /projects/{projectId}/members

- [x] 1.1 Criar `ProjectMemberApiTest` com os cenários **Conta adicionada como membro**, conferindo a linha em `project_members` por `JdbcTemplate`, **Email sem efeito** e a linha de `POST` de **Conta fora do projeto**, e confirmar que falham com o `404` de rota inexistente nos dois primeiros
- [x] 1.2 Criar `V7__create_project_members.sql` e confirmar que `./mvnw test -Dtest=KanbanApplicationTests` sobe o contexto com `ddl-auto=validate`

```diff
+++ b/src/main/resources/db/migration/V7__create_project_members.sql
+CREATE TABLE project_members (
+    id         uuid        PRIMARY KEY,
+    project_id uuid        NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
+    account_id uuid        NOT NULL REFERENCES accounts (id) ON DELETE CASCADE,
+    created_at timestamptz NOT NULL,
+    CONSTRAINT ux_project_members_project_id_account_id UNIQUE (project_id, account_id)
+);
+
+CREATE INDEX ix_project_members_account_id ON project_members (account_id);
+
+CREATE TABLE project_member_permissions (
+    project_member_id uuid NOT NULL REFERENCES project_members (id) ON DELETE CASCADE,
+    permission        text NOT NULL,
+    PRIMARY KEY (project_member_id, permission),
+    CONSTRAINT ck_project_member_permissions_permission CHECK (permission IN (
+        'ADD_BOARDS', 'ADD_MEMBER', 'ARCHIVE_PROJECT', 'EDIT_MEMBER', 'EDIT_PROJECT',
+        'REMOVE_MEMBER', 'RESTORE_PROJECT', 'VIEW_BOARDS', 'VIEW_MEMBER', 'VIEW_PROJECT'
+    ))
+);
```

- [x] 1.3 Criar `ProjectPermission`, `ProjectMember` e `ProjectMemberRepository` com `insertIgnoringDuplicate`, e confirmar que o contexto sobe com a validação do schema

```diff
+++ b/src/main/java/com/william/kanban/project/ProjectPermission.java
+/** Constantes na ordem alfabética do valor JSON: a resposta lista as permissões na ordem de declaração. */
+public enum ProjectPermission {
+
+	@JsonProperty("add_boards")
+	ADD_BOARDS,
+
+	@JsonProperty("add_member")
+	ADD_MEMBER,
+
+	@JsonProperty("archive_project")
+	ARCHIVE_PROJECT,
+
+	@JsonProperty("edit_member")
+	EDIT_MEMBER,
+
+	@JsonProperty("edit_project")
+	EDIT_PROJECT,
+
+	@JsonProperty("remove_member")
+	REMOVE_MEMBER,
+
+	@JsonProperty("restore_project")
+	RESTORE_PROJECT,
+
+	@JsonProperty("view_boards")
+	VIEW_BOARDS,
+
+	@JsonProperty("view_member")
+	VIEW_MEMBER,
+
+	@JsonProperty("view_project")
+	VIEW_PROJECT
+
+}
```

```diff
+++ b/src/main/java/com/william/kanban/project/ProjectMember.java
+@Entity
+@Table(name = "project_members")
+class ProjectMember {
+
+	@Id
+	private UUID id;
+
+	private UUID projectId;
+
+	private UUID accountId;
+
+	@Column(insertable = false, updatable = false)
+	private OffsetDateTime createdAt;
+
+	@ElementCollection
+	@CollectionTable(name = "project_member_permissions", joinColumns = @JoinColumn(name = "project_member_id"))
+	@Column(name = "permission")
+	@Enumerated(EnumType.STRING)
+	private Set<ProjectPermission> permissions = new HashSet<>();
+
+	protected ProjectMember() {
+	}
...
```

```diff
+++ b/src/main/java/com/william/kanban/project/ProjectMemberRepository.java
+interface ProjectMemberRepository extends JpaRepository<ProjectMember, UUID> {
+
+	@Modifying
+	@Query(value = """
+			insert into project_members (id, project_id, account_id, created_at)
+			values (:id, :projectId, :accountId, :createdAt)
+			on conflict (project_id, account_id) do nothing""", nativeQuery = true)
+	void insertIgnoringDuplicate(UUID id, UUID projectId, UUID accountId, OffsetDateTime createdAt);
+
+}
```

- [x] 1.4 Criar `ProjectAccess`, `ProjectService.requireAccess` só com o ramo da dona, `AccountService.findIdByEmail`, `AddProjectMemberRequest` sem anotações de validação, `ProjectMemberService.add` pela sequência do design e o `POST /projects/{projectId}/members` em `ProjectMemberController`, até os testes de 1.1 passarem

```diff
+++ b/src/main/java/com/william/kanban/project/ProjectAccess.java
+public record ProjectAccess(boolean owner, Set<ProjectPermission> permissions) {
+
+	public boolean allows(ProjectPermission permission) {
+		return owner || permissions.contains(permission);
+	}
+
+}
```

```diff
+++ b/src/main/java/com/william/kanban/project/ProjectService.java
+	public ProjectAccess requireAccess(UUID id, UUID accountId, ProjectPermission permission) {
+		requireOwned(id, accountId);
+		return new ProjectAccess(true, Set.of());
+	}
...
```

```diff
+++ b/src/main/java/com/william/kanban/account/AccountService.java
+	public Optional<UUID> findIdByEmail(String email) {
...
```

```diff
+++ b/src/main/java/com/william/kanban/project/ProjectMemberController.java
+@RestController
+@RequestMapping("/projects/{projectId}/members")
+class ProjectMemberController {
...
+	@PostMapping
+	@ResponseStatus(HttpStatus.NO_CONTENT)
+	void add(
+
+			@AuthenticationPrincipal
+			UUID accountId,
+
+			@PathVariable
+			UUID projectId,
+
+			@RequestBody
+			AddProjectMemberRequest request
+
+	) {
...
```

- [x] 1.5 Escrever **Entrada inválida na adição** como `@ParameterizedTest` com as três entradas, confirmar que falha com `204`, e adicionar `@Valid`, `@NotBlank` e `@Email` até passar
- [x] 1.6 Escrever **Vínculo repetido recusado pelo banco**, inserindo a segunda linha por `JdbcTemplate`, e confirmar que passa sem código novo, pela restrição única de 1.2

## 2. GET /projects/{projectId}/members

- [x] 2.1 Escrever **Membros listados**, **Ordem da listagem**, **Dono fora da lista**, **Membro de outro projeto fora da lista**, **Links da listagem para o dono** e **Projeto inexistente**, trocar a conferência por `JdbcTemplate` de **Conta adicionada como membro** pela listagem, e confirmar que falham
- [x] 2.2 Criar `AccountSummary`, `AccountService.summariesOf`, `ProjectMemberRepository.findByProjectIdOrderByCreatedAtDesc` com `@EntityGraph`, `ProjectMemberResponse`, `ProjectMemberView`, `ProjectMembers`, `ProjectMemberService.list`, `ProjectMemberModelAssembler` e o `GET /projects/{projectId}/members`, até os testes de 2.1 passarem

```diff
+++ b/src/main/java/com/william/kanban/account/AccountSummary.java
+public record AccountSummary(UUID id, String email, String displayName) {
+}
```

```diff
+++ b/src/main/java/com/william/kanban/project/ProjectMemberRepository.java
+	@EntityGraph(attributePaths = "permissions")
+	List<ProjectMember> findByProjectIdOrderByCreatedAtDesc(UUID projectId);
...
```

```diff
+++ b/src/main/java/com/william/kanban/project/ProjectMemberResponse.java
+@Relation(collectionRelation = "members")
+record ProjectMemberResponse(
+
+		UUID id,
+
+		@JsonProperty("account_id")
+		UUID accountId,
+
+		String email,
+
+		@JsonProperty("display_name")
+		String displayName,
+
+		Set<ProjectPermission> permissions,
+
+		@JsonProperty("created_at")
+		OffsetDateTime createdAt
+
+) {
+}
```

## 3. GET /projects/{projectId}/members/{memberId}

- [x] 3.1 Escrever **Membro consultado**, gravando as permissões por `JdbcTemplate`, **Membro de outro projeto**, **Membro inexistente**, **Links do membro para o dono** e **Item da coleção de membros**, e confirmar que falham
- [x] 3.2 Criar `ProjectMemberNotFoundException`, pública com construtor package-private, e ligá-la ao `404` no `GlobalExceptionHandler`

```diff
+++ b/src/main/java/com/william/kanban/shared/GlobalExceptionHandler.java
 	@ExceptionHandler({AccountNotFoundException.class, BoardNotFoundException.class,
-			LaneNotFoundException.class, ProjectNotFoundException.class})
+			LaneNotFoundException.class, ProjectMemberNotFoundException.class, ProjectNotFoundException.class})
 	ProblemDetail handleNotFound(RuntimeException e) {
...
```

- [x] 3.3 Criar `ProjectMemberRepository.findByIdAndProjectId` com `@EntityGraph`, `ProjectMemberService.findById` e o `GET /projects/{projectId}/members/{memberId}`, com `permissions` copiado para `EnumSet` no assembler, até os testes de 3.1 passarem

## 4. PUT /projects/{projectId}/members/{memberId}/permissions

- [x] 4.1 Escrever **Permissões substituídas**, **Permissões esvaziadas**, **Permissão repetida**, **Atribuição a membro de outro projeto** e a linha de `PUT .../permissions` de **Self da escrita** em `ProjectMemberApiTest`, e confirmar que falham
- [x] 4.2 Criar `UpdateProjectMemberPermissionsRequest` sem anotações de validação, `ProjectMemberService.updatePermissions`, `@Transactional`, limpando e preenchendo o `Set` do membro, e o `PUT /projects/{projectId}/members/{memberId}/permissions`, respondendo `selfOf` do membro, até os testes de 4.1 passarem
- [x] 4.3 Escrever **Entrada inválida na atribuição** como `@ParameterizedTest` com as cinco entradas, confirmar que falha nas linhas sem o campo, com `null` e com item `null`, e adicionar `@Valid` e os `@NotNull` até passar

```diff
+++ b/src/main/java/com/william/kanban/project/UpdateProjectMemberPermissionsRequest.java
+record UpdateProjectMemberPermissionsRequest(
+
+		@NotNull
+		List<@NotNull ProjectPermission> permissions
+
+) {
+}
```

## 5. DELETE /projects/{projectId}/members/{memberId}

- [x] 5.1 Escrever **Membro removido** e **Remoção de membro de outro projeto**, e confirmar que falham
- [x] 5.2 Criar `ProjectMemberService.remove` e o `DELETE /projects/{projectId}/members/{memberId}`, com `@ResponseStatus(HttpStatus.NO_CONTENT)`, até os testes de 5.1 passarem

## 6. Permissões nos endpoints de membro

- [x] 6.1 Escrever **Membro com a permissão do endpoint** e **Membro sem a permissão do endpoint** como `@ParameterizedTest` com as cinco requisições, **Permissão não implica outra**, **Permissão conferida antes do membro da URL**, **Membro remove a si mesmo**, **Links da listagem para membro**, **Links do membro conforme as permissões** e as linhas de `GET` e `DELETE` de **Conta fora do projeto**, gravando as permissões por `JdbcTemplate`, e confirmar que falham
- [x] 6.2 Escrever **Dono sem permissão** e confirmar que passa sem código novo, pelo ramo da dona de 1.4
- [x] 6.3 Criar `ProjectAccessDeniedException`, pública com construtor package-private, e ligá-la ao `403` no `GlobalExceptionHandler`

```diff
+++ b/src/main/java/com/william/kanban/shared/GlobalExceptionHandler.java
+	@ExceptionHandler(ProjectAccessDeniedException.class)
+	ProblemDetail handleAccessDenied(ProjectAccessDeniedException e) {
+		return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());
+	}
...
```

- [x] 6.4 Criar `ProjectView`, `ProjectMemberRepository.findByProjectIdAndAccountId` com `@EntityGraph` e `ProjectService.load` pelo design, fazer `requireAccess` delegar a ele, e filtrar os links de `ProjectMemberModelAssembler` pela tabela do design, até os testes de 6.1 passarem

```diff
+++ b/src/main/java/com/william/kanban/project/ProjectService.java
 	public ProjectAccess requireAccess(UUID id, UUID accountId, ProjectPermission permission) {
-		requireOwned(id, accountId);
-		return new ProjectAccess(true, Set.of());
+		return load(id, accountId, permission).access();
 	}
+
+	private ProjectView load(UUID id, UUID accountId, ProjectPermission permission) {
...
```

## 7. Acesso de membro aos endpoints de projeto

- [x] 7.1 Em `ProjectApiTest`, escrever **Membro com a permissão do endpoint** e **Membro sem a permissão do endpoint** de projeto como `@ParameterizedTest` com as quatro requisições, e em `ProjectMemberApiTest` **Membro altera as próprias permissões**, e confirmar que falham com `404`
- [x] 7.2 Fazer `findById`, `update`, `archive` e `restore` de `ProjectService` usarem `load` com a permissão do endpoint, `findById` devolver `ProjectView`, e apagar `ProjectRepository.findByIdAndOwnerId`, até os testes de 7.1 e os de **Isolamento entre donos** passarem

## 8. GET /projects com projetos de membro

- [x] 8.1 Escrever **Projeto de membro com view_project** e **Projeto de membro sem view_project**, e confirmar que falham
- [x] 8.2 Trocar os três finders por `owner_id` pelos três `@Query` de visibilidade, criar `ProjectMemberRepository.findByAccountIdAndProjectIdIn` com `@EntityGraph`, e fazer `ProjectService.list` devolver um `ProjectView` por projeto, até os testes de 8.1 e os de **Listagem dos projetos da conta** passarem

```diff
+++ b/src/main/java/com/william/kanban/project/ProjectRepository.java
-	List<Project> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);
+	@Query("""
+			select p from Project p
+			where p.ownerId = :accountId
+			   or exists (select 1 from ProjectMember m join m.permissions granted
+			              where m.projectId = p.id
+			                and m.accountId = :accountId
+			                and granted = com.william.kanban.project.ProjectPermission.VIEW_PROJECT)
+			order by p.createdAt desc""")
+	List<Project> findVisibleOrderByCreatedAtDesc(UUID accountId);
...
```

```diff
+++ b/src/main/java/com/william/kanban/project/ProjectMemberRepository.java
+	@EntityGraph(attributePaths = "permissions")
+	List<ProjectMember> findByAccountIdAndProjectIdIn(UUID accountId, Collection<UUID> projectIds);
...
```

## 9. Links do projeto

- [x] 9.1 Reescrever **Links do projeto ativo** e **Links do projeto arquivado** com `members` e `add-member`, escrever **Links conforme as permissões do membro** como `@ParameterizedTest` com as quatro linhas, e confirmar que falham
- [x] 9.2 Fazer `ProjectModelAssembler.toModel` receber `ProjectView` e filtrar as relações pela tabela do design, até os testes de 9.1 e **Item igual à consulta individual** passarem

## 10. Quadros do projeto para membro

- [x] 10.1 Em `BoardApiTest`, escrever **Criação por membro com add_boards**, **Listagem por membro com view_boards**, **Criação por membro sem add_boards** e **Listagem por membro sem view_boards**, e confirmar que falham com `404`
- [x] 10.2 Trocar `requireOwned` por `requireAccess` em `BoardService.create` e `BoardService.list`, e fazer `list` devolver `ProjectBoards`, até os testes de 10.1 passarem

```diff
+++ b/src/main/java/com/william/kanban/board/BoardService.java
-	Board create(UUID projectId, UUID ownerId, String name, String description) {
-		projectService.requireOwned(projectId, ownerId);
+	Board create(UUID projectId, UUID accountId, String name, String description) {
+		projectService.requireAccess(projectId, accountId, ProjectPermission.ADD_BOARDS);
 		return repository.save(new Board(projectId, name, description));
 	}
...
-	List<Board> list(UUID projectId, UUID ownerId, Boolean archived) {
-		projectService.requireOwned(projectId, ownerId);
+	ProjectBoards list(UUID projectId, UUID accountId, Boolean archived) {
+		ProjectAccess access = projectService.requireAccess(projectId, accountId, ProjectPermission.VIEW_BOARDS);
...
```

- [x] 10.3 Escrever **Quadro de projeto em que a conta é membro** e **Destino em que a conta é membro**, e confirmar que passam sem código novo, porque `BoardService.findById` confere a posse por `isOwned` e `BoardService.move` o destino por `requireOwned`

## 11. Links do quadro para membro

- [x] 11.1 Escrever **Links da listagem para membro** e **Links do quadro para membro** como `@ParameterizedTest` com as três linhas, e confirmar que falham
- [x] 11.2 Fazer `BoardModelAssembler.toModel` e `toCollection` receberem `ProjectAccess`, com `new ProjectAccess(true, Set.of())` em `GET /boards/{id}`, e filtrar as relações pela tabela do design, até os testes de 11.1, **Links do quadro ativo**, **Links do quadro arquivado** e **Links da listagem** passarem

## 12. Integridade e documentação

- [x] 12.1 Escrever **Conta apagada** e **Projeto apagado** de membro, apagando as linhas por `JdbcTemplate`, e confirmar que passam sem código novo, pelas chaves estrangeiras de 1.2
- [x] 12.2 Em `OpenApiResponsesConfigTest`, escrever `documents403ForProjectIdPathVariable` com um método de `SampleController` que recebe `@PathVariable UUID projectId`, esperando `403` e `404`, e confirmar que falha
- [x] 12.3 Acrescentar o `403` a `errorResponsesFromSignature` até 12.2 e `documents404ForPathVariable` passarem

```diff
+++ b/src/main/java/com/william/kanban/shared/OpenApiResponsesConfig.java
 				addProblem(operation, "401", "Token ausente, desconhecido ou expirado");
 			}
+			if (any(parameters, p -> p.hasParameterAnnotation(PathVariable.class)
+					&& "projectId".equals(p.getParameter().getName()))) {
+				addProblem(operation, "403", "Permissão de projeto ausente");
+			}
 			if (any(parameters, p -> p.hasParameterAnnotation(PathVariable.class))) {
...
```

- [x] 12.4 Escrever **OpenAPI lista os endpoints** de membro e acrescentar a conferência do `403` aos testes de OpenAPI de projeto e de quadro, lendo `/v3/api-docs`, e confirmar que passam
- [x] 12.5 Rodar `./mvnw -q verify` e confirmar que o `jacoco` passa dos 80% de instrução e de branch
- [x] 12.6 Rodar `./mvnw test-compile org.pitest:pitest-maven:mutationCoverage` e confirmar que os pacotes `project`, `board` e `account` ficam acima de 80% de mutantes mortos
- [x] 12.7 Rodar `./mvnw spring-boot:run` sobre o Postgres de desenvolvimento e confirmar que a `V7` se aplica sem zerar o banco. Task sem código
- [x] 12.8 Atualizar a seção **Estado do projeto** do `CLAUDE.md` com a capability `project-membership`, o acesso de membro em `project-management` e `board-management`, os métodos públicos de `ProjectService` e `AccountService` e o record `AccountSummary`
