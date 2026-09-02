package dev.blazelight.p4oc.ui.screens.home

import dev.blazelight.p4oc.core.datastore.SavedServer
import dev.blazelight.p4oc.core.datastore.SavedServerRegistry
import dev.blazelight.p4oc.core.network.ConnectionState
import dev.blazelight.p4oc.data.session.RepoState
import dev.blazelight.p4oc.data.session.Snapshot
import dev.blazelight.p4oc.domain.model.Session
import dev.blazelight.p4oc.domain.model.SessionPresence
import dev.blazelight.p4oc.domain.model.SessionStatus
import dev.blazelight.p4oc.domain.server.ServerRef
import dev.blazelight.p4oc.domain.server.WorkspaceKey
import dev.blazelight.p4oc.domain.session.SessionId
import dev.blazelight.p4oc.domain.session.WorkspaceSession
import dev.blazelight.p4oc.domain.workspace.Workspace
import dev.blazelight.p4oc.ui.navigation.Screen
import dev.blazelight.p4oc.ui.tabs.TabInstance
import dev.blazelight.p4oc.ui.tabs.TabState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeSummaryBuilderTest {
    @Test
    fun `summary applies independent workspace and open work limits`() {
        val (server, serverRef) = server("http://alpha.example.com", "Alpha")
        val tabs = (1..30).map { index ->
            tab(
                TabInput(
                    id = "tab-$index",
                    serverRef = serverRef,
                    directory = "/repo-$index",
                    route = Screen.Chat.createRoute("session-$index"),
                    sessionId = "session-$index",
                    sessionTitle = "Session $index",
                ),
            )
        }

        val summary = build(
            servers = listOf(server),
            tabs = tabs,
            workspaceLimit = 5,
            openWorkLimit = 7,
        )

        assertEquals(30, summary.servers.single().openTabCount)
        assertEquals((1..7).map { "tab-$it" }, summary.openWork.map { it.tabId })
        assertEquals(5, summary.workspaces.size)
    }

    @Test
    fun `offline server does not prevent connected server summary`() {
        val (connected, _) = server("http://alpha.example.com", "Alpha")
        val (offline, _) = server("http://beta.example.com", "Beta")

        val summary = HomeSummaryBuilder.build(
            HomeSummaryInput(
                savedServers = listOf(connected, offline),
                connectionStates = mapOf(connected.endpointKey to ConnectionState.Connected),
                tabs = emptyList(),
            ),
        )

        assertEquals(ConnectionState.Connected, summary.servers.first { it.displayName == "Alpha" }.connectionState)
        assertEquals(ConnectionState.Disconnected, summary.servers.first { it.displayName == "Beta" }.connectionState)
        assertTrue(summary.partialFailures.isEmpty())
    }

    @Test
    fun `hydrated sessions create workspaces and recency ordered previews without open tabs`() {
        val (server, serverRef) = server("http://alpha.example.com", "Alpha")
        val repository = scopedRepository(
            serverRef,
            RepoState.Live(
                snapshot(
                    workspaceSession(serverRef, "older", "/one", "Older", updatedAt = 10L),
                    workspaceSession(serverRef, "newer", "/two", "", updatedAt = 30L),
                    statuses = mapOf("newer" to SessionStatus.Busy),
                ),
            ),
        )

        val summary = build(servers = listOf(server), repositories = listOf(repository))

        assertEquals(2, summary.servers.single().sessionCount)
        assertEquals(listOf("newer", "older"), summary.sessions.map { it.sessionId.value })
        assertEquals(listOf(30L, 10L), summary.sessions.map { it.updatedAt })
        assertEquals("Untitled session", summary.sessions.first().title)
        assertEquals(SessionPresence.BUSY, summary.sessions.first().status)
        assertEquals(
            listOf("/two", "/one"),
            summary.workspaces.map { (it.workspaceKey as WorkspaceKey.Directory).value },
        )
        assertTrue(summary.workspaces.all { it.sessionCount == 1 && it.openTabCount == 0 })
    }

    @Test
    fun `same directory and session id remain scoped to their owning server`() {
        val (alpha, alphaRef) = server("http://alpha.example.com", "Alpha")
        val (beta, betaRef) = server("http://beta.example.com", "Beta")
        val alphaRepository = scopedRepository(
            alphaRef,
            RepoState.Live(snapshot(workspaceSession(alphaRef, "shared", "/repo", "Alpha work", 10L))),
        )
        val betaRepository = scopedRepository(
            betaRef,
            RepoState.Live(
                snapshot(
                    workspaceSession(betaRef, "shared", "/repo", "Beta work", 20L),
                    statuses = mapOf("shared" to SessionStatus.Busy),
                ),
            ),
        )
        val betaTab = tab(
            TabInput(
                id = "beta-chat",
                serverRef = betaRef,
                directory = "/repo",
                route = Screen.Chat.createRoute("shared"),
                sessionId = "shared",
            ),
        )

        val summary = build(
            servers = listOf(alpha, beta),
            tabs = listOf(betaTab),
            repositories = listOf(alphaRepository, betaRepository),
        )

        assertEquals(
            setOf(alphaRef.endpointKey, betaRef.endpointKey),
            summary.workspaces.map { it.serverRef.endpointKey }.toSet(),
        )
        assertEquals(
            0,
            summary.workspaces.single { workspace ->
                workspace.serverRef.endpointKey == alphaRef.endpointKey
            }.openTabCount,
        )
        assertEquals(
            1,
            summary.workspaces.single { workspace ->
                workspace.serverRef.endpointKey == betaRef.endpointKey
            }.openTabCount,
        )
        assertEquals("Beta work", summary.openWork.single().title)
        assertEquals(SessionPresence.BUSY, summary.openWork.single().status)
    }

    @Test
    fun `chat tab does not borrow session details from another workspace`() {
        val (server, serverRef) = server("http://alpha.example.com", "Alpha")
        val repository = scopedRepository(
            serverRef,
            RepoState.Live(
                snapshot(
                    workspaceSession(serverRef, "shared", "/other", "Other workspace", 10L),
                    statuses = mapOf("shared" to SessionStatus.Busy),
                ),
            ),
        )
        val chat = tab(
            TabInput(
                id = "target-chat",
                serverRef = serverRef,
                directory = "/target",
                route = Screen.Chat.createRoute("shared"),
                sessionId = "shared",
            ),
        )

        val openWork = build(
            servers = listOf(server),
            tabs = listOf(chat),
            repositories = listOf(repository),
        ).openWork.single()

        assertEquals("Chat", openWork.title)
        assertEquals(null, openWork.status)
    }

    @Test
    fun `open work is typed and excludes home unowned and unsupported tabs`() {
        val (server, serverRef) = server("http://alpha.example.com", "Alpha")
        val tabs = listOf(
            TabInstance.home(),
            tab(
                TabInput(
                    id = "chat",
                    serverRef = serverRef,
                    directory = "/repo",
                    route = Screen.Chat.createRoute("s1"),
                    sessionId = "s1",
                    sessionTitle = "Fix login",
                ),
            ),
            tab(TabInput("files", serverRef, "/repo", "files/src")),
            tab(TabInput("terminal", serverRef, "/repo", "terminal/pty-1")),
            tab(TabInput("unsupported", serverRef, "/repo", "settings")),
            TabInstance(TabState(id = "unowned"), startRoute = "files"),
        )

        val summary = build(servers = listOf(server), tabs = tabs)

        assertEquals(listOf("chat", "files", "terminal"), summary.openWork.map { it.tabId })
        assertEquals(
            listOf(OpenWorkType.Chat, OpenWorkType.Files, OpenWorkType.Terminal),
            summary.openWork.map { it.type },
        )
        assertEquals(
            listOf("Fix login", "Files", "Terminal"),
            summary.openWork.map { it.title },
        )
        assertEquals(4, summary.servers.single().openTabCount)
    }

    @Test
    fun `hydrating and stale repositories preserve snapshots while surfacing partial state`() {
        val (alpha, alphaRef) = server("http://alpha.example.com", "Alpha")
        val (beta, betaRef) = server("http://beta.example.com", "Beta")
        val hydrating = scopedRepository(
            alphaRef,
            RepoState.Hydrating(snapshot = snapshot(workspaceSession(alphaRef, "loading", "/alpha", "Loading", 5L))),
        )
        val stale = scopedRepository(
            betaRef,
            RepoState.Stale(
                snapshot = snapshot(workspaceSession(betaRef, "cached", "/beta", "Cached", 7L)),
                reason = "network unavailable",
            ),
        )

        val summary = build(servers = listOf(alpha, beta), repositories = listOf(hydrating, stale))

        assertTrue(summary.isLoading)
        assertTrue(summary.servers.single { it.serverRef.endpointKey == alphaRef.endpointKey }.isLoading)
        assertFalse(summary.servers.single { it.serverRef.endpointKey == betaRef.endpointKey }.isLoading)
        assertEquals(
            "network unavailable",
            summary.servers.single { it.serverRef.endpointKey == betaRef.endpointKey }.failure,
        )
        assertEquals(listOf("Beta: session data unavailable"), summary.partialFailures)
        assertEquals(setOf("loading", "cached"), summary.sessions.map { it.sessionId.value }.toSet())
        assertEquals(2, summary.workspaces.size)
    }

    @Test
    fun `workspace bound does not truncate sessions and sessions remain newest first`() {
        val (server, serverRef) = server("http://alpha.example.com", "Alpha")
        val sessions = (1..40).map { index ->
            workspaceSession(serverRef, "session-$index", "/repo-$index", "Work $index", index.toLong())
        }.toTypedArray()

        val summary = build(
            servers = listOf(server),
            repositories = listOf(scopedRepository(serverRef, RepoState.Live(snapshot(*sessions)))),
            workspaceLimit = 5,
        )

        assertEquals(5, summary.workspaces.size)
        assertEquals(40, summary.sessions.size)
        assertEquals((40 downTo 1).map { "session-$it" }, summary.sessions.map { it.sessionId.value })
    }

    @Test
    fun `query-only home search spans every saved server`() {
        val (alpha, alphaRef) = server("http://alpha.example.com", "Alpha")
        val (beta, betaRef) = server("http://beta.example.com", "Beta")
        val summary = build(
            servers = listOf(alpha, beta),
            repositories = listOf(
                scopedRepository(
                    alphaRef,
                    RepoState.Live(
                        snapshot(
                            workspaceSession(alphaRef, "old", "/shared/Needle", "Old task", 10L),
                            workspaceSession(alphaRef, "new", "/other", "NEEDLE title", 30L),
                        ),
                    ),
                ),
                scopedRepository(
                    betaRef,
                    RepoState.Live(
                        snapshot(
                            workspaceSession(betaRef, "beta", "/needle", "Needle beta", 50L),
                        ),
                    ),
                ),
            ),
        )

        val filtered = summary.filteredHomeResults("  needle ")

        assertEquals(listOf("beta", "new", "old"), filtered.sessions.map { it.sessionId.value })
        assertEquals(
            setOf(alphaRef.endpointKey, betaRef.endpointKey),
            filtered.workspaces.map { it.serverRef.endpointKey }.toSet(),
        )
    }

    @Test
    fun `blank home search preserves bounded workspaces and complete session order`() {
        val (server, serverRef) = server("http://alpha.example.com", "Alpha")
        val repository = scopedRepository(
            serverRef,
            RepoState.Live(
                snapshot(
                    workspaceSession(serverRef, "one", "/one", "One", 1L),
                    workspaceSession(serverRef, "three", "/three", "Three", 3L),
                    workspaceSession(serverRef, "two", "/two", "Two", 2L),
                ),
            ),
        )
        val summary = build(listOf(server), repositories = listOf(repository), workspaceLimit = 2)

        val filtered = summary.filteredHomeResults("   ")

        assertEquals(2, filtered.workspaces.size)
        assertEquals(listOf("three", "two", "one"), filtered.sessions.map { it.sessionId.value })
    }

    @Test
    fun `blank home search returns all workspaces and sessions across every server`() {
        val (alpha, alphaRef) = server("http://alpha.example.com", "Alpha")
        val (beta, betaRef) = server("http://beta.example.com", "Beta")
        val summary = build(
            servers = listOf(alpha, beta),
            repositories = listOf(
                scopedRepository(
                    alphaRef,
                    RepoState.Live(snapshot(workspaceSession(alphaRef, "alpha", "/alpha", "Alpha", 20L))),
                ),
                scopedRepository(
                    betaRef,
                    RepoState.Live(snapshot(workspaceSession(betaRef, "beta", "/beta", "Beta", 40L))),
                ),
            ),
        )

        val filtered = summary.filteredHomeResults("")

        assertEquals(listOf("beta", "alpha"), filtered.sessions.map { it.sessionId.value })
        assertEquals(
            setOf(alphaRef.endpointKey, betaRef.endpointKey),
            filtered.workspaces.map { it.serverRef.endpointKey }.toSet(),
        )
    }

    private fun build(
        servers: List<SavedServer>,
        tabs: List<TabInstance> = emptyList(),
        repositories: List<ScopedHomeRepositoryState> = emptyList(),
        workspaceLimit: Int = 12,
        openWorkLimit: Int = 24,
    ): HomeSummaryState = HomeSummaryBuilder.build(
        HomeSummaryInput(
            savedServers = servers,
            connectionStates = servers.associate { it.endpointKey to ConnectionState.Connected },
            tabs = tabs,
            repositories = repositories,
            workspaceLimit = workspaceLimit,
            openWorkLimit = openWorkLimit,
        ),
    )

    private fun server(url: String, name: String): Pair<SavedServer, ServerRef> {
        val saved = SavedServerRegistry.fromConnection(url, name)
        return saved to ServerRef.fromEndpointKey(saved.endpointKey, saved.displayName)
    }

    private fun scopedRepository(serverRef: ServerRef, state: RepoState) =
        ScopedHomeRepositoryState(serverRef, state)

    private fun snapshot(
        vararg sessions: WorkspaceSession,
        statuses: Map<String, SessionStatus> = emptyMap(),
    ) = Snapshot(
        sessions = sessions.associateBy { it.id.value },
        statuses = statuses,
    )

    private fun workspaceSession(
        serverRef: ServerRef,
        id: String,
        directory: String,
        title: String,
        updatedAt: Long,
    ) = WorkspaceSession(
        id = SessionId(id),
        workspace = Workspace(serverRef, directory),
        session = Session(
            id = id,
            projectID = "project-$id",
            directory = directory,
            title = title,
            version = "1",
            createdAt = 1L,
            updatedAt = updatedAt,
        ),
    )

    private data class TabInput(
        val id: String,
        val serverRef: ServerRef,
        val directory: String,
        val route: String,
        val sessionId: String? = null,
        val sessionTitle: String? = null,
    )

    private fun tab(input: TabInput) = TabInstance(
        state = TabState(
            id = input.id,
            workspaceKey = WorkspaceKey.Directory(input.directory),
            serverRef = input.serverRef,
            sessionId = input.sessionId,
            sessionTitle = input.sessionTitle,
        ),
        startRoute = input.route,
    )
}
