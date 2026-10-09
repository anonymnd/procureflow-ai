# Hexagonal Architecture

## One-liner
The application core is surrounded by adapters; the core depends on nothing, everything depends on the core.

## Symptoms
- Domain code imports framework/DB/API specifics.
- The core cannot be tested without infrastructure.
- Swapping the database or API means a rewrite.
- The framework dictates the structure.

## Solution
- Ports are interfaces defined by the core (inbound and outbound).
- Adapters implement ports (drivers: web, CLI; driven: DB, API, queue).
- The core contains only domain and application logic.
- Dependencies point inward, always.

## When to use
- The core must be testable without infrastructure.
- The storage or transport may change.
- Instead of: a framework-first structure.

## When NOT to use
- A small app where the framework is the app.
- One storage that will never change.
- A script, not an application.

## Trade-offs
- vs layered: ports and adapters vs fixed layers.
- vs clean architecture: the same idea, different vocabulary (ports vs layers).
- vs framework-first: the core is independent vs the framework leads.

## Common pitfalls
- The domain importing the framework "just for the exception type".
- An adapter that contains business rules.
- Ports defined by the adapter, not by the core.

## Related
- patterns/component/clean-architecture.md — the same dependency rule, layered vocabulary.
- patterns/component/layered.md — when a simple layering is enough.
- patterns/code/dependency-injection.md — how the adapters reach the core.
- patterns/code/repository.md — a typical outbound port.
