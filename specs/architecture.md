# Backend Architecture

## Tech stack

| Concern | Choice |
|---|---|
| Framework | Spring Boot 4.0.5 |
| Language | Java 21 |
| Build | Maven (`mvnw`) |
| Database | PostgreSQL (runtime), H2 (tests) |
| Migrations | Flyway — `V<n>__description.sql` |
| ORM | Spring Data JPA / Hibernate |
| Auth | JWT (JJWT 0.13.0) + API key filter |
| Validation | Jakarta Bean Validation (`spring-boot-starter-validation`) |
| Docs | springdoc-openapi |
| Code style | Spotless + Google Java Format AOSP |
| Utilities | Lombok |

## Package layout

```
com.janne.mailservice
├── MailServiceApplication.java
├── configuration/
│   ├── admin/          # AdminBootstrap — seeds first admin on startup
│   ├── security/       # SecurityFilterChain, CORS, password encoder
│   └── web/            # Jackson, MVC config
├── controller/
│   └── v1/
│       ├── schema/         # *Api.java interfaces (OpenAPI annotations, Javadoc)
│       └── implementation/ # *Controller.java (thin, delegates to services)
├── entity/             # JPA entities, @IdClass composites
├── model/
│   ├── action/         # Inbound DTOs (Create*, Update*, Login*, Grant*, Send*, ...)
│   └── core/           # Outbound DTOs (*Dto.java)
├── repository/         # Spring Data JPA repositories
├── security/
│   ├── apikeyfilter/   # API key extraction + authentication filter
│   └── jwtfilter/      # JWT extraction + authentication filter
└── services/
    ├── auth/           # JwtService, SecurityContextService
    ├── core/           # ApiKeyService, InviteService, MailService,
    │                   #   SettingsService, SmtpConnectionService, UserService
    ├── crypto/         # AES-GCM SMTP password encryption
    └── mail/           # MailDispatcher (builds JavaMailSender from connection)
```

## Layer responsibilities

**Controller (schema + impl split)**
- `*Api.java`: declares the HTTP contract. All `@RequestMapping`, `@Operation`, `@ApiResponse`, and parameter annotations go here. Nothing else.
- `*Controller.java`: implements `*Api`. Extracts `UserEntity` via `SecurityContextService.requireUser()`, calls the appropriate service method, returns the result. No business logic.

**Service**
- All business logic, permission checks, and cross-entity coordination.
- Permission guards in `SmtpConnectionService`:
  - `requireAccess(uuid, user)` — owner, granted user, or admin; throws 404 for others (no information leakage).
  - `requireManage(uuid, user)` — owner or admin only; throws 403.

**Repository**
- Spring Data JPA interfaces only. Custom JPQL in `@Query` annotations where needed.

**Entity**
- JPA mappings only. No logic. Composite PKs use `@IdClass` with a static inner `*Id implements Serializable`.

**Model**
- Plain Java records or Lombok `@Builder` + `@Value` classes.
- `action/` = inbound (request body). `core/` = outbound (response body).

## Database migrations

Files: `src/main/resources/db/migration/V<n>__description.sql`

| File | Contents |
|---|---|
| V1__initial.sql | users, roles |
| V2__smtp_invites_keys_mails.sql | smtp_connection, invite, api_key, mail |
| V3__connection_access.sql | connection_access_entity (join table, composite PK) |

Never edit an existing migration. Add a new `V<n+1>__...sql` for any schema change.
