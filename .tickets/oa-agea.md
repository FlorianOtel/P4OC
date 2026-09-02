---
id: oa-agea
status: closed
deps: [oa-zmqg, oa-sa63]
links: []
created: 2026-07-09T19:48:26Z
type: feature
priority: 1
assignee: Jasmin Le Roux
---
# Rebuild Home as existing-work dashboard

## Problem
Current Home is a flat launcher whose workspaces are derived only from open tabs. It omits resumable work, session recency, status/count context, and safe contextual actions from the approved prototype.

## Evidence / Repro
`HomeSummary` models open-tab-derived workspaces and routes but not session counts, recency, or previews. Current Home exposes flat Servers/Resume/Browse sections. Reference: `local-adb-screenshots/home-workspace-detail-plus.html`.

## UX Constraint
Home opens/resumes/browses existing work. `+` creates new work. Servers are filters/identity, not a hidden global mode. Notifications stay compact.

## Design

Match the information architecture, not necessarily the duplicate bottom navigation, in `home-workspace-detail-plus.html`: compact server strip, one-column workspace cards, recent sessions. Depend on durable identity and explicit scope contracts.

## Acceptance Criteria

- Home has explicit `[ Home ]` identity and a Servers management action.
- A compact All/server filter strip shows badge, friendly name, centralized status, session count, and open-tab count without retargeting tabs.
- Recent workspace cards show server badge/name, workspace/path, session/open-tab counts, and scoped Open/Files/Terminal actions.
- Two to four resumable session previews show meaningful title, server/workspace, recency/status, and Resume; View all opens scoped Sessions.
- Workspaces with sessions but no open tab can appear.
- Empty, loading, partial-failure, and populated states remain task-oriented and scroll correctly on phone.
- Home does not duplicate prominent creation actions or become a notification feed.
- Current-device screenshots cover empty and populated/multi-server states.
- Compile, detekt, and affected tests pass.

