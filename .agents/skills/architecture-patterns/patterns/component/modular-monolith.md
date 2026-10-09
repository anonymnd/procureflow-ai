# Modular Monolith

## One-liner
One deployable, many modules with strict boundaries; the cost of a microservice without the distribution.

## Symptoms
- A "big ball of mud".
- Modules touch each other's internals.
- One change breaks everything.
- Deploy coupling; no clear ownership.

## Solution
- One deployable, several modules.
- Each module has a public API (the rest is private).
- Modules talk only through the public API or events.
- Boundaries are enforced (lint, reviews), not by convention alone.

## When to use
- The domain has 3+ clear areas.
- The team grows; ownership matters.
- Instead of: a ball of mud or premature microservices.

## When NOT to use
- A small app (one module is enough).
- The team is one person and the code is small.
- The modules do not actually have boundaries.

## Trade-offs
- vs microservices: one deployable vs independent deploys.
- vs layered: modules by domain vs layers by role.
- vs flat: boundaries vs fewer objects.

## Common pitfalls
- Modules that "borrow" each other's internals (the boundary is a lie).
- A module that is really a bag of helpers (no domain).
- Boundaries without enforcement (they decay).

## Related
- patterns/component/microservices.md — when the module becomes a service.
- patterns/component/event-driven.md — how modules decouple.
- patterns/component/hexagonal.md — the shape of a single module.
