# Architecture Review Template

Use this template to review an existing system or proposed architecture.

## Review Scope

- System:
- Business goal:
- Critical workflow:
- Current architecture:
- Known incidents or concerns:

## Executive Findings

| Priority | Finding | Impact | Recommendation |
| --- | --- | --- | --- |
| P0/P1/P2/P3 |  |  |  |

## Product And Load Reality

- Users:
- Critical action:
- Read/write ratio:
- Peak traffic:
- Fanout:
- Data growth:
- P50/P95/P99 targets:

## Constraint Diagnosis

```text
Likely first constraint:
Evidence:
What breaks first:
```

## Decision Review

For each major component:

```text
Component:
Problem:
Constraint:
Design choice:
Tradeoff:
Failure mode:
Observability signal:
Verdict: keep / simplify / replace / defer
```

## Data Model Review

- Source of truth:
- Ownership boundaries:
- Indexes:
- Uniqueness constraints:
- Consistency model:
- Idempotency:
- Archival:
- Backup and restore:

## API Review

- Endpoint shape:
- Validation:
- Pagination:
- Error shape:
- Rate limits:
- Idempotent writes:
- Backward compatibility:

## Reliability Review

- Timeouts:
- Retries:
- Backoff and jitter:
- Circuit breakers:
- Queue depth:
- Dead letters:
- Degraded mode:
- Disaster recovery:

## Observability Review

- Logs:
- Metrics:
- Traces:
- P99 latency:
- Traffic:
- Errors:
- Saturation:
- Alerts:
- Dashboards:

## Security Review

- Authentication:
- Authorization:
- Secrets:
- Sensitive data:
- Audit logs:
- Abuse prevention:

## Scale Review

| Stage | What breaks | Required change | Tradeoff |
| --- | --- | --- | --- |
| 10x |  |  |  |
| 100x |  |  |  |
| 1000x |  |  |  |

## Final Answer

```text
The core tradeoff is: ...
```
