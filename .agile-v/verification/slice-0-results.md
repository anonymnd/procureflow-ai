# Slice 0 observed evidence

Date: 2026-10-09. Author: Codex. All checks below are baseline/setup self-checks (I0), not completed Slice 0 acceptance. Application implementation/DB operations have not started.

## VER-S0-BASE-001 - Existing database-free unit suite

- Command: `mvn.cmd -o '-Dtest=SupplierServiceTest' test` using system Maven3.9.11 and Temurin25.0.3, Java target17.
- Observed final output, exit0:

```text
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] Total time:  9.522 s
[INFO] Finished at: 2026-10-09T14:06:02+01:00
```

- Report: `target/surefire-reports/TEST-com.project.app.supplier.service.SupplierServiceTest.xml` (ignored generated artifact).
- Meaning: existing SupplierService Mockito behavior passes. No current PostgreSQL/context/HTTP proof. Compiler reported existing classes up to date in this rerun; earlier inspected run compiled sources. Does not prove a fresh wrapper/package build.
- Warnings: Maven's internal Unsafe usage and Mockito dynamic agent loading on JDK25; no failures. Do not add build plugins merely to suppress these without an actual requirement.

## VER-S0-BASE-002 - Compose and preserved volume

- `docker compose config --quiet`: exit0, no output.
- `docker inspect postgresql --format '{{json .State.Status}} {{json .Mounts}}'`: exit0, status `exited`; named `procureflow_db` at `/data/postgresql`; additional anonymous volume at `/var/lib/postgresql/data`.
- `docker volume inspect procureflow_db --format '{{.Name}}'`: exit0, `procureflow_db`.
- Meaning: existing configuration/volume established; not proof of PostgreSQL credentials, schema, contents or ability to start. No start/create/query/drop/remove command executed.

## VER-S0-BASE-003 - Wrapper distribution availability

- HTTP HEAD to configured `https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.16/apache-maven-3.9.16-bin.zip`: HTTP200.
- HTTP HEAD to system-Maven3.9.11 distribution: HTTP200 (comparison only, no change proposed).
- Current wrapper files: `mvnw`11790 bytes; `mvnw.cmd`0 bytes. No script restoration/download/execution performed.
- Meaning: configured distribution is available; wrapper functionality remains broken until approved repair and execution.

## VER-S0-SETUP-001 - Approved skill/workflow setup

- Ten selected skill directories installed at the revisions in [SKILLS.md](../SKILLS.md); existing spec directory retained. No build-agent/compliance/infrastructure added.
- Installation authority: APR-SETUP-001. Exact added skill files/hash inventory: [manifest](../traceability/skill-installation.json).
- Original source/build/planning fingerprints: [snapshot](../traceability/repository-baseline.json). Final comparison and document-link/ID checks are appended after execution; setup correctness is distinct from feature acceptance.
- Preservation/link/inventory check: exit0; observed `PASS: 30 source/build/planning fingerprints; 10 prior dirty/planning fingerprints; Markdown local links; 10 installed skills and file hashes.` All original paths still match; original dirty source/wrapper and five planning files match the hashes captured before drafting. Repeat only if further changes affect these records.
- Final document check after review edits: exit0; observed `PASS: all local document links; 9 PowerShell blocks parsed without execution; seven requirements with all mandatory fields; 30 original fingerprints unchanged.` This validates document structure/syntax, not the unimplemented verification commands' runtime behavior.
- Fresh-context I1 requirement review completed: five initial gaps resolved; no blocking specification ambiguity on re-review. Full findings/resolutions in [review](slice-0-review.md). This is not application acceptance evidence.
- Files created/reasons/concepts: [setup change map](../traceability/slice-0.md). Application files modified/deleted by this setup: none.

## Required acceptance not yet run

| Checks | Status | Reason |
|---|---|---|
| Restored wrapper/version/build/package | NOT_RUN | Repair not approved/implemented |
| Test target guard | NOT_RUN | Proposed test source/profile not created |
| Fresh DB/Flyway/context | NOT_RUN | Environment proposal pending, existing DB left untouched |
| Supplier HTTP/real persistence/negative cases | NOT_RUN | No app startup/DB test performed |
| Second startup/persistent row | NOT_RUN | Same prerequisite |
| Final README commands/completed-slice reading guide | NOT_RUN | Implementation and final observed results pending |

No Slice 0 completion or stronger independent assurance claim is made. A fresh-context requirement review is recorded separately in [review](slice-0-review.md); it does not prove executable behavior.
