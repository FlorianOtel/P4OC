package dev.blazelight.p4oc.ui.screens.settings

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.blazelight.p4oc.ui.theme.PocketCodeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AgentsConfigScreenSemanticsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun toolLabelIsPresentedAsNonActionableText() {
        composeRule.setContent {
            PocketCodeTheme {
                AgentToolLabel(tool = "bash")
            }
        }

        composeRule.onNodeWithText("bash")
            .assert(!hasClickAction())
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
    }
}
