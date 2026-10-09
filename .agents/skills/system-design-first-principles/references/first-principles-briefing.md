# First-Principles Briefing

Production system design is governed less by named tools and more by software physics: latency, throughput, data distance, consistency, availability, queues, storage input/output, distributed failure, and observability.

## Latency Versus Throughput

Latency is the time for one unit of work to complete. A user clicking "checkout" cares about latency.

Throughput is volume of work per time window. A worker fleet processing one million invoices per hour cares about throughput.

Optimizing one can hurt the other. Batching may improve throughput by reducing overhead per item, but it can increase latency for the first item waiting in the batch.

Decision chain:

```text
Problem: requests feel slow during peak traffic
Constraint: per-request latency exceeds the interactive budget
Design choice: reduce synchronous work, batch non-interactive work, and remove unnecessary round trips
Tradeoff: delayed side effects and more reconciliation logic
Failure mode: queued work falls behind
Observability signal: P99 request latency, queue depth, worker lag
```

## Memory Hierarchy And Data Distance

Data has distance. L1 cache, L2 cache, memory, SSD, disk, and network differ by orders of magnitude. Systems become slower when they move from local memory to disk or from local disk to network.

This matters because "just call another service" can turn a cheap local operation into a network operation with serialization, Transport Layer Security, retries, timeouts, and partial failure.

Use caching, precomputation, denormalized read models, batching, or colocation only when the data-distance cost is visible in latency, input/output, or saturation metrics.

## P50, P99, And Tail Pain

Average latency hides user suffering.

- P50 is the normal user experience.
- P95 shows degraded but common cases.
- P99 and P99.9 reveal tail pain.

If one request fans out to ten downstream calls, the user sees the slowest one. Rare slow downstream calls become common slow user experiences as fanout grows.

Decision chain:

```text
Problem: checkout P99 is high although average latency looks fine
Constraint: checkout fans out to payment, inventory, fraud, shipping, and email synchronously
Design choice: keep payment and inventory synchronous, move email and analytics async, add timeouts to fraud
Tradeoff: some side effects arrive later and require retry reconciliation
Failure mode: async backlog delays email or analytics
Observability signal: endpoint P99, downstream span latency, queue lag
```

## Little's Law

Little's Law says:

```text
in-flight work = arrival rate × time in system
```

If requests arrive at 1,000 per second and each takes 200 milliseconds, the system has about 200 requests in flight. If latency rises to 2 seconds, in-flight work rises to about 2,000. That extra work consumes memory, connections, thread pools, sockets, and Central Processing Unit.

This is why latency regressions can become outages. Slow systems accumulate work until they saturate.

## Amdahl's Law

Amdahl's Law says total speedup is limited by the serial part of the system.

If 20 percent of a request must happen serially, making the remaining 80 percent infinitely parallel cannot make the whole request more than 5x faster.

Use this to avoid wasteful parallelization. First find the serial bottleneck, then decide whether it can be removed, shortened, cached, or moved off the critical path.

## The Knee Of The Curve

Near 100 percent utilization, queue waiting time explodes. A server that is 95 percent busy may look efficient but has little room for bursts, garbage collection, noisy neighbors, lock contention, or slow downstream calls.

Production systems need headroom. The right target depends on workload shape, but high sustained utilization should trigger capacity review.

Decision chain:

```text
Problem: latency spikes during normal traffic bursts
Constraint: worker pool runs near saturation, so queue wait dominates
Design choice: add headroom, autoscale earlier, cap concurrency, and shed low-priority work
Tradeoff: higher baseline cost and possible rejected requests
Failure mode: autoscaling lags behind burst arrival
Observability signal: utilization, queue wait time, rejected requests, P99 latency
```

## Databases As File Input/Output Management

Databases manage files, indexes, caches, locks, logs, and query execution under correctness rules.

B-Tree storage engines are strong for indexed reads, range scans, and transactional workloads. They keep sorted structures that make lookup and range access efficient.

Log-Structured Merge Tree storage engines are strong for high write throughput. They turn random writes into sequential writes, then compact data later. The tradeoff is compaction cost, read amplification, and operational tuning.

Choose based on access pattern, not fashion.

## Storage Physics

Storage has physical constraints:

- Random writes are more expensive than sequential writes.
- Write amplification increases disk work beyond the logical write.
- Checksums detect corruption.
- Replication protects availability but does not automatically protect against bad writes.
- Backups protect against deletion, corruption, and operator error.
- Bit rot and silent corruption matter for long-lived data.

