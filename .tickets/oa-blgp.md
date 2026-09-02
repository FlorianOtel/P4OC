---
id: oa-blgp
status: closed
deps: []
links: []
created: 2026-05-01T17:44:25Z
type: task
priority: 1
assignee: Jasmin Le Roux
parent: oa-gt0g
tags: [design, workspace, nav]
---
# Design lock E: route encoding for Workspace + SessionId

Plan deletes chat/{sessionId}?directory={directory} but doesn't say what replaces it. Options: typed Compose-nav routes (Navigation 2.8+), URL-encoded JSON arg, plain {tabId} with workspace held in tab-scoped state. Each has different deep-link/persistence implications. Decide and document.

## Acceptance Criteria

1) Route shape chosen and rationale documented. 2) Deep link behavior for old routes spec'd (links to design-A). 3) Path encoding for paths-with-spaces, %, ?, #, /, unicode handled. 4) SavedStateHandle vs route-args boundary defined. 5) Sub-session route behavior defined (currently Screen.Chat.createRoute(subSessionId) with no directory).


## Notes

**2026-05-01T18:19:06Z**

Decision locked. See docs/design-locks/E-route-encoding.md
