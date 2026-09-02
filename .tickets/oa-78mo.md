---
id: oa-78mo
status: closed
deps: [oa-zmqg, oa-sa63]
links: []
created: 2026-07-09T19:49:06Z
type: task
priority: 1
assignee: Jasmin Le Roux
---
# Refocus Sessions on existing work

## Problem
Sessions prominently presents New Chat and Open Files creation cards, confusing the product split between browsing existing work and creating via `+`.

## Evidence / Repro
`current-phone-ux-audit.png` shows creation rows before the session list, while Home and Sessions compete as global landing surfaces.

## UX Constraint
Home and Sessions browse/resume existing work; persistent `+` creates new work. Sessions remains the search/history/actions surface and preserves exact server/workspace identity.

## Design

Sessions is contextual existing-work history, not a parallel creation dashboard. Reuse server badges and centralized status components.

## Acceptance Criteria

- Prominent New Chat/Open Files creation cards are removed from Sessions.
- Sessions leads with scoped search/filter/history and resumable existing sessions.
- Rows show durable server/workspace identity, status/recency, and explicit open/resume behavior.
- Global and workspace-filtered Sessions states are distinct without creating a hidden global server mode.
- Session actions preserve immutable ownership.
- Empty state directs creation through persistent + without duplicating a large creation panel.
- Current-device screenshot covers populated and empty states.
- Compile, detekt, and affected tests pass.

