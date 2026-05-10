package dev.blazelight.p4oc.data.files.ofish

import dev.blazelight.p4oc.data.remote.dto.MessageWrapperDto
import dev.blazelight.p4oc.data.remote.dto.PermissionResponseRequest
import dev.blazelight.p4oc.data.remote.dto.SessionDto
import dev.blazelight.p4oc.data.remote.dto.ShellCommandRequest
import dev.blazelight.p4oc.data.remote.dto.TimeDto
import dev.blazelight.p4oc.domain.server.ServerRef
import dev.blazelight.p4oc.domain.workspace.Workspace
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfishSessionFactoryTest {
    @Test
    fun `withSession creates ephemeral session and deletes after success`() = runTest {
        val client = FakeOfishWorkspaceClient()
        val factory = OfishSessionFactory(client, nowMs = { 1000L }, shortId = { "abc" })

        val result = factory.withSession("Probe") { session ->
            assertEquals("created-1", session.id)
            assertEquals("__ofish_probe_1000_abc", session.title)
            "ok"
        }

        assertEquals("ok", result)
        assertEquals(listOf("__ofish_probe_1000_abc"), client.createdTitles)
        assertEquals(listOf("created-1"), client.deletedIds)
    }

    @Test
    fun `withSession deletes after block failure`() = runTest {
        val client = FakeOfishWorkspaceClient()
        val factory = OfishSessionFactory(client, nowMs = { 1000L }, shortId = { "abc" })

        val failure = runCatching {
            factory.withSession("probe") { error("boom") }
        }.exceptionOrNull()

        assertEquals("boom", failure?.message)
        assertEquals(listOf("created-1"), client.deletedIds)
    }

    @Test
    fun `withSession deletes after coroutine cancellation`() = runTest {
        val client = FakeOfishWorkspaceClient()
        val factory = OfishSessionFactory(client, nowMs = { 1000L }, shortId = { "abc" })
        val enteredBlock = CompletableDeferred<Unit>()

        val job = async {
            factory.withSession("probe") {
                enteredBlock.complete(Unit)
                throw CancellationException("cancelled")
            }
        }
        enteredBlock.await()

        val failure = runCatching { job.await() }.exceptionOrNull()

        assertTrue(failure is CancellationException)
        assertEquals(listOf("created-1"), client.deletedIds)
    }

    @Test
    fun `sweepStaleSessions deletes stale OFISH sessions only`() = runTest {
        val stale = sessionDto("stale", "__ofish_probe_1000_old")
        val fresh = sessionDto("fresh", "__ofish_probe_9500_new")
        val normal = sessionDto("normal", "normal")
        val client = FakeOfishWorkspaceClient(listSessionsResult = listOf(stale, fresh, normal))
        val factory = OfishSessionFactory(client, nowMs = { 10_000L }, shortId = { "abc" })

        val report = factory.sweepStaleSessions(maxAgeMillis = 5_000L)

        assertEquals(OfishSweepReport(scanned = 3, staleFound = 1, deleted = 1, failed = 0), report)
        assertEquals(listOf("stale"), client.deletedIds)
        assertEquals(listOf("/repo"), client.listWorkspaceDirectories)
    }

    @Test
    fun `sweepStaleSessions skips registered active OFISH session`() = runTest {
        val client = FakeOfishWorkspaceClient()
        val factory = OfishSessionFactory(client, nowMs = { 10_000L }, shortId = { "active" })
        val sessionCreated = CompletableDeferred<Unit>()
        val allowFinish = CompletableDeferred<Unit>()
        val activeJob = async {
            factory.withSession("probe") {
                sessionCreated.complete(Unit)
                allowFinish.await()
            }
        }
        sessionCreated.await()
        client.listSessionsResult = listOf(sessionDto("created-1", "__ofish_probe_10000_active"))

        val report = factory.sweepStaleSessions(maxAgeMillis = 0L)

        assertEquals(OfishSweepReport(scanned = 1, staleFound = 0, deleted = 0, failed = 0), report)
        assertEquals(emptyList<String>(), client.deletedIds)

        allowFinish.complete(Unit)
        activeJob.await()
        assertEquals(listOf("created-1"), client.deletedIds)
    }

    @Test
    fun `withSession sweeps stale sessions once per workspace interval`() = runTest {
        var now = 7 * 60 * 60 * 1000L
        val stale = sessionDto("stale", "__ofish_probe_1000_old")
        val client = FakeOfishWorkspaceClient(listSessionsResult = listOf(stale))
        val factory = OfishSessionFactory(client, nowMs = { now }, shortId = { "abc" })

        factory.withSession("probe") { }
        factory.withSession("probe") { }

        assertEquals(listOf("stale", "created-1", "created-2"), client.deletedIds)
        assertEquals(1, client.listWorkspaceDirectories.size)

        now += 31 * 60 * 1000L
        factory.withSession("probe") { }

        assertEquals(2, client.listWorkspaceDirectories.size)
    }

    private class FakeOfishWorkspaceClient(
        var listSessionsResult: List<SessionDto> = emptyList(),
    ) : OfishWorkspaceClient {
        override val workspace: Workspace = Workspace(
            server = ServerRef.fromEndpointKey("http://test.local"),
            directory = "/repo",
        )
        val createdTitles = mutableListOf<String>()
        val deletedIds = mutableListOf<String>()
        val listWorkspaceDirectories = mutableListOf<String?>()

        override suspend fun createSession(title: String): SessionDto {
            createdTitles += title
            return sessionDto("created-${createdTitles.size}", title)
        }

        override suspend fun deleteSession(id: String): Boolean {
            deletedIds += id
            return true
        }

        override suspend fun executeShellCommand(sessionId: String, request: ShellCommandRequest): MessageWrapperDto =
            error("not used")

        override suspend fun listSessionsCurrentWorkspace(limit: Int?): List<SessionDto> {
            listWorkspaceDirectories += workspace.directory
            return listSessionsResult
        }

        override suspend fun respondToPermission(id: String, request: PermissionResponseRequest): Boolean = true
    }

    private companion object {
        fun sessionDto(id: String, title: String): SessionDto = SessionDto(
            id = id,
            projectID = "project-$id",
            directory = "/repo",
            title = title,
            version = "1",
            time = TimeDto(created = 1L, updated = 1L),
        )
    }
}
