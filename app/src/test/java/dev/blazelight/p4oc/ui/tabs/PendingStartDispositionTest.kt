package dev.blazelight.p4oc.ui.tabs

import dev.blazelight.p4oc.core.network.ConnectionState
import org.junit.Assert.assertEquals
import org.junit.Test

class PendingStartDispositionTest {
    @Test
    fun `removed saved server terminates before considering connection or api`() {
        assertEquals(
            PendingStartDisposition.SavedServerMissing,
            pendingStartDisposition(
                savedServerExists = false,
                connectionState = ConnectionState.Connected,
                apiAvailable = true,
            ),
        )
    }

    @Test
    fun `connection error is a recoverable terminal state`() {
        assertEquals(
            PendingStartDisposition.ConnectionFailed,
            pendingStartDisposition(
                savedServerExists = true,
                connectionState = ConnectionState.Error("sensitive transport detail"),
                apiAvailable = false,
            ),
        )
    }

    @Test
    fun `connecting and disconnected states wait for registry transition`() {
        listOf(ConnectionState.Connecting, ConnectionState.Disconnected, null).forEach { state ->
            assertEquals(
                PendingStartDisposition.WaitForConnection,
                pendingStartDisposition(
                    savedServerExists = true,
                    connectionState = state,
                    apiAvailable = false,
                ),
            )
        }
    }

    @Test
    fun `connected state without owned api offers recovery rather than waiting`() {
        assertEquals(
            PendingStartDisposition.ApiUnavailable,
            pendingStartDisposition(
                savedServerExists = true,
                connectionState = ConnectionState.Connected,
                apiAvailable = false,
            ),
        )
    }

    @Test
    fun `work runs only when saved server connection and api are all present`() {
        assertEquals(
            PendingStartDisposition.Run,
            pendingStartDisposition(
                savedServerExists = true,
                connectionState = ConnectionState.Connected,
                apiAvailable = true,
            ),
        )
    }
}