Critical data needs backups, restore testing, checksums or integrity checks, and clear recovery objectives.

## Network Tax

Networking is a chain: operating system, sockets, Domain Name System, Border Gateway Protocol, Anycast, Transport Layer Security, edge routing, load balancing, serialization, and application code.

Each hop adds latency and failure probability. Domain Name System issues can break discovery. Border Gateway Protocol routing can change path quality. Anycast can route users to the nearest healthy edge but needs careful health checks. Transport Layer Security adds handshake and certificate concerns. Serialization adds Central Processing Unit and payload cost.

JSON is easy for humans but expensive for machines compared with binary formats. Protocol Buffers or FlatBuffers may be justified when Central Processing Unit cost, bandwidth, mobile battery, or very high request volume dominates.

## Sharding Strategies

Sharding is needed only when a single machine's Central Processing Unit, memory, disk input/output, storage, or operational limit is exhausted after simpler fixes.

Range sharding keeps related data together and supports range queries, but hot ranges can overload one shard.

Hash sharding spreads load more evenly, but range queries and resharding are harder.

Consistent hashing reduces data movement when nodes are added or removed, but it still adds routing and operational complexity.

## Distributed Consensus And Raft

Consensus prevents split-brain when multiple nodes might disagree about leadership or state.

Raft uses leader election, terms, logs, and quorum. A leader accepts writes, replicates them to followers, and commits only when a quorum agrees. This improves correctness under node failure but adds coordination latency and operational complexity.

Use consensus when the system must choose one truth under partial failure, not for ordinary background jobs.

## Caching Strategies

Caching works because hot data is unevenly accessed.

Cache-aside lets application code read from cache, fall back to the source of truth, and then fill the cache. It is flexible but can be stale.

Write-through writes cache and database during the write path. It improves read consistency but slows writes and couples cache availability to writes.

Write-back writes to cache first and flushes later. It is fast but can lose data unless the cache is durable and recovery is designed.

Every cache needs invalidation, time to live, cold-start behavior, stampede protection, and fallback rules.

## Queues Versus Streams

Task queues process discrete jobs. They are appropriate for emails, media processing, webhook delivery, report generation, and other units of work where a worker claims and completes a job.

Streams preserve an immutable log. They are appropriate when the system needs replay, retention, ordered event history, or multiple independent consumers reading the same facts.

Do not choose a stream when a queue is enough. The stream's value is replayable history and fanout, not brand recognition.

## Idempotency

Retries are unavoidable in distributed systems. A retry can duplicate a write unless the operation is idempotent.

Use idempotency keys for external write requests, unique constraints for natural uniqueness, and deduplication tables for asynchronous consumers.

Example:

```text
Problem: payment request times out but may have succeeded
Constraint: retry could charge the customer twice
Design choice: require an idempotency key and store payment result by key
Tradeoff: extra storage and key lifecycle management
Failure mode: key expires too early and duplicate write is accepted
Observability signal: duplicate-key hits, payment retry count, reconciliation mismatches
```

## Circuit Breakers, Backoff, And Jitter

Circuit breakers prevent cascading failure by stopping calls to a failing dependency for a short period.

Exponential backoff reduces retry pressure by spacing retries farther apart.

Jitter randomizes retry timing so many clients do not retry at the same instant and create a thundering herd.

Use these together with timeouts and retry budgets. Retrying forever turns a dependency failure into your own outage.

## Microservice Trap

Microservices add network latency, serialization cost, partial failure, distributed tracing, versioning, deployment coordination, and data ownership problems.

Start with a modular monolith unless organizational scale, independent deployability, failure isolation, or data ownership boundaries justify services.

## Four Golden Signals

Track:

- Latency: how long requests take, especially P95 and P99.
- Traffic: request rate, job rate, event rate, or throughput.
- Errors: failed requests, failed jobs, dependency errors, validation errors.
- Saturation: Central Processing Unit, memory, disk input/output, connection pools, queue depth, thread pools, rate limits.

These signals reveal whether the system is fast, used, correct, and close to collapse.

## Error Budgets

An error budget turns reliability into a business decision. If the target is 99.9 percent availability, the system has about 0.1 percent allowable failure. Spending that budget on risky releases, migrations, or experiments should be intentional.

Reliability work is justified when the budget is being consumed faster than the business can tolerate.
