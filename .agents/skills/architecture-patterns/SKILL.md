---
name: architecture-patterns
license: MIT
description: Reflection protocol, decision tree and pattern catalogue for choosing software architecture and design patterns. Use before designing system architecture, creating a new module/service/class structure, choosing among design patterns, or refactoring tangled code. Covers code-level patterns (strategy, observer, factory, builder, DI, repository, unit of work, data mapper, DTO, specification, value object, aggregate, facade, adapter, decorator, state, template method, command, chain of responsibility, mediator, memoization, cache-aside, pub/sub, feature flag), component-level (hexagonal, clean architecture, layered, modular monolith, event-driven, microservices, plugin architecture, BFF) and system-level (monolith vs microservices, CQRS, event sourcing, outbox, saga, circuit breaker, retry with backoff, bulkhead, idempotency, strangler fig). Do NOT use for trivial edits, typos, renames, or small bug fixes that do not change structure.
---

# Architecture Patterns

Purpose: decide which patterns fit the task and the project — and decide NOT to use patterns. A pattern is applied only when its symptoms match. The default answer is "no pattern".

## When to use

- Starting a new project, service, or module (architecture decision).
- Creating a new class structure, module boundary, or data flow.
- Choosing between design patterns for a concrete task.
- Refactoring code that feels tangled, over-coupled, or duplicated.
- The user explicitly asks about architecture or patterns.

## When NOT to use

- Trivial edits: typos, renames, small bug fixes (roughly <= 50 changed lines) that do not change structure.
- The task fully determines the structure (e.g. "add this column to this table").
- The project already has a convention that covers this case — just follow it.

Tie-breaker question: "Would a plain implementation without any pattern fail?" If no — write plain code and stop.

## Reflection protocol

Fill this block before selecting patterns. Do not skip it. For an existing codebase, check the existing style before answering (search for similar code first).

- Task: new | modify | refactor — scope: function / file / module / service
- Project: size, team size, change frequency, domain complexity
- Existing style: patterns/structures already present in the codebase
- Constraints: stack, infrastructure, team skills, cost
- Default check: is a plain solution without any pattern sufficient? (yes -> stop here, write plain code)

## Decision tree

Read the pattern file in full before applying it. Never apply from memory.

### Code level

- Many branches on "type"/variant, behavior chosen at runtime, new variants break existing code → patterns/code/strategy.md
- One event must reach several independent consumers; A changes imply B, C, D updates → patterns/code/observer.md
- Object creation scattered (new with the same params), product family varies → patterns/code/factory-method.md
- Object with many optional parameters, telescoping constructors → patterns/code/builder.md
- Collaborators hardcoded (new inside class), hard to test, swapping implementations → patterns/code/dependency-injection.md
- Data access calls inside business code, hard to swap storage → patterns/code/repository.md
- Several entities must commit atomically, partial saves, lost updates → patterns/code/unit-of-work.md
- Model leaks persistence details or model == table and you need a boundary → patterns/code/data-mapper.md
- Data crosses a layer boundary (API/DB/clients), entities leak out, accidental mutation → patterns/code/dto.md
- The same query criteria duplicated in many places, composable conditions → patterns/code/specification.md
- Thing identified by its attributes, not by identity; mutable "entity" that should be immutable → patterns/code/value-object.md
- Invariants span several entities, partial updates, concurrent write conflicts → patterns/code/aggregate.md
- Client must use many classes of a subsystem for one operation → patterns/code/facade.md
- Must integrate a third-party/legacy API whose interface you cannot change → patterns/code/adapter.md
- Add caching/logging/behavior to an existing implementation without touching it, combinable behaviors → patterns/code/decorator.md
- Object has many states, transitions, if/else on state, invalid states → patterns/code/state.md
- Same algorithm in several places with small step differences → patterns/code/template-method.md
- Actions must be queued, logged, undone, or retried → patterns/code/command.md
- Which handler serves a request is not known up front; adding handlers changes the caller → patterns/code/chain-of-responsibility.md
- N objects call each other directly, spaghetti of cross-calls → patterns/code/mediator.md
- Expensive pure computation repeated with the same arguments → patterns/code/memoization.md
- Expensive data fetched repeatedly, N+1 queries, TTL/invalidation questions → patterns/code/cache-aside.md
- Producers must not know consumers; cross-module coupling through direct calls → patterns/code/pub-sub.md
- Ship behavior before it is ready, kill switch, gradual rollout → patterns/code/feature-flag.md

### Component level

- Domain code tangled with framework/DB/API specifics, cannot test or swap infrastructure → patterns/component/hexagonal.md
- Dependencies point outward (domain depends on framework), layers unclear → patterns/component/clean-architecture.md
- Everything in one layer, "god service", UI talks to the DB directly → patterns/component/layered.md
- One deployable that grows, modules touch each other's internals → patterns/component/modular-monolith.md
- Synchronous call chains between modules, cascading failures, latency under load → patterns/component/event-driven.md
- Several teams cannot ship independently, deploy bottleneck, scale by part → patterns/component/microservices.md
- Core must be extended by third parties without redeploying, versioned behavior → patterns/component/plugin-architecture.md
- Different frontends need different slices of the same backend, over-fetching → patterns/component/bff.md

