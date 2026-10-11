package dev.blazelight.p4oc.ui.workspace

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import dev.blazelight.p4oc.R
import dev.blazelight.p4oc.domain.server.ServerRef
import dev.blazelight.p4oc.domain.workspace.UserWorkspaceLimits
import dev.blazelight.p4oc.ui.components.TuiAlertDialog
import dev.blazelight.p4oc.ui.components.TuiButton
import dev.blazelight.p4oc.ui.components.TuiConfirmDialog
import dev.blazelight.p4oc.ui.components.TuiDropdownMenuItem
import dev.blazelight.p4oc.ui.components.TuiTextButton
import dev.blazelight.p4oc.ui.components.TuiTextField
import dev.blazelight.p4oc.ui.theme.LocalOpenCodeTheme
import dev.blazelight.p4oc.ui.theme.Spacing

private const val MAX_WORKSPACE_ALIAS_LENGTH = UserWorkspaceLimits.MAX_ALIAS_LENGTH

/** Workspace identity and presentation metadata required by its context menu. */
data class WorkspaceActionSubject(
    val server: ServerRef,
    val directory: String,
    val label: WorkspaceLabel,
    val canAddAsRoot: Boolean,
)

class WorkspaceActionHandlers(
    val onRename: (String?) -> Unit,
    val onResetName: () -> Unit,
    val onCopyPath: () -> Unit,
    val onAddAsWorkspace: () -> Unit,
    val onRemove: () -> Unit,
)

internal enum class WorkspaceActionDialog { Rename, Remove }

@Stable
class WorkspaceActionsState {
    var isMenuOpen: Boolean by mutableStateOf(false)
    internal var dialog: WorkspaceActionDialog? by mutableStateOf(null)

    fun requestRename() {
        isMenuOpen = false
        dialog = WorkspaceActionDialog.Rename
    }

    fun requestRemove() {
        isMenuOpen = false
        dialog = WorkspaceActionDialog.Remove
    }
}

/** Keyed by [subjectKey] so a reordered row never carries a pending rename/remove to another workspace. */
@Composable
fun rememberWorkspaceActionsState(subjectKey: Any): WorkspaceActionsState =
    remember(subjectKey) { WorkspaceActionsState() }

/** Tap runs [onClick]; long-press opens workspace actions and exposes them to accessibility. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.workspaceActionGestures(
    state: WorkspaceActionsState,
    subject: WorkspaceActionSubject,
    handlers: WorkspaceActionHandlers,
    onClick: () -> Unit,
): Modifier {
    val haptic = LocalHapticFeedback.current
    val openActionsLabel = stringResource(R.string.workspace_action_open)
    val accessibilityActions = workspaceAccessibilityActions(state, subject, handlers)
    return combinedClickable(
        onClick = onClick,
        onLongClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            state.isMenuOpen = true
        },
        onLongClickLabel = openActionsLabel,
        role = Role.Button,
    ).semantics { customActions = accessibilityActions }
}

@Composable
private fun workspaceAccessibilityActions(
    state: WorkspaceActionsState,
    subject: WorkspaceActionSubject,
    handlers: WorkspaceActionHandlers,
): List<CustomAccessibilityAction> {
    val rename = stringResource(R.string.workspace_action_rename)
    val reset = stringResource(R.string.workspace_action_reset)
    val copyPath = stringResource(R.string.workspace_action_copy_path)
    val addRoot = stringResource(R.string.workspace_action_add_root)
    val remove = stringResource(R.string.workspace_action_remove)
    return buildList {
        add(workspaceAccessibilityAction(rename) { state.requestRename() })
        if (subject.label.custom != null) {
            add(workspaceAccessibilityAction(reset, handlers.onResetName))
        }
        add(workspaceAccessibilityAction(copyPath, handlers.onCopyPath))
        if (subject.canAddAsRoot && !subject.label.isUserRoot) {
            add(workspaceAccessibilityAction(addRoot, handlers.onAddAsWorkspace))
        }
        if (subject.label.isUserRoot) {
            add(workspaceAccessibilityAction(remove) { state.requestRemove() })
        }
    }
}

private fun workspaceAccessibilityAction(label: String, action: () -> Unit) = CustomAccessibilityAction(label) {
    action()
    true
}

/** Long-press menu and the corresponding rename and remove dialogs. */
@Composable
fun workspaceActionMenu(
    state: WorkspaceActionsState,
    subject: WorkspaceActionSubject,
    handlers: WorkspaceActionHandlers,
) {
    DropdownMenu(
        expanded = state.isMenuOpen,
        onDismissRequest = { state.isMenuOpen = false },
        modifier = Modifier.testTag("workspace_actions_menu"),
    ) {
        workspaceActionMenuItems(state, subject, handlers)
    }
    workspaceActionDialogs(state, subject, handlers)
}

