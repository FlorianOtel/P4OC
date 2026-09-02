---
id: oa-0f4m
status: closed
deps: []
links: []
created: 2026-05-01T17:44:25Z
type: task
priority: 1
assignee: Jasmin Le Roux
parent: oa-gt0g
tags: [design, workspace, sessions]
---
# Design lock C: optimistic mutation rollback contract

Commit 5 ports SessionListViewModel optimistic mutations as reducer intents w/ rollback. Underspecified: which mutations are optimistic (delete/rename/share/unshare/summarize?), what triggers rollback (HTTP error / timeout / SSE contradiction?), user-visible feedback (silent revert / snackbar / toast?), reducer shape (pending intent + confirm/reject events?), interaction with concurrent SSE events on the same entity.

## Acceptance Criteria

1) Per-mutation table: optimistic-or-not, local state transition, rollback trigger, user-visible feedback. 2) Reducer intent/confirm/reject shapes defined. 3) Concurrent SSE event semantics defined (server confirmation arrives before HTTP response — what wins?). 4) Stale-workspace mutation rejection behavior defined.


## Notes

**2026-05-01T18:19:06Z**

Decision locked. See docs/design-locks/C-mutation-on-failure.md
