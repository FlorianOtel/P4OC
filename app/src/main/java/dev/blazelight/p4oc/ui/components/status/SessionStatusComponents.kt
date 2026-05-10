package dev.blazelight.p4oc.ui.components.status

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import dev.blazelight.p4oc.R
import dev.blazelight.p4oc.domain.model.SessionPresence
import dev.blazelight.p4oc.ui.theme.LocalOpenCodeTheme
import dev.blazelight.p4oc.ui.theme.Sizing
import dev.blazelight.p4oc.ui.theme.Spacing

data class SessionStatusVisual(
    val color: Color,
    val label: String,
    val contentDescription: String,
    val icon: ImageVector? = null,
    val showSpinner: Boolean = false,
    val shouldPulse: Boolean = false,
    val showAttentionBadge: Boolean = false,
)

object SessionStatusVisuals {
    @Composable
    fun forPresence(presence: SessionPresence): SessionStatusVisual {
        val theme = LocalOpenCodeTheme.current
        return when (presence) {
            SessionPresence.ERROR -> SessionStatusVisual(
                color = theme.error,
                label = stringResource(R.string.status_legend_error_label),
                contentDescription = stringResource(R.string.status_cd_error),
            )
            SessionPresence.RETRYING -> SessionStatusVisual(
                color = theme.warning,
                label = stringResource(R.string.status_legend_retrying_label),
                contentDescription = stringResource(R.string.status_cd_retrying),
                icon = Icons.Default.Refresh,
                shouldPulse = true,
            )
            SessionPresence.AWAITING_INPUT -> SessionStatusVisual(
                color = theme.warning,
                label = stringResource(R.string.status_legend_awaiting_label),
                contentDescription = stringResource(R.string.status_cd_awaiting),
                shouldPulse = true,
                showAttentionBadge = true,
            )
            SessionPresence.BUSY -> SessionStatusVisual(
                color = theme.primary,
                label = stringResource(R.string.status_legend_running_label),
                contentDescription = stringResource(R.string.status_cd_running),
                showSpinner = true,
                shouldPulse = true,
            )
            SessionPresence.UNREAD -> SessionStatusVisual(
                color = theme.accent,
                label = stringResource(R.string.status_legend_unread_label),
                contentDescription = stringResource(R.string.status_cd_unread),
            )
            SessionPresence.IDLE -> SessionStatusVisual(
                color = theme.textMuted,
                label = stringResource(R.string.status_legend_idle_label),
                contentDescription = stringResource(R.string.status_cd_idle),
            )
            SessionPresence.BACKGROUND -> SessionStatusVisual(
                color = theme.borderSubtle,
                label = stringResource(R.string.status_legend_background_label),
                contentDescription = stringResource(R.string.status_cd_background),
            )
        }
    }
}

@Composable
fun SessionStatusDot(
    presence: SessionPresence,
    modifier: Modifier = Modifier,
    size: Dp = Sizing.indicatorDot,
) {
    val visual = SessionStatusVisuals.forPresence(presence)
    val pulseAlpha = statusPulseAlpha(visual.shouldPulse)

    Box(
        modifier = modifier
            .size(size)
            .alpha(pulseAlpha)
            .testTag("session_status_dot_${presence.name.lowercase()}"),
        contentAlignment = Alignment.Center,
    ) {
        if (visual.showSpinner) {
            CircularProgressIndicator(
                modifier = Modifier.size(size),
                color = visual.color,
                strokeWidth = Sizing.strokeThin,
            )
        } else {
            Icon(
                imageVector = visual.icon ?: Icons.Default.Circle,
                contentDescription = visual.contentDescription,
                modifier = Modifier.size(size),
                tint = visual.color,
            )
        }

        if (visual.showAttentionBadge) {
            Icon(
                imageVector = Icons.Default.PriorityHigh,
                contentDescription = stringResource(R.string.status_cd_needs_attention),
                modifier = Modifier
                    .size(Spacing.sm)
                    .offset(x = Spacing.xs, y = -Spacing.xs),
                tint = visual.color,
            )
        }
    }
}

@Composable
fun SessionStatusRow(
    presence: SessionPresence,
    modifier: Modifier = Modifier,
    label: String? = null,
    size: Dp = Sizing.indicatorDotActive,
) {
    val visual = SessionStatusVisuals.forPresence(presence)
    Row(
        modifier = modifier.testTag("session_status_row_${presence.name.lowercase()}"),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SessionStatusDot(presence = presence, size = size)
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = visual.color,
            )
        }
    }
}

@Composable
private fun statusPulseAlpha(enabled: Boolean): Float {
    if (!enabled) return 1f
    val infiniteTransition = rememberInfiniteTransition(label = "statusPulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "statusPulseAlpha",
    )
    return alpha
}
