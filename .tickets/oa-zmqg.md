---
id: oa-zmqg
status: closed
deps: []
links: []
created: 2026-07-09T19:47:49Z
type: task
priority: 0
assignee: Jasmin Le Roux
---
# Establish durable server names and badges

Problem:
Saved remote targets use the meaningless display name `Remote Server`, so tabs spanning servers cannot be safely distinguished.

Evidence:
`ServerViewModel.kt` hard-codes `Remote Server` for successful manual connections and persistence. Current device screenshots show the same label for saved and recent entries.

UX Constraint:
Server identity must be durable, user-editable, and distinct from connection status. Endpoint remains secondary detail. Never encode identity solely with status color.

## Design

Use discovery/mDNS service name when meaningful, then hostname, then host:port. Badge initials and accent must be deterministic from canonical server identity. Follow sharp dense TUI styling and resource-backed copy.

## Acceptance Criteria

- New saved servers require or derive a meaningful editable name from discovery name, hostname, or host:port; never `Remote Server`.
- Existing generic records receive a deterministic recognizable migration/fallback label.
- Every saved server has a deterministic compact badge identity reusable across Home, tabs, Sessions, server management, dialogs, and target pickers.
- Badge identity and status indicator are separate semantics.
- Rename behavior preserves endpoint identity and updates all consumers.
- Unit tests cover derivation, migration/fallback, stability, and collisions.
- Compile, detekt, and affected unit tests pass.


## Notes

**2026-07-09T20:11:41Z**

Implemented centralized ServerIdentity naming and deterministic badge semantics. Removed production `Remote Server`; new/manual/discovered and legacy saved records derive recognizable identity without changing canonical endpoint behavior. Added focused ServerIdentity and SavedServerRegistry coverage. Verified :app:compileDebugKotlin, :app:detekt, and :app:testDebugUnitTest pass.
