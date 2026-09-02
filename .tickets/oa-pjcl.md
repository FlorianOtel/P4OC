---
id: oa-pjcl
status: closed
deps: [oa-6swf, oa-97i6, oa-0gah]
links: []
created: 2026-07-08T14:42:17Z
type: feature
priority: 2
assignee: Jasmin Le Roux
parent: oa-xju6
---
# Add cross-server attention badge registry

Implement compact attention/badge state for Home, tabs, servers, and workspaces. Do not add notification content to Home.

## Design

Signals may include:
- unread chat response/completion
- waiting for user input/permission/question
- server auth/reconnect/error
- terminal finished/error

Presentation:
- Home badge for aggregate attention.
- Tab badges for tab-local attention.
- Server/workspace badges/dots for scoped attention.
- No notification feed/cards in Home.

## Acceptance Criteria

- Attention state is scoped by server/workspace/tab and does not leak across servers.
- Home shows aggregate badge/dot only, not notification feed content.
- Clearing/focusing tab updates relevant badge state.
- Tests cover badge aggregation and per-server isolation.

