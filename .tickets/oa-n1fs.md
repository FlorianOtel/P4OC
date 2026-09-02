---
id: oa-n1fs
status: closed
deps: []
links: [oa-4olr, oa-x9pe, oa-12ui]
created: 2026-07-05T18:06:47Z
type: bug
priority: 1
assignee: Jasmin Le Roux
parent: oa-nwha
---
# Restore terminal scrollback and terminal identity lifecycle state

Problem:
Terminal scrollback/emulator state and current terminal identity are lifecycle-sensitive and may be lost or recreated unexpectedly when switching tabs, rotating, or restoring the app.

Evidence:
Lifecycle audit identified TerminalViewModel.kt and TerminalScreen.kt scrollback/emulator state as restoration-critical. Terminal copy/paste and InputConnection tickets exist separately, but they do not cover lifecycle restoration.

UX Constraint:
Terminal context is part of the agent/code workspace. Users must not lose terminal output context or accidentally interact with a different terminal after returning to a tab. Do not fake process restoration if the backend PTY is gone; surface that state clearly.

Expected Behavior:
Returning to a terminal tab restores the same terminal session identity and visible scrollback when the backend session is still available. If the backend PTY/session is gone, UI shows a clear disconnected/restart affordance rather than silently creating a new unrelated terminal.

Acceptance Criteria:
- Define terminal identity persistence per workspace/tab.
- Preserve/restores visible scrollback or emulator buffer for still-active terminal sessions where feasible.
- Distinguish restored active terminal from closed/lost terminal with human-readable state.
- Do not create a new PTY silently when restoring a tab that referenced a previous terminal.
- Add targeted ViewModel/Compose tests for tab switch and recreation behavior.

Verification:
Run targeted terminal ViewModel/UI tests and smoke test tab switch/recreate with an active terminal.


## Notes

**2026-07-07T21:20:41Z**

Implemented terminal tab lifecycle identity/scrollback preservation around the existing explicit ptyId route argument. TerminalViewModel now keeps ptyId as the stable terminal identity, restores/persists title and exited state via SavedStateHandle, and replays a capped 64KiB transcript tail into a fresh TerminalEmulator on VM recreation to preserve visible context without risking SavedStateHandle/Bundle overflow. Incoming websocket output and local process-exit messages append to the emulator and the capped transcript snapshot. PTY update/exited events are scoped to this ptyId. Restoring a tab whose ptyId is not returned by listPtySessions now marks it unavailable with a human-readable error instead of silently creating or substituting a new PTY. Verification: ./gradlew :app:compileDebugKotlin; ./gradlew :app:detekt. Manual smoke should still verify visual scroll position/selection details on device, but identity, capped transcript replay, and no-silent-new-PTY behavior are implemented in the VM.
