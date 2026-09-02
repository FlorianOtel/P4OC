---
id: oa-tzta
status: closed
deps: []
links: [oa-e6g3, oa-ua2q, oa-12ui]
created: 2026-07-05T18:06:47Z
type: bug
priority: 2
assignee: Jasmin Le Roux
parent: oa-nwha
---
# Restore file explorer path search and symbol filters per tab

Problem:
File explorer navigation and filter state can be lost or leak across tabs/workspaces because path/search/symbol state is lifecycle-blind or not clearly scoped.

Evidence:
Lifecycle audit identified FilesViewModel.kt and FileExplorerScreen.kt path stack, search query, symbol filters, and related file navigation state as restoration-critical.

UX Constraint:
File navigation state helps prevent wrong-directory mistakes. It should be preserved enough to resume work without taking extra persistent chrome space.

Expected Behavior:
Each file tab/workspace restores its current path, search query, and symbol/filter state when returning. State is isolated between workspaces/tabs and clears only through explicit user action or clear UX policy.

Acceptance Criteria:
- Scope explorer path/search/symbol filter state by workspace/tab.
- Persist or save state across tab switches and configuration changes.
- Ensure a different workspace/tab does not inherit another explorer's path/search/filter state.
- Add tests for same-tab restoration and cross-tab/workspace isolation.
- Show human-readable error if restored path no longer exists, with a safe fallback to workspace root.

Verification:
Run targeted FilesViewModel/FileExplorer tests and smoke test two workspaces/tabs with different explorer paths.


## Notes

**2026-07-07T21:05:42Z**

Implemented file explorer lifecycle restoration with per-tab SavedStateHandle state. FilesViewModel now accepts SavedStateHandle, restores/persists currentPath, pathStack, local filename search query/active state, symbol mode, and symbol query; FileExplorerScreen now reads these from FilesUiState instead of local remember state. Restored non-blank symbol queries re-run search rather than persisting derived symbol results. Restored missing paths now fall back to the workspace root, clear the path stack, and surface a visible root-path restore warning via resource-backed UI text. Koin now registers both SessionListViewModel and FilesViewModel with SavedStateHandle. Added FilesViewModelEditTest coverage for same-tab SavedStateHandle recreation restoring path stack and filters, symbol query re-fetch, and missing restored path root fallback with user-visible restore error. Verification: ./gradlew :app:testDebugUnitTest --tests dev.blazelight.p4oc.ui.screens.files.FilesViewModelEditTest; ./gradlew :app:compileDebugKotlin; ./gradlew :app:detekt (fails only on the same 7 pre-existing findings: SessionListViewModel searchSessions LongMethod, ConnectionManager connectionCandidates ReturnCount, and SessionRepositoryImplTest line-length findings).
