---
id: oa-p98b
status: closed
deps: [oa-6d53]
links: []
created: 2026-05-01T17:45:54Z
type: task
priority: 1
assignee: Jasmin Le Roux
parent: oa-gt0g
tags: [workspace, api]
---
# Commit 3: STRIP directory defaults from OpenCodeApi

Remove '= null' default from every @Query("directory") directory: String? param in OpenCodeApi.kt (~20 methods). Param stays required-nullable; the default is what's gone. This breaks compilation at every call site that doesn't go through WorkspaceClient — that IS the forcing function. Tree red after this commit until commit 8.

## Acceptance Criteria

1) git diff on OpenCodeApi.kt shows ONLY removals of '= null' on @Query("directory") params. 2) No new overloads introduced (no listSessionsGlobal() escape hatches). 3) No callers commented out (// directoryManager pattern). 4) Expected list of broken files documented in ticket BEFORE commit; actual ./gradlew :app:compileDebugKotlin failure set matches. 6) Tree IS red — that's expected.


## Notes

**2026-05-02T11:34:44Z**

Expected red-tree compile breakage before stripping OpenCodeApi directory defaults:

Known likely broken files from direct call-site scan:
- app/src/main/java/dev/blazelight/p4oc/data/session/SessionRepositoryImpl.kt
- app/src/main/java/dev/blazelight/p4oc/core/network/SessionDataCache.kt
- app/src/main/java/dev/blazelight/p4oc/ui/screens/chat/ChatViewModel.kt
- app/src/main/java/dev/blazelight/p4oc/ui/screens/sessions/SessionListViewModel.kt
- app/src/main/java/dev/blazelight/p4oc/ui/screens/diff/SessionDiffScreen.kt

Possible test compile fallout:
- app/src/test/java/dev/blazelight/p4oc/ui/screens/chat/ChatViewModelTest.kt

Expected cause: legacy/direct OpenCodeApi callers that relied on @Query("directory") directory: String? = null defaults rather than passing explicit workspace-scoped directory through WorkspaceClient. This ticket intentionally does not fix call sites.

**2026-05-02T11:36:16Z**

Actual verification after stripping OpenCodeApi directory defaults:

Diff scope:
- app/src/main/java/dev/blazelight/p4oc/core/network/OpenCodeApi.kt only.
- All @Query("directory") directory: String? = null defaults were removed.
- No @Query("directory") defaults remain in OpenCodeApi.kt.
- No overloads added and no callers changed/commented out.

Compile verification:
- export JAVA_HOME=/usr/lib/jvm/java-17-openjdk && ./gradlew :app:compileDebugKotlin
- Result: BUILD SUCCESSFUL. Actual failing files: none.

Additional test compile check:
- export JAVA_HOME=/usr/lib/jvm/java-17-openjdk && ./gradlew :app:compileDebugUnitTestKotlin
- Result: BUILD SUCCESSFUL. Actual failing test files: none.

Mismatch vs original acceptance: the ticket expected a red tree, but current call sites already pass directory explicitly or route through WorkspaceClient, so removing defaults did not produce compile failures.
