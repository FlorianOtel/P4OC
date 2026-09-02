---
id: oa-g8t3
status: closed
deps: []
links: []
created: 2026-05-10T09:45:17Z
type: bug
priority: 1
assignee: Jasmin Le Roux
---
# Route chat SSE UI state through SessionRepository

Problem:
ChatViewModel and WorkspaceViewModel both collect ConnectionManager.scopedEvents. WorkspaceViewModel forwards events into SessionRepository, while ChatViewModel also consumes raw SSE events for permissions, questions, child sessions, session status, session updates, and session errors. This creates split-brain state ownership and makes chat state depend on whether a chat ViewModel is currently alive.

Evidence:
ChatViewModel.observeEvents() collects connectionManager.scopedEvents and handleEvent() mutates dialog queues, isBusy/isSending, childSessionIds, session state, and error state. WorkspaceViewModel also collects connectionManager.scopedEvents and calls sessionRepository.acceptEvent(). SessionRepositoryImpl currently only treats session events as repository state and does not clear streaming flags on SessionStatusChanged/SessionError unless ChatViewModel asks it to.

UX Constraint:
Session state, streaming flags, permission/question prompts, and abort/error presentation must remain correct when the user is on Files/Terminal/Projects, when tabs are switched, and after ViewModel recreation. UI should observe domain state, not raw socket events.

Expected Behavior:
SessionRepository is the owner of session-scoped SSE state transitions. ChatViewModel observes repository flows/state for messages, busy/idle, errors, permissions, questions, child sessions, and unread/completion signals as needed. ChatViewModel no longer directly collects ConnectionManager.scopedEvents for chat session state.

Acceptance Criteria:
- Remove chat-session state mutation driven directly by ChatViewModel's raw scopedEvents collector.
- Move SessionStatusChanged/SessionError streaming-flag cleanup into SessionRepository or a repository-owned reducer path.
- Expose permission/question pending state from repository/domain state, or add a clearly scoped repository-owned event/state flow for them.
- Preserve workspace/server/generation routing; no global/default workspace shortcuts.
- Keep notification observers or non-state side-effect observers separate from repository state, with explicit justification.
- Add tests covering idle/error events clearing streaming flags even when no ChatViewModel is collecting raw SSE.

Verification:
Run ChatViewModel/SessionRepository unit tests and ./gradlew :app:compileDebugKotlin. Manually verify a run can complete/abort while the user is on a non-chat tab, then returning to chat shows correct idle/error state.


## Notes

**2026-05-10T10:55:18Z**

Moved chat-session SSE state ownership into SessionRepositoryImpl. Repository now exposes sessionUiState(sessionId) with session/status/dialog/todo/error/completion state, tracks child sessions for subagent permission/question routing, clears streaming flags on SessionStatusChanged idle, SessionIdle, and SessionError without requiring a ChatViewModel collector, and owns permission/question clear operations. ChatViewModel no longer collects ConnectionManager.scopedEvents or mutates chat state from raw SSE; it observes repository sessionUiState and keeps only UI side effects such as haptics/unread/queued-send decisions. Added tests for idle/error streaming cleanup without ChatViewModel raw SSE collection. Verification: ./gradlew :app:testDebugUnitTest --tests dev.blazelight.p4oc.data.session.SessionRepositoryImplTest --tests dev.blazelight.p4oc.ui.screens.chat.ChatViewModelTest; export JAVA_HOME=/usr/lib/jvm/java-17-openjdk && ./gradlew :app:compileDebugKotlin. Manual run-complete/abort while on non-chat tab still recommended.
