---
id: oa-rnf3
status: closed
deps: []
links: []
created: 2026-05-09T15:47:17Z
type: feature
priority: 2
assignee: Jasmin Le Roux
---
# Expose file create and delete actions in file explorer

File mutation infrastructure exists through OFISH, but the file explorer UI does not expose obvious create/delete workflows. Users should be able to create small files and delete files/folders from mobile without asking the agent to perform the operation.\n\nExpected UX:\n- File explorer provides New file / New folder actions for the current workspace directory.\n- File rows provide Delete through a long-press or overflow menu with confirmation.\n- Empty folders show CTAs such as New file and Upload here.\n- Actions are capability-gated when OFISH/file mutation support is unavailable.\n\nAcceptance criteria:\n- New file creates a file in the current explorer directory using the current Workspace.\n- New folder creates a directory in the current explorer directory, if supported by the mutation backend.\n- Delete removes the selected file/folder only after confirmation.\n- Empty folder state includes create/upload affordances.\n- UI never falls back to a global/default workspace.\n- Failure/conflict/capability-missing states are shown as human-readable messages.


## Notes

**2026-05-09T15:52:42Z**

Standardization note: deliver the full file mutation UI surface, not only a first slice. Include New file, New folder where backend supports it, Rename if feasible, Delete with confirmation, and empty-folder CTAs (New file, Upload here). Long-press/overflow is the correct contextual surface for row actions; creation should also be visible in top-bar/empty state. All actions must be current-Workspace scoped, capability-gated, and show human-readable failure/conflict/capability-missing messages. UI chrome must be justified: prefer contextual menus and empty-state CTAs over persistent buttons that reduce file/agent viewport.

**2026-05-10T14:42:12Z**

Implemented workspace-scoped file explorer mutation UI: top-bar New file/New folder menu, empty-folder New file/Upload here CTAs, long-press row Rename/Delete actions with delete confirmation, capability gating, and human-readable mutation errors. Extended FileRepository/OFISH with createDirectory and renameFile, enabled recursive delete for folders, and added focused OFISH command/client test coverage. Verification: :app:compileDebugKotlin passes. :app:detekt still fails on existing non-ticket findings outside this work after new touched-file findings were addressed. Targeted OFISH unit test run is blocked by an existing ChatViewModelTest constructor mismatch unrelated to this ticket.
