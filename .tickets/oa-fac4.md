---
id: oa-fac4
status: closed
deps: [oa-6swf, oa-97i6, oa-z8r2]
links: []
created: 2026-07-08T14:42:17Z
type: feature
priority: 1
assignee: Jasmin Le Roux
parent: oa-xju6
---
# Replace plus dropdown with context-fast Start Work sheet

Replace the current tabbar + dropdown (New Sessions tab / New Files tab / New Terminal tab) with a context-fast Start Work sheet.

Design rule:
- Home = open existing/resume/browse.
- + = create/open new from the current context.

## Design

StartWorkContext should include:
- source: active tab, Home workspace detail, Home top-level, etc.
- default server/workspace from current tab or selected workspace.
- default action if invoked from an inline + Chat/+Files/+Terminal button.

Actions:
- New chat
- Files tab
- Terminal
- Browse sessions / choose existing
- Choose another target

If invoked on Home top-level with no selected workspace, show target picker first.
If invoked in workspace detail or work tab, default target is prefilled.

## Acceptance Criteria

- + no longer offers New Sessions tab as a primary action.
- From Chat/Files/Terminal, + defaults to that tab's server/workspace.
- From Home workspace detail, + defaults to selected workspace.
- User can choose another server/workspace target.
- Creating Files/Terminal/Chat uses explicit server/workspace and never falls back to hidden global current server.
- Tests cover default target derivation and create/focus behavior for each action.

