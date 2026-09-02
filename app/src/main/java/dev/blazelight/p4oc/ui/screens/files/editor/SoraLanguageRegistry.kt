package dev.blazelight.p4oc.ui.screens.files.editor

import dev.blazelight.p4oc.R
import dev.blazelight.p4oc.core.filetype.FileTypeClassifier

/**
 * Pure filename → TextMate scope mapping for the curated grammar bundle shipped
 * under `app/src/main/assets/textmate/`.
 *
 * Order of resolution:
 *  1. Special `.env` / `.env.<flavour>` family
 *  2. Exact basename match (e.g. `Dockerfile`, `Cargo.lock`)
 *  3. File extension (lower-cased)
 *
 * Returning `null` is meaningful — the caller falls back to plain text via
 * sora's `EmptyLanguage`. Do **not** add silent fallback chains here; we only
 * report scopes for grammars we actually ship.
 *
 * No Android dependencies on purpose: this stays trivially unit-testable.
 */
internal object SoraLanguageRegistry {
    /**
     * Returns the TextMate scope name for [filename], or `null` if no shipped
     * grammar matches. [filename] may be a bare name or a full path; only the
     * basename is consulted.
     */
    fun scopeFor(filename: String): String? {
        return FileTypeClassifier.classify(filename).textMateScope
    }
}

/**
 * String resource for [scope], used as the file-viewer subtitle. Unknown scopes
 * fall back to plain text; only scopes we actually ship are mapped here.
 */
internal fun displayLabelResForScope(scope: String?): Int = when (scope) {
    "source.kotlin" -> R.string.file_language_kotlin
    "source.json" -> R.string.file_language_json
    "source.python" -> R.string.file_language_python
    "source.ts" -> R.string.file_language_typescript
    "source.yaml" -> R.string.file_language_yaml
    "source.toml" -> R.string.file_language_toml
    "source.shell" -> R.string.file_language_shell
    "source.env" -> R.string.file_language_env
    "text.xml" -> R.string.file_language_xml
    "text.html.markdown" -> R.string.file_language_markdown
    else -> R.string.file_language_plain_text
}
