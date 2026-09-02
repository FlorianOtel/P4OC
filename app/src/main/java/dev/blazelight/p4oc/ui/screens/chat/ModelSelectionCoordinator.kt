package dev.blazelight.p4oc.ui.screens.chat

import dev.blazelight.p4oc.data.remote.dto.ModelInput
import dev.blazelight.p4oc.domain.server.ServerGeneration
import dev.blazelight.p4oc.domain.workspace.Workspace
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * A successful active-model write scoped to the exact workspace and server generation
 * that produced it. Consumers must only apply events whose [workspace] and [generation]
 * match their own, so a global coordinator singleton can never cross-update tabs across
 * workspaces or stale server generations.
 */
data class ScopedModelSelectionChange(
    val workspace: Workspace,
    val generation: ServerGeneration,
    val model: ModelInput,
)

/**
 * Shares successful active-model writes with live chat model managers.
 * Because this is a Koin singleton shared process-wide, events carry the originating
 * workspace and server generation and consumers filter on exact matches.
 */
class ModelSelectionCoordinator {
    private val _activeModelChanges =
        MutableSharedFlow<ScopedModelSelectionChange>(extraBufferCapacity = 1)
    val activeModelChanges: SharedFlow<ScopedModelSelectionChange> = _activeModelChanges.asSharedFlow()

    fun publishActiveModel(workspace: Workspace, generation: ServerGeneration, model: ModelInput) {
        _activeModelChanges.tryEmit(ScopedModelSelectionChange(workspace, generation, model))
    }
}
