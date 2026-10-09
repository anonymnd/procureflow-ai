# Saga

## One-liner
A long process as a sequence of local transactions, each with a compensation.

## Symptoms
- A process spans 3+ services.
- Failure requires compensation.
- A multi-step or multi-day process.

## Solution
- A saga is a list of steps, each a local transaction.
- Each step has a compensation (an undo).
- On failure, the compensations run in reverse.
- Orchestrated (a coordinator) or choreographed (events).

## When to use
- A process spans several services.
- A distributed transaction is not available.
- Instead of: a two-phase commit.

## When NOT to use
- One service (a local transaction is enough).
- The process cannot be compensated.
- The process is a single request.

## Trade-offs
- vs a distributed transaction: compensation vs atomicity.
- vs orchestrator: a coordinator vs events (visibility vs coupling).
- vs retry: a saga compensates, a retry repeats.

## Common pitfalls
- A step without a compensation (the saga is broken).
- An orchestrator that knows all services' internals.
- A saga that is really a long-running request (it is a process).

## Related
- patterns/system/event-sourcing.md — when the saga's steps are events.
- patterns/component/microservices.md — where sagas live.
- patterns/system/idempotency.md — the steps must be re-executable.
- patterns/system/outbox.md — publishing the saga's events reliably.
