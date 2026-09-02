package dev.blazelight.p4oc.ui.screens.files

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.blazelight.p4oc.ui.theme.PocketCodeTheme
import dev.blazelight.p4oc.ui.theme.Sizing
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BreadcrumbNavigationSemanticsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun breadcrumbSegmentsExposeMinimumSemanticTargets() {
        composeRule.setContent {
            PocketCodeTheme {
                BreadcrumbNavigation(path = "src/main", onNavigateTo = {})
            }
        }

        listOf("files_breadcrumb_root", "files_breadcrumb_segment_0", "files_breadcrumb_segment_1")
            .forEach { tag ->
                composeRule.onNodeWithTag(tag)
                    .assertHeightIsAtLeast(Sizing.minTouchTarget)
                    .assertWidthIsAtLeast(Sizing.minTouchTarget)
            }
    }
}
