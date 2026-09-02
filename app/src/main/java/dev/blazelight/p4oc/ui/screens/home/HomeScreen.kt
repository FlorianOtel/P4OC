@file:Suppress("TooManyFunctions")

package dev.blazelight.p4oc.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import dev.blazelight.p4oc.R
import dev.blazelight.p4oc.core.network.ConnectionState
import dev.blazelight.p4oc.domain.model.SessionPresence
import dev.blazelight.p4oc.domain.server.WorkspaceKey
import dev.blazelight.p4oc.ui.components.TuiBackButton
import dev.blazelight.p4oc.ui.components.status.SessionStatusDot
import dev.blazelight.p4oc.ui.components.status.connectionStatusColor
import dev.blazelight.p4oc.ui.components.status.connectionStatusDescription
import dev.blazelight.p4oc.ui.tabs.StartWorkSelection
import dev.blazelight.p4oc.ui.tabs.StartWorkTarget
import dev.blazelight.p4oc.ui.theme.LocalOpenCodeTheme
import dev.blazelight.p4oc.ui.theme.ProjectColors
import dev.blazelight.p4oc.ui.theme.Sizing
import dev.blazelight.p4oc.ui.theme.Spacing
import java.util.concurrent.TimeUnit

private const val RECENT_DAY_LIMIT = 30
private const val HOME_WORKSPACE_SHORTCUT_LIMIT = 3
private const val DASH_ON = 10f
private const val DASH_OFF = 8f
data class HomeActions(
    val onBrowseSessions: (StartWorkTarget) -> Unit,
    val onBrowseAllSessions: () -> Unit = {},
    val onOpenFiles: (StartWorkTarget) -> Unit,
    val onOpenTerminal: (StartWorkTarget) -> Unit,
    val onChooseTarget: () -> Unit,
    val onManageServers: () -> Unit = {},
    val onRefresh: () -> Unit = {},
    val onSettings: () -> Unit = {},
    val onFocusTab: (String) -> Unit = {},
    val onResumeSession: (SessionPreview) -> Unit = {},
    val onWorkspaceSelected: (WorkspaceSummary) -> Unit = {},
    val onWorkspaceDetailChanged: (StartWorkSelection) -> Unit = {},
    val onStartScopedWork: (StartWorkTarget) -> Unit = {},
)

private data class HomeOverviewInput(
    val summary: HomeSummaryState,
    val searchQuery: String,
    val showAllWorkspaces: Boolean,
    val onSearchQueryChange: (String) -> Unit,
    val onToggleAllWorkspaces: () -> Unit,
    val onWorkspaceClick: (WorkspaceSummary) -> Unit,
    val actions: HomeActions,
    val listState: LazyListState,
)

private data class WorkspaceDetailInput(
    val workspace: WorkspaceSummary,
    val connectionState: ConnectionState?,
    val openWork: List<OpenWorkSummary>,
    val sessions: List<SessionPreview>,
    val actions: HomeActions,
    val onBack: () -> Unit,
)

