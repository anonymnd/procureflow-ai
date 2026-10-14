# Free development deployment

Scope: accepted Slice0-8, REQ-DEP-001..004 / BL-DEP-R1 / [issue28](https://github.com/anonymnd/procureflow-ai/issues/28). See current STATE and deployment verification records for actual progress. Configuration alone is not evidence of deployment. Grok key provisioning is separate from unapproved/unimplemented Slice9 application behavior.

Use one Render Free Docker service in a dedicated Hobby workspace **without a payment method**, and a new Neon Free PostgreSQL database. Preserve the existing paid-capable workspace and unrelated services. Render Free Postgres expires30days; do not use it. Render's generated HTTPS `*.onrender.com` address contains no `vercel`; it is a hosted subdomain, not ownership of a registrable domain. No domain purchase, paid plan, credit purchase or auto-top-up.

## Source and runtime

Public main has only the old starter application. Publish a separately reviewed snapshot of current accepted files to a deployment branch, preserving remote history and local staged/unstaged work. Select the exact observed commit. No force-push or unrelated README overwrite. `render.yaml` disables automatic deploys and uses existing DB-read readiness `/api/public/offerings`.

Read `Dockerfile`, `docker-entrypoint.sh`, `.dockerignore`, then `src/main/resources/application-deploy.properties`. Maven3.9.16 builds Java17; runtime UID/GID10001 executes only the packaged jar. Image context permits only build inputs. Entrypoint requires nonempty settings and an HTTPS public URL without printing credentials. Native profile listens on Render PORT/default10000, handles platform forwarded headers, keeps secure cookies/CSRF and graceful shutdown. Same-origin browser/APIs avoid a frontend split. No business API/entity/FK/migration changes.

Heap64-256MiB, metaspace128MiB, code cache64MiB; verify512MiB startup/journey without OOM/restart. Pool3/min-idle0, connection10s/idle30s/lifetime10min, keepalive0. PostgreSQL `verify-full` plus Java trust store checks host/certificate; connect10s/socket60s. SMTP uses required STARTTLS/server identity checks and5s connection/10s read/10s write bounds. Local fake non-TLS DB tests may use a documented framework override; never copy TLS-disabling settings to Render.

## Environment contract

| Name | Source |
| --- | --- |
| SPRING_PROFILES_ACTIVE | deploy |
| PROCUREFLOW_DB_URL | Dedicated Neon direct JDBC URL: jdbc:postgresql://host:5432/database; no password in URL. |
| PROCUREFLOW_DB_USERNAME / PROCUREFLOW_DB_PASSWORD | Dedicated database role credentials, platform secrets. |
| PROCUREFLOW_PUBLIC_BASE_URL | Actual observed Render HTTPS URL, no trailing slash. |
| PROCUREFLOW_SMTP_HOST | Activated provider relay; proposed smtp-relay.brevo.com. |
| PROCUREFLOW_SMTP_PORT | 2525; Render Free blocks25/465/587. |
| PROCUREFLOW_SMTP_USERNAME / PROCUREFLOW_SMTP_PASSWORD | SMTP login/key, not an HTTP API key; platform secrets. |
| PROCUREFLOW_EMAIL_FROM | Owner-designated, provider-verified sender. |

Account activation, sender verification and real TLS/delivery are prerequisites. Do not bypass email verification, seed a public admin or send to undesignated recipients. Owner completes any required new password/terms/verification. Put keys only in platform secret settings, never Git, chat, image arguments, logs or browser source. Root .env and *.secrets files are ignored. A new empty database receives unchanged FlywayV1-V8; Hibernate validates, Flyway clean disabled. Never upload/reset local business data.

## Grok and completion

Proposed HTTPS gateway: https://ai-gateway.vercel.sh/v1/chat/completions, model spacexai/grok-4.1-fast-non-reasoning. Dedicated key ProcureFlow-rfq-drafting,30-day expiry,USD1 total soft budget/no refresh. Advertised monthly free credits and model eligibility are not access/accuracy proof. Immediately before inference verify eligibility, available free credits and no charge-capable path; otherwise do not send. No purchase/card/top-up or implicit fallback.

Fake-input smoke<=1000 codepoints, temperature0/max1024 completion tokens,2s connect/60s total/32KiB body, one request/no retry/no tools. Key has no current application consumer. Slice9 must separately revise/review its Grok contract and pass the unchanged real six-case corpus before acceptance.

Before publication: package and run actual container against a newly owned isolated DB; verify nonroot/512MiB/port, V1-V8, DB-read200, private401, secure cookie/CSRF and safe missing-setting failure. Record hashes and separate-context review. Before completion: verify deployed commit/HTTPS/free plans, owner-authorized verification email/login and buyer/supplier RFQ-through-receipt journey. Record actual URL/errors/proof, not planned addresses.

Rollback only to a compatible tested application artifact; preserve schema/data. Neon Free has quotas and a limited restore window, not production backup guarantees. Render sleeps15min idle, shares750 hours/month and loses in-memory sessions on restart. Quotas/cold starts bound this learning deployment; service/assistance must stop rather than upgrade or charge. No permanent-price/uptime/production-readiness promise.
