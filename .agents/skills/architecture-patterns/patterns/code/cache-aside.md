# Cache-Aside

## One-liner
The client checks the cache first; on a miss it reads the source and fills the cache.

## Symptoms
- Repeated expensive fetches (database, API).
- N+1 queries.
- TTL/invalidation questions.
- The data changes rarely.

## Solution
- On read: cache first, source on miss.
- On write: update the source, then invalidate the cache.
- A TTL as a safety net.
- The cache is a layer, not the source of truth.

## When to use
- Reads dominate and the data is relatively static.
- The source is expensive.
- Instead of: hitting the source on every read.

## When NOT to use
- The data is cheap (the cache is overhead).
- Strong consistency is required (the cache is stale by definition).
- Writes dominate (invalidation dominates).

## Trade-offs
- vs write-through: invalidation vs synchronous cache updates.
- vs source of truth: the cache is a view, not the data.
- vs memoization: external data vs pure functions.

## Common pitfalls
- Writing to the source and forgetting to invalidate (stale reads).
- A TTL that is both too long and too short (tuning by guessing).
- Treating the cache as the source of truth.

## Related
- patterns/code/memoization.md — when the "data" is a computed result.
- patterns/system/retry-with-backoff.md — when the source is a flaky external.
- patterns/system/cqrs.md — when reads get their own store.
