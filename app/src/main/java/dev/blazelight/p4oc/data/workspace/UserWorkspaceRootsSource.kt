package dev.blazelight.p4oc.data.workspace

import dev.blazelight.p4oc.core.datastore.SettingsDataStore
import dev.blazelight.p4oc.domain.server.ServerRef
import kotlinx.coroutines.flow.first

/**
 * Persisted user workspace roots for one server. Repositories are bound to their own [ServerRef]
 * when acquired; this never reads tab state.
 */
fun interface UserWorkspaceRootsSource {
    suspend fun roots(server: ServerRef): List<String>
}

class SettingsUserWorkspaceRootsSource(
    private val settingsDataStore: SettingsDataStore,
) : UserWorkspaceRootsSource {
    override suspend fun roots(server: ServerRef): List<String> =
        settingsDataStore.userWorkspaces.first().rootsFor(server.endpointKey).map { it.value }
}
