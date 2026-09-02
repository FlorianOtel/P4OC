package dev.blazelight.p4oc.ui.screens.settings

import dev.blazelight.p4oc.data.remote.dto.AgentDto
import dev.blazelight.p4oc.data.remote.dto.McpStatusDto
import dev.blazelight.p4oc.data.workspace.WorkspaceClient
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkspaceSettingsViewModelTest {
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
    fun agents_useOnlyInjectedWorkspaceClient() = runTest(dispatcher) {
        val ownerClient: WorkspaceClient = mockk()
        val otherClient: WorkspaceClient = mockk(relaxed = true)
        coEvery { ownerClient.getAgents() } returns listOf(AgentDto(name = "owner-agent"))

        val viewModel = AgentsConfigViewModel(ownerClient)
        advanceUntilIdle()

        assertEquals(listOf("owner-agent"), viewModel.state.value.agents.map { it.name })
        coVerify(exactly = 1) { ownerClient.getAgents() }
        coVerify(exactly = 0) { otherClient.getAgents() }
    }

    @Test
    fun agents_staleClientFailureIsSafe() = runTest(dispatcher) {
        val staleClient: WorkspaceClient = mockk()
        coEvery { staleClient.getAgents() } throws IllegalStateException("stale workspace generation")

        val viewModel = AgentsConfigViewModel(staleClient)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoading)
        assertEquals(emptyList<AgentInfo>(), viewModel.state.value.agents)
        assertEquals("Could not load agents. Check the connection and try again.", viewModel.state.value.error)
    }

    @Test
    fun agents_retryRetainsErrorUntilSuccessfulReload() = runTest(dispatcher) {
        val client: WorkspaceClient = mockk()
        coEvery { client.getAgents() } throws IllegalStateException("offline")

        val viewModel = AgentsConfigViewModel(client)
        advanceUntilIdle()

        assertEquals("Could not load agents. Check the connection and try again.", viewModel.state.value.error)

        coEvery { client.getAgents() } returns listOf(AgentDto(name = "build"))
        viewModel.loadAgents()
        advanceUntilIdle()

        assertEquals(null, viewModel.state.value.error)
        assertEquals(listOf("build"), viewModel.state.value.agents.map { it.name })
    }

    @Test
    fun skills_useOnlyInjectedWorkspaceClient() = runTest(dispatcher) {
        val ownerClient: WorkspaceClient = mockk()
        val otherClient: WorkspaceClient = mockk(relaxed = true)
        coEvery { ownerClient.getMcpStatus() } returns mapOf(
            "owner-skill" to McpStatusDto(status = MCP_STATUS_CONNECTED)
        )

        val viewModel = SkillsViewModel(ownerClient)
        advanceUntilIdle()

        assertEquals(listOf("owner-skill"), viewModel.state.value.skills.map { it.name })
        coVerify(exactly = 1) { ownerClient.getMcpStatus() }
        coVerify(exactly = 0) { otherClient.getMcpStatus() }
    }

    @Test
    fun skills_staleClientFailureIsSafe() = runTest(dispatcher) {
        val staleClient: WorkspaceClient = mockk()
        coEvery { staleClient.getMcpStatus() } throws IllegalStateException("stale workspace generation")

        val viewModel = SkillsViewModel(staleClient)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoading)
        assertEquals(emptyList<SkillInfo>(), viewModel.state.value.skills)
        assertEquals(SkillsErrorKind.ApiError, viewModel.state.value.error?.kind)
    }
}
