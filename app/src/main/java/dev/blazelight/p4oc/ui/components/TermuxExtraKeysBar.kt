package dev.blazelight.p4oc.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import dev.blazelight.p4oc.ui.theme.SemanticColors
import dev.blazelight.p4oc.ui.theme.Sizing
import dev.blazelight.p4oc.ui.theme.TuiCodeFontSize
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Terminal extra keys bar in Termux style.
 * Two-row grid layout with transparent buttons and Unicode symbols.
 *
 * Default layout matches Termux:
 * Row 1: ESC  /  ―  HOME  ↑  END  PGUP
 * Row 2: TAB  CTRL ALT  ←  ↓  →  PGDN
 */
@Composable
fun TermuxExtraKeysBar(
    onKeyPress: (String) -> Unit,
    ctrlActive: Boolean,
    altActive: Boolean,
    onCtrlToggle: () -> Unit,
    onAltToggle: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onPaste: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SemanticColors.TerminalKeys.background)
    ) {
        // Row 1: ESC  /  ―  HOME  ↑  END  PGUP
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(Sizing.minTouchTarget)
                .horizontalScroll(rememberScrollState())
        ) {
            ExtraKey("ESC", "\u001B", enabled, onKeyPress, Modifier.width(Sizing.minTouchTarget))
            ExtraKey("/", "/", enabled, onKeyPress, Modifier.width(Sizing.minTouchTarget))
            ExtraKey("―", "-", enabled, onKeyPress, Modifier.width(Sizing.minTouchTarget))
            ExtraKey("HOME", "\u001B[H", enabled, onKeyPress, Modifier.width(Sizing.minTouchTarget))
            RepeatableExtraKey("↑", "\u001B[A", enabled, onKeyPress, Modifier.width(Sizing.minTouchTarget))
            ExtraKey("END", "\u001B[F", enabled, onKeyPress, Modifier.width(Sizing.minTouchTarget))
            ExtraKey("PGUP", "\u001B[5~", enabled, onKeyPress, Modifier.width(Sizing.minTouchTarget))
            ActionExtraKey("PST", enabled && onPaste != null, onPaste ?: {}, Modifier.width(Sizing.minTouchTarget))
        }

        // Row 2: TAB  CTRL  ALT  ←  ↓  →  PGDN
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(Sizing.minTouchTarget)
                .horizontalScroll(rememberScrollState())
        ) {
            ExtraKey("↹", "\t", enabled, onKeyPress, Modifier.width(Sizing.minTouchTarget))
            ModifierKey("CTRL", ctrlActive, enabled, onCtrlToggle, Modifier.width(Sizing.minTouchTarget))
            ModifierKey("ALT", altActive, enabled, onAltToggle, Modifier.width(Sizing.minTouchTarget))
            RepeatableExtraKey("←", "\u001B[D", enabled, onKeyPress, Modifier.width(Sizing.minTouchTarget))
            RepeatableExtraKey("↓", "\u001B[B", enabled, onKeyPress, Modifier.width(Sizing.minTouchTarget))
            RepeatableExtraKey("→", "\u001B[C", enabled, onKeyPress, Modifier.width(Sizing.minTouchTarget))
            ExtraKey("PGDN", "\u001B[6~", enabled, onKeyPress, Modifier.width(Sizing.minTouchTarget))
        }
    }
}

/**
 * Single extra key button (non-repeating).
 */
@Composable
private fun ExtraKey(
    label: String,
    sequence: String,
    enabled: Boolean,
    onKeyPress: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .semantics {
                role = Role.Button
                if (!enabled) disabled()
                onClick(label) {
                    if (enabled) onKeyPress(sequence)
                    enabled
                }
            }
            .background(
                color = if (isPressed) SemanticColors.TerminalKeys.keyPressed else Color.Transparent,
                shape = RectangleShape
            )
            .pointerInput(enabled, sequence) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = {
                        onKeyPress(sequence)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (enabled) {
                SemanticColors.TerminalKeys.keyText
            } else {
                SemanticColors.TerminalKeys.keyText.copy(
                    alpha = 0.5f
                )
            },
            fontSize = TuiCodeFontSize.md,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ActionExtraKey(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .semantics {
                role = Role.Button
                if (!enabled) disabled()
                onClick(label) {
                    if (enabled) onClick()
                    enabled
                }
            }
            .background(
                color = if (isPressed) SemanticColors.TerminalKeys.keyPressed else Color.Transparent,
                shape = RectangleShape
            )
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (enabled) {
                SemanticColors.TerminalKeys.keyText
            } else {
                SemanticColors.TerminalKeys.keyText.copy(alpha = 0.5f)
            },
            fontSize = TuiCodeFontSize.md,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Repeatable extra key button (long-press triggers repeat).
 * Matches Termux behavior: 400ms initial delay, then 80ms repeat interval.
 */
@Composable
private fun RepeatableExtraKey(
    label: String,
    sequence: String,
    enabled: Boolean,
    onKeyPress: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var repeatJob by remember { mutableStateOf<Job?>(null) }

    Box(
        modifier = modifier
            .semantics {
                role = Role.Button
                if (!enabled) disabled()
                onClick(label) {
                    if (enabled) onKeyPress(sequence)
                    enabled
                }
            }
            .background(
                color = if (isPressed) SemanticColors.TerminalKeys.keyPressed else Color.Transparent,
                shape = RectangleShape
            )
            .pointerInput(enabled, sequence) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        isPressed = true

                        // Start repeat job
                        repeatJob = scope.launch {
                            onKeyPress(sequence) // First press
                            delay(400) // Initial delay (Termux: FALLBACK_LONG_PRESS_DURATION)
                            while (true) {
                                onKeyPress(sequence)
                                delay(80) // Repeat delay (Termux: DEFAULT_LONG_PRESS_REPEAT_DELAY)
                            }
                        }

                        tryAwaitRelease()

                        // Cancel repeat on release
                        repeatJob?.cancel()
                        repeatJob = null
                        isPressed = false
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (enabled) {
                SemanticColors.TerminalKeys.keyText
            } else {
                SemanticColors.TerminalKeys.keyText.copy(
                    alpha = 0.5f
                )
            },
            fontSize = TuiCodeFontSize.md,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Modifier key button (CTRL/ALT) with active state.
 * Active state shows in text color, background stays transparent.
 */
@Composable
private fun ModifierKey(
    label: String,
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .semantics {
                role = Role.Checkbox
                toggleableState = ToggleableState(active)
                if (!enabled) disabled()
                onClick(label) {
                    if (enabled) onClick()
                    enabled
                }
            }
            .background(
                color = if (isPressed) SemanticColors.TerminalKeys.keyPressed else Color.Transparent,
                shape = RectangleShape
            )
            .pointerInput(enabled, active) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = {
                        onClick()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = when {
                !enabled -> SemanticColors.TerminalKeys.keyText.copy(alpha = 0.5f)
                active -> SemanticColors.TerminalKeys.activeModifier
                else -> SemanticColors.TerminalKeys.keyText
            },
            fontSize = TuiCodeFontSize.md,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center
        )
    }
}
