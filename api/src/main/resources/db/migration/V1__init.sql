CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    full_name     VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(16)  NOT NULL,          -- ADMIN | USER
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    must_change_password BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE teams (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE team_members (
    team_id   BIGINT NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    user_id   BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    joined_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (team_id, user_id)
);

CREATE TABLE items (
    id                 BIGSERIAL PRIMARY KEY,
    team_id            BIGINT REFERENCES teams(id) ON DELETE CASCADE,
    owner_id           BIGINT REFERENCES users(id) ON DELETE CASCADE,
    title              VARCHAR(255) NOT NULL,
    username           VARCHAR(255),
    encrypted_password BYTEA        NOT NULL,
    url                VARCHAR(1024),
    notes              TEXT,
    created_by         BIGINT       NOT NULL REFERENCES users(id),
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT item_owner_check CHECK (
        (team_id IS NOT NULL AND owner_id IS NULL) OR
        (team_id IS NULL AND owner_id IS NOT NULL)
    )
);

CREATE INDEX idx_items_team_id ON items(team_id);
CREATE INDEX idx_team_members_user_id ON team_members(user_id);

CREATE TABLE audit_logs (
    id         BIGSERIAL PRIMARY KEY,
    user_email VARCHAR(255) NOT NULL,
    item_title VARCHAR(255),
    team_name  VARCHAR(255),
    action     VARCHAR(32)  NOT NULL,
    ip_address VARCHAR(45),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at DESC);
CREATE INDEX idx_audit_logs_user_email ON audit_logs(user_email);
