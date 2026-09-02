---
id: oa-of3b
status: closed
deps: []
links: []
created: 2026-05-10T09:51:56Z
type: bug
priority: 2
assignee: Jasmin Le Roux
---
# Avoid per-frame coroutine launches in PTY WebSocket onMessage

Problem:
PtyWebSocketClient launches a new coroutine for every WebSocket text frame just to emit terminal output. High-volume terminal output can allocate thousands of coroutines per second, causing GC churn and latency.

Evidence:
PtyWebSocketClient.onMessage checks generation, logs, then calls scope.launch { _output.emit(text) }. _output is a MutableSharedFlow<String>(extraBufferCapacity = 1000), so synchronous tryEmit can usually enqueue without allocating a coroutine per message.

UX Constraint:
Terminal output must remain ordered and responsive under verbose commands without overheating/freezing the app. If terminal output is dropped due to overflow, it should be logged and ideally recoverable/visible as terminal stream loss rather than corrupting app state.

Expected Behavior:
WebSocket onMessage uses a low-allocation emission path such as tryEmit or a dedicated actor/channel, preserving generation checks and output ordering.

Acceptance Criteria:
- Remove per-message scope.launch allocation from onMessage.
- Use tryEmit or a dedicated single consumer that avoids unbounded coroutine creation.
- Decide and document overflow behavior for PTY output frames.
- Keep stale generation callbacks ignored.
- Add a stress/manual verification with high-volume terminal output.

Verification:
Run ./gradlew :app:compileDebugKotlin and manually run a verbose terminal command while monitoring responsiveness/logs.

