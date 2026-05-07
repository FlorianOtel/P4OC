package dev.blazelight.p4oc.ui.screens.files.editor

import dev.blazelight.p4oc.ui.theme.opencode.OpenCodeTheme
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import org.eclipse.tm4e.core.registry.IThemeSource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import java.util.concurrent.atomic.AtomicReference

/**
 * Builds an in-memory TextMate theme JSON from the active [OpenCodeTheme] and
 * registers it with Sora's [ThemeRegistry]. Uses only the public API surface
 * (LGPL §6 compliance — no fork/modify of sora-editor).
 *
 * Grammar registration is intentionally minimal: we register no .tmLanguage
 * files, so [io.github.rosemoe.sora.langs.textmate.TextMateLanguage] will
 * fall back to plain text. This gives us palette-mapped chrome (background,
 * foreground, line numbers, selection, current line) immediately without
 * shipping ~MBs of grammar assets. A follow-up ticket can add a curated
 * grammar bundle.
 */
internal object SoraTextMateBootstrap {

    private val activeThemeName = AtomicReference<String?>(null)

    /**
     * Idempotently registers a theme derived from [theme] and marks it as the
     * current ThemeRegistry theme. Returns the registered theme name (suitable
     * for diagnostics; the [io.github.rosemoe.sora.langs.textmate.TextMateColorScheme]
     * already reads the current theme from the registry).
     */
    @Synchronized
    fun applyTheme(theme: OpenCodeTheme): String {
        val themeName = "opencode-${if (theme.isDark) "dark" else "light"}-${theme.name.hashCode()}"
        val registry = ThemeRegistry.getInstance()
        val json = buildThemeJson(themeName, theme)
        val source = IThemeSource.fromString(IThemeSource.ContentType.JSON, json)
        val model = ThemeModel(source, themeName).apply { setDark(theme.isDark) }
        try {
            registry.loadTheme(model)
            registry.setTheme(themeName)
        } catch (t: Throwable) {
            // Fall back silently — theme registry will keep its previous state
            // and the editor renders with default EditorColorScheme.
            return activeThemeName.get() ?: ""
        }
        activeThemeName.set(themeName)
        return themeName
    }

    private fun hex(color: Color): String {
        val argb = color.toArgb()
        // TextMate themes use #RRGGBB or #RRGGBBAA
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        val a = (argb shr 24) and 0xFF
        return if (a == 0xFF) {
            "#%02X%02X%02X".format(r, g, b)
        } else {
            "#%02X%02X%02X%02X".format(r, g, b, a)
        }
    }

    private fun buildThemeJson(name: String, t: OpenCodeTheme): String {
        // Minimal TextMate theme: editor chrome via "colors", token rules via "tokenColors".
        // Scope names match common TextMate grammars.
        return """
{
  "name": "$name",
  "type": "${if (t.isDark) "dark" else "light"}",
  "colors": {
    "editor.background": "${hex(t.background)}",
    "editor.foreground": "${hex(t.text)}",
    "editorLineNumber.foreground": "${hex(t.textMuted)}",
    "editorLineNumber.activeForeground": "${hex(t.text)}",
    "editor.selectionBackground": "${hex(t.accent.copy(alpha = 0.30f))}",
    "editor.lineHighlightBackground": "${hex(t.backgroundElement)}",
    "editorCursor.foreground": "${hex(t.accent)}",
    "editorIndentGuide.background": "${hex(t.borderSubtle)}",
    "editorWhitespace.foreground": "${hex(t.borderSubtle)}"
  },
  "tokenColors": [
    { "scope": ["comment", "punctuation.definition.comment"], "settings": { "foreground": "${hex(t.syntaxComment)}" } },
    { "scope": ["keyword", "storage", "storage.type", "storage.modifier"], "settings": { "foreground": "${hex(t.syntaxKeyword)}" } },
    { "scope": ["entity.name.function", "support.function", "meta.function-call"], "settings": { "foreground": "${hex(t.syntaxFunction)}" } },
    { "scope": ["variable", "variable.other", "variable.parameter"], "settings": { "foreground": "${hex(t.syntaxVariable)}" } },
    { "scope": ["string", "string.quoted"], "settings": { "foreground": "${hex(t.syntaxString)}" } },
    { "scope": ["constant.numeric", "constant.language"], "settings": { "foreground": "${hex(t.syntaxNumber)}" } },
    { "scope": ["entity.name.type", "entity.name.class", "support.type", "support.class"], "settings": { "foreground": "${hex(t.syntaxType)}" } },
    { "scope": ["keyword.operator"], "settings": { "foreground": "${hex(t.syntaxOperator)}" } },
    { "scope": ["punctuation"], "settings": { "foreground": "${hex(t.syntaxPunctuation)}" } }
  ]
}
        """.trimIndent()
    }

}
