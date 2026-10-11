package dev.blazelight.p4oc.ui.workspace

import dev.blazelight.p4oc.domain.workspace.PathRejection
import dev.blazelight.p4oc.domain.workspace.ServerPath
import dev.blazelight.p4oc.domain.workspace.ServerPathParse
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

private const val ROOT_PATH = "/"
private const val PROJECT_PATH = "/srv/app"
private const val PROJECT_PARENT = "/srv"
private const val WINDOWS_PROJECT_PATH = "C:\\workspace\\APP"

class WorkspacePathValidationTest {
    @Test
    fun rejectsInvalidSyntaxBeforeBrowsing() = runBlocking {
        val result = validateWorkspacePath("relative/path", emptyList(), null, allowUnverified = false)

        assertEquals(
            WorkspacePathValidation.Invalid(WorkspacePathProblem.Syntax(PathRejection.NotAbsolute)),
            result,
        )
    }

    @Test
    fun rejectsDuplicateNormalizedPathWithoutBrowsing() = runBlocking {
        val result = validateWorkspacePath(
            raw = "$PROJECT_PATH/",
            existingRoots = listOf(serverPath(PROJECT_PATH)),
            browser = ServerDirectoryBrowser { error("Duplicate paths must not be browsed") },
            allowUnverified = false,
        )

        assertEquals(WorkspacePathValidation.Invalid(WorkspacePathProblem.Duplicate), result)
    }

    @Test
    fun reportsOfflineWhenNoBrowserIsAvailable() = runBlocking {
        val result = validateWorkspacePath(PROJECT_PATH, emptyList(), browser = null, allowUnverified = false)

        assertEquals(WorkspacePathValidation.Invalid(WorkspacePathProblem.Offline), result)
    }

    @Test
    fun verifiesAPathAtTheFilesystemRoot() = runBlocking {
        val root = serverPath(ROOT_PATH)
        val result = validateWorkspacePath(
            ROOT_PATH,
            emptyList(),
            browser = ServerDirectoryBrowser { listed(it) },
            allowUnverified = false,
        )

        assertEquals(WorkspacePathValidation.Valid(root, verified = true), result)
    }

    @Test
    fun reportsMissingDirectoryFromParentListing() = runBlocking {
        // Servers answer a missing path with an error (v1 500 UnknownError, v2 500 empty body).
        val browser = ServerDirectoryBrowser { path ->
            if (path.value == PROJECT_PARENT) listed(path, folders = listOf("other")) else BrowseResult.NotReadable
        }
        val result = validateWorkspacePath(PROJECT_PATH, emptyList(), browser, allowUnverified = false)

        assertEquals(
            WorkspacePathValidation.Invalid(WorkspacePathProblem.Missing("app", PROJECT_PARENT)),
            result,
        )
    }

    @Test
    fun acceptsAFolderHiddenFromTheParentListingWhenItLists() = runBlocking {
        // Dot-folders and folders past the browse cap are not in the parent listing.
        val browser = ServerDirectoryBrowser { path -> listed(path) }
        val result = validateWorkspacePath(PROJECT_PATH, emptyList(), browser, allowUnverified = false)

        assertEquals(WorkspacePathValidation.Valid(serverPath(PROJECT_PATH), verified = true), result)
    }

    @Test
    fun distinguishesAFileFromAMissingDirectory() = runBlocking {
        val browser = ServerDirectoryBrowser { path -> listed(path, files = listOf("app")) }
        val result = validateWorkspacePath(PROJECT_PATH, emptyList(), browser, allowUnverified = false)

        assertEquals(WorkspacePathValidation.Invalid(WorkspacePathProblem.IsFile("app")), result)
    }

    @Test
    fun reportsUnlistableParent() = runBlocking {
        val browser = ServerDirectoryBrowser { BrowseResult.NotReadable }
        val result = validateWorkspacePath(PROJECT_PATH, emptyList(), browser, allowUnverified = false)

        assertEquals(
            WorkspacePathValidation.Invalid(WorkspacePathProblem.ParentNotListable(PROJECT_PARENT)),
            result,
        )
    }

    @Test
    fun reportsUnreadableTargetAfterFindingItInParent() = runBlocking {
        val browser = ServerDirectoryBrowser { path ->
            if (path.value == PROJECT_PARENT) listed(path, folders = listOf("app")) else BrowseResult.NotReadable
        }
        val result = validateWorkspacePath(PROJECT_PATH, emptyList(), browser, allowUnverified = false)

        assertEquals(WorkspacePathValidation.Invalid(WorkspacePathProblem.NotReadable), result)
    }

    @Test
    fun requiresConfirmationWhenBrowsingIsUnsupported() = runBlocking {
        val browser = ServerDirectoryBrowser { BrowseResult.Unsupported }

        assertEquals(
            WorkspacePathValidation.Invalid(WorkspacePathProblem.Unverifiable),
            validateWorkspacePath(PROJECT_PATH, emptyList(), browser, allowUnverified = false),
        )
    }

    @Test
    fun acceptsUnsupportedPathAsUnverifiedAfterConfirmation() = runBlocking {
        val path = serverPath(PROJECT_PATH)
        val browser = ServerDirectoryBrowser { directory ->
            if (directory.value == PROJECT_PARENT) {
                listed(directory, folders = listOf("app"))
            } else {
                BrowseResult.Unsupported
            }
        }

        assertEquals(
            WorkspacePathValidation.Valid(path, verified = false),
            validateWorkspacePath(PROJECT_PATH, emptyList(), browser, allowUnverified = true),
        )
    }

    @Test
    fun matchesWindowsDirectoryNamesWithoutCaseSensitivity() = runBlocking {
        val browser = ServerDirectoryBrowser { path ->
            if (path.value == "C:\\workspace") listed(path, folders = listOf("app")) else listed(path)
        }
        val result = validateWorkspacePath(
            raw = "c:/workspace/APP",
            existingRoots = emptyList(),
            browser = browser,
            allowUnverified = false,
        )

        assertEquals(WorkspacePathValidation.Valid(serverPath(WINDOWS_PROJECT_PATH), verified = true), result)
    }

    private fun listed(
        path: ServerPath,
        folders: List<String> = emptyList(),
        files: List<String> = emptyList(),
    ) = BrowseResult.Listed(path, folders, folders.size, files)

    private fun serverPath(value: String): ServerPath = when (val parsed = ServerPath.parseUserInput(value)) {
        is ServerPathParse.Valid -> parsed.path
        is ServerPathParse.Invalid -> error("Invalid test path: $parsed")
    }
}
