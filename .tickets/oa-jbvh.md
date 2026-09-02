---
id: oa-jbvh
status: closed
deps: []
links: []
created: 2026-05-09T15:43:07Z
type: bug
priority: 2
assignee: Jasmin Le Roux
---
# Add running indicator for sub-agent sessions

Sub-agent sessions do not show a clear progress/running indicator while active. In main sessions, the Stop button effectively acts as a visible proxy for in-progress state, but sub-agent session UI lacks an equivalent status signal.\n\nSteps to reproduce:\n1. Open or start a session that launches/uses a sub-agent.\n2. Observe the main session while it is running.\n3. Observe the sub-agent session UI while the sub-agent is running.\n\nExpected: sub-agent sessions show a visible running/progress indicator comparable to the main session's in-progress affordance.\n\nActual: sub-agent sessions provide no clear visible indication that they are currently running or processing.\n\nAcceptance criteria:\n- Sub-agent sessions display a clear running/progress indicator while active.\n- The indicator appears/disappears based on the sub-agent run state.\n- The indicator is visually consistent with existing session status/progress UI.\n- Main session Stop button behavior remains unchanged.


## Notes

**2026-05-09T15:52:52Z**

Standardization note: sub-agent progress indicator should maximize agent transcript space. Do not add a persistent bulky banner unless needed. Preferred complete UX: compact status glyph/pill in sub-agent list rows, consistent status dot/color semantics, and an optional parent-session aggregate like 'N sub-agents running' only if it fits existing metadata surfaces. No fake percentages; use real run state with spinner/pulse/text. Indicator must appear/disappear from actual sub-agent run state and be accessible.

**2026-05-10T12:16:26Z**

Implemented broad shared-status approach for sub-agent running indicators. Added reusable SessionUiState.presence(...) helper and promoted MessageError.isAborted() to the domain model so status resolution is shared. SessionListViewModel now derives sessionPresences from repository statuses, and SessionListScreen renders all session rows, including sub-agent rows, with shared SessionStatusDot/SessionStatusRow instead of custom raw glyph/status branching. This gives sub-agent rows a compact accessible running indicator from the same SessionPresence semantics as normal sessions. ChatViewModel tab presence derivation now uses the shared SessionUiState.presence helper. Verification: JAVA_HOME=/usr/lib/jvm/java-17-openjdk ./gradlew :app:compileDebugKotlin passed.
