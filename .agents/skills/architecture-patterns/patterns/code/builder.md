# Builder

## One-liner
Stepwise construction of an object with many optional parameters, without telescoping constructors.

## Symptoms
- A constructor with 4+ parameters.
- Telescoping overloads (3-, 5-, 7-argument variants).
- An object with mostly-default fields.
- Configuration via a chain of setters after construction.

## Solution
- A builder with required parameters in the constructor.
- Optional step methods return the builder (fluent).
- build() produces an immutable result.
- Validation lives in build(), not scattered.

## When to use
- Four or more parameters, several optional.
- The object is the result of a configuration process.
- Instead of: telescoping constructors or a bag of setters.

## When NOT to use
- 3 or fewer meaningful parameters: a constructor.
- All parameters are required: a constructor.
- The object is mutable by design: a DTO is simpler.

## Trade-offs
- vs constructor with defaults: stepwise readability vs one expression.
- vs config object: a builder is typed, a config object is a map.
- vs value object: the builder constructs, the VO is the result.

## Common pitfalls
- Builder that accepts anything (god object).
- Validation skipped because build() was never called.
- Builder for 2 parameters (a constructor is enough).

## Related
- patterns/code/dto.md — when the object is a layer-boundary shape.
- patterns/code/factory-method.md — when the family of products varies.
- patterns/code/value-object.md — when the result must be immutable.
