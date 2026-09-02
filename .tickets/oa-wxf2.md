---
id: oa-wxf2
status: closed
deps: []
links: [oa-nwha, oa-77dh, oa-12ui, oa-casy, oa-3yk2, oa-wmvc, oa-qy0f, oa-dygk]
created: 2026-07-05T17:00:06Z
type: bug
priority: 1
assignee: Jasmin Le Roux
parent: oa-casy
---
# Replace chat scroll restoration source test with behavior contract

Problem:
Chat scroll/search/follow-tail state is restoration-critical but current red coverage includes a source-inspection style test that checks for rememberSaveable tokens instead of user-visible behavior. Production code also keeps several restoration-critical chat states in plain remember/rememberLazyListState.

Evidence:
- ChatScreen.kt:138-147 uses listState, showSearch, searchQuery, currentMatchIndex as composition state.
- ChatScreen.kt:151-154 uses shouldFollowTail, didInitialTailScroll, and hasNewContentWhileAway as composition state.
- MainTabScreen.kt:438-449 wraps tab pages in rememberSaveableStateHolder, but plain remember values do not survive process death and can reset when pages are disposed/recreated.
- ChatScrollRestorationTest.kt:10-29 currently checks source strings such as rememberSaveable rather than restoring the UI and asserting scroll behavior.
- Existing androidTest ChatScreenScrollRestorationTest has a useful behavior contract for returning to chat without forcing tail, but coverage is narrow.

UX Constraint:
A user reading older messages or searching inside a long chat must not be yanked to the tail or lose search state when switching tabs, rotating, or restoring the app. New messages should only auto-follow when the user intended to follow the tail.

Expected Behavior:
For the same session/tab, scroll position, follow-tail intent, new-content indicator state, and search query/current match restore across recomposition/configuration restoration. Different sessions must not share scroll state.

## Design

Prefer behavior assertions over implementation-token assertions. Save only user-restoration state, not derived caches. Key saved state by stable session/tab identity; do not use a single global scroll holder.

## Acceptance Criteria

- Add failing behavior-level test for same-session scroll restoration that recreates/re-enters the chat and proves it does not force tail.
- Add failing behavior-level test that search mode/query/current match restore for the same session if product wants search restoration; otherwise explicitly test/search state clears with a clear UX rationale.
- Add failing behavior-level test that scroll state is scoped by session/tab and does not leak to a different session.
- Remove or demote source-inspection red test after behavior coverage exists; tests should not pass merely because code contains rememberSaveable.
- Production fix uses saveable/session-keyed state or ViewModel SavedStateHandle as appropriate.
- Verification: run targeted chat scroll restoration unit/android tests and ./gradlew :app:compileDebugKotlin.


## Notes

**2026-07-05T17:45:56Z**

Scroll red-test follow-up on 2026-07-05:

The current local unit test ChatScrollRestorationTest was renamed to `temporary non acceptance guard requires session scoped saveable chat scroll restoration state`. It is intentionally a source-inspection guard and explicitly does NOT satisfy this ticket's final acceptance criteria. It only remains as a red interim signal that ChatScreen still lacks session-scoped saveable scroll/follow state.

Attempted behavior-level path:
- Existing primary behavior harness: app/src/androidTest/java/dev/blazelight/p4oc/ui/screens/chat/ChatScreenScrollRestorationTest.kt.
- Desired behavior cases: same-session re-entry keeps Jump to bottom visible after scrolling away; different session/viewmodel does not inherit away-from-tail state; returning to an away-from-tail session does not force tail.
- Blocker: instrumentation starts, but AndroidComposeTestRule fails before the test body with `IllegalStateException: Exception handler was not found via a ServiceLoader` from `kotlinx.coroutines.test.TestScopeImpl.enter`. This prevents grounded Compose behavior assertions.

Reported attempted fixes by tester peer:
- Debug test app/runner override to avoid app Koin startup.
- androidTest MockK dependencies.
- androidTest coroutines-android.
- debugImplementation(libs.coroutines.test).
- Koin/CredentialStore startup was resolved; remaining blocker is kotlinx.coroutines.test ServiceLoader/provider packaging/classpath.

Required before closing oa-wxf2:
- Fix androidTest coroutine ServiceLoader/provider classpath so AndroidComposeTestRule can enter test bodies.
- Add/verify behavior tests in ChatScreenScrollRestorationTest for same-session restoration, session isolation, and no forced tail on return.
- Remove or demote the temporary source guard once behavior coverage is running.

**2026-07-06T09:26:45Z**

Red verification update from 2026-07-06:

