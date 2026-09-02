---
id: oa-dpm0
status: closed
deps: []
links: []
created: 2026-07-05T18:04:23Z
type: bug
priority: 1
assignee: Jasmin Le Roux
parent: oa-nwha
---
# Make settings and config writes refresh active runtime state

Problem:
Audit found settings/config writes that do not reliably update active chat/runtime state, plus optimistic writes whose failures are swallowed or only locally represented.

Evidence:
Beyond oa-qy0f model defaults, audited areas include ProviderConfigViewModel.kt provider default model writes, ModelControlsScreen.kt setActiveModel optimistic UI update and failure handling, ModelAgentManager.kt one-shot model/agent loading, ChatViewModel.kt runtime state not observing config changes, SettingsDataStore.kt duplicated favorites/recent model flows, and ConnectionManager.kt reconnect/escalation settings sampled during active runtime windows.

UX Constraint:
When a user changes a model/provider/agent or connection behavior, the active chat should either adopt the new state predictably or clearly explain that the change applies later. Failures must be human-readable and reversible, not silent protocol failures.

Expected Behavior:
Active runtime managers observe the authoritative settings/config source. Writes report success/failure, refresh dependent runtime state, and avoid optimistic local divergence unless there is explicit pending/error UI.

Acceptance Criteria:
- Define which settings apply immediately versus on next session/connection, and expose that behavior in UI copy where needed.
- Make active chat model/agent/runtime state observe provider config and DataStore changes relevant to that session/workspace.
- Handle setActiveModel failures by preserving previous state and showing a human-readable error.
- Avoid duplicated favorite/recent model local state that can diverge from DataStore.
- Add tests for provider default change refreshing active chat state and failed active-model writes.
- Verify reconnect/escalation settings are sampled or observed intentionally.

Verification:
Run targeted ModelAgentManager, ChatViewModel, ProviderConfigViewModel, ModelControls tests. Smoke test model/provider setting changes against an active chat where feasible.


## Notes

**2026-07-05T18:05:36Z**

Superseded by oa-qy0f. Config refresh and ModelControls optimistic-write failure handling were folded into oa-qy0f as the cohesive model/config runtime-state ticket.
