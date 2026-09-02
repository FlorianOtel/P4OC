package dev.blazelight.p4oc.ui.tabs

import dev.blazelight.p4oc.domain.model.SessionConnectionState
import dev.blazelight.p4oc.domain.server.ServerRef
import dev.blazelight.p4oc.domain.server.WorkspaceKey
import dev.blazelight.p4oc.ui.navigation.Screen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Represents a single tab in the top-level tab system.
 *
 * Each tab has its own navigation stack (NavController created inside the pager page)
 * allowing independent navigation within each tab. Tabs are generic containers -
 * any screen type can be shown in any tab.
 */
data class TabState(
    /** Unique identifier for this tab */
    val id: String = UUID.randomUUID().toString(),

    /**
     * Session ID if this tab is currently showing a chat session.
     * Used for uniqueness check - only one tab can show a given session.
     * Null if showing non-chat content (Sessions list, Files, Terminal, Settings).
     */
    val sessionId: String? = null,

    /**
     * Optional session title for display in tab bar.
     * Only populated when sessionId is set.
     */
    val sessionTitle: String? = null,

    /** Workspace key owned by this tab. Null is reserved for legacy recovery. */
    val workspaceKey: WorkspaceKey? = null,

    /** Server endpoint owned by this tab. Null is allowed only for explicitly global surfaces. */
    val serverRef: ServerRef? = null,

    /** Pinned Home is global, leftmost, non-closeable, and not a work tab. */
    val pinnedHome: Boolean = false,
    /** Incremented when workspace changes so navigation graph scoped ViewModels are recreated. */
    val workspaceRevision: Int = 0,
)

/**
 * Wrapper that holds TabState. NavController is created inside the HorizontalPager
 * page composition scope, not stored here (to avoid ViewModelStore lifecycle crashes).
 */
class TabInstance(

    val state: TabState,
    /** Declarative start route for this tab's NavHost. Defaults to Sessions list. */
    val startRoute: String = "sessions"
) {
    val id: String get() = state.id
    val sessionId: String? get() = state.sessionId
    val sessionTitle: String? get() = state.sessionTitle
    val workspaceKey: WorkspaceKey? get() = state.workspaceKey
    val serverRef: ServerRef? get() = state.serverRef
    val serverEndpointKey: String? get() = state.serverRef?.endpointKey
    val workspaceDirectory: String? get() = (state.workspaceKey as? WorkspaceKey.Directory)?.value
    val workspaceRevision: Int get() = state.workspaceRevision

    /** Connection state for this tab (only relevant for chat tabs) */
    private val _connectionState = MutableStateFlow<SessionConnectionState?>(null)
    val isPinnedHome: Boolean get() = state.pinnedHome
    val connectionState: StateFlow<SessionConnectionState?> = _connectionState.asStateFlow()

    /** Update the connection state for this tab */
    fun updateConnectionState(state: SessionConnectionState?) {
        _connectionState.value = state
    }

    fun withState(newState: TabState): TabInstance {
        return TabInstance(newState, startRoute).also {
            it._connectionState.value = this._connectionState.value
        }
    }

    fun withSessionId(sessionId: String?, sessionTitle: String? = null): TabInstance {
        return withState(state.copy(sessionId = sessionId, sessionTitle = sessionTitle))
    }

    fun withServerRef(serverRef: ServerRef?): TabInstance {
        return withState(state.copy(serverRef = serverRef))
    }

    fun withWorkspaceKey(workspaceKey: WorkspaceKey?): TabInstance {
        if (workspaceKey == state.workspaceKey) return this
        return withState(
            state.copy(
                workspaceKey = workspaceKey,
                workspaceRevision = state.workspaceRevision + 1,
                sessionId = null,
                sessionTitle = null,
            ),
        )
    }
    companion object {
        const val HOME_TAB_ID = "pinned-home"

        fun home(): TabInstance = TabInstance(
            TabState(id = HOME_TAB_ID, workspaceKey = WorkspaceKey.Global, pinnedHome = true),
            startRoute = Screen.Home.route,
        )
    }
}