Current checkout does not contain app/src/androidTest/java/dev/blazelight/p4oc/ui/screens/chat/ChatScreenScrollRestorationTest.kt; globbing for *ScrollRestoration* found only app/src/test/java/dev/blazelight/p4oc/ui/screens/chat/ChatScrollRestorationTest.kt.

The existing ChatScrollRestorationTest is intentionally marked as a temporary non-acceptance source-inspection guard. It fails because ChatScreen.kt currently uses remember(uiState.session?.id) for shouldFollowTail, didInitialTailScroll, and hasNewContentWhileAway instead of saveable/session-scoped behavior state. This confirms the regression is visible, but it does not satisfy oa-wxf2 acceptance.

Implementation/fix work for oa-wxf2 still needs behavior-level coverage, ideally Compose UI/androidTest or an extracted state holder/ViewModel/SavedStateHandle test that proves:
- restoring the same session preserves away-from-tail position/follow-tail state,
- switching sessions isolates scroll/follow-tail state by session id,
- returning to an older position does not force-scroll to the tail when new content arrives.

Focused JVM red suite command used:
JAVA_HOME=/usr/lib/jvm/java-17-openjdk ./gradlew :app:testDebugUnitTest --tests dev.blazelight.p4oc.domain.model.ToolStateExtTest --tests dev.blazelight.p4oc.ui.screens.chat.ChatViewModelTest --tests dev.blazelight.p4oc.ui.screens.chat.ModelAgentManagerTest --tests dev.blazelight.p4oc.ui.screens.chat.ChatScrollRestorationTest

Observed result: 45 tests, 7 intentional failures. Scroll guard failure message: ChatScreen scroll restoration must keep follow-tail state saveable and keyed by session id so reopening a session does not force the list back to the tail.

**2026-07-06T11:12:11Z**

Completion update from 2026-07-06:

Replaced the temporary source-inspection scroll guard with behavior-level JVM tests and implemented a session-scoped restoration seam.

Production changes:
- Added ChatScrollRestorationStore / ChatScrollRestorationState / InitialTailDecision as an internal chat state holder for restoration-critical scroll/search/follow-tail behavior.
- ChatScreen now uses rememberSaveable keyed by session id for LazyListState and ChatScrollRestorationState.
- Follow-tail, initial tail-scroll completion, away-from-tail new-content affordance, search open/query/current match, search navigation, and jump-to-bottom transitions now flow through the state holder.
- Session identity scopes restoration so a different session starts with default follow-tail state instead of inheriting another session's away-from-tail/search state.
- Preserved the original loading gate: stale messages while uiState.isLoading=true do not consume the one-time initial tail restoration decision; a later ready render can still scroll to tail.

Tests:
- Removed the temporary non-acceptance source-inspection guard from ChatScrollRestorationTest.
- Added behavior tests for same-session away-from-tail restoration, session isolation, search navigation restoration/isolation, no forced tail on later content while away, jump-to-bottom resuming follow-tail, initial tail restoration happening once without overriding restored away position, and content-not-ready not consuming the initial-tail decision.

Verification:
- ./gradlew :app:testDebugUnitTest --tests dev.blazelight.p4oc.ui.screens.chat.ChatScrollRestorationTest -> PASS
- ./gradlew :app:compileDebugKotlin -> PASS
- ./gradlew :app:detekt -> PASS
- ./gradlew :app:testDebugUnitTest -> expected FAIL with only one remaining mapped red test outside oa-wxf2: ToolStateExtTest permission localization boundary (oa-wmvc). The oa-wxf2 scroll restoration failure now passes.

**2026-07-06T11:20:02Z**

Cleanup update from 2026-07-06:

Trimmed the scroll restoration seam after review to remove test-only production weight:
- Deleted ChatScrollRestorationStore; production never used it because ChatScreen is already session-keyed with rememberSaveable.
- Inlined trivial one-line wrappers for search open/close, search-match follow-tail mutation, and initial-tail marking.
- Kept the useful core: ChatScrollRestorationState, its Saver, and non-trivial transition methods for scroll settled, tail content changes, content readiness, and jump-to-bottom.
- Updated ChatScrollRestorationTest to instantiate ChatScrollRestorationState directly while preserving behavior coverage.

LOC check after trim:
- ChatScrollRestorationState.kt: 88 lines
- ChatScrollRestorationTest.kt: 98 lines

Verification after trim:
- ./gradlew :app:testDebugUnitTest --tests dev.blazelight.p4oc.ui.screens.chat.ChatScrollRestorationTest -> PASS
- ./gradlew :app:compileDebugKotlin -> PASS
- ./gradlew :app:detekt -> PASS
- ./gradlew :app:testDebugUnitTest -> expected FAIL with only the remaining oa-wmvc ToolStateExtTest red failure.
