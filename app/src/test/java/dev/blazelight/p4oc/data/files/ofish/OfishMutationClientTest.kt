package dev.blazelight.p4oc.data.files.ofish

import dev.blazelight.p4oc.data.files.FileOperationResult
import dev.blazelight.p4oc.data.files.FileUploadRequest
import dev.blazelight.p4oc.data.files.FileWriteRequest
import dev.blazelight.p4oc.data.remote.dto.MessageInfoDto
import dev.blazelight.p4oc.data.remote.dto.MessageTimeDto
import dev.blazelight.p4oc.data.remote.dto.MessageWrapperDto
import dev.blazelight.p4oc.data.remote.dto.PartDto
import dev.blazelight.p4oc.data.remote.dto.PermissionResponseRequest
import dev.blazelight.p4oc.data.remote.dto.SessionDto
import dev.blazelight.p4oc.data.remote.dto.ShellCommandRequest
import dev.blazelight.p4oc.data.remote.dto.TimeDto
import dev.blazelight.p4oc.domain.server.ServerRef
import dev.blazelight.p4oc.domain.workspace.Workspace
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.Base64

class OfishMutationClientTest {
    private val capabilities = OfishCapabilities(
        hasBase64 = true,
        base64DecodeFlag = "-d",
        hashCommand = HashCommand.SHA256SUM,
        hasMv = true,
        hasMkdir = true,
        hasRm = true,
        hasAwk = true,
        hasMktemp = true,
        hasChmod = true,
        modeCommand = ModeCommand.STAT_GNU,
    )

    @Test
    fun `missing capabilities fail without creating session`() = runTest {
        val client = FakeOfishWorkspaceClient()
        val mutationClient = mutationClient(client, OfishProbeResult.Missing(listOf("base64"), null))

        val result = mutationClient.writeFile(FileWriteRequest("file.txt", "content"))

        assertTrue(result is FileOperationResult.Failed)
        assertEquals(0, client.createdTitles.size)
    }

    @Test
    fun `invalid mutation path fails without creating session`() = runTest {
        val client = FakeOfishWorkspaceClient()
        val mutationClient = mutationClient(client, OfishProbeResult.Available(capabilities))

        val result = mutationClient.uploadFile(uploadRequest("../secret", byteArrayOf(1)))

        assertTrue(result is FileOperationResult.Failed)
        assertEquals(0, client.createdTitles.size)
    }

    @Test
    fun `write conflict maps to FileOperationResult Conflict and deletes session`() = runTest {
        val client = FakeOfishWorkspaceClient(outputs = ArrayDeque(listOf("### 409 conflict actual=abc")))
        val mutationClient = mutationClient(client, OfishProbeResult.Available(capabilities))

        val result = mutationClient.writeFile(FileWriteRequest("file.txt", "content", expectedHash = "old"))

        assertTrue(result is FileOperationResult.Conflict)
        assertEquals("abc", (result as FileOperationResult.Conflict).currentHash)
        assertEquals(1, client.createdTitles.size)
        assertEquals(listOf("session-1"), client.deletedIds)
    }

    @Test
    fun `create directory conflict maps to FileOperationResult Conflict`() = runTest {
        val client = FakeOfishWorkspaceClient(outputs = ArrayDeque(listOf("### 409 conflict")))
        val mutationClient = mutationClient(client, OfishProbeResult.Available(capabilities))

        val result = mutationClient.createDirectory("dir/new")

        assertTrue(result is FileOperationResult.Conflict)
        assertEquals(1, client.createdTitles.size)
    }

    @Test
    fun `rename missing source maps to failed`() = runTest {
        val client = FakeOfishWorkspaceClient(outputs = ArrayDeque(listOf("### 404 missing")))
        val mutationClient = mutationClient(client, OfishProbeResult.Available(capabilities))

        val result = mutationClient.renameFile("old.txt", "new.txt")

        assertTrue(result is FileOperationResult.Failed)
        assertEquals(1, client.createdTitles.size)
    }

    @Test
    fun `hash guarded write missing maps to conflict`() = runTest {
        val client = FakeOfishWorkspaceClient(outputs = ArrayDeque(listOf("### 404 missing")))
        val mutationClient = mutationClient(client, OfishProbeResult.Available(capabilities))

        val result = mutationClient.writeFile(FileWriteRequest("file.txt", "content", expectedHash = "old"))

        assertTrue(result is FileOperationResult.Conflict)
        assertEquals(null, (result as FileOperationResult.Conflict).currentHash)
    }

