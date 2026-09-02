package dev.blazelight.p4oc.ui.tabs

import dev.blazelight.p4oc.domain.server.ServerRef
import dev.blazelight.p4oc.domain.server.WorkspaceKey
import dev.blazelight.p4oc.ui.screens.home.ScopedHomeRepositoryState

data class StartWorkTarget(
    val serverRef: ServerRef,
    val workspaceKey: WorkspaceKey,
)

sealed interface StartWorkSelection {
    data class Selected(val target: StartWorkTarget) : StartWorkSelection
    data object NeedsSelection : StartWorkSelection
}

enum class StartWorkConnectionState {
    Online,
    Offline,
    AuthRequired,
}

enum class StartWorkAvailability {
    Ready,
    NoServers,
    ServerRemoved,
    WorkspaceMissing,
    Offline,
    AuthRequired,
}

data class StartWorkResolvedContext(
    val context: StartWorkContext,
    val target: StartWorkTarget?,
    val pendingAction: StartWorkAction?,
    val availability: StartWorkAvailability,
)

data class StartWorkContext(
    val source: StartWorkSource,
    val selection: StartWorkSelection,
    val defaultAction: StartWorkAction? = null,
) {
    val selectedTarget: StartWorkTarget?
        get() = (selection as? StartWorkSelection.Selected)?.target
}

fun StartWorkContext.resolve(
    availableServers: Collection<ServerRef>,
    availableWorkspaces: Collection<StartWorkTarget>,
    connectionStates: Map<String, StartWorkConnectionState>,
): StartWorkResolvedContext {
    val target = selectedTarget
    val availability = when {
        availableServers.isEmpty() -> StartWorkAvailability.NoServers
        target == null -> StartWorkAvailability.Ready
        availableServers.none { it.endpointKey == target.serverRef.endpointKey } ->
            StartWorkAvailability.ServerRemoved
        target.workspaceKey != WorkspaceKey.Global && target !in availableWorkspaces ->
            StartWorkAvailability.WorkspaceMissing
        connectionStates[target.serverRef.endpointKey] == StartWorkConnectionState.AuthRequired ->
            StartWorkAvailability.AuthRequired
        connectionStates[target.serverRef.endpointKey] != StartWorkConnectionState.Online ->
            StartWorkAvailability.Offline
        else -> StartWorkAvailability.Ready
    }
    return StartWorkResolvedContext(
        context = this,
        target = target,
        pendingAction = defaultAction,
        availability = availability,
    )
}

enum class StartWorkSource {
    HomeTopLevel,
    HomeWorkspaceDetail,
    ChatTab,
    FilesTab,
    TerminalTab,
    OtherTab,
}

enum class StartWorkAction {
    NewChat,
    Files,
    Terminal,
    BrowseSessions,
    ChooseAnotherTarget,
}

fun startWorkContextFor(tab: TabInstance?): StartWorkContext {
    if (tab == null || tab.isPinnedHome) {
        return StartWorkContext(
            source = StartWorkSource.HomeTopLevel,
            selection = StartWorkSelection.NeedsSelection,
            defaultAction = StartWorkAction.ChooseAnotherTarget,
        )
    }
    val serverRef = tab.serverRef
    val workspaceKey = tab.workspaceKey
    return StartWorkContext(
        source = sourceForRoute(tab.startRoute),
        selection = if (serverRef != null && workspaceKey != null) {
            StartWorkSelection.Selected(StartWorkTarget(serverRef, workspaceKey))
        } else {
            StartWorkSelection.NeedsSelection
        },
        defaultAction = if (serverRef == null || workspaceKey == null) {
            StartWorkAction.ChooseAnotherTarget
        } else {
            null
        },
    )
}

fun startWorkContextForHomeDetail(target: StartWorkTarget): StartWorkContext = StartWorkContext(
    source = StartWorkSource.HomeWorkspaceDetail,
    selection = StartWorkSelection.Selected(target),
)

internal fun deriveStartWorkPickerTargets(
    repositories: List<ScopedHomeRepositoryState>,
): List<StartWorkTarget> = repositories.flatMap { repository ->
    repository.state.snapshot.sessions.values.map { session ->
        StartWorkTarget(repository.serverRef, session.workspace.key)
    }
}.distinct()

/** Workspaces listed per server before the "show more" row takes over. */
internal const val PICKER_WORKSPACE_PAGE_SIZE = 10

/** Workspaces of one server that match [query]; an empty query keeps every entry. */
internal fun StartWorkPickerGroup.matchingTargets(query: String): List<StartWorkTarget> {
    val needle = query.trim()
    if (needle.isEmpty()) return targets
    return targets.filter { target ->
        target.workspaceKey.pickerSearchText().contains(needle, ignoreCase = true)
    }
}

/** One server section of the picker: its header state plus the workspace rows to draw under it. */
internal data class StartWorkPickerRow(
    val group: StartWorkPickerGroup,
    val expanded: Boolean,
    val matchCount: Int,
    val visibleTargets: List<StartWorkTarget>,
    val hiddenCount: Int,
)

internal data class StartWorkPickerViewState(
    val query: String = "",
    /** Explicit expand/collapse the user has toggled, keyed by endpoint. */
    val expandedOverrides: Map<String, Boolean> = emptyMap(),
    /** Endpoints where the user asked to see past [PICKER_WORKSPACE_PAGE_SIZE] workspaces. */
    val showAllEndpointKeys: Set<String> = emptySet(),
    /** Server expanded by default — normally the one the picker was opened from. */
    val defaultExpandedEndpointKey: String? = null,
)

/**
 * Folds [groups] into the picker's server sections. While searching, non-matching servers drop out
 * and matching ones open regardless of collapse state, so results are never hidden behind a header.
 */
internal fun buildStartWorkPickerRows(
    groups: List<StartWorkPickerGroup>,
    state: StartWorkPickerViewState,
): List<StartWorkPickerRow> {
    val searching = state.query.isNotBlank()
    val onlyServer = groups.size == 1
    return groups.mapNotNull { group ->
        val endpointKey = group.server.endpointKey
        val matches = group.matchingTargets(state.query)
        if (searching && matches.isEmpty()) return@mapNotNull null
        val expanded = when {
            searching -> true
            else -> state.expandedOverrides[endpointKey]
                ?: (onlyServer || endpointKey == state.defaultExpandedEndpointKey)
        }
        val showAll = searching || endpointKey in state.showAllEndpointKeys
        val visible = when {
            !expanded -> emptyList()
            showAll -> matches
            else -> matches.take(PICKER_WORKSPACE_PAGE_SIZE)
        }
        StartWorkPickerRow(
            group = group,
            expanded = expanded,
            matchCount = matches.size,
            visibleTargets = visible,
            hiddenCount = if (expanded) matches.size - visible.size else 0,
        )
    }
}

private fun WorkspaceKey.pickerSearchText(): String = when (this) {
    is WorkspaceKey.Directory -> "$value ${value.trimEnd('/').substringAfterLast('/')}"
    WorkspaceKey.Global -> "global server root no directory"
    is WorkspaceKey.SessionScoped -> sessionId.value
}

private fun sourceForRoute(route: String): StartWorkSource = when {
    route.startsWith("chat/") -> StartWorkSource.ChatTab
    route.startsWith("files") -> StartWorkSource.FilesTab
    route.startsWith("terminal/") -> StartWorkSource.TerminalTab
    else -> StartWorkSource.OtherTab
}
