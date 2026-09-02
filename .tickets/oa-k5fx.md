---
id: oa-k5fx
status: closed
deps: []
links: []
created: 2026-05-05T18:19:53Z
type: bug
priority: 2
assignee: Jasmin Le Roux
tags: [terminal, resize]
---
# Send PTY resize updates to server (PATCH /pty/{id})

TerminalViewModel.kt:107-124 has the server PTY resize call commented out with a stale comment claiming the API doesn't exist. The endpoint DOES exist:
  - OpenCodeApi.kt:309-313: @PATCH('pty/{id}') updatePtySession(id, UpdatePtyRequest)
  - PtyDtos.kt:30-33: UpdatePtyRequest(title: String?, size: PtySizeDto?)
  - PtyDtos.kt:35-39: PtySizeDto(rows, cols)

Fix:
1. Delete stale comment at TerminalViewModel.kt:107-109.
2. Restore the launch block at lines 110-124, importing UpdatePtyRequest/PtySizeDto cleanly.
3. Wrap in a 150ms debounce on (rows, cols) tuple — soft-keyboard slide animations spam dimension changes; the existing lastKnownCols/Rows guard at lines 94-99 catches identical-value spam but not animation-frame intermediate values.
4. Log AppLog.w on PATCH failure (NOT error) so local emulator resize never fails. Local resize at line 104 must always run regardless of server outcome.

Test: run opencode serve, attach terminal, rotate device + open soft keyboard, run `tput cols` / `stty size` inside the PTY — confirm values match local emulator.

## Acceptance Criteria

Resize sent to server. Verified via stty size matching local emulator. Soft-keyboard slide doesn't spam PATCH (debounce confirmed). PATCH failure does not break local rendering.


## Notes

**2026-05-05T18:41:38Z**

Implemented. TerminalViewModel.kt — added pendingResize: MutableStateFlow<Pair<Int,Int>?> + observeResizeRequests() collector with .debounce(150ms) under @OptIn(FlowPreview::class). Calls connectionManager.getApi().updatePtySession(...) directly (WorkspaceClient does not wrap PTY endpoints; PATCH /pty/{id} takes only @Path id, no directory query). PATCH failures log AppLog.w. Local emulator resize at line 104 still runs synchronously and unconditionally. Build green.
