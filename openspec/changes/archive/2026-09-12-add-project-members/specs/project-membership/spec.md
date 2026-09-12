## Purpose

Membros de um projeto do kanban e as permissões de projeto de cada um: adição por email, consulta, remoção e atribuição das permissões que liberam endpoints de projeto a uma conta que não é a dona.

## ADDED Requirements

### Requirement: Permissões de projeto

Membro SHALL ser uma conta vinculada a um projeto do qual não é dona. As permissões de projeto SHALL ser `add_member`, `remove_member`, `view_member`, `edit_member`, `view_project`, `edit_project`, `archive_project`, `restore_project`, `add_boards` e `view_boards`, e nenhuma SHALL implicar outra.

Os endpoints de membro SHALL atender o dono do projeto sem permissão nenhuma e o membro que tem a permissão do endpoint:

| endpoint | permissão |
|---|---|
| `POST /projects/{projectId}/members` | `add_member` |
| `GET /projects/{projectId}/members` | `view_member` |
| `GET /projects/{projectId}/members/{memberId}` | `view_member` |
| `DELETE /projects/{projectId}/members/{memberId}` | `remove_member` |
| `PUT /projects/{projectId}/members/{memberId}/permissions` | `edit_member` |

Membro sem a permissão do endpoint SHALL receber `403`, e nenhum membro nem permissão SHALL mudar. A permissão SHALL ser conferida antes da busca do `memberId` da URL. Conta que não é dona nem membro do projeto SHALL receber `404`, com o mesmo `ProblemDetail` de projeto inexistente.

#### Scenario: Membro com a permissão do endpoint

```gherkin
Given o projeto "Produto", com os membros "bia@exemplo.com" e "caio@exemplo.com"
And "bia@exemplo.com" tem somente a permissão <permissão>
When chega <requisição> com o token de "bia@exemplo.com"
Then a resposta é <status>

Examples:
  | permissão     | requisição                                                                                        | status |
  | add_member    | POST /projects/{projectId}/members com email "davi@exemplo.com"                                   | 204    |
  | view_member   | GET /projects/{projectId}/members                                                                 | 200    |
  | view_member   | GET /projects/{projectId}/members/{memberId} de "caio@exemplo.com"                                | 200    |
  | remove_member | DELETE /projects/{projectId}/members/{memberId} de "caio@exemplo.com"                             | 204    |
  | edit_member   | PUT /projects/{projectId}/members/{memberId}/permissions de "caio@exemplo.com" com permissions [] | 200    |
```

#### Scenario: Membro sem a permissão do endpoint

```gherkin
Given o projeto "Produto", com o membro "bia@exemplo.com" sem nenhuma permissão e o membro "caio@exemplo.com" com a permissão "view_project"
When chega <requisição> com o token de "bia@exemplo.com"
Then a resposta é 403
And os membros do projeto e as permissões de cada um continuam iguais

Examples:
  | requisição                                                                                        |
  | POST /projects/{projectId}/members com email "davi@exemplo.com"                                   |
  | GET /projects/{projectId}/members                                                                 |
  | GET /projects/{projectId}/members/{memberId} de "caio@exemplo.com"                                |
  | DELETE /projects/{projectId}/members/{memberId} de "caio@exemplo.com"                             |
  | PUT /projects/{projectId}/members/{memberId}/permissions de "caio@exemplo.com" com permissions [] |
```

#### Scenario: Permissão não implica outra

```gherkin
Given um membro com as permissões "add_member", "remove_member" e "edit_member"
When chega GET /projects/{projectId}/members com o token desse membro
Then a resposta é 403
```

#### Scenario: Dono sem permissão

```gherkin
Given o projeto "Produto" da conta "ana@exemplo.com", sem linha dela em project_members
When chega GET /projects/{projectId}/members com o token de "ana@exemplo.com"
Then a resposta é 200
```

#### Scenario: Permissão conferida antes do membro da URL

```gherkin
Given um membro sem a permissão "remove_member"
When chega DELETE /projects/{projectId}/members/{memberId} com um memberId que nunca existiu e o token desse membro
Then a resposta é 403
```

#### Scenario: Conta fora do projeto

```gherkin
Given o projeto "Produto" da primeira conta, com o membro "bia@exemplo.com"
When chega <requisição> com o token da segunda conta, que não é membro de "Produto"
Then a resposta é 404
And o corpo é igual ao da resposta de projeto inexistente
And os membros do projeto continuam iguais

Examples:
  | requisição                                                           |
  | GET /projects/{projectId}/members                                    |
  | POST /projects/{projectId}/members com email "caio@exemplo.com"      |
  | DELETE /projects/{projectId}/members/{memberId} de "bia@exemplo.com" |
```

