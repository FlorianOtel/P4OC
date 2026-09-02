---
id: oa-sa63
status: closed
deps: []
links: []
created: 2026-07-09T19:48:15Z
type: task
priority: 0
assignee: Jasmin Le Roux
---
# Require explicit server and workspace ownership

## Problem
Creation, browse, and workspace-detail callbacks can omit ownership and fall back to `currentServerRef` or `WorkspaceKey.Global`, allowing work to target the wrong server when tabs span servers.

## Evidence / Repro
The Start Work coordinator silently falls back to current server/global workspace; Home workspace-detail Files/Terminal callbacks do not carry server/workspace arguments; the existing chooser is Files-specific and inferred from open tabs.

## UX Constraint
Every workspace-required action must carry immutable `ServerRef + WorkspaceKey`. Missing scope is an explicit selection state, never a guess, nullable default, fallback chain, or global escape hatch.

## Design

Introduce or reuse one immutable scoped identity value rather than parallel nullable parameters. Migrate every caller cleanly; leave no compatibility overloads or deprecated fallback paths.

## Acceptance Criteria

- Workspace-required callbacks and exported APIs require non-null `ServerRef + WorkspaceKey` (or a single non-null scoped value object).
- Home detail, Sessions, Chat, Files, Terminal, and Start Work invoke actions with their owning scope.
- No workspace-required path falls back to `currentServerRef`, active tab state, settings lastProject, or implicit `WorkspaceKey.Global`.
- `No project context` remains available only as an explicit user-visible choice.
- Missing/removed/ambiguous ownership routes to explicit target selection without retargeting.
- Tests cover mixed-server same-directory names, missing owner, explicit no-project context, and removed server.
- Compile, detekt, and affected unit tests pass.


## Notes

**2026-07-09T20:11:48Z**

Implemented typed StartWorkTarget/StartWorkSelection and NavigationWorkspaceSelection contracts. Home detail and Start Work actions carry exact ServerRef + WorkspaceKey; ambiguous or removed owners require selection; No project context is explicit; blank directories reject instead of falling back. Added mixed-server/missing-owner/removed-server coverage. Verified :app:compileDebugKotlin, :app:detekt, and :app:testDebugUnitTest pass. Pre-existing nullable/global semantics outside this ticket remain in persistence/network boundary models and should be assessed separately rather than treated as this flow's fallback.

**2026-07-10T07:06:46Z**

Final verification: :app:compileDebugKotlin and :app:detekt passed in the integrated run; :app:testDebugUnitTest passed after Home summary migration. Device screenshots ux-final-start-work.png and ux-final-start-work-scoped.png verify ambiguous selection and explicit No project context with exact endpoint ownership; Home verifies endpoint-scoped server identity. Registry execution, persistence, and per-tab ownership tests are included in the passing suite.
