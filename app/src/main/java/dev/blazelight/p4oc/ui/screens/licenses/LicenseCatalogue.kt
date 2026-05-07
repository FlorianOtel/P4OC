package dev.blazelight.p4oc.ui.screens.licenses

/**
 * Hand-curated catalogue of third-party components shipped in the app, plus
 * planned LGPL/GPL integrations that require attribution before they land.
 *
 * Update when bumping versions in `gradle/libs.versions.toml` or adding/removing
 * dependencies in `app/build.gradle.kts`.
 *
 * Grouping convention: copyleft (LGPL/GPL) first, then permissive components
 * grouped by ecosystem. Versions match `libs.versions.toml` at time of update.
 */
internal object LicenseCatalogue {

    // Versions mirrored from gradle/libs.versions.toml. Keep this in sync on
    // every dependency bump in the version catalogue.
    private const val TERMUX = "0.118.0"
    private const val KOTLIN_STDLIB = "2.3.0"
    private const val KOTLINX_SERIALIZATION = "1.10.0"
    private const val KOTLINX_DATETIME = "0.7.1"
    private const val COROUTINES = "1.10.2"
    private const val COMPOSE_BOM = "2026.01.01"
    private const val ACTIVITY_COMPOSE = "1.12.3"
    private const val APPCOMPAT = "1.7.1"
    private const val CORE_KTX = "1.17.0"
    private const val LIFECYCLE = "2.10.0"
    private const val NAVIGATION = "2.9.7"
    private const val DATASTORE = "1.2.0"
    private const val SECURITY_CRYPTO = "1.1.0-alpha06"
    private const val SPLASHSCREEN = "1.2.0"
    private const val CONCURRENT_FUTURES = "1.3.0"
    private const val OKHTTP = "5.3.2"
    private const val RETROFIT = "3.0.0"
    private const val EVENTSOURCE = "4.2.0"
    private const val JAVA_DIFF_UTILS = "4.15"
    private const val KOIN = "4.1.1"
    private const val MARKDOWN_RENDERER = "0.39.2"
    private const val COIL = "2.7.0"

    // Shipped LGPL-2.1 dependency (see oa-lmh0, oa-v3js).
    private const val SORA_EDITOR = "0.23.6"

