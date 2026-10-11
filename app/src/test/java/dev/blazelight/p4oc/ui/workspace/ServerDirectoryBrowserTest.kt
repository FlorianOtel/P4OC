package dev.blazelight.p4oc.ui.workspace

import dev.blazelight.p4oc.core.network.ConnectionState
import dev.blazelight.p4oc.core.network.OpenCodeApi
import dev.blazelight.p4oc.data.remote.dto.FileNodeDto
import dev.blazelight.p4oc.data.server.ActiveServerApiProvider
import dev.blazelight.p4oc.data.server.StaleWorkspaceClientException
import dev.blazelight.p4oc.data.workspace.WorkspaceClient
import dev.blazelight.p4oc.domain.server.ServerGeneration
import dev.blazelight.p4oc.domain.server.ServerRef
import dev.blazelight.p4oc.domain.workspace.ServerPath
import dev.blazelight.p4oc.domain.workspace.Workspace
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

private const val HTTP_NOT_FOUND = 404
private const val HTTP_METHOD_NOT_ALLOWED = 405
private const val HTTP_INTERNAL_SERVER_ERROR = 500
private const val HTTP_NOT_IMPLEMENTED = 501
private const val EXTRA_BROWSE_FOLDERS = 3
private const val FOLDER_NUMBER_WIDTH = 3
private const val BROWSE_TEST_FOLDER_COUNT = MAX_BROWSE_FOLDERS + EXTRA_BROWSE_FOLDERS
private val UNSUPPORTED_HTTP_CODES = listOf(HTTP_NOT_FOUND, HTTP_METHOD_NOT_ALLOWED, HTTP_NOT_IMPLEMENTED)

class ServerDirectoryBrowserTest {
    @Test
    fun mapsV1AndV2EntriesToTheSameFolderNames() {
        val path = serverPath("/workspace/project")
        val v1 = mapBrowseEntries(
            path,
            listOf(entry(name = "notes", path = "workspace/project/notes/", absolute = "/workspace/project/notes")),
        )
        val v2 = mapBrowseEntries(
            path,
            listOf(entry(name = "notes", path = "notes/", absolute = "notes")),
        )

        assertEquals(listOf("notes"), v1.folders)
        assertEquals(v1.folders, v2.folders)
    }

    @Test
    fun filtersHiddenDotSeparatorControlAndEscapeNamesAndSeparatesFiles() {
        val path = serverPath("/workspace")
        val result = mapBrowseEntries(
            path,
            listOf(
                entry(name = "", path = "workspace/visible/", type = "directory"),
                entry(name = "", path = "workspace\\other\\", type = "directory"),
                entry(name = ".", path = ".", type = "directory"),
                entry(name = "..", path = "..", type = "directory"),
                entry(name = ".hidden", path = ".hidden", type = "directory"),
                entry(name = "../outside", path = "../outside", type = "directory"),
                entry(name = "nested/name", path = "nested/name", type = "directory"),
                entry(name = "nested/", path = "nested/", type = "directory"),
                entry(name = "nested\\name", path = "nested\\name", type = "directory"),
                entry(name = "tab\tname", path = "tab\tname", type = "directory"),
                entry(name = "line\nbreak", path = "line\nbreak", type = "directory"),
                entry(name = "README.md", path = "README.md", type = "file"),
                entry(name = ".secret", path = ".secret", type = "file"),
                entry(name = "../outside", path = "../outside", type = "file"),
                entry(name = "ignored", path = "ignored", type = "other"),
            ),
        )

        assertEquals(listOf("other", "visible"), result.folders)
        // Every listed folder must be openable; rows build their child path while composing.
        result.folders.forEach(path::child)
        assertEquals(listOf("README.md"), result.files)
    }

    @Test
    fun capsFoldersAndRetainsTheUncappedTotal() {
        val result = mapBrowseEntries(
            serverPath("/workspace"),
            (0 until BROWSE_TEST_FOLDER_COUNT).map { index ->
                entry(name = "folder-${index.toString().padStart(FOLDER_NUMBER_WIDTH, '0')}", path = "folder-$index")
            },
        )

        assertEquals(MAX_BROWSE_FOLDERS, result.folders.size)
        assertEquals(BROWSE_TEST_FOLDER_COUNT, result.totalFolders)
    }

    @Test
    fun mapsUnsupportedHttpStatuses() {
        UNSUPPORTED_HTTP_CODES.forEach { code ->
            assertSame(BrowseResult.Unsupported, mapBrowseFailure(httpException(code)))
        }
    }

    @Test
    fun mapsOtherHttpStatusesToNotReadable() {
        assertSame(BrowseResult.NotReadable, mapBrowseFailure(httpException(HTTP_INTERNAL_SERVER_ERROR)))
    }

    @Test
    fun mapsIoAndStaleClientFailuresToOffline() {
        assertSame(BrowseResult.Offline, mapBrowseFailure(IOException("offline")))
        assertSame(BrowseResult.Offline, mapBrowseFailure(StaleWorkspaceClientException("stale")))
    }

    @Test(expected = CancellationException::class)
    fun rethrowsCancellation() {
        mapBrowseFailure(CancellationException("cancelled"))
    }

    @Test
    fun browsingRoutesAtTheFilesystemRootWithTheAbsolutePathNotTheTabDirectory() = runBlocking {
        val api = mockk<OpenCodeApi>()
        coEvery { api.listFiles(any(), any(), any()) } returns emptyList()
        val client = WorkspaceClient(
            workspace = Workspace(ServerRef.fromEndpointKey("http://server.test"), "/tab/directory"),
            generation = ServerGeneration(1L),
            apiProvider = ActiveServerApiProvider { _, _ -> api },
            connectionState = MutableStateFlow(ConnectionState.Connected),
        )

        workspaceClientDirectoryBrowser(client).list(serverPath("/workspace/multirepo-probe"))

        coVerify(exactly = 1) { api.listFiles("/workspace/multirepo-probe", "/", null) }
    }

    private fun serverPath(value: String): ServerPath =
        (ServerPath.parseUserInput(value) as dev.blazelight.p4oc.domain.workspace.ServerPathParse.Valid).path

    private fun entry(
        name: String,
        path: String,
        absolute: String = path,
        type: String = "directory",
    ) = FileNodeDto(name = name, path = path, absolute = absolute, type = type)

    private fun httpException(code: Int) = HttpException(
        Response.error<Any>(code, "".toResponseBody()),
    )
}
