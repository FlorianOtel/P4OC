package dev.blazelight.p4oc.ui.screens.home

import dev.blazelight.p4oc.core.datastore.SavedServer
import dev.blazelight.p4oc.core.network.ConnectionState
import dev.blazelight.p4oc.data.session.RepoState
import dev.blazelight.p4oc.domain.model.SessionPresence
import dev.blazelight.p4oc.domain.model.resolveSessionPresence
import dev.blazelight.p4oc.domain.server.ServerRef
import dev.blazelight.p4oc.domain.server.WorkspaceKey
import dev.blazelight.p4oc.domain.session.SessionId
import dev.blazelight.p4oc.ui.tabs.TabInstance

/** A repository snapshot whose immutable owner is known by Home. */
data class ScopedHomeRepositoryState(
    val serverRef: ServerRef,
    val state: RepoState,
)

data class ServerSummary(
    val serverRef: ServerRef,
    val displayName: String,
    val connectionState: ConnectionState,
    val sessionCount: Int,
    val openTabCount: Int,
    val isLoading: Boolean,
    val failure: String? = null,
)

data class WorkspaceSummary(
    val serverRef: ServerRef,
    val workspaceKey: WorkspaceKey,
    val sessionCount: Int,
    val openTabCount: Int,
    val mostRecentAt: Long,
)

enum class OpenWorkType { Chat, Files, Terminal }

data class OpenWorkSummary(
    val tabId: String,
    val serverRef: ServerRef,
    val workspaceKey: WorkspaceKey,
    val type: OpenWorkType,
    val title: String,
    val status: SessionPresence? = null,
)

data class SessionPreview(
    val sessionId: SessionId,
    val serverRef: ServerRef,
    val workspaceKey: WorkspaceKey,
    val title: String,
    val updatedAt: Long,
    val status: SessionPresence,
    val directory: String,
    val childCount: Int,
    val additions: Int,
    val deletions: Int,
    val isShared: Boolean,
)

data class HomeSummaryState(
    val servers: List<ServerSummary>,
    val workspaces: List<WorkspaceSummary>,
    val openWork: List<OpenWorkSummary>,
    val sessions: List<SessionPreview>,
    val isLoading: Boolean,
    val partialFailures: List<String> = emptyList(),
)

data class HomeSummaryInput(
    val savedServers: List<SavedServer>,
    val connectionStates: Map<String, ConnectionState>,
    val tabs: List<TabInstance>,
    val repositories: List<ScopedHomeRepositoryState> = emptyList(),
    val workspaceLimit: Int = 12,
    val openWorkLimit: Int = 24,
)

object HomeSummaryBuilder {
    fun build(input: HomeSummaryInput): HomeSummaryState {
        val workTabs = input.tabs.filterNot { it.isPinnedHome }
        val failures = mutableListOf<String>()
        val sessions = buildSessionPreviews(input.repositories)
        val serverSummaries = buildServerSummaries(input, sessions, workTabs, failures)
        appendRepositoryFailures(input.repositories, failures)
        val openWork = buildOpenWork(workTabs, sessions, input.openWorkLimit)
        val workspaces = buildWorkspaces(sessions, openWork, input.workspaceLimit)

        return HomeSummaryState(
            servers = serverSummaries,
            workspaces = workspaces,
            openWork = openWork,
            sessions = sessions,
            isLoading = input.repositories.any { it.state is RepoState.Hydrating },
            partialFailures = failures.distinct(),
        )
    }
}

private fun buildSessionPreviews(repositories: List<ScopedHomeRepositoryState>): List<SessionPreview> =
    repositories.flatMap { scoped ->
        val sessions = scoped.state.snapshot.sessions.values
        val childCounts = sessions.mapNotNull { it.session.parentID }
            .groupingBy { it }
            .eachCount()
        sessions.map { workspaceSession ->
            val session = workspaceSession.session
            SessionPreview(
                sessionId = workspaceSession.id,
                serverRef = scoped.serverRef,
                workspaceKey = workspaceSession.workspace.key,
                title = session.title.ifBlank { "Untitled session" },
                updatedAt = session.updatedAt,
                status = resolveSessionPresence(scoped.state.snapshot.statuses[session.id]),
                directory = session.directory,
                childCount = childCounts[session.id] ?: 0,
                additions = session.summary?.additions ?: 0,
                deletions = session.summary?.deletions ?: 0,
                isShared = session.shareUrl != null,
            )
        }
    }.distinctBy { it.serverRef.endpointKey to it.sessionId.value }
        .sortedByDescending { it.updatedAt }

