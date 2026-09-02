---
id: oa-cj0w
status: closed
deps: [oa-6swf, oa-7ipn, oa-z8r2, oa-0gah]
links: []
created: 2026-07-08T14:42:17Z
type: bug
priority: 1
assignee: Jasmin Le Roux
parent: oa-xju6
---
# Restore mixed-server tabs and Home state safely

Persist and restore mixed-server tabs, pinned Home, active tab, and selected Home workspace detail safely across process death/app restart.

## Design

Restore order:
1. Load server registry.
2. Restore pinned Home and normal tabs.
3. Resolve server refs for every normal tab.
4. Mark tabs offline/orphaned when server missing/unavailable.
5. Start reconnect for servers used by open tabs according to policy.
6. Home renders restored tabs/summaries immediately with stale/offline labels where needed.

## Acceptance Criteria

- Pinned Home is restored and cannot be duplicated/closed accidentally.
- Mixed-server tabs restore with correct server/workspace/route labels.
- Missing server config produces a clear orphan/offline tab state, not fallback to another server.
- Selected Home workspace detail restoration is specified and tested or intentionally reset to top-level Home.
- Tests cover app restart with Alpha chat + Beta files + Local terminal.

