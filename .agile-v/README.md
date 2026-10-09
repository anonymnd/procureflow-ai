# ProcureFlow requirements and traceability

This lightweight, locally adapted Agile V workflow supports studying and reviewing the project. It does not claim full upstream Agile V conformity. Read [agent rules](../AGENTS.md), [state](STATE.md), [approvals](APPROVALS.md), and [skill provenance](SKILLS.md).

## Delivery loop

1. Inspect source, working tree, build/configuration and actual evidence.
2. Create/update `requirements/slice-N.md`, using stable requirement IDs and a revision.
3. Review ambiguity, contradictions, impact and architectural alternatives; record findings without rewriting human decisions.
4. User approves the specific requirement revision and its normative decision/test-plan/traceability companions. Capture a frozen bundle with a hash for each file before downstream work.
5. Create GitHub Issues from that approved revision, one per meaningful implementation unit. Check existing issues to prevent duplicates; record actual URLs and dependencies.
6. Wait for explicit implementation approval; requirement approval alone is insufficient.
7. Connect actual files/classes, DB/API changes and decisions to requirement/issue IDs.
8. Connect tests to the approved requirements and their revision/baseline.
9. Execute verification and record command, environment, expected/actual result, exit code, reviewer and limitations.
10. Update the slice change map and [code-reading guide](../docs/code-reading-guide.md).

## Records

| Path | Purpose |
|---|---|
| `REQUIREMENTS.md` | Index linking the one authoritative requirement document per slice |
| `requirements/` | Executable requirements and revisioned approved baselines |
| `decisions/` | Slice decisions, alternatives and links to existing planning ADRs |
| `traceability/` | Proposed/actual file map, dependency/DB/API flows and issue/test lineage |
| `tests/` | Requirement-based acceptance and negative/boundary test plans |
| `verification/` | Requirement reviews, observed evidence and pending checks |
| `STATE.md`, `APPROVALS.md` | Durable stage, scope and human gate records |

No application implementation or GitHub Issues have been authorized by the setup approval. Slice 0 is a draft for review. Future slices get records when work begins, rather than empty files now.

## Agent responsibilities and review gate

Codex plans requirements, architecture, task scopes and tests, maintains traceability, prepares issues when authorized, and reviews actual implementation. FCC Claude/Kimi implements explicitly authorized work and applies corrections. Codex writes application code only if the owner explicitly asks Codex to do so. The role directive is recorded as DIR-ROLES-001; details and the required handoff/review format are in [AGENTS.md](../AGENTS.md).

After implementation/tests/traceability/code-reading updates, Codex inspects the actual diff, files and evidence independently of FCC's summary and returns PASS or NEEDS CHANGES. FCC applies corrections and Codex re-reviews before recommending completion. Final human approval remains separate. Reuse the existing slice verification record and issue/conversation for review/handoff; do not add another planning or handoff system.

## Required requirement fields

Each `REQ-SN-NNN` records requirement, rationale, business rule, acceptance criteria, affected actors, related entities, related API behavior, database impact, authorization/security rule, related tests, status and stakeholder source. Include revision/baseline when approved. Distinguish observed behavior from intended behavior.

## Issue body contract

Include slice and approved requirement revision/baseline, related requirement and decision IDs, affected module, acceptance criteria, DB/API/security impact (or none), required test IDs, dependencies and verification evidence expected. Use labels such as `slice-0`, `backend`, `database`, `security`, `test`, `documentation` when meaningful. Do not apply `frontend` to Slice 0. An unavailable GitHub account/permission is a recorded blocker, not an invented issue link.

## Decision contract

Record problem, current evidence, simplest solution, alternatives, trade-offs, overengineering, primary source/production lesson, adaptation to ProcureFlow, failure/verification evidence, revisit trigger, status and approval reference. Connect decisions to requirements and the existing ADRs without changing their confirmed portions.
