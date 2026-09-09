## Purpose

Cadastro e consulta de contas de usuário do kanban, com email único e normalizado e senha armazenada apenas em hash. É a identidade a que board, coluna e card se prendem.

## ADDED Requirements

### Requirement: Cadastro de conta

O sistema SHALL expor `POST /accounts`, que recebe `email`, `display_name` e `password` e cria uma conta com identificador `uuid` gerado pelo sistema.

A resposta de sucesso SHALL ser `201` com os dados públicos da conta: `id`, `email` e `display_name`.

#### Scenario: Conta criada

```gherkin
Given nenhuma conta com o email "ana@exemplo.com"
When chega POST /accounts com email "ana@exemplo.com", display_name e password preenchidos
Then a resposta é 201 com id, email e display_name
And o header Location aponta para /accounts/{id}
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
And a resposta traz "ana@exemplo.com"
```

### Requirement: Unicidade do email

O sistema SHALL aceitar no máximo uma conta por email. A unicidade SHALL ser garantida por índice único no banco.

#### Scenario: Email já cadastrado

```gherkin
Given uma conta com o email "ana@exemplo.com"
When chega POST /accounts com email "ana@exemplo.com"
Then a resposta é 409
And nenhuma conta é criada
```

#### Scenario: Email já cadastrado com outra caixa

```gherkin
Given uma conta com o email "ana@exemplo.com"
When chega POST /accounts com email "ANA@exemplo.com"
Then a resposta é 409
And nenhuma conta é criada
```

### Requirement: Senha armazenada em hash

O sistema SHALL gravar a senha apenas como hash BCrypt. A senha em texto puro SHALL NOT ser persistida, e o `password_hash` SHALL NOT aparecer em nenhuma resposta da API.

#### Scenario: Senha não trafega de volta

```gherkin
Given uma conta criada com a senha "segredo"
When chega GET /accounts/{id} com o id dessa conta
Then o corpo da resposta contém somente id, email e display_name
And nenhum campo password ou password_hash aparece
```

#### Scenario: Senha gravada como hash

```gherkin
When chega POST /accounts com password "segredo"
Then a coluna password_hash guarda um hash BCrypt que valida contra "segredo"
And o valor gravado é diferente de "segredo"
```

### Requirement: Consulta de conta por id

O sistema SHALL expor `GET /accounts/{id}`, que devolve os dados públicos da conta.

#### Scenario: Conta encontrada

```gherkin
Given uma conta existente
When chega GET /accounts/{id} com o id dessa conta
Then a resposta é 200 com id, email e display_name
```

#### Scenario: Conta inexistente

```gherkin
When chega GET /accounts/{id} com um uuid que não pertence a nenhuma conta
Then a resposta é 404
```

### Requirement: Endpoints documentados no OpenAPI

Os dois endpoints de conta SHALL aparecer no documento OpenAPI exposto pela aplicação, com os códigos de resposta que produzem.

#### Scenario: OpenAPI lista os endpoints

```gherkin
When o documento OpenAPI é solicitado
Then ele descreve POST /accounts e GET /accounts/{id}
And lista os códigos de resposta de cada endpoint
```