    val all: List<LicenseEntry> = listOf(
        // ----- Copyleft (LGPL/GPL) -----

        LicenseEntry(
            name = "Sora Editor (sora-editor)",
            version = SORA_EDITOR,
            license = License.LGPL_2_1,
            upstreamUrl = "https://github.com/Rosemoe/sora-editor",
            notes = "Hosts the in-app file editor. Consumed as an unmodified Maven " +
                "artifact (version $SORA_EDITOR, modules editor and language-textmate) " +
                "and used solely through its public APIs (CodeEditor, ThemeRegistry, " +
                "EditorColorScheme, TextMateLanguage, subscribeEvent), dynamically " +
                "linked under LGPL-2.1. You have the right under LGPL-2.1 §6 to replace " +
                "it with a modified version; the unmodified upstream source is available " +
                "at the URL above, and the project's issue tracker (see the relinking " +
                "notice on this screen) is the channel for requesting object code " +
                "suitable for relinking against a modified Sora Editor build."
        ),
        LicenseEntry(
            name = "Sora Editor language-textmate",
            version = SORA_EDITOR,
            license = License.LGPL_2_1,
            upstreamUrl = "https://github.com/Rosemoe/sora-editor",
            notes = "TextMate grammar/theme module distributed as part of sora-editor. " +
                "Same usage and relinking terms apply as the Sora Editor entry above."
        ),
        LicenseEntry(
            name = "Termux terminal-view",
            version = TERMUX,
            license = License.GPL_3_0,
            upstreamUrl = "https://github.com/termux/termux-app",
            notes = "Used to render the in-app terminal. Distributed under GPL-3.0. " +
                "Unmodified upstream source is available at the URL above."
        ),
        LicenseEntry(
            name = "Termux terminal-emulator",
            version = TERMUX,
            license = License.GPL_3_0,
            upstreamUrl = "https://github.com/termux/termux-app",
            notes = "Used as the in-app terminal emulator core. Distributed under " +
                "GPL-3.0. Unmodified upstream source is available at the URL above."
        ),

        // ----- Kotlin / kotlinx -----

        LicenseEntry(
            name = "Kotlin Standard Library",
            version = KOTLIN_STDLIB,
            license = License.APACHE_2_0,
            upstreamUrl = "https://github.com/JetBrains/kotlin",
        ),
        LicenseEntry(
            name = "kotlinx.serialization (json)",
            version = KOTLINX_SERIALIZATION,
            license = License.APACHE_2_0,
            upstreamUrl = "https://github.com/Kotlin/kotlinx.serialization",
        ),
        LicenseEntry(
            name = "kotlinx-datetime",
            version = KOTLINX_DATETIME,
            license = License.APACHE_2_0,
            upstreamUrl = "https://github.com/Kotlin/kotlinx-datetime",
        ),
        LicenseEntry(
            name = "kotlinx.coroutines (test)",
            version = COROUTINES,
            license = License.APACHE_2_0,
            upstreamUrl = "https://github.com/Kotlin/kotlinx.coroutines",
        ),

        // ----- Jetpack Compose / AndroidX -----

        LicenseEntry(
            name = "Jetpack Compose (BOM)",
            version = COMPOSE_BOM,
            license = License.APACHE_2_0,
            upstreamUrl = "https://developer.android.com/jetpack/androidx/releases/compose",
            notes = "Includes compose-ui, compose-foundation, compose-material3, and " +
                "material-icons-extended pulled in transitively from the BOM."
        ),
        LicenseEntry(
            name = "AndroidX Activity Compose",
            version = ACTIVITY_COMPOSE,
            license = License.APACHE_2_0,
            upstreamUrl = "https://developer.android.com/jetpack/androidx/releases/activity",
        ),
        LicenseEntry(
            name = "AndroidX AppCompat",
            version = APPCOMPAT,
            license = License.APACHE_2_0,
            upstreamUrl = "https://developer.android.com/jetpack/androidx/releases/appcompat",
        ),
        LicenseEntry(
            name = "AndroidX Core KTX",
            version = CORE_KTX,
            license = License.APACHE_2_0,
            upstreamUrl = "https://developer.android.com/jetpack/androidx/releases/core",
        ),
        LicenseEntry(
            name = "AndroidX Lifecycle",
            version = LIFECYCLE,
            license = License.APACHE_2_0,
            upstreamUrl = "https://developer.android.com/jetpack/androidx/releases/lifecycle",
        ),
        LicenseEntry(
            name = "AndroidX Navigation Compose",
            version = NAVIGATION,
            license = License.APACHE_2_0,
            upstreamUrl = "https://developer.android.com/jetpack/androidx/releases/navigation",
        ),
        LicenseEntry(
            name = "AndroidX DataStore Preferences",
            version = DATASTORE,
            license = License.APACHE_2_0,
            upstreamUrl = "https://developer.android.com/jetpack/androidx/releases/datastore",
        ),
        LicenseEntry(
            name = "AndroidX Security Crypto",
            version = SECURITY_CRYPTO,
            license = License.APACHE_2_0,
            upstreamUrl = "https://developer.android.com/jetpack/androidx/releases/security",
        ),
        LicenseEntry(
            name = "AndroidX Core SplashScreen",
            version = SPLASHSCREEN,
            license = License.APACHE_2_0,
            upstreamUrl = "https://developer.android.com/jetpack/androidx/releases/core",
        ),
        LicenseEntry(
            name = "AndroidX Concurrent Futures",
            version = CONCURRENT_FUTURES,
            license = License.APACHE_2_0,
            upstreamUrl = "https://developer.android.com/jetpack/androidx/releases/concurrent",
        ),

        // ----- Networking -----

        LicenseEntry(
            name = "OkHttp",
            version = OKHTTP,
            license = License.APACHE_2_0,
            upstreamUrl = "https://github.com/square/okhttp",
            notes = "Includes okhttp and the logging-interceptor module."
        ),
        LicenseEntry(
            name = "Retrofit",
            version = RETROFIT,
            license = License.APACHE_2_0,
            upstreamUrl = "https://github.com/square/retrofit",
            notes = "Includes retrofit and converter-kotlinx-serialization."
        ),
        LicenseEntry(
            name = "LaunchDarkly okhttp-eventsource",
            version = EVENTSOURCE,
            license = License.APACHE_2_0,
            upstreamUrl = "https://github.com/launchdarkly/okhttp-eventsource",
        ),

        // ----- Other -----

        LicenseEntry(
            name = "java-diff-utils",
            version = JAVA_DIFF_UTILS,
            license = License.APACHE_2_0,
            upstreamUrl = "https://github.com/java-diff-utils/java-diff-utils",
        ),
        LicenseEntry(
            name = "Koin",
            version = KOIN,
            license = License.APACHE_2_0,
            upstreamUrl = "https://github.com/InsertKoinIO/koin",
            notes = "Includes koin-android, koin-androidx-compose, and " +
                "koin-androidx-compose-navigation."
        ),
        LicenseEntry(
            name = "mikepenz multiplatform-markdown-renderer",
            version = MARKDOWN_RENDERER,
            license = License.APACHE_2_0,
            upstreamUrl = "https://github.com/mikepenz/multiplatform-markdown-renderer",
            notes = "Includes the android, m3, and code modules."
        ),
        LicenseEntry(
            name = "Coil",
            version = COIL,
            license = License.APACHE_2_0,
            upstreamUrl = "https://github.com/coil-kt/coil",
        ),
    )
}