    @Test
    fun `large write uses bounded upload chunks instead of direct write command`() = runTest {
        val content = "large editor content\n".repeat(4_000)
        val client = FakeOfishWorkspaceClient(
            outputs = ArrayDeque(
                buildList {
                    add("### 200 ok upload=.ofish.upload.tmp")
                    repeat(6) { add("### 200 ok") }
                    add("### 200 ok hash=written")
                },
            ),
        )
        val mutationClient = mutationClient(
            client,
            OfishProbeResult.Available(capabilities),
            uploadChunkBytes = 16 * 1024,
        )

        val result = mutationClient.writeFile(FileWriteRequest("file.txt", content, expectedHash = "old"))

        assertTrue(result is FileOperationResult.Ok)
        assertEquals("written", (result as FileOperationResult.Ok).data.hash)
        val scripts = client.commands.map { it.decodedScript() }
        assertEquals(0, scripts.count { it.contains("#OFISH_WRITE") })
        assertEquals(1, scripts.count { it.contains("#OFISH_UPLOAD_INIT") })
        assertEquals(6, scripts.count { it.contains("#OFISH_UPLOAD_CHUNK") })
        assertEquals(1, scripts.count { it.contains("#OFISH_UPLOAD_FINISH") })
        assertTrue(scripts.first().contains("EXPECTED='old'"))
        assertTrue(scripts.last().contains("EXPECTED='old'"))
        assertEquals(listOf("session-1"), client.deletedIds)
    }

    @Test
    fun `large write finish conflict aborts temporary file`() = runTest {
        val content = "x".repeat(33 * 1024)
        val client = FakeOfishWorkspaceClient(
            outputs = ArrayDeque(
                listOf(
                    "### 200 ok upload=.ofish.upload.tmp",
                    "### 200 ok",
                    "### 409 conflict actual=newer",
                    "### 204 deleted",
                ),
            ),
        )
        val mutationClient = mutationClient(
            client,
            OfishProbeResult.Available(capabilities),
            uploadChunkBytes = 64 * 1024,
        )

        val result = mutationClient.writeFile(FileWriteRequest("file.txt", content, expectedHash = "old"))

        assertTrue(result is FileOperationResult.Conflict)
        assertEquals("newer", (result as FileOperationResult.Conflict).currentHash)
        assertTrue(client.commands.last().decodedScript().contains("#OFISH_UPLOAD_ABORT"))
        assertEquals(listOf("session-1"), client.deletedIds)
    }

    @Test
    fun `upload uses one session for init chunks finish`() = runTest {
        val client = FakeOfishWorkspaceClient(
            outputs = ArrayDeque(
                listOf(
                    "### 200 ok upload=.ofish.upload.tmp",
                    "### 200 ok",
                    "### 200 ok",
                    "### 200 ok hash=abc",
                ),
            ),
        )
        val mutationClient = mutationClient(client, OfishProbeResult.Available(capabilities), uploadChunkBytes = 2)

        val result = mutationClient.uploadFile(uploadRequest("file.bin", byteArrayOf(1, 2, 3, 4)))

        assertTrue(result is FileOperationResult.Ok)
        assertEquals("abc", (result as FileOperationResult.Ok).data.hash)
        assertEquals(1, client.createdTitles.size)
        assertEquals(listOf("session-1"), client.deletedIds)
        assertEquals(4, client.commands.size)
        assertTrue(client.commands.last().contains("(base64 -d 2>/dev/null || base64 -D) | sh"))
    }

    @Test
    fun `upload uses fixed 64 KiB chunks by default`() = runTest {
        val client = FakeOfishWorkspaceClient(
            outputs = ArrayDeque(
                listOf(
                    "### 200 ok upload=.ofish.upload.tmp",
                    "### 200 ok",
                    "### 200 ok",
                    "### 200 ok hash=abc",
                ),
            ),
        )
        val mutationClient = mutationClient(
            client = client,
            probeResult = OfishProbeResult.Available(capabilities),
            uploadChunkBytes = OFISH_DEFAULT_CHUNK_BYTES,
        )
        val bytes = ByteArray(OFISH_DEFAULT_CHUNK_BYTES + 1) { index -> index.toByte() }

        val result = mutationClient.uploadFile(uploadRequest("file.bin", bytes))

        assertTrue(result is FileOperationResult.Ok)
        assertEquals(4, client.commands.size)
        assertEquals(2, client.commands.count { it.decodedScript().contains("#OFISH_UPLOAD_CHUNK") })
    }

