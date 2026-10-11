package dev.blazelight.p4oc.domain.workspace

/**
 * Automatic workspace names (GitHub #72): the shortest right-to-left run of path components that
 * is unique among one server's workspaces, joined with ` / `.
 *
 * Only colliding entries expand. A path with no components below its root is named by its root.
 * The full path is used only when two different paths share every component (for example
 * `C:\x` and `D:\x`). Uniqueness follows each path's case rules.
 */
object WorkspaceAliasGenerator {
    const val SEPARATOR = " / "

    /** Maps each path's [ServerPath.compareKey] to its automatic name. */
    fun generate(paths: Collection<ServerPath>): Map<String, String> {
        val unique = paths.distinctBy { it.compareKey }
        return unique.associate { path -> path.compareKey to aliasFor(path, unique) }
    }

    private fun aliasFor(path: ServerPath, universe: List<ServerPath>): String {
        val components = path.components
        if (components.isEmpty()) return path.root
        val others = universe.filter { it.compareKey != path.compareKey }
        var length = 1
        while (length < components.size && others.any { collides(it, path, length) }) length++
        // Once every component is shown, only an identical component list is still ambiguous.
        val ambiguous = others.any { it.comparableComponents == path.comparableComponents }
        return if (ambiguous) path.value else components.takeLast(length).joinToString(SEPARATOR)
    }

    private fun collides(other: ServerPath, path: ServerPath, length: Int): Boolean =
        other.comparableComponents.takeLast(length) == path.comparableComponents.takeLast(length)
}
