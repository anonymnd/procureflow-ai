# Anti-patterns and over-engineering signals

Read this file when: (a) you are about to combine multiple patterns in one task, (b) a task seems to need more than 3 patterns, (c) you are unsure whether a pattern is justified, (d) you are refactoring and the code smells "patterned".

## Over-engineering signals

If two or more of these are true, stop and re-check the symptoms:

- The pattern is introduced "for the future" (there is no current symptom).
- The task changes fewer than ~50 lines of code.
- The pattern adds a class or interface that nothing currently implements.
- You cannot name the concrete symptom that the pattern solves.
- You cannot say how a plain implementation would fail.
- You are about to combine 4+ patterns in one task.

Default resolution: write the plain implementation and note where it would need to change when the symptom appears.

## Conflicting combinations

| Combination | Problem | Resolution |
|---|---|---|
| Repository + active record | Two persistence abstractions fighting | Pick one: repository (clean model) or active record (model == table) |
| Microservices without module boundaries | Distributed complexity with none of the benefits | Start with a modular monolith, then extract |
| Event-driven + synchronous calls in a cycle | Cycles through events and calls create timing messes | One direction: events between modules, calls only inward |
| CQRS on simple CRUD | Two models for one table | Plain single model; CQRS only with divergent read/write needs |
| Saga for a single-service process | Distributed machinery for a local transaction | Local transaction + unit of work |
| Microservices for one team | Coordination cost without team boundaries | Monolith or modular monolith |
| Observer + mediator + pub-sub all at once | Three coupling mechanisms for one event | Pick one by scope: local (observer), module (mediator), system (event-driven) |
| Feature flag as a permanent branch | Flags outlive their purpose | Convert to strategy/state or delete |
| Hexagonal with framework in the domain | Ports exist but the core still imports the framework | Move framework code to adapters |

## Choosing between similar patterns (quick disambiguation)

- Strategy vs state: strategy = interchangeable algorithms without history; state = transitions with history (state remembers where it came from).
- Observer vs pub-sub: observer = local objects in one scope; pub-sub = cross-module, topics, delivery semantics.
- Decorator vs middleware/chain: decorator wraps one instance with behavior; a chain routes a request through handlers.
- Repository vs data mapper: repository = collection API and queries; data mapper = mapping model to storage. Often used together.
- Facade vs adapter: facade simplifies your own subsystem; adapter translates someone else's interface.
- Caching: memoization (pure function) vs cache-aside (external data).

## When to stop

- "We should use X because it is best practice" — no. Symptom first.
- "The architecture should be future-proof" — no. Simplicity first; patterns are added when symptoms appear.
- One project = one dominant style. If the project is a modular monolith with repository and unit of work, new code continues that style unless symptoms say otherwise.
