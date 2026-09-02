---
id: oa-ua2q
status: closed
deps: []
links: [oa-tzta, oa-e6g3, oa-plno, oa-casy, oa-12ui]
created: 2026-07-05T18:06:47Z
type: bug
priority: 1
assignee: Jasmin Le Roux
parent: oa-nwha
---
# Make workspace-scoped session search explicit

Problem:
Session search can still treat null directory as global/all-project search, which can cross workspace boundaries and reintroduce old wrong-directory bugs.

Evidence:
Workspace audit found SessionRepositoryImpl.searchSessions(query, directory: String? = null) treating null as global plus every project worktree, scoped SessionRepositoryImpl.hydrate() still loading global and all project sessions, and tests encoding broad null-directory search.

UX Constraint:
Users must trust that session search results belong to the active workspace unless they explicitly choose server-global search. Workspace identity should remain compact but clear enough to prevent mistakes.

Expected Behavior:
Workspace-scoped search searches only the current workspace directory. Server-global/all-workspace search is a separate explicit action/type/name and is labeled in UI. Omitted directory parameters cannot silently broaden scope.

Acceptance Criteria:
- Remove nullable default arguments that let callers omit search workspace identity.
- Split workspace-scoped search from explicit server-global/all-project search in API/repository names or types.
- Ensure scoped hydrate/search only returns current workspace sessions unless explicitly global.
- Update tests that currently assert broad null-directory search.
- Add regression test with two workspace directories proving scoped search isolation.
- Add UI copy or result labeling for explicit global search if exposed.

Verification:
Run targeted SessionRepositoryImpl/session list tests and smoke test search in two workspace tabs.


## Notes

**2026-07-06T08:54:53Z**

Clarification from 2026-07-05 workspace/tab UX discussion:

Coordinate this ticket with oa-e6g3. The agreed flat-tab UX is: a fresh top-level Sessions tab is intentionally WorkspaceKey.Global and should show all sessions/top-down view. Workspace-scoped search is still required for contextual/session/project views, but Global is not inherently a bug when it is explicit.

Implementation intent:
- Replace omitted nullable directory defaults with explicit WorkspaceKey input.
- WorkspaceKey.Global search/hydrate means intentional server-wide/all-sessions behavior, appropriate for fresh top-level Sessions.
- WorkspaceKey.Directory(path) search/hydrate means scoped project/session behavior, appropriate for contextual opens.
- Missing legacy workspaceKey = null is not Global; it should recover/ask rather than silently fan out.

Do not implement this by making every Sessions tab directory-scoped. The bug is global fan-out by omission/ambiguous null, not explicit top-level Global behavior.

**2026-07-07T20:01:47Z**

Implemented explicit session search split. Removed the public nullable-default searchSessions(query, directory = null) contract and replaced it with searchSessionsInWorkspace(query, directory) for scoped Directory search and searchSessionsGlobally(query) for explicit Global/all-project search. SessionListViewModel now chooses the explicit API based on the current search scope instead of relying on omitted/null broadening. Added SessionRepositoryImpl tests proving scoped search only queries the requested directory with two matching workspaces present, and explicit global search queries null plus known project worktrees and dedupes duplicate ids. Verification: ./gradlew :app:testDebugUnitTest --tests dev.blazelight.p4oc.data.session.SessionRepositoryImplTest.searchSessions*; ./gradlew :app:compileDebugKotlin.
