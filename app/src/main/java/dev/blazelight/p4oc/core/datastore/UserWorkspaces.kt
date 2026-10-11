package dev.blazelight.p4oc.core.datastore

import dev.blazelight.p4oc.core.log.AppLog
import dev.blazelight.p4oc.domain.workspace.ServerPath
import dev.blazelight.p4oc.domain.workspace.ServerUserWorkspaces
import dev.blazelight.p4oc.domain.workspace.StoredWorkspaceAlias
import dev.blazelight.p4oc.domain.workspace.UserWorkspaceEdits
import dev.blazelight.p4oc.domain.workspace.UserWorkspaceLimits
import dev.blazelight.p4oc.domain.workspace.UserWorkspaceRoot
import dev.blazelight.p4oc.domain.workspace.UserWorkspaceState
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/*
 * Persistence for user-defined workspace roots and custom aliases (`user_workspaces_v1`).
 * Every entry is re-validated on decode; invalid entries are dropped one by one, never repaired.
 */

private const val USER_WORKSPACES_TAG = "UserWorkspaces"
internal const val USER_WORKSPACES_VERSION = 1

private val userWorkspacesJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

@Serializable
internal data class PersistedUserWorkspaces(
    val version: Int = USER_WORKSPACES_VERSION,
    val servers: Map<String, PersistedServerWorkspaces> = emptyMap(),
)

@Serializable
internal data class PersistedServerWorkspaces(
    val roots: List<PersistedWorkspaceRoot> = emptyList(),
    /** Canonical directory → custom alias. */
    val aliases: Map<String, String> = emptyMap(),
)

@Serializable
internal data class PersistedWorkspaceRoot(
    val directory: String,
    val addedAt: Long = 0L,
)

internal fun decodeUserWorkspaces(stored: String?): UserWorkspaceState {
    val persisted = stored?.takeIf { it.isNotBlank() }?.let(::parseUserWorkspaces) ?: return UserWorkspaceState.Empty
    var dropped = 0
    val servers = persisted.servers.mapNotNull { (endpointKey, server) ->
        if (endpointKey.isBlank()) {
            dropped += server.roots.size + server.aliases.size
            return@mapNotNull null
        }
        val decoded = decodeServer(server) { dropped++ }
        decoded.takeUnless { it.isEmpty() }?.let { endpointKey to it }
    }.toMap()
    if (dropped > 0) AppLog.w(USER_WORKSPACES_TAG, "Dropped $dropped invalid user workspace entries")
    return UserWorkspaceState(servers)
}

private fun parseUserWorkspaces(stored: String): PersistedUserWorkspaces? =
    runCatching { userWorkspacesJson.decodeFromString<PersistedUserWorkspaces>(stored) }
        .onFailure { error ->
            AppLog.w(USER_WORKSPACES_TAG, "Ignoring unreadable user workspaces (${error::class.simpleName})")
        }
        .getOrNull()

private fun decodeServer(server: PersistedServerWorkspaces, onDropped: () -> Unit): ServerUserWorkspaces {
    val roots = LinkedHashMap<String, UserWorkspaceRoot>()
    server.roots.forEach { root ->
        val path = ServerPath.parseOrNull(root.directory)
        if (path == null) {
            onDropped()
        } else {
            roots.remove(path.compareKey)
            roots[path.compareKey] = UserWorkspaceRoot(path, root.addedAt)
        }
    }
    val aliases = LinkedHashMap<String, StoredWorkspaceAlias>()
    server.aliases.forEach { (directory, rawAlias) ->
        val path = ServerPath.parseOrNull(directory)
        val alias = UserWorkspaceEdits.sanitizeAlias(rawAlias)
        if (path == null || alias == null) {
            onDropped()
        } else {
            aliases.remove(path.compareKey)
            aliases[path.compareKey] = StoredWorkspaceAlias(path, alias)
        }
    }
    return ServerUserWorkspaces(
        roots = roots.values.take(UserWorkspaceLimits.MAX_ROOTS_PER_SERVER),
        aliases = aliases.entries.take(UserWorkspaceLimits.MAX_ALIASES_PER_SERVER).associate { it.key to it.value },
    )
}

/** Encodes [state]; null when there is nothing to store so the preference can be removed. */
internal fun encodeUserWorkspaces(state: UserWorkspaceState): String? {
    val servers = state.byEndpoint
        .filterValues { !it.isEmpty() }
        .mapValues { (_, server) ->
            PersistedServerWorkspaces(
                roots = server.roots.map { PersistedWorkspaceRoot(it.path.value, it.addedAt) },
                aliases = server.aliases.values.associate { it.path.value to it.alias },
            )
        }
    if (servers.isEmpty()) return null
    return userWorkspacesJson.encodeToString(
        PersistedUserWorkspaces.serializer(),
        PersistedUserWorkspaces(servers = servers),
    )
}

internal fun pruneUserWorkspaces(stored: String?, endpointKey: String): String? =
    encodeUserWorkspaces(UserWorkspaceEdits.pruneServer(decodeUserWorkspaces(stored), endpointKey))

internal fun movedUserWorkspaces(stored: String?, fromEndpointKey: String, toEndpointKey: String): String? =
    encodeUserWorkspaces(UserWorkspaceEdits.moveServer(decodeUserWorkspaces(stored), fromEndpointKey, toEndpointKey))
