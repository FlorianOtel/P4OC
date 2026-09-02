package dev.blazelight.p4oc.ui.components.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamingMarkdownParserTest {
    @Test
    fun `parses fenced code outside prose`() {
        val blocks = parseMarkdownBlocks(
            """
            Intro

            ```kotlin
            val answer = 42
            ```

            Done
            """.trimIndent()
        )

        assertEquals(3, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.Paragraph)
        val code = blocks[1] as MarkdownBlock.CodeFence
        assertEquals("kotlin", code.language)
        assertEquals("val answer = 42", code.code)
        assertTrue(blocks[2] is MarkdownBlock.Paragraph)
    }

    @Test
    fun `keeps open streaming fence as code block`() {
        val blocks = parseMarkdownBlocks(
            """
            ```python
            print("still streaming")
            """.trimIndent()
        )

        val code = blocks.single() as MarkdownBlock.CodeFence
        assertEquals("python", code.language)
        assertEquals("print(\"still streaming\")", code.code)
    }

    @Test
    fun `parses gfm pipe table`() {
        val blocks = parseMarkdownBlocks(
            """
            | Name | Status | Notes |
            | --- | --- | --- |
            | DeepSeek V4 | ok | long variant table cell |
            | GPT-5.4 | ok | none low medium high xhigh |
            """.trimIndent()
        )

        val table = blocks.single() as MarkdownBlock.Table
        assertEquals(
            listOf(
                listOf("Name", "Status", "Notes"),
                listOf("DeepSeek V4", "ok", "long variant table cell"),
                listOf("GPT-5.4", "ok", "none low medium high xhigh"),
            ),
            table.rows,
        )
    }

    @Test
    fun `does not parse delimiterless pipe text as table`() {
        val blocks = parseMarkdownBlocks(
            """
            this | is | prose
            not a | markdown | table
            """.trimIndent()
        )

        assertTrue(blocks.single() is MarkdownBlock.Paragraph)
    }

    @Test
    fun `preserves source ordered list numbers`() {
        val blocks = parseMarkdownBlocks(
            """
            4. first visible item
            7. skipped number from model
            """.trimIndent()
        )

        val list = blocks.single() as MarkdownBlock.ListBlock
        assertEquals(
            listOf(
                MarkdownListItem("4.", "first visible item", ordered = true),
                MarkdownListItem("7.", "skipped number from model", ordered = true),
            ),
            list.items,
        )
    }

    @Test
    fun `preserves nested list indentation`() {
        val blocks = parseMarkdownBlocks(
            """
            - **Home**
              - project tree
              - recent workspaces
            - **Settings**
              - grouped sections
            """.trimIndent()
        )

        val list = blocks.single() as MarkdownBlock.ListBlock
        assertEquals(
            listOf(
                MarkdownListItem("•", "**Home**", indentLevel = 0),
                MarkdownListItem("•", "project tree", indentLevel = 1),
                MarkdownListItem("•", "recent workspaces", indentLevel = 1),
                MarkdownListItem("•", "**Settings**", indentLevel = 0),
                MarkdownListItem("•", "grouped sections", indentLevel = 1),
            ),
            list.items,
        )
    }

    @Test
    fun `keeps mixed ordered and unordered nesting in one list run`() {
        val blocks = parseMarkdownBlocks(
            """
            1. Parent
               - Child
               - Another child
            2. Parent
            """.trimIndent()
        )

        assertEquals(1, blocks.size)
        val list = blocks.single() as MarkdownBlock.ListBlock
        assertEquals(
            listOf(
                MarkdownListItem("1.", "Parent", indentLevel = 0, ordered = true),
                MarkdownListItem("•", "Child", indentLevel = 1, ordered = false),
                MarkdownListItem("•", "Another child", indentLevel = 1, ordered = false),
                MarkdownListItem("2.", "Parent", indentLevel = 0, ordered = true),
            ),
            list.items,
        )
    }

    @Test
    fun `four space nesting maps to a single semantic level`() {
        val blocks = parseMarkdownBlocks(
            """
            - Parent
                - Child
                - Another child
            - Sibling
            """.trimIndent()
        )

        val list = blocks.single() as MarkdownBlock.ListBlock
        assertEquals(
            listOf(
                MarkdownListItem("•", "Parent", indentLevel = 0),
                MarkdownListItem("•", "Child", indentLevel = 1),
                MarkdownListItem("•", "Another child", indentLevel = 1),
                MarkdownListItem("•", "Sibling", indentLevel = 0),
            ),
            list.items,
        )
    }

    @Test
    fun `zero four eight indentation maps to depths zero one two`() {
        val blocks = parseMarkdownBlocks(
            """
            - Level zero
                - Level one
                    - Level two
            """.trimIndent()
        )

        val list = blocks.single() as MarkdownBlock.ListBlock
        assertEquals(
            listOf(
                MarkdownListItem("•", "Level zero", indentLevel = 0),
                MarkdownListItem("•", "Level one", indentLevel = 1),
                MarkdownListItem("•", "Level two", indentLevel = 2),
            ),
            list.items,
        )
    }

    @Test
    fun `stream fragment beginning at indentation maps first item to zero`() {
        val blocks = parseMarkdownBlocks(
            "    - Fragment child\n    - Fragment sibling"
        )

        val list = blocks.single() as MarkdownBlock.ListBlock
        assertEquals(
            listOf(
                MarkdownListItem("•", "Fragment child", indentLevel = 0),
                MarkdownListItem("•", "Fragment sibling", indentLevel = 0),
            ),
            list.items,
        )
    }

    @Test
    fun `blank line splits two lists into separate blocks`() {
        val blocks = parseMarkdownBlocks(
            """
            - first

            - second
            """.trimIndent()
        )

        assertEquals(2, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.ListBlock)
        assertTrue(blocks[1] is MarkdownBlock.ListBlock)
    }

    @Test
    fun `tab indented child maps one semantic level below parent`() {
        val blocks = parseMarkdownBlocks(
            "- Parent\n\t- Tab child\n\t- Tab sibling\n- Second parent"
        )

        val list = blocks.single() as MarkdownBlock.ListBlock
        assertEquals(
            listOf(
                MarkdownListItem("•", "Parent", indentLevel = 0),
                MarkdownListItem("•", "Tab child", indentLevel = 1),
                MarkdownListItem("•", "Tab sibling", indentLevel = 1),
                MarkdownListItem("•", "Second parent", indentLevel = 0),
            ),
            list.items,
        )
    }

    @Test
    fun `tab indented fragment maps first item to zero`() {
        val blocks = parseMarkdownBlocks("\t- Fragment child\n\t- Fragment sibling")

        val list = blocks.single() as MarkdownBlock.ListBlock
        assertEquals(
            listOf(
                MarkdownListItem("•", "Fragment child", indentLevel = 0),
                MarkdownListItem("•", "Fragment sibling", indentLevel = 0),
            ),
            list.items,
        )
    }

    @Test
    fun `display marker leaves normal markers unchanged`() {
        assertEquals("•", displayListMarker("•"))
        assertEquals("1.", displayListMarker("1."))
        assertEquals("10.", displayListMarker("10."))
        assertEquals("100.", displayListMarker("100."))
    }

    @Test
    fun `display marker truncates long markers to six visible characters`() {
        assertEquals("…4567.", displayListMarker("1234567."))
        assertEquals("…7890.", displayListMarker("1234567890."))
    }
}
