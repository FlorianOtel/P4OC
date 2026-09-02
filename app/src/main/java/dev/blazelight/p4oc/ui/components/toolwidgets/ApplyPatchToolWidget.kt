package dev.blazelight.p4oc.ui.components.toolwidgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import dev.blazelight.p4oc.R
import dev.blazelight.p4oc.domain.model.Part
import dev.blazelight.p4oc.domain.model.ToolState
import dev.blazelight.p4oc.ui.components.TuiLoadingIndicator
import dev.blazelight.p4oc.ui.theme.LocalOpenCodeTheme
import dev.blazelight.p4oc.ui.theme.SemanticColors
import dev.blazelight.p4oc.ui.theme.Sizing
import dev.blazelight.p4oc.ui.theme.Spacing
import dev.blazelight.p4oc.ui.theme.TuiCodeFontSize
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

internal enum class PatchFileAction(val marker: String) {
    ADD("A"),
    UPDATE("M"),
    MOVE("R"),
    DELETE("D"),
}

internal data class PatchFileChange(
    val path: String,
    val action: PatchFileAction,
    val movePath: String? = null,
    /** Exact additions; null when unknown (e.g. a delete with no authoritative metadata). */
    val additions: Int? = null,
    /** Exact deletions; null when unknown (e.g. a delete with no authoritative metadata). */
    val deletions: Int? = null,
)

/**
 * Tri-state extraction of the raw patch text so that a *present* malformed key is authoritative
 * and fails closed instead of silently falling back to another key or to nothing.
 */
internal sealed interface PatchInput {
    data object Absent : PatchInput
    data class Valid(val text: String) : PatchInput
    data object Malformed : PatchInput
}

/** `patchText` is the canonical input; the legacy `patch` key is used only when `patchText` is absent. */
internal fun extractPatchInput(input: JsonObject): PatchInput =
    when (val element = input["patchText"] ?: input["patch"]) {
        null -> PatchInput.Absent
        else -> stringValueOrNull(element)?.let { PatchInput.Valid(it) } ?: PatchInput.Malformed
    }

/** Returns the string content only for a string primitive; null for objects/arrays/null/numeric/boolean. */
private fun stringValueOrNull(element: JsonElement): String? =
    (element as? JsonPrimitive)?.takeIf { it.isString }?.content

internal sealed interface PatchResolution {
    data class Valid(
        val text: String,
        val files: List<PatchFileChange>,
    ) : PatchResolution
    data object Invalid : PatchResolution
}

private const val BEGIN_MARKER = "*** Begin Patch"
private const val END_MARKER = "*** End Patch"
private const val END_OF_FILE = "*** End of File"

/** File-section header prefixes that terminate the body of any preceding section. */
private val SECTION_HEADER_PREFIXES = listOf("*** Add File:", "*** Delete File:", "*** Update File:")

/** True for a line that ends the current section (an End marker or another file header). */
private fun isSectionBoundary(line: String): Boolean =
    line.trim() == END_MARKER || SECTION_HEADER_PREFIXES.any { line.startsWith(it) }

/**
 * Cursor over the patch's line list, positioned just past the Begin marker. [peek] returns the
 * current line without consuming it; [advance] consumes it. Section bodies peek at the current
 * line so they can stop at a section boundary without consuming it.
 */
private class PatchCursor(private val lines: List<String>) {
    var index = 1
        private set

    fun peek(): String? = lines.getOrNull(index)

    fun advance(): String? = lines.getOrNull(index).also { if (it != null) index++ }
}

/**
 * Consumes the Add body following its header: at least one `+` line, then a section boundary.
 * Returns the change or null when the body is missing, stray, or unterminated.
 */
private fun parseAdd(header: String, cursor: PatchCursor): PatchFileChange? {
    val path = header.substring("*** Add File:".length).trim().takeIf { it.isNotEmpty() } ?: return null
    var additions = 0
    var malformed = false
    var done = false
    while (!done && !malformed) {
        val body = cursor.peek()
        when {
            body == null -> malformed = true // ran off without an End marker
            isSectionBoundary(body) -> done = true
            body.startsWith("***") -> malformed = true
            !body.startsWith("+") -> malformed = true
            else -> {
                cursor.advance()
                additions++
            }
        }
    }
    return when {
        malformed || additions == 0 -> null
        else -> PatchFileChange(path, PatchFileAction.ADD, additions = additions, deletions = 0)
    }
}

