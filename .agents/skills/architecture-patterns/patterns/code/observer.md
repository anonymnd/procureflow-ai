# Observer

## One-liner
Many independent subscribers react to one event without the publisher knowing them.

## Symptoms
- "When A changes, update B, C, D".
- A accumulates knowledge about B, C, D.
- Adding a consumer changes A.
- The same notification is duplicated in several places.

## Solution
- A subject with subscribe/unsubscribe and a publish step.
- Subscribers implement a handler interface (on event).
- The subject knows nothing about the subscribers.
- Unsubscribe is mandatory: a subscription is a resource.
- Notification order is not guaranteed unless specified.

## When to use
- Two or more consumers, likely to grow.
- Consumers are independent and do not block each other.
- Instead of: a list of concrete consumers inside the publisher.

## When NOT to use
- A single consumer: a direct call is clearer.
- Ordering between consumers matters: observer does not guarantee it.
- Consumers need a response from the subject: that is a call, not an event.

## Trade-offs
- vs direct call: decoupling vs a readable call graph.
- vs pub-sub: local scope in one process vs topics and delivery semantics.
- vs event-driven: object-level coupling vs system-level architecture.

## Common pitfalls
- Subscribers never unsubscribed (leaks).
- A slow subscriber blocks the publisher (consider async).
- Publishing during construction, when state is undefined.

## Related
- patterns/code/pub-sub.md — when the boundary is a module, not an object.
- patterns/code/mediator.md — when the interaction is a web of calls, not a broadcast.
- patterns/component/event-driven.md — when this becomes a system architecture.
