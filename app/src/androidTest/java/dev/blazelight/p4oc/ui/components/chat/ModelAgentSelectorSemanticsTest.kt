package dev.blazelight.p4oc.ui.components.chat

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.blazelight.p4oc.R
import dev.blazelight.p4oc.data.remote.dto.ModelDto
import dev.blazelight.p4oc.data.remote.dto.ModelInput
import dev.blazelight.p4oc.ui.theme.PocketCodeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ModelAgentSelectorSemanticsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun selectedProviderFilterExposesTabAndSelectionSemantics() {
        composeRule.setContent {
            PocketCodeTheme {
                ModelPickerDialog(
                    data = ModelPickerData(
                        availableModels = listOf("provider" to model()),
                        selectedModel = null,
                        favoriteModels = emptySet(),
                        recentModels = emptyList(),
                    ),
                    onModelSelected = {},
                    onToggleFavorite = {},
                    onDismiss = {},
                )
            }
        }

        composeRule.onNode(
            hasAnyDescendant(hasText("All")) and hasRole(Role.Tab),
            useUnmergedTree = true,
        ).assertIsSelected()
    }

    @Test
    fun selectedModelAndFavoriteActionExposeMeaningfulSemantics() {
        val modelInput = ModelInput(providerID = "provider", modelID = "model")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        composeRule.setContent {
            PocketCodeTheme {
                ModelPickerDialog(
                    data = ModelPickerData(
                        availableModels = listOf("provider" to model()),
                        selectedModel = modelInput,
                        favoriteModels = setOf(modelInput),
                        recentModels = emptyList(),
                    ),
                    onModelSelected = {},
                    onToggleFavorite = {},
                    onDismiss = {},
                )
            }
        }

        composeRule.onNode(
            hasAnyDescendant(hasText("Model One")) and hasRole(Role.RadioButton),
            useUnmergedTree = true,
        ).assertIsSelected()
        composeRule.onNodeWithContentDescription(
            context.getString(R.string.cd_remove_from_favorites),
            useUnmergedTree = true,
        ).assert(hasContentDescription(context.getString(R.string.cd_remove_from_favorites)))
    }

    private fun hasRole(role: Role): SemanticsMatcher =
        SemanticsMatcher.expectValue(SemanticsProperties.Role, role)

    private fun model() = ModelDto(
        id = "model",
        providerId = "provider",
        name = "Model One",
    )
}
