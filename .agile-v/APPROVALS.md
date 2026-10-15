# Scoped human approvals

Append actual user decisions; never infer feature approval from tooling approval.

| ID | Date | Approver/source | Scope | Result |
|---|---|---|---|---|
| APR-SETUP-001 | 2026-10-09 | Project owner: "I approve plus add these" followed by "approve" to the combined proposal | Seven Agile V skills, three architecture/Spring skills, project-level installation, lightweight governance/traceability/study structure and drafting Slice 0 | Approved; setup performed |
| APR-PUSH-001 | 2026-10-09 | Project owner: GitHub repository link and "push first" | Commit and push current project work, installed skills and draft planning/specification to origin/main | Authorized; no Slice 0 requirement or implementation approval implied |
| DIR-ROLES-001 | 2026-10-09 | Project owner: explicit Codex / FCC Claude + Kimi role split and review workflow | Codex planning/governance/code review; FCC implementation/corrections; maintain existing workflow and use requested handoff/review format | Active directive; no slice requirement, implementation, issue creation or final approval granted |
| GATE-S0-R1 | 2026-10-10 | Project owner: "i approve slice 0" | REQ-S0-001 through REQ-S0-007 revision 1; DEC-S0-001 through DEC-S0-004; normative test-plan and traceability companions | Approved and frozen as BL-S0-R1; application implementation remains unauthorized |
| GATE-S0-IMPLEMENT | 2026-10-10 | Project owner: "okay now i want you to be the coder see whats is have to be done asked by the plander and do it" | Codex implements approved Slice 0 / BL-S0-R1 delivery units GitHub #1/#2/#3 and Jira SCRUM-6/7/8, including approved isolated-DB verification | Authorized for Codex by explicit owner exception to DIR-ROLES-001; no merge/push/deployment or final approval |

Standing instruction: automatically create GitHub Issues only after the corresponding slice requirement is explicitly approved. Issue creation does not authorize implementation. GATE-S0-R1 now satisfies the requirement gate for Slice 0 issue creation.

Approved bundle: [BL-S0-R1 manifest](requirements/baselines/slice-0-r1/manifest.json). Four reviewed documents were copied byte-for-byte with individual SHA-256 hashes before live lifecycle bookkeeping. Their historical draft/pending labels are superseded by this approval record; acceptance criteria are unchanged. The approval includes the isolated-database preservation boundary in DEC-S0-002. It does not authorize database operations, application/test implementation, merging, deployment, or final slice completion.


## Slice 0 issue publication

2026-10-10: under the standing post-requirement-approval instruction, created [#1](https://github.com/anonymnd/procureflow-ai/issues/1), [#2](https://github.com/anonymnd/procureflow-ai/issues/2), [#3](https://github.com/anonymnd/procureflow-ai/issues/3), each bound to BL-S0-R1 and explicitly NOT AUTHORIZED for implementation. #2 integration depends on #1; #3 depends on both. Issue creation is complete; GATE-S0-IMPLEMENT and final human approval remain absent.

## Implementation authorization supersedes the earlier planning-only state

2026-10-10: GATE-S0-IMPLEMENT records the actual owner instruction to Codex to code approved Slice 0, including approved isolated-DB verification. Earlier paragraphs and issue-publication entries describing an absent implementation gate are historical. The immutable baseline manifest records its capture-time gate state and remains unchanged. No final human acceptance, new commit/push, merge or deployment approval has been granted.


## Final human approval — 2026-10-11

Project owner: “i approve”, following the completed Slice 0 implementation report. Scope: final acceptance/completion of Slice 0 revision 1 / BL-S0-R1, REQ-S0-001 through REQ-S0-007 and DEC-S0-001 through DEC-S0-004, after builder verification and fresh-context I1 technical review PASS. Result: accepted; Slice 0 complete. Evidence is recorded in verification/slice-0-results.md, verification/slice-0-review.md and verification/slice-0-evidence.json. This approval does not authorize a new commit, push, merge or deployment; APR-PUSH-001 only covered the earlier checkpoint.


## Slice 0 publication authorization — 2026-10-11

Project owner explicitly authorized: review current changes, preserve unrelated work, commit the approved Slice 0 implementation and evidence, and push to `origin/main`; do not merge or deploy. This later, scoped instruction authorizes one commit/push of the reviewed Slice 0 change set. It does not authorize other work or deployment.


## GATE-FDP-IMPLEMENT-DELEGATED — 2026-10-15

Distinct implementation authorization under DIR-FDP-OWNER / DIR-DELEGATED-LOOP-003 after VER-FDP-REQ-001 separate PASS, GATE-FDP-REQ-DELEGATED, immutable BL-FDP-R1 SHA256 e6a29a8cd78ea9f3365079482c123509c059d91b69a690fbf35b97e72827f232, and issue34 creation/readback https://github.com/anonymnd/procureflow-ai/issues/34. Authorizes bounded frontend status/progress, explicit guarded production demo runner and marker migration, real isolated DB/browser proof, docs/trace, separate implementation review/corrections. Current owner separately authorizes scoped commit/push/deploy and this production fixture after required review; no general auth bypass, real-data mutation/reset, paid resources or new provider. Preserve main dirty tree/index and existing DBs; publish only exact reviewed delta on current live f61fcdb base.


## DIR-FDP-OWNER / GATE-FDP-REQ-DELEGATED — 2026-10-15

Owner explicitly requests fast YAGNI planning/comparison/code/deployment and production demo users/RFQs/quotations/orders, bypass onboarding only for these fixtures; email deferred. This authorizes scoped production publication/seeding on existing Free/Hobby providers, not general authentication bypass or unrelated data changes. Separate requirement review VER-FDP-REQ-001 PASS precedes this delegated requirement approval of REQ-FDP-001..003@R1, SHA256 e6a29a8cd78ea9f3365079482c123509c059d91b69a690fbf35b97e72827f232. Narrow marker table justified for edit-preserving atomic idempotence; no applied migration edit. Freeze BL-FDP-R1, create meaningful issue, then distinct implementation gate. All gates delegated, not direct owner review.


## GATE-FDP-FINAL-DELEGATED — development acceptance — 2026-10-16

Under DIR-FDP-OWNER / DIR-DELEGATED-LOOP-003, record distinct delegated development acceptance of BL-FDP-R1 / issue34 after separate VER-FDP-IMPL-001 final PASS. Eleven scoped release paths, seven focused fixture tests, browser proof and final JAR/static/source bindings inspected. Original regression118 had one outdated schema allowlist failure; corrected isolated schema test1 passed, remaining checks unchanged. This is delegated acceptance, not direct owner technical review or hosted success.


## GATE-FDP-PUBLISH-DELEGATED — scoped publication execution — 2026-10-16

Distinct publication gate under direct DIR-FDP-OWNER and owner's continuation instructing finish production without interruptions, after separate review PASS and distinct development acceptance. Authorizes committing/pushing only reviewed delta from exact f61fcdb to codex/fsb-demo-production, backend exact-commit deployment on existing Render Free, then frontend promotion on existing Vercel Hobby after backend readiness. Saved existing demo settings observed exact target procureflow/enable true/valid strong credential; no new credential entered in continuation. Authorizes deliberate transaction fixture insertion and normal demo-only account sign-in/read proof, followed by disabling temporary seed flag/removing temporary password setting. No real account/order mutation, paid resources, new provider, auth bypass, destructive migration or unrelated dirty work publication. Hosted verification/separate review/acceptance remain required.
