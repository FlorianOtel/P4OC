package dev.blazelight.p4oc.ui.screens.terminal

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.termux.terminal.TerminalEmulator
import dev.blazelight.p4oc.core.log.AppLog
import dev.blazelight.p4oc.core.network.ApiResult
import dev.blazelight.p4oc.core.network.PtyWebSocketClient
import dev.blazelight.p4oc.core.network.ServerConnectionRegistry
import dev.blazelight.p4oc.core.network.safeApiCall
import dev.blazelight.p4oc.data.remote.dto.PtySizeDto
import dev.blazelight.p4oc.data.remote.dto.UpdatePtyRequest
import dev.blazelight.p4oc.domain.model.OpenCodeEvent
import dev.blazelight.p4oc.terminal.PtyTerminalClient
import dev.blazelight.p4oc.terminal.WebSocketTerminalOutput
import dev.blazelight.p4oc.ui.navigation.Screen
import dev.blazelight.p4oc.ui.workspace.WorkspaceRepositoryOwner
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for a single PTY terminal session.
 * Each terminal tab gets its own instance with its own ptyId and websocket connection.
 */
class TerminalViewModel constructor(
    private val savedStateHandle: SavedStateHandle,
    private val context: Context,
    private val ptyWebSocket: PtyWebSocketClient,
    private val workspaceOwner: WorkspaceRepositoryOwner,
    private val serverConnectionRegistry: ServerConnectionRegistry,
) : ViewModel() {

    companion object {
        private const val TAG = "TerminalViewModel"
        private const val DEFAULT_ROWS = 24
        private const val DEFAULT_COLS = 80
        private const val TRANSCRIPT_ROWS = 2000
        private const val RESIZE_DEBOUNCE_MS = 150L
        private const val TRANSCRIPT_PERSIST_DEBOUNCE_MS = 500L
        private const val MAX_SAVED_TRANSCRIPT_CHARS = 64 * 1024
        internal const val MAX_ACCESSIBLE_SCREEN_CHARS = 4 * 1024
        private const val ACCESSIBLE_SCREEN_REFRESH_MS = 2_000L
        private const val KEY_TRANSCRIPT = "terminal_transcript"
        private const val KEY_TITLE = "terminal_title"
        private const val KEY_EXITED = "terminal_exited"

        private const val MAX_TITLE_CHARS = 1_024
    }

    val ptyId: String = savedStateHandle.get<String>(Screen.Terminal.ARG_PTY_ID)
        ?: throw IllegalArgumentException("ptyId is required for TerminalViewModel")

    private val _uiState = MutableStateFlow(
        TerminalUiState(
            title = savedStateHandle.get<String>(KEY_TITLE)?.take(MAX_TITLE_CHARS),
            isExited = savedStateHandle[KEY_EXITED] ?: false,
        )
    )
    val uiState: StateFlow<TerminalUiState> = _uiState.asStateFlow()

    private val _terminalInvalidations = MutableSharedFlow<Unit>(extraBufferCapacity = 64)
    val terminalInvalidations: SharedFlow<Unit> = _terminalInvalidations.asSharedFlow()

    private val accessibleScreenRefreshes = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val _accessibleScreenText = MutableStateFlow("")
    val accessibleScreenText: StateFlow<String> = _accessibleScreenText.asStateFlow()

    private var emulator: TerminalEmulator? = null
    private var terminalOutput: WebSocketTerminalOutput? = null
    private var terminalClient: PtyTerminalClient? = null
    private var lastKnownCols = 0
    private var lastKnownRows = 0
    private val pendingResize = MutableStateFlow<Pair<Int, Int>?>(null)
    private val transcript = BoundedTerminalTranscript(
        maxChars = MAX_SAVED_TRANSCRIPT_CHARS,
        restored = savedStateHandle.get<String>(KEY_TRANSCRIPT).orEmpty(),
    )
    private val transcriptPersistence = TerminalTranscriptPersistence(
        scope = viewModelScope,
        debounceMillis = TRANSCRIPT_PERSIST_DEBOUNCE_MS,
        snapshot = transcript::snapshot,
        persist = { savedStateHandle[KEY_TRANSCRIPT] = it },
    )

    fun onTerminalSizeChanged(rows: Int, cols: Int) {
        if (cols == lastKnownCols && rows == lastKnownRows) {
            return
        }

        lastKnownCols = cols
        lastKnownRows = rows

        AppLog.d(TAG, "Resizing terminal: ${cols}x$rows")
        emulator?.resize(cols, rows)
        requestTerminalInvalidation()
        pendingResize.value = rows to cols
    }

    init {
        initEmulator()
        replayRestoredTranscript()
        fetchPtyDetails()
        connectToSession()
        observeEvents()
        observeWebSocketOutput()
        observeWebSocketState()
        observeResizeRequests()
        observeAccessibleScreen()
    }

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    private fun observeAccessibleScreen() {
        viewModelScope.launch {
            accessibleScreenRefreshes
                .sample(ACCESSIBLE_SCREEN_REFRESH_MS)
                .collect { refreshAccessibleScreen() }
        }
    }

    private fun refreshAccessibleScreen() {
        val currentEmulator = emulator ?: return
        val visibleText = currentEmulator.screen.getSelectedText(
            0,
            0,
            currentEmulator.mColumns - 1,
            currentEmulator.mRows - 1,
        )
        _accessibleScreenText.value = boundedVisibleTerminalText(visibleText)
    }

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    private fun observeResizeRequests() {
        viewModelScope.launch {
            pendingResize
                .filterNotNull()
                .debounce(RESIZE_DEBOUNCE_MS)
                .collect { (rows, cols) ->
                    val api = serverConnectionRegistry.api(
                        workspaceOwner.workspace.server,
                        workspaceOwner.generation,
                    ) ?: return@collect
                    val result = safeApiCall {
                        api.updatePtySession(
                            ptyId,
                            directory = workspaceOwner.workspace.directory,
                            workspace = null,
                            request = UpdatePtyRequest(size = PtySizeDto(rows = rows, cols = cols)),
                        )
                    }
                    when (result) {
                        is ApiResult.Success -> AppLog.d(TAG, "PTY size updated to ${cols}x$rows")
                        is ApiResult.Error -> AppLog.w(TAG, "Failed to update PTY size")
                    }
                }
        }
    }
    private fun replayRestoredTranscript() {
        val restored = transcript.snapshot()
        if (restored.isEmpty()) return
        val bytes = restored.toByteArray()
        emulator?.append(bytes, bytes.size)
        requestTerminalInvalidation()
    }

    private fun appendTranscript(chunk: String) {
        val bytes = chunk.toByteArray()
        emulator?.append(bytes, bytes.size)
        transcript.append(chunk)
        transcriptPersistence.changed()
        requestTerminalInvalidation()
    }

    private fun fetchPtyDetails() {
        viewModelScope.launch {
            val api = serverConnectionRegistry.api(
                workspaceOwner.workspace.server,
                workspaceOwner.generation,
            ) ?: return@launch
            val result = safeApiCall {
                api.getPtySession(
                    id = ptyId,
                    directory = workspaceOwner.workspace.directory,
                    workspace = null,
                )
            }
            when (result) {
                is ApiResult.Success -> {
                    val title = result.data.title.take(MAX_TITLE_CHARS)
                    savedStateHandle[KEY_TITLE] = title
                    _uiState.update { state -> state.copy(title = title) }
                }
                is ApiResult.Error -> {
                    AppLog.e(TAG, "Failed to fetch PTY details")
                }
            }
        }
    }

    fun getTerminalEmulator(): TerminalEmulator? = emulator

    private fun initEmulator() {
        terminalClient = PtyTerminalClient(
            context = context,
            onTextChanged = { requestTerminalInvalidation() },
            onTitleChanged = { title ->
                AppLog.d(TAG, "Session title changed")
            },
            onSessionFinished = {
                AppLog.d(TAG, "Terminal session finished")
            },
            onBellCallback = {
                AppLog.d(TAG, "Terminal bell")
            },
            onPasteRequest = { text ->
                sendInput(text)
            }
        )

        terminalOutput = WebSocketTerminalOutput(
            webSocket = ptyWebSocket,
            onTitleChanged = { _, newTitle ->
                AppLog.d(TAG, "Terminal title changed")
            },
            onBell = {
                AppLog.d(TAG, "Terminal bell")
            }
        )

        emulator = TerminalEmulator(
            terminalOutput,
            DEFAULT_COLS,
            DEFAULT_ROWS,
            TRANSCRIPT_ROWS,
            terminalClient
        )
    }

    private fun connectToSession() {
        ptyWebSocket.connect(
            ptyId = ptyId,
            directory = workspaceOwner.workspace.directory,
            workspace = null,
        )
        _uiState.update { it.copy(isConnecting = true) }
    }

    private fun observeWebSocketOutput() {
        viewModelScope.launch {
            ptyWebSocket.output.collect { data ->
                appendTranscript(data)
            }
        }
    }

    private fun observeWebSocketState() {
        viewModelScope.launch {
            ptyWebSocket.connectionState.collect { connectionState ->
                when (connectionState) {
                    is PtyWebSocketClient.ConnectionState.Connected -> {
                        AppLog.d(TAG, "WebSocket connected")
                        _uiState.update { it.copy(isConnected = true, isConnecting = false, error = null) }
                    }
                    is PtyWebSocketClient.ConnectionState.Error -> {
                        AppLog.e(TAG, "WebSocket error")
                        _uiState.update {
                            it.copy(
                                error = "Unable to connect to this terminal",
                                isConnected = false,
                                isConnecting = false
                            )
                        }
                    }
                    is PtyWebSocketClient.ConnectionState.Disconnected -> {
                        AppLog.d(TAG, "WebSocket disconnected")
                        _uiState.update {
                            it.copy(
                                isConnected = false,
                                isConnecting = false,
                                error = it.error ?: "Terminal disconnected",
                            )
                        }
                    }
                    is PtyWebSocketClient.ConnectionState.Connecting -> {
                        AppLog.d(TAG, "WebSocket connecting...")
                        _uiState.update { it.copy(isConnecting = true) }
                    }
                }
            }
        }
    }

    private fun observeEvents() {
        viewModelScope.launch {
            serverConnectionRegistry.events(workspaceOwner.workspace.server).collect { scopedEvent ->
                if (
                    scopedEvent.generation != workspaceOwner.generation ||
                    scopedEvent.workspaceKey != workspaceOwner.workspace.key
                ) {
                    return@collect
                }
                when (val event = scopedEvent.event) {
                    is OpenCodeEvent.PtyUpdated -> {
                        if (event.pty.id == ptyId) {
                            val title = event.pty.title.take(MAX_TITLE_CHARS)
                            savedStateHandle[KEY_TITLE] = title
                            _uiState.update { it.copy(title = title) }
                        }
                    }
                    is OpenCodeEvent.PtyExited -> {
                        if (event.id == ptyId) {
                            val exitMessage = "\r\n[Process exited with code ${event.exitCode}]\r\n"
                            appendTranscript(exitMessage)
                            savedStateHandle[KEY_EXITED] = true
                            _uiState.update {
                                it.copy(isExited = true, isConnected = false, isConnecting = false, error = null)
                            }
                        }
                    }
                    is OpenCodeEvent.PtyDeleted -> {
                        if (event.id == ptyId) {
                            savedStateHandle[KEY_EXITED] = true
                            _uiState.update {
                                it.copy(isExited = true, isConnected = false, isConnecting = false, error = null)
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    fun sendInput(input: String) {
        if (input.isEmpty()) return
        if (!ptyWebSocket.isConnected()) {
            _uiState.update { it.copy(error = "Not connected to terminal") }
            return
        }
        ptyWebSocket.send(input)
    }

    fun clearTerminal() {
        emulator?.reset()
        transcript.clear()
        transcriptPersistence.changed()
        requestTerminalInvalidation()
    }

    fun reconnect() {
        if (_uiState.value.isConnecting || _uiState.value.isExited) return
        _uiState.update { it.copy(error = null, isConnected = false, isConnecting = true) }
        ptyWebSocket.reconnect()
    }

    private fun requestTerminalInvalidation() {
        _terminalInvalidations.tryEmit(Unit)
        accessibleScreenRefreshes.tryEmit(Unit)
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        transcriptPersistence.flushNow()
        super.onCleared()
        ptyWebSocket.close()
        emulator = null
        terminalClient = null
        terminalOutput = null
    }
}

internal fun boundedVisibleTerminalText(
    visibleText: String,
    maxChars: Int = TerminalViewModel.MAX_ACCESSIBLE_SCREEN_CHARS,
): String {
    require(maxChars > 0) { "maxChars must be positive" }
    val normalized = visibleText
        .lineSequence()
        .map(String::trimEnd)
        .dropWhile(String::isBlank)
        .toList()
        .dropLastWhile(String::isBlank)
        .joinToString("\n")
    if (normalized.length <= maxChars) return normalized

    var start = normalized.length - maxChars
    if (Character.isLowSurrogate(normalized[start]) && start > 0 && Character.isHighSurrogate(normalized[start - 1])) {
        start++
    }
    return normalized.substring(start)
}

data class TerminalUiState(
    val isConnecting: Boolean = false,
    val isConnected: Boolean = false,
    val isExited: Boolean = false,
    val title: String? = null,
    val error: String? = null
)
