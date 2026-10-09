# Data Mapper

## One-liner
Separates the domain model from its storage form: explicit mapping model to storage.

## Symptoms
- The model knows about tables/columns.
- Persistence details live in the domain.
- Model == table and any change ripples.
- Storage formats leak into business code.

## Solution
- A mapper per aggregate: model to storage form and back.
- The model has no persistence knowledge.
- Mapping is explicit (no magic reflection).
- Storage formats live only in the mapper.

## When to use
- The model and the storage shape differ.
- The storage format may change.
- Instead of: persistence annotations on the model.

## When NOT to use
- The model truly is the table (active record is acceptable).
- Trivial one-to-one mapping with no transforms.
- A read-only projection (a DTO is simpler).

## Trade-offs
- vs repository: the mapper is mapping only, the repository is the API.
- vs active record: a clean model vs pragmatism.
- vs DTO: the mapper is bidirectional, the DTO is one-way.

## Common pitfalls
- The mapper knowing business rules (it maps, nothing else).
- Mapping duplicated across call sites.
- Storage types leaking into the model.

## Related
- patterns/code/repository.md — the API side of the boundary.
- patterns/code/value-object.md — the model the mapper protects.
- patterns/code/dto.md — when only one direction is needed.
