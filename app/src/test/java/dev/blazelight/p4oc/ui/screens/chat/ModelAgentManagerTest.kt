package dev.blazelight.p4oc.ui.screens.chat

import dev.blazelight.p4oc.core.datastore.SessionComposerSelection
import dev.blazelight.p4oc.core.datastore.SettingsDataStore
import dev.blazelight.p4oc.core.log.AppLog
import dev.blazelight.p4oc.core.network.ConnectionState
import dev.blazelight.p4oc.core.network.OpenCodeApi
import dev.blazelight.p4oc.data.remote.dto.AgentDto
import dev.blazelight.p4oc.data.remote.dto.ModelDto
import dev.blazelight.p4oc.data.remote.dto.ModelInput
import dev.blazelight.p4oc.data.remote.dto.ModelRefDto
import dev.blazelight.p4oc.data.remote.dto.ProviderDto
import dev.blazelight.p4oc.data.remote.dto.ProvidersResponseDto
import dev.blazelight.p4oc.data.remote.dto.SessionModelDto
import dev.blazelight.p4oc.data.server.ActiveServerApiProvider
import dev.blazelight.p4oc.data.workspace.WorkspaceClient
import dev.blazelight.p4oc.domain.server.ServerGeneration
import dev.blazelight.p4oc.domain.server.ServerRef
import dev.blazelight.p4oc.domain.workspace.Workspace
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("LargeClass")
class ModelAgentManagerTest {

    private val settingsDataStore: SettingsDataStore = mockk(relaxed = true)
    private val api: OpenCodeApi = mockk()
    private val workspaceClient = WorkspaceClient(
        workspace = Workspace(ServerRef.fromEndpointKey("http://test.local"), "/test"),
        generation = ServerGeneration(1L),
        apiProvider = ActiveServerApiProvider { _, _ -> api },
        connectionState = MutableStateFlow(ConnectionState.Disconnected),
    )

    @Before
    fun setUp() {
        mockkObject(AppLog)
        every { AppLog.d(any(), any<String>()) } returns Unit
        every { AppLog.d(any(), any<() -> String>()) } returns Unit
        every { AppLog.e(any(), any<String>()) } returns Unit
        every { AppLog.e(any(), any<String>(), any()) } returns Unit
        every { settingsDataStore.favoriteModels } returns flowOf(emptySet())
        every { settingsDataStore.recentModels } returns flowOf(emptyList())
        coEvery { settingsDataStore.getComposerSelectionForSession(any(), any()) } returns null
    }

