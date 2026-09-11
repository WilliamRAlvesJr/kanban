## MODIFIED Requirements

Alterados nesta change; o restante de cada bloco é repetição da spec publicada.

- Criação de lane: descrição; cenários "Lane criada em quadro vazio" e "Lane criada no fim".
- Listagem das lanes do quadro: descrição; cenário "Links da listagem".
- Isolamento entre donos: cenário "Endpoint de lane em quadro alheio".
- Lane fora do quadro da URL: descrição; cenário "Lane de outro quadro".
- Substituição do nome: descrição; cenários "Nome substituído" e "Lane arquivada renomeada".
- Arquivamento reversível: descrição; cenários "Lane arquivada", "Lane restaurada", "Arquivamento repetido" e "Restauração de lane ativa".
- Reordenação das lanes ativas: descrição; cenários "Ordem regravada", "Lane arquivada fora da ordem" e "Quadro sem lanes ativas".
- Endpoints documentados no OpenAPI: cenário "OpenAPI lista os endpoints".

### Requirement: Criação de lane

O sistema SHALL expor `POST /boards/{boardId}/lanes`, que recebe `name` e cria no quadro uma lane ativa, com identificador `uuid` gerado pelo sistema e `position` igual à quantidade de lanes ativas do quadro.

A resposta de sucesso SHALL ser `201`. Lanes do mesmo quadro SHALL aceitar o mesmo `name`.

#### Scenario: Lane criada em quadro vazio

```gherkin
Given um quadro sem lanes da conta do token
When chega POST /boards/{boardId}/lanes com name "A fazer"
Then a resposta é 201
And GET no header Location traz id, name "A fazer", position 0, created_at, updated_at e archived_at null
```

#### Scenario: Lane criada no fim

```gherkin
Given um quadro com as lanes ativas "A fazer" em 0 e "Fazendo" em 1 e a lane arquivada "Descartadas"
When chega POST /boards/{boardId}/lanes com name "Feito"
Then a resposta é 201
And GET no header Location traz position 2
```

#### Scenario: Nome repetido no quadro

```gherkin
Given um quadro com a lane ativa "A fazer"
When chega POST /boards/{boardId}/lanes com name "A fazer"
Then a resposta é 201
And o quadro passa a ter duas lanes com name "A fazer"
```

### Requirement: Listagem das lanes do quadro

O sistema SHALL expor `GET /boards/{boardId}/lanes`, que devolve `200` com as lanes do quadro em `_embedded.lanes`: primeiro as ativas, por `position` crescente, depois as arquivadas, por `archived_at` da mais recente para a mais antiga.

O parâmetro `archived` SHALL filtrar a listagem: ausente devolve todas, `false` devolve as ativas e `true` devolve as arquivadas. Valor diferente de `true` e `false` SHALL receber `400`.

`_links` da listagem SHALL trazer `self` e `create-lane`, os dois com `href` `/boards/{boardId}/lanes`, `reorder-lanes`, com `href` `/boards/{boardId}/lanes/order`, e `board`, com `href` `/boards/{boardId}`.

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

#### Scenario: Links da listagem

```gherkin
Given um quadro da conta do token
When chega GET /boards/{boardId}/lanes desse quadro
Then _links traz somente self, create-lane, reorder-lanes e board
And self e create-lane têm href "/boards/{boardId}/lanes"
And reorder-lanes tem href "/boards/{boardId}/lanes/order"
And board tem href "/boards/{boardId}"
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
  | requisição                                                                |
  | POST /boards/{boardId}/lanes com name "Fazendo"                           |
  | GET /boards/{boardId}/lanes                                               |
  | GET /boards/{boardId}/lanes/{laneId} da lane "A fazer"                    |
  | PUT /boards/{boardId}/lanes/{laneId} da lane "A fazer" com name "Backlog" |
  | POST /boards/{boardId}/lanes/{laneId}/archive da lane "A fazer"           |
  | POST /boards/{boardId}/lanes/{laneId}/restore da lane "A fazer"           |
  | PUT /boards/{boardId}/lanes/order com lane_ids de "Feito" e "A fazer"     |
```

#### Scenario: Quadro inexistente

```gherkin
When chega GET /boards/{boardId}/lanes com um boardId que nunca existiu
Then a resposta é 404
```

