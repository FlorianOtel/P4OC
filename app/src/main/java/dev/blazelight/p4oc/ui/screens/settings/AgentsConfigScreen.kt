package dev.blazelight.p4oc.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dev.blazelight.p4oc.R
import dev.blazelight.p4oc.core.network.ApiResult
import dev.blazelight.p4oc.core.network.safeApiCall
import dev.blazelight.p4oc.data.workspace.WorkspaceClient
import dev.blazelight.p4oc.ui.components.TuiAlertDialog
import dev.blazelight.p4oc.ui.components.TuiBadge
import dev.blazelight.p4oc.ui.components.TuiButton
import dev.blazelight.p4oc.ui.components.TuiLoadingScreen
import dev.blazelight.p4oc.ui.components.TuiTextButton
import dev.blazelight.p4oc.ui.components.TuiTopBar
import dev.blazelight.p4oc.ui.theme.LocalOpenCodeTheme
import dev.blazelight.p4oc.ui.theme.SemanticColors
import dev.blazelight.p4oc.ui.theme.Sizing
import dev.blazelight.p4oc.ui.theme.Spacing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AgentInfo(
    val name: String,
    val description: String,
    val systemPrompt: String? = null,
    val tools: List<String> = emptyList(),
    val isBuiltIn: Boolean = true
)

data class AgentsConfigState(
    val agents: List<AgentInfo> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedAgent: AgentInfo? = null
)

