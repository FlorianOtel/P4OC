package dev.blazelight.p4oc.core.network

import okhttp3.Authenticator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class MdnsDiscoveryManagerSeedTest {
    @Test
    fun `seed probe client disables redirect and credential challenges`() {
        val client = buildSeedProbeClient(allowInsecure = false)

        assertFalse(client.followRedirects)
        assertFalse(client.followSslRedirects)
        assertSame(Authenticator.NONE, client.authenticator)
        assertSame(Authenticator.NONE, client.proxyAuthenticator)
    }

    @Test
    fun `seed probe does not trust redirect responses`() {
        assertFalse(300.isOpenCodeSeedResponse())
        assertFalse(301.isOpenCodeSeedResponse())
        assertFalse(302.isOpenCodeSeedResponse())
        assertFalse(307.isOpenCodeSeedResponse())
        assertFalse(308.isOpenCodeSeedResponse())
    }

    @Test
    fun `seed probe recognizes authenticated OpenCode server`() {
        assertTrue(200.isOpenCodeSeedResponse())
        assertTrue(401.isOpenCodeSeedResponse())
        assertFalse(403.isOpenCodeSeedResponse())
        assertFalse(404.isOpenCodeSeedResponse())
    }

    @Test
    fun `normalizeSeed http host defaults 4096`() {
        val normalized = normalizeSeed(DiscoverySeed("example.com"))

        requireNotNull(normalized)
        assertEquals("http://example.com:4096", normalized.canonicalUrl)
        assertEquals("example.com", normalized.host)
        assertEquals(4096, normalized.port)
        assertEquals("http", normalized.scheme)
    }

    @Test
    fun `normalizeSeed https host defaults 4096`() {
        val normalized = normalizeSeed(DiscoverySeed("https://example.com"))

        requireNotNull(normalized)
        assertEquals("https://example.com:4096", normalized.canonicalUrl)
        assertEquals("example.com", normalized.host)
        assertEquals(4096, normalized.port)
        assertEquals("https", normalized.scheme)
    }

    @Test
    fun `normalizeSeed preserves path and strips query fragment`() {
        val normalized = normalizeSeed(DiscoverySeed("https://example.com/foo/bar?x=1#frag"))

        requireNotNull(normalized)
        assertEquals("https://example.com:4096/foo/bar", normalized.canonicalUrl)
    }

    @Test
    fun `normalizeSeed rejects blank and unsupported schemes`() {
        assertNull(normalizeSeed(DiscoverySeed("   ")))
        assertNull(normalizeSeed(DiscoverySeed("ftp://example.com")))
    }

    @Test
    fun `normalizeSeed rejects user info credentials`() {
        assertNull(normalizeSeed(DiscoverySeed("http://user:secret@example.com")))
    }

    @Test
    fun `normalizeSeed preserves ipv6 brackets and path`() {
        val normalized = normalizeSeed(DiscoverySeed("http://[2001:db8::1]/foo"))

        requireNotNull(normalized)
        assertEquals("http://[2001:db8::1]:4096/foo", normalized.canonicalUrl)
        assertEquals("[2001:db8::1]", normalized.host)
    }

    @Test
    fun `normalizeSeed carries allowInsecure from seed`() {
        val normalized = normalizeSeed(DiscoverySeed("https://example.com", allowInsecure = true))

        requireNotNull(normalized)
        assertTrue(normalized.allowInsecure)
    }

    @Test
    fun `mergeDiscoveredServer adds when absent`() {
        val incoming = DiscoveredServer(
            serviceName = "seed:example.com:4096",
            host = "example.com",
            port = 4096,
            url = "http://example.com:4096",
            source = DiscoverySource.SEED,
        )

        val merged = mergeDiscoveredServer(emptyList(), incoming)

        assertEquals(listOf(incoming), merged)
    }

    @Test
    fun `mergeDiscoveredServer does not let insecure seed downgrade strict mdns identity`() {
        val existing = listOf(
            DiscoveredServer(
                serviceName = "opencode-local",
                host = "example.com",
                port = 4096,
                url = "http://example.com:4096",
                source = DiscoverySource.MDNS,
            )
        )
        val incoming = DiscoveredServer(
            serviceName = "seed:example.com:4096",
            host = "example.com",
            port = 4096,
            url = "http://example.com:4096",
            source = DiscoverySource.SEED,
            allowInsecure = true,
        )

        val merged = mergeDiscoveredServer(existing, incoming)

        assertEquals(existing.single(), merged.single())
    }

    @Test
    fun `mergeDiscoveredServer replaces seed with mdns and preserves explicit insecure choice`() {
        val existing = listOf(
            DiscoveredServer(
                serviceName = "seed:example.com:4096",
                host = "example.com",
                port = 4096,
                url = "http://example.com:4096",
                source = DiscoverySource.SEED,
                allowInsecure = true,
            )
        )
        val incoming = DiscoveredServer(
            serviceName = "opencode-local",
            host = "example.com",
            port = 4096,
            url = "http://example.com:4096",
            source = DiscoverySource.MDNS,
            allowInsecure = false,
        )

        val merged = mergeDiscoveredServer(existing, incoming)

        assertEquals(listOf(incoming.copy(allowInsecure = true)), merged)
    }

    @Test
    fun `mergeDiscoveredServer same-source duplicate preserves strict TLS choice`() {
        val existing = listOf(
            DiscoveredServer(
                serviceName = "seed:example.com:4096",
                host = "example.com",
                port = 4096,
                url = "https://example.com:4096",
                source = DiscoverySource.SEED,
                allowInsecure = false,
            )
        )
        val incoming = existing.single().copy(
            serviceName = "seed:example.com:4096-duplicate",
            allowInsecure = true,
        )

        val merged = mergeDiscoveredServer(existing, incoming)

        assertEquals(incoming.copy(allowInsecure = false), merged.single())
    }

    @Test
    fun `mergeDiscoveredServer replaces same-source duplicate`() {
        val existing = listOf(
            DiscoveredServer(
                serviceName = "seed:example.com:4096",
                host = "example.com",
                port = 4096,
                url = "http://example.com:4096",
                source = DiscoverySource.SEED,
            )
        )
        val incoming = DiscoveredServer(
            serviceName = "seed:example.com:4096-updated",
            host = "example.com",
            port = 4096,
            url = "http://example.com:4096",
            source = DiscoverySource.SEED,
        )

        val merged = mergeDiscoveredServer(existing, incoming)

        assertEquals(listOf(incoming), merged)
    }

    @Test
    fun `endpointKey distinguishes different paths`() {
        assertNotEquals(
            endpointKey("http://example.com:4096/a"),
            endpointKey("http://example.com:4096/b"),
        )
    }

    @Test
    fun `endpointKey still dedupes equivalent root urls`() {
        assertEquals(
            endpointKey("example.com"),
            endpointKey("http://example.com:4096/?x=1#frag"),
        )
    }

    @Test
    fun `mergeDiscoveredServer keeps distinct entries for same host different path`() {
        val existing = listOf(
            DiscoveredServer(
                serviceName = "seed:example.com:4096:a",
                host = "example.com",
                port = 4096,
                url = "http://example.com:4096/a",
                source = DiscoverySource.SEED,
            )
        )
        val incoming = DiscoveredServer(
            serviceName = "seed:example.com:4096:b",
            host = "example.com",
            port = 4096,
            url = "http://example.com:4096/b",
            source = DiscoverySource.SEED,
        )

        val merged = mergeDiscoveredServer(existing, incoming)

        assertEquals(listOf(existing.first(), incoming), merged)
    }

    @Test
    fun `mdns discovered servers default to strict tls metadata`() {
        val server = DiscoveredServer(
            serviceName = "opencode-local",
            host = "example.com",
            port = 4096,
            url = "http://example.com:4096",
            source = DiscoverySource.MDNS,
        )

        assertFalse(server.allowInsecure)
    }
}
