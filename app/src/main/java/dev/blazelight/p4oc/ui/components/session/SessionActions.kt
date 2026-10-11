package dev.blazelight.p4oc.ui.components.session

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Summarize
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
import dev.blazelight.p4oc.R
import dev.blazelight.p4oc.ui.components.TuiConfirmDialog
import dev.blazelight.p4oc.ui.components.TuiDropdownMenuItem
import dev.blazelight.p4oc.ui.components.TuiInputDialog
import dev.blazelight.p4oc.ui.theme.LocalOpenCodeTheme

/** What the shared session action menu needs to know about one session row. */
data class SessionActionSubject(
    val sessionId: String,
    val title: String,
    val isShared: Boolean,
    val supportsSharing: Boolean,
    val canFork: Boolean,
)

/**
 * Final session actions. Rename and delete are only invoked after the shared rename/delete
 * dialogs confirm; the host decides which repository and workspace the action runs against.
 */
class SessionActionHandlers(
    val onRename: (newTitle: String) -> Unit,
    val onDelete: () -> Unit,
    val onFork: () -> Unit,
    val onToggleShare: () -> Unit,
    val onViewChanges: () -> Unit,
    val onSummarize: () -> Unit,
)

internal enum class SessionActionDialog { Rename, Delete }

/** Per-row menu and dialog visibility for [sessionActionGestures] and [SessionActionMenu]. */
@Stable
class SessionActionsState {
    var isMenuOpen: Boolean by mutableStateOf(false)
    internal var dialog: SessionActionDialog? by mutableStateOf(null)

    fun requestRename() {
        isMenuOpen = false
        dialog = SessionActionDialog.Rename
    }

    fun requestDelete() {
        isMenuOpen = false
        dialog = SessionActionDialog.Delete
    }
}

/**
 * [subjectKey] identifies the session; when a row slot is reused for a different session (list
 * reorders), the menu and any pending rename/delete dialog are dropped instead of retargeted.
 */
@Composable
fun rememberSessionActionsState(subjectKey: Any): SessionActionsState =
    remember(subjectKey) { SessionActionsState() }

/**
 * Tap runs [onClick]; long-press gives haptic feedback and opens the shared session action menu.
 * Accessibility services get every menu action as a custom action on the row.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.sessionActionGestures(
    state: SessionActionsState,
    subject: SessionActionSubject,
    handlers: SessionActionHandlers,
    onClick: () -> Unit,
): Modifier {
    val haptic = LocalHapticFeedback.current
    val openActionsLabel = stringResource(R.string.sessions_open_actions)
    val accessibilityActions = sessionAccessibilityActions(state, subject, handlers)
    return this
        .combinedClickable(
            onClick = onClick,
            onLongClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                state.isMenuOpen = true
            },
            onLongClickLabel = openActionsLabel,
            role = Role.Button,
        )
        .semantics { customActions = accessibilityActions }
}

@Composable
private fun sessionAccessibilityActions(
    state: SessionActionsState,
    subject: SessionActionSubject,
    handlers: SessionActionHandlers,
): List<CustomAccessibilityAction> {
    val renameLabel = stringResource(R.string.sessions_rename)
    val forkLabel = stringResource(R.string.sessions_fork)
    val viewChangesLabel = stringResource(R.string.sessions_view_changes)
    val summarizeLabel = stringResource(R.string.sessions_summarize)
    val shareLabel = stringResource(if (subject.isShared) R.string.sessions_unshare else R.string.sessions_share)
    val deleteLabel = stringResource(R.string.sessions_delete)
    return buildList {
        add(accessibilityAction(renameLabel) { state.requestRename() })
        if (subject.canFork) add(accessibilityAction(forkLabel, handlers.onFork))
        add(accessibilityAction(viewChangesLabel, handlers.onViewChanges))
        add(accessibilityAction(summarizeLabel, handlers.onSummarize))
        if (subject.supportsSharing) add(accessibilityAction(shareLabel, handlers.onToggleShare))
        add(accessibilityAction(deleteLabel) { state.requestDelete() })
    }
}

private fun accessibilityAction(label: String, action: () -> Unit) = CustomAccessibilityAction(label) {
    action()
    true
}

/**
 * Long-press session menu plus its rename and delete-confirmation dialogs. Place it next to the
 * row content inside the same parent so the dropdown anchors to the row.
 */
