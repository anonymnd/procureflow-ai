# Factory Method

## One-liner
Centralizes object creation: the client asks for a product, the factory decides how it is created.

## Symptoms
- Object creation with the same parameters is scattered (new everywhere).
- The product family varies by platform/environment.
- Creation logic is duplicated.

## Solution
- A factory method in a base class or a separate factory object.
- The client depends on the product interface, not concrete classes.
- The factory encapsulates selection and configuration.
- Creation lives in one place; clients stay stable.

## When to use
- Creation needs configuration that varies per environment.
- The product family may grow.
- Instead of: new in every call site.

## When NOT to use
- Creation is trivial: a constructor is enough.
- A single product class with no family.
- The choice is purely a wiring decision: use DI.

## Trade-offs
- vs constructor: indirection and centralization vs one expression.
- vs DI: factory is explicit per call site, DI is graph-level.
- vs abstract factory (v2): one product family vs several coordinated families.

## Common pitfalls
- Hiding a trivial constructor behind a factory (pure indirection).
- The factory doing more than creating (business logic inside).
- Concrete types leaking into the factory's public API.

## Related
- patterns/code/dependency-injection.md — when creation is part of the object graph.
- patterns/code/builder.md — when the product has many optional parameters.
- patterns/code/dto.md — when the product crosses a layer boundary.
