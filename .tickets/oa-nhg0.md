---
id: oa-nhg0
status: closed
deps: [oa-6zta, oa-cemz]
links: []
created: 2026-05-01T17:45:54Z
type: task
priority: 1
assignee: Jasmin Le Roux
parent: oa-gt0g
tags: [workspace, persistence]
---
# Commit 9: tab-scoped persistence (process-death restoration)

Persists open tabs + per-tab active SessionId/Workspace via TabManager.saveState/restoreState as versioned JSON in DataStore. Per-tab SavedStateHandle for active session. Restore validates Workspace belongs to active server (per design-D) before resurrecting; explicit error UI on mismatch, not silent fallback. NON-NEGOTIABLE: lands in same branch as cutover — without this, cutover ships clean architecture + UX regression.

## Acceptance Criteria

1) Manual: open two tabs different workspaces → force-stop app → reopen → both tabs restored to correct workspace+session. 2) Manual: change server URL → reopen → explicit error UI for stale tabs, NO silent global state. 3) Manual: open session, kill app, reopen — lands on same session (replaces dead lastSessionId). 4) Persisted JSON has version field. 5) Migration handles version mismatch. 6) No resurrection of old lastSessionId / project_worktree semantics under different names.


## Notes

**2026-05-02T13:44:59Z**

Implemented tab-scoped persistence.

Summary:
- Added versioned PersistedTabState/PersistedTab JSON in SettingsDataStore under tab_state_v1.
- Added get/set persisted tab state APIs.
- TabManager now saves open tabs with activeTabId, per-tab sessionId/sessionTitle, workspaceDirectory, and active server endpoint key.
- TabManager restore validates PersistedTabState.version and active ServerRef endpoint key before resurrecting tabs.
- Server mismatch/version mismatch returns explicit RestoreResult; MainTabScreen displays a snackbar and starts fresh instead of silently falling back to global state.
- Session tabs restore as chat routes carrying the same session and workspace directory.
- Terminal tabs intentionally restore as sessions root, not stale PTY sessions.
- MainTabScreen restores once after active server is known and saves whenever tabs/active tab/current server changes.
- Added TabManagerPersistenceTest coverage for versioned save, same-server restore, server mismatch, version mismatch, and terminal route sanitization.

Verification:
- export JAVA_HOME=/usr/lib/jvm/java-17-openjdk && ./gradlew :app:compileDebugKotlin: BUILD SUCCESSFUL
- export JAVA_HOME=/usr/lib/jvm/java-17-openjdk && ./gradlew :app:testDebugUnitTest --tests 'dev.blazelight.p4oc.ui.tabs.*': BUILD SUCCESSFUL
- export JAVA_HOME=/usr/lib/jvm/java-17-openjdk && ./gradlew :app:testDebugUnitTest: BUILD SUCCESSFUL

Greps:
- No old lastSessionId/project_worktree APIs reintroduced.
- Remaining project_worktree/last_session_id literals are only in the migration that removes old prefs.
