package dev.blazelight.p4oc.data.session

import dev.blazelight.p4oc.domain.model.OpenCodeEvent
import dev.blazelight.p4oc.domain.model.Permission
import dev.blazelight.p4oc.domain.model.QuestionRequest
import dev.blazelight.p4oc.domain.model.Session
import dev.blazelight.p4oc.domain.session.SessionId
import dev.blazelight.p4oc.fakes.FakeWorkspaceClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionOwnershipHydrationTest {
    @Test
    fun `hydrated child permission and question events update parent UI state`() = runTest {
        val client = FakeWorkspaceClient().apply {
            setSessions(
                FakeWorkspaceClient.sessionDto(id = "parent"),
                FakeWorkspaceClient.sessionDto(id = "child", parentID = "parent"),
            )
        }
        val repository = repository(client)
        repository.refresh()
        val permission = childPermission()
        val question = QuestionRequest(id = "q-child", sessionID = "child", questions = emptyList())

        repository.acceptEvent(OpenCodeEvent.PermissionRequested(permission))
        repository.acceptEvent(OpenCodeEvent.QuestionAsked(question))

        val parentState = repository.sessionUiState(SessionId("parent")).value
        assertEquals(permission, parentState.pendingPermissionsByCallId["call-child"])
        assertEquals(question, parentState.pendingQuestion)
        assertTrue(repository.sessionUiState(SessionId("child")).value.pendingPermissionsByCallId.isEmpty())
        assertNull(repository.sessionUiState(SessionId("child")).value.pendingQuestion)
    }

    @Test
    fun `refresh replaces stale hydrated child ownership`() = runTest {
        val client = FakeWorkspaceClient().apply {
            setSessions(
                FakeWorkspaceClient.sessionDto(id = "parent"),
                FakeWorkspaceClient.sessionDto(id = "child", parentID = "parent"),
            )
        }
        val repository = repository(client)
        repository.refresh()
        client.setSessions(FakeWorkspaceClient.sessionDto(id = "child"))
        repository.refresh()
        val permission = childPermission()

        repository.acceptEvent(OpenCodeEvent.PermissionRequested(permission))

        val childPermissions = repository.sessionUiState(SessionId("child")).value.pendingPermissionsByCallId
        assertEquals(permission, childPermissions["call-child"])
        assertTrue(repository.sessionUiState(SessionId("parent")).value.pendingPermissionsByCallId.isEmpty())
    }

    @Test
    fun `deleting parent clears child ownership`() = runTest {
        val repository = repository(FakeWorkspaceClient())
        repository.acceptEvent(OpenCodeEvent.SessionCreated(session("parent")))
        repository.acceptEvent(OpenCodeEvent.SessionCreated(session("child").copy(parentID = "parent")))
        repository.acceptEvent(OpenCodeEvent.SessionDeleted(session("parent")))
        val question = QuestionRequest(id = "q-child", sessionID = "child", questions = emptyList())

        repository.acceptEvent(OpenCodeEvent.QuestionAsked(question))

        assertEquals(question, repository.sessionUiState(SessionId("child")).value.pendingQuestion)
        assertNull(repository.sessionUiState(SessionId("parent")).value.pendingQuestion)
    }

    private fun TestScope.repository(client: FakeWorkspaceClient) = SessionRepositoryImpl(
        client,
        nowMs = { 0L },
        dispatcher = StandardTestDispatcher(testScheduler),
    )

    private fun childPermission() = Permission(
        id = "per_child",
        type = "bash",
        patterns = listOf("ls"),
        sessionID = "child",
        messageID = "msg-child",
        callID = "call-child",
        metadata = JsonObject(emptyMap()),
        always = emptyList(),
    )

    private fun session(id: String): Session = Session(
        id = id,
        projectID = "project-$id",
        directory = "/workspace",
        title = id,
        version = "1",
        createdAt = 1L,
        updatedAt = 1L,
    )
}
