# Setup and deployment

## Requirements

Java 17+, the included Maven wrapper, PostgreSQL 17 and Docker for the optional local database. Frontend branches also require Node.js compatible with Vite 8 (Node 22.12+).

## Local development

`docker compose up -d db` starts the development PostgreSQL service. Its checked-in credentials are local development defaults only; do not expose that database publicly or reuse those credentials for hosting. `./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=18084` starts the backend on the port expected by the frontend proxy; use `mvnw.cmd` on Windows.

For the frontend, run `npm ci` and `npm run dev` inside `frontend/`. The Vite configuration proxies API requests to the local backend. Use `npm run build` for the bundled frontend output. The backend can also serve packaged static assets.

Secure cookies are enabled by default. For an HTTP-only local development session, set `PROCUREFLOW_COOKIE_SECURE=false` locally; keep it enabled behind HTTPS in deployment.

## Tests

Unit tests can run with `./mvnw -Dtest=SupplierServiceTest,SupplierControllerTest test` on the original API branch. On current application branches, select the relevant test classes from `src/test/java/`.

Database integration tests require an explicitly configured isolated database and will refuse the normal application database. Set `PROCUREFLOW_TEST_DB_URL`, `PROCUREFLOW_TEST_DB_USERNAME` and `PROCUREFLOW_TEST_DB_PASSWORD`. The allowed JDBC URL is `jdbc:postgresql://localhost:5332/procureflow_slice0_YYYYMMDDHHMMSS` with an optional lowercase suffix. Create that empty database first, then run the applicable Maven tests. Fixture integration classes have their own explicit opt-in environment flags; inspect their annotations before running them.

## Hosted configuration

The Docker image uses the `deploy` Spring profile. Supply these values through the hosting platform's secret/environment configuration:

| Variable | Purpose |
|---|---|
| `PROCUREFLOW_DB_URL` | JDBC PostgreSQL URL without an embedded password |
| `PROCUREFLOW_DB_USERNAME`, `PROCUREFLOW_DB_PASSWORD` | Database credentials |
| `PROCUREFLOW_PUBLIC_BASE_URL` | Canonical HTTPS frontend origin |
| `PROCUREFLOW_SMTP_HOST`, `PROCUREFLOW_SMTP_PORT` | Email relay |
| `PROCUREFLOW_SMTP_USERNAME`, `PROCUREFLOW_SMTP_PASSWORD` | Email credentials |
| `PROCUREFLOW_EMAIL_FROM` | Verified sender address |

The deploy profile requires certificate-verified database TLS and authenticated STARTTLS email. Flyway applies migrations and Hibernate validates the schema. Keep seed profiles and seed enable flags off for normal hosted use. Never commit account passwords, tokens, database URLs containing credentials, environment files or deployment logs.

Vercel's frontend configuration is in `frontend/vercel.json`; Render's container definition is in `render.yaml`. Match the backend and frontend source versions when publishing. Free hosting can sleep after inactivity and lose in-memory sessions on restart. Back up database data and check migration compatibility before rollback.
