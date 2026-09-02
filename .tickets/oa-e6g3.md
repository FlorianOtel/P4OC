---
id: oa-e6g3
status: closed
deps: [oa-qv8d]
links: [oa-tzta, oa-ua2q, oa-casy, oa-12ui]
created: 2026-07-05T18:06:47Z
type: bug
priority: 1
assignee: Jasmin Le Roux
parent: oa-nwha
---
# Require explicit workspace when creating tabs

Problem:
Tab creation APIs can create global/null-workspace tabs by omission, making workspace identity ambiguous and risking old global-directory behavior.

Evidence:
Workspace/default audit found createTab(... workspaceDirectory: String? = null) and TabState default workspaceDirectory = null. Server-global behavior may be valid, but omission should not be indistinguishable from an intentional server-global tab.

UX Constraint:
Workspace/project identity should be visible enough to prevent wrong-directory mistakes, without adding unnecessary persistent chrome. Creation flows should not silently default to global workspace.

Expected Behavior:
Creating a chat/file/terminal/session tab requires an explicit workspace choice or an explicit server-global choice. Persisted TabState distinguishes intentional global workspace from missing/legacy workspace data.

Acceptance Criteria:
- Remove or replace nullable default workspaceDirectory arguments from tab creation APIs.
- Introduce an explicit workspace scope representation for workspace directory versus intentional server-global.
- Handle legacy persisted tabs without guessing: show explicit mismatch/recovery where needed.
- Add tests proving tab creation callers must pass workspace scope.
- Add tests proving intentional server-global tabs are represented distinctly from missing workspace state.

Verification:
Run targeted TabManager/TabState route persistence tests and compile. Smoke test creating tabs from a selected workspace.


## Notes

**2026-07-06T08:53:05Z**

Design correction from 2026-07-05 discussion:

Do NOT introduce a new WorkspaceScope/TabWorkspace sealed hierarchy. The project already has the explicit workspace identity type: domain/server/WorkspaceKey.kt with Directory(value), Global, and SessionScoped(sessionId). Existing Workspace(server, directory) derives Workspace.key, so the fix is to stop storing raw nullable String? workspaceDirectory in TabState and start storing WorkspaceKey? directly.

Target model:
- TabState should hold workspaceKey: WorkspaceKey? instead of workspaceDirectory: String?.
- WorkspaceKey.Directory(path) means explicit project/workspace context.
- WorkspaceKey.Global means intentional server-global/top-down context.
- WorkspaceKey.SessionScoped(sessionId) may be valid for session/event/cache contexts, but must be resolved or rejected before directory-required operations.
- null means missing legacy/ambiguous workspace identity that needs recovery; null does NOT mean Global.

Three-bucket UX/callsite model:
1. Fresh top-level tab/menu creation = Global/top-down view.
   - Tabs are flat, not hierarchical. A fresh Sessions tab should show all sessions/server-wide, not inherit the active tab's workspace.
   - MainTabScreen.kt:350-352 currently passes activeWorkspaceDirectory to new Sessions tab; this should change to explicit WorkspaceKey.Global.
   - MainTabScreen.kt:394-396 top-level terminal creation currently passes activeWorkspaceDirectory; it must stop implicitly inheriting. Use explicit WorkspaceKey.Global if terminal-from-menu is meant to be server default, or add an explicit workspace/server choice if product decides terminal cannot safely be global. Do not inherit by omission.
2. Contextual opens from an existing workspace/session/file keep context.
   - MainTabScreen.kt:498-500 terminal opened from a specific tab should keep that tab's explicit WorkspaceKey.
   - MainTabScreen.kt:541-543 files tab opened for a selected/open workspace should keep the selected explicit Directory key, or explicit Global only if the user chose server-global files.
   - TabNavHost.kt:343-345 sub-session chat opened from current chat should keep workspaceOwner.workspace.key.
