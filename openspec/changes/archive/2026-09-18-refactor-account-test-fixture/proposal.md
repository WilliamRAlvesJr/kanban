## Why

`AccountApiTest`, `AuthApiTest` e `RootApiTest` repetem os mesmos helpers de conta com nomes diferentes: `createAna`, `createAccount`, `loginAsAna`, `tokenOf` e `tokenOfAna`, cada um com a sua cópia da conta de Ana.

## What Changes

- `AccountFixture`, em `com.william.kanban.support` de `src/test`, cria conta pela API e emite token pelo login, a partir de um `ApiClient` e de um `JdbcTemplate`.
- `ApiPaths`, em `com.william.kanban.support` de `src/test`, traz as constantes de caminho `ACCOUNTS`, `ME`, `LOGIN` e `LOGOUT`.
- `TableRows`, em `com.william.kanban.support` de `src/test`, conta as linhas de uma tabela a partir de um `JdbcTemplate`.
- `AccountFixture` expõe as constantes `ANA` e `ANA_LOGIN` e os métodos `createAccount`, `createAna` e `tokenOf`.
- `AccountApiTest`, `AuthApiTest` e `RootApiTest` usam `AccountFixture`, `ApiPaths` e `TableRows` no lugar dos helpers, das contagens e das constantes próprios, e cada teste mantém o nome e as verificações.
- O `CLAUDE.md` registra `AccountFixture`, `ApiPaths` e `TableRows` em Convenções.

Fora desta change: `ProjectApiTest`, `ProjectMemberApiTest`, `BoardApiTest`, `LaneApiTest`.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

Nenhuma.

## Impact

- `src/test/java/com/william/kanban/support/AccountFixture.java`, `src/test/java/com/william/kanban/support/ApiPaths.java` e `src/test/java/com/william/kanban/support/TableRows.java`.
- `src/test/java/com/william/kanban/controller/AccountApiTest.java`, `src/test/java/com/william/kanban/auth/AuthApiTest.java` e `src/test/java/com/william/kanban/shared/RootApiTest.java`.
- `CLAUDE.md`.
