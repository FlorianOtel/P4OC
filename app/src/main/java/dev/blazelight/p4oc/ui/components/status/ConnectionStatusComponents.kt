package dev.blazelight.p4oc.ui.components.status

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import dev.blazelight.p4oc.R
import dev.blazelight.p4oc.core.network.ConnectionState
import dev.blazelight.p4oc.ui.theme.LocalOpenCodeTheme

/**
 * Centralised [ConnectionState] → status-color mapping shared by Home server rows, the
 * workspace-detail header, and the Start Work target card so every surface reports the
 * same status language for a server connection.
 */
@Composable
internal fun connectionStatusColor(state: ConnectionState?): Color {
    val theme = LocalOpenCodeTheme.current
    return when (state) {
        is ConnectionState.Connected -> theme.success
        is ConnectionState.Connecting -> theme.warning
        is ConnectionState.Error -> theme.error
        else -> theme.textMuted
    }
}

/** Centralised short visible [ConnectionState] label, e.g. for the Start Work target card. */
@Composable
internal fun connectionStatusLabel(state: ConnectionState?): String = when (state) {
    is ConnectionState.Connected -> stringResource(R.string.server_status_connected)
    is ConnectionState.Connecting -> stringResource(R.string.server_status_connecting)
    is ConnectionState.Error -> stringResource(R.string.server_status_error)
    else -> stringResource(R.string.server_status_offline)
}

/** Centralised accessibility description for a [ConnectionState], e.g. for Home status dots. */
@Composable
internal fun connectionStatusDescription(state: ConnectionState?): String = when (state) {
    is ConnectionState.Connected -> stringResource(R.string.server_status_cd_connected)
    is ConnectionState.Connecting -> stringResource(R.string.server_status_cd_connecting)
    is ConnectionState.Error -> stringResource(R.string.server_status_cd_error)
    else -> stringResource(R.string.server_status_cd_offline)
}
