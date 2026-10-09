# Chain of Responsibility

## One-liner
A request passes along a chain of handlers; each may handle it or pass it on.

## Symptoms
- Which handler serves a request is not known up front.
- if/else on handler type.
- Adding a handler changes the caller.

## Solution
- A handler interface: handle(request) or pass to the next.
- Handlers are linked in a chain (order is explicit).
- Each handler either handles or delegates to the next.
- The caller hands the request to the chain entry.

## When to use
- A request may be handled by several candidates.
- The handling order is meaningful.
- Instead of: a big if/else router.

## When NOT to use
- A single handler (no chain).
- Fixed routing to one handler (a direct call).
- The handlers are mutually exclusive by design (a router is clearer).

## Trade-offs
- vs if/else router: extensibility vs one place to look.
- vs strategy: a chain is an ordered sequence, strategy is one choice.
- vs event bus: a chain is a request (one handler acts), an event is a broadcast.

## Common pitfalls
- A chain where every handler always passes (no handler acts).
- Handlers that depend on the chain's position (order becomes hidden coupling).
- A request type that is a bag of everything (handlers pick from a mess).

## Related
- patterns/code/strategy.md — when exactly one variant must act.
- patterns/code/command.md — the object that travels the chain.
- patterns/code/mediator.md — when the web of handlers must be centralized.
