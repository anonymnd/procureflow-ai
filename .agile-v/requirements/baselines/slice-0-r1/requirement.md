# Slice 0 - Restore a trustworthy development baseline

- Status: draft_persisted; no application implementation authorized.
- Project: ProcureFlow; created 2026-10-09; revision 1; approved/completed: no.
- Stakeholder directive: project owner, 2026-10-09; inspect actual code, preserve work/data, specify the smallest baseline repair and stop for approval.
- Baseline inspected: Git HEAD `44165770de45f249a4efc94ea923fbf22548b76a` plus the current working tree. Source fingerprints: [repository baseline](../traceability/repository-baseline.json).
- Related records: [decisions](../decisions/slice-0.md), [impact/architecture map](../traceability/slice-0.md), [draft test plan and exact steps](../tests/slice-0-test-plan.md), [review](../verification/slice-0-review.md), [observed evidence](../verification/slice-0-results.md).

## 1. Goal and success

A developer can use the Windows Maven wrapper to build, migrate a fresh isolated PostgreSQL database, start the actual application and exercise existing Supplier CRUD with correct errors. Existing work and business data/schema remain preserved. PostgreSQL housekeeping and the new isolated test database may write within the existing volume; no destructive volume operation is allowed. This establishes a development baseline, not production deployment readiness.

## 2. Actors and context

The actor is the developer/student studying the code and verifying it locally. There is no implemented authentication or organization context. Supplier requests currently have no authorization policy; Slice 0 does not invent one. The unfinished User class is retained for future discussion, not treated as a working account system.

## 3. Current state and smallest scope

### Actual repository findings

| Area | Classification | Evidence/current state | Slice 0 action |
|---|---|---|---|
| Main/test compilation and Supplier unit tests | Already working in inspected environment | System Maven 3.9.11, Temurin JDK 25.0.3; Java target 17; nine Mockito tests pass on 2026-10-09 | Re-run with restored wrapper; keep Java target and Boot version |
| Spring Boot build | Incomplete verification | `pom.xml` parent 4.1.1; JPA/Web MVC/Validation/Flyway/PostgreSQL/Lombok and test starters; no full package/startup run in this task | Package and start against isolated DB |
| Windows wrapper | Broken | `mvnw.cmd` is zero bytes; HEAD contains its original 189-line script | Restore this file from inspected HEAD only; preserve other work |
| Wrapper configuration | Available; execution uncertain | Wrapper 3.3.4, only-script, Maven 3.9.16; configured ZIP responds HTTP 200 to HEAD request | Keep pin; verify download/version and build. `mvnw` exists but Unix execution is not claimed |
| Docker Compose | Configuration working; database runtime uncertain | `docker-compose.yml` parses; postgres:17-alpine, service `db`, container `postgresql`, host port 5332 | Retain existing configuration/container/volume |
| Existing PostgreSQL | Needs verification | Container is exited; named volume `procureflow_db` exists and mounts actual PGDATA `/data/postgresql`; an additional anonymous mount exists | No volume cleanup or recreation; start existing container only after implementation/environment approval |
| Flyway/schema | Incomplete verification | Only `V1__create_supplier_table.sql`, with `suppliers(id BIGSERIAL PK, name VARCHAR(255) NOT NULL)`; Flyway enabled; Hibernate validates | Verify fresh migration and second startup; do not edit V1 or add schema for future concepts |
| Supplier CRUD | Implemented; HTTP/persistence uncertain | Five routes, DTOs, mapper, service and Spring Data repository; mocked service tests pass | Preserve successful contracts; prove actual MVC/persistence flow |
| User scaffold | Broken fresh-schema contract, inferred from mapping | `User` maps `users`; no users migration; fields id/username/email/password/organisationId; no consumers found | Proposed unmapped scaffold, preserving class/fields (DEC-S0-001). Failure has not been reproduced against a DB in this task |
| Not-found errors | Broken wiring, verified by imports | Service throws common `ResourceNotFoundException`; advice handles supplier-specific class; generic catch returns 500 | Advice handles the actual common exception; HTTP GET/PUT/DELETE missing ID return 404 |
| Validation | Incomplete | Request uses `@NotBlank`; schema limits name to 255; no `@Size` | Add maximum 255 and boundary tests; retain nonblank behavior |
| Malformed JSON / path IDs | Needs runtime verification; likely incorrect | Generic `Exception` advice can capture framework client-input exceptions as 500 | Explicitly require 400 for malformed/missing JSON and nonnumeric/out-of-range long ID |
| Other errors | Implemented; not exercised | ApiError fields timestamp/status/error/message; integrity 409; unexpected sanitized 500 | Preserve/test error shape and sanitized messages |
| Tests | Incomplete coverage | Nine Supplier unit tests plus one DB-dependent `contextLoads`; update test stubs a renamed result without asserting actual entity mutation | Strengthen update assertion; add targeted MVC/error and real PostgreSQL integration coverage |
| Uncommitted work | Must preserve | Modified wrapper, mapper, service and service tests; untracked User, planning docs and existing spec skill | Fingerprint before/after; no reset/stash/commit/delete by this task |

