package dev.blazelight.p4oc.ui.attention

import dev.blazelight.p4oc.domain.server.ServerRef
import dev.blazelight.p4oc.domain.server.WorkspaceKey
import org.junit.Assert.assertEquals
import org.junit.Test

class AttentionBadgeRegistryTest {
    @Test
    fun `attention state is isolated by server workspace and tab`() {
        val alpha = ServerRef.fromEndpointKey("http://alpha.example:4096")
        val beta = ServerRef.fromEndpointKey("http://beta.example:4096")
        val workspace = WorkspaceKey.Directory("/repo")
        val registry = AttentionBadgeRegistry()

        registry.set(
            AttentionSignal(
                AttentionKey(alpha, workspace, "tab-a"),
                AttentionSeverity.Warning,
                "awaiting input",
            ),
        )
        registry.set(
            AttentionSignal(
                AttentionKey(beta, workspace, "tab-b"),
                AttentionSeverity.Error,
                "auth",
            ),
        )

        val state = registry.state.value
        assertEquals(2, state.homeCount)
        assertEquals(1, state.forServer(alpha).size)
        assertEquals(1, state.forServer(beta).size)
        assertEquals(1, state.forWorkspace(alpha, workspace).size)
        assertEquals(1, state.forTab("tab-a").size)
    }

    @Test
    fun `clearing focused tab updates aggregate badge without touching other servers`() {
        val alpha = ServerRef.fromEndpointKey("http://alpha.example:4096")
        val beta = ServerRef.fromEndpointKey("http://beta.example:4096")
        val registry = AttentionBadgeRegistry()
        registry.set(
            AttentionSignal(
                AttentionKey(alpha, tabId = "tab-a"),
                AttentionSeverity.Info,
                "done",
            ),
        )
        registry.set(
            AttentionSignal(
                AttentionKey(beta, tabId = "tab-b"),
                AttentionSeverity.Error,
                "auth",
            ),
        )

        registry.clearTab("tab-a")

        assertEquals(1, registry.state.value.homeCount)
        assertEquals(0, registry.state.value.forServer(alpha).size)
        assertEquals(1, registry.state.value.forServer(beta).size)
    }
}
