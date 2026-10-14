# ProcureFlow free development deployment

Revision1, draft_persisted, 2026-10-14, Codex planner (AI-assisted). Stakeholder directive: owner DIR-DEPLOY-FREE-007. Planning/implementation/deployment are separately recorded. This document holds the small deployment requirement with normative decision, impact and test companions; no duplicate planning system. Not yet baselined or authorized for implementation.

## Actual system and constraints

Spring Boot4.1.1 / Java17 serves the existing static browser and APIs from one modular monolith. PostgreSQL and FlywayV1-V8 own commercial state. Identity registration/invitations require SMTP email and HTTPS links; sessions/CSRF must remain protected. Application source through accepted Slice8 is mostly uncommitted; local HEAD132ea7765725290c4c89abaeb263c43d0bcee38a, public origin/main eaacdca94d635ced5aa3044763e60d4685b582f3 contains only the old Slice0 application. Do not deploy that old branch and claim current ProcureFlow. Existing user index/staged work must remain intact; prepare a separate release snapshot/branch from reviewed current files when publishing is authorized. Never force-push or rewrite either history. No Dockerfile/deployment profile exists. Local Docker daemon currently unavailable; packaging/container proof is pending.

## REQ-DEP-001 - runnable existing application on free Render

Deploy the accepted Slice0-8 application, with unchanged business rules, as one Docker web service on Render Free. Static assets and APIs remain same-origin; no frontend split/CORS rewrite. Container uses Java17, a nonroot user, a bounded heap suitable for512MiB, listens on0.0.0.0 and Render PORT, with graceful shutdown. Deployment profile requires externally supplied database and SMTP settings instead of silently using local credentials. Preserve secure/HttpOnly/SameSite cookies, CSRF/auth/grants, Flyway clean disabled and Hibernate validate; no seed credentials or public privileged endpoints. Health check uses existing anonymous GET `/api/public/offerings` (DB query), no new actuator library. Deployed onboarding and a two-party RFQ/quotation/order/receipt journey must work before completion is claimed. Verification TC-DEP-001/002/004.

## REQ-DEP-002 - free address and persistent isolated database

Use Render's generated HTTPS subdomain, preferably `procureflow-ai.onrender.com` if available, otherwise its provider-assigned unique variant. Record only the observed final URL. This satisfies no `vercel` in the address; it is a free hosted subdomain, not an owned registrable domain. No domain purchase or paid first-year promotion. Use a new dedicated Neon Free PostgreSQL project/database (no time limit advertised), encrypted authenticated connection with certificate/hostname validation. Direct connection for Flyway/JPA, a small bounded pool with no idle floor so Neon can sleep. No local/business database upload, existing remote database reuse, reset or migration rewrite. Fresh remote schema gets existing V1-V8. Document free quota/cold-start/session-loss/recovery limitations and schema-safe rollback: prior compatible app artifact, never rollback database by deletion. Verification TC-DEP-001/003/004.

## REQ-DEP-003 - bounded free Grok API setup

Provision one dedicated Vercel AI Gateway key named `ProcureFlow-rfq-drafting`, 30-day expiry, total USD1 usage budget without refresh (credit consumption cap is a soft limit, not a zero-overshoot guarantee). Use only the advertised monthly USD5 free credits; do not purchase credits, enable auto-top-up, add a card or upgrade. Proposed fixed text model `spacexai/grok-4.1-fast-non-reasoning`, advertised free-credit eligible via Vertex. Keep credential out of repository, image, browser JavaScript, logs and chat; inject only as a backend secret. A fake-input, bounded API smoke (and later unchanged Slice9 corpus) establishes actual access; model catalog eligibility alone is no PASS. No provider tools/search, identity/DB/customer context or automatic commercial mutation. This task provisions API access; the existing application has no AI endpoint until Slice9's Grok revision passes its own gates. Verification TC-DEP-005.

