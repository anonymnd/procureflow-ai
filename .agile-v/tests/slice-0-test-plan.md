# Slice 0 draft test plan and verification commands

Revision 1; companion to [requirement revision 1](../requirements/slice-0.md). Status: proposed; no approved baseline or application test implementation yet. This proposal is part of specification drafting, not formal execution of test-designer against a baselined requirement. After approval, bind every test to the approved requirement revision/hash and independently review its expected behavior.

## Test cases

| ID | Requirements | Level / procedure | Expected behavior |
|---|---|---|---|
| TC-S0-001 | REQ-S0-001 | Wrapper version and unit command below | Maven3.9.16; observed JDK recorded; exit0 |
| TC-S0-002 | REQ-S0-001/003 | Existing SupplierServiceTest; strengthen shouldUpdateSupplier to inspect entity passed to save | Nine existing behaviors pass; update actually changes existing entity name, not just mocked response |
| TC-S0-003 | REQ-S0-002/006 | AppApplicationTests plus real-DB integration context | Guarded target; FlywayV1 success; Supplier managed, User not managed; validate succeeds |
| TC-S0-004 | REQ-S0-003/005 | SupplierApiIntegrationTest: create/get/list/update/delete against PostgreSQL | Status/body contract; update persisted on reread; no Location promise; duplicate names allowed |
| TC-S0-005 | REQ-S0-003 | Fresh isolated DB empty list; transactional integration empty-list case | 200 `[]`; no sorting assumption |
| TC-S0-006 | REQ-S0-004 | MVC + real integration missing GET/PUT/DELETE | 404 ApiError status404/error Resource Not Found; no row changes |
| TC-S0-007 | REQ-S0-005 | POST/PUT matrix: missing/null/empty/whitespace, 1,255,256 ASCII chars | Invalid400 VALIDATION_ERROR, no service invocation in MVC test and no DB mutation in integration; valid1/255 persist |
| TC-S0-008 | REQ-S0-004 | POST/PUT malformed/missing JSON; GET/PUT/DELETE `/not-a-number` and overflowing long; negative numeric absent ID | JSON/nonnumeric/overflow400 BAD_REQUEST; negative numeric absent404; no writes |
| TC-S0-009 | REQ-S0-004 | MVC test service throws DataIntegrityViolationException / unexpected exception with sentinel sensitive message | 409 DATA_CONFLICT / 500 INTERNAL_SERVER_ERROR; ApiError fields; no sentinel SQL/password/stack details |
| TC-S0-010 | REQ-S0-001/002/003/007 | Package, real HTTP smoke, second jar startup on same test DB; SQL assertions | CRUD/errors work; persistent marker survives restart; exactly one successful V1; no future tables added |
| TC-S0-011 | REQ-S0-002/006 | Slice0DatabaseGuardTest + misconfigured context preflight | Unsafe/missing target rejected before datasource/Flyway construction; valid URL permits startup; schema creation/update/clean settings rejected |
| TC-S0-012 | REQ-S0-006/007 | Source/worktree preservation + command review | Original source/planning data preserved except approved scoped changes; no drops/volume removals, no unexpected runtime dependency |
| TC-S0-013 | REQ-S0-007 | Numbered documentation/traceability review below | Actual issues/requirements/decisions/files/DB/API/tests/results agree; README and reading guide accurately reflect observed behavior |

No new test dependencies are proposed. MVC tests use the Boot4 packages from existing starters (check official docs); full-context MockMvc tests use a real DB and rollback. Actual port HTTP smoke is separate: server writes are not rolled back by a test-thread transaction.

## Safe test configuration contract (to implement after approval)