class AgentsConfigViewModel constructor(
    private val workspaceClient: WorkspaceClient,
    serverConnectionRegistry: dev.blazelight.p4oc.core.network.ServerConnectionRegistry? = null,
) : ViewModel() {

    private val _state = MutableStateFlow(AgentsConfigState())
    val state: StateFlow<AgentsConfigState> = _state.asStateFlow()

    init {
        loadAgents()
        serverConnectionRegistry?.observeWorkspaceCatalogEvents(
            workspaceClient,
            viewModelScope,
            includeMcp = true,
        ) {
            loadAgents(background = true)
        }
    }

    fun loadAgents() = loadAgents(background = false)

    private fun loadAgents(background: Boolean) {
        viewModelScope.launch {
            if (!background) _state.update { it.copy(isLoading = true) }
            val result = safeApiCall { workspaceClient.getAgents() }
            when (result) {
                is ApiResult.Success -> {
                    val agents = result.data.map { dto ->
                        AgentInfo(
                            name = dto.name,
                            description = dto.description ?: "",
                            systemPrompt = dto.systemPrompt,
                            tools = dto.tools?.keys?.toList() ?: emptyList(),
                            isBuiltIn = dto.isBuiltIn ?: dto.builtIn
                        )
                    }
                    _state.update { it.copy(agents = agents, isLoading = false, error = null) }
                }
                is ApiResult.Error -> {
                    if (!background) {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                error = "Could not load agents. Check the connection and try again.",
                            )
                        }
                    }
                }
            }
        }
    }

    fun selectAgent(agent: AgentInfo?) {
        _state.update { it.copy(selectedAgent = agent) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("FunctionNaming", "LongMethod", "NoNameShadowing")
@Composable
fun AgentsConfigScreen(
    viewModel: AgentsConfigViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val theme = LocalOpenCodeTheme.current
    Scaffold(
        containerColor = theme.background,
        topBar = {
            TuiTopBar(
                title = stringResource(R.string.agents_title),
                onNavigateBack = onNavigateBack,
                actions = {
                    IconButton(
                        onClick = { viewModel.loadAgents() },
                        modifier = Modifier.size(Sizing.iconButtonMd)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.refresh),
                            modifier = Modifier.size(Sizing.iconAction)
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            TuiLoadingScreen(
                modifier = Modifier.padding(padding)
            )
        } else if (state.agents.isEmpty() && state.error != null) {
            val theme = LocalOpenCodeTheme.current
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    Text(
                        text = "✗",
                        style = MaterialTheme.typography.displayMedium,
                        color = theme.error
                    )
                    Text(
                        text = stringResource(R.string.agents_failed_to_load),
                        style = MaterialTheme.typography.titleMedium,
                        color = theme.error
                    )
                    TuiButton(onClick = { viewModel.loadAgents() }) {
                        Text(stringResource(R.string.retry))
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                item {
                    val theme = LocalOpenCodeTheme.current
                    Text(
                        text = stringResource(R.string.agents_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = theme.textMuted,
                        modifier = Modifier.padding(bottom = Spacing.md)
                    )
                }

                val builtInAgents = state.agents.filter { it.isBuiltIn }
                val customAgents = state.agents.filter { !it.isBuiltIn }

                if (builtInAgents.isNotEmpty()) {
                    item {
                        val theme = LocalOpenCodeTheme.current
                        Text(
                            text = stringResource(R.string.agents_builtin),
                            style = MaterialTheme.typography.titleSmall,
                            color = theme.accent
                        )
                    }

                    items(builtInAgents, key = { it.name }) { agent ->
                        AgentCard(
                            agent = agent,
                            onClick = { viewModel.selectAgent(agent) }
                        )
                    }
                }

                if (customAgents.isNotEmpty()) {
                    item {
                        val theme = LocalOpenCodeTheme.current
                        Spacer(Modifier.height(Spacing.md))
                        Text(
                            text = stringResource(R.string.agents_custom),
                            style = MaterialTheme.typography.titleSmall,
                            color = theme.accent
                        )
                    }

                    items(customAgents, key = { it.name }) { agent ->
                        AgentCard(
                            agent = agent,
                            onClick = { viewModel.selectAgent(agent) }
                        )
                    }
                }

                if (state.agents.isEmpty()) {
                    item {
                        EmptyAgentsView()
                    }
                }
            }
        }
    }

    state.selectedAgent?.let { agent ->
        AgentDetailDialog(
            agent = agent,
            onDismiss = { viewModel.selectAgent(null) }
        )
    }
}

@Suppress("FunctionNaming")
@Composable
internal fun AgentToolLabel(tool: String) {
    TuiBadge(text = tool)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AgentCard(
    agent: AgentInfo,
    onClick: () -> Unit
) {
    val theme = LocalOpenCodeTheme.current
    val agentColor = getAgentColor(agent.name)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RectangleShape,
        color = theme.backgroundElement,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.Top
        ) {
            // Square status dot in the agent's color (design 14)
            Box(
                modifier = Modifier
                    .padding(top = Spacing.xs)
                    .size(Sizing.indicatorDotActive)
                    .background(agentColor, RectangleShape)
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "@${agent.name}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        ),
                        fontWeight = FontWeight.SemiBold,
                        color = theme.text
                    )
                    if (agent.isBuiltIn) {
                        Surface(shape = RectangleShape, color = agentColor) {
                            Text(
                                text = stringResource(R.string.agents_builtin),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                ),
                                color = theme.background,
                                modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xxs)
                            )
                        }
                    }
                }
                Text(
                    text = agent.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = theme.textMuted,
                    maxLines = 2,
                    modifier = Modifier.padding(top = Spacing.xxs)
                )

                if (agent.tools.isNotEmpty()) {
                    Spacer(Modifier.height(Spacing.xs))
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        agent.tools.take(3).forEach { tool ->
                            AgentToolLabel(tool)
                        }
                        if (agent.tools.size > 3) {
                            Text(
                                text = "+${agent.tools.size - 3}",
                                style = MaterialTheme.typography.labelSmall,
                                color = theme.textMuted,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AgentDetailDialog(
    agent: AgentInfo,
    onDismiss: () -> Unit
) {
    val theme = LocalOpenCodeTheme.current
    TuiAlertDialog(
        onDismissRequest = onDismiss,
        icon = getAgentIcon(agent.name),
        title = agent.name,
        confirmButton = {
            TuiTextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    ) {
        Text(agent.description)
        AgentTools(agent.tools)
        agent.systemPrompt?.let { AgentSystemPrompt(it, theme.backgroundElement) }
    }
}

@Composable
@Suppress("FunctionNaming")
private fun AgentTools(tools: List<String>) {
    if (tools.isEmpty()) return
    val theme = LocalOpenCodeTheme.current
    Text(
        text = stringResource(R.string.agents_tools),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Medium,
    )
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        tools.forEach { tool ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.Build,
                    contentDescription = stringResource(R.string.agents_tools),
                    modifier = Modifier.size(Sizing.iconXs),
                    tint = theme.textMuted,
                )
                Text(text = tool, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
@Suppress("FunctionNaming")
private fun AgentSystemPrompt(prompt: String, backgroundColor: Color) {
    Text(
        text = stringResource(R.string.agents_system_prompt),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Medium,
    )
    Surface(shape = RectangleShape, color = backgroundColor) {
        Text(
            text = prompt.take(500) + if (prompt.length > 500) "..." else "",
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            ),
            modifier = Modifier.padding(Spacing.lg),
        )
    }
}

@Composable
private fun EmptyAgentsView() {
    val theme = LocalOpenCodeTheme.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Text(
            text = "◇",
            style = MaterialTheme.typography.displayMedium,
            color = theme.textMuted
        )
        Text(
            text = stringResource(R.string.agents_no_agents),
            style = MaterialTheme.typography.titleMedium,
            color = theme.text
        )
        Text(
            text = stringResource(R.string.agents_appear_here),
            style = MaterialTheme.typography.bodyMedium,
            color = theme.textMuted
        )
    }
}

private fun getAgentIcon(name: String) = when (name.lowercase()) {
    "coder", "coding" -> Icons.Default.Code
    "researcher", "research" -> Icons.Default.Search
    "writer", "writing" -> Icons.Default.Edit
    "analyst", "analysis" -> Icons.Default.Analytics
    "planner", "planning" -> Icons.Default.DateRange // EventNote is deprecated, use DateRange instead
    "reviewer", "review" -> Icons.Default.RateReview
    "debugger", "debug" -> Icons.Default.BugReport
    "explorer", "explore" -> Icons.Default.Explore
    else -> Icons.Default.SmartToy
}

@Composable
private fun getAgentColor(name: String): Color = SemanticColors.Agent.forName(name)
