## Why

Uma conta autenticada não tem nenhum recurso próprio no sistema. Sem quadro, não há onde pendurar coluna nem card.

## What Changes

```mermaid
erDiagram
    accounts ||--o{ boards : possui
    boards {
        uuid id PK
        uuid owner_id FK
        text name
        text description
        timestamptz created_at
        timestamptz updated_at
        timestamptz archived_at
    }
```

```mermaid
stateDiagram-v2
    [*] --> ativo: POST /boards
    ativo --> arquivado: POST /boards/{id}/archive
    arquivado --> ativo: POST /boards/{id}/restore
```

- `POST /boards` responde `201` com `id`, `name`, `description`, `created_at`, `updated_at` e `archived_at`, e grava como dono a conta do token.
- `name` é obrigatório e não vazio, com no máximo 100 caracteres; `description` é opcional, com no máximo 500. Entrada inválida responde `400`.
- `GET /boards` responde `200` com todos os quadros do dono do token, ordenados por `created_at` do mais recente para o mais antigo. `archived=false` restringe aos ativos e `archived=true` aos arquivados; valor fora desses dois responde `400`.
- `GET /boards/{id}` responde `200` com o quadro, ativo ou arquivado.
- `PUT /boards/{id}` substitui `name` e `description` e responde `200` com o quadro, com as regras de validação do `POST /boards`. `description` ausente ou `null` grava `null`, e `archived_at` não muda.
- `POST /boards/{id}/archive` carimba `archived_at` e `POST /boards/{id}/restore` grava `null`; os dois respondem `200` com o quadro. Arquivar quadro arquivado ou restaurar quadro ativo responde `200` sem alterar `archived_at`.
- Quadro de outra conta responde `404` em `GET /boards/{id}`, `PUT /boards/{id}`, `POST /boards/{id}/archive` e `POST /boards/{id}/restore`, com o mesmo corpo de quadro inexistente.
- Alterar um quadro carimba `updated_at`. Requisição que não muda nenhum valor deixa `updated_at` intacto.
- Excluir a linha da conta em `accounts` apaga os quadros dela.
- Os endpoints de quadro aparecem no documento OpenAPI com os códigos de resposta que produzem.

## Capabilities

### New Capabilities

- `board-management`: criação, consulta, edição e arquivamento de quadros de uma conta, com isolamento entre donos.

### Modified Capabilities

Nenhuma.

## Impact

- Pacote novo `com.william.kanban.board`, com `Board`, `BoardRepository`, `BoardService`, `BoardController`, `BoardNotFoundException`, `BoardLimits` e os records `CreateBoardRequest`, `UpdateBoardRequest` e `BoardResponse`.
- Endpoints novos: `POST /boards`, `GET /boards`, `GET /boards/{id}`, `PUT /boards/{id}`, `POST /boards/{id}/archive` e `POST /boards/{id}/restore`. Todos exigem token.
- Migration `V3__create_boards.sql`: tabela `boards`, chave estrangeira `owner_id` para `accounts (id)` com `ON DELETE CASCADE`, índice em `owner_id`. As colunas de tempo são `NOT NULL` e não têm `DEFAULT`.
- `GlobalExceptionHandler` passa a converter `BoardNotFoundException` em `404`.
- Classe nova `OpenApiResponsesConfig` em `com.william.kanban.shared`, que documenta no OpenAPI as respostas de erro dos controllers de conta, autenticação e quadro.
- Nenhuma dependência nova no `pom.xml`.
