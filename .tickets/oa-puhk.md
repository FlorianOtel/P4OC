---
id: oa-puhk
status: closed
deps: []
links: []
created: 2026-05-10T09:41:44Z
type: bug
priority: 1
assignee: Jasmin Le Roux
---
# Remove TerminalView reference from TerminalViewModel

Problem:
TerminalViewModel stores a WeakReference<TerminalView> and directly calls postInvalidate() on the Android View. This crosses the ViewModel/UI boundary and can miss invalidations when Compose recreates the AndroidView or when tabs/backgrounding detach the view.

Evidence:
app/src/main/java/dev/blazelight/p4oc/ui/screens/terminal/TerminalViewModel.kt has terminalViewRef: WeakReference<TerminalView>?, attachTerminalView(), clearTerminalView(), and multiple terminalViewRef?.get()?.postInvalidate() calls. The comment at onTextChanged says the view is invalidated directly via postInvalidate.

UX Constraint:
Terminal rendering must stay responsive across tab switches, recomposition, background/foreground, and terminal input/output without leaking or retaining Android Views from a ViewModel.

Expected Behavior:
TerminalViewModel exposes an invalidation signal as data/events. The Compose/AndroidView layer owns the TerminalView instance and collects the signal to call postInvalidate() on the current attached view.

Acceptance Criteria:
- Remove WeakReference<TerminalView> and TerminalView imports from TerminalViewModel.
- Expose a SharedFlow or equivalent one-shot invalidation event from TerminalViewModel.
- TermuxTerminalView.kt or the AndroidView owner collects invalidation events and invalidates the current view instance.
- Preserve all current terminal output/input behavior and emulator state ownership.
- Add or update focused tests where practical for invalidation signal emission.

Verification:
Run ./gradlew :app:compileDebugKotlin and manually verify terminal output updates after switching away from and back to a terminal tab.


## Notes

**2026-05-10T11:08:21Z**

Removed TerminalView ownership from TerminalViewModel. ViewModel now exposes terminalInvalidations SharedFlow and accepts measured terminal row/col changes; Compose/AndroidView layer owns TerminalView invalidation and measurement. Verified with ./gradlew :app:compileDebugKotlin.
