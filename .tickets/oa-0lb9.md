---
id: oa-0lb9
status: closed
deps: []
links: []
created: 2026-05-10T09:55:10Z
type: task
priority: 2
assignee: Jasmin Le Roux
---
# Sweep stale OFISH sessions on startup or hydration

Problem:
OFISH creates hidden background sessions for probes/mutations and relies on finally cleanup. If Android kills the app mid-operation, those sessions can remain on the OpenCode server. The stale sweep implementation exists but is not called.

Evidence:
OfishSessionFactory.sweepStaleSessions(maxAgeMillis, limit) exists and filters OFISH-prefixed sessions, but repository search finds no production call sites. OFISH session names use the __ofish_ prefix. Capability/chunk/file operations create ephemeral OFISH sessions.

UX Constraint:
Users should not see server session lists polluted by old hidden OFISH sessions, and cleanup should not delete active or user-visible sessions. Cleanup failures should be logged/human-readable only where appropriate, not block normal app startup.

Expected Behavior:
The app periodically or opportunistically sweeps stale OFISH sessions for the active server/workspace lifecycle, such as on successful connection, hydration, or before first OFISH use.

Acceptance Criteria:
- Call sweepStaleSessions from an appropriate workspace/server-scoped lifecycle point.
- Use conservative max age and limit values to avoid deleting active probes.
- Ensure sweep runs at most once per connection/workspace interval to avoid network spam.
- Log sweep results and failures without surfacing raw protocol errors to users.
- Add tests for stale session selection if feasible.

Verification:
Run OFISH/session tests and ./gradlew :app:compileDebugKotlin. Manually create/leave a stale OFISH session and verify sweep removes it while preserving active sessions.


## Notes

**2026-05-10T11:16:18Z**

Wired stale OFISH session sweeping into OfishSessionFactory.withSession() before ephemeral session creation. Sweep is conservative: sessions must be at least 6 hours old, list is capped by existing default limit, active in-process OFISH sessions are skipped, and a process-wide per server/workspace guard runs at most once every 30 minutes. Sweep results/failures are logged only. Added unit coverage for once-per-workspace interval behavior. Verified with export JAVA_HOME=/usr/lib/jvm/java-17-openjdk && ./gradlew :app:testDebugUnitTest --tests dev.blazelight.p4oc.data.files.ofish.OfishSessionFactoryTest and ./gradlew :app:compileDebugKotlin.
