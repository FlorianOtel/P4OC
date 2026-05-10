package dev.blazelight.p4oc.ui.screens.files.editor

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
 * Human-readable label for [scope], used as the file-viewer subtitle. Returns
 * `"plain text"` for null/unknown scopes; only scopes we actually ship are
 * mapped here.
 */
internal fun displayLabelForScope(scope: String?): String = when (scope) {
    "source.kotlin" -> "kotlin"
    "source.json" -> "json"
    "source.python" -> "python"
    "source.ts" -> "typescript"
    "source.yaml" -> "yaml"
    "source.toml" -> "toml"
    "source.shell" -> "shell"
    "source.env" -> "env"
    "text.xml" -> "xml"
    "text.html.markdown" -> "markdown"
    else -> "plain text"
}