### Requirement: Lane fora do quadro da URL

`GET /boards/{boardId}/lanes/{laneId}`, `PUT /boards/{boardId}/lanes/{laneId}`, `POST /boards/{boardId}/lanes/{laneId}/archive` e `POST /boards/{boardId}/lanes/{laneId}/restore` com `laneId` inexistente ou de outro quadro SHALL receber `404`. A lane SHALL permanecer inalterada.

#### Scenario: Lane de outro quadro

```gherkin
Given dois quadros da conta do token, com a lane ativa "Backlog" no segundo
When chega <requisição> com o boardId do primeiro quadro e o laneId de "Backlog"
Then a resposta é 404
And "Backlog" continua ativa, com name "Backlog"

Examples:
  | requisição                                                 |
  | GET /boards/{boardId}/lanes/{laneId}                       |
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

O sistema SHALL expor `PUT /boards/{boardId}/lanes/{laneId}`, que substitui `name` da lane, ativa ou arquivada, e responde `200`. `position` e `archived_at` SHALL NOT mudar.

#### Scenario: Nome substituído

```gherkin
Given um quadro com as lanes ativas "A fazer" em 0 e "Feito" em 1
When chega PUT /boards/{boardId}/lanes/{laneId} de "A fazer" com name "Backlog"
Then a resposta é 200
And GET /boards/{boardId}/lanes/{laneId} traz name "Backlog" e position 0
```

#### Scenario: Lane arquivada renomeada

```gherkin
Given uma lane arquivada "Descartadas", com o archived_at guardado
When chega PUT /boards/{boardId}/lanes/{laneId} com name "Canceladas"
Then a resposta é 200
And GET /boards/{boardId}/lanes/{laneId} traz name "Canceladas" e position null
And archived_at continua igual ao guardado
```

### Requirement: Arquivamento reversível

O sistema SHALL expor `POST /boards/{boardId}/lanes/{laneId}/archive`, que carimba `archived_at` com o instante do arquivamento, grava `null` em `position` e desce uma posição cada lane ativa do quadro que estava acima dela, e `POST /boards/{boardId}/lanes/{laneId}/restore`, que grava `null` em `archived_at` e grava em `position` a quantidade de lanes ativas do quadro.

Os dois SHALL responder `200`. Arquivar lane arquivada ou restaurar lane ativa SHALL responder `200` sem alterar nenhuma lane.

#### Scenario: Lane arquivada

```gherkin
Given um quadro com as lanes ativas "A fazer" em 0, "Fazendo" em 1 e "Feito" em 2
When chega POST /boards/{boardId}/lanes/{laneId}/archive de "Fazendo"
Then a resposta é 200
And GET /boards/{boardId}/lanes/{laneId} de "Fazendo" traz archived_at preenchido e position null
And "A fazer" continua em 0 e "Feito" passa para 1
```

#### Scenario: Lane restaurada

```gherkin
Given um quadro com as lanes ativas "A fazer" em 0 e "Feito" em 1 e a lane arquivada "Fazendo"
When chega POST /boards/{boardId}/lanes/{laneId}/restore de "Fazendo"
Then a resposta é 200
And GET /boards/{boardId}/lanes/{laneId} de "Fazendo" traz archived_at null e position 2
And "A fazer" continua em 0 e "Feito" continua em 1
```

#### Scenario: Arquivamento repetido

```gherkin
Given um quadro com a lane ativa "A fazer" em 0 e a lane arquivada "Fazendo", com o archived_at guardado
When chega POST /boards/{boardId}/lanes/{laneId}/archive de "Fazendo"
Then a resposta é 200
And "Fazendo" continua com position null
And archived_at continua igual ao guardado
And "A fazer" continua em 0
```

#### Scenario: Restauração de lane ativa

```gherkin
Given um quadro com as lanes ativas "A fazer" em 0 e "Feito" em 1
When chega POST /boards/{boardId}/lanes/{laneId}/restore de "A fazer"
Then a resposta é 200
And "A fazer" continua ativa, em 0
And "Feito" continua em 1
```

### Requirement: Reordenação das lanes ativas

O sistema SHALL expor `PUT /boards/{boardId}/lanes/order`, que recebe `lane_ids` com os ids das lanes ativas do quadro na nova ordem, grava em cada uma `position` igual ao índice dela na lista e responde `200`. Lanes arquivadas SHALL NOT mudar.

`lane_ids` ausente, com elemento `null` ou com id repetido SHALL receber `400`. Lista cujo conjunto de ids difere do conjunto de lanes ativas do quadro SHALL receber `409`. Nenhuma lane SHALL mudar nesses casos.

#### Scenario: Ordem regravada

```gherkin
Given um quadro com as lanes ativas "A fazer" em 0, "Fazendo" em 1 e "Feito" em 2
When chega PUT /boards/{boardId}/lanes/order com lane_ids de "Feito", "A fazer" e "Fazendo"
Then a resposta é 200
And "Feito" passa para 0, "A fazer" para 1 e "Fazendo" para 2
```

#### Scenario: Lane arquivada fora da ordem

```gherkin
Given um quadro com as lanes ativas "A fazer" em 0 e "Feito" em 1 e a lane arquivada "Descartadas"
When chega PUT /boards/{boardId}/lanes/order com lane_ids de "Feito" e "A fazer"
Then a resposta é 200
And "Feito" passa para 0 e "A fazer" para 1
And "Descartadas" continua arquivada, com position null
```

#### Scenario: Quadro sem lanes ativas

```gherkin
Given um quadro com a lane arquivada "Descartadas" e nenhuma lane ativa
When chega PUT /boards/{boardId}/lanes/order com lane_ids vazio
Then a resposta é 200
And "Descartadas" continua arquivada, com position null
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

