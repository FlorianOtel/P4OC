package dev.blazelight.p4oc.ui.components.todo

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.blazelight.p4oc.R
import dev.blazelight.p4oc.domain.model.Todo
import dev.blazelight.p4oc.ui.components.TuiLoadingIndicator
import dev.blazelight.p4oc.ui.theme.LocalOpenCodeTheme
import dev.blazelight.p4oc.ui.theme.SemanticColors
import dev.blazelight.p4oc.ui.theme.Sizing
import dev.blazelight.p4oc.ui.theme.Spacing

internal const val TODO_STATUS_PENDING = "pending"
internal const val TODO_STATUS_IN_PROGRESS = "in_progress"
internal const val TODO_STATUS_COMPLETED = "completed"
internal const val TODO_STATUS_CANCELLED = "cancelled"
private const val PERCENT_FACTOR = 100

internal fun todoStatusLabelRes(status: String): Int = when (status) {
    TODO_STATUS_PENDING -> R.string.todo_status_pending
    TODO_STATUS_IN_PROGRESS -> R.string.todo_status_in_progress
    TODO_STATUS_COMPLETED -> R.string.todo_status_completed
    TODO_STATUS_CANCELLED -> R.string.todo_status_cancelled
    else -> R.string.todo_status_unknown
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoTrackerSheet(
    todos: List<Todo>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit
) {
    val theme = LocalOpenCodeTheme.current
    val completedCount = todos.count { it.status == TODO_STATUS_COMPLETED }
    val totalCount = todos.size
    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = theme.background,
        shape = RectangleShape,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Spacing.xl)
        ) {
            // TUI Header
            Surface(
                color = theme.backgroundElement,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "[ ${stringResource(R.string.todo_tracker)} ]",
                        style = MaterialTheme.typography.titleMedium,
                        color = theme.text
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (totalCount > 0) {
                            Text(
                                text = stringResource(R.string.progress_count, completedCount, totalCount),
                                style = MaterialTheme.typography.labelMedium,
                                color = theme.accent
                            )
                        }
                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier.size(Sizing.iconButtonSm)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.cd_refresh),
                                tint = theme.textMuted,
                                modifier = Modifier.size(Sizing.iconSm)
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(Sizing.iconButtonSm)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.close),
                                tint = theme.textMuted,
                                modifier = Modifier.size(Sizing.iconSm)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = theme.border)

            // Progress bar
            if (totalCount > 0) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.md)
                ) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Sizing.progressBarHeightSm),
                        color = theme.accent,
                        trackColor = theme.border
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = stringResource(
                            R.string.percent_complete,
                            (progress * PERCENT_FACTOR).toInt(),
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = theme.textMuted
                    )
                }
            }

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        TuiLoadingIndicator()
                    }
                }
                todos.isEmpty() -> {
                    TuiEmptyTodosView()
                }
                else -> {
                    TuiTodoList(todos = todos)
                }
            }
        }
    }
}

@Composable
private fun TuiEmptyTodosView() {
    val theme = LocalOpenCodeTheme.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Text(
            text = "✓",
            style = MaterialTheme.typography.displayMedium,
            color = theme.success
        )
        Text(
            text = stringResource(R.string.no_active_todos),
            style = MaterialTheme.typography.titleSmall,
            color = theme.text
        )
        Text(
            text = stringResource(R.string.agent_no_tasks),
            style = MaterialTheme.typography.bodySmall,
            color = theme.textMuted
        )
    }
}

