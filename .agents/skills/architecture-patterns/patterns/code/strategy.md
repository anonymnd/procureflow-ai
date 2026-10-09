# Strategy

## One-liner
Encapsulates interchangeable algorithms so behavior can be swapped at runtime without touching the caller.

## Symptoms
- if/else or switch on a "type"/"variant" field.
- Adding a new variant edits existing code.
- Behavior is chosen at runtime (user input, config, environment).
- The same branching is repeated in several places.

## Solution
- An interface for the algorithm: one method, the operation.
- Each algorithm is a separate object implementing the interface.
- The context holds a reference to one instance and delegates the call.
- Selection (which algorithm) is done by a factory method or dependency injection.
- The caller depends on the interface, not on concrete classes.

## When to use
- Three or more variants are expected to grow.
- Behavior is chosen at runtime and may differ per instance.
- Instead of: if/else, enum+switch, or parallel class hierarchies.

## When NOT to use
- Two variants with stable logic: if/else is enough.
- Variants are a closed set known at compile time: enum + switch.
- The variants are one-liners: the indirection costs more than it saves.

## Trade-offs
- vs if/else: indirection and extra objects vs extensibility.
- vs enum+switch: runtime swapping vs static exhaustiveness.
- vs state: strategy is stateless; state remembers transitions (see state).

## Common pitfalls
- Strategy objects that hold shared mutable state (breaks thread safety).
- Creating a new strategy instance per call when it is stateless.
- Naming the interface after the pattern, not after what it does.

## Related
- patterns/code/factory-method.md — when the selection logic itself is complex.
- patterns/code/dependency-injection.md — when the choice is a wiring decision.
- patterns/code/state.md — when behavior depends on history, not just type.
