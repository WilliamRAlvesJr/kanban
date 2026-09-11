## Purpose

Projetos de uma conta do kanban, que agrupam os quadros dela, com criação, consulta, edição e arquivamento reversível. Cada projeto tem um dono, e nenhuma conta enxerga projeto de outra.

## ADDED Requirements

### Requirement: Criação de projeto

O sistema SHALL expor `POST /projects`, que recebe `name` e `description` e cria um projeto com identificador `uuid` gerado pelo sistema e dono igual à conta do token.

A resposta de sucesso SHALL ser `201` com `id`, `name`, `description`, `created_at`, `updated_at` e `archived_at`.

#### Scenario: Projeto criado

```gherkin
Given um token emitido para a conta de "ana@exemplo.com"
When chega POST /projects com name "Produto" e description "Time de produto"
Then a resposta é 201 com id, name, description, created_at, updated_at e archived_at
And archived_at vem null
```

#### Scenario: Projeto sem descrição

```gherkin
Given um token emitido para uma conta
When chega POST /projects com name "Produto" e sem o campo description
Then a resposta é 201
And description vem null
```

#### Scenario: Dono tirado do token

```gherkin
Given duas contas cadastradas, cada uma com o seu token
When chega POST /projects com o token da segunda conta
Then a coluna owner_id guarda o id da segunda conta
```

### Requirement: Validação da entrada de criação

O sistema SHALL recusar com `400` a criação com `name` ausente, vazio ou acima de 100 caracteres, e com `description` acima de 500 caracteres. Nenhum projeto SHALL ser criado nesses casos.

#### Scenario: Entrada inválida na criação

```gherkin
When chega POST /projects com <entrada>
Then a resposta é 400
And nenhum projeto é criado

Examples:
  | entrada                                          |
  | name ausente                                     |
  | name ""                                          |
  | name de 101 caracteres                           |
  | name "Produto" e description de 501 caracteres   |
```

### Requirement: Listagem dos projetos da conta

O sistema SHALL expor `GET /projects`, que devolve `200` com os projetos da conta do token, ordenados por `created_at` do mais recente para o mais antigo.

O parâmetro `archived` SHALL filtrar a listagem: ausente devolve todos, `false` devolve os ativos e `true` devolve os arquivados. Valor diferente de `true` e `false` SHALL receber `400`.

#### Scenario: Listagem sem o parâmetro

```gherkin
Given uma conta com um projeto ativo e um projeto arquivado
When chega GET /projects com o token dessa conta
Then a resposta é 200 com os dois projetos
```

#### Scenario: Somente os ativos

```gherkin
Given uma conta com um projeto ativo e um projeto arquivado
When chega GET /projects com archived "false"
Then a resposta é 200 com o projeto ativo
And o projeto arquivado não aparece
```

#### Scenario: Somente os arquivados

```gherkin
Given uma conta com um projeto ativo e um projeto arquivado
When chega GET /projects com archived "true"
Then a resposta é 200 com o projeto arquivado
And o projeto ativo não aparece
```

#### Scenario: Ordem da listagem

```gherkin
Given uma conta com três projetos criados em sequência
When chega GET /projects com o token dessa conta
Then o primeiro item é o projeto criado por último
And o último item é o projeto criado primeiro
```

#### Scenario: Valor inválido em archived

```gherkin
When chega GET /projects com archived "talvez"
Then a resposta é 400
```

#### Scenario: Projeto de outra conta fora da lista

```gherkin
Given duas contas, cada uma com um projeto
When chega GET /projects com o token da primeira conta
Then a resposta traz somente o projeto da primeira conta
```

### Requirement: Consulta de um projeto

O sistema SHALL expor `GET /projects/{projectId}`, que devolve `200` com o projeto da conta do token, arquivado ou não.

#### Scenario: Projeto ativo

```gherkin
Given um projeto ativo da conta do token
When chega GET /projects/{projectId} desse projeto
Then a resposta é 200 com id, name, description, created_at, updated_at e archived_at
```

#### Scenario: Projeto arquivado

```gherkin
Given um projeto arquivado da conta do token
When chega GET /projects/{projectId} desse projeto
Then a resposta é 200
And archived_at traz o instante do arquivamento
```

### Requirement: Isolamento entre donos

Requisição a `GET /projects/{projectId}`, `PUT /projects/{projectId}`, `POST /projects/{projectId}/archive` ou `POST /projects/{projectId}/restore` de projeto que não pertence à conta do token SHALL receber `404`, com o mesmo `ProblemDetail` de projeto inexistente. A resposta SHALL NOT revelar que o projeto existe, e o projeto SHALL permanecer inalterado.

#### Scenario: Endpoint de projeto ativo alheio

```gherkin
Given um projeto ativo da segunda conta com name "Produto"
When chega <requisição> desse projeto com o token da primeira conta
Then a resposta é 404
And o corpo é igual ao da resposta de projeto inexistente
And o projeto continua com name "Produto" e archived_at null

Examples:
  | requisição                                   |
  | GET /projects/{projectId}                    |
  | PUT /projects/{projectId} com name "Outro"   |
  | POST /projects/{projectId}/archive           |
```

#### Scenario: Restauração de projeto alheio

```gherkin
Given um projeto arquivado da segunda conta
When chega POST /projects/{projectId}/restore desse projeto com o token da primeira conta
Then a resposta é 404
And archived_at continua preenchido
```

#### Scenario: Projeto inexistente

```gherkin
When chega GET /projects/{projectId} com um projectId que nunca existiu
Then a resposta é 404
```

### Requirement: Substituição do projeto