@Composable
private fun TuiTodoList(todos: List<Todo>) {
    val theme = LocalOpenCodeTheme.current
    val groupedTodos = todos.groupBy { it.status }
    val inProgress = groupedTodos[TODO_STATUS_IN_PROGRESS] ?: emptyList()
    val pending = groupedTodos[TODO_STATUS_PENDING] ?: emptyList()
    val completed = groupedTodos[TODO_STATUS_COMPLETED] ?: emptyList()
    val cancelled = groupedTodos[TODO_STATUS_CANCELLED] ?: emptyList()

    LazyColumn(
        modifier = Modifier
            .heightIn(max = 400.dp)
            .padding(horizontal = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        if (inProgress.isNotEmpty()) {
            item {
                TuiTodoSectionHeader(
                    status = TODO_STATUS_IN_PROGRESS,
                    count = inProgress.size,
                    color = theme.accent,
                )
            }
            items(inProgress, key = { it.id }) { todo ->
                TuiTodoItem(todo = todo)
            }
        }

        if (pending.isNotEmpty()) {
            item {
                TuiTodoSectionHeader(
                    status = TODO_STATUS_PENDING,
                    count = pending.size,
                    color = theme.textMuted,
                )
            }
            items(pending, key = { it.id }) { todo ->
                TuiTodoItem(todo = todo)
            }
        }

        if (completed.isNotEmpty()) {
            item {
                TuiTodoSectionHeader(
                    status = TODO_STATUS_COMPLETED,
                    count = completed.size,
                    color = theme.success,
                )
            }
            items(completed, key = { it.id }) { todo ->
                TuiTodoItem(todo = todo)
            }
        }

        if (cancelled.isNotEmpty()) {
            item {
                TuiTodoSectionHeader(
                    status = TODO_STATUS_CANCELLED,
                    count = cancelled.size,
                    color = theme.error,
                )
            }
            items(cancelled, key = { it.id }) { todo ->
                TuiTodoItem(todo = todo)
            }
        }
    }
}

@Composable
private fun TuiTodoSectionHeader(status: String, count: Int, color: Color) {
    val theme = LocalOpenCodeTheme.current
    val (statusIcon, _) = getStatusInfo(status)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = statusIcon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(Sizing.iconXs),
        )
        Text(
            text = stringResource(todoStatusLabelRes(status)),
            style = MaterialTheme.typography.labelMedium,
            color = color
        )
        Text(
            text = "[$count]",
            style = MaterialTheme.typography.labelSmall,
            color = theme.textMuted
        )
    }
}

@Composable
private fun TuiTodoItem(todo: Todo) {
    val theme = LocalOpenCodeTheme.current
    val (statusIcon, statusColor) = getStatusInfo(todo.status)
    val priorityColor = getPriorityColor(todo.priority)
    val isCompleted = todo.status == TODO_STATUS_COMPLETED
    val isCancelled = todo.status == TODO_STATUS_CANCELLED

    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable(role = Role.Button) { expanded = !expanded },
        color = if (isCompleted || isCancelled) {
            theme.backgroundElement.copy(alpha = 0.5f)
        } else {
            theme.backgroundElement
        },
        shape = RectangleShape
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.Top
        ) {
            // Status indicator
            Icon(
                imageVector = statusIcon,
                contentDescription = stringResource(todoStatusLabelRes(todo.status)),
                tint = statusColor,
                modifier = Modifier.size(Sizing.iconXs),
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = todo.content,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (todo.status == TODO_STATUS_IN_PROGRESS) FontWeight.Medium else FontWeight.Normal,
                    textDecoration = if (isCompleted || isCancelled) TextDecoration.LineThrough else null,
                    color = if (isCompleted || isCancelled) {
                        theme.textMuted
                    } else {
                        theme.text
                    },
                    maxLines = if (expanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(Spacing.xxs))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "[${todo.priority.uppercase()}]",
                        style = MaterialTheme.typography.labelSmall,
                        color = priorityColor
                    )
                }
            }
        }
    }
}

@Composable
private fun getStatusInfo(status: String): Pair<androidx.compose.ui.graphics.vector.ImageVector, Color> {
    return when (status) {
        TODO_STATUS_PENDING -> Icons.Default.Schedule to SemanticColors.Todo.pending
        TODO_STATUS_IN_PROGRESS -> Icons.Default.PlayCircle to SemanticColors.Todo.inProgress
        TODO_STATUS_COMPLETED -> Icons.Default.CheckCircle to SemanticColors.Todo.completed
        TODO_STATUS_CANCELLED -> Icons.Default.Cancel to SemanticColors.Todo.cancelled
        else -> Icons.Default.Circle to SemanticColors.Todo.pending
    }
}

@Composable
private fun getPriorityColor(priority: String): Color {
    return SemanticColors.Todo.forPriority(priority)
}
