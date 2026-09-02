package dev.blazelight.p4oc.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.blazelight.p4oc.core.datastore.ConnectionSettings
import dev.blazelight.p4oc.core.datastore.SettingsDataStore
import dev.blazelight.p4oc.core.network.ConnectionState
import dev.blazelight.p4oc.core.network.ServerConnectionRegistry
import dev.blazelight.p4oc.core.network.ServerUrl
import dev.blazelight.p4oc.ui.workspace.WorkspaceRepositoryOwner
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

sealed interface SettingsConnectionContext {
    data object Global : SettingsConnectionContext

    data class Tab(val owner: WorkspaceRepositoryOwner) : SettingsConnectionContext
}

class SettingsViewModel constructor(
    private val settingsDataStore: SettingsDataStore,
    private val serverConnectionRegistry: ServerConnectionRegistry,
    private val connectionContext: SettingsConnectionContext,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val connectionSettings: StateFlow<ConnectionSettings> =
        settingsDataStore.connectionSettings
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ConnectionSettings())

    /**
     * Display name of the server these settings are scoped to, or `null` when Settings was opened
     * outside any workspace tab. Provider/agent/skill configuration lives on one specific server,
     * so with several servers registered there is no meaningful "current" one to target — the
     * caller hides that group instead of showing it permanently disabled.
     */
    val scopedServerName: String? = when (val context = connectionContext) {
        SettingsConnectionContext.Global -> null
        is SettingsConnectionContext.Tab -> context.owner.workspace.server.displayName
    }

    /** Whether the app is currently connected to an OpenCode server. */
    val isConnected: StateFlow<Boolean> =
        connectionContext.connectionState(serverConnectionRegistry)
            .map { it is ConnectionState.Connected }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        viewModelScope.launch {
            settingsUiState().collect { state ->
                _uiState.value = state
            }
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            settingsDataStore.setThemeMode(mode)
        }
    }

    fun toggleAutoReconnect() {
        viewModelScope.launch {
            val current = connectionSettings.value
            settingsDataStore.updateConnectionSettings(
                current.copy(autoReconnect = !current.autoReconnect)
            )
        }
    }

    fun updateReconnectTimeout(seconds: Int) {
        viewModelScope.launch {
            val current = connectionSettings.value
            settingsDataStore.updateConnectionSettings(
                current.copy(reconnectTimeoutSeconds = seconds.coerceIn(15, 120))
            )
        }
    }

    suspend fun disconnect() {
        val tab = connectionContext as? SettingsConnectionContext.Tab ?: return
        val serverRef = tab.owner.workspace.server
        if (serverConnectionRegistry.generation(serverRef) != tab.owner.generation) return

        serverConnectionRegistry.disconnect(serverRef)
        val persistedEndpoint = settingsDataStore.getLastConnection()?.first?.url
            ?.let(ServerUrl::endpointKey)
        if (persistedEndpoint == serverRef.endpointKey) {
            settingsDataStore.clearLastConnection()
        }
    }

    private fun settingsUiState(): Flow<SettingsUiState> = when (val context = connectionContext) {
        SettingsConnectionContext.Global -> combine(
            settingsDataStore.serverUrl,
            settingsDataStore.isLocalServer,
            settingsDataStore.themeMode,
        ) { url, isLocal, theme -> SettingsUiState(url, isLocal, theme) }

        is SettingsConnectionContext.Tab -> settingsDataStore.themeMode.map { theme ->
            val endpoint = context.owner.workspace.server.endpointKey
            SettingsUiState(
                serverUrl = endpoint,
                isLocal = endpoint.toHttpUrlOrNull()?.host in LOCAL_HOSTS,
                themeMode = theme,
            )
        }
    }

    private fun SettingsConnectionContext.connectionState(
        registry: ServerConnectionRegistry,
    ): Flow<ConnectionState> = when (this) {
        SettingsConnectionContext.Global -> flowOf(ConnectionState.Disconnected)
        is SettingsConnectionContext.Tab -> registry.connectionState(owner.workspace.server, owner.generation)
    }

    private companion object {
        val LOCAL_HOSTS = setOf("localhost", "127.0.0.1", "::1")
    }
}

data class SettingsUiState(
    val serverUrl: String = "",
    val isLocal: Boolean = true,
    val themeMode: String = "system"
)
