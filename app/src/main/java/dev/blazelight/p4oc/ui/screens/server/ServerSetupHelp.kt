package dev.blazelight.p4oc.ui.screens.server

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import dev.blazelight.p4oc.R
import dev.blazelight.p4oc.ui.theme.LocalOpenCodeTheme
import dev.blazelight.p4oc.ui.theme.Sizing
import dev.blazelight.p4oc.ui.theme.Spacing
import dev.blazelight.p4oc.ui.theme.TuiCodeFontSize

/**
 * "Server Setup" help (design 03): the three install/start/connect steps plus a
 * networking tip. [serverSetupHelpSection] wraps it in the collapsible row used on
 * the connect-to-server screen; the first-run screen embeds [serverSetupHelpContent]
 * directly under its own footer toggle so both screens render the same help.
 */
@Composable
internal fun serverSetupHelpSection() {
    val theme = LocalOpenCodeTheme.current
    var expanded by rememberSaveable { mutableStateOf(false) }

    Column {
        serverSetupHelpToggle(expanded, "server_setup_help_toggle") { expanded = !expanded }
        AnimatedVisibility(expanded) {
            Surface(color = theme.backgroundElement, shape = RectangleShape) {
                serverSetupHelpContent(Modifier.padding(Spacing.md))
            }
        }
    }
}

/**
 * The collapsible header row — "Server Setup · Need help starting a server? ▸".
 * Shared so the first-run footer and the connect screen present the same affordance.
 */
@Composable
internal fun serverSetupHelpToggle(
    expanded: Boolean,
    testTag: String,
    onToggle: () -> Unit,
) {
    val theme = LocalOpenCodeTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(Sizing.buttonHeightLg)
            .background(theme.backgroundPanel, RectangleShape)
            .border(Sizing.strokeMd, theme.backgroundElement, RectangleShape)
            .clickable(role = Role.Button) { onToggle() }
            .padding(horizontal = Spacing.lg)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(
            text = stringResource(R.string.server_setup_title),
            color = theme.text,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            fontSize = TuiCodeFontSize.lg,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(R.string.server_setup_subtitle),
            color = theme.textMuted,
            fontFamily = FontFamily.Monospace,
            fontSize = TuiCodeFontSize.lg,
        )
        Text(
            text = if (expanded) "▾" else "▸",
            color = theme.secondary,
            fontFamily = FontFamily.Monospace,
            fontSize = TuiCodeFontSize.lg,
        )
    }
}

/** The three numbered steps plus the networking tip, without any surrounding chrome. */
@Composable
internal fun serverSetupHelpContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        setupStep(
            number = "1",
            title = stringResource(R.string.server_setup_step1_title),
            command = stringResource(R.string.server_setup_step1_cmd),
        )
        setupStep(
            number = "2",
            title = stringResource(R.string.server_setup_step2_title),
            command = stringResource(R.string.server_setup_step2_cmd),
        )
        setupStep(
            number = "3",
            title = stringResource(R.string.server_setup_step3_title),
            command = stringResource(R.string.server_setup_step3_cmd),
        )
        setupHelpTip()
    }
}

@Composable
private fun setupHelpTip() {
    val theme = LocalOpenCodeTheme.current
    Surface(
        color = theme.primary.copy(alpha = TIP_FILL_ALPHA),
        shape = RectangleShape,
        modifier = Modifier.border(
            Sizing.strokeThin,
            theme.primary.copy(alpha = TIP_BORDER_ALPHA),
            RectangleShape,
        ),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                text = "── ${stringResource(R.string.server_setup_tip_label)} ──",
                fontFamily = FontFamily.Monospace,
                color = theme.primary,
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = emphasize(
                    text = stringResource(R.string.server_setup_tip_text),
                    emphasis = stringResource(R.string.server_setup_tip_flag),
                    emphasisColor = theme.primary,
                ),
                fontFamily = FontFamily.Monospace,
                color = theme.textMuted,
                style = MaterialTheme.typography.bodySmall,
            )
            commandBlock(stringResource(R.string.server_setup_find_ip))
            Text(
                text = stringResource(R.string.server_setup_test_label),
                fontFamily = FontFamily.Monospace,
                color = theme.textMuted,
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = stringResource(R.string.server_setup_test_url),
                fontFamily = FontFamily.Monospace,
                color = theme.primary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun setupStep(
    number: String,
    title: String,
    command: String,
) {
    val theme = LocalOpenCodeTheme.current
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            text = "$number. $title",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = theme.text,
            style = MaterialTheme.typography.bodyMedium,
        )
        commandBlock(command)
    }
}

/** Inset code box: command text in the accent colour, trailing `# ...` comments dimmed. */
@Composable
private fun commandBlock(command: String) {
    val theme = LocalOpenCodeTheme.current
    Surface(
        color = theme.background,
        shape = RectangleShape,
        modifier = Modifier.border(Sizing.strokeThin, theme.border, RectangleShape),
    ) {
        Text(
            text = dimComments(command, theme.textMuted),
            modifier = Modifier.fillMaxWidth().padding(Spacing.sm),
            fontFamily = FontFamily.Monospace,
            color = theme.primary,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

/** Recolours every occurrence of [emphasis] within [text] — used to pick out `--hostname 0.0.0.0`. */
private fun emphasize(text: String, emphasis: String, emphasisColor: Color): AnnotatedString =
    buildAnnotatedString {
        var cursor = 0
        while (true) {
            val match = text.indexOf(emphasis, cursor)
            if (match < 0) break
            append(text.substring(cursor, match))
            withStyle(SpanStyle(color = emphasisColor)) { append(emphasis) }
            cursor = match + emphasis.length
        }
        append(text.substring(cursor))
    }

/** Dims trailing `# ...` comments on each line so `$ ip addr  # Linux` reads as command + note. */
private fun dimComments(command: String, commentColor: Color): AnnotatedString =
    buildAnnotatedString {
        command.lineSequence().forEachIndexed { index, line ->
            if (index > 0) append("\n")
            val hash = line.indexOf('#')
            if (hash < 0) {
                append(line)
            } else {
                append(line.substring(0, hash))
                withStyle(SpanStyle(color = commentColor)) { append(line.substring(hash)) }
            }
        }
    }

private const val TIP_FILL_ALPHA = 0.08f
private const val TIP_BORDER_ALPHA = 0.3f
