## Purpose

Lanes de um quadro do kanban, na ordem definida pelo dono, com criação, edição, arquivamento reversível e reordenação. Nenhuma conta enxerga nem altera lane de quadro de outra conta.

## Requirements

### Requirement: Criação de lane

O sistema SHALL expor `POST /boards/{boardId}/lanes`, que recebe `name` e cria no quadro uma lane ativa, com identificador `uuid` gerado pelo sistema e `position` igual à quantidade de lanes ativas do quadro.

A resposta de sucesso SHALL ser `201` com `id`, `name`, `position`, `created_at`, `updated_at` e `archived_at`. Lanes do mesmo quadro SHALL aceitar o mesmo `name`.

#### Scenario: Lane criada em quadro vazio

```gherkin
Given um quadro sem lanes da conta do token
When chega POST /boards/{boardId}/lanes com name "A fazer"
Then a resposta é 201 com id, name, position, created_at, updated_at e archived_at
And position vem 0
And archived_at vem null
```

#### Scenario: Lane criada no fim

```gherkin
Given um quadro com as lanes ativas "A fazer" em 0 e "Fazendo" em 1 e a lane arquivada "Descartadas"
When chega POST /boards/{boardId}/lanes com name "Feito"
Then a resposta é 201 com position 2
```

#### Scenario: Nome repetido no quadro

```gherkin
Given um quadro com a lane ativa "A fazer"
When chega POST /boards/{boardId}/lanes com name "A fazer"
Then a resposta é 201
And o quadro passa a ter duas lanes com name "A fazer"
```

### Requirement: Validação do nome da lane

`POST /boards/{boardId}/lanes` e `PUT /boards/{boardId}/lanes/{laneId}` com `name` ausente, vazio ou acima de 100 caracteres SHALL receber `400`. Nenhuma lane SHALL ser criada ou alterada nesses casos.

#### Scenario: Nome inválido na criação

```gherkin
Given um quadro sem lanes da conta do token
When chega POST /boards/{boardId}/lanes com <entrada>
Then a resposta é 400
And nenhuma lane é criada

Examples:
  | entrada                |
  | o campo name ausente   |
  | name ""                |
  | name de 101 caracteres |
```

#### Scenario: Nome inválido na substituição

```gherkin
Given uma lane com name "A fazer"
When chega PUT /boards/{boardId}/lanes/{laneId} com <entrada>
Then a resposta é 400
And a lane continua com name "A fazer"

Examples:
  | entrada                |
  | o campo name ausente   |
  | name ""                |
  | name de 101 caracteres |
```

### Requirement: Listagem das lanes do quadro

O sistema SHALL expor `GET /boards/{boardId}/lanes`, que devolve `200` com as lanes do quadro: primeiro as ativas, por `position` crescente, depois as arquivadas, por `archived_at` da mais recente para a mais antiga.

O parâmetro `archived` SHALL filtrar a listagem: ausente devolve todas, `false` devolve as ativas e `true` devolve as arquivadas. Valor diferente de `true` e `false` SHALL receber `400`.

#### Scenario: Ordem da listagem

```gherkin
Given um quadro com as lanes ativas "A fazer" em 0 e "Feito" em 1, com "Feito" criada antes
And as lanes "Antigas", arquivada primeiro, e "Descartadas", arquivada depois
When chega GET /boards/{boardId}/lanes
Then a resposta é 200 com "A fazer", "Feito", "Descartadas" e "Antigas", nessa ordem
```

#### Scenario: Somente as ativas

```gherkin
Given um quadro com as lanes ativas "A fazer" em 0 e "Feito" em 1 e a lane arquivada "Descartadas"
When chega GET /boards/{boardId}/lanes com archived "false"
Then a resposta é 200 com "A fazer" e "Feito", nessa ordem
And "Descartadas" não aparece
```

#### Scenario: Somente as arquivadas

```gherkin
Given um quadro com a lane ativa "A fazer" e as lanes "Antigas", arquivada primeiro, e "Descartadas", arquivada depois
When chega GET /boards/{boardId}/lanes com archived "true"
Then a resposta é 200 com "Descartadas" e "Antigas", nessa ordem
And "A fazer" não aparece
```

#### Scenario: Valor inválido em archived

```gherkin
Given um quadro da conta do token
When chega GET /boards/{boardId}/lanes com archived "talvez"
Then a resposta é 400
```

#### Scenario: Lane de outro quadro fora da lista

