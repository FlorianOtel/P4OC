---
id: oa-lnou
status: closed
deps: [oa-6swf, oa-7ipn, oa-0gah, oa-z8r2]
links: []
created: 2026-07-08T14:42:17Z
type: bug
priority: 1
assignee: Jasmin Le Roux
parent: oa-xju6
---
# Scope repositories and data owners by server and workspace

Repository/data-owner lifecycle must be keyed by server + workspace + generation, not just directory or active tab state.

Home aggregates across servers/workspaces, while Chat/Files/Terminal tabs consume scoped repositories. Duplicate paths on different servers must not share state.

## Design

Update SessionRepositoryProvider/WorkspaceRepositoryOwner patterns so ownership key includes:
- ServerRef / server id / endpoint key
- WorkspaceKey/directory/session/global
- ServerGeneration where relevant

Avoid forbidden patterns:
- Pulling active tab workspace from unrelated UI state inside repository constructors.
- Mutable global workspace variables.
- Fallback directory chains.

## Acceptance Criteria

- Same directory string on two servers creates distinct repository owners and data streams.
- Closing one tab does not close a repository still used by another tab/home detail for same server/workspace.
- Server reconnect/generation changes recreate only affected server/workspace owners.
- Tests cover duplicate directory on two servers and shared owner ref-count/lifecycle behavior.