    @Test
    fun `upload makes progress when bulk stream reads return zero`() = runTest {
        val client = FakeOfishWorkspaceClient(
            outputs = ArrayDeque(
                listOf(
                    "### 200 ok upload=.ofish.upload.tmp",
                    "### 200 ok",
                    "### 200 ok",
                    "### 200 ok hash=abc",
                ),
            ),
        )
        val mutationClient = mutationClient(client, OfishProbeResult.Available(capabilities), uploadChunkBytes = 2)
        val bytes = byteArrayOf(1, 2, 3)
        val stream = object : InputStream() {
            private var offset = 0

            override fun read(): Int = if (offset < bytes.size) bytes[offset++].toInt() and 0xff else -1

            override fun read(buffer: ByteArray, offset: Int, length: Int): Int = 0
        }
        val request = FileUploadRequest(
            path = "file.bin",
            contentLength = bytes.size.toLong(),
            openStream = { stream },
        )

        val result = mutationClient.uploadFile(request)

        assertTrue(result is FileOperationResult.Ok)
        assertEquals(2, client.commands.count { it.decodedScript().contains("#OFISH_UPLOAD_CHUNK") })
        assertEquals(listOf("session-1"), client.deletedIds)
    }

    @Test
    fun `upload short EOF with declared length aborts without finishing`() = runTest {
        val client = FakeOfishWorkspaceClient(
            outputs = ArrayDeque(
                listOf(
                    "### 200 ok upload=.ofish.upload.tmp",
                    "### 200 ok",
                    "### 204 deleted",
                ),
            ),
        )
        val mutationClient = mutationClient(client, OfishProbeResult.Available(capabilities), uploadChunkBytes = 4)
        val request = FileUploadRequest(
            path = "file.bin",
            contentLength = 4,
            openStream = { ByteArrayInputStream(byteArrayOf(1, 2)) },
        )

        val result = mutationClient.uploadFile(request)

        assertTrue(result is FileOperationResult.Failed)
        assertTrue((result as FileOperationResult.Failed).message.contains("expected 4 bytes, streamed 2 bytes"))
        assertEquals(3, client.commands.size)
        assertEquals(1, client.commands.count { it.decodedScript().contains("#OFISH_UPLOAD_CHUNK") })
        assertEquals(0, client.commands.count { it.decodedScript().contains("#OFISH_UPLOAD_FINISH") })
        assertTrue(client.commands.last().decodedScript().contains("#OFISH_UPLOAD_ABORT"))
    }

    @Test
    fun `upload with unknown content length finishes`() = runTest {
        val client = FakeOfishWorkspaceClient(
            outputs = ArrayDeque(
                listOf(
                    "### 200 ok upload=.ofish.upload.tmp",
                    "### 200 ok",
                    "### 200 ok hash=abc",
                ),
            ),
        )
        val mutationClient = mutationClient(client, OfishProbeResult.Available(capabilities), uploadChunkBytes = 4)
        val request = FileUploadRequest(
            path = "file.bin",
            contentLength = -1,
            openStream = { ByteArrayInputStream(byteArrayOf(1, 2)) },
        )

        val result = mutationClient.uploadFile(request)

        assertTrue(result is FileOperationResult.Ok)
        assertEquals("abc", (result as FileOperationResult.Ok).data.hash)
        assertEquals(1, client.commands.count { it.decodedScript().contains("#OFISH_UPLOAD_FINISH") })
        assertEquals(0, client.commands.count { it.decodedScript().contains("#OFISH_UPLOAD_ABORT") })
    }

    @Test
    fun `upload at source byte ceiling finishes`() = runTest {
        val client = FakeOfishWorkspaceClient(
            outputs = ArrayDeque(
                listOf(
                    "### 200 ok upload=.ofish.upload.tmp",
                    "### 200 ok",
                    "### 200 ok hash=abc",
                ),
            ),
        )
        val mutationClient = mutationClient(
            client = client,
            probeResult = OfishProbeResult.Available(capabilities),
            uploadChunkBytesProvider = FixedUploadChunkBytesProvider(4),
            maxUploadSourceBytes = 4,
        )

        val result = mutationClient.uploadFile(uploadRequest("file.bin", byteArrayOf(1, 2, 3, 4)))

        assertTrue(result is FileOperationResult.Ok)
        assertEquals(1, client.commands.count { it.decodedScript().contains("#OFISH_UPLOAD_FINISH") })
        assertEquals(0, client.commands.count { it.decodedScript().contains("#OFISH_UPLOAD_ABORT") })
    }

