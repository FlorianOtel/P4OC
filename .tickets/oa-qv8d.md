---
id: oa-qv8d
status: closed
deps: []
links: []
created: 2026-07-06T12:52:06Z
type: task
priority: 1
assignee: Jasmin Le Roux
parent: oa-nwha
---
# Decide plus-button workspace semantics

Problem:
Implementation of oa-e6g3 was paused because the WorkspaceKey migration affects tab bar plus-button semantics. A mechanical migration could accidentally change plus-created Sessions/Terminal tabs from active-context siblings into always-Global tabs.

Evidence:
Brainstorm on 2026-07-06 converged that plus-button behavior is a product-intent decision, not just a storage refactor. Current code inherits active tab workspace for Sessions/Terminal; ticket notes contain the full decision pause.

Expected Behavior:
Before oa-e6g3 is implemented, choose and document the desired plus-button policy for Sessions, Terminal, Files, contextual opens, Global, Directory, SessionScoped, and legacy/missing workspace states.

Verification:
No production implementation required; verify by updating oa-e6g3 design notes/acceptance with the chosen policy and unblocking the ticket.

## Acceptance Criteria

- Decide whether plus-created Sessions and Terminal inherit the active tab WorkspaceKey or open as explicit Global.
- Decide Files plus-menu behavior: chooser, inherit, or Global.
- Decide how legacy/missing workspace tabs are recovered in UI.
- Update oa-e6g3 notes/acceptance with the chosen policy.
- Unblock oa-e6g3 after the decision is recorded.


## Notes

**2026-07-07T19:53:26Z**

Decision recorded: top-level tab-bar plus actions create explicit server/global views for Sessions and Terminal; they do not inherit the active tab workspace. Files remains chooser-first, offering Global plus currently open Directory workspaces. Contextual opens from an existing tab/session/file inherit the source WorkspaceKey. Legacy or missing workspace identity is not guessed as Global: ambiguous restored tabs are dropped during migration/restore, and remaining null workspace paths refuse workspace-critical actions instead of routing globally. Verified against MainTabScreen.kt top-level Sessions/Terminal creation using globalWorkspaceKey, Files chooser options, contextual terminal using the source tab workspaceKey, TabNavHost sub-session opens using workspaceOwner.workspace.key, and TabManager restore tests for null workspaceKey dropping.
