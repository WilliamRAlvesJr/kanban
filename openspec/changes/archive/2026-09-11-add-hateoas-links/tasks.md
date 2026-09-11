## 1. GET /projects/{projectId} em HAL

- [x] 1.1 Escrever em `ProjectApiTest` os cenários **Conteúdo em HAL**, **Accept HAL**, **Links do projeto ativo** e **Links do projeto arquivado**, e confirmar que falham pelo `Content-Type` `application/json` e pela falta de `_links`
- [x] 1.2 Adicionar `spring-boot-starter-hateoas` ao `pom.xml`, anotar `ProjectResponse` com `@Relation`, criar `ProjectModelAssembler.toModel` com os links do design e fazer `GET /projects/{projectId}` devolver `EntityModel<ProjectResponse>`, até os testes de 1.1 passarem

```diff
+++ b/pom.xml
 		<dependency>
 			<groupId>org.springframework.boot</groupId>
 			<artifactId>spring-boot-starter-webmvc</artifactId>
 		</dependency>
 
+		<dependency>
+			<groupId>org.springframework.boot</groupId>
+			<artifactId>spring-boot-starter-hateoas</artifactId>
+		</dependency>
+
...
```

```diff
+++ b/src/main/java/com/william/kanban/project/ProjectResponse.java
+@Relation(collectionRelation = "projects")
 record ProjectResponse(
...
```

```diff
+++ b/src/main/java/com/william/kanban/project/ProjectModelAssembler.java
+@Component
+class ProjectModelAssembler {
+
+	EntityModel<ProjectResponse> toModel(Project project) {
+		String self = "/projects/" + project.getId();
+		EntityModel<ProjectResponse> model = EntityModel.of(toResponse(project), Link.of(self), Link.of(self, "edit"));
+		model.add(project.getArchivedAt() == null
+				? Link.of(self + "/archive", "archive")
+				: Link.of(self + "/restore", "restore"));
+		return model.add(Link.of(self + "/boards", "boards"), Link.of(self + "/boards", "create-board"));
+	}
...
```

```diff
+++ b/src/main/java/com/william/kanban/project/ProjectController.java
 	@GetMapping("/{projectId}")
-	ProjectResponse findById(
+	EntityModel<ProjectResponse> findById(
...
-		return toResponse(service.findById(projectId, accountId));
+		return assembler.toModel(service.findById(projectId, accountId));
 	}
...
```

## 2. GET /projects em _embedded

- [x] 2.1 Reescrever os testes de **Listagem dos projetos da conta** lendo `$._embedded.projects`, escrever **Links da listagem** de projeto, **Item igual à consulta individual**, **Coleção vazia** e **Self sem query string**, e confirmar que falham
- [x] 2.2 Criar `ProjectModelAssembler.toCollection` com o array vazio do design e fazer `GET /projects` devolver `CollectionModel<?>`, até os testes de 2.1 passarem

```diff
+++ b/src/main/java/com/william/kanban/project/ProjectModelAssembler.java
+	private static final EmbeddedWrappers WRAPPERS = new EmbeddedWrappers(false);
...
+	CollectionModel<?> toCollection(List<Project> projects) {
+		CollectionModel<?> collection = projects.isEmpty()
+				? CollectionModel.of(List.of(WRAPPERS.emptyCollectionOf(ProjectResponse.class)))
+				: CollectionModel.of(projects.stream().map(this::toModel).toList());
+		return collection.add(Link.of("/projects"), Link.of("/projects", "create-project"));
+	}
...
```

## 3. Escritas de projeto só com links

- [x] 3.1 Fazer os helpers que criam projeto em `ProjectApiTest`, `BoardApiTest` e `LaneApiTest` lerem o id do header `Location`. Reescrever **Projeto criado**, **Projeto sem descrição**, **Criação preenche updated_at**, **Projeto substituído**, **Descrição omitida apagada**, **Projeto arquivado editado**, **Projeto arquivado**, **Projeto restaurado** e **Restauração de projeto ativo** lendo o projeto por `GET`, escrever as linhas de projeto de **Self da escrita** e **Location da criação**, e confirmar que falham
- [x] 3.2 Criar `ProjectModelAssembler.selfOf` e fazer a criação devolver `ResponseEntity<RepresentationModel<?>>` e `PUT`, `archive` e `restore` devolverem `RepresentationModel<?>`, até os testes de 3.1 e os de `BoardApiTest` e `LaneApiTest` passarem

