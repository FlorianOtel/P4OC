---
id: oa-7ipn
status: closed
deps: []
links: []
created: 2026-07-08T14:42:17Z
type: task
priority: 1
assignee: Jasmin Le Roux
parent: oa-xju6
---
# Approval gate: multi-server architecture plan

Produce and get explicit approval for an architecture plan before touching core connection/repository lifecycle code.

Plan must cover:
- Server registry/config persistence.
- Multi-server connection registry versus current single ConnectionManager assumptions.
- Tab identity and persistence including server/workspace/route.
- Repository ownership/lifecycle keyed by server + workspace + generation.
- Home aggregation without eager-loading every session on every server.
- Start Work coordinator/context.

## Acceptance Criteria

- Architecture plan states invariants and forbidden patterns: no global/default current server for work actions; every tab/action has explicit server/workspace target.
- Plan includes migration path from current single-server behavior.
- Plan identifies which existing classes change: SettingsDataStore/server config, ConnectionManager, TabManager/TabState, MainTabScreen, SessionRepositoryProvider/WorkspaceRepositoryOwner, SessionListViewModel, Files/Terminal creation flows.
- Plan is approved before implementation tickets that depend on it begin.

