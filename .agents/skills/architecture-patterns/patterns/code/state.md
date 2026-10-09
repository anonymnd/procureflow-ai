# State

## One-liner
Encapsulates state-dependent behavior: behavior changes with the state, transitions are explicit.

## Symptoms
- if/else on a state field.
- Invalid state combinations are possible.
- Adding a state breaks several places.
- Transitions are scattered.

## Solution
- A state interface with the behaviors that differ by state.
- Each state is an object implementing the interface.
- The context holds the current state and delegates.
- Transitions are methods of the state object.

## When to use
- Behavior depends on a state with transitions.
- States are several and grow.
- Instead of: if/else on a status field.

## When NOT to use
- 2-3 states with rare transitions.
- The state is a simple enum with no behavior.
- The state is external (then a state machine library).

## Trade-offs
- vs if/else on enum: objects vs one place to look.
- vs strategy: state remembers where it came from, strategy does not.
- vs finite state machine library: explicit objects vs declarative table.

## Common pitfalls
- States that mutate the context outside the defined transitions.
- States that leak to each other (all states know all states).
- A state object that holds no state (then strategy is simpler).

## Related
- patterns/code/strategy.md — when there is no history, just variants.
- patterns/code/observer.md — when transitions must notify consumers.
- patterns/code/feature-flag.md — when the "state" is really an on/off branch.