### Repository/planning inconsistencies

- README presents React, AI/FastAPI, security, Kubernetes and CI/CD as existing; no such application artifacts exist. Its purchase-request/approval workflow differs from the agreed RFQ workflow and deferred purchase requests.
- README POST example includes `email`, but Supplier accepts only `name` and returns `id/name`; it promises a Location header, which the controller does not set. Correct documentation rather than changing the existing contract.
- README presents resource-not-found handling as working; advice/service exception types differ.
- Living plan's nine unit tests now have current observed evidence, but this does not prove startup or schema correctness. Historical context reports from 2026-10-05 are not present-state evidence.
- Conceptual Organization relationships and ADRs are planned; `User.organisationId` is a primitive field, not a mapped Organization relation or foreign key.
- Roadmap correctly requires environment agreement. DEC-S0-002 supplies a concrete proposal; approval of this specification must include that proposal before DB-backed work starts.

### Included

Restore Windows wrapper; preserve configuration versions; stop scanning the unfinished User as a JPA entity; repair exception matching and targeted bad-input handling; align name validation; add meaningful baseline tests and safe DB test configuration; correct README; record actual traceability, tests and study notes.

### Excluded

Organization/RFQ/Quotation/Order models; account/authentication implementation; Supplier redesign; package-wide cleanup or exception-file deletion; frontend; migrations beyond V1; new libraries/infrastructure; speculative patterns, pagination or scalability features; automatic GitHub Issues before requirement approval.

## 4. API inputs, outputs and stable behavior

Input example: `{"name":"Atlas Supplies"}`. Response example: `{"id":1,"name":"Atlas Supplies"}` (generated ID varies). No email or organization fields. No Location-header change.

| Method/path | Controller -> service | Repository operations | Target behavior |
|---|---|---|---|
| GET `/api/suppliers` | `getAllSuppliers` -> `getAllSuppliers` | `findAll()` | 200 JSON array; empty `[]`; no ordering guarantee added |
| GET `/api/suppliers/{id}` | `getSupplierById` -> `getSupplierById` | `findById(id)` | 200 id/name; absent numeric ID 404 |
| POST `/api/suppliers` | `createSupplier` -> `createSupplier` | `save(entity)` | 201 id/name; invalid name 400 |
| PUT `/api/suppliers/{id}` | `updateSupplier` -> `updateSupplier` | `findById(id)`, `save(entity)` | 200 same ID/new name; absent ID 404; invalid name 400 |
| DELETE `/api/suppliers/{id}` | `deleteSupplier` -> `deleteSupplier` | `findById(id)`, `delete(entity)` | Existing ID 204 empty body; absent/repeated deleted ID 404 |

Requests use `SupplierDtoRequest`; all responses except DELETE use `SupplierDtoResponse`. Mapper `toEntity` is used on create; `toDto` on list/detail/create/update; update directly calls `Supplier.setName`. All operations remain unauthenticated for this local baseline; no security dependency is introduced.

## 5. Main verification flow and repeat behavior

Record worktree -> restore wrapper after implementation approval -> create uniquely named empty test database on the existing server -> configure guarded test profile -> run unit/MVC/integration tests -> package -> start jar on localhost -> create/read/update/delete one supplier -> verify errors -> restart jar on the same test database and verify migration history/persisted row -> record evidence and study map.

The test database is retained as evidence; no automatic drop/volume deletion. A new run uses a new name. Repeat app startup must not duplicate/reapply V1; repeat POST is an ordinary new Supplier creation (no new idempotency contract); repeat DELETE after deletion is 404. Failed steps stop verification and are recorded; they are not replaced by mock passes.

## 6. Database and persistence

