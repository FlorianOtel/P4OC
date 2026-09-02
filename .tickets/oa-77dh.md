---
id: oa-77dh
status: closed
deps: []
links: [oa-wxf2, oa-12ui, oa-cplr, oa-tmpy]
created: 2026-07-05T18:07:13Z
type: bug
priority: 1
assignee: Jasmin Le Roux
parent: oa-nwha
---
# Preserve chat draft queued messages and attachments per session

Problem:
Chat draft text, queued messages, and attachment/input state are lifecycle-critical user-authored state. Audit found this state can be plain ViewModel/Compose state and may be lost or incorrectly reused across session/tab recreation.

Evidence:
Lifecycle audit identified ChatViewModel.kt draft/queued message/attachment state, ChatInputBar.kt input/attachment UI state, and FilePickerManager.kt picker state as restoration-critical. Chat scroll/follow-tail restoration is tracked separately in oa-wxf2; this ticket covers user-authored chat input and pending attachments.

UX Constraint:
The chat input is the core agent workspace. Losing an unsent prompt or queued attachment after tab switching, rotation, process recreation, or file-picker return is a user-data-loss bug. State must be scoped so one session's draft cannot appear in another session.

Expected Behavior:
Draft message text, selected/queued attachments, and pending send state restore for the same workspace/session/tab when safe. They remain isolated between different sessions and workspaces. If an attachment file is no longer available, the UI shows a human-readable recovery/removal state.

Acceptance Criteria:
- Identify the current source of truth for chat draft text, queued sends, selected attachments, and file picker return state.
- Persist or save draft/attachment state keyed by workspace/session/tab identity.
- Ensure switching to another session does not inherit the prior session's draft or attachments.
- Handle missing or inaccessible restored attachments with clear UI instead of silent drop or raw error payload.
- Add behavior/ViewModel tests for same-session restoration, cross-session isolation, and missing attachment recovery where seams exist.

Verification:
Run targeted ChatViewModel/ChatInputBar/FilePicker tests. Smoke test typing a draft with an attachment, switching away/back, and returning from file picker.


## Notes

**2026-07-06T19:23:04Z**

Busy follow-up queue scope was resolved by architecture change rather than persistence: Android-local queuedMessages/queueMessage/sendQueuedMessageIfAny/QueuedMessagesStrip were removed, and busy Send now submits upstream immediately. The visible queued state is derived from upstream transcript messages. Remaining unsent draft/attachment lifecycle persistence is split to oa-cplr.

**2026-07-07T11:19:29Z**

All acceptance criteria are now satisfied by the split implementation: oa-cplr persisted/restored draft text and selected attachment references via the chat SavedStateHandle with session/tab-scoped isolation tests, while oa-tmpy added workspace-scoped restored-attachment validation, unavailable/removable attachment UI state, send blocking before raw failures, and focused missing/inaccessible attachment recovery tests. Android-local busy queued messages were already removed in favor of upstream-submitted follow-ups, with visible queued state derived from transcript messages.
