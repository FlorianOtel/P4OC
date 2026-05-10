package dev.blazelight.p4oc.ui.screens.terminal

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.termux.terminal.TerminalEmulator
import dev.blazelight.p4oc.core.log.AppLog
import dev.blazelight.p4oc.core.network.ApiResult
import dev.blazelight.p4oc.core.network.ConnectionManager
import dev.blazelight.p4oc.core.network.PtyWebSocketClient
import dev.blazelight.p4oc.core.network.safeApiCall
import dev.blazelight.p4oc.data.remote.dto.PtySizeDto
import dev.blazelight.p4oc.data.remote.dto.UpdatePtyRequest
import dev.blazelight.p4oc.domain.model.OpenCodeEvent
import dev.blazelight.p4oc.terminal.PtyTerminalClient
import dev.blazelight.p4oc.terminal.WebSocketTerminalOutput
import dev.blazelight.p4oc.ui.navigation.Screen
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for a single PTY terminal session.
 * Each terminal tab gets its own instance with its own ptyId and websocket connection.
 */
class TerminalViewModel constructor(
    savedStateHandle: SavedStateHandle,
    private val context: Context,
    private val connectionManager: ConnectionManager,
    private val ptyWebSocket: PtyWebSocketClient
) : ViewModel() {

    companion object {
        private const val TAG = "TerminalViewModel"
        private const val DEFAULT_ROWS = 24
        private const val DEFAULT_COLS = 80
        private const val TRANSCRIPT_ROWS = 2000
        private const val RESIZE_DEBOUNCE_MS = 150L
    }

    val ptyId: String = savedStateHandle.get<String>(Screen.Terminal.ARG_PTY_ID)
        ?: throw IllegalArgumentException("ptyId is required for TerminalViewModel")

    private val _uiState = MutableStateFlow(TerminalUiState())
    val uiState: StateFlow<TerminalUiState> = _uiState.asStateFlow()

    private val _terminalInvalidations = MutableSharedFlow<Unit>(extraBufferCapacity = 64)
    val terminalInvalidations: SharedFlow<Unit> = _terminalInvalidations.asSharedFlow()

    private var emulator: TerminalEmulator? = null
    private var terminalOutput: WebSocketTerminalOutput? = null
    private var terminalClient: PtyTerminalClient? = null
    private var lastKnownCols = 0
    private var lastKnownRows = 0
    private val pendingResize = MutableStateFlow<Pair<Int, Int>?>(null)

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
        fetchPtyDetails()
        connectToSession()
        observeEvents()
        observeWebSocketOutput()
        observeWebSocketState()
        observeResizeRequests()
    }

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    private fun observeResizeRequests() {
        viewModelScope.launch {
            pendingResize
                .filterNotNull()
                .debounce(RESIZE_DEBOUNCE_MS)
                .collect { (rows, cols) ->
                    val api = connectionManager.getApi() ?: return@collect
                    val result = safeApiCall {
                        api.updatePtySession(
                            ptyId,
                            UpdatePtyRequest(size = PtySizeDto(rows = rows, cols = cols))
                        )
                    }
                    when (result) {
                        is ApiResult.Success -> AppLog.d(TAG, "PTY size updated to ${cols}x$rows")
                        is ApiResult.Error -> AppLog.w(TAG, "Failed to update PTY size: ${result.message}")
                    }
                }
        }
    }

    private fun fetchPtyDetails() {
        viewModelScope.launch {
            val api = connectionManager.getApi() ?: return@launch
            val result = safeApiCall { api.listPtySessions() }
            when (result) {
                is ApiResult.Success -> {
                    val pty = result.data.find { it.id == ptyId }
                    pty?.let {
                        _uiState.update { state -> state.copy(title = it.title) }
                    }
                }
                is ApiResult.Error -> {
                    AppLog.e(TAG, "Failed to fetch PTY details: ${result.message}")
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
                AppLog.d(TAG, "Session title changed: $title")
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
                AppLog.d(TAG, "Terminal title changed: $newTitle")
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
        ptyWebSocket.connect(ptyId)
        _uiState.update { it.copy(isConnecting = true) }
    }

    private fun observeWebSocketOutput() {
        viewModelScope.launch {
            ptyWebSocket.output.collect { data ->
                val em = emulator ?: return@collect
                val bytes = data.toByteArray()
                em.append(bytes, bytes.size)
                requestTerminalInvalidation()
            }
        }
    }

    private fun observeWebSocketState() {
        viewModelScope.launch {
            ptyWebSocket.connectionState.collect { connectionState ->
                when (connectionState) {
                    is PtyWebSocketClient.ConnectionState.Connected -> {
                        AppLog.d(TAG, "WebSocket connected to ${connectionState.ptyId}")
                        _uiState.update { it.copy(isConnected = true, isConnecting = false) }
                    }
                    is PtyWebSocketClient.ConnectionState.Error -> {
                        AppLog.e(TAG, "WebSocket error: ${connectionState.message}")
                        _uiState.update {
                            it.copy(
                                error = "Connection error: ${connectionState.message}",
                                isConnected = false,
                                isConnecting = false
                            )
                        }
                    }
                    is PtyWebSocketClient.ConnectionState.Disconnected -> {
                        AppLog.d(TAG, "WebSocket disconnected")
                        _uiState.update { it.copy(isConnected = false, isConnecting = false) }
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
            connectionManager.scopedEvents.collect { scopedEvent ->
                when (val event = scopedEvent.event) {
                    is OpenCodeEvent.PtyUpdated -> {
                        if (event.pty.id == ptyId) {
                            _uiState.update { it.copy(title = event.pty.title) }
                        }
                    }
                    is OpenCodeEvent.PtyExited -> {
                        if (event.id == ptyId) {
                            val exitMessage = "\r\n[Process exited with code ${event.exitCode}]\r\n"
                            val bytes = exitMessage.toByteArray()
                            emulator?.append(bytes, bytes.size)
                            requestTerminalInvalidation()
                            _uiState.update { it.copy(isExited = true) }
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
        requestTerminalInvalidation()
    }

    private fun requestTerminalInvalidation() {
        _terminalInvalidations.tryEmit(Unit)
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        ptyWebSocket.disconnect()
        emulator = null
        terminalClient = null
        terminalOutput = null
    }
}

data class TerminalUiState(
    val isConnecting: Boolean = false,
    val isConnected: Boolean = false,
    val isExited: Boolean = false,
    val title: String? = null,
    val error: String? = null
)
