package dev.blazelight.p4oc.ui.components.toolwidgets

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import dev.blazelight.p4oc.domain.model.Part
import dev.blazelight.p4oc.domain.model.ToolState
import dev.blazelight.p4oc.ui.components.TuiLoadingIndicator
import dev.blazelight.p4oc.ui.theme.LocalOpenCodeTheme
import dev.blazelight.p4oc.ui.theme.Spacing
import dev.blazelight.p4oc.ui.theme.TuiCodeFontSize
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Main wrapper component for tool call widgets.
 * Handles state cycling between Oneline, Compact, and Expanded views.
 *
 * HITL tools (like question) always display expanded and don't cycle.
 */
@Composable
fun ToolCallWidget(
    tool: Part.Tool,
    defaultState: ToolWidgetState,
    onToolApprove: (String) -> Unit,
    onToolDeny: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalOpenCodeTheme.current

    // HITL tools (pending state) always show expanded
    val isHitl = tool.state is ToolState.Pending
    val effectiveDefault = if (isHitl) ToolWidgetState.EXPANDED else defaultState

    var currentState by remember(tool.callID) { mutableStateOf(effectiveDefault) }

    // Update state if tool becomes pending (HITL)
    LaunchedEffect(isHitl) {
        if (isHitl) {
            currentState = ToolWidgetState.EXPANDED
        }
    }

    val canCycle = !isHitl // HITL tools don't cycle

    AnimatedContent(
        targetState = currentState,
        modifier = modifier.fillMaxWidth(),
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "tool_widget_state"
    ) { state ->
        when (state) {
            ToolWidgetState.ONELINE -> ToolCallOneline(
                tool = tool,
                onClick = if (canCycle) { { currentState = currentState.next() } } else null,
                modifier = Modifier.fillMaxWidth()
            )
            ToolWidgetState.COMPACT -> ToolCallCompact(
                tool = tool,
                onClick = if (canCycle) { { currentState = currentState.next() } } else null,
                modifier = Modifier.fillMaxWidth()
            )
            ToolWidgetState.EXPANDED -> ToolCallExpanded(
                tool = tool,
                onClick = if (canCycle) { { currentState = currentState.next() } } else null,
                onToolApprove = onToolApprove,
                onToolDeny = onToolDeny,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Oneline view: minimal single-line display
 * Format: ✓ bash
 */
@Composable
fun ToolCallOneline(
    tool: Part.Tool,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val theme = LocalOpenCodeTheme.current
    val (icon, color) = getToolStateIcon(tool.state, theme)

    Row(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable(onClick = onClick, role = Role.Button) else Modifier)
            .background(theme.backgroundPanel.copy(alpha = 0.3f))
            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = TuiCodeFontSize.lg
            ),
            color = color
        )
        Text(
            text = tool.toolName,
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = TuiCodeFontSize.lg
            ),
            color = theme.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Running indicator
        if (tool.state is ToolState.Running) {
            TuiLoadingIndicator()
        }
    }
}

/**
 * Compact view: tool name + brief description
 * Format: ✓ ./gradlew assembleDebug
 * Format: ✓ Read Theme.kt (230 lines)
 * Format: ✓ Modified Theme.kt (+45, -12)
 */
@Composable
fun ToolCallCompact(
    tool: Part.Tool,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val theme = LocalOpenCodeTheme.current
    val (icon, color) = getToolStateIcon(tool.state, theme)
    val description = getToolCompactDescription(tool)

    Row(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable(onClick = onClick, role = Role.Button) else Modifier)
            .background(theme.backgroundPanel.copy(alpha = 0.4f))
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = TuiCodeFontSize.xl
            ),
            color = color
        )

        Text(
            text = description,
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = TuiCodeFontSize.xl
            ),
            color = theme.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        // Running indicator
        if (tool.state is ToolState.Running) {
            TuiLoadingIndicator()
        }

        // Diff stats for edit tools
        getDiffStats(tool)?.let { (added, removed) ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    text = "+$added",
                    style = MaterialTheme.typography.labelSmall,
                    color = theme.success
                )
                Text(
                    text = "-$removed",
                    style = MaterialTheme.typography.labelSmall,
                    color = theme.error
                )
            }
        }
    }
}

/**
 * Expanded view: full details with tool-specific UI
 * Delegates to specialized widgets based on tool type
 */
