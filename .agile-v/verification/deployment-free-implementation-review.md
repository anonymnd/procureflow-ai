# Free deployment implementation review

VER-DEP-IMPL-001, 2026-10-14, Codex reviewer, separate fresh context, conservative independence **I1** under the local lightweight profile. AI-assisted review; same model family, filesystem and tooling as the builder. This is neither organizational independence nor a new gate.

## Verdict and boundaries

**PASS for the local deployment package technical subset:** reviewed container/configuration safely supports proceeding toward a separately reviewed current-source release snapshot and hosted setup under existing scoped authority. No demonstrated application/configuration blocker was found. This is not source-publication authority, an account action approval, full Slice0-8 re-verification or deployment completion.

**NEEDS CHANGES for overall BL-DEP-R1 deployment acceptance:** required hosted DB/email/free-plan/source-commit/HTTPS journey proof is absent. TC-DEP-005 is explicitly owner-deferred by DIR-DEP-GROK-DEFER-008, not passed, deleted or implicitly implemented. No hosted URL or final acceptance is asserted.

Scope: REQ-DEP-001..004@revision1 / BL-DEP-R1 / issue28 / GATE-DEP-IMPLEMENT-DELEGATED. Read authoritative approvals directly; implementation authority is distinct from requirement approval. Issue28's existence/body readback is builder evidence; this reviewer did not query GitHub or external accounts. The frozen combined normative bundle includes decisions, impact and test expectations. Its capture-time draft wording remains immutable and is superseded for stage/authority only by actual approvals.

## Actual change range

Reviewed base/HEAD: `132ea7765725290c4c89abaeb263c43d0bcee38a`; no deployment commit exists. Existing accepted Slice0-8 is mixed staged/unstaged/untracked work, preserved. Review used actual working files and `git status`, both index/worktree awareness and actual new-file contents rather than relying on a tracked-only diff. The deployment-owned application range is new `Dockerfile`, `.dockerignore`, `docker-entrypoint.sh`, `render.yaml`, `src/main/resources/application-deploy.properties`, `docs/deployment.md`; modified `.gitignore`, `.gitattributes`, deployment study paragraph in `docs/code-reading-guide.md`; new `.agile-v/tests/deployment-container-check.cjs` and `.agile-v/verification/deployment-container-results.json`; applicable deployment requirement/baseline/approval/state/results records. No deployment-owned Java/business JavaScript/dependency/entity/FK/migration edit or deletion was identified. Other dirty slice/workflow changes are preserved dependencies, not attributed to deployment.

Inspected surrounding actual `application.properties`, `IdentityEmailService`, `IdentitySecurityConfiguration`, canonical Slice4 browser helpers/scenarios, workflow and study navigation. Endpoint flow remains public catalog controller/service/repository query for readiness, identity controller/service/JavaMailSender for verification, existing procurement routes/services/repositories for commercial writes. Deploy settings create no API DTO, mapper, table, entity relation, owning side or FK; the existing feature/entity maps remain authoritative. Cross-module relation is environment -> native Spring datasource/mail/server -> existing identity/marketplace/procurement. No new business module dependency.

## Evidence inspected and independently observed

Builder I0 evidence: actual native/image build succeeded with tests skipped; isolated deployed-profile real Chromium journey reports **8/8 grouped PASS**, including identity/grants, private two-recipient RFQ, quotations, award/order, fulfillment/receipt/replay and four local STARTTLS messages. Reviewer inspected the executable adapter and unchanged canonical scenario source rather than accepting names alone. The adapter uses declared self-signed browser TLS bypass only for localhost, dynamically reuses canonical helpers/scenarios, and retains existing read-projection rendering fixtures. It does not prove exact server expiry/currency eligibility anew. Local SMTP sink accepts fixture authentication; it proves JavaMailSender performed authentication over required TLS, not real provider credential acceptance or delivery.

Read-only reviewer observations:

