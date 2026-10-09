# CQRS

## One-liner
Separate the write model from the read model: two models for two different jobs.

## Symptoms
- Complex write invariants.
- Divergent read projections.
- Reads and writes scale differently.
- One model does not fit both.

## Solution
- A write model (commands -> state) with invariants.
- A read model (queries -> projections) shaped for the UI.
- The two models are connected by events or synchronization.
- Each model is optimized for its own load.

## When to use
- The write invariants are genuinely complex.
- The read shape diverges from the write shape.
- Instead of: one model doing two jobs.

## When NOT to use
- Simple CRUD (two models for one table).
- The read and write shapes are the same.
- The team cannot own both models.

## Trade-offs
- vs a single model: two models vs one.
- vs projections: CQRS is the whole split, a projection is one side.
- vs event sourcing: CQRS needs no history, ES provides it.

## Common pitfalls
- CQRS as a slogan (two models, no real divergence).
- The read model becoming the source of truth.
- Synchronization between the models that is never tested.

## Related
- patterns/code/aggregate.md — the write side's boundary.
- patterns/system/event-sourcing.md — the history behind the split.
- patterns/code/cache-aside.md — a lighter version of the read side.
- patterns/component/bff.md — when the read shape is per client.
