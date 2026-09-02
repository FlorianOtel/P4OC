---
id: oa-tmpy
status: closed
deps: []
links: [oa-cplr, oa-77dh]
created: 2026-07-06T19:43:41Z
type: bug
priority: 2
assignee: Jasmin Le Roux
---
# Show recovery state for unavailable restored chat attachments

Problem:
Unsent chat composer attachments can now be restored from SavedStateHandle, but restored attachment references are not validated for availability. If a referenced workspace file was moved, deleted, or became inaccessible, the composer may restore a stale chip and only fail later when sending.

Evidence:
oa-cplr intentionally implemented the minimal persistence/isolation scope using SavedStateHandle for input text and SelectedFile references. It does not add an unavailable attachment state or validation path.

UX Constraint:
Attachment recovery must be human-readable and removable. The user should not see raw protocol errors or silently lose restored attachments.

Expected Behavior:
When restored selected attachments are missing or inaccessible, the composer shows an unavailable/removable attachment state or a clear error before Send. Available attachments continue to send normally.

Acceptance Criteria:
- Validate restored selected attachment references against the current workspace/session context before send or when the composer restores.
- Missing/inaccessible attachments show clear UI text and a remove action.
- Send does not silently drop stale attachments or surface raw protocol/JSON errors.
- Add focused tests for available restored attachment, unavailable restored attachment, and remove unavailable attachment.

Verification:
Restore a draft with one existing and one missing attachment; confirm the missing one is visibly recoverable/removable and the existing one still sends.


## Notes

**2026-07-07T11:18:02Z**

Implemented restored attachment recovery with SelectedFile.available, workspace-scoped parent-directory validation in FilePickerManager, unavailable chip styling/label in ChatInputBar, and send blocking with a concise composer error while unavailable attachments remain. Added focused ChatViewModelDraftPersistenceTest coverage for available restored attachments, missing restored attachments, removal clearing the blocker, and validation failures.
