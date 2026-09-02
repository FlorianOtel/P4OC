---
id: oa-nam8
status: closed
deps: []
links: []
created: 2026-07-05T18:04:23Z
type: bug
priority: 1
assignee: Jasmin Le Roux
parent: oa-nwha
---
# Persist lifecycle-critical chat file terminal and picker state

Problem:
Audit found restoration-critical UI state stored in plain in-memory Compose/ViewModel state across chat, file, terminal, picker, command-palette, and session-list flows. Chat scroll restoration is tracked separately in oa-wxf2, but the same lifecycle-blind pattern affects other user state.

Evidence:
Additional audited areas include ChatViewModel.kt draft/queued message/attachment state, FilePickerManager.kt picker state, ChatInputBar.kt input attachment UI state, FileViewerScreen.kt unsaved edit buffer, FilesViewModel.kt and FileExplorerScreen.kt path/search/symbol/filter state, TerminalViewModel.kt and TerminalScreen.kt scrollback/emulator state, ModelAgentManager.kt selected model/reasoning state, CommandPalette.kt draft args, and SessionListScreen.kt/SessionListViewModel.kt search and tree expansion.

UX Constraint:
The app is an agent/chat/code workspace. Losing drafts, edit buffers, terminal context, search state, or file navigation after tab switches, process recreation, or configuration changes can cause wrong-directory mistakes and user data loss.

Expected Behavior:
Each piece of user-authored or restoration-critical state is scoped to the owning workspace/tab/session/file/terminal and survives the lifecycle events appropriate to its risk level. State that cannot be safely restored must fail transparently with a human-readable recovery path.

Acceptance Criteria:
- Classify audited state as ephemeral, saveable, persisted, or intentionally non-restorable.
- Persist chat draft/queued attachments per session/workspace where safe.
- Protect unsaved file edit buffers inside the app workspace; do not rely on external editors.
- Preserve file explorer path/search/symbol filters per tab/workspace where appropriate.
- Preserve terminal scrollback/current terminal identity to the extent feasible without fabricating process state.
- Preserve command palette draft args and session-list search/tree state when returning to the relevant tab.
- Add behavior tests for at least the high-risk data-loss cases: chat draft, unsaved file buffer, file explorer path/search, and terminal scrollback/identity.

Verification:
Use targeted ViewModel/SavedStateHandle tests and Compose behavior tests. For terminal/file editor cases, smoke test tab switch and recreation paths. Run compile after implementation.


## Notes

**2026-07-05T18:05:35Z**

Superseded by narrower lifecycle tickets to be created under oa-nwha. Chat scroll remains oa-wxf2; file editor buffers, terminal scrollback, file explorer state, and session list state need separate cohesive behavior tickets.
