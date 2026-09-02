---
id: oa-es45
status: closed
deps: []
links: [oa-a6l7, oa-1n6h, oa-t4t2, oa-764s, oa-p7ei]
created: 2026-04-19T13:57:20Z
type: task
priority: 2
assignee: Jasmin Le Roux
external-ref: pr-3-cherrypick
tags: [networking, manifest, quick-win]
---
# Networking plumbing: shared ConnectionPool, predictive back, nullable VcsInfo.branch

Small plumbing wins from PR #3 that are each ~1 line but improve things. Bundle together because each is too small for its own PR.

## Changes

### 1. Shared OkHttp ConnectionPool
In ConnectionManager.buildBaseOkHttpClient, add:
```kotlin
private val sharedConnectionPool = ConnectionPool(
    maxIdleConnections = 10,
    keepAliveDuration = 5,
    timeUnit = TimeUnit.MINUTES
)
```
Apply to the base client builder so HTTP/SSE/WS all share it.

### 2. enableOnBackInvokedCallback
AndroidManifest.xml <application>: add `android:enableOnBackInvokedCallback="true"`.
Android 13+ predictive back preview animation. Compose BackHandler already handles this correctly, no code change needed.

### 3. Nullable VcsInfo.branch
`data class VcsInfoDto(val branch: String? = null)` in ProjectDtos.kt.
Defensive fix — avoid MissingFieldException on projects without VCS initialized.
Update any mapper consumers to handle null branch.

## Do NOT include

- Context in ConnectionManager constructor (PR #3 added it for disk cache + native pool, both rejected)
- OkHttp disk Cache
- Forced protocols(HTTP_2, HTTP_1_1)
- Duplicate api.health() pre-warm
- Retrofit downgrade

## Verification

- ./gradlew :app:compileDebugKotlin
- Install on POCO, try swipe-back from any screen, should see peek preview on Android 14+
- Smoke test: connect, open a project without git (if available) and verify no crash

## Acceptance Criteria

1. Compiles cleanly
2. App runs, connects, no regression on reverse proxy + self-signed TLS (PR #1 feature must still work)
3. Swipe-back shows predictive preview on Android 14+
4. No crash on project with null VCS branch

