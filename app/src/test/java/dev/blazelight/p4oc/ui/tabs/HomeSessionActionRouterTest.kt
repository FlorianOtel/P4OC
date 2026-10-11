package dev.blazelight.p4oc.ui.tabs

import dev.blazelight.p4oc.core.network.ConnectionState
import dev.blazelight.p4oc.data.session.SessionRepositoryImpl
import dev.blazelight.p4oc.domain.model.SessionPresence
import dev.blazelight.p4oc.domain.server.ServerGeneration
import dev.blazelight.p4oc.domain.server.ServerRef
import dev.blazelight.p4oc.domain.server.WorkspaceKey
import dev.blazelight.p4oc.domain.session.SessionId
import dev.blazelight.p4oc.domain.workspace.Workspace
import dev.blazelight.p4oc.fakes.FakeWorkspaceClient
import dev.blazelight.p4oc.ui.screens.home.SessionPreview
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeSessionActionRouterTest {
    private val server = ServerRef.fromEndpointKey("http://home.test", "Laptop")
    private val generation = ServerGeneration(7)

    @Test
    fun `directory session acts on its own workspace repository and refreshes the global listing`() = runTest {
        val globalClient = FakeWorkspaceClient(Workspace(server, directory = null)).apply {
            projects = listOf(FakeWorkspaceClient.projectDto("b", "/repo/b"))
            listSessionsResult = listOf(FakeWorkspaceClient.sessionDto(id = "s1", directory = "/repo/b"))
        }
        val globalListing = repository(globalClient)
        globalListing.refresh()
        assertTrue(globalListing.state.value.snapshot.sessions.containsKey("s1"))
        val listingFetchesBefore = globalClient.listProjectsCalls

        val ownClient = FakeWorkspaceClient(Workspace(server, directory = "/repo/b"))
        val ownRepository = repository(ownClient)
        val acquired = mutableListOf<Pair<Workspace, ServerGeneration>>()
        val released = mutableListOf<Pair<Workspace, ServerGeneration>>()
        val router = router(
            acquire = { workspace, gen ->
                acquired += workspace to gen
                ownRepository
            },
            release = { workspace, gen -> released += workspace to gen },
            listings = listOf(globalListing),
        )

        val result = router.run(preview(WorkspaceKey.Directory("/repo/b")), "failed") { repository ->
            repository.deleteSession(SessionId("s1"))
        }
        advanceUntilIdle()

        assertTrue(result is HomeSessionActionResult.Success)
        val expected = Workspace(server, directory = "/repo/b") to generation
        assertEquals(listOf(expected), acquired)
        assertEquals(listOf(expected), released)
        assertEquals(1, ownClient.deleteSessionCalls)
        assertEquals(0, globalClient.deleteSessionCalls)
        assertEquals(listingFetchesBefore + 1, globalClient.listProjectsCalls)
    }

    @Test
    fun `disconnected server fails with a readable message without touching any repository`() = runTest {
        var acquireCalls = 0
        val router = router(
            connection = ConnectionState.Disconnected,
            acquire = { _, _ ->
                acquireCalls += 1
                error("must not acquire")
            },
        )

        val result = router.run(preview(WorkspaceKey.Directory("/repo/b")), "failed") { Unit }

        assertEquals(
            HomeSessionActionResult.Failure("Laptop is not connected. Reconnect it to manage this session."),
            result,
        )
        assertEquals(0, acquireCalls)
    }

    @Test
    fun `failed action reports the action message and still releases the lease`() = runTest {
        val ownClient = FakeWorkspaceClient(Workspace(server, directory = "/repo/b")).apply {
            deleteSessionFailure = IllegalStateException("{\"error\":\"raw protocol\"}")
            projects = emptyList()
        }
        val ownRepository = repository(ownClient)
        var releaseCalls = 0
        val router = router(
            acquire = { _, _ -> ownRepository },
            release = { _, _ -> releaseCalls += 1 },
        )

        val failureMessage = "Could not delete the session. Try again."
        val result = router.run(preview(WorkspaceKey.Directory("/repo/b")), failureMessage) {
            it.deleteSession(SessionId("s1"))
        }
        assertEquals(HomeSessionActionResult.Failure("Could not delete the session. Try again."), result)
        assertEquals(1, releaseCalls)
    }

    @Test
    fun `session without a project directory routes to its own no-project workspace`() {
        val route = resolveHomeSessionRoute(preview(WorkspaceKey.Global), ConnectionState.Connected, generation)

        assertEquals(HomeSessionRoute.Ready(Workspace(server, directory = null), generation), route)
    }

    private fun TestScope.repository(client: FakeWorkspaceClient) = SessionRepositoryImpl(
        client,
        nowMs = { testScheduler.currentTime },
        dispatcher = StandardTestDispatcher(testScheduler),
    )

    private fun TestScope.router(
        connection: ConnectionState = ConnectionState.Connected,
        acquire: (Workspace, ServerGeneration) -> SessionRepositoryImpl = { _, _ -> error("unexpected acquire") },
        release: (Workspace, ServerGeneration) -> Unit = { _, _ -> },
        listings: List<SessionRepositoryImpl> = emptyList(),
    ) = HomeSessionActionRouter(
        connectionState = { connection },
        generation = { generation },
        acquireRepository = acquire,
        releaseRepository = release,
        homeRepositories = { listings },
        refreshScope = this,
    )

    private fun preview(workspaceKey: WorkspaceKey) = SessionPreview(
        sessionId = SessionId("s1"),
        serverRef = server,
        workspaceKey = workspaceKey,
        title = "Session",
        updatedAt = 1L,
        status = SessionPresence.IDLE,
        directory = (workspaceKey as? WorkspaceKey.Directory)?.value.orEmpty(),
        childCount = 0,
        additions = 0,
        deletions = 0,
        isShared = false,
    )
}
