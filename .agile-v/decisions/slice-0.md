# Slice 0 decision proposals

Date: 2026-10-09. Status: proposed; approval pending with requirement revision 1. Existing planning ADRs remain unchanged. No decision here authorizes application edits. Current uncertainty is documented, not resolved through invented runtime evidence.

## DEC-S0-001 - Preserve User as an unmapped scaffold

- Requirements: REQ-S0-002, REQ-S0-006. Issue: not created. Files: `src/main/java/com/project/app/userAccount/model/User.java`; context/integration tests.
- Problem/evidence: `@Entity/@Table(users)` is scanned, but Flyway creates only suppliers. No User consumer is present. Fresh Hibernate validation cannot establish a users mapping against V1 alone.
- Simplest proposal: remove JPA mapping annotations/imports only; keep class, fields, Lombok and constructors. Document as unfinished, not persisted. Keep schema validation enabled.
- Alternatives/trade-offs: add users migration (premature schema/password/org decisions); delete User (loses work); exclude the package with custom entity-scan configuration (hides the mismatch and adds configuration); disable validation (conceals drift). Unmapping preserves source and avoids those changes, but intentionally leaves User unusable for persistence until Slice 1.
- Overengineering now: building identity/Organization or a complete domain/persistence separation to repair a scaffold.
- Source/lesson: [Hibernate schema validation](https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#schema-generation) and [Flyway versioned migrations](https://documentation.red-gate.com/flyway/flyway-concepts/migrations/versioned-migrations). Use explicit schema ownership and detectable drift; no company-specific pattern is needed for this repair.
- ProcureFlow adaptation: preserve the existing Flyway + validate arrangement and defer identity modeling to ADR-001/Slice 1. No database relation is added.
- Failure/verification: test managed metamodel excludes User, fresh context succeeds with only V1, and existing fields/source remain. TC-S0-003/011/012; runtime results pending.
- Revisit: approved Slice 1 identity model and migration. Approval reference: pending.

## DEC-S0-002 - Isolated database, existing PostgreSQL server

- Requirements: REQ-S0-002/003/006/007. Issue: not created. Files: test profile, test-only guard and tests, README; Compose/application configuration unchanged.
- Problem/evidence: existing context test reads normal business datasource; container/volume exist but stored data/credentials are unknown.
- Simplest proposal: reuse the existing server/container after approval, create a new `procureflow_slice0_<timestamp>` database, pass explicit test credentials, and reject unsafe targets before datasource/Flyway starts. Keep the database afterward as evidence. Preservation means no application/test writes to existing business rows/schema, no loss or destructive volume operations; PostgreSQL housekeeping and the isolated DB necessarily write within the same volume. This precise boundary is part of the pending user approval.
- Alternatives/trade-offs: test existing ProcureFlow DB (risks business data); add second container/Testcontainers (stronger isolation but new setup/dependency, not necessary yet); H2 (does not prove PostgreSQL migration behavior). Same server/new DB keeps stack small but requires Docker availability, create-database permission and careful target/credential checks; it is not infrastructure isolation.
- Overengineering now: new orchestration, test platform, snapshots of all business data, or cloud database merely to test one table.
- Source/lesson: [Spring Boot testing documentation](https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html) explains context tests and transactional test behavior. [PostgreSQL CREATE DATABASE](https://www.postgresql.org/docs/17/sql-createdatabase.html) supports an explicit separate DB. There is no invented big-tech attribution.
- ProcureFlow adaptation: real PostgreSQL, existing test starters, rollback for MockMvc integration fixtures; actual HTTP smoke writes only the dedicated DB. Never assume a transaction in the test thread rolls back an HTTP server thread.
- Failure/verification: absent/unsafe URL fails before initialization; valid local target works; real-jar launch preflight rejects overriding environment/JVM configuration and explicitly pins datasource/Flyway/configuration; fresh migration/restart and preexisting source fingerprints checked. TC-S0-003/010/011/012. Access failure is a blocker, not a reason to recreate a volume.
- Revisit: multi-developer/CI needs or unreliable shared server justify separately proposed disposable infrastructure. Approval reference: pending.

## DEC-S0-003 - Restore the existing Windows wrapper

- Requirements: REQ-S0-001. Issue: not created. File: `mvnw.cmd`.
- Problem/evidence: zero-byte working-tree file; original script in HEAD. Existing wrapper distribution URL responds HTTP200; system Maven can run nine tests.
- Simplest proposal: restore this file's inspected HEAD contents, retaining wrapper 3.3.4/Maven3.9.16 and Boot4.1.1/Java17 target.
- Alternatives/trade-offs: require every developer's system Maven (version drift); regenerate wrapper/change versions (unnecessary churn). Restore is small/reversible, but first wrapper use may download Maven and still needs actual execution evidence.
- Overengineering now: build-tool migration, CI pipeline or toolchain framework.
- Source/lesson: [Apache Maven Wrapper](https://maven.apache.org/wrapper/) provides a project-pinned entry point. Adaptation: restore existing mechanism, not new build infrastructure.
- Failure/verification: wrapper -v, unit tests, package exit0. TC-S0-001/002/010. Revisit only if download/tool compatibility actually fails. Approval reference: pending.

## DEC-S0-004 - Targeted Supplier boundary repairs

- Requirements: REQ-S0-003/004/005/007. Issue: not created. Files: existing advice, request DTO, tests and README.
- Problem/evidence: exception import mismatch; NotBlank without column-length bound; broad generic handler; README wrong fields/features.
- Simplest proposal: handle the common exception already thrown, add Size(max255), add targeted bad JSON/ID handlers preserving ApiError, and correct documentation.
- Alternatives/trade-offs: move all advice/rename packages/delete duplicate exceptions (extra cleanup); replace ApiError with ProblemDetail (contract change); add validation framework (existing Jakarta dependency suffices). Narrow repair has less churn but leaves known unused exception scaffolds for later explicit cleanup.
- Overengineering now: hexagonal rewrite, global domain framework, State/Strategy/event patterns for simple CRUD.
- Source/lesson: [Spring MVC validation](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-validation.html) and [exception handling](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-exceptionhandler.html). Adaptation: preserve Controller/Service/Repository/DTO boundaries and prove them at HTTP level.
- Failure/verification: correct400/404/409/500, no writes on invalid inputs, real persisted update. TC-S0-004 through009/013. No DB/FK change. Approval reference: pending.

## Production inspiration for later slices (not Slice 0 implementation)

Amazon and Stripe document idempotency for ambiguous/repeated requests: [Amazon](https://aws.amazon.com/builders-library/making-retries-safe-with-idempotent-APIs/), [Stripe](https://stripe.com/blog/idempotency). The applicable ProcureFlow problem is repeated quotation acceptance in Slice 5; the candidate adaptation is a PostgreSQL transaction plus uniqueness/concurrency protection and explicit repeat semantics. Do not infer Redis, message brokers or distributed exactly-once guarantees from that lesson. Record a new slice decision when the approved requirement specifies the behavior.

Lifecycle guards initially need explicit states/methods, not a State class per status. Strategy/Factory require real algorithm/creation variation. Events require independent reactions. Outbox requires reliable DB-to-external-message publication. CQRS infrastructure requires measured need beyond ordinary read DTOs. No such new mechanism is justified by current Supplier baseline evidence.
