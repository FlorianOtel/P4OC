---
id: oa-3vga
status: closed
deps: []
links: []
created: 2026-05-10T09:45:25Z
type: bug
priority: 1
assignee: Jasmin Le Roux
---
# Make SSE reconnection policy single-owner

Problem:
SSE reconnection is controlled by both LaunchDarkly BackgroundEventSource retry behavior and UI-layer timers in MainTabScreen. Parallel reconnect policies can race, tearing down a recovering stream or causing duplicated reconnect attempts/log noise.

Evidence:
OpenCodeEventSource configures ErrorStrategy.alwaysContinue() and retryDelay(3, TimeUnit.SECONDS). MainTabScreen observes ConnectionState and, after reconnectTimeoutSeconds or Disconnected recovery delays, calls connectionManager.reconnectSse(reason = ...). Foreground resume also calls reconnectSse from UI state.

UX Constraint:
Network drops should produce stable, understandable connection state without flickering, duplicate reconnect spam, or unnecessary navigation back to the server screen. User settings such as autoReconnect and reconnectTimeoutSeconds should still be honored in one place.

Expected Behavior:
OpenCodeEventSource/ConnectionManager own retry, timeout, and final disconnect/escalation policy. MainTabScreen reacts to connection states and navigates only on terminal disconnected states; it does not run competing retry timers.

Acceptance Criteria:
- Define one reconnection owner and move timeout/escalation logic there.
- Remove UI-layer delayed reconnect loops from MainTabScreen, except lifecycle foreground hooks if explicitly justified and race-safe.
- Preserve autoReconnect=false behavior.
- Preserve human-readable connection error states/settings.
- Add tests or fakes for retry exhaustion/escalation where feasible.

Verification:
Run ./gradlew :app:compileDebugKotlin and manually test server stop/start or network loss to confirm reconnection behavior and navigation.


## Notes

**2026-05-10T10:25:08Z**

Made ConnectionManager the single owner for SSE reconnect timeout/escalation. MainTabScreen now delegates foreground resume to ConnectionManager and only reacts to terminal Disconnected states; it no longer runs delayed reconnect loops or final explicit reconnect attempts. ConnectionManager reads connectionSettings for autoReconnect and reconnectTimeoutSeconds, cancels escalation when SSE recovers, escalates Error to Disconnected via the active OpenCodeEventSource, and preserves LaunchDarkly retry ownership. Verification: export JAVA_HOME=/usr/lib/jvm/java-17-openjdk && ./gradlew :app:compileDebugKotlin; ./gradlew :app:testDebugUnitTest --tests dev.blazelight.p4oc.core.network.OpenCodeEventSourceTest. Manual server stop/start still recommended for final release gate.
