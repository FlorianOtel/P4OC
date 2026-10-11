package dev.blazelight.p4oc.ui.tabs

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import dev.blazelight.p4oc.core.log.AppLog
import dev.blazelight.p4oc.core.network.ConnectionState
import dev.blazelight.p4oc.data.session.SessionRepositoryImpl
import dev.blazelight.p4oc.domain.server.ServerGeneration
import dev.blazelight.p4oc.domain.server.ServerRef
import dev.blazelight.p4oc.domain.server.WorkspaceKey
import dev.blazelight.p4oc.domain.workspace.Workspace
import dev.blazelight.p4oc.ui.navigation.Screen
import dev.blazelight.p4oc.ui.screens.home.HomeSessionActions
import dev.blazelight.p4oc.ui.screens.home.SessionPreview
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val TAG = "HomeSessionActionRouter"

/** Where a Home session action runs: the session's own server generation and workspace. */
internal sealed interface HomeSessionRoute {
    data class Ready(val workspace: Workspace, val generation: ServerGeneration) : HomeSessionRoute
    data class Unavailable(val message: String) : HomeSessionRoute
}

internal sealed interface HomeSessionActionResult<out T> {
    data class Success<T>(val value: T) : HomeSessionActionResult<T>
    data class Failure(val message: String) : HomeSessionActionResult<Nothing>
}

/**
 * Resolves a Home session to its own workspace on its own server. The workspace comes only from
 * the session's [SessionPreview.workspaceKey]; there is no fallback to the active tab or any other
 * workspace.
 */
internal fun resolveHomeSessionRoute(
    session: SessionPreview,
    connectionState: ConnectionState,
    generation: ServerGeneration?,
): HomeSessionRoute {
    val serverName = session.serverRef.displayName
    if (connectionState !is ConnectionState.Connected || generation == null) {
        return HomeSessionRoute.Unavailable("$serverName is not connected. Reconnect it to manage this session.")
    }
    val server = session.serverRef
    return when (val key = session.workspaceKey) {
        is WorkspaceKey.Directory -> HomeSessionRoute.Ready(Workspace(server, directory = key.value), generation)
        WorkspaceKey.Global -> HomeSessionRoute.Ready(Workspace(server, directory = null), generation)
        is WorkspaceKey.SessionScoped -> HomeSessionRoute.Unavailable(
            "This session's workspace is not available on $serverName. Open the session to manage it.",
        )
    }
}

/**
 * Runs Home session actions against the repository of the session's own server generation and
 * workspace. The repository is leased for the duration of the action, so an already-open owner for
 * that workspace is shared rather than duplicated. Other Home listings on the same server that show
 * the session are refreshed afterwards, because they only receive live events for their own
 * workspace.
 */
internal class HomeSessionActionRouter(
    private val connectionState: (ServerRef) -> ConnectionState,
    private val generation: (ServerRef) -> ServerGeneration?,
    private val acquireRepository: (Workspace, ServerGeneration) -> SessionRepositoryImpl,
    private val releaseRepository: (Workspace, ServerGeneration) -> Unit,
    private val homeRepositories: (ServerRef) -> List<SessionRepositoryImpl>,
    private val refreshScope: CoroutineScope,
) {
    fun resolve(session: SessionPreview): HomeSessionRoute = resolveHomeSessionRoute(
        session = session,
        connectionState = connectionState(session.serverRef),
        generation = generation(session.serverRef),
    )

    /** Sharing is a server capability, so any Home listing repository of that server answers it. */
    fun supportsSharing(session: SessionPreview): Boolean =
        homeRepositories(session.serverRef).firstOrNull()?.supportsSessionSharing ?: false

    @Suppress("TooGenericExceptionCaught") // Any action failure becomes a readable message, never a crash.
    suspend fun <T> run(
        session: SessionPreview,
        failureMessage: String,
        action: suspend (SessionRepositoryImpl) -> T,
    ): HomeSessionActionResult<T> {
        val route = when (val resolved = resolve(session)) {
            is HomeSessionRoute.Unavailable -> return HomeSessionActionResult.Failure(resolved.message)
            is HomeSessionRoute.Ready -> resolved
        }
        val repository = acquireRepository(route.workspace, route.generation)
        return try {
            val value = action(repository)
            refreshOtherListings(session, repository)
            HomeSessionActionResult.Success(value)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLog.e(TAG, "Home session action failed: ${e.javaClass.simpleName}")
            HomeSessionActionResult.Failure(failureMessage)
        } finally {
            releaseRepository(route.workspace, route.generation)
        }
    }

    @Suppress("TooGenericExceptionCaught") // A failed refresh leaves the listing stale, never crashes Home.
    private fun refreshOtherListings(session: SessionPreview, actedOn: SessionRepositoryImpl) {
        val sessionId = session.sessionId.value
        homeRepositories(session.serverRef)
            .distinct()
            .filter { it !== actedOn && it.state.value.snapshot.sessions.containsKey(sessionId) }
            .forEach { listing ->
                refreshScope.launch {
                    try {
                        listing.refresh()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        AppLog.w(TAG, "Home listing refresh failed: ${e.javaClass.simpleName}")
                    }
                }
            }
    }
}