    @Test
    fun `known upload above source byte ceiling rejects before mutation or opening stream`() = runTest {
        val client = FakeOfishWorkspaceClient()
        val mutationClient = mutationClient(
            client = client,
            probeResult = OfishProbeResult.Available(capabilities),
            uploadChunkBytesProvider = FixedUploadChunkBytesProvider(4),
            maxUploadSourceBytes = 4,
        )
        var opened = false
        val request = FileUploadRequest(
            path = "file.bin",
            contentLength = 5,
            openStream = {
                opened = true
                ByteArrayInputStream(byteArrayOf(1, 2, 3, 4, 5))
            },
        )

        val result = mutationClient.uploadFile(request)

        assertTrue(result is FileOperationResult.Failed)
        assertEquals(UPLOAD_TOO_LARGE_MESSAGE, (result as FileOperationResult.Failed).message)
        assertFalse(opened)
        assertEquals(0, client.createdTitles.size)
        assertEquals(0, client.commands.size)
    }

    @Test
    fun `unknown upload above source byte ceiling aborts without sending excess chunk`() = runTest {
        val client = FakeOfishWorkspaceClient(
            outputs = ArrayDeque(
                listOf(
                    "### 200 ok upload=.ofish.upload.tmp",
                    "### 200 ok",
                    "### 204 deleted",
                ),
            ),
        )
        val mutationClient = mutationClient(
            client = client,
            probeResult = OfishProbeResult.Available(capabilities),
            uploadChunkBytesProvider = FixedUploadChunkBytesProvider(4),
            maxUploadSourceBytes = 4,
        )
        val request = FileUploadRequest(
            path = "file.bin",
            contentLength = -1,
            openStream = { ByteArrayInputStream(byteArrayOf(1, 2, 3, 4, 5)) },
        )

        val result = mutationClient.uploadFile(request)

        assertTrue(result is FileOperationResult.Failed)
        assertEquals(UPLOAD_TOO_LARGE_MESSAGE, (result as FileOperationResult.Failed).message)
        assertEquals(1, client.commands.count { it.decodedScript().contains("#OFISH_UPLOAD_CHUNK") })
        assertEquals(0, client.commands.count { it.decodedScript().contains("#OFISH_UPLOAD_FINISH") })
        assertTrue(client.commands.last().decodedScript().contains("#OFISH_UPLOAD_ABORT"))
    }

    @Test
    fun `upload callback failure aborts temporary file and deletes session`() = runTest {
        val client = FakeOfishWorkspaceClient(
            outputs = ArrayDeque(
                listOf(
                    "### 200 ok upload=.ofish.upload.tmp",
                    "### 200 ok",
                    "### 204 deleted",
                ),
            ),
        )
        val mutationClient = mutationClient(client, OfishProbeResult.Available(capabilities), uploadChunkBytes = 2)

        val result = mutationClient.uploadFile(
            uploadRequest("file.bin", byteArrayOf(1, 2)) { error("progress failed") },
        )

        assertTrue(result is FileOperationResult.Failed)
        assertEquals(3, client.commands.size)
        assertTrue(client.commands.last().decodedScript().contains("#OFISH_UPLOAD_ABORT"))
        assertEquals(listOf("session-1"), client.deletedIds)
    }

    @Test
    fun `upload supports whitespace in destination parent and temp token`() = runTest {
        val token = "parent with spaces/.ofish.upload.tmp"
        val client = FakeOfishWorkspaceClient(
            outputs = ArrayDeque(
                listOf(
                    "### 200 ok upload=$token",
                    "### 200 ok",
                    "### 200 ok hash=abc",
                ),
            ),
        )
        val mutationClient = mutationClient(client, OfishProbeResult.Available(capabilities), uploadChunkBytes = 2)

        val result = mutationClient.uploadFile(uploadRequest("parent with spaces/file.bin", byteArrayOf(1, 2)))

        assertTrue(result is FileOperationResult.Ok)
        assertTrue(client.commands[1].decodedScript().contains("TMP='$token'"))
        assertTrue(client.commands[2].decodedScript().contains("TMP='$token'"))
    }

