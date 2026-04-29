CREATE TABLE connection_access_entity (
    connection_uuid  VARCHAR(36)  NOT NULL,
    user_uuid        VARCHAR(36)  NOT NULL,
    granted_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (connection_uuid, user_uuid)
);

CREATE INDEX idx_connection_access_user ON connection_access_entity (user_uuid);
