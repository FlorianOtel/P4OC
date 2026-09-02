package dev.blazelight.p4oc

import dev.blazelight.p4oc.core.datastore.SavedServer
import dev.blazelight.p4oc.core.datastore.SettingsDataStore
import dev.blazelight.p4oc.ui.navigation.Screen
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class LaunchRoutingTest {

    @Test
    fun `fresh install with no onboarding and no saved server starts Setup`() {
        assertEquals(Screen.Setup.route, resolveLaunchDestination(false, false))
    }

    @Test
    fun `legacy install with saved server but no onboarding flag starts Server`() {
        assertEquals(Screen.Server.route, resolveLaunchDestination(false, true))
    }

    @Test
    fun `onboarding completed with no saved servers starts Server`() {
        assertEquals(Screen.Server.route, resolveLaunchDestination(true, false))
    }

    @Test
    fun `onboarding completed with saved servers starts Server`() {
        assertEquals(Screen.Server.route, resolveLaunchDestination(true, true))
    }

    @Test
    fun `load reads onboarding and saved servers and resolves`() = runTest {
        val store = mockk<SettingsDataStore>(relaxed = true)
        every { store.onboardingCompleted } returns MutableStateFlow(false)
        every { store.savedServers } returns MutableStateFlow(
            listOf(
                SavedServer(
                    id = "s1",
                    endpoint = "http://host:4096",
                    endpointKey = "http://host:4096",
                    displayName = "Host",
                ),
            ),
        )

        assertEquals(Screen.Server.route, loadLaunchDestination(store))
    }

    @Test
    fun `fresh install loads Setup from persisted inputs`() = runTest {
        val store = mockk<SettingsDataStore>(relaxed = true)
        every { store.onboardingCompleted } returns MutableStateFlow(false)
        every { store.savedServers } returns MutableStateFlow(emptyList())

        assertEquals(Screen.Setup.route, loadLaunchDestination(store))
    }

    @Test
    fun `loadLaunchDestination falls back to Setup when the read throws`() = runTest {
        val store = mockk<SettingsDataStore>(relaxed = true)
        every { store.onboardingCompleted } returns flow { throw java.io.IOException("corrupt prefs") }

        assertEquals(Screen.Setup.route, loadLaunchDestination(store))
    }

    @Test
    fun `loadLaunchDestination propagates coroutine cancellation`() = runTest {
        val store = mockk<SettingsDataStore>(relaxed = true)
        val cancelled = CancellationException("cancelled")
        every { store.onboardingCompleted } returns flow { throw cancelled }

        val thrown = try {
            loadLaunchDestination(store)
            null
        } catch (t: Throwable) {
            t
        }
        assertSame(cancelled, thrown)
    }

    @Test
    fun `loadLaunchDestination does not swallow fatal errors`() = runTest {
        val store = mockk<SettingsDataStore>(relaxed = true)
        val fatal = AssertionError("fatal")
        every { store.onboardingCompleted } returns flow { throw fatal }

        val thrown = try {
            loadLaunchDestination(store)
            null
        } catch (t: Throwable) {
            t
        }
        assertSame(fatal, thrown)
    }
}
