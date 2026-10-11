# ProcureFlow code-reading guide and slice study notes

Updated2026-10-10. Slice0 status: requirement revision 1 approved/baselined; explicit FCC implementation authorization pending. This guide distinguishes existing code from proposed changes; it is not a completed-slice changelog.

## What exists before Slice 0

One Spring Boot backend, a name-only Supplier CRUD feature, PostgreSQL Compose configuration, FlywayV1 and an unfinished User entity scaffold. Nine service unit tests pass; database startup and HTTP behavior are not currently verified. There is no implemented React, identity/organization authorization, RFQ, quotation, order or AI feature.

Current Supplier flow:

```text
HTTP -> SupplierController -> SupplierService -> SupplierRepo -> suppliers
              |                     |
         Request DTO              SupplierMapper <-> Supplier / Response DTO
              |
         Jakarta validation

Exception -> GlobalExceptionHandler -> ApiError
```

Service currently throws the common not-found exception, while advice imports another class. That connection explains why a service test can pass without proving HTTP404.

## Slice 0 proposed changes and reasons

Restore the Windows wrapper; preserve User as an unmapped scaffold rather than design identity prematurely; align exception/input handling; cap request names at255; strengthen tests against real PostgreSQL with a safe target; correct README. No application change has happened yet.

Read the authoritative [specification](../.agile-v/requirements/slice-0.md), [decision options](../.agile-v/decisions/slice-0.md), [exact file/action map](../.agile-v/traceability/slice-0.md), [test proposal](../.agile-v/tests/slice-0-test-plan.md), and [observed results](../.agile-v/verification/slice-0-results.md).

## Files to read first and recommended order

1. [Slice0 requirements](../.agile-v/requirements/slice-0.md): intended behavior and scope before reading implementation.
2. [pom.xml](../pom.xml), [application.properties](../src/main/resources/application.properties), [docker-compose.yml](../docker-compose.yml), [V1](../src/main/resources/db/migration/V1__create_supplier_table.sql): dependencies, actual datasource, schema ownership and persisted shape.
3. [SupplierController](../src/main/java/com/project/app/supplier/controller/SupplierController.java): five HTTP entry points and status codes.
4. [SupplierDtoRequest](../src/main/java/com/project/app/supplier/DTO/SupplierDtoRequest.java), [SupplierDtoResponse](../src/main/java/com/project/app/supplier/DTO/SupplierDtoResponse.java): request validation and API boundary.
5. [SupplierService](../src/main/java/com/project/app/supplier/service/SupplierService.java): orchestration, find/save/delete and missing-resource decisions.
6. [SupplierMapper](../src/main/java/com/project/app/supplier/mapper/SupplierMapper.java), [Supplier](../src/main/java/com/project/app/supplier/model/Supplier.java), [SupplierRepo](../src/main/java/com/project/app/supplier/repository/SupplierRepo.java): DTO conversion, entity and inherited database operations.
7. [Common exception](../src/main/java/com/project/app/common/exception/ResourceNotFoundException.java), [GlobalExceptionHandler](../src/main/java/com/project/app/supplier/controller/GlobalExceptionHandler.java), [ApiError](../src/main/java/com/project/app/supplier/controller/ApiError.java): trace the mismatched import and response shape.
8. [User](../src/main/java/com/project/app/userAccount/model/User.java): inspect the unused scaffold and distinguish a primitive organisationId from a real association/FK.
9. [SupplierServiceTest](../src/test/java/com/project/app/supplier/service/SupplierServiceTest.java), [context test](../src/test/java/com/project/app/AppApplicationTests.java), then [proposed verification steps](../.agile-v/tests/slice-0-test-plan.md): learn what mocks prove and what requires the real context/database.
10. [Decision records](../.agile-v/decisions/slice-0.md) and [traceability map](../.agile-v/traceability/slice-0.md): connect the reasons and dependencies back to requirements.

## Important business/API rules

