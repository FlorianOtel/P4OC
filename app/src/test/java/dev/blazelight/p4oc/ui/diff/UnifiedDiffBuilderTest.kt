package dev.blazelight.p4oc.ui.diff

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UnifiedDiffBuilderTest {

    @Test
    fun emitsHeaderForChangedFile() {
        val diff = UnifiedDiffBuilder.build("foo.txt", "a\nb\nc", "a\nB\nc")
        assertTrue(diff.startsWith("--- a/foo.txt\n+++ b/foo.txt\n"))
        assertTrue(diff.contains("@@"))
        // single-line CHANGE => one - and one + plus context
        assertTrue(diff.contains("\n-b\n"))
        assertTrue(diff.contains("\n+B\n"))
    }

    @Test
    fun emitsEmptyStringWhenNoChanges() {
        assertEquals("", UnifiedDiffBuilder.build("x.txt", "same\nlines", "same\nlines"))
    }

    @Test
    fun countsCorrectAdditionsAndDeletions_pureInsert() {
        val counts = UnifiedDiffBuilder.counts("a\nb", "a\nb\nc\nd")
        assertEquals(2, counts.additions)
        assertEquals(0, counts.deletions)
    }

    @Test
    fun countsCorrectAdditionsAndDeletions_pureDelete() {
        val counts = UnifiedDiffBuilder.counts("a\nb\nc\nd", "a\nb")
        assertEquals(0, counts.additions)
        assertEquals(2, counts.deletions)
    }

    @Test
    fun countsCorrectAdditionsAndDeletions_change() {
        // single line replaced
        val counts = UnifiedDiffBuilder.counts("alpha\nbeta\ngamma", "alpha\nBETA\ngamma")
        assertEquals(1, counts.additions)
        assertEquals(1, counts.deletions)
    }

    @Test
    fun parsesBackThroughParsedDiffParser() {
        val diff = UnifiedDiffBuilder.build("a.kt", "alpha\nbeta", "alpha\nGAMMA")
        val parsed = ParsedDiffParser.parse(diff)
        assertEquals(1, parsed.files.size)
        assertTrue(parsed.files.first().hunks.isNotEmpty())
    }
}
