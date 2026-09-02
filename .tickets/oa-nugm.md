---
id: oa-nugm
status: closed
deps: [oa-6swf, oa-7ipn, oa-lnou]
links: []
created: 2026-07-08T14:42:17Z
type: feature
priority: 2
assignee: Jasmin Le Roux
parent: oa-xju6
---
# Create bounded Home session/workspace summary loading

Home needs summaries across servers/workspaces without eagerly loading every session history or subscribing to every repository forever.

## Design

Summary model should be lightweight:
- server id/badge
- workspace id/path/display
- session id/title/updated/status/presence
- open tab indicator
- counts for sessions/tabs where cheap

Loading policy:
- open tabs first
- pinned/recent workspaces next
- last N sessions per connected server/workspace
- lazy load full workspace detail on click
- broad search can call server-side/session APIs with explicit scope

## Acceptance Criteria

- Home top-level renders useful summaries without loading full chat message histories.
- One slow/offline server does not block Home summaries from other servers.
- Summary loading is cancellable/lifecycle-aware.
- Tests cover bounded loading and partial failure behavior.

