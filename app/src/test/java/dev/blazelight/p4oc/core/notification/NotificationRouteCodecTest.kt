package dev.blazelight.p4oc.core.notification

import android.content.Intent
import dev.blazelight.p4oc.domain.server.ServerRef
import dev.blazelight.p4oc.domain.server.WorkspaceKey
import dev.blazelight.p4oc.domain.session.SessionId
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationRouteCodecTest {
    @Test
    fun `identity distinguishes equal session ids across server workspace and kind`() {
        val first = NotificationRoute(
            sessionId = "shared-session",
            serverRef = ServerRef.fromEndpointKey("https://one.example"),
            workspaceKey = WorkspaceKey.Directory("/repo"),
        )
        val otherServer = first.copy(serverRef = ServerRef.fromEndpointKey("https://two.example"))
        val otherWorkspace = first.copy(workspaceKey = WorkspaceKey.Directory("/other"))

        assertNotEquals(
            NotificationRouteCodec.identity(NotificationKind.Permission, first),
            NotificationRouteCodec.identity(NotificationKind.Permission, otherServer),
        )
        assertNotEquals(
            NotificationRouteCodec.identity(NotificationKind.Permission, first),
            NotificationRouteCodec.identity(NotificationKind.Permission, otherWorkspace),
        )
        assertNotEquals(
            NotificationRouteCodec.identity(NotificationKind.Permission, first),
            NotificationRouteCodec.identity(NotificationKind.Question, first),
        )
    }

    @Test
    fun `identity is collision resistant when route hash codes collide`() {
        // Java strings Aa and BB intentionally have equal hash codes.
        val first = NotificationRoute(
            sessionId = "Aa",
            serverRef = ServerRef.fromEndpointKey("https://server.example"),
            workspaceKey = WorkspaceKey.Global,
        )
        val second = first.copy(sessionId = "BB")

        assertEquals(first.hashCode(), second.hashCode())
        assertNotEquals(
            NotificationRouteCodec.identity(NotificationKind.Completion, first),
            NotificationRouteCodec.identity(NotificationKind.Completion, second),
        )
    }

    @Test
    fun `directory workspace route round trips all ownership fields`() {
        val expected = NotificationRoute(
            sessionId = "session-1",
            serverRef = ServerRef.fromEndpointKey("https://server.example:4096"),
            workspaceKey = WorkspaceKey.Directory("/owned/repository"),
        )
        assertEquals(
            expected,
            NotificationRouteCodec.decode("session-1", "https://server.example:4096", "directory", "/owned/repository"),
        )
    }

    @Test
    fun `global and session scoped workspaces round trip explicitly`() {
        listOf(
            WorkspaceKey.Global,
            WorkspaceKey.SessionScoped(SessionId("scope-session")),
        ).forEach { workspace ->
            val expected = NotificationRoute(
                sessionId = "target-session",
                serverRef = ServerRef.fromEndpointKey("http://localhost:4096"),
                workspaceKey = workspace,
            )
            val type = if (workspace == WorkspaceKey.Global) "global" else "session"
            val value = (workspace as? WorkspaceKey.SessionScoped)?.sessionId?.value
            assertEquals(
                expected,
                NotificationRouteCodec.decode("target-session", "http://localhost:4096", type, value),
            )
        }
    }

    @Test
    fun `legacy session-only notification is rejected instead of guessing workspace`() {
        assertNull(NotificationRouteCodec.decode("session-1", null, null, null))
    }

    @Test
    fun `incomplete or blank ownership is rejected`() {
        assertNull(NotificationRouteCodec.decode("session-1", "server", null, null))
        assertNull(NotificationRouteCodec.decode("session-1", " ", "global", null))
    }

    @Test
    fun `cleared route cannot be read again after recreation`() {
        val extras = mutableMapOf(
            "notification.sessionId" to "session-1",
            "notification.serverEndpointKey" to "https://server.example:4096",
            "notification.workspaceType" to "directory",
            "notification.workspaceValue" to "/owned/repository",
        )
        val intent = mockk<Intent>()
        every { intent.getStringExtra(any()) } answers { extras[firstArg()] }
        every { intent.removeExtra(any()) } answers {
            extras.remove(firstArg())
            intent
        }

        assertEquals(
            NotificationRoute(
                sessionId = "session-1",
                serverRef = ServerRef.fromEndpointKey("https://server.example:4096"),
                workspaceKey = WorkspaceKey.Directory("/owned/repository"),
            ),
            NotificationRouteCodec.read(intent),
        )

        NotificationRouteCodec.clear(intent)

        assertNull(NotificationRouteCodec.read(intent))
    }
}
