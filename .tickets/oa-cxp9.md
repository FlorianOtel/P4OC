---
id: oa-cxp9
status: closed
deps: [oa-zmqg, oa-sa63]
links: []
created: 2026-07-09T19:48:46Z
type: feature
priority: 1
assignee: Jasmin Le Roux
---
# Replace Start Work with scoped place flow

## Problem
Start Work exposes an architectural `Target` concept, silently defaults ownership, and uses a Files-specific chooser that loses server identity.

## Evidence / Repro
Current dialog shows three inconsistent target branches and five equal-weight actions. Home workspace detail state is private and never reaches `StartWorkContext`; `defaultAction` is unused; ambiguous paths fall back to current server/global workspace.

UX Constraint
The common contextual path should be `+` then one action. Destination remains visibly explicit and cannot be guessed across servers.

## Design

Use one coordinator and one bottom-sheet flow. Preserve selected scope and pending action through auth/reconnect. Reuse exact scope from Home detail and work tabs.

## Acceptance Criteria

- Start Work uses a context-first bottom sheet showing `In` plus badge, server name, workspace/path, centralized status, and Change.
- Primary actions are New chat, Files, and Terminal; scoped Sessions is secondary.
- Home detail, Chat, Files, and Terminal inherit immutable owning scope.
- Ambiguous Home/global invocation opens a grouped server/workspace picker before actions.
- Picker groups by friendly server identity and includes explicit `No project context`; it never lists an ownerless path.
- No configured, offline, auth-required, removed-server, and missing-workspace states retain intent and offer appropriate recovery without retargeting.
- No fallback to current server or implicit Global remains.
- Current-device screenshots cover contextual and ambiguous invocations.
- Compile, detekt, and affected tests pass.