- `application-slice0-test.properties` resolves `spring.datasource.url`, username, password from explicit `PROCUREFLOW_TEST_DB_URL`, `PROCUREFLOW_TEST_DB_USERNAME`, `PROCUREFLOW_TEST_DB_PASSWORD`; no normal-datasource fallback.
- `ddl-auto=validate`, Flyway enabled, Flyway clean disabled. No SQL cleanup/truncate fixtures against an existing DB.
- Shared test-only `Slice0DatabaseGuard` is an ApplicationContextInitializer used by BOTH full-context test classes, before datasource/Flyway initialization. Guard the final resolved Spring property, not just an unrelated environment variable.
- Allowed resolved URL: `jdbc:postgresql://localhost:5332/procureflow_slice0_` followed by 14 decimal timestamp digits, optionally `_` and lowercase alphanumeric suffix. Reject query parameters/alternate hosts/ports/business DBs and credentials embedded in URL. Also reject unsafe resolved schema-generation/clean settings and separate unsafe Flyway URL overrides. If a separate Flyway URL is configured, require it equal the guarded datasource URL.
- Unit rejection matrix: absent/blank URL; existing `ProcureFlow`; `postgres`; remote host; localhost wrong port; similar prefix without timestamp; query parameter override; alternate Flyway URL; ddl-auto update/create/create-drop; enabled Flyway clean. Assert the initializer throws without constructing a datasource or invoking migration. Use a Spring environment/context fixture, not a live database.
- Guard must not skip missing-DB tests. An absent URL stops with an explicit preflight message. Full-suite success requires the real approved test database.

## Exact verification steps after approval and implementation

These commands are NOT authorized to run yet against a database. Run from the repository root in Windows PowerShell. Stop on every nonzero native exit code; record command, relevant actual output and exit code. Commands use explicit checks because Windows PowerShell does not automatically stop after a failing native command.

### 1. Capture baseline and inspect tools

```powershell
git status --short
git diff --stat
java -version
docker compose config --quiet
if ($LASTEXITCODE -ne 0) { throw 'Compose configuration failed' }
docker inspect postgresql --format '{{.State.Status}}'
if ($LASTEXITCODE -ne 0) { throw 'Existing container unavailable; do not recreate it' }
Get-FileHash src/main/resources/db/migration/V1__create_supplier_table.sql
```

Record the current source fingerprints/diff before implementation. Restore only the agreed wrapper file from HEAD; do not reset any other files. Test the restored entry point:

```powershell
.\mvnw.cmd -v
if ($LASTEXITCODE -ne 0) { throw 'Wrapper failed' }
.\mvnw.cmd '-Dtest=SupplierServiceTest,SupplierControllerTest,Slice0DatabaseGuardTest' test
if ($LASTEXITCODE -ne 0) { throw 'Unit/MVC/guard tests failed' }
```

Expected: Maven3.9.16 and observed JDK; all selected tests pass without DB access. Missing-test failures are real failures, not reasons to allow no tests.

### 2. Start the existing server and create a new isolated database

```powershell
docker start postgresql
if ($LASTEXITCODE -ne 0) { throw 'Existing PostgreSQL cannot start; preserve its volume' }
docker exec postgresql pg_isready -U ProcureFlow -d postgres
if ($LASTEXITCODE -ne 0) { throw 'PostgreSQL not ready; inspect and retry readiness only' }
$slice0DbName = 'procureflow_slice0_' + (Get-Date -Format yyyyMMddHHmmss) + '_' + ([guid]::NewGuid().ToString('N').Substring(0,8))
if ($slice0DbName -notmatch '^procureflow_slice0_[0-9]{14}_[a-z0-9]{8}$') { throw 'Unsafe database name' }
docker exec postgresql createdb -U ProcureFlow -O ProcureFlow $slice0DbName
if ($LASTEXITCODE -ne 0) { throw 'Cannot create isolated DB; do not reuse or reset another database' }
$env:PROCUREFLOW_TEST_DB_URL = 'jdbc:postgresql://localhost:5332/' + $slice0DbName
$env:PROCUREFLOW_TEST_DB_USERNAME = 'ProcureFlow'
$env:PROCUREFLOW_TEST_DB_PASSWORD = 'password'
Write-Output $slice0DbName
```

