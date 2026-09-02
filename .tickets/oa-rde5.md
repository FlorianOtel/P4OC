---
id: oa-rde5
status: closed
deps: [oa-p98b, oa-ja73, oa-vvep, oa-0f4m]
links: []
created: 2026-05-01T17:45:54Z
type: task
priority: 1
assignee: Jasmin Le Roux
parent: oa-gt0g
tags: [workspace, sessions, repo]
---
# Commit 5 (reordered): SessionListViewModel + SessionRepositoryImpl with ported behavior

Reordered before ChatVM rewrite — Chat depends on SessionRepository.messages(WorkspaceSession). Implement SessionRepositoryImpl: port Semaphore(10) bounded concurrency from SessionDataCache, port 30s freshness, port per-project fan-out + global/project dedupe, port stale-server discard, port prewarm. Implement reducer hydrate-then-stream per design-B and optimistic rollback per design-C. Rewrite SessionListViewModel scoped to Workspace, mutations via WorkspaceClient. DELETE SessionDataCache.kt. Tree still red.

## Acceptance Criteria

1) SessionRepositoryImpl.hydrate uses Semaphore(10) literally (or equivalent), verified by grep + behavior test. 2) 30s freshness window present. 3) Optimistic mutation test: mock HTTP 5xx on delete → item reappears with error (per design-C). 4) Hydrate-race test: SSE event during hydrate appears in final ordered state, not lost (per design-B). 5) SessionDataCache.kt does NOT exist (file deleted). 6) No 'directory ?: directoryManager.getDirectory()' fallback. 7) Existing SessionDataCacheTest behavior is REPLACED, not deleted (test ported to SessionRepositoryImplTest). 8) ./gradlew :app:testDebugUnitTest green for these tests.


## Notes

**2026-05-02T11:53:24Z**

Implemented SessionRepositoryImpl + SessionListViewModel rewrite.

Summary:
- Ported SessionDataCache behavior into SessionRepositoryImpl: Semaphore(10), 30s freshness, in-flight prewarm dedupe, global/project fan-out, project/global session dedupe, status hydrate, hydrate event replay, and optimistic delete failure refetch.
- Rewrote SessionListViewModel to consume tab-scoped SessionRepositoryImpl instead of ConnectionManager/DirectoryManager/SessionDataCache.
- Wired SessionListScreen through the per-tab WorkspaceViewModel in TabNavHost.
- Deleted SessionDataCache.kt and replaced SessionDataCacheTest with SessionRepositoryImplTest.
- Removed SessionDataCache from DI and ServerViewModel prewarm calls.

Verification:
- export JAVA_HOME=/usr/lib/jvm/java-17-openjdk && ./gradlew :app:compileDebugKotlin: BUILD SUCCESSFUL
- export JAVA_HOME=/usr/lib/jvm/java-17-openjdk && ./gradlew :app:testDebugUnitTest --tests 'dev.blazelight.p4oc.data.session.*': BUILD SUCCESSFUL
- export JAVA_HOME=/usr/lib/jvm/java-17-openjdk && ./gradlew :app:testDebugUnitTest: BUILD SUCCESSFUL

Note: original ticket said tree still red, but this implementation currently compiles and tests green. The remaining DirectoryManager fallback grep hit is in ChatViewModel, which is reserved for the later ChatViewModel rewrite ticket.
