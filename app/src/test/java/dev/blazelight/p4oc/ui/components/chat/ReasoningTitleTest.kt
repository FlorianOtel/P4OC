package dev.blazelight.p4oc.ui.components.chat

import dev.blazelight.p4oc.domain.model.Part
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Test

class ReasoningTitleTest {
    @Test
    fun `prefers server metadata title`() {
        val part = reasoning(
            text = "Fallback first line",
            metadata = buildJsonObject { put("title", "Checking session persistence") },
        )

        assertEquals("Checking session persistence", reasoningDetailTitle(part))
    }

    @Test
    fun `uses cleaned first meaningful reasoning line as fallback`() {
        val part = reasoning(text = "\n## Comparing server and local state\nMore detail")

        assertEquals("Comparing server and local state", reasoningDetailTitle(part))
    }

    @Test
    fun `removes bold markers from collapsed title only`() {
        val part = reasoning(text = "**Adding full tests and formatting fixes**\nMore detail")

        assertEquals("Adding full tests and formatting fixes", reasoningDetailTitle(part))
        assertEquals("**Adding full tests and formatting fixes**\nMore detail", part.text)
    }

    @Test
    fun `falls back to first reasoning line when metadata title is not a string`() {
        val part = reasoning(
            text = "## Comparing server and local state\nMore detail",
            metadata = buildJsonObject {
                put("title", buildJsonArray { add(JsonPrimitive("nested")) })
            },
        )

        assertEquals("Comparing server and local state", reasoningDetailTitle(part))
    }

    @Test
    fun `falls back to first reasoning line when metadata title is a number`() {
        val part = reasoning(
            text = "Resolving auth handshake",
            metadata = buildJsonObject {
                put("title", JsonPrimitive(42))
            },
        )

        assertEquals("Resolving auth handshake", reasoningDetailTitle(part))
    }

    private fun reasoning(
        text: String,
        metadata: kotlinx.serialization.json.JsonObject? = null,
    ) = Part.Reasoning(
        id = "part",
        sessionID = "session",
        messageID = "message",
        text = text,
        metadata = metadata,
    )
}
