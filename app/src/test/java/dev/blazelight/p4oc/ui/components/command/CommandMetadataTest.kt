package dev.blazelight.p4oc.ui.components.command

import dev.blazelight.p4oc.R
import dev.blazelight.p4oc.domain.model.Command
import dev.blazelight.p4oc.domain.model.CommandSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CommandMetadataTest {
    @Test
    fun `built in command names map to localized description resources`() {
        val expected = mapOf(
            "compact" to R.string.slash_command_compact_desc,
            "clear" to R.string.slash_command_clear_desc,
            "new" to R.string.slash_command_new_desc,
            "undo" to R.string.slash_command_undo_desc,
            "redo" to R.string.slash_command_redo_desc,
            "share" to R.string.slash_command_share_desc,
            "init" to R.string.slash_command_init_desc,
            "help" to R.string.slash_command_help_desc,
            "connect" to R.string.slash_command_connect_desc,
            "bug" to R.string.slash_command_bug_desc,
        )

        expected.forEach { (name, descriptionRes) ->
            assertEquals(descriptionRes, builtInCommandDescriptionRes(name))
        }
    }

    @Test
    fun `unknown command name has no built in description resource`() {
        assertNull(builtInCommandDescriptionRes("deploy"))
    }

    @Test
    fun `built in descriptions resolve without overwriting upstream metadata`() {
        val commands = listOf(
            Command(name = "compact", source = CommandSource.BuiltIn),
            Command(name = "custom-cmd", description = "My custom desc", source = CommandSource.Custom),
            Command(name = "mcp-tool", description = "From server", source = CommandSource.Mcp),
            Command(name = "skill-cmd", description = "From skill", source = CommandSource.Skill),
            Command(name = "future-built-in", description = "Keep fallback", source = CommandSource.BuiltIn),
        )

        val resolved = resolveBuiltInCommandDescriptions(
            commands = commands,
            builtInDescriptions = mapOf("compact" to "Compact from resources"),
        )

        assertEquals("Compact from resources", resolved[0].description)
        assertEquals("My custom desc", resolved[1].description)
        assertEquals("From server", resolved[2].description)
        assertEquals("From skill", resolved[3].description)
        assertEquals("Keep fallback", resolved[4].description)
    }
}
