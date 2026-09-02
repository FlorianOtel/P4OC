---
id: oa-07lr
status: closed
deps: [oa-6swf, oa-97i6, oa-nugm]
links: []
created: 2026-07-08T14:42:17Z
type: feature
priority: 1
assignee: Jasmin Le Roux
parent: oa-xju6
---
# Implement Home workspace detail drill-in

Clicking a workspace card in Home opens a workspace detail view inside Home rather than immediately creating a tab.

Workspace detail is the bridge between existing sessions and contextual Files/Terminal/Chat actions.

## Design

Workspace detail content:
- Workspace identity: server badge/name and full directory.
- Open work in this workspace: existing chat/files/terminal tabs with focus actions.
- Filtered sessions in this workspace using current SessionList behavior where possible.
- Recent workspace activity if available (recent files, terminal status), but do not create a notification feed.
- Small Start new here row: + Chat, + Files, + Terminal, Pin.

Back behavior: workspace detail -> Home top-level.

## Acceptance Criteria

- Workspace card click drills into workspace detail without creating a new tab.
- Existing chat/files/terminal tabs for that workspace are detected and focusable.
- Filtered sessions list supports search/open and retains necessary session actions (rename/share/delete/summarize/view changes) or links to a full filtered Sessions view.
- Start new here actions delegate to Start Work coordinator with target prefilled.
- Tests cover workspace click no-tab-creation, focus existing tab, and start new here target prefill.

