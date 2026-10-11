package dev.blazelight.p4oc.ui.workspace

import androidx.compose.runtime.compositionLocalOf
import dev.blazelight.p4oc.domain.server.ServerRef
import dev.blazelight.p4oc.domain.server.WorkspaceKey
import dev.blazelight.p4oc.domain.workspace.ServerPath
import dev.blazelight.p4oc.domain.workspace.UserWorkspaceState
import dev.blazelight.p4oc.domain.workspace.WorkspaceAliasGenerator
import dev.blazelight.p4oc.domain.workspace.WorkspaceAttribution

/** How one workspace directory is named. The alias is presentation only; identity stays server + path. */
data class WorkspaceLabel(
    /** Emphasized name: custom alias, else automatic alias, else a root-relative name. */
    val primary: String,
    /** Automatic name (GitHub #72), shown next to a custom alias. */
    val generated: String,
    val custom: String?,
    /** Full canonical path (secondary metadata). */
    val path: String,
    val isUserRoot: Boolean,
)

/**
 * Display names for every workspace directory on every saved server.
 *
 * Built from one per-server universe (user roots plus discovered and open-tab directories outside
 * every root) so all screens agree. Never consulted for routing and never holds a "current" workspace.
 */
class WorkspaceLabelIndex private constructor(
    private val servers: Map<String, ServerLabels>,
) {
    private class ServerLabels(
        val roots: List<ServerPath>,
        val aliases: Map<String, String>,
        val generated: Map<String, String>,
    )

    fun rootsFor(endpointKey: String): List<ServerPath> = servers[endpointKey]?.roots.orEmpty()

    /** Grouping key for a server-reported directory: its deepest user root, else the directory itself. */
    fun groupKeyFor(server: ServerRef, directory: String): WorkspaceKey =
        WorkspaceAttribution.groupKeyFor(directory, rootsFor(server.endpointKey))

    fun isUserRoot(server: ServerRef, directory: String): Boolean {
        val path = ServerPath.parseOrNull(directory) ?: return false
        return rootsFor(server.endpointKey).any { it.compareKey == path.compareKey }
    }

    /** Label for a directory workspace; [endpointKey] null means no server context (generic naming). */
    fun directoryLabel(endpointKey: String?, directory: String): WorkspaceLabel {
        val path = ServerPath.parseOrNull(directory)
            ?: return WorkspaceLabel(lastSegment(directory), lastSegment(directory), null, directory, false)
        val server = endpointKey?.let(servers::get)
        val generated = server?.generated?.get(path.compareKey) ?: soleAlias(path)
        val custom = server?.aliases?.get(path.compareKey)
        val primary = custom
            ?: server?.generated?.get(path.compareKey)
            ?: server?.let { rootRelativeName(endpointKey, it, path) }
            ?: generated
        return WorkspaceLabel(
            primary = primary,
            generated = generated,
            custom = custom,
            path = path.value,
            isUserRoot = server?.roots?.any { it.compareKey == path.compareKey } == true,
        )
    }

    /** Primary label for a directory key, or null for Global/SessionScoped keys (callers keep their copy). */
    fun primaryFor(server: ServerRef?, key: WorkspaceKey?): String? =
        (key as? WorkspaceKey.Directory)?.let { directoryLabel(server?.endpointKey, it.value).primary }

    /** Text matched by workspace search: custom and automatic names, full path, and host. */
    fun searchText(server: ServerRef, key: WorkspaceKey): String {
        val directory = (key as? WorkspaceKey.Directory)?.value ?: return server.displayName
        val label = directoryLabel(server.endpointKey, directory)
        return listOfNotNull(
            label.custom,
            label.generated,
            label.primary,
            label.path,
            directory,
            server.displayName,
            server.badgeLabel,
        ).joinToString("\n")
    }

    private fun rootRelativeName(endpointKey: String?, server: ServerLabels, path: ServerPath): String? {
        val root = server.roots.filter { it.contains(path) && it.compareKey != path.compareKey }
            .maxByOrNull { it.components.size }
            ?: return null
        val rootName = directoryLabel(endpointKey, root.value).primary
        return (listOf(rootName) + root.relativeComponents(path)).joinToString(WorkspaceAliasGenerator.SEPARATOR)
    }

    companion object {
        /** Generic naming without user data (previews and tests): one path's own automatic name. */
        val Empty = WorkspaceLabelIndex(emptyMap())

        /**
         * @param directoriesByEndpoint every discovered session directory and open-tab directory per
         * server endpoint; directories inside a user root are labeled relative to that root instead.
         */
        fun build(
            userWorkspaces: UserWorkspaceState,
            directoriesByEndpoint: Map<String, Collection<String>>,
        ): WorkspaceLabelIndex {
            val endpoints = userWorkspaces.byEndpoint.keys + directoriesByEndpoint.keys
            val servers = endpoints.associateWith { endpointKey ->
                val config = userWorkspaces.forServer(endpointKey)
                val roots = config.rootPaths
                val outsideRoots = directoriesByEndpoint[endpointKey].orEmpty()
                    .asSequence()
                    .distinct()
                    .mapNotNull(ServerPath::parseOrNull)
                    .filter { path -> roots.none { it.contains(path) } }
                    .toList()
                ServerLabels(
                    roots = roots,
                    aliases = config.aliases.mapValues { it.value.alias },
                    generated = WorkspaceAliasGenerator.generate(roots + outsideRoots),
                )
            }
            return WorkspaceLabelIndex(servers)
        }

        private fun soleAlias(path: ServerPath): String =
            WorkspaceAliasGenerator.generate(listOf(path)).getValue(path.compareKey)

        private fun lastSegment(directory: String): String =
            directory.trimEnd('/', '\\').substringAfterLast('/').substringAfterLast('\\').ifBlank { directory }
    }
}

/** Display-only workspace names for screens inside a tab; never used to route requests. */
val LocalWorkspaceLabels = compositionLocalOf { WorkspaceLabelIndex.Empty }
