package dev.blazelight.p4oc.data.remote.dto

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.putJsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelReasoningEffortsTest {
    @Test
    fun `reads reasoning efforts from top level variants in server order`() {
        val model = model(
            variants = buildJsonObject {
                putJsonObject("max") {}
                putJsonObject("low") {}
                putJsonObject("medium") {}
                putJsonObject("high") {}
                putJsonObject("minimal") {}
            }
        )

        assertEquals(listOf("max", "low", "medium", "high", "minimal"), model.reasoningEfforts())
    }

    @Test
    fun `reads reasoning efforts from variants option in server order`() {
        val model = model(
            options = buildJsonObject {
                putJsonObject("variants") {
                    putJsonObject("max") {}
                    putJsonObject("low") {}
                    putJsonObject("medium") {}
                    putJsonObject("high") {}
                    putJsonObject("minimal") {}
                }
            }
        )

        assertEquals(listOf("max", "low", "medium", "high", "minimal"), model.reasoningEfforts())
    }

    @Test
    fun `reads reasoning efforts from singular variant option`() {
        val model = model(
            options = buildJsonObject {
                putJsonObject("variant") {
                    putJsonObject("high") {}
                    putJsonObject("max") {}
                }
            }
        )

        assertEquals(listOf("high", "max"), model.reasoningEfforts())
    }

    @Test
    fun `does not infer efforts for DeepSeek V4 reasoning model without variants`() {
        val model = model(id = "deepseek-v4-pro")

        assertEquals(emptyList<String>(), model.reasoningEfforts())
    }

    @Test
    fun `does not infer efforts for non DeepSeek reasoning model without variants`() {
        val model = model(id = "claude-sonnet-4", providerId = "anthropic")

        assertEquals(emptyList<String>(), model.reasoningEfforts())
    }

    @Test
    fun `send message serializes selected effort as opencode variant`() {
        val request = SendMessageRequest(
            variant = "high",
            parts = listOf(PartInputDto(type = "text", text = "hello"))
        )

        val json = Json.encodeToString(request)

        assertTrue(json.contains("\"variant\":\"high\""))
        assertFalse(json.contains("reasoningEffort"))
    }

    private fun model(
        id: String = "deepseek-v4-flash",
        providerId: String = "deepseek",
        options: kotlinx.serialization.json.JsonObject? = null,
        variants: kotlinx.serialization.json.JsonObject? = null
    ) = ModelDto(
        id = id,
        providerId = providerId,
        name = id,
        capabilities = ModelCapabilitiesDto(reasoning = true),
        options = options,
        variants = variants
    )
}
