package dev.blazelight.p4oc.ui.components.toolwidgets

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.blazelight.p4oc.domain.model.Part
import dev.blazelight.p4oc.domain.model.ToolState
import dev.blazelight.p4oc.ui.theme.PocketCodeTheme
import kotlinx.serialization.json.buildJsonObject
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ToolGroupWidgetPermissionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun groupedPendingToolExposesEveryServerPermissionResponse() {
        val responses = mutableListOf<String>()
        composeRule.setContent {
            PocketCodeTheme {
                ToolGroupWidget(
                    tools = listOf(pendingTool()),
                    defaultState = ToolWidgetState.COMPACT,
                    pendingPermissionIdsByCallId = mapOf("call-1" to "permission-1"),
                    onToolApprove = { responses += "once:$it" },
                    onToolAlways = { responses += "always:$it" },
                    onToolDeny = { responses += "deny:$it" },
                )
            }
        }

        composeRule.onNodeWithTag("tool_permission_allow_once_permission-1").performClick()
        composeRule.onNodeWithTag("tool_permission_allow_always_permission-1").performClick()
        composeRule.onNodeWithTag("tool_permission_deny_permission-1").performClick()

        composeRule.runOnIdle {
            assertEquals(
                listOf("once:permission-1", "always:permission-1", "deny:permission-1"),
                responses,
            )
        }
    }

    private fun pendingTool() = Part.Tool(
        id = "part-1",
        sessionID = "session-1",
        messageID = "message-1",
        callID = "call-1",
        toolName = "bash",
        state = ToolState.Pending(input = buildJsonObject {}, rawInput = ""),
    )
}
