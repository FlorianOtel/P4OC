package dev.blazelight.p4oc.ui.components.toolwidgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import dev.blazelight.p4oc.R
import dev.blazelight.p4oc.domain.model.Part
import dev.blazelight.p4oc.domain.model.Todo
import dev.blazelight.p4oc.domain.model.ToolState
import dev.blazelight.p4oc.ui.components.TuiLoadingIndicator
import dev.blazelight.p4oc.ui.components.todo.TODO_STATUS_CANCELLED
import dev.blazelight.p4oc.ui.components.todo.TODO_STATUS_COMPLETED
import dev.blazelight.p4oc.ui.components.todo.TODO_STATUS_IN_PROGRESS
import dev.blazelight.p4oc.ui.components.todo.TODO_STATUS_PENDING
import dev.blazelight.p4oc.ui.components.todo.todoStatusLabelRes
import dev.blazelight.p4oc.ui.theme.LocalOpenCodeTheme
import dev.blazelight.p4oc.ui.theme.SemanticColors
import dev.blazelight.p4oc.ui.theme.Spacing
import dev.blazelight.p4oc.ui.theme.TuiCodeFontSize
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Resolves the todo list for a tool part. Precedence:
 * 1. `todos` in the tool input (fails closed if malformed)
 * 2. `todos` in state metadata (Running/Completed/Error). A present key — even
 *    `todos: JsonNull` — is authoritative and fails closed if it cannot be
 *    parsed; only an absent key may fall through to the Completed output.
 * 3. For a Completed tool, the exact output parsed as a JSON array (fails
 *    closed on invalid JSON/type)
 *
 * Returns null only when no authoritative source is present or a present
 * source is malformed. A malformed authoritative source never silently falls
 * through to the output.
 */
internal fun todosFromTool(tool: Part.Tool): List<Todo>? {
    val state = tool.state
    val metadataTodos = state.todoMetadata?.get("todos")
    return when {
        state.input.containsKey("todos") -> todosFromElement(state.input["todos"])
        // A present metadata key — even `todos: JsonNull` — is authoritative and fails closed;
        // only an absent key (metadataTodos == null) may fall through to the Completed output.
        metadataTodos != null -> todosFromElement(metadataTodos)
        state is ToolState.Completed -> todosFromOutput(state.output)
        else -> null
    }
}

internal fun todosFromToolInput(input: JsonObject): List<Todo>? = todosFromElement(input["todos"])

private fun todosFromOutput(output: String): List<Todo>? {
    val element = runCatching { Json.parseToJsonElement(output.trim()) }.getOrNull() ?: return null
    return todosFromElement(element)
}

@Suppress("ReturnCount") // fail-closed partial-list parse: any malformed element aborts the whole list
private fun todosFromElement(element: JsonElement?): List<Todo>? {
    val values = element as? JsonArray ?: return null
    val todos = ArrayList<Todo>(values.size)
    for ((index, value) in values.withIndex()) {
        val item = value as? JsonObject ?: return null
        val content = jsonString(item, "content")?.takeIf { it.isNotBlank() } ?: return null
        todos += Todo(
            id = jsonStringOrDefault(item, "id") { "tool-todo-$index" } ?: return null,
            content = content,
            status = jsonStringOrDefault(item, "status") { TODO_STATUS_PENDING } ?: return null,
            priority = jsonStringOrDefault(item, "priority") { TODO_PRIORITY_MEDIUM } ?: return null,
        )
    }
    return todos
}

internal fun todoCompactDescription(tool: Part.Tool): String {
    val todos = todosFromTool(tool)
    return todoItemsForDisplay(tool.state, todos)?.let(::todoCountLabel) ?: tool.toolName
}

/** Formats a progress label matching [dev.blazelight.p4oc.ui.components.todo.TodoTrackerSheet]: completed only. */
internal fun todoCountLabel(todos: List<Todo>): String {
    val completed = todos.count { it.status == TODO_STATUS_COMPLETED }
    return "Todos $completed/${todos.size}"
}

/**
 * The list eligible for presentation in the expanded widget. A `ToolState.Error` may resolve
 * attempted-but-not-persisted input todos at the resolver level, but those must never be shown as
 * if they were persisted, so this derivation drops them for failed tools. Non-error states pass the
 * resolved list through unchanged (null stays null).
 */
internal fun todoItemsForDisplay(state: ToolState, resolvedTodos: List<Todo>?): List<Todo>? =
    resolvedTodos.takeUnless { state is ToolState.Error }

private fun jsonString(obj: JsonObject, key: String): String? =
    (obj[key] as? JsonPrimitive)
        ?.takeIf { it.isString }
        ?.contentOrNull

