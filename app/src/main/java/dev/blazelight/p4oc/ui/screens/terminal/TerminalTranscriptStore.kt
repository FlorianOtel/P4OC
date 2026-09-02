package dev.blazelight.p4oc.ui.screens.terminal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * A bounded transcript which keeps incoming frames in order without rebuilding the full transcript
 * for every frame. A String is materialized only when persistence needs a snapshot.
 */
internal class BoundedTerminalTranscript(
    private val maxChars: Int,
    restored: String = "",
) {
    private companion object {
        const val CSI_FINAL_BYTE_MIN = 0x40
        const val CSI_FINAL_BYTE_MAX = 0x7e
    }

    private val chunks = ArrayDeque<String>()
    private var charCount = 0

    init {
        append(restored)
    }

    val size: Int get() = charCount

    fun append(chunk: String) {
        if (chunk.isEmpty()) return
        chunks.addLast(chunk)
        charCount += chunk.length
        trimToLimit()
    }

    fun clear() {
        chunks.clear()
        charCount = 0
    }

    fun snapshot(): String = buildString(charCount) {
        chunks.forEach(::append)
    }

    private fun trimToLimit() {
        var overflow = charCount - maxChars
        while (overflow > 0 && chunks.isNotEmpty()) {
            val first = chunks.removeFirst()
            if (first.length <= overflow) {
                charCount -= first.length
                overflow -= first.length
                continue
            }

            val start = safeTrimStart(first, overflow)
            val remainder = first.substring(start)
            chunks.addFirst(remainder)
            charCount -= start
            overflow = charCount - maxChars
        }
        discardOrphanedLowSurrogate()
    }

    private fun discardOrphanedLowSurrogate() {
        val first = chunks.firstOrNull() ?: return
        if (!Character.isLowSurrogate(first.first())) return
        chunks.removeFirst()
        val remainder = first.substring(1)
        if (remainder.isNotEmpty()) chunks.addFirst(remainder)
        charCount--
    }

    private fun safeTrimStart(value: String, requested: Int): Int {
        var start = requested.coerceIn(0, value.length)
        if (start in 1 until value.length &&
            Character.isHighSurrogate(value[start - 1]) && Character.isLowSurrogate(value[start])
        ) {
            start++
        }

        // If the boundary lands inside an ANSI/VT escape sequence, discard the remainder of that
        // sequence instead of restoring/rendering a syntactically broken control sequence.
        val escape = value.lastIndexOf('\u001b', startIndex = (start - 1).coerceAtLeast(0))
        if (escape >= 0 && escape < start) {
            val terminator = escapeSequenceEnd(value, escape)
            if (terminator >= start) start = terminator + 1
        }
        return start.coerceAtMost(value.length)
    }

    private fun escapeSequenceEnd(value: String, escape: Int): Int {
        val next = escape + 1
        return when {
            next >= value.length -> -1
            value[next] != '[' -> next
            else -> findCsiFinalByte(value, escape + 2)
        }
    }

    private fun findCsiFinalByte(value: String, start: Int): Int {
        for (index in start until value.length) {
            if (value[index].code in CSI_FINAL_BYTE_MIN..CSI_FINAL_BYTE_MAX) return index
        }
        return -1
    }
}

@OptIn(FlowPreview::class)
internal class TerminalTranscriptPersistence(
    scope: CoroutineScope,
    debounceMillis: Long,
    private val snapshot: () -> String,
    private val persist: (String) -> Unit,
) {
    private val changes = Channel<Unit>(Channel.CONFLATED)

    init {
        scope.launch {
            changes.receiveAsFlow()
                .debounce(debounceMillis)
                .collect { flushNow() }
        }
    }

    fun changed() {
        changes.trySend(Unit)
    }

    fun flushNow() {
        persist(snapshot())
    }
}
