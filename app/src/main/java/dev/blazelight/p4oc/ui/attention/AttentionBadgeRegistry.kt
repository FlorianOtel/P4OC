package dev.blazelight.p4oc.ui.attention

import dev.blazelight.p4oc.domain.server.ServerRef
import dev.blazelight.p4oc.domain.server.WorkspaceKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class AttentionKey(
    val serverRef: ServerRef,
    val workspaceKey: WorkspaceKey? = null,
    val tabId: String? = null,
)

enum class AttentionSeverity { Info, Warning, Error }

data class AttentionSignal(
    val key: AttentionKey,
    val severity: AttentionSeverity,
    val reason: String,
)

data class AttentionBadgeState(
    val signals: List<AttentionSignal> = emptyList(),
) {
    val homeCount: Int get() = signals.size
    fun forServer(serverRef: ServerRef): List<AttentionSignal> = signals.filter { it.key.serverRef == serverRef }
    fun forWorkspace(serverRef: ServerRef, workspaceKey: WorkspaceKey): List<AttentionSignal> =
        signals.filter { it.key.serverRef == serverRef && it.key.workspaceKey == workspaceKey }
    fun forTab(tabId: String): List<AttentionSignal> = signals.filter { it.key.tabId == tabId }
}

class AttentionBadgeRegistry {
    private val _state = MutableStateFlow(AttentionBadgeState())
    val state: StateFlow<AttentionBadgeState> = _state.asStateFlow()

    fun set(signal: AttentionSignal) {
        _state.update { current ->
            current.copy(
                signals = current.signals.filterNot { it.key == signal.key && it.reason == signal.reason } + signal,
            )
        }
    }

    fun clearTab(tabId: String) {
        _state.update { current -> current.copy(signals = current.signals.filterNot { it.key.tabId == tabId }) }
    }

    fun clearServer(serverRef: ServerRef) {
        _state.update { current -> current.copy(signals = current.signals.filterNot { it.key.serverRef == serverRef }) }
    }
}
