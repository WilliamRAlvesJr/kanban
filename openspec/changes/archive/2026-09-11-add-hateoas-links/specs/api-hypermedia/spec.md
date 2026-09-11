## Purpose

Formato hipermídia das respostas da API do kanban: representação HAL, coleções em `_embedded`, respostas de escrita só com links e o ponto de entrada `GET /`, de onde o cliente navega sem conhecer as rotas de antemão.

## ADDED Requirements

### Requirement: Representação HAL

Toda resposta de sucesso com corpo SHALL sair em HAL. Requisição sem o header `Accept` ou com `Accept` `application/hal+json` SHALL receber `Content-Type` `application/hal+json`.

`_links` SHALL ser um objeto em que cada relação aponta para um objeto com `href`. Todo `href` SHALL ser um caminho relativo à raiz da aplicação, iniciado por `/`, sem esquema e sem host.

#### Scenario: Conteúdo em HAL

```gherkin
Given um projeto da conta do token
When chega GET /projects/{projectId} desse projeto sem o header Accept
Then o header Content-Type é "application/hal+json"
And _links.self.href é "/projects/{projectId}"
```

#### Scenario: Accept HAL

```gherkin
Given um projeto da conta do token
When chega GET /projects/{projectId} desse projeto com o header Accept "application/hal+json"
Then a resposta é 200
And o header Content-Type é "application/hal+json"
```

### Requirement: Coleções em _embedded

Toda listagem SHALL responder `200` com um objeto que traz os itens, na ordem da listagem, no array `_embedded.projects`, `_embedded.boards` ou `_embedded.lanes`. Cada item SHALL trazer os mesmos campos e os mesmos `_links` da consulta individual do recurso. Listagem sem item SHALL trazer o array vazio.

`_links.self.href` da listagem SHALL ser o caminho da requisição, sem query string.

#### Scenario: Item igual à consulta individual

```gherkin
Given uma conta com um projeto
When chega GET /projects com o token dessa conta
Then _embedded.projects traz um item
And o item é igual ao corpo de GET /projects/{projectId} desse projeto
```

#### Scenario: Coleção vazia

```gherkin
Given uma conta sem projetos
When chega GET /projects com o token dessa conta
Then a resposta é 200
And _embedded.projects é um array vazio
```

#### Scenario: Self sem query string

```gherkin
Given uma conta com um projeto arquivado
When chega GET /projects com archived "true"
Then _links.self.href é "/projects"
```

### Requirement: Escrita responde só com links

Os endpoints de escrita de projeto, quadro e lane SHALL responder com corpo só com `_links`, sem nenhum campo do recurso, e com `_links.self.href` apontando para o recurso afetado. O código de status de cada endpoint é o da spec da capability dele.

As criações SHALL responder com o header `Location` igual a `_links.self.href`.

#### Scenario: Self da escrita

```gherkin
Given o projeto "Produto", com o quadro "Sprint 12", que tem a lane ativa "A fazer"
When chega <requisição> com o token do dono
Then o corpo traz somente _links
And _links.self.href é <self>

Examples:
  | requisição                                                           | self                                          |
  | POST /projects com name "Plataforma"                                 | "/projects/{id do projeto criado}"            |
  | PUT /projects/{projectId} de "Produto" com name "Plataforma"         | "/projects/{projectId}"                       |
  | POST /projects/{projectId}/archive de "Produto"                      | "/projects/{projectId}"                       |
  | POST /projects/{projectId}/restore de "Produto"                      | "/projects/{projectId}"                       |
  | POST /projects/{projectId}/boards com name "Sprint 13"               | "/boards/{id do quadro criado}"               |
  | PUT /boards/{id} de "Sprint 12" com name "Sprint 13"                 | "/boards/{id}"                                |
  | POST /boards/{id}/archive de "Sprint 12"                             | "/boards/{id}"                                |
  | POST /boards/{id}/restore de "Sprint 12"                             | "/boards/{id}"                                |
  | POST /boards/{id}/move de "Sprint 12" com project_id de "Produto"    | "/boards/{id}"                                |
  | POST /boards/{boardId}/lanes com name "Feito"                        | "/boards/{boardId}/lanes/{id da lane criada}" |
  | PUT /boards/{boardId}/lanes/{laneId} de "A fazer" com name "Backlog" | "/boards/{boardId}/lanes/{laneId}"            |
  | POST /boards/{boardId}/lanes/{laneId}/archive de "A fazer"           | "/boards/{boardId}/lanes/{laneId}"            |
  | POST /boards/{boardId}/lanes/{laneId}/restore de "A fazer"           | "/boards/{boardId}/lanes/{laneId}"            |
  | PUT /boards/{boardId}/lanes/order com lane_ids de "A fazer"          | "/boards/{boardId}/lanes"                     |
```

#### Scenario: Location da criação

```gherkin
Given o projeto "Produto", com o quadro "Sprint 12"
When chega <requisição> com o token do dono
Then o header Location é igual a _links.self.href

Examples:
  | requisição                                             |
  | POST /projects com name "Plataforma"                   |
  | POST /projects/{projectId}/boards com name "Sprint 13" |
  | POST /boards/{boardId}/lanes com name "A fazer"        |
```

### Requirement: Erro sem links

Resposta de erro SHALL continuar em `ProblemDetail`, com `Content-Type` `application/problem+json` e sem `_links`.

#### Scenario: Recurso inexistente

```gherkin
When chega GET /projects/{projectId} com um projectId que nunca existiu
Then a resposta é 404 com o header Content-Type "application/problem+json"
And o corpo não traz _links
```

### Requirement: Ponto de entrada da API

O sistema SHALL expor `GET /`, que responde `200` com corpo só com `_links`.

Requisição com token válido SHALL receber `self`, `me`, `projects`, `create-project` e `logout`. Requisição sem token, com token desconhecido, com token expirado ou com header `Authorization` em outro formato SHALL receber `self`, `login` e `create-account`, e SHALL NOT receber `401`.

| relação | href |
|---|---|
| `self` | `/` |
| `me` | `/accounts/me` |
| `projects` | `/projects` |
| `create-project` | `/projects` |
| `logout` | `/auth/logout` |
| `login` | `/auth/login` |
| `create-account` | `/accounts` |

#### Scenario: Entrada sem token

```gherkin
When chega GET / sem o header Authorization
Then a resposta é 200
And _links traz somente self, login e create-account
```

#### Scenario: Entrada com token válido

```gherkin
Given um token válido
When chega GET / com esse token
Then a resposta é 200
And _links traz somente self, me, projects, create-project e logout
```

#### Scenario: Entrada com token inválido

```gherkin
Given <token>
When chega GET / com o header Authorization desse token
Then a resposta é 200
And _links traz somente self, login e create-account

Examples:
  | token                               |
  | um token que nunca foi emitido      |
  | um token cujo expires_at já passou  |
  | o header Authorization "Basic YWJj" |
```

### Requirement: Ponto de entrada documentado no OpenAPI

`GET /` SHALL aparecer no documento OpenAPI exposto pela aplicação, com os códigos de resposta que produz.

#### Scenario: OpenAPI descreve a entrada

```gherkin
When o documento OpenAPI é solicitado
Then ele descreve GET / com a resposta 200
And GET / não lista a resposta 401
```
