CREATE TABLE projects (
    id          uuid        PRIMARY KEY,
    owner_id    uuid        NOT NULL REFERENCES accounts (id) ON DELETE CASCADE,
    name        text        NOT NULL,
    description text,
    created_at  timestamptz NOT NULL,
    updated_at  timestamptz NOT NULL,
    archived_at timestamptz
);

CREATE INDEX ix_projects_owner_id ON projects (owner_id);
