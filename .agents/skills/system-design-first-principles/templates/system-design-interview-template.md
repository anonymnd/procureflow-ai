# System Design Interview Template

Use this template for interview-style system design answers. Keep the answer structured, explicit, and production-aware.

## 1. Clarify Product Reality

- Users:
- Critical action:
- Must feel instant:
- Can be delayed:
- Must never be wrong:

## 2. Requirements

### Functional

- 

### Non-Functional

- Latency:
- Availability:
- Consistency:
- Durability:
- Cost:

## 3. Scale Assumptions

- Daily active users:
- Peak requests per second:
- Read/write ratio:
- Data growth:
- Fanout:
- P50/P95/P99 target:

## 4. API Design

```http
POST /resource
Content-Type: application/json
Idempotency-Key: <key>
```

```json
{
  "example": "request"
}
```

## 5. Data Model

| Entity | Key fields | Indexes | Constraints |
| --- | --- | --- | --- |
|  |  |  |  |

## 6. High-Level Architecture

Start with the simplest viable design. Default to a modular monolith unless constraints justify distributed services.

```text
Problem → Constraint → Design choice → Tradeoff → Failure mode → Observability signal
```

## 7. Core Flows

- Write path:
- Read path:
- Async path:
- Failure path:

## 8. Bottlenecks And Fixes

| Bottleneck | Why it appears | Fix | Tradeoff | Signal |
| --- | --- | --- | --- | --- |
|  |  |  |  |  |

## 9. Reliability

- Idempotency:
- Timeouts:
- Retries:
- Backoff and jitter:
- Circuit breakers:
- Dead letter queues:
- Degraded mode:

## 10. Observability

- P99 latency:
- Traffic:
- Errors:
- Saturation:
- Business metrics:

## 11. Scale Evolution

- MVP:
- 10x:
- 100x:
- 1000x:

## 12. Final Answer

```text
The core tradeoff is: ...
```