/**
 * Consumes a Delete header's following boundary: nothing may follow except a section boundary.
 * Returns the change or null when stray content follows. Deletions are unknown (the removed
 * content is not represented); additions are zero.
 */
private fun parseDelete(header: String, cursor: PatchCursor): PatchFileChange? {
    val path = header.substring("*** Delete File:".length).trim().takeIf { it.isNotEmpty() } ?: return null
    val boundary = cursor.peek()?.let(::isSectionBoundary) == true
    return if (boundary) {
        PatchFileChange(path, PatchFileAction.DELETE, additions = 0, deletions = null)
    } else {
        null
    }
}

private data class UpdateCounts(val additions: Int, val deletions: Int)

/**
 * Consumes the Update body following its header (and optional Move): `@@` hunks, `+`/`-` lines,
 * context ` ` lines, and `*** End of File`, until a section boundary. Returns null when the body
 * is missing, stray, or unterminated.
 */
private fun readUpdateBody(cursor: PatchCursor): UpdateCounts? {
    var additions = 0
    var deletions = 0
    var foundBody = false
    var malformed = false
    var done = false
    while (!done && !malformed) {
        val hunk = cursor.peek()
        when {
            hunk == null -> malformed = true // ran off without an End marker
            isSectionBoundary(hunk) -> done = true
            hunk.trim() == END_OF_FILE || hunk.startsWith("@@") || hunk.startsWith(" ") -> {
                foundBody = true
                cursor.advance()
            }
            hunk.startsWith("+") -> {
                additions++
                foundBody = true
                cursor.advance()
            }
            hunk.startsWith("-") -> {
                deletions++
                foundBody = true
                cursor.advance()
            }
            else -> malformed = true
        }
    }
    return if (malformed || !foundBody) null else UpdateCounts(additions, deletions)
}

/**
 * Consumes an Update/Move section: an optional Move line followed by a non-empty body. Returns the
 * change or null when the path, move target, or body is malformed.
 */
private fun parseUpdate(header: String, cursor: PatchCursor): PatchFileChange? {
    val path = header.substring("*** Update File:".length).trim().takeIf { it.isNotEmpty() }
    var movePath: String? = null
    var malformedMove = false
    val next = cursor.peek()
    if (next?.startsWith("*** Move to:") == true) {
        val target = next.substring("*** Move to:".length).trim()
        if (target.isEmpty()) {
            malformedMove = true
        } else {
            movePath = target
            cursor.advance()
        }
    }
    val counts = if (path != null && !malformedMove) readUpdateBody(cursor) else null
    return when {
        path == null || malformedMove || counts == null -> null
        else -> PatchFileChange(
            path = path,
            action = if (movePath != null) PatchFileAction.MOVE else PatchFileAction.UPDATE,
            movePath = movePath,
            additions = counts.additions,
            deletions = counts.deletions,
        )
    }
}

/**
 * Envelope-aware parser for the OpenCode apply_patch grammar.
 *
 * Recognizes ADD/UPDATE/MOVE/DELETE sections in input order (no dedup). A MOVE is an Update
 * header immediately followed by a non-blank `*** Move to:` line; the move stores source+dst.
 * Returns null (invalid) instead of partial results on: missing/misordered envelope, empty paths,
 * a standalone or misplaced Move directive, or any stray/unsupported line within a section.
 */
@Suppress("ReturnCount") // fail-fast envelope parser: any malformed section aborts the whole patch
internal fun parseApplyPatchSections(text: String): List<PatchFileChange>? {
    val lines = text.lines().map { it.trimEnd('\r') }
    if (lines.firstOrNull()?.trim() != BEGIN_MARKER) return null
    val cursor = PatchCursor(lines)
    val changes = mutableListOf<PatchFileChange>()
    while (true) {
        val line = cursor.advance() ?: return null // ran off without an End marker
        val parsed = when {
            line.trim() == END_MARKER -> {
                // Only trailing blank lines may follow the End marker.
                if (lines.drop(cursor.index).any { it.isNotBlank() }) return null
                return changes.takeIf { it.isNotEmpty() } // empty patch is invalid
            }
            line.trim() == BEGIN_MARKER -> return null // duplicate begin
            line.startsWith("*** Add File:") -> parseAdd(line, cursor)
            line.startsWith("*** Delete File:") -> parseDelete(line, cursor)
            line.startsWith("*** Update File:") -> parseUpdate(line, cursor)
            line.startsWith("*** Move to:") -> null // standalone / misplaced Move
            else -> null // stray content outside any section
        }
        if (parsed == null) return null
        changes += parsed
    }
}

