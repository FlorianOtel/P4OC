package dev.blazelight.p4oc.ui.tabs

import dev.blazelight.p4oc.core.datastore.PersistedTab
import dev.blazelight.p4oc.core.datastore.PersistedTabState
import dev.blazelight.p4oc.core.datastore.PersistedWorkspaceKey
import dev.blazelight.p4oc.domain.server.ServerRef
import dev.blazelight.p4oc.domain.server.WorkspaceKey
import dev.blazelight.p4oc.ui.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TabManagerPersistenceTest {
    private val server = ServerRef.fromEndpointKey("http://localhost:4096")

    @Test
    fun `saveState writes versioned tabs with server endpoint key`() {
        val manager = TabManager()
        val tab = manager.createTab(
            startRoute = Screen.Sessions.route,
            workspaceKey = WorkspaceKey.Global,
            serverRef = server,
            focus = true,
        )
        manager.updateTabWorkspace(tab.id, WorkspaceKey.Directory("/repo/a"))
        manager.updateTabSession(tab.id, "s1", "Title")

        val saved = manager.saveState()!!

        assertEquals(PersistedTabState.CURRENT_VERSION, saved.version)
        assertEquals(server.endpointKey, saved.serverEndpointKey)
        assertEquals(tab.id, saved.activeTabId)
        assertEquals("s1", saved.tabs.single().sessionId)
        assertEquals(PersistedWorkspaceKey.Type.DIRECTORY, saved.tabs.single().workspaceKey?.type)
        assertEquals("/repo/a", saved.tabs.single().workspaceKey?.value)
        assertEquals(server.endpointKey, saved.tabs.single().serverEndpointKey)
    }

    @Test
    fun `restoreState restores session tab as chat route in same workspace`() {
        val manager = TabManager()
        val state = PersistedTabState(
            serverEndpointKey = server.endpointKey,
            activeTabId = "tab-1",
            tabs = listOf(
                PersistedTab(
                    id = "tab-1",
                    startRoute = Screen.Sessions.route,
                    sessionId = "session with space",
                    sessionTitle = "Chat",
                    workspaceKey = PersistedWorkspaceKey(PersistedWorkspaceKey.Type.DIRECTORY, "/repo/a b"),
                ),
            ),
        )

        val result = manager.restoreState(state, server)

        assertTrue(result is RestoreResult.Restored)
        assertEquals("tab-1", manager.activeTabId.value)
        val workTab = manager.tabs.value.single { !it.isPinnedHome }
        assertEquals("session with space", workTab.sessionId)
        assertEquals("/repo/a b", workTab.workspaceDirectory)
        assertEquals(server.endpointKey, workTab.serverEndpointKey)
        assertEquals("chat/session%20with%20space", workTab.startRoute)
    }

    @Test
    fun `restoreState drops tab with missing workspace key instead of restoring as global`() {
        val manager = TabManager()
        val state = PersistedTabState(
            serverEndpointKey = server.endpointKey,
            activeTabId = "ambiguous-tab",
            tabs = listOf(
                PersistedTab(
                    id = "ambiguous-tab",
                    startRoute = Screen.Sessions.route,
                    workspaceKey = null,
                ),
            ),
        )

        val result = manager.restoreState(state, server)

        assertTrue(result is RestoreResult.Empty)
        assertEquals(listOf(TabInstance.HOME_TAB_ID), manager.tabs.value.map { it.id })
        assertEquals(TabInstance.HOME_TAB_ID, manager.activeTabId.value)
    }

    @Test
    fun `restoreState keeps valid tabs and falls back active tab when ambiguous active tab is dropped`() {
        val manager = TabManager()
        val state = PersistedTabState(
            serverEndpointKey = server.endpointKey,
            activeTabId = "ambiguous-tab",
            tabs = listOf(
                PersistedTab(
                    id = "ambiguous-tab",
                    startRoute = Screen.Sessions.route,
                    workspaceKey = null,
                ),
                PersistedTab(
                    id = "directory-tab",
                    startRoute = Screen.Sessions.route,
                    workspaceKey = PersistedWorkspaceKey(PersistedWorkspaceKey.Type.DIRECTORY, "/repo/valid"),
                ),
                PersistedTab(
                    id = "global-tab",
                    startRoute = Screen.Sessions.route,
                    workspaceKey = PersistedWorkspaceKey(PersistedWorkspaceKey.Type.GLOBAL),
                ),
            ),
        )

        val result = manager.restoreState(state, server)

        assertTrue(result is RestoreResult.Restored)
        assertEquals(2, (result as RestoreResult.Restored).count)
        val workTabs = manager.tabs.value.filterNot { it.isPinnedHome }
        assertEquals(listOf("directory-tab", "global-tab"), workTabs.map { it.id })
        assertEquals("directory-tab", manager.activeTabId.value)
        assertEquals("/repo/valid", workTabs[0].workspaceDirectory)
        assertEquals(WorkspaceKey.Global, workTabs[1].workspaceKey)
    }

    @Test
    fun `restoreState skips workspace records with missing required values`() {
        val manager = TabManager()
        val state = PersistedTabState(
            serverEndpointKey = server.endpointKey,
            activeTabId = "missing-directory",
            tabs = listOf(
                PersistedTab(
                    id = "missing-directory",
                    startRoute = Screen.Files.route,
                    workspaceKey = PersistedWorkspaceKey(PersistedWorkspaceKey.Type.DIRECTORY, null),
                ),
                PersistedTab(
                    id = "missing-session",
                    startRoute = Screen.Sessions.route,
                    workspaceKey = PersistedWorkspaceKey(PersistedWorkspaceKey.Type.SESSION_SCOPED, null),
                ),
                PersistedTab(
                    id = "valid-tab",
                    startRoute = Screen.Sessions.route,
                    workspaceKey = PersistedWorkspaceKey(PersistedWorkspaceKey.Type.GLOBAL),
                ),
            ),
        )

        val result = manager.restoreState(state, server)

        assertTrue(result is RestoreResult.Restored)
        assertEquals(1, (result as RestoreResult.Restored).count)
        assertEquals(listOf(TabInstance.HOME_TAB_ID, "valid-tab"), manager.tabs.value.map { it.id })
        assertEquals("valid-tab", manager.activeTabId.value)
    }

    @Test
    fun `restoreState retains first valid duplicate ID and honors it as active`() {
        val manager = TabManager()
        val state = PersistedTabState(
            serverEndpointKey = server.endpointKey,
            activeTabId = "duplicate",
            tabs = listOf(
                PersistedTab(
                    id = "duplicate",
                    startRoute = Screen.Files.route,
                    workspaceKey = PersistedWorkspaceKey(PersistedWorkspaceKey.Type.DIRECTORY, "/first"),
                ),
                PersistedTab(
                    id = "duplicate",
                    startRoute = Screen.Sessions.route,
                    workspaceKey = PersistedWorkspaceKey(PersistedWorkspaceKey.Type.DIRECTORY, "/second"),
                ),
            ),
        )

        val result = manager.restoreState(state, server)

        assertTrue(result is RestoreResult.Restored)
        assertEquals(1, (result as RestoreResult.Restored).count)
        val workTab = manager.tabs.value.single { !it.isPinnedHome }
        assertEquals("duplicate", workTab.id)
        assertEquals("/first", workTab.workspaceDirectory)
        assertEquals(Screen.Files.route, workTab.startRoute)
        assertEquals("duplicate", manager.activeTabId.value)
    }

    @Test
    fun `restoreState lets valid occurrence follow malformed duplicate ID`() {
        val manager = TabManager()
        val state = PersistedTabState(
            serverEndpointKey = server.endpointKey,
            activeTabId = "duplicate",
            tabs = listOf(
                PersistedTab(
                    id = "duplicate",
                    startRoute = Screen.Files.route,
                    workspaceKey = PersistedWorkspaceKey(PersistedWorkspaceKey.Type.DIRECTORY, null),
                ),
                PersistedTab(
                    id = "duplicate",
                    startRoute = Screen.Sessions.route,
                    workspaceKey = PersistedWorkspaceKey(PersistedWorkspaceKey.Type.DIRECTORY, "/valid"),
                ),
            ),
        )

        val result = manager.restoreState(state, server)

        assertTrue(result is RestoreResult.Restored)
        assertEquals(1, (result as RestoreResult.Restored).count)
        assertEquals("/valid", manager.tabs.value.single { !it.isPinnedHome }.workspaceDirectory)
        assertEquals("duplicate", manager.activeTabId.value)
    }

    @Test
    fun `restoreState rejects work tab using reserved Home identity`() {
        val manager = TabManager()
        val state = PersistedTabState(
            serverEndpointKey = server.endpointKey,
            activeTabId = TabInstance.HOME_TAB_ID,
            tabs = listOf(
                PersistedTab(
                    id = TabInstance.HOME_TAB_ID,
                    startRoute = Screen.Files.route,
                    workspaceKey = PersistedWorkspaceKey(PersistedWorkspaceKey.Type.DIRECTORY, "/corrupt"),
                ),
                PersistedTab(
                    id = "valid-tab",
                    startRoute = Screen.Sessions.route,
                    workspaceKey = PersistedWorkspaceKey(PersistedWorkspaceKey.Type.GLOBAL),
                ),
            ),
        )

        val result = manager.restoreState(state, server)

        assertTrue(result is RestoreResult.Restored)
        assertEquals(listOf(TabInstance.HOME_TAB_ID, "valid-tab"), manager.tabs.value.map { it.id })
        assertEquals(TabInstance.HOME_TAB_ID, manager.activeTabId.value)
    }

    @Test
    fun `restoreState reports missing server without restoring wrong server`() {
        val manager = TabManager()
        val oldServer = ServerRef.fromEndpointKey("http://old.example:4096")
        val state = PersistedTabState(
            serverEndpointKey = oldServer.endpointKey,
            activeTabId = "tab-1",
            tabs = listOf(
                PersistedTab(
                    id = "tab-1",
                    startRoute = Screen.Sessions.route,
                    workspaceKey = PersistedWorkspaceKey(PersistedWorkspaceKey.Type.GLOBAL),
                    serverEndpointKey = oldServer.endpointKey,
                ),
            ),
        )

        val result = manager.restoreState(state, mapOf(server.endpointKey to server))

        assertTrue(result is RestoreResult.MissingServer)
        assertEquals(oldServer.endpointKey, (result as RestoreResult.MissingServer).endpointKey)
        assertEquals(listOf(TabInstance.HOME_TAB_ID), manager.tabs.value.map { it.id })
    }

    @Test
    fun `restoreState rejects unsupported version`() {
        val manager = TabManager()
        val state = PersistedTabState(
            version = PersistedTabState.CURRENT_VERSION + 1,
            serverEndpointKey = server.endpointKey,
            activeTabId = "tab-1",
            tabs = listOf(PersistedTab(id = "tab-1", startRoute = Screen.Sessions.route)),
        )

        val result = manager.restoreState(state, server)

        assertTrue(result is RestoreResult.VersionMismatch)
        assertFalse(manager.hasTabs())
    }

    @Test
    fun `restoreState restores mixed-server tabs when all servers are available`() {
        val manager = TabManager()
        val beta = ServerRef.fromEndpointKey("http://beta.example:4096")
        val state = PersistedTabState(
            serverEndpointKey = server.endpointKey,
            activeTabId = "beta-files",
            tabs = listOf(
                PersistedTab(
                    id = "alpha-chat",
                    startRoute = Screen.Sessions.route,
                    sessionId = "s-alpha",
                    workspaceKey = PersistedWorkspaceKey(PersistedWorkspaceKey.Type.DIRECTORY, "/alpha"),
                    serverEndpointKey = server.endpointKey,
                ),
                PersistedTab(
                    id = "beta-files",
                    startRoute = Screen.Files.route,
                    workspaceKey = PersistedWorkspaceKey(PersistedWorkspaceKey.Type.DIRECTORY, "/beta"),
                    serverEndpointKey = beta.endpointKey,
                ),
            ),
        )

        val result = manager.restoreState(
            state,
            mapOf(server.endpointKey to server, beta.endpointKey to beta),
        )

        assertTrue(result is RestoreResult.Restored)
        assertEquals("beta-files", manager.activeTabId.value)
        val workTabs = manager.tabs.value.filterNot { it.isPinnedHome }
        assertEquals(listOf(server.endpointKey, beta.endpointKey), workTabs.map { it.serverEndpointKey })
        assertEquals(listOf("/alpha", "/beta"), workTabs.map { it.workspaceDirectory })
    }

    @Test
    fun `restoreState restores available tabs and reports unavailable mixed server`() {
        val manager = TabManager()
        val missing = ServerRef.fromEndpointKey("http://missing.example:4096")
        val state = PersistedTabState(
            serverEndpointKey = server.endpointKey,
            activeTabId = "missing-files",
            tabs = listOf(
                PersistedTab(
                    id = "alpha-chat",
                    startRoute = Screen.Sessions.route,
                    sessionId = "s-alpha",
                    workspaceKey = PersistedWorkspaceKey(PersistedWorkspaceKey.Type.DIRECTORY, "/alpha"),
                    serverEndpointKey = server.endpointKey,
                ),
                PersistedTab(
                    id = "missing-files",
                    startRoute = Screen.Files.route,
                    workspaceKey = PersistedWorkspaceKey(PersistedWorkspaceKey.Type.DIRECTORY, "/missing"),
                    serverEndpointKey = missing.endpointKey,
                ),
            ),
        )

        val result = manager.restoreState(state, mapOf(server.endpointKey to server))

        assertTrue(result is RestoreResult.MissingServer)
        assertEquals(missing.endpointKey, (result as RestoreResult.MissingServer).endpointKey)
        assertEquals(1, result.restoredCount)
        assertEquals(listOf(TabInstance.HOME_TAB_ID, "alpha-chat"), manager.tabs.value.map { it.id })
        assertEquals("alpha-chat", manager.activeTabId.value)
    }

    @Test
    fun `pinned Home is leftmost non-closeable and not duplicated`() {
        val manager = TabManager()

        manager.ensureHomeTab(focus = true)
        manager.ensureHomeTab(focus = false)
        val work = manager.createTab(
            startRoute = Screen.Files.route,
            workspaceKey = WorkspaceKey.Global,
            serverRef = server,
            focus = true,
        )

        assertEquals(listOf(TabInstance.HOME_TAB_ID, work.id), manager.tabs.value.map { it.id })
        manager.closeTab(TabInstance.HOME_TAB_ID)
        assertEquals(listOf(TabInstance.HOME_TAB_ID, work.id), manager.tabs.value.map { it.id })
    }

    @Test
    fun `saveState does not persist pinned Home as normal tab`() {
        val manager = TabManager()
        manager.ensureHomeTab(focus = true)
        manager.createTab(
            startRoute = Screen.Files.route,
            workspaceKey = WorkspaceKey.Global,
            serverRef = server,
            focus = true,
        )

        val saved = manager.saveState()!!

        assertEquals(1, saved.tabs.size)
        assertEquals(Screen.Files.route, saved.tabs.single().startRoute)
    }

    @Test
    fun `saveState returns null when pinned Home is the only tab`() {
        val manager = TabManager()
        manager.ensureHomeTab(focus = true)

        assertNull(manager.saveState())
    }

    @Test
    fun `saveState preserves each work tab server instead of using one server fallback`() {
        val alpha = ServerRef.fromEndpointKey("http://alpha.example:4096")
        val beta = ServerRef.fromEndpointKey("http://beta.example:4096")
        val manager = TabManager()
        manager.createTab(
            startRoute = Screen.Files.route,
            workspaceKey = WorkspaceKey.Directory("/alpha"),
            serverRef = alpha,
            focus = false,
        )
        manager.createTab(
            startRoute = Screen.Sessions.route,
            workspaceKey = WorkspaceKey.Directory("/beta"),
            serverRef = beta,
            focus = true,
        )

        val saved = manager.saveState()!!

        assertEquals(
            listOf(alpha.endpointKey, beta.endpointKey),
            saved.tabs.map { it.serverEndpointKey },
        )
        assertEquals(listOf("/alpha", "/beta"), saved.tabs.map { it.workspaceKey?.value })
    }

    @Test
    fun `saveState drops ownerless work tabs without borrowing another tab owner`() {
        val manager = TabManager()
        val owned = manager.createTab(
            startRoute = Screen.Files.route,
            workspaceKey = WorkspaceKey.Directory("/owned"),
            serverRef = server,
            focus = false,
        )
        manager.registerTab(
            TabInstance(
                state = TabState(workspaceKey = WorkspaceKey.Directory("/missing-server")),
                startRoute = Screen.Files.route,
            ),
            focus = false,
        )
        manager.registerTab(
            TabInstance(
                state = TabState(serverRef = server),
                startRoute = Screen.Sessions.route,
            ),
            focus = true,
        )

        val saved = manager.saveState()!!

        assertEquals(listOf(owned.id), saved.tabs.map { it.id })
        assertEquals(server.endpointKey, saved.tabs.single().serverEndpointKey)
        assertEquals("/owned", saved.tabs.single().workspaceKey?.value)
    }

    @Test
    fun `app restart restores Home plus Alpha chat Beta files and Local terminal safely`() {
        val alpha = ServerRef.fromEndpointKey("http://alpha.example:4096")
        val beta = ServerRef.fromEndpointKey("http://beta.example:4096")
        val local = ServerRef.fromEndpointKey("http://localhost:4096")
        val beforeRestart = TabManager()
        beforeRestart.ensureHomeTab(focus = false)
        val alphaChat = beforeRestart.createTab(
            startRoute = Screen.Chat.createRoute("alpha-session"),
            workspaceKey = WorkspaceKey.Directory("/alpha"),
            serverRef = alpha,
            focus = true,
        )
        beforeRestart.updateTabSession(alphaChat.id, "alpha-session", "Alpha chat")
        beforeRestart.createTab(
            startRoute = Screen.Files.route,
            workspaceKey = WorkspaceKey.Directory("/beta"),
            serverRef = beta,
            focus = true,
        )
        beforeRestart.createTab(
            startRoute = Screen.Terminal.createRoute("pty-local"),
            workspaceKey = WorkspaceKey.Directory("/local"),
            serverRef = local,
            focus = true,
        )
        val persisted = beforeRestart.saveState()!!

        val afterRestart = TabManager()
        val result = afterRestart.restoreState(
            persisted,
            mapOf(
                alpha.endpointKey to alpha,
                beta.endpointKey to beta,
                local.endpointKey to local,
            ),
        )

        assertTrue(result is RestoreResult.Restored)
        assertEquals(TabInstance.HOME_TAB_ID, afterRestart.tabs.value.first().id)
        val restoredWorkTabs = afterRestart.tabs.value.filterNot { it.isPinnedHome }
        assertEquals(
            listOf(alpha.endpointKey, beta.endpointKey, local.endpointKey),
            restoredWorkTabs.map { it.serverEndpointKey },
        )
        assertEquals(listOf("/alpha", "/beta", "/local"), restoredWorkTabs.map { it.workspaceDirectory })
        assertEquals("chat/alpha-session", restoredWorkTabs[0].startRoute)
        assertEquals(Screen.Files.route, restoredWorkTabs[1].startRoute)
        assertEquals(Screen.Sessions.route, restoredWorkTabs[2].startRoute)
    }

    @Test
    fun `focusOrCreateFilesTab focuses existing files tab for server workspace`() {
        val manager = TabManager()
        val workspace = WorkspaceKey.Directory("/repo")
        val first = manager.focusOrCreateFilesTab(server, workspace)
        val second = manager.focusOrCreateFilesTab(server, workspace)

        assertEquals(first.id, second.id)
        assertEquals(first.id, manager.activeTabId.value)
        assertEquals(1, manager.tabs.value.count { it.startRoute == Screen.Files.route && !it.isPinnedHome })
    }

    @Test
    fun `files focus helper separates identical workspaces on different servers`() {
        val manager = TabManager()
        val workspace = WorkspaceKey.Directory("/repo")
        val otherServer = ServerRef.fromEndpointKey("http://other.example:4096")
        val first = manager.focusOrCreateFilesTab(server, workspace)
        val second = manager.focusOrCreateFilesTab(otherServer, workspace)

        assertEquals(2, manager.tabs.value.count { it.startRoute == Screen.Files.route && !it.isPinnedHome })
        assertEquals(
            listOf(server.endpointKey, otherServer.endpointKey),
            listOf(first.serverEndpointKey, second.serverEndpointKey),
        )
    }

    @Test
    fun `terminal tabs are found by explicit server workspace`() {
        val manager = TabManager()
        val workspace = WorkspaceKey.Directory("/repo")
        val otherWorkspace = WorkspaceKey.Directory("/other")
        manager.createTab(Screen.Terminal.createRoute("pty-1"), workspace, server, focus = true)
        manager.createTab(Screen.Terminal.createRoute("pty-2"), otherWorkspace, server, focus = true)

        assertEquals(listOf("terminal/pty-1"), manager.findTerminalTabs(server, workspace).map { it.startRoute })
    }

    @Test
    fun `dynamic terminal and diff route arguments are encoded`() {
        assertEquals("terminal/pty%2Fwith%20space", Screen.Terminal.createRoute("pty/with space"))
        assertEquals("session_diff/session%2Fwith%20space", Screen.SessionDiff.createRoute("session/with space"))
    }

    @Test
    fun `createPtyRequestForWorkspace uses target workspace cwd`() {
        assertEquals("/repo", createPtyRequestForWorkspace(WorkspaceKey.Directory("/repo")).cwd)
        assertEquals(null, createPtyRequestForWorkspace(WorkspaceKey.Global).cwd)
    }

    @Test
    fun `terminal routes are not persisted as resurrectable tabs`() {
        val manager = TabManager()
        manager.createTab(
            startRoute = Screen.Terminal.createRoute("pty-1"),
            workspaceKey = WorkspaceKey.Global,
            serverRef = server,
            focus = true,
        )

        val saved = manager.saveState()!!

        assertEquals(Screen.Sessions.route, saved.tabs.single().startRoute)
    }

    @Test
    fun `unbound new chat route drops transient focus intent on save`() {
        val manager = TabManager()
        manager.createTab(
            startRoute = Screen.Chat.createRoute("new/session with space", focusInput = true),
            workspaceKey = WorkspaceKey.Directory("/repo"),
            serverRef = server,
            focus = true,
        )

        val saved = manager.saveState()!!

        assertEquals("chat/new%2Fsession%20with%20space", saved.tabs.single().startRoute)
        assertNull(saved.tabs.single().sessionId)

        val restored = TabManager()
        val result = restored.restoreState(saved, server)

        assertTrue(result is RestoreResult.Restored)
        val restoredTab = restored.tabs.value.single { !it.isPinnedHome }
        assertEquals("chat/new%2Fsession%20with%20space", restoredTab.startRoute)
        assertNull(restoredTab.sessionId)
    }
}

@Suppress("unused")
private fun mixedServerPersistenceCompileAnchor() {}
