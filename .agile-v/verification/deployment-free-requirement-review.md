# Free deployment requirement-quality review

Current verdict: **PASS**, VER-DEP-REQ-003 below, corrected unfrozen R1 SHA-256 `9A7EC2BF6C10373E8432E4B897C00B478F923BD7A3ACB1D7CCD2E9BF6A43657B`.

Initial VER-DEP-REQ-001, 2026-10-14, Codex reviewer, AI-assisted separate-context I1: **NEEDS CHANGES** for two bounded, pre-baseline specification corrections. The historical findings and interim review are retained below. This is requirement-quality review, not implementation verification, owner approval or publication authority.

## Reviewed scope and provenance

Reviewed `.agile-v/requirements/deployment-free.md` revision1, combined normative REQ-DEP-001..004 / DEC-DEP-001..005 / TC-DEP-001..005 / proposed impact map. No deployment baseline exists yet; no frozen baseline was edited. Authority consulted: DIR-DEPLOY-FREE-007 and standing delegated workflow in APPROVALS. Role/procedure: AGENTS, RTK, agile-v-core, logic-gatekeeper and the local lightweight profile in `docs/planning/agent-workflow.md`. Current STATE/index still describe Slice9 planning; the new deployment draft is separately identified here, and Slice9 remains unapproved.

Observed local HEAD `132ea7765725290c4c89abaeb263c43d0bcee38a`; local origin/main reference `eaacdca94d635ced5aa3044763e60d4685b582f3`. Inspected git status: accepted work includes staged, unstaged and untracked files; HEAD alone is not a releasable Slice8 artifact. Read origin/main's POM/tree from the local reference; no remote fetch/publication performed. Source inspection: current `pom.xml`, `application.properties`, `AppApplication`, `IdentityEmailService`, `IdentitySecurityConfiguration`, `IdentityService` registration excerpt, `PublicCatalogController`, `PublicCatalogService`, migration paths V1-V8 and `.gitignore`; affected guide/decision navigation and retained Slice8 review/results. No source/profile/container was built or run.

Capture SHA-256:

| Artifact | SHA-256 |
| --- | --- |
| deployment-free.md R1 | `25CDF3F8C901B47B332C8B1727BEBA0ADFECA3775B34E3C7A71F654A74FBD7CA` |
| pom.xml | `BE8C90459397AB2334F84013EA2639FE87C0F699995C9F9294490210B66A54B5` |
| application.properties | `D66443CADD6E4FBE6CBB6F7BD2A77E5059F81CCC357C552B4B5214411FCA3D5D` |
| IdentityEmailService.java | `72DBB1915B550BFE53450BD9310A320C8AB169AAA1BCE131B0BB01205E3F226B` |
| IdentitySecurityConfiguration.java | `2561DF521EC27A09F05409146FEFA9CDD7F785A3856647D25715017A90BD2DA9` |
| APPROVALS.md at review capture | `794971A60F5E074A0AD08834C6791A5A8024F740DA8AB74BFD277E75796D044D` |
| agent-workflow.md | `501713D5E81F6536314155840DEFA9D2D7633E180D1268A04F99329BBCD0BE3F` |

## Findings

### 1. Requirement violations

- **FND-DEP-001 — Medium; REQ-DEP-001/002/004, TC-DEP-001/002; deployment-free.md:11,15,23,49-50.** Expected: quantitative resource/timeout acceptance criteria, sufficient to falsify an unsuitable deployment on 512MiB. Actual: “bounded heap suitable for512MiB”, “small bounded pool” and “bounded connection/read/write timeouts” leave the values and success conditions open. Declaring the platform RAM alone does not define JVM/native headroom or a reproducible proof. This is draft ambiguity, not an observed OOM/SMTP bug. Correction: specify chosen heap ceiling, JDBC maximum pool/minimum idle and finite SMTP timeouts; run the container under the actual 512MiB limit and require startup plus the defined journey without OOM/restart. A reasonable proposed starting point is heap256MiB, pool3/minimumIdle0, SMTP connect/read/write10000ms, with explicit revision if actual proof requires changes. The planner owns final values.
- **FND-DEP-002 — Medium; REQ-DEP-003, TC-DEP-005; deployment-free.md:19,53.** Expected: an auditable synthetic-call bound under the owner's zero-cost constraint. Actual: “one bounded synthetic Grok request” does not define input bound, output token ceiling, request deadline or retry/fallback behavior. The USD1 key budget is explicitly soft, so it cannot substitute for request bounds. Correction: fix one fake input or input-size ceiling, explicit output token maximum, total deadline and no automatic retries/fallback/tools. For example, one short fake purchasing note <=1000 characters, max_output_tokens128 (using the actual compatible request parameter), total deadline30s, one attempt; retain status/model/usage/error with secrets redacted. Confirm free-credit availability and no charge-capable inference path immediately before this single request; stop instead of purchasing credit if unavailable. These bounds concern access smoke only, not the separate Slice9 corpus or implementation.

