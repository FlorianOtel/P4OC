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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import dev.blazelight.p4oc.R
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
@Suppress("LongMethod", "FunctionNaming")
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
            // +1 for the pinned "SLASH COMMANDS" header item at index 0.
            listState.animateScrollToItem(activeIndex + 1)
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
                contentPadding = PaddingValues(bottom = Spacing.hairline)
            ) {
                item { SlashCommandsHeader() }
                when {
                    state.isLoading && filteredCommands.isEmpty() -> {
                        item { SlashCommandMessage(text = stringResource(R.string.slash_commands_loading)) }
                    }
                    state.error != null -> {
                        item { SlashCommandError(text = state.error, onRetry = callbacks.onRetry) }
                        commandItems(filteredCommands, state.activeCommandName, callbacks.onCommandSelected)
                    }
                    filteredCommands.isEmpty() -> item {
                        SlashCommandMessage(
                            text = stringResource(
                                R.string.slash_commands_no_match,
                                state.filter,
                            ),
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

internal class AboveAnchorPopupPositionProvider : PopupPositionProvider {
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
    filterSlashCommands(commands, filter)
}

internal fun filterSlashCommands(commands: List<Command>, filter: String): List<Command> {
    val searchTerm = filter.removePrefix("/").lowercase()
    return if (searchTerm.isEmpty()) {
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
            contentDescription = stringResource(R.string.slash_commands_retry_loading),
            tint = theme.accent,
            modifier = Modifier
                .size(Sizing.minTouchTarget)
                .clickable(role = Role.Button, onClick = onRetry)
                .padding((Sizing.minTouchTarget - Sizing.iconSm) / 2)
        )
    }
}

/** Uppercase, letter-spaced panel header — `SLASH COMMANDS`, matching the design. */
@Composable
@Suppress("FunctionNaming")
private fun SlashCommandsHeader() {
    val theme = LocalOpenCodeTheme.current
    Column(modifier = Modifier.background(theme.background)) {
        Text(
            text = "SLASH COMMANDS",
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp,
            color = theme.textMuted,
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)
        )
        HorizontalDivider(color = theme.border, thickness = Sizing.strokeThin)
    }
}

@Composable
private fun SlashCommandItem(
    command: Command,
    active: Boolean,
    onClick: () -> Unit
) {
    val theme = LocalOpenCodeTheme.current
    val (glyph, glyphColor) = slashCommandIcon(command)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("slash_command_${command.name}")
            .background(if (active) theme.backgroundElement else theme.background)
            .clickable(onClick = onClick, role = Role.Button)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Text(
            text = glyph,
            color = glyphColor,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.widthIn(min = Sizing.iconMd)
        )
        Text(
            text = "/${command.name}",
            color = theme.primary,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(min = Sizing.panelWidthSm)
        )
        command.description?.let { description ->
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = theme.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** Glyph + accent color for a command, keyed on well-known names then falling back to source. */
@Composable
@Suppress("CyclomaticComplexMethod")
private fun slashCommandIcon(command: Command): Pair<String, Color> {
    val theme = LocalOpenCodeTheme.current
    return when (command.name.lowercase()) {
        "model", "models" -> "◆" to theme.secondary
        "agent", "agents", "mode" -> "△" to theme.info
        "diff", "changes" -> "±" to theme.warning
        "undo", "revert" -> "↺" to theme.error
        "redo" -> "↻" to theme.success
        "share" -> "↗" to theme.info
        "unshare" -> "⊘" to theme.textMuted
        "summarize", "compact" -> "≡" to theme.warning
        "clear", "new", "reset" -> "✕" to theme.textMuted
        "init" -> "✦" to theme.accent
        "help" -> "?" to theme.info
        "editor", "edit" -> "✎" to theme.accent
        else -> when (command.source) {
            CommandSource.Skill -> "✧" to theme.info
            CommandSource.Mcp -> "◇" to theme.warning
            CommandSource.Custom -> "▸" to theme.accent
            CommandSource.Subtask -> "▹" to theme.info
            CommandSource.BuiltIn -> "▸" to theme.textMuted
        }
    }
}

internal fun slashCommandSourceCompactLabel(source: CommandSource): String = when (source) {
    CommandSource.BuiltIn -> "[bi]"
    CommandSource.Skill -> "[skill]"
    CommandSource.Mcp -> "[mcp]"
    CommandSource.Custom -> "[custom]"
    CommandSource.Subtask -> "[sub]"
}