- Docker image/container: image `sha256:71107b11a3fd5aec5a519046fa9df33db02577e4f2d927bc870132fd42d4d721`, UID/GID `10001:10001`, memory limit `536870912`, running, OOM false, restart count0, memory345.9MiB at review. Java runtime17.0.20.1. Entrypoint bytes inside image match current source SHA-256.
- Every current `src/main/resources` file byte-matches its `BOOT-INF/classes` entry in the running image jar, including deploy profile, V1-V8 and browser assets. Jar contains146 application class entries. Reviewed source/pom fingerprint below binds current files; resource equivalence does not independently establish byte-for-byte compilation of every Java source into that image.
- Local HTTP readiness GET `/api/public/offerings` returned200; anonymous `/api/procurement/rfqs` returned401; `/api/auth/csrf` returned200 and Secure/SameSite=Lax cookie. XSRF cookie deliberately remains JavaScript-readable; session HttpOnly is configured separately in the existing properties.
- Read-only isolated DB query confirms Flyway versions1..8 all success. An immediate public read followed by `pg_stat_ssl` query confirms application JDBC connection TLSv1.3/TLS_AES_256_GCM_SHA384. A previous idle query showed only the reviewer local socket; this is consistent with min-idle0, not plaintext application proof.
- Missing-setting container has exit1 and only `Missing required deployment setting: PROCUREFLOW_DB_URL` in logs.
- Source inspection confirms SSL verify-full/Java trust store, required SMTP STARTTLS/hostname checking/timeouts, small pool/heap, secure session cookie, graceful shutdown/Render port, no new privileged readiness endpoint, no AI consumer/secret, allowlisted image context and secret exclusions.
- Subsequent implementer-owned `procureflow-deploy-badhost-20261014` uses the same image/512MiB and exits1/stopped; reviewer independently inspected logs reporting certificate SAN mismatch for `wrong-cert-host` and `PgjdbcHostnameVerifier` rejection. This closes the local DB hostname-negative gap. No new application/configuration edit was needed.

Commands included `rtk proxy docker inspect`, `docker image inspect`, `docker exec ... sha256sum`, `docker stats --no-stream`, local curl status/header probes, `docker exec ... psql` SELECT-only queries, and a Node read-only jar/resource comparison and SHA-256 inventory. All were prefixed through RTK. A guessed fixture DB role query failed, then the nonsecret actual role was read and successful SELECTs used it. An unavailable PowerShell hash cmdlet and a quoting error were replaced by Node SHA-256 and correctly quoted SELECT commands. These failed reviewer probes are not tests passed or application failures.

Limits: reviewer did not rerun the browser journey, send any external mail, access credentials/accounts/browser, create resources, change configuration, rebuild image, exercise sustained-load/cold-start/graceful-shutdown or SMTP failure paths, publish source, deploy, or rerun historical full103/browser12 Slice8 suites. Invalid DB hostname rejection was independently inspected after the builder executed it. Production trust-chain/SMTP2525/Render forwarding behavior require hosted proof.

## Ten finding categories

1. **Requirement violations** — F-DEP-001 (MAJOR acceptance gap, FT-PLAN), REQ-DEP-001/002/004, `docs/deployment.md` completion contract and results remaining-proof section: frozen criteria require actual free isolated resources, reviewed deployed commit, live HTTPS URL and designated email/whole journey. Actual evidence is local fixtures and pending account setup. Complete authorized hosted setup and record the observed commit/URL/free-plan and email/journey evidence before final acceptance. No code bug is inferred from missing account proof. REQ-DEP-003 remains explicitly deferred by owner, never PASS.
2. **Workflow violations** — none open. F-DEP-002 (MINOR records gap, FT-PLAN) is CLOSED: implementer added the missing `.agile-v/REQUIREMENTS.md` deployment pointer with baseline, issue, authorization, pending-hosted status and owner Grok deferral; reviewer read it back. Initial stale STATE/results progress statements were reported to implementer; re-read corrected current STATE and current-remaining summary closes that inconsistency. Frozen history is preserved.
3. **Architecture violations** — none observed. Native settings and one monolith follow DEC-DEP-001/003/005; no business abstraction/library/infrastructure beyond approved deployment container/profile.
4. **Bugs** — none demonstrated within reviewed package scope.
5. **Missing tests** — F-DEP-001's hosted TC-DEP-002/003/004 proof remains absent. DB hostname mismatch rejection is now observed; SMTP downgrade rejection was not exercised by reviewer. Inspect hosted certificate/authentication behavior before acceptance. This is a verification limit, not an invented baseline requirement or demonstrated TLS bypass. TC-DEP-005 remains owner-deferred.
6. **Missing/incorrect traceability** — none open in local package; F-DEP-002 index gap is closed. Current results inventory explicitly includes deployment test/JSON and exact created/modified paths; issue/baseline/decision linkage is present. Release snapshot/deployed commit mapping remains legitimately pending and must be added on observation.
7. **Missing/incorrect code-reading documentation** — none open after implementer record reconciliation. Deployment study supplies before/after, rationale, reading order, exact-path references, inherited authorization and explicit no-new-relation/FK/behavior with local-vs-hosted evidence limits.
8. **Security concerns** — none demonstrated in application/config package. Local-only certificate fixture exception is explicit and excluded from image context. Hosted credentials/verified sender/no-card free workspace/charge-capable paths require actual controlled setup; secrets and real recipient details were not inspected or reproduced. No security assurance beyond this scope is claimed.
9. **Overengineering / unnecessary complexity** — none observed. Test adapter's source slicing and machine-specific browser paths are a fixture-maintenance limitation; assertion guards expected boundaries. They introduce no production dependency or feature.
10. **Exact corrections FCC must make** — selected implementer under DIR-ROLES-002: local index/status and DB hostname-negative corrections are closed. Complete permitted hosted free DB/SMTP/Render setup, then record observed plan/payment constraints, isolated resource provenance, source commit, HTTPS URL, designated-email delivery/login and buyer/supplier receipt journey. Obtain separate review of resulting actual publication/deployment evidence and separately record acceptance. Keep Grok disabled/deferred; no inference, card, payment or unapproved AI implementation. Do not alter BL-DEP-R1 or force-push/reset existing work/data.

