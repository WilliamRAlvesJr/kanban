## MODIFIED Requirements

Alterados nesta change; o restante de cada bloco é repetição da spec publicada.

- Listagem dos quadros do projeto: descrição; cenário novo "Links da listagem para membro".
- Links do quadro: descrição; cenário novo "Links do quadro para membro".
- Isolamento entre donos: descrição; cenário novo "Quadro de projeto em que a conta é membro".
- Movimentação de quadro: descrição; cenário novo "Destino em que a conta é membro".
- Endpoints documentados no OpenAPI: descrição; cenário "OpenAPI lista os endpoints".

### Requirement: Listagem dos quadros do projeto

O sistema SHALL expor `GET /projects/{projectId}/boards`, que devolve `200` com os quadros do projeto em `_embedded.boards`, ordenados por `created_at` do mais recente para o mais antigo.

O parâmetro `archived` SHALL filtrar a listagem: ausente devolve todos, `false` devolve os ativos e `true` devolve os arquivados. Valor diferente de `true` e `false` SHALL receber `400`.

`_links` da listagem SHALL trazer `self`, com `href` `/projects/{projectId}/boards`; `create-board`, com o mesmo `href`, só para a conta que pode chamar `POST /projects/{projectId}/boards`; e `project`, com `href` `/projects/{projectId}`, só para a conta que pode chamar `GET /projects/{projectId}`.

#### Scenario: Listagem sem o parâmetro

```gherkin
Given um projeto com um quadro ativo e um quadro arquivado
When chega GET /projects/{projectId}/boards desse projeto
Then a resposta é 200 com os dois quadros
```

#### Scenario: Somente os ativos

```gherkin
Given um projeto com um quadro ativo e um quadro arquivado
When chega GET /projects/{projectId}/boards com archived "false"
Then a resposta é 200 com o quadro ativo
And o quadro arquivado não aparece
```

#### Scenario: Somente os arquivados

```gherkin
Given um projeto com um quadro ativo e um quadro arquivado
When chega GET /projects/{projectId}/boards com archived "true"
Then a resposta é 200 com o quadro arquivado
And o quadro ativo não aparece
```

#### Scenario: Ordem da listagem

```gherkin
Given um projeto com três quadros criados em sequência
When chega GET /projects/{projectId}/boards desse projeto
Then o primeiro item é o quadro criado por último
And o último item é o quadro criado primeiro
```

#### Scenario: Valor inválido em archived

```gherkin
Given um projeto da conta do token
When chega GET /projects/{projectId}/boards com archived "talvez"
Then a resposta é 400
```

#### Scenario: Quadro de outro projeto fora da lista

```gherkin
Given uma conta com dois projetos, cada um com um quadro
When chega GET /projects/{projectId}/boards do primeiro projeto
Then a resposta traz somente o quadro do primeiro projeto
```

#### Scenario: Links da listagem

```gherkin
Given um projeto da conta do token
When chega GET /projects/{projectId}/boards desse projeto
Then _links traz somente self, create-board e project
And self e create-board têm href "/projects/{projectId}/boards"
And project tem href "/projects/{projectId}"
```

#### Scenario: Links da listagem para membro

```gherkin
Given um projeto com um membro que tem somente a permissão "view_boards"
When chega GET /projects/{projectId}/boards desse projeto com o token do membro
Then _links traz somente self
```

### Requirement: Links do quadro

Todo quadro em `GET /boards/{id}` e em `_embedded.boards` SHALL trazer em `_links` a relação `self` e, das demais relações abaixo, só as dos endpoints que a conta do token pode chamar. `archive` SHALL aparecer só no quadro com `archived_at` null, e `restore` só no quadro arquivado.

| relação | href | endpoint |
|---|---|---|
| `self` | `/boards/{id}` | `GET /boards/{id}` |
| `edit` | `/boards/{id}` | `PUT /boards/{id}` |
| `archive` | `/boards/{id}/archive` | `POST /boards/{id}/archive` |
| `restore` | `/boards/{id}/restore` | `POST /boards/{id}/restore` |
| `move` | `/boards/{id}/move` | `POST /boards/{id}/move` |
| `project` | `/projects/{project_id}` | `GET /projects/{projectId}` |
| `lanes` | `/boards/{id}/lanes` | `GET /boards/{boardId}/lanes` |
| `create-lane` | `/boards/{id}/lanes` | `POST /boards/{boardId}/lanes` |
| `reorder-lanes` | `/boards/{id}/lanes/order` | `PUT /boards/{boardId}/lanes/order` |

#### Scenario: Links do quadro ativo

