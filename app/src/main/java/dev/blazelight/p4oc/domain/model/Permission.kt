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
