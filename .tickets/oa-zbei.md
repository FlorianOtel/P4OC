---
id: oa-zbei
status: closed
deps: []
links: []
created: 2026-05-10T09:55:02Z
type: bug
priority: 1
assignee: Jasmin Le Roux
---
# Rehydrate session repository after SSE reconnect

Problem:
SessionRepositoryImpl does not rehydrate on SSE reconnect even though OpenCodeEventSource emits OpenCodeEvent.Connected. If events are missed during network loss, the repository can continue from a stale snapshot and never catch up until manual refresh.

Evidence:
OpenCodeEventSource onOpen sets ConnectionState.Connected and emitEvent(OpenCodeEvent.Connected). SessionRepositoryImpl.acceptEvent only processes events where isSessionEvent(event) is true, and isSessionEvent excludes OpenCodeEvent.Connected. Existing hydrate paths run on init/refresh and some freshness checks, not specifically on reconnect.

UX Constraint:
After Wi-Fi/cellular switches or app resume reconnects, chat/session state should catch up automatically without missing messages, deltas, session deletes, or status transitions.

Expected Behavior:
A successful SSE reconnect triggers a forced/background hydrate for affected workspace repositories before or while resuming live event reduction, preserving the SSE-hydrate race lock semantics.

Acceptance Criteria:
- Handle OpenCodeEvent.Connected or ConnectionState.Connected in repository/store layer to trigger hydrate(force=true) or equivalent catch-up.
- Avoid redundant hydrate storms when multiple tabs share the same workspace repository.
- Preserve HydrationEventBuffer semantics for events arriving during the reconnect hydrate.
- Add tests proving reconnect triggers hydration and missed REST state is incorporated.

Verification:
Run SessionRepository tests and ./gradlew :app:compileDebugKotlin. Manually simulate SSE reconnect and verify missed session/message changes appear.


## Notes

**2026-05-10T11:11:16Z**

Handled OpenCodeEvent.Connected in SessionRepositoryImpl by starting a background hydrate when no hydrate is already in flight. The repository enters Hydrating immediately so session events arriving during reconnect are buffered and replayed over the fetched snapshot. Added SessionRepositoryImpl tests for missed REST state after reconnect and SSE event replay during reconnect hydrate. Verified with ./gradlew :app:testDebugUnitTest --tests dev.blazelight.p4oc.data.session.SessionRepositoryImplTest and ./gradlew :app:compileDebugKotlin.
