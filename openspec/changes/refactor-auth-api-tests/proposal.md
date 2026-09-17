## Why

`AuthApiTest` e `RootApiTest` montam cada requisição e cada verificação com a API crua do `MockMvc`, com `throws Exception` em todo método, header `Authorization` montado à mão e corpo em JSON escrito em string. A regra que cada teste verifica fica escondida nesse ruído, e as duas classes não seguem a forma que o `AccountApiTest` já usa.

## What Changes

- `ApiClient.Request` recebe um header qualquer por `withHeader(name, value)`.
- `ApiClient.Response` confere a ausência do header `Location` por `expectNoLocation()` e devolve o corpo inteiro por `body()`.
- `LoginRequest` ganha `@With` e `@JsonInclude(NON_NULL)`: `withEmail` e `withPassword` devolvem cópia com o campo trocado, e campo `null` fica fora do JSON serializado.
- `AuthApiTest` e `RootApiTest` falam com a API só pelo `ApiClient`, no campo `api`, montam todo corpo por `CreateAccountRequest` e `LoginRequest`, sem `throws Exception`, sem `mockMvc.perform` e sem JSON em string, e cada teste mantém o nome e as verificações.
- As duas classes seguem as regras de formatação de Convenções do `CLAUDE.md`.

Fora desta change: `ProjectApiTest`, `ProjectMemberApiTest`, `BoardApiTest`, `LaneApiTest`, `SecurityConfigTest`.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

Nenhuma.

## Impact

- `src/test/java/com/william/kanban/support/ApiClient.java`.
- `src/test/java/com/william/kanban/auth/AuthApiTest.java` e `src/test/java/com/william/kanban/shared/RootApiTest.java`.
- `src/main/java/com/william/kanban/auth/LoginRequest.java`.
