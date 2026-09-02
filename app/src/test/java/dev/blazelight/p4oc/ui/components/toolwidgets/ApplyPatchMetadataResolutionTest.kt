package dev.blazelight.p4oc.ui.components.toolwidgets

import dev.blazelight.p4oc.domain.model.Part
import dev.blazelight.p4oc.domain.model.ToolState
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ApplyPatchMetadataResolutionTest {

    @Test
    fun `metadata supplies exact delete counts`() {
        val metadata = buildJsonObject {
            put(
                "files",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("relativePath", "src/Old.kt")
                            put("type", "delete")
                            put("additions", 0)
                            put("deletions", 12)
                        },
                    )
                },
            )
        }
        val meta = extractFilesMetadata(metadata)
        assertEquals(FilesMetadata.Valid::class, meta::class)
        val valid = meta as FilesMetadata.Valid
        assertEquals(
            listOf(PatchFileChange("src/Old.kt", PatchFileAction.DELETE, additions = 0, deletions = 12)),
            valid.files,
        )
    }

    @Test
    fun `metadata move type with destination relativePath is honored`() {
        val metadata = buildJsonObject {
            put(
                "files",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("relativePath", "dst.kt")
                            put("type", "move")
                            put("additions", 1)
                            put("deletions", 1)
                        },
                    )
                },
            )
        }
        val input = buildJsonObject {
            put(
                "patchText",
                "*** Begin Patch\n*** Update File: src.kt\n*** Move to: dst.kt\n@@\n-old\n+new\n*** End Patch",
            )
        }
        val tool = tool(input, metadata, completed())
        val resolution = resolveApplyPatch(tool) as PatchResolution.Valid

        assertEquals(1, resolution.files.size)
        val change = resolution.files.first()
        assertEquals(PatchFileAction.MOVE, change.action)
        assertEquals("src.kt", change.path) // source retained from input section
        assertEquals("dst.kt", change.movePath) // destination from metadata relativePath
        assertEquals(1, change.additions)
        assertEquals(1, change.deletions)
    }

    @Test
    fun `present malformed files metadata fails closed without fallback`() {
        val metadata = buildJsonObject { put("files", "not-an-array") }
        val input = buildJsonObject {
            put("patchText", "*** Begin Patch\n*** Add File: a.kt\n+x\n*** End Patch")
        }
        val tool = tool(input, metadata, completed())
        assertEquals(PatchResolution.Invalid, resolveApplyPatch(tool))
    }

    @Test
    fun `present null files metadata is malformed and fails closed`() {
        val metadata = buildJsonObject { put("files", null) }
        val input = buildJsonObject {
            put("patchText", "*** Begin Patch\n*** Add File: a.kt\n+x\n*** End Patch")
        }
        val tool = tool(input, metadata, completed())
        assertEquals(PatchResolution.Invalid, resolveApplyPatch(tool))
    }

    @Test
    fun `negative metadata counts are malformed`() {
        val metadata = buildJsonObject {
            put(
                "files",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("relativePath", "a.kt")
                            put("type", "update")
                            put("additions", -1)
                            put("deletions", 0)
                        },
                    )
                },
            )
        }
        assertEquals(FilesMetadata.Malformed, extractFilesMetadata(metadata))
    }

    @Test
    fun `string metadata counts are malformed despite numeric coercion`() {
        val metadata = buildJsonObject {
            put(
                "files",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("relativePath", "a.kt")
                            put("type", "update")
                            put("additions", "3") // JsonPrimitive string; must not be coerced
                            put("deletions", 1)
                        },
                    )
                },
            )
        }
        assertEquals(FilesMetadata.Malformed, extractFilesMetadata(metadata))
    }

    @Test
    fun `boolean metadata count is malformed`() {
        val metadata = buildJsonObject {
            put(
                "files",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("relativePath", "a.kt")
                            put("type", "update")
                            put("additions", true)
                            put("deletions", 1)
                        },
                    )
                },
            )
        }
        assertEquals(FilesMetadata.Malformed, extractFilesMetadata(metadata))
    }

    @Test
    fun `absent additions or deletions in metadata stay unknown`() {
        val metadata = buildJsonObject {
            put(
                "files",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("relativePath", "a.kt")
                            put("type", "update")
                        },
                    )
                },
            )
        }
        val meta = extractFilesMetadata(metadata) as FilesMetadata.Valid
        assertNull(meta.files.first().additions)
        assertNull(meta.files.first().deletions)
    }

    @Test
    fun `metadata counts differing from input are accepted and override input`() {
        val metadata = buildJsonObject {
            put(
                "files",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("relativePath", "a.kt")
                            put("type", "update")
                            // authoritative diff-based count, differs from raw input
                            put("additions", 9)
                            put("deletions", 1)
                        },
                    )
                },
            )
        }
        val input = buildJsonObject {
            put("patchText", "*** Begin Patch\n*** Update File: a.kt\n@@\n-old\n+new\n*** End Patch")
        }
        val tool = tool(input, metadata, completed())
        val resolution = resolveApplyPatch(tool) as PatchResolution.Valid
        val change = resolution.files.single()
        assertEquals(PatchFileAction.UPDATE, change.action)
        assertEquals(9, change.additions) // metadata overrides input
        assertEquals(1, change.deletions)
        assertEquals("Patch: 1 file +9 -1", applyPatchCompactDescription(tool))
    }

    @Test
    fun `metadata action mismatch resolves invalid`() {
        val metadata = buildJsonObject {
            put(
                "files",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("relativePath", "a.kt")
                            put("type", "add") // input section is an update
                            put("additions", 1)
                            put("deletions", 0)
                        },
                    )
                },
            )
        }
        val input = buildJsonObject {
            put("patchText", "*** Begin Patch\n*** Update File: a.kt\n@@\n-old\n+new\n*** End Patch")
        }
        val tool = tool(input, metadata, completed())
        assertEquals(PatchResolution.Invalid, resolveApplyPatch(tool))
    }

    @Test
    fun `metadata file count mismatch resolves invalid`() {
        // Two input sections but only one authoritative file entry.
        val metadata = buildJsonObject {
            put(
                "files",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("relativePath", "a.kt")
                            put("type", "add")
                            put("additions", 1)
                            put("deletions", 0)
                        },
                    )
                },
            )
        }
        val input = buildJsonObject {
            put("patchText", "*** Begin Patch\n*** Add File: a.kt\n+x\n*** Add File: b.kt\n+y\n*** End Patch")
        }
        val tool = tool(input, metadata, completed())
        assertEquals(PatchResolution.Invalid, resolveApplyPatch(tool))
    }

    @Test
    fun `empty authoritative files metadata over nonempty input is invalid`() {
        val metadata = buildJsonObject {
            put("files", buildJsonArray {})
        }
        val input = buildJsonObject {
            put("patchText", "*** Begin Patch\n*** Add File: a.kt\n+x\n*** End Patch")
        }
        val tool = tool(input, metadata, completed())
        assertEquals(PatchResolution.Invalid, resolveApplyPatch(tool))
    }

    private fun completed() = ToolState.Completed(
        input = buildJsonObject {},
        output = "Success",
        title = "Applied patch",
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
