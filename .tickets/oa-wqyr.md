---
id: oa-wqyr
status: closed
deps: []
links: []
created: 2026-05-05T17:48:39Z
type: feature
priority: 1
assignee: Jasmin Le Roux
parent: oa-ssm2
tags: [files, navigation, phase-1]
---
# Tier A: tab-bar New Files / New Sessions / New Terminal dropdown

Replace the single + tab action in MainTabScreen.kt:286-288 with a small dropdown menu offering New Sessions tab / New Files tab / New Terminal tab. New tab inherits workspaceDirectory from the active tab (tabs.firstOrNull { it.id == activeTabId }?.workspaceDirectory). Fixes the user-reported bug 'I don't see a way to access files directly' — currently Files can only be opened from inside a chat overflow. Also fixes Terminal discoverability for free. Do NOT add a Files entry to ServerScreen — at that point we don't have a workspace yet (AGENTS.md forbidden patterns 4, 9).

## Acceptance Criteria

Fresh-connect users see + dropdown with three options. New Files tab opens FileExplorerScreen scoped to active tab's workspace directory. Workspace is workspace-scoped, not server-global. Council pre-approved.


## Notes

**2026-05-05T18:41:38Z**

Implemented. MainTabScreen.kt — wrapped TabBar in a Box, added DropdownMenu with three items (Sessions/Files/Terminal). workspaceDirectory inherited via tabs.firstOrNull { it.id == activeTabId }?.workspaceDirectory and passed into each tabManager.createTab(...). Terminal item replicates existing PTY-creation flow (api.createPtySession, then createTab with Screen.Terminal.createRoute(ptyId)). TabBar.kt unchanged — its onAddClick callback is now used to toggle the menu. Icons match TabBar's getIconForRoute mapping. Test tags added per AGENTS.md. Build green.
