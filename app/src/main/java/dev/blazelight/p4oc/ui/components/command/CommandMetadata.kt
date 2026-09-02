package dev.blazelight.p4oc.ui.components.command

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import dev.blazelight.p4oc.R
import dev.blazelight.p4oc.domain.model.Command
import dev.blazelight.p4oc.domain.model.CommandSource

@StringRes
fun builtInCommandDescriptionRes(name: String): Int? = when (name) {
    "compact" -> R.string.slash_command_compact_desc
    "clear" -> R.string.slash_command_clear_desc
    "new" -> R.string.slash_command_new_desc
    "undo" -> R.string.slash_command_undo_desc
    "redo" -> R.string.slash_command_redo_desc
    "share" -> R.string.slash_command_share_desc
    "init" -> R.string.slash_command_init_desc
    "help" -> R.string.slash_command_help_desc
    "connect" -> R.string.slash_command_connect_desc
    "bug" -> R.string.slash_command_bug_desc
    else -> null
}

fun resolveBuiltInCommandDescriptions(
    commands: List<Command>,
    builtInDescriptions: Map<String, String>,
): List<Command> = commands.map { command ->
    if (command.source == CommandSource.BuiltIn) {
        builtInDescriptions[command.name]?.let { command.copy(description = it) } ?: command
    } else {
        command
    }
}

@Composable
fun rememberResolvedCommandMetadata(commands: List<Command>): List<Command> {
    val builtInDescriptions = builtInCommandNames.associateWith { name ->
        stringResource(requireNotNull(builtInCommandDescriptionRes(name)))
    }
    return remember(commands, builtInDescriptions) {
        resolveBuiltInCommandDescriptions(commands, builtInDescriptions)
    }
}

private val builtInCommandNames = listOf(
    "compact",
    "clear",
    "new",
    "undo",
    "redo",
    "share",
    "init",
    "help",
    "connect",
    "bug",
)
