---
id: oa-5sro
status: closed
deps: [oa-6swf, oa-yx4y, oa-07lr, oa-fac4, oa-nugm, oa-pjcl, oa-1xnu, oa-cj0w]
links: []
created: 2026-07-08T14:42:17Z
type: task
priority: 1
assignee: Jasmin Le Roux
parent: oa-xju6
---
# Approval gate: staged rollout and visual QA

Before shipping, perform staged verification and visual QA for the pinned Home + Start Work architecture.

## Acceptance Criteria

- Compile, detekt, and relevant unit/UI tests pass.
- Manual/visual QA covers: first connect, restore existing chat, Home server carousel, workspace drill-in, + from active chat, + from Home workspace detail, browse sessions, server auth failure, server removal with open tabs, app background/reconnect.
- Screenshots demonstrate Home = existing work and + = new work distinction.
- Approval recorded before release branch merge.


## Verification Notes

- 2026-07-08: `JAVA_HOME=/usr/lib/jvm/java-17-openjdk ./gradlew :app:compileDebugKotlin && JAVA_HOME=/usr/lib/jvm/java-17-openjdk ./gradlew :app:detekt && JAVA_HOME=/usr/lib/jvm/java-17-openjdk ./gradlew :app:testDebugUnitTest` passed.
- 2026-07-08: Manual/visual QA is blocked because `adb devices` returned no connected emulator/device. Do not close until screenshots cover the required 11 scenarios.
- 2026-07-08: `emulator -list-avds` found `Pixel7`, but booting it failed because x86_64 emulation requires hardware acceleration and `/dev/kvm` is unavailable. `adb wait-for-device` was cancelled after the emulator failure. Visual QA remains blocked until a physical device or KVM-capable emulator is available.
- Planned screenshot walkthrough once device is available: first connect; restore existing chat; Home server carousel; workspace drill-in; + from active chat; + from Home workspace detail; browse sessions; server auth failure; server removal with open tabs; app background/reconnect; Home vs + distinction.
- 2026-07-09: Device visual QA completed on `192.168.24.119:47293` after installing the current debug build. Screenshot evidence is intentionally kept uncommitted under `local-adb-screenshots/oa-5sro-visual-qa/`:
  - First connect: `01_first_connect_server_screen.png`, `02_first_connect_retry.png`, `02_first_connect_home_server_summary.png`.
  - Restore existing chat: `10_restore_existing_chat.png`.
  - Home server carousel / saved server summary: `02_first_connect_home_server_summary.png` and `12_home_server_workspace_open_work.png`.
  - Workspace drill-in: `13_workspace_drill_in.png`.
  - `+` from active chat: `11_plus_from_active_chat.png`.
  - `+` from Home workspace detail: `14_plus_from_workspace_detail.png`.
  - Browse sessions: `15_browse_sessions.png` and `tmp_sessions_for_disconnect.png`.
  - Server auth failure: `02_first_connect_home_server_carousel.png` captured the unauthorized state before retrying with the verified password.
  - Server removal with open tabs: `17_server_removal_open_tabs_warning.png` shows saved server `Remote Server` with `warn remove` while an open tab references that endpoint.
  - App background/reconnect: `16_background_reconnect.png`.
  - Home = existing work versus `+` = new work distinction: `07_connected_home.png`, `08_plus_from_home_start_work.png`, and `09_new_chat_from_home.png`.
- 2026-07-09: QA server/auth verified before visual pass: unauthenticated `GET /config` returned `401`, authenticated `opencode:hunter2` returned `200`, and device TCP reachability to `192.168.24.25:4096` succeeded.
- 2026-07-09: Final verification passed: `JAVA_HOME=/usr/lib/jvm/java-17-openjdk ./gradlew :app:compileDebugKotlin && JAVA_HOME=/usr/lib/jvm/java-17-openjdk ./gradlew :app:detekt && JAVA_HOME=/usr/lib/jvm/java-17-openjdk ./gradlew :app:testDebugUnitTest`.
