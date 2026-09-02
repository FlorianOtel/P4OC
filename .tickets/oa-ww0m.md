---
id: oa-ww0m
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
# Design lock F: SSE event → workspace routing

One /global/event stream per server; per-workspace stores filter inbound events. Decide: filter on what field? Events without a directory field — broadcast to all workspaces, drop, or route by sessionID lookup? Permission events specifically (cross-tab — which workspace owns them)?

## Acceptance Criteria

1) Event-to-workspace routing rules table per event type. 2) Behavior for events without directory field defined. 3) Cross-tab permission/question routing defined (links to design-A). 4) Sub-agent / child-session event routing defined (does parent workspace see child events?). 5) Behavior when no workspace matches event defined.


## Notes

**2026-05-01T18:19:06Z**

Decision locked. See docs/design-locks/F-event-routing.md