```gherkin
Given dois quadros da conta do token, com a lane "A fazer" no primeiro e a lane "Backlog" no segundo
When chega GET /boards/{boardId}/lanes do primeiro quadro
Then a resposta traz somente "A fazer"
```

### Requirement: Isolamento entre donos

Requisição a qualquer endpoint de lane em quadro que não pertence à conta do token SHALL receber `404`, com o mesmo `ProblemDetail` de quadro inexistente. A resposta SHALL NOT revelar que o quadro existe, e nenhuma lane SHALL ser criada ou alterada.

#### Scenario: Endpoint de lane em quadro alheio

```gherkin
Given um quadro da segunda conta com as lanes ativas "A fazer" em 0 e "Feito" em 1
When chega <requisição> nesse quadro com o token da primeira conta
Then a resposta é 404
And o corpo é igual ao da resposta de quadro inexistente
And as lanes do quadro continuam como estavam

Examples:
  | requisição                                                                 |
  | POST /boards/{boardId}/lanes com name "Fazendo"                            |
  | GET /boards/{boardId}/lanes                                                |
  | PUT /boards/{boardId}/lanes/{laneId} da lane "A fazer" com name "Backlog"  |
  | POST /boards/{boardId}/lanes/{laneId}/archive da lane "A fazer"            |
  | POST /boards/{boardId}/lanes/{laneId}/restore da lane "A fazer"            |
  | PUT /boards/{boardId}/lanes/order com lane_ids de "Feito" e "A fazer"      |
```

#### Scenario: Quadro inexistente

```gherkin
When chega GET /boards/{boardId}/lanes com um boardId que nunca existiu
Then a resposta é 404
```

### Requirement: Lane fora do quadro da URL

`PUT /boards/{boardId}/lanes/{laneId}`, `POST /boards/{boardId}/lanes/{laneId}/archive` e `POST /boards/{boardId}/lanes/{laneId}/restore` com `laneId` inexistente ou de outro quadro SHALL receber `404`. A lane SHALL permanecer inalterada.

#### Scenario: Lane de outro quadro

```gherkin
Given dois quadros da conta do token, com a lane ativa "Backlog" no segundo
When chega <requisição> com o boardId do primeiro quadro e o laneId de "Backlog"
Then a resposta é 404
And "Backlog" continua ativa, com name "Backlog"

Examples:
  | requisição                                                 |
  | PUT /boards/{boardId}/lanes/{laneId} com name "Canceladas" |
  | POST /boards/{boardId}/lanes/{laneId}/archive              |
  | POST /boards/{boardId}/lanes/{laneId}/restore              |
```

#### Scenario: Lane inexistente

```gherkin
Given um quadro da conta do token
When chega PUT /boards/{boardId}/lanes/{laneId} com um laneId que nunca existiu
Then a resposta é 404
```

### Requirement: Substituição do nome

O sistema SHALL expor `PUT /boards/{boardId}/lanes/{laneId}`, que substitui `name` da lane, ativa ou arquivada, e responde `200` com a lane. `position` e `archived_at` SHALL NOT mudar.

#### Scenario: Nome substituído

```gherkin
Given um quadro com as lanes ativas "A fazer" em 0 e "Feito" em 1
When chega PUT /boards/{boardId}/lanes/{laneId} de "A fazer" com name "Backlog"
Then a resposta é 200 com name "Backlog" e position 0
```

#### Scenario: Lane arquivada renomeada

```gherkin
Given uma lane arquivada "Descartadas", com o archived_at guardado
When chega PUT /boards/{boardId}/lanes/{laneId} com name "Canceladas"
Then a resposta é 200 com name "Canceladas" e position null
And archived_at continua igual ao guardado
```

### Requirement: Arquivamento reversível

O sistema SHALL expor `POST /boards/{boardId}/lanes/{laneId}/archive`, que carimba `archived_at` com o instante do arquivamento, grava `null` em `position` e desce uma posição cada lane ativa do quadro que estava acima dela, e `POST /boards/{boardId}/lanes/{laneId}/restore`, que grava `null` em `archived_at` e grava em `position` a quantidade de lanes ativas do quadro.

Os dois SHALL responder `200` com a lane. Arquivar lane arquivada ou restaurar lane ativa SHALL responder `200` sem alterar nenhuma lane.

#### Scenario: Lane arquivada

```gherkin
Given um quadro com as lanes ativas "A fazer" em 0, "Fazendo" em 1 e "Feito" em 2
When chega POST /boards/{boardId}/lanes/{laneId}/archive de "Fazendo"
Then a resposta é 200 com archived_at preenchido e position null
And "A fazer" continua em 0 e "Feito" passa para 1
```

