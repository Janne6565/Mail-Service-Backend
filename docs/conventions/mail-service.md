<!-- AUTO-SYNCED from agents KB: projects/mail-service.md @ 8beba02.
     Do NOT edit here — edit the source in ~/projects/agents and re-run scripts/sync-conventions.sh. -->

# Mail Service

Transactional-mail platform: users configure SMTP connections, issue scoped API keys, and send/monitor mails through them.

- **Live:** mail-service.jannekeipert.de
- **Repos:** github.com/Janne6565/{mail-service-backend, Mail-Service-Frontend}
  — **NOT `mailService`**: that is a defunct 9-file predecessor repo; the deployed
  backend is `mail-service-backend` (84+ Java files). ⚠️ The old `mailService` repo
  has a real SMTP password for `info@jannekeipert.de` committed in `.env` — treat as
  leaked; rotate/archive.
- **Local:** clone into `~/projects/mail-service/<repo-name>/`. Always `git pull` before reading. See [repo conventions](README.md#local-repos--clone-on-demand-pull-before-reading).
- **Cluster:** namespace `mailservice` (backend + postgres + frontend); backend monitoring app in `default`.

## Idea
Users register (invite-based), configure SMTP connections (AES-GCM-encrypted passwords), issue per-connection API keys (`mk_<uuid>_<secret>`, scopes SEND / READ_MAILS), and send mails via `POST /api/v1/send`; every mail is persisted for monitoring with configurable retention.

## Stack
- **Frontend:** React 19, TypeScript, Vite 7, Bun, Redux Toolkit, TanStack Router (file-based), axios (hand-maintained typed client in `src/api/api.ts` — orval configured but unused), shadcn/Tailwind v4, react-hook-form + zod, typed i18next (EN/DE in `src/i18n/resources.ts`), Biome. No test runner.
- **Backend:** Spring Boot 4.0.5 / Java 21, Maven, house layout (`controller/v1/schema`+`implementation`), Spring Security, JPA, jjwt 0.13, PostgreSQL, Flyway migrations, springdoc, micrometer-prometheus, Spotless (AOSP).
- **Deploy:** app-of-apps — ArgoCD parent `mail-service-deployment` → children in `mail-service-backend/k8s/apps/` (backend app watches `k8s/base`, Kustomize; monitoring watches `k8s/monitoring`). Frontend: plain `k8s/` in its own repo, ArgoCD app `mail-service-frontend` (defined only in cluster-deployment). Image bumps are manual commits editing `deployment.yaml` (backend tags `main-<sha>`, frontend `staging-<sha>`; `v*` tags → semver + prod-latest).

## Notable (stands out vs other projects)
- **House session model already implemented** (like Cosy): access/refresh JWTs with `tokenType` claim, httpOnly `refreshToken` cookie (path `/api/v1/auth/token`), `GET /api/v1/auth/token` identity exchange, in-memory token in the frontend.
- Invite-based registration (`/auth/register?inviteToken=`), bootstrap admin from `ADMIN_*` env.
- Per-connection API keys with BCrypt-hashed secrets and scopes — the M2M path (used by other projects to send mail).
- SMTP passwords encrypted at rest (AES-GCM via `CIPHER_KEY`).
- **Authentik SSO (federated, 2026-07):** "Login with Authentik" alongside password login; auto-link by email (unique email column), groups `mail-service-admins`/`mail-service-users`, strict gate + role sync. See `concepts/AUTH.md` template.

## Notes for agents
- Secret `mail-service-secrets` (ns `mailservice`) uses snake_case keys (`jwt_secret_key`, `cipher_key`, `admin_*`, `oauth_client_id/secret`); postgres creds in `postgres-secrets`.
- Ingress: one host, `/api` → backend :8080, `/` → frontend :80; TLS secret `mail-service-tls`.
- Management port 8081 (actuator/prometheus), ServiceMonitor in `k8s/monitoring/`.
