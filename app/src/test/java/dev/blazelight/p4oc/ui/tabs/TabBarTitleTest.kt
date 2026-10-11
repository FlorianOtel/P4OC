package dev.blazelight.p4oc.ui.tabs

import dev.blazelight.p4oc.domain.server.ServerRef
import dev.blazelight.p4oc.domain.server.WorkspaceKey
import dev.blazelight.p4oc.domain.session.SessionId
import dev.blazelight.p4oc.domain.workspace.ServerPath
import dev.blazelight.p4oc.domain.workspace.ServerUserWorkspaces
import dev.blazelight.p4oc.domain.workspace.StoredWorkspaceAlias
import dev.blazelight.p4oc.domain.workspace.UserWorkspaceRoot
import dev.blazelight.p4oc.domain.workspace.UserWorkspaceState
import dev.blazelight.p4oc.ui.workspace.WorkspaceLabelIndex
import org.junit.Assert.assertEquals
import org.junit.Test

class TabBarTitleTest {
    private val labels = TabTitleLabels(
        home = "Home",
        fallbackTab = "Tab",
        sessions = "Sessions",
        chat = "Chat",
        files = "Files",
        file = "File",
        terminal = "Terminal",
        settings = "Settings",
        projects = "Projects",
        globalWorkspace = "Global",
        sessionWorkspace = "Session",
    )

    private val server = ServerRef.fromEndpointKey(
        endpointKey = "https://work.example.test",
        displayName = "Production",
    )

    @Test
    fun `pinned Home keeps explicit identity despite work-like state`() {
        val home = TabInstance(
            state = TabState(
                id = TabInstance.HOME_TAB_ID,
                sessionId = "session-1",
                sessionTitle = "Investigate bug",
                workspaceKey = WorkspaceKey.Directory("/repo/project"),
                serverRef = server,
                pinnedHome = true,
            ),
            startRoute = "diff/1",
        )

        assertEquals("Home", getTitleForTab(home, labels, WorkspaceLabelIndex.Empty))
    }

    @Test
    fun `work title is derived from its start route and workspace rather than server presentation`() {
        val work = TabInstance(
            state = TabState(
                workspaceKey = WorkspaceKey.Directory("/repo/project"),
                serverRef = server,
            ),
            startRoute = "sessions",
        )

        assertEquals("Sessions · project", getTitleForTab(work, labels, WorkspaceLabelIndex.Empty))
    }

    @Test
    fun `session work title is derived from its session title and workspace`() {
        val work = TabInstance(
            state = TabState(
                sessionId = "session-1",
                sessionTitle = "Investigate bug",
                workspaceKey = WorkspaceKey.Directory("/repo/project"),
                serverRef = server,
            ),
            startRoute = "sessions",
        )

        assertEquals("Investigate bug · project", getTitleForTab(work, labels, WorkspaceLabelIndex.Empty))
    }

    @Test
    fun `global work uses localized No project context label`() {
        val work = TabInstance(
            state = TabState(workspaceKey = WorkspaceKey.Global, serverRef = server),
            startRoute = "sessions",
        )

        assertEquals(
            "Sessions · No project context",
            getTitleForTab(work, labels.copy(globalWorkspace = "No project context"), WorkspaceLabelIndex.Empty),
        )
    }

    @Test
    fun `custom workspace name replaces the path basename in tab titles`() {
        val path = ServerPath.parseOrNull("/repo/project")!!
        val index = WorkspaceLabelIndex.build(
            UserWorkspaceState(
                mapOf(
                    server.endpointKey to ServerUserWorkspaces(
                        aliases = mapOf(path.compareKey to StoredWorkspaceAlias(path, "Weather App")),
                    ),
                ),
            ),
            mapOf(server.endpointKey to listOf("/repo/project")),
        )
        val work = TabInstance(
            state = TabState(workspaceKey = WorkspaceKey.Directory("/repo/project"), serverRef = server),
            startRoute = "files",
        )

        assertEquals("Files · Weather App", getTitleForTab(work, labels, index))
    }

