---
id: oa-udp9
status: closed
deps: []
links: []
created: 2026-05-10T09:50:09Z
type: task
priority: 2
assignee: Jasmin Le Roux
---
# Scope OFISH capability caches to workspace connection lifecycle

Problem:
OFISH capability and upload chunk-size probes are cached per FileRepository instance, but FileRepositoryFactory.create() is called from multiple screen/viewmodel paths. This can discard caches and repeat expensive shell probes for the same workspace/server.

Evidence:
FileRepositoryFactory.create() constructs new CachedOfishCapabilities and CachedOfishUploadChunkBytes every call. It is called from TabNavHost for FilesViewModel/FileViewerScreen and from FilePickerManager's default repository. OFISH probes require shell sessions and capability/chunk commands.

UX Constraint:
File operations should not repeatedly pay capability-probe latency when the workspace/server has not changed. Cache ownership must remain workspace/server-scoped and must not leak across different servers or workspace generations.

Expected Behavior:
OFISH capability/chunk caches live for the workspace connection lifecycle, not for each transient repository/viewmodel instance.

Acceptance Criteria:
- Introduce a workspace/server/generation-scoped FileRepository or OFISH capability cache provider.
- Reuse capability and chunk-size probe results across chat attachments, Files screen, and FileViewer for the same workspace connection.
- Invalidate caches on disconnect, server generation change, workspace change where necessary, or capability probe failure when appropriate.
- Preserve workspace cutover constraints; no global current workspace singleton.

Verification:
Run file/OFISH tests and ./gradlew :app:compileDebugKotlin. Manually verify repeated file operations in the same workspace do not rerun probes unnecessarily where logs can confirm.


## Notes

**2026-05-10T14:55:14Z**

Closing as superseded/stale. Runtime OFISH capability probing still exists intentionally because mutations need shell tool/flag detection, but the original repeated-cache concern has been addressed by WorkspaceRepositoryOwner owning a shared FileRepository per workspace owner. Files screen, file viewer, and chat uploads now route through that owner-scoped repository/cache in normal navigation. The ticket's upload chunk-size cache concern is stale because chunk-size probing was removed and uploads use a fixed chunk provider. Reopen only with evidence that multiple WorkspaceRepositoryOwner/FileRepository instances for the same tab/workspace/server generation are causing repeated capability probes.
