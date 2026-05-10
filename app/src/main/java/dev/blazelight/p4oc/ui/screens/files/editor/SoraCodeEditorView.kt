package dev.blazelight.p4oc.ui.screens.files.editor

import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import dev.blazelight.p4oc.ui.theme.LocalOpenCodeTheme
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.widget.CodeEditor

/**
 * Hosts a [CodeEditor] inside Compose. The editor is the source of truth for
 * the editing buffer; we surface only debounced text/cursor snapshots to the
 * caller via [onTextChange]/[onSelectionChange].
 *
 * To avoid clobbering Sora's undo history during recomposition, we only call
 * [CodeEditor.setText] when the [contentGeneration] token changes. Bumping the
 * generation is the caller's signal that an external write happened (initial
 * load, server reload, conflict overwrite, discard).
 *
 * Language: a curated TextMate grammar bundle (see
 * `app/src/main/assets/textmate/`) is registered once via
 * [SoraTextMateBootstrap.ensureGrammars]. [filename] is mapped to a scope
 * through [SoraLanguageRegistry]; if the file type is unsupported or the
 * grammar fails to load we fall back to [EmptyLanguage] (plain text). Theme
 * chrome (background, foreground, line numbers, selection, cursor) is applied
 * via [TextMateColorScheme] regardless of language.
 */
@Composable
internal fun SoraCodeEditorView(
    initialContent: String,
    contentGeneration: Int,
    showLineNumbers: Boolean,
    editable: Boolean,
    textSizeSp: Float,
    filename: String,
    modifier: Modifier = Modifier,
    onTextChange: (String) -> Unit,
    onSelectionChange: (line: Int, column: Int) -> Unit = { _, _ -> },
    testTag: String? = null
) {
    val theme = LocalOpenCodeTheme.current
    val onTextChangeState = rememberUpdatedState(onTextChange)
    val onSelectionChangeState = rememberUpdatedState(onSelectionChange)
    val lastAppliedGeneration = remember { intArrayOf(Int.MIN_VALUE) }
    val editorRef = remember { arrayOfNulls<CodeEditor>(1) }
    // Track the last filename we applied a language for so `update{}` only
    // swaps the editor language when the file actually changes.
    val lastAppliedFilename = remember { arrayOfNulls<String>(1) }

    AndroidView(
        modifier = if (testTag != null) modifier.testTag(testTag) else modifier,
        factory = { ctx ->
            // Ensure theme + grammar registries are populated before we create
            // the editor's language. Both are idempotent.
            SoraTextMateBootstrap.applyTheme(theme)
            SoraTextMateBootstrap.ensureGrammars(ctx)
            val editor = CodeEditor(ctx).apply {
                setTypefaceText(Typeface.MONOSPACE)
                setTextSize(textSizeSp)
                setTabWidth(4)
                setLineNumberEnabled(showLineNumbers)
                isEditable = editable
                setEditorLanguage(languageFor(filename))
                applyColorScheme(this)

                subscribeAlways(ContentChangeEvent::class.java) { _ ->
                    onTextChangeState.value(text.toString())
                }
                subscribeAlways(SelectionChangeEvent::class.java) { evt ->
                    val pos = evt.left
                    onSelectionChangeState.value(pos.line, pos.column)
                }
            }
            editor.setText(initialContent)
            lastAppliedGeneration[0] = contentGeneration
            lastAppliedFilename[0] = filename
            editorRef[0] = editor
            editor
        },
        update = { editor ->
            if (editor.isEditable != editable) editor.isEditable = editable
            editor.setLineNumberEnabled(showLineNumbers)
            val targetPx = textSizeSp * editor.resources.displayMetrics.density
            if (Math.abs(editor.textSizePx - targetPx) > 0.5f) {
                editor.setTextSize(textSizeSp)
            }
            if (lastAppliedFilename[0] != filename) {
                editor.setEditorLanguage(languageFor(filename))
                lastAppliedFilename[0] = filename
            }
            if (lastAppliedGeneration[0] != contentGeneration) {
                editor.setText(initialContent)
                lastAppliedGeneration[0] = contentGeneration
            }
        },
        onRelease = { editor ->
            // Critical: prevents native renderer + buffer leak. Public API only
            // (LGPL §6 unaffected).
            editor.release()
            editorRef[0] = null
        }
    )

    // Reapply palette + push fresh color scheme onto the live editor whenever
    // LocalOpenCodeTheme changes. ThemeRegistry mutation alone does not retint
    // an existing CodeEditor; we must call setColorScheme again.
    LaunchedEffect(theme) {
        SoraTextMateBootstrap.applyTheme(theme)
        editorRef[0]?.let { applyColorScheme(it) }
    }
}

private fun applyColorScheme(editor: CodeEditor) {
    runCatching {
        editor.colorScheme = TextMateColorScheme.create(ThemeRegistry.getInstance())
    }
}

/**
 * Resolves [filename] to a sora [Language]: a [TextMateLanguage] when a scope
 * is mapped *and* the grammar registers cleanly, otherwise [EmptyLanguage] so
 * the editor still renders plain text without throwing.
 */
private fun languageFor(filename: String): Language {
    val scope = SoraLanguageRegistry.scopeFor(filename) ?: return EmptyLanguage()
    return runCatching { TextMateLanguage.create(scope, /* autoComplete = */ false) }
        .getOrElse { EmptyLanguage() }
}
