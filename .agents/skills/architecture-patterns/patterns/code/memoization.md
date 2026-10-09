# Memoization

## One-liner
Caches the results of an expensive pure function by its arguments.

## Symptoms
- An expensive pure computation repeats with the same arguments.
- The function is pure (no side effects).
- Arguments are hashable/comparable.

## Solution
- A cache keyed by the arguments.
- On a hit, return the stored result.
- On a miss, compute, store, return.
- An invalidation rule when the function's inputs change.

## When to use
- The computation is expensive and repeated.
- The function is pure.
- Instead of: recomputing.

## When NOT to use
- A cheap computation (the cache overhead wins).
- An impure function (the result depends on time or state).
- Unbounded arguments (memory blow-up).

## Trade-offs
- vs recomputation: memory vs time.
- vs cache-aside: memoization is for pure functions, cache-aside for external data.
- vs caching library: explicit vs drop-in.

## Common pitfalls
- Memoizing an impure function (stale results).
- A cache that never evicts (unbounded growth).
- Arguments that are not comparable (every call is a miss).

## Related
- patterns/code/cache-aside.md — when the data comes from outside.
- patterns/system/retry-with-backoff.md — when the computation is an external call.
- patterns/code/factory-method.md — when the cached thing is constructed, not computed.