Current successful API contract: POST201, list/detail200, PUT200, DELETE204; Supplier has only generated ID and name. Proposed Slice0 rules: missing supplier404; invalid name/malformed JSON/invalid long conversion400; integrity409 and unexpected500 remain sanitized. No name uniqueness/trimming, authentication, pagination or new fields. Exact endpoint-to-controller/service/repository/DTO/test map is in [traceability](../.agile-v/traceability/slice-0.md).

## Classes, modules and database relationships

Controller depends on Service; Service on Repo/Mapper and the common exception; Mapper on Supplier and DTOs; Repo extends JpaRepository<Supplier,Long>. User has no consumers found and no connection to Supplier. Proposed common exception alignment is the only application cross-module repair; no new module is introduced.

- `Supplier -> other entity`: none; cardinality/owning side/FK not applicable; reason Supplier stores only ID/name.
- `User -> Organization`: no implemented relationship; no enforced cardinality, owning side or FK; reason Organization/table/migration do not exist and organisationId is just a field.
- Slice0 added/changed relationships: none. V1 and suppliers.id PK/name VARCHAR255 remain unchanged.

Authorization currently protects none of these endpoints. Input validation is not authorization. Identity and organizational permissions belong to a later approved slice; local baseline success does not mean secure production deployment.

## What changed in this setup task

Ten approved skills were installed and existing spec retained. New AGENTS.md and `.agile-v/` records establish requirements, scoped approvals, technical decisions, file/dependency/API/database traceability, test proposals and observed evidence; this guide provides reading order. Existing source/planning documents and data were preserved. Exact created paths/reasons and skill-file inventory are in [change map](../.agile-v/traceability/slice-0.md) and [installation manifest](../.agile-v/traceability/skill-installation.json). Files modified/deleted from existing application: none. No GitHub Issues created.

## Tests proving behavior so far

Only the existing nine Mockito SupplierService tests have current passing evidence. They prove the mocked service cases, not database/HTTP behavior. The original update test can return a renamed mocked DTO without proving mutation; Slice0 proposes strengthening that assertion plus a real persisted reread. Wrapper/context/HTTP/restart/guard acceptance remains NOT_RUN; see [results](../.agile-v/verification/slice-0-results.md).

## Completed-slice study record contract

After each completed slice, append a section here with approved requirement revision/baseline and issue links; what existed before; actual change and reason; created/modified/deleted files; business rules; class/entity and cross-module relations; each DB relation's cardinality/owning side/FK/reason; endpoint -> controller -> service -> repository/query with DTOs/mappers and authorization; recommended reading order; actual test method names and verification results. Do not replace proposal records with unsupported claims that a feature works.

## Slice 0 approval and issue reading map

2026-10-10: GATE-S0-R1 approves requirement revision 1 and its decision/test/traceability companions. Read the [baseline manifest](../.agile-v/requirements/baselines/slice-0-r1/manifest.json) to identify the byte-for-byte approved inputs, then the live [requirement](../.agile-v/requirements/slice-0.md), [decisions](../.agile-v/decisions/slice-0.md), [test plan](../.agile-v/tests/slice-0-test-plan.md) and [change/architecture map](../.agile-v/traceability/slice-0.md).

