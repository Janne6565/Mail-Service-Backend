CREATE TABLE invite_entity (
    uuid                  VARCHAR(36)  PRIMARY KEY,
    token                 VARCHAR(64)  NOT NULL UNIQUE,
    created_by_user_uuid  VARCHAR(36)  NOT NULL,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    expires_at            TIMESTAMPTZ  NOT NULL,
    used_by_user_uuid     VARCHAR(36),
    used_at               TIMESTAMPTZ
);

CREATE TABLE smtp_connection_entity (
    uuid                 VARCHAR(36)  PRIMARY KEY,
    owner_user_uuid      VARCHAR(36)  NOT NULL,
    label                VARCHAR(255) NOT NULL,
    host                 VARCHAR(255) NOT NULL,
    port                 INTEGER      NOT NULL,
    username             VARCHAR(255) NOT NULL,
    password_ciphertext  TEXT         NOT NULL,
    from_address         VARCHAR(255) NOT NULL,
    use_start_tls        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_smtp_connection_owner ON smtp_connection_entity (owner_user_uuid);

CREATE TABLE api_key_entity (
    uuid                   VARCHAR(36)  PRIMARY KEY,
    smtp_connection_uuid   VARCHAR(36)  NOT NULL,
    label                  VARCHAR(255) NOT NULL,
    secret_hash            VARCHAR(255) NOT NULL,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    last_used_at           TIMESTAMPTZ,
    revoked_at             TIMESTAMPTZ
);

CREATE INDEX idx_api_key_connection ON api_key_entity (smtp_connection_uuid);

CREATE TABLE api_key_entity_scopes (
    api_key_entity_uuid  VARCHAR(36)  NOT NULL REFERENCES api_key_entity (uuid),
    scopes               VARCHAR(50)  NOT NULL,
    PRIMARY KEY (api_key_entity_uuid, scopes)
);

CREATE TABLE mail_entity (
    uuid                   VARCHAR(36)   PRIMARY KEY,
    smtp_connection_uuid   VARCHAR(36)   NOT NULL,
    api_key_uuid           VARCHAR(36),
    recipient              VARCHAR(255)  NOT NULL,
    subject                VARCHAR(500)  NOT NULL,
    body                   TEXT          NOT NULL,
    html                   BOOLEAN       NOT NULL DEFAULT FALSE,
    sent_at                TIMESTAMPTZ   NOT NULL DEFAULT now(),
    success                BOOLEAN       NOT NULL,
    error_message          TEXT
);

CREATE INDEX idx_mail_connection ON mail_entity (smtp_connection_uuid);
CREATE INDEX idx_mail_sent_at    ON mail_entity (sent_at);

CREATE TABLE app_setting_entity (
    setting_key    VARCHAR(100) PRIMARY KEY,
    setting_value  VARCHAR(255) NOT NULL
);

INSERT INTO app_setting_entity (setting_key, setting_value) VALUES ('MAIL_RETENTION_DAYS', '90');
