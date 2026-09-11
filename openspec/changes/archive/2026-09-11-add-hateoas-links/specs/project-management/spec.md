## MODIFIED Requirements

Alterados nesta change; o restante de cada bloco é repetição da spec publicada.

- Criação de projeto: descrição; cenários "Projeto criado" e "Projeto sem descrição".
- Listagem dos projetos da conta: descrição; cenário "Links da listagem".
- Substituição do projeto: descrição; cenários "Projeto substituído", "Descrição omitida apagada" e "Projeto arquivado editado".
- Arquivamento reversível: descrição; cenários "Projeto arquivado", "Projeto restaurado" e "Restauração de projeto ativo".
- Carimbo de updated_at: cenário "Criação preenche updated_at".

### Requirement: Criação de projeto

O sistema SHALL expor `POST /projects`, que recebe `name` e `description` e cria um projeto com identificador `uuid` gerado pelo sistema e dono igual à conta do token.

A resposta de sucesso SHALL ser `201`.

#### Scenario: Projeto criado

```gherkin
Given um token emitido para a conta de "ana@exemplo.com"
When chega POST /projects com name "Produto" e description "Time de produto"
Then a resposta é 201
And GET no header Location traz id, name "Produto", description "Time de produto", created_at, updated_at e archived_at null
```

#### Scenario: Projeto sem descrição

```gherkin
Given um token emitido para uma conta
When chega POST /projects com name "Produto" e sem o campo description
Then a resposta é 201
And GET no header Location traz description null
```

#### Scenario: Dono tirado do token

```gherkin
Given duas contas cadastradas, cada uma com o seu token
When chega POST /projects com o token da segunda conta
Then a coluna owner_id guarda o id da segunda conta
```

### Requirement: Listagem dos projetos da conta

O sistema SHALL expor `GET /projects`, que devolve `200` com os projetos da conta do token em `_embedded.projects`, ordenados por `created_at` do mais recente para o mais antigo.

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

#### Scenario: Links da listagem

```gherkin
Given uma conta com um projeto
When chega GET /projects com o token dessa conta
Then _links traz somente self e create-project, os dois com href "/projects"
```

### Requirement: Substituição do projeto

O sistema SHALL expor `PUT /projects/{projectId}`, que substitui `name` e `description` do projeto e responde `200`. `description` ausente ou `null` SHALL gravar `null`, e `archived_at` SHALL NOT mudar.

#### Scenario: Projeto substituído

```gherkin
Given um projeto com name "Produto" e description "Time de produto"
When chega PUT /projects/{projectId} com name "Plataforma" e description "Outro texto"
Then a resposta é 200
And GET /projects/{projectId} traz name "Plataforma" e description "Outro texto"
```

#### Scenario: Descrição omitida apagada

```gherkin
Given um projeto com description "Time de produto"
When chega PUT /projects/{projectId} com name "Produto" e sem o campo description
Then a resposta é 200
And GET /projects/{projectId} traz description null
```

#### Scenario: Projeto arquivado editado

```gherkin
Given um projeto arquivado, com o archived_at guardado
When chega PUT /projects/{projectId} com name "Plataforma"
Then a resposta é 200
And GET /projects/{projectId} traz name "Plataforma"
And archived_at continua igual ao guardado
```

### Requirement: Arquivamento reversível

O sistema SHALL expor `POST /projects/{projectId}/archive`, que carimba `archived_at` com o instante do arquivamento, e `POST /projects/{projectId}/restore`, que grava `null` em `archived_at`. Os dois SHALL responder `200`. Arquivar projeto arquivado ou restaurar projeto ativo SHALL responder `200` sem alterar `archived_at`.

Arquivar ou restaurar um projeto SHALL NOT alterar os quadros dele.

#### Scenario: Projeto arquivado

```gherkin
Given um projeto ativo
When chega POST /projects/{projectId}/archive
Then a resposta é 200
And GET /projects/{projectId} traz archived_at preenchido
And o projeto deixa de aparecer na listagem com archived "false"
```

#### Scenario: Projeto restaurado

```gherkin
Given um projeto arquivado
When chega POST /projects/{projectId}/restore
Then a resposta é 200
And GET /projects/{projectId} traz archived_at null
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
Then a resposta é 200
And GET /projects/{projectId} traz archived_at null
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
Then GET no header Location traz updated_at preenchido
```

## ADDED Requirements

### Requirement: Links do projeto

Todo projeto em `GET /projects/{projectId}` e em `_embedded.projects` SHALL trazer em `_links` as relações abaixo. `archive` SHALL aparecer só no projeto com `archived_at` null, e `restore` só no projeto arquivado.

| relação | href |
|---|---|
| `self` | `/projects/{id}` |
| `edit` | `/projects/{id}` |
| `archive` | `/projects/{id}/archive` |
| `restore` | `/projects/{id}/restore` |
| `boards` | `/projects/{id}/boards` |
| `create-board` | `/projects/{id}/boards` |

#### Scenario: Links do projeto ativo

```gherkin
Given um projeto ativo da conta do token
When chega GET /projects/{projectId} desse projeto
Then _links traz somente self, edit, archive, boards e create-board
```

#### Scenario: Links do projeto arquivado

```gherkin
Given um projeto arquivado da conta do token
When chega GET /projects/{projectId} desse projeto
Then _links traz somente self, edit, restore, boards e create-board
```
