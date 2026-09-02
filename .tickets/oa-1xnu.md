---
id: oa-1xnu
status: closed
deps: [oa-6swf, oa-fac4, oa-lnou]
links: []
created: 2026-07-08T14:42:17Z
type: bug
priority: 2
assignee: Jasmin Le Roux
parent: oa-xju6
---
# Wire Files and Terminal creation/focus to workspace identity

Files and Terminal actions from Home workspace detail and Start Work must focus existing tabs or create new tabs using explicit server/workspace identity.

## Design

Suggested default policies:
- Files: one Files tab per server/workspace by default; focus existing if present.
- Terminal: allow multiple PTYs, but show/focus recent terminal when selecting existing terminal; + Terminal creates a new PTY in target cwd.
- Chat: one Chat tab per session; New chat creates session in target workspace.

TabManager should expose find/focus helpers by server/workspace/route type.

## Acceptance Criteria

- Home workspace detail can focus existing Files tab for that workspace.
- + Files creates/focuses Files tab using explicit server/workspace.
- + Terminal creates PTY against the target server/workspace cwd, not global/default cwd.
- Tests cover duplicate prevention/focus behavior and explicit PTY cwd/server target.

