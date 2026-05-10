package dev.blazelight.p4oc.domain.model

/** UI-facing session status resolved from backend state plus local attention signals. */
enum class SessionPresence {
    ERROR,
    RETRYING,
    AWAITING_INPUT,
    BUSY,
    UNREAD,
    IDLE,
    BACKGROUND;

    /** Priority for inbox-style ordering (lower = higher priority, appears leftmost). */
    val priority: Int
        get() = ordinal

    val shouldPulse: Boolean
        get() = this == BUSY || this == AWAITING_INPUT || this == RETRYING

    val showsAttentionBadge: Boolean
        get() = this == AWAITING_INPUT
}

data class SessionPresenceSignals(
    val hasError: Boolean = false,
    val pendingUserInput: Boolean = false,
    val isStreaming: Boolean = false,
    val hasRunningTools: Boolean = false,
    val hasUnread: Boolean = false,
    val isBackground: Boolean = false,
)

fun resolveSessionPresence(
    backendStatus: SessionStatus?,
    signals: SessionPresenceSignals = SessionPresenceSignals(),
): SessionPresence = when {
    signals.hasError -> SessionPresence.ERROR
    backendStatus is SessionStatus.Retry -> SessionPresence.RETRYING
    signals.pendingUserInput -> SessionPresence.AWAITING_INPUT
    backendStatus is SessionStatus.Busy || signals.isStreaming || signals.hasRunningTools -> SessionPresence.BUSY
    signals.hasUnread -> SessionPresence.UNREAD
    signals.isBackground -> SessionPresence.BACKGROUND
    else -> SessionPresence.IDLE
}

/** Backwards-compatible name while tab plumbing migrates to SessionPresence. */
typealias SessionConnectionState = SessionPresence
