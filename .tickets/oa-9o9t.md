---
id: oa-9o9t
status: closed
deps: []
links: []
created: 2026-05-10T09:50:29Z
type: bug
priority: 1
assignee: Jasmin Le Roux
---
# Use lifecycle-managed ViewModels in TabNavHost

Problem:
TabNavHost manually constructs Android ViewModel subclasses inside remember blocks. Manually created ViewModels are not attached to a ViewModelStore, so onCleared() is not called and viewModelScope jobs can leak.

Evidence:
TabNavHost creates SessionListViewModel(workspaceViewModel.sessionRepository) inside remember(workspaceViewModel) for Sessions and SessionsFiltered routes. It creates FilesViewModel(FileRepositoryFactory.create(...)) inside remember(workspaceViewModel) for Files and FileViewer routes. These classes extend ViewModel and launch viewModelScope coroutines.

UX Constraint:
Navigating between routes/tabs must not leak collectors, duplicate work, or keep stale screen state alive. ViewModel lifetime should be explicit and lifecycle-managed.

Expected Behavior:
SessionListViewModel and FilesViewModel are provided through Koin/ViewModelProvider with parameters, or converted to non-ViewModel state holders if manually remembered.

Acceptance Criteria:
- Remove manual construction of ViewModel subclasses from remember blocks in TabNavHost.
- Provide lifecycle-managed Koin viewModel definitions/factories for parameterized SessionListViewModel and FilesViewModel, or refactor them out of Android ViewModel inheritance.
- Ensure parameter scoping includes workspace/session repository identity and route-specific filters where needed.
- Verify onCleared() runs when the owning route/tab is actually destroyed.

Verification:
Run ./gradlew :app:compileDebugKotlin and navigate repeatedly between Sessions/Files/FileViewer while checking logs or tests for no duplicate collectors.


## Notes

**2026-05-10T10:32:45Z**

Removed manual remember-based construction of SessionListViewModel and FilesViewModel from TabNavHost. Added parameterized Koin ViewModel definitions for SessionListViewModel(SessionRepositoryImpl) and FilesViewModel(FileRepository), and resolve them with each route NavBackStackEntry as ViewModelStoreOwner plus workspace/route-aware keys. Route/tab teardown now goes through ViewModelProvider so onCleared/viewModelScope cleanup is lifecycle-managed. Verification: export JAVA_HOME=/usr/lib/jvm/java-17-openjdk && ./gradlew :app:compileDebugKotlin. Manual repeated Sessions/Files/FileViewer navigation smoke test still recommended.
