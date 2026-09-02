---
id: oa-t3tb
status: closed
deps: [oa-s5jj]
links: [oa-gtw8, oa-v3js]
created: 2026-05-05T17:57:03Z
type: feature
priority: 1
assignee: Jasmin Le Roux
parent: oa-ssm2
tags: [files, editor, phase-5]
---
# File editor: BasicTextField view/edit toggle + diff-before-save + conflict dialog

Add view/edit toggle to FileViewerScreen top bar (next to existing line-number toggle at FileViewerScreen.kt:63-77). Edit mode swaps SyntaxHighlightedCode for BasicTextField (or BasicTextField2) with monospace text, IME, scroll-into-view, undo/redo via TextFieldState.undoState. Manual line-number gutter (~30 LOC). Code-context toolbar above keyboard: Tab, {}, (), [], ;.

NO live syntax highlighting in edit mode (would need VisualTransformation + incremental re-highlight; defer to future). Plain monospace.

Save flow: 1) compute diff (yours vs server's current content) using a tiny inline Myers diff or java-diff-utils — team to confirm dependency policy; 2) render diff in modal using existing InlineDiffViewer (InlineDiffViewer.kt:172-196); 3) on confirm, call FileRepository.write with expectedHash from initial read; 4) on '### 409 conflict' result, show 3-pane conflict dialog (yours / theirs / common base) — user picks. 5) Never silently overwrite.

Unsaved-changes back-handler: BackHandler intercepts and shows confirm dialog if dirty.

Defer to v2: live syntax highlighting in edit mode, autoindent, bracket matching, search-in-file, goto-line, SoraEditor swap.

## Acceptance Criteria

Edit toggle works. Typing dirties state. Save shows diff modal first. Stale-hash save shows conflict dialog with current+yours+base. Back with unsaved changes shows confirm. Save+exit reloads viewer with fresh content. No new heavyweight editor library; only BasicTextField + (maybe) diff-utils.


## Notes

**2026-05-05T18:19:53Z**

Closed: superseded. Council directive 1 picked SoraEditor over BasicTextField — user explicitly prioritized editing UX over APK weight. New ticket follows.
