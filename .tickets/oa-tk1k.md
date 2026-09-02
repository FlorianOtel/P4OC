---
id: oa-tk1k
status: closed
deps: []
links: []
created: 2026-05-10T09:55:20Z
type: bug
priority: 2
assignee: Jasmin Le Roux
---
# Handle missing or invalid restored chat sessions gracefully

Problem:
Persisted tabs can restore directly to chat routes, including sub-agent sessions. If the session was deleted, archived, or otherwise inaccessible, the tab can land on a dead route instead of recovering to a usable state.

Evidence:
Sub-agent open-in-new-tab creates a tab with startRoute = Screen.Chat.createRoute(subSessionId) and workspaceDirectory = workspace.directory. Tab state restoration persists startRoute. ChatViewModel loads session by id through repository/workspace client; missing-session behavior needs explicit graceful routing.

UX Constraint:
Restored tabs should never trap users on a broken chat. Missing sessions should show a human-readable empty/error state with an action to return to the Sessions list or close the tab.

Expected Behavior:
404/missing getSession or missing messages during restored chat load transitions the tab to a safe screen/state instead of crashing, looping, or leaving permanent loading/error UI.

Acceptance Criteria:
- Detect missing/inaccessible session during ChatViewModel/session load.
- Surface a concise human-readable message and navigation action, or automatically route to Sessions list when appropriate.
- Preserve valid restored root and sub-agent chat tabs.
- Add tests for restored chat route with missing session returning NotFound/404.

Verification:
Run ChatViewModel/navigation tests if available and ./gradlew :app:compileDebugKotlin. Manually restore a tab whose session was deleted and confirm recovery.

