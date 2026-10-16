# VER-FDP-HOSTED-001 — separate hosted publication review

In progress, 2026-10-16. Reviewer /root/fdp_final_review, separate I1 context, no application/provider mutation. Criteria REQ-FDP-001..003@R1 / BL-FDP-R1 / issue34. Development/package PASS is prerequisite, not hosted proof. Scoped publication authority GATE-FDP-PUBLISH-DELEGATED independently read.

Independently observed clean release checkout and HEAD ab4450a0b7ff9aaf49393dfae8fce6fa3d7a81aa; remote refs/heads/codex/fsb-demo-production returns identical exact SHA. Range from f61fcdb1943f335682d8c39ad5fbc662b1d4c122 has25 logical paths, comprising11 reviewed application/setup/test changes plus bounded FDP provenance. Inspected actual additional approval/state/index/reading-map diff; no unrelated application changes. Independently bound all11 committed source paths to reviewed main source after LF normalization. Committed frozen requirement SHA256 remains e6a29a8cd78ea9f3365079482c123509c059d91b69a690fbf35b97e72827f232.

Provider IDs reported by coordinator for later evidence binding: Render dep-db8mgou7bikc73cr6ktg, Vercel preview9sxsTFwFmGy1HWoGy3D3VvxUmbr5, exact commit above. Provider/readiness/fixture/account/tenant/temp-switch removal/live frontend evidence not yet independently reviewed; no hosted verdict or success inferred from these reported IDs. Shared browser deliberately left to coordinator; reviewer will independently run bounded public HTTP after readiness and inspect actual I0 hosted evidence with limitations stated.

## Hosted correction required — NEEDS CHANGES

Coordinator observed exact Render deployment fail after additive V10 with Production demo refused: Remote fixture connection requires TLS. Existing deploy datasource requires verify-full with DefaultJavaSSLFactory. Neon terminates client TLS at proxy; pg_stat_ssl describes the proxy-to-database hop and therefore cannot establish JVM-to-proxy encryption. No frontend promotion or fixture-write success is inferred.

1. Requirement violations: REQ-FDP-002 deliberate fixture cannot complete with required existing datasource security on actual host.
2. Workflow violations: none observed; backend-first rollout correctly stopped.
3. Architecture violations: none beyond misplaced connection-security observation noted below.
4. Bugs: F-FDP-HOST-001, high, ProductionDemoSeeder.run remote TLS guard. Expected verified secure remote datasource accepted; actual server-hop pg_stat_ssl false rejects legitimate client TLS behind proxy.
5. Missing tests: add effective Hikari security property/URL override negative guards and positive proxy-compatible validated connection path.
6. Missing/incorrect traceability: update TLS observation map after correction.
7. Missing/incorrect code-reading documentation: explain actual effective driver settings/URL validation rather than server-hop TLS proof.
8. Security concerns: correction must not disable TLS checks or trust Environment alone; validate effective datasource properties, reject native/JNDI/custom security overrides and URL weakening.
9. Overengineering / unnecessary complexity: none; use existing Hikari native getters/dependency, no reflection/new infrastructure.
10. Exact corrections FCC must make: replace pg_stat_ssl with effective URL-mode Hikari datasource TLS contract (verify-full, trusted default SSL factory, no custom verifier/socket factory, safe query overrides), preserve actual transaction metadata/database checks; focused positive/negative tests, package/review exact correction before redeploy. Keep V10 unchanged because already applied.

## Corrective source/package re-review — PASS

F-FDP-HOST-001 correction independently inspected: ProductionDemoSeeder.validateRemotePool/validateRemoteTls replaces server-hop pg_stat_ssl with actual JdbcTemplate Hikari datasource validation inside the transaction. Only JDBC pool mode accepted; custom/native/JNDI datasource overrides refused. Required driver properties verify-full and DefaultJavaSSLFactory, with no custom hostname verifier/socketFactory; all remote URL query overrides refuse rather than implementing another parser. Actual PostgreSQL metadata/current_database, lock/transaction/collision/receipt and fixture insertion remain unchanged. No dependency/datasource-setting/applied-migration change. This preserves frozen requirement security intent and accepts normal verified client TLS behind proxy.

Read final guard source and observed target/fdp-tls-tests.log eight focused cases (4 guard/4 integration), zero failure/error/skip BUILD SUCCESS; after additional effective-pool override assertions, release guard verify4/4 BUILD SUCCESS and final JAR packaged in target/fdp-tls-release.log. Reviewed actual six staged corrective paths: source/test plus setup/trace/results/study; no unrelated change. Independently matched three correction-manifest source/setup raw hashes and reviewed/main/release/staged bytes; reran combined eleven-source binding and14static/V10/seeder-class JAR checks, all PASS.

Corrective package is technically PASS for scoped publication under existing owner authority; no application correction remains before exact corrected deploy. This closes the demonstrated source bug, not hosted execution. Initial failed deploy remains disclosed. Await actual corrected remote SHA/backend readiness/fixture login-tenancy/front-end promotion/temp switch removal and independent public HTTP before overall hosted verdict.
