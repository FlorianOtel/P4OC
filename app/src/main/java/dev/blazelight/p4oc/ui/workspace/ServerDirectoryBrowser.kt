package dev.blazelight.p4oc.ui.workspace

import dev.blazelight.p4oc.data.remote.dto.FileNodeDto
import dev.blazelight.p4oc.data.server.StaleWorkspaceClientException
import dev.blazelight.p4oc.data.workspace.WorkspaceClient
import dev.blazelight.p4oc.domain.workspace.PathRejection
import dev.blazelight.p4oc.domain.workspace.ServerPath
import dev.blazelight.p4oc.domain.workspace.ServerPathParse
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException

private const val DIRECTORY_TYPE = "directory"
private const val FILE_TYPE = "file"
const val MAX_BROWSE_FOLDERS = 500
private const val HTTP_NOT_FOUND = 404
private const val HTTP_METHOD_NOT_ALLOWED = 405
private const val HTTP_NOT_IMPLEMENTED = 501
private val BROWSE_NAME_ORDER = Comparator<String> { first, second ->
    first.compareTo(second, ignoreCase = true).takeIf { it != 0 } ?: first.compareTo(second)
}

sealed interface BrowseResult {
    data class Listed(
        val path: ServerPath,
        val folders: List<String>,
        val totalFolders: Int,
        val files: List<String> = emptyList(),
    ) : BrowseResult

    data object NotReadable : BrowseResult
    data object Unsupported : BrowseResult
    data object Offline : BrowseResult
}

fun interface ServerDirectoryBrowser {
    suspend fun list(path: ServerPath): BrowseResult
}

/** Real browser over a server-scoped client (the Home owner's `Workspace(server, null)` client). */
fun workspaceClientDirectoryBrowser(client: WorkspaceClient): ServerDirectoryBrowser =
    ServerDirectoryBrowser { path ->
        // IllegalStateException also covers stale clients and cancellation; mapBrowseFailure rethrows cancellation.
        try {
            mapBrowseEntries(path, client.listServerDirectory(path.root, path.value))
        } catch (error: HttpException) {
            mapBrowseFailure(error)
        } catch (error: IOException) {
            mapBrowseFailure(error)
        } catch (error: SerializationException) {
            mapBrowseFailure(error)
        } catch (error: IllegalStateException) {
            mapBrowseFailure(error)
        }
    }

internal fun mapBrowseEntries(path: ServerPath, entries: List<FileNodeDto>): BrowseResult.Listed {
    val folders = ArrayList<String>()
    val files = ArrayList<String>()
    entries.forEach { entry ->
        browseEntryName(entry)?.let { name ->
            when (entry.type) {
                DIRECTORY_TYPE -> folders += name
                FILE_TYPE -> files += name
            }
        }
    }
    folders.sortWith(BROWSE_NAME_ORDER)
    files.sortWith(BROWSE_NAME_ORDER)
    val totalFolders = folders.size
    val cappedFolders = if (totalFolders > MAX_BROWSE_FOLDERS) {
        ArrayList(folders.subList(0, MAX_BROWSE_FOLDERS))
    } else {
        folders
    }
    return BrowseResult.Listed(path, cappedFolders, totalFolders, files)
}

private fun browseEntryName(entry: FileNodeDto): String? {
    val name = if (entry.name.isBlank()) {
        entry.path.trimEnd('/', '\\').substringAfterLast('/').substringAfterLast('\\')
    } else {
        entry.name
    }
    // Hidden entries are skipped; names ServerPath.child can't represent (control characters) too.
    return name.takeUnless { !ServerPath.isValidComponent(it) || it.startsWith('.') }
}

internal fun mapBrowseFailure(error: Throwable): BrowseResult = when (error) {
    is CancellationException -> throw error
    is HttpException -> when (error.code()) {
        HTTP_NOT_FOUND, HTTP_METHOD_NOT_ALLOWED, HTTP_NOT_IMPLEMENTED -> BrowseResult.Unsupported
        else -> BrowseResult.NotReadable
    }
    is IOException, is StaleWorkspaceClientException -> BrowseResult.Offline
    else -> BrowseResult.NotReadable
}

sealed interface WorkspacePathProblem {
    data class Syntax(val reason: PathRejection) : WorkspacePathProblem
    data object Duplicate : WorkspacePathProblem
    data class ParentNotListable(val parent: String) : WorkspacePathProblem
    data class Missing(val name: String, val parent: String) : WorkspacePathProblem
    data class IsFile(val name: String) : WorkspacePathProblem
    data object NotReadable : WorkspacePathProblem
    data object Offline : WorkspacePathProblem

