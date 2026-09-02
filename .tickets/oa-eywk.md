---
id: oa-eywk
status: closed
deps: [oa-pecx]
links: []
created: 2026-03-05T19:51:10Z
type: feature
priority: 1
assignee: Jasmin Le Roux
tags: [ui, sessions, diff]
---
# Session diff viewer screen

New screen showing cumulative file changes for a session using GET /session/:id/diff. Wire into session context menu and chat top bar action. Reuse existing InlineDiffViewer and DiffViewerScreen components. FileDiffDto and getSessionDiff() API already defined.