@Composable
@Suppress("FunctionNaming", "LongMethod")
fun SessionActionMenu(
    state: SessionActionsState,
    subject: SessionActionSubject,
    handlers: SessionActionHandlers,
) {
    val theme = LocalOpenCodeTheme.current
    val runAndClose: (() -> Unit) -> Unit = { action ->
        state.isMenuOpen = false
        action()
    }
    DropdownMenu(
        expanded = state.isMenuOpen,
        onDismissRequest = { state.isMenuOpen = false },
        modifier = Modifier.testTag("session_actions_menu_${subject.sessionId}"),
    ) {
        TuiDropdownMenuItem(
            text = stringResource(R.string.sessions_rename),
            onClick = state::requestRename,
            leadingIcon = Icons.Default.Edit,
            modifier = Modifier.testTag("session_action_rename"),
        )
        TuiDropdownMenuItem(
            text = stringResource(R.string.sessions_fork),
            onClick = { runAndClose(handlers.onFork) },
            leadingIcon = Icons.Default.ContentCopy,
            enabled = subject.canFork,
            modifier = Modifier.testTag("session_action_fork"),
        )
        TuiDropdownMenuItem(
            text = stringResource(R.string.sessions_view_changes),
            onClick = { runAndClose(handlers.onViewChanges) },
            leadingIcon = Icons.Default.Description,
            modifier = Modifier.testTag("session_action_view_changes"),
        )
        TuiDropdownMenuItem(
            text = stringResource(R.string.sessions_summarize),
            onClick = { runAndClose(handlers.onSummarize) },
            leadingIcon = Icons.Default.Summarize,
            modifier = Modifier.testTag("session_action_summarize"),
        )
        if (subject.supportsSharing) {
            TuiDropdownMenuItem(
                text = stringResource(if (subject.isShared) R.string.sessions_unshare else R.string.sessions_share),
                onClick = { runAndClose(handlers.onToggleShare) },
                leadingIcon = if (subject.isShared) Icons.Default.LinkOff else Icons.Default.Share,
                modifier = Modifier.testTag("session_action_share"),
            )
        }
        HorizontalDivider(color = theme.borderSubtle)
        DropdownMenuItem(
            text = { Text(stringResource(R.string.sessions_delete), color = theme.error) },
            onClick = state::requestDelete,
            leadingIcon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.sessions_delete),
                    tint = theme.error,
                )
            },
            modifier = Modifier.testTag("session_action_delete"),
        )
    }
    SessionActionDialogs(state, subject, handlers)
}

@Composable
@Suppress("FunctionNaming")
private fun SessionActionDialogs(
    state: SessionActionsState,
    subject: SessionActionSubject,
    handlers: SessionActionHandlers,
) {
    when (state.dialog) {
        SessionActionDialog.Delete -> TuiConfirmDialog(
            onDismissRequest = { state.dialog = null },
            onConfirm = handlers.onDelete,
            title = stringResource(R.string.sessions_delete_title),
            message = stringResource(R.string.sessions_delete_confirm, subject.title),
            confirmText = stringResource(R.string.sessions_delete),
            dismissText = stringResource(R.string.button_cancel),
            isDestructive = true,
            modifier = Modifier.testTag("session_delete_dialog"),
        )
        SessionActionDialog.Rename -> TuiInputDialog(
            onDismissRequest = { state.dialog = null },
            onConfirm = handlers.onRename,
            title = stringResource(R.string.sessions_rename_title),
            initialValue = subject.title,
            label = stringResource(R.string.sessions_title_optional),
            confirmText = stringResource(R.string.sessions_rename),
            dismissText = stringResource(R.string.button_cancel),
            modifier = Modifier.testTag("session_rename_dialog"),
        )
        null -> Unit
    }
}