```gherkin
Given um quadro ativo da conta do token
When chega GET /boards/{id} desse quadro
Then _links traz somente self, edit, archive, move, project, lanes, create-lane e reorder-lanes
```

#### Scenario: Links do quadro arquivado

```gherkin
Given um quadro arquivado da conta do token
When chega GET /boards/{id} desse quadro
Then _links traz somente self, edit, restore, move, project, lanes, create-lane e reorder-lanes
```

#### Scenario: Link do projeto depois da movimentação

```gherkin
Given um quadro movido do projeto "Produto" para o projeto "Operações"
When chega GET /boards/{id} desse quadro
Then _links.project.href é "/projects/{id de Operações}"
```

#### Scenario: Links do quadro para membro

```gherkin
Given um projeto com um quadro ativo e um membro que tem as permissões <permissões>
When chega GET /projects/{projectId}/boards desse projeto com o token do membro
Then o quadro em _embedded.boards traz em _links somente <links>

Examples:
  | permissões                                                   | links          |
  | "view_boards"                                                | self           |
  | "view_boards" e "view_project"                               | self e project |
  | "view_boards", "view_project", "edit_project" e "add_boards" | self e project |
```

### Requirement: Isolamento entre donos

Um quadro SHALL pertencer à conta dona do projeto dele. Requisição a `GET /boards/{id}`, `PUT /boards/{id}`, `POST /boards/{id}/archive`, `POST /boards/{id}/restore` ou `POST /boards/{id}/move` de quadro que não pertence à conta do token SHALL receber `404`, com o mesmo `ProblemDetail` de quadro inexistente, inclusive quando a conta é membro do projeto do quadro. A resposta SHALL NOT revelar que o quadro existe.

Requisição a `POST /projects/{projectId}/boards` ou `GET /projects/{projectId}/boards` com projeto de que a conta do token não é dona nem membro SHALL receber `404`, com o mesmo `ProblemDetail` de projeto inexistente, e nenhum quadro SHALL ser criado.

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

#### Scenario: Movimentação de quadro alheio

```gherkin
Given um quadro da segunda conta e um projeto da primeira conta
When chega POST /boards/{id}/move desse quadro com project_id do projeto da primeira conta e o token da primeira conta
Then a resposta é 404
And o corpo é igual ao da resposta de quadro inexistente
And o quadro continua no projeto da segunda conta
```

#### Scenario: Quadro inexistente

```gherkin
When chega GET /boards/{id} com um id que nunca existiu
Then a resposta é 404
```

#### Scenario: Quadro de projeto em que a conta é membro

```gherkin
Given um quadro do projeto "Produto" da segunda conta, com a primeira conta como membro de "Produto" com todas as permissões de projeto
When chega GET /boards/{id} desse quadro com o token da primeira conta
Then a resposta é 404
And o corpo é igual ao da resposta de quadro inexistente
```

#### Scenario: Criação em projeto alheio

```gherkin
Given um projeto da segunda conta
When chega POST /projects/{projectId}/boards desse projeto com o token da primeira conta
Then a resposta é 404
And o corpo é igual ao da resposta de projeto inexistente
And nenhum quadro é criado
```

#### Scenario: Listagem de projeto alheio

```gherkin
Given um projeto da segunda conta com um quadro
When chega GET /projects/{projectId}/boards desse projeto com o token da primeira conta
Then a resposta é 404
And o corpo é igual ao da resposta de projeto inexistente
```

#### Scenario: Projeto inexistente

```gherkin
When chega POST /projects/{projectId}/boards com um projectId que nunca existiu
Then a resposta é 404
```

### Requirement: Movimentação de quadro

O sistema SHALL expor `POST /boards/{id}/move`, que recebe `project_id`, grava o quadro no projeto de destino e responde `200`. As lanes SHALL acompanhar o quadro, e `name`, `description` e `archived_at` SHALL NOT mudar. Projeto de destino arquivado SHALL aceitar o quadro, e destino igual ao projeto atual SHALL responder `200` sem alterar o quadro.

`project_id` ausente, `null` ou fora do formato `uuid` SHALL receber `400`. Destino inexistente ou de que a conta do token não é dona SHALL receber `404`, com o mesmo `ProblemDetail` de projeto inexistente, inclusive quando a conta é membro do destino. Nos dois casos o quadro SHALL permanecer no projeto atual.

#### Scenario: Quadro movido

```gherkin
Given uma conta com os projetos "Produto" e "Operações" e um quadro em "Produto" com as lanes "A fazer" e "Feito"
When chega POST /boards/{id}/move com project_id de "Operações"
Then a resposta é 200
And GET /boards/{id} traz project_id de "Operações"
And o quadro aparece na listagem de "Operações" e não aparece na de "Produto"
And as lanes do quadro continuam "A fazer" e "Feito"
```

