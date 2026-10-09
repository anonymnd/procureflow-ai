# Event-Driven Architecture

## One-liner
Components communicate by events, not calls; a producer publishes, consumers react.

## Symptoms
- Synchronous call chains between modules.
- Cascading failures.
- Latency under load.
- A module must react to many things.

## Solution
- An event bus (in-process or a broker).
- Events are facts ("order.placed"), not commands ("place.order").
- Producers do not know the consumers.
- Each consumer is independently deployed/scaled.

## When to use
- Modules must react without a call chain.
- Failure isolation matters.
- Instead of: a synchronous call graph.

## When NOT to use
- A simple request-response.
- One module (events are overhead).
- Strong consistency is required for every step.

## Trade-offs
- vs synchronous calls: decoupling vs a readable flow.
- vs observer: system scope vs object scope.
- vs microservices: a communication style vs a deployment model.

## Common pitfalls
- Events that are really commands (consumers must do something).
- An event storm (every state change is an event).
- No ordering guarantee where it matters.

## Related
- patterns/code/pub-sub.md — the mechanism this is built on.
- patterns/code/observer.md — the local version.
- patterns/system/outbox.md — publishing reliably from a database.
- patterns/system/saga.md — when events coordinate a long process.
