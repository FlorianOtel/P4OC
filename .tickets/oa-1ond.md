---
id: oa-1ond
status: closed
deps: []
links: []
created: 2026-05-10T09:45:46Z
type: task
priority: 2
assignee: Jasmin Le Roux
---
# Avoid main-thread question queue JSON serialization

Problem:
DialogQueueManager serializes pending question state to JSON synchronously while handling question queue changes. If QuestionRequest payloads become large, this can add main-thread work during SSE-driven UI updates.

Evidence:
DialogQueueManager.showNextQuestion() writes savedStateHandle[KEY_PENDING_QUESTION] = json.encodeToString(question). persistQuestionsQueue() builds pendingQuestions.toList() and writes json.encodeToString(queueList). DialogQueueManager is used from ChatViewModel UI/event paths.

UX Constraint:
Permission/question prompts must remain responsive and survive process death, but serialization should not cause visible frame drops or input lag. Failure states should be logged and recover by clearing invalid saved state, not crashing.

Expected Behavior:
Question persistence does any non-trivial JSON encoding off the main thread, then applies the resulting string to SavedStateHandle on the main thread. StateFlow prompt updates remain immediate.

Acceptance Criteria:
- Move pending question/queue JSON encoding off the main thread, or otherwise prove payload size is bounded enough to keep synchronous encoding.
- Avoid races where stale async persistence overwrites newer queue state.
- Preserve process-death restoration behavior.
- Add focused tests for enqueue/clear ordering if the implementation introduces async persistence.

Verification:
Run ChatViewModel/DialogQueueManager tests and ./gradlew :app:compileDebugKotlin.


## Notes

**2026-05-10T11:44:55Z**

Implemented async DialogQueueManager question persistence using injected coroutine scope/dispatcher, Default dispatcher JSON encoding, and version guards to prevent stale writes. Added focused tests for async enqueue/clear ordering. Verification attempted with ./gradlew :app:testDebugUnitTest --tests dev.blazelight.p4oc.ui.screens.chat.DialogQueueManagerTest, but build is currently blocked before tests by unrelated TabBar.kt compile errors: unresolved SessionStateColors and Color type mismatch.
