# Example: Design An AI Market Intelligence Agent

## Product Reality

Users are founders, product leaders, and analysts who want competitor and market updates summarized into weekly briefs. Freshness matters, but most collection and synthesis can be delayed. The system must never fabricate sources or hide uncertainty.

## Load Reality

- Read/write ratio: more background writes and processing than interactive reads.
- Peak traffic: scheduled collection windows and Monday morning brief reads.
- Fanout: each tracked company fans out to news, websites, filings, social sources, and internal notes.
- Data growth: raw documents, extracted facts, embeddings, briefs, and audit logs.
- Expensive operations: external search, scraping, embedding, model inference, deduplication.
- Latency target: dashboard P99 under 1 second for cached briefs; background jobs can take minutes.

## Constraint Diagnosis

The first constraint is third-party latency and background job throughput, not web request latency.

## Architecture

Start with a modular monolith:

- Account and workspace module.
- Source connector module.
- Collection scheduler.
- Extraction and normalization workers.
- Evidence store and brief generator.
- Review and delivery module.

Use PostgreSQL as source of truth. Store raw documents in object storage when size grows beyond comfortable database storage. Use a task queue for collection and synthesis jobs. Add a vector index only if semantic retrieval over evidence is needed and measured as valuable.

Decision chain:

```text
Problem: source collection can be slow and unreliable
Constraint: third-party latency and rate limits dominate
Design choice: run collection asynchronously through a bounded task queue with per-source rate limits
Tradeoff: briefs may lag behind real time
Failure mode: one bad source creates repeated failures and blocks useful work
Observability signal: job success rate, retry count, source latency, queue depth, oldest job age
```

## Data Model

| Entity | Fields | Constraints |
| --- | --- | --- |
| workspaces | id, name, plan | unique name per owner |
| tracked_entities | id, workspace_id, name, domain, tags | indexed workspace_id |
| sources | id, entity_id, type, url, status | unique entity_id + url |
| documents | id, source_id, uri, content_hash, fetched_at | unique content_hash per source |
| facts | id, document_id, claim, confidence, extracted_at | indexed document_id |
| briefs | id, workspace_id, period_start, period_end, status | unique workspace_id + period |
| idempotency_keys | key, actor_id, result_ref, created_at | unique actor_id + key |

Evidence links are part of the source of truth. Generated summaries must reference facts and documents.

## API Design

```http
POST /tracked-entities
Idempotency-Key: entity-create-2026-05-03
```

```json
{
  "name": "Acme AI",
  "domain": "acme.example",
  "tags": ["competitor", "pricing"]
}
```

```http
GET /briefs?workspaceId=wk_123&period=2026-W18
```

Use cursor pagination for documents and facts. Rate limit source additions and manual refresh actions.

## Failure Modes

```text
Problem: model generation may produce unsupported claims
Constraint: language model output is probabilistic and source data may conflict
Design choice: require evidence-linked facts and mark unsupported claims as unknown
Tradeoff: briefs may be less fluent or less complete
Failure mode: missing evidence causes empty or cautious sections
Observability signal: unsupported-claim rejection count, citation coverage, human edit rate
```

Use bounded retries with exponential backoff and jitter for source fetches. Circuit break sources that repeatedly fail. Send poison jobs to dead letter queues with the source, error, and retry history.

## Observability

- Dashboard P99.
- Source fetch latency and error rate by source type.
- Queue depth and oldest job age by job type.
- Model inference latency, token usage, and failure rate.
- Citation coverage.
- Brief generation success rate.
- Cost per workspace and per generated brief.

## Production Readiness

- Encrypt secrets and source credentials.
- Enforce workspace-level authorization.
- Audit manual refreshes and brief publication.
- Document environment variables for model provider, database, object storage, and queue.
- Add backup and restore for PostgreSQL and object storage lifecycle rules.
- Add cost controls for model calls and source polling.

## Scale Evolution

| Stage | Design |
| --- | --- |
| MVP | Modular monolith, PostgreSQL, scheduled workers, one model provider, evidence-linked summaries. |
| 10x | Add per-source rate limiters, object storage for raw documents, worker autoscaling. |
| 100x | Add priority queues, stronger deduplication, search or vector index for evidence retrieval. |
| 1000x | Split collection and generation services only if teams need independent deployability or workloads require different scaling and failure isolation. |

The core tradeoff is: asynchronous evidence-first generation preserves reliability and auditability, but users accept delayed freshness and higher pipeline complexity.
