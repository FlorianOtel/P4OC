---
id: oa-dpgg
status: closed
deps: []
links: []
created: 2026-05-10T09:51:25Z
type: bug
priority: 0
assignee: Jasmin Le Roux
---
# Validate WorkspaceClient generation on every API call

Problem:
WorkspaceClient caches the OpenCodeApi returned by ActiveServerApiProvider at construction time. This means generation/server validation runs once, then later API calls can continue using an old Retrofit API after disconnect/reconnect unless the client itself is discarded.

Evidence:
WorkspaceClient constructor takes ActiveServerApiProvider, but stores private val api: OpenCodeApi = apiProvider.apiFor(workspace.server, generation). KoinModules ActiveServerApiProvider checks active ServerRef and ServerGeneration before returning connectionManager.requireApi(). Because WorkspaceClient caches api, those checks are bypassed on subsequent calls.

UX Constraint:
Workspace-scoped operations must never cross server generations or silently hit a stale server. Failures should be human-readable and should not expose raw protocol payloads.

Expected Behavior:
Every WorkspaceClient API call validates the current active server and generation before accessing OpenCodeApi, throwing a stale/inactive workspace error when the tab's client no longer matches the active connection.

Acceptance Criteria:
- Change WorkspaceClient to resolve api through ActiveServerApiProvider per call, e.g. a getter/delegate, not an eager field.
- Prefer a domain-specific StaleWorkspaceClientException or equivalent over generic check failures if not already present.
- Add tests proving a WorkspaceClient created for generation N fails after ConnectionManager advances to generation N+1.
- Ensure all methods still pass explicit workspace.directory and do not introduce fallback chains.

Verification:
Run WorkspaceClient/DI tests if present and ./gradlew :app:compileDebugKotlin.

