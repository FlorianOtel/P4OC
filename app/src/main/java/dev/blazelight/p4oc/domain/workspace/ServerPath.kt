package dev.blazelight.p4oc.domain.workspace

import java.util.Locale

enum class PathStyle { Posix, Windows }

/** Why a user-typed workspace path was rejected. */
enum class PathRejection { Blank, NotAbsolute, HomeShorthand, FileUri, ControlCharacters, DotSegments }

sealed interface ServerPathParse {
    data class Valid(val path: ServerPath) : ServerPathParse
    data class Invalid(val reason: PathRejection) : ServerPathParse
}

/**
 * A normalized absolute directory on an OpenCode server.
 *
 * POSIX paths compare case-sensitively; Windows drive and UNC paths compare case-insensitively with
 * `\` and `/` treated alike. Containment compares whole components, so `/a/b` never contains `/a/bc`.
 */
class ServerPath private constructor(
    val style: PathStyle,
    /** `/`, `C:\` or `\\host\share`. */
    val root: String,
    /** Components below [root], in their original case. */
    val components: List<String>,
) {
    /** Canonical routing and display form. */
    val value: String = when {
        components.isEmpty() -> root
        style == PathStyle.Posix -> root + components.joinToString(POSIX_SEPARATOR)
        root.endsWith(WINDOWS_SEPARATOR) -> root + components.joinToString(WINDOWS_SEPARATOR)
        else -> root + WINDOWS_SEPARATOR + components.joinToString(WINDOWS_SEPARATOR)
    }

    /** Identity key: equal keys mean the same directory on one server. */
    val compareKey: String = if (style == PathStyle.Windows) value.lowercase(Locale.ROOT) else value

    /** Components normalized for comparison under this path's case rules. */
    val comparableComponents: List<String> =
        if (style == PathStyle.Windows) components.map { it.lowercase(Locale.ROOT) } else components

    private val comparableRoot: String =
        if (style == PathStyle.Windows) root.lowercase(Locale.ROOT) else root

    /** True when [other] is this directory or one of its descendants. */
    fun contains(other: ServerPath): Boolean =
        style == other.style &&
            comparableRoot == other.comparableRoot &&
            other.comparableComponents.size >= comparableComponents.size &&
            other.comparableComponents.subList(0, comparableComponents.size) == comparableComponents

    /** Components of [descendant] below this path; empty when equal. Requires [contains]. */
    fun relativeComponents(descendant: ServerPath): List<String> {
        require(contains(descendant)) { "Path is not inside this workspace" }
        return descendant.components.drop(components.size)
    }

    fun parent(): ServerPath? =
        if (components.isEmpty()) null else ServerPath(style, root, components.dropLast(1))

    fun child(name: String): ServerPath {
        require(isValidComponent(name)) { "Invalid folder name" }
        return ServerPath(style, root, components + name)
    }

    /** True when [name] names this directory's last component under its case rules. */
    fun lastComponentMatches(name: String): Boolean {
        val last = components.lastOrNull() ?: return false
        return if (style == PathStyle.Windows) last.equals(name, ignoreCase = true) else last == name
    }

    override fun equals(other: Any?): Boolean = other is ServerPath && other.compareKey == compareKey

    override fun hashCode(): Int = compareKey.hashCode()

    override fun toString(): String = value

    companion object {
        private const val POSIX_SEPARATOR = "/"
        private const val WINDOWS_SEPARATOR = "\\"
        private const val UNC_PREFIX = "\\\\"
        private const val FILE_URI_PREFIX = "file://"
        private const val DRIVE_PREFIX_LENGTH = 2
        private val DRIVE_PATTERN = Regex("^[A-Za-z]:([\\\\/].*)?$")
        private val WINDOWS_SEPARATORS = Regex("[\\\\/]")

        /** Parses a path the user typed: rejects control characters anywhere, trims spaces, explains rejections. */
        fun parseUserInput(raw: String): ServerPathParse =
            if (raw.any { it.isISOControl() }) {
                ServerPathParse.Invalid(PathRejection.ControlCharacters)
            } else {
                parse(raw.trim())
            }

        /** Parses a server-provided directory as-is; null when it is not a normalizable absolute path. */
        fun parseOrNull(serverValue: String): ServerPath? = (parse(serverValue) as? ServerPathParse.Valid)?.path

        private fun parse(input: String): ServerPathParse {
            val rejection = rejectionFor(input)
            val split = if (rejection == null) splitRoot(input) else null
            return when {
                rejection != null -> ServerPathParse.Invalid(rejection)
                split == null -> ServerPathParse.Invalid(PathRejection.NotAbsolute)
                else -> fromComponents(split)
            }
        }

        private fun fromComponents(split: RootSplit): ServerPathParse {
            val separator = if (split.style == PathStyle.Posix) Regex(POSIX_SEPARATOR) else WINDOWS_SEPARATORS
            val components = split.rest.split(separator).filter { it.isNotEmpty() }
            return if (components.any { it == "." || it == ".." }) {
                ServerPathParse.Invalid(PathRejection.DotSegments)
            } else {
                ServerPathParse.Valid(ServerPath(split.style, split.root, components))
            }
        }

        private fun rejectionFor(input: String): PathRejection? = when {
            input.isBlank() -> PathRejection.Blank
            input.any { it.isISOControl() } -> PathRejection.ControlCharacters
            input.startsWith(FILE_URI_PREFIX, ignoreCase = true) -> PathRejection.FileUri
            input.startsWith("~") -> PathRejection.HomeShorthand
            else -> null
        }

        private class RootSplit(val style: PathStyle, val root: String, val rest: String)

        private fun splitRoot(input: String): RootSplit? = when {
            input.startsWith(POSIX_SEPARATOR) -> RootSplit(PathStyle.Posix, POSIX_SEPARATOR, input)
            DRIVE_PATTERN.matches(input) -> RootSplit(
                style = PathStyle.Windows,
                root = input.substring(0, 1).uppercase(Locale.ROOT) + ":" + WINDOWS_SEPARATOR,
                rest = input.substring(DRIVE_PREFIX_LENGTH),
            )
            input.startsWith(UNC_PREFIX) -> splitUnc(input)
            else -> null
        }

        private fun splitUnc(input: String): RootSplit? {
            val parts = input.substring(UNC_PREFIX.length).split(WINDOWS_SEPARATORS)
            val host = parts.getOrNull(0).orEmpty()
            val share = parts.getOrNull(1).orEmpty()
            if (!isValidComponent(host) || !isValidComponent(share)) return null
            return RootSplit(
                style = PathStyle.Windows,
                root = UNC_PREFIX + host + WINDOWS_SEPARATOR + share,
                rest = parts.drop(2).joinToString(WINDOWS_SEPARATOR),
            )
        }

        /** True when [name] can be one path component; [child] rejects everything else. */
        fun isValidComponent(name: String): Boolean =
            name.isNotBlank() && name != "." && name != ".." &&
                name.none { it == '/' || it == '\\' || it.isISOControl() }
    }
}
