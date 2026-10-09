# Example: Review Existing Software-as-a-Service Architecture

## Input Architecture

- React frontend.
- Node.js backend.
- PostgreSQL database.
- Redis cache.
- BullMQ workers.

## Product Reality

Assume this is a multi-tenant Software-as-a-Service application where users manage business records and trigger background workflows. The critical action is creating or updating tenant-owned records. The system must never leak data across tenants. Background notifications and exports can be delayed.

## Load Reality

- Read/write ratio: likely read-heavy during dashboard usage.
- Peak traffic: business hours and scheduled jobs.
- Fanout: backend may call database, Redis, workers, and third-party APIs.
- Data growth: tenant records, audit logs, jobs, and exports.
- Expensive operations: reports, exports, third-party calls.
- Latency target: interactive API P99 under 500 ms for common record operations.

## Review Findings

### Finding 1: Redis Needs A Named Job Or Cache Constraint

```text
Problem: Redis is present, but the architecture does not say whether it is cache, rate limiter, session store, or queue dependency
Constraint: unclear ownership makes correctness and failure handling ambiguous
Design choice: document Redis use cases separately: cache keys, BullMQ job queues, rate limits, or sessions
Tradeoff: clearer boundaries require more operational documentation and tests
Failure mode: Redis outage either slows reads, breaks jobs, or logs out users unexpectedly
Observability signal: Redis command latency, cache hit ratio, queue depth, session error rate by use case
```

### Finding 2: BullMQ Requires Idempotency And Dead Letter Handling

```text
Problem: background jobs can be retried after worker crashes or timeouts
Constraint: retries can duplicate emails, payments, webhooks, or data mutations
Design choice: add idempotency keys and bounded retries with exponential backoff, jitter, and dead letter queues
Tradeoff: job handlers need deduplication storage and replay tooling
Failure mode: poison jobs churn forever or duplicate side effects
Observability signal: retry count, failed job count, dead letter queue depth, oldest job age
```

### Finding 3: PostgreSQL Should Stay The Source Of Truth

```text
Problem: cache and workers may introduce derived state
Constraint: correctness depends on one authoritative record per tenant object
Design choice: keep PostgreSQL as source of truth with tenant_id scoping, uniqueness constraints, and transactional writes
Tradeoff: some reads may need indexes or caching as traffic grows
Failure mode: cache or job state diverges from database state
Observability signal: database constraint violations, reconciliation mismatches, stale cache reports
```

## API Review

- Require tenant context from authentication, not from client-provided body fields alone.
- Use cursor pagination for large record lists.
- Use consistent error shape.
- Use `Idempotency-Key` on retryable create actions.
- Validate payloads at the Node.js API boundary.

## Data Review

- Every tenant-owned table should include `tenant_id`.
- Add composite indexes for common tenant-scoped queries.
- Add uniqueness constraints scoped by tenant where product rules require it.
- Add audit logs for security-sensitive changes.
- Test migrations against realistic table sizes.

## Reliability Review

- Set explicit timeouts for database, Redis, and third-party calls.
- Use circuit breakers around third-party dependencies that can fail slowly.
- Add degraded mode when Redis cache is unavailable: read from PostgreSQL with tighter rate limits.
- Ensure BullMQ workers are idempotent.
- Add dead letter queue review and replay procedure.

## Observability Review

- API P50, P95, P99 by route and tenant tier.
- Database query P99, lock waits, connection pool saturation.
- Redis latency and errors by use case.
- BullMQ queue depth, oldest job age, retry count, dead letters.
- Authentication failures and authorization denials.
- Cost by tenant tier if noisy tenants exist.

## Scale Evolution

| Stage | Review |
| --- | --- |
| MVP | Current stack is reasonable if Redis use is limited and PostgreSQL is source of truth. |
| 10x | Add indexes, connection pooling, queue monitoring, and cache only for proven hot reads. |
| 100x | Add read replicas for read-heavy dashboards, partition audit logs, and isolate heavy exports. |
| 1000x | Consider service split only if separate teams own workflows or background processing needs independent deployability and failure isolation. |

The core tradeoff is: this architecture can remain simple and production-ready if PostgreSQL owns correctness, while Redis and BullMQ are treated as failure-prone accelerators with explicit observability and recovery.
