package dev.blazelight.p4oc.ui.screens.home

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.blazelight.p4oc.domain.server.ServerRef
import dev.blazelight.p4oc.domain.server.WorkspaceKey
import dev.blazelight.p4oc.ui.theme.PocketCodeTheme
import dev.blazelight.p4oc.ui.workspace.WorkspaceLabelIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeWorkspaceEntryPointsTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val server = ServerRef.fromEndpointKey("http://desktop.test", "desktop")
    private val addRequests = mutableListOf<Pair<ServerRef?, String?>>()
    private val removed = mutableListOf<WorkspaceSummary>()

    private val workspaceActions = HomeWorkspaceActions(
        onAddWorkspace = { server, path -> addRequests += server to path },
        onRename = { _, _ -> },
        onResetName = {},
        onCopyPath = {},
        onRemove = { removed += it },
    )

    private fun summary(workspaces: List<WorkspaceSummary>) = HomeSummaryState(
        servers = emptyList(),
        workspaces = workspaces,
        openWork = emptyList(),
        sessions = emptyList(),
        isLoading = false,
    )

    private fun workspace(directory: String, isUserRoot: Boolean) = WorkspaceSummary(
        serverRef = server,
        workspaceKey = WorkspaceKey.Directory(directory),
        sessionCount = 0,
        openTabCount = 0,
        mostRecentAt = 1L,
        isUserRoot = isUserRoot,
        label = WorkspaceLabelIndex.Empty.directoryLabel(server.endpointKey, directory).copy(isUserRoot = isUserRoot),
    )

    private fun rowTag(directory: String) = "home_workspace_${server.endpointKey}_${WorkspaceKey.Directory(directory)}"

    private fun setHome(workspaces: List<WorkspaceSummary>) {
        composeRule.setContent {
            PocketCodeTheme {
                homeScreen(
                    summary = summary(workspaces),
                    actions = HomeActions(
                        onBrowseSessions = {},
                        onOpenFiles = {},
                        onOpenTerminal = {},
                        onChooseTarget = {},
                        workspaceActions = workspaceActions,
                    ),
                )
            }
        }
    }

    @Test
    fun sectionAddActionOpensAddWorkspaceWithoutServerContext() {
        setHome(listOf(workspace("/srv/app", isUserRoot = true)))

        composeRule.onNodeWithTag("home_workspaces_add").assertHasClickAction().performClick()

        assertEquals(listOf<Pair<ServerRef?, String?>>(null to null), addRequests)
    }

    @Test
    fun emptyWorkspacesStateOffersAddingAProjectFolder() {
        setHome(emptyList())

        composeRule.onNodeWithTag("home_workspaces_empty_add").assertHasClickAction().performClick()

        assertEquals(1, addRequests.size)
    }

    @Test
    fun discoveredWorkspaceLongPressOffersAddAsWorkspacePrefilled() {
        setHome(listOf(workspace("/srv/tool", isUserRoot = false)))

        composeRule.onNodeWithTag(rowTag("/srv/tool")).performTouchInput { longClick() }
        composeRule.onNodeWithTag("workspace_action_remove").assertDoesNotExist()
        composeRule.onNodeWithTag("workspace_action_add_root").performClick()

        assertEquals(listOf<Pair<ServerRef?, String?>>(server to "/srv/tool"), addRequests)
    }

    @Test
    fun detailOverflowAsksForConfirmationBeforeRemovingUserWorkspace() {
        setHome(listOf(workspace("/srv/app", isUserRoot = true)))

        composeRule.onNodeWithTag(rowTag("/srv/app")).performClick()
        composeRule.onNodeWithTag("home_workspace_detail_more").assertHasClickAction().performClick()
        composeRule.onNodeWithTag("workspace_action_add_root").assertDoesNotExist()
        composeRule.onNodeWithTag("workspace_action_remove").performClick()

        composeRule.onNodeWithTag("workspace_remove_dialog").assertExists()
        assertTrue(removed.isEmpty())
    }
}