/** Focuses the tab already showing [route] for this server and workspace, or opens one. */
internal fun TabManager.focusOrOpenWorkspaceRoute(
    serverRef: ServerRef,
    workspaceKey: WorkspaceKey,
    route: String,
) {
    val existing = tabs.value.firstOrNull {
        !it.isPinnedHome &&
            it.serverRef?.endpointKey == serverRef.endpointKey &&
            it.workspaceKey == workspaceKey &&
            it.startRoute == route
    }
    if (existing != null) {
        focusTab(existing.id)
    } else {
        createTab(startRoute = route, workspaceKey = workspaceKey, serverRef = serverRef, focus = true)
    }
}

/** Focuses the tab already showing this session in its own workspace, or opens a chat tab for it. */
internal fun TabManager.focusOrOpenSessionTab(
    serverRef: ServerRef,
    workspaceKey: WorkspaceKey,
    sessionId: String,
) {
    val existing = findSessionTab(serverRef = serverRef, workspaceKey = workspaceKey, sessionId = sessionId)
    if (existing != null) {
        focusTab(existing.id)
    } else {
        createTab(
            startRoute = Screen.Chat.createRoute(sessionId),
            workspaceKey = workspaceKey,
            serverRef = serverRef,
            focus = true,
        )
    }
}

private fun SessionPreview.actionKey(): String = "${serverRef.endpointKey}:${sessionId.value}"

private fun <T> HomeSessionActionResult<T>.reportFailure(showMessage: (String) -> Unit) {
    if (this is HomeSessionActionResult.Failure) showMessage(message)
}

/**
 * Home session row actions. Every action is routed by [router] to the session's own server and
 * workspace; fork, view changes and share follow the Sessions screen behavior.
 */
@Composable
@Suppress("LongMethod")
internal fun rememberHomeSessionActions(
    router: HomeSessionActionRouter,
    tabManager: TabManager,
    scope: CoroutineScope,
    showMessage: (String) -> Unit,
): HomeSessionActions {
    val context = LocalContext.current
    var forkingKeys by remember { mutableStateOf(emptySet<String>()) }
    return HomeSessionActions(
        supportsSharing = router::supportsSharing,
        isForking = { session -> session.actionKey() in forkingKeys },
        onRename = { session, newTitle ->
            scope.launch {
                router.run(session, "Could not rename the session. Try again.") { repository ->
                    repository.renameSession(session.sessionId, newTitle)
                }.reportFailure(showMessage)
            }
        },
        onDelete = { session ->
            scope.launch {
                router.run(session, "Could not delete the session. Try again.") { repository ->
                    repository.deleteSession(session.sessionId)
                }.reportFailure(showMessage)
            }
        },
        onFork = { session ->
            val key = session.actionKey()
            if (key !in forkingKeys) {
                forkingKeys = forkingKeys + key
                scope.launch {
                    try {
                        forkHomeSession(router, tabManager, session, showMessage)
                    } finally {
                        forkingKeys = forkingKeys - key
                    }
                }
            }
        },
        onToggleShare = { session ->
            scope.launch { toggleHomeSessionShare(router, session, context, showMessage) }
        },
        onViewChanges = { session ->
            when (val route = router.resolve(session)) {
                is HomeSessionRoute.Unavailable -> showMessage(route.message)
                is HomeSessionRoute.Ready -> tabManager.focusOrOpenWorkspaceRoute(
                    serverRef = session.serverRef,
                    workspaceKey = route.workspace.key,
                    route = Screen.SessionDiff.createRoute(session.sessionId.value),
                )
            }
        },
        onSummarize = { session ->
            scope.launch {
                router.run(session, "Could not summarize the session. Try again.") { repository ->
                    repository.summarizeSession(session.sessionId)
                }.reportFailure(showMessage)
            }
        },
    )
}

private suspend fun forkHomeSession(
    router: HomeSessionActionRouter,
    tabManager: TabManager,
    session: SessionPreview,
    showMessage: (String) -> Unit,
) {
    val result = router.run(session, "Could not fork the session. Try again.") { repository ->
        repository.forkSession(session.sessionId)
    }
    when (result) {
        is HomeSessionActionResult.Success -> tabManager.focusOrOpenSessionTab(
            serverRef = session.serverRef,
            workspaceKey = result.value.workspace.key,
            sessionId = result.value.id.value,
        )
        is HomeSessionActionResult.Failure -> showMessage(result.message)
    }
}

private suspend fun toggleHomeSessionShare(
    router: HomeSessionActionRouter,
    session: SessionPreview,
    context: Context,
    showMessage: (String) -> Unit,
) {
    if (session.isShared) {
        router.run(session, "Could not stop sharing the session. Try again.") { repository ->
            repository.unshareSession(session.sessionId)
        }.reportFailure(showMessage)
        return
    }
    val result = router.run(session, "Could not share the session. Try again.") { repository ->
        repository.shareSession(session.sessionId).session.shareUrl
    }
    when (result) {
        is HomeSessionActionResult.Success -> result.value?.let { shareUrl ->
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareUrl)
            }
            context.startActivity(Intent.createChooser(intent, null))
        }
        is HomeSessionActionResult.Failure -> showMessage(result.message)
    }
}
