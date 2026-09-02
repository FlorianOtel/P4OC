---
id: oa-3yk2
status: closed
deps: []
links: [oa-nwha, oa-0mel, oa-12ui, oa-casy, oa-dygk, oa-wmvc, oa-qy0f, oa-wxf2]
created: 2026-07-05T16:59:36Z
type: bug
priority: 1
assignee: Jasmin Le Roux
parent: oa-casy
---
# Fix undo and redo slash command dispatch contracts

Problem:
Android advertises /undo and /redo as built-in slash commands, but the chat typed-command path and command palette path are routed through generic executeCommand. Undo/redo are destructive session/file semantics and need explicit dispatch matching the chosen opencode contract, not accidental server-command fallback.

Evidence:
- ChatViewModel.kt:142-165 hardcodes undo/redo in BUILTIN_COMMANDS.
- ChatViewModel.kt:347-353 parses slash commands and calls executeCommand.
- ChatViewModel.kt:563-570 builds ExecuteCommandRequest and calls workspaceClient.executeCommand.
- ChatScreen.kt:492-500 command palette also calls viewModel.executeCommand(command.name, args).
- WorkspaceClient.kt:92-95 already exposes revert/unrevert APIs; ChatViewModel.kt:604-630 has revertSession/unrevertSession helpers.
- Audit noted that upstream TUI /undo removes the most recent user message/responses/file changes using Git, while /redo restores previously undone state; generic server command resolution is not the right boundary for TUI/session actions.

UX Constraint:
Undo/redo must not silently fail as unknown commands, create chat prompt text, or cross workspace/session boundaries. If undo/redo cannot run because of missing Git/session state, surface a human-readable error from the explicit handler.

Expected Behavior:
Typing /undo or selecting undo from the palette dispatches to the explicit undo handler for the current session/workspace. Typing /redo or selecting redo dispatches to the explicit redo handler for the current session/workspace. Neither path uses generic executeCommand unless the team deliberately verifies upstream server-command semantics and encodes that as the contract.

## Design

Introduce a sealed/local command semantics table before branching ad hoc in multiple UI paths. Keep server API commands distinct from Android local/session commands. The command palette should invoke the same dispatcher as typed slash input.

## Acceptance Criteria

- Add failing red unit tests for typed /undo and /redo asserting the exact desired dispatch and non-use of the wrong path.
- Add failing red tests for command palette undo/redo dispatch, not only typed slash input.
- Clarify and encode redo semantics: unrevert existing reverted state versus next-boundary revert behavior; tests must name the chosen behavior.
- Production code routes /undo and /redo through a single explicit command dispatcher shared by typed input and palette.
- Generic executeCommand remains reserved for server/custom/MCP/skill commands.
- Workspace/session identity comes from the current ChatViewModel/WorkspaceClient; no global/default workspace fallback.
- Verification: run targeted ChatViewModel command tests and ./gradlew :app:compileDebugKotlin.


## Notes

**2026-07-05T18:05:09Z**

Broader command audit findings folded into this dispatcher ticket on 2026-07-05:

Do not treat this as only /undo and /redo. The same explicit-dispatcher fix should classify all audited hardcoded built-ins before falling back to generic server/custom/MCP/skill command execution.

Additional commands to classify and test:
- /compact: route to the verified summarize/compaction/session API if supported; otherwise show unsupported/degraded UI.
- /summarize: add/verify alias behavior if supported upstream/TUI.
- /clear and /new: local/session navigation or session lifecycle actions, not generic executeCommand unless upstream contract says otherwise.
- /share and /unshare: use existing share/unshare session APIs if present.
- /help and /connect: local UI/settings/help actions.
- /bug: verify upstream/TUI semantics before exposing; unsupported must be human-readable.

Built-in shadowing risk:
Hardcoded Android commands are currently prepended before server commands and de-duplicated by name. The dispatcher/command source model must avoid Android metadata shadowing server/custom/MCP/skill commands unless the command is intentionally reserved as a local/session built-in.

Acceptance addendum:
Typed slash input and palette selection must share one dispatcher for every classified built-in, and generic executeCommand remains only for commands classified as server/custom/MCP/skill.

**2026-07-06T11:02:26Z**

Completion update from 2026-07-06:

Implemented the explicit shared dispatcher for undo/redo slash commands.

Production changes:
- ChatViewModel.executeCommand now classifies local session commands before server command execution.
- Typed slash input and command palette selection both flow through executeCommand, so /undo and /redo now share one dispatcher.
- undo dispatches to the previous user-message revert boundary using the current session/messages and WorkspaceClient.revertSession; it no longer calls executeCommand.
- redo uses the encoded next-boundary revert semantics: when an active revert points at a user message, redo advances to the next user-message boundary with revertSession; it does not call unrevertSession or generic executeCommand.
- Missing undo/redo boundaries surface human-readable errors: Nothing to undo / Nothing to redo.
- Existing direct revertMessage and unrevertSession UI helpers remain available for row/button actions.

Verification:
- ./gradlew :app:testDebugUnitTest --tests dev.blazelight.p4oc.ui.screens.chat.ChatViewModelTest -> PASS
- ./gradlew :app:compileDebugKotlin -> PASS
- ./gradlew :app:detekt -> PASS
- ./gradlew :app:testDebugUnitTest -> expected FAIL with only two remaining mapped red tests outside oa-3yk2: ToolStateExtTest permission localization boundary (oa-wmvc) and ChatScrollRestorationTest scroll restoration guard (oa-wxf2). The four oa-3yk2 ChatViewModel red failures now pass.
