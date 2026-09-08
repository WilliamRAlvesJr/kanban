CREATE TABLE auth_tokens (
    id         uuid        PRIMARY KEY,
    account_id uuid        NOT NULL REFERENCES accounts (id) ON DELETE CASCADE,
    token_hash text        NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    expires_at timestamptz NOT NULL
);

CREATE UNIQUE INDEX ux_auth_tokens_token_hash ON auth_tokens (token_hash);
CREATE INDEX ix_auth_tokens_account_id ON auth_tokens (account_id);
