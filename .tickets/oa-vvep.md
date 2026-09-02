---
id: oa-vvep
status: closed
deps: []
links: []
created: 2026-05-01T17:44:25Z
type: task
priority: 1
assignee: Jasmin Le Roux
parent: oa-gt0g
tags: [design, workspace, sse]
---
# Design lock B: SSE hydrate-then-stream race semantics

SessionRepository hydrates initial snapshot via REST then layers SSE events. Race: events arriving DURING hydration can be lost or duplicated. Decide buffer-during-hydrate semantics: snapshot boundary, event identity for dedupe, max buffer size + overflow behavior, behavior on hydration failure, whether lifecycle events (Connected/Disconnected/Error) are buffered.

## Acceptance Criteria

1) Decision document covers: snapshot boundary definition, event identity field used for dedupe, max buffer size, overflow policy, hydration-failure recovery, lifecycle event handling. 2) Concrete reducer state shape sketched (Hydrating | Live | Stale). 3) Test cases enumerated for the test-infra ticket to implement. 4) Confirms/decides whether reducer accepts events while in Hydrating state.


## Notes

**2026-05-01T18:19:06Z**

Decision locked. See docs/design-locks/B-sse-hydrate-race.md
