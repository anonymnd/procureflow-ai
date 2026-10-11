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