#### Scenario: Projeto inexistente

```gherkin
When chega GET /projects/{projectId}/members com um projectId que nunca existiu
Then a resposta é 404
```

### Requirement: Adição de membro por email

O sistema SHALL expor `POST /projects/{projectId}/members`, que recebe `email` e responde `204` sem corpo. A conta com esse email, comparado em minúsculas, SHALL virar membro do projeto, sem nenhuma permissão, quando não é a dona nem já é membro dele. Email sem conta cadastrada, da conta dona ou de membro atual SHALL receber a mesma resposta `204` sem gravar nada.

`email` ausente, vazio ou fora do formato de email SHALL receber `400`, e nenhum membro SHALL ser criado.

Uma conta SHALL ter no máximo um vínculo com cada projeto, garantido por restrição única no banco.

#### Scenario: Conta adicionada como membro

```gherkin
Given a conta "bia@exemplo.com" sem vínculo com o projeto "Produto"
When chega POST /projects/{projectId}/members com email "Bia@Exemplo.com" e o token do dono
Then a resposta é 204 sem corpo
And GET /projects/{projectId}/members traz "bia@exemplo.com" com permissions []
```

#### Scenario: Email sem efeito

```gherkin
Given o projeto "Produto" da conta "ana@exemplo.com", com o membro "bia@exemplo.com" com a permissão "view_project"
When chega POST /projects/{projectId}/members com <email> e o token do dono
Then a resposta é 204 sem corpo
And os membros do projeto e as permissões de cada um continuam iguais

Examples:
  | email                                          |
  | email "zeca@exemplo.com", sem conta cadastrada |
  | email "ana@exemplo.com", da conta dona         |
  | email "bia@exemplo.com", de membro atual       |
```

#### Scenario: Entrada inválida na adição

```gherkin
When chega POST /projects/{projectId}/members com <entrada> e o token do dono
Then a resposta é 400
And nenhum membro é criado

Examples:
  | entrada                 |
  | corpo sem o campo email |
  | email ""                |
  | email "bia.exemplo.com" |
```

#### Scenario: Vínculo repetido recusado pelo banco

```gherkin
Given o membro "bia@exemplo.com" no projeto "Produto"
When uma segunda linha com o mesmo project_id e account_id é inserida em project_members
Then o banco recusa a inserção
```

### Requirement: Listagem dos membros do projeto

O sistema SHALL expor `GET /projects/{projectId}/members`, que devolve `200` com os membros do projeto em `_embedded.members`, ordenados por `created_at` do mais recente para o mais antigo. O dono do projeto SHALL NOT aparecer na listagem.

`_links` da listagem SHALL trazer `self`, com `href` `/projects/{projectId}/members`; `add-member`, com o mesmo `href`, só para a conta que pode chamar `POST /projects/{projectId}/members`; e `project`, com `href` `/projects/{projectId}`, só para a conta que pode chamar `GET /projects/{projectId}`.

#### Scenario: Membros listados

```gherkin
Given o projeto "Produto", com os membros "bia@exemplo.com" e "caio@exemplo.com"
When chega GET /projects/{projectId}/members com o token do dono
Then a resposta é 200 com os dois membros
```

#### Scenario: Ordem da listagem

```gherkin
Given um projeto com três membros adicionados em sequência
When chega GET /projects/{projectId}/members com o token do dono
Then o primeiro item é o membro adicionado por último
And o último item é o membro adicionado primeiro
```

#### Scenario: Dono fora da lista

```gherkin
Given o projeto "Produto" da conta "ana@exemplo.com", sem membros
When chega GET /projects/{projectId}/members com o token de "ana@exemplo.com"
Then a resposta é 200
And _embedded.members é um array vazio
```

#### Scenario: Membro de outro projeto fora da lista

```gherkin
Given os projetos "Produto" e "Operações" do mesmo dono, com o membro "bia@exemplo.com" só em "Operações"
When chega GET /projects/{projectId}/members de "Produto" com o token do dono
Then "bia@exemplo.com" não aparece
```

#### Scenario: Links da listagem para o dono

```gherkin
Given um projeto da conta do token
When chega GET /projects/{projectId}/members desse projeto
Then _links traz somente self, add-member e project
And self e add-member têm href "/projects/{projectId}/members"
And project tem href "/projects/{projectId}"
```

#### Scenario: Links da listagem para membro

```gherkin
Given um membro com somente a permissão "view_member"
When chega GET /projects/{projectId}/members com o token desse membro
Then _links traz somente self
```

