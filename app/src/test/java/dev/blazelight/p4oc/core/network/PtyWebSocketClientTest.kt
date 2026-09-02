package dev.blazelight.p4oc.core.network

import org.junit.Assert.assertEquals
import org.junit.Test

class PtyWebSocketClientTest {
    @Test
    fun `websocket URL preserves base path and encodes PTY id as one segment`() {
        val url = buildPtyWebSocketUrl(
            baseUrl = "https://terminal.example.com/opencode/",
            ptyId = "id/with?reserved%chars",
            directory = "/repo/with spaces?and=query",
            workspace = null,
        )

        assertEquals(
            "wss://terminal.example.com/opencode/pty/id%2Fwith%3Freserved%25chars/connect" +
                "?directory=%2Frepo%2Fwith%20spaces%3Fand%3Dquery",
            url,
        )
    }

    @Test
    fun `websocket URL omits explicit null workspace scope`() {
        val url = buildPtyWebSocketUrl(
            baseUrl = "http://terminal.example.com/",
            ptyId = "pty-1",
            directory = null,
            workspace = null,
        )

        assertEquals("ws://terminal.example.com/pty/pty-1/connect", url)
    }
}
