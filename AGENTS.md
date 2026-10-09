# ProcureFlow agent workflow

Read `.agile-v/STATE.md`, `.agile-v/APPROVALS.md`, the current slice requirement, and `docs/planning/decisions.md` before acting. Existing product decisions remain authoritative; proposed ADRs are not approved implementation designs.

## Authorization

- Skill installation and lightweight workflow setup were approved on 2026-10-09. Slice 0 application implementation is NOT approved.
- Inspect the actual repository first. Preserve uncommitted work and existing databases/volumes. Never reset, overwrite unrelated changes, edit applied migrations, or use `docker compose down -v`.
- Persist a slice requirement, review ambiguities/contradictions, and present it to the user. Requirement approval, GitHub Issue creation, and implementation authorization are distinct states.
- After explicit approval of a particular requirement revision and its normative companions, capture their SHA-256 hashes and immutable baseline bundle, then create meaningful GitHub Issues automatically under the user's standing instruction. Approval of tooling, a roadmap, or this workflow is not approval of a slice requirement.
- Implement only after explicit implementation approval. Scope changes affecting approved behavior require a new revision and review.
- Do not invent approvals, issue URLs, test passes, or independent verification. Record unavailable evidence explicitly.

## Lightweight Agile V profile

Use the installed skill instructions for the relevant stage, subject to the user's instructions and this local profile. This is a deliberately adapted workflow, not full upstream Agile V conformity or a certification claim. Do not edit downloaded skills to enforce the adaptation.

- `.agile-v/REQUIREMENTS.md` is an index; `.agile-v/requirements/slice-N.md` is the authoritative slice document. Do not maintain duplicate specifications under `specs/`.
- Consolidate system understanding into the slice current-state section; impact analysis into `.agile-v/traceability/slice-N.md`; decisions into `.agile-v/decisions/slice-N.md`; tests into `.agile-v/tests/slice-N-test-plan.md`; reviews/results into `.agile-v/verification/`.
- `STATE.md` holds the current stage and pending gate; `APPROVALS.md` records the user's actual scoped approvals. No generated tokens, checkpoint system, control-matrix framework, AI-BOM framework, release evaluation framework, graph tooling, or regulated templates are required for this learning project. Record source revisions, actual evidence, and AI authorship directly in these records.
- Missing knowledge graphs are recorded and replaced with direct code inspection; they do not require installing Understand Anything.
- Draft test proposals can accompany the executable specification. Formal test design uses the approved, frozen requirements and referenced constraints, without inheriting implementation reasoning.
- A builder's checks are self-checks (I0). A fresh-context review is reported conservatively as I1, with reviewer scope and limits; it is not organizationally independent assurance. Do not silently treat loading another skill in the same context as independence. Use a separate review context when the applicable skill requires it; record pending review if unavailable.
- The spec skill's automatic implementation after approval is overridden by the user's separate implementation gate. Present the persisted, reviewable document and material choices rather than duplicating the entire document in chat.
- Governance/setup records derive from the user's setup directive, not fabricated approved application REQ IDs. Draft impact/test artifacts reference draft requirements and remain proposals until baseline approval.

## Architecture decisions

Before proposing a pattern or infrastructure, answer: (1) what problem it solves, (2) evidence ProcureFlow has that problem, (3) simplest production-ready solution, (4) alternatives/trade-offs, and (5) what is overengineering now. Add a primary production/framework source where useful, distinguish its lesson from our proposed adaptation, and give a revisit trigger. A company name is not evidence of suitability.

Retain feature-oriented Spring Boot modules, explicit API DTOs, constructor injection, Flyway-managed PostgreSQL, and the modular-monolith direction. Verify generic Spring skill guidance against the actual framework version. New libraries, abstractions, Kafka, Redis, Kubernetes, microservices, CQRS infrastructure, Outbox, and similar additions require a current requirement and explicit justification/approval.

## Traceability and study notes

For every slice maintain `Requirement@revision/baseline -> Decision -> Issue -> Files/classes -> DB/API -> Tests -> Verification`. Use `REQ-SN-001`, `DEC-SN-001`, `TC-SN-001`, and `VER-SN-001` IDs. Issues are grouped by meaningful delivery unit, not tiny files; include requirement IDs, criteria, module, DB/API/security impact, required tests, dependencies and useful existing labels. Record actual issue URLs only after creation, and check for existing matching issues before retrying.

Track exact created/modified/deleted paths, reason, business concept, dependencies, migration, endpoint/controller/service/repository query, DTO/mapper, authorization and tests. Mark proposed vs actual separately. Explain each database relation as `Entity A -> Entity B; cardinality; owning side; FK location; reason`; explicitly state when no relation/FK exists.

Maintain a per-slice feature-to-database architecture map and separate cross-module map. Update `docs/code-reading-guide.md` with before/after behavior, rationale, business rules, exact file changes, reading order, class/entity relations, DB cardinalities/FKs, endpoint flow, authorization and observed tests after each completed slice. Keep a draft study map during specification; do not label it completed.
