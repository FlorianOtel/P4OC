---
id: oa-qu7u
status: closed
deps: []
links: []
created: 2026-05-05T17:48:39Z
type: feature
priority: 1
assignee: Jasmin Le Roux
parent: oa-ssm2
tags: [files, viewer, phase-1]
---
# Tier A: wrap viewer body in SelectionContainer (selectable file text)

Wrap only the body Text inside SyntaxHighlightedCode (SyntaxHighlighter.kt:501-513) in a SelectionContainer so users can select and copy file body text in FileViewerScreen. Add 'selectable: Boolean = false' parameter; default false so chat code blocks are unaffected; pass true from FileViewerScreen.kt:94. Do NOT wrap line-number gutter. User quote: 'A smaller enhancement is select / copy text from file. ... for setting a secret I need to use the agent explaining the exact line to be changed.'

## Acceptance Criteria

Long-press on file body in viewer offers system Copy. Line numbers do not enter the selection. Chat code blocks unchanged (no inadvertent selection in tool widgets / diff viewers). Manual test on phone: select 'API_KEY=...' from a .env-like file, copy, paste into another app.


## Notes

**2026-05-05T18:41:38Z**

Implemented. SyntaxHighlighter.kt — added selectable: Boolean = false param to SyntaxHighlightedCode. When true, body Text wrapped in SelectionContainer (line-number gutter outside). FileViewerScreen.kt — passes selectable = true. CodeSnippet untouched, defaults to false (chat code blocks unaffected). Build green.
