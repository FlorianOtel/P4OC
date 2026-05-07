package dev.blazelight.p4oc.ui.diff

import com.github.difflib.DiffUtils
import com.github.difflib.UnifiedDiffUtils
import com.github.difflib.patch.DeltaType
import com.github.difflib.patch.Patch

/**
 * Builds a real unified diff between two file contents using java-diff-utils.
 * Used by the file editor's "review-before-save" modal and the post-write
 * summary.
 *
 * The builder always emits a synthetic file header (`--- a/<path>` / `+++ b/<path>`)
 * so the result is parseable by [ParsedDiffParser] without further wrapping.
 */
internal object UnifiedDiffBuilder {
    private const val DEFAULT_CONTEXT = 3

    /** Returns a unified diff string. Empty when there are no changes. */
    fun build(
        filePath: String,
        before: String,
        after: String,
        contextLines: Int = DEFAULT_CONTEXT,
    ): String {
        val beforeLines = before.lines()
        val afterLines = after.lines()
        val patch = DiffUtils.diff(beforeLines, afterLines)
        if (patch.deltas.isEmpty()) return ""

        val unified = UnifiedDiffUtils.generateUnifiedDiff(
            "a/$filePath",
            "b/$filePath",
            beforeLines,
            patch,
            contextLines,
        )
        return unified.joinToString(separator = "\n", postfix = "\n")
    }

    /** Lines added (INSERT chunks + target chunks of CHANGE deltas). */
    fun countAdditions(before: String, after: String): Int =
        countChanges(DiffUtils.diff(before.lines(), after.lines())).additions

    /** Lines removed (DELETE chunks + source chunks of CHANGE deltas). */
    fun countDeletions(before: String, after: String): Int =
        countChanges(DiffUtils.diff(before.lines(), after.lines())).deletions

    /** Combined counts; cheaper than calling both helpers separately. */
    fun counts(before: String, after: String): DiffCounts =
        countChanges(DiffUtils.diff(before.lines(), after.lines()))

    private fun countChanges(patch: Patch<String>): DiffCounts {
        var additions = 0
        var deletions = 0
        for (delta in patch.deltas) {
            when (delta.type) {
                DeltaType.INSERT -> additions += delta.target.size()
                DeltaType.DELETE -> deletions += delta.source.size()
                DeltaType.CHANGE -> {
                    deletions += delta.source.size()
                    additions += delta.target.size()
                }
                DeltaType.EQUAL, null -> Unit
            }
        }
        return DiffCounts(additions = additions, deletions = deletions)
    }

    data class DiffCounts(val additions: Int, val deletions: Int)
}