### 2. Workflow violations

None demonstrated. Draft approval/baseline/issue/implementation gates remain absent and distinct. TC-DEP-004 requires a reviewed source snapshot; preserving main, staged work and original history is explicit. Existing baseline immutability is explicit. Review does not exercise publication authority.

### 3. Architecture violations

None. Current POM declares Spring Boot4.1.1 and Java17; proposed Docker runtime/profile retains the native modular monolith, same-origin assets/APIs, existing JPA/Flyway and JavaMailSender. No new dependency or speculative infrastructure is proposed. Prior retained tests used Java25; Java17 package/runtime proof remains an explicit deployment prerequisite, not already-established evidence.

### 4. Bugs

None demonstrated by this requirement review. Current local datasource defaults and SMTP optional STARTTLS are actual source observations; replacing those defaults in the new fail-closed deployment profile is proposed work, not an unimplemented draft defect. IdentityEmailService also defaults public base/from to localhost, so implementation must supply the required real HTTPS URL/verified sender.

### 5. Missing tests

No wholly missing requirement test path. TC-DEP-001..005 cover container/runtime, onboarding/TLS, isolated free resources/secrets, exact deployment snapshot/live journey and provider smoke. Amend quantitative assertions for FND-DEP-001/002. Pending account activation, daemon availability, TLS/delivery, live container and Grok access are disclosed implementation/runtime prerequisites; their absence is not a requirement-review FAIL in itself.

### 6. Missing/incorrect traceability

None requiring a draft correction. REQ -> DEC -> pending issue -> proposed file/provider -> unchanged DB/API -> TC -> pending verification is explicit. Hosting/AI credentials introduce no entity/FK. Existing entity maps remain authoritative; exact file inventory and issue URLs are correctly pending.

### 7. Missing/incorrect code-reading documentation

None requiring a draft correction. Proposed reading order and actual/proposed boundary are present. The human guide update and deployment study remain implementation deliverables. Capture-time guide statements do not override current approval scope.

### 8. Security concerns

No additional demonstrated draft defect. Requirements retain secure session cookie/CSRF/auth/grants, verified DB host/certificate, SMTP required STARTTLS/hostname validation, secret exclusion, no privileged seed and no third-party email authorization. The live two-party journey needs distinct owner-designated recipient identities: actual IdentityService registration rejects an existing normalized email. Obtain the necessary explicit recipient designation before sends; do not infer that a second address/alias is authorized. This is an execution prerequisite, not a demand for a new identity feature. Grok soft-budget uncertainty is addressed by FND-DEP-002 and must not be represented as a hard cap guarantee.

### 9. Overengineering / unnecessary complexity

None. Render + dedicated Neon + native SMTP + gateway setup address concrete requested needs; no Vercel frontend split, actuator, SpringAI or mail adapter is proposed without evidence.

### 10. Exact corrections FCC must make

The current planner/implementer role under DIR-ROLES-002, rather than a fixed product, performs corrections:

1. Revise the live draft and TC-DEP-001/002 with numeric heap/pool/SMTP limits and a 512MiB startup/journey no-OOM acceptance check (FND-DEP-001).
2. Revise REQ-DEP-003/TC-DEP-005 with input/output/deadline/attempt bounds, explicit no automatic retries/fallback, and action-time free-credit/no-charge eligibility check (FND-DEP-002).
3. Request affected-clause separate-context re-review, recording the new requirement revision/hash. Only after PASS record any delegated requirement gate, immutable deployment baseline, meaningful issues and distinct implementation authorization. Do not edit existing Slice0-8 baselines or claim Slice9 approval.

## Evidence limits and outcome