#### Scenario: Lane restaurada

```gherkin
Given um quadro com as lanes ativas "A fazer" em 0 e "Feito" em 1 e a lane arquivada "Fazendo"
When chega POST /boards/{boardId}/lanes/{laneId}/restore de "Fazendo"
Then a resposta é 200 com archived_at null e position 2
And "A fazer" continua em 0 e "Feito" continua em 1
```

#### Scenario: Arquivamento repetido

```gherkin
Given um quadro com a lane ativa "A fazer" em 0 e a lane arquivada "Fazendo", com o archived_at guardado
When chega POST /boards/{boardId}/lanes/{laneId}/archive de "Fazendo"
Then a resposta é 200 com position null
And archived_at continua igual ao guardado
And "A fazer" continua em 0
```

#### Scenario: Restauração de lane ativa

```gherkin
Given um quadro com as lanes ativas "A fazer" em 0 e "Feito" em 1
When chega POST /boards/{boardId}/lanes/{laneId}/restore de "A fazer"
Then a resposta é 200 com archived_at null e position 0
And "Feito" continua em 1
```

### Requirement: Reordenação das lanes ativas

O sistema SHALL expor `PUT /boards/{boardId}/lanes/order`, que recebe `lane_ids` com os ids das lanes ativas do quadro na nova ordem, grava em cada uma `position` igual ao índice dela na lista e responde `200` com as lanes ativas por `position` crescente. Lanes arquivadas SHALL NOT mudar.

`lane_ids` ausente, com elemento `null` ou com id repetido SHALL receber `400`. Lista cujo conjunto de ids difere do conjunto de lanes ativas do quadro SHALL receber `409`. Nenhuma lane SHALL mudar nesses casos.

#### Scenario: Ordem regravada

```gherkin
Given um quadro com as lanes ativas "A fazer" em 0, "Fazendo" em 1 e "Feito" em 2
When chega PUT /boards/{boardId}/lanes/order com lane_ids de "Feito", "A fazer" e "Fazendo"
Then a resposta é 200 com "Feito" em 0, "A fazer" em 1 e "Fazendo" em 2, nessa ordem
```

#### Scenario: Lane arquivada fora da ordem

```gherkin
Given um quadro com as lanes ativas "A fazer" em 0 e "Feito" em 1 e a lane arquivada "Descartadas"
When chega PUT /boards/{boardId}/lanes/order com lane_ids de "Feito" e "A fazer"
Then a resposta é 200 com "Feito" em 0 e "A fazer" em 1
And "Descartadas" não aparece na resposta
And "Descartadas" continua arquivada, com position null
```

#### Scenario: Quadro sem lanes ativas

```gherkin
Given um quadro com a lane arquivada "Descartadas" e nenhuma lane ativa
When chega PUT /boards/{boardId}/lanes/order com lane_ids vazio
Then a resposta é 200 com a lista vazia
```

#### Scenario: Lista malformada

```gherkin
Given um quadro com as lanes ativas "A fazer" em 0 e "Feito" em 1
When chega PUT /boards/{boardId}/lanes/order com <lista>
Then a resposta é 400
And "A fazer" continua em 0 e "Feito" continua em 1

Examples:
  | lista                                    |
  | o campo lane_ids ausente                 |
  | lane_ids de "Feito" e null               |
  | lane_ids de "Feito", "A fazer" e "Feito" |
```

#### Scenario: Conjunto diferente das lanes ativas

```gherkin
Given um quadro com as lanes ativas "A fazer" em 0 e "Feito" em 1 e a lane arquivada "Descartadas"
And a lane "Backlog" em outro quadro da conta do token
When chega PUT /boards/{boardId}/lanes/order com <lista>
Then a resposta é 409
And "A fazer" continua em 0 e "Feito" continua em 1

Examples:
  | lista                                                    |
  | lane_ids somente de "Feito"                              |
  | lane_ids de "Feito", "A fazer" e "Backlog"               |
  | lane_ids de "Feito", "A fazer" e "Descartadas"           |
  | lane_ids de "Feito", "A fazer" e um id que nunca existiu |
```

### Requirement: Sequência de posições

As lanes ativas de um quadro SHALL ter `position` de `0` a `n - 1`, sem buraco e sem repetição, depois de qualquer combinação de requisições ao quadro, inclusive simultâneas. Lane arquivada SHALL ter `position` `null`.

