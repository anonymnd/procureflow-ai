# Template Method

## One-liner
Fixes the skeleton of an algorithm; subclasses fill in individual steps.

## Symptoms
- The same algorithm in several places with small step differences.
- Variants differ in 1-2 steps.
- The order of steps must be preserved.

## Solution
- An abstract class (or base function) defines the fixed order.
- Steps that vary are hooks (overridden or injected).
- The skeleton is final; only steps vary.
- Each step is a named unit.

## When to use
- The order is stable, the steps vary.
- Several variants share most of the steps.
- Instead of: copy-pasted algorithms with edits.

## When NOT to use
- The differences are large (then strategy).
- One variant with no second (no abstraction).
- The order itself may change.

## Trade-offs
- vs strategy: shared skeleton with small differences vs interchangeable algorithms.
- vs inheritance: hooks vs a full hierarchy.
- vs function injection (closures): class-level vs function-level.

## Common pitfalls
- A template method where every step differs (no reuse left).
- Subclasses that reorder steps (the skeleton is final).
- A "hook" that is always empty (dead abstraction).

## Related
- patterns/code/strategy.md — when the variants do not share a skeleton.
- patterns/code/factory-method.md — when creating the variant is also a question.
- patterns/code/decorator.md — when steps are optional add-ons.