The password shown is the repository's existing local-development default, not a production secret. If the stored cluster uses different approved credentials, supply those locally without recording them in evidence; otherwise stop for an environment decision. `pg_isready` alone does not prove JDBC credentials or create-database privilege. The application tests must establish those. No `docker compose up` recreation, drop database, Flyway clean/repair or volume deletion.

### 3. Run the full guarded suite and package

```powershell
.\mvnw.cmd verify
if ($LASTEXITCODE -ne 0) { throw 'Full guarded test/package verification failed' }
Get-ChildItem target/surefire-reports -Filter 'TEST-*.xml'
Get-Item target/app-0.0.1-SNAPSHOT.jar
docker exec postgresql psql -U ProcureFlow -d $slice0DbName -v ON_ERROR_STOP=1 -c 'SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;'
if ($LASTEXITCODE -ne 0) { throw 'Migration history query failed' }
docker exec postgresql psql -U ProcureFlow -d $slice0DbName -v ON_ERROR_STOP=1 -c "SELECT table_name FROM information_schema.tables WHERE table_schema='public' ORDER BY table_name;"
if ($LASTEXITCODE -ne 0) { throw 'Schema query failed' }
```

Expected: every required test runs with zero failures/errors/skips; built jar exists; one successful version1 migration; public business table suppliers plus Flyway history, no users/organization/RFQ/etc tables. Integration fixtures roll back, so the smoke database remains empty of Supplier rows before step4. Table absence is verification, not a migration to remove unknown existing tables.

### 4. Start the real jar in terminal A

Use the same explicit database environment from step2. Before BOTH initial launch and restart, run this preflight. Reject missing credentials, unsafe targets, and inherited Spring/Hibernate/JVM configuration that could override schema generation or targets. Command-line options pin the effective datasource, Flyway target and schema policies; load only the bundled application.properties. No application guard/infrastructure is added for the manual smoke launch.

```powershell
if ($env:PROCUREFLOW_TEST_DB_URL -notmatch '^jdbc:postgresql://localhost:5332/procureflow_slice0_[0-9]{14}_[a-z0-9]{8}$') { throw 'Unsafe smoke DB target' }
if ([string]::IsNullOrWhiteSpace($env:PROCUREFLOW_TEST_DB_USERNAME) -or [string]::IsNullOrWhiteSpace($env:PROCUREFLOW_TEST_DB_PASSWORD)) { throw 'Missing explicit test credentials' }
$slice0OverrideVars = @(Get-ChildItem Env: | Where-Object { $_.Name -match '^(SPRING_|HIBERNATE_)' -or $_.Name -in @('JAVA_TOOL_OPTIONS','JDK_JAVA_OPTIONS','_JAVA_OPTIONS') })
if ($slice0OverrideVars.Count -gt 0) { throw 'Inherited configuration present; use a clean verification shell and preserve unrelated settings' }
$slice0JarArgs = @(
  '-jar', 'target/app-0.0.1-SNAPSHOT.jar',
  '--spring.config.location=classpath:/application.properties',
  '--spring.profiles.active=slice0-smoke',
  '--server.address=127.0.0.1', '--server.port=8080',
  "--spring.datasource.url=$env:PROCUREFLOW_TEST_DB_URL",
  '--spring.datasource.username=${PROCUREFLOW_TEST_DB_USERNAME}',
  '--spring.datasource.password=${PROCUREFLOW_TEST_DB_PASSWORD}',
  "--spring.flyway.url=$env:PROCUREFLOW_TEST_DB_URL",
  '--spring.flyway.user=${PROCUREFLOW_TEST_DB_USERNAME}',
  '--spring.flyway.password=${PROCUREFLOW_TEST_DB_PASSWORD}',
  '--spring.jpa.hibernate.ddl-auto=validate',
  '--spring.flyway.enabled=true', '--spring.flyway.clean-disabled=true'
)
java @slice0JarArgs
```

