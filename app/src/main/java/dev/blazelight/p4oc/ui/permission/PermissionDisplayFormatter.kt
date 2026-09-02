package dev.blazelight.p4oc.ui.permission

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.blazelight.p4oc.R
import dev.blazelight.p4oc.domain.model.Permission
import dev.blazelight.p4oc.domain.model.PermissionKind

object PermissionDisplayFormatter {
    fun title(context: Context, permission: Permission): String {
        val action = actionText(context, permission)
        return title(context, action, permission.patterns)
    }

    fun title(context: Context, actionText: String, patterns: List<String>): String {
        val pattern = patterns.firstOrNull().orEmpty()
        return if (pattern.isNotBlank()) {
            context.getString(R.string.permission_title_with_pattern, actionText, pattern)
        } else {
            actionText
        }
    }

    fun actionText(context: Context, permission: Permission): String {
        val actionRes = actionStringRes(permission.kind)
        return if (actionRes != null) {
            context.getString(actionRes)
        } else {
            context.getString(R.string.permission_action_unknown, unknownAction(permission.type))
        }
    }

    @StringRes
    fun actionStringRes(kind: PermissionKind): Int? = when (kind) {
        PermissionKind.Bash -> R.string.permission_action_bash
        PermissionKind.Edit -> R.string.permission_action_edit
        PermissionKind.Patch -> R.string.permission_action_patch
        PermissionKind.WebFetch -> R.string.permission_action_webfetch
        PermissionKind.Task -> R.string.permission_action_task
        PermissionKind.Skill -> R.string.permission_action_skill
        PermissionKind.ExternalDirectory -> R.string.permission_action_external_directory
        PermissionKind.DoomLoop -> R.string.permission_action_doom_loop
        PermissionKind.Unknown -> null
    }

    fun unknownAction(type: String): String = type.replaceFirstChar { char ->
        if (char.isLowerCase()) char.titlecase() else char.toString()
    }
}

@Composable
fun permissionTitle(permission: Permission): String {
    val actionRes = PermissionDisplayFormatter.actionStringRes(permission.kind)
    val actionText = if (actionRes != null) {
        stringResource(actionRes)
    } else {
        stringResource(R.string.permission_action_unknown, PermissionDisplayFormatter.unknownAction(permission.type))
    }
    val pattern = permission.patterns.firstOrNull().orEmpty()
    return if (pattern.isNotBlank()) {
        stringResource(R.string.permission_title_with_pattern, actionText, pattern)
    } else {
        actionText
    }
}
