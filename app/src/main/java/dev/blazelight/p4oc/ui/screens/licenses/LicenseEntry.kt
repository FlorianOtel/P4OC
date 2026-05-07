package dev.blazelight.p4oc.ui.screens.licenses

import androidx.annotation.RawRes
import dev.blazelight.p4oc.R

/**
 * SPDX-style license identifier with metadata for display and full-text loading.
 */
enum class License(
    val spdxId: String,
    val displayName: String,
    @param:RawRes val rawTextRes: Int,
    val isCopyleft: Boolean,
    val requiresRelinkingNotice: Boolean,
) {
    APACHE_2_0(
        spdxId = "Apache-2.0",
        displayName = "Apache License 2.0",
        rawTextRes = R.raw.license_apache_2_0,
        isCopyleft = false,
        requiresRelinkingNotice = false,
    ),
    LGPL_2_1(
        spdxId = "LGPL-2.1-or-later",
        displayName = "GNU Lesser General Public License, version 2.1",
        rawTextRes = R.raw.license_lgpl_2_1,
        isCopyleft = true,
        requiresRelinkingNotice = true,
    ),
    GPL_3_0(
        spdxId = "GPL-3.0-or-later",
        displayName = "GNU General Public License, version 3",
        rawTextRes = R.raw.license_gpl_3_0,
        isCopyleft = true,
        requiresRelinkingNotice = false,
    ),
}

/**
 * One third-party component shipped (or planned to ship) in the app.
 *
 * Keep this list in sync with `gradle/libs.versions.toml` and `app/build.gradle.kts`.
 * `version = null` means the dependency is not currently in the build but is documented
 * here because it is part of the planned integration surface (e.g. SoraEditor).
 */
data class LicenseEntry(
    val name: String,
    val version: String?,
    val license: License,
    val upstreamUrl: String,
    val notes: String? = null,
)