Expected: Flyway validates current history; Hibernate validates; server starts. Record the approved DB URL and evidence of both migration/schema checks. Credential placeholders are passed literally and resolved from environment, not embedded in command output. If port8080 is occupied, stop for/record an agreed alternate local port and consistently update the following URL; do not kill unrelated processes. The test-only profile is not packaged; slice0-smoke simply selects a deliberately named profile with no added configuration file. Source review confirms only the bundled application.properties applies; if future code adds configuration overrides, revise this launch contract before using it.

### 5. Real HTTP positive flow in terminal B

```powershell
$slice0Api = 'http://127.0.0.1:8080/api/suppliers'
$slice0Empty = Invoke-WebRequest -UseBasicParsing -Uri $slice0Api -TimeoutSec 10
if ($slice0Empty.StatusCode -ne 200 -or $slice0Empty.Content -ne '[]') { throw 'Fresh empty list contract failed' }
$slice0Created = Invoke-WebRequest -UseBasicParsing -Uri $slice0Api -Method Post -ContentType 'application/json' -Body '{"name":"Atlas Supplies"}' -TimeoutSec 10
if ($slice0Created.StatusCode -ne 201) { throw 'POST must return 201' }
$slice0CreatedBody = $slice0Created.Content | ConvertFrom-Json
$slice0SupplierId = $slice0CreatedBody.id
if ($slice0SupplierId -le 0 -or $slice0CreatedBody.name -ne 'Atlas Supplies') { throw 'Create response failed' }
$slice0Read = Invoke-WebRequest -UseBasicParsing -Uri "$slice0Api/$slice0SupplierId" -TimeoutSec 10
if ($slice0Read.StatusCode -ne 200 -or ($slice0Read.Content | ConvertFrom-Json).name -ne 'Atlas Supplies') { throw 'GET failed' }
$slice0Updated = Invoke-WebRequest -UseBasicParsing -Uri "$slice0Api/$slice0SupplierId" -Method Put -ContentType 'application/json' -Body '{"name":"Updated Supplies"}' -TimeoutSec 10
if ($slice0Updated.StatusCode -ne 200 -or ($slice0Updated.Content | ConvertFrom-Json).id -ne $slice0SupplierId) { throw 'PUT response failed' }
$slice0ReadUpdated = Invoke-WebRequest -UseBasicParsing -Uri "$slice0Api/$slice0SupplierId" -TimeoutSec 10
if (($slice0ReadUpdated.Content | ConvertFrom-Json).name -ne 'Updated Supplies') { throw 'Update not persisted' }
$slice0Deleted = Invoke-WebRequest -UseBasicParsing -Uri "$slice0Api/$slice0SupplierId" -Method Delete -TimeoutSec 10
if ($slice0Deleted.StatusCode -ne 204 -or $slice0Deleted.Content.Length -ne 0) { throw 'DELETE contract failed' }
```

Then execute TC006-008 as numbered HTTP steps, using `curl.exe -i` (prints error status/body without PowerShell throwing). Replace `<deleted-id>` with `$slice0SupplierId`; replace `<absent-id>` with 9223372036854775807, verified absent in this fresh test DB.

1. GET `/api/suppliers/<deleted-id>` ->404; repeated DELETE of `<deleted-id>` ->404; PUT valid name and DELETE `<absent-id>` ->404.
2. POST a new `{"name":"Negative-case fixture"}` ->201; retain its ID as `$slice0FixtureId`. Before each rejected operation, GET that row and GET list to record its id/name and count; after rejection repeat those reads and require identical row/count. POST and PUT `$slice0FixtureId` with `{}`, `{"name":null}`, `{"name":""}`, `{"name":"   "}` ->400 VALIDATION_ERROR.
3. POST1/255 ASCII characters ->201; PUT `$slice0FixtureId`1/255 ->200; POST/PUT256 ->400. Record the new fixture name/count after successful operations as the next before-state; rejected operations must leave that state unchanged.
4. Both POST and PUT `$slice0FixtureId` with malformed body `{"name":` and with no body (JSON content type) ->400 BAD_REQUEST; apply before/after checks from step2.
5. Each of GET/PUT/DELETE `/api/suppliers/not-a-number` and `/api/suppliers/9223372036854775808` ->400 BAD_REQUEST; use a valid name body for PUT. Absent negative numeric ID ->404. Apply before/after checks from step2.
6. Inspect all errors: timestamp/status/error/message exist, status matches HTTP, no SQL/credentials/stack trace. TC009's forced409/500 are tested in the MVC suite rather than intentionally damaging the database.

