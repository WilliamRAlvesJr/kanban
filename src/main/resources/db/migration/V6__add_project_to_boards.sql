ALTER TABLE boards DROP COLUMN owner_id;

ALTER TABLE boards ADD COLUMN project_id uuid NOT NULL REFERENCES projects (id) ON DELETE CASCADE;

CREATE INDEX ix_boards_project_id ON boards (project_id);
