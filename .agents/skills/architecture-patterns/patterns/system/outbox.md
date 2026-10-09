# Outbox

## One-liner
Write the event to a table in the same transaction as the data; a relay publishes it.

## Symptoms
- A database write plus a message publish must be atomic.
- Events lost on crash.
- Duplicated/lost events.

## Solution
- An outbox table in the same database.
- The data write and the event insert are one transaction.
- A relay (poller or trigger) publishes the outbox to the broker.
- The relay deduplicates and tracks published events.

## When to use
- "Persist + publish" must be atomic.
- Lost events are unacceptable.
- Instead of: a two-phase commit or a best-effort publish.

## When NOT to use
- The message is optional.
- At-least-once is acceptable and dedup is cheap.
- There is no broker (then a local event log).

## Trade-offs
- vs direct publish: atomicity vs latency.
- vs two-phase commit: a single DB vs a distributed transaction.
- vs event sourcing: the outbox is a transport, ES is a model.

## Common pitfalls
- The relay that publishes twice (no dedup).
- The outbox table that is never cleaned (unbounded growth).
- Publishing the event before the transaction commits.

## Related
- patterns/system/event-sourcing.md — when the events are the source of truth.
- patterns/component/event-driven.md — the architecture the outbox serves.
- patterns/system/idempotency.md — the consumer side of at-least-once.
- patterns/system/saga.md — when the events coordinate a process.
