# Unit of Work

## One-liner
Tracks all changes and commits them atomically as one unit.

## Symptoms
- Manual "save this, then that".
- Partial commits on failure.
- The same object is saved twice (identity problems).
- Lost updates across entities.

## Solution
- The unit of work registers changed entities.
- commit() applies all changes in one transaction.
- An identity map deduplicates (one instance per entity).
- Rollback on error undoes the whole unit.

## When to use
- One request touches several entities.
- Partial commits must be impossible.
- Instead of: scattered save calls.

## When NOT to use
- One entity per request: a direct save.
- Read-only work.
- The storage does not support transactions.

## Trade-offs
- vs per-entity saves: atomicity vs simplicity.
- vs repository without UoW: the transaction boundary has a home.
- vs manual transactions: change tracking and dedup are automatic.

## Common pitfalls
- Keeping the UoW open for a long time (long transactions).
- Business logic mixed with commit orchestration.
- Saving detached objects without refreshing them.

## Related
- patterns/code/repository.md — the collection the unit commits.
- patterns/code/aggregate.md — the consistency unit it protects.
- patterns/code/value-object.md — the values inside the entities.