## REQ-DEP-004 - email and zero user charges

Use the existing JavaMailSender flow with a free transactional-email account, proposed Brevo SMTP on2525 with authenticated required STARTTLS and hostname validation, bounded connection/read/write timeouts. Never silently downgrade TLS or disable verification. Set a verified sender and the actual HTTPS public base URL. Brevo account activation/sender verification and real send remain prerequisites, not promises inferred from docs. Verification email tests target only the owner's explicitly designated address; no messages to others. Do not bypass email verification or create a hardcoded demo admin to hide unavailable email. Keep new deployment resources isolated from the existing ESG/restaurant apps. Check current Render billing and free-plan overage behavior before creation; if an existing paid payment method permits automatic overage, use a new free workspace without a payment method or stop. No change to existing billing/security settings. Suspend/disable at free quotas rather than accepting charges; disclose limitations. Verification TC-DEP-002/003/004.

## Normative decisions

| Decision | Evidence/problem | Choice, alternatives/trade-offs and revisit |
| --- | --- | --- |
| DEC-DEP-001 | Existing long-running Java/JPA/session app needs a Java runtime; Render supports Docker. | One Docker Render service reuses same-origin app; Vercel frontend split adds unnecessary auth/CORS/runtime work. New container exists only for requested hosting. Revisit on measured paid/pilot requirements. |
| DEC-DEP-002 | User wants free URL without vercel; Render provides TLS/subdomain. | Hosted onrender address is simplest. Owned domains normally require purchase/renewal; no misleading promise of free owned .com. Revisit if user owns a domain. |
| DEC-DEP-003 | Render free Postgres expires30days/deletes after grace; data must persist. | New Neon Free Postgres, no temporary Render database. One ordinary JDBC database, no Neon auth/functions/framework. Revisit on quota/backup needs. |
| DEC-DEP-004 | Owner selected Grok; Vercel model catalog advertises free eligible text model. | Vercel gateway fixed Grok4.1 Fast nonreasoning through native HTTP in later Slice9, no SpringAI/new library. Free allowance, rate limits and soft budget acknowledged. No inference/quality claim yet. |
| DEC-DEP-005 | Render Free blocks SMTP25/465/587; existing identity owns email. | Configure native SMTP2525 with TLS required; free Brevo candidate. HTTP mail adapter/library would add code without evidence it is needed. Revisit if activation/TLS/delivery fails. |

## Normative impact/traceability

REQ-DEP-001 -> DEC-DEP-001 -> issue pending -> proposed new Dockerfile/.dockerignore/application-deploy.properties/render.yaml -> no business API/DB changes -> TC-DEP-001/004 -> VER-DEP pending.

REQ-DEP-002/004 -> DEC-DEP-002/003/005 -> issue pending -> those config files + proposed docs/deployment.md/.gitignore secret protection -> existing identity SMTP/link flow and V1-V8 -> TC-DEP-002/003/004 -> VER-DEP pending.

REQ-DEP-003 -> DEC-DEP-004 -> issue pending -> provider account secret only; no application AI code -> TC-DEP-005 -> observed provider proof pending. Exact created/modified/deleted inventory recorded after implementation. Existing frozen Slice0-8 unchanged; Slice9 remains separate unapproved work.

Feature-to-DB map: hosting copies the same schema into a new empty authorized remote database; existing entity/FK/cardinality/owning-side maps in Slice3-8 traceability remain unchanged. Deployment/AI credentials have no application entity or FK. Cross-module map: deployment env -> Spring native datasource/mail/server configuration -> existing identity/procurement/marketplace. No new module relation or repository. Proposed study reading: this document -> application.properties -> IdentityEmailService -> IdentitySecurityConfiguration -> Dockerfile/deploy profile -> docs/deployment.md -> actual smoke evidence. No completed deployment study or test PASS yet.

## Normative verification expectations

