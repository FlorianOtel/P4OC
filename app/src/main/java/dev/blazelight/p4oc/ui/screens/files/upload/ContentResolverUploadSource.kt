package dev.blazelight.p4oc.ui.screens.files.upload

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import dev.blazelight.p4oc.core.mime.FilenameMimeType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream

/**
 * [UploadSource] backed by Android's [ContentResolver]. Always reads on
 * [Dispatchers.IO]. Determines MIME via [ContentResolver.getType] with an
 * extension-based fallback through [FilenameMimeType].
 */
class ContentResolverUploadSource(
    private val resolver: ContentResolver,
) : UploadSource {

    override suspend fun probe(sourceId: String): UploadSourceMetadata = withContext(Dispatchers.IO) {
        val uri = Uri.parse(sourceId)
        var name: String? = null
        var size: Long = -1L
        runCatching {
            resolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null,
                null,
                null
            )?.use { c ->
                if (c.moveToFirst()) {
                    val nameIdx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = c.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx >= 0 && !c.isNull(nameIdx)) name = c.getString(nameIdx)
                    if (sizeIdx >= 0 && !c.isNull(sizeIdx)) size = c.getLong(sizeIdx)
                }
            }
        }
        val fallbackName = name ?: uri.lastPathSegment?.substringAfterLast('/')
        val mime = resolver.getType(uri) ?: FilenameMimeType.resolve(fallbackName)
        UploadSourceMetadata(displayName = fallbackName, sizeBytes = size, mimeType = mime)
    }

    override suspend fun openStream(sourceId: String): InputStream = withContext(Dispatchers.IO) {
        val uri = Uri.parse(sourceId)
        resolver.openInputStream(uri) ?: throw IOException("Unable to open $sourceId")
    }
}
