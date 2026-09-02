---
id: oa-82uy
status: closed
deps: [oa-6d53, oa-blgp]
links: []
created: 2026-05-01T17:45:54Z
type: task
priority: 1
assignee: Jasmin Le Roux
parent: oa-gt0g
tags: [workspace, files, diff]
---
# Commit 6b: Rewrite SessionDiffScreen + FilesViewModel + FileExplorerScreen (path/URI rendering)

SessionDiffScreen receives WorkspaceSession from nav (no DirectoryManager injection). FilesViewModel uses Workspace from per-tab scope. FileExplorerScreen symbol URI parsing at line 219 → WorkspacePath.parseFromServer(uri). Breadcrumb stops mixing '' / '.' / '/abs' — single canonical representation per design-E.

## Acceptance Criteria

1) SessionDiffScreen has no DirectoryManager. 2) Diff opens correctly from session list AND from chat — same workspace. 3) Symbol URI with %20, ?, # parses correctly via WorkspacePath.parseFromServer (round-trip test passes). 4) File viewer handles paths with spaces/unicode. 5) FilesViewModel has no DirectoryManager. 6) Manual smoke: browse files in two workspaces in two tabs without cross-contamination.


## Notes

**2026-05-02T12:55:52Z**

Implemented files/diff workspace rewrite.

Summary:
- FilesViewModel now takes WorkspaceClient and no longer uses ConnectionManager/global API access.
- FileExplorerScreen/FileViewerScreen require an explicitly supplied FilesViewModel; Koin binding for FilesViewModel was removed.
- TabNavHost wires FileExplorerScreen/FileViewerScreen from the per-tab WorkspaceViewModel workspaceClient.
- SessionDiffScreen now takes WorkspaceClient, has no DirectoryManager/ConnectionManager injection, and fetches diff through workspaceClient.getSessionDiff(...).
- WorkspaceClient gained workspace-scoped file/diff helpers.
- Symbol URI parsing now goes through WorkspacePathParser.parseFromServer(...), preserving encoded spaces/unicode and encoded/raw ?/# path characters.
- Breadcrumb navigation now uses one canonical relative representation: root is "" and child paths are relative without leading /.

Verification:
- export JAVA_HOME=/usr/lib/jvm/java-17-openjdk && ./gradlew :app:compileDebugKotlin: BUILD SUCCESSFUL
- export JAVA_HOME=/usr/lib/jvm/java-17-openjdk && ./gradlew :app:testDebugUnitTest --tests 'dev.blazelight.p4oc.domain.workspace.*': BUILD SUCCESSFUL
- export JAVA_HOME=/usr/lib/jvm/java-17-openjdk && ./gradlew :app:testDebugUnitTest: BUILD SUCCESSFUL

Greps:
- No DirectoryManager/ConnectionManager/getApi/file:// usage remains under ui/screens/files.
- SessionDiffScreen has no DirectoryManager/ConnectionManager/getApi usage.
