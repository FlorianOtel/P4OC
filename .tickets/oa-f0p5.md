---
id: oa-f0p5
status: closed
deps: []
links: []
created: 2026-07-05T18:04:23Z
type: bug
priority: 1
assignee: Jasmin Le Roux
parent: oa-nwha
---
# Replace Android-guessed defaults with explicit upstream or user sources

Problem:
Audit found Android-side default values and fallback chains that can override upstream/server truth or hide missing context. The model default complaint is tracked by oa-qy0f, but similar defaulting occurs across terminal, tabs, settings, networking, DTOs, notifications, and file picking.

Evidence:
Additional audited examples include ChatViewModel.kt hardcoded command list, PtyDtos.kt CreatePtyRequest defaults of /bin/bash, cwd ., and title Terminal, MainTabScreen.kt/TabManager.kt new tabs defaulting to global/null workspace by omission, SettingsDataStore.kt duplicated local server defaults, ServerUrl.kt default host/port/username assumptions, ProviderDtos.kt and AgentDtos.kt booleans that turn missing upstream fields into false/true, NotificationHelper.kt default notification labels, and FilePickerManager.kt root sentinel duplicated as '.'.

UX Constraint:
Defaults should prevent friction but must not silently move work into the wrong workspace, launch the wrong shell/cwd, override server defaults, or mask upstream schema changes. Wrong-directory mistakes are especially costly on phones.

Expected Behavior:
Default values come from upstream/server config, explicit app settings, Android resources, or a documented user-visible policy. Missing context is represented as missing, not silently guessed, unless the fallback is intentional and tested.

Acceptance Criteria:
- Inventory hardcoded/default fallback sites from the audit and categorize source of truth for each.
- Remove fallback chains that guess workspace, shell, cwd, model, agent, or server identity.
- Require explicit workspace/cwd/shell when needed, or surface a clear setup/default-selection UI.
- Preserve nullable/missing upstream DTO fields when absence is semantically different from false/default.
- Centralize legitimate Android defaults in one config/resource layer with tests.
- Update tests that currently rely on guessed defaults.

Verification:
Run targeted tests for model/agent defaults, PTY request creation, tab/workspace creation, DTO mapping of missing fields, and file picker roots. Run compile after implementation.


## Notes

**2026-07-05T18:05:35Z**

Superseded by oa-qy0f for model/config defaults plus narrower tickets for terminal PTY defaults and workspace/tab defaults. A broad Android-guessed defaults ticket is too large to implement as one behavior.
