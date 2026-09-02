package dev.blazelight.p4oc.domain.server

import dev.blazelight.p4oc.core.network.ServerUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.util.Locale

data class ServerIdentity(
    val displayName: String,
    val badgeLabel: String,
) {
    companion object {
        private val genericNames = setOf(
            "remote",
            "remote server",
            "server",
            "opencode",
            "opencode server",
        )

        fun derive(endpoint: String, candidateName: String? = null): ServerIdentity {
            val endpointKey = ServerUrl.endpointKey(endpoint)
                ?: throw IllegalArgumentException("Invalid server endpoint: $endpoint")
            val parsed = endpointKey.toHttpUrlOrNull()
                ?: throw IllegalArgumentException("Invalid server endpoint: $endpoint")
            val candidate = candidateName?.trim()?.takeUnless(::isGenericName)
            val displayName = candidate ?: endpointDisplayName(parsed.host, parsed.port)
            return ServerIdentity(
                displayName = displayName,
                badgeLabel = badgeLabel(displayName, endpointKey, isEndpointDerived = candidate == null),
            )
        }

        fun isGenericName(name: String?): Boolean {
            val normalized = name
                ?.trim()
                ?.lowercase(Locale.ROOT)
                ?.replace(Regex("[\\s_-]+"), " ")
                .orEmpty()
            return normalized.isBlank() || normalized in genericNames
        }

        private fun endpointDisplayName(host: String, port: Int): String {
            val isIpAddress = host.all { it.isDigit() || it == '.' } || ':' in host
            if (!isIpAddress || host.equals("localhost", ignoreCase = true)) return host
            val formattedHost = if (':' in host) "[$host]" else host
            return "$formattedHost:$port"
        }

        private fun badgeLabel(displayName: String, endpointKey: String, isEndpointDerived: Boolean): String {
            val words = displayName.split(Regex("[^\\p{L}\\p{N}]+"))
                .filter(String::isNotEmpty)
            val stem = when {
                isEndpointDerived && words.size >= 2 -> "${words[0].first()}${words[1].first()}"
                isEndpointDerived && words.isNotEmpty() -> words.first().take(2)
                words.size >= 2 -> "${words.first().first()}${words.last().first()}"
                words.isNotEmpty() -> words.first().take(2)
                else -> "SV"
            }.uppercase(Locale.ROOT).padEnd(2, 'X')
            val hash = endpointKey.hashCode().toUInt().toString(radix = 36).uppercase(Locale.ROOT)
                .padStart(2, '0').takeLast(2)
            return stem + hash
        }
    }
}
