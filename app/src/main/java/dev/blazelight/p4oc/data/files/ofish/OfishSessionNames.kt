package dev.blazelight.p4oc.data.files.ofish

object OfishSessionNames {
    const val PREFIX = "__ofish_"
    private val TITLE_PATTERN = Regex("^__ofish_[^_]+_(\\d+)_([^_]+)$")
    private val UNSAFE_OPERATION_CHARS = Regex("[^A-Za-z0-9-]+")

    fun build(operationName: String, epochMs: Long, shortId: String): String {
        val sanitizedOperation = sanitizeOperation(operationName)
        val sanitizedShortId = shortId.replace(UNSAFE_OPERATION_CHARS, "").take(12).ifBlank { "id" }
        return "${PREFIX}${sanitizedOperation}_${epochMs}_$sanitizedShortId"
    }

    fun isOfishTitle(title: String?): Boolean = title?.startsWith(PREFIX) == true

    fun parseTimestamp(title: String?): Long? {
        if (!isOfishTitle(title)) return null
        return TITLE_PATTERN.matchEntire(title.orEmpty())
            ?.groupValues
            ?.getOrNull(1)
            ?.toLongOrNull()
    }

    private fun sanitizeOperation(operationName: String): String = operationName
        .trim()
        .lowercase()
        .replace(UNSAFE_OPERATION_CHARS, "-")
        .trim('-')
        .take(32)
        .ifBlank { "op" }
}