## Review artifact binding (SHA-256)

Image manifest-list/container reference: `71107b11a3fd5aec5a519046fa9df33db02577e4f2d927bc870132fd42d4d721`; builder-reported config digest `1e1d85f86f490f2e1e362c1fa40f8cec77803038719ff5052fd1aabda3eae147` is distinct. Running `/app/app.jar`: `d969e6d44ea1b7cad9d3c4eeb2aec4a00296dc6d72ff1aa1aa4a5b378aa4a08e`.

| Artifact | SHA-256 |
| --- | --- |
| Frozen manifest | `049397fa8799e260d9ff2080e05efa1152d71099ef52591c329366090f6bdc7b` |
| Frozen requirement | `9a7ec2bf6c10373e8432e4b897c00b478f923bd7a3acb1d7ccd2e9bf6a43657b` (matches manifest) |
| Dockerfile | `9bda68a2204bd376b46e77fa2b45c1f84e5c98d1bdcfeffdd7b2362fc856ab86` |
| .dockerignore | `0249f55fa977c975e9287690438a6ce7b8bf63b8b8251a059571ce96f038f984` |
| docker-entrypoint.sh | `9042c3bd78b21e1535e98a4a3a6e93d83d035a8dea17aadca0ece292b4b6b126` |
| render.yaml | `b281f04db6d8690d5a03a7cb7c43c3a3ca8a68d6da74642eb816ae94a3a3f653` |
| application-deploy.properties | `054fbfed0a29f3f550e45bb63b29415d26c2b56c9f427095200577e5778b778c` |
| .gitignore | `e4e6963024fed374571a167bcadeb974b8d1f72e34c3884a7548c4cae7953b7e` |
| .gitattributes | `87cd84edb25f0a0b94bc0c67b9d62e52e578e5c11c246b71ec4d2ce9894cb241` |
| docs/deployment.md | `6d3dae7605cdb948e2427d1b2ab54e7737341387c1f0c5651de1e55b0559bc2a` |
| docs/code-reading-guide.md | `994e6002e10f9efb921484dbc44d771ae8d239d20ce6c7095d3d595496eb28a7` |
| deployment-container-check.cjs | `5befc4b8e15f24f5b6dbf0ab92b7aa16e4f8deb6b0c8e9c0744b79b70d89f15c` |
| Canonical slice-4-browser-check.cjs | `74ecbe98ad89bded38e6f194859b55147880b0368bb1712fdaec1d8582edc14d` |
| deployment-container-results.json | `08147019b10c19f4eabab4523dd76bb2c1a9845bb6c0957ec0424b6ae30020b1` |
| deployment-free-results.md (after negative proof) | `42d3c9d1956ba9dc33e10bf46a16be002d784bfcf52782dd5a7bc7b5366919a4` |
| REQUIREMENTS.md (corrected index) | `1a081649f26688e4e572c09f9dc1c732d0dbd9361bb772f70ce11ef349af073f` |
| STATE.md (reconciled capture) | `7e8167bf7a32b49ee8cd7994d3f35ce038b38aff205be1ce43a54a3f9ca2a0f0` |
| APPROVALS.md | `25356966e7066c17cbed8790b32c730fb76970362b5a6f1c797ae9d82855e38f` |

Current source binding: recursively enumerate every regular file under `src` plus `pom.xml`; sort strings `relative/path lowercase-sha256`; SHA-256 of those joined with LF plus final LF = `e43ebf9477b62121de1cbcf8e79514f8b15c52ca52cfe722c0e77629b87356d0` (118 files). This binds current accepted dirty dependency source and deploy resource, not an invented release commit.

VER-DEP-IMPL-001 | TC-DEP-001/local portions of002/004 | REQ-DEP-001/002/004 | PASS (local subset only) | FT-PLAN | actual static package, resource/image binding and read-only runtime evidence inspected.

VER-DEP-IMPL-001 | hosted TC-DEP-002/003/004 | REQ-DEP-001/002/004 | FLAG | FT-PLAN | overall acceptance NEEDS CHANGES; required live evidence absent. TC-DEP-005 explicitly deferred by direct owner direction. No publication/final gate exercised.
