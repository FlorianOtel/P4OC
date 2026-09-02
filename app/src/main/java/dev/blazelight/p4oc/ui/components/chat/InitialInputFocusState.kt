package dev.blazelight.p4oc.ui.components.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue

internal class InitialInputFocusState(
    consumed: Boolean = false,
) {
    private var consumed by mutableStateOf(consumed)

    val isConsumed: Boolean
        get() = consumed

    fun shouldAttempt(requested: Boolean, isActive: Boolean): Boolean =
        requested && isActive && !consumed

    fun markConsumed() {
        consumed = true
    }

    companion object {
        val Saver = Saver(
            save = { it.isConsumed },
            restore = ::InitialInputFocusState,
        )
    }
}
