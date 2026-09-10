## Purpose

Quadros de uma conta do kanban, com criação, consulta, edição e arquivamento reversível. Cada quadro tem um dono, e nenhuma conta enxerga quadro de outra.

## ADDED Requirements

### Requirement: Criação de quadro

O sistema SHALL expor `POST /boards`, que recebe `name` e `description` e cria um quadro com identificador `uuid` gerado pelo sistema e dono igual à conta do token.

A resposta de sucesso SHALL ser `201` com `id`, `name`, `description`, `created_at`, `updated_at` e `archived_at`.

#### Scenario: Quadro criado

```gherkin
Given um token emitido para a conta de "ana@exemplo.com"
When chega POST /boards com name "Sprint 12" e description "Trabalho da sprint"
Then a resposta é 201 com id, name, description, created_at, updated_at e archived_at
And archived_at vem null
```

#### Scenario: Quadro sem descrição

```gherkin
Given um token emitido para uma conta
When chega POST /boards com name "Sprint 12" e sem o campo description
Then a resposta é 201
And description vem null
```

#### Scenario: Dono tirado do token

```gherkin
Given duas contas cadastradas, cada uma com o seu token
When chega POST /boards com o token da segunda conta
Then a coluna owner_id guarda o id da segunda conta
```

### Requirement: Validação da entrada de criação

O sistema SHALL recusar com `400` a criação com `name` ausente, vazio ou acima de 100 caracteres, e com `description` acima de 500 caracteres. Nenhum quadro SHALL ser criado nesses casos.

#### Scenario: Name ausente

```gherkin
When chega POST /boards sem o campo name
Then a resposta é 400
And nenhum quadro é criado
```

#### Scenario: Name vazio

```gherkin
When chega POST /boards com name ""
Then a resposta é 400
And nenhum quadro é criado
```

#### Scenario: Name acima do limite

```gherkin
When chega POST /boards com name de 101 caracteres
Then a resposta é 400
And nenhum quadro é criado
```

#### Scenario: Description acima do limite

```gherkin
When chega POST /boards com description de 501 caracteres
Then a resposta é 400
And nenhum quadro é criado
```

### Requirement: Listagem dos quadros da conta

O sistema SHALL expor `GET /boards`, que devolve `200` com os quadros da conta do token, ordenados por `created_at` do mais recente para o mais antigo.

O parâmetro `archived` SHALL filtrar a listagem: ausente devolve todos, `false` devolve os ativos e `true` devolve os arquivados. Valor diferente de `true` e `false` SHALL receber `400`.

#### Scenario: Listagem sem o parâmetro

```gherkin
Given uma conta com um quadro ativo e um quadro arquivado
When chega GET /boards com o token dessa conta
Then a resposta é 200 com os dois quadros
```

#### Scenario: Somente os ativos

```gherkin
Given uma conta com um quadro ativo e um quadro arquivado
When chega GET /boards com archived "false"
Then a resposta é 200 com o quadro ativo
And o quadro arquivado não aparece
```

#### Scenario: Somente os arquivados

```gherkin
Given uma conta com um quadro ativo e um quadro arquivado
When chega GET /boards com archived "true"
Then a resposta é 200 com o quadro arquivado
And o quadro ativo não aparece
```

#### Scenario: Ordem da listagem

```gherkin
Given uma conta com três quadros criados em sequência
When chega GET /boards com o token dessa conta
Then o primeiro item é o quadro criado por último
And o último item é o quadro criado primeiro
```

#### Scenario: Valor inválido em archived

```gherkin
When chega GET /boards com archived "talvez"
Then a resposta é 400
```

#### Scenario: Quadro de outra conta fora da lista

```gherkin
Given duas contas, cada uma com um quadro
When chega GET /boards com o token da primeira conta
Then a resposta traz somente o quadro da primeira conta
```

### Requirement: Consulta de um quadro

O sistema SHALL expor `GET /boards/{id}`, que devolve `200` com o quadro da conta do token, arquivado ou não.

#### Scenario: Quadro ativo

```gherkin
Given um quadro ativo da conta do token
When chega GET /boards/{id} desse quadro
Then a resposta é 200 com id, name, description, created_at, updated_at e archived_at
```

#### Scenario: Quadro arquivado

```gherkin
Given um quadro arquivado da conta do token
When chega GET /boards/{id} desse quadro
Then a resposta é 200
And archived_at traz o instante do arquivamento
```

### Requirement: Isolamento entre donos

Requisição a `GET /boards/{id}`, `PUT /boards/{id}`, `POST /boards/{id}/archive` ou `POST /boards/{id}/restore` de quadro que não pertence à conta do token SHALL receber `404`, com o mesmo `ProblemDetail` de quadro inexistente. A resposta SHALL NOT revelar que o quadro existe.

#### Scenario: Consulta de quadro alheio

```gherkin
Given um quadro da segunda conta
When chega GET /boards/{id} desse quadro com o token da primeira conta
Then a resposta é 404
And o corpo é igual ao da resposta de quadro inexistente
```

#### Scenario: Edição de quadro alheio

```gherkin
Given um quadro da segunda conta com name "Sprint 12"
When chega PUT /boards/{id} desse quadro com o token da primeira conta
Then a resposta é 404
And o quadro continua com name "Sprint 12"
```

#### Scenario: Arquivamento de quadro alheio

```gherkin
Given um quadro ativo da segunda conta
When chega POST /boards/{id}/archive desse quadro com o token da primeira conta
Then a resposta é 404
And archived_at continua null
```

#### Scenario: Restauração de quadro alheio

```gherkin
Given um quadro arquivado da segunda conta
When chega POST /boards/{id}/restore desse quadro com o token da primeira conta
Then a resposta é 404
And archived_at continua preenchido
```

