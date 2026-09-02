---
id: oa-jvyb
status: closed
deps: []
links: []
created: 2026-05-10T09:41:27Z
type: bug
priority: 1
assignee: Jasmin Le Roux
---
# Make HydrationEventBuffer thread-safe

Problem:
HydrationEventBuffer stores SSE events in a plain ArrayDeque while SessionRepositoryImpl can buffer incoming SSE events and replay/clear the buffer during REST hydration. If these paths overlap, ArrayDeque mutation during replay/clear can produce ConcurrentModificationException or dropped/reordered events.

Evidence:
app/src/main/java/dev/blazelight/p4oc/data/session/HydrationEventBuffer.kt uses a mutable ArrayDeque with unsynchronized size, buffer(), replayOver(), and clear(). SessionRepositoryImpl.acceptEvent() calls hydrateBuffer.buffer(event), while hydrate() later calls hydrateBuffer.replayOver(...) and hydrateBuffer.clear(). Design lock B depends on this buffer preserving SSE hydrate-then-stream race semantics.

UX Constraint:
Users should not see missing session updates, stuck busy state, or crashes during app start/reconnect/session hydration. Do not expose raw protocol state in UI errors.

Expected Behavior:
Hydration buffering and replay are concurrency-safe. Events accepted during hydration are either included in a consistent replay snapshot or buffered for the next replay without corrupting the collection.

Acceptance Criteria:
- Protect all HydrationEventBuffer reads and writes with a single synchronization strategy.
- Preserve buffer capacity eviction behavior.
- Avoid holding locks while doing expensive or callback-heavy reducer work if possible.
- Add a focused unit test that exercises buffer/replay/clear semantics, including capacity behavior.
- Keep SessionRepositoryImpl's public behavior unchanged.

Verification:
Run ./gradlew :app:testDebugUnitTest --tests '*HydrationEventBuffer*' and ./gradlew :app:compileDebugKotlin.

