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
                MarkdownListItem("4.", "first visible item"),
                MarkdownListItem("7.", "skipped number from model"),
            ),
            list.items,
        )
    }
}
