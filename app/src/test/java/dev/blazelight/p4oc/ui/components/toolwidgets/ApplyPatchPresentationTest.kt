package dev.blazelight.p4oc.ui.components.toolwidgets

import dev.blazelight.p4oc.domain.model.Part
import dev.blazelight.p4oc.domain.model.ToolState
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ApplyPatchPresentationTest {

    @Test
    fun `compact description reports file count with correct plural`() {
        val tool = tool(
            patchInput("*** Begin Patch\n*** Update File: a.kt\n@@\n context\n*** Add File: b.kt\n+x\n*** End Patch"),
            metadata = null,
            state = completed(),
        )
        // Two files; one added line (+1) and zero deletions, so only the nonzero +1 total is shown.
        assertEquals("Patch: 2 files +1", applyPatchCompactDescription(tool))
    }

    @Test
    fun `compact description uses singular for one file`() {
        val tool = tool(
            patchInput("*** Begin Patch\n*** Update File: a.kt\n@@\n context\n*** End Patch"),
            metadata = null,
            state = completed(),
        )
        // A single context-only update: one file, no additions/deletions.
        assertEquals("Patch: 1 file", applyPatchCompactDescription(tool))
    }

    @Test
    fun `compact description omits unknown delete total`() {
        val tool = tool(
            patchInput("*** Begin Patch\n*** Delete File: a.kt\n*** End Patch"),
            metadata = null,
            state = completed(),
        )
        assertEquals("Patch: 1 file", applyPatchCompactDescription(tool))
    }

    @Test
    fun `compact description includes known nonzero totals from input without metadata`() {
        val tool = tool(
            patchInput(
                "*** Begin Patch\n*** Update File: a.kt\n@@\n-old\n+new\n*** Add File: b.kt\n+x\n+y\n*** End Patch",
            ),
            metadata = null,
            state = completed(),
        )
        // Input-derived +/- counts are exact for represented add/update/move lines; totals show when
        // complete and nonzero, even without authoritative metadata.
        assertEquals("Patch: 2 files +3 -1", applyPatchCompactDescription(tool))
    }

    @Test
    fun `compact description includes metadata totals and omits unknown delete total`() {
        val metadata = buildJsonObject {
            put(
                "files",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("relativePath", "a.kt")
                            put("type", "update")
                            put("additions", 3)
                            put("deletions", 1)
                        },
                    )
                    add(
                        buildJsonObject {
                            put("relativePath", "b.kt")
                            put("type", "add")
                            put("additions", 2)
                            put("deletions", 0)
                        },
                    )
                },
            )
        }
        val tool = tool(
            patchInput(
                "*** Begin Patch\n*** Update File: a.kt\n@@\n-x\n+x\n+y\n+z\n*** Add File: b.kt\n+a\n+b\n*** End Patch",
            ),
            metadata,
            completed(),
        )
        // Deletions total is +0 for b.kt and +1 for a.kt → -1 known; additions +5 known.
        assertEquals("Patch: 2 files +5 -1", applyPatchCompactDescription(tool))
    }

    @Test
    fun `compact description falls back to tool name on error`() {
        val tool = tool(
            patchInput("*** Begin Patch\n*** Add File: a.kt\n+x\n*** End Patch"),
            metadata = null,
            state = errored(),
        )
        assertEquals("apply_patch", applyPatchCompactDescription(tool))
    }

    @Test
    fun `compact description falls back to tool name on unreadable payload`() {
        val tool = tool(
            buildJsonObject { put("patchText", buildJsonArray { add("nope") }) },
            metadata = null,
            state = completed(),
        )
        assertEquals("apply_patch", applyPatchCompactDescription(tool))
    }

    @Test
    fun `error resolver may parse attempted input but compact hides it`() {
        val patch = "*** Begin Patch\n*** Add File: a.kt\n+x\n*** End Patch"
        val tool = tool(
            patchInput(patch),
            metadata = null,
            state = errored(),
        )
        // The resolver still knows the attempted patch…
        val resolution = resolveApplyPatch(tool)
        assertNotNull(resolution)
        assertEquals(patch, (resolution as PatchResolution.Valid).text)
        // …but the compact/display layer is truthful and never summarizes attempted input.
        assertEquals("apply_patch", applyPatchCompactDescription(tool))
    }

    @Test
    fun `error header summary is null so header shows tool name only`() {
        val tool = tool(
            patchInput("*** Begin Patch\n*** Add File: a.kt\n+x\n*** End Patch"),
            metadata = null,
            state = errored(),
        )
        assertNull(applyPatchHeaderSummary(errored(), resolveApplyPatch(tool)))
    }

    @Test
    fun `completed header summary matches compact summary`() {
        val tool = tool(
            patchInput("*** Begin Patch\n*** Add File: a.kt\n+x\n*** End Patch"),
            metadata = null,
            state = completed(),
        )
        assertEquals(
            applyPatchCompactDescription(tool),
            applyPatchHeaderSummary(completed(), resolveApplyPatch(tool)),
        )
    }

    @Test
    fun `header summary is null for invalid resolution`() {
        assertNull(applyPatchHeaderSummary(completed(), PatchResolution.Invalid))
    }

    @Test
    fun `patch preview strips protocol directives but keeps hunks and content`() {
        val patch = "*** Begin Patch\n" +
            "*** Update File: a.kt\n" +
            "@@ -1 +1 @@\n" +
            "-old\n" +
            "+new\n" +
            " context\n" +
            " *** literal context\n" +
            "+*** literal addition\n" +
            "*** Move to: b.kt\n" +
            "*** End of File\n" +
            "*** End Patch"
        assertEquals(
            listOf(
                "@@ -1 +1 @@",
                "-old",
                "+new",
                " context",
                " *** literal context",
                "+*** literal addition",
            ),
            patchPreviewLines(patch),
        )
    }

    private fun patchInput(text: String) = buildJsonObject { put("patchText", text) }

    private fun completed() = ToolState.Completed(
        input = buildJsonObject {},
        output = "Success",
        title = "Applied patch",
        startedAt = 1,
        endedAt = 2,
    )

    private fun errored() = ToolState.Error(
        input = buildJsonObject {},
        error = "boom",
        startedAt = 1,
        endedAt = 2,
    )

    private fun tool(
        input: kotlinx.serialization.json.JsonObject,
        metadata: kotlinx.serialization.json.JsonObject?,
        state: ToolState,
    ): Part.Tool =
        Part.Tool(
            id = "id",
            sessionID = "session",
            messageID = "message",
            callID = "call",
            toolName = "apply_patch",
            state = when (state) {
                is ToolState.Pending -> state.copy(input = input)
                is ToolState.Running -> state.copy(input = input)
                is ToolState.Completed -> state.copy(input = input, metadata = metadata ?: state.metadata)
                is ToolState.Error -> state.copy(input = input, metadata = metadata ?: state.metadata)
            },
        )
}
