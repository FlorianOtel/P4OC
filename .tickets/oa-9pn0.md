---
id: oa-9pn0
status: closed
deps: []
links: []
created: 2026-07-08T13:27:45Z
type: bug
priority: 1
assignee: Jasmin Le Roux
external-ref: gh-31
---
# Preserve no-port server URL after login

GitHub issue #31 reports that entering a server URL without an explicit port, such as https://my-host.example.com, succeeds initially but is persisted as https://my-host.example.com:4096. Reconnect then fails when the server is exposed through the scheme default port or reverse proxy rather than OpenCode port 4096. Investigation confirmed ServerUrl.normalizeConnectUrl defaults missing ports to DEFAULT_PORT=4096 and ServerViewModel saves that normalized URL via saveLastConnection and addRecentServer.

## Acceptance Criteria

When a user enters an http/https URL without an explicit port, the saved last connection and recent server URL preserve the no-port form. Connection attempts may still try the OpenCode default port internally where appropriate, but this must not overwrite the user-visible persisted reconnect URL. Explicit user ports remain preserved. Add/keep red tests covering ServerUrl no-port preservation and ServerViewModel.connectToRemote persistence.


## Notes

**2026-07-08T13:39:41Z**

Implemented fix: ServerUrl.normalizeConnectUrl now preserves absent user ports for saved/display reconnect URLs, while endpointKey keeps canonical :4096 identity. ConnectionManager.connectionCandidates now owns OpenCode default-port probing by trying :4096 first for no-port inputs and then the preserved no-port URL. Added ServerViewModelIssue31Test covering persisted last/recent URL and updated ServerUrl/ConnectionManager tests. Verification passed: JAVA_HOME=/usr/lib/jvm/java-17-openjdk ./gradlew :app:compileDebugKotlin && ./gradlew :app:detekt && ./gradlew :app:testDebugUnitTest.
