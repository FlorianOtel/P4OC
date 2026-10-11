package dev.blazelight.p4oc.domain.workspace

import dev.blazelight.p4oc.domain.server.WorkspaceKey
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkspaceAttributionTest {
    @Test
    fun `deepest nested root wins while sibling paths use the parent root`() {
        val roots = listOf(path("/w"), path("/w/repo-a"))

        assertEquals(
            WorkspaceKey.Directory("/w/repo-a"),
            WorkspaceAttribution.groupKeyFor("/w/repo-a/sub", roots),
        )
        assertEquals(
            WorkspaceKey.Directory("/w"),
            WorkspaceAttribution.groupKeyFor("/w/notes", roots),
        )
    }

    @Test
    fun `outside paths keep their exact directory key`() {
        val original = "/elsewhere/tool/"

        assertEquals(
            WorkspaceKey.Directory(original),
            WorkspaceAttribution.groupKeyFor(original, listOf(path("/w"))),
        )
    }

    @Test
    fun `blank directory maps to global`() {
        assertEquals(
            WorkspaceKey.Global,
            WorkspaceAttribution.groupKeyFor("  ", listOf(path("/w"))),
        )
    }

    @Test
    fun `unparseable and rootless directories keep their exact key`() {
        val unparseable = "relative/path"
        val absoluteWithoutRoots = "/outside/project/"

        assertEquals(
            WorkspaceKey.Directory(unparseable),
            WorkspaceAttribution.groupKeyFor(unparseable, listOf(path("/w"))),
        )
        assertEquals(
            WorkspaceKey.Directory(absoluteWithoutRoots),
            WorkspaceAttribution.groupKeyFor(absoluteWithoutRoots, emptyList()),
        )
    }

    @Test
    fun `windows containment ignores case and separator style`() {
        assertEquals(
            WorkspaceKey.Directory("C:\\Projects\\App"),
            WorkspaceAttribution.groupKeyFor(
                "c:/projects/app/src",
                listOf(path("C:\\Projects\\App")),
            ),
        )
    }

    @Test
    fun `all five multi repo probe directories map to their user root`() {
        val root = "/workspace/multirepo-probe"
        val probeDirectories = listOf(
            root,
            "$root/repo-a",
            "$root/repo-a/sub/deep",
            "$root/repo-b",
            "$root/notes",
        )

        val groups = probeDirectories.map { directory ->
            WorkspaceAttribution.groupKeyFor(directory, listOf(path(root)))
        }

        assertEquals(List(probeDirectories.size) { WorkspaceKey.Directory(root) }, groups)
    }

    private fun path(input: String): ServerPath =
        (ServerPath.parseUserInput(input) as ServerPathParse.Valid).path
}