@Composable
@Suppress("LongMethod")
fun homeScreen(
    summary: HomeSummaryState,
    actions: HomeActions,
    modifier: Modifier = Modifier,
) {
    var selectedWorkspace by remember { mutableStateOf<WorkspaceSummary?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showAllWorkspaces by rememberSaveable { mutableStateOf(false) }
    val selected = selectedWorkspace
    if (selected != null) {
        workspaceDetail(
            input = WorkspaceDetailInput(
                workspace = selected,
                connectionState = summary.servers.firstOrNull {
                    it.serverRef.endpointKey == selected.serverRef.endpointKey
                }?.connectionState,
                openWork = summary.openWork.filter {
                    it.serverRef.endpointKey == selected.serverRef.endpointKey &&
                        it.workspaceKey == selected.workspaceKey
                },
                sessions = summary.sessions.filter {
                    it.serverRef.endpointKey == selected.serverRef.endpointKey &&
                        it.workspaceKey == selected.workspaceKey
                },
                actions = actions,
                onBack = {
                    selectedWorkspace = null
                    actions.onWorkspaceDetailChanged(StartWorkSelection.NeedsSelection)
                },
            ),
            modifier = modifier,
        )
    } else {
        homeOverview(
            input = HomeOverviewInput(
                summary = summary,
                searchQuery = searchQuery,
                showAllWorkspaces = showAllWorkspaces,
                onSearchQueryChange = { searchQuery = it },
                onToggleAllWorkspaces = { showAllWorkspaces = !showAllWorkspaces },
                onWorkspaceClick = {
                    selectedWorkspace = it
                    actions.onWorkspaceSelected(it)
                    actions.onWorkspaceDetailChanged(
                        StartWorkSelection.Selected(StartWorkTarget(it.serverRef, it.workspaceKey)),
                    )
                },
                actions = actions,
                listState = rememberLazyListState(),
            ),
            modifier = modifier,
        )
    }
}

@Composable
private fun homeOverview(input: HomeOverviewInput, modifier: Modifier) {
    val summary = input.summary
    val results by remember(summary, input.searchQuery) {
        derivedStateOf { summary.filteredHomeResults(input.searchQuery) }
    }
    LazyColumn(
        state = input.listState,
        modifier = modifier.fillMaxSize().testTag("home_screen"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        homeOverviewContent(input, results)
    }
}

private fun LazyListScope.homeOverviewContent(
    input: HomeOverviewInput,
    results: FilteredHomeResults,
) {
    val summary = input.summary
    item { homeHeader(summary, input.actions) }
    item { homeSearchField(input.searchQuery, input.onSearchQueryChange) }
    item { Spacer(Modifier.height(Spacing.sm)) }
    if (summary.isLoading) {
        item {
            infoCard("Refreshing", "Already-loaded work stays available.")
        }
    }
    if (summary.partialFailures.isNotEmpty()) {
        item { infoCard("Some servers did not respond", summary.partialFailures.joinToString(" · ")) }
    }
    // Workspaces first, then the sessions themselves, so resuming a session is one tap from Home.
    homeWorkspaces(input, results.workspaces)
    HomeSessions(input, results.sessions)
    if (input.searchQuery.isBlank()) {
        connectedServers(input, summary.servers)
    }
}

@Composable
private fun homeSearchField(query: String, onQueryChange: (String) -> Unit) {
    val theme = LocalOpenCodeTheme.current
    val searchDescription = stringResource(R.string.home_search_accessibility)
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.labelMedium.copy(
            color = theme.text,
            fontFamily = FontFamily.Monospace,
        ),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(theme.primary),
        modifier = Modifier
            .fillMaxWidth()
            .height(Sizing.textFieldHeightSm)
            .background(theme.backgroundPanel, RectangleShape)
            .border(Sizing.strokeMd, theme.borderSubtle, RectangleShape)
            .semantics { contentDescription = searchDescription }
            .testTag("home_search_field"),
        decorationBox = { field ->
            Row(
                Modifier.fillMaxSize().padding(horizontal = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "/",
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                    color = theme.primary,
                )
                Spacer(Modifier.width(Spacing.xs))
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    field()
                    if (query.isEmpty()) {
                        Text(
                            "Search sessions and workspaces",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                            color = theme.textMuted,
                            maxLines = 1,
                        )
                    }
                }
            }
        },
    )
}

private val HomeSessions: LazyListScope.(HomeOverviewInput, List<SessionPreview>) -> Unit = { input, sessions ->
    item { sectionLabel("Sessions · ${sessions.size}") }
    if (sessions.isEmpty()) {
        item {
            infoCard(
                if (input.searchQuery.isNotBlank()) "No matching sessions" else "No sessions yet",
                if (input.searchQuery.isNotBlank()) "Try another search." else "Use + to start one.",
            )
        }
    } else {
        items(
            items = sessions,
            key = { "${it.serverRef.endpointKey}:${it.sessionId.value}" },
        ) { session ->
            sessionRow(
                session = session,
                onResume = { input.actions.onResumeSession(session) },
                onWorkspace = {
                    input.onWorkspaceClick(
                        WorkspaceSummary(
                            serverRef = session.serverRef,
                            workspaceKey = session.workspaceKey,
                            sessionCount = 0,
                            openTabCount = 0,
                            mostRecentAt = session.updatedAt,
                        ),
                    )
                },
            )
        }
    }
}

