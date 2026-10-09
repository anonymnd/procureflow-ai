# Decision tree (full)

Expanded routing table. `SKILL.md` has the one-line version; this file has full symptom lists and "not when" notes. Use it when the one-line symptom in `SKILL.md` is not enough to decide.

## Contents
- Code level (24 patterns)
- Component level (8 patterns)
- System level (10 patterns)

## Code level

### strategy (patterns/code/strategy.md)
Symptoms:
- if/else or switch on a "type"/"variant" field
- adding a new variant edits existing code
- behavior is chosen at runtime (user input, config, environment)
- the same branching is repeated in several places
Not when: two variants with stable logic — if/else is enough.

### observer (patterns/code/observer.md)
Symptoms:
- "when A changes, update B, C, D"
- A accumulates knowledge about B, C, D
- adding a consumer changes A
- the same notification is duplicated in several places
Not when: a single consumer (call directly); ordering between consumers matters.

### factory-method (patterns/code/factory-method.md)
Symptoms:
- object creation with the same parameters is scattered (new everywhere)
- the product family varies by platform/environment
- creation logic is duplicated
Not when: creation is trivial, there is a single product class.

### builder (patterns/code/builder.md)
Symptoms:
- a constructor with 4+ parameters
- telescoping overloads
- an object with mostly-default fields
- configuration via a chain of setters after construction
Not when: 3 or fewer meaningful parameters.

### dependency-injection (patterns/code/dependency-injection.md)
Symptoms:
- collaborators are created inside the class (new everywhere)
- hard to test without real collaborators
- swapping implementations is impossible
- a deep object graph is built manually
Not when: a small script with one entry point; trivial collaborators.

### repository (patterns/code/repository.md)
Symptoms:
- data access calls live inside business code
- business logic cannot be tested without the database
- storage may need to be swapped
- queries are scattered and duplicated
Not when: simple CRUD over a single table.

### unit-of-work (patterns/code/unit-of-work.md)
Symptoms:
- manual "save this, then that"
- partial commits on failure
- the same object is saved twice (identity problems)
- lost updates across entities
Not when: one entity per request; read-only work.

### data-mapper (patterns/code/data-mapper.md)
Symptoms:
- the model knows about tables/columns
- persistence details live in the domain
- model == table and any change ripples
- storage formats leak into business code
Not when: the model truly is the table (then active record is acceptable).

### dto (patterns/code/dto.md)
Symptoms:
- domain entities appear in API responses
- clients depend on internal structure
- accidental mutation of shared objects
- different consumers need different shapes
Not when: an internal layer with a single consumer.

### specification (patterns/code/specification.md)
Symptoms:
- the same WHERE/conditions are duplicated
- "findXWhereY" methods are proliferating
- conditions are combined ad hoc
- criteria differ slightly per call
Not when: 1-2 queries.

### value-object (patterns/code/value-object.md)
Symptoms:
- an "entity" that has no lifecycle
- equality should be by value, not by id
- shared mutable data (address, money, period)
Not when: there is a real lifecycle (then entity); the value is a primitive.

### aggregate (patterns/code/aggregate.md)
Symptoms:
- invariants span several entities
- partial updates / inconsistent state
- concurrent writes to the same data
- "which entity owns this rule?" is unclear
Not when: a small domain with no cross-entity invariants; reads dominate.

### facade (patterns/code/facade.md)
Symptoms:
- the client uses several classes for one operation
- the subsystem API is wide
- the client must know subsystem internals
Not when: the subsystem is simple; the client needs fine-grained control.

### adapter (patterns/code/adapter.md)
Symptoms:
- integrating an API you cannot change
- a legacy interface is different from yours
- several external services, similar but different
Not when: you can change the source directly.

### decorator (patterns/code/decorator.md)
Symptoms:
- add caching/logging/timeout/retry without touching the core
- behaviors are combinable (cache + log + retry)
- behavior varies per instance
Not when: a fixed set of variants at compile time.

### state (patterns/code/state.md)
Symptoms:
- if/else on a state field
- invalid state combinations are possible
- adding a state breaks several places
- transitions are scattered
Not when: 2-3 states with rare transitions.

### template-method (patterns/code/template-method.md)
Symptoms:
- the same algorithm in several places with small step differences
- variants differ in 1-2 steps
- the order of steps must be preserved
Not when: the differences are large (then strategy).

### command (patterns/code/command.md)
Symptoms:
- actions must be queued, delayed, or logged
- undo/redo is required
- actions must be replayed or audited
Not when: a simple synchronous call with no history.

### chain-of-responsibility (patterns/code/chain-of-responsibility.md)
Symptoms:
- which handler serves a request is not known up front
- if/else on handler type
- adding a handler changes the caller
Not when: a single handler; fixed routing.

