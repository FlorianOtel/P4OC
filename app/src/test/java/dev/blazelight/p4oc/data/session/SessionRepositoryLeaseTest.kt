package dev.blazelight.p4oc.data.session

import dev.blazelight.p4oc.domain.model.Message
import dev.blazelight.p4oc.domain.model.OpenCodeEvent
import dev.blazelight.p4oc.domain.model.SessionStatus
import dev.blazelight.p4oc.domain.model.TokenUsage
import dev.blazelight.p4oc.domain.session.SessionId
import dev.blazelight.p4oc.fakes.FakeWorkspaceClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionRepositoryLeaseTest {
    @Test
    fun `session cache remains until final consumer releases then reopening starts empty`() = runTest {
        val repository = repository(StandardTestDispatcher(testScheduler))
        val sessionId = SessionId(SESSION_ID)
        val firstLease = repository.acquireSession(sessionId)
        val secondLease = repository.acquireSession(sessionId)
        val originalMessages = repository.messages(sessionId)
        val originalUiState = repository.sessionUiState(sessionId)

        repository.acceptEvent(OpenCodeEvent.MessageUpdated(assistantMessage("m1")))
        repository.acceptEvent(OpenCodeEvent.SessionStatusChanged(SESSION_ID, SessionStatus.Busy))
        assertEquals(1, originalMessages.value.size)
        assertEquals(SessionStatus.Busy, originalUiState.value.status)

        firstLease.close()
        assertEquals(1, repository.messages(sessionId).value.size)
        assertEquals(SessionStatus.Busy, repository.sessionUiState(sessionId).value.status)

        secondLease.close()
        assertTrue(originalMessages.value.isEmpty())
        assertEquals(SessionUiState(), originalUiState.value)

        val reopenedLease = repository.acquireSession(sessionId)
        val reopenedMessages = repository.messages(sessionId)
        assertTrue(reopenedMessages.value.isEmpty())
        assertFalse(originalMessages === reopenedMessages)

        repository.acceptEvent(OpenCodeEvent.MessageUpdated(assistantMessage("m2")))
        assertEquals(listOf("m2"), reopenedMessages.value.map { it.message.id })
        reopenedLease.close()
    }

    @Test
    fun `closing a session lease twice releases only its own reference`() = runTest {
        val repository = repository(StandardTestDispatcher(testScheduler))
        val sessionId = SessionId(SESSION_ID)
        val firstLease = repository.acquireSession(sessionId)
        val secondLease = repository.acquireSession(sessionId)
        repository.acceptEvent(OpenCodeEvent.MessageUpdated(assistantMessage("m1")))

        firstLease.close()
        firstLease.close()

        assertEquals(1, repository.messages(sessionId).value.size)
        secondLease.close()
        assertTrue(repository.messages(sessionId).value.isEmpty())
    }

    private fun repository(dispatcher: TestDispatcher) = SessionRepositoryImpl(
        FakeWorkspaceClient(),
        dispatcher = dispatcher,
    )

    private fun assistantMessage(id: String) = Message.Assistant(
        id = id,
        sessionID = SESSION_ID,
        createdAt = 1L,
        parentID = "",
        providerID = "provider",
        modelID = "model",
        mode = "chat",
        agent = "assistant",
        cost = 0.0,
        tokens = TokenUsage(input = 0, output = 0),
    )

    private companion object {
        const val SESSION_ID = "s1"
    }
}
