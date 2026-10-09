# Dependency Injection

## One-liner
A class receives collaborators from outside instead of creating them itself.

## Symptoms
- Collaborators are created inside the class (new everywhere).
- Hard to test without real collaborators.
- Swapping implementations is impossible.
- A deep object graph is built manually.

## Solution
- Dependencies arrive via constructor or method parameters.
- Wiring happens at a composition root (manual or container).
- The class declares only the interface it needs.
- The concrete choice lives outside the class.

## When to use
- Collaborators have alternatives (real/fake, v1/v2).
- The class must be tested without its real collaborators.
- Instead of: hardcoded new and service locators.

## When NOT to use
- A small script with one entry point.
- The collaborator is a simple value (a literal is enough).
- There is exactly one implementation and it will never change.

## Trade-offs
- vs service locator: explicit dependencies vs hidden lookup.
- vs hardcoding: testability vs fewer moving parts.
- Manual wiring vs container: explicitness vs automation.

## Common pitfalls
- Injecting more than the class actually uses.
- The class silently creating a fallback collaborator (hidden dependency).
- Confusing DI with a service locator.

## Related
- patterns/code/factory-method.md — when creation logic is more than a choice.
- patterns/code/repository.md — the classic injected collaborator.
- patterns/component/hexagonal.md — DI is the mechanism behind the ports.
