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

## VER-S0-014 - Requirement approval, baseline and issue publication

Date: 2026-10-10. Actor: Codex, approval/lifecycle administration; I0 self-check, not an independent implementation review. Authority: project owner "i approve slice 0" -> GATE-S0-R1. Requirements: REQ-S0-001 through REQ-S0-007 revision 1, BL-S0-R1; DEC-S0-001 through DEC-S0-004 and normative companions included.

- Checked git status (clean) and HEAD ab33bb68a01ad582569f3778266cb6efb0151b55 before changes.
- Copied the four reviewed companions byte-for-byte before live status bookkeeping; per-file SHA-256 values and original paths are in the baseline manifest.
- Existing-issue connector search returned no issues. Connector create returned HTTP403 "Resource not accessible by integration"; no issue was created by that rejected call.
- Fallback used existing GitHub Git credentials without printing them. Initial sandbox Credential Manager access failed; approved escalated read checked the REST issues and labels. Approved escalated publication created #1/#2/#3 with real URLs, approval/baseline hashes, scope/criteria/tests and dependencies. API result snapshot is an ignored transport artifact under target/slice0-approved-issue-results.json.
- Actual issues: https://github.com/anonymnd/procureflow-ai/issues/1 (IU-S0-A); https://github.com/anonymnd/procureflow-ai/issues/2 (IU-S0-B); https://github.com/anonymnd/procureflow-ai/issues/3 (IU-S0-C). #2 integration depends on #1; #3 depends on #1 and #2. Existing labels enhancement/documentation only.
- Issues explicitly state implementation NOT AUTHORIZED, GATE-S0-IMPLEMENT pending. They link reviewed documents at the source checkpoint and disclose that local approval/baseline bookkeeping is not yet pushed.
- No application/implementation-test source changed; no database/container/volume operation, feature test run, commit or push by this approval task. Runtime acceptance remains NOT_RUN. No implementation review PASS or final human approval claimed.

A first lifecycle-update shell block failed to parse before executing; its corrected block completed with exit0. This affected no approved baseline or application files. Final hash/source/diff checks are recorded below.
Final approval-task checks: exit0; four frozen SHA-256 values match the manifest and snapshot text matches the reviewed source checkpoint; live Markdown local links resolve; Git text conversion is disabled for frozen copies to preserve exact bytes; application/build/planning/skills/AGENTS unchanged and no new application files; diff whitespace clean. A fresh connector search independently fetched all three issue bodies and confirmed baseline/requirements, explicit NOT AUTHORIZED status and actual dependency URLs. This is document/API publication bookkeeping verification, not runtime acceptance or independent implementation assurance.

## Current Slice 0 execution — 2026-10-10

Earlier NOT_RUN/setup statements are historical. Actor: Codex, explicitly authorized builder under GATE-S0-IMPLEMENT, AI-assisted; I0 self-checks. Requirement revision1 / BL-S0-R1, REQ-S0-001..007, DEC-S0-001..004. Preimplementation base ab33bb68a01ad582569f3778266cb6efb0151b55 plus dirty planning files captured before edits. No final human approval/commit/push/merge/deployment.

### VER-S0-015 — Wrapper and database-free suite

TC-S0-001/002/006/007/008/009/011, REQ-S0-001/004/005/006. Restored only mvnw.cmd from 44165770de45f249a4efc94ea923fbf22548b76a. Wrapper `-v`: Maven3.9.16 / Temurin25.0.3, Java target17 unchanged. `rtk proxy powershell -NoProfile -File target/slice0-build.ps1 -TestSelection SupplierServiceTest,SupplierControllerTest,Slice0DatabaseGuardTest`: exit0, 56 tests/0 failures/0 errors/0 skips, finished 2026-10-10T17:49:26+01:00. Builder PASS; no Java17 runtime claim.

### VER-S0-016 — Full guarded PostgreSQL verify/package

TC-S0-002..009/011, REQ-S0-001..006. Started existing container only and created retained isolated `procureflow_slice0_20261010165158_c7b60445`; datasource `jdbc:postgresql://localhost:5332/procureflow_slice0_20261010165158_c7b60445`. No business DB target or drop/reset/volume recreation. PostgreSQL17.11, Hibernate7.4.5.Final; unchanged V1 migrated the fresh DB successfully once.

First full verify had 67 tests, two failed integration JDBC name assertions, no errors/skips. Cause: JPA pending changes not flushed to JDBC inside rollback test transaction. Corrected test helpers to flush/clear before independent JDBC reads, then reran full verify. No application persistence bug inferred from that timing failure. A later fresh-context review found Hikari spring.datasource.hikari.jdbc-url (and native datasource/JNDI/driver properties) could override the guarded primary URL. The reviewer confirmed the Boot 4.1.1 binding path. The guard now rejects alternate Hikari targets unless jdbc-url matches; four rejection and one acceptance cases were added.

