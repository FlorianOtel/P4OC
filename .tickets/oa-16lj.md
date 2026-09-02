---
id: oa-16lj
status: closed
deps: []
links: []
created: 2026-05-10T09:45:36Z
type: task
priority: 2
assignee: Jasmin Le Roux
---
# Replace custom unified diff parsing with java-diff-utils mapping

Problem:
ParsedDiffParser manually parses unified diff sections, headers, hunks, and line numbers even though java-diff-utils is already imported and used for validation. This duplicates library behavior and increases parser bug surface.

Evidence:
app/src/main/java/dev/blazelight/p4oc/ui/diff/ParsedDiff.kt contains ParsedDiffParser with custom splitIntoFileSections(), hunk regex parsing, HunkBuilder, and line-number tracking. It calls UnifiedDiffUtils.parseUnifiedDiff(linesForLibrary) only to validate hunk count, then discards the Patch object and parses manually.

UX Constraint:
Diff rendering must remain stable for git-style multi-file diffs, headerless unified hunks, /dev/null create/delete cases, and files with timestamps in headers. Bad or unsupported diffs should fail gracefully in UI.

Expected Behavior:
Use java-diff-utils as the parser source of truth where it can represent the diff, then map its Patch/delta model into ParsedDiff/ParsedHunk/ParsedDiffLine or a simpler UI model. Keep only minimal glue for file-section metadata the library does not expose.

Acceptance Criteria:
- Delete or substantially reduce custom hunk parsing logic in ParsedDiffParser.
- Use UnifiedDiffUtils/Patch/delta data for hunk and line content mapping.
- Preserve existing ParsedDiffParserTest behavior or intentionally update tests with documented behavior changes.
- Keep support for multiple file sections and headerless fallback if still needed.
- Add regression tests for create/delete, multiple hunks, and malformed diff handling.

Verification:
Run ./gradlew :app:testDebugUnitTest --tests dev.blazelight.p4oc.ui.diff.ParsedDiffParserTest and ./gradlew :app:compileDebugKotlin.


## Notes

**2026-05-10T11:22:58Z**

Started refactor of ParsedDiffParser to map java-diff-utils Patch/delta output into ParsedHunk while keeping minimal file-section/header glue for metadata and context row reconstruction. Removed HunkBuilder/custom hunk object construction. Targeted verification is currently blocked before tests run by unrelated SettingsScreen missing string resource compile errors: settings_help, status_legend_* and related IDs. Ticket remains in_progress until those external compile errors are resolved and ParsedDiffParserTest can be rerun.
