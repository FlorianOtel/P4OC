package dev.blazelight.p4oc.ui.workspace

import dev.blazelight.p4oc.domain.server.ServerRef
import dev.blazelight.p4oc.domain.server.WorkspaceKey
import dev.blazelight.p4oc.domain.session.SessionId
import dev.blazelight.p4oc.domain.workspace.ServerPath
import dev.blazelight.p4oc.domain.workspace.ServerPathParse
import dev.blazelight.p4oc.domain.workspace.ServerUserWorkspaces
import dev.blazelight.p4oc.domain.workspace.StoredWorkspaceAlias
import dev.blazelight.p4oc.domain.workspace.UserWorkspaceRoot
import dev.blazelight.p4oc.domain.workspace.UserWorkspaceState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceLabelIndexTest {
    @Test
    fun `generated aliases use the per server directory universe`() {
        val server = server(ENDPOINT_A, "Desktop")
        val directoryA = "/workspace/A/frontend"
        val directoryB = "/workspace/B/frontend"
        val index = WorkspaceLabelIndex.build(
            UserWorkspaceState.Empty,
            mapOf(ENDPOINT_A to listOf(directoryA, directoryB)),
        )

        assertEquals("A / frontend", index.directoryLabel(server.endpointKey, directoryA).primary)
        assertEquals("B / frontend", index.directoryLabel(server.endpointKey, directoryB).primary)
    }

    @Test
    fun `custom name stays primary when generated names recalculate`() {
        val server = server(ENDPOINT_A, "Desktop")
        val customPath = path("/projects/first/frontend")
        val custom = workspaceState(
            ENDPOINT_A,
            aliases = mapOf(customPath to "UI"),
        )
        val initial = WorkspaceLabelIndex.build(
            custom,
            mapOf(ENDPOINT_A to listOf(customPath.value, "/projects/other/backend")),
        )
        val recalculated = WorkspaceLabelIndex.build(
            custom,
            mapOf(
                ENDPOINT_A to listOf(
                    customPath.value,
                    "/projects/other/backend",
                    "/projects/second/frontend",
                ),
            ),
        )

        assertEquals("UI", initial.directoryLabel(server.endpointKey, customPath.value).primary)
        assertEquals("frontend", initial.directoryLabel(server.endpointKey, customPath.value).generated)
        assertEquals("UI", recalculated.directoryLabel(server.endpointKey, customPath.value).primary)
        assertEquals("first / frontend", recalculated.directoryLabel(server.endpointKey, customPath.value).generated)
    }

    @Test
    fun `reset alias uses the current generated name`() {
        val server = server(ENDPOINT_A, "Desktop")
        val directoryA = "/projects/first/frontend"
        val directoryB = "/projects/second/frontend"
        val index = WorkspaceLabelIndex.build(
            UserWorkspaceState.Empty,
            mapOf(ENDPOINT_A to listOf(directoryA, directoryB)),
        )

        val label = index.directoryLabel(server.endpointKey, directoryA)

        assertNull(label.custom)
        assertEquals("first / frontend", label.generated)
        assertEquals(label.generated, label.primary)
    }

    @Test
    fun `paths under roots use the root alias and do not affect outside aliases`() {
        val server = server(ENDPOINT_A, "Desktop")
        val root = path("/w")
        val rootState = workspaceState(
            ENDPOINT_A,
            roots = listOf(root),
            aliases = mapOf(root to "probe"),
        )
        val index = WorkspaceLabelIndex.build(
            rootState,
            mapOf(ENDPOINT_A to listOf("/w/repo-a/sub", "/w/frontend", "/x/frontend")),
        )

        assertEquals("probe / repo-a / sub", index.directoryLabel(server.endpointKey, "/w/repo-a/sub").primary)
        assertEquals("frontend", index.directoryLabel(server.endpointKey, "/x/frontend").primary)
        assertTrue(index.isUserRoot(server, "/w/"))
        assertFalse(index.isUserRoot(server, "/w/repo-a"))
        assertEquals(
            WorkspaceKey.Directory("/w"),
            index.groupKeyFor(server, "/w/repo-a/sub"),
        )
    }

    @Test
    fun `same path on different endpoints keeps independent aliases`() {
        val path = path("/shared/project")
        val serverA = server(ENDPOINT_A, "Desktop")
        val serverB = server(ENDPOINT_B, "Laptop")
        val state = UserWorkspaceState(
            mapOf(
                ENDPOINT_A to serverData(aliases = mapOf(path to "Desktop project")),
                ENDPOINT_B to serverData(aliases = mapOf(path to "Laptop project")),
            ),
        )
        val index = WorkspaceLabelIndex.build(state, emptyMap())

        assertEquals("Desktop project", index.directoryLabel(serverA.endpointKey, path.value).primary)
        assertEquals("Laptop project", index.directoryLabel(serverB.endpointKey, path.value).primary)
    }

    @Test
    fun `search text includes custom and generated names path and server metadata`() {
        val server = server(ENDPOINT_A, "Desktop Work")
        val workspacePath = path("/apps/frontend")
        val index = WorkspaceLabelIndex.build(
            workspaceState(ENDPOINT_A, aliases = mapOf(workspacePath to "Shop UI")),
            emptyMap(),
        )

        val searchText = index.searchText(server, WorkspaceKey.Directory(workspacePath.value))

        listOf("Shop UI", "frontend", workspacePath.value, server.displayName, server.badgeLabel).forEach { term ->
            assertTrue("search text should contain $term", term in searchText)
        }
    }

    @Test
    fun `primary labels are absent for global and session scoped keys`() {
        val server = server(ENDPOINT_A, "Desktop")

        assertNull(WorkspaceLabelIndex.Empty.primaryFor(server, WorkspaceKey.Global))
        assertNull(
            WorkspaceLabelIndex.Empty.primaryFor(
                server,
                WorkspaceKey.SessionScoped(SessionId("session-1")),
            ),
        )
    }

    @Test
    fun `empty index uses the final path component`() {
        val label = WorkspaceLabelIndex.Empty.directoryLabel(null, "/repo/project/")

        assertEquals("project", label.primary)
        assertEquals("/repo/project", label.path)
    }

    @Test
    fun `unparseable directories retain their path and last segment label`() {
        val directory = "relative/project"
        val label = WorkspaceLabelIndex.Empty.directoryLabel(null, directory)

        assertEquals("project", label.primary)
        assertEquals(directory, label.path)
    }

    private fun workspaceState(
        endpointKey: String,
        roots: List<ServerPath> = emptyList(),
        aliases: Map<ServerPath, String> = emptyMap(),
    ) = UserWorkspaceState(
        mapOf(endpointKey to serverData(roots, aliases)),
    )

    private fun serverData(
        roots: List<ServerPath> = emptyList(),
        aliases: Map<ServerPath, String> = emptyMap(),
    ) = ServerUserWorkspaces(
        roots = roots.map { UserWorkspaceRoot(it, ADDED_AT) },
        aliases = aliases.entries.associate { (path, alias) ->
            path.compareKey to StoredWorkspaceAlias(path, alias)
        },
    )

    private fun server(endpointKey: String, name: String) = ServerRef.fromEndpointKey(endpointKey, name)

    private fun path(input: String): ServerPath =
        (ServerPath.parseUserInput(input) as ServerPathParse.Valid).path

    private companion object {
        const val ADDED_AT = 1_760_000_000_000L
        const val ENDPOINT_A = "https://a.example:4096"
        const val ENDPOINT_B = "https://b.example:4096"
    }
}
