---
id: oa-qy0f
status: closed
deps: []
links: [oa-nwha, oa-12ui, oa-casy, oa-3yk2, oa-wmvc, oa-wxf2, oa-dygk]
created: 2026-07-05T16:59:51Z
type: bug
priority: 1
assignee: Jasmin Le Roux
parent: oa-casy
---
# Fix model agent defaults and config refresh contracts

Problem:
Model/agent selection mixes Android-side fallbacks with upstream/server defaults and does not refresh active chat state after settings writes. Red tests should lock the intended source-of-truth contract before production changes.

Evidence:
- ModelAgentManager.kt:62-72 filters to primary agents and falls back to persisted agent, agent named build, then first primary agent.
- ModelAgentManager.kt:115-130 selects models from recents/defaults during one-shot loadModels.
- ProviderConfigViewModel.kt:71-82 writes api.updateConfig(model = provider/model) but only updates its own UI state.
- ChatViewModel.kt:168-173 creates ModelAgentManager once and calls loadAgents/loadModels at init.
- ChatViewModel.kt:312-324 sends messages using modelAgentManager.selectedModel.value.
- ModelControlsScreen.kt:123-136 optimistically updates selected model and ignores the ApiResult from api.setActiveModel.
- Contract audit found current tests blur good server-default contracts with risky implementation policy such as hardcoded build and first reasoning variant.

UX Constraint:
Users must be able to trust that selected/default model and agent shown in chat match the server/config or their explicit per-chat choice. Settings must not show success or let active chats continue stale model choices after writes.

Expected Behavior:
Server/config defaults are authoritative when no explicit per-chat user override exists. User explicit choices are preserved and validated against current provider/agent data. Recents are a convenience fallback only when no upstream/default choice exists. Reasoning variants are not inferred from collection order unless upstream exposes an explicit default. Active chat state refreshes or reconciles after successful config/model writes.

## Design

Separate selection sources explicitly: upstream/server default, explicit user override, persisted per-session override, recent convenience fallback, and no selection/server-decides. Prefer a shared observable model/config source over one-shot loads. Avoid sending model/variant fields when the intended behavior is to let the server decide.

## Acceptance Criteria

- Add failing red tests proving server/provider default model wins over stale recent model unless the user made an explicit per-chat override.
- Add failing red tests proving reasoning effort/variant is not inferred from first available variant unless an explicit upstream/user default exists.
- Add failing red tests for ProviderConfigViewModel or shared repository behavior where changing default model refreshes/reconciles an active ChatViewModel/ModelAgentManager.
- Add failing red tests for ModelControls selectModel failure rollback or error handling; optimistic UI cannot swallow failed writes.
- Production fix removes hardcoded build as an unconditional default unless upstream config names it explicitly.
- Verification: run targeted ModelAgentManager/ProviderConfig/ModelControls tests and ./gradlew :app:compileDebugKotlin.


## Notes

**2026-07-05T18:05:19Z**

Broader model/config audit findings folded into this ticket on 2026-07-05:

Keep this as the cohesive fix for model/agent defaults, stale runtime after config writes, and ModelControls optimistic-write failure handling. Do not create separate duplicate tickets for provider default refresh or ModelControls rollback unless implementation later proves they need independent sequencing.

Additional evidence/scope:
- ProviderConfigViewModel writes provider default model through api.updateConfig but only updates its own UI state.
- ChatViewModel creates ModelAgentManager once and sends messages using selectedModel from that manager, so active chats can remain stale after config/settings writes.
- ModelControlsScreen optimistically changes selected model and ignores failed setActiveModel ApiResult.
- SettingsDataStore/favorites/recent model state can diverge from local UI state if not observed as source of truth.
- Connection/reconnect settings sampled during active windows should be intentionally sampled or observed; do not let runtime silently diverge.

