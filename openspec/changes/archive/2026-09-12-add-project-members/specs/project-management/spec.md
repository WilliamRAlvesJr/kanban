## MODIFIED Requirements

Alterados nesta change; o restante de cada bloco é repetição da spec publicada.

- Listagem dos projetos da conta: descrição; cenários novos "Projeto de membro com view_project" e "Projeto de membro sem view_project".
- Consulta de um projeto: descrição.
- Links do projeto: descrição; cenários "Links do projeto ativo" e "Links do projeto arquivado"; cenário novo "Links conforme as permissões do membro".
- Isolamento entre donos: descrição.
- Endpoints documentados no OpenAPI: descrição; cenário "OpenAPI lista os endpoints".

### Requirement: Listagem dos projetos da conta

O sistema SHALL expor `GET /projects`, que devolve `200` com os projetos de que a conta do token é dona e os projetos em que ela é membro com a permissão `view_project`, em `_embedded.projects`, ordenados por `created_at` do mais recente para o mais antigo.

O parâmetro `archived` SHALL filtrar a listagem: ausente devolve todos, `false` devolve os ativos e `true` devolve os arquivados. Valor diferente de `true` e `false` SHALL receber `400`.

`_links` da listagem SHALL trazer `self` e `create-project`, os dois com `href` `/projects`.

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

#### Scenario: Projeto de membro com view_project

```gherkin
Given o projeto "Produto" da segunda conta, com a primeira conta como membro com a permissão "view_project"
When chega GET /projects com o token da primeira conta
Then a resposta traz o projeto "Produto"
```

#### Scenario: Projeto de membro sem view_project

```gherkin
Given o projeto "Produto" da segunda conta, com a primeira conta como membro com as permissões "edit_project" e "view_member"
When chega GET /projects com o token da primeira conta
Then o projeto "Produto" não aparece
```

#### Scenario: Links da listagem

```gherkin
Given uma conta com um projeto
When chega GET /projects com o token dessa conta
Then _links traz somente self e create-project, os dois com href "/projects"
```

### Requirement: Consulta de um projeto

O sistema SHALL expor `GET /projects/{projectId}`, que devolve `200` com o projeto, arquivado ou não.

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

### Requirement: Links do projeto

Todo projeto em `GET /projects/{projectId}` e em `_embedded.projects` SHALL trazer em `_links` a relação `self` e, das demais relações abaixo, só as dos endpoints que a conta do token pode chamar. `archive` SHALL aparecer só no projeto com `archived_at` null, e `restore` só no projeto arquivado.

| relação | href | endpoint |
|---|---|---|
| `self` | `/projects/{id}` | `GET /projects/{projectId}` |
| `edit` | `/projects/{id}` | `PUT /projects/{projectId}` |
| `archive` | `/projects/{id}/archive` | `POST /projects/{projectId}/archive` |
| `restore` | `/projects/{id}/restore` | `POST /projects/{projectId}/restore` |
| `boards` | `/projects/{id}/boards` | `GET /projects/{projectId}/boards` |
| `create-board` | `/projects/{id}/boards` | `POST /projects/{projectId}/boards` |
| `members` | `/projects/{id}/members` | `GET /projects/{projectId}/members` |
| `add-member` | `/projects/{id}/members` | `POST /projects/{projectId}/members` |

#### Scenario: Links do projeto ativo

```gherkin
Given um projeto ativo da conta do token
When chega GET /projects/{projectId} desse projeto
Then _links traz somente self, edit, archive, boards, create-board, members e add-member
```

#### Scenario: Links do projeto arquivado

```gherkin
Given um projeto arquivado da conta do token
When chega GET /projects/{projectId} desse projeto
Then _links traz somente self, edit, restore, boards, create-board, members e add-member
```

#### Scenario: Links conforme as permissões do membro

