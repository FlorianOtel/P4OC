---
id: oa-plno
status: closed
deps: []
links: [oa-ua2q, oa-12ui]
created: 2026-07-05T18:06:47Z
type: bug
priority: 2
assignee: Jasmin Le Roux
parent: oa-nwha
---
# Restore session list search and tree expansion state

Problem:
Session list search text and tree expansion state may be lost or leak because they are lifecycle-blind or not clearly scoped.

Evidence:
Lifecycle audit identified SessionListScreen.kt and SessionListViewModel.kt search and tree expansion state as restoration-critical for navigating sessions.

UX Constraint:
Session navigation must remain compact but predictable. Losing search/expansion while switching tabs can make it hard to resume work and can increase wrong-session selection risk.

Expected Behavior:
Session list search and tree expansion restore for the same workspace/server context and do not leak across different workspaces/servers. Clear actions should explicitly reset state.

Acceptance Criteria:
- Scope search query and tree expansion by workspace/server/tab context.
- Preserve state across tab switch and configuration recreation.
- Avoid leaking one workspace/server's expanded tree into another.
- Add tests for same-context restoration and different-context isolation.
- Define behavior when restored search no longer matches any sessions.

Verification:
Run targeted SessionListViewModel/Screen tests and smoke test switching between two workspace session lists.


## Notes

**2026-07-07T20:55:16Z**

Implemented workspace/directory-scoped session list search and tree expansion restoration. SessionListViewModel now receives SavedStateHandle through Koin, persists search queries and expanded session ids by context (global vs directory), restores state on context changes/recreation, and exposes expandedSessionIds/toggleSessionExpanded so SessionListScreen no longer owns lifecycle-blind expansion state. Search clearing resets only the active context; switching contexts prevents query/expanded-session leakage and switching back restores the saved state. Added SessionListViewModelTest coverage for shared-SavedStateHandle recreation, different-directory isolation, blank-query clearing, and restored no-match search semantics. Verification: ./gradlew :app:testDebugUnitTest --tests dev.blazelight.p4oc.ui.screens.sessions.SessionListViewModelTest; ./gradlew :app:compileDebugKotlin; ./gradlew :app:detekt (fails only on pre-existing SessionListViewModel searchSessions LongMethod, ConnectionManager ReturnCount, and SessionRepositoryImplTest line-length findings; no oa-plno-specific findings remain).