#### Scenario: Quadro inexistente

```gherkin
When chega GET /boards/{id} com um id que nunca existiu
Then a resposta é 404
```

### Requirement: Substituição do quadro

O sistema SHALL expor `PUT /boards/{id}`, que substitui `name` e `description` do quadro e responde `200` com o quadro atualizado. `description` ausente ou `null` SHALL gravar `null`, e `archived_at` SHALL NOT mudar.

#### Scenario: Quadro substituído

```gherkin
Given um quadro com name "Sprint 12" e description "Trabalho da sprint"
When chega PUT /boards/{id} com name "Sprint 13" e description "Outro texto"
Then a resposta é 200 com name "Sprint 13" e description "Outro texto"
```

#### Scenario: Descrição omitida apagada

```gherkin
Given um quadro com description "Trabalho da sprint"
When chega PUT /boards/{id} com name "Sprint 12" e sem o campo description
Then a resposta é 200
And description vem null
```

#### Scenario: Quadro arquivado editado

```gherkin
Given um quadro arquivado, com o archived_at guardado
When chega PUT /boards/{id} com name "Sprint 13"
Then a resposta é 200 com name "Sprint 13"
And archived_at continua igual ao guardado
```

### Requirement: Validação da entrada de substituição

`PUT /boards/{id}` com `name` ausente, vazio ou acima de 100 caracteres, ou com `description` acima de 500 caracteres, SHALL receber `400`. O quadro SHALL permanecer inalterado.

#### Scenario: Name ausente na substituição

```gherkin
Given um quadro com name "Sprint 12"
When chega PUT /boards/{id} sem o campo name
Then a resposta é 400
And o quadro continua com name "Sprint 12"
```

#### Scenario: Name vazio na substituição

```gherkin
Given um quadro com name "Sprint 12"
When chega PUT /boards/{id} com name ""
Then a resposta é 400
And o quadro continua com name "Sprint 12"
```

#### Scenario: Name acima do limite na substituição

```gherkin
Given um quadro com name "Sprint 12"
When chega PUT /boards/{id} com name de 101 caracteres
Then a resposta é 400
And o quadro continua com name "Sprint 12"
```

#### Scenario: Description acima do limite na substituição

```gherkin
Given um quadro com description "Trabalho da sprint"
When chega PUT /boards/{id} com name "Sprint 12" e description de 501 caracteres
Then a resposta é 400
And o quadro continua com description "Trabalho da sprint"
```

### Requirement: Arquivamento reversível

O sistema SHALL expor `POST /boards/{id}/archive`, que carimba `archived_at` com o instante do arquivamento, e `POST /boards/{id}/restore`, que grava `null` em `archived_at`. Os dois SHALL responder `200` com o quadro. Arquivar quadro arquivado ou restaurar quadro ativo SHALL responder `200` sem alterar `archived_at`.

#### Scenario: Quadro arquivado

```gherkin
Given um quadro ativo
When chega POST /boards/{id}/archive
Then a resposta é 200 com archived_at preenchido
And o quadro deixa de aparecer na listagem com archived "false"
```

#### Scenario: Quadro restaurado

```gherkin
Given um quadro arquivado
When chega POST /boards/{id}/restore
Then a resposta é 200 com archived_at null
And o quadro volta a aparecer na listagem com archived "false"
```

#### Scenario: Arquivamento repetido

```gherkin
Given um quadro arquivado, com o archived_at guardado
When chega POST /boards/{id}/archive
Then a resposta é 200
And archived_at continua igual ao guardado
```

#### Scenario: Restauração de quadro ativo

```gherkin
Given um quadro ativo
When chega POST /boards/{id}/restore
Then a resposta é 200 com archived_at null
```

### Requirement: Carimbo de updated_at

O sistema SHALL atualizar `updated_at` a cada alteração de valor do quadro. Requisição que não altera nenhum valor SHALL deixar `updated_at` intacto.

#### Scenario: Alteração carimba updated_at

```gherkin
Given um quadro criado, com o updated_at guardado
When chega PUT /boards/{id} com name "Sprint 13"
Then updated_at fica maior que o guardado
```

#### Scenario: Edição sem mudança de valor

```gherkin
Given um quadro com name "Sprint 12" e description "Trabalho da sprint", com o updated_at guardado
When chega PUT /boards/{id} com name "Sprint 12" e description "Trabalho da sprint"
Then updated_at continua igual ao guardado
```

#### Scenario: Criação preenche updated_at

```gherkin
When chega POST /boards com name "Sprint 12"
Then updated_at vem preenchido na resposta
```

### Requirement: Quadros apagados com a conta

Apagar a linha da conta em `accounts` SHALL apagar os quadros dela, arquivados ou não. Nenhum quadro SHALL permanecer sem dono.

#### Scenario: Conta apagada

```gherkin
Given uma conta com um quadro ativo e um quadro arquivado
When a linha dessa conta é apagada de accounts
Then nenhum quadro dela permanece em boards
```

#### Scenario: Quadro de outra conta preservado

```gherkin
Given duas contas, cada uma com um quadro
When a linha da primeira conta é apagada de accounts
Then o quadro da segunda conta continua em boards
```

### Requirement: Endpoints documentados no OpenAPI

Os endpoints de quadro SHALL aparecer no documento OpenAPI exposto pela aplicação, com os códigos de resposta que produzem.

#### Scenario: OpenAPI lista os endpoints

```gherkin
When o documento OpenAPI é solicitado
Then ele descreve POST /boards, GET /boards, GET /boards/{id}, PUT /boards/{id}, POST /boards/{id}/archive e POST /boards/{id}/restore
And lista os códigos de resposta de cada endpoint
```