Read tasks in this order: [#1 reproducible build and safe DB baseline](https://github.com/anonymnd/procureflow-ai/issues/1), [#2 Supplier API errors/validation](https://github.com/anonymnd/procureflow-ai/issues/2), [#3 verification and documentation](https://github.com/anonymnd/procureflow-ai/issues/3). Their scope/files/criteria/tests map to the approved requirements; #3 depends on both earlier tasks. Each issue is a planning brief, NOT AUTHORIZED for implementation until GATE-S0-IMPLEMENT.

What actually changed in this approval task: four frozen companion copies, a manifest and a local line-ending preservation file were added; existing approval/state/index/requirement/decision/test-binding/traceability/verification records and this reading guide were updated. Why: bind reviewed acceptance to real issues without changing requirements. Exact paths/reasons/dependencies are in the change map. Application files created/modified/deleted, DB/FK/cardinality/API/auth changes and new implemented tests: none. Before/after application behavior is identical; previous nine unit-test evidence remains historical, and Slice 0 startup/HTTP/database acceptance remains unrun. This is not a completed-slice study record.
## Slice 0 actual implementation/study record — 2026-10-10

Current state supersedes earlier planning sections: revision 1 / BL-S0-R1 requirements approved by GATE-S0-R1, Codex coding authorized by GATE-S0-IMPLEMENT. Implementation and builder verification are complete; fresh-context technical review PASS (I1) and final owner acceptance were recorded on 2026-10-11. A later scoped publication instruction authorizes the accepted Slice 0 commit/push to origin/main; no deployment is authorized. Issues [#1](https://github.com/anonymnd/procureflow-ai/issues/1), [#2](https://github.com/anonymnd/procureflow-ai/issues/2), [#3](https://github.com/anonymnd/procureflow-ai/issues/3) correspond to SCRUM-6/7/8 under SCRUM-5.

Before: empty Windows wrapper, mapped unused User without users migration, not-found advice wired to the wrong exception, no maximum name validation, and only mocked service evidence. After: wrapper works; User fields remain as ordinary unmapped scaffold; common service exception reaches 404 advice; malformed body/ID conversion returns400; blank/>255 names fail before service; real PostgreSQL CRUD and restart behavior are observed. Existing service/entity/mapper/controller/repository/V1/build versions were preserved. No future features were added.

Read the approved baseline first, then README setup, request DTO and controller, service, mapper/response DTO, repository/entity/V1, common exception and advice, then tests. The earlier linked file reading list is still useful; the exception mismatch and User mapping it describes are now repaired. Exact created/modified/deleted paths, reasons and REQ -> DEC -> GitHub/Jira -> DB/API -> test -> result lineage are in the [actual change map](../.agile-v/traceability/slice-0.md#actual-implementation-map--2026-10-10). No files were deleted.

Trace a POST yourself: validated SupplierDtoRequest -> SupplierController -> SupplierService -> SupplierMapper.toEntity -> SupplierRepo.save -> suppliers INSERT -> SupplierMapper.toDto -> SupplierDtoResponse ->201. List uses findAll; detail uses findById; update loads the existing entity, calls setName and saves; delete loads then deletes. Missing lookups throw the common exception, which advice maps to404. Request parsing/type failures go to targeted400 handlers; validation400; sanitized integrity409/unexpected500. Responses contain id/name only; no Location promise. Input is nonblank/max255; duplicates and spaces are retained.

Supplier -> any other entity: no relationship; cardinality/owning side/FK none; Supplier stores only ID/name. User -> Organization: no relationship/table/FK; organisationId alone is an ordinary field. No new module/DB relation was added. Cross-module connection is SupplierService -> common exception -> Supplier advice. All five routes remain unauthenticated; input validation is not authorization. Future identity/organization design is not implemented by this repair.

Study test layers in order: `SupplierServiceTest.shouldUpdateSupplier` checks the real saved entity argument; `SupplierControllerTest` verifies binding/validation and no service calls on invalid input, plus sanitized errors; `Slice0DatabaseGuardTest` validates the effective configuration before beans; `AppApplicationTests.contextLoads` asserts managed entity/schema/history; `SupplierApiIntegrationTest` proves actual persistence, boundaries and unchanged rejected fixtures. JDBC assertions explicitly flush/clear JPA first because MockMvc joins the test transaction; each fixture rolls back. Real jar HTTP requests use separate transactions and therefore keep the restart marker in the isolated DB.

Observed final run: Maven3.9.16, Temurin25.0.3, PostgreSQL17.11; 72 tests (9 service,16 MVC,36 guard,10 integration,1 context), zero failures/errors/skips, successful package. An earlier 67-test suite passed before five Hikari override checks were added. The 110 real HTTP/startup/restart checks passed on localhost:18080. Missing explicit DB configuration fails before Hikari/Flyway. V1 applied once, only suppliers/history tables, marker31 survives restart. See [method bindings](../.agile-v/tests/slice-0-test-plan.md#actual-test-bindings--2026-10-10-bl-s0-r1) and [results](../.agile-v/verification/slice-0-results.md). Builder evidence is I0; review/final approval are separate.
