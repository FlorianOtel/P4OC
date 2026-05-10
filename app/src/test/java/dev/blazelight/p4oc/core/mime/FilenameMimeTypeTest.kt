package dev.blazelight.p4oc.core.mime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FilenameMimeTypeTest {
    private val lookup = mapOf(
        "txt" to "text/plain",
        "png" to "image/png",
        "jpg" to "image/jpeg",
    )::get

    @Test
    fun `resolves common extensions`() {
        assertEquals("text/plain", FilenameMimeType.resolve("notes.txt", lookup))
        assertEquals("image/png", FilenameMimeType.resolve("image.png", lookup))
    }

    @Test
    fun `resolves uppercase extensions`() {
        assertEquals("image/jpeg", FilenameMimeType.resolve("PHOTO.JPG", lookup))
    }

    @Test
    fun `unknown extension returns null`() {
        assertNull(FilenameMimeType.resolve("archive.unknownextension", lookup))
    }

    @Test
    fun `missing extension returns null`() {
        assertNull(FilenameMimeType.resolve("README", lookup))
        assertNull(FilenameMimeType.resolve(null, lookup))
        assertNull(FilenameMimeType.resolve("   ", lookup))
    }

    @Test
    fun `octet stream fallback preserves chat attachment behavior`() {
        assertEquals(FilenameMimeType.OCTET_STREAM, FilenameMimeType.resolveOrOctetStream("README"))
    }
}
