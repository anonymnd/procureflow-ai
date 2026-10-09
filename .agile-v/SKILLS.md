# Installed skills and local adaptation

Installed: 2026-10-09 under `.agents/skills/`. Authority: `APR-SETUP-001`. Original downloaded skill files and supporting references are retained. These are coding-agent instructions, not runtime application libraries or assurances of production readiness.

| Source repository | Pinned commit | Installed paths under `.agents/skills/` | Use |
|---|---|---|---|
| [Agile-V/agile_v_skills](https://github.com/Agile-V/agile_v_skills) | `4bcc9172f7e00fa3f04bd249584d537a7dbe5600` | `agile-v-core`, `requirement-architect`, `logic-gatekeeper`, `system-understanding-agent`, `impact-analysis-agent`, `test-designer`, `red-team-verifier` | Workflow, requirements/review, source understanding, impact, approved-requirement tests and verification |
| [netsky-prod/architecture-patterns](https://github.com/netsky-prod/architecture-patterns) | `41a8a78a800366e21b3ed58b61bff3bd79859773` | `architecture-patterns` | Symptom-driven pattern selection; default no new pattern |
| [snepraj2709/system-design-first-principles-skill](https://github.com/snepraj2709/system-design-first-principles-skill) | `d1a965c8bf8e5782e54f3b6ec0e18660f160ec37` | `system-design-first-principles` | Constraints, alternatives, failure modes and measured scaling |
| [github/awesome-copilot](https://github.com/github/awesome-copilot) | `82701c24b99488536ca399ff4789a458b7a05db7` | `java-springboot` | Spring conventions, adapted to actual Boot 4.1.1 APIs and project dependencies |
| [dualform-labs/spec-skill](https://github.com/dualform-labs/spec-skill) | Existing installation retained; original revision not established here | `spec` | Pre-implementation specification; separate implementation gate applies |

Agile V skill frontmatter attributes agile-v.org and CC-BY-SA-4.0; architecture-patterns declares MIT. Preserve source notices and consult each source repository for its license. Attribution and installed file fingerprints are recorded in `traceability/skill-installation.json`. No upstream installer shell scripts were executed; the Codex skill-installer selectively downloaded only the approved directories.

## Intentional local differences

[AGENTS.md](../AGENTS.md) defines the approved lightweight adaptation: consolidated Markdown records instead of all upstream canonical artifacts; direct inspection rather than graph tooling; explicit requirement and implementation gates; provenance in this manifest/state/evidence rather than regulated AI-BOM/control frameworks. Upstream documents may link to companion skills/runtime contracts not installed; do not automatically fetch those or introduce dependencies.

Core 1.10 describes fresh context as at most I1; red-team-verifier 1.9 uses I2 for a role-separated invocation. ProcureFlow conservatively reports a fresh-context review as I1 and describes actual separation instead of asserting stronger assurance. Formal test-designer execution starts only after a requirement is approved and baselined; current test plans are draft proposals, not approved verification results.

No build-agent, compliance/regulated skills, graph tooling, new Maven libraries, or infrastructure were installed. Skills become available to Codex on the next turn; future updates require comparing upstream changes and updating the pin, rather than silently following `main`.
