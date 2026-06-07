package dev.blazelight.p4oc.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class Permission(
    val id: String,
    val type: String,
    val patterns: List<String>,
    val sessionID: String,
    val messageID: String,
    val callID: String? = null,
    val metadata: JsonObject,
    val always: List<String>
) {
    val kind: PermissionKind
        get() = PermissionKind.fromType(type)

    val title: String
        get() {
            val action = when (kind) {
                PermissionKind.Bash -> "Execute command"
                PermissionKind.Edit -> "Write to file"
                PermissionKind.Patch -> "Edit file"
                PermissionKind.WebFetch -> "Fetch URL"
                PermissionKind.Task -> "Run sub-agent"
                PermissionKind.Skill -> "Use skill"
                PermissionKind.ExternalDirectory -> "Access external directory"
                PermissionKind.DoomLoop -> "Continue execution"
                PermissionKind.Unknown -> type.replaceFirstChar { it.uppercase() }
            }
            val pattern = patterns.firstOrNull().orEmpty()
            return if (pattern.isNotEmpty()) "$action: $pattern" else action
        }
}

enum class PermissionKind {
    Bash,
    Edit,
    Patch,
    WebFetch,
    Task,
    Skill,
    ExternalDirectory,
    DoomLoop,
    Unknown;

    companion object {
        fun fromType(type: String): PermissionKind = when (type) {
            "bash", "shell" -> Bash
            "edit", "write" -> Edit
            "patch" -> Patch
            "webfetch" -> WebFetch
            "task" -> Task
            "skill" -> Skill
            "external_directory" -> ExternalDirectory
            "doom_loop" -> DoomLoop
            else -> Unknown
        }
    }
}

enum class PermissionResponse(val value: String) {
    ONCE("once"),
    REJECT("reject"),
    ALWAYS("always")
}
