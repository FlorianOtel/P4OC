package dev.blazelight.p4oc.data.session

import dev.blazelight.p4oc.domain.model.MessageError
import dev.blazelight.p4oc.domain.model.Permission
import dev.blazelight.p4oc.domain.model.QuestionRequest
import dev.blazelight.p4oc.domain.model.Session
import dev.blazelight.p4oc.domain.model.SessionPresence
import dev.blazelight.p4oc.domain.model.SessionPresenceSignals
import dev.blazelight.p4oc.domain.model.SessionStatus
import dev.blazelight.p4oc.domain.model.Todo
import dev.blazelight.p4oc.domain.model.isAborted
import dev.blazelight.p4oc.domain.model.resolveSessionPresence

data class SessionUiState(
    val session: Session? = null,
    val status: SessionStatus? = null,
    val pendingPermissionsByCallId: Map<String, Permission> = emptyMap(),
    val pendingQuestion: QuestionRequest? = null,
    val queuedQuestions: List<QuestionRequest> = emptyList(),
    val todos: List<Todo> = emptyList(),
    val error: MessageError? = null,
    val responseCompletedToken: Long = 0L,
)

fun SessionUiState.presence(
    hasUnread: Boolean = false,
    isBackground: Boolean = false,
    hasStreamingText: Boolean = false,
    hasRunningTools: Boolean = false,
): SessionPresence {
    val backendMayBeActive = status == null || status is SessionStatus.Busy || status is SessionStatus.Retry

    return resolveSessionPresence(
        backendStatus = status,
        signals = SessionPresenceSignals(
            hasError = error?.isAborted() == false,
            pendingUserInput = pendingQuestion != null || pendingPermissionsByCallId.isNotEmpty(),
            isStreaming = backendMayBeActive && hasStreamingText,
            hasRunningTools = backendMayBeActive && hasRunningTools,
            hasUnread = hasUnread,
            isBackground = isBackground,
        ),
    )
}
