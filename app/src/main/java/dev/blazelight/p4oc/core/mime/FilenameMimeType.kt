package dev.blazelight.p4oc.core.mime

import android.webkit.MimeTypeMap

object FilenameMimeType {
    const val OCTET_STREAM = "application/octet-stream"

    fun resolve(name: String?): String? {
        return resolve(name) { extension ->
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        }
    }

    internal fun resolve(name: String?, lookup: (String) -> String?): String? {
        if (name.isNullOrBlank()) return null
        val extension = name.substringAfterLast('.', missingDelimiterValue = "").lowercase()
        if (extension.isBlank()) return null
        return lookup(extension)
    }

    fun resolveOrOctetStream(name: String?): String = resolve(name) ?: OCTET_STREAM
}
