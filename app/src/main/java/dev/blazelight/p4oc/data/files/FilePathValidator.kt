package dev.blazelight.p4oc.data.files

internal object FilePathValidator {
    fun normalizeForReadOrList(path: String): Result<String> = normalize(path, allowRoot = true)

    fun normalizeForMutation(path: String): Result<String> = normalize(path, allowRoot = false)

    private fun normalize(path: String, allowRoot: Boolean): Result<String> {
        if (path.indexOf('\u0000') >= 0) {
            return invalid("NUL characters are not allowed in file paths")
        }

        val trimmed = path.trim()
        if (trimmed.isEmpty() || trimmed == "." || trimmed.all { it == '/' }) {
            return if (allowRoot) Result.success("") else invalid("Root file path is not allowed for mutations")
        }

        if (!allowRoot && path != trimmed) {
            return invalid("Leading or trailing whitespace is not allowed in file paths")
        }

        if (trimmed.startsWith("~")) {
            return invalid("Home-relative file paths are not allowed")
        }

        if (trimmed.contains('\\')) {
            return invalid("Backslash path separators are not allowed")
        }

        if (WINDOWS_DRIVE_PATTERN.matches(trimmed)) {
            return invalid("Windows drive file paths are not allowed")
        }

        if (URI_SCHEME_PATTERN.matches(trimmed)) {
            return invalid("URI file paths are not allowed")
        }

        if (trimmed.startsWith("/")) {
            return invalid("Absolute file paths are not allowed")
        }

        val segments = mutableListOf<String>()
        trimmed.split('/').forEach { segment ->
            when (segment) {
                "", "." -> Unit
                ".." -> return invalid("Parent path segments are not allowed")
                else -> segments += segment
            }
        }

        val normalized = segments.joinToString("/")
        if (normalized.isEmpty() && !allowRoot) {
            return invalid("Root file path is not allowed for mutations")
        }

        return Result.success(normalized)
    }

    private fun invalid(message: String): Result<String> = Result.failure(InvalidFilePathException(message))

    private val WINDOWS_DRIVE_PATTERN = Regex("^[A-Za-z]:.*")
    private val URI_SCHEME_PATTERN = Regex("^[A-Za-z][A-Za-z0-9+.-]*:/+.*")
}

internal class InvalidFilePathException(message: String) : IllegalArgumentException(message)