#### Scenario: Criações simultâneas

```gherkin
Given um quadro sem lanes da conta do token
When chegam ao mesmo tempo dez POST /boards/{boardId}/lanes
Then as dez respostas são 201
And as lanes do quadro têm position de 0 a 9, sem repetição
```

#### Scenario: Arquivamento e criação simultâneos

```gherkin
Given um quadro com as lanes ativas "A fazer" em 0, "Fazendo" em 1 e "Feito" em 2
When chegam ao mesmo tempo POST /boards/{boardId}/lanes/{laneId}/archive de "Fazendo" e POST /boards/{boardId}/lanes com name "Revisão"
Then as duas respostas são 200 e 201
And as lanes ativas do quadro têm position de 0 a 2, sem repetição
```

### Requirement: Carimbo de updated_at

O sistema SHALL atualizar `updated_at` da lane quando `name` ou `archived_at` mudar. Mudança de `position` SHALL NOT atualizar `updated_at`, e requisição que não altera nenhum valor SHALL deixar `updated_at` intacto.

#### Scenario: Renomeação carimba updated_at

```gherkin
Given uma lane com name "A fazer", com o updated_at guardado
When chega PUT /boards/{boardId}/lanes/{laneId} com name "Backlog"
Then updated_at fica maior que o guardado
```

#### Scenario: Renomeação sem mudança de valor

```gherkin
Given uma lane com name "A fazer", com o updated_at guardado
When chega PUT /boards/{boardId}/lanes/{laneId} com name "A fazer"
Then updated_at continua igual ao guardado
```

#### Scenario: Arquivamento carimba updated_at

```gherkin
Given uma lane ativa, com o updated_at guardado
When chega POST /boards/{boardId}/lanes/{laneId}/archive
Then updated_at fica maior que o guardado
```

#### Scenario: Restauração carimba updated_at

```gherkin
Given uma lane arquivada, com o updated_at guardado
When chega POST /boards/{boardId}/lanes/{laneId}/restore
Then updated_at fica maior que o guardado
```

#### Scenario: Arquivamento sem mudança de valor

```gherkin
Given uma lane arquivada, com o updated_at guardado
When chega POST /boards/{boardId}/lanes/{laneId}/archive
Then updated_at continua igual ao guardado
```

#### Scenario: Lane deslocada pelo arquivamento

```gherkin
Given um quadro com as lanes ativas "A fazer" em 0 e "Feito" em 1, com o updated_at de "Feito" guardado
When chega POST /boards/{boardId}/lanes/{laneId}/archive de "A fazer"
Then "Feito" passa para 0
And o updated_at de "Feito" continua igual ao guardado
```

#### Scenario: Reordenação não carimba updated_at

```gherkin
Given um quadro com as lanes ativas "A fazer" em 0 e "Feito" em 1, com o updated_at das duas guardado
When chega PUT /boards/{boardId}/lanes/order com lane_ids de "Feito" e "A fazer"
Then o updated_at das duas continua igual ao guardado
```

### Requirement: Lanes apagadas com o quadro

Apagar a linha do quadro em `boards` SHALL apagar as lanes dele, ativas ou arquivadas. Nenhuma lane SHALL permanecer sem quadro.

#### Scenario: Quadro apagado

```gherkin
Given dois quadros, cada um com uma lane ativa e uma lane arquivada
When a linha do primeiro quadro é apagada de boards
Then nenhuma lane do primeiro quadro permanece em lanes
And as lanes do segundo quadro continuam em lanes
```

#### Scenario: Conta apagada

```gherkin
Given uma conta com um quadro que tem uma lane ativa e uma lane arquivada
When a linha dessa conta é apagada de accounts
Then nenhuma lane desse quadro permanece em lanes
```

### Requirement: Endpoints documentados no OpenAPI

Os endpoints de lane SHALL aparecer no documento OpenAPI exposto pela aplicação, com os códigos de resposta que produzem.

#### Scenario: OpenAPI lista os endpoints

```gherkin
When o documento OpenAPI é solicitado
Then ele descreve POST /boards/{boardId}/lanes, GET /boards/{boardId}/lanes, PUT /boards/{boardId}/lanes/{laneId}, POST /boards/{boardId}/lanes/{laneId}/archive, POST /boards/{boardId}/lanes/{laneId}/restore e PUT /boards/{boardId}/lanes/order
And lista os códigos de resposta de cada endpoint, com o 409 de PUT /boards/{boardId}/lanes/order
```
