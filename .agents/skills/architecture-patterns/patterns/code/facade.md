# Facade

## One-liner
A simplified interface over a complex subsystem: one door, not a map of rooms.

## Symptoms
- The client uses several classes for one operation.
- The subsystem API is wide.
- The client must know subsystem internals.

## Solution
- A facade exposes one or a few operations.
- Internally it orchestrates the subsystem.
- The client depends only on the facade.
- The facade adds no business rules (it delegates).

## When to use
- The subsystem is genuinely complex.
- The client needs one operation, not a tour.
- Instead of: the client walking the subsystem API.

## When NOT to use
- The subsystem is simple (the facade is ceremony).
- The client needs fine-grained control (facade hides it).
- The facade would just rename methods.

## Trade-offs
- vs direct API: simplicity vs flexibility.
- vs adapter: facade simplifies your own subsystem, adapter translates someone else's.
- vs mediator: facade is one call direction, mediator centralizes interactions.

## Common pitfalls
- A facade that adds logic (then it is a service, not a facade).
- A facade per client for the same subsystem (duplicates).
- Hiding operations the client actually needs.

## Related
- patterns/code/adapter.md — external interface, not your subsystem.
- patterns/code/mediator.md — centralizing interactions between objects.
- patterns/component/hexagonal.md — the facade is often an application entry point.
