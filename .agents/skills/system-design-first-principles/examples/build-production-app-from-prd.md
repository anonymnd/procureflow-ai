# Example: Build Production App From Product Requirements Document

## PRD Summary

Build a team task tracker where users create projects, assign tasks, comment, upload attachments, and receive notifications. Teams need a dashboard showing overdue work and weekly progress.

## Product Reality

- Users: small and mid-size teams.
- Critical action: create, update, and assign tasks.
- Must feel instant: dashboard load, task updates, comments.
- Can be delayed: email notifications, weekly progress summaries, attachment processing.
- Must never be wrong: permissions, task ownership, audit history.

## Load Reality

- Read/write ratio: read-heavy dashboards and task lists, moderate writes.
- Peak traffic: weekday work hours.
- Fanout: task update may affect database, notifications, activity feed, and search.
- Data growth: tasks, comments, audit logs, attachments.
- Expensive operations: dashboard aggregation, attachment scanning, weekly summary generation.
- Latency target: common API P99 under 500 ms; dashboard P99 under 1 second.

## Constraint Diagnosis

The first constraint is database query shape for tenant-scoped dashboards, followed by asynchronous side effects for notifications and summaries.

## Architecture Proposal

Start with a modular monolith:

- Auth and organization module.
- Project and task module.
- Comment and activity module.
- Attachment module using object storage.
- Notification worker module.
- Reporting module for weekly summaries.

Use PostgreSQL as source of truth. Use object storage for attachments because large binary files do not belong in hot relational tables. Use a task queue for notifications and weekly summaries because users do not need those side effects in the request path.

Decision chain:

```text
Problem: task updates must feel fast but trigger notifications and activity records
Constraint: synchronous fanout would raise P99 latency and amplify third-party email failures
Design choice: commit task update and activity record transactionally, then enqueue notification jobs
Tradeoff: notifications are eventually consistent
Failure mode: queue backlog delays notifications
Observability signal: task update P99, queue depth, oldest notification job age, worker error rate
```

## Data Model

| Entity | Fields | Constraints |
| --- | --- | --- |
| organizations | id, name, plan | unique name where needed |
| users | id, email, name | unique email |
| memberships | organization_id, user_id, role | unique organization_id + user_id |
| projects | id, organization_id, name, status | indexed organization_id |
| tasks | id, project_id, assignee_id, title, status, due_at | indexed project_id + status, assignee_id + due_at |
| comments | id, task_id, author_id, body, created_at | indexed task_id |
| attachments | id, task_id, object_key, filename, scan_status | indexed task_id |
| activity_events | id, organization_id, actor_id, entity_type, entity_id, created_at | indexed organization_id + created_at |
| idempotency_keys | key, actor_id, response_hash, created_at | unique actor_id + key |

Consistency model:

- Strong consistency for permissions and task writes.
- Eventual consistency for notifications, search indexing, and weekly summaries.

## API Design

```http
POST /projects/{projectId}/tasks
Idempotency-Key: create-task-123
```

```json
{
  "title": "Prepare launch checklist",
  "assigneeId": "usr_123",
  "dueAt": "2026-05-10T17:00:00Z"
}
```

```json
{
  "id": "task_456",
  "title": "Prepare launch checklist",
  "status": "open",
  "assigneeId": "usr_123"
}
```

Use cursor pagination for:

- `GET /projects/{projectId}/tasks`
- `GET /tasks/{taskId}/comments`
- `GET /organizations/{organizationId}/activity`

Errors:

```json
{
  "error": {
    "code": "FORBIDDEN",
    "message": "You do not have access to this project."
  }
}
```

## Failure Modes

```text
Problem: attachment scanning may fail or take minutes
Constraint: file processing is slow and may depend on third-party scanners
Design choice: upload to object storage, mark scan_status pending, process asynchronously
Tradeoff: attachment availability is delayed
Failure mode: scanner outage leaves attachments pending
Observability signal: pending scan age, scanner error rate, attachment processing queue depth
```

Use timeouts for object storage and email provider calls. Use bounded retries with exponential backoff and jitter. Send poison jobs to dead letter queues. Degrade by allowing task operations while notifications are delayed.

## Observability

- API P99 by route.
- Dashboard query latency and database input/output.
- Database lock waits and connection pool saturation.
- Notification queue depth and oldest job age.
- Attachment scan backlog.
- Authorization denial rate.
- Weekly summary generation cost and duration.

## Production Readiness

- Authentication through a trusted provider or signed sessions.
- Role-based authorization on organization, project, and task actions.
- Secrets stored in environment or secret manager.
- Environment variable documentation for database, object storage, email, and job queue.
- Database migrations with rollback plan.
- Backups and restore tests for PostgreSQL.
- Object storage lifecycle policy for deleted attachments.
- Tests for validation, authorization, idempotency, and worker retries.

## Scale Evolution

| Stage | Design |
| --- | --- |
| MVP | Modular monolith, PostgreSQL, object storage, simple background queue. |
| 10x | Add dashboard indexes, precomputed counters where query P99 requires it, worker autoscaling. |
| 100x | Partition activity events by time, add read replicas for dashboards, add search index if task search dominates. |
| 1000x | Split reporting or notifications only if team ownership or independent scaling requires it; consider sharding only after tenant data exceeds single-cluster limits. |

The core tradeoff is: keeping the app as a modular monolith protects delivery speed and correctness, while async workers introduce eventual consistency for side effects that do not need to block users.
