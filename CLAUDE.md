# MailService Backend — Claude Guide

## Project overview

Self-hosted multi-tenant SMTP relay. Users own SMTP connections and issue API keys against them. Other users can be granted access to a connection by the owner or an admin.

## Build & run

```bash
./mvnw compile          # compile only
./mvnw verify           # compile + test
./mvnw spring-boot:run  # run locally
```

Tests use H2 in-memory + Hibernate create-drop (no Flyway in tests).

## Code style

Spotless + Google Java Format AOSP variant is enforced.

```bash
./mvnw spotless:apply   # auto-format
./mvnw spotless:check   # CI check
```

Never manually reformat — always run `spotless:apply`.

## Commit convention

`git commit -m "..."` — never override author, never use `--amend` unless explicitly asked.

## Key rules

- All controller interfaces live in `controller/v1/schema/` — annotations, Javadoc, and OpenAPI go there.
- Implementations in `controller/v1/implementation/` are thin: extract the user from context, call the service, return the result.
- Business logic belongs exclusively in services, never in controllers or entities.
- Permission guards (`requireAccess`, `requireManage`) are centralized in `SmtpConnectionService` — do not duplicate them.
- SMTP passwords are stored AES-GCM encrypted — never store plaintext.
- Flyway migration files are in `src/main/resources/db/migration/` and named `V<n>__description.sql`. Never edit an existing migration.

## See also

- `specs/architecture.md` — package layout, layer responsibilities
- `specs/auth.md` — JWT + API key auth flow, permission model
- `specs/patterns.md` — coding conventions, Lombok usage, DTO conventions
