## MODIFIED Requirements

Alterados nesta change; o restante de cada bloco é repetição da spec publicada.

- Coleções em _embedded: descrição; cenário novo "Item da coleção de membros".
- Escrita responde só com links: descrição; cenário "Self da escrita".

### Requirement: Coleções em _embedded

Toda listagem SHALL responder `200` com um objeto que traz os itens, na ordem da listagem, no array `_embedded.projects`, `_embedded.boards`, `_embedded.lanes` ou `_embedded.members`. Para a conta que pode chamar a consulta individual do recurso, cada item SHALL trazer os mesmos campos e os mesmos `_links` dessa consulta. Listagem sem item SHALL trazer o array vazio.

`_links.self.href` da listagem SHALL ser o caminho da requisição, sem query string.

#### Scenario: Item igual à consulta individual

```gherkin
Given uma conta com um projeto
When chega GET /projects com o token dessa conta
Then _embedded.projects traz um item
And o item é igual ao corpo de GET /projects/{projectId} desse projeto
```

#### Scenario: Item da coleção de membros

```gherkin
Given um projeto da conta do token, com um membro
When chega GET /projects/{projectId}/members desse projeto
Then _embedded.members traz um item
And o item é igual ao corpo de GET /projects/{projectId}/members/{memberId} desse membro
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

Os endpoints de escrita de projeto, quadro e lane e `PUT /projects/{projectId}/members/{memberId}/permissions` SHALL responder com corpo só com `_links`, sem nenhum campo do recurso, e com `_links.self.href` apontando para o recurso afetado. O código de status de cada endpoint é o da spec da capability dele.

As criações SHALL responder com o header `Location` igual a `_links.self.href`.

#### Scenario: Self da escrita

```gherkin
Given o projeto "Produto", com o quadro "Sprint 12", que tem a lane ativa "A fazer", e com o membro "bia@exemplo.com"
When chega <requisição> com o token do dono
Then o corpo traz somente _links
And _links.self.href é <self>

Examples:
  | requisição                                                                                       | self                                          |
  | POST /projects com name "Plataforma"                                                             | "/projects/{id do projeto criado}"            |
  | PUT /projects/{projectId} de "Produto" com name "Plataforma"                                     | "/projects/{projectId}"                       |
  | POST /projects/{projectId}/archive de "Produto"                                                  | "/projects/{projectId}"                       |
  | POST /projects/{projectId}/restore de "Produto"                                                  | "/projects/{projectId}"                       |
  | POST /projects/{projectId}/boards com name "Sprint 13"                                           | "/boards/{id do quadro criado}"               |
  | PUT /boards/{id} de "Sprint 12" com name "Sprint 13"                                             | "/boards/{id}"                                |
  | POST /boards/{id}/archive de "Sprint 12"                                                         | "/boards/{id}"                                |
  | POST /boards/{id}/restore de "Sprint 12"                                                         | "/boards/{id}"                                |
  | POST /boards/{id}/move de "Sprint 12" com project_id de "Produto"                                | "/boards/{id}"                                |
  | POST /boards/{boardId}/lanes com name "Feito"                                                    | "/boards/{boardId}/lanes/{id da lane criada}" |
  | PUT /boards/{boardId}/lanes/{laneId} de "A fazer" com name "Backlog"                             | "/boards/{boardId}/lanes/{laneId}"            |
  | POST /boards/{boardId}/lanes/{laneId}/archive de "A fazer"                                       | "/boards/{boardId}/lanes/{laneId}"            |
  | POST /boards/{boardId}/lanes/{laneId}/restore de "A fazer"                                       | "/boards/{boardId}/lanes/{laneId}"            |
  | PUT /boards/{boardId}/lanes/order com lane_ids de "A fazer"                                      | "/boards/{boardId}/lanes"                     |
  | PUT /projects/{projectId}/members/{memberId}/permissions de "bia@exemplo.com" com permissions [] | "/projects/{projectId}/members/{memberId}"    |
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
