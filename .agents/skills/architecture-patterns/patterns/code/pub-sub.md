# Pub/Sub

## One-liner
Producers publish to topics; subscribers subscribe to topics; they do not know each other.

## Symptoms
- The producer must know the consumers.
- Cross-module coupling through direct calls.
- Adding a consumer changes the producer.

## Solution
- A broker (or in-process topic registry).
- Topics are names, not objects.
- Producers publish to a topic; subscribers receive from it.
- Delivery semantics are chosen per topic (at most once, at least once, exactly once).

## When to use
- Modules must react without knowing each other.
- The number of consumers grows.
- Instead of: direct cross-module calls.

## When NOT to use
- A single consumer (a direct call).
- A response is required (that is a synchronous call).
- The topic is a one-off event with no future consumer.

## Trade-offs
- vs direct call: decoupling vs a readable flow.
- vs observer: pub/sub crosses modules, observer is one object scope.
- vs event-driven: pub/sub is the mechanism, event-driven is the architecture.

## Common pitfalls
- A topic with one subscriber that is also the producer (a roundabout).
- Delivery semantics not chosen (duplicates or losses discovered in production).
- Topics named after the producer (they couple the vocabulary).

## Related
- patterns/code/observer.md — the local version.
- patterns/code/mediator.md — when the coordination is a web, not a broadcast.
- patterns/component/event-driven.md — when pub/sub becomes the system architecture.
