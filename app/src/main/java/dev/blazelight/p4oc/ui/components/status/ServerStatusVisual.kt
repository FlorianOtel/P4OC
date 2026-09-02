package dev.blazelight.p4oc.ui.components.status

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import dev.blazelight.p4oc.R
import dev.blazelight.p4oc.ui.screens.server.ServerConnectionStatus
import dev.blazelight.p4oc.ui.theme.LocalOpenCodeTheme
import dev.blazelight.p4oc.ui.theme.Sizing
import dev.blazelight.p4oc.ui.theme.Spacing

data class ServerStatusVisual(
    val color: Color,
    val label: String,
    val contentDescription: String,
    val icon: ImageVector = Icons.Default.Circle,
    val showSpinner: Boolean = false,
)

@Composable
fun serverStatusVisual(status: ServerConnectionStatus): ServerStatusVisual {
    val theme = LocalOpenCodeTheme.current
    return when (status) {
        ServerConnectionStatus.CONNECTED -> ServerStatusVisual(
            theme.success,
            stringResource(R.string.server_status_connected),
            stringResource(R.string.server_status_cd_connected)
        )
        ServerConnectionStatus.CONNECTING -> ServerStatusVisual(
            theme.accent,
            stringResource(R.string.server_status_connecting),
            stringResource(R.string.server_status_cd_connecting),
            Icons.Default.Refresh,
            true
        )
        ServerConnectionStatus.AVAILABLE -> ServerStatusVisual(
            theme.success,
            stringResource(R.string.server_status_nearby),
            stringResource(R.string.server_status_cd_nearby)
        )
        ServerConnectionStatus.DISCONNECTED -> ServerStatusVisual(
            theme.textMuted,
            stringResource(R.string.server_status_offline),
            stringResource(R.string.server_status_cd_offline)
        )
        ServerConnectionStatus.ERROR -> ServerStatusVisual(
            theme.error,
            stringResource(R.string.server_status_error),
            stringResource(R.string.server_status_cd_error),
            Icons.Default.Error
        )
    }
}

@Composable
fun serverStatusIndicator(status: ServerConnectionStatus, modifier: Modifier = Modifier) {
    val visual = serverStatusVisual(status)
    Row(
        modifier = modifier.testTag("server_status_${status.name.lowercase()}"),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (visual.showSpinner) {
            CircularProgressIndicator(
                Modifier.size(Sizing.indicatorDotActive),
                color = visual.color,
                strokeWidth = Sizing.strokeMd
            )
        } else {
            Icon(visual.icon, visual.contentDescription, Modifier.size(Sizing.indicatorDotActive), tint = visual.color)
        }
        Text(visual.label, color = visual.color)
    }
}
