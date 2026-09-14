## Why

Os testes de API montam cada requisição e cada verificação com a API crua do `MockMvc`: `perform`, builder estático, `contentType`, header `Authorization` montado à mão, um `andExpect` por linha e `throws Exception` em todo método. O corpo da requisição é JSON escrito em string. A regra que o teste verifica fica escondida nesse ruído.

## What Changes

- `ApiClient`, no pacote `com.william.kanban.support` de `src/test`, abre a requisição por `get(path)` e `post(path)`.
- A requisição recebe o token por `withToken(token)`, que grava `Authorization: Bearer <token>`, e o corpo por `withBody(body)`, que serializa o objeto em JSON e grava `Content-Type: application/json`.
- `perform()` envia a requisição e devolve a resposta. A requisição não tem método `expect...`, e a resposta não tem método `with...`.
- A resposta confere o status por `expectStatus(HttpStatus)`, o header `Location` por `expectLocation`, o conjunto exato de `_links` por `expectLinks` com um `link(relação, href)` por relação, um campo por `expectJson` com valor ou matcher Hamcrest, e a presença e a ausência de um campo por `expectPresent` e `expectAbsent`.
- A resposta devolve um campo do corpo por `json(path)`.
- Nenhum método de `ApiClient` declara exceção checada.
- `CreateAccountRequest` ganha `@With` do Lombok e `@JsonInclude(NON_NULL)`: `withEmail`, `withDisplayName` e `withPassword` devolvem cópia com o campo trocado, e campo `null` fica fora do JSON serializado.
- `LoginRequest` passa a ser público.
- `AccountApiTest` fala com a API só pelo `ApiClient`, no campo `api`, monta todo corpo por `CreateAccountRequest` e `LoginRequest`, sem `throws Exception` nem JSON em string, e cada teste mantém o nome e as verificações.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

Nenhuma.

## Impact

- Arquivos novos: `src/test/java/com/william/kanban/support/ApiClient.java` e `lombok.config`.
- `src/main/java/com/william/kanban/account/CreateAccountRequest.java` e `src/main/java/com/william/kanban/auth/LoginRequest.java`.
- `src/test/java/com/william/kanban/account/AccountApiTest.java`.
- `pom.xml`: dependência `org.projectlombok:lombok` opcional, o Lombok em `annotationProcessorPaths` do `maven-compiler-plugin` e em `excludes` do `spring-boot-maven-plugin`.
- `CLAUDE.md`, seções Stack e Convenções.
- `README.md`, seções Stack e Estrutura.
