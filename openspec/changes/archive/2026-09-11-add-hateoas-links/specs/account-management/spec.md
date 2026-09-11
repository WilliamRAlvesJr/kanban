## MODIFIED Requirements

Alterados nesta change; o restante de cada bloco é repetição da spec publicada.

- Cadastro de conta: descrição; cenário "Conta criada".
- Normalização do email: cenário "Email enviado com maiúsculas".
- Senha armazenada em hash: cenário "Senha não trafega de volta".
- Consulta da conta autenticada: descrição; cenário "Conta do token".

### Requirement: Cadastro de conta

O sistema SHALL expor `POST /accounts`, que recebe `email`, `display_name` e `password` e cria uma conta com identificador `uuid` gerado pelo sistema. O endpoint SHALL atender sem token.

A resposta de sucesso SHALL ser `201`, com o header `Location` igual a `/accounts/me` e corpo só com `_links`: `self`, com `href` `/accounts/me`, e `login`, com `href` `/auth/login`.

#### Scenario: Conta criada

```gherkin
Given nenhuma conta com o email "ana@exemplo.com"
When chega POST /accounts com email "ana@exemplo.com", display_name e password preenchidos
Then a resposta é 201 com o header Location "/accounts/me"
And o corpo traz somente _links, com self "/accounts/me" e login "/auth/login"
```

#### Scenario: Campo obrigatório ausente

```gherkin
When chega POST /accounts faltando "email", "display_name" ou "password"
Then a resposta é 400
And nenhuma conta é criada
```

#### Scenario: Email em formato inválido

```gherkin
When chega POST /accounts com email "ana.exemplo.com"
Then a resposta é 400
And nenhuma conta é criada
```

### Requirement: Normalização do email

O sistema SHALL converter o email para minúsculas antes de gravar. O email armazenado SHALL estar sempre em minúsculas, e o banco SHALL recusar a gravação de um email não normalizado.

#### Scenario: Email enviado com maiúsculas

```gherkin
Given nenhuma conta com o email "ana@exemplo.com"
When chega POST /accounts com email "Ana@Exemplo.com"
Then a coluna email guarda "ana@exemplo.com"
```

### Requirement: Senha armazenada em hash

O sistema SHALL gravar a senha apenas como hash BCrypt. A senha em texto puro SHALL NOT ser persistida, e o `password_hash` SHALL NOT aparecer em nenhuma resposta da API.

#### Scenario: Senha não trafega de volta

```gherkin
Given um token emitido para uma conta criada com a senha "segredo"
When chega GET /accounts/me com esse token
Then o corpo da resposta contém somente id, email, display_name e _links
And nenhum campo password ou password_hash aparece
```

#### Scenario: Senha gravada como hash

```gherkin
When chega POST /accounts com password "segredo"
Then a coluna password_hash guarda um hash BCrypt que valida contra "segredo"
And o valor gravado é diferente de "segredo"
```

### Requirement: Consulta da conta autenticada

O sistema SHALL expor `GET /accounts/me`, que devolve os dados públicos da conta dona do token da requisição e `_links` com `self`, de `href` `/accounts/me`, e `projects`, de `href` `/projects`.

#### Scenario: Conta do token

```gherkin
Given um token emitido para a conta de "ana@exemplo.com"
When chega GET /accounts/me com esse token
Then a resposta é 200 com id, email e display_name da conta de "ana@exemplo.com"
And _links traz somente self "/accounts/me" e projects "/projects"
```

#### Scenario: Conta de outro token

```gherkin
Given duas contas cadastradas, cada uma com o seu token
When chega GET /accounts/me com o token da segunda conta
Then a resposta traz o id da segunda conta
```
