---
id: oa-casy
status: closed
deps: [oa-wmvc, oa-3yk2, oa-qy0f, oa-wxf2]
links: [oa-nwha, oa-e6g3, oa-ua2q, oa-wmvc, oa-3yk2, oa-qy0f, oa-wxf2, oa-dygk]
created: 2026-07-05T16:58:54Z
type: epic
priority: 1
assignee: Jasmin Le Roux
---
# Resolve four red-test complaint regressions

Problem:
Four complaint areas now have or need intentionally red contract coverage: permission title localization boundary, slash command undo/redo dispatch semantics, model/config default source-of-truth behavior, and chat scroll restoration. The audit confirmed these are not isolated bugs; they are recurring boundary failures around upstream protocol alignment, UI/domain layering, runtime refresh, and lifecycle restoration.

Evidence:
- Permission title: Permission.kt exposes a computed English title; InlinePermissionPrompt and NotificationEventObserver consume that preformatted domain text. Existing ticket oa-wmvc already tracks the production fix.
- Undo/redo dispatch: ChatViewModel hardcodes slash built-ins but generic typed/palette command paths route to executeCommand instead of explicit local/session APIs.
- Model/config defaults: ModelAgentManager chooses Android-side fallbacks such as build/recents/first variants rather than clearly honoring upstream/server defaults and explicit user choices.
- Scroll restoration: ChatScreen uses lifecycle-blind remember state for scroll/search/follow-tail state; source-inspection red test exists but behavior coverage should replace it.

UX Constraint:
Do not hide protocol or lifecycle mistakes behind generic errors. Users must see workspace/session-correct behavior: permission prompts are readable/localizable, undo/redo matches intended opencode semantics, model defaults match server/config unless explicitly overridden, and restored chat tabs do not jump unexpectedly.

Expected Behavior:
Each complaint has a failing red test that states the desired product/upstream contract, followed by production fixes that make those tests pass without shims, aliases, or implementation-token assertions.

## Design

Treat this as a clean contract cutover, not a compatibility shim exercise. Tests should describe the user/upstream behavior, not current implementation shape. Prefer explicit types/dispatch tables over stringly fallback chains. Keep workspace identity explicit per AGENTS.md.

## Acceptance Criteria

- Permission boundary is tracked through existing ticket oa-wmvc and red tests assert domain does not expose localized display title.
- Undo/redo command dispatch has contract tests for typed slash and palette paths, and production code routes to the chosen explicit handler/API instead of accidental generic executeCommand.
- Model/config defaults have contract tests for server default precedence, explicit user override boundaries, and reasoning variant default/null behavior.
- Chat scroll restoration has behavior-level tests for same-session restoration and no forced tail jump; source-inspection tests are removed or demoted after behavior tests exist.
- All child tickets include problem, evidence, UX constraints, expected behavior, acceptance criteria, and verification commands.
- Fixes satisfy targeted tests plus ./gradlew :app:compileDebugKotlin before closing.


## Notes

**2026-07-06T11:35:04Z**

Completion update from 2026-07-06:

All four complaint regression children are now closed:
- oa-qy0f: model/agent defaults and config refresh contracts
- oa-3yk2: undo/redo slash command dispatch contracts
- oa-wxf2: chat scroll restoration behavior contract
- oa-wmvc: permission display/localization boundary

Final verification after the last child fix:
- ./gradlew :app:testDebugUnitTest -> PASS
- ./gradlew :app:compileDebugKotlin -> PASS
- ./gradlew :app:detekt -> PASS

The red-test complaint batch is resolved without remaining known failing tests.
