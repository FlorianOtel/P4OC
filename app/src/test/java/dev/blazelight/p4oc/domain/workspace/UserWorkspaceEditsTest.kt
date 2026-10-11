package dev.blazelight.p4oc.domain.workspace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UserWorkspaceEditsTest {
    @Test
    fun `alias sanitization trims strips controls caps length and drops blank values`() {
        val controls = UserWorkspaceEdits.sanitizeAlias(" \tName\nWith\u0000Control \t ")
        val longAlias = "x".repeat(UserWorkspaceLimits.MAX_ALIAS_LENGTH + 10)

        assertEquals("NameWithControl", controls)
        assertEquals("x".repeat(UserWorkspaceLimits.MAX_ALIAS_LENGTH), UserWorkspaceEdits.sanitizeAlias(longAlias))
        assertNull(UserWorkspaceEdits.sanitizeAlias(" \t\n "))
        assertNull(UserWorkspaceEdits.sanitizeAlias(null))
    }

    @Test
    fun `adding a new root updates state and preserves its timestamp`() {
        val root = path("/workspace/app")
        val edit = UserWorkspaceEdits.addRoot(UserWorkspaceState.Empty, ENDPOINT_A, root, ADDED_AT)

        val updated = (edit as UserWorkspaceEdit.Updated).state
        assertEquals(listOf(UserWorkspaceRoot(root, ADDED_AT)), updated.forServer(ENDPOINT_A).roots)
    }

    @Test
    fun `adding a compare key duplicate returns duplicate`() {
        val existing = path("C:\\Workspace\\App")
        val equivalent = path("c:/workspace/app/")
        val state = stateWithRoots(ENDPOINT_A, listOf(UserWorkspaceRoot(existing, ADDED_AT)))

        assertEquals(
            UserWorkspaceEdit.Duplicate,
            UserWorkspaceEdits.addRoot(state, ENDPOINT_A, equivalent, ADDED_AT + 1),
        )
    }

    @Test
    fun `adding a root at the limit is rejected`() {
        val roots = (0 until UserWorkspaceLimits.MAX_ROOTS_PER_SERVER).map { index ->
            UserWorkspaceRoot(path("/workspace/root-$index"), index.toLong())
        }
        val state = stateWithRoots(ENDPOINT_A, roots)

        assertEquals(
            UserWorkspaceEdit.LimitReached,
            UserWorkspaceEdits.addRoot(state, ENDPOINT_A, path("/workspace/new"), ADDED_AT),
        )
    }

    @Test
    fun `removing a root keeps its custom alias`() {
        val root = path("/workspace/app")
        val alias = StoredWorkspaceAlias(root, "App")
        val state = UserWorkspaceState(
            mapOf(
                ENDPOINT_A to ServerUserWorkspaces(
                    roots = listOf(UserWorkspaceRoot(root, ADDED_AT)),
                    aliases = mapOf(root.compareKey to alias),
                ),
            ),
        )

        val updated = UserWorkspaceEdits.removeRoot(state, ENDPOINT_A, root)

        assertTrue(updated.forServer(ENDPOINT_A).roots.isEmpty())
        assertEquals(alias, updated.forServer(ENDPOINT_A).aliases[root.compareKey])
    }

    @Test
    fun `null blank and generated aliases reset the custom name`() {
        val root = path("/workspace/app")
        val starting = stateWithAlias(root, "Custom")

        listOf<String?>(null, " \t ", "Automatic").forEach { requested ->
            val result = UserWorkspaceEdits.setAlias(starting, ENDPOINT_A, root, requested, "Automatic")
            val updated = (result as UserWorkspaceEdit.Updated).state
            assertFalse(updated.forServer(ENDPOINT_A).aliases.containsKey(root.compareKey))
        }
    }

    @Test
    fun `alias limit rejects new keys but permits updating an existing key`() {
        val aliases = (0 until UserWorkspaceLimits.MAX_ALIASES_PER_SERVER).associate { index ->
            val aliasPath = path("/aliases/$index")
            aliasPath.compareKey to StoredWorkspaceAlias(aliasPath, "alias-$index")
        }
        val state = UserWorkspaceState(mapOf(ENDPOINT_A to ServerUserWorkspaces(aliases = aliases)))
        val existingPath = path("/aliases/0")
        val newPath = path("/aliases/new")

        assertEquals(
            UserWorkspaceEdit.LimitReached,
            UserWorkspaceEdits.setAlias(state, ENDPOINT_A, newPath, "new", "generated"),
        )
        val result = UserWorkspaceEdits.setAlias(state, ENDPOINT_A, existingPath, "renamed", "generated")
        val updated = (result as UserWorkspaceEdit.Updated).state
        assertEquals("renamed", updated.forServer(ENDPOINT_A).aliasFor(existingPath))
        assertEquals(UserWorkspaceLimits.MAX_ALIASES_PER_SERVER, updated.forServer(ENDPOINT_A).aliases.size)
    }

    @Test
    fun `pruning a server leaves other server state alone`() {
        val retainedRoot = path("/retained")
        val removedRoot = path("/removed")
        val state = UserWorkspaceState(
            mapOf(
                ENDPOINT_A to ServerUserWorkspaces(roots = listOf(UserWorkspaceRoot(removedRoot, ADDED_AT))),
                ENDPOINT_B to ServerUserWorkspaces(roots = listOf(UserWorkspaceRoot(retainedRoot, ADDED_AT))),
            ),
        )

        val pruned = UserWorkspaceEdits.pruneServer(state, ENDPOINT_A)

        assertEquals(mapOf(ENDPOINT_B to state.forServer(ENDPOINT_B)), pruned.byEndpoint)
    }

    @Test
    fun `moving workspaces removes the source and gives conflicting target entries precedence`() {
        val shared = path("/shared")
        val targetOnly = path("/target-only")
        val sourceOnly = path("/source-only")
        val sourceAlias = StoredWorkspaceAlias(shared, "Source name")
        val targetAlias = StoredWorkspaceAlias(shared, "Target name")
        val source = ServerUserWorkspaces(
            roots = listOf(
                UserWorkspaceRoot(shared, ADDED_AT),
                UserWorkspaceRoot(sourceOnly, ADDED_AT),
            ),
            aliases = mapOf(shared.compareKey to sourceAlias),
        )
        val target = ServerUserWorkspaces(
            roots = listOf(
                UserWorkspaceRoot(shared, ADDED_AT + 1),
                UserWorkspaceRoot(targetOnly, ADDED_AT),
            ),
            aliases = mapOf(shared.compareKey to targetAlias),
        )
        val state = UserWorkspaceState(mapOf(ENDPOINT_A to source, ENDPOINT_B to target))

        val moved = UserWorkspaceEdits.moveServer(state, ENDPOINT_A, ENDPOINT_B)

        assertFalse(moved.byEndpoint.containsKey(ENDPOINT_A))
        assertEquals(
            listOf(
                UserWorkspaceRoot(shared, ADDED_AT + 1),
                UserWorkspaceRoot(targetOnly, ADDED_AT),
                UserWorkspaceRoot(sourceOnly, ADDED_AT),
            ),
            moved.forServer(ENDPOINT_B).roots,
        )
        assertEquals(targetAlias, moved.forServer(ENDPOINT_B).aliases[shared.compareKey])
    }

    @Test
    fun `moving to the same endpoint or from a missing source is a no op`() {
        val root = path("/workspace/app")
        val state = stateWithRoots(ENDPOINT_A, listOf(UserWorkspaceRoot(root, ADDED_AT)))

        assertEquals(state, UserWorkspaceEdits.moveServer(state, ENDPOINT_A, ENDPOINT_A))
        assertEquals(state, UserWorkspaceEdits.moveServer(state, ENDPOINT_B, ENDPOINT_A))
    }

    @Test
    fun `removing the last entry removes its server from the state`() {
        val root = path("/workspace/app")
        val state = stateWithRoots(ENDPOINT_A, listOf(UserWorkspaceRoot(root, ADDED_AT)))

        val updated = UserWorkspaceEdits.removeRoot(state, ENDPOINT_A, root)

        assertTrue(updated.byEndpoint.isEmpty())
        assertTrue(updated.forServer(ENDPOINT_A).isEmpty())
    }

    private fun stateWithRoots(endpoint: String, roots: List<UserWorkspaceRoot>) =
        UserWorkspaceState(mapOf(endpoint to ServerUserWorkspaces(roots = roots)))

    private fun stateWithAlias(path: ServerPath, alias: String) =
        UserWorkspaceState(
            mapOf(
                ENDPOINT_A to ServerUserWorkspaces(
                    aliases = mapOf(path.compareKey to StoredWorkspaceAlias(path, alias)),
                ),
            ),
        )

    private fun path(input: String): ServerPath =
        (ServerPath.parseUserInput(input) as ServerPathParse.Valid).path

    private companion object {
        const val ADDED_AT = 1_760_000_000_000L
        const val ENDPOINT_A = "https://a.example:4096"
        const val ENDPOINT_B = "https://b.example:4096"
    }
}
