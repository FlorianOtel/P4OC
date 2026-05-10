package dev.blazelight.p4oc.ui.screens.files.upload

/**
 * Abstraction over reading an upload payload from a SAF Uri. The Android
 * implementation lives in [ContentResolverUploadSource]; tests can stub this
 * with in-memory bytes to avoid Robolectric.
 */
interface UploadSource {
    /** Return lightweight metadata for the source (name, size, mime). */
    suspend fun probe(sourceId: String): UploadSourceMetadata

    /** Open a fresh stream for the payload. Callers own closing it. */
    suspend fun openStream(sourceId: String): java.io.InputStream
}

data class UploadSourceMetadata(
    val displayName: String?,
    val sizeBytes: Long,
    val mimeType: String?,
)
