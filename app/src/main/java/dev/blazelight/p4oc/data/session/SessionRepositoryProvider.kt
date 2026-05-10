package dev.blazelight.p4oc.data.session

import dev.blazelight.p4oc.core.network.ConnectionManager
import dev.blazelight.p4oc.data.remote.mapper.MessageMapper
import dev.blazelight.p4oc.data.server.ActiveServerApiProvider
import dev.blazelight.p4oc.data.workspace.WorkspaceClient
import dev.blazelight.p4oc.domain.server.ServerGeneration
import dev.blazelight.p4oc.domain.server.WorkspaceKey
import dev.blazelight.p4oc.domain.workspace.Workspace
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SessionRepositoryProvider(
    private val activeServerApiProvider: ActiveServerApiProvider,
    private val messageMapper: MessageMapper,
    private val connectionManager: ConnectionManager,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    data class Lease(
        val workspaceClient: WorkspaceClient,
        val repository: SessionRepositoryImpl,
    )

    private data class Key(
        val serverKey: String,
        val generation: Long,
        val workspaceKey: String,
    )

    private data class Entry(
        val workspaceClient: WorkspaceClient,
        val repository: SessionRepositoryImpl,
        val eventJob: Job,
        var refCount: Int,
    )

    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val entries = mutableMapOf<Key, Entry>()

    fun acquire(workspace: Workspace, generation: ServerGeneration): Lease = synchronized(this) {
        val key = workspace.toProviderKey(generation)
        val entry = entries.getOrPut(key) {
            val workspaceClient = WorkspaceClient(workspace, generation, activeServerApiProvider)
            val repository = SessionRepositoryImpl(workspaceClient, messageMapper)
            Entry(
                workspaceClient = workspaceClient,
                repository = repository,
                eventJob = collectWorkspaceEvents(workspace, generation, repository),
                refCount = 0,
            )
        }
        entry.refCount += 1
        Lease(entry.workspaceClient, entry.repository)
    }

    fun release(workspace: Workspace, generation: ServerGeneration) {
        val repositoryToClose = synchronized(this) {
            val key = workspace.toProviderKey(generation)
            val entry = entries[key] ?: return
            entry.refCount -= 1
            if (entry.refCount > 0) return
            entries.remove(key)
            entry
        }
        repositoryToClose.eventJob.cancel()
        repositoryToClose.repository.close()
    }

    private fun collectWorkspaceEvents(
        workspace: Workspace,
        generation: ServerGeneration,
        repository: SessionRepositoryImpl,
    ): Job = scope.launch {
        connectionManager.scopedEvents.collect { scopedEvent ->
            if (scopedEvent.serverRef == workspace.server &&
                scopedEvent.generation == generation &&
                scopedEvent.workspaceKey == workspace.key
            ) {
                repository.acceptEvent(scopedEvent.event)
            }
        }
    }

    private fun Workspace.toProviderKey(generation: ServerGeneration): Key = Key(
        serverKey = server.endpointKey,
        generation = generation.value,
        workspaceKey = key.stableKey(),
    )

    private fun WorkspaceKey.stableKey(): String = when (this) {
        WorkspaceKey.Global -> "global"
        is WorkspaceKey.Directory -> "directory:$value"
        is WorkspaceKey.SessionScoped -> "session:${sessionId.value}"
    }
}
