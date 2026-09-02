package dev.blazelight.p4oc.ui.components.chat

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InitialInputFocusStateTest {

    @Test
    fun `request waits for active tab and never replays after consumption`() {
        val state = InitialInputFocusState()

        assertFalse(state.shouldAttempt(requested = true, isActive = false))
        assertTrue(state.shouldAttempt(requested = true, isActive = true))
        state.markConsumed()
        assertFalse(state.shouldAttempt(requested = true, isActive = true))
        assertFalse(state.shouldAttempt(requested = true, isActive = false))
    }

    @Test
    fun `missing or failed request remains unconsumed`() {
        val state = InitialInputFocusState()

        assertFalse(state.shouldAttempt(requested = false, isActive = true))
        assertTrue(state.shouldAttempt(requested = true, isActive = true))
        assertTrue(state.shouldAttempt(requested = true, isActive = true))
    }

    @Test
    fun `saver restores consumed request`() {
        val restored = InitialInputFocusState.Saver.restore(true)!!

        assertFalse(restored.shouldAttempt(requested = true, isActive = true))
    }
}
