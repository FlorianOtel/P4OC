package dev.blazelight.p4oc.domain.workspace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class WorkspacePathTest {
    @Test
    fun `relative workspace path exposes relative value`() {
        val path = WorkspacePath.Relative(RelativePath("app/build.gradle.kts"))

        assertEquals("app/build.gradle.kts", path.value)
    }

    @Test
    fun `relative workspace path rejects invalid server values`() {
        listOf("", "   ", "/absolute/path").forEach { value ->
            assertThrows(IllegalArgumentException::class.java) {
                RelativePath(value)
            }
        }
    }

    @Test
    fun `absolute workspace path requires leading slash`() {
        val path = WorkspacePath.Absolute("/home/user/project")

        assertEquals("/home/user/project", path.value)
    }

    @Test
    fun `absolute workspace path rejects file scheme`() {
        assertThrows(IllegalArgumentException::class.java) {
            WorkspacePath.Absolute("file:///home/user/project")
        }
    }
}