/**
 * Returns [defaultValue] when [key] is absent or JSON null, the string value when it is a string
 * primitive, or null (failing the whole parse) when it is present but malformed (number, boolean,
 * object, array).
 */
private inline fun jsonStringOrDefault(obj: JsonObject, key: String, defaultValue: () -> String): String? =
    when (val value = obj[key]) {
        null, is JsonNull -> defaultValue()
        is JsonPrimitive -> if (value.isString) value.contentOrNull else null
        else -> null
    }

/** Metadata exposed by Running/Completed/Error states; Pending has none. */
private val ToolState.todoMetadata: JsonObject?
    get() = when (this) {
        is ToolState.Running -> metadata
        is ToolState.Completed -> metadata
        is ToolState.Error -> metadata
        is ToolState.Pending -> null
    }

private const val TODO_PRIORITY_MEDIUM = "medium"

@Composable
@Suppress("FunctionNaming", "LongParameterList")
internal fun TodoWidgetExpanded(
    tool: Part.Tool,
    onClick: (() -> Unit)?,
    showApprovalActions: Boolean,
    approvalRequestId: String = tool.callID,
    onToolApprove: (String) -> Unit,
    onToolDeny: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = LocalOpenCodeTheme.current
    val state = tool.state
    val resolvedTodos = remember(tool.callID, tool.state) { todosFromTool(tool) }
    val displayTodos = todoItemsForDisplay(state, resolvedTodos)

    Column(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .background(theme.backgroundPanel.copy(alpha = 0.5f))
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        TodoWidgetHeader(tool = tool, state = state, todos = displayTodos)

        when {
            state is ToolState.Error -> {
                // Failed: input todos were attempted but not persisted, so showing them would be
                // misleading. Render only the human-readable notice (never raw protocol/stack text).
                Text(
                    text = stringResource(R.string.todo_tool_failed),
                    style = MaterialTheme.typography.bodySmall,
                    color = theme.error,
                )
            }
            displayTodos != null && displayTodos.isNotEmpty() -> {
                displayTodos.forEach { todo -> TodoToolRow(todo) }
            }
            displayTodos != null -> {
                // Valid but empty list: a human-readable empty state instead of a bare 0/0 body.
                Text(
                    text = stringResource(R.string.no_active_todos),
                    style = MaterialTheme.typography.bodySmall,
                    color = theme.textMuted,
                )
            }
            state is ToolState.Completed -> {
                // Malformed/unknown completed todo payload: fail closed with a readable notice.
                Text(
                    text = stringResource(R.string.todo_list_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = theme.textMuted,
                )
            }
            else -> {
                // Pending/Running with no list yet: the header already conveys state; surface the
                // running title without falsely claiming the payload is malformed.
                val runningTitle = (state as? ToolState.Running)?.title
                if (runningTitle != null) {
                    Text(
                        text = runningTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = theme.textMuted,
                    )
                }
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
private fun TodoWidgetHeader(
    tool: Part.Tool,
    state: ToolState,
    todos: List<Todo>?,
) {
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
            modifier = Modifier.semantics {
                contentDescription = statusDesc
            },
        )
        Text(
            text = if (todos == null) tool.toolName else todoCountLabel(todos),
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = TuiCodeFontSize.lg,
            ),
            color = theme.text,
        )
        Spacer(Modifier.weight(1f))
        if (state is ToolState.Running) {
            TuiLoadingIndicator()
        }
    }
}

@Composable
@Suppress("FunctionNaming")
private fun TodoToolRow(todo: Todo) {
    val theme = LocalOpenCodeTheme.current
    val statusLabel = stringResource(todoStatusLabelRes(todo.status))
    val completed = todo.status == TODO_STATUS_COMPLETED
    val cancelled = todo.status == TODO_STATUS_CANCELLED
    val statusColor = SemanticColors.Todo.forStatus(todo.status)
    val marker = when (todo.status) {
        TODO_STATUS_IN_PROGRESS -> "▶"
        TODO_STATUS_COMPLETED -> "✓"
        TODO_STATUS_CANCELLED -> "×"
        else -> "○"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(theme.backgroundElement)
            .padding(Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = marker,
            color = statusColor,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.semantics {
                contentDescription = statusLabel
            },
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
            Text(
                text = todo.content,
                style = MaterialTheme.typography.bodySmall,
                color = if (completed || cancelled) theme.textMuted else theme.text,
                textDecoration = if (completed || cancelled) TextDecoration.LineThrough else null,
            )
            Text(
                text = "[${todo.priority.uppercase()}]",
                style = MaterialTheme.typography.labelSmall,
                color = SemanticColors.Todo.forPriority(todo.priority),
            )
        }
    }
}
