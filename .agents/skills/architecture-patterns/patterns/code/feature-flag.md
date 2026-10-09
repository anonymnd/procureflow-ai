# Feature Flag

## One-liner
Switches behavior on/off without changing the code path in production.

## Symptoms
- Shipping behavior before it is ready.
- Kill switch.
- Gradual rollout / A/B.

## Solution
- A flag with a name, a default, and a scope (user, percent, environment).
- The code checks the flag at the decision point.
- Flags have an owner and an expiry.
- Rollout is a flag value, not a deploy.

## When to use
- Release and deploy are decoupled.
- A rollback must not be a redeploy.
- Instead of: feature branches that live for weeks.

## When NOT to use
- A permanent branch (then strategy/state).
- A flag that is always on (dead code).
- A flag that guards a bug (fix the bug, do not flag it).

## Trade-offs
- vs deploy: two dimensions (deploy vs enable) vs one.
- vs configuration: a flag is a binary behavior switch, config is data.
- vs A/B framework: a flag is the primitive, the framework is the tool.

## Common pitfalls
- Flags that never expire (the codebase accumulates ghosts).
- A flag that guards a bug (hides the fix).
- Nested flags (flag of a flag) that become unreadable.

## Related
- patterns/code/strategy.md — when the branch is permanent.
- patterns/code/state.md — when the flag is really a lifecycle.
- patterns/system/strangler-fig.md — flags during a migration.
