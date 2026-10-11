package dev.blazelight.p4oc.domain.workspace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerPathTest {
    @Test
    fun `posix paths collapse repeated and trailing separators`() {
        val normalized = valid("/home/dev//projects/app/")
        val root = valid("/")
        val doubledRoot = valid("//x")

        assertEquals("/home/dev/projects/app", normalized.value)
        assertEquals("/", root.value)
        assertEquals("/", root.root)
        assertEquals(emptyList<String>(), root.components)
        assertEquals("/x", doubledRoot.value)
    }

    @Test
    fun `windows drive paths normalize and recognize drive roots`() {
        val normalized = valid("c:/a//b\\")
        val driveOnly = valid("C:")
        val driveRoot = valid("C:\\")

        assertEquals("C:\\a\\b", normalized.value)
        assertEquals("C:\\", driveOnly.root)
        assertEquals("C:\\", driveOnly.value)
        assertEquals("C:\\", driveRoot.value)
        assertEquals(emptyList<String>(), driveRoot.components)
    }

    @Test
    fun `unc paths retain the share as their root`() {
        val unc = valid("\\\\host\\share\\x")
        val incomplete = ServerPath.parseUserInput("\\\\host")

        assertEquals(PathStyle.Windows, unc.style)
        assertEquals("\\\\host\\share", unc.root)
        assertEquals(listOf("x"), unc.components)
        assertEquals("\\\\host\\share\\x", unc.value)
        assertEquals(ServerPathParse.Invalid(PathRejection.NotAbsolute), incomplete)
    }

    @Test
    fun `windows equality is case insensitive while posix equality is case sensitive`() {
        val windowsUpper = valid("C:\\Projects\\App")
        val windowsLower = valid("c:/projects/app")
        val posixUpper = valid("/Projects/App")
        val posixLower = valid("/projects/app")

        assertEquals(windowsUpper.compareKey, windowsLower.compareKey)
        assertEquals(windowsUpper, windowsLower)
        assertNotEquals(posixUpper.compareKey, posixLower.compareKey)
        assertNotEquals(posixUpper, posixLower)
    }

    @Test
    fun `user input trims while server values are not trimmed`() {
        val parsed = ServerPath.parseUserInput("  /a/b  ")

        assertEquals(ServerPathParse.Valid(valid("/a/b")), parsed)
        assertNull(ServerPath.parseOrNull(" /a"))
    }

    @Test
    fun `user input explains every rejected path class`() {
        val rejected = listOf(
            "   " to PathRejection.Blank,
            "relative/x" to PathRejection.NotAbsolute,
            "C:foo" to PathRejection.NotAbsolute,
            "~/x" to PathRejection.HomeShorthand,
            "file:///a" to PathRejection.FileUri,
            "/a\nb" to PathRejection.ControlCharacters,
            "/a/../b" to PathRejection.DotSegments,
            "/a/./b" to PathRejection.DotSegments,
        )

        rejected.forEach { (input, reason) ->
            assertEquals(input, ServerPathParse.Invalid(reason), ServerPath.parseUserInput(input))
            assertNull(input, ServerPath.parseOrNull(input))
        }
    }

    @Test
    fun `control characters at the edges are rejected before trimming`() {
        assertEquals(
            ServerPathParse.Invalid(PathRejection.ControlCharacters),
            ServerPath.parseUserInput("\n/a"),
        )
    }

    @Test
    fun `containment uses components and requires matching style and root`() {
        val parent = valid("/a/b")

        assertTrue(parent.contains(valid("/a/b")))
        assertTrue(parent.contains(valid("/a/b/c")))
        assertFalse(parent.contains(valid("/a/bc")))
        assertFalse(parent.contains(valid("C:\\a\\b")))
        assertFalse(valid("C:\\a").contains(valid("D:\\a\\b")))
    }

    @Test
    fun `relative components and parent preserve the descendant spelling`() {
        val parent = valid("/a/b")
        val descendant = valid("/a/b/Repo/Sub")

        assertEquals(listOf("Repo", "Sub"), parent.relativeComponents(descendant))
        assertEquals("/a", valid("/a/b").parent()?.value)
        assertNull(valid("/").parent())
    }

    @Test
    fun `child accepts one component and rejects invalid names`() {
        val parent = valid("C:\\a")

        assertEquals("C:\\a\\folder", parent.child("folder").value)
        listOf("..", "a/b", "").forEach { name ->
            assertThrows(IllegalArgumentException::class.java) { parent.child(name) }
        }
    }

    @Test
    fun `last component matching follows path case rules`() {
        assertTrue(valid("C:\\a\\Folder").lastComponentMatches("folder"))
        assertFalse(valid("/a/Folder").lastComponentMatches("folder"))
        assertFalse(valid("/").lastComponentMatches("/"))
    }

    private fun valid(input: String): ServerPath =
        (ServerPath.parseUserInput(input) as ServerPathParse.Valid).path
}
