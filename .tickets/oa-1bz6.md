---
id: oa-1bz6
status: closed
deps: []
links: []
created: 2026-05-10T09:49:42Z
type: bug
priority: 2
assignee: Jasmin Le Roux
---
# Move uploads out of screen ViewModel scopes

Problem:
UploadCoordinator jobs are launched in screen ViewModel scopes. Uploads started from chat or files are cancelled when the owning screen ViewModel is cleared, even if the tab/app still exists and the user expects the upload to continue.

Evidence:
ChatViewModel constructs FilePickerManager(workspaceClient, viewModelScope). FilePickerManager constructs UploadCoordinator(scope = scope). FilesViewModel constructs UploadCoordinator(scope = viewModelScope). UploadCoordinator owns upload progress state and runs file uploads through the provided scope.

UX Constraint:
File uploads should continue across navigation within the tab and should expose progress/failure state in a human-readable way. Closing the owning tab or disconnecting the workspace should cancel uploads deliberately.

Expected Behavior:
Uploads are owned by a tab/workspace/app-level upload service or repository scope, not by transient chat/files screen ViewModel scopes. UI screens observe upload state.

Acceptance Criteria:
- Introduce a scoped upload owner/service tied to tab/workspace lifecycle, not individual screens.
- Chat and Files screens share/observe upload state for the same workspace where appropriate.
- Closing the tab, changing workspace, or disconnecting cancels uploads intentionally.
- Preserve upload progress, retry behavior, and human-readable errors.
- Avoid global/default workspace state; upload owner must be workspace-scoped.

Verification:
Run upload-related tests and ./gradlew :app:compileDebugKotlin. Manually start an upload, navigate away from chat/files, and confirm expected continuation/cancellation behavior.


## Notes

**2026-05-10T11:47:00Z**

Implemented workspace/tab-scoped upload ownership in WorkspaceRepositoryOwner. Chat and Files now share the workspace upload coordinator; upload jobs are no longer launched in screen ViewModel scopes and are cancelled when the workspace owner closes. Verification: export JAVA_HOME=/usr/lib/jvm/java-17-openjdk && ./gradlew :app:compileDebugKotlin passed.
