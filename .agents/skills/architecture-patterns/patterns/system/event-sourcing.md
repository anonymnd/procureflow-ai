# Event Sourcing

## One-liner
The state is a function of the events: store the events, rebuild the state.

## Symptoms
- An audit of changes is required.
- Temporal queries ("what was the state in March?").
- State must be rebuildable.
- Complex invariants over time.

## Solution
- Events are the source of truth (facts, past tense).
- The state is a projection over the event stream.
- Each event is immutable and ordered.
- The projection is rebuilt by replaying the stream.

## When to use
- The history of changes is a product requirement.
- Audit and temporal queries matter.
- Instead of: a mutable state with an audit log.

## When NOT to use
- You do not need history.
- The domain is simple CRUD.
- The team will not maintain the projection.

## Trade-offs
- vs CRUD: events as truth vs state as truth.
- vs audit log: the log is the source vs the log is a copy.
- vs CQRS: ES provides the history, CQRS splits the models.

## Common pitfalls
- Events that are really state snapshots (the stream is a log of states).
- A projection that is never replayed (drift).
- An event stream that is not ordered (replay is undefined).

## Related
- patterns/system/cqrs.md — the read side this feeds.
- patterns/system/saga.md — when events coordinate long processes.
- patterns/system/outbox.md — publishing events reliably.
- patterns/code/command.md — the commands that produce events.
