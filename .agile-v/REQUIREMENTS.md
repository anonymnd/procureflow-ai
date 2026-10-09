# Requirement index

This is an index, not a second specification. Follow the authoritative slice link.

| Slice | Authoritative document | Revision | Status | Approved baseline |
|---|---|---|---|---|
| 0 - Trustworthy development baseline | [slice-0.md](requirements/slice-0.md) | 1 | Draft persisted; awaiting review/approval | None |

High-level scope remains in [living plan](../docs/planning/living-plan.md), [confirmed decisions](../docs/planning/decisions.md), [roadmap](../docs/planning/vertical-slices.md) and [planning ADRs](../docs/planning/architecture-decisions.md). No later slice requirement is approved merely because it appears in the roadmap.

On approval, preserve a revisioned bundle under `requirements/baselines/slice-0-r1/`: this requirement document plus its normative decision, test-plan and traceability companions. Record every file's SHA-256, original path and approval reference in a manifest, and use that exact bundle for issue creation, implementation and verification. Material requirement or acceptance changes require a new revision and approval; never edit a frozen baseline in place. Later live records may append implementation/results without altering the frozen acceptance bundle.
