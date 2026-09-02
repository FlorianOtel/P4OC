---
id: oa-fuc8
status: closed
deps: []
links: []
created: 2026-05-01T17:44:25Z
type: task
priority: 1
assignee: Jasmin Le Roux
parent: oa-gt0g
tags: [ci, guards, workspace]
---
# CI guards phase 1: forbid forward-only reward-hack patterns

Add scripts/no-ambient-directory.sh in two-phase form. Phase 1 fails build on patterns NOT in codebase today (the temptation patterns an agent might invent during cutover). Wired into CI workflow + pre-commit. Phase 1 patterns: Workspace.global, Workspace.DEFAULT, Workspace.current, var workspace, WorkspaceManager singleton, fun withWorkspace, tabManager.activeTabWorkspace. Phase 2 (DirectoryManager, SessionDataCache, etc.) is added but commented; gets enabled in the demolition ticket.

## Acceptance Criteria

1) Script at scripts/no-ambient-directory.sh exists, supports --phase=1 and --phase=2 modes. 2) Phase 1 exits 0 on current main. 3) Phase 1 exits nonzero on a planted regression PR (verify by adding 'val Workspace.global = ...' temporarily). 4) Wired into .github/workflows/* AND a pre-commit hook. 5) README/AGENTS.md note explains the two phases. 6) Phase 2 patterns are present in script (commented or gated) but not enforced.


## Notes

**2026-05-01T17:47:48Z**

Cancelled — user dropped CI guard plan from cutover.
