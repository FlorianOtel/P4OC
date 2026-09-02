---
id: oa-uiiw
status: closed
deps: []
links: []
created: 2026-05-10T09:50:20Z
type: bug
priority: 2
assignee: Jasmin Le Roux
---
# Throttle terminal redraw invalidations to frame rate

Problem:
TerminalViewModel invalidates the TerminalView once per WebSocket output chunk. Fast terminal output can schedule far more redraws than the display frame rate and saturate the UI thread.

Evidence:
TerminalViewModel.observeWebSocketOutput() collects ptyWebSocket.output, appends bytes to TerminalEmulator, then calls terminalViewRef?.get()?.postInvalidate() for every data chunk. PtyExited and clearTerminal also invalidate directly. A separate ticket covers removing TerminalView references from the ViewModel.

UX Constraint:
Terminal output should remain responsive under high-throughput commands such as build logs or cat large files, without Compose recomposition per chunk or UI freezes.

Expected Behavior:
Terminal rendering coalesces invalidations to roughly one per frame using postOnAnimation, a frame clock, or equivalent view-layer throttling. Network/emulator ingestion remains ordered and lossless.

Acceptance Criteria:
- Coalesce multiple terminal output chunks into a single pending view invalidation per frame.
- Prefer implementing throttling in the AndroidView/view layer after removing direct View references from TerminalViewModel.
- Preserve immediate redraw for clear/exited states within the same throttling model.
- Add a stress/manual verification command for high-volume terminal output.

Verification:
Run ./gradlew :app:compileDebugKotlin and manually run a high-output terminal command, confirming UI remains responsive.


## Notes

**2026-05-10T14:43:09Z**

No implementation needed: terminal invalidations are already routed through the UI layer via terminalInvalidations, and Android/View invalidation already coalesces drawing to frame boundaries enough for current needs. Closing as not actionable unless profiling shows UI-thread churn under real terminal load.