    @Test
    fun `tab inside a user root is named relative to the root`() {
        val root = ServerPath.parseOrNull("/w")!!
        val index = WorkspaceLabelIndex.build(
            UserWorkspaceState(
                mapOf(server.endpointKey to ServerUserWorkspaces(roots = listOf(UserWorkspaceRoot(root, 1L)))),
            ),
            mapOf(server.endpointKey to listOf("/w/repo-a/sub")),
        )
        val work = TabInstance(
            state = TabState(
                sessionId = "s1",
                sessionTitle = "Fix bug",
                workspaceKey = WorkspaceKey.Directory("/w/repo-a/sub"),
                serverRef = server,
            ),
            startRoute = "chat/s1",
        )

        assertEquals("Fix bug · w / repo-a / sub", getTitleForTab(work, labels, index))
    }

    @Test
    fun `same workspace name on two servers gets server badges only when both are open`() {
        val other = ServerRef.fromEndpointKey("https://laptop.example.test", "Laptop")
        val alpha = TabInstance(
            state = TabState(id = "a", workspaceKey = WorkspaceKey.Directory("/srv/api"), serverRef = server),
            startRoute = "files",
        )
        val beta = TabInstance(
            state = TabState(id = "b", workspaceKey = WorkspaceKey.Directory("/home/me/api"), serverRef = other),
            startRoute = "files",
        )
        val unique = TabInstance(
            state = TabState(id = "c", workspaceKey = WorkspaceKey.Directory("/srv/web"), serverRef = server),
            startRoute = "files",
        )

        val titles = getTitlesForTabs(listOf(alpha, beta, unique), labels, WorkspaceLabelIndex.Empty)

        assertEquals("Files · api · ${server.badgeLabel}", titles.getValue("a"))
        assertEquals("Files · api · ${other.badgeLabel}", titles.getValue("b"))
        assertEquals("Files · web", titles.getValue("c"))
        assertEquals(
            mapOf("a" to "Files · api"),
            getTitlesForTabs(listOf(alpha), labels, WorkspaceLabelIndex.Empty),
        )
    }

    @Test
    fun `sessions route includes global workspace suffix`() {
        assertEquals(
            "Sessions · Global",
            getTitleForRoute("sessions", labels, workspaceKey = WorkspaceKey.Global),
        )
    }

    @Test
    fun `filtered sessions route includes directory workspace suffix`() {
        assertEquals(
            "Sessions · project",
            getTitleForRoute("sessions?project=1", labels, workspaceKey = WorkspaceKey.Directory("/repo/project")),
        )
    }

    @Test
    fun `chat route uses session title with directory workspace suffix`() {
        assertEquals(
            "Investigate bug · project",
            getTitleForRoute(
                route = "chat/session-1",
                labels = labels,
                sessionTitle = "Investigate bug",
                workspaceKey = WorkspaceKey.Directory("/repo/project"),
            ),
        )
    }

    @Test
    fun `chat route falls back to localized chat label`() {
        assertEquals(
            "Chat · Session",
            getTitleForRoute(
                route = "chat/session-1",
                labels = labels,
                workspaceKey = WorkspaceKey.SessionScoped(SessionId("session-1")),
            ),
        )
    }

    @Test
    fun `files route in global workspace uses compact global label`() {
        assertEquals(
            "Global",
            getTitleForRoute("files", labels, workspaceKey = WorkspaceKey.Global),
        )
    }

    @Test
    fun `files path route uses directory basename`() {
        assertEquals(
            "project",
            getTitleForRoute("files/src/Main.kt", labels, workspaceKey = WorkspaceKey.Directory("/repo/project/")),
        )
    }

    @Test
    fun `settings and nested settings routes use settings label`() {
        assertEquals("Settings", getTitleForRoute("settings", labels))
        assertEquals("Settings", getTitleForRoute("settings/about", labels))
    }

    @Test
    fun `projects route uses projects label`() {
        assertEquals("Projects", getTitleForRoute("projects", labels))
    }

    @Test
    fun `unknown and null routes use fallback label`() {
        assertEquals("Tab", getTitleForRoute("diff/1", labels))
        assertEquals("Tab", getTitleForRoute(null, labels))
    }
}