Acceptance addendum:
- Active chat must either refresh/reconcile when provider/model/agent defaults change or clearly indicate changes apply only to future chats.
- setActiveModel failure must preserve previous selected state and surface a human-readable error.
- Duplicate local model/favorite/recent state should be removed or made derived from the authoritative flow.

**2026-07-06T10:10:42Z**

Progress update from 2026-07-06:

Implemented and verified the mapped red failure for server/provider default precedence in ModelAgentManager.loadModels. Selection now chooses an available server default before falling back to available recent models. Targeted ModelAgentManagerTest passed, and the focused red suite dropped from 7 failures to 6: `ModelAgentManagerTest > loadModels prefers server default over app recent model` now passes.

Verification run:
- JAVA_HOME=/usr/lib/jvm/java-17-openjdk ./gradlew :app:testDebugUnitTest --tests dev.blazelight.p4oc.ui.screens.chat.ModelAgentManagerTest -> PASS
- JAVA_HOME=/usr/lib/jvm/java-17-openjdk ./gradlew :app:compileDebugKotlin -> PASS
- JAVA_HOME=/usr/lib/jvm/java-17-openjdk ./gradlew :app:detekt -> PASS after line-length/import-order cleanup in red tests
- Focused four-complaint red suite still fails intentionally with 6 remaining failures outside this fixed model-precedence assertion.

Do not close oa-qy0f yet. The ticket still has broader unmet acceptance: ProviderConfig/shared refresh behavior, ModelControls setActiveModel failure rollback/error handling, and hardcoded build-as-agent-default policy need tests and production fixes.

**2026-07-06T10:35:21Z**

Completion update from 2026-07-06:

Completed the full model/config defaults and refresh contract.

Production changes:
- ModelAgentManager no longer hardcodes `build` as an unconditional default agent; persisted session agent still wins, otherwise server order chooses the first primary non-hidden agent.
- ModelAgentManager tracks explicit user model selection separately from agent-provided/default selection. Server/provider default now wins over stale recents when there is no explicit override; recents are only fallback. Explicit still-available user model choices survive reload; unavailable explicit choices reconcile to server default/fallback.
- Added ModelSelectionCoordinator as the shared active-model refresh seam. Koin provides a singleton and wires it to ChatViewModel, ModelControlsViewModel, and ProviderConfigViewModel.
- ModelControlsViewModel only updates selectedModelId after successful setActiveModel(true), rolls back/preserves previous selection on false/error/no API/missing model, surfaces human-readable errors, and publishes successful active model changes.
- ProviderConfigViewModel updates currentModel only after updateConfig succeeds, preserves previous state on failure, clears error on success, and publishes successful provider/model config changes.
- Chat ModelAgentManager instances collect coordinator activeModelChanges and reconcile selectedModel when no explicit user or agent model override is active.

Tests added/updated:
- ModelAgentManagerTest covers server default vs recents, no reasoning-effort inference, server-order agent default instead of hardcoded build, explicit model preservation/reconciliation, and coordinator publish -> selectedModel reconciliation with explicit/agent guard cases.
- ModelControlsViewModelTest covers success, false success rollback, API error rollback, no API/missing model rollback, and coordinator publishing only after successful API update.
- ProviderConfigViewModelTest covers no optimistic currentModel update, failure preservation/error, and coordinator publishing only after successful updateConfig.

Verification:
- ./gradlew :app:testDebugUnitTest --tests dev.blazelight.p4oc.ui.screens.chat.ModelAgentManagerTest --tests dev.blazelight.p4oc.ui.screens.settings.ModelControlsViewModelTest --tests dev.blazelight.p4oc.ui.screens.settings.ProviderConfigViewModelTest -> PASS
- ./gradlew :app:compileDebugKotlin -> PASS
- ./gradlew :app:detekt -> PASS
- ./gradlew :app:testDebugUnitTest -> expected FAIL from six remaining red tests mapped to oa-wmvc, oa-wxf2, and oa-3yk2; no oa-qy0f failures remained.
