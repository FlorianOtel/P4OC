---
id: oa-6swf
status: closed
deps: []
links: []
created: 2026-07-08T14:42:17Z
type: task
priority: 1
assignee: Jasmin Le Roux
parent: oa-xju6
---
# Approval gate: pinned Home plus Start Work UX spec

Produce and get explicit approval for the detailed UX contract before implementation.

Spec must be grounded in actual app flows, not generic dashboard visuals:
- Home opens existing/resumable work.
- + creates/opens new work from current context.
- Workspace click opens Home workspace detail rather than immediately creating a tab.
- Sessions list remains available as filtered history/browse inside Home/workspace detail.
- Notifications remain badges/dots only.

## Design

Use these local prototypes as design evidence/input:
- local-adb-screenshots/home-workspace-detail-plus.html
- local-adb-screenshots/pinned-home-app-structure-variants.html
- local-adb-screenshots/pinned-home-ux-variants.html

Preferred direction from exploration:
- Top-level Home: server carousel/status filters, recent workspace cards, sessions list.
- Workspace detail: workspace identity, open work in this workspace, filtered sessions, small Start new here row.
- + sheet: target prefilled from active tab or selected Home workspace, actions New chat / Files tab / Terminal / Choose another target.

## Acceptance Criteria

- Approved UX doc describes exact Home top-level sections, workspace detail sections, and + sheet behavior.
- Back behavior is specified: Home top -> workspace detail -> filtered sessions returns within Home, not tab-close behavior.
- Empty/offline/auth-required states are specified.
- Decision is recorded whether + stays separate because it is context-fast, or is merged into Home if not.

