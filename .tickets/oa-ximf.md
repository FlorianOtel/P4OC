---
id: oa-ximf
status: closed
deps: [oa-6swf, oa-7ipn]
links: []
created: 2026-07-08T14:42:17Z
type: feature
priority: 1
assignee: Jasmin Le Roux
parent: oa-xju6
---
# Add durable server registry and management model

Create a durable saved-server registry distinct from last connection/recent servers. This supports Home server carousel/status, Start Work target selection, and server management.

Current state has last connection and recent server concepts. Multi-server Home needs saved server inventory with stable identity, display URL, canonical endpoint key, credentials linkage, friendly name, connection policy, and default/pinned metadata.

## Design

Candidate data model:
SavedServer(id, endpoint, endpointKey, displayName, username, allowInsecure, lastConnectedAt, pinned, defaultWorkspace)

Preserve the issue #31 invariant:
- display/persisted URL preserves user-entered no-port URL.
- endpointKey is canonical identity.
- connection candidates are internal probing details.

Credential material stays outside report/UI and remains in CredentialStore or equivalent secure storage keyed by server identity.

## Acceptance Criteria

- Saved servers can be added, edited, removed, listed, and looked up by stable id/endpoint key.
- Existing last-connection/recent-server data migrates or is surfaced without data loss.
- No API keys/passwords are stored in DataStore JSON/plain preferences.
- Unit tests cover no-port URL preservation, endpoint dedupe, edit/remove behavior, and migration from existing recent/last connection state.

