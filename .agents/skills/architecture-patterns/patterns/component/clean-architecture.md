# Clean Architecture

## One-liner
Code is organized in layers by business distance; dependencies point inward.

## Symptoms
- The domain depends on the UI/DB.
- Layers are unclear; "where does this code go?" is a recurring question.
- The framework leaks into business logic.

## Solution
- Entities (pure domain), use cases (application), interface adapters, frameworks.
- Dependencies point only inward.
- Each layer defines interfaces the outer layer implements.
- Business rules live only in the inner layers.

## When to use
- The domain must outlive the framework.
- Several teams need a shared layout.
- Instead of: a framework-first or layer-by-convenience layout.

## When NOT to use
- A small project (one layer is enough).
- The domain is trivial.
- The team will not follow the rule.

## Trade-offs
- vs hexagonal: layers vs ports (the same core idea).
- vs layered: a strict rule vs a simple split.
- vs framework-first: the domain leads vs the framework leads.

## Common pitfalls
- The "use case" layer becoming a god service.
- Entities depending on the use cases (inward violation).
- The rule ignored "for this one module".

## Related
- patterns/component/hexagonal.md — the ports-and-adapters version.
- patterns/component/layered.md — the simpler middle ground.
- patterns/component/modular-monolith.md — when the app outgrows one module.
