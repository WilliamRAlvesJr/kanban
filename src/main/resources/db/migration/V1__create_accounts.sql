CREATE TABLE accounts (
    id            uuid        PRIMARY KEY,
    email         text        NOT NULL CHECK (email = lower(email)),
    display_name  text        NOT NULL,
    password_hash text        NOT NULL,
    created_at    timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_accounts_email ON accounts (email);
