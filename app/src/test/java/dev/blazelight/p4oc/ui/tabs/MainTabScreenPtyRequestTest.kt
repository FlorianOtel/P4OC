package dev.blazelight.p4oc.ui.tabs

import dev.blazelight.p4oc.domain.server.WorkspaceKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MainTabScreenPtyRequestTest {
    @Test
    fun `global workspace terminal request omits client guessed process context`() {
        val request = createPtyRequestForWorkspace(WorkspaceKey.Global)

        assertNull(request.command)
        assertEquals(emptyList<String>(), request.args)
        assertNull(request.cwd)
        assertNull(request.title)
    }

    @Test
    fun `directory workspace terminal request uses directory cwd and basename title`() {
        val request = createPtyRequestForWorkspace(WorkspaceKey.Directory("/repo/project"))

        assertEquals("/repo/project", request.cwd)
        assertEquals("project", request.title)
        assertEquals(emptyList<String>(), request.args)
        assertNull(request.command)
        assertNotEquals(".", request.cwd)
    }
}
