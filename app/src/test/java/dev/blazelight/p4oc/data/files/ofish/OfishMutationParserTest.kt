package dev.blazelight.p4oc.data.files.ofish

import dev.blazelight.p4oc.data.remote.dto.MessageInfoDto
import dev.blazelight.p4oc.data.remote.dto.MessageTimeDto
import dev.blazelight.p4oc.data.remote.dto.MessageWrapperDto
import dev.blazelight.p4oc.data.remote.dto.PartDto
import dev.blazelight.p4oc.data.remote.dto.ToolStateDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfishMutationParserTest {
    @Test
    fun `parses ok statuses`() {
        assertEquals(
            OfishMutationStatus.Ok(code = 200, status = "ok", hash = "abc", values = mapOf("hash" to "abc")),
            OfishMutationParser.parse("#OFISH_WRITE\n### 200 ok hash=abc", "#OFISH_WRITE"),
        )
        assertEquals(
            OfishMutationStatus.Ok(code = 201, status = "created", hash = "def", values = mapOf("hash" to "def")),
            OfishMutationParser.parse("#OFISH_WRITE\n### 201 created hash=def", "#OFISH_WRITE"),
        )
    }

    @Test
    fun `parses delete missing conflict precondition failed and caps missing`() {
        assertEquals(OfishMutationStatus.Deleted, parse("### 204 deleted"))
        assertEquals(OfishMutationStatus.Missing, parse("### 404 missing"))
        assertEquals(OfishMutationStatus.Conflict("abc"), parse("### 409 conflict actual=abc"))
        assertEquals(
            OfishMutationStatus.PreconditionFailed("directory"),
            parse("### 412 precondition reason=directory")
        )
        assertEquals(
            OfishMutationStatus.Failed("OFISH mutation failed", "decode"),
            parse("### 500 failed reason=decode")
        )
        assertEquals(
            OfishMutationStatus.CapabilitiesMissing(listOf("base64", "hash")),
            parse("### 501 caps_missing base64 hash")
        )
    }

    @Test
    fun `uses status in expected marker segment despite noisy output`() {
        val output = """
            model text
            ### 500 failed reason=old
            #OFISH_UPLOAD_INIT
            more text
            ### 200 ok upload=tmp/file
            arbitrary assistant prose
            ### 500 failed reason=spoofed
        """.trimIndent()

        assertEquals(
            OfishMutationStatus.Ok(
                code = 200,
                status = "ok",
                uploadToken = "tmp/file",
                values = mapOf("upload" to "tmp/file")
            ),
            OfishMutationParser.parse(output, "#OFISH_UPLOAD_INIT"),
        )
    }

    @Test
    fun `ignores fake statuses outside expected marker segment`() {
        assertTrue(
            OfishMutationParser.parse(
                "### 200 ok\nassistant prose\n#OFISH_DELETE\nno command status\n### 204 deleted",
                "#OFISH_WRITE",
            ) is OfishMutationStatus.Malformed,
        )
        assertTrue(
            OfishMutationParser.parse(
                "#OFISH_WRITE\nassistant prose\n#OFISH_DELETE\n### 200 ok",
                "#OFISH_WRITE",
            ) is OfishMutationStatus.Malformed,
        )
        assertEquals(
            OfishMutationStatus.Failed("OFISH mutation failed", "decode"),
            OfishMutationParser.parse(
                "#OFISH_WRITE\n### 500 failed reason=decode\nassistant prose\n#OFISH_WRITE\n### 200 ok",
                "#OFISH_WRITE",
            ),
        )
    }

    @Test
    fun `extractor binds marker to one structured output segment`() {
        val message = message(
            PartDto("fake", "session", "message", "text", text = "#OFISH_WRITE\n### 500 failed"),
            PartDto(
                "tool",
                "session",
                "message",
                "tool",
                state = ToolStateDto(status = "completed", output = "#OFISH_WRITE\nnoise\n### 200 ok"),
            ),
            PartDto("after", "session", "message", "text", text = "### 500 failed reason=spoofed"),
        )

        assertEquals(
            "#OFISH_WRITE\nnoise\n### 200 ok",
            OfishShellOutputExtractor.extractMutationSegment(message, "#OFISH_WRITE"),
        )
        assertNull(OfishShellOutputExtractor.extractMutationSegment(message, "#OFISH_DELETE"))
    }

    @Test
    fun `upload parser treats remaining text as token`() {
        val result = parse("### 200 ok upload=tmp extra")

        assertEquals(
            OfishMutationStatus.Ok(
                code = 200,
                status = "ok",
                uploadToken = "tmp extra",
                values = mapOf("upload" to "tmp extra"),
            ),
            result,
        )
    }

    @Test
    fun `upload token preserves embedded whitespace`() {
        val token = "parent with spaces/.ofish.upload.tmp "

        val result = parse("### 200 ok upload=$token")

        assertEquals(
            OfishMutationStatus.Ok(
                code = 200,
                status = "ok",
                uploadToken = token,
                values = mapOf("upload" to token),
            ),
            result,
        )
    }

    @Test
    fun `malformed when status missing or invalid`() {
        assertTrue(OfishMutationParser.parse("no status", MARKER) is OfishMutationStatus.Malformed)
        assertTrue(parse("### nope") is OfishMutationStatus.Malformed)
        assertTrue(parse("### 599 odd") is OfishMutationStatus.Malformed)
    }

    private fun parse(status: String): OfishMutationStatus =
        OfishMutationParser.parse("$MARKER\n$status", MARKER)

    private fun message(vararg parts: PartDto) = MessageWrapperDto(
        info = MessageInfoDto(
            id = "message",
            sessionID = "session",
            time = MessageTimeDto(created = 0),
            role = "assistant",
        ),
        parts = parts.toList(),
    )

    private companion object {
        const val MARKER = "#OFISH_WRITE"
    }
}