/** Authoritative `files` metadata from the tool/state metadata. */
internal sealed interface FilesMetadata {
    data object Absent : FilesMetadata
    data class Valid(val files: List<PatchFileChange>) : FilesMetadata
    data object Malformed : FilesMetadata
}

/**
 * Parses the whole `files` array fail-closed. A present malformed/null `files` key is authoritative
 * and must not fall back. `additions`/`deletions`, when present, must be non-negative integers;
 * when absent they stay unknown (never faked to zero).
 */
@Suppress("ReturnCount") // fail-fast metadata parse: any malformed element aborts the whole array
internal fun extractFilesMetadata(metadata: JsonObject?): FilesMetadata {
    val filesElement = metadata?.get("files") ?: return FilesMetadata.Absent
    val array = filesElement as? JsonArray ?: return FilesMetadata.Malformed
    val files = mutableListOf<PatchFileChange>()
    for (element in array) {
        val obj = element as? JsonObject ?: return FilesMetadata.Malformed
        val relativePath = obj["relativePath"]?.let(::stringValueOrNull)?.trim()
            ?.takeIf { it.isNotEmpty() } ?: return FilesMetadata.Malformed
        val action = fileTypeToAction(
            obj["type"]?.let(::stringValueOrNull)?.trim() ?: return FilesMetadata.Malformed
        )
            ?: return FilesMetadata.Malformed
        val additions = when (val parsed = parseCount(obj, "additions")) {
            is CountParse.Absent -> null
            is CountParse.Value -> parsed.count
            is CountParse.Malformed -> return FilesMetadata.Malformed
        }
        val deletions = when (val parsed = parseCount(obj, "deletions")) {
            is CountParse.Absent -> null
            is CountParse.Value -> parsed.count
            is CountParse.Malformed -> return FilesMetadata.Malformed
        }
        files += PatchFileChange(relativePath, action, null, additions, deletions)
    }
    return FilesMetadata.Valid(files)
}

private fun fileTypeToAction(type: String): PatchFileAction? = when (type) {
    "add" -> PatchFileAction.ADD
    "update" -> PatchFileAction.UPDATE
    "delete" -> PatchFileAction.DELETE
    "move" -> PatchFileAction.MOVE
    else -> null
}

/**
 * Distinguishes absent (unknown → null result) from a present malformed value (authoritative failure).
 * Returns a [CountParse] so the caller can tell the two apart.
 */
private sealed interface CountParse {
    data object Absent : CountParse
    data class Value(val count: Int) : CountParse
    data object Malformed : CountParse
}

private fun parseCount(obj: JsonObject, key: String): CountParse =
    when (val value = obj[key]) {
        null -> CountParse.Absent
        is JsonPrimitive -> {
            // Only numeric primitives are valid: a string like "3" must not be coerced via
            // intOrNull, and a boolean primitive ("true") fails intOrNull anyway.
            val count = if (value.isString) null else value.intOrNull?.takeIf { it >= 0 }
            if (count != null) CountParse.Value(count) else CountParse.Malformed
        }
        else -> CountParse.Malformed
    }

/**
 * Resolves the state-aware presentation model for a tool call. Attempted input is parsed even in
 * the Error state (so the attempted patch is known), but callers decide whether to surface it.
 */
internal fun resolveApplyPatch(tool: Part.Tool): PatchResolution =
    when (val input = extractPatchInput(tool.state.input)) {
        PatchInput.Absent, PatchInput.Malformed -> PatchResolution.Invalid
        is PatchInput.Valid -> resolveValidPatch(input.text, tool.state)
    }

private fun resolveValidPatch(text: String, state: ToolState): PatchResolution {
    val sections = parseApplyPatchSections(text) ?: return PatchResolution.Invalid
    val metadata = when (state) {
        is ToolState.Running -> state.metadata
        is ToolState.Completed -> state.metadata
        is ToolState.Error -> state.metadata
        is ToolState.Pending -> null
    }

    return when (val filesMetadata = extractFilesMetadata(metadata)) {
        is FilesMetadata.Malformed -> PatchResolution.Invalid
        is FilesMetadata.Absent -> PatchResolution.Valid(text, sections)
        is FilesMetadata.Valid ->
            mergeMetadata(filesMetadata.files, sections)?.let { PatchResolution.Valid(text, it) }
                ?: PatchResolution.Invalid
    }
}

