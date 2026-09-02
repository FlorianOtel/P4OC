package dev.blazelight.p4oc.core.datastore

import dev.blazelight.p4oc.data.remote.dto.ModelInput
import dev.blazelight.p4oc.domain.server.ServerRef
import dev.blazelight.p4oc.domain.workspace.Workspace
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsDataStoreSelectedModelTest {
    @Test
    fun `composer selection round trips explicit variant and default`() {
        val model = ModelInput(providerID = "openai", modelID = "gpt-5")
        val high = SessionComposerSelection(model = model, variant = "high", pendingServerSync = true)
        val selectionKey = "workspace-session"

        val withHigh = updatedSessionComposerSelections(null, selectionKey, high)
        assertEquals(high, composerSelectionForSession(withHigh, selectionKey))

        val explicitDefault = high.copy(variant = null)
        val withDefault = updatedSessionComposerSelections(withHigh, selectionKey, explicitDefault)
        assertEquals(explicitDefault, composerSelectionForSession(withDefault, selectionKey))
    }

    @Test
    fun `composer selection key isolates server workspace and session`() {
        val serverA = ServerRef.fromEndpointKey("http://server-a")
        val serverB = ServerRef.fromEndpointKey("http://server-b")

        val keys = setOf(
            sessionComposerSelectionKey(Workspace(serverA, "/repo-a"), "session-1"),
            sessionComposerSelectionKey(Workspace(serverA, "/repo-b"), "session-1"),
            sessionComposerSelectionKey(Workspace(serverB, "/repo-a"), "session-1"),
            sessionComposerSelectionKey(Workspace(serverA, "/repo-a"), "session-2"),
        )

        assertEquals(4, keys.size)
    }

    @Test
    fun `composer selection key round trips through the same server workspace and session`() {
        val server = ServerRef.fromEndpointKey("http://server-a")
        val workspace = Workspace(server, "/repo-a")

        val key = sessionComposerSelectionKey(workspace, "session-1")

        val stored = updatedSessionComposerSelections(
            null,
            key,
            SessionComposerSelection(model = ModelInput(providerID = "openai", modelID = "gpt-5")),
        )

        assertEquals(
            ModelInput(providerID = "openai", modelID = "gpt-5"),
            composerSelectionForSession(stored, key)?.model,
        )
    }

    @Test
    fun `malformed composer state recovers for lookup and next write`() {
        assertNull(composerSelectionForSession("not-json", "session"))
        val selection = SessionComposerSelection(
            model = ModelInput(providerID = "anthropic", modelID = "claude-3"),
            variant = "high",
        )

        val recovered = updatedSessionComposerSelections("not-json", "session", selection)

        assertEquals(selection, composerSelectionForSession(recovered, "session"))
    }

    @Test
    fun `lookup returns stored selection and null for unknown key`() {
        val selection = SessionComposerSelection(
            model = ModelInput(providerID = "anthropic", modelID = "claude-3"),
            variant = "high",
            pendingServerSync = true,
        )
        val stored = updatedSessionComposerSelections(null, "session-1", selection)

        assertEquals(selection, composerSelectionForSession(stored, "session-1"))
        assertNull(composerSelectionForSession(stored, "session-2"))
    }

    @Test
    fun `oldest composer selection is evicted when cap is exceeded`() {
        var stored: String? = null
        repeat(MAX_SESSION_COMPOSER_SELECTIONS + 1) { index ->
            stored = updatedSessionComposerSelections(
                stored,
                "session-$index",
                SessionComposerSelection(
                    model = ModelInput(providerID = "provider", modelID = "model-$index"),
                ),
            )
        }

        val selections =
            Json.decodeFromString<LinkedHashMap<String, SessionComposerSelection>>(checkNotNull(stored))
        assertEquals(MAX_SESSION_COMPOSER_SELECTIONS, selections.size)
        assertFalse("session-0" in selections)
        assertTrue("session-1" in selections)
        assertTrue("session-${MAX_SESSION_COMPOSER_SELECTIONS}" in selections)
    }

    @Test
    fun `re-write moves an existing selection to the newest position`() {
        var stored: String? = null
        stored = updatedSessionComposerSelections(
            stored,
            "session-a",
            SessionComposerSelection(model = ModelInput(providerID = "p", modelID = "a")),
        )
        stored = updatedSessionComposerSelections(
            stored,
            "session-b",
            SessionComposerSelection(model = ModelInput(providerID = "p", modelID = "b")),
        )
        stored = updatedSessionComposerSelections(
            stored,
            "session-a",
            SessionComposerSelection(model = ModelInput(providerID = "p", modelID = "a2")),
        )

        val selections =
            Json.decodeFromString<LinkedHashMap<String, SessionComposerSelection>>(checkNotNull(stored))
        // Re-wrote session-a last, so it moves to the newest position and is evicted after session-b.
        assertEquals(listOf("session-b", "session-a"), selections.keys.toList())
        assertEquals("p/a2", selections["session-a"]?.let { "${it.model.providerID}/${it.model.modelID}" })
    }
}
