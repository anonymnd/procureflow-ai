# Circuit Breaker

## One-liner
A switch that opens after repeated failures, so the caller does not wait on a dead dependency.

## Symptoms
- A slow/failing dependency drags down the caller.
- Timeouts accumulate.
- Cascading failures.

## Solution
- Three states: closed (pass), open (fail fast), half-open (probe).
- A failure threshold opens the circuit.
- A timeout closes it again (half-open first).
- A fallback (cache, default, error) is defined per call.

## When to use
- The dependency is external and may fail.
- The caller must not wait on the dependency.
- Instead of: a timeout that accumulates.

## When NOT to use
- The dependency is local.
- Every call is independent and cheap.
- The fallback is "nothing" (then the caller must handle it).

## Trade-offs
- vs timeout: fail-fast vs wait.
- vs retry: the breaker stops the flow, the retry repeats it.
- vs bulkhead: the breaker is a switch, the bulkhead is a wall.

## Common pitfalls
- A breaker with no fallback (it fails, but the caller is still stuck).
- A threshold that is too low (flapping) or too high (never opens).
- A breaker that protects the dependency but not the caller (the caller is still blocked).

## Related
- patterns/system/retry-with-backoff.md — the other half of resilience.
- patterns/system/bulkhead.md — isolating the resources.
- patterns/component/microservices.md — where the breakers live.
- patterns/code/decorator.md — the breaker is often a decorator.