```diff
+++ b/src/main/java/com/william/kanban/project/ProjectController.java
 	@PostMapping
 	@ResponseStatus(HttpStatus.CREATED)
-	ProjectResponse create(
+	ResponseEntity<RepresentationModel<?>> create(
...
-		return toResponse(service.create(accountId, request.name(), request.description()));
+		RepresentationModel<?> model = assembler.selfOf(service.create(accountId, request.name(), request.description()));
+		return ResponseEntity.created(model.getRequiredLink(IanaLinkRelations.SELF).toUri()).body(model);
 	}
...
```

## 4. GET de quadro em HAL

- [x] 4.1 Escrever em `BoardApiTest` os cenários **Links do quadro ativo**, **Links do quadro arquivado** e **Link do projeto depois da movimentação**, reescrever os testes de **Listagem dos quadros do projeto** lendo `$._embedded.boards`, escrever **Links da listagem** de quadro, e confirmar que falham
- [x] 4.2 Anotar `BoardResponse` com `@Relation(collectionRelation = "boards")`, criar `BoardModelAssembler.toModel` e `BoardModelAssembler.toCollection` e fazer `GET /boards/{id}` e `GET /projects/{projectId}/boards` devolverem os modelos, até os testes de 4.1 passarem

## 5. Escritas de quadro só com links

- [x] 5.1 Fazer os helpers que criam quadro em `BoardApiTest`, `LaneApiTest` e `ProjectApiTest` lerem o id do header `Location`. Reescrever **Quadro criado**, **Quadro sem descrição**, **Criação preenche updated_at**, **Quadro substituído**, **Descrição omitida apagada**, **Quadro arquivado editado**, **Quadro arquivado**, **Quadro restaurado**, **Restauração de quadro ativo**, **Quadro movido**, **Destino arquivado** e **Destino igual ao projeto atual** lendo o quadro por `GET`, escrever as linhas de quadro de **Self da escrita** e **Location da criação**, e confirmar que falham
- [x] 5.2 Criar `BoardModelAssembler.selfOf` e mudar o retorno da criação, de `PUT`, `archive`, `restore` e `move` como em 3.2, até os testes de 5.1 e os de `LaneApiTest` e `ProjectApiTest` passarem

## 6. GET /boards/{boardId}/lanes/{laneId}

- [x] 6.1 Escrever em `LaneApiTest` os cenários **Lane ativa**, **Lane arquivada**, **Links da lane ativa** e **Links da lane arquivada**, as linhas de `GET` de **Endpoint de lane em quadro alheio** e **Lane de outro quadro**, e a linha de `GET /boards/{boardId}/lanes/{laneId}` em `documentsLaneEndpoints`, e confirmar que falham
- [x] 6.2 Anotar `LaneResponse` com `@Relation(collectionRelation = "lanes")`, criar `LaneModelAssembler.toModel`, `LaneService.findById` e o `GET /boards/{boardId}/lanes/{laneId}`, até os testes de 6.1 passarem

```diff
+++ b/src/main/java/com/william/kanban/lane/LaneService.java
+	Lane findById(UUID boardId, UUID laneId, UUID ownerId) {
+		boardService.requireOwned(boardId, ownerId);
+		return findInBoard(boardId, laneId);
+	}
...
```

```diff
+++ b/src/main/java/com/william/kanban/lane/LaneController.java
+	@GetMapping("/{laneId}")
+	EntityModel<LaneResponse> findById(
+
+			@AuthenticationPrincipal
+			UUID accountId,
+
+			@PathVariable
+			UUID boardId,
+
+			@PathVariable
+			UUID laneId
+
+	) {
+		return assembler.toModel(service.findById(boardId, laneId, accountId));
+	}
...
```

## 7. GET /boards/{boardId}/lanes em _embedded

- [x] 7.1 Reescrever os testes de **Listagem das lanes do quadro** lendo `$._embedded.lanes`, escrever **Links da listagem** de lane, e confirmar que falham
- [x] 7.2 Criar `LaneModelAssembler.toCollection` e fazer `GET /boards/{boardId}/lanes` devolver `CollectionModel<?>`, até os testes de 7.1 passarem

## 8. Escritas de lane só com links

