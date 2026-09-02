package dev.blazelight.p4oc.core.network

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.net.Inet6Address
import java.net.InetAddress

object ServerUrl {
    const val DEFAULT_PORT = 4096
    const val DEFAULT_USERNAME = "opencode"

    fun normalizeConnectUrl(input: String): String? {
        val components = parse(input) ?: return null
        return buildUrl(
            scheme = components.scheme,
            host = components.formattedHost,
            port = components.explicitPort,
            path = components.path,
        )
    }

    fun endpointKey(input: String): String? {
        val components = parse(input) ?: return null
        return buildUrl(
            scheme = components.scheme,
            host = components.formattedHost,
            port = components.canonicalPort,
            path = components.path,
        )
    }

    /**
     * Cleartext authentication is permitted only for exact localhost or literal local-network
     * addresses. Other hostnames are deliberately excluded: resolving one here would make the
     * decision vulnerable to DNS changes and rebinding between validation and connection.
     */
    @Suppress("ReturnCount")
    fun allowsCleartextCredentials(input: String): Boolean {
        val trimmed = input.trim()
        val candidate = if (trimmed.contains("://")) trimmed else "http://$trimmed"
        val parsed = stripIpv6ZoneId(candidate).toHttpUrlOrNull() ?: return false
        if (parsed.scheme != "http") return true

        val host = parsed.host.substringBefore('%')
        return host.equals("localhost", ignoreCase = true) ||
            isPrivateIpv4Literal(host) || isPrivateIpv6Literal(host)
    }

    @Suppress("MagicNumber", "ReturnCount")
    private fun isPrivateIpv4Literal(host: String): Boolean {
        val octets = host.split('.')
        if (octets.size != 4) return false
        val values = octets.map { part ->
            if (part.isEmpty() || part.any { !it.isDigit() }) return false
            part.toIntOrNull()?.takeIf { it in 0..255 } ?: return false
        }
        return values[0] == 127 ||
            values[0] == 10 ||
            (values[0] == 172 && values[1] in 16..31) ||
            (values[0] == 192 && values[1] == 168)
    }

    @Suppress("MagicNumber", "ReturnCount")
    private fun isPrivateIpv6Literal(host: String): Boolean {
        if (':' !in host || host.any { it !in "0123456789abcdefABCDEF:." }) return false
        val address = runCatching { InetAddress.getByName(host) }.getOrNull() as? Inet6Address
            ?: return false
        val bytes = address.address
        val first = bytes[0].toInt() and 0xff
        val second = bytes[1].toInt() and 0xff
        val isLoopback = bytes.dropLast(1).all { it.toInt() == 0 } && bytes.last().toInt() == 1
        val isUniqueLocal = first and 0xfe == 0xfc
        val isLinkLocal = first == 0xfe && second and 0xc0 == 0x80
        return isLoopback || isUniqueLocal || isLinkLocal
    }

    private data class ParsedServerUrl(
        val scheme: String,
        val formattedHost: String,
        val explicitPort: Int?,
        val canonicalPort: Int,
        val path: String,
    )

    private fun parse(input: String): ParsedServerUrl? {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return null

        val candidate = if (trimmed.contains("://")) trimmed else "http://$trimmed"
        val sanitizedCandidate = stripIpv6ZoneId(candidate)
        val parsed = sanitizedCandidate.toHttpUrlOrNull() ?: return null
        val scheme = parsed.scheme.lowercase()
        if (scheme != "http" && scheme != "https") return null

        val host = parsed.host.substringBefore('%').lowercase()
        val formattedHost = if (':' in host) "[$host]" else host
        val explicitPort = parsed.port.takeIf { hasExplicitPort(sanitizedCandidate) }
        val canonicalPort = explicitPort ?: DEFAULT_PORT
        val path = parsed.encodedPath
            .takeUnless { it == "/" }
            ?.trimEnd('/')
            .orEmpty()

        return ParsedServerUrl(
            scheme = scheme,
            formattedHost = formattedHost,
            explicitPort = explicitPort,
            canonicalPort = canonicalPort,
            path = path,
        )
    }

    private fun buildUrl(
        scheme: String,
        host: String,
        port: Int?,
        path: String,
    ): String {
        val base = if (port == null) "$scheme://$host" else "$scheme://$host:$port"
        return if (path.isEmpty()) base else "$base$path"
    }

    private fun stripIpv6ZoneId(candidate: String): String {
        val schemePrefix = candidate.substringBefore("://", missingDelimiterValue = "")
        if (schemePrefix.isBlank()) return candidate

        val prefix = "$schemePrefix://"
        val rest = candidate.removePrefix(prefix)
        if (!rest.startsWith("[")) return candidate

        val closingBracket = rest.indexOf(']')
        if (closingBracket < 0) return candidate

        val host = rest.substring(1, closingBracket).substringBefore('%')
        val remainder = rest.substring(closingBracket + 1)
        return "$prefix[$host]$remainder"
    }

    private fun hasExplicitPort(candidate: String): Boolean {
        val authority = candidate
            .substringAfter("://")
            .substringBefore('/')
            .substringBefore('?')
            .substringBefore('#')
            .substringAfterLast('@')

        if (authority.startsWith("[")) {
            return authority.substringAfter("]", missingDelimiterValue = "").startsWith(":")
        }

        return authority.contains(':')
    }
}
