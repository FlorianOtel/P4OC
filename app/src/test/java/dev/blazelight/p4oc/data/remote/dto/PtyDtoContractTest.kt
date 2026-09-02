package dev.blazelight.p4oc.data.remote.dto

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PtyDtoContractTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `PTY decodes running and exited upstream shapes plus nullable legacy pid`() {
        val running = json.decodeFromString<PtyDto>(
            """
                {"id":"p1","title":"shell","command":"bash","args":[],"cwd":"/repo","status":"running","pid":42}
            """.trimIndent(),
        )
        val exited = json.decodeFromString<PtyDto>(
            """
                {"id":"p2","title":"done","command":"bash","args":[],"cwd":"/repo","status":"exited",
                "pid":null,"exitCode":7}
            """.trimIndent(),
        )

        assertEquals(42, running.pid)
        assertNull(running.exitCode)
        assertNull(exited.pid)
        assertEquals(7, exited.exitCode)
    }
}