- No migration, FK, cardinality, or table change is proposed.
- Existing Supplier table: `suppliers.id` primary key with generated sequence; `suppliers.name` nonnull, max 255. Duplicate names remain allowed; no uniqueness or normalization rule added.
- `Supplier -> other entity`: no relation; cardinality not applicable; owning side none; FK location none; reason Supplier currently stores only ID/name.
- `User -> Organization`: not an implemented relation; cardinality/owning side/FK none in current schema; `organisationId` alone enforces nothing. Preserve it as scaffold data, without mapping it or adding future tables.
- Flyway remains schema owner, `ddl-auto=validate`; do not use create/update/create-drop, Flyway clean/repair, or edit V1.
- Proposed isolated database: `procureflow_slice0_<timestamp>` on existing localhost:5332 PostgreSQL. Existing `ProcureFlow` DB, volume, and unrelated databases are not test targets. Existing DB contents/history are unknown; verify access read-only if needed before any future change.

## 7. Error and validation policy

Valid names: nonblank String with length <=255 under Jakarta `@Size` semantics (Java character sequence length). Do not silently trim, truncate or reject duplicates. Test ordinary ASCII bounds; no new Unicode normalization policy.

Invalid POST/PUT name (missing/null/empty/whitespace/256 ASCII characters): 400, ApiError `VALIDATION_ERROR`; service/repository must not write. Missing or syntactically malformed JSON on POST and PUT: 400 ApiError `BAD_REQUEST`. Nonnumeric or overflowing long ID on GET/PUT/DELETE detail routes: 400 ApiError `BAD_REQUEST`. Valid numeric ID with no row: 404, error `Resource Not Found`, service's supplier-not-found message. Negative numeric IDs remain numeric lookups and return 404 if absent; no new positive-ID constraint. Repeated DELETE of a removed ID returns404.

Integrity violation: 409 `DATA_CONFLICT`, sanitized existing message. Unexpected exception: 500 `INTERNAL_SERVER_ERROR`, sanitized existing message. Do not change all framework errors or install a new API error framework; only the specified cases need targeted handlers. Tests do not require one particular field-error ordering when multiple constraints fail.

## 8. Nonfunctional acceptance boundaries

Zero application/test writes to the pre-existing business databases' rows/schema; zero data loss or destructive volume operations; zero loss of pre-existing source/planning work; zero new runtime dependencies; five stable CRUD routes; all required tests pass with zero failures/errors/skips. Starting PostgreSQL and creating/writing the approved isolated database necessarily permits PostgreSQL internal housekeeping and test-DB writes within its existing volume. Local HTTP smoke calls use a 10-second client timeout to detect hangs, not a production latency SLO. Workload is one developer and the described small fixtures; no invented throughput/availability promise. No paid service or user-data upload is required. Logs/evidence must not expose account passwords, credentials or stack traces in HTTP responses.

## 9. Environment and constraints

Windows PowerShell in project root; Maven wrapper 3.3.4/3.9.16; Spring Boot 4.1.1; Java target 17, observed runtime Temurin 25.0.3. The proposed acceptance environment uses the observed JDK; do not claim Java 17 runtime testing unless actually run. Docker Desktop must be available; preserve postgres:17-alpine and port 5332. Existing PG credentials/database accessibility require verification; stop and record a blocker if initialization credentials no longer match, never recreate the data volume.

Test profile requires explicit `PROCUREFLOW_TEST_DB_URL` and credentials. A shared test-only context initializer must reject missing/non-local/non-5332/non-`procureflow_slice0_` targets before datasource/Flyway initialization. Include unit tests for the guard. Do not add H2/Testcontainers/ArchUnit/Spring Modulith or modify pom.xml merely because a skill recommends them.

## 10. Affected files/modules and implementation limits

Exact proposed file actions and dependencies are in [traceability](../traceability/slice-0.md). Main-source modifications are limited to `mvnw.cmd`, `User.java`, `GlobalExceptionHandler.java`, `SupplierDtoRequest.java`, and README. Existing mapper/service/entity/repository and migration remain intact unless new evidence reveals an approved requirement cannot be met without a documented revision. Test additions/configuration are test-only. No file deletion is proposed; duplicate unused exception classes are deferred.

## 11. Requirements and Definition of Done