    @Test
    fun `unsafe upload token rejected before chunks`() = runTest {
        val client = FakeOfishWorkspaceClient(
            outputs = ArrayDeque(listOf("### 200 ok upload=/tmp/evil", "### 204 deleted")),
        )
        val mutationClient = mutationClient(client, OfishProbeResult.Available(capabilities), uploadChunkBytes = 2)

        val result = mutationClient.uploadFile(uploadRequest("dir/file.bin", byteArrayOf(1, 2, 3, 4)))

        assertTrue(result is FileOperationResult.Failed)
        assertEquals(2, client.commands.size)
        assertTrue(client.commands.last().decodedScript().contains("#OFISH_UPLOAD_ABORT"))
        assertTrue(client.commands.last().decodedScript().contains("TMP='/tmp/evil'"))
    }

    @Test
    fun `cancellation after upload init aborts temporary file`() = runTest {
        val initReturned = CompletableDeferred<Unit>()
        val continueAfterInit = CompletableDeferred<Unit>()
        val client = FakeOfishWorkspaceClient(
            outputs = ArrayDeque(listOf("### 200 ok upload=.ofish.upload.tmp", "### 204 deleted")),
        )
        val mutationClient = mutationClient(
            client,
            OfishProbeResult.Available(capabilities),
            uploadChunkBytesProvider = UploadChunkBytesProvider {
                initReturned.complete(Unit)
                continueAfterInit.await()
                2
            },
        )
        val upload = async {
            mutationClient.uploadFile(uploadRequest("file.bin", byteArrayOf(1, 2)))
        }
        initReturned.await()

        upload.cancel(CancellationException("cancel upload"))
        continueAfterInit.complete(Unit)
        runCatching { upload.await() }

        assertTrue(client.commands.last().decodedScript().contains("#OFISH_UPLOAD_ABORT"))
        assertEquals(listOf("session-1"), client.deletedIds)
    }

    @Test
    fun `nested upload token is rejected before chunks`() = runTest {
        assertUploadTokenRejected("dir/.ofish.upload.evil/target", destinationPath = "dir/file.bin")
    }

    @Test
    fun `sibling upload token is rejected before chunks`() = runTest {
        assertUploadTokenRejected("other/.ofish.upload.tmp", destinationPath = "dir/file.bin")
    }

    @Test
    fun `extra segment upload token is rejected before chunks`() = runTest {
        assertUploadTokenRejected("dir/sub/.ofish.upload.tmp", destinationPath = "dir/file.bin")
    }

    @Test
    fun `wrong parent upload token is rejected before chunks`() = runTest {
        assertUploadTokenRejected(".ofish.upload.tmp", destinationPath = "dir/file.bin")
    }

    @Test
    fun `upload finish threads expected hash for final recheck`() = runTest {
        val client = FakeOfishWorkspaceClient(
            outputs = ArrayDeque(
                listOf(
                    "### 200 ok upload=.ofish.upload.tmp",
                    "### 200 ok",
                    "### 409 conflict actual=newer",
                    "### 204 deleted",
                ),
            ),
        )
        val mutationClient = mutationClient(client, OfishProbeResult.Available(capabilities), uploadChunkBytes = 4)

        val result = mutationClient.uploadFile(uploadRequest("file.bin", byteArrayOf(1, 2), expectedHash = "old"))

        assertTrue(result is FileOperationResult.Conflict)
        assertEquals("newer", (result as FileOperationResult.Conflict).currentHash)
        assertTrue(client.commands.any { it.contains("(base64 -d 2>/dev/null || base64 -D) | sh") })
    }

    @Test
    fun `concurrent cached capabilities probe once`() = runTest {
        val client = FakeOfishWorkspaceClient(
            outputs = ArrayDeque(
                listOf(
                    "caps base64=1 base64_decode=-d hash=sha256sum " +
                        "mv=1 mkdir=1 rm=1 awk=1 mktemp=1 chmod=1 mode=stat -c %a\n### 200 ok",
                ),
            ),
        )
        val probe = OfishCapabilityProbe(client, OfishSessionFactory(client))
        val cache = CachedOfishCapabilities(probe)

        val results = (1..8).map { async { cache.get() } }.awaitAll()

        assertTrue(results.all { it is OfishProbeResult.Available })
        assertEquals(1, client.commands.size)
        assertEquals(1, client.createdTitles.size)
    }

