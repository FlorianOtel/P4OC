package dev.blazelight.p4oc.ui.components.toolwidgets

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ApplyPatchParserInputTest {

    @Test
    fun `parses add update and delete file sections in order`() {
        val files = parseApplyPatchSections(
            """
            *** Begin Patch
            *** Update File: src/Main.kt
            @@
            -old
            +new
            *** Add File: src/New.kt
            +content
            +more
            *** Delete File: src/Old.kt
            *** End Patch
            """.trimIndent()
        )

        assertNotNull(files)
        assertEquals(
            listOf(
                PatchFileChange("src/Main.kt", PatchFileAction.UPDATE, additions = 1, deletions = 1),
                PatchFileChange("src/New.kt", PatchFileAction.ADD, additions = 2, deletions = 0),
                PatchFileChange("src/Old.kt", PatchFileAction.DELETE, additions = 0, deletions = null),
            ),
            files,
        )
    }

    @Test
    fun `preserves duplicate sections without dedup`() {
        val files = parseApplyPatchSections(
            """
            *** Begin Patch
            *** Add File: a.kt
            +one
            *** Add File: a.kt
            +two
            *** End Patch
            """.trimIndent()
        )

        assertNotNull(files)
        assertEquals(2, files?.size)
        assertEquals(
            listOf(
                PatchFileChange("a.kt", PatchFileAction.ADD, additions = 1, deletions = 0),
                PatchFileChange("a.kt", PatchFileAction.ADD, additions = 1, deletions = 0),
            ),
            files,
        )
    }

    @Test
    fun `parses exact move grammar with source and destination`() {
        val files = parseApplyPatchSections(
            """
            *** Begin Patch
            *** Update File: src/Old.kt
            *** Move to: src/New.kt
            @@
            -old
            +new
            *** End Patch
            """.trimIndent()
        )

        assertNotNull(files)
        assertEquals(
            listOf(
                PatchFileChange("src/Old.kt", PatchFileAction.MOVE, "src/New.kt", additions = 1, deletions = 1),
            ),
            files,
        )
    }

    @Test
    fun `rejects patch without end marker`() {
        val files = parseApplyPatchSections(
            """
            *** Begin Patch
            *** Add File: a.kt
            +content
            """.trimIndent()
        )
        assertNull(files)
    }

    @Test
    fun `rejects patch without begin marker`() {
        val files = parseApplyPatchSections(
            """
            *** Add File: a.kt
            +content
            *** End Patch
            """.trimIndent()
        )
        assertNull(files)
    }

    @Test
    fun `rejects empty patch`() {
        val files = parseApplyPatchSections("*** Begin Patch\n*** End Patch")
        assertNull(files)
    }

    @Test
    fun `rejects standalone move directive`() {
        val files = parseApplyPatchSections(
            """
            *** Begin Patch
            *** Move to: b.kt
            *** End Patch
            """.trimIndent()
        )
        assertNull(files)
    }

    @Test
    fun `rejects move not immediately after update header`() {
        val files = parseApplyPatchSections(
            """
            *** Begin Patch
            *** Update File: a.kt
            @@
            -old
            +new
            *** Move to: b.kt
            *** End Patch
            """.trimIndent()
        )
        assertNull(files)
    }

    @Test
    fun `rejects empty path`() {
        val files = parseApplyPatchSections(
            """
            *** Begin Patch
            *** Add File:
            +content
            *** End Patch
            """.trimIndent()
        )
        assertNull(files)
    }

    @Test
    fun `rejects stray content inside a section`() {
        val files = parseApplyPatchSections(
            """
            *** Begin Patch
            *** Update File: a.kt
            @@
            -old
            random junk
            +new
            *** End Patch
            """.trimIndent()
        )
        assertNull(files)
    }

    @Test
    fun `rejects update without any body lines`() {
        val files = parseApplyPatchSections(
            """
            *** Begin Patch
            *** Update File: a.kt
            *** End Patch
            """.trimIndent()
        )
        assertNull(files)
    }

    @Test
    fun `uses legacy patch key only when patchText is absent`() {
        val input = buildJsonObject {
            put("patch", "*** Begin Patch\n*** Add File: a.kt\n+x\n*** End Patch")
        }
        assertEquals(PatchInput.Valid::class, extractPatchInput(input)::class)
    }

    @Test
    fun `present malformed patchText fails closed without falling back`() {
        val input = buildJsonObject {
            put(
                "patchText",
                buildJsonArray {
                    add("a")
                    add("b")
                },
            ) // JsonArray, not a string
            put("patch", "*** Begin Patch\n*** Add File: a.kt\n+x\n*** End Patch")
        }
        assertEquals(PatchInput.Malformed, extractPatchInput(input))
    }

    @Test
    fun `non string patchText of object or number is malformed`() {
        assertEquals(PatchInput.Malformed, extractPatchInput(buildJsonObject { put("patchText", 42) }))
        assertEquals(PatchInput.Malformed, extractPatchInput(buildJsonObject { put("patchText", true) }))
        assertEquals(PatchInput.Malformed, extractPatchInput(buildJsonObject { put("patchText", buildJsonObject {}) }))
    }

    @Test
    fun `numeric and boolean patchText are malformed despite json content coercion`() {
        // JsonPrimitive.contentOrNull coerces these to "42"/"true", so extraction must require isString.
        assertEquals(PatchInput.Malformed, extractPatchInput(buildJsonObject { put("patchText", 4.2) }))
        assertEquals(PatchInput.Malformed, extractPatchInput(buildJsonObject { put("patchText", false) }))
        assertEquals(PatchInput.Malformed, extractPatchInput(buildJsonObject { put("patchText", JsonNull) }))
    }

    @Test
    fun `numeric and boolean legacy patch are malformed`() {
        assertEquals(PatchInput.Malformed, extractPatchInput(buildJsonObject { put("patch", 7) }))
        assertEquals(PatchInput.Malformed, extractPatchInput(buildJsonObject { put("patch", true) }))
    }

    @Test
    fun `absent patchText and patch is absent`() {
        assertEquals(PatchInput.Absent, extractPatchInput(buildJsonObject {}))
    }
}
