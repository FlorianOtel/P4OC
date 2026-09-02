---
id: oa-poi7
status: closed
deps: []
links: []
created: 2026-07-05T18:04:23Z
type: bug
priority: 1
assignee: Jasmin Le Roux
parent: oa-nwha
---
# Eliminate workspace null and global fallback leakage

Problem:
Workspace scoping audit found remaining null/global fallback leaks after the workspace/session cutover. These can reintroduce wrong-directory and multi-tab ambiguity even though the worst forbidden patterns are absent.

Evidence:
Positive findings: no Workspace.DEFAULT or Workspace.global(server), no app-global CurrentWorkspace singleton, no data-layer active-tab workspace access, no nullable withWorkspace escape hatch, no parallel chat message buffer, and OpenCodeApi directory params do not appear to use = null defaults. Remaining concerns include SessionRepositoryImpl.searchSessions(query, directory: String? = null) treating null as global plus every project worktree, scoped SessionRepositoryImpl.hydrate() still loading global and all project sessions, tests encoding broad null-directory search, createTab(... workspaceDirectory: String? = null) allowing global tabs by omission, and TabState defaulting workspaceDirectory to null.

UX Constraint:
Workspace/project identity must be visible enough to prevent wrong-directory mistakes, but persistent chrome should remain compact. Multi-tab behavior must never guess a directory from stale/global context.

Expected Behavior:
Every session, file, command, terminal, and tab operation uses the workspace owned by that tab/server. Server-global behavior is explicit and intentionally represented by a scoped null directory only when that is the selected workspace, not by omitted parameters or fallback chains.

Acceptance Criteria:
- Remove nullable default arguments that let callers omit workspace identity from repository/tab APIs.
- Make global/server-wide session search explicit in type/name/UI and separate from workspace-scoped search.
- Ensure SessionRepositoryImpl scoped hydration only hydrates the current workspace unless explicitly asked for server-global search.
- Require tab creation callers to pass an explicit workspace choice or route through a workspace-selection flow.
- Update tests that encode broad null-directory search to assert explicit scoped/global behavior.
- Add regression tests for two tabs on different workspaces showing isolated session/search results.

Verification:
Run targeted repository/tab manager/session list tests. Smoke test creating tabs for two directories and listing/searching sessions without cross-contamination.


## Notes

**2026-07-05T18:05:36Z**

Superseded by narrower workspace tickets for null-directory session search leakage and tab creation null-workspace defaults. The broad workspace category hid two different fixable behaviors.