### System level

- Choosing how to decompose the system at project start or growth → patterns/system/monolith-vs-microservices.md
- Complex write invariants plus divergent read projections, different scaling needs → patterns/system/cqrs.md
- Audit of changes required, temporal queries, rebuild state from history → patterns/system/event-sourcing.md
- Database write plus message publish must be atomic; events lost on crash → patterns/system/outbox.md
- Long business process spans several services, multi-step, needs compensation → patterns/system/saga.md
- A slow/failing dependency drags down the caller, timeouts, cascades → patterns/system/circuit-breaker.md
- Transient failures (network blips, 503s) cause real errors → patterns/system/retry-with-backoff.md
- One slow dependency exhausts the shared resource pool → patterns/system/bulkhead.md
- At-least-once delivery / retries produce duplicates → patterns/system/idempotency.md
- Legacy system must be replaced incrementally, no big-bang rewrite → patterns/system/strangler-fig.md

## Index

One line per pattern file. Full catalogue:

Code level:
- patterns/code/strategy.md — interchangeable algorithms, chosen at runtime
- patterns/code/observer.md — many subscribers to one event
- patterns/code/factory-method.md — centralized object creation
- patterns/code/builder.md — stepwise construction of a complex object
- patterns/code/dependency-injection.md — explicit dependencies, swappable implementations
- patterns/code/repository.md — collection abstraction over persistence
- patterns/code/unit-of-work.md — atomic commit of a set of changes
- patterns/code/data-mapper.md — persistence separated from the domain model
- patterns/code/dto.md — data shape for a layer boundary
- patterns/code/specification.md — composable query criteria
- patterns/code/value-object.md — identity by value, immutable
- patterns/code/aggregate.md — consistency boundary for writes
- patterns/code/facade.md — simple interface over a complex subsystem
- patterns/code/adapter.md — translates an incompatible interface
- patterns/code/decorator.md — adds behavior at runtime
- patterns/code/state.md — object states and transitions
- patterns/code/template-method.md — fixed algorithm with variable steps
- patterns/code/command.md — action as an object
- patterns/code/chain-of-responsibility.md — handlers pass the request along
- patterns/code/mediator.md — centralizes interaction between objects
- patterns/code/memoization.md — caches a pure computation
- patterns/code/cache-aside.md — caches expensive data with invalidation
- patterns/code/pub-sub.md — decouples producers and consumers by topic
- patterns/code/feature-flag.md — toggles behavior by condition

Component level:
- patterns/component/hexagonal.md — core isolated from infrastructure via ports
- patterns/component/clean-architecture.md — layers by inward dependencies
- patterns/component/layered.md — presentation / service / data separation
- patterns/component/modular-monolith.md — one deployable, strict module boundaries
- patterns/component/event-driven.md — asynchronous communication via events
- patterns/component/microservices.md — services per bounded context
- patterns/component/plugin-architecture.md — extension without redeploy
- patterns/component/bff.md — backend aggregation per frontend

System level:
- patterns/system/monolith-vs-microservices.md — decomposition decision by criteria
- patterns/system/cqrs.md — separate read and write models
- patterns/system/event-sourcing.md — history as the source of truth
- patterns/system/outbox.md — reliable event publishing from the database
- patterns/system/saga.md — long-running process across services
- patterns/system/circuit-breaker.md — protection from a failing dependency
- patterns/system/retry-with-backoff.md — retries for transient failures
- patterns/system/bulkhead.md — isolated resources per dependency
- patterns/system/idempotency.md — duplicates have the same effect
- patterns/system/strangler-fig.md — incremental migration off legacy

## Selection rules

1. Default: no pattern. Apply a pattern only when the symptoms in its "When to use" match the task.
2. Maximum 1-3 patterns per task. If you need more, stop and re-check the symptoms.
3. Consistency: the project's existing style takes priority over a "better" pattern. Introduce a new pattern only with an explicit justification in your answer.
4. Read the pattern file in full before applying it (never from memory).
5. Do not combine conflicting patterns — see references/anti-patterns.md.
6. The "When NOT to use" section of a pattern is as important as "When to use".

For conflicts and over-engineering signals, read references/anti-patterns.md. For the full symptom table, see references/decision-tree.md.

## Output format

Before writing code, print this block (keep it 5-10 lines):

- Level: code | component | system
- Chosen: pattern (file) — why (one line)
- Rejected: pattern — why not (only if there were candidates)
- Consistency: matches project style | new project: adopted ...

Then write the code following the chosen pattern(s).
