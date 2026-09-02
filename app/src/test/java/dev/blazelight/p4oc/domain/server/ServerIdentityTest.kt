package dev.blazelight.p4oc.domain.server

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerIdentityTest {
    @Test
    fun `meaningful discovery name wins over endpoint hostname`() {
        val identity = ServerIdentity.derive(
            endpoint = "https://build-box.local:4096",
            candidateName = "  Jasmin Workstation  ",
        )

        assertEquals("Jasmin Workstation", identity.displayName)
        assertEquals("JWMX", identity.badgeLabel)
    }

    @Test
    fun `generic legacy names migrate to recognizable hostname identity`() {
        listOf("Remote Server", "Remote", "Server", "OpenCode Server", "  ").forEach { legacyName ->
            val identity = ServerIdentity.derive("https://build-box.local:4096", legacyName)

            assertEquals("build-box.local", identity.displayName)
            assertTrue(identity.badgeLabel.startsWith("BB"))
        }
    }

    @Test
    fun `numeric endpoint falls back to host and port so targets remain distinguishable`() {
        val first = ServerIdentity.derive("http://192.168.1.4:4096")
        val second = ServerIdentity.derive("http://192.168.1.4:5096")

        assertEquals("192.168.1.4:4096", first.displayName)
        assertEquals("192.168.1.4:5096", second.displayName)
        assertNotEquals(first.badgeLabel, second.badgeLabel)
    }

    @Test
    fun `badge is stable across equivalent endpoint spellings and reload`() {
        val original = ServerIdentity.derive("BUILD-BOX.local", "Jasmin Workstation")
        val normalizedReload = ServerIdentity.derive(
            "http://build-box.local:4096/?ignored=true#fragment",
            "Jasmin Workstation",
        )

        assertEquals(original, normalizedReload)
    }

    @Test
    fun `same human initials on different servers have disambiguated badges`() {
        val alpha = ServerIdentity.derive("http://alpha.example:4096", "Build Box")
        val beta = ServerIdentity.derive("http://beta.example:4096", "Build Box")

        assertTrue(alpha.badgeLabel.startsWith("BB"))
        assertTrue(beta.badgeLabel.startsWith("BB"))
        assertNotEquals(alpha.badgeLabel, beta.badgeLabel)
    }

    @Test
    fun `rename updates human badge stem without changing canonical endpoint ownership`() {
        val before = ServerRef.fromEndpoint("build-box.local", "Build Box")
        val after = ServerRef.fromEndpoint("http://build-box.local:4096", "Jasmin Workstation")

        assertEquals(before, after)
        assertEquals(before.hashCode(), after.hashCode())
        assertTrue(before.badgeLabel.startsWith("BB"))
        assertTrue(after.badgeLabel.startsWith("JW"))
        assertNotEquals(before.badgeLabel, after.badgeLabel)
    }

    @Test
    fun `invalid endpoint cannot manufacture a server identity`() {
        assertThrows(IllegalArgumentException::class.java) {
            ServerIdentity.derive("ftp://build-box.local", "Build Box")
        }
    }
}
