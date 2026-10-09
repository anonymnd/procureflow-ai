# Decorator

## One-liner
Adds behavior to an object at runtime, without touching the core class.

## Symptoms
- Adding caching/logging/timeout/retry without touching the core.
- Behaviors are combinable (cache + log + retry).
- Behavior varies per instance.

## Solution
- A decorator implements the same interface as the core.
- It holds the core and wraps each call.
- Behaviors compose (one decorator can wrap another).
- The core does not know it is wrapped.

## When to use
- Behavior is optional and combinable.
- The core must stay clean.
- Instead of: a class hierarchy of variants.

## When NOT to use
- A fixed set of variants at compile time.
- The behavior needs state across all instances (a decorator is per instance).
- One optional flag (a simple if is enough).

## Trade-offs
- vs subclass: runtime composition vs static hierarchy.
- vs middleware/chain: decorator wraps one object, a chain routes a request.
- vs AOP: decorator is explicit, AOP is woven.

## Common pitfalls
- A decorator that changes the interface (it must stay transparent).
- Decorators that cannot be combined (ordering is undefined).
- Wrapping the same instance in several decorators in the wrong order.

## Related
- patterns/code/chain-of-responsibility.md — routing through handlers.
- patterns/code/strategy.md — swapping algorithms, not adding behavior.
- patterns/system/retry-with-backoff.md — a decorator is the natural place for it.
