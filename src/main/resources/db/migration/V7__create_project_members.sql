CREATE TABLE project_members (
    id         uuid        PRIMARY KEY,
    project_id uuid        NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    account_id uuid        NOT NULL REFERENCES accounts (id) ON DELETE CASCADE,
    created_at timestamptz NOT NULL,
    CONSTRAINT ux_project_members_project_id_account_id UNIQUE (project_id, account_id)
);

CREATE INDEX ix_project_members_account_id ON project_members (account_id);

CREATE TABLE project_member_permissions (
    project_member_id uuid NOT NULL REFERENCES project_members (id) ON DELETE CASCADE,
    permission        text NOT NULL,
    PRIMARY KEY (project_member_id, permission),
    CONSTRAINT ck_project_member_permissions_permission CHECK (permission IN (
        'ADD_BOARDS', 'ADD_MEMBER', 'ARCHIVE_PROJECT', 'EDIT_MEMBER', 'EDIT_PROJECT',
        'REMOVE_MEMBER', 'RESTORE_PROJECT', 'VIEW_BOARDS', 'VIEW_MEMBER', 'VIEW_PROJECT'
    ))
);
