---
id: oa-ka08
status: closed
deps: [oa-6zta]
links: []
created: 2026-05-01T17:45:54Z
type: task
priority: 2
assignee: Jasmin Le Roux
parent: oa-gt0g
tags: [docs, workspace]
---
# AGENTS.md + openspec docs: forbidden patterns post-cutover

Document the 10 reward-hacking traps from the plan in AGENTS.md so future agents see them. Cover: no Workspace.global/DEFAULT, no directory: String? = null defaults, no fallback chains, no tabManager.activeTabWorkspace from data layer, no var workspace, no app-global currentWorkspace singleton, no parallel ChatMessageBuffer, no compatibility route silent guess, no *Global API variants, no withWorkspace { Workspace? }.

## Acceptance Criteria

1) AGENTS.md updated with Forbidden Patterns section. 2) openspec/AGENTS.md updated. 3) Each pattern has: example bad code, example good code, why it's bad.


## Notes

**2026-05-02T13:51:47Z**

Updated workspace forbidden-pattern docs.

Summary:
- Added Workspace Cutover Forbidden Patterns section to AGENTS.md.
- Created openspec/AGENTS.md with matching guidance because openspec/AGENTS.md did not exist in this checkout.
- Covered all 10 patterns with bad example, good example, and rationale:
  1. no Workspace.DEFAULT/global
  2. no directory: String? = null API defaults
  3. no directory fallback chains
  4. no data-layer active tab workspace access
  5. no mutable workspace variables
  6. no app-global current workspace singleton
  7. no parallel chat message buffers
  8. no compatibility route silent guessing
  9. no global API variants
  10. no nullable withWorkspace escape hatches.