### Requirement: Consulta de um membro

O sistema SHALL expor `GET /projects/{projectId}/members/{memberId}`, que devolve `200` com `id`, `account_id`, `email`, `display_name`, `permissions` e `created_at` do membro. `email` e `display_name` SHALL ser os valores da conta do membro, e `permissions` SHALL ser um array com as permissões do membro em ordem alfabética.

`memberId` inexistente ou de membro de outro projeto SHALL receber `404`, com o mesmo `ProblemDetail` nos dois casos.

#### Scenario: Membro consultado

```gherkin
Given a conta "bia@exemplo.com" com display_name "Bia", membro do projeto "Produto" com as permissões "view_project" e "add_member"
When chega GET /projects/{projectId}/members/{memberId} desse membro com o token do dono
Then a resposta é 200 com id, account_id da conta, email "bia@exemplo.com", display_name "Bia" e created_at
And permissions é ["add_member", "view_project"]
```

#### Scenario: Membro de outro projeto

```gherkin
Given os projetos "Produto" e "Operações" do mesmo dono, com o membro "bia@exemplo.com" só em "Operações"
When chega GET /projects/{projectId}/members/{memberId} com o projectId de "Produto" e o memberId de "bia@exemplo.com"
Then a resposta é 404
And o corpo é igual ao da resposta de membro inexistente
```

#### Scenario: Membro inexistente

```gherkin
Given um projeto da conta do token
When chega GET /projects/{projectId}/members/{memberId} com um memberId que nunca existiu
Then a resposta é 404
```

### Requirement: Links do membro

Todo membro em `GET /projects/{projectId}/members/{memberId}` e em `_embedded.members` SHALL trazer em `_links` a relação `self` e, das relações `remove` e `edit-permissions`, só as dos endpoints que a conta do token pode chamar.

| relação | href | endpoint |
|---|---|---|
| `self` | `/projects/{projectId}/members/{id}` | `GET /projects/{projectId}/members/{memberId}` |
| `remove` | `/projects/{projectId}/members/{id}` | `DELETE /projects/{projectId}/members/{memberId}` |
| `edit-permissions` | `/projects/{projectId}/members/{id}/permissions` | `PUT /projects/{projectId}/members/{memberId}/permissions` |

#### Scenario: Links do membro para o dono

```gherkin
Given um projeto da conta do token, com um membro
When chega GET /projects/{projectId}/members/{memberId} desse membro
Then _links traz somente self, remove e edit-permissions
```

#### Scenario: Links do membro conforme as permissões

```gherkin
Given um projeto com os membros "bia@exemplo.com" e "caio@exemplo.com"
And "bia@exemplo.com" tem as permissões <permissões>
When chega GET /projects/{projectId}/members/{memberId} de "caio@exemplo.com" com o token de "bia@exemplo.com"
Then _links traz somente <links>

Examples:
  | permissões                      | links                   |
  | "view_member"                   | self                    |
  | "view_member" e "remove_member" | self e remove           |
  | "view_member" e "edit_member"   | self e edit-permissions |
```

### Requirement: Atribuição das permissões do membro

O sistema SHALL expor `PUT /projects/{projectId}/members/{memberId}/permissions`, que recebe `permissions`, um array de permissões de projeto, substitui todas as permissões do membro por esse conjunto e responde `200`. Array vazio SHALL deixar o membro sem permissão, e valor repetido SHALL ser gravado uma vez.

`permissions` ausente, `null`, fora do formato de array, com item `null` ou com item fora das permissões de projeto SHALL receber `400`, e as permissões do membro SHALL permanecer inalteradas.

O membro com `edit_member` SHALL poder alterar as próprias permissões, inclusive atribuindo a si permissões que não tem.

#### Scenario: Permissões substituídas

```gherkin
Given um membro com as permissões "view_project" e "add_member"
When chega PUT /projects/{projectId}/members/{memberId}/permissions desse membro com permissions ["view_member", "edit_project"] e o token do dono
Then a resposta é 200
And GET /projects/{projectId}/members/{memberId} traz permissions ["edit_project", "view_member"]
```

#### Scenario: Permissões esvaziadas

```gherkin
Given um membro com a permissão "view_project"
When chega PUT /projects/{projectId}/members/{memberId}/permissions desse membro com permissions [] e o token do dono
Then a resposta é 200
And GET /projects/{projectId}/members/{memberId} traz permissions []
```

#### Scenario: Permissão repetida

```gherkin
Given um membro sem nenhuma permissão
When chega PUT /projects/{projectId}/members/{memberId}/permissions desse membro com permissions ["view_project", "view_project"] e o token do dono
Then a resposta é 200
And GET /projects/{projectId}/members/{memberId} traz permissions ["view_project"]
```

