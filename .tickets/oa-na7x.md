---
id: oa-na7x
status: closed
deps: []
links: []
created: 2026-05-10T09:49:26Z
type: bug
priority: 0
assignee: Jasmin Le Roux
---
# Prevent SSE event buffer from dropping delta events

Problem:
OpenCodeEventSource uses MutableSharedFlow buffers with BufferOverflow.DROP_OLDEST. For delta-based message streams, silently dropping older events can corrupt assistant text or session state if consumers lag.

Evidence:
OpenCodeEventSource declares _events and _directoryEvents with replay = 0, extraBufferCapacity = 256, onBufferOverflow = BufferOverflow.DROP_OLDEST. SessionRepositoryImpl.applyDelta appends message part delta text, so lost MessagePartUpdated events can mean lost characters/words.

UX Constraint:
Chat output must never silently corrupt streamed text. If the app cannot keep up, it should apply backpressure, recover through hydration, or surface a human-readable sync/reconnect state rather than showing incomplete text as if valid.

Expected Behavior:
SSE event delivery to repository-owned state is lossless for ordered delta streams, or has explicit recovery semantics that rehydrates before rendering final state.

Acceptance Criteria:
- Replace DROP_OLDEST with a lossless/backpressured strategy or introduce explicit overflow recovery that rehydrates affected sessions.
- Preserve stale generation protection and directory routing.
- Confirm LaunchDarkly callback threading is not blocked in a way that deadlocks shutdown/reconnect; if SUSPEND cannot be used directly from callback code, use a dedicated channel/actor with safe backpressure.
- Add a test or stress harness that emits more than the previous buffer capacity of ordered delta events and verifies no text is lost.

Verification:
Run new SSE buffer tests and ./gradlew :app:compileDebugKotlin.


## Notes

**2026-05-10T10:17:39Z**

Implemented lossless ordered SSE event pump: LaunchDarkly callbacks enqueue into an internal unlimited channel, and a single IO coroutine emits to zero-buffer SharedFlows with suspension/backpressure instead of DROP_OLDEST. Added OpenCodeEventSourceTest stress coverage for 300 ordered message.part.updated deltas with a slow collector. Verification: ./gradlew :app:testDebugUnitTest --tests dev.blazelight.p4oc.core.network.OpenCodeEventSourceTest; export JAVA_HOME=/usr/lib/jvm/java-17-openjdk && ./gradlew :app:compileDebugKotlin
