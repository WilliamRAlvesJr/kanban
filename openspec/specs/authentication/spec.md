## Purpose

Troca email e senha por um token opaco que identifica a conta em cada requisição, com validade de 24 horas e revogação imediata no logout. É o que permite a um recurso ter dono.

## Requirements

### Requirement: Emissão do token

O sistema SHALL expor `POST /auth/login`, que recebe `email` e `password` e responde `201` com `token`, `token_type` igual a `Bearer` e `expires_at`.

O `token` SHALL ser gerado por fonte criptográfica e SHALL aparecer em claro apenas nessa resposta.

#### Scenario: Credenciais corretas

```gherkin
Given uma conta com o email "ana@exemplo.com" e a senha "segredo"
When chega POST /auth/login com email "ana@exemplo.com" e password "segredo"
Then a resposta é 201 com token, token_type "Bearer" e expires_at
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

### Requirement: Credencial inválida indistinguível

O sistema SHALL responder `401` com o mesmo `ProblemDetail` quando o email não existe e quando a senha não confere. A resposta SHALL NOT informar qual dos dois falhou, e o custo de verificação SHALL ser o mesmo nos dois casos.

#### Scenario: Senha errada

```gherkin
Given uma conta com o email "ana@exemplo.com" e a senha "segredo"
When chega POST /auth/login com email "ana@exemplo.com" e password "errada"
Then a resposta é 401
And nenhum token é emitido
```

#### Scenario: Email não cadastrado

```gherkin
Given nenhuma conta com o email "bruno@exemplo.com"
When chega POST /auth/login com email "bruno@exemplo.com" e password "segredo"
Then a resposta é 401
And o corpo é igual ao da resposta de senha errada
```

### Requirement: Um token por login

O sistema SHALL emitir um token independente a cada login. Um login novo SHALL NOT invalidar token emitido antes.

#### Scenario: Dois logins da mesma conta

```gherkin
Given uma conta que já fez login e guardou o primeiro token
When chega POST /auth/login com as mesmas credenciais
Then a resposta traz um token diferente do primeiro
And o primeiro token continua aceito em GET /accounts/me
```

### Requirement: Validade do token

O token SHALL valer por 24 horas a partir da emissão, e o prazo SHALL ser configurável pela propriedade `kanban.auth.token-ttl`. Requisição com token expirado SHALL receber `401`.

#### Scenario: Token dentro do prazo

```gherkin
Given um token emitido há uma hora
When chega GET /accounts/me com esse token
Then a resposta é 200
```

#### Scenario: Token expirado

```gherkin
Given um token cujo expires_at já passou
When chega GET /accounts/me com esse token
Then a resposta é 401
```

### Requirement: Requisição autenticada por Bearer

O sistema SHALL aceitar o token no header `Authorization`, no formato `Bearer <token>`, e SHALL identificar a conta dona do token na requisição.

Requisição a endpoint protegido sem token, com header em outro formato ou com token desconhecido SHALL receber `401` em `ProblemDetail`.

#### Scenario: Token válido

```gherkin
Given um token emitido para a conta de "ana@exemplo.com"
When chega GET /accounts/me com o header Authorization "Bearer <token>"
Then a resposta é 200 com os dados da conta de "ana@exemplo.com"
```

#### Scenario: Sem header Authorization

```gherkin
When chega GET /accounts/me sem o header Authorization
Then a resposta é 401 em ProblemDetail
```

#### Scenario: Header em outro formato

```gherkin
When chega GET /accounts/me com o header Authorization "Basic YWJj"
Then a resposta é 401 em ProblemDetail
```

#### Scenario: Token desconhecido

```gherkin
When chega GET /accounts/me com um token que nunca foi emitido
Then a resposta é 401 em ProblemDetail
```

### Requirement: Revogação por logout

O sistema SHALL expor `POST /auth/logout`, que revoga o token da própria requisição e responde `204`. O token revogado SHALL receber `401` na requisição seguinte, e os demais tokens da conta SHALL continuar válidos.

#### Scenario: Logout revoga o token usado

```gherkin
Given um token válido
When chega POST /auth/logout com esse token
Then a resposta é 204
And a chamada seguinte a GET /accounts/me com o mesmo token responde 401
```

#### Scenario: Logout não atinge os outros tokens

```gherkin
Given uma conta com dois tokens emitidos
When chega POST /auth/logout com o primeiro token
Then a chamada a GET /accounts/me com o segundo token responde 200
```

#### Scenario: Logout sem token

```gherkin
When chega POST /auth/logout sem o header Authorization
Then a resposta é 401
```

### Requirement: Token armazenado apenas como hash

O sistema SHALL gravar apenas o hash do token. O valor em claro SHALL NOT ser persistido, e SHALL NOT aparecer em nenhuma resposta além da emissão.

#### Scenario: Valor em claro fora do banco

```gherkin
Given um token recém-emitido
When a tabela auth_tokens é consultada
Then nenhuma coluna guarda o valor em claro do token
```

### Requirement: Endpoints abertos

O sistema SHALL atender `POST /accounts`, `POST /auth/login` e a documentação OpenAPI sem token. Todo outro endpoint SHALL exigir token.

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

### Requirement: Autenticação documentada no OpenAPI

O documento OpenAPI SHALL descrever `POST /auth/login` e `POST /auth/logout` com os códigos de resposta que produzem, e SHALL declarar o esquema de segurança `bearer`.

#### Scenario: OpenAPI descreve o login

```gherkin
When o documento OpenAPI é solicitado
Then ele descreve POST /auth/login e POST /auth/logout
And declara um securityScheme do tipo http com scheme "bearer"
```
