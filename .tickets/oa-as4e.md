---
id: oa-as4e
status: closed
deps: []
links: []
created: 2026-05-10T09:54:51Z
type: task
priority: 1
assignee: Jasmin Le Roux
---
# Share SessionRepository by workspace instead of tab

Problem:
SessionRepositoryImpl instances are currently tied to tab/workspace ViewModel lifetimes. Multiple tabs opened to the same server/workspace can create duplicate repositories, duplicate hydration work, and divergent in-memory snapshots for the same workspace.

Evidence:
WorkspaceViewModel owns val sessionRepository = SessionRepositoryImpl(...) and is created per tab via TabNavHost/TouchWorkspaceViewModel parameters including tabId, workspace, and generation. Opening the same workspace in multiple tabs creates separate WorkspaceViewModel and SessionRepositoryImpl instances.

UX Constraint:
Tabs showing the same workspace should agree on session/message state, deletes, streaming flags, and SSE updates without redundant network load. Closing one tab must not close the shared repository if another tab still uses it.

Expected Behavior:
Session repositories are keyed by server/generation/workspace key, with tab-level consumers attaching to shared repository state. Repository lifetime is reference-counted or otherwise tied to active workspace consumers and disconnect/generation changes.

Acceptance Criteria:
- Introduce a WorkspaceStore/SessionRepositoryProvider keyed by server, generation, and WorkspaceKey.
- Reuse one SessionRepositoryImpl for multiple tabs targeting the same workspace generation.
- Close a shared repository only when the last owning tab/workspace consumer is gone or generation changes.
- Preserve per-tab navigation state separately from shared domain state.
- Ensure optimistic deletes/updates propagate to all tabs observing the same workspace.
- Avoid global/default workspace shortcuts or active-tab data-layer access.

Verification:
Run session repository tests and ./gradlew :app:compileDebugKotlin. Manually open two tabs to the same workspace, delete/update a session in one, and confirm the other updates without full manual refresh.


## Notes

**2026-05-10T10:39:00Z**

Added SessionRepositoryProvider keyed by server endpoint key, generation, and stable WorkspaceKey. WorkspaceViewModel now acquires a shared workspace client/repository lease and releases it on onCleared instead of constructing/closing a per-tab repository. Provider reference-counts consumers and closes the repository only on final release. SSE scopedEvents collection moved into the provider entry so a shared repository has one event consumer even when multiple tabs attach. Added SessionRepositoryProviderTest coverage for same-key reuse, reference retention until final release, replacement after final release, and generation separation. Verification: ./gradlew :app:testDebugUnitTest --tests dev.blazelight.p4oc.data.session.SessionRepositoryProviderTest --tests dev.blazelight.p4oc.data.session.SessionRepositoryImplTest; export JAVA_HOME=/usr/lib/jvm/java-17-openjdk && ./gradlew :app:compileDebugKotlin. Manual two-tabs-same-workspace update/delete smoke test still recommended.
