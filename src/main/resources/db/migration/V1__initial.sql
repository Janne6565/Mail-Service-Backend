CREATE TABLE user_entity (
    uuid         VARCHAR(36)  PRIMARY KEY,
    username     VARCHAR(255) NOT NULL UNIQUE,
    email        VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    role         VARCHAR(20)  NOT NULL DEFAULT 'USER',
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