Browser provider facts were not independently re-observed. The draft's primary documentation URLs/capture notes are planner evidence, not I1 browser proof; plan, quota, card/overage, model eligibility, account/key options, terms and actual endpoint behavior require action-time validation. No account/credential/resource creation, terms acceptance, inference, email, DB change, Docker run, publication or application edit occurred in this review. Retained Slice8 PASS/test counts are historical provenance only and do not prove deployed behavior. Docker daemon absence is recorded by the planner; this context did not independently probe it.

**Decision: NEEDS CHANGES**, FND-DEP-001/002 only. Scope reviewed: REQ-DEP-001..004; skipped unchanged application acceptance criteria; baseline edited: no. Re-review the precise revised limits; do not turn not-yet-run implementation checks into planning-stage blockers. This technical verdict approves no gate or deployment.

## Affected-clause re-review — VER-DEP-REQ-002

The planner added a numeric configuration contract while R1 is still an unfrozen draft. Actually reread the modified normative verification section; SHA-256 `0F485E6B2CD69EBF568CD2C3450CAC0B3F39A8F0E3FEEAD7B84F3C44D6CE2119`. HEAD/reference and source scope remain unchanged; no revised baseline exists. Earlier findings above describe the original capture, not the current complete draft.

- FND-DEP-001 closed: initial/max heap64/256MiB, metaspace128MiB, code cache64MiB, container512MiB; pool3/minimumIdle0 and concrete JDBC/SMTP timeouts are now normative. Actual 512MiB runtime fit must still be observed before publication. Container startup and the defined journey must complete without memory-related restart/OOM; this interprets the explicit fit criterion, not a claim that it already passes.
- FND-DEP-002 partly closed: connect2s, total60s through body reading, output1024 completion tokens, raw response32KiB, fixed model, temperature0, no tools, sequential requests and no automatic retries now define output/time behavior. Still add a concrete fake-input size bound (suggest <=1000 characters) and explicitly check free-credit availability/no charge-capable inference path immediately before the smoke. Render's payment-method stop condition does not itself validate the Vercel inference account. Soft usage budget is not a hard no-charge guarantee; no provider charge is asserted to have occurred.

**Current verdict: NEEDS CHANGES**, only the remaining FND-DEP-002 clauses. All original browser/runtime/account/authority limits remain. No new implementation test failure or requirement contradiction is alleged.

## Final affected-clause re-review — VER-DEP-REQ-003

Actually reread the final numeric contract and appended Additional proof boundaries. Corrected unfrozen R1 SHA-256 is `9A7EC2BF6C10373E8432E4B897C00B478F923BD7A3ACB1D7CCD2E9BF6A43657B`. Re-observed HEAD `132ea7765725290c4c89abaeb263c43d0bcee38a` and origin/main local reference `eaacdca94d635ced5aa3044763e60d4685b582f3`. This is a final affected-clause separate-context I1 re-review after VER-DEP-REQ-002, not a new source/runtime verification or a frozen baseline amendment. The planner now reports Docker Desktop started; this reviewer did not independently observe that startup or container proof, and the earlier absence statement remains a historical capture.

- **FND-DEP-001 closed.** Numeric heap/metaspace/code cache/pool/JDBC/SMTP limits remain; the draft now explicitly requires startup plus TC-DEP-004 browser journey under512MiB without OOM/restart and revision instead of implicitly upgrading to paid capacity.
- **FND-DEP-002 closed.** Fixed fake input <=1000 Unicode code points is normative alongside the explicit token/time/response/attempt bounds. Immediately before sending, selected-model eligibility, available free credits and absence of purchased-credit/auto-top-up/other charge-capable path must be verified; uncertainty or insufficiency forbids sending. The USD1 budget is expressly not zero-charge proof. No paid inference is assumed safe.

All ten finding categories above were reconsidered for the revised clauses: no open requirement/workflow/architecture/bug/test-path/traceability/study/security/complexity finding remains; exact corrections are complete. Actual Java17 container fit, account/terms/billing/key options, TLS/email, publication snapshot/index preservation, deployed two-party journey and synthetic API access remain future execution proof under their existing scope and authorization constraints. Provider-browser assertions remain recorded planner evidence with the same I1 limitations.

**Final current verdict: PASS for requirement quality of this exact corrected R1 hash.** Neither application readiness, provider availability, free-only execution nor deployment success is claimed. No application edits, gates, baseline edits or account actions occurred in this context. Record any requirement approval, immutable deployment baseline, issue/implementation authorization and later deployment/acceptance separately.
