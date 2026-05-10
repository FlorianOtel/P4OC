package dev.blazelight.p4oc.ui.diff

import com.github.difflib.UnifiedDiffUtils
import com.github.difflib.patch.AbstractDelta
import com.github.difflib.patch.DeltaType
import com.github.difflib.patch.Patch

internal data class ParsedDiff(
    val files: List<ParsedFileDiff>
)

internal data class ParsedFileDiff(
    val oldFileName: String?,
    val newFileName: String?,
    val displayFileName: String,
    val hunks: List<ParsedHunk>
)

internal data class ParsedHunk(
    val header: String,
    val oldStart: Int,
    val oldCount: Int,
    val newStart: Int,
    val newCount: Int,
    val lines: List<ParsedDiffLine>
)

internal data class ParsedDiffLine(
    val type: ParsedDiffLineType,
    val content: String,
    val oldLineNumber: Int?,
    val newLineNumber: Int?
)

internal enum class ParsedDiffLineType {
    CONTEXT,
    ADDED,
    REMOVED,
    HEADER
}

internal object ParsedDiffParser {
    private val hunkHeaderRegex = Regex("""^@@ -(\d+)(?:,(\d+))? \+(\d+)(?:,(\d+))? @@.*$""")

    fun parse(diffContent: String): ParsedDiff {
        if (diffContent.isBlank()) return ParsedDiff(emptyList())

        val sections = splitIntoFileSections(diffContent.lines())
        val files = sections.mapNotNull { parseFileSection(it) }
        return ParsedDiff(files)
    }

    private fun splitIntoFileSections(lines: List<String>): List<List<String>> {
        val sections = mutableListOf<List<String>>()
        var current = mutableListOf<String>()
        var insideHunk = false

        fun flush() {
            if (current.any { it.startsWith("@@") || it.startsWith("--- ") || it.startsWith("+++ ") }) {
                sections.add(current.toList())
            }
            current = mutableListOf()
            insideHunk = false
        }

        for (line in lines) {
            val startsNewGitFile = line.startsWith("diff --git ") && current.isNotEmpty()
            val startsNewPlainFile = line.startsWith("--- ") && current.isNotEmpty() && !insideHunk
            if (startsNewGitFile || startsNewPlainFile) {
                flush()
            }

            current.add(line)

            if (line.startsWith("@@")) {
                insideHunk = true
            }
        }

        flush()
        return sections
    }

    private fun parseFileSection(lines: List<String>): ParsedFileDiff? {
        val headerInfo = extractFileHeaders(lines)
        val hunkHeaderCount = lines.count { it.startsWith("@@") }
        val canonicalPatch = canonicalPatchFor(lines, headerInfo, hunkHeaderCount) ?: return null

        val hunkHeaders = collectHunkSections(lines)
        val hunks = mapPatchToHunks(canonicalPatch, hunkHeaders)
        if (hunks.isEmpty()) return null

        return ParsedFileDiff(
            oldFileName = headerInfo.oldFileName,
            newFileName = headerInfo.newFileName,
            displayFileName = displayFileName(headerInfo.newFileName, headerInfo.oldFileName),
            hunks = hunks
        )
    }

    private fun canonicalPatchFor(
        lines: List<String>,
        headerInfo: FileHeaderInfo,
        hunkHeaderCount: Int
    ): Patch<String>? {
        if (hunkHeaderCount == 0) return null

        val linesForLibrary = if (headerInfo.hasBothHeaders) {
            lines
        } else {
            // Lenient fallback for headerless unified hunks emitted by some tools. The synthetic
            // file headers are only for java-diff-utils; renderer rows still come from original lines.
            listOf("--- original", "+++ revised") + lines.dropWhile { !it.startsWith("@@") }
        }

        val patch = try {
            UnifiedDiffUtils.parseUnifiedDiff(linesForLibrary)
        } catch (_: RuntimeException) {
            return null
        }

        return if (patch.getDeltas().size == hunkHeaderCount) patch else null
    }

    private fun extractFileHeaders(lines: List<String>): FileHeaderInfo {
        var oldFileName: String? = null
        var newFileName: String? = null
        var sawOldHeader = false
        var sawNewHeader = false
        var insideHunk = false

        for (line in lines) {
            when {
                line.startsWith("@@") -> insideHunk = true
                !insideHunk && line.startsWith("--- ") -> {
                    sawOldHeader = true
                    oldFileName = normalizeFileName(line.removePrefix("--- "))
                }
                !insideHunk && line.startsWith("+++ ") -> {
                    sawNewHeader = true
                    newFileName = normalizeFileName(line.removePrefix("+++ "))
                }
            }
        }

        return FileHeaderInfo(
            oldFileName = oldFileName,
            newFileName = newFileName,
            hasBothHeaders = sawOldHeader && sawNewHeader
        )
    }