Numeric configuration contract: Java heap initial64MiB/max256MiB, metaspace max128MiB, code cache max64MiB; container memory512MiB; one instance. JDBC pool max3/min-idle0, connection acquisition10s, idle timeout30s, max lifetime10min, keepalive disabled; URL connect timeout10s/socket timeout60s. SMTP connection5s/read10s/write10s, STARTTLS required and server identity checked. Synthetic Grok smoke has2s connect and60s total deadline through body reading,32KiB raw response cap, temperature0/max1024 completion tokens, fixed model, no tools, one request at a time and no automatic retries. Later six-case evaluation uses these same caps, but is not an accuracy PASS until actually observed. On provider failure/timeout/quota error stop and record it; do not top up or switch models implicitly. All these values are development limits, not throughput/latency guarantees; verify actual container fits512MiB before publication.

Additional proof boundaries: use only a fixed fake Grok smoke input of at most1000 Unicode code points. Immediately before sending, verify the selected model is still eligible, free credit is available and the selected account has no purchased-credit/auto-top-up/other charge-capable path for this request; if any is uncertain or insufficient, do not send. TheUSD1 soft budget alone is not proof of zero charges. Container startup and the TC-DEP-004 browser journey must complete under the512MiB memory limit without OOM/restart; otherwise revise the deployment plan rather than choosing a paid instance implicitly.

| Test | Proof |
| --- | --- |
| TC-DEP-001 | Package current source, build/run actual container against a newly guarded isolated PostgreSQL DB; migrationsV1-V8, Hibernate validate, overridden port, nonroot UID, bounded memory, public DB-read200, anonymous private401, secure cookie/CSRF behavior; deployment profile missing required DB/SMTP config fails instead of local fallback. Existing sufficient test evidence preserved; observe current relevant proof before publication. |
| TC-DEP-002 | Check SMTP2525 STARTTLS/certificate support, configure verified sender, register owner-designated email through HTTPS, receive correct one-time link, verify/login, revoked access behavior unchanged. Sender/account/SMTP failure must not be labelled complete; no email to others. |
| TC-DEP-003 | Actual plans are free with no payment additions, credits purchase or auto-top-up; recorded free-only resources isolated from other apps, observed HTTPS URL excludes vercel, no30-day expiring DB; inspect secret exclusion/image context without printing secrets. |
| TC-DEP-004 | Publish reviewed source snapshot to a separate deployment branch preserving user work/remote history, deploy exact observed commit, confirm live readiness and small real-browser buyer/supplier journey through receipt; errors and cold starts disclosed. Rollback previous compatible app only; databases/local volumes untouched. |
| TC-DEP-005 | Actual key creation after action-time authorization, verify name/budget/expiry/secret placement, one bounded synthetic Grok request through HTTPS with no tools/customer data; capture status/model/usage/error with key redacted. Slice9 accuracy gate remains the unchanged six-case corpus and separate review. |

## Captured primary evidence (2026-10-14 browser)

- https://render.com/docs/free : idle15min,750 shared hours, ephemeral FS, SMTP25/465/587 blocked; Free Postgres expires30days; overage billed if payment method exists.
- https://neon.com/pricing : Free USD0, no time limits/card required,1GB/project,100CU-hrs/month/project, scale-to-zero, limited restore window.
- https://vercel.com/docs/ai-gateway/pricing : free USD5/month, eligible subset/rate limits, paid credit purchase ends free allowance; budgets soft caps.
- https://vercel.com/ai-gateway/models/grok-4.1-fast-non-reasoning : identifier spacexai/grok-4.1-fast-non-reasoning, Vertex free-credit eligible; input0.20/output0.50 USD per1M tokens. Actual request pending.
- https://developers.brevo.com/docs/smtp-integration : SMTP2525 supported; requires SMTP key (not API key). STARTTLS/certificate/activation/delivery proof pending. No terms accepted, account/key/resource created or payment committed.
