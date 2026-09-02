package dev.blazelight.p4oc.ui.components.toolwidgets

import dev.blazelight.p4oc.domain.model.Part
import dev.blazelight.p4oc.domain.model.ToolState
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Test

class ToolCallWidgetTest {

    @Test
    fun shellFamilyWithoutCommand_usesActualToolName() {
        assertEquals("shell", getToolCompactDescription(tool("shell")))
        assertEquals("Execute", getToolCompactDescription(tool("Execute")))
        assertEquals(
            "bash",
            getToolCompactDescription(
                tool("bash", command = "   "),
            ),
        )
    }

    @Test
    fun shellFamilyWithCommand_keepsCompactBashLabel() {
        assertEquals(
            "bash echo hello",
            getToolCompactDescription(tool("shell", command = " echo hello ")),
        )
    }

    @Test
    fun applyPatch_validOneFilePatch_usesPatchSummaryNotToolName() {
        assertEquals(
            "Patch: 1 file +1",
            getToolCompactDescription(
                tool(
                    "apply_patch",
                    patchText = "*** Begin Patch\n*** Add File: a.kt\n+x\n*** End Patch",
                ),
            ),
        )
    }

    private fun tool(name: String, command: String? = null, patchText: String? = null): Part.Tool {
        val input = buildJsonObject {
            command?.let { put("command", it) }
            patchText?.let { put("patchText", it) }
        }
        val state = if (patchText != null) {
            ToolState.Completed(
                input = input,
                output = "Applied patch",
                title = "Applied patch",
                startedAt = 1,
                endedAt = 2,
            )
        } else {
            ToolState.Pending(input = input, rawInput = "")
        }
        return Part.Tool(
            id = "part",
            sessionID = "session",
            messageID = "message",
            callID = "call",
            toolName = name,
            state = state,
        )
    }
}
