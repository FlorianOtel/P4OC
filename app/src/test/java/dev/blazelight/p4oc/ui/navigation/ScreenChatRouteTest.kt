package dev.blazelight.p4oc.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenChatRouteTest {
    @Test
    fun `existing session route keeps input unfocused`() {
        assertEquals("chat/session%2Fone", Screen.Chat.createRoute("session/one"))
    }

    @Test
    fun `new session route requests initial input focus`() {
        assertEquals(
            "chat/session%2Fone?focusInput=true",
            Screen.Chat.createRoute("session/one", focusInput = true),
        )
    }
}
