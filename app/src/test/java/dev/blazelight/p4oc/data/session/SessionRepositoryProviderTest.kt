package dev.blazelight.p4oc.data.session

import dev.blazelight.p4oc.core.network.ConnectionManager
import dev.blazelight.p4oc.core.network.OpenCodeApi
import dev.blazelight.p4oc.data.remote.mapper.MessageMapper
import dev.blazelight.p4oc.data.server.ActiveServerApiProvider
import dev.blazelight.p4oc.domain.model.OpenCodeEvent
import dev.blazelight.p4oc.domain.model.Session
import dev.blazelight.p4oc.domain.server.ScopedEvent
import dev.blazelight.p4oc.domain.server.ServerGeneration
import dev.blazelight.p4oc.domain.server.ServerRef
import dev.blazelight.p4oc.domain.workspace.Workspace
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionRepositoryProviderTest {
    private val server = ServerRef.fromEndpointKey("http://fake.test")
    private val workspace = Workspace(server = server, directory = "/repo")
    private val generation = ServerGeneration(1)

    @Test
    fun `acquire reuses repository for same workspace generation`() {
        val provider = provider()

        val first = provider.acquire(workspace, generation)
        val second = provider.acquire(workspace, generation)

        assertSame(first.repository, second.repository)
        assertSame(first.workspaceClient, second.workspaceClient)
    }

    @Test
    fun `repository remains retained until final matching release`() {
        val provider = provider()
        val first = provider.acquire(workspace, generation)
        provider.acquire(workspace, generation)

        provider.release(workspace, generation)
        val afterSingleRelease = provider.acquire(workspace, generation)

        assertSame(first.repository, afterSingleRelease.repository)
    }

    @Test
    fun `final release closes repository and next acquire creates replacement`() {
        val provider = provider()
        val first = provider.acquire(workspace, generation)

        provider.release(workspace, generation)
        val second = provider.acquire(workspace, generation)

        assertNotSame(first.repository, second.repository)
        assertNotSame(first.workspaceClient, second.workspaceClient)
    }

    @Test
    fun `different generation gets separate repository`() {
        val provider = provider()

        val first = provider.acquire(workspace, generation)
        val second = provider.acquire(workspace, ServerGeneration(2))

        assertNotSame(first.repository, second.repository)
        assertNotSame(first.workspaceClient, second.workspaceClient)
    }

    @Test
    fun `provider routes scoped events to shared repository`() = runTest {
        val event = sessionCreatedEvent("s1")
        val provider = provider(
            scopedEvents = flowOf(
                ScopedEvent(
                    serverRef = server,
                    generation = generation,
                    workspaceKey = workspace.key,
                    event = event,
                ),
            ),
            dispatcher = StandardTestDispatcher(testScheduler),
        )

        val lease = provider.acquire(workspace, generation)
        testScheduler.advanceUntilIdle()

        assertTrue(lease.repository.state.value is RepoState.Hydrating)
    }

    private fun provider(
        scopedEvents: Flow<ScopedEvent> = emptyFlow(),
        dispatcher: CoroutineDispatcher = StandardTestDispatcher(),
    ): SessionRepositoryProvider = SessionRepositoryProvider(
        activeServerApiProvider = ActiveServerApiProvider { _, _ -> mockk<OpenCodeApi>(relaxed = true) },
        messageMapper = MessageMapper(Json { ignoreUnknownKeys = true }),
        connectionManager = mockk<ConnectionManager> {
            every { this@mockk.scopedEvents } returns scopedEvents
        },
        dispatcher = dispatcher,
    )

    private fun sessionCreatedEvent(id: String): OpenCodeEvent.SessionCreated = OpenCodeEvent.SessionCreated(
        session = Session(
            id = id,
            projectID = "project-$id",
            directory = workspace.directory.orEmpty(),
            title = id,
            version = "1",
            createdAt = 1L,
            updatedAt = 1L,
        ),
    )
}
