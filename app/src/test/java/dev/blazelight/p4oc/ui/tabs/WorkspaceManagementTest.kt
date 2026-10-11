package dev.blazelight.p4oc.ui.tabs

import dev.blazelight.p4oc.data.session.RepoState
import dev.blazelight.p4oc.data.session.Snapshot
import dev.blazelight.p4oc.domain.model.Session
import dev.blazelight.p4oc.domain.server.ServerRef
import dev.blazelight.p4oc.domain.server.WorkspaceKey
import dev.blazelight.p4oc.domain.session.SessionId
import dev.blazelight.p4oc.domain.session.WorkspaceSession
import dev.blazelight.p4oc.domain.workspace.ServerPath
import dev.blazelight.p4oc.domain.workspace.ServerUserWorkspaces
import dev.blazelight.p4oc.domain.workspace.StoredWorkspaceAlias
import dev.blazelight.p4oc.domain.workspace.UserWorkspaceRoot
import dev.blazelight.p4oc.domain.workspace.UserWorkspaceState
import dev.blazelight.p4oc.domain.workspace.Workspace
import dev.blazelight.p4oc.ui.screens.home.ScopedHomeRepositoryState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkspaceManagementTest {
    private val alpha = ServerRef.fromEndpointKey("http://alpha.test", "Alpha")
    private val beta = ServerRef.fromEndpointKey("http://beta.test", "Beta")

    @Test
    fun `only servers whose root set changed are refreshed`() {
        val before = state(alpha to listOf("/a"), beta to listOf("/b"))
        val betaWithAlias = before.forServer(beta.endpointKey).copy(
            aliases = mapOf(path("/b").compareKey to StoredWorkspaceAlias(path("/b"), "Bee")),
        )
        val renamedOnly = UserWorkspaceState(before.byEndpoint + (beta.endpointKey to betaWithAlias))
        val added = state(alpha to listOf("/a", "/a2"), beta to listOf("/b"))

        assertEquals(emptySet<String>(), changedRootEndpoints(before, renamedOnly))
        assertEquals(setOf(alpha.endpointKey), changedRootEndpoints(before, added))
        assertEquals(setOf(beta.endpointKey), changedRootEndpoints(before, state(alpha to listOf("/a"))))
    }

    @Test
    fun `label directories combine session and open tab directories per server`() {
        val repositories = listOf(
            repository(alpha, session(alpha, "s1", "/repo/a", 1L), session(alpha, "s2", "", 2L)),
            repository(beta, session(beta, "s3", "/repo/a", 3L)),
        )
        val tabs = listOf(
            tab(alpha, WorkspaceKey.Directory("/repo/b")),
            tab(alpha, WorkspaceKey.Global),
        )

        val directories = workspaceDirectoriesByEndpoint(repositories, tabs)

        assertEquals(listOf("/repo/a", "/repo/b"), directories[alpha.endpointKey])
        assertEquals(listOf("/repo/a"), directories[beta.endpointKey])
    }

    @Test
    fun `browse starts at the parent of the most recently updated session directory on that server`() {
        val repositories = listOf(
            repository(
                alpha,
                session(alpha, "old", "/srv/old/project", 1L),
                session(alpha, "new", "/srv/new/project", 9L),
            ),
            repository(beta, session(beta, "other", "/beta/project", 20L)),
        )

        assertEquals("/srv/new", browseStartFor(alpha.endpointKey, repositories, emptyList())?.value)
        assertEquals(
            "/tabs",
            browseStartFor(alpha.endpointKey, emptyList(), listOf(tab(alpha, WorkspaceKey.Directory("/tabs/x"))))
                ?.value,
        )
        assertNull(browseStartFor("http://unknown.test", repositories, emptyList()))
    }

    private fun path(value: String) = ServerPath.parseOrNull(value)!!

    private fun state(vararg servers: Pair<ServerRef, List<String>>) = UserWorkspaceState(
        servers.associate { (server, roots) ->
            server.endpointKey to ServerUserWorkspaces(roots = roots.map { UserWorkspaceRoot(path(it), 1L) })
        },
    )

    private fun repository(server: ServerRef, vararg sessions: WorkspaceSession) = ScopedHomeRepositoryState(
        server,
        RepoState.Live(Snapshot(sessions = sessions.associateBy { it.id.value })),
    )

    private fun session(server: ServerRef, id: String, directory: String, updatedAt: Long) = WorkspaceSession(
        id = SessionId(id),
        workspace = Workspace(server, directory.takeIf { it.isNotBlank() }),
        session = Session(
            id = id,
            projectID = "project-$id",
            directory = directory,
            title = id,
            version = "1",
            createdAt = 1L,
            updatedAt = updatedAt,
        ),
    )

    private fun tab(server: ServerRef, key: WorkspaceKey) = TabInstance(
        state = TabState(workspaceKey = key, serverRef = server),
        startRoute = "files",
    )
}
