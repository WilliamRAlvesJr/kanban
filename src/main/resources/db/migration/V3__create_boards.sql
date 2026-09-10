CREATE TABLE boards (
    id          uuid        PRIMARY KEY,
    owner_id    uuid        NOT NULL REFERENCES accounts (id) ON DELETE CASCADE,
    name        text        NOT NULL,
    description text,
    created_at  timestamptz NOT NULL,
    updated_at  timestamptz NOT NULL,
    archived_at timestamptz
);

CREATE INDEX ix_boards_owner_id ON boards (owner_id);
