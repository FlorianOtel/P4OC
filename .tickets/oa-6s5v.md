---
id: oa-6s5v
status: closed
deps: []
links: []
created: 2026-05-10T09:55:29Z
type: bug
priority: 1
assignee: Jasmin Le Roux
---
# Dispatch chat run mutations outside ChatViewModel scope

Problem:
ChatViewModel launches sendMessageAsync and abortSession calls in viewModelScope. If the user navigates away while the HTTP request is in flight, coroutine cancellation can abort the client request even though the server may already have started or needs the stop request delivered.

Evidence:
ChatViewModel.sendMessage paths call safeApiCall { workspaceClient.sendMessageAsync(sessionId, request) } inside viewModelScope.launch. abort/stop paths are also invoked from ChatViewModel scope. WorkspaceClient.sendMessageAsync is a Retrofit suspend call that waits for HTTP completion.

UX Constraint:
Starting or stopping an agent run should be tied to session/workspace intent, not transient chat screen lifetime. Navigating back should not orphan server work or cancel a requested stop silently.

Expected Behavior:
Run-triggering mutations are dispatched through SessionRepository or a workspace/app-scoped command queue that survives chat screen teardown until request acknowledgement/failure.

Acceptance Criteria:
- Move sendMessageAsync and abortSession dispatch ownership out of ChatViewModel viewModelScope, or explicitly shield critical network acknowledgement from UI cancellation.
- Preserve per-session busy/sending state and human-readable error presentation.
- Ensure closing the tab/workspace/disconnect cancels or reconciles pending commands intentionally.
- Add tests for navigation/cancellation during in-flight send/abort where feasible.

Verification:
Run ChatViewModel/SessionRepository tests and ./gradlew :app:compileDebugKotlin. Manually send a prompt and navigate away immediately; returning should show a consistent run state.


## Notes

**2026-05-10T10:29:22Z**

Verified existing in-progress implementation: sendMessageAsync and abortSession are now dispatched by SessionRepositoryImpl in repository scope and ChatViewModel only awaits the returned Deferred for UI state/error updates. Repository tests cover cancelling a UI waiter while send/abort remains in flight; fake repository/client and ChatViewModel tests were updated accordingly. Verification: ./gradlew :app:testDebugUnitTest --tests dev.blazelight.p4oc.data.session.SessionRepositoryImplTest --tests dev.blazelight.p4oc.ui.screens.chat.ChatViewModelTest; export JAVA_HOME=/usr/lib/jvm/java-17-openjdk && ./gradlew :app:compileDebugKotlin. Manual send-and-navigate-away smoke test remains recommended.
