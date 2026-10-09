# Bulkhead

## One-liner
Isolate resources (threads, connections, memory) so one failure does not exhaust the rest.

## Symptoms
- One slow dependency exhausts the shared pool.
- All requests wait.
- Cascading exhaustion.

## Solution
- A pool of resources per dependency (or per class of requests).
- A slow dependency exhausts its own pool only.
- The pools are sized by the dependency's expected load.
- A failure in one pool is invisible to the others.

## When to use
- Several external dependencies.
- The resources are shared (a thread pool, a connection pool).
- Instead of: one shared pool for everything.

## When NOT to use
- A single dependency.
- The resources are not shared.
- The isolation is more expensive than the failure.

## Trade-offs
- vs a shared pool: isolation vs utilization.
- vs a circuit breaker: the bulkhead is a wall, the breaker is a switch.
- vs a per-request pool: the bulkhead is a group, the per-request is a unit.

## Common pitfalls
- A bulkhead that is too small (underutilization) or too large (no isolation).
- A bulkhead that is shared with the breaker (the two patterns fight).
- Isolating by dependency, not by class of requests.

## Related
- patterns/system/circuit-breaker.md — the switch that opens.
- patterns/system/retry-with-backoff.md — the retry that fills the pool.
- patterns/component/microservices.md — where the bulkheads live.
- patterns/code/decorator.md — the bulkhead is often a decorator.
