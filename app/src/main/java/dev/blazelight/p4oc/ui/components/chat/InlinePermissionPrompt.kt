package dev.blazelight.p4oc.ui.components.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import dev.blazelight.p4oc.R
import dev.blazelight.p4oc.domain.model.Permission
import dev.blazelight.p4oc.ui.permission.permissionTitle
import dev.blazelight.p4oc.ui.theme.LocalOpenCodeTheme
import dev.blazelight.p4oc.ui.theme.Sizing
import dev.blazelight.p4oc.ui.theme.Spacing
import dev.blazelight.p4oc.ui.theme.TuiCodeFontSize

/**
 * Compact inline permission prompt that appears below a tool widget.
 * Shows: permission title + Allow/Always/Reject buttons
 */
@Composable
fun InlinePermissionPrompt(
    permission: Permission,
    onAllow: () -> Unit,
    onAlways: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalOpenCodeTheme.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(theme.backgroundElement)
    ) {
        // Left accent strip — matches the design's `border-left:2px` permission card.
        Box(
            modifier = Modifier
                .width(Sizing.strokeThick)
                .fillMaxHeight()
                .background(theme.warning)
        )
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            // Header: "permission · <action>"
            Text(
                text = "permission · ${permission.type}",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = FontFamily.Monospace
                ),
                color = theme.warning
            )
            Text(
                text = permissionTitle(permission),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = TuiCodeFontSize.lg
                ),
                color = theme.text
            )

            // Action buttons row — allow once / always / deny
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                OutlinedButton(
                    onClick = onAllow,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .heightIn(min = Sizing.minTouchTarget)
                        .testTag("permission_allow_once_${permission.id}"),
                    shape = RectangleShape,
                    contentPadding = PaddingValues(horizontal = Spacing.sm, vertical = Spacing.none),
                    border = BorderStroke(Sizing.strokeMd, theme.success),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.success)
                ) {
                    Text(
                        stringResource(R.string.allow),
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                OutlinedButton(
                    onClick = onAlways,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .heightIn(min = Sizing.minTouchTarget)
                        .testTag("permission_always_allow_${permission.id}"),
                    shape = RectangleShape,
                    contentPadding = PaddingValues(horizontal = Spacing.sm, vertical = Spacing.none),
                    border = BorderStroke(Sizing.strokeMd, theme.border),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.textMuted)
                ) {
                    Text(
                        stringResource(R.string.always_allow),
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .heightIn(min = Sizing.minTouchTarget)
                        .testTag("permission_deny_${permission.id}"),
                    shape = RectangleShape,
                    contentPadding = PaddingValues(horizontal = Spacing.sm, vertical = Spacing.none),
                    border = BorderStroke(Sizing.strokeMd, theme.error),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.error)
                ) {
                    Text(
                        stringResource(R.string.deny),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}