#### Scenario: Destino arquivado

```gherkin
Given um quadro num projeto ativo e um projeto arquivado da mesma conta
When chega POST /boards/{id}/move com project_id do projeto arquivado
Then a resposta é 200
And GET /boards/{id} traz project_id do projeto arquivado
```

#### Scenario: Destino igual ao projeto atual

```gherkin
Given um quadro no projeto "Produto", com o updated_at guardado
When chega POST /boards/{id}/move com project_id de "Produto"
Then a resposta é 200
And GET /boards/{id} traz project_id de "Produto"
And updated_at continua igual ao guardado
```

#### Scenario: Entrada inválida na movimentação

```gherkin
Given um quadro no projeto "Produto"
When chega POST /boards/{id}/move com <entrada>
Then a resposta é 400
And o quadro continua em "Produto"

Examples:
  | entrada                      |
  | corpo sem o campo project_id |
  | project_id null              |
  | project_id "abc"             |
```

#### Scenario: Destino de outra conta

```gherkin
Given um quadro no projeto "Produto" da primeira conta e um projeto da segunda conta
When chega POST /boards/{id}/move com project_id do projeto da segunda conta e o token da primeira conta
Then a resposta é 404
And o corpo é igual ao da resposta de projeto inexistente
And o quadro continua em "Produto"
```

#### Scenario: Destino em que a conta é membro

```gherkin
Given um quadro no projeto "Produto" da primeira conta e o projeto "Operações" da segunda conta, com a primeira conta como membro de "Operações" com todas as permissões de projeto
When chega POST /boards/{id}/move com project_id de "Operações" e o token da primeira conta
Then a resposta é 404
And o corpo é igual ao da resposta de projeto inexistente
And o quadro continua em "Produto"
```

#### Scenario: Destino inexistente

```gherkin
Given um quadro no projeto "Produto"
When chega POST /boards/{id}/move com um project_id que nunca existiu
Then a resposta é 404
And o quadro continua em "Produto"
```

### Requirement: Endpoints documentados no OpenAPI

Os endpoints de quadro SHALL aparecer no documento OpenAPI exposto pela aplicação, com os códigos de resposta que produzem. `POST /projects/{projectId}/boards` e `GET /projects/{projectId}/boards` SHALL listar a resposta `403`.

#### Scenario: OpenAPI lista os endpoints

```gherkin
When o documento OpenAPI é solicitado
Then ele descreve POST /projects/{projectId}/boards, GET /projects/{projectId}/boards, GET /boards/{id}, PUT /boards/{id}, POST /boards/{id}/archive, POST /boards/{id}/restore e POST /boards/{id}/move
And lista os códigos de resposta de cada endpoint
And POST /projects/{projectId}/boards e GET /projects/{projectId}/boards listam a resposta 403
```

## ADDED Requirements

### Requirement: Acesso de membro aos quadros do projeto

`POST /projects/{projectId}/boards` SHALL atender o membro do projeto com a permissão `add_boards`, e `GET /projects/{projectId}/boards` o membro com a permissão `view_boards`, com a mesma resposta dada ao dono. Membro sem a permissão do endpoint SHALL receber `403`, e nenhum quadro SHALL ser criado.

#### Scenario: Criação por membro com add_boards

```gherkin
Given o projeto "Produto", com um membro que tem somente a permissão "add_boards"
When chega POST /projects/{projectId}/boards com name "Sprint 12" e o token do membro
Then a resposta é 201
And a listagem de quadros de "Produto" traz o quadro "Sprint 12"
And GET no header Location com o token do membro responde 404
```

#### Scenario: Listagem por membro com view_boards

```gherkin
Given o projeto "Produto", com os quadros "Sprint 12" e "Sprint 13" e um membro que tem somente a permissão "view_boards"
When chega GET /projects/{projectId}/boards com o token do membro
Then a resposta é 200 com os dois quadros
```

#### Scenario: Criação por membro sem add_boards

```gherkin
Given o projeto "Produto", com um membro que tem todas as permissões de projeto menos "add_boards"
When chega POST /projects/{projectId}/boards com name "Sprint 12" e o token do membro
Then a resposta é 403
And nenhum quadro é criado
```

#### Scenario: Listagem por membro sem view_boards

```gherkin
Given o projeto "Produto", com um quadro e um membro que tem todas as permissões de projeto menos "view_boards"
When chega GET /projects/{projectId}/boards com o token do membro
Then a resposta é 403
```
