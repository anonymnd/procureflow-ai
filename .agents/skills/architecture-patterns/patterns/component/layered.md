# Layered Architecture

## One-liner
The codebase is split into layers; each layer talks only to the layer below.

## Symptoms
- Everything lives in one layer ("god service").
- The UI talks to the database directly.
- Business logic sits in the controller.

## Solution
- Layers: presentation, business, data access.
- A layer depends only on the layer below.
- Each layer has its own vocabulary (DTOs at the boundary).
- A request flows down, a response flows up.

## When to use
- The app has at least three concerns that must not mix.
- The team needs a simple, known layout.
- Instead of: a god service.

## When NOT to use
- A small script.
- Two layers are enough.
- The layers are just a formality (the code already crosses them).

## Trade-offs
- vs hexagonal: a fixed layering vs ports and adapters.
- vs flat: a boundary vs fewer objects.
- vs microservices: layers inside one deployable vs services.

## Common pitfalls
- The "business" layer doing data access (layer bleed).
- The presentation layer holding rules.
- A DTO for every call (the layers become a ceremony).

## Related
- patterns/component/clean-architecture.md — when the layers need a strict rule.
- patterns/component/hexagonal.md — when the core must be framework-free.
- patterns/code/dto.md — the shape at each layer boundary.
