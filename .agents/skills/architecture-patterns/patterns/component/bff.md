# BFF (Backend for Frontend)

## One-liner
A backend shaped for one client (web, mobile, IoT), not a generic API for all.

## Symptoms
- Web and mobile need different slices of the same data.
- Over-fetching.
- The client assembles many calls where one is enough.

## Solution
- One BFF per client type.
- The BFF aggregates domain services and returns the client's shape.
- The domain services stay generic.
- The BFF owns the client's caching and pagination.

## When to use
- Several clients with different needs.
- The clients over-fetch or under-fetch.
- Instead of: a generic API the client adapts.

## When NOT to use
- A single client.
- The clients need the same shape.
- The BFF would just rename fields.

## Trade-offs
- vs a generic API: a perfect shape per client vs one API.
- vs a GraphQL gateway: explicit aggregation vs query-driven.
- vs client-side aggregation: server-side vs client-side.

## Common pitfalls
- A BFF that duplicates domain logic (it aggregates, not decides).
- Two BFFs for the same client (a split brain).
- The BFF becoming a second domain (it is a view, not a source).

## Related
- patterns/code/dto.md — the shape the BFF produces.
- patterns/component/event-driven.md — when the BFF reacts to domain events.
- patterns/system/cqrs.md — when reads get their own model.
