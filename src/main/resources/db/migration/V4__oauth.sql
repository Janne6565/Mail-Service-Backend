-- Federated OIDC login ("Login with Authentik").
-- Password login and OAuth login both resolve a local user_entity and issue the Mail Service's own
-- JWTs. OAuth-only users have no password; password_hash is already nullable (V1), so no change.

-- Single-use CSRF state for the authorization-code flow (10-min TTL, evicted hourly).
CREATE TABLE oauth_state (
    state      VARCHAR(64)  PRIMARY KEY,
    issued_at  TIMESTAMPTZ  NOT NULL
);

-- Links an external provider identity to a local user. Email lives on user_entity, not here.
CREATE TABLE oauth_identity (
    uuid              VARCHAR(36)  PRIMARY KEY,
    user_uuid         VARCHAR(36)  NOT NULL REFERENCES user_entity (uuid) ON DELETE CASCADE,
    provider          VARCHAR(32)  NOT NULL,
    provider_subject  VARCHAR(255) NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_oauth_identity UNIQUE (provider, provider_subject)
);

CREATE INDEX idx_oauth_identity_user ON oauth_identity (user_uuid);
