---
id: oa-p7ei
status: closed
deps: []
links: [oa-a6l7, oa-1n6h, oa-t4t2, oa-764s, oa-es45]
created: 2026-04-19T13:58:18Z
type: feature
priority: 1
assignee: Jasmin Le Roux
external-ref: pr-3-cherrypick
tags: [networking, perf, sessions]
---
# Unify connect probe + session prefetch via shared Deferred — drop /health, save ~700ms

## Error handling

- listProjects returns 401/403 → auth failure, ConnectionState.Error, disconnect
- listProjects returns 404 / network failure → server not reachable or not OpenCode, ConnectionState.Error
- listProjects returns 200 with [] → valid empty project list, proceed, no sessions to prefetch
- prewarm failure (network flap mid-prefetch) → log warn, SessionListVM falls back to fresh fetch on open
- Disconnect mid-prefetch → invalidate() cancels Deferred, no stale state

## Connect flow changes

ConnectionManager.connect:
- Replace `val healthResult = runCatching { api.health() }` with `val probeResult = runCatching { withTimeout(8000) { api.listProjects() } }`
- On success, keep projects around; pass them to cache.prewarm as seed so the cache doesn't re-fetch projects
- DO NOT add Context constructor param (rejected)
- DO NOT add OkHttp disk Cache (rejected)
- DO NOT add duplicate api.health() pre-warm (rejected)

ServerViewModel.connect callsite:
- After connectionManager.connect succeeds, call sessionDataCache.prewarm(projectsFromProbe)
- Rely on current SSE connection state flow for UI → no change

ConnectionManager.disconnect:
- Call sessionDataCache.invalidate() before clearing _connection

## Expected win

On 5-project server with 100ms RTT:
- -1 RTT from removing health call
- -1 full fan-out from Deferred sharing (VM doesn't re-fetch)
- -(N-1) × RTT from parallelizing statuses
Total: roughly 700–900ms from tap Connect to sessions populated

## Blocks on

- oa-t4t2 (PR E benchmarks) for baseline measurement

## Verify

- Run StartupBenchmark + tap-connect-to-sessions-rendered trace before and after
- Manual: connect, should see sessions appear immediately after nav animation
- Manual: disconnect mid-prefetch, reconnect — no stale data
- Manual: switch from server A to server B, verify no A's sessions shown after B connects

Kill the dedicated /health round-trip on connect. Use listProjects() as the probe — it both validates the server is OpenCode-shaped AND returns data we need for the session list. Share the in-flight Deferred between ServerViewModel (prewarm) and SessionListViewModel (consume) so we never duplicate the fan-out.

## Current flow (main)

```
connect() → api.health()                    ~RTT
         → buildSseClient + store connection
         → navigate to Sessions             ~300ms anim
         → SessionListVM.loadSessions()     listProjects + N parallel listSessions
         → SessionListVM.loadSessionStatuses()  N sequential getSessionStatuses  ← wasteful
```

## Proposed flow

```
connect()  → withTimeout(8000) { api.listProjects() }   ← this IS the probe, returns projects
           → start SSE
           → cache.prewarm(projects)       fires listSessions fan-out + statuses in parallel
           → complete Result.success
ServerVM navigates
SessionListVM.init → cache.awaitOrFetch()  ← joins in-flight Deferred, no duplicate requests
```

## New: SessionDataCache

File: app/src/main/java/dev/blazelight/p4oc/core/network/SessionDataCache.kt

Responsibilities:
- Singleton (Koin)
- Hold @Volatile Deferred<Result<CachedSessions>>?
- prewarm(seedProjects) kicks off fan-out, stores Deferred
- awaitOrFetch() returns the in-flight Deferred's result (or starts a new one if null)
- invalidate() cancels in-flight + clears on disconnect
- Server-identity check via connectionManager.currentBaseUrl (add this getter)
- 30s freshness window for reconnect
- **Concurrency limit 10 in-flight, NO project count cap** — use Semaphore(10) around each async{} so all projects prefetch, just throttled

Data shape:
```kotlin
data class CachedSessions(
    val sessions: List<SessionWithProject>,
    val projects: List<ProjectInfo>,
    val statuses: Map<String, SessionStatus>,  // NEW — cache statuses too
    val fetchedAtMs: Long,
    val serverBaseUrl: String
)
```

## Also in this PR

### Parallelize loadSessionStatuses
Currently SessionListViewModel.loadSessionStatuses runs N sequential getSessionStatuses calls (one per project + global). Convert to async/awaitAll with the same Semaphore(10) gate. This is a pure bug fix — no sequential reason.

### withTimeout(8000) on the connect probe
Wraps listProjects instead of health.

### api.health() stays defined
Unused in connect path, available for future test

## Acceptance Criteria

1. Sessions screen paints immediately on first nav after connect (LAN)
2. No duplicate network requests between prewarm and SessionListVM
3. On-disconnect invalidate cancels pending Deferred
4. Server-switch shows correct server's sessions only
5. Benchmark delta >300ms improvement vs baseline (from PR E)
6. PR #1 self-signed TLS toggle still works
7. Unit tests for SessionDataCache Deferred-sharing semantics
8. No regression on sequential loadSessionStatuses (now parallel)

