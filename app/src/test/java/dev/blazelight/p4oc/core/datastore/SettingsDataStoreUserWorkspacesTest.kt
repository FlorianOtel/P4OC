package dev.blazelight.p4oc.core.datastore

import dev.blazelight.p4oc.domain.workspace.ServerPath
import dev.blazelight.p4oc.domain.workspace.ServerPathParse
import dev.blazelight.p4oc.domain.workspace.ServerUserWorkspaces
import dev.blazelight.p4oc.domain.workspace.StoredWorkspaceAlias
import dev.blazelight.p4oc.domain.workspace.UserWorkspaceLimits
import dev.blazelight.p4oc.domain.workspace.UserWorkspaceRoot
import dev.blazelight.p4oc.domain.workspace.UserWorkspaceState
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsDataStoreUserWorkspacesTest {
    @Test
    fun `workspace persistence round trips roots timestamps and aliases`() {
        val root = path("/workspace/app")
        val aliasPath = path("C:\\Projects\\frontend")
        val state = UserWorkspaceState(
            mapOf(
                ENDPOINT_A to ServerUserWorkspaces(
                    roots = listOf(UserWorkspaceRoot(root, ADDED_AT)),
                    aliases = mapOf(aliasPath.compareKey to StoredWorkspaceAlias(aliasPath, "Shop UI")),
                ),
            ),
        )

        val stored = encodeUserWorkspaces(state)

        assertEquals(state, decodeUserWorkspaces(stored))
    }

    @Test
    fun `null and blank workspace values decode to empty state`() {
        assertEquals(UserWorkspaceState.Empty, decodeUserWorkspaces(null))
        assertEquals(UserWorkspaceState.Empty, decodeUserWorkspaces(""))
        assertEquals(UserWorkspaceState.Empty, decodeUserWorkspaces(" \n "))
    }

    @Test
    fun `malformed workspace json decodes to empty state`() {
        assertEquals(UserWorkspaceState.Empty, decodeUserWorkspaces("not-json"))
    }

    @Test
    fun `invalid root entries are dropped without losing valid entries`() {
        val stored = fixture(
            mapOf(
                ENDPOINT_A to PersistedServerWorkspaces(
                    roots = listOf(
                        PersistedWorkspaceRoot("/valid/one", ADDED_AT),
                        PersistedWorkspaceRoot("relative/path", ADDED_AT + 1),
                        PersistedWorkspaceRoot("/valid/two", ADDED_AT + 2),
                    ),
                ),
            ),
        )

        val roots = decodeUserWorkspaces(stored).forServer(ENDPOINT_A).roots

        assertEquals(
            listOf(
                UserWorkspaceRoot(path("/valid/one"), ADDED_AT),
                UserWorkspaceRoot(path("/valid/two"), ADDED_AT + 2),
            ),
            roots,
        )
    }

    @Test
    fun `duplicate compare keys keep the last root and alias entry`() {
        val stored = fixture(
            mapOf(
                ENDPOINT_A to PersistedServerWorkspaces(
                    roots = listOf(
                        PersistedWorkspaceRoot("c:/projects/app", ADDED_AT),
                        PersistedWorkspaceRoot("C:\\PROJECTS\\APP", ADDED_AT + 1),
                    ),
                    aliases = linkedMapOf(
                        "c:/projects/app" to "First name",
                        "C:\\PROJECTS\\APP" to "Last name",
                    ),
                ),
            ),
        )

        val server = decodeUserWorkspaces(stored).forServer(ENDPOINT_A)

        assertEquals(1, server.roots.size)
        assertEquals("C:\\PROJECTS\\APP", server.roots.single().path.value)
        assertEquals(ADDED_AT + 1, server.roots.single().addedAt)
        assertEquals("Last name", server.aliasFor(path("c:/projects/app")))
    }

    @Test
    fun `decoded aliases are trimmed stripped and capped`() {
        val longAlias = "x".repeat(UserWorkspaceLimits.MAX_ALIAS_LENGTH + 10)
        val stored = fixture(
            mapOf(
                ENDPOINT_A to PersistedServerWorkspaces(
                    aliases = mapOf(
                        "/workspace/app" to " \tShop\n UI\u0000 ",
                        "/workspace/blank" to " \n\t ",
                        "/workspace/long" to longAlias,
                    ),
                ),
            ),
        )

        val aliases = decodeUserWorkspaces(stored).forServer(ENDPOINT_A).aliases

        assertEquals("Shop UI", aliases.getValue(path("/workspace/app").compareKey).alias)
        assertEquals(
            "x".repeat(UserWorkspaceLimits.MAX_ALIAS_LENGTH),
            aliases.getValue(path("/workspace/long").compareKey).alias,
        )
        assertEquals(2, aliases.size)
    }

    @Test
    fun `decoding caps roots and aliases at their per server limits`() {
        val roots = (0..UserWorkspaceLimits.MAX_ROOTS_PER_SERVER).map { index ->
            PersistedWorkspaceRoot("/roots/$index", index.toLong())
        }
        val aliases = (0..UserWorkspaceLimits.MAX_ALIASES_PER_SERVER).associate { index ->
            "/aliases/$index" to "alias-$index"
        }
        val stored = fixture(
            mapOf(ENDPOINT_A to PersistedServerWorkspaces(roots = roots, aliases = aliases)),
        )

        val server = decodeUserWorkspaces(stored).forServer(ENDPOINT_A)

        assertEquals(UserWorkspaceLimits.MAX_ROOTS_PER_SERVER, server.roots.size)
        assertEquals(UserWorkspaceLimits.MAX_ALIASES_PER_SERVER, server.aliases.size)
    }

    @Test
    fun `encoding an empty state removes the preference value`() {
        assertNull(encodeUserWorkspaces(UserWorkspaceState.Empty))
    }

    @Test
    fun `pruning removes only the requested endpoint`() {
        val first = path("/first")
        val second = path("/second")
        val state = UserWorkspaceState(
            mapOf(
                ENDPOINT_A to ServerUserWorkspaces(roots = listOf(UserWorkspaceRoot(first, ADDED_AT))),
                ENDPOINT_B to ServerUserWorkspaces(roots = listOf(UserWorkspaceRoot(second, ADDED_AT + 1))),
            ),
        )

        val pruned = decodeUserWorkspaces(pruneUserWorkspaces(encodeUserWorkspaces(state), ENDPOINT_A))

        assertEquals(mapOf(ENDPOINT_B to state.forServer(ENDPOINT_B)), pruned.byEndpoint)
    }

    @Test
    fun `moving endpoint workspaces merges with target precedence`() {
        val shared = path("/shared")
        val sourceOnly = path("/source")
        val targetOnly = path("/target")
        val state = UserWorkspaceState(
            mapOf(
                ENDPOINT_A to ServerUserWorkspaces(
                    roots = listOf(
                        UserWorkspaceRoot(shared, ADDED_AT),
                        UserWorkspaceRoot(sourceOnly, ADDED_AT + 1),
                    ),
                    aliases = mapOf(
                        shared.compareKey to StoredWorkspaceAlias(shared, "Source name"),
                        sourceOnly.compareKey to StoredWorkspaceAlias(sourceOnly, "Source only"),
                    ),
                ),
                ENDPOINT_B to ServerUserWorkspaces(
                    roots = listOf(
                        UserWorkspaceRoot(shared, ADDED_AT + 2),
                        UserWorkspaceRoot(targetOnly, ADDED_AT + 3),
                    ),
                    aliases = mapOf(shared.compareKey to StoredWorkspaceAlias(shared, "Target name")),
                ),
            ),
        )

        val moved = decodeUserWorkspaces(movedUserWorkspaces(encodeUserWorkspaces(state), ENDPOINT_A, ENDPOINT_B))
        val target = moved.forServer(ENDPOINT_B)

        assertTrue(ENDPOINT_A !in moved.byEndpoint)
        assertEquals(ADDED_AT + 2, target.roots.first { it.path == shared }.addedAt)
        assertEquals("Target name", target.aliasFor(shared))
        assertEquals("Source only", target.aliasFor(sourceOnly))
        assertTrue(target.roots.any { it.path == sourceOnly })
    }

    @Test
    fun `blank endpoint keys are dropped while valid servers survive`() {
        val validRoot = PersistedWorkspaceRoot("/workspace/app", ADDED_AT)
        val stored = fixture(
            mapOf(
                "  " to PersistedServerWorkspaces(roots = listOf(PersistedWorkspaceRoot("/ignored", ADDED_AT))),
                ENDPOINT_A to PersistedServerWorkspaces(roots = listOf(validRoot)),
            ),
        )

        val decoded = decodeUserWorkspaces(stored)

        assertEquals(setOf(ENDPOINT_A), decoded.byEndpoint.keys)
        assertEquals(listOf(UserWorkspaceRoot(path("/workspace/app"), ADDED_AT)), decoded.forServer(ENDPOINT_A).roots)
    }

    @Test
    fun `missing workspace preference leaves existing tab state json unchanged`() {
        val tabStateJson = """{"version":1,"tabs":[{"workspace":"/repo"}]}"""
        val existingPreferences = mutableMapOf("tab_state_v1" to tabStateJson)
        val beforeDecode = existingPreferences.toMap()

        assertEquals(UserWorkspaceState.Empty, decodeUserWorkspaces(existingPreferences["user_workspaces_v1"]))
        assertEquals(tabStateJson, existingPreferences["tab_state_v1"])
        assertEquals(beforeDecode, existingPreferences)
    }

    private fun fixture(servers: Map<String, PersistedServerWorkspaces>): String =
        Json.encodeToString(
            PersistedUserWorkspaces.serializer(),
            PersistedUserWorkspaces(servers = servers),
        )

    private fun path(input: String): ServerPath =
        (ServerPath.parseUserInput(input) as ServerPathParse.Valid).path

    private companion object {
        const val ADDED_AT = 1_760_000_000_000L
        const val ENDPOINT_A = "https://a.example:4096"
        const val ENDPOINT_B = "https://b.example:4096"
    }
}
