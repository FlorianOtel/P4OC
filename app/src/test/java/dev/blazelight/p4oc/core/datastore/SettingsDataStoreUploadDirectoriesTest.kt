package dev.blazelight.p4oc.core.datastore

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsDataStoreUploadDirectoriesTest {
    @Test
    fun `entries are capped and the oldest entry is evicted`() {
        val current = (0 until MAX_LAST_UPLOAD_DIRECTORIES)
            .associateTo(linkedMapOf()) { "workspace-$it" to "/path/$it" }

        val updated = updateLastUploadDirectories(current, "workspace-new", "/path/new")

        assertEquals(MAX_LAST_UPLOAD_DIRECTORIES, updated.size)
        assertFalse(updated.containsKey("workspace-0"))
        assertEquals("/path/new", updated["workspace-new"])
    }

    @Test
    fun `updating an existing entry makes it most recent`() {
        val current = (0 until MAX_LAST_UPLOAD_DIRECTORIES)
            .associateTo(linkedMapOf()) { "workspace-$it" to "/path/$it" }

        val refreshed = updateLastUploadDirectories(current, "workspace-0", "/path/refreshed")
        val updated = updateLastUploadDirectories(refreshed, "workspace-new", "/path/new")

        assertEquals("/path/refreshed", updated["workspace-0"])
        assertFalse(updated.containsKey("workspace-1"))
        assertEquals("workspace-new", updated.keys.last())
    }

    @Test
    fun `blank path removes an entry`() {
        val current = linkedMapOf("workspace-1" to "/one", "workspace-2" to "/two")

        val updated = updateLastUploadDirectories(current, "workspace-1", " ")

        assertEquals(mapOf("workspace-2" to "/two"), updated)
    }

    @Test
    fun `blank workspace key leaves entries unchanged`() {
        val current = linkedMapOf("workspace" to "/path")

        val updated = updateLastUploadDirectories(current, " ", "/other")

        assertSame(current, updated)
    }

    @Test
    fun `malformed stored data recovers as an empty map`() {
        assertTrue(decodeLastUploadDirectories("not-json").isEmpty())
    }

    @Test
    fun `oversized stored data retains the most recent entries`() {
        val stored = (0..MAX_LAST_UPLOAD_DIRECTORIES)
            .associateTo(linkedMapOf()) { "workspace-$it" to "/path/$it" }

        val decoded = decodeLastUploadDirectories(Json.encodeToString(stored))

        assertEquals(MAX_LAST_UPLOAD_DIRECTORIES, decoded.size)
        assertFalse(decoded.containsKey("workspace-0"))
        assertEquals("workspace-$MAX_LAST_UPLOAD_DIRECTORIES", decoded.keys.last())
    }
}
