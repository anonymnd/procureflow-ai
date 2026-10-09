# Example: Design A URL Shortener

## Product Reality

Users create short links and redirect visitors to long URLs. Link creation can take a few hundred milliseconds. Redirects must feel instant. The redirect target must never be wrong.

## Load Reality

- Read/write ratio: heavily read-biased, often 100:1 or higher.
- Peak traffic: redirects spike when links are shared publicly.
- Fanout: redirect path should avoid fanout.
- Data growth: links and click events grow continuously.
- Expensive operations: analytics aggregation and abuse scanning.
- Latency target: redirect P50 under 30 ms from application, P99 under 150 ms before network edge effects.

## Constraint Diagnosis

The first constraint is redirect latency and database read throughput. The write path is less sensitive.

## Architecture

Start with a modular monolith:

- Web/API module for link creation.
- Redirect module optimized for lookup and 301/302 response.
- Analytics module that records click events asynchronously.
- PostgreSQL as source of truth.
- Optional cache only after hot links create measurable database pressure.

Decision chain:

```text
Problem: redirects must be fast under read-heavy traffic
Constraint: database lookup latency and read throughput dominate
Design choice: indexed short_code lookup in PostgreSQL, then add cache-aside for hot links when P99 or database read input/output requires it
Tradeoff: cache can return stale targets until invalidation or time to live expires
Failure mode: cache stampede during viral traffic
Observability signal: redirect P99, cache hit ratio, database read QPS, cache miss latency
```

## Data Model

| Entity | Fields | Constraints |
| --- | --- | --- |
| links | id, short_code, long_url, owner_id, created_at, expires_at, disabled_at | unique short_code, indexed owner_id |
| click_events | id, link_id, timestamp, referrer, user_agent_hash, country | indexed link_id and timestamp |
| idempotency_keys | key, user_id, response_hash, created_at | unique key per user |

Source of truth is PostgreSQL. Click events can be eventually consistent because analytics can lag without breaking redirects.

## API Design

```http
POST /links
Idempotency-Key: 2f9c2d7e
Content-Type: application/json
```

```json
{
  "longUrl": "https://example.com/very/long/path",
  "customCode": "launch"
}
```

```json
{
  "shortUrl": "https://sho.rt/launch",
  "longUrl": "https://example.com/very/long/path"
}
```

Redirect:

```http
GET /{shortCode}
```

Errors use one shape:

```json
{
  "error": {
    "code": "LINK_NOT_FOUND",
    "message": "Short link not found."
  }
}
```

## Failure Modes

```text
Problem: click analytics should not slow redirects
Constraint: analytics writes add latency and can fail independently
Design choice: enqueue click event after resolving the link
Tradeoff: analytics becomes eventually consistent
Failure mode: queue backlog drops or delays click counts
Observability signal: queue depth, oldest click event age, worker error rate
```

Use bounded retries with exponential backoff and jitter for analytics writes. Poison events go to a dead letter queue. Redirect still works in degraded mode if analytics is down.

## Observability

- Redirect P50, P95, P99.
- Link creation latency.
- Cache hit ratio if cache exists.
- Database read input/output and query P99.
- Queue depth and oldest click event age.
- 404 rate, abuse-block rate, disabled-link redirects.

## Production Readiness

- Validate URLs and block dangerous schemes.
- Rate limit link creation by user and IP.
- Add abuse scanning for malware and phishing.
- Store secrets outside source control.
- Back up PostgreSQL and test restore.
- Use migrations for schema changes.
- Add rollback for application deploys.

## Scale Evolution

| Stage | Design |
| --- | --- |
| MVP | Modular monolith, PostgreSQL, indexed lookup, async analytics. |
| 10x | Add cache-aside for hot links if database read P99 rises. |
| 100x | Add read replicas, edge cache for immutable safe redirects, partition click events by time. |
| 1000x | Consider sharding links by short_code hash only if storage or read input/output exceeds a single database cluster's practical limit. |

The core tradeoff is: keeping redirects simple and correct avoids operational complexity early, while analytics and caching become eventually consistent when scale requires them.
