---
id: oa-cemz
status: closed
deps: []
links: []
created: 2026-05-01T17:44:25Z
type: task
priority: 1
assignee: Jasmin Le Roux
parent: oa-gt0g
tags: [design, workspace, connection]
---
# Design lock D: server identity / ServerRef equality

Without a server identity model, persisted tabs can resurrect against the wrong server, and stale WorkspaceClients can survive reconnect. Decide: equal-by-baseUrl-string, equal-by-normalized-URL, equal-by-config-id, equal-by-connection-generation? What changes on reconnect to same URL? On re-auth? Is ServerRef monotonically epoch'd?

## Acceptance Criteria

1) ServerRef equality defined precisely. 2) Reconnect-same-URL behavior defined. 3) Re-auth behavior defined. 4) Stale-client rejection mechanism defined (ActiveServerApiProvider guard). 5) Persistence validation behavior defined (what does 'is this workspace on the active server' mean).


## Notes

**2026-05-01T18:19:06Z**

Decision locked. See docs/design-locks/D-server-identity.md