    /** Server can't list folders; the path may still be added after the user confirms. */
    data object Unverifiable : WorkspacePathProblem
}

sealed interface WorkspacePathValidation {
    data class Valid(val path: ServerPath, val verified: Boolean) : WorkspacePathValidation
    data class Invalid(val problem: WorkspacePathProblem) : WorkspacePathValidation
}

suspend fun validateWorkspacePath(
    raw: String,
    existingRoots: List<ServerPath>,
    browser: ServerDirectoryBrowser?,
    allowUnverified: Boolean,
): WorkspacePathValidation = when (val parsed = ServerPath.parseUserInput(raw)) {
    is ServerPathParse.Invalid -> WorkspacePathValidation.Invalid(WorkspacePathProblem.Syntax(parsed.reason))
    is ServerPathParse.Valid -> validateParsedWorkspacePath(parsed.path, existingRoots, browser, allowUnverified)
}

private suspend fun validateParsedWorkspacePath(
    path: ServerPath,
    existingRoots: List<ServerPath>,
    browser: ServerDirectoryBrowser?,
    allowUnverified: Boolean,
): WorkspacePathValidation = when {
    existingRoots.any { it.compareKey == path.compareKey } ->
        WorkspacePathValidation.Invalid(WorkspacePathProblem.Duplicate)
    browser == null -> WorkspacePathValidation.Invalid(WorkspacePathProblem.Offline)
    path.parent() == null -> validateRootPath(path, browser, allowUnverified)
    else -> validateChildPath(path, browser, allowUnverified)
}

private suspend fun validateRootPath(
    path: ServerPath,
    browser: ServerDirectoryBrowser,
    allowUnverified: Boolean,
): WorkspacePathValidation = when (browser.list(path)) {
    is BrowseResult.Listed -> WorkspacePathValidation.Valid(path, verified = true)
    BrowseResult.NotReadable -> WorkspacePathValidation.Invalid(WorkspacePathProblem.NotReadable)
    BrowseResult.Offline -> WorkspacePathValidation.Invalid(WorkspacePathProblem.Offline)
    BrowseResult.Unsupported -> unverifiablePath(path, allowUnverified)
}

private suspend fun validateChildPath(
    path: ServerPath,
    browser: ServerDirectoryBrowser,
    allowUnverified: Boolean,
): WorkspacePathValidation {
    val parent = requireNotNull(path.parent())
    return when (val parentResult = browser.list(parent)) {
        is BrowseResult.Listed -> validateListedParent(path, parent, parentResult, browser, allowUnverified)
        BrowseResult.NotReadable -> WorkspacePathValidation.Invalid(
            WorkspacePathProblem.ParentNotListable(parent.value),
        )
        BrowseResult.Offline -> WorkspacePathValidation.Invalid(WorkspacePathProblem.Offline)
        BrowseResult.Unsupported -> unverifiablePath(path, allowUnverified)
    }
}

/**
 * The parent listing hides dot-folders and caps at [MAX_BROWSE_FOLDERS], so a name absent from it is
 * only reported missing when listing the target itself also fails.
 */
private suspend fun validateListedParent(
    path: ServerPath,
    parent: ServerPath,
    listing: BrowseResult.Listed,
    browser: ServerDirectoryBrowser,
    allowUnverified: Boolean,
): WorkspacePathValidation {
    val name = path.components.last()
    if (listing.files.any(path::lastComponentMatches)) {
        return WorkspacePathValidation.Invalid(WorkspacePathProblem.IsFile(name))
    }
    val listedAsFolder = listing.folders.any(path::lastComponentMatches)
    return when (browser.list(path)) {
        is BrowseResult.Listed -> WorkspacePathValidation.Valid(path, verified = true)
        BrowseResult.NotReadable -> WorkspacePathValidation.Invalid(
            if (listedAsFolder) WorkspacePathProblem.NotReadable else WorkspacePathProblem.Missing(name, parent.value),
        )
        BrowseResult.Offline -> WorkspacePathValidation.Invalid(WorkspacePathProblem.Offline)
        BrowseResult.Unsupported -> unverifiablePath(path, allowUnverified)
    }
}

private fun unverifiablePath(path: ServerPath, allowUnverified: Boolean): WorkspacePathValidation =
    if (allowUnverified) {
        WorkspacePathValidation.Valid(path, verified = false)
    } else {
        WorkspacePathValidation.Invalid(WorkspacePathProblem.Unverifiable)
    }