#### Scenario: Entrada inválida na atribuição

```gherkin
Given um membro com a permissão "view_project"
When chega PUT /projects/{projectId}/members/{memberId}/permissions desse membro com <entrada> e o token do dono
Then a resposta é 400
And GET /projects/{projectId}/members/{memberId} traz permissions ["view_project"]

Examples:
  | entrada                        |
  | corpo sem o campo permissions  |
  | permissions null               |
  | permissions "view_project"     |
  | permissions ["delete_project"] |
  | permissions [null]             |
```

#### Scenario: Membro altera as próprias permissões

```gherkin
Given o membro "bia@exemplo.com" com somente a permissão "edit_member"
When chega PUT /projects/{projectId}/members/{memberId}/permissions de "bia@exemplo.com" com permissions ["edit_member", "view_project"] e o token de "bia@exemplo.com"
Then a resposta é 200
And GET /projects/{projectId} com o token de "bia@exemplo.com" responde 200
```

#### Scenario: Atribuição a membro de outro projeto

```gherkin
Given os projetos "Produto" e "Operações" do mesmo dono, com o membro "bia@exemplo.com" só em "Operações" com a permissão "view_project"
When chega PUT /projects/{projectId}/members/{memberId}/permissions com o projectId de "Produto", o memberId de "bia@exemplo.com" e permissions []
Then a resposta é 404
And "bia@exemplo.com" continua com a permissão "view_project" em "Operações"
```

### Requirement: Remoção de membro

O sistema SHALL expor `DELETE /projects/{projectId}/members/{memberId}`, que apaga o membro e as permissões dele e responde `204` sem corpo. O membro com `remove_member` SHALL poder remover qualquer membro do projeto, inclusive a si mesmo.

#### Scenario: Membro removido

```gherkin
Given o membro "bia@exemplo.com" com a permissão "view_project"
When chega DELETE /projects/{projectId}/members/{memberId} de "bia@exemplo.com" com o token do dono
Then a resposta é 204 sem corpo
And GET /projects/{projectId}/members/{memberId} de "bia@exemplo.com" responde 404
And GET /projects/{projectId} com o token de "bia@exemplo.com" responde 404
And nenhuma linha desse membro permanece em project_member_permissions
```

#### Scenario: Membro remove a si mesmo

```gherkin
Given o membro "bia@exemplo.com" com somente a permissão "remove_member"
When chega DELETE /projects/{projectId}/members/{memberId} de "bia@exemplo.com" com o token de "bia@exemplo.com"
Then a resposta é 204
And "bia@exemplo.com" deixa de ser membro do projeto
```

#### Scenario: Remoção de membro de outro projeto

```gherkin
Given os projetos "Produto" e "Operações" do mesmo dono, com o membro "bia@exemplo.com" só em "Operações"
When chega DELETE /projects/{projectId}/members/{memberId} com o projectId de "Produto" e o memberId de "bia@exemplo.com"
Then a resposta é 404
And "bia@exemplo.com" continua membro de "Operações"
```

### Requirement: Membros apagados com a conta e com o projeto

Apagar a linha da conta em `accounts` SHALL apagar os vínculos dela em `project_members` e as permissões desses vínculos. Apagar a linha do projeto em `projects` SHALL apagar os membros dele e as permissões desses membros.

#### Scenario: Conta apagada

```gherkin
Given a conta "bia@exemplo.com", membro de dois projetos com a permissão "view_project" em cada um
When a linha dessa conta é apagada de accounts
Then nenhuma linha dela permanece em project_members
And nenhuma permissão desses vínculos permanece em project_member_permissions
```

#### Scenario: Projeto apagado

```gherkin
Given um projeto com dois membros, cada um com a permissão "view_project"
When a linha desse projeto é apagada de projects
Then nenhum membro dele permanece em project_members
And nenhuma permissão desses membros permanece em project_member_permissions
```

### Requirement: Endpoints documentados no OpenAPI

Os endpoints de membro SHALL aparecer no documento OpenAPI exposto pela aplicação, com os códigos de resposta que produzem.

#### Scenario: OpenAPI lista os endpoints

```gherkin
When o documento OpenAPI é solicitado
Then ele descreve POST /projects/{projectId}/members, GET /projects/{projectId}/members, GET /projects/{projectId}/members/{memberId}, DELETE /projects/{projectId}/members/{memberId} e PUT /projects/{projectId}/members/{memberId}/permissions
And lista os códigos de resposta de cada endpoint
And cada um desses endpoints lista a resposta 403
```
