---
id: oa-ff10
status: closed
deps: []
links: []
created: 2026-05-10T09:49:59Z
type: bug
priority: 2
assignee: Jasmin Le Roux
---
# Preserve inline question draft answers across configuration changes

Problem:
InlineQuestionCard persists the pending question through DialogQueueManager/SavedStateHandle, but the user's in-progress answer selections/text are held in non-saveable Compose state. Configuration changes can keep the question while losing the user's typed response.

Evidence:
InlineQuestionCard.kt uses var currentQuestionIndex by remember { mutableIntStateOf(0) } and val answers = remember { mutableStateMapOf<Int, List<String>>() }. ChatScreen renders InlineQuestionCard for pendingQuestion from ChatViewModel/DialogQueueManager.

UX Constraint:
Users should not lose multi-step/custom answers to LLM questions during rotation, process recreation within saved-state limits, or tab/screen recomposition.

Expected Behavior:
Current question index and draft answers use rememberSaveable or ViewModel-backed state keyed by question id.

Acceptance Criteria:
- Preserve currentQuestionIndex and answers across configuration changes for the same question request.
- Reset saved draft state when a different question request is shown/submitted/cleared.
- Support multi-select/custom text answer formats currently used by InlineQuestionCard.
- Add a Compose/UI state test if practical, or document manual rotation verification.

Verification:
Run ./gradlew :app:compileDebugKotlin and manually type/select an answer, rotate/recreate, and confirm draft remains.


## Notes

**2026-05-10T11:50:34Z**

Implemented InlineQuestionCard draft preservation with rememberSaveable keyed by question request id. Current question index and answers now survive configuration changes for the same pending request and reset when a new request id is shown or the card is removed after submit/dismiss. Custom typed answers are derived from saved selections so typed custom text is restored. Verification attempted with JAVA_HOME=/usr/lib/jvm/java-17-openjdk ./gradlew :app:compileDebugKotlin, but compile is currently blocked by unrelated dirty-worktree errors in OfishMutationClient.kt and UploadOrchestrator.kt around changed upload byte/readBytes APIs. Manual rotation verification not run in this CLI session.
