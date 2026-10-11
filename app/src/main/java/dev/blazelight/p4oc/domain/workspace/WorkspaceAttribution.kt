package dev.blazelight.p4oc.domain.workspace

import dev.blazelight.p4oc.domain.server.WorkspaceKey

/**
 * Groups a server-reported directory under the deepest user workspace root that contains it.
 *
 * The result is a grouping and labeling key only; it is never used to route requests. Directories
 * outside every root (or not parseable as absolute paths) keep their exact key, as before user roots.
 */
object WorkspaceAttribution {
    fun groupKeyFor(directory: String, roots: List<ServerPath>): WorkspaceKey {
        if (directory.isBlank()) return WorkspaceKey.Global
        val root = deepestRootFor(directory, roots)
        return WorkspaceKey.Directory(root?.value ?: directory)
    }

    /** The deepest root containing [directory], or null when none does. */
    fun deepestRootFor(directory: String, roots: List<ServerPath>): ServerPath? {
        val path = if (roots.isEmpty()) null else ServerPath.parseOrNull(directory)
        return path?.let { target -> roots.filter { it.contains(target) }.maxByOrNull { it.components.size } }
    }
}
