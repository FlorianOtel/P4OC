package dev.blazelight.p4oc.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.pluralStringResource
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
import dev.blazelight.p4oc.ui.components.TuiLoadingScreen
import dev.blazelight.p4oc.ui.components.TuiTextButton
import dev.blazelight.p4oc.ui.components.TuiTopBar
import dev.blazelight.p4oc.ui.theme.LocalOpenCodeTheme
import dev.blazelight.p4oc.ui.theme.Sizing
import dev.blazelight.p4oc.ui.theme.Spacing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SkillInfo(
    val name: String,
    val status: String,
    val errorDetail: String? = null,
    val source: String,
    val tools: List<String> = emptyList(),
    val resources: List<String> = emptyList(),
    val prompts: List<String> = emptyList()
) {
    val isEnabled: Boolean get() = status == MCP_STATUS_CONNECTED
}

data class SkillsState(
    val skills: List<SkillInfo> = emptyList(),
    val isLoading: Boolean = false,
    val error: SkillsError? = null,
    val selectedSkill: SkillInfo? = null
)

data class SkillsError(
    val kind: SkillsErrorKind,
)

enum class SkillsErrorKind {
    NotConnected,
    ApiError,
}

internal const val MCP_STATUS_CONNECTED = "connected"

internal fun mcpStatusDescriptionRes(status: String): Int = when (status) {
    MCP_STATUS_CONNECTED -> R.string.skills_status_connected
    "disabled" -> R.string.skills_status_disabled
    "failed" -> R.string.skills_status_failed
    "needs_auth" -> R.string.skills_status_needs_auth
    "needs_client_registration" -> R.string.skills_status_needs_client_registration
    else -> R.string.skills_status_unknown
}

