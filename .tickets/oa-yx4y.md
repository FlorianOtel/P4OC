---
id: oa-yx4y
status: closed
deps: [oa-6swf, oa-ximf]
links: []
created: 2026-07-08T14:42:17Z
type: feature
priority: 2
assignee: Jasmin Le Roux
parent: oa-xju6
---
# Build Servers management screen

Build the dedicated Servers/Connections management surface. This is not the work hierarchy; it manages connection config, credentials, discovery, reconnect, and deletion.

## Design

Surface should include:
- Saved server list with status badges/dots.
- Add/edit server URL/display name/username/allow insecure/default workspace.
- Login/update credentials.
- Reconnect/disconnect actions.
- mDNS/discovered server section where appropriate.
- Remove server with warnings when open tabs/recent workspaces reference it.

## Acceptance Criteria

- User can manage server configs without entering Home work hierarchy.
- Removing a server with open tabs requires confirmation and states what happens to affected tabs.
- Auth-required/offline/reconnecting states are visible as status/badges.
- Tests or UI smoke notes cover add/edit/remove/reconnect and remove-with-open-tabs warning.

