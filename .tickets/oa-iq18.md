---
id: oa-iq18
status: closed
deps: [oa-zmqg, oa-sa63]
links: []
created: 2026-07-09T19:48:36Z
type: feature
priority: 1
assignee: Jasmin Le Roux
---
# Build scoped Home workspace detail

## Problem
Home workspace detail displays raw route strings and promises scoped browsing while its callbacks can perform global/current-server actions.

## Evidence / Repro
Current detail shows raw routes such as `chat/...`; Browse filtered sessions does not implement a filtered destination; Files and Terminal callbacks omit ownership.

## UX Constraint
Workspace click drills into Home without creating a tab. Detail is an existing-work surface first; creation actions are subordinate and always exactly scoped.

## Design

Use the approved workspace-detail structure in `home-workspace-detail-plus.html`. Reuse Home identity/status components and immutable scoped callbacks.

## Acceptance Criteria

- Detail header shows back, workspace name/path, durable server badge/name, and centralized status.
- Open work is represented as typed Chat/Files/Terminal cards with meaningful titles/status and Focus actions, never raw routes.
- Sessions are filtered to exact server/workspace, searchable when useful, and resumable.
- New chat/Files/Terminal actions are visually subordinate and invoke the shared coordinator with exact ownership.
- Back returns to the prior Home filter/scroll context without tab creation.
- Mixed-server same-directory behavior is tested.
- Current-device screenshot verifies populated detail.
- Compile, detekt, and affected tests pass.