/**
 * Merges authoritative metadata with parsed input sections under a same-order contract.
 * Pairs metadata[i] with sections[i]; a size mismatch or action mismatch (extra/dropped files, or
 * an empty authoritative array over nonempty input) resolves to null (Invalid) rather than a silent
 * partial/incorrect result. Counts are NOT required to match: the server computes metadata counts
 * from the applied/formatted old→new diff, so they may legitimately differ from raw patch body
 * lines. Metadata's non-null counts override input; absent metadata counts fall back to input.
 * Path equality is not required so the server's path normalization (e.g. `./a.kt` vs `a.kt`) is
 * tolerated. For a MOVE the metadata exposes the destination as `relativePath`, so the input
 * section's source path is retained.
 */
@Suppress("ReturnCount") // strict same-order merge: any mismatch aborts without a partial result
private fun mergeMetadata(metaFiles: List<PatchFileChange>, sections: List<PatchFileChange>): List<PatchFileChange>? {
    if (metaFiles.size != sections.size) return null
    val merged = ArrayList<PatchFileChange>(metaFiles.size)
    for (index in metaFiles.indices) {
        val meta = metaFiles[index]
        val section = sections[index]
        if (meta.action != section.action) return null
        val action = meta.action
        merged += PatchFileChange(
            path = if (action == PatchFileAction.MOVE) section.path else meta.path,
            action = action,
            movePath = if (action == PatchFileAction.MOVE) meta.path else null,
            additions = meta.additions ?: section.additions,
            deletions = meta.deletions ?: section.deletions,
        )
    }
    return merged
}

internal fun patchSummary(files: List<PatchFileChange>): String {
    val fileWord = if (files.size == 1) "file" else "files"
    val parts = mutableListOf("Patch: ${files.size} $fileWord")
    knownTotal(files, PatchFileChange::additions)?.takeIf { it != 0 }?.let { parts += "+$it" }
    knownTotal(files, PatchFileChange::deletions)?.takeIf { it != 0 }?.let { parts += "-$it" }
    return parts.joinToString(" ")
}

/**
 * Sums a per-file count across all files, but only when every file has a known value; otherwise the
 * total is unknown (e.g. a delete without authoritative metadata has no represented deletion count).
 */
private fun knownTotal(files: List<PatchFileChange>, count: (PatchFileChange) -> Int?): Int? =
    if (files.all { count(it) != null }) files.sumOf { count(it)!! } else null

/**
 * State-aware compact label: affected file count plus known total `+N`/`-N`. The Error state (and any
 * unreadable payload) falls back to the tool name and never summarizes attempted input.
 */
internal fun applyPatchCompactDescription(tool: Part.Tool): String =
    if (tool.state is ToolState.Error) {
        tool.toolName
    } else {
        (resolveApplyPatch(tool) as? PatchResolution.Valid)?.let { patchSummary(it.files) }
            ?: tool.toolName
    }

/**
 * Expanded-header label derived from an already-resolved patch (never re-resolves). Returns null in
 * the Error state so the header renders the tool name and never summarizes attempted input, and
 * null for Invalid; otherwise it is the same state-aware summary as compact.
 */
internal fun applyPatchHeaderSummary(state: ToolState, resolution: PatchResolution): String? {
    if (state is ToolState.Error) return null
    return (resolution as? PatchResolution.Valid)?.let { patchSummary(it.files) }
}

@Composable
@Suppress("FunctionNaming", "LongParameterList")
internal fun ApplyPatchWidgetExpanded(
    tool: Part.Tool,
    onClick: (() -> Unit)?,
    showApprovalActions: Boolean = true,
    approvalRequestId: String = tool.callID,
    onToolApprove: (String) -> Unit,
    onToolDeny: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = tool.state
    val resolution = remember(tool.callID, state) { resolveApplyPatch(tool) }
    val theme = LocalOpenCodeTheme.current
    val summary = applyPatchHeaderSummary(state, resolution)

    Column(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .background(theme.backgroundPanel.copy(alpha = 0.5f))
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        ApplyPatchHeader(tool = tool, state = state, summary = summary)

        when {
            state is ToolState.Error -> {
                Text(
                    text = stringResource(R.string.patch_tool_failed),
                    style = MaterialTheme.typography.bodySmall,
                    color = theme.error,
                )
            }
            resolution is PatchResolution.Valid -> {
                resolution.files.forEach { change -> PatchFileRow(change) }
                PatchTextPreview(resolution.text)
            }
            else -> {
                Text(
                    text = stringResource(R.string.patch_details_unreadable),
                    style = MaterialTheme.typography.bodySmall,
                    color = theme.textMuted,
                )
            }
        }

        if (showApprovalActions) {
            PendingApprovalButtons(
                onApprove = { onToolApprove(approvalRequestId) },
                onDeny = { onToolDeny(approvalRequestId) },
            )
        }
    }
}