private fun LazyListScope.homeWorkspaces(input: HomeOverviewInput, filteredWorkspaces: List<WorkspaceSummary>) {
    val searching = input.searchQuery.isNotBlank()
    val capped = searching || input.showAllWorkspaces
    val visible = if (capped) filteredWorkspaces else filteredWorkspaces.take(HOME_WORKSPACE_SHORTCUT_LIMIT)
    item { sectionLabel(if (searching) "Workspaces" else "Recent workspaces") }
    if (filteredWorkspaces.isEmpty()) {
        item {
            infoCard(
                when {
                    input.summary.isLoading -> "Looking for workspaces"
                    searching -> "No matching workspaces"
                    else -> "No workspaces yet"
                },
                if (searching) "Try another search." else "Use + to start something new.",
            )
        }
        return
    }
    item { dividedRows(visible) { workspace -> workspaceShortcut(workspace, input.onWorkspaceClick) } }
    val hidden = filteredWorkspaces.size - visible.size
    if (!searching && (hidden > 0 || input.showAllWorkspaces)) {
        item {
            homeMoreRow(
                label = if (input.showAllWorkspaces) "− show less" else "+ $hidden more",
                onClick = input.onToggleAllWorkspaces,
                testTag = "home_workspaces_toggle",
            )
        }
    }
}

/** Inline `+ N more` / `− show less` affordance under a capped Home list. */
@Composable
private fun homeMoreRow(label: String, onClick: () -> Unit, testTag: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Sizing.minTouchTarget)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = Spacing.sm)
            .testTag(testTag),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
            color = LocalOpenCodeTheme.current.accent,
        )
    }
}

@Composable
private fun homeHeader(summary: HomeSummaryState, actions: HomeActions) {
    val theme = LocalOpenCodeTheme.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
            Text(
                "Home",
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                color = theme.text,
            )
            Text(
                stringResource(R.string.home_summary_counts, summary.sessions.size, summary.workspaces.size),
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = theme.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        homeHeaderIconButton(Icons.Default.Refresh, "Refresh", actions.onRefresh, "home_refresh")
        homeHeaderIconButton(Icons.Default.Add, "Add server", actions.onManageServers, "home_add_server")
        homeHeaderIconButton(Icons.Default.Settings, "Settings", actions.onSettings, "home_settings")
    }
}

@Composable
private fun homeHeaderIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    testTag: String,
) {
    val theme = LocalOpenCodeTheme.current
    Surface(
        onClick = onClick,
        shape = RectangleShape,
        color = theme.background,
        modifier = Modifier
            .size(Sizing.minTouchTarget)
            .semantics { contentDescription = description }
            .testTag(testTag),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = theme.textMuted, modifier = Modifier.size(Sizing.iconMd))
        }
    }
}

private fun LazyListScope.connectedServers(
    input: HomeOverviewInput,
    servers: List<ServerSummary>,
) {
    if (servers.isEmpty()) return
    item { sectionLabel("Servers") }
    items(servers, key = { "server_${it.serverRef.endpointKey}" }) { server ->
        serverLauncherRow(server, input.actions.onManageServers)
    }
}

@Composable
private fun serverLauncherRow(server: ServerSummary, onOpen: () -> Unit) {
    val theme = LocalOpenCodeTheme.current
    val statusColor = connectionStatusColor(server.connectionState)
    val statusLabel = connectionStatusDescription(server.connectionState)
    Surface(
        onClick = onOpen,
        shape = RectangleShape,
        color = theme.backgroundPanel,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_connected_server_${server.serverRef.endpointKey}"),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = Sizing.minTouchTarget)
                .padding(horizontal = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Box(
                Modifier.size(Sizing.indicatorDot)
                    .background(statusColor, RectangleShape)
                    .semantics { contentDescription = statusLabel },
            )
            Text(
                server.displayName,
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                color = theme.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                "${server.sessionCount} sessions",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = theme.textMuted,
            )
            Text("›", style = MaterialTheme.typography.labelMedium, color = theme.border)
        }
    }
}

/** Colored project pill (dark text on a per-project color), shared by rows and the tree. */
@Composable
private fun projectPill(workspace: WorkspaceSummary) {
    val key = "${workspace.serverRef.endpointKey}:${workspace.workspaceKey}"
    Surface(shape = RectangleShape, color = ProjectColors.colorForProject(key)) {
        Text(
            workspace.workspaceKey.displayLabel(),
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
            color = ProjectColors.textColorForProject(key),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .widthIn(max = Sizing.chipMaxWidth)
                .padding(horizontal = Spacing.sm, vertical = Spacing.xxs),
        )
    }
}

