package dev.blazelight.p4oc.data.files

import dev.blazelight.p4oc.data.files.ofish.CachedOfishCapabilities
import dev.blazelight.p4oc.data.files.ofish.OfishCapabilityProbe
import dev.blazelight.p4oc.data.files.ofish.OfishFileRepository
import dev.blazelight.p4oc.data.files.ofish.OfishMutationClient
import dev.blazelight.p4oc.data.files.ofish.OfishSessionFactory
import dev.blazelight.p4oc.data.files.ofish.OfishUploadChunkProbe
import dev.blazelight.p4oc.data.files.ofish.CachedOfishUploadChunkBytes
import dev.blazelight.p4oc.data.files.ofish.WorkspaceClientOfishAdapter
import dev.blazelight.p4oc.data.workspace.WorkspaceClient

internal object FileRepositoryFactory {
    fun create(workspaceClient: WorkspaceClient): FileRepository {
        val delegate = WorkspaceFileRepository(workspaceClient)
        val ofishClient = WorkspaceClientOfishAdapter(workspaceClient)
        val sessionFactory = OfishSessionFactory(ofishClient)
        val capabilityProbe = OfishCapabilityProbe(ofishClient, sessionFactory)
        val capabilityCache = CachedOfishCapabilities(capabilityProbe)
        val chunkProbe = OfishUploadChunkProbe(ofishClient, sessionFactory)
        val chunkCache = CachedOfishUploadChunkBytes(chunkProbe)
        val mutationClient = OfishMutationClient(
            client = ofishClient,
            sessionFactory = sessionFactory,
            capabilityCache = capabilityCache,
            uploadChunkBytes = chunkCache,
        )
        return OfishFileRepository(
            delegate = delegate,
            mutationClient = mutationClient,
        )
    }
}
