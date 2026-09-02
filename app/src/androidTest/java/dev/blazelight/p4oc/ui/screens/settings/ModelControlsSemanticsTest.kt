package dev.blazelight.p4oc.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.blazelight.p4oc.R
import dev.blazelight.p4oc.ui.theme.PocketCodeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ModelControlsSemanticsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun selectedModelAndFavoriteExposeTruthfulSemantics() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        composeRule.setContent {
            PocketCodeTheme {
                Column(Modifier.selectableGroup()) {
                    ModelCard(
                        model = model(isFavorite = true),
                        isSelected = true,
                        onSelect = {},
                        onToggleFavorite = {},
                    )
                }
            }
        }

        composeRule.onNode(
            hasAnyDescendant(hasText("Model One")) and hasRole(Role.RadioButton),
            useUnmergedTree = true,
        )
            .assertIsSelected()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    context.getString(R.string.models_current_model),
                )
            )

        composeRule.onNodeWithContentDescription(
            context.getString(R.string.cd_remove_from_favorites),
            useUnmergedTree = true,
        )
            .assert(hasContentDescription(context.getString(R.string.cd_remove_from_favorites)))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.On))
    }

    @Test
    fun capabilityBadgesHaveNoFakeClickActions() {
        composeRule.setContent {
            PocketCodeTheme {
                Column(Modifier.selectableGroup()) {
                    ModelCard(
                        model = model(isFavorite = false),
                        isSelected = false,
                        onSelect = {},
                        onToggleFavorite = {},
                    )
                }
            }
        }

        composeRule.onNodeWithContentDescription(
            InstrumentationRegistry.getInstrumentation().targetContext
                .getString(R.string.cd_add_to_favorites),
            useUnmergedTree = true,
        ).assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.Off))

        composeRule.onNode(hasText("Tools"), useUnmergedTree = true).assertHasNoClickAction()
        composeRule.onNode(hasText("Reasoning"), useUnmergedTree = true).assertHasNoClickAction()
    }

    private fun hasRole(role: Role): SemanticsMatcher =
        SemanticsMatcher.expectValue(SemanticsProperties.Role, role)

    private fun model(isFavorite: Boolean) = ModelInfo(
        id = "model",
        name = "Model One",
        providerId = "provider",
        supportsTools = true,
        supportsReasoning = true,
        isFavorite = isFavorite,
    )
}
