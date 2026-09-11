## MODIFIED Requirements

Alterados nesta change; o restante de cada bloco é repetição da spec publicada.

- Criação de quadro no projeto: descrição; cenários "Quadro criado" e "Quadro sem descrição".
- Listagem dos quadros do projeto: descrição; cenário "Links da listagem".
- Substituição do quadro: descrição; cenários "Quadro substituído", "Descrição omitida apagada" e "Quadro arquivado editado".
- Arquivamento reversível: descrição; cenários "Quadro arquivado", "Quadro restaurado" e "Restauração de quadro ativo".
- Movimentação de quadro: descrição; cenários "Quadro movido", "Destino arquivado" e "Destino igual ao projeto atual".
- Carimbo de updated_at: cenário "Criação preenche updated_at".

### Requirement: Criação de quadro no projeto

O sistema SHALL expor `POST /projects/{projectId}/boards`, que recebe `name` e `description` e cria no projeto da URL um quadro com identificador `uuid` gerado pelo sistema. Projeto arquivado SHALL aceitar a criação.

A resposta de sucesso SHALL ser `201`.

#### Scenario: Quadro criado

```gherkin
Given um projeto da conta do token
When chega POST /projects/{projectId}/boards com name "Sprint 12" e description "Trabalho da sprint"
Then a resposta é 201
And GET no header Location traz id, project_id do projeto da URL, name "Sprint 12", description "Trabalho da sprint", created_at, updated_at e archived_at null
```

#### Scenario: Quadro sem descrição

```gherkin
Given um projeto da conta do token
When chega POST /projects/{projectId}/boards com name "Sprint 12" e sem o campo description
Then a resposta é 201
And GET no header Location traz description null
```

#### Scenario: Quadro no projeto da URL

```gherkin
Given uma conta com dois projetos
When chega POST /projects/{projectId}/boards com o id do segundo projeto
Then a coluna project_id guarda o id do segundo projeto
```

#### Scenario: Quadro em projeto arquivado

```gherkin
Given um projeto arquivado da conta do token
When chega POST /projects/{projectId}/boards com name "Sprint 12"
Then a resposta é 201
And o projeto continua com archived_at preenchido
```

### Requirement: Listagem dos quadros do projeto

O sistema SHALL expor `GET /projects/{projectId}/boards`, que devolve `200` com os quadros do projeto em `_embedded.boards`, ordenados por `created_at` do mais recente para o mais antigo.

O parâmetro `archived` SHALL filtrar a listagem: ausente devolve todos, `false` devolve os ativos e `true` devolve os arquivados. Valor diferente de `true` e `false` SHALL receber `400`.

`_links` da listagem SHALL trazer `self` e `create-board`, os dois com `href` `/projects/{projectId}/boards`, e `project`, com `href` `/projects/{projectId}`.

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

### Requirement: Substituição do quadro

O sistema SHALL expor `PUT /boards/{id}`, que substitui `name` e `description` do quadro e responde `200`. `description` ausente ou `null` SHALL gravar `null`, e `archived_at` SHALL NOT mudar.

#### Scenario: Quadro substituído

```gherkin
Given um quadro com name "Sprint 12" e description "Trabalho da sprint"
When chega PUT /boards/{id} com name "Sprint 13" e description "Outro texto"
Then a resposta é 200
And GET /boards/{id} traz name "Sprint 13" e description "Outro texto"
```

#### Scenario: Descrição omitida apagada

```gherkin
Given um quadro com description "Trabalho da sprint"
When chega PUT /boards/{id} com name "Sprint 12" e sem o campo description
Then a resposta é 200
And GET /boards/{id} traz description null
```

#### Scenario: Quadro arquivado editado

```gherkin
Given um quadro arquivado, com o archived_at guardado
When chega PUT /boards/{id} com name "Sprint 13"
Then a resposta é 200
And GET /boards/{id} traz name "Sprint 13"
And archived_at continua igual ao guardado
```

### Requirement: Arquivamento reversível

O sistema SHALL expor `POST /boards/{id}/archive`, que carimba `archived_at` com o instante do arquivamento, e `POST /boards/{id}/restore`, que grava `null` em `archived_at`. Os dois SHALL responder `200`. Arquivar quadro arquivado ou restaurar quadro ativo SHALL responder `200` sem alterar `archived_at`.

#### Scenario: Quadro arquivado

```gherkin
Given um quadro ativo
When chega POST /boards/{id}/archive
Then a resposta é 200
And GET /boards/{id} traz archived_at preenchido
And o quadro deixa de aparecer na listagem com archived "false"
```

#### Scenario: Quadro restaurado

```gherkin
Given um quadro arquivado
When chega POST /boards/{id}/restore
Then a resposta é 200
And GET /boards/{id} traz archived_at null
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
Then a resposta é 200
And GET /boards/{id} traz archived_at null
```

### Requirement: Movimentação de quadro

O sistema SHALL expor `POST /boards/{id}/move`, que recebe `project_id`, grava o quadro no projeto de destino e responde `200`. As lanes SHALL acompanhar o quadro, e `name`, `description` e `archived_at` SHALL NOT mudar. Projeto de destino arquivado SHALL aceitar o quadro, e destino igual ao projeto atual SHALL responder `200` sem alterar o quadro.

`project_id` ausente, `null` ou fora do formato `uuid` SHALL receber `400`. Destino inexistente ou de outra conta SHALL receber `404`, com o mesmo `ProblemDetail` de projeto inexistente. Nos dois casos o quadro SHALL permanecer no projeto atual.

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

#### Scenario: Destino inexistente

```gherkin
Given um quadro no projeto "Produto"
When chega POST /boards/{id}/move com um project_id que nunca existiu
Then a resposta é 404
And o quadro continua em "Produto"
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
Given um projeto da conta do token
When chega POST /projects/{projectId}/boards com name "Sprint 12"
Then GET no header Location traz updated_at preenchido
```

#### Scenario: Movimentação carimba updated_at

```gherkin
Given um quadro no primeiro de dois projetos da conta, com o updated_at guardado
When chega POST /boards/{id}/move com project_id do segundo projeto
Then updated_at fica maior que o guardado
```

## ADDED Requirements

### Requirement: Links do quadro

Todo quadro em `GET /boards/{id}` e em `_embedded.boards` SHALL trazer em `_links` as relações abaixo. `archive` SHALL aparecer só no quadro com `archived_at` null, e `restore` só no quadro arquivado.

| relação | href |
|---|---|
| `self` | `/boards/{id}` |
| `edit` | `/boards/{id}` |
| `archive` | `/boards/{id}/archive` |
| `restore` | `/boards/{id}/restore` |
| `move` | `/boards/{id}/move` |
| `project` | `/projects/{project_id}` |
| `lanes` | `/boards/{id}/lanes` |
| `create-lane` | `/boards/{id}/lanes` |
| `reorder-lanes` | `/boards/{id}/lanes/order` |

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
