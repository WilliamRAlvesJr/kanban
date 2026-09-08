## ADDED Requirements

### Requirement: Consulta da conta autenticada

O sistema SHALL expor `GET /accounts/me`, que devolve os dados públicos da conta dona do token da requisição.

#### Scenario: Conta do token

```gherkin
Given um token emitido para a conta de "ana@exemplo.com"
When chega GET /accounts/me com esse token
Then a resposta é 200 com id, email e display_name da conta de "ana@exemplo.com"
```

#### Scenario: Conta de outro token

```gherkin
Given duas contas cadastradas, cada uma com o seu token
When chega GET /accounts/me com o token da segunda conta
Then a resposta traz o id da segunda conta
```

## MODIFIED Requirements

Alterados nesta change; o restante de cada bloco é repetição da spec publicada.

- Cadastro de conta: descrição, Conta criada
- Senha armazenada em hash: Senha não trafega de volta
- Endpoints documentados no OpenAPI: descrição, OpenAPI lista os endpoints

### Requirement: Cadastro de conta

O sistema SHALL expor `POST /accounts`, que recebe `email`, `display_name` e `password` e cria uma conta com identificador `uuid` gerado pelo sistema. O endpoint SHALL atender sem token.

A resposta de sucesso SHALL ser `201` com os dados públicos da conta: `id`, `email` e `display_name`.

#### Scenario: Conta criada

```gherkin
Given nenhuma conta com o email "ana@exemplo.com"
When chega POST /accounts com email "ana@exemplo.com", display_name e password preenchidos
Then a resposta é 201 com id, email e display_name
And a resposta não traz o header Location
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

### Requirement: Senha armazenada em hash

O sistema SHALL gravar a senha apenas como hash BCrypt. A senha em texto puro SHALL NOT ser persistida, e o `password_hash` SHALL NOT aparecer em nenhuma resposta da API.

#### Scenario: Senha não trafega de volta

```gherkin
Given um token emitido para uma conta criada com a senha "segredo"
When chega GET /accounts/me com esse token
Then o corpo da resposta contém somente id, email e display_name
And nenhum campo password ou password_hash aparece
```

#### Scenario: Senha gravada como hash

```gherkin
When chega POST /accounts com password "segredo"
Then a coluna password_hash guarda um hash BCrypt que valida contra "segredo"
And o valor gravado é diferente de "segredo"
```

### Requirement: Endpoints documentados no OpenAPI

Os endpoints de conta SHALL aparecer no documento OpenAPI exposto pela aplicação, com os códigos de resposta que produzem.

#### Scenario: OpenAPI lista os endpoints

```gherkin
When o documento OpenAPI é solicitado
Then ele descreve POST /accounts e GET /accounts/me
And lista os códigos de resposta de cada endpoint
```

## REMOVED Requirements

### Requirement: Consulta de conta por id

**Reason**: A conta é identificada pelo token da requisição, e o endpoint por id expunha qualquer conta a qualquer chamador.

**Migration**: Trocar `GET /accounts/{id}` por `GET /accounts/me` com o header `Authorization: Bearer <token>`.