/** Renders [items] as a single continuous panel, each row separated by a thin divider. */
@Composable
private fun <T> dividedRows(items: List<T>, row: @Composable (T) -> Unit) {
    val theme = LocalOpenCodeTheme.current
    Column(Modifier.fillMaxWidth()) {
        items.forEachIndexed { index, value ->
            row(value)
            if (index != items.lastIndex) {
                HorizontalDivider(thickness = Sizing.strokeThin, color = theme.borderSubtle)
            }
        }
    }
}

@Composable
private fun workspaceShortcut(
    workspace: WorkspaceSummary,
    onOpen: (WorkspaceSummary) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    val theme = LocalOpenCodeTheme.current
    Surface(
        onClick = { onOpen(workspace) },
        shape = RectangleShape,
        color = theme.backgroundPanel,
        modifier = modifier,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(Sizing.listItemHeightMd)
                .padding(horizontal = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            projectPill(workspace)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                Text(
                    "${workspace.sessionCount} ${if (workspace.sessionCount == 1) "session" else "sessions"}" +
                        " · ${recency(workspace.mostRecentAt)}",
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                    color = theme.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${workspace.serverRef.displayName} · ${workspace.workspaceKey.detailLabel()}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = theme.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text("›", style = MaterialTheme.typography.labelMedium, color = theme.border)
        }
    }
}

@Composable
@Suppress("LongMethod")
private fun sessionRow(
    session: SessionPreview,
    onResume: () -> Unit,
    onWorkspace: (() -> Unit)? = null,
) {
    val theme = LocalOpenCodeTheme.current
    Surface(
        onClick = onResume,
        shape = RectangleShape,
        color = theme.backgroundElement,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .width(Sizing.strokeThick)
                    .fillMaxHeight()
                    .background(ProjectColors.colorForProject("server:${session.serverRef.endpointKey}")),
            )
            Row(
                Modifier.weight(1f).padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                    Text(
                        session.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = theme.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "● ${session.status.label()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = session.status.statusColor(theme),
                        )
                        Text(
                            recency(session.updatedAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = theme.textMuted,
                        )
                        if (session.childCount > 0) {
                            Text(
                                "[${session.childCount} sub]",
                                style = MaterialTheme.typography.labelSmall,
                                color = theme.info,
                            )
                        }
                        if (session.additions > 0) {
                            Text(
                                "+${session.additions}",
                                style = MaterialTheme.typography.labelSmall,
                                color = theme.success,
                            )
                        }
                        if (session.deletions > 0) {
                            Text(
                                "-${session.deletions}",
                                style = MaterialTheme.typography.labelSmall,
                                color = theme.error,
                            )
                        }
                        if (session.isShared) {
                            Text(
                                stringResource(R.string.home_shared_badge),
                                style = MaterialTheme.typography.labelSmall,
                                color = theme.info,
                            )
                        }
                    }
                }
                SessionWorkspaceLabel(session, onWorkspace)
            }
        }
    }
}

private val SessionWorkspaceLabel: @Composable (SessionPreview, (() -> Unit)?) -> Unit = { session, onWorkspace ->
    val projectKey = "${session.serverRef.endpointKey}:${session.workspaceKey}"
    Surface(
        shape = RectangleShape,
        color = ProjectColors.colorForProject(projectKey),
        modifier = Modifier
            .widthIn(max = Sizing.chipMaxWidth)
            .then(
                if (onWorkspace == null) Modifier else Modifier.clickable(role = Role.Button, onClick = onWorkspace),
            ),
    ) {
        Text(
            session.workspaceKey.displayLabel(),
            style = MaterialTheme.typography.labelSmall,
            color = ProjectColors.textColorForProject(projectKey),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        )
    }
}

private fun SessionPresence.statusColor(
    theme: dev.blazelight.p4oc.ui.theme.opencode.OpenCodeTheme,
) = when (this) {
    SessionPresence.ERROR, SessionPresence.RETRYING -> theme.error
    SessionPresence.AWAITING_INPUT -> theme.warning
    SessionPresence.BUSY, SessionPresence.UNREAD -> theme.success
    SessionPresence.IDLE, SessionPresence.BACKGROUND -> theme.textMuted
}

