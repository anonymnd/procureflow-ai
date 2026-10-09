# Mediator

## One-liner
Centralizes interactions between a group of objects: they talk to the mediator, not to each other.

## Symptoms
- Objects call each other in a web (A->B, B->C, C->A).
- Adding an object changes several others.
- The interaction graph is spaghetti.

## Solution
- A mediator object knows the group.
- The objects ask the mediator to coordinate.
- The objects do not reference each other.
- Interaction rules live in the mediator.

## When to use
- A stable group of objects interacts in a web.
- The interactions are a policy, not an accident.
- Instead of: direct cross-references.

## When NOT to use
- 2-3 objects (a direct call is clearer).
- One-directional interaction (no web).
- The interactions change per instance (a mediator is a shared policy).

## Trade-offs
- vs direct calls: a central point vs a readable graph.
- vs observer: mediator is two-way coordination, observer is one-way broadcast.
- vs pub-sub: mediator is local, pub-sub crosses modules.

## Common pitfalls
- A mediator that becomes a god object (all logic inside).
- Objects that still reference each other "for convenience".
- A mediator per group that duplicates the same policy.

## Related
- patterns/code/observer.md — when the interaction is one-way.
- patterns/code/pub-sub.md — when the boundary is a module.
- patterns/code/chain-of-responsibility.md — when the interaction is a sequence.
