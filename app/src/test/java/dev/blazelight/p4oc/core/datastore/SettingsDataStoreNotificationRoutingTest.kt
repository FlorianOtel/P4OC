package dev.blazelight.p4oc.core.datastore

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SettingsDataStoreNotificationRoutingTest {
    @Test
    fun `routing round trip prunes All entries`() {
        val encoded = encodeServerRouting(
            mapOf(
                "https://off.example:4096" to NotificationRoutingMode.Off,
                "https://all.example:4096" to NotificationRoutingMode.All,
            ),
        )

        assertEquals(
            mapOf("https://off.example:4096" to NotificationRoutingMode.Off),
            decodeServerRouting(encoded),
        )
    }

    @Test
    fun `routing containing only All is not persisted`() {
        assertNull(
            encodeServerRouting(
                mapOf("https://all.example:4096" to NotificationRoutingMode.All),
            ),
        )
    }

    @Test
    fun `malformed routing JSON falls back to default routing`() {
        assertEquals(emptyMap<String, NotificationRoutingMode>(), decodeServerRouting("not-json"))
    }

    @Test
    fun `removing server prunes routing by endpoint key`() {
        val endpointKey = "https://removed.example:4096"
        val encoded = encodeServerRouting(
            mapOf(
                endpointKey to NotificationRoutingMode.Off,
                "https://retained.example:4096" to NotificationRoutingMode.Mentions,
            ),
        )

        assertEquals(
            mapOf("https://retained.example:4096" to NotificationRoutingMode.Mentions),
            decodeServerRouting(pruneServerRouting(encoded, endpointKey)),
        )
    }
}
