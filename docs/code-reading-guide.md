# ProcureFlow code-reading guide and slice study notes

Updated2026-10-09. Slice0 status: specification drafted, implementation pending. This guide distinguishes existing code from proposed changes; it is not a completed-slice changelog.

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