@Composable
@Suppress("FunctionNaming", "LongMethod")
fun ToolCallExpanded(
    tool: Part.Tool,
    onClick: (() -> Unit)?,
    showApprovalActions: Boolean = true,
    approvalRequestId: String = tool.callID,
    onToolApprove: (String) -> Unit,
    onToolDeny: (String) -> Unit,
    onOpenSubSession: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    when (tool.toolName.lowercase()) {
        "bash", "execute", "shell" -> BashWidgetExpanded(
            tool = tool,
            onClick = onClick,
            showApprovalActions = showApprovalActions,
            approvalRequestId = approvalRequestId,
            onToolApprove = onToolApprove,
            onToolDeny = onToolDeny,
            modifier = modifier
        )
        "read", "read_file", "serena_read_file" -> ReadWidgetExpanded(
            tool = tool,
            onClick = onClick,
            modifier = modifier
        )
        "edit", "write", "morph_edit_file", "serena_replace_content", "serena_create_text_file" -> EditWidgetExpanded(
            tool = tool,
            onClick = onClick,
            modifier = modifier
        )
        "task" -> TaskWidgetExpanded(
            tool = tool,
            onClick = onClick,
            showApprovalActions = showApprovalActions,
            approvalRequestId = approvalRequestId,
            onToolApprove = onToolApprove,
            onToolDeny = onToolDeny,
            onOpenSubSession = onOpenSubSession,
            modifier = modifier
        )
        in TODO_TOOLS -> TodoWidgetExpanded(
            tool = tool,
            onClick = onClick,
            showApprovalActions = showApprovalActions,
            approvalRequestId = approvalRequestId,
            onToolApprove = onToolApprove,
            onToolDeny = onToolDeny,
            modifier = modifier,
        )
        "apply_patch" -> ApplyPatchWidgetExpanded(
            tool = tool,
            onClick = onClick,
            showApprovalActions = showApprovalActions,
            approvalRequestId = approvalRequestId,
            onToolApprove = onToolApprove,
            onToolDeny = onToolDeny,
            modifier = modifier,
        )
        else -> DefaultWidgetExpanded(
            tool = tool,
            onClick = onClick,
            showApprovalActions = showApprovalActions,
            approvalRequestId = approvalRequestId,
            onToolApprove = onToolApprove,
            onToolDeny = onToolDeny,
            modifier = modifier
        )
    }
}

// ============== Helper Functions ==============

@Composable
private fun getToolStateIcon(
    state: ToolState,
    theme: dev.blazelight.p4oc.ui.theme.opencode.OpenCodeTheme
): Pair<String, androidx.compose.ui.graphics.Color> {
    return when (state) {
        is ToolState.Running -> "◐" to theme.warning
        is ToolState.Pending -> "○" to theme.secondary
        is ToolState.Error -> "✗" to theme.error
        is ToolState.Completed -> "✓" to theme.success
    }
}

/**
 * Get compact description for a tool based on its type and input
 */
internal fun getToolCompactDescription(tool: Part.Tool): String {
    val input = tool.state.input
    val name = tool.toolName.lowercase()

    // TUI `verb arg` form, matching design 05: `read SseClient.kt`, `grep "reconnect"`, `bash ./gradlew …`.
    return when {
        name in SHELL_TOOLS -> shellDescription(tool, input)
        name in READ_TOOLS -> pathDescription("read", input)
        name in EDIT_TOOLS -> pathDescription("edit", input)
        name in WRITE_TOOLS -> pathDescription("write", input)
        name in GLOB_TOOLS -> patternDescription("glob", input, "file_mask") ?: tool.toolName
        name in GREP_TOOLS -> patternDescription("grep", input, "substring_pattern", 40, quoted = true)
            ?: tool.toolName
        name in TODO_TOOLS -> todoCompactDescription(tool)
        name == "apply_patch" -> applyPatchCompactDescription(tool)
        else -> tool.toolName
    }
}

private val SHELL_TOOLS = setOf("bash", "execute", "shell")
private val READ_TOOLS = setOf("read", "read_file", "serena_read_file")
private val EDIT_TOOLS = setOf("edit", "morph_edit_file", "serena_replace_content")
private val WRITE_TOOLS = setOf("write", "serena_create_text_file")
private val GLOB_TOOLS = setOf("glob", "find", "serena_find_file")
private val GREP_TOOLS = setOf("grep", "search", "serena_search_for_pattern")
private val TODO_TOOLS = setOf("todowrite", "todoread")

private fun shellDescription(tool: Part.Tool, input: JsonObject): String {
    val command = extractParam(input, "command")?.trim()?.takeIf(String::isNotEmpty)?.take(60)
    return command?.let { "bash $it" } ?: tool.toolName
}

private fun pathDescription(verb: String, input: JsonObject): String {
    val path = extractParam(input, "filePath")
        ?: extractParam(input, "path")
        ?: extractParam(input, "relative_path")
    return "$verb ${path?.substringAfterLast("/") ?: "file"}"
}

private fun patternDescription(
    verb: String,
    input: JsonObject,
    fallbackParam: String,
    limit: Int = Int.MAX_VALUE,
    quoted: Boolean = false,
): String? {
    val pattern = extractParam(input, "pattern") ?: extractParam(input, fallbackParam)
    if (pattern == null) return null
    val value = pattern.take(limit)
    return if (quoted) "$verb \"$value\"" else "$verb $value"
}

/**
 * Extract a parameter value from JsonObject
 */
private fun extractParam(input: JsonObject, paramName: String): String? {
    return try {
        input[paramName]?.jsonPrimitive?.content
    } catch (e: Exception) {
        null
    }
}

/**
 * Get diff stats (added, removed lines) for edit tools
 */
private fun getDiffStats(tool: Part.Tool): Pair<Int, Int>? {
    val metadata = when (val state = tool.state) {
        is ToolState.Completed -> state.metadata
        is ToolState.Running -> state.metadata
        is ToolState.Error -> state.metadata
        else -> null
    } ?: return null

    return try {
        val added = metadata["linesAdded"]?.jsonPrimitive?.content?.toIntOrNull() ?: return null
        val removed = metadata["linesRemoved"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
        added to removed
    } catch (e: Exception) {
        null
    }
}