class SkillsViewModel constructor(
    private val workspaceClient: WorkspaceClient,
    serverConnectionRegistry: dev.blazelight.p4oc.core.network.ServerConnectionRegistry? = null,
) : ViewModel() {

    private val _state = MutableStateFlow(SkillsState())
    val state: StateFlow<SkillsState> = _state.asStateFlow()

    init {
        loadSkills()
        serverConnectionRegistry?.observeWorkspaceCatalogEvents(
            workspaceClient,
            viewModelScope,
            mcpOnly = true,
        ) { loadSkills(background = true) }
    }

    fun loadSkills() = loadSkills(background = false)

    private fun loadSkills(background: Boolean) {
        viewModelScope.launch {
            if (!background) _state.update { it.copy(isLoading = true) }
            val result = safeApiCall { workspaceClient.getMcpStatus() }
            when (result) {
                is ApiResult.Success -> {
                    val skills = result.data.map { (name, status) ->
                        SkillInfo(
                            name = name,
                            status = status.status,
                            errorDetail = status.error,
                            source = "mcp",
                            tools = emptyList(),
                            resources = emptyList(),
                            prompts = emptyList()
                        )
                    }
                    _state.update { it.copy(skills = skills, isLoading = false) }
                }
                is ApiResult.Error -> {
                    if (!background) {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                error = SkillsError(SkillsErrorKind.ApiError),
                            )
                        }
                    }
                }
            }
        }
    }

    fun selectSkill(skill: SkillInfo?) {
        _state.update { it.copy(selectedSkill = skill) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("FunctionNaming", "LongMethod", "NoNameShadowing")
@Composable
fun SkillsScreen(
    viewModel: SkillsViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val errorMessage = state.error?.let { error ->
        when (error.kind) {
            SkillsErrorKind.NotConnected -> stringResource(R.string.skills_error_not_connected)
            SkillsErrorKind.ApiError -> stringResource(R.string.skills_error_generic)
        }
    }
    // Show error in snackbar
    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }

    val theme = LocalOpenCodeTheme.current
    Scaffold(
        containerColor = theme.background,
        topBar = {
            TuiTopBar(
                title = stringResource(R.string.skills_title),
                onNavigateBack = onNavigateBack,
                actions = {
                    IconButton(
                        onClick = { viewModel.loadSkills() },
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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (state.isLoading) {
            TuiLoadingScreen(
                modifier = Modifier.padding(padding)
            )
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
                        text = stringResource(R.string.skills_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = theme.textMuted,
                        modifier = Modifier.padding(bottom = Spacing.md)
                    )
                }

                val connectedSkills = state.skills.filter { it.isEnabled }
                val disconnectedSkills = state.skills.filter { !it.isEnabled }

                if (connectedSkills.isNotEmpty()) {
                    item {
                        val theme = LocalOpenCodeTheme.current
                        Text(
                            text = stringResource(R.string.skills_connected),
                            style = MaterialTheme.typography.titleSmall,
                            color = theme.accent
                        )
                    }

                    items(connectedSkills, key = { it.name }) { skill ->
                        SkillCard(
                            skill = skill,
                            onClick = { viewModel.selectSkill(skill) }
                        )
                    }
                }

                if (disconnectedSkills.isNotEmpty()) {
                    item {
                        val theme = LocalOpenCodeTheme.current
                        Spacer(Modifier.height(Spacing.md))
                        Text(
                            text = stringResource(R.string.skills_disconnected),
                            style = MaterialTheme.typography.titleSmall,
                            color = theme.error
                        )
                    }

                    items(disconnectedSkills, key = { it.name }) { skill ->
                        SkillCard(
                            skill = skill,
                            onClick = { viewModel.selectSkill(skill) }
                        )
                    }
                }

                if (state.skills.isEmpty()) {
                    item {
                        EmptySkillsView()
                    }
                }
            }
        }
    }

    state.selectedSkill?.let { skill ->
        SkillDetailDialog(
            skill = skill,
            onDismiss = { viewModel.selectSkill(null) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SkillCard(
    skill: SkillInfo,
    onClick: () -> Unit
) {
    val theme = LocalOpenCodeTheme.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RectangleShape,
        colors = CardDefaults.cardColors(containerColor = theme.backgroundElement)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RectangleShape,
                    color = if (skill.isEnabled) {
                        theme.success.copy(alpha = 0.2f)
                    } else {
                        theme.error.copy(alpha = 0.2f)
                    }
                ) {
                    Icon(
                        Icons.Default.Extension,
                        contentDescription = stringResource(R.string.cd_skill_icon),
                        modifier = Modifier.padding(Spacing.md),
                        tint = if (skill.isEnabled) {
                            theme.success
                        } else {
                            theme.error
                        }
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                    ) {
                        Text(
                            text = skill.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Surface(
                            shape = RectangleShape,
                            color = if (skill.isEnabled) {
                                theme.success.copy(alpha = 0.2f)
                            } else {
                                theme.error.copy(alpha = 0.2f)
                            }
                        ) {
                            Text(
                                text = if (skill.isEnabled) {
                                    stringResource(
                                        R.string.skills_connected
                                    )
                                } else {
                                    stringResource(R.string.skills_disconnected)
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (skill.isEnabled) {
                                    theme.success
                                } else {
                                    theme.error
                                },
                                modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xxs)
                            )
                        }
                    }
                    val statusDescription = stringResource(mcpStatusDescriptionRes(skill.status))
                    Text(
                        text = statusDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = theme.textMuted
                    )
                    skill.errorDetail?.let {
                        Text(
                            text = stringResource(R.string.skills_error_generic),
                            style = MaterialTheme.typography.labelSmall,
                            color = theme.error
                        )
                    }

                    if (skill.tools.isNotEmpty() || skill.resources.isNotEmpty()) {
                        Spacer(Modifier.height(Spacing.xs))
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                            if (skill.tools.isNotEmpty()) {
                                Text(
                                    text = pluralStringResource(
                                        R.plurals.skills_tools_count,
                                        skill.tools.size,
                                        skill.tools.size,
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = theme.accent
                                )
                            }
                            if (skill.resources.isNotEmpty()) {
                                Text(
                                    text = pluralStringResource(
                                        R.plurals.skills_resources_count,
                                        skill.resources.size,
                                        skill.resources.size,
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = theme.secondary
                                )
                            }
                        }
                    }
                }
            }

            // Read-only status — server API does not support toggling skills from client
        }
    }
}

@Composable
private fun SkillDetailDialog(
    skill: SkillInfo,
    onDismiss: () -> Unit
) {
    val theme = LocalOpenCodeTheme.current
    TuiAlertDialog(
        onDismissRequest = onDismiss,
        icon = Icons.Default.Extension,
        title = skill.name,
        confirmButton = {
            TuiTextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    ) {
        Text(stringResource(mcpStatusDescriptionRes(skill.status)))
        skill.errorDetail?.let {
            Text(stringResource(R.string.skills_error_generic))
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.skills_source),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = skill.source,
                style = MaterialTheme.typography.bodySmall
            )
        }

        if (skill.tools.isNotEmpty()) {
            Text(
                text = stringResource(R.string.skills_tools),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                skill.tools.forEach { tool ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Build,
                            contentDescription = stringResource(R.string.skills_tools),
                            modifier = Modifier.size(Sizing.iconXs),
                            tint = theme.textMuted
                        )
                        Text(
                            text = tool,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        if (skill.resources.isNotEmpty()) {
            Text(
                text = stringResource(R.string.skills_resources),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                skill.resources.forEach { resource ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Storage,
                            contentDescription = stringResource(R.string.skills_resources),
                            modifier = Modifier.size(Sizing.iconXs),
                            tint = theme.textMuted
                        )
                        Text(
                            text = resource,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptySkillsView() {
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
            text = stringResource(R.string.skills_no_skills),
            style = MaterialTheme.typography.titleMedium,
            color = theme.text
        )
        Text(
            text = stringResource(R.string.skills_appear_here),
            style = MaterialTheme.typography.bodyMedium,
            color = theme.textMuted
        )
    }
}
