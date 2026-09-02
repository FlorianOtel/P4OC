---
id: oa-gtw8
status: closed
deps: []
links: [oa-v3js, oa-t3tb, oa-12ui]
created: 2026-07-05T18:06:47Z
type: bug
priority: 1
assignee: Jasmin Le Roux
parent: oa-nwha
---
# Preserve in-app file editor buffers across lifecycle

Problem:
Unsaved in-app file edit buffers are lifecycle-critical and may be lost when the file viewer/editor is disposed, recreated, or switched across tabs.

Evidence:
Lifecycle audit identified FileViewerScreen.kt unsaved edit buffer state as restoration-critical. Project instructions forbid relying on external editors for core editing workflows, so in-app editor state must be protected.

UX Constraint:
Losing unsaved edits is a severe user-data-loss bug. File editing must remain tabbed inside P4OC by default and must respect workspace/file identity.

Expected Behavior:
Unsaved edits are scoped by workspace, tab, and file path. Switching tabs, configuration changes, or process recreation should preserve the draft where feasible. Conflicts or unavailable files must be shown with clear recovery options, not silent overwrite/discard.

Acceptance Criteria:
- Identify current file editor buffer source of truth and lifecycle boundaries.
- Persist unsaved edit buffers per workspace/tab/file path or explicitly store recoverable drafts.
- Detect file-on-disk changes/conflicts before saving a restored draft.
- Add behavior tests for typing edits, switching away/back, and preserving the buffer.
- Add conflict/failure tests for deleted or externally modified files if supported by repository seams.

Verification:
Run targeted file viewer/editor tests and smoke test editing a file, switching tabs, rotating/recreating, and returning.


## Notes

**2026-07-07T21:17:58Z**

Implemented in-app file editor draft preservation using the FilesViewModel SavedStateHandle scope added for oa-tzta. Current source of truth remains FileEditState in FilesViewModel; it is now restored from/persisted to SavedStateHandle per files tab/back-stack entry with path, original content, current draft content, and baseline hash. All FileEditState mutations now go through updateEditState(), keeping transient UI flags and draft content synchronized to SavedStateHandle. Recreating the VM restores dirty drafts as dirty; re-loading the same path preserves a restored dirty draft instead of clobbering it with server content, while saves still use the restored baseline hash for stale-write conflict detection through FileWriteRequest.expectedHash. Added FilesViewModelEditTest coverage for dirty-buffer restoration across VM recreation and same-path load preserving a restored dirty buffer, in addition to existing save/conflict/discard coverage. Verification: ./gradlew :app:testDebugUnitTest --tests dev.blazelight.p4oc.ui.screens.files.FilesViewModelEditTest; ./gradlew :app:compileDebugKotlin; ./gradlew :app:detekt.