### Requirement: Endpoints documentados no OpenAPI

Os endpoints de lane SHALL aparecer no documento OpenAPI exposto pela aplicação, com os códigos de resposta que produzem.

#### Scenario: OpenAPI lista os endpoints

```gherkin
When o documento OpenAPI é solicitado
Then ele descreve POST /boards/{boardId}/lanes, GET /boards/{boardId}/lanes, GET /boards/{boardId}/lanes/{laneId}, PUT /boards/{boardId}/lanes/{laneId}, POST /boards/{boardId}/lanes/{laneId}/archive, POST /boards/{boardId}/lanes/{laneId}/restore e PUT /boards/{boardId}/lanes/order
And lista os códigos de resposta de cada endpoint, com o 409 de PUT /boards/{boardId}/lanes/order
```

## ADDED Requirements

### Requirement: Consulta de uma lane

O sistema SHALL expor `GET /boards/{boardId}/lanes/{laneId}`, que devolve `200` com a lane do quadro da URL, ativa ou arquivada.

#### Scenario: Lane ativa

```gherkin
Given um quadro da conta do token com as lanes ativas "A fazer" em 0 e "Feito" em 1
When chega GET /boards/{boardId}/lanes/{laneId} de "Feito"
Then a resposta é 200 com id, name "Feito", position 1, created_at, updated_at e archived_at null
```

#### Scenario: Lane arquivada

```gherkin
Given um quadro da conta do token com a lane arquivada "Descartadas"
When chega GET /boards/{boardId}/lanes/{laneId} de "Descartadas"
Then a resposta é 200 com position null
And archived_at traz o instante do arquivamento
```

### Requirement: Links da lane

Toda lane em `GET /boards/{boardId}/lanes/{laneId}` e em `_embedded.lanes` SHALL trazer em `_links` as relações abaixo. `archive` SHALL aparecer só na lane com `archived_at` null, e `restore` só na lane arquivada.

| relação | href |
|---|---|
| `self` | `/boards/{boardId}/lanes/{id}` |
| `edit` | `/boards/{boardId}/lanes/{id}` |
| `archive` | `/boards/{boardId}/lanes/{id}/archive` |
| `restore` | `/boards/{boardId}/lanes/{id}/restore` |
| `board` | `/boards/{boardId}` |

#### Scenario: Links da lane ativa

```gherkin
Given uma lane ativa num quadro da conta do token
When chega GET /boards/{boardId}/lanes/{laneId} dessa lane
Then _links traz somente self, edit, archive e board
```

#### Scenario: Links da lane arquivada

```gherkin
Given uma lane arquivada num quadro da conta do token
When chega GET /boards/{boardId}/lanes/{laneId} dessa lane
Then _links traz somente self, edit, restore e board
```