3. Legacy/ambiguous restored workspace-critical tabs recover instead of guessing.
   - Persisted nonblank old workspaceDirectory migrates to WorkspaceKey.Directory(value).
   - Known top-level/global-safe routes may migrate to WorkspaceKey.Global.
   - Workspace-critical legacy routes with null/missing workspace become workspaceKey = null and show a recovery state asking the user to choose context; do not silently convert them to Global.

Acceptance addendum:
- TabManager.createTab requires WorkspaceKey for new calls; no default nullable workspaceDirectory arg.
- TabState persistence distinguishes explicit Global from missing legacy null.
- Tests must prove fresh top-level Sessions creation uses Global, contextual opens preserve Directory, and legacy missing workspace does not become Global.
- API/repository boundary conversion remains: Directory -> directory string, Global -> null, SessionScoped -> resolve/reject depending endpoint.

**2026-07-06T11:44:50Z**

Decision pause from 2026-07-06:

Before implementing the WorkspaceKey migration, we paused because the change affects tab bar plus-button semantics. Tester added red contract tests in TabManagerPersistenceTest targeting the desired WorkspaceKey API, but production has not been changed yet.

Brainstorm consensus:
- The architecture should eventually distinguish WorkspaceKey.Directory, WorkspaceKey.Global, WorkspaceKey.SessionScoped, and legacy/missing ambiguity.
- The plus button is a product-intent question, not just a storage refactor.
- Preserving current/mobile UX likely means plus-created Sessions and Terminal should inherit the active tab's explicit workspace key, while Files should keep a chooser; contextual opens should inherit source context.
- Avoid a mechanical migration that turns plus actions into always-Global unless that product behavior is explicitly chosen.

Recommended next decision before implementation: choose between full WorkspaceKey cutover with plus-button inheritance preserved, a smaller createTab guardrail, or deferring oa-e6g3 for another narrower P1.

**2026-07-06T12:52:16Z**

Blocked on oa-qv8d. Do not resume implementation until plus-button workspace semantics are decided and recorded; current red tests target a possible API but production migration is intentionally paused.

**2026-07-06T12:54:34Z**

Follow-up cleanup: removed the compile-red speculative WorkspaceKey contract tests from TabManagerPersistenceTest while oa-e6g3 is blocked on oa-qv8d. Left existing persistence tests compiling against current production API. Verified with ./gradlew :app:testDebugUnitTest --tests dev.blazelight.p4oc.ui.tabs.TabManagerPersistenceTest.

**2026-07-07T19:53:56Z**

Implemented by WorkspaceKey cutover and follow-up restore hardening. TabManager.createTab now requires a non-null WorkspaceKey with no nullable workspaceDirectory default; TabState stores WorkspaceKey?; persistence writes PersistedWorkspaceKey so explicit Global is distinct from missing/null; restoreState drops current-version tabs with missing workspaceKey and returns Empty if none survive; legacy migration drops unresolved tabs and retains activeTabId only when the active tab survived. Current MainTabScreen uses explicit WorkspaceKey.Global for top-level Sessions and Terminal, Files chooser offers Global and open Directory workspaces, and contextual terminal/sub-session opens preserve the source WorkspaceKey. Verification: ./gradlew :app:testDebugUnitTest --tests dev.blazelight.p4oc.ui.tabs.TabManagerPersistenceTest, ./gradlew :app:compileDebugKotlin, ./gradlew :app:detekt.

**2026-07-07T19:54:12Z**

Acceptance deviation documented after closure: the original notes proposed an in-app recovery state for workspace-critical legacy tabs with missing workspace identity. The implemented and user-approved policy is intentionally simpler: ambiguous restored tabs are dropped during migration/restore; if no tabs survive, the app falls back to the normal fresh explicit Global tab path. This still satisfies the invariant that missing workspace identity is never silently converted to Global, but recovery is reset/drop rather than an in-app chooser UI.
