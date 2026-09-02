---
id: oa-cplr
status: closed
deps: []
links: [oa-77dh, oa-tmpy]
created: 2026-07-06T19:22:45Z
type: bug
priority: 1
assignee: Jasmin Le Roux
---
# Persist unsent chat drafts and attachments per tab

Problem:
The local busy-message queue has been removed in favor of upstream-submitted follow-ups, but unsent chat composer state remains local UI/ViewModel state. Typed-but-not-sent draft text and selected attachments may still be lost or incorrectly reused across tab/session recreation, process return, or file picker return.

Evidence:
The busy follow-up refactor deletes ChatUiState.queuedMessages and sends submitted prompts upstream immediately, so queued prompt loss is no longer a local persistence problem. Remaining user-authored state before Send still lives in ChatUiState.inputText and FilePickerManager attached-file state.

UX Constraint:
The chat composer is core workspace state. Draft text or selected attachments must not leak across sessions/workspaces/tabs. Missing/inaccessible attachment recovery is tracked separately in oa-tmpy.

Expected Behavior:
Unsent draft text and selected attachment references restore for the same tab/session/workspace when safe. Switching to a different session/workspace/tab does not inherit the prior draft or attachments. Unavailable restored attachment recovery is out of scope for this ticket and tracked by oa-tmpy.

Acceptance Criteria:
- Identify the current source of truth for unsent draft text, selected attachments, and file picker return state after the busy-follow-up queue refactor.
- Persist or save draft/attachment state keyed by existing tab/session/workspace identity without introducing global/default workspace fallbacks.
- Ensure switching sessions/workspaces/tabs does not leak draft text or attachments.
- Add focused behavior/ViewModel tests for same-key restoration, cross-key isolation, and clearing persisted state after successful send.
- Link unavailable restored attachment recovery to oa-tmpy rather than implementing it in this persistence pass.

Verification:
Run targeted ChatViewModel draft persistence tests plus compile and detekt. Smoke test typing a draft with an attachment, switching away/back, and returning from file picker.


## Notes

**2026-07-06T19:44:07Z**

Implemented minimal scoped composer persistence using the existing ChatViewModel SavedStateHandle: draft text and selected attachment references are restored for the same chat ViewModel scope, isolated by distinct SavedStateHandles, and cleared after successful send. Missing/inaccessible restored attachment recovery was intentionally not implemented in this pass to avoid overbuilding the persistence layer; follow-up tracked as oa-tmpy.
