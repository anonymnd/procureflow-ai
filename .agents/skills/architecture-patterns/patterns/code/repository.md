# Repository

## One-liner
An abstraction over a collection of objects: the domain works with a collection, not a database.

## Symptoms
- Data access calls live inside business code.
- Business logic cannot be tested without the database.
- Storage may need to be swapped.
- Queries are scattered and duplicated.

## Solution
- A repository per aggregate, not per table.
- A query API: find, list, criteria.
- The implementation hides ORM/SQL.
- The repository does not leak persistence types to the domain.

## When to use
- The domain must not know the storage.
- The storage may change (database, API, files).
- Instead of: SQL calls in services.

## When NOT to use
- Simple CRUD over a single table: direct ORM calls.
- Ephemeral in-memory data.
- The "domain" is a thin script.

## Trade-offs
- vs direct ORM: a boundary vs pragmatism.
- vs data mapper: a repository is a thicker collection API.
- vs active record: a clean model vs a pragmatic model.

## Common pitfalls
- One repository per table instead of per aggregate.
- ORM/SQL types leaking into the domain.
- A repository used for a one-off report (a query is enough).

## Related
- patterns/code/unit-of-work.md — where the transaction boundary lives.
- patterns/code/data-mapper.md — the mapping half of the boundary.
- patterns/code/aggregate.md — what a repository collects.
- patterns/component/clean-architecture.md — where the repository sits.
