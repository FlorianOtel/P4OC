package dev.blazelight.p4oc.ui.screens.settings

import dev.blazelight.p4oc.core.datastore.ConnectionSettings
import dev.blazelight.p4oc.core.datastore.SettingsDataStore
import dev.blazelight.p4oc.core.network.ConnectionState
import dev.blazelight.p4oc.core.network.ServerConfig
import dev.blazelight.p4oc.core.network.ServerConnectionRegistry
import dev.blazelight.p4oc.domain.server.ServerGeneration
import dev.blazelight.p4oc.domain.server.ServerRef
import dev.blazelight.p4oc.domain.workspace.Workspace
import dev.blazelight.p4oc.ui.workspace.WorkspaceRepositoryOwner
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelRegistryTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun tabSettingsObserveAndDisconnectOnlyTheirExactServerGeneration() = runTest(dispatcher) {
        val alpha = ServerRef.fromEndpoint("https://alpha.example.com")
        val beta = ServerRef.fromEndpoint("https://beta.example.com")
        val generation = ServerGeneration(3)
        val alphaState = MutableStateFlow<ConnectionState>(ConnectionState.Connected)
        val betaState = MutableStateFlow<ConnectionState>(ConnectionState.Connected)
        val registry = mockk<ServerConnectionRegistry>(relaxed = true)
        every { registry.connectionState(alpha, generation) } returns alphaState
        every { registry.connectionState(beta) } returns betaState
        every { registry.generation(alpha) } returns generation
        val settings = settingsDataStore()
        coEvery { settings.getLastConnection() } returns
            (ServerConfig("https://beta.example.com", "Beta", false) to null)
        val owner = owner(alpha, generation)
        val viewModel = SettingsViewModel(settings, registry, SettingsConnectionContext.Tab(owner))

        val collection = backgroundScope.launch(dispatcher) { viewModel.isConnected.collect {} }
        advanceUntilIdle()
        assertTrue(viewModel.isConnected.value)

        viewModel.disconnect()
        advanceUntilIdle()

        verify(exactly = 1) { registry.disconnect(alpha) }
        verify(exactly = 0) { registry.disconnect(beta) }
        coVerify(exactly = 0) { settings.clearLastConnection() }
        assertTrue(betaState.value is ConnectionState.Connected)
        collection.cancel()
    }

    @Test
    fun tabDisconnectClearsLastConnectionOnlyWhenPersistedEndpointMatches() = runTest(dispatcher) {
        val server = ServerRef.fromEndpoint("https://alpha.example.com")
        val generation = ServerGeneration(7)
        val registry = mockk<ServerConnectionRegistry>(relaxed = true)
        every { registry.connectionState(server, generation) } returns
            MutableStateFlow(ConnectionState.Connected)
        every { registry.generation(server) } returns generation
        val settings = settingsDataStore()
        coEvery { settings.getLastConnection() } returns
            (ServerConfig("https://alpha.example.com/", "Alpha", false) to null)
        val viewModel = SettingsViewModel(settings, registry, SettingsConnectionContext.Tab(owner(server, generation)))

        viewModel.disconnect()
        advanceUntilIdle()

        verify(exactly = 1) { registry.disconnect(server) }
        coVerify(exactly = 1) { settings.clearLastConnection() }
    }

    @Test
    fun globalSettingsAreDisconnectedAndCannotGuessARegistryServer() = runTest(dispatcher) {
        val registry = mockk<ServerConnectionRegistry>(relaxed = true)
        val settings = settingsDataStore()
        coEvery { settings.getLastConnection() } returns
            (ServerConfig("https://alpha.example.com", "Alpha", false) to null)
        val viewModel = SettingsViewModel(settings, registry, SettingsConnectionContext.Global)

        viewModel.disconnect()
        advanceUntilIdle()

        assertFalse(viewModel.isConnected.value)
        verify(exactly = 0) { registry.disconnect(any()) }
        coVerify(exactly = 0) { settings.getLastConnection() }
        coVerify(exactly = 0) { settings.clearLastConnection() }
    }

    private fun settingsDataStore(): SettingsDataStore = mockk(relaxUnitFun = true) {
        every { connectionSettings } returns flowOf(ConnectionSettings())
        every { serverUrl } returns flowOf("http://localhost:4096")
        every { isLocalServer } returns flowOf(true)
        every { themeMode } returns flowOf("system")
    }

    private fun owner(server: ServerRef, generation: ServerGeneration): WorkspaceRepositoryOwner {
        val owner = mockk<WorkspaceRepositoryOwner>()
        every { owner.workspace } returns Workspace(server, "/workspace")
        every { owner.generation } returns generation
        return owner
    }
}
