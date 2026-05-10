package dev.blazelight.p4oc.data.files

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FilePathValidatorTest {
    @Test
    fun `read list root variants normalize to empty`() {
        listOf("", " ", ".", "/", "///").forEach { path ->
            assertEquals("", FilePathValidator.normalizeForReadOrList(path).getOrThrow())
        }
    }

    @Test
    fun `read list normalizes harmless relative paths`() {
        assertEquals("src/Main.kt", FilePathValidator.normalizeForReadOrList(" src//./Main.kt ").getOrThrow())
    }

    @Test
    fun `read list rejects unsafe paths`() {
        listOf(
            "/etc/passwd",
            "//server/path",
            "..",
            "../secret",
            "src/..",
            "src/../secret",
            "src\\..\\secret",
            "C:/Users/file.txt",
            "file:/tmp/file.txt",
            "http://example.test/file.txt",
            "~/secret",
            "safe\u0000path",
        ).forEach { path ->
            assertTrue("Expected path to be rejected: $path", FilePathValidator.normalizeForReadOrList(path).isFailure)
        }
    }

    @Test
    fun `mutation rejects root variants`() {
        listOf("", " ", ".", "/", "///").forEach { path ->
            assertTrue(
                "Expected mutation root to be rejected: $path",
                FilePathValidator.normalizeForMutation(path).isFailure
            )
        }
    }

    @Test
    fun `mutation accepts safe relative paths`() {
        assertEquals("src/Main.kt", FilePathValidator.normalizeForMutation("src//./Main.kt").getOrThrow())
    }
}
