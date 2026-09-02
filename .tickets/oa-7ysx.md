---
id: oa-7ysx
status: closed
deps: []
links: []
created: 2026-05-01T17:44:25Z
type: task
priority: 1
assignee: Jasmin Le Roux
parent: oa-gt0g
tags: [design, workspace]
---
# Design lock A: deep link migration + permission/question modality

Decide: (a) what happens to old chat/{sessionId}?directory={directory} deep links — explicit error screen vs silent drop; (b) permission/question dialogs — per-tab modal or app-modal? If global event fires permission for workspace A while tab B is active, where does prompt appear? Document with concrete examples.

## Acceptance Criteria

1) Decision document written (in openspec/ or docs/). 2) Each open question has ONE chosen behavior + ONE rejected alternative documented. 3) Concrete worked examples: old deep link, perm event for background tab, perm event after owning tab closed, server switch mid-prompt. 4) Names exact files affected. 5) No 'TBD' or 'follow-up' for cutover-critical items.


## Notes

**2026-05-01T18:19:06Z**

Decision locked. See docs/design-locks/A-deep-links-and-prompts.md
