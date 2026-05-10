package dev.blazelight.p4oc.ui.workspace

import dev.blazelight.p4oc.core.log.AppLog
import dev.blazelight.p4oc.data.files.FileRepository
import dev.blazelight.p4oc.data.files.FileRepositoryFactory
import dev.blazelight.p4oc.data.session.SessionRepositoryImpl
import dev.blazelight.p4oc.data.session.SessionRepositoryProvider
import dev.blazelight.p4oc.data.workspace.WorkspaceClient
import dev.blazelight.p4oc.domain.server.ServerGeneration
import dev.blazelight.p4oc.domain.workspace.Workspace
import dev.blazelight.p4oc.ui.screens.files.upload.UploadCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class WorkspaceRepositoryOwner(
    val tabId: String,
    val workspace: Workspace,
    val generation: ServerGeneration,
    private val sessionRepositoryProvider: SessionRepositoryProvider,
) {
    private val repositoryLease = sessionRepositoryProvider.acquire(workspace, generation)
    val workspaceClient: WorkspaceClient = repositoryLease.workspaceClient
    val sessionRepository: SessionRepositoryImpl = repositoryLease.repository
    val fileRepository: FileRepository = FileRepositoryFactory.create(workspaceClient)
    private val uploadScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val uploadCoordinator = UploadCoordinator(
        scope = uploadScope,
        repositoryFactory = { fileRepository },
    )

    val identityHash: Int = System.identityHashCode(this)
    private var closed = false

    init {
        AppLog.i(TAG, logPrefix("init"))
    }

    fun touch(destinationRoute: String?) {
        AppLog.d(TAG) { "${logPrefix("touch")} destination=$destinationRoute" }
    }

    fun close() {
        if (closed) return
        closed = true
        uploadCoordinator.cancel()
        uploadScope.cancel()
        sessionRepositoryProvider.release(workspace, generation)
        AppLog.i(TAG, logPrefix("close"))
    }

    private fun logPrefix(event: String): String =
        "WorkspaceRepositoryOwner.$event tabId=$tabId workspaceKey=${workspace.key} server=${workspace.server.endpointKey} generation=${generation.value} identity=$identityHash"

    private companion object {
        const val TAG = "WorkspaceRepositoryOwner"
    }
}
