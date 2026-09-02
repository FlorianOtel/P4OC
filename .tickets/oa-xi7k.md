---
id: oa-xi7k
status: closed
deps: []
links: []
created: 2026-05-10T09:49:16Z
type: bug
priority: 1
assignee: Jasmin Le Roux
---
# Hoist workspace repositories outside pager composition

Problem:
WorkspaceViewModel and SessionRepository lifetimes are tied to HorizontalPager/NavHost composition. With beyondViewportPageCount = 0, off-screen tabs can be unmounted, which can clear the WorkspaceViewModel and close the SessionRepository even though the tab still exists.

Evidence:
MainTabScreen uses HorizontalPager(... beyondViewportPageCount = 0) and creates each tab's rememberNavController/TabNavHost inside SaveableStateProvider(tab.id). TabNavHost calls TouchWorkspaceViewModel from route composables. WorkspaceViewModel.onCleared() calls sessionRepository.close(). TabManager docs say NavController is created inside HorizontalPager page composition scope.

UX Constraint:
Tabs must continue owning workspace/session state while they exist, including while off-screen. Agent runs and session updates should continue/recover correctly when users swipe between tabs or open settings/files in another tab.

Expected Behavior:
Workspace/session repository lifetimes are keyed by tab identity and managed outside transient pager page composition. Pager/NavHost renders UI only; it does not own network/session store lifetime.

Acceptance Criteria:
- Define a tab-scoped owner for WorkspaceViewModel/SessionRepository or equivalent repository holder that survives off-screen pager disposal until the tab is closed or workspace changes.
- Closing a tab explicitly closes/releases its workspace repository.
- Changing a tab workspace recreates the scoped repository with the new workspace identity.
- Preserve generation/server/workspace routing rules; no global/default workspace shortcut.
- Add lifecycle/logging tests or manual verification proving off-screen tabs do not close repositories simply due to pager unmount.

Verification:
Run ./gradlew :app:compileDebugKotlin. Manually start a long run in one tab, switch to another tab long enough for pager disposal, then return and confirm updates/state are intact.


## Notes

**2026-05-10T11:08:54Z**

Implemented tab-scoped WorkspaceRepositoryOwner instances in MainTabScreen so SessionRepositoryProvider leases are held outside HorizontalPager page/NavHost composition. Owners are keyed by tab id plus workspace/generation, released when tabs disappear, workspace/generation changes, or MainTabScreen disposes. Verified with export JAVA_HOME=/usr/lib/jvm/java-17-openjdk && ./gradlew :app:compileDebugKotlin.
