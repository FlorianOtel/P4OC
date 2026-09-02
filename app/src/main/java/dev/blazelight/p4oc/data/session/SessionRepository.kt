package dev.blazelight.p4oc.data.session

import dev.blazelight.p4oc.data.remote.dto.SendMessageRequest
import dev.blazelight.p4oc.domain.model.MessageWithParts
import dev.blazelight.p4oc.domain.model.OpenCodeEvent
import dev.blazelight.p4oc.domain.session.SessionId
import dev.blazelight.p4oc.domain.session.WorkspaceSession
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.StateFlow

interface SessionRepository {
    val state: StateFlow<RepoState>

    suspend fun refresh()

    suspend fun getSession(id: SessionId): WorkspaceSession?

    fun acceptEvent(event: OpenCodeEvent)

    fun messages(sessionId: SessionId): StateFlow<List<MessageWithParts>>

    fun sessionUiState(sessionId: SessionId): StateFlow<SessionUiState>

    /**
     * Keeps the cached message and UI state for [sessionId] alive until the returned
     * lease is closed. The final lease release evicts that per-session state.
     */
    fun acquireSession(sessionId: SessionId): AutoCloseable

    fun clearPermission(sessionId: SessionId, permissionId: String)

    fun clearPermissionByRequestId(sessionId: SessionId, requestId: String)

    fun clearQuestion(sessionId: SessionId, requestId: String? = null)

    /** Loads the newest [limit] messages and returns the number supplied by the server. */
    suspend fun loadMessages(sessionId: SessionId, limit: Int): Int

    /**
     * Reconciles the cached message state for [sessionId] against the server's authoritative REST
     * window, using the same active-lease, revision-safe recovery algorithm as reconnect recovery.
     * This is the single entry point for recovering missed terminal SSE updates after a successful
     * async send. It is a no-op when the session has no active lease or has been deleted.
     */
    suspend fun reconcileMessages(sessionId: SessionId)

    fun sendMessageAsync(sessionId: SessionId, request: SendMessageRequest): Deferred<Result<Unit>>

    fun abortSession(sessionId: SessionId): Deferred<Result<Boolean>>

    fun clearStreamingFlags(sessionId: SessionId)

    fun close()
}
