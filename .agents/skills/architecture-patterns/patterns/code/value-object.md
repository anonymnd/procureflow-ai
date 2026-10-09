# Value Object

## One-liner
A thing identified by its attributes (value), not by identity; immutable.

## Symptoms
- An "entity" that has no lifecycle.
- Equality should be by value, not by id.
- Shared mutable data (address, money, period).
- Accidental mutation of shared data.

## Solution
- An immutable value object.
- Equality by fields.
- Construction via a factory (with validation).
- The VO validates its own invariants.
- VOs are nested in entities/aggregates.

## When to use
- The thing has no identity of its own.
- Two instances with the same data are the same thing.
- Instead of: mutable "entities" for values.

## When NOT to use
- There is a real lifecycle (then entity).
- The value is a primitive (a string is enough).
- The thing must be replaced in place (that is identity).

## Trade-offs
- vs entity: identity vs value.
- vs plain struct/POJO: invariants and equality.
- vs DTO: a VO is a domain concept, a DTO is a boundary shape.

## Common pitfalls
- A mutable VO (immutability is the point).
- An ID field on a VO (that is identity, not value).
- Comparing VOs by reference instead of by fields.

## Related
- patterns/code/aggregate.md — where VOs live.
- patterns/code/factory-method.md — how VOs are constructed.
- patterns/code/dto.md — when the same shape crosses a boundary.