@Composable
private fun workspaceDetail(
    input: WorkspaceDetailInput,
    modifier: Modifier,
) {
    val theme = LocalOpenCodeTheme.current
    val workspace = input.workspace
    val target = StartWorkTarget(workspace.serverRef, workspace.workspaceKey)
    Column(modifier.fillMaxSize().testTag("home_workspace_detail")) {
        workspaceDetailHeader(input)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item { workspaceDetailCard(workspace) }
            item { sectionLabel("Open work") }
            workspaceOpenWork(input)
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    sectionLabel("Sessions")
                    Box(
                        modifier = Modifier
                            .widthIn(min = Sizing.minTouchTarget)
                            .heightIn(min = Sizing.minTouchTarget)
                            .clickable(role = Role.Button) { input.actions.onStartScopedWork(target) }
                            .testTag("home_workspace_detail_new"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "+ new",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = theme.accent,
                        )
                    }
                }
            }
            workspaceSessions(input)
            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}

@Composable
private fun workspaceDetailHeader(input: WorkspaceDetailInput) {
    val theme = LocalOpenCodeTheme.current
    val statusColor = connectionStatusColor(input.connectionState)
    val statusLabel = connectionStatusDescription(input.connectionState)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        TuiBackButton(
            onClick = input.onBack,
            description = "Back to Home",
            modifier = Modifier.testTag("home_workspace_detail_back"),
        )
        Box(
            Modifier
                .size(Sizing.indicatorDotActive)
                .background(statusColor, RectangleShape)
                .semantics { contentDescription = statusLabel },
        )
        Text(
            input.workspace.workspaceKey.displayLabel(),
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
            color = theme.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .size(Sizing.minTouchTarget)
                .clickable(role = Role.Button, onClick = input.actions.onRefresh)
                .semantics { contentDescription = "Refresh" }
                .testTag("home_workspace_detail_refresh"),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = null,
                tint = theme.textMuted,
                modifier = Modifier.size(Sizing.iconMd),
            )
        }
    }
}

@Composable
private fun workspaceDetailCard(workspace: WorkspaceSummary) {
    val theme = LocalOpenCodeTheme.current
    Surface(shape = RectangleShape, color = theme.backgroundElement, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                projectPill(workspace)
                Text(
                    "${workspace.serverRef.badgeLabel} · ${workspace.serverRef.displayName}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = theme.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                workspace.workspaceKey.detailLabel(),
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = theme.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun LazyListScope.workspaceOpenWork(input: WorkspaceDetailInput) {
    if (input.openWork.isEmpty()) {
        item { dashedEmptyCard("Nothing open", "Tabs for this workspace show up here") }
    } else {
        items(input.openWork, key = { it.tabId }) { work ->
            openWorkCard(work) { input.actions.onFocusTab(work.tabId) }
        }
    }
}

private fun LazyListScope.workspaceSessions(input: WorkspaceDetailInput) {
    if (input.sessions.isEmpty()) {
        item { dashedEmptyCard("No sessions", "Start one with + new") }
    } else {
        items(input.sessions, key = { it.sessionId.value }) { session ->
            workspaceSessionRow(session) { input.actions.onResumeSession(session) }
        }
    }
}

@Composable
private fun workspaceSessionRow(session: SessionPreview, onResume: () -> Unit) {
    val theme = LocalOpenCodeTheme.current
    Surface(
        onClick = onResume,
        shape = RectangleShape,
        color = theme.backgroundElement,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            SessionStatusDot(session.status, size = Sizing.indicatorDotActive)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                Text(
                    session.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = theme.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${session.status.label()} · ${recency(session.updatedAt)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = theme.textMuted,
                    maxLines = 1,
                )
            }
            SessionWorkspaceLabel(session, null)
        }
    }
}

@Composable
private fun dashedEmptyCard(title: String, body: String) {
    val theme = LocalOpenCodeTheme.current
    Row(
        Modifier
            .fillMaxWidth()
            .dashedBorder(theme.borderSubtle)
            .padding(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text("⌀", style = MaterialTheme.typography.bodyMedium, color = theme.textMuted)
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = theme.textMuted)
            Text(body, style = MaterialTheme.typography.labelSmall, color = theme.textMuted)
        }
    }
}

private fun Modifier.dashedBorder(color: Color): Modifier = drawBehind {
    drawRect(
        color = color,
        style = Stroke(
            width = Sizing.strokeMd.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(DASH_ON, DASH_OFF), 0f),
        ),
    )
}