O sistema SHALL expor `PUT /projects/{projectId}`, que substitui `name` e `description` do projeto e responde `200` com o projeto atualizado. `description` ausente ou `null` SHALL gravar `null`, e `archived_at` SHALL NOT mudar.

#### Scenario: Projeto substituído

```gherkin
Given um projeto com name "Produto" e description "Time de produto"
When chega PUT /projects/{projectId} com name "Plataforma" e description "Outro texto"
Then a resposta é 200 com name "Plataforma" e description "Outro texto"
```

#### Scenario: Descrição omitida apagada

```gherkin
Given um projeto com description "Time de produto"
When chega PUT /projects/{projectId} com name "Produto" e sem o campo description
Then a resposta é 200
And description vem null
```

#### Scenario: Projeto arquivado editado

```gherkin
Given um projeto arquivado, com o archived_at guardado
When chega PUT /projects/{projectId} com name "Plataforma"
Then a resposta é 200 com name "Plataforma"
And archived_at continua igual ao guardado
```

### Requirement: Validação da entrada de substituição

`PUT /projects/{projectId}` com `name` ausente, vazio ou acima de 100 caracteres, ou com `description` acima de 500 caracteres, SHALL receber `400`. O projeto SHALL permanecer inalterado.

#### Scenario: Entrada inválida na substituição

```gherkin
Given um projeto com name "Produto" e description "Time de produto"
When chega PUT /projects/{projectId} com <entrada>
Then a resposta é 400
And o projeto continua com name "Produto" e description "Time de produto"

Examples:
  | entrada                                          |
  | name ausente                                     |
  | name ""                                          |
  | name de 101 caracteres                           |
  | name "Produto" e description de 501 caracteres   |
```

### Requirement: Arquivamento reversível

O sistema SHALL expor `POST /projects/{projectId}/archive`, que carimba `archived_at` com o instante do arquivamento, e `POST /projects/{projectId}/restore`, que grava `null` em `archived_at`. Os dois SHALL responder `200` com o projeto. Arquivar projeto arquivado ou restaurar projeto ativo SHALL responder `200` sem alterar `archived_at`.

Arquivar ou restaurar um projeto SHALL NOT alterar os quadros dele.

#### Scenario: Projeto arquivado

```gherkin
Given um projeto ativo
When chega POST /projects/{projectId}/archive
Then a resposta é 200 com archived_at preenchido
And o projeto deixa de aparecer na listagem com archived "false"
```

#### Scenario: Projeto restaurado

```gherkin
Given um projeto arquivado
When chega POST /projects/{projectId}/restore
Then a resposta é 200 com archived_at null
And o projeto volta a aparecer na listagem com archived "false"
```

#### Scenario: Arquivamento repetido

```gherkin
Given um projeto arquivado, com o archived_at guardado
When chega POST /projects/{projectId}/archive
Then a resposta é 200
And archived_at continua igual ao guardado
```

#### Scenario: Restauração de projeto ativo

```gherkin
Given um projeto ativo
When chega POST /projects/{projectId}/restore
Then a resposta é 200 com archived_at null
```

#### Scenario: Quadros intactos no arquivamento

```gherkin
Given um projeto ativo com um quadro ativo e um quadro arquivado, com o archived_at e o updated_at de cada quadro guardados
When chega POST /projects/{projectId}/archive
Then archived_at e updated_at de cada quadro continuam iguais aos guardados
```

#### Scenario: Quadros intactos na restauração

```gherkin
Given um projeto arquivado com um quadro arquivado, com o archived_at do quadro guardado
When chega POST /projects/{projectId}/restore
Then archived_at do quadro continua igual ao guardado
```

### Requirement: Carimbo de updated_at

O sistema SHALL atualizar `updated_at` a cada alteração de valor do projeto. Requisição que não altera nenhum valor SHALL deixar `updated_at` intacto.

#### Scenario: Alteração carimba updated_at

```gherkin
Given um projeto criado, com o updated_at guardado
When chega PUT /projects/{projectId} com name "Plataforma"
Then updated_at fica maior que o guardado
```

#### Scenario: Edição sem mudança de valor

```gherkin
Given um projeto com name "Produto" e description "Time de produto", com o updated_at guardado
When chega PUT /projects/{projectId} com name "Produto" e description "Time de produto"
Then updated_at continua igual ao guardado
```

#### Scenario: Criação preenche updated_at

```gherkin
When chega POST /projects com name "Produto"
Then updated_at vem preenchido na resposta
```

### Requirement: Projetos apagados com a conta

Apagar a linha da conta em `accounts` SHALL apagar os projetos dela, arquivados ou não. Nenhum projeto SHALL permanecer sem dono.

#### Scenario: Conta apagada

```gherkin
Given uma conta com um projeto ativo e um projeto arquivado
When a linha dessa conta é apagada de accounts
Then nenhum projeto dela permanece em projects
```

#### Scenario: Projeto de outra conta preservado

```gherkin
Given duas contas, cada uma com um projeto
When a linha da primeira conta é apagada de accounts
Then o projeto da segunda conta continua em projects
```

### Requirement: Endpoints documentados no OpenAPI

Os endpoints de projeto SHALL aparecer no documento OpenAPI exposto pela aplicação, com os códigos de resposta que produzem.

#### Scenario: OpenAPI lista os endpoints

```gherkin
When o documento OpenAPI é solicitado
Then ele descreve POST /projects, GET /projects, GET /projects/{projectId}, PUT /projects/{projectId}, POST /projects/{projectId}/archive e POST /projects/{projectId}/restore
And lista os códigos de resposta de cada endpoint
```
