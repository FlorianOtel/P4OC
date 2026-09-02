---
id: oa-r8yn
status: closed
deps: []
links: []
created: 2026-05-09T15:57:46Z
type: feature
priority: 2
assignee: Jasmin Le Roux
---
# Add status dot legend to Settings Help

Problem:\nStatus dots and run indicators appear across tabs, sessions, sub-agents, chat, files, and connection UI, but users do not have a single in-app explanation of what the colors/motion mean.\n\nEvidence:\nCurrent code uses dots/spinners in multiple places including chat connection state, tab state, session list state, and dirty file title markers. Semantics are partially centralized but still not exposed to users.\n\nUX Constraint:\nDo not add persistent explanatory chrome to agent/chat/file surfaces. The legend belongs in Settings -> Help so it explains the system without consuming workspace space.\n\nExpected Behavior:\nSettings -> Help includes a concise status legend explaining connected/idle, running/busy, awaiting user input, retrying/reconnecting, error, background/cold, and dirty/unsaved states.\n\nAcceptance Criteria:\n- Settings -> Help includes a status indicator legend.\n- Legend matches the centralized status dot semantics in AGENTS.md and app code.\n- Running/busy uses real run state only; no fake percentages.\n- Awaiting-user state is distinguishable from generic running.\n- Dirty/unsaved file marker is documented.\n- The legend does not add persistent UI chrome to chat/session/file screens.\n\nVerification:\n- Open Settings -> Help and verify each status state is explained.\n- Verify labels/colors match the current theme/state mapping.


## Notes

**2026-05-10T11:24:21Z**

Implemented Settings > Help status indicator legend with entries for connected/idle, running/busy, awaiting user input, retrying/reconnecting, error, background/cold, and dirty/unsaved. Legend uses existing theme status colors and does not add persistent chrome to chat/session/file screens. Verified with export JAVA_HOME=/usr/lib/jvm/java-17-openjdk && ./gradlew :app:compileDebugKotlin.
