package dev.blazelight.p4oc.domain.workspace

/** A workspace root the user added on one server. Identity is the server endpoint plus [path]. */
data class UserWorkspaceRoot(
    val path: ServerPath,
    val addedAt: Long,
)

/** A custom display name for one workspace directory on one server. */
data class StoredWorkspaceAlias(
    val path: ServerPath,
    val alias: String,
)

/** One server's user workspace roots and custom aliases (aliases keyed by [ServerPath.compareKey]). */
data class ServerUserWorkspaces(
    val roots: List<UserWorkspaceRoot> = emptyList(),
    val aliases: Map<String, StoredWorkspaceAlias> = emptyMap(),
) {
    val rootPaths: List<ServerPath> get() = roots.map { it.path }

    fun isRoot(path: ServerPath): Boolean = roots.any { it.path.compareKey == path.compareKey }

    fun aliasFor(path: ServerPath): String? = aliases[path.compareKey]?.alias

    fun isEmpty(): Boolean = roots.isEmpty() && aliases.isEmpty()
}

/** Every saved server's user workspaces, keyed by normalized server endpoint key. */
data class UserWorkspaceState(
    val byEndpoint: Map<String, ServerUserWorkspaces> = emptyMap(),
) {
    fun forServer(endpointKey: String): ServerUserWorkspaces = byEndpoint[endpointKey] ?: EMPTY_SERVER

    fun rootsFor(endpointKey: String): List<ServerPath> = forServer(endpointKey).rootPaths

    companion object {
        private val EMPTY_SERVER = ServerUserWorkspaces()
        val Empty = UserWorkspaceState()
    }
}

object UserWorkspaceLimits {
    const val MAX_ROOTS_PER_SERVER = 64
    const val MAX_ALIASES_PER_SERVER = 256
    const val MAX_ALIAS_LENGTH = 64
}

sealed interface UserWorkspaceEdit {
    data class Updated(val state: UserWorkspaceState) : UserWorkspaceEdit
    data object Duplicate : UserWorkspaceEdit
    data object LimitReached : UserWorkspaceEdit
}

/** Pure edits of [UserWorkspaceState]; the settings store persists their results. */
object UserWorkspaceEdits {
    /** Trims, removes control characters, and caps a custom alias; null when nothing usable remains. */
    fun sanitizeAlias(raw: String?): String? = raw
        ?.filterNot { it.isISOControl() }
        ?.trim()
        ?.take(UserWorkspaceLimits.MAX_ALIAS_LENGTH)
        ?.trim()
        ?.takeIf { it.isNotEmpty() }

    fun addRoot(state: UserWorkspaceState, endpointKey: String, path: ServerPath, addedAt: Long): UserWorkspaceEdit {
        val server = state.forServer(endpointKey)
        return when {
            server.isRoot(path) -> UserWorkspaceEdit.Duplicate
            server.roots.size >= UserWorkspaceLimits.MAX_ROOTS_PER_SERVER -> UserWorkspaceEdit.LimitReached
            else -> UserWorkspaceEdit.Updated(
                state.withServer(endpointKey, server.copy(roots = server.roots + UserWorkspaceRoot(path, addedAt))),
            )
        }
    }

    /** Removes the root and keeps any custom alias, which still applies wherever the directory appears. */
    fun removeRoot(state: UserWorkspaceState, endpointKey: String, path: ServerPath): UserWorkspaceState {
        val server = state.forServer(endpointKey)
        return state.withServer(
            endpointKey,
            server.copy(roots = server.roots.filterNot { it.path.compareKey == path.compareKey }),
        )
    }

    /** Sets or resets (blank / null / equal to [generated]) the custom alias for [path]. */
    fun setAlias(
        state: UserWorkspaceState,
        endpointKey: String,
        path: ServerPath,
        alias: String?,
        generated: String?,
    ): UserWorkspaceEdit {
        val server = state.forServer(endpointKey)
        val sanitized = sanitizeAlias(alias)?.takeUnless { it == generated }
        val exists = path.compareKey in server.aliases
        val aliases = when {
            sanitized == null -> server.aliases - path.compareKey
            !exists && server.aliases.size >= UserWorkspaceLimits.MAX_ALIASES_PER_SERVER ->
                return UserWorkspaceEdit.LimitReached
            else -> server.aliases + (path.compareKey to StoredWorkspaceAlias(path, sanitized))
        }
        return UserWorkspaceEdit.Updated(state.withServer(endpointKey, server.copy(aliases = aliases)))
    }

    fun pruneServer(state: UserWorkspaceState, endpointKey: String): UserWorkspaceState =
        UserWorkspaceState(state.byEndpoint - endpointKey)

    /** Moves [fromEndpointKey]'s workspaces onto [toEndpointKey]; the target's own entries win conflicts. */
    fun moveServer(state: UserWorkspaceState, fromEndpointKey: String, toEndpointKey: String): UserWorkspaceState {
        val source = state.byEndpoint[fromEndpointKey]
        if (source == null || fromEndpointKey == toEndpointKey) return state
        val target = state.forServer(toEndpointKey)
        val targetKeys = target.roots.mapTo(HashSet()) { it.path.compareKey }
        val merged = ServerUserWorkspaces(
            roots = (target.roots + source.roots.filterNot { it.path.compareKey in targetKeys })
                .take(UserWorkspaceLimits.MAX_ROOTS_PER_SERVER),
            aliases = (source.aliases + target.aliases).entries
                .take(UserWorkspaceLimits.MAX_ALIASES_PER_SERVER)
                .associate { it.key to it.value },
        )
        return UserWorkspaceState(state.byEndpoint - fromEndpointKey + (toEndpointKey to merged))
    }

    private fun UserWorkspaceState.withServer(endpointKey: String, server: ServerUserWorkspaces): UserWorkspaceState =
        if (server.isEmpty()) {
            UserWorkspaceState(byEndpoint - endpointKey)
        } else {
            UserWorkspaceState(byEndpoint + (endpointKey to server))
        }
}
