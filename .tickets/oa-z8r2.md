---
id: oa-z8r2
status: closed
deps: [oa-6swf, oa-7ipn, oa-ximf]
links: []
created: 2026-07-08T14:42:17Z
type: bug
priority: 1
assignee: Jasmin Le Roux
parent: oa-xju6
---
# Make tab identity explicitly server scoped

Ensure every normal tab carries explicit server + workspace + route identity. Home is the only global/non-workspace pinned surface.

Current workspace cutover already requires explicit workspace identity. Multi-server Home extends this: workspace identity must include server identity everywhere tabs/actions are persisted or restored.

## Design

Audit and update:
- TabInstance/TabState persisted model.
- TabManager.createTab inputs.
- Tab restore/save path in MainTabScreen/SettingsDataStore.
- Tab title/icon labels to include compact server/workspace context where needed.
- Existing Screen.Sessions/Files/Terminal routes.

Home tab:
- pinned, non-closeable, global aggregator route.
- not treated as a normal Sessions tab.

## Acceptance Criteria

- Persisted tabs include enough server identity to restore mixed-server tabs.
- Restoring a tab whose server is missing/unavailable yields a clear offline/orphan state, not wrong-server fallback.
- No work tab is created with null/default server context unless explicitly global and justified.
- Tests cover save/restore mixed-server tabs and missing-server restore.

