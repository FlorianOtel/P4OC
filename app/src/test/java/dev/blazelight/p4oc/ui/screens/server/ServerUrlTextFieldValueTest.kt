package dev.blazelight.p4oc.ui.screens.server

import org.junit.Assert.assertEquals
import org.junit.Test

class ServerUrlTextFieldValueTest {
    @Test
    fun serverUrlTextFieldValue_placesCursorAtEndOfReplacementUrl() {
        val url = "http://192.168.24.25:4096"

        val value = serverUrlTextFieldValue(url)

        assertEquals(url, value.text)
        assertEquals(url.length, value.selection.start)
        assertEquals(url.length, value.selection.end)
    }

    @Test
    fun serverUrlTextFieldValue_placesCursorAtEndOfShortReplacementUrl() {
        val url = "http://pi.local:4096"

        val value = serverUrlTextFieldValue(url)

        assertEquals(url, value.text)
        assertEquals(url.length, value.selection.start)
        assertEquals(url.length, value.selection.end)
    }
}
