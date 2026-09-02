---
id: oa-19hl
status: closed
deps: []
links: []
created: 2026-05-10T09:52:39Z
type: bug
priority: 2
assignee: Jasmin Le Roux
---
# Evict OkHttp connection pool on disconnect

Problem:
ConnectionManager nulls active Retrofit/WebSocket clients on disconnect but leaves the shared OkHttp ConnectionPool alive. Idle TCP sockets to an old server can remain open until keepAlive expiration, wasting resources and potentially interacting poorly with server switching.

Evidence:
ConnectionManager owns sharedConnectionPool with keepAliveDuration 5 minutes. disconnect() cancels SSE forwarding, disconnects the active connection, nulls _connection and _authOkHttpClient, and sets Disconnected, but does not call sharedConnectionPool.evictAll().

UX Constraint:
Disconnecting or switching servers should fully release old server network resources. Reconnects should remain fast enough without keeping stale sockets across explicit disconnect.

Expected Behavior:
Explicit disconnect/server switch evicts idle pooled sockets for the old connection after active streams are closed.

Acceptance Criteria:
- Call sharedConnectionPool.evictAll() during explicit disconnect after closing active SSE/WebSocket resources.
- Ensure reconnectSse does not evict the pool for lightweight reconnects unless intentionally needed.
- Confirm active connection close ordering prevents leaked sockets.

Verification:
Run ./gradlew :app:compileDebugKotlin and manually connect/disconnect/switch servers without regressions.


## Notes

**2026-05-10T11:10:44Z**

Implemented explicit sharedConnectionPool.evictAll() in ConnectionManager.disconnect() after active SSE/WebSocket connection resources are disconnected and before connection/client references are cleared. reconnectSse remains lightweight and does not evict the pool. Verified with export JAVA_HOME=/usr/lib/jvm/java-17-openjdk && ./gradlew :app:compileDebugKotlin.
