package dev.blazelight.p4oc.ui.screens.server

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import dev.blazelight.p4oc.R
import dev.blazelight.p4oc.core.network.DiscoveredServer
import dev.blazelight.p4oc.core.network.DiscoveryState
import dev.blazelight.p4oc.ui.theme.LocalOpenCodeTheme
import dev.blazelight.p4oc.ui.theme.Sizing
import dev.blazelight.p4oc.ui.theme.Spacing
import dev.blazelight.p4oc.ui.theme.TuiCodeFontSize

private const val SCAN_PULSE_MIN_ALPHA = 0.35f
private const val SCAN_PULSE_DURATION_MS = 700

/**
 * Nearby-server discovery (design 03): flat scanning row + "use" rows, no boxed panel.
 */
@Composable
internal fun discoveredServersSection(
    servers: List<DiscoveredServer>,
    discoveryState: DiscoveryState,
    isConnecting: Boolean,
    onServerClick: (DiscoveredServer) -> Unit,
) {
    Column {
        if (discoveryState == DiscoveryState.SCANNING) {
            scanningRow()
            Spacer(Modifier.height(Spacing.md))
        }
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            servers.forEach { server ->
                discoveredServerRow(server, isConnecting, onServerClick)
            }
        }
    }
}

@Composable
private fun discoveredServerRow(
    server: DiscoveredServer,
    isConnecting: Boolean,
    onServerClick: (DiscoveredServer) -> Unit,
) {
    val theme = LocalOpenCodeTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(theme.backgroundPanel, RectangleShape)
            .clickable(enabled = !isConnecting, role = Role.Button) {
                onServerClick(server)
            }
            .testTag("discovered_server_${server.serviceName}")
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.mdLg),
    ) {
        Box(
            modifier = Modifier
                .size(Sizing.indicatorDotActive)
                .background(theme.info, RectangleShape),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = server.serviceName,
                color = theme.text,
                fontSize = TuiCodeFontSize.xl,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${server.host}:${server.port}",
                color = theme.textMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = TuiCodeFontSize.md,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = stringResource(R.string.server_use),
            color = theme.secondary,
            fontFamily = FontFamily.Monospace,
            fontSize = TuiCodeFontSize.md,
        )
    }
}

@Composable
private fun scanningRow() {
    val theme = LocalOpenCodeTheme.current
    val transition = rememberInfiniteTransition(label = "scanning")
    val alpha by transition.animateFloat(
        initialValue = SCAN_PULSE_MIN_ALPHA,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(SCAN_PULSE_DURATION_MS), RepeatMode.Reverse),
        label = "scanPulse",
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(Sizing.iconXxs),
            color = theme.warning.copy(alpha = alpha),
            strokeWidth = Sizing.strokeThick,
        )
        Text(
            text = stringResource(R.string.setup_scanning_nearby),
            color = theme.warning,
            fontFamily = FontFamily.Monospace,
            fontSize = TuiCodeFontSize.md,
        )
    }
}