private fun buildServerSummaries(
    input: HomeSummaryInput,
    sessions: List<SessionPreview>,
    workTabs: List<TabInstance>,
    failures: MutableList<String>,
): List<ServerSummary> {
    val repositoryByServer = input.repositories
        .associateBy { it.serverRef.endpointKey }
    return input.savedServers.mapNotNull { saved ->
        runCatching {
            val repository = repositoryByServer[saved.endpointKey]
            ServerSummary(
                serverRef = ServerRef.fromEndpointKey(saved.endpointKey, saved.displayName),
                displayName = saved.displayName,
                connectionState = input.connectionStates[saved.endpointKey] ?: ConnectionState.Disconnected,
                sessionCount = sessions.count { it.serverRef.endpointKey == saved.endpointKey },
                openTabCount = workTabs.count { it.serverEndpointKey == saved.endpointKey },
                isLoading = repository?.state is RepoState.Hydrating,
                failure = (repository?.state as? RepoState.Stale)?.reason,
            )
        }.getOrElse {
            failures += saved.displayName
            null
        }
    }
}

private fun appendRepositoryFailures(
    repositories: List<ScopedHomeRepositoryState>,
    failures: MutableList<String>,
) {
    repositories.mapNotNullTo(failures) { scoped ->
        (scoped.state as? RepoState.Stale)?.let {
            "${scoped.serverRef.displayName}: session data unavailable"
        }
    }
}

private fun buildOpenWork(
    tabs: List<TabInstance>,
    sessions: List<SessionPreview>,
    limit: Int,
): List<OpenWorkSummary> = tabs.mapNotNull { it.toOpenWorkSummary(sessions) }.take(limit)

private fun TabInstance.toOpenWorkSummary(sessions: List<SessionPreview>): OpenWorkSummary? {
    val ownedServer = serverRef
    val ownedWorkspace = workspaceKey
    val type = openWorkType()
    if (ownedServer == null || ownedWorkspace == null || type == null) return null
    val matchingSession = sessionId?.let { sessionId ->
        sessions.firstOrNull { session ->
            session.sessionId.value == sessionId &&
                session.serverRef.endpointKey == ownedServer.endpointKey &&
                session.workspaceKey == ownedWorkspace
        }
    }
    return OpenWorkSummary(
        tabId = id,
        serverRef = ownedServer,
        workspaceKey = ownedWorkspace,
        type = type,
        title = openWorkTitle(type, matchingSession),
        status = matchingSession?.status,
    )
}

private fun TabInstance.openWorkType(): OpenWorkType? = when {
    sessionId != null || startRoute.startsWith("chat/") -> OpenWorkType.Chat
    startRoute.startsWith("files") -> OpenWorkType.Files
    startRoute.startsWith("terminal/") -> OpenWorkType.Terminal
    else -> null
}

private fun TabInstance.openWorkTitle(
    type: OpenWorkType,
    matchingSession: SessionPreview?,
): String = when (type) {
    OpenWorkType.Chat -> sessionTitle ?: matchingSession?.title ?: "Chat"
    OpenWorkType.Files -> "Files"
    OpenWorkType.Terminal -> "Terminal"
}

private fun buildWorkspaces(
    sessions: List<SessionPreview>,
    openWork: List<OpenWorkSummary>,
    limit: Int,
): List<WorkspaceSummary> {
    val workspaceKeys = (
        sessions.map { it.serverRef to it.workspaceKey } +
            openWork.map { it.serverRef to it.workspaceKey }
        ).distinctBy { it.first.endpointKey to it.second }
    return workspaceKeys.map { (serverRef, workspaceKey) ->
        workspaceSummary(serverRef, workspaceKey, sessions, openWork)
    }.sortedWith(
        compareByDescending<WorkspaceSummary> { it.mostRecentAt }
            .thenBy { it.workspaceKey.toString() },
    ).take(limit)
}

private fun workspaceSummary(
    serverRef: ServerRef,
    workspaceKey: WorkspaceKey,
    sessions: List<SessionPreview>,
    openWork: List<OpenWorkSummary>,
): WorkspaceSummary {
    val scopedSessions = sessions.filter {
        it.serverRef.endpointKey == serverRef.endpointKey && it.workspaceKey == workspaceKey
    }
    return WorkspaceSummary(
        serverRef = serverRef,
        workspaceKey = workspaceKey,
        sessionCount = scopedSessions.size,
        openTabCount = openWork.count {
            it.serverRef.endpointKey == serverRef.endpointKey && it.workspaceKey == workspaceKey
        },
        mostRecentAt = scopedSessions.maxOfOrNull { it.updatedAt } ?: 0L,
    )
}
