# Microservices

## One-liner
The system is a set of small, independently deployable services, each owning its data.

## Symptoms
- Teams cannot ship independently.
- A deploy bottleneck.
- Scale by part.
- The monolith is a deploy risk.

## Solution
- One service per bounded context (one team, one deploy).
- Each service owns its database (database per service).
- Services communicate via APIs or events.
- A service is small enough to be understood by its team.

## When to use
- Several teams ship independently.
- Scale/availability differs by area.
- The monolith is a real bottleneck, not a hypothetical one.

## When NOT to use
- One team.
- A small domain.
- The team does not yet have a modular monolith.

## Trade-offs
- vs monolith: independence vs operational cost.
- vs modular monolith: distributed boundaries vs in-process boundaries.
- vs serverless: long-lived services vs functions.

## Common pitfalls
- Distributed monolith (services that call each other synchronously in a chain).
- A shared database between services (the boundary is a lie).
- A service per table, not per context.

## Related
- patterns/component/modular-monolith.md — the stage before this.
- patterns/component/event-driven.md — the typical communication style.
- patterns/system/saga.md — transactions across services.
- patterns/system/circuit-breaker.md — isolating failures between services.
