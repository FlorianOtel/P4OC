---
id: oa-ja73
status: closed
deps: [oa-qr52, oa-vvep, oa-0f4m, oa-blgp]
links: []
created: 2026-05-01T17:45:54Z
type: task
priority: 1
assignee: Jasmin Le Roux
parent: oa-gt0g
tags: [tests, workspace]
---
# Test infrastructure: workspace/session fakes + pure-function tests

Without these, ChatViewModel/SessionListViewModel rewrites cannot be unit-tested and verification falls entirely on manual smoke. That's where reward-hacking lives. Add: FakeWorkspaceClient, FakeSessionRepository, FakeServerEventGateway. Plus pure-function tests for SessionReducer (per design-B), WorkspacePath round-trip, route encode/decode round-trip (per design-E).

## Acceptance Criteria

1) Fakes exist in app/src/test/.../fakes/. 2) WorkspacePath round-trip test: parseFromServer(toAttachmentUrl(p)) == p for paths with spaces/unicode/dots. 3) WorkspacePath rejects file://, absolute, blank. 4) Route encode/decode round-trip test for the chosen encoding. 5) SessionReducer test: hydrate-then-stream race buffers events correctly (per design-B). 6) Optimistic rollback test: mock HTTP 5xx → reducer rolls back (per design-C). 7) ./gradlew :app:testDebugUnitTest green.

