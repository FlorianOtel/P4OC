---
id: oa-v3js
status: closed
deps: [oa-lmh0]
links: [oa-gtw8, oa-t3tb]
created: 2026-05-05T18:19:53Z
type: feature
priority: 1
assignee: Jasmin Le Roux
parent: oa-ssm2
tags: [files, editor, sora, phase-5]
---
# File editor: SoraEditor (AndroidView) + diff-before-save + conflict dialog

Adopt SoraEditor (https://github.com/Rosemoe/sora-editor) as the file editor. Maven coords:
  io.github.Rosemoe.sora-editor:editor:<latest>
  io.github.Rosemoe.sora-editor:language-textmate:<latest>

Pin a current version after checking releases. Council directive 1 picked Sora over BasicTextField + roll-our-own because user explicitly prioritized editing UX over APK weight. Sora gives undo/redo, line numbers, code folding, gestures (pinch-zoom, magnifier), search/replace, TextMate highlighting — all production-grade.

Architecture:
- Host via AndroidView(factory = { CodeEditor(ctx).apply { /* one-time init */ } }, update = { /* react to state */ }, onRelease = { it.release() }). onRelease prevents the View leak this kind of heavy native widget caused historically.
- Avoid recreating the editor on recomposition; treat CodeEditor as the source of truth for the editing buffer; surface only text/isDirty/cursor snapshots to the ViewModel via subscribeEvent(ContentChangeEvent::class), debounced.
- Wrap in a Box for chrome; do NOT add .clickable {} — Sora handles its own gestures.

Theming:
- Generate a TextMate theme JSON in memory from LocalOpenCodeTheme (OpenCodeTheme.kt:64-73 already enumerates syntaxComment/Keyword/Function/Variable/String/Number/Type/Operator/Punctuation). Map to TextMate scopes. Reload via ThemeRegistry whenever LocalOpenCodeTheme changes. ~80 LOC.
- Editor area uses generated TextMate theme; surrounding chrome (toolbar, file path bar, action sheet) stays on LocalOpenCodeTheme.

Save flow: 1) compute diff via java-diff-utils (see ticket for diff parser swap); 2) render diff modal using existing InlineDiffViewer; 3) on confirm, FileRepository.write with expectedHash; 4) on '### 409 conflict', show 3-pane conflict dialog (yours / theirs / common base).

Unsaved-changes back-handler: BackHandler intercepts when editor.isModified, shows confirm.

NOT v1: LSP autocomplete (defer to future ticket via :editor-lsp module). Replaces oa-t3tb.

## Acceptance Criteria

SoraEditor renders inside FileViewerScreen edit mode. TextMate theme matches LocalOpenCodeTheme palette. Undo/redo work. Line numbers visible. Save shows diff modal. Stale-hash save shows conflict. Back with unsaved changes shows confirm. Manual test: open .env, change a line, save, verify content on disk matches (use ofish read after save). APK size delta documented in PR.


## Notes

**2026-05-05T18:25:53Z**

LICENSE FLAG (user caught): SoraEditor is LGPL-2.1, not Apache-2.0. This is acceptable for a Play-Store-distributed Android app but creates compliance obligations:

1. Add an 'Open source licenses' screen in the app that lists SoraEditor (and other deps) with full LGPL-2.1 text bundled.
2. Display prominent notice that the LGPL library is used (typically inside the licenses screen).
3. Offer the unstripped library object code on request OR reference the upstream unmodified AAR (practical interpretation for Android: 'this is io.github.Rosemoe.sora-editor:editor:<version>, source at github.com/Rosemoe/sora-editor').
4. DO NOT fork/modify SoraEditor's code — modifications must be LGPL-released back. Theme integration must happen via the public ThemeRegistry/EditorColorScheme APIs only.
5. R8/ProGuard shrinking is fine; static linking via Gradle is fine; Play Store distribution is fine.

If LGPL compliance is unwanted, alternatives:
- CodeView (Apache-2.0, simpler, regex-based highlighting only)
- BasicTextField + helpers (Apache-2.0, slower to ship, no syntax highlighting in edit mode v1)

Council missed this — adding to acceptance criteria of this ticket: 'License compliance ticket filed and verified before this ticket closes.'
