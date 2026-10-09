# Specification

## One-liner
Composable, reusable query criteria: conditions as composable objects.

## Symptoms
- The same WHERE/conditions are duplicated.
- "findXWhereY" methods are proliferating.
- Conditions are combined ad hoc.
- Criteria differ slightly per call.

## Solution
- A specification object: a predicate with and/or/not composition.
- The repository or query layer evaluates the specification.
- Criteria are reusable and testable.
- Combinations are declared, not rewritten.

## When to use
- The same criteria with variations.
- Criteria are combined (and/or/not).
- Instead of: findXWhereY methods.

## When NOT to use
- 1-2 queries: plain conditions.
- Static and few criteria.
- The query is a single fixed report.

## Trade-offs
- vs raw conditions: reusability vs readability at call sites.
- vs query DTO: a specification is composable, a DTO is flat.
- vs query builder library: a specification is explicit objects.

## Common pitfalls
- A specification for a one-off query.
- Side effects inside the predicate.
- Over-combining (a spec is as unreadable as findXWhereY when huge).

## Related
- patterns/code/repository.md — where specifications are evaluated.
- patterns/code/value-object.md — when a criterion is a domain value.
- patterns/code/dto.md — when the query shape is fixed.
