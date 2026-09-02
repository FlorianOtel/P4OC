---
id: oa-0r8n
status: closed
deps: []
links: []
created: 2026-05-10T09:51:33Z
type: bug
priority: 1
assignee: Jasmin Le Roux
---
# Raise OkHttp per-host limits for persistent SSE and PTY sockets

Problem:
The app uses persistent SSE and PTY WebSocket connections to the same host. OkHttp's default Dispatcher maxRequestsPerHost is 5, so several terminal tabs plus SSE can starve ordinary REST calls behind long-lived connections.

Evidence:
ConnectionManager builds REST/SSE/WebSocket clients from a shared base OkHttpClient/ConnectionPool but does not configure Dispatcher.maxRequestsPerHost. SSE holds a long-lived connection, and each terminal tab uses a WebSocket. OkHttp defaults can queue additional requests per host when the limit is reached.

UX Constraint:
Opening multiple terminal tabs must not make chat sends, project/session loading, file operations, or settings API calls hang indefinitely.

Expected Behavior:
ConnectionManager configures an OkHttp Dispatcher appropriate for multiple persistent connections per server, and all derived clients share or use a compatible dispatcher policy.

Acceptance Criteria:
- Set maxRequestsPerHost high enough for expected concurrent SSE/WebSocket/REST usage, with a documented rationale.
- Verify derived REST/SSE/WebSocket clients use the intended Dispatcher policy.
- Preserve connection pooling and auth behavior.
- Add a test or manual verification scenario with SSE + multiple terminals + REST request.

Verification:
Run ./gradlew :app:compileDebugKotlin. Manually open several terminal tabs and verify session/project/file REST calls still complete.
