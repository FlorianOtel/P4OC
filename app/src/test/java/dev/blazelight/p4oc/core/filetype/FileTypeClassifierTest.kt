package dev.blazelight.p4oc.core.filetype

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FileTypeClassifierTest {
    @Test
    fun `classifies representative categories`() {
        val cases = mapOf(
            "Main.kt" to FileTypeCategory.Code,
            "settings.json" to FileTypeCategory.Config,
            "README.md" to FileTypeCategory.Document,
            "logo.png" to FileTypeCategory.Image,
            "backup.zip" to FileTypeCategory.Archive,
            "deploy.sh" to FileTypeCategory.Shell,
            "build.gradle" to FileTypeCategory.Build,
            ".gitignore" to FileTypeCategory.Git,
            "Cargo.lock" to FileTypeCategory.Lock,
            ".env.local" to FileTypeCategory.Env,
            "index.html" to FileTypeCategory.Web,
            "schema.sql" to FileTypeCategory.Database,
        )

        cases.forEach { (filename, category) ->
            assertEquals("category for $filename", category, FileTypeClassifier.classify(filename).category)
        }
    }

    @Test
    fun `preserves shipped textmate scopes`() {
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
            "Dockerfile" to "source.shell",
            "Cargo.lock" to "source.toml",
            ".env.production" to "source.env",
        )

        cases.forEach { (filename, scope) ->
            assertEquals("scope for $filename", scope, FileTypeClassifier.classify(filename).textMateScope)
        }
    }

    @Test
    fun `paths are reduced to basenames and extensions are case insensitive`() {
        assertEquals(FileTypeCategory.Code, FileTypeClassifier.classify("/foo/bar/App.kt").category)
        assertEquals("source.kotlin", FileTypeClassifier.classify("C:\\projects\\app\\Main.KTS").textMateScope)
        assertEquals(FileTypeCategory.Image, FileTypeClassifier.classify("assets/LOGO.PNG").category)
    }

    @Test
    fun `unknown and extensionless files have no scope`() {
        val cases = listOf("", "noextension", "/some/dir/", "photo.unknown", ".bashrc", "weird.")

        cases.forEach { filename ->
            val metadata = FileTypeClassifier.classify(filename)
            assertEquals("category for $filename", FileTypeCategory.Unknown, metadata.category)
            assertNull("scope for $filename", metadata.textMateScope)
        }
    }
}
