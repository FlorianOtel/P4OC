---
id: oa-hrtb
status: closed
deps: [oa-zmqg]
links: []
created: 2026-07-09T19:48:57Z
type: task
priority: 1
assignee: Jasmin Le Roux
---
# Stabilize work tab identity and titles

## Problem
Tab titles are generic, route-derived, and heavily truncated (`Tab`, `Global`, `Sessions · …`, `Terminal d…`), so users cannot safely distinguish work spanning multiple servers.

## Evidence / Repro
The pinned Home route previously fell through to `Tab`; nested routes update generic title mappings; the current phone screenshot shows ambiguous and truncated labels.

## UX Constraint
Pinned Home is the fixed global anchor. Closeable tabs represent stable work objects with visible server/workspace identity; nested navigation must not rename them.

## Design

Use fixed icon-only Home on narrow widths or `Home` where space permits, scrolling closeable tabs, and fixed trailing +. Do not add duplicate bottom navigation.

## Acceptance Criteria

- Pinned non-closeable Home has explicit localized Home title/icon and never uses generic fallback.
- Work tabs have stable object identity independent of nested route changes.
- Each closeable tab exposes deterministic server badge plus meaningful work title and workspace context within phone constraints.
- `Global` is replaced in user-facing identity by explicit `No project context` where intentionally selected.
- Accessibility names retain full server/workspace/object identity when visible text truncates.
- Mixed-server and narrow-width title behavior is tested and verified on device.
- Compile and detekt pass.