- [x] 8.1 Fazer `createLane` de `LaneApiTest` ler o id do header `Location`. Reescrever **Lane criada em quadro vazio**, **Lane criada no fim**, **Nome substituído**, **Lane arquivada renomeada**, **Lane arquivada**, **Lane restaurada**, **Arquivamento repetido**, **Restauração de lane ativa**, **Ordem regravada**, **Lane arquivada fora da ordem** e **Quadro sem lanes ativas** lendo as lanes por `GET` ou por `JdbcTemplate`, escrever as linhas de lane de **Self da escrita** e **Location da criação**, e confirmar que falham
- [x] 8.2 Criar `LaneModelAssembler.selfOf` e `LaneModelAssembler.selfOfCollection` e mudar o retorno da criação, de `PUT`, `archive`, `restore` e `order` como em 3.2, até os testes de 8.1 passarem

## 9. Conta em HAL

- [x] 9.1 Fazer os helpers que devolvem o id de conta criada, em todos os `*ApiTest`, lerem o id em `accounts` por `JdbcTemplate`. Reescrever **Conta criada**, **Email enviado com maiúsculas**, **Senha não trafega de volta** e **Conta do token**, e confirmar que falham
- [x] 9.2 Fazer `POST /accounts` devolver `ResponseEntity<RepresentationModel<?>>` e `GET /accounts/me` devolver `EntityModel<AccountResponse>`, com os links da spec, até os testes de 9.1 passarem

```diff
+++ b/src/main/java/com/william/kanban/account/AccountController.java
 class AccountController {
 
+	private static final String ME = "/accounts/me";
+
...
 	@PostMapping
 	@ResponseStatus(HttpStatus.CREATED)
-	AccountResponse create(
+	ResponseEntity<RepresentationModel<?>> create(
...
-		return toResponse(
-				service.create(request.email(), request.displayName(), request.password()));
+		service.create(request.email(), request.displayName(), request.password());
+		return ResponseEntity.created(URI.create(ME))
+				.body(new RepresentationModel<>(List.of(Link.of(ME), Link.of("/auth/login", "login"))));
 	}
...
```

## 10. Login com links

- [x] 10.1 Reescrever **Credenciais corretas** em `AuthApiTest` com `_links` e sem `Location`, e confirmar que falha
- [x] 10.2 Fazer `POST /auth/login` devolver `EntityModel<LoginResponse>` com `me` e `logout`, até o teste de 10.1 passar

## 11. GET /

- [x] 11.1 Criar `RootApiTest` em `com.william.kanban.shared` com **Entrada sem token**, que cobre o cenário de mesmo nome das specs `api-hypermedia` e `authentication`, **Entrada com token válido**, **Entrada com token inválido** e **OpenAPI descreve a entrada**, e confirmar que falham com `401`
- [x] 11.2 Liberar `GET /` no `SecurityConfig` e criar `RootController`, até os testes de 11.1 passarem

```diff
+++ b/src/main/java/com/william/kanban/auth/SecurityConfig.java
 				.authorizeHttpRequests(requests -> requests
+						.requestMatchers(HttpMethod.GET, "/").permitAll()
 						.requestMatchers(HttpMethod.POST, "/accounts", "/auth/login").permitAll()
...
```

```diff
+++ b/src/main/java/com/william/kanban/shared/RootController.java
+@RestController
+class RootController {
+
+	@GetMapping("/")
+	RepresentationModel<?> root(
+
+			@CurrentSecurityContext
+			SecurityContext context
+
+	) {
+		if (context.getAuthentication().getPrincipal() instanceof UUID) {
...
```

## 12. Integridade e documentação

- [x] 12.1 Escrever em `ProjectApiTest` o cenário **Recurso inexistente** e confirmar que passa sem código novo, porque o `GlobalExceptionHandler` responde em `ProblemDetail`
- [x] 12.2 Rodar os `documents*Endpoints` e `OpenApiResponsesConfigTest`, ajustar as asserções de `components.schemas` ao nome que o springdoc dá aos modelos HAL, e confirmar que passam sem mudança no `OpenApiResponsesConfig`
- [x] 12.3 Rodar `./mvnw -q verify` e confirmar que o `jacoco` passa dos 80% de instrução e de branch
- [x] 12.4 Rodar `./mvnw test-compile org.pitest:pitest-maven:mutationCoverage` e confirmar que os pacotes `project`, `board`, `lane`, `account`, `auth` e `shared` ficam acima de 80% de mutantes mortos
- [x] 12.5 Atualizar o `CLAUDE.md`: a capability `api-hypermedia`, os assemblers, `GET /` e `GET /boards/{boardId}/lanes/{laneId}` em **Estado do projeto**, a resposta de `POST /accounts`, o `spring-boot-starter-hateoas` em **Stack**, e em **Convenções** o `href` relativo e a escrita que responde só com links. Task sem código
