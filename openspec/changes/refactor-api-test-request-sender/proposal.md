## Why

Os testes de API montam cada requisição e cada verificação com a API crua do `MockMvc`: `perform`, builder estático, `contentType`, header `Authorization` montado à mão, um `andExpect` por linha e `throws Exception` em todo método. A regra que o teste verifica fica escondida nesse ruído.

## What Changes

- `RequestSender`, no pacote `com.william.kanban.support` de `src/test`, abre a requisição por `get(path)` e `post(path)`.
- A requisição recebe o token por `as(token)`, que grava `Authorization: Bearer <token>`, e o corpo por `body(json)`, que grava `Content-Type: application/json`.
- `expectStatus(HttpStatus)` dispara a requisição e confere o status. Nenhum outro método dispara.
- A resposta confere o header `Location` por `expectLocation`, o conjunto exato de `_links` por `expectLinks` com pares de relação e `href`, um campo por `expectJson` com valor ou matcher Hamcrest, e a presença e a ausência de um campo por `expectPresent` e `expectAbsent`.
- A resposta devolve um campo do corpo por `json(path)`.
- Nenhum método de `RequestSender` declara exceção checada.
- `AccountApiTest` fala com a API só pelo `RequestSender`, sem `throws Exception`, e cada teste mantém o nome e as verificações.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

Nenhuma.

## Impact

- `src/test/java/com/william/kanban/support/RequestSender.java`: arquivo novo.
- `src/test/java/com/william/kanban/account/AccountApiTest.java`.
- `CLAUDE.md`, seção Convenções.
- Nenhuma mudança em `src/main` nem no `pom.xml`.
