CREATE TABLE lanes (
    id          uuid        PRIMARY KEY,
    board_id    uuid        NOT NULL REFERENCES boards (id) ON DELETE CASCADE,
    name        text        NOT NULL,
    position    integer,
    created_at  timestamptz NOT NULL,
    updated_at  timestamptz NOT NULL,
    archived_at timestamptz,
    CONSTRAINT ux_lanes_board_id_position UNIQUE (board_id, position) DEFERRABLE INITIALLY IMMEDIATE,
    CONSTRAINT ck_lanes_position_archived CHECK ((archived_at IS NULL) = (position IS NOT NULL))
);
