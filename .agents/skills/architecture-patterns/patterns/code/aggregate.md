# Aggregate

## One-liner
A consistency boundary: a cluster of entities that changes together under invariants.

## Symptoms
- Invariants span several entities.
- Partial updates / inconsistent state.
- Concurrent writes to the same data.
- "Which entity owns this rule?" is unclear.

## Solution
- An aggregate root as the only entry point.
- Invariants are enforced inside the boundary.
- Other aggregates are referenced by ID.
- The aggregate is loaded and saved as a whole.

## When to use
- Several entities share invariants.
- Writes must keep the cluster consistent.
- Instead of: cross-entity saves without a rule.

## When NOT to use
- A small domain with no cross-entity invariants.
- Reads dominate (projections are enough).
- The "cluster" is two fields (a value object is enough).

## Trade-offs
- vs per-entity saves: invariants vs granularity.
- vs distributed transactions: a boundary vs a service.
- vs CQRS: a write boundary vs read projections.

## Common pitfalls
- A huge aggregate (boundaries should stay small).
- Direct references to other aggregates (IDs only).
- Mutating the aggregate root's children directly (bypasses invariants).

## Related
- patterns/code/value-object.md — the values inside the boundary.
- patterns/code/repository.md — how the aggregate is loaded and saved.
- patterns/code/unit-of-work.md — the commit unit it protects.
- patterns/system/cqrs.md — when reads get their own model.
