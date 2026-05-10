package dev.blazelight.p4oc.data.files.ofish

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfishMutationParserTest {
    @Test
    fun `parses ok statuses`() {
        assertEquals(
            OfishMutationStatus.Ok(code = 200, status = "ok", hash = "abc", values = mapOf("hash" to "abc")),
            OfishMutationParser.parse("### 200 ok hash=abc"),
        )
        assertEquals(
            OfishMutationStatus.Ok(code = 201, status = "created", hash = "def", values = mapOf("hash" to "def")),
            OfishMutationParser.parse("### 201 created hash=def"),
        )
    }

    @Test
    fun `parses delete missing conflict precondition failed and caps missing`() {
        assertEquals(OfishMutationStatus.Deleted, OfishMutationParser.parse("### 204 deleted"))
        assertEquals(OfishMutationStatus.Missing, OfishMutationParser.parse("### 404 missing"))
        assertEquals(OfishMutationStatus.Conflict("abc"), OfishMutationParser.parse("### 409 conflict actual=abc"))
        assertEquals(
            OfishMutationStatus.PreconditionFailed("directory"),
            OfishMutationParser.parse("### 412 precondition reason=directory")
        )
        assertEquals(
            OfishMutationStatus.Failed("OFISH mutation failed", "decode"),
            OfishMutationParser.parse("### 500 failed reason=decode")
        )
        assertEquals(
            OfishMutationStatus.CapabilitiesMissing(listOf("base64", "hash")),
            OfishMutationParser.parse("### 501 caps_missing base64 hash")
        )
    }

    @Test
    fun `uses last status line in noisy output`() {
        val output = """
            model text
            ### 500 failed reason=old
            more text
            ### 200 ok upload=tmp/file
        """.trimIndent()

        assertEquals(
            OfishMutationStatus.Ok(
                code = 200,
                status = "ok",
                uploadToken = "tmp/file",
                values = mapOf("upload" to "tmp/file")
            ),
            OfishMutationParser.parse(output),
        )
    }

    @Test
    fun `key value parser keeps values whitespace bounded`() {
        val result = OfishMutationParser.parse("### 200 ok upload=tmp extra")

        assertEquals(
            OfishMutationStatus.Ok(code = 200, status = "ok", uploadToken = "tmp", values = mapOf("upload" to "tmp")),
            result,
        )
    }

    @Test
    fun `malformed when status missing or invalid`() {
        assertTrue(OfishMutationParser.parse("no status") is OfishMutationStatus.Malformed)
        assertTrue(OfishMutationParser.parse("### nope") is OfishMutationStatus.Malformed)
        assertTrue(OfishMutationParser.parse("### 599 odd") is OfishMutationStatus.Malformed)
    }
}