```gherkin
Given um projeto <estado> com um membro que tem as permissões <permissões>
When chega GET /projects/{projectId} desse projeto com o token do membro
Then _links traz somente <links>

Examples:
  | estado    | permissões                                                   | links                           |
  | ativo     | "view_project"                                               | self                            |
  | ativo     | "view_project", "archive_project" e "restore_project"        | self e archive                  |
  | arquivado | "view_project", "archive_project" e "restore_project"        | self e restore                  |
  | ativo     | "view_project", "edit_project", "view_boards" e "add_member" | self, edit, boards e add-member |
```

### Requirement: Isolamento entre donos

Requisição a `GET /projects/{projectId}`, `PUT /projects/{projectId}`, `POST /projects/{projectId}/archive` ou `POST /projects/{projectId}/restore` de projeto de que a conta do token não é dona nem membro SHALL receber `404`, com o mesmo `ProblemDetail` de projeto inexistente. A resposta SHALL NOT revelar que o projeto existe, e o projeto SHALL permanecer inalterado.

#### Scenario: Endpoint de projeto ativo alheio

```gherkin
Given um projeto ativo da segunda conta com name "Produto"
When chega <requisição> desse projeto com o token da primeira conta
Then a resposta é 404
And o corpo é igual ao da resposta de projeto inexistente
And o projeto continua com name "Produto" e archived_at null

Examples:
  | requisição                                 |
  | GET /projects/{projectId}                  |
  | PUT /projects/{projectId} com name "Outro" |
  | POST /projects/{projectId}/archive         |
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

### Requirement: Endpoints documentados no OpenAPI

Os endpoints de projeto SHALL aparecer no documento OpenAPI exposto pela aplicação, com os códigos de resposta que produzem. `GET /projects/{projectId}`, `PUT /projects/{projectId}`, `POST /projects/{projectId}/archive` e `POST /projects/{projectId}/restore` SHALL listar a resposta `403`.

#### Scenario: OpenAPI lista os endpoints

```gherkin
When o documento OpenAPI é solicitado
Then ele descreve POST /projects, GET /projects, GET /projects/{projectId}, PUT /projects/{projectId}, POST /projects/{projectId}/archive e POST /projects/{projectId}/restore
And lista os códigos de resposta de cada endpoint
And GET /projects/{projectId}, PUT /projects/{projectId}, POST /projects/{projectId}/archive e POST /projects/{projectId}/restore listam a resposta 403
```

## ADDED Requirements

### Requirement: Acesso de membro ao projeto

Os endpoints abaixo SHALL atender o membro do projeto que tem a permissão indicada com a mesma resposta dada ao dono. Membro sem a permissão SHALL receber `403`, e o projeto SHALL permanecer inalterado.

| endpoint | permissão |
|---|---|
| `GET /projects/{projectId}` | `view_project` |
| `PUT /projects/{projectId}` | `edit_project` |
| `POST /projects/{projectId}/archive` | `archive_project` |
| `POST /projects/{projectId}/restore` | `restore_project` |

#### Scenario: Membro com a permissão do endpoint

```gherkin
Given o projeto "Produto" <estado>, com um membro que tem somente a permissão <permissão>
When chega <requisição> com o token do membro
Then a resposta é 200

Examples:
  | estado    | permissão       | requisição                                      |
  | ativo     | view_project    | GET /projects/{projectId}                       |
  | ativo     | edit_project    | PUT /projects/{projectId} com name "Plataforma" |
  | ativo     | archive_project | POST /projects/{projectId}/archive              |
  | arquivado | restore_project | POST /projects/{projectId}/restore              |
```

#### Scenario: Membro sem a permissão do endpoint

```gherkin
Given o projeto "Produto" <estado>, com o archived_at guardado, e um membro com todas as permissões de projeto menos <permissão>
When chega <requisição> com o token do membro
Then a resposta é 403
And o projeto continua com name "Produto" e o archived_at guardado

Examples:
  | estado    | permissão       | requisição                                      |
  | ativo     | view_project    | GET /projects/{projectId}                       |
  | ativo     | edit_project    | PUT /projects/{projectId} com name "Plataforma" |
  | ativo     | archive_project | POST /projects/{projectId}/archive              |
  | arquivado | restore_project | POST /projects/{projectId}/restore              |
```
