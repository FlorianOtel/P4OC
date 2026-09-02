---
id: oa-0gah
status: closed
deps: [oa-6swf, oa-7ipn, oa-ximf]
links: []
created: 2026-07-08T14:42:17Z
type: feature
priority: 1
assignee: Jasmin Le Roux
parent: oa-xju6
---
# Introduce multi-server connection registry

Replace/extend single active ConnectionManager assumptions with a registry capable of managing multiple server connections independently.

Flattened mixed-server tabs require Alpha chat, Beta files, and Local terminal to coexist without changing a global current server under the user's feet.

## Design

Candidate API:
ServerConnectionRegistry.connection(serverRef): StateFlow<ConnectionState>
ServerConnectionRegistry.api(serverRef): OpenCodeApi?
ServerConnectionRegistry.connect(serverId)
ServerConnectionRegistry.disconnect(serverId)
ServerConnectionRegistry.reconnectAll()

Each server owns its own OkHttp/auth client, API, SSE/event source, generation, reconnect policy, credential state, and coroutine scope.

Lifecycle policy:
- Servers with open tabs are kept/reconnected on foreground.
- Servers with no open tabs can lazy-connect when selected/opened.
- Auth failures stop retry storms and show badge/state.

## Acceptance Criteria

- Two saved servers can have independent connection states simultaneously.
- Failure/auth issue on one server does not block or overwrite another server's state.
- Existing single-server flows still work.
- Tests cover independent connect/disconnect/error states and foreground reconnect policy.

