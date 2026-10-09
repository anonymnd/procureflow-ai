# Command

## One-liner
Turns an action into an object: the action can be queued, delayed, logged, or undone.

## Symptoms
- Actions must be queued, delayed, or logged.
- Undo/redo is required.
- Actions must be replayed or audited.

## Solution
- A command object encapsulates the action (parameters inside).
- The sender hands the command to a receiver.
- The receiver stores/queues/executes the command.
- Undo is either a paired inverse or a captured state.

## When to use
- Actions need a lifecycle beyond the call (queue, delay, audit).
- Undo/redo is a requirement.
- Instead of: direct calls with side channels.

## When NOT to use
- A simple synchronous call with no history.
- The action cannot be captured (external side effect, no inverse).
- One action, one sender, one receiver.

## Trade-offs
- vs direct call: lifecycle vs one expression.
- vs event: a command is "do X now or later", an event is "X happened".
- vs queue of functions: objects are inspectable and serializable.

## Common pitfalls
- Commands that are just lambdas with no parameters (then a closure is enough).
- Undo implemented by "redo in reverse" without an inverse.
- Commands that hold state instead of parameters.

## Related
- patterns/code/chain-of-responsibility.md — when the receiver is chosen at runtime.
- patterns/code/observer.md — when the "command" is really a broadcast.
- patterns/system/idempotency.md — when commands are replayed.
