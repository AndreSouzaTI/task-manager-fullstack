CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(120) NOT NULL,
    email         VARCHAR(160) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL
);

CREATE TABLE tasks (
    id           BIGSERIAL PRIMARY KEY,
    owner_id     BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title        VARCHAR(140)  NOT NULL,
    description  VARCHAR(2000),
    status       VARCHAR(20)   NOT NULL,
    priority     VARCHAR(10)   NOT NULL,
    due_date     DATE,
    completed_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ   NOT NULL,
    updated_at   TIMESTAMPTZ   NOT NULL
);

CREATE INDEX idx_tasks_owner_status ON tasks (owner_id, status);
