---
id: oa-4olr
status: closed
deps: []
links: [oa-9ev3, oa-n1fs, oa-x9pe]
created: 2026-05-10T09:56:06Z
type: task
priority: 3
assignee: Jasmin Le Roux
---
# Evaluate removing custom terminal InputConnection wrapper

Problem:
TermuxTerminalView wraps Termux TerminalView with custom KeyInterceptingContainer and TerminalInputView that implements InputConnection behavior and translates keys/text manually. If Termux TerminalView can handle software keyboard input directly, this wrapper adds OEM keyboard compatibility risk and maintenance cost.

Evidence:
TermuxTerminalView.kt defines KeyInterceptingContainer and TerminalInputView. TerminalInputView overrides onCreateInputConnection(), commitText(), deleteSurroundingText(), and sendKeyEvent(), translating Android key events into terminal input. The underlying com.termux.view.TerminalView is already a terminal widget designed for keyboard input.

UX Constraint:
Terminal input must work across Gboard, Samsung Keyboard, SwiftKey, hardware keyboards, IME composition, arrows/control keys, delete/backspace, paste, and special terminal escape sequences. Do not regress core terminal editing.

Expected Behavior:
Use Termux TerminalView's native input handling where possible. Keep custom wrapper code only for documented Compose interop gaps or specific missing keys, with tests/manual matrix.

Acceptance Criteria:
- Verify whether TerminalView can own focus and IME input directly inside AndroidView Compose interop.
- If native input works, delete or reduce KeyInterceptingContainer/TerminalInputView.
- If custom input remains necessary, document exactly why and add focused handling tests/manual verification notes.
- Preserve terminal resize/focus behavior and content descriptions/test tags.

Verification:
Run ./gradlew :app:compileDebugKotlin and manually test terminal input on software and hardware keyboard scenarios.


## Notes

**2026-07-07T21:21:20Z**

Evaluation result: do not remove the custom TerminalInputView/KeyInterceptingContainer wrapper in this change. Verified through the current implementation and termux-view public API (javap on terminal-view 0.118.0) that TerminalView does expose native onCreateInputConnection/onKeyDown/onKeyUp and text-selection support, but the app wrapper is currently carrying Compose interop behavior that is not trivially redundant: it owns a hidden focusable text-editor view to reliably summon the IME from AndroidView taps, translates common hardware/navigation keys into terminal escape sequences, preserves the existing ctrl/alt extra-key path through TerminalScreen.wrappedKeyInput, and delegates long-press selection to TerminalView by returning false from TerminalViewClient.onLongPress. Removing it would require a device/IME matrix across Gboard, Samsung Keyboard, SwiftKey, hardware keyboards, deletion/composition/arrows/control keys, paste, focus after tab switch, and rotation. Given oa-x9pe paste and oa-n1fs lifecycle changes now depend on the existing focus/session path, the safer decision is to keep the wrapper and document that native TerminalView input may be revisited only with a full manual matrix. Verification for the keep decision: ./gradlew :app:compileDebugKotlin and ./gradlew :app:detekt passed after terminal changes.
