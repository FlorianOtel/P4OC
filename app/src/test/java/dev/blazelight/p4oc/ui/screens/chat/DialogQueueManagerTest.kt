package dev.blazelight.p4oc.ui.screens.chat

import androidx.lifecycle.SavedStateHandle
import dev.blazelight.p4oc.core.log.AppLog
import dev.blazelight.p4oc.domain.model.Permission
import dev.blazelight.p4oc.domain.model.Question
import dev.blazelight.p4oc.domain.model.QuestionOption
import dev.blazelight.p4oc.domain.model.QuestionRequest
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DialogQueueManagerTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setUp() {
        mockkObject(AppLog)
        every { AppLog.d(any(), any<String>()) } returns Unit
        every { AppLog.d(any(), any<() -> String>()) } returns Unit
        every { AppLog.e(any(), any<String>()) } returns Unit
        every { AppLog.e(any(), any<String>(), any()) } returns Unit
    }

    @After
    fun tearDown() {
        unmockkObject(AppLog)
    }

    @Test
    fun enqueuePermission_addsPermissionByCallId() {
        val handle = SavedStateHandle()
        val manager = manager(handle)
        val permission = permission(id = "p1", callId = "call-1")

        manager.enqueuePermission(permission)

        assertEquals(permission, manager.pendingPermissionsByCallId.value["call-1"])
    }

    @Test
    fun enqueuePermission_withNullCallIdDoesNotAddAnything() {
        val handle = SavedStateHandle()
        val manager = manager(handle)
        val permission = permission(id = "p1", callId = null)

        manager.enqueuePermission(permission)

        assertTrue(manager.pendingPermissionsByCallId.value.isEmpty())
    }

    @Test
    fun clearPermission_removesMatchingPermissionById() {
        val handle = SavedStateHandle()
        val manager = manager(handle)
        val first = permission(id = "p1", callId = "call-1")
        val second = permission(id = "p2", callId = "call-2")

        manager.enqueuePermission(first)
        manager.enqueuePermission(second)
        manager.clearPermission(first.id)

        assertNull(manager.pendingPermissionsByCallId.value["call-1"])
        assertEquals(second, manager.pendingPermissionsByCallId.value["call-2"])
    }

    @Test
    fun clearPermissionByRequestId_removesMatchingPermissionById() {
        val handle = SavedStateHandle()
        val manager = manager(handle)
        val first = permission(id = "request-1", callId = "call-1")
        val second = permission(id = "request-2", callId = "call-2")

        manager.enqueuePermission(first)
        manager.enqueuePermission(second)
        manager.clearPermissionByRequestId("request-1")

        assertNull(manager.pendingPermissionsByCallId.value["call-1"])
        assertEquals(second, manager.pendingPermissionsByCallId.value["call-2"])
    }

    @Test
    fun corruptSavedStateHandleData_doesNotCrash() {
        val handle = SavedStateHandle(
            mapOf(
                KEY_PENDING_QUESTION to "{bad-json}",
                KEY_PENDING_QUESTIONS_QUEUE to "[bad-json"
            )
        )

        val manager = manager(handle)

        assertNull(manager.pendingQuestion.value)
        assertNull(handle.get<String>(KEY_PENDING_QUESTION))
        assertNull(handle.get<String>(KEY_PENDING_QUESTIONS_QUEUE))
    }

    private fun permission(id: String, callId: String?): Permission {
        return Permission(
            id = id,
            type = "read",
            patterns = listOf("*.kt"),
            sessionID = "session-1",
            messageID = "message-1",
            callID = callId,
            metadata = buildJsonObject { },
            always = emptyList()
        )
    }

    @Test
    fun enqueueQuestion_showsImmediately_whenNoCurrentQuestion() = runTest {
        val handle = SavedStateHandle()
        val manager = manager(handle)
        val question = questionRequest(id = "q1")

        manager.enqueueQuestion(question)
        advanceUntilIdle()

        assertEquals(question, manager.pendingQuestion.value)
        assertEquals(json.encodeToString(question), handle.get<String>(KEY_PENDING_QUESTION))
    }

    @Test
    fun clearQuestion_advancesToNextInQueue() = runTest {
        val handle = SavedStateHandle()
        val manager = manager(handle)
        val first = questionRequest(id = "q1")
        val second = questionRequest(id = "q2")

        manager.enqueueQuestion(first)
        manager.enqueueQuestion(second)
        assertEquals(first, manager.pendingQuestion.value)

        manager.clearQuestion()
        advanceUntilIdle()

        assertEquals(second, manager.pendingQuestion.value)
        assertEquals(json.encodeToString(second), handle.get<String>(KEY_PENDING_QUESTION))
    }

    @Test
    fun clearQuestion_clears_whenQueueEmpty() = runTest {
        val handle = SavedStateHandle()
        val manager = manager(handle)
        val only = questionRequest(id = "q1")

        manager.enqueueQuestion(only)
        manager.clearQuestion()
        advanceUntilIdle()

        assertNull(manager.pendingQuestion.value)
        assertNull(handle.get<String>(KEY_PENDING_QUESTION))
    }

    @Test
    fun clearQuestion_beforePersistenceCompletes_doesNotRestoreClearedQuestion() = runTest {
        val handle = SavedStateHandle()
        val manager = manager(handle)
        val only = questionRequest(id = "q1")

        manager.enqueueQuestion(only)
        manager.clearQuestion()
        advanceUntilIdle()

        assertNull(manager.pendingQuestion.value)
        assertNull(handle.get<String>(KEY_PENDING_QUESTION))
        assertNull(handle.get<String>(KEY_PENDING_QUESTIONS_QUEUE))
    }

    private fun questionRequest(id: String): QuestionRequest {
        return QuestionRequest(
            id = id,
            sessionID = "session-1",
            questions = listOf(
                Question(
                    header = "Header",
                    question = "Q?",
                    options = listOf(QuestionOption(label = "Yes", description = ""))
                )
            )
        )
    }

    private fun TestScope.manager(handle: SavedStateHandle): DialogQueueManager {
        val dispatcher = StandardTestDispatcher(testScheduler)
        return DialogQueueManager(handle, json, this, dispatcher)
    }

    private fun manager(handle: SavedStateHandle): DialogQueueManager {
        val dispatcher = StandardTestDispatcher()
        val scope = TestScope(dispatcher)
        return DialogQueueManager(handle, json, scope, dispatcher)
    }

    private companion object {
        const val KEY_PENDING_QUESTION = "pending_question"
        const val KEY_PENDING_QUESTIONS_QUEUE = "pending_questions_queue"
    }
}