The intentional missing-config preflight had overwritten its context report; it was saved separately and final verify reran on fresh `procureflow_slice0_20261010172658_272a1249`. Command `rtk proxy powershell -NoProfile -File target/slice0-verify.ps1` ran verify with explicit safe environment; exit0, finished 2026-10-10T18:29:07+01:00. **72 tests, zero failures/errors/skips; BUILD SUCCESS**, jar packaged. Reports: target/surefire-reports/TEST-*.xml (9 service, 16 MVC, 36 guard, 10 integration, 1 context). Read-only SQL: one successful V1, tables flyway_schema_history/suppliers only, count0 after rollback. The preceding 67-test run passed before the five Hikari cases. The 110 HTTP/restart checks ran against first retained DB procureflow_slice0_20261010165158_c7b60445; application behavior code was unchanged by the guard-only correction.

### VER-S0-017 — Actual unsafe/missing-config rejection

TC-S0-011, REQ-S0-002/006. Guard unit matrix passes. `rtk proxy powershell -NoProfile -File target/slice0-preflight.ps1`: script exit0, expected Maven exit1; missing explicit target rejected with `Slice 0 database preflight:` before Hikari startup or migration. Log target/slice0-preflight.log. First harness attempt aborted on PowerShell treating native stderr as an error; corrected only capture handling and reran. No datasource initialized. Builder PASS.

### VER-S0-018 — Packaged HTTP and restart

TC-S0-010 plus CRUD/client matrices, REQ-S0-001..005/007. Initial smoke refused occupied8080 before starting a jar; unrelated service preserved. Verification-only port18080 chosen. `rtk proxy python target/slice0-http-smoke.py`: exit0, **110 recorded HTTP/startup/restart checks PASS**. Both jar starts use same safe DB, explicit local credentials/placeholders, validate/Flyway/clean-disabled, localhost-only binding and owned process cleanup. Credentials excluded from results.

Observed POST201/list-detail200/PUT200 persisted rename/DELETE204 empty; missing/negative/repeated deleted ID404; nonnumeric/overflow long400; POST+PUT missing/null/empty/blank/256-name400 VALIDATION_ERROR; missing/malformed JSON400 BAD_REQUEST; valid1/255 names persisted; duplicate/untrimmed names retained. Each rejected request left list snapshot unchanged. Both starts succeed; marker ID31 and all six retained rows survive restart. HTTP timeouts10s. 409/500 fault injection is proven in MVC (TC-S0-009), not by intentionally damaging the DB.

Artifacts: target/slice0-http-results.json, target/slice0-jar-start-1.log, target/slice0-jar-start-2.log. HTTP DB procureflow_slice0_20261010165158_c7b60445 remains with six rows; post-review full-suite DB procureflow_slice0_20261010172658_272a1249 remains empty. Jar processes stopped. Builder PASS; local-only development acceptance, no deployment/security readiness claim.

### VER-S0-019 — Preservation/documentation evidence

TC-S0-012/013, REQ-S0-006/007. Actual file/test/DB/API map and study record updated, README corrected to real backend and safe commands. Immutable baseline hash check, original-work comparison, final source/report/log fingerprints and local link checks are captured in [evidence manifest](slice-0-evidence.json). Fresh-context actual review is separate in [review](slice-0-review.md); builder checks do not prove human acceptance.


Owner final acceptance of Slice 0 revision1 / BL-S0-R1 was recorded in [APPROVALS.md](../APPROVALS.md) on 2026-10-11 after the above evidence and I1 PASS review. This records acceptance, not commit/push/merge/deployment authority.

## Slice 0 publication preflight - 2026-10-11

Resumed the owner's recorded instruction to audit and publish only the accepted Slice 0 change set. The previous command-runner failure is not reproduced in this context. `rtk proxy py target/slice0-audit.py` completed with exit0 using approved runtime access: four frozen hashes, 53 preservation comparisons, 72 retained passing test results, 110 retained passing HTTP checks, missing-configuration rejection and local documentation links verified. This audit inspects retained evidence; it is not a new application-test run or a new independent review. Actual staged application/test diff inspected against approved scope and the existing I1 review; no additional application changes made.

Git's default whitespace check flags preserved CRLF in the immutable manifest; the CR-at-EOL-aware check passes without editing frozen content. Corrected current publication authority in STATE and stale review/acceptance wording in the actual study section. Evidence limitation labels now reflect recorded final acceptance. Publication remains scoped to one Slice 0 checkpoint, not merge/deployment.