@Composable
private fun openWorkCard(work: OpenWorkSummary, onFocus: () -> Unit) {
    val icon = when (work.type) {
        OpenWorkType.Chat -> Icons.AutoMirrored.Filled.Chat
        OpenWorkType.Files -> Icons.Default.Folder
        OpenWorkType.Terminal -> Icons.Default.Terminal
    }
    val status = work.status?.label() ?: "Open"
    val theme = LocalOpenCodeTheme.current
    Surface(shape = RectangleShape, color = theme.backgroundElement, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Icon(icon, contentDescription = work.type.name, tint = theme.textMuted)
            Column(Modifier.weight(1f)) {
                Text(work.title, style = MaterialTheme.typography.labelMedium, color = theme.text)
                Text(
                    "${work.type.name} · $status",
                    style = MaterialTheme.typography.bodySmall,
                    color = theme.textMuted,
                )
            }
            compactAction("Focus", onFocus)
        }
    }
}

@Composable
private fun infoCard(title: String, body: String) {
    val theme = LocalOpenCodeTheme.current
    Surface(shape = RectangleShape, color = theme.backgroundElement, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = theme.text)
            Text(body, style = MaterialTheme.typography.bodySmall, color = theme.textMuted)
        }
    }
}

@Composable
private fun compactAction(label: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RectangleShape, color = LocalOpenCodeTheme.current.background) {
        Text(
            label,
            modifier = Modifier.padding(Spacing.xs),
            style = MaterialTheme.typography.labelMedium,
            color = LocalOpenCodeTheme.current.accent
        )
    }
}

@Composable
private fun sectionLabel(text: String) = Text(
    text.uppercase(),
    style = MaterialTheme.typography.labelSmall.copy(
        fontFamily = FontFamily.Monospace,
        letterSpacing = 1.sp,
    ),
    color = LocalOpenCodeTheme.current.textMuted,
)

internal data class FilteredHomeResults(
    val workspaces: List<WorkspaceSummary>,
    val sessions: List<SessionPreview>,
)

/** Query-only Home search: every saved server's workspaces and sessions are searched. */
internal fun HomeSummaryState.filteredHomeResults(
    query: String,
): FilteredHomeResults {
    val needle = query.trim()
    val matchingWorkspaces = ArrayList<WorkspaceSummary>(workspaces.size)
    for (workspace in workspaces) {
        if (needle.isEmpty() || workspace.matchesSearch(needle)) matchingWorkspaces += workspace
    }
    val matchingSessions = ArrayList<SessionPreview>(sessions.size)
    for (session in sessions) {
        if (needle.isEmpty() || session.matchesSearch(needle)) matchingSessions += session
    }
    matchingSessions.sortByDescending { it.updatedAt }
    return FilteredHomeResults(matchingWorkspaces, matchingSessions)
}

private fun WorkspaceSummary.matchesSearch(query: String): Boolean =
    serverRef.displayName.contains(query, ignoreCase = true) ||
        workspaceKey.displayLabel().contains(query, ignoreCase = true) ||
        workspaceKey.detailLabel().contains(query, ignoreCase = true)

private fun SessionPreview.matchesSearch(query: String): Boolean =
    title.contains(query, ignoreCase = true) ||
        directory.contains(query, ignoreCase = true) ||
        serverRef.displayName.contains(query, ignoreCase = true) ||
        workspaceKey.displayLabel().contains(query, ignoreCase = true) ||
        workspaceKey.detailLabel().contains(query, ignoreCase = true)

private fun WorkspaceKey.displayLabel(): String = when (this) {
    WorkspaceKey.Global -> "No project context"
    is WorkspaceKey.Directory -> value.trimEnd('/').substringAfterLast('/').ifBlank { value }
    is WorkspaceKey.SessionScoped -> "Session ${sessionId.value}"
}
private fun WorkspaceKey.detailLabel(): String = when (this) {
    WorkspaceKey.Global -> "No project context"
    is WorkspaceKey.Directory -> value
    is WorkspaceKey.SessionScoped -> "Session-scoped workspace"
}

private fun SessionPresence.label() = name.lowercase().replaceFirstChar { it.uppercase() }
private fun recency(timestamp: Long, now: Long = System.currentTimeMillis()): String {
    if (timestamp <= 0) return "No recent session"
    val elapsed = (now - timestamp).coerceAtLeast(0)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(elapsed)
    val hours = TimeUnit.MILLISECONDS.toHours(elapsed)
    val days = TimeUnit.MILLISECONDS.toDays(elapsed)
    return when {
        minutes < 1 -> "Just now"
        hours < 1 -> "${minutes}m ago"
        days < 1 -> "${hours}h ago"
        days < RECENT_DAY_LIMIT -> "${days}d ago"
        else -> "Older"
    }
}
