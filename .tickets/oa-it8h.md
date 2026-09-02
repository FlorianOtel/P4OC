---
id: oa-it8h
status: closed
deps: [oa-pecx]
links: []
created: 2026-03-05T19:51:12Z
type: feature
priority: 1
assignee: Jasmin Le Roux
tags: [ui, chat]
---
# Message revert/unrevert controls

Add per-message revert button on assistant messages with tool calls using POST /session/:id/revert. Show sticky banner when revert active with Unrevert button (POST /session/:id/unrevert). RevertSessionRequest DTO exists. Show TuiConfirmDialog before revert.