@Composable
private fun workspaceActionMenuItems(
    state: WorkspaceActionsState,
    subject: WorkspaceActionSubject,
    handlers: WorkspaceActionHandlers,
) {
    val theme = LocalOpenCodeTheme.current
    TuiDropdownMenuItem(
        text = stringResource(R.string.workspace_action_rename),
        onClick = state::requestRename,
        leadingIcon = Icons.Default.Edit,
        modifier = Modifier.testTag("workspace_action_rename"),
    )
    if (subject.label.custom != null) {
        TuiDropdownMenuItem(
            text = stringResource(R.string.workspace_action_reset),
            onClick = { closeWorkspaceMenu(state, handlers.onResetName) },
            leadingIcon = Icons.Default.Refresh,
            modifier = Modifier.testTag("workspace_action_reset"),
        )
    }
    TuiDropdownMenuItem(
        text = stringResource(R.string.workspace_action_copy_path),
        onClick = { closeWorkspaceMenu(state, handlers.onCopyPath) },
        leadingIcon = Icons.Default.ContentCopy,
        modifier = Modifier.testTag("workspace_action_copy_path"),
    )
    if (subject.canAddAsRoot && !subject.label.isUserRoot) {
        TuiDropdownMenuItem(
            text = stringResource(R.string.workspace_action_add_root),
            onClick = { closeWorkspaceMenu(state, handlers.onAddAsWorkspace) },
            leadingIcon = Icons.Default.FolderOpen,
            modifier = Modifier.testTag("workspace_action_add_root"),
        )
    }
    if (subject.label.isUserRoot) {
        HorizontalDivider(color = theme.borderSubtle)
        workspaceRemoveMenuItem(state)
    }
}

private fun closeWorkspaceMenu(state: WorkspaceActionsState, action: () -> Unit) {
    state.isMenuOpen = false
    action()
}

@Composable
private fun workspaceRemoveMenuItem(state: WorkspaceActionsState) {
    val theme = LocalOpenCodeTheme.current
    val removeLabel = stringResource(R.string.workspace_action_remove)
    DropdownMenuItem(
        text = { Text(removeLabel, color = theme.error) },
        onClick = state::requestRemove,
        leadingIcon = {
            Icon(Icons.Default.Delete, contentDescription = removeLabel, tint = theme.error)
        },
        modifier = Modifier.testTag("workspace_action_remove"),
    )
}

@Composable
private fun workspaceActionDialogs(
    state: WorkspaceActionsState,
    subject: WorkspaceActionSubject,
    handlers: WorkspaceActionHandlers,
) {
    when (state.dialog) {
        WorkspaceActionDialog.Rename -> workspaceRenameDialog(state, subject, handlers)
        WorkspaceActionDialog.Remove -> TuiConfirmDialog(
            onDismissRequest = { state.dialog = null },
            onConfirm = handlers.onRemove,
            title = stringResource(R.string.workspace_remove_title),
            message = stringResource(R.string.workspace_remove_message, subject.label.primary),
            confirmText = stringResource(R.string.workspace_action_remove),
            dismissText = stringResource(R.string.button_cancel),
            isDestructive = true,
            modifier = Modifier.testTag("workspace_remove_dialog"),
        )
        null -> Unit
    }
}

@Composable
private fun workspaceRenameDialog(
    state: WorkspaceActionsState,
    subject: WorkspaceActionSubject,
    handlers: WorkspaceActionHandlers,
) {
    var name by remember(subject.label.primary) { mutableStateOf(subject.label.primary) }
    TuiAlertDialog(
        onDismissRequest = { state.dialog = null },
        title = stringResource(R.string.workspace_rename_title),
        modifier = Modifier.testTag("workspace_rename_dialog"),
        confirmButton = { workspaceRenameSaveButton(state, name, subject, handlers) },
        dismissButton = { workspaceRenameDismissButtons(state, subject, handlers) },
    ) {
        workspaceRenameDialogContent(name, { name = it }, subject)
    }
}

@Composable
private fun workspaceRenameSaveButton(
    state: WorkspaceActionsState,
    name: String,
    subject: WorkspaceActionSubject,
    handlers: WorkspaceActionHandlers,
) {
    TuiButton(
        onClick = {
            state.dialog = null
            saveWorkspaceName(name, subject.label.generated, handlers)
        },
        modifier = Modifier.testTag("workspace_rename_save"),
    ) {
        Text(stringResource(R.string.workspace_rename_save))
    }
}

@Composable
private fun workspaceRenameDismissButtons(
    state: WorkspaceActionsState,
    subject: WorkspaceActionSubject,
    handlers: WorkspaceActionHandlers,
) {
    Row(horizontalArrangement = Arrangement.End) {
        if (subject.label.custom != null) {
            TuiTextButton(
                onClick = {
                    state.dialog = null
                    handlers.onResetName()
                },
                modifier = Modifier.testTag("workspace_rename_reset"),
            ) {
                Text(
                    stringResource(R.string.workspace_rename_reset),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        TuiTextButton(
            onClick = { state.dialog = null },
            modifier = Modifier.testTag("workspace_rename_cancel"),
        ) {
            Text(stringResource(R.string.button_cancel))
        }
    }
}

@Composable
private fun workspaceRenameDialogContent(
    name: String,
    onNameChange: (String) -> Unit,
    subject: WorkspaceActionSubject,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.cardContentSpacing),
    ) {
        TuiTextField(
            value = name,
            onValueChange = { if (it.length <= MAX_WORKSPACE_ALIAS_LENGTH) onNameChange(it) },
            modifier = Modifier.testTag("workspace_rename_field"),
            singleLine = true,
            maxLines = 1,
        )
        Text(stringResource(R.string.workspace_rename_automatic_name, subject.label.generated))
        Text(stringResource(R.string.workspace_rename_path, subject.label.path))
    }
}

private fun saveWorkspaceName(name: String, generated: String, handlers: WorkspaceActionHandlers) {
    val customName = name.trim()
    when {
        customName.isEmpty() -> handlers.onRename(null)
        customName == generated -> handlers.onResetName()
        else -> handlers.onRename(customName)
    }
}
