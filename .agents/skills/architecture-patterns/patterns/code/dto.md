# DTO

## One-liner
A dedicated data shape for a layer boundary (API, client, storage).

## Symptoms
- Domain entities appear in API responses.
- Clients depend on internal structure.
- Accidental mutation of shared objects.
- Different consumers need different shapes.

## Solution
- A DTO class per boundary.
- Explicit mapping from the domain to the DTO.
- Immutability or controlled mutation.
- DTOs know nothing about the domain.

## When to use
- Data crosses a layer (API, client, another service).
- Consumers need different shapes.
- Instead of: leaking entities.

## When NOT to use
- Internal code where entities are enough (YAGNI).
- A single consumer with the same shape.
- The boundary is already a stable contract.

## Trade-offs
- vs returning entities: decoupling vs duplication.
- vs value object: a DTO is a boundary shape, a VO is a domain concept.
- vs data mapper: a DTO is the shape, the mapper is the mechanism.

## Common pitfalls
- DTOs everywhere, not only at boundaries.
- Behavior put inside a DTO (it is data, not domain).
- Mapping forgotten when the domain shape changes.

## Related
- patterns/code/value-object.md — when the shape is a domain concept.
- patterns/code/data-mapper.md — when the mapping is bidirectional.
- patterns/component/bff.md — when the shape is per client.