Every requirement below inherits stakeholder source: project owner, baseline/traceability directive dated 2026-10-09; revision 1, no approved baseline; status **draft_persisted**. Acceptance tests and exact executable commands/numbered steps are in [test plan](../tests/slice-0-test-plan.md). That document is a draft normative companion, not observed results.

### REQ-S0-001 - Reproducible Windows build

- Requirement: restore the existing Windows wrapper and build/test/package the project with its pinned Maven distribution.
- Rationale: empty wrapper prevents the documented development entry point from running.
- Business rule: baseline restoration preserves the current Supplier behavior and project versions.
- Acceptance criteria: wrapper -v reports Maven 3.9.16 and actual JDK; existing unit tests pass; full guarded verification/package succeeds.
- Affected actors: developer.
- Related entities: Supplier; User scaffold.
- Related API behavior: all five routes remain available after startup.
- Database impact: none from build; DB tests use isolated target only.
- Authorization/security rule: no application permissions added; no dependency/version expansion.
- Related tests: TC-S0-001, TC-S0-002, TC-S0-010.
- Status: draft_persisted; decision DEC-S0-003.

### REQ-S0-002 - Fresh migration and context compatibility

- Requirement: Flyway V1 and Hibernate validation start successfully on the isolated fresh DB, and second startup preserves data/history.
- Rationale: mapped User has no migration, so a trustworthy fresh baseline is not established.
- Business rule: preserve unfinished User source without introducing future identity behavior.
- Acceptance criteria: User fields/class retained but not a managed entity; Supplier is managed; V1 succeeds once; schema validates; second startup preserves a created row; no users/organization schema added by application migration.
- Affected actors: developer.
- Related entities: Supplier, unmapped User scaffold.
- Related API behavior: existing Supplier API starts without a users table.
- Database impact: isolated database creation for verification only; no schema/migration change.
- Authorization/security rule: target guard and explicit local credentials; Flyway clean disabled.
- Related tests: TC-S0-003, TC-S0-010, TC-S0-011.
- Status: draft_persisted; decisions DEC-S0-001, DEC-S0-002.

### REQ-S0-003 - Preserve successful Supplier CRUD

- Requirement: create/list/read/update/delete use the existing paths, DTO fields and status codes with actual PostgreSQL persistence.
- Rationale: mocked unit passes alone do not establish the complete application flow.
- Business rule: name-only suppliers; generated stable ID; duplicate names allowed; no Location-header or ordering contract added.
- Acceptance criteria: POST201; GET list/detail200; PUT200 same ID/persisted changed name; DELETE204 empty; deleted detail404; empty list[] on fresh database.
- Affected actors: local API caller/developer.
- Related entities: Supplier.
- Related API behavior: section 4 contract.
- Database impact: writes restricted to isolated fixture database; existing suppliers table unchanged.
- Authorization/security rule: current API has no authorization; explicitly documented local-only acceptance boundary.
- Related tests: TC-S0-002, TC-S0-004, TC-S0-005, TC-S0-010.
- Status: draft_persisted; decision DEC-S0-004.

### REQ-S0-004 - Correct missing-resource and client-input errors

- Requirement: return the specified 404/400 errors through ApiError; preserve sanitized 409/500 behavior.
- Rationale: wrong exception import and broad fallback obscure actionable client errors.
- Business rule: absent supplier is not a server failure; invalid client input must not mutate data.
- Acceptance criteria: missing GET/PUT/DELETE404; nonnumeric/overflow IDs and malformed/missing JSON400; integrity409; unexpected500 with no internal details; no writes on invalid/missing-resource requests.
- Affected actors: local API caller/developer.
- Related entities: Supplier.
- Related API behavior: section 7 error contract.
- Database impact: none; rejected operations leave existing rows unchanged.
- Authorization/security rule: responses sanitize internal failures; do not expose SQL/credentials/stack traces.
- Related tests: TC-S0-006, TC-S0-008, TC-S0-009.
- Status: draft_persisted; decision DEC-S0-004.

### REQ-S0-005 - Align name validation with schema

- Requirement: POST/PUT reject blank/missing/null names and names over 255 Java character-sequence units before service invocation.
- Rationale: requests otherwise reach a narrower database column and become persistence errors.
- Business rule: do not trim/truncate, add uniqueness, or expand Supplier fields.
- Acceptance criteria: nonblank 1/255 ASCII characters succeed; missing/null/empty/whitespace/256 fail400; existing row/count unchanged; MVC tests prove service not invoked for invalid names.
- Affected actors: local API caller/developer.
- Related entities: Supplier.
- Related API behavior: validation error ApiError for POST/PUT.
- Database impact: unchanged VARCHAR(255); no migration.
- Authorization/security rule: validate at API boundary; parameterized Spring Data operations remain.
- Related tests: TC-S0-007, TC-S0-004.
- Status: draft_persisted; decision DEC-S0-004.

