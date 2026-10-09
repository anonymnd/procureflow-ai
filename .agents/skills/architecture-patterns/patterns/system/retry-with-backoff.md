# Retry with Backoff

## One-liner
Repeat a failed operation with a growing delay, so transient failures recover.

## Symptoms
- Transient failures cause real errors.
- Retries without backoff create a storm.
- A failure is temporary, not permanent.

## Solution
- A retry policy: max attempts, base delay, multiplier.
- Exponential backoff with jitter.
- A distinction: retryable (5xx, timeout) vs non-retryable (4xx).
- A total timeout bounds the whole retry loop.

## When to use
- The operation is idempotent.
- The failure is transient.
- Instead of: a one-shot call that fails.

## When NOT to use
- A non-idempotent operation.
- A permanent failure.
- The operation is cheap and local.

## Trade-offs
- vs a one-shot: recovery vs latency.
- vs a circuit breaker: the retry repeats, the breaker stops.
- vs a queue: the retry is synchronous, the queue is asynchronous.

## Common pitfalls
- Retrying a non-idempotent operation (duplicates).
- No jitter (the retries synchronize into a storm).
- A retry that retries forever (no total timeout).

## Related
- patterns/system/circuit-breaker.md — the breaker stops the storm.
- patterns/system/idempotency.md — the precondition for a safe retry.
- patterns/system/bulkhead.md — isolating the retried resource.
- patterns/code/cache-aside.md — when the retry is for a read.