    @After
    fun tearDown() {
        unmockkObject(AppLog)
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private fun makeAgent(
        name: String,
        mode: String? = "primary",
        hidden: Boolean? = null,
        model: ModelRefDto? = null
    ) = AgentDto(name = name, description = "desc", mode = mode, hidden = hidden, model = model)

    private fun makeModel(id: String, providerId: String) = ModelDto(
        id = id,
        providerId = providerId,
        name = "Model $id"
    )

    // ── loadAgents ──────────────────────────────────────────────────────────

    @Test
    fun `loadAgents includes primary and all non-hidden agents`() = runTest {
        val agents = listOf(
            makeAgent("build", mode = "primary"),
            makeAgent("code", mode = "primary"),
            makeAgent("general", mode = "all"),
            makeAgent("hidden-agent", mode = "primary", hidden = true),
            makeAgent("subagent", mode = "subagent")
        )
        coEvery { api.getAgents(any(), null) } returns agents

        val manager = ModelAgentManager(workspaceClient, settingsDataStore, this)
        manager.loadAgents()
        advanceUntilIdle()

        val result = manager.availableAgents.value
        assertEquals(3, result.size)
        assertEquals("build", result[0].name)
        assertEquals("code", result[1].name)
        assertEquals("general", result[2].name)
    }

    @Test
    fun `loadAgents selects first primary agent from server order`() = runTest {
        val agents = listOf(
            makeAgent("code", mode = "primary"),
            makeAgent("build", mode = "primary"),
            makeAgent("ask", mode = "primary")
        )
        coEvery { api.getAgents(any(), null) } returns agents

        val manager = ModelAgentManager(workspaceClient, settingsDataStore, this)
        manager.loadAgents()
        advanceUntilIdle()

        assertEquals("code", manager.selectedAgent.value)
    }

    @Test
    fun `loadAgents selects first agent when build not available`() = runTest {
        val agents = listOf(
            makeAgent("code", mode = "primary"),
            makeAgent("ask", mode = "primary")
        )
        coEvery { api.getAgents(any(), null) } returns agents

        val manager = ModelAgentManager(workspaceClient, settingsDataStore, this)
        manager.loadAgents()
        advanceUntilIdle()

        assertEquals("code", manager.selectedAgent.value)
    }

    @Test
    fun `loadAgents selects configured model for default agent`() = runTest {
        val agents = listOf(
            makeAgent(
                "build",
                mode = "primary",
                model = ModelRefDto(providerID = "anthropic", modelID = "claude-3")
            )
        )
        coEvery { api.getAgents(any(), null) } returns agents

        val manager = ModelAgentManager(workspaceClient, settingsDataStore, this)
        manager.loadAgents()
        advanceUntilIdle()

        assertEquals(ModelInput(providerID = "anthropic", modelID = "claude-3"), manager.selectedModel.value)
    }

    @Test
    fun `selectAgent selects configured model`() = runTest {
        val agents = listOf(
            makeAgent(
                "code",
                mode = "primary",
                model = ModelRefDto(providerID = "openai", modelID = "gpt-4")
            ),
            makeAgent(
                "build",
                mode = "primary",
                model = ModelRefDto(providerID = "anthropic", modelID = "claude-3")
            )
        )
        coEvery { api.getAgents(any(), null) } returns agents

        val manager = ModelAgentManager(workspaceClient, settingsDataStore, this)
        manager.loadAgents()
        advanceUntilIdle()

        manager.selectAgent("code")

        assertEquals(ModelInput(providerID = "openai", modelID = "gpt-4"), manager.selectedModel.value)
    }

    @Test
    fun `loadAgents agent model is not overwritten by loadModels default`() = runTest {
        val agents = listOf(
            makeAgent(
                "build",
                mode = "primary",
                model = ModelRefDto(providerID = "anthropic", modelID = "claude-3")
            )
        )
        val providersResponse = ProvidersResponseDto(
            all = listOf(
                ProviderDto(
                    id = "openai",
                    name = "OpenAI",
                    source = "env",
                    models = mapOf(
                        "gpt-4" to makeModel("gpt-4", "openai")
                    )
                )
            ),
            default = mapOf("openai" to "gpt-4"),
            connected = listOf("openai")
        )
        coEvery { api.getAgents(any(), null) } returns agents
        coEvery { api.getProviders(any(), null) } returns providersResponse

        val manager = ModelAgentManager(workspaceClient, settingsDataStore, this)
        manager.loadAgents()
        advanceUntilIdle()
        manager.loadModels()
        advanceUntilIdle()

        assertEquals(ModelInput(providerID = "anthropic", modelID = "claude-3"), manager.selectedModel.value)
    }

    @Test
    fun `loadAgents handles API error gracefully`() = runTest {
        coEvery { api.getAgents(any(), null) } throws RuntimeException("Network error")

        val manager = ModelAgentManager(workspaceClient, settingsDataStore, this)
        manager.loadAgents()
        advanceUntilIdle()

        assertTrue(manager.availableAgents.value.isEmpty())
        assertNull(manager.selectedAgent.value)
    }

    // ── loadModels ──────────────────────────────────────────────────────────

    @Test
    fun `loadModels selects last used model when available`() = runTest {
        val recentModel = ModelInput(providerID = "anthropic", modelID = "claude-3")
        every { settingsDataStore.recentModels } returns flowOf(listOf(recentModel))

        val providersResponse = ProvidersResponseDto(
            all = listOf(
                ProviderDto(
                    id = "anthropic",
                    name = "Anthropic",
                    source = "env",
                    models = mapOf(
                        "claude-3" to makeModel("claude-3", "anthropic")
                    )
                )
            ),
            default = mapOf("openai" to "gpt-4"),
            connected = listOf("anthropic")
        )
        coEvery { api.getProviders(any(), null) } returns providersResponse

        val manager = ModelAgentManager(workspaceClient, settingsDataStore, this)
        advanceUntilIdle()

        manager.loadModels()
        advanceUntilIdle()

        val selected = manager.selectedModel.value
        assertNotNull(selected)
        assertEquals("anthropic", selected!!.providerID)
        assertEquals("claude-3", selected.modelID)
    }

    @Test
    fun `loadModels prefers server default over app recent model`() = runTest {
        val recentModel = ModelInput(providerID = "anthropic", modelID = "claude-3")
        every { settingsDataStore.recentModels } returns flowOf(listOf(recentModel))

        val providersResponse = ProvidersResponseDto(
            all = listOf(
                ProviderDto(
                    id = "anthropic",
                    name = "Anthropic",
                    source = "env",
                    models = mapOf(
                        "claude-3" to makeModel("claude-3", "anthropic")
                    )
                ),
                ProviderDto(
                    id = "openai",
                    name = "OpenAI",
                    source = "env",
                    models = mapOf(
                        "gpt-4" to makeModel("gpt-4", "openai")
                    )
                )
            ),
            default = mapOf("openai" to "gpt-4"),
            connected = listOf("anthropic", "openai")
        )
        coEvery { api.getProviders(any(), null) } returns providersResponse

        val manager = ModelAgentManager(workspaceClient, settingsDataStore, this)
        advanceUntilIdle()

        manager.loadModels()
        advanceUntilIdle()

        assertEquals(ModelInput(providerID = "openai", modelID = "gpt-4"), manager.selectedModel.value)
    }

    @Test
    fun `loadModels restores available model selected for this session before server default`() = runTest {
        val sessionModel = ModelInput(providerID = "anthropic", modelID = "claude-3")
        coEvery {
            settingsDataStore.getComposerSelectionForSession(workspaceClient.workspace, "session-1")
        } returns SessionComposerSelection(model = sessionModel, pendingServerSync = true)
        val providersResponse = ProvidersResponseDto(
            all = listOf(
                ProviderDto(
                    id = "anthropic",
                    name = "Anthropic",
                    source = "env",
                    models = mapOf("claude-3" to makeModel("claude-3", "anthropic")),
                ),
                ProviderDto(
                    id = "openai",
                    name = "OpenAI",
                    source = "env",
                    models = mapOf("gpt-4" to makeModel("gpt-4", "openai")),
                ),
            ),
            default = mapOf("openai" to "gpt-4"),
            connected = listOf("anthropic", "openai"),
        )
        coEvery { api.getProviders(any(), null) } returns providersResponse

        val manager = ModelAgentManager(workspaceClient, settingsDataStore, this, sessionId = "session-1")
        manager.loadModels()
        advanceUntilIdle()

        assertEquals(sessionModel, manager.selectedModel.value)
    }

    @Test
    fun `loadModels ignores unavailable model selected for this session`() = runTest {
        coEvery {
            settingsDataStore.getComposerSelectionForSession(workspaceClient.workspace, "session-1")
        } returns SessionComposerSelection(
            model = ModelInput(providerID = "anthropic", modelID = "claude-3"),
            pendingServerSync = true,
        )
        val defaultModel = ModelInput(providerID = "openai", modelID = "gpt-4")
        coEvery { api.getProviders(any(), null) } returns ProvidersResponseDto(
            all = listOf(
                ProviderDto(
                    id = "openai",
                    name = "OpenAI",
                    source = "env",
                    models = mapOf("gpt-4" to makeModel("gpt-4", "openai")),
                ),
            ),
            default = mapOf("openai" to "gpt-4"),
            connected = listOf("openai"),
        )

        val manager = ModelAgentManager(workspaceClient, settingsDataStore, this, sessionId = "session-1")
        manager.loadModels()
        advanceUntilIdle()

        assertEquals(defaultModel, manager.selectedModel.value)
    }

    @Test
    fun `selectModel persists selection for this session`() = runTest {
        val selected = ModelInput(providerID = "anthropic", modelID = "claude-3")
        val manager = ModelAgentManager(workspaceClient, settingsDataStore, this, sessionId = "session-1")

        manager.selectModel(selected)
        advanceUntilIdle()

        coVerify(exactly = 1) {
            settingsDataStore.setComposerSelectionForSession(
                workspaceClient.workspace,
                "session-1",
                SessionComposerSelection(model = selected, variant = null, pendingServerSync = true),
            )
        }
        coVerify(exactly = 1) { settingsDataStore.addRecentModel(selected) }
    }

    @Test
    fun `loadModels restores persisted model and reasoning effort after manager recreation`() = runTest {
        val model = ModelInput(providerID = "openai", modelID = "gpt-5")
        coEvery {
            settingsDataStore.getComposerSelectionForSession(workspaceClient.workspace, "session-1")
        } returns SessionComposerSelection(model = model, variant = "high", pendingServerSync = true)
        coEvery { api.getProviders(any(), null) } returns reasoningProviders()

        val manager = ModelAgentManager(workspaceClient, settingsDataStore, this, sessionId = "session-1")
        manager.loadModels()
        advanceUntilIdle()

        assertEquals(model, manager.selectedModel.value)
        assertEquals("high", manager.currentReasoningEffort())
    }

    @Test
    fun `server session model and variant override synced local selection`() = runTest {
        val localModel = ModelInput(providerID = "openai", modelID = "gpt-5")
        coEvery {
            settingsDataStore.getComposerSelectionForSession(workspaceClient.workspace, "session-1")
        } returns SessionComposerSelection(model = localModel, variant = "low", pendingServerSync = false)
        coEvery { api.getProviders(any(), null) } returns reasoningProviders()
        val manager = ModelAgentManager(workspaceClient, settingsDataStore, this, sessionId = "session-1")

        manager.applyServerSessionModel(SessionModelDto(id = "gpt-5", providerID = "openai", variant = "high"))
        manager.loadModels()
        advanceUntilIdle()

        assertEquals(localModel, manager.selectedModel.value)
        assertEquals("high", manager.currentReasoningEffort())
    }

    @Test
    fun `fresh unsupported server variant normalizes to null and final record is synced`() = runTest {
        val model = ModelInput(providerID = "openai", modelID = "gpt-5")
        // No local persisted selection: getComposerSelectionForSession returns null by default.
        coEvery { api.getProviders(any(), null) } returns reasoningProviders()
        val manager = ModelAgentManager(workspaceClient, settingsDataStore, this, sessionId = "session-1")

        manager.applyServerSessionModel(
            SessionModelDto(id = "gpt-5", providerID = "openai", variant = "obsolete")
        )
        manager.loadModels()
        advanceUntilIdle()

        assertEquals(model, manager.selectedModel.value)
        assertNull(manager.currentReasoningEffort())
        // The final persisted record is the normalized, acknowledged (non-pending) selection;
        // the raw unsupported server variant must never be written.
        coVerify {
            settingsDataStore.setComposerSelectionForSession(
                workspaceClient.workspace,
                "session-1",
                SessionComposerSelection(model = model, variant = null, pendingServerSync = false),
            )
        }
    }

    @Test
    fun `pending local selection wins over stale server session until sent`() = runTest {
        val model = ModelInput(providerID = "openai", modelID = "gpt-5")
        coEvery {
            settingsDataStore.getComposerSelectionForSession(workspaceClient.workspace, "session-1")
        } returns SessionComposerSelection(model = model, variant = "high", pendingServerSync = true)
        coEvery { api.getProviders(any(), null) } returns reasoningProviders()
        val manager = ModelAgentManager(workspaceClient, settingsDataStore, this, sessionId = "session-1")

        manager.applyServerSessionModel(SessionModelDto(id = "gpt-5", providerID = "openai", variant = "low"))
        manager.loadModels()
        advanceUntilIdle()

        assertEquals("high", manager.currentReasoningEffort())
    }

    @Test
    fun `unsupported persisted reasoning effort is restored as default`() = runTest {
        val model = ModelInput(providerID = "openai", modelID = "gpt-5")
        coEvery {
            settingsDataStore.getComposerSelectionForSession(workspaceClient.workspace, "session-1")
        } returns SessionComposerSelection(model = model, variant = "obsolete", pendingServerSync = true)
        coEvery { api.getProviders(any(), null) } returns reasoningProviders()
        val manager = ModelAgentManager(workspaceClient, settingsDataStore, this, sessionId = "session-1")

        manager.loadModels()
        advanceUntilIdle()

        assertEquals(model, manager.selectedModel.value)
        assertNull(manager.currentReasoningEffort())
    }

    @Test
    fun `loadModels skips disconnected provider default observed in OpenCode 1 18 16`() = runTest {
        val providersResponse = ProvidersResponseDto(
            all = listOf(
                ProviderDto(
                    id = "zhipuai",
                    name = "Zhipu AI",
                    source = "models.dev",
                    models = mapOf(
                        "glm-5v-turbo" to makeModel("glm-5v-turbo", "zhipuai")
                    )
                ),
                ProviderDto(
                    id = "opencode",
                    name = "OpenCode",
                    source = "env",
                    models = mapOf(
                        "big-pickle" to makeModel("big-pickle", "opencode")
                    )
                )
            ),
            default = linkedMapOf(
                "zhipuai" to "glm-5v-turbo",
                "opencode" to "big-pickle",
            ),
            connected = listOf("opencode")
        )
        coEvery { api.getProviders(any(), null) } returns providersResponse

        val manager = ModelAgentManager(workspaceClient, settingsDataStore, this)
        manager.loadModels()
        advanceUntilIdle()

        assertEquals(ModelInput(providerID = "opencode", modelID = "big-pickle"), manager.selectedModel.value)
    }

    private fun reasoningProviders() = ProvidersResponseDto(
        all = listOf(
            ProviderDto(
                id = "openai",
                name = "OpenAI",
                source = "env",
                models = mapOf(
                    "gpt-5" to ModelDto(
                        id = "gpt-5",
                        providerId = "openai",
                        name = "GPT-5",
                        variants = kotlinx.serialization.json.buildJsonObject {
                            put("low", kotlinx.serialization.json.buildJsonObject {})
                            put("high", kotlinx.serialization.json.buildJsonObject {})
                        },
                    )
                ),
            )
        ),
        default = mapOf("openai" to "gpt-5"),
        connected = listOf("openai"),
    )

    @Test
    fun `loadModels keeps explicit user selected model when still available`() = runTest {
        val selectedModel = ModelInput(providerID = "anthropic", modelID = "claude-3")
        val providersResponse = ProvidersResponseDto(
            all = listOf(
                ProviderDto(
                    id = "anthropic",
                    name = "Anthropic",
                    source = "env",
                    models = mapOf(
                        "claude-3" to makeModel("claude-3", "anthropic")
                    )
                ),
                ProviderDto(
                    id = "openai",
                    name = "OpenAI",
                    source = "env",
                    models = mapOf(
                        "gpt-4" to makeModel("gpt-4", "openai")
                    )
                )
            ),
            default = mapOf("openai" to "gpt-4"),
            connected = listOf("anthropic", "openai")
        )
        coEvery { api.getProviders(any(), null) } returns providersResponse

        val manager = ModelAgentManager(workspaceClient, settingsDataStore, this)
        manager.selectModel(selectedModel)
        advanceUntilIdle()

        manager.loadModels()
        advanceUntilIdle()

        assertEquals(selectedModel, manager.selectedModel.value)
    }

    @Test
    fun `loadModels reconciles unavailable explicit model to server default`() = runTest {
        val staleModel = ModelInput(providerID = "anthropic", modelID = "claude-3")
        val providersResponse = ProvidersResponseDto(
            all = listOf(
                ProviderDto(
                    id = "openai",
                    name = "OpenAI",
                    source = "env",
                    models = mapOf(
                        "gpt-4" to makeModel("gpt-4", "openai")
                    )
                )
            ),
            default = mapOf("openai" to "gpt-4"),
            connected = listOf("openai")
        )
        coEvery { api.getProviders(any(), null) } returns providersResponse

        val manager = ModelAgentManager(workspaceClient, settingsDataStore, this)
        manager.selectModel(staleModel)
        advanceUntilIdle()

        manager.loadModels()
        advanceUntilIdle()

        assertEquals(ModelInput(providerID = "openai", modelID = "gpt-4"), manager.selectedModel.value)
    }

    @Test
    fun `loadModels does not infer reasoning effort from first available variant without explicit default`() = runTest {
        every { settingsDataStore.recentModels } returns flowOf(emptyList())

        val providersResponse = ProvidersResponseDto(
            all = listOf(
                ProviderDto(
                    id = "anthropic",
                    name = "Anthropic",
                    source = "env",
                    models = mapOf(
                        "claude-3" to ModelDto(
                            id = "claude-3",
                            providerId = "anthropic",
                            name = "Claude 3",
                            variants = kotlinx.serialization.json.JsonObject(
                                mapOf(
                                    "low" to kotlinx.serialization.json.JsonObject(emptyMap()),
                                    "high" to kotlinx.serialization.json.JsonObject(emptyMap())
                                )
                            )
                        )
                    )
                )
            ),
            default = mapOf("anthropic" to "claude-3"),
            connected = listOf("anthropic")
        )
        coEvery { api.getProviders(any(), null) } returns providersResponse

        val manager = ModelAgentManager(workspaceClient, settingsDataStore, this)
        advanceUntilIdle()

        manager.loadModels()
        advanceUntilIdle()

        assertEquals(ModelInput(providerID = "anthropic", modelID = "claude-3"), manager.selectedModel.value)
        assertNull(
            "A provider/model default is not a reasoning-effort default; absent explicit upstream/user effort, " +
                "do not silently choose the first representable variant.",
            manager.currentReasoningEffort()
        )
    }

    @Test
    fun `reselecting same model keeps reasoning effort and matching ack clears pending`() = runTest {
        val model = ModelInput(providerID = "openai", modelID = "gpt-5")
        coEvery { api.getProviders(any(), null) } returns reasoningProviders()
        val manager = ModelAgentManager(workspaceClient, settingsDataStore, this, sessionId = "session-1")
        manager.loadModels()
        advanceUntilIdle()

        manager.selectReasoningEffort("high")
        advanceUntilIdle()

        // Re-selecting the already-selected model from the composer must retain the effort,
        // not silently drop it back to null in the pending record.
        clearMocks(settingsDataStore, answers = false, recordedCalls = true)
        manager.selectModel(model)
        advanceUntilIdle()

        assertEquals("high", manager.currentReasoningEffort())
        coVerify(exactly = 1) {
            settingsDataStore.setComposerSelectionForSession(
                workspaceClient.workspace,
                "session-1",
                SessionComposerSelection(model = model, variant = "high", pendingServerSync = true),
            )
        }

        // The exact sent variant acknowledges and flushes the pending record.
        manager.markComposerSelectionSent(model, "high")
        advanceUntilIdle()

        coVerify {
            settingsDataStore.setComposerSelectionForSession(
                workspaceClient.workspace,
                "session-1",
                SessionComposerSelection(model = model, variant = "high", pendingServerSync = false),
            )
        }
    }

    @Test
    fun `loadModels selects default model when no recent`() = runTest {
        every { settingsDataStore.recentModels } returns flowOf(emptyList())

        val providersResponse = ProvidersResponseDto(
            all = listOf(
                ProviderDto(
                    id = "openai",
                    name = "OpenAI",
                    source = "env",
                    models = mapOf(
                        "gpt-4" to makeModel("gpt-4", "openai")
                    )
                )
            ),
            default = mapOf("openai" to "gpt-4"),
            connected = listOf("openai")
        )
        coEvery { api.getProviders(any(), null) } returns providersResponse

        val manager = ModelAgentManager(workspaceClient, settingsDataStore, this)
        advanceUntilIdle()

        manager.loadModels()
        advanceUntilIdle()

        val selected = manager.selectedModel.value
        assertNotNull(selected)
        assertEquals("openai", selected!!.providerID)
        assertEquals("gpt-4", selected.modelID)
    }

    @Test
    fun `active model change updates selection when no agent or explicit model override`() = runTest {
        val coordinator = ModelSelectionCoordinator()
        val manager = ModelAgentManager(workspaceClient, settingsDataStore, backgroundScope, null, coordinator)
        advanceUntilIdle()
        runCurrent()

        val publishedModel = ModelInput(providerID = "anthropic", modelID = "claude-3")
        coordinator.publishActiveModel(workspaceClient.workspace, workspaceClient.generation, publishedModel)
        runCurrent()

        assertEquals(publishedModel, manager.selectedModel.value)
    }

    @Test
    fun `active model change does not replace explicit user selection`() = runTest {
        val coordinator = ModelSelectionCoordinator()
        val manager = ModelAgentManager(workspaceClient, settingsDataStore, backgroundScope, null, coordinator)
        val explicitModel = ModelInput(providerID = "openai", modelID = "gpt-4")
        manager.selectModel(explicitModel)
        advanceUntilIdle()
        runCurrent()

        coordinator.publishActiveModel(
            workspaceClient.workspace,
            workspaceClient.generation,
            ModelInput(providerID = "anthropic", modelID = "claude-3"),
        )
        runCurrent()

        assertEquals(explicitModel, manager.selectedModel.value)
    }

    @Test
    fun `active model change does not replace agent model selection`() = runTest {
        val coordinator = ModelSelectionCoordinator()
        val agents = listOf(
            makeAgent(
                "code",
                mode = "primary",
                model = ModelRefDto(providerID = "openai", modelID = "gpt-4")
            )
        )
        coEvery { api.getAgents(any(), null) } returns agents
        val manager = ModelAgentManager(workspaceClient, settingsDataStore, backgroundScope, null, coordinator)
        manager.loadAgents()
        advanceUntilIdle()
        runCurrent()

        coordinator.publishActiveModel(
            workspaceClient.workspace,
            workspaceClient.generation,
            ModelInput(providerID = "anthropic", modelID = "claude-3"),
        )
        runCurrent()

        assertEquals(ModelInput(providerID = "openai", modelID = "gpt-4"), manager.selectedModel.value)
    }

    @Test
    fun `active model change from matching workspace and generation updates selection`() = runTest {
        val coordinator = ModelSelectionCoordinator()
        val manager = ModelAgentManager(workspaceClient, settingsDataStore, backgroundScope, null, coordinator)
        advanceUntilIdle()
        runCurrent()

        val publishedModel = ModelInput(providerID = "anthropic", modelID = "claude-3")
        coordinator.publishActiveModel(
            workspaceClient.workspace,
            workspaceClient.generation,
            publishedModel,
        )
        runCurrent()

        assertEquals(publishedModel, manager.selectedModel.value)
    }

    @Test
    fun `active model change from a different workspace is rejected`() = runTest {
        val coordinator = ModelSelectionCoordinator()
        val manager = ModelAgentManager(workspaceClient, settingsDataStore, backgroundScope, null, coordinator)
        advanceUntilIdle()
        runCurrent()

        val otherWorkspace = Workspace(
            ServerRef.fromEndpointKey("http://test.local"),
            "/other-workspace",
        )
        val publishedModel = ModelInput(providerID = "anthropic", modelID = "claude-3")
        coordinator.publishActiveModel(
            otherWorkspace,
            workspaceClient.generation,
            publishedModel,
        )
        runCurrent()

        assertNull(manager.selectedModel.value)
    }

    @Test
    fun `active model change from a stale generation is rejected`() = runTest {
        val coordinator = ModelSelectionCoordinator()
        val manager = ModelAgentManager(workspaceClient, settingsDataStore, backgroundScope, null, coordinator)
        advanceUntilIdle()
        runCurrent()

        val publishedModel = ModelInput(providerID = "anthropic", modelID = "claude-3")
        coordinator.publishActiveModel(
            workspaceClient.workspace,
            ServerGeneration(workspaceClient.generation.value - 1),
            publishedModel,
        )
        runCurrent()

        assertNull(manager.selectedModel.value)
    }

    // ── selectModel ─────────────────────────────────────────────────────────

    @Test
    fun `selectModel adds to recent models`() = runTest {
        val model = ModelInput(providerID = "anthropic", modelID = "claude-3")

        val manager = ModelAgentManager(workspaceClient, settingsDataStore, this)
        manager.selectModel(model)
        advanceUntilIdle()

        assertEquals(model, manager.selectedModel.value)
        coVerify { settingsDataStore.addRecentModel(model) }
    }

    @Test
    fun `selectReasoningEffort persists normalized variant with pending sync`() = runTest {
        val model = ModelInput(providerID = "openai", modelID = "gpt-5")
        coEvery { api.getProviders(any(), null) } returns reasoningProviders()
        val manager = ModelAgentManager(workspaceClient, settingsDataStore, this, sessionId = "session-1")
        manager.loadModels()
        advanceUntilIdle()

        manager.selectReasoningEffort("high")
        advanceUntilIdle()

        assertEquals("high", manager.currentReasoningEffort())
        coVerify(exactly = 1) {
            settingsDataStore.setComposerSelectionForSession(
                workspaceClient.workspace,
                "session-1",
                SessionComposerSelection(model = model, variant = "high", pendingServerSync = true),
            )
        }
    }

    @Test
    fun `restored unsupported variant normalizes to null and persists pending`() = runTest {
        val model = ModelInput(providerID = "openai", modelID = "gpt-5")
        coEvery {
            settingsDataStore.getComposerSelectionForSession(workspaceClient.workspace, "session-1")
        } returns SessionComposerSelection(model = model, variant = "obsolete", pendingServerSync = true)
        coEvery { api.getProviders(any(), null) } returns reasoningProviders()
        val manager = ModelAgentManager(workspaceClient, settingsDataStore, this, sessionId = "session-1")

        manager.loadModels()
        advanceUntilIdle()

        assertNull(manager.currentReasoningEffort())
        coVerify(exactly = 1) {
            settingsDataStore.setComposerSelectionForSession(
                workspaceClient.workspace,
                "session-1",
                SessionComposerSelection(model = model, variant = null, pendingServerSync = true),
            )
        }
    }

    @Test
    fun `selectAgent persists agent model selection with pending sync`() = runTest {
        val agents = listOf(
            makeAgent("build", mode = "primary", model = ModelRefDto(providerID = "openai", modelID = "gpt-5"))
        )
        coEvery { api.getAgents(any(), null) } returns agents
        coEvery { api.getProviders(any(), null) } returns reasoningProviders()
        val manager = ModelAgentManager(workspaceClient, settingsDataStore, this, sessionId = "session-1")
        manager.loadAgents()
        manager.loadModels()
        advanceUntilIdle()

        manager.selectAgent("build")
        advanceUntilIdle()

        val agentModel = ModelInput(providerID = "openai", modelID = "gpt-5")
        coVerify(exactly = 1) {
            settingsDataStore.setComposerSelectionForSession(
                workspaceClient.workspace,
                "session-1",
                SessionComposerSelection(model = agentModel, variant = null, pendingServerSync = true),
            )
        }
    }

    @Test
    fun `selectAgent with same model preserves retained reasoning effort`() = runTest {
        val model = ModelInput(providerID = "openai", modelID = "gpt-5")
        coEvery { api.getProviders(any(), null) } returns reasoningProviders()
        coEvery { api.getAgents(any(), null) } returns listOf(
            makeAgent("build", mode = "primary", model = ModelRefDto(providerID = "openai", modelID = "gpt-5"))
        )
        val manager = ModelAgentManager(workspaceClient, settingsDataStore, this, sessionId = "session-1")
        manager.loadAgents()
        manager.loadModels()
        advanceUntilIdle()

        // Select the model the agent's configured model already matches, keeping the effort.
        manager.selectModel(model)
        manager.selectReasoningEffort("high")
        advanceUntilIdle()

        clearMocks(settingsDataStore, answers = false, recordedCalls = true)
        manager.selectAgent("build")
        advanceUntilIdle()

        assertEquals("high", manager.currentReasoningEffort())
        coVerify(exactly = 1) {
            settingsDataStore.setComposerSelectionForSession(
                workspaceClient.workspace,
                "session-1",
                SessionComposerSelection(model = model, variant = "high", pendingServerSync = true),
            )
        }
    }

    @Test
    fun `matching acknowledgment clears pending and allows later server authority`() = runTest {
        val model = ModelInput(providerID = "openai", modelID = "gpt-5")
        coEvery {
            settingsDataStore.getComposerSelectionForSession(workspaceClient.workspace, "session-1")
        } returns SessionComposerSelection(model = model, variant = "high", pendingServerSync = true)
        coEvery { api.getProviders(any(), null) } returns reasoningProviders()
        val manager = ModelAgentManager(workspaceClient, settingsDataStore, this, sessionId = "session-1")
        manager.loadModels()
        advanceUntilIdle()
        assertEquals("high", manager.currentReasoningEffort())

        // Matching ack flushes the pending local selection; the server now owns the value.
        manager.markComposerSelectionSent(model, "high")
        advanceUntilIdle()
        manager.applyServerSessionModel(SessionModelDto(id = "gpt-5", providerID = "openai", variant = "low"))
        advanceUntilIdle()

        assertEquals("low", manager.currentReasoningEffort())
        coVerify {
            settingsDataStore.setComposerSelectionForSession(
                workspaceClient.workspace,
                "session-1",
                SessionComposerSelection(model = model, variant = "high", pendingServerSync = false),
            )
        }
    }

    @Test
    fun `stale acknowledgment preserves a newer pending selection`() = runTest {
        val model = ModelInput(providerID = "openai", modelID = "gpt-5")
        coEvery {
            settingsDataStore.getComposerSelectionForSession(workspaceClient.workspace, "session-1")
        } returns SessionComposerSelection(model = model, variant = "high", pendingServerSync = true)
        coEvery { api.getProviders(any(), null) } returns reasoningProviders()
        val manager = ModelAgentManager(workspaceClient, settingsDataStore, this, sessionId = "session-1")
        manager.loadModels()
        advanceUntilIdle()

        // A newer explicit variant supersedes the pending record.
        manager.selectReasoningEffort("low")
        advanceUntilIdle()

        // A stale ack for the earlier (high) selection must not flush the newer pending (low).
        manager.markComposerSelectionSent(model, "high")
        advanceUntilIdle()

        assertEquals("low", manager.currentReasoningEffort())
        coVerify(exactly = 1) {
            settingsDataStore.setComposerSelectionForSession(
                workspaceClient.workspace,
                "session-1",
                SessionComposerSelection(model = model, variant = "low", pendingServerSync = true),
            )
        }
    }

    @Test
    fun `loadModels stale read does not clobber newer in-flight explicit selection`() = runTest {
        val staleSelection = SessionComposerSelection(
            model = ModelInput(providerID = "openai", modelID = "gpt-5"),
            variant = "high",
            pendingServerSync = true,
        )
        val readGate = CompletableDeferred<Unit>()
        coEvery {
            settingsDataStore.getComposerSelectionForSession(workspaceClient.workspace, "session-1")
        } coAnswers {
            readGate.await()
            staleSelection
        }
        coEvery { api.getProviders(any(), null) } returns reasoningProviders()

        val manager = ModelAgentManager(workspaceClient, settingsDataStore, this, sessionId = "session-1")

        // Start loadModels; it reads providers, then blocks inside the persistence read.
        manager.loadModels()
        runCurrent()
        assertFalse(readGate.isCompleted)

        // While the stale read is suspended, the user picks a newer model + reasoning effort.
        val newerModel = ModelInput(providerID = "openai", modelID = "gpt-5")
        manager.selectModel(newerModel)
        manager.selectReasoningEffort("low")
        runCurrent()

        // Release the stale storage read; it must not overwrite the newer in-memory selection.
        readGate.complete(Unit)
        advanceUntilIdle()

        assertEquals(newerModel, manager.selectedModel.value)
        assertEquals("low", manager.currentReasoningEffort())

        // A later matching ack must correspond to the newer (low) selection, not the stale one.
        manager.markComposerSelectionSent(newerModel, "low")
        advanceUntilIdle()

        assertEquals("low", manager.currentReasoningEffort())
        coVerify(exactly = 1) {
            settingsDataStore.setComposerSelectionForSession(
                workspaceClient.workspace,
                "session-1",
                SessionComposerSelection(model = newerModel, variant = "low", pendingServerSync = false),
            )
        }
    }

    @Test
    fun `newer selection cannot persist early and is the final completed write`() = runTest {
        val modelA = ModelInput(providerID = "openai", modelID = "gpt-5")
        val modelB = ModelInput(providerID = "openai", modelID = "gpt-4")
        val firstWriteStarted = CompletableDeferred<Unit>()
        val secondWriteStarted = CompletableDeferred<Unit>()
        val firstWriteGate = CompletableDeferred<Unit>()
        val secondWriteGate = CompletableDeferred<Unit>()
        // A's write cannot start B's until it completes, so call order maps 1 -> A, 2 -> B.
        val completionOrder = mutableListOf<ModelInput>()
        var callCount = 0
        coEvery {
            settingsDataStore.setComposerSelectionForSession(any(), any(), any())
        } coAnswers {
            callCount++
            when (callCount) {
                1 -> {
                    firstWriteStarted.complete(Unit)
                    firstWriteGate.await()
                    completionOrder.add(modelA)
                }
                2 -> {
                    secondWriteStarted.complete(Unit)
                    secondWriteGate.await()
                    completionOrder.add(modelB)
                }
            }
        }

        val manager = ModelAgentManager(workspaceClient, settingsDataStore, this, sessionId = "session-1")

        // First selection starts persisting and blocks mid-write (holds the serialization lock).
        manager.selectModel(modelA)
        runCurrent()
        assertTrue(firstWriteStarted.isCompleted)

        // A newer selection is made while the first write is blocked.
        manager.selectModel(modelB)
        runCurrent()
        // It is queued behind the first write; it must not start (and therefore not complete) early.
        assertFalse(secondWriteStarted.isCompleted)

        // Release the first write; the newer one now proceeds but is held open for ordering proof.
        firstWriteGate.complete(Unit)
        runCurrent()
        assertTrue(secondWriteStarted.isCompleted)
        assertTrue(completionOrder == listOf(modelA))
        assertFalse(secondWriteGate.isCompleted)

        // Release the newer write; it completes last.
        secondWriteGate.complete(Unit)
        advanceUntilIdle()

        assertEquals(listOf(modelA, modelB), completionOrder)
        coVerify(exactly = 1) {
            settingsDataStore.setComposerSelectionForSession(
                workspaceClient.workspace,
                "session-1",
                SessionComposerSelection(model = modelB, variant = null, pendingServerSync = true),
            )
        }
    }

    @Test
    fun `restored session selection survives unrelated coordinator change`() = runTest {
        val coordinator = ModelSelectionCoordinator()
        val model = ModelInput(providerID = "openai", modelID = "gpt-5")
        coEvery {
            settingsDataStore.getComposerSelectionForSession(workspaceClient.workspace, "session-1")
        } returns SessionComposerSelection(model = model, variant = "high", pendingServerSync = true)
        coEvery { api.getProviders(any(), null) } returns reasoningProviders()
        val manager = ModelAgentManager(
            workspaceClient,
            settingsDataStore,
            backgroundScope,
            sessionId = "session-1",
            modelSelectionCoordinator = coordinator,
        )
        manager.loadModels()
        advanceUntilIdle()
        runCurrent()
        assertEquals(model, manager.selectedModel.value)

        coordinator.publishActiveModel(
            workspaceClient.workspace,
            workspaceClient.generation,
            ModelInput(providerID = "anthropic", modelID = "claude-3"),
        )
        runCurrent()

        assertEquals(model, manager.selectedModel.value)
    }

    @Test
    fun `coordinator events never replace server session selection`() = runTest {
        val coordinator = ModelSelectionCoordinator()
        val serverModel = ModelInput(providerID = "openai", modelID = "gpt-5")
        coEvery { api.getProviders(any(), null) } returns reasoningProviders()
        val manager = ModelAgentManager(
            workspaceClient,
            settingsDataStore,
            backgroundScope,
            sessionId = "session-1",
            modelSelectionCoordinator = coordinator,
        )
        manager.applyServerSessionModel(SessionModelDto(id = "gpt-5", providerID = "openai", variant = "high"))
        manager.loadModels()
        advanceUntilIdle()
        runCurrent()
        assertEquals(serverModel, manager.selectedModel.value)

        coordinator.publishActiveModel(
            workspaceClient.workspace,
            workspaceClient.generation,
            ModelInput(providerID = "anthropic", modelID = "claude-3"),
        )
        runCurrent()

        assertEquals(serverModel, manager.selectedModel.value)
    }
}
