package dev.blazelight.p4oc.data.session

import dev.blazelight.p4oc.domain.model.MessageError
import dev.blazelight.p4oc.domain.model.Question
import dev.blazelight.p4oc.domain.model.QuestionOption
import dev.blazelight.p4oc.domain.model.QuestionRequest
import dev.blazelight.p4oc.domain.model.SessionPresence
import dev.blazelight.p4oc.domain.model.SessionStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionUiStateTest {
    @Test
    fun `presence resolver handles status and signal permutations`() {
        val cases = listOf(
            Case(
                name = "idle read no activity",
                status = SessionStatus.Idle,
                expected = SessionPresence.IDLE,
            ),
            Case(
                name = "idle unread",
                status = SessionStatus.Idle,
                hasUnread = true,
                expected = SessionPresence.UNREAD,
            ),
            Case(
                name = "idle background",
                status = SessionStatus.Idle,
                isBackground = true,
                expected = SessionPresence.BACKGROUND,
            ),
            Case(
                name = "idle unread beats background",
                status = SessionStatus.Idle,
                hasUnread = true,
                isBackground = true,
                expected = SessionPresence.UNREAD,
            ),
            Case(
                name = "idle ignores stale running tool and stays unread",
                status = SessionStatus.Idle,
                hasUnread = true,
                hasRunningTools = true,
                expected = SessionPresence.UNREAD,
            ),
            Case(
                name = "idle ignores stale streaming text and stays idle",
                status = SessionStatus.Idle,
                hasStreamingText = true,
                expected = SessionPresence.IDLE,
            ),
            Case(
                name = "idle ignores all stale activity and stays unread",
                status = SessionStatus.Idle,
                hasUnread = true,
                hasStreamingText = true,
                hasRunningTools = true,
                expected = SessionPresence.UNREAD,
            ),
            Case(
                name = "unknown read no activity",
                status = null,
                expected = SessionPresence.IDLE,
            ),
            Case(
                name = "unknown background",
                status = null,
                isBackground = true,
                expected = SessionPresence.BACKGROUND,
            ),
            Case(
                name = "unknown streaming text means busy",
                status = null,
                hasStreamingText = true,
                expected = SessionPresence.BUSY,
            ),
            Case(
                name = "unknown running tool means busy",
                status = null,
                hasRunningTools = true,
                expected = SessionPresence.BUSY,
            ),
            Case(
                name = "unknown busy beats unread",
                status = null,
                hasUnread = true,
                hasRunningTools = true,
                expected = SessionPresence.BUSY,
            ),
            Case(
                name = "busy status means busy",
                status = SessionStatus.Busy,
                expected = SessionPresence.BUSY,
            ),
            Case(
                name = "busy beats unread and background",
                status = SessionStatus.Busy,
                hasUnread = true,
                isBackground = true,
                expected = SessionPresence.BUSY,
            ),
            Case(
                name = "retry beats busy signals and unread",
                status = SessionStatus.Retry(attempt = 1, message = "retry", next = 2L),
                hasUnread = true,
                hasStreamingText = true,
                hasRunningTools = true,
                expected = SessionPresence.RETRYING,
            ),
            Case(
                name = "awaiting input beats busy",
                status = SessionStatus.Busy,
                hasPendingQuestion = true,
                expected = SessionPresence.AWAITING_INPUT,
            ),
            Case(
                name = "awaiting input beats unread",
                status = SessionStatus.Idle,
                hasPendingQuestion = true,
                hasUnread = true,
                expected = SessionPresence.AWAITING_INPUT,
            ),
            Case(
                name = "retry beats awaiting input",
                status = SessionStatus.Retry(attempt = 1, message = "retry", next = 2L),
                hasPendingQuestion = true,
                expected = SessionPresence.RETRYING,
            ),
            Case(
                name = "error beats retry and awaiting input",
                status = SessionStatus.Retry(attempt = 1, message = "retry", next = 2L),
                hasError = true,
                hasPendingQuestion = true,
                expected = SessionPresence.ERROR,
            ),
            Case(
                name = "aborted error does not beat idle",
                status = SessionStatus.Idle,
                hasAbortedError = true,
                expected = SessionPresence.IDLE,
            ),
            Case(
                name = "aborted error still allows unread",
                status = SessionStatus.Idle,
                hasAbortedError = true,
                hasUnread = true,
                expected = SessionPresence.UNREAD,
            ),
        )

        cases.forEach { case ->
            val state = SessionUiState(
                status = case.status,
                pendingQuestion = if (case.hasPendingQuestion) questionRequest() else null,
                error = when {
                    case.hasError -> MessageError(name = "ProviderError", message = "boom")
                    case.hasAbortedError -> MessageError(name = "MessageAbortedError", message = "Aborted")
                    else -> null
                },
            )

            val actual = state.presence(
                hasUnread = case.hasUnread,
                isBackground = case.isBackground,
                hasStreamingText = case.hasStreamingText,
                hasRunningTools = case.hasRunningTools,
            )

            assertEquals(case.name, case.expected, actual)
        }
    }

    private data class Case(
        val name: String,
        val status: SessionStatus?,
        val hasUnread: Boolean = false,
        val isBackground: Boolean = false,
        val hasStreamingText: Boolean = false,
        val hasRunningTools: Boolean = false,
        val hasPendingQuestion: Boolean = false,
        val hasError: Boolean = false,
        val hasAbortedError: Boolean = false,
        val expected: SessionPresence,
    )

    private fun questionRequest(): QuestionRequest = QuestionRequest(
        id = "question-1",
        sessionID = "session-1",
        questions = listOf(
            Question(
                header = "Choose",
                question = "Continue?",
                options = listOf(QuestionOption(label = "Yes", description = "Continue")),
            ),
        ),
    )
}
