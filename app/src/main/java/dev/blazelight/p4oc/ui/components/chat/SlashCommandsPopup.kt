package dev.blazelight.p4oc.ui.components.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import dev.blazelight.p4oc.domain.model.Command
import dev.blazelight.p4oc.domain.model.CommandSource
import dev.blazelight.p4oc.ui.theme.LocalOpenCodeTheme
import dev.blazelight.p4oc.ui.theme.Sizing
import dev.blazelight.p4oc.ui.theme.Spacing

/**
 * Inline popup that appears above the chat input when user types "/"
 * Shows filtered list of available slash commands
 */
@Composable
fun SlashCommandsPopup(
    state: SlashCommandsPopupState,
    callbacks: SlashCommandsPopupCallbacks,
    modifier: Modifier = Modifier
) {
    val theme = LocalOpenCodeTheme.current
    val filteredCommands = rememberFilteredCommands(state.commands, state.filter)
    val activeIndex = filteredCommands.indexOfFirst { it.name == state.activeCommandName }
    val listState = rememberLazyListState()

    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0) {
            listState.animateScrollToItem(activeIndex)
        }
    }

    Popup(
        popupPositionProvider = AboveAnchorPopupPositionProvider(),
        onDismissRequest = callbacks.onDismiss,
        properties = PopupProperties(
            focusable = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = modifier
                .heightIn(max = 220.dp)
                .padding(bottom = Spacing.xs)
                .testTag("slash_commands_popup")
                .border(Sizing.strokeMd, theme.border, RectangleShape),
            shape = RectangleShape,
            color = theme.background,
            shadowElevation = 8.dp
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(vertical = Spacing.hairline)
            ) {
                when {
                    state.isLoading && filteredCommands.isEmpty() -> {
                        item { SlashCommandMessage(text = "Loading workspace commands...") }
                    }
                    state.error != null -> {
                        item { SlashCommandError(text = state.error, onRetry = callbacks.onRetry) }
                        commandItems(filteredCommands, state.activeCommandName, callbacks.onCommandSelected)
                    }
                    filteredCommands.isEmpty() -> item {
                        SlashCommandMessage(
                            text = "No commands match ${state.filter}",
                            modifier = Modifier.testTag("slash_commands_empty")
                        )
                    }
                    else -> {
                        commandItems(filteredCommands, state.activeCommandName, callbacks.onCommandSelected)
                    }
                }
            }
        }
    }
}

private class AboveAnchorPopupPositionProvider : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val x = anchorBounds.left.coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
        val y = (anchorBounds.top - popupContentSize.height).coerceAtLeast(0)
        return IntOffset(x, y)
    }
}

@Stable
data class SlashCommandsPopupState(
    val commands: List<Command>,
    val filter: String,
    val isLoading: Boolean,
    val error: String?,
    val activeCommandName: String?
)

@Stable
data class SlashCommandsPopupCallbacks(
    val onRetry: () -> Unit,
    val onCommandSelected: (Command) -> Unit,
    val onDismiss: () -> Unit
)

@Composable
private fun rememberFilteredCommands(
    commands: List<Command>,
    filter: String
): List<Command> = remember(commands, filter) {
    val searchTerm = filter.removePrefix("/").lowercase()
    if (searchTerm.isEmpty()) {
        commands
    } else {
        commands.filter { command ->
            command.name.lowercase().contains(searchTerm) ||
                command.description?.lowercase()?.contains(searchTerm) == true
        }
    }
}

private fun LazyListScope.commandItems(
    commands: List<Command>,
    activeCommandName: String?,
    onCommandSelected: (Command) -> Unit
) {
    items(commands, key = { it.name }) { command ->
        SlashCommandItem(
            command = command,
            active = command.name == activeCommandName,
            onClick = { onCommandSelected(command) }
        )
    }
}

@Composable
private fun SlashCommandMessage(
    text: String,
    modifier: Modifier = Modifier
) {
    val theme = LocalOpenCodeTheme.current
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace,
        color = theme.textMuted,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)
    )
}

@Composable
private fun SlashCommandError(
    text: String,
    onRetry: () -> Unit
) {
    val theme = LocalOpenCodeTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = theme.warning,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = "Retry loading commands",
            tint = theme.accent,
            modifier = Modifier
                .size(Sizing.iconSm)
                .clickable(role = Role.Button, onClick = onRetry)
        )
    }
}

@Composable
private fun SlashCommandItem(
    command: Command,
    active: Boolean,
    onClick: () -> Unit
) {
    val theme = LocalOpenCodeTheme.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("slash_command_${command.name}")
            .background(if (active) theme.backgroundElement else theme.background)
            .clickable(onClick = onClick, role = Role.Button)
            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Text(
            text = "/${command.name}",
            color = theme.accent,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = command.source.compactLabel(),
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = command.source.badgeColor(),
            maxLines = 1
        )
    }
}

@Composable
private fun CommandSource.badgeColor() = when (this) {
    CommandSource.BuiltIn -> LocalOpenCodeTheme.current.textMuted
    CommandSource.Skill -> LocalOpenCodeTheme.current.info
    CommandSource.Mcp -> LocalOpenCodeTheme.current.warning
    CommandSource.Custom -> LocalOpenCodeTheme.current.accent
    CommandSource.Subtask -> LocalOpenCodeTheme.current.info
}

private fun CommandSource.compactLabel(): String = when (this) {
    CommandSource.BuiltIn -> "[bi]"
    CommandSource.Skill -> "[skill]"
    CommandSource.Mcp -> "[mcp]"
    CommandSource.Custom -> "[custom]"
    CommandSource.Subtask -> "[sub]"
}
