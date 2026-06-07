package dev.blazelight.p4oc.ui.screens.licenses

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import dev.blazelight.p4oc.R
import dev.blazelight.p4oc.ui.components.TuiCard
import dev.blazelight.p4oc.ui.components.TuiTextButton
import dev.blazelight.p4oc.ui.components.TuiTopBar
import dev.blazelight.p4oc.ui.theme.LocalOpenCodeTheme
import dev.blazelight.p4oc.ui.theme.Sizing
import dev.blazelight.p4oc.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(
    onNavigateBack: () -> Unit,
    viewModel: LicensesViewModel = koinViewModel(),
) {
    val theme = LocalOpenCodeTheme.current
    val context = LocalContext.current
    val texts by viewModel.texts.collectAsState()
    val loading by viewModel.loadingTexts.collectAsState()
    val entries = remember { LicenseCatalogue.all }
    val requestChannelUrl = stringResource(R.string.licenses_request_channel_url)
    var expandedKey by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = theme.background,
        topBar = {
            TuiTopBar(
                title = stringResource(R.string.licenses_title),
                onNavigateBack = onNavigateBack,
            )
        },
    ) { padding ->
        LazyColumn(
            state = rememberLazyListState(),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("licenses_screen"),
            contentPadding = PaddingValues(
                horizontal = Spacing.screenPadding,
                vertical = Spacing.md,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item(key = "header_intro") { IntroNote() }
            // Show the copyleft source-availability notice whenever any
            // shipped entry triggers it (LGPL relinking + GPL conveying
            // source). Hidden entirely when no copyleft components ship.
            val hasCopyleftSourceNotice = entries.any { it.license.requiresSourceNotice }
            if (hasCopyleftSourceNotice) {
                item(key = "header_copyleft") {
                    CopyleftSourceNote(
                        showLgpl = entries.any { it.license == License.LGPL_2_1 },
                        showGpl = entries.any { it.license == License.GPL_3_0 },
                        onOpenRequestChannel = {
                            openUrl(context, requestChannelUrl)
                        },
                    )
                }
            }
            items(
                items = entries,
                key = { entry -> "${entry.name}::${entry.version ?: "planned"}" },
            ) { entry ->
                val key = "${entry.name}::${entry.version ?: "planned"}"
                val expanded = expandedKey == key
                LicenseRow(
                    entry = entry,
                    expanded = expanded,
                    fullText = texts[entry.license.rawTextRes],
                    isLoading = loading.contains(entry.license.rawTextRes),
                    onToggle = {
                        expandedKey = if (expanded) null else key
                        if (!expanded) viewModel.loadLicenseText(entry.license.rawTextRes)
                    },
                    onOpenUpstream = { openUrl(context, entry.upstreamUrl) },
                )
            }
        }
    }
}

@Composable
private fun IntroNote() {
    val theme = LocalOpenCodeTheme.current
    TuiCard {
        Text(
            text = stringResource(R.string.licenses_intro),
            style = MaterialTheme.typography.bodySmall,
            color = theme.text,
        )
    }
}

@Composable
private fun CopyleftSourceNote(
    showLgpl: Boolean,
    showGpl: Boolean,
    onOpenRequestChannel: () -> Unit,
) {
    val theme = LocalOpenCodeTheme.current
    TuiCard {
        Text(
            text = stringResource(R.string.licenses_copyleft_title),
            style = MaterialTheme.typography.titleSmall,
            color = theme.text,
        )
        if (showLgpl) {
            Text(
                text = stringResource(R.string.licenses_copyleft_body_lgpl),
                style = MaterialTheme.typography.bodySmall,
                color = theme.textMuted,
            )
        }
        if (showGpl) {
            Text(
                text = stringResource(R.string.licenses_copyleft_body_gpl),
                style = MaterialTheme.typography.bodySmall,
                color = theme.textMuted,
            )
        }
        TuiTextButton(
            onClick = onOpenRequestChannel,
            modifier = Modifier.testTag("licenses_request_channel_button"),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                modifier = Modifier.size(Sizing.iconSm),
            )
            Spacer(Modifier.width(Spacing.xs))
            Text(stringResource(R.string.licenses_request_channel_action))
        }
    }
}

@Composable
private fun LicenseRow(
    entry: LicenseEntry,
    expanded: Boolean,
    fullText: String?,
    isLoading: Boolean,
    onToggle: () -> Unit,
    onOpenUpstream: () -> Unit,
) {
    val theme = LocalOpenCodeTheme.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("licenses_item_${entry.name}"),
        color = theme.backgroundElement,
        shape = RectangleShape,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button, onClick = onToggle)
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    modifier = Modifier.size(Sizing.iconSm),
                    tint = if (entry.license.isCopyleft) theme.warning else theme.textMuted,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = theme.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = buildSubtitle(entry),
                        style = MaterialTheme.typography.bodySmall,
                        color = theme.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = stringResource(
                        if (expanded) R.string.licenses_collapse else R.string.licenses_expand,
                    ),
                    modifier = Modifier.size(Sizing.iconSm),
                    tint = theme.textMuted,
                )
            }

            AnimatedVisibility(visible = expanded) {
                LicenseDetail(
                    entry = entry,
                    fullText = fullText,
                    isLoading = isLoading,
                    onOpenUpstream = onOpenUpstream,
                )
            }

            HorizontalDivider(
                thickness = Sizing.dividerThickness,
                color = theme.borderSubtle,
            )
        }
    }
}

@Composable
private fun LicenseDetail(
    entry: LicenseEntry,
    fullText: String?,
    isLoading: Boolean,
    onOpenUpstream: () -> Unit,
) {
    val theme = LocalOpenCodeTheme.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        if (entry.notes != null) {
            Text(
                text = entry.notes,
                style = MaterialTheme.typography.bodySmall,
                color = theme.text,
            )
        }

        TuiTextButton(
            onClick = onOpenUpstream,
            modifier = Modifier.testTag("licenses_upstream_${entry.name}"),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                modifier = Modifier.size(Sizing.iconSm),
            )
            Spacer(Modifier.width(Spacing.xs))
            Text(
                text = stringResource(R.string.licenses_open_upstream),
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Text(
            text = stringResource(
                R.string.licenses_full_text_heading,
                entry.license.displayName,
            ),
            style = MaterialTheme.typography.labelMedium,
            color = theme.textMuted,
        )

        when {
            isLoading || fullText == null -> {
                Text(
                    text = stringResource(R.string.licenses_loading),
                    style = MaterialTheme.typography.bodySmall,
                    color = theme.textMuted,
                )
            }
            else -> {
                Surface(
                    color = theme.backgroundPanel,
                    shape = RectangleShape,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier
                            .heightIn(max = Sizing.embeddedScrollMaxHeight)
                            .verticalScroll(rememberScrollState())
                            .padding(Spacing.md),
                    ) {
                        Text(
                            text = fullText,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                            ),
                            color = theme.text,
                        )
                    }
                }
            }
        }
    }
}

private fun buildSubtitle(entry: LicenseEntry): String {
    // Shipped entries always carry a version (Maven version or "commit:<sha>"
    // for vendored snapshots). null is reserved for documented-but-not-shipped
    // entries; show "—" rather than misleadingly labelling them "planned".
    val versionPart = entry.version ?: "—"
    return "$versionPart  •  ${entry.license.spdxId}"
}

private fun openUrl(context: android.content.Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}
