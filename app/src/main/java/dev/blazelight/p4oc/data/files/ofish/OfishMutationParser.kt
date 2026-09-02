package dev.blazelight.p4oc.data.files.ofish

internal object OfishMutationParser {
    private val STATUS_LINE = Regex("^###\\s+(\\d{3})\\s+(\\S+)(?:\\s+(.*))?$")
    private val KEY_VALUE = Regex("(\\w+)=([^\\s]+)")
    private val UPLOAD_VALUE = Regex("(?:^|\\s)upload=(.*)$")

    @Suppress("ReturnCount")
    fun parse(output: String, expectedMarker: String): OfishMutationStatus {
        val lines = output.lineSequence().toList()
        val markerIndex = lines.indexOfFirst { it == expectedMarker }
        if (markerIndex == -1) {
            return OfishMutationStatus.Malformed("Malformed OFISH mutation output: missing $expectedMarker marker")
        }

        // A shell command prints its marker before doing any work and exactly one terminal
        // status line. Stop at the first valid status in that segment: text produced by the
        // assistant before the marker or after the command's status is not command output.
        val match = lines.asSequence()
            .drop(markerIndex + 1)
            .takeWhile { !it.startsWith("#OFISH_") }
            .map { it.trimStart() }
            .mapNotNull(STATUS_LINE::matchEntire)
            .firstOrNull()
            ?: return OfishMutationStatus.Malformed("Malformed OFISH mutation output: missing status line")
        val code = match.groupValues[1].toInt()
        val status = match.groupValues[2]
        val remainder = match.groupValues.getOrNull(3).orEmpty()
        val values = KEY_VALUE.findAll(remainder).associate { it.groupValues[1] to it.groupValues[2] }.toMutableMap()
        // The upload token is a path emitted as the final field. Unlike hashes and reason codes,
        // it may contain whitespace and must reach shell quoting byte-for-byte intact.
        UPLOAD_VALUE.find(remainder)?.groupValues?.get(1)?.let { values["upload"] = it }

        return when (code) {
            200, 201 -> OfishMutationStatus.Ok(
                code = code,
                status = status,
                hash = values["hash"],
                uploadToken = values["upload"],
                values = values,
            )
            204 -> OfishMutationStatus.Deleted
            404 -> OfishMutationStatus.Missing
            409 -> OfishMutationStatus.Conflict(actualHash = values["actual"])
            412 -> OfishMutationStatus.PreconditionFailed(reason = values["reason"])
            500 -> OfishMutationStatus.Failed(message = "OFISH mutation failed", reason = values["reason"])
            501 -> OfishMutationStatus.CapabilitiesMissing(
                missing = remainder
                    .removePrefix(status)
                    .trim()
                    .split(Regex("\\s+"))
                    .filter { it.isNotBlank() },
            )
            else -> OfishMutationStatus.Malformed("Unsupported OFISH mutation status code: $code")
        }
    }
}

internal sealed interface OfishMutationStatus {
    data class Ok(
        val code: Int,
        val status: String,
        val hash: String? = null,
        val uploadToken: String? = null,
        val values: Map<String, String> = emptyMap(),
    ) : OfishMutationStatus

    data object Deleted : OfishMutationStatus

    data object Missing : OfishMutationStatus

    data class Conflict(val actualHash: String?) : OfishMutationStatus

    data class PreconditionFailed(val reason: String?) : OfishMutationStatus

    data class CapabilitiesMissing(val missing: List<String>) : OfishMutationStatus

    data class Failed(
        val message: String,
        val reason: String? = null,
    ) : OfishMutationStatus

    data class Malformed(val message: String) : OfishMutationStatus
}