### REQ-S0-006 - Safe verification and preservation

- Requirement: guard database-backed tests against existing business databases and preserve all pre-existing work/data.
- Rationale: the context test currently inherits the normal local business datasource.
- Business rule: no business data reset, migration repair or unrelated source overwrite.
- Acceptance criteria: absent/unsafe test targets fail before DB initialization; allowed generated local target works; original files' contents remain except scoped approved edits; no volume drop/DB drop; evidence states all commands and target.
- Affected actors: developer/project owner.
- Related entities: Supplier fixtures; preserved User scaffold.
- Related API behavior: testing only, production API unchanged.
- Database impact: one new named isolated DB per verification run; retained, not dropped.
- Authorization/security rule: reject unsafe target before datasource/Flyway; credentials supplied explicitly and not recorded in results.
- Related tests: TC-S0-003, TC-S0-011, TC-S0-012.
- Status: draft_persisted; decision DEC-S0-002.

### REQ-S0-007 - Accurate documentation and connected study record

- Requirement: document actual setup and implemented behavior, and maintain complete slice lineage/read order.
- Rationale: README overstates features and the owner needs to study connected implementation.
- Business rule: proposed/implemented/verified are separate; no fabricated approvals/issues/evidence.
- Acceptance criteria: README commands work on agreed environment and describe name-only API; exact file map records actions/reasons/concepts/dependencies; endpoint flow/DTO/repo/auth/test/DB relations and actual verification link to requirements/decisions/issues; code-reading guide has before/after/read order/tests; completed status only after all criteria pass.
- Affected actors: student/developer/reviewer.
- Related entities: Supplier; scaffold User; no future implementation.
- Related API behavior: section 4 contract, accurately documented.
- Database impact: documents existing table and absence of FKs; no schema change.
- Authorization/security rule: instructions scoped to local development, no false security/production-readiness claim.
- Related tests: TC-S0-010, TC-S0-012, TC-S0-013.
- Status: draft_persisted; decisions DEC-S0-002, DEC-S0-004.

### Definition of Done

All seven requirements are explicitly approved/baselined, linked to actual issues, implemented only after separate approval, and proven by the linked test plan. Approval binds this requirement revision together with the current decision, test-plan and traceability companions; freeze copies and hashes of all four before issue creation. Normative acceptance changes require a new reviewed bundle, not a silent companion edit. Wrapper/package, fresh migration/context, real HTTP CRUD and negative cases, restart persistence and safety guard all pass without skipped required cases. Record commands, actual outputs/exit codes and scope of independent review. Preserve prior work/data, correct README and finish study/traceability records. No planned future entity/library/infrastructure appears. This document is not done while DB/HTTP checks are unrun.

## 12. Decisions and user discretion

Confirmed: preservation, no application work before specification approval and explicit implementation approval, no future domain/infrastructure now, strong traceability and learning notes; skill/workflow setup approval is separate.

Proposals requiring review with this specification: DEC-S0-001 unmapped User scaffold rather than premature migration; DEC-S0-002 isolated DB on the existing server plus test-only guard; DEC-S0-003 restore original wrapper and retain existing versions; DEC-S0-004 minimal targeted exception/validation repairs preserving API shape.

The user wants options and reasons for technical choices. Material schema, API, authorization, tool/dependency or business-behavior changes outside this specification must be proposed for review. If access to the existing DB is unavailable, record the blocker and request a new environment decision; do not reset it or install substitute infrastructure. Numeric production performance/recovery targets are outside this local baseline and require an agreed pilot workload later.

## 13. Implementation log and verification evidence

No application implementation performed. Skill/workflow setup completed under APR-SETUP-001. Baseline source inspection, Compose parsing, wrapper-distribution HEAD checks and existing unit tests are recorded in [results](../verification/slice-0-results.md). Generated `target/` test outputs may have changed; they are ignored build artifacts, not source changes. GitHub Issues and approved baseline do not exist yet. The detailed [review](../verification/slice-0-review.md) identifies remaining proposed decisions and verification gaps.
