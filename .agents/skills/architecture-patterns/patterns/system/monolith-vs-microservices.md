# Monolith vs Microservices

## One-liner
A decision framework: start as a monolith; split only when the cost is proven, not hypothetical.

## Symptoms
- A decomposition choice at project start.
- A monolith that is hard to change.
- Teams are blocked by each other.
- Scale/deploys are uneven.

## Solution
- Default: a monolith (or a modular monolith).
- Split signals: independent deploy, independent scale, independent ownership, fault isolation.
- Each split is a service with its own data.
- The decision is revisited, not made once.

## When to use
- At project start (choose the default).
- When a monolith shows real split signals.
- Instead of: a premature split or a never-ending monolith.

## When NOT to use
- A small project (the debate is noise).
- The "signals" are hypothetical.
- The team cannot operate several services.

## Trade-offs
- vs microservices: simplicity vs independence.
- vs modular monolith: one deployable vs several.
- vs serverless: services vs functions.

## Common pitfalls
- A distributed monolith (split without independence).
- The decision made once and never revisited.
- Splitting by tech stack, not by domain.

## Related
- patterns/component/microservices.md — the split form.
- patterns/component/modular-monolith.md — the default form.
- patterns/system/cqrs.md — when reads and writes diverge inside a service.
- patterns/system/strangler-fig.md — migrating out of a legacy monolith.