    private fun mapPatchToHunks(
        patch: Patch<String>,
        headers: List<ParsedHunkHeader>
    ): List<ParsedHunk> {
        val deltas = patch.getDeltas()
        if (deltas.size != headers.size) return emptyList()
        return deltas.zip(headers).map { (delta, header) -> mapDeltaToHunk(delta, header) }
    }

    private fun mapDeltaToHunk(delta: AbstractDelta<String>, header: ParsedHunkHeader): ParsedHunk {
        val lines = buildList {
            add(
                ParsedDiffLine(
                    type = ParsedDiffLineType.HEADER,
                    content = header.header,
                    oldLineNumber = null,
                    newLineNumber = null
                )
            )

            var oldLine = header.oldStart.coerceAtLeast(1)
            var newLine = header.newStart.coerceAtLeast(1)

            header.bodyLines.forEach { line ->
                when {
                    line.startsWith("\\") -> Unit
                    line.startsWith("+") -> {
                        val content = line.drop(1)
                        if (content in delta.target.lines || delta.type == DeltaType.INSERT || delta.type == DeltaType.CHANGE) {
                            add(ParsedDiffLine(ParsedDiffLineType.ADDED, content, null, newLine++))
                        }
                    }
                    line.startsWith("-") -> {
                        val content = line.drop(1)
                        if (content in delta.source.lines || delta.type == DeltaType.DELETE || delta.type == DeltaType.CHANGE) {
                            add(ParsedDiffLine(ParsedDiffLineType.REMOVED, content, oldLine++, null))
                        }
                    }
                    line.startsWith(" ") -> {
                        val content = line.drop(1)
                        add(ParsedDiffLine(ParsedDiffLineType.CONTEXT, content, oldLine++, newLine++))
                    }
                    line.isEmpty() -> {
                        add(ParsedDiffLine(ParsedDiffLineType.CONTEXT, "", oldLine++, newLine++))
                    }
                }
            }
        }

        return ParsedHunk(
            header = header.header,
            oldStart = header.oldStart,
            oldCount = header.oldCount,
            newStart = header.newStart,
            newCount = header.newCount,
            lines = lines
        )
    }

    private fun collectHunkSections(lines: List<String>): List<ParsedHunkHeader> {
        val hunks = mutableListOf<ParsedHunkHeader>()
        var current: ParsedHunkHeader? = null

        fun flush() {
            current?.let { hunks.add(it) }
            current = null
        }

        for (line in lines) {
            if (line.startsWith("@@")) {
                flush()
                current = parseHunkHeader(line)
            } else {
                current = current?.let { it.copy(bodyLines = it.bodyLines + line) }
            }
        }
        flush()

        return hunks
    }

    private fun parseHunkHeader(header: String): ParsedHunkHeader? {
        val match = hunkHeaderRegex.find(header) ?: return null
        return ParsedHunkHeader(
            header = header,
            oldStart = match.groupValues[1].toIntOrNull() ?: return null,
            oldCount = match.groupValues[2].takeIf { it.isNotEmpty() }?.toIntOrNull() ?: 1,
            newStart = match.groupValues[3].toIntOrNull() ?: return null,
            newCount = match.groupValues[4].takeIf { it.isNotEmpty() }?.toIntOrNull() ?: 1
        )
    }

    private fun normalizeFileName(rawFileName: String): String? {
        val withoutTimestamp = rawFileName.substringBefore('\t').substringBefore("  ").trim()
        if (withoutTimestamp.isEmpty() || withoutTimestamp == "/dev/null") return null
        return withoutTimestamp.removePrefix("a/").removePrefix("b/")
    }

    private fun displayFileName(newFileName: String?, oldFileName: String?): String =
        newFileName ?: oldFileName.orEmpty()

    private data class FileHeaderInfo(
        val oldFileName: String?,
        val newFileName: String?,
        val hasBothHeaders: Boolean
    )

    private data class ParsedHunkHeader(
        val header: String,
        val oldStart: Int,
        val oldCount: Int,
        val newStart: Int,
        val newCount: Int,
        val bodyLines: List<String> = emptyList()
    )
}

internal fun ParsedDiff.primaryFile(): ParsedFileDiff? = files.firstOrNull()

internal fun ParsedDiff.allHunks(): List<ParsedHunk> = files.flatMap { it.hunks }
