# Strangler Fig

## One-liner
Replace a legacy system piece by piece: the new system grows around the old one until the old one is dead.

## Symptoms
- A legacy system must be replaced.
- A big-bang rewrite is too risky.
- The legacy must keep working during migration.

## Solution
- A facade in front of the legacy system.
- The new system handles one feature at a time.
- The facade routes: handled -> new, not yet -> legacy.
- The legacy is read-only; the new system owns its data.

## When to use
- The legacy must keep working during the migration.
- The rewrite is too big for one go.
- Instead of: a big-bang rewrite.

## When NOT to use
- A small legacy (a rewrite is enough).
- The legacy is already dead (no traffic).
- The migration is a one-off, not a process.

## Trade-offs
- vs a rewrite: a long migration vs a short outage.
- vs a parallel run: the strangler is incremental, the parallel is a race.
- vs a feature flag: the strangler is a migration, the flag is a rollout.

## Common pitfalls
- The facade that becomes a second legacy (it is not migrated).
- A feature that is migrated and then unmigrated (the facade is not removed).
- The new system that depends on the legacy's internals (the boundary is a lie).

## Related
- patterns/component/microservices.md — the end state of a strangler.
- patterns/system/monolith-vs-microservices.md — the decision the strangler serves.
- patterns/component/event-driven.md — how the new and the old coexist.
- patterns/code/adapter.md — the facade is an adapter over the legacy.
