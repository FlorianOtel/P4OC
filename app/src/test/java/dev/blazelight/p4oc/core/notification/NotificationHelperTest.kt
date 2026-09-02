package dev.blazelight.p4oc.core.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Test

class NotificationHelperTest {
    @Test
    fun `text below the bound is unchanged`() {
        val text = "a".repeat(MAX_NOTIFICATION_TEXT_CODE_POINTS - 1)

        assertSame(text, boundedNotificationText(text, "fallback"))
    }

    @Test
    fun `text exactly at the bound is unchanged`() {
        val text = "a".repeat(MAX_NOTIFICATION_TEXT_CODE_POINTS)

        assertSame(text, boundedNotificationText(text, "fallback"))
    }

    @Test
    fun `text above the bound replaces the final allowed code point with an ellipsis`() {
        val text = "a".repeat(MAX_NOTIFICATION_TEXT_CODE_POINTS + 1)

        assertEquals(
            "a".repeat(MAX_NOTIFICATION_TEXT_CODE_POINTS - 1) + "…",
            boundedNotificationText(text, "fallback"),
        )
    }

    @Test
    fun `surrogate pair at truncation boundary remains intact`() {
        val prefix = "a".repeat(MAX_NOTIFICATION_TEXT_CODE_POINTS - 1)
        val text = prefix + "\uD83D\uDE80" + "tail"

        val bounded = boundedNotificationText(text, "fallback")

        assertEquals(prefix + "…", bounded)
        assertEquals(MAX_NOTIFICATION_TEXT_CODE_POINTS, bounded.codePointCount(0, bounded.length))
        assertFalse(bounded.any { Character.isSurrogate(it) })
    }

    @Test
    fun `null text uses fallback without changing it`() {
        val fallback = "Session completed"

        assertSame(fallback, boundedNotificationText(null, fallback))
    }

    @Test
    fun `oversized fallback is bounded too`() {
        val fallback = "f".repeat(MAX_NOTIFICATION_TEXT_CODE_POINTS + 1)

        assertEquals(
            "f".repeat(MAX_NOTIFICATION_TEXT_CODE_POINTS - 1) + "…",
            boundedNotificationText(null, fallback),
        )
    }
}
