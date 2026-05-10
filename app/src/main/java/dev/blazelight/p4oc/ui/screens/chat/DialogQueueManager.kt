package dev.blazelight.p4oc.ui.screens.chat

import androidx.lifecycle.SavedStateHandle
import dev.blazelight.p4oc.core.log.AppLog
import dev.blazelight.p4oc.domain.model.Permission
import dev.blazelight.p4oc.domain.model.QuestionRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Manages permission and question dialog queues with SavedStateHandle persistence.
 */
class DialogQueueManager(
    private val savedStateHandle: SavedStateHandle,
    private val json: Json,
    private val scope: CoroutineScope,
    private val persistenceDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val pendingQuestions = ConcurrentLinkedQueue<QuestionRequest>()
    private var pendingQuestionPersistenceVersion = 0L
    private var pendingQuestionsQueuePersistenceVersion = 0L

    private val _pendingQuestion = MutableStateFlow<QuestionRequest?>(null)
    val pendingQuestion: StateFlow<QuestionRequest?> = _pendingQuestion.asStateFlow()

    private val _pendingPermissionsByCallId = MutableStateFlow<Map<String, Permission>>(emptyMap())
    val pendingPermissionsByCallId: StateFlow<Map<String, Permission>> = _pendingPermissionsByCallId.asStateFlow()

    init {
        restorePendingDialogState()
    }

    /**
     * Restore pending question state from SavedStateHandle after process death.
     */
    private fun restorePendingDialogState() {
        // Restore pending question
        savedStateHandle.get<String>(KEY_PENDING_QUESTION)?.let { jsonString ->
            try {
                val question = json.decodeFromString<QuestionRequest>(jsonString)
                _pendingQuestion.value = question
                AppLog.d(TAG, "Restored pending question: ${question.id}")
            } catch (e: Exception) {
                AppLog.e(TAG, "Failed to restore pending question", e)
                savedStateHandle.remove<String>(KEY_PENDING_QUESTION)
            }
        }

        // Restore pending questions queue
        savedStateHandle.get<String>(KEY_PENDING_QUESTIONS_QUEUE)?.let { jsonString ->
            try {
                val questions = json.decodeFromString<List<QuestionRequest>>(jsonString)
                pendingQuestions.addAll(questions)
                AppLog.d(TAG, "Restored ${questions.size} queued questions")
            } catch (e: Exception) {
                AppLog.e(TAG, "Failed to restore pending questions queue", e)
                savedStateHandle.remove<String>(KEY_PENDING_QUESTIONS_QUEUE)
            }
        }
    }

    fun enqueuePermission(permission: Permission) {
        // Add to callID map for inline rendering
        permission.callID?.let { callId ->
            _pendingPermissionsByCallId.update { it + (callId to permission) }
        }
    }

    fun setPermissionsByCallId(permissions: Map<String, Permission>) {
        _pendingPermissionsByCallId.value = permissions
    }

    fun enqueueQuestion(request: QuestionRequest) {
        pendingQuestions.offer(request)
        showNextQuestion()
    }

    fun setPendingQuestion(request: QuestionRequest?) {
        _pendingQuestion.value = request
        if (request == null) {
            clearPersistedPendingQuestion()
        } else {
            persistPendingQuestion(request)
        }
    }

    fun clearPermission(permissionId: String) {
        // Clear from inline map
        _pendingPermissionsByCallId.update { map ->
            map.filterValues { it.id != permissionId }
        }
    }

    fun clearPermissionByRequestId(requestId: String) {
        // Clear from inline map
        _pendingPermissionsByCallId.update { map ->
            map.filterValues { it.id != requestId }
        }
    }

    fun clearQuestion() {
        _pendingQuestion.value = null
        clearPersistedPendingQuestion()
        showNextQuestion()
    }

    private fun showNextQuestion() {
        if (_pendingQuestion.value == null) {
            pendingQuestions.poll()?.let { question ->
                _pendingQuestion.value = question
                persistPendingQuestion(question)
            }
        }
        persistQuestionsQueue()
    }

    private fun persistPendingQuestion(request: QuestionRequest) {
        val version = ++pendingQuestionPersistenceVersion
        scope.launch {
            val encoded = try {
                withContext(persistenceDispatcher) {
                    json.encodeToString(request)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                AppLog.e(TAG, "Failed to persist pending question", e)
                if (version == pendingQuestionPersistenceVersion) {
                    savedStateHandle.remove<String>(KEY_PENDING_QUESTION)
                }
                return@launch
            }
            if (version == pendingQuestionPersistenceVersion && _pendingQuestion.value == request) {
                savedStateHandle[KEY_PENDING_QUESTION] = encoded
            }
        }
    }

    private fun clearPersistedPendingQuestion() {
        pendingQuestionPersistenceVersion++
        savedStateHandle.remove<String>(KEY_PENDING_QUESTION)
    }

    private fun persistQuestionsQueue() {
        val version = ++pendingQuestionsQueuePersistenceVersion
        scope.launch {
            val encoded = try {
                withContext(persistenceDispatcher) {
                    pendingQuestions.toList().takeIf { it.isNotEmpty() }?.let { queueList ->
                        json.encodeToString(queueList)
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                AppLog.e(TAG, "Failed to persist pending questions queue", e)
                if (version == pendingQuestionsQueuePersistenceVersion) {
                    savedStateHandle.remove<String>(KEY_PENDING_QUESTIONS_QUEUE)
                }
                return@launch
            }
            if (version == pendingQuestionsQueuePersistenceVersion) {
                if (encoded == null) {
                    savedStateHandle.remove<String>(KEY_PENDING_QUESTIONS_QUEUE)
                } else {
                    savedStateHandle[KEY_PENDING_QUESTIONS_QUEUE] = encoded
                }
            }
        }
    }

    private companion object {
        const val TAG = "DialogQueueManager"
        const val KEY_PENDING_QUESTION = "pending_question"
        const val KEY_PENDING_QUESTIONS_QUEUE = "pending_questions_queue"
    }
}
