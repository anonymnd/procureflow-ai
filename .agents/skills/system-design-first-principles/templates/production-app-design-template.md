# Production Application Design Template

Use this template when turning a product idea into a production-ready application architecture.

## 1. Product Reality

- Users:
- Critical action:
- Must feel instant:
- Can be delayed:
- Must never be wrong:

## 2. Load Reality

- Read/write ratio:
- Peak traffic:
- Fanout:
- Data growth:
- Expensive operations:
- P50/P95/P99 latency targets:

## 3. Dominant Constraint

```text
Problem:
Constraint:
Why this dominates first:
```

## 4. Simplest Viable Architecture

Start with the smallest design that satisfies the constraints.

```text
Problem → Constraint → Design choice → Tradeoff → Failure mode → Observability signal
```

## 5. Data Model

- Source of truth:
- Entities:
- Indexes:
- Uniqueness constraints:
- Idempotency keys:
- Consistency model:
- Archival strategy:

## 6. API Design

- Endpoints:
- Request examples:
- Response examples:
- Pagination:
- Validation:
- Error shape:
- Rate limits:
- Idempotent write behavior:

## 7. Failure Modes

- Timeouts:
- Retries:
- Exponential backoff and jitter:
- Circuit breakers:
- Poison jobs:
- Dead letter queues:
- Partial failure:
- Degraded mode:

## 8. Observability

- Logs:
- Metrics:
- Traces:
- Latency percentiles:
- Traffic:
- Errors:
- Saturation:
- Alerts:
- Dashboards:

## 9. Production Readiness

- Authentication:
- Authorization:
- Secrets:
- Migrations:
- Deployment:
- Rollback:
- Backups:
- Disaster recovery:
- Cost controls:

## 10. Scale Evolution

| Stage | Constraint | Design change | Tradeoff | Signal |
| --- | --- | --- | --- | --- |
| MVP |  |  |  |  |
| 10x |  |  |  |  |
| 100x |  |  |  |  |
| 1000x |  |  |  |  |

## 11. Final Answer

```text
The core tradeoff is: ...
```
