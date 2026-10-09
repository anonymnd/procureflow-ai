# Plugin Architecture

## One-liner
The core defines an extension point; plugins are loaded at runtime and extend behavior.

## Symptoms
- The core must be extended by third parties.
- Different versions of behavior.
- The core grows with every extension.

## Solution
- A core with a well-defined extension point (an interface + a discovery mechanism).
- Plugins implement the extension point.
- Plugins are discovered (directory, registry, entry points).
- The core does not know the plugin list at compile time.

## When to use
- Extensions come from outside the team.
- The core must ship without the extensions.
- Instead of: a growing if/else inside the core.

## When NOT to use
- A closed set of extensions (a strategy is enough).
- The extensions need deep core internals (the boundary is a lie).
- The core is small and stable.

## Trade-offs
- vs strategy: runtime loading vs compile-time choice.
- vs microservices: in-process extension vs networked service.
- vs configuration: plugins add behavior, config changes data.

## Common pitfalls
- Plugins reaching into the core's internals (the boundary is a lie).
- No versioning of the extension point (plugins break the core).
- A plugin that is really a second core (two brains).

## Related
- patterns/code/strategy.md — when the extensions are closed and known.
- patterns/code/feature-flag.md — when the extension is an optional behavior, not a module.
- patterns/component/microservices.md — when the extension crosses a process.