    private suspend fun assertUploadTokenRejected(uploadToken: String, destinationPath: String) {
        val client = FakeOfishWorkspaceClient(
            outputs = ArrayDeque(listOf("### 200 ok upload=$uploadToken", "### 204 deleted")),
        )
        val mutationClient = mutationClient(client, OfishProbeResult.Available(capabilities), uploadChunkBytes = 2)

        val result = mutationClient.uploadFile(uploadRequest(destinationPath, byteArrayOf(1, 2, 3, 4)))

        assertTrue(result is FileOperationResult.Failed)
        assertEquals(2, client.commands.size)
        assertTrue(client.commands.last().decodedScript().contains("#OFISH_UPLOAD_ABORT"))
    }

    private fun mutationClient(
        client: FakeOfishWorkspaceClient,
        probeResult: OfishProbeResult,
        uploadChunkBytes: Int = 256 * 1024,
    ): OfishMutationClient = mutationClient(
        client = client,
        probeResult = probeResult,
        uploadChunkBytesProvider = FixedUploadChunkBytesProvider(uploadChunkBytes),
    )

    private fun mutationClient(
        client: FakeOfishWorkspaceClient,
        probeResult: OfishProbeResult,
        uploadChunkBytesProvider: UploadChunkBytesProvider,
        maxUploadSourceBytes: Long = MAX_UPLOAD_SOURCE_BYTES,
    ): OfishMutationClient {
        val probe = OfishCapabilityProbe(client, OfishSessionFactory(client))
        return OfishMutationClient(
            client = client,
            sessionFactory = OfishSessionFactory(client),
            capabilityCache = FakeCapabilityCache(probe, probeResult),
            commandBuilder = OfishCommandBuilder(),
            uploadChunkBytes = uploadChunkBytesProvider,
            maxUploadSourceBytes = maxUploadSourceBytes,
        )
    }

    private fun String.decodedScript(): String {
        val encoded = Regex("printf %s '?([A-Za-z0-9+/=]+)'? ").find(this)?.groupValues?.get(1)
            ?: error("missing wrapped script")
        return String(Base64.getDecoder().decode(encoded), Charsets.UTF_8)
    }

    private fun uploadRequest(
        path: String,
        bytes: ByteArray,
        expectedHash: String? = null,
        onBytesUploaded: (suspend (Long) -> Unit)? = null,
    ) = FileUploadRequest(
        path = path,
        contentLength = bytes.size.toLong(),
        openStream = { ByteArrayInputStream(bytes) as InputStream },
        expectedHash = expectedHash,
        onBytesUploaded = onBytesUploaded,
    )

    private class FakeCapabilityCache(
        probe: OfishCapabilityProbe,
        private val result: OfishProbeResult,
    ) : CachedOfishCapabilities(probe) {
        override suspend fun get(): OfishProbeResult = result
    }

    private inner class FakeOfishWorkspaceClient(
        val outputs: ArrayDeque<String> = ArrayDeque(),
    ) : OfishWorkspaceClient {
        override val workspace: Workspace = Workspace(
            server = ServerRef.fromEndpoint("http://localhost:4096", "local"),
            directory = "/repo",
        )
        val createdTitles = mutableListOf<String>()
        val deletedIds = mutableListOf<String>()
        val commands = mutableListOf<String>()

        override suspend fun createSession(title: String): SessionDto {
            delay(1)
            createdTitles += title
            return SessionDto(
                id = "session-${createdTitles.size}",
                projectID = "project",
                directory = "/repo",
                title = title,
                version = "test",
                time = TimeDto(created = 0),
            )
        }

        override suspend fun deleteSession(id: String): Boolean {
            deletedIds += id
            return true
        }

        override suspend fun executeShellCommand(sessionId: String, request: ShellCommandRequest): MessageWrapperDto {
            commands += request.command
            val script = request.command.decodedScript()
            val marker = Regex("#OFISH_[A-Z_]+").find(script)?.value
                ?: error("OFISH command did not contain a marker")
            val output = outputs.removeFirstOrNull() ?: "### 200 ok"
            return message("$marker\n$output")
        }

        override suspend fun listSessionsCurrentWorkspace(limit: Int?): List<SessionDto> = emptyList()

        override suspend fun respondToPermission(id: String, request: PermissionResponseRequest): Boolean = true

        private fun message(output: String): MessageWrapperDto = MessageWrapperDto(
            info = MessageInfoDto(
                id = "message",
                sessionID = "session",
                time = MessageTimeDto(created = 0),
                role = "assistant",
            ),
            parts = listOf(
                PartDto(
                    id = "part",
                    sessionID = "session",
                    messageID = "message",
                    type = "text",
                    text = output,
                ),
            ),
        )
    }
}
