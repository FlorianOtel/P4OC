package dev.blazelight.p4oc.ui.components

import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.blazelight.p4oc.domain.model.Permission
import dev.blazelight.p4oc.ui.components.chat.InlinePermissionPrompt
import dev.blazelight.p4oc.ui.theme.PocketCodeTheme
import dev.blazelight.p4oc.ui.theme.Sizing
import kotlinx.serialization.json.buildJsonObject
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TouchTargetSemanticsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun sharedButtonsExposeMinimumTouchTargets() {
        composeRule.setContent {
            PocketCodeTheme {
                TuiButton(onClick = {}, modifier = Modifier.testTag("button")) { Text("Button") }
                TuiOutlinedButton(onClick = {}, modifier = Modifier.testTag("outlined")) { Text("Outlined") }
                TuiTextButton(onClick = {}, modifier = Modifier.testTag("text")) { Text("Text") }
                TuiIconButton(onClick = {}, modifier = Modifier.testTag("icon")) { Text("+") }
            }
        }

        listOf("button", "outlined", "text", "icon").forEach { tag ->
            composeRule.onNodeWithTag(tag)
                .assertHeightIsAtLeast(Sizing.minTouchTarget)
                .assertWidthIsAtLeast(Sizing.minTouchTarget)
        }
    }

    @Test
    fun inlinePermissionActionsExposeMinimumTouchTargets() {
        composeRule.setContent {
            PocketCodeTheme {
                InlinePermissionPrompt(
                    permission = Permission(
                        id = "permission-1",
                        type = "bash",
                        patterns = listOf("echo"),
                        sessionID = "session-1",
                        messageID = "message-1",
                        metadata = buildJsonObject {},
                        always = emptyList(),
                    ),
                    onAllow = {},
                    onAlways = {},
                    onReject = {},
                )
            }
        }

        listOf(
            "permission_deny_permission-1",
            "permission_always_allow_permission-1",
            "permission_allow_once_permission-1",
        ).forEach { tag ->
            composeRule.onNodeWithTag(tag)
                .assertHeightIsAtLeast(Sizing.minTouchTarget)
                .assertWidthIsAtLeast(Sizing.minTouchTarget)
        }
    }

    @Test
    fun inlinePermissionActionsRouteTheExactRequest() {
        val routed = mutableListOf<String>()
        composeRule.setContent {
            PocketCodeTheme {
                InlinePermissionPrompt(
                    permission = Permission(
                        id = "permission-exact",
                        type = "bash",
                        patterns = listOf("echo"),
                        sessionID = "session-1",
                        messageID = "",
                        metadata = buildJsonObject {},
                        always = emptyList(),
                    ),
                    onAllow = { routed += "permission-exact:once" },
                    onAlways = { routed += "permission-exact:always" },
                    onReject = { routed += "permission-exact:reject" },
                )
            }
        }

        composeRule.onNodeWithTag("permission_allow_once_permission-exact").performClick()
        composeRule.onNodeWithTag("permission_always_allow_permission-exact").performClick()
        composeRule.onNodeWithTag("permission_deny_permission-exact").performClick()

        composeRule.runOnIdle {
            assertEquals(
                listOf(
                    "permission-exact:once",
                    "permission-exact:always",
                    "permission-exact:reject",
                ),
                routed,
            )
        }
    }
}
