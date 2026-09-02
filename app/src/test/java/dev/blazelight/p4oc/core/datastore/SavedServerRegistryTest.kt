package dev.blazelight.p4oc.core.datastore

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedServerRegistryTest {

    @Test
    fun `fromConnection preserves no-port endpoint for display and canonicalizes endpoint key`() {
        val server = SavedServerRegistry.fromConnection(
            url = "https://my-host.example.com",
            name = "Remote",
            username = "opencode",
        )

        assertEquals("https://my-host.example.com", server.endpoint)
        assertEquals("https://my-host.example.com:4096", server.endpointKey)
        assertEquals(server.endpointKey, server.id)
        assertEquals("my-host.example.com", server.displayName)
        assertEquals("opencode", server.username)
    }

    @Test
    fun `merge keeps authoritative TLS configuration when deduping endpoint forms`() {
        val bare = SavedServerRegistry.fromConnection(
            url = "https://my-host.example.com",
            name = "Remote",
        )
        val explicit = SavedServerRegistry.fromConnection(
            url = "https://my-host.example.com:4096",
            name = "Updated Remote",
            allowInsecure = true,
            pinned = true,
        )

        val merged = SavedServerRegistry.merge(listOf(bare, explicit))

        assertEquals(1, merged.size)
        assertEquals("https://my-host.example.com", merged.single().endpoint)
        assertEquals("https://my-host.example.com:4096", merged.single().endpointKey)
        assertFalse(merged.single().allowInsecure)
        assertTrue(merged.single().pinned)
    }

    @Test
    fun `upsert replaces existing server by stable id`() {
        val original = SavedServerRegistry.fromConnection(
            url = "http://alpha.example.com",
            name = "Alpha",
        )
        val beta = SavedServerRegistry.fromConnection(
            url = "http://beta.example.com",
            name = "Beta",
        )
        val edited = original.copy(
            displayName = "Alpha edited",
            username = "jasmin",
            defaultWorkspace = "/work/p4oc",
        )

        val updated = SavedServerRegistry.upsert(listOf(original, beta), edited)

        assertEquals(2, updated.size)
        assertEquals("Alpha edited", updated.first { it.id == original.id }.displayName)
        assertEquals("jasmin", updated.first { it.id == original.id }.username)
        assertEquals("/work/p4oc", updated.first { it.id == original.id }.defaultWorkspace)
        assertEquals("Beta", updated.first { it.id == beta.id }.displayName)
    }

    @Test
    fun `merge surfaces migrated last connection and recent servers without data loss`() {
        val lastConnection = SavedServerRegistry.fromConnection(
            url = "https://alpha.example.com",
            name = "Alpha last",
            username = "last-user",
        )
        val recentDuplicate = SavedServerRegistry.fromConnection(
            url = "https://alpha.example.com:4096",
            name = "Alpha recent",
            allowInsecure = true,
        )
        val recentOnly = SavedServerRegistry.fromConnection(
            url = "http://beta.example.com:9999",
            name = "Beta recent",
        )

        val migrated = SavedServerRegistry.merge(listOf(lastConnection, recentDuplicate, recentOnly))

        assertEquals(2, migrated.size)
        val alpha = migrated.first { it.endpointKey == "https://alpha.example.com:4096" }
        assertEquals("https://alpha.example.com", alpha.endpoint)
        assertEquals("last-user", alpha.username)
        assertFalse(alpha.allowInsecure)
        assertTrue(migrated.any { it.displayName == "Beta recent" })
    }

    @Test
    fun `secure upsert stays secure when merged with stale insecure representations`() {
        val insecure = SavedServerRegistry.fromConnection(
            url = "https://alpha.example.com",
            name = "Alpha",
            allowInsecure = true,
            pinned = true,
        )
        val secure = insecure.copy(allowInsecure = false)

        val resaved = SavedServerRegistry.upsert(listOf(insecure), secure).single()
        val merged = SavedServerRegistry.merge(
            listOf(
                resaved,
                insecure.copy(endpoint = "https://alpha.example.com:4096"),
            ),
        ).single()

        assertFalse(resaved.allowInsecure)
        assertFalse(merged.allowInsecure)
        assertTrue(merged.pinned)
    }

    @Test
    fun `normalize migrates legacy generic name without changing durable endpoint id`() {
        val legacy = SavedServer(
            id = "https://build-box.local:4096",
            endpoint = "https://build-box.local",
            endpointKey = "https://build-box.local:4096",
            displayName = "Remote Server",
            pinned = true,
        )

        val migrated = SavedServerRegistry.normalize(legacy)

        assertEquals(legacy.id, migrated.id)
        assertEquals(legacy.endpointKey, migrated.endpointKey)
        assertEquals("build-box.local", migrated.displayName)
        assertEquals(migrated.badgeLabel, SavedServerRegistry.normalize(migrated).badgeLabel)
        assertTrue(migrated.pinned)
    }

    @Test
    fun `rename preserves endpoint identity while updating reusable badge identity`() {
        val original = SavedServerRegistry.fromConnection(
            url = "https://build-box.local",
            name = "Build Box",
        )
        val renamed = SavedServerRegistry.upsert(
            current = listOf(original),
            server = original.copy(displayName = "Jasmin Workstation"),
        ).single()

        assertEquals(original.id, renamed.id)
        assertEquals(original.endpointKey, renamed.endpointKey)
        assertEquals("Jasmin Workstation", renamed.displayName)
        assertTrue(original.badgeLabel.startsWith("BB"))
        assertTrue(renamed.badgeLabel.startsWith("JW"))
    }

    @Test
    fun `saved server model has no password or api key field`() {
        val propertyNames = SavedServer::class.java.declaredFields.map { it.name }

        assertFalse(propertyNames.any { it.contains("password", ignoreCase = true) })
        assertFalse(propertyNames.any { it.contains("apiKey", ignoreCase = true) })
        assertFalse(propertyNames.any { it.contains("token", ignoreCase = true) })
    }
}
