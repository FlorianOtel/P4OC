---
id: oa-d55a
status: closed
deps: [oa-6d53, oa-cemz]
links: []
created: 2026-05-01T17:45:54Z
type: task
priority: 1
assignee: Jasmin Le Roux
parent: oa-gt0g
tags: [workspace, projects, server]
---
# Commit 6a: Rewrite ProjectsViewModel + ServerViewModel (workspace switching + disconnect cascade)

ProjectsViewModel.selectProject — instead of mutating global directory, construct Workspace and switch ACTIVE TAB's workspace only. ServerViewModel.disconnect — tear down all WorkspaceViewModel scopes. ServerViewModel.initializeProjectContext — delete the unconditional setDirectory(null) wipe. This is a UX semantic change: tab B unaffected when tab A switches projects.

## Acceptance Criteria

1) Manual smoke: tab A on project X, tab B on project Y; switch tab A to project Z; tab B STILL shows Y. 2) Manual smoke: disconnect → all tabs torn down (no zombie WorkspaceClients). 3) ProjectsViewModel has no DirectoryManager dependency. 4) ServerViewModel has no DirectoryManager + no SessionDataCache dependency. 5) initializeProjectContext does NOT wipe directory unconditionally.


## Notes

**2026-05-02T12:21:56Z**

Implemented ProjectsViewModel + ServerViewModel workspace switching rewrite.

Summary:
- ProjectsViewModel no longer depends on DirectoryManager and no longer persists/mutates a global directory on project selection.
- ProjectsScreen now delegates project selection to its caller only.
- TabManager/TabState now track per-tab workspaceDirectory plus workspaceRevision.
- TabNavHost builds Workspace from per-tab workspaceDirectory or explicit chat route directory, not DirectoryManager.
- Project selection updates only the current tab workspace and navigates to filtered sessions.
- Workspace nav graph route now includes workspaceRevision so switching a tab project recreates that tab scoped WorkspaceViewModel/WorkspaceClient while other tabs are unaffected.
- ServerViewModel no longer depends on DirectoryManager and initializeProjectContext no longer clears directory.

Verification:
- export JAVA_HOME=/usr/lib/jvm/java-17-openjdk && ./gradlew :app:compileDebugKotlin: BUILD SUCCESSFUL
- export JAVA_HOME=/usr/lib/jvm/java-17-openjdk && ./gradlew :app:testDebugUnitTest: BUILD SUCCESSFUL

Greps:
- No DirectoryManager/setDirectory usage remains in ui/screens/projects or ui/screens/server.
- No DirectoryManager/getDirectory usage remains in TabNavHost.
