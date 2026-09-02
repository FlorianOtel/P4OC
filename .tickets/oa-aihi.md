---
id: oa-aihi
status: closed
deps: [oa-rde5, oa-ja73, oa-7ysx, oa-ww0m]
links: []
created: 2026-05-01T17:45:54Z
type: task
priority: 1
assignee: Jasmin Le Roux
parent: oa-gt0g
tags: [workspace, chat]
---
# Commit 4: Rewrite ChatViewModel against WorkspaceClient + SessionRepository, delete MessageStore

ChatViewModel ctor takes WorkspaceClient + SessionRepository + WorkspaceSession from SavedStateHandle. Delete MessageStore field; observe SessionRepository.messages(WorkspaceSession). Delete loadSession side effect on DirectoryManager. Delete file:// literal at line 477 → AttachmentRef.File(WorkspacePath(...)). Delete getDirectory() helper. Permission/question responses derive workspace from session per design-A and design-F. DELETE MessageStore.kt.

## Acceptance Criteria

1) ChatViewModel has no DirectoryManager dependency. 2) loadSession does NOT call directoryManager.setDirectory. 3) MessageStore.kt does NOT exist. 4) No 'file://' string literal in ui/screens/chat/. 5) Manual smoke: open session, send 'hello', stream returns. 6) Manual smoke: attach file with relative path, server receives correct relative path (verify with logging). 7) Manual smoke: open child session — workspace preserved. 8) Existing MessageStoreTest behavior is REPLACED in SessionReducerTest.


## Notes

**2026-05-02T12:08:52Z**

Implemented ChatViewModel workspace rewrite.

Summary:
- ChatViewModel now receives WorkspaceClient + SessionRepositoryImpl and no longer depends on DirectoryManager.
- Removed loadSession side effect that wrote to DirectoryManager.
- Moved message state/update behavior into SessionRepositoryImpl.messages(...), loadMessages(...), clearStreamingFlags(...), and acceptEvent(...).
- Deleted MessageStore.kt.
- Replaced MessageStoreTest with SessionRepositoryMessageStateTest.
- Removed file:// attachment literal in chat; attachments now use WorkspacePath.Relative(RelativePath(...)).toAttachmentUrl().
- Chat API calls now go through WorkspaceClient for send, command, permissions/questions, todos, VCS, revert/unrevert, abort, and command listing.
- TabNavHost wires ChatViewModel using the per-tab WorkspaceViewModel workspaceClient/sessionRepository.

Verification:
- export JAVA_HOME=/usr/lib/jvm/java-17-openjdk && ./gradlew :app:compileDebugKotlin: BUILD SUCCESSFUL
- export JAVA_HOME=/usr/lib/jvm/java-17-openjdk && ./gradlew :app:testDebugUnitTest --tests 'dev.blazelight.p4oc.ui.screens.chat.*' --tests 'dev.blazelight.p4oc.data.session.*': BUILD SUCCESSFUL
- export JAVA_HOME=/usr/lib/jvm/java-17-openjdk && ./gradlew :app:testDebugUnitTest: BUILD SUCCESSFUL

Greps:
- No DirectoryManager/MessageStore/file:// usages remain under ui/screens/chat/.
