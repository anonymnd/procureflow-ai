# Product Requirements Document To Architecture Template

Use this template when converting a Product Requirements Document into implementation architecture.

## 1. Product Interpretation

- Product goal:
- Primary users:
- Critical user journeys:
- Success metrics:
- Non-goals:

## 2. Requirements Extracted From PRD

| Requirement | Type | Production implication |
| --- | --- | --- |
|  | Functional / Non-functional |  |

## 3. Product Reality

- Must feel instant:
- Can be delayed:
- Must never be wrong:
- Can be eventually consistent:

## 4. Load Reality

- Expected traffic:
- Peak multiplier:
- Read/write ratio:
- Data growth:
- Fanout:
- Expensive operations:
- P50/P95/P99 target:

## 5. Architecture Proposal

Start with the simplest viable architecture.

```text
Problem:
Constraint:
Design choice:
Tradeoff:
Failure mode:
Observability signal:
```

## 6. Module Boundaries

| Module | Owns | Does not own | Public contract |
| --- | --- | --- | --- |
|  |  |  |  |

## 7. Data Model

- Source of truth:
- Entities:
- Indexes:
- Uniqueness constraints:
- Idempotency keys:
- Consistency model:
- Archival:

## 8. API And Events

- Synchronous APIs:
- Async jobs:
- Events, if justified:
- Request validation:
- Error shape:
- Rate limits:

## 9. Production Engineering Plan

- Configuration and environment variables:
- Authentication:
- Authorization:
- Logging:
- Metrics:
- Tracing:
- Tests:
- Deployment:
- Rollback:
- Backups:
- Cost controls:

## 10. Implementation Plan

| Step | Change | Risk | Verification |
| --- | --- | --- | --- |
| 1 |  |  |  |

## 11. Scale Evolution

- MVP:
- 10x:
- 100x:
- 1000x:

## 12. Final Answer

```text
The core tradeoff is: ...
```
