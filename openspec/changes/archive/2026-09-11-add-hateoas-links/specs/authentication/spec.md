## MODIFIED Requirements

Alterados nesta change; o restante de cada bloco é repetição da spec publicada.

- Emissão do token: descrição; cenário "Credenciais corretas".
- Endpoints abertos: descrição; cenário "Entrada sem token".

### Requirement: Emissão do token

O sistema SHALL expor `POST /auth/login`, que recebe `email` e `password` e responde `201` com `token`, `token_type` igual a `Bearer`, `expires_at` e `_links` com `me`, de `href` `/accounts/me`, e `logout`, de `href` `/auth/logout`. A resposta SHALL NOT trazer o header `Location`.

O `token` SHALL ser gerado por fonte criptográfica e SHALL aparecer em claro apenas nessa resposta.

#### Scenario: Credenciais corretas

```gherkin
Given uma conta com o email "ana@exemplo.com" e a senha "segredo"
When chega POST /auth/login com email "ana@exemplo.com" e password "segredo"
Then a resposta é 201 com token, token_type "Bearer" e expires_at
And _links traz somente me "/accounts/me" e logout "/auth/logout"
And a resposta não traz o header Location
```

#### Scenario: Email enviado com maiúsculas

```gherkin
Given uma conta com o email "ana@exemplo.com" e a senha "segredo"
When chega POST /auth/login com email "ANA@Exemplo.com" e password "segredo"
Then a resposta é 201 com token, token_type "Bearer" e expires_at
```

#### Scenario: Campo obrigatório ausente

```gherkin
When chega POST /auth/login faltando "email" ou "password"
Then a resposta é 400
And nenhum token é emitido
```

### Requirement: Endpoints abertos

O sistema SHALL atender `GET /`, `POST /accounts`, `POST /auth/login` e a documentação OpenAPI sem token. Todo outro endpoint SHALL exigir token.

#### Scenario: Cadastro sem token

```gherkin
When chega POST /accounts sem o header Authorization
Then a resposta é 201
```

#### Scenario: Documentação sem token

```gherkin
When chega GET /v3/api-docs sem o header Authorization
Then a resposta é 200
```

#### Scenario: Entrada sem token

```gherkin
When chega GET / sem o header Authorization
Then a resposta é 200
```
