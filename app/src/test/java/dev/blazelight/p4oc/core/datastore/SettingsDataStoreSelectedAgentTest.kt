package dev.blazelight.p4oc.core.datastore

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class SettingsDataStoreSelectedAgentTest {
    @Test
    fun `lookup returns stored agent and null for unknown session`() {
        val stored = """{"session-1":"build","session-2":"plan"}"""

        assertEquals("build", selectedAgentForSession(stored, "session-1"))
        assertNull(selectedAgentForSession(stored, "unknown"))
    }

    @Test
    fun `oldest selection is evicted when cap is exceeded`() {
        var stored: String? = null
        repeat(MAX_SESSION_AGENT_SELECTIONS + 1) { index ->
            stored = updatedSessionAgentSelections(stored, "session-$index", "agent-$index")
        }

        val selections = Json.decodeFromString<LinkedHashMap<String, String>>(checkNotNull(stored))
        assertEquals(MAX_SESSION_AGENT_SELECTIONS, selections.size)
        assertFalse("session-0" in selections)
        assertEquals("agent-1", selections["session-1"])
        assertEquals("agent-${MAX_SESSION_AGENT_SELECTIONS}", selections["session-$MAX_SESSION_AGENT_SELECTIONS"])
    }

    @Test
    fun `updating an existing selection makes it most recent`() {
        var stored: String? = null
        repeat(MAX_SESSION_AGENT_SELECTIONS) { index ->
            stored = updatedSessionAgentSelections(stored, "session-$index", "agent-$index")
        }

        stored = updatedSessionAgentSelections(stored, "session-0", "updated")
        stored = updatedSessionAgentSelections(stored, "new-session", "new-agent")

        val selections = Json.decodeFromString<LinkedHashMap<String, String>>(stored)
        assertEquals("updated", selections["session-0"])
        assertFalse("session-1" in selections)
        assertEquals("session-0", selections.keys.elementAt(selections.size - 2))
        assertEquals("new-session", selections.keys.last())
    }

    @Test
    fun `malformed state recovers for lookup and next write`() {
        assertNull(selectedAgentForSession("not-json", "session"))

        val recovered = updatedSessionAgentSelections("not-json", "session", "build")

        assertEquals("build", selectedAgentForSession(recovered, "session"))
        assertEquals(mapOf("session" to "build"), Json.decodeFromString<Map<String, String>>(recovered))
    }
}
