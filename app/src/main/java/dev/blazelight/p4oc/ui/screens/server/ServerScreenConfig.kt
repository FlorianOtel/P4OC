package dev.blazelight.p4oc.ui.screens.server

import dev.blazelight.p4oc.core.datastore.SavedServer

internal data class ServerScreenConfig(
    val autoReconnect: Boolean = true,
    val showManualFormInitially: Boolean = false,
    val onConnectSavedServer: ((SavedServer) -> Unit)? = null,
    val onNavigateBack: (() -> Unit)? = null,
)
