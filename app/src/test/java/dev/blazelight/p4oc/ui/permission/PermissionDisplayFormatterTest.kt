package dev.blazelight.p4oc.ui.permission

import android.content.Context
import dev.blazelight.p4oc.R
import dev.blazelight.p4oc.domain.model.PermissionKind
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PermissionDisplayFormatterTest {

    private val context = mockk<Context> {
        every {
            getString(R.string.permission_title_with_pattern, any<String>(), any<String>())
        } answers {
            val formatArgs = arg<Array<Any>>(1)
            "${formatArgs[0]}: ${formatArgs[1]}"
        }
    }

    @Test
    fun `known permission kinds resolve to localized action resources`() {
        val cases = listOf(
            PermissionKind.Bash to R.string.permission_action_bash,
            PermissionKind.Edit to R.string.permission_action_edit,
            PermissionKind.Patch to R.string.permission_action_patch,
            PermissionKind.WebFetch to R.string.permission_action_webfetch,
            PermissionKind.Task to R.string.permission_action_task,
            PermissionKind.Skill to R.string.permission_action_skill,
            PermissionKind.ExternalDirectory to R.string.permission_action_external_directory,
            PermissionKind.DoomLoop to R.string.permission_action_doom_loop,
        )

        cases.forEach { (kind, expectedResource) ->
            assertEquals("resource for $kind", expectedResource, PermissionDisplayFormatter.actionStringRes(kind))
        }
        assertNull(PermissionDisplayFormatter.actionStringRes(PermissionKind.Unknown))
    }

    @Test
    fun `unknown permission action preserves raw type except first-character capitalization`() {
        assertEquals("Custom_tool", PermissionDisplayFormatter.unknownAction("custom_tool"))
        assertEquals("MCPTool", PermissionDisplayFormatter.unknownAction("MCPTool"))
    }

    @Test
    fun `title appends first non-empty pattern to already localized action`() {
        assertEquals(
            "Execute command: rm -rf /tmp/test",
            PermissionDisplayFormatter.title(context, "Execute command", listOf("rm -rf /tmp/test", "ignored"))
        )
    }

    @Test
    fun `title returns localized action when first pattern is absent or blank`() {
        assertEquals("Execute command", PermissionDisplayFormatter.title(context, "Execute command", emptyList()))
        assertEquals("Execute command", PermissionDisplayFormatter.title(context, "Execute command", listOf("")))
    }
}
