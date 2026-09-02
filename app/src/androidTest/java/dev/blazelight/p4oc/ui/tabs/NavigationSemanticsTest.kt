package dev.blazelight.p4oc.ui.tabs

import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.blazelight.p4oc.ui.theme.PocketCodeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationSemanticsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun activeTabExposesSelectedSemantics() {
        val home = TabInstance.home()
        val work = TabInstance(TabState(id = "work"))

        composeRule.setContent {
            PocketCodeTheme {
                TabBar(
                    tabs = listOf(home, work),
                    activeTabId = work.id,
                    tabTitles = mapOf(home.id to "Home", work.id to "Files"),
                    tabIcons = mapOf(home.id to getIconForTab(home), work.id to getIconForTab(work)),
                    tabConnectionStates = emptyMap(),
                    onTabClick = {},
                    onTabClose = {},
                    onAddClick = {},
                )
            }
        }

        composeRule.onNodeWithTag("tab_home").assertIsNotSelected()
        composeRule.onNodeWithTag("work_tab_work").assertIsSelected()
    }
}