Command example for step1:

```powershell
curl.exe --max-time 10 -i "$slice0Api/$slice0SupplierId"
if ($LASTEXITCODE -ne 0) { throw 'HTTP request failed to complete' }
```

For steps2-5, prefer the automated MVC/integration matrix as the repeatable assertions; retain HTTP status/body from at least the missing-resource and invalid-name smoke examples. A successful curl process alone is not proof of the expected HTTP status.

### 6. Restart and persistence/migration evidence

```powershell
$slice0Marker = Invoke-WebRequest -UseBasicParsing -Uri $slice0Api -Method Post -ContentType 'application/json' -Body '{"name":"Slice0 restart marker"}' -TimeoutSec 10
if ($slice0Marker.StatusCode -ne 201) { throw 'Marker creation failed' }
$slice0MarkerId = ($slice0Marker.Content | ConvertFrom-Json).id
Write-Output $slice0MarkerId
```

Stop only the foreground application in terminal A with Ctrl+C, then repeat the ENTIRE step4 preflight and launch using the same datasource. In B, GET `$slice0Api/$slice0MarkerId` ->200 and exact marker name. Copy the generated DB name from step2 into `$slice0DbName` in B and run:

```powershell
if ($slice0DbName -notmatch '^procureflow_slice0_[0-9]{14}_[a-z0-9]{8}$') { throw 'Unsafe DB target' }
docker exec postgresql psql -U ProcureFlow -d $slice0DbName -v ON_ERROR_STOP=1 -c 'SELECT version, success FROM flyway_schema_history ORDER BY installed_rank;'
if ($LASTEXITCODE -ne 0) { throw 'Restart history query failed' }
```

Expected one successful V1, marker preserved, no schema drift. Leave this dedicated DB/container/volume intact. Do not stop a shared database service as cleanup. Remove only test datasource environment variables from the verification shell when done, or close that shell.

### 7. Documentation and traceability review

1. Compare actual diff to the approved requirement baseline and proposed file map; record exact created/modified/deleted files, and explain any approved scope change.
2. Check original source/planning fingerprints; approved edits are explained, all other original paths match. No V1/Compose/pom change or data reset.
3. Follow each REQ through decision, real issue, actual files/classes, DB/API and actual test methods to observed results. Unrun tests are not passes.
4. Read README from a fresh developer perspective and execute its agreed commands. Clearly distinguish implemented Supplier baseline, unfinished User, and future product plans; name-only examples; no Location promise.
5. Read `docs/code-reading-guide.md` in its recommended order; confirm class dependencies, endpoint/service/repository/DTO/auth mapping, cardinalities/FKs and before/after explanations against actual source.
6. Record fresh-context review scope/results separately from builder self-checks. Do not label self-review independent verification.
7. Update results, study guide and workflow state only after all required behavior/evidence is present. User approval remains a separate record.

## Evidence format

For each `VER-S0-NNN`: test/REQ revision+baseline; timestamp; actor/reviewer and separation; target URL without credentials; command or numbered UI/HTTP steps; expected vs actual; exit/status code; report/output locator; PASS/FAIL/NOT_RUN; residual limits. Keep build logs under ignored target if useful and summarize durable evidence here; never paste credentials or hidden reasoning.
