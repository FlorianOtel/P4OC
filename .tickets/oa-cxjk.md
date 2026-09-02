---
id: oa-cxjk
status: closed
deps: []
links: []
created: 2026-05-05T18:19:53Z
type: task
priority: 2
assignee: Jasmin Le Roux
parent: oa-ssm2
tags: [diff, library, cleanup]
---
# Replace hand-rolled diff parser with java-diff-utils 4.15

Add io.github.java-diff-utils:java-diff-utils:4.15.0 (https://github.com/java-diff-utils/java-diff-utils, Apache-2.0). Use UnifiedDiffUtils.parseUnifiedDiff(...) and DiffUtils.diff(original, revised) to:

1. Replace parseInlineDiff() at InlineDiffViewer.kt:172-196 (~80 LOC of brittle regex parsing).
2. Replace duplicated parsing logic in DiffViewerScreen.kt:52-104.
3. Provide diff computation for the editor's diff-before-save modal (see editor ticket).

Keep the renderers — they're tightly tied to LocalOpenCodeTheme, SemanticColors.Diff, monospace TUI layout, and expandable chat cards. Just feed them structured diff models from the library.

Introduce one internal ParsedDiff/ParsedFileDiff/ParsedHunk/ParsedDiffLine model that both viewers consume. Delete duplicate parsing.

## Acceptance Criteria

java-diff-utils dep added. Both InlineDiffViewer and DiffViewerScreen consume the same parsed model. Existing diff rendering output identical (visual regression test). Editor save flow uses DiffUtils.diff(server, mine) → unified diff → InlineDiffViewer.

