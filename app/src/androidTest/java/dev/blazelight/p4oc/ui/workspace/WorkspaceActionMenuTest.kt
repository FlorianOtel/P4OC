package dev.blazelight.p4oc.ui.workspace

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.blazelight.p4oc.domain.server.ServerRef
import dev.blazelight.p4oc.ui.theme.PocketCodeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkspaceActionMenuTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun longPressOpensWorkspaceMenu() {
        setWorkspaceContent()

        openMenu()

        composeRule.onNodeWithTag("workspace_actions_menu").assertExists()
        composeRule.onNodeWithTag("workspace_action_rename").assertExists()
        composeRule.onNodeWithTag("workspace_action_copy_path").assertExists()
    }

    @Test
    fun resetIsVisibleForCustomNames() {
        setWorkspaceContent(custom = "My project")
        openMenu()
        composeRule.onNodeWithTag("workspace_action_reset").assertExists()
    }

    @Test
    fun resetIsHiddenForAutomaticNames() {
        setWorkspaceContent()
        openMenu()
        composeRule.onNodeWithTag("workspace_action_reset").assertDoesNotExist()
    }

    @Test
    fun addAsWorkspaceIsHiddenForUndiscoveredDirectories() {
        setWorkspaceContent()
        openMenu()
        composeRule.onNodeWithTag("workspace_action_add_root").assertDoesNotExist()
    }

    @Test
    fun addAsWorkspaceIsVisibleForDiscoveredDirectories() {
        setWorkspaceContent(canAddAsRoot = true)
        openMenu()
        composeRule.onNodeWithTag("workspace_action_add_root").assertExists()
    }

    @Test
    fun addAsWorkspaceIsHiddenForRoots() {
        setWorkspaceContent(isUserRoot = true, canAddAsRoot = true)
        openMenu()
        composeRule.onNodeWithTag("workspace_action_add_root").assertDoesNotExist()
    }

    @Test
    fun removeIsHiddenForDiscoveredDirectories() {
        setWorkspaceContent()
        openMenu()
        composeRule.onNodeWithTag("workspace_action_remove").assertDoesNotExist()
    }

    @Test
    fun removeIsVisibleForUserRoots() {
        setWorkspaceContent(isUserRoot = true)
        openMenu()
        composeRule.onNodeWithTag("workspace_action_remove").assertExists()
    }

    @Test
    fun savingBlankRenameCallsRenameWithNull() {
        val savedNames = mutableListOf<String?>()
        setWorkspaceContent(custom = "My project") { savedNames.add(it) }
        openMenu()
        composeRule.onNodeWithTag("workspace_action_rename").performClick()
        composeRule.onNodeWithTag("workspace_rename_field").performTextReplacement("")
        composeRule.onNodeWithTag("workspace_rename_save").performClick()

        assertEquals(listOf<String?>(null), savedNames)
    }

    @Test
    fun discoveredRowExposesVisibleAccessibilityActions() {
        setWorkspaceContent(canAddAsRoot = true)

        val actions = composeRule.onNodeWithTag("workspace_row")
            .fetchSemanticsNode()
            .config[SemanticsActions.CustomActions]
            .map { it.label }

        assertEquals(listOf("Rename…", "Copy path", "Add as workspace"), actions)
    }

    @Test
    fun rootRowExposesVisibleAccessibilityActions() {
        setWorkspaceContent(custom = "Alias", isUserRoot = true, canAddAsRoot = true)

        val actions = composeRule.onNodeWithTag("workspace_row")
            .fetchSemanticsNode()
            .config[SemanticsActions.CustomActions]
            .map { it.label }

        assertEquals(listOf("Rename…", "Reset to automatic name", "Copy path", "Remove workspace"), actions)
    }

    private fun setWorkspaceContent(
        custom: String? = null,
        isUserRoot: Boolean = false,
        canAddAsRoot: Boolean = false,
        onRename: (String?) -> Unit = {},
    ) {
        composeRule.setContent {
            workspaceActionMenuUnderTest(custom, isUserRoot, canAddAsRoot, onRename)
        }
    }

    private fun openMenu() {
        composeRule.onNodeWithTag("workspace_row").performTouchInput { longClick() }
    }

    @Composable
    private fun workspaceActionMenuUnderTest(
        custom: String?,
        isUserRoot: Boolean,
        canAddAsRoot: Boolean,
        onRename: (String?) -> Unit,
    ) {
        PocketCodeTheme {
            val state = rememberWorkspaceActionsState()
            val server = ServerRef.fromEndpoint("http://workspace.test:4096", "Test server")
            val subject = WorkspaceActionSubject(
                server = server,
                directory = "/srv/project",
                label = WorkspaceLabel(
                    primary = custom ?: "project",
                    generated = "project",
                    custom = custom,
                    path = "/srv/project",
                    isUserRoot = isUserRoot,
                ),
                canAddAsRoot = canAddAsRoot,
            )
            val handlers = WorkspaceActionHandlers(
                onRename = onRename,
                onResetName = {},
                onCopyPath = {},
                onAddAsWorkspace = {},
                onRemove = {},
            )
            Box(Modifier.fillMaxSize()) {
                Text(
                    text = "Project",
                    modifier = Modifier
                        .testTag("workspace_row")
                        .workspaceActionGestures(state, subject, handlers) {},
                )
                workspaceActionMenu(state, subject, handlers)
            }
        }
    }
}
