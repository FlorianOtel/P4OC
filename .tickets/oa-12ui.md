---
id: oa-12ui
status: closed
deps: [oa-wmvc, oa-3yk2, oa-qy0f, oa-wxf2, oa-ivwp, oa-vf6h, oa-prjv, oa-3l1w, oa-0mel, oa-9ev3, oa-n1fs, oa-gtw8, oa-tzta, oa-plno, oa-ua2q, oa-e6g3, oa-77dh]
links: [oa-prjv, oa-tzta, oa-n1fs, oa-qy0f, oa-e6g3, oa-9ev3, oa-wmvc, oa-gtw8, oa-plno, oa-wxf2, oa-vf6h, oa-3l1w, oa-0mel, oa-ivwp, oa-3yk2, oa-ua2q, oa-77dh]
created: 2026-07-05T18:04:23Z
type: task
priority: 1
assignee: Jasmin Le Roux
parent: oa-nwha
---
# Rewrite tests that preserve stale implementation contracts

Problem:
Audit found tests that lock in current implementation details or incorrect fallback behavior instead of intended user/upstream contracts. These tests can make future correct fixes look like regressions.

Evidence:
Examples include EventMapperTest.kt and older ToolStateExtTest.kt asserting English permission title behavior, ChatViewModelTest.kt previously missing command palette dispatch paths, ModelAgentManagerTest.kt and ModelReasoningEffortsTest.kt encoding fragile model/reasoning defaults, TabChatRouteCodecTest.kt and TabManagerPersistenceTest.kt preserving older route/tab shapes, ChatScrollRestorationTest.kt inspecting ChatScreen.kt source strings, and ChatScreenScrollRestorationTest.kt existing but blocked by androidTest harness issues.

UX Constraint:
Tests should protect user-visible behavior and architectural contracts, not implementation accidents. Red tests should fail for the intended reason and should not require future agents to rediscover why a bad fallback was once expected.

Expected Behavior:
Tests assert protocol/domain/UI boundaries, typed versus palette command behavior, explicit default precedence, workspace scoping, and lifecycle restoration behavior. Source-inspection tests are temporary only and replaced by behavior tests where possible.

Acceptance Criteria:
- Inventory stale-contract tests from audit and tag each as keep, rewrite, or delete.
- Replace English/domain permission assertions with raw data plus resource/UI formatter assertions.
- Ensure command tests cover typed and palette dispatch paths for local/session/server commands.
- Rewrite model/reasoning tests to assert server/config/user-default precedence without first-item fallback assumptions.
- Replace ChatScrollRestorationTest source guard with androidTest behavior coverage after the coroutine ServiceLoader blocker is fixed.
- Update route/tab persistence tests to assert current workspace/session identity requirements, not legacy compatibility guessing.

Verification:
Run targeted tests for each rewritten area and confirm failures, if any, point to production contract gaps rather than stale test expectations.


## Notes

**2026-07-07T21:22:11Z**

Inventory/resolution summary after child tickets: stale-contract coverage has been rewritten or superseded across the audited areas. Permission/notification/resource display assertions were moved to resource-boundary or pure resId mapper tests in oa-ivwp, oa-3l1w, oa-0mel, oa-prjv, and oa-vf6h rather than asserting English strings in domain state. Slash command typed/palette behavior and metadata were covered by oa-3yk2, oa-0mel, and oa-dygk tests. Model/agent and reasoning default precedence was handled by closed child tickets oa-qy0f/oa-wmvc. Route/tab/workspace persistence tests were updated by oa-e6g3/oa-9ev3 and cleanup fixes to assert explicit WorkspaceKey identity and dropping ambiguous restored tabs instead of legacy guessing. Lifecycle behavior tests were added for chat draft/attachments, file explorer state, file editor drafts, and session list state in oa-77dh/oa-tzta/oa-gtw8/oa-plno. Remaining androidTest-only chat scroll behavior is covered by oa-wxf2's rewritten contract; source-inspection guards are no longer the primary lifecycle proof. Verification across child tickets included their targeted JVM tests plus current ./gradlew :app:compileDebugKotlin and ./gradlew :app:detekt. No additional stale-test blocker remains in the ready set; future stale tests discovered should be opened as specific follow-up bugs rather than keeping this umbrella open.
