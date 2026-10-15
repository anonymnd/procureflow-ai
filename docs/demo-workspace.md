# Local demo workspace

This fixture is opt-in and local only. Regular registration and hosted databases are unchanged. It creates one buyer administrator and two supplier administrators with published storefronts and ten products each, one product per category. No email, quotations, orders, prices or payments are fabricated.

Use PostgreSQL 17 and a **new** database named `procureflow_demo_<your_suffix>` on `localhost`, `127.0.0.1` or `::1`. Supply its credentials explicitly. Do not use an existing business database. The runner checks both the declared URL and actual transaction connection metadata URL (including Hikari overrides), plus the actual connected database and refuses the `deploy` profile, remote hosts and JDBC query overrides.

PowerShell from the repository root, after creating that isolated database:

```powershell
$env:PROCUREFLOW_DEMO_DB_URL='jdbc:postgresql://localhost:5332/procureflow_demo_example'
$env:PROCUREFLOW_DEMO_DB_USERNAME='<local database user>'
$env:PROCUREFLOW_DEMO_DB_PASSWORD='<local database password>'
$env:PROCUREFLOW_DEMO_PASSWORD='<choose a 12–128 character demo password>'
$env:PROCUREFLOW_DEMO_SEED_ENABLED='true'
rtk npm --prefix frontend run build
rtk proxy .\mvnw.cmd -o -DskipTests package
rtk proxy java -jar target/app-0.0.1-SNAPSHOT.jar --spring.profiles.active=demo --server.port=18140
```

Use the actual jar filename produced in `target` if it differs. Open `http://127.0.0.1:18140/?view=login`. All three fixture accounts use the explicitly supplied demo password:

| Account | Organization | Access |
| --- | --- | --- |
| buyer@procureflow.invalid | Demo Buyer Studio | Buyer/admin; opens catalog |
| supplier.one@procureflow.invalid | Atlas Demo Supply | Supplier/admin; ten products |
| supplier.two@procureflow.invalid | Meridian Demo Goods | Supplier/admin; ten products |

The `.invalid` addresses are fictional and verified only by this guarded fixture. Regular users still verify their email. The current local preview password is stored only in ignored `target/wd-demo-password.txt`; never commit it or reuse it for a real account.

Repeat startup is a no-op for a complete compatible fixture and preserves passwords and product edits/publication. Partial data, changed fixture organization identity/grants, or name collisions cause startup refusal with no repair/reset. Concurrent startup uses one PostgreSQL transaction and advisory transaction lock. Additional product creation may make the fixed ten-per-supplier fixture incompatible on restart; use a new isolated demo database for another pristine demo. No cleanup command is provided.

Buyer journey: browse/filter products → Request quotation → edit quantity/unit/specification → explicitly create draft → choose supplier recipients and send → open Quotations → compare submitted offers → review/accept → track order → acknowledge receipt. Supplier journey: Your products/Supplier profile → Received RFQs or Quotations → create/save/submit quotation → Orders & tracking → confirm/process/dispatch. My profile edits your name; organization name is admin-only. Help explains the steps. Published products have prices on request, not checkout prices.

Development acceptance and publication are separate. These accounts and new features have not been inserted or deployed to the live service.

## Production demonstration fixture (REQ-FDP-002)

The distinct `ProductionDemoSeeder` is opt-in under `deploy`, with `PROCUREFLOW_PRODUCTION_DEMO_ENABLED=true`, `PROCUREFLOW_PRODUCTION_DEMO_DATABASE` set to the exact observed database, and `PROCUREFLOW_PRODUCTION_DEMO_PASSWORD` supplied privately (20–128 characters, at least ten distinct characters). Never reuse a real account password. The existing local runner above remains local-only.

The new fixture creates Demo Buyer Workshop, Demo Atlas Supply and Demo Meridian Goods. Each has an administrator and employee:

| Organization | Administrator | Employee |
| --- | --- | --- |
| Demo Buyer Workshop | demo.buyer@procureflow.invalid | demo.buyer.employee@procureflow.invalid |
| Demo Atlas Supply | demo.atlas@procureflow.invalid | demo.atlas.employee@procureflow.invalid |
| Demo Meridian Goods | demo.meridian@procureflow.invalid | demo.meridian.employee@procureflow.invalid |

All six use the supplied demo password and ordinary Sign in. Only these new fictional accounts are preverified; sessions, CSRF, grants and tenancy apply normally. No mail is sent. Two supplier storefronts contain 20 published products; the buyer has seven RFQs, six quotations and four orders spanning Created, Processing, Dispatched and Received. All terms explicitly forbid real fulfillment/payment.

The runner checks the actual transaction PostgreSQL URL/database and TLS for remote connections. An advisory transaction lock serializes startup; V10 adds `demo_seed_completions`, written last in the same transaction. Its ID arrays are provenance without FKs, so the receipt does not prevent later account deletion. Reruns validate immutable account/email/organization bindings and change nothing, preserving names, grants, passwords and commercial progress. Reserved identity collisions, incomplete data or changed bindings refuse startup without repair. Disable the enable flag and remove the temporary password setting after verified insertion. Keep the database target for documentation if desired. Never reset/delete customer data to retry.

Publication status and observed fixture execution are recorded in `.agile-v/verification/fsb-demo-production-results.md`; source availability alone does not establish that production was seeded.