### mediator (patterns/code/mediator.md)
Symptoms:
- objects call each other in a web (A->B, B->C, C->A)
- adding an object changes several others
- the interaction graph is spaghetti
Not when: 2-3 objects; one-directional interaction.

### memoization (patterns/code/memoization.md)
Symptoms:
- an expensive pure computation repeats with the same arguments
- the function is pure (no side effects)
- arguments are hashable/comparable
Not when: a cheap computation; an impure function; unbounded arguments.

### cache-aside (patterns/code/cache-aside.md)
Symptoms:
- repeated expensive fetches (database, API)
- N+1 queries
- TTL/invalidation questions
- the data changes rarely
Not when: the data is cheap; strong consistency is required.

### pub-sub (patterns/code/pub-sub.md)
Symptoms:
- the producer must know the consumers
- cross-module coupling through direct calls
- adding a consumer changes the producer
Not when: a single consumer; a response is required (synchronous call).

### feature-flag (patterns/code/feature-flag.md)
Symptoms:
- shipping behavior before it is ready
- kill switch
- gradual rollout / A/B
Not when: a permanent branch (then strategy/state).

## Component level

### hexagonal (patterns/component/hexagonal.md)
Symptoms:
- domain code imports framework/DB/API specifics
- the core cannot be tested without infrastructure
- swapping the database or API means a rewrite
- the framework dictates the structure
Not when: a small app where the framework is the app.

### clean-architecture (patterns/component/clean-architecture.md)
Symptoms:
- the domain depends on the UI/DB
- layers are unclear; "where does this code go?" is a recurring question
- the framework leaks into business logic
Not when: a small project.

### layered (patterns/component/layered.md)
Symptoms:
- everything lives in one layer ("god service")
- the UI talks to the database directly
- business logic sits in the controller
Not when: a small script; two layers are enough.

### modular-monolith (patterns/component/modular-monolith.md)
Symptoms:
- a "big ball of mud"
- modules touch each other's internals
- one change breaks everything
- deploy coupling; no clear ownership
Not when: a small app (one module is enough).

### event-driven (patterns/component/event-driven.md)
Symptoms:
- synchronous call chains between modules
- cascading failures
- latency under load
- a module must react to many things
Not when: a simple request-response.

### microservices (patterns/component/microservices.md)
Symptoms:
- teams cannot ship independently
- a deploy bottleneck
- scale by part
- the monolith is a deploy risk
Not when: one team; a small domain.

### plugin-architecture (patterns/component/plugin-architecture.md)
Symptoms:
- the core must be extended by third parties
- different versions of behavior
- the core grows with every extension
Not when: a closed set of extensions.

### bff (patterns/component/bff.md)
Symptoms:
- web and mobile need different slices of the same data
- over-fetching
- the client assembles many calls where one is enough
Not when: a single client.

## System level

### monolith-vs-microservices (patterns/system/monolith-vs-microservices.md)
Symptoms:
- a decomposition choice at project start
- a monolith that is hard to change
- teams are blocked by each other
- scale/deploys are uneven
Not when: a small project (then plain monolith).

### cqrs (patterns/system/cqrs.md)
Symptoms:
- complex write invariants
- divergent read projections
- reads and writes scale differently
- one model does not fit both
Not when: simple CRUD.

### event-sourcing (patterns/system/event-sourcing.md)
Symptoms:
- an audit of changes is required
- temporal queries ("what was the state in March?")
- state must be rebuildable
- complex invariants over time
Not when: you do not need history.

### outbox (patterns/system/outbox.md)
Symptoms:
- a database write plus a message publish must be atomic
- events lost on crash
- duplicated/lost events
Not when: the message is optional.

### saga (patterns/system/saga.md)
Symptoms:
- a process spans 3+ services
- failure requires compensation
- a multi-step or multi-day process
Not when: one service (a local transaction is enough).

### circuit-breaker (patterns/system/circuit-breaker.md)
Symptoms:
- a slow/failing dependency drags down the caller
- timeouts accumulate
- cascading failures
Not when: the dependency is local.

### retry-with-backoff (patterns/system/retry-with-backoff.md)
Symptoms:
- transient failures cause real errors
- retries without backoff create a storm
Not when: a non-idempotent operation; a permanent failure.

### bulkhead (patterns/system/bulkhead.md)
Symptoms:
- one slow dependency exhausts the shared pool
- all requests wait
- cascading exhaustion
Not when: a single dependency.

### idempotency (patterns/system/idempotency.md)
Symptoms:
- retries produce duplicates
- at-least-once delivery
- processes get re-executed
Not when: at-most-once is acceptable; read-only operations.

### strangler-fig (patterns/system/strangler-fig.md)
Symptoms:
- a legacy system must be replaced
- a big-bang rewrite is too risky
- the legacy must keep working during migration
Not when: a small legacy (a rewrite is enough).
