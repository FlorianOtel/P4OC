---
id: oa-8dfl
status: closed
deps: []
links: []
created: 2026-05-10T09:55:50Z
type: task
priority: 2
assignee: Jasmin Le Roux
---
# Remove pseudo file URI attachment path codec

Problem:
Chat file attachments wrap workspace-relative paths in a pseudo file:/// URI and URL-encode path segments before sending them in JSON. This adds URI parsing/encoding failure modes for data that can be represented as raw JSON strings.

Evidence:
WorkspacePathAttachmentCodec.kt defines WorkspacePath.Relative.toAttachmentUrl() as file:/// plus encoded path segments and parseFromServer() to decode file:// values. ChatViewModel uses WorkspacePath.Relative(RelativePath(file.path)).toAttachmentUrl() for attachment url. FileExplorerScreen parses symbol.uri through WorkspacePathAttachmentCodec.parseFromServer().

UX Constraint:
Workspace file paths with spaces, unicode, punctuation, or platform-specific characters should round-trip without brittle URI semantics. Do not leak workspace directories or use navigation-route encoding rules for API payloads.

Expected Behavior:
Attachment DTOs use raw workspace-relative path strings unless the OpenCode backend explicitly requires URI-formatted attachment URLs. Any server-required format should be isolated and documented.

Acceptance Criteria:
- Confirm OpenCode API expected attachment url/path format.
- If raw strings are accepted, delete WorkspacePathAttachmentCodec and send raw relative paths in JSON bodies.
- Update server symbol/file click parsing to avoid unnecessary file:// decoding where possible.
- Add tests for paths with spaces, percent signs, unicode, query/hash-like characters, and slashes.

Verification:
Run mapper/chat/file tests and ./gradlew :app:compileDebugKotlin. Manually attach/open files with special characters in names.


## Notes

**2026-05-10T12:25:27Z**

Confirmed OpenCode FilePartInput.url examples and prompt processor require file:/file:// or data: URLs, not raw relative paths. Removed shared WorkspacePathAttachmentCodec; chat now builds the backend-required file URL locally from the scoped workspace directory, and symbol results expose decoded relative paths separately. Verified with targeted unit tests and :app:compileDebugKotlin.
