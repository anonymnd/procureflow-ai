# Adapter

## One-liner
Translates an external interface into the interface the codebase expects.

## Symptoms
- Integrating an API you cannot change.
- A legacy interface is different from yours.
- Several external services, similar but different.

## Solution
- An adapter implements your interface.
- Internally it calls the foreign API and maps types.
- The caller sees only your interface.
- One adapter per foreign system.

## When to use
- The foreign interface cannot be changed.
- The same foreign system is used in several places.
- Instead of: sprinkling foreign calls everywhere.

## When NOT to use
- You can change the source directly.
- One call site, one-off integration.
- The foreign API is already close to what you need.

## Trade-offs
- vs direct calls: one translation point vs fewer objects.
- vs facade: adapter changes the interface, facade simplifies yours.
- vs wrapper: same idea, adapter emphasizes interface conformance.

## Common pitfalls
- The adapter leaking foreign types into its public API.
- Several adapters for the same foreign system.
- The adapter doing business logic (it translates, nothing else).

## Related
- patterns/code/facade.md — simplifying your own subsystem.
- patterns/code/dto.md — the shapes at the boundary the adapter maps.
- patterns/component/event-driven.md — when the foreign system is event-driven.
