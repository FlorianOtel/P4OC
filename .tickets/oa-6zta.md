---
id: oa-6zta
status: closed
deps: [oa-rde5, oa-aihi, oa-d55a, oa-82uy]
links: []
created: 2026-05-01T17:45:54Z
type: task
priority: 1
assignee: Jasmin Le Roux
parent: oa-gt0g
tags: [workspace, demolition]
---
# Commit 7+8 merged: STRIP ConnectionManager wiring + DELETE DirectoryManager + dead DataStore keys

Demolition gate. Delete: ConnectionManager directoryProvider wiring (line 110) + setOnDirectoryChangedListener block (line 132). Delete: OpenCodeEventSource directoryProvider param. DELETE core/network/DirectoryManager.kt entirely. Delete from SettingsDataStore: KEY_PROJECT_WORKTREE, KEY_LAST_SESSION_ID, lastSessionId flow, get/setProjectWorktree, get/setLastSessionId. Add DataStore migration so existing users don't crash on first open with old prefs. Delete the two singleton bindings in KoinModules.kt. Tree compiles green here.

## Acceptance Criteria

1) DirectoryManager.kt does NOT exist. 2) SessionDataCache.kt does NOT exist (already deleted in T12). 3) MessageStore.kt does NOT exist (already deleted in T13). 4) ConnectionManager has no DirectoryManager param. 5) OpenCodeEventSource has no directoryProvider param. 6) project_worktree, last_session_id keys NOT in SettingsDataStore. 7) DataStore migration exists; planted user with old prefs launches without crash. 8) ./gradlew :app:compileDebugKotlin GREEN. 9) ./gradlew :app:assembleDebug produces APK that launches.


## Notes

**2026-05-02T13:14:05Z**

Implemented demolition pass.

Summary:
- Deleted core/network/DirectoryManager.kt.
- Removed DirectoryManager from ConnectionManager constructor and Koin DI.
- Removed ConnectionManager directoryProvider wiring and setOnDirectoryChangedListener reconnect block.
- Removed OpenCodeEventSource directoryProvider constructor parameter.
- Removed SettingsDataStore KEY_PROJECT_WORKTREE / KEY_LAST_SESSION_ID declarations, lastSessionId flow, setLastSessionId, projectWorktree flow, getProjectWorktree, and setProjectWorktree.
- Added DataStore migration that removes old project_worktree and last_session_id persisted keys for existing users.

Verification:
- export JAVA_HOME=/usr/lib/jvm/java-17-openjdk && ./gradlew :app:compileDebugKotlin: BUILD SUCCESSFUL
- export JAVA_HOME=/usr/lib/jvm/java-17-openjdk && ./gradlew :app:testDebugUnitTest: BUILD SUCCESSFUL
- export JAVA_HOME=/usr/lib/jvm/java-17-openjdk && ./gradlew :app:assembleDebug: BUILD SUCCESSFUL

Greps:
- DirectoryManager, directoryProvider, setOnDirectoryChangedListener, KEY_PROJECT_WORKTREE, KEY_LAST_SESSION_ID, lastSessionId/getProjectWorktree/setProjectWorktree APIs are gone from app source.
- Remaining literal last_session_id/project_worktree references are only inside the migration that deletes old prefs.
- SessionDataCache.kt and MessageStore.kt remain deleted.
