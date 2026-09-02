package dev.blazelight.p4oc.ui.components.chat

import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import dev.blazelight.p4oc.domain.model.Command
import dev.blazelight.p4oc.domain.model.CommandSource
import org.junit.Assert.assertEquals
import org.junit.Test

class SlashCommandsPopupTest {
    @Test
    fun `filter slash commands returns all commands for empty slash filter`() {
        val commands = listOf(
            Command(name = "compact", description = "Summarize this session", source = CommandSource.BuiltIn),
            Command(name = "deploy", description = "Ship the current workspace", source = CommandSource.Custom),
            Command(name = "review", description = "Run a code review", source = CommandSource.Skill),
        )

        assertEquals(commands, filterSlashCommands(commands, ""))
        assertEquals(commands, filterSlashCommands(commands, "/"))
    }

    @Test
    fun `filter slash commands filters by command name`() {
        val commands = listOf(
            Command(name = "compact", description = "Summarize this session", source = CommandSource.BuiltIn),
            Command(name = "deploy", description = "Ship the current workspace", source = CommandSource.Custom),
            Command(name = "review", description = "Run a code review", source = CommandSource.Skill),
        )

        assertEquals(
            listOf(commands[1]),
            filterSlashCommands(commands, "/dep")
        )
        assertEquals(
            listOf(commands[0]),
            filterSlashCommands(commands, "COM")
        )
    }

    @Test
    fun `filter slash commands filters by resolved description text`() {
        val commands = listOf(
            Command(name = "compact", description = "Summarize this session", source = CommandSource.BuiltIn),
            Command(name = "deploy", description = "Ship the current workspace", source = CommandSource.Custom),
            Command(name = "review", description = "Run a code review", source = CommandSource.Skill),
            Command(name = "empty", description = null, source = CommandSource.Custom),
        )

        assertEquals(
            listOf(commands[0]),
            filterSlashCommands(commands, "/summarize")
        )
        assertEquals(
            listOf(commands[2]),
            filterSlashCommands(commands, "CODE REVIEW")
        )
    }

    @Test
    fun `slash command source compact label returns expected labels`() {
        assertEquals("[bi]", slashCommandSourceCompactLabel(CommandSource.BuiltIn))
        assertEquals("[skill]", slashCommandSourceCompactLabel(CommandSource.Skill))
        assertEquals("[mcp]", slashCommandSourceCompactLabel(CommandSource.Mcp))
        assertEquals("[custom]", slashCommandSourceCompactLabel(CommandSource.Custom))
        assertEquals("[sub]", slashCommandSourceCompactLabel(CommandSource.Subtask))
    }

    @Test
    fun `above anchor popup position provider positions popup above anchor`() {
        val position = AboveAnchorPopupPositionProvider().calculatePosition(
            anchorBounds = IntRect(left = 40, top = 160, right = 240, bottom = 200),
            windowSize = IntSize(width = 300, height = 500),
            layoutDirection = LayoutDirection.Ltr,
            popupContentSize = IntSize(width = 120, height = 90),
        )

        assertEquals(40, position.x)
        assertEquals(70, position.y)
    }

    @Test
    fun `above anchor popup position provider clamps x within window`() {
        val position = AboveAnchorPopupPositionProvider().calculatePosition(
            anchorBounds = IntRect(left = 260, top = 160, right = 300, bottom = 200),
            windowSize = IntSize(width = 300, height = 500),
            layoutDirection = LayoutDirection.Ltr,
            popupContentSize = IntSize(width = 120, height = 90),
        )

        assertEquals(180, position.x)
        assertEquals(70, position.y)
    }

    @Test
    fun `above anchor popup position provider clamps y to zero when not enough space above`() {
        val position = AboveAnchorPopupPositionProvider().calculatePosition(
            anchorBounds = IntRect(left = 40, top = 50, right = 240, bottom = 90),
            windowSize = IntSize(width = 300, height = 500),
            layoutDirection = LayoutDirection.Ltr,
            popupContentSize = IntSize(width = 120, height = 90),
        )

        assertEquals(40, position.x)
        assertEquals(0, position.y)
    }
}
