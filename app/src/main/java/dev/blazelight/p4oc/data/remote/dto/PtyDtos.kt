package dev.blazelight.p4oc.data.remote.dto

import kotlinx.serialization.Serializable

// ============================================================================
// PTY Types
// ============================================================================

@Serializable
data class PtyDto(
    val id: String,
    val title: String,
    val command: String,
    val args: List<String>,
    val cwd: String,
    val status: String,
    // Some deployed servers return null while the process is starting despite the upstream schema.
    val pid: Int? = null,
    val exitCode: Int? = null,
)

@Serializable
data class CreatePtyRequest(
    val command: String? = null,
    val args: List<String> = emptyList(),
    val cwd: String? = null,
    val title: String? = null,
    val env: Map<String, String> = emptyMap()
)

@Serializable
data class UpdatePtyRequest(
    val title: String? = null,
    val size: PtySizeDto? = null
)

@Serializable
data class PtySizeDto(
    val rows: Int,
    val cols: Int
)

@Serializable
data class PtyInputRequest(
    val data: String
)

@Serializable
data class PtyOutputDto(
    val lines: List<String> = emptyList(),
    val totalLines: Int = 0,
    val hasMore: Boolean = false
)
