package dev.blazelight.p4oc.ui.screens.terminal

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TerminalTranscriptStoreTest {
    @Test
    fun `burst frames remain bounded and ordered`() {
        val transcript = BoundedTerminalTranscript(maxChars = 10)

        transcript.append("0123")
        transcript.append("4567")
        transcript.append("89AB")

        assertEquals(10, transcript.size)
        assertEquals("23456789AB", transcript.snapshot())
    }

    @Test
    fun `trim does not split a surrogate pair`() {
        val transcript = BoundedTerminalTranscript(maxChars = 4)

        transcript.append("ab😀cd")

        assertEquals("😀cd", transcript.snapshot())
        assertFalse(Character.isLowSurrogate(transcript.snapshot().first()))
    }

    @Test
    fun `trim does not retain low surrogate when pair arrived in separate frames`() {
        val transcript = BoundedTerminalTranscript(maxChars = 3)

        transcript.append("ab\uD83D")
        transcript.append("\uDE00cd")

        assertEquals("cd", transcript.snapshot())
        assertFalse(Character.isLowSurrogate(transcript.snapshot().first()))
    }

    @Test
    fun `trim avoids retaining tail of csi escape sequence`() {
        val transcript = BoundedTerminalTranscript(maxChars = 5)

        transcript.append("abc\u001b[31mXY")

        assertEquals("XY", transcript.snapshot())
        assertFalse(transcript.snapshot().contains("[31"))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `burst changes debounce to one persistence write and explicit flush saves final state`() = runTest {
        val transcript = BoundedTerminalTranscript(maxChars = 64)
        val snapshots = mutableListOf<String>()
        val persistence = TerminalTranscriptPersistence(
            scope = backgroundScope,
            debounceMillis = 500,
            snapshot = transcript::snapshot,
            persist = snapshots::add,
        )

        repeat(100) {
            transcript.append(it.toString())
            persistence.changed()
        }
        runCurrent()
        advanceTimeBy(499)
        runCurrent()
        assertTrue(snapshots.isEmpty())

        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf(transcript.snapshot()), snapshots)

        transcript.append("final")
        persistence.changed()
        persistence.flushNow()
        assertEquals(transcript.snapshot(), snapshots.last())
        assertEquals(2, snapshots.size)
    }

    @Test
    fun `oversized restored transcript is bounded safely`() {
        val transcript = BoundedTerminalTranscript(maxChars = 4, restored = "ab😀cd")

        assertEquals(4, transcript.size)
        assertEquals("😀cd", transcript.snapshot())
    }

    @Test
    fun `accessible text exposes only bounded tail of visible screen`() {
        val visibleScreen = "hidden history must not be supplied\n" + "x".repeat(20)

        val result = boundedVisibleTerminalText(visibleScreen, maxChars = 10)

        assertEquals("x".repeat(10), result)
        assertEquals(10, result.length)
    }

    @Test
    fun `accessible text removes empty screen padding`() {
        val result = boundedVisibleTerminalText("\n  prompt ready   \n     \n", maxChars = 100)

        assertEquals("  prompt ready", result)
    }

    @Test
    fun `accessible text cap does not split surrogate pair`() {
        val result = boundedVisibleTerminalText("abc😀de", maxChars = 4)

        assertEquals("😀de", result)
        assertEquals(4, result.length)
        assertFalse(Character.isLowSurrogate(result.first()))
    }
}
