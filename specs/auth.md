# Authentication & Authorization

## Two authentication mechanisms

### 1. JWT (user sessions)

- Issued on `POST /api/v1/auth/login` as a short-lived Bearer token.
- Verified by `JwtFilter` — parses the token with `JwtService`, loads `UserEntity`, puts a `UsernamePasswordAuthenticationToken` in the `SecurityContext`.
- JWT subject = user UUID.
- Used for all UI-facing endpoints.

### 2. API key (programmatic access)

- Issued per SMTP connection via `POST /api/v1/smtp-connections/{id}/api-keys`.
- Verified by `ApiKeyFilter` — hashes the presented key, looks it up in the DB, puts an `ApiKeyAuthenticationToken` in the `SecurityContext`.
- Scopes stored on `ApiKeyEntity`. Currently supported: `SEND_MAIL`, `READ_MAILS`.
- Used for the `POST /api/v1/send/{connectionId}` (external send) and `GET /api/v1/mail/{connectionId}/mails` endpoints.

Both filters run before Spring Security's authorization layer. Routes protected by JWT must not be hit with an API key and vice versa — they are separate `SecurityFilterChain` beans ordered by priority.

## Extracting the current user

```java
UserEntity user = securityContextService.requireUser();
```

Throws `401` if the context holds no authenticated user or the principal type is wrong.

## Permission model

| Role | Can do |
|---|---|
| `ADMIN` | Everything — bypasses ownership checks |
| `USER` (owner) | Full CRUD on their connections, manage access list, view/send mail |
| `USER` (granted) | View connection, send mail, manage API keys, view mail history |
| `USER` (no relation) | Cannot see the connection at all (returns 404, not 403) |

**Guards (in `SmtpConnectionService`):**

```java
requireAccess(connectionUuid, user)  // owner | granted | admin → ok; otherwise 404
requireManage(connectionUuid, user)  // owner | admin → ok; otherwise 403
```

Prefer `requireAccess` for read/send/key operations. Use `requireManage` for edit, delete, and access list management.

## Registration flow

New users register via invite tokens (single-use, time-limited). Admin creates invite at `POST /api/v1/admin/invites`, shares the token. User registers at `POST /api/v1/auth/register?token=<token>`.

First user is bootstrapped as admin by `AdminBootstrap` on startup if no users exist.
