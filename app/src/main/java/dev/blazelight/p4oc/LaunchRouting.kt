package dev.blazelight.p4oc

import dev.blazelight.p4oc.core.datastore.SettingsDataStore
import dev.blazelight.p4oc.core.log.AppLog
import dev.blazelight.p4oc.ui.navigation.Screen
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

/**
 * The cold-start destination is stable for the navigation back stack and is derived from persisted
 * onboarding state plus saved servers.
 *
 * A returning user with any saved server must reach the server-management/connect flow even when
 * offline, so a failed auto-reconnect or an old install whose onboarding flag was never set cannot
 * strand them on first-run Setup. A genuinely fresh install (no onboarding completion and no saved
 * server) still starts on Setup. Any read failure falls back conservatively to Setup so the splash
 * gate always releases.
 */
internal fun resolveLaunchDestination(onboardingCompleted: Boolean, hasSavedServers: Boolean): String =
    if (onboardingCompleted || hasSavedServers) Screen.Server.route else Screen.Setup.route

/**
 * Reads the persisted inputs with a conservative fallback so the splash gate never hangs.
 * Ordinary persisted-read failures must still release the splash and land on Setup; coroutine
 * cancellation and fatal JVM [Error]s must propagate unchanged so callers can react to them.
 */
internal suspend fun loadLaunchDestination(settingsDataStore: SettingsDataStore): String {
    val decision = runCatching {
        val onboardingCompleted = settingsDataStore.onboardingCompleted.first()
        val hasSavedServers = settingsDataStore.savedServers.first().isNotEmpty()
        resolveLaunchDestination(onboardingCompleted, hasSavedServers)
    }
    return decision.getOrElse { error ->
        // Coroutine cancellation and fatal JVM Errors must propagate unchanged; only ordinary
        // persisted-read failures are recovered by falling back to Setup.
        if (error is CancellationException || error is Error) throw error
        AppLog.w(TAG, "Launch routing read failed (${error::class.simpleName}); falling back to Setup")
        Screen.Setup.route
    }
}

private const val TAG = "LaunchRouting"