@Composable
@Suppress("FunctionNaming")
private fun ApplyPatchHeader(tool: Part.Tool, state: ToolState, summary: String?) {
    val theme = LocalOpenCodeTheme.current
    val (icon, color) = getStateIconColor(state, theme)
    val statusDesc = when (state) {
        is ToolState.Running -> stringResource(R.string.cd_agent_running)
        is ToolState.Pending -> stringResource(R.string.cd_tool_status)
        is ToolState.Error -> stringResource(R.string.cd_agent_failed)
        is ToolState.Completed -> stringResource(R.string.cd_agent_completed)
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            modifier = Modifier.semantics { contentDescription = statusDesc },
        )
        Text(
            text = summary ?: tool.toolName,
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = TuiCodeFontSize.lg,
            ),
            color = theme.text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (state is ToolState.Running) {
            TuiLoadingIndicator()
        }
    }
}

@Composable
@Suppress("FunctionNaming")
private fun PatchFileRow(change: PatchFileChange) {
    val theme = LocalOpenCodeTheme.current
    val color = when (change.action) {
        PatchFileAction.ADD -> theme.success
        PatchFileAction.UPDATE -> theme.accent
        PatchFileAction.MOVE -> theme.info
        PatchFileAction.DELETE -> theme.error
    }
    val actionLabel = stringResource(
        when (change.action) {
            PatchFileAction.ADD -> R.string.patch_action_add
            PatchFileAction.UPDATE -> R.string.patch_action_update
            PatchFileAction.MOVE -> R.string.patch_action_move
            PatchFileAction.DELETE -> R.string.patch_action_delete
        }
    )
    val label = when (change.action) {
        PatchFileAction.MOVE -> "${change.path} → ${change.movePath.orEmpty()}"
        else -> change.path
    }
    val counts = buildString {
        change.additions?.let { append("+$it ") }
        change.deletions?.let { append("-$it") }
    }.trim()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = change.action.marker,
            color = color,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.semantics { contentDescription = actionLabel },
        )
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelSmall,
            color = theme.text,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (counts.isNotEmpty()) {
            Text(
                text = counts,
                style = MaterialTheme.typography.labelSmall,
                color = theme.textMuted,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

@Composable
@Suppress("FunctionNaming")
private fun PatchTextPreview(text: String) {
    val theme = LocalOpenCodeTheme.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = Sizing.embeddedScrollMaxHeight)
            .background(theme.backgroundElement)
            .horizontalScroll(rememberScrollState())
            .verticalScroll(rememberScrollState())
            .padding(Spacing.sm),
    ) {
        Column {
            patchPreviewLines(text).forEach { line -> PatchLine(line) }
        }
    }
}

/**
 * Preview lines after stripping protocol directive lines that begin at column zero with `*** `
 * (Begin/End envelope, Add/Update/Delete File, Move to, End of File). Their file identity is already
 * shown by the structured [PatchFileRow] entries above, so the preview keeps only the applied content:
 * `@@` hunks, `+`/`-` lines, and space-prefixed context lines (including context/content that merely
 * contains a literal `***` after its `+`/space prefix).
 */
internal fun patchPreviewLines(text: String): List<String> =
    text.lineSequence().filterNot { it.startsWith("*** ") }.toList()

@Composable
@Suppress("FunctionNaming")
private fun PatchLine(line: String) {
    val theme = LocalOpenCodeTheme.current
    val (background, foreground) = when {
        line.startsWith("+") -> SemanticColors.Diff.addedBackground to SemanticColors.Diff.addedText
        line.startsWith("-") -> SemanticColors.Diff.removedBackground to SemanticColors.Diff.removedText
        line.startsWith("@@") -> theme.accent.copy(alpha = 0.1f) to theme.accent
        else -> Color.Transparent to theme.textMuted
    }
    Text(
        text = line,
        modifier = Modifier.background(background).padding(horizontal = Spacing.xs),
        style = MaterialTheme.typography.bodySmall,
        color = foreground,
        fontFamily = FontFamily.Monospace,
        fontSize = TuiCodeFontSize.sm,
    )
}
