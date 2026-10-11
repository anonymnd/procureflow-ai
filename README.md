# ProcureFlow

ProcureFlow currently contains a Spring Boot backend with name-only Supplier CRUD, PostgreSQL and Flyway. Slice 0 restores this development baseline under [BL-S0-R1](.agile-v/requirements/baselines/slice-0-r1/manifest.json) and GATE-S0-IMPLEMENT. The owner accepted Slice 0 on 2026-10-11 after verification and review. Changes remain local and unpublished.

The agreed future procurement direction is RFQ -> quotation -> order; see [confirmed decisions](docs/planning/decisions.md). React, AI services, authentication, organization permissions and that workflow are future work. User is an unmapped scaffold.

## Environment

Windows PowerShell, Docker Desktop and an installed JDK are needed. Java compilation target is 17; observed verification used Temurin 25.0.3, not a Java 17 runtime. Spring Boot is 4.1.1; the wrapper pins Maven 3.9.16. Compose defines PostgreSQL 17 service `db`, container `postgresql`, host port 5332. Preserve existing databases and volumes.

Select your installed JDK and check the wrapper:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot'
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path
.\mvnw.cmd -v
```

The Unix wrapper is present; execution evidence for this slice is Windows only.

## Safe verification

Run from the repository root. Database-free tests:

```powershell
.\mvnw.cmd '-Dtest=SupplierServiceTest,SupplierControllerTest,Slice0DatabaseGuardTest' test
if ($LASTEXITCODE -ne 0) { throw 'Database-free tests failed' }
```

Full tests require a **new empty isolated database**. This workspace already has the `postgresql` container: start it without recreating volumes. If absent, inspect Compose and your environment before first-time provisioning. Use the existing server's credentials; changing Compose variables does not reset an initialized volume's credentials.

```powershell
docker start postgresql
if ($LASTEXITCODE -ne 0) { throw 'Existing PostgreSQL container could not start' }
$slice0Db = 'procureflow_slice0_' + (Get-Date -Format 'yyyyMMddHHmmss') + '_' + [guid]::NewGuid().ToString('N').Substring(0,8)
$env:PROCUREFLOW_TEST_DB_USERNAME = 'ProcureFlow'
$env:PROCUREFLOW_TEST_DB_PASSWORD = [System.Net.NetworkCredential]::new('', (Read-Host 'Existing local PostgreSQL password' -AsSecureString)).Password
docker exec postgresql createdb -U $env:PROCUREFLOW_TEST_DB_USERNAME $slice0Db
if ($LASTEXITCODE -ne 0) { throw 'Isolated database creation failed; do not recreate volumes' }
$env:PROCUREFLOW_TEST_DB_URL = "jdbc:postgresql://localhost:5332/$slice0Db"
.\mvnw.cmd verify
if ($LASTEXITCODE -ne 0) { throw 'Verification failed' }
```

Both database-backed test classes activate `slice0-test` and a test-only initializer. It validates the resolved datasource/Flyway targets before database beans initialize. Missing configuration, business DB names, remote hosts, wrong ports and unsafe schema generation fail. Fixtures roll back, though sequences can advance. Keep the isolated database as evidence. Do not drop databases/volumes, run Flyway clean/repair or enable Hibernate create/update to make tests pass. Full `test`/`verify` without explicit test configuration intentionally fails.

## Run the packaged backend locally

After verification, use the same isolated target. The jar does not contain the test-only guard: validate the URL and use these explicit local settings. Avoid inherited Spring/Hibernate/JVM overrides; the [approved test plan](.agile-v/tests/slice-0-test-plan.md) explains the verification protocol.

```powershell
if ($env:PROCUREFLOW_TEST_DB_URL -notmatch '^jdbc:postgresql://localhost:5332/procureflow_slice0_[0-9]{14}_[a-z0-9]{8}$') { throw 'Unsafe verification target' }
java -jar target/app-0.0.1-SNAPSHOT.jar '--spring.config.location=classpath:/application.properties' '--spring.profiles.active=slice0-smoke' '--server.address=127.0.0.1' '--server.port=18080' "--spring.datasource.url=$env:PROCUREFLOW_TEST_DB_URL" '--spring.datasource.username=${PROCUREFLOW_TEST_DB_USERNAME}' '--spring.datasource.password=${PROCUREFLOW_TEST_DB_PASSWORD}' "--spring.flyway.url=$env:PROCUREFLOW_TEST_DB_URL" '--spring.flyway.user=${PROCUREFLOW_TEST_DB_USERNAME}' '--spring.flyway.password=${PROCUREFLOW_TEST_DB_PASSWORD}' '--spring.jpa.hibernate.ddl-auto=validate' '--spring.flyway.enabled=true' '--spring.flyway.clean-disabled=true'
```

In another PowerShell window:

```powershell
Invoke-RestMethod http://127.0.0.1:18080/api/suppliers
Invoke-RestMethod -Method Post -Uri http://127.0.0.1:18080/api/suppliers -ContentType 'application/json' -Body '{"name":"Atlas Supplies"}'
```

Stop with Ctrl+C. Restarting on the same isolated database preserves rows without reapplying V1. Verification used port 18080 because 8080 was occupied. Normal `application.properties` still targets the existing local development DB: inspect it before ordinary runs. The API has no authentication/authorization; this documented run binds to localhost.

## Supplier API

| Method/path | Result |
|---|---|
| POST `/api/suppliers` | 201, generated `id` and supplied `name` |
| GET `/api/suppliers` | 200, array (empty `[]` when no rows) |
| GET `/api/suppliers/{id}` | 200, `id`/`name` |
| PUT `/api/suppliers/{id}` | 200, same `id` and updated `name` |
| DELETE `/api/suppliers/{id}` | 204, empty body |

Request: `{"name":"Atlas Supplies"}`. Response: `{"id":1,"name":"Atlas Supplies"}`; IDs vary. No email field or Location-header guarantee. Names are nonblank, at most 255 Java character-sequence units, preserved as supplied; duplicates are allowed.

Errors contain `timestamp`, `status`, `error`, `message`: invalid names -> 400 `VALIDATION_ERROR`; malformed/missing JSON and nonnumeric/overflowing IDs -> 400 `BAD_REQUEST`; absent numeric IDs (including negative/repeatedly deleted IDs) -> 404 `Resource Not Found`; integrity failures -> sanitized 409 `DATA_CONFLICT`; unexpected failures -> sanitized 500 `INTERNAL_SERVER_ERROR`.

## Read the code

`src/main/java/com/project/app/supplier/` contains controller -> service -> repository -> PostgreSQL, with explicit DTOs and mapper. Unchanged Flyway `V1__create_supplier_table.sql` owns `suppliers(id BIGSERIAL PRIMARY KEY, name VARCHAR(255) NOT NULL)`. Supplier has no entity relations/FKs. User has no mapped table or Organization relationship.

Start with the [code-reading guide](docs/code-reading-guide.md), [traceability](.agile-v/traceability/slice-0.md) and [observed evidence](.agile-v/verification/slice-0-results.md). `.agile-v` is authoritative; Jira/GitHub mirror planning. Slice 0 final acceptance is recorded separately; it does not authorize commit, push, merge or deployment.
