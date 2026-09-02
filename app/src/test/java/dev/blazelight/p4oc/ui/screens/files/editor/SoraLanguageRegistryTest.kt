package dev.blazelight.p4oc.ui.screens.files.editor

import dev.blazelight.p4oc.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pure JVM unit tests for [SoraLanguageRegistry].
 *
 * No Android, no Robolectric — the mapper is intentionally a plain Kotlin
 * object so we can exercise it cheaply.
 */
class SoraLanguageRegistryTest {

    @Test
    fun `extension based mappings`() {
        val cases = mapOf(
            "App.kt" to "source.kotlin",
            "build.gradle.kts" to "source.kotlin",
            "settings.json" to "source.json",
            "tsconfig.jsonc" to "source.json",
            "data.json5" to "source.json",
            "README.md" to "text.html.markdown",
            "NOTES.markdown" to "text.html.markdown",
            "config.yml" to "source.yaml",
            "ci.yaml" to "source.yaml",
            "pyproject.toml" to "source.toml",
            "AndroidManifest.xml" to "text.xml",
            "deploy.sh" to "source.shell",
            "run.bash" to "source.shell",
            "rc.zsh" to "source.shell",
            "index.ts" to "source.ts",
            "App.tsx" to "source.ts",
            "main.js" to "source.ts",
            "module.mjs" to "source.ts",
            "legacy.cjs" to "source.ts",
            "Component.jsx" to "source.ts",
            "main.py" to "source.python",
            "stub.pyi" to "source.python",
        )
        cases.forEach { (filename, expected) ->
            assertEquals("scope for $filename", expected, SoraLanguageRegistry.scopeFor(filename))
        }
    }

    @Test
    fun `dotenv family`() {
        assertEquals("source.env", SoraLanguageRegistry.scopeFor(".env"))
        assertEquals("source.env", SoraLanguageRegistry.scopeFor(".env.local"))
        assertEquals("source.env", SoraLanguageRegistry.scopeFor(".env.production"))
        assertEquals(
            "source.env",
            SoraLanguageRegistry.scopeFor("/repo/services/api/.env.staging"),
        )
    }

    @Test
    fun `basename specials`() {
        assertEquals("source.shell", SoraLanguageRegistry.scopeFor("Dockerfile"))
        assertEquals("source.shell", SoraLanguageRegistry.scopeFor("/tmp/work/Dockerfile"))
        assertEquals("source.toml", SoraLanguageRegistry.scopeFor("Cargo.lock"))
    }

    @Test
    fun `paths are reduced to basenames`() {
        assertEquals("source.kotlin", SoraLanguageRegistry.scopeFor("/foo/bar/App.kt"))
        assertEquals(
            "source.kotlin",
            SoraLanguageRegistry.scopeFor("C:\\projects\\app\\Main.kt"),
        )
    }

    @Test
    fun `extension matching is case insensitive`() {
        assertEquals("source.python", SoraLanguageRegistry.scopeFor("Main.PY"))
        assertEquals("source.kotlin", SoraLanguageRegistry.scopeFor("Foo.KTS"))
        assertEquals("text.xml", SoraLanguageRegistry.scopeFor("layout.XML"))
    }

    @Test
    fun `display labels map known scopes to language resources`() {
        val cases = mapOf(
            "source.kotlin" to R.string.file_language_kotlin,
            "text.html.markdown" to R.string.file_language_markdown,
        )

        cases.forEach { (scope, expected) ->
            assertEquals("label resource for $scope", expected, displayLabelResForScope(scope))
        }
    }

    @Test
    fun `display labels fall back to plain text for null and unknown scopes`() {
        assertEquals(R.string.file_language_plain_text, displayLabelResForScope(null))
        assertEquals(
            R.string.file_language_plain_text,
            displayLabelResForScope("source.language-we-do-not-ship"),
        )
    }

    @Test
    fun `unknown extensions return null`() {
        assertNull(SoraLanguageRegistry.scopeFor("photo.jpg"))
        assertNull(SoraLanguageRegistry.scopeFor("data.bin"))
        assertNull(SoraLanguageRegistry.scopeFor("archive.tar.gz"))
    }

    @Test
    fun `dotfiles without inner extension return null`() {
        assertNull(SoraLanguageRegistry.scopeFor(".gitignore"))
        assertNull(SoraLanguageRegistry.scopeFor(".bashrc"))
        // A trailing dot is not a real extension.
        assertNull(SoraLanguageRegistry.scopeFor("weird."))
    }

    @Test
    fun `empty and unknown inputs`() {
        assertNull(SoraLanguageRegistry.scopeFor(""))
        assertNull(SoraLanguageRegistry.scopeFor("noextension"))
        assertNull(SoraLanguageRegistry.scopeFor("/some/dir/"))
    }
}
