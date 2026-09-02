---
id: oa-7uuf
status: closed
deps: []
links: []
created: 2026-05-10T09:55:57Z
type: chore
priority: 3
assignee: Jasmin Le Roux
---
# Simplify mDNS service resolution with coroutine Mutex

Problem:
MdnsDiscoveryManager manually serializes Android NSD resolve calls using ConcurrentLinkedQueue, isResolving flags, and callback-driven queue polling. This is custom state-machine code for a one-at-a-time constraint that Kotlin coroutines can express more simply.

Evidence:
MdnsDiscoveryManager has resolveQueue = ConcurrentLinkedQueue<NsdServiceInfo>(), @Volatile isResolving, enqueueResolve(), startNextResolveLocked/resolve callbacks, and a separate Semaphore for seed HTTP probes. Android NSD only allows one resolve at a time.

UX Constraint:
mDNS discovery should remain reliable without duplicate or stuck resolves. Discovery failures should remain logged/human-readable and not block manual server entry.

Expected Behavior:
Use structured concurrency, such as Mutex.withLock around a suspend resolveService wrapper or a single-worker Channel, instead of manual queue/boolean state.

Acceptance Criteria:
- Replace manual resolveQueue/isResolving state with Mutex or Channel worker.
- Preserve service found/lost behavior and cancellation on stop.
- Preserve probe concurrency limits where useful.
- Add tests or manual verification for multiple discovered services in quick succession.

Verification:
Run ./gradlew :app:compileDebugKotlin and manually test mDNS discovery where possible.


## Notes

**2026-05-10T11:55:26Z**

Replaced MdnsDiscoveryManager manual resolveQueue/isResolving state machine with per-discovery resolve jobs serialized by a coroutine Mutex. NSD resolve is wrapped in suspendCancellableCoroutine; stopDiscovery cancels the resolve parent job; service found/lost behavior and seed probe Semaphore concurrency are preserved. Verification: JAVA_HOME=/usr/lib/jvm/java-17-openjdk ./gradlew :app:compileDebugKotlin passed. Manual mDNS discovery test not run in this CLI session.
